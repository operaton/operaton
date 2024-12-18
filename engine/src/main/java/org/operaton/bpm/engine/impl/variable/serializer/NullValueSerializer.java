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
package org.operaton.bpm.engine.impl.variable.serializer;

import org.operaton.bpm.engine.variable.impl.value.NullValueImpl;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Used to serialize untyped null values.
 *
 * @author Daniel Meyer
 * @author Tom Baeyens
 */
public class NullValueSerializer extends AbstractTypedValueSerializer<NullValueImpl> {

  public NullValueSerializer() {
    super(ValueType.NULL);
  }

  @Override
  public String getName() {
    return ValueType.NULL.getName().toLowerCase();
  }

  @Override
  public NullValueImpl convertToTypedValue(UntypedValueImpl untypedValue) {
    return !untypedValue.isTransient() ? NullValueImpl.INSTANCE : NullValueImpl.INSTANCE_TRANSIENT;
  }

  public void writeValue(NullValueImpl value, ValueFields valueFields) {
    // nothing to do
  }

  @Override
  public NullValueImpl readValue(ValueFields valueFields, boolean deserialize, boolean asTransientValue) {
    return NullValueImpl.INSTANCE;
  }

  @Override
  protected boolean canWriteValue(TypedValue value) {
    return value.getValue() == null;
  }

}
