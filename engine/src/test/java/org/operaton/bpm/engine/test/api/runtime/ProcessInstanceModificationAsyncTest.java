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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertNotSame;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.management.ActivityStatistics;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.TransitionInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Thorben Lindhauer
 *
 */
class ProcessInstanceModificationAsyncTest {

  protected static final String EXCLUSIVE_GATEWAY_ASYNC_BEFORE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGatewayAsyncTask.bpmn20.xml";

  protected static final String ASYNC_BEFORE_ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.asyncBeforeOneTaskProcess.bpmn20.xml";
  protected static final String ASYNC_BEFORE_ONE_SCOPE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.asyncBeforeOneScopeTaskProcess.bpmn20.xml";

  protected static final String NESTED_ASYNC_BEFORE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelAsyncBeforeOneTaskProcess.bpmn20.xml";
  protected static final String NESTED_ASYNC_BEFORE_SCOPE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelAsyncBeforeOneScopeTaskProcess.bpmn20.xml";
  protected static final String NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelAsyncBeforeConcurrentScopeTaskProcess.bpmn20.xml";
  protected static final String NESTED_ASYNC_BEFORE_IO_LISTENER_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelAsyncBeforeOneTaskProcessIoAndListeners.bpmn20.xml";

  protected static final String ASYNC_AFTER_ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.asyncAfterOneTaskProcess.bpmn20.xml";

  protected static final String NESTED_ASYNC_AFTER_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelAsyncAfterOneTaskProcess.bpmn20.xml";
  protected static final String NESTED_ASYNC_AFTER_END_EVENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelAsyncAfterEndEventProcess.bpmn20.xml";

  protected static final String ASYNC_AFTER_FAILING_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.asyncAfterFailingTaskProcess.bpmn20.xml";
  protected static final String ASYNC_BEFORE_FAILING_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.asyncBeforeFailingTaskProcess.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;

  @Deployment(resources = EXCLUSIVE_GATEWAY_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testStartBeforeAsync() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .execute();

    // the task does not yet exist because it is started asynchronously
    Task task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertThat(task).isNull();

    // and there is no activity instance for task2 yet
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .transition("task2")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("task1").concurrent().noScope().up()
        .child("task2").concurrent().noScope()
      .done());

    // when the async job is executed
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    testRule.executeAvailableJobs();

    // then there is the task
    task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertThat(task).isNotNull();

