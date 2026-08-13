---
status: "proposed"
date: 2026-08-13
decision-makers: Operaton maintainers
consulted: —
informed: Operaton contributors
---

# A2A connector is a Connect SPI connector built on the A2A Java SDK

## Context and Problem Statement

Processes increasingly need to delegate a step to an AI agent. The
[Agent2Agent (A2A) protocol](https://a2a-protocol.org/) v1.0 is the emerging open standard for that, and Operaton
has no way to call an A2A agent today.

Two questions have to be answered together. Where does such a call live in Operaton, and do we implement the
protocol ourselves or take a dependency on the official
[A2A Java SDK](https://github.com/a2aproject/a2a-java)? The second question matters because the SDK's transitive
closure lands new third-party jars on the engine classpath, and AGENTS.md requires an ADR for major dependencies.

## Decision Drivers

* A modeller who is not a Java developer must be able to add an agent call by dragging a task and filling in
  fields.
* The BPMN must stay valid and portable; the activity should remain a plain `bpmn:serviceTask`.
* A2A is more than a request/response call: it has task lifecycle states, artifact streaming with chunk
  reassembly, push-notification configuration, stream resubscription, and transport negotiation off the agent
  card.
* The engine classpath is shared by every process application, so new dependencies are expensive.
* A protocol version bump should touch as few files as possible; A2A is young and still moving.
* Nothing running on a job executor thread may block without a bound.

## Considered Options

* Connect SPI connector using the A2A Java SDK
* Connect SPI connector implementing the A2A JSON-RPC calls directly on `httpclient5` and Jackson
* An external task worker shipped outside the engine
* A new BPMN element type with a custom `ActivityBehavior`

## Decision Outcome

Chosen option: **Connect SPI connector using the A2A Java SDK**, as a new opt-in module `connect/a2a`
(`org.operaton.connect:operaton-connect-a2a`) registered under connector id `a2a`.

A Connect connector gets declarative input/output mapping in the properties panel for free, runs inside the
engine so there is no extra service to operate, and leaves the diagram portable.

The SDK is used because the protocol surface that has to be right is much larger than the four JSON-RPC calls it
looks like from the outside. In particular `ClientConfig` owns the streaming-versus-polling decision,
`ClientTaskManager` reassembles `TaskArtifactUpdateEvent` chunks (`append` / `lastChunk`), `Client.subscribeToTask`
handles resubscription after a dropped stream, and `ClientBuilder.findBestClientTransport()` negotiates transport
from the card's `supportedInterfaces` and `preferredTransport`. Reimplementing that would be reimplementing a
moving specification.

To contain the dependency, all SDK usage sits behind `A2aAgent`, an interface whose methods take and return only
strings, maps and lists. `SdkA2aAgent` is the only class importing `org.a2aproject.sdk`.

### Consequences

* Good, because a new agent call needs no Java at all: an element template plus input/output parameters.
* Good, because task lifecycle, artifact assembly and push-notification configuration are handled by the
  reference implementation rather than by us.
* Good, because HTTP goes through the SDK's `JdkA2AHttpClient` abstraction over `java.net.http.HttpClient`, so
  there is **no Netty and no Jackson conflict, and no shading was needed**.
* Good, because a protocol bump touches two files.
* Bad, because the transitive closure adds `protobuf-java`, `protobuf-java-util` (and with it Guava) and
  `proto-google-common-protos` to the classpath of anyone using the connector. `a2a-java-sdk-client-transport-jsonrpc`
  compile-depends on `a2a-java-sdk-spec-grpc` even when only JSON-RPC is used, so these cannot be excluded without
  patching the SDK. `gson` is already managed by Operaton, and `jakarta.enterprise.cdi-api` and
  `jakarta.inject-api` already come from the Jakarta EE BOM.
* Bad, because the module needs `operaton-engine` at `provided` scope in order to throw `BpmnError`, which is a
  layering wrinkle for a `connect/*` module. It creates no reactor cycle: `engine` only reaches `connect` through
  `operaton-connect-connectors-all` under the non-default `check-plugins` profile, and this module is deliberately
  not part of `connectors-all`.
* Bad, because 1.2.0.Final's own release notes mention resolving split packages across modules, i.e. the SDK's
  module layout is not settled yet.
* Neutral, because the connector is opt-in. It is not in `connectors-all` (which shades and relocates
  `org.apache`, unsafe for protobuf) and has no WildFly distro module yet, so nobody pays for these dependencies
  unless they add the module.

### Confirmation

* Unit tests cover request building, response mapping, error classification and the reattach and timeout paths
  against a scripted `A2aAgent`.
* Engine-level tests run the three example processes on an in-memory engine and assert that a `BpmnError` reaches
  an error boundary event, that outputs land in process variables, and that the async and polling shapes behave as
  modelled.
* `A2aAgent` having no SDK types in its signatures is what keeps the seam honest, and is visible in review.

## Pros and Cons of the Options

### Connect SPI connector implementing JSON-RPC directly

Four calls (`SendMessage`, `GetTask`, `ListTasks`, `CreateTaskPushNotificationConfig`) over `httpclient5` and
Jackson, both already managed by Operaton.

* Good, because zero new dependencies and no shading question at all.
* Good, because full control over timeouts and logging.
* Bad, because it reimplements artifact chunk reassembly, streaming and resubscription, task lifecycle handling
  and transport negotiation, all of which the SDK already does.
* Bad, because every A2A revision becomes our maintenance problem, and v1.0 already renamed the JSON-RPC methods
  from the `message/send` style to `SendMessage`.

### External task worker outside the engine

* Good, because the engine classpath stays untouched.
* Bad, because it is another service to deploy, monitor and secure.
* Bad, because the modeller loses the properties-panel input/output mapping and needs a worker deployment for
  every new agent call, which defeats the main requirement.

### New BPMN element type with a custom ActivityBehavior

* Good, because agent calls could be first-class in the palette.
* Bad, because it makes the BPMN non-portable and commits the engine to a modelling concept while A2A is still
  changing.
* Bad, because it is a far larger change than a connector for the same user-visible result.
