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
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinitionQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyDecisionRequirementsDefinitionQueryTest {

  protected static final String DECISION_REQUIREMENTS_DEFINITION_KEY = "score";
  protected static final String DMN = "org/operaton/bpm/engine/test/dmn/deployment/drdScore.dmn11.xml";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    testRule.deploy(DMN);
    testRule.deployForTenant(TENANT_ONE, DMN);
    testRule.deployForTenant(TENANT_TWO, DMN);
  }

  @Test
  void queryNoTenantIdSet() {
    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void queryByTenantId() {
    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = repositoryService.
        createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void queryByTenantIds() {
    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void queryByDefinitionsWithoutTenantId() {
    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void queryByTenantIdsIncludeDefinitionsWithoutTenantId() {
    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_ONE)
        .includeDecisionRequirementsDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_TWO)
        .includeDecisionRequirementsDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeDecisionRequirementsDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void queryByKey() {
    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY);
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .withoutTenantId();
    // one definition without tenant id
    assertThat(query.count()).isOne();

    query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .tenantIdIn(TENANT_ONE);
    // one definition for tenant one
    assertThat(query.count()).isOne();
  }

  @Test
  void queryByLatestNoTenantIdSet() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .latestVersion();
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    Map<String, DecisionRequirementsDefinition> definitionsForTenant = getDecisionRequirementsDefinitionsForTenant(query.list());
    assertThat(definitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(definitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(definitionsForTenant.get(null).getVersion()).isEqualTo(1);
  }

  @Test
  void queryByLatestWithTenantId() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    var decisionRequirementsDefinition = query.singleResult();
    assertThat(decisionRequirementsDefinition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(decisionRequirementsDefinition.getVersion()).isEqualTo(2);

    query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();

    decisionRequirementsDefinition = query.singleResult();
    assertThat(decisionRequirementsDefinition.getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(decisionRequirementsDefinition.getVersion()).isEqualTo(1);
  }

  @Test
  void queryByLatestWithTenantIds() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc();
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(2L);

    Map<String, DecisionRequirementsDefinition> definitionsForTenant = getDecisionRequirementsDefinitionsForTenant(query.list());
    assertThat(definitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(definitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
  }

  @Test
  void queryByLatestWithoutTenantId() {
    // deploy a second version without tenant id
    testRule.deploy(DMN);

    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .latestVersion()
        .withoutTenantId();

    assertThat(query.count()).isOne();

    var decisionRequirementsDefinition = query.singleResult();
    assertThat(decisionRequirementsDefinition.getTenantId()).isNull();
    assertThat(decisionRequirementsDefinition.getVersion()).isEqualTo(2);
  }

  @Test
  void queryByLatestWithTenantIdsIncludeDefinitionsWithoutTenantId() {
    // deploy a second version without tenant id
    testRule.deploy(DMN);
    // deploy a third version for tenant one
    testRule.deployForTenant(TENANT_ONE, DMN);
    testRule.deployForTenant(TENANT_ONE, DMN);

    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionKey(DECISION_REQUIREMENTS_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeDecisionRequirementsDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);

    Map<String, DecisionRequirementsDefinition> definitionsForTenant = getDecisionRequirementsDefinitionsForTenant(query.list());
    assertThat(definitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(3);
    assertThat(definitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(definitionsForTenant.get(null).getVersion()).isEqualTo(2);
  }

  @Test
  void queryByNonExistingTenantId() {
    DecisionRequirementsDefinitionQuery query = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void failQueryByTenantIdNull() {
    // given
    var decisionRequirementsDefinitionQuery = repositoryService.createDecisionRequirementsDefinitionQuery();

    // when/then
    assertThatThrownBy(() -> decisionRequirementsDefinitionQuery.tenantIdIn((String) null))
      .isInstanceOf(NullValueException.class);
  }

  @Test
  void querySortingAsc() {
    // exclude definitions without tenant id because of database-specific ordering
    var decisionRequirementsDefinitions = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(decisionRequirementsDefinitions).hasSize(2);
    assertThat(decisionRequirementsDefinitions.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(decisionRequirementsDefinitions.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void querySortingDesc() {
    // exclude definitions without tenant id because of database-specific ordering
    var decisionRequirementsDefinitions = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(decisionRequirementsDefinitions).hasSize(2);
    assertThat(decisionRequirementsDefinitions.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(decisionRequirementsDefinitions.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void queryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void queryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeDecisionRequirementsDefinitionsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void queryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void queryDisabledTenantCheck() {
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

  protected Map<String, DecisionRequirementsDefinition> getDecisionRequirementsDefinitionsForTenant(List<DecisionRequirementsDefinition> definitions) {
    Map<String, DecisionRequirementsDefinition> definitionsForTenant = new HashMap<>();

    for (DecisionRequirementsDefinition definition : definitions) {
      definitionsForTenant.put(definition.getTenantId(), definition);
    }
    return definitionsForTenant;
  }

}
