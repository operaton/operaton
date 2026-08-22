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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.connect.Connectors;
import org.operaton.connect.a2a.impl.A2aConnectorImpl;
import org.operaton.connect.a2a.impl.FakeA2aAgentFactory;
import org.operaton.connect.spi.Connector;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the example processes on a real engine, with a scripted agent behind the connector.
 *
 * <p>
 * These tests exist to prove the parts that only the engine can prove: that a {@code BpmnError} actually reaches
 * an error boundary event, that outputs land in process variables, and that the async and polling shapes work as
 * modelled. Nothing here touches the network.
 * </p>
 */
class A2aConnectorProcessTest {

  private static final String SEND_SYNC = "org/operaton/connect/a2a/examples/a2a-send-sync.bpmn";
  private static final String SEND_ASYNC = "org/operaton/connect/a2a/examples/a2a-send-async.bpmn";
  private static final String POLL_FALLBACK = "org/operaton/connect/a2a/examples/a2a-poll-fallback.bpmn";

  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();

  private RuntimeService runtimeService;
  private HistoryService historyService;
  private ManagementService managementService;
  private FakeA2aAgentFactory agents;

  @BeforeEach
  void setUp() {
    runtimeService = engineExtension.getRuntimeService();
    historyService = engineExtension.getHistoryService();
    managementService = engineExtension.getManagementService();

    agents = new FakeA2aAgentFactory();
    a2aConnector().setAgentFactory(agents);
  }

  @Test
  void theConnectorIsDiscoveredByTheServiceLoader() {
    Connector<?> connector = Connectors.getConnector(A2aConnector.ID);

    assertThat(connector).isNotNull();
    assertThat(connector.getId()).isEqualTo(A2aConnector.ID);
  }

  @Test
  @Deployment(resources = SEND_SYNC)
  void sendSyncPutsTheAnswerIntoProcessVariables() {
    agents.completesWith("task-1", "ctx-1", "The answer is 42");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("a2aSendSync", variables());

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult()).isNull();
    assertThat(historicVariable(instance.getId(), "answer")).isEqualTo("The answer is 42");
    assertThat(historicVariable(instance.getId(), "a2aTaskId")).isEqualTo("task-1");
    assertThat(historicVariable(instance.getId(), "a2aContextId")).isEqualTo("ctx-1");
    assertThat(historicVariable(instance.getId(), "a2aState")).isEqualTo("TASK_STATE_COMPLETED");
    assertThat(historicVariable(instance.getId(), "a2aTruncated")).isEqualTo(false);
  }

  @Test
  @Deployment(resources = SEND_SYNC)
  void anAgentFailureIsCaughtByTheErrorBoundaryEvent() {
    agents.failsWith("task-1", "ctx-1");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("a2aSendSync", variables());

    assertThat(activityCompleted(instance.getId(), "failed")).isTrue();
    assertThat(activityCompleted(instance.getId(), "done")).isFalse();
  }

  @Test
  @Deployment(resources = SEND_SYNC)
  void theHeaderValueIsSentButNeverPutIntoAVariable() {
    agents.completesWith("task-1", "ctx-1", "ok");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("a2aSendSync", variables());

    assertThat(agents.lastConfig().headers()).containsEntry("Authorization", "Bearer secret-token");
    assertThat(historicVariable(instance.getId(), "headers")).isNull();
  }

  @Test
  @Deployment(resources = SEND_ASYNC)
  void sendAsyncWaitsAtTheReceiveTaskUntilTheCallbackIsCorrelated() {
    agents.submitsAs("task-1", "ctx-1");

    Map<String, Object> variables = variables();
    variables.put("documentUrl", "https://files.example.com/document.pdf");
    variables.put("callbackUrl", "https://my-engine.example.com/a2a/callback");
    variables.put("callbackToken", "shared-secret");
    variables.put("callbackCredentials", "callback-secret");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("a2aSendAsync", variables);

    // The push notification config travelled with the send, and the ids needed to correlate are already stored.
    assertThat(agents.lastSend().callbackUrl()).isEqualTo("https://my-engine.example.com/a2a/callback");
    assertThat(agents.lastSend().returnImmediately()).isTrue();
    assertThat(runtimeService.getVariable(instance.getId(), "a2aTaskId")).isEqualTo("task-1");
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult())
        .isNotNull();

    // This is what a webhook endpoint does when the agent calls it.
    runtimeService.createMessageCorrelation("a2aCallback")
        .processInstanceVariableEquals("a2aTaskId", "task-1")
        .correlateWithResult();

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult()).isNull();
    assertThat(activityCompleted(instance.getId(), "done")).isTrue();
  }

  @Test
  @Deployment(resources = SEND_ASYNC)
  void aCallbackThatNeverArrivesIsHandledByTheTimerBoundaryEvent() {
    agents.submitsAs("task-1", "ctx-1");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("a2aSendAsync", variables());

    Job timer = managementService.createJobQuery().processInstanceId(instance.getId()).timers().singleResult();
    assertThat(timer).isNotNull();
    managementService.executeJob(timer.getId());

    assertThat(activityCompleted(instance.getId(), "gaveUp")).isTrue();
  }

  @Test
  @Deployment(resources = POLL_FALLBACK)
  void thePollingFallbackReadsTheTaskOnATimer() {
    agents.submitsAs("task-1", "ctx-1");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("a2aPollFallback", variables());
    assertThat(runtimeService.getVariable(instance.getId(), "a2aTaskId")).isEqualTo("task-1");

    // Still working: the process loops back to the timer rather than finishing.
    agents.getTaskReturns("task-1", "ctx-1", "TASK_STATE_WORKING", null);
    managementService.executeJob(firstTimer(instance.getId()));
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult())
        .isNotNull();

    // Now finished: the next poll ends the process with the answer.
    agents.getTaskReturns("task-1", "ctx-1", "TASK_STATE_COMPLETED", "42");
    managementService.executeJob(firstTimer(instance.getId()));

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult()).isNull();
    assertThat(historicVariable(instance.getId(), "answer")).isEqualTo("42");
  }

  private String firstTimer(String processInstanceId) {
    Job timer = managementService.createJobQuery().processInstanceId(processInstanceId).timers().list().get(0);
    return timer.getId();
  }

  private static Map<String, Object> variables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("agentUrl", "https://agent.example.com");
    variables.put("agentToken", "secret-token");
    variables.put("question", "What is the answer?");
    return variables;
  }

  private Object historicVariable(String processInstanceId, String name) {
    var variable = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(processInstanceId)
        .variableName(name)
        .singleResult();
    return variable == null ? null : variable.getValue();
  }

  private boolean activityCompleted(String processInstanceId, String activityId) {
    return historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId)
        .activityId(activityId)
        .count() > 0;
  }

  private static A2aConnectorImpl a2aConnector() {
    return Connectors.getConnector(A2aConnector.ID);
  }

}
