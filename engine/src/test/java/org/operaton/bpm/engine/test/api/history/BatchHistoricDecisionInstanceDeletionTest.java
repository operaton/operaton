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
package org.operaton.bpm.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.impl.batch.BatchSeedJobHandler;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.BatchHelper;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class BatchHistoricDecisionInstanceDeletionTest {

  protected static String DECISION = "decision";
  protected static final Date TEST_DATE = new Date(1457326800000L);

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(rule);
  protected BatchDeletionHelper helper = new BatchDeletionHelper(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testRule);

  private int defaultBatchJobsPerSeed;
  private int defaultInvocationsPerBatchJob;
  private boolean defaultEnsureJobDueDateSet;

  protected ProcessEngineConfigurationImpl configuration;
  protected DecisionService decisionService;
  protected HistoryService historyService;

  protected List<String> decisionInstanceIds;

  @Parameterized.Parameter(0)
  public boolean ensureJobDueDateSet;

  @Parameterized.Parameter(1)
  public Date currentTime;

  @Parameterized.Parameters(name = "Job DueDate is set: {0}")
  public static Collection<Object[]> scenarios() {
    return Arrays.asList(new Object[][] {
      { false, null },
      { true, TEST_DATE }
    });
  }

  @Before
  public void setup() {
    ClockUtil.setCurrentTime(TEST_DATE);
    historyService = rule.getHistoryService();
    decisionService = rule.getDecisionService();
    decisionInstanceIds = new ArrayList<>();
  }

  @Before
  public void storeEngineSettings() {
    configuration = rule.getProcessEngineConfiguration();
    defaultEnsureJobDueDateSet = configuration.isEnsureJobDueDateNotNull();
    defaultBatchJobsPerSeed = configuration.getBatchJobsPerSeed();
    defaultInvocationsPerBatchJob = configuration.getInvocationsPerBatchJob();
    configuration.setEnsureJobDueDateNotNull(ensureJobDueDateSet);
  }

  @Before
  public void executeDecisionInstances() {
    testRule.deploy("org/operaton/bpm/engine/test/api/dmn/Example.dmn");

    VariableMap variables = Variables.createVariables()
        .putValue("status", "silver")
        .putValue("sum", 723);

    for (int i = 0; i < 10; i++) {
      decisionService.evaluateDecisionByKey(DECISION).variables(variables).evaluate();
    }

    List<HistoricDecisionInstance> decisionInstances = historyService.createHistoricDecisionInstanceQuery().list();
    for(HistoricDecisionInstance decisionInstance : decisionInstances) {
      decisionInstanceIds.add(decisionInstance.getId());
    }
  }

  @After
  public void restoreEngineSettings() {
    configuration.setBatchJobsPerSeed(defaultBatchJobsPerSeed);
    configuration.setInvocationsPerBatchJob(defaultInvocationsPerBatchJob);
    configuration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
    ClockUtil.reset();
  }

  @Test
  public void createBatchDeletionByIds() {
    // when
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);

    // then
    assertBatchCreated(batch, 10);
  }

  @Test
  public void createBatchDeletionByInvalidIds() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricDecisionInstancesAsync((List<String>) null, null))
      .isInstanceOf(BadUserRequestException.class);
  }

  @Test
  public void createBatchDeletionByQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    // when
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    // then
    assertBatchCreated(batch, 10);
  }

  @Test
  public void createBatchDeletionByInvalidQuery() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricDecisionInstancesAsync((HistoricDecisionInstanceQuery) null, null))
      .isInstanceOf(BadUserRequestException.class);
  }

  @Test
  public void createBatchDeletionByInvalidQueryByKey() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey("foo");

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricDecisionInstancesAsync(query, null))
      .isInstanceOf(BadUserRequestException.class);
  }

  @Test
  public void createBatchDeletionByIdsAndQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    // when
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    // then
    assertBatchCreated(batch, 10);
  }

  @Test
  public void createSeedJobByIds() {
    // when
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);

    // then there exists a seed job definition with the batch id as
    // configuration
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition).isNotNull();
    assertThat(seedJobDefinition.getDeploymentId()).isNotNull();
    assertThat(seedJobDefinition.getJobConfiguration()).isEqualTo(batch.getId());
    assertThat(seedJobDefinition.getJobType()).isEqualTo(BatchSeedJobHandler.TYPE);

    // and there exists a deletion job definition
    JobDefinition deletionJobDefinition = helper.getExecutionJobDefinition(batch);
    assertThat(deletionJobDefinition).isNotNull();
    assertThat(deletionJobDefinition.getJobType()).isEqualTo(Batch.TYPE_HISTORIC_DECISION_INSTANCE_DELETION);

    // and a seed job with no relation to a process or execution etc.
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob).isNotNull();
    assertThat(seedJob.getJobDefinitionId()).isEqualTo(seedJobDefinition.getId());
    assertThat(seedJob.getDuedate()).isEqualTo(currentTime);
    assertThat(seedJob.getDeploymentId()).isEqualTo(seedJobDefinition.getDeploymentId());
    assertThat(seedJob.getProcessDefinitionId()).isNull();
    assertThat(seedJob.getProcessDefinitionKey()).isNull();
    assertThat(seedJob.getProcessInstanceId()).isNull();
    assertThat(seedJob.getExecutionId()).isNull();

    // but no deletion jobs where created
    List<Job> deletionJobs = helper.getExecutionJobs(batch);
    assertThat(deletionJobs).isEmpty();
  }

  @Test
  public void createSeedJobByQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    // when
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    // then there exists a seed job definition with the batch id as
    // configuration
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition).isNotNull();
    assertThat(seedJobDefinition.getDeploymentId()).isNotNull();
    assertThat(seedJobDefinition.getJobConfiguration()).isEqualTo(batch.getId());
    assertThat(seedJobDefinition.getJobType()).isEqualTo(BatchSeedJobHandler.TYPE);

    // and there exists a deletion job definition
    JobDefinition deletionJobDefinition = helper.getExecutionJobDefinition(batch);
    assertThat(deletionJobDefinition).isNotNull();
    assertThat(deletionJobDefinition.getJobType()).isEqualTo(Batch.TYPE_HISTORIC_DECISION_INSTANCE_DELETION);

    // and a seed job with no relation to a process or execution etc.
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob).isNotNull();
    assertThat(seedJob.getJobDefinitionId()).isEqualTo(seedJobDefinition.getId());
    assertThat(seedJob.getDuedate()).isEqualTo(currentTime);
    assertThat(seedJob.getDeploymentId()).isEqualTo(seedJobDefinition.getDeploymentId());
    assertThat(seedJob.getProcessDefinitionId()).isNull();
    assertThat(seedJob.getProcessDefinitionKey()).isNull();
    assertThat(seedJob.getProcessInstanceId()).isNull();
    assertThat(seedJob.getExecutionId()).isNull();

    // but no deletion jobs where created
    List<Job> deletionJobs = helper.getExecutionJobs(batch);
    assertThat(deletionJobs).isEmpty();
  }

  @Test
  public void createSeedJobByIdsAndQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    // when
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    // then there exists a seed job definition with the batch id as
    // configuration
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition).isNotNull();
    assertThat(seedJobDefinition.getDeploymentId()).isNotNull();
    assertThat(seedJobDefinition.getJobConfiguration()).isEqualTo(batch.getId());
    assertThat(seedJobDefinition.getJobType()).isEqualTo(BatchSeedJobHandler.TYPE);

    // and there exists a deletion job definition
    JobDefinition deletionJobDefinition = helper.getExecutionJobDefinition(batch);
    assertThat(deletionJobDefinition).isNotNull();
    assertThat(deletionJobDefinition.getJobType()).isEqualTo(Batch.TYPE_HISTORIC_DECISION_INSTANCE_DELETION);

    // and a seed job with no relation to a process or execution etc.
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob).isNotNull();
    assertThat(seedJob.getJobDefinitionId()).isEqualTo(seedJobDefinition.getId());
    assertThat(seedJob.getDuedate()).isEqualTo(currentTime);
    assertThat(seedJob.getDeploymentId()).isEqualTo(seedJobDefinition.getDeploymentId());
    assertThat(seedJob.getProcessDefinitionId()).isNull();
    assertThat(seedJob.getProcessDefinitionKey()).isNull();
    assertThat(seedJob.getProcessInstanceId()).isNull();
    assertThat(seedJob.getExecutionId()).isNull();

    // but no deletion jobs where created
    List<Job> deletionJobs = helper.getExecutionJobs(batch);
    assertThat(deletionJobs).isEmpty();
  }

  @Test
  public void createDeletionJobsByIds() {
    // given
    rule.getProcessEngineConfiguration().setBatchJobsPerSeed(5);

    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);

    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    JobDefinition deletionJobDefinition = helper.getExecutionJobDefinition(batch);;

    // when
    helper.executeSeedJob(batch);

    // then
    List<Job> deletionJobs = helper.getJobsForDefinition(deletionJobDefinition);
    assertThat(deletionJobs).hasSize(5);

    for (Job deletionJob : deletionJobs) {
      assertThat(deletionJob.getJobDefinitionId()).isEqualTo(deletionJobDefinition.getId());
      assertThat(deletionJob.getDuedate()).isEqualTo(currentTime);
      assertThat(deletionJob.getProcessDefinitionId()).isNull();
      assertThat(deletionJob.getProcessDefinitionKey()).isNull();
      assertThat(deletionJob.getProcessInstanceId()).isNull();
      assertThat(deletionJob.getExecutionId()).isNull();
    }

    // and the seed job still exists
    Job seedJob = helper.getJobForDefinition(seedJobDefinition);
    assertThat(seedJob).isNotNull();
  }

  @Test
  public void createDeletionJobsByIdsInDifferentDeployments() {
    // given a second deployment and instances
    executeDecisionInstances();

    // assume
    List<DecisionDefinition> definitions = rule.getRepositoryService().createDecisionDefinitionQuery().orderByDecisionDefinitionVersion().asc().list();
    assertThat(definitions).hasSize(2);
    String deploymentIdOne = definitions.get(0).getDeploymentId();
    String deploymentIdTwo = definitions.get(1).getDeploymentId();

    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.getDeploymentId()).isEqualTo(deploymentIdOne);

    // when
    helper.executeSeedJob(batch);

    // then there is a second seed job with the same deployment id
    Job seedJobTwo = helper.getSeedJob(batch);
    assertThat(seedJobTwo).isNotNull();
    assertThat(seedJobTwo.getDeploymentId()).isEqualTo(seedJob.getDeploymentId());

    // when
    helper.executeSeedJob(batch);

    // then there is no seed job anymore and 10 deletion jobs for every deployment exist
    assertThat(helper.getSeedJob(batch)).isNull();
    List<Job> deletionJobs = helper.getExecutionJobs(batch);
    assertThat(deletionJobs).hasSize(20);
    assertThat(getJobCountByDeployment(deletionJobs, deploymentIdOne)).isEqualTo(10L);
    assertThat(getJobCountByDeployment(deletionJobs, deploymentIdTwo)).isEqualTo(10L);
  }

  @Test
  public void createDeletionJobsByIdsWithDeletedDeployment() {
    // given a second deployment and instances
    executeDecisionInstances();

    List<DecisionDefinition> definitions = rule.getRepositoryService().createDecisionDefinitionQuery().orderByDecisionDefinitionVersion().asc().list();
    assertThat(definitions).hasSize(2);
    String deploymentIdOne = definitions.get(0).getDeploymentId();
    String deploymentIdTwo = definitions.get(1).getDeploymentId();

    // ... and the second deployment is deleted
    rule.getRepositoryService().deleteDeployment(deploymentIdTwo);

    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.getDeploymentId()).isEqualTo(deploymentIdOne);

    // when
    helper.executeSeedJob(batch);

    // then there is a second seed job with the same deployment id
    Job seedJobTwo = helper.getSeedJob(batch);
    assertThat(seedJobTwo).isNotNull();
    assertThat(seedJobTwo.getDeploymentId()).isEqualTo(seedJob.getDeploymentId());

    // when
    helper.executeSeedJob(batch);

    // then there is no seed job anymore
    assertThat(helper.getSeedJob(batch)).isNull();

    // ...and 10 deletion jobs for the first deployment and 10 jobs for no deployment exist
    List<Job> deletionJobs = helper.getExecutionJobs(batch);
    assertThat(deletionJobs).hasSize(20);
    assertThat(getJobCountByDeployment(deletionJobs, deploymentIdOne)).isEqualTo(10L);
    assertThat(getJobCountByDeployment(deletionJobs, null)).isEqualTo(10L);

    // cleanup
    helper.executeJobs(batch);
  }

  @Test
  public void createDeletionJobsByQuery() {
    // given
    rule.getProcessEngineConfiguration().setBatchJobsPerSeed(5);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    JobDefinition deletionJobDefinition = helper.getExecutionJobDefinition(batch);;

    // when
    helper.executeSeedJob(batch);

    // then
    List<Job> deletionJobs = helper.getJobsForDefinition(deletionJobDefinition);
    assertThat(deletionJobs).hasSize(5);

    for (Job deletionJob : deletionJobs) {
      assertThat(deletionJob.getJobDefinitionId()).isEqualTo(deletionJobDefinition.getId());
      assertThat(deletionJob.getDuedate()).isEqualTo(currentTime);
      assertThat(deletionJob.getProcessDefinitionId()).isNull();
      assertThat(deletionJob.getProcessDefinitionKey()).isNull();
      assertThat(deletionJob.getProcessInstanceId()).isNull();
      assertThat(deletionJob.getExecutionId()).isNull();
    }

    // and the seed job still exists
    Job seedJob = helper.getJobForDefinition(seedJobDefinition);
    assertThat(seedJob).isNotNull();
  }

  @Test
  public void createDeletionJobsByIdsAndQuery() {
    // given
    rule.getProcessEngineConfiguration().setBatchJobsPerSeed(5);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    JobDefinition deletionJobDefinition = helper.getExecutionJobDefinition(batch);;

    // when
    helper.executeSeedJob(batch);

    // then
    List<Job> deletionJobs = helper.getJobsForDefinition(deletionJobDefinition);
    assertThat(deletionJobs).hasSize(5);

    for (Job deletionJob : deletionJobs) {
      assertThat(deletionJob.getJobDefinitionId()).isEqualTo(deletionJobDefinition.getId());
      assertThat(deletionJob.getDuedate()).isEqualTo(currentTime);
      assertThat(deletionJob.getProcessDefinitionId()).isNull();
      assertThat(deletionJob.getProcessDefinitionKey()).isNull();
      assertThat(deletionJob.getProcessInstanceId()).isNull();
      assertThat(deletionJob.getExecutionId()).isNull();
    }

    // and the seed job still exists
    Job seedJob = helper.getJobForDefinition(seedJobDefinition);
    assertThat(seedJob).isNotNull();
  }

  @Test
  public void createMonitorJobByIds() {
    // given
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);

    // when
    helper.completeSeedJobs(batch);

    // then the seed job definition still exists but the seed job is removed
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition).isNotNull();

    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob).isNull();

    // and a monitor job definition and job exists
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    assertThat(monitorJobDefinition).isNotNull();

    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob).isNotNull();
  }

  @Test
  public void createMonitorJobByQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    // when
    helper.completeSeedJobs(batch);

    // then the seed job definition still exists but the seed job is removed
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition).isNotNull();

    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob).isNull();

    // and a monitor job definition and job exists
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    assertThat(monitorJobDefinition).isNotNull();

    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob).isNotNull();
  }

  @Test
  public void createMonitorJobByIdsAndQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    // when
    helper.completeSeedJobs(batch);

    // then the seed job definition still exists but the seed job is removed
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition).isNotNull();

    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob).isNull();

    // and a monitor job definition and job exists
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    assertThat(monitorJobDefinition).isNotNull();

    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob).isNotNull();
  }

  @Test
  public void deleteInstancesByIds() {
    // given
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);

    helper.completeSeedJobs(batch);
    List<Job> deletionJobs = helper.getExecutionJobs(batch);

    // when
    for (Job deletionJob : deletionJobs) {
      helper.executeJob(deletionJob);
    }

    // then
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
  }

  @Test
  public void deleteInstancesByQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    helper.completeSeedJobs(batch);
    List<Job> deletionJobs = helper.getExecutionJobs(batch);

    // when
    for (Job deletionJob : deletionJobs) {
      helper.executeJob(deletionJob);
    }

    // then
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
  }

  @Test
  public void deleteInstancesByIdsAndQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    helper.completeSeedJobs(batch);
    List<Job> deletionJobs = helper.getExecutionJobs(batch);

    // when
    for (Job deletionJob : deletionJobs) {
      helper.executeJob(deletionJob);
    }

    // then
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
  }

  @Test
  public void shouldSetInvocationsPerBatchType() {
    // given
    configuration.getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_HISTORIC_DECISION_INSTANCE_DELETION, 42);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery()
        .decisionDefinitionKey(DECISION);

    // when
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    configuration.setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @Test
  public void shouldSetExecutionStartTimeInBatchAndHistory() {
    // given
    ClockUtil.setCurrentTime(TEST_DATE);
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch);

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    batch = rule.getManagementService().createBatchQuery().singleResult();

    assertThat(batch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
    assertThat(historicBatch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
  }

  protected void assertBatchCreated(Batch batch, int decisionInstanceCount) {
    assertThat(batch).isNotNull();
    assertThat(batch.getId()).isNotNull();
    assertThat(batch.getType()).isEqualTo(Batch.TYPE_HISTORIC_DECISION_INSTANCE_DELETION);
    assertThat(batch.getTotalJobs()).isEqualTo(decisionInstanceCount);
    assertThat(batch.getBatchJobsPerSeed()).isEqualTo(defaultBatchJobsPerSeed);
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(defaultInvocationsPerBatchJob);
  }

  protected long getJobCountByDeployment(List<Job> jobs, String deploymentId) {
    return jobs.stream().filter(j -> Objects.equals(deploymentId, j.getDeploymentId())).count();
  }

  class BatchDeletionHelper extends BatchHelper {

    public BatchDeletionHelper(ProcessEngineRule engineRule) {
      super(engineRule);
    }

    @Override
    public JobDefinition getExecutionJobDefinition(Batch batch) {
      return getManagementService().createJobDefinitionQuery()
          .jobDefinitionId(batch.getBatchJobDefinitionId())
          .jobType(Batch.TYPE_HISTORIC_DECISION_INSTANCE_DELETION)
          .singleResult();
    }
  }

}
