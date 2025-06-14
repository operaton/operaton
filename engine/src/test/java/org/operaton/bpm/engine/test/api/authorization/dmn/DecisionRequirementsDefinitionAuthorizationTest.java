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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.authorization.Resources.DECISION_REQUIREMENTS_DEFINITION;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.io.InputStream;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

@Parameterized
public class DecisionRequirementsDefinitionAuthorizationTest {

  protected static final String DMN_FILE = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";
  protected static final String DRD_FILE = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.png";

  protected static final String DEFINITION_KEY = "dish";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  protected RepositoryService repositoryService;

  @Parameter(0)
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(DECISION_REQUIREMENTS_DEFINITION, DEFINITION_KEY, "userId", Permissions.READ)),
      scenario()
        .withAuthorizations(
          grant(DECISION_REQUIREMENTS_DEFINITION, DEFINITION_KEY, "userId", Permissions.READ))
          .succeeds(),
      scenario()
          .withAuthorizations(
            grant(DECISION_REQUIREMENTS_DEFINITION, "*", "userId", Permissions.READ))
            .succeeds()
      );
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
  @Deployment(resources = { DMN_FILE })
  void getDecisionRequirementsDefinition() {

    String decisionRequirementsDefinitionId = repositoryService
      .createDecisionRequirementsDefinitionQuery()
      .decisionRequirementsDefinitionKey(DEFINITION_KEY)
      .singleResult().getId();

    // when
    authRule.init(scenario).withUser("userId").bindResource("decisionRequirementsDefinitionKey", DEFINITION_KEY).start();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.getDecisionRequirementsDefinition(decisionRequirementsDefinitionId);

    if (authRule.assertScenario(scenario)) {
      assertThat(decisionRequirementsDefinition).isNotNull();
    }
  }

  @TestTemplate
  @Deployment(resources = { DMN_FILE })
  void getDecisionRequirementsModel() {

    // given
    String decisionRequirementsDefinitionId = repositoryService
      .createDecisionRequirementsDefinitionQuery()
      .decisionRequirementsDefinitionKey(DEFINITION_KEY)
      .singleResult().getId();

    // when
    authRule.init(scenario).withUser("userId").bindResource("decisionRequirementsDefinitionKey", DEFINITION_KEY).start();

    InputStream decisionRequirementsModel = repositoryService.getDecisionRequirementsModel(decisionRequirementsDefinitionId);

    if (authRule.assertScenario(scenario)) {
      assertThat(decisionRequirementsModel).isNotNull();
    }
  }

  @TestTemplate
  @Deployment(resources = { DMN_FILE, DRD_FILE })
  void getDecisionRequirementsDiagram() {

    // given
    String decisionRequirementsDefinitionId = repositoryService
      .createDecisionRequirementsDefinitionQuery()
      .decisionRequirementsDefinitionKey(DEFINITION_KEY)
      .singleResult().getId();

    // when
    authRule.init(scenario).withUser("userId").bindResource("decisionRequirementsDefinitionKey", DEFINITION_KEY).start();

    InputStream decisionRequirementsDiagram = repositoryService.getDecisionRequirementsDiagram(decisionRequirementsDefinitionId);

    if (authRule.assertScenario(scenario)) {
      assertThat(decisionRequirementsDiagram).isNotNull();
    }
  }
}
