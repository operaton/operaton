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

import java.util.List;
import java.util.Map;

import org.operaton.connect.spi.ConnectorResponse;

/**
 * The output parameters of an {@link A2aConnector} call. Each one can be mapped to a process variable with an
 * {@code operaton:outputParameter}, for example {@code <operaton:outputParameter name="answer">${text}</operaton:outputParameter>}.
 *
 * @since 2.2
 */
public interface A2aResponse extends ConnectorResponse {

  /**
   * The agent's answer as plain text: the text parts of the final status message joined by newlines. Empty
   * when the agent answered only with artifacts or non-text parts.
   */
  String PARAM_TEXT = "text";

  /** The full final status message, including all of its parts, as a {@code Map}. */
  String PARAM_STATUS_MESSAGE = "statusMessage";

  /** All artifacts the agent produced, as a {@code List<Map>}, each with its {@code parts}. */
  String PARAM_ARTIFACTS = "artifacts";

  /** The text parts of all artifacts joined by newlines. A convenience for the common single-text-artifact case. */
  String PARAM_ARTIFACT_TEXT = "artifactText";

  /** The whole A2A task as a {@code Map}: id, contextId, status, artifacts and metadata. */
  String PARAM_TASK = "task";

  /** The A2A task id. Needed to correlate a push notification back to the waiting process instance. */
  String PARAM_TASK_ID = "taskId";

  /** The A2A context id. Pass it to a later activity to continue the same conversation. */
  String PARAM_CONTEXT_ID = "contextId";

  /**
   * The A2A task state, one of {@code TASK_STATE_SUBMITTED}, {@code TASK_STATE_WORKING},
   * {@code TASK_STATE_INPUT_REQUIRED}, {@code TASK_STATE_AUTH_REQUIRED}, {@code TASK_STATE_COMPLETED},
   * {@code TASK_STATE_CANCELED}, {@code TASK_STATE_FAILED} or {@code TASK_STATE_REJECTED}.
   */
  String PARAM_STATE = "state";

  /** Metadata the agent returned on the task, as a {@code Map}. */
  String PARAM_TASK_METADATA = "taskMetadata";

  /** Metadata the agent returned on the final status message, as a {@code Map}. */
  String PARAM_MESSAGE_METADATA = "messageMetadata";

  /** The conversation history as a {@code List<Map>}, present only when {@code includeHistory} is set. */
  String PARAM_HISTORY = "history";

  /**
   * {@code true} when at least one value was too large for a process variable and was truncated or replaced
   * by a reference. See {@link A2aRequest#PARAM_MAX_VARIABLE_SIZE}.
   */
  String PARAM_TRUNCATED = "truncated";

  /** @return the agent's answer as plain text, never {@code null} */
  String getText();

  /** @return the A2A task id, or {@code null} if the agent replied with a message instead of a task */
  String getTaskId();

  /** @return the A2A context id, or {@code null} if the agent did not provide one */
  String getContextId();

  /** @return the A2A task state, or {@code null} if the agent replied with a message instead of a task */
  String getState();

  /** @return the artifacts the agent produced, never {@code null} */
  List<Map<String, Object>> getArtifacts();

  /** @return {@code true} if any value was truncated or replaced by a reference */
  boolean isTruncated();

}
