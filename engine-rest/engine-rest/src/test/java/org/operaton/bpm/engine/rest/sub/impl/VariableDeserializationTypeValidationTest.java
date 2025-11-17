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
package org.operaton.bpm.engine.rest.sub.impl;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.runtime.DeserializationTypeValidator;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.times;

class VariableDeserializationTypeValidationTest {
  AbstractVariablesResource variablesResourceSpy;
  DeserializationTypeValidator validator;

  @BeforeEach
  void setUpMocks() {
    validator = Mockito.mock(DeserializationTypeValidator.class);

    ProcessEngineConfiguration configurationMock = Mockito.mock(ProcessEngineConfiguration.class);
    Mockito.when(configurationMock.isDeserializationTypeValidationEnabled()).thenReturn(true);
    Mockito.when(configurationMock.getDeserializationTypeValidator()).thenReturn(validator);

    variablesResourceSpy = createVariablesResourceSpy();
    Mockito.when(variablesResourceSpy.getProcessEngineConfiguration()).thenReturn(configurationMock);
  }

  @Test
  void shouldValidateNothingForPrimitiveClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(int.class);
    setValidatorMockResult(true);

    // when
    variablesResourceSpy.validateType(type);

    // then
    Mockito.verifyNoInteractions(validator);
  }

  @Test
  void shouldValidateBaseTypeOnlyForSimpleClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(String.class);
    setValidatorMockResult(true);

    // when
    variablesResourceSpy.validateType(type);

    // then
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateBaseTypeOnlyForComplexClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Complex.class);
    setValidatorMockResult(true);

    // when
    variablesResourceSpy.validateType(type);

    // then
    Mockito.verify(validator).validate("org.operaton.bpm.engine.rest.sub.impl.VariableDeserializationTypeValidationTest$Complex");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateContentTypeOnlyForArrayClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructType(Integer[].class);
    setValidatorMockResult(true);

    // when
    variablesResourceSpy.validateType(type);

    // then
    Mockito.verify(validator).validate("java.lang.Integer");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateCollectionAndContentTypeForCollectionClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.lang.String>");
    setValidatorMockResult(true);

    // when
    variablesResourceSpy.validateType(type);

    // then
    Mockito.verify(validator).validate("java.util.ArrayList");
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateCollectionAndContentTypeForNestedCollectionClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.util.ArrayList<java.lang.String>>");
    setValidatorMockResult(true);

    // when
    variablesResourceSpy.validateType(type);

    // then
    Mockito.verify(validator, times(2)).validate("java.util.ArrayList");
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateMapAndKeyAndContentTypeForMapClass() {
    // given
    JavaType type = TypeFactory.defaultInstance().constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.Integer>");
    setValidatorMockResult(true);

    // when
    variablesResourceSpy.validateType(type);

    // then
    Mockito.verify(validator).validate("java.util.HashMap");
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verify(validator).validate("java.lang.Integer");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validateType_shouldFail_givenInvalidType_args")
  void validateType_shouldFail_givenInvalidType(String testName, JavaType type, String expectedMessagePart) {
    // given
    setValidatorMockResult(false);

    // when
    Exception e = assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> variablesResourceSpy.validateType(type)).actual();

    // then
    assertThat(e.getMessage()).contains(expectedMessagePart);
  }

  static Stream<Arguments> validateType_shouldFail_givenInvalidType_args () {
    return Stream.of(
      Arguments.of("Simple Class", TypeFactory.defaultInstance().constructType(String.class), "[java.lang.String]"),
      Arguments.of("Complex Class", TypeFactory.defaultInstance().constructType(Complex.class), "[org.operaton.bpm.engine.rest.sub.impl.VariableDeserializationTypeValidationTest$Complex]"),
      Arguments.of("Array Class", TypeFactory.defaultInstance().constructType(Integer[].class), "[java.lang.Integer]"),
      Arguments.of("Collection Class", TypeFactory.defaultInstance().constructFromCanonical("java.util.ArrayList<java.lang.String>"), "[java.util.ArrayList, java.lang.String]"),
      Arguments.of("Map Class String,Integer", TypeFactory.defaultInstance().constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.Integer>"), "[java.util.HashMap, java.lang.String, java.lang.Integer]"),
      Arguments.of("Map Class String,String", TypeFactory.defaultInstance().constructFromCanonical("java.util.HashMap<java.lang.String, java.lang.String>"), "[java.util.HashMap, java.lang.String]")
    );
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

  protected void setValidatorMockResult(boolean result) {
    Mockito.when(validator.validate(Mockito.anyString())).thenReturn(result);
  }

  protected AbstractVariablesResource createVariablesResourceSpy() {
    return Mockito.spy(new AbstractVariablesResource(Mockito.mock(ProcessEngine.class), "test", Mockito.mock(ObjectMapper.class)) {

      @Override
      protected void updateVariableEntities(VariableMap variables, List<String> deletions) {
        // no-op
      }

      @Override
      protected void setVariableEntity(String variableKey, TypedValue variableValue) {
        // no-op
      }

      @Override
      protected void removeVariableEntity(String variableKey) {
        // no-op
      }

      @Override
      protected TypedValue getVariableEntity(String variableKey, boolean deserializeValue) {
        return null;
      }

      @Override
      protected VariableMap getVariableEntities(boolean deserializeValues) {
        return null;
      }

      @Override
      protected String getResourceTypeName() {
        return null;
      }
    });
  }

}
