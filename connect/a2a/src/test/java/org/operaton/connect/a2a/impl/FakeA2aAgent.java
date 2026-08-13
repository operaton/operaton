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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A scripted stand-in for a real agent.
 *
 * <p>
 * Tests use this instead of stubbing the A2A wire protocol, so that what is under test is the connector's own
 * behaviour: which operation it runs, what it sends, how long it waits, and what it turns a task state into.
 * The last scripted result repeats, which is what makes a "still working forever" agent easy to express.
 * </p>
 */
class FakeA2aAgent implements A2aAgent {

  final List<SendCommand> sends = new ArrayList<>();
  final List<String> getTaskCalls = new ArrayList<>();
  final Deque<TaskSnapshot> sendResults = new ArrayDeque<>();
  final Deque<TaskSnapshot> getTaskResults = new ArrayDeque<>();

  Optional<String> reattachableTaskId = Optional.empty();
  RuntimeException sendFailure;
  boolean closed;

  @Override
  public TaskSnapshot send(SendCommand command) {
    sends.add(command);
    if (sendFailure != null) {
      throw sendFailure;
    }
    return next(sendResults);
  }

  @Override
  public TaskSnapshot getTask(String taskId, Integer historyLength) {
    getTaskCalls.add(taskId);
    return next(getTaskResults);
  }

  @Override
  public Optional<String> findLatestTaskId(String contextId) {
    return reattachableTaskId;
  }

  @Override
  public void close() {
    closed = true;
  }

  /** Consumes the queue until one result is left, then keeps returning that one. */
  private static TaskSnapshot next(Deque<TaskSnapshot> results) {
    if (results.isEmpty()) {
      throw new IllegalStateException("The test did not script a result for this call");
    }
    return results.size() > 1 ? results.poll() : results.peek();
  }

  static TaskSnapshot snapshot(String taskId, String contextId, String state, String text) {
    return new TaskSnapshot(taskId, contextId, state, message(text), List.of(), Map.of(), List.of(), false);
  }

  static Map<String, Object> message(String text) {
    if (text == null) {
      return null;
    }
    Map<String, Object> part = new LinkedHashMap<>();
    part.put("type", "text");
    part.put("text", text);

    Map<String, Object> message = new LinkedHashMap<>();
    message.put("messageId", "agent-message");
    message.put("role", "ROLE_AGENT");
    message.put("parts", List.of(part));
    message.put("metadata", Map.of("model", "test-model"));
    return message;
  }

  static Map<String, Object> artifact(String name, String text) {
    Map<String, Object> part = new LinkedHashMap<>();
    part.put("type", "text");
    part.put("text", text);

    Map<String, Object> artifact = new LinkedHashMap<>();
    artifact.put("artifactId", "artifact-" + name);
    artifact.put("name", name);
    artifact.put("parts", List.of(part));
    artifact.put("metadata", Map.of());
    return artifact;
  }

}
