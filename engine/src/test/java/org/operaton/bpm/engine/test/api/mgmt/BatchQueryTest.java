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
package org.operaton.bpm.engine.test.api.mgmt;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchQuery;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.assertj.core.api.Assertions.*;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.batchById;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;

/**
 * @author Thorben Lindhauer
 *
 */
class BatchQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  protected BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
    ClockUtil.reset();
  }

  @Test
  void testBatchQuery() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);

    // when
    List<Batch> list = managementService.createBatchQuery().list();

    // then
    assertThat(list).extracting(Batch::getId).containsExactly(batch1.getId(), batch2.getId());
  }

  @Test
  void testBatchQueryResult() {
    // given
    ClockUtil.setCurrentTime(new Date());
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    Batch resultBatch = managementService.createBatchQuery().singleResult();

    // then
    assertThat(batch).isNotNull();

    assertThat(resultBatch.getId()).isEqualTo(batch.getId());
    assertThat(resultBatch.getBatchJobDefinitionId()).isEqualTo(batch.getBatchJobDefinitionId());
    assertThat(resultBatch.getMonitorJobDefinitionId()).isEqualTo(batch.getMonitorJobDefinitionId());
    assertThat(resultBatch.getSeedJobDefinitionId()).isEqualTo(batch.getSeedJobDefinitionId());
    assertThat(resultBatch.getTenantId()).isEqualTo(batch.getTenantId());
    assertThat(resultBatch.getType()).isEqualTo(batch.getType());
    assertThat(resultBatch.getBatchJobsPerSeed()).isEqualTo(batch.getBatchJobsPerSeed());
    assertThat(resultBatch.getInvocationsPerBatchJob()).isEqualTo(batch.getInvocationsPerBatchJob());
    assertThat(resultBatch.getTotalJobs()).isEqualTo(batch.getTotalJobs());
    assertThat(resultBatch.getJobsCreated()).isEqualTo(batch.getJobsCreated());
    assertThat(resultBatch.isSuspended()).isEqualTo(batch.isSuspended());
    assertThat(batch.getStartTime()).isCloseTo(resultBatch.getStartTime(), 1000);
    assertThat(batch.getStartTime()).isCloseTo(ClockUtil.getCurrentTime(), 1000);
  }

  @Test
  void testBatchQueryById() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    Batch resultBatch = managementService.createBatchQuery().batchId(batch1.getId()).singleResult();

    // then
    assertThat(resultBatch).isNotNull();
    assertThat(resultBatch.getId()).isEqualTo(batch1.getId());
  }

  @Test
  void testBatchQueryByIdNull() {
    var batchQuery = managementService.createBatchQuery();
    assertThatThrownBy(() -> batchQuery.batchId(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Batch id is null");
  }

  @Test
  void testBatchQueryByType() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = managementService.createBatchQuery().type(batch1.getType()).count();

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void testBatchQueryByNonExistingType() {
    // given
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = managementService.createBatchQuery().type("foo").count();

    // then
    assertThat(count).isZero();
  }

  @Test
  void testBatchQueryByTypeNull() {
    var batchQuery = managementService.createBatchQuery();
    assertThatThrownBy(() -> batchQuery.type(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Type is null");
  }

  @Test
  void testBatchQueryCount() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = managementService.createBatchQuery().count();

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void testBatchQueryOrderByIdAsc() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<Batch> orderedBatches = managementService.createBatchQuery().orderById().asc().list();

    // then
    verifySorting(orderedBatches, batchById());
  }

  @Test
  void testBatchQueryOrderByIdDec() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<Batch> orderedBatches = managementService.createBatchQuery().orderById().desc().list();

    // then
    verifySorting(orderedBatches, inverted(batchById()));
  }

  @Test
  void testBatchQueryOrderingPropertyWithoutOrder() {
    var batchQuery = managementService.createBatchQuery().orderById();
    assertThatThrownBy(batchQuery::singleResult)
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("Invalid query: call asc() or desc() after using orderByXX()");
  }

  @Test
  void testBatchQueryOrderWithoutOrderingProperty() {
    var batchQuery = managementService.createBatchQuery();
    assertThatThrownBy(batchQuery::asc)
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("You should call any of the orderBy methods " + "first before specifying a direction");
  }

  @Test
  void testBatchQueryBySuspendedBatches() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    managementService.suspendBatchById(batch1.getId());
    managementService.suspendBatchById(batch2.getId());
    managementService.activateBatchById(batch1.getId());

    // then
    BatchQuery query = managementService.createBatchQuery().suspended();
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult().getId()).isEqualTo(batch2.getId());
  }

  @Test
  void testBatchQueryByActiveBatches() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);
    Batch batch3 = helper.migrateProcessInstancesAsync(1);

    // when
    managementService.suspendBatchById(batch1.getId());
    managementService.suspendBatchById(batch2.getId());
    managementService.activateBatchById(batch1.getId());

    // then
    BatchQuery query = managementService.createBatchQuery().active();
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);

    assertThat(query.list()).extracting(Batch::getId).containsExactly(batch1.getId(), batch3.getId());
  }

}
