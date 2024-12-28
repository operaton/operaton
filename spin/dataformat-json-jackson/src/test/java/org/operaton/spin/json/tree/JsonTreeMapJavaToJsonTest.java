/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.spin.json.tree;

import org.operaton.spin.SpinList;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.mapping.Order;
import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;
import static org.operaton.spin.json.JsonTestConstants.createExampleOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTreeMapJavaToJsonTest {

  @Test
  void shouldMapJavaObjectToJson() {
    Order exampleOrder = createExampleOrder();

    String json = JSON(exampleOrder).toString();

    assertThatJson(json).isEqualTo(EXAMPLE_JSON);
  }

  @Test
  void shouldMapListToJson() {
    List<String> names = new ArrayList<>();
    names.add("Waldo");
    names.add("Hugo");
    names.add("Kermit");

    String json = JSON(names).toString();

    String expectedJson = "[\"Waldo\", \"Hugo\", \"Kermit\"]";
    assertThatJson(json).isEqualTo(expectedJson);
  }

  @Test
  void shouldMapArrayToJson() {
    String[] names = new String[] { "Waldo", "Hugo", "Kermit" };

    String json = JSON(names).toString();

    String expectedJson = "[\"Waldo\", \"Hugo\", \"Kermit\"]";
    assertThatJson(json).isEqualTo(expectedJson);
  }

  @Test
  void shouldMapMapToJson() {
    Map<String, Object> javaMap = new HashMap<>();
    javaMap.put("aKey", "aValue");
    javaMap.put("anotherKey", 42);

    String json = JSON(javaMap).toString();

    String expectedJson = "{\"aKey\" : \"aValue\", \"anotherKey\" : 42}";
    assertThatJson(json).isEqualTo(expectedJson);
  }

  @Test
  void shouldFailWithNull() {
    assertThrows(IllegalArgumentException.class, () -> JSON(null));
  }

  @Test
  void shouldMapPrimitiveBooleanToJson() {
    SpinJsonNode node = JSON(true);
    assertThat(node.isBoolean()).isTrue();
    assertThat(node.isValue()).isTrue();
    assertThat(node.boolValue()).isTrue();
  }

  @Test
  void shouldMapPrimitiveNumberToJson() {
    SpinJsonNode node = JSON(42);
    assertThat(node.isNumber()).isTrue();
    assertThat(node.isValue()).isTrue();
    assertThat(node.numberValue()).isEqualTo(42);
  }

  @Test
  void shouldMapListOfPrimitiveStrings() {
    List<String> inputList = new ArrayList<>();
    inputList.add("Waldo");
    inputList.add("Hugo");
    inputList.add("Kermit");

    SpinJsonNode node = JSON(inputList);
    assertThat(node.isArray()).isTrue();

    SpinList<SpinJsonNode> elements = node.elements();
    assertThat(elements.get(0).stringValue()).isEqualTo("Waldo");
    assertThat(elements.get(1).stringValue()).isEqualTo("Hugo");
    assertThat(elements.get(2).stringValue()).isEqualTo("Kermit");
  }

}
