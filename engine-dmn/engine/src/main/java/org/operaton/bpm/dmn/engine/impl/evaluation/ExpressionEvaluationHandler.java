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

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.operaton.bpm.dmn.engine.impl.CachedCompiledScriptSupport;
import org.operaton.bpm.dmn.engine.impl.CachedExpressionSupport;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnEngineLogger;
import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.el.VariableContextScriptBindings;
import org.operaton.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.commons.utils.StringUtil;

public class ExpressionEvaluationHandler {

  protected static final DmnEngineLogger LOG = DmnEngineLogger.ENGINE_LOGGER;

  protected final DmnScriptEngineResolver scriptEngineResolver;
  protected final ElProvider elProvider;
  protected final FeelEngine feelEngine;

  public ExpressionEvaluationHandler(DefaultDmnEngineConfiguration configuration) {
    this.scriptEngineResolver = configuration.getScriptEngineResolver();
    this.elProvider = configuration.getElProvider();
    this.feelEngine = configuration.getFeelEngine();
  }

    /**
   * Evaluates the given expression in the specified expression language using the provided variable context.
   *
   * @param expressionLanguage the language used in the expression
   * @param expression the DmnExpressionImpl object representing the expression
   * @param variableContext the context containing variables for evaluation
   * @return the result of evaluating the expression, or null if the expression text is null
   */
  public Object evaluateExpression(String expressionLanguage, DmnExpressionImpl expression, VariableContext variableContext) {
    String expressionText = getExpressionTextForLanguage(expression, expressionLanguage);
    if (expressionText != null) {

      if (isFeelExpressionLanguage(expressionLanguage)) {
        return evaluateFeelSimpleExpression(expressionText, variableContext);

      } else if (isElExpression(expressionLanguage)) {
        return evaluateElExpression(expressionLanguage, expressionText, variableContext, expression);

      } else {
        return evaluateScriptExpression(expressionLanguage, variableContext, expressionText, expression);
      }
    } else {
      return null;
    }
  }

    /**
   * Evaluates a script expression using the specified expression language,
   * variable context, expression text, and cached compiled script support.
   *
   * @param expressionLanguage The expression language to use for evaluation
   * @param variableContext The variable context to use for evaluation
   * @param expressionText The text of the expression to evaluate
   * @param cachedCompiledScriptSupport The support for caching compiled scripts
   * @return The result of evaluating the script expression
   */
  protected Object evaluateScriptExpression(String expressionLanguage, VariableContext variableContext, String expressionText, CachedCompiledScriptSupport cachedCompiledScriptSupport) {
    ScriptEngine scriptEngine = getScriptEngineForName(expressionLanguage);
    // wrap script engine bindings + variable context and pass enhanced
    // bindings to the script engine.
    Bindings bindings = VariableContextScriptBindings.wrap(scriptEngine.createBindings(), variableContext);
    bindings.put("variableContext", variableContext);

    try {
      if (scriptEngine instanceof Compilable) {

        CompiledScript compiledScript = cachedCompiledScriptSupport.getCachedCompiledScript();
        if (compiledScript == null) {
          synchronized (cachedCompiledScriptSupport) {
            compiledScript = cachedCompiledScriptSupport.getCachedCompiledScript();

            if(compiledScript == null) {
              Compilable compilableScriptEngine = (Compilable) scriptEngine;
              compiledScript = compilableScriptEngine.compile(expressionText);

              cachedCompiledScriptSupport.cacheCompiledScript(compiledScript);
            }
          }
        }

        return compiledScript.eval(bindings);
      }
      else {
        return scriptEngine.eval(expressionText, bindings);
      }
    }
    catch (ScriptException e) {
      throw LOG.unableToEvaluateExpression(expressionText, scriptEngine.getFactory().getLanguageName(), e);
    }
  }

