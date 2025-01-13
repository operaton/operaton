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
package org.operaton.bpm.engine.rest.dto.converter;

import org.operaton.bpm.engine.rest.exception.InvalidRequestException;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Thorben Lindhauer
 */
public abstract class JacksonAwareStringToTypeConverter<T> implements StringToTypeConverter<T> {

  protected ObjectMapper objectMapper;

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  protected T mapToType(String value, Class<T> typeClass) {
    try {
      return objectMapper.readValue(value, typeClass);
    } catch (JsonProcessingException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, String.format("Cannot convert value %s to java type %s",
        value, typeClass.getName()));
    }
  }
}
