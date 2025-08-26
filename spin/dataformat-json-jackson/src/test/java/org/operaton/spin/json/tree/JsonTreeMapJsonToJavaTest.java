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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Test;

import org.operaton.spin.json.SpinJsonDataFormatException;
import org.operaton.spin.json.SpinJsonException;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.mapping.Order;
import org.operaton.spin.json.mapping.RegularCustomer;

import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.json.JsonTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTreeMapJsonToJavaTest {

  @Test
  void shouldMapJsonObjectToJavaObject() {
    Order order = JSON(EXAMPLE_JSON).mapTo(Order.class);
    assertIsExampleOrder(order);
  }

  @Test
  void shouldFailMappingToMismatchingClass() {
    SpinJsonNode jsonNode = JSON(EXAMPLE_JSON);
    assertThatThrownBy(() -> jsonNode.mapTo(RegularCustomer.class))
        .isInstanceOf(SpinJsonException.class)
        .hasMessageMatching("SPIN/JACKSON-JSON-01006 Cannot deserialize .* to java type .*");
  }

  @Test
  void shouldMapByCanonicalString() {
    Order order = JSON(EXAMPLE_JSON).mapTo(Order.class.getCanonicalName());
    assertIsExampleOrder(order);
  }

  @Test
  void shouldMapListByCanonicalString() {
    JavaType desiredType =
        TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Order.class);

    List<Order> orders = JSON(EXAMPLE_JSON_COLLECTION).mapTo(desiredType.toCanonical());

    assertThat(orders).hasSize(1);
    assertIsExampleOrder(orders.get(0));
  }

  @Test
  void shouldFailForMalformedTypeString() {
    SpinJsonNode jsonNode = JSON(EXAMPLE_JSON_COLLECTION);
    assertThrows(SpinJsonDataFormatException.class, () -> jsonNode.mapTo("rubbish"));
  }

}
