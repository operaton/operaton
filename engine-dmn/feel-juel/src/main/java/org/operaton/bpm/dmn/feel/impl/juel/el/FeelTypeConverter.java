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
package org.operaton.bpm.dmn.feel.impl.juel.el;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.operaton.bpm.impl.juel.jakarta.el.ELException;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

import org.operaton.bpm.impl.juel.TypeConverterImpl;

public class FeelTypeConverter extends TypeConverterImpl {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

    /**
   * Coerces the given value to a Boolean.
   * 
   * @param value the value to coerce
   * @return the Boolean representation of the value
   * @throws IllegalArgumentException if the value cannot be converted to a Boolean
   */
  @Override
  protected Boolean coerceToBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    else {
      throw LOG.unableToConvertValue(value, Boolean.class);
    }
  }

    /**
   * Coerces the given value to a BigDecimal if possible.
   * 
   * @param value the value to be coerced
   * @return the value coerced to a BigDecimal
   * @throws IllegalArgumentException if the value cannot be coerced to a BigDecimal
   */
  @Override
  protected BigDecimal coerceToBigDecimal(Object value) {
    if (value instanceof BigDecimal) {
      return (BigDecimal)value;
    }
    else if (value instanceof BigInteger) {
      return new BigDecimal((BigInteger)value);
    }
    else if (value instanceof Number) {
      return new BigDecimal(((Number)value).doubleValue());
    }
    else {
      throw LOG.unableToConvertValue(value, BigDecimal.class);
    }
  }

    /**
   * Coerces the given value to a BigInteger.
   *
   * @param value the value to be coerced
   * @return the BigInteger representation of the value
   * @throws IllegalArgumentException if the value cannot be converted to a BigInteger
   */
  @Override
  protected BigInteger coerceToBigInteger(Object value) {
    if (value instanceof BigInteger) {
      return (BigInteger)value;
    }
    else if (value instanceof BigDecimal) {
      return ((BigDecimal)value).toBigInteger();
    }
    else if (value instanceof Number) {
      return BigInteger.valueOf(((Number)value).longValue());
    }
    else {
      throw LOG.unableToConvertValue(value, BigInteger.class);
    }
  }

    /**
   * Coerces an object value to a Double.
   *
   * @param value the object value to be coerced
   * @return the Double value of the input object
   * @throws IllegalArgumentException if unable to convert the value to a Double
   */
  @Override
  protected Double coerceToDouble(Object value) {
    if (value instanceof Double) {
      return (Double)value;
    }
    else if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    else {
      throw LOG.unableToConvertValue(value, Double.class);
    }
  }

    /**
   * Coerces the given value to a Long.
   * 
   * @param value the value to coerce
   * @return the coerced Long value
   * @throws IllegalArgumentException if the value cannot be coerced to Long
   */
  @Override
  protected Long coerceToLong(Object value) {
    if (value instanceof Long) {
      return (Long)value;
    }
    else if (value instanceof Number && isLong((Number) value)) {
      return ((Number) value).longValue();
    }
    else {
      throw LOG.unableToConvertValue(value, Long.class);
    }
  }

    /**
   * Coerces an object value into a String representation.
   *
   * @param value the object value to be coerced
   * @return the String representation of the object value
   * @throws IllegalArgumentException if the value cannot be converted to a String
   */
  @Override
  protected String coerceToString(Object value) {
    if (value instanceof String) {
      return (String)value;
    }
    else if (value instanceof Enum<?>) {
      return ((Enum<?>)value).name();
    }
    else {
      throw LOG.unableToConvertValue(value, String.class);
    }
  }

    /**
   * Convert the given value to the specified type.
   *
   * @param value the value to convert
   * @param type the class representing the type to convert to
   * @return the converted value
   * @throws ELException if unable to convert the value
   */
  @Override
  public <T> T convert(Object value, Class<T> type) throws ELException {
    try {
      return super.convert(value, type);
    }
    catch (ELException e) {
      throw LOG.unableToConvertValue(value, type, e);
    }
  }

    /**
   * Checks if a given Number value is a whole number by converting it to a double and comparing it with its long value.
   * 
   * @param value the Number value to be checked
   * @return true if the value is a whole number, false otherwise
   */
  protected boolean isLong(Number value) {
    double doubleValue = value.doubleValue();
    return doubleValue == (long) doubleValue;
  }

}
