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
package org.operaton.bpm.application.impl.deployment;
import java.util.List;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.application.ProcessApplicationManager;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionEntity;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
@ExtendWith(ProcessEngineExtension.class)
public class RedeploymentRegistrationTest {

  protected static final String DEPLOYMENT_NAME = "my-deployment";

  protected static final String BPMN_RESOURCE_1 = "org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml";
  protected static final String BPMN_RESOURCE_2 = "org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml";

  protected static final String CMMN_RESOURCE_1 = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";
  protected static final String CMMN_RESOURCE_2 = "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn";

  protected static final String DMN_RESOURCE_1 = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDmnDeployment.dmn11.xml";
  protected static final String DMN_RESOURCE_2 = "org/operaton/bpm/engine/test/dmn/deployment/dmnScore.dmn11.xml";

  protected static final String DRD_RESOURCE_1 = "org/operaton/bpm/engine/test/dmn/deployment/drdScore.dmn11.xml";
  protected static final String DRD_RESOURCE_2 = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  protected EmbeddedProcessApplication processApplication;

  RepositoryService repositoryService;
  ManagementService managementService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @Parameter(0)
  public String resource1;

  @Parameter(1)
  public String resource2;

  @Parameter(2)
  public String definitionKey1;

  @Parameter(3)
  public String definitionKey2;

  @Parameter(4)
  public TestProvider testProvider;

