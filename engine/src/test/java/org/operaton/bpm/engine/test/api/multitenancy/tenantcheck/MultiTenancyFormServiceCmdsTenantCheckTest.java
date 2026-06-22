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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyFormServiceCmdsTenantCheckTest {
 protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "formKeyProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected TaskService taskService;
  protected FormService formService;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;
  protected RepositoryService repositoryService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  // GetStartForm test
  @Test
  void testGetStartFormWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    StartFormData startFormData = formService.getStartFormData(instance.getProcessDefinitionId());

    // then
    assertThat(startFormData).isNotNull();
    assertThat(startFormData.getFormKey()).isEqualTo("aStartFormKey");
  }

  @Test
  void testGetStartFormWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
    "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    String processDefinitionId = instance.getProcessDefinitionId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getStartFormData(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition '" + processDefinitionId
      +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testGetStartFormWithDisabledTenantCheck() {

    testRule.deployForTenant(TENANT_ONE,
    "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    StartFormData startFormData = formService.getStartFormData(instance.getProcessDefinitionId());

    // then
    assertThat(startFormData).isNotNull();
    assertThat(startFormData.getFormKey()).isEqualTo("aStartFormKey");

  }

  // GetRenderedStartForm
  @Test
  void testGetRenderedStartFormWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/request.html");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    assertThat(formService.getRenderedStartForm(processDefinitionId, "juel")).isNotNull();
  }

  @Test
  void testGetRenderedStartFormWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/request.html");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getRenderedStartForm(processDefinitionId, "juel"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition '" + processDefinitionId
      +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testGetRenderedStartFormWithDisabledTenantCheck() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/request.html");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    assertThat(formService.getRenderedStartForm(processDefinitionId, "juel")).isNotNull();
  }

  // submitStartForm
  @Test
  void testSubmitStartFormWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/request.html");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("employeeName", "demo");

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    assertThat(formService.submitStartForm(processDefinitionId, properties)).isNotNull();
  }

  @Test
  void testSubmitStartFormWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/request.html");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("employeeName", "demo");

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.submitStartForm(processDefinitionId, properties))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create an instance of the process definition '" + processDefinitionId
      +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testSubmitStartFormWithDisabledTenantcheck() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/request.html");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("employeeName", "demo");

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // when
    assertThat(formService.submitStartForm(processDefinitionId, properties)).isNotNull();

  }

  // getStartFormKey
  @Test
  void testGetStartFormKeyWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getProcessDefinitionId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    assertThat(formService.getStartFormKey(processDefinitionId)).isEqualTo("aStartFormKey");

  }

  @Test
  void testGetStartFormKeyWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getProcessDefinitionId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getStartFormKey(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition '%s' because it belongs to no authenticated tenant.".formatted(processDefinitionId));

  }

  @Test
  void testGetStartFormKeyWithDisabledTenantCheck() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getProcessDefinitionId();

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // then
    assertThat(formService.getStartFormKey(processDefinitionId)).isEqualTo("aStartFormKey");

  }

  // GetTaskForm test
  @Test
  void testGetTaskFormWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    String taskId = taskService.createTaskQuery().singleResult().getId();

    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
    assertThat(taskFormData.getFormKey()).isEqualTo("aTaskFormKey");
  }

  @Test
  void testGetTaskFormWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getTaskFormData(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '%s' because it belongs to no authenticated tenant.".formatted(taskId));

  }

  @Test
  void testGetTaskFormWithDisabledTenantCheck() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();
    assertThat(taskFormData.getFormKey()).isEqualTo("aTaskFormKey");

  }

  // submitTaskForm
  @Test
  void testSubmitTaskFormWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
    "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    runtimeService.startProcessInstanceById(processDefinitionId);

    assertThat(taskService.createTaskQuery().processDefinitionId(processDefinitionId).count()).isOne();

    String taskId = taskService.createTaskQuery().processDefinitionId(processDefinitionId).singleResult().getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    formService.submitTaskForm(taskId, null);

    // task gets completed on execution of submitTaskForm
    assertThat(taskService.createTaskQuery().processDefinitionId(processDefinitionId).count()).isZero();
  }

  @Test
  void testSubmitTaskFormWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    runtimeService.startProcessInstanceById(processDefinitionId);

    String taskId = taskService.createTaskQuery().processDefinitionId(processDefinitionId).singleResult().getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.submitTaskForm(taskId, null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot work on task '" + taskId
          +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testSubmitTaskFormWithDisabledTenantCheck() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    runtimeService.startProcessInstanceById(processDefinitionId);

    String taskId = taskService.createTaskQuery().processDefinitionId(processDefinitionId).singleResult().getId();

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    formService.submitTaskForm(taskId, null);

    // task gets completed on execution of submitTaskForm
    assertThat(taskService.createTaskQuery().processDefinitionId(processDefinitionId).count()).isZero();
  }

  // getRenderedTaskForm
  @Test
  void testGetRenderedTaskFormWithAuthenticatedTenant() {

    // deploy tenants
    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/task.html").getId();

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("room", "5b");
    properties.put("speaker", "Mike");
    formService.submitStartForm(procDefId, properties).getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(formService.getRenderedTaskForm(taskId, "juel")).isEqualTo("Mike is speaking in room 5b");
  }

  @Test
  void testGetRenderedTaskFormWithNoAuthenticatedTenant() {

    // deploy tenants
    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/task.html").getId();

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("room", "5b");
    properties.put("speaker", "Mike");
    formService.submitStartForm(procDefId, properties);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getRenderedTaskForm(taskId, "juel"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '" + taskId
          +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testGetRenderedTaskFormWithDisabledTenantCheck() {

    // deploy tenants
    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/task.html").getId();

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("room", "5b");
    properties.put("speaker", "Mike");
    formService.submitStartForm(procDefId, properties);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // then
    assertThat(formService.getRenderedTaskForm(taskId, "juel")).isEqualTo("Mike is speaking in room 5b");
  }

  // getTaskFormKey
  @Test
  void testGetTaskFormKeyWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    Task task = taskService.createTaskQuery().singleResult();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    assertThat(formService.getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey())).isEqualTo("aTaskFormKey");

  }

  @Test
  void testGetTaskFormKeyWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    Task task = taskService.createTaskQuery().singleResult();
    String processDefinitionId = task.getProcessDefinitionId();
    String taskDefinitionKey = task.getTaskDefinitionKey();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getTaskFormKey(processDefinitionId, taskDefinitionKey))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition '" + processDefinitionId
      +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testGetTaskFormKeyWithDisabledTenantCheck() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    Task task = taskService.createTaskQuery().singleResult();

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    formService.getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
    // then
    assertThat(formService.getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey())).isEqualTo("aTaskFormKey");
  }
}
