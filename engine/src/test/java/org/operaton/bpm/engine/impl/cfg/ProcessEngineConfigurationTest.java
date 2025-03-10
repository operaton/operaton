/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.operaton.bpm.engine.impl.ProcessEngineLogger.CONFIG_LOGGER;

import java.sql.Connection;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;

public class ProcessEngineConfigurationTest {

  private ProcessEngineConfigurationImpl engineConfiguration;
  private ConfigurationLogger logger;
  private static final int SERIALIZABLE_VALUE = Connection.TRANSACTION_SERIALIZABLE;
  private static final String SERIALIZABLE_NAME = "SERIALIZABLE";
  public static final ProcessEngineException EXPECTED_EXCEPTION = CONFIG_LOGGER.invalidTransactionIsolationLevel(SERIALIZABLE_NAME);

  @BeforeEach
  void setUp() {
    this.engineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault();
    this.logger = mock(ConfigurationLogger.class);
    when(logger.invalidTransactionIsolationLevel(SERIALIZABLE_NAME)).thenReturn(EXPECTED_EXCEPTION);
    engineConfiguration.initDataSource(); // initialize the datasource for the first time so we can modify the level
    ProcessEngineConfigurationImpl.LOG = logger;
  }

  @AfterAll
  static void cleanUp() {
    ProcessEngineConfigurationImpl.LOG = CONFIG_LOGGER;
  }

  @Test
  void shouldEnableStandaloneTasksByDefault() {
    // when
    ProcessEngineConfigurationImpl engineCfg = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();

    // then
    assertThat(engineCfg.isStandaloneTasksEnabled()).isTrue();
  }

  @Test
  void shouldEnableImplicitUpdatesDetectionByDefault() {
    // when
    ProcessEngineConfigurationImpl engineCfg = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
    // then
    assertThat(engineCfg.isImplicitVariableUpdateDetectionEnabled()).isTrue();
  }

  @Test
  void validIsolationLevel() {
    // given
    ((PooledDataSource) engineConfiguration.getDataSource()).setDefaultTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
    // when
    assertThatCode(() -> engineConfiguration.initDataSource()).doesNotThrowAnyException();
  }

  @Test
  void invalidIsolationLevelWithSkipFlagDisabled() {
    // given
    ((PooledDataSource) engineConfiguration.getDataSource()).setDefaultTransactionIsolationLevel(SERIALIZABLE_VALUE);
    // when then
    assertThatThrownBy(() -> engineConfiguration.initDataSource())
        .isInstanceOf(ProcessEngineException.class)
        .hasMessage(EXPECTED_EXCEPTION.getMessage());
  }

  @Test
  void invalidIsolationLevelWithSkipFlagEnabled() {
    // given
    ((PooledDataSource) engineConfiguration.getDataSource()).setDefaultTransactionIsolationLevel(SERIALIZABLE_VALUE);
    engineConfiguration.setSkipIsolationLevelCheck(true);
    // when
    engineConfiguration.initDataSource();
    // then
    verify(logger).logSkippedIsolationLevelCheck(SERIALIZABLE_NAME);
  }

  @Test
  void validIsolationLevelPropertyFromFileIsSetCorrectly() {
    // given
    ProcessEngineConfigurationImpl engineCfg = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("operaton.cfg.skipIsolationLevelCheckEnabled.xml");
    // then
    assertThat(engineCfg.skipIsolationLevelCheck).isTrue();
  }
}
