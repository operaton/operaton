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

import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionResultEntriesImpl implements DmnDecisionResultEntries {

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
   * Copies all of the mappings from the specified map to this map.
   *
   * @param values a map containing the key-value pairs to be added to this map
   */
  public void putAllValues(Map<String, TypedValue> values) {
    outputValues.putAll(values);
  }

    /**
   * Retrieves the entry with the specified name from the outputValues map.
   * 
   * @param name the key of the entry to retrieve
   * @return the entry value corresponding to the specified name
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T getEntry(String name) {
    return (T) outputValues.get(name).getValue();
  }

    /**
   * Retrieves the value associated with the specified name from the outputValues map and casts it to the generic type T.
   * 
   * @param name the name of the entry to retrieve
   * @return the value associated with the specified name, casted to type T
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T extends TypedValue> T getEntryTyped(String name) {
    return (T) outputValues.get(name);
  }

    /**
   * Returns the first entry from the outputValues map as a TypedValue object.
   * If the map is empty, returns null.
   * 
   * @return the first entry from the outputValues map as a TypedValue object, or null if the map is empty
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
   * Returns a single entry of a TypedValue object. If there is more than one entry, an exception is thrown.
   * 
   * @param <T> the type of TypedValue
   * @return the single entry TypedValue
   * @throws DecisionOutputException if there is more than one value in the output
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
   * Returns the first entry in the outputValues list if it is not empty, otherwise returns null.
   * 
   * @param <T> the type of the element to be returned
   * @return the first entry in the outputValues list or null if the list is empty
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
   * Returns a single entry from the output values, casting it to the specified type.
   * 
   * @return the single entry from the output values, or null if the list is empty
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
   * Returns a map of key-value pairs representing the output values.
   *
   * @return a map containing the keys and corresponding values from the outputValues map
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
   * Returns the map of output values with String keys and TypedValue values.
   *
   * @return the map of output values
   */
  public Map<String, TypedValue> getEntryMapTyped() {
    return outputValues;
  }

    /**
   * Returns the size of the output values list.
   *
   * @return the size of the output values list
   */
  @Override
  public int size() {
    return outputValues.size();
  }

    /**
   * Checks if the output values list is empty.
   *
   * @return true if the output values list is empty, false otherwise
   */
  @Override
  public boolean isEmpty() {
    return outputValues.isEmpty();
  }

    /**
   * Returns true if this map contains a mapping for the specified key.
   * @param key the key whose presence in this map is to be tested
   * @return true if this map contains a mapping for the specified key
   */
  @Override
  public boolean containsKey(Object key) {
    return outputValues.containsKey(key);
  }

    /**
   * Returns a set of keys from the outputValues map.
   *
   * @return a set of keys from the outputValues map
   */
  @Override
  public Set<String> keySet() {
    return outputValues.keySet();
  }

    /**
   * Returns a collection of values from the outputValues map.
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
   * Returns a string representation of the output values.
   *
   * @return a string representation of the output values
   */
  @Override
  public String toString() {
    return outputValues.toString();
  }

    /**
   * Returns true if this map contains a mapping for the specified value.
   *
   * @param value the value to check for containment in the map
   * @return true if the map contains the specified value, false otherwise
   */
  @Override
  public boolean containsValue(Object value) {
    return values().contains(value);
  }

    /**
   * Retrieves the value corresponding to the specified key from the outputValues map.
   * 
   * @param key the key whose associated value is to be returned
   * @return the value associated with the specified key, or null if no value is present
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
   * Throws an UnsupportedOperationException as decision output is immutable.
   * 
   * @param key the key of the mapping
   * @param value the value of the mapping
   * @return (does not return anything as it throws an exception)
   * @throws UnsupportedOperationException always thrown as decision output is immutable
   */
  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as decision output is immutable.
   *
   * @param key the key of the object to be removed
   * @return does not return anything
   * @throws UnsupportedOperationException always thrown
   */
  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

    /**
   * Throws UnsupportedOperationException since the decision output is immutable.
   */
  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision output is immutable and cannot be cleared.
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
    * Returns the value of the typed value, or null if the typed value is null.
    * 
    * @return the value of the typed value, or null if the typed value is null
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
     * Throws an UnsupportedOperationException with a message indicating that the decision output entry is immutable.
     *
     * @param value the value to set
     * @return N/A
     * @throws UnsupportedOperationException always
     */
    @Override
    public Object setValue(Object value) {
      throw new UnsupportedOperationException("decision output entry is immutable");
    }

  }

}
