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
package org.operaton.bpm.engine.test.bpmn.event.compensate;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance;
import org.operaton.bpm.engine.test.bpmn.event.compensate.ReadLocalVariableListener.VariableEvent;
import org.operaton.bpm.engine.test.bpmn.event.compensate.helper.BookFlightService;
import org.operaton.bpm.engine.test.bpmn.event.compensate.helper.CancelFlightService;
import org.operaton.bpm.engine.test.bpmn.event.compensate.helper.GetVariablesDelegate;
import org.operaton.bpm.engine.test.bpmn.event.compensate.helper.SetVariablesDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Daniel Meyer
 */
class CompensateEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;

  @Test
  void testCompensateOrder() {
    //given two process models, only differ in order of the activities
    final String PROCESS_MODEL_WITH_REF_BEFORE = "org/operaton/bpm/engine/test/bpmn/event/compensate/compensation_reference-before.bpmn";
    final String PROCESS_MODEL_WITH_REF_AFTER = "org/operaton/bpm/engine/test/bpmn/event/compensate/compensation_reference-after.bpmn";

    //when model with ref before is deployed
    DeploymentBuilder deploymentBuilder1 = repositoryService.createDeployment().addClasspathResource(PROCESS_MODEL_WITH_REF_BEFORE);
    assertThatCode(() -> engineRule.manageDeployment(deploymentBuilder1.deploy())).doesNotThrowAnyException();
    //then no problem will occur

    //when model with ref after is deployed
    DeploymentBuilder deploymentBuilder2 = repositoryService.createDeployment().addClasspathResource(PROCESS_MODEL_WITH_REF_AFTER);
    assertThatCode(() -> engineRule.manageDeployment(deploymentBuilder2.deploy())).doesNotThrowAnyException();
    //then also no problem should occur
  }

  @Deployment
  @Test
  void testCompensateSubprocess() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);

    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  void testCompensateSubprocessInsideSubprocess() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    completeTask("Book Hotel");
    completeTask("Book Flight");

    // throw compensation event
    completeTask("throw compensation");

    // execute compensation handlers
    completeTask("Cancel Hotel");
    completeTask("Cancel Flight");

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensateParallelSubprocess() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);

    Task singleResult = taskService.createTaskQuery().singleResult();
    taskService.complete(singleResult.getId());

    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  void testCompensateParallelSubprocessCompHandlerWaitstate() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    List<Task> compensationHandlerTasks = taskService.createTaskQuery().taskDefinitionKey("undoBookHotel").list();
    assertThat(compensationHandlerTasks).hasSize(5);

    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(processInstance.getId());
    List<ActivityInstance> compensationHandlerInstances = testRule.getInstancesForActivityId(rootActivityInstance, "undoBookHotel");
    assertThat(compensationHandlerInstances).hasSize(5);

    for (Task task : compensationHandlerTasks) {
      taskService.complete(task.getId());
    }

    Task singleResult = taskService.createTaskQuery().singleResult();
    taskService.complete(singleResult.getId());

    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensateParallelSubprocessCompHandlerWaitstate.bpmn20.xml")
  @Test
  void testDeleteParallelSubprocessCompHandlerWaitstate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    // five inner tasks
    List<Task> compensationHandlerTasks = taskService.createTaskQuery().taskDefinitionKey("undoBookHotel").list();
    assertThat(compensationHandlerTasks).hasSize(5);

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "");

    // then the process has been removed
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testCompensateMiSubprocess() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);

    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  void testCompensateScope() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(5);

    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());

  }

  // See: https://app.camunda.com/jira/browse/CAM-1410
  @Deployment
  @Test
  void testCompensateActivityRef() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isNull();

    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());

  }

  /**
   * CAM-3628
   */
  @Deployment
  @Test
  void testCompensateSubprocessWithBoundaryEvent() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("compensateProcess");

    Task compensationTask = taskService.createTaskQuery().singleResult();
    assertThat(compensationTask).isNotNull();
    assertThat(compensationTask.getTaskDefinitionKey()).isEqualTo("undoSubprocess");

    taskService.complete(compensationTask.getId());
    runtimeService.signal(instance.getId());
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment
  @Test
  void testCompensateActivityInSubprocess() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("compensateProcess");

    Task scopeTask = taskService.createTaskQuery().singleResult();
    taskService.complete(scopeTask.getId());

    // process has not yet thrown compensation
    // when throw compensation
    runtimeService.signal(instance.getId());
    // then
    Task compensationTask = taskService.createTaskQuery().singleResult();
    assertThat(compensationTask).isNotNull();
    assertThat(compensationTask.getTaskDefinitionKey()).isEqualTo("undoScopeTask");

    taskService.complete(compensationTask.getId());
    runtimeService.signal(instance.getId());
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment
  @Test
  void testCompensateActivityInConcurrentSubprocess() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("compensateProcess");

    Task scopeTask = taskService.createTaskQuery().taskDefinitionKey("scopeTask").singleResult();
    taskService.complete(scopeTask.getId());

    Task outerTask = taskService.createTaskQuery().taskDefinitionKey("outerTask").singleResult();
    taskService.complete(outerTask.getId());

    // process has not yet thrown compensation
    // when throw compensation
    runtimeService.signal(instance.getId());

    // then
    Task compensationTask = taskService.createTaskQuery().singleResult();
    assertThat(compensationTask).isNotNull();
    assertThat(compensationTask.getTaskDefinitionKey()).isEqualTo("undoScopeTask");

    taskService.complete(compensationTask.getId());
    runtimeService.signal(instance.getId());
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment
  @Test
  void testCompensateConcurrentMiActivity() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    // complete 4 of 5 user tasks
    completeTasks("Book Hotel", 4);

    // throw compensation event
    completeTaskWithVariable("Request Vacation", "accept", false);

    // should not compensate activity before multi instance activity is completed
    assertThat(taskService.createTaskQuery().taskName("Cancel Hotel").count()).isZero();

    // complete last open task and end process instance
    completeTask("Book Hotel");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensateConcurrentMiSubprocess() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    // complete 4 of 5 user tasks
    completeTasks("Book Hotel", 4);

    // throw compensation event
    completeTaskWithVariable("Request Vacation", "accept", false);

    // should not compensate activity before multi instance activity is completed
    assertThat(taskService.createTaskQuery().taskName("Cancel Hotel").count()).isZero();

    // complete last open task and end process instance
    completeTask("Book Hotel");

    runtimeService.signal(processInstanceId);
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensateActivityRefMiActivity() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    completeTasks("Book Hotel", 5);

    // throw compensation event for activity
    completeTaskWithVariable("Request Vacation", "accept", false);

    // execute compensation handlers for each execution of the subprocess
    assertThat(taskService.createTaskQuery().count()).isEqualTo(5);
    completeTasks("Cancel Hotel", 5);

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensateActivityRefMiSubprocess() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    completeTasks("Book Hotel", 5);

    // throw compensation event for activity
    completeTaskWithVariable("Request Vacation", "accept", false);

    // execute compensation handlers for each execution of the subprocess
    assertThat(taskService.createTaskQuery().count()).isEqualTo(5);
    completeTasks("Cancel Hotel", 5);

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCallActivityCompensationHandler.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensationHandler.bpmn20.xml"})
  @Test
  void testCallActivityCompensationHandler() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    if (!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(5);
    }

    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    if (!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(6);
    }

  }

  @Deployment
  @Test
  void testCompensateMiSubprocessVariableSnapshots() {
    // see referenced java delegates in the process definition.

    List<String> hotels = List.of("Rupert", "Vogsphere", "Milliways", "Taunton", "Ysolldins");

    SetVariablesDelegate.setValues(hotels);

    // SetVariablesDelegate take the first element of static list and set the value as local variable
    // GetVariablesDelegate read the variable and add the value to static list

    runtimeService.startProcessInstanceByKey("compensateProcess");

    if (!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(5);
    }

    assertThat(GetVariablesDelegate.values).containsAll(hotels);
  }

  @Deployment
  @Test
  void testCompensateMiSubprocessWithCompensationEventSubprocessVariableSnapshots() {
    // see referenced java delegates in the process definition.

    List<String> hotels = List.of("Rupert", "Vogsphere", "Milliways", "Taunton", "Ysolldins");

    SetVariablesDelegate.setValues(hotels);

    // SetVariablesDelegate take the first element of static list and set the value as local variable
    // GetVariablesDelegate read the variable and add the value to static list

    runtimeService.startProcessInstanceByKey("compensateProcess");

    if (!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(5);
    }

    assertThat(GetVariablesDelegate.values).containsAll(hotels);
  }

  @Deployment
  @Disabled("Fix CAM-4268")
  @Test
  void testCompensateMiSubprocessVariableSnapshotOfElementVariable() {
    Map<String, Object> variables = new HashMap<>();
    // multi instance collection
    List<String> flights = List.of("STS-14", "STS-28");
    variables.put("flights", flights);

    // see referenced java delegates in the process definition
    // java delegates read element variable (flight) and add the variable value
    // to a static list
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess", variables);

    if (!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookFlight").count()).isEqualTo(flights.size());
    }

    // java delegates should be invoked for each element in collection
    assertThat(BookFlightService.bookedFlights).isEqualTo(flights);
    assertThat(CancelFlightService.canceledFlights).isEqualTo(flights);

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationTriggeredByEventSubProcessActivityRef.bpmn20.xml"})
  @Test
  @SuppressWarnings("deprecation")
  void testCompensateActivityRefTriggeredByEventSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");
    testRule.assertProcessEnded(processInstance.getId());

    HistoricVariableInstanceQuery historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(processInstance.getId()).variableName("undoBookHotel");

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getVariableName()).isEqualTo("undoBookHotel");
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(5);

      assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("undoBookFlight").count()).isZero();
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationTriggeredByEventSubProcessInSubProcessActivityRef.bpmn20.xml"})
  @Test
  @SuppressWarnings("deprecation")
  void testCompensateActivityRefTriggeredByEventSubprocessInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");
    testRule.assertProcessEnded(processInstance.getId());

    HistoricVariableInstanceQuery historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(processInstance.getId()).variableName("undoBookHotel");

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getVariableName()).isEqualTo("undoBookHotel");
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(5);

      assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("undoBookFlight").count()).isZero();
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationInEventSubProcessActivityRef.bpmn20.xml"})
  @Test
  @SuppressWarnings("deprecation")
  void testCompensateActivityRefInEventSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");
    testRule.assertProcessEnded(processInstance.getId());

    HistoricVariableInstanceQuery historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery().variableName("undoBookSecondHotel");

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getVariableName()).isEqualTo("undoBookSecondHotel");
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(5);

      assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("undoBookFlight").count()).isZero();

      assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("undoBookHotel").count()).isZero();
    }
  }

  /**
   * enable test case when bug is fixed
   *
   * @see <a href="https://app.camunda.com/jira/browse/CAM-4304">https://app.camunda.com/jira/browse/CAM-4304</a>
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationInEventSubProcess.bpmn20.xml"})
  @Test
  @SuppressWarnings("deprecation")
  void testCompensateInEventSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");
    testRule.assertProcessEnded(processInstance.getId());

    HistoricVariableInstanceQuery historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery().variableName("undoBookSecondHotel");

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getVariableName()).isEqualTo("undoBookSecondHotel");
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(5);

      historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery().variableName("undoBookFlight");

      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(5);

      historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery().variableName("undoBookHotel");

      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(5);
    }
  }

  @Deployment
  @Test
  void testExecutionListeners() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("start", 0);
    variables.put("end", 0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess", variables);

    int started = (Integer) runtimeService.getVariable(processInstance.getId(), "start");
    assertThat(started).isEqualTo(5);

    int ended = (Integer) runtimeService.getVariable(processInstance.getId(), "end");
    assertThat(ended).isEqualTo(5);

    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      long finishedCount = historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").finished().count();
      assertThat(finishedCount).isEqualTo(5);
    }
  }

  @Deployment
  @Test
  void testActivityInstanceTreeWithoutEventScope() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = instance.getId();

    // when
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    // then
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
          .activity("task")
        .done());
  }

  @Deployment
  @Test
  void testConcurrentExecutionsAndPendingCompensation() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = instance.getId();
    String taskId = taskService.createTaskQuery().taskDefinitionKey("innerTask").singleResult().getId();

    // when (1)
    taskService.complete(taskId);

    // then (1)
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree).matches(
        describeExecutionTree(null)
        .scope()
          .child("task1").concurrent().noScope().up()
          .child("task2").concurrent().noScope().up()
          .child("subProcess").eventScope().scope().up()
        .done());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
          .activity("task1")
          .activity("task2")
        .done());

    // when (2)
    taskId = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult().getId();
    taskService.complete(taskId);

    // then (2)
    executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree).matches(
        describeExecutionTree("task2")
        .scope()
          .child("subProcess").eventScope().scope().up()
        .done());

    tree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
          .activity("task2")
        .done());

    // when (3)
    taskId = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult().getId();
    taskService.complete(taskId);

    // then (3)
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensationEndEventWithScope() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    if (!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(5);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookFlight").count()).isEqualTo(5);
    }

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testCompensationEndEventWithActivityRef() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    if (!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(5);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookFlight").count()).isZero();
    }

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.activityWithCompensationEndEvent.bpmn20.xml")
  @Test
  void testActivityInstanceTreeForCompensationEndEvent(){
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
       describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("end")
          .activity("undoBookHotel")
      .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.compensationMiActivity.bpmn20.xml")
  @Test
  void testActivityInstanceTreeForMiActivity(){
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
       describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("end")
          .beginMiBody("bookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
      .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensateParallelSubprocessCompHandlerWaitstate.bpmn20.xml")
  @Test
  void testActivityInstanceTreeForParallelMiActivityInSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("parallelTask")
        .activity("throwCompensate")
        .beginScope("scope")
          .beginMiBody("bookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
        .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.compensationMiSubprocess.bpmn20.xml")
  @Test
  void testActivityInstanceTreeForMiSubprocess(){
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    completeTasks("Book Hotel", 5);
    // throw compensation event
    completeTask("throwCompensation");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("throwingCompensation")
        .beginMiBody("scope")
          .activity("undoBookHotel")
          .activity("undoBookHotel")
          .activity("undoBookHotel")
          .activity("undoBookHotel")
          .activity("undoBookHotel")
      .done());
  }

  @Deployment
  @Disabled("CAM-4903")
  @Test
  void testActivityInstanceTreeForMiSubProcessDefaultHandler() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    completeTasks("Book Hotel", 5);
    // throw compensation event
    completeTask("throwCompensation");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("throwingCompensation")
        .beginMiBody("scope")
          .beginScope("scope")
            .activity("undoBookHotel")
          .endScope()
          .beginScope("scope")
            .activity("undoBookHotel")
          .endScope()
          .beginScope("scope")
            .activity("undoBookHotel")
          .endScope()
          .beginScope("scope")
            .activity("undoBookHotel")
          .endScope()
          .beginScope("scope")
            .activity("undoBookHotel")
      .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.activityWithCompensationEndEvent.bpmn20.xml")
  @Test
  void testCancelProcessInstanceWithActiveCompensation() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationEventSubProcess.bpmn20.xml"})
  @Test
  void testCompensationEventSubProcessWithScope() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("bookingProcess").getId();

    completeTask("Book Flight");
    completeTask("Book Hotel");

    // throw compensation event for current scope (without activityRef)
    completeTaskWithVariable("Validate Booking", "valid", false);

    // first - compensate book flight
    assertThat(taskService.createTaskQuery().count()).isOne();
    completeTask("Cancel Flight");
    // second - compensate book hotel
    assertThat(taskService.createTaskQuery().count()).isOne();
    completeTask("Cancel Hotel");
    // third - additional compensation handler
    completeTask("Update Customer Record");

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensationEventSubProcessWithActivityRef() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("bookingProcess").getId();

    completeTask("Book Hotel");
    completeTask("Book Flight");

    // throw compensation event for specific scope (with activityRef = subprocess)
    completeTaskWithVariable("Validate Booking", "valid", false);

    // compensate the activity within this scope
    assertThat(taskService.createTaskQuery().count()).isOne();
    completeTask("Cancel Hotel");

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationEventSubProcess.bpmn20.xml"})
  @Test
  void testActivityInstanceTreeForCompensationEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("bookingProcess");

    completeTask("Book Flight");
    completeTask("Book Hotel");

    // throw compensation event
    completeTaskWithVariable("Validate Booking", "valid", false);

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
         describeActivityInstanceTree(processInstance.getProcessDefinitionId())
           .activity("throwCompensation")
           .beginScope("booking-subprocess")
             .activity("cancelFlight")
             .beginScope("compensationSubProcess")
               .activity("compensateFlight")
         .done());
  }

  @Deployment
  @Test
  void testCompensateMiSubprocessWithCompensationEventSubProcess() {
    Map<String, Object> variables = new HashMap<>();
    // multi instance collection
    variables.put("flights", List.of("STS-14", "STS-28"));

    String processInstanceId = runtimeService.startProcessInstanceByKey("bookingProcess", variables).getId();

    completeTask("Book Flight");
    completeTask("Book Hotel");

    completeTask("Book Flight");
    completeTask("Book Hotel");

    // throw compensation event
    completeTaskWithVariable("Validate Booking", "valid", false);

    // execute compensation handlers for each execution of the subprocess
    completeTasks("Cancel Flight", 2);
    completeTasks("Cancel Hotel", 2);
    completeTasks("Update Customer Record", 2);

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensateParallelMiSubprocessWithCompensationEventSubProcess() {
    Map<String, Object> variables = new HashMap<>();
    // multi instance collection
    variables.put("flights", List.of("STS-14", "STS-28"));

    String processInstanceId = runtimeService.startProcessInstanceByKey("bookingProcess", variables).getId();

    completeTasks("Book Flight", 2);
    completeTasks("Book Hotel", 2);

    // throw compensation event
    completeTaskWithVariable("Validate Booking", "valid", false);

    // execute compensation handlers for each execution of the subprocess
    completeTasks("Cancel Flight", 2);
    completeTasks("Cancel Hotel", 2);
    completeTasks("Update Customer Record", 2);

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensationEventSubprocessWithoutBoundaryEvents() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    completeTask("Book Hotel");
    completeTask("Book Flight");

    // throw compensation event
    completeTask("throw compensation");

    // execute compensation handlers
    completeTask("Cancel Flight");
    completeTask("Cancel Hotel");

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensationEventSubprocessReThrowCompensationEvent() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    completeTask("Book Hotel");
    completeTask("Book Flight");

    // throw compensation event
    completeTask("throw compensation");

    // execute compensation handler and re-throw compensation event
    completeTask("Cancel Hotel");
    // execute compensation handler at subprocess
    completeTask("Cancel Flight");

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensationEventSubprocessConsumeCompensationEvent() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("compensateProcess").getId();

    completeTask("Book Hotel");
    completeTask("Book Flight");

    // throw compensation event
    completeTask("throw compensation");

    // execute compensation handler and consume compensation event
    completeTask("Cancel Hotel");
    // compensation handler at subprocess (Cancel Flight) should not be executed
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testSubprocessCompensationHandler() {

    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessCompensationHandler");

    // when throwing compensation
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    // then the compensation handler has been activated
    // and the user task in the sub process can be successfully completed
    Task subProcessTask = taskService.createTaskQuery().singleResult();
    assertThat(subProcessTask).isNotNull();
    assertThat(subProcessTask.getTaskDefinitionKey()).isEqualTo("subProcessTask");

    taskService.complete(subProcessTask.getId());

    // and the task following compensation can be successfully completed
    Task afterCompensationTask = taskService.createTaskQuery().singleResult();
    assertThat(afterCompensationTask).isNotNull();
    assertThat(afterCompensationTask.getTaskDefinitionKey()).isEqualTo("beforeEnd");

    taskService.complete(afterCompensationTask.getId());

    // and the process has successfully ended
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testSubprocessCompensationHandler.bpmn20.xml")
  @Test
  void testSubprocessCompensationHandlerActivityInstanceTree() {

    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessCompensationHandler");

    // when throwing compensation
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    // then the activity instance tree is correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
       describeActivityInstanceTree(processInstance.getProcessDefinitionId())
         .activity("throwCompensate")
         .beginScope("compensationHandler")
           .activity("subProcessTask")
       .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testSubprocessCompensationHandler.bpmn20.xml")
  @Test
  void testSubprocessCompensationHandlerDeleteProcessInstance() {

    // given a process instance in compensation
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessCompensationHandler");
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    // when deleting the process instance
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then the process instance is ended
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Disabled("CAM-4387")
  @Test
  void testSubprocessCompensationHandlerWithEventSubprocess() {
    // given a process instance in compensation
    runtimeService.startProcessInstanceByKey("subProcessCompensationHandlerWithEventSubprocess");
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    // when the event subprocess is triggered that is defined as part of the compensation handler
    runtimeService.correlateMessage("Message");

    // then activity instance tree is correct
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("eventSubProcessTask");
  }

  /**
   * CAM-4387
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.testSubprocessCompensationHandlerWithEventSubprocess.bpmn20.xml")
  @Disabled("CAM-4387")
  @Test
  void testSubprocessCompensationHandlerWithEventSubprocessActivityInstanceTree() {
    // given a process instance in compensation
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessCompensationHandlerWithEventSubprocess");
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    // when the event subprocess is triggered that is defined as part of the compensation handler
    runtimeService.correlateMessage("Message");

    // then the event subprocess has been triggered
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("throwCompensate")
          .beginScope("compensationHandler")
            .beginScope("eventSubProcess")
              .activity("eventSubProcessTask")
       .done());
  }

  @Deployment
  @Disabled("CAM-4387")
  @Test
  void testReceiveTaskCompensationHandler() {
    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveTaskCompensationHandler");

    // when triggering compensation
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    // then there is a message event subscription for the receive task compensation handler
    EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getEventType()).isEqualTo(EventType.MESSAGE.name());

    // and triggering the message completes compensation
    runtimeService.correlateMessage("Message");

    Task afterCompensationTask = taskService.createTaskQuery().singleResult();
    assertThat(afterCompensationTask).isNotNull();
    assertThat(afterCompensationTask.getTaskDefinitionKey()).isEqualTo("beforeEnd");

    taskService.complete(afterCompensationTask.getId());

    // and the process has successfully ended
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testConcurrentScopeCompensation() {
    // given a process instance with two concurrent tasks, one of which is waiting
    // before throwing compensation
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("concurrentScopeCompensation");
    Task beforeCompensationTask = taskService.createTaskQuery().taskDefinitionKey("beforeCompensationTask").singleResult();
    Task concurrentTask = taskService.createTaskQuery().taskDefinitionKey("concurrentTask").singleResult();

    // when throwing compensation such that two subprocesses are compensated
    taskService.complete(beforeCompensationTask.getId());

    // then both compensation handlers have been executed
    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricVariableInstanceQuery historicVariableInstanceQuery = historyService
          .createHistoricVariableInstanceQuery().variableName("compensateScope1Task");

      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(1);

      historicVariableInstanceQuery = historyService
          .createHistoricVariableInstanceQuery().variableName("compensateScope2Task");

      assertThat(historicVariableInstanceQuery.count()).isOne();
      assertThat(historicVariableInstanceQuery.list().get(0).getValue()).isEqualTo(1);
    }

    // and after completing the concurrent task, the process instance ends successfully
    taskService.complete(concurrentTask.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testLocalVariablesInEndExecutionListener() {
    // given
    SetLocalVariableListener setListener = new SetLocalVariableListener("foo", "bar");
    ReadLocalVariableListener readListener = new ReadLocalVariableListener("foo");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process",
      Variables.createVariables()
        .putValue("setListener", setListener)
        .putValue("readListener", readListener));

    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();

    // when executing the compensation handler
    taskService.complete(beforeCompensationTask.getId());

    // then the variable listener has been invoked and was able to read the variable on the end event
    readListener = (ReadLocalVariableListener) runtimeService.getVariable(processInstance.getId(), "readListener");

    assertThat(readListener.getVariableEvents()).hasSize(1);

    VariableEvent event = readListener.getVariableEvents().get(0);
    assertThat(event.getVariableName()).isEqualTo("foo");
    assertThat(event.getVariableValue()).isEqualTo("bar");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Disabled("Activity id at index 2 is 'subProcess' instead of 'subProcessEnd' - investigate")
  @Test
  void testDeleteInstanceWithEventScopeExecution()
  {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("foo")
    .startEvent("start")
    .subProcess("subProcess")
      .embeddedSubProcess()
        .startEvent("subProcessStart")
        .endEvent("subProcessEnd")
    .subProcessDone()
    .userTask("userTask")
    .done();

    modelInstance = ModifiableBpmnModelInstance.modify(modelInstance)
      .addSubProcessTo("subProcess")
      .id("eventSubProcess")
        .triggerByEvent()
        .embeddedSubProcess()
          .startEvent()
            .compensateEventDefinition()
            .compensateEventDefinitionDone()
          .endEvent()
      .done();

   testRule.deploy(modelInstance);

    long dayInMillis = 1000 * 60 * 60 * 24;
    Date date1 = new Date(10 * dayInMillis);
    ClockUtil.setCurrentTime(date1);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("foo");

    // when
    Date date2 = new Date(date1.getTime() + dayInMillis);
    ClockUtil.setCurrentTime(date2);
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    List<HistoricActivityInstance> historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
        .orderByActivityId().asc().list();
    assertThat(historicActivityInstance).hasSize(5);

    assertThat(historicActivityInstance.get(0).getActivityId()).isEqualTo("start");
    assertThat(historicActivityInstance.get(0).getEndTime()).isEqualTo(date1);
    assertThat(historicActivityInstance.get(1).getActivityId()).isEqualTo("subProcess");
    assertThat(historicActivityInstance.get(1).getEndTime()).isEqualTo(date1);
    assertThat(historicActivityInstance.get(2).getActivityId()).isEqualTo("subProcessEnd");
    assertThat(historicActivityInstance.get(2).getEndTime()).isEqualTo(date1);
    assertThat(historicActivityInstance.get(3).getActivityId()).isEqualTo("subProcessStart");
    assertThat(historicActivityInstance.get(3).getEndTime()).isEqualTo(date1);
    assertThat(historicActivityInstance.get(4).getActivityId()).isEqualTo("userTask");
    assertThat(historicActivityInstance.get(4).getEndTime()).isEqualTo(date2);


  }

  private void completeTask(String taskName) {
    completeTasks(taskName, 1);
  }

  private void completeTasks(String taskName, int times) {
    List<Task> tasks = taskService.createTaskQuery().taskName(taskName).list();

    assertThat(times)
      .as("Actual there are %d open tasks with name '%s'. Expected at least %d".formatted(tasks.size(), taskName, times))
      .isLessThanOrEqualTo(tasks.size());

    Iterator<Task> taskIterator = tasks.iterator();
    for (int i = 0; i < times; i++) {
      Task task = taskIterator.next();
      taskService.complete(task.getId());
    }
  }

  private void completeTaskWithVariable(String taskName, String variable, Object value) {
    Task task = taskService.createTaskQuery().taskName(taskName).singleResult();
    assertThat(task).as("No open task with name '%s'".formatted(taskName)).isNotNull();

    Map<String, Object> variables = new HashMap<>();
    if (variable != null) {
      variables.put(variable, value);
    }

    taskService.complete(task.getId(), variables);
  }

}
