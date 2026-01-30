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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProvider;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderCaseInstanceContext;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderHistoricDecisionInstanceContext;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderProcessInstanceContext;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.RestartProcessInstanceSyncTest.SetVariableExecutionListenerImpl;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.util.IncrementCounterListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ClockTestUtil;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Anna Pazola
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class RestartProcessInstanceAsyncTest {

  protected static final Date TEST_DATE = new Date(1457326800000L);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  protected BatchRestartHelper helper = new BatchRestartHelper(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;

  TenantIdProvider defaultTenantIdProvider;
  boolean defaultEnsureJobDueDateSet;

  @BeforeEach
  void init() {
    defaultTenantIdProvider = processEngineConfiguration.getTenantIdProvider();
    defaultEnsureJobDueDateSet = processEngineConfiguration.isEnsureJobDueDateNotNull();
  }

  @AfterEach
  void reset() {
    helper.removeAllRunningAndHistoricBatches();
    processEngineConfiguration.setTenantIdProvider(defaultTenantIdProvider);
    processEngineConfiguration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @Test
  void createBatchRestart() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    List<String> processInstanceIds = List.of(processInstance1.getId(), processInstance2.getId());

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startAfterActivity("userTask2")
        .processInstanceIds(processInstanceIds)
        .executeAsync();

    // then
    assertBatchCreated(batch, 2);
  }

  @Test
  void restartProcessInstanceWithNullProcessDefinitionId() {
    assertThatThrownBy(() -> runtimeService.restartProcessInstances(null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("processDefinitionId is null");
  }

  @Test
  void restartProcessInstanceWithoutInstructions() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances(processDefinition.getId()).processInstanceIds(processInstance.getId());

    assertThatThrownBy(restartProcessInstanceBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("instructions is empty");
  }

  @Test
  void restartProcessInstanceWithoutProcessInstanceIds() {
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances("foo").startAfterActivity("bar");

    assertThatThrownBy(restartProcessInstanceBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("processInstanceIds is empty");
  }

  @Test
  void restartProcessInstanceWithNullProcessInstanceId() {
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances("foo")
      .startAfterActivity("bar")
      .processInstanceIds((String) null);

    assertThatThrownBy(restartProcessInstanceBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("processInstanceIds contains null value");
  }

  @Test
  void restartNotExistingProcessInstance() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startBeforeActivity("bar")
        .processInstanceIds("aaa")
        .executeAsync();
    helper.completeSeedJobs(batch);

    assertThatThrownBy(() -> helper.executeJobs(batch))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Historic process instance cannot be found");
  }

  @Test
  void shouldRestartProcessInstance() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).active().singleResult();
    Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).active().singleResult();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startBeforeActivity("userTask1")
        .processInstanceIds(processInstance1.getId(),processInstance2.getId())
        .executeAsync();

    helper.completeSeedJobs(batch);

    // then
    helper.getExecutionJobs(batch).forEach(j -> assertThat(j.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId()));

    // when
    helper.completeExecutionJobs(batch);
    helper.completeMonitorJobs(batch);

    // and
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().active().list();
    ProcessInstance restartedProcessInstance = restartedProcessInstances.get(0);
    Task restartedTask = engineRule.getTaskService().createTaskQuery().processInstanceId(restartedProcessInstance.getId()).active().singleResult();
    assertThat(restartedTask.getTaskDefinitionKey()).isEqualTo(task1.getTaskDefinitionKey());

    restartedProcessInstance = restartedProcessInstances.get(1);
    restartedTask = engineRule.getTaskService().createTaskQuery().processInstanceId(restartedProcessInstance.getId()).active().singleResult();
    assertThat(restartedTask.getTaskDefinitionKey()).isEqualTo(task2.getTaskDefinitionKey());
  }

  @Test
  void shouldAssignRestartProcessInstanceIdOnlyToRestartedProcessInstances() {
    // given process instances 1 and 2
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);

    var processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    var processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when process instance 1 is restarted
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
            .startBeforeActivity("userTask1")
            .processInstanceIds(processInstance1.getId())
            .executeAsync();

    helper.completeSeedJobs(batch);

    helper.getExecutionJobs(batch).forEach(j -> assertThat(j.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId()));
    helper.completeExecutionJobs(batch);
    helper.completeMonitorJobs(batch);

    // then the restartedProcessInstanceId should be populated only for the restarted process instance 1
    var restartedProcessInstance = runtimeService.createProcessInstanceQuery()
            .active()
            .singleResult();

    var historicProcessInstance1 = (HistoricProcessInstanceEntity) historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(restartedProcessInstance.getId())
            .singleResult();

    var historicProcessInstance2 = (HistoricProcessInstanceEntity) historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processInstance2.getId())
            .singleResult();

    assertThat(historicProcessInstance1.getRestartedProcessInstanceId()).isEqualTo(processInstance1.getId());
    assertThat(historicProcessInstance2.getRestartedProcessInstanceId()).isNull();
  }

  @Test
  void shouldRestartProcessInstanceWithParallelGateway() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startBeforeActivity("userTask1")
        .startBeforeActivity("userTask2")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().active().list();
    for (ProcessInstance restartedProcessInstance : restartedProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(restartedProcessInstance.getId());
      Assertions.assertThat(updatedTree).isNotNull();
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(restartedProcessInstance.getId());
      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .activity("userTask1")
          .activity("userTask2")
          .done());
    }
  }

  @Test
  void shouldRestartProcessInstanceWithSubProcess() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startBeforeActivity("subProcess")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().active().list();
    for (ProcessInstance restartedProcessInstance : restartedProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(restartedProcessInstance.getId());
      Assertions.assertThat(updatedTree).isNotNull();
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(restartedProcessInstance.getId());
      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .beginScope("subProcess")
          .activity("userTask")
          .done());
    }
  }

  @Test
  void shouldRestartProcessInstanceWithInitialVariables() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
        .startEvent()
        .userTask("userTask1")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableExecutionListenerImpl.class.getName())
        .userTask("userTask2")
        .endEvent()
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

     // initial variables
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValue("var", "bar"));
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValue("var", "bar"));

    // variables update
    List<Task> tasks = taskService.createTaskQuery().processDefinitionId(processDefinition.getId()).active().list();
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // delete process instances
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .initialSetOfVariables()
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().list();
    VariableInstance variableInstance1 = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstances.get(0).getId()).singleResult();
    VariableInstance variableInstance2 = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstances.get(1).getId()).singleResult();

    assertThat(restartedProcessInstances.get(0).getId()).isEqualTo(variableInstance1.getExecutionId());
    assertThat(restartedProcessInstances.get(1).getId()).isEqualTo(variableInstance2.getExecutionId());
    assertThat(variableInstance1.getName()).isEqualTo("var");
    assertThat(variableInstance1.getValue()).isEqualTo("bar");
    assertThat(variableInstance2.getName()).isEqualTo("var");
    assertThat(variableInstance2.getValue()).isEqualTo("bar");
  }

  @Test
  void shouldRestartProcessInstanceWithVariables() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
        .startEvent()
        .userTask("userTask1")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableExecutionListenerImpl.class.getName())
        .userTask("userTask2")
        .endEvent()
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    // variables are set at the beginning
    runtimeService.setVariable(processInstance1.getId(), "var", "bar");
    runtimeService.setVariable(processInstance2.getId(), "var", "bb");

    // variables are changed
    List<Task> tasks = taskService.createTaskQuery().processDefinitionId(processDefinition.getId()).active().list();
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // process instances are deleted
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startBeforeActivity("userTask1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().active().list();
    ProcessInstance restartedProcessInstance = restartedProcessInstances.get(0);
    VariableInstance variableInstance1 = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).singleResult();
    assertThat(restartedProcessInstance.getId()).isEqualTo(variableInstance1.getExecutionId());

    restartedProcessInstance = restartedProcessInstances.get(1);
    VariableInstance variableInstance2 = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstance.getId()).singleResult();
    assertThat(restartedProcessInstance.getId()).isEqualTo(variableInstance2.getExecutionId());
    assertThat(variableInstance2.getName()).isEqualTo(variableInstance1.getName());
    assertThat(variableInstance1.getName()).isEqualTo("var");
    assertThat(variableInstance2.getValue()).isEqualTo(variableInstance1.getValue());
    assertThat(variableInstance2.getValue()).isEqualTo("foo");
  }

  @Test
  void shouldNotSetLocalVariables() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    Execution subProcess1 = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).activityId("userTask").singleResult();
    Execution subProcess2 = runtimeService.createExecutionQuery().processInstanceId(processInstance2.getId()).activityId("userTask").singleResult();
    runtimeService.setVariableLocal(subProcess1.getId(), "local", "foo");
    runtimeService.setVariableLocal(subProcess2.getId(), "local", "foo");

    runtimeService.setVariable(processInstance1.getId(), "var", "bar");
    runtimeService.setVariable(processInstance2.getId(), "var", "bar");


    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");


    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask")
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .executeAsync();

    helper.completeBatch(batch);
    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().list();
    List<VariableInstance> variables1 = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstances.get(0).getId()).list();
    assertThat(variables1).hasSize(1);
    assertThat(variables1.get(0).getName()).isEqualTo("var");
    assertThat(variables1.get(0).getValue()).isEqualTo("bar");
    List<VariableInstance> variables2 = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstances.get(1).getId()).list();
    assertThat(variables1).hasSize(1);
    assertThat(variables2.get(0).getName()).isEqualTo("var");
    assertThat(variables2.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  void shouldRestartProcessInstanceUsingHistoricProcessInstanceQuery() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).active().singleResult();

    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");
    Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).active().singleResult();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    HistoricProcessInstanceQuery historicProcessInstanceQuery = engineRule.getHistoryService().createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId());

    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startBeforeActivity("userTask1")
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().active().list();
    ProcessInstance restartedProcessInstance = restartedProcessInstances.get(0);
    Task restartedTask = taskService.createTaskQuery().processInstanceId(restartedProcessInstance.getId()).active().singleResult();
    assertThat(restartedTask.getTaskDefinitionKey()).isEqualTo(task1.getTaskDefinitionKey());

    restartedProcessInstance = restartedProcessInstances.get(1);
    restartedTask = taskService.createTaskQuery().processInstanceId(restartedProcessInstance.getId()).active().singleResult();
    assertThat(restartedTask.getTaskDefinitionKey()).isEqualTo(task2.getTaskDefinitionKey());
  }

  @Test
  void testBatchCreationWithOverlappingProcessInstanceIdsAndQuery() {
    // given
    int processInstanceCount = 2;
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    HistoricProcessInstanceQuery processInstanceQuery = engineRule.getHistoryService()
        .createHistoricProcessInstanceQuery()
        .processDefinitionId(processDefinition.getId());

    Batch batch = runtimeService
      .restartProcessInstances(processDefinition.getId())
      .startTransition("flow1")
      .processInstanceIds(processInstance1.getId(), processInstance2.getId())
      .historicProcessInstanceQuery(processInstanceQuery)
      .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = engineRule.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).list();
    assertThat(processInstanceCount).isEqualTo(restartedProcessInstances.size());
  }

  @Test
  void testMonitorJobPollingForCompletion() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(false);

    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
        .restartProcessInstances(processDefinition.getId())
        .startTransition("flow1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    // when the seed job creates the monitor job
    Date createDate = ClockTestUtil.setClockToDateWithoutMilliseconds();
    helper.completeSeedJobs(batch);

    // then the monitor job has a no due date set
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob).isNotNull();
    assertThat(monitorJob.getDuedate()).isNull();

    // when the monitor job is executed
    helper.executeMonitorJob(batch);

    // then the monitor job has a due date of the default batch poll time
    monitorJob = helper.getMonitorJob(batch);
    Date dueDate = helper.addSeconds(createDate, 30);
    assertThat(monitorJob.getDuedate()).isEqualTo(dueDate);
  }

  @Test
  void testMonitorJobPollingForCompletionDueDateSet() {
    ClockUtil.setCurrentTime(TEST_DATE);
    processEngineConfiguration.setEnsureJobDueDateNotNull(true);

    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
      .restartProcessInstances(processDefinition.getId())
      .startTransition("flow1")
      .processInstanceIds(processInstance1.getId(), processInstance2.getId())
      .executeAsync();

    // when the seed job creates the monitor job
    helper.completeSeedJobs(batch);

    // then the monitor job has the create date as due date set
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob).isNotNull();
    assertThat(monitorJob.getDuedate()).isEqualTo(TEST_DATE);

    // when the monitor job is executed
    helper.executeMonitorJob(batch);

    // then the monitor job has a due date of the default batch poll time
    monitorJob = helper.getMonitorJob(batch);
    Date dueDate = helper.addSeconds(TEST_DATE, 30);
    assertThat(monitorJob.getDuedate()).isEqualTo(dueDate);
  }

  @Test
  void testMonitorJobRemovesBatchAfterCompletion() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
        .restartProcessInstances(processDefinition.getId())
        .startTransition("flow1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    helper.executeMonitorJob(batch);

    // then the batch was completed and removed
    assertThat(engineRule.getManagementService().createBatchQuery().count()).isZero();

    // and the seed jobs was removed
    assertThat(engineRule.getManagementService().createJobQuery().count()).isZero();
  }

  @Test
  void testBatchDeletionWithCascade() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
        .restartProcessInstances(processDefinition.getId())
        .startTransition("flow1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeSeedJobs(batch);

    engineRule.getManagementService().deleteBatch(batch.getId(), true);

    // then the batch was deleted
    assertThat(engineRule.getManagementService().createBatchQuery().count()).isZero();

    // and the seed and execution job definition were deleted
    assertThat(engineRule.getManagementService().createJobDefinitionQuery().count()).isZero();

    // and the seed job and execution jobs were deleted
    assertThat(engineRule.getManagementService().createJobQuery().count()).isZero();
  }

  @Test
  void testBatchDeletionWithoutCascade() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
        .restartProcessInstances(processDefinition.getId())
        .startTransition("flow1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeSeedJobs(batch);

    engineRule.getManagementService().deleteBatch(batch.getId(), false);

    // then the batch was deleted
    assertThat(engineRule.getManagementService().createBatchQuery().count()).isZero();

    // and the seed and execution job definition were deleted
    assertThat(engineRule.getManagementService().createJobDefinitionQuery().count()).isZero();

    // and the seed job and execution jobs were deleted
    assertThat(engineRule.getManagementService().createJobQuery().count()).isZero();
  }

  @Test
  void testBatchWithFailedSeedJobDeletionWithCascade() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
        .restartProcessInstances(processDefinition.getId())
        .startTransition("flow1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    // create incident
    Job seedJob = helper.getSeedJob(batch);
    engineRule.getManagementService().setJobRetries(seedJob.getId(), 0);

    engineRule.getManagementService().deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = engineRule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @Test
  void testBatchWithFailedExecutionJobDeletionWithCascade() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
        .restartProcessInstances(processDefinition.getId())
        .startTransition("flow1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeSeedJobs(batch);

    // create incidents
    List<Job> executionJobs = helper.getExecutionJobs(batch);
    for (Job executionJob : executionJobs) {
      engineRule.getManagementService().setJobRetries(executionJob.getId(), 0);
    }

    engineRule.getManagementService().deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = engineRule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @Test
  void testBatchWithFailedMonitorJobDeletionWithCascade() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService
        .restartProcessInstances(processDefinition.getId())
        .startTransition("flow1")
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .executeAsync();

    helper.completeSeedJobs(batch);

    // create incident
    Job monitorJob = helper.getMonitorJob(batch);
    engineRule.getManagementService().setJobRetries(monitorJob.getId(), 0);

    engineRule.getManagementService().deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = engineRule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @Test
  void testJobsExecutionByJobExecutorWithAuthorizationEnabledAndTenant() {
    // given
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);
    ProcessDefinition processDefinition = testRule.deployForTenantAndGetDefinition("tenantId", ProcessModels.TWO_TASKS_PROCESS);

    try {
      ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
      ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

      List<String> list = List.of(processInstance1.getId(), processInstance2.getId());

      runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
      runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

      // when
      Batch batch = runtimeService
          .restartProcessInstances(processDefinition.getId())
          .startTransition("flow1")
          .processInstanceIds(list)
          .executeAsync();
      helper.completeSeedJobs(batch);

      testRule.waitForJobExecutorToProcessAllJobs();

      List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).list();
      // then all process instances were restarted
      for (ProcessInstance restartedProcessInstance : restartedProcessInstances) {
        ActivityInstance updatedTree = runtimeService.getActivityInstance(restartedProcessInstance.getId());
        Assertions.assertThat(updatedTree).isNotNull();
        assertThat(updatedTree.getProcessInstanceId()).isEqualTo(restartedProcessInstance.getId());
        assertThat(restartedProcessInstance.getTenantId()).isEqualTo("tenantId");

        assertThat(updatedTree).hasStructure(
            describeActivityInstanceTree(
                processDefinition.getId())
            .activity("userTask2")
            .done());
      }

    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
    }

  }

  @Test
  void restartProcessInstanceWithNotMatchingProcessDefinition() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.deleteProcessInstance(processInstance.getId(), null);
    BpmnModelInstance instance2 = Bpmn.createExecutableProcess().startEvent().done();
    ProcessDefinition processDefinition2 = testRule.deployAndGetDefinition(instance2);

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition2.getId())
        .startBeforeActivity("userTask1")
        .processInstanceIds(processInstance.getId())
        .executeAsync();

    assertThatThrownBy(() -> helper.completeBatch(batch))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Its process definition '%s' does not match given process definition '%s'".formatted(processDefinition.getId(), processDefinition2.getId()));
  }

  @Test
  void shouldRestartProcessInstanceWithoutBusinessKey() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process", "businessKey1", (String) null);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process", "businessKey2", (String) null);

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .withoutBusinessKey()
    .executeAsync();

    helper.completeBatch(batch);
    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().list();
    ProcessInstance restartedProcessInstance1 = restartedProcessInstances.get(0);
    ProcessInstance restartedProcessInstance2 = restartedProcessInstances.get(1);
    assertThat(restartedProcessInstance1.getBusinessKey()).isNull();
    assertThat(restartedProcessInstance2.getBusinessKey()).isNull();
  }

  @Test
  void shouldRestartProcessInstanceWithBusinessKey() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process", "businessKey1", (String) null);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process", "businessKey2", (String) null);

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .executeAsync();

    helper.completeBatch(batch);
    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().list();
    ProcessInstance restartedProcessInstance1 = restartedProcessInstances.get(0);
    ProcessInstance restartedProcessInstance2 = restartedProcessInstances.get(1);
    assertThat(restartedProcessInstance1.getBusinessKey()).isNotNull();
    assertThat(restartedProcessInstance2.getBusinessKey()).isNotNull();
  }

  @Test
  void shouldRestartProcessInstanceWithoutCaseInstanceId() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process", null, "caseInstanceId1");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process", null, "caseInstanceId2");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .executeAsync();

    helper.completeBatch(batch);
    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().list();
    ProcessInstance restartedProcessInstance1 = restartedProcessInstances.get(0);
    ProcessInstance restartedProcessInstance2 = restartedProcessInstances.get(1);
    assertThat(restartedProcessInstance1.getCaseInstanceId()).isNull();
    assertThat(restartedProcessInstance2.getCaseInstanceId()).isNull();
  }

  @Test
  void shouldRestartProcessInstanceWithTenant() {
    // given
    ProcessDefinition processDefinition = testRule.deployForTenantAndGetDefinition("tenantId", ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");


    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .executeAsync();

    helper.completeBatch(batch);
    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().list();
    assertThat(restartedProcessInstances.get(0).getTenantId()).isNotNull();
    assertThat(restartedProcessInstances.get(1).getTenantId()).isNotNull();
    assertThat(restartedProcessInstances.get(0).getTenantId()).isEqualTo("tenantId");
    assertThat(restartedProcessInstances.get(1).getTenantId()).isEqualTo("tenantId");
  }

  @Test
  void shouldSkipCustomListeners() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(modify(ProcessModels.TWO_TASKS_PROCESS).activityBuilder("userTask1")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, IncrementCounterListener.class.getName()).done());
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    IncrementCounterListener.counter = 0;
    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .skipCustomListeners()
    .executeAsync();

    helper.completeBatch(batch);
    // then
    assertThat(IncrementCounterListener.counter).isZero();
  }

  @Test
  void shouldSkipIoMappings() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(
        modify(ProcessModels.TWO_TASKS_PROCESS).activityBuilder("userTask1").operatonInputParameter("foo", "bar").done());
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .skipIoMappings()
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).list();
    Execution task1Execution = runtimeService.createExecutionQuery().processInstanceId(restartedProcessInstances.get(0).getId()).activityId("userTask1").singleResult();
    assertThat(task1Execution).isNotNull();
    assertThat(runtimeService.getVariable(task1Execution.getId(), "foo")).isNull();

    task1Execution = runtimeService.createExecutionQuery().processInstanceId(restartedProcessInstances.get(1).getId()).activityId("userTask1").singleResult();
    assertThat(task1Execution).isNotNull();
    assertThat(runtimeService.getVariable(task1Execution.getId(), "foo")).isNull();
  }

  @Test
  void shouldRetainTenantIdOfSharedProcessDefinition() {
    // given
    engineRule.getProcessEngineConfiguration()
      .setTenantIdProvider(new TestTenantIdProvider());

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(processInstance.getTenantId()).isEqualTo(TestTenantIdProvider.TENANT_ID);
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
      .startBeforeActivity(ProcessModels.USER_TASK_ID)
      .processInstanceIds(processInstance.getId())
      .executeAsync();

    helper.completeBatch(batch);

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
      .processDefinitionId(processDefinition.getId()).singleResult();

    assertThat(restartedInstance).isNotNull();
    assertThat(restartedInstance.getTenantId()).isEqualTo(TestTenantIdProvider.TENANT_ID);
  }

  @Test
  void shouldSkipTenantIdProviderOnRestart() {
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
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
      .startBeforeActivity(ProcessModels.USER_TASK_ID)
      .processInstanceIds(processInstance.getId())
      .executeAsync();

    helper.completeBatch(batch);

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
      .processDefinitionId(processDefinition.getId()).singleResult();

    assertThat(restartedInstance).isNotNull();
    assertThat(restartedInstance.getTenantId()).isEqualTo(TestTenantIdProvider.TENANT_ID);
  }

  @Test
  void shouldNotSetInitialVariablesIfThereIsNoUniqueStartActivity() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.createProcessInstanceById(processDefinition.getId())
        .startBeforeActivity("userTask2")
        .startBeforeActivity("userTask1")
        .execute();

    ProcessInstance processInstance2 = runtimeService.createProcessInstanceById(processDefinition.getId())
        .startBeforeActivity("userTask1")
        .startBeforeActivity("userTask2")
        .setVariable("foo", "bar")
        .execute();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
    .startBeforeActivity("userTask1")
    .initialSetOfVariables()
    .processInstanceIds(processInstance1.getId(), processInstance2.getId())
    .executeAsync();

    helper.completeBatch(batch);

    // then
    List<ProcessInstance> restartedProcessInstances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).list();
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().processInstanceIdIn(restartedProcessInstances.get(0).getId(), restartedProcessInstances.get(1).getId()).list();
    assertThat(variables).isEmpty();
  }

  @Test
  void shouldSetInvocationsPerBatchType() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_PROCESS_INSTANCE_RESTART, 42);

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    List<String> processInstanceIds = List.of(processInstance1.getId(), processInstance2.getId());

    // when
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startAfterActivity("userTask2")
        .processInstanceIds(processInstanceIds)
        .executeAsync();

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @Test
  void shouldSetExecutionStartTimeInBatchAndHistory() {
    // given
    ClockUtil.setCurrentTime(TEST_DATE);

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance1 = runtimeService.createProcessInstanceById(processDefinition.getId())
        .startBeforeActivity("userTask1")
        .execute();
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId())
        .startAfterActivity("userTask2")
        .processInstanceIds(processInstance1.getId())
        .executeAsync();
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch);

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    batch = managementService.createBatchQuery().singleResult();

    assertThat(batch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
    assertThat(historicBatch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
  }

  protected void assertBatchCreated(Batch batch, int processInstanceCount) {
    assertThat(batch).isNotNull();
    assertThat(batch.getId()).isNotNull();
    assertThat(batch.getType()).isEqualTo("instance-restart");
    assertThat(batch.getTotalJobs()).isEqualTo(processInstanceCount);
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
