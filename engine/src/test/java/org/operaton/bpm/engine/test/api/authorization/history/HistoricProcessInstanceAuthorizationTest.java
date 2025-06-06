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

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE_HISTORY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.*;
import org.operaton.bpm.engine.history.*;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.query.PeriodUnit;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
class HistoricProcessInstanceAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String MESSAGE_START_PROCESS_KEY = "messageStartProcess";

  protected String deploymentId;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/messageStartEventProcess.bpmn20.xml")
            .getId();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);
  }

  // historic process instance query //////////////////////////////////////////////////////////

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    HistoricProcessInstance instance = query.singleResult();
    assertThat(instance).isNotNull();
    assertThat(instance.getId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    HistoricProcessInstance instance = query.singleResult();
    assertThat(instance).isNotNull();
    assertThat(instance.getId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void shouldNotFindInstanceWithRevokedReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // historic process instance query (multiple process instances) ////////////////////////

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
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

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
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

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
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    verifyQueryResults(query, 7);
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
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    verifyQueryResults(query, 3);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  // delete historic process instance //////////////////////////////

  @Test
  void testDeleteHistoricProcessInstanceWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    try {
      // when
      historyService.deleteHistoricProcessInstance(processInstanceId);
      fail("Exception expected: It should not be possible to delete the historic process instance");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(DELETE_HISTORY.getName(), message);
      testRule.assertTextPresent(PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testDeleteHistoricProcessInstanceWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, DELETE_HISTORY);

    // when
    historyService.deleteHistoricProcessInstance(processInstanceId);

    // then
    disableAuthorization();
    long count = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .count();
    assertThat(count).isZero();
    enableAuthorization();
  }

  @Test
  void testDeleteHistoricProcessInstanceWithDeleteHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    // when
    historyService.deleteHistoricProcessInstance(processInstanceId);

    // then
    disableAuthorization();
    long count = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .count();
    assertThat(count).isZero();
    enableAuthorization();
  }

  @Test
  void testDeleteHistoricProcessInstanceAfterDeletingDeployment() {
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
    historyService.deleteHistoricProcessInstance(processInstanceId);

    // then
    disableAuthorization();
    long count = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .count();
    assertThat(count).isZero();
    enableAuthorization();
  }

  // create historic process instance report

  @Test
  void testHistoricProcessInstanceReportWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    HistoricProcessInstanceReport report = historyService.createHistoricProcessInstanceReport();

    assertThatThrownBy(() -> report.duration(PeriodUnit.MONTH))
        .withFailMessage("Exception expected: It should not be possible to create a historic process instance report")
        .isInstanceOf(AuthorizationException.class)
        .extracting("missingAuthorizations", as(list(MissingAuthorization.class)))
        .hasSize(1)
        .first()
        .satisfies(missingAuthorization -> {
          assertThat(missingAuthorization.getViolatedPermissionName()).isEqualTo(READ_HISTORY.getName());
          assertThat(missingAuthorization.getResourceType()).isEqualTo(PROCESS_DEFINITION.resourceName());
          assertThat(missingAuthorization.getResourceId()).isEqualTo(ANY);
        });
  }

  @Test
  void testHistoricProcessInstanceReportWithHistoryReadPermissionOnAny() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(PeriodUnit.MONTH);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testReportWithoutQueryCriteriaAndAnyReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, "*", userId, READ_HISTORY);

    // when
    List<DurationReportResult> result = historyService
        .createHistoricProcessInstanceReport()
        .duration(PeriodUnit.MONTH);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testReportWithoutQueryCriteriaAndNoReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    // when
    HistoricProcessInstanceReport report = historyService.createHistoricProcessInstanceReport();

    // then
    assertThatThrownBy(() -> report.duration(PeriodUnit.MONTH))
        .withFailMessage("Exception expected: It should not be possible to create a historic process instance report")
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testReportWithQueryCriterionProcessDefinitionKeyInAndReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, userId, READ_HISTORY);

    // when
    List<DurationReportResult> result = historyService
      .createHistoricProcessInstanceReport()
      .processDefinitionKeyIn(PROCESS_KEY, MESSAGE_START_PROCESS_KEY)
      .duration(PeriodUnit.MONTH);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testReportWithQueryCriterionProcessDefinitionKeyInAndMissingReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricProcessInstanceReport report = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn(PROCESS_KEY, MESSAGE_START_PROCESS_KEY);
    assertThatThrownBy(() -> report.duration(PeriodUnit.MONTH))
        .withFailMessage("Exception expected: It should not be possible to create a historic process instance report")
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testReportWithQueryCriterionProcessDefinitionIdInAndReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, userId, READ_HISTORY);

    // when
    List<DurationReportResult> result = historyService
      .createHistoricProcessInstanceReport()
      .processDefinitionIdIn(processInstance1.getProcessDefinitionId(), processInstance2.getProcessDefinitionId())
      .duration(PeriodUnit.MONTH);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testReportWithQueryCriterionProcessDefinitionIdInAndMissingReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
    var historicProcessInstanceReport = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionIdIn(processInstance1.getProcessDefinitionId(), processInstance2.getProcessDefinitionId());

    // when
    try {
      historicProcessInstanceReport.duration(PeriodUnit.MONTH);

      // then
      fail("Exception expected: It should not be possible to create a historic process instance report");
    } catch (AuthorizationException e) {
      // expected
    }
  }

  @Test
  void testReportWithMixedQueryCriteriaAndReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, userId, READ_HISTORY);

    // when
    List<DurationReportResult> result = historyService
      .createHistoricProcessInstanceReport()
      .processDefinitionKeyIn(PROCESS_KEY)
      .processDefinitionIdIn(processInstance2.getProcessDefinitionId())
      .duration(PeriodUnit.MONTH);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testReportWithMixedQueryCriteriaAndMissingReadHistoryPermission() {
    // given
    ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
    runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
    HistoricProcessInstanceReport report = historyService
        .createHistoricProcessInstanceReport()
        .processDefinitionKeyIn(PROCESS_KEY)
        .processDefinitionIdIn(processInstance2.getProcessDefinitionId());

    // when
    assertThatThrownBy(() -> report.duration(PeriodUnit.MONTH))
        // then
        .withFailMessage("Exception expected: It should not be possible to create a historic process instance report")
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testReportWithQueryCriterionProcessInstanceIdInWrongProcessDefinitionId() {
    // when
    List<DurationReportResult> result = historyService
      .createHistoricProcessInstanceReport()
      .processDefinitionIdIn("aWrongProcessDefinitionId")
      .duration(PeriodUnit.MONTH);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testHistoryCleanupReportWithPermissions() {
    // given
    prepareProcessInstances(PROCESS_KEY, -6, 5, 10);

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, Permissions.READ, Permissions.READ_HISTORY);
    createGrantAuthorizationGroup(PROCESS_DEFINITION, PROCESS_KEY, groupId, Permissions.READ, Permissions.READ_HISTORY);

    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getCleanableProcessInstanceCount()).isEqualTo(10);
    assertThat(reportResults.get(0).getFinishedProcessInstanceCount()).isEqualTo(10);
  }

  @Test
  void testHistoryCleanupReportWithReadPermissionOnly() {
    // given
    prepareProcessInstances(PROCESS_KEY, -6, 5, 10);

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, Permissions.READ);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void testHistoryCleanupReportWithReadHistoryPermissionOnly() {
    // given
    prepareProcessInstances(PROCESS_KEY, -6, 5, 10);

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, Permissions.READ_HISTORY);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void testHistoryCleanupReportWithoutPermissions() {
    // given
    prepareProcessInstances(PROCESS_KEY, -6, 5, 10);

    // when
    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).isEmpty();
  }

  @Test
  void shouldNotFindCleanupReportWithRevokedReadHistoryPermissionOnProcessDefinition() {
    // given
    prepareProcessInstances(PROCESS_KEY, -6, 5, 10);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, Permissions.READ, Permissions.READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, Permissions.READ_HISTORY);

    List<CleanableHistoricProcessInstanceReportResult> reportResults = historyService.createCleanableHistoricProcessInstanceReport().list();

    // then
    assertThat(reportResults).hasSize(1);
    assertThat(reportResults.get(0).getProcessDefinitionKey()).isEqualTo(MESSAGE_START_PROCESS_KEY);
    assertThat(reportResults.get(0).getCleanableProcessInstanceCount()).isZero();
    assertThat(reportResults.get(0).getFinishedProcessInstanceCount()).isZero();
  }

  @Test
  void testCheckAllHistoricProcessInstancePermissions() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    // when
    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, ANY, userId,
        HistoricProcessInstancePermissions.ALL);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.NONE, Resources.HISTORIC_PROCESS_INSTANCE)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.READ, Resources.HISTORIC_PROCESS_INSTANCE)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.ALL, Resources.HISTORIC_PROCESS_INSTANCE)).isTrue();
  }

  @Test
  void testCheckReadHistoricProcessInstancePermissions() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    // when
    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, ANY, userId,
        HistoricProcessInstancePermissions.READ);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.NONE, Resources.HISTORIC_PROCESS_INSTANCE)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.READ, Resources.HISTORIC_PROCESS_INSTANCE)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.ALL, Resources.HISTORIC_PROCESS_INSTANCE)).isFalse();
  }

  @Test
  void testCheckNoneHistoricProcessInstancePermission() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    // when
    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, ANY, userId,
        HistoricProcessInstancePermissions.NONE);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.NONE, Resources.HISTORIC_PROCESS_INSTANCE)).isTrue();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.READ, Resources.HISTORIC_PROCESS_INSTANCE)).isFalse();

    assertThat(authorizationService.isUserAuthorized(userId, null,
        HistoricProcessInstancePermissions.ALL, Resources.HISTORIC_PROCESS_INSTANCE)).isFalse();
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

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
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

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

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

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
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testHistoricProcessInstancePermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getProcessInstanceId();

    disableAuthorization();

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testDeleteHistoricAuthorizationRelatedToHistoricProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    String taskId = selectSingleTask().getId();

    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, DELETE_HISTORY);

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // assume
    AuthorizationQuery authorizationQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE)
        .resourceId(processInstanceId);

    assertThat(authorizationQuery.list())
        .extracting("resourceId")
        .containsExactly(processInstanceId);

    // when
    historyService.deleteHistoricProcessInstance(processInstanceId);

    // then
    authorizationQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE)
        .resourceId(processInstanceId);

    assertThat(authorizationQuery.list()).isEmpty();
  }

  // helper ////////////////////////////////////////////////////////

  protected void prepareProcessInstances(String key, int daysInThePast, Integer historyTimeToLive, int instanceCount) {
    ProcessDefinition processDefinition = selectProcessDefinitionByKey(key);
    disableAuthorization();
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinition.getId(), historyTimeToLive);
    enableAuthorization();

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(new Date(), daysInThePast));

    List<String> processInstanceIds = new ArrayList<>();
    for (int i = 0; i < instanceCount; i++) {
      ProcessInstance processInstance = startProcessInstanceByKey(key);
      processInstanceIds.add(processInstance.getId());
    }

    disableAuthorization();
    runtimeService.deleteProcessInstances(processInstanceIds, null, true, true);
    enableAuthorization();

    ClockUtil.setCurrentTime(oldCurrentTime);
  }

}
