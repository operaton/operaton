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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class ProcessInstanceModificationEventTest {

  protected static final String INTERMEDIATE_TIMER_CATCH_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.intermediateTimerCatch.bpmn20.xml";
  protected static final String MESSAGE_START_EVENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.messageStart.bpmn20.xml";
  protected static final String TIMER_START_EVENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.timerStart.bpmn20.xml";
  protected static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml";
  protected static final String TERMINATE_END_EVENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.terminateEnd.bpmn20.xml";
  protected static final String CANCEL_END_EVENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.cancelEnd.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;

  @Deployment(resources = INTERMEDIATE_TIMER_CATCH_PROCESS)
  @Test
  void testStartBeforeIntermediateCatchEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("intermediateCatchEvent")
      .execute();


    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task")
        .activity("intermediateCatchEvent")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("intermediateCatchEvent").scope()
          .done());

    ActivityInstance catchEventInstance = getChildInstanceForActivity(updatedTree, "intermediateCatchEvent");

    // and there is a timer job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getExecutionId()).isEqualTo(catchEventInstance.getExecutionIds()[0]);

    completeTasksInOrder("task");
    testRule.executeAvailableJobs();
    testRule.assertProcessEnded(processInstanceId);

  }

  @Deployment(resources = MESSAGE_START_EVENT_PROCESS)
  @Test
  void testStartBeforeMessageStartEvent() {
    runtimeService.correlateMessage("startMessage");
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();

    EventSubscription startEventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(startEventSubscription).isNotNull();

    String processInstanceId = processInstance.getId();

    // when I start before the message start event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theStart")
      .execute();

    // then there are two instances of "task"
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task")
        .activity("task")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task").concurrent().noScope().up()
          .child("task").concurrent().noScope()
        .done());

    // and there is only the message start event subscription
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(subscription).isNotNull();
    assertThat(subscription.getId()).isEqualTo(startEventSubscription.getId());

    completeTasksInOrder("task", "task");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = TIMER_START_EVENT_PROCESS)
  @Test
  void testStartBeforeTimerStartEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    Job startTimerJob = managementService.createJobQuery().singleResult();
    assertThat(startTimerJob).isNotNull();

    // when I start before the timer start event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theStart")
      .execute();

    // then there are two instances of "task"
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task")
        .activity("task")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task").concurrent().noScope().up()
          .child("task").concurrent().noScope()
          .done());

    // and there is only one timer job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getId()).isEqualTo(startTimerJob.getId());

    completeTasksInOrder("task", "task");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void testStartBeforNoneStartEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // when I start before the none start event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theStart")
      .execute();

    // then there are two instances of "task"
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("theTask")
        .activity("theTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("theTask").concurrent().noScope().up()
          .child("theTask").concurrent().noScope()
          .done());

    // and the process can be ended as usual
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void testStartBeforeNoneEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // when I start before the none end event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theEnd")
      .execute();

    // then there is no effect
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("theTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree("theTask").scope()
          .done());

    completeTasksInOrder("theTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = TERMINATE_END_EVENT_PROCESS)
  @Test
  void testStartBeforeTerminateEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    // when I start before the terminate end event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("terminateEnd")
      .execute();

    // then the process instance is terminated
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNull();
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CANCEL_END_EVENT_PROCESS)
  @Test
  void testStartBeforeCancelEndEventConcurrent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    Task txTask = taskService.createTaskQuery().singleResult();
    assertThat(txTask.getTaskDefinitionKey()).isEqualTo("txTask");

    // when I start before the cancel end event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("cancelEnd")
      .execute();

    // then the subprocess instance is cancelled
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("afterCancellation")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("afterCancellation").scope()
        .done());

    Task afterCancellationTask = taskService.createTaskQuery().singleResult();
    assertThat(afterCancellationTask).isNotNull();
    assertThat(afterCancellationTask.getId()).isNotEqualTo(txTask.getId());
    assertThat(afterCancellationTask.getTaskDefinitionKey()).isEqualTo("afterCancellation");
  }

  @Deployment(resources = CANCEL_END_EVENT_PROCESS)
  @Test
  void testStartBeforeCancelEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    // complete the transaction subprocess once
    Task txTask = taskService.createTaskQuery().singleResult();
    assertThat(txTask.getTaskDefinitionKey()).isEqualTo("txTask");

    taskService.complete(txTask.getId(), Variables.createVariables().putValue("success", true));

    Task afterSuccessTask = taskService.createTaskQuery().singleResult();
    assertThat(afterSuccessTask.getTaskDefinitionKey()).isEqualTo("afterSuccess");

    // when I start before the cancel end event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("cancelEnd")
      .execute();

    // then a new subprocess instance is created and immediately cancelled
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("afterCancellation")
        .activity("afterSuccess")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("afterCancellation").concurrent().noScope().up()
        .child("afterSuccess").concurrent().noScope().up()
        .child("tx").scope().eventScope()
      .done());

    // the compensation for the completed tx has not been triggered
    assertThat(taskService.createTaskQuery().taskDefinitionKey("undoTxTask").count()).isZero();

    // complete the process
    Task afterCancellationTask = taskService.createTaskQuery().taskDefinitionKey("afterCancellation").singleResult();
    assertThat(afterCancellationTask).isNotNull();

    taskService.complete(afterCancellationTask.getId());
    taskService.complete(afterSuccessTask.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  protected ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      if (childInstance.getActivityId().equals(activityId)) {
        return childInstance;
      }
    }

    return null;
  }

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      assertThat(!tasks.isEmpty()).as("task for activity %s does not exist".formatted(taskName)).isTrue();
      taskService.complete(tasks.get(0).getId());
    }
  }
}
