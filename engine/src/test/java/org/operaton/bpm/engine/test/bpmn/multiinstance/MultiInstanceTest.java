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
package org.operaton.bpm.engine.test.bpmn.multiinstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.utils.CollectionUtil;

import static org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate.leaveExecution;
import static org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate.throwError;
import static org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate.throwException;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Joram Barrez
 * @author Bernd Ruecker
 */
class MultiInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml"})
  @Test
  void testSequentialUserTasks() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks",
            CollectionUtil.singletonMap("nrOfLoops", 3));
    String procId = processInstance.getId();

    // now there is now 1 activity instance below the pi:
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstance expectedTree = describeActivityInstanceTree(processInstance.getProcessDefinitionId())
      .beginMiBody("miTasks")
        .activity("miTasks")
    .done();
    assertThat(tree).hasStructure(expectedTree);

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");
    assertThat(task.getAssignee()).isEqualTo("kermit_0");
    taskService.complete(task.getId());

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(expectedTree);

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");
    assertThat(task.getAssignee()).isEqualTo("kermit_1");
    taskService.complete(task.getId());

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(expectedTree);

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");
    assertThat(task.getAssignee()).isEqualTo("kermit_2");
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().singleResult()).isNull();
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml"})
  @Test
  void testSequentialUserTasksHistory() {
    runtimeService.startProcessInstanceByKey("miSequentialUserTasks",
            CollectionUtil.singletonMap("nrOfLoops", 4)).getId();
    for (int i=0; i<4; i++) {
      taskService.complete(taskService.createTaskQuery().singleResult().getId());
    }

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
      assertThat(historicActivityInstances).hasSize(4);
      for (HistoricActivityInstance hai : historicActivityInstances) {
        assertThat(hai.getActivityId()).isNotNull();
        assertThat(hai.getActivityName()).isNotNull();
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getEndTime()).isNotNull();
        assertThat(hai.getAssignee()).isNotNull();
      }

    }

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
      assertThat(historicTaskInstances).hasSize(4);
      for (HistoricTaskInstance ht : historicTaskInstances) {
        assertThat(ht.getAssignee()).isNotNull();
        assertThat(ht.getStartTime()).isNotNull();
        assertThat(ht.getEndTime()).isNotNull();
      }

    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml"})
  @Test
  void testSequentialUserTasksWithTimer() {
    String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks",
            CollectionUtil.singletonMap("nrOfLoops", 3)).getId();

    // Complete 1 tasks
    taskService.complete(taskService.createTaskQuery().singleResult().getId());

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml"})
  @Test
  void testSequentialUserTasksCompletionCondition() {
    String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks",
            CollectionUtil.singletonMap("nrOfLoops", 10)).getId();

    // 10 tasks are to be created, but completionCondition stops them at 5
    for (int i=0; i<5; i++) {
      Task task = taskService.createTaskQuery().singleResult();
      taskService.complete(task.getId());
    }
    assertThat(taskService.createTaskQuery().singleResult()).isNull();
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testSequentialMITasksExecutionListener() {
    RecordInvocationListener.reset();

    Map<String, Object> vars = new HashMap<>();
    vars.put("nrOfLoops", 2);
    runtimeService.startProcessInstanceByKey("miSequentialListener", vars);

    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_START)).isEqualTo(1);
    assertThat(RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_END)).isNull();

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_START)).isEqualTo(2);
    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_END)).isEqualTo(1);

    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_START)).isEqualTo(2);
    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_END)).isEqualTo(2);
  }

  @Deployment
  @Test
  void testParallelMITasksExecutionListener() {
    RecordInvocationListener.reset();

    Map<String, Object> vars = new HashMap<>();
    vars.put("nrOfLoops", 5);
    runtimeService.startProcessInstanceByKey("miSequentialListener", vars);

    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_START)).isEqualTo(5);
    assertThat(RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_END)).isNull();

    List<Task> tasks = taskService.createTaskQuery().list();
    taskService.complete(tasks.get(0).getId());

    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_START)).isEqualTo(5);
    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_END)).isEqualTo(1);

    taskService.complete(tasks.get(1).getId());
    taskService.complete(tasks.get(2).getId());
    taskService.complete(tasks.get(3).getId());
    taskService.complete(tasks.get(4).getId());

    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_START)).isEqualTo(5);
    assertThat((int) RecordInvocationListener.INVOCATIONS.get(ExecutionListener.EVENTNAME_END)).isEqualTo(5);
  }

  @Deployment
  @Test
  void testNestedSequentialUserTasks() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialUserTasks").getId();

    for (int i=0; i<3; i++) {
      Task task = taskService.createTaskQuery().taskAssignee("kermit").singleResult();
      assertThat(task.getName()).isEqualTo("My Task");
      ActivityInstance processInstance = runtimeService.getActivityInstance(procId);
      List<ActivityInstance> instancesForActivityId = testRule.getInstancesForActivityId(processInstance, "miTasks");
      assertThat(instancesForActivityId).hasSize(1);
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testParallelUserTasks() {
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    String procId = procInst.getId();

    List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getName()).isEqualTo("My Task 0");
    assertThat(tasks.get(1).getName()).isEqualTo("My Task 1");
    assertThat(tasks.get(2).getName()).isEqualTo("My Task 2");

    ActivityInstance processInstance = runtimeService.getActivityInstance(procId);
    assertThat(processInstance.getActivityInstances("miTasks")).hasSize(3);

    taskService.complete(tasks.get(0).getId());

    processInstance = runtimeService.getActivityInstance(procId);

    assertThat(processInstance.getActivityInstances("miTasks")).hasSize(2);

    taskService.complete(tasks.get(1).getId());

    processInstance = runtimeService.getActivityInstance(procId);
    assertThat(processInstance.getActivityInstances("miTasks")).hasSize(1);

    taskService.complete(tasks.get(2).getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testParallelReceiveTasks() {
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("miParallelReceiveTasks");
    String procId = procInst.getId();

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(3);

    List<Execution> receiveTaskExecutions = runtimeService
        .createExecutionQuery().activityId("miTasks").list();

    for (Execution execution : receiveTaskExecutions) {
      runtimeService.messageEventReceived("message", execution.getId());
    }
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelReceiveTasks.bpmn20.xml")
  @Test
  void testParallelReceiveTasksAssertEventSubscriptionRemoval() {
    runtimeService.startProcessInstanceByKey("miParallelReceiveTasks");

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(3);

    List<Execution> receiveTaskExecutions = runtimeService
        .createExecutionQuery().activityId("miTasks").list();

    // signal one of the instances
    runtimeService.messageEventReceived("message", receiveTaskExecutions.get(0).getId());

    // now there should be two subscriptions left
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelUserTasks.bpmn20.xml"})
  @Test
  void testParallelUserTasksHistory() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    for (Task task : taskService.createTaskQuery().list()) {
      taskService.complete(task.getId());
    }

    // Validate history
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().list();
      for (int i=0; i<historicTaskInstances.size(); i++) {
        HistoricTaskInstance hi = historicTaskInstances.get(i);
        assertThat(hi.getStartTime()).isNotNull();
        assertThat(hi.getEndTime()).isNotNull();
        assertThat(hi.getAssignee()).isEqualTo("kermit_" + i);
      }

      HistoricActivityInstance multiInstanceBodyInstance = historyService.createHistoricActivityInstanceQuery()
          .activityId("miTasks#multiInstanceBody").singleResult();
      assertThat(multiInstanceBodyInstance).isNotNull();
      assertThat(multiInstanceBodyInstance.getParentActivityInstanceId()).isEqualTo(pi.getId());

      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
      assertThat(historicActivityInstances).hasSize(3);
      for (HistoricActivityInstance hai : historicActivityInstances) {
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getEndTime()).isNotNull();
        assertThat(hai.getAssignee()).isNotNull();
        assertThat(hai.getActivityType()).isEqualTo("userTask");
        assertThat(hai.getParentActivityInstanceId()).isEqualTo(multiInstanceBodyInstance.getId());
        assertThat(hai.getTaskId()).isNotNull();
      }
    }
  }

  @Deployment
  @Test
  void testParallelUserTasksWithTimer() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksWithTimer").getId();

    List<Task> tasks = taskService.createTaskQuery().list();
    taskService.complete(tasks.get(0).getId());

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testParallelUserTasksCompletionCondition() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksCompletionCondition").getId();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(5);

    // Completing 3 tasks gives 50% of tasks completed, which triggers completionCondition
    for (int i=0; i<3; i++) {
      assertThat(taskService.createTaskQuery().count()).isEqualTo(5 - i);
      taskService.complete(tasks.get(i).getId());
    }
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testParallelUserTasksBasedOnCollection() {
    List<String> assigneeList = List.of("kermit", "gonzo", "mispiggy", "fozzie", "bubba");
    String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksBasedOnCollection",
          CollectionUtil.singletonMap("assigneeList", assigneeList)).getId();

    List<Task> tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
    assertThat(tasks).hasSize(5);
    assertThat(tasks.get(0).getAssignee()).isEqualTo("bubba");
    assertThat(tasks.get(1).getAssignee()).isEqualTo("fozzie");
    assertThat(tasks.get(2).getAssignee()).isEqualTo("gonzo");
    assertThat(tasks.get(3).getAssignee()).isEqualTo("kermit");
    assertThat(tasks.get(4).getAssignee()).isEqualTo("mispiggy");

    // Completing 3 tasks will trigger completioncondition
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    taskService.complete(tasks.get(2).getId());
    assertThat(taskService.createTaskQuery().count()).isZero();
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelUserTasksBasedOnCollection.bpmn20.xml")
  @Test
  void testEmptyCollectionInMI() {
    List<String> assigneeList = new ArrayList<>();
    String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksBasedOnCollection",
      CollectionUtil.singletonMap("assigneeList", assigneeList)).getId();

    assertThat(taskService.createTaskQuery().count()).isZero();
    testRule.assertProcessEnded(procId);
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> activities = historyService
          .createHistoricActivityInstanceQuery()
          .processInstanceId(procId)
          .orderByActivityId()
          .asc().list();
      assertThat(activities).hasSize(3);
      assertThat(activities.get(0).getActivityId()).isEqualTo("miTasks#multiInstanceBody");
      assertThat(activities.get(1).getActivityId()).isEqualTo("theEnd");
      assertThat(activities.get(2).getActivityId()).isEqualTo("theStart");
    }
  }

  @Deployment
  @Test
  void testParallelUserTasksBasedOnCollectionExpression() {
    DelegateEvent.clearEvents();

    runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("myBean", new DelegateBean()));

    List<DelegateEvent> recordedEvents = DelegateEvent.getEvents();
    assertThat(recordedEvents).hasSize(2);

    assertThat(recordedEvents.get(0).getCurrentActivityId()).isEqualTo("miTasks#multiInstanceBody");
    assertThat(recordedEvents.get(1).getCurrentActivityId()).isEqualTo("miTasks#multiInstanceBody"); // or miTasks

    DelegateEvent.clearEvents();
  }

  @Test
  void testSequentialUserTasksBasedOnCollectionExpression() {
    DelegateEvent.clearEvents();

    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask("miTasks")
            .multiInstance()
            .sequential()
            .operatonCollection("${myBean.resolveCollection(execution)}")
            .operatonElementVariable("elementVar")
            .multiInstanceDone()
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("myBean", new DelegateBean()));

    Task singleResult = taskService.createTaskQuery().singleResult();
    taskService.complete(singleResult.getId());
    List<DelegateEvent> recordedEvents = DelegateEvent.getEvents();
    assertThat(recordedEvents).hasSize(2);

    assertThat(recordedEvents.get(0).getCurrentActivityId()).isEqualTo("miTasks#multiInstanceBody");
    assertThat(recordedEvents.get(1).getCurrentActivityId()).isEqualTo("miTasks#multiInstanceBody"); // or miTasks

    DelegateEvent.clearEvents();
  }

  @Deployment
  @Test
  void testParallelUserTasksCustomExtensions() {
    Map<String, Object> vars = new HashMap<>();
    List<String> assigneeList = List.of("kermit", "gonzo", "fozzie");
    vars.put("assigneeList", assigneeList);
    runtimeService.startProcessInstanceByKey("miSequentialUserTasks", vars);

    for (String assignee : assigneeList) {
      Task task = taskService.createTaskQuery().singleResult();
      assertThat(task.getAssignee()).isEqualTo(assignee);
      taskService.complete(task.getId());
    }
  }

  @Deployment
  @Test
  void testParallelUserTasksExecutionAndTaskListeners() {
    runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    Execution waitState = runtimeService.createExecutionQuery().singleResult();
    assertThat(runtimeService.getVariable(waitState.getId(), "taskListenerCounter")).isEqualTo(3);
    assertThat(runtimeService.getVariable(waitState.getId(), "executionListenerCounter")).isEqualTo(3);
  }

  @Deployment
  @Test
  void testNestedParallelUserTasks() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedParallelUserTasks").getId();

    List<Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
    for (Task task : tasks) {
      assertThat(task.getName()).isEqualTo("My Task");
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @ParameterizedTest
  @CsvSource({
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialScriptTasks.bpmn20.xml, miSequentialScriptTask, 5, 10",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialScriptTasks.bpmn20.xml, miSequentialScriptTask, 200, 19900",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelScriptTasks.bpmn20.xml, miParallelScriptTask, 10, 45",
  })
  void scriptTaskShouldSumAfterLoop (String bpmnResource, String processDefinitionKey, int nrOfLoops, int expectedSum) {
    // given
    testRule.deploy(bpmnResource);
    Map<String, Object> vars = new HashMap<>();
    vars.put("sum", 0);
    vars.put("nrOfLoops", nrOfLoops);

    // when
    runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);
    Execution waitStateExecution = runtimeService.createExecutionQuery().singleResult();
    int sum = (Integer) runtimeService.getVariable(waitStateExecution.getId(), "sum");

    // then
    assertThat(sum).isEqualTo(expectedSum);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialScriptTasks.bpmn20.xml"})
  @Test
  void testSequentialScriptTasksHistory() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("sum", 0);
    vars.put("nrOfLoops", 7);
    runtimeService.startProcessInstanceByKey("miSequentialScriptTask", vars);

    // Validate history
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").orderByActivityId().asc().list();
      assertThat(historicInstances).hasSize(7);
      for (int i=0; i<7; i++) {
        HistoricActivityInstance hai = historicInstances.get(i);
        assertThat(hai.getActivityType()).isEqualTo("scriptTask");
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getEndTime()).isNotNull();
      }
    }
  }

  @Deployment
  @Test
  void testSequentialScriptTasksCompletionCondition() {
    runtimeService.startProcessInstanceByKey("miSequentialScriptTaskCompletionCondition").getId();
    Execution waitStateExecution = runtimeService.createExecutionQuery().singleResult();
    int sum = (Integer) runtimeService.getVariable(waitStateExecution.getId(), "sum");
    assertThat(sum).isEqualTo(5);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelScriptTasks.bpmn20.xml"})
  @Test
  void testParallelScriptTasksHistory() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("sum", 0);
    vars.put("nrOfLoops", 4);
    runtimeService.startProcessInstanceByKey("miParallelScriptTask", vars);
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").list();
      assertThat(historicActivityInstances).hasSize(4);
      for (HistoricActivityInstance hai : historicActivityInstances) {
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getStartTime()).isNotNull();
      }
    }
  }

  @Deployment
  @Test
  void testParallelScriptTasksCompletionCondition() {
    runtimeService.startProcessInstanceByKey("miParallelScriptTaskCompletionCondition");
    Execution waitStateExecution = runtimeService.createExecutionQuery().singleResult();
    int sum = (Integer) runtimeService.getVariable(waitStateExecution.getId(), "sum");
    assertThat(sum).isEqualTo(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelScriptTasksCompletionCondition.bpmn20.xml"})
  @Test
  void testParallelScriptTasksCompletionConditionHistory() {
    runtimeService.startProcessInstanceByKey("miParallelScriptTaskCompletionCondition");
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").list();
      assertThat(historicActivityInstances).hasSize(2);
    }
  }

  @Deployment
  @Test
  void testSequentialSubProcess() {
    String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocess").getId();

    TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
    for (int i=0; i<4; i++) {
      List<Task> tasks = query.list();
      assertThat(tasks).hasSize(2);

      assertThat(tasks.get(0).getName()).isEqualTo("task one");
      assertThat(tasks.get(1).getName()).isEqualTo("task two");

      taskService.complete(tasks.get(0).getId());
      taskService.complete(tasks.get(1).getId());

      if(i != 3) {
        List<String> activities = runtimeService.getActiveActivityIds(procId);
        assertThat(activities)
                .isNotNull()
                .hasSize(2);
      }
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testSequentialSubProcessEndEvent() {
    // ACT-1185: end-event in subprocess causes inactivated execution
    String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocess").getId();

    TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
    for (int i=0; i<4; i++) {
      List<Task> tasks = query.list();
      assertThat(tasks).hasSize(1);

      assertThat(tasks.get(0).getName()).isEqualTo("task one");

      taskService.complete(tasks.get(0).getId());

      // Last run, the execution no longer exists
      if(i != 3) {
        List<String> activities = runtimeService.getActiveActivityIds(procId);
        assertThat(activities)
                .isNotNull()
                .hasSize(1);
      }
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialSubProcess.bpmn20.xml"})
  @Test
  void testSequentialSubProcessHistory() {
    runtimeService.startProcessInstanceByKey("miSequentialSubprocess");
    for (int i=0; i<4; i++) {
      List<Task> tasks = taskService.createTaskQuery().list();
      taskService.complete(tasks.get(0).getId());
      taskService.complete(tasks.get(1).getId());
    }

    // Validate history
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> onlySubProcessInstances = historyService.createHistoricActivityInstanceQuery().activityType("subProcess").list();
      assertThat(onlySubProcessInstances).hasSize(4);

      List<HistoricActivityInstance> historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("subProcess").list();
      assertThat(historicInstances).hasSize(4);
      for (HistoricActivityInstance hai : historicInstances) {
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getEndTime()).isNotNull();
      }

      historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
      assertThat(historicInstances).hasSize(8);
      for (HistoricActivityInstance hai : historicInstances) {
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getEndTime()).isNotNull();
      }
    }
  }

  @Deployment
  @Test
  void testSequentialSubProcessWithTimer() {
    String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessWithTimer").getId();

    // Complete one subprocess
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testSequentialSubProcessCompletionCondition() {
    String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();

    TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
    for (int i=0; i<3; i++) {
      List<Task> tasks = query.list();
      assertThat(tasks).hasSize(2);

      assertThat(tasks.get(0).getName()).isEqualTo("task one");
      assertThat(tasks.get(1).getName()).isEqualTo("task two");

      taskService.complete(tasks.get(0).getId());
      taskService.complete(tasks.get(1).getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testNestedSequentialSubProcess() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialSubProcess").getId();

    for (int i=0; i<3; i++) {
      List<Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
      taskService.complete(tasks.get(0).getId());
      taskService.complete(tasks.get(1).getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testNestedSequentialSubProcessWithTimer() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialSubProcessWithTimer").getId();

    for (int i=0; i<2; i++) {
      List<Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
      taskService.complete(tasks.get(0).getId());
      taskService.complete(tasks.get(1).getId());
    }

    // Complete one task, to make it a bit more trickier
    List<Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
    taskService.complete(tasks.get(0).getId());

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testParallelSubProcess() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocess").getId();
    List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(4);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelSubProcess.bpmn20.xml"})
  @Test
  void testParallelSubProcessHistory() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("miParallelSubprocess");

    // Validate history
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("miSubProcess").list();
      assertThat(historicActivityInstances).hasSize(2);
      for (HistoricActivityInstance hai : historicActivityInstances) {
        assertThat(hai.getStartTime()).isNotNull();
        // now end time is null
        assertThat(hai.getEndTime()).isNull();
        assertThat(hai.getParentActivityInstanceId()).as(pi.getId()).isNotNull();
      }
    }

    for (Task task : taskService.createTaskQuery().list()) {
      taskService.complete(task.getId());
    }

    // Validate history
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("miSubProcess").list();
      assertThat(historicActivityInstances).hasSize(2);
      for (HistoricActivityInstance hai : historicActivityInstances) {
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getEndTime()).isNotNull();
        assertThat(hai.getParentActivityInstanceId()).as(pi.getId()).isNotNull();
      }
    }
  }

  @Deployment
  @Test
  void testParallelSubProcessWithTimer() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessWithTimer").getId();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(6);

    // Complete two tasks
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testParallelSubProcessCompletionCondition() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessCompletionCondition").getId();

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(4);

    // get activities of a single subprocess
    ActivityInstance[] taskActivities = runtimeService.getActivityInstance(procId)
      .getActivityInstances("miSubProcess")[0]
      .getChildActivityInstances();

    for (ActivityInstance taskActivity : taskActivities) {
      Task task = taskService.createTaskQuery().activityInstanceIdIn(taskActivity.getId()).singleResult();
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testParallelSubProcessAllAutomatic() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessAllAutomatics",
            CollectionUtil.singletonMap("nrOfLoops", 5)).getId();
    Execution waitState = runtimeService.createExecutionQuery().singleResult();
    assertThat(runtimeService.getVariable(waitState.getId(), "sum")).isEqualTo(10);

    runtimeService.signal(waitState.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelSubProcessAllAutomatic.bpmn20.xml"})
  @Test
  void testParallelSubProcessAllAutomaticCompletionCondition() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessAllAutomatics",
            CollectionUtil.singletonMap("nrOfLoops", 10)).getId();
    Execution waitState = runtimeService.createExecutionQuery().singleResult();
    assertThat(runtimeService.getVariable(waitState.getId(), "sum")).isEqualTo(12);

    runtimeService.signal(waitState.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testNestedParallelSubProcess() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedParallelSubProcess").getId();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(8);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(procId);
  }

  @ParameterizedTest
  @CsvSource({
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelSubProcessWithTimer.bpmn20.xml, miNestedParallelSubProcess, 12, 3",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivityWithTimer.bpmn20.xml, miParallelCallActivity, 6, 2",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivityWithTimer.bpmn20.xml, miNestedParallelCallActivityWithTimer, 4, 3"
  })
  void parallelExecutionWithTimer (String bpmnResource, String processDefinitionKey, int expectedTaskCount, int completeTaskCount) {
    testRule.deploy(bpmnResource);
    testRule.deploy("org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml");

    String procId = runtimeService.startProcessInstanceByKey(processDefinitionKey).getId();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(expectedTaskCount);

    for (int i=0; i<completeTaskCount; i++) {
      taskService.complete(tasks.get(i).getId());
    }

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testSequentialCallActivity() {
    String procId = runtimeService.startProcessInstanceByKey("miSequentialCallActivity").getId();

    for (int i=0; i<3; i++) {
      List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
      assertThat(tasks).hasSize(2);
      assertThat(tasks.get(0).getName()).isEqualTo("task one");
      assertThat(tasks.get(1).getName()).isEqualTo("task two");
      taskService.complete(tasks.get(0).getId());
      taskService.complete(tasks.get(1).getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivityWithList.bpmn20.xml")
  @Test
  void testSequentialCallActivityWithList() {
    ArrayList<String> list = new ArrayList<>();
    list.add("one");
    list.add("two");

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("list", list);

    String procId = runtimeService.startProcessInstanceByKey("parentProcess", variables).getId();

    Task task1 = taskService.createTaskQuery().processVariableValueEquals("element", "one").singleResult();
    Task task2 = taskService.createTaskQuery().processVariableValueEquals("element", "two").singleResult();

    assertThat(task1).isNotNull();
    assertThat(task2).isNotNull();

    HashMap<String, Object> subVariables = new HashMap<>();
    subVariables.put("x", "y");

    taskService.complete(task1.getId(), subVariables);
    taskService.complete(task2.getId(), subVariables);

    Task task3 = taskService.createTaskQuery().processDefinitionKey("midProcess").singleResult();
    assertThat(task3).isNotNull();
    taskService.complete(task3.getId());

    Task task4 = taskService.createTaskQuery().processDefinitionKey("parentProcess").singleResult();
    assertThat(task4).isNotNull();
    taskService.complete(task4.getId());

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivityWithTimer.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testSequentialCallActivityWithTimer() {
    String procId = runtimeService.startProcessInstanceByKey("miSequentialCallActivityWithTimer").getId();

    // Complete first subprocess
    List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("task one");
    assertThat(tasks.get(1).getName()).isEqualTo("task two");
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testParallelCallActivity() {
    String procId = runtimeService.startProcessInstanceByKey("miParallelCallActivity").getId();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(12);
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testParallelCallActivityHistory() {
    runtimeService.startProcessInstanceByKey("miParallelCallActivity");
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(12);
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      // Validate historic processes
      List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
      assertThat(historicProcessInstances).hasSize(7); // 6 subprocesses + main process
      for (HistoricProcessInstance hpi : historicProcessInstances) {
        assertThat(hpi.getStartTime()).isNotNull();
        assertThat(hpi.getEndTime()).isNotNull();
      }

      // Validate historic activities
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("callActivity").list();
      assertThat(historicActivityInstances).hasSize(6);
      for (HistoricActivityInstance hai : historicActivityInstances) {
        assertThat(hai.getStartTime()).isNotNull();
        assertThat(hai.getEndTime()).isNotNull();
      }
    }

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      // Validate historic tasks
      List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
      assertThat(historicTaskInstances).hasSize(12);
      for (HistoricTaskInstance hti : historicTaskInstances) {
        assertThat(hti.getStartTime()).isNotNull();
        assertThat(hti.getEndTime()).isNotNull();
        assertThat(hti.getAssignee()).isNotNull();
        assertThat(hti.getDeleteReason()).isEqualTo("completed");
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedSequentialCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testNestedSequentialCallActivity() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialCallActivity").getId();

    for (int i=0; i<4; i++) {
      List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
      assertThat(tasks).hasSize(2);
      assertThat(tasks.get(0).getName()).isEqualTo("task one");
      assertThat(tasks.get(1).getName()).isEqualTo("task two");
      taskService.complete(tasks.get(0).getId());
      taskService.complete(tasks.get(1).getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedSequentialCallActivityWithTimer.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testNestedSequentialCallActivityWithTimer() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialCallActivityWithTimer").getId();

    // first instance
    List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("task one");
    assertThat(tasks.get(1).getName()).isEqualTo("task two");
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // one task of second instance
    tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    taskService.complete(tasks.get(0).getId());

    // Fire timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    Task taskAfterTimer = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
    taskService.complete(taskAfterTimer.getId());

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testNestedParallelCallActivity() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivity").getId();

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(14);
    for (int i = 0; i < 14; i++) {
      taskService.complete(tasks.get(i).getId());
    }

    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivityCompletionCondition.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  @Test
  void testNestedParallelCallActivityCompletionCondition() {
    String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivityCompletionCondition").getId();

    assertThat(taskService.createTaskQuery().count()).isEqualTo(8);

    for (int i = 0; i < 2; i++) {
      ProcessInstance nextSubProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("externalSubProcess").listPage(0, 1).get(0);
      List<Task> tasks = taskService.createTaskQuery().processInstanceId(nextSubProcessInstance.getId()).list();
      assertThat(tasks).hasSize(2);
      for (Task task : tasks) {
        taskService.complete(task.getId());
      }
    }

    testRule.assertProcessEnded(procId);
  }

  // ACT-764
  @Deployment
  @Test
  void testSequentialServiceTaskWithClass() {
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("multiInstanceServiceTask", CollectionUtil.singletonMap("result", 5));
    Integer result = (Integer) runtimeService.getVariable(procInst.getId(), "result");
    assertThat(result.intValue()).isEqualTo(160);

    runtimeService.signal(procInst.getId());
    testRule.assertProcessEnded(procInst.getId());
  }

  @Deployment
  @Test
  void testSequentialServiceTaskWithClassAndCollection() {
    Collection<Integer> items = List.of(1,2,3,4,5,6);
    Map<String, Object> vars = new HashMap<>();
    vars.put("result", 1);
    vars.put("items", items);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("multiInstanceServiceTask", vars);
    Integer result = (Integer) runtimeService.getVariable(procInst.getId(), "result");
    assertThat(result.intValue()).isEqualTo(720);

    runtimeService.signal(procInst.getId());
    testRule.assertProcessEnded(procInst.getId());
  }

  // ACT-901
  @Deployment
  @Test
  void testAct901() {

    Date startTime = ClockUtil.getCurrentTime();

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("multiInstanceSubProcess");
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();
    assertThat(tasks).hasSize(5);

    ClockUtil.setCurrentTime(new Date(startTime.getTime() + 61000L)); // timer is set to one minute
    List<Job> timers = managementService.createJobQuery().list();
    assertThat(timers).hasSize(5);

    // Execute all timers one by one (single thread vs thread pool of job executor, which leads to optimisticlockingexceptions!)
    for (Job timer : timers) {
      managementService.executeJob(timer.getId());
    }

    // All tasks should be canceled
    tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();
    assertThat(tasks).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.callActivityWithBoundaryErrorEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.throwingErrorEventSubProcess.bpmn20.xml"})
  @Test
  void testMultiInstanceCallActivityWithErrorBoundaryEvent() {
    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("assignees", List.of("kermit", "gonzo"));

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    // finish first call activity with error
    variableMap = new HashMap<>();
    variableMap.put("done", false);
    taskService.complete(tasks.get(0).getId(), variableMap);

    tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);

    taskService.complete(tasks.get(0).getId());

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").list();
    assertThat(processInstances).isEmpty();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.callActivityWithBoundaryErrorEventSequential.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.throwingErrorEventSubProcess.bpmn20.xml"})
  @Test
  void testSequentialMultiInstanceCallActivityWithErrorBoundaryEvent() {
    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("assignees", List.of("kermit", "gonzo"));

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);

    // finish first call activity with error
    variableMap = new HashMap<>();
    variableMap.put("done", false);
    taskService.complete(tasks.get(0).getId(), variableMap);

    tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);

    taskService.complete(tasks.get(0).getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedMultiInstanceTasks.bpmn20.xml"})
  @Test
  void testNestedMultiInstanceTasks() {
    List<String> processes = List.of("process A", "process B");
    List<String> assignees = List.of("kermit", "gonzo");
    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("subProcesses", processes);
    variableMap.put("assignees", assignees);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miNestedMultiInstanceTasks", variableMap);

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(processes.size() * assignees.size());

    for (Task t : tasks) {
      taskService.complete(t.getId());
    }

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("miNestedMultiInstanceTasks").list();
    assertThat(processInstances).isEmpty();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedMultiInstanceTasks.bpmn20.xml"})
  @Test
  void testNestedMultiInstanceTasksActivityInstance() {
    List<String> processes = List.of("process A", "process B");
    List<String> assignees = List.of("kermit", "gonzo");
    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("subProcesses", processes);
    variableMap.put("assignees", assignees);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miNestedMultiInstanceTasks", variableMap);

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstanceAssert.assertThat(activityInstance)
    .hasStructure(
        ActivityInstanceAssert
        .describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("subprocess1")
          .beginScope("subprocess1")
            .beginMiBody("miTasks")
              .activity("miTasks")
              .activity("miTasks")
            .endScope()
          .endScope()
          .beginScope("subprocess1")
            .beginMiBody("miTasks")
              .activity("miTasks")
              .activity("miTasks")
            .endScope()
          .endScope()
        .done());

  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelUserTasks.bpmn20.xml"})
  @Test
  void testActiveExecutionsInParallelTasks() {
    runtimeService.startProcessInstanceByKey("miParallelUserTasks").getId();

    ProcessInstance instance = runtimeService.createProcessInstanceQuery().singleResult();

    List<Execution> executions = runtimeService.createExecutionQuery().list();
    assertThat(executions).hasSize(5);

    for (Execution execution : executions) {
      ExecutionEntity entity = (ExecutionEntity) execution;

      if (!entity.getId().equals(instance.getId()) && !entity.getParentId().equals(instance.getId())) {
        // child executions
        assertThat(entity.isActive()).isTrue();
      } else {
        // process instance and scope execution
        assertThat(entity.isActive()).isFalse();
      }
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownByExecuteOfSequentialAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwException()).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByExecuteOfSequentialAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwError()).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownBySignalOfSequentialAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    // signal 2 times to execute first sequential behaviors
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownBySignalOfSequentialAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    // signal 2 times to execute first sequential behaviors
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownByExecuteOfParallelAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwException()).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByExecuteOfParallelAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwError()).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownBySignalOfParallelAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").list().get(3);
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownBySignalOfParallelAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").list().get(3);
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownByExecuteOfSequentialDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwException());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByExecuteOfSequentialDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwError());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownBySignalOfSequentialDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    // signal 2 times to execute first sequential behaviors
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownBySequentialDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownBySignalOfSequentialDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    // signal 2 times to execute first sequential behaviors
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());
    runtimeService.setVariables(pi, leaveExecution());
    runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownByExecuteOfParallelDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwException());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByExecuteOfParallelDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwError());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownBySignalOfParallelDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").list().get(3);
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/multiinstance/MultiInstanceTest.testCatchErrorThrownByParallelDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownBySignalOfParallelDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").list().get(3);
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }
}
