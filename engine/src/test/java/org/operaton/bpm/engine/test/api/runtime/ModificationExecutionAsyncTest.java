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

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.batch.BatchSeedJobHandler;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateEvent;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateExecutionListener;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;

import java.util.*;

import org.assertj.core.api.Assertions;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ModificationExecutionAsyncTest {

  protected static final Date START_DATE = new Date(1457326800000L);

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(rule);
  protected BatchModificationHelper helper = new BatchModificationHelper(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testRule);

  protected ProcessEngineConfigurationImpl configuration;
  protected RuntimeService runtimeService;
  protected HistoryService historyService;

  protected BpmnModelInstance instance;

  private int defaultBatchJobsPerSeed;
  private int defaultInvocationsPerBatchJob;
  private boolean defaultEnsureJobDueDateSet;

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
  public void initServices() {
    runtimeService = rule.getRuntimeService();
    historyService = rule.getHistoryService();
  }

  @Before
  public void setClock() {
    ClockUtil.setCurrentTime(START_DATE);
  }

  @Before
  public void storeEngineSettings() {
    configuration = rule.getProcessEngineConfiguration();
    defaultBatchJobsPerSeed = configuration.getBatchJobsPerSeed();
    defaultInvocationsPerBatchJob = configuration.getInvocationsPerBatchJob();
    defaultEnsureJobDueDateSet = configuration.isEnsureJobDueDateNotNull();
    configuration.setEnsureJobDueDateNotNull(ensureJobDueDateSet);
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

  @After
  public void resetClock() {
    ClockUtil.reset();
  }

  @After
  public void restoreEngineSettings() {
    configuration.setBatchJobsPerSeed(defaultBatchJobsPerSeed);
    configuration.setInvocationsPerBatchJob(defaultInvocationsPerBatchJob);
    configuration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @After
  public void removeInstanceIds() {
    helper.currentProcessInstances = new ArrayList<>();
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @Test
  public void createBatchModification() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> processInstanceIds = helper.startInstances("process1", 2);

    Batch batch = runtimeService.createModification(processDefinition.getId()).startAfterActivity("user2").processInstanceIds(processInstanceIds).executeAsync();

    assertBatchCreated(batch, 2);
  }

  @Test
  public void createModificationWithNullProcessInstanceIdsListAsync() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceIds((List<String>) null);

    try {
      modificationBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Process instance ids is empty");
    }
  }

  @Test
  public void createModificationWithNullProcessDefinitionId() {
    try {
      runtimeService.createModification(null);
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("processDefinitionId is null");
    }
  }


  @Test
  public void createModificationUsingProcessInstanceIdsListWithNullValueAsync() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceIds(Arrays.asList("foo", null, "bar"));

    try {
      modificationBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Process instance ids contains null value");
    }
  }

  @Test
  public void createModificationWithEmptyProcessInstanceIdsListAsync() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceIds(Collections.<String> emptyList());
    try {
      modificationBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Process instance ids is empty");
    }
  }

  @Test
  public void createModificationWithNullProcessInstanceIdsArrayAsync() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceIds((String[]) null);

    try {
      modificationBuilder.executeAsync();
      fail("Should not be able to modify");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Process instance ids is empty");
    }
  }

  @Test
  public void createModificationUsingProcessInstanceIdsArrayWithNullValueAsync() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").cancelAllForActivity("user1").processInstanceIds("foo", null, "bar");

    try {
      modificationBuilder.executeAsync();
      fail("Should not be able to modify");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Process instance ids contains null value");
    }
  }

  @Test
  public void testNullProcessInstanceQueryAsync() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceQuery(null);

    try {
      modificationBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Process instance ids is empty");
    }
  }

  @Test
  public void testNullHistoricProcessInstanceQueryAsync() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").historicProcessInstanceQuery(null);

    try {
      modificationBuilder.executeAsync();
      fail("Should not succeed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Process instance ids is empty");
    }
  }

  @Test
  public void createModificationWithNonExistingProcessDefinitionId() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 2);
    var modificationBuilder = runtimeService.createModification("foo").cancelAllForActivity("activityId").processInstanceIds(processInstanceIds);
    try {
      modificationBuilder.executeAsync();
      fail("Should not succed");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("processDefinition is null");
    }
  }

  @Test
  public void createSeedJob() {
    // when
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 3, "user1", processDefinition.getId());

    // then there exists a seed job definition with the batch id as
    // configuration
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertNotNull(seedJobDefinition);
    assertThat(seedJobDefinition.getJobConfiguration()).isEqualTo(batch.getId());
    assertThat(seedJobDefinition.getJobType()).isEqualTo(BatchSeedJobHandler.TYPE);
    assertThat(processDefinition.getDeploymentId()).isEqualTo(seedJobDefinition.getDeploymentId());

    // and there exists a modification job definition
    JobDefinition modificationJobDefinition = helper.getExecutionJobDefinition(batch);
    assertNotNull(modificationJobDefinition);
    assertThat(modificationJobDefinition.getJobType()).isEqualTo(Batch.TYPE_PROCESS_INSTANCE_MODIFICATION);

    // and a seed job with no relation to a process or execution etc.
    Job seedJob = helper.getSeedJob(batch);
    assertNotNull(seedJob);
    assertThat(seedJob.getJobDefinitionId()).isEqualTo(seedJobDefinition.getId());
    assertThat(seedJob.getDuedate()).isEqualTo(currentTime);
    assertThat(seedJob.getDeploymentId()).isEqualTo(seedJobDefinition.getDeploymentId());
    assertNull(seedJob.getProcessDefinitionId());
    assertNull(seedJob.getProcessDefinitionKey());
    assertNull(seedJob.getProcessInstanceId());
    assertNull(seedJob.getExecutionId());

    // but no modification jobs where created
    List<Job> modificationJobs = helper.getExecutionJobs(batch);
    assertThat(modificationJobs.size()).isEqualTo(0);
  }

  @Test
  public void createModificationJobs() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    rule.getProcessEngineConfiguration().setBatchJobsPerSeed(10);
    Batch batch = helper.startAfterAsync("process1", 20, "user1", processDefinition.getId());
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    JobDefinition modificationJobDefinition = helper.getExecutionJobDefinition(batch);;

    helper.executeSeedJob(batch);

    List<Job> modificationJobs = helper.getJobsForDefinition(modificationJobDefinition);
    assertThat(modificationJobs.size()).isEqualTo(10);

    for (Job modificationJob : modificationJobs) {
      assertThat(modificationJob.getJobDefinitionId()).isEqualTo(modificationJobDefinition.getId());
      assertThat(modificationJob.getDuedate()).isEqualTo(currentTime);
      assertNull(modificationJob.getProcessDefinitionId());
      assertNull(modificationJob.getProcessDefinitionKey());
      assertNull(modificationJob.getProcessInstanceId());
      assertNull(modificationJob.getExecutionId());
    }

    // and the seed job still exists
    Job seedJob = helper.getJobForDefinition(seedJobDefinition);
    assertNotNull(seedJob);
  }

  @Test
  public void createMonitorJob() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 10, "user1", processDefinition.getId());

    // when
    helper.completeSeedJobs(batch);

    // then the seed job definition still exists but the seed job is removed
    JobDefinition seedJobDefinition = helper.getSeedJobDefinition(batch);
    assertNotNull(seedJobDefinition);

    Job seedJob = helper.getSeedJob(batch);
    assertNull(seedJob);

    // and a monitor job definition and job exists
    JobDefinition monitorJobDefinition = helper.getMonitorJobDefinition(batch);
    assertNotNull(monitorJobDefinition);

    Job monitorJob = helper.getMonitorJob(batch);
    assertNotNull(monitorJob);
  }

  @Test
  public void executeModificationJobsForStartAfter() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    Batch batch = helper.startAfterAsync("process1", 10, "user1", processDefinition.getId());
    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    for (String processInstanceId : helper.currentProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .activity("user1")
          .activity("user2")
          .done());
    }

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void executeModificationJobsForStartBefore() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    Batch batch = helper.startBeforeAsync("process1", 10, "user2", processDefinition.getId());
    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    for (String processInstanceId : helper.currentProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .activity("user1")
          .activity("user2")
          .done());
    }

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void executeModificationJobsForStartTransition() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    Batch batch = helper.startTransitionAsync("process1", 10, "seq", processDefinition.getId());
    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    for (String processInstanceId : helper.currentProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .activity("user1")
          .activity("user2")
          .done());
    }

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void executeModificationJobsForCancelAll() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.cancelAllAsync("process1", 10, "user1", processDefinition.getId());
    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    for (String processInstanceId : helper.currentProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNull(updatedTree);
    }

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void executeModificationJobsForStartAfterAndCancelAll() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);
    List<String> instances = helper.startInstances("process1", 10);

    Batch batch = runtimeService
        .createModification(processDefinition.getId())
        .startAfterActivity("user1")
        .cancelAllForActivity("user1")
        .processInstanceIds(instances)
        .executeAsync();

    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    for (String processInstanceId : helper.currentProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .activity("user2")
          .done());
    }

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void executeModificationJobsForStartBeforeAndCancelAll() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> instances = helper.startInstances("process1", 10);

    Batch batch = runtimeService
        .createModification(processDefinition.getId())
        .startBeforeActivity("user1")
        .cancelAllForActivity("user1")
        .processInstanceIds(instances)
        .executeAsync();

    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    for (String processInstanceId : helper.currentProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNull(updatedTree);
    }

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void executeModificationJobsForStartTransitionAndCancelAll() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> instances = helper.startInstances("process1", 10);

    Batch batch = runtimeService
        .createModification(processDefinition.getId())
        .startTransition("seq")
        .cancelAllForActivity("user1")
        .processInstanceIds(instances)
        .executeAsync();

    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    for (String processInstanceId : helper.currentProcessInstances) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .activity("user2")
          .done());
    }

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void executeModificationJobsForProcessInstancesWithDifferentStates() {

    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 1);
    Task task = rule.getTaskService().createTaskQuery().singleResult();
    rule.getTaskService().complete(task.getId());

    List<String> anotherProcessInstanceIds = helper.startInstances("process1", 1);
    processInstanceIds.addAll(anotherProcessInstanceIds);

    Batch batch = runtimeService.createModification(processDefinition.getId()).startBeforeActivity("user2").processInstanceIds(processInstanceIds).executeAsync();

    helper.completeSeedJobs(batch);
    List<Job> modificationJobs = helper.getExecutionJobs(batch);

    // when
    for (Job modificationJob : modificationJobs) {
      helper.executeJob(modificationJob);
    }

    // then all process instances where modified
    ActivityInstance updatedTree = null;
    String processInstanceId = processInstanceIds.get(0);
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processDefinition.getId()).activity("user2").activity("user2").done());

    processInstanceId = processInstanceIds.get(1);
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processDefinition.getId()).activity("user1").activity("user2").done());

    // and the no modification jobs exist
    assertThat(helper.getExecutionJobs(batch).size()).isEqualTo(0);

    // but a monitor job exists
    assertNotNull(helper.getMonitorJob(batch));
  }

  @Test
  public void testMonitorJobPollingForCompletion() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 3, "user1", processDefinition.getId());

    // when the seed job creates the monitor job
    Date createDate = START_DATE;
    helper.completeSeedJobs(batch);

    // then the monitor job has a no due date set
    Job monitorJob = helper.getMonitorJob(batch);
    assertNotNull(monitorJob);
    assertThat(monitorJob.getDuedate()).isEqualTo(currentTime);

    // when the monitor job is executed
    helper.executeMonitorJob(batch);

    // then the monitor job has a due date of the default batch poll time
    monitorJob = helper.getMonitorJob(batch);
    Date dueDate = helper.addSeconds(createDate, 30);
    assertThat(monitorJob.getDuedate()).isEqualTo(dueDate);
  }

  @Test
  public void testMonitorJobRemovesBatchAfterCompletion() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startBeforeAsync("process1", 10, "user2", processDefinition.getId());
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    // when
    helper.executeMonitorJob(batch);

    // then the batch was completed and removed
    assertThat(rule.getManagementService().createBatchQuery().count()).isEqualTo(0);

    // and the seed jobs was removed
    assertThat(rule.getManagementService().createJobQuery().count()).isEqualTo(0);
  }

  @Test
  public void testBatchDeletionWithCascade() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startTransitionAsync("process1", 10, "seq", processDefinition.getId());
    helper.completeSeedJobs(batch);

    // when
    rule.getManagementService().deleteBatch(batch.getId(), true);

    // then the batch was deleted
    assertThat(rule.getManagementService().createBatchQuery().count()).isEqualTo(0);

    // and the seed and modification job definition were deleted
    assertThat(rule.getManagementService().createJobDefinitionQuery().count()).isEqualTo(0);

    // and the seed job and modification jobs were deleted
    assertThat(rule.getManagementService().createJobQuery().count()).isEqualTo(0);
  }

  @Test
  public void testBatchDeletionWithoutCascade() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startBeforeAsync("process1", 10, "user2", processDefinition.getId());
    helper.completeSeedJobs(batch);

    // when
    rule.getManagementService().deleteBatch(batch.getId(), false);

    // then the batch was deleted
    assertThat(rule.getManagementService().createBatchQuery().count()).isEqualTo(0);

    // and the seed and modification job definition were deleted
    assertThat(rule.getManagementService().createJobDefinitionQuery().count()).isEqualTo(0);

    // and the seed job and modification jobs were deleted
    assertThat(rule.getManagementService().createJobQuery().count()).isEqualTo(0);
  }

  @Test
  public void testBatchWithFailedSeedJobDeletionWithCascade() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.cancelAllAsync("process1", 2, "user1", processDefinition.getId());

    // create incident
    Job seedJob = helper.getSeedJob(batch);
    rule.getManagementService().setJobRetries(seedJob.getId(), 0);

    // when
    rule.getManagementService().deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = rule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isEqualTo(0);
  }

  @Test
  public void testBatchWithFailedModificationJobDeletionWithCascade() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 2, "user1", processDefinition.getId());
    helper.completeSeedJobs(batch);

    // create incidents
    List<Job> modificationJobs = helper.getExecutionJobs(batch);
    for (Job modificationJob : modificationJobs) {
      rule.getManagementService().setJobRetries(modificationJob.getId(), 0);
    }

    // when
    rule.getManagementService().deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = rule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isEqualTo(0);
  }

  @Test
  public void testBatchWithFailedMonitorJobDeletionWithCascade() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startBeforeAsync("process1", 2, "user2", processDefinition.getId());
    helper.completeSeedJobs(batch);

    // create incident
    Job monitorJob = helper.getMonitorJob(batch);
    rule.getManagementService().setJobRetries(monitorJob.getId(), 0);

    // when
    rule.getManagementService().deleteBatch(batch.getId(), true);

    // then the no historic incidents exists
    long historicIncidents = rule.getHistoryService().createHistoricIncidentQuery().count();
    assertThat(historicIncidents).isEqualTo(0);
  }

  @Test
  public void testModificationJobsExecutionByJobExecutorWithAuthorizationEnabledAndTenant() {
    ProcessEngineConfigurationImpl processEngineConfiguration = rule.getProcessEngineConfiguration();

    processEngineConfiguration.setAuthorizationEnabled(true);
    ProcessDefinition processDefinition = testRule.deployForTenantAndGetDefinition("tenantId", instance);

    try {
      Batch batch = helper.startAfterAsync("process1", 10, "user1", processDefinition.getId());
      helper.completeSeedJobs(batch);

      testRule.executeAvailableJobs();

      // then all process instances where modified
      for (String processInstanceId : helper.currentProcessInstances) {
        ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
        assertNotNull(updatedTree);
        assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

        assertThat(updatedTree).hasStructure(
            describeActivityInstanceTree(
                processDefinition.getId())
            .activity("user1")
            .activity("user2")
            .done());
      }

    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
    }

  }

  @Test
  public void testBatchExecutionFailureWithMissingProcessInstance() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);
    Batch batch = helper.startAfterAsync("process1", 2, "user1", processDefinition.getId());
    helper.completeSeedJobs(batch);

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String deletedProcessInstanceId = processInstances.get(0).getId();

    // when
    runtimeService.deleteProcessInstance(deletedProcessInstanceId, "test");
    helper.executeJobs(batch);

    // then the remaining process instance was modified
    for (String processInstanceId : helper.currentProcessInstances) {
      if (processInstanceId.equals(helper.currentProcessInstances.get(0))) {
        ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
        assertNull(updatedTree);
        continue;
      }

      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
          .activity("user1")
          .activity("user2")
          .done());
    }

    // and one batch job failed and has 2 retries left
    List<Job> modificationJobs = helper.getExecutionJobs(batch);
    assertThat(modificationJobs.size()).isEqualTo(1);

    Job failedJob = modificationJobs.get(0);
    assertThat(failedJob.getRetries()).isEqualTo(2);
    assertThat(failedJob.getExceptionMessage()).startsWith("ENGINE-13036");
    assertThat(failedJob.getExceptionMessage()).contains("Process instance '" + deletedProcessInstanceId + "' cannot be modified");
  }

  @Test
  public void testBatchExecutionFailureWithHistoricQueryThatMatchesDeletedInstance() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> startedInstances = helper.startInstances("process1", 3);

    String deletedProcessInstanceId = startedInstances.get(0);

    runtimeService.deleteProcessInstance(deletedProcessInstanceId, "test");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId());

    Batch batch = runtimeService
        .createModification(processDefinition.getId())
        .startAfterActivity("user1")
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then the remaining process instance was modified
    for (String processInstanceId : startedInstances) {
      if (processInstanceId.equals(deletedProcessInstanceId)) {
        ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
        assertNull(updatedTree);
        continue;
      }

      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinition.getId())
              .activity("user1")
              .activity("user2")
              .done());
    }

    // and one batch job failed and has 2 retries left
    List<Job> modificationJobs = helper.getExecutionJobs(batch);
    assertThat(modificationJobs.size()).isEqualTo(1);

    Job failedJob = modificationJobs.get(0);
    assertThat(failedJob.getRetries()).isEqualTo(2);
    assertThat(failedJob.getExceptionMessage()).startsWith("ENGINE-13036");
    assertThat(failedJob.getExceptionMessage()).contains("Process instance '" + deletedProcessInstanceId + "' cannot be modified");
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.syncAfterOneTaskProcess.bpmn20.xml" })
  public void testBatchExecutionWithHistoricQueryUnfinished() {
    // given
    List<String> startedInstances = helper.startInstances("oneTaskProcess", 3);

    TaskService taskService = rule.getTaskService();
    Task task = taskService.createTaskQuery().processInstanceId(startedInstances.get(0)).singleResult();
    String processDefinitionId = task.getProcessDefinitionId();
    String completedProcessInstanceId = task.getProcessInstanceId();
    assertNotNull(task);
    taskService.complete(task.getId());

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery().unfinished().processDefinitionId(processDefinitionId);
    assertThat(historicProcessInstanceQuery.count()).isEqualTo(2);

    // then
    Batch batch = runtimeService
        .createModification(processDefinitionId)
        .startAfterActivity("theStart")
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    //     then the remaining process instance was modified
    for (String processInstanceId : startedInstances) {
      if (processInstanceId.equals(completedProcessInstanceId)) {
        ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
        assertNull(updatedTree);
        continue;
      }

      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinitionId)
              .activity("theTask")
              .activity("theTask")
              .done());
    }

    // and one batch job failed and has 2 retries left
    List<Job> modificationJobs = helper.getExecutionJobs(batch);
    assertThat(modificationJobs.size()).isEqualTo(0);
  }

  @Test
  public void testBatchCreationWithProcessInstanceQuery() {
    int processInstanceCount = 15;
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);
    helper.startInstances("process1", 15);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(processInstanceCount);

    // when
    Batch batch = runtimeService
      .createModification(processDefinition.getId())
      .startAfterActivity("user1")
      .processInstanceQuery(processInstanceQuery)
      .executeAsync();

    // then a batch is created
    assertBatchCreated(batch, processInstanceCount);
  }

  @Test
  public void testBatchCreationWithHistoricProcessInstanceQuery() {
    int processInstanceCount = 15;
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);
    helper.startInstances("process1", 15);

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId());
    assertThat(historicProcessInstanceQuery.count()).isEqualTo(processInstanceCount);

    // when
    Batch batch = runtimeService
        .createModification(processDefinition.getId())
        .startAfterActivity("user1")
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .executeAsync();

    // then a batch is created
    assertBatchCreated(batch, processInstanceCount);
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.syncAfterOneTaskProcess.bpmn20.xml" })
  public void testBatchExecutionFailureWithFinishedInstanceId() {
    // given
    List<String> startedInstances = helper.startInstances("oneTaskProcess", 3);

    TaskService taskService = rule.getTaskService();
    Task task = taskService.createTaskQuery().processInstanceId(startedInstances.get(0)).singleResult();
    String processDefinitionId = task.getProcessDefinitionId();
    String completedProcessInstanceId = task.getProcessInstanceId();
    assertNotNull(task);
    taskService.complete(task.getId());

    // then
    Batch batch = runtimeService
        .createModification(processDefinitionId)
        .startAfterActivity("theStart")
        .processInstanceIds(startedInstances)
        .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    //     then the remaining process instance was modified
    for (String processInstanceId : startedInstances) {
      if (processInstanceId.equals(completedProcessInstanceId)) {
        ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
        assertNull(updatedTree);
        continue;
      }

      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinitionId)
              .activity("theTask")
              .activity("theTask")
              .done());
    }

    //    and one batch job failed and has 2 retries left
    List<Job> modificationJobs = helper.getExecutionJobs(batch);
    assertThat(modificationJobs.size()).isEqualTo(1);

    Job failedJob = modificationJobs.get(0);
    assertThat(failedJob.getRetries()).isEqualTo(2);
    assertThat(failedJob.getExceptionMessage()).startsWith("ENGINE-13036");
    assertThat(failedJob.getExceptionMessage()).contains("Process instance '" + completedProcessInstanceId + "' cannot be modified");
  }


  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.syncAfterOneTaskProcess.bpmn20.xml" })
  public void testBatchExecutionFailureWithHistoricQueryThatMatchesFinishedInstance() {
    // given
    List<String> startedInstances = helper.startInstances("oneTaskProcess", 3);

    TaskService taskService = rule.getTaskService();
    Task task = taskService.createTaskQuery().processInstanceId(startedInstances.get(0)).singleResult();
    String processDefinitionId = task.getProcessDefinitionId();
    String completedProcessInstanceId = task.getProcessInstanceId();
    assertNotNull(task);
    taskService.complete(task.getId());

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinitionId);
    assertThat(historicProcessInstanceQuery.count()).isEqualTo(3);

    // then
    Batch batch = runtimeService
        .createModification(processDefinitionId)
        .startAfterActivity("theStart")
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    //     then the remaining process instance was modified
    for (String processInstanceId : startedInstances) {
      if (processInstanceId.equals(completedProcessInstanceId)) {
        ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
        assertNull(updatedTree);
        continue;
      }

      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      assertNotNull(updatedTree);
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              processDefinitionId)
              .activity("theTask")
              .activity("theTask")
              .done());
    }

    // and one batch job failed and has 2 retries left
    List<Job> modificationJobs = helper.getExecutionJobs(batch);
    assertThat(modificationJobs.size()).isEqualTo(1);

    Job failedJob = modificationJobs.get(0);
    assertThat(failedJob.getRetries()).isEqualTo(2);
    assertThat(failedJob.getExceptionMessage()).startsWith("ENGINE-13036");
    assertThat(failedJob.getExceptionMessage()).contains("Process instance '" + completedProcessInstanceId + "' cannot be modified");
  }


  @Test
  public void testBatchCreationWithOverlappingProcessInstanceIdsAndQuery() {
    int processInstanceCount = 15;
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);
    List<String> processInstanceIds = helper.startInstances("process1", 15);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(processInstanceCount);

    // when
    Batch batch = runtimeService
      .createModification(processDefinition.getId())
      .startTransition("seq")
      .processInstanceIds(processInstanceIds)
      .processInstanceQuery(processInstanceQuery)
      .executeAsync();

    // then a batch is created
    assertBatchCreated(batch, processInstanceCount);
  }

  @Test
  public void testBatchCreationWithOverlappingProcessInstanceIdsAndHistoricQuery() {
    int processInstanceCount = 15;
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);
    List<String> processInstanceIds = helper.startInstances("process1", 15);

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId());
    assertThat(historicProcessInstanceQuery.count()).isEqualTo(processInstanceCount);

    // when
    Batch batch = runtimeService
        .createModification(processDefinition.getId())
        .startTransition("seq")
        .processInstanceIds(processInstanceIds)
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .executeAsync();

    // then a batch is created
    assertBatchCreated(batch, processInstanceCount);
  }

  @Test
  public void testBatchCreationWithOverlappingHistoricQueryAndQuery() {
    // given
    int processInstanceCount = 15;
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);
    helper.startInstances("process1", processInstanceCount);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(processInstanceCount);
    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId());
    assertThat(historicProcessInstanceQuery.count()).isEqualTo(processInstanceCount);

    // when
    Batch batch = runtimeService
        .createModification(processDefinition.getId())
        .startTransition("seq")
        .processInstanceQuery(processInstanceQuery)
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .executeAsync();

    // then a batch is created
    assertBatchCreated(batch, processInstanceCount);
  }

  @Test
  public void testListenerInvocation() {
    // given
    DelegateEvent.clearEvents();
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(modify(instance)
        .activityBuilder("user2")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, DelegateExecutionListener.class.getName())
        .done()
      );

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    Batch batch = runtimeService
      .createModification(processDefinition.getId())
      .startBeforeActivity("user2")
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    List<DelegateEvent> recordedEvents = DelegateEvent.getEvents();
    assertThat(recordedEvents.size()).isEqualTo(1);

    DelegateEvent event = recordedEvents.get(0);
    assertThat(event.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(event.getCurrentActivityId()).isEqualTo("user2");

    DelegateEvent.clearEvents();
  }

  @Test
  public void testSkipListenerInvocationF() {
    // given
    DelegateEvent.clearEvents();
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(modify(instance)
        .activityBuilder("user2")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, DelegateExecutionListener.class.getName())
        .done());

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    Batch batch = runtimeService
      .createModification(processDefinition.getId())
      .cancelAllForActivity("user2")
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .skipCustomListeners()
      .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    assertThat(DelegateEvent.getEvents().size()).isEqualTo(0);
  }

  @Test
  public void testIoMappingInvocation() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(modify(instance)
      .activityBuilder("user1")
      .operatonInputParameter("foo", "bar")
      .done()
    );

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    Batch batch = runtimeService
      .createModification(processDefinition.getId())
      .startAfterActivity("user2")
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    VariableInstance inputVariable = runtimeService.createVariableInstanceQuery().singleResult();
    Assert.assertNotNull(inputVariable);
    assertThat(inputVariable.getName()).isEqualTo("foo");
    assertThat(inputVariable.getValue()).isEqualTo("bar");

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(inputVariable.getActivityInstanceId()).isEqualTo(activityInstance.getActivityInstances("user1")[0].getId());
  }

  @Test
  public void testSkipIoMappingInvocation() {
    // given

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(modify(instance)
        .activityBuilder("user2")
        .operatonInputParameter("foo", "bar")
        .done());


    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    Batch batch = runtimeService
      .createModification(processDefinition.getId())
      .startBeforeActivity("user2")
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .skipIoMappings()
      .executeAsync();

    helper.completeSeedJobs(batch);

    // when
    helper.executeJobs(batch);

    // then
    assertThat(runtimeService.createVariableInstanceQuery().count()).isEqualTo(0);
  }

  @Test
  public void testCancelWithoutFlag() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 1);

    // when
    Batch batch = runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user")
      .processInstanceIds(processInstanceIds)
      .executeAsync();

    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    // then
    assertThat(runtimeService.createExecutionQuery().list().size()).isEqualTo(0);
  }

  @Test
  public void testCancelWithoutFlag2() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 1);

    // when
    Batch batch = runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user", false)
      .processInstanceIds(processInstanceIds)
      .executeAsync();

    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    // then
    assertThat(runtimeService.createExecutionQuery().list().size()).isEqualTo(0);
  }

  @Test
  public void testCancelWithFlag() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 1);

    // when
    Batch batch = runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user", true)
      .processInstanceIds(processInstanceIds)
      .executeAsync();

    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    // then
    ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().singleResult();
    assertNotNull(execution);
    assertThat(execution.getActivityId()).isEqualTo("user");
  }

  @Test
  public void testCancelWithFlagForManyInstances() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 10);

    // when
    Batch batch = runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user", true)
      .processInstanceIds(processInstanceIds)
      .executeAsync();

    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    // then
    for (String processInstanceId : processInstanceIds) {
      Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).singleResult();
      assertNotNull(execution);
      assertThat(((ExecutionEntity) execution).getActivityId()).isEqualTo("user");
    }
  }

  @Test
  public void shouldSetInvocationsPerBatchType() {
    // given
    configuration.getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_PROCESS_INSTANCE_MODIFICATION, 42);

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> processInstanceIds = helper.startInstances("process1", 2);

    // when
    Batch batch = runtimeService.createModification(processDefinition.getId())
        .startAfterActivity("user2")
        .processInstanceIds(processInstanceIds)
        .executeAsync();

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    configuration.setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void shouldSetExecutionStartTimeInBatchAndHistory() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    Batch batch = helper.startAfterAsync("process1", 20, "user1", processDefinition.getId());
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch);

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = rule.getHistoryService().createHistoricBatchQuery().singleResult();
    batch = rule.getManagementService().createBatchQuery().singleResult();

    Assertions.assertThat(batch.getExecutionStartTime()).isCloseTo(START_DATE, 1000);
    Assertions.assertThat(historicBatch.getExecutionStartTime()).isCloseTo(START_DATE, 1000);

    // clear
    configuration.setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  protected void assertBatchCreated(Batch batch, int processInstanceCount) {
    assertNotNull(batch);
    assertNotNull(batch.getId());
    assertThat(batch.getType()).isEqualTo("instance-modification");
    assertThat(batch.getTotalJobs()).isEqualTo(processInstanceCount);
    assertThat(batch.getBatchJobsPerSeed()).isEqualTo(defaultBatchJobsPerSeed);
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(defaultInvocationsPerBatchJob);
  }

}
