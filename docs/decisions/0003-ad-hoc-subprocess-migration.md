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

Successful non-compensation Activities inside an ad-hoc scope use `ACTIVITY_END_DEFERRED`
(`activity-end-deferred`) when terminal or when a condition or latched completion decision requires
handling before outgoing flow selection.
Async-after jobs persist that phase, including when the activity has multiple outgoing sequence flows:
completion and outgoing-flow selection have not happened yet. Outgoing activities without a condition
or latch retain the established TAKE path, and compensation handlers retain `endCompensation`.
Migration preserves a deferred operation when relocating the pending work into either an ordinary
or an ad-hoc flow scope. Changing/removing the target completion expression therefore applies when
the job resumes in an ad-hoc scope. Existing
`TRANSITION_NOTIFY_LISTENER_TAKE` jobs have already selected a transition and disposed of the
activity scope; migration does not replay their source activity's completed output or completion
decision. In particular, adding a target completion condition does not retroactively count a source
activity that already reached TAKE without one.

When an already-selected TAKE continuation maps to a terminal target, migration persists
`ACTIVITY_END_DISPOSED` (`activity-end-disposed`) with the selected transition ID. That phase skips
the child's already-performed output, scope destruction and successful-completion bookkeeping;
structural enclosing scopes still finish normally. It has no new end-listener phase because those
listeners already ran. A subsequent migration to a target with outgoing flows restores TAKE under
the existing single-flow or matching-transition-ID policy. A missing match among multiple outgoing
flows is rejected before mutation. The terminal intermediate must not erase selection provenance
or turn the job back into a deferred completion that would rerun output.

Uncaught BPMN errors instead persist `ACTIVITY_END_RETIRE` (`activity-end-retire`), including errors
on terminal activities. Matching end-listener operations preserve the successful/retiring distinction
before async-after scheduling. Retirement performs the normal end and scope cleanup, but does not
select outgoing flows, enable downstream activities or count an ad-hoc success. Migration preserves
this intent across renamed activities, ordinary/ad-hoc relocation, and intermediate terminal targets;
removing outgoing flows and later adding them again does not change retirement into completion.

An uncaught-error report can leave a user-task or external-task entity attached while its async
retirement job waits. The retained-END scope adapter uses the existing task migration observers
to detach and reattach those dependencies to the current execution across reparenting and scope
changes. Task identity, local variables and history binding, or external-task identity, lock and
retry state, follow the target mapping and remain until retirement resumes. Migration does not
restart the work or discard its retained task state merely because it is represented by an END job.

Legacy generic `ACTIVITY_END` (`activity-end`) jobs remain readable. A generic END whose source
activity has outgoing flows is recognized as retirement and migrated to the explicit retirement
operation. A legacy terminal generic END contains no persisted evidence distinguishing successful
completion from an uncaught error. It retains the existing terminal-END target-continuation policy;
migration cannot reconstruct missing error provenance. Newly created terminal-error jobs use the
explicit retirement operation and do not have that ambiguity.

A pending, not-yet-disposed scoped activity-end continuation (for example a task with input/output
mappings) retains its scope-local variables and pending output mapping. Such continuations support
identical/renamed migration and insertion/removal of ordinary flow scopes. The retained activity scope execution is
reattached rather than replaced; carrier-local variables retain their ownership, and existing timer
and event-subscription migration handlers transfer boundary dependencies. The pending output mapping
runs once when the job resumes. A successful continuation then selects outgoing flows, including in
an ordinary target flow scope; retirement skips that selection.
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
| End intent | Deferred terminal success and condition/latch-required completion; established no-condition outgoing TAKE and compensation path; no retroactive source-completion count when a target adds a condition; explicit deferred-end operation retained through ordinary relocation/roundtrip; uncaught-error retirement retained through rename and ordinary/ad-hoc targets with multiple outgoing flows; terminal source and intermediate terminal target; legacy outgoing generic END normalized to retirement; no downstream tasks, enabled tokens or success counts on retirement; output/end-listener cleanup counts preserved; selected TAKE persisted as DISPOSED through terminal targets, immediate cleanup and roundtrip to one/multiple flows; unmatched selected-flow rejection; structural wrapper cleanup without child-output replay |
| Event dependencies | Timer boundary job identity/due date and firing; message boundary subscription identity and post-migration correlation |
| Pending error dependencies | User-task identity, target definition/key and local payload retained before async retirement, then removed by resume cleanup; external-task identity, lock and retries retained through migration until retirement resumes |
| State/history | Process/scope/child variables; input mappings not replayed and target output mappings used; repeated completion IDs and counts; mapped activity references; cancellation-policy changes; latch after target expression removal/change; activity/task history identity; active scope history ID retained while definition/activity ID/name/type update to target; finished child history stays source |
| Atomicity/security | Invalid single-instance ordering leaves tasks/jobs/state intact; invalid later instance rolls back earlier migration in the same command; authorization and tenant rejection preserve source state |
| Suspension | Suspended instance with both running and enabled children across rename and ordinary wrapper insertion; all retained/new executions stay suspended; trigger and explicit completion reject without changing tokens, tasks or variables; resumption completes successfully. Individually suspended and process-suspended scoped async-after jobs retain identity, retries, locals and suspension through wrapper insertion, stay absent from the active/executable job query until activation, then continue |
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
- Legacy terminal generic `activity-end` jobs do not encode whether an uncaught error caused the end.
  Their established migration policy is retained; successful/error intent is guaranteed only where
  the persisted operation or the supported legacy outgoing-flow classification distinguishes it.
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
In particular, run `AdHocActivityEndSemanticsTest` alongside the scoped-activity-end migration suite
to verify ordinary/ad-hoc error retirement, sync/async and I/O-scoped paths, listener errors,
no-condition retirement and cancellation. The matrix describes regression coverage; it does not
replace execution of these tests against the final runtime correction.

### Known validation boundary: ordinary scoped-error cleanup

The new runtime fixtures exposed an existing generic cleanup defect, reproduced separately without
the ad-hoc continuation: after an ordinary scoped activity retires on an uncaught BPMN error, its
output mapping can be evaluated again during process-end cleanup after the child's locals have
been removed. An expression reading such a local can therefore fail. This continuation does not
change that generic cleanup path or claim to fix the defect.

The ordinary scoped-error fixtures in `AdHocActivityEndSemanticsTest` retain an input mapping to
create the activity scope, with no output mapping, to isolate their intended termination and
no-downstream-flow assertions. They do not establish an
output-once guarantee for the affected ordinary error path. Ad-hoc scoped-error fixtures retain
their local-variable expressions, and the dedicated ad-hoc output-count assertions remain unchanged.
Their results must be reported separately from this baseline limitation and from final-suite status.
