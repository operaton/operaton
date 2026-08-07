#!/usr/bin/env node
// Long-running bot that:
//   - works external tasks across all topics, with configurable failure injection
//   - completes user tasks with log-normal delays
//   - spawns new instances at a target rate, weighted by process key
//   - dispatches messages to drive message-start processes
//
// Reproducible via config.seed (everything random is forked off it).
//
// Flags:
//   --auto-deploy        run deploy.js before starting workers
//   --no-spawner         skip the auto-spawner (just process the queue)
//   --workers-only       only external + user task workers (no spawner / message dispatch)
//   --quiet              less log noise

import { spawn as childSpawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { dirname, resolve as r } from 'node:path'
import { load_config } from './lib/config.js'
import { make_engine_client } from './lib/engine.js'
import { make_rng, resolve_vars } from './lib/rng.js'
import { TOPICS } from './lib/topics.js'

const here = dirname(fileURLToPath(import.meta.url))

const flags = new Set(process.argv.slice(2))
const flag = (n) => flags.has(n)
const QUIET = flag('--quiet')
const log = (...a) => QUIET || console.log(...a)
const warn = (...a) => console.warn(...a)

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// ---- external task worker ----------------------------------------------------

const make_external_task_worker = (client, rng, config) => {
  const { lockDurationMs, maxTasksPerFetch, pollIntervalMs, failureRatePerTopic } =
    config.externalTask
  const worker_id = 'dev-bot-' + Math.random().toString(36).slice(2, 8)
  const failure_rate = (topic) =>
    failureRatePerTopic[topic] ?? failureRatePerTopic.default ?? 0

  const work_one = async (task) => {
    const handler = TOPICS[task.topicName] ?? TOPICS.default
    const topic_rng = rng.fork('topic:' + task.topicName)
    if (topic_rng.chance(failure_rate(task.topicName))) {
      await client.post(`/external-task/${task.id}/failure`, {
        workerId: worker_id,
        errorMessage: `Synthetic failure on ${task.topicName}`,
        retries: 0,
        retryTimeout: 0,
      })
      log(`✗ ${task.topicName} ${task.id.slice(0, 8)}`)
      return
    }
    try {
      const result = handler(task, topic_rng)
      await client.post(`/external-task/${task.id}/complete`, {
        workerId: worker_id,
        variables: result.variables,
      })
      log(`✓ ${task.topicName} ${task.id.slice(0, 8)}`)
    } catch (e) {
      await client
        .post(`/external-task/${task.id}/failure`, {
          workerId: worker_id,
          errorMessage: e.message,
          retries: 0,
          retryTimeout: 0,
        })
        .catch(() => {})
      warn(`! ${task.topicName} ${task.id.slice(0, 8)}: ${e.message}`)
    }
  }

  const loop = async () => {
    while (true) {
      try {
        const topics_to_fetch = Object.keys(TOPICS)
          .filter((t) => t !== 'default')
          .map((t) => ({ topicName: t, lockDuration: lockDurationMs }))
        const tasks = await client.post('/external-task/fetchAndLock', {
          workerId: worker_id,
          maxTasks: maxTasksPerFetch,
          topics: topics_to_fetch,
        })
        if (tasks.length === 0) {
          await sleep(pollIntervalMs)
          continue
        }
        await Promise.all(tasks.map(work_one))
      } catch (e) {
        warn('external-task loop error:', e.message)
        await sleep(2000)
      }
    }
  }

  return { run: loop, workerId: worker_id }
}

// ---- user task completer -----------------------------------------------------

const make_user_task_completer = (client, rng, config) => {
  const { pollIntervalMs, completionDelayMedianMs, completionDelaySigma, stallProbability } =
    config.userTaskCompleter
  const scheduled = new Set()
  const form_cache = new Map() // formKey/refName → schema  (null when no form)

  const has_form = (task) =>
    !!task.formKey ||
    !!task.camundaFormRef ||
    !!task.operatonFormRef

  const cache_key = (task) =>
    task.formKey || JSON.stringify(task.camundaFormRef ?? task.operatonFormRef ?? '')

  const fetch_form_schema = async (task) => {
    if (!has_form(task)) return null
    const key = cache_key(task)
    if (form_cache.has(key)) return form_cache.get(key)
    try {
      const schema = await client.get(`/task/${task.id}/deployed-form`)
      const ok = schema && Array.isArray(schema.components) ? schema : null
      form_cache.set(key, ok)
      return ok
    } catch {
      form_cache.set(key, null)
      return null
    }
  }

  const synth_value = (component, topic_rng) => {
    const required = component.validate?.required
    if (!required && topic_rng.chance(0.5)) return undefined
    switch (component.type) {
      case 'textfield':
        return component.label ? `synthetic ${component.label}` : 'synthetic'
      case 'textarea':
        return 'auto-completed by load bot'
      case 'number': {
        const lo = component.validate?.min ?? 0
        const hi = component.validate?.max ?? Math.max(lo + 1, 100)
        return topic_rng.int(lo, hi)
      }
      case 'checkbox':
        return topic_rng.chance(0.5)
      case 'radio':
      case 'select':
        return component.values?.length ? topic_rng.pick(component.values).value : undefined
      case 'datetime':
      case 'date':
        return new Date().toISOString().slice(0, 10)
      default:
        return undefined
    }
  }

  const synth_variables = (schema, task_rng) => {
    const out = {}
    for (const c of schema.components ?? []) {
      if (!c.key || c.disabled) continue
      const v = synth_value(c, task_rng)
      if (v === undefined) continue
      out[c.key] = {
        value: v,
        type:
          typeof v === 'boolean' ? 'Boolean' :
          typeof v === 'number'  ? (Number.isInteger(v) ? 'Long' : 'Double') :
          'String',
      }
    }
    return out
  }

  const complete_after_delay = async (task) => {
    if (scheduled.has(task.id)) return
    scheduled.add(task.id)
    const task_rng = rng.fork('userTask:' + task.id)
    const delay = task_rng.logNormal(completionDelayMedianMs, completionDelaySigma)
    await sleep(delay)
    if (task_rng.chance(stallProbability)) {
      scheduled.delete(task.id)
      return
    }
    try {
      const schema = await fetch_form_schema(task)
      if (schema) {
        const variables = synth_variables(schema, task_rng)
        await client.post(`/task/${task.id}/submit-form`, { variables })
      } else {
        await client.post(`/task/${task.id}/complete`, { variables: {} })
      }
      log(`✓ user-task ${task.id.slice(0, 8)} (after ${(delay / 1000) | 0}s)`)
    } catch (e) {
      if (e.status !== 404) warn(`! user-task ${task.id.slice(0, 8)}: ${e.message}`)
    } finally {
      scheduled.delete(task.id)
    }
  }

  const loop = async () => {
    while (true) {
      try {
        const tasks = await client.get('/task?maxResults=200')
        for (const t of tasks) {
          if (!scheduled.has(t.id)) void complete_after_delay(t)
        }
      } catch (e) {
        warn('user-task loop error:', e.message)
      }
      await sleep(pollIntervalMs)
    }
  }
  return { run: loop }
}

// ---- spawner -----------------------------------------------------------------

const make_spawner = (client, rng, config, business_key_prefix) => {
  const { instancesPerMinute, weights } = config.spawner
  const interval_ms = 60000 / Math.max(1, instancesPerMinute)
  const entries = Object.entries(weights)
  let counter = 0

  const start_one = async () => {
    const key = rng.weighted(entries)
    const business_key = `${business_key_prefix}${key}-${++counter}`
    try {
      await client.post(`/process-definition/key/${key}/start`, {
        businessKey: business_key,
        variables: resolve_vars(default_vars_for(key), rng.fork('start:' + key)),
      })
      log(`+ ${key} (${business_key})`)
    } catch (e) {
      // Process key not deployed? note and back off briefly.
      if (e.status === 404) {
        warn(`process key not deployed: ${key} — skipping (deploy first?)`)
      } else {
        warn(`spawn error (${key}):`, e.message)
      }
    }
  }

  const loop = async () => {
    while (true) {
      void start_one()
      await sleep(interval_ms)
    }
  }
  return { run: loop }
}

const default_vars_for = (key) => {
  switch (key) {
    case 'orderFulfillment':
      return { customerEmail: 'random:enum:alice@example.com,bob@example.com,carol@example.com' }
    case 'insuranceClaim':
      return {
        claimType: 'random:enum:auto,home,health',
        amount: 'random:int:200:30000',
      }
    case 'loanApproval':
      return { amount: 'random:int:1000:50000', credit: 'random:enum:good,fair,poor' }
    case 'documentReview':
      return { documentId: 'random:int:1:10000' }
    default:
      return {}
  }
}

// ---- message dispatcher ------------------------------------------------------

const make_message_dispatcher = (client, rng, config) => {
  const { intervalMs, messages } = config.messageDispatcher
  const entries = Object.entries(messages)
  if (entries.length === 0) return { run: async () => {} }

  const dispatch = async () => {
    const [name, spec] = rng.weighted(entries.map(([k, v]) => [[k, v], v.weight ?? 1]))
    try {
      await client.post('/message', {
        messageName: name,
        processVariables: resolve_vars(spec.vars, rng.fork('msg:' + name)),
        resultEnabled: false,
      })
      log(`» ${name}`)
    } catch (e) {
      // No process definition listening — that's not an error in dev.
      if (e.status !== 400 && e.status !== 500) warn(`message ${name}:`, e.message)
    }
  }

  const loop = async () => {
    while (true) {
      void dispatch()
      await sleep(intervalMs)
    }
  }
  return { run: loop }
}

// ---- orchestration -----------------------------------------------------------

const run_deploy = () =>
  new Promise((resolve_p, reject) => {
    const child = childSpawn(process.execPath, [r(here, 'deploy.js')], {
      stdio: 'inherit',
    })
    child.on('exit', (code) =>
      code === 0 ? resolve_p() : reject(new Error(`deploy exited ${code}`)),
    )
  })

const main = async () => {
  const config = await load_config()
  const client = make_engine_client(config)
  const rng = make_rng(config.seed ?? 1)

  if (flag('--auto-deploy')) {
    log('Auto-deploying first…')
    await run_deploy()
  }

  log(`Bot starting against ${config.engine.url}  (seed=${config.seed})`)

  const ext = make_external_task_worker(client, rng.fork('ext'), config)
  const usr = make_user_task_completer(client, rng.fork('usr'), config)

  const tasks = [ext.run(), usr.run()]
  if (!flag('--workers-only')) {
    if (!flag('--no-spawner')) {
      const sp = make_spawner(client, rng.fork('spawn'), config, config.businessKeyPrefix)
      tasks.push(sp.run())
    }
    const md = make_message_dispatcher(client, rng.fork('msg'), config)
    tasks.push(md.run())
  }

  await Promise.all(tasks)
}

main().catch((err) => {
  console.error(err.stack || err.message)
  process.exit(1)
})
