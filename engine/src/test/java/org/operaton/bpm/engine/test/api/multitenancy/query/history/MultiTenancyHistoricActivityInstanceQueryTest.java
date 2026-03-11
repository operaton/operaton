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
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicActivityInstanceByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
class MultiTenancyHistoricActivityInstanceQueryTest {

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
    BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .endEvent()
    .done();

    // given
    testRule.deployForTenant(TENANT_NULL, oneTaskProcess);
    testRule.deployForTenant(TENANT_ONE, oneTaskProcess);
    testRule.deployForTenant(TENANT_TWO, oneTaskProcess);

    startProcessInstanceForTenant(TENANT_NULL);
    startProcessInstanceForTenant(TENANT_ONE);
    startProcessInstanceForTenant(TENANT_TWO);
  }

  @Test
  void shouldQueryWithoutTenantId() {
    // when
    HistoricActivityInstanceQuery query = historyService
        .createHistoricActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(6L);
  }

  @Test
  void shouldQueryFilterWithoutTenantId() {
    // when
    HistoricActivityInstanceQuery query = historyService
        .createHistoricActivityInstanceQuery()
        .withoutTenantId();

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByTenantId() {
    // when
    HistoricActivityInstanceQuery queryTenantOne = historyService
        .createHistoricActivityInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    HistoricActivityInstanceQuery queryTenantTwo = historyService
        .createHistoricActivityInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    // then
    assertThat(queryTenantOne.count()).isEqualTo(2L);
    assertThat(queryTenantTwo.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByTenantIds() {
    // when
    HistoricActivityInstanceQuery query = historyService
        .createHistoricActivityInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(4L);
  }

  @Test
  void shouldQueryByNonExistingTenantId() {
    // when
    HistoricActivityInstanceQuery query = historyService
        .createHistoricActivityInstanceQuery()
        .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery();

    assertThatThrownBy(() -> historicActivityInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void shouldQuerySortingAsc() {
    // when
    List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
        .orderByTenantId()
        .asc()
        .list();

    // then
    assertThat(historicActivityInstances).hasSize(6);
    verifySorting(historicActivityInstances, historicActivityInstanceByTenantId());
  }

  @Test
  void shouldQuerySortingDesc() {
    // when
    List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
        .orderByTenantId()
        .desc()
        .list();

    // then
    assertThat(historicActivityInstances).hasSize(6);
    verifySorting(historicActivityInstances, inverted(historicActivityInstanceByTenantId()));
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(2L); // null-tenant instances are still included
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(4L); // null-tenant instances are still included
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.withoutTenantId().count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(6L); // null-tenant instances are still included
    assertThat(query.withoutTenantId().count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryDisabledTenantCheck() {
    // given
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(6L);
  }

  protected ProcessInstance startProcessInstanceForTenant(String tenant) {
    return runtimeService.createProcessInstanceByKey("testProcess")
        .processDefinitionTenantId(tenant)
        .execute();
  }

}
