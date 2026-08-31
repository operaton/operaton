/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.connect.a2a;

import org.operaton.connect.spi.ConnectorRequest;

/**
 * The input parameters of an {@link A2aConnector} call.
 *
 * <p>
 * Every parameter can be set from BPMN as an {@code operaton:inputParameter}, either as a literal value
 * or as an expression resolving against process variables. Parameters not listed here are ignored.
 * </p>
 *
 * @since 2.2
 */
public interface A2aRequest extends ConnectorRequest<A2aResponse> {

  /** Which A2A call to make: {@link #OPERATION_SEND_SYNC}, {@link #OPERATION_SEND_ASYNC} or {@link #OPERATION_GET_TASK}. */
  String PARAM_OPERATION = "operation";

  /** Send the message and wait for the task to reach a final state. */
  String OPERATION_SEND_SYNC = "sendSync";
  /** Send the message, register a push notification callback and return immediately. */
  String OPERATION_SEND_ASYNC = "sendAsync";
  /** Fetch the current state of a task that is already known. */
  String OPERATION_GET_TASK = "getTask";

  /**
   * The agent location. Both forms are accepted:
   * <ul>
   *   <li>a service base URL, e.g. {@code https://agent.example.com} - the agent card is then read from
   *       {@code https://agent.example.com/.well-known/agent-card.json}</li>
   *   <li>an explicit agent card URL ending in {@code .json}, which is fetched as-is</li>
   * </ul>
   */
  String PARAM_URL = "url";

  /** A {@code Map<String, String>} of HTTP headers sent with every A2A call, e.g. {@code Authorization}. */
  String PARAM_HEADERS = "headers";

  /** The plain text of the message to send. The simple, one-field case. */
  String PARAM_MESSAGE = "message";

  /**
   * Additional message parts, as a {@code List<Map<String, Object>>}. Each entry needs a {@code type} of
   * {@code text}, {@code file} or {@code data}:
   * <ul>
   *   <li>{@code {type: "text", text: "..."}}</li>
   *   <li>{@code {type: "file", uri: "https://...", mimeType: "application/pdf", name: "report.pdf"}}</li>
   *   <li>{@code {type: "file", bytes: "<base64>", mimeType: "...", name: "..."}}</li>
   *   <li>{@code {type: "data", data: {...}}}</li>
   * </ul>
   * A {@code metadata} map may be added to any part.
   */
  String PARAM_PARTS = "parts";

  /** A {@code Map<String, Object>} of metadata attached to the outgoing message. */
  String PARAM_METADATA = "metadata";

  /** A {@code List<String>} of A2A extension URIs the message activates. */
  String PARAM_EXTENSIONS = "extensions";

  /** A {@code List<String>} of task ids the agent should treat as context for this message. */
  String PARAM_REFERENCE_TASK_IDS = "referenceTaskIds";

  /** Continues an existing conversation. Set this to span a multi-turn exchange over several activities. */
  String PARAM_CONTEXT_ID = "contextId";

  /** Continues an existing task, or reattaches to one. Also used as the target of {@link #OPERATION_GET_TASK}. */
  String PARAM_TASK_ID = "taskId";

  /** Overrides the deterministic message id. Rarely needed. */
  String PARAM_MESSAGE_ID = "messageId";

  /** A {@code Map<String, Object>} of metadata attached to the send request rather than to the message. */
  String PARAM_REQUEST_METADATA = "requestMetadata";

  /** The A2A tenant to scope the call to. */
  String PARAM_TENANT = "tenant";

  /** A {@code List<String>} of MIME types the process can accept back, e.g. {@code text/plain}. */
  String PARAM_ACCEPTED_OUTPUT_MODES = "acceptedOutputModes";

  /** How many past messages of the conversation the agent should return. */
  String PARAM_HISTORY_LENGTH = "historyLength";

  /** Where the agent should POST push notifications. Required for {@link #OPERATION_SEND_ASYNC}. */
  String PARAM_CALLBACK_URL = "callbackUrl";

