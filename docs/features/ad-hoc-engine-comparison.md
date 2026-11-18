# Ad-hoc engine comparison

Research date: 2026-10-09. This is a documentation/source comparison, not a comparative
runtime benchmark. No Zeebe or Flowable implementation code was copied.

Versions inspected: [Zeebe/Camunda 8.10.2](https://github.com/camunda/camunda/releases/tag/8.10.2)
(released 2026-10-07), with 8.8/8.9 checks for feature introduction;
[Flowable OSS 8.0.0](https://github.com/flowable/flowable-engine/releases/tag/flowable-8.0.0)
(released 2026-02-27). Flowable enterprise product version numbers are separate.

| Concern | Zeebe | Flowable OSS | This Operaton continuation |
| --- | --- | --- | --- |
| Activation | Element-ID batches, expressions and controlling job-worker mode | Java discovery/individual-activation APIs | Existing discovery, initial collection and explicit batches retained |
| Repetition | Repetition and duplicate IDs in one batch supported | Repeated Parallel activation allowed | Repetition across calls retained; duplicate IDs within one batch remain rejected |
| Sequential ordering | No separate AdHoc ordering scheduler found in inspected model/transformer | Rejects another API activation while a child exists | Persistent enabled tokens; explicit selection in both modes; one running outer Activity in Sequential |
| Completion unit | Generated inner instance covers an activated internal flow | Evaluates during outgoing-flow handling; can continue downstream while draining | Direct BPMN Activity completes before further inner Activity starts; nested subprocess/MI is one outer Activity |
| Manual completion | Worker instructions/API mode | Requires no running children | Existing cancel-or-reject behavior according to cancelRemainingInstances retained |
| Migration | Since 8.9: active scopes supported, with preserved scope topology | Generic process migration exists; no dedicated AdHoc contract verified in inspected source/tests | Explicit state-preserving implementation and regression matrix, including permitted ordinary-scope topology changes |

## Evidence and important distinctions

The [Zeebe transformer](https://github.com/camunda/camunda/blob/8.10.2/zeebe/engine/src/main/java/io/camunda/zeebe/engine/processing/deployment/model/transformer/AdHocSubProcessTransformer.java)
does not read BPMN AdHoc ordering; the executable model has no corresponding scheduling
state. This is a source-derived conclusion. Sequential multi-instance support is a separate
feature and must not be described as AdHoc Sequential ordering support.

Zeebe's [inner-instance processor](https://github.com/camunda/camunda/blob/8.9.0/zeebe/engine/src/main/java/io/camunda/zeebe/engine/processing/bpmn/container/AdHocSubProcessInnerInstanceProcessor.java)
provides a scope around each activation's full flow. Its
[activation regression](https://github.com/camunda/camunda/blob/8.9.0/zeebe/engine/src/test/java/io/camunda/zeebe/engine/processing/adhocsubprocess/ActivateAdHocSubProcessActivityTest.java#L308-L335)
explicitly covers duplicate IDs. Worker-driven completion stores a flag; source inspection of
BPMN-expression completion does not establish the same persistent true-to-false latch contract
as Operaton. See the [current processor](https://github.com/camunda/camunda/blob/8.10.2/zeebe/engine/src/main/java/io/camunda/zeebe/engine/processing/bpmn/container/AdHocSubProcessProcessor.java).

Flowable's [activation command](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-engine/src/main/java/org/flowable/engine/impl/cmd/ExecuteActivityForAdhocSubProcessCmd.java)
checks scope, direct no-incoming-flow eligibility and current Sequential concurrency before
creating an execution. This does not prove model-wide fan-out validation. Its
[completion operation](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-engine/src/main/java/org/flowable/engine/impl/agenda/TakeOutgoingSequenceFlowsOperation.java#L293-L325)
and [draining regression](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-engine/src/test/java/org/flowable/engine/test/bpmn/subprocess/adhoc/AdhocSubProcessTest.java#L384-L442)
show a downstream task continuing after the condition becomes true with cancellation disabled.
Its [manual completion command](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-engine/src/main/java/org/flowable/engine/impl/cmd/CompleteAdhocSubProcessCmd.java)
rejects remaining children. These behaviors are comparison points, not automatic requirements
for Operaton.

## Validation layers

[BPMN 2.0.2 sections 10.3.5 and 13.3.5](https://www.omg.org/spec/BPMN/2.0.2/PDF)
are the independent semantic reference. Multiple tokens can enable multiple activities even
in Sequential ordering; enabled activities are not necessarily running activities. A performer
can select them one at a time. Engine implementations differ; agreement between
engines does not supersede the standard.

A sound validation design distinguishes:

1. Deployment-time checks for statically invalid structure.
2. Command preflight for known scope, target IDs, current concurrency and batch validity.
3. Runtime checks for paths determined by future variables, events or async work.

A database transaction can roll back engine state within that transaction. It cannot retract an
external service call already made, or undo an earlier committed async transaction. Therefore
“reject the command atomically” must not be presented as a guarantee against every future path
or external side effect. The approved Operaton design persists enabled activities and requires selection in both ordering
modes. Internal fan-out can therefore enable multiple choices without starting them together.
This implementation is independently grounded in BPMN; Zeebe source, tests and comments are
not copied, adapted or translated.
