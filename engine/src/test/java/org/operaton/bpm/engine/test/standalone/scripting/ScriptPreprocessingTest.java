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
package org.operaton.bpm.engine.test.standalone.scripting;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.scripting.ExecutableScript;
import org.operaton.bpm.engine.impl.scripting.ScriptFactory;
import org.operaton.bpm.engine.impl.scripting.SourceExecutableScript;
import org.operaton.bpm.engine.impl.scripting.env.ScriptingEnvironment;
import org.operaton.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.operaton.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessorRequest;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ProcessEngineExtension.class)
class ScriptPreprocessingTest {

  private static final String SCRIPT_LANGUAGE = "groovy";
  private static final String ORIGINAL_SCRIPT = "1 + 1";
  private static final String PREPROCESSED_SCRIPT = "1 + 2";

  ProcessEngineConfigurationImpl processEngineConfiguration;
  private ScriptFactory scriptFactory;

  @BeforeEach
  void setUp() {
    scriptFactory = processEngineConfiguration.getScriptFactory();
    processEngineConfiguration.setEnableScriptPreprocessing(false);
    processEngineConfiguration.setScriptPreprocessors(null);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setEnableScriptPreprocessing(false);
    processEngineConfiguration.setScriptPreprocessors(null);
  }

  @Test
  void shouldReturnOriginalScriptWhenPreprocessingDisabled() {
    AtomicReference<String> capturedScript = new AtomicReference<>();
    processEngineConfiguration.setEnableScriptPreprocessing(false);
    processEngineConfiguration.addScriptPreprocessor(capturingPreprocessor(capturedScript, PREPROCESSED_SCRIPT));

    SourceExecutableScript script = createScript(ORIGINAL_SCRIPT);

    Object result = executeScript(script);

    assertThat(capturedScript.get()).isNull();
    assertThat(result).isEqualTo(2);
  }

  @Test
  void shouldApplyPreprocessorWhenEnabled() {
    AtomicReference<String> capturedScript = new AtomicReference<>();
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.addScriptPreprocessor(capturingPreprocessor(capturedScript, PREPROCESSED_SCRIPT));

    SourceExecutableScript script = createScript(ORIGINAL_SCRIPT);

    Object result = executeScript(script);

    assertThat(capturedScript.get()).isEqualTo(ORIGINAL_SCRIPT);
    assertThat(result).isEqualTo(3);
  }

  @Test
  void shouldFallBackToOriginalScriptWhenPreprocessorReturnsNull() {
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.addScriptPreprocessor(nullReturningPreprocessor());

    SourceExecutableScript script = createScript(ORIGINAL_SCRIPT);

    Object result = executeScript(script);

    assertThat(result).isEqualTo(2);
  }

  @Test
  void shouldFallBackToOriginalScriptWhenPreprocessorThrows() {
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.addScriptPreprocessor(throwingPreprocessor());

    SourceExecutableScript script = createScript(ORIGINAL_SCRIPT);

    Object result = executeScript(script);

    assertThat(result).isEqualTo(2);
  }

  @Test
  void shouldApplyPreprocessorsInOrderWhenMultipleConfigured() {
    AtomicReference<String> inputToSecond = new AtomicReference<>();
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.addScriptPreprocessor(appendingPreprocessor("-a"));
    processEngineConfiguration.addScriptPreprocessor(capturingPreprocessor(inputToSecond, PREPROCESSED_SCRIPT));

    SourceExecutableScript script = createScript(ORIGINAL_SCRIPT);

    Object result = executeScript(script);

    assertThat(inputToSecond.get()).isEqualTo("1 + 1-a");
    assertThat(result).isEqualTo(3);
  }

  private SourceExecutableScript createScript(String source) {
    return (SourceExecutableScript) scriptFactory.createScriptFromSource(SCRIPT_LANGUAGE, source);
  }

  private Object executeScript(ExecutableScript script) {
    ScriptingEnvironment scriptingEnvironment = processEngineConfiguration.getScriptingEnvironment();
    return processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(commandContext -> scriptingEnvironment.execute(script, null));
  }

  private ScriptPreprocessor capturingPreprocessor(AtomicReference<String> capture, String output) {
    return new ScriptPreprocessor() {
      @Override
      public String process(ScriptPreprocessorRequest request) {
        capture.set(request.getScript());
        return output;
      }

      @Override
      public String getName() {
        return "capturingPreprocessor";
      }
    };
  }

  private ScriptPreprocessor nullReturningPreprocessor() {
    return new ScriptPreprocessor() {
      @Override
      public String process(ScriptPreprocessorRequest request) {
        return null;
      }

      @Override
      public String getName() {
        return "nullReturningPreprocessor";
      }
    };
  }

  private ScriptPreprocessor throwingPreprocessor() {
    return new ScriptPreprocessor() {
      @Override
      public String process(ScriptPreprocessorRequest request) {
        throw new IllegalStateException("Simulated preprocessing failure");
      }

      @Override
      public String getName() {
        return "throwingPreprocessor";
      }
    };
  }

  private ScriptPreprocessor appendingPreprocessor(String suffix) {
    return new ScriptPreprocessor() {
      @Override
      public String process(ScriptPreprocessorRequest request) {
        return request.getScript() + suffix;
      }

      @Override
      public String getName() {
        return "appendingPreprocessor[" + suffix + "]";
      }
    };
  }
}
