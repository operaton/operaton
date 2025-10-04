package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the PRIORITY hit policy as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The PRIORITY hit policy returns only the rule with the highest priority among all matching rules.
 * Priority is determined by the order in which output values are listed in the decision table's output
 * definition. The rule whose output value appears first in the output value list has the highest priority.</p>
 *
 * <p><strong>Difference to OUTPUT_ORDER:</strong> While OUTPUT_ORDER returns all matching rules sorted
 * by their output values lexicographically, PRIORITY returns only the single highest-priority rule
 * and requires at least one output value to be defined for validation purposes.</p>
 *
 * <p>This handler extends {@link SortingHitPolicyHandler}, using its sorting infrastructure
 * with a comparator based on output value priority to select the single highest-priority rule.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Returns only the highest-priority rule (not all matching rules)</li>
 *   <li>Priority is based on output value order in the decision table definition</li>
 *   <li>Requires at least one output value to be defined</li>
 *   <li>Uses comparator to find the minimum (highest priority) rule</li>
 * </ul>
 */
public final class PriorityHitPolicyHandler extends SortingHitPolicyHandler {

  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.PRIORITY, null);
  private static final int MINIMUM_REQUIRED_OUTPUT_VALUES = 1;

  /**
   * Validates the outputs of the decision table for adherence to the PRIORITY hit policy.
   *
   * <p>The PRIORITY hit policy requires at least one output value to be defined in the
   * decision table outputs. This validation ensures that the priority comparison can be
   * performed based on the output value definitions. If no output values are defined,
   * priority cannot be determined, and an exception is thrown.</p>
   *
   * @param matchingRules the list of decision rules that matched, which may be used for
   *                      validation or further processing
   * @param decisionTableOutput the list of decision table outputs to be checked for
   *                             compliance with the minimum required output values
   * @throws DmnHitPolicyException if the number of outputs with values is less than
   *                               the required minimum
   */
  @Override
  protected void checkOutputs(List<DmnEvaluatedDecisionRule> matchingRules,
                              List<DmnDecisionTableOutputImpl> decisionTableOutput) {
    long outputValuesCount = countOutputsWithValues(decisionTableOutput);
    validateMinimumOutputValues(outputValuesCount);
  }

  /**
   * Counts the number of decision table outputs that contain values.
   *
   * @param decisionTableOutput the list of decision table outputs to be examined
   * @return the count of outputs containing values
   */
  private long countOutputsWithValues(List<DmnDecisionTableOutputImpl> decisionTableOutput) {
    return decisionTableOutput.stream().filter(this::hasOutputValues).count();
  }

  /**
   * Determines if the given decision table output contains any output values.
   *
   * @param output the decision table output to be checked
   * @return true if the output contains non-empty output values, false otherwise
   */
  private boolean hasOutputValues(DmnDecisionTableOutputImpl output) {
    return output.getOutputValues() != null && !output.getOutputValues().isEmpty();
  }

  /**
   * Validates whether the number of output values in the decision table meets the minimum
   * required for the PRIORITY hit policy. If the number of output values is less than
   * the required minimum, an exception is thrown.
   *
   * @param outputValuesCount the count of output values in the decision table
   *                          to be validated against the minimum requirement
   * @throws DmnHitPolicyException if the count of output values is below the required minimum
   */
  private void validateMinimumOutputValues(long outputValuesCount) {
    if (outputValuesCount < MINIMUM_REQUIRED_OUTPUT_VALUES) {
      throw LOG.priorityHitPolicyRequiresAtLeastOneOutputValue();
    }
  }

  /**
   * Evaluates the decision rules using the PRIORITY hit policy and returns the highest
   * priority rule among the matching rules.
   *
   * <p>The ruleComparator determines priority based on the output value order defined in
   * the decision table. The rule with the minimum comparator value (highest priority) is
   * selected. Empty streams are already handled by the parent class validation logic.</p>
   *
   * @param matchingRulesStream a stream of decision rules that match the evaluation criteria
   * @param ruleComparator a comparator used to determine the highest priority rule among the matching rules
   * @return a list containing the single decision rule with the highest priority
   *         according to the specified rule comparator
   */
  @Override
  protected List<DmnEvaluatedDecisionRule> evaluatePolicy(Stream<DmnEvaluatedDecisionRule> matchingRulesStream,
                                                          Comparator<DmnEvaluatedDecisionRule> ruleComparator) {
    DmnEvaluatedDecisionRule highestPriorityRule = findHighestPriorityRule(matchingRulesStream, ruleComparator);
    return List.of(highestPriorityRule);
  }

  /**
   * Finds and returns the highest priority rule from a stream of matching decision rules
   * based on a provided comparator.
   *
   * @param matchingRulesStream a stream of evaluated decision rules that match the evaluation criteria
   * @param ruleComparator a comparator used to evaluate and determine the highest priority rule
   *                       among the matching rules
   * @return the evaluated decision rule with the highest priority according to the provided comparator
   * @throws DmnHitPolicyException if no matching rule is found in the provided stream
   */
  private DmnEvaluatedDecisionRule findHighestPriorityRule(Stream<DmnEvaluatedDecisionRule> matchingRulesStream,
                                                           Comparator<DmnEvaluatedDecisionRule> ruleComparator) {
    return matchingRulesStream.min(ruleComparator)
        .orElseThrow(LOG::priorityHitPolicyRequiresAtLeastOneMatchingRule);
  }

  /**
   * Retrieves the hit policy entry associated with this handler.
   * The hit policy entry represents the specific hit policy and optional aggregator
   * used for evaluating decision tables.
   *
   * @return the hit policy entry defining the PRIORITY hit policy (without aggregator)
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns a string representation of the PriorityHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy
   */
  @Override
  public String toString() {
    return "PriorityHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + "}";
  }
}
