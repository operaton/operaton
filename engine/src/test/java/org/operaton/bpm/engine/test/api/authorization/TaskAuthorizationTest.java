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
package org.operaton.bpm.engine.test.api.authorization;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.TaskServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultAuthorizationProvider;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.*;
import static org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions.UPDATE_TASK_VARIABLE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.operaton.bpm.engine.authorization.TaskPermissions.UPDATE_VARIABLE;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Roman Smirnov
 *
 */
class TaskAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String CASE_KEY = "oneTaskCase";
  protected static final String DEMO_ASSIGNEE_PROCESS_KEY = "demoAssigneeProcess";
  protected static final String CANDIDATE_USERS_PROCESS_KEY = "candidateUsersProcess";
  protected static final String CANDIDATE_GROUPS_PROCESS_KEY = "candidateGroupsProcess";
  protected static final String INVALID_PERMISSION = "invalidPermission";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn",
        "org/operaton/bpm/engine/test/api/authorization/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/candidateUsersProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/candidateGroupsProcess.bpmn20.xml");
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

      List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().includeDeleted().list();
      for (HistoricVariableInstance variable : variables) {
        commandContext.getDbEntityManager().delete((HistoricVariableInstanceEntity) variable);
      }
      return null;
    });
  }

  public Permission getDefaultTaskPermissionForUser() {
    // get the default task assignee permission
    ProcessEngineConfigurationImpl processEngineConfiguration =
      (ProcessEngineConfigurationImpl) processEngine
        .getProcessEngineConfiguration();

    return processEngineConfiguration.getDefaultUserPermissionForTask();
  }


  // task query ///////////////////////////////////////////////////////

  @Test
  void testSimpleQueryWithTaskInsideProcessWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithTaskInsideProcessWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithTaskInsideProcessWithReadPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithTaskInsideProcessWithReadPermissionOnOneTaskProcess() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_TASK);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithTaskInsideProcessWithReadPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_TASK);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_TASK);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_TASK);
    createGrantAuthorization(TASK, ANY, userId, READ);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void shouldNotFindTaskWithRevokedReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, ALL);
    createGrantAuthorization(TASK, ANY, ANY, ALL);
    createRevokeAuthorization(TASK, taskId, userId, READ);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void shouldNotFindTaskWithRevokedReadTaskPermissionOnDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, ALL);
    createGrantAuthorization(TASK, ANY, ANY, ALL);
    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_TASK);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithTaskInsideProcessWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithTaskInsideProcessWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);

    disableAuthorization();
    String taskId = taskService.createTaskQuery().processDefinitionKey(PROCESS_KEY).listPage(0, 1).get(0).getId();
    enableAuthorization();

    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryWithTaskInsideProcessWithReadPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);

    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 7);
  }

  @Test
  void testQueryWithTaskInsideProcessWithReadPermissionOnOneTaskProcess() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_TASK);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryWithTaskInsideProcessWithReadPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);
    startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_TASK);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 7);
  }

  @Test
  void testQueryWithTaskInsideCaseWithoutAuthorization() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryWithStandaloneTaskWithoutAuthorization() {
    // given
    String taskId = "newTask";
    createTask(taskId);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  @Test
  void testQueryWithStandaloneTaskWithReadPermissionOnTask() {
    // given
    String taskId = "newTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  /**
   * CAM-12410 implements a single join for the process definition query filters
   * and the authorization check. This test assures that the query works when
   * both are used.
   */
  @Test
  void testQueryWithProcessDefinitionFilter() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    startProcessInstanceByKey(PROCESS_KEY);

    // when
    TaskQuery query = taskService.createTaskQuery().processDefinitionKey(PROCESS_KEY);

    // then
    verifyQueryResults(query, 1);
  }

  // new task /////////////////////////////////////////////////////////////

  @Test
  void testNewTaskWithoutAuthorization() {
    assertThatThrownBy(() -> taskService.newTask())
      .withFailMessage("Exception expected: It should not be possible to create a new task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have 'CREATE' permission on resource 'Task'");
  }

  @Test
  void testNewTask() {
    // given
    createGrantAuthorization(TASK, ANY, userId, CREATE);

    // when
    Task task = taskService.newTask();

    // then
    assertThat(task).isNotNull();
  }

  // save task (insert) //////////////////////////////////////////////////////////

  @Test
  void testSaveTaskInsertWithoutAuthorization() {
    // given
    TaskEntity task = new TaskEntity();

    // when
    assertThatThrownBy(() -> taskService.saveTask(task))
      // then
      .withFailMessage("Exception expected: It should not be possible to save a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have 'CREATE' permission on resource 'Task'");
  }

  @Test
  void testSaveTaskInsert() {
    // given
    TaskEntity task = new TaskEntity();
    task.setAssignee("demo");

    createGrantAuthorization(TASK, ANY, userId, CREATE);

    // when
    taskService.saveTask(task);

    // then
    task = (TaskEntity) selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    String taskId = task.getId();
    deleteTask(taskId, true);
  }

  @Test
  void testSaveAndUpdateTaskWithTaskAssignPermission() {
    // given
    TaskEntity task = new TaskEntity();
    task.setAssignee("demo");

    createGrantAuthorization(TASK, ANY, userId, CREATE, TASK_ASSIGN);

    // when
    taskService.saveTask(task);

    task.delegate("demoNew");

    taskService.saveTask(task);

    // then
    task = (TaskEntity) selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demoNew");

    String taskId = task.getId();
    deleteTask(taskId, true);
  }

  // save (standalone) task (update) //////////////////////////////////////////////////////////

  @Test
  void testSaveStandaloneTaskUpdateWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    Task task = selectSingleTask();

    // when
    assertThatThrownBy(() -> taskService.saveTask(task))
      // then
      .withFailMessage("Exception expected: It should not be possible to save a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN'");

    deleteTask(taskId, true);
  }

  @Test
  void testSaveStandaloneTaskUpdate() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    Task task = selectSingleTask();
    task.setAssignee("demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  // save (process) task (update) //////////////////////////////////////////////////////////

  @Test
  void testSaveProcessTaskUpdateWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();

    // when
    assertThatThrownBy(() -> taskService.saveTask(task))
      // then
      .withFailMessage("Exception expected: It should not be possible to save a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(task.getId())
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSaveProcessTaskUpdateWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    createGrantAuthorization(TASK, task.getId(), userId, UPDATE);

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testSaveProcessTaskUpdateWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    createGrantAuthorization(TASK, task.getId(), userId, TASK_ASSIGN);

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testSaveProcessTaskUpdateWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testSaveProcessTaskUpdateWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testSaveProcessTaskUpdateWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testSaveProcessTaskUpdateWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // save (case) task (update) //////////////////////////////////////////////////////////

  @Test
  void testSaveCaseTaskUpdate() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    // when
    taskService.saveTask(task);

    // then
    task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // delete task ///////////////////////////////////////////////////////////////////////

  @Test
  void testDeleteTaskWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.deleteTask(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have 'DELETE' permission on resource 'myTask' of type 'Task'");

    deleteTask(taskId, true);
  }

  @Test
  void testDeleteTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, DELETE);

    // when
    taskService.deleteTask(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();

    // triggers a db clean up
    deleteTask(taskId, true);
  }

  // delete tasks ///////////////////////////////////////////////////////////////////////

  @Test
  void testDeleteTasksWithoutAuthorization() {
    // given
    String firstTaskId = "myTask1";
    createTask(firstTaskId);
    String secondTaskId = "myTask2";
    createTask(secondTaskId);

    // when
    var taskIds = List.of(firstTaskId, secondTaskId);
    assertThatThrownBy(() -> taskService.deleteTasks(taskIds))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete tasks.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have 'DELETE' permission on resource 'myTask1' of type 'Task'");

    taskIds.forEach(taskId -> deleteTask(taskId, true));
  }

  @Test
  void testDeleteTasksWithDeletePermissionOnFirstTask() {
    // given
    String firstTaskId = "myTask1";
    createTask(firstTaskId);
    createGrantAuthorization(TASK, firstTaskId, userId, DELETE);

    String secondTaskId = "myTask2";
    createTask(secondTaskId);

    // when
    var taskIds = List.of(firstTaskId, secondTaskId);
    assertThatThrownBy(() -> taskService.deleteTasks(taskIds))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete tasks.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have 'DELETE' permission on resource 'myTask2' of type 'Task'");

    taskIds.forEach(taskId -> deleteTask(taskId, true));
  }

  @Test
  void testDeleteTasks() {
    // given
    String firstTaskId = "myTask1";
    createTask(firstTaskId);
    String secondTaskId = "myTask2";
    createTask(secondTaskId);

    createGrantAuthorization(TASK, ANY, userId, DELETE);

    // when
    taskService.deleteTasks(List.of(firstTaskId, secondTaskId));

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();

    // triggers a db clean up
    deleteTask(firstTaskId, true);
    deleteTask(secondTaskId, true);
  }

  // set assignee on standalone task /////////////////////////////////////////////

  @Test
  void testStandaloneTaskSetAssigneeWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.setAssignee(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to set an assignee")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetAssignee() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetAssigneeWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  // set assignee on process task /////////////////////////////////////////////

  @Test
  void testProcessTaskSetAssigneeWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.setAssignee(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to set an assignee")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskSetAssigneeWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetAssigneeWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetAssigneeWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetAssigneeWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetAssigneeWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetAssigneeWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetAssignee() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // set assignee on case task /////////////////////////////////////////////

  @Test
  void testCaseTaskSetAssignee() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // set owner on standalone task /////////////////////////////////////////////

  @Test
  void testStandaloneTaskSetOwnerWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.setOwner(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to set an owner")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetOwner() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetOwnerWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  // set owner on process task /////////////////////////////////////////////

  @Test
  void testProcessTaskSetOwnerWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.setOwner(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to set an owner")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskSetOwnerWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetOwnerWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetOwnerWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetOwnerWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetOwnerWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetOwnerWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetOwner() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskSetOwnerWithTaskAssignPermission() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  // set owner on case task /////////////////////////////////////////////

  @Test
  void testCaseTaskSetOwner() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.setOwner(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getOwner()).isEqualTo("demo");
  }

  // add candidate user ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskAddCandidateUserWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.addCandidateUser(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to add a candidate user")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddCandidateUser() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    deleteTask(taskId, true);
  }

  // add candidate user ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskAddCandidateUserWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.addCandidateUser(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to add a candidate user")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskAddCandidateUserWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateUserWithTaskAssignPermissionRevokeOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createRevokeAuthorization(TASK, taskId, userId, TASK_ASSIGN);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    assertThatThrownBy(() -> taskService.addCandidateUser(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to add an user identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");
  }

  @Test
  void testProcessTaskAddCandidateUserWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateUserWithGrantTaskAssignAndRevokeUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);
    createRevokeAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateUserWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateUserWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateUserWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateUserWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateUser() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // add candidate user ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskAddCandidateUser() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // add candidate group ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskAddCandidateGroupWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.addCandidateGroup(taskId, "accounting"))
      // then
      .withFailMessage("Exception expected: It should not be possible to add a candidate group")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddCandidateGroup() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddCandidateGroupWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    deleteTask(taskId, true);
  }

  // add candidate group ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskAddCandidateGroupWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.addCandidateGroup(taskId, "accounting"))
      // then
      .withFailMessage("Exception expected: It should not be possible to add a candidate group")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskAddCandidateGroupWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroupWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroupWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroupWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroupWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroupWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroup() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroupWithTaskAssignPermission() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddCandidateGroupWithTaskAssignPermissionRevoked() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);
    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // add candidate group ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskAddCandidateGroup() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.addCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // add user identity link ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskAddUserIdentityLinkWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to add an user identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddUserIdentityLink() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddUserIdentityLinkWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);


    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    deleteTask(taskId, true);
  }

  // add user identity link ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskAddUserIdentityLinkWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to add an user identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskAddUserIdentityLinkWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddUserIdentityLinkWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddUserIdentityLinkWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddUserIdentityLinkWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddUserIdentityLinkWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddUserIdentityLinkWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddUserIdentityLink() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // add user identity link ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskAddUserIdentityLink() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getUserId()).isEqualTo("demo");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // add group identity link ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskAddGroupIdentityLinkWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to add a group identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddGroupIdentityLink() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    deleteTask(taskId, true);
  }

  // add group identity link ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskAddGroupIdentityLinkWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to add a group identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskAddGroupIdentityLinkWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddGroupIdentityLinkWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddGroupIdentityLinkWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  void testProcessTaskAddGroupIdentityLink() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // add group identity link ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskAddGroupIdentityLink() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.addGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask)
            .isNotNull()
            .hasSize(1);

    IdentityLink identityLink = linksForTask.get(0);
    assertThat(identityLink).isNotNull();

    assertThat(identityLink.getGroupId()).isEqualTo("accounting");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  // delete candidate user ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskDeleteCandidateUserWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    // when
    assertThatThrownBy(() -> taskService.deleteCandidateUser(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a candidate user")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDeleteCandidateUser() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDeleteCandidateUserWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();

    deleteTask(taskId, true);
  }

  // delete candidate user ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskDeleteCandidateUserWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    // when
    assertThatThrownBy(() -> taskService.deleteCandidateUser(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a candidate user")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskDeleteCandidateUserWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateUserWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateUserWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateUserWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateUserWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateUserWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateUser() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // delete candidate user ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskDeleteCandidateUser() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // delete candidate group ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskDeleteCandidateGroupWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateGroup(taskId, "accounting");

    // when
    assertThatThrownBy(() -> taskService.deleteCandidateGroup(taskId, "accounting"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a candidate group")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDeleteCandidateGroup() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDeleteCandidateGroupWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();

    deleteTask(taskId, true);
  }

  // delete candidate group ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskDeleteCandidateGroupWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    // when
    assertThatThrownBy(() -> taskService.deleteCandidateGroup(taskId, "accounting"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a candidate group")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskDeleteCandidateGroupWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateGroupWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateGroupWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateGroupWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateGroupWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateGroupWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteCandidateGroup() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // delete candidate group ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskDeleteCandidateGroup() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    // when
    taskService.deleteCandidateGroup(taskId, "accounting");

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // delete user identity link ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskDeleteUserIdentityLinkWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    // when
    assertThatThrownBy(() -> taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete an user identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDeleteUserIdentityLink() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDeleteUserIdentityLinkWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();

    deleteTask(taskId, true);
  }

  // delete user identity link ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    // when
    assertThatThrownBy(() -> taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete an user identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteUserIdentityLink() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteUserIdentityLinkWithTaskAssignPermission() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // delete user identity link ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskDeleteUserIdentityLink() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // delete group identity link ((standalone) task) /////////////////////////////////////////////

  @Test
  void testStandaloneTaskDeleteGroupIdentityLinkWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateGroup(taskId, "accounting");

    // when
    assertThatThrownBy(() -> taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a group identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDeleteGroupIdentityLink() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();

    deleteTask(taskId, true);
  }

  // delete group identity link ((process) task) /////////////////////////////////////////////

  @Test
  void testProcessTaskDeleteGroupIdentityLinkWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    // when
    assertThatThrownBy(() -> taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a group identity link")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskDeleteGroupIdentityLinkWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteGroupIdentityLinkWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteGroupIdentityLinkWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  @Test
  void testProcessTaskDeleteGroupIdentityLink() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // delete group identity link ((case) task) /////////////////////////////////////////////

  @Test
  void testCaseTaskDeleteGroupIdentityLink() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateGroup(taskId, "accounting");

    // when
    taskService.deleteGroupIdentityLink(taskId, "accounting", IdentityLinkType.CANDIDATE);

    // then
    disableAuthorization();
    List<IdentityLink> linksForTask = taskService.getIdentityLinksForTask(taskId);
    enableAuthorization();

    assertThat(linksForTask).isNotNull().isEmpty();
  }

  // get identity links ((standalone) task) ////////////////////////////////////////////////

  @Test
  void testStandaloneTaskGetIdentityLinksWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    // when
    assertThatThrownBy(() -> taskService.getIdentityLinksForTask(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to get identity links")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have 'READ' permission on resource 'myTask' of type 'Task'");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskGetIdentityLinks() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(taskId);

    // then
    assertThat(identityLinksForTask)
            .isNotNull()
            .isNotEmpty();

    deleteTask(taskId, true);
  }

  // get identity links ((process) task) ////////////////////////////////////////////////

  @Test
  void testProcessTaskGetIdentityLinksWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    // when
    assertThatThrownBy(() -> taskService.getIdentityLinksForTask(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to get the identity links")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(READ.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(READ_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskGetIdentityLinksWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(taskId);

    // then
    assertThat(identityLinksForTask)
            .isNotNull()
            .isNotEmpty();
  }

  @Test
  void testProcessTaskGetIdentityLinksWithReadPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(taskId);

    // then
    assertThat(identityLinksForTask)
            .isNotNull()
            .isNotEmpty();
  }

  @Test
  void testProcessTaskGetIdentityLinksWithReadTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_TASK);

    // when
    List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(taskId);

    // then
    assertThat(identityLinksForTask)
            .isNotNull()
            .isNotEmpty();
  }

  @Test
  void testProcessTaskGetIdentityLinks() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_TASK);

    // when
    List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(taskId);

    // then
    assertThat(identityLinksForTask)
            .isNotNull()
            .isNotEmpty();
  }

  // get identity links ((case) task) ////////////////////////////////////////////////

  @Test
  void testCaseTaskGetIdentityLinks() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    addCandidateUser(taskId, "demo");

    // when
    List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(taskId);

    // then
    assertThat(identityLinksForTask)
            .isNotNull()
            .isNotEmpty();
  }

  // claim (standalone) task ////////////////////////////////////////////////////////////

  @Test
  void testStandaloneTaskClaimTaskWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.claim(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to claim the task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions:");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskClaimTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskClaimTaskWithTaskWorkPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_WORK);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskClaimTaskWithRevokeTaskWorkPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createRevokeAuthorization(TASK, taskId, userId, TASK_WORK);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    assertThatThrownBy(() -> taskService.claim(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to complete a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_WORK");

    deleteTask(taskId, true);
  }

  // claim (process) task ////////////////////////////////////////////////////////////

  @Test
  void testProcessTaskClaimTaskWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.claim(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to claim the task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskClaimTaskWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskClaimTaskWithTaskWorkPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_WORK);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskClaimTaskWithGrantTaskWorkAndRevokeUpdatePermissionsOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_WORK);
    createRevokeAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskClaimTaskWithRevokeTaskWorkPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createRevokeAuthorization(TASK, taskId, userId, TASK_WORK);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    assertThatThrownBy(() -> taskService.complete(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to complete a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_WORK");
  }

  @Test
  void testProcessTaskClaimTaskWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskClaimTaskWithTaskWorkPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, TASK_WORK);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskClaimTaskWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskClaimTaskWithTaskWorkPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_WORK);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

  }

  @Test
  void testProcessTaskClaimTaskWithRevokeTaskWorkPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_WORK);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    assertThatThrownBy(() -> taskService.complete(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to complete a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_WORK");

  }

  @Test
  void testProcessTaskClaimTask() {
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // claim (case) task ////////////////////////////////////////////////////////////

  @Test
  void testCaseTaskClaimTask() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.claim(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // complete (standalone) task ////////////////////////////////////////////////////////////

  @Test
  void testStandaloneTaskCompleteTaskWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.complete(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to complete a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_WORK");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskCompleteTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();

    if (!processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_NONE)) {
      historyService.deleteHistoricTaskInstance(taskId);
    }
  }

  @Test
  void testStandaloneTaskCompleteWithTaskWorkPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_WORK);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();

    if (!processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_NONE)) {
      historyService.deleteHistoricTaskInstance(taskId);
    }
  }

  // complete (process) task ////////////////////////////////////////////////////////////

  @Test
  void testProcessTaskCompleteTaskWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.complete(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to complete a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskCompleteTaskWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  @Test
  void testProcessTaskCompleteTaskWithTaskWorkPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_WORK);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  @Test
  void testProcessTaskCompleteTaskWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  @Test
  void testProcessTaskCompleteTaskWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  @Test
  void testProcessTaskCompleteTaskWithTaskWorkPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_WORK);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  @Test
  void testProcessTaskCompleteTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  // complete (case) task ////////////////////////////////////////////////////////////

  @Test
  void testCaseTaskCompleteTask() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.complete(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNull();
  }

  // delegate (standalone) task ///////////////////////////////////////////////////////

  @Test
  void testStandaloneTaskDelegateTaskWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.delegateTask(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delegate a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_ASSIGN");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDelegateTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskDelegateTaskWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");

    deleteTask(taskId, true);
  }

  // delegate (process) task ///////////////////////////////////////////////////////////

  @Test
  void testProcessTaskDelegateTaskWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.delegateTask(taskId, "demo"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delegate a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskDelegateTaskWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskDelegateTaskWithTaskAssignPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskDelegateTaskWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskDelegateTaskWithTaskAssignPermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, ANY, userId, TASK_ASSIGN);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskDelegateTaskWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskDelegateTaskWithTaskAssignPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskDelegateTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  @Test
  void testProcessTaskDelegateTaskWithTaskAssignPermission() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, TASK_ASSIGN);

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // delegate (case) task /////////////////////////////////////////////////////////////////

  @Test
  void testCaseTaskDelegateTask() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo("demo");
  }

  // resolve (standalone) task ///////////////////////////////////////////////////////

  @Test
  void testStandaloneTaskResolveTaskWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    assertThatThrownBy(() -> taskService.resolveTask(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to resolve a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'TASK_WORK");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskResolveTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setAssignee(taskId, userId);
    delegateTask(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.resolveTask(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo(userId);

    deleteTask(taskId, true);
  }

  // delegate (process) task ///////////////////////////////////////////////////////////

  @Test
  void testProcessTaskResolveTaskWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    // when
    assertThatThrownBy(() -> taskService.resolveTask(taskId))
      // then
      .withFailMessage("Exception expected: It should not be possible to resolve a task")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(UPDATE.getName())
      .hasMessageContaining(taskId)
      .hasMessageContaining(TASK.resourceName())
      .hasMessageContaining(UPDATE_TASK.getName())
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testProcessTaskResolveTaskWithUpdatePermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, userId);
    delegateTask(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.resolveTask(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo(userId);
  }

  @Test
  void testProcessTaskResolveTaskWithUpdatePermissionOnAnyTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, userId);
    delegateTask(taskId, "demo");

    createGrantAuthorization(TASK, ANY, userId, UPDATE);

    // when
    taskService.resolveTask(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo(userId);
  }

  @Test
  void testProcessTaskResolveTaskWithUpdateTasksPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, userId);
    delegateTask(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.resolveTask(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo(userId);
  }

  @Test
  void testProcessTaskResolveTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, userId);
    delegateTask(taskId, "demo");

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, UPDATE_TASK);

    // when
    taskService.resolveTask(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo(userId);
  }

  // delegate (case) task /////////////////////////////////////////////////////////////////

  @Test
  void testCaseTaskResolveTask() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, userId);
    delegateTask(taskId, "demo");

    // when
    taskService.resolveTask(taskId);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getAssignee()).isEqualTo(userId);
  }

  @Test
  void testCaseTaskSetPriority() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    // when
    taskService.setPriority(taskId, 80);

    // then
    Task task = selectSingleTask();
    assertThat(task).isNotNull();
    assertThat(task.getPriority()).isEqualTo(80);
  }

  // get sub tasks ((standalone) task) ////////////////////////////////////

  @Test
  void testStandaloneTaskGetSubTasksWithoutAuthorization() {
    // given
    String parentTaskId = "parentTaskId";
    createTask(parentTaskId);

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks).isEmpty();

    deleteTask(parentTaskId, true);
  }

  @Test
  void testStandaloneTaskGetSubTasksWithReadPermissionOnSub1() {
    // given
    String parentTaskId = "parentTaskId";
    createTask(parentTaskId);

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    createGrantAuthorization(TASK, "sub1", userId, READ);

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks)
            .isNotEmpty()
            .hasSize(1);

    assertThat(subTasks.get(0).getId()).isEqualTo("sub1");

    deleteTask(parentTaskId, true);
  }

  @Test
  void testStandaloneTaskGetSubTasks() {
    // given
    String parentTaskId = "parentTaskId";
    createTask(parentTaskId);

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks)
            .isNotEmpty()
            .hasSize(2);

    deleteTask(parentTaskId, true);
  }

  // get sub tasks ((process) task) ////////////////////////////////////

  @Test
  void testProcessTaskGetSubTasksWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String parentTaskId = selectSingleTask().getId();

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks).isEmpty();
  }

  @Test
  void testProcessTaskGetSubTasksWithReadPermissionOnSub1() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String parentTaskId = selectSingleTask().getId();

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    createGrantAuthorization(TASK, "sub1", userId, READ);

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks)
            .isNotEmpty()
            .hasSize(1);

    assertThat(subTasks.get(0).getId()).isEqualTo("sub1");
  }

  @Test
  void testProcessTaskGetSubTasks() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String parentTaskId = selectSingleTask().getId();

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks)
            .isNotEmpty()
            .hasSize(2);
  }

  // get sub tasks ((case) task) ////////////////////////////////////

  @Test
  void testCaseTaskGetSubTasksWithoutAuthorization() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String parentTaskId = selectSingleTask().getId();

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks).isEmpty();
  }

  @Test
  void testCaseTaskGetSubTasksWithReadPermissionOnSub1() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String parentTaskId = selectSingleTask().getId();

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    createGrantAuthorization(TASK, "sub1", userId, READ);

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks)
            .isNotEmpty()
            .hasSize(1);

    assertThat(subTasks.get(0).getId()).isEqualTo("sub1");
  }

  @Test
  void testCaseTaskGetSubTasks() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String parentTaskId = selectSingleTask().getId();

    disableAuthorization();
    Task sub1 = taskService.newTask("sub1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("sub2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);
    enableAuthorization();

    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    List<Task> subTasks = taskService.getSubTasks(parentTaskId);

    // then
    assertThat(subTasks)
            .isNotEmpty()
            .hasSize(2);
  }

  // clear authorization ((standalone) task) ////////////////////////

  @Test
  void testStandaloneTaskClearAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();
    assertThat(authorization).isNotNull();

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();
    authorization = authorizationService
        .createAuthorizationQuery()
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();

    deleteTask(taskId, true);
  }

  // clear authorization ((process) task) ////////////////////////

  @Test
  void testProcessTaskClearAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();
    assertThat(authorization).isNotNull();

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();
    authorization = authorizationService
        .createAuthorizationQuery()
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();
  }

  // set assignee -> an authorization is available (standalone task) /////////////////////////////////////////

  @Test
  void testStandaloneTaskSetAssigneeCreateNewAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetAssigneeUpdateAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetAssigneeToNullAuthorizationStillAvailable() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // set assignee to demo -> an authorization for demo is available
    taskService.setAssignee(taskId, "demo");

    // when
    taskService.setAssignee(taskId, null);

    // then
    // authorization for demo is still available
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testQueryStandaloneTaskSetAssignee() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // set assignee to demo -> an authorization for demo is available
    taskService.setAssignee(taskId, "demo");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetAssigneeOutsideCommandContextInsert() {
    // given
    String taskId = "myTask";
    createGrantAuthorization(TASK, ANY, userId, CREATE);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    Task task = taskService.newTask(taskId);
    task.setAssignee("demo");

    // when
    taskService.saveTask(task);

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetAssigneeOutsideCommandContextSave() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    Task task = selectSingleTask();

    task.setAssignee("demo");

    // when
    taskService.saveTask(task);

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  // set assignee -> an authorization is available (process task) /////////////////////////////////////////

  @Test
  void testProcessTaskSetAssigneeCreateNewAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testProcessTaskSetAssigneeUpdateAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testProcessTaskSetAssigneeToNullAuthorizationStillAvailable() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // set assignee to demo -> an authorization for demo is available
    taskService.setAssignee(taskId, "demo");

    // when
    taskService.setAssignee(taskId, null);

    // then
    // authorization for demo is still available
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testQueryProcessTaskSetAssignee() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // set assignee to demo -> an authorization for demo is available
    taskService.setAssignee(taskId, "demo");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
  }

  @Test
  void testProcessTaskAssignee() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, DEMO_ASSIGNEE_PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    runtimeService.startProcessInstanceByKey(DEMO_ASSIGNEE_PROCESS_KEY);

    // then
    // an authorization for demo has been created
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    // demo is able to retrieve the task
    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
  }

  // set assignee -> should not create an authorization (case task) /////////////////////////////////////////

  @Test
  void testCaseTaskSetAssigneeNoAuthorization() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();
  }

  // set owner -> an authorization is available (standalone task) /////////////////////////////////////////

  @Test
  void testStandaloneTaskSetOwnerCreateNewAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetOwnerUpdateAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testQueryStandaloneTaskSetOwner() {
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // set owner to demo -> an authorization for demo is available
    taskService.setOwner(taskId, "demo");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetOwnerOutsideCommandContextInsert() {
    // given
    String taskId = "myTask";
    createGrantAuthorization(TASK, ANY, userId, CREATE);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    Task task = taskService.newTask(taskId);
    task.setOwner("demo");

    // when
    taskService.saveTask(task);

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSetOwnerOutsideCommandContextSave() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    Task task = selectSingleTask();

    task.setOwner("demo");

    // when
    taskService.saveTask(task);

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  // set owner -> an authorization is available (process task) /////////////////////////////////////////

  @Test
  void testProcessTaskSetOwnerCreateNewAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testProcessTaskSetOwnerUpdateAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testQueryProcessTaskSetOwner() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // set owner to demo -> an authorization for demo is available
    taskService.setOwner(taskId, "demo");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
  }

  // set owner -> should not create an authorization  (case task) /////////////////////////////////

  @Test
  void testCaseTaskSetOwnerNoAuthorization() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.setOwner(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();
  }

  // add candidate user -> an authorization is available (standalone task) /////////////////

  @Test
  void testStandaloneTaskAddCandidateUserCreateNewAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddCandidateUserUpdateAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testQueryStandaloneTaskAddCandidateUser() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // add candidate user -> an authorization for demo is available
    taskService.addCandidateUser(taskId, "demo");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
    deleteTask(taskId, true);
  }

  @Test
  void testQueryStandaloneTaskAddCandidateUserWithTaskAssignPermission() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, TASK_ASSIGN);

    // add candidate user -> an authorization for demo is available
    taskService.addCandidateUser(taskId, "demo");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
    deleteTask(taskId, true);
  }

  // add candidate user -> an authorization is available (process task) ////////////////////

  @Test
  void testProcessTaskAddCandidateUserCreateNewAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testProcessTaskAddCandidateUserUpdateAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testQueryProcessTaskAddCandidateUser() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // add candidate user -> an authorization for demo is available
    taskService.addCandidateUser(taskId, "demo");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
  }

  @Test
  void testProcessTaskCandidateUsers() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, CANDIDATE_USERS_PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    runtimeService.startProcessInstanceByKey(CANDIDATE_USERS_PROCESS_KEY);

    // then
    // an authorization for demo has been created
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    // an authorization for test has been created
    disableAuthorization();
    authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("test")
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    // demo is able to retrieve the task
    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    // test is able to retrieve the task
    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);
  }

  // add candidate user -> should not create an authorization  (case task) /////////////////////////////////

  @Test
  void testCaseTaskAddCandidateUserNoAuthorization() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .userIdIn("demo")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();
  }

  // add candidate group -> an authorization is available (standalone task) /////////////////

  @Test
  void testStandaloneTaskAddCandidateGroupCreateNewAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateGroup(taskId, "management");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .groupIdIn("management")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskAddCandidateGroupUpdateAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.addCandidateGroup(taskId, "management");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .groupIdIn("management")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testQueryStandaloneTaskAddCandidateGroup() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // add candidate group -> an authorization for group management is available
    taskService.addCandidateGroup(taskId, "management");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", List.of("management"));

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
    deleteTask(taskId, true);
  }

  // add candidate group -> an authorization is available (process task) ////////////////////

  @Test
  void testProcessTaskAddCandidateGroupCreateNewAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateGroup(taskId, "management");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .groupIdIn("management")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testProcessTaskAddCandidateGroupUpdateAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);
    createGrantAuthorization(TASK, taskId, "demo", DELETE);

    // when
    taskService.addCandidateGroup(taskId, "management");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .groupIdIn("management")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();
  }

  @Test
  void testQueryProcessTaskAddCandidateGroup() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // add candidate group -> an authorization for group management is available
    taskService.addCandidateGroup(taskId, "management");

    identityService.clearAuthentication();
    identityService.setAuthentication("demo", List.of("management"));

    // when
    Task task = taskService.createTaskQuery().singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));
  }

  @Test
  void testProcessTaskCandidateGroups() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, CANDIDATE_GROUPS_PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    runtimeService.startProcessInstanceByKey(CANDIDATE_GROUPS_PROCESS_KEY);

    // then
    // an authorization for management has been created
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .groupIdIn("management")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    // an authorization for accounting has been created
    disableAuthorization();
    authorization = authorizationService
        .createAuthorizationQuery()
        .groupIdIn("accounting")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNotNull();
    assertThat(authorization.getResourceType()).isEqualTo(TASK.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo(taskId);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(getDefaultTaskPermissionForUser())).isTrue();

    // management is able to retrieve the task
    identityService.clearAuthentication();
    identityService.setAuthentication("demo", List.of("management"));

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);

    // accounting is able to retrieve the task
    identityService.clearAuthentication();
    identityService.setAuthentication(userId, List.of(groupId));

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getId()).isEqualTo(taskId);
  }

  // add candidate group -> should not create an authorization (case task) /////////////////////////////////

  @Test
  void testCaseTaskAddCandidateGroupNoAuthorization() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.addCandidateGroup(taskId, "management");

    // then
    disableAuthorization();
    Authorization authorization = authorizationService
        .createAuthorizationQuery()
        .groupIdIn("management")
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();
  }

  // TaskService#getVariable() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariable() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    // when
    Object variable = taskService.getVariable(taskId, VARIABLE_NAME);

    // then
    assertThat(variable).isEqualTo(VARIABLE_VALUE);
  }

  // TaskService#getVariableLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariableLocal() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariablesLocal(taskId, getVariables());
    enableAuthorization();

    // when
    Object variable = taskService.getVariableLocal(taskId, VARIABLE_NAME);

    // then
    assertThat(variable).isEqualTo(VARIABLE_VALUE);
  }

  // TaskService#getVariableTyped() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariableTyped() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    // when
    TypedValue typedValue = taskService.getVariableTyped(taskId, VARIABLE_NAME);

    // then
    assertThat(typedValue).isNotNull();
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE);
  }

  // TaskService#getVariableLocalTyped() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariableLocalTyped() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariablesLocal(taskId, getVariables());
    enableAuthorization();

    // when
    TypedValue typedValue = taskService.getVariableLocalTyped(taskId, VARIABLE_NAME);

    // then
    assertThat(typedValue).isNotNull();
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE);
  }

  // TaskService#getVariables() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariables() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    // when
    Map<String, Object> variables = taskService.getVariables(taskId);

    // then
