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
package org.operaton.bpm.engine.test.api.runtime.migration.history;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.AsyncProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CompensationModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.MultiInstanceProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationHistoricVariablesTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  protected static final BpmnModelInstance ONE_BOUNDARY_TASK = ModifiableBpmnModelInstance.modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
      .boundaryEvent()
      .message("Message")
      .done();

  protected static final BpmnModelInstance CONCURRENT_BOUNDARY_TASKS = ModifiableBpmnModelInstance.modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
      .boundaryEvent()
      .message("Message")
      .moveToActivity("userTask2")
      .boundaryEvent()
      .message("Message")
      .done();

  protected static final BpmnModelInstance SUBPROCESS_CONCURRENT_BOUNDARY_TASKS = ModifiableBpmnModelInstance.modify(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS)
      .activityBuilder("userTask1")
      .boundaryEvent()
      .message("Message")
      .moveToActivity("userTask2")
      .boundaryEvent()
      .message("Message")
      .done();

  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void noHistoryUpdateOnSameStructureMigration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_BOUNDARY_TASK);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ONE_BOUNDARY_TASK);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree scopeExecution = executionTreeBeforeMigration.getExecutions().get(0);

    runtimeService.setVariableLocal(scopeExecution.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then there is still one historic variable instance
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();

    // and no additional historic details
    assertThat(historyService.createHistoricDetailQuery().count()).isOne();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void noHistoryUpdateOnAddScopeMigration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CONCURRENT_BOUNDARY_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(SUBPROCESS_CONCURRENT_BOUNDARY_TASKS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0)
        .getParent();

    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then there is still one historic variable instance
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();

    // and no additional historic details
    assertThat(historyService.createHistoricDetailQuery().count()).isOne();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testMigrateHistoryVariableInstance() {
    //given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceDefinition.getId());

    runtimeService.setVariable(processInstance.getId(), "test", 3537);
    HistoricVariableInstance instance = historyService.createHistoricVariableInstanceQuery().singleResult();

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    //when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    //then
    HistoricVariableInstance migratedInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(migratedInstance.getProcessDefinitionKey()).isEqualTo(targetDefinition.getKey());
    assertThat(migratedInstance.getProcessDefinitionId()).isEqualTo(targetDefinition.getId());
    assertThat(migratedInstance.getActivityInstanceId()).isEqualTo(instance.getActivityInstanceId());
    assertThat(migratedInstance.getExecutionId()).isEqualTo(instance.getExecutionId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testMigrateHistoryVariableInstanceMultiInstance() {
    //given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_SUBPROCESS_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities()
      .build();

    //when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    //then
    List<HistoricVariableInstance> migratedVariables = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(migratedVariables).hasSize(6); // 3 loop counter + nrOfInstance + nrOfActiveInstances + nrOfCompletedInstances

    for (HistoricVariableInstance variable : migratedVariables) {
      assertThat(variable.getProcessDefinitionKey()).isEqualTo(targetDefinition.getKey());
      assertThat(variable.getProcessDefinitionId()).isEqualTo(targetDefinition.getId());

    }
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testMigrateEventScopeVariable() {
    //given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapActivities("userTask2", "userTask2")
      .mapActivities("subProcess", "subProcess")
      .mapActivities("compensationBoundary", "compensationBoundary")
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceDefinition.getId());

    Execution subProcessExecution = runtimeService.createExecutionQuery().activityId("userTask1").singleResult();

    runtimeService.setVariableLocal(subProcessExecution.getId(), "foo", "bar");

    testHelper.completeTask("userTask1");

    Execution eventScopeExecution = runtimeService.createExecutionQuery().activityId("subProcess").singleResult();
    HistoricVariableInstance eventScopeVariable = historyService
      .createHistoricVariableInstanceQuery()
      .executionIdIn(eventScopeExecution.getId())
      .singleResult();

    //when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(processInstance.getId())
      .execute();

    // then
    HistoricVariableInstance historicVariableInstance = historyService
      .createHistoricVariableInstanceQuery()
      .variableId(eventScopeVariable.getId())
      .singleResult();
    assertThat(historicVariableInstance.getProcessDefinitionId()).isEqualTo(targetDefinition.getId());
  }


  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testMigrateHistoricVariablesAsyncBeforeStartEvent() {
    //given
    String userTask = "task";
    BpmnModelInstance failing =
        Bpmn.createExecutableProcess("Process")
        .startEvent("startEvent")
        .operatonAsyncBefore(true)
        .serviceTask("failing")
        .operatonClass("foo")
        .userTask(userTask)
        .endEvent("endEvent")
        .done();
    BpmnModelInstance passing =
        Bpmn.createExecutableProcess("Process")
        .startEvent("startEvent")
        .operatonAsyncBefore(true)
        .userTask(userTask)
        .endEvent("endEvent")
        .done();

    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(failing);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(passing);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceDefinition.getId(),
        Variables.createVariables().putValue("foo", "bar"));

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    executeJob(job);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("startEvent", "startEvent")
        .mapActivities(userTask, userTask)
        .build();

    // when migrate
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then the failed job is also migrated
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isZero();
    managementService.setJobRetries(job.getId(), 1);

    // when the failed job is executed again
    executeJob(managementService.createJobQuery().singleResult());

    // then job succeeds
    assertThat(managementService.createJobQuery().singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().activityIdIn(userTask).singleResult()).isNotNull();

    // and variable history was written
    HistoricVariableInstance migratedInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(migratedInstance.getProcessDefinitionKey()).isEqualTo(targetDefinition.getKey());
    assertThat(migratedInstance.getProcessDefinitionId()).isEqualTo(targetDefinition.getId());

    // details
    HistoricVariableUpdateEventEntity historicDetail = (HistoricVariableUpdateEventEntity) historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(historicDetail).isNotNull();
    assertThat(historicDetail.isInitial()).isTrue();
    assertThat(historicDetail.getVariableName()).isEqualTo("foo");
    assertThat(historicDetail.getTextValue()).isEqualTo("bar");
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testMigrateHistoryVariableInstanceWithAsyncBefore() {
    //given
    BpmnModelInstance model = AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS;

    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(model)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceDefinition.getId());

    runtimeService.setVariable(processInstance.getId(), "test", 3537);
    HistoricVariableInstance instance = historyService.createHistoricVariableInstanceQuery().singleResult();

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    //when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    //then
    HistoricVariableInstance migratedInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(migratedInstance.getProcessDefinitionKey()).isEqualTo(targetDefinition.getKey());
    assertThat(migratedInstance.getProcessDefinitionId()).isEqualTo(targetDefinition.getId());
    assertThat(migratedInstance.getActivityInstanceId()).isEqualTo(instance.getActivityInstanceId());
    assertThat(migratedInstance.getExecutionId()).isEqualTo(instance.getExecutionId());
  }

  protected void executeJob(Job job) {
    while (job != null && job.getRetries() > 0) {
      executeJobIgnoringException(managementService, job.getId());

      job = managementService.createJobQuery().jobId(job.getId()).singleResult();
    }
  }
}
