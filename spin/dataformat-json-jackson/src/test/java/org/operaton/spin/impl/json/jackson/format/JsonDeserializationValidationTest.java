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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import org.operaton.spin.DeserializationTypeValidator;
import org.operaton.spin.SpinRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

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
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateBaseTypeOnlyForComplexClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Complex.class);
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    Mockito.verify(validator).validate("org.operaton.spin.impl.json.jackson.format.JsonDeserializationValidationTest$Complex");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateContentTypeOnlyForArrayClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Integer[].class);
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    Mockito.verify(validator).validate("java.lang.Integer");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateCollectionAndContentTypeForCollectionClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.lang.String>");
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    Mockito.verify(validator).validate("java.util.ArrayList");
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateCollectionAndContentTypeForNestedCollectionClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.util.ArrayList<java.lang.String>>");
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    Mockito.verify(validator, times(2)).validate("java.util.ArrayList");
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateMapAndKeyAndContentTypeForMapClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.Integer>");
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(type, validator);

    // then
    Mockito.verify(validator).validate("java.util.HashMap");
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verify(validator).validate("java.lang.Integer");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldFailForSimpleClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      JavaType type = TypeFactory.defaultInstance().constructType(String.class);
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(type, validator);
    });
    assertTrue(exception.getMessage().contains("[java.lang.String]"));
  }

  @Test
  void shouldFailForComplexClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      JavaType type = TypeFactory.defaultInstance().constructType(Complex.class);
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(type, validator);
    });
    assertTrue(exception.getMessage().contains("[org.operaton.spin.impl.json.jackson.format.JsonDeserializationValidationTest$Complex]"));
  }

  @Test
  void shouldFailForArrayClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      JavaType type = TypeFactory.defaultInstance().constructType(Integer[].class);
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(type, validator);
    });
    assertTrue(exception.getMessage().contains("[java.lang.Integer]"));
  }

  @Test
  void shouldFailForCollectionClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.lang.String>");
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(type, validator);
    });
    assertTrue(exception.getMessage().contains("[java.util.ArrayList, java.lang.String]"));
  }

  @Test
  void shouldFailForMapClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.Integer>");
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(type, validator);
    });
    assertTrue(exception.getMessage().contains("[java.util.HashMap, java.lang.String, java.lang.Integer]"));
  }

  @Test
  void shouldFailOnceForMapClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.String>");
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(type, validator);
    });
    assertTrue(exception.getMessage().contains("[java.util.HashMap, java.lang.String]"));
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
