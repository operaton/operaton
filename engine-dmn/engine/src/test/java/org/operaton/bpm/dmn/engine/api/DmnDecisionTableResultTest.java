/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.engine.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Fail;
import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.junit.Test;

public class DmnDecisionTableResultTest extends DmnEngineTest {

  public static final String NO_OUTPUT_VALUE = "noOutputValue";
  public static final String SINGLE_OUTPUT_VALUE = "singleOutputValue";
  public static final String MULTIPLE_OUTPUT_VALUES = "multipleOutputValues";

  public static final String RESULT_TEST_DMN = "DmnResultTest.dmn";
  public static final String RESULT_TEST_WITH_TYPES_DMN = "DmnResultTypedTest.dmn";
  public static final String RESULT_TEST_WITH_SINGLE_UNNAMED_OUTPUT_DMN = "DmnResultTest.testSingleOutputNoName.dmn";

    /**
   * This method tests the evaluation of a decision table when no result is returned.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testNoResult() {
    DmnDecisionTableResult results = evaluateWithMatchingRules();

    assertThat(results).isEmpty();
    assertThat(results.getFirstResult()).isNull();
    assertThat(results.getSingleResult()).isNull();

    assertThat((Object) results.getSingleEntry()).isNull();
    assertThat((Object) results.getSingleEntryTyped()).isNull();
  }

    /**
   * This method tests the evaluation of a single result from a decision table.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testSingleResult() {
    DmnDecisionTableResult results = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    assertThat(results).hasSize(1);
    assertSingleOutputValue(results.get(0));
    assertSingleOutputValue(results.getFirstResult());
    assertSingleOutputValue(results.getSingleResult());

    assertThat((String) results.getSingleEntry()).isEqualTo("singleValue");
  }

    /**
   * This method tests the evaluation of a decision table with multiple results.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testMultipleResults() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE, SINGLE_OUTPUT_VALUE, MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(3);

    DmnDecisionRuleResult ruleResult = decisionResult.get(0);
    assertNoOutputValue(ruleResult);
    ruleResult = decisionResult.get(1);
    assertSingleOutputValue(ruleResult);
    ruleResult = decisionResult.get(2);
    assertMultipleOutputValues(ruleResult);

    ruleResult = decisionResult.getFirstResult();
    assertNoOutputValue(ruleResult);

    try {
      decisionResult.getSingleResult();
      failBecauseExceptionWasNotThrown(DmnDecisionResultException.class);
    }
    catch (DmnDecisionResultException e){
      assertThat(e)
        .hasMessageStartingWith("DMN-01008")
        .hasMessageContaining("singleValue")
        .hasMessageContaining("multipleValues1")
        .hasMessageContaining("multipleValues2");
    }

    try {
      decisionResult.getSingleEntry();
      failBecauseExceptionWasNotThrown(DmnDecisionResultException.class);
    }
    catch (DmnDecisionResultException e){
      assertThat(e)
        .hasMessageStartingWith("DMN-01008")
        .hasMessageContaining("singleValue")
        .hasMessageContaining("multipleValues1")
        .hasMessageContaining("multipleValues2");
    }
  }

    /**
   * This method tests the scenario where there is no output value generated from a decision table evaluation.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testNoOutputValue() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    assertNoOutputValue(decisionResult.getFirstResult());

    assertThat((Object) decisionResult.getSingleEntry()).isNull();
  }

    /**
   * Test method for evaluating a decision table with a single output value.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testSingleOutputValue() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);
    assertThat(decisionResult).hasSize(1);

    assertSingleOutputValue(decisionResult.getFirstResult());

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("singleValue");
  }

    /**
   * This method tests the evaluation of a decision table with a single output without a name.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_SINGLE_UNNAMED_OUTPUT_DMN)
  public void testSingleOutputNoName() {
    DmnDecisionTableResult decisionResult = evaluateDecisionTable();
    assertThat(decisionResult).hasSize(1);

    assertThat(decisionResult.getFirstResult()).hasSize(1);
    assertThat((String) decisionResult.getFirstResult().getSingleEntry()).isEqualTo("outputValue");
    assertThat(decisionResult.getFirstResult().get(null)).isEqualTo("outputValue");

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("outputValue");
  }

    /**
   * This method tests the evaluation of a DMN decision table with multiple output values. 
   * It verifies that the decision result has a size of 1, asserts the values of the first result, 
   * and expects a DmnDecisionResultException to be thrown when trying to get a single entry.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testMultipleOutputValues() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(1);

    assertMultipleOutputValues(decisionResult.getFirstResult());

    try {
      decisionResult.getSingleEntry();
      failBecauseExceptionWasNotThrown(DmnDecisionResultException.class);
    }
    catch (DmnDecisionResultException e){
      assertThat(e)
        .hasMessageStartingWith("DMN-01007")
        .hasMessageContaining("multipleValues1")
        .hasMessageContaining("multipleValues2");
    }
  }

    /**
   * Tests the collection of output values from a DMN decision table result.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testCollectOutputValues() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(NO_OUTPUT_VALUE, SINGLE_OUTPUT_VALUE, MULTIPLE_OUTPUT_VALUES);
    assertThat(decisionResult).hasSize(3);

    List<String> entryValues = decisionResult.collectEntries("firstOutput");
    assertThat(entryValues).containsExactly("singleValue", "multipleValues1");

    entryValues = decisionResult.collectEntries("secondOutput");
    assertThat(entryValues).containsExactly("multipleValues2");
  }

    /**
   * This method tests the output list of a decision table by evaluating the decision result
   * and checking the size and contents of the resulting list of entry maps.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testOutputList() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE, MULTIPLE_OUTPUT_VALUES);

    List<Map<String, Object>> entryMapList = decisionResult.getResultList();
    assertThat(entryMapList).hasSize(2);

    Map<String, Object> firstResult = entryMapList.get(0);
    assertThat(firstResult).hasSize(1);
    assertThat(firstResult).containsEntry("firstOutput", "singleValue");

    Map<String, Object> secondResult = entryMapList.get(1);
    assertThat(secondResult).hasSize(2);
    assertThat(secondResult).containsEntry("firstOutput", "multipleValues1");
    assertThat(secondResult).containsEntry("secondOutput", "multipleValues2");
  }

    /**
   * This method tests the value mapping functionality by evaluating a decision table with multiple output values
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testValueMap() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(MULTIPLE_OUTPUT_VALUES);

    DmnDecisionRuleResult ruleResult = decisionResult.getSingleResult();
    assertThat(ruleResult).hasSize(2);

    Map<String, Object> entryMap = ruleResult.getEntryMap();
    assertThat(entryMap).hasSize(2);
    assertThat(entryMap).containsEntry("firstOutput", "multipleValues1");
    assertThat(entryMap).containsEntry("secondOutput", "multipleValues2");
  }

    /**
   * Tests the evaluation of a decision table with a single output untyped value.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testSingleOutputUntypedValue() {
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

    /**
   * Test method to verify the behavior of a decision table with a single output typed value.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_TYPES_DMN)
  public void testSingleOutputTypedValue() {
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

    /**
   * This method tests the evaluation of a decision table with a single output value
   * and verifies that the typed value retrieved is equal to an untyped value "singleValue".
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void testSingleEntryUntypedValue() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    TypedValue typedValue = decisionResult.getSingleEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.untypedValue("singleValue"));
  }

    /**
   * This method tests the retrieval of a single entry typed value from a decision table result.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_WITH_TYPES_DMN)
  public void testSingleEntryTypedValue() {
    DmnDecisionTableResult decisionResult = evaluateWithMatchingRules(SINGLE_OUTPUT_VALUE);

    TypedValue typedValue = decisionResult.getSingleEntryTyped();
    assertThat(typedValue).isEqualTo(Variables.stringValue("singleValue"));
  }

  // helper methods

    /**
   * Evaluates the decision table with the provided matching rules.
   * 
   * @param matchingRules the matching rules to evaluate the decision table with
   * @return the result of evaluating the decision table
   */
  protected DmnDecisionTableResult evaluateWithMatchingRules(String... matchingRules) {
    List<String> matchingRulesList = Arrays.asList(matchingRules);
    variables.putValue(NO_OUTPUT_VALUE, matchingRulesList.contains(NO_OUTPUT_VALUE));
    variables.putValue(SINGLE_OUTPUT_VALUE, matchingRulesList.contains(SINGLE_OUTPUT_VALUE));
    variables.putValue(MULTIPLE_OUTPUT_VALUES, matchingRulesList.contains(MULTIPLE_OUTPUT_VALUES));
    return evaluateDecisionTable();
  }

