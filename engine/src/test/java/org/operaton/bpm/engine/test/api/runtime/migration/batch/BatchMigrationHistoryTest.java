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
package org.operaton.bpm.engine.test.api.runtime.migration.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.batch.BatchMonitorJobHandler;
import org.operaton.bpm.engine.impl.batch.BatchSeedJobHandler;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.MigrationTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class BatchMigrationHistoryTest {

  protected static final Date START_DATE = new Date(1457326800000L);

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected MigrationTestRule migrationRule = new MigrationTestRule(engineRule);
  protected BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  protected ProcessEngineConfigurationImpl configuration;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;

  protected ProcessDefinition sourceProcessDefinition;
  protected ProcessDefinition targetProcessDefinition;

  protected boolean defaultEnsureJobDueDateSet;

  @Parameterized.Parameter(0)
  public boolean ensureJobDueDateSet;

  @Parameterized.Parameter(1)
  public Date currentTime;

  @Parameterized.Parameters(name = "Job DueDate is set: {0}")
  public static Collection<Object[]> scenarios() {
    return Arrays.asList(new Object[][] {
      { false, null },
      { true, START_DATE }
    });
  }

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(migrationRule);

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
  }

  @Before
  public void setupConfiguration() {
    configuration = engineRule.getProcessEngineConfiguration();
    defaultEnsureJobDueDateSet = configuration.isEnsureJobDueDateNotNull();
    configuration.setEnsureJobDueDateNotNull(ensureJobDueDateSet);
  }

  @Before
  public void setClock() {
    ClockUtil.setCurrentTime(START_DATE);
  }

  @After
  public void resetClock() {
    ClockUtil.reset();
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @After
  public void resetConfiguration() {
    configuration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @Test
  public void testHistoricBatchCreation() {
    // when
    Batch batch = helper.migrateProcessInstancesAsync(10);

    // then a historic batch was created
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    assertNotNull(historicBatch);
    assertThat(historicBatch.getId()).isEqualTo(batch.getId());
    assertThat(historicBatch.getType()).isEqualTo(batch.getType());
    assertThat(historicBatch.getTotalJobs()).isEqualTo(batch.getTotalJobs());
    assertThat(historicBatch.getBatchJobsPerSeed()).isEqualTo(batch.getBatchJobsPerSeed());
    assertThat(historicBatch.getInvocationsPerBatchJob()).isEqualTo(batch.getInvocationsPerBatchJob());
    assertThat(historicBatch.getSeedJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(historicBatch.getMonitorJobDefinitionId()).isEqualTo(batch.getMonitorJobDefinitionId());
    assertThat(historicBatch.getBatchJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(historicBatch.getStartTime()).isEqualTo(START_DATE);
    assertThat(historicBatch.getStartTime()).isEqualTo(batch.getStartTime());
    assertThat(historicBatch.getExecutionStartTime()).isEqualTo(batch.getExecutionStartTime());
    assertNull(historicBatch.getEndTime());
  }

  @Test
  public void testHistoricBatchCompletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    Date endDate = helper.addSecondsToClock(12);

    // when
    helper.executeMonitorJob(batch);

    // then the historic batch has an end time set
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    assertNotNull(historicBatch);
    assertThat(historicBatch.getEndTime()).isEqualTo(endDate);
  }

  @Test
  public void testHistoricSeedJobLog() {
    // when
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // then a historic job log exists for the seed job
    HistoricJobLog jobLog = helper.getHistoricSeedJobLog(batch).get(0);
    assertNotNull(jobLog);
    assertTrue(jobLog.isCreationLog());
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.getDeploymentId()).isEqualTo(helper.sourceProcessDefinition.getDeploymentId());
    assertNull(jobLog.getProcessDefinitionId());
    assertNull(jobLog.getExecutionId());
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    // when the seed job is executed
    Date executionDate = helper.addSecondsToClock(12);
    helper.completeSeedJobs(batch);

    // then a new historic job log exists for the seed job
    jobLog = helper.getHistoricSeedJobLog(batch).get(1);
    assertNotNull(jobLog);
    assertTrue(jobLog.isSuccessLog());
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getDeploymentId()).isEqualTo(helper.sourceProcessDefinition.getDeploymentId());
    assertNull(jobLog.getProcessDefinitionId());
    assertNull(jobLog.getExecutionId());
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

  }

  @Test
  public void testHistoricMonitorJobLog() {
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when the seed job is executed
    helper.completeSeedJobs(batch);

    Job monitorJob = helper.getMonitorJob(batch);
    List<HistoricJobLog> jobLogs = helper.getHistoricMonitorJobLog(batch, monitorJob);
    assertThat(jobLogs).hasSize(1);

    // then a creation historic job log exists for the monitor job without due date
    HistoricJobLog jobLog = jobLogs.get(0);
    assertCommonMonitorJobLogProperties(batch, jobLog);
    assertTrue(jobLog.isCreationLog());
    assertThat(jobLog.getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    // when the monitor job is executed
    Date executionDate = helper.addSecondsToClock(15);
    Date monitorJobDueDate = helper.addSeconds(executionDate, 30);
    helper.executeMonitorJob(batch);

    jobLogs = helper.getHistoricMonitorJobLog(batch, monitorJob);
    assertThat(jobLogs).hasSize(2);

    // then a success job log was created for the last monitor job
    jobLog = jobLogs.get(1);
    assertCommonMonitorJobLogProperties(batch, jobLog);
    assertTrue(jobLog.isSuccessLog());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    // and a creation job log for the new monitor job was created with due date
    monitorJob = helper.getMonitorJob(batch);
    jobLogs = helper.getHistoricMonitorJobLog(batch, monitorJob);
    assertThat(jobLogs).hasSize(1);

    jobLog = jobLogs.get(0);
    assertCommonMonitorJobLogProperties(batch, jobLog);
    assertTrue(jobLog.isCreationLog());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getJobDueDate()).isEqualTo(monitorJobDueDate);

    // when the migration and monitor jobs are executed
    executionDate = helper.addSecondsToClock(15);
    helper.executeJobs(batch);
    helper.executeMonitorJob(batch);

    jobLogs = helper.getHistoricMonitorJobLog(batch, monitorJob);
    assertThat(jobLogs).hasSize(2);

    // then a success job log was created for the last monitor job
    jobLog = jobLogs.get(1);
    assertCommonMonitorJobLogProperties(batch, jobLog);
    assertTrue(jobLog.isSuccessLog());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getJobDueDate()).isEqualTo(monitorJobDueDate);
  }

  @Test
  public void testHistoricBatchJobLog() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);

    String sourceDeploymentId = helper.getSourceProcessDefinition().getDeploymentId();

    // when
    Date executionDate = helper.addSecondsToClock(12);
    helper.executeJobs(batch);

    // then a historic job log exists for the batch job
    HistoricJobLog jobLog = helper.getHistoricBatchJobLog(batch).get(0);
    assertNotNull(jobLog);
    assertTrue(jobLog.isCreationLog());
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MIGRATION);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.getDeploymentId()).isEqualTo(sourceDeploymentId);
    assertNull(jobLog.getProcessDefinitionId());
    assertNull(jobLog.getExecutionId());
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    jobLog = helper.getHistoricBatchJobLog(batch).get(1);
    assertNotNull(jobLog);
    assertTrue(jobLog.isSuccessLog());
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MIGRATION);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getDeploymentId()).isEqualTo(sourceDeploymentId);
    assertNull(jobLog.getProcessDefinitionId());
    assertNull(jobLog.getExecutionId());
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);
  }

  @Test
  public void testHistoricBatchForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then the end time was set for the historic batch
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    assertNotNull(historicBatch);
    assertThat(historicBatch.getEndTime()).isEqualTo(deletionDate);
  }

  @Test
  public void testHistoricSeedJobLogForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricSeedJobLog(batch).get(1);
    assertNotNull(jobLog);
    assertTrue(jobLog.isDeletionLog());
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @Test
  public void testHistoricMonitorJobLogForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricMonitorJobLog(batch).get(1);
    assertNotNull(jobLog);
    assertTrue(jobLog.isDeletionLog());
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @Test
  public void testHistoricBatchJobLogForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricBatchJobLog(batch).get(1);
    assertNotNull(jobLog);
    assertTrue(jobLog.isDeletionLog());
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @Test
  public void testDeleteHistoricBatch() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);
    helper.executeMonitorJob(batch);

    // when
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    historyService.deleteHistoricBatch(historicBatch.getId());

    // then the historic batch was removed and all job logs
    assertNull(helper.getHistoricBatch(batch));
    assertTrue(helper.getHistoricSeedJobLog(batch).isEmpty());
    assertTrue(helper.getHistoricMonitorJobLog(batch).isEmpty());
    assertTrue(helper.getHistoricBatchJobLog(batch).isEmpty());
  }

  @Test
  public void testHistoricSeedJobIncidentDeletion() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    Job seedJob = helper.getSeedJob(batch);
    managementService.setJobRetries(seedJob.getId(), 0);

    managementService.deleteBatch(batch.getId(), false);

    // when
    historyService.deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isEqualTo(0);
  }

  @Test
  public void testHistoricMonitorJobIncidentDeletion() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    helper.completeSeedJobs(batch);
    Job monitorJob = helper.getMonitorJob(batch);
    managementService.setJobRetries(monitorJob.getId(), 0);

    managementService.deleteBatch(batch.getId(), false);

    // when
    historyService.deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isEqualTo(0);
  }

  @Test
  public void testHistoricBatchJobLogIncidentDeletion() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(3);

    helper.completeSeedJobs(batch);
    helper.failExecutionJobs(batch, 3);

    managementService.deleteBatch(batch.getId(), false);

    // when
    historyService.deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isEqualTo(0);
  }

  protected void assertCommonMonitorJobLogProperties(Batch batch, HistoricJobLog jobLog) {
    assertNotNull(jobLog);
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getMonitorJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchMonitorJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertNull(jobLog.getDeploymentId());
    assertNull(jobLog.getProcessDefinitionId());
    assertNull(jobLog.getExecutionId());
  }


}
