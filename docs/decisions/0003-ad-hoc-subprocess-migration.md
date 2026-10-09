---
status: proposed
date: 2026-10-09
---

# Preserve entered ad-hoc subprocess state during process-instance migration

## Context and Problem Statement

An ad-hoc subprocess is an independently addressable scope even when it has no running children.
Enabled, not-yet-started activities are persisted as marked no-job transition tokens.
Its completion history, completion decision and live children survive between separate trigger calls.
Treating the scope as unsupported would prevent useful migrations. Treating it as an ordinary
subprocess, without additional checks, can silently remove its state, leave a renamed scope pointing
at the old definition or violate sequential ordering.

## Decision Drivers

- Preserve activity/execution identity, variables, completion counts, repeated completions and the completion latch.
- Continue discovery, explicit activation, normal completion and explicit scope completion against the target model.
- Reject unsupported transformations before changing the instance, with actionable validation failures.
- Reuse existing migration of tasks, jobs, subscriptions, multi-instance state and history.

## Considered Options

- Reject all active ad-hoc scopes.
- Enable generic migration without ad-hoc-specific state handling.
- Enable state-preserving migration with structural and live-state validation.

## Decision Outcome

Choose state-preserving migration with validation.

Empty waiting ad-hoc scopes, enabled scopes and scopes with a latched completion decision must retain
a mapped ad-hoc owner. Scopes with running activities or live routing waits, without enabled tokens or
a latch, may be mapped explicitly to ordinary subprocesses, preserving the scope, locals and inert completion metadata. They may also be
removed using the existing ordinary wrapper-removal contract: source scope output/end listeners run
unless skipped, and unmapped scope-local state is removed. Migration is not an implicit decision to
complete an idle owner, activate pending work, or discard a latched completion decision.

Fresh target ad-hoc owners may be inserted around running mapped work, or introduced by mapping an
ordinary subprocess to an ad-hoc subprocess. They start with zero completed activities and a false
latch; configured initial activities and mapped activity input/start behavior are not replayed.
New wrapper scopes retain the existing migration contract for their own input/start mappings.
A preserved ad-hoc-to-ordinary conversion marks its inert context with the reserved
`adHocRetiredContext` variable and a versioned engine sentinel. Returning that same scope to ad-hoc
resumes its tracked history and refreshes live counts without counting completions that occurred
while the scope was ordinary. Resumption removes the marker. Migration continues translating
mapped historical IDs while the context is retired. Unmarked ordinary scopes containing reserved
ad-hoc variable names are rejected rather than silently overwriting business data. Ordinary child
locals named `adHocEnabledActivity` are also rejected when transferred into a new ad-hoc context;
inherited business variables that remain outside that context do not conflict. Retirement rejects
an existing marker instead of overwriting it. Failed ordering validation leaves retired
state intact for a later valid return. Existing mapped ad-hoc owners must remain in the same relative
ancestor order. Enabled tokens must retain their original mapped activation owner, even if adding an
owner around an already-running activity would be valid.

`mapEqualActivities()` includes ad-hoc scopes. Explicit mappings support scope and child ID changes,
and state-sensitive ordinary/ad-hoc conversion. Ordinary subprocess ancestors can be inserted or
removed according to existing migration rules. Conversion to an ad-hoc owner expands a compacted
ordinary execution tree so the new owner remains independently addressable; conversion back restores
ordinary last-child compaction and draining semantics.

The scope execution is repositioned at its target activity, including renamed composite activities.
Engine-owned completed/last-completed activity references are translated through explicit
migration mappings. Active IDs and their count are refreshed from the fully migrated activity tree,
including asynchronous waits and inserted/removed ordinary scopes. The completed list retains order and duplicate entries. Completed source IDs
without a mapping remain historical source IDs, including activities removed from the target model.
Completed counts, the completion latch and application variables are preserved. Live active and enabled
IDs/counts are refreshed from the target tree without treating enabled tokens as started activities. User expressions, serialized
business objects and user-maintained strings are never rewritten.

A completion decision already recorded remains final. Migration does not reevaluate the completion
condition, replay configured initial activation or create an extra completion. The target behavior
applies on subsequent execution, with the existing latch retained even if the target condition changes
or is removed. A change to `cancelRemainingInstances` applies on the next completion continuation;
migration itself does not cancel children or reevaluate the completion condition. A latched scope continues draining its existing children and cannot accept new activations.

