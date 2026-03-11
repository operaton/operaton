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
package org.operaton.bpm.engine.test.bpmn.async;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
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

class AsyncStartEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;
  ManagementService managementService;

  @Deployment
  @Test
  void testAsyncStartEvent() {
    runtimeService.startProcessInstanceByKey("asyncStartEvent");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).as("The user task should not have been reached yet").isNull();

    assertThat(runtimeService.createExecutionQuery().activityId("startEvent").count()).isOne();

    testRule.executeAvailableJobs();
    task = taskService.createTaskQuery().singleResult();

    assertThat(runtimeService.createExecutionQuery().activityId("startEvent").count()).isZero();

    assertThat(task).as("The user task should have been reached").isNotNull();
  }

  @Deployment
  @Test
  void testAsyncStartEventListeners() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("asyncStartEvent");

    assertThat(runtimeService.getVariable(instance.getId(), "listener")).isNull();

    testRule.executeAvailableJobs();

    assertThat(runtimeService.getVariable(instance.getId(), "listener")).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncStartEvent.bpmn20.xml")
  @Test
  void testAsyncStartEventActivityInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncStartEvent");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .transition("startEvent")
        .done());
  }

  @Deployment
  @Test
  void testMultipleAsyncStartEvents() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    runtimeService.correlateMessage("newInvoiceMessage", new HashMap<>(), variables);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    testRule.executeAvailableJobs();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterMessageStartEvent");

    taskService.complete(task.getId());

    // assert process instance is ended
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testCallActivity-super.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testCallActivity-sub.bpmn20.xml"
  })
  @Test
  void testCallActivity() {
    runtimeService.startProcessInstanceByKey("super");

    ProcessInstance pi = runtimeService
        .createProcessInstanceQuery()
        .processDefinitionKey("sub")
        .singleResult();

    assertThat(pi).isInstanceOf(ExecutionEntity.class);

    assertThat(((ExecutionEntity) pi).getActivityId()).isEqualTo("theSubStart");

  }

  @Deployment
  @Test
  void testAsyncSubProcessStartEvent() {
    runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).as("The subprocess user task should not have been reached yet").isNull();

    assertThat(runtimeService.createExecutionQuery().activityId("StartEvent_2").count()).isOne();

    testRule.executeAvailableJobs();
    task = taskService.createTaskQuery().singleResult();

    assertThat(runtimeService.createExecutionQuery().activityId("StartEvent_2").count()).isZero();
    assertThat(task).as("The subprocess user task should have been reached").isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncSubProcessStartEvent.bpmn")
  @Test
  void testAsyncSubProcessStartEventActivityInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("SubProcess_1")
            .transition("StartEvent_2")
        .done());
  }

  @Deployment
  @Test
  void shouldRunAfterMessageStartInEventSubprocess() {
    // given
    // instance is waiting in async before on start event
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    Job job = managementService.createJobQuery().singleResult();

    // an event sub process is triggered before the job is executed
    runtimeService.createMessageCorrelation("start_sub")
      .processInstanceId(processInstanceId)
      .correlate();

    // when the job is executed
    managementService.executeJob(job.getId());

    // then
    // the user task after the async continuation is reached successfully
    Task task = taskService.createTaskQuery().singleResult();

    assertThat(runtimeService.createExecutionQuery().activityId("StartEvent_1").count()).isZero();
    assertThat(task).as("The user task should have been reached").isNotNull();

    // and the event sub process is still active
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);
    assertThat(executionTree)
      .matches(
        describeExecutionTree(null).scope()
        .child("user-task").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("external-task").scope()
      .done());

    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(activityInstanceTree).hasStructure(
        describeActivityInstanceTree(processDefinitionId)
          .activity("user-task")
          .beginScope("sub-process")
            .activity("external-task")
          .done());
  }
}
