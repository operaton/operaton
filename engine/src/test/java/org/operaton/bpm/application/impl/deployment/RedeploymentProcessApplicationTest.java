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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.application.ProcessApplicationExecutionException;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@Parameterized
@ExtendWith(ProcessEngineExtension.class)
public class RedeploymentProcessApplicationTest {

  protected static final String DEPLOYMENT_NAME = "my-deployment";

  protected static final String BPMN_RESOURCE_1 = "org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml";
  protected static final String BPMN_RESOURCE_2 = "org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml";

  protected static final String CMMN_RESOURCE_1 = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";
  protected static final String CMMN_RESOURCE_2 = "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn";

  protected static final String DMN_RESOURCE_1 = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDmnDeployment.dmn11.xml";
  protected static final String DMN_RESOURCE_2 = "org/operaton/bpm/engine/test/dmn/deployment/dmnScore.dmn11.xml";

  protected static final String DRD_RESOURCE_1 = "org/operaton/bpm/engine/test/dmn/deployment/drdScore.dmn11.xml";
  protected static final String DRD_RESOURCE_2 = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  static ProcessEngineConfigurationImpl processEngineConfiguration;
  static RepositoryService repositoryService;
  static RuntimeService runtimeService;
  static CaseService caseService;
  static DecisionService decisionService;
  static ManagementService managementService;

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

  public boolean enforceHistoryTimeToLive;

  public final List<Deployment> deploymentsToCleanup = new ArrayList<>();

