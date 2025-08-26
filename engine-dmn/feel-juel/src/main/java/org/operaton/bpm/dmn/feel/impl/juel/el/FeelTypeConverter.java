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
package org.operaton.bpm.dmn.feel.impl.juel.el;

import java.math.BigDecimal;
import java.math.BigInteger;
import jakarta.el.ELException;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;
import org.operaton.bpm.impl.juel.TypeConverterImpl;

public class FeelTypeConverter extends TypeConverterImpl {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  @Override
  protected Boolean coerceToBoolean(Object value) {
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    else {
      throw LOG.unableToConvertValue(value, Boolean.class);
    }
  }

  @Override
  protected BigDecimal coerceToBigDecimal(Object value) {
    if (value instanceof BigDecimal bigDecimalValue) {
      return bigDecimalValue;
    }
    else if (value instanceof BigInteger bigIntegerValue) {
      return new BigDecimal(bigIntegerValue);
    }
    else if (value instanceof Number numberValue) {
      return BigDecimal.valueOf(numberValue.doubleValue());
    }
    else {
      throw LOG.unableToConvertValue(value, BigDecimal.class);
    }
  }

  @Override
  protected BigInteger coerceToBigInteger(Object value) {
    if (value instanceof BigInteger bigIntegerValue) {
      return bigIntegerValue;
    }
    else if (value instanceof BigDecimal bigDecimalValue) {
      return bigDecimalValue.toBigInteger();
    }
    else if (value instanceof Number numberValue) {
      return BigInteger.valueOf(numberValue.longValue());
    }
    else {
      throw LOG.unableToConvertValue(value, BigInteger.class);
    }
  }

  @Override
  protected Double coerceToDouble(Object value) {
    if (value instanceof Double doubleValue) {
      return doubleValue;
    }
    else if (value instanceof Number numberValue) {
      return numberValue.doubleValue();
    }
    else {
      throw LOG.unableToConvertValue(value, Double.class);
    }
  }

  @Override
  protected Long coerceToLong(Object value) {
    if (value instanceof Long longValue) {
      return longValue;
    }
    else if (value instanceof Number numberValue && isLong(numberValue)) {
      return numberValue.longValue();
    }
    else {
      throw LOG.unableToConvertValue(value, Long.class);
    }
  }

  @Override
  protected String coerceToString(Object value) {
    if (value instanceof String stringValue) {
      return stringValue;
    }
    else if (value instanceof Enum<?> enumValue) {
      return enumValue.name();
    }
    else {
      throw LOG.unableToConvertValue(value, String.class);
    }
  }

  @Override
  public <T> T convert(Object value, Class<T> type) throws ELException {
    try {
      return super.convert(value, type);
    }
    catch (ELException e) {
      throw LOG.unableToConvertValue(value, type, e);
    }
  }

  protected boolean isLong(Number value) {
    double doubleValue = value.doubleValue();
    return doubleValue == (long) doubleValue;
  }

}
