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
package org.operaton.bpm.engine.test.api.form;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.util.OperatonFormUtils.findAllOperatonFormDefinitionEntities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.form.OperatonFormRef;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.repository.OperatonFormDefinition;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.util.OperatonFormUtils;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

public class RetrieveOperatonFormRefTest {

  protected static final String TASK_FORM_CONTENT_V1 = "{\"id\"=\"myTaskForm\",\"type\": \"default\",\"components\": []}";
  protected static final String TASK_FORM_CONTENT_V2 = "{\"id\"=\"myTaskForm\",\"type\": \"default\",\"components\":[{\"key\": \"textfield1\",\"label\": \"Text Field\",\"type\": \"textfield\"}]}";
  protected static final String START_FORM_CONTENT_V1 = "{\"id\"=\"myStartForm\",\"type\": \"default\",\"components\": []}";
  protected static final String START_FORM_CONTENT_V2 = "{\"id\"=\"myStartForm\",\"type\": \"default\",\"components\":[{\"key\": \"textfield1\",\"label\": \"Text Field\",\"type\": \"textfield\"}]}";

  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);
  protected TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule).around(tempFolder);

  private RuntimeService runtimeService;
  private TaskService taskService;
  private RepositoryService repositoryService;
  private FormService formService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    repositoryService = engineRule.getRepositoryService();
    formService = engineRule.getFormService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @After
  public void tearDown() {
    List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery().list();
    for (org.operaton.bpm.engine.repository.Deployment deployment : deployments) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  /* TASK FORMS */

  @Test
  public void shouldRetrieveTaskFormBindingLatestWithSingleVersionSeparateDeloyments() throws IOException {
    // given two separate deployments
    deployClasspathResources(true,
        "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.taskFormBindingLatest.bpmn",
        "org/operaton/bpm/engine/test/api/form/task.form");

    runtimeService.startProcessInstanceByKey("taskFormBindingLatest");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());
    InputStream deployedForm = formService.getDeployedTaskForm(task.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(1);

    assertTaskFormData(taskFormData, "myTaskForm", "latest", null);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(getClasspathResourceContent("org/operaton/bpm/engine/test/api/form/task.form"));
  }

  @Test
  public void shouldRetrieveTaskFormBindingLatestWithMultipleVersions() throws IOException {
    // given two versions of the same form
    deployUpdateFormResource(TASK_FORM_CONTENT_V1, TASK_FORM_CONTENT_V2, "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.taskFormBindingLatest.bpmn");

    runtimeService.startProcessInstanceByKey("taskFormBindingLatest");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());
    InputStream deployedForm = formService.getDeployedTaskForm(task.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(2);

    assertTaskFormData(taskFormData, "myTaskForm", "latest", null);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(TASK_FORM_CONTENT_V2);
  }

  @Test
  public void shouldRetrieveTaskFormBindingDeployment() throws IOException {
    // given two versions of the same form
    deployUpdateFormResource(TASK_FORM_CONTENT_V1, TASK_FORM_CONTENT_V2, "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.taskFormBindingDeployment.bpmn");

    runtimeService.startProcessInstanceByKey("taskFormBindingDeployment");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());
    InputStream deployedForm = formService.getDeployedTaskForm(task.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(2);

    assertTaskFormData(taskFormData, "myTaskForm", "deployment", null);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(TASK_FORM_CONTENT_V1);
  }

  @Test
  public void shouldRetrieveTaskFormBindingVersionWithMultipleVersions() throws IOException {
    // given two versions of the same form
    deployUpdateFormResource(TASK_FORM_CONTENT_V1, TASK_FORM_CONTENT_V2, "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.taskFormBindingVersion1.bpmn");

    runtimeService.startProcessInstanceByKey("taskFormBindingVersion");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());
    InputStream deployedForm = formService.getDeployedTaskForm(task.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(2);

    assertTaskFormData(taskFormData, "myTaskForm", "version", 1);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(TASK_FORM_CONTENT_V1);
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.taskFormBindingLatest.bpmn"})
  public void shouldFailToRetrieveTaskFormBindingLatestUnexistingKey() {
    // given BPMN model references missing form
    runtimeService.startProcessInstanceByKey("taskFormBindingLatest");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).isEmpty();

    assertTaskFormData(taskFormData, "myTaskForm", "latest", null);

    assertThatThrownBy(() -> formService.getDeployedTaskForm(taskId))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No Operaton Form Definition was found for Operaton Form Ref");
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.taskFormBindingDeployment.bpmn"})
  public void shouldFailToRetrieveTaskFormBindingDeploymentUnexistingKey() {
    // given BPMN model references missing form
    runtimeService.startProcessInstanceByKey("taskFormBindingDeployment");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).isEmpty();

    assertTaskFormData(taskFormData, "myTaskForm", "deployment", null);

    assertThatThrownBy(() -> formService.getDeployedTaskForm(taskId))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No Operaton Form Definition was found for Operaton Form Ref");
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.taskFormBindingVersion2.bpmn",
      "org/operaton/bpm/engine/test/api/form/task.form" })
  public void shouldFailToRetrieveTaskFormBindingVersionUnexistingVersion() {
    // given BPMN model references missing form
    runtimeService.startProcessInstanceByKey("taskFormBindingVersion");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).hasSize(1);

    assertTaskFormData(taskFormData, "myTaskForm", "version", 2);

    assertThatThrownBy(() -> formService.getDeployedTaskForm(taskId))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No Operaton Form Definition was found for Operaton Form Ref");
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.shouldRetrieveTaskFormBindingLatestWithKeyExpression.bpmn",
      "org/operaton/bpm/engine/test/api/form/task.form" })
  public void shouldRetrieveTaskFormBindingLatestWithKeyExpression() throws IOException {
    // given BPMN model referencing form by ${key} expression
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("key", "myTaskForm");
    runtimeService.startProcessInstanceByKey("taskFormBindingLatest", parameters);

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());
    InputStream deployedForm = formService.getDeployedTaskForm(task.getId());

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).hasSize(1);

    assertTaskFormData(taskFormData, "myTaskForm", "latest", null);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(getClasspathResourceContent("org/operaton/bpm/engine/test/api/form/task.form"));
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.shouldRetrieveTaskFormBindingVersionWithExpression.bpmn",
      "org/operaton/bpm/engine/test/api/form/task.form" })
  public void shouldRetrieveTaskFormBindingVersionWithExpression() throws IOException {
    // given BPMN model referencing version by ${ver} expression
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("ver", "1");
    runtimeService.startProcessInstanceByKey("taskFormBindingVersion", parameters);

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    Task task = taskService.createTaskQuery().singleResult();
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());
    InputStream deployedForm = formService.getDeployedTaskForm(task.getId());

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).hasSize(1);

    assertTaskFormData(taskFormData, "myTaskForm", "version", 1);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(getClasspathResourceContent("org/operaton/bpm/engine/test/api/form/task.form"));
  }

  /* START FORMS */

  @Test
  public void shouldRetrieveStartFormBindingLatestWithSingleVersionSeparateDeloyments() throws IOException {
    // given two separate deployments
    deployClasspathResources(true,
        "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.startFormBindingLatest.bpmn",
        "org/operaton/bpm/engine/test/api/form/start.form");

    runtimeService.startProcessInstanceByKey("startFormBindingLatest");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    InputStream deployedForm = formService.getDeployedStartForm(processDefinition.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(1);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(getClasspathResourceContent("org/operaton/bpm/engine/test/api/form/start.form"));
  }

  @Test
  public void shouldRetrieveStartFormBindingLatestWithMultipleVersions() throws IOException {
    // given two versions of the same form
    deployUpdateFormResource(START_FORM_CONTENT_V1, START_FORM_CONTENT_V2, "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.startFormBindingLatest.bpmn");

    runtimeService.startProcessInstanceByKey("startFormBindingLatest");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    InputStream deployedForm = formService.getDeployedStartForm(processDefinition.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(2);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(START_FORM_CONTENT_V2);
  }

  @Test
  public void shouldRetrieveStartFormBindingDeployment() throws IOException {
    // given two versions of the same form
    deployUpdateFormResource(START_FORM_CONTENT_V1, START_FORM_CONTENT_V2, "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.startFormBindingDeployment.bpmn");

    runtimeService.startProcessInstanceByKey("startFormBindingDeployment");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    InputStream deployedForm = formService.getDeployedStartForm(processDefinition.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(2);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(START_FORM_CONTENT_V1);
  }

  @Test
  public void shouldRetrieveStartFormBindingVersionWithMultipleVersions() throws IOException {
    // given two versions of the same form
    deployUpdateFormResource(START_FORM_CONTENT_V1, START_FORM_CONTENT_V2, "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.startFormBindingVersion1.bpmn");

    runtimeService.startProcessInstanceByKey("startFormBindingVersion");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    InputStream deployedForm = formService.getDeployedStartForm(processDefinition.getId());

    // then
    assertThat(deployments).hasSize(2);
    assertThat(definitions).hasSize(2);

    assertThat(IOUtils.toString(deployedForm, UTF_8)).isEqualTo(START_FORM_CONTENT_V1);
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.startFormBindingLatest.bpmn"})
  public void shouldFailToRetrieveStartFormBindingLatestUnexistingKey() {
    // given BPMN model references missing form
    runtimeService.startProcessInstanceByKey("startFormBindingLatest");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    String processDefinitionId = processDefinition.getId();

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).isEmpty();

    assertThatThrownBy(() -> formService.getDeployedStartForm(processDefinitionId))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("No Operaton Form Definition was found for Operaton Form Ref");
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.startFormBindingDeployment.bpmn"})
  public void shouldFailToRetrieveStartFormBindingDeploymentUnexistingKey() {
    // given BPMN model references missing form
    runtimeService.startProcessInstanceByKey("startFormBindingDeployment");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    String processDefinitionId = processDefinition.getId();

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).isEmpty();

    assertThatThrownBy(() -> formService.getDeployedStartForm(processDefinitionId))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("No Operaton Form Definition was found for Operaton Form Ref");
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/RetrieveOperatonFormRefTest.startFormBindingVersion2.bpmn",
      "org/operaton/bpm/engine/test/api/form/start.form" })
  public void shouldFailToRetrieveStartFormBindingVersionUnexistingVersion() {
    // given BPMN model references missing form
    runtimeService.startProcessInstanceByKey("startFormBindingVersion");

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    String processDefinitionId = processDefinition.getId();

    // then
    assertThat(deployments).hasSize(1);
    assertThat(definitions).hasSize(1);

    assertThatThrownBy(() -> formService.getDeployedStartForm(processDefinitionId))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("No Operaton Form Definition was found for Operaton Form Ref");
  }

  /* HELPER METHODS */

  private void assertTaskFormData(TaskFormData taskFormData, String expectedKey, String expectedBinding, Integer expectedVersion) {
    OperatonFormRef operatonFormRef = taskFormData.getOperatonFormRef();
    assertThat(operatonFormRef.getKey()).isEqualTo(expectedKey);
    assertThat(operatonFormRef.getBinding()).isEqualTo(expectedBinding);
    assertThat(operatonFormRef.getVersion()).isEqualTo(expectedVersion);
    assertThat(taskFormData.getFormKey()).isNull();
  }

  private String getClasspathResourceContent(String path) throws IOException {
    InputStream inputStream = ReflectUtil.getResourceAsStream(path);
    return IOUtils.toString(inputStream, UTF_8);
  }

  private void deployClasspathResources(boolean separateDeployments, String... paths) {
    if (separateDeployments) {
      for (String path : paths) {
        testRule.deploy(path);
      }
    } else {
      testRule.deploy(paths);
    }
  }

  private void deployUpdateFormResource(String v1Content, String v2Content, String... additionalResourcesForFirstDeployment) throws IOException {
    FileInputStream form;
    // deploy BPMN with first version of form
    form = OperatonFormUtils.writeTempFormFile("form.form", v1Content, tempFolder);
    DeploymentBuilder builder = repositoryService.createDeployment().name(getClass().getSimpleName())
        .addInputStream("form", form);
    for (String path : additionalResourcesForFirstDeployment) {
      builder.addClasspathResource(path);
    }
    builder.deploy();

    // deploy second version of form
    form = OperatonFormUtils.writeTempFormFile("form.form", v2Content, tempFolder);
    repositoryService.createDeployment().name(getClass().getSimpleName()).addInputStream("form", form).deploy();
  }
}
