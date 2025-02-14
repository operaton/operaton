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
package org.operaton.bpm.engine.test.api.multitenancy.query;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.batchByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.batchStatisticsByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchStatistics;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Thorben Lindhauer
 *
 */
public class MultiTenancyBatchQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain defaultRuleChin = RuleChain.outerRule(engineRule).around(testHelper);

  protected BatchMigrationHelper batchHelper = new BatchMigrationHelper(engineRule);

  protected ManagementService managementService;
  protected IdentityService identityService;

  protected Batch sharedBatch;
  protected Batch tenant1Batch;
  protected Batch tenant2Batch;

  @Before
  public void initServices() {
    managementService= engineRule.getManagementService();
    identityService = engineRule.getIdentityService();
  }

  @Before
  public void deployProcesses() {
    ProcessDefinition sharedDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenant1Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenant2Definition = testHelper.deployForTenantAndGetDefinition(TENANT_TWO, ProcessModels.ONE_TASK_PROCESS);

    sharedBatch = batchHelper.migrateProcessInstanceAsync(sharedDefinition, sharedDefinition);
    tenant1Batch = batchHelper.migrateProcessInstanceAsync(tenant1Definition, tenant1Definition);
    tenant2Batch = batchHelper.migrateProcessInstanceAsync(tenant2Definition, tenant2Definition);
  }

  @After
  public void removeBatches() {
    batchHelper.removeAllRunningAndHistoricBatches();
  }

  @Test
  public void testBatchQueryNoAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, null);

    // then
    List<Batch> batches = managementService.createBatchQuery().list();
    assertThat(batches).hasSize(1);
    assertThat(batches.get(0).getId()).isEqualTo(sharedBatch.getId());

    assertThat(managementService.createBatchQuery().count()).isEqualTo(1);

    identityService.clearAuthentication();
  }

  @Test
  public void testBatchQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));

    // when
    List<Batch> batches = managementService.createBatchQuery().list();

    // then
    assertThat(batches).hasSize(2);
    assertBatches(batches, tenant1Batch.getId(), sharedBatch.getId());

    assertThat(managementService.createBatchQuery().count()).isEqualTo(2);

    identityService.clearAuthentication();
  }

  @Test
  public void testBatchQueryAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE, TENANT_TWO));

    // when
    List<Batch> batches = managementService.createBatchQuery().list();

    // then
    assertThat(batches).hasSize(3);
    assertThat(managementService.createBatchQuery().count()).isEqualTo(3);

    identityService.clearAuthentication();
  }

  @Test
  public void testBatchStatisticsNoAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery().list();

    // then
    assertThat(statistics).hasSize(1);
    assertThat(statistics.get(0).getId()).isEqualTo(sharedBatch.getId());

    assertThat(managementService.createBatchStatisticsQuery().count()).isEqualTo(1);

    identityService.clearAuthentication();
  }

  @Test
  public void testBatchStatisticsAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, singletonList(TENANT_ONE));

    // when
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery().list();

    // then
    assertThat(statistics).hasSize(2);

    assertThat(managementService.createBatchStatisticsQuery().count()).isEqualTo(2);

    identityService.clearAuthentication();
  }

  @Test
  public void testBatchStatisticsAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE, TENANT_TWO));

    // then
    List<BatchStatistics> statistics = managementService.createBatchStatisticsQuery().list();
    assertThat(statistics).hasSize(3);

    assertThat(managementService.createBatchStatisticsQuery().count()).isEqualTo(3);

    identityService.clearAuthentication();
  }

  @Test
  public void testBatchQueryFilterByTenant() {
    // when
    Batch returnedBatch = managementService.createBatchQuery().tenantIdIn(TENANT_ONE).singleResult();

    // then
    assertThat(returnedBatch).isNotNull();
    assertThat(returnedBatch.getId()).isEqualTo(tenant1Batch.getId());
  }

  @Test
  public void testBatchQueryFilterByTenants() {
    // when
    List<Batch> returnedBatches = managementService.createBatchQuery()
      .tenantIdIn(TENANT_ONE, TENANT_TWO)
      .orderByTenantId()
      .asc()
      .list();

    // then
    assertThat(returnedBatches).hasSize(2);
    assertThat(returnedBatches.get(0).getId()).isEqualTo(tenant1Batch.getId());
    assertThat(returnedBatches.get(1).getId()).isEqualTo(tenant2Batch.getId());
  }

  @Test
  public void testBatchQueryFilterWithoutTenantId() {
    // when
    Batch returnedBatch = managementService.createBatchQuery().withoutTenantId().singleResult();

    // then
    assertThat(returnedBatch).isNotNull();
    assertThat(returnedBatch.getId()).isEqualTo(sharedBatch.getId());
  }

  @Test
  public void testBatchQueryFailOnNullTenantIdCase1() {

    String[] tenantIds = null;
    var batchQuery = managementService.createBatchQuery();
    try {
      batchQuery.tenantIdIn(tenantIds);
      fail("exception expected");
    }
    catch (NullValueException e) {
      // happy path
    }
  }

  @Test
  public void testBatchQueryFailOnNullTenantIdCase2() {

    String[] tenantIds = new String[]{ null };
    var batchQuery = managementService.createBatchQuery();
    try {
      batchQuery.tenantIdIn(tenantIds);
      fail("exception expected");
    }
    catch (NullValueException e) {
      // happy path
    }
  }

  @Test
  public void testOrderByTenantIdAsc() {

    // when
    List<Batch> orderedBatches = managementService.createBatchQuery().orderByTenantId().asc().list();

    // then
    verifySorting(orderedBatches, batchByTenantId());
  }

  @Test
  public void testOrderByTenantIdDesc() {

    // when
    List<Batch> orderedBatches = managementService.createBatchQuery().orderByTenantId().desc().list();

    // then
    verifySorting(orderedBatches, inverted(batchByTenantId()));
  }

  @Test
  public void testBatchStatisticsQueryFilterByTenant() {
    // when
    BatchStatistics returnedBatch = managementService.createBatchStatisticsQuery().tenantIdIn(TENANT_ONE).singleResult();

    // then
    assertThat(returnedBatch).isNotNull();
    assertThat(returnedBatch.getId()).isEqualTo(tenant1Batch.getId());
  }

  @Test
  public void testBatchStatisticsQueryFilterByTenants() {
    // when
    List<BatchStatistics> returnedBatches = managementService.createBatchStatisticsQuery()
      .tenantIdIn(TENANT_ONE, TENANT_TWO)
      .orderByTenantId()
      .asc()
      .list();

    // then
    assertThat(returnedBatches).hasSize(2);
    assertThat(returnedBatches.get(0).getId()).isEqualTo(tenant1Batch.getId());
    assertThat(returnedBatches.get(1).getId()).isEqualTo(tenant2Batch.getId());
  }

  @Test
  public void testBatchStatisticsQueryFilterWithoutTenantId() {
    // when
    BatchStatistics returnedBatch = managementService.createBatchStatisticsQuery().withoutTenantId().singleResult();

    // then
    assertThat(returnedBatch).isNotNull();
    assertThat(returnedBatch.getId()).isEqualTo(sharedBatch.getId());
  }

  @Test
  public void testBatchStatisticsQueryFailOnNullTenantIdCase1() {

    String[] tenantIds = null;
    var batchStatisticsQuery = managementService.createBatchStatisticsQuery();
    try {
      batchStatisticsQuery.tenantIdIn(tenantIds);
      fail("exception expected");
    }
    catch (NullValueException e) {
      // happy path
    }
  }

  @Test
  public void testBatchStatisticsQueryFailOnNullTenantIdCase2() {

    String[] tenantIds = new String[]{ null };
    var batchStatisticsQuery = managementService.createBatchStatisticsQuery();
    try {
      batchStatisticsQuery.tenantIdIn(tenantIds);
      fail("exception expected");
    }
    catch (NullValueException e) {
      // happy path
    }
  }

  @Test
  public void testBatchStatisticsQueryOrderByTenantIdAsc() {
    // when
    List<BatchStatistics> orderedBatches = managementService.createBatchStatisticsQuery().orderByTenantId().asc().list();

    // then
    verifySorting(orderedBatches, batchStatisticsByTenantId());
  }

  @Test
  public void testBatchStatisticsQueryOrderByTenantIdDesc() {
    // when
    List<BatchStatistics> orderedBatches = managementService.createBatchStatisticsQuery().orderByTenantId().desc().list();

    // then
    verifySorting(orderedBatches, inverted(batchStatisticsByTenantId()));
  }

  protected void assertBatches(List<? extends Batch> actualBatches, String... expectedIds) {
    assertThat(actualBatches).hasSize(expectedIds.length);

    Set<String> actualIds = new HashSet<>();
    for (Batch batch : actualBatches) {
      actualIds.add(batch.getId());
    }

    for (String expectedId : expectedIds) {
      assertThat(actualIds).contains(expectedId);
    }
  }
}
