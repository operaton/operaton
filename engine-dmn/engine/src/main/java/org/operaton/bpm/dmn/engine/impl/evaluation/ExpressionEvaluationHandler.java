/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.engine.impl.evaluation;

import org.operaton.bpm.dmn.engine.impl.*;
import org.operaton.bpm.dmn.engine.impl.el.VariableContextScriptBindings;
import org.operaton.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.commons.utils.StringUtil;
import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import javax.script.*;

/**
 * Handles the evaluation of various types of expressions in the DMN engine.
 * This class supports FEEL, EL, and script-based expressions.
 */
public class ExpressionEvaluationHandler {

  // Logger for the DMN engine
  protected static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  // Resolver for script engines
  protected final DmnScriptEngineResolver scriptEngineResolver;
  // Provider for EL expressions
  protected final ElProvider elProvider;
  // Engine for FEEL expressions
  protected final FeelEngine feelEngine;

  /**
   * Constructor for initializing the ExpressionEvaluationHandler.
   *
   * @param configuration the DMN engine configuration
   */
  public ExpressionEvaluationHandler(DefaultDmnEngineConfiguration configuration) {
    this.scriptEngineResolver = configuration.getScriptEngineResolver();
    this.elProvider = configuration.getElProvider();
    this.feelEngine = configuration.getFeelEngine();
  }

  /**
   * Evaluates an expression based on its language.
   *
   * @param expressionLanguage the language of the expression (e.g., FEEL, EL, script)
   * @param expression the expression to evaluate
   * @param variableContext the context containing variables for evaluation
   * @return the result of the evaluated expression
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
   * Evaluates a script-based expression.
   *
   * @param expressionLanguage the language of the script
   * @param variableContext the context containing variables for evaluation
   * @param expressionText the script expression text
   * @param cachedCompiledScriptSupport support for caching compiled scripts
   * @return the result of the evaluated script
   */
  protected Object evaluateScriptExpression(String expressionLanguage, VariableContext variableContext,
                                            String expressionText, CachedCompiledScriptSupport cachedCompiledScriptSupport) {
    ScriptEngine scriptEngine = getScriptEngineForName(expressionLanguage);
    Bindings bindings = VariableContextScriptBindings.wrap(scriptEngine.createBindings(), variableContext);
    bindings.put("variableContext", variableContext);

    try {
      if (scriptEngine instanceof Compilable compilableScriptEngine) {
        CompiledScript compiledScript = cachedCompiledScriptSupport.getCachedCompiledScript();
        if (compiledScript == null) {
          compiledScript = cachedCompiledScriptSupport.executeWithLock(() -> {
            CompiledScript cached = cachedCompiledScriptSupport.getCachedCompiledScript();
            if (cached == null) {
              try {
                CompiledScript cs = compilableScriptEngine.compile(expressionText);
                cachedCompiledScriptSupport.cacheCompiledScript(cs);
                return cs;
              } catch (ScriptException e) {
                throw new IllegalStateException("Error compiling script", e);
              }
            } else {
              return cached;
            }
          });
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
   * Evaluates an EL (Expression Language) expression.
   *
   * @param expressionLanguage the language of the expression
   * @param expressionText the EL expression text
   * @param variableContext the context containing variables for evaluation
   * @param cachedExpressionSupport support for caching EL expressions
   * @return the result of the evaluated EL expression
   */
  protected Object evaluateElExpression(String expressionLanguage, String expressionText,
                                        VariableContext variableContext, CachedExpressionSupport cachedExpressionSupport) {
    try {
      ElExpression elExpression = cachedExpressionSupport.getCachedExpression();
      if (elExpression == null) {
        elExpression = cachedExpressionSupport.executeWithLock(() -> {
          ElExpression cached = cachedExpressionSupport.getCachedExpression();
          if (cached == null) {
            ElExpression el = elProvider.createExpression(expressionText);
            cachedExpressionSupport.setCachedExpression(el);
            return el;
          } else {
            return cached;
          }
        });
      }
      return elExpression.getValue(variableContext);
    }
    catch(Exception e) {
      throw LOG.unableToEvaluateExpression(expressionText, expressionLanguage, e);
    }
  }

  /**
   * Evaluates a simple FEEL expression.
   *
   * @param expressionText the FEEL expression text
   * @param variableContext the context containing variables for evaluation
   * @return the result of the evaluated FEEL expression
   */
  protected Object evaluateFeelSimpleExpression(String expressionText, VariableContext variableContext) {
    return feelEngine.evaluateSimpleExpression(expressionText, variableContext);
  }

  /**
   * Retrieves the expression text for a given language.
   *
   * @param expression the expression object
   * @param expressionLanguage the language of the expression
   * @return the expression text, or null if not applicable
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
   * Checks if the given language is a JUEL expression language.
   *
   * @param expressionLanguage the language to check
   * @return true if it is a JUEL expression language, false otherwise
   */
  private boolean isJuelExpression(String expressionLanguage) {
    return DefaultDmnEngineConfiguration.JUEL_EXPRESSION_LANGUAGE.equalsIgnoreCase(expressionLanguage);
  }

  /**
   * Retrieves the script engine for a given language.
   *
   * @param expressionLanguage the language of the script
   * @return the corresponding script engine
   * @throws IllegalArgumentException if no script engine is found
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
   * Checks if the given language is an EL expression language.
   *
   * @param expressionLanguage the language to check
   * @return true if it is an EL expression language, false otherwise
   */
  protected boolean isElExpression(String expressionLanguage) {
    return isJuelExpression(expressionLanguage);
  }

  /**
   * Checks if the given language is a FEEL expression language.
   *
   * @param expressionLanguage the language to check
   * @return true if it is a FEEL expression language, false otherwise
   */
  public boolean isFeelExpressionLanguage(String expressionLanguage) {
    ensureNotNull("expressionLanguage", expressionLanguage);
    return expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE) ||
      expressionLanguage.equalsIgnoreCase(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_ALTERNATIVE) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN12) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN13) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN14) ||
      expressionLanguage.equals(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN15);
  }

}
