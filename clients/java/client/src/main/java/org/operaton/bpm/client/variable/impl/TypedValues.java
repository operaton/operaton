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
package org.operaton.bpm.client.variable.impl;

import org.operaton.bpm.client.impl.ExternalTaskClientLogger;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

import java.util.HashMap;
import java.util.Map;

public class TypedValues<T extends TypedValue> {

  protected static final ExternalTaskClientLogger LOG = ExternalTaskClientLogger.CLIENT_LOGGER;

  protected ValueMappers<T> serializers;

  public TypedValues(ValueMappers<T> serializers) {
    this.serializers = serializers;
  }

  public Map<String, TypedValueField> serializeVariables(Map<String, Object> variables) {
    Map<String, TypedValueField> result = new HashMap<>();

    if (variables != null) {
      for (var vars : variables.entrySet()) {
        String variableName = vars.getKey();
        Object variableValue;
        if (variables instanceof VariableMap variableMap) {
          variableValue = variableMap.getValueTyped(variableName);
        }
        else {
          variableValue = vars.getValue();
        }

        try {
          @SuppressWarnings("unchecked")
          T typedValue = (T) createTypedValue(variableValue);
          TypedValueField typedValueField = toTypedValueField(typedValue);
          result.put(variableName, typedValueField);
        }
        catch (Exception e) {
          throw LOG.cannotSerializeVariable(variableName, e);
        }

      }

    }

    return result;
  }

  public Map<String, VariableValue<T>> wrapVariables(ExternalTask externalTask, Map<String, TypedValueField> variables) {
    String executionId = externalTask.getExecutionId();

    Map<String, VariableValue<T>> result = new HashMap<>();

    if (variables != null) {
      variables.forEach((variableName, variableValue) -> {

        String typeName = variableValue.getType();
        typeName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
        variableValue.setType(typeName);

        VariableValue<T> value = new VariableValue<>(executionId, variableName, variableValue, serializers);
        result.put(variableName, value);
      });
    }

    return result;
  }

  protected  TypedValueField toTypedValueField(T typedValue) {
    ValueMapper<T> serializer = findSerializer(typedValue);

    if(typedValue instanceof UntypedValueImpl untypedValue) {
      typedValue = serializer.convertToTypedValue(untypedValue);
    }

    TypedValueField typedValueField = new TypedValueField();

    serializer.writeValue(typedValue, typedValueField);

    ValueType valueType = typedValue.getType();
    typedValueField.setValueInfo(valueType.getValueInfo(typedValue));

    String typeName = valueType.getName();
    String typeNameCapitalized = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
    typedValueField.setType(typeNameCapitalized);

    return typedValueField;
  }

  protected ValueMapper<T> findSerializer(T typedValue) {
    return serializers.findMapperForTypedValue(typedValue);
  }

  protected TypedValue createTypedValue(Object value) {
    TypedValue typedValue;

    if (value instanceof TypedValue v) {
      typedValue = v;
    }
    else {
      typedValue = Variables.untypedValue(value);
    }

    return typedValue;
  }

}
