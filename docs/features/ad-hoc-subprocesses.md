# Ad-hoc subprocess lifecycle

Ad-hoc subprocesses retain an addressable scope while idle, running or draining. The
runtime API uses that scope execution ID for discovery, activation and explicit completion.
The existing `Parallel` and `Sequential` ordering modes, repeated parallel activation,
`activeTasksCollection` initial activation, and `operaton:autoComplete` extension remain supported.

## Activation

Initially, only direct eligible activities with no incoming flow can be selected. A nested
subprocess is selected as a whole; arbitrary descendants cannot be started through the outer
scope. Migration can place an already enabled token inside an ordinary wrapper; that existing
token remains discoverable and selectable through its owning ad-hoc scope. Multi-instance activities
are exposed using their BPMN activity ID and activated through their generated loop body.
The generated `#multiInstanceBody` ID is not a public activation target.

A batch is validated before it mutates the instance. All requested child executions are
reserved before any synchronous child runs. This prevents the first synchronous completion
from prematurely closing the scope or starting later batch members outside it. A completion
condition can cancel reserved members before they start. Validation and execution use the
normal command transaction: database changes roll back on failure. This does not compensate
external side effects already performed by application delegates. Future asynchronous
continuations are validated and executed in their own commands.

Explicit activation in Sequential ordering allows one open direct activity at a time. A subprocess or multi-instance
body counts as one direct activity, even if it contains multiple running descendants. Async
continuations remain open work until their completion has been processed.

## Enabled activities and sequence flows

An incoming sequence-flow token enables its target Activity without starting it. The enabled
execution is persisted using the normal execution/variable tables; no schema change is needed.
Discovery lists both reusable no-incoming-flow starters and currently enabled activities.
`AdHocActivity.isStarterActivity()` distinguishes starters and `getEnabledExecutionIds()` exposes
every waiting token, including multiple arrivals for the same BPMN activity. Activation consumes
one eligible waiting token per requested activity, in deterministic execution-ID order. Duplicate activity
IDs in one request remain rejected; repeated calls can consume additional tokens in Parallel mode.
These IDs identify current engine executions, not historical activation generations; an execution
can be reused by a loop, so clients should rediscover current readiness after each operation.

This applies to both Parallel and Sequential ordering. Sequential allows multiple enabled
activities, but permits only one selected direct Activity to run. Parallel permits several
selected Activities. Gateways and intermediate events route tokens normally, including joins
and loops; they do not consume an Activity slot. An ordinary subprocess or multi-instance body
is one outer Activity, regardless of its internal concurrency. A migrated ordinary wrapper with
only enabled work does not consume a running slot until that work is selected.

Enabled activities have no started activity instance, task, input mapping or async-before job.
These are created only by activation. Enabled executions survive engine restart and migration;
renamed mappings update their target while retaining token identity and local variables.

This intentionally changes the draft PR's automatic internal-flow continuation. Callers must
perform discovery and activate downstream activities explicitly. Existing method signatures,
initial activation collections, and repeatable root activation are retained.

## Completion

After an inner activity completes, the completion condition observes its completed state.
Outgoing flow selection follows that decision. End listeners, output mappings, activity history
and async-after boundaries use the normal PVM lifecycle exactly once. Gateway and intermediate
event routing do not count as additional activity completions.

Successful completion and error retirement are distinct continuations. Successful non-compensation
Activities inside an ad-hoc scope use `activity-end-deferred` when terminal, or when a condition or
latched decision must be handled before outgoing flow selection. Outgoing activities with no condition and no latch
retain the established transition-TAKE path; compensation handlers retain their compensation-end
path. A migrated already-selected TAKE does not retroactively count its source activity when the
target adds a completion condition.

Uncaught BPMN errors use `activity-end-retire`, including errors on terminal activities.
Retirement follows normal end-listener and scope-cleanup behavior, but does not select outgoing
flows, enable downstream work or increment ad-hoc successful-completion
counts. An async-after boundary persists this distinction, so resuming a job or migrating it does
not turn an error into successful completion. The existing option to throw after an unhandled
BPMN error remains applicable.

Once a condition is satisfied, completion remains latched. Discovery returns no new activations
and trigger requests are rejected. With `cancelRemainingInstances=true`, remaining work is
canceled and its execution/job/variable state removed. With `false`, already running activities
drain without starting subsequent inner activities. Enabled work is discarded, including
pending-only migrated wrapper scopes and their dependent state. The scope leaves when that work has ended.

Without a completion condition, the existing auto-completion behavior applies: the scope
completes after started work finishes and no open children remain. Setting
`operaton:autoComplete="false"` keeps it open for explicit completion. Explicit completion
continues to reject active children when cancellation is disabled; it does not silently switch
to a deferred-completion request.

Direct start and end events are rejected during parsing. Nested ordinary/event subprocesses
continue to use their own event rules. These constraints and the completion/order model follow
[BPMN 2.0.2, sections 10.3.5 and 13.3.5](https://www.omg.org/spec/BPMN/2.0.2/PDF).

## Completion context

When a completion condition is configured, its scope-local context includes completed, active
and enabled activity IDs and counts. Completion IDs retain repeated completions; enabled IDs
retain multiple pending copies. A completed ID without a migration mapping remains a historical
source ID. The completion decision is evaluated after the completing Activity's end lifecycle
and before taking its outgoing flows, so tokens produced by those flows are not yet enabled at
that decision point.

The `adHoc*` and `nrOf*AdHocActivities` context variables, including the per-execution
`adHocEnabledActivity` marker and `adHocRetiredContext` migration marker, are engine-owned
state. Applications must not overwrite them or manufacture marker values.
Business data and expressions are not automatically rewritten when activities are renamed.

## Migration and attribution

See [the migration ADR and regression matrix](../decisions/0003-ad-hoc-subprocess-migration.md)
for supported transformations, preserved state, and explicit restrictions. Migration does not
restart initial activities or reevaluate completion during the migration transaction.

Live running or routing scopes can convert between ordinary and ad-hoc subprocesses, subject
to target ordering. Returning a preserved ordinary scope to ad-hoc resumes its retired
completion context; completions during the ordinary interval are not counted as ad-hoc
completions. Enabled, idle and completion-latched owners require a mapped ad-hoc owner.
Enabled tokens may move through ordinary wrappers but retain their activation owner.
Pending async-after activity ends preserve variables and apply the target output mapping
once; variable-name collisions that would lose a value are rejected before mutation.
Migration also preserves whether that pending end represents successful completion or error
retirement. Only the successful continuation can select new target outgoing flows. Legacy generic
terminal-END jobs have the compatibility boundary described in the migration ADR because they did
not persist that distinction.
Tasks or external tasks still attached while an async error-retirement job waits migrate with their
identity, local state and applicable lock/retry state intact until the retirement job cleans them up.

If outgoing flow selection and activity-scope cleanup already happened, migration to a terminal
target records `activity-end-disposed`. Resuming it does not repeat child output or successful
completion, while enclosing structural scopes still finish normally. A later migration to a target
with outgoing flows uses the existing single-flow or matching selected-flow rule; it does not
reevaluate the completed activity's flow conditions.

See [source provenance](ad-hoc-provenance.md) for the preserved original import and source
comparisons used during this continuation.
The migration ADR also records a separately reproduced ordinary scoped-error output-cleanup
limitation and the corresponding targeted-test boundary; this change does not repair that
preexisting generic cleanup path.
