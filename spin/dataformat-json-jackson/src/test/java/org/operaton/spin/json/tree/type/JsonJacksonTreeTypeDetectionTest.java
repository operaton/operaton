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
package org.operaton.spin.json.tree.type;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.spin.DataFormats;
import org.operaton.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.operaton.spin.impl.json.jackson.format.MapJacksonJsonTypeDetector;
import org.operaton.spin.impl.json.jackson.format.SetJacksonJsonTypeDetector;
import org.operaton.spin.json.mapping.Customer;
import org.operaton.spin.json.mapping.RegularCustomer;
import org.operaton.spin.spi.DataFormatMapper;

import static org.operaton.spin.DataFormats.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonJacksonTreeTypeDetectionTest {
  private static final SetJacksonJsonTypeDetector SET_JACKSON_JSON_TYPE_DETECTOR = new SetJacksonJsonTypeDetector();
  private static final MapJacksonJsonTypeDetector MAP_JACKSON_JSON_TYPE_DETECTOR = new MapJacksonJsonTypeDetector();

  public JacksonJsonDataFormat dataFormatWithSetTypeDetector =
      new JacksonJsonDataFormat(DataFormats.JSON_DATAFORMAT_NAME);

  public JacksonJsonDataFormat dataFormatWithMapTypeDetector =
      new JacksonJsonDataFormat(DataFormats.JSON_DATAFORMAT_NAME);

  @BeforeEach
  void configure() {
    dataFormatWithSetTypeDetector.addTypeDetector(SET_JACKSON_JSON_TYPE_DETECTOR);
    dataFormatWithMapTypeDetector.addTypeDetector(MAP_JACKSON_JSON_TYPE_DETECTOR);
  }

  @Test
  void shouldDetectTypeFromObject() {
    // given
    RegularCustomer customer = new RegularCustomer();

    // when
    String canonicalTypeString = json().getMapper().getCanonicalTypeName(customer);

    // then
    assertThat(canonicalTypeString).isEqualTo("org.operaton.spin.json.mapping.RegularCustomer");
  }

  @Test
  void shouldDetectListType() {
    // given
    List<Customer> customers = new ArrayList<>();
    customers.add(new RegularCustomer());

    // when
    String canonicalTypeString = json().getMapper().getCanonicalTypeName(customers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.ArrayList<org.operaton.spin.json.mapping.RegularCustomer>");
  }

  @Test
  void shouldDetectListTypeFromEmptyList() {
    // given
    List<RegularCustomer> customers = new ArrayList<>();

    // when
    String canonicalTypeString = json().getMapper().getCanonicalTypeName(customers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.ArrayList<java.lang.Object>");
  }

  @Test
  void shouldDetectSetType() {
    // given
    Set<Customer> customers = new HashSet<>();
    customers.add(new RegularCustomer());

    // when
    String canonicalTypeString = dataFormatWithSetTypeDetector.getCanonicalTypeName(customers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashSet<org.operaton.spin.json.mapping.RegularCustomer>");
  }

  @Test
  void shouldDetectSetTypeFromEmptySet() {
    // given
    Set<RegularCustomer> customers = new HashSet<>();

    // when
    String canonicalTypeString = dataFormatWithSetTypeDetector.getCanonicalTypeName(customers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashSet<java.lang.Object>");
  }

  @Test
  void shouldDetectMapType() {
    // given
    Map<String, Customer> customers = new HashMap<>();
    customers.put("foo", new RegularCustomer());

    // when
    String canonicalTypeString = dataFormatWithMapTypeDetector.getCanonicalTypeName(customers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.Map<java.lang.String,org.operaton.spin.json.mapping.RegularCustomer>");
  }

  @Test
  void shouldDetectMapTypeFromEmptyMap() {
    // given
    Map<Integer, RegularCustomer> customers = new HashMap<>();

    // when
    String canonicalTypeString = dataFormatWithMapTypeDetector.getCanonicalTypeName(customers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashMap<java.lang.Object,java.lang.Object>");
  }

  @Test
  void shouldHandleNullParameter() {
    DataFormatMapper mapper = json().getMapper();
    assertThatThrownBy(() -> mapper.getCanonicalTypeName(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldHandleListOfLists() {
    // given
    List<List<RegularCustomer>> nestedCustomers = new ArrayList<>();
    List<RegularCustomer> customers = new ArrayList<>();
    customers.add(new RegularCustomer());
    nestedCustomers.add(customers);

    // when
    String canonicalTypeString = json().getMapper().getCanonicalTypeName(nestedCustomers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.ArrayList<java.util.ArrayList<org.operaton.spin.json.mapping.RegularCustomer>>");
  }

  @Test
  void shouldHandleSetOfSets() {
    // given
    Set<Set<RegularCustomer>> nestedCustomers = new HashSet<>();
    Set<RegularCustomer> customers = new HashSet<>();
    customers.add(new RegularCustomer());
    nestedCustomers.add(customers);

    // when
    String canonicalTypeString = dataFormatWithSetTypeDetector.getCanonicalTypeName(nestedCustomers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashSet<java.util.HashSet<org.operaton.spin.json.mapping.RegularCustomer>>");
  }

  @Test
  void shouldHandleSetOfLists() {
    // given
    Set<List<RegularCustomer>> nestedCustomers = new HashSet<>();
    List<RegularCustomer> customers = new ArrayList<>();
    customers.add(new RegularCustomer());
    nestedCustomers.add(customers);

    // when
    String canonicalTypeString = dataFormatWithSetTypeDetector.getCanonicalTypeName(nestedCustomers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashSet<java.util.ArrayList<org.operaton.spin.json.mapping.RegularCustomer>>");
  }

  @Test
  void shouldHandleMapOfMaps() {
    // given
    Map<String, Map<Integer, RegularCustomer>> nestedCustomers = new HashMap<>();
    Map<Integer, RegularCustomer> customers = new HashMap<>();
    customers.put(42, new RegularCustomer());
    nestedCustomers.put("foo", customers);

    // when
    String canonicalTypeString = dataFormatWithMapTypeDetector.getCanonicalTypeName(nestedCustomers);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.Map<java.lang.String,java.util.Map<java.lang.Integer,org.operaton.spin.json.mapping.RegularCustomer>>");
  }

  @Test
  void shouldHandleMapWithNullAndStringValue() {
    // given
    Map<String, Object> map = new HashMap<>();
    map.put("bar", null);
    map.put("foo", "baz");

    // when
    String canonicalTypeString = dataFormatWithMapTypeDetector.getCanonicalTypeName(map);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.Map<java.lang.String,java.lang.String>");
  }

  @Test
  void shouldHandleMapWithNullAndNullValue() {
    // given
    Map<String, Object> map = new HashMap<>();
    map.put("foo", null);
    map.put("bar", null);

    // when
    String canonicalTypeString = dataFormatWithMapTypeDetector.getCanonicalTypeName(map);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.Map<java.lang.String,java.lang.Object>");
  }

  @Test
  void shouldHandleMapWithSingleNullValue() {
    // given
    Map<String, Object> map = new HashMap<>();
    map.put("bar", null);

    // when
    String canonicalTypeString = dataFormatWithMapTypeDetector.getCanonicalTypeName(map);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.Map<java.lang.String,java.lang.Object>");
  }

  @Test
  void shouldHandleSetWithSingleStringValue() {
    // given
    Set<String> set = new HashSet<>();
    set.add("foo");

    // when
    String canonicalTypeString = dataFormatWithSetTypeDetector.getCanonicalTypeName(set);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashSet<java.lang.String>");
  }

  @Test
  void shouldHandleSetWithNullAndStringValue() {
    // given
    Set<Object> set = new HashSet<>();
    set.add(null);
    set.add("foo");

    // when
    String canonicalTypeString = dataFormatWithSetTypeDetector.getCanonicalTypeName(set);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashSet<java.lang.String>");
  }

  @Test
  void shouldHandleSetWithNullValue() {
    // given
    Set<String> set = new HashSet<>();
    set.add(null);

    // when
    String canonicalTypeString = dataFormatWithSetTypeDetector.getCanonicalTypeName(set);

    // then
    assertThat(canonicalTypeString).isEqualTo("java.util.HashSet<java.lang.Object>");
  }

}