    /**
   * Evaluates an EL expression using the specified expression language, expression text, variable context, and cached expression support.
   * If the cached expression is not available, a new expression is created and cached. 
   * If an exception occurs during evaluation, a log message is generated.
   * 
   * @param expressionLanguage the expression language used
   * @param expressionText the text of the expression to evaluate
   * @param variableContext the context containing variables for evaluation
   * @param cachedExpressionSupport the support for caching expressions
   * @return the result of evaluating the expression
   */
  protected Object evaluateElExpression(String expressionLanguage, String expressionText, VariableContext variableContext, CachedExpressionSupport cachedExpressionSupport) {
    try {
      ElExpression elExpression = cachedExpressionSupport.getCachedExpression();

      if (elExpression == null) {
        synchronized (cachedExpressionSupport) {
          elExpression = cachedExpressionSupport.getCachedExpression();
          if(elExpression == null) {
            elExpression = elProvider.createExpression(expressionText);
            cachedExpressionSupport.setCachedExpression(elExpression);
          }
        }
      }

      return elExpression.getValue(variableContext);
    }
    // yes, we catch all exceptions
    catch(Exception e) {
      throw LOG.unableToEvaluateExpression(expressionText, expressionLanguage, e);
    }
  }

    /**
   * Evaluates a simple FEEL expression using the provided expression text and variable context.
   * 
   * @param expressionText the text of the expression to be evaluated
   * @param variableContext the context containing variables to be used in the evaluation
   * @return the result of evaluating the expression
   */
  protected Object evaluateFeelSimpleExpression(String expressionText, VariableContext variableContext) {
    return feelEngine.evaluateSimpleExpression(expressionText, variableContext);
  }

  // helper ///////////////////////////////////////////////////////////////////

    /**
   * Returns the expression text for a given DMN expression based on the expression language.
   *
   * @param expression the DMN expression
   * @param expressionLanguage the expression language for the DMN expression
   * @return the expression text with proper formatting based on the expression language
   */
  protected String getExpressionTextForLanguage(DmnExpressionImpl expression, String expressionLanguage) {
    String expressionText = expression.getExpression();
    if (expressionText != null) {
      if (isJuelExpression(expressionLanguage) && !StringUtil.isExpression(expressionText)) {
        return "${" + expressionText + "}";
      } else {
        return expressionText;
      }
    } else {
      return null;
    }
  }

    /**
   * Checks if the given expression language is JUEL expression language.
   * 
   * @param expressionLanguage the expression language to check
   * @return true if the expression language is JUEL, false otherwise
   */
  private boolean isJuelExpression(String expressionLanguage) {
    return DefaultDmnEngineConfiguration.JUEL_EXPRESSION_LANGUAGE.equalsIgnoreCase(expressionLanguage);
  }

    /**
   * Returns the ScriptEngine for the specified expression language.
   * 
   * @param expressionLanguage the expression language for which to get the ScriptEngine
   * @return the ScriptEngine for the specified expression language
   * @throws IllegalArgumentException if the expression language is null
   * @throws IllegalStateException if no ScriptEngine is found for the specified expression language
   */
  protected ScriptEngine getScriptEngineForName(String expressionLanguage) {
    ensureNotNull("expressionLanguage", expressionLanguage);
    ScriptEngine scriptEngine = scriptEngineResolver.getScriptEngineForLanguage(expressionLanguage);
    if (scriptEngine != null) {
      return scriptEngine;

    } else {
      throw LOG.noScriptEngineFoundForLanguage(expressionLanguage);
    }
  }

    /**
   * Check if the given expression is an EL expression.
   * 
   * @param expressionLanguage the expression language to check
   * @return true if the expression is an EL expression, false otherwise
   */
  protected boolean isElExpression(String expressionLanguage) {
    return isJuelExpression(expressionLanguage);
  }

    /**
   * Checks if the given expression language is a valid FEEL expression language.
   * Valid FEEL expression languages include DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE, 
   * DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_ALTERNATIVE, DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN12, 
   * DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN13, DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN14, 
   * and DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN15.
   *
   * @param expressionLanguage the expression language to check
   * @return true if the expression language is a valid FEEL expression language, false otherwise
   */
  public boolean isFeelExpressionLanguage(String expressionLanguage) {
    ensureNotNull("expressionLanguage", expressionLanguage);
    return expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE) ||
      expressionLanguage.toLowerCase().equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_ALTERNATIVE) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN12) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN13) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN14) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN15);
  }

}
