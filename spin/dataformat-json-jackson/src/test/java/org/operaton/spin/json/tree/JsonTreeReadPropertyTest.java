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

import org.operaton.spin.SpinList;
import org.operaton.spin.impl.util.SpinIoUtil;
import org.operaton.spin.json.SpinJsonDataFormatException;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPropertyException;
import org.operaton.spin.spi.SpinDataFormatException;
import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Stefan Hentschel
 */
class JsonTreeReadPropertyTest {

  protected SpinJsonNode jsonNode;
  protected SpinJsonNode order;
  protected SpinJsonNode dueUntil;
  protected SpinJsonNode id;
  protected SpinJsonNode customers;
  protected SpinJsonNode orderDetails;
  protected SpinJsonNode active;
  protected SpinJsonNode nullValue;

  @BeforeEach
  void readJson() {
    jsonNode = JSON(EXAMPLE_JSON);
    order = jsonNode.prop("order");
    dueUntil = jsonNode.prop("dueUntil");
    id = jsonNode.prop("id");
    customers = jsonNode.prop("customers");
    orderDetails = jsonNode.prop("orderDetails");
    active = jsonNode.prop("active");
    nullValue = jsonNode.prop("nullValue");
  }

  @Test
  void checkForProperty() {
    assertThat(jsonNode.hasProp("order")).isTrue();
    assertThat(order.hasProp("order")).isFalse();
    assertThat(dueUntil.hasProp("order")).isFalse();
    assertThat(id.hasProp("order")).isFalse();
    assertThat(customers.hasProp("order")).isFalse();
    assertThat(orderDetails.hasProp("price")).isTrue();
    assertThat(active.hasProp("order")).isFalse();
    assertThat(jsonNode.hasProp("nullValue")).isTrue();
  }

  @Test
  void shouldReadProperty() {
    assertThat(jsonNode).isNotNull();

    SpinJsonNode property = jsonNode.prop("order");
    assertThat(property).isNotNull();
  }

  @Test
  void shouldFailToReadNonProperty() {
    assertThrows(SpinJsonPropertyException.class, () -> jsonNode.prop("nonExisting"));
    assertThrows(SpinJsonPropertyException.class, () -> order.prop("nonExisting"));
    assertThrows(SpinJsonPropertyException.class, () -> customers.prop("nonExisting"));
    assertThrows(IllegalArgumentException.class, () -> jsonNode.prop(null));
    assertThrows(SpinJsonPropertyException.class, () -> order.prop("null"));
    assertThrows(SpinJsonPropertyException.class, () -> customers.prop("null"));
  }

  @Test
  void checkForObjectValue() {
    assertThat(jsonNode.isObject()).isTrue();
    assertThat(order.isObject()).isFalse();
    assertThat(dueUntil.isObject()).isFalse();
    assertThat(id.isObject()).isFalse();
    assertThat(customers.isObject()).isFalse();
    assertThat(orderDetails.isObject()).isTrue();
    assertThat(active.isObject()).isFalse();
  }

  @Test
  void shouldReadObjectProperty() {
    assertThat(jsonNode.prop("order")).isNotNull();
    assertThat(orderDetails.prop("article")).isNotNull();
  }

  @Test
  void shouldFailToReadNonObject() {
    SpinJsonNode orderNode = jsonNode.prop("order");
    SpinJsonNode roundedPriceNode = orderDetails.prop("roundedPrice");

    assertThrows(SpinJsonPropertyException.class, () -> orderNode.prop("nonExisting"));
    assertThrows(SpinJsonPropertyException.class, () -> roundedPriceNode.prop("nonExisting"));
  }

  @Test
  void checkForStringValue() {
    assertThat(jsonNode.isString()).isFalse();
    assertThat(order.isString()).isTrue();
    assertThat(dueUntil.isString()).isFalse();
    assertThat(id.isString()).isFalse();
    assertThat(customers.isString()).isFalse();
    assertThat(orderDetails.isString()).isFalse();
    assertThat(active.isString()).isFalse();
  }

  @Test
  void shouldReadStringValue() {
    assertThat(order.stringValue()).isEqualTo("order1");
    assertThat(orderDetails.prop("article").stringValue()).isEqualTo("operatonBPM");
    assertThat(customers.elements().get(0).prop("name").stringValue()).isEqualTo("Kermit");
  }

  @Test
  void shouldFailToReadNonStringValue() {
    SpinJsonNode currenciesNode = orderDetails.prop("currencies");
    assertThrows(SpinJsonDataFormatException.class, () -> jsonNode.stringValue());
    assertThrows(SpinJsonDataFormatException.class, () -> dueUntil.stringValue());
    assertThrows(SpinDataFormatException.class, currenciesNode::stringValue);
  }

  @Test
  void checkForNumberValue() {
    assertThat(jsonNode.isNumber()).isFalse();
    assertThat(order.isNumber()).isFalse();
    assertThat(dueUntil.isNumber()).isTrue();
    assertThat(id.isNumber()).isTrue();
    assertThat(customers.isNumber()).isFalse();
    assertThat(orderDetails.isNumber()).isFalse();
    assertThat(active.isNumber()).isFalse();
  }

  @Test
  void shouldReadNumberValue() {
    assertThat(dueUntil.numberValue()).isEqualTo(20150112);
    assertThat(id.numberValue()).isEqualTo(1234567890987654321L);
  }

