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

  @Override
  public TypedValue transform(Object value) throws IllegalArgumentException {
    if (value instanceof Number numberValue) {
      int intValue = transformNumber(numberValue);
      return Variables.integerValue(intValue);

    } else if (value instanceof String stringValue) {
      int intValue = transformString(stringValue);
      return Variables.integerValue(intValue);

    } else {
      throw new IllegalArgumentException();
    }
  }

  protected int transformNumber(Number value) {
    if(isInteger(value)){
      return value.intValue();
    } else {
      throw new IllegalArgumentException();
    }
  }

  protected boolean isInteger(Number value) {
    double doubleValue = value.doubleValue();
    return doubleValue == (int) doubleValue;
  }

  protected int transformString(String value) {
    return Integer.parseInt(value);
  }

}
