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
package org.operaton.bpm.dmn.engine.hitpolicy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.DmnHitPolicyException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;

import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class HitPolicyTest extends DmnEngineTest {

  private static final Double DOUBLE_MIN = -Double.MAX_VALUE;

  private static final String DEFAULT_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.default.single.dmn";
  private static final String DEFAULT_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.default.compound.dmn";
  private static final String UNIQUE_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.unique.single.dmn";
  private static final String UNIQUE_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.unique.compound.dmn";
  private static final String ANY_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.any.single.dmn";
  private static final String ANY_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.any.compound.dmn";
  private static final String FIRST_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.first.single.dmn";
  private static final String FIRST_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.first.compound.dmn";
  private static final String RULE_ORDER_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.ruleOrder.single.dmn";
  private static final String RULE_ORDER_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.ruleOrder.compound.dmn";
  private static final String COLLECT_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.single.dmn";
  private static final String COLLECT_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.compound.dmn";
  private static final String COLLECT_SUM_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.sum.single.dmn";
  private static final String COLLECT_SUM_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.sum.compound.dmn";
  private static final String COLLECT_MIN_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.min.single.dmn";
  private static final String COLLECT_MIN_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.min.compound.dmn";
  private static final String COLLECT_MAX_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.max.single.dmn";
  private static final String COLLECT_MAX_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.max.compound.dmn";
  private static final String COLLECT_COUNT_SINGLE = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.count.single.dmn";
  private static final String COLLECT_COUNT_COMPOUND = "org/operaton/bpm/dmn/engine/hitpolicy/HitPolicyTest.collect.count.compound.dmn";

  @Test
  @DecisionResource(resource = DEFAULT_SINGLE)
  void defaultHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = DEFAULT_SINGLE)
  void defaultHitPolicySingleOutputSingleMatchingRule() {
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
  @DecisionResource(resource = DEFAULT_SINGLE)
  void defaultHitPolicySingleOutputMultipleMatchingRules() {
    assertThatThrownBy(() -> evaluateDecisionTable(true, true, false, "a", "b", "c"))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03001");

    assertThatThrownBy(() -> evaluateDecisionTable(true, false, true, "a", "b", "c"))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03001");

    assertThatThrownBy(() -> evaluateDecisionTable(false, true, true, "a", "b", "c"))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03001");

    assertThatThrownBy(() -> evaluateDecisionTable(true, true, true, "a", "b", "c"))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03001");
  }

  @Test
  @DecisionResource(resource = DEFAULT_COMPOUND)
  void defaultHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = DEFAULT_COMPOUND)
  void defaultHitPolicyCompoundOutputSingleMatchingRule() {
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

  @ParameterizedTest
  @CsvSource({
    "true, true, false, a, b, c",
    "true, false, true, a, b, c",
    "false, true, true, a, b, c",
    "true, true, true, a, b, c"
  })
  @DecisionResource(resource = DEFAULT_COMPOUND)
  void defaultHitPolicyCompoundOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3, String output1, String output2, String output3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, output1, output2, output3))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03001");
  }

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
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    assertThatDecisionTableResult(false, true, false, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    assertThatDecisionTableResult(false, false, true, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
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
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));

    assertThatDecisionTableResult(false, true, false, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "b"), entry("out2", "b"), entry("out3", "b"));

    assertThatDecisionTableResult(false, false, true, "a", "b", "c")
      .hasSingleResult()
      .containsOnly(entry("out1", "c"), entry("out2", "c"), entry("out3", "c"));
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
      .containsOnly(entry("out1", "a"), entry("out2", "a"), entry("out3", "a"));
  }

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

  @Test
  @DecisionResource(resource = RULE_ORDER_SINGLE)
  void ruleOrderHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = RULE_ORDER_SINGLE)
  void ruleOrderHitPolicySingleOutputSingleMatchingRule() {
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
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = RULE_ORDER_COMPOUND)
  void ruleOrderHitPolicyCompoundOutputSingleMatchingRule() {
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

  @Test
  @DecisionResource(resource = COLLECT_SUM_SINGLE)
  void collectSumHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, 10, 20L, 30.034)
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = COLLECT_SUM_SINGLE)
  void collectSumHitPolicySingleOutputSingleMatchingRule() {
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
  @DecisionResource(resource = COLLECT_SUM_SINGLE)
  void collectSumHitPolicySingleOutputMultipleMatchingRules() {
    assertThatDecisionTableResult(true, true, false, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(30L);

    assertThatDecisionTableResult(true, false, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(40.034);

    assertThatDecisionTableResult(false, true, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(50.034);

    assertThatDecisionTableResult(true, true, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(60.034);

    assertThatDecisionTableResult(true, true, false, MAX_VALUE, Long.MAX_VALUE - MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Long.MAX_VALUE);

    assertThatDecisionTableResult(true, true, false, MIN_VALUE, Long.MIN_VALUE - MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(Long.MIN_VALUE);

    assertThatDecisionTableResult(true, false, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE - MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Double.MAX_VALUE);

    assertThatDecisionTableResult(true, false, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN - MIN_VALUE)
      .hasSingleResult()
      .hasSingleEntry(DOUBLE_MIN);

    assertThatDecisionTableResult(false, true, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE - Long.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Double.MAX_VALUE);

    assertThatDecisionTableResult(false, true, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN - Long.MIN_VALUE)
      .hasSingleResult()
      .hasSingleEntry(DOUBLE_MIN);

    assertThatDecisionTableResult(true, true, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE - MAX_VALUE - Long.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry(Double.MAX_VALUE);

    assertThatDecisionTableResult(true, true, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN - MIN_VALUE - Long.MIN_VALUE)
      .hasSingleResult()
      .hasSingleEntry(DOUBLE_MIN);

    assertThatDecisionTableResult(true, true, false, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(3L);

    assertThatDecisionTableResult(true, false, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(4.0);

    assertThatDecisionTableResult(false, true, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(5.0);

    assertThatDecisionTableResult(true, true, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(6.0);

    assertThatThrownBy(() -> evaluateDecisionTable(true, true, true, 10, 20L, true))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03004");
  }

  @Test
  @DecisionResource(resource = COLLECT_SUM_COMPOUND)
  void collectSumHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "true, false, false",
    "false, true, false",
    "false, false, true"
  })
  @DecisionResource(resource = COLLECT_SUM_COMPOUND)
  void collectSumHitPolicyCompoundOutputSingleMatchingRule(boolean input1, boolean input2, boolean input3) {
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
  @DecisionResource(resource = COLLECT_SUM_COMPOUND)
  void collectSumHitPolicyCompoundOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, 1, 2L, 3d))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03003");
  }

  @Test
  @DecisionResource(resource = COLLECT_MIN_SINGLE)
  void collectMinHitPolicySingleOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, 10, 20L, 30.034)
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = COLLECT_MIN_SINGLE)
  void collectMinHitPolicySingleOutputSingleMatchingRule() {
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
  @DecisionResource(resource = COLLECT_MIN_SINGLE)
  void collectMinHitPolicySingleOutputMultipleMatchingRules() {
    assertThatDecisionTableResult(true, true, false, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(10L);

    assertThatDecisionTableResult(true, false, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(10.0);

    assertThatDecisionTableResult(false, true, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(20.0);

    assertThatDecisionTableResult(true, true, true, 10, 20L, 30.034)
      .hasSingleResult()
      .hasSingleEntry(10.0);

    assertThatDecisionTableResult(true, true, false, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry((long) MAX_VALUE);

    assertThatDecisionTableResult(true, true, false, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(Long.MIN_VALUE);

    assertThatDecisionTableResult(true, false, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry((double) MAX_VALUE);

    assertThatDecisionTableResult(true, false, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(DOUBLE_MIN);

    assertThatDecisionTableResult(false, true, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry((double) Long.MAX_VALUE);

    assertThatDecisionTableResult(false, true, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(DOUBLE_MIN);

    assertThatDecisionTableResult(true, true, true, MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE)
      .hasSingleResult()
      .hasSingleEntry((double) MAX_VALUE);

    assertThatDecisionTableResult(true, true, true, MIN_VALUE, Long.MIN_VALUE, DOUBLE_MIN)
      .hasSingleResult()
      .hasSingleEntry(DOUBLE_MIN);

    assertThatDecisionTableResult(true, true, false, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(1L);

    assertThatDecisionTableResult(true, false, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(1.0);

    assertThatDecisionTableResult(false, true, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(2.0);

    assertThatDecisionTableResult(true, true, true, (byte) 1, (short) 2, 3f)
      .hasSingleResult()
      .hasSingleEntry(1.0);

    assertThatThrownBy(() -> evaluateDecisionTable(true, true, true, 10, 20L, true))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03004");
  }

  @Test
  @DecisionResource(resource = COLLECT_MIN_COMPOUND)
  void collectMinHitPolicyCompoundOutputNoMatchingRule() {
    assertThatDecisionTableResult(false, false, false, "a", "b", "c")
      .isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "true, false, false",
    "false, true, false",
    "false, false, true"
  })
  @DecisionResource(resource = COLLECT_MIN_COMPOUND)
  void collectMinHitPolicyCompoundOutputSingleMatchingRule(boolean input1, boolean input2, boolean input3) {
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
  @DecisionResource(resource = COLLECT_MIN_COMPOUND)
  void collectMinHitPolicyCompoundOutputMultipleMatchingRules(boolean input1, boolean input2, boolean input3) {
    assertThatThrownBy(() -> evaluateDecisionTable(input1, input2, input3, 1, 2L, 3d))
      .isInstanceOf(DmnHitPolicyException.class)
      .hasMessageStartingWith("DMN-03003");
  }

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

  // helper methods

  List<Object> collectSingleOutputEntries(DmnDecisionTableResult results) {
    List<Object> values = new ArrayList<>();
    for (DmnDecisionRuleResult result : results) {
      values.add(result.getSingleEntry());
    }
    return values;
  }

  DmnDecisionTableResult evaluateDecisionTable(Boolean input1, Boolean input2, Boolean input3, Object output1, Object output2, Object output3) {
    variables.put("input1", input1);
    variables.put("input2", input2);
    variables.put("input3", input3);
    variables.put("output1", output1);
    variables.put("output2", output2);
    variables.put("output3", output3);

    return evaluateDecisionTable();
  }

  DmnDecisionTableResultAssert assertThatDecisionTableResult(Boolean input1, Boolean input2, Boolean input3, Object output1, Object output2, Object output3) {
    return assertThat(evaluateDecisionTable(input1, input2, input3, output1, output2, output3));
  }

}