Sequential-to-parallel changes are supported. Parallel-to-sequential changes are supported only when
the resulting target scope has at most one running direct child. The validation counts activity instances
and asynchronous transition instances, but excludes directly enabled no-job tokens; multiple running
instances of the same task count separately. A normal
subprocess or multi-instance body counts as one direct child even if it has multiple descendants.
The guard also considers normal-scope removal exposing multiple running children in the target.
Multiple enabled copies remain valid in sequential scopes; they are explicitly activated one at a time.

Enabled tokens participate in migration as transition instances without requiring or creating jobs.
Their marker and local variables move with them; unchanged flow-scope mappings retain execution IDs.
Every live enabled target requires a mapping, including generated multi-instance bodies. Automatic
mapping also recognizes enabled-capable synchronous activities inside ad-hoc contexts. Async-before
on the target applies only when the enabled activity is explicitly activated after migration.

Ordinary wrappers can be inserted around an enabled token or removed again. The generic migration
procedure creates/removes wrapper scopes and their normal listeners/input-output mappings. The parked
child remains unstarted and is discovered recursively through ordinary wrappers, stopping at nested
ad-hoc ownership boundaries. Activating it resumes within its actual target flow scope. This support
does not authorize activation of arbitrary descendants without a corresponding persisted enabled token.
An ordinary wrapper containing only enabled leaves is structurally entered but does not consume a
sequential running slot. Completion discards the maximal pending-only ordinary subtree with normal
scope cancellation, retaining any subtree that still has genuinely running descendants when
`cancelRemainingInstances=false`. Unstarted enabled activities acquire no start/end history or end
listener invocation merely because their structural wrapper is cleaned up.

Pending ad-hoc `ACTIVITY_END` async-after jobs keep that operation, including when the activity has
multiple outgoing sequence flows: completion and outgoing-flow selection have not happened yet.
Changing/removing the target completion expression therefore applies when the job resumes. Existing
`TRANSITION_NOTIFY_LISTENER_TAKE` jobs retain their previously selected transition; migration does not
replay a source activity that already completed under a model without a condition.

A pending scoped activity-end continuation (for example a task with input/output mappings) retains its
scope-local variables and pending output mapping. Such continuations support identical/renamed
migration and insertion/removal of ordinary flow scopes. The retained activity scope execution is
reattached rather than replaced; carrier-local variables retain their ownership, and existing timer
and event-subscription migration handlers transfer boundary dependencies. The output mapping and
outgoing-flow selection run once when the job resumes, including in an ordinary target flow scope.
The same retained-scope path applies to ordinary terminal scoped activity-end continuations.
Adding/removing the pending activity's I/O or event scope follows the established mapped-activity
conversion policy: input/start behavior is not replayed; local variables transfer; only the target
pending output mapping executes. Removing a scope with same-name scope and carrier-local variables
is rejected before mutation because collapsing those variables would overwrite one value. Target
boundary subscriptions/timers can emerge or be removed through the existing generic handlers.
Already transition-selected async-after work is not rewound to execute target output again.

The engine owns the scope variables `adHocActiveActivityIds`, `nrOfActiveAdHocActivities`,
`adHocCompletedActivityIds`, `nrOfCompletedAdHocActivities`, `adHocLastCompletedActivityId`,
`adHocCompletionConditionSatisfied`, `adHocEnabledActivityIds`, and `nrOfEnabledAdHocActivities`,
the enabled-token local marker `adHocEnabledActivity`, and the retired-scope marker
`adHocRetiredContext`. Applications must not overwrite these names or manufacture marker values.

No database schema changes are required.

### Supported regression matrix

`MigrationAdHocSubProcessTest`, `MigrationAdHocEnabledActivityTest`,
`MigrationAdHocIntegrationTest`, `MigrationAdHocScopedActivityEndTest`, and
`MigrationAdHocOwnerChangeTest` exercise:

