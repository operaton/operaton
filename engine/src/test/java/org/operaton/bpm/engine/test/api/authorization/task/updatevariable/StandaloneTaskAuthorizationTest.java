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
package org.operaton.bpm.engine.test.api.authorization.task.updatevariable;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.TaskServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.operaton.bpm.engine.authorization.TaskPermissions.UPDATE_VARIABLE;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.revoke;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yana.Vasileva
 *
 */
@Parameterized
public class StandaloneTaskAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected TaskService taskService;
  protected RuntimeService runtimeService;
  protected HistoryService historyService;

  protected static final String USER_ID = "userId";
  protected String taskId = "myTask";
  protected static final String VARIABLE_NAME = "aVariableName";
  protected static final String VARIABLE_VALUE = "aVariableValue";
  protected static final String PROCESS_KEY = "oneTaskProcess";

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(TASK, "taskId", USER_ID, UPDATE),
          grant(TASK, "taskId", USER_ID, UPDATE_VARIABLE)),
      scenario()
        .withAuthorizations(
          grant(TASK, "taskId", USER_ID, UPDATE)),
      scenario()
        .withAuthorizations(
          grant(TASK, "*", USER_ID, UPDATE)),
      scenario()
        .withAuthorizations(
          grant(TASK, "taskId", USER_ID, UPDATE_VARIABLE)),
      scenario()
        .withAuthorizations(
          grant(TASK, "*", USER_ID, UPDATE_VARIABLE))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(TASK, "*", "*", UPDATE),
          revoke(TASK, "taskId", USER_ID, UPDATE))
        .failsDueToRequired(
          grant(TASK, "taskId", USER_ID, UPDATE),
          grant(TASK, "taskId", USER_ID, UPDATE_VARIABLE))
      );
  }

  @BeforeEach
  void setUp() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    taskService = engineRule.getTaskService();
    runtimeService = engineRule.getRuntimeService();
    historyService = engineRule.getHistoryService();

    authRule.createUserAndGroup("userId", "groupId");
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
    taskService.deleteTask(taskId, true);
    for (HistoricVariableInstance historicVariableInstance : historyService.createHistoricVariableInstanceQuery().includeDeleted().list()) {
      historyService.deleteHistoricVariableInstance(historicVariableInstance.getId());
    }
  }

  @TestTemplate
  void testSetVariable() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.setVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // then
    if (authRule.assertScenario(scenario)) {
      verifySetVariables();
    }
  }

  @TestTemplate
  void testSetVariableLocal() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.setVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // then
    if (authRule.assertScenario(scenario)) {
      verifySetVariables();
    }
  }

  @TestTemplate
  void testSetVariables() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.setVariables(taskId, getVariables());

    // then
    if (authRule.assertScenario(scenario)) {
      verifySetVariables();
    }
  }

  @TestTemplate
  void testSetVariablesLocal() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.setVariablesLocal(taskId, getVariables());

    // then
    if (authRule.assertScenario(scenario)) {
      verifySetVariables();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testRemoveVariable() {
    // given
    createTask(taskId);

    taskService.setVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.removeVariable(taskId, VARIABLE_NAME);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testRemoveVariableLocal() {
    // given
    createTask(taskId);

    taskService.setVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.removeVariableLocal(taskId, VARIABLE_NAME);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testRemoveVariables() {
    // given
    createTask(taskId);

    taskService.setVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.removeVariables(taskId, List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testRemoveVariablesLocal() {
    // given
    createTask(taskId);

    taskService.setVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    taskService.removeVariablesLocal(taskId, List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testUpdateVariablesAdd() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    ((TaskServiceImpl) taskService).updateVariables(taskId, getVariables(), null);

    // then
    if (authRule.assertScenario(scenario)) {
      verifySetVariables();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testUpdateVariablesRemove() {
    // given
    createTask(taskId);
    taskService.setVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    ((TaskServiceImpl) taskService).updateVariables(taskId, null, List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testUpdateVariablesAddRemove() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    ((TaskServiceImpl) taskService).updateVariables(taskId, getVariables(), List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testUpdateVariablesLocalAdd() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    ((TaskServiceImpl) taskService).updateVariablesLocal(taskId, getVariables(), null);

    // then
    if (authRule.assertScenario(scenario)) {
      verifySetVariables();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testUpdateVariablesLocalRemove() {
    // given
    createTask(taskId);
    taskService.setVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    ((TaskServiceImpl) taskService).updateVariablesLocal(taskId, null, List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  @TestTemplate
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testUpdateVariablesLocalAddRemove() {
    // given
    createTask(taskId);

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    ((TaskServiceImpl) taskService).updateVariablesLocal(taskId, getVariables(), List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyRemoveVariable();
    }
  }

  protected void verifySetVariables() {
    verifyVariableInstanceCount(1);
    assertThat(runtimeService.createVariableInstanceQuery().singleResult()).isNotNull();
  }

  protected void verifyRemoveVariable() {
    verifyVariableInstanceCount(0);
    assertThat(runtimeService.createVariableInstanceQuery().singleResult()).isNull();
    HistoricVariableInstance deletedVariable = historyService.createHistoricVariableInstanceQuery().includeDeleted().singleResult();
    assertThat(deletedVariable.getState()).isEqualTo("DELETED");
  }

  protected void verifyVariableInstanceCount(int count) {
    assertThat(runtimeService.createVariableInstanceQuery().list()).hasSize(count);
    assertThat(runtimeService.createVariableInstanceQuery().count()).isEqualTo(count);
  }

  protected void createTask(final String taskId) {
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);
  }

  protected VariableMap getVariables() {
    return Variables.createVariables().putValue(VARIABLE_NAME, VARIABLE_VALUE);
  }

}
