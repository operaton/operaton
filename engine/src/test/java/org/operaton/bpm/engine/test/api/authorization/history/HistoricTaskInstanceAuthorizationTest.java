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

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE_HISTORY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.HISTORIC_PROCESS_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.HISTORIC_TASK;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.HistoricTaskPermissions;
import org.operaton.bpm.engine.authorization.MissingAuthorization;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.TaskPermissions;
import org.operaton.bpm.engine.history.DurationReportResult;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.history.HistoricTaskInstanceReport;
import org.operaton.bpm.engine.history.HistoricTaskInstanceReportResult;
import org.operaton.bpm.engine.query.PeriodUnit;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
class HistoricTaskInstanceAuthorizationTest extends AuthorizationTest {

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
        "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn").getId();
    super.setUp();

  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);
  }

  // historic task instance query (standalone task) ///////////////////////////////////////

  @Test
  void testQueryAfterStandaloneTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  // historic task instance query (process task) //////////////////////////////////////////

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void shouldNotFindTaskWithRevokedReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // historic task instance query (multiple process instances) ////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 7);
  }

  // historic task instance query (case task) ///////////////////////////////////////

  @Test
  void testQueryAfterCaseTask() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  // historic task instance query (mixed tasks) ////////////////////////////////////

  @Test
  void testMixedQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);

    createTask("one");
    createTask("two");
    createTask("three");
    createTask("four");
    createTask("five");

    testRule.createCaseInstanceByKey(CASE_KEY);
    testRule.createCaseInstanceByKey(CASE_KEY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

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
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);

    createTask("one");
    createTask("two");
    createTask("three");
    createTask("four");
    createTask("five");

    testRule.createCaseInstanceByKey(CASE_KEY);
    testRule.createCaseInstanceByKey(CASE_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

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
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);

    createTask("one");
    createTask("two");
    createTask("three");
    createTask("four");
    createTask("five");

    testRule.createCaseInstanceByKey(CASE_KEY);
    testRule.createCaseInstanceByKey(CASE_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 14);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  // delete deployment (cascade = false)

  @Test
  void testQueryAfterDeletingDeployment() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
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
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    verifyQueryResults(query, 3);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  // delete historic task (standalone task) ///////////////////////

  @Test
  void testDeleteStandaloneTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    disableAuthorization();
    HistoricTaskInstanceQuery query = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(taskId);
    verifyQueryResults(query, 0);
    enableAuthorization();

    deleteTask(taskId, true);
  }

  // delete historic task (process task) ///////////////////////

  @Test
  void testDeleteProcessTaskWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    assertThatThrownBy(() -> historyService.deleteHistoricTaskInstance(taskId))
      .withFailMessage("Exception expected: It should not be possible to delete the historic task instance")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(userId)
      .hasMessageContaining(DELETE_HISTORY.getName())
      .hasMessageContaining(PROCESS_KEY)
      .hasMessageContaining(PROCESS_DEFINITION.resourceName())
    ;
  }

  @Test
  void testDeleteProcessTaskWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, DELETE_HISTORY);

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    disableAuthorization();
    HistoricTaskInstanceQuery query = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(taskId);
    verifyQueryResults(query, 0);
    enableAuthorization();
  }

  @Test
  void testDeleteProcessTaskWithDeleteHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    disableAuthorization();
    HistoricTaskInstanceQuery query = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(taskId);
    verifyQueryResults(query, 0);
    enableAuthorization();
  }

  @Test
  void testDeleteHistoricTaskInstanceAfterDeletingDeployment() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    disableAuthorization();
    repositoryService.deleteDeployment(deploymentId);
    enableAuthorization();

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    disableAuthorization();
    HistoricTaskInstanceQuery query = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(taskId);
    verifyQueryResults(query, 0);
    enableAuthorization();

    disableAuthorization();
    historyService.deleteHistoricProcessInstance(processInstanceId);
    enableAuthorization();
  }

  @Test
  void testHistoricTaskInstanceDurationReportWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    HistoricTaskInstanceReport report = historyService.createHistoricTaskInstanceReport();

    assertThatThrownBy(() -> report.duration(PeriodUnit.MONTH))
        .withFailMessage("Exception expected: It should not be possible to create a historic task instance report")
        .isInstanceOf(AuthorizationException.class)
        .extracting("missingAuthorizations", as(list(MissingAuthorization.class)))
        .hasSize(1)
        .first()
        .satisfies(missingAuthorization -> {
          assertThat(missingAuthorization.getViolatedPermissionName()).isEqualTo(READ_HISTORY.toString());
          assertThat(missingAuthorization.getResourceType()).isEqualTo(PROCESS_DEFINITION.resourceName());
          assertThat(missingAuthorization.getResourceId()).isEqualTo(ANY);
        });
  }

  @Test
  void testHistoricTaskInstanceReportWithHistoryReadPermissionOnAny() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    List<DurationReportResult> result = historyService
            .createHistoricTaskInstanceReport()
            .duration(PeriodUnit.MONTH);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskInstanceCountByProcessDefinitionReportWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    HistoricTaskInstanceReport report = historyService.createHistoricTaskInstanceReport();

    assertThatThrownBy(report::countByProcessDefinitionKey)
        .withFailMessage("Exception expected: It should not be possible to create a historic task instance report")
        .isInstanceOf(AuthorizationException.class)
        .extracting("missingAuthorizations", as(list(MissingAuthorization.class)))
        .hasSize(1)
        .first()
        .satisfies(missingAuthorization -> {
          assertThat(missingAuthorization.getViolatedPermissionName()).isEqualTo(READ_HISTORY.toString());
          assertThat(missingAuthorization.getResourceType()).isEqualTo(PROCESS_DEFINITION.resourceName());
          assertThat(missingAuthorization.getResourceId()).isEqualTo(ANY);
        });
  }

  @Test
  void testHistoricTaskInstanceReportGroupedByProcessDefinitionKeyWithHistoryReadPermissionOnAny() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    List<HistoricTaskInstanceReportResult> result = historyService
            .createHistoricTaskInstanceReport()
            .countByProcessDefinitionKey();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskInstanceGroupedByTaskNameReportWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    HistoricTaskInstanceReport report = historyService.createHistoricTaskInstanceReport();

    assertThatThrownBy(report::countByTaskName)
        .withFailMessage("Exception expected: It should not be possible to create a historic task instance report")
        .isInstanceOf(AuthorizationException.class)
        .extracting("missingAuthorizations", as(list(MissingAuthorization.class)))
        .hasSize(1)
        .first()
        .satisfies(missingAuthorization -> {
          assertThat(missingAuthorization.getViolatedPermissionName()).isEqualTo(READ_HISTORY.toString());
          assertThat(missingAuthorization.getResourceType()).isEqualTo(PROCESS_DEFINITION.resourceName());
          assertThat(missingAuthorization.getResourceId()).isEqualTo(ANY);
        });
  }

  @Test
  void testHistoricTaskInstanceGroupedByTaskNameReportWithHistoryReadPermissionOnAny() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    List<HistoricTaskInstanceReportResult> result = historyService
            .createHistoricTaskInstanceReport()
            .countByTaskName();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckAllHistoricTaskPermissions() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    // when
    createGrantAuthorization(HISTORIC_TASK, ANY, userId, HistoricTaskPermissions.ALL);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricTaskPermissions.NONE, HISTORIC_TASK)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricTaskPermissions.READ, HISTORIC_TASK)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricTaskPermissions.READ_VARIABLE, HISTORIC_TASK)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricTaskPermissions.ALL, HISTORIC_TASK)).isTrue();
  }

  @Test
  void testCheckReadHistoricTaskPermissions() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    // when
    createGrantAuthorization(HISTORIC_TASK, ANY, userId, HistoricTaskPermissions.READ);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricTaskPermissions.NONE, HISTORIC_TASK)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricTaskPermissions.READ, HISTORIC_TASK)).isTrue();
  }

  @Test
  void testCheckNoneHistoricTaskPermission() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    // when
    createGrantAuthorization(HISTORIC_TASK, ANY, userId, HistoricTaskPermissions.NONE);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricTaskPermissions.NONE, HISTORIC_TASK)).isTrue();
  }

  @Test
  void testCheckNonePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadPermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String taskId = "aTaskId";
    createTask(taskId);

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

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

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

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
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckNonePermissionOndHistoricTaskAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadPermissionOndHistoricTaskAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskPermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);

    disableAuthorization();

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskReadPermissionGrantedWhenAssign() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    createGrantAuthorization(TASK, taskId, userId, TaskPermissions.TASK_ASSIGN);
    enableAuthorization();

    taskService.setAssignee(taskId, userId);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskReadPermissionGrantedWhenAddingIdentityLinkOnStandaloneTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String taskId = "aTaskId";
    createTask(taskId);

    disableAuthorization();
    createGrantAuthorization(TASK, taskId, userId, TaskPermissions.TASK_ASSIGN);
    enableAuthorization();

    taskService.setAssignee(taskId, userId);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);

    // clear
    deleteTask(taskId, true);
  }

  @Test
  void testHistoricTaskReadPermissionGrantedWhenSettingOwner() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    createGrantAuthorization(TASK, taskId, userId, TaskPermissions.TASK_ASSIGN);
    enableAuthorization();

    taskService.setOwner(taskId, userId);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskReadPermissionGrantedWhenSettingCandidateUser() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    createGrantAuthorization(TASK, taskId, userId, TaskPermissions.TASK_ASSIGN);
    enableAuthorization();

    taskService.addCandidateUser(taskId, userId);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskReadPermissionGrantedWhenSettingCandidateGroup() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    disableAuthorization();
    createGrantAuthorization(TASK, taskId, userId, TaskPermissions.TASK_ASSIGN);
    enableAuthorization();

    taskService.addCandidateGroup(taskId, groupId);

    // when
    List<HistoricTaskInstance> result = historyService.createHistoricTaskInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testStandaloneTaskClearHistoricAuthorization() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, ALL);
    createGrantAuthorization(HISTORIC_TASK, taskId, userId, ALL);

    disableAuthorization();
    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(HISTORIC_TASK)
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();
    assertThat(authorization).isNotNull();

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    disableAuthorization();
    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(HISTORIC_TASK)
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();

    // clear
    taskService.deleteTask(taskId);
  }

  @Test
  void testProcessTaskClearHistoricAuthorization() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, ALL);
    createGrantAuthorization(HISTORIC_TASK, taskId, userId, ALL);

    disableAuthorization();
    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(HISTORIC_TASK)
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();
    assertThat(authorization).isNotNull();

    taskService.complete(taskId);

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    disableAuthorization();
    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(HISTORIC_TASK)
        .resourceId(taskId)
        .singleResult();
    enableAuthorization();

    assertThat(authorization).isNull();

    // clear
    taskService.deleteTask(taskId);
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadPermissionOnCompletedHHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

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
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstanceAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

}
