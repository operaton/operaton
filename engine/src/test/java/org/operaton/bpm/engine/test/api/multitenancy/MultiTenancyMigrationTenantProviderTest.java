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
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProvider;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderCaseInstanceContext;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderHistoricDecisionInstanceContext;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderProcessInstanceContext;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
@SuppressWarnings("java:S1874") // Use of synchronous execute() method is a acceptable in test code
class MultiTenancyMigrationTenantProviderTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(configuration -> configuration.setTenantIdProvider(new VariableBasedTenantIdProvider()))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  @Test
  void cannotMigrateInstanceBetweenDifferentTenants() {
    // given
    ProcessDefinition sharedDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition tenantDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_TWO, ProcessModels.ONE_TASK_PROCESS);

    ProcessInstance processInstance = startInstanceForTenant(sharedDefinition, TENANT_ONE);
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sharedDefinition.getId(), tenantDefinition.getId())
        .mapEqualActivities()
        .build();
    var migrationBuilder = engineRule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(List.of(processInstance.getId()));

    // when/then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot migrate process instance '%s' to a process definition of a different tenant ('tenant1' != 'tenant2')".formatted(processInstance.getId()));

    // then
    assertThat(migrationPlan).isNotNull();
  }

  @Test
  void canMigrateInstanceBetweenSameTenantCase2() {
    // given
    ProcessDefinition sharedDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployForTenantAndGetDefinition(TENANT_ONE, ProcessModels.ONE_TASK_PROCESS);

    ProcessInstance processInstance = startInstanceForTenant(sharedDefinition, TENANT_ONE);
    MigrationPlan migrationPlan = engineRule.getRuntimeService().createMigrationPlan(sharedDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    // when
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    assertInstanceOfDefinition(processInstance, targetDefinition);
  }

  @Test
  void canMigrateWithProcessInstanceQueryAllInstancesOfAuthenticatedTenant() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = engineRule
        .getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance1 = startInstanceForTenant(sourceDefinition, TENANT_ONE);
    ProcessInstance processInstance2 = startInstanceForTenant(sourceDefinition, TENANT_TWO);

    // when
    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE));
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceQuery(engineRule.getRuntimeService().createProcessInstanceQuery())
      .execute();
    engineRule.getIdentityService().clearAuthentication();

    // then
    assertInstanceOfDefinition(processInstance1, targetDefinition);
    assertInstanceOfDefinition(processInstance2, sourceDefinition);
  }

  @Test
  void canMigrateWithProcessInstanceQueryAllInstancesOfAuthenticatedTenants() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = engineRule
        .getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance1 = startInstanceForTenant(sourceDefinition, TENANT_ONE);
    ProcessInstance processInstance2 = startInstanceForTenant(sourceDefinition, TENANT_TWO);

    // when
    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));
    engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceQuery(engineRule.getRuntimeService().createProcessInstanceQuery())
      .execute();
    engineRule.getIdentityService().clearAuthentication();

    // then
    assertInstanceOfDefinition(processInstance1, targetDefinition);
    assertInstanceOfDefinition(processInstance2, targetDefinition);
  }

  protected void assertInstanceOfDefinition(ProcessInstance processInstance, ProcessDefinition targetDefinition) {
    assertThat(engineRule.getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstance.getId())
      .processDefinitionId(targetDefinition.getId())
      .count()).isOne();
  }

  protected ProcessInstance startInstanceForTenant(ProcessDefinition processDefinition, String tenantId) {
    return engineRule.getRuntimeService()
      .startProcessInstanceById(processDefinition.getId(),
          Variables.createVariables().putValue(VariableBasedTenantIdProvider.TENANT_VARIABLE, tenantId));
  }

  public static class VariableBasedTenantIdProvider implements TenantIdProvider {
    public static final String TENANT_VARIABLE = "tenantId";

    @Override
    public String provideTenantIdForProcessInstance(TenantIdProviderProcessInstanceContext ctx) {
      return (String) ctx.getVariables().get(TENANT_VARIABLE);
    }

    @Override
    public String provideTenantIdForCaseInstance(TenantIdProviderCaseInstanceContext ctx) {
      return (String) ctx.getVariables().get(TENANT_VARIABLE);
    }

    @Override
    public String provideTenantIdForHistoricDecisionInstance(TenantIdProviderHistoricDecisionInstanceContext ctx) {
      return null;
    }
  }
}
