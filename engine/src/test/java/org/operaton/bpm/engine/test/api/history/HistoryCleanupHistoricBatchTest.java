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
package org.operaton.bpm.engine.test.api.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.BatchModificationHelper;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_END_TIME_BASED;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoryCleanupHistoricBatchTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .configurator(configuration ->
      configuration.setHistoryCleanupDegreeOfParallelism(3)).build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);

  BatchMigrationHelper migrationHelper = new BatchMigrationHelper(engineRule, migrationRule);
  BatchModificationHelper modificationHelper = new BatchModificationHelper(engineRule);

  private static final String DEFAULT_TTL_DAYS = "P5D";

  private Random random = new Random();

  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected ManagementService managementService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @BeforeEach
  void init() {
    processEngineConfiguration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_END_TIME_BASED);
  }

  @AfterEach
  void clearDatabase() {
    migrationHelper.removeAllRunningAndHistoricBatches();

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

      List<Job> jobs = managementService.createJobQuery().list();
      for (Job job : jobs) {
        commandContext.getJobManager().deleteJob((JobEntity) job);
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
      }

      List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();
      for (HistoricIncident historicIncident : historicIncidents) {
        commandContext.getDbEntityManager().delete((HistoricIncidentEntity) historicIncident);
      }

      commandContext.getMeterLogManager().deleteAll();

      return null;
    });
  }

  @AfterEach
  void resetConfiguration() {
    processEngineConfiguration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);
    processEngineConfiguration.setBatchOperationHistoryTimeToLive(null);
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(null);
  }

  @Test
  void testCleanupHistoricBatch() {
    initBatchOperationHistoryTimeToLive(DEFAULT_TTL_DAYS);
    int daysInThePast = -11;

    // given
    prepareHistoricBatches(3, daysInThePast);
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(3);

    // when
    runHistoryCleanup();

    // then
    assertThat(historyService.createHistoricBatchQuery().count()).isZero();
  }

  @Test
  void testCleanupHistoricJobLog() {
    initBatchOperationHistoryTimeToLive(DEFAULT_TTL_DAYS);
    int daysInThePast = -11;

    // given
    prepareHistoricBatches(1, daysInThePast);
    HistoricBatch batch = historyService.createHistoricBatchQuery().singleResult();
    String batchId = batch.getId();

    // when
    runHistoryCleanup();

    // then
    assertThat(historyService.createHistoricBatchQuery().count()).isZero();
    assertThat(historyService.createHistoricJobLogQuery().jobDefinitionConfiguration(batchId).count()).isZero();
  }

  @Test
  void testCleanupHistoricIncident() {
    initBatchOperationHistoryTimeToLive(DEFAULT_TTL_DAYS);
    ClockUtil.setCurrentTime(DateUtils.addDays(new Date(), -11));

    BatchEntity batch = (BatchEntity) createFailingMigrationBatch();

    migrationHelper.completeSeedJobs(batch);

    List<Job> list = managementService.createJobQuery().list();
    for (Job job : list) {
      if ("instance-migration".equals(((JobEntity) job).getJobHandlerType())) {
        managementService.setJobRetries(job.getId(), 1);
      }
    }
    migrationHelper.executeJobs(batch);

    List<String> byteArrayIds = findExceptionByteArrayIds();

    ClockUtil.setCurrentTime(DateUtils.addDays(new Date(), -10));
    managementService.deleteBatch(batch.getId(), false);
    ClockUtil.setCurrentTime(new Date());

    // given
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    String batchId = historicBatch.getId();

    // when
    runHistoryCleanup();

    assertThat(historyService.createHistoricBatchQuery().count()).isZero();
    assertThat(historyService.createHistoricJobLogQuery().jobDefinitionConfiguration(batchId).count()).isZero();
    assertThat(historyService.createHistoricIncidentQuery().count()).isZero();
    verifyByteArraysWereRemoved(byteArrayIds.toArray(new String[] {}));
  }

  @Test
  void testHistoryCleanupBatchMetrics() {
    initBatchOperationHistoryTimeToLive(DEFAULT_TTL_DAYS);
    // given
    int daysInThePast = -11;
    int batchesCount = 5;
    prepareHistoricBatches(batchesCount, daysInThePast);

    // when
    runHistoryCleanup();

    // then
    final long removedBatches = managementService.createMetricsQuery().name(Metrics.HISTORY_CLEANUP_REMOVED_BATCH_OPERATIONS).sum();

    assertThat(removedBatches).isEqualTo(batchesCount);
  }

  @Test
  void testBatchOperationTypeConfigurationOnly() {
    Map<String, String> map = new HashMap<>();
    map.put("instance-migration", "P2D");
    map.put("instance-deletion", DEFAULT_TTL_DAYS);
    processEngineConfiguration.setBatchOperationHistoryTimeToLive(null);
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(map);
    processEngineConfiguration.initHistoryCleanup();

    assertThat(processEngineConfiguration.getBatchOperationHistoryTimeToLive()).isNull();

    Date startDate = ClockUtil.getCurrentTime();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    List<String> batchIds = new ArrayList<>();

    int migrationCountBatch = 10;
    batchIds.addAll(createMigrationBatchList(migrationCountBatch));

    int cancelationCountBatch = 20;
    batchIds.addAll(createCancelationBatchList(cancelationCountBatch));

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -7));

    for (String batchId : batchIds) {
      managementService.deleteBatch(batchId, false);
    }

    ClockUtil.setCurrentTime(new Date());

    // when
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(30);
    runHistoryCleanup();

    // then
    assertThat(historyService.createHistoricBatchQuery().count()).isZero();
    for (String batchId : batchIds) {
      assertThat(historyService.createHistoricJobLogQuery().jobDefinitionConfiguration(batchId).count()).isZero();
    }
  }

  private void runHistoryCleanup() {
    historyService.cleanUpHistoryAsync(true);
    final List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job historyCleanupJob: historyCleanupJobs) {
      managementService.executeJob(historyCleanupJob.getId());
    }
  }

  @Test
  void testMixedConfiguration() {
    Map<String, String> map = new HashMap<>();
    map.put("instance-modification", "P20D");
    processEngineConfiguration.setBatchOperationHistoryTimeToLive(DEFAULT_TTL_DAYS);
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(map);
    processEngineConfiguration.initHistoryCleanup();

    Date startDate = ClockUtil.getCurrentTime();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    Batch modificationBatch = createModificationBatch();
    List<String> batchIds = new ArrayList<>();
    batchIds.add(modificationBatch.getId());

    int migrationCountBatch = 10;
    batchIds.addAll(createMigrationBatchList(migrationCountBatch));

    int cancelationCountBatch = 20;
    batchIds.addAll(createCancelationBatchList(cancelationCountBatch));

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -8));

    for (String batchId : batchIds) {
      managementService.deleteBatch(batchId, false);
    }

    ClockUtil.setCurrentTime(new Date());

    // when
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(31);
    runHistoryCleanup();

    // then
    HistoricBatch modificationHistoricBatch = historyService.createHistoricBatchQuery().singleResult(); // the other batches should be cleaned
    assertThat(modificationHistoricBatch.getId()).isEqualTo(modificationBatch.getId());
    assertThat(historyService.createHistoricJobLogQuery().jobDefinitionConfiguration(modificationBatch.getId()).count()).isEqualTo(2);
    batchIds.remove(modificationBatch.getId());
    for (String batchId : batchIds) {
      assertThat(historyService.createHistoricJobLogQuery().jobDefinitionConfiguration(batchId).count()).isZero();
    }
  }

  @Test
  void testWrongGlobalConfiguration() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("PD");

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid value");
  }

  @Test
  void testWrongSpecificConfiguration() {
    // given
    Map<String, String> map = new HashMap<>();
    map.put("instance-modification", "PD");
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(map);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid value");

  }

  @Test
  void testWrongGlobalConfigurationNegativeTTL() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P-1D");

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid value");
  }

  @Test
  void testWrongSpecificConfigurationNegativeTTL() {
    // given
    Map<String, String> map = new HashMap<>();
    map.put("instance-modification", "P-5D");
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(map);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid value");
  }

  private void initBatchOperationHistoryTimeToLive(String days) {
    processEngineConfiguration.setBatchOperationHistoryTimeToLive(days);
    processEngineConfiguration.initHistoryCleanup();
  }

  private BpmnModelInstance createModelInstance() {
    return Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent("start")
        .userTask("userTask1")
        .sequenceFlowId("seq")
        .userTask("userTask2")
        .endEvent("end")
        .done();
  }

  private void prepareHistoricBatches(int batchesCount, int daysInThePast) {
    Date startDate = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    List<Batch> list = new ArrayList<>();
    for (int i = 0; i < batchesCount; i++) {
      list.add(migrationHelper.migrateProcessInstancesAsync(1));
    }

    for (Batch batch : list) {
      migrationHelper.completeSeedJobs(batch);
      migrationHelper.executeJobs(batch);

      ClockUtil.setCurrentTime(DateUtils.setMinutes(DateUtils.addDays(startDate, ++daysInThePast), random.nextInt(60)));
      migrationHelper.executeMonitorJob(batch);
    }

    ClockUtil.setCurrentTime(new Date());
  }

  private Batch createFailingMigrationBatch() {
    BpmnModelInstance instance = createModelInstance();

    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(instance);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(instance);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    return runtimeService.newMigration(migrationPlan).processInstanceIds(List.of(processInstance.getId(), "unknownId")).executeAsync();
  }

  private List<String> createMigrationBatchList(int migrationCountBatch) {
    List<String> batchIds = new ArrayList<>();
    for (int i = 0; i < migrationCountBatch; i++) {
      batchIds.add(migrationHelper.migrateProcessInstancesAsync(1).getId());
    }
    return batchIds;
  }

  private Batch createModificationBatch() {
    BpmnModelInstance instance = createModelInstance();
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    return modificationHelper.startAfterAsync("process", 1, "userTask1", processDefinition.getId());
  }

  private List<String> createCancelationBatchList(int cancelationCountBatch) {
    List<String> batchIds = new ArrayList<>();
    BpmnModelInstance instance = createModelInstance();
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    String pId = runtimeService.startProcessInstanceById(processDefinition.getId()).getId();
    for (int i = 0; i < cancelationCountBatch; i++) {
      batchIds.add(runtimeService.deleteProcessInstancesAsync(List.of(pId), "create-deletion-batch").getId());
    }
    return batchIds;
  }

  private void verifyByteArraysWereRemoved(final String... errorDetailsByteArrayIds) {
    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> {
      for (String errorDetailsByteArrayId : errorDetailsByteArrayIds) {
        assertThat(commandContext.getDbEntityManager().selectOne("selectByteArray", errorDetailsByteArrayId)).isNull();
      }
      return null;
    });
  }

  private List<String> findExceptionByteArrayIds() {
    List<String> exceptionByteArrayIds = new ArrayList<>();
    List<HistoricJobLog> historicJobLogs = historyService.createHistoricJobLogQuery().list();
    for (HistoricJobLog historicJobLog : historicJobLogs) {
      HistoricJobLogEventEntity historicJobLogEventEntity = (HistoricJobLogEventEntity) historicJobLog;
      if (historicJobLogEventEntity.getExceptionByteArrayId() != null) {
        exceptionByteArrayIds.add(historicJobLogEventEntity.getExceptionByteArrayId());
      }
    }
    return exceptionByteArrayIds;
  }

}
