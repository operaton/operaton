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
package org.operaton.bpm.engine.test.dmn.businessruletask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.dmn.DecisionDefinitionNotFoundException;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DmnBusinessRuleTaskTest {

  public static final String DECISION_PROCESS = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml";
  public static final String DECISION_PROCESS_EXPRESSION = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRefExpression.bpmn20.xml";
  public static final String DECISION_PROCESS_COMPOSITEEXPRESSION = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRefCompositeExpression.bpmn20.xml";
  public static final String DECISION_PROCESS_LATEST = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRefLatestBinding.bpmn20.xml";
  public static final String DECISION_PROCESS_DEPLOYMENT = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRefDeploymentBinding.bpmn20.xml";
  public static final String DECISION_PROCESS_VERSION = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRefVersionBinding.bpmn20.xml";
  public static final String DECISION_OKAY_DMN = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionOkay.dmn11.xml";
  public static final String DECISION_NOT_OKAY_DMN = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionNotOkay.dmn11.xml";
  public static final String DECISION_VERSION_TAG_OKAY_DMN = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionVersionTagOkay.dmn11.xml";
  public static final String DECISION_POJO_DMN = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testPojo.dmn11.xml";

  public static final String DECISION_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/engine/test/dmn/deployment/DecisionWithLiteralExpression.dmn";
  public static final String DRD_DISH_RESOURCE = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  public static final String DECISION_OKAY_DMN12 = "org/operaton/bpm/engine/test/dmn/businessruletask/dmn12/DmnBusinessRuleTaskTest.testDecisionOkay.dmn";

  public static final String DECISION_OKAY_DMN13 = "org/operaton/bpm/engine/test/dmn/businessruletask/dmn13/DmnBusinessRuleTaskTest.testDecisionOkay.dmn";

  public static final BpmnModelInstance BPMN_VERSION_TAG_BINDING = Bpmn.createExecutableProcess("process")
              .startEvent()
              .businessRuleTask()
                    .operatonDecisionRef("decision")
                    .operatonDecisionRefBinding("versionTag")
                    .operatonDecisionRefVersionTag("0.0.2")
                    .operatonMapDecisionResult("singleEntry")
                    .operatonResultVariable("result")
              .endEvent()
                    .operatonAsyncBefore()
              .done();

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineRule);

  RuntimeService runtimeService;

  @Deployment(resources = {DECISION_PROCESS, DECISION_PROCESS_EXPRESSION, DECISION_OKAY_DMN})
  @Test
  void decisionRef() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    assertThat(getDecisionResult(processInstance)).isEqualTo("okay");

    processInstance = startExpressionProcess("testDecision", 1);
    assertThat(getDecisionResult(processInstance)).isEqualTo("okay");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_PROCESS_EXPRESSION, DECISION_OKAY_DMN12})
  @Test
  void testDmn12Decision() {
    decisionRef();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_PROCESS_EXPRESSION, DECISION_OKAY_DMN13})
  @Test
  void testDmn13Decision() {
    decisionRef();
  }

  @Deployment(resources = DECISION_PROCESS)
  @Test
  void noDecisionFound() {

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      .isInstanceOf(DecisionDefinitionNotFoundException.class)
      .hasMessageContaining("no decision definition deployed with key 'testDecision'");
  }

  @Deployment(resources = DECISION_PROCESS_EXPRESSION)
  @Test
  void noDecisionFoundRefByExpression() {

    // when/then
    assertThatThrownBy(() -> startExpressionProcess("testDecision", 1))
      .isInstanceOf(DecisionDefinitionNotFoundException.class)
      .hasMessageContaining("no decision definition deployed with key = 'testDecision', version = '1' and tenant-id = 'null");
  }

  @Deployment(resources = {DECISION_PROCESS_LATEST, DECISION_OKAY_DMN})
  @Test
  void decisionRefLatestBinding() {
    testRule.deploy(DECISION_NOT_OKAY_DMN);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    assertThat(getDecisionResult(processInstance)).isEqualTo("not okay");
  }

  @Deployment(resources = {DECISION_PROCESS_DEPLOYMENT, DECISION_OKAY_DMN})
  @Test
  void decisionRefDeploymentBinding() {
    testRule.deploy(DECISION_NOT_OKAY_DMN);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    assertThat(getDecisionResult(processInstance)).isEqualTo("okay");
  }

  @Deployment(resources = {DECISION_PROCESS_VERSION, DECISION_PROCESS_EXPRESSION, DECISION_OKAY_DMN})
  @Test
  void decisionRefVersionBinding() {
    testRule.deploy(DECISION_NOT_OKAY_DMN);
    testRule.deploy(DECISION_OKAY_DMN);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
    assertThat(getDecisionResult(processInstance)).isEqualTo("not okay");

    processInstance = startExpressionProcess("testDecision", 2);
    assertThat(getDecisionResult(processInstance)).isEqualTo("not okay");
  }

  @Test
  void decisionRefVersionTagBinding() {
    // given
    testRule.deploy(DECISION_VERSION_TAG_OKAY_DMN);
    testRule.deploy(BPMN_VERSION_TAG_BINDING);

    // when
    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("process")
        .setVariable("status", "gold")
        .execute();

    // then
    assertThat(getDecisionResult(processInstance)).isEqualTo("A");
  }

  @Test
  void decisionRefVersionTagBindingExpression() {
    // given
    testRule.deploy(DECISION_VERSION_TAG_OKAY_DMN);
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("versionTag")
          .operatonDecisionRefVersionTag("${versionTagExpr}")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("result")
        .endEvent()
          .operatonAsyncBefore()
        .done());

    // when
    VariableMap variables = Variables.createVariables()
        .putValue("versionTagExpr", "0.0.2")
        .putValue("status", "gold");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    // then
    assertThat(getDecisionResult(processInstance)).isEqualTo("A");
  }

  @Test
  void decisionRefVersionTagBindingWithoutVersionTag() {

    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
        .operatonDecisionRef("testDecision")
        .operatonDecisionRefBinding("versionTag")
        .operatonMapDecisionResult("singleEntry")
        .operatonResultVariable("result")
        .endEvent()
        .operatonAsyncBefore()
        .done();

    // when/then
    assertThatThrownBy(() -> testRule.deploy(modelInstance))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Could not parse BPMN process.");
  }

  @Test
  void decisionRefVersionTagBindingNoneDecisionDefinition() {
    // given
    testRule.deploy(BPMN_VERSION_TAG_BINDING);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
      .isInstanceOf(DecisionDefinitionNotFoundException.class)
      .hasMessageContaining("no decision definition deployed with key = 'decision', versionTag = '0.0.2' and tenant-id = 'null'");
  }

  @Test
  void decisionRefVersionTagBindingTwoDecisionDefinitions() {
    // given
    testRule.deploy(DECISION_VERSION_TAG_OKAY_DMN);
    testRule.deploy(DECISION_VERSION_TAG_OKAY_DMN);
    testRule.deploy(BPMN_VERSION_TAG_BINDING);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Found more than one decision definition for key 'decision' and versionTag '0.0.2'");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_POJO_DMN})
  @Test
  void testPojo() {
    VariableMap variables = Variables.createVariables()
      .putValue("pojo", new TestPojo("okay", 13.37));
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess", variables);

    assertThat(getDecisionResult(processInstance)).isEqualTo("okay");
  }

  @Deployment(resources = DECISION_LITERAL_EXPRESSION_DMN)
  @Test
  void evaluateDecisionWithLiteralExpression() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decisionLiteralExpression")
          .operatonResultVariable("result")
          .operatonMapDecisionResult("singleEntry")
        .endEvent()
          .operatonAsyncBefore()
        .done());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", Variables.createVariables()
        .putValue("a", 2)
        .putValue("b", 3));

    assertThat(getDecisionResult(processInstance)).isEqualTo(5);
  }

  @Deployment(resources = DRD_DISH_RESOURCE)
  @Test
  void evaluateDecisionWithRequiredDecisions() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("dish-decision")
          .operatonResultVariable("result")
          .operatonMapDecisionResult("singleEntry")
        .endEvent()
          .operatonAsyncBefore()
        .done());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    assertThat(getDecisionResult(processInstance)).isEqualTo("Light salad");
  }

  @Deployment(resources = {DECISION_PROCESS_COMPOSITEEXPRESSION, DECISION_OKAY_DMN})
  @Test
  void decisionRefWithCompositeExpression() {
    VariableMap variables = Variables.createVariables()
      .putValue("version", 1);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcessCompositeExpression", variables);

    assertThat(getDecisionResult(processInstance)).isEqualTo("okay");
  }

  protected ProcessInstance startExpressionProcess(Object decisionKey, Object version) {
    VariableMap variables = Variables.createVariables()
        .putValue("decision", decisionKey)
        .putValue("version", version);
    return runtimeService.startProcessInstanceByKey("testProcessExpression", variables);
  }

  protected Object getDecisionResult(ProcessInstance processInstance) {
    // the single entry of the single result of the decision result is stored as process variable
    return runtimeService.getVariable(processInstance.getId(), "result");
  }

}
