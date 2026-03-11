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

import java.util.Set;

import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 *
 * @author Daniel Meyer
 */
public abstract class AbstractTypedValueSerializer<T extends TypedValue> implements TypedValueSerializer<T> {

  public static final Set<String> BINARY_VALUE_TYPES = Set.of(ValueType.BYTES.getName(),ValueType.FILE.getName());

  protected ValueType valueType;

  protected AbstractTypedValueSerializer(ValueType type) {
    valueType = type;
  }

  @Override
  public ValueType getType() {
    return valueType;
  }

  @Override
  public String getSerializationDataformat() {
    // default implementation returns null
    return null;
  }

  @Override
  public boolean canHandle(TypedValue value) {
    if(value.getType() != null && !valueType.getClass().isAssignableFrom(value.getType().getClass())) {
      return false;
    }
    else {
      return canWriteValue(value);
    }
  }

  protected abstract boolean canWriteValue(TypedValue value);

  @Override
  public boolean isMutableValue(T typedValue) {
    // default
    return false;
  }

}
