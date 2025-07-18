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
package org.operaton.bpm.engine.test.history.dmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.history.DecisionServiceDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Philipp Ossler
 * @author Ingo Richtsmeier
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
public class HistoricDecisionInstanceTest {

  public static final String DECISION_CASE = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.caseWithDecisionTask.cmmn";
  public static final String DECISION_CASE_WITH_DECISION_SERVICE = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.testCaseDecisionEvaluatedWithDecisionServiceInsideDelegate.cmmn";
  public static final String DECISION_CASE_WITH_DECISION_SERVICE_INSIDE_RULE = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.testManualActivationRuleEvaluatesDecision.cmmn";
  public static final String DECISION_CASE_WITH_DECISION_SERVICE_INSIDE_IF_PART = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.testIfPartEvaluatesDecision.cmmn";

  public static final String DECISION_PROCESS = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.processWithBusinessRuleTask.bpmn20.xml";

  public static final String DECISION_SINGLE_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml";
  public static final String DECISION_MULTIPLE_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionMultipleOutput.dmn11.xml";
  public static final String DECISION_COMPOUND_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionCompoundOutput.dmn11.xml";
  public static final String DECISION_MULTIPLE_INPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionMultipleInput.dmn11.xml";
  public static final String DECISION_COLLECT_SUM_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionCollectSum.dmn11.xml";
  public static final String DECISION_RETURNS_TRUE = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.returnsTrue.dmn11.xml";

  public static final String DECISION_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/engine/test/api/dmn/DecisionWithLiteralExpression.dmn";

  public static final String DRG_DMN = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  public static final String DECISION_DEFINITION_KEY = "testDecision";

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  HistoryService historyService;
  DecisionService decisionService;
  IdentityService identityService;
  CaseService caseService;
  ManagementService managementService;

