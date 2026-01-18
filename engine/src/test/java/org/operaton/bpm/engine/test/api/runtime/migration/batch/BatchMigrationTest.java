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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.batch.BatchSeedJobHandler;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateEvent;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateExecutionListener;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Parameterized
public class BatchMigrationTest {

  protected static final Date TEST_DATE = new Date(1457326800000L);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl configuration;
  RuntimeService runtimeService;
  ManagementService managementService;
  HistoryService historyService;

  int defaultBatchJobsPerSeed;
  int defaultInvocationsPerBatchJob;
  boolean defaultEnsureJobDueDateSet;

  @Parameter(0)
  public boolean ensureJobDueDateSet;

  @Parameter(1)
  public Date currentTime;

  @Parameters(name = "Job DueDate is set: {0}")
  public static Collection<Object[]> scenarios() {
    return Arrays.asList(new Object[][] {
      { false, null },
      { true, TEST_DATE }
    });
  }

  @BeforeEach
  void storeEngineSettings() {
    configuration = engineRule.getProcessEngineConfiguration();
    defaultBatchJobsPerSeed = configuration.getBatchJobsPerSeed();
    defaultInvocationsPerBatchJob = configuration.getInvocationsPerBatchJob();
    defaultEnsureJobDueDateSet = configuration.isEnsureJobDueDateNotNull();
    configuration.setEnsureJobDueDateNotNull(ensureJobDueDateSet);
  }

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @AfterEach
  void restoreEngineSettings() {
    configuration.setBatchJobsPerSeed(defaultBatchJobsPerSeed);
    configuration.setInvocationsPerBatchJob(defaultInvocationsPerBatchJob);
    configuration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }


