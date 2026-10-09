#!/usr/bin/env node
// Tiny HTTP server + control panel.
//
// Endpoints (all JSON unless noted):
//   GET  /                           — static UI
//   GET  /api/status                 — bot status + recent jobs
//   POST /api/deploy                 — run deploy.js, return output
//   POST /api/spawn                  — body: { process?, message?, count?, vars? }
//   POST /api/stress                 — body: { preset?, count?, mode?, duration?, rate?, process? }
//   POST /api/stress/cancel          — kill the running stress job, if any
//   POST /api/bot/start              — body: { autoDeploy?, noSpawner? }
//   POST /api/bot/stop               — terminate the bot child
//   GET  /api/jobs/:id               — full output for a job (long-running stress)

import { createServer } from 'node:http'
import { spawn as spawn_child } from 'node:child_process'
import { readFile, stat } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { dirname, resolve as r, join, normalize } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const PORT = parseInt(process.env.PORT ?? '3000', 10)
const PUBLIC_DIR = r(here, 'public')

// ---- in-memory job ledger ----------------------------------------------------

const jobs = new Map()
const MAX_JOBS = 50

const new_job = (kind, args = []) => {
  const id = Date.now().toString(36) + Math.random().toString(36).slice(2, 6)
  const job = {
    id,
    kind,
    args,
    startedAt: new Date().toISOString(),
    finishedAt: null,
    exitCode: null,
    output: '',
    child: null,
  }
  jobs.set(id, job)
  // prune
  if (jobs.size > MAX_JOBS) {
    const oldest = [...jobs.keys()].slice(0, jobs.size - MAX_JOBS)
    oldest.forEach((k) => jobs.delete(k))
  }
  return job
}

const job_summary = (j) => ({
  id: j.id,
  kind: j.kind,
  args: j.args,
  startedAt: j.startedAt,
  finishedAt: j.finishedAt,
  exitCode: j.exitCode,
  running: j.finishedAt === null,
  outputTail: j.output.length > 4000 ? '…' + j.output.slice(-4000) : j.output,
})

const run_script = (script, args, kind) => {
  const job = new_job(kind, args)
  const child = spawn_child(process.execPath, [r(here, script), ...args], {
    cwd: here,
    env: process.env,
  })
  job.child = child
  child.stdout.on('data', (b) => (job.output += b.toString()))
  child.stderr.on('data', (b) => (job.output += b.toString()))
  child.on('exit', (code) => {
    job.exitCode = code ?? -1
    job.finishedAt = new Date().toISOString()
    job.child = null
  })
  return job
}

// One slot for the bot child; one for the active stress job (so we can cancel).
let bot_job = null
let stress_job = null

// ---- request helpers ---------------------------------------------------------

const read_body = (req) =>
  new Promise((resolve_p, reject) => {
    let s = ''
    req.on('data', (c) => (s += c))
    req.on('end', () => {
      if (!s) return resolve_p({})
      try {
        resolve_p(JSON.parse(s))
      } catch {
        reject(new Error('invalid JSON'))
      }
    })
    req.on('error', reject)
  })

const send_json = (res, status, obj) => {
  res.writeHead(status, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify(obj))
}

const send_static = async (res, url_path) => {
  const safe = normalize(url_path).replace(/^(\.\.[\/\\])+/, '')
  const candidate = safe === '/' || safe === '' ? '/index.html' : safe
  const file = join(PUBLIC_DIR, candidate)
  try {
    const st = await stat(file)
    if (!st.isFile()) throw 0
    const buf = await readFile(file)
    const ext = file.split('.').pop()
    const ct = {
      html: 'text/html; charset=utf-8',
      css: 'text/css; charset=utf-8',
      js: 'application/javascript; charset=utf-8',
      svg: 'image/svg+xml',
    }[ext] ?? 'application/octet-stream'
    res.writeHead(200, { 'Content-Type': ct })
    res.end(buf)
  } catch {
    res.writeHead(404)
    res.end('not found')
  }
}

// ---- spawn arg builders ------------------------------------------------------

const spawn_args = (body) => {
  const a = []
  if (body.process) a.push('--process', body.process)
  if (body.message) a.push('--message', body.message)
  if (body.count) a.push('--count', String(body.count))
  if (body.vars) a.push('--vars', body.vars)
  if (body['business-key']) a.push('--business-key', body['business-key'])
  return a
}

