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
package org.operaton.bpm.engine.test.api.authorization.jobdefinition;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@Parameterized
public class SetJobDefinitionPriorityAuthorizationTest {

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
          grant(Resources.PROCESS_DEFINITION, "processDefinitionKey", "userId", Permissions.UPDATE)),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "processDefinitionKey", "userId", Permissions.UPDATE))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "*", "userId", Permissions.UPDATE))
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
  void testSetJobDefinitionPriority() {

    // given
    JobDefinition jobDefinition = engineRule.getManagementService().createJobDefinitionQuery().singleResult();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processDefinitionKey", "process")
      .start();

    engineRule.getManagementService().setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42);

    // then
    if (authRule.assertScenario(scenario)) {
      JobDefinition updatedJobDefinition = engineRule.getManagementService().createJobDefinitionQuery().singleResult();
      assertThat((long) updatedJobDefinition.getOverridingJobPriority()).isEqualTo(42);
    }

  }

  @TestTemplate
  @Deployment(resources = "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml")
  void testResetJobDefinitionPriority() {
    // given
    JobDefinition jobDefinition = engineRule.getManagementService().createJobDefinitionQuery().singleResult();
    engineRule.getManagementService().setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42);

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processDefinitionKey", "process")
      .start();

    engineRule.getManagementService().clearOverridingJobPriorityForJobDefinition(jobDefinition.getId());

    // then
    if (authRule.assertScenario(scenario)) {
      JobDefinition updatedJobDefinition = engineRule.getManagementService().createJobDefinitionQuery().singleResult();
      assertThat(updatedJobDefinition.getOverridingJobPriority()).isNull();
    }
  }

}
