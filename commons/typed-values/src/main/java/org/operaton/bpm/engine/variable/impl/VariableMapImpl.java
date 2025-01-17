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
package org.operaton.bpm.engine.variable.impl;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
public class VariableMapImpl implements VariableMap, Serializable, VariableContext {

  private static final long serialVersionUID = 1L;

  protected Map<String, TypedValue> variables = new HashMap<>();

  public VariableMapImpl(VariableMapImpl map) {
    variables = new HashMap<>(map.variables);
  }

  public VariableMapImpl(Map<String, Object> map) {
    if(map != null) {
      putAll(map);
    }
  }

  public VariableMapImpl() {
  }

  // VariableMap implementation //////////////////////////////

  @Override
  public VariableMap putValue(String name, Object value) {
    put(name, value);
    return this;
  }

  @Override
  public VariableMap putValueTyped(String name, TypedValue value) {
    variables.put(name, value);
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getValue(String name, Class<T> type) {
    Object object = get(name);
    if(object == null) {
      return null;
    }
    else if (type.isAssignableFrom(object.getClass())) {
      return (T) object;

    } else {
      throw new ClassCastException("Cannot cast variable named '"+name+"' with value '"+object+"' to type '"+type+"'.");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends TypedValue> T getValueTyped(String name) {
    return (T) variables.get(name);
  }

  // java.uitil Map<String, Object> implementation ////////////////////////////////////////

  @Override
  public int size() {
    return variables.size();
  }

  @Override
  public boolean isEmpty() {
    return variables.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return variables.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    for (TypedValue varValue : variables.values()) {
      if(value == varValue.getValue()) {
        return true;
      } else if(value != null && value.equals(varValue.getValue())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Object get(Object key) {
    TypedValue typedValue = variables.get(key);

    if(typedValue != null) {
      return typedValue.getValue();
    }
    else {
      return null;
    }
  }

  @Override
  public Object put(String key, Object value) {

    TypedValue typedValue = Variables.untypedValue(value);

    TypedValue prevValue = variables.put(key, typedValue);

    if(prevValue != null) {
      return prevValue.getValue();
    }
    else {
      return null;
    }
  }

  @Override
  public Object remove(Object key) {
    TypedValue prevValue = variables.remove(key);

    if(prevValue != null) {
      return prevValue.getValue();
    }
    else {
      return null;
    }
  }

  public void putAll(Map<? extends String, ? extends Object> m) {
    if(m != null) {
      if(m instanceof VariableMapImpl variableMapImpl) {
        variables.putAll(variableMapImpl.variables);
      }
      else {
        for (var entry : m.entrySet()) {
          put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  @Override
  public void clear() {
    variables.clear();
  }

  @Override
  public Set<String> keySet() {
    return variables.keySet();
  }

  @Override
  public Collection<Object> values() {

    // NOTE: cannot naively return List of values here. A proper implementation must return a
    // Collection which is backed by the actual variable map

    return new AbstractCollection<>() {

      @Override
      public Iterator<Object> iterator() {

        // wrapped iterator. Must be local to the iterator() method
        final Iterator<TypedValue> iterator = variables.values().iterator();

        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public Object next() {
            return iterator.next().getValue();
          }

          @Override
          public void remove() {
            iterator.remove();
          }
        };
      }

      @Override
      public int size() {
        return variables.size();
      }

    };
  }

  @Override
  public Set<java.util.Map.Entry<String, Object>> entrySet() {

    // NOTE: cannot naively return Set of entries here. A proper implementation must
    // return a Set which is backed by the actual map

    return new AbstractSet<>() {

      @Override
      public Iterator<java.util.Map.Entry<String, Object>> iterator() {

        return new Iterator<>() {

          // wrapped iterator. Must be local to the iterator() method
          final Iterator<java.util.Map.Entry<String, TypedValue>> iterator = variables.entrySet().iterator();

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public java.util.Map.Entry<String, Object> next() {

            final java.util.Map.Entry<String, TypedValue> underlyingEntry = iterator.next();

            // return wrapper backed by the underlying entry
            return new Entry<>() {
              @Override
              public String getKey() {
                return underlyingEntry.getKey();
              }

              @Override
              public Object getValue() {
                return underlyingEntry.getValue().getValue();
              }

              @Override
              public Object setValue(Object value) {
                TypedValue typedValue = Variables.untypedValue(value);
                return underlyingEntry.setValue(typedValue);
              }

              @Override
              public final boolean equals(Object o) {
                if (!(o instanceof Map.Entry))
                  return false;
                Entry e = (Entry) o;
                Object k1 = getKey();
                Object k2 = e.getKey();
                if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                  Object v1 = getValue();
                  Object v2 = e.getValue();
                  if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
                }
                return false;
              }

              @Override
              public final int hashCode() {
                String key = getKey();
                Object value = getValue();
                return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
              }
            };
          }

          @Override
          public void remove() {
            iterator.remove();
          }
        };
      }

      @Override
      public int size() {
        return variables.size();
      }

    };
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("{\n");
    for (Entry<String, TypedValue> variable : variables.entrySet()) {
      stringBuilder.append("  ");
      stringBuilder.append(variable.getKey());
      stringBuilder.append(" => ");
      stringBuilder.append(variable.getValue());
      stringBuilder.append("\n");
    }
    stringBuilder.append("}");
    return stringBuilder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return asValueMap().equals(obj);
  }

  @Override
  public int hashCode() {
    return asValueMap().hashCode();
  }

  public Map<String, Object> asValueMap() {
    return new HashMap<>(this);
  }

  @Override
  public TypedValue resolve(String variableName) {
    return getValueTyped(variableName);
  }

  @Override
  public boolean containsVariable(String variableName) {
    return containsKey(variableName);
  }

  @Override
  public VariableContext asVariableContext() {
    return this;
  }

}