const stress_args = (body) => {
  const a = []
  if (body.preset) a.push('--preset', body.preset)
  if (body.count) a.push('--count', String(body.count))
  if (body.mode) a.push('--mode', body.mode)
  if (body.duration) a.push('--duration', body.duration)
  if (body.rate) a.push('--rate', body.rate)
  if (body.process) a.push('--process', body.process)
  return a
}

// ---- routes ------------------------------------------------------------------

const route = async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  const { pathname } = url

  if (req.method === 'GET' && pathname === '/api/status') {
    return send_json(res, 200, {
      bot: bot_job ? job_summary(bot_job) : null,
      stress: stress_job ? job_summary(stress_job) : null,
      recent: [...jobs.values()].slice(-15).reverse().map(job_summary),
    })
  }

  if (req.method === 'GET' && pathname.startsWith('/api/jobs/')) {
    const id = pathname.slice('/api/jobs/'.length)
    const j = jobs.get(id)
    if (!j) return send_json(res, 404, { error: 'no such job' })
    return send_json(res, 200, { ...job_summary(j), output: j.output })
  }

  if (req.method === 'POST' && pathname === '/api/deploy') {
    const job = run_script('deploy.js', [], 'deploy')
    job.child.on('exit', () => {})
    // wait briefly to capture early output for nicer UX
    await new Promise((r) => setTimeout(r, 300))
    return send_json(res, 200, job_summary(job))
  }

  if (req.method === 'POST' && pathname === '/api/spawn') {
    const body = await read_body(req)
    const job = run_script('spawn.js', spawn_args(body), 'spawn')
    return send_json(res, 200, job_summary(job))
  }

  if (req.method === 'POST' && pathname === '/api/stress') {
    if (stress_job && stress_job.finishedAt === null) {
      return send_json(res, 409, { error: 'a stress run is already in progress' })
    }
    const body = await read_body(req)
    stress_job = run_script('stress.js', stress_args(body), 'stress')
    stress_job.child.on('exit', () => {
      // leave stress_job pointing at last run, so the UI can still see results
    })
    return send_json(res, 200, job_summary(stress_job))
  }

  if (req.method === 'POST' && pathname === '/api/stress/cancel') {
    if (!stress_job || stress_job.finishedAt !== null || !stress_job.child) {
      return send_json(res, 200, { ok: true, msg: 'nothing to cancel' })
    }
    stress_job.child.kill('SIGTERM')
    return send_json(res, 200, { ok: true })
  }

  if (req.method === 'POST' && pathname === '/api/bot/start') {
    if (bot_job && bot_job.finishedAt === null) {
      return send_json(res, 200, job_summary(bot_job))
    }
    const body = await read_body(req)
    const args = []
    if (body.autoDeploy) args.push('--auto-deploy')
    if (body.noSpawner) args.push('--no-spawner')
    if (body.workersOnly) args.push('--workers-only')
    bot_job = run_script('bot.js', args, 'bot')
    return send_json(res, 200, job_summary(bot_job))
  }

  if (req.method === 'POST' && pathname === '/api/bot/stop') {
    if (!bot_job || bot_job.finishedAt !== null || !bot_job.child) {
      return send_json(res, 200, { ok: true, msg: 'bot not running' })
    }
    bot_job.child.kill('SIGTERM')
    return send_json(res, 200, { ok: true })
  }

  if (req.method === 'GET') return send_static(res, pathname)
  res.writeHead(405)
  res.end()
}

const server = createServer((req, res) => {
  route(req, res).catch((e) => {
    send_json(res, 500, { error: e.message ?? String(e) })
  })
})

server.listen(PORT, () => {
  console.log(`dev-fixtures control panel on http://0.0.0.0:${PORT}`)
})

let shutting_down = false
const shutdown = (signal) => {
  if (shutting_down) {
    // second signal: don't be polite
    process.exit(1)
  }
  shutting_down = true
  console.log(`got ${signal}, shutting down…`)
  for (const j of jobs.values()) {
    if (j.child) {
      try { j.child.kill('SIGTERM') } catch {}
    }
  }
  server.close(() => process.exit(0))
  // hard cap so we never sit on a hung child
  setTimeout(() => {
    console.warn('shutdown timeout, exiting')
    process.exit(0)
  }, 3000).unref()
}
process.on('SIGTERM', () => shutdown('SIGTERM'))
process.on('SIGINT', () => shutdown('SIGINT'))
