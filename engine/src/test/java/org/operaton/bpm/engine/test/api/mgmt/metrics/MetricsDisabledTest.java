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
package org.operaton.bpm.engine.test.api.mgmt.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Asserts engine functionality is metrics are disabled
 *
 * @author Daniel Meyer
 *
 */
class MetricsDisabledTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .configurationResource("org/operaton/bpm/engine/test/api/mgmt/metrics/metricsDisabledTest.cfg.xml").build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;

  // (to run, remove "FAILING" from methodname)
  @Test
  void testQueryMetricsIfMetricsIsDisabled() {

    // given
    // that the metrics are disabled (see xml configuration referenced in constructor)
    assertThat(processEngineConfiguration.isMetricsEnabled()).isFalse();
    assertThat(processEngineConfiguration.isDbMetricsReporterActivate()).isFalse();

    // then
    // it is possible to execute a query
    managementService.createMetricsQuery().sum();

  }

  @Test
  void testReportNowIfMetricsDisabled() {

    // given
    // that the metrics reporter is disabled
    assertThat(processEngineConfiguration.isDbMetricsReporterActivate()).isFalse();

    // when/then
    // I cannot invoke
    assertThatThrownBy(() -> managementService.reportDbMetricsNow())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Metrics reporting is disabled");
  }
}
