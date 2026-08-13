# Operaton Connect - A2A

Call an AI agent from a BPMN service task over the [Agent2Agent (A2A) protocol](https://a2a-protocol.org/),
without writing Java.

The activity stays a plain `bpmn:serviceTask`. Everything is configured with `operaton:inputParameter` and
`operaton:outputParameter`, so a new agent call is a matter of dragging a task onto the canvas and filling in
fields.

```xml
<serviceTask id="askAgent" name="Ask the agent">
  <extensionElements>
    <operaton:connector>
      <operaton:connectorId>a2a</operaton:connectorId>
      <operaton:inputOutput>
        <operaton:inputParameter name="operation">sendSync</operaton:inputParameter>
        <operaton:inputParameter name="url">${agentUrl}</operaton:inputParameter>
        <operaton:inputParameter name="message">${question}</operaton:inputParameter>
        <operaton:outputParameter name="answer">${text}</operaton:outputParameter>
      </operaton:inputOutput>
    </operaton:connector>
  </extensionElements>
</serviceTask>
```

- Protocol: **A2A v1.0**
- Transport: **JSON-RPC 2.0** only (see [Limitations](#known-limitations))
- Built on the [A2A Java SDK](https://github.com/a2aproject/a2a-java) `org.a2aproject.sdk:a2a-java-sdk-client`

## Installation

The connector is not part of `operaton-connect-connectors-all`, so it has to be added deliberately.

1. Add the connect plugin to the engine, exactly as for the HTTP connector:

   ```xml
   <property name="processEnginePlugins">
     <list>
       <bean class="org.operaton.connect.plugin.impl.ConnectProcessEnginePlugin" />
     </list>
   </property>
   ```

2. Put `operaton-connect-a2a` and its dependencies on the engine classpath:

   ```xml
   <dependency>
     <groupId>org.operaton.connect</groupId>
     <artifactId>operaton-connect-a2a</artifactId>
   </dependency>
   ```

The connector registers itself through `java.util.ServiceLoader`
(`META-INF/services/org.operaton.connect.spi.ConnectorProvider`) under the id **`a2a`**.

### Modeler support

`src/main/resources/element-templates/operaton-a2a-connector.json` is an element template giving a labelled form
for every field below. Copy it into your modeler's `resources/element-templates` directory. It validates against
the published `@camunda/element-templates-json-schema`. Binding types use the `camunda:` prefix because that is
what that schema defines; if your modeler build expects `operaton:`-prefixed binding types, rename them.

## Operations

| `operation` | What it does |
|---|---|
| `sendSync` | Sends the message, then polls until the task reaches a final state. For agents that answer quickly. Bounded by `waitTimeout`. |
| `sendAsync` | Sends the message and returns immediately with the task id, context id and state. With a `callbackUrl` the agent is asked to push a notification; without one, poll with `getTask`. |
| `getTask` | Reads the current state of a task you already know the id of. |

`sendSync` polls rather than holding a server-sent-event stream open, because a stream would pin a job executor
thread for as long as the agent takes to think. The cost is up to one `pollInterval` of extra latency.

## Inputs

Every input can be a literal or an expression such as `${someVariable}`. Numbers and booleans may be written as
strings, so `3000` and `"3000"` both work.

### Agent

| Input | Meaning |
|---|---|
| `operation` | **Required.** `sendSync`, `sendAsync` or `getTask`. |
| `url` | **Required.** Either a service base URL such as `https://agent.example.com`, in which case the agent card is read from `https://agent.example.com/.well-known/agent-card.json`, or a full agent card URL ending in `.json`, which is fetched as given. |
| `headers` | A map of HTTP headers sent with every A2A call, including the agent card fetch. This is where `Authorization` goes. |

### Message

| Input | Meaning |
|---|---|
| `message` | The plain text to send. The easy, one-field case. |
| `parts` | A list of maps for anything beyond plain text. See [Parts](#parts). |
| `metadata` | A map attached to the outgoing **message**. |
| `extensions` | A2A extension URIs the message activates, as a list or a comma-separated string. |
| `referenceTaskIds` | Earlier task ids the agent should treat as context. |
| `requestMetadata` | A map attached to the send **request** rather than to the message. |
| `tenant` | The A2A tenant to scope the call to. |
| `acceptedOutputModes` | MIME types you can handle back, e.g. `text/plain, application/json`. |
| `historyLength` | How many earlier messages the agent should return. |

### Conversation

| Input | Meaning |
|---|---|
| `contextId` | Continues an existing conversation. Pass `${a2aContextId}` from an earlier agent task to make a multi-turn exchange span several activities. |
| `taskId` | Required for `getTask`. On a send operation, appends the message to that existing task. |

### Push notification (`sendAsync`)

| Input | Meaning |
|---|---|
| `callbackUrl` | Where the agent POSTs the notification. Omit it to use the polling fallback instead. |
| `callbackToken` | A token the agent echoes back, so your webhook can tell a real notification from a forged one. |
| `callbackAuthScheme` | e.g. `Bearer`. |
| `callbackAuthCredentials` | The credential the agent presents to your webhook. |

### Timeouts

Nothing is unbounded.

| Input | Default | Meaning |
|---|---|---|
| `connectTimeout` | `10000` | TCP connect timeout in ms. |
| `readTimeout` | `30000` | Read timeout of a single HTTP call in ms. |
| `waitTimeout` | `120000` | Total time `sendSync` waits for a final state before raising `a2a-timeout`. |
| `pollInterval` | `2000` | How often `sendSync` polls while waiting. |

### Reliability

| Input | Default | Meaning |
|---|---|---|
| `idempotencyKey` | none | A value stable across job retries and unique per activity instance. The element template defaults it to `${execution.getProcessInstanceId()}-${execution.getActivityInstanceId()}`. |
| `reattachOnRetry` | `true` | Look for a task an earlier failed attempt already created before sending again. |
| `maxVariableSize` | `65536` | The largest value written into a process variable. |
| `messageId` | derived | Overrides the derived message id. Rarely needed. |
| `includeHistory` | `false` | Whether to expose the conversation history as an output. |

### Parts

`parts` is a list of maps, each with a `type`. Any part may also carry a `metadata` map.

```xml
<operaton:inputParameter name="parts">
  <operaton:list>
    <operaton:map>
      <operaton:entry key="type">file</operaton:entry>
      <operaton:entry key="uri">${documentUrl}</operaton:entry>
      <operaton:entry key="mimeType">application/pdf</operaton:entry>
      <operaton:entry key="name">invoice.pdf</operaton:entry>
    </operaton:map>
  </operaton:list>
</operaton:inputParameter>
```

| `type` | Keys |
|---|---|
| `text` | `text` |
| `file` | either `uri`, or inline `bytes` as base64, plus optional `mimeType` and `name` |
| `data` | `data`, any structured value |

A2A has no input-side artifacts: artifacts are produced by the agent, and `parts` is the outgoing counterpart.

## Outputs

Map any of these with `<operaton:outputParameter name="yourVariable">${output}</operaton:outputParameter>`.

| Output | Meaning |
|---|---|
| `text` | The agent's answer as plain text, and the usual one to map. Taken from the text parts of the final status message; when the agent closes the task without one and answers only with artifacts, which is common (google-adk agents do it), it falls back to the artifact text so the one-field case stays one field. |
| `statusMessage` | The full final message as a map, including all parts. |
| `artifacts` | **All** artifacts the agent produced, as a list of maps, each with its `parts`. |
| `artifactText` | The text parts of all artifacts joined by newlines. |
| `task` | The whole task as a map: `id`, `contextId`, `status`, `artifacts`, `metadata`. |
| `taskId` | The A2A task id. Needed to correlate a push notification. |
| `contextId` | The A2A context id. Pass to a later activity to continue the conversation. |
| `state` | One of `TASK_STATE_SUBMITTED`, `TASK_STATE_WORKING`, `TASK_STATE_INPUT_REQUIRED`, `TASK_STATE_AUTH_REQUIRED`, `TASK_STATE_COMPLETED`, `TASK_STATE_CANCELED`, `TASK_STATE_FAILED`, `TASK_STATE_REJECTED`. |
| `taskMetadata` | Metadata the agent returned on the task. |
| `messageMetadata` | Metadata the agent returned on the final message. |
| `history` | The conversation history, only when `includeHistory` is `true`. |
| `truncated` | `true` when a value was too large for a process variable. See [Large payloads](#large-payloads). |

## Errors

| Error code | When |
|---|---|
| `a2a-task-failed` | The task ended in `TASK_STATE_FAILED`. |
| `a2a-task-rejected` | The task ended in `TASK_STATE_REJECTED`. |
| `a2a-task-canceled` | The task ended in `TASK_STATE_CANCELED`. |
| `a2a-auth-required` | The agent needs credentials it was not given. |
| `a2a-timeout` | `sendSync` gave up after `waitTimeout`. |
| `a2a-protocol-error` | A permanent protocol or transport failure, such as HTTP 4xx or a malformed response. |

Catch them with an error boundary event:

```xml
<error id="a2aTaskFailedError" name="A2A task failed" errorCode="a2a-task-failed" />
...
<boundaryEvent id="agentFailed" attachedToRef="askAgent">
  <errorEventDefinition errorRef="a2aTaskFailedError" />
</boundaryEvent>
```

**Retryable versus terminal.** Connection resets, read timeouts, HTTP 408, 425, 429 and 5xx are left as a
`ConnectorRequestException`, so the job executor retries and eventually raises an incident. Only the codes above
become a `BpmnError`.

**Output parameters are not mapped when an error is thrown.** The task id is therefore included in the error
message, so a process can still find the task with `getTask`.

**`TASK_STATE_INPUT_REQUIRED` is deliberately not an error.** The activity completes normally with all outputs
mapped, so you can branch on `${a2aState}` with a gateway and send the follow-up from a second agent task,
passing the same `contextId` and `taskId`.

## Webhook correlation for `sendAsync`

The connector does not receive push notifications; that endpoint is yours. What it guarantees is that everything
needed to correlate is committed as a process variable before the process reaches its wait state.

1. Model `sendAsync` with `callbackUrl` pointing at your endpoint, and map `taskId` to a variable such as
   `a2aTaskId`.
2. Let the process wait at a receive task.
3. In your endpoint, verify the `callbackToken`, then correlate:

   ```java
   runtimeService.createMessageCorrelation("a2aCallback")
                 .processInstanceVariableEquals("a2aTaskId", taskIdFromCallback)
                 .correlateWithResult();
   ```

4. Attach a timer boundary event to the receive task as the backstop for a notification that never arrives.

**Handle the early callback.** A fast agent can call your webhook before the process instance has committed and
reached the receive task. Correlation then finds nothing. Your endpoint must treat "no matching execution" as a
retryable condition and try again with a short backoff, rather than dropping the notification.

See `src/test/resources/org/operaton/connect/a2a/examples/a2a-send-async.bpmn`.

## Large payloads

Process variables live in `ACT_RU_VARIABLE` and are copied into `ACT_HI_DETAIL`. A multi-megabyte base64 blob
written there is copied on every update, bloats the history tables and slows down every query that touches the
instance. So:

- **Returned files.** A file that arrives as a URI is passed through as a URI, which costs nothing. A file that
  arrives inline is only passed through while its base64 is at most `maxVariableSize`. Above that, the part keeps
  its `mimeType`, `name` and `sizeBytes` and gains `truncated: true`, but the bytes are dropped, and the
  top-level `truncated` output becomes `true`.
- **Returned text.** Text longer than `maxVariableSize` is cut short with a marker.
- **Sent files.** An inline `bytes` part larger than `maxVariableSize` is rejected outright rather than silently
  truncated, because sending half a file is worse than failing.

**The trade-off.** The alternative would be to store large payloads somewhere else automatically, which means
picking a blob store and owning its lifecycle, retention and cleanup, and giving the engine a new external
dependency. That decision belongs to the process application, not to a connector. So the connector refuses to
put the payload in the database and tells you it did, and the intended handling is to have the agent return a
URI, or to fetch the bytes yourself from a delegate or external task when you need them.

If you genuinely want inline bytes in a variable, raise `maxVariableSize` deliberately and size your database
accordingly.

## Idempotency, and what it cannot promise

Operaton retries failed jobs, and a duplicate agent run costs money and may have side effects. Two things make
this less likely:

1. **A deterministic message id**, derived from `idempotencyKey`. An agent that deduplicates on `messageId` will
   not run the work twice.
2. **A reattach probe.** When no `contextId` is supplied, one is derived from `idempotencyKey`, which means the
   context holds exactly one task per activity instance. Before sending, the connector calls `ListTasks` for that
   context and, if it finds a task, reattaches with `GetTask` instead of sending again. Only a genuine transport
   failure during the probe (an `IOException`, or HTTP 408/425/429/5xx) fails the job; anything else degrades to a
   normal send.

   **`ListTasks` is optional in A2A and many agents do not implement it**, answering `-32601 Method not found`.
   That is expected and costs you only the extra round trip, but it does mean the probe is no protection on such
   an agent, and the deterministic `messageId` is all that is left. Verified against a google-adk agent, which
   does not implement `ListTasks` but does honour a client-supplied `contextId`.

**Be aware of the honest limits.** A2A does not let a client choose the task id: `MessageSendParams` has no field
for it, and the agent assigns `Task.id` when it creates the task. A `Message.taskId` refers to a task that
already exists. Combined with the fact that an Operaton job retry rolls back the transaction, and with it any
variable the previous attempt wrote, the connector cannot simply remember the task id it got last time. So
exactly-once cannot be guaranteed by the client alone. For an expensive agent, prefer:

- `sendAsync` plus a receive task, so the send is committed the moment it succeeds, or
- `operaton:failedJobRetryTimeCycle="R0/PT0S"` on the service task, so a failed send never silently re-dispatches
  and instead raises an incident for a human.

Set `reattachOnRetry` to `false` to skip the probe and its extra round trip.

## Security

- Header values are resolvable from process variables or engine configuration, so no secret has to sit in the
  diagram.
- Header **values** and request bodies are never logged. Header **names** are logged at debug level, so a missing
  `Authorization` can be diagnosed without leaking the token.
- Validate the `callbackToken` in your webhook before correlating.

## Known limitations

- **JSON-RPC only.** gRPC and HTTP+JSON are not registered as transports, and the client's transport preference
  is set to win over the agent card, so an agent that only speaks gRPC is not supported.
- **No streaming.** `SendStreamingMessage` and `SubscribeToTask` are not used, and the HTTP client's
  server-sent-event methods throw `UnsupportedOperationException`. `sendSync` polls instead. Incremental artifact
  chunks are therefore not observed; the connector reads the assembled task.
- **Not in `connectors-all`.** That artifact shades its dependencies and relocates `org.apache`, which is not a
  safe thing to do to protobuf. Add this module explicitly.
- **No WildFly distro module.** Wiring this into `distro/wildfly` means shipping protobuf, Guava and
  `proto-google-common-protos` as JBoss modules, which is a decision for the distribution, not for this module.
  On WildFly, deploy the connector with your process application for now.
- **Agent card caching.** Clients are cached per agent URL, headers and timeouts, up to 64 entries, after which
  the cache is emptied wholesale. A card change is not noticed until the cache is cleared or the engine restarts.
- **`maxVariableSize` counts characters** for text, not UTF-8 bytes, so a multi-byte string can exceed the limit
  in bytes by up to a factor of four.

## Examples

Under `src/test/resources/org/operaton/connect/a2a/examples/`:

| File | Shows |
|---|---|
| `a2a-send-sync.bpmn` | `sendSync` with an error boundary event, headers from variables, and metadata |
| `a2a-send-async.bpmn` | `sendAsync` with a push notification, a receive task, a file part by reference, and a timer boundary event |
| `a2a-poll-fallback.bpmn` | `sendAsync` without a callback, then `getTask` on a timer loop, for agents without `capabilities.pushNotifications` |

## Design

The A2A SDK is used behind `A2aAgent`, a small interface whose methods only take and return strings, maps and
lists. `SdkA2aAgent` is the only class that imports `org.a2aproject.sdk`, so an SDK or protocol bump touches it
and `TimeoutAwareA2AHttpClient` and nothing else.

`TimeoutAwareA2AHttpClient` exists because the SDK's own `JdkA2AHttpClient` never sets a per-request timeout, and
neither `A2AHttpClient` nor its builders expose one. Without it an unresponsive agent would hold a job executor
thread until the OS gave up on the socket.
