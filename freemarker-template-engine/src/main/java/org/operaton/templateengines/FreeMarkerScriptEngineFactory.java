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
package org.operaton.templateengines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * {@link ScriptEngineFactory} to create a JSR 223 compatible
 * wrapper of the FreeMarker template engine.
 *
 * @author Sebastian Menski
 */
public class FreeMarkerScriptEngineFactory implements ScriptEngineFactory {

  public static final String NAME = "freemarker";
  public static final String VERSION = "2.3.29";

  public static final List<String> names;
  public static final List<String> extensions;
  public static final List<String> mimeTypes;

  static {
    names = Collections.unmodifiableList(Arrays.asList(NAME, "Freemarker", "FreeMarker"));
    extensions = Collections.unmodifiableList(Collections.singletonList("ftl"));
    mimeTypes = Collections.emptyList();
  }

  @Override
  public String getEngineName() {
    return NAME;
  }

  @Override
  public String getEngineVersion() {
    return VERSION;
  }

  @Override
  public List<String> getExtensions() {
    return extensions;
  }

  @Override
  public List<String> getMimeTypes() {
    return mimeTypes;
  }

  @Override
  public List<String> getNames() {
    return names;
  }

  @Override
  public String getLanguageName() {
    return NAME;
  }

  @Override
  public String getLanguageVersion() {
    return VERSION;
  }

  @Override
  public Object getParameter(String key) {
    if (ScriptEngine.NAME.equals(key)) {
      return getLanguageName();
    } else if (ScriptEngine.ENGINE.equals(key)) {
      return getEngineName();
    } else if (ScriptEngine.ENGINE_VERSION.equals(key)) {
      return getEngineVersion();
    } else if (ScriptEngine.LANGUAGE.equals(key)) {
      return getLanguageName();
    } else if (ScriptEngine.LANGUAGE_VERSION.equals(key)) {
      return getLanguageVersion();
    } else if ("THREADING".equals(key)) {
      return "MULTITHREADED";
    } else {
      return null;
    }
  }

  @Override
  public String getMethodCallSyntax(String object, String method, String... args) {
    return "${%s.%s(%s)}".formatted(object, method, joinStrings(", ", args));
  }

  @Override
  public String getOutputStatement(String toDisplay) {
    return toDisplay;
  }

  @Override
  public String getProgram(String... statements) {
    return joinStrings("\n", statements);
  }

  protected String joinStrings(String delimiter, String[] values) {
    if (values == null) {
      return null;
    }
    else {
      return String.join(delimiter, values);
    }
  }

  @Override
  public ScriptEngine getScriptEngine() {
    return new FreeMarkerScriptEngine(this);
  }

}
