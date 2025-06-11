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
package org.operaton.bpm.engine.test.api.runtime;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

@Parameterized
public class CreateAndResolveIncidentAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.UPDATE),
          grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.UPDATE_INSTANCE)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.UPDATE)
        )
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.UPDATE_INSTANCE)
        )
        .succeeds()
    );
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  void createIncident() {
    //given
    testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process");
    ExecutionEntity execution = (ExecutionEntity) engineRule.getRuntimeService().createExecutionQuery().active().singleResult();

    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("processInstance", processInstance.getId())
        .bindResource("processDefinition", "Process")
        .start();

    engineRule.getRuntimeService()
        .createIncident("foo", execution.getId(), execution.getActivityId(), "bar");

    // then
    authRule.assertScenario(scenario);
  }

  @TestTemplate
  void resolveIncident() {
    testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process");
    ExecutionEntity execution = (ExecutionEntity) engineRule.getRuntimeService().createExecutionQuery().active().singleResult();

    authRule.disableAuthorization();
    Incident incident = engineRule.getRuntimeService()
        .createIncident("foo", execution.getId(), execution.getActivityId(), "bar");

    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("processInstance", processInstance.getId())
        .bindResource("processDefinition", "Process")
        .start();

    // when
    engineRule.getRuntimeService().resolveIncident(incident.getId());

    // then
    authRule.assertScenario(scenario);
  }
}
