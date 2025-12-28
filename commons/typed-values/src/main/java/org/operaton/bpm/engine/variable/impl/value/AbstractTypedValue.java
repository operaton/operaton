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
package org.operaton.bpm.engine.variable.impl.value;

import java.io.Serial;

import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
public class AbstractTypedValue<T> implements TypedValue {

  @Serial private static final long serialVersionUID = 1L;

  protected T value;

  protected ValueType type;

  protected boolean isTransient;

  public AbstractTypedValue(T value, ValueType type) {
    this.value = value;
    this.type = type;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public ValueType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "Value '%s' of type '%s', isTransient=%s".formatted(value, type, isTransient);
  }

  @Override
  public boolean isTransient() {
    return isTransient;
  }

  public void setTransient(boolean isTransient) {
    this.isTransient = isTransient;
  }

}
