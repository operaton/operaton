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

import org.operaton.spin.Spin;
import org.operaton.spin.impl.test.Script;
import org.operaton.spin.impl.test.ScriptTest;
import org.operaton.spin.impl.test.ScriptVariable;
import org.operaton.spin.json.SpinJsonDataFormatException;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPropertyException;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON_FILE_NAME;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thorben Lindhauer
 *
 */
public abstract class JsonTreeReadPropertyScriptTest extends ScriptTest {

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadProperty() {
    SpinJsonNode property = script.getVariable("property");
    String value = script.getVariable("value");

    assertThat(property).isNotNull();
    assertThat(value).isEqualTo("order1");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldCheckStringValue() {
    Boolean value1 = script.getVariable("value1");
    Boolean value2 = script.getVariable("value2");
    Boolean value3 = script.getVariable("value3");
    Boolean value4 = script.getVariable("value4");
    Boolean value5 = script.getVariable("value5");

    assertThat(value1).isTrue();
    assertThat(value2).isFalse();
    assertThat(value3).isFalse();
    assertThat(value4).isFalse();
    assertThat(value5).isFalse();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldCheckNumberValue() {
    Boolean value1 = script.getVariable("value1");
    Boolean value2 = script.getVariable("value2");
    Boolean value3 = script.getVariable("value3");
    Boolean value4 = script.getVariable("value4");
    Boolean value5 = script.getVariable("value5");

    assertThat(value1).isFalse();
    assertThat(value2).isTrue();
    assertThat(value3).isFalse();
    assertThat(value4).isFalse();
    assertThat(value5).isFalse();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldCheckBooleanValue() {
    Boolean value1 = script.getVariable("value1");
    Boolean value2 = script.getVariable("value2");
    Boolean value3 = script.getVariable("value3");
    Boolean value4 = script.getVariable("value4");
    Boolean value5 = script.getVariable("value5");

    assertThat(value1).isFalse();
    assertThat(value2).isFalse();
    assertThat(value3).isFalse();
    assertThat(value4).isFalse();
    assertThat(value5).isTrue();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldCheckArrayValue() {
    Boolean value1 = script.getVariable("value1");
    Boolean value2 = script.getVariable("value2");
    Boolean value3 = script.getVariable("value3");
    Boolean value4 = script.getVariable("value4");
    Boolean value5 = script.getVariable("value5");

    assertThat(value1).isFalse();
    assertThat(value2).isFalse();
    assertThat(value3).isTrue();
    assertThat(value4).isFalse();
    assertThat(value5).isFalse();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldCheckValue() {
    Object orderValue = script.getVariable("value1");
    Object dueUntilValue = script.getVariable("value2");

    assertThat(orderValue)
      .isInstanceOf(String.class)
      .isEqualTo("order1");

    assertThat(dueUntilValue)
      .isInstanceOf(Number.class);

    assertThat(((Number) dueUntilValue).longValue())
      .isEqualTo(20150112L);
  }

  /**
   * One for array
   */
  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToCheckObject(){
    assertThrows(SpinJsonDataFormatException.class, this::failingWithException);
  }

  /**
   * One for child node
   */
  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToCheckObject2(){
    assertThrows(SpinJsonDataFormatException.class, this::failingWithException);
  }

  /**
   * One for not existent property
   */
  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToReadProperty(){
    assertThrows(SpinJsonPropertyException.class, this::failingWithException);
  }

  /**
   * One for property argument equals null
   */
  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToReadProperty2(){
    assertThrows(IllegalArgumentException.class, this::failingWithException);
  }


  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadTextValue() {
    String value1 = script.getVariable("value1");
    String value2 = script.getVariable("value2");
    String value3 = script.getVariable("value3");

    assertThat(value1).isEqualTo("order1");
    assertThat(value2).isEqualTo("Kermit");
    assertThat(value3).isEqualTo("operatonBPM");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadNumberValue() {
    Number value1 = script.getVariable("value1");
    Number value2 = script.getVariable("value2");
    Number value3 = script.getVariable("value3");

    assertThat(value1.longValue()).isEqualTo(20150112L);

    // python returns bigInt instead of Long
    assertThat(value2.longValue()).isEqualTo(1234567890987654321L);
    assertThat(value3).isEqualTo(1234567.13);
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToReadNumberValue() {
    assertThrows(SpinJsonDataFormatException.class, this::failingWithException);
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToReadBooleanValue() {
    assertThrows(SpinJsonDataFormatException.class, this::failingWithException);
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToReadStringValue() {
    assertThrows(SpinJsonDataFormatException.class, this::failingWithException);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadBooleanValue() {
    Boolean value1 = script.getVariable("value1");

    assertThat(value1).isTrue();
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadChildNode() {
    SpinJsonNode childNode = script.getVariable("childNode");
    String value = script.getVariable("value");

    assertThat(childNode).isNotNull();
    assertThat(value).isEqualTo("operatonBPM");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadChildNodeProperty() {
    SpinJsonNode property1 = script.getVariable("property1");
    SpinJsonNode property2 = script.getVariable("property2");

    Number value1 = script.getVariable("value1");
    String value2 = script.getVariable("value2");

    assertThat(property1).isNotNull();
    assertThat(property2).isNotNull();

    // Ruby casts this to long instead int
    assertThat(value1.longValue()).isEqualTo(1234567L);
    assertThat(value2).isEqualTo("dollar");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadObjectInArrayChildNode() {
    SpinJsonNode property1 = script.getVariable("property1");
    SpinJsonNode property2 = script.getVariable("property2");

    String value1 = script.getVariable("value1");
    String value2 = script.getVariable("value2");

    assertThat(property1).isNotNull();
    assertThat(property2).isNotNull();

    assertThat(value1).isEqualTo("Kermit");
    assertThat(value2).isEqualTo("Waldo");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadListOfNodes() {
    String value1 = script.getVariable("value1");
    String value2 = script.getVariable("value2");
    String value3 = script.getVariable("value3");

    assertThat(value1).isEqualTo("order");
    assertThat(value2).isEqualTo("dueUntil");
    assertThat(value3).isEqualTo("orderDetails");
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailToReadObjectInNonArray(){
    assertThrows(SpinJsonDataFormatException.class, this::failingWithException);
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldBeSameAsJavaValue() {

    SpinJsonNode node = Spin.JSON(EXAMPLE_JSON);
    SpinJsonNode childNode = node.prop("orderDetails");

    SpinJsonNode property1 = node.prop("order");
    SpinJsonNode property2 = childNode.prop("price");
    SpinJsonNode property3 = node.prop("active");

    String javaVariable1 = property1.stringValue();
    Number javaVariable2 = property2.numberValue();
    Boolean javaVariable3 = property3.boolValue();

    String scriptVariable1 = script.getVariable("stringValue");
    Number scriptVariable2 = script.getVariable("numberValue");
    Boolean scriptVariable3 = script.getVariable("boolValue");

    assertThat(javaVariable1).isEqualTo(scriptVariable1);
    assertThat(javaVariable2).isEqualTo(scriptVariable2);
    assertThat(javaVariable3).isEqualTo(scriptVariable3);
  }

}

