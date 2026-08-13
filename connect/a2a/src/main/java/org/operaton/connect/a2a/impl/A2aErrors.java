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

import java.util.Set;

import org.operaton.bpm.engine.delegate.BpmnError;

import static org.operaton.connect.a2a.impl.A2aConnectorLogger.LOG;

/**
 * Turns A2A task states and failed calls into something a BPMN diagram can react to.
 *
 * <p>
 * The rule is: anything that the agent decided is over and went badly becomes a {@code BpmnError} with a
 * stable code that an error boundary event can catch. Anything that might succeed on a second attempt stays an
 * exception, so the job executor retries it and eventually raises an incident. A task waiting for more input
 * is neither, and is mapped to normal outputs.
 * </p>
 */
final class A2aErrors {

  static final String STATE_SUBMITTED = "TASK_STATE_SUBMITTED";
  static final String STATE_WORKING = "TASK_STATE_WORKING";
  static final String STATE_INPUT_REQUIRED = "TASK_STATE_INPUT_REQUIRED";
  static final String STATE_AUTH_REQUIRED = "TASK_STATE_AUTH_REQUIRED";
  static final String STATE_COMPLETED = "TASK_STATE_COMPLETED";
  static final String STATE_CANCELED = "TASK_STATE_CANCELED";
  static final String STATE_FAILED = "TASK_STATE_FAILED";
  static final String STATE_REJECTED = "TASK_STATE_REJECTED";

  /** The agent is done with this task, for better or worse. */
  private static final Set<String> FINAL_STATES =
      Set.of(STATE_COMPLETED, STATE_CANCELED, STATE_FAILED, STATE_REJECTED);

  /** The agent has stopped and is waiting for the caller to do something. */
  private static final Set<String> INTERRUPTED_STATES =
      Set.of(STATE_INPUT_REQUIRED, STATE_AUTH_REQUIRED);

  static final String CODE_TASK_FAILED = "a2a-task-failed";
  static final String CODE_TASK_REJECTED = "a2a-task-rejected";
  static final String CODE_TASK_CANCELED = "a2a-task-canceled";
  static final String CODE_AUTH_REQUIRED = "a2a-auth-required";
  static final String CODE_TIMEOUT = "a2a-timeout";
  static final String CODE_PROTOCOL_ERROR = "a2a-protocol-error";

  private A2aErrors() {
  }

  /**
   * @return {@code true} when there is no point waiting any longer, either because the task is over or
   *         because the agent is waiting on us
   */
  static boolean isSettled(String state) {
    return state == null || FINAL_STATES.contains(state) || INTERRUPTED_STATES.contains(state);
  }

  /**
   * Raises a {@code BpmnError} if the task ended badly. A completed task, and a task waiting for input,
   * return normally so that their outputs get mapped.
   *
   * @param snapshot the settled task
   */
  static void failIfUnsuccessful(A2aAgent.TaskSnapshot snapshot) {
    String state = snapshot.state();
    if (state == null || STATE_COMPLETED.equals(state)) {
      return;
    }
    switch (state) {
      case STATE_FAILED -> throw bpmnError(CODE_TASK_FAILED, snapshot);
      case STATE_REJECTED -> throw bpmnError(CODE_TASK_REJECTED, snapshot);
      case STATE_CANCELED -> throw bpmnError(CODE_TASK_CANCELED, snapshot);
      case STATE_AUTH_REQUIRED -> throw bpmnError(CODE_AUTH_REQUIRED, snapshot);
      case STATE_INPUT_REQUIRED -> LOG.inputRequired(snapshot.taskId(), snapshot.contextId());
      default -> {
        // submitted or working: sendAsync returns these on purpose, so they are not a failure
      }
    }
  }

  /**
   * Raises the timeout error for a task that never settled.
   *
   * @param snapshot the last known state of the task
   * @param waitedMs how long we waited
   */
  static BpmnError timedOut(A2aAgent.TaskSnapshot snapshot, long waitedMs) {
    return new BpmnError(CODE_TIMEOUT, "A2A task '%s' in context '%s' was still '%s' after %d ms. %s"
        .formatted(snapshot.taskId(), snapshot.contextId(), snapshot.state(), waitedMs, recoveryHint(snapshot)));
  }

  /**
   * Translates a failed call into either a retryable exception or a {@code BpmnError}.
   *
   * @param cause the failed call
   * @param url the agent URL, for the message
   * @return the exception to throw
   */
  static RuntimeException translate(A2aCallException cause, String url) {
    if (cause.isRetryable()) {
      return LOG.transportFailure(url, cause);
    }
    return new BpmnError(CODE_PROTOCOL_ERROR, cause.getMessage());
  }

  private static BpmnError bpmnError(String code, A2aAgent.TaskSnapshot snapshot) {
    return new BpmnError(code, "A2A task '%s' in context '%s' ended in state '%s'. %s"
        .formatted(snapshot.taskId(), snapshot.contextId(), snapshot.state(), recoveryHint(snapshot)));
  }

  /**
   * Output parameters are not mapped when an error is thrown, so the ids a process needs to follow up on the
   * task have to travel inside the error message.
   */
  private static String recoveryHint(A2aAgent.TaskSnapshot snapshot) {
    return "Use operation 'getTask' with taskId '%s' to read the full task.".formatted(snapshot.taskId());
  }

}
