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
package org.operaton.bpm.engine.impl.form.type;

import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;


/**
 * @author Tom Baeyens
 */
public class EnumFormType extends SimpleFormFieldType {

  public static final String TYPE_NAME = "enum";

  protected Map<String, String> values;

  public EnumFormType(Map<String, String> values) {
    this.values = values;
  }

  @Override
  public String getName() {
    return TYPE_NAME;
  }

  @Override
  public Object getInformation(String key) {
    if ("values".equals(key)) {
      return values;
    }
    return null;
  }

  @Override
  public TypedValue convertValue(TypedValue propertyValue) {
    Object value = propertyValue.getValue();
    if(value == null || value instanceof String) {
      validateValue(value);
      return Variables.stringValue((String) value, propertyValue.isTransient());
    }
    else {
      throw new ProcessEngineException("Value '%s' is not of type String.".formatted(value));
    }
  }

  protected void validateValue(Object value) {
    if(value != null && values != null && !values.containsKey(value)) {
      throw new ProcessEngineException("Invalid value for enum form property: %s".formatted(value));
    }
  }

  public Map<String, String> getValues() {
    return values;
  }

  // ////////////////// deprecated ////////////////////////////////////////

  @Override
  public Object convertFormValueToModelValue(Object propertyValue) {
    validateValue(propertyValue);
    return propertyValue;
  }

  @Override
  public String convertModelValueToFormValue(Object modelValue) {
    if(modelValue != null) {
      if(!(modelValue instanceof String)) {
        throw new ProcessEngineException("Model value should be a String");
      }
      validateValue(modelValue);
    }
    return (String) modelValue;
  }

}
