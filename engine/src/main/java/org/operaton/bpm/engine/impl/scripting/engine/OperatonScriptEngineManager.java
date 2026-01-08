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
package org.operaton.bpm.engine.impl.scripting.engine;

import static org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines.GRAAL_JS_SCRIPT_ENGINE_NAME;

import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

import static org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines.GROOVY_SCRIPTING_LANGUAGE;
import static org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines.JYTHON_SCRIPT_ENGINE_NAME;

/**
 * Custom Script Engine Manager that can execute custom logic:
 * <p>
 * a) after the discovery of the engines on the classpath; the respective engine factories are created
 * b) before the engines are created.
 * </p>
 *
 * <p>
 * If custom logic is needed for a specific engine after the classpath detection, before the engine creation,
 * it can be added to the classes map.
 * </p>
 */
public class OperatonScriptEngineManager extends ScriptEngineManager {

  private static final String JS_SYNTAX_EXTENSIONS_OPTION = "js.syntax-extensions";
  private static final String JS_SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_OPTION = "js.script-engine-global-scope-import";
  private static final String JS_LOAD_OPTION = "js.load";
  private static final String JS_PRINT_OPTION = "js.print";
  private static final String JS_GLOBAL_ARGUMENTS_OPTION = "js.global-arguments";

  public static final String OPERATON_NAMESPACE = "org.operaton";
  public static final String CAMUNDA_NAMESPACE = "org.camunda";

  protected final Map<String, Runnable> engineNameToInitLogicMappings = Map.of(
    GRAAL_JS_SCRIPT_ENGINE_NAME, this::disableGraalVMInterpreterOnlyModeWarnings,
    JYTHON_SCRIPT_ENGINE_NAME, this::disablePythonImportSiteWarnings
  );

  public OperatonScriptEngineManager() {
    super(); // creates engine factories after classpath discovery
    applyConfigOnEnginesAfterClasspathDiscovery();
  }

  protected void applyConfigOnEnginesAfterClasspathDiscovery() {
    var engineNames = getEngineNamesFoundInClasspath();

    for (var engineName : engineNames) {
      executeConfigurationBeforeEngineCreation(engineName);
    }
  }

  protected List<String> getEngineNamesFoundInClasspath() {
    var engineFactories = getEngineFactories();

    return engineFactories.stream()
        .map(ScriptEngineFactory::getEngineName)
        .toList();

  }

  @Override
  public ScriptEngine getEngineByName(String shortName) {
    ProcessEngineConfigurationImpl config = org.operaton.bpm.engine.impl.context.Context
        .getProcessEngineConfiguration();

    if (config != null && ProcessEngineConfigurationImpl.DB_CAMUNDA_COMPATIBILITY_TRANSLATION_MODE
        .equals(config.getCamundaCompatibilityMode()) && GRAAL_JS_SCRIPT_ENGINE_NAME.equalsIgnoreCase(shortName)) {

      OperatonClassLoader operatonClassLoader = new OperatonClassLoader(Thread.currentThread().getContextClassLoader());

      Context.Builder builder = Context.newBuilder("js").allowExperimentalOptions(true)
          .hostClassLoader(operatonClassLoader)
          .option(JS_SYNTAX_EXTENSIONS_OPTION, "true")
          .option(JS_LOAD_OPTION, "true")
          .option(JS_PRINT_OPTION, "true")
          .option(JS_GLOBAL_ARGUMENTS_OPTION, "true")
          .option(JS_SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_OPTION, "true");

      if (config.isConfigureScriptEngineHostAccess()) {
        // make sure Graal JS can provide access to the host and can lookup classes
        //          scriptEngine.getContext().setAttribute("polyglot.js.allowHostAccess", true, ScriptContext.ENGINE_SCOPE);
        //          scriptEngine.getContext().setAttribute("polyglot.js.allowHostClassLookup", true, ScriptContext.ENGINE_SCOPE);
        builder.allowHostAccess(HostAccess.ALL).allowHostClassLookup(className -> true);
      }
      if (config.isEnableScriptEngineLoadExternalResources()) {
        // make sure Graal JS can load external scripts
        //          scriptEngine.getContext().setAttribute("polyglot.js.allowIO", true, ScriptContext.ENGINE_SCOPE);
        //        builder.allowIO(true);
      }
      if (config.isEnableScriptEngineNashornCompatibility()) {
        // enable Nashorn compatibility mode
        //          scriptEngine.getContext().setAttribute("polyglot.js.nashorn-compat", true, ScriptContext.ENGINE_SCOPE);
        builder.allowAllAccess(true);
        //          builder.allowHostAccess(NASHORN_HOST_ACCESS);
      }

      return GraalJSScriptEngine.create(null, builder);
    } else if (ProcessEngineConfigurationImpl.DB_CAMUNDA_COMPATIBILITY_TRANSLATION_MODE
        .equals(config.getCamundaCompatibilityMode()) && GROOVY_SCRIPTING_LANGUAGE.equalsIgnoreCase(shortName)) {
      return org.operaton.bpm.engine.impl.scripting.util.OperatonScriptEngineUtil.createGroovyScriptEngine();
    }


    return super.getEngineByName(shortName);
  }

