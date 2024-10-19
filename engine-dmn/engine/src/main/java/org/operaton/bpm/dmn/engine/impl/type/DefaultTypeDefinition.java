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

import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Philipp Ossler
 */
public class DefaultTypeDefinition implements DmnTypeDefinition {

    /**
   * Transforms the given object value into a TypedValue using Variables.untypedValue method.
   *
   * @param value the object value to transform
   * @return the transformed TypedValue
   * @throws IllegalArgumentException if unable to transform the object value
   */
  @Override
  public TypedValue transform(Object value) throws IllegalArgumentException {
    return Variables.untypedValue(value);
  }

    /**
   * Returns the type name as "untyped".
   *
   * @return the string "untyped"
   */
  @Override
  public String getTypeName() {
    return "untyped";
  }

    /**
   * Returns a string representation of the DefaultTypeDefinition object.
   * 
   * @return a string representation of the DefaultTypeDefinition object
   */
  @Override
  public String toString() {
    return "DefaultTypeDefinition []";
  }

    /**
   * Indicates whether some other object is "equal to" this one.
   * This method checks if the specified object is the same as the current object.
   * 
   * @param obj the object to compare with
   * @return true if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    return true;
  }

}
