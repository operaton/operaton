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
package org.operaton.bpm.client.variable.impl.mapper;

import org.operaton.bpm.client.variable.impl.AbstractTypedValueMapper;
import org.operaton.bpm.client.variable.impl.TypedValueField;
import org.operaton.bpm.engine.variable.type.PrimitiveValueType;
import org.operaton.bpm.engine.variable.value.PrimitiveValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

public abstract class PrimitiveValueMapper<T extends PrimitiveValue<?>> extends AbstractTypedValueMapper<T> {

  protected PrimitiveValueMapper(PrimitiveValueType variableType) {
    super(variableType);
  }

  @Override
  public T readValue(TypedValueField typedValueField, boolean deserializeObjectValue) {
    return readValue(typedValueField);
  }

  public abstract T readValue(TypedValueField typedValueField);

  @Override
  public PrimitiveValueType getType() {
    return (PrimitiveValueType) super.getType();
  }

  protected boolean isAssignable(Object value) {
    Class<?> javaType = getType().getJavaType();
    return javaType.isAssignableFrom(value.getClass());
  }

  protected boolean canWriteValue(TypedValue typedValue) {
    Object value = typedValue.getValue();
    return value == null || isAssignable(value);
  }

  protected boolean canReadValue(TypedValueField typedValueField) {
    Object value = typedValueField.getValue();
    return value == null || isAssignable(value);
  }

}