verifyGetVariables(variables);  }

  // TaskService#getVariablesLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariablesLocal() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariablesLocal(taskId, getVariables());
    enableAuthorization();

    // when
    Map<String, Object> variables = taskService.getVariablesLocal(taskId);

    // then
verifyGetVariables(variables);  }

  // TaskService#getVariablesTyped() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariablesTyped() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    // when
    VariableMap variables = taskService.getVariablesTyped(taskId);

    // then
verifyGetVariables(variables);  }

  // TaskService#getVariablesLocalTyped() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariablesLocalTyped() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariablesLocal(taskId, getVariables());
    enableAuthorization();

    // when
    Map<String, Object> variables = taskService.getVariablesLocalTyped(taskId);

    // then
verifyGetVariables(variables);  }

  // TaskService#getVariables() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariablesByName() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    // when
    Map<String, Object> variables = taskService.getVariables(taskId, List.of(VARIABLE_NAME));

    // then
verifyGetVariables(variables);  }

  // TaskService#getVariablesLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariablesLocalByName() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariablesLocal(taskId, getVariables());
    enableAuthorization();

    // when
    Map<String, Object> variables = taskService.getVariablesLocal(taskId, List.of(VARIABLE_NAME));

    // then
verifyGetVariables(variables);  }

  // TaskService#getVariables() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariablesTypedByName() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    // when
    VariableMap variables = taskService.getVariablesTyped(taskId, List.of(VARIABLE_NAME), false);

    // then
