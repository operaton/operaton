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
package org.operaton.spin.json;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.spin.json.mapping.CustomerList;
import org.operaton.spin.json.mapping.GenericCustomerList;
import org.operaton.spin.json.mapping.Order;
import org.operaton.spin.json.mapping.RegularCustomer;
import org.operaton.spin.json.mapping.dmn.DmnDecisionResultEntries;
import org.operaton.spin.json.mapping.dmn.DmnDecisionResultEntriesImpl;
import org.operaton.spin.json.mapping.dmn.DmnDecisionResultImpl;
import org.operaton.spin.spi.DataFormatMapper;
import org.operaton.spin.spi.DataFormatReader;
import org.operaton.spin.spi.DataFormatWriter;

import static org.operaton.spin.DataFormats.json;
import static org.assertj.core.api.Assertions.assertThat;

class JsonSerializationTest {

  @Test
  void notGenericList() throws Exception {
    CustomerList<RegularCustomer> customers = new CustomerList<>();
    customers.add(new RegularCustomer("someCustomer", 5));

    String canonicalTypeString = json().getMapper().getCanonicalTypeName(customers);
    assertThat(canonicalTypeString).isNotNull();

    final byte[] bytes = serializeToByteArray(customers);
    assertThat(bytes).isNotEmpty();

    final Object o = deserializeFromByteArray(bytes, canonicalTypeString);
    assertThat(o).isInstanceOf(CustomerList.class);

    CustomerList<?> deserializedCustomerList = (CustomerList<?>) o;
    assertThat(deserializedCustomerList.get(0).getName()).isEqualTo("someCustomer");
    assertThat(deserializedCustomerList.get(0).getContractStartDate()).isEqualTo(5);
  }

  @Test
  void order() throws Exception {
    Order order = JsonTestConstants.createExampleOrder();

    String canonicalTypeString = json().getMapper().getCanonicalTypeName(order);
    assertThat(canonicalTypeString).isNotNull();

    final byte[] bytes = serializeToByteArray(order);
    assertThat(bytes).isNotEmpty();

    final Object o = deserializeFromByteArray(bytes, canonicalTypeString);
    assertThat(o).isInstanceOf(Order.class);

    Order deserializedOrder = (Order) o;
    JsonTestConstants.assertIsExampleOrder(deserializedOrder);
  }

  @Test
  void plainTypeArray() throws Exception {
    int[] array = new int[]{5, 10};

    String canonicalTypeString = json().getMapper().getCanonicalTypeName(array);
    assertThat(canonicalTypeString).isNotNull();

    final byte[] bytes = serializeToByteArray(array);
    assertThat(bytes).isNotEmpty();

    final Object o = deserializeFromByteArray(bytes, canonicalTypeString);
    assertThat(o).isInstanceOf(int[].class);

    int[] deserializedArray = (int[]) o;
    assertThat(deserializedArray[0]).isEqualTo(5);
    assertThat(deserializedArray[1]).isEqualTo(10);
  }

  @Test
  void genericList() throws Exception {
    GenericCustomerList<RegularCustomer> customers = new GenericCustomerList<>();
    customers.add(new RegularCustomer("someCustomer", 5));

    String canonicalTypeString = json().getMapper().getCanonicalTypeName(customers);
    assertThat(canonicalTypeString).isNotNull();

    final byte[] bytes = serializeToByteArray(customers);
    assertThat(bytes).isNotEmpty();

    final Object o = deserializeFromByteArray(bytes, canonicalTypeString);
    assertThat(o).isInstanceOf(GenericCustomerList.class);

    GenericCustomerList<?> deserializedCustomerList = (GenericCustomerList<?>) o;
    assertThat(deserializedCustomerList.get(0).getName()).isEqualTo("someCustomer");
    assertThat(deserializedCustomerList.get(0).getContractStartDate()).isEqualTo(5);

  }

  @Test
  void serializeAndDeserializeGenericCollection() throws Exception {
    List<DmnDecisionResultEntries> ruleResults = new ArrayList<>();
    final DmnDecisionResultEntriesImpl result1 = new DmnDecisionResultEntriesImpl();
    result1.putValue("key1", "value1");
    result1.putValue("key2", "value2");
    ruleResults.add(result1);
    final DmnDecisionResultEntriesImpl result2 = new DmnDecisionResultEntriesImpl();
    result2.putValue("key3", "value3");
    result2.putValue("key4", "value4");
    ruleResults.add(result2);
    DmnDecisionResultImpl dmnDecisionResult = new DmnDecisionResultImpl(ruleResults);

    String canonicalTypeString = json().getMapper().getCanonicalTypeName(dmnDecisionResult);
    assertThat(canonicalTypeString).isNotNull();

    final byte[] bytes = serializeToByteArray(dmnDecisionResult);
    assertThat(bytes).isNotEmpty();

    //deserialization is not working for this kind of class
  }

  @Test
  void deserializeHashMap() throws Exception {
    final byte[] bytes = "{\"foo\": \"bar\"}".getBytes();
    assertThat(bytes).isNotEmpty();

    @SuppressWarnings("unchecked")
    final HashMap<String,String> deserializedMap = (HashMap<String, String>) deserializeFromByteArray(bytes, "java.util.HashMap<java.lang.String, java.lang.String>");
    assertThat(deserializedMap)
      .containsKey("foo")
      .containsEntry("foo", "bar");
  }

  protected byte[] serializeToByteArray(Object deserializedObject) throws Exception {
    DataFormatMapper mapper = json().getMapper();
    DataFormatWriter writer = json().getWriter();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OutputStreamWriter outWriter = new OutputStreamWriter(out);

    try (out; outWriter; BufferedWriter bufferedWriter = new BufferedWriter(outWriter)) {
      Object mappedObject = mapper.mapJavaToInternal(deserializedObject);
      writer.writeToWriter(bufferedWriter, mappedObject);
      return out.toByteArray();
    }
  }

  protected Object deserializeFromByteArray(byte[] bytes, String objectTypeName) throws Exception {
    DataFormatMapper mapper = json().getMapper();
    DataFormatReader reader = json().getReader();

    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    InputStreamReader inReader = new InputStreamReader(bais);

    try (bais; inReader; BufferedReader bufferedReader = new BufferedReader(inReader)) {
      Object mappedObject = reader.readInput(bufferedReader);
      return doDeserialization(mapper, mappedObject, objectTypeName);
    }
  }

  protected Object doDeserialization(DataFormatMapper mapper, Object mappedObject, String objectTypeName) {
    return mapper.mapInternalToJava(mappedObject, objectTypeName);
  }

}
