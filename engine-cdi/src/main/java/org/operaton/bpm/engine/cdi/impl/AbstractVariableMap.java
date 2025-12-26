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
package org.operaton.bpm.engine.cdi.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

abstract class AbstractVariableMap implements VariableMap {

  @Inject
  protected BusinessProcess businessProcess;

  protected abstract Object getVariable(String variableName);
  protected abstract <T extends TypedValue> T getVariableTyped(String variableName);

  protected abstract void setVariable(String variableName, Object value);

  @Override
  public Object get(Object key) {
    if(key == null) {
      throw new IllegalArgumentException("This map does not support 'null' keys.");
    }
    return getVariable(key.toString());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getValue(String name, Class<T> type) {
    Object object = get(name);
    if (object == null) {
      return null;
    } else if (type.isAssignableFrom(object.getClass())) {
      return (T) object;
    } else {
      throw new ClassCastException("Cannot cast variable named '%s' with value '%s' to type '%s'.".formatted(name, object, type));
    }
  }

  @Override
  public <T extends TypedValue> T getValueTyped(String name) {
    if (name == null) {
      throw new IllegalArgumentException("This map does not support 'null' keys.");
    }
    return getVariableTyped(name);
  }

  @Override
  public Object put(String key, Object value) {
    if(key == null) {
      throw new IllegalArgumentException("This map does not support 'null' keys.");
    }
    Object variableBefore = getVariable(key);
    setVariable(key, value);
    return variableBefore;
  }

  @Override
  public void putAll(Map< ? extends String, ? extends Object> m) {
    for (var newEntry : m.entrySet()) {
      setVariable(newEntry.getKey(), newEntry.getValue());
    }
  }

  @Override
  public VariableMap putValue(String name, Object value) {
    put(name, value);
    return this;
  }

  @Override
  public VariableMap putValueTyped(String name, TypedValue value) {
    if(name == null) {
      throw new IllegalArgumentException("This map does not support 'null' names.");
    }
    setVariable(name, value);
    return this;
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException(getClass().getName()+".size() is not supported.");
  }

  @Override
  public boolean isEmpty() {
    throw new UnsupportedOperationException(getClass().getName()+".isEmpty() is not supported.");
  }

  @Override
  public boolean containsKey(Object key) {
    throw new UnsupportedOperationException(getClass().getName()+".containsKey() is not supported.");
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException(getClass().getName()+".containsValue() is not supported.");
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("%s.remove is unsupported. Use %s.put(key, null)".formatted(getClass().getName(), getClass().getName()));
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(getClass().getName()+".clear() is not supported.");
  }

  @Override
  public Set<String> keySet() {
    throw new UnsupportedOperationException(getClass().getName()+".keySet() is not supported.");
  }

  @Override
  public Collection<Object> values() {
    throw new UnsupportedOperationException(getClass().getName()+".values() is not supported.");
  }

  @Override
  public Set<java.util.Map.Entry<String, Object>> entrySet() {
    throw new UnsupportedOperationException(getClass().getName()+".entrySet() is not supported.");
  }

  @Override
  public VariableContext asVariableContext() {
    throw new UnsupportedOperationException(getClass().getName()+".asVariableContext() is not supported.");
  }

}
