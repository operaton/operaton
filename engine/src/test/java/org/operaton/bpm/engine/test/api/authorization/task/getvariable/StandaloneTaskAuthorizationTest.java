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
package org.operaton.bpm.engine.test.api.authorization.task.getvariable;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yana.Vasileva
 *
 */
public abstract class StandaloneTaskAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected TaskService taskService;
  protected RuntimeService runtimeService;

  public static final String USER_ID = "userId";
  public String taskId = "myTask";
  public static final String VARIABLE_NAME = "aVariableName";
  public static final String VARIABLE_VALUE = "aVariableValue";
  public static final String PROCESS_KEY = "oneTaskProcess";
  protected boolean ensureSpecificVariablePermission;

  @BeforeEach
  public void setUp() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    taskService = engineRule.getTaskService();
    runtimeService = engineRule.getRuntimeService();

    authRule.createUserAndGroup("userId", "groupId");
  }

  @AfterEach
  public void tearDown() {
    authRule.deleteUsersAndGroups();
    taskService.deleteTask(taskId, true);
  }

  @TestTemplate
  void testGetVariable() {
    // given
    createTask(taskId);

    taskService.setVariables(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    Object variable = taskService.getVariable(taskId, VARIABLE_NAME);

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(variable).isEqualTo(VARIABLE_VALUE);
    }
  }

  @TestTemplate
  void testGetVariableLocal() {
    // given
    createTask(taskId);

    taskService.setVariablesLocal(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    Object variable = taskService.getVariableLocal(taskId, VARIABLE_NAME);

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(variable).isEqualTo(VARIABLE_VALUE);
    }
  }

  @TestTemplate
  void testGetVariableTyped() {
    // given
    createTask(taskId);

    taskService.setVariables(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    TypedValue typedValue = taskService.getVariableTyped(taskId, VARIABLE_NAME);

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(typedValue).isNotNull();
      assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE);
    }
  }

  @TestTemplate
  void testGetVariableLocalTyped() {
    // given
    createTask(taskId);

    taskService.setVariablesLocal(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    TypedValue typedValue = taskService.getVariableLocalTyped(taskId, VARIABLE_NAME);

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(typedValue).isNotNull();
      assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE);
    }
  }

  @TestTemplate
  void testGetVariables() {
    // given
    createTask(taskId);

    taskService.setVariables(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    Map<String, Object> variables = taskService.getVariables(taskId);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  @TestTemplate
  void testGetVariablesLocal() {
    // given
    createTask(taskId);

    taskService.setVariablesLocal(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    Map<String, Object> variables = taskService.getVariablesLocal(taskId);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  @TestTemplate
  void testGetVariablesTyped() {
    createTask(taskId);

    taskService.setVariables(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    VariableMap variables = taskService.getVariablesTyped(taskId);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  @TestTemplate
  void testGetVariablesLocalTyped() {
    createTask(taskId);

    taskService.setVariablesLocal(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    VariableMap variables = taskService.getVariablesLocalTyped(taskId);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  @TestTemplate
  void testGetVariablesByName() {
    // given
    createTask(taskId);

    taskService.setVariables(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    Map<String, Object> variables = taskService.getVariables(taskId, List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  @TestTemplate
  void testGetVariablesLocalByName() {
    // given
    createTask(taskId);

    taskService.setVariablesLocal(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    Map<String, Object> variables = taskService.getVariablesLocal(taskId, List.of(VARIABLE_NAME));

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  @TestTemplate
  void testGetVariablesTypedByName() {
    createTask(taskId);

    taskService.setVariables(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    VariableMap variables = taskService.getVariablesTyped(taskId, List.of(VARIABLE_NAME), false);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  @TestTemplate
  void testGetVariablesLocalTypedByName() {
    createTask(taskId);

    taskService.setVariablesLocal(taskId, getVariables());

    // when
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("taskId", taskId)
        .start();

    VariableMap variables = taskService.getVariablesLocalTyped(taskId, List.of(VARIABLE_NAME), false);

    // then
    if (authRule.assertScenario(scenario)) {
      verifyGetVariables(variables);
    }
  }

  protected void createTask(final String taskId) {
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);
  }

  protected VariableMap getVariables() {
    return Variables.createVariables().putValue(ProcessTaskAuthorizationTest.VARIABLE_NAME, ProcessTaskAuthorizationTest.VARIABLE_VALUE);
  }

  protected void verifyGetVariables(Map<String, Object> variables) {
    assertThat(variables)
            .isNotNull()
            .isNotEmpty();
    assertThat(variables)
            .hasSize(1)
            .containsEntry(ProcessTaskAuthorizationTest.VARIABLE_NAME, ProcessTaskAuthorizationTest.VARIABLE_VALUE);
  }

}
