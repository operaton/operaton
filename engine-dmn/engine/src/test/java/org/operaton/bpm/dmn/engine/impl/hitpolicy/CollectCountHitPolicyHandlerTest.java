package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.DmnHitPolicyException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;

class CollectCountHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final String COLLECT_COUNT_SINGLE =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/CollectCountHitPolicyHandlerTest.collect.count.single.dmn";
  private static final String COLLECT_COUNT_COMPOUND =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/CollectCountHitPolicyHandlerTest.collect.count.compound.dmn";

  @Test
  @DecisionResource(resource = COLLECT_COUNT_SINGLE)
  void collectCountHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, 10, "b", 30.034)
      .hasSingleResult()
      .hasSingleEntry(0);
  }

  @Test
  @DecisionResource(resource = COLLECT_COUNT_SINGLE)
  void collectCountHitPolicySingleOutputSingleMatchingRule() {
    assertThatDecisionTableResult(true, false, false, 10, "b", 30.034)
      .hasSingleResult()
      .hasSingleEntry(1);

    assertThatDecisionTableResult(false, true, false, 10, "b", 30.034)
      .hasSingleResult()
      .hasSingleEntry(1);

    assertThatDecisionTableResult(false, false, true, 10, "b", 30.034)
      .hasSingleResult()
      .hasSingleEntry(1);
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, false, 2",
    "true, false, true, 2",
    "false, true, true, 2",
    "true, true, true, 3"
  })
  @DecisionResource(resource = COLLECT_COUNT_SINGLE)
  void collectCountHitPolicySingleOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3, int expectedCount) {
    assertThatDecisionTableResult(input1, input2, input3, 10, "b", 30.034)
      .hasSingleResult()
      .hasSingleEntry(expectedCount);
  }

  @Test
  @DecisionResource(resource = COLLECT_COUNT_COMPOUND)
  void collectCountHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry(0);
  }

  @ParameterizedTest
  @CsvSource({
    "true, false, false",
    "false, true, false",
    "false, false, true"
  })
  @DecisionResource(resource = COLLECT_COUNT_COMPOUND)
  void collectCountHitPolicyCompoundOutputSingleMatchingRule(boolean input1, boolean input2, boolean input3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, 1, 2L, 3d))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03003");
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, false",
    "true, false, true",
    "false, true, true",
    "true, true, true"
  })
  @DecisionResource(resource = COLLECT_COUNT_COMPOUND)
  void collectCountHitPolicyCompoundOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, 1, 2L, 3d))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03003");
  }

  private DmnDecisionTableResultAssert assertThatDecisionTableResult(Boolean input1, Boolean input2, Boolean input3, Object output1, Object output2, Object output3) {
    variables.put("input1", input1);
    variables.put("input2", input2);
    variables.put("input3", input3);
    variables.put("output1", output1);
    variables.put("output2", output2);
    variables.put("output3", output3);

    return assertThat(evaluateDecisionTable());
  }

  private org.operaton.bpm.dmn.engine.DmnDecisionTableResult evaluateDecisionTable(Boolean input1, Boolean input2, Boolean input3, Object output1, Object output2, Object output3) {
    variables.put("input1", input1);
    variables.put("input2", input2);
    variables.put("input3", input3);
    variables.put("output1", output1);
    variables.put("output2", output2);
    variables.put("output3", output3);

    return evaluateDecisionTable();
  }
}
