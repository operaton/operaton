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

import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.impl.value.NullValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;
import static org.operaton.bpm.engine.variable.Variables.*;
import static org.operaton.bpm.engine.variable.type.ValueType.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philipp Ossler *
 */
public class PrimitiveValueTest {

  protected static final Date DATE_VALUE = new Date();
  protected static final String LOCAL_DATE_VALUE = "2015-09-18";
  protected static final String LOCAL_TIME_VALUE = "10:00:00";
  protected static final String PERIOD_VALUE = "P14D";
  protected static final byte[] BYTES_VALUE = "a".getBytes();

  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { STRING, "someString", stringValue("someString"), stringValue(null) },
        { INTEGER, 1, integerValue(1), integerValue(null) },
        { BOOLEAN, true, booleanValue(true), booleanValue(null) },
        { NULL, null, untypedNullValue(), untypedNullValue() },
        { SHORT, (short) 1, shortValue((short) 1), shortValue(null) },
        { DOUBLE, 1d, doubleValue(1d), doubleValue(null) },
        { DATE, DATE_VALUE, dateValue(DATE_VALUE), dateValue(null) },
        { BYTES, BYTES_VALUE, byteArrayValue(BYTES_VALUE), byteArrayValue(null) }
      });
  }
  public ValueType valueType;
  public Object value;
  public TypedValue typedValue;
  public TypedValue nullValue;

  protected String variableName = "variable";

  @MethodSource("data")
  @ParameterizedTest(name = "{index}: {0} = {1}")
  void createPrimitiveVariableUntyped(ValueType initValueType, Object initValue, TypedValue initTypedValue, TypedValue initNullValue) {
    initPrimitiveValueTest(initValueType, initValue, initTypedValue, initNullValue);
    VariableMap variables = createVariables().putValue(variableName, initValue);

    assertEquals(initValue, variables.get(variableName));
    assertEquals(initValue, variables.getValueTyped(variableName).getValue());

    // no type information present
    TypedValue typed = variables.getValueTyped(variableName);
    if (!(typed instanceof NullValueImpl)) {
      assertNull(typed.getType());
      assertEquals(variables.get(variableName), typed.getValue());
    } else {
      assertEquals(NULL, typed.getType());
    }
  }

  @MethodSource("data")
  @ParameterizedTest(name = "{index}: {0} = {1}")
  void createPrimitiveVariableTyped(ValueType valueType, Object value, TypedValue typedValue, TypedValue nullValue) {
    initPrimitiveValueTest(valueType, value, typedValue, nullValue);
    VariableMap variables = createVariables().putValue(variableName, typedValue);

    // get return value
    assertEquals(value, variables.get(variableName));

    // type is not lost
    assertEquals(valueType, variables.getValueTyped(variableName).getType());

    // get wrapper
    Object stringValue = variables.getValueTyped(variableName).getValue();
    assertEquals(value, stringValue);
  }

  @MethodSource("data")
  @ParameterizedTest(name = "{index}: {0} = {1}")
  void createPrimitiveVariableNull(ValueType valueType, Object value, TypedValue typedValue, TypedValue nullValue) {
    initPrimitiveValueTest(valueType, value, typedValue, nullValue);
    VariableMap variables = createVariables().putValue(variableName, nullValue);

    // get return value
    assertNull(variables.get(variableName));

    // type is not lost
    assertEquals(valueType, variables.getValueTyped(variableName).getType());

    // get wrapper
    Object stringValue = variables.getValueTyped(variableName).getValue();
    assertNull(stringValue);
  }

  public void initPrimitiveValueTest(ValueType valueType, Object value, TypedValue typedValue, TypedValue nullValue) {
    this.valueType = valueType;
    this.value = value;
    this.typedValue = typedValue;
    this.nullValue = nullValue;
  }

}
