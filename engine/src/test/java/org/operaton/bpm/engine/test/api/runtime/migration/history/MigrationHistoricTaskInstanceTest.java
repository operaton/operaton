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
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.Task;
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
class MigrationHistoricTaskInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  RuntimeService runtimeService;
  HistoryService historyService;
  TaskService taskService;

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateHistoryUserTaskInstance() {
    //given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(ProcessModels.ONE_TASK_PROCESS)
          .changeElementId("Process", "Process2")
          .changeElementId("userTask", "userTask2"));

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask2")
        .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    HistoricTaskInstanceQuery sourceHistoryTaskInstanceQuery =
        historyService.createHistoricTaskInstanceQuery()
          .processDefinitionId(sourceProcessDefinition.getId());
    HistoricTaskInstanceQuery targetHistoryTaskInstanceQuery =
        historyService.createHistoricTaskInstanceQuery()
          .processDefinitionId(targetProcessDefinition.getId());

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    //when
    assertThat(sourceHistoryTaskInstanceQuery.count()).isOne();
    assertThat(targetHistoryTaskInstanceQuery.count()).isZero();
    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    runtimeService.newMigration(migrationPlan)
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    //then
    assertThat(sourceHistoryTaskInstanceQuery.count()).isZero();
    assertThat(targetHistoryTaskInstanceQuery.count()).isOne();

    HistoricTaskInstance instance = targetHistoryTaskInstanceQuery.singleResult();
    assertThat(instance.getProcessDefinitionKey()).isEqualTo(targetProcessDefinition.getKey());
    assertThat(instance.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    assertThat(instance.getTaskDefinitionKey()).isEqualTo("userTask2");
    assertThat(instance.getActivityInstanceId()).isEqualTo(activityInstance.getActivityInstances("userTask")[0].getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateWithSubTask() {
    //given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    Task subTask = taskService.newTask();
    subTask.setParentTaskId(task.getId());
    taskService.saveTask(subTask);

    // when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then the historic sub task instance is still the same
    HistoricTaskInstance historicSubTaskAfterMigration = historyService
        .createHistoricTaskInstanceQuery().taskId(subTask.getId()).singleResult();

    assertThat(historicSubTaskAfterMigration).isNotNull();
    assertThat(historicSubTaskAfterMigration.getProcessDefinitionId()).isNull();
    assertThat(historicSubTaskAfterMigration.getProcessDefinitionKey()).isNull();
    assertThat(historicSubTaskAfterMigration.getExecutionId()).isNull();
    assertThat(historicSubTaskAfterMigration.getActivityInstanceId()).isNull();
  }
}
