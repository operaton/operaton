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

import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.DurationReportResult;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.query.PeriodUnit;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * @author Stefan Hentschel.
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricTaskDurationReportTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension processEngineTestRule = new ProcessEngineTestExtension(processEngineRule);

  ProcessEngineConfiguration processEngineConfiguration;
  HistoryService historyService;

  protected static final String PROCESS_DEFINITION_KEY = "HISTORIC_TASK_INST_REPORT";
  protected static final String ANOTHER_PROCESS_DEFINITION_KEY = "ANOTHER_HISTORIC_TASK_INST_REPORT";


  @BeforeEach
  void setUp() {
    processEngineTestRule.deploy(createProcessWithUserTask(PROCESS_DEFINITION_KEY));
    processEngineTestRule.deploy(createProcessWithUserTask(ANOTHER_PROCESS_DEFINITION_KEY));
  }

  @AfterEach
  void cleanUp() {
    List<Task> list = processEngineRule.getTaskService().createTaskQuery().list();
    for( Task task : list ) {
      processEngineRule.getTaskService().deleteTask(task.getId(), true);
    }
  }

  @Test
  void testHistoricTaskInstanceDurationReportQuery() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 6, 14, 11, 43);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 11, 43);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 8, 14, 11, 43);

    // when
    List<DurationReportResult> taskReportResults = historyService.createHistoricTaskInstanceReport().duration(PeriodUnit.MONTH);

    // then
    assertThat(taskReportResults).hasSize(3);
  }

  @Test
  void testHistoricTaskInstanceDurationReportWithCompletedAfterDate() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 11, 43);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 8, 14, 11, 43);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 11, 43);

    // when
    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 11, 14, 12, 5);

    List<DurationReportResult> taskReportResults = historyService
      .createHistoricTaskInstanceReport()
      .completedAfter(calendar.getTime())
      .duration(PeriodUnit.MONTH);

    // then
    assertThat(taskReportResults).hasSize(1);
  }

  @Test
  void testHistoricTaskInstanceDurationReportWithCompletedBeforeDate() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 11, 43);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 8, 14, 11, 43);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 6, 14, 11, 43);

    // when
    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 11, 14, 12, 5);

    List<DurationReportResult> taskReportResults = historyService
      .createHistoricTaskInstanceReport()
      .completedBefore(calendar.getTime())
      .duration(PeriodUnit.MONTH);

    // then
    assertThat(taskReportResults).hasSize(2);
  }

  @Test
  void testHistoricTaskInstanceDurationReportResults() {
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 11, 43);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 11, 43);

    DurationReportResult taskReportResult = historyService
      .createHistoricTaskInstanceReport()
      .duration(PeriodUnit.MONTH).get(0);

    List<HistoricTaskInstance> historicTaskInstances = historyService
      .createHistoricTaskInstanceQuery()
      .processDefinitionKey(PROCESS_DEFINITION_KEY)
      .list();

    long min = 0;
    long max = 0;
    long sum = 0;

    for (int i = 0; i < historicTaskInstances.size(); i++) {
      HistoricTaskInstance historicProcessInstance = historicTaskInstances.get(i);
      Long duration = historicProcessInstance.getDurationInMillis();
      sum = sum + duration;
      max = i > 0 ? Math.max(max, duration) : duration;
      min = i > 0 ? Math.min(min, duration) : duration;
    }

    long avg = sum / historicTaskInstances.size();

    assertThat(taskReportResult.getMaximum()).as("maximum").isEqualTo(max);
    assertThat(taskReportResult.getMinimum()).as("minimum").isEqualTo(min);
    assertThat(taskReportResult.getAverage()).as("average").isCloseTo(avg, within(0L));

  }

  @Test
  void testCompletedAfterWithNullValue() {
    // given
    var historicTaskInstanceReport = historyService.createHistoricTaskInstanceReport();

    // when/then
    assertThatThrownBy(() -> historicTaskInstanceReport.completedAfter(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("completedAfter");
  }

  @Test
  void testCompletedBeforeWithNullValue() {
    // given
    var historicTaskInstanceReport = historyService.createHistoricTaskInstanceReport();

    // when/then
    assertThatThrownBy(() -> historicTaskInstanceReport.completedBefore(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("completedBefore");
  }

  protected BpmnModelInstance createProcessWithUserTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .userTask(key + "_task1")
      .name(key + " Task 1")
      .endEvent()
      .done();
  }

  protected void completeTask(String pid) {
    Task task = processEngineRule.getTaskService().createTaskQuery().processInstanceId(pid).singleResult();
    processEngineRule.getTaskService().complete(task.getId());
  }

  protected void setCurrentTime(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
    Calendar calendar = Calendar.getInstance();
    // Calendars month start with 0 = January
    calendar.set(year, month - 1, dayOfMonth, hourOfDay, minute);
    ClockUtil.setCurrentTime(calendar.getTime());
  }

  protected void addToCalendar(int field, int month) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(ClockUtil.getCurrentTime());
    calendar.add(field, month);
    ClockUtil.setCurrentTime(calendar.getTime());
  }

  protected void startAndCompleteProcessInstance(String key, int year, int month, int dayOfMonth, int hourOfDay, int minute) {
    setCurrentTime(year, month, dayOfMonth , hourOfDay, minute);

    ProcessInstance pi = processEngineRule.getRuntimeService().startProcessInstanceByKey(key);

    addToCalendar(Calendar.MONTH, 5);
    completeTask(pi.getId());

    ClockUtil.reset();
  }
}
