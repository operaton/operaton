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
package org.operaton.bpm.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceStatisticsQuery;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;


/**
 * @author Askar Akhmerov
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricDecisionInstanceStatisticsQueryTest {

  protected static final String DISH_DRG_DMN = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";
  protected static final String SCORE_DRG_DMN = "org/operaton/bpm/engine/test/dmn/deployment/drdScore.dmn11.xml";

  protected static final String NON_EXISTING = "fake";
  protected static final String DISH_DECISION = "dish-decision";
  protected static final String TEMPERATURE = "temperature";
  protected static final String DAY_TYPE = "dayType";
  protected static final String WEEKEND = "Weekend";

  protected DecisionService decisionService;
  protected RepositoryService repositoryService;
  protected HistoryService historyService;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @BeforeEach
  void setUp() {
    testRule.deploy(DISH_DRG_DMN);
  }

  @Test
  void testStatisticForRootDecisionEvaluation() {
    // given
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue(TEMPERATURE, 21).putValue(DAY_TYPE, WEEKEND))
        .evaluate();

    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue(TEMPERATURE, 11).putValue(DAY_TYPE, WEEKEND))
        .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();

    // when
    HistoricDecisionInstanceStatisticsQuery statisticsQuery = historyService
        .createHistoricDecisionInstanceStatisticsQuery(
            decisionRequirementsDefinition.getId());

    // then
    assertThat(statisticsQuery.count()).isEqualTo(3L);
    assertThat(statisticsQuery.list()).hasSize(3);
    assertThat(statisticsQuery.list().get(0).getEvaluations()).isEqualTo(2);
    assertThat(statisticsQuery.list().get(0).getDecisionDefinitionKey()).isNotNull();
  }

  @Test
  void testStatisticForRootDecisionWithInstanceConstraintEvaluation() {
    // given
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue(TEMPERATURE, 21).putValue(DAY_TYPE, WEEKEND))
        .evaluate();

    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue(TEMPERATURE, 11).putValue(DAY_TYPE, WEEKEND))
        .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();


    String decisionInstanceId = engineRule.getHistoryService()
        .createHistoricDecisionInstanceQuery()
        .decisionRequirementsDefinitionId(decisionRequirementsDefinition.getId())
        .rootDecisionInstancesOnly()
        .list()
        .get(0)
        .getId();

    // when
    HistoricDecisionInstanceStatisticsQuery query = historyService
        .createHistoricDecisionInstanceStatisticsQuery(
            decisionRequirementsDefinition.getId())
        .decisionInstanceId(decisionInstanceId);

    // then
    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.list()).hasSize(3);
    assertThat(query.list().get(0).getEvaluations()).isEqualTo(1);
    assertThat(query.list().get(0).getDecisionDefinitionKey()).isNotNull();
  }

  @Test
  void testStatisticForRootDecisionWithFakeInstanceConstraintEvaluation() {
    // given
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue(TEMPERATURE, 21).putValue(DAY_TYPE, WEEKEND))
        .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();

    // when
    HistoricDecisionInstanceStatisticsQuery query = historyService
        .createHistoricDecisionInstanceStatisticsQuery(
            decisionRequirementsDefinition.getId())
        .decisionInstanceId(NON_EXISTING);

    // then
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testStatisticForRootDecisionWithNullInstanceConstraintEvaluation() {
    // given
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue(TEMPERATURE, 21).putValue(DAY_TYPE, WEEKEND))
        .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();

    // when
    assertThatThrownBy(() -> {HistoricDecisionInstanceStatisticsQuery query = historyService
                      .createHistoricDecisionInstanceStatisticsQuery(
                              decisionRequirementsDefinition.getId())
                      .decisionInstanceId(null);

                query.count();
                query.list();
            })
            .isInstanceOf(NotValidException.class);
  }

  @Test
  void testStatisticForChildDecisionEvaluation() {
    // given
    decisionService.evaluateDecisionTableByKey("season")
        .variables(Variables.createVariables().putValue(TEMPERATURE, 21))
        .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();

    // when
    HistoricDecisionInstanceStatisticsQuery statisticsQuery = historyService
        .createHistoricDecisionInstanceStatisticsQuery(
            decisionRequirementsDefinition.getId());

    //then
    assertThat(statisticsQuery.count()).isEqualTo(1L);
    assertThat(statisticsQuery.list()).hasSize(1);
    assertThat(statisticsQuery.list().get(0).getEvaluations()).isEqualTo(1);
    assertThat(statisticsQuery.list().get(0).getDecisionDefinitionKey()).isNotNull();
  }

  @Test
  void testStatisticConstrainedToOneDRD() {
    // given
    testRule.deploy(SCORE_DRG_DMN);

    //when
    decisionService.evaluateDecisionTableByKey("score-decision")
        .variables(Variables.createVariables().putValue("input", "john"))
        .evaluate();

    decisionService.evaluateDecisionTableByKey("season")
        .variables(Variables.createVariables().putValue(TEMPERATURE, 21))
        .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .decisionRequirementsDefinitionName("Score")
        .singleResult();

    HistoricDecisionInstanceStatisticsQuery statisticsQuery = historyService
        .createHistoricDecisionInstanceStatisticsQuery(
            decisionRequirementsDefinition.getId());

    //then
    assertThat(statisticsQuery.count()).isEqualTo(1L);
    assertThat(statisticsQuery.list()).hasSize(1);
    assertThat(statisticsQuery.list().get(0).getEvaluations()).isEqualTo(1);
    assertThat(statisticsQuery.list().get(0).getDecisionDefinitionKey()).isNotNull();
  }

  @Test
  void testStatisticDoesNotExistForFakeId() {
    assertThat(
        historyService.createHistoricDecisionInstanceStatisticsQuery(
            NON_EXISTING).count()).isZero();

    assertThat(
        historyService.createHistoricDecisionInstanceStatisticsQuery(
            NON_EXISTING).list()).isEmpty();

  }

  @Test
  void testStatisticThrowsExceptionOnNullConstraintsCount() {
    // when/then
    var historicDecisionInstanceStatisticsQuery = historyService.createHistoricDecisionInstanceStatisticsQuery(null);
    assertThatThrownBy(historicDecisionInstanceStatisticsQuery::count)
      .isInstanceOf(NullValueException.class);
  }

  @Test
  void testStatisticThrowsExceptionOnNullConstraintsList() {
    // when/then
    var historicDecisionInstanceStatisticsQuery = historyService.createHistoricDecisionInstanceStatisticsQuery(null);
    assertThatThrownBy(historicDecisionInstanceStatisticsQuery::list)
      .isInstanceOf(NullValueException.class);
  }

  @Test
  void testStatisticForNotEvaluatedDRD() {
    // given
    DecisionRequirementsDefinition decisionRequirementsDefinition =
        repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();

    // when
    HistoricDecisionInstanceStatisticsQuery statisticsQuery = historyService.createHistoricDecisionInstanceStatisticsQuery(
        decisionRequirementsDefinition.getId());

    // then
    assertThat(statisticsQuery.count()).isZero();
    assertThat(statisticsQuery.list()).isEmpty();
  }
}
