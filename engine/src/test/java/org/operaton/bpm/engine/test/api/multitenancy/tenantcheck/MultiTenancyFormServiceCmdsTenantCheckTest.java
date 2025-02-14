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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class MultiTenancyFormServiceCmdsTenantCheckTest {
 protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "formKeyProcess";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();

  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  protected TaskService taskService;

  protected FormService formService;

  protected RuntimeService runtimeService;

  protected IdentityService identityService;

  protected RepositoryService repositoryService;

  protected ProcessEngineConfiguration processEngineConfiguration;

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  @Before
  public void init() {

    taskService = engineRule.getTaskService();

    formService = engineRule.getFormService();

    identityService = engineRule.getIdentityService();

    runtimeService = engineRule.getRuntimeService();

    repositoryService = engineRule.getRepositoryService();

    processEngineConfiguration = engineRule.getProcessEngineConfiguration();

  }

  // GetStartForm test
  @Test
  public void testGetStartFormWithAuthenticatedTenant() {

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
  public void testGetStartFormWithNoAuthenticatedTenant() {

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
  public void testGetStartFormWithDisabledTenantCheck() {

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
  public void testGetRenderedStartFormWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/request.html");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    assertThat(formService.getRenderedStartForm(processDefinitionId, "juel")).isNotNull();
  }

  @Test
  public void testGetRenderedStartFormWithNoAuthenticatedTenant() {

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
  public void testGetRenderedStartFormWithDisabledTenantCheck() {

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
  public void testSubmitStartFormWithAuthenticatedTenant() {

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
  public void testSubmitStartFormWithNoAuthenticatedTenant() {

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
  public void testSubmitStartFormWithDisabledTenantcheck() {

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
  public void testGetStartFormKeyWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getProcessDefinitionId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    assertThat(formService.getStartFormKey(processDefinitionId)).isEqualTo("aStartFormKey");

  }

  @Test
  public void testGetStartFormKeyWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getProcessDefinitionId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getStartFormKey(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition '" + processDefinitionId + "' because it belongs to no authenticated tenant.");

  }

  @Test
  public void testGetStartFormKeyWithDisabledTenantCheck() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getProcessDefinitionId();

    identityService.setAuthentication("aUserId", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // then
    assertThat(formService.getStartFormKey(processDefinitionId)).isEqualTo("aStartFormKey");

  }

  // GetTaskForm test
  @Test
  public void testGetTaskFormWithAuthenticatedTenant() {

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
  public void testGetTaskFormWithNoAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> formService.getTaskFormData(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '" + taskId + "' because it belongs to no authenticated tenant.");

  }

  @Test
  public void testGetTaskFormWithDisabledTenantCheck() {

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
  public void testSubmitTaskFormWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
    "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    runtimeService.startProcessInstanceById(processDefinitionId);

    assertThat(taskService.createTaskQuery().processDefinitionId(processDefinitionId).count()).isEqualTo(1);

    String taskId = taskService.createTaskQuery().processDefinitionId(processDefinitionId).singleResult().getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    formService.submitTaskForm(taskId, null);

    // task gets completed on execution of submitTaskForm
    assertThat(taskService.createTaskQuery().processDefinitionId(processDefinitionId).count()).isEqualTo(0);
  }

  @Test
  public void testSubmitTaskFormWithNoAuthenticatedTenant() {

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
  public void testSubmitTaskFormWithDisabledTenantCheck() {

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
    assertThat(taskService.createTaskQuery().processDefinitionId(processDefinitionId).count()).isEqualTo(0);
  }

  // getRenderedTaskForm
  @Test
  public void testGetRenderedTaskFormWithAuthenticatedTenant() {

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
  public void testGetRenderedTaskFormWithNoAuthenticatedTenant() {

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
  public void testGetRenderedTaskFormWithDisabledTenantCheck() {

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
  public void testGetTaskFormKeyWithAuthenticatedTenant() {

    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/authorization/formKeyProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    Task task = taskService.createTaskQuery().singleResult();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    assertThat(formService.getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey())).isEqualTo("aTaskFormKey");

  }

  @Test
  public void testGetTaskFormKeyWithNoAuthenticatedTenant() {

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
  public void testGetTaskFormKeyWithDisabledTenantCheck() {

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
