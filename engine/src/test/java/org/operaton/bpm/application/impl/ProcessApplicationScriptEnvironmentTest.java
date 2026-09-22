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
package org.operaton.bpm.application.impl;

import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.SimpleBindings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.engine.impl.scripting.engine.DefaultScriptEngineResolver;
import org.operaton.bpm.engine.impl.scripting.engine.OperatonScriptEngineManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProcessApplicationScriptEnvironmentTest {

  protected static final String PROCESS_APPLICATION_SCRIPT_ENGINE_NAME = "processApplicationScriptEngine";

  @Test
  void shouldUseOperatonScriptEngineManagerWithProcessApplicationClassloader(@TempDir Path serviceRoot)
      throws Exception {
    Path servicesDir = serviceRoot.resolve("META-INF/services");
    Files.createDirectories(servicesDir);
    Files.writeString(servicesDir.resolve(ScriptEngineFactory.class.getName()),
        ProcessApplicationTestScriptEngineFactory.class.getName());

    URL[] processApplicationClasspath = { serviceRoot.toUri().toURL() };
    try (URLClassLoader processApplicationClassloader =
        new URLClassLoader(processApplicationClasspath, getClass().getClassLoader())) {
      ProcessApplicationInterface processApplication = mock(ProcessApplicationInterface.class);
      when(processApplication.getProcessApplicationClassloader()).thenReturn(processApplicationClassloader);
      ProcessApplicationScriptEnvironment scriptEnvironment =
          new ProcessApplicationScriptEnvironment(processApplication);

      ScriptEngine scriptEngine =
          scriptEnvironment.getScriptEngineForName(PROCESS_APPLICATION_SCRIPT_ENGINE_NAME, false);

      assertThat(scriptEngine).isInstanceOf(ProcessApplicationTestScriptEngine.class);
      assertThat(scriptEnvironment.processApplicationScriptEngineResolver)
          .isInstanceOf(DefaultScriptEngineResolver.class);
      assertThat(scriptEnvironment.processApplicationScriptEngineResolver.getScriptEngineManager())
          .isInstanceOf(OperatonScriptEngineManager.class);
      verify(processApplication).getProcessApplicationClassloader();
    }
  }

  public static class ProcessApplicationTestScriptEngineFactory implements ScriptEngineFactory {

    @Override
    public String getEngineName() {
      return PROCESS_APPLICATION_SCRIPT_ENGINE_NAME;
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
      return List.of(PROCESS_APPLICATION_SCRIPT_ENGINE_NAME);
    }

    @Override
    public String getLanguageName() {
      return PROCESS_APPLICATION_SCRIPT_ENGINE_NAME;
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
    public String getMethodCallSyntax(String obj, String m, String... args) {
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
      return new ProcessApplicationTestScriptEngine(this);
    }

  }

  public static class ProcessApplicationTestScriptEngine extends AbstractScriptEngine {

    protected final ScriptEngineFactory factory;

    protected ProcessApplicationTestScriptEngine(ScriptEngineFactory factory) {
      this.factory = factory;
    }

    @Override
    public Object eval(String script, ScriptContext context) {
      return null;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) {
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
