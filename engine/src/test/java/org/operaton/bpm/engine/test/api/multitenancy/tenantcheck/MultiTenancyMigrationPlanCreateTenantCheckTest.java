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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Thorben Lindhauer
 *
 */
public class MultiTenancyMigrationPlanCreateTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testHelper);

  @Test
  public void canCreateMigrationPlanForDefinitionsOfAuthenticatedTenant() {
    // given
    ProcessDefinition tenant1Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenant2Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);


    // when
    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE));
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(tenant1Definition.getId(), tenant2Definition.getId())
      .mapEqualActivities()
      .build();

    // then
    assertThat(migrationPlan).isNotNull();
  }

  @Test
  public void cannotCreateMigrationPlanForDefinitionsOfNonAuthenticatedTenantsCase1() {
    // given
    ProcessDefinition tenant1Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenant2Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_TWO));
    var migrationPlanBuilder = engineRule.getRuntimeService()
      .createMigrationPlan(tenant1Definition.getId(), tenant2Definition.getId())
      .mapEqualActivities();

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get process definition '" + tenant1Definition.getId()
      + "' because it belongs to no authenticated tenant");
  }

  @Test
  public void cannotCreateMigrationPlanForDefinitionsOfNonAuthenticatedTenantsCase2() {
    // given
    ProcessDefinition tenant1Definition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenant2Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_TWO));
    var migrationInstructionsBuilder = engineRule.getRuntimeService()
      .createMigrationPlan(tenant1Definition.getId(), tenant2Definition.getId())
      .mapEqualActivities();

    // when/then
    assertThatThrownBy(migrationInstructionsBuilder::build)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get process definition '" + tenant2Definition.getId()
      + "' because it belongs to no authenticated tenant");
  }

  @Test
  public void cannotCreateMigrationPlanForDefinitionsOfNonAuthenticatedTenantsCase3() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    engineRule.getIdentityService().setAuthentication("user", null, null);
    var migrationInstructionsBuilder = engineRule.getRuntimeService()
      .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities();

    // when/then
    assertThatThrownBy(migrationInstructionsBuilder::build)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get process definition '" + sourceDefinition.getId()
      + "' because it belongs to no authenticated tenant");
  }


  @Test
  public void canCreateMigrationPlanForSharedDefinitionsWithNoAuthenticatedTenants() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    // when
    engineRule.getIdentityService().setAuthentication("user", null, null);
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities()
      .build();

    // then
    assertThat(migrationPlan).isNotNull();
  }


  @Test
  public void canCreateMigrationPlanWithDisabledTenantCheck() {

    // given
    ProcessDefinition tenant1Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenant2Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    // when
    engineRule.getIdentityService().setAuthentication("user", null, null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(tenant1Definition.getId(), tenant2Definition.getId())
      .mapEqualActivities()
      .build();

    // then
    assertThat(migrationPlan).isNotNull();

  }
}
