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

class ProcessEnginePreviewFeaturesConfigurationTest {

  @AfterEach
  void clearSystemProperty() {
    System.clearProperty(ProcessEngineConfigurationImpl.PROPERTY_PREVIEW_FEATURES_ENABLED);
  }

  @Test
  void shouldDefaultToFalseWhenNotConfigured() {
    // given
    StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();

    // when
    configuration.buildProcessEngine();

    // then
    assertThat(configuration.isPreviewFeaturesEnabled()).isFalse();
  }

  @Test
  void shouldRespectExplicitConfigurationOverSystemProperty() {
    // given
    System.setProperty(ProcessEngineConfigurationImpl.PROPERTY_PREVIEW_FEATURES_ENABLED, "true");

    StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
    configuration.setPreviewFeaturesEnabled(false);

    // when
    configuration.buildProcessEngine();

    // then
    assertThat(configuration.isPreviewFeaturesEnabled()).isFalse();
  }

  @Test
  void shouldUseSystemPropertyWhenNotExplicitlyConfigured() {
    // given
    System.setProperty(ProcessEngineConfigurationImpl.PROPERTY_PREVIEW_FEATURES_ENABLED, "true");
    StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();

    // when
    configuration.buildProcessEngine();

    // then
    assertThat(configuration.isPreviewFeaturesEnabled()).isTrue();
  }

  @Test
  void shouldPropagatePreviewFeaturesToDmnEngineConfiguration() {
    // given
    StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
    configuration.setPreviewFeaturesEnabled(true);

    // when
    configuration.buildProcessEngine();

    // then
    DmnEngine dmnEngine = configuration.getDmnEngine();
    assertThat(dmnEngine).isNotNull();

    DefaultDmnEngineConfiguration dmnConfiguration = (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
    assertThat(dmnConfiguration.isPreviewFeaturesEnabled()).isTrue();
  }
}

