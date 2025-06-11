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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicBatchByEndTime;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicBatchById;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicBatchByStartTime;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.test.util.ClockTestUtil;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricBatchQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  protected static BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @Test
  void testBatchQuery() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);

    // when
    List<HistoricBatch> list = historyService.createHistoricBatchQuery().list();

    // then
    assertThat(list).hasSize(2);

    List<String> batchIds = new ArrayList<>();
    for (HistoricBatch resultBatch : list) {
      batchIds.add(resultBatch.getId());
    }

    assertThat(batchIds)
            .contains(batch1.getId())
            .contains(batch2.getId());
  }

  @Test
  void testBatchQueryResult() {
    Date startDate = new Date(10000L);
    Date endDate = new Date(40000L);

    // given
    ClockUtil.setCurrentTime(startDate);
    Batch batch = helper.migrateProcessInstancesAsync(1);
    helper.completeSeedJobs(batch);
    helper.executeJobs(batch);

    ClockUtil.setCurrentTime(endDate);
    helper.executeMonitorJob(batch);

    // when
    HistoricBatch resultBatch = historyService.createHistoricBatchQuery().singleResult();

    // then
    assertThat(resultBatch).isNotNull();

    assertThat(resultBatch.getId()).isEqualTo(batch.getId());
    assertThat(resultBatch.getBatchJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(resultBatch.getMonitorJobDefinitionId()).isEqualTo(batch.getMonitorJobDefinitionId());
    assertThat(resultBatch.getSeedJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(resultBatch.getTenantId()).isEqualTo(batch.getTenantId());
    assertThat(resultBatch.getType()).isEqualTo(batch.getType());
    assertThat(resultBatch.getBatchJobsPerSeed()).isEqualTo(batch.getBatchJobsPerSeed());
    assertThat(resultBatch.getInvocationsPerBatchJob()).isEqualTo(batch.getInvocationsPerBatchJob());
    assertThat(resultBatch.getTotalJobs()).isEqualTo(batch.getTotalJobs());
    assertThat(resultBatch.getStartTime()).isEqualTo(startDate);
    assertThat(resultBatch.getEndTime()).isEqualTo(endDate);
  }

  @Test
  void testBatchQueryById() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    HistoricBatch resultBatch = historyService.createHistoricBatchQuery().batchId(batch1.getId()).singleResult();

    // then
    assertThat(resultBatch).isNotNull();
    assertThat(resultBatch.getId()).isEqualTo(batch1.getId());
  }

  @Test
  void testBatchQueryByIdNull() {
    var historicBatchQuery = historyService.createHistoricBatchQuery();
    try {
      historicBatchQuery.batchId(null);
      fail("exception expected");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("Batch id is null");
    }
  }

  @Test
  void testBatchQueryByType() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = historyService.createHistoricBatchQuery().type(batch1.getType()).count();

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void testBatchQueryByNonExistingType() {
    // given
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = historyService.createHistoricBatchQuery().type("foo").count();

    // then
    assertThat(count).isZero();
  }

  @Test
  void testBatchByState() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);

    helper.completeBatch(batch1);

    // when
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .completed(true)
      .singleResult();

    // then
    assertThat(historicBatch.getId()).isEqualTo(batch1.getId());

    // when
    historicBatch = historyService.createHistoricBatchQuery()
      .completed(false)
      .singleResult();

    // then
    assertThat(historicBatch.getId()).isEqualTo(batch2.getId());
  }

  @Test
  void testBatchQueryByTypeNull() {
    var historicBatchQuery = historyService.createHistoricBatchQuery();
    try {
      historicBatchQuery.type(null);
      fail("exception expected");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("Type is null");
    }
  }

  @Test
  void testBatchQueryCount() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = historyService.createHistoricBatchQuery().count();

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void testBatchQueryOrderByIdAsc() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<HistoricBatch> orderedBatches = historyService.createHistoricBatchQuery().orderById().asc().list();

    // then
    verifySorting(orderedBatches, historicBatchById());
  }

  @Test
  void testBatchQueryOrderByIdDec() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<HistoricBatch> orderedBatches = historyService.createHistoricBatchQuery().orderById().desc().list();

    // then
    verifySorting(orderedBatches, inverted(historicBatchById()));
  }

  @Test
  void testBatchQueryOrderByStartTimeAsc() {
    // given
    ClockTestUtil.setClockToDateWithoutMilliseconds();
    helper.migrateProcessInstancesAsync(1);
    ClockTestUtil.incrementClock(1000);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<HistoricBatch> orderedBatches = historyService.createHistoricBatchQuery().orderByStartTime().asc().list();

    // then
    verifySorting(orderedBatches, historicBatchByStartTime());
  }

  @Test
  void testBatchQueryOrderByStartTimeDec() {
    // given
    ClockTestUtil.setClockToDateWithoutMilliseconds();
    helper.migrateProcessInstancesAsync(1);
    ClockTestUtil.incrementClock(1000);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<HistoricBatch> orderedBatches = historyService.createHistoricBatchQuery().orderByStartTime().desc().list();

    // then
    verifySorting(orderedBatches, inverted(historicBatchByStartTime()));
  }

  @Test
  void testBatchQueryOrderByEndTimeAsc() {
    // given
    ClockTestUtil.setClockToDateWithoutMilliseconds();
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    helper.completeBatch(batch1);

    ClockTestUtil.incrementClock(1000);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);
    helper.completeBatch(batch2);

    // when
    List<HistoricBatch> orderedBatches = historyService.createHistoricBatchQuery().orderByEndTime().asc().list();

    // then
    verifySorting(orderedBatches, historicBatchByEndTime());
  }

  @Test
  void testBatchQueryOrderByEndTimeDec() {
    // given
    ClockTestUtil.setClockToDateWithoutMilliseconds();
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    helper.completeBatch(batch1);

    ClockTestUtil.incrementClock(1000);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);
    helper.completeBatch(batch2);

    // when
    List<HistoricBatch> orderedBatches = historyService.createHistoricBatchQuery().orderByEndTime().desc().list();

    // then
    verifySorting(orderedBatches, inverted(historicBatchByEndTime()));
  }

  @Test
  void testBatchQueryOrderingPropertyWithoutOrder() {
    var historicBatchQuery = historyService.createHistoricBatchQuery().orderById();
    try {
      historicBatchQuery.singleResult();
      fail("exception expected");
    }
    catch (NotValidException e) {
      assertThat(e.getMessage()).contains("Invalid query: call asc() or desc() after using orderByXX()");
    }
  }

  @Test
  void testBatchQueryOrderWithoutOrderingProperty() {
    var historicBatchQuery = historyService.createHistoricBatchQuery();
    try {
      historicBatchQuery.asc();
      fail("exception expected");
    }
    catch (NotValidException e) {
      assertThat(e.getMessage()).contains("You should call any of the orderBy methods first before specifying a direction");
    }
  }
}
