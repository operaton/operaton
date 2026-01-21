package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.entry;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.test.DecisionResource;

class FirstHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final String FIRST_SINGLE =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/FirstHitPolicyHandlerTest.first.single.dmn";
  private static final String FIRST_COMPOUND =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/FirstHitPolicyHandlerTest.first.compound.dmn";

  @Test
  @DecisionResource(resource = FIRST_SINGLE)
  void firstHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = FIRST_SINGLE)
  void firstHitPolicySingleOutputSingleMatchingRule() {
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

  @Test
  @DecisionResource(resource = FIRST_SINGLE)
  void firstHitPolicySingleOutputMultipleMatchingRules() {
    assertThatDecisionTableResult(true, true, false, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry("a");

    assertThatDecisionTableResult(true, true, false, "c", "b", "a")
      .hasSingleResult()
      .hasSingleEntry("c");

    assertThatDecisionTableResult(true, false, true, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry("a");

    assertThatDecisionTableResult(true, false, true, "c", "b", "a")
      .hasSingleResult()
      .hasSingleEntry("c");

    assertThatDecisionTableResult(false, true, true, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry("b");

    assertThatDecisionTableResult(false, true, true, "c", "b", "a")
      .hasSingleResult()
      .hasSingleEntry("b");

    assertThatDecisionTableResult(true, true, true, "a", "b", "c")
      .hasSingleResult()
      .hasSingleEntry("a");

    assertThatDecisionTableResult(true, true, true, "c", "b", "a")
      .hasSingleResult()
      .hasSingleEntry("c");
  }

  @Test
  @DecisionResource(resource = FIRST_COMPOUND)
  void firstHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = FIRST_COMPOUND)
  void firstHitPolicyCompoundOutputSingleMatchingRule() {
    assertThatDecisionTableResult(true, false, false, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    assertThatDecisionTableResult(false, true, false, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    assertThatDecisionTableResult(false, false, true, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
  }

  @Test
  @DecisionResource(resource = FIRST_COMPOUND)
  void firstHitPolicyCompoundOutputMultipleMatchingRules() {
    assertThatDecisionTableResult(true, true, false, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    assertThatDecisionTableResult(true, true, false, "c", "b", "a")
      .hasSingleResult()
      .containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));

    assertThatDecisionTableResult(true, false, true, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    assertThatDecisionTableResult(true, false, true, "c", "b", "a")
      .hasSingleResult()
      .containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));

    assertThatDecisionTableResult(false, true, true, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    assertThatDecisionTableResult(false, true, true, "c", "b", "a")
      .hasSingleResult()
      .containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    assertThatDecisionTableResult(true, true, true, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    assertThatDecisionTableResult(true, true, true, "c", "b", "a")
      .hasSingleResult()
      .containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
  }

  private org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert assertThatDecisionTableResult(Boolean input1, Boolean input2, Boolean input3, Object output1, Object output2, Object output3) {
    variables.put("input1", input1);
    variables.put("input2", input2);
    variables.put("input3", input3);
    variables.put("output1", output1);
    variables.put("output2", output2);
    variables.put("output3", output3);

    return assertThat(evaluateDecisionTable());
  }
}
