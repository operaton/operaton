/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.operaton.bpm.engine.impl.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessEnginePreviewFeaturesConfigurationTest {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessEnginePreviewFeaturesConfigurationTest.class);

  private ProcessEngine processEngine;

  @AfterEach
  void unregisterEngine() {
    if (processEngine != null) {
      try {
        ProcessEngines.unregister(processEngine);
      } catch (Exception e) {
        LOG.warn("Error while unregistering process engine", e);
      }

      try {
        processEngine.close();
      } catch (Exception e) {
        LOG.warn("Error while closing process engine", e);
      } finally {
        processEngine = null;
      }
    }
  }

  @Test
  void shouldDefaultToFalseWhenNotConfigured() {
    // given
    NoSchemaStandaloneInMemProcessEngineConfiguration configuration =
        new NoSchemaStandaloneInMemProcessEngineConfiguration();

    // when
    processEngine = configuration.buildProcessEngine();

    // then
    assertThat(configuration.isPreviewFeaturesEnabled()).isFalse();
  }


  @Test
  void shouldPropagatePreviewFeaturesToDmnEngineConfiguration() {
    // given
    NoSchemaStandaloneInMemProcessEngineConfiguration configuration =
        new NoSchemaStandaloneInMemProcessEngineConfiguration();
    configuration.setPreviewFeaturesEnabled(true);

    // when
    processEngine = configuration.buildProcessEngine();

    // then
    DmnEngine dmnEngine = configuration.getDmnEngine();
    assertThat(dmnEngine).isNotNull();

    DefaultDmnEngineConfiguration dmnConfiguration =
        (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
    assertThat(dmnConfiguration.isPreviewFeaturesEnabled()).isTrue();
  }

  /**
   * <p>Test configuration that does not perform any schema operations when the
   * engine is built.</p>
   *
   * <p>This avoids triggering real database schema operations (e.g.
   * <code>ENGINE-03017</code>) while still fully initializing the process engine
   * configuration including the preview features logic.</p>
   */
  private static class NoSchemaStandaloneInMemProcessEngineConfiguration
      extends StandaloneInMemProcessEngineConfiguration {

    @Override
    public ProcessEngine buildProcessEngine() {
      init();
      return new NoSchemaProcessEngineImpl(this);
    }

    private static class NoSchemaProcessEngineImpl extends ProcessEngineImpl {
      NoSchemaProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
      }

      @Override
      protected void executeSchemaOperations() {
        // nop - do not execute create schema operations
      }
    }
  }
}
