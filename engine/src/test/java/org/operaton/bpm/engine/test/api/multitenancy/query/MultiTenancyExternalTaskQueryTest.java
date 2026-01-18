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

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyExternalTaskQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ExternalTaskService externalTaskService;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected RuntimeService runtimeService;

  @BeforeEach
  void setUp() {
    BpmnModelInstance process = Bpmn.createExecutableProcess()
      .startEvent()
      .serviceTask()
        .operatonType("external")
        .operatonTopic("test")
      .endEvent()
    .done();

    testRule.deployForTenant(TENANT_ONE, process);
    testRule.deployForTenant(TENANT_TWO, process);

    startProcessInstance(TENANT_ONE);
    startProcessInstance(TENANT_TWO);
  }

  @Test
  void testQueryWithoutTenantId() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery();

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByTenantId() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();

    assertThatThrownBy(() -> externalTaskQuery.tenantIdIn((String) null))
      .isInstanceOf(NullValueException.class)
      .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery()
        .orderByTenantId()
        .asc()
        .list();

    assertThat(externalTasks).hasSize(2);
    assertThat(externalTasks.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(externalTasks.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery()
        .orderByTenantId()
        .desc()
        .list();

    assertThat(externalTasks).hasSize(2);
    assertThat(externalTasks.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(externalTasks.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();

    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();
    assertThat(query.count()).isEqualTo(2L);
  }

  protected void startProcessInstance(String tenant) {
    String processDefinitionId = repositoryService
      .createProcessDefinitionQuery()
      .tenantIdIn(tenant)
      .singleResult()
      .getId();

    runtimeService.startProcessInstanceById(processDefinitionId);
  }

}
