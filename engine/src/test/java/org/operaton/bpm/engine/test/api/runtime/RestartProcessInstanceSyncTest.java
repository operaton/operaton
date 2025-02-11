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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProvider;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderCaseInstanceContext;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderHistoricDecisionInstanceContext;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderProcessInstanceContext;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.AsyncProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.util.IncrementCounterListener;
import org.operaton.bpm.engine.test.bpmn.async.AsyncListener;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 * @author Anna Pazola
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class RestartProcessInstanceSyncTest {

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected HistoryService historyService;
  protected TenantIdProvider defaultTenantIdProvider;

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    historyService = engineRule.getHistoryService();
    defaultTenantIdProvider = engineRule.getProcessEngineConfiguration().getTenantIdProvider();
  }

  @After
  public void reset() {
    engineRule.getProcessEngineConfiguration().setTenantIdProvider(defaultTenantIdProvider);
  }

  @Test
  public void shouldRestartSimpleProcessInstance() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().singleResult();
    // process instance was deleted
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().active().singleResult();
    Task restartedTask = engineRule.getTaskService().createTaskQuery().processInstanceId(restartedProcessInstance.getId()).active().singleResult();
    assertThat(restartedTask.getTaskDefinitionKey()).isEqualTo(task.getTaskDefinitionKey());

    HistoricProcessInstanceEntity historicProcessInstanceEntity = (HistoricProcessInstanceEntity) historyService.createHistoricProcessInstanceQuery().processInstanceId(restartedProcessInstance.getId()).singleResult();
    assertThat(historicProcessInstanceEntity.getRestartedProcessInstanceId()).isEqualTo(processInstance.getId());

  }

  @Test
  public void shouldRestartProcessInstanceWithTwoTasks() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    // the first task is completed
    Task userTask1 = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().singleResult();
    taskService.complete(userTask1.getId());
    Task userTask2 = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().singleResult();
    // delete process instance
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask2")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().active().singleResult();
    Task restartedTask = taskService.createTaskQuery().processInstanceId(restartedProcessInstance.getId()).active().singleResult();
    assertThat(restartedTask.getTaskDefinitionKey()).isEqualTo(userTask2.getTaskDefinitionKey());

    ActivityInstance updatedTree = runtimeService.getActivityInstance(restartedProcessInstance.getId());
    assertNotNull(updatedTree);
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(restartedProcessInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(
            processDefinition.getId())
        .activity("userTask2")
        .done());

  }

  @Test
  public void shouldRestartProcessInstanceWithParallelGateway() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .startBeforeActivity("userTask2")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().active().singleResult();
    ActivityInstance updatedTree = runtimeService.getActivityInstance(restartedProcessInstance.getId());
    assertNotNull(updatedTree);
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(restartedProcessInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(
            processDefinition.getId())
        .activity("userTask1")
        .activity("userTask2")
        .done());
  }

  @Test
  public void shouldRestartProcessInstanceWithSubProcess() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("subProcess")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().active().singleResult();
    ActivityInstance updatedTree = runtimeService.getActivityInstance(restartedProcessInstance.getId());
    assertNotNull(updatedTree);
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(restartedProcessInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(
            processDefinition.getId())
        .beginScope("subProcess")
        .activity("userTask")
        .done());
  }

  @Test
  public void shouldRestartProcessInstanceWithVariables() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
        .startEvent()
        .userTask("userTask1")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableExecutionListenerImpl.class.getName())
        .userTask("userTask2")
        .endEvent()
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    // variable is set at the beginning
    runtimeService.setVariable(processInstance.getId(), "var", "bar");

    // variable is changed
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().singleResult();
    taskService.complete(task.getId());

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().active().singleResult();
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).singleResult();

    assertThat(restartedProcessInstance.getId()).isEqualTo(variableInstance.getExecutionId());
    assertThat(variableInstance.getName()).isEqualTo("var");
    assertThat(variableInstance.getValue()).isEqualTo("foo");
  }

  @Test
  public void shouldRestartProcessInstanceWithInitialVariables() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
        .startEvent("startEvent")
        .userTask("userTask1")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableExecutionListenerImpl.class.getName())
        .userTask("userTask2")
        .endEvent()
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    // initial variable
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValue("var", "bar"));

    // variable update
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().singleResult();
    taskService.complete(task.getId());

    // delete process instance
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .initialSetOfVariables()
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().active().singleResult();
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).singleResult();

    assertThat(restartedProcessInstance.getId()).isEqualTo(variableInstance.getExecutionId());
    assertThat(variableInstance.getName()).isEqualTo("var");
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Test
  public void shouldNotSetLocalVariables() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    Execution subProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId())
        .activityId("userTask").singleResult();
    runtimeService.setVariableLocal(subProcess.getId(), "local", "foo");
    runtimeService.setVariable(processInstance.getId(), "var", "bar");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getName()).isEqualTo("var");
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  public void shouldNotSetInitialVersionOfLocalVariables() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValue("var", "bar"));

    Execution subProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("userTask").singleResult();
    runtimeService.setVariableLocal(subProcess.getId(), "local", "foo");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
      .startBeforeActivity("userTask")
      .processInstanceIds(processInstance.getId())
      .initialSetOfVariables()
      .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getName()).isEqualTo("var");
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  public void shouldNotSetInitialVersionOfVariables() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.setVariable(processInstance.getId(), "bar", "foo");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask")
    .processInstanceIds(processInstance.getId())
    .initialSetOfVariables()
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).list();
    assertThat(variables.size()).isEqualTo(0);

    // details
    HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historyService
        .createHistoricDetailQuery()
        .singleResult();

    assertNotNull(detail);
    assertFalse(detail.isInitial());
    assertThat(detail.getVariableName()).isEqualTo("bar");
    assertThat(detail.getTextValue()).isEqualTo("foo");
  }

  @Test
  public void shouldSetInitialVersionOfVariables() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process",
        Variables.createVariables().putValue("var", "bar"));
    runtimeService.setVariable(processInstance.getId(), "bar", "foo");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask")
    .processInstanceIds(processInstance.getId())
    .initialSetOfVariables()
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getName()).isEqualTo("var");
    assertThat(variables.get(0).getValue()).isEqualTo("bar");

    // details
    HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historyService.createHistoricDetailQuery()
        .processInstanceId(restartedProcessInstance.getId())
        .singleResult();

    assertNotNull(detail);
    assertTrue(detail.isInitial());
    assertThat(detail.getVariableName()).isEqualTo("var");
    assertThat(detail.getTextValue()).isEqualTo("bar");
  }

  @Test
  public void shouldSetInitialVersionOfVariablesAsyncBeforeStartEvent() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_START_EVENT_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process",
        Variables.createVariables().putValue("var", "bar"));
    runtimeService.setVariable(processInstance.getId(), "bar", "foo");

    Job job = engineRule.getManagementService().createJobQuery().singleResult();
    engineRule.getManagementService().executeJob(job.getId());

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
      .startBeforeActivity("userTask")
      .processInstanceIds(processInstance.getId())
      .initialSetOfVariables()
      .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery()
        .processDefinitionId(processDefinition.getId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(restartedProcessInstance.getId()).list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getName()).isEqualTo("var");
    assertThat(variables.get(0).getValue()).isEqualTo("bar");

    // details
    HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historyService.createHistoricDetailQuery()
        .processInstanceId(restartedProcessInstance.getId())
        .singleResult();

    assertNotNull(detail);
    assertTrue(detail.isInitial());
    assertThat(detail.getVariableName()).isEqualTo("var");
    assertThat(detail.getTextValue()).isEqualTo("bar");
  }

  @Test
  public void shouldSetInitialVersionOfVariablesAsyncBeforeStartEventEndExecutionListener() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .operatonAsyncBefore()
        .operatonExecutionListenerClass("end", AsyncListener.class)
        .userTask("task")
        .endEvent()
        .done();

    testRule.deployAndGetDefinition(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("var", "bar"));
    runtimeService.setVariable(processInstance.getId(), "bar", "foo");

    Job job = engineRule.getManagementService().createJobQuery().singleResult();
    engineRule.getManagementService().executeJob(job.getId());

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
    .startBeforeActivity("task")
    .processInstanceIds(processInstance.getId())
    .initialSetOfVariables()
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery()
        .processDefinitionId(processInstance.getProcessDefinitionId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(restartedProcessInstance.getId()).variableName("var").list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
    variables = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(restartedProcessInstance.getId()).variableName("listener").list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getValue()).isEqualTo("listener invoked");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncStartEventListeners.bpmn20.xml"})
  public void shouldSetInitialVersionOfVariablesAsyncBeforeStartEventExecutionListener() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncStartEvent",
        Variables.createVariables().putValue("var", "bar"));
    runtimeService.setVariable(processInstance.getId(), "bar", "foo");

    Job job = engineRule.getManagementService().createJobQuery().singleResult();
    engineRule.getManagementService().executeJob(job.getId());

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
    .startBeforeActivity("task")
    .processInstanceIds(processInstance.getId())
    .initialSetOfVariables()
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery()
        .processDefinitionId(processInstance.getProcessDefinitionId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(restartedProcessInstance.getId()).variableName("var").list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
    variables = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(restartedProcessInstance.getId()).variableName("listener").list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getValue()).isEqualTo("listener invoked");
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testCallActivity-super.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testCallActivity-sub.bpmn20.xml"
  })
  public void shouldSetInitialVersionOfVariablesAsyncBeforeCallActivity() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("super",
        Variables.createVariables().putValue("var", "bar"));
    runtimeService.setVariable(processInstance.getId(), "bar", "foo");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
      .startBeforeActivity("theCallActivity")
      .processInstanceIds(processInstance.getId())
      .initialSetOfVariables()
      .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).active().singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(restartedProcessInstance.getId()).variableName("var").list();
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  public void shouldRestartProcessInstanceUsingHistoricProcessInstanceQuery() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    HistoricProcessInstanceQuery historicProcessInstanceQuery = engineRule.getHistoryService()
        .createHistoricProcessInstanceQuery()
        .processDefinitionId(processDefinition.getId());

    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .historicProcessInstanceQuery(historicProcessInstanceQuery)
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().active().singleResult();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(restartedProcessInstance.getId());
    assertNotNull(updatedTree);
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(restartedProcessInstance.getId());
    assertThat(updatedTree).hasStructure(
        describeActivityInstanceTree(
            processDefinition.getId())
        .activity("userTask1")
        .done());
  }

  @Test
  public void restartProcessInstanceWithNullProcessDefinitionId() {
    assertThatThrownBy(() -> runtimeService.restartProcessInstances(null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("processDefinitionId is null");
  }

  @Test
  public void restartProcessInstanceWithoutProcessInstanceIds() {
    // given
    var restartProcessInstanceBuilder = runtimeService
      .restartProcessInstances("foo")
      .startAfterActivity("bar");
    // when
    assertThatThrownBy(restartProcessInstanceBuilder::execute)
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("processInstanceIds is empty");
  }

  @Test
  public void restartProcessInstanceWithoutInstructions() {
    // given
    var restartProcessInstanceBuilder = runtimeService
      .restartProcessInstances("foo")
      .processInstanceIds("bar");
    // when
    assertThatThrownBy(restartProcessInstanceBuilder::execute)
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Restart instructions cannot be empty");
  }

  @Test
  public void restartProcessInstanceWithNullProcessInstanceId() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var restartProcessInstanceBuilder = runtimeService
      .restartProcessInstances(processDefinition.getId())
      .startAfterActivity("bar")
      .processInstanceIds((String) null);
    // when
    assertThatThrownBy(restartProcessInstanceBuilder::execute)
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Process instance ids cannot be null");
  }

  @Test
  public void restartNotExistingProcessInstance() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var restartProcessInstanceBuilder = runtimeService
      .restartProcessInstances(processDefinition.getId())
      .startBeforeActivity("bar")
      .processInstanceIds("aaa");
    // when
    assertThatThrownBy(restartProcessInstanceBuilder::execute)
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Historic process instance cannot be found");
  }

  @Test
  public void restartProcessInstanceWithNotMatchingProcessDefinition() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process2").startEvent().userTask().endEvent().done();
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    ProcessDefinition processDefinition2 = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.deleteProcessInstance(processInstance.getId(), null);
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances(
      processDefinition.getId()).startBeforeActivity("userTask").processInstanceIds(processInstance.getId());
    // when
    assertThatThrownBy(restartProcessInstanceBuilder::execute)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Its process definition '" + processDefinition2.getId() + "' does not match given process definition '" + processDefinition.getId() + "'");
  }

  @Test
  public void shouldRestartProcessInstanceWithoutBusinessKey() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process", "businessKey", (String) null);
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance.getId())
    .withoutBusinessKey()
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    assertNull(restartedProcessInstance.getBusinessKey());
  }

  @Test
  public void shouldRestartProcessInstanceWithBusinessKey() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process", "businessKey", (String) null);
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    assertNotNull(restartedProcessInstance.getBusinessKey());
    assertThat(restartedProcessInstance.getBusinessKey()).isEqualTo("businessKey");
  }

  @Test
  public void shouldRestartProcessInstanceWithoutCaseInstanceId() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process", null, "caseInstanceId");
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    assertNull(restartedProcessInstance.getCaseInstanceId());
  }

  @Test
  public void shouldRestartProcessInstanceWithTenant() {
    // given
    ProcessDefinition processDefinition = testRule.deployForTenantAndGetDefinition("tenantId", ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().singleResult();
    assertNotNull(restartedProcessInstance.getTenantId());
    assertThat(restartedProcessInstance.getTenantId()).isEqualTo(processInstance.getTenantId());
  }

  @Test
  public void shouldSkipCustomListeners() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(modify(ProcessModels.TWO_TASKS_PROCESS).activityBuilder("userTask1")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, IncrementCounterListener.class.getName()).done());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    IncrementCounterListener.counter = 0;
    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance.getId())
    .skipCustomListeners()
    .execute();

    // then
    assertThat(IncrementCounterListener.counter).isEqualTo(0);
  }

  @Test
  public void shouldSkipIoMappings() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(
        modify(ProcessModels.TWO_TASKS_PROCESS).activityBuilder("userTask1").operatonInputParameter("foo", "bar").done());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .skipIoMappings()
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    Execution task1Execution = runtimeService.createExecutionQuery().activityId("userTask1").singleResult();
    assertNotNull(task1Execution);
    assertNull(runtimeService.getVariable(task1Execution.getId(), "foo"));
  }

  @Test
  public void shouldRetainTenantIdOfSharedProcessDefinition() {
    // given
    engineRule.getProcessEngineConfiguration()
      .setTenantIdProvider(new TestTenantIdProvider());

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(processInstance.getTenantId()).isEqualTo(TestTenantIdProvider.TENANT_ID);
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
        .startBeforeActivity(ProcessModels.USER_TASK_ID)
        .processInstanceIds(processInstance.getId())
        .execute();

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
        .processDefinitionId(processDefinition.getId()).singleResult();

    assertNotNull(restartedInstance);
    assertThat(restartedInstance.getTenantId()).isEqualTo(TestTenantIdProvider.TENANT_ID);
  }

  @Test
  public void shouldSkipTenantIdProviderOnRestart() {
    // given
    engineRule.getProcessEngineConfiguration()
        .setTenantIdProvider(new TestTenantIdProvider());

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(processInstance.getTenantId()).isEqualTo(TestTenantIdProvider.TENANT_ID);
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // set tenant id provider to fail to verify it is not called during instantiation
    engineRule.getProcessEngineConfiguration()
        .setTenantIdProvider(new FailingTenantIdProvider());

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
      .startBeforeActivity(ProcessModels.USER_TASK_ID)
      .processInstanceIds(processInstance.getId())
      .execute();

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
      .processDefinitionId(processDefinition.getId()).singleResult();

    assertNotNull(restartedInstance);
    assertThat(restartedInstance.getTenantId()).isEqualTo(TestTenantIdProvider.TENANT_ID);
  }

  @Test
  public void shouldNotSetInitialVariablesIfThereIsNoUniqueStartActivity() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);

    ProcessInstance processInstance = runtimeService.createProcessInstanceById(processDefinition.getId())
        .startBeforeActivity("userTask1")
        .startBeforeActivity("userTask2")
        .setVariable("foo", "bar")
        .execute();

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .initialSetOfVariables()
    .processInstanceIds(processInstance.getId())
    .execute();

    // then
    ProcessInstance restartedProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).singleResult();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).list();
    assertThat(variables.size()).isEqualTo(0);
  }

  @Test
  public void shouldNotRestartActiveProcessInstance() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances(
        processDefinition.getId())
      .startBeforeActivity("userTask1")
      .initialSetOfVariables()
      .processInstanceIds(processInstance.getId());

    // when/then
    assertThatThrownBy(restartProcessInstanceBuilder::execute)
      .isInstanceOf(ProcessEngineException.class);
  }

  public static class SetVariableExecutionListenerImpl implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      execution.setVariable("var", "foo");
    }
  }

  public static class TestTenantIdProvider extends FailingTenantIdProvider {

    static final String TENANT_ID = "testTenantId";

    @Override
    public String provideTenantIdForProcessInstance(TenantIdProviderProcessInstanceContext ctx) {
      return TENANT_ID;
    }

  }

  public static class FailingTenantIdProvider implements TenantIdProvider {

    @Override
    public String provideTenantIdForProcessInstance(TenantIdProviderProcessInstanceContext ctx) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String provideTenantIdForCaseInstance(TenantIdProviderCaseInstanceContext ctx) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String provideTenantIdForHistoricDecisionInstance(TenantIdProviderHistoricDecisionInstanceContext ctx) {
      throw new UnsupportedOperationException();
    }
  }

}
