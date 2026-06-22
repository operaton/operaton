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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinitionQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Resources.DECISION_REQUIREMENTS_DEFINITION;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.revoke;
import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class DecisionRequirementsDefinitionQueryAuthorizationTest {

  protected static final String DMN_FILE = "org/operaton/bpm/engine/test/dmn/deployment/drdScore.dmn11.xml";
  protected static final String ANOTHER_DMN = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  protected static final String DEFINITION_KEY = "score";
  protected static final String ANOTHER_DEFINITION_KEY = "dish";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  protected RepositoryService repositoryService;

  @Parameter(0)
  public AuthorizationScenario scenario;

  @Parameter(1)
  public String[] expectedDefinitionKeys;

  @Parameters
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
      { scenario()
          .withoutAuthorizations()
          .succeeds(), expectedDefinitions() },
      { scenario()
          .withAuthorizations(
           grant(DECISION_REQUIREMENTS_DEFINITION, DEFINITION_KEY, "userId", Permissions.READ))
          .succeeds(), expectedDefinitions(DEFINITION_KEY) },
      { scenario()
        .withAuthorizations(
          grant(DECISION_REQUIREMENTS_DEFINITION, ANY, "userId", Permissions.READ))
        .succeeds(), expectedDefinitions(DEFINITION_KEY, ANOTHER_DEFINITION_KEY) },
      { scenario()
          .withAuthorizations(
            grant(DECISION_REQUIREMENTS_DEFINITION, DEFINITION_KEY, "userId", Permissions.READ),
            grant(DECISION_REQUIREMENTS_DEFINITION, ANY, "userId", Permissions.READ))
          .succeeds(), expectedDefinitions(DEFINITION_KEY, ANOTHER_DEFINITION_KEY) },
      { scenario()
          .withAuthorizations(
            grant(DECISION_REQUIREMENTS_DEFINITION, ANY, ANY, Permissions.READ),
            revoke(DECISION_REQUIREMENTS_DEFINITION, ANY, "userId", Permissions.READ))
          .succeeds(), expectedDefinitions() }
    });
  }

  @BeforeEach
  void setUp() {
    authRule.createUserAndGroup("userId", "groupId");
    repositoryService = engineRule.getRepositoryService();
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  @Deployment(resources = { DMN_FILE, ANOTHER_DMN })
  void queryDecisionRequirementsDefinitions() {

    // when
    authRule.init(scenario).withUser("userId").bindResource("decisionRequirementsDefinitionKey", DEFINITION_KEY).start();

    DecisionRequirementsDefinitionQuery query = engineRule.getRepositoryService().createDecisionRequirementsDefinitionQuery();
    long count = query.count();
    List<DecisionRequirementsDefinition> definitions = query.list();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(count).isEqualTo(expectedDefinitionKeys.length);

      List<String> definitionKeys = getDefinitionKeys(definitions);
      assertThat(definitionKeys).containsExactlyInAnyOrder(expectedDefinitionKeys);
    }
  }

  protected List<String> getDefinitionKeys(List<DecisionRequirementsDefinition> definitions) {
    List<String> definitionKeys = new ArrayList<>();
    for (DecisionRequirementsDefinition definition : definitions) {
      definitionKeys.add(definition.getKey());
    }
    return definitionKeys;
  }

  protected static String[] expectedDefinitions(String... keys) {
    return keys;
  }

}
