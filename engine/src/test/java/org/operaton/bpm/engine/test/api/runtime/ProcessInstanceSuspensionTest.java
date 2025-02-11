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
package org.operaton.bpm.engine.test.api.runtime;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.*;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
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
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class ProcessInstanceSuspensionTest extends PluggableProcessEngineTest {
  private static final Map<String, String> emptyProperties = emptyMap();
  private static final Map<String, Object> emptyProcessVariables = emptyMap();

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testProcessInstanceActiveByDefault() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspendActivateProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());

    //suspend
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertTrue(processInstance.isSuspended());

    //activate
    runtimeService.activateProcessInstanceById(processInstance.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspendActivateProcessInstanceByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());

    //suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertTrue(processInstance.isSuspended());

    //activate
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspendActivateProcessInstanceByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());

    //suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertTrue(processInstance.isSuspended());

    //activate
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testActivateAlreadyActiveProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());

    try {
      //activate
      runtimeService.activateProcessInstanceById(processInstance.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertFalse(processInstance.isSuspended());
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      //activate
      runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertFalse(processInstance.isSuspended());
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      //activate
      runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertFalse(processInstance.isSuspended());
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspendAlreadySuspendedProcessInstance() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());

    runtimeService.suspendProcessInstanceById(processInstance.getId());

    try {
      runtimeService.suspendProcessInstanceById(processInstance.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertTrue(processInstance.isSuspended());
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertTrue(processInstance.isSuspended());
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }

    try {
      runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
      processInstance = runtimeService.createProcessInstanceQuery().singleResult();
      assertTrue(processInstance.isSuspended());
    } catch (ProcessEngineException e) {
      fail("Should not fail");
    }
  }

  @Deployment(resources={
          "org/operaton/bpm/engine/test/api/runtime/superProcessWithMultipleNestedSubProcess.bpmn20.xml",
          "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
          "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"
          })
  @Test
  public void testQueryForActiveAndSuspendedProcessInstances() {
    runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(0);

    ProcessInstance piToSuspend = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("nestedSubProcessQueryTest")
            .singleResult();
    runtimeService.suspendProcessInstanceById(piToSuspend.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(1);

    assertThat(runtimeService.createProcessInstanceQuery().suspended().singleResult().getId()).isEqualTo(piToSuspend.getId());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithMultipleNestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"
      })
  @Test
  public void testQueryForActiveAndSuspendedProcessInstancesByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("nestedSubProcessQueryTest")
        .singleResult();

    runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(0);

    ProcessInstance piToSuspend = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("nestedSubProcessQueryTest")
            .singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(1);

    assertThat(runtimeService.createProcessInstanceQuery().suspended().singleResult().getId()).isEqualTo(piToSuspend.getId());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithMultipleNestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"
      })
  @Test
  public void testQueryForActiveAndSuspendedProcessInstancesByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("nestedSubProcessQueryTest")
        .singleResult();

    runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(0);

    ProcessInstance piToSuspend = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("nestedSubProcessQueryTest")
            .singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(1);

    assertThat(runtimeService.createProcessInstanceQuery().suspended().singleResult().getId()).isEqualTo(piToSuspend.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskSuspendedAfterProcessInstanceSuspension() {

    // Start Process Instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspense process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // Assert that the task is now also suspended
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertTrue(task.isSuspended());
    }

    // Activate process instance again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertFalse(task.isSuspended());
    }
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testPreserveCreateTimeOnUpdatedTask() {
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
    assertTrue(task.isSuspended());

    // then
    assertThat(task.getCreateTime()).isEqualTo(createTime);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskSuspendedAfterProcessInstanceSuspensionByProcessDefinitionId() {

    // Start Process Instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspense process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    // Assert that the task is now also suspended
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertTrue(task.isSuspended());
    }

    // Activate process instance again
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertFalse(task.isSuspended());
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskSuspendedAfterProcessInstanceSuspensionByProcessDefinitionKey() {

    // Start Process Instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspense process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    // Assert that the task is now also suspended
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertTrue(task.isSuspended());
    }

    // Activate process instance again
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    for (Task task : tasks) {
      assertFalse(task.isSuspended());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskQueryAfterProcessInstanceSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);

    task = taskService.createTaskQuery().active().singleResult();
    assertNotNull(task);

    // Suspend
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(0);

    // Activate
    runtimeService.activateProcessInstanceById(processInstance.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(0);
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(1);

    // Completing should end the process instance
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskQueryAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);

    task = taskService.createTaskQuery().active().singleResult();
    assertNotNull(task);

    // Suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(0);

    // Activate
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(0);
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(1);

    // Completing should end the process instance
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskQueryAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);

    task = taskService.createTaskQuery().active().singleResult();
    assertNotNull(task);

    // Suspend
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(0);

    // Activate
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(0);
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(1);

    // Completing should end the process instance
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testChildExecutionsSuspendedAfterProcessInstanceSuspend() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testChildExecutionsSuspended");
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertTrue(execution.isSuspended());
    }

    // Activate again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertFalse(execution.isSuspended());
    }

    // Finish process
    while (taskService.createTaskQuery().count() > 0) {
      for (Task task : taskService.createTaskQuery().list()) {
        taskService.complete(task.getId());
      }
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testChildExecutionsSuspendedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  public void testChildExecutionsSuspendedAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testChildExecutionsSuspended");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());

    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertTrue(execution.isSuspended());
    }

    // Activate again
    runtimeService.activateProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());
    executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertFalse(execution.isSuspended());
    }

    // Finish process
    while (taskService.createTaskQuery().count() > 0) {
      for (Task task : taskService.createTaskQuery().list()) {
        taskService.complete(task.getId());
      }
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testChildExecutionsSuspendedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  public void testChildExecutionsSuspendedAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testChildExecutionsSuspended");
    runtimeService.suspendProcessInstanceByProcessDefinitionKey("testChildExecutionsSuspended");

    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertTrue(execution.isSuspended());
    }

    // Activate again
    runtimeService.activateProcessInstanceByProcessDefinitionKey("testChildExecutionsSuspended");
    executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution execution : executions) {
      assertFalse(execution.isSuspended());
    }

    // Finish process
    while (taskService.createTaskQuery().count() > 0) {
      for (Task task : taskService.createTaskQuery().list()) {
        taskService.complete(task.getId());
      }
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testChangeVariablesAfterProcessInstanceSuspend() {
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
      runtimeService.removeVariables(processInstance.getId(), Arrays.asList("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }


    try {
      runtimeService.removeVariablesLocal(processInstance.getId(), Arrays.asList("one", "two", "three"));
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
  public void testChangeVariablesAfterProcessInstanceSuspendByProcessDefinitionId() {
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
      runtimeService.removeVariables(processInstance.getId(), Arrays.asList("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }


    try {
      runtimeService.removeVariablesLocal(processInstance.getId(), Arrays.asList("one", "two", "three"));
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
  public void testChangeVariablesAfterProcessInstanceSuspendByProcessDefinitionKey() {
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
      runtimeService.removeVariables(processInstance.getId(), Arrays.asList("one", "two", "three"));
    } catch (ProcessEngineException e) {
      fail("This should be possible");
    }

    try {
      runtimeService.removeVariablesLocal(processInstance.getId(), Arrays.asList("one", "two", "three"));
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
  public void testSubmitTaskFormFailAfterProcessInstanceSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    var taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();

    try {
      formService.submitTaskFormData(taskQuery, emptyProperties);
      fail();
    } catch(SuspendedEntityInteractionException e) {
      // This is expected
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSubmitTaskFormFailAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    var taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();

    try {
      formService.submitTaskFormData(taskQuery, emptyProperties);
      fail();
    } catch(SuspendedEntityInteractionException e) {
      // This is expected
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSubmitTaskFormFailAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    var taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();

    try {
      formService.submitTaskFormData(taskQuery, emptyProperties);
      fail();
    } catch(SuspendedEntityInteractionException e) {
      // This is expected
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testProcessInstanceSignalFailAfterSuspend() {

    // Suspend process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    var processInstanceId = processInstance.getId();
    runtimeService.suspendProcessInstanceById(processInstanceId);

    try {
      runtimeService.signal(processInstanceId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signal(processInstanceId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testProcessInstanceSignalFailAfterSuspendByProcessDefinitionId() {

    // Suspend process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    String processInstanceId = processInstance.getId();

    try {
      runtimeService.signal(processInstanceId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signal(processInstanceId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testProcessInstanceSignalFailAfterSuspendByProcessDefinitionKey() {

    // Suspend process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    var processInstanceId = processInstance.getId();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    try {
      runtimeService.signal(processInstanceId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signal(processInstanceId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment
  @Test
  public void testMessageEventReceiveFailAfterSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();

    try {
      runtimeService.messageEventReceived("someMessage", executionId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.messageEventReceived("someMessage", executionId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testMessageEventReceiveFailAfterSuspend.bpmn20.xml"})
  @Test
  public void testMessageEventReceiveFailAfterSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();

    try {
      runtimeService.messageEventReceived("someMessage", executionId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.messageEventReceived("someMessage", executionId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testMessageEventReceiveFailAfterSuspend.bpmn20.xml"})
  @Test
  public void testMessageEventReceiveFailAfterSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();

    try {
      runtimeService.messageEventReceived("someMessage", executionId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.messageEventReceived("someMessage", executionId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }
  }

  @Deployment
  @Test
  public void testSignalEventReceivedAfterProcessInstanceSuspended() {

    final String signal = "Some Signal";

    // Test if process instance can be completed using the signal
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // Now test when suspending the process instance: the process instance shouldn't be continued
    processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    runtimeService.signalEventReceived(signal, new HashMap<>());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();
    try {
      runtimeService.signalEventReceived(signal, executionId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signalEventReceived(signal, executionId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    // Activate and try again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testSignalEventReceivedAfterProcessInstanceSuspended.bpmn20.xml"})
  @Test
  public void testSignalEventReceivedAfterProcessInstanceSuspendedByProcessDefinitionId() {

    final String signal = "Some Signal";

    // Test if process instance can be completed using the signal
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // Now test when suspending the process instance: the process instance shouldn't be continued
    processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    runtimeService.signalEventReceived(signal, new HashMap<>());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();
    try {
      runtimeService.signalEventReceived(signal, executionId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signalEventReceived(signal, executionId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    // Activate and try again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testSignalEventReceivedAfterProcessInstanceSuspended.bpmn20.xml"})
  @Test
  public void testSignalEventReceivedAfterProcessInstanceSuspendedByProcessDefinitionKey() {

    final String signal = "Some Signal";

    // Test if process instance can be completed using the signal
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // Now test when suspending the process instance: the process instance shouldn't be continued
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("signalSuspendedProcessInstance")
        .singleResult();

    processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    runtimeService.signalEventReceived(signal, new HashMap<>());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    var executionId = subscription.getExecutionId();
    try {
      runtimeService.signalEventReceived(signal, executionId);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    try {
      runtimeService.signalEventReceived(signal, executionId, emptyProcessVariables);
      fail();
    } catch (SuspendedEntityInteractionException e) {
      // This is expected
      testRule.assertTextPresent("is suspended", e.getMessage());
    }

    // Activate and try again
    runtimeService.activateProcessInstanceById(processInstance.getId());
    runtimeService.signalEventReceived(signal);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskLifecycleOperationsFailAfterProcessInstanceSuspend() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertNotNull(task);
    var taskId = task.getId();

    // Suspend the process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // Completing the task should fail
    try {
      taskService.complete(taskId);
      fail("It is not allowed to complete a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Claiming the task should fail
    try {
      taskService.claim(taskId, "jos");
      fail("It is not allowed to claim a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }



    // Adding candidate groups on the task should fail
    try {
      taskService.addCandidateGroup(taskId, "blahGroup");
      fail("It is not allowed to add a candidate group on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding candidate users on the task should fail
    try {
      taskService.addCandidateUser(taskId, "blahUser");
      fail("It is not allowed to add a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding group identity links on the task should fail
    try {
      taskService.addGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE);
      fail("It is not allowed to add a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding an identity link on the task should fail
    try {
      taskService.addUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER);
      fail("It is not allowed to add an identityLink on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }


    // Set an assignee on the task should fail
    try {
      taskService.setAssignee(taskId, "mispiggy");
      fail("It is not allowed to set an assignee on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Set an owner on the task should fail
    try {
      taskService.setOwner(taskId, "kermit");
      fail("It is not allowed to set an owner on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing candidate groups on the task should fail
    try {
      taskService.deleteCandidateGroup(taskId, "blahGroup");
      fail("It is not allowed to remove a candidate group on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing candidate users on the task should fail
    try {
      taskService.deleteCandidateUser(taskId, "blahUser");
      fail("It is not allowed to remove a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing group identity links on the task should fail
    try {
      taskService.deleteGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE);
      fail("It is not allowed to remove a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing an identity link on the task should fail
    try {
      taskService.deleteUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER);
      fail("It is not allowed to remove an identityLink on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskLifecycleOperationsFailAfterProcessInstanceSuspendByProcessDefinitionId() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertNotNull(task);
    var taskId = task.getId();

    // Suspend the process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    // Completing the task should fail
    try {
      taskService.complete(taskId);
      fail("It is not allowed to complete a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Claiming the task should fail
    try {
      taskService.claim(taskId, "jos");
      fail("It is not allowed to claim a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }



    // Adding candidate groups on the task should fail
    try {
      taskService.addCandidateGroup(taskId, "blahGroup");
      fail("It is not allowed to add a candidate group on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding candidate users on the task should fail
    try {
      taskService.addCandidateUser(taskId, "blahUser");
      fail("It is not allowed to add a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding group identity links on the task should fail
    try {
      taskService.addGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE);
      fail("It is not allowed to add a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding an identity link on the task should fail
    try {
      taskService.addUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER);
      fail("It is not allowed to add an identityLink on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }


    // Set an assignee on the task should fail
    try {
      taskService.setAssignee(taskId, "mispiggy");
      fail("It is not allowed to set an assignee on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Set an owner on the task should fail
    try {
      taskService.setOwner(taskId, "kermit");
      fail("It is not allowed to set an owner on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing candidate groups on the task should fail
    try {
      taskService.deleteCandidateGroup(taskId, "blahGroup");
      fail("It is not allowed to remove a candidate group on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing candidate users on the task should fail
    try {
      taskService.deleteCandidateUser(taskId, "blahUser");
      fail("It is not allowed to remove a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing group identity links on the task should fail
    try {
      taskService.deleteGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE);
      fail("It is not allowed to remove a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing an identity link on the task should fail
    try {
      taskService.deleteUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER);
      fail("It is not allowed to remove an identityLink on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskLifecycleOperationsFailAfterProcessInstanceSuspendByProcessDefinitionKey() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertNotNull(task);
    var taskId = task.getId();

    // Suspend the process instance
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    // Completing the task should fail
    try {
      taskService.complete(taskId);
      fail("It is not allowed to complete a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Claiming the task should fail
    try {
      taskService.claim(taskId, "jos");
      fail("It is not allowed to claim a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }



    // Adding candidate groups on the task should fail
    try {
      taskService.addCandidateGroup(taskId, "blahGroup");
      fail("It is not allowed to add a candidate group on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding candidate users on the task should fail
    try {
      taskService.addCandidateUser(taskId, "blahUser");
      fail("It is not allowed to add a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding group identity links on the task should fail
    try {
      taskService.addGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE);
      fail("It is not allowed to add a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Adding an identity link on the task should fail
    try {
      taskService.addUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER);
      fail("It is not allowed to add an identityLink on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }


    // Set an assignee on the task should fail
    try {
      taskService.setAssignee(taskId, "mispiggy");
      fail("It is not allowed to set an assignee on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Set an owner on the task should fail
    try {
      taskService.setOwner(taskId, "kermit");
      fail("It is not allowed to set an owner on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing candidate groups on the task should fail
    try {
      taskService.deleteCandidateGroup(taskId, "blahGroup");
      fail("It is not allowed to remove a candidate group on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing candidate users on the task should fail
    try {
      taskService.deleteCandidateUser(taskId, "blahUser");
      fail("It is not allowed to remove a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing group identity links on the task should fail
    try {
      taskService.deleteGroupIdentityLink(taskId, "blahGroup", IdentityLinkType.CANDIDATE);
      fail("It is not allowed to remove a candidate user on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }

    // Removing an identity link on the task should fail
    try {
      taskService.deleteUserIdentityLink(taskId, "blahUser", IdentityLinkType.OWNER);
      fail("It is not allowed to remove an identityLink on a task of a suspended process instance");
    } catch (SuspendedEntityInteractionException e) {
      // This is good
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSubTaskCreationFailAfterProcessInstanceSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    Task subTask = taskService.newTask("someTaskId");
    subTask.setParentTaskId(task.getId());

    try {
      taskService.saveTask(subTask);
      fail("Creating sub tasks for suspended task should not be possible");
    } catch (SuspendedEntityInteractionException e) {
      // expected
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSubTaskCreationFailAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    Task subTask = taskService.newTask("someTaskId");
    subTask.setParentTaskId(task.getId());

    try {
      taskService.saveTask(subTask);
      fail("Creating sub tasks for suspended task should not be possible");
    } catch (SuspendedEntityInteractionException e) {
      // expected
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSubTaskCreationFailAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    Task subTask = taskService.newTask("someTaskId");
    subTask.setParentTaskId(task.getId());

    try {
      taskService.saveTask(subTask);
      fail("Creating sub tasks for suspended task should not be possible");
    } catch (SuspendedEntityInteractionException e) {
      // expected
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testTaskNonLifecycleOperationsSucceedAfterProcessInstanceSuspend() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    assertNotNull(task);

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
      taskService.removeVariables(task.getId(), Arrays.asList("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariablesLocal(task.getId(), Arrays.asList("one", "two"));
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
  public void testTaskNonLifecycleOperationsSucceedAfterProcessInstanceSuspendByProcessDefinitionId() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());
    assertNotNull(task);

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
      taskService.removeVariables(task.getId(), Arrays.asList("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariablesLocal(task.getId(), Arrays.asList("one", "two"));
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
  public void testTaskNonLifecycleOperationsSucceedAfterProcessInstanceSuspendByProcessDefinitionKey() {

    // Start a new process instance with one task
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertNotNull(task);

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
      taskService.removeVariables(task.getId(), Arrays.asList("one", "two"));
    } catch (SuspendedEntityInteractionException e) {
      fail("should be allowed");
    }

    try {
      taskService.removeVariablesLocal(task.getId(), Arrays.asList("one", "two"));
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
  public void testJobNotExecutedAfterProcessInstanceSuspend() {

    Date now = new Date();
    ClockUtil.setCurrentTime(now);

    // Suspending the process instance should also stop the execution of jobs for that process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // The jobs should not be executed now
    ClockUtil.setCurrentTime(new Date(now.getTime() + (60 * 60 * 1000))); // Timer is set to fire on 5 minutes
    assertThat(managementService.createJobQuery().executable().count()).isEqualTo(0);

    // Activation of the process instance should now allow for job execution
    runtimeService.activateProcessInstanceById(processInstance.getId());
    assertThat(managementService.createJobQuery().executable().count()).isEqualTo(1);
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testJobNotExecutedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  public void testJobNotExecutedAfterProcessInstanceSuspendByProcessDefinitionId() {

    Date now = new Date();
    ClockUtil.setCurrentTime(now);

    // Suspending the process instance should also stop the execution of jobs for that process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // The jobs should not be executed now
    ClockUtil.setCurrentTime(new Date(now.getTime() + (60 * 60 * 1000))); // Timer is set to fire on 5 minutes
    assertThat(managementService.createJobQuery().executable().count()).isEqualTo(0);

    // Activation of the process instance should now allow for job execution
    runtimeService.activateProcessInstanceByProcessDefinitionId(processDefinition.getId());
    assertThat(managementService.createJobQuery().executable().count()).isEqualTo(1);
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.testJobNotExecutedAfterProcessInstanceSuspend.bpmn20.xml"})
  @Test
  public void testJobNotExecutedAfterProcessInstanceSuspendByProcessDefinitionKey() {

    Date now = new Date();
    ClockUtil.setCurrentTime(now);

    // Suspending the process instance should also stop the execution of jobs for that process instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // The jobs should not be executed now
    ClockUtil.setCurrentTime(new Date(now.getTime() + (60 * 60 * 1000))); // Timer is set to fire on 5 minutes
    assertThat(managementService.createJobQuery().executable().count()).isEqualTo(0);

    // Activation of the process instance should now allow for job execution
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    assertThat(managementService.createJobQuery().executable().count()).isEqualTo(1);
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callSimpleProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  public void testCallActivityReturnAfterProcessInstanceSuspend() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callSimpleProcess");
    runtimeService.suspendProcessInstanceById(instance.getId());

    Task task = taskService.createTaskQuery().singleResult();
    var taskId = task.getId();

    try {
      taskService.complete(taskId);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    // should be successful after reactivation
    runtimeService.activateProcessInstanceById(instance.getId());
    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callSimpleProcess.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  public void testCallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callSimpleProcess");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());

    Task task = taskService.createTaskQuery().singleResult();
    var taskId = task.getId();

    try {
      taskService.complete(taskId);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());
    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callSimpleProcess.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  public void testCallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("callSimpleProcess")
        .singleResult();

    runtimeService.startProcessInstanceByKey("callSimpleProcess");
    runtimeService.suspendProcessInstanceByProcessDefinitionKey(processDefinition.getKey());

    Task task = taskService.createTaskQuery().singleResult();
    var taskId = task.getId();

    try {
      taskService.complete(taskId);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callMISimpleProcess.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  public void testMICallActivityReturnAfterProcessInstanceSuspend() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callMISimpleProcess");
    runtimeService.suspendProcessInstanceById(instance.getId());

    List<Task> tasks = taskService.createTaskQuery().list();
    Task task1 = tasks.get(0);
    Task task2 = tasks.get(1);
    String task1Id = task1.getId();
    String task2Id = task2.getId();

    try {
      taskService.complete(task1Id);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    try {
      taskService.complete(task2Id);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    // should be successful after reactivation
    runtimeService.activateProcessInstanceById(instance.getId());
    taskService.complete(task1Id);
    taskService.complete(task2Id);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callMISimpleProcess.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  public void testMICallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionId() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callMISimpleProcess");
    runtimeService.suspendProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());

    List<Task> tasks = taskService.createTaskQuery().list();
    Task task1 = tasks.get(0);
    Task task2 = tasks.get(1);
    String task1Id = task1.getId();
    String task2Id = task2.getId();

    try {
      taskService.complete(task1Id);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    try {
      taskService.complete(task2Id);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionId(instance.getProcessDefinitionId());
    taskService.complete(task1Id);
    taskService.complete(task2Id);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceSuspensionTest.callMISimpleProcess.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  public void testMICallActivityReturnAfterProcessInstanceSuspendByProcessDefinitionKey() {
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

    try {
      taskService.complete(task1Id);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    try {
      taskService.complete(task2Id);
      fail("this should not be successful, as the execution of a suspended instance is resumed");
    } catch (SuspendedEntityInteractionException e) {
      // this is expected to fail
    }

    // should be successful after reactivation
    runtimeService.activateProcessInstanceByProcessDefinitionKey(processDefinition.getKey());
    taskService.complete(task1Id);
    taskService.complete(task2Id);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testStartBeforeActivityForSuspendProcessInstance() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testStartAfterActivityForSuspendProcessInstance() {
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
  public void testSuspensionByIdCascadesToExternalTasks() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    ExternalTask task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(task1.isSuspended());

    // when the process instance is suspended
    runtimeService.suspendProcessInstanceById(processInstance1.getId());

    // then the task is suspended
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertTrue(task1.isSuspended());

    // the other task is not
    ExternalTask task2 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance2.getId()).singleResult();
    assertFalse(task2.isSuspended());

    // when it is activated again
    runtimeService.activateProcessInstanceById(processInstance1.getId());

    // then the task is activated too
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(task1.isSuspended());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspensionByProcessDefinitionIdCascadesToExternalTasks() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    ExternalTask task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(task1.isSuspended());

    // when the process instance is suspended
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance1.getProcessDefinitionId());

    // then the task is suspended
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertTrue(task1.isSuspended());

    // the other task is not
    ExternalTask task2 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance2.getId()).singleResult();
    assertFalse(task2.isSuspended());

    // when it is activated again
    runtimeService.activateProcessInstanceByProcessDefinitionId(processInstance1.getProcessDefinitionId());

    // then the task is activated too
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(task1.isSuspended());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspensionByProcessDefinitionKeyCascadesToExternalTasks() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    ExternalTask task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(task1.isSuspended());

    // when the process instance is suspended
    runtimeService.suspendProcessInstanceByProcessDefinitionKey("oneExternalTaskProcess");

    // then the task is suspended
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertTrue(task1.isSuspended());

    // the other task is not
    ExternalTask task2 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance2.getId()).singleResult();
    assertFalse(task2.isSuspended());

    // when it is activated again
    runtimeService.activateProcessInstanceByProcessDefinitionKey("oneExternalTaskProcess");

    // then the task is activated too
    task1 = externalTaskService.createExternalTaskQuery()
        .processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(task1.isSuspended());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspendAndActivateProcessInstanceByIdUsingBuilder() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertFalse(processInstance.isSuspended());

    //suspend
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessInstanceId(processInstance.getId())
      .suspend();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);

    //activate
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessInstanceId(processInstance.getId())
      .activate();

    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspendAndActivateProcessInstanceByProcessDefinitionIdUsingBuilder() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);

    //suspend
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .suspend();

    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);

    //activate
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .activate();

    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSuspendAndActivateProcessInstanceByProcessDefinitionKeyUsingBuilder() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);

    //suspend
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey("oneTaskProcess")
      .suspend();

    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);

    //activate
    runtimeService
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey("oneTaskProcess")
      .activate();

    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testJobSuspensionStateUpdate() {

    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    String id = instance.getProcessInstanceId();

    //when
    runtimeService.suspendProcessInstanceById(id);
    Job job = managementService.createJobQuery().processInstanceId(id).singleResult();

    // then
    assertTrue(job.isSuspended());
  }

}
