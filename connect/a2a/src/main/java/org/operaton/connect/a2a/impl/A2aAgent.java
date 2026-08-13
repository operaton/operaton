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
package org.operaton.connect.a2a.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the connector needs from an A2A agent, expressed without any reference to the A2A SDK.
 *
 * <p>
 * This is the seam that keeps a protocol or SDK version bump contained: {@link SdkA2aAgent} is the only
 * implementation and the only place that imports {@code org.a2aproject.sdk}. Everything on this interface is
 * either a {@code String}, a number, or a plain {@code Map}/{@code List} that can be written straight into a
 * process variable.
 * </p>
 */
public interface A2aAgent extends AutoCloseable {

  /**
   * How to reach an agent. Two agents are considered the same, and share a cached client, when their configs
   * are equal, so this record deliberately has value semantics.
   *
   * @param url the agent base URL or agent card URL
   * @param headers HTTP headers to send with every call; values are secret and never logged
   * @param connectTimeoutMs connect timeout in milliseconds
   * @param readTimeoutMs read timeout of a single HTTP call in milliseconds
   * @param maxVariableSize largest value in bytes to put into a process variable
   */
  record Config(String url,
                Map<String, String> headers,
                int connectTimeoutMs,
                int readTimeoutMs,
                int maxVariableSize) {
  }

  /**
   * A message to send to an agent.
   *
   * @param messageId the client-chosen message id; deterministic so that an agent can deduplicate a retry
   * @param contextId the conversation to continue, or {@code null} to let the agent start one
   * @param taskId the task to continue, or {@code null} to let the agent create one
   * @param text the plain text of the message, or {@code null}
   * @param parts additional parts, each a map with a {@code type} of {@code text}, {@code file} or {@code data}
   * @param metadata metadata to attach to the message
   * @param extensions A2A extension URIs the message activates
   * @param referenceTaskIds tasks the agent should treat as context
   * @param requestMetadata metadata to attach to the send request rather than to the message
   * @param tenant the tenant to scope the call to, or {@code null}
   * @param acceptedOutputModes MIME types the caller can accept back
   * @param historyLength how much conversation history to return, or {@code null}
   * @param callbackUrl where the agent should POST push notifications, or {@code null} for none
   * @param callbackToken a token the agent echoes back in the push notification
   * @param callbackAuthScheme the authentication scheme for the callback
   * @param callbackAuthCredentials the credentials for the callback
   * @param returnImmediately whether the agent may return before the task reaches a final state
   */
  record SendCommand(String messageId,
                     String contextId,
                     String taskId,
                     String text,
                     List<Map<String, Object>> parts,
                     Map<String, Object> metadata,
                     List<String> extensions,
                     List<String> referenceTaskIds,
                     Map<String, Object> requestMetadata,
                     String tenant,
                     List<String> acceptedOutputModes,
                     Integer historyLength,
                     String callbackUrl,
                     String callbackToken,
                     String callbackAuthScheme,
                     String callbackAuthCredentials,
                     boolean returnImmediately) {
  }

  /**
   * The state of an A2A task, already flattened into values that can be written to process variables.
   *
   * @param taskId the task id, or {@code null} when the agent answered with a bare message
   * @param contextId the context id, or {@code null}
   * @param state the {@code TASK_STATE_*} name, or {@code null} when the agent answered with a bare message
   * @param statusMessage the final status message including its parts, or {@code null}
   * @param artifacts every artifact the agent produced, never {@code null}
   * @param taskMetadata metadata on the task, never {@code null}
   * @param history the conversation history, never {@code null}
   * @param truncated whether any value was truncated or replaced by a reference to stay within
   *        {@link Config#maxVariableSize()}
   */
  record TaskSnapshot(String taskId,
                      String contextId,
                      String state,
                      Map<String, Object> statusMessage,
                      List<Map<String, Object>> artifacts,
                      Map<String, Object> taskMetadata,
                      List<Map<String, Object>> history,
                      boolean truncated) {
  }

  /**
   * Sends a message to the agent.
   *
   * @param command what to send
   * @return the task as it stands when the agent responds, which may not be a final state
   * @throws A2aCallException if the call fails
   */
  TaskSnapshot send(SendCommand command);

  /**
   * Fetches the current state of a task.
   *
   * @param taskId the task to fetch
   * @param historyLength how much conversation history to return, or {@code null} for the agent's default
   * @return the current state of the task
   * @throws A2aCallException if the call fails, including when the agent does not know the task
   */
  TaskSnapshot getTask(String taskId, Integer historyLength);

  /**
   * Looks for the most recent task in a context, so that a retry can reattach to a task an earlier attempt
   * already created instead of dispatching a second one.
   *
   * @param contextId the context to search
   * @return the id of the most recent task in that context, or empty if there is none or if the agent does not
   *         implement {@code ListTasks}
   */
  Optional<String> findLatestTaskId(String contextId);

  @Override
  void close();

}
