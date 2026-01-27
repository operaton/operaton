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
package org.operaton.bpm.engine.test.api.multitenancy.cmmn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyProcessTaskTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String CMMN_LATEST = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithProcessTask.cmmn";
  protected static final String CMMN_LATEST_WITH_MANUAL_ACTIVATION = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithProcessTaskWithManualActivation.cmmn";
  protected static final String CMMN_DEPLOYMENT = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithProcessTaskDeploymentBinding.cmmn";
  protected static final String CMMN_VERSION = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithProcessTaskVersionBinding.cmmn";
  protected static final String CMMN_VERSION_2 = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithProcessTaskVersionBinding_v2.cmmn";

  protected static final String CMMN_TENANT_CONST = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithProcessTaskTenantIdConst.cmmn";
  protected static final String CMMN_TENANT_EXPR = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithProcessTaskTenantIdExpr.cmmn";

  protected static final String PROCESS_TASK_ID = "PI_ProcessTask_1";

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .userTask()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected CaseService caseService;

  @Test
  void testStartProcessInstanceWithDeploymentBinding() {

    testRule.deployForTenant(TENANT_ONE, PROCESS, CMMN_DEPLOYMENT);
    testRule.deployForTenant(TENANT_TWO, PROCESS, CMMN_DEPLOYMENT);

    createCaseInstance("testCaseDeployment", TENANT_ONE);
    createCaseInstance("testCaseDeployment", TENANT_TWO);

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("testProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartProcessInstanceWithLatestBindingSameVersion() {

    testRule.deployForTenant(TENANT_ONE, PROCESS, CMMN_LATEST_WITH_MANUAL_ACTIVATION);
    testRule.deployForTenant(TENANT_TWO, PROCESS, CMMN_LATEST_WITH_MANUAL_ACTIVATION);

    createCaseInstance("testCase", TENANT_ONE);
    createCaseInstance("testCase", TENANT_TWO);

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("testProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartProcessInstanceWithLatestBindingDifferentVersion() {

    testRule.deployForTenant(TENANT_ONE, PROCESS, CMMN_LATEST_WITH_MANUAL_ACTIVATION);

    testRule.deployForTenant(TENANT_TWO, PROCESS, CMMN_LATEST_WITH_MANUAL_ACTIVATION);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    createCaseInstance("testCase", TENANT_ONE);
    createCaseInstance("testCase", TENANT_TWO);

    ProcessDefinition latestProcessTenantTwo = repositoryService.createProcessDefinitionQuery()
        .tenantIdIn(TENANT_TWO).processDefinitionKey("testProcess").latestVersion().singleResult();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("testProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).processDefinitionId(latestProcessTenantTwo.getId()).count()).isOne();
  }

  @Test
  void testStartProcessInstanceWithVersionBinding() {

    testRule.deployForTenant(TENANT_ONE, PROCESS, CMMN_VERSION);
    testRule.deployForTenant(TENANT_TWO, PROCESS, CMMN_VERSION);

    createCaseInstance("testCaseVersion", TENANT_ONE);
    createCaseInstance("testCaseVersion", TENANT_TWO);

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("testProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testFailStartProcessInstanceFromOtherTenantWithDeploymentBinding() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_DEPLOYMENT);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    // when/then
    assertThatThrownBy(() -> createCaseInstance("testCaseDeployment", TENANT_ONE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no processes deployed with key = 'testProcess'");
  }

  @Test
  void testFailStartProcessInstanceFromOtherTenantWithLatestBinding() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_LATEST);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    // when/then
    assertThatThrownBy(() -> createCaseInstance("testCase", TENANT_ONE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no processes deployed with key 'testProcess'");
  }

  @Test
  void testFailStartProcessInstanceFromOtherTenantWithVersionBinding() {
    // given
    testRule.deployForTenant(TENANT_ONE, PROCESS, CMMN_VERSION_2);

    testRule.deployForTenant(TENANT_TWO, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    // when/then
    assertThatThrownBy(() -> createCaseInstance("testCaseVersion", TENANT_ONE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no processes deployed with key = 'testProcess'");
  }

  @Test
  void testProcessRefTenantIdConstant() {
   testRule.deploy(CMMN_TENANT_CONST);
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    caseService.withCaseDefinitionByKey("testCase").create();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("testProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testProcessRefTenantIdExpression() {
   testRule.deploy(CMMN_TENANT_EXPR);
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    caseService.withCaseDefinitionByKey("testCase").create();

    CaseExecution caseExecution = caseService.createCaseExecutionQuery().activityId(PROCESS_TASK_ID).singleResult();
    caseService.withCaseExecution(caseExecution.getId()).manualStart();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("testProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  protected void createCaseInstance(String caseDefinitionKey, String tenantId) {
    caseService.withCaseDefinitionByKey(caseDefinitionKey).caseDefinitionTenantId(tenantId).create();

    CaseExecution caseExecution = caseService.createCaseExecutionQuery().activityId(PROCESS_TASK_ID).tenantIdIn(tenantId).singleResult();
    caseService.withCaseExecution(caseExecution.getId()).manualStart();
  }

}
