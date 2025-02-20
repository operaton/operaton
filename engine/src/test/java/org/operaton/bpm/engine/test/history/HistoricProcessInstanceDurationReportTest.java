/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.history;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import static org.operaton.bpm.engine.query.PeriodUnit.MONTH;
import static org.operaton.bpm.engine.query.PeriodUnit.QUARTER;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.DurationReportResult;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceReport;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.query.PeriodUnit;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
public class HistoricProcessInstanceDurationReportTest extends PluggableProcessEngineTest {

  private final Random random = new Random();

  @Test
  public void testDurationReportByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
      .periodUnit(MONTH)
      // period: 01 (January)
      .startAndCompleteProcessInstance("process", 2016, 0, 1, 10, 0) // 01.01.2016 10:00
      .startAndCompleteProcessInstance("process", 2016, 0, 1, 10, 0) // 01.01.2016 10:00
      .startAndCompleteProcessInstance("process", 2016, 0, 1, 10, 0) // 01.01.2016 10:00
      .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testTwoInstancesInSamePeriodByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        // period: 01 (January)
        .startAndCompleteProcessInstance("process", 2016, 0, 1, 10, 0) // 01.01.2016 10:00
        .startAndCompleteProcessInstance("process", 2016, 0, 15, 10, 0) // 15.01.2016 10:00
        .startAndCompleteProcessInstance("process", 2016, 0, 15, 10, 0) // 15.01.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testDurationReportInDifferentPeriodsByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        // period: 11 (November)
        .startAndCompleteProcessInstance("process", 2015, 10, 1, 10, 0) // 01.11.2015 10:00
        // period: 12 (December)
        .startAndCompleteProcessInstance("process", 2015, 11, 1, 10, 0) // 01.12.2015 10:00
        // period: 01 (January)
        .startAndCompleteProcessInstance("process", 2016, 0, 1, 10, 0) // 01.01.2016 10:00
        // period: 02 (February)
        .startAndCompleteProcessInstance("process", 2016, 1, 1, 10, 0) // 01.02.2016 10:00
        // period: 03 (March)
        .startAndCompleteProcessInstance("process", 2016, 2, 1, 10, 0) // 01.03.2016 10:00
        // period: 04 (April)
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        // period: 05 (May)
        .startAndCompleteProcessInstance("process", 2016, 4, 1, 10, 0) // 01.05.2016 10:00
        // period: 06 (June)
        .startAndCompleteProcessInstance("process", 2016, 5, 1, 10, 0) // 01.06.2016 10:00
        // period: 07 (July)
        .startAndCompleteProcessInstance("process", 2016, 6, 1, 10, 0) // 01.07.2016 10:00
        // period: 08 (August)
        .startAndCompleteProcessInstance("process", 2016, 7, 1, 10, 0) // 01.08.2016 10:00
        // period: 09 (September)
        .startAndCompleteProcessInstance("process", 2016, 8, 1, 10, 0) // 01.09.2016 10:00
        // period: 10 (October)
        .startAndCompleteProcessInstance("process", 2016, 9, 1, 10, 0) // 01.10.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testSamePeriodDifferentYearByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        // period: 01 (January)
        .startAndCompleteProcessInstance("process", 2015, 1, 1, 10, 0) // 01.01.2015 10:00
        .startAndCompleteProcessInstance("process", 2016, 1, 1, 10, 0) // 01.01.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testDurationReportByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        // period: 2. quarter
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testTwoInstancesInSamePeriodDurationReportByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        // period: 2. quarter
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .startAndCompleteProcessInstance("process", 2016, 5, 1, 10, 0) // 01.05.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testDurationReportInDifferentPeriodsByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        // period: 4. quarter (2015)
        .startAndCompleteProcessInstance("process", 2015, 10, 1, 10, 0) // 01.11.2015 10:00
        .startAndCompleteProcessInstance("process", 2015, 11, 1, 10, 0) // 01.12.2015 10:00
        // period: 1. quarter (2016)
        .startAndCompleteProcessInstance("process", 2016, 1, 1, 10, 0) // 01.02.2016 10:00
        .startAndCompleteProcessInstance("process", 2015, 2, 1, 10, 0) // 01.03.2016 10:00
        // period: 2. quarter (2016)
        .startAndCompleteProcessInstance("process", 2015, 3, 1, 10, 0) // 01.04.2016 10:00
        .startAndCompleteProcessInstance("process", 2015, 5, 1, 10, 0) // 01.06.2016 10:00
        // period: 3. quarter (2016)
        .startAndCompleteProcessInstance("process", 2015, 6, 1, 10, 0) // 01.07.2016 10:00
        .startAndCompleteProcessInstance("process", 2015, 7, 1, 10, 0) // 01.08.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testSamePeriodDifferentYearByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        // period: 1. quarter
        .startAndCompleteProcessInstance("process", 2015, 1, 1, 10, 0) // 01.01.2015 10:00
        .startAndCompleteProcessInstance("process", 2016, 1, 1, 10, 0) // 01.01.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByInvalidPeriodUnit() {
    HistoricProcessInstanceReport report = historyService.createHistoricProcessInstanceReport();

