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
package org.operaton.bpm.engine.rest.sub.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.rest.dto.PatchVariablesDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.mapper.MultipartFormData;
import org.operaton.bpm.engine.rest.mapper.MultipartFormData.FormPart;
import org.operaton.bpm.engine.rest.sub.VariableResource;
import org.operaton.bpm.engine.runtime.DeserializationTypeValidator;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;


public abstract class AbstractVariablesResource implements VariableResource {

  protected static final String DEFAULT_BINARY_VALUE_TYPE = "Bytes";

  protected ProcessEngine engine;
  protected String resourceId;
  protected ObjectMapper objectMapper;

  protected AbstractVariablesResource(ProcessEngine engine, String resourceId, ObjectMapper objectMapper) {
    this.engine = engine;
    this.resourceId = resourceId;
    this.objectMapper = objectMapper;
  }

  @Override
  public Map<String, VariableValueDto> getVariables(boolean deserializeValues) {

    VariableMap variables = getVariableEntities(deserializeValues);

    return VariableValueDto.fromMap(variables);
  }

  @Override
  public VariableValueDto getVariable(String variableName, boolean deserializeValue) {
    TypedValue value = getTypedValueForVariable(variableName, deserializeValue);
    return VariableValueDto.fromTypedValue(value);

  }

  protected TypedValue getTypedValueForVariable(String variableName, boolean deserializeValue) {
    TypedValue value;
    try {
       value = getVariableEntity(variableName, deserializeValue);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = "Cannot get %s variable %s: %s".formatted(getResourceTypeName(), variableName, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }

    if (value == null) {
      String errorMessage = "%s variable with name %s does not exist".formatted(getResourceTypeName(), variableName);
      throw new InvalidRequestException(Status.NOT_FOUND, errorMessage);
    }
    return value;
  }

  @Override
  public Response getVariableBinary(String variableName) {
    TypedValue typedValue = getTypedValueForVariable(variableName, false);
    return new VariableResponseProvider().getResponseForTypedVariable(typedValue, resourceId);
  }

  @Override
  public void putVariable(String variableName, VariableValueDto variable) {
    try {
      TypedValue typedValue = variable.toTypedValue(engine, objectMapper);
      setVariableEntity(variableName, typedValue);

    } catch (RestException e) {
      throw new InvalidRequestException(e.getStatus(), e,
        "Cannot put %s variable %s: %s".formatted(getResourceTypeName(), variableName, e.getMessage()));
    } catch (BadUserRequestException e) {
      throw new RestException(Status.BAD_REQUEST, e,
        "Cannot put %s variable %s: %s".formatted(getResourceTypeName(), variableName, e.getMessage()));
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e,
        "Cannot put %s variable %s: %s".formatted(getResourceTypeName(), variableName, e.getMessage()));
    }
  }

  @Override
  public void setBinaryVariable(String variableKey, MultipartFormData payload) {
    if(payload.getNamedPart("type") != null) {
      setBinaryVariableFromTypePart(variableKey, payload);
    } else {
      setBinaryVariableFromDataPart(variableKey, payload);
    }
  }

