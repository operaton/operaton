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
package org.operaton.bpm.engine.impl.scripting;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.operaton.bpm.engine.ScriptCompilationException;
import org.operaton.bpm.engine.ScriptEvaluationException;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;

/**
 * A script which is provided as source code.
 *
 * @author Daniel Meyer
 *
 */
public class SourceExecutableScript extends CompiledExecutableScript {

  private static final ScriptLogger LOG = ProcessEngineLogger.SCRIPT_LOGGER;

  /** The source of the script. */
  protected String scriptSource;

  /** Flag to signal if the script should be compiled */
  protected boolean shouldBeCompiled = true;

  public SourceExecutableScript(String language, String source) {
    super(language);
    scriptSource = source;
  }

  @Override
  public Object evaluate(ScriptEngine engine, VariableScope variableScope, Bindings bindings) {
    if (shouldBeCompiled) {
      compileScript(engine);
    }

    if (getCompiledScript() != null) {
      return super.evaluate(engine, variableScope, bindings);
    }
    else {
      try {
        return evaluateScript(engine, bindings);
      } catch (ScriptException e) {
        Throwable cause = e.getCause();
        if (cause instanceof BpmnError bpmnError) {
          throw bpmnError;
        }
        String activityIdMessage = getActivityIdExceptionMessage(variableScope);
        throw new ScriptEvaluationException("Unable to evaluate script%s: %s".formatted(activityIdMessage, e.getMessage()), e);
      }
    }
  }

  protected void compileScript(ScriptEngine engine) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    if (processEngineConfiguration.isEnableScriptEngineCaching() && processEngineConfiguration.isEnableScriptCompilation()) {

      if (getCompiledScript() == null && shouldBeCompiled) {
        synchronized (this) {
          if (getCompiledScript() == null && shouldBeCompiled) {
            // try to compile script
            compiledScript = compile(engine, language, scriptSource);

            // either the script was successfully compiled or it can't be
            // compiled but we won't try it again
            shouldBeCompiled = false;
          }
        }
      }

    }
    else {
      // if script compilation is disabled abort
      shouldBeCompiled = false;
    }
  }

  public CompiledScript compile(ScriptEngine scriptEngine, String language, String src) {
    if (scriptEngine instanceof Compilable compilingEngine && !"ecmascript".equalsIgnoreCase(scriptEngine.getFactory().getLanguageName())) {
      try {
        CompiledScript compiledScript = compilingEngine.compile(src);

        LOG.debugCompiledScriptUsing(language);

        return compiledScript;

      } catch (ScriptException e) {
        throw new ScriptCompilationException("Unable to compile script: %s".formatted(e.getMessage()), e);

      }

    } else {
      // engine does not support compilation
      return null;
    }

  }

  protected Object evaluateScript(ScriptEngine engine, Bindings bindings) throws ScriptException {
    LOG.debugEvaluatingNonCompiledScript(scriptSource);
    return engine.eval(scriptSource, bindings);
  }

  public String getScriptSource() {
    return scriptSource;
  }

  /**
   * Sets the script source code. And invalidates any cached compilation result.
   *
   * @param scriptSource
   *          the new script source code
   */
  public void setScriptSource(String scriptSource) {
    this.compiledScript = null;
    shouldBeCompiled = true;
    this.scriptSource = scriptSource;
  }

  public boolean isShouldBeCompiled() {
    return shouldBeCompiled;
  }

}
