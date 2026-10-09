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
package org.operaton.bpm.engine.impl.scripting.preprocessor;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.delegate.VariableScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ScriptPreprocessorRequestTest {

  @Test
  void shouldConstructWithAllParameters() {
    String script = "var x = 1;";
    String language = "javascript";
    VariableScope variableScope = mock(VariableScope.class);

    ScriptPreprocessorRequest request = new ScriptPreprocessorRequest(script, language, variableScope);

    assertThat(request.getScript()).isEqualTo(script);
    assertThat(request.getLanguage()).isEqualTo(language);
    assertThat(request.getVariableScope()).isEqualTo(variableScope);
  }

  @Test
  void shouldConstructWithAllNullFields() {
    ScriptPreprocessorRequest request = new ScriptPreprocessorRequest(null, null, null);

    assertThat(request.getScript()).isNull();
    assertThat(request.getLanguage()).isNull();
    assertThat(request.getVariableScope()).isNull();
  }

  @Test
  void shouldReturnEmptyScriptAsIs() {
    ScriptPreprocessorRequest request = new ScriptPreprocessorRequest("", "groovy", null);

    assertThat(request.getScript()).isEmpty();
    assertThat(request.getLanguage()).isEqualTo("groovy");
    assertThat(request.getVariableScope()).isNull();
  }
}