  @Test
  void shouldFailToReadNonNumberValue() {
    assertThrows(SpinJsonDataFormatException.class, () -> jsonNode.numberValue());
    assertThrows(SpinJsonDataFormatException.class, () -> order.numberValue());
    assertThrows(SpinDataFormatException.class, () -> customers.numberValue());
  }

  @Test
  void checkForBooleanValue() {
    assertThat(jsonNode.isBoolean()).isFalse();
    assertThat(order.isBoolean()).isFalse();
    assertThat(dueUntil.isBoolean()).isFalse();
    assertThat(id.isBoolean()).isFalse();
    assertThat(customers.isBoolean()).isFalse();
    assertThat(orderDetails.isBoolean()).isFalse();
    assertThat(active.isBoolean()).isTrue();
  }

  @Test
  void shouldReadBooleanValue() {
    assertThat(active.boolValue()).isTrue();
    assertThat(orderDetails.prop("paid").boolValue()).isFalse();
  }

  @Test
  void shouldFailToReadNonBooleanValue() {
    assertThrows(SpinJsonDataFormatException.class, () -> jsonNode.boolValue());
    assertThrows(SpinJsonDataFormatException.class, () -> order.boolValue());
    assertThrows(SpinDataFormatException.class, () -> customers.boolValue());
  }

  @Test
  void checkForValue() {
    assertThat(jsonNode.isValue()).isFalse();
    assertThat(order.isValue()).isTrue();
    assertThat(dueUntil.isValue()).isTrue();
    assertThat(id.isValue()).isTrue();
    assertThat(customers.isValue()).isFalse();
    assertThat(orderDetails.isValue()).isFalse();
    assertThat(active.isValue()).isTrue();
  }

  @Test
  void shouldReadValue() {
    assertThat(order.value())
        .isInstanceOf(String.class)
        .isEqualTo("order1");

    assertThat(dueUntil.value())
        .isInstanceOf(Number.class)
        .isEqualTo(20150112);

    assertThat(active.value())
        .isInstanceOf(Boolean.class)
        .isEqualTo(true);
  }

  @Test
  void shouldFailToReadNonValue() {
    assertThrows(SpinJsonDataFormatException.class, () -> jsonNode.value());
    assertThrows(SpinJsonDataFormatException.class, () -> customers.value());
    assertThrows(SpinDataFormatException.class, () -> orderDetails.value());
  }

  @Test
  void checkForArrayValue() {
    assertThat(jsonNode.isArray()).isFalse();
    assertThat(order.isArray()).isFalse();
    assertThat(dueUntil.isArray()).isFalse();
    assertThat(id.isArray()).isFalse();
    assertThat(customers.isArray()).isTrue();
    assertThat(orderDetails.isArray()).isFalse();
    assertThat(active.isArray()).isFalse();
  }

  @Test
  void shouldReadArrayValue() {
    SpinList<SpinJsonNode> customerElements = customers.elements();
    SpinList<SpinJsonNode> currenciesElements = orderDetails.prop("currencies").elements();

    assertThat(customerElements).hasSize(3);
    assertThat(currenciesElements).hasSize(2);

    assertThat(customerElements.get(0).prop("name").stringValue()).isEqualTo("Kermit");
    assertThat(currenciesElements.get(0).stringValue()).isEqualTo("euro");
  }

  @Test
  void shouldFailToReadNonArrayValue() {
    assertThrows(SpinJsonDataFormatException.class, () -> jsonNode.elements());
    assertThrows(SpinJsonDataFormatException.class, () -> order.elements());
    assertThrows(SpinDataFormatException.class, () -> id.elements());
  }

  @Test
  void checkForNullValue() {
    assertThat(nullValue.isNull()).isTrue();
    assertThat(jsonNode.isNull()).isFalse();
    assertThat(order.isNull()).isFalse();
    assertThat(dueUntil.isNull()).isFalse();
    assertThat(id.isNull()).isFalse();
    assertThat(customers.isNull()).isFalse();
    assertThat(orderDetails.isNull()).isFalse();
    assertThat(active.isNull()).isFalse();
  }

  @Test
  void shouldReadNullValue() {
    assertThat(nullValue.isValue()).isTrue();
    assertThat(nullValue.value()).isNull();
  }

  @Test
  void shouldReadFieldNames() {
    assertThat(jsonNode.fieldNames()).contains("order", "dueUntil", "id", "customers", "orderDetails", "active");
    assertThat(customers.fieldNames()).isEmpty();
    assertThat(orderDetails.fieldNames()).contains("article", "price", "roundedPrice", "currencies", "paid");
  }

  @Test
  void shouldFailToReadNonFieldNames() {
    assertThrows(SpinJsonDataFormatException.class, () -> order.fieldNames());
    assertThrows(SpinJsonDataFormatException.class, () -> dueUntil.fieldNames());
    assertThrows(SpinDataFormatException.class, () -> active.fieldNames());
  }


  /**
   * Tests an issue with Jackson 2.4.1
   * <p>
   * The test contains a negative float at character position 8000 which is important
   * to provoke Jackson bug #146.
   * See also <a href="https://github.com/FasterXML/jackson-core/issues/146">the Jackson bug report</a>.
   * </p>
   */
  @Test
  void shouldNotFailWithJackson146Bug() {
    // this should not fail
    SpinJsonNode node = JSON(SpinIoUtil.fileAsString("org/operaton/spin/json/jackson146.json"));

    // file has 4000 characters in length a
    // 20 characters per repeated JSON object
    assertThat(node.prop("abcdef").elements()).hasSize(200);
  }
}
