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
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class DefaultMetricsConfigurationTest {
  private DefaultMetricsConfiguration defaultMetricsConfiguration;
  private final OperatonBpmProperties operatonBpmProperties = new OperatonBpmProperties();
  private final SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();

  @BeforeEach
  void setUp() {
    defaultMetricsConfiguration = new DefaultMetricsConfiguration(operatonBpmProperties);
    defaultMetricsConfiguration.init();

    invokeMethod(configuration, "initMetrics");
  }

  @Test
  void enabled() {
    assertThat(configuration.isMetricsEnabled()).isTrue();
    assertThat(operatonBpmProperties.getMetrics().isEnabled()).isTrue();

    operatonBpmProperties.getMetrics().setEnabled(false);
    defaultMetricsConfiguration.preInit(configuration);
    assertThat(configuration.isMetricsEnabled()).isFalse();

    operatonBpmProperties.getMetrics().setEnabled(true);
    defaultMetricsConfiguration.preInit(configuration);
    assertThat(configuration.isMetricsEnabled()).isTrue();
  }

  @Test
  void dbMetricsReporterActivate() {
    assertThat(configuration.isDbMetricsReporterActivate()).isTrue();
    assertThat(operatonBpmProperties.getMetrics().isDbReporterActivate()).isTrue();

    operatonBpmProperties.getMetrics().setDbReporterActivate(false);
    defaultMetricsConfiguration.preInit(configuration);
    assertThat(configuration.isDbMetricsReporterActivate()).isFalse();

    operatonBpmProperties.getMetrics().setDbReporterActivate(true);
    defaultMetricsConfiguration.preInit(configuration);
    assertThat(configuration.isDbMetricsReporterActivate()).isTrue();
  }
}
