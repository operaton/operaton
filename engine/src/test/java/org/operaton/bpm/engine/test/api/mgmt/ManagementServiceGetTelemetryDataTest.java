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
package org.operaton.bpm.engine.test.api.mgmt;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.ManagementServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.metrics.MetricsRegistry;
import org.operaton.bpm.engine.impl.telemetry.dto.TelemetryDataImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.ParseUtil;
import org.operaton.bpm.engine.telemetry.ApplicationServer;
import org.operaton.bpm.engine.telemetry.Metric;
import org.operaton.bpm.engine.telemetry.TelemetryData;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.operaton.bpm.engine.management.Metrics.DECISION_INSTANCES;
import static org.operaton.bpm.engine.management.Metrics.EXECUTED_DECISION_ELEMENTS;
import static org.operaton.bpm.engine.management.Metrics.FLOW_NODE_INSTANCES;
import static org.operaton.bpm.engine.management.Metrics.PROCESS_INSTANCES;
import static org.assertj.core.api.Assertions.assertThat;

class ManagementServiceGetTelemetryDataTest {

  protected static final String IS_TELEMETRY_ENABLED_CMD_NAME = "IsTelemetryEnabledCmd";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  protected ProcessEngineConfigurationImpl configuration;
  protected ManagementServiceImpl managementService;

  protected MetricsRegistry metricsRegistry;

  protected TelemetryDataImpl defaultTelemetryData;

  @BeforeEach
  void setup() {
    managementService = (ManagementServiceImpl) engineRule.getManagementService();
    metricsRegistry = configuration.getMetricsRegistry();

    defaultTelemetryData = new TelemetryDataImpl(configuration.getTelemetryData());

    clearTelemetry();
  }

  @AfterEach
  void tearDown() {
    clearTelemetry();

    configuration.setTelemetryData(defaultTelemetryData);
  }

  protected void clearTelemetry() {
    metricsRegistry.clearDiagnosticsMetrics();
    managementService.deleteMetrics(null);
    configuration.getDiagnosticsRegistry().clear();
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldReturnTelemetryData_TelemetryDisabled() {
    // given
    managementService.toggleTelemetry(false);

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getInstallation()).isNotEmpty();
  }

  @Test
  void shouldReturnProductInfo() {
    // given default configuration

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    assertThat(telemetryData.getProduct().getName()).isEqualTo("Operaton BPM Runtime");
    assertThat(telemetryData.getProduct().getEdition()).isEqualTo("community");
    assertThat(telemetryData.getProduct().getVersion()).isEqualTo(ParseUtil.parseProcessEngineVersion(true).getVersion());
  }

  @Test
  void shouldReturnDatabaseInfo() {
    // given default configuration

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    assertThat(telemetryData.getProduct().getInternals().getDatabase().getVendor())
        .isEqualTo(engineRule.getProcessEngineConfiguration().getDatabaseVendor());
    assertThat(telemetryData.getProduct().getInternals().getDatabase().getVersion())
        .isEqualTo(engineRule.getProcessEngineConfiguration().getDatabaseVersion());
  }

  @Test
  void shouldReturnJDKInfo() {
    // given default configuration

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    assertThat(telemetryData.getProduct().getInternals().getJdk().getVendor())
        .isEqualTo(ParseUtil.parseJdkDetails().getVendor());
    assertThat(telemetryData.getProduct().getInternals().getJdk().getVersion())
        .isEqualTo(ParseUtil.parseJdkDetails().getVersion());
  }

  @Test
  void shouldReturnWebapps() {
    // given
    managementService.addWebappToTelemetry("cockpit");
    managementService.addWebappToTelemetry("admin");

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    assertThat(telemetryData.getProduct().getInternals().getWebapps()).containsExactlyInAnyOrder("cockpit", "admin");
  }

  @Test
  void shouldReturnApplicationServerInfo() {
    // given
    managementService.addApplicationServerInfoToTelemetry("Apache Tomcat/10.0.1");

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    ApplicationServer applicationServer = telemetryData.getProduct().getInternals().getApplicationServer();
    assertThat(applicationServer.getVendor()).isEqualTo("Apache Tomcat");
    assertThat(applicationServer.getVersion()).isEqualTo("Apache Tomcat/10.0.1");
  }

