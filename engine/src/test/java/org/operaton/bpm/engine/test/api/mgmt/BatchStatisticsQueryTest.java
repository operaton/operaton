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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.batchStatisticsById;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.batchStatisticsByStartTime;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchStatistics;
import org.operaton.bpm.engine.batch.BatchStatisticsQuery;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

class BatchStatisticsQueryTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  protected BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  protected ManagementService managementService;
  protected int defaultBatchJobsPerSeed;

  @BeforeEach
  void saveAndReduceBatchJobsPerSeed() {
    ProcessEngineConfigurationImpl configuration = engineRule.getProcessEngineConfiguration();
    defaultBatchJobsPerSeed = configuration.getBatchJobsPerSeed();
    // reduce number of batch jobs per seed to not have to create a lot of instances
    configuration.setBatchJobsPerSeed(10);
  }

  @AfterEach
  void resetBatchJobsPerSeed() {
    engineRule.getProcessEngineConfiguration()
      .setBatchJobsPerSeed(defaultBatchJobsPerSeed);
    ClockUtil.reset();
  }

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @Test
  void testQuery() {
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery().list();
    assertThat(statistics).isEmpty();

    Batch batch1 = helper.createMigrationBatchWithSize(1);

    statistics = managementService.createBatchStatisticsQuery().list();
    assertThat(statistics).hasSize(1);
    assertThat(statistics.get(0).getId()).isEqualTo(batch1.getId());

    Batch batch2 = helper.createMigrationBatchWithSize(1);
    Batch batch3 = helper.createMigrationBatchWithSize(1);

    statistics = managementService.createBatchStatisticsQuery().list();
    assertThat(statistics).hasSize(3);

    helper.completeBatch(batch1);
    helper.completeBatch(batch3);

    statistics = managementService.createBatchStatisticsQuery().list();
    assertThat(statistics).hasSize(1);
    assertThat(statistics.get(0).getId()).isEqualTo(batch2.getId());

    helper.completeBatch(batch2);

    statistics = managementService.createBatchStatisticsQuery().list();
    assertThat(statistics).isEmpty();
  }

  @Test
  void testQueryCount() {
    long count = managementService.createBatchStatisticsQuery().count();
    assertThat(count).isZero();

    Batch batch1 = helper.createMigrationBatchWithSize(1);

    count = managementService.createBatchStatisticsQuery().count();
    assertThat(count).isEqualTo(1);

    Batch batch2 = helper.createMigrationBatchWithSize(1);
    Batch batch3 = helper.createMigrationBatchWithSize(1);

    count = managementService.createBatchStatisticsQuery().count();
    assertThat(count).isEqualTo(3);

    helper.completeBatch(batch1);
    helper.completeBatch(batch3);

    count = managementService.createBatchStatisticsQuery().count();
    assertThat(count).isEqualTo(1);

    helper.completeBatch(batch2);

    count = managementService.createBatchStatisticsQuery().count();
    assertThat(count).isZero();
  }

  @Test
  void testQueryById() {
    // given
    helper.createMigrationBatchWithSize(1);
    Batch batch = helper.createMigrationBatchWithSize(1);

    // when
    BatchStatistics statistics = managementService.createBatchStatisticsQuery()
      .batchId(batch.getId())
      .singleResult();

    // then
    assertThat(statistics.getId()).isEqualTo(batch.getId());
  }

  @Test
  void testQueryByNullId() {
    // given
    var batchStatisticsQuery = managementService.createBatchStatisticsQuery();
    // when
    assertThatThrownBy(() -> batchStatisticsQuery.batchId(null))
      // then
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Batch id is null");
  }

  @Test
  void testQueryByUnknownId() {
    // given
    helper.createMigrationBatchWithSize(1);
    helper.createMigrationBatchWithSize(1);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery()
      .batchId("unknown")
      .list();

    // then
    assertThat(statistics).isEmpty();
  }

  @Test
  void testQueryByType() {
    // given
    helper.createMigrationBatchWithSize(1);
    helper.createMigrationBatchWithSize(1);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery()
      .type(Batch.TYPE_PROCESS_INSTANCE_MIGRATION)
      .list();

    // then
    assertThat(statistics).hasSize(2);
  }

  @Test
  void testQueryByNullType() {
    // given
    var batchStatisticsQuery = managementService.createBatchStatisticsQuery();
    // when
    assertThatThrownBy(() -> batchStatisticsQuery.type(null))
      // then
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Type is null");
  }

  @Test
  void testQueryByUnknownType() {
    // given
    helper.createMigrationBatchWithSize(1);
    helper.createMigrationBatchWithSize(1);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery()
      .type("unknown")
      .list();

    // then
    assertThat(statistics).isEmpty();
  }

  @Test
  void testQueryOrderByIdAsc() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery()
      .orderById().asc()
      .list();

    // then
    verifySorting(statistics, batchStatisticsById());
  }

  @Test
  void testQueryOrderByIdDec() {
    // given
    helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery()
      .orderById().desc()
      .list();

    // then
    verifySorting(statistics, inverted(batchStatisticsById()));
  }

  @Test
  void testQueryOrderByStartTimeAsc() {
    // given
    helper.migrateProcessInstancesAsync(1);
    final long oneHour = 60 * 60 * 1000L;
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + oneHour));
    helper.migrateProcessInstancesAsync(1);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery()
      .orderByStartTime().asc()
      .list();

    // then
    verifySorting(statistics, batchStatisticsByStartTime());
  }

  @Test
  void testQueryOrderByStartTimeDesc() {
    // given
    helper.migrateProcessInstancesAsync(1);
    final long oneHour = 60 * 60 * 1000L;
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + oneHour));
    helper.migrateProcessInstancesAsync(1);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery()
      .orderByStartTime().desc()
      .list();

    // then
    verifySorting(statistics, inverted(batchStatisticsByStartTime()));
  }

  @Test
  void testQueryOrderingPropertyWithoutOrder() {
    // given
    var batchStatisticsQuery = managementService.createBatchStatisticsQuery().orderById();
    // when
    assertThatThrownBy(batchStatisticsQuery::list)
      // then
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("Invalid query: call asc() or desc() after using orderByXX()");
  }

  @Test
  void testQueryOrderWithoutOrderingProperty() {
    // given
    BatchStatisticsQuery batchStatisticsQuery = managementService.createBatchStatisticsQuery();
    // when
    assertThatThrownBy(batchStatisticsQuery::asc)
      // then
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("You should call any of the orderBy methods first before specifying a direction");
  }

  @Test
  void testStatisticsNoExecutionJobsGenerated() {
    // given
    helper.createMigrationBatchWithSize(3);

    // when
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    // then
    assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
    assertThat(batchStatistics.getJobsCreated()).isZero();
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(3);
    assertThat(batchStatistics.getCompletedJobs()).isZero();
    assertThat(batchStatistics.getFailedJobs()).isZero();
  }

  @Test
  void testStatisticsMostExecutionJobsGenerated() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(13);

    // when
    helper.executeJob(helper.getSeedJob(batch));

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(13);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(10);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(13);
    assertThat(batchStatistics.getCompletedJobs()).isZero();
    assertThat(batchStatistics.getFailedJobs()).isZero();
  }

  @Test
  void testStatisticsAllExecutionJobsGenerated() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(3);

    // when
    helper.completeSeedJobs(batch);

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(3);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(3);
    assertThat(batchStatistics.getCompletedJobs()).isZero();
    assertThat(batchStatistics.getFailedJobs()).isZero();
  }

  @Test
  void testStatisticsOneCompletedJob() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(3);

    // when
    helper.completeSeedJobs(batch);
    helper.completeJobs(batch, 1);

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(3);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(2);
    assertThat(batchStatistics.getCompletedJobs()).isEqualTo(1);
    assertThat(batchStatistics.getFailedJobs()).isZero();
  }

  @Test
  void testStatisticsOneFailedJob() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(3);

    // when
    helper.completeSeedJobs(batch);
    helper.failExecutionJobs(batch, 1);

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(3);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(3);
    assertThat(batchStatistics.getCompletedJobs()).isZero();
    assertThat(batchStatistics.getFailedJobs()).isEqualTo(1);
  }

  @Test
  void testStatisticsOneCompletedAndOneFailedJob() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(3);

    // when
    helper.completeSeedJobs(batch);
    helper.completeJobs(batch, 1);
    helper.failExecutionJobs(batch, 1);

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(3);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(2);
    assertThat(batchStatistics.getCompletedJobs()).isEqualTo(1);
    assertThat(batchStatistics.getFailedJobs()).isEqualTo(1);
  }

  @Test
  void testStatisticsRetriedFailedJobs() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(3);

    // when
    helper.completeSeedJobs(batch);
    helper.failExecutionJobs(batch, 3);

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(3);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(3);
    assertThat(batchStatistics.getCompletedJobs()).isZero();
    assertThat(batchStatistics.getFailedJobs()).isEqualTo(3);

    // when
    helper.setRetries(batch, 3, 1);
    helper.completeJobs(batch, 3);

    // then
    batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(3);
    assertThat(batchStatistics.getRemainingJobs()).isZero();
    assertThat(batchStatistics.getCompletedJobs()).isEqualTo(3);
    assertThat(batchStatistics.getFailedJobs()).isZero();
  }

  @Test
  void testStatisticsWithDeletedJobs() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(13);

    // when
    helper.executeJob(helper.getSeedJob(batch));
    deleteMigrationJobs(batch);

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(13);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(10);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(3);
    assertThat(batchStatistics.getCompletedJobs()).isEqualTo(10);
    assertThat(batchStatistics.getFailedJobs()).isZero();
  }

  @Test
  void testStatisticsWithNotAllGeneratedAndAlreadyCompletedAndFailedJobs() {
    // given
    Batch batch = helper.createMigrationBatchWithSize(13);

    // when
    helper.executeJob(helper.getSeedJob(batch));
    helper.completeJobs(batch, 2);
    helper.failExecutionJobs(batch, 2);

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery()
      .singleResult();

    assertThat(batchStatistics.getTotalJobs()).isEqualTo(13);
    assertThat(batchStatistics.getJobsCreated()).isEqualTo(10);
    assertThat(batchStatistics.getRemainingJobs()).isEqualTo(11);
    assertThat(batchStatistics.getCompletedJobs()).isEqualTo(2);
    assertThat(batchStatistics.getFailedJobs()).isEqualTo(2);
  }

  @Test
  void testMultipleBatchesStatistics() {
    // given
    Batch batch1 = helper.createMigrationBatchWithSize(3);
    Batch batch2 = helper.createMigrationBatchWithSize(13);
    Batch batch3 = helper.createMigrationBatchWithSize(15);

    // when
    helper.executeJob(helper.getSeedJob(batch2));
    helper.completeJobs(batch2, 2);
    helper.failExecutionJobs(batch2, 3);

    helper.executeJob(helper.getSeedJob(batch3));
    deleteMigrationJobs(batch3);
    helper.executeJob(helper.getSeedJob(batch3));
    helper.completeJobs(batch3, 2);
    helper.failExecutionJobs(batch3, 3);

    // then
    List<BatchStatistics> batchStatisticsList = managementService.createBatchStatisticsQuery()
      .list();

    for (BatchStatistics batchStatistics : batchStatisticsList) {
      if (batch1.getId().equals(batchStatistics.getId())) {
        // batch 1
        assertThat(batchStatistics.getTotalJobs()).isEqualTo(3);
        assertThat(batchStatistics.getJobsCreated()).isZero();
        assertThat(batchStatistics.getRemainingJobs()).isEqualTo(3);
        assertThat(batchStatistics.getCompletedJobs()).isZero();
        assertThat(batchStatistics.getFailedJobs()).isZero();
      }
      else if (batch2.getId().equals(batchStatistics.getId())) {
        // batch 2
        assertThat(batchStatistics.getTotalJobs()).isEqualTo(13);
        assertThat(batchStatistics.getJobsCreated()).isEqualTo(10);
        assertThat(batchStatistics.getRemainingJobs()).isEqualTo(11);
        assertThat(batchStatistics.getCompletedJobs()).isEqualTo(2);
        assertThat(batchStatistics.getFailedJobs()).isEqualTo(3);
      }
      else if (batch3.getId().equals(batchStatistics.getId())) {
        // batch 3
        assertThat(batchStatistics.getTotalJobs()).isEqualTo(15);
        assertThat(batchStatistics.getJobsCreated()).isEqualTo(15);
        assertThat(batchStatistics.getRemainingJobs()).isEqualTo(3);
        assertThat(batchStatistics.getCompletedJobs()).isEqualTo(12);
        assertThat(batchStatistics.getFailedJobs()).isEqualTo(3);
      }
    }
  }

  @Test
  void testStatisticsSuspend() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    managementService.suspendBatchById(batch.getId());

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery().batchId(batch.getId()).singleResult();

    assertThat(batchStatistics.isSuspended()).isTrue();
  }

  @Test
  void testStatisticsActivate() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(1);
    managementService.suspendBatchById(batch.getId());

    // when
    managementService.activateBatchById(batch.getId());

    // then
    BatchStatistics batchStatistics = managementService.createBatchStatisticsQuery().batchId(batch.getId()).singleResult();

    assertThat(batchStatistics.isSuspended()).isFalse();
  }

  @Test
  void testStatisticsQueryBySuspendedBatches() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);
    helper.migrateProcessInstancesAsync(1);

    // when
    managementService.suspendBatchById(batch1.getId());
    managementService.suspendBatchById(batch2.getId());
    managementService.activateBatchById(batch1.getId());

    // then
    BatchStatisticsQuery query = managementService.createBatchStatisticsQuery().suspended();
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult().getId()).isEqualTo(batch2.getId());
  }

  @Test
  void testStatisticsQueryByActiveBatches() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(1);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);
    Batch batch3 = helper.migrateProcessInstancesAsync(1);

    // when
    managementService.suspendBatchById(batch1.getId());
    managementService.suspendBatchById(batch2.getId());
    managementService.activateBatchById(batch1.getId());

    // then
    BatchStatisticsQuery query = managementService.createBatchStatisticsQuery().active();
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

  @Test
  void shouldQueryByFailures() {
    // given
    Batch batch1 = helper.migrateProcessInstancesAsync(2);
    Batch batch2 = helper.migrateProcessInstancesAsync(1);
    helper.executeSeedJob(batch1);
    helper.executeSeedJob(batch2);
    helper.failExecutionJobs(batch1, 1);

    // when
    final long total = managementService.createBatchStatisticsQuery().count();
    BatchStatisticsQuery queryWithFailures = managementService.createBatchStatisticsQuery().withFailures();
    BatchStatisticsQuery queryWithoutFailures = managementService.createBatchStatisticsQuery().withoutFailures();

    // then
    assertThat(total).isEqualTo(2);

    final List<BatchStatistics> batchWithFailures = queryWithFailures.list();
    assertThat(batchWithFailures).hasSize(1);
    assertThat(queryWithFailures.count()).isEqualTo(1);
    final BatchStatistics batch1Statistics = batchWithFailures.get(0);
    assertThat(batch1Statistics.getId()).isEqualTo(batch1.getId());
    assertThat(batch1Statistics.getFailedJobs()).isEqualTo(1);
    assertThat(batch1Statistics.getTotalJobs()).isEqualTo(2);

    final List<BatchStatistics> batchWithoutFailures = queryWithoutFailures.list();
    assertThat(batchWithoutFailures).hasSize(1);
    assertThat(queryWithoutFailures.count()).isEqualTo(1);
    final BatchStatistics batch2Statistics = batchWithoutFailures.get(0);
    assertThat(batch2Statistics.getId()).isEqualTo(batch2.getId());
    assertThat(batch2Statistics.getFailedJobs()).isZero();
    assertThat(batch2Statistics.getTotalJobs()).isEqualTo(1);
  }

  @Test
  void shouldQueryByStartedAfter() {
    // given
    final long oneMin = 60 * 1000L;
    ClockUtil.setCurrentTime(ClockUtil.getCurrentTime());
    final Date oneMinLater = new Date(ClockUtil.getCurrentTime().getTime() + oneMin);
    final Date oneMinEarlier= new Date(ClockUtil.getCurrentTime().getTime() - oneMin);
    final Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    BatchStatisticsQuery query1 = managementService.createBatchStatisticsQuery().startedAfter(oneMinEarlier);
    BatchStatisticsQuery query2 = managementService.createBatchStatisticsQuery().startedAfter(oneMinLater);

    // then
    final List<BatchStatistics> batchStatistics = query1.list();
    assertThat(batchStatistics.get(0).getId()).isEqualTo(batch.getId());
    assertThat(batchStatistics.get(0).getStartTime()).isCloseTo(ClockUtil.getCurrentTime(), 1000);
    assertThat(batchStatistics).hasSize(1);
    assertThat(query1.count()).isEqualTo(1);

    assertThat(query2.count()).isZero();
    assertThat(query2.list()).isEmpty();
  }

  @Test
  void shouldQueryByStartedBefore() {
    // given
    final long oneMin = 60 * 1000L;
    ClockUtil.setCurrentTime(ClockUtil.getCurrentTime());
    final Date oneMinLater = new Date(ClockUtil.getCurrentTime().getTime() + oneMin);
    final Date oneMinEarlier= new Date(ClockUtil.getCurrentTime().getTime() - oneMin);
    final Batch batch = helper.migrateProcessInstancesAsync(1);

    // when
    BatchStatisticsQuery query1 = managementService.createBatchStatisticsQuery().startedBefore(oneMinEarlier);
    BatchStatisticsQuery query2 = managementService.createBatchStatisticsQuery().startedBefore(oneMinLater);

    // then
    assertThat(query1.count()).isZero();
    assertThat(query1.list()).isEmpty();

    final List<BatchStatistics> batchStatistics = query2.list();
    assertThat(batchStatistics.get(0).getId()).isEqualTo(batch.getId());
    assertThat(batchStatistics.get(0).getStartTime()).isCloseTo(ClockUtil.getCurrentTime(), 1000);
    assertThat(batchStatistics).hasSize(1);
    assertThat(query2.count()).isEqualTo(1);
  }

  @Test
  void shouldQueryByCreatedBy() {
    // given
    engineRule.getIdentityService().setAuthenticatedUserId("user1");
    final Batch batch1 = helper.migrateProcessInstancesAsync(1);
    engineRule.getIdentityService().setAuthenticatedUserId("user2");
    final Batch batch2 = helper.migrateProcessInstancesAsync(1);

    // when
    BatchStatisticsQuery query1 = managementService.createBatchStatisticsQuery().createdBy("user1");
    BatchStatisticsQuery query2 = managementService.createBatchStatisticsQuery().createdBy("user2");
    BatchStatisticsQuery query3 = managementService.createBatchStatisticsQuery().createdBy("user3");

    // then
    final BatchStatistics user1Batch = query1.list().get(0);
    assertThat(user1Batch.getId()).isEqualTo(batch1.getId());
    assertThat(user1Batch.getCreateUserId()).isEqualTo(batch1.getCreateUserId());
    assertThat(query1.list()).hasSize(1);
    assertThat(query1.count()).isEqualTo(1);

    final BatchStatistics user2Batch = query2.list().get(0);
    assertThat(user2Batch.getId()).isEqualTo(batch2.getId());
    assertThat(user2Batch.getCreateUserId()).isEqualTo(batch2.getCreateUserId());
    assertThat(query2.list()).hasSize(1);
    assertThat(query2.count()).isEqualTo(1);

    assertThat(query3.list()).isEmpty();
    assertThat(query3.count()).isZero();
  }

  protected void deleteMigrationJobs(Batch batch) {
    for (Job migrationJob: helper.getExecutionJobs(batch)) {
      managementService.deleteJob(migrationJob.getId());
    }
  }

}
