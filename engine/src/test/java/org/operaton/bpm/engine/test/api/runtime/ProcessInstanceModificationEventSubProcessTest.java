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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Job;
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
 * @author Roman Smirnov
 *
 */
class ProcessInstanceModificationEventSubProcessTest {

  protected static final String INTERRUPTING_EVENT_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingEventSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_EVENT_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingEventSubProcess.bpmn20.xml";
  protected static final String INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingEventSubProcessInsideSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingEventSubProcessInsideSubProcess.bpmn20.xml";
  protected static final String CANCEL_AND_RESTART = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationEventSubProcessTest.testCancelAndRestart.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;

  @ParameterizedTest(name = "Start before: {0}")
  @CsvSource({
    "Task inside event sub-process, " + INTERRUPTING_EVENT_SUBPROCESS + ", eventSubProcessTask, task1;task2;eventSubProcessTask",
    "Task inside non-interrupting event sub-process, " + NON_INTERRUPTING_EVENT_SUBPROCESS + ", eventSubProcessTask, task1;task2;eventSubProcessTask",
    "Start event inside non-interrupting event sub-process, " + NON_INTERRUPTING_EVENT_SUBPROCESS + ", eventProcessStart, task1;task2;eventSubProcessTask",
    "Non-interrupting event sub-process, " + NON_INTERRUPTING_EVENT_SUBPROCESS + ", eventSubProcess, task1;eventSubProcessTask;task2"
  })
  void testStartBeforeTaskInsideEventSubProcess(String name, String bpmnResource, String startBefore, String completeTasksInOrder) {
    // given
    testRule.deploy(bpmnResource);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    // when
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity(startBefore)
      .execute();

    // then
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .beginScope("eventSubProcess")
        .activity("eventSubProcessTask")
        .endScope()
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
          .child("eventSubProcessTask").scope()
          .done());

    completeTasksInOrder(completeTasksInOrder.split(";"));
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  @Test
  void testStartBeforeTaskInsideEventSubProcessAndCancelTaskOutsideEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("eventSubProcess")
          .activity("eventSubProcessTask")
        .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);

  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  @Test
  void testStartBeforeStartEventInsideEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("eventSubProcess")
          .activity("eventSubProcessTask")
        .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  @Test
  void testStartBeforeEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("eventSubProcess")
          .activity("eventSubProcessTask")
        .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS)
  @Test
  void testStartBeforeTaskInsideNonInterruptingEventSubProcessAndCancelTaskOutsideEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("eventSubProcess")
          .activity("eventSubProcessTask")
        .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeTaskInsideEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
          .beginScope("subProcess")
            .beginScope("eventSubProcess")
              .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("task1", "eventSubProcessTask", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeStartEventInsideEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
          .beginScope("subProcess")
            .beginScope("eventSubProcess")
              .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("eventSubProcessTask", "task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
          .beginScope("subProcess")
            .beginScope("eventSubProcess")
              .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("task1", "eventSubProcessTask", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @ParameterizedTest(name = "Start before: {0}")
  @CsvSource({
    "Task inside event sub-process inside sub-process, " + INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS + ", eventSubProcessTask",
    "Task inside non-interrupting event sub-process inside sub-process, " + NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS + ", eventSubProcessTask",
    "Start event inside non-interrupting event sub-process inside sub-process, " + NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS + ", eventProcessStart",
    "Non-interrupting event sub-process inside sub-process, " + NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS + ", eventSubProcess",
  })
  void task2ShouldStay(String name, String bpmnResource, String startBeforeActivity) {
    // given
    testRule.deploy(bpmnResource);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    // when
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    // then
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("subProcess")
        .activity("task2")
        .beginScope("eventSubProcess")
        .activity("eventSubProcessTask")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
          .child("task2").concurrent().noScope().up()
          .child(null).concurrent().noScope()
          .child("eventSubProcessTask").scope()
          .done());

    completeTasksInOrder("task2", "eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeStartEventInsideEventSubProcessInsideSubProcessTask2ShouldBeCancelled() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("subProcess")
          .beginScope("eventSubProcess")
            .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
            .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeEventSubProcessInsideSubProcessTask2ShouldBeCancelled() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("subProcess")
          .beginScope("eventSubProcess")
            .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
            .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeTaskInsideNonInterruptingEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .beginScope("subProcess")
          .beginScope("eventSubProcess")
            .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("task1", "eventSubProcessTask", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeStartEventInsideNonInterruptingEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .beginScope("subProcess")
          .beginScope("eventSubProcess")
            .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("task1", "task2", "eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  @Test
  void testStartBeforeNonInterruptingEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .beginScope("subProcess")
          .beginScope("eventSubProcess")
            .activity("eventSubProcessTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("eventSubProcessTask").scope()
        .done());

    completeTasksInOrder("task1", "task2", "eventSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);

  }

  @Deployment
  @Test
  void testTimerJobPreservationOnCancellationAndStart() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerEventSubProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    Job timerJob = managementService.createJobQuery().singleResult();
    assertThat(timerJob).isNotNull();

    // when the process instance is bare intermediately due to cancellation
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task"))
      .startBeforeActivity("task")
      .execute();

    // then it is still the same job

    Job remainingTimerJob = managementService.createJobQuery().singleResult();
    assertThat(remainingTimerJob).isNotNull();

    assertThat(remainingTimerJob.getId()).isEqualTo(timerJob.getId());
    assertThat(remainingTimerJob.getDuedate()).isEqualTo(timerJob.getDuedate());

  }


  @Deployment(resources = CANCEL_AND_RESTART)
  @Test
  void testProcessInstanceModificationInEventSubProcessCancellationAndRestart() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ProcessWithEventSubProcess");

    // assume
    Task task = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("UserTaskEventSubProcess")
        .singleResult();
    assertThat(task).isNotNull();

    // when
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("UserTaskEventSubProcess")
      .startAfterActivity("UserTaskEventSubProcess")
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();
  }

  protected String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
    ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
    if (instance != null) {
      return instance.getId();
    }
    return null;
  }

  /**
   * Important that only the direct children are considered here. If you change this,
   * the test assertions are not as tight anymore.
   */
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
