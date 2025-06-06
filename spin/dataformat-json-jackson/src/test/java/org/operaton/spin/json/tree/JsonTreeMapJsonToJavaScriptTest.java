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
import org.operaton.spin.json.SpinJsonDataFormatException;
import org.operaton.spin.json.SpinJsonException;
import org.operaton.spin.json.mapping.Order;
import org.operaton.spin.json.mapping.RegularCustomer;
import static org.operaton.spin.json.JsonTestConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class JsonTreeMapJsonToJavaScriptTest extends ScriptTest {

  @Test
  @Script(
    name = "JsonTreeMapJsonToJavaScriptTest.mapToType",
    execute = false
  )
  @ScriptVariable(name = "input", file=EXAMPLE_JSON_FILE_NAME)
  public void shouldMapJsonObjectToJavaObject() throws Throwable {
    Map<String, Object> variables = newMap("mapToType", Order.class);
    Order order = script.execute(variables).getVariable("result");
    assertIsExampleOrder(order);
  }

  @Test
  @Script(
    name = "JsonTreeMapJsonToJavaScriptTest.mapToType",
    execute = false
  )
  @ScriptVariable(name = "input", file=EXAMPLE_JSON_FILE_NAME)
  public void shouldFailMappingToMismatchingClass() {
    Map<String, Object> variables = newMap("mapToType", RegularCustomer.class);
    assertThrows(SpinJsonException.class, () ->
      failingWithException(variables));
  }

  @Test
  @Script(
    name = "JsonTreeMapJsonToJavaScriptTest.mapToType",
    execute = false
  )
  @ScriptVariable(name = "input", file=EXAMPLE_JSON_FILE_NAME)
  public void shouldMapByCanonicalString() throws Throwable {
    Map<String, Object> variables = newMap("mapToType", Order.class.getCanonicalName());
    Order order = script.execute(variables).getVariable("result");
    assertIsExampleOrder(order);
  }

  @Test
  @Script(
    name = "JsonTreeMapJsonToJavaScriptTest.mapToCollection",
    execute = false
  )
  public void shouldMapListByCanonicalString() throws Throwable {
    Map<String, Object> variables = new HashMap<>();
    variables.put("input", EXAMPLE_JSON_COLLECTION);
    variables.put("collectionType", ArrayList.class);
    variables.put("mapToType", Order.class);

    List<Order> orders = script.execute(variables).getVariable("result");

    assertThat(orders).hasSize(1);
    assertIsExampleOrder(orders.get(0));
  }

  @Test
  @Script(
    name = "JsonTreeMapJsonToJavaScriptTest.mapToType",
    variables = {
      @ScriptVariable(name = "input", file=EXAMPLE_JSON_FILE_NAME),
      @ScriptVariable(name = "mapToType", value = "rubbish")
    },
    execute = false
  )
  public void shouldFailForMalformedTypeString() {
    assertThrows(SpinJsonDataFormatException.class, this::failingWithException);
  }

  protected Map<String, Object> newMap(String key, Object value) {
    Map<String, Object> result = new HashMap<>();
    result.put(key, value);

    return result;
  }

}
