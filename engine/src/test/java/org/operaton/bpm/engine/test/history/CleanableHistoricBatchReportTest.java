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
package org.operaton.bpm.engine.test.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.BatchModificationHelper;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class CleanableHistoricBatchReportTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper migrationHelper = new BatchMigrationHelper(engineRule, migrationRule);
  BatchModificationHelper modificationHelper = new BatchModificationHelper(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  HistoryService historyService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ManagementService managementService;

  @AfterEach
  void cleanUp() {
    ClockUtil.reset();
    migrationHelper.removeAllRunningAndHistoricBatches();
    processEngineConfiguration.setBatchOperationHistoryTimeToLive(null);
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(null);
  }

  @Test
  void testReportMixedConfiguration() {
    Map<String, String> map = new HashMap<>();
    int modOperationsTTL = 20;
    map.put("instance-modification", "P20D");
    int defaultTTL = 5;
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(map);
    processEngineConfiguration.initHistoryCleanup();

    Date startDate = new Date();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    Batch modificationBatch = createModificationBatch();
    List<String> batchIds = new ArrayList<>();
    batchIds.add(modificationBatch.getId());

    int migrationCountBatch = 10;
    List<String> batchIds1 = new ArrayList<>();
    batchIds1.addAll(createMigrationBatchList(migrationCountBatch));

    int cancelationCountBatch = 20;
    List<String> batchIds2 = new ArrayList<>();
    batchIds2.addAll(createCancelationBatchList(cancelationCountBatch));


    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -8));

    for (String batchId : batchIds) {
      managementService.deleteBatch(batchId, false);
    }

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -2));

    for (int i = 0; i < 4; i++) {
      managementService.deleteBatch(batchIds1.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -7));
    for (int i = 6; i < batchIds1.size(); i++) {
      managementService.deleteBatch(batchIds1.get(i), false);
    }

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -10));
    for (int i = 0; i < 7; i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -5));
    for (int i = 7; i < 11; i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -1));
    for (int i = 13; i < batchIds2.size(); i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }

    ClockUtil.setCurrentTime(DateUtils.addSeconds(startDate, 1));

    // when
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(31);

    List<CleanableHistoricBatchReportResult> list = historyService.createCleanableHistoricBatchReport().list();
    assertThat(list).hasSize(3);
    for (CleanableHistoricBatchReportResult result : list) {
      if ("instance-migration".equals(result.getBatchType())) {
        checkResultNumbers(result, 4, 8, defaultTTL);
      } else if ("instance-modification".equals(result.getBatchType())) {
        checkResultNumbers(result, 0, 1, modOperationsTTL);
      } else if ("instance-deletion".equals(result.getBatchType())) {
        checkResultNumbers(result, 11, 18, defaultTTL);
      }
    }
  }

  @Test
  void testReportNoDefaultConfiguration() {
    Map<String, String> map = new HashMap<>();
    int modOperationsTTL = 5;
    map.put("instance-modification", "P5D");
    int delOperationsTTL = 7;
    map.put("instance-deletion", "P7D");
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(map);
    processEngineConfiguration.initHistoryCleanup();
    assertThat(processEngineConfiguration.getBatchOperationHistoryTimeToLive()).isNull();

    Date startDate = new Date();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    Batch modificationBatch = createModificationBatch();
    List<String> batchIds = new ArrayList<>();
    batchIds.add(modificationBatch.getId());

    int migrationCountBatch = 10;
    List<String> batchIds1 = new ArrayList<>();
    batchIds1.addAll(createMigrationBatchList(migrationCountBatch));

    int cancelationCountBatch = 20;
    List<String> batchIds2 = new ArrayList<>();
    batchIds2.addAll(createCancelationBatchList(cancelationCountBatch));


    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -8));

    for (String batchId : batchIds) {
      managementService.deleteBatch(batchId, false);
    }

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -2));

    for (int i = 0; i < 4; i++) {
      managementService.deleteBatch(batchIds1.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -7));
    for (int i = 6; i < batchIds1.size(); i++) {
      managementService.deleteBatch(batchIds1.get(i), false);
    }

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -10));
    for (int i = 0; i < 7; i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -5));
    for (int i = 7; i < 11; i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -1));
    for (int i = 13; i < batchIds2.size(); i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }

    ClockUtil.setCurrentTime(DateUtils.addSeconds(startDate, 1));

    // when
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(31);

    List<CleanableHistoricBatchReportResult> list = historyService.createCleanableHistoricBatchReport().list();
    assertThat(list).hasSize(3);
    for (CleanableHistoricBatchReportResult result : list) {
      if ("instance-migration".equals(result.getBatchType())) {
        checkResultNumbers(result, 0, 8, null);
      } else if ("instance-modification".equals(result.getBatchType())) {
        checkResultNumbers(result, 1, 1, modOperationsTTL);
      } else if ("instance-deletion".equals(result.getBatchType())) {
        checkResultNumbers(result, delOperationsTTL, 18, delOperationsTTL);
      }
    }
  }

  @Test
  void testReportNoTTLConfiguration() {
    processEngineConfiguration.initHistoryCleanup();
    assertThat(processEngineConfiguration.getBatchOperationHistoryTimeToLive()).isNull();

    Date startDate = new Date();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    int cancelationCountBatch = 20;
    List<String> batchIds2 = new ArrayList<>();
    batchIds2.addAll(createCancelationBatchList(cancelationCountBatch));

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -10));
    for (int i = 0; i < 7; i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -5));
    for (int i = 7; i < 11; i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -1));
    for (int i = 13; i < batchIds2.size(); i++) {
      managementService.deleteBatch(batchIds2.get(i), false);
    }

    ClockUtil.setCurrentTime(DateUtils.addSeconds(startDate, 1));

    // when
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(20);

    assertThat(historyService.createCleanableHistoricBatchReport().count()).isOne();
    checkResultNumbers(historyService.createCleanableHistoricBatchReport().singleResult(), 0, 18, null);
  }

  @Test
  void testReportZeroTTL() {
    Map<String, String> map = new HashMap<>();
    int modOperationsTTL = 0;
    map.put("instance-modification", "P0D");
    processEngineConfiguration.setBatchOperationsForHistoryCleanup(map);
    processEngineConfiguration.initHistoryCleanup();

    Date startDate = ClockUtil.getCurrentTime();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    Batch modificationBatch = createModificationBatch();
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -7));

    managementService.deleteBatch(modificationBatch.getId(), false);

    CleanableHistoricBatchReportResult result = historyService.createCleanableHistoricBatchReport().singleResult();
    assertThat(result).isNotNull();
    checkResultNumbers(result, 1, 1, modOperationsTTL);
  }

  @Test
  void testReportOrderByFinishedProcessInstance() {
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();
    assertThat(processEngineConfiguration.getBatchOperationHistoryTimeToLive()).isNotNull();

    Date startDate = new Date();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    List<String> batchIds = new ArrayList<>();

    Batch modificationBatch = createModificationBatch();
    batchIds.add(modificationBatch.getId());

    int migrationCountBatch = 10;
    batchIds.addAll(createMigrationBatchList(migrationCountBatch));

    int cancelationCountBatch = 20;
    batchIds.addAll(createCancelationBatchList(cancelationCountBatch));

    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -8));

    for (String batchId : batchIds) {
      managementService.deleteBatch(batchId, false);
    }

    ClockUtil.setCurrentTime(DateUtils.addSeconds(startDate, 1));

    // assume
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(31);

    // then
    List<CleanableHistoricBatchReportResult> reportResultAsc = historyService
        .createCleanableHistoricBatchReport()
        .orderByFinishedBatchOperation()
        .asc()
        .list();
    assertThat(reportResultAsc).hasSize(3);
    assertThat(reportResultAsc.get(0).getBatchType()).isEqualTo("instance-modification");
    assertThat(reportResultAsc.get(1).getBatchType()).isEqualTo("instance-migration");
    assertThat(reportResultAsc.get(2).getBatchType()).isEqualTo("instance-deletion");

    List<CleanableHistoricBatchReportResult> reportResultDesc = historyService
        .createCleanableHistoricBatchReport()
        .orderByFinishedBatchOperation()
        .desc()
        .list();
    assertThat(reportResultDesc).hasSize(3);
    assertThat(reportResultDesc.get(0).getBatchType()).isEqualTo("instance-deletion");
    assertThat(reportResultDesc.get(1).getBatchType()).isEqualTo("instance-migration");
    assertThat(reportResultDesc.get(2).getBatchType()).isEqualTo("instance-modification");
  }

  private void checkResultNumbers(CleanableHistoricBatchReportResult result, int expectedCleanable, int expectedFinished, Integer expectedTTL) {
    assertThat(result.getCleanableBatchesCount()).isEqualTo(expectedCleanable);
    assertThat(result.getFinishedBatchesCount()).isEqualTo(expectedFinished);
    assertThat(result.getHistoryTimeToLive()).isEqualTo(expectedTTL);
  }

  private BpmnModelInstance createModelInstance() {
    return Bpmn.createExecutableProcess("process")
        .startEvent("start")
        .userTask("userTask1")
        .sequenceFlowId("seq")
        .userTask("userTask2")
        .endEvent("end")
        .done();
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
    BpmnModelInstance instance = createModelInstance();
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    String pId = runtimeService.startProcessInstanceById(processDefinition.getId()).getId();
    List<String> batchIds = new ArrayList<>();
    for (int i = 0; i < cancelationCountBatch; i++) {
      batchIds.add(runtimeService.deleteProcessInstancesAsync(List.of(pId), "create-deletion-batch").getId());
    }
    return batchIds;
  }

}
