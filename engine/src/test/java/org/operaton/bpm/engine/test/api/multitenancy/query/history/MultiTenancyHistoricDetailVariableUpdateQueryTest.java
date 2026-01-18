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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricDetailQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicDetailByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricDetailVariableUpdateQueryTest {

  protected static final String TENANT_NULL = null;
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String VARIABLE_NAME = "myVar";
  protected static final String TENANT_NULL_VAR = "tenantNullVar";
  protected static final String TENANT_ONE_VAR = "tenant1Var";
  protected static final String TENANT_TWO_VAR = "tenant2Var";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    // given
    BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .userTask()
      .endEvent()
    .done();

    testRule.deployForTenant(TENANT_NULL, oneTaskProcess);
    testRule.deployForTenant(TENANT_ONE, oneTaskProcess);
    testRule.deployForTenant(TENANT_TWO, oneTaskProcess);

    ProcessInstance processInstanceNull = startProcessInstanceForTenant(TENANT_NULL, TENANT_NULL_VAR);
    ProcessInstance processInstanceOne = startProcessInstanceForTenant(TENANT_ONE, TENANT_ONE_VAR);
    ProcessInstance processInstanceTwo = startProcessInstanceForTenant(TENANT_TWO, TENANT_TWO_VAR);

    completeUserTask(processInstanceNull, TENANT_NULL_VAR + "_updated");
    completeUserTask(processInstanceOne, TENANT_ONE_VAR + "_updated");
    completeUserTask(processInstanceTwo, TENANT_TWO_VAR + "_updated");

  }

  @Test
  void shouldQueryWithoutTenantId() {
    // when
    HistoricDetailQuery query = historyService
        .createHistoricDetailQuery()
        .variableUpdates();

    // then
    assertThat(query.count()).isEqualTo(6L);
  }

  @Test
  void shouldQueryFilterWithoutTenantId() {
    // when
    HistoricDetailQuery query = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .withoutTenantId();

    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByTenantId() {
    // when
    HistoricDetailQuery queryTenantOne = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .tenantIdIn(TENANT_ONE);

    HistoricDetailQuery queryTenantTwo = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .tenantIdIn(TENANT_TWO);

    // then
    assertThat(queryTenantOne.count()).isEqualTo(2L);
    assertThat(queryTenantTwo.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByTenantIds() {
    // when
    HistoricDetailQuery query = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(4L);
  }

  @Test
  void shouldQueryByNonExistingTenantId() {
    // when
    HistoricDetailQuery query = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicDetailQuery = historyService.createHistoricDetailQuery()
        .variableUpdates();
    assertThatThrownBy(() -> historicDetailQuery.tenantIdIn((String) null)).isInstanceOf(NullValueException.class);
  }

  @Test
  void shouldQuerySortingAsc() {
    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery()
        .variableUpdates()
        .orderByTenantId()
        .asc()
        .list();

    // then
    assertThat(historicDetails).hasSize(6);
    verifySorting(historicDetails, historicDetailByTenantId());
  }

  @Test
  void shouldQuerySortingDesc() {
    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery()
        .variableUpdates()
        .orderByTenantId()
        .desc()
        .list();

    // then
    assertThat(historicDetails).hasSize(6);
    verifySorting(historicDetails, inverted(historicDetailByTenantId()));
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.count()).isEqualTo(2L); // null-tenant instances are still included
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

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
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.count()).isEqualTo(6L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryDisabledTenantCheck() {
    // given
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.count()).isEqualTo(6L); // null-tenant instances are still included
  }

  protected ProcessInstance startProcessInstanceForTenant(String tenant, String variableValue) {
    return runtimeService.createProcessInstanceByKey("testProcess")
        .setVariable(VARIABLE_NAME, variableValue)
        .processDefinitionTenantId(tenant)
        .execute();
  }

  protected void completeUserTask(ProcessInstance processInstance, String varValue) {
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();

    Map<String, Object> updatedVariables = new HashMap<>();
    updatedVariables.put(VARIABLE_NAME, varValue);
    taskService.complete(task.getId(), updatedVariables);
  }

}
