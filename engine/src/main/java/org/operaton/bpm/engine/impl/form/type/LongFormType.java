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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.LongValue;
import org.operaton.bpm.engine.variable.value.TypedValue;



/**
 * @author Tom Baeyens
 */
public class LongFormType extends SimpleFormFieldType {

  public static final String TYPE_NAME = "long";

  @Override
  public String getName() {
    return TYPE_NAME;
  }

  @Override
  public TypedValue convertValue(TypedValue propertyValue) {
    if(propertyValue instanceof LongValue) {
      return propertyValue;
    }
    else {
      Object value = propertyValue.getValue();
      if(value == null) {
        return Variables.longValue(null, propertyValue.isTransient());
      }
      else if((value instanceof Number) || (value instanceof String)) {
        return Variables.longValue(Long.valueOf(value.toString()), propertyValue.isTransient());
      }
      else {
        throw new ProcessEngineException("Value '%s' is not of type Long.".formatted(value));
      }
    }
  }

  // deprecated ////////////////////////////////////////////

  @Override
  public Object convertFormValueToModelValue(Object propertyValue) {
    if (propertyValue==null || "".equals(propertyValue)) {
      return null;
    }
    return Long.valueOf(propertyValue.toString());
  }

  @Override
  public String convertModelValueToFormValue(Object modelValue) {
    if (modelValue==null) {
      return null;
    }
    return modelValue.toString();
  }


}
