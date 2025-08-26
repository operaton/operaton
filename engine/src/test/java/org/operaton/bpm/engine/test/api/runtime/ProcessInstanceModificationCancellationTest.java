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

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
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
 * Tests cancellation of four basic patterns of active activities in a scope:
 * <ul>
 *  <li>single, non-scope activity
 *  <li>single, scope activity
 *  <li>two concurrent non-scope activities
 *  <li>two concurrent scope activities
 * </ul>
 *
 * @author Thorben Lindhauer
 */
class ProcessInstanceModificationCancellationTest {

  // the four patterns as described above
  protected static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml";
  protected static final String ONE_SCOPE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.oneScopeTaskProcess.bpmn20.xml";
  protected static final String CONCURRENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String CONCURRENT_SCOPE_TASKS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGatewayScopeTasks.bpmn20.xml";

  // the four patterns nested in a subprocess and with an outer parallel task
  protected static final String NESTED_PARALLEL_ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelOneTaskProcess.bpmn20.xml";
  protected static final String NESTED_PARALLEL_ONE_SCOPE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelOneScopeTaskProcess.bpmn20.xml";
  protected static final String NESTED_PARALLEL_CONCURRENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelGateway.bpmn20.xml";
  protected static final String NESTED_PARALLEL_CONCURRENT_SCOPE_TASKS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nestedParallelGatewayScopeTasks.bpmn20.xml";

  protected static final String LISTENER_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.listenerProcess.bpmn20.xml";
  protected static final String FAILING_OUTPUT_MAPPINGS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.failingOutputMappingProcess.bpmn20.xml";

  protected static final String INTERRUPTING_EVENT_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingEventSubProcess.bpmn20.xml";

  protected static final String CALL_ACTIVITY_PROCESS = "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml";
  protected static final String SIMPLE_SUBPROCESS = "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml";
  protected static final String TWO_SUBPROCESSES = "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testTwoSubProcesses.bpmn20.xml";
  protected static final String NESTED_CALL_ACTIVITY = "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testNestedCallActivity.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void testCancellationInOneTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "theTask"))
      .execute();

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void testCancelAllInOneTaskProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // two instance of theTask
    runtimeService.createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theTask")
      .execute();

