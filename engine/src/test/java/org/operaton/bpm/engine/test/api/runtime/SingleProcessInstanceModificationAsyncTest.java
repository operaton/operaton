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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Yana Vasileva
 */
class SingleProcessInstanceModificationAsyncTest {

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";
  protected static final String LOOP_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.loop.bpmn";
  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String ONE_SCOPE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.oneScopeTaskProcess.bpmn20.xml";
  protected static final String TRANSITION_LISTENER_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.transitionListeners.bpmn20.xml";
  protected static final String TASK_LISTENER_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.taskListeners.bpmn20.xml";
  protected static final String IO_MAPPING_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.ioMapping.bpmn20.xml";
  protected static final String CALL_ACTIVITY_PARENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.testCancelCallActivityParentProcess.bpmn";
  protected static final String CALL_ACTIVITY_CHILD_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.testCancelCallActivityChildProcess.bpmn";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  ManagementService managementService;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;

  @AfterEach
  void tearDown() {
    List<Batch> batches = managementService.createBatchQuery().list();
    for (Batch batch : batches) {
      managementService.deleteBatch(batch.getId(), true);
    }

    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      managementService.deleteJob(job.getId());
    }
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testTheDeploymentIdIsSet() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processDefinitionId = processInstance.getProcessDefinitionId();
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(processDefinitionId)
        .singleResult();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    Job job = managementService.createJobQuery()
        .jobDefinitionId(modificationBatch.getSeedJobDefinitionId())
        .singleResult();
    // seed job
    managementService.executeJob(job.getId());

    for (Job pending : managementService.createJobQuery()
        .jobDefinitionId(modificationBatch.getBatchJobDefinitionId())
        .list()) {
      managementService.executeJob(pending.getId());
      Assertions.assertThat(pending.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    }
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testCancellation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree("task2").scope().done());

