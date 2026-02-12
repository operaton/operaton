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
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReportResult;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class CleanableHistoricDecisionInstanceReportTest {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  RepositoryService repositoryService;

  protected static final String DECISION_DEFINITION_KEY = "one";
  protected static final String SECOND_DECISION_DEFINITION_KEY = "two";
  protected static final String THIRD_DECISION_DEFINITION_KEY = "anotherDecision";
  protected static final String FOURTH_DECISION_DEFINITION_KEY = "decision";

  @BeforeEach
  void setUp() {
    testRule.deploy("org/operaton/bpm/engine/test/repository/one.dmn");
  }

  @AfterEach
  void cleanUp() {

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();
    for (HistoricDecisionInstance historicDecisionInstance : historicDecisionInstances) {
      historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstance.getId());
    }
  }

  protected void prepareDecisionInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount) {
    List<DecisionDefinition> decisionDefinitions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(key).list();
    assertThat(decisionDefinitions).hasSize(1);
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitions.get(0).getId(), historyTimeToLive);

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(oldCurrentTime, daysInThePast));

    Map<String, Object> variables = Variables.createVariables().putValue("status", "silver").putValue("sum", 723);
    for (int i = 0; i < instanceCount; i++) {
      engineRule.getDecisionService().evaluateDecisionByKey(key).variables(variables).evaluate();
    }

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

  @Test
  void testReportComplex() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/repository/two.dmn", "org/operaton/bpm/engine/test/api/dmn/Another_Example.dmn",
        "org/operaton/bpm/engine/test/api/dmn/Example.dmn");
    prepareDecisionInstances(DECISION_DEFINITION_KEY, 0, 5, 10);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10);
    prepareDecisionInstances(SECOND_DECISION_DEFINITION_KEY, -6, null, 10);
    prepareDecisionInstances(THIRD_DECISION_DEFINITION_KEY, -6, 5, 10);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();
    String secondDecisionDefinitionId = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(SECOND_DECISION_DEFINITION_KEY).singleResult().getId();
    CleanableHistoricDecisionInstanceReportResult secondReportResult = historyService.createCleanableHistoricDecisionInstanceReport().decisionDefinitionIdIn(secondDecisionDefinitionId).singleResult();
    CleanableHistoricDecisionInstanceReportResult thirdReportResult = historyService.createCleanableHistoricDecisionInstanceReport().decisionDefinitionKeyIn(THIRD_DECISION_DEFINITION_KEY).singleResult();

    // then
    assertThat(reportResults).hasSize(4);
    for (CleanableHistoricDecisionInstanceReportResult result : reportResults) {
      if (DECISION_DEFINITION_KEY.equals(result.getDecisionDefinitionKey())) {
        checkResultNumbers(result, 10, 20);
      } else if (SECOND_DECISION_DEFINITION_KEY.equals(result.getDecisionDefinitionKey())) {
        checkResultNumbers(result, 0, 10);
      } else if (THIRD_DECISION_DEFINITION_KEY.equals(result.getDecisionDefinitionKey())) {
        checkResultNumbers(result, 10, 10);
      } else if (FOURTH_DECISION_DEFINITION_KEY.equals(result.getDecisionDefinitionKey())) {
        checkResultNumbers(result, 0, 0);
      }
    }
    checkResultNumbers(secondReportResult, 0, 10);
    checkResultNumbers(thirdReportResult, 10, 10);

  }

  private void checkResultNumbers(CleanableHistoricDecisionInstanceReportResult result, int expectedCleanable, int expectedFinished) {
    assertThat(result.getCleanableDecisionInstanceCount()).isEqualTo(expectedCleanable);
    assertThat(result.getFinishedDecisionInstanceCount()).isEqualTo(expectedFinished);
  }

  @Test
  void testReportWithAllCleanableInstances() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();
    long count = historyService.createCleanableHistoricDecisionInstanceReport().count();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(count).isOne();

    checkResultNumbers(reportResults.get(0), 10, 10);
  }

  @Test
  void testReportWithPartiallyCleanableInstances() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 5);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, 0, 5, 5);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    checkResultNumbers(reportResults.get(0), 5, 10);
  }

  @Test
  void testReportWithZeroHistoryTTL() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 0, 5);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, 0, 0, 5);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    checkResultNumbers(reportResults.get(0), 10, 10);
  }

  @Test
  void testReportWithNullHistoryTTL() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, null, 5);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, 0, null, 5);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    checkResultNumbers(reportResults.get(0), 0, 10);
  }

  @Test
  void testReportByInvalidDecisionDefinitionId() {
    // given
    CleanableHistoricDecisionInstanceReport report = historyService.createCleanableHistoricDecisionInstanceReport();

    // when/then
    assertThatThrownBy(() -> report.decisionDefinitionIdIn(null))
      .isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> report.decisionDefinitionIdIn("abc", null, "def"))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testReportByInvalidDecisionDefinitionKey() {
    // given
    CleanableHistoricDecisionInstanceReport report = historyService.createCleanableHistoricDecisionInstanceReport();

    // when/then
    assertThatThrownBy(() -> report.decisionDefinitionKeyIn(null))
      .isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> report.decisionDefinitionKeyIn("abc", null, "def"))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testReportCompact() {
    // given
    List<DecisionDefinition> decisionDefinitions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(DECISION_DEFINITION_KEY).list();
    assertThat(decisionDefinitions).hasSize(1);

    // assume
    List<CleanableHistoricDecisionInstanceReportResult> resultWithZeros = historyService.createCleanableHistoricDecisionInstanceReport().list();
    assertThat(resultWithZeros).hasSize(1);
    assertThat(resultWithZeros.get(0).getFinishedDecisionInstanceCount()).isZero();

    // when
    long resultCountWithoutZeros = historyService.createCleanableHistoricDecisionInstanceReport().compact().count();

    // then
    assertThat(resultCountWithoutZeros).isZero();
  }

  @Test
  void testReportOrderByFinishedAsc() {
    // give
    testRule.deploy("org/operaton/bpm/engine/test/repository/two.dmn", "org/operaton/bpm/engine/test/api/dmn/Another_Example.dmn");
    prepareDecisionInstances(SECOND_DECISION_DEFINITION_KEY, -6, 5, 6);
    prepareDecisionInstances(THIRD_DECISION_DEFINITION_KEY, -6, 5, 8);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 4);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResult = historyService
        .createCleanableHistoricDecisionInstanceReport()
        .orderByFinished()
        .asc()
        .list();

    // then
    assertThat(reportResult).hasSize(3);
    assertThat(reportResult.get(0).getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
    assertThat(reportResult.get(1).getDecisionDefinitionKey()).isEqualTo(SECOND_DECISION_DEFINITION_KEY);
    assertThat(reportResult.get(2).getDecisionDefinitionKey()).isEqualTo(THIRD_DECISION_DEFINITION_KEY);
  }

  @Test
  void testReportOrderByFinishedDesc() {
    // give
    testRule.deploy("org/operaton/bpm/engine/test/repository/two.dmn", "org/operaton/bpm/engine/test/api/dmn/Another_Example.dmn");
    prepareDecisionInstances(SECOND_DECISION_DEFINITION_KEY, -6, 5, 6);
    prepareDecisionInstances(THIRD_DECISION_DEFINITION_KEY, -6, 5, 8);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 4);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResult = historyService
        .createCleanableHistoricDecisionInstanceReport()
        .orderByFinished()
        .desc()
        .list();

    // then
    assertThat(reportResult).hasSize(3);
    assertThat(reportResult.get(0).getDecisionDefinitionKey()).isEqualTo(THIRD_DECISION_DEFINITION_KEY);
    assertThat(reportResult.get(1).getDecisionDefinitionKey()).isEqualTo(SECOND_DECISION_DEFINITION_KEY);
    assertThat(reportResult.get(2).getDecisionDefinitionKey()).isEqualTo(DECISION_DEFINITION_KEY);
  }
}
