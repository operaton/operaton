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

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPropertyException;

import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Stefan Hentschel
 */
class JsonTreeSetPropertyTest {

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
  void setStringProperty() {
    String newComment = "42!";

    jsonNode.prop("comment", newComment);
    SpinJsonNode comment = jsonNode.prop("comment");

    assertThat(comment.isString()).isTrue();
    assertThat(comment.stringValue()).isEqualTo(newComment);
  }

  @Test
  void replaceStringProperty() {
    String value = "new Order";
    String oldValue = order.stringValue();

    assertThat(order.isString()).isTrue();
    assertThat(customers.isArray()).isTrue();

    // set new values
    jsonNode.prop("order", value);
    jsonNode.prop("customers", value);
    SpinJsonNode newValue1 = jsonNode.prop("order");
    SpinJsonNode newValue2 = jsonNode.prop("customers");

    assertThat(newValue1.stringValue()).isNotEqualTo(oldValue);
    assertThat(newValue1.stringValue()).isEqualTo(value);

    assertThat(newValue2.isArray()).isFalse();
    assertThat(newValue2.stringValue()).isEqualTo(value);
  }

  @Test
  void setIntegerProperty() {
    Integer newComment = 42;
    jsonNode.prop("comment", newComment);
    SpinJsonNode comment = jsonNode.prop("comment");

    assertThat(comment.isNumber()).isTrue();
    assertThat(comment.numberValue()).isEqualTo(newComment);
  }

  @Test
  void replaceIntegerProperty() {
    Integer value = 42;
    Integer oldValue = dueUntil.numberValue().intValue();

    assertThat(customers.isArray()).isTrue();
    assertThat(dueUntil.isNumber()).isTrue();

    // set new values
    jsonNode.prop("dueUntil", value);
    jsonNode.prop("customers", value);
    SpinJsonNode newValue1 = jsonNode.prop("dueUntil");
    SpinJsonNode newValue2 = jsonNode.prop("customers");

    assertThat(newValue1.numberValue()).isNotEqualTo(oldValue);
    assertThat(newValue1.numberValue()).isEqualTo(value);

    assertThat(newValue2.isArray()).isFalse();
    assertThat(newValue2.isNumber()).isTrue();
    assertThat(newValue2.numberValue()).isEqualTo(value);
  }

  @Test
  void setFloatProperty() {
    Float floatValue = 42.00F;
    jsonNode.prop("comment", floatValue);
    SpinJsonNode comment = jsonNode.prop("comment");

    assertThat(comment.isNumber()).isTrue();
    assertThat(comment.numberValue()).isEqualTo(floatValue);
  }

  @Test
  void replaceFloatProperty() {
    SpinJsonNode price = orderDetails.prop("price");
    Float value = 42.00F;

    Float oldValue = price.numberValue().floatValue();

    assertThat(customers.isArray()).isTrue();
    assertThat(price.isNumber()).isTrue();

    // set new values
    orderDetails.prop("price", value);
    jsonNode.prop("customers", value);
    SpinJsonNode newValue1 = orderDetails.prop("price");
    SpinJsonNode newValue2 = jsonNode.prop("customers");

    assertThat(newValue1.numberValue()).isNotEqualTo(oldValue);
    assertThat(newValue1.numberValue()).isEqualTo(value);

    assertThat(newValue2.isArray()).isFalse();
    assertThat(newValue2.isNumber()).isTrue();
    assertThat(newValue2.numberValue()).isEqualTo(value);
  }

  @Test
  void setLongProperty() {
    Long longValue = 4200000000L;
    jsonNode.prop("comment", longValue);
    SpinJsonNode comment = jsonNode.prop("comment");

    assertThat(comment.isNumber()).isTrue();
    assertThat(comment.numberValue().longValue()).isEqualTo(longValue);
  }

  @Test
  void replaceLongProperty() {
    Long value = 4200000000L;

    Long oldValue = id.numberValue().longValue();

    assertThat(customers.isArray()).isTrue();
    assertThat(id.isNumber()).isTrue();

    // set new values
    jsonNode.prop("id", value);
    jsonNode.prop("customers", value);
    SpinJsonNode newValue1 = jsonNode.prop("id");
    SpinJsonNode newValue2 = jsonNode.prop("customers");

    assertThat(newValue1.numberValue()).isNotEqualTo(oldValue);
    assertThat(newValue1.numberValue()).isEqualTo(value);

    assertThat(newValue2.isArray()).isFalse();
    assertThat(newValue2.isNumber()).isTrue();
    assertThat(newValue2.numberValue()).isEqualTo(value);
  }

