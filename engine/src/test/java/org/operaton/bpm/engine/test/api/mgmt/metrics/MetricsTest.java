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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 *
 */
class MetricsTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testExtension = new ProcessEngineTestExtension(engineExtension);

  protected RuntimeService runtimeService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;

  protected void clearMetrics() {
    Collection<Meter> meters = processEngineConfiguration.getMetricsRegistry().getDbMeters().values();
    for (Meter meter : meters) {
      meter.getAndClear();
    }
    managementService.deleteMetrics(null);
    processEngineConfiguration.setDbMetricsReporterActivate(false);
  }

  @BeforeEach
  void initMetrics() {
    //clean up before start
    clearMetrics();
    testExtension.deploy(Bpmn.createExecutableProcess("testProcess")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .manualTask()
        .endEvent()
        .done());
  }

  @AfterEach
  void cleanUp() {
    clearMetrics();
  }

  @Test
  void testStartAndEndMetricsAreEqual() {
    // given

    //when
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    //then end and start metrics are equal
    long start = managementService.createMetricsQuery()
                                  .name(Metrics.ACTIVTY_INSTANCE_START)
                                  .sum();
    long end = managementService.createMetricsQuery()
                                  .name(Metrics.ACTIVTY_INSTANCE_END)
                                  .sum();
    assertEquals(end, start);
    assertEquals(start, managementService.createMetricsQuery().name(Metrics.FLOW_NODE_INSTANCES).sum());
  }

  @Test
  void testEndMetricWithWaitState() {
    //given
    testExtension.deploy(Bpmn.createExecutableProcess("userProcess")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("Task")
        .endEvent()
        .done());

    //when
    runtimeService.startProcessInstanceByKey("userProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    //then end is not equal to start since a wait state exist at Task
    long start = managementService.createMetricsQuery()
                                  .name(Metrics.ACTIVTY_INSTANCE_START)
                                  .sum();
    long end = managementService.createMetricsQuery()
                                  .name(Metrics.ACTIVTY_INSTANCE_END)
                                  .sum();
    assertThat(start).isNotEqualTo(end);
    assertEquals(start, managementService.createMetricsQuery().name(Metrics.FLOW_NODE_INSTANCES).sum());
    assertEquals(2, start);
    assertEquals(1, end);

    //when completing the task
    String id = engineExtension.getTaskService().createTaskQuery().processDefinitionKey("userProcess").singleResult().getId();
    engineExtension.getTaskService().complete(id);

    //then start and end is equal
    start = managementService.createMetricsQuery()
                                  .name(Metrics.ACTIVTY_INSTANCE_START)
                                  .sum();
    end = managementService.createMetricsQuery()
                                  .name(Metrics.ACTIVTY_INSTANCE_END)
                                  .sum();
    assertEquals(end, start);
    assertEquals(start, managementService.createMetricsQuery().name(Metrics.FLOW_NODE_INSTANCES).sum());
  }

  @Test
  void testDeleteMetrics() {

    // given
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // a count of 7 (start, end and root process instance)
    assertEquals(7l, managementService.createMetricsQuery()
            .sum());

    // if
    // we delete with timestamp "null"
    managementService.deleteMetrics(null);

    // then
    // all entries are deleted
    assertEquals(0l, managementService.createMetricsQuery()
            .sum());
  }

  @Test
  void testDeleteMetricsWithTimestamp() {

    // given
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // a count of 7 (start, end and root process instance)
    assertEquals(7l, managementService.createMetricsQuery()
            .sum());

    // if
    // we delete with timestamp older or equal to the timestamp of the log entry
    managementService.deleteMetrics(ClockUtil.getCurrentTime());

    // then
    // all entries are deleted
    assertEquals(0l, managementService.createMetricsQuery()
            .name(Metrics.ACTIVTY_INSTANCE_START)
            .sum());
  }

  @Test
  void testDeleteMetricsWithTimestampBefore() {

    // given
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // a count of 7 (start, end and root process instance)
    assertEquals(7l, managementService.createMetricsQuery()
            .sum());

    // if
    // we delete with timestamp before the timestamp of the log entry
    managementService.deleteMetrics(new Date(ClockUtil.getCurrentTime().getTime() - 10000));

    // then
    // the entires are NOT deleted
    assertEquals(7l, managementService.createMetricsQuery()
            .sum());
  }

  @Test
  void testDeleteMetricsWithReporterId() {
    // indicate that db metrics reporter is active (although it is not)
    processEngineConfiguration.setDbMetricsReporterActivate(true);

    // given
    processEngineConfiguration.getDbMetricsReporter().setReporterId("reporter1");
    runtimeService.startProcessInstanceByKey("testProcess");
    managementService.reportDbMetricsNow();

    processEngineConfiguration.getDbMetricsReporter().setReporterId("reporter2");
    runtimeService.startProcessInstanceByKey("testProcess");
    managementService.reportDbMetricsNow();

    assertEquals(3l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START).reporter("reporter1")
            .sum());

    // when the metrics for reporter1 are deleted
    managementService.deleteMetrics(null, "reporter1");

    // then
    assertEquals(0l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START).reporter("reporter1")
            .sum());
    assertEquals(3l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START).reporter("reporter2")
            .sum());

    // cleanup
    processEngineConfiguration.setDbMetricsReporterActivate(false);
    processEngineConfiguration.getDbMetricsReporter().setReporterId(null);
  }

  @Test
  void testReportNow() {
    // indicate that db metrics reporter is active (although it is not)
    processEngineConfiguration.setDbMetricsReporterActivate(true);

    // given
    runtimeService.startProcessInstanceByKey("testProcess");

    // when
    managementService.reportDbMetricsNow();

    // then the metrics have been reported
    assertEquals(3l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START)
            .sum());

    // cleanup
    processEngineConfiguration.setDbMetricsReporterActivate(false);
  }

  @Test
  void testReportNowIfMetricsIsDisabled() {
    boolean defaultIsMetricsEnabled = processEngineConfiguration.isMetricsEnabled();

    try {
      // given
      processEngineConfiguration.setMetricsEnabled(false);

      // when/then
      assertThatThrownBy(() -> managementService.reportDbMetricsNow())
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Metrics reporting is disabled");
    }
    finally {
      // reset metrics setting
      processEngineConfiguration.setMetricsEnabled(defaultIsMetricsEnabled);
    }
  }

  @Test
  void testReportNowIfReporterIsNotActive() {
    boolean defaultIsMetricsEnabled = processEngineConfiguration.isMetricsEnabled();
    boolean defaultIsMetricsReporterActivate = processEngineConfiguration.isDbMetricsReporterActivate();

    try {
      // given
      processEngineConfiguration.setMetricsEnabled(true);
      processEngineConfiguration.setDbMetricsReporterActivate(false);

      // when/then
      assertThatThrownBy(() -> managementService.reportDbMetricsNow())
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Metrics reporting to database is disabled");
    } finally {
      processEngineConfiguration.setMetricsEnabled(defaultIsMetricsEnabled);
      processEngineConfiguration.setDbMetricsReporterActivate(defaultIsMetricsReporterActivate);
    }
  }

  @Test
  void testQuery() {
    // given
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // then (query assertions)
    assertEquals(0l, managementService.createMetricsQuery().name("UNKNOWN").sum());
    assertEquals(3l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START).sum());

    assertEquals(7l, managementService.createMetricsQuery().sum());
    assertEquals(7l, managementService.createMetricsQuery().startDate(new Date(1000)).sum());
    assertEquals(7l, managementService.createMetricsQuery().startDate(new Date(1000))
            .endDate(new Date(ClockUtil.getCurrentTime().getTime() + 2000l)).sum()); // + 2000 for milliseconds imprecision on some databases (MySQL)
    assertEquals(0l, managementService.createMetricsQuery().startDate(new Date(ClockUtil.getCurrentTime().getTime() + 1000l)).sum());
    assertEquals(0l, managementService.createMetricsQuery().startDate(new Date(ClockUtil.getCurrentTime().getTime() + 1000l)).endDate(ClockUtil.getCurrentTime()).sum());

    // given
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // then (query assertions)
    assertEquals(14l, managementService.createMetricsQuery().sum());
    assertEquals(14l, managementService.createMetricsQuery().startDate(new Date(1000)).sum());
    assertEquals(14l, managementService.createMetricsQuery().startDate(new Date(1000)).endDate(new Date(ClockUtil.getCurrentTime().getTime() + 2000l)).sum()); // + 2000 for milliseconds imprecision on some databases (MySQL)
    assertEquals(0l, managementService.createMetricsQuery().startDate(new Date(ClockUtil.getCurrentTime().getTime() + 1000l)).sum());
    assertEquals(0l, managementService.createMetricsQuery().startDate(new Date(ClockUtil.getCurrentTime().getTime() + 1000l)).endDate(ClockUtil.getCurrentTime()).sum());
  }

  @Test
  void testQueryEndDateExclusive() {
    // given
    // note: dates should be exact seconds due to missing milliseconds precision on
    // older mysql versions
    // cannot insert 1970-01-01 00:00:00 into MySQL
    ClockUtil.setCurrentTime(new Date(5000L));
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    ClockUtil.setCurrentTime(new Date(6000L));
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    ClockUtil.setCurrentTime(new Date(7000L));
    runtimeService.startProcessInstanceByKey("testProcess");
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // then Query#startDate is inclusive and Query#endDate is exclusive
    assertEquals(21l, managementService.createMetricsQuery().sum());
    assertEquals(21l, managementService.createMetricsQuery().startDate(new Date(0)).sum());
    assertEquals(14l, managementService.createMetricsQuery().startDate(new Date(0)).endDate(new Date(7000L)).sum());
    assertEquals(21l, managementService.createMetricsQuery().startDate(new Date(0)).endDate(new Date(8000L)).sum());

  }

  @Test
  void testReportWithReporterId() {
    // indicate that db metrics reporter is active (although it is not)
    processEngineConfiguration.setDbMetricsReporterActivate(true);

    // given

    // when
    processEngineConfiguration.getDbMetricsReporter().setReporterId("reporter1");
    runtimeService.startProcessInstanceByKey("testProcess");
    managementService.reportDbMetricsNow();

    // and
    processEngineConfiguration.getDbMetricsReporter().setReporterId("reporter2");
    runtimeService.startProcessInstanceByKey("testProcess");
    managementService.reportDbMetricsNow();

    // then the metrics have been reported
    assertEquals(6l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START)
            .sum());

    // and are grouped by reporter
    assertEquals(3l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START).reporter("reporter1")
            .sum());
    assertEquals(3l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START).reporter("reporter2")
            .sum());
    assertEquals(0l, managementService.createMetricsQuery().name(Metrics.ACTIVTY_INSTANCE_START).reporter("aNonExistingReporter")
            .sum());

    // cleanup
    processEngineConfiguration.setDbMetricsReporterActivate(false);
    processEngineConfiguration.getDbMetricsReporter().setReporterId(null);
  }

}
