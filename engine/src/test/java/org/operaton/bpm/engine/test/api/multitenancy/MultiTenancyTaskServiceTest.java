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
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 *
 */
class MultiTenancyTaskServiceTest {

  private static final String TENANT_1 = "the-tenant-1";
  private static final String TENANT_2 = "the-tenant-2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected TaskService taskService;

  @Test
  void testStandaloneTaskCreateWithTenantId() {

    // given a transient task with tenant id
    Task task = taskService.newTask();
    task.setTenantId(TENANT_1);

    // if
    // it is saved
    taskService.saveTask(task);

    // then
    // when I load it, the tenant id is preserved
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getTenantId()).isEqualTo(TENANT_1);

    // Finally, delete task
    deleteTasks(task);
  }

  @Test
  void testStandaloneTaskCannotChangeTenantIdIfNull() {

    // given a persistent task without tenant id
    Task task = taskService.newTask();
    taskService.saveTask(task);
    Task savedTask = taskService.createTaskQuery().singleResult();

    // given
    savedTask.setTenantId(TENANT_1);

    // then
    // an exception is thrown on 'save'
    assertThatThrownBy(() -> taskService.saveTask(savedTask))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-03072 Cannot change tenantId of Task");

    // Finally, delete task
    deleteTasks(savedTask);
  }

  @Test
  void testStandaloneTaskCannotChangeTenantId() {

    // given a persistent task with tenant id
    Task task = taskService.newTask();
    task.setTenantId(TENANT_1);
    taskService.saveTask(task);
    Task savedTask = taskService.createTaskQuery().singleResult();

    // given
    savedTask.setTenantId(TENANT_2);

    // then
    // an exception is thrown on 'save'
    assertThatThrownBy(() -> taskService.saveTask(savedTask))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-03072 Cannot change tenantId of Task");

    // Finally, delete task
    deleteTasks(savedTask);
  }

  @Test
  void testStandaloneTaskCannotSetDifferentTenantIdOnSubTask() {

    // given a persistent task with a tenant id
    Task task = taskService.newTask();
    task.setTenantId(TENANT_1);
    taskService.saveTask(task);

    // given
    // I create a subtask with a different tenant id
    Task subTask = taskService.newTask();
    subTask.setParentTaskId(task.getId());
    subTask.setTenantId(TENANT_2);

    // then an exception is thrown on save
    assertThatThrownBy(() -> taskService.saveTask(subTask))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-03073 Cannot set different tenantId on subtask than on parent Task");

    // Finally, delete task
    deleteTasks(task);
  }

  @Test
  void testStandaloneTaskCannotSetDifferentTenantIdOnSubTaskWithNull() {

    // given a persistent task without tenant id
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // given
    // I create a subtask with a different tenant id
    Task subTask = taskService.newTask();
    subTask.setParentTaskId(task.getId());
    subTask.setTenantId(TENANT_1);

    // then an exception is thrown on save
    assertThatThrownBy(() -> taskService.saveTask(subTask))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-03073 Cannot set different tenantId on subtask than on parent Task");

    // Finally, delete task
    deleteTasks(task);
  }

  @Test
  void testStandaloneTaskPropagateTenantIdToSubTask() {

    // given a persistent task with a tenant id
    Task task = taskService.newTask();
    task.setTenantId(TENANT_1);
    taskService.saveTask(task);

    // if
    // I create a subtask without tenant id
    Task subTask = taskService.newTask();
    subTask.setParentTaskId(task.getId());
    taskService.saveTask(subTask);

    // then
    // the parent task's tenant id is propagated to the sub task
    subTask = taskService.createTaskQuery().taskId(subTask.getId()).singleResult();
    assertThat(subTask.getTenantId()).isEqualTo(TENANT_1);

    // Finally, delete task
    deleteTasks(subTask, task);
  }

  @Test
  void testStandaloneTaskPropagatesTenantIdToVariableInstance() {
    // given a task with tenant id
    Task task = taskService.newTask();
    task.setTenantId(TENANT_1);
    taskService.saveTask(task);

    // if we set a variable for the task
    taskService.setVariable(task.getId(), "var", "test");

    // then a variable instance with the same tenant id is created
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(variableInstance).isNotNull();
    assertThat(variableInstance.getTenantId()).isEqualTo(TENANT_1);

    deleteTasks(task);
  }

  @Test
  void testGetIdentityLinkWithTenantIdForCandidateUsers() {

    // given
    BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess("testProcess")
    .startEvent()
    .userTask("task").operatonCandidateUsers("aUserId")
    .endEvent()
    .done();

    testRule.deployForTenant("tenant", oneTaskProcess);

    ProcessInstance tenantProcessInstance = runtimeService.createProcessInstanceByKey("testProcess")
    .processDefinitionTenantId("tenant")
    .execute();

    Task tenantTask = taskService
        .createTaskQuery()
        .processInstanceId(tenantProcessInstance.getId())
        .singleResult();

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(tenantTask.getId());
    assertThat(identityLinks).hasSize(1);
    assertThat(identityLinks.get(0).getTenantId()).isEqualTo("tenant");
  }

  @Test
  void testGetIdentityLinkWithTenantIdForCandidateGroup() {

    // given
    BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess("testProcess")
    .startEvent()
    .userTask("task").operatonCandidateGroups("aGroupId")
    .endEvent()
    .done();

    testRule.deployForTenant("tenant", oneTaskProcess);

    ProcessInstance tenantProcessInstance = runtimeService.createProcessInstanceByKey("testProcess")
    .processDefinitionTenantId("tenant")
    .execute();

    Task tenantTask = taskService
        .createTaskQuery()
        .processInstanceId(tenantProcessInstance.getId())
        .singleResult();

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(tenantTask.getId());
    assertThat(identityLinks).hasSize(1);
    assertThat(identityLinks.get(0).getTenantId()).isEqualTo("tenant");
  }

  protected void deleteTasks(Task... tasks) {
    for(Task task : tasks) {
      taskService.deleteTask(task.getId(), true);
    }
  }

}
