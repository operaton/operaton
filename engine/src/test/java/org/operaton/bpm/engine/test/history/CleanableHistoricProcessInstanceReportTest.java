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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReportResult;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class CleanableHistoricProcessInstanceReportTest {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  TaskService taskService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;

  protected static final String PROCESS_DEFINITION_KEY = "HISTORIC_INST";
  protected static final String SECOND_PROCESS_DEFINITION_KEY = "SECOND_HISTORIC_INST";
  protected static final String THIRD_PROCESS_DEFINITION_KEY = "THIRD_HISTORIC_INST";
  protected static final String FOURTH_PROCESS_DEFINITION_KEY = "FOURTH_HISTORIC_INST";

  @BeforeEach
  void setUp() {
    testRule.deploy(createProcessWithUserTask(PROCESS_DEFINITION_KEY));
  }

  @AfterEach
  void cleanUp() {
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    for (ProcessInstance processInstance : processInstances) {
      runtimeService.deleteProcessInstance(processInstance.getId(), null, true, true);
    }

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.deleteTask(task.getId(), true);
    }

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
      historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
    }
  }

  protected BpmnModelInstance createProcessWithUserTask(String key) {
    return Bpmn.createExecutableProcess(key)
        .startEvent()
        .userTask(key + "_task1")
          .name(key + " Task 1")
        .endEvent()
        .done();
  }

  protected void prepareProcessInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount) {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).list();
    assertThat(processDefinitions).hasSize(1);
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitions.get(0).getId(), historyTimeToLive);

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(oldCurrentTime, daysInThePast));

    List<String> processInstanceIds = new ArrayList<>();
    for (int i = 0; i < instanceCount; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key);
      processInstanceIds.add(processInstance.getId());
    }
    runtimeService.deleteProcessInstances(processInstanceIds, null, true, true);

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

  @Test
  void testReportComplex() {
    testRule.deploy(createProcessWithUserTask(SECOND_PROCESS_DEFINITION_KEY));
    testRule.deploy(createProcessWithUserTask(THIRD_PROCESS_DEFINITION_KEY));
    testRule.deploy(createProcessWithUserTask(FOURTH_PROCESS_DEFINITION_KEY));
    // given
    prepareProcessInstances(PROCESS_DEFINITION_KEY, 0, 5, 10);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10);
    prepareProcessInstances(SECOND_PROCESS_DEFINITION_KEY, -6, 5, 10);
    prepareProcessInstances(THIRD_PROCESS_DEFINITION_KEY, -6, null, 10);
    prepareProcessInstances(FOURTH_PROCESS_DEFINITION_KEY, -6, 0, 10);

    repositoryService.deleteProcessDefinition(
        repositoryService.createProcessDefinitionQuery().processDefinitionKey(SECOND_PROCESS_DEFINITION_KEY).singleResult().getId(), false);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();
    CleanableHistoricProcessInstanceReportResult secondReportResult = historyService.createCleanableHistoricProcessInstanceReport().processDefinitionIdIn(repositoryService.createProcessDefinitionQuery().processDefinitionKey(THIRD_PROCESS_DEFINITION_KEY).singleResult().getId()).singleResult();
    CleanableHistoricProcessInstanceReportResult thirdReportResult = historyService.createCleanableHistoricProcessInstanceReport().processDefinitionKeyIn(FOURTH_PROCESS_DEFINITION_KEY).singleResult();

    // then
    assertThat(reportResults).hasSize(3);
    for (CleanableHistoricProcessInstanceReportResult result : reportResults) {
      if (PROCESS_DEFINITION_KEY.equals(result.getProcessDefinitionKey())) {
        checkResultNumbers(result, 10, 20);
      } else if (THIRD_PROCESS_DEFINITION_KEY.equals(result.getProcessDefinitionKey())) {
        checkResultNumbers(result, 0, 10);
      } else if (FOURTH_PROCESS_DEFINITION_KEY.equals(result.getProcessDefinitionKey())) {
        checkResultNumbers(result, 10, 10);
      }
    }
    checkResultNumbers(secondReportResult, 0, 10);
    checkResultNumbers(thirdReportResult, 10, 10);
  }

  private void checkResultNumbers(CleanableHistoricProcessInstanceReportResult result, int expectedCleanable, int expectedFinished) {
    assertThat(result.getCleanableProcessInstanceCount()).isEqualTo(expectedCleanable);
    assertThat(result.getFinishedProcessInstanceCount()).isEqualTo(expectedFinished);
  }

  @Test
  void testReportWithAllCleanableInstances() {
    // given
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();
    long count = historyService.createCleanableHistoricProcessInstanceReport().count();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(count).isOne();

    checkResultNumbers(reportResults.get(0), 10, 10);
  }

  @Test
  void testReportWithPartiallyCleanableInstances() {
    // given
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 5);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, 0, 5, 5);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);

    checkResultNumbers(reportResults.get(0), 5, 10);
  }

  @Test
  void testReportWithZeroHistoryTTL() {
    // given
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 0, 5);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, 0, 0, 5);

    // when
    CleanableHistoricProcessInstanceReportResult result = historyService.createCleanableHistoricProcessInstanceReport().singleResult();

    // then
    checkResultNumbers(result, 10, 10);
  }

  @Test
  void testReportWithNullHistoryTTL() {
    // given
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, null, 5);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, 0, null, 5);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);

    checkResultNumbers(reportResults.get(0), 0, 10);
  }

  @Test
  void testReportByInvalidProcessDefinitionId() {
    // given
    CleanableHistoricProcessInstanceReport report = historyService.createCleanableHistoricProcessInstanceReport();

    // when/then
    assertThatThrownBy(() -> report.processDefinitionIdIn(null))
      .isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> report.processDefinitionIdIn("abc", null, "def"))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testReportByInvalidProcessDefinitionKey() {
    // given
    CleanableHistoricProcessInstanceReport report = historyService.createCleanableHistoricProcessInstanceReport();

    // when/then
    assertThatThrownBy(() -> report.processDefinitionKeyIn(null))
      .isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> report.processDefinitionKeyIn("abc", null, "def"))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testReportCompact() {
    // given
    List<ProcessDefinition> pdList = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).list();
    assertThat(pdList).hasSize(1);
    runtimeService.startProcessInstanceById(pdList.get(0).getId());

    List<CleanableHistoricProcessInstanceReportResult> resultWithZeros = historyService.createCleanableHistoricProcessInstanceReport().list();
    assertThat(resultWithZeros).hasSize(1);
    assertThat(resultWithZeros.get(0).getFinishedProcessInstanceCount()).isZero();

    // when
    long resultCountWithoutZeros = historyService.createCleanableHistoricProcessInstanceReport().compact().count();

    // then
    assertThat(resultCountWithoutZeros).isZero();
  }

  @Test
  void testReportOrderByFinishedAsc() {
    testRule.deploy(createProcessWithUserTask(SECOND_PROCESS_DEFINITION_KEY));
    testRule.deploy(createProcessWithUserTask(THIRD_PROCESS_DEFINITION_KEY));
    // given
    prepareProcessInstances(SECOND_PROCESS_DEFINITION_KEY, -6, 5, 6);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 4);
    prepareProcessInstances(THIRD_PROCESS_DEFINITION_KEY, -6, 5, 8);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResult = historyService
      .createCleanableHistoricProcessInstanceReport()
      .orderByFinished()
      .asc()
      .list();

    // then
    assertThat(reportResult).hasSize(3);
    assertThat(reportResult.get(0).getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
    assertThat(reportResult.get(1).getProcessDefinitionKey()).isEqualTo(SECOND_PROCESS_DEFINITION_KEY);
    assertThat(reportResult.get(2).getProcessDefinitionKey()).isEqualTo(THIRD_PROCESS_DEFINITION_KEY);
  }

  @Test
  void testReportOrderByFinishedDesc() {
    testRule.deploy(createProcessWithUserTask(SECOND_PROCESS_DEFINITION_KEY));
    testRule.deploy(createProcessWithUserTask(THIRD_PROCESS_DEFINITION_KEY));
    // given
    prepareProcessInstances(SECOND_PROCESS_DEFINITION_KEY, -6, 5, 6);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 4);
    prepareProcessInstances(THIRD_PROCESS_DEFINITION_KEY, -6, 5, 8);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResult = historyService
      .createCleanableHistoricProcessInstanceReport()
      .orderByFinished()
      .desc()
      .list();

    // then
    assertThat(reportResult).hasSize(3);
    assertThat(reportResult.get(0).getProcessDefinitionKey()).isEqualTo(THIRD_PROCESS_DEFINITION_KEY);
    assertThat(reportResult.get(1).getProcessDefinitionKey()).isEqualTo(SECOND_PROCESS_DEFINITION_KEY);
    assertThat(reportResult.get(2).getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
  }
}
