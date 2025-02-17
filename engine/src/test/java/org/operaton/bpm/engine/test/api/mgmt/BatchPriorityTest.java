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

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobPriorityProvider;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.runtime.migration.MigrationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class BatchPriorityTest {

  public static final long CUSTOM_PRIORITY = DefaultJobPriorityProvider.DEFAULT_PRIORITY + 10;

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
  protected long defaultBatchJobPriority;

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
    identityService = engineRule.getIdentityService();
  }

  @Before
  public void saveAndReduceBatchConfiguration() {
    ProcessEngineConfigurationImpl configuration = engineRule.getProcessEngineConfiguration();
    defaultBatchJobsPerSeed = configuration.getBatchJobsPerSeed();
    defaultBatchJobPriority = configuration.getBatchJobPriority();
    // reduce number of batch jobs per seed to not have to create a lot of instances
    configuration.setBatchJobsPerSeed(1);
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @After
  public void resetBatchJobsPerSeed() {
    ProcessEngineConfigurationImpl processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    processEngineConfiguration.setBatchJobsPerSeed(defaultBatchJobsPerSeed);
    processEngineConfiguration.setBatchJobPriority(defaultBatchJobPriority);
  }

  @Test
  public void seedJobShouldHaveDefaultPriority() {
    // when
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // then
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.getPriority()).isEqualTo(DefaultJobPriorityProvider.DEFAULT_PRIORITY);
  }

  @Test
  public void monitorJobShouldHaveDefaultPriority() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    helper.completeSeedJobs(batch);

    // then
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.getPriority()).isEqualTo(DefaultJobPriorityProvider.DEFAULT_PRIORITY);
  }

  @Test
  public void batchExecutionJobShouldHaveDefaultPriority() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    helper.completeSeedJobs(batch);

    // then
    Job executionJob = helper.getExecutionJobs(batch).get(0);
    assertThat(executionJob.getPriority()).isEqualTo(DefaultJobPriorityProvider.DEFAULT_PRIORITY);
  }

  @Test
  public void seedJobShouldGetPriorityFromProcessEngineConfiguration() {
    // given
    setBatchJobPriority(CUSTOM_PRIORITY);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // then
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void monitorJobShouldGetPriorityFromProcessEngineConfiguration() {
    // given
    setBatchJobPriority(CUSTOM_PRIORITY);
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    helper.completeSeedJobs(batch);

    // then
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void executionJobShouldGetPriorityFromProcessEngineConfiguration() {
    // given
    setBatchJobPriority(CUSTOM_PRIORITY);
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    helper.completeSeedJobs(batch);

    // then
    Job executionJob = helper.getExecutionJobs(batch).get(0);
    assertThat(executionJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void seedJobShouldGetPriorityFromOverridingJobDefinitionPriority() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(2);
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    managementService.setOverridingJobPriorityForJobDefinition(seedJobDefinition.getId(), CUSTOM_PRIORITY);

    // when
    helper.executeSeedJob(batch);

    // then
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void seedJobShouldGetPriorityFromOverridingJobDefinitionPriorityWithCascade() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);

    // when
    managementService.setOverridingJobPriorityForJobDefinition(seedJobDefinition.getId(), CUSTOM_PRIORITY, true);

    // then
    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void monitorJobShouldGetPriorityOverridingJobDefinitionPriority() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    managementService.setOverridingJobPriorityForJobDefinition(monitorJobDefinition.getId(), CUSTOM_PRIORITY);

    // when
    helper.completeSeedJobs(batch);

    // then
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void monitorJobShouldGetPriorityOverridingJobDefinitionPriorityWithCascade() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    helper.completeSeedJobs(batch);

    // when
    managementService.setOverridingJobPriorityForJobDefinition(monitorJobDefinition.getId(), CUSTOM_PRIORITY, true);

    // then
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void executionJobShouldGetPriorityFromOverridingJobDefinitionPriority() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    JobDefinition executionJobDefinition = helper.getExecutionJobDefinition(batch);
    managementService.setOverridingJobPriorityForJobDefinition(executionJobDefinition.getId(), CUSTOM_PRIORITY, true);

    // when
    helper.completeSeedJobs(batch);

    // then
    Job executionJob = helper.getExecutionJobs(batch).get(0);
    assertThat(executionJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  @Test
  public void executionJobShouldGetPriorityFromOverridingJobDefinitionPriorityWithCascade() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    JobDefinition executionJobDefinition = helper.getExecutionJobDefinition(batch);
    helper.completeSeedJobs(batch);

    // when
    managementService.setOverridingJobPriorityForJobDefinition(executionJobDefinition.getId(), CUSTOM_PRIORITY, true);

    // then
    Job executionJob = helper.getExecutionJobs(batch).get(0);
    assertThat(executionJob.getPriority()).isEqualTo(CUSTOM_PRIORITY);
  }

  protected void setBatchJobPriority(long priority) {
    engineRule.getProcessEngineConfiguration()
      .setBatchJobPriority(priority);
  }
}
