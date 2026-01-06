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

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * A JUnit 5 extension to define and create a {@link ScriptEngine}
 * based on {@literal @}{@link ScriptEngineRule} annotation.
 *
 * <p>
 * Author: Sebastian Menski
 * </p>
 */
public class ScriptEngineRule implements BeforeAllCallback, TestInstancePostProcessor {

  private static final SpinTestLogger LOG = SpinTestLogger.TEST_LOGGER;

  private static final Map<String, ScriptEngine> cachedEngines = new HashMap<>();

  private static final String GRAAL_JS_SCRIPT_ENGINE_NAME = "Graal.js";

  private ScriptEngine scriptEngine;

  @Override
  public void beforeAll(ExtensionContext context) {
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    System.setProperty("python.import.site", "false");

    scriptEngine = createScriptEngine(context);
    if (scriptEngine != null) {
      LOG.scriptEngineFoundForLanguage(scriptEngine.getFactory().getLanguageName());
    }
  }

  /**
   * Create script engine from {@literal @}{@link org.operaton.spin.impl.test.ScriptEngine} annotation. The created
   * script engines will be cached to speed up subsequent creations.
   *
   * @param context the {@link ExtensionContext} of the test method
   * @return the script engine or null if none suitable is found
   */
  private ScriptEngine createScriptEngine(ExtensionContext context) {
    org.operaton.spin.impl.test.ScriptEngine annotation = context.getRequiredTestClass().getAnnotation(org.operaton.spin.impl.test.ScriptEngine.class);
    if (annotation == null) {
      return null;
    } else {
      String language = annotation.value();
      if (!cachedEngines.containsKey(language)) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName(language);
        if (engine == null) {
          throw LOG.noScriptEngineFoundForLanguage(language);
        }
        if (GRAAL_JS_SCRIPT_ENGINE_NAME.equals(engine.getFactory().getEngineName())) {
          configureGraalJsScriptEngine(engine);
        }
        cachedEngines.put(language, engine);
      }
      return cachedEngines.get(language);
    }
  }

  /**
   * Get the script engine defined by the {@literal @}{@link org.operaton.spin.impl.test.ScriptEngine} annotation.
   *
   * @return the script engine or null if no script engine was found
   */
  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }

  protected void configureGraalJsScriptEngine(ScriptEngine scriptEngine) {
    // Ensure GraalVM JS provides access to the host and can look up classes.
    scriptEngine.getContext().setAttribute("polyglot.js.allowHostAccess", true, ScriptContext.ENGINE_SCOPE);
    scriptEngine.getContext().setAttribute("polyglot.js.allowHostClassLookup", true, ScriptContext.ENGINE_SCOPE);
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    // Attach the script engine to test instance if necessary.
    if (testInstance instanceof ScriptEngineRule rule) {
      rule.scriptEngine = this.scriptEngine;
    }
  }
}

