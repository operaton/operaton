/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.bpmn.event.message;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 *
 * @author Kristin Polenz
 */
public class MessageNonInterruptingBoundaryEventTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testSingleNonInterruptingBoundaryMessageEvent() {
    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    Task userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    assertNotNull(userTask);

    Execution execution = runtimeService.createExecutionQuery()
      .messageEventSubscriptionName("messageName")
      .singleResult();
    assertNotNull(execution);

    // 1. case: message received before completing the task

    runtimeService.messageEventReceived("messageName", execution.getId());
    // event subscription not removed
    execution = runtimeService.createExecutionQuery()
            .messageEventSubscriptionName("messageName")
            .singleResult();
    assertNotNull(execution);

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessage").singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    // send a message a second time
    runtimeService.messageEventReceived("messageName", execution.getId());
    // event subscription not removed
    execution = runtimeService.createExecutionQuery()
            .messageEventSubscriptionName("messageName")
            .singleResult();
    assertNotNull(execution);

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessage").singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    // now complete the user task with the message boundary event
    userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    assertNotNull(userTask);

    taskService.complete(userTask.getId());

    // event subscription removed
    execution = runtimeService.createExecutionQuery()
            .messageEventSubscriptionName("messageName")
            .singleResult();
    assertNull(execution);

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterTask").singleResult();
    assertNotNull(userTask);

    taskService.complete(userTask.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // 2nd. case: complete the user task cancels the message subscription

    runtimeService.startProcessInstanceByKey("process");

    userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    assertNotNull(userTask);
    taskService.complete(userTask.getId());

    execution = runtimeService.createExecutionQuery()
      .messageEventSubscriptionName("messageName")
      .singleResult();
    assertNull(execution);

    userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterTask").singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testNonInterruptingEventInCombinationWithReceiveTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

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
    assertNotNull(task1);

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);

  }

  @Deployment
  @Test
  public void testNonInterruptingEventInCombinationWithReceiveTaskInConcurrentSubprocess() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);


    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    Task afterFork = taskService.createTaskQuery()
        .taskDefinitionKey("afterFork")
        .singleResult();
    taskService.complete(afterFork.getId());

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

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
  public void testNonInterruptingEventInCombinationWithReceiveTaskInsideSubProcess() {
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

    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertNotEquals(processInstanceId, ((ExecutionEntity) task1Execution).getParentId());

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertNotEquals(processInstanceId, ((ExecutionEntity) task1Execution).getParentId());

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertNotEquals(processInstanceId, ((ExecutionEntity) task2Execution).getParentId());

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(((ExecutionEntity) task1Execution).getParentId());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testNonInterruptingEventInCombinationWithUserTaskInsideSubProcess() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertNotNull(innerTask);

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
    assertNotNull(task1);


    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

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
  public void testNonInterruptingEventInCombinationWithUserTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertNotNull(innerTask);

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
    assertNotNull(task1);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

    executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
      .child("task1").noScope().concurrent().up()
      .child("task2").noScope().concurrent()
    .done());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }


  @Deployment
  @Test
  public void testNonInterruptingWithUserTaskAndBoundaryEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertNotNull(task2);

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertNotEquals(processInstanceId, ((ExecutionEntity) task2Execution).getParentId());

    // when (2)
    taskService.complete(task2.getId());

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

    task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("tasks")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testNestedEvents() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertNotNull(innerTask);

    Execution innerTaskExecution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertNotEquals(processInstanceId, ((ExecutionEntity) innerTaskExecution).getParentId());

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertNotNull(innerTask);

    innerTaskExecution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertNotEquals(processInstanceId, ((ExecutionEntity) innerTaskExecution).getParentId());

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertNotNull(task1Execution);

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    // when (3)
    runtimeService.correlateMessage("thirdMessage");

    // then (3)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertNotNull(task1Execution);

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task2")
        .singleResult();
    assertNotNull(task2Execution);

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/message/MessageNonInterruptingBoundaryEventTest.testNestedEvents.bpmn20.xml"})
  @Test
  public void testNestedEventsAnotherExecutionOrder() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("secondMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertNotNull(task1Execution);

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    // when (2)
    runtimeService.correlateMessage("firstMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task innerTask = taskService.createTaskQuery()
        .taskDefinitionKey("innerTask")
        .singleResult();
    assertNotNull(innerTask);

    Execution innerTaskExecution = runtimeService
        .createExecutionQuery()
        .activityId("innerTask")
        .singleResult();

    assertNotEquals(processInstanceId, ((ExecutionEntity) innerTaskExecution).getParentId());

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertNotNull(task1Execution);

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    // when (3)
    runtimeService.correlateMessage("thirdMessage");

    // then (3)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();
    assertNotNull(task1);

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("task1")
        .singleResult();
    assertNotNull(task1Execution);

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task2")
        .singleResult();
    assertNotNull(task2);

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task2")
        .singleResult();
    assertNotNull(task2Execution);

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(processInstanceId);

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

}
