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
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyCaseTaskTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String CMMN_LATEST = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithCaseTask.cmmn";
  protected static final String CMMN_LATEST_WITH_MANUAL_ACTIVATION = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithCaseTaskWithManualActivation.cmmn";
  protected static final String CMMN_DEPLOYMENT = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithCaseTaskDeploymentBinding.cmmn";
  protected static final String CMMN_VERSION = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithCaseTaskVersionBinding.cmmn";
  protected static final String CMMN_VERSION_2 = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithCaseTaskVersionBinding_v2.cmmn";

  protected static final String CMMN_TENANT_CONST = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithCaseTaskTenantIdConst.cmmn";
  protected static final String CMMN_TENANT_EXPR = "org/operaton/bpm/engine/test/api/multitenancy/CaseWithCaseTaskTenantIdExpr.cmmn";

  protected static final String CMMN_CASE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

  protected static final String CASE_TASK_ID = "PI_CaseTask_1";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected CaseService caseService;
  protected RepositoryService repositoryService;

  @Test
  void testStartCaseInstanceWithDeploymentBinding() {

    testRule.deployForTenant(TENANT_ONE, CMMN_DEPLOYMENT, CMMN_CASE);
    testRule.deployForTenant(TENANT_TWO, CMMN_DEPLOYMENT, CMMN_CASE);

    createCaseInstance("caseTaskCaseDeployment", TENANT_ONE);
    createCaseInstance("caseTaskCaseDeployment", TENANT_TWO);

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartCaseInstanceWithLatestBindingSameVersion() {

    testRule.deployForTenant(TENANT_ONE, CMMN_LATEST_WITH_MANUAL_ACTIVATION, CMMN_CASE);
    testRule.deployForTenant(TENANT_TWO, CMMN_LATEST_WITH_MANUAL_ACTIVATION, CMMN_CASE);

    createCaseInstance("caseTaskCase", TENANT_ONE);
    createCaseInstance("caseTaskCase", TENANT_TWO);

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartCaseInstanceWithLatestBindingDifferentVersion() {

    testRule.deployForTenant(TENANT_ONE, CMMN_LATEST_WITH_MANUAL_ACTIVATION, CMMN_CASE);

    testRule.deployForTenant(TENANT_TWO, CMMN_LATEST_WITH_MANUAL_ACTIVATION, CMMN_CASE);
    testRule.deployForTenant(TENANT_TWO, CMMN_CASE);

    createCaseInstance("caseTaskCase", TENANT_ONE);
    createCaseInstance("caseTaskCase", TENANT_TWO);

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();

    CaseDefinition latestCaseDefinitionTenantTwo = repositoryService.createCaseDefinitionQuery().
        caseDefinitionKey("oneTaskCase").tenantIdIn(TENANT_TWO).latestVersion().singleResult();
    query = caseService.createCaseInstanceQuery().caseDefinitionId(latestCaseDefinitionTenantTwo.getId());
    assertThat(query.count()).isOne();
  }

  @Test
  void testStartCaseInstanceWithVersionBinding() {

    testRule.deployForTenant(TENANT_ONE, CMMN_VERSION, CMMN_CASE);
    testRule.deployForTenant(TENANT_TWO, CMMN_VERSION, CMMN_CASE);

    createCaseInstance("caseTaskCaseVersion", TENANT_ONE);
    createCaseInstance("caseTaskCaseVersion", TENANT_TWO);

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testFailStartCaseInstanceFromOtherTenantWithDeploymentBinding() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_DEPLOYMENT);
    testRule.deployForTenant(TENANT_TWO, CMMN_CASE);

    // when/then
    assertThatThrownBy(() -> createCaseInstance("caseTaskCaseDeployment", TENANT_ONE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no case definition deployed with key = 'oneTaskCase'");
  }

  @Test
  void testFailStartCaseInstanceFromOtherTenantWithLatestBinding() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_LATEST);
    testRule.deployForTenant(TENANT_TWO, CMMN_CASE);

    // when/then
    assertThatThrownBy(() -> createCaseInstance("caseTaskCase", TENANT_ONE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no case definition deployed with key 'oneTaskCase'");
  }

  @Test
  void testFailStartCaseInstanceFromOtherTenantWithVersionBinding() {
    // given
    testRule.deployForTenant(TENANT_ONE, CMMN_VERSION_2, CMMN_CASE);

    testRule.deployForTenant(TENANT_TWO, CMMN_CASE);
    testRule.deployForTenant(TENANT_TWO, CMMN_CASE);

    // when/then
    assertThatThrownBy(() -> createCaseInstance("caseTaskCaseVersion", TENANT_ONE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no case definition deployed with key = 'oneTaskCase'");
  }

  @Test
  void testCaseRefTenantIdConstant() {
   testRule.deploy(CMMN_TENANT_CONST);
    testRule.deployForTenant(TENANT_ONE, CMMN_CASE);

    caseService.withCaseDefinitionByKey("caseTaskCase").create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testCaseRefTenantIdExpression() {
   testRule.deploy(CMMN_TENANT_EXPR);
    testRule.deployForTenant(TENANT_ONE, CMMN_CASE);

    caseService.withCaseDefinitionByKey("caseTaskCase").create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  protected void createCaseInstance(String caseDefinitionKey, String tenantId) {
    caseService.withCaseDefinitionByKey(caseDefinitionKey).caseDefinitionTenantId(tenantId).create();

    CaseExecution caseExecution = caseService.createCaseExecutionQuery().activityId(CASE_TASK_ID).tenantIdIn(tenantId).singleResult();
    caseService.withCaseExecution(caseExecution.getId()).manualStart();
  }

}
