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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
class MultiTenancyMigrationExecuteTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  @Test
  void canMigrateWithAuthenticatedTenant() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = engineRule
        .getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    // when
    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE));
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Collections.singletonList(processInstance.getId()))
      .execute();

    // then
    assertMigratedTo(processInstance, targetDefinition);

  }

  @Test
  void cannotMigrateOfNonAuthenticatedTenant() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = engineRule
        .getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_TWO));
    var migrationPlanExecutionBuilder = engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Collections.singletonList(processInstance.getId()));

    // when/then
    assertThatThrownBy(migrationPlanExecutionBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot migrate process instance '" + processInstance.getId()
      + "' because it belongs to no authenticated tenant");

  }

  @Test
  void cannotMigrateWithNoAuthenticatedTenant() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = engineRule
        .getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    engineRule.getIdentityService().setAuthentication("user", null, null);
    var migrationPlanExecutionBuilder = engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Collections.singletonList(processInstance.getId()));

    // when/then
    assertThatThrownBy(migrationPlanExecutionBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot migrate process instance '" + processInstance.getId()
      + "' because it belongs to no authenticated tenant");
  }

  @Test
  void canMigrateSharedInstanceWithNoTenant() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = engineRule
        .getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    // when
    engineRule.getIdentityService().setAuthentication("user", null, null);
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Collections.singletonList(processInstance.getId()))
      .execute();

    // then
    assertMigratedTo(processInstance, targetDefinition);

  }

  @Test
  void canMigrateInstanceWithTenantCheckDisabled() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = engineRule
        .getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    // when
    engineRule.getIdentityService().setAuthentication("user", null, null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Collections.singletonList(processInstance.getId()))
      .execute();

    // then
    assertMigratedTo(processInstance, targetDefinition);

  }

  protected void assertMigratedTo(ProcessInstance processInstance, ProcessDefinition targetDefinition) {
    assertThat(engineRule.getRuntimeService()
        .createProcessInstanceQuery()
        .processInstanceId(processInstance.getId())
        .processDefinitionId(targetDefinition.getId())
        .count()).isEqualTo(1);
  }
}
