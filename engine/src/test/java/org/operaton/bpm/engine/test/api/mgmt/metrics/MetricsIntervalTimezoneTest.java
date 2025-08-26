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

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.metrics.MetricsRegistry;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.MetricIntervalValue;

import static org.operaton.bpm.engine.management.Metrics.ACTIVTY_INSTANCE_START;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Represents a test suite for the metrics interval query to check if the
 * timestamps are read in a correct time zone.
 * <p>
 * This was a problem before the column MILLISECONDS_ was added.
 * </p>
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class MetricsIntervalTimezoneTest extends AbstractMetricsIntervalTest {

  @Test
  void testTimestampIsInCorrectTimezone() {
    //given generated metric data started at DEFAULT_INTERVAL ends at 3 * DEFAULT_INTERVAL

    //when metric query is executed (hint last interval is returned as first)
    List<MetricIntervalValue> metrics = managementService.createMetricsQuery().limit(1).interval();

    //then metric interval time should be less than FIRST_INTERVAL + 3 * DEFAULT_INTERVAL
    //and larger than first interval time, if not than we have a timezone problem
    long metricIntervalTime = metrics.get(0).getTimestamp().getTime();
    assertThat(metricIntervalTime)
      .isLessThan(firstInterval.plusMinutes(3 * DEFAULT_INTERVAL).getMillis())
      .isGreaterThan(firstInterval.getMillis());

    //when current time is used and metric is reported
    Date currentTime = new Date();
    MetricsRegistry metricsRegistry = processEngineConfiguration.getMetricsRegistry();
    ClockUtil.setCurrentTime(currentTime);
    metricsRegistry.markOccurrence(ACTIVTY_INSTANCE_START, 1);
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    //then current time should be larger than metric interval time
    List<MetricIntervalValue> m2 = managementService.createMetricsQuery().limit(1).interval();
    assertThat(m2.get(0).getTimestamp().getTime()).isLessThan(currentTime.getTime());
  }
}