  private void setBinaryVariableFromTypePart(String variableKey, MultipartFormData payload) {
    FormPart dataPart = payload.getNamedPart("data");
    FormPart objectTypePart = payload.getNamedPart("type");

    if (dataPart.getContentType() != null && dataPart.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON)) {
      Object object = deserializeJsonObject(objectTypePart.getTextContent(), dataPart.getBinaryContent());

      if (object != null) {
        setVariableEntity(variableKey, Variables.objectValue(object).create());
      }
    } else {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Unrecognized content type for serialized java type: "+ dataPart.getContentType());
    }

  }

  private void setBinaryVariableFromDataPart(String variableKey, MultipartFormData payload) {
    FormPart dataPart = payload.getNamedPart("data");
    FormPart valueTypePart = payload.getNamedPart("valueType");
    String valueTypeName = DEFAULT_BINARY_VALUE_TYPE;
    if (valueTypePart != null) {
      if (valueTypePart.getTextContent() == null) {
        throw new InvalidRequestException(Status.BAD_REQUEST,
            "Form part with name 'valueType' must have a text/plain value");
      }

      valueTypeName = valueTypePart.getTextContent();
    }

    VariableValueDto valueDto = VariableValueDto.fromFormPart(valueTypeName, dataPart);
    try {

      TypedValue typedValue = valueDto.toTypedValue(engine, objectMapper);
      setVariableEntity(variableKey, typedValue);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = "Cannot put %s variable %s: %s".formatted(getResourceTypeName(), variableKey, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }
  }

  protected Object deserializeJsonObject(String className, byte[] data) {
    try {
      JavaType type = TypeFactory.defaultInstance().constructFromCanonical(className);
      validateType(type);
      return objectMapper.readValue(new String(data, StandardCharsets.UTF_8), type);
    } catch(Exception e) {
      throw new InvalidRequestException(Status.INTERNAL_SERVER_ERROR, "Could not deserialize JSON object: "+e.getMessage());
    }
  }

  /**
   * Validate the type with the help of the validator in the engine.<br>
   * Note: when adjusting this method, please also consider adjusting
   * the {@code JacksonJsonDataFormatMapper#validateType} in the Engine Spin Plugin
   */
  protected void validateType(JavaType type) {
    if (getProcessEngineConfiguration().isDeserializationTypeValidationEnabled()) {
      DeserializationTypeValidator validator = getProcessEngineConfiguration().getDeserializationTypeValidator();
      if (validator != null) {
        List<String> invalidTypes = new ArrayList<>();
        validateType(type, validator, invalidTypes);
        if (!invalidTypes.isEmpty()) {
          throw new IllegalArgumentException("The following classes are not whitelisted for deserialization: %s".formatted(invalidTypes));
        }
      }
    }
  }

  protected void validateType(JavaType type, DeserializationTypeValidator validator, List<String> invalidTypes) {
    if (!type.isPrimitive()) {
      if (!type.isArrayType()) {
        validateTypeInternal(type, validator, invalidTypes);
      }
      if (type.isMapLikeType()) {
        validateType(type.getKeyType(), validator, invalidTypes);
      }
      if (type.isContainerType() || type.hasContentType()) {
        validateType(type.getContentType(), validator, invalidTypes);
      }
    }
  }

  protected void validateTypeInternal(JavaType type, DeserializationTypeValidator validator, List<String> invalidTypes) {
    String className = type.getRawClass().getName();
    if (!validator.validate(className) && !invalidTypes.contains(className)) {
      invalidTypes.add(className);
    }
  }

  protected ProcessEngineConfiguration getProcessEngineConfiguration() {
    return engine.getProcessEngineConfiguration();
  }

  @Override
  public void deleteVariable(String variableName) {
    try {
      removeVariableEntity(variableName);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = "Cannot delete %s variable %s: %s".formatted(getResourceTypeName(), variableName, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }

  }

  @Override
  public void modifyVariables(PatchVariablesDto patch) {
    VariableMap variableModifications = null;
    try {
      variableModifications = VariableValueDto.toMap(patch.getModifications(), engine, objectMapper);

    } catch (RestException e) {
      String errorMessage = "Cannot modify variables for %s: %s".formatted(getResourceTypeName(), e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    }

    List<String> variableDeletions = patch.getDeletions();

    try {
      updateVariableEntities(variableModifications, variableDeletions);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = "Cannot modify variables for %s %s: %s".formatted(getResourceTypeName(), resourceId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }


  }

  protected abstract VariableMap getVariableEntities(boolean deserializeValues);

  protected abstract void updateVariableEntities(VariableMap variables, List<String> deletions);

  protected abstract TypedValue getVariableEntity(String variableKey, boolean deserializeValue);

  protected abstract void setVariableEntity(String variableKey, TypedValue variableValue);

  protected abstract void removeVariableEntity(String variableKey);

  protected abstract String getResourceTypeName();

}
