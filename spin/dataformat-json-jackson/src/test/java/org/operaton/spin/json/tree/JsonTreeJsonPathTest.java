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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.spin.SpinList;
import org.operaton.spin.json.SpinJsonDataFormatException;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPathException;
import org.operaton.spin.json.SpinJsonPathQuery;

import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Author: Stefan Hentschel
 */
class JsonTreeJsonPathTest {

  protected SpinJsonNode jsonNode;

  @BeforeEach
  void readJson() {
    jsonNode = JSON(EXAMPLE_JSON);
  }

  @Test
  void shouldGetElementFromJsonPath() {
    SpinJsonNode node = jsonNode.jsonPath("$.orderDetails").element();

    assertThat(node.isObject()).isTrue();
    assertThat(node.prop("article").isString()).isTrue();
  }

  @Test
  void shouldGetElementListFromJsonPath() {
    SpinList<SpinJsonNode> node = jsonNode.jsonPath("$.customers").elementList();

    assertThat(node).hasSize(3);
    assertThat(node.get(0).isObject()).isTrue();
    assertThat(node.get(0).prop("name").isString()).isTrue();
  }

  @Test
  void shouldGetBooleanFromJsonPath() {
    Boolean active = jsonNode.jsonPath("$.active").boolValue();

    assertThat(active).isTrue();
  }

  @Test
  void shouldGetStringFromJsonPath() {
    String order = jsonNode.jsonPath("$.order").stringValue();

    assertThat(order).isEqualTo("order1");
  }

  @Test
  void shouldGetNumberFromJsonPath() {
    Number order = jsonNode.jsonPath("$.id").numberValue();

    assertThat(order.longValue()).isEqualTo(1234567890987654321L);
  }

  @Test
  void shouldGetNullNode() {
    SpinJsonNode node = jsonNode.jsonPath("$.nullValue").element();
    assertThat(node.isNull()).isTrue();
  }

  @Test
  void shouldGetSingleArrayEntry() {
    SpinJsonNode node = jsonNode.jsonPath("$.customers[0]").element();

    assertThat(node.isObject()).isTrue();
    assertThat(node.prop("name").isString()).isTrue();
    assertThat(node.prop("name").stringValue()).isEqualTo("Kermit");
  }

  @Test
  void shouldGetMultipleArrayEntries() {
    SpinList<SpinJsonNode> nodeList = jsonNode.jsonPath("$.customers[0:2]").elementList();

    assertThat(nodeList).hasSize(2);
    assertThat(nodeList.get(0).prop("name").stringValue()).isEqualTo("Kermit");
    assertThat(nodeList.get(1).prop("name").stringValue()).isEqualTo("Waldo");
  }

  @Test
  void shouldGetFilteredResult() {
    SpinList<SpinJsonNode> nodeList = jsonNode.jsonPath("$.customers[?(@.name == 'Klo')]").elementList();

    assertThat(nodeList).isEmpty();

    nodeList = jsonNode.jsonPath("$.customers[?(@.name == 'Waldo')]").elementList();

    assertThat(nodeList).hasSize(1);
    assertThat(nodeList.get(0).prop("name").stringValue()).isEqualTo("Waldo");
  }

  @Test
  void shouldGetMultipleArrayPropertyValues() {
    SpinList<SpinJsonNode> nodeList = jsonNode.jsonPath("$.customers[*].name").elementList();

    assertThat(nodeList).hasSize(3);
    assertThat(nodeList.get(0).stringValue()).isEqualTo("Kermit");
    assertThat(nodeList.get(1).stringValue()).isEqualTo("Waldo");
    assertThat(nodeList.get(2).stringValue()).isEqualTo("Johnny");
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.....", ""})
  void failReadingJsonPath(String path) {
    assertThrows(SpinJsonPathException.class, () -> jsonNode.jsonPath(path));
  }

  @Test
  void failAccessNonExistentProperty() {
    SpinJsonPathQuery pathQuery = jsonNode.jsonPath("$.order.test");
    assertThrows(SpinJsonPathException.class, pathQuery::element);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.order" , "$.id", "$.active", "$" })
  void failReadingElementList(String path) {
    var pathQuery = jsonNode.jsonPath(path);
    assertThrows(SpinJsonDataFormatException.class, pathQuery::elementList);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.customers", "$.active", "$.id", "$" })
  void failReadingStringProperty(String path) {
    var pathQuery = jsonNode.jsonPath(path);
    assertThrows(SpinJsonDataFormatException.class, pathQuery::stringValue);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.customers", "$.active", "$.order", "$" })
  void failReadingNumberProperty(String path) {
    var pathQuery = jsonNode.jsonPath(path);
    assertThrows(SpinJsonDataFormatException.class, pathQuery::numberValue);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.customers", "$.id", "$.order", "$" })
  void failReadingBooleanProperty(String path) {
    var pathQuery = jsonNode.jsonPath(path);
    assertThrows(SpinJsonDataFormatException.class, pathQuery::boolValue);
  }

  @Test
  void failOnNonExistingJsonPath() {
    SpinJsonNode json = JSON("{\"a\": {\"id\": \"a\"}, \"b\": {\"id\": \"b\"}}");
    SpinJsonPathQuery pathQuery = json.jsonPath("$.c?(@.id)");
    assertThrows(SpinJsonPathException.class, pathQuery::element);
  }
}