  @TestTemplate
  void testNullMigrationPlan() {
    var migrationPlanExecutionBuilder = runtimeService.newMigration(null).processInstanceIds(List.of("process"));
    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("migration plan is null");
    }
  }

  @TestTemplate
  void testNullProcessInstanceIdsList() {
    ProcessDefinition testProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds((List<String>) null);

    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @TestTemplate
  void testProcessInstanceIdsListWithNullValue() {
    ProcessDefinition testProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(Arrays.asList("foo", null, "bar"));

    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids contains null value");
    }
  }

  @TestTemplate
  void testEmptyProcessInstanceIdsList() {
    ProcessDefinition testProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(emptyList());

    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @TestTemplate
  void testNullProcessInstanceIdsArray() {
    ProcessDefinition testProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds((String[]) null);

    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @TestTemplate
  void testProcessInstanceIdsArrayWithNullValue() {
    ProcessDefinition testProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds("foo", null, "bar");

    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids contains null value");
    }
  }

  @TestTemplate
  void testNullProcessInstanceQuery() {
    ProcessDefinition testProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceQuery(null);

    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @TestTemplate
  void testEmptyProcessInstanceQuery() {
    ProcessDefinition testProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstanceQuery emptyProcessInstanceQuery = runtimeService.createProcessInstanceQuery();
    assertThat(emptyProcessInstanceQuery.count()).isZero();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceQuery(emptyProcessInstanceQuery);

    try {
      migrationPlanExecutionBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @TestTemplate
  void testBatchCreation() {
    // when
    Batch batch = helper.migrateProcessInstancesAsync(15);

    // then a batch is created
    assertBatchCreated(batch, 15);
  }

  @TestTemplate
  void testSeedJobCreation() {
    ClockUtil.setCurrentTime(TEST_DATE);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(10);

    // then there exists a seed job definition with the batch id as configuration
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertThat(seedJobDefinition).isNotNull();
    assertThat(seedJobDefinition.getJobConfiguration()).isEqualTo(batch.getId());
    assertThat(seedJobDefinition.getJobType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(seedJobDefinition.getDeploymentId()).isEqualTo(helper.sourceProcessDefinition.getDeploymentId());

    // and there exists a migration job definition
    JobDefinition migrationJobDefinition = helper.getExecutionJobDefinition(batch);
    assertThat(migrationJobDefinition).isNotNull();
    assertThat(migrationJobDefinition.getJobType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MIGRATION);

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

    // but no migration jobs where created
    List<Job> migrationJobs = helper.getExecutionJobs(batch);
    assertThat(migrationJobs).isEmpty();
  }

  @TestTemplate
  void testMigrationJobsCreation() {
    ClockUtil.setCurrentTime(TEST_DATE);

    // reduce number of batch jobs per seed to not have to create a lot of instances
    engineRule.getProcessEngineConfiguration().setBatchJobsPerSeed(10);

    Batch batch = helper.migrateProcessInstancesAsync(20);
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    JobDefinition migrationJobDefinition = helper.getExecutionJobDefinition(batch);
    String sourceDeploymentId = helper.getSourceProcessDefinition().getDeploymentId();

    // when
    helper.executeSeedJob(batch);

    // then there exist migration jobs
    List<Job> migrationJobs = helper.getJobsForDefinition(migrationJobDefinition);
    assertThat(migrationJobs).hasSize(10);

    for (Job migrationJob : migrationJobs) {
      assertThat(migrationJob.getJobDefinitionId()).isEqualTo(migrationJobDefinition.getId());
      assertThat(migrationJob.getDuedate()).isEqualTo(currentTime);
      assertThat(migrationJob.getDeploymentId()).isEqualTo(sourceDeploymentId);
      assertThat(migrationJob.getProcessDefinitionId()).isNull();
      assertThat(migrationJob.getProcessDefinitionKey()).isNull();
      assertThat(migrationJob.getProcessInstanceId()).isNull();
      assertThat(migrationJob.getExecutionId()).isNull();
    }

    // and the seed job still exists
    Job seedJob = helper.getJobForDefinition(seedJobDefinition);
    assertThat(seedJob).isNotNull();
  }

  @TestTemplate
  void testMonitorJobCreation() {
    Batch batch = helper.migrateProcessInstancesAsync(10);

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

  @TestTemplate
  void testMigrationJobsExecution() {
    Batch batch = helper.migrateProcessInstancesAsync(10);
    helper.completeSeedJobs(batch);
    List<Job> migrationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job migrationJob : migrationJobs) {
      helper.executeJob(migrationJob);
    }

    // then all process instances where migrated
    assertThat(helper.countSourceProcessInstances()).isZero();
    assertThat(helper.countTargetProcessInstances()).isEqualTo(10);

    // and the no migration jobs exist
    assertThat(helper.getExecutionJobs(batch)).isEmpty();

    // but a monitor job exists
    assertThat(helper.getMonitorJob(batch)).isNotNull();
  }

  @TestTemplate
  void testMigrationJobsExecutionByJobExecutorWithAuthorizationEnabledAndTenant() {
    ProcessEngineConfigurationImpl processEngineConfiguration = engineRule.getProcessEngineConfiguration();

    processEngineConfiguration.setAuthorizationEnabled(true);

    try {
      Batch batch = helper.migrateProcessInstancesAsyncForTenant(10, "someTenantId");
      helper.completeSeedJobs(batch);

      testRule.waitForJobExecutorToProcessAllJobs();

      // then all process instances were migrated
      assertThat(helper.countSourceProcessInstances()).isZero();
      assertThat(helper.countTargetProcessInstances()).isEqualTo(10);

    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
    }
  }

  @TestTemplate
  void testNumberOfJobsCreatedBySeedJobPerInvocation() {
    // reduce number of batch jobs per seed to not have to create a lot of instances
    int batchJobsPerSeed = 10;
    engineRule.getProcessEngineConfiguration().setBatchJobsPerSeed(10);

    Batch batch = helper.migrateProcessInstancesAsync(batchJobsPerSeed * 2 + 4);

    // when
    helper.executeSeedJob(batch);

    // then the default number of jobs was created
    assertThat(helper.getExecutionJobs(batch)).hasSize(batch.getBatchJobsPerSeed());

    // when the seed job is executed a second time
    helper.executeSeedJob(batch);

    // then the same amount of jobs was created
    assertThat(helper.getExecutionJobs(batch)).hasSize(2 * batch.getBatchJobsPerSeed());

    // when the seed job is executed a third time
    helper.executeSeedJob(batch);

    // then the all jobs where created
    assertThat(helper.getExecutionJobs(batch)).hasSize(2 * batch.getBatchJobsPerSeed() + 4);

    // and the seed job is removed
    assertThat(helper.getSeedJob(batch)).isNull();
  }

  @TestTemplate
  void testDefaultBatchConfiguration() {
    ProcessEngineConfigurationImpl cfg = engineRule.getProcessEngineConfiguration();
    assertThat(cfg.getBatchJobsPerSeed()).isEqualTo(100);
    assertThat(cfg.getInvocationsPerBatchJob()).isEqualTo(1);
    assertThat(cfg.getBatchPollTime()).isEqualTo(30);
  }

  @TestTemplate
  void testCustomNumberOfJobsCreateBySeedJob() {
    ProcessEngineConfigurationImpl cfg = engineRule.getProcessEngineConfiguration();
    cfg.setBatchJobsPerSeed(2);
    cfg.setInvocationsPerBatchJob(5);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(20);

    // then the configuration was saved in the batch job
    assertThat(batch.getBatchJobsPerSeed()).isEqualTo(2);
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(5);

    // and the size was correctly calculated
    assertThat(batch.getTotalJobs()).isEqualTo(4);

    // when the seed job is executed
    helper.executeSeedJob(batch);

    // then there exist the first batch of migration jobs
    assertThat(helper.getExecutionJobs(batch)).hasSize(2);

    // when the seed job is executed a second time
    helper.executeSeedJob(batch);

    // then the full batch of migration jobs exist
    assertThat(helper.getExecutionJobs(batch)).hasSize(4);

    // and the seed job is removed
    assertThat(helper.getSeedJob(batch)).isNull();
  }

  @TestTemplate
  void testMonitorJobPollingForCompletion() {
    ClockUtil.setCurrentTime(TEST_DATE);

    Batch batch = helper.migrateProcessInstancesAsync(10);

    // when the seed job creates the monitor job
    Date createDate = TEST_DATE;
    helper.completeSeedJobs(batch);

    // then the monitor job has a no due date set
    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob).isNotNull();
    assertThat(monitorJob.getDuedate()).isEqualTo(currentTime);

    // when the monitor job is executed
    helper.executeMonitorJob(batch);

    // then the monitor job has a due date of the default batch poll time
    monitorJob = helper.getMonitorJob(batch);
    Date dueDate = helper.addSeconds(createDate, 30);
    assertThat(monitorJob.getDuedate()).isEqualTo(dueDate);
  }

  @TestTemplate
  void testMonitorJobRemovesBatchAfterCompletion() {
    Batch batch = helper.migrateProcessInstancesAsync(10);
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    // when
    helper.executeMonitorJob(batch);

    // then the batch was completed and removed
    assertThat(managementService.createBatchQuery().count()).isZero();

    // and the seed jobs was removed
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @TestTemplate
  void testBatchDeletionWithCascade() {
    Batch batch = helper.migrateProcessInstancesAsync(10);
    helper.completeSeedJobs(batch);

    // when
    managementService.deleteBatch(batch.getId(), true);

    // then the batch was deleted
    assertThat(managementService.createBatchQuery().count()).isZero();

    // and the seed and migration job definition were deleted
    assertThat(managementService.createJobDefinitionQuery().count()).isZero();

    // and the seed job and migration jobs were deleted
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @TestTemplate
  void testBatchDeletionWithoutCascade() {
    Batch batch = helper.migrateProcessInstancesAsync(10);
    helper.completeSeedJobs(batch);

    // when
    managementService.deleteBatch(batch.getId(), false);

    // then the batch was deleted
    assertThat(managementService.createBatchQuery().count()).isZero();

    // and the seed and migration job definition were deleted
    assertThat(managementService.createJobDefinitionQuery().count()).isZero();

    // and the seed job and migration jobs were deleted
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @TestTemplate
  void testBatchWithFailedSeedJobDeletionWithCascade() {
    Batch batch = helper.migrateProcessInstancesAsync(2);

    // create incident
    Job seedJob = helper.getSeedJob(batch);
    managementService.setJobRetries(seedJob.getId(), 0);

    // when
    managementService.deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @TestTemplate
  void testBatchWithFailedMigrationJobDeletionWithCascade() {
    Batch batch = helper.migrateProcessInstancesAsync(2);
    helper.completeSeedJobs(batch);

    // create incidents
    List<Job> migrationJobs = helper.getExecutionJobs(batch);
    for (Job migrationJob : migrationJobs) {
      managementService.setJobRetries(migrationJob.getId(), 0);
    }

    // when
    managementService.deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @TestTemplate
  void testBatchWithFailedMonitorJobDeletionWithCascade() {
    Batch batch = helper.migrateProcessInstancesAsync(2);
    helper.completeSeedJobs(batch);

    // create incident
    Job monitorJob = helper.getMonitorJob(batch);
    managementService.setJobRetries(monitorJob.getId(), 0);

    // when
    managementService.deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = historyService.createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isZero();
  }

  @TestTemplate
  void testBatchExecutionFailureWithMissingProcessInstance() {
    Batch batch = helper.migrateProcessInstancesAsync(2);
    helper.completeSeedJobs(batch);

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String deletedProcessInstanceId = processInstances.get(0).getId();

    // when
    runtimeService.deleteProcessInstance(deletedProcessInstanceId, "test");
    helper.executeJobs(batch);

    // then the remaining process instance was migrated
    assertThat(helper.countSourceProcessInstances()).isZero();
    assertThat(helper.countTargetProcessInstances()).isOne();

    // and one batch job failed and has 2 retries left
    List<Job> migrationJobs = helper.getExecutionJobs(batch);
    assertThat(migrationJobs).hasSize(1);

    Job failedJob = migrationJobs.get(0);
    assertThat(failedJob.getRetries()).isEqualTo(2);
    assertThat(failedJob.getExceptionMessage()).startsWith("ENGINE-23003");
    assertThat(failedJob.getExceptionMessage()).contains("Process instance '%s' cannot be migrated".formatted(deletedProcessInstanceId));
  }

  @TestTemplate
  void testBatchCreationWithProcessInstanceQuery() {
    int processInstanceCount = 15;

    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    for (int i = 0; i < processInstanceCount; i++) {
      runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    }

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    assertThat(sourceProcessInstanceQuery.count()).isEqualTo(processInstanceCount);

    // when
    Batch batch = runtimeService.newMigration(migrationPlan)
      .processInstanceQuery(sourceProcessInstanceQuery)
      .executeAsync();

    // then a batch is created
    assertBatchCreated(batch, processInstanceCount);
  }

  @TestTemplate
  void testBatchCreationWithOverlappingProcessInstanceIdsAndQuery() {
    int processInstanceCount = 15;

    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    List<String> processInstanceIds = new ArrayList<>();
    for (int i = 0; i < processInstanceCount; i++) {
      processInstanceIds.add(
        runtimeService.startProcessInstanceById(sourceProcessDefinition.getId()).getId()
      );
    }

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    assertThat(sourceProcessInstanceQuery.count()).isEqualTo(processInstanceCount);

    // when
    Batch batch = runtimeService.newMigration(migrationPlan)
      .processInstanceIds(processInstanceIds)
      .processInstanceQuery(sourceProcessInstanceQuery)
      .executeAsync();

    // then a batch is created
    assertBatchCreated(batch, processInstanceCount);
  }

  @TestTemplate
  void testListenerInvocationForNewlyCreatedScope() {
    // given
    DelegateEvent.clearEvents();

    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, DelegateExecutionListener.class.getName())
      .done()
    );

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Batch batch = engineRule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .executeAsync();
    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    List<DelegateEvent> recordedEvents = DelegateEvent.getEvents();
    assertThat(recordedEvents).hasSize(1);

    DelegateEvent event = recordedEvents.get(0);
    assertThat(event.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    assertThat(event.getCurrentActivityId()).isEqualTo("subProcess");

    DelegateEvent.clearEvents();
  }

  @TestTemplate
  void testSkipListenerInvocationForNewlyCreatedScope() {
    // given
    DelegateEvent.clearEvents();

    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, DelegateExecutionListener.class.getName())
      .done()
    );

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Batch batch = engineRule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .skipCustomListeners()
      .executeAsync();
    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    assertThat(DelegateEvent.getEvents()).isEmpty();
  }

  @TestTemplate
  void testIoMappingInvocationForNewlyCreatedScope() {
    // given
    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonInputParameter("foo", "bar")
      .done()
    );

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Batch batch = engineRule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .executeAsync();
    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    VariableInstance inputVariable = engineRule.getRuntimeService().createVariableInstanceQuery().singleResult();
    assertThat(inputVariable).isNotNull();
    assertThat(inputVariable.getName()).isEqualTo("foo");
    assertThat(inputVariable.getValue()).isEqualTo("bar");

    ActivityInstance activityInstance = engineRule.getRuntimeService().getActivityInstance(processInstance.getId());
    assertThat(inputVariable.getActivityInstanceId()).isEqualTo(activityInstance.getActivityInstances("subProcess")[0].getId());
  }

  @TestTemplate
  void testSkipIoMappingInvocationForNewlyCreatedScope() {
 // given
    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonInputParameter("foo", "bar")
      .done()
    );

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Batch batch = engineRule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .skipIoMappings()
      .executeAsync();
    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    assertThat(engineRule.getRuntimeService().createVariableInstanceQuery().count()).isZero();
  }

  @TestTemplate
  void testUpdateEventTrigger() {
    // given
    String newMessageName = "newMessage";

    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_RECEIVE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(modify(ProcessModels.ONE_RECEIVE_TASK_PROCESS)
      .renameMessage("Message", newMessageName)
    );

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .updateEventTriggers()
      .build();

    Batch batch = runtimeService.newMigration(migrationPlan)
      .processInstanceIds(Collections.singletonList(processInstance.getId()))
      .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then the message event subscription's event name was changed
    EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(eventSubscription.getEventName()).isEqualTo(newMessageName);
  }

  @TestTemplate
  void testDeleteBatchJobManually() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(1);
    helper.completeSeedJobs(batch);

    JobEntity migrationJob = (JobEntity) helper.getExecutionJobs(batch).get(0);
    String byteArrayId = migrationJob.getJobHandlerConfigurationRaw();

    ByteArrayEntity byteArrayEntity = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired()
      .execute(new GetByteArrayCommand(byteArrayId));
    assertThat(byteArrayEntity).isNotNull();

    // when
    managementService.deleteJob(migrationJob.getId());

    // then
    byteArrayEntity = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired()
      .execute(new GetByteArrayCommand(byteArrayId));
    assertThat(byteArrayEntity).isNull();
  }

  @TestTemplate
  void testMigrateWithVarargsArray() {
    ProcessDefinition sourceDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceById(sourceDefinition.getId());
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceById(sourceDefinition.getId());

    // when
    Batch batch = runtimeService.newMigration(migrationPlan)
      .processInstanceIds(processInstance1.getId(), processInstance2.getId())
      .executeAsync();

    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);
    helper.executeMonitorJob(batch);

    // then
    assertThat(runtimeService.createProcessInstanceQuery()
        .processDefinitionId(targetDefinition.getId()).count()).isEqualTo(2);
  }

  @TestTemplate
  void shouldSetInvocationsPerBatchType() {
    // given
    configuration.getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_PROCESS_INSTANCE_MIGRATION, 42);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(15);

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    configuration.setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldSetExecutionStartTimeInBatchAndHistory() {
    // given
    ClockUtil.setCurrentTime(TEST_DATE);
    Batch batch = helper.migrateProcessInstancesAsync(15);
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch);

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    batch = managementService.createBatchQuery().singleResult();

    assertThat(batch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
    assertThat(historicBatch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
  }

  protected void assertBatchCreated(Batch batch, int processInstanceCount) {
    assertThat(batch).isNotNull();
    assertThat(batch.getId()).isNotNull();
    assertThat(batch.getType()).isEqualTo("instance-migration");
    assertThat(batch.getTotalJobs()).isEqualTo(processInstanceCount);
    assertThat(batch.getBatchJobsPerSeed()).isEqualTo(defaultBatchJobsPerSeed);
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(defaultInvocationsPerBatchJob);
  }

  public class GetByteArrayCommand implements Command<ByteArrayEntity> {

    protected String byteArrayId;

    public GetByteArrayCommand(String byteArrayId) {
      this.byteArrayId = byteArrayId;
    }

    @Override
    public ByteArrayEntity execute(CommandContext commandContext) {
      return (ByteArrayEntity) commandContext.getDbEntityManager()
        .selectOne("selectByteArray", byteArrayId);
    }

  }

}
