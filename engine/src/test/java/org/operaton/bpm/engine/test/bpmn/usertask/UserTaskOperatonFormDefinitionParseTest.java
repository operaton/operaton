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
package org.operaton.bpm.engine.test.bpmn.usertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.form.FormDefinition;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class UserTaskOperatonFormDefinitionParseTest {

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain chain = RuleChain.outerRule(engineRule).around(testRule);

  public RepositoryService repositoryService;
  public ProcessEngineConfigurationImpl processEngineConfiguration;

  @Before
  public void setup() {
    repositoryService = engineRule.getRepositoryService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @After
  public void tearDown() {
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  protected ActivityImpl findActivityInDeployedProcessDefinition(String activityId) {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition).isNotNull();

    ProcessDefinitionEntity cachedProcessDefinition = processEngineConfiguration.getDeploymentCache()
        .getProcessDefinitionCache().get(processDefinition.getId());
    return cachedProcessDefinition.findActivity(activityId);
  }

  @Test
  @Deployment
  public void shouldParseOperatonFormDefinitionVersionBinding() {
    // given a deployed process with a UserTask containing an Operaton Form definition with version binding
    // then
    TaskDefinition taskDefinition = findUserTaskDefinition("UserTask");
    FormDefinition formDefinition = taskDefinition.getFormDefinition();

    assertThat(taskDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");
    assertThat(formDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");

    assertThat(taskDefinition.getOperatonFormDefinitionBinding()).isEqualTo("version");
    assertThat(formDefinition.getOperatonFormDefinitionBinding()).isEqualTo("version");

    assertThat(taskDefinition.getOperatonFormDefinitionVersion().getExpressionText()).isEqualTo("1");
    assertThat(formDefinition.getOperatonFormDefinitionVersion().getExpressionText()).isEqualTo("1");
  }

  @Test
  @Deployment
  public void shouldParseOperatonFormDefinitionLatestBinding() {
    // given a deployed process with a UserTask containing an Operaton Form definition with latest binding
    // then
    TaskDefinition taskDefinition = findUserTaskDefinition("UserTask");
    FormDefinition formDefinition = taskDefinition.getFormDefinition();

    assertThat(taskDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");
    assertThat(formDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");

    assertThat(taskDefinition.getOperatonFormDefinitionBinding()).isEqualTo("latest");
    assertThat(formDefinition.getOperatonFormDefinitionBinding()).isEqualTo("latest");
  }

  @Test
  @Deployment
  public void shouldParseOperatonFormDefinitionDeploymentBinding() {
    // given a deployed process with a UserTask containing an Operaton Form definition with deployment binding
    // then
    TaskDefinition taskDefinition = findUserTaskDefinition("UserTask");
    FormDefinition formDefinition = taskDefinition.getFormDefinition();

    assertThat(taskDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");
    assertThat(formDefinition.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId");

    assertThat(taskDefinition.getOperatonFormDefinitionBinding()).isEqualTo("deployment");
    assertThat(formDefinition.getOperatonFormDefinitionBinding()).isEqualTo("deployment");
  }

  @Test
  @Deployment
  public void shouldParseTwoUserTasksWithOperatonFormDefinition() {
    // given a deployed process with two UserTask containing an Operaton Form definition with deployment binding
    // then
    TaskDefinition taskDefinition1 = findUserTaskDefinition("UserTask_1");
    FormDefinition formDefinition1 = taskDefinition1.getFormDefinition();

    assertThat(taskDefinition1.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId_1");
    assertThat(formDefinition1.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId_1");

    assertThat(taskDefinition1.getOperatonFormDefinitionBinding()).isEqualTo("deployment");
    assertThat(formDefinition1.getOperatonFormDefinitionBinding()).isEqualTo("deployment");

    TaskDefinition taskDefinition2 = findUserTaskDefinition("UserTask_2");
    FormDefinition formDefinition2 = taskDefinition2.getFormDefinition();
    assertThat(taskDefinition2.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId_2");
    assertThat(formDefinition2.getOperatonFormDefinitionKey().getExpressionText()).isEqualTo("formId_2");

    assertThat(taskDefinition2.getOperatonFormDefinitionBinding()).isEqualTo("version");
    assertThat(formDefinition2.getOperatonFormDefinitionBinding()).isEqualTo("version");

    assertThat(taskDefinition2.getOperatonFormDefinitionVersion().getExpressionText()).isEqualTo("2");
    assertThat(formDefinition2.getOperatonFormDefinitionVersion().getExpressionText()).isEqualTo("2");
  }

  @Test
  public void shouldNotParseOperatonFormDefinitionUnsupportedBinding() {
    // given a deployed process with a UserTask containing an Operaton Form definition with unsupported binding
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "shouldNotParseOperatonFormDefinitionUnsupportedBinding");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    // when/then expect parse exception
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Invalid element definition: value for formRefBinding attribute has to be one of [deployment, latest, version] but was unsupported");
  }

  @Test
  public void shouldNotParseOperatonFormDefinitionAndFormKey() {
    // given a deployed process with a UserTask containing an Operaton Form definition and formKey
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "shouldNotParseOperatonFormDefinitionAndFormKey");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    // when/then expect parse exception
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Invalid element definition: only one of the attributes formKey and formRef is allowed.");
  }

  private TaskDefinition findUserTaskDefinition(String activityId) {
    ActivityImpl userTask = findActivityInDeployedProcessDefinition(activityId);
    assertThat(userTask).isNotNull();

    return ((UserTaskActivityBehavior) userTask.getActivityBehavior()).getTaskDecorator()
        .getTaskDefinition();
  }
}
