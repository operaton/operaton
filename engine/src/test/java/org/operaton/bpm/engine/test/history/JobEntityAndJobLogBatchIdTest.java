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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutableProcessUtil.USER_TASK_PROCESS;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.batch.BatchMonitorJobHandler;
import org.operaton.bpm.engine.impl.batch.BatchSeedJobHandler;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.history.removaltime.batch.helper.BatchSetRemovalTimeExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.batch.BatchExtension;
import org.operaton.bpm.engine.test.util.BatchRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class JobEntityAndJobLogBatchIdTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  BatchExtension batchRule = new BatchExtension(engineRule, testRule);
  @RegisterExtension
  BatchSetRemovalTimeExtension batchRemovalTimeRule = new BatchSetRemovalTimeExtension(engineRule, testRule);

  RuntimeService runtimeService;
  HistoryService historyService;
  ManagementService managementService;
  DecisionService decisionService;
  ExternalTaskService externalTaskService;

  private BpmnModelInstance getTwoUserTasksProcess() {
    return Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask("task1")
        .userTask("task2")
        .endEvent()
        .done();
  }

  private BpmnModelInstance getTimerProcess() {
    return Bpmn.createExecutableProcess("process")
        .startEvent()
        .timerWithDuration("PT5H")
        .endEvent()
        .done();
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_SetHistoricBatchRemovalTime() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // create historic Batch
    Batch setVariablesBatch = runtimeService.setVariablesAsync(List.of(processInstance.getId()),
        Variables.createVariables().putValue("foo", "bar"));
    batchRule.syncExec(setVariablesBatch);

    // set historic batch removal time
    Batch setRemovalTimeBatch = historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(batchRemovalTimeRule.REMOVAL_TIME)
        .byQuery(historyService.createHistoricBatchQuery())
        .executeAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(setRemovalTimeBatch);

    // then
    assertProcessedJobs(setRemovalTimeBatch, processedJobs);
    /*
     * there is no way to filter for the historic job log of only the set historic batch removal time batch
     * as a workaround: check that the historic job log contains the corresponding batch id for seed and
     * monitor jobs from both batches.
     */
    assertThat(setVariablesBatch.getId()).isNotNull();
    assertThat(setRemovalTimeBatch.getId()).isNotNull();
    List<HistoricJobLog> batchSeedJobLogs = historyService.createHistoricJobLogQuery()
        .successLog()
        .jobDefinitionType(BatchSeedJobHandler.TYPE)
        .list();
    assertThat(batchSeedJobLogs).extracting("batchId")
        .containsExactlyInAnyOrder(setVariablesBatch.getId(), setRemovalTimeBatch.getId());
    List<HistoricJobLog> batchMonitorJobLogs = historyService.createHistoricJobLogQuery()
        .successLog()
        .jobDefinitionType(BatchMonitorJobHandler.TYPE)
        .list();
    assertThat(batchMonitorJobLogs).extracting("batchId")
        .containsExactlyInAnyOrder(setVariablesBatch.getId(), setRemovalTimeBatch.getId());
    List<HistoricJobLog> batchExecutionJobLogs = historyService.createHistoricJobLogQuery()
        .successLog()
        .jobDefinitionType(Batch.TYPE_BATCH_SET_REMOVAL_TIME)
        .list();
    assertThat(batchExecutionJobLogs).extracting("batchId").containsOnly(setRemovalTimeBatch.getId());
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_SetVariables() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    ProcessInstance process = runtimeService.startProcessInstanceByKey("process");

    Batch batch = runtimeService.setVariablesAsync(List.of(process.getId()), Variables.createVariables().putValue("foo", "bar"));

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_SET_VARIABLES);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetBatchIdOnJobAndJobLog_DecisionSetRemovalTime() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
        .variables(
          Variables.createVariables()
            .putValue("temperature", 32)
            .putValue("dayType", "Weekend")
        ).evaluate();

    Batch batch = historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(batchRemovalTimeRule.REMOVAL_TIME)
        .byQuery(historyService.createHistoricDecisionInstanceQuery())
        .executeAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    // we expect three execution jobs, all should have the batch id
    assertHistoricJobLogs(batch, Batch.TYPE_DECISION_SET_REMOVAL_TIME);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetBatchIdOnJobAndJobLog_DeleteHistoricDecisionInstances() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
        .variables(
            Variables.createVariables()
                .putValue("temperature", 32)
                .putValue("dayType", "Weekend")
        ).evaluate();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery()
        .decisionDefinitionKey("dish-decision");

    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_HISTORIC_DECISION_INSTANCE_DELETION);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_DeleteHistoricProcessInstances() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .endEvent()
        .done());
    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey("process");

    Batch batch = historyService.deleteHistoricProcessInstancesAsync(query, null);

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_DeleteProcessInstances() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    ProcessInstance process = runtimeService.startProcessInstanceByKey("process");

    Batch batch = runtimeService.deleteProcessInstancesAsync(List.of(process.getId()), null);

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_PROCESS_INSTANCE_DELETION);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_MessageCorrelation() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .intermediateCatchEvent().message("message")
        .userTask()
        .endEvent()
        .done());
    ProcessInstance process = runtimeService.startProcessInstanceByKey("process");

    Batch batch = runtimeService.createMessageCorrelationAsync("message")
        .processInstanceIds(List.of(process.getId()))
        .correlateAllAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_CORRELATE_MESSAGE);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_Migration() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(USER_TASK_PROCESS);
    ProcessInstance process = runtimeService.startProcessInstanceByKey("process");
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(getTwoUserTasksProcess());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    Batch batch = runtimeService.newMigration(migrationPlan).processInstanceIds(List.of(process.getId())).executeAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_PROCESS_INSTANCE_MIGRATION);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_Modification() {
    // given
    testRule.deploy(getTwoUserTasksProcess());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // when
    Batch batch = runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(tree.getActivityInstances("task1")[0].getId())
        .startBeforeActivity("task2")
        .executeAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_PROCESS_INSTANCE_MODIFICATION);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_ProcessSetRemovalTime() {
    // given
    testRule.deploy(getTwoUserTasksProcess());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Batch batch = historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(batchRemovalTimeRule.REMOVAL_TIME)
        .byIds(processInstance.getId())
        .executeAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_PROCESS_SET_REMOVAL_TIME);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_RestartProcessInstance() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    Batch batch = runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
        .processInstanceIds(processInstance.getId())
        .startBeforeActivity("task1")
        .executeAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_PROCESS_INSTANCE_RESTART);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_SetExternalTaskRetries() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask().operatonExternalTask("topic")
        .endEvent()
        .done());
    runtimeService.startProcessInstanceByKey("process");

    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();

    Batch batch = externalTaskService.setRetriesAsync(List.of(externalTask.getId()), null, 5);

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_SET_EXTERNAL_TASK_RETRIES);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_SetJobRetries() {
    // given
    testRule.deploy(getTimerProcess());
    runtimeService.startProcessInstanceByKey("process");

    Job timerJob = managementService.createJobQuery().singleResult();

    Batch batch = managementService.setJobRetriesAsync(List.of(timerJob.getId()), 5);

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_SET_JOB_RETRIES);
  }

  @Test
  void shouldSetBatchIdOnJobAndJobLog_UpdateProcessInstancesSuspendState() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Batch batch = runtimeService.updateProcessInstanceSuspensionState()
        .byProcessInstanceIds(List.of(processInstance.getId()))
        .suspendAsync();

    // when
    Map<String, List<Job>> processedJobs = batchRule.syncExec(batch);

    // then
    assertProcessedJobs(batch, processedJobs);
    assertHistoricJobLogs(batch, Batch.TYPE_PROCESS_INSTANCE_UPDATE_SUSPENSION_STATE);
  }

  @Test
  void shouldNotSetBatchIdOnJobOrJobLog_nonBatchJob() {
    // given
    testRule.deploy(getTimerProcess());
    runtimeService.startProcessInstanceByKey("process");

    Job timerJob = managementService.createJobQuery().singleResult();

    // when
    managementService.executeJob(timerJob.getId());

    // then
    List<HistoricJobLog> historicJobLogs = historyService.createHistoricJobLogQuery().list();
    assertThat(historicJobLogs).hasSize(2);
    assertThat(historicJobLogs).extracting("batchId").containsOnlyNulls();
  }

  // HELPER

  private void assertProcessedJobs(Batch batch, Map<String, List<Job>> processedJobs) {
    assertThat(processedJobs.get(BatchRule.SEED_JOB)).extracting("batchId").containsOnly(batch.getId());
    assertThat(processedJobs.get(BatchRule.EXECUTION_JOBS)).extracting("batchId").containsOnly(batch.getId());
    assertThat(processedJobs.get(BatchRule.MONITOR_JOB)).extracting("batchId").containsOnly(batch.getId());
  }

  private void assertHistoricJobLogs(Batch batch, String expectedBatchType) {
    assertThat(batch.getId()).isNotNull();
    List<HistoricJobLog> batchSeedJobLogs = historyService.createHistoricJobLogQuery()
        .successLog()
        .jobDefinitionType(BatchSeedJobHandler.TYPE)
        .list();
    assertThat(batchSeedJobLogs).extracting("batchId").containsOnly(batch.getId());
    List<HistoricJobLog> batchMonitorJobLogs = historyService.createHistoricJobLogQuery()
        .successLog()
        .jobDefinitionType(BatchMonitorJobHandler.TYPE)
        .list();
    assertThat(batchMonitorJobLogs).extracting("batchId").containsOnly(batch.getId());
    List<HistoricJobLog> batchExecutionJobLogs = historyService.createHistoricJobLogQuery()
        .successLog()
        .jobDefinitionType(expectedBatchType)
        .list();
    assertThat(batchExecutionJobLogs).extracting("batchId").containsOnly(batch.getId());
  }
}