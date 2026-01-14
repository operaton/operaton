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
package org.operaton.bpm.dmn.engine.transform;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.impl.*;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.FirstHitPolicyHandler;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.UniqueHitPolicyHandler;
import org.operaton.bpm.dmn.engine.impl.transform.DmnTransformException;
import org.operaton.bpm.dmn.engine.impl.type.DefaultTypeDefinition;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.commons.utils.IoUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmnTransformTest extends DmnEngineTest {

  private static final String TRANSFORM_DMN = "org/operaton/bpm/dmn/engine/transform/DmnTransformTest.dmn";

  private static final String DECISION_WITH_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/dmn/engine/transform/DecisionWithLiteralExpression.dmn";

  private static final String REQUIRED_DECISIONS_DMN = "org/operaton/bpm/dmn/engine/api/RequiredDecision.dmn";
  private static final String MULTIPLE_REQUIRED_DECISIONS_DMN = "org/operaton/bpm/dmn/engine/api/MultipleRequiredDecisions.dmn";
  private static final String MULTI_LEVEL_MULTIPLE_REQUIRED_DECISIONS_DMN = "org/operaton/bpm/dmn/engine/api/MultilevelMultipleRequiredDecisions.dmn";
  private static final String LOOP_REQUIRED_DECISIONS_DMN = "org/operaton/bpm/dmn/engine/api/LoopInRequiredDecision.dmn";
  private static final String LOOP_REQUIRED_DECISIONS_DIFFERENT_ORDER_DMN = "org/operaton/bpm/dmn/engine/api/LoopInRequiredDecision2.dmn";
  private static final String SELF_REQUIRED_DECISIONS_DMN = "org/operaton/bpm/dmn/engine/api/SelfRequiredDecision.dmn";

  @Test
  void shouldTransformDecisions() {
    List<DmnDecision> decisions = parseDecisionsFromFile(TRANSFORM_DMN);
    assertThat(decisions).hasSize(2);

    DmnDecision decision = decisions.get(0);
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo("decision1");
    assertThat(decision.getName()).isEqualTo("operaton");

    // decision2 should be ignored as it isn't supported by the DMN engine

    decision = decisions.get(1);
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo("decision3");
    assertThat(decision.getName()).isEqualTo("operaton");
  }

  @Test
  void shouldTransformDecisionTables() {
    List<DmnDecision> decisions = parseDecisionsFromFile(TRANSFORM_DMN);
    DmnDecision decision = decisions.get(0);
    assertThat(decision.isDecisionTable()).isTrue();
    assertThat(decision).isInstanceOf(DmnDecisionImpl.class);

    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
    assertThat(decisionTable.getHitPolicyHandler()).isInstanceOf(UniqueHitPolicyHandler.class);

    decision = decisions.get(1);
    assertThat(decision.isDecisionTable()).isTrue();
    assertThat(decision).isInstanceOf(DmnDecisionImpl.class);

    decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
    assertThat(decisionTable.getHitPolicyHandler()).isInstanceOf(FirstHitPolicyHandler.class);
  }

  @Test
  void shouldTransformInputs() {
    DmnDecisionImpl decisionEntity = (DmnDecisionImpl) parseDecisionFromFile("decision1", TRANSFORM_DMN);
    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionEntity.getDecisionLogic();
    List<DmnDecisionTableInputImpl> inputs = decisionTable.getInputs();
    assertThat(inputs).hasSize(2);

    DmnDecisionTableInputImpl input = inputs.get(0);
    assertThat(input.getId()).isEqualTo("input1");
    assertThat(input.getName()).isEqualTo("operaton");
    assertThat(input.getInputVariable()).isEqualTo("operaton");

    DmnExpressionImpl inputExpression = input.getExpression();
    assertThat(inputExpression).isNotNull();
    assertThat(inputExpression.getId()).isEqualTo("inputExpression");
    assertThat(inputExpression.getName()).isNull();
    assertThat(inputExpression.getExpressionLanguage()).isEqualTo("operaton");
    assertThat(inputExpression.getExpression()).isEqualTo("operaton");

    assertThat(inputExpression.getTypeDefinition()).isNotNull();
    assertThat(inputExpression.getTypeDefinition().getTypeName()).isEqualTo("string");

    input = inputs.get(1);
    assertThat(input.getId()).isEqualTo("input2");
    assertThat(input.getName()).isNull();
    assertThat(input.getInputVariable()).isEqualTo("cellInput");

    inputExpression = input.getExpression();
    assertThat(inputExpression).isNotNull();
    assertThat(inputExpression.getId()).isNull();
    assertThat(inputExpression.getName()).isNull();
    assertThat(inputExpression.getExpressionLanguage()).isNull();
    assertThat(inputExpression.getExpression()).isNull();

    assertThat(inputExpression.getTypeDefinition()).isNotNull();
    assertThat(inputExpression.getTypeDefinition()).isEqualTo(new DefaultTypeDefinition());
  }

  @Test
  void shouldTransformOutputs() {
    DmnDecisionImpl decisionEntity = (DmnDecisionImpl) parseDecisionFromFile("decision1", TRANSFORM_DMN);
    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionEntity.getDecisionLogic();
    List<DmnDecisionTableOutputImpl> outputs = decisionTable.getOutputs();
    assertThat(outputs).hasSize(2);

    DmnDecisionTableOutputImpl output = outputs.get(0);
    assertThat(output.getId()).isEqualTo("output1");
    assertThat(output.getName()).isEqualTo("operaton");
    assertThat(output.getOutputName()).isEqualTo("operaton");
    assertThat(output.getTypeDefinition()).isNotNull();
    assertThat(output.getTypeDefinition().getTypeName()).isEqualTo("string");

    output = outputs.get(1);
    assertThat(output.getId()).isEqualTo("output2");
    assertThat(output.getName()).isNull();
    assertThat(output.getOutputName()).isEqualTo("out2");
    assertThat(output.getTypeDefinition()).isNotNull();
    assertThat(output.getTypeDefinition()).isEqualTo(new DefaultTypeDefinition());
  }

  @Test
  void shouldTransformRules() {
    DmnDecisionImpl decisionEntity = (DmnDecisionImpl) parseDecisionFromFile("decision1", TRANSFORM_DMN);
    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionEntity.getDecisionLogic();
    List<DmnDecisionTableRuleImpl> rules = decisionTable.getRules();
    assertThat(rules).hasSize(1);

    DmnDecisionTableRuleImpl rule = rules.get(0);

    List<DmnExpressionImpl> conditions = rule.getConditions();
    assertThat(conditions).hasSize(2);

    DmnExpressionImpl condition = conditions.get(0);
    assertThat(condition.getId()).isEqualTo("inputEntry");
    assertThat(condition.getName()).isEqualTo("operaton");
    assertThat(condition.getExpressionLanguage()).isEqualTo("operaton");
    assertThat(condition.getExpression()).isEqualTo("operaton");

    condition = conditions.get(1);
    assertThat(condition.getId()).isNull();
    assertThat(condition.getName()).isNull();
    assertThat(condition.getExpressionLanguage()).isNull();
    assertThat(condition.getExpression()).isNull();

    List<DmnExpressionImpl> conclusions = rule.getConclusions();
    assertThat(conclusions).hasSize(2);

    DmnExpressionImpl dmnOutputEntry = conclusions.get(0);
    assertThat(dmnOutputEntry.getId()).isEqualTo("outputEntry");
    assertThat(dmnOutputEntry.getName()).isEqualTo("operaton");
    assertThat(dmnOutputEntry.getExpressionLanguage()).isEqualTo("operaton");
    assertThat(dmnOutputEntry.getExpression()).isEqualTo("operaton");

    dmnOutputEntry = conclusions.get(1);
    assertThat(dmnOutputEntry.getId()).isNull();
    assertThat(dmnOutputEntry.getName()).isNull();
    assertThat(dmnOutputEntry.getExpressionLanguage()).isNull();
    assertThat(dmnOutputEntry.getExpression()).isNull();
  }

  @Test
  void shouldTransformDecisionWithLiteralExpression() {
    List<DmnDecision> decisions = parseDecisionsFromFile(DECISION_WITH_LITERAL_EXPRESSION_DMN);
    assertThat(decisions).hasSize(1);

    DmnDecision decision = decisions.get(0);
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo("decision");
    assertThat(decision.getName()).isEqualTo("Decision");

    assertThat(decision.getDecisionLogic())
      .isNotNull()
      .isInstanceOf(DmnDecisionLiteralExpressionImpl.class);

    DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression = (DmnDecisionLiteralExpressionImpl) decision.getDecisionLogic();

    DmnVariableImpl variable = dmnDecisionLiteralExpression.getVariable();
    assertThat(variable).isNotNull();
    assertThat(variable.getId()).isEqualTo("v1");
    assertThat(variable.getName()).isEqualTo("c");
    assertThat(variable.getTypeDefinition()).isNotNull();
    assertThat(variable.getTypeDefinition().getTypeName()).isEqualTo("integer");

    DmnExpressionImpl dmnExpression = dmnDecisionLiteralExpression.getExpression();
    assertThat(dmnExpression).isNotNull();
    assertThat(dmnExpression.getId()).isEqualTo("e1");
    assertThat(dmnExpression.getExpressionLanguage()).isEqualTo("groovy");
    assertThat(dmnExpression.getExpression()).isEqualTo("a + b");
    assertThat(dmnExpression.getTypeDefinition()).isNull();
  }

  @Test
  void shouldParseDecisionWithRequiredDecisions() {
    InputStream inputStream = IoUtil.fileAsStream(REQUIRED_DECISIONS_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    DmnDecision buyProductDecision = dmnEngine.parseDecision("buyProduct", modelInstance);
    assertDecision(buyProductDecision, "buyProduct");

    Collection<DmnDecision> buyProductrequiredDecisions = buyProductDecision.getRequiredDecisions();
    assertThat(buyProductrequiredDecisions).hasSize(1);

    DmnDecision buyComputerDecision = getDecision(buyProductrequiredDecisions, "buyComputer");
    assertThat(buyComputerDecision).isNotNull();

    Collection<DmnDecision> buyComputerRequiredDecision = buyComputerDecision.getRequiredDecisions();
    assertThat(buyComputerRequiredDecision).hasSize(1);

    DmnDecision buyElectronicDecision = getDecision(buyComputerRequiredDecision, "buyElectronic");
    assertThat(buyElectronicDecision).isNotNull();

    assertThat(buyElectronicDecision.getRequiredDecisions()).isEmpty();
  }

  @Test
  void shouldParseDecisionsWithRequiredDecisions() {
    InputStream inputStream = IoUtil.fileAsStream(REQUIRED_DECISIONS_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    List<DmnDecision> decisions = dmnEngine.parseDecisions(modelInstance);

    DmnDecision buyProductDecision = getDecision(decisions, "buyProduct");
    assertThat(buyProductDecision).isNotNull();
    Collection<DmnDecision> requiredProductDecisions = buyProductDecision.getRequiredDecisions();
    assertThat(requiredProductDecisions).hasSize(1);

    DmnDecision requiredProductDecision = getDecision(requiredProductDecisions, "buyComputer");
    assertThat(requiredProductDecision).isNotNull();

    DmnDecision buyComputerDecision = getDecision(decisions, "buyComputer");
    assertThat(buyComputerDecision).isNotNull();
    Collection<DmnDecision> buyComputerRequiredDecisions = buyComputerDecision.getRequiredDecisions();
    assertThat(buyComputerRequiredDecisions).hasSize(1);

    DmnDecision buyComputerRequiredDecision = getDecision(buyComputerRequiredDecisions, "buyElectronic");
    assertThat(buyComputerRequiredDecision).isNotNull();

    DmnDecision buyElectronicDecision = getDecision(decisions, "buyElectronic");
    assertThat(buyElectronicDecision).isNotNull();

    Collection<DmnDecision> buyElectronicRequiredDecisions = buyElectronicDecision.getRequiredDecisions();
    assertThat(buyElectronicRequiredDecisions).isEmpty();
  }

  @Test
  void shouldParseDecisionWithMultipleRequiredDecisions() {
    InputStream inputStream = IoUtil.fileAsStream(MULTIPLE_REQUIRED_DECISIONS_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);
    DmnDecision decision = dmnEngine.parseDecision("car",modelInstance);
    Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
    assertThat(requiredDecisions).hasSize(2);

    DmnDecision carPriceDecision = getDecision(requiredDecisions, "carPrice");
    assertThat(carPriceDecision).isNotNull();

    DmnDecision carSpeedDecision = getDecision(requiredDecisions, "carSpeed");
    assertThat(carSpeedDecision).isNotNull();
  }

  @Test
  void shouldDetectLoopInParseDecisionWithRequiredDecision() {
    InputStream inputStream = IoUtil.fileAsStream(LOOP_REQUIRED_DECISIONS_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    assertThatThrownBy(() -> decision = dmnEngine.parseDecision("buyProduct", modelInstance))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02015")
      .hasMessageContaining("has a loop");
  }

  @Test
  void shouldDetectLoopInParseDecisionWithRequiredDecisionOfDifferentOrder() {
    InputStream inputStream = IoUtil.fileAsStream(LOOP_REQUIRED_DECISIONS_DIFFERENT_ORDER_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    assertThatThrownBy(() -> dmnEngine.parseDecisions(modelInstance))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02015")
      .hasMessageContaining("has a loop");
  }

  @Test
  void shouldDetectLoopInParseDecisionWithSelfRequiredDecision() {
    InputStream inputStream = IoUtil.fileAsStream(SELF_REQUIRED_DECISIONS_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    assertThatThrownBy(() -> decision = dmnEngine.parseDecision("buyProduct", modelInstance))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02015")
      .hasMessageContaining("has a loop");
  }

  @Test
  void shouldNotDetectLoopInMultiLevelDecisionWithMultipleRequiredDecision() {
    InputStream inputStream = IoUtil.fileAsStream(MULTI_LEVEL_MULTIPLE_REQUIRED_DECISIONS_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    List<DmnDecision> decisions = dmnEngine.parseDecisions(modelInstance);
    assertThat(decisions).hasSize(8);
  }

  @Test
  void shouldTransformDecisionRequirementsGraph() {
    InputStream inputStream = IoUtil.fileAsStream(REQUIRED_DECISIONS_DMN);
    DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

    assertThat(drg).isNotNull();
    assertThat(drg.getKey()).isEqualTo("buy-decision");
    assertThat(drg.getName()).isEqualTo("Buy Decision");
    assertThat(drg.getDecisionKeys())
      .hasSize(3)
      .contains("buyProduct", "buyComputer", "buyElectronic");

    Collection<DmnDecision> decisions = drg.getDecisions();
    assertThat(decisions)
      .hasSize(3)
      .extracting("key")
      .contains("buyProduct", "buyComputer", "buyElectronic");
  }

  protected void assertDecision(DmnDecision decision, String key) {
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo(key);
  }

  protected DmnDecision getDecision(Collection<DmnDecision> decisions, String key) {
    for(DmnDecision decision: decisions) {
      if(decision.getKey().equals(key)) {
        return decision;
      }
    }
    return null;
  }
}
