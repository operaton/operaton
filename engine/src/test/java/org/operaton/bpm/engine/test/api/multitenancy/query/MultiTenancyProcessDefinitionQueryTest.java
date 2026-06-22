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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyProcessDefinitionQueryTest {

  protected static final String PROCESS_DEFINITION_KEY = "process";
  protected static final BpmnModelInstance emptyProcess = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY).startEvent().done();

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    testRule.deploy(emptyProcess);
    testRule.deployForTenant(TENANT_ONE, emptyProcess);
    testRule.deployForTenant(TENANT_TWO, emptyProcess);
  }

  @Test
  void testQueryNoTenantIdSet() {
    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = repositoryService.
        createProcessDefinitionQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByDefinitionsWithoutTenantId() {
    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIdsIncludeDefinitionsWithoutTenantId() {
    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn(TENANT_ONE)
        .includeProcessDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn(TENANT_TWO)
        .includeProcessDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeProcessDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByKey() {
    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY);
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .withoutTenantId();
    // one definition without tenant id
    assertThat(query.count()).isOne();

    query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .tenantIdIn(TENANT_ONE);
    // one definition for tenant one
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByLatestNoTenantIdSet() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, emptyProcess);

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .latestVersion();
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    Map<String, ProcessDefinition> processDefinitionsForTenant = getProcessDefinitionsForTenant(query.list());
    assertThat(processDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(processDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(processDefinitionsForTenant.get(null).getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithTenantId() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, emptyProcess);

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    ProcessDefinition processDefinition = query.singleResult();
    assertThat(processDefinition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(processDefinition.getVersion()).isEqualTo(2);

    query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();

    processDefinition = query.singleResult();
    assertThat(processDefinition.getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(processDefinition.getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithTenantIds() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, emptyProcess);

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(2L);

    Map<String, ProcessDefinition> processDefinitionsForTenant = getProcessDefinitionsForTenant(query.list());
    assertThat(processDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(processDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithoutTenantId() {
    // deploy a second version without tenant id
   testRule.deploy(emptyProcess);

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .latestVersion()
        .withoutTenantId();

    assertThat(query.count()).isOne();

    ProcessDefinition processDefinition = query.singleResult();
    assertThat(processDefinition.getTenantId()).isNull();
    assertThat(processDefinition.getVersion()).isEqualTo(2);
  }

  @Test
  void testQueryByLatestWithTenantIdsIncludeDefinitionsWithoutTenantId() {
    // deploy a second version without tenant id
   testRule.deploy(emptyProcess);
    // deploy a third version for tenant one
    testRule.deployForTenant(TENANT_ONE, emptyProcess);
    testRule.deployForTenant(TENANT_ONE, emptyProcess);

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeProcessDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);

    Map<String, ProcessDefinition> processDefinitionsForTenant = getProcessDefinitionsForTenant(query.list());
    assertThat(processDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(3);
    assertThat(processDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(processDefinitionsForTenant.get(null).getVersion()).isEqualTo(2);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var processDefinitionQuery = repositoryService.createProcessDefinitionQuery();

    assertThatThrownBy(() -> processDefinitionQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<ProcessDefinition> processDefinitions = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(processDefinitions).hasSize(2);
    assertThat(processDefinitions.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(processDefinitions.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<ProcessDefinition> processDefinitions = repositoryService
        .createProcessDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(processDefinitions).hasSize(2);
    assertThat(processDefinitions.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(processDefinitions.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  protected Map<String, ProcessDefinition> getProcessDefinitionsForTenant(List<ProcessDefinition> processDefinitions) {
    Map<String, ProcessDefinition> definitionsForTenant = new HashMap<>();

    for (ProcessDefinition definition : processDefinitions) {
      definitionsForTenant.put(definition.getTenantId(), definition);
    }
    return definitionsForTenant;
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeProcessDefinitionsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

}
