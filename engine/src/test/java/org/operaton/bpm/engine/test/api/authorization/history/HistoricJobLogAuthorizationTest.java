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
package org.operaton.bpm.engine.test.api.authorization.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricJobLogAuthorizationTest extends AuthorizationTest {

  protected static final String TIMER_START_PROCESS_KEY = "timerStartProcess";
  protected static final String TIMER_BOUNDARY_PROCESS_KEY = "timerBoundaryProcess";
  protected static final String ONE_INCIDENT_PROCESS_KEY = "process";

  protected String batchId;
  protected String deploymentId;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/authorization/timerStartEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml")
            .getId();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendProcessDefinitionHandler.TYPE);
      return null;
    });
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);

    if (batchId != null) {
      managementService.deleteBatch(batchId, true);
      batchId = null;
    }
  }

  // historic job log query (start timer job) ////////////////////////////////

  @Test
  void testStartTimerJobLogQueryWithoutAuthorization() {
    // given

    // when

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testStartTimerJobLogQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testStartTimerJobLogQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 1);
  }

  // historic job log query ////////////////////////////////////////////////

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithHistoryReadPermissionOnProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 4);
  }

  @Test
  void testSimpleQueryWithHistoryReadPermissionOnAnyProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 5);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 5);
  }

  @Test
  void shouldNotFindJobLogWithRevokedHistoryReadPermissionOnAnyProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // historic job log query (multiple process instance) ////////////////////////////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    disableAuthorization();
    String jobId = managementService.createJobQuery().processDefinitionKey(TIMER_START_PROCESS_KEY).singleResult().getId();
    managementService.executeJob(jobId);
    jobId = managementService.createJobQuery().processDefinitionKey(TIMER_START_PROCESS_KEY).singleResult().getId();
    managementService.executeJob(jobId);
    enableAuthorization();

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithHistoryReadPermissionOnProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    disableAuthorization();
    String jobId = managementService.createJobQuery().processDefinitionKey(TIMER_START_PROCESS_KEY).singleResult().getId();
    managementService.executeJob(jobId);
    jobId = managementService.createJobQuery().processDefinitionKey(TIMER_START_PROCESS_KEY).singleResult().getId();
    managementService.executeJob(jobId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 12);
  }

  @Test
  void testQueryWithHistoryReadPermissionOnAnyProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    disableAuthorization();
    String jobId = managementService.createJobQuery().processDefinitionKey(TIMER_START_PROCESS_KEY).singleResult().getId();
    managementService.executeJob(jobId);
    jobId = managementService.createJobQuery().processDefinitionKey(TIMER_START_PROCESS_KEY).singleResult().getId();
    managementService.executeJob(jobId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 17);
  }

  // historic job log query (standalone job) ///////////////////////

  @Test
  void testQueryAfterStandaloneJob() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY, true, new Date());
    enableAuthorization();

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 1);

    HistoricJobLog jobLog = query.singleResult();
    assertThat(jobLog.getProcessDefinitionKey()).isNull();

    deleteDeployment(deploymentId);

    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.deleteJob(jobId);
    enableAuthorization();
  }

  // delete deployment (cascade = false)

  @Test
  void testQueryAfterDeletingDeployment() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, READ_HISTORY);

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
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    verifyQueryResults(query, 6);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  // get historic job log exception stacktrace (standalone) /////////////////////

  @Test
  void testGetHistoricStandaloneJobLogExceptionStacktrace() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY, true, new Date());
    enableAuthorization();
    String jobLogId = historyService.createHistoricJobLogQuery().singleResult().getId();

    // when
    String stacktrace = historyService.getHistoricJobLogExceptionStacktrace(jobLogId);

    // then
    assertThat(stacktrace).isNull();

    deleteDeployment(deploymentId);

    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.deleteJob(jobId);
    enableAuthorization();
  }

  // get historic job log exception stacktrace /////////////////////

  @Test
  void testGetHistoricJobLogExceptionStacktraceWithoutAuthorization() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    disableAuthorization();
    String jobLogId = historyService.createHistoricJobLogQuery().failureLog().listPage(0, 1).get(0).getId();
    enableAuthorization();

    try {
      // when
      historyService.getHistoricJobLogExceptionStacktrace(jobLogId);
      fail("Exception expected: It should not be possible to get the historic job log exception stacktrace");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(READ_HISTORY.getName(), message);
      testRule.assertTextPresent(ONE_INCIDENT_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testGetHistoricJobLogExceptionStacktraceWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    disableAuthorization();
    String jobLogId = historyService.createHistoricJobLogQuery().failureLog().listPage(0, 1).get(0).getId();
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    String stacktrace = historyService.getHistoricJobLogExceptionStacktrace(jobLogId);

    // then
    assertThat(stacktrace).isNotNull();
  }

  @Test
  void testGetHistoricJobLogExceptionStacktraceWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    disableAuthorization();
    String jobLogId = historyService.createHistoricJobLogQuery().failureLog().listPage(0, 1).get(0).getId();
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    String stacktrace = historyService.getHistoricJobLogExceptionStacktrace(jobLogId);

    // then
    assertThat(stacktrace).isNotNull();
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(
            processInstanceId,
            processInstanceId,
            processInstanceId,
            processInstanceId
        );
  }

  @Test
  void testCheckNoneOnHistoricProcessInstanceAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(
            processInstanceId,
            processInstanceId,
            processInstanceId,
            processInstanceId
        );
  }

  @Test
  void testCheckReadOnHistoricProcessInstanceAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(
            processInstanceId,
            processInstanceId,
            processInstanceId,
            processInstanceId
        );
  }

  @Test
  void testHistoricProcessInstancePermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    disableAuthorization();

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(
            processInstanceId,
            processInstanceId,
            processInstanceId,
            processInstanceId
        );
  }

  @Test
  void testSkipAuthOnNonProcessJob() {
    // given
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    disableAuthorization();
    batchId =
        runtimeService.deleteProcessInstancesAsync(List.of(processInstanceId), "bar")
            .getId();
    enableAuthorization();

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.list())
        .extracting("jobDefinitionType", "processInstanceId")
        .containsExactly(tuple("batch-seed-job", null));
  }

  @Test
  void testSkipAuthOnNonProcessJob_HistoricInstancePermissionsEnabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    disableAuthorization();
    batchId =
        runtimeService.deleteProcessInstancesAsync(List.of(processInstanceId), "bar")
            .getId();
    enableAuthorization();

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.list())
        .extracting("jobDefinitionType", "processInstanceId")
        .containsExactly(tuple("batch-seed-job", null));
  }

}
