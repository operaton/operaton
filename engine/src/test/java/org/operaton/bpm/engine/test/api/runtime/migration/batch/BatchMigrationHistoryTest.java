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
package org.operaton.bpm.engine.test.api.runtime.migration.batch;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

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
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class BatchMigrationHistoryTest {

  protected static final Date START_DATE = new Date(1457326800000L);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  ProcessEngineConfigurationImpl configuration;
  RuntimeService runtimeService;
  ManagementService managementService;
  HistoryService historyService;

  ProcessDefinition sourceProcessDefinition;
  ProcessDefinition targetProcessDefinition;

  boolean defaultEnsureJobDueDateSet;

  @Parameter(0)
  public boolean ensureJobDueDateSet;

  @Parameter(1)
  public Date currentTime;

  @Parameters(name = "Job DueDate is set: {0}")
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
      { false, null },
      { true, START_DATE }
    });
  }

  @BeforeEach
  void setupConfiguration() {
    configuration = engineRule.getProcessEngineConfiguration();
    defaultEnsureJobDueDateSet = configuration.isEnsureJobDueDateNotNull();
    configuration.setEnsureJobDueDateNotNull(ensureJobDueDateSet);
  }

  @BeforeEach
  void setClock() {
    ClockUtil.setCurrentTime(START_DATE);
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @AfterEach
  void resetConfiguration() {
    configuration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @TestTemplate
  void testHistoricBatchCreation() {
    // when
    Batch batch = helper.migrateProcessInstancesAsync(10);

    // then a historic batch was created
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    assertThat(historicBatch).isNotNull();
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
    assertThat(historicBatch.getEndTime()).isNull();
  }

  @TestTemplate
  void testHistoricBatchCompletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    Date endDate = helper.addSecondsToClock(12);

    // when
    helper.executeMonitorJob(batch);

    // then the historic batch has an end time set
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    assertThat(historicBatch).isNotNull();
    assertThat(historicBatch.getEndTime()).isEqualTo(endDate);
  }

  @TestTemplate
  void testHistoricSeedJobLog() {
    // when
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // then a historic job log exists for the seed job
    HistoricJobLog jobLog = helper.getHistoricSeedJobLog(batch).get(0);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isCreationLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.getDeploymentId()).isEqualTo(helper.sourceProcessDefinition.getDeploymentId());
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    // when the seed job is executed
    Date executionDate = helper.addSecondsToClock(12);
    helper.completeSeedJobs(batch);

    // then a new historic job log exists for the seed job
    jobLog = helper.getHistoricSeedJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isSuccessLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getDeploymentId()).isEqualTo(helper.sourceProcessDefinition.getDeploymentId());
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

  }

  @TestTemplate
  void testHistoricMonitorJobLog() {
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when the seed job is executed
    helper.completeSeedJobs(batch);

    Job monitorJob = helper.getMonitorJob(batch);
    List<HistoricJobLog> jobLogs = helper.getHistoricMonitorJobLog(batch, monitorJob);
    assertThat(jobLogs).hasSize(1);

    // then a creation historic job log exists for the monitor job without due date
    HistoricJobLog jobLog = jobLogs.get(0);
    assertCommonMonitorJobLogProperties(batch, jobLog);
    assertThat(jobLog.isCreationLog()).isTrue();
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
    assertThat(jobLog.isSuccessLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    // and a creation job log for the new monitor job was created with due date
    monitorJob = helper.getMonitorJob(batch);
    jobLogs = helper.getHistoricMonitorJobLog(batch, monitorJob);
    assertThat(jobLogs).hasSize(1);

    jobLog = jobLogs.get(0);
    assertCommonMonitorJobLogProperties(batch, jobLog);
    assertThat(jobLog.isCreationLog()).isTrue();
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
    assertThat(jobLog.isSuccessLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getJobDueDate()).isEqualTo(monitorJobDueDate);
  }

  @TestTemplate
  void testHistoricBatchJobLog() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);

    String sourceDeploymentId = helper.getSourceProcessDefinition().getDeploymentId();

    // when
    Date executionDate = helper.addSecondsToClock(12);
    helper.executeJobs(batch);

    // then a historic job log exists for the batch job
    HistoricJobLog jobLog = helper.getHistoricBatchJobLog(batch).get(0);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isCreationLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MIGRATION);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.getDeploymentId()).isEqualTo(sourceDeploymentId);
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    jobLog = helper.getHistoricBatchJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isSuccessLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MIGRATION);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getDeploymentId()).isEqualTo(sourceDeploymentId);
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);
  }

  @TestTemplate
  void testHistoricBatchForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then the end time was set for the historic batch
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    assertThat(historicBatch).isNotNull();
    assertThat(historicBatch.getEndTime()).isEqualTo(deletionDate);
  }

  @TestTemplate
  void testHistoricSeedJobLogForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricSeedJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isDeletionLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @TestTemplate
  void testHistoricMonitorJobLogForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricMonitorJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isDeletionLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @TestTemplate
  void testHistoricBatchJobLogForBatchDeletion() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    managementService.deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricBatchJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isDeletionLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @TestTemplate
  void testDeleteHistoricBatch() {
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);
    helper.executeMonitorJob(batch);

    // when
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    historyService.deleteHistoricBatch(historicBatch.getId());

    // then the historic batch was removed and all job logs
    assertThat(helper.getHistoricBatch(batch)).isNull();
    assertThat(helper.getHistoricSeedJobLog(batch)).isEmpty();
    assertThat(helper.getHistoricMonitorJobLog(batch)).isEmpty();
    assertThat(helper.getHistoricBatchJobLog(batch)).isEmpty();
  }

  @TestTemplate
  void testHistoricSeedJobIncidentDeletion() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    Job seedJob = helper.getSeedJob(batch);
    managementService.setJobRetries(seedJob.getId(), 0);

    managementService.deleteBatch(batch.getId(), false);

    // when
    historyService.deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @TestTemplate
  void testHistoricMonitorJobIncidentDeletion() {
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
    assertThat(historicIncidents).isZero();
  }

  @TestTemplate
  void testHistoricBatchJobLogIncidentDeletion() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(3);

    helper.completeSeedJobs(batch);
    helper.failExecutionJobs(batch, 3);

    managementService.deleteBatch(batch.getId(), false);

    // when
    historyService.deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  protected void assertCommonMonitorJobLogProperties(Batch batch, HistoricJobLog jobLog) {
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getMonitorJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchMonitorJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getDeploymentId()).isNull();
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
  }


}
