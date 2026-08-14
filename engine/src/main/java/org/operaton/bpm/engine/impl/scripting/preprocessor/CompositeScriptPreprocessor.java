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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.scripting.ScriptLogger;

/**
 * Composite {@link ScriptPreprocessor} that applies configured preprocessors sequentially.
 */
public class CompositeScriptPreprocessor implements ScriptPreprocessor {

  private static final ScriptLogger LOG = ProcessEngineLogger.SCRIPT_LOGGER;

  private final List<ScriptPreprocessor> preprocessors;

  public CompositeScriptPreprocessor(List<ScriptPreprocessor> preprocessors) {
    if (preprocessors == null) {
      this.preprocessors = Collections.emptyList();
    } else {
      this.preprocessors = preprocessors.stream()
          .filter(Objects::nonNull)
          .toList();
    }
  }

  @Override
  public String process(ScriptPreprocessorRequest request) {
    if (request == null) {
      LOG.warnScriptPreprocessorRequestNull(getName());
      return null;
    }

    String script = request.getScript();
    if (script == null) {
      return null;
    }

    for (ScriptPreprocessor preprocessor : preprocessors) {
      ScriptPreprocessorRequest nextRequest = new ScriptPreprocessorRequest(script, request.getLanguage(), request.getVariableScope());
      String processedScript = preprocessor.process(nextRequest);
      if (processedScript != null) {
        script = processedScript;
      }
    }

    return script;
  }

  public List<ScriptPreprocessor> getPreprocessors() {
    return Collections.unmodifiableList(new ArrayList<>(preprocessors));
  }

  @Override
  public String getName() {
    return "CompositeScriptPreprocessor";
  }
}
