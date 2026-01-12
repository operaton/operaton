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
package org.operaton.bpm.integrationtest.functional.spin.dataformat;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.operaton.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.operaton.spin.spi.DataFormatConfigurator;

/**
 * @author Thorben Lindhauer
 *
 */
public class JsonDataFormatConfigurator implements DataFormatConfigurator<JacksonJsonDataFormat> {

  // For test code usage - note: SimpleDateFormat is required by Jackson's ObjectMapper.setDateFormat()
  // Each usage should create a new instance via getDateFormat() for thread-safety
  public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
  
  public static SimpleDateFormat getDateFormat() {
    return new SimpleDateFormat(DATE_FORMAT_PATTERN);
  }
  
  // For backward compatibility with existing test code
  public static final SimpleDateFormat DATE_FORMAT = getDateFormat();

  @Override
  public Class<JacksonJsonDataFormat> getDataFormatClass() {
    return JacksonJsonDataFormat.class;
  }

  @Override
  public void configure(JacksonJsonDataFormat dataFormat) {
    ObjectMapper objectMapper = dataFormat.getObjectMapper();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.setDateFormat(getDateFormat());

  }

}
