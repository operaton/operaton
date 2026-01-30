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
package org.operaton.bpm.engine.test.cmmn.decisiontask;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.exception.dmn.DecisionDefinitionNotFoundException;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class DmnDecisionTaskTest extends CmmnTest {

  static final String CMMN_CALL_DECISION_CONSTANT = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionAsConstant.cmmn";
  static final String CMMN_CALL_DECISION_CONSTANT_WITH_MANUAL_ACTIVATION = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionAsConstantWithManualActiovation.cmmn";
  static final String CMMN_CALL_DECISION_EXPRESSION = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionAsExpressionStartsWithDollar.cmmn";
  static final String CMMN_CALL_DECISION_EXPRESSION_WITH_MANUAL_ACTIVATION = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionAsExpressionStartsWithDollarWithManualActiovation.cmmn";

  static final String DECISION_OKAY_DMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testDecisionOkay.dmn11.xml";
  static final String DECISION_NOT_OKAY_DMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testDecisionNotOkay.dmn11.xml";
  static final String DECISION_POJO_DMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testPojo.dmn11.xml";

  static final String DECISION_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/engine/test/dmn/deployment/DecisionWithLiteralExpression.dmn";
  static final String DRD_DISH_RESOURCE = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  static final String CASE_KEY = "case";
  static final String DECISION_TASK = "PI_DecisionTask_1";

  @Deployment(resources = {CMMN_CALL_DECISION_CONSTANT, DECISION_OKAY_DMN})
  @Test
  void testCallDecisionAsConstant() {
    // given
    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY);

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo("okay");
  }

  @Deployment(resources = {
      CMMN_CALL_DECISION_EXPRESSION,
      DECISION_OKAY_DMN
  })
  @Test
  void testCallDecisionAsExpressionStartsWithDollar() {
    // given
    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY, Variables.createVariables().putValue("testDecision", "testDecision"));

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo("okay");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionAsExpressionStartsWithHash.cmmn",
      DECISION_OKAY_DMN
  })
  @Test
  void testCallDecisionAsExpressionStartsWithHash() {
    // given
    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY, Variables.createVariables().putValue("testDecision", "testDecision"));

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo("okay");
  }

  @ParameterizedTest
  @CsvSource({
      "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallLatestDecision.cmmn, not okay",
      "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionByDeployment.cmmn, okay",
      "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionByVersion.cmmn, not okay"
  })
  void callShouldHaveExpectedResult(String cmmnResource, String expectedResult) {
    // given
    testRule.deploy(cmmnResource, DECISION_OKAY_DMN);

    String deploymentId = repositoryService.createDeployment()
        .addClasspathResource(DECISION_NOT_OKAY_DMN)
        .deploy()
        .getId();

    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY);

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo(expectedResult);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionByVersionAsExpressionStartsWithDollar.cmmn",
      DECISION_OKAY_DMN
  })
  @Test
  void testCallDecisionByVersionAsExpressionStartsWithDollar() {
    // given
    String deploymentId = repositoryService.createDeployment()
        .addClasspathResource(DECISION_NOT_OKAY_DMN)
        .deploy()
        .getId();

    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY, Variables.createVariables().putValue("myVersion", 2));

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo("not okay");

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskTest.testCallDecisionByVersionAsExpressionStartsWithHash.cmmn",
      DECISION_OKAY_DMN
  })
  @Test
  void testCallDecisionByVersionAsExpressionStartsWithHash() {
    // given
    String deploymentId = repositoryService.createDeployment()
        .addClasspathResource(DECISION_NOT_OKAY_DMN)
        .deploy()
        .getId();

    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY, Variables.createVariables().putValue("myVersion", 2));

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo("not okay");

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = CMMN_CALL_DECISION_CONSTANT_WITH_MANUAL_ACTIVATION)
  @Test
  void testDecisionNotFound() {
    // given
    createCaseInstanceByKey(CASE_KEY);
    String decisionTaskId = queryCaseExecutionByActivityId(DECISION_TASK).getId();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(decisionTaskId);

    // when/then
    assertThatThrownBy(caseExecutionCommandBuilder::manualStart)
      .isInstanceOf(DecisionDefinitionNotFoundException.class)
      .hasMessageContaining("no decision definition deployed with key 'testDecision'");
  }

  @Deployment(resources = {
      CMMN_CALL_DECISION_CONSTANT,
      DECISION_POJO_DMN
  })
  @Test
  void testPojo() {
    // given
    VariableMap variables = Variables.createVariables()
      .putValue("pojo", new TestPojo("okay", 13.37));
    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY, variables);

    assertThat(getDecisionResult(caseInstance)).isEqualTo("okay");
  }

  @Deployment(resources = {CMMN_CALL_DECISION_CONSTANT, DECISION_OKAY_DMN})
  @Test
  void testIgnoreNonBlockingFlag() {
    // given
    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY);

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo("okay");
  }

  @Deployment(resources = {CMMN_CALL_DECISION_EXPRESSION_WITH_MANUAL_ACTIVATION, DECISION_LITERAL_EXPRESSION_DMN})
  @Test
  void testCallDecisionWithLiteralExpression() {
    // given
    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY, Variables.createVariables()
        .putValue("testDecision", "decisionLiteralExpression")
        .putValue("a", 2)
        .putValue("b", 3));

    String decisionTaskId = queryCaseExecutionByActivityId(DECISION_TASK).getId();

    // when
    caseService
      .withCaseExecution(decisionTaskId)
      .manualStart();

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo(5);
  }

  @Deployment(resources = {CMMN_CALL_DECISION_EXPRESSION, DRD_DISH_RESOURCE})
  @Test
  void testCallDecisionWithRequiredDecisions() {
    // given
    CaseInstance caseInstance = createCaseInstanceByKey(CASE_KEY, Variables.createVariables()
        .putValue("testDecision", "dish-decision")
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    // then
    assertThat(queryCaseExecutionByActivityId(DECISION_TASK)).isNull();
    assertThat(getDecisionResult(caseInstance)).isEqualTo("Light salad");
  }

  protected Object getDecisionResult(CaseInstance caseInstance) {
    return caseService.getVariable(caseInstance.getId(), "result");
  }

}
