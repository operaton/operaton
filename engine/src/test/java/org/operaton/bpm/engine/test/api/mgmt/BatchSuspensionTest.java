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
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.EntityTypes.BATCH;
import static org.junit.Assert.fail;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.AbstractSetBatchStateCmd;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.MigrationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class BatchSuspensionTest {

  public static final String USER_ID = "userId";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected MigrationTestRule migrationRule = new MigrationTestRule(engineRule);
  protected BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(migrationRule);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;
  protected IdentityService identityService;

  protected int defaultBatchJobsPerSeed;

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
    identityService = engineRule.getIdentityService();
  }

  @Before
  public void saveAndReduceBatchJobsPerSeed() {
    ProcessEngineConfigurationImpl configuration = engineRule.getProcessEngineConfiguration();
    defaultBatchJobsPerSeed = configuration.getBatchJobsPerSeed();
    // reduce number of batch jobs per seed to not have to create a lot of instances
    configuration.setBatchJobsPerSeed(1);
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @After
  public void resetBatchJobsPerSeed() {
    engineRule.getProcessEngineConfiguration()
      .setBatchJobsPerSeed(defaultBatchJobsPerSeed);
  }

  @Test
  public void shouldSuspendBatch() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    managementService.suspendBatchById(batch.getId());

    // then
    batch = managementService.createBatchQuery().batchId(batch.getId()).singleResult();
    assertThat(batch.isSuspended()).isTrue();
  }

  @Test
  public void shouldFailWhenSuspendingUsingUnknownId() {
    try {
      managementService.suspendBatchById("unknown");
      fail("Exception expected");
    }
    catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Batch for id 'unknown' cannot be found");
    }
  }

  @Test
  public void shouldFailWhenSuspendingUsingNullId() {
    try {
      managementService.suspendBatchById(null);
      fail("Exception expected");
    }
    catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("batch id is null");
    }
  }

  @Test
  public void shouldSuspendSeedJobAndDefinition() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    managementService.suspendBatchById(batch.getId());

    // then
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition.isSuspended()).isTrue();

    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.isSuspended()).isTrue();
  }

  @Test
  public void shouldCreateSuspendedSeedJob() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(2);
    managementService.suspendBatchById(batch.getId());

    // when
    helper.executeSeedJob(batch);

    // then
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.isSuspended()).isTrue();
  }

  @Test
  public void shouldSuspendMonitorJobAndDefinition() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.executeSeedJob(batch);

    // when
    managementService.suspendBatchById(batch.getId());

    // then
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    assertThat(monitorJobDefinition.isSuspended()).isTrue();

    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.isSuspended()).isTrue();
  }

  @Test
  public void shouldCreateSuspendedMonitorJob() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());

    // when
    helper.executeSeedJob(batch);

    // then
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.isSuspended()).isTrue();
  }

  @Test
  public void shouldSuspendExecutionJobsAndDefinition() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.executeSeedJob(batch);

    // when
    managementService.suspendBatchById(batch.getId());

    // then
    JobDefinition migrationJobDefinition = helper.getExecutionJobDefinition(batch);
    assertThat(migrationJobDefinition.isSuspended()).isTrue();

    Job migrationJob = helper.getExecutionJobs(batch).get(0);
    assertThat(migrationJob.isSuspended()).isTrue();
  }

  @Test
  public void shouldCreateSuspendedExecutionJobs() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());

    // when
    helper.executeSeedJob(batch);

    // then
    Job migrationJob = helper.getExecutionJobs(batch).get(0);
    assertThat(migrationJob.isSuspended()).isTrue();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void shouldCreateUserOperationLogForBatchSuspension() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    managementService.suspendBatchById(batch.getId());
    identityService.clearAuthentication();

    // then
    UserOperationLogEntry entry = historyService.createUserOperationLogQuery()
      .singleResult();

    assertThat(entry).isNotNull();
    assertThat(entry.getBatchId()).isEqualTo(batch.getId());
    assertThat(entry.getProperty()).isEqualTo(AbstractSetBatchStateCmd.SUSPENSION_STATE_PROPERTY);
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(SuspensionState.SUSPENDED.getName());
  }

  @Test
  public void shouldActivateBatch() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());

    // when
    managementService.activateBatchById(batch.getId());

    // then
    batch = managementService.createBatchQuery().batchId(batch.getId()).singleResult();
    assertThat(batch.isSuspended()).isFalse();
  }

  @Test
  public void shouldFailWhenActivatingUsingUnknownId() {
    try {
      managementService.activateBatchById("unknown");
      fail("Exception expected");
    }
    catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Batch for id 'unknown' cannot be found");
    }
  }

  @Test
  public void shouldFailWhenActivatingUsingNullId() {
    try {
      managementService.activateBatchById(null);
      fail("Exception expected");
    }
    catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("batch id is null");
    }
  }

  @Test
  public void shouldActivateSeedJobAndDefinition() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());

    // when
    managementService.activateBatchById(batch.getId());

    // then
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition.isSuspended()).isFalse();

    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.isSuspended()).isFalse();
  }

  @Test
  public void shouldCreateActivatedSeedJob() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(2);

    // when
    helper.executeSeedJob(batch);

    // then
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.isSuspended()).isFalse();
  }

  @Test
  public void shouldActivateMonitorJobAndDefinition() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());
    helper.executeSeedJob(batch);

    // when
    managementService.activateBatchById(batch.getId());

    // then
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    assertThat(monitorJobDefinition.isSuspended()).isFalse();

    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.isSuspended()).isFalse();
  }

  @Test
  public void shouldCreateActivatedMonitorJob() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    helper.executeSeedJob(batch);

    // then
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.isSuspended()).isFalse();
  }

  @Test
  public void shouldActivateExecutionJobsAndDefinition() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());
    helper.executeSeedJob(batch);

    // when
    managementService.activateBatchById(batch.getId());

    // then
    JobDefinition migrationJobDefinition = helper.getExecutionJobDefinition(batch);
    assertThat(migrationJobDefinition.isSuspended()).isFalse();

    Job migrationJob = helper.getExecutionJobs(batch).get(0);
    assertThat(migrationJob.isSuspended()).isFalse();
  }

  @Test
  public void shouldCreateActivatedExecutionJobs() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    helper.executeSeedJob(batch);

    // then
    Job migrationJob = helper.getExecutionJobs(batch).get(0);
    assertThat(migrationJob.isSuspended()).isFalse();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void shouldCreateUserOperationLogForBatchActivation() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    managementService.activateBatchById(batch.getId());
    identityService.clearAuthentication();

    // then
    UserOperationLogEntry entry = historyService.createUserOperationLogQuery()
      .singleResult();

    assertThat(entry).isNotNull();
    assertThat(entry.getBatchId()).isEqualTo(batch.getId());
    assertThat(entry.getProperty()).isEqualTo(AbstractSetBatchStateCmd.SUSPENSION_STATE_PROPERTY);
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(SuspensionState.ACTIVE.getName());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testUserOperationLogQueryByBatchEntityType() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    managementService.suspendBatchById(batch1.getId());
    managementService.suspendBatchById(batch2.getId());
    managementService.activateBatchById(batch1.getId());
    identityService.clearAuthentication();

    // then
    UserOperationLogQuery query = historyService.createUserOperationLogQuery().entityType(BATCH);
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testUserOperationLogQueryByBatchId() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    managementService.suspendBatchById(batch1.getId());
    managementService.suspendBatchById(batch2.getId());
    managementService.activateBatchById(batch1.getId());
    identityService.clearAuthentication();

    // then
    UserOperationLogQuery query = historyService.createUserOperationLogQuery().batchId(batch1.getId());
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);

    query = historyService.createUserOperationLogQuery().batchId(batch2.getId());
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);
  }

}
