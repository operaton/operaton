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
package org.operaton.bpm.engine.test.api.authorization.dmn;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philipp Ossler
 */
@Parameterized
public class EvaluateDecisionAuthorizationTest {

  protected static final String DMN_FILE = "org/operaton/bpm/engine/test/api/dmn/Example.dmn";
  protected static final String DECISION_DEFINITION_KEY = "decision";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(Resources.DECISION_DEFINITION, DECISION_DEFINITION_KEY, "userId", Permissions.CREATE_INSTANCE)),
      scenario()
        .withAuthorizations(
          grant(Resources.DECISION_DEFINITION, DECISION_DEFINITION_KEY, "userId", Permissions.CREATE_INSTANCE))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.DECISION_DEFINITION, "*", "userId", Permissions.CREATE_INSTANCE))
        .succeeds()
      );
  }

  @BeforeEach
  void setUp() {
    authRule.createUserAndGroup("userId", "groupId");
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  @Deployment(resources = DMN_FILE)
  void evaluateDecisionById() {

    // given
    DecisionDefinition decisionDefinition = engineRule.getRepositoryService().createDecisionDefinitionQuery().singleResult();

    // when
    authRule.init(scenario).withUser("userId").bindResource("decisionDefinitionKey", DECISION_DEFINITION_KEY).start();

    DmnDecisionTableResult decisionResult = engineRule.getDecisionService().evaluateDecisionTableById(decisionDefinition.getId(), createVariables());

    // then
    if (authRule.assertScenario(scenario)) {
      assertThatDecisionHasExpectedResult(decisionResult);
    }
  }

  @TestTemplate
  @Deployment(resources = DMN_FILE)
  void evaluateDecisionByKey() {

    // given
    DecisionDefinition decisionDefinition = engineRule.getRepositoryService().createDecisionDefinitionQuery().singleResult();

    // when
    authRule.init(scenario).withUser("userId").bindResource("decisionDefinitionKey", DECISION_DEFINITION_KEY).start();

    DmnDecisionTableResult decisionResult = engineRule.getDecisionService().evaluateDecisionTableByKey(decisionDefinition.getKey(), createVariables());

    // then
    if (authRule.assertScenario(scenario)) {
      assertThatDecisionHasExpectedResult(decisionResult);
    }
  }

  @TestTemplate
  @Deployment(resources = DMN_FILE)
  void evaluateDecisionByKeyAndVersion() {

    // given
    DecisionDefinition decisionDefinition = engineRule.getRepositoryService().createDecisionDefinitionQuery().singleResult();

    // when
    authRule.init(scenario).withUser("userId").bindResource("decisionDefinitionKey", DECISION_DEFINITION_KEY).start();

    DmnDecisionTableResult decisionResult = engineRule.getDecisionService().evaluateDecisionTableByKeyAndVersion(decisionDefinition.getKey(),
        decisionDefinition.getVersion(), createVariables());

    // then
    if (authRule.assertScenario(scenario)) {
      assertThatDecisionHasExpectedResult(decisionResult);
    }
  }

  protected VariableMap createVariables() {
    return Variables.createVariables().putValue("status", "silver").putValue("sum", 723);
  }

  protected void assertThatDecisionHasExpectedResult(DmnDecisionTableResult decisionResult) {
    assertThat(decisionResult)
            .isNotNull()
            .hasSize(1);
    String value = decisionResult.getSingleResult().getFirstEntry();
    assertThat(value).isEqualTo("ok");
  }

}
