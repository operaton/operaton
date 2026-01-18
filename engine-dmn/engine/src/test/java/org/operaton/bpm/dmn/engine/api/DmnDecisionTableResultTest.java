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

import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmnDecisionTableResultTest extends DmnEngineTest {

  private static final String NO_OUTPUT_VALUE = "noOutputValue";
  private static final String SINGLE_OUTPUT_VALUE = "singleOutputValue";
  private static final String MULTIPLE_OUTPUT_VALUES = "multipleOutputValues";

  private static final String RESULT_TEST_DMN = "DmnResultTest.dmn";
  private static final String RESULT_TEST_WITH_TYPES_DMN = "DmnResultTypedTest.dmn";
  private static final String RESULT_TEST_WITH_SINGLE_UNNAMED_OUTPUT_DMN = "DmnResultTest.testSingleOutputNoName.dmn";

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void noResult() {
    DmnDecisionTableResult results = evaluateWithMatchingRules();

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
    DmnDecisionTableResult results = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    assertThat(results).hasSize(1);
    assertSingleOutputValue(results.get(0));
    assertSingleOutputValue(results.getFirstResult());
    assertSingleOutputValue(results.getSingleResult());

    assertThat((String) results.getSingleEntry()).isEqualTo("singleValue");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void multipleResults() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE, SINGLE_OUTPUT_VALUE,
      MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(3);

    DmnDecisionRuleResult ruleResult = decisionResult.get(0);
    assertNoOutputValue(ruleResult);
    ruleResult = decisionResult.get(1);
    assertSingleOutputValue(ruleResult);
    ruleResult = decisionResult.get(2);
    assertMultipleOutputValues(ruleResult);

    ruleResult = decisionResult.getFirstResult();
    assertNoOutputValue(ruleResult);

    assertThatThrownBy(decisionResult::getSingleResult).isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01008")
      .hasMessageContaining("singleValue")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");

    assertThatThrownBy(decisionResult::getSingleEntry).isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01008")
      .hasMessageContaining("singleValue")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void noOutputValue() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    assertNoOutputValue(decisionResult.getFirstResult());

    Object result = decisionResult.getSingleEntry();
    assertThat(result).isNull();
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void singleOutputValue() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    assertSingleOutputValue(decisionResult.getFirstResult());

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("singleValue");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_SINGLE_UNNAMED_OUTPUT_DMN)
  void singleOutputNoName() {
    DmnDecisionTableResult decisionResult = evaluateDecisionTable();
    assertThat(decisionResult).hasSize(1);

    assertThat(decisionResult.getFirstResult()).hasSize(1);
    assertThat((String) decisionResult.getFirstResult().getSingleEntry()).isEqualTo("outputValue");
    assertThat(decisionResult.getFirstResult()).containsEntry(null, "outputValue");

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("outputValue");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void multipleOutputValues() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(1);

    assertMultipleOutputValues(decisionResult.getFirstResult());

    assertThatThrownBy(decisionResult::getSingleEntry).isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01007")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  void collectOutputValues() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE, SINGLE_OUTPUT_VALUE,
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
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE, MULTIPLE_OUTPUT_VALUES);

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
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(MULTIPLE_OUTPUT_VALUES);

    DmnDecisionRuleResult ruleResult = decisionResult.getSingleResult();
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
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    DmnDecisionRuleResult ruleResult = decisionResult.getFirstResult();

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
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    DmnDecisionRuleResult ruleResult = decisionResult.getFirstResult();

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
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    TypedValue typedValue = decisionResult.getSingleEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.untypedValue("singleValue"));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_TYPES_DMN)
  void singleEntryTypedValue() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    TypedValue typedValue = decisionResult.getSingleEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.stringValue("singleValue"));
  }

  // helper methods

  private DmnDecisionTableResult evaluateWithMatchingRules(String... matchingRules) {
    List<String> matchingRulesList = List.of(matchingRules);
    variables.putValue(NO_OUTPUT_VALUE, matchingRulesList.contains(NO_OUTPUT_VALUE));
    variables.putValue(SINGLE_OUTPUT_VALUE, matchingRulesList.contains(SINGLE_OUTPUT_VALUE));
    variables.putValue(MULTIPLE_OUTPUT_VALUES, matchingRulesList.contains(MULTIPLE_OUTPUT_VALUES));
    return evaluateDecisionTable();
  }

  private void assertSingleOutputValue(DmnDecisionRuleResult decisionRuleResult) {
    assertThat(decisionRuleResult).hasSize(1);

    String value = (String) decisionRuleResult.get("firstOutput");
    assertThat(value).isEqualTo("singleValue");

    value = (String) decisionRuleResult.get("secondOutput");
    assertThat(value).isNull();

    value = decisionRuleResult.getFirstEntry();
    assertThat(value).isEqualTo("singleValue");

    value = decisionRuleResult.getSingleEntry();
    assertThat(value).isEqualTo("singleValue");
  }

  private void assertNoOutputValue(DmnDecisionRuleResult decisionRuleResult) {
    assertThat(decisionRuleResult).isEmpty();
  }

  private void assertMultipleOutputValues(DmnDecisionRuleResult decisionRuleResult) {
    assertThat(decisionRuleResult).hasSize(2);

    String value = (String) decisionRuleResult.get("firstOutput");
    assertThat(value).isEqualTo("multipleValues1");

    value = (String) decisionRuleResult.get("secondOutput");
    assertThat(value).isEqualTo("multipleValues2");

    value = decisionRuleResult.getFirstEntry();
    assertThat(value).isEqualTo("multipleValues1");

    assertThatThrownBy(decisionRuleResult::getSingleEntry)
      .isInstanceOf(DmnDecisionResultException.class)
      .hasMessageStartingWith("DMN-01007")
      .hasMessageContaining("multipleValues1")
      .hasMessageContaining("multipleValues2");
  }

}