    try {
      report.duration(null);
      fail("Exception expected");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testReportByStartedBeforeByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
      .periodUnit(MONTH)
      .startAndCompleteProcessInstance("process", 2016, 0, 15, 10, 0) // 15.01.2016 10:00
      .done();

    // start a second process instance
    createReportScenario()
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .done();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 0, 16, 0, 0, 0);

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .startedBefore(calendar.getTime())
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByStartedBeforeByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
      .periodUnit(QUARTER)
      .startAndCompleteProcessInstance("process", 2016, 0, 15, 10, 0) // 15.01.2016 10:00
      .startAndCompleteProcessInstance("process", 2016, 0, 15, 10, 0) // 15.01.2016 10:00
      .startAndCompleteProcessInstance("process", 2016, 0, 15, 10, 0) // 15.01.2016 10:00
      .done();

    // start a second process instance
    createReportScenario()
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .done();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 0, 16, 0, 0, 0);

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .startedBefore(calendar.getTime())
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByInvalidStartedBefore() {
    HistoricProcessInstanceReport report = historyService.createHistoricProcessInstanceReport();

    try {
      report.startedBefore(null);
      fail("Exception expected");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testReportByStartedAfterByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    createReportScenario()
      .startAndCompleteProcessInstance("process", 2015, 11, 15, 10, 0) // 15.12.2015 10:00
      .done();

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .done();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 0, 1, 0, 0, 0);

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .startedAfter(calendar.getTime())
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByStartedAfterByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    createReportScenario()
      .startAndCompleteProcessInstance("process", 2015, 11, 15, 10, 0) // 15.12.2015 10:00
      .done();

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        .startAndCompleteProcessInstance("process", 2016, 3, 1, 10, 0) // 01.04.2016 10:00
        .done();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 0, 1, 0, 0, 0);

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .startedAfter(calendar.getTime())
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByInvalidStartedAfter() {
    HistoricProcessInstanceReport report = historyService.createHistoricProcessInstanceReport();

    try {
      report.startedAfter(null);
      fail("Exception expected");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testReportByStartedAfterAndStartedBeforeByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        .startAndCompleteProcessInstance("process", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .startAndCompleteProcessInstance("process", 2016, 2, 1, 10, 0) // 01.03.2016 10:00
        .done();

    createReportScenario()
        .startAndCompleteProcessInstance("process", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 0, 1, 0, 0, 0);
    Date after = calendar.getTime();
    calendar.set(2016, 2, 31, 23, 59, 59);
    Date before = calendar.getTime();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .startedAfter(after)
        .startedBefore(before)
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByStartedAfterAndStartedBeforeByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        .startAndCompleteProcessInstance("process", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .startAndCompleteProcessInstance("process", 2016, 2, 1, 10, 0) // 01.03.2016 10:00
        .done();

    createReportScenario()
        .startAndCompleteProcessInstance("process", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 0, 1, 0, 0, 0);
    Date after = calendar.getTime();
    calendar.set(2016, 2, 31, 23, 59, 59);
    Date before = calendar.getTime();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .startedAfter(after)
        .startedBefore(before)
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportWithExcludingConditions() {
    // given
   testRule.deploy(createProcessWithUserTask("process"));

    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);

    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .startedAfter(hourFromNow.getTime())
        .startedBefore(hourAgo.getTime())
        .duration(MONTH);

    // then
    Assertions.assertThat(result).isEmpty();
  }

  @Test
  public void testReportByProcessDefinitionIdByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    String processDefinitionId1 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process1")
        .singleResult()
        .getId();

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .done();

    createReportScenario()
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processDefinitionId1)
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByProcessDefinitionIdByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    String processDefinitionId1 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process1")
        .singleResult()
        .getId();

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .done();

    createReportScenario()
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processDefinitionId1)
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByMultipleProcessDefinitionIdByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    String processDefinitionId1 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process1")
        .singleResult()
        .getId();

    String processDefinitionId2 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process2")
        .singleResult()
        .getId();

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processDefinitionId1, processDefinitionId2)
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByMultipleProcessDefinitionIdByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    String processDefinitionId1 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process1")
        .singleResult()
        .getId();

    String processDefinitionId2 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process2")
        .singleResult()
        .getId();

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processDefinitionId1, processDefinitionId2)
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByInvalidProcessDefinitionId() {
    HistoricProcessInstanceReport report = historyService.createHistoricProcessInstanceReport();

    assertThatThrownBy(() -> report.processDefinitionIdIn((String) null))
      .isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> report.processDefinitionIdIn("abc", null, "def"))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  public void testReportByProcessDefinitionKeyByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .done();

    createReportScenario()
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn("process1")
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByProcessDefinitionKeyByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .done();

    createReportScenario()
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn("process1")
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByMultipleProcessDefinitionKeyByMonth() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(MONTH)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn("process1", "process2")
        .duration(MONTH);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByMultipleProcessDefinitionKeyByQuarter() {
    // given
   testRule.deploy(createProcessWithUserTask("process1"), createProcessWithUserTask("process2"));

    DurationReportResultAssertion assertion = createReportScenario()
        .periodUnit(QUARTER)
        .startAndCompleteProcessInstance("process1", 2016, 1, 15, 10, 0) // 15.02.2016 10:00
        .startAndCompleteProcessInstance("process2", 2016, 3, 15, 10, 0) // 15.04.2016 10:00
        .done();

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn("process1", "process2")
        .duration(QUARTER);

    // then
    assertThat(result).matches(assertion);
  }

  @Test
  public void testReportByInvalidProcessDefinitionKey() {
    HistoricProcessInstanceReport report = historyService.createHistoricProcessInstanceReport();

    try {
      report.processDefinitionKeyIn((String) null);
    } catch (NotValidException e) {
      // expected
    }

    try {
      report.processDefinitionKeyIn("abc", null, "def");
    } catch (NotValidException e) {
      // expected
    }
  }

  protected BpmnModelInstance createProcessWithUserTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .userTask()
      .endEvent()
    .done();
  }

  protected class DurationReportScenarioBuilder {

    protected PeriodUnit periodUnit = MONTH;

    protected DurationReportResultAssertion assertion = new DurationReportResultAssertion();

    public DurationReportScenarioBuilder periodUnit(PeriodUnit periodUnit) {
      this.periodUnit = periodUnit;
      assertion.setPeriodUnit(periodUnit);
      return this;
    }

    protected void setCurrentTime(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
      Calendar calendar = Calendar.getInstance();
      calendar.set(year, month, dayOfMonth, hourOfDay, minute);
      ClockUtil.setCurrentTime(calendar.getTime());
    }

    protected void addToCalendar(int field, int month) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(ClockUtil.getCurrentTime());
      calendar.add(field, month);
      ClockUtil.setCurrentTime(calendar.getTime());
    }

    public DurationReportScenarioBuilder startAndCompleteProcessInstance(String key, int year, int month, int dayOfMonth, int hourOfDay, int minute) {
      setCurrentTime(year, month, dayOfMonth, hourOfDay, minute);

      ProcessInstance pi = runtimeService.startProcessInstanceByKey(key);

      int period = month;
      if (periodUnit == QUARTER) {
        period = month / 3;
      }
      assertion.addDurationReportResult(period+1, pi.getId());

      addToCalendar(Calendar.MONTH, 5);
      addToCalendar(Calendar.SECOND, random.nextInt(60));
      Task task = taskService.createTaskQuery()
          .processInstanceId(pi.getId())
          .singleResult();
      taskService.complete(task.getId());

      return this;
    }

    public DurationReportResultAssertion done() {
      return assertion;
    }

  }

  protected class DurationReportResultAssertion {

    protected PeriodUnit periodUnit = MONTH;
    protected Map<Integer, Set<String>> periodToProcessInstancesMap = new HashMap<>();

    public DurationReportResultAssertion addDurationReportResult(int period, String processInstanceId) {
      Set<String> processInstances = periodToProcessInstancesMap.computeIfAbsent(period, k -> new HashSet<>());
      processInstances.add(processInstanceId);
      return this;
    }

    public DurationReportResultAssertion setPeriodUnit(PeriodUnit periodUnit) {
      this.periodUnit = periodUnit;
      return this;
    }

    public void assertReportResults(List<DurationReportResult> actual) {
      Assertions.assertThat(actual).as("Report size").hasSize(periodToProcessInstancesMap.size());

      for (DurationReportResult reportResult : actual) {
        Assertions.assertThat(reportResult.getPeriodUnit()).as("Period unit").isEqualTo(periodUnit);

        int period = reportResult.getPeriod();
        Set<String> processInstancesInPeriod = periodToProcessInstancesMap.get(period);
        Assertions.assertThat(processInstancesInPeriod).as("Unexpected report for period " + period).isNotNull();

        List<HistoricProcessInstance> historicProcessInstances = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceIds(processInstancesInPeriod)
            .finished()
            .list();

        long max = 0;
        long min = 0;
        long sum = 0;

        for (int i = 0; i < historicProcessInstances.size(); i++) {
          HistoricProcessInstance historicProcessInstance = historicProcessInstances.get(i);
          Long duration = historicProcessInstance.getDurationInMillis();
          sum = sum + duration;
          max = i > 0 ? Math.max(max, duration) : duration;
          min = i > 0 ? Math.min(min, duration) : duration;
        }

        long avg = sum / historicProcessInstances.size();

        Assertions.assertThat(reportResult.getMaximum()).as("maximum").isEqualTo(max);
        Assertions.assertThat(reportResult.getMinimum()).as("minimum").isEqualTo(min);
        Assertions.assertThat(reportResult.getAverage()).as("average").isCloseTo(avg, Offset.offset(1L));
      }
    }

  }

  protected class DurationReportResultAssert {

    protected List<DurationReportResult> actual;

    public DurationReportResultAssert(List<DurationReportResult> actual) {
      this.actual = actual;
    }

    public void matches(DurationReportResultAssertion assertion) {
      assertion.assertReportResults(actual);
    }

  }

  protected DurationReportScenarioBuilder createReportScenario() {
    return new DurationReportScenarioBuilder();
  }

  protected DurationReportResultAssert assertThat(List<DurationReportResult> actual) {
    return new DurationReportResultAssert(actual);
  }

}
