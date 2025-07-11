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

import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.impl.metrics.MetricsRegistry;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * Represents the abstract metrics interval test class, which contains methods
 * for generating metrics and clean up afterwards.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public abstract class AbstractMetricsIntervalTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testExtension = new ProcessEngineTestExtension(engineExtension);

  protected static final String REPORTER_ID = "REPORTER_ID";
  protected static final int DEFAULT_INTERVAL = 15;
  protected static final int DEFAULT_INTERVAL_MILLIS = 15 * 60 * 1000;
  protected static final int MIN_OCCURENCE = 1;
  protected static final int MAX_OCCURENCE = 250;

  protected RuntimeService runtimeService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected String lastReporterId;
  protected DateTime firstInterval;
  protected int metricsCount;
  protected MetricsRegistry metricsRegistry;
  protected Random rand;

  protected void generateMeterData(long dataCount, long interval) {
    //set up for randomnes
    Set<String> metricNames = metricsRegistry.getDbMeters().keySet();
    metricsCount = metricNames.size();

    //start date is the default interval since mariadb can't set 0 as timestamp
    long startDate = DEFAULT_INTERVAL_MILLIS;
    firstInterval = new DateTime(startDate);
    //we will have 5 metric reports in an interval
    int dataPerInterval = 5;

    //generate data
    for (int i = 0; i < dataCount; i++) {
      //calulate diff so timer can be set correctly
      long diff = interval / dataPerInterval;
      for (int j = 0; j < dataPerInterval; j++) {
        ClockUtil.setCurrentTime(new Date(startDate));
        //generate random count of data per interv
        //for each metric
        reportMetrics();
        startDate += diff;
      }
    }
  }

  protected void reportMetrics() {
    for (String metricName : metricsRegistry.getDbMeters().keySet()) {
      //mark random occurence
      long occurence = rand.nextInt((MAX_OCCURENCE - MIN_OCCURENCE) + 1) + MIN_OCCURENCE;
      metricsRegistry.markOccurrence(metricName, occurence);
    }
    //report logged metrics
    processEngineConfiguration.getDbMetricsReporter().reportNow();
  }

  protected void clearMetrics() {
    clearLocalMetrics();
    managementService.deleteMetrics(null);
  }

  protected void clearLocalMetrics() {
    Collection<Meter> meters = processEngineConfiguration.getMetricsRegistry().getDbMeters().values();
    for (Meter meter : meters) {
      meter.getAndClear();
    }
  }

  @BeforeEach
  public void initMetrics() {
    runtimeService = engineExtension.getRuntimeService();
    processEngineConfiguration = engineExtension.getProcessEngineConfiguration();
    managementService = engineExtension.getManagementService();

    //clean up before start
    clearMetrics();

    //init metrics
    processEngineConfiguration.setDbMetricsReporterActivate(true);
    lastReporterId = processEngineConfiguration.getDbMetricsReporter().getMetricsCollectionTask().getReporter();
    processEngineConfiguration.getDbMetricsReporter().setReporterId(REPORTER_ID);
    metricsRegistry = processEngineConfiguration.getMetricsRegistry();
    rand = new Random(new Date().getTime());
    generateMeterData(3, DEFAULT_INTERVAL_MILLIS);
  }

  @AfterEach
  public void cleanUp() {
    ClockUtil.reset();
    processEngineConfiguration.setDbMetricsReporterActivate(false);
    processEngineConfiguration.getDbMetricsReporter().setReporterId(lastReporterId);
    clearMetrics();
  }
}
