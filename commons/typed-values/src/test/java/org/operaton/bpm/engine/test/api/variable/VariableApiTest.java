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
package org.operaton.bpm.engine.test.api.variable;
import static org.operaton.bpm.engine.variable.Variables.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
class VariableApiTest {

  private static final String DESERIALIZED_OBJECT_VAR_NAME = "deserializedObject";
  private static final ExampleObject DESERIALIZED_OBJECT_VAR_VALUE = new ExampleObject();

  private static final String SERIALIZATION_DATA_FORMAT_NAME = "data-format-name";

  @Test
  void createObjectVariables() {

      VariableMap variables = createVariables()
              .putValue(DESERIALIZED_OBJECT_VAR_NAME, objectValue(DESERIALIZED_OBJECT_VAR_VALUE));

      assertThat(variables.get(DESERIALIZED_OBJECT_VAR_NAME)).isEqualTo(DESERIALIZED_OBJECT_VAR_VALUE);
      assertThat(variables.getValue(DESERIALIZED_OBJECT_VAR_NAME, ExampleObject.class)).isEqualTo(DESERIALIZED_OBJECT_VAR_VALUE);

      Object untypedValue = variables.getValueTyped(DESERIALIZED_OBJECT_VAR_NAME).getValue();
      assertThat(untypedValue).isEqualTo(DESERIALIZED_OBJECT_VAR_VALUE);

      ExampleObject typedValue = variables.<ObjectValue>getValueTyped(DESERIALIZED_OBJECT_VAR_NAME).getValue(ExampleObject.class);
      assertThat(typedValue).isEqualTo(DESERIALIZED_OBJECT_VAR_VALUE);

      // object type name is not yet available
      assertThat(variables.<ObjectValue>getValueTyped(DESERIALIZED_OBJECT_VAR_NAME).getObjectTypeName()).isNull();
      // class is available
      assertThat(variables.<ObjectValue>getValueTyped(DESERIALIZED_OBJECT_VAR_NAME).getObjectType()).isEqualTo(DESERIALIZED_OBJECT_VAR_VALUE.getClass());


      variables = createVariables()
              .putValue(DESERIALIZED_OBJECT_VAR_NAME, objectValue(DESERIALIZED_OBJECT_VAR_VALUE).serializationDataFormat(SERIALIZATION_DATA_FORMAT_NAME));

      assertThat(variables.get(DESERIALIZED_OBJECT_VAR_NAME)).isEqualTo(DESERIALIZED_OBJECT_VAR_VALUE);
  }

  @Test
  void variableMapWithoutCreateVariables() {
      VariableMap map1 = putValue("foo", true).putValue("bar", 20);
      VariableMap map2 = putValueTyped("foo", booleanValue(true)).putValue("bar", integerValue(20));

      assertThat(map2).isEqualTo(map1);
      assertThat(map1.values().containsAll(map2.values())).isTrue();
  }

  @Test
  void variableMapCompatibility() {

      // test compatibility with Map<String, Object>
      VariableMap map1 = createVariables()
              .putValue("foo", 10)
              .putValue("bar", 20);

      // assert the map is assignable to Map<String,Object>
      @SuppressWarnings("unused")
      Map<String, Object> assignable = map1;

      VariableMap map2 = createVariables()
              .putValueTyped("foo", integerValue(10))
              .putValueTyped("bar", integerValue(20));

      Map<String, Object> map3 = new HashMap<>();
      map3.put("foo", 10);
      map3.put("bar", 20);

      // equals()
      assertThat(map2).isEqualTo(map1);
      assertThat(map3).isEqualTo(map2);
      assertThat(fromMap(map1)).isEqualTo(map1);
      assertThat(fromMap(map3)).isEqualTo(map1);

      // hashCode()
      assertThat(map2.hashCode()).isEqualTo(map1.hashCode());
      assertThat(map3.hashCode()).isEqualTo(map2.hashCode());

      // values()
      Collection<Object> values1 = map1.values();
      Collection<Object> values2 = map2.values();
      Collection<Object> values3 = map3.values();
      assertThat(values1.containsAll(values2)).isTrue();
      assertThat(values2.containsAll(values1)).isTrue();
      assertThat(values2.containsAll(values3)).isTrue();
      assertThat(values3.containsAll(values2)).isTrue();

      // entry set
      assertThat(map2.entrySet()).isEqualTo(map1.entrySet());
      assertThat(map3.entrySet()).isEqualTo(map2.entrySet());
  }

