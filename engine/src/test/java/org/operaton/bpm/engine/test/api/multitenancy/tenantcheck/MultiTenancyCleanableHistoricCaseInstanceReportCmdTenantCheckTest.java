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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.history.CleanableHistoricCaseInstanceReportResult;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class MultiTenancyCleanableHistoricCaseInstanceReportCmdTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  private static final String CASE_DEFINITION_KEY = "one";

  protected static final String CMMN_MODEL = "org/operaton/bpm/engine/test/repository/one.cmmn";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();

  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected ProcessEngineConfiguration processEngineConfiguration;
  protected CaseService caseService;
  protected HistoryService historyService;

  protected String caseDefinitionId;

  @Before
  public void init() {
    repositoryService = engineRule.getRepositoryService();
    identityService = engineRule.getIdentityService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    caseService = engineRule.getCaseService();
    historyService = engineRule.getHistoryService();
  }

  private void prepareCaseInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount, String tenantId) {
    // update time to live
    List<CaseDefinition> caseDefinitions = null;
    if (tenantId != null) {
      caseDefinitions = repositoryService.createCaseDefinitionQuery().caseDefinitionKey(key).tenantIdIn(tenantId).list();
    } else {
      caseDefinitions = repositoryService.createCaseDefinitionQuery().caseDefinitionKey(key).withoutTenantId().list();
    }
    assertThat(caseDefinitions).hasSize(1);
    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitions.get(0).getId(), historyTimeToLive);

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(new Date(), daysInThePast));

    for (int i = 0; i < instanceCount; i++) {
      CaseInstance caseInstance = caseService.createCaseInstanceById(caseDefinitions.get(0).getId());
      if (tenantId != null) {
        assertThat(caseInstance.getTenantId()).isEqualTo(tenantId);
      }
      caseService.terminateCaseExecution(caseInstance.getId());
      caseService.closeCaseInstance(caseInstance.getId());
    }

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

  @Test
  public void testReportNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    identityService.setAuthentication("user", null, null);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  public void testReportWithAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  public void testReportDisabledTenantCheck() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    testRule.deployForTenant(TENANT_TWO, CMMN_MODEL);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(2);
    assertThat(reportResults.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResults.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  public void testReportTenantIdInNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);
    testRule.deployForTenant(TENANT_TWO, CMMN_MODEL);

    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, null);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricCaseInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricCaseInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricCaseInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).isEmpty();
    assertThat(reportResultsTwo).isEmpty();
  }

  @Test
  public void testReportTenantIdInWithAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);
    testRule.deployForTenant(TENANT_TWO, CMMN_MODEL);

    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricCaseInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricCaseInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricCaseInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResultsTwo).isEmpty();
  }

  @Test
  public void testReportTenantIdInDisabledTenantCheck() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);
    testRule.deployForTenant(TENANT_TWO, CMMN_MODEL);

    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricCaseInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricCaseInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricCaseInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResultsTwo).hasSize(1);
    assertThat(reportResultsTwo.get(0).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  public void testReportWithoutTenantId() {
    // given
    testRule.deploy(CMMN_MODEL);

    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, null);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().withoutTenantId().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isNull();
  }

  @Test
  public void testReportTenantIdInWithoutTenantId() {
    // given
    testRule.deploy(CMMN_MODEL);
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);

    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, null);
    prepareCaseInstances(CASE_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);

    // when
    List<CleanableHistoricCaseInstanceReportResult> reportResults = historyService.createCleanableHistoricCaseInstanceReport().withoutTenantId().list();
    List<CleanableHistoricCaseInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricCaseInstanceReport().tenantIdIn(TENANT_ONE).list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isNull();
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
  }
}
