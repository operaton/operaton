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
package org.operaton.bpm.engine.impl.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.scripting.preprocessor.CompositeScriptPreprocessor;
import org.operaton.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.operaton.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessorRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessEngineConfigurationScriptPreprocessorTest {

  private ProcessEngineConfigurationImpl configuration;

  @BeforeEach
  void setUp() {
    configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResourceDefault();
  }

  @Test
  void shouldBeDisabledByDefault() {
    assertThat(configuration.isEnableScriptPreprocessing()).isFalse();
  }

  @Test
  void shouldEnableScriptPreprocessing() {
    configuration.setEnableScriptPreprocessing(true);

    assertThat(configuration.isEnableScriptPreprocessing()).isTrue();
  }

  @Test
  void shouldReturnNullEffectivePreprocessorWhenDisabled() {
    configuration.setEnableScriptPreprocessing(false);
    configuration.setScriptPreprocessors(Collections.singletonList(mockPreprocessor("p1")));

    assertThat(configuration.getEffectiveScriptPreprocessor()).isNull();
  }

  @Test
  void shouldReturnNullEffectivePreprocessorWhenNoPreprocessorsConfigured() {
    configuration.setEnableScriptPreprocessing(true);
    configuration.setScriptPreprocessors(null);

    assertThat(configuration.getEffectiveScriptPreprocessor()).isNull();
  }

  @Test
  void shouldReturnSinglePreprocessorDirectlyWhenOnlyOneConfigured() {
    ScriptPreprocessor preprocessor = mockPreprocessor("only");
    configuration.setEnableScriptPreprocessing(true);
    configuration.setScriptPreprocessors(Collections.singletonList(preprocessor));

    ScriptPreprocessor effective = configuration.getEffectiveScriptPreprocessor();

    assertThat(effective).isSameAs(preprocessor);
  }

  @Test
  void shouldReturnCompositePreprocessorWhenMultipleConfigured() {
    configuration.setEnableScriptPreprocessing(true);
    configuration.setScriptPreprocessors(Arrays.asList(mockPreprocessor("p1"), mockPreprocessor("p2")));

    ScriptPreprocessor effective = configuration.getEffectiveScriptPreprocessor();

    assertThat(effective).isInstanceOf(CompositeScriptPreprocessor.class);
  }

  @Test
  void shouldInvalidateCacheWhenPreprocessorsReplaced() {
    ScriptPreprocessor first = mockPreprocessor("first");
    ScriptPreprocessor second = mockPreprocessor("second");
    configuration.setEnableScriptPreprocessing(true);
    configuration.setScriptPreprocessors(Collections.singletonList(first));
    assertThat(configuration.getEffectiveScriptPreprocessor()).isSameAs(first);

    configuration.setScriptPreprocessors(Collections.singletonList(second));

    assertThat(configuration.getEffectiveScriptPreprocessor()).isSameAs(second);
  }

  @Test
  void shouldAddScriptPreprocessor() {
    ScriptPreprocessor preprocessor = mockPreprocessor("p1");

    configuration.addScriptPreprocessor(preprocessor);

    List<ScriptPreprocessor> preprocessors = configuration.getScriptPreprocessors();
    assertThat(preprocessors).containsExactly(preprocessor);
  }

  @Test
  void shouldIgnoreNullOnAddScriptPreprocessor() {
    configuration.addScriptPreprocessor(null);

    assertThat(configuration.getScriptPreprocessors()).isNullOrEmpty();
  }

  @Test
  void shouldReturnDefensiveCopyOfPreprocessors() {
    configuration.setScriptPreprocessors(Collections.singletonList(mockPreprocessor("p1")));

    List<ScriptPreprocessor> copy = configuration.getScriptPreprocessors();
    copy.clear();

    assertThat(configuration.getScriptPreprocessors()).hasSize(1);
  }

  private ScriptPreprocessor mockPreprocessor(String name) {
    ScriptPreprocessor preprocessor = mock(ScriptPreprocessor.class);
    when(preprocessor.getName()).thenReturn(name);
    when(preprocessor.process(any(ScriptPreprocessorRequest.class))).thenReturn(null);
    return preprocessor;
  }
}