    // and there is an activity instance for task2
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .activity("task2")
      .done());

    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  /**
   * starting after a task should not respect that tasks asyncAfter setting
   */
  @Deployment
  @Test
  void testStartAfterAsync() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startAfterActivity("task2")
      .execute();

    // there is now a job for the end event after task2
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    Execution jobExecution = runtimeService.createExecutionQuery().activityId("end2").executionId(job.getExecutionId()).singleResult();
    assertThat(jobExecution).isNotNull();

    // end process
    completeTasksInOrder("task1");
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testCancelParentScopeOfAsyncBeforeActivity() {
    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // when I cancel the subprocess
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "subProcess"))
      .execute();

    // then the process instance is in a valid state
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    Assertions.assertThat(updatedTree).isNotNull();

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = NESTED_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testCancelParentScopeOfAsyncBeforeScopeActivity() {
    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // when I cancel the subprocess
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "subProcess"))
      .execute();

    // then the process instance is in a valid state
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    Assertions.assertThat(updatedTree).isNotNull();

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testCancelParentScopeOfParallelAsyncBeforeScopeActivity() {
    // given a process instance with two concurrent async scope tasks in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedConcurrentTasksProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // when I cancel the subprocess
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "subProcess"))
      .execute();

    // then the process instance is in a valid state
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    Assertions.assertThat(updatedTree).isNotNull();

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = NESTED_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testCancelAsyncActivityInstanceFails() {
    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getChildTransitionInstanceForTargetActivity(tree, "innerTask").getId());

    // the the async task is not an activity instance so it cannot be cancelled as follows
    try {
      processInstanceModificationBuilder.execute();
      fail("should not succeed");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("activityInstance is null", e.getMessage());
    }
  }

  @Deployment(resources = NESTED_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testCancelAsyncBeforeTransitionInstance() {
    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when the async task is cancelled via cancelTransitionInstance
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "innerTask").getId())
      .execute();

    // then the job has been removed
    assertThat(managementService.createJobQuery().count()).isZero();

    // and the activity instance and execution trees match
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("outerTask")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
    .done());

    // and the process can be completed successfully
    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());
  }


  @Deployment(resources = ASYNC_BEFORE_ONE_TASK_PROCESS)
  @Test
  void testCancelAsyncBeforeTransitionInstanceEndsProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstanceId)
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "theTask").getId())
      .execute();

    // then the process instance has ended
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ASYNC_BEFORE_ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCancelAsyncBeforeScopeTransitionInstanceEndsProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstanceId)
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "theTask").getId())
      .execute();

    // then the process instance has ended
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ASYNC_BEFORE_ONE_TASK_PROCESS)
  @Test
  void testCancelAndStartAsyncBeforeTransitionInstance() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    Job asyncJob = managementService.createJobQuery().singleResult();

    // when cancelling the only transition instance in the process and immediately starting it again
    runtimeService.createProcessInstanceModification(processInstanceId)
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "theTask").getId())
      .startBeforeActivity("theTask")
      .execute();

    // then the activity instance tree should be as before
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .transition("theTask")
        .done());

    // and the async job should be a new one
    Job newAsyncJob = managementService.createJobQuery().singleResult();
    assertThat(newAsyncJob.getId()).isNotEqualTo(asyncJob.getId());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("theTask").scope()
    .done());

    // and the process can be completed successfully
    testRule.executeAvailableJobs();
    completeTasksInOrder("theTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testCancelNestedConcurrentTransitionInstance() {
    // given a process instance with an instance of outerTask and two asynchronous tasks nested
    // in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedConcurrentTasksProcess");
    String processInstanceId = processInstance.getId();

    // when one of the inner transition instances is cancelled
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "innerTask1").getId())
      .execute();

    // then the activity instance and execution trees should match
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("outerTask")
          .beginScope("subProcess")
            .transition("innerTask2")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child("innerTask2").scope()
    .done());

    // and the job for innerTask2 should still be there and assigned to the correct execution
    Job innerTask2Job = managementService.createJobQuery().singleResult();
    assertThat(innerTask2Job).isNotNull();

    Execution innerTask2Execution = runtimeService.createExecutionQuery().activityId("innerTask2").singleResult();
    assertThat(innerTask2Execution).isNotNull();

    assertThat(innerTask2Execution.getId()).isEqualTo(innerTask2Job.getExecutionId());

    // and completing the process should succeed
    completeTasksInOrder("outerTask");
    managementService.executeJob(innerTask2Job.getId());
    completeTasksInOrder("innerTask2");

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testCancelNestedConcurrentTransitionInstanceWithConcurrentScopeTask() {
    // given a process instance where the job for innerTask2 is already executed
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedConcurrentTasksProcess");
    String processInstanceId = processInstance.getId();

    Job innerTask2Job = managementService.createJobQuery().activityId("innerTask2").singleResult();
    assertThat(innerTask2Job).isNotNull();
    managementService.executeJob(innerTask2Job.getId());

    // when the transition instance to innerTask1 is cancelled
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "innerTask1").getId())
      .execute();

    // then the activity instance and execution tree should match
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("outerTask")
          .beginScope("subProcess")
            .activity("innerTask2")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("innerTask2").scope()
    .done());

    // and there should be no job for innerTask1 anymore
    assertThat(managementService.createJobQuery().activityId("innerTask1").count()).isZero();

    // and completing the process should succeed
    completeTasksInOrder("innerTask2", "outerTask");

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_ASYNC_BEFORE_IO_LISTENER_PROCESS)
  @Test
  void testCancelTransitionInstanceShouldNotInvokeIoMappingAndListenersOfTargetActivity() {
    RecorderExecutionListener.clear();

    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when the async task is cancelled via cancelTransitionInstance
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "innerTask").getId())
      .execute();

    // then no io mapping is executed and no end listener is executed
    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();
    assertThat(runtimeService.createVariableInstanceQuery().variableName("outputMappingExecuted").count()).isZero();

    // and the process can be completed successfully
    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = NESTED_ASYNC_AFTER_TASK_PROCESS)
  @Test
  void testCancelAsyncAfterTransitionInstance() {
    // given a process instance with an asyncAfter task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");

    Task innerTask = taskService.createTaskQuery().taskDefinitionKey("innerTask").singleResult();
    assertThat(innerTask).isNotNull();
    taskService.complete(innerTask.getId());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when the async task is cancelled via cancelTransitionInstance
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "innerTask").getId())
      .execute();

    // then the job has been removed
    assertThat(managementService.createJobQuery().count()).isZero();

    // and the activity instance and execution trees match
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("outerTask")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
    .done());

    // and the process can be completed successfully
    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = NESTED_ASYNC_AFTER_END_EVENT_PROCESS)
  @Test
  void testCancelAsyncAfterEndEventTransitionInstance() {
    // given a process instance with an asyncAfter end event in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedAsyncEndEventProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when the async task is cancelled via cancelTransitionInstance
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "subProcessEnd").getId())
      .execute();

    // then the job has been removed
    assertThat(managementService.createJobQuery().count()).isZero();

    // and the activity instance and execution trees match
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("outerTask")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
    .done());

    // and the process can be completed successfully
    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = ASYNC_AFTER_ONE_TASK_PROCESS)
  @Test
  void testCancelAsyncAfterTransitionInstanceEndsProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstanceId)
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "theTask").getId())
      .execute();

    // then the process instance has ended
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCancelAsyncAfterTransitionInstanceInvokesParentListeners() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstanceId)
      .cancelTransitionInstance(getChildTransitionInstanceForTargetActivity(tree, "subProcessEnd").getId())
      .execute();

    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(1);
    RecordedEvent event = RecorderExecutionListener.getRecordedEvents().get(0);
    assertThat(event.getActivityId()).isEqualTo("subProcess");

    RecorderExecutionListener.clear();
  }

  @Deployment(resources = NESTED_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testCancelAllCancelsTransitionInstances() {
    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");

    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when the async task is cancelled via cancelAll
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("innerTask")
      .execute();

    // then the job has been removed
    assertThat(managementService.createJobQuery().count()).isZero();

    // and the activity instance and execution trees match
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("outerTask")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
    .done());

    // and the process can be completed successfully
    completeTasksInOrder("outerTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = ASYNC_AFTER_FAILING_TASK_PROCESS)
  @Test
  void testStartBeforeAsyncAfterTask() {
    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingAfterAsyncTask");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // when
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task1")
      .execute();

    // then there are two transition instances of task1
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .transition("task1")
        .transition("task1")
      .done());

    // when all jobs are executed
    testRule.executeAvailableJobs();

    // then the tree is still the same, since the jobs failed
    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .transition("task1")
        .transition("task1")
      .done());
  }

  @Deployment(resources = ASYNC_AFTER_FAILING_TASK_PROCESS)
  @Test
  void testStartBeforeAsyncAfterTaskActivityStatistics() {
    // given a process instance with an async task in a subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingAfterAsyncTask");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // there is one statistics instance
    List<ActivityStatistics> statistics = managementService
        .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);
    assertThat(statistics.get(0).getId()).isEqualTo("task1");
    assertThat(statistics.get(0).getFailedJobs()).isZero();
    assertThat(statistics.get(0).getIncidentStatistics()).isEmpty();
    assertThat(statistics.get(0).getInstances()).isEqualTo(1);

    // when
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task1")
      .execute();

    // then there are statistics instances of task1
    statistics = managementService
      .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
      .includeFailedJobs()
      .includeIncidents()
      .list();

    assertThat(statistics).hasSize(1);
    assertThat(statistics.get(0).getId()).isEqualTo("task1");
    assertThat(statistics.get(0).getFailedJobs()).isZero();
    assertThat(statistics.get(0).getIncidentStatistics()).isEmpty();
    assertThat(statistics.get(0).getInstances()).isEqualTo(2);


    // when all jobs are executed
    testRule.executeAvailableJobs();

  }


  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testCancelAllTransitionInstanceInScope() {
    // given there are two transition instances in an inner scope
    // and an active activity instance in an outer scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedOneTaskProcess")
      .startBeforeActivity("innerTask")
      .startBeforeActivity("innerTask")
      .startBeforeActivity("outerTask")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    // when i cancel both transition instances
    TransitionInstance[] transitionInstances = tree.getTransitionInstances("innerTask");

    runtimeService.createProcessInstanceModification(instance.getId())
      .cancelTransitionInstance(transitionInstances[0].getId())
      .cancelTransitionInstance(transitionInstances[1].getId())
      .execute();

    // then the outer activity instance is the only one remaining
    tree = runtimeService.getActivityInstance(instance.getId());

    assertThat(tree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testCancelTransitionInstanceTwiceFails() {
    // given there are two transition instances in an inner scope
    // and an active activity instance in an outer scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedOneTaskProcess")
      .startBeforeActivity("innerTask")
      .startBeforeActivity("innerTask")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    // when i cancel both transition instances
    TransitionInstance[] transitionInstances = tree.getTransitionInstances("innerTask");

    // this test ensures that the replacedBy link of executions is not followed
    // in case the original execution was actually removed/cancelled
    String transitionInstanceId = transitionInstances[0].getId();
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(instance.getId())
        .cancelTransitionInstance(transitionInstanceId)
        .cancelTransitionInstance(transitionInstanceId);
    try {
      processInstanceModificationBuilder.execute();
      fail("should not be possible to cancel the first instance twice");
    } catch (NotValidException e) {
      testRule.assertTextPresentIgnoreCase("Cannot perform instruction: Cancel transition instance '" + transitionInstanceId
          + "'; Transition instance '" + transitionInstanceId + "' does not exist: transitionInstance is null",
          e.getMessage());
    }
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_ASYNC_BEFORE_TASK_PROCESS)
  @Test
  void testCancelTransitionInstanceTwiceFailsCase2() {
    // given there are two transition instances in an inner scope
    // and an active activity instance in an outer scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedOneTaskProcess")
      .startBeforeActivity("innerTask")
      .startBeforeActivity("innerTask")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    // when i cancel both transition instances
    TransitionInstance[] transitionInstances = tree.getTransitionInstances("innerTask");
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(instance.getId())
        .cancelTransitionInstance(transitionInstances[0].getId()) // compacts the tree;
                                                                  // => execution for transitionInstances[1] is replaced by scope execution
        .startBeforeActivity("innerTask")                         // expand tree again
                                                                  // => scope execution is replaced by a new concurrent execution
        .startBeforeActivity("innerTask")
        .cancelTransitionInstance(transitionInstances[1].getId()) // does not trigger compaction
        .cancelTransitionInstance(transitionInstances[1].getId());

    // this test ensures that the replacedBy link of executions is not followed
    // in case the original execution was actually removed/cancelled

    try {
      processInstanceModificationBuilder.execute();
      fail("should not be possible to cancel the first instance twice");
    } catch (NotValidException e) {
      String transitionInstanceId = transitionInstances[1].getId();
      testRule.assertTextPresentIgnoreCase("Cannot perform instruction: Cancel transition instance '" + transitionInstanceId
          + "'; Transition instance '" + transitionInstanceId + "' does not exist: transitionInstance is null",
          e.getMessage());
    }
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testCancelStartCancelInScope() {
    // given there are two transition instances in an inner scope
    // and an active activity instance in an outer scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedConcurrentTasksProcess")
      .startBeforeActivity("innerTask1")
      .startBeforeActivity("innerTask1")
      .startBeforeActivity("outerTask")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    // when i cancel both transition instances
    TransitionInstance[] transitionInstances = tree.getTransitionInstances("innerTask1");

    runtimeService.createProcessInstanceModification(instance.getId())
      .cancelTransitionInstance(transitionInstances[0].getId()) // triggers tree compaction
      .startBeforeActivity("innerTask2")                        // triggers tree expansion
      .cancelTransitionInstance(transitionInstances[1].getId())
      .execute();

    // then the outer activity instance is the only one remaining
    tree = runtimeService.getActivityInstance(instance.getId());

    assertThat(tree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .transition("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child("innerTask2").scope()
      .done());
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testStartAndCancelAllForTransitionInstance() {
    // given there is one transition instance in a scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedConcurrentTasksProcess")
      .startBeforeActivity("innerTask1")
      .startBeforeActivity("innerTask1")
      .startBeforeActivity("innerTask1")
      .execute();

    // when I start an activity in the same scope
    // and cancel the first transition instance
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("innerTask2")
      .cancelAllForActivity("innerTask1")
      .execute();

    // then the activity was successfully instantiated
    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    assertThat(tree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .beginScope("subProcess")
          .transition("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("innerTask2").scope()
      .done());
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testRepeatedStartAndCancellationForTransitionInstance() {
    // given there is one transition instance in a scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedConcurrentTasksProcess")
      .startBeforeActivity("innerTask1")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());
    TransitionInstance transitionInstance = tree.getTransitionInstances("innerTask1")[0];

    // when I start an activity in the same scope
    // and cancel the first transition instance
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("innerTask2")  // expand tree
      .cancelAllForActivity("innerTask2") // compact tree
      .startBeforeActivity("innerTask2")  // expand tree
      .cancelAllForActivity("innerTask2") // compact tree
      .startBeforeActivity("innerTask2")  // expand tree
      .cancelAllForActivity("innerTask2") // compact tree
      .cancelTransitionInstance(transitionInstance.getId())
      .execute();

    // then the process has ended
    testRule.assertProcessEnded(instance.getId());
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testRepeatedCancellationAndStartForTransitionInstance() {
    // given there is one transition instance in a scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedConcurrentTasksProcess")
      .startBeforeActivity("innerTask1")
      .startBeforeActivity("innerTask1")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());
    TransitionInstance[] transitionInstances = tree.getTransitionInstances("innerTask1");

    // when I start an activity in the same scope
    // and cancel the first transition instance
    runtimeService.createProcessInstanceModification(instance.getId())
      .cancelTransitionInstance(transitionInstances[0].getId()) // compact tree
      .startBeforeActivity("innerTask2")  // expand tree
      .cancelAllForActivity("innerTask2") // compact tree
      .startBeforeActivity("innerTask2")  // expand tree
      .cancelAllForActivity("innerTask2") // compact tree
      .startBeforeActivity("innerTask2")  // expand tree
      .cancelTransitionInstance(transitionInstances[1].getId())
      .execute();

    // then there is only an activity instance for innerTask2
    tree = runtimeService.getActivityInstance(instance.getId());

    assertThat(tree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .beginScope("subProcess")
          .transition("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("innerTask2").scope()
      .done());
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testStartBeforeAndCancelSingleTransitionInstance() {
    // given there is one transition instance in a scope
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedConcurrentTasksProcess")
      .startBeforeActivity("innerTask1")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());
    TransitionInstance transitionInstance = tree.getTransitionInstances("innerTask1")[0];

    // when I start an activity in the same scope
    // and cancel the first transition instance
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("innerTask2")
      .cancelTransitionInstance(transitionInstance.getId())
      .execute();

    // then the activity was successfully instantiated
    tree = runtimeService.getActivityInstance(instance.getId());

    assertThat(tree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .beginScope("subProcess")
          .transition("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("innerTask2").scope()
      .done());
  }

  /**
   * CAM-4090
   */
  @Deployment(resources = NESTED_PARALLEL_ASYNC_BEFORE_SCOPE_TASK_PROCESS)
  @Test
  void testStartBeforeSyncEndAndCancelSingleTransitionInstance() {
    // given there is one transition instance in a scope and an outer activity instance
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("nestedConcurrentTasksProcess")
      .startBeforeActivity("outerTask")
      .startBeforeActivity("innerTask1")
      .execute();

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());
    TransitionInstance transitionInstance = tree.getTransitionInstances("innerTask1")[0];

    // when I start an activity in the same scope that ends immediately
    // and cancel the first transition instance
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("subProcessEnd2")
      .cancelTransitionInstance(transitionInstance.getId())
      .execute();

    // then only the outer activity instance is left
    tree = runtimeService.getActivityInstance(instance.getId());

    assertThat(tree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());
  }

  @Deployment(resources = ASYNC_BEFORE_FAILING_TASK_PROCESS)
  @Test
  void testRestartAFailedServiceTask() {
    // given a failed job
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("failingAfterBeforeTask")
      .startBeforeActivity("task2")
      .execute();

    testRule.executeAvailableJobs();
    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();

    // when the service task is restarted
    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("task2")
      .cancelTransitionInstance(tree.getTransitionInstances("task2")[0].getId())
      .execute();

    testRule.executeAvailableJobs();

    // then executing the task has failed again and there is a new incident
    Incident newIncident = runtimeService.createIncidentQuery().singleResult();
    assertThat(newIncident).isNotNull();

    assertNotSame(incident.getId(), newIncident.getId());
  }

  protected String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
    ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
    if (instance != null) {
      return instance.getId();
    }
    return null;
  }

  protected ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
    if (activityId.equals(activityInstance.getActivityId())) {
      return activityInstance;
    }

    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      ActivityInstance instance = getChildInstanceForActivity(childInstance, activityId);
      if (instance != null) {
        return instance;
      }
    }

    return null;
  }

  protected TransitionInstance getChildTransitionInstanceForTargetActivity(ActivityInstance activityInstance, String targetActivityId) {
    for (TransitionInstance childTransitionInstance : activityInstance.getChildTransitionInstances()) {
      if (targetActivityId.equals(childTransitionInstance.getActivityId())) {
        return childTransitionInstance;
      }
    }

    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      TransitionInstance instance = getChildTransitionInstanceForTargetActivity(childInstance, targetActivityId);
      if (instance != null) {
        return instance;
      }
    }

    return null;
  }

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      assertThat(!tasks.isEmpty()).as("task for activity " + taskName + " does not exist").isTrue();
      taskService.complete(tasks.get(0).getId());
    }
  }
}
