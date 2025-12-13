package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.DmnHitPolicyException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;

class UniqueHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final String UNIQUE_SINGLE =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/UniqueHitPolicyHandlerTest.unique.single.dmn";
  private static final String UNIQUE_COMPOUND =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/UniqueHitPolicyHandlerTest.unique.compound.dmn";

  @Test
  @DecisionResource(resource = UNIQUE_SINGLE)
  void uniqueHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = UNIQUE_SINGLE)
  void uniqueHitPolicySingleOutputSingleMatchingRule() {
    assertThatDecisionTableResult(true, false, false, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry("a");

    assertThatDecisionTableResult(false, true, false, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry("b");

    assertThatDecisionTableResult(false, false, true, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry("c");
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, false, a, b, c",
    "true, false, true, a, b, c",
    "false, true, true, a, b, c",
    "true, true, true, a, b, c"
  })
  @DecisionResource(resource = UNIQUE_SINGLE)
  void uniqueHitPolicySingleOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3, String output1, String output2, String output3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, output1, output2, output3))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03001");
  }

  @Test
  @DecisionResource(resource = UNIQUE_COMPOUND)
  void uniqueHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = UNIQUE_COMPOUND)
  void uniqueHitPolicyCompoundOutputSingleMatchingRule() {
    assertThatDecisionTableResult(true, false, false, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(org.assertj.core.api.Assertions.entry("out1", "a"), org.assertj.core.api.Assertions.entry("out2", "a"), org.assertj.core.api.Assertions.entry("out3", "a"));

    assertThatDecisionTableResult(false, true, false, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(org.assertj.core.api.Assertions.entry("out1", "b"), org.assertj.core.api.Assertions.entry("out2", "b"), org.assertj.core.api.Assertions.entry("out3", "b"));

    assertThatDecisionTableResult(false, false, true, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(org.assertj.core.api.Assertions.entry("out1", "c"), org.assertj.core.api.Assertions.entry("out2", "c"), org.assertj.core.api.Assertions.entry("out3", "c"));
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, false, a, b, c",
    "true, false, true, a, b, c",
    "false, true, true, a, b, c",
    "true, true, true, a, b, c"
  })
  @DecisionResource(resource = UNIQUE_COMPOUND)
  void uniqueHitPolicyCompoundOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3, String output1, String output2, String output3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, output1, output2, output3))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03001");
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
