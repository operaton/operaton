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

package org.operaton.bpm.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import org.operaton.bpm.dmn.engine.impl.transform.DmnTransformException;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.commons.testing.ProcessEngineLoggingRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class HistoryTimeToLiveDeploymentTest {

  protected static final String CONFIG_LOGGER = "org.operaton.bpm.engine.cfg";

  protected static final String EXPECTED_DEFAULT_CONFIG_MSG = "History Time To Live (TTL) cannot be null. ";

  protected static final String EXPECTED_LONGER_TTL_MSG = "The specified Time To Live (TTL) in the model is longer than the global TTL configuration. "
      + "The historic data related to this model will be cleaned up at later point comparing to the other processes.";

  protected static final String HTTL_CONFIG_VALUE = "180";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  @Rule
  public ProcessEngineLoggingRule loggingRule = new ProcessEngineLoggingRule()
      .watch(CONFIG_LOGGER)
      .level(Level.DEBUG);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;
  protected ProcessEngine processEngine;

  protected String historyTimeToLive;

  @Before
  public void setUp() {
    processEngine = engineRule.getProcessEngine();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    repositoryService = engineRule.getRepositoryService();
    managementService = engineRule.getManagementService();

    historyTimeToLive = processEngineConfiguration.getHistoryTimeToLive();
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);
  }

  @After
  public void tearDown() {
    processEngineConfiguration.setHistoryTimeToLive(historyTimeToLive);
    processEngineConfiguration.setEnforceHistoryTimeToLive(false);
    ClockUtil.reset();
  }

  @Test
  public void processWithoutHTTLShouldFail() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml");
    // when
    assertThatThrownBy(() -> testRule.deploy(deploymentBuilder))
        // then
        .isInstanceOf(ParseException.class)
        .hasMessageContaining(EXPECTED_DEFAULT_CONFIG_MSG)
        .hasMessageContaining("TTL is necessary for the History Cleanup to work. The following options are possible:")
        .hasMessageContaining("* Set historyTimeToLive in the model")
        .hasMessageContaining("* Set a default historyTimeToLive as a global process engine configuration")
        .hasMessageContaining("* (Not recommended) Deactivate the enforceTTL config to disable this check");
  }

  @Test
  public void processWithHTTLShouldSucceed() {
    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version3.bpmn20.xml"));

    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    // then
    assertThat(deployment).isNotNull();
  }

  @Test
  public void caseWithHTTLShouldSucceed() {
    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithHistoryTimeToLive.cmmn"));

    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    // then
    assertThat(deployment).isNotNull();
  }

  @Test
  public void caseWithoutHTTLShouldFail() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn");
    // when
    assertThatThrownBy(() -> testRule.deploy(deploymentBuilder))
        // then
        .isInstanceOf(ProcessEngineException.class)
        .hasCauseInstanceOf(NotValidException.class)
        .hasStackTraceContaining(EXPECTED_DEFAULT_CONFIG_MSG);
  }

  @Test
  public void decisionWithHTTLShouldSucceed() {
    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/dmn/Example.dmn"));

    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    // then
    assertThat(deployment).isNotNull();
  }

  @Test
  public void decisionWithoutHTTLShouldFail() {
    var deploymentBuilder = repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/dmn/Another_Example.dmn");
    // when
    assertThatThrownBy(() -> testRule.deploy(deploymentBuilder))
        // then
        .isInstanceOf(ProcessEngineException.class)
        .hasCauseInstanceOf(DmnTransformException.class)
        .hasStackTraceContaining(EXPECTED_DEFAULT_CONFIG_MSG);
  }

  @Test
  public void shouldDeploySuccessfullyDueToProcessEngineConfigFallback() {
    // given
    processEngineConfiguration.setHistoryTimeToLive("5");

    // when
    testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    // then
    assertThat(deployment).isNotNull();
  }

  @Test
  public void shouldNotLogMessageOnDefaultConfigOriginatingFromConfig() {
    // given
    processEngineConfiguration.setHistoryTimeToLive(HTTL_CONFIG_VALUE);

    // when
    testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    // then
    assertThat(loggingRule.getFilteredLog(EXPECTED_DEFAULT_CONFIG_MSG)).isEmpty();
  }

  @Test
  public void shouldGetDeployedProcess() {
    // given
    processEngineConfiguration.setEnforceHistoryTimeToLive(false);

    // when
    DeploymentWithDefinitions definitions = testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    // then
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);
    processEngineConfiguration.getDeploymentCache().purgeCache();
    repositoryService.getProcessDefinition(definitions.getDeployedProcessDefinitions().get(0).getId());
  }

  @Test
  public void shouldGetDeployedDecision() {
    // given
    processEngineConfiguration.setEnforceHistoryTimeToLive(false);

    // when
    DeploymentWithDefinitions definitions = testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/dmn/Another_Example.dmn"));

    // then
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);
    processEngineConfiguration.getDeploymentCache().purgeCache();
    repositoryService.getDecisionDefinition(definitions.getDeployedDecisionDefinitions().get(0).getId());
  }

  @Test
  public void shouldGetDeployedCase() {
    // given
    processEngineConfiguration.setEnforceHistoryTimeToLive(false);

    // when
    DeploymentWithDefinitions definitions = testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn"));

    // then
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);
    processEngineConfiguration.getDeploymentCache().purgeCache();
    repositoryService.getCaseDefinition(definitions.getDeployedCaseDefinitions().get(0).getId());
  }

  @Test
  public void shouldLogMessageOnLongerTTLInProcessModel() {
    // given
    String nonDefaultValue = "179";
    processEngineConfiguration.setHistoryTimeToLive(nonDefaultValue);

    // when
    deployProcessDefinitions();

    // then
    assertThat(loggingRule.getFilteredLog("definitionKey: process; " + EXPECTED_LONGER_TTL_MSG)).hasSize(1);
  }

  @Test
  public void shouldLogMessageOnLongerTTLInfCaseModel() {
    // given
    String nonDefaultValue = "179";
    processEngineConfiguration.setHistoryTimeToLive(nonDefaultValue);

    // when
    testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/case_with_365_httl.cmmn"));

    // then
    assertThat(loggingRule.getFilteredLog("definitionKey: testCase; " + EXPECTED_LONGER_TTL_MSG)).hasSize(1);
  }

  @Test
  public void shouldLogMessageOnLongerTTLInDecisionModel() {
    // given
    String nonDefaultValue = "179";
    processEngineConfiguration.setHistoryTimeToLive(nonDefaultValue);

    // when
    testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/decision_with_365_httl.dmn"));

    // then
    assertThat(loggingRule.getFilteredLog("definitionKey: testDecision; " + EXPECTED_LONGER_TTL_MSG)).hasSize(1);
  }

  protected void deployProcessDefinitions() {
    testRule.deploy(
      Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(365)
        .startEvent()
        .userTask()
        .endEvent()
        .done());
  }

}
