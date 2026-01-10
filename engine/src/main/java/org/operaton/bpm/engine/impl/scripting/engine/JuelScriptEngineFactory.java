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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Factory to create {@link JuelScriptEngine}s.
 *
 * @author Frederik Heremans
 */
public class JuelScriptEngineFactory implements ScriptEngineFactory {

  private static List<String> names;
  private static final List<String> extensions;
  private static final List<String> mimeTypes;

  static {
    names = Collections.unmodifiableList(Arrays.asList("juel"));
    extensions = names;
    mimeTypes = Collections.unmodifiableList(new ArrayList<String>(0));
  }

  @Override
  public String getEngineName() {
    return "juel";
  }

  @Override
  public String getEngineVersion() {
    return "1.0";
  }

  @Override
  public List<String> getExtensions() {
    return extensions;
  }

  @Override
  public String getLanguageName() {
    return "JSP 2.1 EL";
  }

  @Override
  public String getLanguageVersion() {
    return "2.1";
  }

  @Override
  public String getMethodCallSyntax(String obj, String method, String... arguments) {
    throw new UnsupportedOperationException("Method getMethodCallSyntax is not supported");
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
  public String getOutputStatement(String toDisplay) {
    // We will use out:print function to output statements
    StringBuilder stringBuffer = new StringBuilder();
    stringBuffer.append("out:print(\"");

    int length = toDisplay.length();
    for (int i = 0; i < length; i++) {
      char c = toDisplay.charAt(i);
      switch (c) {
      case '"':
        stringBuffer.append("\\\"");
        break;
      case '\\':
        stringBuffer.append("\\\\");
        break;
      default:
        stringBuffer.append(c);
        break;
      }
    }
    stringBuffer.append("\")");
    return stringBuffer.toString();
  }

  @Override
  public String getParameter(String key) {
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
  public String getProgram(String... statements) {
    // Each statement is wrapped in '${}' to comply with EL
    StringBuilder buf = new StringBuilder();
    if (statements.length != 0) {
      for (int i = 0; i < statements.length; i++) {
        buf.append("${");
        buf.append(statements[i]);
        buf.append("} ");
      }
    }
    return buf.toString();
  }

  @Override
  public ScriptEngine getScriptEngine() {
    return new JuelScriptEngine(this);
  }

}
