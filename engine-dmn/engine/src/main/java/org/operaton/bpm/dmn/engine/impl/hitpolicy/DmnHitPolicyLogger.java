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
package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

public class DmnHitPolicyLogger extends DmnLogger {

    /**
   * Creates a DmnHitPolicyException if the hit policy is set to UNIQUE and multiple rules have been matched.
   * 
   * @param matchingRules the list of decision rules that have been matched
   * @return a DmnHitPolicyException with a specific exception message
   */
  public DmnHitPolicyException uniqueHitPolicyOnlyAllowsSingleMatchingRule(List<DmnEvaluatedDecisionRule> matchingRules) {
    return new DmnHitPolicyException(exceptionMessage(
      "001",
      "Hit policy '{}' only allows a single rule to match. Actually match rules: '{}'.", HitPolicy.UNIQUE, matchingRules)
    );
  }

    /**
   * Creates a new DmnHitPolicyException with a specific message indicating that the any hit policy requires all outputs to be equal for multiple matching rules.
   *
   * @param matchingRules a list of DmnEvaluatedDecisionRule objects representing the matching rules
   * @return a DmnHitPolicyException with a formatted exception message
   */
  public DmnHitPolicyException anyHitPolicyRequiresThatAllOutputsAreEqual(List<DmnEvaluatedDecisionRule> matchingRules) {
    return new DmnHitPolicyException(exceptionMessage(
      "002",
      "Hit policy '{}' only allows multiple matching rules with equal output. Matching rules: '{}'.", HitPolicy.ANY, matchingRules)
    );
  }

    /**
   * Creates a DmnHitPolicyException with a message indicating that aggregation is not applicable on compound decision output.
   * 
   * @param aggregator the BuiltinAggregator used for aggregation
   * @param outputEntries a Map of output entries for the compound decision output
   * @return a DmnHitPolicyException with the appropriate exception message
   */
  public DmnHitPolicyException aggregationNotApplicableOnCompoundOutput(BuiltinAggregator aggregator, Map<String, DmnEvaluatedOutput> outputEntries) {
    return new DmnHitPolicyException(exceptionMessage(
      "003",
      "Unable to execute aggregation '{}' on compound decision output '{}'. Only one output entry allowed.", aggregator, outputEntries)
    );
  }

    /**
   * Creates a DmnHitPolicyException with a specific exception message indicating that values cannot be converted to aggregatable types.
   *
   * @param values The list of TypedValue objects that could not be converted
   * @param targetClasses The target classes to which the values could not be converted
   * @return a new DmnHitPolicyException with the formatted exception message
   */
  public DmnHitPolicyException unableToConvertValuesToAggregatableTypes(List<TypedValue> values, Class<?>... targetClasses) {
    return new DmnHitPolicyException(exceptionMessage(
      "004",
      "Unable to convert value '{}' to a support aggregatable type '{}'.", values, targetClasses)
    );
  }

}
