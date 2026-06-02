// Per-topic completion logic for external tasks.
// Each handler returns { variables } to write back, or throws to fail the task.

const ok = (variables = {}) => ({ variables: shape_vars(variables) })

const shape_vars = (vars) => {
  const out = {}
  for (const [k, v] of Object.entries(vars)) {
    out[k] = {
      value: v,
      type:
        typeof v === 'boolean'
          ? 'Boolean'
          : typeof v === 'number'
            ? Number.isInteger(v)
              ? 'Integer'
              : 'Double'
            : 'String',
    }
  }
  return out
}

export const TOPICS = {
  'validate-order': (task, rng) =>
    ok({
      needsReview: rng.chance(0.3),
      orderTotal: rng.int(20, 5000),
    }),

  'charge-payment': () => ok({ chargeId: 'ch_' + Math.random().toString(36).slice(2, 10) }),

  'send-email': () => ok({ emailSentAt: new Date().toISOString() }),

  'update-inventory': () => ok({ inventoryUpdated: true }),

  'credit-check': (task, rng) =>
    ok({
      score: rng.int(300, 850),
      bureau: rng.pick(['equifax', 'experian', 'transunion']),
    }),

  'risk-assess': (task, rng) =>
    ok({
      risk: rng.weighted([
        ['low', 60],
        ['medium', 30],
        ['high', 10],
      ]),
    }),

  'compute-damage': (task, rng) =>
    ok({ estimatedDamage: rng.int(500, 50000) }),

  'compensate-payment': () => ok({ refunded: true }),

  // Fallback handler for any topic the bot encounters but isn't pre-configured.
  default: () => ok(),
}