  @Parameters
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
      { BPMN_RESOURCE_1, BPMN_RESOURCE_2, "processOne", "processTwo", processDefinitionTestProvider() },
      { CMMN_RESOURCE_1, CMMN_RESOURCE_2, "oneTaskCase", "twoTaskCase", caseDefinitionTestProvider() },
      { DMN_RESOURCE_1, DMN_RESOURCE_2, "decision", "score-decision", decisionDefinitionTestProvider() },
      { DRD_RESOURCE_1, DRD_RESOURCE_2, "score", "dish", decisionRequirementsDefinitionTestProvider() }
    });
  }

  @BeforeEach
  void init() {
    enforceHistoryTimeToLive = processEngineConfiguration.isEnforceHistoryTimeToLive();
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setEnforceHistoryTimeToLive(enforceHistoryTimeToLive);

    if (!deploymentsToCleanup.isEmpty()) {
      deleteDeployments(deploymentsToCleanup);
    }
  }

  @TestTemplate
  void definitionOnePreviousDeploymentWithPA() {
    // given

    MyEmbeddedProcessApplication application = new MyEmbeddedProcessApplication();

    // first deployment
    Deployment deployment1 = repositoryService
        .createDeployment(application.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    Deployment deployment2 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .deploy();

    assertThat(testProvider.countDefinitionsByKey(definitionKey1)).isEqualTo(2);

    // when
    testProvider.createInstanceByDefinitionKey(definitionKey1);

    // then
    assertThat(application.isCalled()).isTrue();

    deploymentsToCleanup.addAll(List.of(deployment1, deployment2));
  }

  @TestTemplate
  void redeploymentShouldFailOnNullHTTLAndEnforceHistoryTimeToLiveTrue() {
    // given
    Deployment deployment1;
    Deployment deployment2 = null;
    // given
    MyEmbeddedProcessApplication application = new MyEmbeddedProcessApplication();
    // given
    processEngineConfiguration.setEnforceHistoryTimeToLive(false);
    // given
    deployment1 = repositoryService
          .createDeployment(application.getReference())
          .name(DEPLOYMENT_NAME)
          .addClasspathResource(resource1)
          .deploy();
    // given
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);

    // given
    var deploymentBuilder = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId());

    assertThatThrownBy(deploymentBuilder::deploy)
        .withFailMessage("Deployment2 should throw ProcessEngineException due to mandatory historyTimeToLive")
        .isInstanceOf(ProcessEngineException.class);

    // cleanup
    if (deployment1 != null) {
      deploymentsToCleanup.add(deployment1);
    }

    if (deployment2 != null) {
      deploymentsToCleanup.add(deployment2);
    }
  }

  @TestTemplate
  void definitionTwoPreviousDeploymentWithPA() {
    // given

    // first deployment
    MyEmbeddedProcessApplication application1 = new MyEmbeddedProcessApplication();
    Deployment deployment1 = repositoryService
        .createDeployment(application1.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    MyEmbeddedProcessApplication application2 = new MyEmbeddedProcessApplication();
    Deployment deployment2 = repositoryService
        .createDeployment(application2.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    Deployment deployment3 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .deploy();

    assertThat(testProvider.countDefinitionsByKey(definitionKey1)).isEqualTo(3);

    // when
    testProvider.createInstanceByDefinitionKey(definitionKey1);

    // then
    assertThat(application1.isCalled()).isFalse();
    assertThat(application2.isCalled()).isTrue();

    deploymentsToCleanup.addAll(List.of(deployment1, deployment2, deployment3));
  }

  @TestTemplate
  void definitionTwoPreviousDeploymentFirstDeploymentWithPA() {
    // given

    // first deployment
    MyEmbeddedProcessApplication application1 = new MyEmbeddedProcessApplication();
    Deployment deployment1 = repositoryService
        .createDeployment(application1.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    Deployment deployment2 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    Deployment deployment3 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .deploy();

    assertThat(testProvider.countDefinitionsByKey(definitionKey1)).isEqualTo(3);

    // when
    testProvider.createInstanceByDefinitionKey(definitionKey1);

    // then
    assertThat(application1.isCalled()).isTrue();

    deploymentsToCleanup.addAll(List.of(deployment1, deployment2, deployment3));
  }

  @TestTemplate
  void definitionTwoPreviousDeploymentDeleteSecondDeployment() {
    // given

    // first deployment
    MyEmbeddedProcessApplication application1 = new MyEmbeddedProcessApplication();
    Deployment deployment1 = repositoryService
        .createDeployment(application1.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    MyEmbeddedProcessApplication application2 = new MyEmbeddedProcessApplication();
    Deployment deployment2 = repositoryService
        .createDeployment(application2.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    Deployment deployment3 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .deploy();

    assertThat(testProvider.countDefinitionsByKey(definitionKey1)).isEqualTo(3);

    // when
    deleteDeployments(deployment2);
    testProvider.createInstanceByDefinitionKey(definitionKey1);

    // then
    assertThat(application1.isCalled()).isTrue();
    assertThat(application2.isCalled()).isFalse();

    deploymentsToCleanup.addAll(List.of(deployment1, deployment3));
  }

  @TestTemplate
  void definitionTwoPreviousDeploymentUnregisterSecondPA() {
    // given

    // first deployment
    MyEmbeddedProcessApplication application1 = new MyEmbeddedProcessApplication();
    Deployment deployment1 = repositoryService
        .createDeployment(application1.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    MyEmbeddedProcessApplication application2 = new MyEmbeddedProcessApplication();
    Deployment deployment2 = repositoryService
        .createDeployment(application2.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    Deployment deployment3 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .deploy();

    assertThat(testProvider.countDefinitionsByKey(definitionKey1)).isEqualTo(3);

    // when
    managementService.unregisterProcessApplication(deployment2.getId(), true);
    testProvider.createInstanceByDefinitionKey(definitionKey1);

    // then
    assertThat(application1.isCalled()).isTrue();
    assertThat(application2.isCalled()).isFalse();

    deploymentsToCleanup.addAll(List.of(deployment1, deployment2, deployment3));
  }

  @TestTemplate
  void definitionTwoDifferentPreviousDeploymentsWithDifferentPA() {
    // given

    // first deployment
    MyEmbeddedProcessApplication application1 = new MyEmbeddedProcessApplication();
    Deployment deployment1 = repositoryService
        .createDeployment(application1.getReference())
        .name(DEPLOYMENT_NAME + "-1")
        .addClasspathResource(resource1)
        .deploy();

    // second deployment
    MyEmbeddedProcessApplication application2 = new MyEmbeddedProcessApplication();
    Deployment deployment2 = repositoryService
        .createDeployment(application2.getReference())
        .name(DEPLOYMENT_NAME + "-2")
        .addClasspathResource(resource2)
        .deploy();

    // second deployment
    Deployment deployment3 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-3")
        .addDeploymentResources(deployment1.getId())
        .addDeploymentResources(deployment2.getId())
        .deploy();

    assertThat(testProvider.countDefinitionsByKey(definitionKey1)).isEqualTo(2);
    assertThat(testProvider.countDefinitionsByKey(definitionKey2)).isEqualTo(2);

    // when (1)
    testProvider.createInstanceByDefinitionKey(definitionKey1);

    // then (1)
    assertThat(application1.isCalled()).isTrue();
    assertThat(application2.isCalled()).isFalse();

    // reset flag
    application1.setCalled(false);

    // when (2)
    testProvider.createInstanceByDefinitionKey(definitionKey2);

    // then (2)
    assertThat(application1.isCalled()).isFalse();
    assertThat(application2.isCalled()).isTrue();

    deploymentsToCleanup.addAll(List.of(deployment1, deployment2, deployment3));
  }

  @TestTemplate
  void definitionTwoPreviousDeploymentsWithDifferentPA() {
    // given

    // first deployment
    MyEmbeddedProcessApplication application1 = new MyEmbeddedProcessApplication();
    Deployment deployment1 = repositoryService
      .createDeployment(application1.getReference())
      .name(DEPLOYMENT_NAME)
      .addClasspathResource(resource1)
      .addClasspathResource(resource2)
      .deploy();

    // second deployment
    MyEmbeddedProcessApplication application2 = new MyEmbeddedProcessApplication();
    Deployment deployment2 = repositoryService
        .createDeployment(application2.getReference())
        .name(DEPLOYMENT_NAME)
        .addClasspathResource(resource1)
        .deploy();

    // third deployment
    Deployment deployment3 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .deploy();

    assertThat(testProvider.countDefinitionsByKey(definitionKey1)).isEqualTo(3);
    assertThat(testProvider.countDefinitionsByKey(definitionKey2)).isEqualTo(2);

    // when (1)
    testProvider.createInstanceByDefinitionKey(definitionKey1);

    // then (1)
    assertThat(application1.isCalled()).isFalse();
    assertThat(application2.isCalled()).isTrue();

    // reset flag
    application2.setCalled(false);

    // when (2)
    testProvider.createInstanceByDefinitionKey(definitionKey2);

    // then (2)
    assertThat(application1.isCalled()).isTrue();
    assertThat(application2.isCalled()).isFalse();

    deploymentsToCleanup.addAll(List.of(deployment1, deployment2, deployment3));
  }

  protected void deleteDeployments(List<Deployment> deployments) {
    Deployment[] array = new Deployment[deployments.size()];
    array = deployments.toArray(array);

    deleteDeployments(array);
  }

  protected void deleteDeployments(Deployment... deployments){
    for (Deployment deployment : deployments) {
      repositoryService.deleteDeployment(deployment.getId(), true);
      managementService.unregisterProcessApplication(deployment.getId(), false);
    }
  }

  protected interface TestProvider {
    long countDefinitionsByKey(String definitionKey);

    void createInstanceByDefinitionKey(String definitionKey);
  }

  protected static TestProvider processDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public long countDefinitionsByKey(String definitionKey) {
        return repositoryService.createProcessDefinitionQuery().processDefinitionKey(definitionKey).count();
      }

      @Override
      public void createInstanceByDefinitionKey(String definitionKey) {
        runtimeService.startProcessInstanceByKey(definitionKey, Variables.createVariables()
            .putValue("a", 1).putValue("b", 1));
      }

    };
  }

  protected static TestProvider caseDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public long countDefinitionsByKey(String definitionKey) {
        return repositoryService.createCaseDefinitionQuery().caseDefinitionKey(definitionKey).count();
      }

      @Override
      public void createInstanceByDefinitionKey(String definitionKey) {
        caseService.createCaseInstanceByKey(definitionKey);
      }

    };
  }

  protected static TestProvider decisionDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public long countDefinitionsByKey(String definitionKey) {
        return repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(definitionKey).count();
      }

      @Override
      public void createInstanceByDefinitionKey(String definitionKey) {
        decisionService.evaluateDecisionTableByKey(definitionKey)
          .variables(Variables.createVariables().putValue("input", "john"))
          .evaluate();
      }

    };
  }

  protected static TestProvider decisionRequirementsDefinitionTestProvider() {
    return new TestProvider() {

      @Override
      public long countDefinitionsByKey(String definitionKey) {
        return repositoryService.createDecisionRequirementsDefinitionQuery().decisionRequirementsDefinitionKey(definitionKey).count();
      }

      @Override
      public void createInstanceByDefinitionKey(String definitionKey) {
        decisionService.evaluateDecisionTableByKey(definitionKey + "-decision")
          .variables(Variables.createVariables()
              .putValue("temperature", 21)
              .putValue("dayType", "Weekend")
              .putValue("input", "John"))
          .evaluate();
      }

    };
  }

  public class MyEmbeddedProcessApplication extends EmbeddedProcessApplication {

    protected ProcessApplicationReference reference;
    protected boolean called;

    @Override
    public ProcessApplicationReference getReference() {
      if (reference == null) {
        reference = super.getReference();
      }
      return reference;
    }

    @Override
    public <T> T execute(Callable<T> callable) throws ProcessApplicationExecutionException {
      called = true;
      return super.execute(callable);
    }

    public boolean isCalled() {
      return called;
    }

    public void setCalled(boolean called) {
      this.called = called;
    }

  }

}
