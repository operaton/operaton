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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.event.conditional.SetVariableDelegate;
import org.operaton.bpm.engine.test.bpmn.executionlistener.CurrentActivityExecutionListener.CurrentActivity;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExecutionListener;

import static org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Frederik Heremans
 */
public class ExecutionListenerTest {

  protected static final String PROCESS_KEY = "Process";

  protected static final String ERROR_CODE = "208";
  protected static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException("Intended exception from delegate");

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;
  RepositoryService repositoryService;

  @BeforeEach
  void clearRecorderListener() {
    RecorderExecutionListener.clear();
  }

  @BeforeEach
  void resetListener() {
    ThrowBPMNErrorDelegate.reset();
    ThrowRuntimeExceptionDelegate.reset();
  }

  public void assertProcessEnded(final String processInstanceId) {
    ProcessInstance processInstance = runtimeService
            .createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();

    if (processInstance != null) {
      throw new AssertionFailedError("Expected finished process instance '" + processInstanceId + "' but it was still in the db");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenersProcess.bpmn20.xml"})
  void testExecutionListenersOnAllPossibleElements() {

    // Process start executionListener will have executionListener class that sets 2 variables
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess", "businessKey123");

    String varSetInExecutionListener = (String) runtimeService.getVariable(processInstance.getId(), "variableSetInExecutionListener");
    assertThat(varSetInExecutionListener).isNotNull().isEqualTo("firstValue");

    // Check if business key was available in execution listener
    String businessKey = (String) runtimeService.getVariable(processInstance.getId(), "businessKeyInExecution");
    assertThat(businessKey).isNotNull().isEqualTo("businessKey123");

    // Transition take executionListener will set 2 variables
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    varSetInExecutionListener = (String) runtimeService.getVariable(processInstance.getId(), "variableSetInExecutionListener");

    assertThat(varSetInExecutionListener).isNotNull().isEqualTo("secondValue");

    ExampleExecutionListenerPojo myPojo = new ExampleExecutionListenerPojo();
    runtimeService.setVariable(processInstance.getId(), "myPojo", myPojo);

    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    // First usertask uses a method-expression as executionListener: ${myPojo.myMethod(execution.eventName)}
    ExampleExecutionListenerPojo pojoVariable = (ExampleExecutionListenerPojo) runtimeService.getVariable(processInstance.getId(), "myPojo");
    assertThat(pojoVariable.getReceivedEventName()).isNotNull();
    assertThat(pojoVariable.getReceivedEventName()).isEqualTo("end");

    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenersStartEndEvent.bpmn20.xml"})
  void testExecutionListenersOnStartEndEvents() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess");
    testRule.assertProcessEnded(processInstance.getId());

    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(4);

    assertThat(recordedEvents.get(0).getActivityId()).isEqualTo("theStart");
    assertThat(recordedEvents.get(0).getActivityName()).isEqualTo("Start Event");
    assertThat(recordedEvents.get(0).getParameter()).isEqualTo("Start Event Listener");
    assertThat(recordedEvents.get(0).getEventName()).isEqualTo("end");
    assertThat(recordedEvents.get(0).isCanceled()).isFalse();

    assertThat(recordedEvents.get(1).getActivityId()).isEqualTo("noneEvent");
    assertThat(recordedEvents.get(1).getActivityName()).isEqualTo("None Event");
    assertThat(recordedEvents.get(1).getParameter()).isEqualTo("Intermediate Catch Event Listener");
    assertThat(recordedEvents.get(1).getEventName()).isEqualTo("end");
    assertThat(recordedEvents.get(1).isCanceled()).isFalse();

    assertThat(recordedEvents.get(2).getActivityId()).isEqualTo("signalEvent");
    assertThat(recordedEvents.get(2).getActivityName()).isEqualTo("Signal Event");
    assertThat(recordedEvents.get(2).getParameter()).isEqualTo("Intermediate Throw Event Listener");
    assertThat(recordedEvents.get(2).getEventName()).isEqualTo("start");
    assertThat(recordedEvents.get(2).isCanceled()).isFalse();

    assertThat(recordedEvents.get(3).getActivityId()).isEqualTo("theEnd");
    assertThat(recordedEvents.get(3).getActivityName()).isEqualTo("End Event");
    assertThat(recordedEvents.get(3).getParameter()).isEqualTo("End Event Listener");
    assertThat(recordedEvents.get(3).getEventName()).isEqualTo("start");
    assertThat(recordedEvents.get(3).isCanceled()).isFalse();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenersFieldInjectionProcess.bpmn20.xml"})
  void testExecutionListenerFieldInjection() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("myVar", "listening!");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess", variables);

    Object varSetByListener = runtimeService.getVariable(processInstance.getId(), "var");
    assertThat(varSetByListener)
      .isInstanceOf(String.class)
      // Result is a concatenation of fixed injected field and injected expression
      .isEqualTo("Yes, I am listening!");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenersCurrentActivity.bpmn20.xml"})
  void testExecutionListenerCurrentActivity() {

    CurrentActivityExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess");
    testRule.assertProcessEnded(processInstance.getId());

    List<CurrentActivity> currentActivities = CurrentActivityExecutionListener.getCurrentActivities();
    assertThat(currentActivities).hasSize(3);

    assertThat(currentActivities.get(0).getActivityId()).isEqualTo("theStart");
    assertThat(currentActivities.get(0).getActivityName()).isEqualTo("Start Event");

    assertThat(currentActivities.get(1).getActivityId()).isEqualTo("noneEvent");
    assertThat(currentActivities.get(1).getActivityName()).isEqualTo("None Event");

    assertThat(currentActivities.get(2).getActivityId()).isEqualTo("theEnd");
    assertThat(currentActivities.get(2).getActivityName()).isEqualTo("End Event");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenerTest.testOnBoundaryEvents.bpmn20.xml"})
  void testOnBoundaryEvents() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Job firstTimer = managementService.createJobQuery().timers().singleResult();

    managementService.executeJob(firstTimer.getId());

    Job secondTimer = managementService.createJobQuery().timers().singleResult();

    managementService.executeJob(secondTimer.getId());

    testRule.assertProcessEnded(processInstance.getId());

    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(2);

    assertThat(recordedEvents.get(0).getActivityId()).isEqualTo("timer1");
    assertThat(recordedEvents.get(0).getParameter()).isEqualTo("start boundary listener");
    assertThat(recordedEvents.get(0).getEventName()).isEqualTo("start");
    assertThat(recordedEvents.get(0).isCanceled()).isFalse();

    assertThat(recordedEvents.get(1).getActivityId()).isEqualTo("timer2");
    assertThat(recordedEvents.get(1).getParameter()).isEqualTo("end boundary listener");
    assertThat(recordedEvents.get(1).getEventName()).isEqualTo("end");
    assertThat(recordedEvents.get(1).isCanceled()).isFalse();
  }

  @Test
  @Deployment
  void testScriptListener() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(processInstance.isEnded()).isTrue();


    if (processEngineRule.getProcessEngineConfiguration().getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
      long count = query.count();
      assertThat(count).isEqualTo(5);

      HistoricVariableInstance variableInstance;
      String[] variableNames = new String[]{"start-start", "start-end", "start-take", "end-start", "end-end"};
      for (String variableName : variableNames) {
        variableInstance = query.variableName(variableName).singleResult();
        assertThat(variableInstance).as("Unable to find variable with name '%s'".formatted(variableName)).isNotNull();
        assertThat((Boolean) variableInstance.getValue()).as("Variable '%s' should be set to true".formatted(variableName)).isTrue();
      }
    }
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenerTest.testScriptResourceListener.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/executionlistener/executionListener.groovy"
  })
  void testScriptResourceListener() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(processInstance.isEnded()).isTrue();

    if (processEngineRule.getProcessEngineConfiguration().getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
      long count = query.count();
      assertThat(count).isEqualTo(5);

      HistoricVariableInstance variableInstance;
      String[] variableNames = new String[]{"start-start", "start-end", "start-take", "end-start", "end-end"};
      for (String variableName : variableNames) {
        variableInstance = query.variableName(variableName).singleResult();
        assertThat(variableInstance).as("Unable to find variable with name '%s'".formatted(variableName)).isNotNull();
        assertThat((Boolean) variableInstance.getValue()).as("Variable '%s' should be set to true".formatted(variableName)).isTrue();
      }
    }
  }

