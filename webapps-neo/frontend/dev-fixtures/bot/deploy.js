#!/usr/bin/env node
// Deploys every *.bpmn and *.dmn under processes/ and rules/ as one deployment.
// Idempotent: enable-duplicate-filtering + deploy-changed-only mean unchanged
// resources are skipped.
//
// Files matching `<name>-v<N>.<ext>` are held out and deployed as separate,
// ordered deployments — that's how we get v2/v3/... of the same process key
// (each deployment with the same process id bumps the engine's version).

import { readdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { dirname, resolve, basename } from 'node:path'
import { load_config } from './lib/config.js'
import { make_engine_client } from './lib/engine.js'

const here = dirname(fileURLToPath(import.meta.url))
const root = resolve(here, '..')

const VERSIONED = /-v(\d+)\.(bpmn|dmn|form)$/

const collect = async (dir, accept) => {
  const out = []
  let entries
  try {
    entries = await readdir(dir, { withFileTypes: true })
  } catch {
    return out
  }
  for (const e of entries) {
    const p = resolve(dir, e.name)
    if (e.isDirectory()) out.push(...(await collect(p, accept)))
    else if (accept(e.name)) out.push(p)
  }
  return out
}

const summarize = (result) => {
  const deployed = Object.values(result.deployedProcessDefinitions ?? {}).map(
    (d) => `${d.key} (v${d.version})`,
  )
  const deployedDmn = Object.values(result.deployedDecisionDefinitions ?? {}).map(
    (d) => `${d.key} (v${d.version})`,
  )
  console.log(`Deployment ${result.id} → ${result.name}`)
  if (deployed.length) console.log('  Process definitions: ' + deployed.join(', '))
  if (deployedDmn.length) console.log('  Decision definitions: ' + deployedDmn.join(', '))
  if (deployed.length === 0 && deployedDmn.length === 0) {
    console.log('  Nothing changed. (deploy-changed-only)')
  }
}

const main = async () => {
  const config = await load_config()
  const client = make_engine_client(config)

  const all = [
    ...(await collect(resolve(root, 'processes'), (n) => n.endsWith('.bpmn'))),
    ...(await collect(resolve(root, 'rules'), (n) => n.endsWith('.dmn'))),
    ...(await collect(resolve(root, 'forms'), (n) => n.endsWith('.form'))),
  ]
  if (all.length === 0) {
    console.error('No .bpmn / .dmn / .form files found under dev-fixtures/.')
    process.exit(1)
  }

  const base_files = all.filter((f) => !VERSIONED.test(basename(f)))
  const versioned_files = all
    .filter((f) => VERSIONED.test(basename(f)))
    .map((f) => ({ path: f, version: Number(basename(f).match(VERSIONED)[1]) }))
    .sort((a, b) => a.version - b.version)

  console.log(`Deploying ${base_files.length} base files…`)
  base_files.forEach((f) => console.log('  ' + f.replace(root + '/', '')))
  summarize(await client.deploy({ name: 'dev-fixtures', files: base_files }))

  for (const { path, version } of versioned_files) {
    const stem = basename(path).replace(VERSIONED, '')
    const name = `dev-fixtures-${stem}-v${version}`
    console.log(`\nDeploying ${path.replace(root + '/', '')} as ${name}…`)
    summarize(await client.deploy({ name, files: [path] }))
  }
}

main().catch((err) => {
  console.error(err.message)
  process.exit(1)
})
