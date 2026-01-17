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
import java.util.List;

import java.util.Collection;
import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.impl.value.NullValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.operaton.bpm.engine.variable.Variables.*;
import static org.operaton.bpm.engine.variable.type.ValueType.*;
import static org.assertj.core.api.Assertions.assertThat;

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
    return List.of(new Object[][] {
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

    assertThat(variables).containsEntry(variableName, initValue);
    assertThat(variables.getValueTyped(variableName).getValue()).isEqualTo(initValue);

    // no type information present
    TypedValue typed = variables.getValueTyped(variableName);
    if (!(typed instanceof NullValueImpl)) {
      assertThat(typed.getType()).isNull();
      assertThat(typed.getValue()).isEqualTo(variables.get(variableName));
    } else {
      assertThat(typed.getType()).isEqualTo(NULL);
    }
  }

  @MethodSource("data")
  @ParameterizedTest(name = "{index}: {0} = {1}")
  void createPrimitiveVariableTyped(ValueType valueType, Object value, TypedValue typedValue, TypedValue nullValue) {
    initPrimitiveValueTest(valueType, value, typedValue, nullValue);
    VariableMap variables = createVariables().putValue(variableName, typedValue);

    // get return value
    assertThat(variables).containsEntry(variableName, value);

    // type is not lost
    assertThat(variables.getValueTyped(variableName).getType()).isEqualTo(valueType);

    // get wrapper
    Object stringValue = variables.getValueTyped(variableName).getValue();
    assertThat(stringValue).isEqualTo(value);
  }

  @MethodSource("data")
  @ParameterizedTest(name = "{index}: {0} = {1}")
  void createPrimitiveVariableNull(ValueType valueType, Object value, TypedValue typedValue, TypedValue nullValue) {
    initPrimitiveValueTest(valueType, value, typedValue, nullValue);
    VariableMap variables = createVariables().putValue(variableName, nullValue);

    // get return value
    assertThat(variables.get(variableName)).isNull();

    // type is not lost
    assertThat(variables.getValueTyped(variableName).getType()).isEqualTo(valueType);

    // get wrapper
    Object stringValue = variables.getValueTyped(variableName).getValue();
    assertThat(stringValue).isNull();
  }

  public void initPrimitiveValueTest(ValueType valueType, Object value, TypedValue typedValue, TypedValue nullValue) {
    this.valueType = valueType;
    this.value = value;
    this.typedValue = typedValue;
    this.nullValue = nullValue;
  }

}
