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
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;

import java.util.List;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class HistoricActivityInstanceAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String MESSAGE_START_PROCESS_KEY = "messageStartProcess";

  protected String deploymentId;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = createDeployment(null,
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
    deleteDeployment(deploymentId);
  }

  // historic activity instance query /////////////////////////////////

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testSimpleQueryMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void shouldNotFindInstanceWithRevokedReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // historic activity instance query (multiple process instances) ////////////////////////

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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 6);
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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 14);
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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // then
    verifyQueryResults(query, 9);

    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    createGrantAuthorization(Resources.HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId);

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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId);
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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId, processInstanceId);
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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId, processInstanceId);
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
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId, processInstanceId);
  }

  @Test
  void testHistoricProcessInstancePermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getProcessInstanceId();

    disableAuthorization();

    // when
    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId);

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId, processInstanceId);
  }

}
