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
import org.operaton.bpm.engine.variable.value.BooleanValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Transform values of type {@link String} into {@link BooleanValue}.
 *
 * @author Philipp Ossler
 */
public class BooleanDataTypeTransformer implements DmnDataTypeTransformer {

    /**
   * Transforms the given value into a TypedValue representing a boolean.
   * If the value is a Boolean, it returns the boolean value as a TypedValue.
   * If the value is a String, it transforms the string into a boolean value 
   * and returns it as a TypedValue.
   * Throws an IllegalArgumentException if the value is neither a Boolean nor a String.
   *
   * @param value the value to transform into a boolean TypedValue
   * @return a TypedValue representing a boolean value
   * @throws IllegalArgumentException if the value is not a Boolean or a String
   */
  @Override
  public TypedValue transform(Object value) throws IllegalArgumentException {
    if (value instanceof Boolean) {
      return Variables.booleanValue((Boolean) value);

    } else if (value instanceof String) {
      boolean booleanValue = transformString((String) value);
      return Variables.booleanValue(booleanValue);

    } else {
      throw new IllegalArgumentException();
    }
  }

    /**
   * Converts a string value to a boolean value by checking if it equals "true" or "false".
   * Throws an IllegalArgumentException if the input is neither "true" nor "false".
   * 
   * @param value the string value to be transformed
   * @return true if the value is "true", false if the value is "false"
   * @throws IllegalArgumentException if the value is neither "true" nor "false"
   */
  protected boolean transformString(String value) {
    if (value.equalsIgnoreCase("true")) {
      return true;
    } else if (value.equalsIgnoreCase("false")) {
      return false;
    } else {
      throw new IllegalArgumentException();
    }
  }

}
