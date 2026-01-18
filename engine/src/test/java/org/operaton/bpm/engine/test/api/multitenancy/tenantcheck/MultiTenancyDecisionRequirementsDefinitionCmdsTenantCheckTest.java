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
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

class MultiTenancyDecisionRequirementsDefinitionCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String DRG_DMN = "org/operaton/bpm/engine/test/api/multitenancy/DecisionRequirementsGraph.dmn";
  protected static final String DRD_DMN = "org/operaton/bpm/engine/test/api/multitenancy/DecisionRequirementsGraph.png";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected String decisionRequirementsDefinitionId;

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_ONE, DRG_DMN, DRD_DMN);
    decisionRequirementsDefinitionId = repositoryService.createDecisionRequirementsDefinitionQuery()
      .singleResult().getId();
  }

  @Test
  void failToGetDecisionRequirementsDefinitionNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDecisionRequirementsDefinition(decisionRequirementsDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the decision requirements definition");
  }

  @Test
  void getDecisionRequirementsDefinitionWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DecisionRequirementsDefinition definition = repositoryService.getDecisionRequirementsDefinition(decisionRequirementsDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void getDecisionRequirementsDefinitionDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DecisionRequirementsDefinition definition = repositoryService.getDecisionRequirementsDefinition(decisionRequirementsDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void failToGetDecisionRequirementsModelNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDecisionRequirementsModel(decisionRequirementsDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the decision requirements definition");
  }

  @Test
  void getDecisionRequirementsModelWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getDecisionRequirementsModel(decisionRequirementsDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void getDecisionRequirementsModelDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getDecisionRequirementsModel(decisionRequirementsDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void failToGetDecisionRequirementsDiagramNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDecisionRequirementsDiagram(decisionRequirementsDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the decision requirements definition");
  }

  @Test
  void getDecisionDiagramWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getDecisionRequirementsDiagram(decisionRequirementsDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void getDecisionDiagramDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getDecisionRequirementsDiagram(decisionRequirementsDefinitionId);

    assertThat(inputStream).isNotNull();
  }

}
