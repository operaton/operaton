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
 * <p>This class provides common functionality for implementing DMN hit policies that require
 * sorting of matching rules, specifically the PRIORITY and OUTPUT_ORDER hit policies as defined
 * in the DMN 1.3 specification (section 8.2.8).</p>
 *
 * <p><strong>Supported Hit Policies:</strong></p>
 * <ul>
 *   <li><strong>PRIORITY:</strong> Returns the highest-priority rule based on output value order
 *       in the decision table definition. Requires at least one output value to be defined.</li>
 *   <li><strong>OUTPUT_ORDER:</strong> Returns all matching rules sorted lexicographically by
 *       their output values according to the output order defined in the decision table.</li>
 * </ul>
 *
 * <p><strong>Justification for sealed class:</strong></p>
 * <ol>
 *   <li>{@code SortingHitPolicyHandler} is only suitable for implementing the hit policies
 *       <em>PRIORITY</em> and <em>OUTPUT_ORDER</em>. Extending it for other hit policies
 *       is not meaningful and is explicitly excluded.</li>
 *   <li>Using a sealed class enables pattern matching and exhaustiveness checks if the
 *       project is migrated to Java 21 or later with preview features, or Java 24+ where
 *       this becomes a standard feature.</li>
 * </ol>
 *
 * <p>This class provides common functionality for:</p>
 * <ul>
 *   <li>Validating decision table evaluation events and decision logic types</li>
 *   <li>Checking output values against predefined output value lists</li>
 *   <li>Creating comparators for sorting rules based on output value positions</li>
 *   <li>Applying sorting logic to multiple matching rules</li>
 * </ul>
 *
 * @see PriorityHitPolicyHandler
 * @see OutputOrderHitPolicyHandler
 * @see DmnHitPolicyHandler
 */
public abstract sealed class SortingHitPolicyHandler implements DmnHitPolicyHandler permits PriorityHitPolicyHandler, OutputOrderHitPolicyHandler {

  protected static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

  /**
   * Applies specific hit policy logic to the provided decision table evaluation event.
   *
   * <p>This method orchestrates the complete hit policy evaluation process:</p>
   * <ol>
   *   <li>Validates the event and decision logic are of the expected implementation types</li>
   *   <li>Performs hit-policy-specific output validation via {@link #checkOutputs}</li>
   *   <li>Ensures at least one rule matches (required for PRIORITY and OUTPUT_ORDER)</li>
   *   <li>For single matching rules: validates output values against allowed values</li>
   *   <li>For multiple matching rules: applies sorting logic via {@link #evaluatePolicy}</li>
   * </ol>
   *
   * @param decisionTableEvaluationEvent the event representing the evaluation of a decision table,
   *                                     containing matching rules and evaluation context
   * @return the processed decision table evaluation event with updated matching rules
   *         (either validated single rule or sorted multiple rules)
   * @throws DmnHitPolicyException if:
   *         <ul>
   *           <li>No matching rules exist (both PRIORITY and OUTPUT_ORDER require at least one)</li>
   *           <li>The event is not of type {@link DmnDecisionTableEvaluationEventImpl}</li>
   *           <li>The decision logic is not of type {@link DmnDecisionTableImpl}</li>
   *           <li>Output validation fails (e.g., output value not in allowed values list)</li>
   *           <li>Hit-policy-specific validation fails (e.g., PRIORITY requires output values)</li>
   *         </ul>
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
   * Validates the specific implementation requirements for outputs according to the hit policy.
   *
   * <p>This method is called before any sorting or filtering logic is applied, allowing
   * each hit policy to enforce its specific validation rules:</p>
   * <ul>
   *   <li><strong>PRIORITY:</strong> Validates that at least one output has defined output values,
   *       as these are required to determine rule priority</li>
   *   <li><strong>OUTPUT_ORDER:</strong> No specific validation needed; all matching rules are
   *       valid by definition, and sorting is based on the output value order</li>
   * </ul>
   *
   * @param matchingRules the list of matching rules to validate
   * @param decisionTableOutput the decision table output definitions to validate against
   * @throws DmnHitPolicyException if the outputs do not meet the hit policy requirements
   */
  protected abstract void checkOutputs(List<DmnEvaluatedDecisionRule> matchingRules,
                                       List<DmnDecisionTableOutputImpl> decisionTableOutput);

