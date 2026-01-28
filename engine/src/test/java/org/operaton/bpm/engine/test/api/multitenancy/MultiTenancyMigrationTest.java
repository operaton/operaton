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
package org.operaton.bpm.engine.test.api.multitenancy;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MultiTenancyMigrationTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  @Test
  void cannotCreateMigrationPlanBetweenDifferentTenants() {
    // given
    ProcessDefinition tenant1Definition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenant2Definition = testHelper.deployForTenantAndGetDefinition(TENANT_TWO, ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = engineRule.getRuntimeService().createMigrationPlan(tenant1Definition.getId(), tenant2Definition.getId())
      .mapEqualActivities();

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot migrate process instances between processes of different tenants ('tenant1' != 'tenant2')");
  }

  @Test
  void canCreateMigrationPlanFromTenantToNoTenant() {
    // given
    ProcessDefinition sharedDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenantDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);


    // when
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(tenantDefinition.getId(), sharedDefinition.getId())
      .mapEqualActivities()
      .build();

    // then
    assertThat(migrationPlan).isNotNull();
  }

  @Test
  void canCreateMigrationPlanFromNoTenantToTenant() {
    // given
    ProcessDefinition sharedDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenantDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);


    // when
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sharedDefinition.getId(), tenantDefinition.getId())
      .mapEqualActivities()
      .build();

    // then
    assertThat(migrationPlan).isNotNull();
  }

  @Test
  void canCreateMigrationPlanForNoTenants() {
    // given
    ProcessDefinition sharedDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);


    // when
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sharedDefinition.getId(), sharedDefinition.getId())
      .mapEqualActivities()
      .build();

    // then
    assertThat(migrationPlan).isNotNull();
  }

  @Test
  void canMigrateInstanceBetweenSameTenantCase1() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    // when
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    assertMigratedTo(processInstance, targetDefinition);
  }

  @Test
  void cannotMigrateInstanceWithoutTenantIdToDifferentTenant() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();
    var migrationBuilder = engineRule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(List.of(processInstance.getId()));

    // when/then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot migrate process instance '" + processInstance.getId()
              + "' without tenant to a process definition with a tenant ('tenant1')");
  }

  @Test
  void canMigrateInstanceWithTenantIdToDefinitionWithoutTenantId() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    // when
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    assertMigratedTo(processInstance, targetDefinition);
  }

  protected void assertMigratedTo(ProcessInstance processInstance, ProcessDefinition targetDefinition) {
    assertThat(engineRule.getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstance.getId())
      .processDefinitionId(targetDefinition.getId())
      .count()).isOne();
  }
}
