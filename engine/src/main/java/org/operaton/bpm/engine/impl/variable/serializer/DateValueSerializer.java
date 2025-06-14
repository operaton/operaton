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
package org.operaton.bpm.engine.impl.variable.serializer;

import java.util.Date;

import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.DateValue;


/**
 * Serializes Dates as long values
 *
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class DateValueSerializer extends PrimitiveValueSerializer<DateValue> {

  public DateValueSerializer() {
    super(ValueType.DATE);
  }

  @Override
  public DateValue convertToTypedValue(UntypedValueImpl untypedValue) {
    return Variables.dateValue((Date) untypedValue.getValue(), untypedValue.isTransient());
  }

  @Override
  public DateValue readValue(ValueFields valueFields, boolean asTransientValue) {
    Long longValue = valueFields.getLongValue();
    Date dateValue = null;
    if (longValue!=null) {
      dateValue = new Date(longValue);
    }
    return Variables.dateValue(dateValue, asTransientValue);
  }

  public void writeValue(DateValue typedValue, ValueFields valueFields) {
    Date dateValue = typedValue.getValue();
    if (dateValue != null) {
      valueFields.setLongValue(dateValue.getTime());
    } else {
      valueFields.setLongValue(null);
    }
  }

}
