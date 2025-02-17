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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class BatchModificationHistoryTest {

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(rule);
  protected BatchModificationHelper helper = new BatchModificationHelper(rule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected BpmnModelInstance instance;

  private int defaultBatchJobsPerSeed;
  private int defaultInvocationsPerBatchJob;
  private boolean defaultEnsureJobDueDateSet;

  protected static final Date START_DATE = new Date(1457326800000L);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testRule);

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

  @Before
  public void setClock() {
    ClockUtil.setCurrentTime(START_DATE);
  }

  @Before
  public void createBpmnModelInstance() {
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .userTask("user1")
        .sequenceFlowId("seq")
        .userTask("user2")
        .endEvent("end")
        .done();
  }

  @Before
  public void initServices() {
    runtimeService = rule.getRuntimeService();
  }

  @Before
  public void storeEngineSettings() {
    processEngineConfiguration = rule.getProcessEngineConfiguration();
    defaultBatchJobsPerSeed = processEngineConfiguration.getBatchJobsPerSeed();
    defaultInvocationsPerBatchJob = processEngineConfiguration.getInvocationsPerBatchJob();
    defaultEnsureJobDueDateSet = processEngineConfiguration.isEnsureJobDueDateNotNull();
    processEngineConfiguration.setEnsureJobDueDateNotNull(ensureJobDueDateSet);
  }

  @After
  public void removeInstanceIds() {
    helper.currentProcessInstances = new ArrayList<>();
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @After
  public void restoreEngineSettings() {
    processEngineConfiguration.setBatchJobsPerSeed(defaultBatchJobsPerSeed);
    processEngineConfiguration.setInvocationsPerBatchJob(defaultInvocationsPerBatchJob);
    processEngineConfiguration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @After
  public void resetClock() {
    ClockUtil.reset();
  }

  @Test
  public void testHistoricBatchCreation() {
    // when
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 10, "user1", processDefinition.getId());

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
    assertThat(historicBatch.getEndTime()).isNull();
  }

  @Test
  public void testHistoricBatchCompletion() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 1, "user1", processDefinition.getId());
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

  @Test
  public void testHistoricSeedJobLog() {
    // when
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.cancelAllAsync("process1", 1, "user1", processDefinition.getId());

    // then a historic job log exists for the seed job
    HistoricJobLog jobLog = helper.getHistoricSeedJobLog(batch).get(0);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isCreationLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    // when the seed job is executed
    Date executionDate = helper.addSecondsToClock(12);
    helper.executeSeedJob(batch);

    // then a new historic job log exists for the seed job
    jobLog = helper.getHistoricSeedJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isSuccessLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

  }

  @Test
  public void testHistoricMonitorJobLog() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 1, "user1", processDefinition.getId());

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

    // when the modification and monitor jobs are executed
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

  @Test
  public void testHistoricBatchJobLog() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 1, "user1", processDefinition.getId());
    helper.completeSeedJobs(batch);

    // when
    Date executionDate = helper.addSecondsToClock(12);
    helper.executeJobs(batch);

    // then a historic job log exists for the batch job
    HistoricJobLog jobLog = helper.getHistoricBatchJobLog(batch).get(0);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isCreationLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MODIFICATION);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);

    jobLog = helper.getHistoricBatchJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isSuccessLog()).isTrue();
    assertThat(jobLog.getJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(jobLog.getJobDefinitionType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MODIFICATION);
    assertThat(jobLog.getJobDefinitionConfiguration()).isEqualTo(batch.getId());
    assertThat(jobLog.getTimestamp()).isEqualTo(executionDate);
    assertThat(jobLog.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    assertThat(jobLog.getProcessDefinitionId()).isNull();
    assertThat(jobLog.getExecutionId()).isNull();
    assertThat(jobLog.getJobDueDate()).isEqualTo(currentTime);
  }

  @Test
  public void testHistoricBatchForBatchDeletion() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startTransitionAsync("process1", 1, "seq", processDefinition.getId());

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    rule.getManagementService().deleteBatch(batch.getId(), false);

    // then the end time was set for the historic batch
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    assertThat(historicBatch).isNotNull();
    assertThat(historicBatch.getEndTime()).isEqualTo(deletionDate);
  }

  @Test
  public void testHistoricSeedJobLogForBatchDeletion() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startBeforeAsync("process1", 1, "user1", processDefinition.getId());

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    rule.getManagementService().deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricSeedJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isDeletionLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @Test
  public void testHistoricMonitorJobLogForBatchDeletion() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 1, "user1", processDefinition.getId());
    helper.completeSeedJobs(batch);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    rule.getManagementService().deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricMonitorJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isDeletionLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @Test
  public void testHistoricBatchJobLogForBatchDeletion() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startBeforeAsync("process1", 1, "user2", processDefinition.getId());
    helper.completeSeedJobs(batch);

    // when
    Date deletionDate = helper.addSecondsToClock(12);
    rule.getManagementService().deleteBatch(batch.getId(), false);

    // then a deletion historic job log was added
    HistoricJobLog jobLog = helper.getHistoricBatchJobLog(batch).get(1);
    assertThat(jobLog).isNotNull();
    assertThat(jobLog.isDeletionLog()).isTrue();
    assertThat(jobLog.getTimestamp()).isEqualTo(deletionDate);
  }

  @Test
  public void testDeleteHistoricBatch() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startTransitionAsync("process1", 1, "seq", processDefinition.getId());
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);
    helper.executeMonitorJob(batch);

    // when
    HistoricBatch historicBatch = helper.getHistoricBatch(batch);
    rule.getHistoryService().deleteHistoricBatch(historicBatch.getId());

    // then the historic batch was removed and all job logs
    assertThat(helper.getHistoricBatch(batch)).isNull();
    assertThat(helper.getHistoricSeedJobLog(batch)).isEmpty();
    assertThat(helper.getHistoricMonitorJobLog(batch)).isEmpty();
    assertThat(helper.getHistoricBatchJobLog(batch)).isEmpty();
  }

  @Test
  public void testHistoricSeedJobIncidentDeletion() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startBeforeAsync("process1", 1, "user2", processDefinition.getId());

    Job seedJob = helper.getSeedJob(batch);
    rule.getManagementService().setJobRetries(seedJob.getId(), 0);

    rule.getManagementService().deleteBatch(batch.getId(), false);

    // when
    rule.getHistoryService().deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = rule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @Test
  public void testHistoricMonitorJobIncidentDeletion() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startTransitionAsync("process1", 1, "seq", processDefinition.getId());

    helper.completeSeedJobs(batch);
    Job monitorJob = helper.getMonitorJob(batch);
    rule.getManagementService().setJobRetries(monitorJob.getId(), 0);

    rule.getManagementService().deleteBatch(batch.getId(), false);

    // when
    rule.getHistoryService().deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = rule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @Test
  public void testHistoricBatchJobLogIncidentDeletion() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 3, "user1", processDefinition.getId());

    helper.completeSeedJobs(batch);
    helper.failExecutionJobs(batch, 3);

    rule.getManagementService().deleteBatch(batch.getId(), false);

    // when
    rule.getHistoryService().deleteHistoricBatch(batch.getId());

    // then the historic incident was deleted
    long historicIncidents = rule.getHistoryService().createHistoricIncidentQuery().count();
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
