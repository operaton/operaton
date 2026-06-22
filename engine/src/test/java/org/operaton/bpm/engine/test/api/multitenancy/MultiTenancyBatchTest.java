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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MultiTenancyBatchTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected BatchMigrationHelper batchHelper = new BatchMigrationHelper(engineRule);

  protected ManagementService managementService;
  protected HistoryService historyService;
  protected IdentityService identityService;

  protected ProcessDefinition tenant1Definition;
  protected ProcessDefinition tenant2Definition;
  protected ProcessDefinition sharedDefinition;

  @BeforeEach
  void deployProcesses() {
    sharedDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    tenant1Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    tenant2Definition = testHelper.deployForTenantAndGetDefinition(TENANT_TWO, ProcessModels.ONE_TASK_PROCESS);
  }

  @AfterEach
  void removeBatches() {
    batchHelper.removeAllRunningAndHistoricBatches();
  }

  /**
   * Source: no tenant id
   * Target: no tenant id
   */
  @Test
  void testBatchTenantIdCase1() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(sharedDefinition, sharedDefinition);

    // then
    assertThat(batch.getTenantId()).isNull();
  }

  /**
   * Source: tenant 1
   * Target: no tenant id
   */
  @Test
  void testBatchTenantIdCase2() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant1Definition, sharedDefinition);

    // then
    assertThat(batch.getTenantId()).isEqualTo(TENANT_ONE);
  }

  /**
   * Source: no tenant id
   * Target: tenant 1
   */
  @Test
  void testBatchTenantIdCase3() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(sharedDefinition, tenant1Definition);

    // then
    assertThat(batch.getTenantId()).isNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testHistoricBatchTenantId() {
    // given
    batchHelper.migrateProcessInstanceAsync(tenant1Definition, tenant1Definition);

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    assertThat(historicBatch.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testBatchJobDefinitionsTenantId() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant1Definition, tenant1Definition);

    // then
    JobDefinition migrationJobDefinition = batchHelper.getExecutionJobDefinition(batch);
    assertThat(migrationJobDefinition.getTenantId()).isEqualTo(TENANT_ONE);

    JobDefinition monitorJobDefinition = batchHelper.getMonitorJobDefinition(batch);
    assertThat(monitorJobDefinition.getTenantId()).isEqualTo(TENANT_ONE);

    JobDefinition seedJobDefinition = batchHelper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testBatchJobsTenantId() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant1Definition, tenant1Definition);

    // then
    Job seedJob = batchHelper.getSeedJob(batch);
    assertThat(seedJob.getTenantId()).isEqualTo(TENANT_ONE);

    batchHelper.completeSeedJobs(batch);

    List<Job> migrationJob = batchHelper.getExecutionJobs(batch);
    assertThat(migrationJob.get(0).getTenantId()).isEqualTo(TENANT_ONE);

    Job monitorJob = batchHelper.getMonitorJob(batch);
    assertThat(monitorJob.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testDeleteBatch() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant1Definition, tenant1Definition);

    // when
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));
    managementService.deleteBatch(batch.getId(), true);
    identityService.clearAuthentication();

    // then
    assertThat(managementService.createBatchQuery().count()).isZero();
  }

  @Test
  void testDeleteBatchFailsWithWrongTenant() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant2Definition, tenant2Definition);
    var batchId = batch.getId();

    // when
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));

    // then
    assertThatThrownBy(() -> managementService.deleteBatch(batchId, true))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete batch '"
        + batch.getId() + "' because it belongs to no authenticated tenant");

    identityService.clearAuthentication();
  }

  @Test
  void testSuspendBatch() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant1Definition, tenant1Definition);

    // when
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));
    managementService.suspendBatchById(batch.getId());
    identityService.clearAuthentication();

    // then
    batch = managementService.createBatchQuery().batchId(batch.getId()).singleResult();
    assertThat(batch.isSuspended()).isTrue();
  }

  @Test
  void testSuspendBatchFailsWithWrongTenant() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant2Definition, tenant2Definition);
    var batchId = batch.getId();

    // when
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));

    // then
    assertThatThrownBy(() -> managementService.suspendBatchById(batchId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot suspend batch '"
      + batch.getId() +"' because it belongs to no authenticated tenant");

    identityService.clearAuthentication();
  }

  @Test
  void testActivateBatch() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant1Definition, tenant1Definition);
    managementService.suspendBatchById(batch.getId());

    // when
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));
    managementService.activateBatchById(batch.getId());
    identityService.clearAuthentication();

    // then
    batch = managementService.createBatchQuery().batchId(batch.getId()).singleResult();
    assertThat(batch.isSuspended()).isFalse();
  }

  @Test
  void testActivateBatchFailsWithWrongTenant() {
    // given
    Batch batch = batchHelper.migrateProcessInstanceAsync(tenant2Definition, tenant2Definition);
    managementService.suspendBatchById(batch.getId());
    var batchId = batch.getId();

    // when
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));

    // then
    assertThatThrownBy(() -> managementService.activateBatchById(batchId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot activate batch '"
      + batch.getId() + "' because it belongs to no authenticated tenant");

    identityService.clearAuthentication();
  }

}