  /**
   * Applies the specific hit policy evaluation logic to the matching rules.
   *
   * <p>This method determines how matching rules are processed according to the hit policy:</p>
   * <ul>
   *   <li><strong>PRIORITY:</strong> Uses the comparator to find the single rule with the
   *       highest priority (minimum comparator value) via {@code stream.min()}</li>
   *   <li><strong>OUTPUT_ORDER:</strong> Sorts all rules using the comparator via
   *       {@code stream.sorted()}, returning all rules in lexicographical order</li>
   * </ul>
   *
   * <p>The ruleComparator is created by {@link #createRuleComparator} and compares rules based
   * on the index positions of their output values within predefined output value lists.</p>
   *
   * @param matchingRulesStream stream of matching rules to evaluate (guaranteed non-empty by caller)
   * @param ruleComparator comparator to use for ordering rules based on output value positions
   * @return list of evaluated rules according to the specific hit policy
   *         (single element for PRIORITY, all elements sorted for OUTPUT_ORDER)
   */
  protected abstract List<DmnEvaluatedDecisionRule> evaluatePolicy(Stream<DmnEvaluatedDecisionRule> matchingRulesStream,
                                                                   Comparator<DmnEvaluatedDecisionRule> ruleComparator);

  /**
   * Validates that the evaluation event is of the expected implementation type.
   *
   * <p>This validation ensures that the event provides access to internal implementation
   * details required for hit policy processing, such as the ability to update matching rules.</p>
   *
   * @param event the event to validate
   * @return the validated event cast to {@link DmnDecisionTableEvaluationEventImpl}
   * @throws DmnHitPolicyException if event is not of the expected implementation type
   */
  private DmnDecisionTableEvaluationEventImpl validateEventType(DmnDecisionTableEvaluationEvent event) {
    if (!(event instanceof DmnDecisionTableEvaluationEventImpl eventImpl)) {
      throw LOG.unsupportedEventType(event.getClass().getSimpleName());
    }
    return eventImpl;
  }

  /**
   * Validates that the decision logic is of the expected implementation type.
   *
   * <p>This validation ensures that the decision logic provides access to output definitions
   * required for creating comparators and validating output values.</p>
   *
   * @param event the evaluation event containing the decision logic
   * @return the validated decision table cast to {@link DmnDecisionTableImpl}
   * @throws DmnHitPolicyException if decision logic is not of the expected implementation type
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
   * <p>When only one rule matches, this method validates that any output values conform
   * to the predefined output value lists (if such lists are defined). This ensures that
   * even single-rule results comply with the decision table's output constraints.</p>
   *
   * @param rule the single matching rule to validate
   * @param outputs the decision table output definitions containing allowed values
   * @throws DmnHitPolicyException if any output value is not in its corresponding allowed values list
   */
  private void validateSingleRuleOutputs(DmnEvaluatedDecisionRule rule, List<DmnDecisionTableOutputImpl> outputs) {
    outputs.stream()
        .filter(output -> output.getOutputValues() != null && !output.getOutputValues().isEmpty())
        .forEach(output -> validateOutputValue(rule, output, outputs.size()));
  }

