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
package org.operaton.bpm.engine.test.jobexecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JobExecutorPriorityRangeConfigurationTest {

  private static final String SOME_PROCESS_ENGINE_NAME = "testProcessEngine";

  ProcessEngineConfigurationImpl config;

  protected Long defaultJobExecutorPriorityRangeMin;
  protected Long defaultJobExecutorPriorityRangeMax;

  @BeforeEach
  void setup() {
    config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("operaton.cfg.xml");
    config.setProcessEngineName(SOME_PROCESS_ENGINE_NAME);
    defaultJobExecutorPriorityRangeMin = config.getJobExecutorPriorityRangeMin();
    defaultJobExecutorPriorityRangeMax = config.getJobExecutorPriorityRangeMax();
  }

  @AfterEach
  void tearDown() {
    config.setJobExecutorPriorityRangeMin(defaultJobExecutorPriorityRangeMin);
    config.setJobExecutorPriorityRangeMax(defaultJobExecutorPriorityRangeMax);
  }

  @Test
  void shouldAcceptValidPriorityRangeConfiguration() {
    // given
    config.setJobExecutorPriorityRangeMin(10L);
    config.setJobExecutorPriorityRangeMax(10L);

    // when
    ProcessEngine engine = config.buildProcessEngine();

    try {
      // then
      assertThat(config.getJobExecutorPriorityRangeMin()).isEqualTo(10L);
      assertThat(config.getJobExecutorPriorityRangeMax()).isEqualTo(10L);
    } finally {
      engine.close();
    }
  }

  @Test
  void shouldAllowNegativePriorityRangeConfiguration() {
    // given
    config.setJobExecutorPriorityRangeMin(-10L);
    config.setJobExecutorPriorityRangeMax(-5);

    // when
    assertDoesNotThrow(() -> {
      ProcessEngine engine = config.buildProcessEngine();
      engine.close();
    });
  }

  @Test
  void shouldThrowExceptionOnNegativeMaxPriorityRangeConfiguration() {
    // given
    config.setJobExecutorPriorityRangeMin(0L);
    config.setJobExecutorPriorityRangeMax(-10L);

    // then
    assertThatThrownBy(() -> config.buildProcessEngine())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("ENGINE-14031 Invalid configuration for job executor priority range. Reason: jobExecutorPriorityRangeMin can not be greater than jobExecutorPriorityRangeMax");
  }

  @Test
  void shouldThrowExceptionJobExecutorPriorityMinLargerThanMax() {
    // given
    config.setJobExecutorPriorityRangeMin(10L);
    config.setJobExecutorPriorityRangeMax(5L);

    // then
    assertThatThrownBy(() -> config.buildProcessEngine())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("ENGINE-14031 Invalid configuration for job executor priority range. Reason: jobExecutorPriorityRangeMin can not be greater than jobExecutorPriorityRangeMax");
  }
}
