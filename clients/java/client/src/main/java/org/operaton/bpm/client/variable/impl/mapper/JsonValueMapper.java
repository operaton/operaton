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

import org.operaton.bpm.client.variable.ClientValues;
import org.operaton.bpm.client.variable.impl.TypedValueField;
import org.operaton.bpm.client.variable.value.JsonValue;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;

public class JsonValueMapper extends PrimitiveValueMapper<JsonValue> {

  public JsonValueMapper() {
    super(ClientValues.JSON);
  }

  @Override
  public JsonValue convertToTypedValue(UntypedValueImpl untypedValue) {
    return ClientValues.jsonValue((String) untypedValue.getValue());
  }

  public void writeValue(JsonValue jsonValue, TypedValueField typedValueField) {
    typedValueField.setValue(jsonValue.getValue());
  }

  @Override
  public JsonValue readValue(TypedValueField typedValueField) {
    return ClientValues.jsonValue((String) typedValueField.getValue());
  }

}