    // complete the process
    completeTasksInOrder("task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testCancellationThatEndsProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task2"))
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testCancellationWithWrongProcessInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    try {
      Batch modificationBatch = runtimeService.createProcessInstanceModification("foo")
          .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
          .cancelActivityInstance(getInstanceIdForActivity(tree, "task2"))
          .executeAsync();
      Assertions.assertThat(modificationBatch).isNotNull();
      executeSeedAndBatchJobs(modificationBatch);
      testRule.assertProcessEnded(processInstance.getId());

    } catch (ProcessEngineException e) {
      Assertions.assertThat(e.getMessage()).startsWith("ENGINE-13036");
      Assertions.assertThat(e.getMessage()).contains("Process instance '%s' cannot be modified".formatted("foo"));
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartBefore() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startBeforeActivity("task2")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1")
            .activity("task2")
            .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task2")
        .concurrent()
        .noScope()
        .done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartBeforeWithAncestorInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startBeforeActivity("task2", tree.getId())
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1")
            .activity("task2")
            .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task2")
        .concurrent()
        .noScope()
        .done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = LOOP_PROCESS)
  @Test
  void testStartBeforeWithAncestorInstanceIdWithAncestorCancelled() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("ids", new ArrayList<>(List.of("1")));
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("loopProcess", vars);
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    String ancestorActivityId = getChildInstanceForActivity(tree, "loop").getId();
    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getChildInstanceForActivity(tree, "task").getId())
        .startBeforeActivity("task", ancestorActivityId)
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    try {
      // when
      executeSeedAndBatchJobs(modificationBatch);
      fail(
          "It should not be possible to start before the 'task' activity because the 'task' activity has already been cancelled.");
    } catch (ProcessEngineException e) {
      // then
      testRule.assertTextPresentIgnoreCase(
          "Cannot perform instruction: Start before activity 'task' with ancestor activity instance '"
              + ancestorActivityId + "'; Ancestor activity instance '" + ancestorActivityId
              + "' does not exist: ancestorInstance is null", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartBeforeNonExistingActivity() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    Batch modificationBatch = runtimeService.createProcessInstanceModification(instance.getId()).startBeforeActivity("someNonExistingActivity").executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    try {
      executeSeedAndBatchJobs(modificationBatch);
      fail("should not succeed");
    } catch (NotValidException e) {
      // then
      testRule.assertTextPresentIgnoreCase("element 'someNonExistingActivity' does not exist in process ",
          e.getMessage());
    }
  }

  /**
   * CAM-3718
   */
  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testEndProcessInstanceIntermediately() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .startAfterActivity("task1")
        .startBeforeActivity("task1")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree("task1").scope().done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isOne();

    // complete the process
    completeTasksInOrder("task1");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartTransition() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startTransition("flow4")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1")
            .activity("task2")
            .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task2")
        .concurrent()
        .noScope()
        .done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartTransitionWithAncestorInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startTransition("flow4", tree.getId())
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1")
            .activity("task2")
            .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task2")
        .concurrent()
        .noScope()
        .done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartTransitionInvalidTransitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();
    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstanceId).startTransition("invalidFlowId").executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    try {
      executeSeedAndBatchJobs(modificationBatch);

      fail("should not succeed");

    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("Cannot perform instruction: " + "Start transition 'invalidFlowId'; "
              + "Element 'invalidFlowId' does not exist in process '" + processInstance.getProcessDefinitionId() + "'",
          e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartAfter() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startAfterActivity("theStart")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1")
            .activity("task1")
            .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task1")
        .concurrent()
        .noScope()
        .done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task1");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartAfterWithAncestorInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startAfterActivity("theStart", tree.getId())
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1")
            .activity("task1")
            .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task1")
        .concurrent()
        .noScope()
        .done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task1");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = LOOP_PROCESS)
  @Test
  void testStartAfterWithAncestorInstanceIdWithAncestorCancelled() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("ids", new ArrayList<>(List.of("1")));
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("loopProcess", vars);
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    String ancestorActivityId = getChildInstanceForActivity(tree, "loop").getId();
    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getChildInstanceForActivity(tree, "task").getId())
        .startAfterActivity("task", ancestorActivityId)
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    try {
      // when
      executeSeedAndBatchJobs(modificationBatch);
      fail(
          "It should not be possible to start after the 'task' activity because the 'task' activity has already been cancelled.");
    } catch (ProcessEngineException e) {
      // then
      testRule.assertTextPresentIgnoreCase(
          "Cannot perform instruction: Start after activity 'task' with ancestor activity instance '"
              + ancestorActivityId + "'; Ancestor activity instance '" + ancestorActivityId
              + "' does not exist: ancestorInstance is null", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartAfterActivityAmbiguousTransitions() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();
    Batch modificationBatch = runtimeService
          .createProcessInstanceModification(processInstanceId)
          .startAfterActivity("fork")
          .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    try {
      executeSeedAndBatchJobs(modificationBatch);
      fail("should not succeed since 'fork' has more than one outgoing sequence flow");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("activity has more than one outgoing sequence flow", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartAfterActivityNoOutgoingTransitions() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();
    Batch modificationBatch = runtimeService
          .createProcessInstanceModification(processInstanceId)
          .startAfterActivity("theEnd")
          .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    try {
      executeSeedAndBatchJobs(modificationBatch);
      fail("should not succeed since 'theEnd' has no outgoing sequence flow");

    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("activity has no outgoing sequence flow to take", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartAfterNonExistingActivity() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    Batch modificationBatch = runtimeService
          .createProcessInstanceModification(instance.getId())
          .startAfterActivity("someNonExistingActivity")
          .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    try {
      executeSeedAndBatchJobs(modificationBatch);
      fail("should not succeed");
    } catch (NotValidException e) {
      // then
      testRule.assertTextPresentIgnoreCase(
          "Cannot perform instruction: " + "Start after activity 'someNonExistingActivity'; "
              + "Activity 'someNonExistingActivity' does not exist: activity is null", e.getMessage());
    }
  }

  @Deployment(resources = ONE_SCOPE_TASK_PROCESS)
  @Test
  void testScopeTaskStartBefore() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startBeforeActivity("theTask")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("theTask")
            .activity("theTask")
            .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child(null)
        .concurrent()
        .noScope()
        .child("theTask")
        .scope()
        .up()
        .up()
        .child(null)
        .concurrent()
        .noScope()
        .child("theTask")
        .scope()
        .done());

    Assertions.assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    completeTasksInOrder("theTask", "theTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_SCOPE_TASK_PROCESS)
  @Test
  void testScopeTaskStartAfter() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // when starting after the task, essentially nothing changes in the process
    // instance
    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startAfterActivity("theTask")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("theTask").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope().child("theTask").scope().done());

    // when starting after the start event, regular concurrency happens
    Batch modificationBatch2 = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startAfterActivity("theStart")
        .executeAsync();
    Assertions.assertThat(modificationBatch2).isNotNull();
    executeSeedAndBatchJobs(modificationBatch2);

    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("theTask")
            .activity("theTask")
            .done());

    executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child(null)
        .concurrent()
        .noScope()
        .child("theTask")
        .scope()
        .up()
        .up()
        .child(null)
        .concurrent()
        .noScope()
        .child("theTask")
        .scope()
        .done());

    completeTasksInOrder("theTask", "theTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = TASK_LISTENER_PROCESS)
  @Test
  void testSkipTaskListenerInvocation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenerProcess", "brum",
        Collections.singletonMap("listener", new RecorderTaskListener()));

    String processInstanceId = processInstance.getId();

    RecorderTaskListener.clear();

    // when I start an activity with "skip listeners" setting
    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstanceId)
        .startBeforeActivity("task")
        .executeAsync(true, false);
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    // then no listeners are invoked
    Assertions.assertThat(RecorderTaskListener.getRecordedEvents()).isEmpty();

    // when I cancel an activity with "skip listeners" setting
    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstanceId);

    Batch batch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getChildInstanceForActivity(activityInstanceTree, "task").getId())
        .executeAsync(true, false);
    Assertions.assertThat(batch).isNotNull();
    executeSeedAndBatchJobs(batch);

    // then no listeners are invoked
    Assertions.assertThat(RecorderTaskListener.getRecordedEvents()).isEmpty();
  }

  @Deployment(resources = IO_MAPPING_PROCESS)
  @Test
  void testSkipIoMappings() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ioMappingProcess");

    // when I start task2
    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .startBeforeActivity("task2")
        .executeAsync(false, true);
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    // then the input mapping should not have executed
    Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
    Assertions.assertThat(task2Execution).isNotNull();

    Assertions.assertThat(runtimeService.getVariable(task2Execution.getId(), "inputMappingExecuted")).isNull();

    // when I cancel task2
    Batch modificationBatch2 = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelAllForActivity("task2")
        .executeAsync(false, true);
    Assertions.assertThat(modificationBatch2).isNotNull();
    executeSeedAndBatchJobs(modificationBatch2);

    // then the output mapping should not have executed
    Assertions.assertThat(runtimeService.getVariable(processInstance.getId(), "outputMappingExecuted")).isNull();
  }

  @Deployment(resources = TRANSITION_LISTENER_PROCESS)
  @Test
  void testStartTransitionListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("transitionListenerProcess",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));

    Batch modificationBatch = runtimeService.createProcessInstanceModification(instance.getId())
        .startTransition("flow2")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    // transition listener should have been invoked
    List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
    Assertions.assertThat(events).hasSize(1);

    RecordedEvent event = events.get(0);
    Assertions.assertThat(event.getTransitionId()).isEqualTo("flow2");

    RecorderExecutionListener.clear();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(instance.getId());

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task2")
        .concurrent()
        .noScope()
        .done());

    completeTasksInOrder("task1", "task2", "task2");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = TRANSITION_LISTENER_PROCESS)
  @Test
  void testStartAfterActivityListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("transitionListenerProcess",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));

    Batch modificationBatch = runtimeService.createProcessInstanceModification(instance.getId())
        .startTransition("flow2")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    // transition listener should have been invoked
    List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
    Assertions.assertThat(events).hasSize(1);

    RecordedEvent event = events.get(0);
    Assertions.assertThat(event.getTransitionId()).isEqualTo("flow2");

    RecorderExecutionListener.clear();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    Assertions.assertThat(updatedTree).isNotNull();
    Assertions.assertThat(updatedTree.getProcessInstanceId()).isEqualTo(instance.getId());

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope()
        .child("task1")
        .concurrent()
        .noScope()
        .up()
        .child("task2")
        .concurrent()
        .noScope()
        .done());

    completeTasksInOrder("task1", "task2", "task2");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testCancellationAndStartBefore() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    Batch modificationBatch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .startBeforeActivity("task2")
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(activityInstanceTree).isNotNull();
    Assertions.assertThat(activityInstanceTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    ActivityInstanceAssert.assertThat(activityInstanceTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree("task2").scope().done());

    completeTasksInOrder("task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testCancelNonExistingActivityInstance() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    Batch modificationBatch = runtimeService
          .createProcessInstanceModification(instance.getId())
          .cancelActivityInstance("nonExistingActivityInstance")
          .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    // when - then throw exception
    try {
      executeSeedAndBatchJobs(modificationBatch);
      fail("should not succeed");
    } catch (NotValidException e) {
      testRule.assertTextPresent("Cannot perform instruction: Cancel activity instance 'nonExistingActivityInstance'; "
          + "Activity instance 'nonExistingActivityInstance' does not exist", e.getMessage());
    }

  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testCancelNonExistingTransitionInstance() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    Batch modificationBatch = runtimeService
          .createProcessInstanceModification(instance.getId())
          .cancelTransitionInstance("nonExistingActivityInstance")
          .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();

    // when - then throw exception
    try {
      executeSeedAndBatchJobs(modificationBatch);
      fail("should not succeed");
    } catch (NotValidException e) {
      testRule.assertTextPresent(
          "Cannot perform instruction: Cancel transition instance 'nonExistingActivityInstance'; "
              + "Transition instance 'nonExistingActivityInstance' does not exist", e.getMessage());
    }

  }

  @Deployment(resources = {CALL_ACTIVITY_PARENT_PROCESS, CALL_ACTIVITY_CHILD_PROCESS})
  @Test
  void testCancelCallActivityInstance() {
    // given
    ProcessInstance parentprocess = runtimeService.startProcessInstanceByKey("parentprocess");
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("subprocess")
        .singleResult();

    ActivityInstance subProcessActivityInst = runtimeService.getActivityInstance(subProcess.getId());

    // when
    Batch modificationBatch = runtimeService.createProcessInstanceModification(subProcess.getId())
        .startBeforeActivity("childEnd", subProcess.getId())
        .cancelActivityInstance(getInstanceIdForActivity(subProcessActivityInst, "innerTask"))
        .executeAsync();
    Assertions.assertThat(modificationBatch).isNotNull();
    executeSeedAndBatchJobs(modificationBatch);

    // then
    testRule.assertProcessEnded(parentprocess.getId());
  }

  @Test
  void testModifyNullProcessInstance() {
    try {
      runtimeService.createProcessInstanceModification(null);
      fail("should not succeed");
    } catch (NotValidException e) {
      testRule.assertTextPresent("processInstanceId is null", e.getMessage());
    }
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testSetInvocationsPerBatchType() {
    // given
    processEngineConfiguration.getInvocationsPerBatchJobByBatchType().put(Batch.TYPE_PROCESS_INSTANCE_MODIFICATION, 42);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    Batch batch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task2"))
        .executeAsync();

    // then
    Assertions.assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    processEngineConfiguration.setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  protected void executeSeedAndBatchJobs(Batch batch) {
    Job job = managementService.createJobQuery().jobDefinitionId(batch.getSeedJobDefinitionId()).singleResult();
    // seed job
    managementService.executeJob(job.getId());


    for (Job pending : managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list()) {
      managementService.executeJob(pending.getId());
    }
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

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      Assertions.assertThat(!tasks.isEmpty()).as("task for activity %s does not exist".formatted(taskName)).isTrue();
      taskService.complete(tasks.get(0).getId());
    }
  }
}