  @BeforeEach
  void setUp() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();


  }

  @AfterEach
  void tearDown() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();


  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDecisionInstanceProperties() {

    startProcessInstanceAndEvaluateDecision();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(DECISION_DEFINITION_KEY).singleResult().getId();
    String activityInstanceId = historyService.createHistoricActivityInstanceQuery().activityId("task").singleResult().getId();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    assertThat(historicDecisionInstance.getDecisionDefinitionId()).isEqualTo(decisionDefinitionId);
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("sample decision");

    assertThat(historicDecisionInstance.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(historicDecisionInstance.getProcessDefinitionId()).isEqualTo(processDefinition.getId());

    assertThat(historicDecisionInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());

    assertThat(historicDecisionInstance.getCaseDefinitionKey()).isNull();
    assertThat(historicDecisionInstance.getCaseDefinitionId()).isNull();

    assertThat(historicDecisionInstance.getCaseInstanceId()).isNull();

    assertThat(historicDecisionInstance.getActivityId()).isEqualTo("task");
    assertThat(historicDecisionInstance.getActivityInstanceId()).isEqualTo(activityInstanceId);

    assertThat(historicDecisionInstance.getRootDecisionInstanceId()).isNull();
    assertThat(historicDecisionInstance.getDecisionRequirementsDefinitionId()).isNull();
    assertThat(historicDecisionInstance.getDecisionRequirementsDefinitionKey()).isNull();

    assertThat(historicDecisionInstance.getEvaluationTime()).isNotNull();
  }

  @Deployment(resources = {DECISION_CASE, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testCaseDecisionInstanceProperties() {

    CaseInstance caseInstance = createCaseInstanceAndEvaluateDecision();

    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionId(caseInstance.getCaseDefinitionId())
        .singleResult();

    String decisionDefinitionId = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .singleResult()
        .getId();

    String activityInstanceId = historyService
        .createHistoricCaseActivityInstanceQuery()
        .caseActivityId("PI_DecisionTask_1")
        .singleResult()
        .getId();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    assertThat(historicDecisionInstance.getDecisionDefinitionId()).isEqualTo(decisionDefinitionId);
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("sample decision");

    assertThat(historicDecisionInstance.getProcessDefinitionKey()).isNull();
    assertThat(historicDecisionInstance.getProcessDefinitionId()).isNull();
    assertThat(historicDecisionInstance.getProcessInstanceId()).isNull();

    assertThat(historicDecisionInstance.getCaseDefinitionKey()).isEqualTo(caseDefinition.getKey());
    assertThat(historicDecisionInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
    assertThat(historicDecisionInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());

    assertThat(historicDecisionInstance.getActivityId()).isEqualTo("PI_DecisionTask_1");
    assertThat(historicDecisionInstance.getActivityInstanceId()).isEqualTo(activityInstanceId);

    assertThat(historicDecisionInstance.getEvaluationTime()).isNotNull();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDecisionInputInstanceProperties() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().includeInputs().singleResult();
    List<HistoricDecisionInputInstance> inputs = historicDecisionInstance.getInputs();
    assertThat(inputs)
      .isNotNull()
      .hasSize(1);

    HistoricDecisionInputInstance input = inputs.get(0);
    assertThat(input.getDecisionInstanceId()).isEqualTo(historicDecisionInstance.getId());
    assertThat(input.getClauseId()).isEqualTo("in");
    assertThat(input.getClauseName()).isEqualTo("input");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testMultipleDecisionInstances() {

    startProcessInstanceAndEvaluateDecision("a");
    waitASignificantAmountOfTime();
    startProcessInstanceAndEvaluateDecision("b");

    List<HistoricDecisionInstance> historicDecisionInstances = historyService
        .createHistoricDecisionInstanceQuery()
        .includeInputs()
        .orderByEvaluationTime().asc()
        .list();
    assertThat(historicDecisionInstances).hasSize(2);

    List<HistoricDecisionInputInstance> inputsOfFirstDecision = historicDecisionInstances.get(0).getInputs();
    assertThat(inputsOfFirstDecision).hasSize(1);
    assertThat(inputsOfFirstDecision.get(0).getValue()).isEqualTo("a");

    List<HistoricDecisionInputInstance> inputsOfSecondDecision = historicDecisionInstances.get(1).getInputs();
    assertThat(inputsOfSecondDecision).hasSize(1);
    assertThat(inputsOfSecondDecision.get(0).getValue()).isEqualTo("b");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_MULTIPLE_INPUT_DMN})
  @Test
  void testMultipleDecisionInputInstances() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("input1", "a");
    variables.put("input2", 1);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().includeInputs().singleResult();
    List<HistoricDecisionInputInstance> inputs = historicDecisionInstance.getInputs();
    assertThat(inputs).hasSize(2);

    assertThat(inputs.get(0).getValue()).isEqualTo("a");
    assertThat(inputs.get(1).getValue()).isEqualTo(1);
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDisableDecisionInputInstanceByteValue() {

    byte[] bytes = "object".getBytes();
    startProcessInstanceAndEvaluateDecision(bytes);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().includeInputs().disableBinaryFetching().singleResult();
    List<HistoricDecisionInputInstance> inputs = historicDecisionInstance.getInputs();
    assertThat(inputs).hasSize(1);

    HistoricDecisionInputInstance input = inputs.get(0);
    assertThat(input.getTypeName()).isEqualTo("bytes");
    assertThat(input.getValue()).isNull();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDecisionOutputInstanceProperties() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().includeOutputs().singleResult();
    List<HistoricDecisionOutputInstance> outputs = historicDecisionInstance.getOutputs();
    assertThat(outputs)
      .isNotNull()
      .hasSize(1);

    HistoricDecisionOutputInstance output = outputs.get(0);
    assertThat(output.getDecisionInstanceId()).isEqualTo(historicDecisionInstance.getId());
    assertThat(output.getClauseId()).isEqualTo("out");
    assertThat(output.getClauseName()).isEqualTo("output");

    assertThat(output.getRuleId()).isEqualTo("rule");
    assertThat(output.getRuleOrder()).isEqualTo(1);

    assertThat(output.getVariableName()).isEqualTo("result");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_MULTIPLE_OUTPUT_DMN})
  @Test
  void testMultipleDecisionOutputInstances() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().includeOutputs().singleResult();
    List<HistoricDecisionOutputInstance> outputs = historicDecisionInstance.getOutputs();
    assertThat(outputs).hasSize(2);

    HistoricDecisionOutputInstance firstOutput = outputs.get(0);
    assertThat(firstOutput.getClauseId()).isEqualTo("out1");
    assertThat(firstOutput.getRuleId()).isEqualTo("rule1");
    assertThat(firstOutput.getRuleOrder()).isEqualTo(1);
    assertThat(firstOutput.getVariableName()).isEqualTo("result1");
    assertThat(firstOutput.getValue()).isEqualTo("okay");

    HistoricDecisionOutputInstance secondOutput = outputs.get(1);
    assertThat(secondOutput.getClauseId()).isEqualTo("out1");
    assertThat(secondOutput.getRuleId()).isEqualTo("rule2");
    assertThat(secondOutput.getRuleOrder()).isEqualTo(2);
    assertThat(secondOutput.getVariableName()).isEqualTo("result1");
    assertThat(secondOutput.getValue()).isEqualTo("not okay");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_COMPOUND_OUTPUT_DMN})
  @Test
  void testCompoundDecisionOutputInstances() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().includeOutputs().singleResult();
    List<HistoricDecisionOutputInstance> outputs = historicDecisionInstance.getOutputs();
    assertThat(outputs).hasSize(2);

    HistoricDecisionOutputInstance firstOutput = outputs.get(0);
    assertThat(firstOutput.getClauseId()).isEqualTo("out1");
    assertThat(firstOutput.getRuleId()).isEqualTo("rule1");
    assertThat(firstOutput.getRuleOrder()).isEqualTo(1);
    assertThat(firstOutput.getVariableName()).isEqualTo("result1");
    assertThat(firstOutput.getValue()).isEqualTo("okay");

    HistoricDecisionOutputInstance secondOutput = outputs.get(1);
    assertThat(secondOutput.getClauseId()).isEqualTo("out2");
    assertThat(secondOutput.getRuleId()).isEqualTo("rule1");
    assertThat(secondOutput.getRuleOrder()).isEqualTo(1);
    assertThat(secondOutput.getVariableName()).isEqualTo("result2");
    assertThat(secondOutput.getValue()).isEqualTo("not okay");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_COLLECT_SUM_DMN})
  @Test
  void testCollectResultValue() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().singleResult();

    assertThat(historicDecisionInstance.getCollectResultValue()).isNotNull();
    assertThat(historicDecisionInstance.getCollectResultValue()).isEqualTo(3.0);
  }

  @Deployment(resources = DECISION_LITERAL_EXPRESSION_DMN)
  @Test
  void testDecisionInstancePropertiesOfDecisionLiteralExpression() {
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();

    decisionService.evaluateDecisionByKey("decision")
      .variables(Variables.createVariables().putValue("sum", 2205))
      .evaluate();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().includeInputs().includeOutputs();
    assertThat(query.count()).isEqualTo(1L);

    HistoricDecisionInstance historicDecisionInstance = query.singleResult();

    assertThat(historicDecisionInstance.getDecisionDefinitionId()).isEqualTo(decisionDefinition.getId());
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo("decision");
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("Decision with Literal Expression");
    assertThat(historicDecisionInstance.getEvaluationTime()).isNotNull();

    assertThat(historicDecisionInstance.getInputs()).isEmpty();

    List<HistoricDecisionOutputInstance> outputs = historicDecisionInstance.getOutputs();
    assertThat(outputs).hasSize(1);

    HistoricDecisionOutputInstance output = outputs.get(0);
    assertThat(output.getVariableName()).isEqualTo("result");
    assertThat(output.getTypeName()).isEqualTo("string");
    assertThat((String) output.getValue()).isEqualTo("ok");

    assertThat(output.getClauseId()).isNull();
    assertThat(output.getClauseName()).isNull();
    assertThat(output.getRuleId()).isNull();
    assertThat(output.getRuleOrder()).isNull();
  }

  @Deployment(resources = DRG_DMN)
  @Test
  void testDecisionInstancePropertiesOfDrdDecision() {

    decisionService.evaluateDecisionTableByKey("dish-decision")
      .variables(Variables.createVariables().putValue("temperature", 21).putValue("dayType", "Weekend"))
      .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.count()).isEqualTo(3L);

    HistoricDecisionInstance rootHistoricDecisionInstance = query.decisionDefinitionKey("dish-decision").singleResult();
    HistoricDecisionInstance requiredHistoricDecisionInstance1 = query.decisionDefinitionKey("season").singleResult();
    HistoricDecisionInstance requiredHistoricDecisionInstance2 = query.decisionDefinitionKey("guestCount").singleResult();

    assertThat(rootHistoricDecisionInstance.getRootDecisionInstanceId()).isNull();
    assertThat(rootHistoricDecisionInstance.getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinition.getId());
    assertThat(rootHistoricDecisionInstance.getDecisionRequirementsDefinitionKey()).isEqualTo(decisionRequirementsDefinition.getKey());

    assertThat(requiredHistoricDecisionInstance1.getRootDecisionInstanceId()).isEqualTo(rootHistoricDecisionInstance.getId());
    assertThat(requiredHistoricDecisionInstance1.getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinition.getId());
    assertThat(requiredHistoricDecisionInstance1.getDecisionRequirementsDefinitionKey()).isEqualTo(decisionRequirementsDefinition.getKey());

    assertThat(requiredHistoricDecisionInstance2.getRootDecisionInstanceId()).isEqualTo(rootHistoricDecisionInstance.getId());
    assertThat(requiredHistoricDecisionInstance2.getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinition.getId());
    assertThat(requiredHistoricDecisionInstance2.getDecisionRequirementsDefinitionKey()).isEqualTo(decisionRequirementsDefinition.getKey());
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDeleteHistoricDecisionInstances() {
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION_DEFINITION_KEY);

    startProcessInstanceAndEvaluateDecision();

    assertThat(query.count()).isEqualTo(1L);

    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();
    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinition.getId());

    assertThat(query.count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDeleteHistoricDecisionInstanceByInstanceId() {

    // given
    startProcessInstanceAndEvaluateDecision();
    HistoricDecisionInstanceQuery query =
        historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION_DEFINITION_KEY);

    assertThat(query.count()).isEqualTo(1L);
    HistoricDecisionInstance historicDecisionInstance = query.includeInputs().includeOutputs().singleResult();

    // when
    historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstance.getId());

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void testDeleteHistoricDecisionInstanceByUndeployment() {
    String firstDeploymentId = repositoryService.createDeployment()
      .addClasspathResource(DECISION_PROCESS)
      .addClasspathResource(DECISION_SINGLE_OUTPUT_DMN)
      .deploy().getId();

    startProcessInstanceAndEvaluateDecision();

    String secondDeploymentId = repositoryService.createDeployment()
        .addClasspathResource(DECISION_PROCESS)
        .addClasspathResource(DECISION_MULTIPLE_OUTPUT_DMN)
        .deploy().getId();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    assertThat(query.count()).isEqualTo(1L);

    repositoryService.deleteDeployment(firstDeploymentId, true);
    assertThat(query.count()).isZero();
  }

  @Deployment(resources = {DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDecisionEvaluatedWithDecisionService() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("input1", "test");
    decisionService.evaluateDecisionTableByKey(DECISION_DEFINITION_KEY, variables);

    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(DECISION_DEFINITION_KEY).singleResult().getId();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    assertThat(historicDecisionInstance.getDecisionDefinitionId()).isEqualTo(decisionDefinitionId);
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("sample decision");

    assertThat(historicDecisionInstance.getEvaluationTime()).isNotNull();
    // references to process instance should be null since the decision is not evaluated while executing a process instance
    assertThat(historicDecisionInstance.getProcessDefinitionKey()).isNull();
    assertThat(historicDecisionInstance.getProcessDefinitionId()).isNull();
    assertThat(historicDecisionInstance.getProcessInstanceId()).isNull();
    assertThat(historicDecisionInstance.getActivityId()).isNull();
    assertThat(historicDecisionInstance.getActivityInstanceId()).isNull();
    // the user should be null since no user was authenticated during evaluation
    assertThat(historicDecisionInstance.getUserId()).isNull();
  }

  @Deployment(resources = {DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDecisionEvaluatedWithAuthenticatedUser() {
    identityService.setAuthenticatedUserId("demo");
    VariableMap variables = Variables.putValue("input1", "test");
    decisionService.evaluateDecisionTableByKey(DECISION_DEFINITION_KEY, variables);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    // the user should be set since the decision was evaluated with the decision service
    assertThat(historicDecisionInstance.getUserId()).isEqualTo("demo");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDecisionEvaluatedWithAuthenticatedUserFromProcess() {
    identityService.setAuthenticatedUserId("demo");
    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    // the user should be null since the decision was evaluated by the process
    assertThat(historicDecisionInstance.getUserId()).isNull();
  }

  @Deployment(resources = {DECISION_CASE_WITH_DECISION_SERVICE, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDecisionEvaluatedWithAuthenticatedUserFromCase() {
    identityService.setAuthenticatedUserId("demo");
    createCaseInstanceAndEvaluateDecision();

    HistoricDecisionInstance historicDecisionInstance = historyService
        .createHistoricDecisionInstanceQuery()
        .singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    // the user should be null since decision was evaluated by the case
    assertThat(historicDecisionInstance.getUserId()).isNull();
  }

  @Deployment(resources = {DECISION_CASE_WITH_DECISION_SERVICE, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testCaseDecisionEvaluatedWithDecisionServiceInsideDelegate() {

    CaseInstance caseInstance = createCaseInstanceAndEvaluateDecision();

    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionId(caseInstance.getCaseDefinitionId())
        .singleResult();

    String decisionDefinitionId = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .singleResult()
        .getId();

    String activityInstanceId = historyService
        .createHistoricCaseActivityInstanceQuery()
        .caseActivityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    HistoricDecisionInstance historicDecisionInstance = historyService
        .createHistoricDecisionInstanceQuery()
        .singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    assertThat(historicDecisionInstance.getDecisionDefinitionId()).isEqualTo(decisionDefinitionId);
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("sample decision");

    // references to case instance should be set since the decision is evaluated while executing a case instance
    assertThat(historicDecisionInstance.getProcessDefinitionKey()).isNull();
    assertThat(historicDecisionInstance.getProcessDefinitionId()).isNull();
    assertThat(historicDecisionInstance.getProcessInstanceId()).isNull();
    assertThat(historicDecisionInstance.getCaseDefinitionKey()).isEqualTo(caseDefinition.getKey());
    assertThat(historicDecisionInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
    assertThat(historicDecisionInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(historicDecisionInstance.getActivityId()).isEqualTo("PI_HumanTask_1");
    assertThat(historicDecisionInstance.getActivityInstanceId()).isEqualTo(activityInstanceId);
    assertThat(historicDecisionInstance.getEvaluationTime()).isNotNull();
  }

  @Deployment(resources = {DECISION_CASE_WITH_DECISION_SERVICE_INSIDE_RULE, DECISION_RETURNS_TRUE})
  @Test
  void testManualActivationRuleEvaluatesDecision() {

    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("input1", null)
        .setVariable("myBean", new DecisionServiceDelegate())
        .create();

    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionId(caseInstance.getCaseDefinitionId())
        .singleResult();

    String decisionDefinitionId = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .singleResult()
        .getId();

    String activityInstanceId = historyService
        .createHistoricCaseActivityInstanceQuery()
        .caseActivityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    HistoricDecisionInstance historicDecisionInstance = historyService
        .createHistoricDecisionInstanceQuery()
        .singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    assertThat(historicDecisionInstance.getDecisionDefinitionId()).isEqualTo(decisionDefinitionId);
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("sample decision");

    // references to case instance should be set since the decision is evaluated while executing a case instance
    assertThat(historicDecisionInstance.getProcessDefinitionKey()).isNull();
    assertThat(historicDecisionInstance.getProcessDefinitionId()).isNull();
    assertThat(historicDecisionInstance.getProcessInstanceId()).isNull();
    assertThat(historicDecisionInstance.getCaseDefinitionKey()).isEqualTo(caseDefinition.getKey());
    assertThat(historicDecisionInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
    assertThat(historicDecisionInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(historicDecisionInstance.getActivityId()).isEqualTo("PI_HumanTask_1");
    assertThat(historicDecisionInstance.getActivityInstanceId()).isEqualTo(activityInstanceId);
    assertThat(historicDecisionInstance.getEvaluationTime()).isNotNull();
  }

  @Deployment(resources = {DECISION_CASE_WITH_DECISION_SERVICE_INSIDE_IF_PART, DECISION_RETURNS_TRUE})
  @Test
  void testIfPartEvaluatesDecision() {

    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("input1", null)
        .setVariable("myBean", new DecisionServiceDelegate())
        .create();

    String humanTask1 = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();
    caseService.completeCaseExecution(humanTask1);

    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionId(caseInstance.getCaseDefinitionId())
        .singleResult();

    String decisionDefinitionId = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .singleResult()
        .getId();

    String activityInstanceId = historyService
        .createHistoricCaseActivityInstanceQuery()
        .caseActivityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    HistoricDecisionInstance historicDecisionInstance = historyService
        .createHistoricDecisionInstanceQuery()
        .singleResult();

    assertThat(historicDecisionInstance).isNotNull();
    assertThat(historicDecisionInstance.getDecisionDefinitionId()).isEqualTo(decisionDefinitionId);
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("sample decision");

    // references to case instance should be set since the decision is evaluated while executing a case instance
    assertThat(historicDecisionInstance.getProcessDefinitionKey()).isNull();
    assertThat(historicDecisionInstance.getProcessDefinitionId()).isNull();
    assertThat(historicDecisionInstance.getProcessInstanceId()).isNull();
    assertThat(historicDecisionInstance.getCaseDefinitionKey()).isEqualTo(caseDefinition.getKey());
    assertThat(historicDecisionInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
    assertThat(historicDecisionInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(historicDecisionInstance.getActivityId()).isEqualTo("PI_HumanTask_1");
    assertThat(historicDecisionInstance.getActivityInstanceId()).isEqualTo(activityInstanceId);
    assertThat(historicDecisionInstance.getEvaluationTime()).isNotNull();
  }

  @Test
  void testTableNames() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    assertThat(managementService.getTableName(HistoricDecisionInstance.class)).isEqualTo(tablePrefix +"ACT_HI_DECINST");

    assertThat(managementService.getTableName(HistoricDecisionInstanceEntity.class)).isEqualTo(tablePrefix + "ACT_HI_DECINST");
  }

  protected ProcessInstance startProcessInstanceAndEvaluateDecision() {
    return startProcessInstanceAndEvaluateDecision(null);
  }

  protected ProcessInstance startProcessInstanceAndEvaluateDecision(Object input) {
    return runtimeService.startProcessInstanceByKey("testProcess", getVariables(input));
  }

  protected CaseInstance createCaseInstanceAndEvaluateDecision() {
    return caseService
        .withCaseDefinitionByKey("case")
        .setVariables(getVariables("test"))
        .create();
  }

  protected VariableMap getVariables(Object input) {
    VariableMap variables = Variables.createVariables();
    variables.put("input1", input);
    return variables;
  }

  /**
   * Use between two rule evaluations to ensure the expected order by evaluation time.
   */
  protected void waitASignificantAmountOfTime() {
    DateTime now = new DateTime(ClockUtil.getCurrentTime());
    ClockUtil.setCurrentTime(now.plusSeconds(10).toDate());
  }

}
