/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.engine.test.jobexecutor;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests configuration of the SKIP LOCKED feature.
 * Verifies that the property is properly initialized and can be configured.
 */
class JobExecutorSkipLockedConfigurationTest {

  @Test
  void testDefaultConfigurationIsFalse() {
    // given: a default process engine configuration
    ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
    config.setJdbcUrl("jdbc:h2:mem:test-default-config");
    config.setDatabaseSchemaUpdate("create-drop");

    // when: building the engine
    ProcessEngine engine = config.buildProcessEngine();

    try {
      // then: skip locked is disabled by default (for backward compatibility)
      assertThat(((ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration())
        .isJobExecutorAcquireWithSkipLocked()).isFalse();
    } finally {
      engine.close();
    }
  }

  @Test
  void testConfigurationCanBeEnabled() {
    // given: a configuration with skip locked enabled
    ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
    config.setJdbcUrl("jdbc:h2:mem:test-enabled-config");
    config.setDatabaseSchemaUpdate("create-drop");
    config.setJobExecutorAcquireWithSkipLocked(true);

    // when: building the engine
    ProcessEngine engine = config.buildProcessEngine();

    try {
      // then: skip locked is enabled
      assertThat(((ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration())
        .isJobExecutorAcquireWithSkipLocked()).isTrue();
    } finally {
      engine.close();
    }
  }

  @Test
  void testConfigurationCanBeDisabled() {
    // given: a configuration with skip locked explicitly disabled
    ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
    config.setJdbcUrl("jdbc:h2:mem:test-disabled-config");
    config.setDatabaseSchemaUpdate("create-drop");
    config.setJobExecutorAcquireWithSkipLocked(false);

    // when: building the engine
    ProcessEngine engine = config.buildProcessEngine();

    try {
      // then: skip locked is disabled
      assertThat(((ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration())
        .isJobExecutorAcquireWithSkipLocked()).isFalse();
    } finally {
      engine.close();
    }
  }

  @Test
  void testConfigurationToggle() {
    // given: a configuration
    ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
    config.setJdbcUrl("jdbc:h2:mem:test-toggle-config");
    config.setDatabaseSchemaUpdate("create-drop");

    // when: toggling the setting before engine build
    assertThat(config.isJobExecutorAcquireWithSkipLocked()).isFalse();
    config.setJobExecutorAcquireWithSkipLocked(true);
    assertThat(config.isJobExecutorAcquireWithSkipLocked()).isTrue();
    config.setJobExecutorAcquireWithSkipLocked(false);
    assertThat(config.isJobExecutorAcquireWithSkipLocked()).isFalse();

    // then: the setting is correctly stored
    ProcessEngine engine = config.buildProcessEngine();

    try {
      assertThat(((ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration())
        .isJobExecutorAcquireWithSkipLocked()).isFalse();
    } finally {
      engine.close();
    }
  }

  @Test
  void testConfigurationPersistsAfterEngineBuild() {
    // given: a configuration with skip locked enabled
    ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
    config.setJdbcUrl("jdbc:h2:mem:test-persist-config");
    config.setDatabaseSchemaUpdate("create-drop");
    config.setJobExecutorAcquireWithSkipLocked(true);

    // when: building the engine
    ProcessEngine engine = config.buildProcessEngine();

    try {
      ProcessEngineConfigurationImpl runtimeConfig =
        (ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration();

      // then: the configuration is available at runtime
      assertThat(runtimeConfig.isJobExecutorAcquireWithSkipLocked()).isTrue();

      // and: can be modified at runtime (though not recommended in production)
      runtimeConfig.setJobExecutorAcquireWithSkipLocked(false);
      assertThat(runtimeConfig.isJobExecutorAcquireWithSkipLocked()).isFalse();

    } finally {
      engine.close();
    }
  }

  @Test
  void testConfigurationWorksWithOtherJobExecutorSettings() {
    // given: a configuration with multiple job executor settings
    ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
    config.setJdbcUrl("jdbc:h2:mem:test-combined-config");
    config.setDatabaseSchemaUpdate("create-drop");
    config.setJobExecutorAcquireWithSkipLocked(true);
    config.setJobExecutorAcquireByPriority(true);
    config.setJobExecutorAcquireByDueDate(true);

    // when: building the engine
    ProcessEngine engine = config.buildProcessEngine();

    try {
      ProcessEngineConfigurationImpl runtimeConfig =
        (ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration();

      // then: all settings are correctly preserved
      assertThat(runtimeConfig.isJobExecutorAcquireWithSkipLocked()).isTrue();
      assertThat(runtimeConfig.isJobExecutorAcquireByPriority()).isTrue();
      assertThat(runtimeConfig.isJobExecutorAcquireByDueDate()).isTrue();
    } finally {
      engine.close();
    }
  }
}
