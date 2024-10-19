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
package org.operaton.bpm.dmn.engine.test.asserts;

import org.assertj.core.api.AbstractMapAssert;
import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionRuleResultAssert extends AbstractMapAssert<DmnDecisionRuleResultAssert, DmnDecisionRuleResult, String, Object> {

  public DmnDecisionRuleResultAssert(DmnDecisionRuleResult decisionRuleResult) {
    super(decisionRuleResult, DmnDecisionRuleResultAssert.class);
  }

    /**
   * Asserts that the DmnDecisionRuleResult contains a single entry with the specified value.
   * 
   * @param value the value to check for in the DmnDecisionRuleResult
   * @return this DmnDecisionRuleResultAssert for further assertions
   */
  public DmnDecisionRuleResultAssert hasSingleEntry(Object value) {
    hasSize(1);
    containsValue(value);

    return this;
  }

    /**
   * Verifies that the result has a single entry with the specified TypedValue.
   * 
   * @param value the TypedValue to compare with
   * @return this DmnDecisionRuleResultAssert for further assertions
   */
  public DmnDecisionRuleResultAssert hasSingleEntryTyped(TypedValue value) {
    hasSize(1);

    TypedValue actualValue = actual.getSingleEntryTyped();
    failIfTypedValuesAreNotEqual(value, actualValue);

    return this;
  }

    /**
   * Checks if the given expected typed value is equal to the actual typed value.
   * If the actual value is null when it should not be or if the values are not equal, a failure message is generated.
   *
   * @param expectedValue The expected TypedValue
   * @param actualValue The actual TypedValue
   */
  protected void failIfTypedValuesAreNotEqual(TypedValue expectedValue, TypedValue actualValue) {
    if (actualValue == null && expectedValue != null) {
      failWithMessage("Expected value to be '%s' but was null", expectedValue);
    }
    else if (actualValue != null && !actualValue.equals(expectedValue)) {
      failWithMessage("Expected typed value to be '%s' but was '%s'", expectedValue, actualValue);
    }
  }

}
