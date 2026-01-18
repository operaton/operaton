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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReportResult;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyCleanableHistoricDecisionInstanceReportCmdTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  private static final String DECISION_DEFINITION_KEY = "decision";

  protected static final String DMN_MODEL = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected TaskService taskService;
  protected HistoryService historyService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  @Test
  void testReportNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    identityService.setAuthentication("user", null, null);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void testReportWithAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testReportDisabledTenantCheck() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10,TENANT_ONE);
    testRule.deployForTenant(TENANT_TWO, DMN_MODEL);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(2);
    assertThat(reportResults.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResults.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testReportTenantIdInNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);
    testRule.deployForTenant(TENANT_TWO, DMN_MODEL);

    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, null);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricDecisionInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricDecisionInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricDecisionInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).isEmpty();
    assertThat(reportResultsTwo).isEmpty();
  }

  @Test
  void testReportTenantIdInWithAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);
    testRule.deployForTenant(TENANT_TWO, DMN_MODEL);

    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricDecisionInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricDecisionInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricDecisionInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResultsTwo).isEmpty();
  }

  @Test
  void testReportTenantIdInDisabledTenantCheck() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);
    testRule.deployForTenant(TENANT_TWO, DMN_MODEL);

    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricDecisionInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricDecisionInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricDecisionInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResultsTwo).hasSize(1);
    assertThat(reportResultsTwo.get(0).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testReportWithoutTenantId() {
    // given
    testRule.deploy(DMN_MODEL);

    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, null);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().withoutTenantId().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isNull();
  }

  @Test
  void testReportTenantIdInWithoutTenantId() {
    // given
    testRule.deploy(DMN_MODEL);
    testRule.deployForTenant(TENANT_ONE, DMN_MODEL);

    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, null);
    prepareDecisionInstances(DECISION_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);

    // when
    List<CleanableHistoricDecisionInstanceReportResult> reportResults = historyService.createCleanableHistoricDecisionInstanceReport().withoutTenantId().list();
    List<CleanableHistoricDecisionInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricDecisionInstanceReport().tenantIdIn(TENANT_ONE).list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isNull();
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
  }

  protected void prepareDecisionInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount, String tenantId) {
    List<DecisionDefinition> decisionDefinitions = null;
    if (tenantId != null) {
      decisionDefinitions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(key).tenantIdIn(tenantId).list();
    } else {
      decisionDefinitions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(key).withoutTenantId().list();
    }
    assertThat(decisionDefinitions).hasSize(1);
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitions.get(0).getId(), historyTimeToLive);

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(oldCurrentTime, daysInThePast));

    Map<String, Object> variables = Variables.createVariables().putValue("status", "silver").putValue("sum", 723);
    for (int i = 0; i < instanceCount; i++) {
      if (tenantId != null) {
        engineRule.getDecisionService().evaluateDecisionByKey(key).decisionDefinitionTenantId(tenantId).variables(variables).evaluate();
      } else {
        engineRule.getDecisionService().evaluateDecisionByKey(key).decisionDefinitionWithoutTenantId().variables(variables).evaluate();
      }
    }

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

}
