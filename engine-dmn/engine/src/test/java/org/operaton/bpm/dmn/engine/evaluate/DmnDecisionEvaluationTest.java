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
package org.operaton.bpm.dmn.engine.evaluate;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.*;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnEvaluationException;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.commons.utils.IoUtil;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;
import static org.operaton.bpm.engine.variable.Variables.createVariables;

public class DmnDecisionEvaluationTest extends DmnEngineTest {

  public static final String DMN_MULTI_LEVEL_MULTIPLE_INPUT_SINGLE_OUTPUT = "org/operaton/bpm/dmn/engine/evaluate/EvaluateMultiLevelDecisionsWithMultipleInputAndSingleOutput.dmn";
  public static final String DMN_DECISIONS_WITH_MULTIPLE_MATCHING_RULES = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDecisionsWithMultipleMatchingRules.groovy.dmn";
  public static final String DMN_DECISIONS_WITH_NO_MATCHING_RULE_IN_PARENT = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDecisionsWithNoMatchingRuleInParent.groovy.dmn";
  public static final String DMN_DECISIONS_WITH_MULTIPLE_MATCHING_RULES_MULTIPLE_OUTPUTS = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDecisionsWithMultipleMatchingRulesAndMultipleOutputs.groovy.dmn";
  public static final String DMN_SHARED_DECISIONS = "org/operaton/bpm/dmn/engine/evaluate/EvaluateSharedDecisions.dmn";
  public static final String DMN_DECISIONS_WITH_DIFFERENT_INPUT_OUTPUT_TYPES = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDecisionsWithDifferentInputAndOutputTypes.groovy.dmn";
  public static final String DMN_DECISIONS_WITH_DEFAULT_RULE_IN_CHILD = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDecisionsWithDefaultRuleInChild.groovy.dmn";
  public static final String DMN_DECISIONS_WITH_INVALID_INPUT_TYPE = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDecisionsWithInvalidInputTypeInParent.groovy.dmn";
  public static final String DMN_DECISIONS_WITH_PARENT_DECISION = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDecisionsWithParentDecision.dmn";
  public static final String DMN_DECISIONS_WITH_DISH_DECISON_EXAMPLE = "org/operaton/bpm/dmn/engine/evaluate/EvaluateDrdDishDecisionExample.dmn";

  public static final String DMN_DECISION_WITH_LITERAL_EXPRESSION = "org/operaton/bpm/dmn/engine/evaluate/DecisionWithLiteralExpression.dmn";
  public static final String DMN_DRG_WITH_LITERAL_EXPRESSION = "org/operaton/bpm/dmn/engine/evaluate/DrgWithLiteralExpression.dmn";
  public static final String DMN_DECISION_WITH_BEAN_INVOCATION_IN_LITERAL_EXPRESSION = "org/operaton/bpm/dmn/engine/evaluate/DecisionWithBeanInvocationInLiteralExpression.dmn";

