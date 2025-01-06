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
package org.operaton.bpm.engine.test.api.history;

import org.operaton.bpm.engine.CaseService;
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
import org.operaton.bpm.engine.test.api.runtime.migration.MigrationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;

import java.util.*;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricInstanceForCleanupQueryTest {

  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);
  protected MigrationTestRule migrationRule = new MigrationTestRule(engineRule);
  protected BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  @Rule public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule).around(migrationRule);

  private HistoryService historyService;
  private ManagementService managementService;
  private CaseService caseService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @Before
  public void init() {
    historyService = engineRule.getHistoryService();
    managementService = engineRule.getManagementService();
    caseService = engineRule.getCaseService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @After
  public void clearDatabase() {
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
  public void testSortHistoricBatchesForCleanup() {
    Date startDate = ClockUtil.getCurrentTime();
    int daysInThePast = -11;
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, daysInThePast));

    // given
    List<Batch> list = Arrays.asList(helper.migrateProcessInstancesAsync(1), helper.migrateProcessInstancesAsync(1), helper.migrateProcessInstancesAsync(1));

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
    assertEquals(3, historicList.size());

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

      HistoricBatchManager historicBatchManager = commandContext.getHistoricBatchManager();
      List<String> ids = historicBatchManager.findHistoricBatchIdsForCleanup(7, batchOperationsMap, 0, 59);
      assertEquals(3, ids.size());
      HistoricBatchEntity instance0 = historicBatchManager.findHistoricBatchById(ids.get(0));
      HistoricBatchEntity instance1 = historicBatchManager.findHistoricBatchById(ids.get(1));
      HistoricBatchEntity instance2 = historicBatchManager.findHistoricBatchById(ids.get(2));
      assertTrue(instance0.getEndTime().before(instance1.getEndTime()));
      assertTrue(instance1.getEndTime().before(instance2.getEndTime()));

      return null;
    });
  }

}
