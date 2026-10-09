#!/usr/bin/env node
// Start one or many process instances.
//
// Usage:
//   spawn --process orderFulfillment
//   spawn --process loanApproval --count 5 --vars amount=50000,credit=good
//   spawn --message loan-application-received --vars amount=10000,credit=fair
//   spawn --process insuranceClaim --business-key foo

import { load_config } from './lib/config.js'
import { make_engine_client } from './lib/engine.js'
import { make_rng, resolve_vars } from './lib/rng.js'

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
  console.log(`Usage:
  spawn --process <key> [--count N] [--vars k=v,k=v] [--business-key KEY]
  spawn --message <name> [--count N] [--vars k=v,k=v]

Examples:
  spawn --process orderFulfillment
  spawn --process loanApproval --count 10 --vars amount=12000,credit=good
  spawn --message loan-application-received --vars amount=8000`)
}

const parse_vars = (s) => {
  if (!s || typeof s !== 'string') return {}
  return Object.fromEntries(
    s
      .split(',')
      .map((kv) => kv.split('='))
      .filter(([k, v]) => k && v !== undefined)
      .map(([k, v]) => [k.trim(), v.trim()]),
  )
}

const wrap_camunda_vars = (raw) => {
  const out = {}
  for (const [k, v] of Object.entries(raw)) {
    let value = v,
      type = 'String'
    if (/^-?\d+$/.test(v)) {
      value = parseInt(v, 10)
      type = 'Integer'
    } else if (/^-?\d+\.\d+$/.test(v)) {
      value = parseFloat(v)
      type = 'Double'
    } else if (v === 'true' || v === 'false') {
      value = v === 'true'
      type = 'Boolean'
    }
    out[k] = { value, type }
  }
  return out
}

const main = async () => {
  if (args.help || (!args.process && !args.message)) {
    help()
    process.exit(args.help ? 0 : 1)
  }
  const config = await load_config()
  const client = make_engine_client(config)
  const count = parseInt(args.count ?? '1', 10)
  const vars = wrap_camunda_vars(parse_vars(args.vars))
  const rng = make_rng((config.seed ?? 1) ^ Date.now())

  for (let i = 0; i < count; i++) {
    const business_key =
      args['business-key'] ??
      `${config.businessKeyPrefix}manual-${Date.now().toString(36)}-${i}`
    try {
      if (args.message) {
        await client.post('/message', {
          messageName: args.message,
          processVariables: vars,
          businessKey: business_key,
          resultEnabled: true,
        })
        console.log(`» message ${args.message} (${business_key})`)
      } else {
        const r = await client.post(`/process-definition/key/${args.process}/start`, {
          businessKey: business_key,
          variables: vars,
        })
        console.log(`+ ${args.process} → ${r.id} (${business_key})`)
      }
    } catch (e) {
      console.error(`! ${args.process || args.message}:`, e.message)
      process.exit(1)
    }
  }

  void rng // suppress unused
}

main().catch((e) => {
  console.error(e.stack || e.message)
  process.exit(1)
})
