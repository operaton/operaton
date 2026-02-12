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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class DefaultVariableSerializers implements VariableSerializers {

  protected List<TypedValueSerializer<?>> serializerList = new ArrayList<>();
  protected Map<String, TypedValueSerializer<?>> serializerMap = new HashMap<>();

  public DefaultVariableSerializers() {
  }

  public DefaultVariableSerializers(DefaultVariableSerializers serializers) {
    this.serializerList.addAll(serializers.serializerList);
    this.serializerMap.putAll(serializers.serializerMap);
  }

  @Override
  public TypedValueSerializer<?> getSerializerByName(String serializerName) {
     return serializerMap.get(serializerName);
  }

  @Override
  public TypedValueSerializer<?> findSerializerForValue(TypedValue value, VariableSerializerFactory fallBackSerializerFactory) {
    assertValueTypeNotNull(value);
    List<TypedValueSerializer<?>> matchedSerializers = findMatchingSerializers(value);

    if(matchedSerializers.isEmpty()) {
      return handleNoMatchingSerializers(value, fallBackSerializerFactory);
    } else if(matchedSerializers.size() == 1) {
      return matchedSerializers.get(0);
    } else {
      // ambiguous match, use default serializer
      return handleAmbiguousMatches(matchedSerializers);
    }
  }

  private void assertValueTypeNotNull(TypedValue value) {
    ValueType type = value.getType();
    if (type != null && type.isAbstract()) {
      throw new ProcessEngineException("Cannot serialize value of abstract type %s".formatted(type.getName()));
    }
  }

  private List<TypedValueSerializer<?>> findMatchingSerializers(TypedValue value) {
    List<TypedValueSerializer<?>> matchedSerializers = new ArrayList<>();
    ValueType type = value.getType();
    for (TypedValueSerializer<?> serializer : serializerList) {
      // if type is null => ask handler whether it can handle the value
      // OR if types match, this handler can handle values of this type
      //    => BUT we still need to ask as the handler may not be able to handle ALL values of this type.
      if((type == null || serializer.getType().equals(type)) && serializer.canHandle(value)) {
        matchedSerializers.add(serializer);
        if(serializer.getType().isPrimitiveValueType()) {
          break;
        }
      }
    }
    return matchedSerializers;
  }

  private TypedValueSerializer<?> handleNoMatchingSerializers(TypedValue value, VariableSerializerFactory fallBackSerializerFactory) {
    if (fallBackSerializerFactory != null) {
      TypedValueSerializer<?> serializer = fallBackSerializerFactory.getSerializer(value);
      if (serializer != null) {
        return serializer;
      }
    }
    throw new ProcessEngineException("Cannot find serializer for value '%s'.".formatted(value));
  }

  private TypedValueSerializer<?> handleAmbiguousMatches(List<TypedValueSerializer<?>> matchedSerializers) {
    String defaultSerializationFormat = Context.getProcessEngineConfiguration().getDefaultSerializationFormat();
    if(defaultSerializationFormat != null) {
      for (TypedValueSerializer<?> typedValueSerializer : matchedSerializers) {
        if(defaultSerializationFormat.equals(typedValueSerializer.getSerializationDataformat())) {
          return typedValueSerializer;
        }
      }
    }

    // no default serialization dataformat defined or default dataformat cannot serialize this value => use first serializer
    return matchedSerializers.get(0);
  }

  @Override
  public TypedValueSerializer<?> findSerializerForValue(TypedValue value) {
    return findSerializerForValue(value, null);
  }

  @Override
  public DefaultVariableSerializers addSerializer(TypedValueSerializer<?> serializer) {
    return addSerializer(serializer, serializerList.size());
  }

  @Override
  public DefaultVariableSerializers addSerializer(TypedValueSerializer<?> serializer, int index) {
    serializerList.add(index, serializer);
    serializerMap.put(serializer.getName(), serializer);
    return this;
  }

  public void setSerializerList(List<TypedValueSerializer<?>> serializerList) {
    this.serializerList.clear();
    this.serializerList.addAll(serializerList);
    this.serializerMap.clear();
    for (TypedValueSerializer<?> serializer : serializerList) {
      serializerMap.put(serializer.getName(), serializer);
    }
  }

  @Override
  public int getSerializerIndex(TypedValueSerializer<?> serializer) {
    return serializerList.indexOf(serializer);
  }

  @Override
  public int getSerializerIndexByName(String serializerName) {
    TypedValueSerializer<?> serializer = serializerMap.get(serializerName);
    if(serializer != null) {
      return getSerializerIndex(serializer);
    } else {
      return -1;
    }
  }

  @Override
  public VariableSerializers removeSerializer(TypedValueSerializer<?> serializer) {
    serializerList.remove(serializer);
    serializerMap.remove(serializer.getName());
    return this;
  }

  @Override
  public VariableSerializers join(VariableSerializers other) {
    DefaultVariableSerializers copy = new DefaultVariableSerializers();

    // "other" serializers override existing ones if their names match
    for (TypedValueSerializer<?> thisSerializer : serializerList) {
      TypedValueSerializer<?> serializer = other.getSerializerByName(thisSerializer.getName());

      if (serializer == null) {
        serializer = thisSerializer;
      }

      copy.addSerializer(serializer);
    }

    // add all "other" serializers that did not exist before to the end of the list
    for (TypedValueSerializer<?> otherSerializer : other.getSerializers()) {
      if (!copy.serializerMap.containsKey(otherSerializer.getName())) {
        copy.addSerializer(otherSerializer);
      }
    }


    return copy;
  }

  @Override
  public List<TypedValueSerializer<?>> getSerializers() {
    return new ArrayList<>(serializerList);
  }

}
