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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
class MultiTenancyExecutionVariableCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String VARIABLE_1 = "testVariable1";
  protected static final String VARIABLE_2 = "testVariable2";

  protected static final String VARIABLE_VALUE_1 = "test1";
  protected static final String VARIABLE_VALUE_2 = "test2";

  protected static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  protected static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .userTask()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected String processInstanceId;

  @BeforeEach
  void init() {

    // deploy tenants
    testRule.deployForTenant(TENANT_ONE, ONE_TASK_PROCESS);

    processInstanceId = engineRule.getRuntimeService()
    .startProcessInstanceByKey(PROCESS_DEFINITION_KEY,
         Variables.createVariables()
         .putValue(VARIABLE_1, VARIABLE_VALUE_1)
         .putValue(VARIABLE_2, VARIABLE_VALUE_2))
    .getId();
  }

  @Test
  void getExecutionVariableWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(engineRule.getRuntimeService().getVariable(processInstanceId, VARIABLE_1)).isEqualTo(VARIABLE_VALUE_1);
  }

  @Test
  void getExecutionVariableWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    var runtimeService = engineRule.getRuntimeService();

    // when/then
    assertThatThrownBy(() -> runtimeService.getVariable(processInstanceId, VARIABLE_1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void getExecutionVariableWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(engineRule.getRuntimeService().getVariable(processInstanceId, VARIABLE_1)).isEqualTo(VARIABLE_VALUE_1);

  }

  // get typed execution variable
  @Test
  void getExecutionVariableTypedWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(engineRule.getRuntimeService().getVariableTyped(processInstanceId, VARIABLE_1).getValue()).isEqualTo(VARIABLE_VALUE_1);
  }

  @Test
  void getExecutionVariableTypedWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    var runtimeService = engineRule.getRuntimeService();

    // when/then
    assertThatThrownBy(() -> runtimeService.getVariableTyped(processInstanceId, VARIABLE_1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void getExecutionVariableTypedWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);

    // if
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(engineRule.getRuntimeService().getVariableTyped(processInstanceId, VARIABLE_1).getValue()).isEqualTo(VARIABLE_VALUE_1);

  }

  // get execution variables
  @Test
  void getExecutionVariablesWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(engineRule.getRuntimeService().getVariables(processInstanceId)).hasSize(2);
  }

  @Test
  void getExecutionVariablesWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    var runtimeService = engineRule.getRuntimeService();

    // when/then
    assertThatThrownBy(() -> runtimeService.getVariables(processInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void getExecutionVariablesWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(engineRule.getRuntimeService().getVariables(processInstanceId)).hasSize(2);

  }

  // set execution variable
  @Test
  void setExecutionVariableWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    engineRule.getRuntimeService().setVariable(processInstanceId, "newVariable", "newValue");
    assertThat(engineRule.getRuntimeService().getVariables(processInstanceId)).hasSize(3);
  }

  @Test
  void setExecutionVariableWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    var runtimeService = engineRule.getRuntimeService();

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariable(processInstanceId, "newVariable", "newValue"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void setExecutionVariableWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    engineRule.getRuntimeService().setVariable(processInstanceId, "newVariable", "newValue");
    assertThat(engineRule.getRuntimeService().getVariables(processInstanceId)).hasSize(3);
  }

  // remove execution variable
  @Test
  void removeExecutionVariableWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));
    engineRule.getRuntimeService().removeVariable(processInstanceId, VARIABLE_1);

    // then
    assertThat(engineRule.getRuntimeService().getVariables(processInstanceId)).hasSize(1);
  }

  @Test
  void removeExecutionVariableWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    var runtimeService = engineRule.getRuntimeService();

    // when/then
    assertThatThrownBy(() -> runtimeService.removeVariable(processInstanceId, VARIABLE_1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void removeExecutionVariableWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    engineRule.getRuntimeService().removeVariable(processInstanceId, VARIABLE_1);

    // then
    assertThat(engineRule.getRuntimeService().getVariables(processInstanceId)).hasSize(1);
  }
}
