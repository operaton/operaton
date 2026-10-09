// Reproducible PRNG: mulberry32. Seeded once, threaded everywhere.

export const make_rng = (seed) => {
  let s = (seed >>> 0) || 1
  const next = () => {
    s = (s + 0x6d2b79f5) >>> 0
    let t = s
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
  return {
    next,
    int: (lo, hi) => Math.floor(next() * (hi - lo + 1)) + lo,
    pick: (arr) => arr[Math.floor(next() * arr.length)],
    weighted: (entries) => {
      // entries: [[item, weight], ...]
      const total = entries.reduce((s, [, w]) => s + w, 0)
      let r = next() * total
      for (const [item, w] of entries) {
        if ((r -= w) <= 0) return item
      }
      return entries[entries.length - 1][0]
    },
    chance: (p) => next() < p,
    // log-normal sample for completion delays. medianMs and sigma in log-space.
    logNormal: (medianMs, sigma) => {
      // Box-Muller for a normal sample, then exponentiate around log(median)
      const u1 = Math.max(next(), 1e-9)
      const u2 = next()
      const z = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2)
      return Math.exp(Math.log(medianMs) + sigma * z)
    },
    // child rng with a derived seed (so subsystems are independent but reproducible)
    fork: (label) => {
      let h = 0
      for (let i = 0; i < label.length; i++) {
        h = (Math.imul(h, 31) + label.charCodeAt(i)) | 0
      }
      return make_rng((s ^ h) >>> 0)
    },
  }
}

// Resolve a "random:..." spec to a concrete value, using the given rng.
// Specs:
//   random:int:lo:hi
//   random:enum:a,b,c
//   random:bool[:p]    (default p=0.5)
//   random:float:lo:hi
// Anything else is returned as-is.
export const resolve_value_spec = (spec, rng) => {
  if (typeof spec !== 'string' || !spec.startsWith('random:')) return spec
  const [, kind, ...rest] = spec.split(':')
  if (kind === 'int') {
    const [lo, hi] = rest.map(Number)
    return rng.int(lo, hi)
  }
  if (kind === 'float') {
    const [lo, hi] = rest.map(Number)
    return lo + rng.next() * (hi - lo)
  }
  if (kind === 'enum') {
    return rng.pick(rest.join(':').split(','))
  }
  if (kind === 'bool') {
    const p = rest.length ? Number(rest[0]) : 0.5
    return rng.chance(p)
  }
  return spec
}

// Resolve a vars object: { k: spec, ... } → { k: { value, type } }
export const resolve_vars = (specs, rng) => {
  const out = {}
  for (const [k, spec] of Object.entries(specs ?? {})) {
    const v = resolve_value_spec(spec, rng)
    out[k] = { value: v, type: typeof_for_camunda(v) }
  }
  return out
}

const typeof_for_camunda = (v) => {
  if (typeof v === 'boolean') return 'Boolean'
  if (typeof v === 'number') return Number.isInteger(v) ? 'Integer' : 'Double'
  return 'String'
}
