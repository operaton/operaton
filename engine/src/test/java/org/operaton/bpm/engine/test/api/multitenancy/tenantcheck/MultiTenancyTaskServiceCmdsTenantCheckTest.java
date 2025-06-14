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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

class MultiTenancyTaskServiceCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  protected static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent()
    .userTask()
    .endEvent()
    .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected TaskService taskService;
  protected IdentityService identityService;

  protected Task task;

  @BeforeEach
  void init() {

    testRule.deployForTenant(TENANT_ONE, ONE_TASK_PROCESS);

    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    task = engineRule.getTaskService().createTaskQuery().singleResult();
  }

  // save test cases
  @Test
  void saveTaskWithAuthenticatedTenant() {

    task = taskService.newTask("newTask");
    task.setTenantId(TENANT_ONE);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.saveTask(task);
    // then
    assertThat(taskService.createTaskQuery().taskId(task.getId()).count()).isEqualTo(1L);

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  void saveTaskWithNoAuthenticatedTenant() {

    task = taskService.newTask("newTask");
    task.setTenantId(TENANT_ONE);

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.saveTask(task))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create the task '"
          + task.getId() +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void saveTaskWithDisabledTenantCheck() {

    task = taskService.newTask("newTask");
    task.setTenantId(TENANT_ONE);

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    taskService.saveTask(task);
    // then
    assertThat(taskService.createTaskQuery().taskId(task.getId()).count()).isEqualTo(1L);
    taskService.deleteTask(task.getId(), true);
  }

  // update task test
  @Test
  void updateTaskWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    task.setAssignee("aUser");
    taskService.saveTask(task);

    // then
    assertThat(taskService.createTaskQuery().taskAssignee("aUser").count()).isEqualTo(1L);
  }

  @Test
  void updateTaskWithNoAuthenticatedTenant() {

    task.setAssignee("aUser");
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.saveTask(task))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + task.getId() +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void updateTaskWithDisabledTenantCheck() {

    task.setAssignee("aUser");
    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    taskService.saveTask(task);
    assertThat(taskService.createTaskQuery().taskAssignee("aUser").count()).isEqualTo(1L);

  }

  // claim task test
  @Test
  void claimTaskWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    taskService.claim(task.getId(), "bUser");
    assertThat(taskService.createTaskQuery().taskAssignee("bUser").count()).isEqualTo(1L);
  }

  @Test
  void claimTaskWithNoAuthenticatedTenant() {
    // given
    String taskId = task.getId();
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.claim(taskId, "bUser"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot work on task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void claimTaskWithDisableTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    taskService.claim(task.getId(), "bUser");
    assertThat(taskService.createTaskQuery().taskAssignee("bUser").count()).isEqualTo(1L);

  }

  // complete the task test
  @Test
  void completeTaskWithAuthenticatedTenant() {
    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().taskId(task.getId()).active().count()).isZero();
  }

  @Test
  void completeTaskWithNoAuthenticatedTenant() {
    // given
    String taskId = task.getId();
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.complete(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot work on task '"
          + taskId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void completeWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().taskId(task.getId()).active().count()).isZero();
  }

  // delegate task test
  @Test
  void delegateTaskWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.delegateTask(task.getId(), "demo");

    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isEqualTo(1L);
  }

  @Test
  void delegateTaskWithNoAuthenticatedTenant() {
    // given
    String taskId = task.getId();
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.delegateTask(taskId, "demo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void delegateTaskWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    taskService.delegateTask(task.getId(), "demo");
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isEqualTo(1L);
  }

  // resolve task test
  @Test
  void resolveTaskWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.resolveTask(task.getId());

    assertThat(taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED).taskId(task.getId()).count()).isEqualTo(1L);
  }

  @Test
  void resolveTaskWithNoAuthenticatedTenant() {
    // given
    String taskId = task.getId();
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.resolveTask(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot work on task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void resolveTaskWithDisableTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    taskService.resolveTask(task.getId());
    assertThat(taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED).taskId(task.getId()).count()).isEqualTo(1L);
  }

  // delete task test
  @Test
  void deleteTaskWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    task = createTaskForTenant();
    assertThat(taskService.createTaskQuery().taskId(task.getId()).count()).isEqualTo(1L);

    // then
    taskService.deleteTask(task.getId(), true);
    assertThat(taskService.createTaskQuery().taskId(task.getId()).count()).isZero();
  }

  @Test
  void deleteTaskWithNoAuthenticatedTenant() {

    try {
      task = createTaskForTenant();
      String taskId = task.getId();
      identityService.setAuthentication("aUserId", null);

      // when/then
      assertThatThrownBy(() -> taskService.deleteTask(taskId, true))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Cannot delete the task '"
            + taskId +"' because it belongs to no authenticated tenant.");

    } finally {
      identityService.clearAuthentication();
      taskService.deleteTask(task.getId(), true);
    }



  }

  @Test
  void deleteTaskWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    task = createTaskForTenant();
    assertThat(taskService.createTaskQuery().taskId(task.getId()).count()).isEqualTo(1L);

    // then
    taskService.deleteTask(task.getId(), true);
    assertThat(taskService.createTaskQuery().taskId(task.getId()).count()).isZero();
  }

  protected Task createTaskForTenant() {
    Task newTask = taskService.newTask("newTask");
    newTask.setTenantId(TENANT_ONE);
    taskService.saveTask(newTask);

    return newTask;

  }
}
