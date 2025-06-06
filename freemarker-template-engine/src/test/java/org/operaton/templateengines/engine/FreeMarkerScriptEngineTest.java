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
package org.operaton.templateengines.engine;

import java.util.Arrays;
import java.util.Collection;

import javax.script.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.templateengines.engine.util.Greeter;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Sebastian Menski
 */
class FreeMarkerScriptEngineTest {

  protected static ScriptEngine scriptEngine;
  protected Bindings bindings;

  protected String template;
  protected String expected;

  @BeforeAll
  static void getScriptEngine() {
    ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    scriptEngine = scriptEngineManager.getEngineByName("freemarker");
  }

  @BeforeEach
  void createBindings() {
    bindings = new SimpleBindings();
  }

  protected String evaluate(String template) throws ScriptException {
    return (String) scriptEngine.eval(template, bindings);
  }

  @Test
  void scriptEngineExists() {
    assertThat(scriptEngine).isNotNull();
  }

  @Test
  void variableExpansion() throws ScriptException {
    bindings.put("name", "world");
    expected = "Hello world!";
    template = "Hello ${name}!";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void javaProperties() throws ScriptException {
    bindings.put("greeter", new Greeter());
    expected = "!";
    template = "${greeter.suffix}";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void javaMethodCall() throws ScriptException {
    bindings.put("greeter", new Greeter());
    expected = "Hello world!";
    template = "${greeter.hello('world')}";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void javaArrays() throws ScriptException {
    bindings.put("myarray", new String[]{"hello", "foo", "world", "bar"});
    expected = "4 hello world!";
    template = "${myarray?size} ${myarray[0]} ${myarray[2]}!";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void javaBoolean() throws ScriptException {
    bindings.put("mybool", false);
    expected = "Hello world!";
    template = "<#if mybool>okey<#else>Hello world!</#if>";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void javaInteger() throws ScriptException {
    bindings.put("myint", 6);
    expected = "42";
    template = "<#assign myint = myint + 36>${myint}";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void javaCollection() throws ScriptException {
    Collection<String> names = Arrays.asList("tweety", "duffy", "tom");
    bindings.put("names", names);
    expected = "tweety, duffy, tom";
    template = "<#list names as name>${name}<#if name_has_next>, </#if></#list>";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void defineBlock() throws ScriptException {
    bindings.put("who", "world");
    expected = "Hello world!";
    template = "<#macro block>Hello ${who}!</#macro><@block/>";
    assertThat(evaluate(template)).isEqualTo(expected);
  }

  @Test
  void failingEvaluation() {
    try {
      String invalidTemplate = "${}";
      evaluate(invalidTemplate);
      fail("Expected a ScriptException");
    } catch (ScriptException e) {
      // happy path
    }
  }

}
