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
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyIncidentQueryTest {

  protected static final BpmnModelInstance BPMN = Bpmn.createExecutableProcess("failingProcess")
      .startEvent()
      .serviceTask()
        .operatonExpression("${failing}")
        .operatonAsyncBefore()
      .endEvent()
      .done();

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
    testRule.deployForTenant(TENANT_ONE, BPMN);
    testRule.deployForTenant(TENANT_TWO, BPMN);

    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_ONE);
    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_TWO);
  }

  @Test
  void testQueryWithoutTenantId() {
    IncidentQuery query = runtimeService
        .createIncidentQuery();

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByTenantId() {
    IncidentQuery query = runtimeService
        .createIncidentQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = runtimeService
        .createIncidentQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    IncidentQuery query = runtimeService
        .createIncidentQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    IncidentQuery query = runtimeService
        .createIncidentQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var incidentQuery = runtimeService.createIncidentQuery();

    assertThatThrownBy(() -> incidentQuery.tenantIdIn((String[]) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds is null");
  }

  @Test
  void testQuerySortingAsc() {
    List<Incident> incidents = runtimeService.createIncidentQuery()
        .orderByTenantId()
        .asc()
        .list();

    assertThat(incidents).hasSize(2);
    assertThat(incidents.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(incidents.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    List<Incident> incidents = runtimeService.createIncidentQuery()
        .orderByTenantId()
        .desc()
        .list();

    assertThat(incidents).hasSize(2);
    assertThat(incidents.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(incidents.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    IncidentQuery query = runtimeService.createIncidentQuery();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    IncidentQuery query = runtimeService.createIncidentQuery();

    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    IncidentQuery query = runtimeService.createIncidentQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    IncidentQuery query = runtimeService.createIncidentQuery();
    assertThat(query.count()).isEqualTo(2L);
  }

  protected void startProcessInstanceAndExecuteFailingJobForTenant(String tenant) {
    runtimeService.createProcessInstanceByKey("failingProcess").processDefinitionTenantId(tenant).execute();

    testRule.executeAvailableJobs();
  }

}
