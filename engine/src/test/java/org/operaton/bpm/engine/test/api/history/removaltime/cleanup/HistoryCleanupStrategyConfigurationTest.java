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
package org.operaton.bpm.engine.test.api.history.removaltime.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_END_TIME_BASED;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_END;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_NONE;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_START;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Tassilo Weidner
 */
class HistoryCleanupStrategyConfigurationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected static ProcessEngineConfigurationImpl engineConfiguration;

  @BeforeEach
  void init() {
    engineConfiguration
      .setHistoryCleanupStrategy(null)
      .setHistoryRemovalTimeStrategy(null)
      .initHistoryCleanup();
  }

  @AfterAll
  static void tearDown() {
    engineConfiguration
      .setHistoryRemovalTimeStrategy(null)
      .initHistoryRemovalTime();
    engineConfiguration
      .setHistoryCleanupStrategy(null)
      .initHistoryCleanup();
  }

  @Test
  void shouldAutomaticallyConfigure() {
    // given

    engineConfiguration
      .setHistoryCleanupStrategy(null);

    // when
    engineConfiguration.initHistoryCleanup();

    // then
    assertThat(engineConfiguration.getHistoryCleanupStrategy()).isEqualTo(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);
  }

  @Test
  void shouldConfigureToRemovalTimeBased() {
    // given

    engineConfiguration
      .setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);

    // when
    engineConfiguration.initHistoryCleanup();

    // then
    assertThat(engineConfiguration.getHistoryCleanupStrategy()).isEqualTo(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);
  }

  @Test
  void shouldConfigureToRemovalTimeBasedWithRemovalTimeStrategyToEnd() {
    // given

    engineConfiguration
      .setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED)
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END);

    // when
    engineConfiguration.initHistoryCleanup();

    // then
    assertThat(engineConfiguration.getHistoryCleanupStrategy()).isEqualTo(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);
    assertThat(engineConfiguration.getHistoryRemovalTimeStrategy()).isEqualTo(HISTORY_REMOVAL_TIME_STRATEGY_END);
  }

  @Test
  void shouldConfigureToRemovalTimeBasedWithRemovalTimeStrategyToStart() {
    // given

    engineConfiguration
      .setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED)
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START);

    // when
    engineConfiguration.initHistoryCleanup();

    // then
    assertThat(engineConfiguration.getHistoryCleanupStrategy()).isEqualTo(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);
    assertThat(engineConfiguration.getHistoryRemovalTimeStrategy()).isEqualTo(HISTORY_REMOVAL_TIME_STRATEGY_START);
  }


  @Test
  void shouldConfigureToEndTimeBased() {
    // given

    engineConfiguration
      .setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_END_TIME_BASED);

    // when
    engineConfiguration.initHistoryCleanup();

    // then
    assertThat(engineConfiguration.getHistoryCleanupStrategy()).isEqualTo(HISTORY_CLEANUP_STRATEGY_END_TIME_BASED);
  }

  @Test
  void shouldConfigureWithNotExistentStrategy() {
    // given

    engineConfiguration
      .setHistoryCleanupStrategy("nonExistentStrategy");

    // when/then
    assertThatThrownBy(() -> engineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("history cleanup strategy must be either set to 'removalTimeBased' or 'endTimeBased'.");
  }

  @Test
  void shouldConfigureToRemovalTimeBasedWithRemovalTimeStrategyToNone() {
    // given

    engineConfiguration
      .setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED)
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE);

    // when/then
    assertThatThrownBy(() -> engineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("history removal time strategy cannot be set to 'none' in conjunction with 'removalTimeBased' history cleanup strategy.");
  }

}
