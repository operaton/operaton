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
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.api.multitenancy.StaticTenantIdTestProvider;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyProcessInstanceQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .userTask()
      .endEvent()
    .done();

   testRule.deploy(oneTaskProcess);
    testRule.deployForTenant(TENANT_ONE, oneTaskProcess);
    testRule.deployForTenant(TENANT_TWO, oneTaskProcess);

    runtimeService.createProcessInstanceByKey("testProcess").processDefinitionWithoutTenantId().execute();
    runtimeService.createProcessInstanceByKey("testProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("testProcess").processDefinitionTenantId(TENANT_TWO).execute();
  }

  @Test
  void testQueryNoTenantIdSet() {
    ProcessInstanceQuery query = runtimeService.
        createProcessInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = runtimeService
        .createProcessInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByInstancesWithoutTenantId() {
    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByNonExistingTenantId() {
    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();

    assertThatThrownBy(() -> processInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude instances without tenant id because of database-specific ordering
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(processInstances).hasSize(2);
    assertThat(processInstances.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(processInstances.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude instances without tenant id because of database-specific ordering
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(processInstances).hasSize(2);
    assertThat(processInstances.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(processInstances.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByProcessDefinitionWithoutTenantId() {
    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .processDefinitionWithoutTenantId();

    // then
    assertThat(query.count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryByProcessDefinitionWithoutTenantId_VaryingProcessInstanceTenantId() {
    // given
    StaticTenantIdTestProvider tenantIdProvider = new StaticTenantIdTestProvider(null);
    processEngineConfiguration.setTenantIdProvider(tenantIdProvider);

    tenantIdProvider.setTenantIdProvider("anotherTenantId");

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .execute();

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .processDefinitionWithoutTenantId();

    // then
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.withoutTenantId().count()).isOne();
    assertThat(query.tenantIdIn("anotherTenantId").count()).isOne();

    // cleanup
    processEngineConfiguration.setTenantIdProvider(null);
  }

}
