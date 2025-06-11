/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.bpmn.executionlistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.bpmn.executionlistener.ThrowingHistoryEventProducer.ERROR_CODE;
import static org.operaton.bpm.engine.test.bpmn.executionlistener.ThrowingHistoryEventProducer.EXCEPTION_MESSAGE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class ThrowingHistoryExecutionListenerTest {

  protected static final String PROCESS_KEY = "Process";
  protected static final String INTERNAL_ERROR_CODE = "208";
  protected static final ThrowingHistoryEventProducer HISTORY_PRODUCER = new ThrowingHistoryEventProducer();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(config -> config.setHistoryEventProducer(HISTORY_PRODUCER))
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;
  RepositoryService repositoryService;

  @AfterEach
  void reset() {
    HISTORY_PRODUCER.reset();
  }

  // UNCAUGHT EXCEPTION AFTER FAILED CUSTOM END LISTENER

  @Test
  void shouldFailForExceptionInHistoryListenerAfterBpmnErrorInEndListenerWithErrorBoundary() {
    // given
    HISTORY_PRODUCER.failsWithException().failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage(EXCEPTION_MESSAGE);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void shouldFailForExceptionInHistoryListenerAfterBpmnErrorInEndListenerWithErrorBoundaryOnSubprocess() {
    // given
    HISTORY_PRODUCER.failsWithException().failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInSubprocessAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage(EXCEPTION_MESSAGE);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void shouldFailForExceptionInHistoryListenerAfterBpmnErrorInEndListenerWithErrorStartInEventSubprocess() {
    // given
    HISTORY_PRODUCER.failsWithException().failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInEventSubprocessAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage(EXCEPTION_MESSAGE);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  // UNCAUGHT EXCEPTION AFTER FAILED CUSTOM START LISTENER

  @Test
  void shouldFailForExceptionInHistoryListenerAfterBpmnErrorInStartListenerWithErrorBoundary() {
    // given
    HISTORY_PRODUCER.failsWithException().failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage(EXCEPTION_MESSAGE);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void shouldFailForExceptionInHistoryListenerAfterBpmnErrorInStartListenerWithErrorBoundaryOnSubprocess() {
    // given
    HISTORY_PRODUCER.failsWithException().failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInSubprocessAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage(EXCEPTION_MESSAGE);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void shouldFailForExceptionInHistoryListenerAfterBpmnErrorInStartListenerWithErrorStartInEventSubprocess() {
    // given
    HISTORY_PRODUCER.failsWithException().failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInEventSubprocessAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage(EXCEPTION_MESSAGE);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  // CAUGHT EXCEPTION AFTER FAILED CUSTOM END LISTENER
  // NOTE: it is fine to alter the result of these tests, see https://jira.camunda.com/browse/CAM-14408

  @Test
  void shouldCatchBpmnErrorFromHistoryListenerAfterBpmnErrorInEndListenerWithErrorBoundary() {
    // given
    HISTORY_PRODUCER.failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyHistoryListenerErrorGotCaught();
    // and historic activity is still in running state since the history listener failed
    verifyActivityRunning("throw");
  }

  @Test
  void shouldCatchBpmnErrorFromHistoryListenerAfterBpmnErrorInEndListenerWithErrorBoundaryOnSubprocess() {
    // given
    HISTORY_PRODUCER.failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInSubprocessAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyHistoryListenerErrorGotCaught();
    // and historic activity is still in running state since the history listener failed
    verifyActivityRunning("throw");
  }

  @Test
  void shouldCatchBpmnErrorFromHistoryListenerAfterBpmnErrorInEndListenerWithErrorStartInEventSubprocess() {
    // given
    HISTORY_PRODUCER.failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInEventSubprocessAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyHistoryListenerErrorGotCaught();
    // and historic activity is still in running state since the history listener failed
    verifyActivityRunning("throw");
  }

  // CAUGHT EXCEPTION AFTER FAILED CUSTOM START LISTENER

  @Test
  void shouldFailForBpmnErrorInHistoryListenerAfterBpmnErrorInStartListenerWithErrorBoundary() {
    // given
    HISTORY_PRODUCER.failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(BpmnError.class);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void shouldFailForBpmnErrorInHistoryListenerAfterBpmnErrorInStartListenerWithErrorBoundaryOnSubprocess() {
    // given
    HISTORY_PRODUCER.failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInSubprocessAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(BpmnError.class);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void shouldFailForBpmnErrorInHistoryListenerAfterBpmnErrorInStartListenerWithErrorStartInEventSubprocess() {
    // given
    HISTORY_PRODUCER.failsAtActivity("throw");
    BpmnModelInstance model = createModelWithCatchInEventSubprocessAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(BpmnError.class);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  protected BpmnModelInstance createModelWithCatchInServiceTaskAndListener(String eventName) {
    return Bpmn.createExecutableProcess(PROCESS_KEY)
          .startEvent()
          .userTask("userTask1")
          .serviceTask("throw")
            .operatonExecutionListenerClass(eventName, ThrowBPMNErrorDelegate.class)
            .operatonExpression("${true}")
          .boundaryEvent("errorEvent")
            .error(INTERNAL_ERROR_CODE)
            .userTask("afterCatchInternal")
            .endEvent("endEventInternal")
          .moveToActivity("throw")
            .boundaryEvent("errorEventHistory")
            .error(ERROR_CODE)
            .userTask("afterCatchHistory")
            .endEvent("endEventHistory")
          .moveToActivity("throw")
          .userTask("afterService")
          .endEvent()
          .done();
  }

  protected BpmnModelInstance createModelWithCatchInSubprocessAndListener(String eventName) {
    return Bpmn.createExecutableProcess(PROCESS_KEY)
          .startEvent()
          .userTask("userTask1")
          .subProcess("sub")
            .embeddedSubProcess()
            .startEvent("inSub")
            .serviceTask("throw")
              .operatonExecutionListenerClass(eventName, ThrowBPMNErrorDelegate.class)
              .operatonExpression("${true}")
              .userTask("afterService")
              .endEvent()
            .subProcessDone()
          .boundaryEvent("errorEvent")
            .error(INTERNAL_ERROR_CODE)
            .userTask("afterCatch")
            .endEvent("endEvent")
          .moveToActivity("sub")
          .boundaryEvent("errorEventHistory")
            .error(ERROR_CODE)
            .userTask("afterCatchHistory")
            .endEvent("endEventHistory")
          .moveToActivity("sub")
          .userTask("afterSub")
          .endEvent()
          .done();
  }

  protected BpmnModelInstance createModelWithCatchInEventSubprocessAndListener(String eventName) {
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExecutionListenerClass(eventName, ThrowBPMNErrorDelegate.class)
          .operatonExpression("${true}")
        .userTask("afterService")
        .endEvent()
        .done();
    processBuilder.eventSubProcess()
       .startEvent("errorEvent").error(INTERNAL_ERROR_CODE)
       .userTask("afterCatch")
       .endEvent();
    processBuilder.eventSubProcess()
      .startEvent("errorEventHistory").error(ERROR_CODE)
      .userTask("afterCatchHistory")
      .endEvent();
    return model;
  }

  protected void verifyHistoryListenerErrorGotCaught() {
    assertThat(taskService.createTaskQuery().list()).hasSize(1);
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("afterCatchHistory");
  }

  protected void verifyActivityRunning(String activityName) {
    assertThat(historyService.createHistoricActivityInstanceQuery()
        .activityName(activityName)
        .unfinished()
        .count()).isEqualTo(1);
  }

  public static class ThrowBPMNErrorDelegate implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      throw new BpmnError(ERROR_CODE, "business error");
    }
  }

}