  /** A token the agent echoes back in the push notification, so the receiver can validate it. */
  String PARAM_CALLBACK_TOKEN = "callbackToken";

  /** The authentication scheme the agent should use when calling {@link #PARAM_CALLBACK_URL}. */
  String PARAM_CALLBACK_AUTH_SCHEME = "callbackAuthScheme";

  /** The credentials the agent should present when calling {@link #PARAM_CALLBACK_URL}. */
  String PARAM_CALLBACK_AUTH_CREDENTIALS = "callbackAuthCredentials";

  /** Connect timeout in milliseconds. Defaults to {@value #DEFAULT_CONNECT_TIMEOUT_MS}. */
  String PARAM_CONNECT_TIMEOUT = "connectTimeout";

  /** Read timeout of a single HTTP call in milliseconds. Defaults to {@value #DEFAULT_READ_TIMEOUT_MS}. */
  String PARAM_READ_TIMEOUT = "readTimeout";

  /**
   * Total time in milliseconds {@link #OPERATION_SEND_SYNC} waits for a final task state before raising
   * {@code a2a-timeout}. Defaults to {@value #DEFAULT_WAIT_TIMEOUT_MS}.
   */
  String PARAM_WAIT_TIMEOUT = "waitTimeout";

  /** How often {@link #OPERATION_SEND_SYNC} polls while waiting, in milliseconds. Defaults to {@value #DEFAULT_POLL_INTERVAL_MS}. */
  String PARAM_POLL_INTERVAL = "pollInterval";

  /**
   * Whether to look for a task a previous, rolled back attempt already created before sending again.
   * Defaults to {@code true}. See the module README for what this can and cannot guarantee.
   */
  String PARAM_REATTACH_ON_RETRY = "reattachOnRetry";

  /**
   * A value that is stable across job retries of the same activity and unique per activity instance. It is
   * hashed into the message id and, when no {@link #PARAM_CONTEXT_ID} is given, into the context id.
   * The element template defaults it to {@code ${execution.getProcessInstanceId()}-${execution.getActivityInstanceId()}}.
   */
  String PARAM_IDEMPOTENCY_KEY = "idempotencyKey";

  /**
   * The largest value in bytes that is written into a process variable. Larger file parts are replaced by a
   * reference and larger texts are truncated. Defaults to {@value #DEFAULT_MAX_VARIABLE_SIZE}.
   */
  String PARAM_MAX_VARIABLE_SIZE = "maxVariableSize";

  /** Whether to expose the conversation history as an output. Defaults to {@code false}. */
  String PARAM_INCLUDE_HISTORY = "includeHistory";

  int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
  int DEFAULT_READ_TIMEOUT_MS = 30_000;
  int DEFAULT_WAIT_TIMEOUT_MS = 120_000;
  int DEFAULT_POLL_INTERVAL_MS = 2_000;
  int DEFAULT_MAX_VARIABLE_SIZE = 64 * 1024;

  /**
   * Sets the operation to perform.
   *
   * @param operation one of {@link #OPERATION_SEND_SYNC}, {@link #OPERATION_SEND_ASYNC}, {@link #OPERATION_GET_TASK}
   * @return this request
   */
  A2aRequest operation(String operation);

  /**
   * Sets the agent location.
   *
   * @param url a service base URL or an agent card URL
   * @return this request
   */
  A2aRequest url(String url);

  /**
   * Adds a single HTTP header to send with every A2A call.
   *
   * @param field the header name
   * @param value the header value
   * @return this request
   */
  A2aRequest header(String field, String value);

  /**
   * Sets the plain text of the message to send.
   *
   * @param message the message text
   * @return this request
   */
  A2aRequest message(String message);

  /**
   * Sets the task to continue, reattach to, or query.
   *
   * @param taskId the A2A task id
   * @return this request
   */
  A2aRequest taskId(String taskId);

  /**
   * Sets the conversation to continue.
   *
   * @param contextId the A2A context id
   * @return this request
   */
  A2aRequest contextId(String contextId);

}
