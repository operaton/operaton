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
package org.operaton.bpm.engine.test.api.multitenancy.query;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyVariableInstanceQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .userTask()
      .endEvent()
    .done();

    testRule.deployForTenant(TENANT_ONE, oneTaskProcess);
    testRule.deployForTenant(TENANT_TWO, oneTaskProcess);

    startProcessInstanceForTenant(TENANT_ONE);
    startProcessInstanceForTenant(TENANT_TWO);
  }

  @Test
  void testQueryWithoutTenantId() {
    VariableInstanceQuery query = runtimeService.
        createVariableInstanceQuery();

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByTenantId() {
    VariableInstanceQuery query = runtimeService.
        createVariableInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = runtimeService
        .createVariableInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    VariableInstanceQuery query = runtimeService.
        createVariableInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    VariableInstanceQuery query = runtimeService.
        createVariableInstanceQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var variableInstanceQuery = runtimeService.createVariableInstanceQuery();

    assertThatThrownBy(() -> variableInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery()
        .orderByTenantId()
        .asc()
        .list();

    assertThat(variableInstances).hasSize(2);
    assertThat(variableInstances.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(variableInstances.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery()
        .orderByTenantId()
        .desc()
        .list();

    assertThat(variableInstances).hasSize(2);
    assertThat(variableInstances.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(variableInstances.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(2L);
  }

  protected void startProcessInstanceForTenant(String tenant) {
    String processDefinitionId = repositoryService
      .createProcessDefinitionQuery()
      .tenantIdIn(tenant)
      .singleResult()
      .getId();

    VariableMap variables = Variables
        .createVariables()
        .putValue("var", "test");

    runtimeService.startProcessInstanceById(processDefinitionId, variables);
  }

}
