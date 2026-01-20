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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricBatchManagerBatchesForCleanupTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  HistoryService historyService;

  @AfterEach
  void clearDatabase() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @Parameter(0)
  public int historicBatchHistoryTTL;

  @Parameter(1)
  public int daysInThePast;

  @Parameter(2)
  public int batch1EndTime;

  @Parameter(3)
  public int batch2EndTime;

  @Parameter(4)
  public int batchSize;

  @Parameter(5)
  public int resultCount;

  @Parameters
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
        // all historic batches are old enough to be cleaned up
        { 5, -11, -6, -7, 50, 2 },
        // one batch should be cleaned up
        { 5, -11, -3, -7, 50, 1 },
        // not enough time has passed
        { 5, -11, -3, -4, 50, 0 },
        // batchSize will reduce the result
        { 5, -11, -6, -7, 1, 1 } });
  }

  @SuppressWarnings("unchecked")
  @TestTemplate
  void testFindHistoricBatchIdsForCleanup() {
    // given
    String batchType = prepareHistoricBatches(2);
    final Map<String, Integer> batchOperationsMap = new HashedMap();
    batchOperationsMap.put(batchType, historicBatchHistoryTTL);


    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> {
      // when
      List<String> historicBatchIdsForCleanup = commandContext.getHistoricBatchManager().findHistoricBatchIdsForCleanup(batchSize, batchOperationsMap, 0, 59);

      // then
      assertThat(historicBatchIdsForCleanup).hasSize(resultCount);

      if (resultCount > 0) {

        List<HistoricBatch> historicBatches = historyService.createHistoricBatchQuery().list();

        for (HistoricBatch historicBatch : historicBatches) {
          historicBatch.getEndTime().before(DateUtils.addDays(ClockUtil.getCurrentTime(), historicBatchHistoryTTL));
        }
      }

      return null;
    });
  }

  private String prepareHistoricBatches(int batchesCount) {
    Date startDate = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    List<Batch> list = new ArrayList<>();
    for (int i = 0; i < batchesCount; i++) {
      list.add(helper.migrateProcessInstancesAsync(1));
    }

    Batch batch1 = list.get(0);
    String batchType = batch1.getType();
    helper.completeSeedJobs(batch1);
    helper.executeJobs(batch1);
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, batch1EndTime));
    helper.executeMonitorJob(batch1);

    Batch batch2 = list.get(1);
    helper.completeSeedJobs(batch2);
    helper.executeJobs(batch2);
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, batch2EndTime));
    helper.executeMonitorJob(batch2);

    ClockUtil.setCurrentTime(new Date());

    return batchType;
  }
}