    // when
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .cancelAllForActivity("theTask")
      .execute();

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void testCancellationAndCreationInOneTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "theTask"))
      .startBeforeActivity("theTask")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(updatedTree.getId()).isEqualTo(tree.getId());
    assertThat(getInstanceIdForActivity(updatedTree, "theTask")).isNotEqualTo(getInstanceIdForActivity(tree, "theTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("theTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("theTask").scope()
        .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void testCreationAndCancellationInOneTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theTask")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "theTask"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "theTask")).isNotEqualTo(getInstanceIdForActivity(tree, "theTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("theTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("theTask").scope()
        .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCancellationInOneScopeTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "theTask"))
      .execute();

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCancelAllInOneScopeTaskProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // two instances of theTask
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theTask")
      .execute();

    // then
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("theTask")
      .execute();

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCancellationAndCreationInOneScopeTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "theTask"))
      .startBeforeActivity("theTask")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "theTask")).isNotEqualTo(getInstanceIdForActivity(tree, "theTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("theTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("theTask").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCreationAndCancellationInOneScopeTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theTask")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "theTask"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "theTask")).isNotEqualTo(getInstanceIdForActivity(tree, "theTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("theTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("theTask").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CONCURRENT_PROCESS)
  @Test
  void testCancellationInConcurrentProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("task2").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CONCURRENT_PROCESS)
  @Test
  void testCancelAllInConcurrentProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    // two instances in task1
    runtimeService.createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("task1")
      .execute();

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("task1")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("task2").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }


  @Deployment(resources = CONCURRENT_PROCESS)
  @Test
  void testCancellationAndCreationInConcurrentProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("task1")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "task1")).isNotEqualTo(getInstanceIdForActivity(tree, "task1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("task1").noScope().concurrent().up()
        .child("task2").noScope().concurrent()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CONCURRENT_PROCESS)
  @Test
  void testCreationAndCancellationInConcurrentProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task1")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "task1")).isNotEqualTo(getInstanceIdForActivity(tree, "task1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("task1").noScope().concurrent().up()
        .child("task2").noScope().concurrent()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testCancellationInConcurrentScopeTasksProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "task1")).isNotEqualTo(getInstanceIdForActivity(tree, "task1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("task2").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testCancelAllInConcurrentScopeTasksProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    // two instances of task1
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task1")
      .execute();


    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("task1")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("task2").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testCancellationAndCreationInConcurrentScopeTasksProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("task1")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "task1")).isNotEqualTo(getInstanceIdForActivity(tree, "task1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child(null).noScope().concurrent()
          .child("task1").scope().up().up()
        .child(null).noScope().concurrent()
          .child("task2").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testCreationAndCancellationInConcurrentScopeTasksProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task1")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "task1")).isNotEqualTo(getInstanceIdForActivity(tree, "task1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .activity("task2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child(null).noScope().concurrent()
          .child("task1").scope().up().up()
        .child(null).noScope().concurrent()
          .child("task2").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_TASK_PROCESS)
  @Test
  void testCancellationInNestedOneTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
        .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_TASK_PROCESS)
  @Test
  void testScopeCancellationInNestedOneTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "subProcess"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_TASK_PROCESS)
  @Test
  void testCancellationAndCreationInNestedOneTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask"))
      .startBeforeActivity("innerTask")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
      .child("outerTask").concurrent().noScope().up()
      .child(null).concurrent().noScope()
        .child("innerTask").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_TASK_PROCESS)
  @Test
  void testCreationAndCancellationInNestedOneTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child("innerTask").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCancellationInNestedOneScopeTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneScopeTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_SCOPE_TASK_PROCESS)
  @Test
  void testScopeCancellationInNestedOneScopeTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneScopeTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "subProcess"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCancellationAndCreationInNestedOneScopeTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneScopeTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask"))
      .startBeforeActivity("innerTask")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
      .child("outerTask").concurrent().noScope().up()
      .child(null).concurrent().noScope()
        .child(null).scope()
          .child("innerTask").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_ONE_SCOPE_TASK_PROCESS)
  @Test
  void testCreationAndCancellationInNestedOneScopeTaskProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedOneScopeTaskProcess");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("innerTask").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_PROCESS)
  @Test
  void testCancellationInNestedConcurrentProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child("innerTask2").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_PROCESS)
  @Test
  void testScopeCancellationInNestedConcurrentProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "subProcess"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_PROCESS)
  @Test
  void testCancellationAndCreationInNestedConcurrentProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask1"))
      .startBeforeActivity("innerTask1")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask1")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask1")
          .activity("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").noScope().concurrent().up()
        .child(null).noScope().concurrent()
          .child(null).scope()
            .child("innerTask1").noScope().concurrent().up()
            .child("innerTask2").noScope().concurrent()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(3);
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_PROCESS)
  @Test
  void testCreationAndCancellationInNestedConcurrentProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask1")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask1")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask1")
          .activity("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").noScope().concurrent().up()
        .child(null).noScope().concurrent()
          .child(null).scope()
            .child("innerTask1").noScope().concurrent().up()
            .child("innerTask2").noScope().concurrent()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(3);
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testCancellationInNestedConcurrentScopeTasksProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGatewayScopeTasks");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("innerTask2").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testScopeCancellationInNestedConcurrentScopeTasksProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGatewayScopeTasks");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "subProcess"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("outerTask").scope()
      .done());

    // assert successful completion of process
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testCancellationAndCreationInNestedConcurrentScopeTasksProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGatewayScopeTasks");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask1"))
      .startBeforeActivity("innerTask1")
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask1")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask1")
          .activity("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("innerTask1").scope().up().up()
            .child(null).concurrent().noScope()
              .child("innerTask2").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(3);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NESTED_PARALLEL_CONCURRENT_SCOPE_TASKS_PROCESS)
  @Test
  void testCreationAndCancellationInNestedConcurrentScopeTasksProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGatewayScopeTasks");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask1")
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask1"))
      .execute();

    testRule.assertProcessNotEnded(processInstanceId);

    // assert activity instance
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(getInstanceIdForActivity(updatedTree, "innerTask1")).isNotEqualTo(getInstanceIdForActivity(tree, "innerTask1"));

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask1")
          .activity("innerTask2")
      .done());

    // assert executions
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree(null).scope()
        .child("outerTask").concurrent().noScope().up()
        .child(null).noScope().concurrent()
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("innerTask1").scope().up().up()
            .child(null).concurrent().noScope()
              .child("innerTask2").scope()
      .done());

    // assert successful completion of process
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(3);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = LISTENER_PROCESS)
  @Test
  void testEndListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "listenerProcess",
        Collections.singletonMap("listener", new RecorderExecutionListener()));

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // when one inner task is cancelled
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask1"))
      .execute();

    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(1);
    RecordedEvent innerTask1EndEvent = RecorderExecutionListener.getRecordedEvents().get(0);
    assertThat(innerTask1EndEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(innerTask1EndEvent.getActivityId()).isEqualTo("innerTask1");
    assertThat(innerTask1EndEvent.getActivityInstanceId()).isEqualTo(getInstanceIdForActivity(tree, "innerTask1"));

    // when the second inner task is cancelled
    RecorderExecutionListener.clear();
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask2"))
      .execute();

    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(2);
    RecordedEvent innerTask2EndEvent = RecorderExecutionListener.getRecordedEvents().get(0);
    assertThat(innerTask2EndEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(innerTask2EndEvent.getActivityId()).isEqualTo("innerTask2");
    assertThat(innerTask2EndEvent.getActivityInstanceId()).isEqualTo(getInstanceIdForActivity(tree, "innerTask2"));

    RecordedEvent subProcessEndEvent = RecorderExecutionListener.getRecordedEvents().get(1);
    assertThat(subProcessEndEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(subProcessEndEvent.getActivityId()).isEqualTo("subProcess");
    assertThat(subProcessEndEvent.getActivityInstanceId()).isEqualTo(getInstanceIdForActivity(tree, "subProcess"));

    // when the outer task is cancelled (and so the entire process)
    RecorderExecutionListener.clear();
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "outerTask"))
      .execute();

    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(2);
    RecordedEvent outerTaskEndEvent = RecorderExecutionListener.getRecordedEvents().get(0);
    assertThat(outerTaskEndEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(outerTaskEndEvent.getActivityId()).isEqualTo("outerTask");
    assertThat(outerTaskEndEvent.getActivityInstanceId()).isEqualTo(getInstanceIdForActivity(tree, "outerTask"));

    RecordedEvent processEndEvent = RecorderExecutionListener.getRecordedEvents().get(1);
    assertThat(processEndEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(processEndEvent.getActivityId()).isNull();
    assertThat(processEndEvent.getActivityInstanceId()).isEqualTo(tree.getId());

    RecorderExecutionListener.clear();
  }

  /**
   * Tests the case that an output mapping exists that expects variables
   * that do not exist yet when the activities are cancelled
   */
  @Deployment(resources = FAILING_OUTPUT_MAPPINGS_PROCESS)
  @Test
  void testSkipOutputMappingsOnCancellation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingOutputMappingProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // then executing the following cancellations should not fail because
    // it skips the output mapping
    // cancel inner task
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask"))
      .execute(false, true);

    // cancel outer task
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "outerTask"))
      .execute(false, true);

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  @Test
  void testProcessInstanceEventSubscriptionsPreservedOnIntermediateCancellation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // event subscription for the event subprocess
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(subscription).isNotNull();
    assertThat(subscription.getProcessInstanceId()).isEqualTo(processInstance.getId());

    // when I execute cancellation and then start, such that the intermediate state of the process instance
    // has no activities
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("task1")
      .execute();

    // then the message event subscription remains (i.e. it is not deleted and later re-created)
    EventSubscription updatedSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(updatedSubscription).isNotNull();
    assertThat(updatedSubscription.getId()).isEqualTo(subscription.getId());
    assertThat(updatedSubscription.getProcessInstanceId()).isEqualTo(subscription.getProcessInstanceId());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void testProcessInstanceVariablesPreservedOnIntermediateCancellation() {
    ProcessInstance processInstance = runtimeService
        .startProcessInstanceByKey("oneTaskProcess", Variables.createVariables().putValue("var", "value"));

    // when I execute cancellation and then start, such that the intermediate state of the process instance
    // has no activities
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "theTask"))
      .startBeforeActivity("theTask")
      .execute();

    // then the process instance variables remain
    Object variable = runtimeService.getVariable(processInstance.getId(), "var");
    assertThat(variable)
            .isNotNull()
            .isEqualTo("value");
  }

  public String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
    ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
    if (instance != null) {
      return instance.getId();
    }
    return null;
  }

  public ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
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

  /**
   * Test case for checking cancellation of process instances in call activity subprocesses
   * <p>
   * Test should propagate upward and destroy all process instances
   * </p>
   */
  @Deployment(resources = {
      SIMPLE_SUBPROCESS,
      CALL_ACTIVITY_PROCESS
  })
  @Test
  void testCancellationInCallActivitySubProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    String processInstanceId = processInstance.getId();

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();

    // Completing the task continues the process which leads to calling the subprocess
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();


    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(2);

    ActivityInstance tree = runtimeService.getActivityInstance(taskInSubProcess.getProcessInstanceId());
    // when
    runtimeService
      .createProcessInstanceModification(taskInSubProcess.getProcessInstanceId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task"))
      .execute();


    // then
    testRule.assertProcessEnded(processInstanceId);

    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList).isEmpty();
  }

  @Deployment(resources = {
      SIMPLE_SUBPROCESS,
      CALL_ACTIVITY_PROCESS
  })
  @Test
  void testCancellationAndRestartInCallActivitySubProcess() {
    // given
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();

    // Completing the task continues the process which leads to calling the subprocess
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();


    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(2);

    ActivityInstance tree = runtimeService.getActivityInstance(taskInSubProcess.getProcessInstanceId());
    // when
    runtimeService
      .createProcessInstanceModification(taskInSubProcess.getProcessInstanceId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task"))
      .startBeforeActivity("task")
      .execute();

    // then
    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList).hasSize(2);
  }

  /**
   * Test case for checking cancellation of process instances in call activity subprocesses
   * <p>
   * Test that upward cancellation respects other process instances
   * </p>
   */
  @Deployment(resources = {
      SIMPLE_SUBPROCESS,
      TWO_SUBPROCESSES
  })
  @Test
  void testSingleCancellationWithTwoSubProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callTwoSubProcesses");
    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(3);

    List<Task> taskList = taskService.createTaskQuery().list();
    assertThat(taskList)
            .isNotNull()
            .hasSize(2);

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getProcessInstanceId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(2);

    ActivityInstance tree = runtimeService.getActivityInstance(taskList.get(0).getProcessInstanceId());

    // when
    runtimeService
      .createProcessInstanceModification(taskList.get(0).getProcessInstanceId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task"))
      .execute();

    // then

    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(2);

    // How man call activities
    activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getProcessInstanceId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(1);
  }

  /**
   * Test case for checking deletion of process instances in nested call activity subprocesses
   * <p>
   * Checking that nested call activities will propagate upward over multiple nested levels
   * </p>
   */
  @Deployment(resources = {
      SIMPLE_SUBPROCESS,
      NESTED_CALL_ACTIVITY,
      CALL_ACTIVITY_PROCESS
  })
  @Test
  void testCancellationMultilevelProcessInstanceInCallActivity() {
    // given
    runtimeService.startProcessInstanceByKey("nestedCallActivity");

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();

    // Completing the task continues the process which leads to calling the subprocess
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();

    // Completing the task continues the sub process which leads to calling the deeper subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskInNestedSubProcess = taskQuery.singleResult();

    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(3);

    ActivityInstance tree = runtimeService.getActivityInstance(taskInNestedSubProcess.getProcessInstanceId());

    // when
    runtimeService
      .createProcessInstanceModification(taskInNestedSubProcess.getProcessInstanceId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task"))
      .execute();

    // then
    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList).isEmpty();
  }

}
