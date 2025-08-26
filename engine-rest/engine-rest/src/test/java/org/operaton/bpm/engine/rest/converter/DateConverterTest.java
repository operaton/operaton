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
package org.operaton.bpm.engine.rest.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;

class DateConverterTest {
  private DateConverter converter;

  @BeforeEach
  void setUp() {
    converter = new DateConverter();
  }

  @Test
  void shouldFailForDoubleQuotedValue() {
    //when
    assertThrows(InvalidRequestException.class, () -> converter.convertQueryParameterToType("\"pizza\""));
  }

  @Test
  void shouldFailForSingleDoubleQuotedValue() {
    //when
    assertThrows(InvalidRequestException.class, () -> converter.convertQueryParameterToType("2014-01-01T00:00:00+0200\""));
  }

  @Test
  void shouldConvertDate() throws Exception {
    //given
    String value = "2014-01-01T00:00:00+0200";
    ObjectMapper mock = mock(ObjectMapper.class);
    converter.setObjectMapper(mock);
    when(mock.readValue(anyString(), eq(Date.class))).thenReturn(DateTimeUtil.parseDate(value));

    //when
    Date date = converter.convertQueryParameterToType(value);

    //then
    assertEquals(date, DateTimeUtil.parseDate(value));
  }
}
