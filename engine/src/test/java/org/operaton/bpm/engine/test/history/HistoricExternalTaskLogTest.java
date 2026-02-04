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
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_TOPIC;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.createDefaultExternalTaskModel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricExternalTaskLogTest {

  protected static final String WORKER_ID = "aWorkerId";
  protected static final String ERROR_MESSAGE = "This is an error!";
  protected static final String ERROR_DETAILS = "These are the error details!";
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

  @AfterEach
  void tearDown() {
    List<ExternalTask> list = externalTaskService.createExternalTaskQuery().workerId(WORKER_ID).list();
    for (ExternalTask externalTask : list) {
      externalTaskService.unlock(externalTask.getId());
    }
  }

  @Test
  void testHistoricExternalTaskLogCreateProperties() {

    // given
    ExternalTask task = startExternalTaskProcess();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .creationLog()
      .singleResult();

    // then
    assertHistoricLogPropertiesAreProperlySet(task, log);
    assertThat(log.getWorkerId()).isNull();
    assertLogIsInCreatedState(log);

  }

  @Test
  void testHistoricExternalTaskLogFailedProperties() {

    // given
    ExternalTask task = startExternalTaskProcess();
    reportExternalTaskFailure(task.getId());
    task = externalTaskService.createExternalTaskQuery().singleResult();

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .singleResult();

    // then
    assertHistoricLogPropertiesAreProperlySet(task, null, log);
    assertThat(log.getWorkerId()).isEqualTo(WORKER_ID);
    assertLogIsInFailedState(log);

  }

  @Test
  void testHistoricExternalTaskLogSuccessfulProperties() {

    // given
    ExternalTask task = startExternalTaskProcess();
    completeExternalTask(task.getId());

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .successLog()
      .singleResult();

    // then
    assertHistoricLogPropertiesAreProperlySet(task, log);
    assertThat(log.getWorkerId()).isEqualTo(WORKER_ID);
    assertLogIsInSuccessfulState(log);

  }

  @Test
  void testHistoricExternalTaskLogDeletedProperties() {

    // given
    ExternalTask task = startExternalTaskProcess();
    runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "Dummy reason for deletion!");

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .deletionLog()
      .singleResult();

    // then
    assertHistoricLogPropertiesAreProperlySet(task, log);
    assertThat(log.getWorkerId()).isNull();
    assertLogIsInDeletedState(log);

  }

  @Test
  void testRetriesAndWorkerIdWhenFirstFailureAndThenComplete() {

    // given
    ExternalTask task = startExternalTaskProcess();
    reportExternalTaskFailure(task.getId());
    completeExternalTask(task.getId());

    // when
    HistoricExternalTaskLog log = historyService
      .createHistoricExternalTaskLogQuery()
      .successLog()
      .singleResult();

    // then
    assertThat(log.getWorkerId()).isEqualTo(WORKER_ID);
    assertThat(log.getRetries()).isEqualTo(Integer.valueOf(1));
    assertLogIsInSuccessfulState(log);
  }

  @Test
  void testErrorDetails() {
    // given
    ExternalTask task = startExternalTaskProcess();
    reportExternalTaskFailure(task.getId());

    // when
    String failedHistoricExternalTaskLogId = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .singleResult()
      .getId();

    // then
    String stacktrace = historyService.getHistoricExternalTaskLogErrorDetails(failedHistoricExternalTaskLogId);
    assertThat(stacktrace).isEqualTo(ERROR_DETAILS);
  }

  @Test
  void testErrorDetailsWithTwoDifferentErrorsThrown() {
    // given
    ExternalTask task = startExternalTaskProcess();
    String firstErrorDetails = "Dummy error details!";
    String secondErrorDetails = ERROR_DETAILS;
    reportExternalTaskFailure(task.getId(), ERROR_MESSAGE, firstErrorDetails);
    ensureEnoughTimePassedByForTimestampOrdering();
    reportExternalTaskFailure(task.getId(), ERROR_MESSAGE, secondErrorDetails);

    // when
    List<HistoricExternalTaskLog> list = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .orderByTimestamp()
      .asc()
      .list();

    String firstFailedLogId = list.get(0).getId();
    String secondFailedLogId = list.get(1).getId();

    // then
    String stacktrace1 = historyService.getHistoricExternalTaskLogErrorDetails(firstFailedLogId);
    String stacktrace2 = historyService.getHistoricExternalTaskLogErrorDetails(secondFailedLogId);
    assertThat(stacktrace1).isNotNull();
    assertThat(stacktrace2).isNotNull();
    assertThat(stacktrace1).isEqualTo(firstErrorDetails);
    assertThat(stacktrace2).isEqualTo(secondErrorDetails);
  }


  @Test
  void testGetExceptionStacktraceForNonexistentExternalTaskId() {
    // when/then
    assertThatThrownBy(() -> historyService.getHistoricExternalTaskLogErrorDetails("foo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No historic external task log found with id foo");
  }

  @Test
  void testGetExceptionStacktraceForNullExternalTaskId() {
    // when/then
    assertThatThrownBy(() -> historyService.getHistoricExternalTaskLogErrorDetails(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historicExternalTaskLogId is null");
  }

  @Test
  void testErrorMessageTruncation() {
    // given
    String exceptionMessage = createStringOfLength(1000);
    ExternalTask task = startExternalTaskProcess();
    reportExternalTaskFailure(task.getId(), exceptionMessage, ERROR_DETAILS);

    // when
    HistoricExternalTaskLog failedLog = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .singleResult();

    String errorMessage = failedLog.getErrorMessage();
    String expectedErrorMessage = exceptionMessage.substring(0, ExternalTaskEntity.MAX_EXCEPTION_MESSAGE_LENGTH);

    // then
    assertThat(failedLog).isNotNull();
    assertThat(errorMessage).hasSize(ExternalTaskEntity.MAX_EXCEPTION_MESSAGE_LENGTH).isEqualTo(expectedErrorMessage);

  }

  // helper

  protected void assertLogIsInCreatedState(HistoricExternalTaskLog log) {
    assertThat(log.isCreationLog()).isTrue();
    assertThat(log.isFailureLog()).isFalse();
    assertThat(log.isSuccessLog()).isFalse();
    assertThat(log.isDeletionLog()).isFalse();
  }

  protected void assertLogIsInFailedState(HistoricExternalTaskLog log) {
    assertThat(log.isCreationLog()).isFalse();
    assertThat(log.isFailureLog()).isTrue();
    assertThat(log.isSuccessLog()).isFalse();
    assertThat(log.isDeletionLog()).isFalse();
  }

  protected void assertLogIsInSuccessfulState(HistoricExternalTaskLog log) {
    assertThat(log.isCreationLog()).isFalse();
    assertThat(log.isFailureLog()).isFalse();
    assertThat(log.isSuccessLog()).isTrue();
    assertThat(log.isDeletionLog()).isFalse();
  }

  protected void assertLogIsInDeletedState(HistoricExternalTaskLog log) {
    assertThat(log.isCreationLog()).isFalse();
    assertThat(log.isFailureLog()).isFalse();
    assertThat(log.isSuccessLog()).isFalse();
    assertThat(log.isDeletionLog()).isTrue();
  }

  protected void assertHistoricLogPropertiesAreProperlySet(ExternalTask task, HistoricExternalTaskLog log) {
    assertHistoricLogPropertiesAreProperlySet(task, task.getRetries(), log);
  }

  protected void assertHistoricLogPropertiesAreProperlySet(ExternalTask task, Integer retries, HistoricExternalTaskLog log) {
    assertThat(log).isNotNull();
    assertThat(log.getId()).isNotNull();
    assertThat(log.getTimestamp()).isNotNull();

    assertThat(log.getExternalTaskId()).isEqualTo(task.getId());
    assertThat(log.getActivityId()).isEqualTo(task.getActivityId());
    assertThat(log.getActivityInstanceId()).isEqualTo(task.getActivityInstanceId());
    assertThat(log.getTopicName()).isEqualTo(task.getTopicName());
    assertThat(log.getRetries()).isEqualTo(retries);
    assertThat(log.getExecutionId()).isEqualTo(task.getExecutionId());
    assertThat(log.getProcessInstanceId()).isEqualTo(task.getProcessInstanceId());
    assertThat(log.getProcessDefinitionId()).isEqualTo(task.getProcessDefinitionId());
    assertThat(log.getProcessDefinitionKey()).isEqualTo(task.getProcessDefinitionKey());
    assertThat(log.getPriority()).isEqualTo(task.getPriority());
  }

  protected void completeExternalTask(String externalTaskId) {
    externalTaskService.fetchAndLock(100, WORKER_ID, false)
      .topic(DEFAULT_TOPIC, LOCK_DURATION)
      .execute();
    externalTaskService.complete(externalTaskId, WORKER_ID);
  }

  protected void reportExternalTaskFailure(String externalTaskId) {
    reportExternalTaskFailure(externalTaskId, ERROR_MESSAGE, ERROR_DETAILS);
  }

  protected void reportExternalTaskFailure(String externalTaskId, String errorMessage, String errorDetails) {
    externalTaskService.fetchAndLock(100, WORKER_ID, false)
      .topic(DEFAULT_TOPIC, LOCK_DURATION)
      .execute();
    externalTaskService.handleFailure(externalTaskId, WORKER_ID, errorMessage, errorDetails, 1, 0L);
  }

  protected ExternalTask startExternalTaskProcess() {
    BpmnModelInstance oneExternalTaskProcess = createDefaultExternalTaskModel().build();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(oneExternalTaskProcess);
    ProcessInstance pi = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    return externalTaskService.createExternalTaskQuery().processInstanceId(pi.getId()).singleResult();
  }

  protected String createStringOfLength(int count) {
    return repeatString(count, "a");
  }

  protected String repeatString(int count, String with) {
    return new String(new char[count]).replace("\0", with);
  }

  protected void ensureEnoughTimePassedByForTimestampOrdering() {
    long timeToAddInSeconds = 5 * 1000L;
    Date nowPlus5Seconds = new Date(ClockUtil.getCurrentTime().getTime() + timeToAddInSeconds);
    ClockUtil.setCurrentTime(nowPlus5Seconds);
  }

}
