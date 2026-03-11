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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricCaseActivityStatistics;
import org.operaton.bpm.engine.history.HistoricCaseActivityStatisticsQuery;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
@ExtendWith(ProcessEngineExtension.class)
class HistoricCaseActivityStatisticsQueryTest {

  HistoryService historyService;
  CaseService caseService;
  RepositoryService repositoryService;

  @Test
  void testCaseDefinitionNull() {
    var historicCaseActivityStatisticsQuery = historyService
        .createHistoricCaseActivityStatisticsQuery(null);
    // given

    // when
    assertThatThrownBy(historicCaseActivityStatisticsQuery::list).isInstanceOf(NullValueException.class);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  void testNoCaseActivityInstances() {
    // given
    String caseDefinitionId = getCaseDefinition().getId();

    // when
    HistoricCaseActivityStatisticsQuery query = historyService.createHistoricCaseActivityStatisticsQuery(caseDefinitionId);

    // then
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  void testSingleTask() {
    // given
    String caseDefinitionId = getCaseDefinition().getId();

    createCaseByKey(5, "oneTaskCase");

    // when
    HistoricCaseActivityStatisticsQuery query = historyService.createHistoricCaseActivityStatisticsQuery(caseDefinitionId);

    // then
    List<HistoricCaseActivityStatistics> statistics = query.list();

    assertThat(query.count()).isOne();
    assertThat(statistics).hasSize(1);
    assertStatisitcs(statistics.get(0), "PI_HumanTask_1", 5, 0, 0, 0, 0, 0);
  }

  @Test
  @Deployment
  void testMultipleTasks() {

    // given
    String caseDefinitionId = getCaseDefinition().getId();

    createCaseByKey(5, "case");

    disableByActivity("DISABLED");
    completeByActivity("COMPLETED");
    terminateByActivity("TERMINATED");

    // when
    HistoricCaseActivityStatisticsQuery query = historyService.createHistoricCaseActivityStatisticsQuery(caseDefinitionId);

    // then
    List<HistoricCaseActivityStatistics> statistics = query.list();
    assertThat(statistics).hasSize(6);
    assertThat(query.count()).isEqualTo(6);

    assertStatisitcs(statistics.get(0), "ACTIVE", 5, 0, 0, 0, 0, 0);
    assertStatisitcs(statistics.get(1), "AVAILABLE", 0, 5, 0, 0, 0, 0);
    assertStatisitcs(statistics.get(2), "COMPLETED", 0, 0, 5, 0, 0, 0);
    assertStatisitcs(statistics.get(3), "DISABLED", 0, 0, 0, 5, 0, 0);
    assertStatisitcs(statistics.get(4), "ENABLED", 0, 0, 0, 0, 5, 0);
    assertStatisitcs(statistics.get(5), "TERMINATED", 0, 0, 0, 0, 0, 5);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricCaseActivityStatisticsQueryTest.testMultipleTasks.cmmn"
  })
  void testStateCount() {

    // given
    String caseDefinitionId = getCaseDefinition().getId();

    createCaseByKey(3, "case");
    completeByActivity("ACTIVE");
    manuallyStartByActivity("AVAILABLE");
    completeByActivity("AVAILABLE");

    createCaseByKey(5, "case");
    completeByActivity("ACTIVE");
    disableByActivity("AVAILABLE");
    reenableByActivity("AVAILABLE");
    manuallyStartByActivity("AVAILABLE");
    terminateByActivity("AVAILABLE");

    createCaseByKey(5, "case");
    terminateByActivity("ACTIVE");

    manuallyStartByActivity("ENABLED");
    completeByActivity("ENABLED");

    manuallyStartByActivity("DISABLED");
    terminateByActivity("DISABLED");

    createCaseByKey(2, "case");
    disableByActivity("DISABLED");

    // when
    HistoricCaseActivityStatisticsQuery query = historyService.createHistoricCaseActivityStatisticsQuery(caseDefinitionId);

    // then
    List<HistoricCaseActivityStatistics> statistics = query.list();
    assertThat(statistics).hasSize(6);
    assertThat(query.count()).isEqualTo(6);

    assertStatisitcs(statistics.get(0), "ACTIVE", 2, 0, 8, 0, 0, 5);
    assertStatisitcs(statistics.get(1), "AVAILABLE", 0, 7, 3, 0, 0, 5);
    assertStatisitcs(statistics.get(2), "COMPLETED", 15, 0, 0, 0, 0, 0);
    assertStatisitcs(statistics.get(3), "DISABLED", 0, 0, 0, 2, 0, 13);
    assertStatisitcs(statistics.get(4), "ENABLED", 0, 0, 13, 0, 2, 0);
    assertStatisitcs(statistics.get(5), "TERMINATED", 15, 0, 0, 0, 0, 0);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
      "org/operaton/bpm/engine/test/history/HistoricCaseActivityStatisticsQueryTest.testMultipleTasks.cmmn"
  })
  void testMultipleCaseDefinitions() {

    // given
    String caseDefinitionId1 = getCaseDefinition("oneTaskCase").getId();
    String caseDefinitionId2 = getCaseDefinition("case").getId();

    createCaseByKey(5, "oneTaskCase");
    createCaseByKey(10, "case");

    // when
    HistoricCaseActivityStatisticsQuery query1 = historyService.createHistoricCaseActivityStatisticsQuery(caseDefinitionId1);
    HistoricCaseActivityStatisticsQuery query2 = historyService.createHistoricCaseActivityStatisticsQuery(caseDefinitionId2);

    // then
    assertThat(query1.list()).hasSize(1);
    assertThat(query2.list()).hasSize(6);

  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricCaseActivityStatisticsQueryTest.testMultipleTasks.cmmn"
  })
  void testPagination() {
    // given
    String caseDefinitionId = getCaseDefinition().getId();

    createCaseByKey(5, "case");

    // when
    List<HistoricCaseActivityStatistics> statistics = historyService
        .createHistoricCaseActivityStatisticsQuery(caseDefinitionId)
        .listPage(2, 1);

    // then
    assertThat(statistics).hasSize(1);
    assertThat(statistics.get(0).getId()).isEqualTo("COMPLETED");
  }

  protected void assertStatisitcs(HistoricCaseActivityStatistics statistics,
      String id, long active, long availabe, long completed, long disabled, long enabled, long terminated) {
    assertThat(statistics.getId()).isEqualTo(id);
    assertThat(statistics.getActive()).isEqualTo(active);
    assertThat(statistics.getAvailable()).isEqualTo(availabe);
    assertThat(statistics.getCompleted()).isEqualTo(completed);
    assertThat(statistics.getDisabled()).isEqualTo(disabled);
    assertThat(statistics.getEnabled()).isEqualTo(enabled);
    assertThat(statistics.getTerminated()).isEqualTo(terminated);
  }

  protected void createCaseByKey(int numberOfInstances, String key) {
    for (int i = 0; i < numberOfInstances; i++) {
      caseService.createCaseInstanceByKey(key);
    }
  }

  protected CaseDefinition getCaseDefinition() {
    return repositoryService.createCaseDefinitionQuery().singleResult();
  }

  protected CaseDefinition getCaseDefinition(String key) {
    return repositoryService.createCaseDefinitionQuery().caseDefinitionKey(key).singleResult();
  }

  protected List<CaseExecution> getCaseExecutionsByActivity(String activityId) {
    return caseService.createCaseExecutionQuery()
        .activityId(activityId)
        .list();
  }

  protected void disableByActivity(String activityId) {
    List<CaseExecution> executions = getCaseExecutionsByActivity(activityId);
    for (CaseExecution caseExecution : executions) {
      caseService.disableCaseExecution(caseExecution.getId());
    }
  }

  protected void reenableByActivity(String activityId) {
    List<CaseExecution> executions = getCaseExecutionsByActivity(activityId);
    for (CaseExecution caseExecution : executions) {
      caseService.reenableCaseExecution(caseExecution.getId());
    }
  }

  protected void manuallyStartByActivity(String activityId) {
    List<CaseExecution> executions = getCaseExecutionsByActivity(activityId);
    for (CaseExecution caseExecution : executions) {
      caseService.manuallyStartCaseExecution(caseExecution.getId());
    }
  }

  protected void completeByActivity(String activityId) {
    List<CaseExecution> executions = getCaseExecutionsByActivity(activityId);
    for (CaseExecution caseExecution : executions) {
      caseService.completeCaseExecution(caseExecution.getId());
    }
  }

  protected void terminateByActivity(String activityId) {
    List<CaseExecution> executions = getCaseExecutionsByActivity(activityId);
    for (CaseExecution caseExecution : executions) {
      caseService.terminateCaseExecution(caseExecution.getId());
    }
  }

}