  @Test
  @Deployment
  void testExecutionListenerOnTerminateEndEvent() {
    RecorderExecutionListener.clear();

    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();

    assertThat(recordedEvents).hasSize(2);

    assertThat(recordedEvents.get(0).getEventName()).isEqualTo("start");
    assertThat(recordedEvents.get(1).getEventName()).isEqualTo("end");

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenerTest.testOnCancellingBoundaryEvent.bpmn"})
  void testOnCancellingBoundaryEvents() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Job timer = managementService.createJobQuery().timers().singleResult();

    managementService.executeJob(timer.getId());

    testRule.assertProcessEnded(processInstance.getId());

    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(1);

    assertThat(recordedEvents.get(0).getActivityId()).isEqualTo("UserTask_1");
    assertThat(recordedEvents.get(0).getEventName()).isEqualTo("end");
    assertThat(recordedEvents.get(0).isCanceled()).isTrue();
  }

  private static final String MESSAGE = "cancelMessage";
  public static final BpmnModelInstance PROCESS_SERVICE_TASK_WITH_EXECUTION_START_LISTENER = Bpmn.createExecutableProcess(PROCESS_KEY)
          .startEvent()
          .parallelGateway("fork")
          .userTask("userTask1")
          .serviceTask("sendTask")
            .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, SendMessageDelegate.class.getName())
            .operatonExpression("${true}")
          .endEvent("endEvent")
            .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
          .moveToLastGateway()
          .userTask("userTask2")
          .boundaryEvent("boundaryEvent")
          .message(MESSAGE)
          .endEvent("endBoundaryEvent")
          .moveToNode("userTask2")
          .endEvent()
          .done();

  @Test
  void testServiceTaskExecutionListenerCall() {
    testRule.deploy(PROCESS_SERVICE_TASK_WITH_EXECUTION_START_LISTENER);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().list()).isEmpty();
    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(1);
    assertThat(recordedEvents.get(0).getActivityId()).isEqualTo("endEvent");
  }

  public static final BpmnModelInstance PROCESS_SERVICE_TASK_WITH_TWO_EXECUTION_START_LISTENER = modify(PROCESS_SERVICE_TASK_WITH_EXECUTION_START_LISTENER)
          .activityBuilder("sendTask")
          .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
          .done();

  @Test
  void testServiceTaskTwoExecutionListenerCall() {
    testRule.deploy(PROCESS_SERVICE_TASK_WITH_TWO_EXECUTION_START_LISTENER);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().list()).isEmpty();
    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(2);
    assertThat(recordedEvents.get(0).getActivityId()).isEqualTo("sendTask");
    assertThat(recordedEvents.get(1).getActivityId()).isEqualTo("endEvent");
  }

  public static final BpmnModelInstance PROCESS_SERVICE_TASK_WITH_EXECUTION_START_LISTENER_AND_SUB_PROCESS = modify(Bpmn.createExecutableProcess(PROCESS_KEY)
          .startEvent()
          .userTask("userTask")
          .serviceTask("sendTask")
            .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, SendMessageDelegate.class.getName())
            .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
            .operatonExpression("${true}")
          .endEvent("endEvent")
            .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
          .done())
          .addSubProcessTo(PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent("startSubProcess")
              .interrupting(false)
              .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
              .message(MESSAGE)
            .userTask("subProcessTask")
              .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
            .endEvent("endSubProcess")
          .done();

  @Test
  void testServiceTaskExecutionListenerCallAndSubProcess() {
    testRule.deploy(PROCESS_SERVICE_TASK_WITH_EXECUTION_START_LISTENER_AND_SUB_PROCESS);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask").singleResult();
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().list()).hasSize(1);

    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(4);
    assertThat(recordedEvents.get(0).getActivityId()).isEqualTo("startSubProcess");
    assertThat(recordedEvents.get(1).getActivityId()).isEqualTo("subProcessTask");
    assertThat(recordedEvents.get(2).getActivityId()).isEqualTo("sendTask");
    assertThat(recordedEvents.get(3).getActivityId()).isEqualTo("endEvent");
  }

  public static class SendMessageDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
      runtimeService.correlateMessage(MESSAGE);
    }
  }

  @Test
  void testEndExecutionListenerIsCalledOnlyOnce() {

    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("conditionalProcessKey")
      .startEvent()
      .userTask()
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableDelegate.class.getName())
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .endEvent()
      .done();

    modelInstance = modify(modelInstance)
      .addSubProcessTo("conditionalProcessKey")
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent()
      .interrupting(true)
      .conditionalEventDefinition()
      .condition("${variable == 1}")
      .conditionalEventDefinitionDone()
      .endEvent().done();

    testRule.deploy(modelInstance);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("conditionalProcessKey");
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then end listener sets variable and triggers conditional event
    //end listener should be called only once
    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenerTest.testMultiInstanceCancelation.bpmn20.xml")
  void testMultiInstanceCancelationDoesNotAffectEndListener() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MultiInstanceCancelation");
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(2).getId());

    // when
    taskService.complete(tasks.get(3).getId());

    // then
    testRule.assertProcessEnded(processInstance.getId());
    if (processEngineRule.getProcessEngineConfiguration().getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      HistoricVariableInstance endVariable = historyService.createHistoricVariableInstanceQuery()
          .processInstanceId(processInstance.getId())
          .variableName("finished")
          .singleResult();
      assertThat(endVariable).isNotNull();
      assertThat(endVariable.getValue()).isNotNull();
      assertThat(Boolean.parseBoolean(String.valueOf(endVariable.getValue()))).isTrue();
    }
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/executionlistener/ExecutionListenerTest.testMultiInstanceCancelation.bpmn20.xml")
  void testProcessInstanceCancelationNoticedInEndListener() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MultiInstanceCancelation");
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(2).getId());

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "myReason");

    // then
    testRule.assertProcessEnded(processInstance.getId());
    if (processEngineRule.getProcessEngineConfiguration().getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      HistoricVariableInstance endVariable = historyService.createHistoricVariableInstanceQuery()
          .processInstanceId(processInstance.getId())
          .variableName("canceled")
          .singleResult();
      assertThat(endVariable).isNotNull();
      assertThat(endVariable.getValue()).isNotNull();
      assertThat(Boolean.parseBoolean(String.valueOf(endVariable.getValue()))).isTrue();
    }
  }

  @Test
  void testThrowExceptionInStartListenerServiceTaskWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_START, true);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Intended exception from delegate");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void testThrowExceptionInEndListenerAndServiceTaskWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_END, true);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    String taskId = task.getId();

    // when listeners are invoked
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Intended exception from delegate");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void testThrowExceptionInEndListenerAndServiceTaskWithCatchException() {
    // given
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_END, true, true);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught(true);
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowExceptionInEndListenerAndSubprocessWithCatchException() {
    // given
    BpmnModelInstance model = createModelWithCatchInSubprocessAndListener(ExecutionListener.EVENTNAME_END, true, true);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught(true);
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowExceptionInEndListenerAndEventSubprocessWithCatchException() {
    // given
    BpmnModelInstance model = createModelWithCatchInEventSubprocessAndListener(ExecutionListener.EVENTNAME_END, true, true);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught(true);
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartListenerServiceTaskWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartListenerAndSubprocessWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInSubprocessAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartListenerAndEventSubprocessWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInEventSubprocessAndListener(ExecutionListener.EVENTNAME_START);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInEndListenerAndServiceTaskWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInServiceTaskAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInEndListenerAndSubprocessWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInSubprocessAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInEndListenerAndEventSubprocessWithCatch() {
    // given
    BpmnModelInstance model = createModelWithCatchInEventSubprocessAndListener(ExecutionListener.EVENTNAME_END);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInTakeListenerAndEventSubprocessWithCatch() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .userTask("userTask1")
        .sequenceFlowId("flow1")
        .userTask("afterListener")
        .endEvent()
        .done();

    OperatonExecutionListener listener = model.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonClass(ThrowBPMNErrorDelegate.class.getName());
    model.<SequenceFlow>getModelElementById("flow1").builder().addExtensionElement(listener);

    processBuilder.eventSubProcess()
        .startEvent("errorEvent").error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
  }

  @Test
  void testThrowBpmnErrorInStartListenerOfStartEventAndEventSubprocessWithCatch() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
        .userTask("afterListener")
        .endEvent()
        .done();

    processBuilder.eventSubProcess()
        .startEvent("errorEvent").error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent();

    testRule.deploy(model);
    // when the listeners are invoked
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    // then
    verifyErrorGotCaught();
  }

  @Test
  void testThrowBpmnErrorInStartListenerOfStartEventAndSubprocessWithCatch() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .subProcess("sub")
          .embeddedSubProcess()
            .startEvent("inSub")
            .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
            .userTask("afterListener")
            .endEvent()
          .subProcessDone()
        .boundaryEvent("errorEvent")
        .error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent("endEvent")
        .moveToActivity("sub")
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
  }

  @Test
  void testThrowBpmnErrorInEndListenerOfLastEventAndEventProcessWithCatch() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExpression("${true}")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ThrowBPMNErrorDelegate.class.getName())
        .done();

    processBuilder.eventSubProcess()
        .startEvent("errorEvent").error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent();

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    Task afterCatch = taskService.createTaskQuery().singleResult();
    assertThat(afterCatch).isNotNull();
    assertThat(afterCatch.getName()).isEqualTo("afterCatch");
    assertThat(ThrowBPMNErrorDelegate.invocations).isEqualTo(1);

    // and completing this task ends the process instance
    taskService.complete(afterCatch.getId());

    assertThat(runtimeService.createExecutionQuery().count()).isZero();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInEndListenerOfLastEventAndServiceTaskWithCatch() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExpression("${true}")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ThrowBPMNErrorDelegate.class.getName())
        .boundaryEvent()
        .error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent()
        .done();

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartListenerOfLastEventAndServiceTaskWithCatch() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExpression("${true}")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
        .boundaryEvent()
        .error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent()
        .done();

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInEndListenerOfLastEventAndSubprocessWithCatch() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .subProcess("sub")
          .embeddedSubProcess()
            .startEvent("inSub")
            .serviceTask("throw")
              .operatonExpression("${true}")
              .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ThrowBPMNErrorDelegate.class.getName())
        .boundaryEvent()
        .error(ERROR_CODE)
        .userTask("afterCatch")
        .moveToActivity("sub")
        .userTask("afterSub")
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartListenerOfLastEventAndSubprocessWithCatch() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .subProcess("sub")
          .embeddedSubProcess()
            .startEvent("inSub")
            .serviceTask("throw")
              .operatonExpression("${true}")
              .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
        .boundaryEvent()
        .error(ERROR_CODE)
        .userTask("afterCatch")
        .moveToActivity("sub")
        .userTask("afterSub")
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartListenerServiceTaskAndEndListener() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetsVariableDelegate.class.getName())
          .operatonExpression("${true}")
        .boundaryEvent("errorEvent")
        .error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent("endEvent")
        .moveToActivity("throw")
        .userTask("afterService")
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    // end listener is called
    assertThat(runtimeService.createVariableInstanceQuery().variableName("foo").singleResult().getValue()).isEqualTo("bar");
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartListenerOfStartEventAndCallActivity() {
    // given
    BpmnModelInstance subprocess = Bpmn.createExecutableProcess("subprocess")
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
          .operatonExpression("${true}")
        .userTask("afterService")
        .done();
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance parent = processBuilder
        .startEvent()
        .callActivity()
          .calledElement("subprocess")
        .userTask("afterCallActivity")
        .done();

    processBuilder.eventSubProcess()
    .startEvent("errorEvent").error(ERROR_CODE)
      .userTask("afterCatch")
    .endEvent();

    testRule.deploy(parent, subprocess);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInEndListenerInConcurrentExecutionAndEventSubprocessWithCatch() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .parallelGateway("fork")
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
          .operatonExpression("${true}")
        .userTask("afterService")
        .endEvent()
        .moveToLastGateway()
        .userTask("userTask2")
        .done();
    processBuilder.eventSubProcess()
       .startEvent("errorEvent").error(ERROR_CODE)
         .userTask("afterCatch")
       .endEvent();

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    // when the listeners are invoked
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  void testThrowBpmnErrorInStartExpressionListenerAndEventSubprocessWithCatch() {
    // given
    processEngineRule.getProcessEngineConfiguration().getBeans().put("myListener", new ThrowBPMNErrorDelegate());

    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, "${myListener.notify(execution)}")
          .operatonExpression("${true}")
        .userTask("afterService")
        .endEvent()
        .done();
    processBuilder.eventSubProcess()
       .startEvent("errorEvent").error(ERROR_CODE)
         .userTask("afterCatch")
       .endEvent();

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    // when listeners are invoked
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.complete(task.getId());

    // then
    verifyErrorGotCaught();
    verifyActivityCanceled("throw");
  }

  @Test
  @Deployment
  void testThrowBpmnErrorInEndScriptListenerAndSubprocessWithCatch() {
    // when the listeners are invoked
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    // then
    assertThat(taskService.createTaskQuery().list()).hasSize(1);
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("afterCatch");
    verifyActivityCanceled("task1");
  }

  @Test
  void testThrowUncaughtBpmnErrorFromEndListenerShouldNotTriggerListenerAgain() {

    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExpression("${true}")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ThrowBPMNErrorDelegate.class.getName())
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when the listeners are invoked
    taskService.complete(task.getId());

    // then

    // the process has ended, because the error was not caught
    assertThat(runtimeService.createExecutionQuery().count()).isZero();

    // the listener was only called once
    assertThat(ThrowBPMNErrorDelegate.invocations).isEqualTo(1);
    verifyActivityEnded("throw");
  }

  @Test
  void testThrowUncaughtBpmnErrorFromStartListenerShouldNotTriggerListenerAgain() {

    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExpression("${true}")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();

    // when the listeners are invoked
    taskService.complete(task.getId());

    // then

    // the process has ended, because the error was not caught
    assertThat(runtimeService.createExecutionQuery().count()).isZero();

    // the listener was only called once
    assertThat(ThrowBPMNErrorDelegate.invocations).isEqualTo(1);
    verifyActivityEnded("throw");
  }

  @Test
  void testThrowBpmnErrorInEndListenerMessageCorrelationShouldNotTriggerPropagation() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .subProcess("sub")
          .embeddedSubProcess()
            .startEvent("inSub")
            .userTask("taskWithListener")
            .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ThrowBPMNErrorDelegate.class.getName())
            .boundaryEvent("errorEvent")
            .error(ERROR_CODE)
            .userTask("afterCatch")
            .endEvent()
          .subProcessDone()
        .boundaryEvent("message")
        .message("foo")
        .userTask("afterMessage")
        .endEvent("endEvent")
        .moveToActivity("sub")
        .endEvent()
        .done();

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.complete(task.getId());
    // assert
    assertThat(taskService.createTaskQuery().list()).hasSize(1);
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("taskWithListener");

    // when
    // the listeners are invoked
    assertThatThrownBy(() -> runtimeService.correlateMessage("foo"))
      .hasMessageContaining("business error");

    // then
    assertThat(ThrowBPMNErrorDelegate.invocations).isEqualTo(1);
  }

  @Test
  void testThrowBpmnErrorInStartListenerOnModificationShouldNotTriggerPropagation() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask("userTask1")
        .subProcess("sub")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowBPMNErrorDelegate.class.getName())
          .embeddedSubProcess()
          .startEvent("inSub")
          .serviceTask("throw")
            .operatonExpression("${true}")
          .boundaryEvent("errorEvent1")
          .error(ERROR_CODE)
          .subProcessDone()
        .boundaryEvent("errorEvent2")
        .error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent("endEvent")
        .moveToActivity("sub")
        .userTask("afterSub")
        .endEvent()
        .done();
    DeploymentWithDefinitions deployment = testRule.deploy(model);
    ProcessDefinition definition = deployment.getDeployedProcessDefinitions().get(0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    var modificationBuilder = runtimeService.createModification(definition.getId()).startBeforeActivity("throw").processInstanceIds(processInstance.getId());

    // when/then
    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(BpmnError.class)
      .hasMessageContaining("business error");
  }

  @Test
  void testThrowBpmnErrorInProcessStartListenerShouldNotTriggerPropagation() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .userTask("afterThrow")
        .endEvent()
        .done();

    processBuilder.eventSubProcess()
        .startEvent("errorEvent").error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent();

    OperatonExecutionListener listener = model.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_START);
    listener.setOperatonClass(ThrowBPMNErrorDelegate.class.getName());
    model.<org.operaton.bpm.model.bpmn.instance.Process>getModelElementById(PROCESS_KEY).builder().addExtensionElement(listener);

    testRule.deploy(model);

    // when
    // listeners are invoked
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(PROCESS_KEY))
      .hasMessageContaining("business error");

    // then
    assertThat(ThrowBPMNErrorDelegate.invocations).isEqualTo(1);
  }

  @Test
  void testThrowBpmnErrorInProcessEndListenerShouldNotTriggerPropagation() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .endEvent()
        .done();

    processBuilder.eventSubProcess()
        .startEvent("errorEvent").error(ERROR_CODE)
        .userTask("afterCatch")
        .endEvent();

    OperatonExecutionListener listener = model.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_END);
    listener.setOperatonClass(ThrowBPMNErrorDelegate.class.getName());
    model.<org.operaton.bpm.model.bpmn.instance.Process>getModelElementById(PROCESS_KEY).builder().addExtensionElement(listener);

    testRule.deploy(model);

    // when
    // listeners are invoked
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(PROCESS_KEY))
      .hasMessageContaining("business error");

    // then
    assertThat(ThrowBPMNErrorDelegate.invocations).isEqualTo(1);
  }

  protected BpmnModelInstance createModelWithCatchInServiceTaskAndListener(String eventName) {
    return createModelWithCatchInServiceTaskAndListener(eventName, false);
  }

  protected BpmnModelInstance createModelWithCatchInServiceTaskAndListener(String eventName, boolean throwException) {
    return createModelWithCatchInServiceTaskAndListener(eventName, throwException, false);
  }

  protected BpmnModelInstance createModelWithCatchInServiceTaskAndListener(String eventName, boolean throwException, boolean catchException) {
    return Bpmn.createExecutableProcess(PROCESS_KEY)
          .startEvent()
          .userTask("userTask1")
          .serviceTask("throw")
            .operatonExecutionListenerClass(eventName, throwException ? ThrowRuntimeExceptionDelegate.class : ThrowBPMNErrorDelegate.class)
            .operatonExpression("${true}")
          .boundaryEvent("errorEvent")
          .error(catchException ? RUNTIME_EXCEPTION.getClass().getName() : ERROR_CODE)
          .userTask("afterCatch")
          .endEvent("endEvent")
          .moveToActivity("throw")
          .userTask("afterService")
          .endEvent()
          .done();
  }

  protected BpmnModelInstance createModelWithCatchInSubprocessAndListener(String eventName) {
    return createModelWithCatchInSubprocessAndListener(eventName, false, false);
  }

  protected BpmnModelInstance createModelWithCatchInSubprocessAndListener(String eventName, boolean throwException, boolean catchException) {
    return Bpmn.createExecutableProcess(PROCESS_KEY)
          .startEvent()
          .userTask("userTask1")
          .subProcess("sub")
            .embeddedSubProcess()
            .startEvent("inSub")
            .serviceTask("throw")
              .operatonExecutionListenerClass(eventName, throwException ? ThrowRuntimeExceptionDelegate.class : ThrowBPMNErrorDelegate.class)
              .operatonExpression("${true}")
              .userTask("afterService")
              .endEvent()
            .subProcessDone()
          .boundaryEvent("errorEvent")
          .error(catchException ? RUNTIME_EXCEPTION.getClass().getName() : ERROR_CODE)
          .userTask("afterCatch")
          .endEvent("endEvent")
          .moveToActivity("sub")
          .userTask("afterSub")
          .endEvent()
          .done();
  }

  protected BpmnModelInstance createModelWithCatchInEventSubprocessAndListener(String eventName) {
    return createModelWithCatchInEventSubprocessAndListener(eventName, false, false);
  }

  protected BpmnModelInstance createModelWithCatchInEventSubprocessAndListener(String eventName, boolean throwException, boolean catchException) {
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);
    BpmnModelInstance model = processBuilder
        .startEvent()
        .userTask("userTask1")
        .serviceTask("throw")
          .operatonExecutionListenerClass(eventName, throwException ? ThrowRuntimeExceptionDelegate.class : ThrowBPMNErrorDelegate.class)
          .operatonExpression("${true}")
        .userTask("afterService")
        .endEvent()
        .done();
    processBuilder.eventSubProcess()
       .startEvent("errorEvent").error(catchException ? RUNTIME_EXCEPTION.getClass().getName() : ERROR_CODE)
         .userTask("afterCatch")
       .endEvent();
    return model;
  }

  protected void verifyErrorGotCaught() {
    verifyErrorGotCaught(false);
  }

  protected void verifyErrorGotCaught(boolean useExceptionDelegate) {
    assertThat(taskService.createTaskQuery().list()).hasSize(1);
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("afterCatch");
    assertThat(useExceptionDelegate ? ThrowRuntimeExceptionDelegate.invocations : ThrowBPMNErrorDelegate.invocations).isEqualTo(1);
  }

  protected void verifyActivityCanceled(String activityName) {
    if (processEngineRule.getProcessEngineConfiguration().getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      assertThat(historyService.createHistoricActivityInstanceQuery()
          .activityName(activityName)
          .canceled()
          .count()).isEqualTo(1);
    }
  }

  protected void verifyActivityEnded(String activityName) {
    if (processEngineRule.getProcessEngineConfiguration().getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      assertThat(historyService.createHistoricActivityInstanceQuery()
          .activityName(activityName)
          .completeScope()
          .count()).isEqualTo(1);
    }
  }

  public static class ThrowBPMNErrorDelegate implements ExecutionListener {

    public static int invocations;

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      invocations++;
      throw new BpmnError(ERROR_CODE, "business error");
    }

    public static void reset() {
      invocations = 0;
    }
  }

  public static class ThrowRuntimeExceptionDelegate implements ExecutionListener {

    public static int invocations;

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      invocations++;
      throw RUNTIME_EXCEPTION;
    }

    public static void reset() {
      invocations = 0;
    }
  }

  public static class SetsVariableDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      execution.setVariable("foo", "bar");
    }
  }
}
