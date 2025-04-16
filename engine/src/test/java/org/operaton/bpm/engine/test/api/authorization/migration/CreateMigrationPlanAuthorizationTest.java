/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.authorization.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
@Parameterized
public class CreateMigrationPlanAuthorizationTest {

  @RegisterExtension
  public static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  public AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  public ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.READ)),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.READ))
        .failsDueToRequired(
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.READ)),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.READ))
        .failsDueToRequired(
            grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.READ)),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.READ),
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.READ))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "*", "userId", Permissions.READ))
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
  @Deployment(resources = "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml")
  void testCreate() {

    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("sourceDefinitionKey", sourceDefinition.getKey())
      .bindResource("targetDefinitionKey", targetDefinition.getKey())
      .start();

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
      .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities()
      .build();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(migrationPlan).isNotNull();
    }

  }
}
