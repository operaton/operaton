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
package org.operaton.bpm.engine.test.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
public class TypedValueAssert {

  public static void assertObjectValueDeserializedNull(ObjectValue typedValue) {
    assertThat(typedValue).isNotNull();
    assertThat(typedValue.isDeserialized()).isTrue();
    assertThat(typedValue.getSerializationDataFormat()).isNotNull();
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getValueSerialized()).isNull();
    assertThat(typedValue.getObjectType()).isNull();
    assertThat(typedValue.getObjectTypeName()).isNull();
  }

  public static void assertObjectValueSerializedNull(ObjectValue typedValue) {
    assertThat(typedValue).isNotNull();
    assertThat(typedValue.isDeserialized()).isFalse();
    assertThat(typedValue.getSerializationDataFormat()).isNotNull();
    assertThat(typedValue.getValueSerialized()).isNull();
    assertThat(typedValue.getObjectTypeName()).isNull();
  }

  public static void assertObjectValueDeserialized(ObjectValue typedValue, Object value) {
    Class<? extends Object> expectedObjectType = value.getClass();
    assertThat(typedValue.isDeserialized()).isTrue();

    assertThat(typedValue.getType()).isEqualTo(ValueType.OBJECT);

    assertThat(typedValue.getValue()).isEqualTo(value);
    assertThat(typedValue.getValue(expectedObjectType)).isEqualTo(value);

    assertThat(typedValue.getObjectType()).isEqualTo(expectedObjectType);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(expectedObjectType.getName());
  }

  public static void assertObjectValueSerializedJava(ObjectValue typedValue, Object value) {
    assertThat(typedValue.getSerializationDataFormat()).isEqualTo(Variables.SerializationDataFormats.JAVA.getName());

    try {
      // validate this is the base 64 encoded string representation of the serialized value of the java object
      String valueSerialized = typedValue.getValueSerialized();
      byte[] decodedObject = Base64.getDecoder().decode(valueSerialized.getBytes(StandardCharsets.UTF_8));
      ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(decodedObject));
      assertThat(objectInputStream.readObject()).isEqualTo(value);
    }
    catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static void assertUntypedNullValue(TypedValue nullValue) {
    assertThat(nullValue).isNotNull();
    assertThat(nullValue.getValue()).isNull();
    assertThat(nullValue.getType()).isEqualTo(ValueType.NULL);
  }

  private TypedValueAssert() {
  }


}
