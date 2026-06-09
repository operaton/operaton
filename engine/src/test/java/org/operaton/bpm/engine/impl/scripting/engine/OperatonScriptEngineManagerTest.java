/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.engine.impl.scripting.engine;

import java.io.Reader;
import java.net.URL;
import java.util.List;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.context.Context;

import static org.assertj.core.api.Assertions.assertThat;

class OperatonScriptEngineManagerTest {

  @Test
  void shouldCreateScriptEngineWithForkClassNameMappingClassLoaderWhenCompatibilityMappingIsEnabled() {
    Context.setProcessEngineConfiguration(new StandaloneInMemProcessEngineConfiguration()
      .setEnableForkClassNameCompatibilityMapping(true));

    try {
      OperatonScriptEngineManager scriptEngineManager = new OperatonScriptEngineManager();
      scriptEngineManager.registerEngineName("mapping-test", new RecordingScriptEngineFactory());

      RecordingScriptEngine scriptEngine = (RecordingScriptEngine) scriptEngineManager.getEngineByName("mapping-test");

      assertThat(scriptEngine.loadedClass).isEqualTo(BpmnError.class);
      assertThat(scriptEngine.loadedClassResource).isNotNull();
    } finally {
      Context.removeProcessEngineConfiguration();
    }
  }

  private static class RecordingScriptEngineFactory implements ScriptEngineFactory {

    @Override
    public String getEngineName() {
      return "mapping-test";
    }

    @Override
    public String getEngineVersion() {
      return "1";
    }

    @Override
    public List<String> getExtensions() {
      return List.of();
    }

    @Override
    public List<String> getMimeTypes() {
      return List.of();
    }

    @Override
    public List<String> getNames() {
      return List.of("mapping-test");
    }

    @Override
    public String getLanguageName() {
      return "mapping-test";
    }

    @Override
    public String getLanguageVersion() {
      return "1";
    }

    @Override
    public Object getParameter(String key) {
      return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String method, String... args) {
      return null;
    }

    @Override
    public String getOutputStatement(String toDisplay) {
      return null;
    }

    @Override
    public String getProgram(String... statements) {
      return null;
    }

    @Override
    public ScriptEngine getScriptEngine() {
      try {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> loadedClass = contextClassLoader
            .loadClass("org.cibseven.bpm.engine.delegate.BpmnError");
        URL loadedClassResource = contextClassLoader
            .getResource("org/cibseven/bpm/engine/delegate/BpmnError.class");
        return new RecordingScriptEngine(this, loadedClass, loadedClassResource);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }
  }

  private static class RecordingScriptEngine extends AbstractScriptEngine {

    private final ScriptEngineFactory factory;
    private final Class<?> loadedClass;
    private final URL loadedClassResource;

    private RecordingScriptEngine(ScriptEngineFactory factory, Class<?> loadedClass, URL loadedClassResource) {
      this.factory = factory;
      this.loadedClass = loadedClass;
      this.loadedClassResource = loadedClassResource;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
      return null;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
      return null;
    }

    @Override
    public Bindings createBindings() {
      return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
      return factory;
    }
  }
}
