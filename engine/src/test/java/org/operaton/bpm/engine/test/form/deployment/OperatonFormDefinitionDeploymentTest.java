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
package org.operaton.bpm.engine.test.form.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.util.OperatonFormUtils.findAllOperatonFormDefinitionEntities;
import static org.operaton.bpm.engine.test.util.OperatonFormUtils.writeTempFormFile;

import java.io.IOException;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.OperatonFormDefinition;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

public class OperatonFormDefinitionDeploymentTest {

  protected static final String SIMPLE_FORM = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeploymentTest.simple_form.form";
  protected static final String SIMPLE_FORM_DUPLICATE = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeploymentTest.simple_form_duplicate.form";
  protected static final String COMPLEX_FORM = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeploymentTest.complex_form.form";
  protected static final String SIMPLE_BPMN = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeploymentTest.simpleBPMN.bpmn";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);
  protected TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule).around(tempFolder);

  RepositoryService repositoryService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @Before
  public void init() {
    repositoryService = engineRule.getRepositoryService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @After
  public void tearDown() {
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    for (Deployment deployment : deployments) {
      repositoryService.deleteDeployment(deployment.getId());
    }
  }

  @Test
  public void shouldDeployTheSameFormTwiceWithoutDuplicateFiltering() {
    // when
    createDeploymentBuilder(false).addClasspathResource(SIMPLE_FORM).deploy();
    createDeploymentBuilder(false).addClasspathResource(SIMPLE_FORM).deploy();

    // then
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(2);

    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(definitions).hasSize(2);
    assertThat(definitions).extracting("version").containsExactlyInAnyOrder(1, 2);
    assertThat(definitions).extracting("deploymentId").containsExactlyInAnyOrder(deployments.stream().map(Deployment::getId).toArray());
    assertThat(definitions).extracting("resourceName").containsExactly(SIMPLE_FORM, SIMPLE_FORM);
  }

  @Test
  public void shouldNotDeployTheSameFormTwiceWithDuplicateFiltering() {
    // when
    createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM).deploy();
    createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM).deploy();

    // then
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(1);

    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(definitions).hasSize(1);
    OperatonFormDefinition definition = definitions.get(0);
    assertThat(definition.getVersion()).isEqualTo(1);
    assertThat(definition.getDeploymentId()).isEqualTo(deployments.get(0).getId());
    assertThat(definition.getResourceName()).isEqualTo(SIMPLE_FORM);
  }

  @Test
  public void shouldNotDeployTheSameFormTwiceWithDuplicateFilteringAndAdditionalResources() {
    // when
    Deployment firstDeployment = createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM).deploy();
    createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM)
        .addClasspathResource(SIMPLE_BPMN).deploy();

    // then
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(2);

    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(definitions).hasSize(1);
    OperatonFormDefinition definition = definitions.get(0);
    assertThat(definition.getVersion()).isEqualTo(1);
    assertThat(definition.getDeploymentId()).isEqualTo(firstDeployment.getId());
    assertThat(definition.getResourceName()).isEqualTo(SIMPLE_FORM);
  }

  @Test
  public void shouldDeployDifferentFormsFromDifferentDeployments() {
    // when
    createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM).deploy();
    createDeploymentBuilder(true).addClasspathResource(COMPLEX_FORM).deploy();

    // then
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(2);

    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(definitions).hasSize(2);
    assertThat(definitions).extracting("version").containsExactly(1, 1);
    assertThat(definitions).extracting("deploymentId").containsExactlyInAnyOrder(deployments.stream().map(Deployment::getId).toArray());
    assertThat(definitions).extracting("resourceName").containsExactlyInAnyOrder(SIMPLE_FORM, COMPLEX_FORM);
  }

  @Test
  public void shouldDeployDifferentFormsFromOneDeployment() {
    // when
    createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM).addClasspathResource(COMPLEX_FORM).deploy();

    // then
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(1);
    String deploymentId = deployments.get(0).getId();

    List<OperatonFormDefinition> definitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(definitions).hasSize(2);
    assertThat(definitions).extracting("version").containsExactly(1, 1);
    assertThat(definitions).extracting("deploymentId").containsExactly(deploymentId, deploymentId);
    assertThat(definitions).extracting("resourceName").containsExactlyInAnyOrder(SIMPLE_FORM, COMPLEX_FORM);
  }

  @Test
  public void shouldFailDeploymentWithMultipleFormsDuplicateId() {
    // when
    assertThatThrownBy(() -> {
      createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM).addClasspathResource(SIMPLE_FORM_DUPLICATE).deploy();
    }).isInstanceOf(ProcessEngineException.class)
    .hasMessageContaining("The deployment contains definitions with the same key 'simpleForm' (id attribute), this is not allowed");
  }

  @Test
  public void shouldDeleteFormDefinitionWhenDeletingDeployment() {
    // given
    Deployment deployment = createDeploymentBuilder(true).addClasspathResource(SIMPLE_FORM).addClasspathResource(COMPLEX_FORM).deploy();
    List<OperatonFormDefinition> formDefinitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();

    // when
    repositoryService.deleteDeployment(deployment.getId());

    // then
    // before deletion of deployment
    assertThat(formDefinitions).hasSize(2);
    assertThat(deployments).hasSize(1);

    // after deletion of deployment
    assertThat(findAllOperatonFormDefinitionEntities(processEngineConfiguration)).isEmpty();
    assertThat(repositoryService.createDeploymentQuery().list()).isEmpty();
  }

  @Test
  public void shouldUpdateVersionForChangedFormResource() throws IOException {
    // given
    String fileName = "myForm.form";
    String formContent1 = "{\"id\"=\"myForm\",\"type\": \"default\",\"components\":[{\"key\": \"button3\",\"label\": \"Button\",\"type\": \"button\"}]}";
    String formContent2 = "{\"id\"=\"myForm\",\"type\": \"default\",\"components\": []}";

    createDeploymentBuilder(true).addInputStream(fileName, writeTempFormFile(fileName, formContent1, tempFolder)).deploy();

    // when deploy changed file
    createDeploymentBuilder(true).addInputStream(fileName, writeTempFormFile(fileName, formContent2, tempFolder)).deploy();

    // then
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(2);
    assertThat(deployments).extracting("tenantId").containsExactly(null, null);
    List<OperatonFormDefinition> formDefinitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(formDefinitions).extracting("version").containsExactlyInAnyOrder(1, 2);
    assertThat(formDefinitions).extracting("resourceName").containsExactly(fileName, fileName);
    assertThat(formDefinitions).extracting("deploymentId").containsExactlyInAnyOrder(deployments.stream().map(Deployment::getId).toArray());

  }

  @Test
  public void shouldUpdateVersionForChangedFormResourceWithTenant() throws IOException {
    // given
    String fileName = "myForm.form";
    String formContent1 = "{\"id\"=\"myForm\",\"type\": \"default\",\"components\":[{\"key\": \"button3\",\"label\": \"Button\",\"type\": \"button\"}]}";
    String formContent2 = "{\"id\"=\"myForm\",\"type\": \"default\",\"components\": []}";

    createDeploymentBuilder(true).tenantId("tenant1").addInputStream(fileName, writeTempFormFile(fileName, formContent1, tempFolder)).deploy();

    // when deploy changed file
    createDeploymentBuilder(true).tenantId("tenant1").addInputStream(fileName, writeTempFormFile(fileName, formContent2, tempFolder)).deploy();

    // then
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(2);
    assertThat(deployments).extracting("tenantId").containsExactly("tenant1", "tenant1");
    List<OperatonFormDefinition> formDefinitions = findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(formDefinitions).extracting("version").containsExactlyInAnyOrder(1, 2);
    assertThat(formDefinitions).extracting("resourceName").containsExactly(fileName, fileName);
    assertThat(formDefinitions).extracting("deploymentId").containsExactlyInAnyOrder(deployments.stream().map(Deployment::getId).toArray());

  }

  private DeploymentBuilder createDeploymentBuilder(boolean filterDuplicates) {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name(getClass().getSimpleName());
    if (filterDuplicates) {
      deploymentBuilder.enableDuplicateFiltering(filterDuplicates);
    }
    return deploymentBuilder;
  }
}
