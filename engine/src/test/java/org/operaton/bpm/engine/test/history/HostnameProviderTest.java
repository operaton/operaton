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
package org.operaton.bpm.engine.test.history;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.event.HostnameProvider;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.metrics.MetricsReporterIdProvider;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.management.MetricIntervalValue;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class HostnameProviderTest {

  public static final String ENGINE_NAME = "TEST_ENGINE";
  public static final String STATIC_HOSTNAME = "STATIC";
  public static final String CUSTOM_HOSTNAME = "CUSTOM_HOST";
  public static final String CUSTOM_REPORTER = "CUSTOM_REPORTER";

  @Parameters(name = "Expected hostname: {3}, reporter: {4}")
  public static Collection<Object[]> data() {
    return List.of(new Object[][]{
        {null, null, null, ENGINE_NAME, ENGINE_NAME},
        {STATIC_HOSTNAME, null, null, STATIC_HOSTNAME, STATIC_HOSTNAME},
        {STATIC_HOSTNAME, new CustomHostnameProvider(), null, STATIC_HOSTNAME, STATIC_HOSTNAME},
        {STATIC_HOSTNAME, new CustomHostnameProvider(), new CustomMetricsReporterIdProvider(), STATIC_HOSTNAME, STATIC_HOSTNAME},
        {STATIC_HOSTNAME, null, new CustomMetricsReporterIdProvider(), STATIC_HOSTNAME, STATIC_HOSTNAME},
        {null, new CustomHostnameProvider(), null, CUSTOM_HOSTNAME, CUSTOM_HOSTNAME},
        {null, new CustomHostnameProvider(), new CustomMetricsReporterIdProvider(), CUSTOM_HOSTNAME, CUSTOM_HOSTNAME},
        {null, null, new CustomMetricsReporterIdProvider(), ENGINE_NAME, CUSTOM_REPORTER}
    });
  }

  @Parameter(0)
  public String hostname;
  @Parameter(1)
  public HostnameProvider hostnameProvider;
  @Parameter(2)
  public MetricsReporterIdProvider reporterProvider;
  @Parameter(3)
  public String expectedHostname;
  @Parameter(4)
  public String expectedReporter;

  ProcessEngineConfigurationImpl configuration;
  ProcessEngine engine;
  ManagementService managementService;

  @BeforeEach
  void setUp() {
    configuration =
        (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
            .createStandaloneInMemProcessEngineConfiguration();

    configuration
        .setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testHostnameProvider")
        .setProcessEngineName(ENGINE_NAME)
        .setHostname(hostname)
        .setHostnameProvider(hostnameProvider)
        .setMetricsReporterIdProvider(reporterProvider);

    engine = configuration.buildProcessEngine();
    configuration.getMetricsRegistry().markOccurrence("TEST", 1L);
    configuration.getDbMetricsReporter().reportNow();

    managementService = configuration.getManagementService();
  }

  @AfterEach
  void tearDown() {
    closeProcessEngine();
  }

  @TestTemplate
  void shouldUseCustomHostname() {
    // given a Process Engine with specified hostname parameters

    // when
    String customHostname = configuration.getHostname();

    // then
    assertThat(customHostname).containsIgnoringCase(expectedHostname);
  }

  @TestTemplate
  void shouldUseCustomMetricsReporterId() {
    // given a Process Engine with some specified hostname and metric properties

    // when
    List<MetricIntervalValue> metrics = managementService
                                                     .createMetricsQuery()
                                                     .limit(1)
                                                     .interval();

    // then
    assertThat(metrics).hasSize(1);
    assertThat(metrics.get(0).getReporter()).containsIgnoringCase(expectedReporter);
  }

  public static class CustomHostnameProvider implements HostnameProvider {
    @Override
    public String getHostname(ProcessEngineConfigurationImpl processEngineConfiguration) {
      return CUSTOM_HOSTNAME;
    }
  }

  public static class CustomMetricsReporterIdProvider implements MetricsReporterIdProvider {
    @Override
    public String provideId(ProcessEngine processEngine) {
      return CUSTOM_REPORTER;
    }
  }

  protected void closeProcessEngine() {
    final HistoryService historyService = engine.getHistoryService();
    configuration.getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {

      List<Job> jobs = historyService.findHistoryCleanupJobs();
      for (Job job: jobs) {
        commandContext.getJobManager().deleteJob((JobEntity) job);
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
      }

      //cleanup "detached" historic job logs
      final List<HistoricJobLog> list = historyService.createHistoricJobLogQuery().list();
      for (HistoricJobLog jobLog: list) {
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobLog.getJobId());
      }

      commandContext.getMeterLogManager().deleteAll();

      return null;
    });

    engine.close();
    engine = null;
  }
}
