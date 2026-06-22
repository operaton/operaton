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
package org.operaton.bpm.engine.test.api.multitenancy;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
class MultiTenancyMigrationAsyncTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  protected static ProcessEngineExtension defaultEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension defaultTestRule = new ProcessEngineTestExtension(defaultEngineRule);
  @RegisterExtension
  protected static MigrationTestExtension migrationRule = new MigrationTestExtension(defaultEngineRule);

  protected BatchMigrationHelper batchHelper = new BatchMigrationHelper(defaultEngineRule, migrationRule);

  @AfterEach
  void removeBatches() {
    batchHelper.removeAllRunningAndHistoricBatches();
  }

  @Test
  void canMigrateInstanceBetweenSameTenantCase1() {
    // given
    ProcessDefinition sourceDefinition = defaultTestRule.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = defaultTestRule.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    ProcessInstance processInstance = defaultEngineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    MigrationPlan migrationPlan = defaultEngineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    Batch batch = defaultEngineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .executeAsync();

    batchHelper.completeSeedJobs(batch);

    // when
    batchHelper.executeJobs(batch);

    // then
    assertMigratedTo(processInstance, targetDefinition);
  }

  @Test
  void cannotMigrateInstanceWithoutTenantIdToDifferentTenant() {
    // given
    ProcessDefinition sourceDefinition = defaultTestRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = defaultTestRule.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    ProcessInstance processInstance = defaultEngineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    MigrationPlan migrationPlan = defaultEngineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    Batch batch = defaultEngineRule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(List.of(processInstance.getId()))
        .executeAsync();

    batchHelper.completeSeedJobs(batch);

    // when
    batchHelper.executeJobs(batch);

    // then
    Job migrationJob = batchHelper.getExecutionJobs(batch).get(0);
    assertThat(migrationJob.getExceptionMessage()).contains
      ("Cannot migrate process instance '" + processInstance.getId()
            + "' without tenant to a process definition with a tenant ('tenant1')");
  }

  protected void assertMigratedTo(ProcessInstance processInstance, ProcessDefinition targetDefinition) {
    assertThat(defaultEngineRule.getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstance.getId())
      .processDefinitionId(targetDefinition.getId())
      .count()).isOne();
  }
}
