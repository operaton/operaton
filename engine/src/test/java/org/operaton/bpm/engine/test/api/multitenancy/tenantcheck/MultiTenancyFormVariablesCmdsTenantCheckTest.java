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
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

class MultiTenancyFormVariablesCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "testProcess";

  protected static final String VARIABLE_1 = "testVariable1";
  protected static final String VARIABLE_2 = "testVariable2";

  protected static final String VARIABLE_VALUE_1 = "test1";
  protected static final String VARIABLE_VALUE_2 = "test2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessInstance instance;

  protected static final String START_FORM_RESOURCE = "org/operaton/bpm/engine/test/api/form/FormServiceTest.startFormFields.bpmn20.xml";

  @BeforeEach
  void init() {

    // deploy tenants
    testRule.deployForTenant(TENANT_ONE, START_FORM_RESOURCE);
    instance = engineRule.getRuntimeService()
    .startProcessInstanceByKey(PROCESS_DEFINITION_KEY, Variables.createVariables().putValue(VARIABLE_1, VARIABLE_VALUE_1)
         .putValue(VARIABLE_2, VARIABLE_VALUE_2));
  }

  // start form variables
  @Test
  void testGetStartFormVariablesWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    assertThat(engineRule.getFormService().getStartFormVariables(instance.getProcessDefinitionId())).hasSize(4);

  }

  @Test
  void testGetStartFormVariablesWithNoAuthenticatedTenant() {
    engineRule.getIdentityService().setAuthentication("aUserId", null);
    String processDefinitionId = instance.getProcessDefinitionId();
    var formService = engineRule.getFormService();

    // when/then
    assertThatThrownBy(() -> formService.getStartFormVariables(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition '"
          + processDefinitionId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testGetStartFormVariablesWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    assertThat(engineRule.getFormService().getStartFormVariables(instance.getProcessDefinitionId())).hasSize(4);

  }

  @Test
  void testGetTaskFormVariablesWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();

    assertThat(engineRule.getFormService().getTaskFormVariables(task.getId())).hasSize(2);

  }

  @Test
  void testGetTaskFormVariablesWithNoAuthenticatedTenant() {

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    String taskId = task.getId();

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    var formService = engineRule.getFormService();

    // when/then
    assertThatThrownBy(() -> formService.getTaskFormVariables(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testGetTaskFormVariablesWithDisabledTenantCheck() {

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    assertThat(engineRule.getFormService().getTaskFormVariables(task.getId())).hasSize(2);

  }
}
