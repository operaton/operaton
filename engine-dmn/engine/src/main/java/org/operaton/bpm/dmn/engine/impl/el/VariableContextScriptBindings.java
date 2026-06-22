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
package org.operaton.bpm.dmn.engine.impl.el;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * A Script {@link Bindings} implementation wrapping a provided
 * {@link VariableContext} and {@link Bindings} instance.
 *
 * <p>
 * Enhances the Bindings with the variables resolvable through the {@link VariableContext}.
 * The variables are treated as read only: all mutating operations write through to the
 * wrapped {@link Bindings}.
 * </p>
 *
 * @author Daniel Meyer
 *
 */
public class VariableContextScriptBindings implements Bindings {

  protected Bindings wrappedBindings;

  protected VariableContext variableContext;

  public VariableContextScriptBindings(Bindings wrappedBindings, VariableContext variableContext) {
    this.wrappedBindings = wrappedBindings;
    this.variableContext = variableContext;
  }

  /**
   * Dedicated implementation which does not fall back on the {@link #calculateBindingMap()} for performance reasons
   */
  @Override
  public boolean containsKey(Object key) {
    if(wrappedBindings.containsKey(key)) {
      return true;
    }
    if (key instanceof String keyString) {
      return variableContext.containsVariable(keyString);
    }
    else {
      return false;
    }
  }

  /**
   * Dedicated implementation which does not fall back on the {@link #calculateBindingMap()} for performance reasons
   */
  @Override
  public Object get(Object key) {
    Object result = null;

    if(wrappedBindings.containsKey(key)) {
      result = wrappedBindings.get(key);
    }
    else {
      if (key instanceof String keyString) {
        TypedValue resolvedValue = variableContext.resolve(keyString);
        result = unpack(resolvedValue);
      }
    }

    return result;
  }

  /**
   * Dedicated implementation which does not fall back on the {@link #calculateBindingMap()} for performance reasons
   */
  @Override
  public Object put(String name, Object value) {
    // only write to the wrapped bindings
    return wrappedBindings.put(name, value);
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return calculateBindingMap().entrySet();
  }

  @Override
  public Set<String> keySet() {
    return calculateBindingMap().keySet();
  }

  @Override
  public int size() {
    return calculateBindingMap().size();
  }

  @Override
  public Collection<Object> values() {
    return calculateBindingMap().values();
  }

  public void putAll(Map< ? extends String, ?> toMerge) {
    for (var entry : toMerge.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Object remove(Object key) {
    return wrappedBindings.remove(key);
  }

  @Override
  public void clear() {
    wrappedBindings.clear();
  }

  @Override
  public boolean containsValue(Object value) {
    return calculateBindingMap().containsValue(value);
  }

  @Override
  public boolean isEmpty() {
    return calculateBindingMap().isEmpty();
  }

  protected Map<String, Object> calculateBindingMap() {

    Map<String, Object> bindingMap = new HashMap<>();

    Set<String> keySet = variableContext.keySet();
    for (String variableName : keySet) {
      bindingMap.put(variableName, unpack(variableContext.resolve(variableName)));
    }

    Set<Entry<String, Object>> wrappedBindingsEntries = wrappedBindings.entrySet();
    for (Entry<String, Object> entry : wrappedBindingsEntries) {
      bindingMap.put(entry.getKey(), entry.getValue());
    }

    return bindingMap;
  }

  protected Object unpack(TypedValue resolvedValue) {
    if(resolvedValue != null) {
      return resolvedValue.getValue();
    }
    return null;
  }

  public static VariableContextScriptBindings wrap(Bindings wrappedBindings, VariableContext variableContext) {
    return new VariableContextScriptBindings(wrappedBindings, variableContext);
  }

}
