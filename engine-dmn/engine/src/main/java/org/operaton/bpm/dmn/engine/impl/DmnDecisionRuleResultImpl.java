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
package org.operaton.bpm.dmn.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionRuleResultImpl implements DmnDecisionRuleResult {

  private static final long serialVersionUID = 1L;

  public static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected final Map<String, TypedValue> outputValues = new LinkedHashMap<String, TypedValue>();

    /**
   * Puts a value with the specified name into the outputValues map.
   *
   * @param name the name of the value to put
   * @param value the TypedValue to put
   */
  public void putValue(String name, TypedValue value) {
    outputValues.put(name, value);
  }

    /**
   * Copies all the key-value mappings from the specified map to this map.
   * 
   * @param values a map containing key-value mappings to be added to this map
   */
  public void putAllValues(Map<String, TypedValue> values) {
    outputValues.putAll(values);
  }

    /**
   * Retrieves the entry with the specified name from the outputValues map and returns it.
   * Suppresses unchecked warning since the method casts the retrieved value to type T.
   * 
   * @param name the name of the entry to retrieve
   * @return the entry with the specified name
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T getEntry(String name) {
    return (T) outputValues.get(name).getValue();
  }

    /**
   * Retrieves a value from the outputValues map with the given name and casts it to the specified type T.
   * 
   * @param name the name of the value to retrieve
   * @return the retrieved value casted to type T
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T extends TypedValue> T getEntryTyped(String name) {
    return (T) outputValues.get(name);
  }

    /**
   * Retrieves the first entry in the outputValues map and casts it to the specified type.
   * 
   * @return the first entry in the map as the specified type, or null if the map is empty
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T extends TypedValue> T getFirstEntryTyped() {
    if (!outputValues.isEmpty()) {
      return (T) outputValues.values().iterator().next();
    } else {
      return null;
    }
  }

    /**
   * Returns a single entry of a specific type from the output values. If there is more than one value, an exception is thrown.
   * 
   * @param <T> the type of the entry to be returned
   * @return the single entry of type T from the output values
   * @throws DecisionOutputException if there is more than one value in the output values
   */
  @Override
  public <T extends TypedValue> T getSingleEntryTyped() {
    if (outputValues.size() > 1) {
      throw LOG.decisionOutputHasMoreThanOneValue(this);
    } else {
      return getFirstEntryTyped();
    }
  }

    /**
   * Returns the first entry in the outputValues list, casted to type T.
   * If the list is empty, returns null.
   * 
   * @return the first entry in the list, or null if the list is empty
   */
  @SuppressWarnings("unchecked")
  public <T> T getFirstEntry() {
    if (!outputValues.isEmpty()) {
      return (T) getFirstEntryTyped().getValue();
    } else {
      return null;
    }
  }

    /**
   * Retrieves a single entry from the outputValues list.
   * 
   * @return the single entry as type T, or null if the list is empty
   */
  @SuppressWarnings("unchecked")
  public <T> T getSingleEntry() {
    if (!outputValues.isEmpty()) {
      return (T) getSingleEntryTyped().getValue();
    } else {
      return null;
    }
  }

    /**
   * Returns a map containing all key-value pairs from the output values.
   * The keys are taken directly from the output values, and the corresponding values
   * are retrieved by calling the get method with each key.
   * 
   * @return a map containing all key-value pairs from the output values
   */
  @Override
  public Map<String, Object> getEntryMap() {
    Map<String, Object> valueMap = new HashMap<String, Object>();

    for (String key : outputValues.keySet()) {
      valueMap.put(key, get(key));
    }

    return valueMap;
  }

    /**
   * Returns a map containing string keys and TypedValue values.
   *
   * @return the map of string keys to TypedValue values
   */
  public Map<String, TypedValue> getEntryMapTyped() {
    return outputValues;
  }

    /**
   * Returns the size of the outputValues list.
   */
  @Override
  public int size() {
    return outputValues.size();
  }

    /**
   * Returns true if the outputValues list is empty, false otherwise.
   *
   * @return true if the outputValues list is empty, false otherwise
   */
  @Override
  public boolean isEmpty() {
    return outputValues.isEmpty();
  }

    /**
   * Returns true if the specified key is contained in the map.
   * @param key the key to check for containment
   * @return true if the key is contained in the map, false otherwise
   */
  @Override
  public boolean containsKey(Object key) {
    return outputValues.containsKey(key);
  }

    /**
   * Returns a set of all keys in the outputValues map.
   *
   * @return a set of strings representing the keys in the outputValues map
   */
  @Override
  public Set<String> keySet() {
    return outputValues.keySet();
  }

    /**
   * Returns a collection of values extracted from the outputValues map.
   *
   * @return a collection of values
   */
  @Override
  public Collection<Object> values() {
    List<Object> values = new ArrayList<Object>();

    for (TypedValue typedValue : outputValues.values()) {
      values.add(typedValue.getValue());
    }

    return values;
  }

    /**
   * Returns a string representation of the output values list.
   *
   * @return a string containing the output values list
   */
  @Override
  public String toString() {
    return outputValues.toString();
  }

    /**
   * Returns true if this map contains the specified value.
   *
   * @param value the value to check for in the map
   * @return true if the map contains the specified value, otherwise false
   */
  @Override
  public boolean containsValue(Object value) {
    return values().contains(value);
  }

    /**
   * Returns the value associated with the specified key from the outputValues map.
   * 
   * @param key the key whose associated value is to be returned
   * @return the value associated with the specified key, or null if no value is associated with the key
   */
  @Override
  public Object get(Object key) {
    TypedValue typedValue = outputValues.get(key);
    if (typedValue != null) {
      return typedValue.getValue();
    } else {
      return null;
    }
  }

    /**
   * Throws UnsupportedOperationException as decision output is immutable.
   */
  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision output is immutable.
   *
   * @param key the key to be removed
   * @return nothing, as an exception is thrown
   * @throws UnsupportedOperationException always, as the decision output is immutable
   */
  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision output is immutable.
   */
  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision output is immutable
   */
  @Override
  public void clear() {
    throw new UnsupportedOperationException("decision output is immutable");
  }

    /**
   * Returns a set of entries containing the key-value pairs of the output values.
   *
   * @return a set of entries containing the key-value pairs of the output values
   */
  @Override
  public Set<Entry<String, Object>> entrySet() {
    Set<Entry<String, Object>> entrySet = new HashSet<Entry<String, Object>>();

    for (Entry<String, TypedValue> typedEntry : outputValues.entrySet()) {
      DmnDecisionRuleOutputEntry entry = new DmnDecisionRuleOutputEntry(typedEntry.getKey(), typedEntry.getValue());
      entrySet.add(entry);
    }

    return entrySet;
  }

  protected class DmnDecisionRuleOutputEntry implements Entry<String, Object> {

    protected final String key;
    protected final TypedValue typedValue;

    public DmnDecisionRuleOutputEntry(String key, TypedValue typedValue) {
      this.key = key;
      this.typedValue = typedValue;
    }

        /**
     * Returns the key associated with this object.
     *
     * @return the key
     */
    @Override
    public String getKey() {
      return key;
    }

        /**
     * Retrieves the value of the typed value, if it is not null, otherwise returns null.
     *
     * @return the value of the typed value, or null if typed value is null
     */
    @Override
    public Object getValue() {
      if (typedValue != null) {
        return typedValue.getValue();
      } else {
        return null;
      }
    }

        /**
     * Throws an UnsupportedOperationException as the decision output entry is immutable.
     */
    @Override
    public Object setValue(Object value) {
      throw new UnsupportedOperationException("decision output entry is immutable");
    }

  }

}