    /**
   * Asserts that the provided decision rule result contains a single output value with the expected value "singleValue".
   * 
   * @param decisionRuleResult the decision rule result to be validated
   */
  protected void assertSingleOutputValue(DmnDecisionRuleResult decisionRuleResult) {
    assertThat(decisionRuleResult.size()).isEqualTo(1);

    String value = (String) decisionRuleResult.get("firstOutput");
    assertThat(value).isEqualTo("singleValue");

    value = (String) decisionRuleResult.get("secondOutput");
    assertThat(value).isNull();

    value = decisionRuleResult.getFirstEntry();
    assertThat(value).isEqualTo("singleValue");

    value = decisionRuleResult.getSingleEntry();
    assertThat(value).isEqualTo("singleValue");
  }

    /**
   * Asserts that the given decision rule result has no output values.
   * 
   * @param decisionRuleResult the decision rule result to be checked
   */
  protected void assertNoOutputValue(DmnDecisionRuleResult decisionRuleResult) {
    assertThat(decisionRuleResult.size()).isEqualTo(0);

    String value = (String) decisionRuleResult.get("firstOutput");
    assertThat(value).isNull();

    value = (String) decisionRuleResult.get("secondOutput");
    assertThat(value).isNull();

    value = decisionRuleResult.getFirstEntry();
    assertThat(value).isNull();

    value = decisionRuleResult.getSingleEntry();
    assertThat(value).isNull();
  }

    /**
   * Asserts that the given decision rule result contains multiple output values. 
   * It checks the size of the result, the values of specific output keys, 
   * and verifies the behavior of getting single entry from the result.
   * 
   * @param decisionRuleResult the decision rule result to be validated
   */
  protected void assertMultipleOutputValues(DmnDecisionRuleResult decisionRuleResult) {
    assertThat(decisionRuleResult.size()).isEqualTo(2);

    String value = (String) decisionRuleResult.get("firstOutput");
    assertThat(value).isEqualTo("multipleValues1");

    value = (String) decisionRuleResult.get("secondOutput");
    assertThat(value).isEqualTo("multipleValues2");

    value = decisionRuleResult.getFirstEntry();
    assertThat(value).isEqualTo("multipleValues1");

    try {
      decisionRuleResult.getSingleEntry();
      Fail.failBecauseExceptionWasNotThrown(DmnDecisionResultException.class);
    }
    catch (DmnDecisionResultException e) {
      assertThat(e)
        .hasMessageStartingWith("DMN-01007")
        .hasMessageContaining("multipleValues1")
        .hasMessageContaining("multipleValues2");
    }
  }

}
