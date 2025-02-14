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

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchStatistics;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestBaseRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Thorben Lindhauer
 *
 */
public class BatchStatisticsQueryAuthorizationTest {

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public AuthorizationTestBaseRule authRule = new AuthorizationTestBaseRule(engineRule);
  public ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(authRule).around(testHelper);

  protected MigrationPlan migrationPlan;
  protected Batch batch1;
  protected Batch batch2;
  protected Batch batch3;

  @Before
  public void setUp() {
    authRule.createUserAndGroup("user", "group");
  }

  @Before
  public void deployProcessesAndCreateMigrationPlan() {
    ProcessInstance pi = createMigrationPlan();

    batch1 = engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(pi.getId()))
      .executeAsync();

    Job seedJob = engineRule.getManagementService().createJobQuery().singleResult();
    engineRule.getManagementService().executeJob(seedJob.getId());

    batch2 = engineRule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(Arrays.asList(pi.getId()))
        .executeAsync();
  }

  @After
  public void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @After
  public void deleteBatches() {
    engineRule.getManagementService().deleteBatch(batch1.getId(), true);
    engineRule.getManagementService().deleteBatch(batch2.getId(), true);
    if (batch3 != null) {
      engineRule.getManagementService().deleteBatch(batch3.getId(), true);
    }
  }

  @Test
  public void testQueryList() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<BatchStatistics> batches = engineRule.getManagementService().createBatchStatisticsQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(1);
    assertThat(batches.get(0).getId()).isEqualTo(batch1.getId());

    // and the visibility of jobs is not restricted
    assertThat(batches.get(0).getJobsCreated()).isEqualTo(1);
    assertThat(batches.get(0).getRemainingJobs()).isEqualTo(1);
    assertThat(batches.get(0).getTotalJobs()).isEqualTo(1);
  }

  @Test
  public void testQueryCount() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    long count = engineRule.getManagementService().createBatchStatisticsQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testQueryNoAuthorizations() {
    // when
    authRule.enableAuthorization("user");
    long count = engineRule.getManagementService().createBatchStatisticsQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(count).isEqualTo(0);
  }

  @Test
  public void testQueryListAccessAll() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<BatchStatistics> batches = engineRule.getManagementService().createBatchStatisticsQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(2);
  }

  @Test
  public void testQueryListMultiple() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "user", Permissions.READ);
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<BatchStatistics> batches = engineRule.getManagementService().createBatchStatisticsQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(2);
  }

  @Test
  public void testBatchStatisticsAndCreateUserId() {
    // given
    ProcessInstance pi = createMigrationPlan();

    // when
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "userId", Permissions.CREATE);
    authRule.createGrantAuthorization(Resources.PROCESS_DEFINITION, ANY, "userId", Permissions.MIGRATE_INSTANCE);

    authRule.enableAuthorization("userId");
    batch3 = engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(pi.getId()))
      .executeAsync();
    authRule.disableAuthorization();

    // then
    BatchStatistics batchStatistics = engineRule.getManagementService().createBatchStatisticsQuery().batchId(batch3.getId()).singleResult();
    assertThat(batchStatistics.getCreateUserId()).isEqualTo("userId");
  }

  @Test
  public void shouldNotFindStatisticsWithRevokedReadPermissionOnBatch() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, ANY, Permissions.READ);
    authRule.createRevokeAuthorization(Resources.BATCH, ANY, "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<BatchStatistics> batches = engineRule.getManagementService().createBatchStatisticsQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).isEmpty();
  }

  protected ProcessInstance createMigrationPlan() {
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    return engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
  }
}