  /**
   * Validates that an output value is in the list of allowed values.
   *
   * <p>This validation is performed for outputs that have predefined output value lists
   * in the decision table definition. It ensures that the actual output value of a rule
   * matches one of the allowed values.</p>
   *
   * @param rule the rule containing the output value to validate
   * @param output the output definition with the list of allowed values
   * @param totalOutputs the total number of outputs (used to determine if output name is required)
   * @throws DmnHitPolicyException if the output value is not found in the allowed values list
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
   * <p>This method coordinates the sorting process by:</p>
   * <ol>
   *   <li>Creating a comparator based on the decision table's output definitions</li>
   *   <li>Delegating to the hit-policy-specific {@link #evaluatePolicy} method</li>
   *   <li>Updating the evaluation event with the processed rules</li>
   * </ol>
   *
   * @param evaluationEvent the event to update with sorted/filtered rules
   * @param decisionTable the decision table containing output definitions for comparison
   * @param matchingRules the original list of matching rules to process
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
   * <p><strong>Sorting Behavior:</strong></p>
   * <p>The sorting behavior depends on the output configuration:</p>
   * <ul>
   *   <li><strong>With predefined output values:</strong> Rules are sorted by the index position
   *       of their actual output values within the predefined output value lists. Lower index
   *       means higher priority (appears first in the sorted result).</li>
   *   <li><strong>Without predefined output values:</strong> Returns a no-op comparator {@code (a, b) -> 0}
   *       that preserves the original order of matching rules.</li>
   *   <li><strong>Multiple outputs with predefined values:</strong> Comparators are chained using
   *       {@link Comparator#thenComparing}, following the order of outputs in the decision table.
   *       This creates a lexicographical comparison based on output positions.</li>
   * </ul>
   *
   * <p><strong>Example:</strong> If an output has the value list {@code ["high", "medium", "low"]},
   * a rule with output value "high" (index 0) will be sorted before a rule with "medium" (index 1).</p>
   *
   * <p><strong>Performance Note:</strong> While caching this comparator would improve
   * performance for repeated evaluations of the same decision table, it is currently not feasible
   * because a new {@link DmnDecisionTableImpl} instance is created for each evaluation. This
   * prevents using the decision table as a stable cache key. Future optimizations could consider
   * instance reuse or alternative caching strategies based on decision table IDs.</p>
   *
   * @param decisionTable the decision table containing the output definitions used
   *                      for creating the sorting logic
   * @return a comparator for ordering {@code DmnEvaluatedDecisionRule} objects based
   *         on their output values, or a no-op comparator if no sortable outputs exist
   * @throws DmnHitPolicyException if an output value is not found in the predefined
   *                               output value list during comparison (thrown by the comparator
   *                               during actual comparison, not during creation)
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
   * <p>This method handles different output naming scenarios:</p>
   * <ul>
   *   <li><strong>Named output:</strong> Retrieves the output by its name from the rule's output entries</li>
   *   <li><strong>Single unnamed output:</strong> Returns the only output entry (since there's no ambiguity)</li>
   *   <li><strong>Multiple unnamed outputs:</strong> Throws an exception, as outputs must be named when
   *       multiple outputs exist to avoid ambiguity</li>
   * </ul>
   *
   * @param rule the rule containing the outputs to search
   * @param outputName the name of the output to retrieve (may be null or empty for single outputs)
   * @param isSingleOutput whether this is the only output in the decision table
   * @return the evaluated output matching the criteria
   * @throws DmnHitPolicyException if the output cannot be uniquely determined (multiple unnamed outputs)
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
   *
   * <p>This class encapsulates the logic for comparing two rules based on a single output's
   * value positions. It stores the output configuration and provides a method to create
   * a {@link Comparator} that compares rules by looking up the index positions of their
   * output values in the predefined output value list.</p>
   *
   * <p>Multiple {@code SortableOutput} instances can be chained together using
   * {@link Comparator#thenComparing} to create a lexicographical comparison across
   * multiple outputs.</p>
   */
  private static class SortableOutput {
    private final String outputName;
    private final List<TypedValue> outputValues;
    private final boolean isSingleOutput;

    /**
     * Creates a sortable output wrapper for a decision table output.
     *
     * @param decisionTableOutput the output definition containing the output name and allowed values
     * @param totalOutputs the total number of outputs in the decision table (used to determine
     *                     if output names are required)
     */
    public SortableOutput(DmnDecisionTableOutputImpl decisionTableOutput, int totalOutputs) {
      this.outputName = decisionTableOutput.getOutputName();
      this.outputValues = decisionTableOutput.getOutputValues();
      this.isSingleOutput = totalOutputs == 1;
    }

    /**
     * Creates a comparator for rules based on this output's values.
     *
     * <p>The comparator compares two rules by:</p>
     * <ol>
     *   <li>Retrieving the output value for each rule</li>
     *   <li>Finding the index position of each value in the predefined output value list</li>
     *   <li>Comparing the index positions (lower index = higher priority)</li>
     * </ol>
     *
     * @return a comparator for sorting rules based on this output's value positions
     * @throws DmnHitPolicyException if an output value is not found in the predefined
     *                               output value list during comparison
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
