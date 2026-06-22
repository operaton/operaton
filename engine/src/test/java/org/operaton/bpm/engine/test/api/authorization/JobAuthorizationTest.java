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

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendJobDefinitionHandler;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class JobAuthorizationTest extends AuthorizationTest {

  protected static final String TIMER_START_PROCESS_KEY = "timerStartProcess";
  protected static final String TIMER_BOUNDARY_PROCESS_KEY = "timerBoundaryProcess";
  protected static final String ONE_INCIDENT_PROCESS_KEY = "process";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/authorization/timerStartEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml");
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendJobDefinitionHandler.TYPE);
      return null;
    });
  }

  // job query (jobs associated to a process) //////////////////////////////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryWithReadPermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryWithMultiple() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void shouldNotFindJobWithRevokedReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, ANY, ALL);
    createRevokeAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadInstancePermissionOnTimerStartProcessDefinition() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 1);

    Job job = query.singleResult();
    assertThat(job.getProcessInstanceId()).isNull();
    assertThat(job.getProcessDefinitionKey()).isEqualTo(TIMER_START_PROCESS_KEY);
  }

  @Test
  void testQueryWithReadInstancePermissionOnTimerBoundaryProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 1);

    Job job = query.singleResult();
    assertThat(job.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(job.getProcessDefinitionKey()).isEqualTo(TIMER_BOUNDARY_PROCESS_KEY);
  }

  @Test
  void testQueryWithReadInstancePermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 2);
  }

  // job query (standalone job) /////////////////////////////////

  @Test
  void testStandaloneJobQueryWithoutAuthorization() {
    // given
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, true, new Date(oneWeekFromStartTime));
    enableAuthorization();

    // when
    JobQuery query = managementService.createJobQuery();

    // then
    verifyQueryResults(query, 1);

    Job job = query.singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getProcessInstanceId()).isNull();
    assertThat(job.getProcessDefinitionKey()).isNull();

    deleteJob(job.getId());
  }

  // execute job (standalone job) ////////////////////////////////

  @Test
  void testExecuteStandaloneJob() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, UPDATE);

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, false, new Date(oneWeekFromStartTime));
    enableAuthorization();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.executeJob(jobId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY);
    assertThat(jobDefinition.isSuspended()).isTrue();
  }

  // delete standalone job ////////////////////////////////

  @Test
  void testDeleteStandaloneJob() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, UPDATE);

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, false, new Date(oneWeekFromStartTime));
    enableAuthorization();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.deleteJob(jobId);

    // then
    Job job = selectJobById(jobId);
    assertThat(job).isNull();
  }

  // set job retries (standalone) ////////////////////////////////

  @Test
  void testSetStandaloneJobRetries() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, UPDATE);

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, false, new Date(oneWeekFromStartTime));
    enableAuthorization();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.setJobRetries(jobId, 1);

    // then
    Job job = selectJobById(jobId);
    assertThat(job.getRetries()).isEqualTo(1);

    deleteJob(jobId);
  }

  // set job retries (standalone) ////////////////////////////////

  @Test
  void testSetStandaloneJobDueDate() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, UPDATE);

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, false, new Date(oneWeekFromStartTime));
    enableAuthorization();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.setJobDuedate(jobId, null);

    // then
    Job job = selectJobById(jobId);
    assertThat(job.getDuedate()).isNull();

    deleteJob(jobId);
  }

  // get exception stacktrace ///////////////////////////////////////////

  @Test
  void testGetExceptionStacktraceWithoutAuthorization() {
    // given
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    disableAuthorization();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when + then
    assertThatThrownBy(() -> managementService.getJobExceptionStacktrace(jobId),
            "It should not be possible to get the exception stacktrace")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining("%s' permission on resource '%s' of type '%s' or '".formatted(READ.getName(), processInstanceId, PROCESS_INSTANCE.resourceName()))
        .hasMessageContaining("%s' permission on resource '%s' of type '%s'".formatted(READ_INSTANCE.getName(), ONE_INCIDENT_PROCESS_KEY, PROCESS_DEFINITION.resourceName()));
  }

  @Test
  void testGetExceptionStacktraceWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    String jobExceptionStacktrace = managementService.getJobExceptionStacktrace(jobId);

    // then
    assertThat(jobExceptionStacktrace).isNotNull();
  }

  @Test
  void testGetExceptionStacktraceReadPermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    String jobExceptionStacktrace = managementService.getJobExceptionStacktrace(jobId);

    // then
    assertThat(jobExceptionStacktrace).isNotNull();
  }

  @Test
  void testGetExceptionStacktraceWithReadInstancePermissionOnTimerBoundaryProcessDefinition() {
    // given
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    String jobExceptionStacktrace = managementService.getJobExceptionStacktrace(jobId);

    // then
    assertThat(jobExceptionStacktrace).isNotNull();
  }

  @Test
  void testGetExceptionStacktraceWithReadInstancePermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    String jobExceptionStacktrace = managementService.getJobExceptionStacktrace(jobId);

    // then
    assertThat(jobExceptionStacktrace).isNotNull();
  }

  // get exception stacktrace (standalone) ////////////////////////////////

  @Test
  void testStandaloneJobGetExceptionStacktrace() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, UPDATE);

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, false, new Date(oneWeekFromStartTime));
    enableAuthorization();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    String jobExceptionStacktrace = managementService.getJobExceptionStacktrace(jobId);

    // then
    assertThat(jobExceptionStacktrace).isNull();

    deleteJob(jobId);
  }

  // suspend job by id //////////////////////////////////////////


  @Test
  void testSuspendStandaloneJobById() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, UPDATE);

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, false, new Date(oneWeekFromStartTime));
    enableAuthorization();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.suspendJobById(jobId);

    // then
    Job job = selectJobById(jobId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();

    deleteJob(jobId);
  }

  // activate job by id //////////////////////////////////////////

  @Test
  void testActivateStandaloneJobById() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, UPDATE);

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);

    disableAuthorization();
    // creates a new "standalone" job
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_START_PROCESS_KEY, false, new Date(oneWeekFromStartTime));
    enableAuthorization();

    String jobId = managementService.createJobQuery().singleResult().getId();
    suspendJobById(jobId);

    // when
    managementService.activateJobById(jobId);

    // then
    Job job = selectJobById(jobId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();

    deleteJob(jobId);
  }

  // helper /////////////////////////////////////////////////////

  protected Job selectAnyJob() {
    disableAuthorization();
    Job job = managementService.createJobQuery().listPage(0, 1).get(0);
    enableAuthorization();
    return job;
  }

  protected void deleteJob(String jobId) {
    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();
  }

  protected Job selectJobByProcessInstanceId(String processInstanceId) {
    disableAuthorization();
    Job job = managementService
        .createJobQuery()
        .processInstanceId(processInstanceId)
        .singleResult();
    enableAuthorization();
    return job;
  }

  protected Job selectJobById(String jobId) {
    disableAuthorization();
    Job job = managementService
        .createJobQuery()
        .jobId(jobId)
        .singleResult();
    enableAuthorization();
    return job;
  }

  protected JobDefinition selectJobDefinitionByProcessDefinitionKey(String processDefinitionKey) {
    disableAuthorization();
    JobDefinition jobDefinition = managementService
        .createJobDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .singleResult();
    enableAuthorization();
    return jobDefinition;
  }

}
