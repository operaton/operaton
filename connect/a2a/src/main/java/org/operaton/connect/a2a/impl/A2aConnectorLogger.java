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

import org.operaton.connect.ConnectorRequestException;
import org.operaton.connect.impl.ConnectLogger;

/**
 * Logging for the A2A connector.
 *
 * <p>
 * Header values and message bodies are never logged. Header <em>names</em> are logged at debug level so that a
 * missing {@code Authorization} header can be diagnosed without leaking the token.
 * </p>
 */
public class A2aConnectorLogger extends ConnectLogger {

  public static final String PROJECT_CODE = "A2A";

  public static final A2aConnectorLogger LOG =
      createLogger(A2aConnectorLogger.class, PROJECT_CODE, "org.operaton.bpm.connect.a2a", "01");

  public void sendingMessage(String operation, String url, String messageId) {
    logDebug("001", "Sending A2A '{}' to '{}' with message id '{}'", operation, url, messageId);
  }

  public void headersSet(String headerNames) {
    logDebug("002", "Sending A2A request with headers: {} (values redacted)", headerNames);
  }

  public void reattachedToTask(String taskId, String contextId) {
    logInfo("003", "Reattached to already running A2A task '{}' in context '{}' instead of sending again", taskId, contextId);
  }

  public void reattachProbeUnsupported(String url) {
    logDebug("004", "Agent at '{}' does not support ListTasks, cannot probe for a task to reattach to", url);
  }

  public void waitingForTask(String taskId, String state, long remainingMs) {
    logDebug("005", "A2A task '{}' is '{}', waiting up to {} ms more", taskId, state, remainingMs);
  }

  public void valueTruncated(String what, int sizeBytes, int maxBytes) {
    logInfo("006", "A2A {} of {} bytes exceeds maxVariableSize of {} bytes and was truncated or replaced by a reference",
        what, sizeBytes, maxBytes);
  }

  public void inputRequired(String taskId, String contextId) {
    logInfo("007", "A2A task '{}' in context '{}' requires further input; mapped to outputs without raising an error",
        taskId, contextId);
  }

  public ConnectorRequestException operationRequired() {
    return new ConnectorRequestException(exceptionMessage("008",
        "Input parameter 'operation' is required and must be one of 'sendSync', 'sendAsync', 'getTask'"));
  }

  public ConnectorRequestException unknownOperation(String operation) {
    return new ConnectorRequestException(exceptionMessage("009",
        "Unknown A2A operation '{}'. Supported operations are 'sendSync', 'sendAsync', 'getTask'", operation));
  }

  public ConnectorRequestException urlRequired() {
    return new ConnectorRequestException(exceptionMessage("010", "Input parameter 'url' is required"));
  }

  public ConnectorRequestException taskIdRequiredForGetTask() {
    return new ConnectorRequestException(exceptionMessage("011",
        "Input parameter 'taskId' is required for the 'getTask' operation"));
  }

  public ConnectorRequestException invalidNumber(String parameter, Object value) {
    return new ConnectorRequestException(exceptionMessage("013",
        "Input parameter '{}' must be a number but was '{}'", parameter, value));
  }

  public ConnectorRequestException invalidMap(String parameter, Object value) {
    return new ConnectorRequestException(exceptionMessage("014",
        "Input parameter '{}' must be a map but was of type '{}'", parameter,
        value == null ? "null" : value.getClass().getName()));
  }

  public ConnectorRequestException invalidList(String parameter, Object value) {
    return new ConnectorRequestException(exceptionMessage("015",
        "Input parameter '{}' must be a list but was of type '{}'", parameter,
        value == null ? "null" : value.getClass().getName()));
  }

  public ConnectorRequestException unknownPartType(Object type) {
    return new ConnectorRequestException(exceptionMessage("016",
        "Unknown part type '{}'. Supported types are 'text', 'file' and 'data'", type));
  }

  public ConnectorRequestException inlineFileTooLarge(int sizeBytes, int maxBytes) {
    return new ConnectorRequestException(exceptionMessage("017",
        "Inline file part of {} bytes exceeds maxVariableSize of {} bytes. Send the file by uri instead",
        sizeBytes, maxBytes));
  }

  public ConnectorRequestException transportFailure(String url, Exception cause) {
    return new ConnectorRequestException(exceptionMessage("018",
        "Could not reach the A2A agent at '{}'. The job will be retried", url), cause);
  }

  public ConnectorRequestException interrupted(String taskId) {
    return new ConnectorRequestException(exceptionMessage("019",
        "Interrupted while waiting for A2A task '{}'", taskId));
  }

  public ConnectorRequestException messageOrPartsRequired() {
    return new ConnectorRequestException(exceptionMessage("020",
        "Input parameter 'message' or 'parts' is required to send a message to an agent"));
  }

  public ConnectorRequestException filePartNeedsUriOrBytes() {
    return new ConnectorRequestException(exceptionMessage("022",
        "A file part needs either a 'uri' or inline 'bytes'"));
  }

  public void exceptionWhileClosingAgent(Exception cause) {
    logDebug("021", "Exception while closing an A2A client: {}", cause.getMessage());
  }

}
