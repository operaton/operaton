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
package org.operaton.bpm.engine.test.history;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_EXTERNAL_TASK_NAME;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_TOPIC;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.createDefaultExternalTaskModel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricExternalTaskLogQueryTest {

  protected static final String WORKER_ID = "aWorkerId";
  protected static final long LOCK_DURATION = 5 * 60L * 1000L;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;
  ExternalTaskService externalTaskService;

  @Test
  void testQuery() {

    // given
    ExternalTask task = startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryById() {
    // given
    startExternalTaskProcesses(2);
    String logId = retrieveFirstHistoricExternalTaskLog().getId();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .logId(logId)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getId()).isEqualTo(logId);
  }

  @Test
  void testQueryFailsByInvalidId() {

    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.logId(null))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryByNonExistingId() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .logId("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByExternalTaskId() {
    // given
    startExternalTaskProcesses(2);
    String logExternalTaskId = retrieveFirstHistoricExternalTaskLog().getExternalTaskId();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .externalTaskId(logExternalTaskId)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(logExternalTaskId);
  }

  @Test
  void testQueryFailsByInvalidExternalTaskId() {

    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.externalTaskId(null))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryByNonExistingExternalTaskId() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .externalTaskId("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByTopicName() {

    // given
    String dummyTopic = "dummy";
    startExternalTaskProcessGivenTopicName(dummyTopic);
    ExternalTask task = startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .topicName(DEFAULT_TOPIC)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryFailsByInvalidTopicName() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.topicName(null))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNonExistingTopicName() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .topicName("foo bar")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByWorkerId() {
    // given
    List<ExternalTask> taskList = startExternalTaskProcesses(2);
    ExternalTask taskToCheck = taskList.get(1);
    completeExternalTaskWithWorker(taskList.get(0).getId(), "dummyWorker");
    completeExternalTaskWithWorker(taskToCheck.getId(), WORKER_ID);

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .workerId(WORKER_ID)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(taskToCheck.getId());
  }

  @Test
  void testQueryFailsByInvalidWorkerId() {
    // given
    ExternalTask task = startExternalTaskProcess();
    completeExternalTask(task.getId());
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.workerId(null))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryByNonExistingWorkerId() {

    // given
    ExternalTask task = startExternalTaskProcess();
    completeExternalTask(task.getId());

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .workerId("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByErrorMessage() {
    // given
    List<ExternalTask> taskList = startExternalTaskProcesses(2);
    String errorMessage = "This is an important error!";
    reportExternalTaskFailure(taskList.get(0).getId(), "Dummy error message");
    reportExternalTaskFailure(taskList.get(1).getId(), errorMessage);

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .errorMessage(errorMessage)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(taskList.get(1).getId());
  }

  @Test
  void testQueryFailsByInvalidErrorMessage() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.errorMessage(null))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNonExistingErrorMessage() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .errorMessage("asdfasdf")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByActivityId() {
    // given
    startExternalTaskProcessGivenActivityId("dummyName");
    ExternalTask task = startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .activityIdIn(DEFAULT_EXTERNAL_TASK_NAME)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryFailsByActivityIdsIsNull() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.activityIdIn((String[]) null))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryFailsByActivityIdsContainNull() {
    // given
    startExternalTaskProcess();
    String[] activityIdsContainNull = {"a", null, "b"};
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.activityIdIn(activityIdsContainNull))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryFailsByActivityIdsContainEmptyString() {
    // given
    startExternalTaskProcess();
    String[] activityIdsContainEmptyString = {"a", "", "b"};
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery
        .activityIdIn(activityIdsContainEmptyString))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNonExistingActivityIds() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .activityIdIn("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByActivityInstanceIds() {
    // given
    startExternalTaskProcessGivenActivityId("dummyName");
    ExternalTask task = startExternalTaskProcess();
    String activityInstanceId = historyService.createHistoricActivityInstanceQuery()
      .activityId(DEFAULT_EXTERNAL_TASK_NAME)
      .singleResult()
      .getId();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .activityInstanceIdIn(activityInstanceId)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryFailsByActivityInstanceIdsIsNull() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.activityInstanceIdIn((String[]) null))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryFailsByActivityInstanceIdsContainNull() {
    // given
    startExternalTaskProcess();
    String[] activityIdsContainNull = {"a", null, "b"};
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.activityInstanceIdIn(activityIdsContainNull))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryFailsByActivityInstanceIdsContainEmptyString() {
    // given
    startExternalTaskProcess();
    String[] activityIdsContainEmptyString = {"a", "", "b"};
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.activityInstanceIdIn(activityIdsContainEmptyString))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNonExistingActivityInstanceIds() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .activityInstanceIdIn("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByExecutionIds() {
    // given
    startExternalTaskProcesses(2);
    HistoricExternalTaskLog taskLog = retrieveFirstHistoricExternalTaskLog();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .executionIdIn(taskLog.getExecutionId())
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getId()).isEqualTo(taskLog.getId());
  }

  @Test
  void testQueryFailsByExecutionIdsIsNull() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.executionIdIn((String[]) null))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryFailsByExecutionIdsContainNull() {
    // given
    startExternalTaskProcess();
    String[] activityIdsContainNull = {"a", null, "b"};
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.executionIdIn(activityIdsContainNull))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryFailsByExecutionIdsContainEmptyString() {
    // given
    startExternalTaskProcess();
    String[] activityIdsContainEmptyString = {"a", "", "b"};
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.executionIdIn(activityIdsContainEmptyString))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNonExistingExecutionIds() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .executionIdIn("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByProcessInstanceId() {
    // given
    startExternalTaskProcesses(2);
    String processInstanceId = retrieveFirstHistoricExternalTaskLog().getProcessInstanceId();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testQueryFailsByInvalidProcessInstanceId() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.processInstanceId(null))
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryByNonExistingProcessInstanceId() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .processInstanceId("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByProcessDefinitionId() {
    // given
    startExternalTaskProcesses(2);
    String definitionId = retrieveFirstHistoricExternalTaskLog().getProcessDefinitionId();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .processDefinitionId(definitionId)
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getProcessDefinitionId()).isEqualTo(definitionId);
  }

  @Test
  void testQueryFailsByInvalidProcessDefinitionId() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.processDefinitionId(null))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNonExistingProcessDefinitionId() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .processDefinitionId("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByProcessDefinitionKey() {
    // given
    startExternalTaskProcessGivenProcessDefinitionKey("dummyProcess");
    ExternalTask task = startExternalTaskProcessGivenProcessDefinitionKey("Process");

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .processDefinitionKey(task.getProcessDefinitionKey())
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryFailsByInvalidProcessDefinitionKey() {
    // given
    startExternalTaskProcess();
    var historicExternalTaskLogQuery = historyService
      .createHistoricExternalTaskLogQuery();

    // when/then
    assertThatThrownBy(() -> historicExternalTaskLogQuery.processDefinitionKey(null))
      .isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNonExistingProcessDefinitionKey() {

    // given
    startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .processDefinitionKey("foo")
      .singleResult();

    // then
    assertThat(log).isNull();
  }

  @Test
  void testQueryByCreationLog() {
    // given
    ExternalTask task = startExternalTaskProcess();
    completeExternalTask(task.getId());

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .creationLog()
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryByFailureLog() {
    // given
    ExternalTask task = startExternalTaskProcess();
    reportExternalTaskFailure(task.getId(), "Dummy error message!");

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryBySuccessLog() {
    // given
    ExternalTask task = startExternalTaskProcess();
    completeExternalTask(task.getId());

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .successLog()
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryByDeletionLog() {
    // given
    ExternalTask task = startExternalTaskProcess();
    runtimeService.deleteProcessInstance(task.getProcessInstanceId(), null);

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .deletionLog()
      .singleResult();

    // then
    assertThat(log).isNotNull();
    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
  }

  @Test
  void testQueryByLowerThanOrEqualAPriority() {

    // given
    startExternalTaskProcesses(5);

    // when
    List<HistoricExternalTaskLog> externalTaskLogs = historyService
      .createHistoricExternalTaskLogQuery()
      .priorityLowerThanOrEquals(2L)
      .list();

    // then
    assertThat(externalTaskLogs).hasSize(3);
    for (HistoricExternalTaskLog log : externalTaskLogs) {
      assertThat(log.getPriority() <= 2).isTrue();
    }

  }

  @Test
  void testQueryByHigherThanOrEqualAPriority() {

    // given
    startExternalTaskProcesses(5);

    // when
    List<HistoricExternalTaskLog> externalTaskLogs = historyService
      .createHistoricExternalTaskLogQuery()
      .priorityHigherThanOrEquals(2L)
      .list();

    // then
    assertThat(externalTaskLogs).hasSize(3);
    for (HistoricExternalTaskLog log : externalTaskLogs) {
      assertThat(log.getPriority() >= 2).isTrue();
    }

  }

  @Test
  void testQueryByPriorityRange() {

    // given
    startExternalTaskProcesses(5);

    // when
    List<HistoricExternalTaskLog> externalTaskLogs = historyService
      .createHistoricExternalTaskLogQuery()
      .priorityLowerThanOrEquals(3L)
      .priorityHigherThanOrEquals(1L)
      .list();

    // then
    assertThat(externalTaskLogs).hasSize(3);
    for (HistoricExternalTaskLog log : externalTaskLogs) {
      assertThat(log.getPriority() <= 3 && log.getPriority() >= 1).isTrue();
    }

  }

  @Test
  void testQueryByDisjunctivePriorityStatements() {

    // given
    startExternalTaskProcesses(5);

    // when
    List<HistoricExternalTaskLog> externalTaskLogs = historyService
      .createHistoricExternalTaskLogQuery()
      .priorityLowerThanOrEquals(1L)
      .priorityHigherThanOrEquals(4L)
      .list();

    // then
    assertThat(externalTaskLogs).isEmpty();
  }


  // helper methods

  protected HistoricExternalTaskLog retrieveFirstHistoricExternalTaskLog() {
    return historyService
      .createHistoricExternalTaskLogQuery()
      .list()
      .get(0);
  }

  protected void completeExternalTaskWithWorker(String externalTaskId, String workerId) {
    completeExternalTask(externalTaskId, DEFAULT_TOPIC, workerId, false);

  }

  protected void completeExternalTask(String externalTaskId) {
    completeExternalTask(externalTaskId, DEFAULT_TOPIC, WORKER_ID, false);
  }

  protected void completeExternalTask(String externalTaskId, String topic, String workerId, boolean usePriority) {
    List<LockedExternalTask> list = externalTaskService.fetchAndLock(100, workerId, usePriority)
      .topic(topic, LOCK_DURATION)
      .execute();
    externalTaskService.complete(externalTaskId, workerId);
    // unlock the remaining tasks
    for (LockedExternalTask lockedExternalTask : list) {
      if (!lockedExternalTask.getId().equals(externalTaskId)) {
        externalTaskService.unlock(lockedExternalTask.getId());
      }
    }
  }

  protected void reportExternalTaskFailure(String externalTaskId, String errorMessage) {
    reportExternalTaskFailure(externalTaskId, DEFAULT_TOPIC, WORKER_ID, 1, false, errorMessage);
  }

  protected void reportExternalTaskFailure(String externalTaskId, String topic, String workerId, Integer retries,
                                           boolean usePriority, String errorMessage) {
    List<LockedExternalTask> list = externalTaskService.fetchAndLock(100, workerId, usePriority)
      .topic(topic, LOCK_DURATION)
      .execute();
    externalTaskService.handleFailure(externalTaskId, workerId, errorMessage, retries, 0L);

    for (LockedExternalTask lockedExternalTask : list) {
        externalTaskService.unlock(lockedExternalTask.getId());
    }
  }

  protected List<ExternalTask> startExternalTaskProcesses(int count) {
    List<ExternalTask> list = new LinkedList<>();
    for (int ithPrio = 0; ithPrio < count; ithPrio++) {
      list.add(startExternalTaskProcessGivenPriority(ithPrio));
    }
    return list;
  }

  protected ExternalTask startExternalTaskProcessGivenTopicName(String topicName) {
    BpmnModelInstance processModelWithCustomTopic = createDefaultExternalTaskModel().topic(topicName).build();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(processModelWithCustomTopic);
    ProcessInstance pi = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    return externalTaskService.createExternalTaskQuery().processInstanceId(pi.getId()).singleResult();
  }

  protected ExternalTask startExternalTaskProcessGivenActivityId(String activityId) {
    BpmnModelInstance processModelWithCustomActivityId = createDefaultExternalTaskModel().externalTaskName(activityId).build();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(processModelWithCustomActivityId);
    ProcessInstance pi = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    return externalTaskService.createExternalTaskQuery().processInstanceId(pi.getId()).singleResult();
  }

  protected ExternalTask startExternalTaskProcessGivenProcessDefinitionKey(String processDefinitionKey) {
    BpmnModelInstance processModelWithCustomKey = createDefaultExternalTaskModel().processKey(processDefinitionKey).build();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(processModelWithCustomKey);
    ProcessInstance pi = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    return externalTaskService.createExternalTaskQuery().processInstanceId(pi.getId()).singleResult();
  }

  protected ExternalTask startExternalTaskProcessGivenPriority(int priority) {
    BpmnModelInstance processModelWithCustomPriority = createDefaultExternalTaskModel().priority(priority).build();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(processModelWithCustomPriority);
    ProcessInstance pi = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    return externalTaskService.createExternalTaskQuery().processInstanceId(pi.getId()).singleResult();
  }

  protected ExternalTask startExternalTaskProcess() {
    BpmnModelInstance oneExternalTaskProcess = createDefaultExternalTaskModel().build();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(oneExternalTaskProcess);
    ProcessInstance pi = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    return externalTaskService.createExternalTaskQuery().processInstanceId(pi.getId()).singleResult();
  }

}
