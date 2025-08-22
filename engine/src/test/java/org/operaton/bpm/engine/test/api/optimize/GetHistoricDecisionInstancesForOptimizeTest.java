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
package org.operaton.bpm.engine.test.api.optimize;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class GetHistoricDecisionInstancesForOptimizeTest {

  public static final String DECISION_PROCESS =
    "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.processWithBusinessRuleTask.bpmn20.xml";
  public static final String DECISION_SINGLE_OUTPUT_DMN =
    "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml";

  protected static final String VARIABLE_NAME = "aVariableName";
  protected static final String VARIABLE_VALUE = "aVariableValue";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected String userId = "test";
  OptimizeService optimizeService;
  IdentityService identityService;
  RuntimeService runtimeService;
  AuthorizationService authorizationService;

  @BeforeEach
  void init() {
    ProcessEngineConfigurationImpl config =
      engineRule.getProcessEngineConfiguration();
    optimizeService = config.getOptimizeService();

    createUser(userId);
  }

  @AfterEach
  void cleanUp() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
    ClockUtil.reset();
  }

  @BeforeEach
  void enableDmnFeelLegacyBehavior() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
            .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();
  }

  @AfterEach
  void disableDmnFeelLegacyBehavior() {

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
            .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void getCompletedHistoricDecisionInstances() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(pastDate(), null, 10);

    // then
    assertThat(decisionInstances).hasSize(1);
    assertThatDecisionsHaveAllImportantInformation(decisionInstances);
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void decisionInputInstanceProperties() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(pastDate(), null, 10);

    // then
    assertThat(decisionInstances).hasSize(1);
    HistoricDecisionInstance decisionInstance = decisionInstances.get(0);
    List<HistoricDecisionInputInstance> inputs = decisionInstance.getInputs();
    assertThat(inputs)
      .isNotNull()
      .hasSize(1);

    HistoricDecisionInputInstance input = inputs.get(0);
    assertThat(input.getDecisionInstanceId()).isEqualTo(decisionInstance.getId());
    assertThat(input.getClauseId()).isEqualTo("in");
    assertThat(input.getClauseName()).isEqualTo("input");
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void decisionOutputInstanceProperties() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(pastDate(), null, 10);

    // then
    assertThat(decisionInstances).hasSize(1);
    HistoricDecisionInstance decisionInstance = decisionInstances.get(0);
    List<HistoricDecisionOutputInstance> outputs = decisionInstance.getOutputs();
    assertThat(outputs)
      .isNotNull()
      .hasSize(1);

    HistoricDecisionOutputInstance output = outputs.get(0);
    assertThat(output.getDecisionInstanceId()).isEqualTo(decisionInstance.getId());
    assertThat(output.getClauseId()).isEqualTo("out");
    assertThat(output.getClauseName()).isEqualTo("output");

    assertThat(output.getRuleId()).isEqualTo("rule");
    assertThat(output.getRuleOrder()).isEqualTo(1);

    assertThat(output.getVariableName()).isEqualTo("result");
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void fishedAfterParameterWorks() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    ProcessInstance secondProcessInstance =
      runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(now, null, 10);

    // then
    assertThat(decisionInstances).hasSize(1);
    HistoricDecisionInstance decisionInstance = decisionInstances.get(0);
    assertThat(decisionInstance.getProcessInstanceId()).isEqualTo(secondProcessInstance.getId());
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void fishedAtParameterWorks() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    ProcessInstance firstProcessInstance =
      runtimeService.startProcessInstanceByKey("testProcess", variables);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(null, now, 10);

    // then
    assertThat(decisionInstances).hasSize(1);
    HistoricDecisionInstance decisionInstance = decisionInstances.get(0);
    assertThat(decisionInstance.getProcessInstanceId()).isEqualTo(firstProcessInstance.getId());
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void fishedAfterAndFinishedAtParameterWorks() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    Date now = new Date();
    Date nowMinus2Seconds = new Date(now.getTime() - 2000L);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);

    ClockUtil.setCurrentTime(nowMinus2Seconds);
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    ClockUtil.setCurrentTime(now);
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(now, now, 10);

    // then
    assertThat(decisionInstances).isEmpty();
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void maxResultsParameterWorks() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(null, null, 2);

    // then
    assertThat(decisionInstances).hasSize(2);
  }

  @Test
  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  void resultIsSortedByEvaluationTime() {
    // given start process and evaluate decision
    VariableMap variables = Variables.createVariables();
    variables.put("input1", null);
    Date now = new Date();
    Date nowMinus2Seconds = new Date(now.getTime() - 2000L);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);

    ClockUtil.setCurrentTime(nowMinus2Seconds);
    ProcessInstance firstProcessInstance =
      runtimeService.startProcessInstanceByKey("testProcess", variables);
    ClockUtil.setCurrentTime(now);
    ProcessInstance secondProcessInstance =
      runtimeService.startProcessInstanceByKey("testProcess", variables);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    ProcessInstance thirdProcessInstance =
      runtimeService.startProcessInstanceByKey("testProcess", variables);

    // when
    List<HistoricDecisionInstance> decisionInstances =
      optimizeService.getHistoricDecisionInstances(pastDate(), null, 3);

    // then
    assertThat(decisionInstances).hasSize(3);
    assertThat(decisionInstances.get(0).getProcessInstanceId()).isEqualTo(firstProcessInstance.getId());
    assertThat(decisionInstances.get(1).getProcessInstanceId()).isEqualTo(secondProcessInstance.getId());
    assertThat(decisionInstances.get(2).getProcessInstanceId()).isEqualTo(thirdProcessInstance.getId());
  }

  private Date pastDate() {
    return new Date(2L);
  }

  protected void createUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);
  }

  private void assertThatDecisionsHaveAllImportantInformation(List<HistoricDecisionInstance> decisionInstances) {
    assertThat(decisionInstances).hasSize(1);
    HistoricDecisionInstance decisionInstance =
      decisionInstances.get(0);


    assertThat(decisionInstance).isNotNull();
    assertThat(decisionInstance.getProcessDefinitionKey()).isEqualTo("testProcess");
    assertThat(decisionInstance.getProcessDefinitionId()).isNotNull();
    assertThat(decisionInstance.getDecisionDefinitionId()).isNotNull();
    assertThat(decisionInstance.getDecisionDefinitionKey()).isEqualTo("testDecision");
    assertThat(decisionInstance.getDecisionDefinitionName()).isEqualTo("sample decision");

    assertThat(decisionInstance.getActivityId()).isEqualTo("task");
    assertThat(decisionInstance.getActivityInstanceId()).isNotNull();

    assertThat(decisionInstance.getProcessInstanceId()).isNotNull();
    assertThat(decisionInstance.getRootProcessInstanceId()).isNotNull();

    assertThat(decisionInstance.getCaseDefinitionKey()).isNull();
    assertThat(decisionInstance.getCaseDefinitionId()).isNull();

    assertThat(decisionInstance.getCaseInstanceId()).isNull();

    assertThat(decisionInstance.getRootDecisionInstanceId()).isNull();
    assertThat(decisionInstance.getDecisionRequirementsDefinitionId()).isNull();
    assertThat(decisionInstance.getDecisionRequirementsDefinitionKey()).isNull();

    assertThat(decisionInstance.getEvaluationTime()).isNotNull();
  }

}
