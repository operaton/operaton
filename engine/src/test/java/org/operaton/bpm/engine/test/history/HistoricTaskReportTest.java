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
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.HistoricTaskInstanceReportResult;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Stefan Hentschel.
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricTaskReportTest {

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
  void testHistoricTaskInstanceReportQuery() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    processEngineTestRule.deploy(createProcessWithUserTask(PROCESS_DEFINITION_KEY));
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    // when
    List<HistoricTaskInstanceReportResult> historicTaskInstanceReportResults = historyService
      .createHistoricTaskInstanceReport()
      .countByTaskName();

    // then
    assertThat(historicTaskInstanceReportResults).hasSize(2);
    assertThat(historicTaskInstanceReportResults.get(0).getCount()).isEqualTo(2);
    assertThat(historicTaskInstanceReportResults.get(0).getProcessDefinitionKey()).isEqualTo(ANOTHER_PROCESS_DEFINITION_KEY);
    assertThat(historicTaskInstanceReportResults.get(0).getProcessDefinitionName()).isEqualTo("name_" + ANOTHER_PROCESS_DEFINITION_KEY);
    assertThat(historicTaskInstanceReportResults.get(0).getTaskName()).isEqualTo(ANOTHER_PROCESS_DEFINITION_KEY + " Task 1");

    assertThat(historicTaskInstanceReportResults.get(1).getProcessDefinitionId()).contains(":2:");
  }

  @Test
  void testHistoricTaskInstanceReportGroupedByProcessDefinitionKey() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    processEngineTestRule.deploy(createProcessWithUserTask(PROCESS_DEFINITION_KEY));
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    // when
    List<HistoricTaskInstanceReportResult> historicTaskInstanceReportResults = historyService
      .createHistoricTaskInstanceReport()
      .countByProcessDefinitionKey();

    // then
    assertThat(historicTaskInstanceReportResults).hasSize(2);
    assertThat(historicTaskInstanceReportResults.get(0).getProcessDefinitionId()).contains(":1:");
    assertThat(historicTaskInstanceReportResults.get(0).getProcessDefinitionName()).isEqualTo("name_" + ANOTHER_PROCESS_DEFINITION_KEY);

    assertThat(historicTaskInstanceReportResults.get(0).getProcessDefinitionKey()).isEqualTo(ANOTHER_PROCESS_DEFINITION_KEY);
  }

  @Test
  void testHistoricTaskInstanceReportWithCompletedAfterDate() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 8, 14, 12, 1);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    // when
    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 11, 14, 12, 5);

    List<HistoricTaskInstanceReportResult> historicTaskInstanceReportResults = historyService
      .createHistoricTaskInstanceReport()
      .completedAfter(calendar.getTime())
      .countByProcessDefinitionKey();

    // then
    assertThat(historicTaskInstanceReportResults).hasSize(1);
    assertThat(historicTaskInstanceReportResults.get(0).getCount()).isOne();
  }

  @Test
  void testHistoricTaskInstanceReportWithCompletedBeforeDate() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 8, 14, 12, 1);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    // when
    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 11, 14, 12, 5);

    List<HistoricTaskInstanceReportResult> historicTaskInstanceReportResults = historyService
      .createHistoricTaskInstanceReport()
      .completedBefore(calendar.getTime())
      .countByProcessDefinitionKey();

    // then
    assertThat(historicTaskInstanceReportResults).hasSize(2);
    assertThat(historicTaskInstanceReportResults.get(0).getCount()).isOne();
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

  @Test
  void testReportWithNullTaskName() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    // when
    BpmnModelInstance instance = Bpmn.createExecutableProcess(ANOTHER_PROCESS_DEFINITION_KEY)
      .name("name_" + ANOTHER_PROCESS_DEFINITION_KEY)
      .startEvent()
        .userTask("task1_" + ANOTHER_PROCESS_DEFINITION_KEY)
        .name(null)
        .endEvent()
      .done();

    processEngineTestRule.deploy(instance);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 11, 14, 12, 5);

    List<HistoricTaskInstanceReportResult> historicTaskInstanceReportResults = historyService
      .createHistoricTaskInstanceReport()
      .completedBefore(calendar.getTime())
      .countByTaskName();

    assertThat(historicTaskInstanceReportResults).hasSize(1);
    assertThat(historicTaskInstanceReportResults.get(0).getCount()).isOne();
  }

  @Test
  void testReportWithEmptyTaskName() {
    // given
    startAndCompleteProcessInstance(PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    // when
    BpmnModelInstance instance = Bpmn.createExecutableProcess(ANOTHER_PROCESS_DEFINITION_KEY)
      .name("name_" + ANOTHER_PROCESS_DEFINITION_KEY)
      .startEvent()
        .userTask("task1_" + ANOTHER_PROCESS_DEFINITION_KEY)
        .name("")
      .endEvent()
      .done();

    processEngineTestRule.deploy(instance);
    startAndCompleteProcessInstance(ANOTHER_PROCESS_DEFINITION_KEY, 2016, 7, 14, 12, 1);

    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, 11, 14, 12, 5);

    List<HistoricTaskInstanceReportResult> historicTaskInstanceReportResults = historyService
      .createHistoricTaskInstanceReport()
      .completedBefore(calendar.getTime())
      .countByTaskName();

    assertThat(historicTaskInstanceReportResults).hasSize(1);
    assertThat(historicTaskInstanceReportResults.get(0).getCount()).isOne();
  }

  protected BpmnModelInstance createProcessWithUserTask(String key) {
    double random = ThreadLocalRandom.current().nextDouble();
    return Bpmn.createExecutableProcess(key)
      .name("name_" + key)
      .startEvent()
        .userTask(key + "_" + random + "_task1")
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
