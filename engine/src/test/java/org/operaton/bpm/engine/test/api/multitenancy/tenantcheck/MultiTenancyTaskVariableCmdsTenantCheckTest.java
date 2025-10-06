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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

class MultiTenancyTaskVariableCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String VARIABLE_1 = "testVariable1";
  protected static final String VARIABLE_2 = "testVariable2";

  protected static final String VARIABLE_VALUE_1 = "test1";
  protected static final String VARIABLE_VALUE_2 = "test2";

  protected static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  protected static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent()
    .userTask("task")
    .endEvent()
    .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  private TaskService taskService;

  protected String taskId;

  @BeforeEach
  void init() {

    // deploy tenants
    testRule.deployForTenant(TENANT_ONE, ONE_TASK_PROCESS);

    engineRule.getRuntimeService()
    .startProcessInstanceByKey(PROCESS_DEFINITION_KEY,
         Variables.createVariables()
         .putValue(VARIABLE_1, VARIABLE_VALUE_1)
         .putValue(VARIABLE_2, VARIABLE_VALUE_2))
    .getId();
    taskId = taskService.createTaskQuery().singleResult().getId();

  }

  // get task variable
  @Test
  void getTaskVariableWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    assertThat(taskService.getVariable(taskId, VARIABLE_1)).isEqualTo(VARIABLE_VALUE_1);
  }

  @Test
  void getTaskVariableWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.getVariable(taskId, VARIABLE_1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }


  @Test
  void getTaskVariableWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(taskService.getVariable(taskId, VARIABLE_1)).isEqualTo(VARIABLE_VALUE_1);
  }

  // get task variable typed
  @Test
  void getTaskVariableTypedWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(taskService.getVariableTyped(taskId, VARIABLE_1).getValue()).isEqualTo(VARIABLE_VALUE_1);

  }

  @Test
  void getTaskVariableTypedWithNoAuthenticatedTenant() {
    // given
    engineRule.getIdentityService().setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.getVariableTyped(taskId, VARIABLE_1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '"
          + taskId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void getTaskVariableTypedWithDisableTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(taskService.getVariableTyped(taskId, VARIABLE_1).getValue()).isEqualTo(VARIABLE_VALUE_1);
  }

  // get task variables
  @Test
  void getTaskVariablesWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(taskService.getVariables(taskId)).hasSize(2);
  }

  @Test
  void getTaskVariablesWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.getVariables(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void getTaskVariablesWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    assertThat(taskService.getVariables(taskId)).hasSize(2);
  }


  // set variable test
  @Test
  void setTaskVariableWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));
    taskService.setVariable(taskId, "newVariable", "newValue");

    assertThat(taskService.getVariables(taskId)).hasSize(3);
  }

  @Test
  void setTaskVariableWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.setVariable(taskId, "newVariable", "newValue"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void setTaskVariableWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    taskService.setVariable(taskId, "newVariable", "newValue");
    assertThat(taskService.getVariables(taskId)).hasSize(3);

  }

  // remove variable test
  @Test
  void removeTaskVariableWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));
    taskService.removeVariable(taskId, VARIABLE_1);
    // then
    assertThat(taskService.getVariables(taskId)).hasSize(1);
  }

  @Test
  void removeTaskVariablesWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.removeVariable(taskId, VARIABLE_1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the task '"
          + taskId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void removeTaskVariablesWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    taskService.removeVariable(taskId, VARIABLE_1);
    assertThat(taskService.getVariables(taskId)).hasSize(1);
  }

}
