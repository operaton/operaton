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
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.batchById;
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
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchQuery;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
public class BatchQueryTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  protected BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;

  @AfterEach
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
    ClockUtil.reset();
  }

  @Test
  public void testBatchQuery() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);

    // when
    List<Batch> list = managementService.createBatchQuery().list();

    // then
    assertThat(list).hasSize(2);

    List<String> batchIds = new ArrayList<>();
    for (Batch resultBatch : list) {
      batchIds.add(resultBatch.getId());
    }

    assertThat(batchIds).containsExactly(batch1.getId(), batch2.getId());
  }

  @Test
  public void testBatchQueryResult() {
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
  public void testBatchQueryById() {
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
  public void testBatchQueryByIdNull() {
    var batchQuery = managementService.createBatchQuery();
    try {
      batchQuery.batchId(null);
      fail("exception expected");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("Batch id is null");
    }
  }

  @Test
  public void testBatchQueryByType() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = managementService.createBatchQuery().type(batch1.getType()).count();

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void testBatchQueryByNonExistingType() {
    // given
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = managementService.createBatchQuery().type("foo").count();

    // then
    assertThat(count).isZero();
  }

  @Test
  public void testBatchQueryByTypeNull() {
    var batchQuery = managementService.createBatchQuery();
    try {
      batchQuery.type(null);
      fail("exception expected");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("Type is null");
    }
  }

  @Test
  public void testBatchQueryCount() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    long count = managementService.createBatchQuery().count();

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void testBatchQueryOrderByIdAsc() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<Batch> orderedBatches = managementService.createBatchQuery().orderById().asc().list();

    // then
    verifySorting(orderedBatches, batchById());
  }

  @Test
  public void testBatchQueryOrderByIdDec() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<Batch> orderedBatches = managementService.createBatchQuery().orderById().desc().list();

    // then
    verifySorting(orderedBatches, inverted(batchById()));
  }

  @Test
  public void testBatchQueryOrderingPropertyWithoutOrder() {
    var batchQuery = managementService.createBatchQuery().orderById();
    try {
      batchQuery.singleResult();
      fail("exception expected");
    }
    catch (NotValidException e) {
      assertThat(e.getMessage()).contains("Invalid query: "
          + "call asc() or desc() after using orderByXX()");
    }
  }

  @Test
  public void testBatchQueryOrderWithoutOrderingProperty() {
    var batchQuery = managementService.createBatchQuery();
    try {
      batchQuery.asc();
      fail("exception expected");
    }
    catch (NotValidException e) {
      assertThat(e.getMessage()).contains("You should call any of the orderBy methods "
          + "first before specifying a direction");
    }
  }

  @Test
  public void testBatchQueryBySuspendedBatches() {
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
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult().getId()).isEqualTo(batch2.getId());
  }

  @Test
  public void testBatchQueryByActiveBatches() {
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

    List<String> foundIds = new ArrayList<>();
    for (Batch batch : query.list()) {
      foundIds.add(batch.getId());
    }
    assertThat(foundIds).contains(
      batch1.getId(),
      batch3.getId()
    );
  }

}