  @Parameters
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
      { BPMN_RESOURCE_1, BPMN_RESOURCE_2, "processOne", "processTwo", processDefinitionTestProvider() },
      { CMMN_RESOURCE_1, CMMN_RESOURCE_2, "oneTaskCase", "twoTaskCase", caseDefinitionTestProvider() },
      { DMN_RESOURCE_1, DMN_RESOURCE_2, "decision", "score-decision", decisionDefinitionTestProvider() },
      { DRD_RESOURCE_1, DRD_RESOURCE_2, "score", "dish", decisionRequirementsDefinitionTestProvider() } });
  }

  @BeforeEach
  void init() {
    processApplication = new EmbeddedProcessApplication();
  }

  @TestTemplate
  void registrationNotFoundByDeploymentId() {
    // given
    ProcessApplicationReference reference = processApplication.getReference();

    Deployment deployment1 = repositoryService.createDeployment(reference)
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .deploy();

    assertThat(getProcessApplicationForDeployment(deployment1.getId())).isEqualTo(reference);

    // when
    Deployment deployment2 = repositoryService.createDeployment()
      .name(DEPLOYMENT_NAME)
      .addDeploymentResources(deployment1.getId())
      .deploy();

    // then
    assertThat(getProcessApplicationForDeployment(deployment2.getId())).isNull();
  }

  @TestTemplate
  void registrationNotFoundByDefinition() {
    // given

    // first deployment
    Deployment deployment1 = repositoryService.createDeployment()
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .deploy();

    // second deployment
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addClasspathResource(resource1).deploy();

    // when
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addDeploymentResources(deployment1.getId()).deploy();

    String definitionId = getLatestDefinitionIdByKey(definitionKey1);

    // then
    assertThat(getProcessApplicationForDefinition(definitionId)).isNull();
  }

  @TestTemplate
  void registrationFoundByDeploymentId() {
    // given
    ProcessApplicationReference reference1 = processApplication.getReference();

    Deployment deployment1 = repositoryService.createDeployment(reference1)
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .deploy();

    assertThat(getProcessApplicationForDeployment(deployment1.getId())).isEqualTo(reference1);

    // when
    ProcessApplicationReference reference2 = processApplication.getReference();

    Deployment deployment2 = repositoryService.createDeployment(reference2)
      .name(DEPLOYMENT_NAME)
      .addDeploymentResources(deployment1.getId())
      .deploy();

    // then
    assertThat(getProcessApplicationForDeployment(deployment2.getId())).isEqualTo(reference2);
  }

  @TestTemplate
  void registrationFoundFromPreviousDefinition() {
    // given
    ProcessApplicationReference reference = processApplication.getReference();
    Deployment deployment1 = repositoryService.createDeployment(reference)
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .deploy();

    // when
    Deployment deployment2 = repositoryService.createDeployment()
      .name(DEPLOYMENT_NAME)
      .addDeploymentResources(deployment1.getId())
      .deploy();

    String definitionId = getLatestDefinitionIdByKey(definitionKey1);

    // then
    assertThat(getProcessApplicationForDefinition(definitionId)).isEqualTo(reference);

    // and the reference is not cached
    assertThat(getProcessApplicationForDeployment(deployment2.getId())).isNull();
  }

  @TestTemplate
  void registrationFoundFromLatestDeployment() {
    // given
    ProcessApplicationReference reference1 = processApplication.getReference();
    Deployment deployment1 = repositoryService.createDeployment(reference1)
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .deploy();

    // when
    ProcessApplicationReference reference2 = processApplication.getReference();
    Deployment deployment2 = repositoryService.createDeployment(reference2)
      .name(DEPLOYMENT_NAME)
      .addDeploymentResources(deployment1.getId())
      .deploy();

    String definitionId = getLatestDefinitionIdByKey(definitionKey1);

    // then
    assertThat(getProcessApplicationForDefinition(definitionId)).isEqualTo(reference2);
    assertThat(getProcessApplicationForDeployment(deployment2.getId())).isEqualTo(reference2);
  }

  @TestTemplate
  void registrationFoundOnlyForOneProcessDefinition() {
    // given

    // first deployment
    Deployment deployment1 = repositoryService.createDeployment()
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .addClasspathResource(resource2)
      .deploy();

    // second deployment
    ProcessApplicationReference reference2 = processApplication.getReference();
    repositoryService.createDeployment(reference2).name(DEPLOYMENT_NAME).addClasspathResource(resource1).deploy();

    // when
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addDeploymentResources(deployment1.getId()).deploy();

    String firstDefinitionId = getLatestDefinitionIdByKey(definitionKey1);
    String secondDefinitionId = getLatestDefinitionIdByKey(definitionKey2);

    // then
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isEqualTo(reference2);
    assertThat(getProcessApplicationForDefinition(secondDefinitionId)).isNull();
  }

  @TestTemplate
  void registrationFoundFromDifferentDeployment() {
    // given

    // first deployment
    ProcessApplicationReference reference1 = processApplication.getReference();
    Deployment deployment1 = repositoryService.createDeployment(reference1)
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .addClasspathResource(resource2)
      .deploy();

    // second deployment
    ProcessApplicationReference reference2 = processApplication.getReference();
    repositoryService.createDeployment(reference2).name(DEPLOYMENT_NAME).addClasspathResource(resource1).deploy();

    // when
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addDeploymentResources(deployment1.getId()).deploy();

    String firstDefinitionId = getLatestDefinitionIdByKey(definitionKey1);
    String secondDefinitionId = getLatestDefinitionIdByKey(definitionKey2);

    // then
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isEqualTo(reference2);
    assertThat(getProcessApplicationForDefinition(secondDefinitionId)).isEqualTo(reference1);
  }

  @TestTemplate
  void registrationFoundFromSameDeployment() {
    // given

    // first deployment
    ProcessApplicationReference reference1 = processApplication.getReference();
    Deployment deployment1 = repositoryService.createDeployment(reference1)
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .addClasspathResource(resource2)
      .deploy();

    // second deployment
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addClasspathResource(resource1).deploy();

    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addClasspathResource(resource2).deploy();

    // when
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addDeploymentResources(deployment1.getId()).deploy();

    String firstDefinitionId = getLatestDefinitionIdByKey(definitionKey1);
    String secondDefinitionId = getLatestDefinitionIdByKey(definitionKey1);

    // then
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isEqualTo(reference1);
    assertThat(getProcessApplicationForDefinition(secondDefinitionId)).isEqualTo(reference1);
  }

  @TestTemplate
  void registrationFoundFromDifferentDeployments() {
    // given

    // first deployment
    ProcessApplicationReference reference1 = processApplication.getReference();
    Deployment deployment1 = repositoryService.createDeployment(reference1)
      .name(DEPLOYMENT_NAME + "-1")
      .addClasspathResource(resource1)
      .deploy();

    // second deployment
    ProcessApplicationReference reference2 = processApplication.getReference();
    repositoryService.createDeployment(reference2)
      .name(DEPLOYMENT_NAME + "-2")
      .addClasspathResource(resource2)
      .deploy();

    // when
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addDeploymentResources(deployment1.getId()).deploy();

    String firstDefinitionId = getLatestDefinitionIdByKey(definitionKey1);
    String secondDefinitionId = getLatestDefinitionIdByKey(definitionKey2);

    // then
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isEqualTo(reference1);
    assertThat(getProcessApplicationForDefinition(secondDefinitionId)).isEqualTo(reference2);
  }

  @TestTemplate
  void registrationNotFoundWhenDeletingDeployment() {
    // given

    // first deployment
    Deployment deployment1 = repositoryService.createDeployment()
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .deploy();

    // second deployment
    ProcessApplicationReference reference2 = processApplication.getReference();
    Deployment deployment2 = repositoryService.createDeployment(reference2)
      .name(DEPLOYMENT_NAME)
      .addDeploymentResources(deployment1.getId())
      .deploy();

    // when (1)
    // third deployment
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addDeploymentResources(deployment1.getId()).deploy();

    String firstDefinitionId = getLatestDefinitionIdByKey(definitionKey1);

    // then (1)
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isEqualTo(reference2);

    // when (2)
    deleteDeployment(deployment2);

    // then (2)
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isNull();
  }

  @TestTemplate
  void registrationFoundAfterDiscardingDeploymentCache() {
    // given

    // first deployment
    Deployment deployment1 = repositoryService.createDeployment()
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .deploy();

    // second deployment
    ProcessApplicationReference reference2 = processApplication.getReference();
    repositoryService.createDeployment(reference2)
      .name(DEPLOYMENT_NAME)
      .addDeploymentResources(deployment1.getId())
      .deploy();

    // when (1)
    // third deployment
    repositoryService.createDeployment().name(DEPLOYMENT_NAME).addDeploymentResources(deployment1.getId()).deploy();

    String firstDefinitionId = getLatestDefinitionIdByKey(definitionKey1);

    // then (1)
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isEqualTo(reference2);

    // when (2)
    discardDefinitionCache();

    // then (2)
    assertThat(getProcessApplicationForDefinition(firstDefinitionId)).isEqualTo(reference2);
  }

  // helper ///////////////////////////////////////////

  @AfterEach
  void cleanUp() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      deleteDeployment(deployment);
    }
  }

  protected void deleteDeployment(Deployment deployment) {
    repositoryService.deleteDeployment(deployment.getId(), true);
    managementService.unregisterProcessApplication(deployment.getId(), false);
  }

  protected ProcessApplicationReference getProcessApplicationForDeployment(String deploymentId) {
    ProcessApplicationManager processApplicationManager = processEngineConfiguration.getProcessApplicationManager();
    return processApplicationManager.getProcessApplicationForDeployment(deploymentId);
  }

  protected void discardDefinitionCache() {
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();
    processEngineConfiguration.getDeploymentCache().discardCaseDefinitionCache();
    processEngineConfiguration.getDeploymentCache().discardDecisionDefinitionCache();
    processEngineConfiguration.getDeploymentCache().discardDecisionRequirementsDefinitionCache();
  }

  protected String getLatestDefinitionIdByKey(String key) {
    return testProvider.getLatestDefinitionIdByKey(repositoryService, key);
  }

  protected ProcessApplicationReference getProcessApplicationForDefinition(String definitionId) {
    return processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(testProvider.createGetProcessApplicationCommand(definitionId));
  }

  private interface TestProvider {
    Command<ProcessApplicationReference> createGetProcessApplicationCommand(String definitionId);

    String getLatestDefinitionIdByKey(RepositoryService repositoryService, String key);
  }

  protected static TestProvider processDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public Command<ProcessApplicationReference> createGetProcessApplicationCommand(final String definitionId) {
        return commandContext -> {
          ProcessEngineConfigurationImpl configuration = commandContext.getProcessEngineConfiguration();
          DeploymentCache deploymentCache = configuration.getDeploymentCache();
          ProcessDefinitionEntity definition = deploymentCache.findDeployedProcessDefinitionById(definitionId);
          return ProcessApplicationContextUtil.getTargetProcessApplication(definition);
        };
      }

      @Override
      public String getLatestDefinitionIdByKey(RepositoryService repositoryService, String key) {
        return repositoryService.createProcessDefinitionQuery()
          .processDefinitionKey(key)
          .latestVersion()
          .singleResult()
          .getId();
      }

    };
  }

  protected static TestProvider caseDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public Command<ProcessApplicationReference> createGetProcessApplicationCommand(final String definitionId) {
        return commandContext -> {
          ProcessEngineConfigurationImpl configuration = commandContext.getProcessEngineConfiguration();
          DeploymentCache deploymentCache = configuration.getDeploymentCache();
          CaseDefinitionEntity definition = deploymentCache.findDeployedCaseDefinitionById(definitionId);
          return ProcessApplicationContextUtil.getTargetProcessApplication(definition);
        };
      }

      @Override
      public String getLatestDefinitionIdByKey(RepositoryService repositoryService, String key) {
        return repositoryService.createCaseDefinitionQuery()
          .caseDefinitionKey(key)
          .latestVersion()
          .singleResult()
          .getId();
      }

    };
  }

  protected static TestProvider decisionDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public Command<ProcessApplicationReference> createGetProcessApplicationCommand(final String definitionId) {
        return commandContext -> {
          ProcessEngineConfigurationImpl configuration = commandContext.getProcessEngineConfiguration();
          DeploymentCache deploymentCache = configuration.getDeploymentCache();
          DecisionDefinitionEntity definition = deploymentCache.findDeployedDecisionDefinitionById(definitionId);
          return ProcessApplicationContextUtil.getTargetProcessApplication(definition);
        };
      }

      @Override
      public String getLatestDefinitionIdByKey(RepositoryService repositoryService, String key) {
        return repositoryService.createDecisionDefinitionQuery()
          .decisionDefinitionKey(key)
          .latestVersion()
          .singleResult()
          .getId();
      }

    };
  }

  protected static TestProvider decisionRequirementsDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public Command<ProcessApplicationReference> createGetProcessApplicationCommand(final String definitionId) {
        return commandContext -> {
          ProcessEngineConfigurationImpl configuration = commandContext.getProcessEngineConfiguration();
          DeploymentCache deploymentCache = configuration.getDeploymentCache();
          DecisionRequirementsDefinitionEntity definition = deploymentCache.findDeployedDecisionRequirementsDefinitionById(
            definitionId);
          return ProcessApplicationContextUtil.getTargetProcessApplication(definition);
        };
      }

      @Override
      public String getLatestDefinitionIdByKey(RepositoryService repositoryService, String key) {
        return repositoryService.createDecisionRequirementsDefinitionQuery()
          .decisionRequirementsDefinitionKey(key)
          .latestVersion()
          .singleResult()
          .getId();
      }

    };
  }

}
