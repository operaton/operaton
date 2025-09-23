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
package org.operaton.bpm.engine.test.api.authorization.history;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReportResult;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE_HISTORY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.DECISION_DEFINITION;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Philipp Ossler
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricDecisionInstanceAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "testProcess";
  protected static final String DECISION_DEFINITION_KEY = "testDecision";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy("org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.processWithBusinessRuleTask.bpmn20.xml",
        "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml");

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();
  }

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startProcessInstanceAndEvaluateDecision();

    // when
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnDecisionDefinition() {
    // given
    startProcessInstanceAndEvaluateDecision();
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ_HISTORY);

    // when
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryWithReadPermissionOnAnyDecisionDefinition() {
    // given
    startProcessInstanceAndEvaluateDecision();
    createGrantAuthorization(DECISION_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryWithMultiple() {
    // given
    startProcessInstanceAndEvaluateDecision();
    createGrantAuthorization(DECISION_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ_HISTORY);

    // when
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void shouldNotFindDecisionInstanceWithRevokedReadPermissionOnAnyDecisionDefinition() {
    // given
    startProcessInstanceAndEvaluateDecision();
    createGrantAuthorization(DECISION_DEFINITION, ANY, ANY, READ_HISTORY);
    createRevokeAuthorization(DECISION_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testDeleteHistoricDecisionInstanceWithoutAuthorization(){
    // given
    startProcessInstanceAndEvaluateDecision();
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();

    assertThatThrownBy(() -> historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId))
      .isInstanceOf(AuthorizationException.class)
      .hasMessage("The user with id 'test' does not have 'DELETE_HISTORY' permission on resource 'testDecision' of type 'DecisionDefinition'.");
  }

  @Test
  void testDeleteHistoricDecisionInstanceWithDeleteHistoryPermissionOnDecisionDefinition() {
    // given
    startProcessInstanceAndEvaluateDecision();
    createGrantAuthorization(DECISION_DEFINITION, ANY, userId, DELETE_HISTORY);
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();


    // when
    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId);

    // then
    disableAuthorization();
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
    enableAuthorization();
}

  @Test
  void testDeleteHistoricDecisionInstanceWithDeleteHistoryPermissionOnAnyDecisionDefinition() {
    // given
    startProcessInstanceAndEvaluateDecision();
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, DELETE_HISTORY);
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();

    // when
    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId);

    // then
    disableAuthorization();
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
    enableAuthorization();
  }

  @Test
  void testDeleteHistoricDecisionInstanceByInstanceIdWithoutAuthorization() {

    // given
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ_HISTORY);
    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    String historicDecisionInstanceId = query.includeInputs().includeOutputs().singleResult().getId();

    assertThatThrownBy(() -> historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstanceId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessage("The user with id 'test' does not have 'DELETE_HISTORY' permission on resource 'testDecision' of type 'DecisionDefinition'.");
  }

  @Test
  void testDeleteHistoricDecisionInstanceByInstanceIdWithDeleteHistoryPermissionOnDecisionDefinition() {

    // given
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, DELETE_HISTORY, READ_HISTORY);
    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    verifyQueryResults(query, 1);
    HistoricDecisionInstance historicDecisionInstance = query.includeInputs().includeOutputs().singleResult();

    // when
    historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstance.getId());

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testHistoryCleanupReportWithoutAuthorization() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void testHistoryCleanupReportWithAuthorization() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10);

    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, Permissions.READ, Permissions.READ_HISTORY);
    createGrantAuthorizationGroup(DECISION_DEFINITION, DECISION_DEFINITION_KEY, groupId, Permissions.READ, Permissions.READ_HISTORY);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getCleanableDecisionInstanceCount()).isEqualTo(10);
    assertThat(reportResults.get(0).getFinishedDecisionInstanceCount()).isEqualTo(10);
  }

  @Test
  void testHistoryCleanupReportWithReadPermissionOnly() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10);

    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, Permissions.READ);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void testHistoryCleanupReportWithReadHistoryPermissionOnly() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10);

    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, Permissions.READ_HISTORY);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void shouldNotFindCleanupReportWithRevokedReadHistoryPermissionOnDecisionDefinition() {
    // given
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10);

    createGrantAuthorization(DECISION_DEFINITION, ANY, userId, Permissions.READ, Permissions.READ_HISTORY);
    createRevokeAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, Permissions.READ_HISTORY);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  protected void startProcessInstanceAndEvaluateDecision() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("input1", null);
    startProcessInstanceByKey(PROCESS_KEY, variables);
  }

  protected void prepareDecisionInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount) {
    DecisionDefinition decisionDefinition = selectDecisionDefinitionByKey(key);
    disableAuthorization();
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), historyTimeToLive);
    enableAuthorization();

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(oldCurrentTime, daysInThePast));

    Map<String, Object> variables = Variables.createVariables().putValue("input1", null);
    for (int i = 0; i < instanceCount; i++) {
      disableAuthorization();
      decisionService.evaluateDecisionByKey(key).variables(variables).evaluate();
      enableAuthorization();
    }

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

}
