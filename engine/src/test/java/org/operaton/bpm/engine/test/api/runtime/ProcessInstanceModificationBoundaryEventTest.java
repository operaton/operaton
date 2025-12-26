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
import org.junit.jupiter.params.provider.ValueSource;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ActivityInstance;
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
class ProcessInstanceModificationBoundaryEventTest {

  protected static final String INTERRUPTING_BOUNDARY_EVENT = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEvent.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEvent.bpmn20.xml";

  protected static final String INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEventInsideSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEventInsideSubProcess.bpmn20.xml";

  protected static final String INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEventOnSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEventOnSubProcess.bpmn20.xml";

  protected static final String INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEventWithParallelGateway.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEventWithParallelGateway.bpmn20.xml";

  protected static final String INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY_INSIDE_SUB_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEventWithParallelGatewayInsideSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY_INSIDE_SUB_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEventWithParallelGatewayInsideSubProcess.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT)
  @Test
  void testTask1AndStartBeforeTaskAfterBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .activity("taskAfterBoundaryEvent")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("taskAfterBoundaryEvent").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("task1").scope()
        .done());

    completeTasksInOrder("task1", "task2", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);

  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT)
  @Test
  void testTask1AndStartBeforeBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
    .matches(
      describeExecutionTree("taskAfterBoundaryEvent").scope()
      .done());

    completeTasksInOrder("taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @ParameterizedTest(name = "Start Before: {0}")
  @CsvSource({
    "Task after boundary event, " + INTERRUPTING_BOUNDARY_EVENT + ", taskAfterBoundaryEvent",
    "Boundary event, " + INTERRUPTING_BOUNDARY_EVENT + ", boundaryEvent",
    "Task after non-interrupting boundary event, " + NON_INTERRUPTING_BOUNDARY_EVENT + ", taskAfterBoundaryEvent",
    "Non-interrupting boundary event, " + NON_INTERRUPTING_BOUNDARY_EVENT + ", boundaryEvent"
  })
  void testTask2AndStartBeforeGivenEvent(String name, String bpmnResource, String startBeforeActivity) {
    // given
    testRule.deploy(bpmnResource);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    // when
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity(startBeforeActivity)
      .execute();

    // then
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task2")
        .activity("taskAfterBoundaryEvent")
        .done());


    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("taskAfterBoundaryEvent").concurrent().noScope().up()
          .child("task2").concurrent().noScope()
          .done());

    completeTasksInOrder("task2", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT)
  @Test
  void testTask1AndStartBeforeTaskAfterNonInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("task1")
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("taskAfterBoundaryEvent").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("task1").scope()
        .done());

    completeTasksInOrder("task1", "taskAfterBoundaryEvent", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT)
  @Test
  void testTask1AndStartBeforeNonInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("task1")
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("taskAfterBoundaryEvent").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("task1").scope()
        .done());

    completeTasksInOrder("task1", "taskAfterBoundaryEvent", "task2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS,
    NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS
  })
  void testTask1AndStartBeforeInnerTaskAfterBoundaryEventInsideSubProcess(String bpmnResource) {
    // given
    testRule.deploy(bpmnResource);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    // when
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerTaskAfterBoundaryEvent")
      .execute();

    // then
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("subProcess")
        .activity("innerTask1")
        .activity("innerTaskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
          .child("innerTaskAfterBoundaryEvent").concurrent().noScope().up()
          .child(null).concurrent().noScope()
          .child("innerTask1").scope()
          .done());

    completeTasksInOrder("innerTask1", "innerTaskAfterBoundaryEvent", "innerTask2");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  @Test
  void testTask1AndStartBeforeBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("innerTaskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("innerTaskAfterBoundaryEvent").scope()
        .done());

    completeTasksInOrder("innerTaskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  @Test
  void testTask1AndStartBeforeNonInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("innerTask1")
            .activity("innerTaskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
            .child("innerTaskAfterBoundaryEvent").concurrent().noScope().up()
            .child(null).concurrent().noScope()
              .child("innerTask1").scope()
        .done());

    completeTasksInOrder("innerTask1", "innerTask2", "innerTaskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  @Test
  void testTask2AndStartBeforeTaskAfterBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerTaskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("innerTask2")
            .activity("innerTaskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
            .child("innerTaskAfterBoundaryEvent").concurrent().noScope().up()
            .child("innerTask2").concurrent().noScope()
        .done());

    completeTasksInOrder("innerTask2", "innerTaskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS,
    NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS
  })
  void testTask2AndStartBeforeInnerBoundaryEventInsideSubProcess(String bpmnResource) {
    // given
    testRule.deploy(bpmnResource);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    // when
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerBoundaryEvent")
      .execute();

    // then
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginScope("subProcess")
        .activity("innerTask2")
        .activity("innerTaskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
          .child("innerTaskAfterBoundaryEvent").concurrent().noScope().up()
          .child("innerTask2").concurrent().noScope()
          .done());

    completeTasksInOrder("innerTask2", "innerTaskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  @Test
  void testTask2AndStartBeforeTaskAfterNonInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerTaskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("innerTask2")
            .activity("innerTaskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
            .child("innerTaskAfterBoundaryEvent").concurrent().noScope().up()
            .child("innerTask2").concurrent().noScope()
        .done());

    completeTasksInOrder("innerTask2", "innerTaskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  @Test
  void testStartBeforeTaskAfterBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("innerTask")
          .endScope()
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("taskAfterBoundaryEvent").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("innerTask").scope()
        .done());

    completeTasksInOrder("innerTask", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  @Test
  void testStartBeforeBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree("taskAfterBoundaryEvent").scope()
        .done());

    completeTasksInOrder("taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  @Test
  void testStartBeforeTaskAfterNonInterruptingBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("innerTask")
          .endScope()
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("taskAfterBoundaryEvent").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("innerTask").scope()
        .done());

    completeTasksInOrder("innerTask", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  @Test
  void testStartBeforeNonInterruptingBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("innerTask")
          .endScope()
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("taskAfterBoundaryEvent").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("innerTask").scope()
        .done());

    completeTasksInOrder("innerTask", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY)
  @Test
  void testStartBeforeInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("task1")
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child("taskAfterBoundaryEvent").concurrent().noScope()
        .done());

    completeTasksInOrder("task1", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY)
  @Test
  void testStartBeforeNonInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("task1")
          .activity("task2")
          .activity("taskAfterBoundaryEvent")
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child("task1").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("task2").scope().up().up()
          .child("taskAfterBoundaryEvent").concurrent().noScope()
        .done());

    completeTasksInOrder("task1", "task2", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY_INSIDE_SUB_PROCESS)
  @Test
  void testStartBeforeInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("task1")
            .activity("taskAfterBoundaryEvent")
          .endScope()
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
            .child("task1").concurrent().noScope().up()
            .child("taskAfterBoundaryEvent").concurrent().noScope()
        .done());

    completeTasksInOrder("task1", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_WITH_PARALLEL_GATEWAY_INSIDE_SUB_PROCESS)
  @Test
  void testStartBeforeNonInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .activity("task1")
            .activity("task2")
            .activity("taskAfterBoundaryEvent")
          .endScope()
        .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
          .child(null).scope()
            .child("task1").concurrent().noScope().up()
            .child(null).concurrent().noScope()
              .child("task2").scope().up().up()
            .child("taskAfterBoundaryEvent").concurrent().noScope()
        .done());

    completeTasksInOrder("task1", "task2", "taskAfterBoundaryEvent");
    testRule.assertProcessEnded(processInstanceId);
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
