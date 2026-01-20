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

import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.DurationReportResult;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.query.PeriodUnit.MONTH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kristin.polenz
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricProcessInstanceReportCmdTenantCheckTest {

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
  protected TaskService taskService;
  protected HistoryService historyService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected static final BpmnModelInstance BPMN_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .userTask()
      .endEvent()
    .done();

  @Test
  void getDurationReportByMonthNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);

    startAndCompleteProcessInstance(null);

    identityService.setAuthentication("user", null, null);

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(MONTH);

    assertThat(result).isEmpty();
  }

  @Test
  void getDurationReportByMonthWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);

    startAndCompleteProcessInstance(null);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(MONTH);

    assertThat(result).hasSize(1);
  }

  @Test
  void getDurationReportByMonthDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);

    startAndCompleteProcessInstance(null);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(MONTH);

    assertThat(result).hasSize(1);
  }

  @Test
  void getReportByMultipleProcessDefinitionIdByMonthNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    startAndCompleteProcessInstance(TENANT_ONE);
    startAndCompleteProcessInstance(TENANT_TWO);

    String processDefinitionIdOne = repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).singleResult().getId();
    String processDefinitionIdTwo = repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_TWO).singleResult().getId();

    identityService.setAuthentication("user", null, null);

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processDefinitionIdOne, processDefinitionIdTwo)
        .duration(MONTH);

    assertThat(result).isEmpty();
  }

  @Test
  void getReportByMultipleProcessDefinitionIdByMonthWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    startAndCompleteProcessInstance(TENANT_ONE);
    startAndCompleteProcessInstance(TENANT_TWO);

    String processDefinitionIdOne = repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).singleResult().getId();
    String processDefinitionIdTwo = repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_TWO).singleResult().getId();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processDefinitionIdOne, processDefinitionIdTwo)
        .duration(MONTH);

    assertThat(result).hasSize(1);
  }

  @Test
  void getReportByMultipleProcessDefinitionIdByMonthDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    startAndCompleteProcessInstance(TENANT_ONE);
    startAndCompleteProcessInstance(TENANT_TWO);

    String processDefinitionIdOne = repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).singleResult().getId();
    String processDefinitionIdTwo = repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_TWO).singleResult().getId();

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processDefinitionIdOne, processDefinitionIdTwo)
        .duration(MONTH);

    assertThat(result).hasSize(2);
  }

  @Test
  void getReportByProcessDefinitionKeyByMonthNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    startAndCompleteProcessInstance(TENANT_ONE);
    startAndCompleteProcessInstance(TENANT_TWO);

    identityService.setAuthentication("user", null, null);

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn(PROCESS_DEFINITION_KEY)
        .duration(MONTH);

    assertThat(result).isEmpty();
  }

  @Test
  void getReportByProcessDefinitionKeyByMonthWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    startAndCompleteProcessInstance(TENANT_ONE);
    startAndCompleteProcessInstance(TENANT_TWO);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn(PROCESS_DEFINITION_KEY)
        .duration(MONTH);

    assertThat(result).hasSize(1);
  }

  @Test
  void getReportByProcessDefinitionKeyByMonthDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    startAndCompleteProcessInstance(TENANT_ONE);
    startAndCompleteProcessInstance(TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn(PROCESS_DEFINITION_KEY)
        .duration(MONTH);

    assertThat(result).hasSize(2);
  }

  // helper //////////////////////////////////////////////////////////

  protected String startAndCompleteProcessInstance(String tenantId) {
    String processInstanceId = null;
    if (tenantId == null) {
      processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    } else {
      processInstanceId = runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY)
          .processDefinitionTenantId(tenantId).execute().getId();
    }

    addToCalendar(Calendar.MONTH, 5);

    Task task = taskService.createTaskQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    taskService.complete(task.getId());

    return processInstanceId;
  }

  protected void addToCalendar(int field, int month) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(ClockUtil.getCurrentTime());
    calendar.add(field, month);
    ClockUtil.setCurrentTime(calendar.getTime());
  }

}