  public static final String DRG_COLLECT_DMN = "org/operaton/bpm/dmn/engine/transform/DrgCollectTest.dmn";
  public static final String DRG_RULE_ORDER_DMN = "org/operaton/bpm/dmn/engine/transform/DrgRuleOrderTest.dmn";

  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    return new DefaultDmnEngineConfiguration()
      .enableFeelLegacyBehavior(true);
  }

  @Test
  void shouldEvaluateDrdDishDecisionExample() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("Dish", DMN_DECISIONS_WITH_DISH_DECISON_EXAMPLE) , createVariables()
      .putValue("temperature", 20)
      .putValue("dayType", "Weekend"));

    assertThat(results)
      .hasSingleResult()
      .containsEntry("desiredDish", "Steak");
  }

  @Test
  void shouldEvaluateDecisionWithRequiredDecisionByKey() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_MULTI_LEVEL_MULTIPLE_INPUT_SINGLE_OUTPUT) , createVariables()
      .putValue("xx", "xx")
      .putValue("yy", "yy")
      .putValue("zz", "zz")
      .putValue("ll", "ll")
      .asVariableContext());

    assertThat(results)
    .hasSingleResult()
    .containsEntry("aa", "aa");

  }

  @Test
  void shouldFailDecisionEvaluationWithRequiredDecisionAndNoMatchingRuleInChildDecision() {

    try {
      dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_MULTI_LEVEL_MULTIPLE_INPUT_SINGLE_OUTPUT) , createVariables()
        .putValue("xx", "pp")
        .putValue("yy", "yy")
        .putValue("zz", "zz")
        .putValue("ll", "ll")
        .asVariableContext());
    } catch(DmnEvaluationException e) {
      assertThat(e)
      .hasMessageStartingWith("DMN-01002")
      .hasMessageContaining("Unable to evaluate expression for language 'juel': '${dd}'");
    }
  }

  @Test
  void shouldFailDecisionEvaluationWithRequiredDecisionAndMissingInput() {

    try {
      dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_MULTI_LEVEL_MULTIPLE_INPUT_SINGLE_OUTPUT) , createVariables()
        .putValue("xx", "xx")
        .putValue("yy", "yy")
        .putValue("zz", "zz")
        .asVariableContext());
    } catch(DmnEvaluationException e) {
      assertThat(e)
      .hasMessageStartingWith("DMN-01002")
      .hasMessageContaining("Unable to evaluate expression for language 'juel': '${ll}'");
    }
  }

  @Test
  void shouldEvaluateDecisionsWithRequiredDecisionAndMultipleMatchingRules() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_MULTIPLE_MATCHING_RULES) , createVariables()
        .putValue("dd", 3)
        .putValue("ee", "ee")
        .asVariableContext());

    List<Map<String, Object>> resultList = results.getResultList();
    assertThat(resultList.get(0)).containsEntry("aa", "aa");
    assertThat(resultList.get(1)).containsEntry("aa", "aaa");
  }

  @Test
  void shouldEvaluateDecisionsWithRequiredDecisionAndMultipleMatchingRulesMultipleOutputs() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_MULTIPLE_MATCHING_RULES_MULTIPLE_OUTPUTS) , createVariables()
        .putValue("dd", "dd")
        .putValue("ee", "ee")
        .asVariableContext());

    List<Map<String, Object>> resultList = results.getResultList();
    assertThat(resultList.get(0)).containsEntry("aa", "aa");
    assertThat(resultList.get(1)).containsEntry("aa", "aaa");

  }

  @Test
  void shouldEvaluateDecisionWithRequiredDecisionAndNoMatchingRuleInParentDecision() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_NO_MATCHING_RULE_IN_PARENT) , createVariables()
      .putValue("dd", "dd")
      .putValue("ee", "ee")
      .asVariableContext());

    List<Map<String, Object>> resultList = results.getResultList();
    assertThat(resultList).isEmpty();

  }

  @Test
  void shouldEvaluateDecisionsWithRequiredDecisionAndParentDecision() {

   DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_PARENT_DECISION) , createVariables()
     .putValue("ff", true)
     .putValue("dd", 5)
     .asVariableContext());

   assertThat(results)
     .hasSingleResult()
     .containsEntry("aa", 7.0);
  }

  @Test
  void shouldEvaluateSharedDecisions() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_SHARED_DECISIONS) , createVariables()
      .putValue("ff", "ff")
      .asVariableContext());

    assertThat(results)
      .hasSingleResult()
      .containsEntry("aa", "aa");
  }

  @Test
  void shouldEvaluateDecisionsWithDifferentInputAndOutputTypes() {
    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_DIFFERENT_INPUT_OUTPUT_TYPES) , createVariables()
      .putValue("dd", "5")
      .putValue("ee", 21)
      .asVariableContext());

    assertThat(results.get(0))
      .containsEntry("aa", 7.1);

    results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_DIFFERENT_INPUT_OUTPUT_TYPES) , createVariables()
      .putValue("dd", "5")
      .putValue("ee", 2147483650L)
      .asVariableContext());

    assertThat(results.get(0))
    .containsEntry("aa", 7.0);
  }

  @Test
  void shouldEvaluateDecisionsWithNoMatchingRuleAndDefaultRuleInParent() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_DIFFERENT_INPUT_OUTPUT_TYPES) , createVariables()
      .putValue("dd", "7")
      .putValue("ee", 2147483650L)
      .asVariableContext());

    assertThat(results)
      .hasSingleResult()
      .containsEntry("aa", 7.2);
  }

  @Test
  void shouldEvaluateDecisionsWithDefaultRuleInChildDecision() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_DEFAULT_RULE_IN_CHILD) , createVariables()
      .putValue("dd", "7") // There is no rule in the table matching the input 7
      .asVariableContext());

    assertThat(results)
      .hasSingleResult()
      .containsEntry("aa", 7.0);
  }

  @Test
  void shouldEvaluateDecisionsWithUserInputForParentDecision() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_DIFFERENT_INPUT_OUTPUT_TYPES) , createVariables()
      .putValue("bb", "bb")
      .putValue("dd", "7")
      .putValue("ee", 2147483650L)
      .asVariableContext());

    // input value provided by the user is overriden by the child decision
    assertThat(results)
      .hasSingleResult()
      .containsEntry("aa", 7.2);
  }

  @Test
  void shouldEvaluateDecisionsWithInputTypeMisMatchInChildDecision() {
    try {
      dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_DIFFERENT_INPUT_OUTPUT_TYPES) , createVariables()
        .putValue("dd", "7")
        .putValue("ee", "abc")
        .asVariableContext());
    } catch(DmnEngineException e) {
      assertThat(e)
      .hasMessageStartingWith("DMN-01005")
      .hasMessageContaining("Invalid value 'abc' for clause with type 'long'");
    }
  }

  @Test
  void shouldEvaluateDecisionsWithInputTypeMisMatchInParentDecision() {

    try {
      dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_INVALID_INPUT_TYPE) , createVariables()
        .putValue("dd", 5)
        .asVariableContext());
    } catch(DmnEngineException e) {
      assertThat(e)
      .hasMessageStartingWith("DMN-01005")
      .hasMessageContaining("Invalid value 'bb' for clause with type 'integer'");
    }
  }

  @Test
  void shouldEvaluateDecisionWithLiteralExpression() {
    DmnDecisionResult result = dmnEngine.evaluateDecision(parseDecisionFromFile("decision", DMN_DECISION_WITH_LITERAL_EXPRESSION) ,
        createVariables()
          .putValue("a", 2)
          .putValue("b", 3));

    assertThat(result.getSingleResult().keySet()).containsOnly("c");

    assertThat((int) result.getSingleEntry())
      .isNotNull()
      .isEqualTo(5);
  }

  @Test
  void shouldEvaluateDecisionsDrgWithLiteralExpression() {
    DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("dish-decision", DMN_DRG_WITH_LITERAL_EXPRESSION) ,
        createVariables()
          .putValue("temperature", 31)
          .putValue("dayType", "WeekDay"));

    assertThat(result)
      .hasSingleResult()
      .containsEntry("desiredDish", "Light Salad");
  }

  @Test
  void shouldEvaluateDecisionWithBeanInvocationInLiteralExpression() {
    DmnDecisionResult result = dmnEngine.evaluateDecision(parseDecisionFromFile("decision", DMN_DECISION_WITH_BEAN_INVOCATION_IN_LITERAL_EXPRESSION) ,
        createVariables()
          .putValue("x", 2)
          .putValue("bean", new TestBean(3)));

    assertThat((int) result.getSingleEntry())
      .isNotNull()
      .isEqualTo(6);
  }

  @Test
  void shouldEvaluateDecisionWithCollectHitPolicyReturningAList() {
    DmnDecisionRequirementsGraph graph = dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_COLLECT_DMN));
    initVariables();
    variables.putValue("dayType","WeekDay");

    DmnDecisionResult result = dmnEngine.evaluateDecision(graph.getDecision("dish-decision"), variables);
    assertThat((String) result.getSingleEntry())
      .isNotNull()
      .isEqualTo("Steak");
  }

  @Test
  void shouldEvaluateDecisionWithRuleOrderHitPolicyReturningAList() {
    DmnDecisionRequirementsGraph graph = dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_RULE_ORDER_DMN));
    initVariables();
    variables.putValue("dayType","WeekDay");

    DmnDecisionResult result = dmnEngine.evaluateDecision(graph.getDecision("dish-decision"), variables);
    assertThat((String) result.getSingleEntry())
      .isNotNull()
      .isEqualTo("Steak");
  }
}
