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
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.dmn.DmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author kristin.polenz
 */
class MultiTenancyDecisionDefinitionCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String DMN_MODEL = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected String decisionDefinitionId;

  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);

    decisionDefinitionId = repositoryService.createDecisionDefinitionQuery().singleResult().getId();

  }

  @Test
  void failToGetDecisionModelNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDecisionModel(decisionDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the decision definition");
  }

  @Test
  void getDecisionModelWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getDecisionModel(decisionDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void getDecisionModelDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getDecisionModel(decisionDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void failToGetDecisionDiagramNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDecisionDiagram(decisionDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the decision definition");

  }

  @Test
  void getDecisionDiagramWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getDecisionDiagram(decisionDefinitionId);

    // inputStream is always null because there is no decision diagram at the moment
    // what should be deployed as a diagram resource for DMN?
    assertThat(inputStream).isNull();
  }

  @Test
  void getDecisionDiagramDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getDecisionDiagram(decisionDefinitionId);

    // inputStream is always null because there is no decision diagram at the moment
    // what should be deployed as a diagram resource for DMN?
    assertThat(inputStream).isNull();
  }

  @Test
  void failToGetDecisionDefinitionNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDecisionDefinition(decisionDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the decision definition");
  }

  @Test
  void getDecisionDefinitionWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DecisionDefinition definition = repositoryService.getDecisionDefinition(decisionDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void getDecisionDefinitionDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DecisionDefinition definition = repositoryService.getDecisionDefinition(decisionDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void failToGetDmnModelInstanceNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDmnModelInstance(decisionDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the decision definition");
  }

  @Test
  void updateHistoryTimeToLiveWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, 6);

    DecisionDefinition definition = repositoryService.getDecisionDefinition(decisionDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(definition.getHistoryTimeToLive()).isEqualTo(6);
  }

  @Test
  void updateHistoryTimeToLiveDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, 6);

    DecisionDefinition definition = repositoryService.getDecisionDefinition(decisionDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(definition.getHistoryTimeToLive()).isEqualTo(6);
  }

  @Test
  void updateHistoryTimeToLiveNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, 6))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the decision definition");
  }

  @Test
  void getDmnModelInstanceWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DmnModelInstance modelInstance = repositoryService.getDmnModelInstance(decisionDefinitionId);

    assertThat(modelInstance).isNotNull();
  }

  @Test
  void getDmnModelInstanceDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DmnModelInstance modelInstance = repositoryService.getDmnModelInstance(decisionDefinitionId);

    assertThat(modelInstance).isNotNull();
  }

}
