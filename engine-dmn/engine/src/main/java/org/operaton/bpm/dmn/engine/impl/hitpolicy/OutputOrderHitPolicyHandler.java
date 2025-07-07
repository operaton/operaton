package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * This class implements the handling for the OUTPUT_ORDER hit policy in a decision table.
 *
 * The OUTPUT_ORDER hit policy evaluates all matching rules of a decision table while
 * preserving their order based on a specified rule comparator. This ensures that the output
 * is consistent with the defined order in the decision table, sorted as per the provided comparator.
 *
 * This handler extends the {@code SortingHitPolicyHandler}, leveraging its sorting capabilities
 * to enforce the OUTPUT_ORDER policy.
 *
 * Key Features:
 * - Retains and evaluates all matching rules.
 * - Sorts evaluated rules in accordance with a specified comparator.
 * - Does not perform additional checks specific to OUTPUT_ORDER.
 */
public final class OutputOrderHitPolicyHandler extends SortingHitPolicyHandler {

  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.OUTPUT_ORDER, null);

  /**
   * Performs validation of the outputs of matching decision rules for the OUTPUT_ORDER hit policy.
   *
   * The OUTPUT_ORDER hit policy does not impose additional validation checks on the evaluated
   * outputs, so this method currently does not perform any checks or operations.
   *
   * @param matchingRules the list of matching decision rules that have been evaluated
   * @param decisionTableOutput the list of decision table outputs corresponding to the evaluated rules
   */
  @Override
  protected void checkOutputs(List<DmnEvaluatedDecisionRule> matchingRules,
                              List<DmnDecisionTableOutputImpl> decisionTableOutput) {
    // No specific checks for OUTPUT_ORDER hit policy
  }

  /**
   * Evaluates all matching decision rules using the OUTPUT_ORDER hit policy and sorts them
   * based on the specified comparator.
   *
   * @param matchingRulesStream the stream of matching decision rules to be evaluated
   * @param ruleComparator the comparator used to sort the evaluated decision rules
   * @return a list of evaluated decision rules sorted according to the provided comparator
   */
  @Override
  protected List<DmnEvaluatedDecisionRule> evaluatePolicy(Stream<DmnEvaluatedDecisionRule> matchingRulesStream,
                                                          Comparator<DmnEvaluatedDecisionRule> ruleComparator) {
    return matchingRulesStream.sorted(ruleComparator).toList();
  }

  /**
   * Retrieves the hit policy entry associated with this handler.
   * The hit policy entry represents the specific hit policy and optional aggregator
   * used for evaluating decision tables.
   *
   * @return the hit policy entry defining the hit policy and associated aggregator
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns a string representation of the OutputOrderHitPolicyHandler.
   *
   * @return a string that represents the OutputOrderHitPolicyHandler instance
   */
  @Override
  public String toString() {
    return "OutputOrderHitPolicyHandler{}";
  }
}
