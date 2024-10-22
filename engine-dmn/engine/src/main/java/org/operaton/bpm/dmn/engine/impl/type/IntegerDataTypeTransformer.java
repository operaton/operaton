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
package org.operaton.bpm.dmn.engine.impl.type;

import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformer;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.IntegerValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Transform values of type {@link Number} and {@link String} into {@link IntegerValue}.
 *
 * @author Philipp Ossler
 */
public class IntegerDataTypeTransformer implements DmnDataTypeTransformer {

    /**
   * Transforms the given Object value into an Integer TypedValue. If the value is a Number, it calls transformNumber method. If the value is a String, it calls transformString method. Throws IllegalArgumentException if the value is neither a Number nor a String.
   * 
   * @param value the Object value to transform
   * @return the transformed Integer TypedValue
   * @throws IllegalArgumentException if the value is neither a Number nor a String
   */
  @Override
  public TypedValue transform(Object value) throws IllegalArgumentException {
    if (value instanceof Number) {
      int intValue = transformNumber((Number) value);
      return Variables.integerValue(intValue);

    } else if (value instanceof String) {
      int intValue = transformString((String) value);
      return Variables.integerValue(intValue);

    } else {
      throw new IllegalArgumentException();
    }
  }

    /**
   * Transforms a Number object into an integer value.
   * 
   * @param value the Number object to transform
   * @return the integer value of the Number object
   * @throws IllegalArgumentException if the Number object is not an integer
   */
  protected int transformNumber(Number value) {
    if(isInteger(value)){
      return value.intValue();
    } else {
      throw new IllegalArgumentException();
    }
  }

    /**
   * Checks if the given Number value is an integer by comparing its double value with its integer value.
   * 
   * @param value the Number value to be checked
   * @return true if the value is an integer, false otherwise
   */
  protected boolean isInteger(Number value) {
    double doubleValue = value.doubleValue();
    return doubleValue == (int) doubleValue;
  }

    /**
   * Parses a string into an integer value.
   * 
   * @param value the string to be parsed
   * @return the integer value parsed from the input string
   */
  protected int transformString(String value) {
    return Integer.parseInt(value);
  }

}
