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
package org.operaton.bpm.run.test.health;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.health.HealthResult;
import org.operaton.bpm.engine.health.HealthService;
import org.operaton.bpm.run.health.HealthController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@TestPropertySource(properties = {"operaton.run.health.rest-endpoint.enabled=true"})
class HealthControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  HealthService healthService;

  @Test
  void shouldReturn200WhenUp() throws Exception {
    when(healthService.check()).thenReturn(new HealthResult("UP", "2026-01-01T00:00:00Z", null, Map.of()));

    mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void shouldReturn503WhenDown() throws Exception {
    when(healthService.check()).thenReturn(new HealthResult("DOWN", "2026-01-01T00:00:00Z", null, Map.of()));

    mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  @Test
  void shouldReturn503WhenUnknown() throws Exception {
    when(healthService.check()).thenReturn(new HealthResult("UNKNOWN", "2026-01-01T00:00:00Z", null, Map.of()));

    mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("UNKNOWN"));
  }
}
