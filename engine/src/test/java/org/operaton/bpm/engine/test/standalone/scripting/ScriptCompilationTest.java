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
package org.operaton.bpm.engine.test.standalone.scripting;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.impl.scripting.ExecutableScript;
import org.operaton.bpm.engine.impl.scripting.ScriptFactory;
import org.operaton.bpm.engine.impl.scripting.SourceExecutableScript;
import org.operaton.bpm.engine.impl.scripting.env.ScriptingEnvironment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Stefan Hentschel.
 */
public class ScriptCompilationTest extends PluggableProcessEngineTest {

  protected static final String SCRIPT_LANGUAGE = "groovy";
  protected static final String EXAMPLE_SCRIPT = "println 'hello world'";

  protected ScriptFactory scriptFactory;

  @Before
  public void setUp() {
    scriptFactory = processEngineConfiguration.getScriptFactory();
  }

  protected SourceExecutableScript createScript(String language, String source) {
    return (SourceExecutableScript) scriptFactory.createScriptFromSource(language, source);
  }

  @Test
  public void testScriptShouldBeCompiledByDefault() {
    // when a script is created
    SourceExecutableScript script = createScript(SCRIPT_LANGUAGE, EXAMPLE_SCRIPT);
    assertThat(script).isNotNull();

    // then it should not be compiled on creation
    assertThat(script.isShouldBeCompiled()).isTrue();
    assertThat(script.getCompiledScript()).isNull();

    // but after first execution
    executeScript(script);

    // it was compiled
    assertThat(script.isShouldBeCompiled()).isFalse();
    assertThat(script.getCompiledScript()).isNotNull();
  }

  @Test
  public void testDisableScriptCompilation() {
    // when script compilation is disabled and a script is created
    processEngineConfiguration.setEnableScriptCompilation(false);
    SourceExecutableScript script = createScript(SCRIPT_LANGUAGE, EXAMPLE_SCRIPT);
    assertThat(script).isNotNull();

    // then it should not be compiled on creation
    assertThat(script.isShouldBeCompiled()).isTrue();
    assertThat(script.getCompiledScript()).isNull();

    // and after first execution
    executeScript(script);

    // it was also not compiled
    assertThat(script.isShouldBeCompiled()).isFalse();
    assertThat(script.getCompiledScript()).isNull();

    // re-enable script compilation
    processEngineConfiguration.setEnableScriptCompilation(true);
  }

  @Test
  public void testDisableScriptCompilationByDisabledScriptEngineCaching() {
    // when script engine caching is disabled and a script is created
    processEngineConfiguration.setEnableScriptEngineCaching(false);
    SourceExecutableScript script = createScript(SCRIPT_LANGUAGE, EXAMPLE_SCRIPT);
    assertThat(script).isNotNull();

    // then it should not be compiled on creation
    assertThat(script.isShouldBeCompiled()).isTrue();
    assertThat(script.getCompiledScript()).isNull();

    // and after first execution
    executeScript(script);

    // it was also not compiled
    assertThat(script.isShouldBeCompiled()).isFalse();
    assertThat(script.getCompiledScript()).isNull();

    // re-enable script engine caching
    processEngineConfiguration.setEnableScriptEngineCaching(true);
  }

  @Test
  public void testOverrideScriptSource() {
    // when a script is created and executed
    SourceExecutableScript script = createScript(SCRIPT_LANGUAGE, EXAMPLE_SCRIPT);
    assertThat(script).isNotNull();
    executeScript(script);

    // it was compiled
    assertThat(script.isShouldBeCompiled()).isFalse();
    assertThat(script.getCompiledScript()).isNotNull();

    // if the script source changes
    script.setScriptSource(EXAMPLE_SCRIPT);

    // then it should not be compiled after change
    assertThat(script.isShouldBeCompiled()).isTrue();
    assertThat(script.getCompiledScript()).isNull();

    // but after next execution
    executeScript(script);

    // it is compiled again
    assertThat(script.isShouldBeCompiled()).isFalse();
    assertThat(script.getCompiledScript()).isNotNull();
  }

  protected Object executeScript(final ExecutableScript script) {
    final ScriptingEnvironment scriptingEnvironment = processEngineConfiguration.getScriptingEnvironment();
    return processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(commandContext -> scriptingEnvironment.execute(script, null));
  }

}
