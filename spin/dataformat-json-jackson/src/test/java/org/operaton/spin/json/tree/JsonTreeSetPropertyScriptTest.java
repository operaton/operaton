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
package org.operaton.spin.json.tree;

import org.operaton.spin.impl.test.Script;
import org.operaton.spin.impl.test.ScriptTest;
import org.operaton.spin.impl.test.ScriptVariable;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPropertyException;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON_FILE_NAME;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thorben Lindhauer
 */
public abstract class JsonTreeSetPropertyScriptTest extends ScriptTest {

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetStringProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    String value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();
    assertThat(value).isEqualTo("42!");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceStringProperty() {
    String oldValue = script.getVariable("oldValue");
    String newValue = script.getVariable("newValue");

    assertThat(newValue)
      .isNotEqualTo(oldValue)
      .isEqualTo("new Order");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetIntegerProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    Number value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();
    // Ruby casts Number to long
    assertThat(value.intValue()).isEqualTo(42);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceIntegerProperty() {
    String oldValue = script.getVariable("oldValue");
    Number newValue = script.getVariable("newValue");

    assertThat(newValue.toString()).isNotEqualTo(oldValue);
    // Ruby casts Number to long
    assertThat(newValue.intValue()).isEqualTo(42);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetFloatProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    Number value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();

    // python returns Double, needs to cast to Float
    assertThat(value.floatValue()).isEqualTo(42.00F);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceFloatProperty() {
    String oldValue = script.getVariable("oldValue");
    Number newValue = script.getVariable("newValue");

    assertThat(newValue.toString()).isNotEqualTo(oldValue);
    // Python returns a double instead a float
    assertThat(newValue.floatValue()).isEqualTo(42.00F);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetLongProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    Number value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();

    // python returns BigInt, needs to cast to Long
    assertThat(value.floatValue()).isEqualTo(4200000000L);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceLongProperty() {
    String oldValue = script.getVariable("oldValue");
    Number newValue = script.getVariable("newValue");

    assertThat(newValue.toString()).isNotEqualTo(oldValue);
    // python returns a BigInt, needs to cast it
    assertThat(newValue.longValue()).isEqualTo(4200000000L);
  }

  /**
   * Please note: in jython there is a known issue that Boolean
   * and boolean values are casted to long if a matching method
   * is found first. See script source code for workaround.
   */
  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetBooleanProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    Boolean value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();
    assertThat(value).isFalse();
  }

  /**
   * Please note: in jython there is a known issue that Boolean
   * and boolean values are casted to long if a matching method
   * is found first. See script source code for workaround.
   */
  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceBooleanProperty() {
    String oldValue = script.getVariable("oldValue");
    Boolean newValue = script.getVariable("newValue");

    assertThat(newValue).isNotEqualTo(oldValue);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetArrayProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    String value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();
    assertThat(propertyNode.isArray()).isTrue();
    assertThat(value).isEqualTo("test2");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceArrayProperty() {
    SpinJsonNode oldValue = script.getVariable("oldValue");
    SpinJsonNode newValue = script.getVariable("newValue");

    assertThat(oldValue.isString()).isTrue();
    assertThat(newValue).isNotEqualTo(oldValue);
    assertThat(newValue.isArray()).isTrue();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetObjectProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    String value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();
    assertThat(propertyNode.isObject()).isTrue();
    assertThat(value).isEqualTo("42!");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceObjectProperty() {

    SpinJsonNode oldValue = script.getVariable("oldValue");
    SpinJsonNode newValue = script.getVariable("newValue");

    assertThat(oldValue.isString()).isTrue();
    assertThat(newValue).isNotEqualTo(oldValue);
    assertThat(newValue.isObject()).isTrue();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldSetNullProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    Object value = script.getVariable("value");

    assertThat(propertyNode).isNotNull();
    assertThat(propertyNode.isNull()).isTrue();
    assertThat(value).isNull();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReplaceNullProperty() {
    SpinJsonNode propertyNode = script.getVariable("propertyNode");
    Object newValue = script.getVariable("newValue");

    assertThat(propertyNode).isNotNull();
    assertThat(propertyNode.isNull()).isTrue();
    assertThat(newValue).isNull();
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailWhileSettingObject() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("date", new Date());
    assertThrows(SpinJsonPropertyException.class, () -> failingWithException(variables));
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailWhileSettingArray() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("date", new String[] { "a", "b", "c" });
    assertThrows(SpinJsonPropertyException.class, () -> failingWithException(variables));
  }

}

