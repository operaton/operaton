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

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import org.operaton.bpm.dmn.engine.impl.DmnEngineLogger;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnTypeDefinitionImpl implements DmnTypeDefinition {

  protected static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected String typeName;
  protected DmnDataTypeTransformer transformer;

  public DmnTypeDefinitionImpl(String typeName, DmnDataTypeTransformer transformer) {
    this.typeName = typeName;
    this.transformer = transformer;
  }

    /**
   * Transforms a given object value to a TypedValue. If the value is null, 
   * returns an untypedNullValue TypedValue. Otherwise, calls the transformNotNullValue method 
   * to transform the non-null value to a TypedValue.
   *
   * @param value the object value to transform
   * @return the transformed TypedValue
   */
  @Override
  public TypedValue transform(Object value) {
    if (value == null) {
      return Variables.untypedNullValue();
    } else {
      return transformNotNullValue(value);
    }
  }

    /**
   * Transforms a non-null value using the provided transformer.
   *
   * @param value the non-null value to transform
   * @return the transformed value
   * @throws InvalidValueException if the transformation fails
   */
  protected TypedValue transformNotNullValue(Object value) {
    ensureNotNull("transformer", transformer);

    try {

      return transformer.transform(value);

    } catch (IllegalArgumentException e) {
      throw LOG.invalidValueForTypeDefinition(typeName, value);
    }
  }

    /**
   * Returns the type name.
   *
   * @return the type name
   */
  public String getTypeName() {
    return typeName;
  }

    /**
   * Sets the type name for the object.
   *
   * @param typeName the new type name to be set
   */
  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

    /**
   * Sets the DMN data type transformer to be used.
   * 
   * @param transformer the DMN data type transformer to set
   */
  public void setTransformer(DmnDataTypeTransformer transformer) {
    this.transformer = transformer;
  }

    /**
   * Returns a string representation of the DmnTypeDefinitionImpl object, 
   * including the type name.
   */
  @Override
  public String toString() {
    return "DmnTypeDefinitionImpl{" +
      "typeName='" + typeName + '\'' +
      '}';
  }

}
