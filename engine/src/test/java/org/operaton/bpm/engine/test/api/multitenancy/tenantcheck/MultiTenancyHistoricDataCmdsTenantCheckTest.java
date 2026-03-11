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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricCaseInstanceQuery;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.runtime.CaseInstanceBuilder;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author kristin.polenz
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricDataCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String PROCESS_DEFINITION_KEY = "failingProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected CaseService caseService;
  protected DecisionService decisionService;
  protected HistoryService historyService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected static final BpmnModelInstance BPMN_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent().endEvent().done();

  protected static final BpmnModelInstance BPMN_ONETASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent().userTask("task1").moveToActivity("task1").endEvent().done();

  protected static final BpmnModelInstance FAILING_BPMN_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .serviceTask()
        .operatonExpression("${failing}")
        .operatonAsyncBefore()
      .endEvent()
      .done();

  protected static final String CMMN_PROCESS_WITH_MANUAL_ACTIVATION = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn";

  protected static final String DMN = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";

  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();
    for(HistoricTaskInstance instance : historyService.createHistoricTaskInstanceQuery().list()) {
      historyService.deleteHistoricTaskInstance(instance.getId());
    }
  }

  @Test
  void failToDeleteHistoricProcessInstanceNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    String processInstanceId = startProcessInstance(null);

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstance(processInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No historic process instance found");
  }

  @Test
  void deleteHistoricProcessInstanceWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    String processInstanceId = startProcessInstance(null);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    historyService.deleteHistoricProcessInstance(processInstanceId);

    identityService.clearAuthentication();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    assertThat(query.count()).isZero();
  }

  @Test
  void deleteHistoricProcessInstanceWithDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_PROCESS);

    String processInstanceIdOne = startProcessInstance(TENANT_ONE);
    String processInstanceIdTwo = startProcessInstance(TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    historyService.deleteHistoricProcessInstance(processInstanceIdOne);
    historyService.deleteHistoricProcessInstance(processInstanceIdTwo);

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
    assertThat(query.count()).isZero();
  }

  @Test
  void failToDeleteHistoricTaskInstanceNoAuthenticatedTenants() {
    String taskId = createTaskForTenant(TENANT_ONE);

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricTaskInstance(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the historic task instance");
  }

  @Test
  void deleteHistoricTaskInstanceWithAuthenticatedTenant() {
    String taskId = createTaskForTenant(TENANT_ONE);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    historyService.deleteHistoricTaskInstance(taskId);

    identityService.clearAuthentication();

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    assertThat(query.count()).isZero();
  }

  @Test
  void deleteHistoricTaskInstanceWithDisabledTenantCheck() {
    String taskIdOne = createTaskForTenant(TENANT_ONE);
    String taskIdTwo = createTaskForTenant(TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    historyService.deleteHistoricTaskInstance(taskIdOne);
    historyService.deleteHistoricTaskInstance(taskIdTwo);

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    assertThat(query.count()).isZero();
  }

  @Test
  void failToDeleteHistoricCaseInstanceNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, CMMN_PROCESS_WITH_MANUAL_ACTIVATION);
    String caseInstanceId = createAndCloseCaseInstance(null);

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricCaseInstance(caseInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the historic case instance");
  }

  @Test
  void deleteHistoricCaseInstanceWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, CMMN_PROCESS_WITH_MANUAL_ACTIVATION);
    String caseInstanceId = createAndCloseCaseInstance(null);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    historyService.deleteHistoricCaseInstance(caseInstanceId);

    identityService.clearAuthentication();

    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

    assertThat(query.count()).isZero();
  }

  @Test
  void deleteHistoricCaseInstanceWithDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, CMMN_PROCESS_WITH_MANUAL_ACTIVATION);
    testRule.deployForTenant(TENANT_TWO, CMMN_PROCESS_WITH_MANUAL_ACTIVATION);

    String caseInstanceIdOne = createAndCloseCaseInstance(TENANT_ONE);
    String caseInstanceIdTwo = createAndCloseCaseInstance(TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    historyService.deleteHistoricCaseInstance(caseInstanceIdOne);
    historyService.deleteHistoricCaseInstance(caseInstanceIdTwo);

    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();
    assertThat(query.count()).isZero();
  }

  @Test
  void deleteHistoricDecisionInstanceNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, DMN);

    String decisionDefinitionId = evaluateDecisionTable(null);

    identityService.setAuthentication("user", null, null);

    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId);

    identityService.clearAuthentication();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.count()).isOne();
  }

  @Test
  void deleteHistoricDecisionInstanceWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, DMN);
    String decisionDefinitionId = evaluateDecisionTable(null);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId);

    identityService.clearAuthentication();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.count()).isZero();
  }

  @Test
  void deleteHistoricDecisionInstanceWithDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, DMN);
    testRule.deployForTenant(TENANT_TWO, DMN);

    String decisionDefinitionIdOne = evaluateDecisionTable(TENANT_ONE);
    String decisionDefinitionIdTwo = evaluateDecisionTable(TENANT_TWO);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionIdOne);
    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionIdTwo);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.count()).isZero();
  }

  @Test
  void failToDeleteHistoricDecisionInstanceByInstanceIdNoAuthenticatedTenants() {

    // given
    testRule.deployForTenant(TENANT_ONE, DMN);
    evaluateDecisionTable(null);

    HistoricDecisionInstanceQuery query =
        historyService.createHistoricDecisionInstanceQuery();
    HistoricDecisionInstance historicDecisionInstance = query.includeInputs().includeOutputs().singleResult();
    String historicDecisionInstanceId = historicDecisionInstance.getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the historic decision instance");
  }

  @Test
  void deleteHistoricDecisionInstanceByInstanceIdWithAuthenticatedTenant() {

    // given
    testRule.deployForTenant(TENANT_ONE, DMN);
    evaluateDecisionTable(null);

    HistoricDecisionInstanceQuery query =
        historyService.createHistoricDecisionInstanceQuery();
    HistoricDecisionInstance historicDecisionInstance = query.includeInputs().includeOutputs().singleResult();

    // when
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));
    historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstance.getId());

    // then
    identityService.clearAuthentication();
    assertThat(query.count()).isZero();
  }

  @Test
  void deleteHistoricDecisionInstanceByInstanceIdWithDisabledTenantCheck() {

    // given
    testRule.deployForTenant(TENANT_ONE, DMN);
    testRule.deployForTenant(TENANT_TWO, DMN);

    evaluateDecisionTable(TENANT_ONE);
    evaluateDecisionTable(TENANT_TWO);

    HistoricDecisionInstanceQuery query =
        historyService.createHistoricDecisionInstanceQuery();
    List<HistoricDecisionInstance> historicDecisionInstances = query.includeInputs().includeOutputs().list();
    assertThat(historicDecisionInstances).hasSize(2);

    // when user has no authorization
    identityService.setAuthentication("user", null, null);
    // and when tenant check is disabled
    processEngineConfiguration.setTenantCheckEnabled(false);
    // and when all decision instances are deleted
    for(HistoricDecisionInstance in: historicDecisionInstances){
      historyService.deleteHistoricDecisionInstanceByInstanceId(in.getId());
    }

    // then
    identityService.clearAuthentication();
    assertThat(query.count()).isZero();
  }

  @Test
  void failToGetHistoricJobLogExceptionStacktraceNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, FAILING_BPMN_PROCESS);
    String processInstanceId = startProcessInstance(null);

    String historicJobLogId = historyService.createHistoricJobLogQuery()
        .processInstanceId(processInstanceId).singleResult().getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> historyService.getHistoricJobLogExceptionStacktrace(historicJobLogId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the historic job log");

  }

  @Test
  void getHistoricJobLogExceptionStacktraceWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, FAILING_BPMN_PROCESS);
    String processInstanceId = startProcessInstance(null);

    testRule.executeAvailableJobs();

    HistoricJobLog log = historyService.createHistoricJobLogQuery()
        .processInstanceId(processInstanceId).failureLog().listPage(0, 1).get(0);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    String historicJobLogExceptionStacktrace = historyService.getHistoricJobLogExceptionStacktrace(log.getId());

    assertThat(historicJobLogExceptionStacktrace).isNotNull();
  }

  @Test
  void getHistoricJobLogExceptionStacktraceWithDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, FAILING_BPMN_PROCESS);

    String processInstanceId = startProcessInstance(TENANT_ONE);

    testRule.executeAvailableJobs();

    HistoricJobLog log = historyService.createHistoricJobLogQuery()
        .processInstanceId(processInstanceId).failureLog().listPage(0, 1).get(0);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    String historicJobLogExceptionStacktrace = historyService.getHistoricJobLogExceptionStacktrace(log.getId());

    assertThat(historicJobLogExceptionStacktrace).isNotNull();
  }

  @Test
  void failToDeleteHistoricVariableInstanceNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, BPMN_ONETASK_PROCESS);
    String processInstanceId = startProcessInstance(null);
    runtimeService.setVariable(processInstanceId, "myVariable", "testValue");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).singleResult().getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstance(variableInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the historic variable instance '%s' because it belongs to no authenticated tenant.".formatted(variableInstanceId));

    cleanUpAfterVariableInstanceTest(processInstanceId);
  }

  @Test
  void deleteHistoricVariableInstanceWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, BPMN_ONETASK_PROCESS);
    String processInstanceId = startProcessInstance(null);
    runtimeService.setVariable(processInstanceId, "myVariable", "testValue");
    HistoricVariableInstanceQuery variableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId);

    assertThat(variableQuery.count()).isOne();
    String variableInstanceId = variableQuery.singleResult().getId();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    historyService.deleteHistoricVariableInstance(variableInstanceId);
    assertThat(variableQuery.count()).isZero();
    cleanUpAfterVariableInstanceTest(processInstanceId);
  }

  @Test
  void deleteHistoricVariableInstanceWithDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, BPMN_ONETASK_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_ONETASK_PROCESS);

    String processInstanceIdOne = startProcessInstance(TENANT_ONE);
    String processInstanceIdTwo = startProcessInstance(TENANT_TWO);

    runtimeService.setVariable(processInstanceIdOne, "myVariable", "testValue");
    runtimeService.setVariable(processInstanceIdTwo, "myVariable", "testValue");
    HistoricVariableInstanceQuery variableQueryOne = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIdOne);
    HistoricVariableInstanceQuery variableQueryTwo = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIdTwo);

    assertThat(variableQueryOne.count()).isOne();
    assertThat(variableQueryTwo.count()).isOne();
    String variableInstanceIdOne = variableQueryOne.singleResult().getId();
    String variableInstanceIdTwo = variableQueryTwo.singleResult().getId();

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    historyService.deleteHistoricVariableInstance(variableInstanceIdOne);
    historyService.deleteHistoricVariableInstance(variableInstanceIdTwo);
    assertThat(variableQueryOne.count()).isZero();
    assertThat(variableQueryTwo.count()).isZero();

    cleanUpAfterVariableInstanceTest(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  void failToDeleteHistoricVariableInstancesNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, BPMN_ONETASK_PROCESS);
    String processInstanceId = startProcessInstance(null);
    runtimeService.setVariable(processInstanceId, "myVariable", "testValue");
    runtimeService.setVariable(processInstanceId, "myVariable", "testValue2");

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the historic variable instances of process instance '%s' because it belongs to no authenticated tenant.".formatted(processInstanceId));

    cleanUpAfterVariableInstanceTest(processInstanceId);
  }

  @Test
  void deleteHistoricVariableInstancesWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, BPMN_ONETASK_PROCESS);
    String processInstanceId = startProcessInstance(null);
    runtimeService.setVariable(processInstanceId, "myVariable", "testValue");
    runtimeService.setVariable(processInstanceId, "myVariable", "testValue2");

    HistoricVariableInstanceQuery variableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId);
    assertThat(variableQuery.count()).isOne();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    historyService.deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
    assertThat(variableQuery.count()).isZero();
    cleanUpAfterVariableInstanceTest(processInstanceId);
  }

  @Test
  void deleteHistoricVariableInstancesWithDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, BPMN_ONETASK_PROCESS);
    testRule.deployForTenant(TENANT_TWO, BPMN_ONETASK_PROCESS);

    String processInstanceIdOne = startProcessInstance(TENANT_ONE);
    String processInstanceIdTwo = startProcessInstance(TENANT_TWO);

    runtimeService.setVariable(processInstanceIdOne, "myVariable", "testValue");
    runtimeService.setVariable(processInstanceIdOne, "mySecondVariable", "testValue2");
    runtimeService.setVariable(processInstanceIdTwo, "myVariable", "testValue");
    runtimeService.setVariable(processInstanceIdTwo, "mySecondVariable", "testValue2");
    HistoricVariableInstanceQuery variableQueryOne = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIdOne);
    HistoricVariableInstanceQuery variableQueryTwo = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIdTwo);

    assertThat(variableQueryOne.count()).isEqualTo(2L);
    assertThat(variableQueryTwo.count()).isEqualTo(2L);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    historyService.deleteHistoricVariableInstancesByProcessInstanceId(processInstanceIdOne);
    assertThat(variableQueryOne.count()).isZero();
    assertThat(variableQueryTwo.count()).isEqualTo(2L);

    historyService.deleteHistoricVariableInstancesByProcessInstanceId(processInstanceIdTwo);
    assertThat(variableQueryTwo.count()).isZero();

    cleanUpAfterVariableInstanceTest(processInstanceIdOne, processInstanceIdTwo);
  }

  // helper //////////////////////////////////////////////////////////

  protected String startProcessInstance(String tenantId) {
    if (tenantId == null) {
      return runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    } else {
      return runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY)
          .processDefinitionTenantId(tenantId).execute().getId();
    }
  }

  protected String createAndCloseCaseInstance(String tenantId) {
    String caseInstanceId;

    CaseInstanceBuilder builder = caseService.withCaseDefinitionByKey("oneTaskCase");
    if (tenantId == null) {
      caseInstanceId = builder.create().getId();
    } else {
      caseInstanceId =  builder.caseDefinitionTenantId(tenantId).create().getId();
    }

    caseService.completeCaseExecution(caseInstanceId);
    caseService.closeCaseInstance(caseInstanceId);

    return caseInstanceId;
  }

  protected String evaluateDecisionTable(String tenantId) {
    String decisionDefinitionId;

    if (tenantId == null) {
      decisionDefinitionId = repositoryService.createDecisionDefinitionQuery().singleResult().getId();
    } else {
      decisionDefinitionId = repositoryService.createDecisionDefinitionQuery()
          .tenantIdIn(tenantId).singleResult().getId();
    }

    VariableMap variables = Variables.createVariables().putValue("status", "bronze");
    decisionService.evaluateDecisionTableById(decisionDefinitionId, variables);

    return decisionDefinitionId;
  }

  protected String createTaskForTenant(String tenantId) {
    Task task = taskService.newTask();
    task.setTenantId(tenantId);

    taskService.saveTask(task);
    taskService.complete(task.getId());

    return task.getId();
  }

  protected void cleanUpAfterVariableInstanceTest(String... processInstanceIds) {
    processEngineConfiguration.setTenantCheckEnabled(false);
    for (String processInstanceId : processInstanceIds) {
      Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
      if (task != null) {
        taskService.complete(task.getId());
      }
      historyService.deleteHistoricProcessInstance(processInstanceId);
    }

    identityService.clearAuthentication();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
    assertThat(query.count()).isZero();
    processEngineConfiguration.setTenantCheckEnabled(true);
  }

}
