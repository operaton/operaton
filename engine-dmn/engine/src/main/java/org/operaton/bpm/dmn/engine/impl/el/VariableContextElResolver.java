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
package org.operaton.bpm.dmn.engine.impl.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import org.operaton.bpm.impl.juel.jakarta.el.ELContext;
import org.operaton.bpm.impl.juel.jakarta.el.ELResolver;

import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
public class VariableContextElResolver extends ELResolver {

  public static final String VARIABLE_CONTEXT_KEY = "variableContext";

    /**
   * Retrieves the value of a property from a VariableContext object in the ELContext.
   * If the base object is null, it checks if the property is the VariableContext key 
   * and returns the VariableContext object if it matches. Otherwise, it resolves the 
   * property using the VariableContext and returns the unpacked value.
   *
   * @param context the ELContext
   * @param base the base object
   * @param property the property to retrieve the value of
   * @return the value of the property, or null if not found
   */
  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    if (base == null) {
      VariableContext variableContext = (VariableContext) context.getContext(VariableContext.class);
      if(variableContext != null) {
        if(VARIABLE_CONTEXT_KEY.equals(property)) {
          context.setPropertyResolved(true);
          return variableContext;
        }
        TypedValue typedValue = variableContext.resolve((String) property);
        if(typedValue != null) {
          context.setPropertyResolved(true);
          return unpack(typedValue);
        }
      }
    }
    return null;
  }

    /**
   * This method sets the value of a property in the given EL context. It is read-only.
   *
   * @param context the EL context
   * @param base the base object on which the property is being set
   * @param property the property to set
   * @param value the value to set the property to
   */
  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    // read only
  }

    /**
  * Returns true to indicate that the property is always read-only.
  *
  * @param context the ELContext object
  * @param base the base object
  * @param property the property object
  * @return true indicating the property is read-only
  */
  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    // always read only
    return true;
  }

    /**
   * Returns the common property type for the given EL context and object.
   *
   * @param arg0 the EL context
   * @param arg1 the object
   * @return the common property type as a Class object
   */
  public Class<?> getCommonPropertyType(ELContext arg0, Object arg1) {
      return Object.class;
  }

    /**
   * Retrieves the feature descriptors from the provided ELContext and object.
   *
   * @param arg0 the ELContext to retrieve feature descriptors from
   * @param arg1 the object to retrieve feature descriptors for
   * @return an Iterator of FeatureDescriptors retrieved from the ELContext and object
   */
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1) {
    return null;
  }

    /**
   * Returns the class type of Object.
   * 
   * @param arg0 the EL context
   * @param arg1 the first object
   * @param arg2 the second object
   * @return the class type of Object
   */
  public Class<?> getType(ELContext arg0, Object arg1, Object arg2) {
      return Object.class;
  }

    /**
   * Unpacks the value from a TypedValue object and returns it.
   * 
   * @param typedValue the TypedValue object to unpack
   * @return the unpacked value or null if the TypedValue is null
   */
  protected Object unpack(TypedValue typedValue) {
    if(typedValue != null) {
      return typedValue.getValue();
    }
    return null;
  }

}
