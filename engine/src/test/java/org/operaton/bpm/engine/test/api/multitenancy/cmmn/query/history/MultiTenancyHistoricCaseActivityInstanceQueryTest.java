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
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstanceQuery;
import org.operaton.bpm.engine.runtime.CaseInstanceBuilder;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicCaseActivityInstanceByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
class MultiTenancyHistoricCaseActivityInstanceQueryTest {

  protected static final String CMMN_FILE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

  protected static final String TENANT_NULL = null;
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected CaseService caseService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    // given
    testRule.deployForTenant(TENANT_NULL, CMMN_FILE);
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);

    createCaseInstance(TENANT_NULL);
    createCaseInstance(TENANT_ONE);
    createCaseInstance(TENANT_TWO);
  }

  @Test
  void shouldQueryWithoutTenantId() {
    // when
    HistoricCaseActivityInstanceQuery query = historyService
        .createHistoricCaseActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryFilterWithoutTenantId() {
    // when
    HistoricCaseActivityInstanceQuery query = historyService
        .createHistoricCaseActivityInstanceQuery()
        .withoutTenantId();

    // then
    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryByTenantId() {
    // when
    HistoricCaseActivityInstanceQuery queryTenantOne = historyService
        .createHistoricCaseActivityInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    HistoricCaseActivityInstanceQuery queryTenantTwo = historyService
        .createHistoricCaseActivityInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    // then
    assertThat(queryTenantOne.count()).isOne();
    assertThat(queryTenantTwo.count()).isOne();
  }

  @Test
  void shouldQueryByTenantIds() {
    // when
    HistoricCaseActivityInstanceQuery query = historyService
        .createHistoricCaseActivityInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByNonExistingTenantId() {
    // when
    HistoricCaseActivityInstanceQuery query = historyService
        .createHistoricCaseActivityInstanceQuery()
        .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicCaseActivityInstanceQuery = historyService.createHistoricCaseActivityInstanceQuery();

    assertThatThrownBy(() -> historicCaseActivityInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void shouldQuerySortingAsc() {
    // when
    List<HistoricCaseActivityInstance> historicCaseActivityInstances = historyService
        .createHistoricCaseActivityInstanceQuery()
        .orderByTenantId()
        .asc()
        .list();

    // then
    assertThat(historicCaseActivityInstances).hasSize(3);
    verifySorting(historicCaseActivityInstances, historicCaseActivityInstanceByTenantId());
  }

  @Test
  void shouldQuerySortingDesc() {
    // when
    List<HistoricCaseActivityInstance> historicCaseActivityInstances = historyService.createHistoricCaseActivityInstanceQuery()
        .orderByTenantId()
        .desc()
        .list();

    // then
    assertThat(historicCaseActivityInstances).hasSize(3);
    verifySorting(historicCaseActivityInstances, inverted(historicCaseActivityInstanceByTenantId()));
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    HistoricCaseActivityInstanceQuery query = historyService.createHistoricCaseActivityInstanceQuery();

    // then
    assertThat(query.count()).isOne(); // null-tenant instances are still included
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    HistoricCaseActivityInstanceQuery query = historyService.createHistoricCaseActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(2L);  // null-tenant instances are still included
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void shouldQueryAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    // when
    HistoricCaseActivityInstanceQuery query = historyService.createHistoricCaseActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3L); // null-tenant instances are still included
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void shouldQueryDisabledTenantCheck() {
    // given
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    HistoricCaseActivityInstanceQuery query = historyService.createHistoricCaseActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3L);
  }

  protected void createCaseInstance(String tenantId) {
    CaseInstanceBuilder builder = caseService.withCaseDefinitionByKey("oneTaskCase");
    builder.caseDefinitionTenantId(tenantId).create();
  }

}
