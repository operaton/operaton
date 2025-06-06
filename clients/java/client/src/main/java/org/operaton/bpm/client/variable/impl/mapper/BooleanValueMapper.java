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
package org.operaton.bpm.client.variable.impl.mapper;

import org.operaton.bpm.client.variable.impl.TypedValueField;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.BooleanValue;

public class BooleanValueMapper extends PrimitiveValueMapper<BooleanValue> {

  public BooleanValueMapper() {
    super(ValueType.BOOLEAN);
  }

  public BooleanValue convertToTypedValue(UntypedValueImpl untypedValue) {
    return Variables.booleanValue((Boolean) untypedValue.getValue());
  }

  @Override
  public BooleanValue readValue(TypedValueField typedValueField) {
    Boolean boolValue = (Boolean) typedValueField.getValue();
    return Variables.booleanValue(boolValue);
  }

  public void writeValue(BooleanValue booleanValue, TypedValueField typedValueField) {
    Boolean boolValue = booleanValue.getValue();
    typedValueField.setValue(boolValue);
  }

}
