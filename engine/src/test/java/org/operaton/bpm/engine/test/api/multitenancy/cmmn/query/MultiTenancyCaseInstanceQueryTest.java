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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.CaseDefinitionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyCaseInstanceQueryTest {

  protected static final String CMMN_FILE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected CaseService caseService;

  @BeforeEach
  void setUp() {
    testRule.deploy(CMMN_FILE);
    testRule.deployForTenant(TENANT_ONE, CMMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_FILE);

    createCaseInstance(null);
    createCaseInstance(TENANT_ONE);
    createCaseInstance(TENANT_TWO);
  }

  @Test
  void testQueryNoTenantIdSet() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = caseService
        .createCaseInstanceQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByInstancesWithoutTenantId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByNonExistingTenantId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var caseInstanceQuery = caseService.createCaseInstanceQuery();
    assertThatThrownBy(() -> caseInstanceQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude case instances without tenant id because of database-specific ordering
    List<CaseInstance> caseInstances = caseService.createCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(caseInstances).hasSize(2);
    assertThat(caseInstances.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(caseInstances.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude case instances without tenant id because of database-specific ordering
    List<CaseInstance> caseInstances = caseService.createCaseInstanceQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(caseInstances).hasSize(2);
    assertThat(caseInstances.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(caseInstances.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

  protected CaseInstance createCaseInstance(String tenantId) {
    String caseDefinitionId = null;

    CaseDefinitionQuery caseDefinitionQuery = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase");
    if (tenantId == null) {
      caseDefinitionId = caseDefinitionQuery.withoutTenantId().singleResult().getId();
    } else {
      caseDefinitionId = caseDefinitionQuery.tenantIdIn(tenantId).singleResult().getId();
    }

    return caseService.withCaseDefinition(caseDefinitionId).create();
  }

}
