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
package org.operaton.bpm.qa.performance.engine.query;
import java.util.List;

import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.qa.performance.engine.junit.ProcessEnginePerformanceTestCase;
import org.operaton.bpm.qa.performance.engine.loadgenerator.tasks.GenerateMetricsTask;
import org.operaton.bpm.qa.performance.engine.steps.MetricIntervalStep;
import org.operaton.bpm.qa.performance.engine.steps.MetricSumStep;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class MetricPerformanceTest extends ProcessEnginePerformanceTestCase {
  public String name;
  public Date startDate;
  public Date endDate;

  public static Iterable<Object[]> params() {
    return List.of(new Object[][]
    {
      {null,null,null},
      {Metrics.ACTIVTY_INSTANCE_START, null, null},
      {Metrics.ACTIVTY_INSTANCE_START, new Date(0), null},
      {null, new Date(0), null},
      {null, null, new Date(GenerateMetricsTask.INTERVAL*250)},
      {Metrics.ACTIVTY_INSTANCE_START, null, new Date(GenerateMetricsTask.INTERVAL*250)},
      {Metrics.ACTIVTY_INSTANCE_START, new Date(0), new Date(GenerateMetricsTask.INTERVAL*250)},
      {null, new Date(0), new Date(GenerateMetricsTask.INTERVAL*250)}
    });
  }

  @MethodSource("params")
  @ParameterizedTest(name = "{index}")
  void metricInterval(String name, Date startDate, Date endDate) {
    initMetricPerformanceTest(name, startDate, endDate);
    performanceTest().step(new MetricIntervalStep(name, startDate, endDate, engine)).run();
  }

  @MethodSource("params")
  @ParameterizedTest(name = "{index}")
  void metricSum(String name, Date startDate, Date endDate) {
    initMetricPerformanceTest(name, startDate, endDate);
    performanceTest().step(new MetricSumStep(name, startDate, endDate, engine)).run();
  }

  public void initMetricPerformanceTest(String name, Date startDate, Date endDate) {
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
  }
}
