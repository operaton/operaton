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
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicVariableInstanceByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class MultiTenancyHistoricVariableInstanceQueryTest {

  protected static final String TENANT_NULL = null;
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String TENANT_NULL_VAR = "tenantNullVar";
  protected static final String TENANT_ONE_VAR = "tenant1Var";
  protected static final String TENANT_TWO_VAR = "tenant2Var";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected TaskService taskService;
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

    startProcessInstanceForTenant(TENANT_NULL, TENANT_NULL_VAR);
    startProcessInstanceForTenant(TENANT_ONE, TENANT_ONE_VAR);
    startProcessInstanceForTenant(TENANT_TWO, TENANT_TWO_VAR);
  }

  @Test
  void shouldQueryWithoutTenantId() {
    // when
    HistoricVariableInstanceQuery query = historyService
        .createHistoricVariableInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryFilterWithoutTenantId() {
    // when
    HistoricVariableInstanceQuery query = historyService
        .createHistoricVariableInstanceQuery()
        .withoutTenantId();

    // then
    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryByTenantId() {
    // when
    HistoricVariableInstanceQuery queryTenantOne = historyService
        .createHistoricVariableInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    HistoricVariableInstanceQuery queryTenantTwo = historyService
        .createHistoricVariableInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    // then
    assertThat(queryTenantOne.count()).isOne();
    assertThat(queryTenantOne.list().get(0).getValue()).isEqualTo(TENANT_ONE_VAR);
    assertThat(queryTenantTwo.count()).isOne();
    assertThat(queryTenantTwo.list().get(0).getValue()).isEqualTo(TENANT_TWO_VAR);
  }

  @Test
  void shouldQueryByTenantIds() {
    // when
    HistoricVariableInstanceQuery query = historyService
        .createHistoricVariableInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByNonExistingTenantId() {
    // when
    HistoricVariableInstanceQuery query = historyService
        .createHistoricVariableInstanceQuery()
        .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery();
    assertThatThrownBy(() -> historicVariableInstanceQuery.tenantIdIn((String) null)).isInstanceOf(NullValueException.class);
  }

  @Test
  void shouldQuerySortingAsc() {
    // when
    List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
        .orderByTenantId()
        .asc()
        .list();

    // then
    assertThat(historicVariableInstances).hasSize(3); // null-tenant instances are still included
    verifySorting(historicVariableInstances, historicVariableInstanceByTenantId());
  }

  @Test
  void shouldQuerySortingDesc() {
    // when
    List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
        .orderByTenantId()
        .desc()
        .list();

    // then
    assertThat(historicVariableInstances).hasSize(3); // null-tenant instances are still included
    verifySorting(historicVariableInstances, inverted(historicVariableInstanceByTenantId()));
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.count()).isOne(); // null-tenant instances are still included
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(2L); // null-tenant instances are still included
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
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3); // null-tenant instances are still included
    assertThat(query.withoutTenantId().count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void shouldQueryDisabledTenantCheck() {
    // given
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.count()).isEqualTo(3L);
  }

  protected ProcessInstance startProcessInstanceForTenant(String tenant, String variableValue) {
    return runtimeService.createProcessInstanceByKey("testProcess")
        .setVariable("myVar", variableValue)
        .processDefinitionTenantId(tenant)
        .execute();
  }

}
