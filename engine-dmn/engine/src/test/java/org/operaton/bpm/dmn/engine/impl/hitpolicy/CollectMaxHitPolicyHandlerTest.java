package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.DmnHitPolicyException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;

class CollectMaxHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final Double DOUBLE_MIN = -Double.MAX_VALUE;

  private static final String COLLECT_MAX_SINGLE =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/CollectMaxHitPolicyHandlerTest.collect.max.single.dmn";
  private static final String COLLECT_MAX_COMPOUND =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/CollectMaxHitPolicyHandlerTest.collect.max.compound.dmn";

  @Test
  @DecisionResource(resource = COLLECT_MAX_SINGLE)
  void collectMaxHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, 10, 20L, 30.034)
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = COLLECT_MAX_SINGLE)
  void collectMaxHitPolicySingleOutputSingleMatchingRule() {
    assertThatDecisionTableResult(true, false, false, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(10);

    assertThatDecisionTableResult(false, true, false, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(20L);

    assertThatDecisionTableResult(false, false, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(30.034);

    assertThatDecisionTableResult(true, false, false, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(MAX_VALUE);

    assertThatDecisionTableResult(true, false, false, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(MIN_VALUE);

    assertThatDecisionTableResult(false, true, false, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Long.MAX_VALUE);

    assertThatDecisionTableResult(false, true, false, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(Long.MIN_VALUE);

    assertThatDecisionTableResult(false, false, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Double.MAX_VALUE);

    assertThatDecisionTableResult(false, false, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(DOUBLE_MIN);

    assertThatDecisionTableResult(true, false, false, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(1L);

    assertThatDecisionTableResult(false, true, false, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(2L);

    assertThatDecisionTableResult(false, false, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(3.0);

    assertThatThrownBy(() -> evaluateDecisionTable(false, false, true, 10, 20L, "c"))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03004");
  }

  @Test
  @DecisionResource(resource = COLLECT_MAX_SINGLE)
  void collectMaxHitPolicySingleOutputMultipleMatchingRules() {
    assertThatDecisionTableResult(true, true, false, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(20L);

    assertThatDecisionTableResult(true, false, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(30.034);

    assertThatDecisionTableResult(false, true, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(30.034);

    assertThatDecisionTableResult(true, true, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(30.034);

    assertThatDecisionTableResult(true, true, false, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Long.MAX_VALUE);

    assertThatDecisionTableResult(true, true, false, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry((long) MIN_VALUE);

    assertThatDecisionTableResult(true, false, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Double.MAX_VALUE);

    assertThatDecisionTableResult(true, false, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry((double) MIN_VALUE);

    assertThatDecisionTableResult(false, true, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Double.MAX_VALUE);

    assertThatDecisionTableResult(false, true, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry((double) Long.MIN_VALUE);

    assertThatDecisionTableResult(true, true, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Double.MAX_VALUE);

    assertThatDecisionTableResult(true, true, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry((double) MIN_VALUE);

    assertThatDecisionTableResult(true, true, false, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(2L);

    assertThatDecisionTableResult(true, false, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(3.0);

    assertThatDecisionTableResult(false, true, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(3.0);

    assertThatDecisionTableResult(true, true, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(3.0);

    assertThatThrownBy(() -> evaluateDecisionTable(true, true, true, 10, 20L, true))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03004");
  }

  @Test
  @DecisionResource(resource = COLLECT_MAX_COMPOUND)
  void collectMaxHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "true, false, false",
    "false, true, false",
    "false, false, true"
  })
  @DecisionResource(resource = COLLECT_MAX_COMPOUND)
  void collectMaxHitPolicyCompoundOutputSingleMatchingRule(boolean input1, boolean input2, boolean input3) {
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
  @DecisionResource(resource = COLLECT_MAX_COMPOUND)
  void collectMaxHitPolicyCompoundOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3) {
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