  @Test
  void setBooleanProperty() {
    jsonNode.prop("comment", true);
    SpinJsonNode comment = jsonNode.prop("comment");

    assertThat(comment.isBoolean()).isTrue();
    assertThat(comment.boolValue()).isTrue();
  }

  @Test
  void replaceBooleanProperty() {
    active = jsonNode.prop("active");

    Boolean oldValue = active.boolValue();
    Boolean value = Boolean.FALSE.equals(oldValue);

    assertThat(customers.isArray()).isTrue();
    assertThat(active.isBoolean()).isTrue();

    // set new values
    jsonNode.prop("active", value);
    jsonNode.prop("customers", value);
    SpinJsonNode newValue1 = jsonNode.prop("active");
    SpinJsonNode newValue2 = jsonNode.prop("customers");

    assertThat(newValue1.boolValue()).isNotEqualTo(oldValue);
    assertThat(newValue1.boolValue()).isEqualTo(value);

    assertThat(newValue2.isArray()).isFalse();
    assertThat(newValue2.isBoolean()).isTrue();
    assertThat(newValue2.boolValue()).isEqualTo(value);
  }

  @Test
  void setArrayProperty() {
    ArrayList<Object> list1 = new ArrayList<>();
    ArrayList<Object> list2 = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();

    map.put("id1", "object1");
    map.put("id2", 1337);

    list2.add("n");
    list2.add(32);

    list1.add("test");
    list1.add(42);
    list1.add(list2);
    list1.add(map);

    jsonNode.prop("comment", list1);
    SpinJsonNode comment = jsonNode.prop("comment");

    assertThat(comment.isArray()).isTrue();
    assertThat(comment.elements()).hasSize(4);
    assertThat(comment.elements().get(0).stringValue()).isEqualTo("test");
    assertThat(comment.elements().get(2).elements().get(1).numberValue()).isEqualTo(32);
    assertThat(comment.elements().get(3).isObject()).isTrue();
    assertThat(comment.elements().get(3).hasProp("id2")).isTrue();
  }

  @Test
  void replaceArrayProperty() {

    assertThat(customers.isArray()).isTrue();
    assertThat(customers.elements()).hasSize(3);
    assertThat(active.isBoolean()).isTrue();
    assertThat(active.boolValue()).isTrue();

    // Build new values
    ArrayList<Object> list1 = new ArrayList<>();
    ArrayList<Object> list2 = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();

    map.put("id1", "object1");
    map.put("id2", 1337);

    list2.add("n");
    list2.add(32);

    list1.add("test");
    list1.add(42);
    list1.add("filler");
    list1.add(list2);
    list1.add(map);

    jsonNode.prop("customers", list1);
    jsonNode.prop("active", list1);
    customers = jsonNode.prop("customers");
    active = jsonNode.prop("active");

    assertThat(customers.isArray()).isTrue();
    assertThat(customers.elements()).hasSize(5);
    assertThat(customers.elements().get(0).stringValue()).isEqualTo("test");
    assertThat(customers.elements().get(3).elements().get(1).numberValue()).isEqualTo(32);
    assertThat(customers.elements().get(4).isObject()).isTrue();
    assertThat(customers.elements().get(4).hasProp("id2")).isTrue();

    assertThat(active.isArray()).isTrue();
    assertThat(active.elements()).hasSize(5);
    assertThat(active.elements().get(0).stringValue()).isEqualTo("test");
    assertThat(active.elements().get(3).elements().get(1).numberValue()).isEqualTo(32);
    assertThat(active.elements().get(4).isObject()).isTrue();
    assertThat(active.elements().get(4).hasProp("id2")).isTrue();
  }

  @Test
  void setObjectProperty() {
    Map<String, Object> map = new HashMap<>();
    map.put("id1", "object1");
    map.put("id2", 1337);

    jsonNode.prop("comment", map);
    SpinJsonNode comment = jsonNode.prop("comment");

    assertThat(comment.isObject()).isTrue();
    assertThat(comment.hasProp("id1")).isTrue();
    assertThat(comment.prop("id2").isNumber()).isTrue();
    assertThat(comment.prop("id2").numberValue()).isEqualTo(1337);
  }

