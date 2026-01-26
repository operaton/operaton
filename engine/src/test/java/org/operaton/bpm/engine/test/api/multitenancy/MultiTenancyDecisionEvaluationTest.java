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
package org.operaton.bpm.engine.test.api.multitenancy;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyDecisionEvaluationTest {

  protected static final String DMN_FILE = "org/operaton/bpm/engine/test/api/dmn/Example.dmn";
  protected static final String DMN_FILE_SECOND_VERSION = "org/operaton/bpm/engine/test/api/dmn/Example_v2.dmn";

  protected static final String DECISION_DEFINITION_KEY = "decision";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String RESULT_OF_FIRST_VERSION = "ok";
  protected static final String RESULT_OF_SECOND_VERSION = "notok";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected DecisionService decisionService;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;

  @Test
  void testFailToEvaluateDecisionByIdWithoutTenantId() {
    // given
    testRule.deploy(DMN_FILE);

    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();
    VariableMap variables = createVariables();
    String decisionDefinitionId = decisionDefinition.getId();

    var decisionsEvaluationBuilder = decisionService.evaluateDecisionById(decisionDefinitionId)
        .variables(variables)
        .decisionDefinitionWithoutTenantId();

    // when/then
    assertThatThrownBy(decisionsEvaluationBuilder::evaluate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a tenant-id");
  }

  @Test
  void testFailToEvaluateDecisionByIdWithTenantId() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);

    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();
    VariableMap variables = createVariables();
    String decisionDefinitionId = decisionDefinition.getId();

    var decisionsEvaluationBuilder = decisionService.evaluateDecisionById(decisionDefinitionId)
        .variables(variables)
        .decisionDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(decisionsEvaluationBuilder::evaluate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a tenant-id");
  }

  @Test
  void testFailToEvaluateDecisionByKeyForNonExistingTenantID() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    VariableMap variables = createVariables();

    var decisionsEvaluationBuilder = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(variables)
        .decisionDefinitionTenantId("nonExistingTenantId");

    // when/then
    assertThatThrownBy(decisionsEvaluationBuilder::evaluate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no decision definition deployed with key 'decision' and tenant-id 'nonExistingTenantId'");
  }

  @Test
  void testFailToEvaluateDecisionByKeyForMultipleTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    VariableMap variables = createVariables();
    var decisionsEvaluationBuilder = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(variables);

    // when/then
    assertThatThrownBy(decisionsEvaluationBuilder::evaluate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("multiple tenants.");
  }

  @Test
  void testEvaluateDecisionByKeyWithoutTenantId() {
   testRule.deploy(DMN_FILE);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(createVariables())
        .decisionDefinitionWithoutTenantId()
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  @Test
  void testEvaluateDecisionByKeyForAnyTenants() {
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(createVariables())
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }


  @Test
  void testEvaluateDecisionByKeyAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_SECOND_VERSION);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(createVariables())
        .decisionDefinitionTenantId(TENANT_ONE)
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  @Test
  void testEvaluateDecisionByKeyLatestVersionAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_ONE, DMN_FILE_SECOND_VERSION);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(createVariables())
        .decisionDefinitionTenantId(TENANT_ONE)
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_SECOND_VERSION);
  }

  @Test
  void testEvaluateDecisionByKeyVersionAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);

    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_SECOND_VERSION);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(createVariables())
        .version(1)
        .decisionDefinitionTenantId(TENANT_TWO)
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  @Test
  void testEvaluateDecisionByKeyWithoutTenantIdNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

   testRule.deploy(DMN_FILE);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .decisionDefinitionWithoutTenantId()
        .variables(createVariables())
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  @Test
  void testFailToEvaluateDecisionByKeyNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    VariableMap variables = createVariables();
    var decisionsEvaluationBuilder = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(variables);

    // when/then
    assertThatThrownBy(decisionsEvaluationBuilder::evaluate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no decision definition deployed with key 'decision'");
  }

  @Test
  void testFailToEvaluateDecisionByKeyWithTenantIdNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    VariableMap variables = createVariables();
    var decisionsEvaluationBuilder = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .decisionDefinitionTenantId(TENANT_ONE)
        .variables(variables);

    // when/then
    assertThatThrownBy(decisionsEvaluationBuilder::evaluate
    )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot evaluate the decision");
  }

  @Test
  void testFailToEvaluateDecisionByIdNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);

    DecisionDefinition decisionDefinition = repositoryService
      .createDecisionDefinitionQuery()
      .singleResult();

    identityService.setAuthentication("user", null, null);
    VariableMap variables = createVariables();
    String decisionDefinitionId = decisionDefinition.getId();
    var decisionsEvaluationBuilder = decisionService.evaluateDecisionById(decisionDefinitionId)
        .variables(variables);

    // when/then
    assertThatThrownBy(decisionsEvaluationBuilder::evaluate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot evaluate the decision");
  }

  @Test
  void testEvaluateDecisionByKeyWithTenantIdAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
      .decisionDefinitionTenantId(TENANT_ONE)
      .variables(createVariables())
      .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  @Test
  void testEvaluateDecisionByIdAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);

    DecisionDefinition decisionDefinition = repositoryService
        .createDecisionDefinitionQuery()
        .singleResult();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionById(decisionDefinition.getId())
        .variables(createVariables())
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  @Test
  void testEvaluateDecisionByKeyWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_SECOND_VERSION);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(createVariables())
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  @Test
  void testEvaluateDecisionByKeyWithTenantIdDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, DMN_FILE);

    DmnDecisionResult decisionResult = decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .decisionDefinitionTenantId(TENANT_ONE)
        .variables(createVariables())
        .evaluate();

    assertThatDecisionHasResult(decisionResult, RESULT_OF_FIRST_VERSION);
  }

  protected VariableMap createVariables() {
    return Variables.createVariables().putValue("status", "silver").putValue("sum", 723);
  }

  protected void assertThatDecisionHasResult(DmnDecisionResult decisionResult, Object expectedValue) {
    assertThat(decisionResult)
            .isNotNull()
            .hasSize(1);
    String value = decisionResult.getSingleResult().getFirstEntry();
    assertThat(value).isEqualTo(expectedValue);
  }

}
