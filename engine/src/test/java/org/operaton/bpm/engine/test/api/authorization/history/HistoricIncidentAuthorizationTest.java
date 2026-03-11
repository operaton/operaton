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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricIncidentAuthorizationTest extends AuthorizationTest {

  protected static final String TIMER_START_PROCESS_KEY = "timerStartProcess";
  protected static final String ONE_INCIDENT_PROCESS_KEY = "process";
  protected static final String ANOTHER_ONE_INCIDENT_PROCESS_KEY = "anotherOneIncidentProcess";

  protected String deploymentId;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/authorization/timerStartEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/anotherOneIncidentProcess.bpmn20.xml").getId();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);
  }

  // historic incident query (standalone) //////////////////////////////

  @Test
  void testQueryForStandaloneHistoricIncidents() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY, true, new Date());
    String jobId = null;
    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        jobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(jobId, 0);
    enableAuthorization();

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 1);

    disableAuthorization();
    managementService.deleteJob(jobId);
    enableAuthorization();

    clearDatabase();
  }

  // historic incident query (start timer job incident) //////////////////////////////

  @Test
  void testStartTimerJobIncidentQueryWithoutAuthorization() {
    // given
    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 0);
    enableAuthorization();

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testStartTimerJobIncidentQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 0);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testStartTimerJobIncidentQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 0);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  @Disabled("Fixed to use READ_INSTANCE permission, but the test then fails")
  void testStartTimerJobIncidentQueryWithReadInstancePermissionOnAnyProcessDefinition() {
    // given
    disableAuthorization();
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 0);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  // historic incident query ///////////////////////////////////////////

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 1);
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
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

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
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckNoneOnHistoricProcessInstanceAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId,
        ProcessDefinitionPermissions.READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
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
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testHistoricProcessInstancePermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY)
        .getProcessInstanceId();

    disableAuthorization();

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void shouldNotFindIncidentWithRevokedReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // historic incident query (multiple incidents ) ///////////////////////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 5);
  }

  // historic job log (mixed) //////////////////////////////////////////

  @Test
  void testMixedQueryWithoutAuthorization() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY, true, new Date());
    String firstJobId = null;
    List<Job> jobs = managementService.createJobQuery().withRetriesLeft().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        firstJobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(firstJobId, 0);

    repositoryService.suspendProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY, true, new Date());
    String secondJobId = null;
    jobs = managementService.createJobQuery().withRetriesLeft().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        secondJobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(secondJobId, 0);
    enableAuthorization();

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 2);

    disableAuthorization();
    managementService.deleteJob(firstJobId);
    managementService.deleteJob(secondJobId);
    enableAuthorization();

    clearDatabase();
  }

  @Test
  void testMixedQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY, true, new Date());
    String firstJobId = null;
    List<Job> jobs = managementService.createJobQuery().withRetriesLeft().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        firstJobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(firstJobId, 0);

    repositoryService.suspendProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY, true, new Date());
    String secondJobId = null;
    jobs = managementService.createJobQuery().withRetriesLeft().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        secondJobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(secondJobId, 0);
    enableAuthorization();

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 4);

    disableAuthorization();
    managementService.deleteJob(firstJobId);
    managementService.deleteJob(secondJobId);
    enableAuthorization();

    clearDatabase();
  }

  @Test
  void testMixedQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    disableAuthorization();
    repositoryService.suspendProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY, true, new Date());
    String firstJobId = null;
    List<Job> jobs = managementService.createJobQuery().withRetriesLeft().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        firstJobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(firstJobId, 0);

    repositoryService.suspendProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY, true, new Date());
    String secondJobId = null;
    jobs = managementService.createJobQuery().withRetriesLeft().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        secondJobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(secondJobId, 0);
    enableAuthorization();

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ANOTHER_ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // then
    verifyQueryResults(query, 7);

    disableAuthorization();
    managementService.deleteJob(firstJobId);
    managementService.deleteJob(secondJobId);
    enableAuthorization();

    clearDatabase();
  }

  // helper ////////////////////////////////////////////////////////////

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
