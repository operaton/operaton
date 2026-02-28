package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.DmnHitPolicyException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;

import java.util.ArrayList;
import java.util.List;

import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;

class AnyHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final String ANY_SINGLE =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/AnyHitPolicyHandlerTest.any.single.dmn";
  private static final String ANY_COMPOUND =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/AnyHitPolicyHandlerTest.any.compound.dmn";

  @Test
  @DecisionResource(resource = ANY_SINGLE)
  void anyHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = ANY_SINGLE)
  void anyHitPolicySingleOutputSingleMatchingRule() {
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
  @DecisionResource(resource = ANY_SINGLE)
  void anyHitPolicySingleOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3, String output1, String output2, String output3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, output1, output2, output3))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03002");

    assertThatDecisionTableResult(input1, input2, input3, "a", "a", "a")
      .hasSingleResult()
      .hasSingleEntry("a");
  }

  @Test
  @DecisionResource(resource = ANY_COMPOUND)
  void anyHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = ANY_COMPOUND)
  void anyHitPolicyCompoundOutputSingleMatchingRule() {
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
  @DecisionResource(resource = ANY_COMPOUND)
  void anyHitPolicyCompoundOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3, String output1, String output2, String output3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, output1, output2, output3))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03002");

    assertThatDecisionTableResult(input1, input2, input3, "a", "a", "a")
      .hasSingleResult()
      .containsOnly(org.assertj.core.api.Assertions.entry("out1", "a"), org.assertj.core.api.Assertions.entry("out2", "a"), org.assertj.core.api.Assertions.entry("out3", "a"));
  }

  private DmnDecisionTableResult evaluateDecisionTable(Boolean input1, Boolean input2, Boolean input3, Object output1, Object output2, Object output3) {
    variables.put("input1", input1);
    variables.put("input2", input2);
    variables.put("input3", input3);
    variables.put("output1", output1);
    variables.put("output2", output2);
    variables.put("output3", output3);

    return evaluateDecisionTable();
  }

  private DmnDecisionTableResultAssert assertThatDecisionTableResult(Boolean input1, Boolean input2, Boolean input3, Object output1, Object output2, Object output3) {
    return assertThat(evaluateDecisionTable(input1, input2, input3, output1, output2, output3));
  }
}
