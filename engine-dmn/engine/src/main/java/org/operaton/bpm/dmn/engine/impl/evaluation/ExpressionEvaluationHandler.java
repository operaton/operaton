/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.script.*;

import org.operaton.bpm.dmn.engine.impl.*;
import org.operaton.bpm.dmn.engine.impl.el.VariableContextScriptBindings;
import org.operaton.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.commons.utils.StringUtil;

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

public class ExpressionEvaluationHandler {

  protected static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected final DmnScriptEngineResolver scriptEngineResolver;
  protected final ElProvider elProvider;
  protected final FeelEngine feelEngine;

  public ExpressionEvaluationHandler(DefaultDmnEngineConfiguration configuration) {
    this.scriptEngineResolver = configuration.getScriptEngineResolver();
    this.elProvider = configuration.getElProvider();
    this.feelEngine = configuration.getFeelEngine();
  }

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

  protected Object evaluateScriptExpression(String expressionLanguage, VariableContext variableContext, String expressionText, CachedCompiledScriptSupport cachedCompiledScriptSupport) {
    ScriptEngine scriptEngine = getScriptEngineForName(expressionLanguage);
    // wrap script engine bindings + variable context and pass enhanced
    // bindings to the script engine.
    Bindings bindings = VariableContextScriptBindings.wrap(scriptEngine.createBindings(), variableContext);
    bindings.put("variableContext", variableContext);

    try {
      if (scriptEngine instanceof Compilable compilableScriptEngine) {

        CompiledScript compiledScript = cachedCompiledScriptSupport.getCachedCompiledScript();
        if (compiledScript == null) {
          synchronized (cachedCompiledScriptSupport.getCacheLock()) {
            compiledScript = cachedCompiledScriptSupport.getCachedCompiledScript();

            if (compiledScript == null) {
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

  protected Object evaluateElExpression(String expressionLanguage, String expressionText, VariableContext variableContext, CachedExpressionSupport cachedExpressionSupport) {
    try {
      ElExpression elExpression = cachedExpressionSupport.getCachedExpression();

      if (elExpression == null) {
        synchronized (cachedExpressionSupport.getCacheLock()) {
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

  protected Object evaluateFeelSimpleExpression(String expressionText, VariableContext variableContext) {
    return feelEngine.evaluateSimpleExpression(expressionText, variableContext);
  }

  // helper ///////////////////////////////////////////////////////////////////

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

  private boolean isJuelExpression(String expressionLanguage) {
    return DefaultDmnEngineConfiguration.JUEL_EXPRESSION_LANGUAGE.equalsIgnoreCase(expressionLanguage);
  }

  protected ScriptEngine getScriptEngineForName(String expressionLanguage) {
    ensureNotNull("expressionLanguage", expressionLanguage);
    ScriptEngine scriptEngine = scriptEngineResolver.getScriptEngineForLanguage(expressionLanguage);
    if (scriptEngine != null) {
      return scriptEngine;

    } else {
      throw LOG.noScriptEngineFoundForLanguage(expressionLanguage);
    }
  }

  protected boolean isElExpression(String expressionLanguage) {
    return isJuelExpression(expressionLanguage);
  }

  public boolean isFeelExpressionLanguage(String expressionLanguage) {
    ensureNotNull("expressionLanguage", expressionLanguage);
    return DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE.equals(expressionLanguage) ||
      DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_ALTERNATIVE.equalsIgnoreCase(expressionLanguage) ||
      DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN12.equals(expressionLanguage) ||
      DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN13.equals(expressionLanguage) ||
      DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN14.equals(expressionLanguage) ||
      DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN15.equals(expressionLanguage);
  }

}
