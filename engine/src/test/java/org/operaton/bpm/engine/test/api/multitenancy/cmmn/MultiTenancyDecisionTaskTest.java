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
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class MultiTenancyDecisionTaskTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String CMMN_LATEST = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithDecisionTask.cmmn";
  protected static final String CMMN_DEPLOYMENT = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithDecisionTaskDeploymentBinding.cmmn";
  protected static final String CMMN_VERSION = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithDecisionTaskVersionBinding.cmmn";
  protected static final String CMMN_VERSION_2 = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithDecisionTaskVersionBinding_v2.cmmn";
  protected static final String CMMN_CONST = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithDecisionTaskTenantIdConst.cmmn";
  protected static final String CMMN_WITHOUT_TENANT = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithDecisionTaskWithoutTenantId.cmmn";
  protected static final String CMMN_EXPR = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithDecisionTaskTenantIdExpr.cmmn";

  protected static final String DMN_FILE = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";
  protected static final String DMN_FILE_VERSION_TWO = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable_v2.dmn";

  protected static final String CASE_DEFINITION_KEY = "caseDecisionTask";
  protected static final String DECISION_TASK_ID = "PI_DecisionTask_1";

  protected static final String RESULT_OF_VERSION_ONE = "A";
  protected static final String RESULT_OF_VERSION_TWO = "C";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected CaseService caseService;

  @Test
  void testEvaluateDecisionWithDeploymentBinding() {
    testRule.deployForTenant(TENANT_ONE, CMMN_DEPLOYMENT, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_DEPLOYMENT, DMN_FILE_VERSION_TWO);

    CaseInstance caseInstanceOne = createCaseInstance(CASE_DEFINITION_KEY, TENANT_ONE);
    CaseInstance caseInstanceTwo = createCaseInstance(CASE_DEFINITION_KEY, TENANT_TWO);

    assertThat((String)caseService.getVariable(caseInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)caseService.getVariable(caseInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testEvaluateDecisionWithLatestBindingSameVersion() {
    testRule.deployForTenant(TENANT_ONE, CMMN_LATEST, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_LATEST, DMN_FILE_VERSION_TWO);

    CaseInstance caseInstanceOne = createCaseInstance(CASE_DEFINITION_KEY, TENANT_ONE);
    CaseInstance caseInstanceTwo = createCaseInstance(CASE_DEFINITION_KEY, TENANT_TWO);

    assertThat((String)caseService.getVariable(caseInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)caseService.getVariable(caseInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testEvaluateDecisionWithLatestBindingDifferentVersions() {
    testRule.deployForTenant(TENANT_ONE, CMMN_LATEST, DMN_FILE);

    testRule.deployForTenant(TENANT_TWO, CMMN_LATEST, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, CMMN_LATEST, DMN_FILE_VERSION_TWO);

    CaseInstance caseInstanceOne = createCaseInstance(CASE_DEFINITION_KEY, TENANT_ONE);
    CaseInstance caseInstanceTwo = createCaseInstance(CASE_DEFINITION_KEY, TENANT_TWO);

    assertThat((String)caseService.getVariable(caseInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)caseService.getVariable(caseInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testEvaluateDecisionWithVersionBinding() {
    testRule.deployForTenant(TENANT_ONE, CMMN_VERSION, DMN_FILE);
    testRule.deployForTenant(TENANT_ONE, DMN_FILE_VERSION_TWO);

    testRule.deployForTenant(TENANT_TWO, CMMN_VERSION, DMN_FILE_VERSION_TWO);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);

    CaseInstance caseInstanceOne = createCaseInstance(CASE_DEFINITION_KEY, TENANT_ONE);
    CaseInstance caseInstanceTwo = createCaseInstance(CASE_DEFINITION_KEY, TENANT_TWO);

    assertThat((String)caseService.getVariable(caseInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)caseService.getVariable(caseInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testFailEvaluateDecisionFromOtherTenantWithDeploymentBinding() {
    testRule.deployForTenant(TENANT_ONE, CMMN_DEPLOYMENT);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);

    try {
      createCaseInstance(CASE_DEFINITION_KEY, TENANT_ONE);

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no decision definition deployed with key = 'decision'");
    }
  }

  @Test
  void testFailEvaluateDecisionFromOtherTenantWithLatestBinding() {
    testRule.deployForTenant(TENANT_ONE, CMMN_LATEST);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);

    try {
      createCaseInstance(CASE_DEFINITION_KEY, TENANT_ONE);

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no decision definition deployed with key 'decision'");
    }
  }

  @Test
  void testFailEvaluateDecisionFromOtherTenantWithVersionBinding() {
    testRule.deployForTenant(TENANT_ONE, CMMN_VERSION_2, DMN_FILE);

    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);

    try {
      createCaseInstance(CASE_DEFINITION_KEY, TENANT_ONE);

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no decision definition deployed with key = 'decision', version = '2' and tenant-id = 'tenant1'");
    }
  }

  @Test
  void testEvaluateDecisionRefTenantIdConstant() {
   testRule.deploy(CMMN_CONST);
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);

    CaseInstance caseInstance = createCaseInstance(CASE_DEFINITION_KEY);

    assertThat((String)caseService.getVariable(caseInstance.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
  }

  @Test
  void testEvaluateDecisionRefWithoutTenantIdConstant() {
    testRule.deployForTenant(TENANT_ONE, CMMN_WITHOUT_TENANT);
   testRule.deploy(DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);

    CaseInstance caseInstance = createCaseInstance(CASE_DEFINITION_KEY);

    assertThat((String)caseService.getVariable(caseInstance.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
  }

  @Test
  void testEvaluateDecisionRefTenantIdExpression() {
   testRule.deploy(CMMN_EXPR);
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);

    CaseInstance caseInstance = createCaseInstance(CASE_DEFINITION_KEY);

    assertThat((String)caseService.getVariable(caseInstance.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
  }

  protected CaseInstance createCaseInstance(String caseDefinitionKey, String tenantId) {
    CaseInstance caseInstance = caseService.withCaseDefinitionByKey(caseDefinitionKey).caseDefinitionTenantId(tenantId).create();

    CaseExecution caseExecution = caseService.createCaseExecutionQuery().activityId(DECISION_TASK_ID).tenantIdIn(tenantId).singleResult();
    caseService.withCaseExecution(caseExecution.getId()).setVariable("status", "gold").manualStart();
    return caseInstance;
  }

  protected CaseInstance createCaseInstance(String caseDefinitionKey) {
    CaseInstance caseInstance = caseService.withCaseDefinitionByKey(caseDefinitionKey).create();

    CaseExecution caseExecution = caseService.createCaseExecutionQuery().activityId(DECISION_TASK_ID).singleResult();
    caseService.withCaseExecution(caseExecution.getId()).setVariable("status", "gold").manualStart();
    return caseInstance;
  }

}
