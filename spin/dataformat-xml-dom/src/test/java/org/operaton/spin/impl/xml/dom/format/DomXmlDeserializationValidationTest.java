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
package org.operaton.spin.impl.xml.dom.format;

import org.operaton.spin.DeserializationTypeValidator;
import org.operaton.spin.SpinRuntimeException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomXmlDeserializationValidationTest {

  protected DeserializationTypeValidator validator;
  protected static DomXmlDataFormat format;

  @BeforeAll
  static void setUpMocks() {
    format = new DomXmlDataFormat("test");
  }

  @AfterAll
  static void tearDown() {
    format = null;
  }

  @Test
  void shouldValidateNothingForPrimitiveClass() {
    // given
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(int.class, validator);

    // then
    Mockito.verifyNoInteractions(validator);
  }

  @Test
  void shouldValidateBaseTypeOnlyForBaseClass() {
    // given
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(String.class, validator);

    // then
    Mockito.verify(validator).validate("java.lang.String");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateBaseTypeOnlyForComplexClass() {
    // given
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(Complex.class, validator);

    // then
    Mockito.verify(validator).validate("org.operaton.spin.impl.xml.dom.format.DomXmlDeserializationValidationTest$Complex");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldValidateContentTypeOnlyForArrayClass() {
    // given
    validator = createValidatorMock(true);

    // when
    format.getMapper().validateType(Integer[].class, validator);

    // then
    Mockito.verify(validator).validate("java.lang.Integer");
    Mockito.verifyNoMoreInteractions(validator);
  }

  @Test
  void shouldFailForSimpleClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(String.class, validator);
    });
    assertTrue(exception.getMessage().contains("'java.lang.String'"));
  }

  @Test
  void shouldFailForComplexClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(Complex.class, validator);
    });
    assertTrue(exception.getMessage().contains("'org.operaton.spin.impl.xml.dom.format.DomXmlDeserializationValidationTest$Complex'"));
  }

  @Test
  void shouldFailForArrayClass() {
    Throwable exception = assertThrows(SpinRuntimeException.class, () -> {
      // given
      validator = createValidatorMock(false);

      // when
      format.getMapper().validateType(Integer[].class, validator);
    });
    assertTrue(exception.getMessage().contains("'java.lang.Integer'"));
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