  @Test
  void shouldStartWithCommandCountZero() {
    // given default telemetry data and empty telemetry registry

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    assertThat(telemetryData.getProduct().getInternals().getCommands()).isEmpty();
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldNotResetCommandCount() {
    // given default telemetry data and empty telemetry registry
    // create command data
    managementService.isTelemetryEnabled();

    // when invoking getter twice
    managementService.getTelemetryData();
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then count should not reset
    assertThat(telemetryData.getProduct().getInternals().getCommands().get(IS_TELEMETRY_ENABLED_CMD_NAME).getCount()).isOne();
  }

  @Test
  void shouldStartWithMetricsCountZero() {
    // given default telemetry data and empty telemetry registry

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    Map<String, Metric> metrics = telemetryData.getProduct().getInternals().getMetrics();
    assertThat(metrics).containsOnlyKeys(FLOW_NODE_INSTANCES, PROCESS_INSTANCES, EXECUTED_DECISION_ELEMENTS,
        DECISION_INSTANCES);
    assertThat(metrics.get(FLOW_NODE_INSTANCES).getCount()).isZero();
    assertThat(metrics.get(PROCESS_INSTANCES).getCount()).isZero();
    assertThat(metrics.get(EXECUTED_DECISION_ELEMENTS).getCount()).isZero();
    assertThat(metrics.get(DECISION_INSTANCES).getCount()).isZero();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldNotResetMetricsCount() {
    // given default telemetry data and empty telemetry registry
    // create metrics data
    engineRule.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");

    // when invoking getter twice
    managementService.getTelemetryData();
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then count should not reset
    assertThat(telemetryData.getProduct().getInternals().getMetrics().get(FLOW_NODE_INSTANCES).getCount()).isEqualTo(2);
    assertThat(telemetryData.getProduct().getInternals().getMetrics().get(PROCESS_INSTANCES).getCount()).isOne();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldCollectMetrics_TelemetryDisabled() {
    // given default configuration

    engineRule.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");

    // when
    TelemetryData telemetryDataAfterPiStart = managementService.getTelemetryData();

    // then
    assertThat(telemetryDataAfterPiStart.getProduct().getInternals().getMetrics().get(PROCESS_INSTANCES).getCount()).isOne();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @SuppressWarnings("deprecation")
  void shouldCollectCommands_TelemetryDisabled() {
    // given default configuration

    // trigger Command invocation
    managementService.isTelemetryEnabled();

    // when
    TelemetryData telemetryDataAfterPiStart = managementService.getTelemetryData();

    // then
    assertThat(telemetryDataAfterPiStart.getProduct().getInternals().getCommands().get(IS_TELEMETRY_ENABLED_CMD_NAME)
      .getCount()).isOne();
  }

  @Test
  void shouldSetDataCollectionTimeFrameToEngineStartTimeWhenTelemetryDisabled() {
    // given default telemetry data and empty telemetry registry
    // current time after engine startup but before fetching telemetry data
    Date beforeGetTelemetry = ClockUtil.getCurrentTime();
    // move clock by one second to pass some time before fetching telemetry
    ClockUtil.offset(1000L);

    // when
    TelemetryData telemetryData = managementService.getTelemetryData();

    // then
    assertThat(telemetryData.getProduct().getInternals().getDataCollectionStartDate()).isBefore(beforeGetTelemetry);
  }

  @Test
  void shouldNotResetCollectionTimeFrameAfterGetTelemetryWhenTelemetryDisabled() {
    // given default telemetry data and empty telemetry registry
    TelemetryData initialTelemetryData = managementService.getTelemetryData();

    // when fetching telemetry data again
    TelemetryData secondTelemetryData = managementService.getTelemetryData();

    // then the data collection time frame should not reset after the first call
    assertThat(initialTelemetryData.getProduct().getInternals().getDataCollectionStartDate())
        .isEqualTo(secondTelemetryData.getProduct().getInternals().getDataCollectionStartDate());
  }

  @Test
  void shouldNotResetCollectionTimeFrameAfterGetTelemetry() {
    // given default telemetry data and empty telemetry registry
    // and default configuration

    managementService.toggleTelemetry(true);
    TelemetryData initialTelemetryData = managementService.getTelemetryData();

    // when fetching telemetry data again
    TelemetryData secondTelemetryData = managementService.getTelemetryData();

    // then the data collection time frame should not reset after the first call
    assertThat(initialTelemetryData.getProduct().getInternals().getDataCollectionStartDate())
        .isEqualTo(secondTelemetryData.getProduct().getInternals().getDataCollectionStartDate());
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldNotResetCollectionTimeFrameAfterToggleTelemetry() {
    // given default telemetry data and empty telemetry registry
    // and default configuration
    Date beforeToggleTelemetry = managementService.getTelemetryData().getProduct().getInternals()
        .getDataCollectionStartDate();

    // when
    managementService.toggleTelemetry(false);

    // then
    Date afterToggleTelemetry = managementService.getTelemetryData().getProduct().getInternals()
        .getDataCollectionStartDate();

    assertThat(beforeToggleTelemetry)
      .isNotNull()
      .isEqualTo(afterToggleTelemetry);
  }

}
