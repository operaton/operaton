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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
class MultiTenancyHistoricProcessInstanceQueryTest {

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

    testRule.deploy(oneTaskProcess);
    testRule.deployForTenant(TENANT_ONE, oneTaskProcess);
    testRule.deployForTenant(TENANT_TWO, oneTaskProcess);

    startProcessInstance();
    startProcessInstanceForTenant(TENANT_ONE);
    startProcessInstanceForTenant(TENANT_TWO);
  }

  @Test
  void shouldQueryNoTenantIdSet() {
    HistoricProcessInstanceQuery query = historyService.
        createHistoricProcessInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryByTenantId() {
    HistoricProcessInstanceQuery query = historyService
        .createHistoricProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = historyService
        .createHistoricProcessInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryByTenantIds() {
    HistoricProcessInstanceQuery query = historyService
        .createHistoricProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByNonExistingTenantId() {
    HistoricProcessInstanceQuery query = historyService
        .createHistoricProcessInstanceQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void shouldQueryByWithoutTenantId() {
    HistoricProcessInstanceQuery query = historyService
      .createHistoricProcessInstanceQuery()
      .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    assertThatThrownBy(() -> historicProcessInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void shouldQuerySortingAsc() {
    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(historicProcessInstances).hasSize(2);
    assertThat(historicProcessInstances.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(historicProcessInstances.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void shouldQuerySortingDesc() {
    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(historicProcessInstances).hasSize(2);
    assertThat(historicProcessInstances.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(historicProcessInstances.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void shouldQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void shouldQueryDisabledTenantCheck() {
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

  protected ProcessInstance startProcessInstanceForTenant(String tenant) {
    return runtimeService.createProcessInstanceByKey("testProcess")
        .processDefinitionTenantId(tenant)
        .execute();
  }

  protected ProcessInstance startProcessInstance() {
    return runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .execute();
  }

}
