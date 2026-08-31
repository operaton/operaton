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
import java.util.function.Function;

/**
 * Hands the connector a scripted agent and records what it was asked to do.
 *
 * <p>
 * Lives in this package so that the engine-level tests, which sit in the public package, can script an agent
 * without the internals leaking into the connector's public API.
 * </p>
 */
public class FakeA2aAgentFactory implements Function<A2aAgent.Config, A2aAgent> {

  private final FakeA2aAgent agent = new FakeA2aAgent();

  private A2aAgent.Config lastConfig;

  @Override
  public A2aAgent apply(A2aAgent.Config config) {
    this.lastConfig = config;
    return agent;
  }

  /** The agent answers straight away, with everything a process would want to read. */
  public void completesWith(String taskId, String contextId, String text) {
    agent.sendResults.clear();
    agent.sendResults.add(new A2aAgent.TaskSnapshot(taskId, contextId, A2aErrors.STATE_COMPLETED,
        FakeA2aAgent.message(text),
        List.of(FakeA2aAgent.artifact("summary", text)),
        Map.of("model", "test-model"), List.of(), false));
  }

  /** The agent gives up on the task, which the connector turns into the {@code a2a-task-failed} BPMN error. */
  public void failsWith(String taskId, String contextId) {
    agent.sendResults.clear();
    agent.sendResults.add(FakeA2aAgent.snapshot(taskId, contextId, A2aErrors.STATE_FAILED,
        "I could not complete this"));
  }

  /** The agent accepts the work and will finish later, which is what sendAsync expects. */
  public void submitsAs(String taskId, String contextId) {
    agent.sendResults.clear();
    agent.sendResults.add(FakeA2aAgent.snapshot(taskId, contextId, A2aErrors.STATE_SUBMITTED, null));
  }

  /** Sets what the next {@code getTask} calls return, replacing anything scripted before. */
  public void getTaskReturns(String taskId, String contextId, String state, String text) {
    agent.getTaskResults.clear();
    agent.getTaskResults.add(new A2aAgent.TaskSnapshot(taskId, contextId, state,
        FakeA2aAgent.message(text),
        text == null ? List.of() : List.of(FakeA2aAgent.artifact("summary", text)),
        Map.of(), List.of(), false));
  }

  public A2aAgent.Config lastConfig() {
    return lastConfig;
  }

  public A2aAgent.SendCommand lastSend() {
    return agent.sends.get(agent.sends.size() - 1);
  }

  public List<A2aAgent.SendCommand> sends() {
    return agent.sends;
  }

  public List<String> getTaskCalls() {
    return agent.getTaskCalls;
  }

}
