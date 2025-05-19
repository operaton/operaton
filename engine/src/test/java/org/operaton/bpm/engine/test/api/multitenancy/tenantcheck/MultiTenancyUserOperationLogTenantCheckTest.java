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
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_FULL;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@RequiredHistoryLevel(HISTORY_FULL)
class MultiTenancyUserOperationLogTenantCheckTest {

  protected static final String USER_ONE = "aUserId";
  protected static final String USER_TWO = "user_two";
  protected static final String USER_WITHOUT_TENANT = "aUserId1";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String PROCESS_NAME = "process";
  protected static final String AN_ANNOTATION = "anAnnotation";

  protected static final BpmnModelInstance MODEL = Bpmn.createExecutableProcess(PROCESS_NAME).startEvent().userTask("aTaskId").done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected TaskService taskService;
  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @Test
  void shouldSetAnnotationWithoutTenant() {
    // given
    testRule.deploy(MODEL);
    identityService.setAuthentication(USER_ONE, null);

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();

    // when
    historyService.setAnnotationForOperationLogById(singleResult.getOperationId(), AN_ANNOTATION);
    singleResult = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(singleResult.getTenantId()).isNull();
  }

  @Test
  void shouldSetAnnotationWithTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();

    // when
    historyService.setAnnotationForOperationLogById(singleResult.getOperationId(), AN_ANNOTATION);
    singleResult = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(singleResult.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldThrowExceptionWhenSetAnnotationWithDifferentTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    String operationId = singleResult.getOperationId();

    identityService.setAuthentication(USER_TWO, null, List.of(TENANT_TWO));

    // when/then
    assertThatThrownBy(() -> historyService.setAnnotationForOperationLogById(operationId, AN_ANNOTATION))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the user operation log entry '"
                            + singleResult.getId() +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void shouldThrowExceptionWhenSetAnnotationWithNoAuthenticatedTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    String operationId = singleResult.getOperationId();

    identityService.setAuthentication(USER_WITHOUT_TENANT, null);

    // when/then
    assertThatThrownBy(() -> historyService.setAnnotationForOperationLogById(operationId, AN_ANNOTATION))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the user operation log entry '"
                            + singleResult.getId() +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void shouldClearAnnotationWithoutTenant() {
    // given
    testRule.deploy(MODEL);
    identityService.setAuthentication(USER_ONE, null);

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    historyService.setAnnotationForOperationLogById(singleResult.getOperationId(), AN_ANNOTATION);

    // when
    historyService.clearAnnotationForOperationLogById(singleResult.getOperationId());
    singleResult = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(singleResult.getTenantId()).isNull();
  }

  @Test
  void shouldClearAnnotationWithTenant() {
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();

    historyService.setAnnotationForOperationLogById(singleResult.getOperationId(), AN_ANNOTATION);

    // when
    historyService.clearAnnotationForOperationLogById(singleResult.getOperationId());
    singleResult = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(singleResult.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldThrowExceptionWhenClearAnnotationWithDifferentTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    String operationId = singleResult.getOperationId();
    historyService.setAnnotationForOperationLogById(operationId, AN_ANNOTATION);

    identityService.setAuthentication(USER_TWO, null, List.of(TENANT_TWO));

    // when/then
    assertThatThrownBy(() ->  historyService.clearAnnotationForOperationLogById(operationId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the user operation log entry '"
                            + singleResult.getId() +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void shouldThrowExceptionWhenClearAnnotationWithNoAuthenticatedTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    String operationId = singleResult.getOperationId();
    historyService.setAnnotationForOperationLogById(operationId, AN_ANNOTATION);

    identityService.setAuthentication(USER_WITHOUT_TENANT, null);

    // when/then
    assertThatThrownBy(() ->  historyService.clearAnnotationForOperationLogById(operationId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the user operation log entry '"
                            + singleResult.getId() +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void shouldDeleteWithoutTenant() {
    // given
    testRule.deploy(MODEL);
    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    identityService.setAuthenticatedUserId("paul");

    taskService.complete(taskId);

    String entryId = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult().getId();

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    assertThat(singleResult).isNull();
  }

  @Test
  void shouldDeleteWithTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    String entryId = singleResult.getId();


    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    assertThat(singleResult).isNull();
  }

  @Test
  void shouldThrownExceptionWhenDeleteWithDifferentTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    String entryId = singleResult.getId();

    identityService.setAuthentication(USER_TWO, null, List.of(TENANT_TWO));

    // when/then
    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the user operation log entry '"
                            + entryId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void shouldThrownExceptionWhenDeleteWithNoAuthenticatedTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    String entryId = singleResult.getId();

    identityService.setAuthentication(USER_WITHOUT_TENANT, null);

    // when/then
    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the user operation log entry '"
                            + entryId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void shouldDeleteWhenTenantCheckDisabled() {
    // given
    testRule.deployForTenant(TENANT_ONE, MODEL);
    identityService.setAuthentication(USER_ONE, null, List.of(TENANT_ONE));

    runtimeService.startProcessInstanceByKey(PROCESS_NAME);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    UserOperationLogEntry singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    // assume
    assertThat(singleResult).isNotNull();
    String entryId = singleResult.getId();

    identityService.setAuthentication(USER_WITHOUT_TENANT, null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    singleResult = historyService.createUserOperationLogQuery().entityType(EntityTypes.TASK).singleResult();
    assertThat(singleResult).isNull();

    // finish
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(true);
  }
}
