/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.engine.rest.dto;

import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.type.ValueTypeResolver;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VariableValueDtoTest {
  ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  ProcessEngineConfiguration processEngineConfiguration;

  @Mock
  ValueTypeResolver valueTypeResolver;

  @Mock
  ProcessEngine processEngine;

  @BeforeEach
  void setUp() {
    // Mock the process engine to return the mocked configuration and resolver
    lenient().when(processEngine.getProcessEngineConfiguration())
        .thenReturn(processEngineConfiguration);
    lenient().when(processEngineConfiguration.getValueTypeResolver())
        .thenReturn(valueTypeResolver);

  }

  @Test
  void toMap_returnsEmptyVariableMap_forNullInput() {
    // when
    VariableMap result = VariableValueDto.toMap(null, processEngine, objectMapper);

    // then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void toMap_returnsEmptyVariableMap_forEmptyInput() {
    // when
    VariableMap result = VariableValueDto.toMap(emptyMap(), processEngine, objectMapper);

    // then
    assertThat(result).isNotNull().isEmpty();
  }

  @ParameterizedTest
  @MethodSource("toMap_convertsSimpleValues_args")
  void toMap_convertsSimpleValues(String varName, Object value) {
    // given
    VariableValueDto dto = new VariableValueDto();
    dto.setValue(value);

    // when
    VariableMap result = VariableValueDto.toMap(Map.of(varName, dto), processEngine, objectMapper);

    // then
    assertThat(result).isNotNull().containsEntry(varName, value);
  }

  static Stream<Arguments> toMap_convertsSimpleValues_args() {
    return Stream.of(
      Arguments.of("stringVar", "hello"),
      Arguments.of("intVar", Integer.valueOf(42)),
      Arguments.of("boolVar", Boolean.TRUE)
    );
  }

  @Test
  void toMap_respectsTransientFlag_inValueInfo() {
    // given
    VariableValueDto dto = new VariableValueDto();
    dto.setValue(Integer.valueOf(123));
    dto.setValueInfo(Map.of(ValueType.VALUE_INFO_TRANSIENT, Boolean.TRUE));

    // when
    VariableMap result = VariableValueDto.toMap(Map.of("transientVar", dto), processEngine, objectMapper);

    // then
    // raw value still accessible
    assertThat(result).containsEntry("transientVar", Integer.valueOf(123));

    // typed view preserves transient flag
    TypedValue typed = result.getValueTyped("transientVar");
    assertThat(typed).isNotNull();
    assertThat(typed.isTransient()).isTrue();
  }
}
