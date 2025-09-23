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
package org.operaton.bpm.engine.test.api.authorization.history;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.HistoricTaskPermissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricDetailQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.HISTORIC_PROCESS_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.HISTORIC_TASK;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricDetailAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String MESSAGE_START_PROCESS_KEY = "messageStartProcess";
  protected static final String CASE_KEY = "oneTaskCase";

  protected String deploymentId;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/messageStartEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn")
            .getId();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);
    processEngineConfiguration.setEnforceSpecificVariablePermission(false);
  }

  // historic variable update query (standalone task) /////////////////////////////////////////////

  @Test
  void testQueryAfterStandaloneTaskVariableUpdates() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    taskService.setVariables(taskId, getVariables());
    enableAuthorization();

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  // historic variable update query (process task) /////////////////////////////////////////////

  @Test
  void testSimpleVariableUpdateQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleVariableUpdateQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleVariableUpdateQueryMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleVariableUpdateQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void shouldNotFindDetailWithRevokedReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 0);
  }

  // historic variable update query (multiple process instances) ///////////////////////////////////////////

  @Test
  void testVariableUpdateQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testVariableUpdateQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testVariableUpdateQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 7);
  }

  // historic variable update query (case variables) /////////////////////////////////////////////

  @Test
  void testQueryAfterCaseVariables() {
    // given
    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 1);
  }

  // historic variable update query (mixed) ////////////////////////////////////

  @Test
  void testMixedQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());

    createTask("one");
    createTask("two");
    createTask("three");
    createTask("four");
    createTask("five");

    disableAuthorization();
    taskService.setVariables("one", getVariables());
    taskService.setVariables("two", getVariables());
    taskService.setVariables("three", getVariables());
    taskService.setVariables("four", getVariables());
    taskService.setVariables("five", getVariables());
    enableAuthorization();

    createCaseInstanceByKey(CASE_KEY, getVariables());
    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 7);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  @Test
  void testMixedQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());

    createTask("one");
    createTask("two");
    createTask("three");
    createTask("four");
    createTask("five");

    disableAuthorization();
    taskService.setVariables("one", getVariables());
    taskService.setVariables("two", getVariables());
    taskService.setVariables("three", getVariables());
    taskService.setVariables("four", getVariables());
    taskService.setVariables("five", getVariables());
    enableAuthorization();

    createCaseInstanceByKey(CASE_KEY, getVariables());
    createCaseInstanceByKey(CASE_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 10);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  @Test
  void testMixedQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());

    createTask("one");
    createTask("two");
    createTask("three");
    createTask("four");
    createTask("five");

    disableAuthorization();
    taskService.setVariables("one", getVariables());
    taskService.setVariables("two", getVariables());
    taskService.setVariables("three", getVariables());
    taskService.setVariables("four", getVariables());
    taskService.setVariables("five", getVariables());
    enableAuthorization();

    createCaseInstanceByKey(CASE_KEY, getVariables());
    createCaseInstanceByKey(CASE_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().variableUpdates();

    // then
    verifyQueryResults(query, 14);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  // historic form field query //////////////////////////////////////////////////////

  @Test
  void testSimpleFormFieldQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().formFields();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleFormFieldQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().formFields();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleFormFieldQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().formFields();

    // then
    verifyQueryResults(query, 1);
  }

  // historic variable update query (multiple process instances) ///////////////////////////////////////////

  @Test
  void testFormFieldQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().formFields();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testFormFieldQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().formFields();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testFormFieldQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery().formFields();

    // then
    verifyQueryResults(query, 7);
  }

  // historic detail query (variable update + form field) //////////

  @Test
  void testDetailQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testDetailQueryWithReadHistoryOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    verifyQueryResults(query, 7);
  }

  @Test
  void testDetailQueryWithReadHistoryOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    verifyQueryResults(query, 7);
  }

  // delete deployment (cascade = false)

  @Test
  void testQueryAfterDeletingDeployment() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY);
    taskId = selectSingleTask().getId();
    disableAuthorization();
    formService.submitTaskForm(taskId, getVariables());
    enableAuthorization();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    disableAuthorization();
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    enableAuthorization();

    disableAuthorization();
    repositoryService.deleteDeployment(deploymentId);
    enableAuthorization();

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    verifyQueryResults(query, 7);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  @Test
  void testCheckNonePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadPermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String taskId = "aTaskId";
    createTask(taskId);
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).hasSize(1);

    // clear
    deleteTask(taskId, true);
  }

  @Test
  void testCheckNonePermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String taskId = "aTaskId";
    createTask(taskId);
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).isEmpty();

    // clear
    deleteTask(taskId, true);
  }

  @Test
  void testCheckReadPermissionOnCompletedHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckNonePermissionOnHistoricTaskAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadPermissionOnHistoricTaskAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadVariablePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testOnlyReadPermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testIgnoreReadVariablePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(false);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testCheckReadVariablePermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    String taskId = "aTaskId";
    createTask(taskId);
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).hasSize(1);

    // clear
    deleteTask(taskId, true);
  }

  @Test
  void testCheckReadVariablePermissionOnCompletedHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadVariablePermissionOnHistoricTaskAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckNonePermissionOnHistoricTaskAndReadHistoryVariablePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.READ_HISTORY_VARIABLE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadHistoryVariablePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.READ_HISTORY_VARIABLE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testOnlyReadPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.READ);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testIgnoreReadHistoryVariablePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnforceSpecificVariablePermission(false);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.READ_HISTORY_VARIABLE);

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery().list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testHistoricTaskPermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();

    taskService.setVariable(taskId, "foo", "bar");

    // when
    List<HistoricDetail> result = historyService.createHistoricDetailQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance_GlobalVariable() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    disableAuthorization();
    runtimeService.setVariable(processInstanceId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance_LocalVariable() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadPermissionOnCompletedHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckNoneOnHistoricProcessInstanceAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadOnHistoricProcessInstanceAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

}
