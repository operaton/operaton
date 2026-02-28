package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;

class CollectHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final String COLLECT_SINGLE =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/CollectHitPolicyHandlerTest.collect.single.dmn";
  private static final String COLLECT_COMPOUND =
      "org/operaton/bpm/dmn/engine/impl/hitpolicy/CollectHitPolicyHandlerTest.collect.compound.dmn";

  @Test
  @DecisionResource(resource = COLLECT_SINGLE)
  void collectHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = COLLECT_SINGLE)
  void collectHitPolicySingleOutputSingleMatchingRule() {
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
  @DecisionResource(resource = COLLECT_SINGLE)
  void collectHitPolicySingleOutputMultipleMatchingRules() {
    DmnDecisionTableResult results = evaluateDecisionTable(true, true, false, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsOnlyOnce("a", "b");

    results = evaluateDecisionTable(true, false, true, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsOnlyOnce("a", "c");

    results = evaluateDecisionTable(false, true, true, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsOnlyOnce("b", "c");

    results = evaluateDecisionTable(true, true, true, "a", "b", "c");
    assertThat(results).hasSize(3);
    assertThat(collectSingleOutputEntries(results)).containsOnlyOnce("a", "b", "c");
  }

  @Test
  @DecisionResource(resource = COLLECT_COMPOUND)
  void collectHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = COLLECT_COMPOUND)
  void collectHitPolicyCompoundOutputSingleMatchingRule() {
    DmnDecisionTableResult results = evaluateDecisionTable(true, false, false, "a", "b", "c");
    assertThat(results)
      .hasSingleResult()
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    results = evaluateDecisionTable(false, true, false, "a", "b", "c");
    assertThat(results)
      .hasSingleResult()
      .containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    results = evaluateDecisionTable(false, false, true, "a", "b", "c");
    assertThat(results)
      .hasSingleResult()
      .containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
  }

  private java.util.List<Object> collectSingleOutputEntries(DmnDecisionTableResult results) {
    java.util.List<Object> values = new java.util.ArrayList<>();
    for (org.operaton.bpm.dmn.engine.DmnDecisionRuleResult result : results) {
      values.add(result.getSingleEntry());
    }
    return values;
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
