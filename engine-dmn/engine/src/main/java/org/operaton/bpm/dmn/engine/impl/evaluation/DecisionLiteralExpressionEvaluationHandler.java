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

import java.util.Collections;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionLiteralExpressionEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionLogicEvaluationEvent;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionLiteralExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultEntriesImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultImpl;
import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.DmnVariableImpl;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnDecisionLiteralExpressionEvaluationEventImpl;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DecisionLiteralExpressionEvaluationHandler implements DmnDecisionLogicEvaluationHandler {

  protected final ExpressionEvaluationHandler expressionEvaluationHandler;

  protected final String literalExpressionLanguage;

  public DecisionLiteralExpressionEvaluationHandler(DefaultDmnEngineConfiguration configuration) {
    expressionEvaluationHandler = new ExpressionEvaluationHandler(configuration);

    literalExpressionLanguage = configuration.getDefaultLiteralExpressionLanguage();
  }

    /**
   * Evaluates a decision by evaluating the literal expression associated with the decision.
   * 
   * @param decision the decision to evaluate
   * @param variableContext the context containing variables for evaluation
   * @return the evaluation event representing the result of the evaluation
   */
  @Override
  public DmnDecisionLogicEvaluationEvent evaluate(DmnDecision decision, VariableContext variableContext) {
    DmnDecisionLiteralExpressionEvaluationEventImpl evaluationResult = new DmnDecisionLiteralExpressionEvaluationEventImpl();
    evaluationResult.setDecision(decision);
    evaluationResult.setExecutedDecisionElements(1);

    DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression = (DmnDecisionLiteralExpressionImpl) decision.getDecisionLogic();
    DmnVariableImpl variable = dmnDecisionLiteralExpression.getVariable();
    DmnExpressionImpl expression = dmnDecisionLiteralExpression.getExpression();

    Object evaluateExpression = evaluateLiteralExpression(expression, variableContext);
    TypedValue typedValue = variable.getTypeDefinition().transform(evaluateExpression);

    evaluationResult.setOutputValue(typedValue);
    evaluationResult.setOutputName(variable.getName());

    return evaluationResult;
  }

    /**
   * Evaluates a literal expression using the specified expression language and variable context.
   * If no expression language is specified, the default literal expression language is used.
   *
   * @param expression the literal expression to evaluate
   * @param variableContext the context containing variables to be used in the evaluation
   * @return the result of evaluating the literal expression
   */
  protected Object evaluateLiteralExpression(DmnExpressionImpl expression, VariableContext variableContext) {
    String expressionLanguage = expression.getExpressionLanguage();
    if (expressionLanguage == null) {
      expressionLanguage = literalExpressionLanguage;
    }
    return expressionEvaluationHandler.evaluateExpression(expressionLanguage, expression, variableContext);
  }

    /**
   * Generates a decision result based on the evaluation event passed as parameter.
   *
   * @param event the decision logic evaluation event
   * @return the generated decision result
   */
  @Override
  public DmnDecisionResult generateDecisionResult(DmnDecisionLogicEvaluationEvent event) {
    DmnDecisionLiteralExpressionEvaluationEvent evaluationEvent = (DmnDecisionLiteralExpressionEvaluationEvent) event;

    DmnDecisionResultEntriesImpl result = new DmnDecisionResultEntriesImpl();
    result.putValue(evaluationEvent.getOutputName(), evaluationEvent.getOutputValue());

    return new DmnDecisionResultImpl(Collections.<DmnDecisionResultEntries> singletonList(result));
  }

}
