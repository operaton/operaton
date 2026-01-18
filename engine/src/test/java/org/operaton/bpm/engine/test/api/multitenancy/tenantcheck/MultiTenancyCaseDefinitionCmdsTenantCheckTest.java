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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;
import java.util.List;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author kristin.polenz
 */
class MultiTenancyCaseDefinitionCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String CMMN_MODEL = "org/operaton/bpm/engine/test/api/cmmn/emptyStageCase.cmmn";
  protected static final String CMMN_DIAGRAM = "org/operaton/bpm/engine/test/api/cmmn/emptyStageCase.png";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected String caseDefinitionId;

  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL, CMMN_DIAGRAM);

    caseDefinitionId = repositoryService.createCaseDefinitionQuery().singleResult().getId();
  }

  @Test
  void failToGetCaseModelNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getCaseModel(caseDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the case definition");
  }

  @Test
  void getCaseModelWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getCaseModel(caseDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void getCaseModelDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getCaseModel(caseDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void failToGetCaseDiagramNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getCaseDiagram(caseDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the case definition");
  }

  @Test
  void getCaseDiagramWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getCaseDiagram(caseDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void getCaseDiagramDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getCaseDiagram(caseDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void failToGetCaseDefinitionNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getCaseDefinition(caseDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the case definition");

  }

  @Test
  void getCaseDefinitionWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    CaseDefinition definition = repositoryService.getCaseDefinition(caseDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void getCaseDefinitionDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    CaseDefinition definition = repositoryService.getCaseDefinition(caseDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void failToGetCmmnModelInstanceNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getCmmnModelInstance(caseDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the case definition");
  }

  @Test
  void getCmmnModelInstanceWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    CmmnModelInstance modelInstance = repositoryService.getCmmnModelInstance(caseDefinitionId);

    assertThat(modelInstance).isNotNull();
  }

  @Test
  void getCmmnModelInstanceDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    CmmnModelInstance modelInstance = repositoryService.getCmmnModelInstance(caseDefinitionId);

    assertThat(modelInstance).isNotNull();
  }

  @Test
  void updateHistoryTimeToLiveWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitionId, 6);

    CaseDefinition definition = repositoryService.getCaseDefinition(caseDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(definition.getHistoryTimeToLive()).isEqualTo(6);
  }

  @Test
  void updateHistoryTimeToLiveDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitionId, 6);

    CaseDefinition definition = repositoryService.getCaseDefinition(caseDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(definition.getHistoryTimeToLive()).isEqualTo(6);
  }

  @Test
  void updateHistoryTimeToLiveNoAuthenticatedTenants(){
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitionId, 6))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case definition");
  }

}
