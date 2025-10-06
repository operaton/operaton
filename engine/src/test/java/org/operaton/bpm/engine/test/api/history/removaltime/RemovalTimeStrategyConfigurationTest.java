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
package org.operaton.bpm.engine.test.api.history.removaltime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryRemovalTimeProvider;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_END;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_NONE;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author Tassilo Weidner
 */
class RemovalTimeStrategyConfigurationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected static ProcessEngineConfigurationImpl processEngineConfiguration;

  @BeforeEach
  void init() {
    processEngineConfiguration
      .setHistoryRemovalTimeStrategy(null)
      .setHistoryRemovalTimeProvider(null)
      .initHistoryRemovalTime();
  }

  @AfterAll
  static void tearDown() {
    processEngineConfiguration
      .setHistoryRemovalTimeStrategy(null)
      .setHistoryRemovalTimeProvider(null)
      .initHistoryRemovalTime();
  }

  @Test
  void shouldAutomaticallyConfigure() {
    // given

    processEngineConfiguration
      .setHistoryRemovalTimeProvider(null)
      .setHistoryRemovalTimeStrategy(null);

    // when
    processEngineConfiguration.initHistoryRemovalTime();

    // then
    assertThat(processEngineConfiguration.getHistoryRemovalTimeStrategy()).isEqualTo(HISTORY_REMOVAL_TIME_STRATEGY_END);
    assertThat(processEngineConfiguration.getHistoryRemovalTimeProvider()).isInstanceOf(HistoryRemovalTimeProvider.class);
  }

  @Test
  void shouldConfigureToStart() {
    // given

    processEngineConfiguration
      .setHistoryRemovalTimeProvider(mock(HistoryRemovalTimeProvider.class))
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START);

    // when
    processEngineConfiguration.initHistoryRemovalTime();

    // then
    assertThat(processEngineConfiguration.getHistoryRemovalTimeStrategy()).isEqualTo(HISTORY_REMOVAL_TIME_STRATEGY_START);
    assertThat(processEngineConfiguration.getHistoryRemovalTimeProvider()).isInstanceOf(HistoryRemovalTimeProvider.class);
  }

  @Test
  void shouldConfigureToEnd() {
    // given

    processEngineConfiguration
      .setHistoryRemovalTimeProvider(mock(HistoryRemovalTimeProvider.class))
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END);

    // when
    processEngineConfiguration.initHistoryRemovalTime();

    // then
    assertThat(processEngineConfiguration.getHistoryRemovalTimeStrategy()).isEqualTo(HISTORY_REMOVAL_TIME_STRATEGY_END);
    assertThat(processEngineConfiguration.getHistoryRemovalTimeProvider()).isInstanceOf(HistoryRemovalTimeProvider.class);
  }

  @Test
  void shouldConfigureToNone() {
    // given

    processEngineConfiguration
      .setHistoryRemovalTimeProvider(mock(HistoryRemovalTimeProvider.class))
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE);

    // when
    processEngineConfiguration.initHistoryRemovalTime();

    // then
    assertThat(processEngineConfiguration.getHistoryRemovalTimeStrategy()).isEqualTo(HISTORY_REMOVAL_TIME_STRATEGY_NONE);
    assertThat(processEngineConfiguration.getHistoryRemovalTimeProvider()).isInstanceOf(HistoryRemovalTimeProvider.class);
  }

  @Test
  void shouldConfigureWithoutProvider() {
    // given

    processEngineConfiguration
      .setHistoryRemovalTimeProvider(null)
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END);

    // when
    processEngineConfiguration.initHistoryRemovalTime();

    // then
    assertThat(processEngineConfiguration.getHistoryRemovalTimeStrategy()).isEqualTo(HISTORY_REMOVAL_TIME_STRATEGY_END);
    assertThat(processEngineConfiguration.getHistoryRemovalTimeProvider()).isInstanceOf(HistoryRemovalTimeProvider.class);
  }

  @Test
  void shouldConfigureWithNotExistentStrategy() {
    // given
    processEngineConfiguration.setHistoryRemovalTimeStrategy("notExistentStrategy");

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryRemovalTime())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("history removal time strategy must be set to 'start', 'end' or 'none'");

    // assume
    assertThat(processEngineConfiguration.getHistoryRemovalTimeProvider()).isInstanceOf(HistoryRemovalTimeProvider.class);
  }

}
