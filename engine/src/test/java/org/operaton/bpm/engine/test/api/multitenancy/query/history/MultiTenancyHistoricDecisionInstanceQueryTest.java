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
package org.operaton.bpm.engine.test.api.multitenancy.query.history;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicDecisionInstanceByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricDecisionInstanceQueryTest {

  protected static final String DMN = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";

  protected static final String TENANT_NULL = null;
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected DecisionService decisionService;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_NULL, DMN);
    testRule.deployForTenant(TENANT_ONE, DMN);
    testRule.deployForTenant(TENANT_TWO, DMN);

    // given
    evaluateDecisionInstance();
    evaluateDecisionInstanceForTenant(TENANT_ONE);
    evaluateDecisionInstanceForTenant(TENANT_TWO);
  }

  @Test
  void shouldQueryWithoutTenantId() {
    HistoricDecisionInstanceQuery query = historyService.
        createHistoricDecisionInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryByTenantId() {
    HistoricDecisionInstanceQuery query = historyService
        .createHistoricDecisionInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = historyService
        .createHistoricDecisionInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryByTenantIds() {
    // when
    HistoricDecisionInstanceQuery query = historyService
        .createHistoricDecisionInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  public void shouldQueryByNonExistingTenantId() {
    // when
    HistoricDecisionInstanceQuery query = historyService
        .createHistoricDecisionInstanceQuery()
        .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    assertThatThrownBy(() -> historicDecisionInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void shouldQuerySortingAsc() {
    // when
    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery()
        .orderByTenantId()
        .asc()
        .list();

    // then
    assertThat(historicDecisionInstances).hasSize(3);
    verifySorting(historicDecisionInstances, historicDecisionInstanceByTenantId());
  }

  @Test
  void shouldQuerySortingDesc() {
    // when
    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery()
        .orderByTenantId()
        .desc()
        .list();

    // then
    assertThat(historicDecisionInstances).hasSize(3);
    verifySorting(historicDecisionInstances, inverted(historicDecisionInstanceByTenantId()));
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    // givem
    identityService.setAuthentication("user", null, null);

    // when
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    assertThat(query.count()).isOne(); // null-tenant instances are still visible
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(2L); // null-tenant instances are also visible
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
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3L); // null-tenant instances are also visible
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
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryFilterWithoutTenantId() {
    // when
    HistoricDecisionInstanceQuery query = historyService
        .createHistoricDecisionInstanceQuery()
        .withoutTenantId();

    //then
    assertThat(query.count()).isOne();
  }

  protected void evaluateDecisionInstanceForTenant(String tenant) {
    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery()
        .tenantIdIn(tenant)
        .singleResult()
        .getId();

    VariableMap variables = Variables.createVariables().putValue("status", "bronze");
    decisionService.evaluateDecisionTableById(decisionDefinitionId, variables);
  }

  protected void evaluateDecisionInstance() {
    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery()
         .withoutTenantId()
         .singleResult()
         .getId();

    VariableMap variables = Variables.createVariables().putValue("status", "bronze");
    decisionService.evaluateDecisionTableById(decisionDefinitionId, variables);
  }

}
