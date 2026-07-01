---
status: "Proposed"
date: 2026-07-01
decision-makers: ["@javahippie"]
consulted: []
informed: []
---

# Version Tag Filtering for Message-Correlated Events

## Context and Problem Statement

When a message **starts a new process instance**, Operaton can only correlate it to the
**latest** version of a process definition. Correlating a message to an already **running**
instance is not affected — a waiting execution belongs to a concrete definition version and
is matched regardless of whether that version is the latest. The limitation is specific to
message start events, and it is not expressed in the correlation logic itself but is a
consequence of the persistence model: a message start-event subscription in the
`ACT_RU_EVENT_SUBSCR` table exists only for the latest deployed version of a process. When a
new version is deployed, the engine deletes the previous version's start-event subscriptions
and creates new ones for the new version. As a result, older versions become unreachable for
**starting** new instances through message correlation.

Some users bundle related process (and decision) definitions under a shared **version
tag** and keep several versions active in parallel, each serving a different use case. For
these users, a message must be routed to the version that carries a specific version tag,
not always to the globally latest version. Operaton offers no way to express this today.

This ADR asks: **How should Operaton allow a message to start a new instance of a
non-latest process definition version, selected by version tag, while preserving the current
latest-version-only behaviour as the default?**

See [issue #1559](https://github.com/operaton/operaton/issues/1559).

## Decision Drivers

- Preserve the current default behaviour: without a version tag, correlation must continue
  to target the latest version (backward compatibility).
- Make older, still-relevant versions startable via message correlation when selected by
  version tag.
- Keep the correlation query path simple and efficient.
- Stay consistent with how version tags are already modelled and resolved in Operaton
  (columns, entities, parsing, and call-activity resolution).
- Keep the database migration cost across the seven supported database vendors reasonable
  and reversible.

## Considered Options

1. Retain event subscriptions and add a `VERSION_TAG_` column to `ACT_RU_EVENT_SUBSCR`
2. Retain event subscriptions and resolve the version tag through a join to `ACT_RE_PROCDEF`
3. Resolve the version at correlation time via the deployment cache, without retaining subscriptions
4. Status quo — keep latest-version-only behaviour and rely on external workarounds

## Decision Outcome

Chosen option: **"Retain event subscriptions and add a `VERSION_TAG_` column to
`ACT_RU_EVENT_SUBSCR`"** (Option 1), because it directly satisfies the reported need, keeps
the correlation query the simplest of the retaining options (the version tag is available
on the subscription row itself, without a join), and stores the tag where it can be indexed
and filtered. It builds on the version-tag mechanics already present elsewhere in the
engine and keeps the correlation code path close to its current shape.

To keep the current behaviour intact, previous versions do not accumulate subscriptions
without bound. The retained set is **at most one active start subscription per
`(key, tenant, version tag)` — the latest version bearing that tag — plus the global latest
version**. Deploying a new version therefore still supersedes (deletes) the subscription of
an earlier version that carries the **same** tag, and only retains earlier versions that
carry a **different**, non-empty tag. Correlation without a version tag continues to resolve
to the latest version.

### Consequences

- Good, because messages can be routed to a specific, non-latest version selected by its
  version tag, enabling parallel, tag-bundled versions.
- Good, because the version tag lives directly on the subscription row, so correlation
  filtering needs no additional join and can be indexed.
- Good, because it reuses the established version-tag model and the existing correlation
  code path rather than introducing a new resolution mechanism.
- Bad, because it requires a database schema change (a new column on `ACT_RU_EVENT_SUBSCR`)
  with create scripts and upgrade scripts for all supported database vendors.
- Bad, because retaining subscriptions for multiple versions changes long-standing
  invariants (e.g. the "one message start subscription per name and tenant" guard) that must
  be relaxed carefully to avoid regressions.
- Bad, because the deploy-time and undeploy-time subscription lifecycle must be kept
  symmetric: undeployment has to restore the same-tag predecessor without duplicating an
  already-retained subscription, adding complexity to the existing restore logic in
  `DeleteProcessDefinitionsByIdsCmd`.
- Neutral, because the version tag is denormalised onto the subscription row and must be
  kept consistent with the process definition; because subscriptions are immutable for a
  deployed version, the value is written once at subscription creation.

### Confirmation

- Integration tests that deploy multiple tagged versions of the same process, assert that
  each version keeps its start-event subscription, and verify that correlation with a
  version tag reaches the intended version while correlation without a tag reaches the
  latest version.
- A test asserting that deploying multiple versions with distinct version tags no longer
  triggers the duplicate message-start-subscription error.
- Lifecycle tests covering undeployment: undeploying a version that superseded a same-tag
  predecessor restores that predecessor's subscription (the tag becomes correlatable again),
  undeploying a version whose tag also exists on a retained sibling does not create a
  duplicate subscription, and undeploying a retained non-latest tagged version restores
  nothing.
- Schema tests exercising the new create scripts and the version upgrade scripts, validated
  by the engine's create-versus-upgrade schema parity harness, for at least H2 and
  PostgreSQL.
- REST tests posting a correlation request containing a `versionTag` and asserting the
  correct version is started, together with regeneration and validation of the OpenAPI
  specification.

## Pros and Cons of the Options

### Retain event subscriptions and add a `VERSION_TAG_` column to `ACT_RU_EVENT_SUBSCR`

Stop deleting the previous version's start-event subscriptions on redeploy, store the
process definition's version tag on the subscription row, and filter correlation by it.

- Good, because the version tag is present on the subscription itself, so correlation
  filtering requires no join and can be indexed.
- Good, because it matches the approach proposed in the originating issue and keeps the
  correlation query close to its current form.
- Neutral, because the tag is denormalised and must be written consistently at subscription
  creation.
- Bad, because it requires a new column with create and upgrade scripts for all supported
  database vendors.

### Retain event subscriptions and resolve the version tag through a join to `ACT_RE_PROCDEF`

Retain the subscriptions as above, but do not add a column. Each start-event subscription's
`CONFIGURATION_` column already holds the process definition id, so the version tag can be
resolved by joining `ACT_RU_EVENT_SUBSCR` to `ACT_RE_PROCDEF.VERSION_TAG_` at query time.

- Good, because it avoids any database schema change and keeps the version tag in a single
  authoritative place (`ACT_RE_PROCDEF`), with no denormalisation to keep consistent.
- Neutral, because the join follows an existing pattern (the conditional start-event query
  already joins the subscription table to `ACT_RE_PROCDEF`).
- Bad, because it makes the correlation queries more complex and couples them to the process
  definition table.
- Bad, because filtering and indexing on the version tag are less direct than a column on the
  subscription row.

### Resolve the version at correlation time via the deployment cache, without retaining subscriptions

Leave the subscription lifecycle unchanged (only the latest version keeps a subscription)
and instead resolve the target definition by version tag at correlation time, similar to how
call activities resolve a called definition
(`DeploymentCache.findDeployedProcessDefinitionByKeyVersionTagAndTenantId`).

- Good, because it needs no schema change and no change to the deployment/retention logic.
- Bad, because it does not actually satisfy the requirement: correlation starts from a
  message name, and without retained subscriptions there is no mapping from a message name to
  a non-latest version. Resolving by version tag would additionally require a process
  definition key, changing the shape of the correlation API and its usage.
- Bad, because it diverges from how message correlation works today and would special-case the
  version-tag path.

### Status quo — keep latest-version-only behaviour and rely on external workarounds

Make no change and expect users to model around the limitation outside Operaton.

- Neutral, because it has no implementation or migration cost.
- Bad, because it leaves the reported use case unsupported.
- Bad, because the originating issue explicitly rejects external workarounds as diverging too
  far from Operaton and as risky to upgrade.

## Open Points for Discussion

These questions affect scope and semantics but do not change the chosen storage approach.
They are recorded here for the reviewers to settle.

### Event-type scope

On redeploy, message, signal, and conditional start-event subscriptions are all deleted
together for the previous version. The concrete, reported use case is **message**
correlation. Retention and version-tag filtering could be scoped to message start events
only, or applied consistently to signal and conditional start events as well.

- Scoping to message start events keeps the change small and focused on the reported need.
- Extending to all three start-event types is more consistent but enlarges the surface,
  including how signal delivery and conditional evaluation select among retained versions.

Recommended starting point: **message start events only**, with the boundary made explicit
so it can be revisited if the same need arises for signals or conditions.

### Default behaviour when multiple versions subscribe

Once several versions retain a subscription for the same message name, a correlation without
a version tag would match multiple subscriptions. Today this raises a
`MismatchingMessageCorrelationException` ("too many"). The default must instead continue to
resolve to the **latest** version, so that existing behaviour is preserved for callers that
do not use version tags.

A version tag is otherwise treated as an ordinary correlation criterion and introduces no new
error semantics. A version tag that matches no subscription simply narrows the candidate set
to empty, after which the existing per-method behaviour applies: a single correlation
(`correlate` / `correlateWithResult` / `correlateStartMessage`) throws
`MismatchingMessageCorrelationException` just as an unmatched business key or tenant id does,
while `correlateAll` correlates to zero targets without raising.

### Duplicate message-start-subscription guard

The deployer currently forbids two message start-event subscriptions sharing the same
message name within a tenant. Retaining subscriptions for multiple versions makes this a
legitimate state, so the uniqueness constraint should become **message name per tenant per
version tag** rather than message name per tenant. This is the same latest-per-tag rule seen
from the guard's side: a second subscription with a distinct tag is allowed, a second
subscription with the same tag supersedes the first.

### Multiple versions sharing a version tag

The same version tag can legitimately be applied to more than one deployment. Two distinct
cases must be distinguished, because they behave differently:

- **Different keys sharing a tag** (the bundling use case): retention is per process
  definition key, so each key keeps its own subscriptions and there is no conflict.
- **Multiple versions of the same key sharing a tag**: keeping an active start subscription
  for each would make a tag ambiguously match several subscriptions for the same message
  name. This must not happen.

The retention rule on the deployment side is therefore **latest-per-tag, not
keep-everything**: at most one active start subscription per `(key, tenant, version tag)` is
kept, plus the global latest version. Deploying a new version with the **same** tag as an
earlier retained version supersedes (deletes) that earlier subscription; only earlier
versions with a **different**, non-empty tag are retained. Untagged previous versions are
removed as they are today, since they are neither selectable by tag nor the global latest
any longer.

This write-side rule keeps the correlation read side unambiguous: a tag resolves to exactly
one retained subscription. Where a tag could still resolve to more than one candidate (for
example across the transitional data of an upgrade), correlation should resolve to the
latest version carrying the tag, mirroring the existing behaviour of
`findProcessDefinitionByKeyVersionOrVersionTag`.

### Undeployment must restore the same-tag predecessor symmetrically

Retention on deploy has a mirror obligation on **undeploy / delete**. Consider version A
(tag `1.12`) superseded by version B (same tag `1.12`): deploying B deletes A's subscription,
so the tag points at B. If B is then undeployed, the tag must fall back to A rather than
becoming uncorrelatable.

The engine already restores start-event subscriptions when the latest definition is deleted:
`DeleteProcessDefinitionsByIdsCmd` finds the previous surviving version and re-runs
`BpmnDeployer.addEventSubscriptions` for it (and full-deployment deletion funnels through the
same command). This restore is **not version-tag-aware**, and retention breaks it in two ways
that must be addressed:

- **Restore can duplicate a retained subscription.** When the predecessor being restored
  already holds a live subscription under a *different* tag, blindly re-adding its
  subscriptions creates a duplicate. Restore must be idempotent with respect to already-retained
  versions.
- **Restore must follow the version tag, not just the global latest.** Today restore only fires
  when the deleted definition is the global latest and only restores the immediate predecessor.
  Under latest-per-tag, deleting the version that holds a tag's subscription must restore the
  next-latest *surviving* version bearing that **same** tag (which need not be the immediate
  predecessor), while deleting a retained non-latest tagged version must restore nothing.

In short, the deploy-time supersession rule and the undeploy-time restore rule are two halves
of the same invariant (at most one active subscription per `(key, tenant, version tag)`) and
must be implemented together to avoid orphaned or duplicated subscriptions.

## More Information

- [Operaton issue #1559 – Version tag filtering for event subscriptions](https://github.com/operaton/operaton/issues/1559)
- [MADR format](https://adr.github.io/madr/)
