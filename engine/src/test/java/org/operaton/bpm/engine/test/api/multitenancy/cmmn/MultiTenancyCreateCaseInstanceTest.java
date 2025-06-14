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
package org.operaton.bpm.engine.test.api.multitenancy.cmmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

class MultiTenancyCreateCaseInstanceTest {

  protected static final String CMMN_FILE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

  protected static final String CASE_DEFINITION_KEY = "oneTaskCase";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected CaseService caseService;
  protected IdentityService identityService;

  @Test
  void testFailToCreateCaseInstanceByIdWithoutTenantId() {
   testRule.deploy(CMMN_FILE);

    CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
    var caseInstanceBuilder = caseService.withCaseDefinition(caseDefinition.getId())
          .caseDefinitionWithoutTenantId();

    try {
      caseInstanceBuilder.create();
      fail("BadUserRequestException exception");
    } catch(BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Cannot specify a tenant-id");
    }
  }

  @Test
  void testFailToCreateCaseInstanceByIdWithTenantId() {
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);

    CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
    var caseInstanceBuilder = caseService.withCaseDefinition(caseDefinition.getId())
          .caseDefinitionTenantId(TENANT_ONE);

    try {
      caseInstanceBuilder.create();
      fail("BadUserRequestException exception");
    } catch(BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Cannot specify a tenant-id");
    }
  }

  @Test
  void testFailToCreateCaseInstanceByKeyForNonExistingTenantID() {
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);
    var caseInstanceBuilder = caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY)
          .caseDefinitionTenantId("nonExistingTenantId");

    try {
      caseInstanceBuilder.create();
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no case definition deployed with key 'oneTaskCase' and tenant-id 'nonExistingTenantId'");
    }
  }

  @Test
  void testFailToCreateCaseInstanceByKeyForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);
    var caseInstanceBuilder = caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY);

    try {
      caseInstanceBuilder.create();
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("multiple tenants.");
    }
  }

  @Test
  void testCreateCaseInstanceByKeyWithoutTenantId() {
   testRule.deploy(CMMN_FILE);

    caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY)
        .caseDefinitionWithoutTenantId()
        .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.singleResult().getTenantId()).isNull();

  }

  @Test
  void testCreateCaseInstanceByKeyForAnyTenants() {
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);

    caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY)
        .create();

    assertThat(caseService.createCaseInstanceQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testCreateCaseInstanceByKeyAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);

    caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY)
        .caseDefinitionTenantId(TENANT_ONE)
        .create();

    assertThat(caseService.createCaseInstanceQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testCreateCaseInstanceByKeyWithoutTenantIdNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

   testRule.deploy(CMMN_FILE);

    caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .caseDefinitionWithoutTenantId()
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
  }

  @Test
  void testFailToCreateCaseInstanceByKeyNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    var caseInstanceBuilder = caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY);

    try {
      caseInstanceBuilder.create();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no case definition deployed with key 'oneTaskCase'");
    }
  }

  @Test
  void testFailToCreateCaseInstanceByKeyWithTenantIdNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    var caseInstanceBuilder = caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY)
        .caseDefinitionTenantId(TENANT_ONE);

    try {
      caseInstanceBuilder.create();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot create an instance of the case definition");
    }
  }

  @Test
  void testFailToCreateCaseInstanceByIdNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .singleResult();

    identityService.setAuthentication("user", null, null);
    var caseInstanceBuilder = caseService.withCaseDefinition(caseDefinition.getId());

    try {
      caseInstanceBuilder.create();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot create an instance of the case definition");
    }
  }

  @Test
  void testCreateCaseInstanceByKeyWithTenantIdAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);

    caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .caseDefinitionTenantId(TENANT_ONE)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testCreateCaseInstanceByIdAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);

    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .singleResult();

    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    caseService.withCaseDefinition(caseDefinition.getId()).create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testCreateCaseInstanceByKeyWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);

    caseService.withCaseDefinitionByKey(CASE_DEFINITION_KEY).create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testCreateCaseInstanceByKeyWithTenantIdDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .caseDefinitionTenantId(TENANT_ONE)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

}
