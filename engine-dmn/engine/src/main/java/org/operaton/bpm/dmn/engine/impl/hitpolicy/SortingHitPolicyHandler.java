package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.operaton.bpm.dmn.engine.DmnDecisionLogic;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnDecisionTableEvaluationEventImpl;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Abstract base class for hit policy handlers that use sorting as part of their evaluation strategy.
 *
 * <strong>Justification for sealed:</strong>
 * 1. `SortingHitPolicyHandler` is only suitable for implementing the hit policies <em>PRIORITY</em> and <em>OUTPUT ORDER</em>. Extending it for other hit policies is not meaningful and is explicitly excluded.
 * 2. Using a sealed class enables pattern matching and exhaustiveness checks if the project is migrated to Java 24 or later.
 *
 * Provides common functionality for validating and processing decision table evaluation events.
 */
public abstract sealed class SortingHitPolicyHandler implements DmnHitPolicyHandler permits PriorityHitPolicyHandler, OutputOrderHitPolicyHandler {

  protected static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

  /**
   * Applies specific hit policy logic to the provided decision table evaluation event.
   * Ensures the event and decision logic are of valid types, checks the outputs
   * against matching rules, and applies sorting logic if necessary.
   *
   * @param decisionTableEvaluationEvent the event representing the evaluation of a decision table
   * @return the processed decision table evaluation event with updated matching rules or outputs
   * @throws DmnHitPolicyException if there are no matching rules, unsupported event/decision logic,
   *         or validation errors during evaluation
   */
  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    DmnDecisionTableEvaluationEventImpl evaluationEvent = validateEventType(decisionTableEvaluationEvent);
    DmnDecisionTableImpl decisionTable = validateDecisionLogic(evaluationEvent);

    List<DmnEvaluatedDecisionRule> matchingRules = evaluationEvent.getMatchingRules();
    List<DmnDecisionTableOutputImpl> outputs = decisionTable.getOutputs();

    checkOutputs(matchingRules, outputs);

    if (matchingRules.isEmpty()) {
      throw LOG.hitPolicyRequiresAtLeastOneMatchingRule(getHitPolicyEntry().getHitPolicy());
    } else if (matchingRules.size() == 1) {
      validateSingleRuleOutputs(matchingRules.get(0), outputs);
    } else {
      applySortingLogic(evaluationEvent, decisionTable, matchingRules);
    }

