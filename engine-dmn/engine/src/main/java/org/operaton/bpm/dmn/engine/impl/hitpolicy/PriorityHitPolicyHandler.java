package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * PriorityHitPolicyHandler is a concrete implementation of the SortingHitPolicyHandler
 * class, which enforces the "Priority" hit policy for decision tables in a DMN engine.
 *
 * The "Priority" hit policy ensures that out of all the matching decision rules,
 * only the rule with the highest priority (as determined by a specified comparator)
 * is selected as the applicable rule. At least one output value is required in
 * the decision table outputs for this policy to apply.
 */
public final class PriorityHitPolicyHandler extends SortingHitPolicyHandler {

  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.PRIORITY, null);
  private static final int MINIMUM_REQUIRED_OUTPUT_VALUES = 1;

  /**
   * Validates the outputs of the decision table for adherence to the "Priority" hit policy.
   * Specifically, this method ensures that at least one output value is present in the
   * decision table outputs. If the validation fails, an exception is thrown.
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
   * required for the "Priority" hit policy. If the number of output values is less than
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
   * Evaluates the decision rules using the "Priority" hit policy and returns the highest
   * priority rule among the matching rules.
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
   * Retrieves the {@link HitPolicyEntry} associated with this handler.
   *
   * @return the hit policy entry defined for this handler.
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns a string representation of the PriorityHitPolicyHandler object.
   *
   * @return a string describing the PriorityHitPolicyHandler instance
   */
  @Override
  public String toString() {
    return "PriorityHitPolicyHandler{}";
  }
}
