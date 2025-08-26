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
package org.operaton.spin.impl.test;

import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.spin.impl.util.SpinIoUtil;
import org.operaton.spin.scripting.SpinScriptEnv;

/**
 * A JUnit5 {@link org.junit.jupiter.api.extension.Extension} to load and execute a script.
 * Executes a {@link org.operaton.spin.impl.test.ScriptEngine}.
 *
 * <p>Provides support for loading scripts and managing script variables.
 */
public class ScriptExtension implements BeforeEachCallback, AfterEachCallback {

  private static final SpinTestLogger LOG = SpinTestLogger.TEST_LOGGER;

  private String script;
  private String scriptPath;
  private ScriptEngine scriptEngine;

  /**
   * The variables of the script accessed during script execution.
   */
  protected final Map<String, Object> variables = new HashMap<>();

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    loadScript(context);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    for (Object variable : variables.values()) {
      if (variable instanceof Reader reader) {
        SpinIoUtil.closeSilently(reader);
      }
    }
  }

  /**
   * Load a script and the script variables defined.
   *
   * @param context the test context
   */
  private void loadScript(ExtensionContext context) throws Exception {
    scriptEngine = getScriptEngine(context);
    if (scriptEngine == null) {
      return;
    }

    script = getScript(context);
    collectScriptVariables(context);
    if ("ruby".equalsIgnoreCase(scriptEngine.getFactory().getLanguageName())) {
      variables.put("org.jruby.embed.clear.variables", true);
    }
    boolean execute = isExecuteScript(context);
    if (execute) {
      executeScript();
    }
  }

  private ScriptEngine getScriptEngine(ExtensionContext context) {
    try {
      Object testInstance = context.getRequiredTestInstance();
      var scriptEngineRule = (ScriptEngineRule) testInstance.getClass().getField("scriptEngine").get(testInstance);
      return scriptEngineRule.getScriptEngine();
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }

  private String getScript(ExtensionContext context) {
    Script scriptAnnotation = context.getRequiredTestMethod().getAnnotation(Script.class);
    if (scriptAnnotation == null) {
      return null;
    }
    String scriptBasename = getScriptBasename(scriptAnnotation, context);
    scriptPath = getScriptPath(scriptBasename, context);
    File file = SpinIoUtil.getClasspathFile(scriptPath, context.getRequiredTestClass().getClassLoader());
    return SpinIoUtil.fileAsString(file);
  }

  private void collectScriptVariables(ExtensionContext context) {
    ScriptVariable scriptVariable = context.getRequiredTestMethod().getAnnotation(ScriptVariable.class);
    collectScriptVariable(scriptVariable);

    Script scriptAnnotation = context.getRequiredTestMethod().getAnnotation(Script.class);
    if (scriptAnnotation != null) {
      for (ScriptVariable variable : scriptAnnotation.variables()) {
        collectScriptVariable(variable);
      }
    }
  }

  private void collectScriptVariable(ScriptVariable scriptVariable) {
    if (scriptVariable == null) {
      return;
    }
    String name = scriptVariable.name();
    String value = scriptVariable.value();
    String filename = scriptVariable.file();
    boolean isNull = scriptVariable.isNull();
    if (isNull) {
      variables.put(name, null);
      LOG.scriptVariableFound(name, "isNull", null);
    } else if (!filename.isEmpty()) {
      Reader fileAsReader = SpinIoUtil.classpathResourceAsReader(filename);
      variables.put(name, fileAsReader);
      LOG.scriptVariableFound(name, "reader", filename);
    } else {
      variables.put(name, value);
      LOG.scriptVariableFound(name, "string", value);
    }
  }

  private boolean isExecuteScript(ExtensionContext context) {
    Script annotation = context.getRequiredTestMethod().getAnnotation(Script.class);
    return annotation != null && annotation.execute();
  }

  private void executeScript() throws Exception {
    if (scriptEngine != null) {
      try {
        String environment = SpinScriptEnv.get(scriptEngine.getFactory().getLanguageName());

        Bindings bindings = new SimpleBindings(variables);
        LOG.executeScriptWithScriptEngine(scriptPath, scriptEngine.getFactory().getEngineName());
        scriptEngine.eval(environment, bindings);
        scriptEngine.eval(script, bindings);
      } catch (ScriptException e) {
        if ("graal.js".equalsIgnoreCase(scriptEngine.getFactory().getEngineName())) {
          if (e.getCause() instanceof Exception ex) {
            throw ex;
          }
          throw new RuntimeException(e.getCause());
        }
        throw LOG.scriptExecutionError(scriptPath, e);
      }
    }
  }

  public ScriptExtension execute(Map<String, Object> scriptVariables) throws Exception {
    variables.putAll(scriptVariables);
    executeScript();
    return this;
  }

  public ScriptExtension execute() throws Exception {
    executeScript();
    return this;
  }

  private String getScriptBasename(Script scriptAnnotation, ExtensionContext context) {
    String scriptBasename = scriptAnnotation.value();
    if (scriptBasename.isEmpty()) {
      scriptBasename = scriptAnnotation.name();
    }
    if (scriptBasename.isEmpty()) {
      scriptBasename = context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName();
    }
    return scriptBasename;
  }

  private String getPackageDirectoryPath(ExtensionContext context) {
    String packageName = context.getRequiredTestClass().getPackageName();
    return replaceDotsWithPathSeparators(packageName) + File.separator;
  }

  private String replaceDotsWithPathSeparators(String path) {
    return path.replace(".", File.separator);
  }

  private String getScriptPath(String scriptBasename, ExtensionContext context) {
    return getPackageDirectoryPath(context) + scriptBasename + getScriptExtension();
  }

  private String getScriptExtension() {
    String languageName = scriptEngine.getFactory().getLanguageName();
    String extension = SpinScriptEnv.getExtension(languageName);
    if (extension == null) {
      LOG.noScriptExtensionFoundForScriptLanguage(languageName);
      return "";
    } else {
      return "." + extension;
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T getVariable(String name) {
    try {
      if ("ECMAScript".equals(scriptEngine.getFactory().getLanguageName())) {
        return (T) getVariableJs(name);
      } else {
        return (T) variables.get(name);
      }
    } catch (ClassCastException e) {
      throw LOG.cannotCastVariableError(name, e);
    }
  }
  @SuppressWarnings("unchecked")
  private Object getVariableJs(String name) {
    Object variable = variables.get(name);
    if (variable == null) {
      if (variables.containsKey("nashorn.global")) {
        variable = ((Map<String, Object>) variables.get("nashorn.global")).get(name);
      } else if (variables.containsKey("polyglot.context")) {
        Value member = ((org.graalvm.polyglot.Context) variables.get("polyglot.context")).getBindings("js").getMember(name);
        variable = member == null ? null : member.as(Object.class);
      }
    }
    return variable;
  }

  public ScriptExtension setVariable(String name, Object value) {
    variables.put(name, value);
    return this;
  }
}
