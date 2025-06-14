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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_PROCESS_KEY;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_TOPIC;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.createDefaultExternalTaskModel;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;

import java.util.List;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLogQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricExternalTaskLogAuthorizationTest extends AuthorizationTest {

  protected static final String WORKER_ID = "aWorkerId";
  protected static final long LOCK_DURATION = 5 * 60L * 1000L;
  protected static final String ERROR_DETAILS = "These are the error details!";
  protected static final String ANOTHER_PROCESS_KEY = "AnotherProcess";

  @Override
  @BeforeEach
  public void setUp() {
    BpmnModelInstance defaultModel = createDefaultExternalTaskModel().build();
    BpmnModelInstance modifiedModel = createDefaultExternalTaskModel().processKey(ANOTHER_PROCESS_KEY).build();
    testRule.deploy(defaultModel, modifiedModel);
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);
  }

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithHistoryReadPermissionOnProcessDefinition() {

    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithHistoryReadPermissionOnAnyProcessDefinition() {

    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithMultipleAuthorizations() {
    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure();

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithHistoryReadPermissionOnOneProcessDefinition() {
    // given
    startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure();
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 6);
  }

  @Test
  void testQueryWithHistoryReadPermissionOnAnyProcessDefinition() {
    // given
    startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 8);
  }

  @Test
  void shouldNotFindLogsWithRevokedHistoryReadPermissionOnAnyProcessDefinition() {
    // given
    startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testGetErrorDetailsWithoutAuthorization() {
    // given
    startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure();

    disableAuthorization();
    String failedHistoricExternalTaskLogId = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .list()
      .get(0)
      .getId();
    enableAuthorization();

    try {
      // when
      historyService.getHistoricExternalTaskLogErrorDetails(failedHistoricExternalTaskLogId);
      fail("Exception expected: It should not be possible to retrieve the error details");
    } catch (AuthorizationException e) {
      // then
      String exceptionMessage = e.getMessage();
      testRule.assertTextPresent(userId, exceptionMessage);
      testRule.assertTextPresent(READ_HISTORY.getName(), exceptionMessage);
      testRule.assertTextPresent(DEFAULT_PROCESS_KEY, exceptionMessage);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), exceptionMessage);
    }
  }

  @Test
  void testGetErrorDetailsWithHistoryReadPermissionOnProcessDefinition() {

    // given
    startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure();
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId, READ_HISTORY);

    String failedHistoricExternalTaskLogId = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .list()
      .get(0)
      .getId();

    // when
    String stacktrace = historyService.getHistoricExternalTaskLogErrorDetails(failedHistoricExternalTaskLogId);

    // then
    assertThat(stacktrace).isNotNull().isEqualTo(ERROR_DETAILS);
  }

  @Test
  void testGetErrorDetailsWithHistoryReadPermissionOnProcessAnyDefinition() {

    // given
    startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    String failedHistoricExternalTaskLogId = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .list()
      .get(0)
      .getId();

    // when
    String stacktrace = historyService.getHistoricExternalTaskLogErrorDetails(failedHistoricExternalTaskLogId);

    // then
    assertThat(stacktrace).isNotNull().isEqualTo(ERROR_DETAILS);
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(DEFAULT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(DEFAULT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckNoneOnHistoricProcessInstanceAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(DEFAULT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId,
        ProcessDefinitionPermissions.READ_HISTORY);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadOnHistoricProcessInstanceAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(DEFAULT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testHistoricProcessInstancePermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(DEFAULT_PROCESS_KEY)
        .getProcessInstanceId();

    disableAuthorization();

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  protected void startThreeProcessInstancesDeleteOneAndCompleteTwoWithFailure() {
    disableAuthorization();
    ProcessInstance pi1 = startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    ProcessInstance pi2 = startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    ProcessInstance pi3 = startProcessInstanceByKey(ANOTHER_PROCESS_KEY);

    completeExternalTaskWithFailure(pi1);
    completeExternalTaskWithFailure(pi2);

    runtimeService.deleteProcessInstance(pi3.getId(), "Dummy reason for deletion!");
    enableAuthorization();
  }

  protected void completeExternalTaskWithFailure(ProcessInstance pi) {
    ExternalTask task = externalTaskService
      .createExternalTaskQuery()
      .processInstanceId(pi.getId())
      .singleResult();
    completeExternalTaskWithFailure(task.getId());
  }

  protected void completeExternalTaskWithFailure(String externalTaskId) {
    List<LockedExternalTask> list = externalTaskService.fetchAndLock(5, WORKER_ID, false)
      .topic(DEFAULT_TOPIC, LOCK_DURATION)
      .execute();
    externalTaskService.handleFailure(externalTaskId, WORKER_ID, "This is an error!", ERROR_DETAILS, 1, 0L);
    externalTaskService.complete(externalTaskId, WORKER_ID);
    // unlock the remaining tasks
    for (LockedExternalTask lockedExternalTask : list) {
      if (!lockedExternalTask.getId().equals(externalTaskId)) {
        externalTaskService.unlock(lockedExternalTask.getId());
      }
    }
  }

}
