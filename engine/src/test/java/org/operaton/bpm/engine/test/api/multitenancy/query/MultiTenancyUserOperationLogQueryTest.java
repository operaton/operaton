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
package org.operaton.bpm.engine.test.api.multitenancy.query;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyUserOperationLogQueryTest {
  protected static final String USER_ONE = "aUserId";
  protected static final String USER_TWO = "aUserId1";
  protected static final String USER_WITHOUT_TENANT = "aUserId1";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";
  protected static final String PROCESS_NAME = "process";

  protected static final BpmnModelInstance MODEL = Bpmn.createExecutableProcess(PROCESS_NAME)
      .startEvent().userTask("aTaskId").done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected TaskService taskService;
  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @Test
  void shouldReturnNoResultsWithoutTenant() {
    // given logs with assigned tenant
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    // when authenticated user without tenants
    identityService.setAuthentication(USER_WITHOUT_TENANT, null);
    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).count()).isZero();
    assertThat(singleResult).isNull();
  }

  @Test
  void shouldReturnResultsWhenMultipleTenants() {
    // given task with no tenant, with TENANT_ONE, and with TENANT_TWO
    testRule.deploy(MODEL);
    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    identityService.setAuthenticatedUserId(USER_WITHOUT_TENANT);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));
    runtimeService.createProcessInstanceByKey(PROCESS_NAME)
        .processDefinitionTenantId(TENANT_ONE)
        .execute();
    taskId = taskService.createTaskQuery().tenantIdIn(TENANT_ONE).singleResult().getId();
    taskService.complete(taskId);

    testRule.deployForTenant(TENANT_TWO, MODEL);
    identityService.setAuthentication(USER_TWO, null, List.of(TENANT_TWO));
    runtimeService.createProcessInstanceByKey(PROCESS_NAME)
        .processDefinitionTenantId(TENANT_TWO)
        .execute();
    taskId = taskService.createTaskQuery().tenantIdIn(TENANT_TWO).singleResult().getId();
    taskService.complete(taskId);

    // when query with USER_TWO
    List<UserOperationLogEntry> list = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .list();

    // then
    assertThat(historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).count()).isEqualTo(2);
    assertThat(list).hasSize(2);
    assertThat(list.get(0).getTenantId()).isIn(null, TENANT_TWO);
    assertThat(list.get(1).getTenantId()).isIn(null, TENANT_TWO);
  }

  @Test
  void shouldReturnResultsWhenTwoTenant() {
    // given logs with assigned tenant
    // and user belonging to two tenants
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE, TENANT_TWO));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    // when
    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).count()).isOne();
    assertThat(singleResult.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldReturnResultsWithoutTenantId() {
    // given task with no tenant, with TENANT_ONE, and with TENANT_TWO
    testRule.deploy(MODEL);
    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    identityService.setAuthenticatedUserId(USER_WITHOUT_TENANT);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));
    runtimeService.createProcessInstanceByKey(PROCESS_NAME)
        .processDefinitionTenantId(TENANT_ONE)
        .execute();
    taskId = taskService.createTaskQuery().tenantIdIn(TENANT_ONE).singleResult().getId();
    taskService.complete(taskId);

    testRule.deployForTenant(TENANT_TWO, MODEL);
    identityService.setAuthentication(USER_TWO, null, List.of(TENANT_TWO));
    runtimeService.createProcessInstanceByKey(PROCESS_NAME)
        .processDefinitionTenantId(TENANT_TWO)
        .execute();
    taskId = taskService.createTaskQuery().tenantIdIn(TENANT_TWO).singleResult().getId();
    taskService.complete(taskId);

    // when query without tenant id
    List<UserOperationLogEntry> list = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .withoutTenantId()
        .list();

    // then
    assertThat(historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).withoutTenantId().count()).isOne();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getTenantId()).isNull();
  }

  @Test
  void shouldReturnResultsTenantIdIn() {
    // given task with no tenant, with TENANT_ONE, and with TENANT_TWO
    testRule.deploy(MODEL);
    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    identityService.setAuthenticatedUserId(USER_WITHOUT_TENANT);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));
    runtimeService.createProcessInstanceByKey(PROCESS_NAME)
        .processDefinitionTenantId(TENANT_ONE)
        .execute();
    taskId = taskService.createTaskQuery().tenantIdIn(TENANT_ONE).singleResult().getId();
    taskService.complete(taskId);

    testRule.deployForTenant(TENANT_TWO, MODEL);
    identityService.setAuthentication(USER_TWO, null, List.of(TENANT_TWO));
    runtimeService.createProcessInstanceByKey(PROCESS_NAME)
        .processDefinitionTenantId(TENANT_TWO)
        .execute();
    taskId = taskService.createTaskQuery().tenantIdIn(TENANT_TWO).singleResult().getId();
    taskService.complete(taskId);

    // when query with USER_TWO
    List<UserOperationLogEntry> list = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .tenantIdIn(TENANT_TWO)
        .list();

    // then
    assertThat(historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getTenantId()).isEqualTo(TENANT_TWO);
  }
}