  @Test
  void replaceObjectProperty() {
    SpinJsonNode childNode = orderDetails;
    String oldValue = childNode.prop("article").stringValue();

    assertThat(oldValue).isEqualTo("operatonBPM");

    Map<String, Object> map = new HashMap<>();
    map.put("id1", "object1");
    map.put("id2", 1337);

    jsonNode.prop("orderDetails", map);
    SpinJsonNode comment = jsonNode.prop("orderDetails");

    assertThat(comment.isObject()).isTrue();
    assertThat(comment.hasProp("id1")).isTrue();
    assertThat(comment.prop("id2").isNumber()).isTrue();
    assertThat(comment.prop("id2").numberValue()).isEqualTo(1337);
  }

  @Test
  void setPropertyWithJSON() {
    String json = "{\"agent\":\"Smith\"}";

    jsonNode.prop("name", JSON(json));
    SpinJsonNode name = jsonNode.prop("name");
    assertThat(name.isObject()).isTrue();
    assertThat(name.prop("agent").stringValue()).isEqualTo("Smith");
  }

  @Test
  void replacePropertyWithJSON() {
    String json = "{\"agent\":\"Smith\"}";

    assertThat(active.isBoolean()).isTrue();

    jsonNode.prop("active", JSON(json));
    active = jsonNode.prop("active");
    assertThat(active.isBoolean()).isFalse();
    assertThat(active.isObject()).isTrue();
    assertThat(active.prop("agent").stringValue()).isEqualTo("Smith");
  }

  @Test
  void failWhileSettingObjectWithMap() {
    Date date = new Date();
    Map<String, Object> map = new HashMap<>();
    map.put("date", date);

    assertThatExceptionOfType(SpinJsonPropertyException.class).isThrownBy(() -> jsonNode.prop("test", map));
  }

  @Test
  void failWhileSettingObjectWithList() {
    Date date = new Date();
    ArrayList<Object> list = new ArrayList<>();
    list.add(date);

    assertThatExceptionOfType(SpinJsonPropertyException.class).isThrownBy(() -> jsonNode.prop("test", list));
  }

  @Test
  void replaceNullProperty() {
    jsonNode.prop("order", (String) null);
    jsonNode.prop("id", (String) null);
    jsonNode.prop("nullValue", (String) null);

    assertThat(jsonNode.prop("order").isNull()).isTrue();
    assertThat(jsonNode.prop("order").value()).isNull();

    assertThat(jsonNode.prop("id").isNull()).isTrue();
    assertThat(jsonNode.prop("id").value()).isNull();

    assertThat(jsonNode.prop("nullValue").isNull()).isTrue();
    assertThat(jsonNode.prop("nullValue").value()).isNull();
  }

  @Test
  void setNullStringProperty() {
    jsonNode.prop("newNullValue", (String) null);

    assertThat(jsonNode.prop("newNullValue").isNull()).isTrue();
    assertThat(jsonNode.prop("newNullValue").value()).isNull();
  }

  @Test
  void setNullMapProperty() {
    jsonNode.prop("newNullValue", (Map) null);

    assertThat(jsonNode.prop("newNullValue").isNull()).isTrue();
    assertThat(jsonNode.prop("newNullValue").value()).isNull();
  }

  @Test
  void setNullListProperty() {
    jsonNode.prop("newNullValue", (List) null);

    assertThat(jsonNode.prop("newNullValue").isNull()).isTrue();
    assertThat(jsonNode.prop("newNullValue").value()).isNull();
  }

  @Test
  void setNullBooleanProperty() {
    jsonNode.prop("newNullValue", (Boolean) null);

    assertThat(jsonNode.prop("newNullValue").isNull()).isTrue();
    assertThat(jsonNode.prop("newNullValue").value()).isNull();
  }

  @Test
  void setNullNumberProperty() {
    jsonNode.prop("newNullValue", (Number) null);

    assertThat(jsonNode.prop("newNullValue").isNull()).isTrue();
    assertThat(jsonNode.prop("newNullValue").value()).isNull();
  }

  @Test
  void setNullSpinJsonNodeProperty() {
    jsonNode.prop("newNullValue", (SpinJsonNode) null);

    assertThat(jsonNode.prop("newNullValue").isNull()).isTrue();
    assertThat(jsonNode.prop("newNullValue").value()).isNull();
  }
}
