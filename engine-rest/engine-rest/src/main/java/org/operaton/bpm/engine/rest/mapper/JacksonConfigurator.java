/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Properties;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.util.EngineUtilLogger;
import org.operaton.bpm.engine.rest.hal.Hal;

@Provider
@Produces({MediaType.APPLICATION_JSON, Hal.APPLICATION_HAL_JSON})
public class JacksonConfigurator implements ContextResolver<ObjectMapper> {

  protected static final EngineUtilLogger LOG = ProcessEngineLogger.UTIL_LOGGER;

  public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static String dateFormatString = DEFAULT_DATE_FORMAT;

  private static final String PROPERTIES_FILE = "jackson.properties";
  private static final String LENGTH_PROPERTY = "jackson.maxStringLength";
  private static final int maxStringLength = loadMaxStringLength();

  public static ObjectMapper configureObjectMapper(ObjectMapper mapper) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    mapper.setDateFormat(dateFormat);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    mapper.registerModule(new JavaTimeModule());
    configureStreamReadConstraints(mapper);

    return mapper;
  }

  private static int loadMaxStringLength() {
    int defaultMaxStringLength = StreamReadConstraints.DEFAULT_MAX_STRING_LEN;
    Properties properties = new Properties();

    try (InputStream inputStream = JacksonConfigurator.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
      if (inputStream != null) {
        properties.load(inputStream);
      }
    } catch (IOException e) {
      throw LOG.exceptionWhileReadingFile(PROPERTIES_FILE, e);
    }

    return Integer.parseInt(properties.getProperty(LENGTH_PROPERTY, String.valueOf(defaultMaxStringLength)));
  }

  /**
   * Configures StreamReadConstraints on the ObjectMapper if the Jackson version supports it.
   * StreamReadConstraints was added in Jackson 2.15.0.
   */
  private static void configureStreamReadConstraints(ObjectMapper mapper) {
    try {
      StreamReadConstraints streamReadConstraints = StreamReadConstraints.builder()
          .maxStringLength(maxStringLength)
          .build();
      mapper.getFactory().setStreamReadConstraints(streamReadConstraints);
    } catch (NoSuchMethodError e) {
      // Can happen when an application server provides older Jackson modules.
    }
  }

  @Override
  public ObjectMapper getContext(Class<?> clazz) {
    return configureObjectMapper(new ObjectMapper());
  }

  public static void setDateFormatString(String dateFormat) {
    dateFormatString = dateFormat;
  }

  public static String getDateFormatString() {
    return dateFormatString;
  }
}
