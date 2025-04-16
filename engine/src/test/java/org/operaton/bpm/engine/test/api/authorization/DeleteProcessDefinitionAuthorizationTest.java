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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@Parameterized
public class DeleteProcessDefinitionAuthorizationTest {

  public static final String PROCESS_DEFINITION_KEY = "one";

  @RegisterExtension
  public static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  public AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  public ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(Resources.PROCESS_DEFINITION, PROCESS_DEFINITION_KEY, "userId", Permissions.DELETE)),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, PROCESS_DEFINITION_KEY, "userId", Permissions.DELETE))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "*", "userId", Permissions.DELETE))
        .succeeds()
      );
  }

  @BeforeEach
  void setUp() {
    authRule.createUserAndGroup("userId", "groupId");
    repositoryService = engineRule.getRepositoryService();
    runtimeService = engineRule.getRuntimeService();
    historyService = engineRule.getHistoryService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
    repositoryService = null;
    runtimeService = null;
    processEngineConfiguration = null;
  }

  @TestTemplate
  void testDeleteProcessDefinition() {
    testHelper.deploy("org/operaton/bpm/engine/test/repository/twoProcesses.bpmn20.xml");
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    authRule.init(scenario)
      .withUser("userId")
      .start();

    //when a process definition is been deleted
    repositoryService.deleteProcessDefinition(processDefinitions.get(0).getId());

    //then only one process definition should remain
    if (authRule.assertScenario(scenario)) {
      assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1);
    }
  }


  @TestTemplate
  void testDeleteProcessDefinitionCascade() {
    // given process definition and a process instance
    BpmnModelInstance bpmnModel = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY).startEvent().userTask().endEvent().done();
    testHelper.deploy(bpmnModel);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).executeWithVariablesInReturn();

    authRule.init(scenario)
      .withUser("userId")
      .start();

    //when the corresponding process definition is cascading deleted from the deployment
    repositoryService.deleteProcessDefinition(processDefinition.getId(), true);

    //then exist no process instance and no definition
    if (authRule.assertScenario(scenario)) {
      assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
      assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();
      if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {
        assertThat(engineRule.getHistoryService().createHistoricActivityInstanceQuery().count()).isZero();
      }
    }
  }

  @TestTemplate
  void testDeleteProcessDefinitionsByKey() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinition();
    }

    authRule.init(scenario)
      .withUser("userId")
      .start();

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(PROCESS_DEFINITION_KEY)
      .withoutTenantId()
      .delete();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();
    }
  }

  @TestTemplate
  void testDeleteProcessDefinitionsByKeyCascade() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinition();
    }

    runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    authRule.init(scenario)
      .withUser("userId")
      .start();

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(PROCESS_DEFINITION_KEY)
      .withoutTenantId()
      .cascade()
      .delete();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
      assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();

      if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {
        assertThat(historyService.createHistoricActivityInstanceQuery().count()).isZero();
      }
    }
  }

  @TestTemplate
  void testDeleteProcessDefinitionsByIds() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinition();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey(PROCESS_DEFINITION_KEY);

    authRule.init(scenario)
      .withUser("userId")
      .start();

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .delete();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();
    }
  }

  @TestTemplate
  void testDeleteProcessDefinitionsByIdsCascade() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinition();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey(PROCESS_DEFINITION_KEY);

    runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    authRule.init(scenario)
      .withUser("userId")
      .start();

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .cascade()
      .delete();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
      assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();

      if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {
        assertThat(historyService.createHistoricActivityInstanceQuery().count()).isZero();
      }
    }
  }

  private void deployProcessDefinition() {
    testHelper.deploy(Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .userTask()
      .endEvent()
      .done());
  }

  private String[] findProcessDefinitionIdsByKey(String processDefinitionKey) {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey(processDefinitionKey).list();
    List<String> processDefinitionIds = new ArrayList<>();
    for (ProcessDefinition processDefinition: processDefinitions) {
      processDefinitionIds.add(processDefinition.getId());
    }

    return processDefinitionIds.toArray(new String[0]);
  }

}
