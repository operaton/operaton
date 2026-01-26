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

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.*;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE_HISTORY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions.UPDATE_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.HISTORIC_TASK;
import static org.operaton.bpm.engine.authorization.Resources.OPERATION_LOG_CATEGORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.UserOperationLogCategoryPermissions.DELETE;
import static org.operaton.bpm.engine.authorization.UserOperationLogCategoryPermissions.READ;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_ADMIN;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_OPERATOR;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_TASK_WORKER;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class UserOperationLogAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";
  protected static final String ONE_TASK_CASE_KEY = "oneTaskCase";
  protected static final String TIMER_BOUNDARY_PROCESS_KEY = "timerBoundaryProcess";

  protected String deploymentId;
  protected String testTaskId;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn",
        "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml")
            .getId();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);

    if (testTaskId != null) {
      deleteTask(testTaskId, true);
      testTaskId = null;

    }
  }

  // standalone task ///////////////////////////////

  @Test
  void testQueryCreateStandaloneTaskUserOperationLogWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  @Test
  void testQueryCreateStandaloneTaskUserOperationLogWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  // CAM-9888
  public void failing_testQueryCreateStandaloneTaskUserOperationLogWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  @Test
  void testQueryCreateStandaloneTaskUserOperationLogWithReadPermissionOnCategory() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  @Test
  void testQueryCreateStandaloneTaskUserOperationLogWithReadPermissionOnAnyCategory() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  @Test
  void testQueryCreateStandaloneTaskUserOperationLogWithReadPermissionOnAnyCategoryAndRevokeReadHistoryOnProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);
    createRevokeAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 1);// "revoke specific process definition" has no effect since task log is not related to a definition

    deleteTask(taskId, true);
  }

  // CAM-9888
  public void failing_testQueryCreateStandaloneTaskUserOperationLogWithReadPermissionOnAnyCategoryAndRevokeReadHistoryOnAnyProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);
    createRevokeAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    // "grant all categories" should precede over "revoke all process definitions"
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  @Test
  void testQuerySetAssigneeStandaloneTaskUserOperationLogWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setAssignee(taskId, "demo");

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  @Test
  void testQuerySetAssigneeStandaloneTaskUserOperationLogWithReadPermissionOnProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  // CAM-9888
  public void failing_testQuerySetAssigneeStandaloneTaskUserOperationLogWithReadPermissionOnAnyProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  @Test
  void testQuerySetAssigneeStandaloneTaskUserOperationLogWithReadPermissionOnCategory() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    deleteTask(taskId, true);
  }

  @Test
  void testQuerySetAssigneeStandaloneTaskUserOperationLogWithReadPermissionOnAnyCategory() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    deleteTask(taskId, true);
  }

  // (process) user task /////////////////////////////

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithMultiple() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY)
        .getProcessInstanceId();

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY)
        .getProcessInstanceId();

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId);
  }

  @Test
  void testCheckNoneOnHistoricProcessInstanceAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY)
        .getProcessInstanceId();

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId,
        ProcessDefinitionPermissions.READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId);
  }

  @Test
  void testCheckReadOnHistoricProcessInstanceAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY)
        .getProcessInstanceId();

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId);
  }

  @Test
  void testCheckNoneOnHistoricProcessInstanceAndTaskWorkerCategory() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY)
        .getProcessInstanceId();

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_PROCESS_INSTANCE,
        processInstanceId, userId, HistoricProcessInstancePermissions.NONE);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER,
        userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadOnHistoricProcessInstanceAndAdminCategory() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY)
        .getProcessInstanceId();

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_ADMIN, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId);
  }

  @Test
  void testHistoricProcessInstancePermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY)
        .getProcessInstanceId();

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId);
  }

  @Test
  void testCheckNonePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(HISTORIC_TASK, taskId, userId,
        HistoricTaskPermissions.NONE);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(HISTORIC_TASK, taskId, userId,
        HistoricTaskPermissions.READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactly(taskId);
  }

  @Test
  void testCheckReadPermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testTaskId = "aTaskId";
    createTask(testTaskId);

    disableAuthorization();
    taskService.setAssignee(testTaskId, userId);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(HISTORIC_TASK, testTaskId, userId,
        HistoricTaskPermissions.READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactly(testTaskId, testTaskId);
  }

  @Test
  void testCheckNonePermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testTaskId = "aTaskId";
    createTask(testTaskId);
    disableAuthorization();
    taskService.setAssignee(testTaskId, userId);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(HISTORIC_TASK, testTaskId, userId,
        HistoricTaskPermissions.NONE);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnCompletedHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setAssignee(taskId, userId);
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(HISTORIC_TASK, taskId, userId,
        HistoricTaskPermissions.READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactly(taskId, taskId);
  }

  @Test
  void testCheckNonePermissionOnHistoricTaskAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(HISTORIC_TASK, taskId, userId,
        HistoricTaskPermissions.NONE);
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY,
        userId, ProcessDefinitionPermissions.READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactlyInAnyOrder(taskId, null);
  }

  @Test
  void testCheckReadPermissionOnHistoricTaskAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(HISTORIC_TASK, taskId, userId,
        HistoricTaskPermissions.READ);
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactly(taskId);
  }

  @Test
  void testCheckNoneOnHistoricTaskAndTaskWorkerCategory() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_TASK,
        taskId, userId, HistoricTaskPermissions.NONE);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER,
        userId, UserOperationLogCategoryPermissions.READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactly(taskId);
  }

  @Test
  void testCheckReadOnHistoricTaskAndAdminCategory() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(Resources.HISTORIC_TASK, taskId, userId,
        HistoricTaskPermissions.READ);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY,
        CATEGORY_ADMIN, userId, UserOperationLogCategoryPermissions.READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactly(taskId);
  }

  @Test
  void testHistoricTaskPermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    disableAuthorization();

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    assertThat(query.list())
        .extracting("taskId")
        .containsExactlyInAnyOrder(taskId, null);
  }


  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithReadPermissionOnCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithReadPermissionOnAnyCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithReadPermissionOnAnyCategoryAndRevokeOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);
    createRevokeAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);// "revoke process definition" wins over "grant all categories" since task log is related to the definition
  }

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithReadPermissionOnAnyCategoryAndRevokeOnUnrelatedProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);
    createRevokeAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_CASE_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);// "revoke process definition" has no effect since task log is not related to the definition
  }

  @Test
  void testQuerySetAssigneeTaskUserOperationLogWithReadPermissionOnAnyCategoryAndRevokeOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);
    createRevokeAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);// "revoke all process definitions" wins over "grant all categories"
  }

  // (case) human task /////////////////////////////

  @Test
  void testQuerySetAssigneeHumanTaskUserOperationLogWithoutAuthorization() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQuerySetAssigneeHumanTaskUserOperationLogWithReadHistoryPermissionOnProcessDefinition() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_CASE_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // CAM-9888
  public void failing_testQuerySetAssigneeHumanTaskUserOperationLogWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQuerySetAssigneeHumanTaskUserOperationLogWithReadPermissionOnCategory() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQuerySetAssigneeHumanTaskUserOperationLogWithReadPermissionOnAnyCategory() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  // standalone job ///////////////////////////////

  @Test
  void testQuerySetStandaloneJobRetriesUserOperationLogWithoutAuthorization() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, new Date());
    enableAuthorization();

    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();

    clearDatabase();
  }

  @Test
  void testQuerySetStandaloneJobRetriesUserOperationLogWithReadHistoryPermissionOnProcessDefinition() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, new Date());
    enableAuthorization();

    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then only user operation logs of non-standalone jobs are visible
    verifyQueryResults(query, 2);
    assertThat(query.list().get(0).getProcessDefinitionKey()).isEqualTo(ONE_TASK_PROCESS_KEY);
    assertThat(query.list().get(1).getProcessDefinitionKey()).isEqualTo(ONE_TASK_PROCESS_KEY);

    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();

    clearDatabase();
  }

  @Test
  void testQuerySetStandaloneJobRetriesUserOperationLogWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    disableAuthorization();
    identityService.clearAuthentication();
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, new Date());
    enableAuthorization();

    disableAuthorization();
    identityService.setAuthentication(userId, null);
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then only non-standalone jobs entries
    verifyQueryResults(query, 1);

    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();

    clearDatabase();
  }

  @Test
  void testQuerySetStandaloneJobRetriesUserOperationLogWithReadPermissionOnCategory() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, new Date());
    enableAuthorization();

    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_OPERATOR, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then expect 3 entries (due to necessary permission on 'Operator' category, the definition
    // suspension, as well as the flag for related instances suspension can be seen as well)
    verifyQueryResults(query, 3);

    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();

    clearDatabase();
  }

  @Test
  void testQuerySetStandaloneJobRetriesUserOperationLogWithReadPermissionOnAnyCategory() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, new Date());
    enableAuthorization();

    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 3);

    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();

    clearDatabase();
  }

  @Test
  void testQuerySetStandaloneJobRetriesUserOperationLogWithReadPermissionOnWrongCategory() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, new Date());
    enableAuthorization();

    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();

    clearDatabase();
  }

  // job ///////////////////////////////

  @Test
  void testQuerySetJobRetriesUserOperationLogWithoutAuthorization() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    String jobId = selectSingleJob().getId();

    disableAuthorization();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQuerySetJobRetriesUserOperationLogWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    String jobId = selectSingleJob().getId();

    disableAuthorization();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQuerySetJobRetriesUserOperationLogWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    String jobId = selectSingleJob().getId();

    disableAuthorization();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQuerySetJobRetriesUserOperationLogWithReadPermissionOnCategory() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    String jobId = selectSingleJob().getId();

    disableAuthorization();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_OPERATOR, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQuerySetJobRetriesUserOperationLogWithReadPermissionOnAnyCategory() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    String jobId = selectSingleJob().getId();

    disableAuthorization();
    managementService.setJobRetries(jobId, 5);
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);
  }

  // process definition ////////////////////////////////////////////

  @Test
  void testQuerySuspendProcessDefinitionUserOperationLogWithoutAuthorization() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessDefinitionUserOperationLogWithReadHistoryPermissionOnProcessDefinition() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessDefinitionUserOperationLogWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessDefinitionUserOperationLogWithReadHPermissionOnCategory() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_OPERATOR, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessDefinitionUserOperationLogWithReadHPermissionOnAnyCategory() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  // process instance //////////////////////////////////////////////

  @Test
  void testQuerySuspendProcessInstanceUserOperationLogWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessInstanceUserOperationLogWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessInstanceUserOperationLogWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessInstanceUserOperationLogWithReadPermissionOnCategory() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_OPERATOR, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  @Test
  void testQuerySuspendProcessInstanceUserOperationLogWithReadPermissionOnAnyCategory() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 2);

    clearDatabase();
  }

  // delete deployment (cascade = false)

  @Test
  void testQueryAfterDeletingDeploymentWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    deleteDeployment(deploymentId, false);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 0);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  @Test
  void testQueryAfterDeletingDeploymentWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY);

    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    deleteDeployment(deploymentId, false);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 3);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  @Test
  void testQueryAfterDeletingDeploymentWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    deleteDeployment(deploymentId, false);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 3);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  @Test
  void testQueryAfterDeletingDeploymentWithReadPermissionOnCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    deleteDeployment(deploymentId, false);

    // when
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_OPERATOR, userId, READ);
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then expect 1 entry (start process instance)
    verifyQueryResults(query, 1);

    // and when
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, READ);

    // then expect 3 entries (start process instance, set assignee, complete task)
    verifyQueryResults(query, 3);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  @Test
  void testQueryAfterDeletingDeploymentWithReadPermissionOnAnyCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, READ);

    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    deleteDeployment(deploymentId, false);

    // when
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();

    // then
    verifyQueryResults(query, 3);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  // delete user operation log (standalone) ////////////////////////

  @Test
  void testDeleteStandaloneEntryWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    // when
    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId),
        "It should not be possible to delete the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);

    deleteTask(taskId, true);
  }

  @Test
  void testDeleteStandaloneEntryWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, DELETE_HISTORY);

    assertThatThrownBy(
        () -> historyService.deleteUserOperationLogEntry(entryId),
        "It should not be possible to delete the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);

    deleteTask(taskId, true);
  }

  @Test
  void testDeleteStandaloneEntryWithDeleteHistoryPermissionOnAnyProcessDefinition() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    assertThatThrownBy(
        () -> historyService.deleteUserOperationLogEntry(entryId),
        "It should not be possible to delete the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);

    deleteTask(taskId, true);
  }

  @Test
  void testDeleteStandaloneEntryWithDeletePermissionOnCategory() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, DELETE);

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    assertThat(historyService.createUserOperationLogQuery().singleResult()).isNull();

    deleteTask(taskId, true);
  }

  @Test
  void testDeleteStandaloneEntryWithDeletePermissionOnAnyCategory() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, DELETE);

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    assertThat(historyService.createUserOperationLogQuery().singleResult()).isNull();

    deleteTask(taskId, true);
  }

  // delete user operation log /////////////////////////////////////

  @Test
  void testDeleteEntryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().entityType("Task").singleResult().getId();
    enableAuthorization();

    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId),
        "It should not be possible to delete the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE_HISTORY.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName())
        .hasMessageContaining(DELETE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);
  }

  @Test
  void testDeleteEntryWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, DELETE_HISTORY);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().entityType("Task").singleResult().getId();
    enableAuthorization();

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    disableAuthorization();
    assertThat(historyService.createUserOperationLogQuery().entityType("Task").singleResult()).isNull();
    enableAuthorization();
  }

  @Test
  void testDeleteEntryWithDeleteHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().entityType("Task").singleResult().getId();
    enableAuthorization();

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    disableAuthorization();
    assertThat(historyService.createUserOperationLogQuery().entityType("Task").singleResult()).isNull();
    enableAuthorization();
  }

  @Test
  void testDeleteEntryWithDeletePermissionOnCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    createGrantAuthorization(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, DELETE);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().entityType("Task").singleResult().getId();
    enableAuthorization();

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    disableAuthorization();
    assertThat(historyService.createUserOperationLogQuery().entityType("Task").singleResult()).isNull();
    enableAuthorization();
  }

  @Test
  void testDeleteEntryWithDeletePermissionOnAnyCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");
    createGrantAuthorization(OPERATION_LOG_CATEGORY, ANY, userId, DELETE);

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().entityType("Task").singleResult().getId();
    enableAuthorization();

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    disableAuthorization();
    assertThat(historyService.createUserOperationLogQuery().entityType("Task").singleResult()).isNull();
    enableAuthorization();
  }

  @Test
  void testDeleteEntryAfterDeletingDeployment() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_HISTORY, DELETE_HISTORY);

    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    deleteDeployment(deploymentId, false);

    String entryId = historyService.createUserOperationLogQuery().entityType("Task").singleResult().getId();

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    disableAuthorization();
    assertThat(historyService.createUserOperationLogQuery().entityType("Task").singleResult()).isNull();
    enableAuthorization();

    disableAuthorization();
    historyService.deleteHistoricProcessInstance(processInstanceId);
    enableAuthorization();
  }

  // delete user operation log (case) //////////////////////////////

  @Test
  void testCaseDeleteEntryWithoutAuthorization() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId),
        "It should not be possible to delete the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);
  }

  @Test
  void testCaseDeleteEntryWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ONE_TASK_CASE_KEY, userId, DELETE_HISTORY);

    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId),
        "It should not be possible to delete the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);
  }

  @Test
  void testCaseDeleteEntryWithDeleteHistoryPermissionOnAnyProcessDefinition() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId),
        "It should not be possible to delete the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);
  }

  @Test
  void testCaseDeleteEntryWithDeletePermissionOnCategory() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, DELETE);

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    assertThat(historyService.createUserOperationLogQuery().singleResult()).isNull();
  }

  @Test
  void testCaseDeleteEntryWithDeletePermissionOnAnyCategory() {
    // given
    testRule.createCaseInstanceByKey(ONE_TASK_CASE_KEY);
    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    disableAuthorization();
    String entryId = historyService.createUserOperationLogQuery().singleResult().getId();
    enableAuthorization();

    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, ANY, userId, DELETE);

    // when
    historyService.deleteUserOperationLogEntry(entryId);

    // then
    assertThat(historyService.createUserOperationLogQuery().singleResult()).isNull();
  }

  // update user operation log //////////////////////////////

  @Test
  void testUpdateEntryWithUpdateHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE_HISTORY);

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    // when
    historyService.setAnnotationForOperationLogById(operationId, "anAnnotation");

    disableAuthorization();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult();

    enableAuthorization();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).contains("anAnnotation");
  }

  @Test
  void testUpdateEntryWithUpdateHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_HISTORY);

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    // when
    historyService.setAnnotationForOperationLogById(operationId, "anAnnotation");

    disableAuthorization();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult();

    enableAuthorization();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).contains("anAnnotation");
  }


  @Test
  void testUpdateEntryWithUpdateHistoryPermissionOnAnyProcessDefinition_Standalone() {
    // given
    createTask("aTaskId");

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_HISTORY);

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    assertThatThrownBy(() -> historyService.setAnnotationForOperationLogById(operationId, "anAnnotation"),
        "It should not be possible to update the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);

    // cleanup
    deleteTask("aTaskId", true);
  }

  @Test
  void testUpdateEntryRelatedToProcessDefinitionWithUpdatePermissionOnCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorization(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, UserOperationLogCategoryPermissions.UPDATE);

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    // when
    historyService.setAnnotationForOperationLogById(operationId, "anAnnotation");

    disableAuthorization();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult();

    enableAuthorization();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).contains("anAnnotation");
  }

  @Test
  void testUpdateEntryRelatedToProcessDefinitionWithUpdatePermissionOnAnyCategory() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String taskId = selectSingleTask().getId();
    setAssignee(taskId, "demo");

    createGrantAuthorization(OPERATION_LOG_CATEGORY, ANY, userId, UserOperationLogCategoryPermissions.UPDATE);

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    // when
    historyService.setAnnotationForOperationLogById(operationId, "anAnnotation");

    disableAuthorization();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult();

    enableAuthorization();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).contains("anAnnotation");
  }

  @Test
  void testUpdateEntryWithoutAuthorization() {
    // given
    createTask("aTaskId");

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    assertThatThrownBy(() -> historyService.setAnnotationForOperationLogById(operationId, "anAnnotation"),
        "It should not be possible to update the user operation log")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(OPERATION_LOG_CATEGORY.resourceName())
        .hasMessageContaining(CATEGORY_TASK_WORKER);

    // cleanup
    deleteTask("aTaskId", true);
  }

  @Test
  void testUpdateEntryWithUpdatePermissionOnCategory() {
    // given
    createTask("aTaskId");

    createGrantAuthorization(OPERATION_LOG_CATEGORY, CATEGORY_TASK_WORKER, userId, UserOperationLogCategoryPermissions.UPDATE);

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    // when
    historyService.setAnnotationForOperationLogById(operationId, "anAnnotation");

    disableAuthorization();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult();

    enableAuthorization();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).contains("anAnnotation");

    // cleanup
    deleteTask("aTaskId", true);
  }

  @Test
  void testUpdateEntryWithUpdatePermissionOnAnyCategory() {
    // given
    createTask("aTaskId");

    createGrantAuthorization(OPERATION_LOG_CATEGORY, ANY, userId, UserOperationLogCategoryPermissions.UPDATE);

    disableAuthorization();

    String operationId = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult()
        .getOperationId();

    enableAuthorization();

    // when
    historyService.setAnnotationForOperationLogById(operationId, "anAnnotation");

    disableAuthorization();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType("Task")
        .singleResult();

    enableAuthorization();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).contains("anAnnotation");

    // cleanup
    deleteTask("aTaskId", true);
  }

  // helper ////////////////////////////////////////////////////////

  protected Job selectSingleJob() {
    disableAuthorization();
    Job job = managementService.createJobQuery().singleResult();
    enableAuthorization();
    return job;
  }

  protected void clearDatabase() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendProcessDefinitionHandler.TYPE);
      List<HistoricIncident> incidents = Context.getProcessEngineConfiguration().getHistoryService().createHistoricIncidentQuery().list();
      for (HistoricIncident incident : incidents) {
        commandContext.getHistoricIncidentManager().delete((HistoricIncidentEntity) incident);
      }
      return null;
    });
  }
}
