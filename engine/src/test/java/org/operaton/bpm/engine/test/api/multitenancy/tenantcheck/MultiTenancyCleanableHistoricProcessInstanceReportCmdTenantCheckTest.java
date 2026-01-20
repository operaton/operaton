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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReportResult;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyCleanableHistoricProcessInstanceReportCmdTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String PROCESS_DEFINITION_KEY = "testProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected static final BpmnModelInstance BPMN_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .userTask()
      .endEvent()
    .done();

  @Test
  void testReportNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);

    identityService.setAuthentication("user", null, null);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void testReportWithAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testReportDisabledTenantCheck() {
    // given
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(2);
    assertThat(reportResults.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResults.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testReportTenantIdInNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, null);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricProcessInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricProcessInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricProcessInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).isEmpty();
    assertThat(reportResultsTwo).isEmpty();
  }

  @Test
  void testReportTenantIdInWithAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricProcessInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricProcessInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricProcessInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResultsTwo).isEmpty();
  }

  @Test
  void testReportTenantIdInDisabledTenantCheck() {
    // given
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricProcessInstanceReport().tenantIdIn(TENANT_ONE).list();
    List<CleanableHistoricProcessInstanceReportResult> reportResultsTwo = historyService.createCleanableHistoricProcessInstanceReport().tenantIdIn(TENANT_TWO).list();

    // then
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(reportResultsTwo).hasSize(1);
    assertThat(reportResultsTwo.get(0).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testReportWithoutTenantId() {
    // given
    testRule.deploy(BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, null);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().withoutTenantId().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isNull();
  }

  @Test
  void testReportTenantIdInWithoutTenantId() {
    // given
    testRule.deploy(BPMN_PROCESS);
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);

    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, TENANT_ONE);
    prepareProcessInstances(PROCESS_DEFINITION_KEY, -6, 5, 10, null);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().withoutTenantId().list();
    List<CleanableHistoricProcessInstanceReportResult> reportResultsOne = historyService.createCleanableHistoricProcessInstanceReport().tenantIdIn(TENANT_ONE).list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getTenantId()).isNull();
    assertThat(reportResultsOne).hasSize(1);
    assertThat(reportResultsOne.get(0).getTenantId()).isEqualTo(TENANT_ONE);
  }

  protected void prepareProcessInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount, String tenantId) {
    List<ProcessDefinition> processDefinitions = null;
    if (tenantId == null) {
      processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).withoutTenantId().list();
    } else {
      processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).tenantIdIn(tenantId).list();
    }
    assertThat(processDefinitions).hasSize(1);
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitions.get(0).getId(), historyTimeToLive);

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(oldCurrentTime, daysInThePast));

    List<String> processInstanceIds = new ArrayList<>();
    {
      for (int i = 0; i < instanceCount; i++) {
        String processInstanceId = null;
        if (tenantId == null) {
          processInstanceId = runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionWithoutTenantId().execute().getId();
        } else {
          processInstanceId = runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionTenantId(tenantId).execute().getId();
        }
        processInstanceIds.add(processInstanceId);
      }
    }
    runtimeService.deleteProcessInstances(processInstanceIds, null, true, true);

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

}
