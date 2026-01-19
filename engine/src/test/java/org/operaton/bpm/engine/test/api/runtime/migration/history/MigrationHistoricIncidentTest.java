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
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.AsyncProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MigrationHistoricIncidentTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  RuntimeService runtimeService;
  HistoryService historyService;
  ManagementService managementService;
  RepositoryService repositoryService;

  @Test
  void testMigrateHistoricIncident() {
    // given
    ProcessDefinition sourceProcess = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcess = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS)
      .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY)
      .changeElementId("userTask", "newUserTask"));

    JobDefinition targetJobDefinition =
        managementService
          .createJobDefinitionQuery()
          .processDefinitionId(targetProcess.getId())
          .singleResult();

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcess.getId(), targetProcess.getId())
      .mapActivities("userTask", "newUserTask")
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcess.getId());

    Job job = managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 0);

    HistoricIncident incidentBeforeMigration = historyService.createHistoricIncidentQuery().singleResult();

    // when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
    assertThat(historicIncident).isNotNull();

    assertThat(historicIncident.getActivityId()).isEqualTo("newUserTask");
    assertThat(historicIncident.getJobDefinitionId()).isEqualTo(targetJobDefinition.getId());
    assertThat(historicIncident.getProcessDefinitionId()).isEqualTo(targetProcess.getId());
    assertThat(historicIncident.getProcessDefinitionKey()).isEqualTo(targetProcess.getKey());
    assertThat(historicIncident.getExecutionId()).isEqualTo(processInstance.getId());

    // and other properties have not changed
    assertThat(historicIncident.getCreateTime()).isEqualTo(incidentBeforeMigration.getCreateTime());
    assertThat(historicIncident.getProcessInstanceId()).isEqualTo(incidentBeforeMigration.getProcessInstanceId());

  }

  @Test
  void testMigrateHistoricIncidentAddScope() {
    // given
    ProcessDefinition sourceProcess = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcess = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcess.getId(), targetProcess.getId())
      .mapActivities("userTask", "userTask")
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcess.getId());

    Job job = managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 0);

    // when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
    assertThat(historicIncident).isNotNull();
    assertThat(historicIncident.getExecutionId()).isEqualTo(activityInstance.getTransitionInstances("userTask")[0].getExecutionId());
  }
}
