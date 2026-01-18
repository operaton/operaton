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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
class ProcessInstanceSuspensionTest {
  private static final Map<String, String> emptyProperties = emptyMap();
  private static final Map<String, Object> emptyProcessVariables = emptyMap();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  FormService formService;
  ExternalTaskService externalTaskService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessInstanceActiveByDefault() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendActivateProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();

    //suspend
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isTrue();

    //activate
    runtimeService.activateProcessInstanceById(processInstance.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendActivateProcessInstanceByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();

    //suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isTrue();

    //activate
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendActivateProcessInstanceByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();

    //suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isTrue();

    //activate
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testActivateAlreadyActiveProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();

    try {
      //activate
      runtimeService.activateProcessInstanceById(processInstance.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(processInstance.isSuspended()).isFalse();
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      //activate
      runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(processInstance.isSuspended()).isFalse();
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      //activate
      runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(processInstance.isSuspended()).isFalse();
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendAlreadySuspendedProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();

    runtimeService.suspendProcessInstanceById(processInstance.getId());

    try {
      runtimeService.suspendProcessInstanceById(processInstance.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(processInstance.isSuspended()).isTrue();
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(processInstance.isSuspended()).isTrue();
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(processInstance.isSuspended()).isTrue();
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithMultipleNestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"
  })
  @Test
  void testQueryForActiveAndSuspendedProcessInstances() {
    runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();

    ProcessInstance piToSuspend = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("nestedSubProcessQueryTest")
            .singleResult();
    runtimeService.suspendProcessInstanceById(piToSuspend.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isOne();

    assertThat(runtimeService.createProcessInstanceQuery().suspended().singleResult().getId()).isEqualTo(piToSuspend.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithMultipleNestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"
  })
  @Test
  void testQueryForActiveAndSuspendedProcessInstancesByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("nestedSubProcessQueryTest")
        .singleResult();

    runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();

    ProcessInstance piToSuspend = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("nestedSubProcessQueryTest")
            .singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isOne();

    assertThat(runtimeService.createProcessInstanceQuery().suspended().singleResult().getId()).isEqualTo(piToSuspend.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithMultipleNestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"
  })
  @Test
  void testQueryForActiveAndSuspendedProcessInstancesByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("nestedSubProcessQueryTest")
        .singleResult();

    runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();

    ProcessInstance piToSuspend = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("nestedSubProcessQueryTest")
            .singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isOne();

    assertThat(runtimeService.createProcessInstanceQuery().suspended().singleResult().getId()).isEqualTo(piToSuspend.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskSuspendedAfterProcessInstanceSuspension() {

    // Start Process Instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspense process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // Assert that the task is now also suspended
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertThat(task.isSuspended()).isTrue();
    }

    // Activate process instance again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertThat(task.isSuspended()).isFalse();
    }
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testPreserveCreateTimeOnUpdatedTask() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    Task taskBeforeSuspension = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date createTime = taskBeforeSuspension.getCreateTime();

    // when
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // assume
    assertThat(task.isSuspended()).isTrue();

    // then
    assertThat(task.getCreateTime()).isEqualTo(createTime);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskSuspendedAfterProcessInstanceSuspensionByProcessDefinitionId() {

    // Start Process Instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspense process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    // Assert that the task is now also suspended
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertThat(task.isSuspended()).isTrue();
    }

    // Activate process instance again
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertThat(task.isSuspended()).isFalse();
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskSuspendedAfterProcessInstanceSuspensionByProcessDefinitionKey() {

    // Start Process Instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspense process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    // Assert that the task is now also suspended
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertThat(task.isSuspended()).isTrue();
    }

    // Activate process instance again
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertThat(task.isSuspended()).isFalse();
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskQueryAfterProcessInstanceSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    task = taskService.createTaskQuery().active().singleResult();
    assertThat(task).isNotNull();

    // Suspend
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().suspended().count()).isOne();
    assertThat(taskService.createTaskQuery().active().count()).isZero();

    // Activate
    runtimeService.activateProcessInstanceById(processInstance.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().suspended().count()).isZero();
    assertThat(taskService.createTaskQuery().active().count()).isOne();

    // Completing should end the process instance
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskQueryAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    task = taskService.createTaskQuery().active().singleResult();
    assertThat(task).isNotNull();

    // Suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().suspended().count()).isOne();
    assertThat(taskService.createTaskQuery().active().count()).isZero();

    // Activate
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().suspended().count()).isZero();
    assertThat(taskService.createTaskQuery().active().count()).isOne();

    // Completing should end the process instance
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskQueryAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    task = taskService.createTaskQuery().active().singleResult();
    assertThat(task).isNotNull();

    // Suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().suspended().count()).isOne();
    assertThat(taskService.createTaskQuery().active().count()).isZero();

    // Activate
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().suspended().count()).isZero();
    assertThat(taskService.createTaskQuery().active().count()).isOne();

