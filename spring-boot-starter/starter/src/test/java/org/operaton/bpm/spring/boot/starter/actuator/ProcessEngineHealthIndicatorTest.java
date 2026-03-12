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
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import org.operaton.bpm.engine.ProcessEngine;

import static org.operaton.bpm.engine.test.util.ProcessEngineUtils.newRandomProcessEngineName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessEngineHealthIndicatorTest {

  private static final String PROCESS_ENGINE_NAME = newRandomProcessEngineName();

  @Mock
  private ProcessEngine processEngine;

  @Test
  void nullTest() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ProcessEngineHealthIndicator(null))
      .withMessage("processEngine must not be null");
  }

  @Test
  void upTest() {
    when(processEngine.getName()).thenReturn(PROCESS_ENGINE_NAME);
    Health health = new ProcessEngineHealthIndicator(processEngine).health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("name", PROCESS_ENGINE_NAME);
  }
}
