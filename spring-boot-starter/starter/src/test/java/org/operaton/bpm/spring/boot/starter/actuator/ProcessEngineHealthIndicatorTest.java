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
package org.operaton.bpm.spring.boot.starter.actuator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.health.HealthResult;
import org.operaton.bpm.engine.health.HealthService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.operaton.bpm.engine.test.util.ProcessEngineUtils.newRandomProcessEngineName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessEngineHealthIndicatorTest {

  private static final String PROCESS_ENGINE_NAME = newRandomProcessEngineName();

  @Mock
  private ProcessEngine processEngine;

  @Mock
  private HealthService healthService;

  @Test
  void nullProcessEngineTest() {
    assertThatNullPointerException()
            .isThrownBy(() -> new ProcessEngineHealthIndicator(null, healthService))
            .withMessage("processEngine must not be null");
  }

  @Test
  void nullHealthServiceTest() {
    assertThatNullPointerException()
            .isThrownBy(() -> new ProcessEngineHealthIndicator(processEngine, null))
            .withMessage("healthService must not be null");
  }

  @Test
  void upTest() {
    when(processEngine.getName()).thenReturn(PROCESS_ENGINE_NAME);
    when(healthService.check()).thenReturn(new HealthResult("UP", null, null, Map.of()));

    Health health = new ProcessEngineHealthIndicator(processEngine, healthService).health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("name", PROCESS_ENGINE_NAME);
  }

  @Test
  void downTest() {
    when(processEngine.getName()).thenReturn(PROCESS_ENGINE_NAME);
    when(healthService.check()).thenReturn(new HealthResult("DOWN", null, null, Map.of()));

    Health health = new ProcessEngineHealthIndicator(processEngine, healthService).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("name", PROCESS_ENGINE_NAME);
  }

  @Test
  void unknownStatusTest() {
    when(processEngine.getName()).thenReturn(PROCESS_ENGINE_NAME);
    when(healthService.check()).thenReturn(new HealthResult("UNKNOWN", null, null, Map.of()));

    Health health = new ProcessEngineHealthIndicator(processEngine, healthService).health();

    assertThat(health.getStatus().getCode()).isEqualTo("UNKNOWN");
    assertThat(health.getDetails()).containsEntry("name", PROCESS_ENGINE_NAME);
  }

  @Test
  void customStatusTest() {
    when(processEngine.getName()).thenReturn(PROCESS_ENGINE_NAME);
    when(healthService.check()).thenReturn(new HealthResult("DEGRADED", null, null, Map.of()));

    Health health = new ProcessEngineHealthIndicator(processEngine, healthService).health();

    assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
  }
}
