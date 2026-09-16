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

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeScriptPreprocessorTest {

  @Test
  void shouldApplyPreprocessorsInOrder() {
    ScriptPreprocessor first = scriptPreprocessor("first", request -> request.getScript() + "-first");
    ScriptPreprocessor second = scriptPreprocessor("second", request -> request.getScript() + "-second");

    CompositeScriptPreprocessor composite = new CompositeScriptPreprocessor(Arrays.asList(first, second));

    String processed = composite.process(new ScriptPreprocessorRequest("source", "groovy", null));

    assertThat(processed).isEqualTo("source-first-second");
  }

  @Test
  void shouldContinueWhenPreprocessorReturnsNull() {
    ScriptPreprocessor first = scriptPreprocessor("first", request -> null);
    ScriptPreprocessor second = scriptPreprocessor("second", request -> request.getScript() + "-processed");

    CompositeScriptPreprocessor composite = new CompositeScriptPreprocessor(Arrays.asList(first, second));

    String processed = composite.process(new ScriptPreprocessorRequest("source", "groovy", null));

    assertThat(processed).isEqualTo("source-processed");
  }

  @Test
  void shouldIgnoreNullPreprocessorsInConstructor() {
    ScriptPreprocessor nonNull = scriptPreprocessor("only", request -> request.getScript() + "-ok");

    CompositeScriptPreprocessor composite = new CompositeScriptPreprocessor(Arrays.asList(null, nonNull, null));

    String processed = composite.process(new ScriptPreprocessorRequest("source", "groovy", null));

    assertThat(processed).isEqualTo("source-ok");
    assertThat(composite.getPreprocessors()).containsExactly(nonNull);
  }

  @Test
  void shouldReturnNullWhenRequestIsNull() {
    CompositeScriptPreprocessor composite = new CompositeScriptPreprocessor(null);

    assertThat(composite.process(null)).isNull();
  }

  @Test
  void shouldReturnNullWhenScriptIsNull() {
    AtomicBoolean called = new AtomicBoolean(false);
    ScriptPreprocessor preprocessor = scriptPreprocessor("probe", request -> {
      called.set(true);
      return request.getScript() + "-changed";
    });

    CompositeScriptPreprocessor composite = new CompositeScriptPreprocessor(Collections.singletonList(preprocessor));

    String result = composite.process(new ScriptPreprocessorRequest(null, "groovy", null));

    assertThat(result).isNull();
    assertThat(called).isFalse();
  }

  @Test
  void shouldProcessEmptyScript() {
    ScriptPreprocessor preprocessor = scriptPreprocessor("probe", request -> request.getScript() + "-changed");

    CompositeScriptPreprocessor composite = new CompositeScriptPreprocessor(Collections.singletonList(preprocessor));

    String result = composite.process(new ScriptPreprocessorRequest("", "groovy", null));

    assertThat(result).isEqualTo("-changed");
  }

  @Test
  void shouldExposeCompositeName() {
    CompositeScriptPreprocessor composite = new CompositeScriptPreprocessor(null);

    assertThat(composite.getName()).isEqualTo("CompositeScriptPreprocessor");
  }

  private ScriptPreprocessor scriptPreprocessor(String name, Function<ScriptPreprocessorRequest, String> behavior) {
    return new ScriptPreprocessor() {
      @Override
      public String process(ScriptPreprocessorRequest request) {
        return behavior.apply(request);
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }
}
