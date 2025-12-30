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
package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * The {@code DmnHitPolicyLogger} class is a specialized logger that provides methods to log and
 * generate exceptions related to issues in decision table hit policies. These methods are
 * designed to handle scenarios where specific hit policy rules or requirements are violated
 * during a decision evaluation process.
 *
 * This class extends {@link DmnLogger} and provides specific methods to construct detailed
 * {@link DmnHitPolicyException} instances for various hit policy-related violations, including:
 * - Violations of the unique and any hit policies.
 * - Errors in handling aggregation, outputs, or event types related to decision tables.
 * - Problems in implementing specific rules required by certain hit policies.
 *
 * Methods in this class typically return a {@link DmnHitPolicyException} instance containing a
 * detailed message that helps in identifying and debugging the cause of the policy violation.
 *
 * The {@code DmnHitPolicyLogger} plays a critical role in providing meaningful feedback and error
 * reporting within the Camunda Decision Model framework, particularly for decision table evaluations.
 */
public class DmnHitPolicyLogger extends DmnLogger {

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that the {@code UNIQUE} hit policy
   * only allows a single rule to match. This exception is thrown when multiple rules match under the
   * {@code UNIQUE} hit policy, which violates its requirements.
   *
   * @param matchingRules the list of decision rules that matched under the {@code UNIQUE} hit policy
   *                      when only a single match was allowed
   * @return a {@link DmnHitPolicyException} with a detailed message specifying the rules that matched
   *         and explaining why the {@code UNIQUE} hit policy was violated
   */
  public DmnHitPolicyException uniqueHitPolicyOnlyAllowsSingleMatchingRule(List<DmnEvaluatedDecisionRule> matchingRules) {
    return new DmnHitPolicyException(exceptionMessage(
      "001",
      "Hit policy '{}' only allows a single rule to match. Actually match rules: '{}'.", HitPolicy.UNIQUE, matchingRules)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that the {@code ANY} hit policy
   * requires all outputs from multiple matching rules to be equal. This exception is thrown when
   * the outputs of the matching rules do not comply with this requirement.
   *
   * @param matchingRules the list of matching decision rules whose outputs are evaluated for equality
   * @return a {@link DmnHitPolicyException} with a detailed message specifying the issue with the outputs
   */
  public DmnHitPolicyException anyHitPolicyRequiresThatAllOutputsAreEqual(List<DmnEvaluatedDecisionRule> matchingRules) {
    return new DmnHitPolicyException(exceptionMessage(
      "002",
      "Hit policy '{}' only allows multiple matching rules with equal output. Matching rules: '{}'.", HitPolicy.ANY, matchingRules)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that aggregation is not
   * applicable to a compound decision output. This exception is thrown when an aggregation
   * operation is attempted on multiple output entries, which is not supported as only one
   * output entry is allowed.
   *
   * @param aggregator the {@link BuiltinAggregator} that was attempted to be applied on the outputs
   * @param outputEntries a map of {@link DmnEvaluatedOutput} instances representing the compound decision outputs
   * @return a {@link DmnHitPolicyException} with a detailed message about the unsupported aggregation operation
   */
  public DmnHitPolicyException aggregationNotApplicableOnCompoundOutput(BuiltinAggregator aggregator, Map<String, DmnEvaluatedOutput> outputEntries) {
    return new DmnHitPolicyException(exceptionMessage(
      "003",
      "Unable to execute aggregation '{}' on compound decision output '{}'. Only one output entry allowed.", aggregator, outputEntries)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} to indicate that the conversion
   * of the provided values to the specified aggregatable types has failed. This exception
   * is used to notify that the conversion process for the specified values and target
   * classes could not be successfully completed.
   *
   * @param values a list of {@link TypedValue} instances that were attempted to be converted.
   * @param targetClasses the target classes representing the aggregatable types to which
   *                      the values were intended to be converted.
   * @return a {@link DmnHitPolicyException} containing a detailed message about the conversion failure.
   */
  public DmnHitPolicyException unableToConvertValuesToAggregatableTypes(List<TypedValue> values, Class<?>... targetClasses) {
    return new DmnHitPolicyException(exceptionMessage(
      "004",
      "Unable to convert value '{}' to a support aggregatable type '{}'.", values, targetClasses)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that the specified event type
   * is unsupported. This exception is used to notify that only {@code DmnDecisionTableEvaluationEventImpl}
   * is supported during preview, and an incompatible event type was received.
   *
   * @param eventType the event type that is unsupported
   * @return a {@link DmnHitPolicyException} with a detailed message about the unsupported event type
   */
  public DmnHitPolicyException unsupportedEventType(String eventType) {
    return new DmnHitPolicyException(exceptionMessage(
      "005",
      "Only DmnDecisionTableEvaluationEventImpl is supported in preview. Received: '{}'.", eventType)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that the specified
   * decision logic type is unsupported. This exception is used to notify that only
   * {@code DmnDecisionTableImpl} is supported for preview, and an incompatible
   * decision logic type was received.
   *
   * @param decisionLogicType the decision logic type that is unsupported
   * @return a {@link DmnHitPolicyException} with a detailed message about the unsupported decision logic type
   */
  public DmnHitPolicyException unsupportedDecisionLogic(String decisionLogicType) {
    return new DmnHitPolicyException(exceptionMessage(
      "006",
      "Only DmnDecisionTableImpl is supported in preview. Received: '{}'.", decisionLogicType)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that the specified hit policy
   * requires at least one matching rule. This exception is thrown when no rules match the conditions
   * while using a hit policy that mandates at least one match.
   *
   * @param hitPolicy the {@link HitPolicy} that requires at least one matching rule
   * @return a {@link DmnHitPolicyException} containing a detailed message about the missing matching rule
   */
  public DmnHitPolicyException hitPolicyRequiresAtLeastOneMatchingRule(HitPolicy hitPolicy) {
    return new DmnHitPolicyException(exceptionMessage(
      "007",
      "Hit policy '{}' requires at least one matching rule.", hitPolicy)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that an output must have a name
   * when there are multiple outputs in the decision table. This exception is thrown in cases where
   * the decision table structure does not comply with this requirement.
   *
   * @return a {@link DmnHitPolicyException} with a specific message describing the issue.
   */
  public DmnHitPolicyException outputMustHaveNameWhenMultipleOutputs() {
    return new DmnHitPolicyException(exceptionMessage(
      "008",
      "Output must have a name when there are multiple outputs.")
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} to indicate that a specified output value
   * is not found in the allowed output values defined for a specific output name.
   *
   * @param outputName the name of the output for which the value is being checked
   * @param outputValue the output value that was expected but not found in the allowed values
   * @param allowedValues the list of allowed output values for the specified output name
   * @return a {@link DmnHitPolicyException} with a detailed message indicating the mismatched output value
   */
  public DmnHitPolicyException outputValueNotFoundInOutputValues(String outputName, Object outputValue, List<?> allowedValues) {
    return new DmnHitPolicyException(exceptionMessage(
      "009",
      "Output value '{}' not found in allowed output values for output '{}'. Allowed values: '{}'.", outputValue, outputName, allowedValues)
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that the priority hit policy
   * requires at least one output value in the decision table outputs. This exception is thrown
   * when the decision table outputs do not contain any value while using the priority hit policy.
   *
   * @return a {@link DmnHitPolicyException} with a specific message describing the issue
   */
  public DmnHitPolicyException priorityHitPolicyRequiresAtLeastOneOutputValue() {
    return new DmnHitPolicyException(exceptionMessage(
      "010",
      "Priority hit policy requires at least one output value in the decision table outputs.")
    );
  }

  /**
   * Creates and returns a {@link DmnHitPolicyException} indicating that the priority hit policy
   * requires at least one matching rule. This is thrown when no matching rules are identified
   * under the priority hit policy.
   *
   * @return a {@link DmnHitPolicyException} with a specific message indicating the issue
   */
  public DmnHitPolicyException priorityHitPolicyRequiresAtLeastOneMatchingRule() {
    return new DmnHitPolicyException(exceptionMessage(
      "011",
      "Priority hit policy requires at least one matching rule.")
    );
  }

  /**
   * Logs a debug message indicating that a type conversion attempt has failed.
   *
   * @param targetType the name of the target type to which the conversion was attempted
   * @param details the exception containing details of the failed conversion attempt
   */
  public void typeConversionAttemptFailed(String targetType, Exception details) {
    logDebug("012", "Type conversion attempt to '{}' failed.", targetType, details);
  }

  /**
   * Logs a debug message indicating that a specific number type was successfully detected
   * for the given values during type conversion analysis.
   *
   * @param typeName the name of the detected number type
   * @param valueCount the number of values that were analyzed
   */
  public void numberTypeDetected(String typeName, int valueCount) {
    logDebug("013", "Number type '{}' detected for {} values.", typeName, valueCount);
  }

}