  @Test
  void serializationDataFormats() {
      ObjectValue objectValue = objectValue(DESERIALIZED_OBJECT_VAR_VALUE).serializationDataFormat(SerializationDataFormats.JAVA).create();
      assertThat(objectValue.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());

      objectValue = objectValue(DESERIALIZED_OBJECT_VAR_VALUE).serializationDataFormat(SerializationDataFormats.JSON).create();
      assertThat(objectValue.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JSON.getName());

      objectValue = objectValue(DESERIALIZED_OBJECT_VAR_VALUE).serializationDataFormat(SerializationDataFormats.XML).create();
      assertThat(objectValue.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.XML.getName());
  }

  @Test
  void emptyVariableMapAsVariableContext() {
      VariableContext varContext = createVariables().asVariableContext();
      assertThat(varContext.keySet().size()).isEqualTo(0);
      assertThat(varContext.resolve("nonExisting")).isNull();
      assertThat(varContext.containsVariable("nonExisting")).isFalse();
  }

  @Test
  void testEmptyVariableContext() {
      VariableContext varContext = emptyVariableContext();
      assertThat(varContext.keySet().size()).isEqualTo(0);
      assertThat(varContext.resolve("nonExisting")).isNull();
      assertThat(varContext.containsVariable("nonExisting")).isFalse();
  }

  @Test
  void variableMapAsVariableContext() {
      VariableContext varContext = createVariables()
              .putValueTyped("someValue", integerValue(1)).asVariableContext();

      assertThat(varContext.keySet().size()).isEqualTo(1);

      assertThat(varContext.resolve("nonExisting")).isNull();
      assertThat(varContext.containsVariable("nonExisting")).isFalse();

      assertThat(varContext.resolve("someValue").getValue()).isEqualTo(1);
      assertThat(varContext.containsVariable("someValue")).isTrue();
  }

  @Test
  void transientVariables() throws URISyntaxException {
    VariableMap variableMap = createVariables().putValueTyped("foo", doubleValue(10.0, true))
                     .putValueTyped("bar", integerValue(10, true))
                     .putValueTyped("aa", booleanValue(true, true))
                     .putValueTyped("bb", stringValue("bb", true))
                     .putValueTyped("test", byteArrayValue("test".getBytes(), true))
                     .putValueTyped("blob", fileValue(new File(this.getClass().getClassLoader().getResource("org/operaton/bpm/engine/test/variables/simpleFile.txt").toURI()), true))
                     .putValueTyped("val", dateValue(new Date(), true))
                     .putValueTyped("var", objectValue(BigDecimal.valueOf(10), true).create())
                     .putValueTyped("short", shortValue((short)12, true))
                     .putValueTyped("long", longValue((long)10, true))
                     .putValueTyped("file", fileValue("org/operaton/bpm/engine/test/variables/simpleFile.txt").setTransient(true).create())
                     .putValueTyped("hi", untypedValue("stringUntyped", true))
                     .putValueTyped("null", untypedValue(null, true))
                     .putValueTyped("ser", serializedObjectValue("{\"name\" : \"foo\"}", true).create());

    for (Entry<String, Object> e : variableMap.entrySet()) {
      TypedValue value = variableMap.getValueTyped(e.getKey());
        assertThat(value.isTransient())
                .as("Variable '%s' is not transient: %s", e.getKey(), value)
                .isTrue();
    }
  }

  @Test
  void transientVariablesRaw() throws URISyntaxException {
    VariableMap variableMap = createVariables().putValueTyped("foo", doubleValue(10.0, true))
                     .putValue("bar", integerValue(10, true))
                     .putValue("aa", booleanValue(true, true))
                     .putValue("bb", stringValue("bb", true))
                     .putValue("test", byteArrayValue("test".getBytes(), true))
                     .putValue("blob", fileValue(new File(this.getClass().getClassLoader().getResource("org/operaton/bpm/engine/test/variables/simpleFile.txt").toURI()), true))
                     .putValue("val", dateValue(new Date(), true))
                     .putValue("var", objectValue(BigDecimal.valueOf(10), true).create())
                     .putValue("varBuilder", objectValue(BigDecimal.valueOf(10), true))
                     .putValue("short", shortValue((short)12, true))
                     .putValue("long", longValue((long)10, true))
                     .putValue("file", fileValue("org/operaton/bpm/engine/test/variables/simpleFile.txt").setTransient(true).create())
                     .putValue("hi", untypedValue("stringUntyped", true))
                     .putValue("null", untypedValue(null, true))
                     .putValue("ser", serializedObjectValue("{\"name\" : \"foo\"}", true).create())
                     .putValue("serBuilder", serializedObjectValue("{\"name\" : \"foo\"}", true));

    for (Entry<String, Object> e : variableMap.entrySet()) {
      TypedValue value = variableMap.getValueTyped(e.getKey());
        assertThat(value.isTransient())
                .as("Variable '%s' is not transient: %s", e.getKey(), value)
                .isTrue();
    }
  }
}
