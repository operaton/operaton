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
package org.operaton.bpm.engine.test.history.useroperationlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

class SyncApiUserOperationLogLimitConfigurationTest {

  ProcessEngine engine;

  @AfterEach
  void tearDown() {
    if(engine != null) {
      engine.close();
    }
  }

  @Test
  void shouldConfigureDefault() {
    // given standard configuration
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("operaton.cfg.xml");
    processEngineConfiguration.setProcessEngineName("secondDefault"); // different name is needed to avoid issues with cached engines

    // when engine startup
    startEngineManaged(processEngineConfiguration);

    // then
    assertThat(processEngineConfiguration.getLogEntriesPerSyncOperationLimit()).isEqualTo(1L);
  }

  @Test
  void shouldAllowToConfigureNegativeOne() {
    // given configuration with logEntriesPerSyncOperationLimit=-1
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource(
            "/org/operaton/bpm/engine/test/history/useroperationlog/SyncApiUserOperationLogLimitConfigurationTest.shouldAllowToConfigureNegativeOne.cfg.xml");

    // when engine startup
    startEngineManaged(processEngineConfiguration);

    // then no exceptions
    assertThat(processEngineConfiguration.getLogEntriesPerSyncOperationLimit()).isEqualTo(-1L);
  }

  @Test
  void shouldAllowToConfigurePositiveValue() {
    // given configuration with logEntriesPerSyncOperationLimit=17000
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource(
            "/org/operaton/bpm/engine/test/history/useroperationlog/SyncApiUserOperationLogLimitConfigurationTest.shouldAllowToConfigurePositiveValue.cfg.xml");

    // when engine startup
    startEngineManaged(processEngineConfiguration);

    // then no exceptions
    assertThat(processEngineConfiguration.getLogEntriesPerSyncOperationLimit()).isEqualTo(17000);
  }

  @Test
  void shouldThrowExceptionWhenConfigureZero() {
    // given configuration with logEntriesPerSyncOperationLimit=0
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource(
            "/org/operaton/bpm/engine/test/history/useroperationlog/SyncApiUserOperationLogLimitConfigurationTest.shouldThrowExceptionWhenConfigureZero.cfg.xml");

    // when engine startup
    assertThatThrownBy(() -> startEngineManaged(processEngineConfiguration))
    // then
    .isInstanceOf(ProcessEngineException.class)
    .hasMessage("Invalid configuration for logEntriesPerSyncOperationLimit. Configured value needs to be either -1 or greater than 0 but was 0.");
  }

  @Test
  void shouldThrowExceptionWhenConfigureLowNegativeValue() {
    // given configuration with logEntriesPerSyncOperationLimit=-10
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource(
            "/org/operaton/bpm/engine/test/history/useroperationlog/SyncApiUserOperationLogLimitConfigurationTest.shouldThrowExceptionWhenConfigureLowNegativeValue.cfg.xml");

    // when engine startup
    assertThatThrownBy(() -> startEngineManaged(processEngineConfiguration))
    // then
    .isInstanceOf(ProcessEngineException.class)
    .hasMessage("Invalid configuration for logEntriesPerSyncOperationLimit. Configured value needs to be either -1 or greater than 0 but was -10.");
  }

  private ProcessEngine startEngineManaged(ProcessEngineConfiguration config) {
    engine = config.buildProcessEngine();
    return engine;
  }

}
