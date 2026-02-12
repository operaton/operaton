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

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.CleanableHistoricCaseInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricCaseInstanceReportResult;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class CleanableHistoricCaseInstanceReportTest {
  private static final String FORTH_CASE_DEFINITION_KEY = "case";
  private static final String THIRD_CASE_DEFINITION_KEY = "oneTaskCase";
  private static final String SECOND_CASE_DEFINITION_KEY = "oneCaseTaskCase";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  CaseService caseService;
  TaskService taskService;

  static final String CASE_DEFINITION_KEY = "one";

  @BeforeEach
  void setUp() {
    testRule.deploy("org/operaton/bpm/engine/test/repository/one.cmmn");
  }

  @AfterEach
  void cleanUp() {
    List<HistoricCaseInstance> instanceList = historyService.createHistoricCaseInstanceQuery().active().list();
    if (!instanceList.isEmpty()) {
      for (HistoricCaseInstance instance : instanceList) {

        caseService.terminateCaseExecution(instance.getId());
        caseService.closeCaseInstance(instance.getId());
      }
    }
    List<HistoricCaseInstance> historicCaseInstances = historyService.createHistoricCaseInstanceQuery().list();
    for (HistoricCaseInstance historicCaseInstance : historicCaseInstances) {
      historyService.deleteHistoricCaseInstance(historicCaseInstance.getId());
    }
  }

  private void prepareCaseInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount) {
    // update time to live
    List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().caseDefinitionKey(key).list();
    assertThat(caseDefinitions).hasSize(1);
    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitions.get(0).getId(), historyTimeToLive);

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(oldCurrentTime, daysInThePast));

    for (int i = 0; i < instanceCount; i++) {
      CaseInstance caseInstance = caseService.createCaseInstanceByKey(key);
      caseService.terminateCaseExecution(caseInstance.getId());
      caseService.closeCaseInstance(caseInstance.getId());
    }

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

  private void checkResultNumbers(CleanableHistoricCaseInstanceReportResult result, int expectedCleanable, int expectedFinished) {
    assertThat(result.getCleanableCaseInstanceCount()).isEqualTo(expectedCleanable);
    assertThat(result.getFinishedCaseInstanceCount()).isEqualTo(expectedFinished);
  }

  @Test
  void testReportWithAllCleanableInstances() {
    // given
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();
    long count = historyService.createCleanableHistoricCaseInstanceReport().count();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(count).isOne();
    checkResultNumbers(reportResults.get(0), 10, 10);
  }

  @Test
  void testReportWithPartiallyCleanableInstances() {
    // given
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 5);
    prepareCaseInstances(CASE_DEFINITION_KEY, 0, 5, 5);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    checkResultNumbers(reportResults.get(0), 5, 10);
  }

  @Test
  void testReportWithZeroHistoryTTL() {
    // given
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 0, 5);
    prepareCaseInstances(CASE_DEFINITION_KEY, 0, 0, 5);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    checkResultNumbers(reportResults.get(0), 10, 10);
  }

  @Test
  void testReportWithNullHistoryTTL() {
    // given
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, null, 5);
    prepareCaseInstances(CASE_DEFINITION_KEY, 0, null, 5);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    checkResultNumbers(reportResults.get(0), 0, 10);
  }

  @Test
  void testReportComplex() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn", "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
        "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithHistoryTimeToLive.cmmn");
    prepareCaseInstances(CASE_DEFINITION_KEY, 0, 5, 10);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10);
    prepareCaseInstances(SECOND_CASE_DEFINITION_KEY, -6, null, 10);
    prepareCaseInstances(THIRD_CASE_DEFINITION_KEY, -6, 5, 10);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();
    String id = repositoryService.createCaseDefinitionQuery().caseDefinitionKey(SECOND_CASE_DEFINITION_KEY).singleResult().getId();
    CleanableHistoricCaseInstanceReportResult secondReportResult = historyService
        .createCleanableHistoricCaseInstanceReport()
        .caseDefinitionIdIn(id)
        .singleResult();
    CleanableHistoricCaseInstanceReportResult thirdReportResult = historyService
        .createCleanableHistoricCaseInstanceReport()
        .caseDefinitionKeyIn(THIRD_CASE_DEFINITION_KEY)
        .singleResult();

    // then
    assertThat(reportResults).hasSize(4);
    for (CleanableHistoricCaseInstanceReportResult result : reportResults) {
      if (CASE_DEFINITION_KEY.equals(result.getCaseDefinitionKey())) {
        checkResultNumbers(result, 10, 20);
      } else if (SECOND_CASE_DEFINITION_KEY.equals(result.getCaseDefinitionKey())) {
        checkResultNumbers(result, 0, 10);
      } else if (THIRD_CASE_DEFINITION_KEY.equals(result.getCaseDefinitionKey())) {
        checkResultNumbers(result, 10, 10);
      } else if (FORTH_CASE_DEFINITION_KEY.equals(result.getCaseDefinitionKey())) {
        checkResultNumbers(result, 0, 0);
      }
    }
    checkResultNumbers(secondReportResult, 0, 10);
    checkResultNumbers(thirdReportResult, 10, 10);
  }

  @Test
  void testReportByInvalidCaseDefinitionId() {
    // given
    CleanableHistoricCaseInstanceReport report = historyService.createCleanableHistoricCaseInstanceReport();

    // when/then
    assertThatThrownBy(() -> report.caseDefinitionIdIn(null))
      .isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> report.caseDefinitionIdIn("abc", null, "def"))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testReportByInvalidCaseDefinitionKey() {
    // given
    CleanableHistoricCaseInstanceReport report = historyService.createCleanableHistoricCaseInstanceReport();

    // when/then
    assertThatThrownBy(() -> report.caseDefinitionKeyIn(null))
      .isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> report.caseDefinitionKeyIn("abc", null, "def"))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testReportCompact() {
    // given
    List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().caseDefinitionKey(CASE_DEFINITION_KEY).list();
    assertThat(caseDefinitions).hasSize(1);

    List<CleanableHistoricCaseInstanceReportResult> resultWithZeros = historyService.createCleanableHistoricCaseInstanceReport().list();
    assertThat(resultWithZeros).hasSize(1);
    assertThat(resultWithZeros.get(0).getFinishedCaseInstanceCount()).isZero();

    // when
    long resultCountWithoutZeros = historyService.createCleanableHistoricCaseInstanceReport().compact().count();

    // then
    assertThat(resultCountWithoutZeros).isZero();
  }

  @Test
  void testReportOrderByFinishedAsc() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn", "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn");
    prepareCaseInstances(THIRD_CASE_DEFINITION_KEY, -6, 5, 8);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 4);
    prepareCaseInstances(SECOND_CASE_DEFINITION_KEY, -6, 5, 6);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResult = historyService
        .createCleanableHistoricCaseInstanceReport()
        .orderByFinished()
        .asc()
        .list();

    // then
    assertThat(reportResult).hasSize(3);
    assertThat(reportResult.get(0).getCaseDefinitionKey()).isEqualTo(CASE_DEFINITION_KEY);
    assertThat(reportResult.get(1).getCaseDefinitionKey()).isEqualTo(SECOND_CASE_DEFINITION_KEY);
    assertThat(reportResult.get(2).getCaseDefinitionKey()).isEqualTo(THIRD_CASE_DEFINITION_KEY);
  }

  @Test
  void testReportOrderByFinishedDesc() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn", "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn");
    prepareCaseInstances(THIRD_CASE_DEFINITION_KEY, -6, 5, 8);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 4);
    prepareCaseInstances(SECOND_CASE_DEFINITION_KEY, -6, 5, 6);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResult = historyService
        .createCleanableHistoricCaseInstanceReport()
        .orderByFinished()
        .desc()
        .list();

    // then
    assertThat(reportResult).hasSize(3);
    assertThat(reportResult.get(0).getCaseDefinitionKey()).isEqualTo(THIRD_CASE_DEFINITION_KEY);
    assertThat(reportResult.get(1).getCaseDefinitionKey()).isEqualTo(SECOND_CASE_DEFINITION_KEY);
    assertThat(reportResult.get(2).getCaseDefinitionKey()).isEqualTo(CASE_DEFINITION_KEY);
  }

}
