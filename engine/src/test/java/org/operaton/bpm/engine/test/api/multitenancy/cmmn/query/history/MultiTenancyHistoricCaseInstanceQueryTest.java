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
package org.operaton.bpm.engine.test.api.multitenancy.cmmn.query.history;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.history.HistoricCaseInstanceQuery;
import org.operaton.bpm.engine.runtime.CaseInstanceBuilder;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
class MultiTenancyHistoricCaseInstanceQueryTest {

  protected static final String CMMN_FILE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected CaseService caseService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    testRule.deploy(CMMN_FILE);
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);

    caseService.withCaseDefinitionByKey("oneTaskCase").caseDefinitionWithoutTenantId().create();
    createCaseInstance(TENANT_ONE);
    createCaseInstance(TENANT_TWO);
  }

  @Test
  void shouldQueryNoTenantIdSet() {
    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryByTenantId() {
    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = historyService
        .createHistoricCaseInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryByTenantIds() {
    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByInstancesWithoutTenantId() {
    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryByNonExistingTenantId() {
    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicCaseInstanceQuery = historyService.createHistoricCaseInstanceQuery();

    assertThatThrownBy(() -> historicCaseInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void shouldQuerySortingAsc() {
    // exclude historic case instances without tenant id because of database-specific ordering
    List<HistoricCaseInstance> historicCaseInstances = historyService.createHistoricCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(historicCaseInstances).hasSize(2);
    assertThat(historicCaseInstances.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(historicCaseInstances.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void shouldQuerySortingDesc() {
    // exclude historic case instances without tenant id because of database-specific ordering
    List<HistoricCaseInstance> historicCaseInstances = historyService.createHistoricCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(historicCaseInstances).hasSize(2);
    assertThat(historicCaseInstances.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(historicCaseInstances.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void shouldQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void shouldQueryDisabledTenantCheck() {
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

  protected void createCaseInstance(String tenantId) {
    CaseInstanceBuilder builder = caseService.withCaseDefinitionByKey("oneTaskCase");
    builder.caseDefinitionTenantId(tenantId).create();
  }

}
