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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.impl.batch.history.HistoricBatchEntity;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricBatchManager;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricInstanceForCleanupQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  private HistoryService historyService;
  private ManagementService managementService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @AfterEach
  void clearDatabase() {
    helper.removeAllRunningAndHistoricBatches();

    clearMetrics();
  }

  protected void clearMetrics() {
    Collection<Meter> meters = processEngineConfiguration.getMetricsRegistry().getDbMeters().values();
    for (Meter meter : meters) {
      meter.getAndClear();
    }
    managementService.deleteMetrics(null);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testSortHistoricBatchesForCleanup() {
    Date startDate = ClockUtil.getCurrentTime();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    // given
    List<Batch> list = List.of(helper.migrateProcessInstancesAsync(1), helper.migrateProcessInstancesAsync(1), helper.migrateProcessInstancesAsync(1));

    String batchType = list.get(0).getType();
    final Map<String, Integer> batchOperationsMap = new HashedMap();
    batchOperationsMap.put(batchType, 4);

    for (Batch batch : list) {
      helper.completeSeedJobs(batch);
      helper.executeJobs(batch);

      ClockUtil.setCurrentTime(DateUtils.addDays(startDate, ++daysInThePast));
      helper.executeMonitorJob(batch);
    }

    ClockUtil.setCurrentTime(new Date());
    // when
    List<HistoricBatch> historicList = historyService.createHistoricBatchQuery().list();
    assertThat(historicList).hasSize(3);

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

      HistoricBatchManager historicBatchManager = commandContext.getHistoricBatchManager();
      List<String> ids = historicBatchManager.findHistoricBatchIdsForCleanup(7, batchOperationsMap, 0, 59);
      assertThat(ids).hasSize(3);
      HistoricBatchEntity instance0 = historicBatchManager.findHistoricBatchById(ids.get(0));
      HistoricBatchEntity instance1 = historicBatchManager.findHistoricBatchById(ids.get(1));
      HistoricBatchEntity instance2 = historicBatchManager.findHistoricBatchById(ids.get(2));
      assertThat(instance0.getEndTime().before(instance1.getEndTime())).isTrue();
      assertThat(instance1.getEndTime().before(instance2.getEndTime())).isTrue();

      return null;
    });
  }

}
