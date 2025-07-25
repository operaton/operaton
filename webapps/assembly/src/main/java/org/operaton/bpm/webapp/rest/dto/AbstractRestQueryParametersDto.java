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
package org.operaton.bpm.webapp.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.operaton.bpm.webapp.db.QueryParameters;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.JacksonAwareStringToTypeConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringToTypeConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.variable.Variables;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

public abstract class AbstractRestQueryParametersDto<T> extends QueryParameters {
  protected static final String DEFAULT_ORDER = "RES.ID_ asc";

  private static final long serialVersionUID = 1L;

  protected static final String SORT_ORDER_ASC_VALUE = "asc";
  protected static final String SORT_ORDER_DESC_VALUE = "desc";

  private static final List<String> VALID_SORT_ORDER_VALUES;
  static {
    VALID_SORT_ORDER_VALUES = new ArrayList<>();
    VALID_SORT_ORDER_VALUES.add(SORT_ORDER_ASC_VALUE);
    VALID_SORT_ORDER_VALUES.add(SORT_ORDER_DESC_VALUE);
  }

  protected String sortBy;
  protected String sortOrder;


  protected ObjectMapper objectMapper;

  // required for populating via jackson
  protected AbstractRestQueryParametersDto() { }

  protected AbstractRestQueryParametersDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    this.objectMapper = objectMapper;
    for (Entry<String, List<String>> param : queryParameters.entrySet()) {
      String key = param.getKey();
      String value = param.getValue().iterator().next();
      this.setValueBasedOnAnnotation(key, value);
    }
    if (!sortOptionsValid()) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Only a single sorting parameter specified. sortBy and sortOrder required");
    }
  }

  @OperatonQueryParam("sortBy")
  public void setSortBy(String sortBy) {
    if (!isValidSortByValue(sortBy)) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "sortBy parameter has invalid value: " + sortBy);
    }
    this.sortBy = sortBy;
  }

  @OperatonQueryParam("sortOrder")
  public void setSortOrder(String sortOrder) {
    if (!VALID_SORT_ORDER_VALUES.contains(sortOrder)) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "sortOrder parameter has invalid value: " + sortOrder);
    }
    this.sortOrder = sortOrder;
  }

  protected boolean sortOptionsValid() {
    return (sortBy != null && sortOrder != null) || (sortBy == null && sortOrder == null);
  }

  public String getInternalOrderBy() {
    return getOrderBy();
  }

  @Deprecated(since = "1.0")
  public String getOrderBy() {
    if (sortBy != null) {
      if (sortOrder == null || sortOrder.isEmpty()) {
        sortOrder = SORT_ORDER_ASC_VALUE;
      }
      return String.format("%s %s", getOrderByValue(sortBy), sortOrder);
    }
    return DEFAULT_ORDER;
  }

  protected abstract String getOrderByValue(String sortBy);

  protected abstract boolean isValidSortByValue(String value);

  protected Object resolveVariableValue(Object value) {
    if (value != null && Number.class.isAssignableFrom(value.getClass())) {
      return Variables.numberValue((Number) value);
    } else if (value != null && objectMapper != null) {
      try {
        return objectMapper.readValue("\"" + value + "\"", Date.class);
      } catch (Exception e) {
        // ignore the exception
      }
    }
    return value;
  }

  /**
   * Finds the methods that are annotated with a {@link OperatonQueryParam} with a value that matches the key parameter.
   * Before invoking these methods, the annotated {@link StringToTypeConverter} is used to convert the String value to the desired Java type.
   * @param key
   * @param value
   */
  protected void setValueBasedOnAnnotation(String key, String value) {
    List<Method> matchingMethods = findMatchingAnnotatedMethods(key);
    for (Method method : matchingMethods) {
      Class<? extends JacksonAwareStringToTypeConverter<?>> converterClass = findAnnotatedTypeConverter(method);
      if (converterClass == null) {
        continue;
      }

      JacksonAwareStringToTypeConverter<?> converter = null;
      try {
        converter = converterClass.getDeclaredConstructor().newInstance();
        converter.setObjectMapper(objectMapper);
        Object convertedValue = converter.convertQueryParameterToType(value);
        method.invoke(this, convertedValue);
      } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
        throw new RestException(Status.INTERNAL_SERVER_ERROR, e, "Server error.");
      } catch (InvocationTargetException e) {
        throw new InvalidRequestException(Status.BAD_REQUEST, e, "Cannot set query parameter '" + key + "' to value '" + value + "'");
      } catch (RestException e) {
        throw new InvalidRequestException(e.getStatus(), e,
            "Cannot set query parameter '" + key + "' to value '" + value + "': " + e.getMessage());
      }
    }
  }

  private List<Method> findMatchingAnnotatedMethods(String parameterName) {
    List<Method> result = new ArrayList<>();
    Method[] methods = this.getClass().getMethods();
    for (Method method : methods) {
      Annotation[] methodAnnotations = method.getAnnotations();

      for (Annotation annotation : methodAnnotations) {
        if (annotation instanceof OperatonQueryParam parameterAnnotation && parameterAnnotation.value().equals(parameterName)) {
            result.add(method);
        }
      }
    }
    return result;
  }

  private Class<? extends JacksonAwareStringToTypeConverter<?>> findAnnotatedTypeConverter(Method method) {
    Annotation[] methodAnnotations = method.getAnnotations();

    for (int j = 0; j < methodAnnotations.length; j++) {
      Annotation annotation = methodAnnotations[j];
      if (annotation instanceof OperatonQueryParam parameterAnnotation) {
        return parameterAnnotation.converter();
      }
    }
    return null;
  }

  @JsonIgnore
  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
}
