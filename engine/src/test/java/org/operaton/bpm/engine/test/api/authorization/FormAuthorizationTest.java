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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_TASK;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_TASK;
import static org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions.READ_TASK_VARIABLE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.operaton.bpm.engine.authorization.TaskPermissions.READ_VARIABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.variable.VariableMap;

/**
 * @author Roman Smirnov
 *
 */
class FormAuthorizationTest extends AuthorizationTest {

  protected static final String FORM_PROCESS_KEY = "FormsProcess";
  protected static final String RENDERED_FORM_PROCESS_KEY = "renderedFormProcess";
  protected static final String CASE_KEY = "oneTaskCase";

  protected String deploymentId;
  protected boolean ensureSpecificVariablePermission;

  @BeforeEach
  @Override
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/form/DeployedFormsProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/form/start.html",
        "org/operaton/bpm/engine/test/api/form/task.html",
        "org/operaton/bpm/engine/test/api/authorization/renderedFormProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn").getId();
    ensureSpecificVariablePermission = processEngineConfiguration.isEnforceSpecificVariablePermission();
    super.setUp();
  }

  @AfterEach
  @Override
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnforceSpecificVariablePermission(ensureSpecificVariablePermission);
  }

  // get start form data ///////////////////////////////////////////

  @Test
  void testGetStartFormDataWithoutAuthorizations() {
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    assertThatThrownBy(() -> formService.getStartFormData(processDefinitionId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetStartFormData() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ);

    // when
    StartFormData startFormData = formService.getStartFormData(processDefinitionId);

    // then
    assertThat(startFormData).isNotNull();
    assertThat(startFormData.getFormKey()).isEqualTo("deployment:org/operaton/bpm/engine/test/api/form/start.html");
  }

  // get rendered start form /////////////////////////////////////

  @Test
  void testGetRenderedStartFormWithoutAuthorization() {
    String processDefinitionId = selectProcessDefinitionByKey(RENDERED_FORM_PROCESS_KEY).getId();

    assertThatThrownBy(() -> formService.getRenderedStartForm(processDefinitionId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(RENDERED_FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetRenderedStartForm() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(RENDERED_FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ);

    // when
    Object renderedStartForm = formService.getRenderedStartForm(processDefinitionId);

    // then
    assertThat(renderedStartForm).isNotNull();
  }

  // get start form variables //////////////////////////////////

  @Test
  void testGetStartFormVariablesWithoutAuthorization() {
    String processDefinitionId = selectProcessDefinitionByKey(RENDERED_FORM_PROCESS_KEY).getId();

    assertThatThrownBy(() -> formService.getStartFormVariables(processDefinitionId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(RENDERED_FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetStartFormVariables() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(RENDERED_FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ);

    // when
    VariableMap variables = formService.getStartFormVariables(processDefinitionId);

    // then
    assertThat(variables)
            .isNotNull()
            .hasSize(1);
  }

  // submit start form /////////////////////////////////////////

  @Test
  void testSubmitStartFormWithoutAuthorization() {
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    assertThatThrownBy(() -> formService.submitStartForm(processDefinitionId, null),
            "It should not possible to submit a start form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(CREATE.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName());
  }

  @Test
  void testSubmitStartFormWithCreatePermissionOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when + then
    assertThatThrownBy(() -> formService.submitStartForm(processDefinitionId, null),
            "It should not possible to submit a start form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(CREATE_INSTANCE.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSubmitStartFormWithCreateInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, CREATE_INSTANCE);

    // when + then
    assertThatThrownBy(() -> formService.submitStartForm(processDefinitionId, null),
            "It should not possible to submit a start form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(CREATE.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName());
  }

  @Test
  void testSubmitStartForm() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    ProcessInstance instance = formService.submitStartForm(processDefinitionId, null);

    // then
    assertThat(instance).isNotNull();
  }

  // get task form data (standalone task) /////////////////////////////////

  @Test
  void testStandaloneTaskGetTaskFormDataWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when + then
    assertThatThrownBy(() -> formService.getTaskFormData(taskId),
            "It should not possible to get task form data")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName());


    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    // when + then (2)
    assertThatThrownBy(() -> formService.getTaskFormData(taskId),
            "It should not possible to get task form data")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ_VARIABLE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName());

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskGetTaskFormData() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    // Standalone task, no TaskFormData available
    assertThat(taskFormData).isNull();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskGetTaskFormDataWithReadVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    // Standalone task, no TaskFormData available
    assertThat(taskFormData).isNull();

    deleteTask(taskId, true);
  }

  // get task form data (process task) /////////////////////////////////

  @Test
  void testProcessTaskGetTaskFormDataWithoutAuthorization() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when + then
    assertThatThrownBy(() -> formService.getTaskFormData(taskId),
            "It should not possible to get task form data")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(READ_TASK.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    // when + then (2)
    assertThatThrownBy(() -> formService.getTaskFormData(taskId),
            "It should not possible to get task form data")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ_VARIABLE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(READ_TASK_VARIABLE.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskGetTaskFormDataWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
  }

  @Test
  void testProcessTaskGetTaskFormDataWithReadTaskPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
  }

  @Test
  void testProcessTaskGetTaskFormDataWithReadVariablePermissionOnTask() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
  }

  @Test
  void testProcessTaskGetTaskFormDataWithReadTaskVariablePermissionOnProcessDefinition() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ_TASK_VARIABLE);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
  }

  @Test
  void testProcessTaskGetTaskFormData() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
  }

  // get task form data (case task) /////////////////////////////////

  @Test
  void testCaseTaskGetTaskFormData() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
  }

  // get rendered task form (standalone task) //////////////////

  @Test
  void testStandaloneTaskGetTaskRenderedFormWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when + then
    assertThatThrownBy(() -> formService.getRenderedTaskForm(taskId),
            "It should not possible to get rendered task form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName());

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    // when + then (2)
    assertThatThrownBy(() -> formService.getRenderedTaskForm(taskId),
            "It should not possible to get rendered task form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ_VARIABLE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName());

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskGetTaskRenderedForm() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    // Standalone task, no TaskFormData available
    assertThatExceptionOfType(NullValueException.class).isThrownBy(() -> formService.getRenderedTaskForm(taskId));

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskGetTaskRenderedFormWithReadVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    // Standalone task, no TaskFormData available
    assertThatExceptionOfType(NullValueException.class).isThrownBy(() -> formService.getRenderedTaskForm(taskId));

    deleteTask(taskId, true);
  }

  // get rendered task form (process task) /////////////////////////////////

  @Test
  void testProcessTaskGetRenderedTaskFormWithoutAuthorization() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when + then
    assertThatThrownBy(() -> formService.getRenderedTaskForm(taskId),
            "It should not possible to get rendered task form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(READ_TASK.getName())
        .hasMessageContaining(RENDERED_FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    // when + then (2)
    assertThatThrownBy(() -> formService.getRenderedTaskForm(taskId),
            "It should not possible to get rendered task form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ_VARIABLE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(READ_TASK_VARIABLE.getName())
        .hasMessageContaining(RENDERED_FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskGetRenderedTaskFormWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    Object taskForm = formService.getRenderedTaskForm(taskId);

    // then
    assertThat(taskForm).isNotNull();
  }

  @Test
  void testProcessTaskGetRenderedTaskFormWithReadTaskPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    Object taskForm = formService.getRenderedTaskForm(taskId);

    // then
    assertThat(taskForm).isNotNull();
  }

  @Test
  void testProcessTaskGetRenderedTaskFormWithReadTaskVariablesPermissionOnProcessDefinition() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK_VARIABLE);

    // when
    Object taskForm = formService.getRenderedTaskForm(taskId);

    // then
    assertThat(taskForm).isNotNull();
  }

  @Test
  void testProcessTaskGetRenderedTaskFormWithReadVariablePermissionOnTask() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    Object taskForm = formService.getRenderedTaskForm(taskId);

    // then
    assertThat(taskForm).isNotNull();
  }

  @Test
  void testProcessTaskGetRenderedTaskForm() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    Object taskForm = formService.getRenderedTaskForm(taskId);

    // then
    assertThat(taskForm).isNotNull();
  }

  // get rendered task form (case task) /////////////////////////////////

  @Test
  void testCaseTaskGetRenderedTaskForm() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    Object taskForm = formService.getRenderedTaskForm(taskId);

    // then
    assertThat(taskForm).isNull();
  }

  // get task form variables (standalone task) ////////////////////////

  @Test
  void testStandaloneTaskGetTaskFormVariablesWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when + then
    assertThatThrownBy(() -> formService.getTaskFormVariables(taskId),
            "It should not possible to get task form variables")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName());

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    // when + then (2)
    assertThatThrownBy(() -> formService.getTaskFormVariables(taskId),
            "It should not possible to get task form variables")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ_VARIABLE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName());

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskGetTaskFormVariables() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskGetTaskFormVariablesWithReadVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();

    deleteTask(taskId, true);
  }

  // get task form variables (process task) /////////////////////////////////

  @Test
  void testProcessTaskGetTaskFormVariablesWithoutAuthorization() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when + then
    assertThatThrownBy(() -> formService.getTaskFormVariables(taskId),
            "It should not possible to get task form variables")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(READ_TASK.getName())
        .hasMessageContaining(RENDERED_FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());

    // given (2)
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    // when + then (2)
    assertThatThrownBy(() -> formService.getTaskFormVariables(taskId),
            "It should not possible to get task form variables")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ_VARIABLE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(READ_TASK_VARIABLE.getName())
        .hasMessageContaining(RENDERED_FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskGetTaskFormVariablesWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables)
            .isNotNull()
            .hasSize(1);
  }

  @Test
  void testProcessTaskGetTaskFormVariablesWithReadTaskPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables)
            .isNotNull()
            .hasSize(1);
  }

  @Test
  void testProcessTaskGetTaskFormVariables() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables)
            .isNotNull()
            .hasSize(1);
  }

  @Test
  void testProcessTaskGetTaskFormVariablesWithReadVariablePermissionOnTask() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables)
            .isNotNull()
            .hasSize(1);
  }

  @Test
  void testProcessTaskGetTaskFormVariablesWithReadTaskVariablePermissionOnProcessDefinition() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK_VARIABLE);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables)
            .isNotNull()
            .hasSize(1);
  }

  // get task form variables (case task) /////////////////////////////////

  @Test
  void testCaseTaskGetTaskFormVariables() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull().isEmpty();
  }

  // submit task form (standalone task) ////////////////////////////////

  @Test
  void testStandaloneTaskSubmitTaskFormWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when + then
    assertThatThrownBy(() -> formService.submitTaskForm(taskId, null),
            "It should not possible to submit a task form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName());

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSubmitTaskForm() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    formService.submitTaskForm(taskId, null);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();

    deleteTask(taskId, true);
  }

  // submit task form (process task) ////////////////////////////////

  @Test
  void testProcessTaskSubmitTaskFormWithoutAuthorization() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when + then
    assertThatThrownBy(() -> formService.submitTaskForm(taskId, null),
            "It should not possible to submit a task form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(UPDATE_TASK.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskSubmitTaskFormWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    formService.submitTaskForm(taskId, null);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  @Test
  void testProcessTaskSubmitTaskFormWithUpdateTaskPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, UPDATE_TASK);

    // when
    formService.submitTaskForm(taskId, null);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  @Test
  void testProcessTaskSubmitTaskForm() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, UPDATE_TASK);

    // when
    formService.submitTaskForm(taskId, null);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  // submit task form (case task) ////////////////////////////////

  @Test
  void testCaseTaskSubmitTaskForm() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    formService.submitTaskForm(taskId, null);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  // get start form key ////////////////////////////////////////

  @Test
  void testGetStartFormKeyWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> formService.getStartFormKey(processDefinitionId),
            "It should not possible to get a start form key")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetStartFormKey() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ);

    // when
    String formKey = formService.getStartFormKey(processDefinitionId);

    // then
    assertThat(formKey).isEqualTo("deployment:org/operaton/bpm/engine/test/api/form/start.html");
  }

  // get task form key ////////////////////////////////////////

  @Test
  void testGetTaskFormKeyWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> formService.getTaskFormKey(processDefinitionId, "task"),
            "It should not possible to get a task form key")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetTaskFormKey() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ);

    // when
    String formKey = formService.getTaskFormKey(processDefinitionId, "task");

    // then
    assertThat(formKey).isEqualTo("deployment:org/operaton/bpm/engine/test/api/form/task.html");
  }

  // get deployed start form////////////////////////////////////////

  @Test
  void testGetDeployedStartForm() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ);

    // when
    InputStream inputStream = formService.getDeployedStartForm(processDefinitionId);
    assertThat(inputStream).isNotNull();
  }

  @Test
  void testGetDeployedStartFormWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> formService.getDeployedStartForm(processDefinitionId),
            "It should not possible to get a deployed start form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  // get deployed task form////////////////////////////////////////

  @Test
  void testGetDeployedTaskForm() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    InputStream inputStream = formService.getDeployedTaskForm(taskId);
    assertThat(inputStream).isNotNull();
  }

  @Test
  void testGetDeployedTaskFormWithoutAuthorization() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when + then
    assertThatThrownBy(() -> formService.getDeployedTaskForm(taskId),
            "It should not possible to get a deployed task form")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(FORM_PROCESS_KEY)
        .hasMessageContaining(TASK.resourceName());
  }

  // helper ////////////////////////////////////////////////////////////////////////////////

  protected void setReadVariableAsDefaultReadVariablePermission() {
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);
  }
}
