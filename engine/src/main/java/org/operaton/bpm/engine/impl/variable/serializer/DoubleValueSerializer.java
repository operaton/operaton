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

import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.DoubleValue;

/**
 *
 * @author Daniel Meyer
 */
public class DoubleValueSerializer extends PrimitiveValueSerializer<DoubleValue> {

  public DoubleValueSerializer() {
    super(ValueType.DOUBLE);
  }

  @Override
  public DoubleValue convertToTypedValue(UntypedValueImpl untypedValue) {
    return Variables.doubleValue((Double) untypedValue.getValue(), untypedValue.isTransient());
  }

  public void writeValue(DoubleValue value, ValueFields valueFields) {
    valueFields.setDoubleValue(value.getValue());
  }

  @Override
  public DoubleValue readValue(ValueFields valueFields, boolean asTransientValue) {
    return Variables.doubleValue(valueFields.getDoubleValue(), asTransientValue);
  }

}
