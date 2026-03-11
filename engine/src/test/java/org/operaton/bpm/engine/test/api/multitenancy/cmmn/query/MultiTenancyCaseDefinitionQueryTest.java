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
package org.operaton.bpm.engine.test.api.multitenancy.cmmn.query;

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
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.CaseDefinitionQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyCaseDefinitionQueryTest {

  protected static final String CASE_DEFINITION_KEY = "Case_1";
  protected static final String CMMN = "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testSimpleDeployment.cmmn";

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
    testRule.deploy(CMMN);
    testRule.deployForTenant(TENANT_ONE, CMMN);
    testRule.deployForTenant(TENANT_TWO, CMMN);
  }

  @Test
  void testQueryNoTenantIdSet() {
    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = repositoryService.
        createCaseDefinitionQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByDefinitionsWithoutTenantId() {
    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .withoutTenantId();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIdsIncludeDefinitionsWithoutTenantId() {
    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn(TENANT_ONE)
        .includeCaseDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn(TENANT_TWO)
        .includeCaseDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeCaseDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByKey() {
    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY);
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .withoutTenantId();
    // one definition without tenant id
    assertThat(query.count()).isOne();

    query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .tenantIdIn(TENANT_ONE);
    // one definition for tenant one
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByLatestNoTenantIdSet() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, CMMN);

    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .latestVersion();
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(3L);

    Map<String, CaseDefinition> caseDefinitionsForTenant = getCaseDefinitionsForTenant(query.list());
    assertThat(caseDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(caseDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(caseDefinitionsForTenant.get(null).getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithTenantId() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, CMMN);

    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    CaseDefinition caseDefinition = query.singleResult();
    assertThat(caseDefinition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(caseDefinition.getVersion()).isEqualTo(2);

    query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();

    caseDefinition = query.singleResult();
    assertThat(caseDefinition.getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(caseDefinition.getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithTenantIds() {
    // deploy a second version for tenant one
    testRule.deployForTenant(TENANT_ONE, CMMN);

    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);
    // one definition for each tenant
    assertThat(query.count()).isEqualTo(2L);

    Map<String, CaseDefinition> caseDefinitionsForTenant = getCaseDefinitionsForTenant(query.list());
    assertThat(caseDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(2);
    assertThat(caseDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByLatestWithoutTenantId() {
    // deploy a second version without tenant id
   testRule.deploy(CMMN);

    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .latestVersion()
        .withoutTenantId();

    assertThat(query.count()).isOne();

    CaseDefinition cDefinition = query.singleResult();
    assertThat(cDefinition.getTenantId()).isNull();
    assertThat(cDefinition.getVersion()).isEqualTo(2);
  }

  @Test
  void testQueryByLatestWithTenantIdsIncludeDefinitionsWithoutTenantId() {
    // deploy a second version without tenant id
   testRule.deploy(CMMN);
    // deploy a third version for tenant one
    testRule.deployForTenant(TENANT_ONE, CMMN);
    testRule.deployForTenant(TENANT_ONE, CMMN);

    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeCaseDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);

    Map<String, CaseDefinition> caseDefinitionsForTenant = getCaseDefinitionsForTenant(query.list());
    assertThat(caseDefinitionsForTenant.get(TENANT_ONE).getVersion()).isEqualTo(3);
    assertThat(caseDefinitionsForTenant.get(TENANT_TWO).getVersion()).isEqualTo(1);
    assertThat(caseDefinitionsForTenant.get(null).getVersion()).isEqualTo(2);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    CaseDefinitionQuery query = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var caseDefinitionQuery = repositoryService.createCaseDefinitionQuery();
    assertThatThrownBy(() -> caseDefinitionQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<CaseDefinition> caseDefinitions = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(caseDefinitions).hasSize(2);
    assertThat(caseDefinitions.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(caseDefinitions.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<CaseDefinition> caseDefinitions = repositoryService
        .createCaseDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(caseDefinitions).hasSize(2);
    assertThat(caseDefinitions.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(caseDefinitions.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    CaseDefinitionQuery query = repositoryService.createCaseDefinitionQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    CaseDefinitionQuery query = repositoryService.createCaseDefinitionQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeCaseDefinitionsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    CaseDefinitionQuery query = repositoryService.createCaseDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    CaseDefinitionQuery query = repositoryService.createCaseDefinitionQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

  protected Map<String, CaseDefinition> getCaseDefinitionsForTenant(List<CaseDefinition> definitions) {
    Map<String, CaseDefinition> definitionsForTenant = new HashMap<>();

    for (CaseDefinition definition : definitions) {
      definitionsForTenant.put(definition.getTenantId(), definition);
    }
    return definitionsForTenant;
  }

}
