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
package org.operaton.bpm.engine.test.bpmn.event.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionTree;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Kristin Polenz
 */
class MessageNonInterruptingBoundaryEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testSingleNonInterruptingBoundaryMessageEvent() {
    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    Task userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    assertThat(userTask).isNotNull();

    Execution execution = runtimeService.createExecutionQuery()
      .messageEventSubscriptionName("messageName")
      .singleResult();
    assertThat(execution).isNotNull();

    // 1. case: message received before completing the task

    runtimeService.messageEventReceived("messageName", execution.getId());
    // event subscription not removed
    execution = runtimeService.createExecutionQuery()
            .messageEventSubscriptionName("messageName")
            .singleResult();
    assertThat(execution).isNotNull();

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessage").singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    // send a message a second time
    runtimeService.messageEventReceived("messageName", execution.getId());
    // event subscription not removed
    execution = runtimeService.createExecutionQuery()
            .messageEventSubscriptionName("messageName")
            .singleResult();
    assertThat(execution).isNotNull();

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessage").singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    // now complete the user task with the message boundary event
    userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    assertThat(userTask).isNotNull();

    taskService.complete(userTask.getId());

    // event subscription removed
    execution = runtimeService.createExecutionQuery()
            .messageEventSubscriptionName("messageName")
            .singleResult();
    assertThat(execution).isNull();

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterTask").singleResult();
    assertThat(userTask).isNotNull();

    taskService.complete(userTask.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // 2nd. case: complete the user task cancels the message subscription

    runtimeService.startProcessInstanceByKey("process");

    userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    assertThat(userTask).isNotNull();
    taskService.complete(userTask.getId());

    execution = runtimeService.createExecutionQuery()
      .messageEventSubscriptionName("messageName")
      .singleResult();
    assertThat(execution).isNull();

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterTask").singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testNonInterruptingEventInCombinationWithReceiveTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isOne();

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);

  }

  @Deployment
  @Test
  void testNonInterruptingEventInCombinationWithReceiveTaskInConcurrentSubprocess() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);


    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    Task afterFork = taskService.createTaskQuery()
        .taskDefinitionKey("afterFork")
        .singleResult();
    taskService.complete(afterFork.getId());

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
      .createExecutionQuery()
      .activityId("task2")
      .singleResult();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(processInstanceId);

    taskService.complete(task2.getId());
    taskService.complete(task1.getId());

    testRule.assertProcessEnded(processInstanceId);

  }

  @Deployment
  @Test
  void testNonInterruptingEventInCombinationWithReceiveTaskInsideSubProcess() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = instance.getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    ActivityInstance activityInstance = runtimeService.getActivityInstance(instance.getId());
    assertThat(activityInstance).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
        .beginScope("subProcess")
          .activity("task1")
          .beginScope("innerSubProcess")
            .activity("receiveTask")
        .done());

    assertThat(taskService.createTaskQuery().count()).isOne();

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isNotEqualTo(processInstanceId);

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isNotEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isNotEqualTo(processInstanceId);

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(((ExecutionEntity) task1Execution).getParentId());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testNonInterruptingEventInCombinationWithUserTaskInsideSubProcess() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertThat(innerTask).isNotNull();

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child(null).scope()
          .child("task1").noScope().concurrent().up()
          .child(null).noScope().concurrent()
            .child("innerTask").scope()
        .done());

    // when (2)
    taskService.complete(innerTask.getId());

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();


    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child(null).scope()
          .child("task1").noScope().concurrent().up()
          .child("task2").noScope().concurrent()
        .done());

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testNonInterruptingEventInCombinationWithUserTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertThat(innerTask).isNotNull();

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
      .child("task1").noScope().concurrent().up()
      .child(null).noScope().concurrent()
        .child("innerTask").scope()
      .done());

    // when (2)
    taskService.complete(innerTask.getId());

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
      .child("task1").noScope().concurrent().up()
      .child("task2").noScope().concurrent()
    .done());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }


  @Deployment
  @Test
  void testNonInterruptingWithUserTaskAndBoundaryEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isNotEqualTo(processInstanceId);

    // when (2)
    taskService.complete(task2.getId());

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("tasks")
        .singleResult();
    assertThat(task2Execution).isNull();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testNestedEvents() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isOne();

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertThat(innerTask).isNotNull();

    Execution innerTaskExecution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertThat(((ExecutionEntity) innerTaskExecution).getParentId()).isNotEqualTo(processInstanceId);

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertThat(innerTask).isNotNull();

    innerTaskExecution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertThat(((ExecutionEntity) innerTaskExecution).getParentId()).isNotEqualTo(processInstanceId);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertThat(task1Execution).isNotNull();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    // when (3)
    runtimeService.correlateMessage("thirdMessage");

    // then (3)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertThat(task1Execution).isNotNull();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task2")
        .singleResult();
    assertThat(task2Execution).isNotNull();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/message/MessageNonInterruptingBoundaryEventTest.testNestedEvents.bpmn20.xml"})
  @Test
  void testNestedEventsAnotherExecutionOrder() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("secondMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isOne();

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertThat(task1Execution).isNotNull();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    // when (2)
    runtimeService.correlateMessage("firstMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertThat(innerTask).isNotNull();

    Execution innerTaskExecution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertThat(((ExecutionEntity) innerTaskExecution).getParentId()).isNotEqualTo(processInstanceId);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertThat(task1Execution).isNotNull();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    // when (3)
    runtimeService.correlateMessage("thirdMessage");

    // then (3)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertThat(task1).isNotNull();

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertThat(task1Execution).isNotNull();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task2")
        .singleResult();
    assertThat(task2Execution).isNotNull();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

}
