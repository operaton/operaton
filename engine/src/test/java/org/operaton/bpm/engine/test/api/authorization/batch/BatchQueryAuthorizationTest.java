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
import static org.operaton.bpm.engine.authorization.Authorization.ANY;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
class BatchQueryAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
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
      .processInstanceIds(Collections.singletonList(pi.getId()))
      .executeAsync();

    batch2 = engineRule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(Collections.singletonList(pi.getId()))
        .executeAsync();
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @AfterEach
  void deleteBatches() {
    engineRule.getManagementService().deleteBatch(batch1.getId(), true);
    engineRule.getManagementService().deleteBatch(batch2.getId(), true);
  }

  @Test
  void testQueryList() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<Batch> batches = engineRule.getManagementService().createBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(1);
    assertThat(batches.get(0).getId()).isEqualTo(batch1.getId());
  }

  @Test
  void testQueryCount() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    long count = engineRule.getManagementService().createBatchQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(count).isEqualTo(1);
  }

  @Test
  void testQueryNoAuthorizations() {
    // when
    authRule.enableAuthorization("user");
    long count = engineRule.getManagementService().createBatchQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(count).isZero();
  }

  @Test
  void testQueryListAccessAll() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<Batch> batches = engineRule.getManagementService().createBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(2);
  }

  @Test
  void testQueryListMultiple() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, batch1.getId(), "user", Permissions.READ);
    authRule.createGrantAuthorization(Resources.BATCH, ANY, "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<Batch> batches = engineRule.getManagementService().createBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).hasSize(2);
  }

  @Test
  void shouldFindEmptyBatchListWithRevokedReadPermissionOnAllBatches() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, ANY, Permissions.READ);
    authRule.createRevokeAuthorization(Resources.BATCH, ANY, "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    List<Batch> batches = engineRule.getManagementService().createBatchQuery().list();
    authRule.disableAuthorization();

    // then
    assertThat(batches).isEmpty();
  }

  @Test
  void shouldFindNoBatchWithRevokedReadPermissionOnAllBatches() {
    // given
    authRule.createGrantAuthorization(Resources.BATCH, ANY, ANY, Permissions.READ);
    authRule.createRevokeAuthorization(Resources.BATCH, ANY, "user", Permissions.READ);

    // when
    authRule.enableAuthorization("user");
    long batchCount = engineRule.getManagementService().createBatchQuery().count();
    authRule.disableAuthorization();

    // then
    assertThat(batchCount).isZero();
  }
}
