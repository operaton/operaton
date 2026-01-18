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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstanceBuilder;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.StringValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author kristin.polenz
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyCaseInstanceCmdsTenantCheckTest {

  protected static final String VARIABLE_NAME = "myVar";
  protected static final String VARIABLE_VALUE = "myValue";

  protected static final String TENANT_ONE = "tenant1";

  protected static final String CMMN_MODEL = "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn";

  protected static final String ACTIVITY_ID = "PI_HumanTask_1";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected IdentityService identityService;
  protected CaseService caseService;
  protected HistoryService historyService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected String caseInstanceId;
  protected String caseExecutionId;

  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_ONE, CMMN_MODEL);

    caseInstanceId = createCaseInstance(null);

    caseExecutionId = getCaseExecution().getId();
  }

  @Test
  void manuallyStartCaseExecutionNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.manuallyStartCaseExecution(caseExecutionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void manuallyStartCaseExecutionWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.manuallyStartCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    CaseExecution caseExecution = getCaseExecution();

    assertThat(caseExecution.isActive()).isTrue();
  }

  @Test
  void manuallyStartCaseExecutionDisabledTenantCheck() {
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.manuallyStartCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    CaseExecution caseExecution = getCaseExecution();

    assertThat(caseExecution.isActive()).isTrue();
  }

  @Test
  void disableCaseExecutionNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.disableCaseExecution(caseExecutionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void disableCaseExecutionWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.disableCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    HistoricCaseActivityInstance historicCaseActivityInstance = getHistoricCaseActivityInstance();

    assertThat(historicCaseActivityInstance).isNotNull();
    assertThat(historicCaseActivityInstance.isDisabled()).isTrue();
  }

  @Test
  void disableCaseExecutionDisabledTenantCheck() {
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.disableCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    HistoricCaseActivityInstance historicCaseActivityInstance = getHistoricCaseActivityInstance();

    assertThat(historicCaseActivityInstance).isNotNull();
    assertThat(historicCaseActivityInstance.isDisabled()).isTrue();
  }

  @Test
  void reenableCaseExecutionNoAuthenticatedTenants() {
    caseService.disableCaseExecution(caseExecutionId);

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.reenableCaseExecution(caseExecutionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void reenableCaseExecutionWithAuthenticatedTenant() {
    caseService.disableCaseExecution(caseExecutionId);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.reenableCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    CaseExecution caseExecution = getCaseExecution();

    assertThat(caseExecution.isEnabled()).isTrue();
  }

  @Test
  void reenableCaseExecutionDisabledTenantCheck() {
    caseService.disableCaseExecution(caseExecutionId);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.reenableCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    CaseExecution caseExecution = getCaseExecution();

    assertThat(caseExecution.isEnabled()).isTrue();
  }

  @Test
  void completeCaseExecutionNoAuthenticatedTenants() {
    caseService.manuallyStartCaseExecution(caseExecutionId);

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.completeCaseExecution(caseExecutionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void completeCaseExecutionWithAuthenticatedTenant() {
    caseService.manuallyStartCaseExecution(caseExecutionId);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.completeCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    HistoricCaseActivityInstance historicCaseActivityInstance = getHistoricCaseActivityInstance();

    assertThat(historicCaseActivityInstance).isNotNull();
    assertThat(historicCaseActivityInstance.isCompleted()).isTrue();
  }

  @Test
  void completeCaseExecutionDisabledTenantCheck() {
    caseService.manuallyStartCaseExecution(caseExecutionId);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.completeCaseExecution(caseExecutionId);

    identityService.clearAuthentication();

    HistoricCaseActivityInstance historicCaseActivityInstance = getHistoricCaseActivityInstance();

    assertThat(historicCaseActivityInstance).isNotNull();
    assertThat(historicCaseActivityInstance.isCompleted()).isTrue();
  }

  @Test
  void closeCaseInstanceNoAuthenticatedTenants() {
    caseService.completeCaseExecution(caseInstanceId);

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.closeCaseInstance(caseInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void closeCaseInstanceWithAuthenticatedTenant() {
    caseService.completeCaseExecution(caseInstanceId);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.closeCaseInstance(caseInstanceId);

    identityService.clearAuthentication();

    HistoricCaseInstance historicCaseInstance = getHistoricCaseInstance();

    assertThat(historicCaseInstance).isNotNull();
    assertThat(historicCaseInstance.isClosed()).isTrue();
  }

  @Test
  void closeCaseInstanceDisabledTenantCheck() {
    caseService.completeCaseExecution(caseInstanceId);

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.closeCaseInstance(caseInstanceId);

    identityService.clearAuthentication();

    HistoricCaseInstance historicCaseInstance = getHistoricCaseInstance();

    assertThat(historicCaseInstance).isNotNull();
    assertThat(historicCaseInstance.isClosed()).isTrue();
  }

  @Test
  void terminateCaseInstanceNoAuthenticatedTenants() {

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.terminateCaseExecution(caseInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void terminateCaseExecutionWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.terminateCaseExecution(caseInstanceId);

    HistoricCaseInstance historicCaseInstance = getHistoricCaseInstance();

    assertThat(historicCaseInstance).isNotNull();
    assertThat(historicCaseInstance.isTerminated()).isTrue();

  }

  @Test
  void terminateCaseExecutionDisabledTenantCheck() {

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.terminateCaseExecution(caseInstanceId);

    HistoricCaseInstance historicCaseInstance = getHistoricCaseInstance();

    assertThat(historicCaseInstance).isNotNull();
    assertThat(historicCaseInstance.isTerminated()).isTrue();
  }

  @Test
  void getVariablesNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.getVariables(caseExecutionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the case execution");
  }

  @Test
  void getVariablesWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    Map<String, Object> variables = caseService.getVariables(caseExecutionId);

    assertThat(variables)
      .isNotNull()
      .containsKey(VARIABLE_NAME);
  }

  @Test
  void getVariablesDisabledTenantCheck() {
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    Map<String, Object> variables = caseService.getVariables(caseExecutionId);

    assertThat(variables)
      .isNotNull()
      .containsKey(VARIABLE_NAME);
  }

  @Test
  void getVariableNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.getVariable(caseExecutionId, VARIABLE_NAME))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the case execution");
  }

  @Test
  void getVariableWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    String variableValue = (String) caseService.getVariable(caseExecutionId, VARIABLE_NAME);

    assertThat(variableValue).isEqualTo(VARIABLE_VALUE);
  }

  @Test
  void getVariableDisabledTenantCheck() {
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    String variableValue = (String) caseService.getVariable(caseExecutionId, VARIABLE_NAME);

    assertThat(variableValue).isEqualTo(VARIABLE_VALUE);
  }

  @Test
  void getVariableTypedNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.getVariableTyped(caseExecutionId, VARIABLE_NAME))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the case execution");
  }

  @Test
  void getVariableTypedWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    StringValue variable = caseService.getVariableTyped(caseExecutionId, VARIABLE_NAME);

    assertThat(variable.getValue()).isEqualTo(VARIABLE_VALUE);
  }

  @Test
  void getVariableTypedDisabledTenantCheck() {
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    StringValue variable = caseService.getVariableTyped(caseExecutionId, VARIABLE_NAME);

    assertThat(variable.getValue()).isEqualTo(VARIABLE_VALUE);
  }

  @Test
  void removeVariablesNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.removeVariable(caseExecutionId, VARIABLE_NAME))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void removeVariablesWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.removeVariable(caseExecutionId, VARIABLE_NAME);

    identityService.clearAuthentication();

    Map<String, Object> variables = caseService.getVariables(caseExecutionId);
    assertThat(variables).isEmpty();
  }

  @Test
  void removeVariablesDisabledTenantCheck() {
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.removeVariable(caseExecutionId, VARIABLE_NAME);

    identityService.clearAuthentication();

    Map<String, Object> variables = caseService.getVariables(caseExecutionId);
    assertThat(variables).isEmpty();
  }

  @Test
  void setVariableNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> caseService.setVariable(caseExecutionId, "newVar", "newValue"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the case execution");
  }

  @Test
  void setVariableWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    caseService.setVariable(caseExecutionId, "newVar", "newValue");

    identityService.clearAuthentication();

    Map<String, Object> variables = caseService.getVariables(caseExecutionId);
    assertThat(variables)
      .isNotNull()
      .containsKeys(VARIABLE_NAME, "newVar");
  }

  @Test
  void setVariableDisabledTenantCheck() {
    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    caseService.setVariable(caseExecutionId, "newVar", "newValue");

    identityService.clearAuthentication();

    Map<String, Object> variables = caseService.getVariables(caseExecutionId);
    assertThat(variables)
      .isNotNull()
      .containsKeys(VARIABLE_NAME, "newVar");
  }

  protected String createCaseInstance(String tenantId) {
    VariableMap variables = Variables.putValue(VARIABLE_NAME, VARIABLE_VALUE);
    CaseInstanceBuilder builder = caseService.withCaseDefinitionByKey("twoTaskCase").setVariables(variables);
    if (tenantId == null) {
      return builder.create().getId();
    } else {
      return builder.caseDefinitionTenantId(tenantId).create().getId();
    }
  }

  protected CaseExecution getCaseExecution() {
    return caseService.createCaseExecutionQuery().activityId(ACTIVITY_ID).singleResult();
  }

  protected HistoricCaseActivityInstance getHistoricCaseActivityInstance() {
    return historyService.createHistoricCaseActivityInstanceQuery().caseActivityId(ACTIVITY_ID).singleResult();
  }

  protected HistoricCaseInstance getHistoricCaseInstance() {
    return historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();
  }

}
