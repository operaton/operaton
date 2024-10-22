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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;
import static org.operaton.bpm.engine.variable.Variables.createVariables;

import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.DmnEngineException;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnEvaluationException;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.commons.utils.IoUtil;
import org.junit.Test;

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

    /**
   * Returns a new instance of DefaultDmnEngineConfiguration with Feel Legacy Behavior enabled.
   *
   * @return a new DefaultDmnEngineConfiguration with Feel Legacy Behavior enabled
   */
  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    return new DefaultDmnEngineConfiguration()
      .enableFeelLegacyBehavior(true);
  }

    /**
   * Evaluates the decision table for the DRD Dish Decision example with specified temperature and day type,
   * and asserts that the result contains a single entry with the desired dish set to "Steak".
   */
  @Test
  public void shouldEvaluateDrdDishDecisionExample() {

    /**
   * This method tests the evaluation of a decision with a required decision key.
   */
  @Test
  public void shouldEvaluateDecisionWithRequiredDecisionByKey() {

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
  public void shouldFailDecisionEvaluationWithRequiredDecisionAndNoMatchingRuleInChildDecision() {

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

    /**
   * This method tests the failure of decision evaluation when a required decision does not have a matching rule in a child decision.
   */
  @Test
  public void shouldFailDecisionEvaluationWithRequiredDecisionAndMissingInput() {

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

    /**
   * This method tests that the decision evaluation fails when a required decision has missing input variables.
   */
  @Test
  public void shouldFailDecisionEvaluationWithRequiredDecisionAndMissingInput() {
  
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

    /**
   * This method tests the evaluation of decisions with a required decision and multiple matching rules.
   */
  @Test
  public void shouldEvaluateDecisionsWithRequiredDecisionAndMultipleMatchingRules() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_MULTIPLE_MATCHING_RULES) , createVariables()
        .putValue("dd", 3)
        .putValue("ee", "ee")
        .asVariableContext());

    List<Map<String, Object>> resultList = results.getResultList();
    assertThat(resultList.get(0)).containsEntry("aa", "aa");
    assertThat(resultList.get(1)).containsEntry("aa", "aaa");
  }

    /**
   * This method tests the evaluation of decisions with a required decision and multiple matching rules that result in multiple outputs.
   */
  @Test
  public void shouldEvaluateDecisionsWithRequiredDecisionAndMultipleMatchingRulesMultipleOutputs() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_MULTIPLE_MATCHING_RULES_MULTIPLE_OUTPUTS) , createVariables()
        .putValue("dd", "dd")
        .putValue("ee", "ee")
        .asVariableContext());

    List<Map<String, Object>> resultList = results.getResultList();
    assertThat(resultList.get(0)).containsEntry("aa", "aa");
    assertThat(resultList.get(1)).containsEntry("aa", "aaa");

  }

    /**
   * This method tests the evaluation of a decision with a required decision and no matching rule in the parent decision. 
   * It evaluates the decision table and checks if the result list is empty.
   */
  @Test
  public void shouldEvaluateDecisionWithRequiredDecisionAndNoMatchingRuleInParentDecision() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_NO_MATCHING_RULE_IN_PARENT) , createVariables()
      .putValue("dd", "dd")
      .putValue("ee", "ee")
      .asVariableContext());

    List<Map<String, Object>> resultList = results.getResultList();
    assertThat(resultList.size()).isEqualTo(0);

  }

    /**
   * This method tests the evaluation of decisions with a required parent decision. It evaluates a decision table
   * with specific inputs and verifies that the result matches the expected output.
   */
  @Test
  public void shouldEvaluateDecisionsWithRequiredDecisionAndParentDecision() {

   DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_PARENT_DECISION) , createVariables()
     .putValue("ff", true)
     .putValue("dd", 5)
     .asVariableContext());

   assertThat(results)
     .hasSingleResult()
     .containsEntry("aa", 7.0);
  }

    /**
   * This method tests the evaluation of shared decisions using the DMN engine. It evaluates a specific decision table 
   * with the provided variables and asserts that the result contains a single entry with key "aa" and value "aa".
   */
  @Test
  public void shouldEvaluateSharedDecisions() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_SHARED_DECISIONS) , createVariables()
      .putValue("ff", "ff")
      .asVariableContext());

    assertThat(results)
      .hasSingleResult()
      .containsEntry("aa", "aa");
  }

    /**
   * This method tests the evaluation of decisions with different input and output types by using the DMN engine to evaluate a decision table with different input and output types.
   */
  @Test
  public void shouldEvaluateDecisionsWithDifferentInputAndOutputTypes() {
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

    /**
   * This method tests the evaluation of decisions when there is no matching rule and a default rule in the parent.
   */
  @Test
  public void shouldEvaluateDecisionsWithNoMatchingRuleAndDefaultRuleInParent() {

    /**
   * This method tests the evaluation of decisions with a default rule in a child decision table.
   */
  @Test
  public void shouldEvaluateDecisionsWithDefaultRuleInChildDecision() {

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("A", DMN_DECISIONS_WITH_DEFAULT_RULE_IN_CHILD) , createVariables()
      .putValue("dd", "7") // There is no rule in the table matching the input 7
      .asVariableContext());

    assertThat(results)
      .hasSingleResult()
      .containsEntry("aa", 7.0);
  }

  @Test
  public void shouldEvaluateDecisionsWithUserInputForParentDecision() {

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

    /**
   * This method evaluates decisions with user input for the parent decision.
   */
  @Test
  public void shouldEvaluateDecisionsWithInputTypeMisMatchInChildDecision() {
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

    /**
   * This method tests the evaluation of decisions with input type mismatch in a child decision. 
   */
  @Test
  public void shouldEvaluateDecisionsWithInputTypeMisMatchInParentDecision() {

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

    /**
   * This method tests the evaluation of decisions with input type mismatch in parent decision.
   */
  @Test
  public void shouldEvaluateDecisionWithLiteralExpression() {
    DmnDecisionResult result = dmnEngine.evaluateDecision(parseDecisionFromFile("decision", DMN_DECISION_WITH_LITERAL_EXPRESSION) ,
        createVariables()
          .putValue("a", 2)
          .putValue("b", 3));

    assertThat(result.getSingleResult().keySet()).containsOnly("c");

    /**
   * This method tests the evaluation of a decision with a literal expression. It evaluates a decision with variables "a" and "b" set to 2 and 3 respectively,
   * and expects the result to contain only key "c" with a single entry value of 5.
   */
  @Test
    public void shouldEvaluateDecisionWithLiteralExpression() {
      DmnDecisionResult result = dmnEngine.evaluateDecision(parseDecisionFromFile("decision", DMN_DECISION_WITH_LITERAL_EXPRESSION) ,
          createVariables()
            .putValue("a", 2)
            .putValue("b", 3));
  
      assertThat(result.getSingleResult().keySet()).containsOnly("c");
  
      assertThat((int) result.getSingleEntry())
        .isNotNull()
        .isEqualTo(5);
    }

    /**
   * This method tests the evaluation of a decision table with literal expressions in a DMN.
   */
  @Test
  public void shouldEvaluateDecisionsDrgWithLiteralExpression() {
    DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(parseDecisionFromFile("dish-decision", DMN_DRG_WITH_LITERAL_EXPRESSION) ,
        createVariables()
          .putValue("temperature", 31)
          .putValue("dayType", "WeekDay"));

    assertThat(result)
      .hasSingleResult()
      .containsEntry("desiredDish", "Light Salad");
  }

    /**
   * Test method to evaluate a decision with bean invocation in a literal expression.
   */
  @Test
  public void shouldEvaluateDecisionWithBeanInvocationInLiteralExpression() {
    DmnDecisionResult result = dmnEngine.evaluateDecision(parseDecisionFromFile("decision", DMN_DECISION_WITH_BEAN_INVOCATION_IN_LITERAL_EXPRESSION) ,
        createVariables()
          .putValue("x", 2)
          .putValue("bean", new TestBean(3)));

    assertThat((int) result.getSingleEntry())
      .isNotNull()
      .isEqualTo(6);
  }

    /**
   * This method tests the evaluation of a decision with collect hit policy returning a list.
   */
  @Test
  public void shouldEvaluateDecisionWithCollectHitPolicyReturningAList() {
    DmnDecisionRequirementsGraph graph = dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_COLLECT_DMN));
    initVariables();
    variables.putValue("dayType","WeekDay");

    DmnDecisionResult result = dmnEngine.evaluateDecision(graph.getDecision("dish-decision"), variables);
    assertThat((String) result.getSingleEntry())
      .isNotNull()
      .isEqualTo("Steak");
  }

    /**
   * This method tests the evaluation of a decision with rule order hit policy returning a list.
   */
  @Test
  public void shouldEvaluateDecisionWithRuleOrderHitPolicyReturningAList() {
    DmnDecisionRequirementsGraph graph = dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_RULE_ORDER_DMN));
    initVariables();
    variables.putValue("dayType","WeekDay");

    DmnDecisionResult result = dmnEngine.evaluateDecision(graph.getDecision("dish-decision"), variables);
    assertThat((String) result.getSingleEntry())
      .isNotNull()
      .isEqualTo("Steak");
  }
}
