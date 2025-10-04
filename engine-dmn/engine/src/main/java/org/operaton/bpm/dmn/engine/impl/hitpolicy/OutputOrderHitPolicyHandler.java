package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the OUTPUT_ORDER hit policy as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The OUTPUT_ORDER hit policy returns all matching rules, sorted by their output values in
 * decreasing order. The output order is determined lexicographically based on the sequence
 * of outputs defined in the decision table.</p>
 *
 * <p><strong>Difference to PRIORITY:</strong> While PRIORITY uses the order of rules in the
 * decision table and requires at least one output value for validation, OUTPUT_ORDER uses
 * the lexicographical order of output values and returns all matching rules without
 * additional validation constraints.</p>
 *
 * <p>This handler extends {@link SortingHitPolicyHandler}, using its sorting infrastructure
 * with a comparator based on output values rather than rule priority.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Returns all matching rules (not just the first)</li>
 *   <li>Sorts by output value order (lexicographical)</li>
 *   <li>No additional output validation required beyond parent class</li>
 *   <li>Comparator evaluates outputs in the sequence defined by the decision table model</li>
 * </ul>
 */
public final class OutputOrderHitPolicyHandler extends SortingHitPolicyHandler {

  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.OUTPUT_ORDER, null);

  /**
   * Performs validation of the outputs of matching decision rules for the OUTPUT_ORDER hit policy.
   *
   * <p>The OUTPUT_ORDER hit policy does not require validation of outputs because the sorting
   * is based on the output order defined in the decision table model, not on the output values
   * themselves. All matching rules are valid by definition, and the sorting comparator handles
   * the ordering logic.</p>
   *
   * <p>This method exists to fulfill the contract of the abstract parent class
   * {@link SortingHitPolicyHandler}, but intentionally performs no operations for OUTPUT_ORDER.</p>
   *
   * @param matchingRules the list of matching decision rules that have been evaluated
   * @param decisionTableOutput the list of decision table outputs corresponding to the evaluated rules
   */
  @Override
  protected void checkOutputs(List<DmnEvaluatedDecisionRule> matchingRules,
                              List<DmnDecisionTableOutputImpl> decisionTableOutput) {
    // No specific checks for OUTPUT_ORDER hit policy.
    // The parent class ensures at least one matching rule exists.
    // Sorting is based on output value order, not on validation constraints.
  }

  /**
   * Evaluates all matching decision rules using the OUTPUT_ORDER hit policy and sorts them
   * based on the output order defined in the decision table.
   *
   * <p>The ruleComparator sorts rules according to the lexicographical order of their output values,
   * following the sequence of outputs as defined in the decision table model. Empty streams are
   * already handled by the parent class validation logic.</p>
   *
   * @param matchingRulesStream the stream of matching decision rules to be evaluated
   * @param ruleComparator the comparator that sorts rules based on output value order (lexicographical)
   * @return a list of evaluated decision rules sorted according to the output order
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
   * @return the hit policy entry defining the OUTPUT_ORDER hit policy (without aggregator)
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns a string representation of the OutputOrderHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy
   */
  @Override
  public String toString() {
    return "OutputOrderHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + "}";
  }
}
