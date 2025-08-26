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
package org.operaton.bpm.qa.upgrade.gson.batch;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

/**
 * @author Tassilo Weidner
 */
public final class MigrationBatchScenario {

  private MigrationBatchScenario() {
  }

  @DescribesScenario("initMigrationBatch")
  public static ScenarioSetup initMigrationBatch() {
    return new ScenarioSetup() {
      public void execute(ProcessEngine engine, String scenarioName) {

        String sourceProcessDefinitionId = engine.getRepositoryService().createDeployment()
          .addClasspathResource("org/operaton/bpm/qa/upgrade/gson/oneTaskProcessMigrationV1.bpmn20.xml")
          .deployWithResult()
          .getDeployedProcessDefinitions()
          .get(0)
          .getId();

        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
          String processInstanceId = engine.getRuntimeService()
            .startProcessInstanceById(sourceProcessDefinitionId, "MigrationBatchScenario").getId();

          processInstanceIds.add(processInstanceId);
        }

        String targetProcessDefinitionId = engine.getRepositoryService().createDeployment()
          .addClasspathResource("org/operaton/bpm/qa/upgrade/gson/oneTaskProcessMigrationV2.bpmn20.xml")
          .deployWithResult()
          .getDeployedProcessDefinitions()
          .get(0)
          .getId();

        MigrationPlan migrationPlan = engine.getRuntimeService()
          .createMigrationPlan(sourceProcessDefinitionId, targetProcessDefinitionId)
          .mapActivities("userTask1", "userTask1")
          .mapActivities("conditional", "conditional")
            .updateEventTrigger()
          .build();

        Batch batch = engine.getRuntimeService().newMigration(migrationPlan)
          .processInstanceIds(processInstanceIds)
          .skipIoMappings()
          .skipCustomListeners()
          .executeAsync();
        engine.getManagementService().setProperty("MigrationBatchScenario.batchId", batch.getId());
      }
    };
  }
}
