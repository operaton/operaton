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
package org.operaton.bpm.engine.test.bpmn.event.start;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.form.FormDefinition;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings({"java:S4144", "java:S5976"})
class StartEventOperatonFormDefinitionParseTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @AfterEach
  void tearDown() {
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  protected FormDefinition getStartFormDefinition() {
    return getProcessDefinition().getStartFormDefinition();
  }

  private ProcessDefinitionEntity getProcessDefinition() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    return processEngineConfiguration.getDeploymentCache()
        .getProcessDefinitionCache().get(processDefinition.getId());
  }

  @Test
  @Deployment
  void shouldParseOperatonFormDefinitionVersionBinding() {
    // given a deployed process with a StartEvent containing an Operaton Form definition with version binding
    // then
    FormDefinition startFormDefinition = getStartFormDefinition();

    assertThat(startFormDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");
    assertThat(startFormDefinition.getOperatonFormDefinitionBinding()).isEqualTo("version");
    assertThat(startFormDefinition.getOperatonFormDefinitionVersion().getExpressionText()).isEqualTo("1");
  }

  @Test
  @Deployment
  void shouldParseOperatonFormDefinitionLatestBinding() {
    // given a deployed process with a StartEvent containing an Operaton Form definition with latest binding
    // then
    FormDefinition startFormDefinition = getStartFormDefinition();

    assertThat(startFormDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");
    assertThat(startFormDefinition.getOperatonFormDefinitionBinding()).isEqualTo("latest");
  }

  @Test
  @Deployment
  void shouldParseOperatonFormDefinitionMultipleStartEvents() {
    // given a deployed process with a StartEvent containing an Operaton Form definition with latest binding and another StartEvent inside a subprocess
    // then
    FormDefinition startFormDefinition = getStartFormDefinition();

    assertThat(startFormDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");
    assertThat(startFormDefinition.getOperatonFormDefinitionBinding()).isEqualTo("latest");
  }

  @Test
  @Deployment
  void shouldParseOperatonFormDefinitionDeploymentBinding() {
    // given a deployed process with a StartEvent containing an Operaton Form definition with deployment binding
    // then
    FormDefinition startFormDefinition = getStartFormDefinition();

    assertThat(startFormDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");
    assertThat(startFormDefinition.getOperatonFormDefinitionBinding()).isEqualTo("deployment");
  }

  @Test
  void shouldNotParseOperatonFormDefinitionUnsupportedBinding() {
    // given a deployed process with a UserTask containing an Operaton Form definition with unsupported binding
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "shouldNotParseOperatonFormDefinitionUnsupportedBinding");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    // when/then expect parse exception
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Invalid element definition: value for formRefBinding attribute has to be one of [deployment, latest, version] but was unsupported");
  }

  @Test
  void shouldNotParseOperatonFormDefinitionAndFormKey() {
    // given a deployed process with a UserTask containing an Operaton Form definition and formKey
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "shouldNotParseOperatonFormDefinitionAndFormKey");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    // when/then expect parse exception
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Invalid element definition: only one of the attributes formKey and formRef is allowed.");
  }
}
