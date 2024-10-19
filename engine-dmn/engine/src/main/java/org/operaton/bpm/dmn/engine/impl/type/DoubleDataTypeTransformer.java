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
import org.operaton.bpm.engine.variable.value.DoubleValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Transform values of type {@link Number} and {@link String} into {@link DoubleValue}.
 *
 * @author Philipp Ossler
 */
public class DoubleDataTypeTransformer implements DmnDataTypeTransformer {

    /**
   * Transforms the input value to a TypedValue of type double.
   *
   * @param value the input value to transform
   * @return a TypedValue containing the transformed double value
   * @throws IllegalArgumentException if the input value is not a Number or a String
   */
  @Override
  public TypedValue transform(Object value) throws IllegalArgumentException {
    if (value instanceof Number) {
      double doubleValue = transformNumber((Number) value);
      return Variables.doubleValue(doubleValue);

    } else if (value instanceof String) {
      double doubleValue = transformString((String) value);
      return Variables.doubleValue(doubleValue);

    } else {
      throw new IllegalArgumentException();
    }
  }

    /**
   * Converts a Number to a double value.
   * 
   * @param value the Number to be transformed
   * @return the double value of the input Number
   */
  protected double transformNumber(Number value) {
    return value.doubleValue();
  }

    /**
   * Parses a string value into a double.
   * 
   * @param value the string value to be parsed
   * @return the double value parsed from the string
   */
  protected double transformString(String value) {
    return Double.parseDouble(value);
  }

}