| Area | Cases |
| --- | --- |
| Lifecycle | Before entry; empty active scope; active children; latched/draining; scope completed; ended process rejected |
| Enabled scheduling | No-job token identity/variables; sequential/parallel; active+enabled mix; duplicate enabled copies; rename/missing mapping; ordinary wrapper insertion/removal; pending-only wrapper cleanup for both cancellation policies and mixed running descendants; unstarted composite listener/history preservation; nested ad-hoc ownership; MI bodies/instances; target async-before; synchronous service task; join wait and loop; multi-process rollback |
| Mapping | Equal/generated mapping; renamed scope/children; absent empty-scope mapping; absent active-child mapping; removed historical completed activity |
| Types/topology | Fresh owner insertion; ordinary/ad-hoc conversion with compacted/scoped child; running/routing owner removal with output/listener skip policy; ad-hoc/ordinary roundtrip with retired progress, reserved-name collision and rollback checks; atomic idle/enabled/latched owner-removal rejection; enabled-owner transfer rejection; ordinary scope added/removed; nested ordinary/ad-hoc and fresh owners per MI iteration |
| Ordering | Sequential to parallel; parallel to sequential with zero/one/two active children; asynchronous waits; scope removal exposing concurrent children |
| Multi-instance | Sequential/parallel ad-hoc instances; sequential/parallel task body and instances inside ad-hoc; continued remaining iterations |
| Asynchronous work | Ad-hoc async-before/async-after; child async-before/async-after; preserved job identity/retries/due date/priority and incident identity/recovery; continued execution; pending completion with one/two outgoing flows; target condition changes; scoped I/O continuation; ordinary wrapper relocation/roundtrip; retained timer/subscription dependencies and exact output/end-listener counts; scope-status addition/removal and variable-shadowing rejection |
| Event dependencies | Timer boundary job identity/due date and firing; message boundary subscription identity and post-migration correlation |
| State/history | Process/scope/child variables; input mappings not replayed and target output mappings used; repeated completion IDs and counts; mapped activity references; cancellation-policy changes; latch after target expression removal/change; activity/task history identity; active scope history ID retained while definition/activity ID/name/type update to target; finished child history stays source |
| Atomicity/security | Invalid single-instance ordering leaves tasks/jobs/state intact; invalid later instance rolls back earlier migration in the same command; authorization and tenant rejection preserve source state |
| Generic integrations | Called process/task identity and completion; external task lock/retries/variables and completion; noninterrupting message event-subprocess subscription/correlation; compensation ownership preservation and missing-mapping rejection; async batch seed/execution/monitor lifecycle with mixed enabled/running children |
| Public operations | Target discovery, trigger, completion and explicit completion after migration |

### Exclusions and boundaries

- Idle, enabled and latched ad-hoc owners cannot be removed or converted to ordinary subprocesses:
  those states have no equivalent ordinary continuation. These are explicit ad-hoc-specific live-state
  restrictions, not generic migration limitations. Owners with running activities or routing waits,
  without a latch or pending enabled work, have the state-preserving conversion/removal support described above.
- Enabled tokens cannot silently acquire another ad-hoc activation owner. Added fresh owners around
  running work are supported, but transferring pending selection authority requires a separate policy.
- Missing mappings for live children remain invalid under the existing migration rules. Migration
  is not an implicit cancellation or completion API.
- Pending scoped async-after continuations support flow-scope relocation and scope-status changes.
  Boundary dependencies retain generic mapping/update/removal rules; unsupported ownership and
  same-name variables that would collapse into one scope fail atomically.
- Existing generic constraints on multi-instance addition/removal, sequential/parallel multi-instance
  behavior changes, compensation, event trigger updates, async continuation compatibility and tenant
  boundaries still apply. This change does not relax those contracts.
- Representative ad-hoc integrations cover call activities, external tasks, noninterrupting message
  event subprocesses, compensation subscriptions, timer/message boundaries and async migration batches.
  Other event types and combinations retain the existing generic migration contracts and validators;
  this matrix does not claim every Cartesian combination.
- An already ended process has no runtime instance to migrate. Finished activity history remains
  associated with the source definition; active historic records follow the normal migration behavior.
- Application-owned variables and completion expressions that name activity IDs must be adapted by
  the application when required. Unmapped completed IDs deliberately remain historical.

### Consequences

- Useful live migrations are supported without resetting user progress or discarding completion decisions.
- The added validation is state-sensitive rather than a blanket rejection of active scopes.
- Owner topology changes are state-sensitive. Fresh owners never fabricate historical completions;
  retiring owners follows either preserved-scope conversion or existing removed-scope cleanup semantics.

### Confirmation

Run `MigrationAdHocSubProcessTest`, `MigrationAdHocEnabledActivityTest`,
`MigrationAdHocIntegrationTest`, `MigrationAdHocScopedActivityEndTest`, and
`MigrationAdHocOwnerChangeTest` with the normal engine tests
and the existing migration suites.
Include the ad-hoc runtime lifecycle tests because post-migration execution must obey the same ordering,
completion and multi-instance invariants as instances started on the target definition directly.
