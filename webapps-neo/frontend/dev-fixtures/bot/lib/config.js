import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))

export const load_config = async (overrides = {}) => {
  const path = resolve(here, '..', 'config.json')
  const raw = JSON.parse(await readFile(path, 'utf8'))
  return deep_merge(deep_merge(raw, env_overrides()), overrides)
}

// Pick up DEV_FIXTURES_* env vars so the same image runs in docker
// without editing config.json.
const env_overrides = () => {
  const o = {}
  const url = process.env.DEV_FIXTURES_ENGINE_URL
  const u = process.env.DEV_FIXTURES_ENGINE_USERNAME
  const p = process.env.DEV_FIXTURES_ENGINE_PASSWORD
  const seed = process.env.DEV_FIXTURES_SEED
  if (url || u || p) {
    o.engine = {}
    if (url) o.engine.url = url
    if (u || p) o.engine.auth = { username: u ?? 'demo', password: p ?? 'demo' }
  }
  if (seed) o.seed = parseInt(seed, 10)
  return o
}

const deep_merge = (a, b) => {
  if (b === undefined) return a
  if (typeof a !== 'object' || a === null) return b
  if (typeof b !== 'object' || b === null) return b
  const out = { ...a }
  for (const k of Object.keys(b)) out[k] = deep_merge(a[k], b[k])
  return out
}
