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
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationHistoricActivityInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  RuntimeService runtimeService;
  HistoryService historyService;

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateHistoryActivityInstance() {
    //given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(ProcessModels.ONE_TASK_PROCESS)
          .changeElementId("Process", "Process2")
          .changeElementId("userTask", "userTask2")
          .changeElementName("userTask", "new activity name"));

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask2")
        .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    HistoricActivityInstanceQuery sourceHistoryActivityInstanceQuery =
        historyService.createHistoricActivityInstanceQuery()
          .processDefinitionId(sourceProcessDefinition.getId());
    HistoricActivityInstanceQuery targetHistoryActivityInstanceQuery =
        historyService.createHistoricActivityInstanceQuery()
          .processDefinitionId(targetProcessDefinition.getId());

    //when
    assertThat(sourceHistoryActivityInstanceQuery.count()).isEqualTo(2);
    assertThat(targetHistoryActivityInstanceQuery.count()).isZero();
    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    runtimeService.newMigration(migrationPlan)
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    // then one instance of the start event still belongs to the source process
    // and one active user task instances is now migrated to the target process
    assertThat(sourceHistoryActivityInstanceQuery.count()).isOne();
    assertThat(targetHistoryActivityInstanceQuery.count()).isOne();

    HistoricActivityInstance instance = targetHistoryActivityInstanceQuery.singleResult();
    assertMigratedTo(instance, targetProcessDefinition, "userTask2");
    assertThat(instance.getActivityName()).isEqualTo("new activity name");
    assertThat(instance.getParentActivityInstanceId()).isEqualTo(processInstance.getId());
    assertThat(instance.getActivityType()).isEqualTo("userTask");
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateHistoricSubProcessInstance() {
    //given
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(processDefinition.getId(), processDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(processDefinition.getId());

    // when
    rule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    List<HistoricActivityInstance> historicInstances = historyService
        .createHistoricActivityInstanceQuery()
        .processInstanceId(processInstance.getId())
        .unfinished()
        .orderByActivityId()
        .asc()
        .list();

    assertThat(historicInstances).hasSize(2);

    assertMigratedTo(historicInstances.get(0), processDefinition, "subProcess");
    assertMigratedTo(historicInstances.get(1), processDefinition, "userTask");
    assertThat(historicInstances.get(0).getParentActivityInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicInstances.get(1).getParentActivityInstanceId()).isEqualTo(historicInstances.get(0).getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateHistoricSubProcessRename() {
    //given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
        .changeElementId("subProcess", "newSubProcess"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("subProcess", "newSubProcess")
        .mapActivities("userTask", "userTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    // when
    rule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    List<HistoricActivityInstance> historicInstances = historyService
        .createHistoricActivityInstanceQuery()
        .processInstanceId(processInstance.getId())
        .unfinished()
        .orderByActivityId()
        .asc()
        .list();

    assertThat(historicInstances).hasSize(2);

    assertMigratedTo(historicInstances.get(0), targetDefinition, "newSubProcess");
    assertMigratedTo(historicInstances.get(1), targetDefinition, "userTask");
    assertThat(historicInstances.get(0).getParentActivityInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicInstances.get(1).getParentActivityInstanceId()).isEqualTo(historicInstances.get(0).getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testHistoricActivityInstanceBecomeScope() {
    //given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    // when
    rule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    List<HistoricActivityInstance> historicInstances = historyService
        .createHistoricActivityInstanceQuery()
        .processInstanceId(processInstance.getId())
        .unfinished()
        .orderByActivityId()
        .asc()
        .list();

    assertThat(historicInstances).hasSize(1);

    assertMigratedTo(historicInstances.get(0), targetDefinition, "userTask");
    assertThat(historicInstances.get(0).getParentActivityInstanceId()).isEqualTo(processInstance.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateHistoricActivityInstanceAddScope() {
    //given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("userTask", "userTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    // when
    rule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    List<HistoricActivityInstance> historicInstances = historyService
        .createHistoricActivityInstanceQuery()
        .processInstanceId(processInstance.getId())
        .unfinished()
        .orderByActivityId()
        .asc()
        .list();

    assertThat(historicInstances).hasSize(2);

    assertMigratedTo(historicInstances.get(0), targetDefinition, "subProcess");
    assertMigratedTo(historicInstances.get(1), targetDefinition, "userTask");
    assertThat(historicInstances.get(0).getParentActivityInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicInstances.get(1).getParentActivityInstanceId()).isEqualTo(historicInstances.get(0).getId());
  }

  protected void assertMigratedTo(HistoricActivityInstance activityInstance, ProcessDefinition processDefinition, String activityId) {
    assertThat(activityInstance.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(activityInstance.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(activityInstance.getActivityId()).isEqualTo(activityId);
  }

}
