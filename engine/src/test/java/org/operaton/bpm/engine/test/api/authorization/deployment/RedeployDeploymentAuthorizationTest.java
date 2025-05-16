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
package org.operaton.bpm.engine.test.api.authorization.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;


/**
 * @author Roman Smirnov
 *
 */
@Parameterized
public class RedeployDeploymentAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestExtension.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(Resources.DEPLOYMENT, "*", "userId", Permissions.CREATE)),
      scenario()
        .withAuthorizations(
          grant(Resources.DEPLOYMENT, "*", "userId", Permissions.CREATE))
        .failsDueToRequired(
          grant(Resources.DEPLOYMENT, "deploymentId", "userId", Permissions.READ)),
      scenario()
        .withAuthorizations(
          grant(Resources.DEPLOYMENT, "deploymentId", "userId", Permissions.READ),
          grant(Resources.DEPLOYMENT, "*", "userId", Permissions.CREATE))
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
  void testRedeploy() {
    // given
    RepositoryService repositoryService = engineRule.getRepositoryService();

    BpmnModelInstance model1 = Bpmn.createExecutableProcess("process1").startEvent().done();
    BpmnModelInstance model2 = Bpmn.createExecutableProcess("process2").startEvent().done();

    // first deployment
    Deployment deployment1 = repositoryService
        .createDeployment()
        .addModelInstance("process1.bpmn", model1)
        .addModelInstance("process2.bpmn", model2)
        .deploy();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("deploymentId", deployment1.getId())
      .start();

    Deployment deployment2 = repositoryService
      .createDeployment()
      .addDeploymentResources(deployment1.getId())
      .deploy();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
      deleteDeployments(deployment2);
      deleteAuthorizations();
    }

    deleteDeployments(deployment1);
  }

  protected void deleteDeployments(Deployment... deployments){
    for (Deployment deployment : deployments) {
      engineRule.getRepositoryService().deleteDeployment(deployment.getId(), true);
    }
  }

  protected void deleteAuthorizations() {
    AuthorizationService authorizationService = engineRule.getAuthorizationService();
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

}
