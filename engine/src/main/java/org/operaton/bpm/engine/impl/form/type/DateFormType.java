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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;


/**
 * @author Tom Baeyens
 */
public class DateFormType extends AbstractFormFieldType {

  public static final String TYPE_NAME = "date";

  protected String datePattern;
  protected DateFormat dateFormat;

  public DateFormType(String datePattern) {
    this.datePattern = datePattern;
    this.dateFormat = new SimpleDateFormat(datePattern);
  }

  @Override
  public String getName() {
    return TYPE_NAME;
  }

  @Override
  public Object getInformation(String key) {
    if ("datePattern".equals(key)) {
      return datePattern;
    }
    return null;
  }

  @Override
  public TypedValue convertToModelValue(TypedValue propertyValue) {
    Object value = propertyValue.getValue();
    if(value == null) {
      return Variables.dateValue(null, propertyValue.isTransient());
    }
    else if(value instanceof Date date) {
      return Variables.dateValue(date, propertyValue.isTransient());
    }
    else if(value instanceof String string) {
      String strValue = string.trim();
      if (strValue.isEmpty()) {
        return Variables.dateValue(null, propertyValue.isTransient());
      }
      try {
        return Variables.dateValue((Date) dateFormat.parseObject(strValue), propertyValue.isTransient());
      } catch (ParseException e) {
        throw new ProcessEngineException("Could not parse value '%s' as date using date format '%s'.".formatted(value, datePattern));
      }
    }
    else {
      throw new ProcessEngineException("Value '%s' cannot be transformed into a Date.".formatted(value));
    }
  }

  @Override
  public TypedValue convertToFormValue(TypedValue modelValue) {
    if(modelValue.getValue() == null) {
      return Variables.stringValue("", modelValue.isTransient());
    } else if(modelValue.getType() == ValueType.DATE) {
      return Variables.stringValue(dateFormat.format(modelValue.getValue()), modelValue.isTransient());
    }
    else {
      throw new ProcessEngineException("Expected value to be of type '%s' but got '%s'.".formatted(ValueType.DATE, modelValue.getType()));
    }
  }

  // deprecated //////////////////////////////////////////////////////////

  @Override
  public Object convertFormValueToModelValue(Object propertyValue) {
    if (propertyValue==null || "".equals(propertyValue)) {
      return null;
    }
    try {
      return dateFormat.parseObject(propertyValue.toString());
    } catch (ParseException e) {
      throw new ProcessEngineException("invalid date value "+propertyValue);
    }
  }

  @Override
  public String convertModelValueToFormValue(Object modelValue) {
    if (modelValue==null) {
      return null;
    }
    return dateFormat.format(modelValue);
  }

}
