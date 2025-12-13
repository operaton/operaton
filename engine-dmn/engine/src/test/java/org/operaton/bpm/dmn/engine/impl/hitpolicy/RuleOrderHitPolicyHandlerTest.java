package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.test.DecisionResource;

class RuleOrderHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final String RULE_ORDER_SINGLE =
      "org/operaton/bpm/engine/impl/hitpolicy/RuleOrderHitPolicyHandlerTest.ruleOrder.single.dmn";
  private static final String RULE_ORDER_COMPOUND =
      "org/operaton/bpm/engine/impl/hitpolicy/RuleOrderHitPolicyHandlerTest.ruleOrder.compound.dmn";

  @Test
  @DecisionResource(resource = RULE_ORDER_SINGLE)
  void ruleOrderHitPolicySingleOutputNoMatchingRule() {
    DmnDecisionTableResult results = evaluateDecisionTable(false, false, false, "a", "b", "c");
    assertThat(results).isEmpty();
  }

  @Test
  @DecisionResource(resource = RULE_ORDER_SINGLE)
  void ruleOrderHitPolicySingleOutputSingleMatchingRule() {
    DmnDecisionTableResult results = evaluateDecisionTable(true, false, false, "a", "b", "c");
    assertThat(results).hasSize(1);
    assertThat(collectSingleOutputEntries(results)).containsExactly("a");

    results = evaluateDecisionTable(false, true, false, "a", "b", "c");
    assertThat(results).hasSize(1);
    assertThat(collectSingleOutputEntries(results)).containsExactly("b");

    results = evaluateDecisionTable(false, false, true, "a", "b", "c");
    assertThat(results).hasSize(1);
    assertThat(collectSingleOutputEntries(results)).containsExactly("c");
  }

  @Test
  @DecisionResource(resource = RULE_ORDER_SINGLE)
  void ruleOrderHitPolicySingleOutputMultipleMatchingRules() {
    DmnDecisionTableResult results = evaluateDecisionTable(true, true, false, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsExactly("a", "b");

    results = evaluateDecisionTable(true, true, false, "c", "b", "a");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsExactly("c", "b");

    results = evaluateDecisionTable(true, false, true, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsExactly("a", "c");

    results = evaluateDecisionTable(true, false, true, "c", "b", "a");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsExactly("c", "a");

    results = evaluateDecisionTable(false, true, true, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsExactly("b", "c");

    results = evaluateDecisionTable(false, true, true, "c", "b", "a");
    assertThat(results).hasSize(2);
    assertThat(collectSingleOutputEntries(results)).containsExactly("b", "a");

    results = evaluateDecisionTable(true, true, true, "a", "b", "c");
    assertThat(results).hasSize(3);
    assertThat(collectSingleOutputEntries(results)).containsExactly("a", "b", "c");

    results = evaluateDecisionTable(true, true, true, "c", "b", "a");
    assertThat(results).hasSize(3);
    assertThat(collectSingleOutputEntries(results)).containsExactly("c", "b", "a");
  }

  @Test
  @DecisionResource(resource = RULE_ORDER_COMPOUND)
  void ruleOrderHitPolicyCompoundOutputNoMatchingRule() {
    DmnDecisionTableResult results = evaluateDecisionTable(false, false, false, "a", "b", "c");
    assertThat(results).isEmpty();
  }

  @Test
  @DecisionResource(resource = RULE_ORDER_COMPOUND)
  void ruleOrderHitPolicyCompoundOutputSingleMatchingRule() {
    DmnDecisionTableResult results = evaluateDecisionTable(true, false, false, "a", "b", "c");
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    results = evaluateDecisionTable(false, true, false, "a", "b", "c");
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    results = evaluateDecisionTable(false, false, true, "a", "b", "c");
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
  }

  @Test
  @DecisionResource(resource = RULE_ORDER_COMPOUND)
  void ruleOrderHitPolicyCompoundOutputMultipleMatchingRules() {
    DmnDecisionTableResult results = evaluateDecisionTable(true, true, false, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));
    assertThat(results.get(1)).containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    results = evaluateDecisionTable(true, true, false, "c", "b", "a");
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
    assertThat(results.get(1)).containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    results = evaluateDecisionTable(true, false, true, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));
    assertThat(results.get(1)).containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));

    results = evaluateDecisionTable(true, false, true, "c", "b", "a");
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
    assertThat(results.get(1)).containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    results = evaluateDecisionTable(false, true, true, "a", "b", "c");
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));
    assertThat(results.get(1)).containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));

    results = evaluateDecisionTable(false, true, true, "c", "b", "a");
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));
    assertThat(results.get(1)).containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    results = evaluateDecisionTable(true, true, true, "a", "b", "c");
    assertThat(results).hasSize(3);
    assertThat(results.get(0)).containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));
    assertThat(results.get(1)).containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));
    assertThat(results.get(2)).containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));

    results = evaluateDecisionTable(true, true, true, "c", "b", "a");
    assertThat(results).hasSize(3);
    assertThat(results.get(0)).containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
    assertThat(results.get(1)).containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));
    assertThat(results.get(2)).containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));
  }

  private List<Object> collectSingleOutputEntries(DmnDecisionTableResult results) {
    List<Object> values = new ArrayList<>();
    for (DmnDecisionRuleResult result : results) {
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
}
