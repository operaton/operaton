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

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.standalone.deploy.SingleVariableListener;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
class ProcessInstanceModificationTest {

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";
  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String SUBPROCESS_LISTENER_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocessListeners.bpmn20.xml";
  protected static final String SUBPROCESS_BOUNDARY_EVENTS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocessBoundaryEvents.bpmn20.xml";
  protected static final String ONE_SCOPE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.oneScopeTaskProcess.bpmn20.xml";
  protected static final String TRANSITION_LISTENER_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.transitionListeners.bpmn20.xml";
  protected static final String TASK_LISTENER_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.taskListeners.bpmn20.xml";
  protected static final String IO_MAPPING_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.ioMapping.bpmn20.xml";
  protected static final String IO_MAPPING_ON_SUB_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.ioMappingOnSubProcess.bpmn20.xml";
  protected static final String IO_MAPPING_ON_SUB_PROCESS_AND_NESTED_SUB_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.ioMappingOnSubProcessNested.bpmn20.xml";
  protected static final String LISTENERS_ON_SUB_PROCESS_AND_NESTED_SUB_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.listenersOnSubProcessNested.bpmn20.xml";
  protected static final String DOUBLE_NESTED_SUB_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.doubleNestedSubprocess.bpmn20.xml";
  protected static final String TRANSACTION_WITH_COMPENSATION_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.testTransactionWithCompensation.bpmn20.xml";
  protected static final String CALL_ACTIVITY_PARENT_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.testCancelCallActivityParentProcess.bpmn";
  protected static final String CALL_ACTIVITY_CHILD_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.testCancelCallActivityChildProcess.bpmn";
  protected static final BpmnModelInstance SIMPLE_TASK_PROCESS_WITH_DELETE_LISTENER = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask")
        .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, SingleVariableListener.class)
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  HistoryService historyService;

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testCancellation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "task1")).execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task2").done());

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

    runtimeService.createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task2"))
      .execute();

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testCancellationWithWrongProcessInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    try {
      runtimeService.createProcessInstanceModification("foo")
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task2"))
        .execute();
      testRule.assertProcessEnded(processInstance.getId());

    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).startsWith("ENGINE-13036");
      assertThat(e.getMessage()).contains("Process instance '" + "foo" + "' cannot be modified");
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartBefore() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("task2").execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task2").concurrent().noScope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

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

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("task2", tree.getId()).execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task2").concurrent().noScope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = DOUBLE_NESTED_SUB_PROCESS)
  @Test
  void testStartBeforeWithAncestorInstanceIdTwoScopesUp() {
    // given two instances of the outer subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("doubleNestedSubprocess");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("subProcess").execute();
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("innerSubProcessTask");

    // when I start the inner subprocess task without explicit ancestor
    try {
      processInstanceModificationBuilder.execute();
      // then the command fails
      fail("should not succeed because the ancestors are ambiguous");
    } catch (ProcessEngineException e) {
      // happy path
    }

    // when I start the inner subprocess task with an explicit ancestor activity
    // instance id
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    ActivityInstance randomSubProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");

    // then the command succeeds
    runtimeService.createProcessInstanceModification(processInstanceId).startBeforeActivity("innerSubProcessTask", randomSubProcessInstance.getId()).execute();

    // and the trees are correct
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree)
        .hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("subProcess").activity("subProcessTask").endScope()
            .beginScope("subProcess").activity("subProcessTask").beginScope("innerSubProcess").activity("innerSubProcessTask").done());

    ActivityInstance innerSubProcessInstance = getChildInstanceForActivity(updatedTree, "innerSubProcess");
    assertThat(innerSubProcessInstance.getParentActivityInstanceId()).isEqualTo(randomSubProcessInstance.getId());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope().child(null).concurrent().noScope().child("subProcessTask").scope().up().up()
        .child(null).concurrent().noScope().child(null).scope().child("subProcessTask").concurrent().noScope().up().child(null).concurrent().noScope()
        .child("innerSubProcessTask").scope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

    // complete the process
    completeTasksInOrder("subProcessTask", "subProcessTask", "innerSubProcessTask", "innerSubProcessTask", "innerSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = DOUBLE_NESTED_SUB_PROCESS)
  @Test
  void testStartBeforeWithInvalidAncestorInstanceId() {
    // given two instances of the outer subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("doubleNestedSubprocess");
    String processInstanceId = processInstance.getId();
    var processInstanceModificationInstantiationBuilder = runtimeService.createProcessInstanceModification(
      processInstance.getId()).startBeforeActivity("subProcess", "noValidActivityInstanceId");

    try {
      processInstanceModificationInstantiationBuilder.execute();
      fail("Exception expected");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("Cannot perform instruction: " + "Start before activity 'subProcess' with ancestor activity instance 'noValidActivityInstanceId'; "
          + "Ancestor activity instance 'noValidActivityInstanceId' does not exist", e.getMessage());
    }

    var processInstanceModificationBuilder1 = runtimeService.createProcessInstanceModification(processInstance.getId());
    try {
      processInstanceModificationBuilder1.startBeforeActivity("subProcess", null);
      fail("Exception expected");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("ancestorActivityInstanceId is null", e.getMessage());
    }

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    String subProcessTaskId = getInstanceIdForActivity(tree, "subProcessTask");

    var processInstanceModificationInstantiationBuilder2 = runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("subProcess", subProcessTaskId);
    try {
      processInstanceModificationInstantiationBuilder2.execute();
      fail("should not succeed because subProcessTask is a child of subProcess");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("Cannot perform instruction: " + "Start before activity 'subProcess' with ancestor activity instance '" + subProcessTaskId + "'; "
          + "Scope execution for '" + subProcessTaskId + "' cannot be found in parent hierarchy of flow element 'subProcess'", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartBeforeNonExistingActivity() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(instance.getId()).startBeforeActivity("someNonExistingActivity");

    try {
      // when
      processInstanceModificationBuilder.execute();
      fail("should not succeed");
    } catch (NotValidException e) {
      // then
      testRule.assertTextPresentIgnoreCase("element 'someNonExistingActivity' does not exist in process ", e.getMessage());
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

    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .startAfterActivity("task1").startBeforeActivity("task1").execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree("task1").scope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    // complete the process
    completeTasksInOrder("task1");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartTransition() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startTransition("flow4").execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task2").concurrent().noScope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

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

    runtimeService.createProcessInstanceModification(processInstance.getId()).startTransition("flow4", tree.getId()).execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task2").concurrent().noScope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = DOUBLE_NESTED_SUB_PROCESS)
  @Test
  void testStartTransitionWithAncestorInstanceIdTwoScopesUp() {
    // given two instances of the outer subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("doubleNestedSubprocess");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("subProcess").execute();
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId()).startTransition("flow5");

    // when I start the inner subprocess task without explicit ancestor
    try {
      processInstanceModificationBuilder.execute();
      // then the command fails
      fail("should not succeed because the ancestors are ambiguous");
    } catch (ProcessEngineException e) {
      // happy path
    }

    // when I start the inner subprocess task with an explicit ancestor activity
    // instance id
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    ActivityInstance randomSubProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");

    // then the command succeeds
    runtimeService.createProcessInstanceModification(processInstanceId).startTransition("flow5", randomSubProcessInstance.getId()).execute();

    // and the trees are correct
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree)
        .hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("subProcess").activity("subProcessTask").endScope()
            .beginScope("subProcess").activity("subProcessTask").beginScope("innerSubProcess").activity("innerSubProcessTask").done());

    ActivityInstance innerSubProcessInstance = getChildInstanceForActivity(updatedTree, "innerSubProcess");
    assertThat(innerSubProcessInstance.getParentActivityInstanceId()).isEqualTo(randomSubProcessInstance.getId());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope().child(null).concurrent().noScope().child("subProcessTask").scope().up().up()
        .child(null).concurrent().noScope().child(null).scope().child("subProcessTask").concurrent().noScope().up().child(null).concurrent().noScope()
        .child("innerSubProcessTask").scope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

    // complete the process
    completeTasksInOrder("subProcessTask", "subProcessTask", "innerSubProcessTask", "innerSubProcessTask", "innerSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = DOUBLE_NESTED_SUB_PROCESS)
  @Test
  void testStartTransitionWithInvalidAncestorInstanceId() {
    // given two instances of the outer subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("doubleNestedSubprocess");
    String processInstanceId = processInstance.getId();
    var processInstanceModificationInstantiationBuilder1 = runtimeService.createProcessInstanceModification(processInstanceId)
      .startTransition("flow5", "noValidActivityInstanceId");

    try {
      processInstanceModificationInstantiationBuilder1.execute();
      fail("Exception expected");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("Cannot perform instruction: " + "Start transition 'flow5' with ancestor activity instance 'noValidActivityInstanceId'; "
          + "Ancestor activity instance 'noValidActivityInstanceId' does not exist", e.getMessage());
    }

    var processInstanceModificationInstantiationBuilder2 = runtimeService.createProcessInstanceModification(processInstanceId);
    try {
      processInstanceModificationInstantiationBuilder2.startTransition("flow5", null);
      fail("Exception expected");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("ancestorActivityInstanceId is null", e.getMessage());
    }

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    String subProcessTaskId = getInstanceIdForActivity(tree, "subProcessTask");
    var processInstanceModificationInstantiationBuilder3 = runtimeService.createProcessInstanceModification(processInstanceId)
      .startTransition("flow5", subProcessTaskId);

    try {
      processInstanceModificationInstantiationBuilder3.execute();
      fail("should not succeed because subProcessTask is a child of subProcess");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("Cannot perform instruction: " + "Start transition 'flow5' with ancestor activity instance '" + subProcessTaskId + "'; "
          + "Scope execution for '" + subProcessTaskId + "' cannot be found in parent hierarchy of flow element 'flow5'", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartTransitionCase2() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startTransition("flow2").execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task1").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task1").concurrent().noScope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task1");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartTransitionInvalidTransitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstanceId).startTransition("invalidFlowId");

    try {
      processInstanceModificationBuilder.execute();

      fail("should not succeed");

    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("Cannot perform instruction: " + "Start transition 'invalidFlowId'; " + "Element 'invalidFlowId' does not exist in process '"
          + processInstance.getProcessDefinitionId() + "'", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartAfter() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("theStart").execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task1").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task1").concurrent().noScope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

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

    runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("theStart", tree.getId()).execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task1").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task1").concurrent().noScope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // complete the process
    completeTasksInOrder("task1", "task1");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = DOUBLE_NESTED_SUB_PROCESS)
  @Test
  void testStartAfterWithAncestorInstanceIdTwoScopesUp() {
    // given two instances of the outer subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("doubleNestedSubprocess");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("subProcess").execute();
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("innerSubProcessStart");

    // when I start the inner subprocess task without explicit ancestor
    try {
      processInstanceModificationBuilder.execute();
      // then the command fails
      fail("should not succeed because the ancestors are ambiguous");
    } catch (ProcessEngineException e) {
      // happy path
    }

    // when I start the inner subprocess task with an explicit ancestor activity
    // instance id
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    ActivityInstance randomSubProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");

    // then the command succeeds
    runtimeService.createProcessInstanceModification(processInstanceId).startAfterActivity("innerSubProcessStart", randomSubProcessInstance.getId()).execute();

    // and the trees are correct
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree)
        .hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("subProcess").activity("subProcessTask").endScope()
            .beginScope("subProcess").activity("subProcessTask").beginScope("innerSubProcess").activity("innerSubProcessTask").done());

    ActivityInstance innerSubProcessInstance = getChildInstanceForActivity(updatedTree, "innerSubProcess");
    assertThat(innerSubProcessInstance.getParentActivityInstanceId()).isEqualTo(randomSubProcessInstance.getId());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope().child(null).concurrent().noScope().child("subProcessTask").scope().up().up()
        .child(null).concurrent().noScope().child(null).scope().child("subProcessTask").concurrent().noScope().up().child(null).concurrent().noScope()
        .child("innerSubProcessTask").scope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

    // complete the process
    completeTasksInOrder("subProcessTask", "subProcessTask", "innerSubProcessTask", "innerSubProcessTask", "innerSubProcessTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = DOUBLE_NESTED_SUB_PROCESS)
  @Test
  void testStartAfterWithInvalidAncestorInstanceId() {
    // given two instances of the outer subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("doubleNestedSubprocess");
    String processInstanceId = processInstance.getId();
    var processInstanceModificationInstantiationBuilder1 = runtimeService.createProcessInstanceModification(processInstanceId)
      .startAfterActivity("innerSubProcessStart", "noValidActivityInstanceId");

    try {
      processInstanceModificationInstantiationBuilder1.execute();
      fail("Exception expected");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent(
          "Cannot perform instruction: " + "Start after activity 'innerSubProcessStart' with ancestor activity instance 'noValidActivityInstanceId'; "
              + "Ancestor activity instance 'noValidActivityInstanceId' does not exist",
          e.getMessage());
    }

    var processInstanceModificationInstantiationBuilder2 = runtimeService.createProcessInstanceModification(processInstanceId);
    try {
      processInstanceModificationInstantiationBuilder2.startAfterActivity("innerSubProcessStart", null);
      fail("Exception expected");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("ancestorActivityInstanceId is null", e.getMessage());
    }

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    String subProcessTaskId = getInstanceIdForActivity(tree, "subProcessTask");

    var processInstanceModificationInstantiationBuilder3 = runtimeService.createProcessInstanceModification(processInstanceId)
      .startAfterActivity("innerSubProcessStart", subProcessTaskId);
    try {
      processInstanceModificationInstantiationBuilder3.execute();
      fail("should not succeed because subProcessTask is a child of subProcess");
    } catch (NotValidException e) {
      // happy path
      testRule.assertTextPresent("Cannot perform instruction: " + "Start after activity 'innerSubProcessStart' with ancestor activity instance '" + subProcessTaskId
          + "'; " + "Scope execution for '" + subProcessTaskId + "' cannot be found in parent hierarchy of flow element 'flow5'", e.getMessage());
    }
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartAfterActivityAmbiguousTransitions() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstanceId).startAfterActivity("fork");

    try {
      processInstanceModificationBuilder.execute();

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
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstanceId).startAfterActivity("theEnd");

    try {
      processInstanceModificationBuilder.execute();

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
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(instance.getId()).startAfterActivity("someNonExistingActivity");

    try {
      // when
      processInstanceModificationBuilder.execute();
      fail("should not succeed");
    } catch (NotValidException e) {
      // then
      testRule.assertTextPresentIgnoreCase("Cannot perform instruction: " + "Start after activity 'someNonExistingActivity'; "
          + "Activity 'someNonExistingActivity' does not exist: activity is null", e.getMessage());
    }
  }

  @Deployment(resources = ONE_SCOPE_TASK_PROCESS)
  @Test
  void testScopeTaskStartBefore() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("theTask").execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("theTask").activity("theTask").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope().child(null).concurrent().noScope().child("theTask").scope().up().up().child(null)
        .concurrent().noScope().child("theTask").scope().done());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
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
    runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("theTask").execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("theTask").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope().child("theTask").scope().done());

    // when starting after the start event, regular concurrency happens
    runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("theStart").execute();

    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("theTask").activity("theTask").done());

    executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree(null).scope().child(null).concurrent().noScope().child("theTask").scope().up().up().child(null)
        .concurrent().noScope().child("theTask").scope().done());

    completeTasksInOrder("theTask", "theTask");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = SUBPROCESS_BOUNDARY_EVENTS_PROCESS)
  @Test
  void testStartBeforeEventSubscription() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("innerTask").execute();

    // then two timer jobs should have been created
    assertThat(managementService.createJobQuery().count()).isEqualTo(2);
    Job innerJob = managementService.createJobQuery().activityId("innerTimer").singleResult();
    assertThat(innerJob).isNotNull();
    assertThat(innerJob.getExecutionId()).isEqualTo(runtimeService.createExecutionQuery().activityId("innerTask").singleResult().getId());

    Job outerJob = managementService.createJobQuery().activityId("outerTimer").singleResult();
    assertThat(outerJob).isNotNull();

    // when executing the jobs
    managementService.executeJob(innerJob.getId());

    Task innerBoundaryTask = taskService.createTaskQuery().taskDefinitionKey("innerAfterBoundaryTask").singleResult();
    assertThat(innerBoundaryTask).isNotNull();

    managementService.executeJob(outerJob.getId());

    Task outerBoundaryTask = taskService.createTaskQuery().taskDefinitionKey("outerAfterBoundaryTask").singleResult();
    assertThat(outerBoundaryTask).isNotNull();

  }

  @Deployment(resources = SUBPROCESS_LISTENER_PROCESS)
  @Test
  void testActivityExecutionListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess",
        Collections.singletonMap("listener", new RecorderExecutionListener()));

    String processInstanceId = processInstance.getId();

    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("innerTask").execute();

    // assert activity instance tree
    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(activityInstanceTree).isNotNull();
    assertThat(activityInstanceTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(activityInstanceTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("outerTask").beginScope("subProcess").activity("innerTask").done());

    // assert listener invocations
    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(2);

    ActivityInstance subprocessInstance = getChildInstanceForActivity(activityInstanceTree, "subProcess");
    ActivityInstance innerTaskInstance = getChildInstanceForActivity(subprocessInstance, "innerTask");

    RecordedEvent firstEvent = recordedEvents.get(0);
    RecordedEvent secondEvent = recordedEvents.get(1);

    assertThat(firstEvent.getActivityId()).isEqualTo("subProcess");
    assertThat(firstEvent.getActivityInstanceId()).isEqualTo(subprocessInstance.getId());
    assertThat(secondEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);

    assertThat(secondEvent.getActivityId()).isEqualTo("innerTask");
    assertThat(secondEvent.getActivityInstanceId()).isEqualTo(innerTaskInstance.getId());
    assertThat(secondEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);

    RecorderExecutionListener.clear();

    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(innerTaskInstance.getId()).execute();

    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(2);
  }

  @Deployment(resources = SUBPROCESS_LISTENER_PROCESS)
  @Test
  void testSkipListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess",
        Collections.singletonMap("listener", new RecorderExecutionListener()));

    String processInstanceId = processInstance.getId();

    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();

    // when I start an activity with "skip listeners" setting
    runtimeService.createProcessInstanceModification(processInstanceId).startBeforeActivity("innerTask").execute(true, false);

    // then no listeners are invoked
    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();

    // when I cancel an activity with "skip listeners" setting
    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstanceId);

    runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getChildInstanceForActivity(activityInstanceTree, "innerTask").getId()).execute(true, false);

    // then no listeners are invoked
    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();

    // when I cancel an activity that ends the process instance
    runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getChildInstanceForActivity(activityInstanceTree, "outerTask").getId()).execute(true, false);

    // then no listeners are invoked
    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Test
  void shouldSkipCustomListenersOnProcessInstanceModification() {
    //given
    testRule.deploy(SIMPLE_TASK_PROCESS_WITH_DELETE_LISTENER);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);

    //when
    runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelAllForActivity("userTask")
        .startAfterActivity("userTask")
        .execute(true, false);

    //then
    HistoricVariableInstance isListenerCalled = historyService.createHistoricVariableInstanceQuery()
        .variableName("isListenerCalled")
        .singleResult();

    assertThat(isListenerCalled).isNull();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Test
  void shouldNotSkipCustomListenersOnProcessInstanceModification() {
    //given
    testRule.deploy(SIMPLE_TASK_PROCESS_WITH_DELETE_LISTENER);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);

    //when
    runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelAllForActivity("userTask")
        .startAfterActivity("userTask")
        .execute(false, false);

    //then
    HistoricVariableInstance isListenerCalled = historyService.createHistoricVariableInstanceQuery()
        .variableName("isListenerCalled")
        .singleResult();

    assertThat(isListenerCalled).isNotNull();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Test
  void shouldNotSkipCustomListenersWithoutFlagPassedOnProcessInstanceModification() {
    //given
    testRule.deploy(SIMPLE_TASK_PROCESS_WITH_DELETE_LISTENER);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);

    //when
    runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelAllForActivity("userTask")
        .startAfterActivity("userTask")
        .execute();

    //then
    HistoricVariableInstance isListenerCalled = historyService.createHistoricVariableInstanceQuery()
        .variableName("isListenerCalled")
        .singleResult();

    assertThat(isListenerCalled).isNotNull();
  }

  @Deployment(resources = TASK_LISTENER_PROCESS)
  @Test
  void testSkipTaskListenerInvocation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenerProcess",
        Collections.singletonMap("listener", new RecorderTaskListener()));

    String processInstanceId = processInstance.getId();

    RecorderTaskListener.clear();

    // when I start an activity with "skip listeners" setting
    runtimeService.createProcessInstanceModification(processInstanceId).startBeforeActivity("task").execute(true, false);

    // then no listeners are invoked
    assertThat(RecorderTaskListener.getRecordedEvents()).isEmpty();

    // when I cancel an activity with "skip listeners" setting
    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstanceId);

    runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getChildInstanceForActivity(activityInstanceTree, "task").getId()).execute(true, false);

    // then no listeners are invoked
    assertThat(RecorderTaskListener.getRecordedEvents()).isEmpty();
  }

  @Deployment(resources = IO_MAPPING_PROCESS)
  @Test
  void testSkipIoMappings() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ioMappingProcess");

    // when I start task2
    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("task2").execute(false, true);

    // then the input mapping should not have executed
    Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
    assertThat(task2Execution).isNotNull();

    assertThat(runtimeService.getVariable(task2Execution.getId(), "inputMappingExecuted")).isNull();

    // when I cancel task2
    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelAllForActivity("task2").execute(false, true);

    // then the output mapping should not have executed
    assertThat(runtimeService.getVariable(processInstance.getId(), "outputMappingExecuted")).isNull();
  }

  @Deployment(resources = IO_MAPPING_ON_SUB_PROCESS)
  @Test
  void testSkipIoMappingsOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("boundaryEvent").execute(false, true);

    // then the output mapping should not have executed
    assertThat(runtimeService.getVariable(processInstance.getId(), "outputMappingExecuted")).isNull();
  }

  /**
   * should also skip io mappings that are defined on already instantiated
   * ancestor scopes and that may be executed due to the ancestor scope
   * completing within the modification command.
   */
  @Deployment(resources = IO_MAPPING_ON_SUB_PROCESS_AND_NESTED_SUB_PROCESS)
  @Test
  void testSkipIoMappingsOnSubProcessNested() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("boundaryEvent").execute(false, true);

    // then the output mapping should not have executed
    assertThat(runtimeService.getVariable(processInstance.getId(), "outputMappingExecuted")).isNull();
  }

  @Deployment(resources = LISTENERS_ON_SUB_PROCESS_AND_NESTED_SUB_PROCESS)
  @Test
  void testSkipListenersOnSubProcessNested() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("boundaryEvent").execute(true, false);

    testRule.assertProcessEnded(processInstance.getId());

    // then the output mapping should not have executed
    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();
  }

  @Deployment(resources = TRANSITION_LISTENER_PROCESS)
  @Test
  void testStartTransitionListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("transitionListenerProcess",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));

    runtimeService.createProcessInstanceModification(instance.getId()).startTransition("flow2").execute();

    // transition listener should have been invoked
    List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
    assertThat(events).hasSize(1);

    RecordedEvent event = events.get(0);
    assertThat(event.getTransitionId()).isEqualTo("flow2");

    RecorderExecutionListener.clear();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(instance.getId());

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(instance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task2").concurrent().noScope().done());

    completeTasksInOrder("task1", "task2", "task2");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = TRANSITION_LISTENER_PROCESS)
  @Test
  void testStartAfterActivityListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("transitionListenerProcess",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));

    runtimeService.createProcessInstanceModification(instance.getId()).startTransition("flow2").execute();

    // transition listener should have been invoked
    List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
    assertThat(events).hasSize(1);

    RecordedEvent event = events.get(0);
    assertThat(event.getTransitionId()).isEqualTo("flow2");

    RecorderExecutionListener.clear();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(instance.getId());

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(instance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(instance.getId(), processEngine);

    assertThat(executionTree)
        .matches(describeExecutionTree(null).scope().child("task1").concurrent().noScope().up().child("task2").concurrent().noScope().done());

    completeTasksInOrder("task1", "task2", "task2");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartBeforeWithVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("task2").setVariable("procInstVar", "procInstValue")
        .setVariableLocal("localVar", "localValue").setVariables(Variables.createVariables().putValue("procInstMapVar", "procInstMapValue"))
        .setVariablesLocal(Variables.createVariables().putValue("localMapVar", "localMapValue")).execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task1").activity("task2").done());

    ActivityInstance task2Instance = getChildInstanceForActivity(updatedTree, "task2");
    Assertions.assertThat(task2Instance).isNotNull();
    assertThat(task2Instance.getExecutionIds()).hasSize(1);
    String task2ExecutionId = task2Instance.getExecutionIds()[0];

    assertThat(runtimeService.createVariableInstanceQuery().count()).isEqualTo(4);
    assertThat(runtimeService.getVariableLocal(processInstance.getId(), "procInstVar")).isEqualTo("procInstValue");
    assertThat(runtimeService.getVariableLocal(task2ExecutionId, "localVar")).isEqualTo("localValue");
    assertThat(runtimeService.getVariableLocal(processInstance.getId(), "procInstMapVar")).isEqualTo("procInstMapValue");
    assertThat(runtimeService.getVariableLocal(task2ExecutionId, "localMapVar")).isEqualTo("localMapValue");

    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testCancellationAndStartBefore() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .startBeforeActivity("task2").execute();

    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(activityInstanceTree).isNotNull();
    assertThat(activityInstanceTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(activityInstanceTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree("task2").scope().done());

    completeTasksInOrder("task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testCompensationRemovalOnCancellation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensationProcess");

    Execution taskExecution = runtimeService.createExecutionQuery().activityId("innerTask").singleResult();
    Task task = taskService.createTaskQuery().executionId(taskExecution.getId()).singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());
    // there should be a compensation event subscription for innerTask now
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    // when innerTask2 is cancelled
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask2")).execute();

    // then the innerTask compensation should be removed
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testCompensationCreation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensationProcess");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("innerTask").execute();

    Execution task2Execution = runtimeService.createExecutionQuery().activityId("innerTask").singleResult();
    Task task = taskService.createTaskQuery().executionId(task2Execution.getId()).singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(3);

    // trigger compensation
    Task outerTask = taskService.createTaskQuery().taskDefinitionKey("outerTask").singleResult();
    assertThat(outerTask).isNotNull();
    taskService.complete(outerTask.getId());

    // then there are two compensation tasks and the afterSubprocessTask:
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("innerAfterBoundaryTask").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("outerAfterBoundaryTask").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("taskAfterSubprocess").count()).isEqualTo(1);

    // complete process
    completeTasksInOrder("taskAfterSubprocess", "innerAfterBoundaryTask", "outerAfterBoundaryTask");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testNoCompensationCreatedOnCancellation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensationProcess");
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // one on outerTask, one on innerTask
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // when inner task is cancelled
    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "innerTask")).execute();

    // then no compensation event subscription exists
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    // and the compensation throw event does not trigger compensation handlers
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("outerTask");

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartActivityInTransactionWithCompensation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    completeTasksInOrder("userTask");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("undoTask");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree)
        .hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask").done());

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("userTask").execute();

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask")
        .activity("userTask").done());

    completeTasksInOrder("userTask");

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree)
        .hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask").done());

    Task newTask = taskService.createTaskQuery().singleResult();
    assertNotSame(task.getId(), newTask.getId());

    completeTasksInOrder("undoTask", "afterCancel");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartActivityWithAncestorInTransactionWithCompensation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    completeTasksInOrder("userTask");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("undoTask");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree)
        .hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask").done());

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("userTask", processInstance.getId()).execute();

    completeTasksInOrder("userTask");

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask")
        .endScope().beginScope("tx").activity("txEnd").activity("undoTask").done());

    completeTasksInOrder("undoTask", "undoTask", "afterCancel", "afterCancel");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartAfterActivityDuringCompensation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    completeTasksInOrder("userTask");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("undoTask");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("userTask").execute();

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("afterCancel");

    completeTasksInOrder("afterCancel");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testCancelCompensatingTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "undoTask")).execute();

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testCancelCompensatingTaskAndStartActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "undoTask"))
        .startBeforeActivity("userTask").execute();

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("userTask").done());

    completeTasksInOrder("userTask", "undoTask", "afterCancel");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testCancelCompensatingTaskAndStartActivityWithAncestor() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId()).cancelActivityInstance(getInstanceIdForActivity(tree, "undoTask"))
        .startBeforeActivity("userTask", processInstance.getId()).execute();

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("userTask").done());

    completeTasksInOrder("userTask", "undoTask", "afterCancel");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartActivityAndCancelCompensatingTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("userTask")
        .cancelActivityInstance(getInstanceIdForActivity(tree, "undoTask")).execute();

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("userTask").done());

    completeTasksInOrder("userTask", "undoTask", "afterCancel");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartCompensatingTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("undoTask").execute();

    completeTasksInOrder("undoTask");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask");

    completeTasksInOrder("userTask", "undoTask", "afterCancel");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartAdditionalCompensatingTaskAndCompleteOldCompensationTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    Task firstUndoTask = taskService.createTaskQuery().singleResult();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("undoTask").execute();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask")
        .activity("undoTask").done());

    taskService.complete(firstUndoTask.getId());

    Task secondUndoTask = taskService.createTaskQuery().taskDefinitionKey("undoTask").singleResult();
    assertThat(secondUndoTask).isNull();

    completeTasksInOrder("afterCancel");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartAdditionalCompensatingTaskAndCompleteNewCompensatingTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    Task firstUndoTask = taskService.createTaskQuery().taskDefinitionKey("undoTask").singleResult();

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("undoTask").setVariableLocal("new", true).execute();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask")
        .activity("undoTask").done());

    String taskExecutionId = runtimeService.createExecutionQuery().variableValueEquals("new", true).singleResult().getId();
    Task secondUndoTask = taskService.createTaskQuery().executionId(taskExecutionId).singleResult();

    assertThat(secondUndoTask).isNotNull();
    assertNotSame(firstUndoTask.getId(), secondUndoTask.getId());
    taskService.complete(secondUndoTask.getId());

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree)
        .hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask").done());

    completeTasksInOrder("undoTask", "afterCancel");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartCompensationBoundary() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    String processInstanceId = processInstance.getId();

    var processInstanceModificationInstantiationBuilder1 = runtimeService.createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("compensateBoundaryEvent");
    try {
      processInstanceModificationInstantiationBuilder1.execute();

      fail("should not succeed");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("compensation boundary event", e.getMessage());
    }

    var processInstanceModificationInstantiationBuilder2 = runtimeService.createProcessInstanceModification(processInstanceId)
      .startAfterActivity("compensateBoundaryEvent");
    try {
      processInstanceModificationInstantiationBuilder2.execute();

      fail("should not succeed");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("no outgoing sequence flow", e.getMessage());
    }
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartCancelEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("txEnd").execute();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("afterCancel");

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartCancelBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("catchCancelTx").execute();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("afterCancel");

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = TRANSACTION_WITH_COMPENSATION_PROCESS)
  @Test
  void testStartTaskAfterCancelBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    completeTasksInOrder("userTask");

    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("afterCancel").execute();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).beginScope("tx").activity("txEnd").activity("undoTask")
        .endScope().activity("afterCancel").done());

    completeTasksInOrder("afterCancel", "undoTask", "afterCancel");

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testCancelNonExistingActivityInstance() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("exclusiveGateway");
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(instance.getId()).cancelActivityInstance("nonExistingActivityInstance");

    // when - then throw exception
    try {
      processInstanceModificationBuilder.execute();
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
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(instance.getId()).cancelTransitionInstance("nonExistingActivityInstance");

    // when - then throw exception
    try {
      processInstanceModificationBuilder.execute();
      fail("should not succeed");
    } catch (NotValidException e) {
      testRule.assertTextPresent("Cannot perform instruction: Cancel transition instance 'nonExistingActivityInstance'; "
          + "Transition instance 'nonExistingActivityInstance' does not exist", e.getMessage());
    }

  }

  @Deployment(resources = { CALL_ACTIVITY_PARENT_PROCESS, CALL_ACTIVITY_CHILD_PROCESS })
  public void FAILING_testCancelCallActivityInstance() {
    // given
    ProcessInstance parentProcess = runtimeService.startProcessInstanceByKey("parentprocess");
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").singleResult();

    ActivityInstance subProcessActivityInst = runtimeService.getActivityInstance(subProcess.getId());

    // when
    runtimeService.createProcessInstanceModification(subProcess.getId()).startBeforeActivity("childEnd", subProcess.getId())
        .cancelActivityInstance(getInstanceIdForActivity(subProcessActivityInst, "innerTask")).execute();

    // then
    testRule.assertProcessEnded(parentProcess.getId());
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

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/concurrentExecutionVariable.bpmn20.xml")
  void shouldNotDeleteVariablesWhenConcurrentExecution() {
    // given process with user task
    String processInstanceId = runtimeService.createProcessInstanceByKey("process")
        .setVariable("featureIssueId", 178825)
        .setVariable("implementationCategory", "category-value")
        .startBeforeActivity("Implement_feature")
        .execute()
        .getId();

    // process variables are there
    assertThat(runtimeService.getVariable(processInstanceId, "featureIssueId"))
        .isEqualTo(178825);
    assertThat(runtimeService.getVariable(processInstanceId, "implementationCategory"))
        .isEqualTo("category-value");
    assertThat(runtimeService.getVariable(processInstanceId, "implementationIssue"))
        .isNull();

    // when: triggering script task that will set `issueId`
    runtimeService.createProcessInstanceModification(processInstanceId)
        .startBeforeActivity("Set_issue_id")
        .execute();

    // then
    // `implementationIssue` is set by event `Implement_feature_issue_id_set` after `issueId` was set
    // and the process variables are still available
    assertThat(runtimeService.getVariable(processInstanceId, "implementationIssue"))
        .isEqualTo(777);
    assertThat(runtimeService.getVariable(processInstanceId, "featureIssueId"))
        .isEqualTo(178825);
    assertThat(runtimeService.getVariable(processInstanceId, "implementationCategory"))
        .isEqualTo("category-value");
  }

  // TODO: check if starting with a non-existing activity/transition id is
  // handled properly

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
      assertThat(!tasks.isEmpty()).as("task for activity " + taskName + " does not exist").isTrue();
      taskService.complete(tasks.get(0).getId());
    }
  }
}