verifyGetVariables(variables);  }

  // TaskService#getVariablesLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskGetVariablesLocalTypedByName() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariablesLocal(taskId, getVariables());
    enableAuthorization();

    // when
    Map<String, Object> variables = taskService.getVariablesLocalTyped(taskId, List.of(VARIABLE_NAME), false);

    // then
verifyGetVariables(variables);  }

  // TaskService#setVariable() (case task) /////////////////////////////////////

  @Test
  void testCaseTaskSetVariable() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    verifySetVariable(taskId);
  }

  // TaskService#setVariableLocal() (case task) /////////////////////////////////////

  @Test
  void testCaseTaskSetVariableLocal() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    verifySetVariableLocal(taskId);
  }

  // TaskService#setVariables() (case task) /////////////////////////////////////

  @Test
  void testCaseTaskSetVariables() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    verifySetVariables(taskId);
  }

  // TaskService#setVariablesLocal() (case task) /////////////////////////////////////

  @Test
  void testCaseTaskSetVariablesLocal() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    verifySetVariablesLocal(taskId);
  }

  // TaskService#removeVariable() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskRemoveVariable() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    verifyRemoveVariable(taskId);
  }

  // TaskService#removeVariableLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskRemoveVariableLocal() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    enableAuthorization();

    verifyRemoveVariableLocal(taskId);
  }

  // TaskService#removeVariables() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskRemoveVariables() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    verifyRemoveVariables(taskId);
  }

  // TaskService#removeVariablesLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskRemoveVariablesLocal() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.setVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    enableAuthorization();

    verifyRemoveVariablesLocal(taskId);
  }

  // TaskServiceImpl#updateVariablesLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskUpdateVariablesLocal() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    verifyUpdateVariablesLocal(taskId);
  }

  // TaskServiceImpl#updateVariablesLocal() (case task) ////////////////////////////////////////////

  @Test
  void testCaseTaskUpdateVariables() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();

    verifyUpdateVariables(taskId);
  }

  @Test
  void testStandaloneTaskSaveWithGenericResourceIdOwner() {
    createGrantAuthorization(TASK, ANY, userId, CREATE);

    Task task = taskService.newTask();
    task.setOwner("*");

    // when
    assertThatThrownBy(() -> taskService.saveTask(task))
      .withFailMessage("it should not be possible to save a task with the generic resource id *")
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create default authorization for owner *: id cannot be *. * is a reserved identifier");
  }

  @Test
  void testStandaloneTaskSaveWithGenericResourceIdOwnerTaskServiceApi() {
    createGrantAuthorization(TASK, ANY, userId, CREATE, UPDATE);

    Task task = taskService.newTask();
    taskService.saveTask(task);

    // when
    String taskId = task.getId();
    assertThatThrownBy(() -> taskService.setOwner(taskId, "*"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create default authorization for owner *: id cannot be *. * is a reserved identifier");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSaveWithGenericResourceIdAssignee() {
    createGrantAuthorization(TASK, ANY, userId, CREATE);

    Task task = taskService.newTask();
    task.setAssignee("*");

    // when
    assertThatThrownBy(() -> taskService.saveTask(task))
      // then
      .withFailMessage("it should not be possible to save a task with the generic resource id *")
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create default authorization for assignee *: id cannot be *. * is a reserved identifier");
  }

  @Test
  void testStandaloneTaskSaveWithGenericResourceIdAssigneeTaskServiceApi() {
    createGrantAuthorization(TASK, ANY, userId, CREATE, UPDATE);

    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    // when
    assertThatThrownBy(() -> taskService.setAssignee(taskId, "*"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create default authorization for assignee *: id cannot be *. * is a reserved identifier");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSaveIdentityLinkWithGenericUserId() {
    // given
    createGrantAuthorization(TASK, ANY, userId, CREATE, UPDATE);

    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    // when
    assertThatThrownBy(() -> taskService.addUserIdentityLink(taskId, "*", "someLink"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot grant default authorization for identity link to user *: id cannot be *. * is a reserved identifier.");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSaveIdentityLinkWithGenericGroupId() {
    // given
    createGrantAuthorization(TASK, ANY, userId, CREATE, UPDATE);

    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    // when
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "*", "someLink"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot grant default authorization for identity link to group *: id cannot be *. * is a reserved identifier.");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSaveIdentityLinkWithGenericGroupIdAndTaskAssignPermission() {
    // given
    createGrantAuthorization(TASK, ANY, userId, CREATE, TASK_ASSIGN);

    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    // when
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "*", "someLink"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot grant default authorization for identity link to group *: id cannot be *. * is a reserved identifier.");

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskSaveIdentityLinkWithGenericTaskId() {
    createGrantAuthorization(TASK, ANY, userId, CREATE, UPDATE);

    Task task = taskService.newTask();
    taskService.saveTask(task);

    // when
    assertThatThrownBy(() -> taskService.addUserIdentityLink("*", "aUserId", "someLink"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find task with id *");

    // when
    assertThatThrownBy(() -> taskService.addGroupIdentityLink("*", "aGroupId", "someLink"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find task with id *");

    deleteTask(task.getId(), true);
  }

  @Test
  void testStandaloneTaskSaveIdentityLinkWithGenericTaskIdAndTaskAssignPermission() {
    createGrantAuthorization(TASK, ANY, userId, CREATE, TASK_ASSIGN);

    Task task = taskService.newTask();
    taskService.saveTask(task);

    // when
    assertThatThrownBy(() -> taskService.addUserIdentityLink("*", "aUserId", "someLink"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find task with id *");

    // when
    assertThatThrownBy(() -> taskService.addGroupIdentityLink("*", "aGroupId", "someLink"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find task with id *");

    deleteTask(task.getId(), true);
  }

  @Deployment
  @Test
  void testSetGenericResourceIdAssignee() {
    // given
    createGrantAuthorization(Resources.PROCESS_DEFINITION, Authorization.ANY, userId, CREATE_INSTANCE);
    createGrantAuthorization(Resources.PROCESS_INSTANCE, Authorization.ANY, userId, CREATE);

    // when
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("genericResourceIdAssignmentProcess"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create default authorization for assignee *: id cannot be *. * is a reserved identifier.");
  }

  @Test
  void testAssignSameAssigneeAndOwnerToTask() {
    // given
    createGrantAuthorization(Resources.TASK, Authorization.ANY, userId, Permissions.ALL);

    // when
    Task newTask = taskService.newTask();
    newTask.setAssignee("Horst");
    newTask.setOwner("Horst");

    // when
    assertThatCode(() -> taskService.saveTask(newTask))
      // then
      .withFailMessage("Setting same assignee and owner to user should not fail!")
      .doesNotThrowAnyException();

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  void testPermissionsOnAssignSameAssigneeAndOwnerToTask() {

    try {
      // given
      createGrantAuthorization(Resources.TASK, Authorization.ANY, userId, Permissions.CREATE, Permissions.DELETE, Permissions.READ);
      processEngineConfiguration.setResourceAuthorizationProvider(new MyExtendedPermissionDefaultAuthorizationProvider());

      // when
      Task newTask = taskService.newTask();
      newTask.setAssignee("Horst");
      newTask.setOwner("Horst");
      taskService.saveTask(newTask);

      // then
      Authorization auth = authorizationService.createAuthorizationQuery().userIdIn("Horst").singleResult();
      assertThat(auth.isPermissionGranted(Permissions.DELETE)).isTrue();

      taskService.deleteTask(newTask.getId(), true);

    } finally {
      processEngineConfiguration.setResourceAuthorizationProvider(new DefaultAuthorizationProvider());
    }


  }

  @Deployment
  @Test
  void testAssignSameAssigneeAndOwnerToProcess() {
    //given
    createGrantAuthorization(Resources.PROCESS_DEFINITION, Authorization.ANY, userId, Permissions.ALL);
    createGrantAuthorization(Resources.PROCESS_INSTANCE, Authorization.ANY, userId, Permissions.ALL);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    List<Authorization> auths = authorizationService.createAuthorizationQuery().userIdIn("horst").list();
    assertThat(auths).hasSize(1);
  }

  @Deployment
  @Test
  void testAssignSameUserToProcessTwice() {
    //given
    createGrantAuthorization(Resources.PROCESS_DEFINITION, Authorization.ANY, userId, Permissions.ALL);
    createGrantAuthorization(Resources.PROCESS_INSTANCE, Authorization.ANY, userId, Permissions.ALL);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    List<Authorization> auths = authorizationService.createAuthorizationQuery().userIdIn("hans").list();
    assertThat(auths).hasSize(1);
  }

  @Deployment
  @Test
  void testAssignSameGroupToProcessTwice() {
    //given
    createGrantAuthorization(Resources.PROCESS_DEFINITION, Authorization.ANY, userId, Permissions.ALL);
    createGrantAuthorization(Resources.PROCESS_INSTANCE, Authorization.ANY, userId, Permissions.ALL);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    List<Authorization> auths = authorizationService.createAuthorizationQuery().groupIdIn("abc").list();
    assertThat(auths).hasSize(1);
  }


  // helper ////////////////////////////////////////////////////////////////////////////////

  protected void verifyMessageIsValid(String taskId, String message) {
    testRule.assertTextPresent(userId, message);
    testRule.assertTextPresent(UPDATE.getName(), message);
    testRule.assertTextPresent(UPDATE_VARIABLE.getName(), message);
    testRule.assertTextPresent(taskId, message);
    testRule.assertTextPresent(TASK.resourceName(), message);
    testRule.assertTextPresent(UPDATE_TASK.getName(), message);
    testRule.assertTextPresent(UPDATE_TASK_VARIABLE.getName(), message);
    testRule.assertTextPresent(PROCESS_KEY, message);
    testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
  }

  protected void verifyVariableInstanceCountDisabledAuthorization(int count) {
    disableAuthorization();
    verifyQueryResults(runtimeService.createVariableInstanceQuery(), count);
    enableAuthorization();
  }

  protected void verifySetVariable(String taskId) {
    // when
    taskService.setVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // then
    verifyVariableInstanceCountDisabledAuthorization(1);
  }

  protected void verifySetVariableLocal(String taskId) {
    // when
    taskService.setVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // then
    verifyVariableInstanceCountDisabledAuthorization(1);
  }

  protected void verifySetVariables(String taskId) {
    // when
    taskService.setVariables(taskId, getVariables());

    // then
    verifyVariableInstanceCountDisabledAuthorization(1);
  }

  protected void verifySetVariablesLocal(String taskId) {
    // when
    taskService.setVariablesLocal(taskId, getVariables());

    // then
    verifyVariableInstanceCountDisabledAuthorization(1);
  }

  protected void verifyRemoveVariable(String taskId) {
    // when
    taskService.removeVariable(taskId, VARIABLE_NAME);

    verifyVariableInstanceCountDisabledAuthorization(0);
  }

  protected void verifyRemoveVariableLocal(String taskId) {
    // when
    taskService.removeVariableLocal(taskId, VARIABLE_NAME);

    verifyVariableInstanceCountDisabledAuthorization(0);
  }

  protected void verifyRemoveVariables(String taskId) {
    // when
    taskService.removeVariables(taskId, List.of(VARIABLE_NAME));

    verifyVariableInstanceCountDisabledAuthorization(0);
  }

  protected void verifyRemoveVariablesLocal(String taskId) {
    // when
    taskService.removeVariablesLocal(taskId, List.of(VARIABLE_NAME));

    // then
    verifyVariableInstanceCountDisabledAuthorization(0);
  }

  protected void verifyUpdateVariables(String taskId) {
    // when (1)
    ((TaskServiceImpl) taskService).updateVariables(taskId, getVariables(), null);

    // then (1)
    verifyVariableInstanceCountDisabledAuthorization(1);

    // when (2)
    ((TaskServiceImpl) taskService).updateVariables(taskId, null, List.of(VARIABLE_NAME));

    // then (2)
    verifyVariableInstanceCountDisabledAuthorization(0);

    // when (3)
    ((TaskServiceImpl) taskService).updateVariables(taskId, getVariables(), List.of(VARIABLE_NAME));

    // then (3)
    verifyVariableInstanceCountDisabledAuthorization(0);
  }

  protected void verifyUpdateVariablesLocal(String taskId) {
    // when (1)
    ((TaskServiceImpl) taskService).updateVariablesLocal(taskId, getVariables(), null);

    // then (1)
    verifyVariableInstanceCountDisabledAuthorization(1);

    // when (2)
    ((TaskServiceImpl) taskService).updateVariablesLocal(taskId, null, List.of(VARIABLE_NAME));

    // then (2)
    verifyVariableInstanceCountDisabledAuthorization(0);

    // when (3)
    ((TaskServiceImpl) taskService).updateVariablesLocal(taskId, getVariables(), List.of(VARIABLE_NAME));

    // then (3)
    verifyVariableInstanceCountDisabledAuthorization(0);
  }

  protected void verifyGetVariables(Map<String, Object> variables) {
    assertThat(variables)
      .isNotNull()
      .isNotEmpty()
      .hasSize(1)
      .containsEntry(VARIABLE_NAME, VARIABLE_VALUE);
  }

}
