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

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicIncidentByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricIncidentQueryTest {

  protected static final BpmnModelInstance BPMN = Bpmn.createExecutableProcess("failingProcess")
      .startEvent()
      .serviceTask()
        .operatonAsyncBefore()
        .operatonExpression("${failing}")
      .endEvent()
      .done();

  protected static final String TENANT_NULL = null;
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    // given
    testRule.deployForTenant(TENANT_NULL, BPMN);
    testRule.deployForTenant(TENANT_ONE, BPMN);
    testRule.deployForTenant(TENANT_TWO, BPMN);

    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_NULL);
    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_ONE);
    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_TWO);
  }

  @Test
  void shouldQueryWithoutTenantId() {
    // when
    HistoricIncidentQuery query = historyService
        .createHistoricIncidentQuery();

    // then
    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryFilterWithoutTenantId() {
    // when
    HistoricIncidentQuery query = historyService
        .createHistoricIncidentQuery()
        .withoutTenantId();

    // then
    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryByTenantId() {
    // when
    HistoricIncidentQuery queryTenantOne = historyService
        .createHistoricIncidentQuery()
        .tenantIdIn(TENANT_ONE);

    HistoricIncidentQuery queryTenantTwo = historyService
        .createHistoricIncidentQuery()
        .tenantIdIn(TENANT_TWO);

    // then
    assertThat(queryTenantOne.count()).isOne();
    assertThat(queryTenantTwo.count()).isOne();
  }

  @Test
  void shouldQueryByTenantIds() {
    // when
    HistoricIncidentQuery query = historyService
        .createHistoricIncidentQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByNonExistingTenantId() {
    // when
    HistoricIncidentQuery query = historyService
        .createHistoricIncidentQuery()
        .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicIncidentQuery = historyService.createHistoricIncidentQuery();

    assertThatThrownBy(() -> historicIncidentQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void shouldQuerySortingAsc() {
    // when
    List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery()
        .orderByTenantId()
        .asc()
        .list();

    // then
    assertThat(historicIncidents).hasSize(3);
    verifySorting(historicIncidents, historicIncidentByTenantId());
  }

  @Test
  void shouldQuerySortingDesc() {
    // when
    List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery()
        .orderByTenantId()
        .desc()
        .list();

    // then
    assertThat(historicIncidents).hasSize(3);
    verifySorting(historicIncidents, inverted(historicIncidentByTenantId()));
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    assertThat(query.count()).isOne(); // null-tenant incidents are still included
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    assertThat(query.count()).isEqualTo(2L); // null-tenant incidents are still included
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
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    assertThat(query.count()).isEqualTo(3L); // null-tenant incidents are still included
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
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    assertThat(query.count()).isEqualTo(3L);
  }

  protected void startProcessInstanceAndExecuteFailingJobForTenant(String tenant) {
    runtimeService.createProcessInstanceByKey("failingProcess").processDefinitionTenantId(tenant).execute();
    testRule.executeAvailableJobs();
  }

}
