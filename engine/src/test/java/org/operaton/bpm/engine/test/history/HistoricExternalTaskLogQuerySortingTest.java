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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricExternalTaskLogQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil;
import org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.NullTolerantComparator;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.*;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_TOPIC;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.createDefaultExternalTaskModel;
import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricExternalTaskLogQuerySortingTest {

  private static final String WORKER_ID = "aWorkerId";
  private static final Long LOCK_DURATION = 5 * 60L * 1000L;

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
  void testQuerySortingByTimestampAsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByTimestamp()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskByTimestamp());
  }

  @Test
  void testQuerySortingByTimestampDsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByTimestamp()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskByTimestamp()));
  }

  @Test
  void testQuerySortingByTaskIdAsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByExternalTaskId()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByExternalTaskId());
  }

  @Test
  void testQuerySortingByTaskIdDsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByExternalTaskId()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByExternalTaskId()));
  }

  @Test
  void testQuerySortingByRetriesAsc() {

    // given
    int taskCount = 10;
    List<ExternalTask> list = startProcesses(taskCount);
    reportExternalTaskFailure(list);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .failureLog()
      .orderByRetries()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByRetries());
  }

  @Test
  void testQuerySortingByRetriesDsc() {

    // given
    int taskCount = 10;
    List<ExternalTask> list = startProcesses(taskCount);
    reportExternalTaskFailure(list);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .failureLog()
      .orderByRetries()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByRetries()));
  }

  @Test
  void testQuerySortingByPriorityAsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByPriority()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByPriority());
  }

  @Test
  void testQuerySortingByPriorityDsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByPriority()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByPriority()));
  }

  @Test
  void testQuerySortingByTopicNameAsc() {

    // given
    int taskCount = 10;
    startProcessesByTopicName(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByTopicName()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByTopicName());
  }

  @Test
  void testQuerySortingByTopicNameDsc() {

    // given
    int taskCount = 10;
    startProcessesByTopicName(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByTopicName()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByTopicName()));
  }

  @Test
  void testQuerySortingByWorkerIdAsc() {

    // given
    int taskCount = 10;
    List<ExternalTask> list = startProcesses(taskCount);
    completeExternalTasksWithWorkers(list);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .successLog()
      .orderByWorkerId()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByWorkerId());
  }

  @Test
  void testQuerySortingByWorkerIdDsc() {

    // given
    int taskCount = 10;
    List<ExternalTask> list = startProcesses(taskCount);
    completeExternalTasksWithWorkers(list);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .successLog()
      .orderByWorkerId()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByWorkerId()));
  }

  @Test
  void testQuerySortingByActivityIdAsc() {

    // given
    int taskCount = 10;
    startProcessesByActivityId(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByActivityId()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByActivityId());
  }

  @Test
  void testQuerySortingByActivityIdDsc() {

    // given
    int taskCount = 10;
    startProcessesByActivityId(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByActivityId()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByActivityId()));
  }

  @Test
  void testQuerySortingByActivityInstanceIdAsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByActivityInstanceId()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByActivityInstanceId());
  }

  @Test
  void testQuerySortingByActivityInstanceIdDsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByActivityInstanceId()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByActivityInstanceId()));
  }

  @Test
  void testQuerySortingByExecutionIdAsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByExecutionId()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByExecutionId());
  }


  @Test
  void testQuerySortingByExecutionIdDsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByExecutionId()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByExecutionId()));
  }

  @Test
  void testQuerySortingByProcessInstanceIdAsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByProcessInstanceId()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByProcessInstanceId());
  }


  @Test
  void testQuerySortingByProcessInstanceIdDsc() {

    // given
    int taskCount = 10;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByProcessInstanceId()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByProcessInstanceId()));
  }

  @Test
  void testQuerySortingByProcessDefinitionIdAsc() {

    // given
    int taskCount = 8;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByProcessDefinitionId()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByProcessDefinitionId());
  }


  @Test
  void testQuerySortingByProcessDefinitionIdDsc() {

    // given
    int taskCount = 8;
    startProcesses(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByProcessDefinitionId()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByProcessDefinitionId()));
  }

  @Test
  void testQuerySortingByProcessDefinitionKeyAsc() {

    // given
    int taskCount = 10;
    startProcessesByProcessDefinitionKey(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByProcessDefinitionKey()
      .asc();

    // then
    verifyQueryWithOrdering(query, taskCount, historicExternalTaskLogByProcessDefinitionKey(engineRule.getProcessEngine()));
  }


  @Test
  void testQuerySortingByProcessDefinitionKeyDsc() {

    // given
    int taskCount = 10;
    startProcessesByProcessDefinitionKey(taskCount);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();
    query
      .orderByProcessDefinitionKey()
      .desc();

    // then
    verifyQueryWithOrdering(query, taskCount, inverted(historicExternalTaskLogByProcessDefinitionKey(engineRule.getProcessEngine())));
  }

  // helper ------------------------------------

  protected void completeExternalTasksWithWorkers(List<ExternalTask> taskLIst) {
    for (Integer i=0; i<taskLIst.size(); i++) {
      completeExternalTaskWithWorker(taskLIst.get(i).getId(), i.toString());
    }
  }

  protected void completeExternalTaskWithWorker(String externalTaskId, String workerId) {
    completeExternalTask(externalTaskId, DEFAULT_TOPIC, workerId, false);

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

  protected void reportExternalTaskFailure(List<ExternalTask> taskLIst) {
    for (Integer i=0; i<taskLIst.size(); i++) {
      reportExternalTaskFailure(taskLIst.get(i).getId(), DEFAULT_TOPIC, WORKER_ID, i+1, false, "foo");
    }
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

  protected List<ExternalTask> startProcesses(int count) {
    List<ExternalTask> list = new LinkedList<>();
    for (int ithPrio = 0; ithPrio < count; ithPrio++) {
      list.add(startExternalTaskProcessGivenPriority(ithPrio));
      ensureEnoughTimePassedByForTimestampOrdering();
    }
    return list;
  }

  protected List<ExternalTask> startProcessesByTopicName(int count) {
    List<ExternalTask> list = new LinkedList<>();
    for (Integer ithTopic = 0; ithTopic < count; ithTopic++) {
      list.add(startExternalTaskProcessGivenTopicName(ithTopic.toString()));
    }
    return list;
  }

  protected List<ExternalTask> startProcessesByActivityId(int count) {
    List<ExternalTask> list = new LinkedList<>();
    for (Integer ithTopic = 0; ithTopic < count; ithTopic++) {
      list.add(startExternalTaskProcessGivenActivityId("Activity" + ithTopic.toString()));
    }
    return list;
  }

  protected List<ExternalTask> startProcessesByProcessDefinitionKey(int count) {
    List<ExternalTask> list = new LinkedList<>();
    for (Integer ithTopic = 0; ithTopic < count; ithTopic++) {
      list.add(startExternalTaskProcessGivenProcessDefinitionKey("ProcessKey" + ithTopic.toString()));
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

  protected void verifyQueryWithOrdering(HistoricExternalTaskLogQuery query, int countExpected, NullTolerantComparator<HistoricExternalTaskLog> expectedOrdering) {
    assertThat(query.list()).hasSize(countExpected);
    assertThat(query.count()).isEqualTo(countExpected);
    TestOrderingUtil.verifySorting(query.list(), expectedOrdering);
  }

  protected void ensureEnoughTimePassedByForTimestampOrdering() {
    long timeToAddInSeconds = 5 * 1000L;
    Date nowPlus5Seconds = new Date(ClockUtil.getCurrentTime().getTime() + timeToAddInSeconds);
    ClockUtil.setCurrentTime(nowPlus5Seconds);
  }

}
