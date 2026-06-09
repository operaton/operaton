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
package org.operaton.bpm.engine.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonConfiguratorTest {

  private static final int DEFAULT_LIMIT = StreamReadConstraints.DEFAULT_MAX_STRING_LEN;
  private static final int HIGHER_LIMIT = 50_000_000;

  @Test
  void shouldDeserializeStringBelowConfiguredLimit() throws JsonProcessingException {
    assertStringRoundTrip(HIGHER_LIMIT - 1);
  }

  @Test
  void shouldDeserializeStringAtConfiguredLimit() throws JsonProcessingException {
    assertStringRoundTrip(HIGHER_LIMIT);
  }

  @Test
  void shouldRejectStringAboveConfiguredLimit() {
    assertThatThrownBy(() -> assertStringRoundTrip(HIGHER_LIMIT + 1))
        .isInstanceOf(StreamConstraintsException.class);
  }

  @Test
  void shouldDeserializeStringBelowDefaultLimit() throws JsonProcessingException {
    assertStringRoundTrip(DEFAULT_LIMIT - 1);
  }

  @Test
  void shouldDeserializeStringAtDefaultLimit() throws JsonProcessingException {
    assertStringRoundTrip(DEFAULT_LIMIT);
  }

  @Test
  void shouldDeserializeStringAboveDefaultLimit() throws JsonProcessingException {
    assertStringRoundTrip(DEFAULT_LIMIT + 1);
  }

  private void assertStringRoundTrip(int dataLength) throws JsonProcessingException {
    ObjectMapper objectMapper = new JacksonConfigurator().getContext(ObjectMapper.class);

    StreamReadConstraints constraints = objectMapper.getFactory().streamReadConstraints();
    assertThat(constraints.getMaxStringLength()).isEqualTo(HIGHER_LIMIT);

    String longString = "x".repeat(dataLength);
    String json = objectMapper.writeValueAsString(longString);
    String deserializedString = objectMapper.readValue(json, String.class);

    assertThat(deserializedString).isEqualTo(longString);
  }

}