  private static class OperatonClassLoader extends ClassLoader {

    public OperatonClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

      if (name != null && name.startsWith(CAMUNDA_NAMESPACE)) {
        name = name.replace(CAMUNDA_NAMESPACE, OPERATON_NAMESPACE);
      }

      return Class.forName(name);
    }
  }

  /**
   * Creates a OperatonClassLoader that can be used by external script engines.
   * This classloader automatically translates Camunda namespace classes to Operaton namespace.
   *
   * Example usage with Groovy:
   * <pre>
   * ClassLoader operatonClassLoader = CamundaScriptEngineManager.createOperatonClassLoader();
   * GroovyClassLoader groovyClassLoader = new GroovyClassLoader(operatonClassLoader);
   * ScriptEngine groovyEngine = new GroovyScriptEngineImpl(groovyClassLoader);
   * </pre>
   *
   * @return A ClassLoader that handles Operaton namespace translation
   */
  public static ClassLoader createOperatonClassLoader() {
    return createOperatonClassLoader(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a OperatonClassLoader with a specific parent ClassLoader.
   *
   * @param parent The parent ClassLoader
   * @return A ClassLoader that handles Operaton namespace translation
   */
  public static ClassLoader createOperatonClassLoader(ClassLoader parent) {
    return new OperatonClassLoader(parent);
  }

  /**
   * Fetches the config logic of a given engine from the mappings and executes it in case it exists.
   *
   * @param engineName the given engine name
   */
  protected void executeConfigurationBeforeEngineCreation(String engineName) {
    var config = engineNameToInitLogicMappings.get(engineName);
    if (config != null) {
      config.run();
    }
  }

  protected void disableGraalVMInterpreterOnlyModeWarnings() {
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
  }

  protected void disablePythonImportSiteWarnings() {
    System.setProperty("python.import.site", "false");
  }

  /**
   * Creates a Groovy script engine with OperatonClassLoader using reflection to avoid compile-time dependency.
   * Returns null if Groovy classes are not available at runtime.
   */
  protected ScriptEngine createGroovyScriptEngineWithOperatonClassLoader() {
    try {
      ClassLoader operatonClassLoader = createOperatonClassLoader();

      // Use reflection to create GroovyClassLoader
      Class<?> groovyClassLoaderClass = Class.forName("groovy.lang.GroovyClassLoader", true, operatonClassLoader);
      var groovyClassLoaderConstructor = groovyClassLoaderClass.getConstructor(ClassLoader.class);
      var groovyClassLoader = groovyClassLoaderConstructor.newInstance(operatonClassLoader);

      // Use reflection to create GroovyScriptEngineImpl
      Class<?> groovyScriptEngineClass = Class.forName("org.codehaus.groovy.jsr223.GroovyScriptEngineImpl", true, operatonClassLoader);
      var groovyScriptEngineConstructor = groovyScriptEngineClass.getConstructor(groovyClassLoaderClass);
      return (ScriptEngine) groovyScriptEngineConstructor.newInstance(groovyClassLoader);

    } catch (Exception e) {
      // Log the error and fall back to standard script engine manager
      System.err.println("Failed to create Groovy script engine with OperatonClassLoader: " + e.getMessage());
      return super.getEngineByName(GROOVY_SCRIPTING_LANGUAGE);
    }
  }
}
