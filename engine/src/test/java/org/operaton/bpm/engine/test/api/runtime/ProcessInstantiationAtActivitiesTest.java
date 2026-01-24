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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class ProcessInstantiationAtActivitiesTest {

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";
  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String LISTENERS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationAtActivitiesTest.listeners.bpmn20.xml";
  protected static final String IO_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationAtActivitiesTest.ioMappings.bpmn20.xml";
  protected static final String ASYNC_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGatewayAsyncTask.bpmn20.xml";
  protected static final String SYNC_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationAtActivitiesTest.synchronous.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ManagementService managementService;
  IdentityService identityService;
  TaskService taskService;

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testSingleActivityInstantiation() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task1")
      .execute();

    // then
    assertThat(instance).isNotNull();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("task1")
      .done());

    // and it is possible to end the process
    completeTasksInOrder("task1");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testSingleActivityInstantiationById() {
    // given
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceById(processDefinitionId)
      .startBeforeActivity("task1")
      .execute();

    // then
    assertThat(instance).isNotNull();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("task1")
      .done());

    // and it is possible to end the process
    completeTasksInOrder("task1");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testSingleActivityInstantiationSetBusinessKey() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .businessKey("businessKey")
      .startBeforeActivity("task1")
      .execute();

    // then
    assertThat(instance).isNotNull();
    assertThat(instance.getBusinessKey()).isEqualTo("businessKey");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testSingleActivityInstantiationSetCaseInstanceId() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .caseInstanceId("caseInstanceId")
      .startBeforeActivity("task1")
      .execute();

    // then
    assertThat(instance).isNotNull();
    assertThat(instance.getCaseInstanceId()).isEqualTo("caseInstanceId");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartEventInstantiation() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("theStart")
      .execute();

    // then
    assertThat(instance).isNotNull();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("task1")
      .done());

    // and it is possible to end the process
    completeTasksInOrder("task1");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartEventInstantiationWithVariables() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("theStart")
      .setVariable("aVariable", "aValue")
      .execute();

    // then
    assertThat(instance).isNotNull();

    assertThat(runtimeService.getVariable(instance.getId(), "aVariable")).isEqualTo("aValue");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testStartWithInvalidInitialActivity() {
    // given
    var processInstantiationBuilder = runtimeService
          .createProcessInstanceByKey("exclusiveGateway")
          .startBeforeActivity("someNonExistingActivity");

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(NotValidException.class)
        .message()
        .containsIgnoringCase("element 'someNonExistingActivity' does not exist in process ");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testMultipleActivitiesInstantiation() {

    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task1")
      .startBeforeActivity("task2")
      .startBeforeActivity("task1")
      .execute();

    // then
    assertThat(instance).isNotNull();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("task1")
        .activity("task2")
        .activity("task1")
      .done());

    // and it is possible to end the process
    completeTasksInOrder("task1", "task2", "task1");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testMultipleActivitiesInstantiationWithVariables() {
    // when
    runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task1")
        .setVariableLocal("aVar1", "aValue1")
      .startBeforeActivity("task2")
        .setVariableLocal("aVar2", "aValue2")
      .execute();

    // then
    // variables for task2's execution
    Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
    assertThat(task2Execution).isNotNull();
    assertThat(runtimeService.getVariableLocal(task2Execution.getId(), "aVar1")).isNull();
    assertThat(runtimeService.getVariableLocal(task2Execution.getId(), "aVar2")).isEqualTo("aValue2");

    // variables for task1's execution
    Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
    assertThat(task1Execution).isNotNull();

    assertThat(runtimeService.getVariableLocal(task1Execution.getId(), "aVar2")).isNull();

    // this variable is not a local variable on execution1 due to tree expansion
    assertThat(runtimeService.getVariableLocal(task1Execution.getId(), "aVar1")).isNull();
    assertThat(runtimeService.getVariable(task1Execution.getId(), "aVar1")).isEqualTo("aValue1");

  }

  @Deployment(resources = SUBPROCESS_PROCESS)
  @Test
  void testNestedActivitiesInstantiation() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("subprocess")
      .startBeforeActivity("innerTask")
      .startBeforeActivity("outerTask")
      .startBeforeActivity("innerTask")
      .execute();

    // then
    assertThat(instance).isNotNull();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .activity("outerTask")
        .beginScope("subProcess")
          .activity("innerTask")
          .activity("innerTask")
      .done());

    // and it is possible to end the process
    completeTasksInOrder("innerTask", "innerTask", "outerTask", "innerTask");
    testRule.assertProcessEnded(instance.getId());
  }

  @Test
  void testStartNonExistingProcessDefinition() {
    // given
    var processInstantiationBuilder1 = runtimeService.createProcessInstanceById("I don't exist").startBeforeActivity("start");

    // when/then
    assertThatThrownBy(processInstantiationBuilder1::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("no deployed process definition found with id");

    // given
    var processInstantiationBuilder2 = runtimeService.createProcessInstanceByKey("I don't exist either").startBeforeActivity("start");

    // when/then
    assertThatThrownBy(processInstantiationBuilder2::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("no processes deployed with key");
  }

  @Test
  void testStartNullProcessDefinition() {
    var processInstantiationBuilder1 = runtimeService.createProcessInstanceById(null).startBeforeActivity("start");
    assertThatThrownBy(processInstantiationBuilder1::execute).isInstanceOf(ProcessEngineException.class);

    var processInstantiationBuilder2 = runtimeService.createProcessInstanceByKey(null).startBeforeActivity("start");
    assertThatThrownBy(processInstantiationBuilder2::execute).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = LISTENERS_PROCESS)
  @Test
  void testListenerInvocation() {
    RecorderExecutionListener.clear();

    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("listenerProcess")
      .startBeforeActivity("innerTask")
      .execute();

    // then
    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .beginScope("subProcess")
          .activity("innerTask")
      .done());

    List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
    assertThat(events).hasSize(3);

    RecordedEvent processStartEvent = events.get(0);
    assertThat(processStartEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);
    assertThat(processStartEvent.getActivityId()).isEqualTo("innerTask");

    RecordedEvent subProcessStartEvent = events.get(1);
    assertThat(subProcessStartEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);
    assertThat(subProcessStartEvent.getActivityId()).isEqualTo("subProcess");

    RecordedEvent innerTaskStartEvent = events.get(2);
    assertThat(innerTaskStartEvent.getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);
    assertThat(innerTaskStartEvent.getActivityId()).isEqualTo("innerTask");

  }

  @Deployment(resources = LISTENERS_PROCESS)
  @Test
  void testSkipListenerInvocation() {
    RecorderExecutionListener.clear();

    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("listenerProcess")
      .startBeforeActivity("innerTask")
      .execute(true, true);

    // then
    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .beginScope("subProcess")
          .activity("innerTask")
      .done());

    List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
    assertThat(events).isEmpty();
  }

  @Deployment(resources = IO_PROCESS)
  @Test
  void testIoMappingInvocation() {
    // when
    runtimeService
      .createProcessInstanceByKey("ioProcess")
      .startBeforeActivity("innerTask")
      .execute();

    // then no io mappings have been executed
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
        .orderByVariableName().asc().list();
    assertThat(variables).hasSize(2);

    Execution innerTaskExecution = runtimeService.createExecutionQuery().activityId("innerTask").singleResult();
    VariableInstance innerTaskVariable = variables.get(0);
    assertThat(innerTaskVariable.getName()).isEqualTo("innerTaskVariable");
    assertThat(innerTaskVariable.getValue()).isEqualTo("innerTaskValue");
    assertThat(innerTaskVariable.getExecutionId()).isEqualTo(innerTaskExecution.getId());

    VariableInstance subProcessVariable = variables.get(1);
    assertThat(subProcessVariable.getName()).isEqualTo("subProcessVariable");
    assertThat(subProcessVariable.getValue()).isEqualTo("subProcessValue");
    assertThat(subProcessVariable.getExecutionId()).isEqualTo(((ExecutionEntity) innerTaskExecution).getParentId());
  }

  @Deployment(resources = IO_PROCESS)
  @Test
  void testSkipIoMappingInvocation() {
    // when
    runtimeService
      .createProcessInstanceByKey("ioProcess")
      .startBeforeActivity("innerTask")
      .execute(true, true);

    // then no io mappings have been executed
    assertThat(runtimeService.createVariableInstanceQuery().count()).isZero();
  }

  @Deployment(resources = SUBPROCESS_PROCESS)
  @Test
  void testSetProcessInstanceVariable() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("subprocess")
      .setVariable("aVariable1", "aValue1")
      .setVariableLocal("aVariable2", "aValue2")
      .setVariables(Variables.createVariables().putValue("aVariable3", "aValue3"))
      .setVariablesLocal(Variables.createVariables().putValue("aVariable4", "aValue4"))
      .startBeforeActivity("innerTask")
      .execute();

    // then
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
        .orderByVariableName().asc().list();

    assertThat(variables).hasSize(4);
    assertThat(variables.get(0).getName()).isEqualTo("aVariable1");
    assertThat(variables.get(0).getValue()).isEqualTo("aValue1");
    assertThat(variables.get(0).getExecutionId()).isEqualTo(instance.getId());

    assertThat(variables.get(1).getName()).isEqualTo("aVariable2");
    assertThat(variables.get(1).getValue()).isEqualTo("aValue2");
    assertThat(variables.get(1).getExecutionId()).isEqualTo(instance.getId());

    assertThat(variables.get(2).getName()).isEqualTo("aVariable3");
    assertThat(variables.get(2).getValue()).isEqualTo("aValue3");
    assertThat(variables.get(2).getExecutionId()).isEqualTo(instance.getId());

    assertThat(variables.get(3).getName()).isEqualTo("aVariable4");
    assertThat(variables.get(3).getValue()).isEqualTo("aValue4");
    assertThat(variables.get(3).getExecutionId()).isEqualTo(instance.getId());

  }

  @Deployment(resources = ASYNC_PROCESS)
  @Test
  void testStartAsyncTask() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task2")
      .execute();

    // then
    assertThat(instance).isNotNull();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(instance.getId());
    assertThat(updatedTree).isNotNull();

    ActivityInstanceAssert.assertThat(updatedTree).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        .transition("task2")
      .done());

    // and it is possible to end the process
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    completeTasksInOrder("task2");
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = SYNC_PROCESS)
  @Test
  void testStartMultipleTasksInSyncProcess() {
    RecorderExecutionListener.clear();

    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("syncProcess")
      .startBeforeActivity("syncTask")
      .startBeforeActivity("syncTask")
      .startBeforeActivity("syncTask")
      .execute();

    // then the request was successful even though the process instance has already ended
    assertThat(instance).isNotNull();
    testRule.assertProcessEnded(instance.getId());

    // and the execution listener was invoked correctly
    List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
    assertThat(events).hasSize(8);

    // process start event
    assertThat(events.get(0).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);
    assertThat(events.get(0).getActivityId()).isEqualTo("syncTask");

    // start instruction 1
    assertThat(events.get(1).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);
    assertThat(events.get(1).getActivityId()).isEqualTo("syncTask");
    assertThat(events.get(2).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(events.get(2).getActivityId()).isEqualTo("syncTask");

    // start instruction 2
    assertThat(events.get(3).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);
    assertThat(events.get(3).getActivityId()).isEqualTo("syncTask");
    assertThat(events.get(4).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(events.get(4).getActivityId()).isEqualTo("syncTask");

    // start instruction 3
    assertThat(events.get(5).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_START);
    assertThat(events.get(5).getActivityId()).isEqualTo("syncTask");
    assertThat(events.get(6).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(events.get(6).getActivityId()).isEqualTo("syncTask");

    // process end event
    assertThat(events.get(7).getEventName()).isEqualTo(ExecutionListener.EVENTNAME_END);
    assertThat(events.get(7).getActivityId()).isEqualTo("end");
  }

  @Deployment
  @Test
  void testInitiatorVariable() {
    // given
    identityService.setAuthenticatedUserId("kermit");

    // when
    ProcessInstance instance = runtimeService
        .createProcessInstanceByKey("initiatorProcess")
        .startBeforeActivity("task")
        .execute();

    // then
    String initiator = (String) runtimeService.getVariable(instance.getId(), "initiator");
    assertThat(initiator).isEqualTo("kermit");

    identityService.clearAuthentication();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/concurrentExecutionVariableWithSubprocess.bpmn20.xml"})
  void shouldFinishProcessWithIoMappingAndEventSubprocess() {
    // given
    // Start process instance before the FirstTask (UserTask) with I/O mapping
    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("process")
        .startBeforeActivity("FirstTask")
        .execute();

    // There should be one execution in the event sub process since the condition is met
    List<Execution> executions = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("Event_0s2zckl")
        .list();
    assertThat(executions).hasSize(1);

    // when the user tasks are completed
    String id = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
    taskService.complete(id);
    id = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
    taskService.complete(id);

    // then
    // Sub process should finish since the second condition should be true as well
    executions = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("Event_0s2zckl")
        .list();
    assertThat(executions).isEmpty();
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
