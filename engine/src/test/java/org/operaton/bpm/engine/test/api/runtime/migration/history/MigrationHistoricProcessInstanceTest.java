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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

/**
 *
 * @author Christopher Zell
 */
class MigrationHistoricProcessInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  RuntimeService runtimeService;
  HistoryService historyService;

  //============================================================================
  //===================================Migration================================
  //============================================================================
  protected ProcessDefinition sourceProcessDefinition;
  protected ProcessDefinition targetProcessDefinition;
  protected MigrationPlan migrationPlan;

  @BeforeEach
  void initTest() {
    sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ModifiableBpmnModelInstance modifiedModel = modify(ProcessModels.ONE_TASK_PROCESS).changeElementId("Process", "Process2")
                                                                                      .changeElementId("userTask", "userTask2");
    targetProcessDefinition = testHelper.deployAndGetDefinition(modifiedModel);
    migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
                                                                                            .mapActivities("userTask", "userTask2")
                                                                                            .build();
    runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateHistoryProcessInstance() {
    //given
    HistoricProcessInstanceQuery sourceHistoryProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery()
          .processDefinitionId(sourceProcessDefinition.getId());
    HistoricProcessInstanceQuery targetHistoryProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery()
          .processDefinitionId(targetProcessDefinition.getId());


    //when
    assertThat(sourceHistoryProcessInstanceQuery.count()).isEqualTo(1);
    assertThat(targetHistoryProcessInstanceQuery.count()).isZero();
    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    runtimeService.newMigration(migrationPlan)
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    //then
    assertThat(sourceHistoryProcessInstanceQuery.count()).isZero();
    assertThat(targetHistoryProcessInstanceQuery.count()).isEqualTo(1);

    HistoricProcessInstance instance = targetHistoryProcessInstanceQuery.singleResult();
    assertThat(targetProcessDefinition.getKey()).isEqualTo(instance.getProcessDefinitionKey());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testMigrateHistoryProcessInstanceState() {
    //given
    HistoricProcessInstanceQuery sourceHistoryProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery()
          .processDefinitionId(sourceProcessDefinition.getId());
    HistoricProcessInstanceQuery targetHistoryProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery()
          .processDefinitionId(targetProcessDefinition.getId());

    HistoricProcessInstance historicProcessInstanceBeforeMigration = sourceHistoryProcessInstanceQuery.singleResult();
    assertThat(historicProcessInstanceBeforeMigration.getState()).isEqualTo(HistoricProcessInstance.STATE_ACTIVE);

    //when
    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    runtimeService.newMigration(migrationPlan)
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    //then
    HistoricProcessInstance instance = targetHistoryProcessInstanceQuery.singleResult();
    assertThat(instance.getState()).isEqualTo(historicProcessInstanceBeforeMigration.getState());
  }

}
