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
package org.operaton.bpm.engine.test.api.authorization.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReport;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricBatchQueryAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  static ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected MigrationPlan migrationPlan;
  protected Batch batch1;
  protected Batch batch2;

  @BeforeEach
  void setUp() {
    authRule.createUserAndGroup("user", "group");
  }

  @BeforeEach
  void deployProcessesAndCreateMigrationPlan() {
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance pi = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    batch1 = engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(pi.getId()))
      .executeAsync();

    batch2 = engineRule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(List.of(pi.getId()))
        .executeAsync();
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
    removeAllRunningAndHistoricBatches();
    engineRule.getProcessEngineConfiguration().setBatchOperationHistoryTimeToLive(null);
    engineRule.getProcessEngineConfiguration().setBatchOperationsForHistoryCleanup(null);
  }

  private void removeAllRunningAndHistoricBatches() {
    HistoryService historyService = engineRule.getHistoryService();
    ManagementService managementService = engineRule.getManagementService();

    for (Batch batch : managementService.createBatchQuery().list()) {
      managementService.deleteBatch(batch.getId(), true);
    }

    // remove history of completed batches
    for (HistoricBatch historicBatch : historyService.createHistoricBatchQuery().list()) {
      historyService.deleteHistoricBatch(historicBatch.getId());
    }
  }

  @Test
  void testQueryList() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ_HISTORY);

    // when
    authRule.enableAuthorization("user");
    List<HistoricBatch> batches = engineRule.getHistoryService().createHistoricBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(1);
    assertThat(batches.get(0).getId()).isEqualTo(batch1.getId());
  }

  @Test
  void testQueryCount() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ_HISTORY);

    // when
    authRule.enableAuthorization("user");
    long count = engineRule.getHistoryService().createHistoricBatchQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(count).isEqualTo(1);
  }

  @Test
  void testQueryNoAuthorizations() {
    // when
    authRule.enableAuthorization("user");
    long count = engineRule.getHistoryService().createHistoricBatchQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(count).isZero();
  }

  @Test
  void testQueryListAccessAll() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "user", Permissions.READ_HISTORY);

    // when
    authRule.enableAuthorization("user");
    List<HistoricBatch> batches = engineRule.getHistoryService().createHistoricBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(2);
  }

  @Test
  void testQueryListMultiple() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "user", Permissions.READ_HISTORY);
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ_HISTORY);

    // when
    authRule.enableAuthorization("user");
    List<HistoricBatch> batches = engineRule.getHistoryService().createHistoricBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(2);
  }

  @Test
  void shouldFindEmptyBatchListWithRevokedReadHistoryPermissionOnAllBatches() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, ANY, Permissions.READ_HISTORY);
    authRule.createRevokeAuthorization(Resources.BATCH, ANY, "user", Permissions.READ_HISTORY);

    // when
    authRule.enableAuthorization("user");
    List<HistoricBatch> batches = engineRule.getHistoryService().createHistoricBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).isEmpty();
  }

  @Test
  void shouldNotFindBatchWithRevokedReadHistoryPermissionOnAllBatches() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, ANY, Permissions.READ_HISTORY);
    authRule.createRevokeAuthorization(Resources.BATCH, ANY, "user", Permissions.READ_HISTORY);

    // when
    authRule.enableAuthorization("user");
    long batchCount = engineRule.getHistoryService().createHistoricBatchQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(batchCount).isZero();
  }

  @Test
  void testHistoryCleanupReportQueryWithPermissions() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "user", Permissions.READ_HISTORY);
    String migrationOperationsTTL = "P0D";
    prepareBatch(migrationOperationsTTL);

    authRule.enableAuthorization("user");
    CleanableHistoricBatchReportResult result = engineRule.getHistoryService().createCleanableHistoricBatchReport().singleResult();
    authRule.disableAuthorization();

    assertThat(result).isNotNull();
    checkResultNumbers(result, 1, 1, 0);
  }

  @Test
  void testHistoryCleanupReportQueryWithoutPermission() {
    // given
    String migrationOperationsTTL = "P0D";
    prepareBatch(migrationOperationsTTL);
    CleanableHistoricBatchReport batchReport = engineRule.getHistoryService().createCleanableHistoricBatchReport();

    authRule.enableAuthorization("user");
    try {
      assertThatThrownBy(batchReport::list)
          .isInstanceOf(AuthorizationException.class);
    } finally {
      authRule.disableAuthorization();
    }
  }

  private void prepareBatch(String migrationOperationsTTL) {
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(false);
    Map<String, String> map = new HashMap<>();
    map.put("instance-migration", migrationOperationsTTL);
    engineRule.getProcessEngineConfiguration().setBatchOperationsForHistoryCleanup(map);
    engineRule.getProcessEngineConfiguration().initHistoryCleanup();

    Date startDate = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -11));
    String batchId = createBatch();
    ClockUtil.setCurrentTime(DateUtils.addDays(startDate, -7));

    engineRule.getManagementService().deleteBatch(batchId, false);

    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);
  }

  private void checkResultNumbers(CleanableHistoricBatchReportResult result, int expectedCleanable, int expectedFinished, Integer expectedTTL) {
    assertThat(result.getCleanableBatchesCount()).isEqualTo(expectedCleanable);
    assertThat(result.getFinishedBatchesCount()).isEqualTo(expectedFinished);
    assertThat(result.getHistoryTimeToLive()).isEqualTo(expectedTTL);
  }


  private String createBatch() {
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan plan = engineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance pi = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

     Batch batch = engineRule.getRuntimeService()
      .newMigration(plan)
      .processInstanceIds(List.of(pi.getId()))
      .executeAsync();

     return batch.getId();
  }
}