    return evaluationEvent;
  }

  /**
   * Validates the specific implementation requirements for outputs.
   *
   * @param matchingRules       the list of matching rules
   * @param decisionTableOutput the decision table output definitions
   */
  protected abstract void checkOutputs(List<DmnEvaluatedDecisionRule> matchingRules,
                                       List<DmnDecisionTableOutputImpl> decisionTableOutput);

  /**
   * Applies the specific hit policy evaluation logic to the matching rules.
   *
   * @param matchingRulesStream stream of matching rules to evaluate
   * @param ruleComparator      comparator to use for ordering rules
   * @return list of evaluated rules according to the specific hit policy
   */
  protected abstract List<DmnEvaluatedDecisionRule> evaluatePolicy(Stream<DmnEvaluatedDecisionRule> matchingRulesStream,
                                                                   Comparator<DmnEvaluatedDecisionRule> ruleComparator);

  /**
   * Validates that the event is of the expected type.
   *
   * @param event the event to validate
   * @return the validated event cast to the expected type
   * @throws DmnHitPolicyException if event is not of the expected type
   */
  private DmnDecisionTableEvaluationEventImpl validateEventType(DmnDecisionTableEvaluationEvent event) {
    if (!(event instanceof DmnDecisionTableEvaluationEventImpl eventImpl)) {
      throw LOG.unsupportedEventType(event.getClass().getSimpleName());
    }
    return eventImpl;
  }

  /**
   * Validates that the decision logic is of the expected type.
   *
   * @param event the evaluation event containing the decision logic
   * @return the validated decision table cast to the expected type
   * @throws DmnHitPolicyException if decision logic is not of the expected type
   */
  private DmnDecisionTableImpl validateDecisionLogic(DmnDecisionTableEvaluationEventImpl event) {
    DmnDecisionLogic decisionLogic = event.getDecision().getDecisionLogic();
    if (!(decisionLogic instanceof DmnDecisionTableImpl decisionTable)) {
      throw LOG.unsupportedDecisionLogic(decisionLogic.getClass().getSimpleName());
    }
    return decisionTable;
  }

  /**
   * Validates outputs for a single matching rule.
   *
   * @param rule    the single matching rule
   * @param outputs the decision table outputs
   * @throws DmnHitPolicyException if validation fails
   */
  private void validateSingleRuleOutputs(DmnEvaluatedDecisionRule rule, List<DmnDecisionTableOutputImpl> outputs) {
    outputs.stream()
        .filter(output -> output.getOutputValues() != null && !output.getOutputValues().isEmpty())
        .forEach(output -> validateOutputValue(rule, output, outputs.size()));
  }

  /**
   * Validates that an output value is in the list of allowed values.
   *
   * @param rule         the rule containing the output value
   * @param output       the output definition with allowed values
   * @param totalOutputs the total number of outputs
   * @throws DmnHitPolicyException if validation fails
   */
  private void validateOutputValue(DmnEvaluatedDecisionRule rule, DmnDecisionTableOutputImpl output, int totalOutputs) {
    String outputName = output.getOutputName();
    DmnEvaluatedOutput evaluatedOutput = getEvaluatedOutput(rule, outputName, totalOutputs == 1);

    if (evaluatedOutput != null && !output.getOutputValues().contains(evaluatedOutput.getValue())) {
      String errorOutputName = outputName != null ? outputName : "unnamed output";
      throw LOG.outputValueNotFoundInOutputValues(errorOutputName, evaluatedOutput.getValue(), output.getOutputValues());
    }
  }

  /**
   * Applies the sorting logic to multiple matching rules.
   *
   * @param evaluationEvent the event to update with sorted rules
   * @param decisionTable   the decision table being evaluated
   * @param matchingRules   the original matching rules
   */
  private void applySortingLogic(DmnDecisionTableEvaluationEventImpl evaluationEvent,
                                 DmnDecisionTableImpl decisionTable,
                                 List<DmnEvaluatedDecisionRule> matchingRules) {
    Stream<DmnEvaluatedDecisionRule> matchingRulesStream = matchingRules.stream();
    Comparator<DmnEvaluatedDecisionRule> ruleComparator = createRuleComparator(decisionTable);
    List<DmnEvaluatedDecisionRule> sortedRules = evaluatePolicy(matchingRulesStream, ruleComparator);
    evaluationEvent.setMatchingRules(sortedRules);
  }

  /**
   * Creates a comparator for ordering evaluated decision rules based on the output values
   * defined in the decision table. The comparator sorts rules according to the position
   * of their output values within the predefined output value lists.
   *
   * <p>The sorting behavior depends on the output configuration:
   * <ul>
   *   <li>If outputs have predefined values (output value lists), rules are sorted by
   *       the index position of their actual output values within these lists</li>
   *   <li>If no outputs have predefined values, returns a no-op comparator that
   *       preserves the original order</li>
   *   <li>For multiple outputs with predefined values, comparators are chained using
   *       the order of outputs in the decision table</li>
   * </ul>
   *
   * <p><strong>Performance Note:</strong> While caching this comparator would improve
   * performance for repeated evaluations, it is currently not feasible because a new
   * {@link DmnDecisionTableImpl} instance is created for each evaluation, preventing
   * the use of the decision table as a stable cache key.
   *
   * @param decisionTable the decision table containing the output definitions used
   *                      for creating the sorting logic
   * @return a comparator for ordering {@code DmnEvaluatedDecisionRule} objects based
   *         on their output values, or a no-op comparator if no sortable outputs exist
   * @throws DmnHitPolicyException if an output value is not found in the predefined
   *                               output value list during comparison
   */
  private static Comparator<DmnEvaluatedDecisionRule> createRuleComparator(DmnDecisionTableImpl decisionTable) {
    List<DmnDecisionTableOutputImpl> outputs = decisionTable.getOutputs();
    int totalOutputs = outputs.size();

    return outputs.stream()
        .filter(output -> output.getOutputValues() != null && !output.getOutputValues().isEmpty())
        .map(output -> new SortableOutput(output, totalOutputs))
        .map(SortableOutput::toComparator)
        .reduce(Comparator::thenComparing)
        .orElse((a, b) -> 0);
  }

  /**
   * Gets the evaluated output for a rule based on the output name.
   *
   * @param rule           the rule containing the outputs
   * @param outputName     the name of the output to retrieve
   * @param isSingleOutput whether this is the only output
   * @return the evaluated output
   * @throws DmnHitPolicyException if the output cannot be determined
   */
  private static DmnEvaluatedOutput getEvaluatedOutput(DmnEvaluatedDecisionRule rule,
                                                       String outputName,
                                                       boolean isSingleOutput) {
    if (outputName != null && !outputName.isEmpty()) {
      return rule.getOutputEntries().get(outputName);
    } else if (isSingleOutput) {
      return rule.getOutputEntries().values().iterator().next();
    } else {
      throw LOG.outputMustHaveNameWhenMultipleOutputs();
    }
  }

  /**
   * Helper class to create comparators for sorting rules based on output values.
   */
  private static class SortableOutput {
    private final String outputName;
    private final List<TypedValue> outputValues;
    private final boolean isSingleOutput;

    public SortableOutput(DmnDecisionTableOutputImpl decisionTableOutput, int totalOutputs) {
      this.outputName = decisionTableOutput.getOutputName();
      this.outputValues = decisionTableOutput.getOutputValues();
      this.isSingleOutput = totalOutputs == 1;
    }

    /**
     * Creates a comparator for rules based on this output's values.
     *
     * @return a comparator for sorting rules
     */
    public Comparator<DmnEvaluatedDecisionRule> toComparator() {
      return (rule1, rule2) -> {
        DmnEvaluatedOutput output1 = getEvaluatedOutput(rule1, outputName, isSingleOutput);
        DmnEvaluatedOutput output2 = getEvaluatedOutput(rule2, outputName, isSingleOutput);

        int index1 = outputValues.indexOf(output1.getValue());
        int index2 = outputValues.indexOf(output2.getValue());

        if (index1 < 0 || index2 < 0) {
          String errorOutputName = outputName != null ? outputName : "unnamed output";
          Object invalidValue = index1 < 0 ? output1.getValue() : output2.getValue();
          throw LOG.outputValueNotFoundInOutputValues(errorOutputName, invalidValue, outputValues);
        }

        return Integer.compare(index1, index2);
      };
    }
  }
}
