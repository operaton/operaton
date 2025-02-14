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
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.variable.VariableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class FormAuthorizationTest extends AuthorizationTest {

  protected static final String FORM_PROCESS_KEY = "FormsProcess";
  protected static final String RENDERED_FORM_PROCESS_KEY = "renderedFormProcess";
  protected static final String CASE_KEY = "oneTaskCase";

  protected String deploymentId;
  protected boolean ensureSpecificVariablePermission;

  @Before
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

  @After
  @Override
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnforceSpecificVariablePermission(ensureSpecificVariablePermission);
  }

  // get start form data ///////////////////////////////////////////

  @Test
  public void testGetStartFormDataWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    try {
      // when
      formService.getStartFormData(processDefinitionId);
      fail("Exception expected: It should not be possible to get start form data");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testGetStartFormData() {
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
  public void testGetRenderedStartFormWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(RENDERED_FORM_PROCESS_KEY).getId();

    try {
      // when
      formService.getRenderedStartForm(processDefinitionId);
      fail("Exception expected: It should not be possible to get start form data");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(RENDERED_FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testGetRenderedStartForm() {
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
  public void testGetStartFormVariablesWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(RENDERED_FORM_PROCESS_KEY).getId();

    try {
      // when
      formService.getStartFormVariables(processDefinitionId);
      fail("Exception expected: It should not be possible to get start form data");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(RENDERED_FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testGetStartFormVariables() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(RENDERED_FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ);

    // when
    VariableMap variables = formService.getStartFormVariables(processDefinitionId);

    // then
    assertThat(variables).isNotNull();
    assertThat(variables).hasSize(1);
  }

  // submit start form /////////////////////////////////////////

  @Test
  public void testSubmitStartFormWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    try {
      // when
      formService.submitStartForm(processDefinitionId, null);
      fail("Exception expected: It should not possible to submit a start form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(CREATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  @Test
  public void testSubmitStartFormWithCreatePermissionOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    try {
      // when
      formService.submitStartForm(processDefinitionId, null);
      fail("Exception expected: It should not possible to submit a start form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(CREATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testSubmitStartFormWithCreateInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, CREATE_INSTANCE);

    try {
      // when
      formService.submitStartForm(processDefinitionId, null);
      fail("Exception expected: It should not possible to submit a start form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(CREATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  @Test
  public void testSubmitStartForm() {
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
  public void testStandaloneTaskGetTaskFormDataWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    try {
      // when
      formService.getTaskFormData(taskId);
      fail("Exception expected: It should not possible to get task form data");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    try {
      // when (2)
      formService.getTaskFormData(taskId);
      fail("Exception expected: It should not possible to get task form data");
    } catch (AuthorizationException e) {
      // then (2)
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ_VARIABLE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }

    deleteTask(taskId, true);
  }

  @Test
  public void testStandaloneTaskGetTaskFormData() {
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
  public void testStandaloneTaskGetTaskFormDataWithReadVariablePermission() {
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
  public void testProcessTaskGetTaskFormDataWithoutAuthorization() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    try {
      // when
      formService.getTaskFormData(taskId);
      fail("Exception expected: It should not possible to get task form data");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
      testRule.assertTextPresent(READ_TASK.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    try {
      // when (2)
      formService.getTaskFormData(taskId);
      fail("Exception expected: It should not possible to get task form data");
    } catch (AuthorizationException e) {
      // then (2)
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ_VARIABLE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
      testRule.assertTextPresent(READ_TASK_VARIABLE.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testProcessTaskGetTaskFormDataWithReadPermissionOnTask() {
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
  public void testProcessTaskGetTaskFormDataWithReadTaskPermissionOnProcessDefinition() {
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
  public void testProcessTaskGetTaskFormDataWithReadVariablePermissionOnTask() {
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
  public void testProcessTaskGetTaskFormDataWithReadTaskVariablePermissionOnProcessDefinition() {
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
  public void testProcessTaskGetTaskFormData() {
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
  public void testCaseTaskGetTaskFormData() {
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
  public void testStandaloneTaskGetTaskRenderedFormWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    try {
      // when
      formService.getRenderedTaskForm(taskId);
      fail("Exception expected: It should not possible to get rendered task form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    try {
      // when (2)
      formService.getRenderedTaskForm(taskId);
      fail("Exception expected: It should not possible to get rendered task form");
    } catch (AuthorizationException e) {
      // then (2)
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ_VARIABLE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }


    deleteTask(taskId, true);
  }

  @Test
  public void testStandaloneTaskGetTaskRenderedForm() {
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
  public void testStandaloneTaskGetTaskRenderedFormWithReadVariablePermission() {
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
  public void testProcessTaskGetRenderedTaskFormWithoutAuthorization() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    try {
      // when
      formService.getRenderedTaskForm(taskId);
      fail("Exception expected: It should not possible to get rendered task form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
      testRule.assertTextPresent(READ_TASK.getName(), message);
      testRule.assertTextPresent(RENDERED_FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    try {
      // when (2)
      formService.getRenderedTaskForm(taskId);
      fail("Exception expected: It should not possible to get rendered task form");
    } catch (AuthorizationException e) {
      // then (2)
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ_VARIABLE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
      testRule.assertTextPresent(READ_TASK_VARIABLE.getName(), message);
      testRule.assertTextPresent(RENDERED_FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testProcessTaskGetRenderedTaskFormWithReadPermissionOnTask() {
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
  public void testProcessTaskGetRenderedTaskFormWithReadTaskPermissionOnProcessDefinition() {
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
  public void testProcessTaskGetRenderedTaskFormWithReadTaskVariablesPermissionOnProcessDefinition() {
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
  public void testProcessTaskGetRenderedTaskFormWithReadVariablePermissionOnTask() {
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
  public void testProcessTaskGetRenderedTaskForm() {
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
  public void testCaseTaskGetRenderedTaskForm() {
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
  public void testStandaloneTaskGetTaskFormVariablesWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    try {
      // when
      formService.getTaskFormVariables(taskId);
      fail("Exception expected: It should not possible to get task form variables");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }

    // given (2)
    setReadVariableAsDefaultReadVariablePermission();

    try {
      // when (2)
      formService.getTaskFormVariables(taskId);
      fail("Exception expected: It should not possible to get task form variables");
    } catch (AuthorizationException e) {
      // then (2)
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ_VARIABLE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }

    deleteTask(taskId, true);
  }

  @Test
  public void testStandaloneTaskGetTaskFormVariables() {
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
  public void testStandaloneTaskGetTaskFormVariablesWithReadVariablePermission() {
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
  public void testProcessTaskGetTaskFormVariablesWithoutAuthorization() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    try {
      // when
      formService.getTaskFormVariables(taskId);
      fail("Exception expected: It should not possible to get task form variables");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
      testRule.assertTextPresent(READ_TASK.getName(), message);
      testRule.assertTextPresent(RENDERED_FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }

    // given (2)
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    try {
      // when (2)
      formService.getTaskFormVariables(taskId);
      fail("Exception expected: It should not possible to get task form variables");
    } catch (AuthorizationException e) {
      // then (2)
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ_VARIABLE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
      testRule.assertTextPresent(READ_TASK_VARIABLE.getName(), message);
      testRule.assertTextPresent(RENDERED_FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testProcessTaskGetTaskFormVariablesWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();
    assertThat(variables).hasSize(1);
  }

  @Test
  public void testProcessTaskGetTaskFormVariablesWithReadTaskPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();
    assertThat(variables).hasSize(1);
  }

  @Test
  public void testProcessTaskGetTaskFormVariables() {
    // given
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();
    assertThat(variables).hasSize(1);
  }

  @Test
  public void testProcessTaskGetTaskFormVariablesWithReadVariablePermissionOnTask() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();
    assertThat(variables).hasSize(1);
  }

  @Test
  public void testProcessTaskGetTaskFormVariablesWithReadTaskVariablePermissionOnProcessDefinition() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(RENDERED_FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, RENDERED_FORM_PROCESS_KEY, userId, READ_TASK_VARIABLE);

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();
    assertThat(variables).hasSize(1);
  }

  // get task form variables (case task) /////////////////////////////////

  @Test
  public void testCaseTaskGetTaskFormVariables() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    VariableMap variables = formService.getTaskFormVariables(taskId);

    // then
    assertThat(variables).isNotNull();
    assertThat(variables).isEmpty();
  }

  // submit task form (standalone task) ////////////////////////////////

  @Test
  public void testStandaloneTaskSubmitTaskFormWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    try {
      // when
      formService.submitTaskForm(taskId, null);
      fail("Exception expected: It should not possible to submit a task form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }

    deleteTask(taskId, true);
  }

  @Test
  public void testStandaloneTaskSubmitTaskForm() {
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
  public void testProcessTaskSubmitTaskFormWithoutAuthorization() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    try {
      // when
      formService.submitTaskForm(taskId, null);
      fail("Exception expected: It should not possible to submit a task form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(taskId, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
      testRule.assertTextPresent(UPDATE_TASK.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testProcessTaskSubmitTaskFormWithUpdatePermissionOnTask() {
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

  @Test
  public void testProcessTaskSubmitTaskFormWithUpdateTaskPermissionOnProcessDefinition() {
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

  @Test
  public void testProcessTaskSubmitTaskForm() {
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
  public void testCaseTaskSubmitTaskForm() {
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
  public void testGetStartFormKeyWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    try {
      // when
      formService.getStartFormKey(processDefinitionId);
      fail("Exception expected: It should not possible to get a start form key");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testGetStartFormKey() {
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
  public void testGetTaskFormKeyWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    try {
      // when
      formService.getTaskFormKey(processDefinitionId, "task");
      fail("Exception expected: It should not possible to get a task form key");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  public void testGetTaskFormKey() {
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
  public void testGetDeployedStartForm() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, FORM_PROCESS_KEY, userId, READ);

    // when
    InputStream inputStream = formService.getDeployedStartForm(processDefinitionId);
    assertThat(inputStream).isNotNull();
  }

  @Test
  public void testGetDeployedStartFormWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(FORM_PROCESS_KEY).getId();

    try {
      // when
      formService.getDeployedStartForm(processDefinitionId);
      fail("Exception expected: It should not possible to get a deployed start form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  // get deployed task form////////////////////////////////////////

  @Test
  public void testGetDeployedTaskForm() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    InputStream inputStream = formService.getDeployedTaskForm(taskId);
    assertThat(inputStream).isNotNull();
  }

  @Test
  public void testGetDeployedTaskFormWithoutAuthorization() {
    // given
    startProcessInstanceByKey(FORM_PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    try {
      // when
      formService.getDeployedTaskForm(taskId);
      fail("Exception expected: It should not possible to get a deployed task form");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ.getName(), message);
      testRule.assertTextPresent(FORM_PROCESS_KEY, message);
      testRule.assertTextPresent(TASK.resourceName(), message);
    }
  }

  // helper ////////////////////////////////////////////////////////////////////////////////

  protected void setReadVariableAsDefaultReadVariablePermission() {
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);
  }
}
