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
 * Enhances the Bindings with the variables resolvable through the {@link VariableContext}.
 * The variables are treated as read only: all mutating operations write through to the
 * wrapped {@link Bindings}.
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
  public boolean containsKey(Object key) {
    if(wrappedBindings.containsKey(key)) {
      return true;
    }
    if (key instanceof String) {
      return variableContext.containsVariable((String) key);
    }
    else {
      return false;
    }
  }

  /**
   * Dedicated implementation which does not fall back on the {@link #calculateBindingMap()} for performance reasons
   */
  public Object get(Object key) {
    Object result = null;

    if(wrappedBindings.containsKey(key)) {
      result = wrappedBindings.get(key);
    }
    else {
      if (key instanceof String) {
        TypedValue resolvedValue = variableContext.resolve((String) key);
        result = unpack(resolvedValue);
      }
    }

    return result;
  }

  /**
   * Dedicated implementation which does not fall back on the {@link #calculateBindingMap()} for performance reasons
   */
  public Object put(String name, Object value) {
    // only write to the wrapped bindings
    return wrappedBindings.put(name, value);
  }

    /**
   * Returns a set view of the mappings contained in this object's binding map.
   *
   * @return a set view of the mappings contained in this object's binding map
   */
  public Set<Entry<String, Object>> entrySet() {
    return calculateBindingMap().entrySet();
  }

    /**
   * Returns a set containing all the keys in the binding map.
   *
   * @return a set of strings representing the keys in the binding map
   */
  public Set<String> keySet() {
    return calculateBindingMap().keySet();
  }

    /**
   * Returns the size of the binding map calculated by the method calculateBindingMap.
   */
  public int size() {
    return calculateBindingMap().size();
  }

    /**
   * Returns a collection of values from the binding map.
   *
   * @return a collection of values from the binding map
   */
  public Collection<Object> values() {
    return calculateBindingMap().values();
  }

    /**
   * Copies all of the mappings from the specified map to this map.
   * 
   * @param toMerge the map containing key-value pairs to be merged with this map
   */
  public void putAll(Map< ? extends String, ?> toMerge) {
    for (Entry<? extends String, ?> entry : toMerge.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

    /**
   * Removes the mapping for a key from this map if it is present.
   * 
   * @param key the key whose mapping is to be removed from the map
   * @return the previous value associated with key, or null if there was no mapping for key
   */
  public Object remove(Object key) {
    return wrappedBindings.remove(key);
  }

    /**
   * Clears the wrapped bindings.
   */
  public void clear() {
    wrappedBindings.clear();
  }

    /**
   * Returns true if the calculated binding map contains the specified value.
   * 
   * @param value the value to check for
   * @return true if the value is found in the calculated binding map, false otherwise
   */
  public boolean containsValue(Object value) {
    return calculateBindingMap().containsValue(value);
  }

    /**
  * Checks if the binding map is empty.
  * 
  * @return true if the binding map is empty, false otherwise
  */
  public boolean isEmpty() {
    return calculateBindingMap().isEmpty();
  }

    /**
   * Calculates the binding map by iterating over the variable context and wrapped bindings,
   * unpacking the values and adding them to the map.
   * 
   * @return the binding map containing variable names and their corresponding values
   */
  protected Map<String, Object> calculateBindingMap() {

    /**
   * Unpacks the value from a TypedValue object if it is not null.
   *
   * @param resolvedValue the TypedValue object to unpack
   * @return the unpacked value if resolvedValue is not null, otherwise returns null
   */
  protected Object unpack(TypedValue resolvedValue) {
    if(resolvedValue != null) {
      return resolvedValue.getValue();
    }
    return null;
  }

    /**
   * Wraps the given bindings and variable context into a new VariableContextScriptBindings object.
   * 
   * @param wrappedBindings the bindings to wrap
   * @param variableContext the variable context to wrap
   * @return a new VariableContextScriptBindings object with the given bindings and variable context
   */
  public static VariableContextScriptBindings wrap(Bindings wrappedBindings, VariableContext variableContext) {
    return new VariableContextScriptBindings(wrappedBindings, variableContext);
  }

}
