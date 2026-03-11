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
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinitionQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyDecisionDefinitionQueryTest {

  protected static final String DECISION_DEFINITION_KEY = "decision";
  protected static final String DMN = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";

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
    testRule.deploy(DMN);
    testRule.deployForTenant(TENANT_ONE, DMN);
    testRule.deployForTenant(TENANT_TWO, DMN);
  }

  @Test
  void testQueryNoTenantIdSet() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = repositoryService.
        createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByDefinitionsWithoutTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIdsIncludeDefinitionsWithoutTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_TWO)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByKey() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY);
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .withoutTenantId();
    // one definition without tenant id
    assertThat(query.count()).isOne();

    query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .tenantIdIn(TENANT_ONE);
    // one definition for tenant one
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByLatestNoTenantIdSet() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion();
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    Map<String, DecisionDefinition> decisionDefinitionsForTenant = getDecisionDefinitionsForTenant(query.list());
    assertThat(decisionDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(decisionDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(decisionDefinitionsForTenant.get(null).getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithTenantId() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    DecisionDefinition decisionDefinition = query.singleResult();
    assertThat(decisionDefinition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(decisionDefinition.getVersion()).isEqualTo(2);

    query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();

    decisionDefinition = query.singleResult();
    assertThat(decisionDefinition.getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(decisionDefinition.getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithTenantIds() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc();
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(2L);

    Map<String, DecisionDefinition> decisionDefinitionsForTenant = getDecisionDefinitionsForTenant(query.list());
    assertThat(decisionDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(decisionDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithoutTenantId() {
    // deploy a second version without tenant id
   testRule.deploy(DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .withoutTenantId();

    assertThat(query.count()).isOne();

    DecisionDefinition decisionDefinition = query.singleResult();
    assertThat(decisionDefinition.getTenantId()).isNull();
    assertThat(decisionDefinition.getVersion()).isEqualTo(2);
  }

  @Test
  void testQueryByLatestWithTenantIdsIncludeDefinitionsWithoutTenantId() {
    // deploy a second version without tenant id
   testRule.deploy(DMN);
    // deploy a third version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);

    Map<String, DecisionDefinition> decisionDefinitionsForTenant = getDecisionDefinitionsForTenant(query.list());
    assertThat(decisionDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(3);
    assertThat(decisionDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(decisionDefinitionsForTenant.get(null).getVersion()).isEqualTo(2);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var decisionDefinitionQuery = repositoryService.createDecisionDefinitionQuery();

    assertThatThrownBy(() -> decisionDefinitionQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<DecisionDefinition> decisionDefinitions = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(decisionDefinitions).hasSize(2);
    assertThat(decisionDefinitions.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(decisionDefinitions.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<DecisionDefinition> decisionDefinitions = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(decisionDefinitions).hasSize(2);
    assertThat(decisionDefinitions.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(decisionDefinitions.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeDecisionDefinitionsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

  protected Map<String, DecisionDefinition> getDecisionDefinitionsForTenant(List<DecisionDefinition> decisionDefinitions) {
    Map<String, DecisionDefinition> definitionsForTenant = new HashMap<>();

    for (DecisionDefinition definition : decisionDefinitions) {
      definitionsForTenant.put(definition.getTenantId(), definition);
    }
    return definitionsForTenant;
  }

}
