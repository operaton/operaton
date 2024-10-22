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
package org.operaton.bpm.dmn.engine.impl.evaluation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionLogicEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationListener;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedInput;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultEntriesImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableInputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;
import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnDecisionTableEvaluationEventImpl;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnEvaluatedDecisionRuleImpl;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnEvaluatedInputImpl;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnEvaluatedOutputImpl;
import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.impl.context.CompositeVariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DecisionTableEvaluationHandler implements DmnDecisionLogicEvaluationHandler {

  protected final ExpressionEvaluationHandler expressionEvaluationHandler;
  protected final FeelEngine feelEngine;

  protected final List<DmnDecisionTableEvaluationListener> evaluationListeners;

  protected final String inputExpressionExpressionLanguage;
  protected final String inputEntryExpressionLanguage;
  protected final String outputEntryExpressionLanguage;

  protected final boolean returnBlankTableOutputAsNull;

  public DecisionTableEvaluationHandler(DefaultDmnEngineConfiguration configuration) {
    expressionEvaluationHandler = new ExpressionEvaluationHandler(configuration);
    feelEngine = configuration.getFeelEngine();

    evaluationListeners = configuration.getDecisionTableEvaluationListeners();

    inputExpressionExpressionLanguage = configuration.getDefaultInputExpressionExpressionLanguage();
    inputEntryExpressionLanguage = configuration.getDefaultInputEntryExpressionLanguage();
    outputEntryExpressionLanguage = configuration.getDefaultOutputEntryExpressionLanguage();
    returnBlankTableOutputAsNull = configuration.isReturnBlankTableOutputAsNull();
  }

    /**
   * Evaluates a DMN decision using the specified decision and variable context.
   * 
   * @param decision the DMN decision to evaluate
   * @param variableContext the variable context to use for evaluation
   * @return the evaluation event containing the result of the evaluation
   */
  @Override
  public DmnDecisionLogicEvaluationEvent evaluate(DmnDecision decision, VariableContext variableContext) {
    DmnDecisionTableEvaluationEventImpl evaluationResult = new DmnDecisionTableEvaluationEventImpl();
    evaluationResult.setDecisionTable(decision);

    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
    evaluationResult.setExecutedDecisionElements(calculateExecutedDecisionElements(decisionTable));

    evaluateDecisionTable(decisionTable, variableContext, evaluationResult);

    // apply hit policy
    decisionTable.getHitPolicyHandler().apply(evaluationResult);

    // notify listeners
    for (DmnDecisionTableEvaluationListener evaluationListener : evaluationListeners) {
      evaluationListener.notify(evaluationResult);
    }

    return evaluationResult;
  }

    /**
   * Calculates the total number of decision elements executed in the given decision table.
   * This is determined by adding the number of input and output elements, and multiplying it by the number of rules in the decision table.
   *
   * @param decisionTable the decision table for which to calculate the executed decision elements
   * @return the total number of executed decision elements in the decision table
   */
  protected long calculateExecutedDecisionElements(DmnDecisionTableImpl decisionTable) {
    return (decisionTable.getInputs().size() + decisionTable.getOutputs().size()) * decisionTable.getRules().size();
  }

    /**
   * Evaluates the given decision table using the provided variable context and updates the evaluation result.
   * 
   * @param decisionTable the decision table to evaluate
   * @param variableContext the variable context containing the input variables
   * @param evaluationResult the evaluation result to update with the outcome
   */
  protected void evaluateDecisionTable(DmnDecisionTableImpl decisionTable, VariableContext variableContext, DmnDecisionTableEvaluationEventImpl evaluationResult) {
    int inputSize = decisionTable.getInputs().size();
    List<DmnDecisionTableRuleImpl> matchingRules = new ArrayList<DmnDecisionTableRuleImpl>(decisionTable.getRules());
    for (int inputIdx = 0; inputIdx < inputSize; inputIdx++) {
      // evaluate input
      DmnDecisionTableInputImpl input = decisionTable.getInputs().get(inputIdx);
      DmnEvaluatedInput evaluatedInput = evaluateInput(input, variableContext);
      evaluationResult.getInputs().add(evaluatedInput);

      // compose local variable context out of global variable context enhanced with the value of the current input.
      VariableContext localVariableContext = getLocalVariableContext(input, evaluatedInput, variableContext);

      // filter rules applicable with this input
      matchingRules = evaluateInputForAvailableRules(inputIdx, input, matchingRules, localVariableContext);
    }

    setEvaluationOutput(decisionTable, matchingRules, variableContext, evaluationResult);
  }

    /**
   * Evaluates the input of a Decision Table using the provided input and variable context.
   *
   * @param input the decision table input to evaluate
   * @param variableContext the variable context containing the variables for evaluation
   * @return the evaluated input with the corresponding value set
   */
  protected DmnEvaluatedInput evaluateInput(DmnDecisionTableInputImpl input, VariableContext variableContext) {
    DmnEvaluatedInputImpl evaluatedInput = new DmnEvaluatedInputImpl(input);

    DmnExpressionImpl expression = input.getExpression();
    if (expression != null) {
      Object value = evaluateInputExpression(expression, variableContext);
      TypedValue typedValue = expression.getTypeDefinition().transform(value);
      evaluatedInput.setValue(typedValue);
    }
    else {
      evaluatedInput.setValue(Variables.untypedNullValue());
    }

    return evaluatedInput;
  }

    /**
   * Evaluates the input for available rules based on the condition index, input, available rules, and variable context.
   * Returns a list of matching rules.
   */
  protected List<DmnDecisionTableRuleImpl> evaluateInputForAvailableRules(int conditionIdx, DmnDecisionTableInputImpl input, List<DmnDecisionTableRuleImpl> availableRules, VariableContext variableContext) {
    List<DmnDecisionTableRuleImpl> matchingRules = new ArrayList<DmnDecisionTableRuleImpl>();
    for (DmnDecisionTableRuleImpl availableRule : availableRules) {
      DmnExpressionImpl condition = availableRule.getConditions().get(conditionIdx);
      if (isConditionApplicable(input, condition, variableContext)) {
        matchingRules.add(availableRule);
      }
    }
    return matchingRules;
  }

    /**
   * Evaluates if the condition is applicable based on the input entry, condition expression, and variable context.
   * 
   * @param input the decision table input
   * @param condition the condition expression to evaluate
   * @param variableContext the variable context for evaluation
   * @return true if the condition is applicable, false otherwise
   */
  protected boolean isConditionApplicable(DmnDecisionTableInputImpl input, DmnExpressionImpl condition, VariableContext variableContext) {
    Object result = evaluateInputEntry(input, condition, variableContext);
    return result != null && result.equals(true);
  }

    /**
   * Set the evaluation output for a decision table based on the matching rules and variable context.
   * 
   * @param decisionTable the decision table to evaluate
   * @param matchingRules the list of matching rules
   * @param variableContext the variable context used for evaluation
   * @param evaluationResult the evaluation result to update with the matching rules
   */
  protected void setEvaluationOutput(DmnDecisionTableImpl decisionTable, List<DmnDecisionTableRuleImpl> matchingRules, VariableContext variableContext, DmnDecisionTableEvaluationEventImpl evaluationResult) {
    List<DmnDecisionTableOutputImpl> decisionTableOutputs = decisionTable.getOutputs();

    List<DmnEvaluatedDecisionRule> evaluatedDecisionRules = new ArrayList<DmnEvaluatedDecisionRule>();
    for (DmnDecisionTableRuleImpl matchingRule : matchingRules) {
      DmnEvaluatedDecisionRule evaluatedRule = evaluateMatchingRule(decisionTableOutputs, matchingRule, variableContext);
      evaluatedDecisionRules.add(evaluatedRule);
    }
    evaluationResult.setMatchingRules(evaluatedDecisionRules);
  }

  protected DmnEvaluatedDecisionRule evaluateMatchingRule(List<DmnDecisionTableOutputImpl> decisionTableOutputs, DmnDecisionTableRuleImpl matchingRule, VariableContext variableContext) {
    DmnEvaluatedDecisionRuleImpl evaluatedDecisionRule = new DmnEvaluatedDecisionRuleImpl(matchingRule);
    Map<String, DmnEvaluatedOutput> outputEntries = evaluateOutputEntries(decisionTableOutputs, matchingRule, variableContext);
    evaluatedDecisionRule.setOutputEntries(outputEntries);

    /**
   * Evaluates a matching rule based on the decision table outputs and input variables.
   *
   * @param decisionTableOutputs the list of decision table outputs
   * @param matchingRule the matching rule to be evaluated
   * @param variableContext the variable context containing input variables
   * @return the evaluated decision rule
   */
  protected DmnEvaluatedDecisionRule evaluateMatchingRule(List<DmnDecisionTableOutputImpl> decisionTableOutputs, 
                                                          DmnDecisionTableRuleImpl matchingRule, 
                                                          VariableContext variableContext) {
      DmnEvaluatedDecisionRuleImpl evaluatedDecisionRule = new DmnEvaluatedDecisionRuleImpl(matchingRule);
      Map<String, DmnEvaluatedOutput> outputEntries = evaluateOutputEntries(decisionTableOutputs, matchingRule, variableContext);
      evaluatedDecisionRule.setOutputEntries(outputEntries);
  
      return evaluatedDecisionRule;
  }

    /**
   * Returns a local variable context based on the input, evaluated input, and existing variable context.
   *
   * @param input the decision table input
   * @param evaluatedInput the evaluated input
   * @param variableContext the existing variable context
   * @return the local variable context
   */
  protected VariableContext getLocalVariableContext(DmnDecisionTableInputImpl input, DmnEvaluatedInput evaluatedInput, VariableContext variableContext) {
    if (isNonEmptyExpression(input.getExpression())) {
      String inputVariableName = evaluatedInput.getInputVariable();

      return CompositeVariableContext.compose(
        Variables.createVariables()
            .putValue("inputVariableName", inputVariableName)
            .putValueTyped(inputVariableName, evaluatedInput.getValue())
            .asVariableContext(),
        variableContext
      );
    } else {
      return variableContext;
    }
  }

    /**
   * Checks if the given DmnExpressionImpl is not null and its expression is not empty after trimming.
   * 
   * @param expression the DmnExpressionImpl to check
   * @return true if the expression is not null and not empty after trimming, false otherwise
   */
  protected boolean isNonEmptyExpression(DmnExpressionImpl expression) {
    return expression != null && expression.getExpression() != null && !expression.getExpression().trim().isEmpty();
  }

    /**
   * Evaluates the input expression using the provided expression, variable context, and expression language.
   * If the expression language is null, it defaults to the inputExpressionExpressionLanguage.
   * 
   * @param expression the expression to evaluate
   * @param variableContext the context containing variables for evaluation
   * @return the result of evaluating the expression
   */
  protected Object evaluateInputExpression(DmnExpressionImpl expression, VariableContext variableContext) {
    String expressionLanguage = expression.getExpressionLanguage();
    if (expressionLanguage == null) {
      expressionLanguage = inputExpressionExpressionLanguage;
    }
    return expressionEvaluationHandler.evaluateExpression(expressionLanguage, expression, variableContext);
  }

    /**
   * Evaluates the input entry based on the condition and variable context.
   * If the condition is non-empty, evaluates it using the specified expression language.
   * If the expression language is FEEL, uses evaluateFeelSimpleUnaryTests method.
   * Otherwise, evaluates the expression using evaluateExpression method.
   * Returns true if the condition is empty.
   *
   * @param input the decision table input
   * @param condition the expression condition
   * @param variableContext the context containing variables
   * @return the result of evaluating the input entry
   */
  protected Object evaluateInputEntry(DmnDecisionTableInputImpl input, DmnExpressionImpl condition, VariableContext variableContext) {
    if (isNonEmptyExpression(condition)) {
      String expressionLanguage = condition.getExpressionLanguage();
      if (expressionLanguage == null) {
        expressionLanguage = inputEntryExpressionLanguage;
      }
      if (expressionEvaluationHandler.isFeelExpressionLanguage(expressionLanguage)) {
        return evaluateFeelSimpleUnaryTests(input, condition, variableContext);
      } else {
        return expressionEvaluationHandler.evaluateExpression(expressionLanguage, condition, variableContext);
      }
    }
    else {
      return true; // input entries without expressions are true
    }
  }

    /**
   * Evaluates the output entries of a decision table based on the matching rule and variable context.
   *
   * @param decisionTableOutputs the list of decision table outputs
   * @param matchingRule the matching rule for the decision table
   * @param variableContext the context containing variables for evaluation
   * @return a map of evaluated output entries
   */
  protected Map<String, DmnEvaluatedOutput> evaluateOutputEntries(List<DmnDecisionTableOutputImpl> decisionTableOutputs, DmnDecisionTableRuleImpl matchingRule, VariableContext variableContext) {
    Map<String, DmnEvaluatedOutput> outputEntries = new LinkedHashMap<>();

    for (int outputIdx = 0; outputIdx < decisionTableOutputs.size(); outputIdx++) {
      DmnExpressionImpl conclusion = matchingRule.getConclusions().get(outputIdx);

      boolean isNonEmptyExpression = isNonEmptyExpression(conclusion);
      if (returnBlankTableOutputAsNull || isNonEmptyExpression) {
        DmnDecisionTableOutputImpl decisionTableOutput = decisionTableOutputs.get(outputIdx);
        Object value = isNonEmptyExpression ? evaluateOutputEntry(conclusion, variableContext) : null;
        // transform to output type
        TypedValue typedValue = decisionTableOutput.getTypeDefinition().transform(value);

        // set on result
        DmnEvaluatedOutputImpl evaluatedOutput = new DmnEvaluatedOutputImpl(decisionTableOutput, typedValue);
        outputEntries.put(decisionTableOutput.getOutputName(), evaluatedOutput);
      }
    }

    return outputEntries;
  }

    /**
   * This method evaluates the output entry using the specified conclusion and variable context.
   *
   * @param conclusion the conclusion representing the output entry to be evaluated
   * @param variableContext the variable context containing the variables to be used during evaluation
   * @return the result of evaluating the output entry
   */
  protected Object evaluateOutputEntry(DmnExpressionImpl conclusion, VariableContext variableContext) {
    String expressionLanguage = conclusion.getExpressionLanguage();
    if (expressionLanguage == null) {
      expressionLanguage = outputEntryExpressionLanguage;
    }
    return expressionEvaluationHandler.evaluateExpression(expressionLanguage, conclusion, variableContext);
  }

    /**
   * Evaluates a simple unary test expression based on the given condition and input values.
   * 
   * @param input the decision table input
   * @param condition the condition expression to evaluate
   * @param variableContext the context containing variable values
   * @return the result of evaluating the simple unary test expression
   */
  protected Object evaluateFeelSimpleUnaryTests(DmnDecisionTableInputImpl input, DmnExpressionImpl condition, VariableContext variableContext) {
    String expressionText = condition.getExpression();
    if (expressionText != null) {
      return feelEngine.evaluateSimpleUnaryTests(expressionText, input.getInputVariable(), variableContext);
    }
    else {
      return null;
    }
  }

    /**
   * Generates a decision result based on the evaluation event passed as a parameter.
   *
   * @param event the DmnDecisionLogicEvaluationEvent to generate the decision result from
   * @return the generated DmnDecisionResult
   */
  @Override
  public DmnDecisionResult generateDecisionResult(DmnDecisionLogicEvaluationEvent event) {
    DmnDecisionTableEvaluationEvent evaluationResult = (DmnDecisionTableEvaluationEvent) event;

    List<DmnDecisionResultEntries> ruleResults = new ArrayList<DmnDecisionResultEntries>();

    if (evaluationResult.getCollectResultName() != null || evaluationResult.getCollectResultValue() != null) {
      DmnDecisionResultEntriesImpl ruleResult = new DmnDecisionResultEntriesImpl();
      ruleResult.putValue(evaluationResult.getCollectResultName(), evaluationResult.getCollectResultValue());
      ruleResults.add(ruleResult);
    }
    else {
      for (DmnEvaluatedDecisionRule evaluatedRule : evaluationResult.getMatchingRules()) {
        DmnDecisionResultEntriesImpl ruleResult = new DmnDecisionResultEntriesImpl();
        for (DmnEvaluatedOutput evaluatedOutput : evaluatedRule.getOutputEntries().values()) {
          ruleResult.putValue(evaluatedOutput.getOutputName(), evaluatedOutput.getValue());
        }
        ruleResults.add(ruleResult);
      }
    }

    return new DmnDecisionResultImpl(ruleResults);
  }

}
