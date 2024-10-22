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
package org.operaton.bpm.dmn.feel.impl.juel;

import org.operaton.bpm.impl.juel.jakarta.el.ELContext;
import org.operaton.bpm.impl.juel.jakarta.el.ELException;
import org.operaton.bpm.impl.juel.jakarta.el.ExpressionFactory;
import org.operaton.bpm.impl.juel.jakarta.el.ValueExpression;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.juel.el.ElContextFactory;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelTransform;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.commons.utils.cache.Cache;

public class FeelEngineImpl implements FeelEngine {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  protected FeelToJuelTransform transform;
  protected ExpressionFactory expressionFactory;
  protected ElContextFactory elContextFactory;
  protected Cache<TransformExpressionCacheKey, String> transformExpressionCache;

  public FeelEngineImpl(FeelToJuelTransform transform, ExpressionFactory expressionFactory, ElContextFactory elContextFactory,
      Cache<TransformExpressionCacheKey, String> transformExpressionCache) {
    this.transform = transform;
    this.expressionFactory = expressionFactory;
    this.elContextFactory = elContextFactory;
    this.transformExpressionCache = transformExpressionCache;
  }

    /**
   * Throws an exception indicating that simple expressions are not supported.
   *
   * @param simpleExpression the simple expression to be evaluated
   * @param variableContext the variable context to use for evaluation
   * @return the result of evaluating the simple expression
   * @throws UnsupportedOperationException indicating that simple expressions are not supported
   */
  public <T> T evaluateSimpleExpression(String simpleExpression, VariableContext variableContext) {
    throw LOG.simpleExpressionNotSupported();
  }

    /**
   * Evaluates a simple unary test expression with the given input and variable context.
   *
   * @param simpleUnaryTests the simple unary test expression to evaluate
   * @param inputName the name of the input
   * @param variableContext the variable context containing variables for evaluation
   * @return the result of evaluating the simple unary test expression
   */
  public boolean evaluateSimpleUnaryTests(String simpleUnaryTests, String inputName, VariableContext variableContext) {
    try {
      ELContext elContext = createContext(variableContext);
      ValueExpression valueExpression = transformSimpleUnaryTests(simpleUnaryTests, inputName, elContext);
       return (Boolean) valueExpression.getValue(elContext);
    }
    catch (FeelMissingFunctionException e) {
      throw LOG.unknownFunction(simpleUnaryTests, e);
    }
    catch (FeelMissingVariableException e) {
      if (inputName.equals(e.getVariable())) {
        throw LOG.unableToEvaluateExpressionAsNotInputIsSet(simpleUnaryTests, e);
      }
      else {
        throw LOG.unknownVariable(simpleUnaryTests, e);
      }
    }
    catch (FeelConvertException e) {
      throw LOG.unableToConvertValue(simpleUnaryTests, e);
    }
    catch (ELException e) {
      if (e.getCause() instanceof FeelMethodInvocationException) {
        throw LOG.unableToInvokeMethod(simpleUnaryTests, (FeelMethodInvocationException) e.getCause());
      }
      else {
        throw LOG.unableToEvaluateExpression(simpleUnaryTests, e);
      }
    }
  }

    /**
   * Creates an EL context using the specified variable context.
   *
   * @param variableContext the variable context to create the EL context with
   * @return the EL context created with the specified variable context
   */
  protected ELContext createContext(VariableContext variableContext) {
    return elContextFactory.createContext(expressionFactory, variableContext);
  }

    /**
   * Transforms simple unary tests to a JUEL expression and creates a ValueExpression using the expression factory.
   * 
   * @param simpleUnaryTests the simple unary tests to transform
   * @param inputName the name of the input
   * @param elContext the ELContext to use for creating the ValueExpression
   * @return the created ValueExpression
   */
  protected ValueExpression transformSimpleUnaryTests(String simpleUnaryTests, String inputName, ELContext elContext) {

    String juelExpression = transformToJuelExpression(simpleUnaryTests, inputName);

    try {
      return expressionFactory.createValueExpression(elContext, juelExpression, Object.class);
    }
    catch (ELException e) {
      throw LOG.invalidExpression(simpleUnaryTests, e);
    }
  }

    /**
   * Transforms a simple unary test expression to a JUEL expression, caching the result for future use.
   * 
   * @param simpleUnaryTests the simple unary test expression to transform
   * @param inputName the name of the input being transformed
   * @return the JUEL expression transformed from the given simple unary test expression
   */
  protected String transformToJuelExpression(String simpleUnaryTests, String inputName) {

    TransformExpressionCacheKey cacheKey = new TransformExpressionCacheKey(simpleUnaryTests, inputName);
    String juelExpression = transformExpressionCache.get(cacheKey);

    if (juelExpression == null) {
      juelExpression = transform.transformSimpleUnaryTests(simpleUnaryTests, inputName);
      transformExpressionCache.put(cacheKey, juelExpression);
    }
    return juelExpression;
  }

}
