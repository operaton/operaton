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
package org.operaton.spin.impl.json.jackson.format;

import org.operaton.spin.DeserializationTypeValidator;
import org.operaton.spin.SpinRuntimeException;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JsonDeserializationValidationTest {

  protected DeserializationTypeValidator validator;
  protected static JacksonJsonDataFormat format;

  @BeforeAll
  static void setUpMocks() {
    format = new JacksonJsonDataFormat("test");
  }

  @AfterAll
  static void tearDown() {
    format = null;
  }

  @Test
  void shouldValidateNothingForPrimitiveClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(int.class);
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    Mockito.verifyNoInteractions(validator);
  }

  @Test
  void shouldValidateBaseTypeOnlyForBaseClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(String.class);
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    verify(validator).validate("java.lang.String");
    verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateBaseTypeOnlyForComplexClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Complex.class);
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    verify(validator).validate("org.operaton.spin.impl.json.jackson.format.JsonDeserializationValidationTest$Complex");
    verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateContentTypeOnlyForArrayClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Integer[].class);
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    verify(validator).validate("java.lang.Integer");
    verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateCollectionAndContentTypeForCollectionClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.lang.String>");
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    verify(validator).validate("java.util.ArrayList");
    verify(validator).validate("java.lang.String");
    verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateCollectionAndContentTypeForNestedCollectionClass() {
    // given
    JavaType type = TypeFactory.defaultInstance()
        .constructFromCanonical("java.util.ArrayList<java.util.ArrayList<java.lang.String>>");
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    verify(validator, times(2)).validate("java.util.ArrayList");
    verify(validator).validate("java.lang.String");
    verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateMapAndKeyAndContentTypeForMapClass() {
    // given
    JavaType type = TypeFactory.defaultInstance()
        .constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.Integer>");
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    verify(validator).validate("java.util.HashMap");
    verify(validator).validate("java.lang.String");
    verify(validator).validate("java.lang.Integer");
    verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldFailForSimpleClass() {
    JavaType type = TypeFactory.defaultInstance().constructType(String.class);
    // given
    validator = createValidatorMock(false);

    // when
    assertThatThrownBy(() -> format.getMapper().validateType(type, validator))
        .isInstanceOf(SpinRuntimeException.class)
        .hasMessageContaining("[java.lang.String]");
  }

  @Test
  void shouldFailForComplexClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Complex.class);
    validator = createValidatorMock(false);

    // when
    assertThatThrownBy(() -> format.getMapper().validateType(type, validator))
        .isInstanceOf(SpinRuntimeException.class)
        .hasMessageContaining("[org.operaton.spin.impl.json.jackson.format.JsonDeserializationValidationTest$Complex]");
  }

  @Test
  void shouldFailForArrayClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Integer[].class);
    validator = createValidatorMock(false);

    // when & then
    assertThatThrownBy(() -> format.getMapper().validateType(type, validator))
        .isInstanceOf(SpinRuntimeException.class)
        .hasMessageContaining("[java.lang.Integer]");
  }

  @Test
  void shouldFailForCollectionClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.lang.String>");
    validator = createValidatorMock(false);

    // when & then
    assertThatThrownBy(() -> format.getMapper().validateType(type, validator))
        .isInstanceOf(SpinRuntimeException.class)
        .hasMessageContaining("[java.util.ArrayList, java.lang.String]");
  }

  @Test
  void shouldFailForMapClass() {
    // given
    JavaType type = TypeFactory.defaultInstance()
        .constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.Integer>");
    validator = createValidatorMock(false);

    // when & then
    assertThatThrownBy(() -> format.getMapper().validateType(type, validator))
        .isInstanceOf(SpinRuntimeException.class)
        .hasMessageContaining("[java.util.HashMap, java.lang.String, java.lang.Integer]");
  }

  @Test
  void shouldFailOnceForMapClass() {
    // given
    JavaType type = TypeFactory.defaultInstance()
        .constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.String>");
    validator = createValidatorMock(false);

    // when & then
    assertThatThrownBy(() -> format.getMapper().validateType(type, validator))
        .isInstanceOf(SpinRuntimeException.class)
        .hasMessageContaining("[java.util.HashMap, java.lang.String]");
  }

  public static class Complex {
    private Nested nested;

    public Nested getNested() {
      return nested;
    }
  }

  public static class Nested {
    private int testInt;

    public int getTestInt() {
      return testInt;
    }
  }

  protected DeserializationTypeValidator createValidatorMock(boolean result) {
    DeserializationTypeValidator newValidator = Mockito.mock(DeserializationTypeValidator.class);
    Mockito.when(newValidator.validate(Mockito.anyString())).thenReturn(result);
    return newValidator;
  }
}