    // Completing should end the process instance
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testChildExecutionsSuspendedAfterProcessInstanceSuspend() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testChildExecutionsSuspended");
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertThat(execution.isSuspended()).isTrue();
    }

    // Activate again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertThat(execution.isSuspended()).isFalse();
    }

    // Finish process
    while (taskService.createTaskQuery().count() > 0) {
      for (Task task : taskService.createTaskQuery().list()) {
        taskService.complete(task.getId());
      }
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testChildExecutionsSuspendedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  void testChildExecutionsSuspendedAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testChildExecutionsSuspended");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());

    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertThat(execution.isSuspended()).isTrue();
    }

    // Activate again
    runtimeService.activateProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());
    executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertThat(execution.isSuspended()).isFalse();
    }

    // Finish process
    while (taskService.createTaskQuery().count() > 0) {
      for (Task task : taskService.createTaskQuery().list()) {
        taskService.complete(task.getId());
      }
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testChildExecutionsSuspendedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  void testChildExecutionsSuspendedAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testChildExecutionsSuspended");
    runtimeService.suspendProcessInstanceByProcessDefinitionKey("testChildExecutionsSuspended");

    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertThat(execution.isSuspended()).isTrue();
    }

    // Activate again
    runtimeService.activateProcessInstanceByProcessDefinitionKey("testChildExecutionsSuspended");
    executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertThat(execution.isSuspended()).isFalse();
    }

    // Finish process
    while (taskService.createTaskQuery().count() > 0) {
      for (Task task : taskService.createTaskQuery().list()) {
        taskService.complete(task.getId());
      }
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testChangeVariablesAfterProcessInstanceSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    try {
      runtimeService.removeVariable(processInstance.getId(), "someVariable");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariableLocal(processInstance.getId(), "someVariable");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariables(processInstance.getId(), List.of("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }


    try {
      runtimeService.removeVariablesLocal(processInstance.getId(), List.of("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariable(processInstance.getId(), "someVariable", "someValue");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariableLocal(processInstance.getId(), "someVariable", "someValue");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariables(processInstance.getId(), new HashMap<>());
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariablesLocal(processInstance.getId(), new HashMap<>());
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testChangeVariablesAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());

    try {
      runtimeService.removeVariable(processInstance.getId(), "someVariable");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariableLocal(processInstance.getId(), "someVariable");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariables(processInstance.getId(), List.of("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }


    try {
      runtimeService.removeVariablesLocal(processInstance.getId(), List.of("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariable(processInstance.getId(), "someVariable", "someValue");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariableLocal(processInstance.getId(), "someVariable", "someValue");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariables(processInstance.getId(), new HashMap<>());
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariablesLocal(processInstance.getId(), new HashMap<>());
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testChangeVariablesAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    try {
      runtimeService.removeVariable(processInstance.getId(), "someVariable");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariableLocal(processInstance.getId(), "someVariable");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariables(processInstance.getId(), List.of("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariablesLocal(processInstance.getId(), List.of("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariable(processInstance.getId(), "someVariable", "someValue");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariableLocal(processInstance.getId(), "someVariable", "someValue");
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariables(processInstance.getId(), new HashMap<>());
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.setVariablesLocal(processInstance.getId(), new HashMap<>());
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSubmitTaskFormFailAfterProcessInstanceSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    var taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();

    assertThatThrownBy(() -> formService.submitTaskFormData(taskQuery, emptyProperties)).isInstanceOf(SuspendedEntityInteractionException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSubmitTaskFormFailAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    var taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();

    assertThatThrownBy(() -> formService.submitTaskFormData(taskQuery, emptyProperties)).isInstanceOf(SuspendedEntityInteractionException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSubmitTaskFormFailAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    var taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();

    assertThatThrownBy(() -> formService.submitTaskFormData(taskQuery, emptyProperties)).isInstanceOf(SuspendedEntityInteractionException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessInstanceSignalFailAfterSuspend() {

    // Suspend process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    var processInstanceId = processInstance.getId();
    runtimeService.suspendProcessInstanceById(processInstanceId);

    try {
      runtimeService.signal(processInstanceId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signal(processInstanceId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessInstanceSignalFailAfterSuspendByProcessDefinitionId() {

    // Suspend process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    String processInstanceId = processInstance.getId();

    try {
      runtimeService.signal(processInstanceId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signal(processInstanceId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessInstanceSignalFailAfterSuspendByProcessDefinitionKey() {

    // Suspend process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    var processInstanceId = processInstance.getId();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    try {
      runtimeService.signal(processInstanceId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signal(processInstanceId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment
  @Test
  void testMessageEventReceiveFailAfterSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();

    try {
      runtimeService.messageEventReceived("someMessage", executionId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.messageEventReceived("someMessage", executionId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testMessageEventReceiveFailAfterSuspend.bpmn20.xml"})
  @Test
  void testMessageEventReceiveFailAfterSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();

    try {
      runtimeService.messageEventReceived("someMessage", executionId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.messageEventReceived("someMessage", executionId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testMessageEventReceiveFailAfterSuspend.bpmn20.xml"})
  @Test
  void testMessageEventReceiveFailAfterSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();

    try {
      runtimeService.messageEventReceived("someMessage", executionId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.messageEventReceived("someMessage", executionId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment
  @Test
  void testSignalEventReceivedAfterProcessInstanceSuspended() {

    final String signal = "Some Signal";

    // Test if process instance can be completed using the signal
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    assertThat(processInstance).isNotNull();
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // Now test when suspending the process instance: the process instance shouldn't be continued
    processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    runtimeService.signalEventReceived(signal, new HashMap<>());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();
    try {
      runtimeService.signalEventReceived(signal, executionId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signalEventReceived(signal, executionId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    // Activate and try again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testSignalEventReceivedAfterProcessInstanceSuspended.bpmn20.xml"})
  @Test
  void testSignalEventReceivedAfterProcessInstanceSuspendedByProcessDefinitionId() {

    final String signal = "Some Signal";

    // Test if process instance can be completed using the signal
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    assertThat(processInstance).isNotNull();
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // Now test when suspending the process instance: the process instance shouldn't be continued
    processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    runtimeService.signalEventReceived(signal, new HashMap<>());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();
    try {
      runtimeService.signalEventReceived(signal, executionId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signalEventReceived(signal, executionId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    // Activate and try again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testSignalEventReceivedAfterProcessInstanceSuspended.bpmn20.xml"})
  @Test
  void testSignalEventReceivedAfterProcessInstanceSuspendedByProcessDefinitionKey() {

    final String signal = "Some Signal";

    // Test if process instance can be completed using the signal
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    assertThat(processInstance).isNotNull();
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // Now test when suspending the process instance: the process instance shouldn't be continued
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("signalSuspendedProcessInstance")
        .singleResult();

    processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    runtimeService.signalEventReceived(signal, new HashMap<>());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();
    try {
      runtimeService.signalEventReceived(signal, executionId);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signalEventReceived(signal, executionId, emptyProcessVariables);
      fail("");
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    // Activate and try again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskLifecycleOperationsFailAfterProcessInstanceSuspend() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    var taskId = task.getId();

    // Suspend the process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // Completing the task should fail
    assertThatThrownBy(() -> taskService.complete(taskId)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Claiming the task should fail
    assertThatThrownBy(() -> taskService.claim(taskId, "jos")).isInstanceOf(SuspendedEntityInteractionException.class);


    // Adding candidate groups on the task should fail
    assertThatThrownBy(() -> taskService.addCandidateGroup(taskId, "blahGroup")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding candidate users on the task should fail
    assertThatThrownBy(() -> taskService.addCandidateUser(taskId, "blahUser")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding group identity links on the task should fail
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding an identity link on the task should fail
    assertThatThrownBy(() -> taskService.addUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER)).isInstanceOf(SuspendedEntityInteractionException.class);


    // Set an assignee on the task should fail
    assertThatThrownBy(() -> taskService.setAssignee(taskId, "mispiggy")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Set an owner on the task should fail
    assertThatThrownBy(() -> taskService.setOwner(taskId, "kermit")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing candidate groups on the task should fail
    assertThatThrownBy(() -> taskService.deleteCandidateGroup(taskId, "blahGroup")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing candidate users on the task should fail
    assertThatThrownBy(() -> taskService.deleteCandidateUser(taskId, "blahUser")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing group identity links on the task should fail
    assertThatThrownBy(() -> taskService.deleteGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing an identity link on the task should fail
    assertThatThrownBy(() -> taskService.deleteUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER)).isInstanceOf(SuspendedEntityInteractionException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskLifecycleOperationsFailAfterProcessInstanceSuspendByProcessDefinitionId() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    var taskId = task.getId();

    // Suspend the process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    // Completing the task should fail
    assertThatThrownBy(() -> taskService.complete(taskId)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Claiming the task should fail
    assertThatThrownBy(() -> taskService.claim(taskId, "jos")).isInstanceOf(SuspendedEntityInteractionException.class);


    // Adding candidate groups on the task should fail
    assertThatThrownBy(() -> taskService.addCandidateGroup(taskId, "blahGroup")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding candidate users on the task should fail
    assertThatThrownBy(() -> taskService.addCandidateUser(taskId, "blahUser")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding group identity links on the task should fail
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding an identity link on the task should fail
    assertThatThrownBy(() -> taskService.addUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER)).isInstanceOf(SuspendedEntityInteractionException.class);


    // Set an assignee on the task should fail
    assertThatThrownBy(() -> taskService.setAssignee(taskId, "mispiggy")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Set an owner on the task should fail
    assertThatThrownBy(() -> taskService.setOwner(taskId, "kermit")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing candidate groups on the task should fail
    assertThatThrownBy(() -> taskService.deleteCandidateGroup(taskId, "blahGroup")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing candidate users on the task should fail
    assertThatThrownBy(() -> taskService.deleteCandidateUser(taskId, "blahUser")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing group identity links on the task should fail
    assertThatThrownBy(() -> taskService.deleteGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing an identity link on the task should fail
    assertThatThrownBy(() -> taskService.deleteUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER)).isInstanceOf(SuspendedEntityInteractionException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskLifecycleOperationsFailAfterProcessInstanceSuspendByProcessDefinitionKey() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    var taskId = task.getId();

    // Suspend the process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    // Completing the task should fail
    assertThatThrownBy(() -> taskService.complete(taskId)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Claiming the task should fail
    assertThatThrownBy(() -> taskService.claim(taskId, "jos")).isInstanceOf(SuspendedEntityInteractionException.class);


    // Adding candidate groups on the task should fail
    assertThatThrownBy(() -> taskService.addCandidateGroup(taskId, "blahGroup")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding candidate users on the task should fail
    assertThatThrownBy(() -> taskService.addCandidateUser(taskId, "blahUser")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding group identity links on the task should fail
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Adding an identity link on the task should fail
    assertThatThrownBy(() -> taskService.addUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER)).isInstanceOf(SuspendedEntityInteractionException.class);


    // Set an assignee on the task should fail
    assertThatThrownBy(() -> taskService.setAssignee(taskId, "mispiggy")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Set an owner on the task should fail
    assertThatThrownBy(() -> taskService.setOwner(taskId, "kermit")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing candidate groups on the task should fail
    assertThatThrownBy(() -> taskService.deleteCandidateGroup(taskId, "blahGroup")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing candidate users on the task should fail
    assertThatThrownBy(() -> taskService.deleteCandidateUser(taskId, "blahUser")).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing group identity links on the task should fail
    assertThatThrownBy(() -> taskService.deleteGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE)).isInstanceOf(SuspendedEntityInteractionException.class);

    // Removing an identity link on the task should fail
    assertThatThrownBy(() -> taskService.deleteUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER)).isInstanceOf(SuspendedEntityInteractionException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSubTaskCreationFailAfterProcessInstanceSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    Task subTask = taskService.newTask("someTaskId");
    subTask.setParentTaskId(task.getId());

    assertThatThrownBy(() -> taskService.saveTask(subTask)).isInstanceOf(SuspendedEntityInteractionException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSubTaskCreationFailAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    Task subTask = taskService.newTask("someTaskId");
    subTask.setParentTaskId(task.getId());

    assertThatThrownBy(() -> taskService.saveTask(subTask)).isInstanceOf(SuspendedEntityInteractionException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSubTaskCreationFailAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    Task subTask = taskService.newTask("someTaskId");
    subTask.setParentTaskId(task.getId());

    assertThatThrownBy(() -> taskService.saveTask(subTask)).isInstanceOf(SuspendedEntityInteractionException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskNonLifecycleOperationsSucceedAfterProcessInstanceSuspend() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    assertThat(task).isNotNull();

    try {
      taskService.setVariable(task.getId(), "someVar", "someValue");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.setVariableLocal(task.getId(), "someVar", "someValue");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      HashMap<String, String> variables = new HashMap<>();
      variables.put("varOne", "one");
      variables.put("varTwo", "two");
      taskService.setVariables(task.getId(), variables);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      HashMap<String, String> variables = new HashMap<>();
      variables.put("varOne", "one");
      variables.put("varTwo", "two");
      taskService.setVariablesLocal(task.getId(), variables);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariable(task.getId(), "someVar");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariableLocal(task.getId(), "someVar");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariables(task.getId(), List.of("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariablesLocal(task.getId(), List.of("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    if(processEngineConfiguration.getHistoryLevel().getId() > HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {

      try {
        taskService.createComment(task.getId(), processInstance.getId(), "test comment");
      } catch (SuspendedEntityInteractionException e) {
        fail("should be allowed");
      }

      try {
        taskService.createAttachment("text", task.getId(), processInstance.getId(), "tesTastName", "testDescription", "http://test.com");
      } catch (SuspendedEntityInteractionException e) {
        fail("should be allowed");
      }

    }


    try {
      taskService.setPriority(task.getId(), 99);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskNonLifecycleOperationsSucceedAfterProcessInstanceSuspendByProcessDefinitionId() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());
    assertThat(task).isNotNull();

    try {
      taskService.setVariable(task.getId(), "someVar", "someValue");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.setVariableLocal(task.getId(), "someVar", "someValue");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      HashMap<String, String> variables = new HashMap<>();
      variables.put("varOne", "one");
      variables.put("varTwo", "two");
      taskService.setVariables(task.getId(), variables);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      HashMap<String, String> variables = new HashMap<>();
      variables.put("varOne", "one");
      variables.put("varTwo", "two");
      taskService.setVariablesLocal(task.getId(), variables);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariable(task.getId(), "someVar");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariableLocal(task.getId(), "someVar");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariables(task.getId(), List.of("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariablesLocal(task.getId(), List.of("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    if(processEngineConfiguration.getHistoryLevel().getId() > HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {

      try {
        taskService.createComment(task.getId(), processInstance.getId(), "test comment");
      } catch (SuspendedEntityInteractionException e) {
        fail("should be allowed");
      }

      try {
        taskService.createAttachment("text", task.getId(), processInstance.getId(), "tesTastName", "testDescription", "http://test.com");
      } catch (SuspendedEntityInteractionException e) {
        fail("should be allowed");
      }

    }


    try {
      taskService.setPriority(task.getId(), 99);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskNonLifecycleOperationsSucceedAfterProcessInstanceSuspendByProcessDefinitionKey() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(task).isNotNull();

    try {
      taskService.setVariable(task.getId(), "someVar", "someValue");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.setVariableLocal(task.getId(), "someVar", "someValue");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      HashMap<String, String> variables = new HashMap<>();
      variables.put("varOne", "one");
      variables.put("varTwo", "two");
      taskService.setVariables(task.getId(), variables);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      HashMap<String, String> variables = new HashMap<>();
      variables.put("varOne", "one");
      variables.put("varTwo", "two");
      taskService.setVariablesLocal(task.getId(), variables);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariable(task.getId(), "someVar");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariableLocal(task.getId(), "someVar");
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariables(task.getId(), List.of("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariablesLocal(task.getId(), List.of("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    if(processEngineConfiguration.getHistoryLevel().getId() > HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {

      try {
        taskService.createComment(task.getId(), processInstance.getId(), "test comment");
      } catch (SuspendedEntityInteractionException e) {
        fail("should be allowed");
      }

      try {
        taskService.createAttachment("text", task.getId(), processInstance.getId(), "tesTastName", "testDescription", "http://test.com");
      } catch (SuspendedEntityInteractionException e) {
        fail("should be allowed");
      }

    }


    try {
      taskService.setPriority(task.getId(), 99);
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }
  }

  @Deployment
  @Test
  void testJobNotExecutedAfterProcessInstanceSuspend() {

    Date now = new Date();
    ClockUtil.setCurrentTime(now);

    // Suspending the process instance should also stop the execution of jobs for that process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isOne();
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    assertThat(managementService.createJobQuery().count()).isOne();

    // The jobs should not be executed now
    ClockUtil.setCurrentTime(new Date(now.getTime() + (60 * 60 * 1000))); // Timer is set to fire on 5 minutes
    assertThat(managementService.createJobQuery().executable().count()).isZero();

    // Activation of the process instance should now allow for job execution
    runtimeService.activateProcessInstanceById(processInstance.getId());
    assertThat(managementService.createJobQuery().executable().count()).isOne();
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testJobNotExecutedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  void testJobNotExecutedAfterProcessInstanceSuspendByProcessDefinitionId() {

    Date now = new Date();
    ClockUtil.setCurrentTime(now);

    // Suspending the process instance should also stop the execution of jobs for that process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isOne();
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isOne();

    // The jobs should not be executed now
    ClockUtil.setCurrentTime(new Date(now.getTime() + (60 * 60 * 1000))); // Timer is set to fire on 5 minutes
    assertThat(managementService.createJobQuery().executable().count()).isZero();

    // Activation of the process instance should now allow for job execution
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(managementService.createJobQuery().executable().count()).isOne();
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testJobNotExecutedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  void testJobNotExecutedAfterProcessInstanceSuspendByProcessDefinitionKey() {

    Date now = new Date();
    ClockUtil.setCurrentTime(now);

    // Suspending the process instance should also stop the execution of jobs for that process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isOne();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(managementService.createJobQuery().count()).isOne();

    // The jobs should not be executed now
    ClockUtil.setCurrentTime(new Date(now.getTime() + (60 * 60 * 1000))); // Timer is set to fire on 5 minutes
    assertThat(managementService.createJobQuery().executable().count()).isZero();

    // Activation of the process instance should now allow for job execution
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(managementService.createJobQuery().executable().count()).isOne();
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callSimpleProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  void testCallActivityReturnAfterProcessInstanceSuspend() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callSimpleProcess");
    runtimeService.suspendProcessInstanceById(instance.getId());

    Task task = taskService.createTaskQuery().singleResult();
    var taskId = task.getId();

    assertThatThrownBy(() -> taskService.complete(taskId)).isInstanceOf(SuspendedEntityInteractionException.class);

    // should be successful after reactivation
    runtimeService.activateProcessInstanceById(instance.getId());
    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callSimpleProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  void testCallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callSimpleProcess");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());

    Task task = taskService.createTaskQuery().singleResult();
    var taskId = task.getId();

    assertThatThrownBy(() -> taskService.complete(taskId)).isInstanceOf(SuspendedEntityInteractionException.class);

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());
    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callSimpleProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  void testCallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("callSimpleProcess")
        .singleResult();

    runtimeService.startProcessInstanceByKey("callSimpleProcess");
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    Task task = taskService.createTaskQuery().singleResult();
    var taskId = task.getId();

    assertThatThrownBy(() -> taskService.complete(taskId)).isInstanceOf(SuspendedEntityInteractionException.class);

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callMISimpleProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  void testMICallActivityReturnAfterProcessInstanceSuspend() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callMISimpleProcess");
    runtimeService.suspendProcessInstanceById(instance.getId());

    List<Task> tasks = taskService.createTaskQuery().list();
    Task task1 = tasks.get(0);
    Task task2 = tasks.get(1);
    String task1Id = task1.getId();
    String task2Id = task2.getId();

    assertThatThrownBy(() -> taskService.complete(task1Id)).isInstanceOf(SuspendedEntityInteractionException.class);

    assertThatThrownBy(() -> taskService.complete(task2Id)).isInstanceOf(SuspendedEntityInteractionException.class);

    // should be successful after reactivation
    runtimeService.activateProcessInstanceById(instance.getId());
    taskService.complete(task1Id);
    taskService.complete(task2Id);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callMISimpleProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  void testMICallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callMISimpleProcess");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());

    List<Task> tasks = taskService.createTaskQuery().list();
    Task task1 = tasks.get(0);
    Task task2 = tasks.get(1);
    String task1Id = task1.getId();
    String task2Id = task2.getId();

    assertThatThrownBy(() -> taskService.complete(task1Id)).isInstanceOf(SuspendedEntityInteractionException.class);

    assertThatThrownBy(() -> taskService.complete(task2Id)).isInstanceOf(SuspendedEntityInteractionException.class);

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());
    taskService.complete(task1Id);
    taskService.complete(task2Id);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callMISimpleProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  void testMICallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("callMISimpleProcess")
        .singleResult();
    runtimeService.startProcessInstanceByKey("callMISimpleProcess");
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    List<Task> tasks = taskService.createTaskQuery().list();
    Task task1 = tasks.get(0);
    Task task2 = tasks.get(1);
    String task1Id = task1.getId();
    String task2Id = task2.getId();

    assertThatThrownBy(() -> taskService.complete(task1Id)).isInstanceOf(SuspendedEntityInteractionException.class);

    assertThatThrownBy(() -> taskService.complete(task2Id)).isInstanceOf(SuspendedEntityInteractionException.class);

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    taskService.complete(task1Id);
    taskService.complete(task2Id);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testStartBeforeActivityForSuspendProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    //start process instance
    runtimeService.startProcessInstanceById(processDefinition.getId());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspend process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("theTask");

    // try to start before activity for suspended processDefinition
    try {
      processInstanceModificationBuilder.execute();
      fail("Exception is expected but not thrown");
    } catch(SuspendedEntityInteractionException e) {
      testRule.assertTextPresentIgnoreCase("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testStartAfterActivityForSuspendProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    //start process instance
    runtimeService.startProcessInstanceById(processDefinition.getId());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspend process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("theTask");

    // try to start after activity for suspended processDefinition
    try {
      processInstanceModificationBuilder.execute();
      fail("Exception is expected but not thrown");
    } catch(SuspendedEntityInteractionException e) {
      testRule.assertTextPresentIgnoreCase("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @Test
  void testSuspensionByIdCascadesToExternalTasks() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    ExternalTask task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isFalse();

    // when the process instance is suspended
    runtimeService.suspendProcessInstanceById(processInstance1.getId());

    // then the task is suspended
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isTrue();

    // the other task is not
    ExternalTask task2 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance2.getId()).singleResult();
    assertThat(task2.isSuspended()).isFalse();

    // when it is activated again
    runtimeService.activateProcessInstanceById(processInstance1.getId());

    // then the task is activated too
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @Test
  void testSuspensionByProcessDefinitionIdCascadesToExternalTasks() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    ExternalTask task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isFalse();

    // when the process instance is suspended
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance1.getProcessDefinitionId());

    // then the task is suspended
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isTrue();

    // the other task is not
    ExternalTask task2 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance2.getId()).singleResult();
    assertThat(task2.isSuspended()).isFalse();

    // when it is activated again
    runtimeService.activateProcessInstanceByProcessDefinitionId(processInstance1.getProcessDefinitionId());

    // then the task is activated too
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @Test
  void testSuspensionByProcessDefinitionKeyCascadesToExternalTasks() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    ExternalTask task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isFalse();

    // when the process instance is suspended
    runtimeService.suspendProcessInstanceByProcessDefinitionKey("oneExternalTaskProcess");

    // then the task is suspended
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isTrue();

    // the other task is not
    ExternalTask task2 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance2.getId()).singleResult();
    assertThat(task2.isSuspended()).isFalse();

    // when it is activated again
    runtimeService.activateProcessInstanceByProcessDefinitionKey("oneExternalTaskProcess");

    // then the task is activated too
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertThat(task1.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendAndActivateProcessInstanceByIdUsingBuilder() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();

    //suspend
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessInstanceId(processInstance.getId())
      .suspend();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isOne();

    //activate
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessInstanceId(processInstance.getId())
      .activate();

    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendAndActivateProcessInstanceByProcessDefinitionIdUsingBuilder() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();

    //suspend
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isOne();

    //activate
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .activate();

    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendAndActivateProcessInstanceByProcessDefinitionKeyUsingBuilder() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();

    //suspend
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey("oneTaskProcess")
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isOne();

    //activate
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey("oneTaskProcess")
      .activate();

    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();
  }

  @Deployment
  @Test
  void testJobSuspensionStateUpdate() {

    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    String id = instance.getProcessInstanceId();

    //when
    runtimeService.suspendProcessInstanceById(id);
    Job job = managementService.createJobQuery().processInstanceId(id).singleResult();

    // then
    assertThat(job.isSuspended()).isTrue();
  }

}
