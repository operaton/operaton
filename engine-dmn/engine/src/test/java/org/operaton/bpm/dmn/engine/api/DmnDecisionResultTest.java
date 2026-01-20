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
package org.operaton.bpm.dmn.engine.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmnDecisionResultTest extends DmnEngineTest {

  private static final String NO_OUTPUT_VALUE = "noOutputValue";
  private static final String SINGLE_OUTPUT_VALUE = "singleOutputValue";
  private static final String MULTIPLE_OUTPUT_VALUES = "multipleOutputValues";

  private static final String RESULT_TEST_DMN = "DmnResultTest.dmn";
  private static final String RESULT_TEST_WITH_TYPES_DMN = "DmnResultTypedTest.dmn";
  private static final String RESULT_TEST_WITH_SINGLE_UNNAMED_OUTPUT_DMN = "DmnResultTest.testSingleOutputNoName.dmn";

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void noResult() {
    DmnDecisionResult results = evaluateWithMatchingRules();

    assertThat(results).isEmpty();
    assertThat(results.getFirstResult()).isNull();
    assertThat(results.getSingleResult()).isNull();

    Object result = results.getSingleEntry();
    assertThat(result).isNull();
    assertThat((Object) results.getSingleEntryTyped()).isNull();
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void singleResult() {
    DmnDecisionResult results = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    assertThat(results).hasSize(1);
    assertSingleOutputValue(results.get(0));
    assertSingleOutputValue(results.getFirstResult());
    assertSingleOutputValue(results.getSingleResult());

    assertThat((String) results.getSingleEntry()).isEqualTo("singleValue");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void multipleResults() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE, SINGLE_OUTPUT_VALUE,
      MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(3);

    DmnDecisionResultEntries ruleResult = decisionResult.get(0);
    assertNoOutputValue(ruleResult);
    ruleResult = decisionResult.get(1);
    assertSingleOutputValue(ruleResult);
    ruleResult = decisionResult.get(2);
    assertMultipleOutputValues(ruleResult);

    ruleResult = decisionResult.getFirstResult();
    assertNoOutputValue(ruleResult);

    assertThatThrownBy(decisionResult::getSingleResult)
      .isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01011")
      .hasMessageContaining("singleValue")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");

    assertThatThrownBy(decisionResult::getSingleEntry)
      .isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01011")
      .hasMessageContaining("singleValue")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void noOutputValue() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    assertNoOutputValue(decisionResult.getFirstResult());

    Object result = decisionResult.getSingleEntry();
    assertThat(result).isNull();
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void singleOutputValue() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    assertSingleOutputValue(decisionResult.getFirstResult());

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("singleValue");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_SINGLE_UNNAMED_OUTPUT_DMN)
  void singleOutputNoName() {
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, variables);
    assertThat(decisionResult).hasSize(1);

    assertThat(decisionResult.getFirstResult()).hasSize(1);
    assertThat((String) decisionResult.getFirstResult().getSingleEntry()).isEqualTo("outputValue");
    assertThat(decisionResult.getFirstResult()).containsEntry(null, "outputValue");

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("outputValue");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void multipleOutputValues() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(1);

    assertMultipleOutputValues(decisionResult.getFirstResult());

    assertThatThrownBy(decisionResult::getSingleEntry).isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01010")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void collectOutputValues() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE, SINGLE_OUTPUT_VALUE,
      MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(3);

    List<String> entryValues = decisionResult.collectEntries("firstOutput");
    assertThat(entryValues).containsExactly("singleValue", "multipleValues1");

    entryValues = decisionResult.collectEntries("secondOutput");
    assertThat(entryValues).containsExactly("multipleValues2");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void outputList() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE, MULTIPLE_OUTPUT_VALUES);

    List<Map<String, Object>> entryMapList = decisionResult.getResultList();
    assertThat(entryMapList).hasSize(2);

    Map<String, Object> firstResult = entryMapList.get(0);
    assertThat(firstResult)
      .hasSize(1)
      .containsEntry("firstOutput", "singleValue");

    Map<String, Object> secondResult = entryMapList.get(1);
    assertThat(secondResult)
      .hasSize(2)
      .containsEntry("firstOutput", "multipleValues1")
      .containsEntry("secondOutput", "multipleValues2");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void valueMap() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(MULTIPLE_OUTPUT_VALUES);

    DmnDecisionResultEntries ruleResult = decisionResult.getSingleResult();
    assertThat(ruleResult).hasSize(2);

    Map<String, Object> entryMap = ruleResult.getEntryMap();
    assertThat(entryMap)
      .hasSize(2)
      .containsEntry("firstOutput", "multipleValues1")
      .containsEntry("secondOutput", "multipleValues2");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void singleOutputUntypedValue() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    DmnDecisionResultEntries ruleResult = decisionResult.getFirstResult();

    TypedValue typedEntry = ruleResult.getEntryTyped("firstOutput");
    assertThat(typedEntry).isEqualTo(Variables.untypedValue("singleValue"));

    typedEntry = ruleResult.getEntryTyped("secondOutput");
    assertThat(typedEntry).isNull();

    typedEntry = ruleResult.getFirstEntryTyped();
    assertThat(typedEntry).isEqualTo(Variables.untypedValue("singleValue"));

    typedEntry = ruleResult.getSingleEntryTyped();
    assertThat(typedEntry).isEqualTo(Variables.untypedValue("singleValue"));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_TYPES_DMN)
  void singleOutputTypedValue() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    DmnDecisionResultEntries ruleResult = decisionResult.getFirstResult();

    TypedValue typedValue = ruleResult.getEntryTyped("firstOutput");
    assertThat(typedValue).isEqualTo(Variables.stringValue("singleValue"));

    typedValue = ruleResult.getEntryTyped("secondOutput");
    assertThat(typedValue).isNull();

    typedValue = ruleResult.getFirstEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.stringValue("singleValue"));

    typedValue = ruleResult.getSingleEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.stringValue("singleValue"));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void singleEntryUntypedValue() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    TypedValue typedValue = decisionResult.getSingleEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.untypedValue("singleValue"));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_TYPES_DMN)
  void singleEntryTypedValue() {
    DmnDecisionResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    TypedValue typedValue = decisionResult.getSingleEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.stringValue("singleValue"));
  }

  // helper methods

  private DmnDecisionResult evaluateWithMatchingRules(String... matchingRules) {
    List<String> matchingRulesList = List.of(matchingRules);
    variables.putValue(NO_OUTPUT_VALUE, matchingRulesList.contains(NO_OUTPUT_VALUE));
    variables.putValue(SINGLE_OUTPUT_VALUE, matchingRulesList.contains(SINGLE_OUTPUT_VALUE));
    variables.putValue(MULTIPLE_OUTPUT_VALUES, matchingRulesList.contains(MULTIPLE_OUTPUT_VALUES));

    return dmnEngine.evaluateDecision(decision, variables);
  }

  private void assertSingleOutputValue(DmnDecisionResultEntries result) {
    assertThat(result).hasSize(1);

    String value = (String) result.get("firstOutput");
    assertThat(value).isEqualTo("singleValue");

    value = (String) result.get("secondOutput");
    assertThat(value).isNull();

    value = result.getFirstEntry();
    assertThat(value).isEqualTo("singleValue");

    value = result.getSingleEntry();
    assertThat(value).isEqualTo("singleValue");
  }

  private void assertNoOutputValue(DmnDecisionResultEntries result) {
    assertThat(result).isEmpty();
  }

  private void assertMultipleOutputValues(DmnDecisionResultEntries result) {
    assertThat(result).hasSize(2);

    String value = (String) result.get("firstOutput");
    assertThat(value).isEqualTo("multipleValues1");

    value = (String) result.get("secondOutput");
    assertThat(value).isEqualTo("multipleValues2");

    value = result.getFirstEntry();
    assertThat(value).isEqualTo("multipleValues1");

    assertThatThrownBy(result::getSingleEntry).isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01010")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");
  }

}
