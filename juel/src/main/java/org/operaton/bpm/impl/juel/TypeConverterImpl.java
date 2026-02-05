/*
 * Based on JUEL 2.2.1 code, 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.operaton.bpm.impl.juel;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import jakarta.el.ELException;

/**
 * Type Conversions as described in EL 2.1 specification (section 1.17).
 */
public class TypeConverterImpl implements TypeConverter {
  @Serial private static final long serialVersionUID = 1L;
  private static final String ERROR_COERCE_TYPE = "error.coerce.type";
  private static final String ERROR_COERCE_VALUE = "error.coerce.value";

  protected Boolean coerceToBoolean(Object value) {
    if (value == null || "".equals(value)) {
      return Boolean.FALSE;
    }
    if (value instanceof Boolean isBoolean) {
      return isBoolean;
    }
    if (value instanceof String string) {
      return Boolean.valueOf(string);
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Boolean.class));
  }

  protected Character coerceToCharacter(Object value) {
    if (value == null || "".equals(value)) {
      return (char) 0;
    }
    if (value instanceof Character character) {
      return character;
    }
    if (value instanceof Number number) {
      return (char) number.shortValue();
    }
    if (value instanceof String string) {
      return string.charAt(0);
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Character.class));
  }

  protected BigDecimal coerceToBigDecimal(Object value) {
    if (value == null || "".equals(value)) {
      return BigDecimal.valueOf(0L);
    }
    if (value instanceof BigDecimal bigDecimal) {
      return bigDecimal;
    }
    if (value instanceof BigInteger bigInteger) {
      return new BigDecimal(bigInteger);
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    if (value instanceof String string) {
      try {
        return new BigDecimal(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, BigDecimal.class));
      }
    }
    if (value instanceof Character character) {
      return BigDecimal.valueOf((short) character.charValue());
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), BigDecimal.class));
  }

  protected BigInteger coerceToBigInteger(Object value) {
    if (value == null || "".equals(value)) {
      return BigInteger.valueOf(0L);
    }
    if (value instanceof BigInteger bigInteger) {
      return bigInteger;
    }
    if (value instanceof BigDecimal bigDecimal) {
      return bigDecimal.toBigInteger();
    }
    if (value instanceof Number number) {
      return BigInteger.valueOf(number.longValue());
    }
    if (value instanceof String string) {
      try {
        return new BigInteger(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, BigInteger.class));
      }
    }
    if (value instanceof Character character) {
      return BigInteger.valueOf((short) character.charValue());
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), BigInteger.class));
  }

  protected Double coerceToDouble(Object value) {
    if (value == null || "".equals(value)) {
      return (double) 0;
    }
    if (value instanceof Double doubleValue) {
      return doubleValue;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    if (value instanceof String string) {
      try {
        return Double.valueOf(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, Double.class));
      }
    }
    if (value instanceof Character character) {
      return (double) (short) character.charValue();
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Double.class));
  }

  protected Float coerceToFloat(Object value) {
    if (value == null || "".equals(value)) {
      return (float) 0;
    }
    if (value instanceof Float floatValue) {
      return floatValue;
    }
    if (value instanceof Number number) {
      return number.floatValue();
    }
    if (value instanceof String string) {
      try {
        return Float.valueOf(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, Float.class));
      }
    }
    if (value instanceof Character character) {
      return (float) (short) character.charValue();
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Float.class));
  }

  protected Long coerceToLong(Object value) {
    if (value == null || "".equals(value)) {
      return 0l;
    }
    if (value instanceof Long longValue) {
      return longValue;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String string) {
      try {
        return Long.valueOf(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, Long.class));
      }
    }
    if (value instanceof Character character) {
      return (long) (short) character.charValue();
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Long.class));
  }

  protected Integer coerceToInteger(Object value) {
    if (value == null || "".equals(value)) {
      return 0;
    }
    if (value instanceof Integer integerValue) {
      return integerValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String string) {
      try {
        return Integer.valueOf(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, Integer.class));
      }
    }
    if (value instanceof Character character) {
      return (int) (short) character.charValue();
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Integer.class));
  }

  protected Short coerceToShort(Object value) {
    if (value == null || "".equals(value)) {
      return (short) 0;
    }
    if (value instanceof Short shortValue) {
      return shortValue;
    }
    if (value instanceof Number number) {
      return number.shortValue();
    }
    if (value instanceof String string) {
      try {
        return Short.valueOf(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, Short.class));
      }
    }
    if (value instanceof Character character) {
      return (short) character.charValue();
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Short.class));
  }

  protected Byte coerceToByte(Object value) {
    if (value == null || "".equals(value)) {
      return (byte) 0;
    }
    if (value instanceof Byte byteValue) {
      return byteValue;
    }
    if (value instanceof Number number) {
      return number.byteValue();
    }
    if (value instanceof String string) {
      try {
        return Byte.valueOf(string);
      } catch (NumberFormatException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, Byte.class));
      }
    }
    if (value instanceof Character character) {
      return Short.valueOf((short) character.charValue()).byteValue();
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), Byte.class));
  }

  protected String coerceToString(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof String string) {
      return string;
    }
    if (value instanceof Enum<?> enumValue) {
      return enumValue.name();
    }
    return value.toString();
  }

  @SuppressWarnings("unchecked")
  protected <T extends Enum<T>> T coerceToEnum(Object value, Class<T> type) {
    if (value == null || "".equals(value)) {
      return null;
    }
    if (type.isInstance(value)) {
      return (T) value;
    }
    if (value instanceof String string) {
      try {
        return Enum.valueOf(type, string);
      } catch (IllegalArgumentException e) {
        throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, type));
      }
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), type));
  }

  protected Object coerceStringToType(String value, Class<?> type) {
    PropertyEditor editor = PropertyEditorManager.findEditor(type);
    if (editor == null) {
      if ("".equals(value)) {
        return null;
      }
      throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, String.class, type));
    } else {
      if ("".equals(value)) {
        try {
          editor.setAsText(value);
        } catch (IllegalArgumentException e) {
          return null;
        }
      } else {
        try {
          editor.setAsText(value);
        } catch (IllegalArgumentException e) {
          throw new ELException(LocalMessages.get(ERROR_COERCE_VALUE, value, type));
        }
      }
      return editor.getValue();
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes", "java:S3776" })
  protected Object coerceToType(Object value, Class<?> type) {
    if (type == String.class) {
      return coerceToString(value);
    }
    if (type == Long.class || type == long.class) {
      return coerceToLong(value);
    }
    if (type == Double.class || type == double.class) {
      return coerceToDouble(value);
    }
    if (type == Boolean.class || type == boolean.class) {
      return coerceToBoolean(value);
    }
    if (type == Integer.class || type == int.class) {
      return coerceToInteger(value);
    }
    if (type == Float.class || type == float.class) {
      return coerceToFloat(value);
    }
    if (type == Short.class || type == short.class) {
      return coerceToShort(value);
    }
    if (type == Byte.class || type == byte.class) {
      return coerceToByte(value);
    }
    if (type == Character.class || type == char.class) {
      return coerceToCharacter(value);
    }
    if (type == BigDecimal.class) {
      return coerceToBigDecimal(value);
    }
    if (type == BigInteger.class) {
      return coerceToBigInteger(value);
    }
    if (type.getSuperclass() == Enum.class) {
      return coerceToEnum(value, (Class<? extends Enum>) type);
    }
    if (value == null || value.getClass() == type || type.isInstance(value)) {
      return value;
    }
    if (value instanceof String string) {
      return coerceStringToType(string, type);
    }
    throw new ELException(LocalMessages.get(ERROR_COERCE_TYPE, value.getClass(), type));
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T convert(Object value, Class<T> type) throws ELException {
    return (T) coerceToType(value, type);
  }
}
