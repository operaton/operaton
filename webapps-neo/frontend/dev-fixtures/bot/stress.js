#!/usr/bin/env node
// Stress profiles. Default count: 1000.
//
// Usage:
//   stress                            # default preset, 1000 instances, rampup
//   stress --preset huge              # one of: tiny, small, default, big, huge, custom
//   stress --count 5000               # implies --preset custom
//   stress --mode burst               # one of: burst, rampup, soak
//   stress --duration 10m             # only meaningful with --mode soak
//   stress --rate 120/min             # spawn rate cap (rampup/soak)
//   stress --process orderFulfillment # restrict to one process key (default: weighted mix)
//
// Modes:
//   burst   – fire all N start requests as fast as the engine accepts them, then exit.
//   rampup  – grow to N instances by easing in: linearly increase rate over duration.
//   soak    – maintain ~N concurrent instances by topping up as they finish, for duration.

import { load_config } from './lib/config.js'
import { make_engine_client } from './lib/engine.js'
import { make_rng, resolve_vars } from './lib/rng.js'

const PRESETS = {
  tiny: 50,
  small: 100,
  default: 1000,
  big: 10000,
  huge: 100000,
}

const args = (() => {
  const out = {}
  const argv = process.argv.slice(2)
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    if (a.startsWith('--')) {
      const k = a.slice(2)
      const v = argv[i + 1]?.startsWith('--') ? true : argv[++i]
      out[k] = v ?? true
    }
  }
  return out
})()

const help = () => {
  console.log(`Usage: stress [options]

Presets (--preset):
  tiny     50
  small    100
  default  1000   (default)
  big      10000
  huge     100000
  custom   set via --count

Options:
  --count N             override preset
  --mode burst|rampup|soak    default: rampup
  --duration 10m              for rampup or soak (default: 5m)
  --rate 60/min               cap spawn rate (default: from config)
  --process KEY               restrict to one process; default uses config weights
  --no-stop                   leave instances running (default: just spawn, don't tear down)`)
}

const parse_duration = (s) => {
  if (!s) return 0
  const m = String(s).match(/^(\d+)([smh])?$/)
  if (!m) return 0
  const n = parseInt(m[1], 10)
  const unit = m[2] || 's'
  return n * { s: 1000, m: 60_000, h: 3_600_000 }[unit]
}

const parse_rate = (s, fallback) => {
  if (!s) return fallback
  const m = String(s).match(/^(\d+)\/(min|sec|hour|hr)$/i)
  if (!m) return fallback
  const n = parseInt(m[1], 10)
  const u = m[2].toLowerCase()
  return u.startsWith('min') ? n / 60 : u.startsWith('sec') ? n : n / 3600
}

const default_vars_for = (key) => {
  switch (key) {
    case 'orderFulfillment':
      return { customerEmail: 'random:enum:alice@example.com,bob@example.com' }
    case 'insuranceClaim':
      return { claimType: 'random:enum:auto,home,health', amount: 'random:int:200:30000' }
    case 'loanApproval':
      return { amount: 'random:int:1000:50000', credit: 'random:enum:good,fair,poor' }
    case 'documentReview':
      return { documentId: 'random:int:1:10000' }
    default:
      return {}
  }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const main = async () => {
  if (args.help) {
    help()
    return
  }

  const config = await load_config()
  const client = make_engine_client(config)
  const rng = make_rng((config.seed ?? 1) ^ Date.now())

  const preset = args.preset ?? (args.count !== undefined ? 'custom' : 'default')
  const count = parseInt(args.count ?? PRESETS[preset] ?? PRESETS.default, 10)
  if (!count || count < 0) {
    console.error('Invalid count.')
    process.exit(1)
  }
  const mode = args.mode ?? 'rampup'
  const duration_ms = parse_duration(args.duration ?? (mode === 'rampup' ? '5m' : '10m'))
  const rate_per_sec = parse_rate(args.rate, config.spawner.instancesPerMinute / 60)

  const weight_entries = args.process
    ? [[args.process, 1]]
    : Object.entries(config.spawner.weights)

  const pick_key = () => rng.weighted(weight_entries)

  const start_one = async () => {
    const key = pick_key()
    const business_key = `${config.businessKeyPrefix}stress-${key}-${Math.random()
      .toString(36)
      .slice(2, 8)}`
    try {
      await client.post(`/process-definition/key/${key}/start`, {
        businessKey: business_key,
        variables: resolve_vars(default_vars_for(key), rng.fork('stress:' + key)),
      })
      return true
    } catch (e) {
      if (e.status === 404) {
        console.error(`process key not deployed: ${key}`)
      }
      return false
    }
  }

  console.log(
    `stress: preset=${preset} count=${count} mode=${mode} rate≈${rate_per_sec.toFixed(2)}/s` +
      (mode !== 'burst' ? ` duration=${duration_ms / 1000}s` : ''),
  )
  if (count >= 10000) {
    console.warn(
      '! Heads up: large run — the engine + dashboard may slow down significantly.',
    )
  }

  let started = 0
  let started_ok = 0
  const t_start = Date.now()
  const log_progress = setInterval(() => {
    const elapsed = ((Date.now() - t_start) / 1000) | 0
    process.stdout.write(`\rstarted ${started_ok}/${count}  (${elapsed}s)        `)
  }, 1000)

  if (mode === 'burst') {
    // Fire as fast as we can, in waves of 50 to avoid socket pile-up.
    const concurrency = 50
    while (started < count) {
      const batch = Math.min(concurrency, count - started)
      started += batch
      const results = await Promise.all(Array.from({ length: batch }, () => start_one()))
      started_ok += results.filter(Boolean).length
    }
  } else if (mode === 'rampup') {
    // Linearly increase rate from 1/s to 2*rate over duration_ms, until count reached.
    const peak = rate_per_sec * 2
    const start_t = Date.now()
    while (started < count) {
      const t = (Date.now() - start_t) / duration_ms
      const cur_rate = Math.min(peak, 1 + t * (peak - 1))
      const interval = 1000 / Math.max(0.5, cur_rate)
      started++
      if (await start_one()) started_ok++
      await sleep(interval)
    }
  } else if (mode === 'soak') {
    // Top-up to ~count concurrent for duration_ms.
    const end_t = Date.now() + duration_ms
    while (Date.now() < end_t) {
      let active = 0
      try {
        const r = await client.post('/process-instance/count', {
          processDefinitionKeyIn: weight_entries.map(([k]) => k),
        })
        active = r.count ?? 0
      } catch {
        // best-effort
      }
      const need = Math.max(0, count - active)
      const batch = Math.min(need, 50)
      if (batch > 0) {
        const results = await Promise.all(Array.from({ length: batch }, () => start_one()))
        started += batch
        started_ok += results.filter(Boolean).length
      }
      await sleep(1000 / Math.max(1, rate_per_sec))
    }
  } else {
    console.error(`unknown mode: ${mode}`)
    process.exit(1)
  }

  clearInterval(log_progress)
  const elapsed = ((Date.now() - t_start) / 1000) | 0
  console.log(`\nstress done: started ${started_ok}/${count} in ${elapsed}s`)
}

main().catch((e) => {
  console.error(e.stack || e.message)
  process.exit(1)
})
