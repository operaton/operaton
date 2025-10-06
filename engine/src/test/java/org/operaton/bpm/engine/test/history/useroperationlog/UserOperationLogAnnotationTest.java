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
package org.operaton.bpm.engine.test.history.useroperationlog;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(HISTORY_FULL)
class UserOperationLogAnnotationTest {

  static final String USER_ID = "demo";
  static final String TASK_ID = "aTaskId";
  static final String ANNOTATION = "anAnnotation";
  static final String TASK_NAME = "aTaskName";
  static final String OPERATION_ID = "operationId";
  static final Date CREATE_TIME = new GregorianCalendar(2013, Calendar.MARCH, 18, 13, 0, 0).getTime();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  TaskService taskService;

  @AfterEach
  void clearDatabase() {
    taskService.deleteTask(TASK_ID, true);
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @BeforeEach
  void setAuthentication() {
    engineRule.getIdentityService()
        .setAuthenticatedUserId(USER_ID);
  }

  @AfterEach
  void clearAuthentication() {
    engineRule.getIdentityService()
        .clearAuthentication();
  }

  @Test
  void shouldSetAnnotation() {
    // given
    createTask();

    UserOperationLogEntry userOperationLogEntry = historyService
        .createUserOperationLogQuery()
        .singleResult();

    // assume
    assertThat(userOperationLogEntry).isNotNull();

    // when
    historyService.setAnnotationForOperationLogById(userOperationLogEntry.getOperationId(), ANNOTATION);

    userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).isEqualTo(ANNOTATION);
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-10664
   */
  @Test
  void shouldSetAnnotation_WithPreservedTimeStamp() {
    // given
    ClockUtil.setCurrentTime(CREATE_TIME);

    createTask();

    UserOperationLogEntry userOperationLogEntry = historyService
        .createUserOperationLogQuery()
        .singleResult();

    // assume
    assertThat(userOperationLogEntry).isNotNull();

    // when
    historyService.setAnnotationForOperationLogById(userOperationLogEntry.getOperationId(), ANNOTATION);

    userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).isEqualTo(ANNOTATION);
    assertThat(userOperationLogEntry.getTimestamp()).isEqualTo(CREATE_TIME);
  }

  @Test
  void shouldSetAnnotationForAllEntries() {
    // given
    Task task = createTask();

    updateMultiplePropertiesOfTask(task);

    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_UPDATE)
        .list();

    // assume
    assertThat(userOperationLogEntries).hasSize(2);

    String operationId = userOperationLogEntries.get(0)
        .getOperationId();

    // when
    historyService.setAnnotationForOperationLogById(operationId, ANNOTATION);

    userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_UPDATE)
        .list();

    // then
    assertThat(userOperationLogEntries.get(0).getAnnotation()).isEqualTo(ANNOTATION);
    assertThat(userOperationLogEntries.get(1).getAnnotation()).isEqualTo(ANNOTATION);
  }

  @Test
  void shouldClearAnnotation() {
    // given
    createTask();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .singleResult();

    // assume
    assertThat(userOperationLogEntry).isNotNull();

    historyService.setAnnotationForOperationLogById(userOperationLogEntry.getOperationId(), ANNOTATION);

    userOperationLogEntry = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE)
        .singleResult();

    assertThat(userOperationLogEntry.getAnnotation()).isEqualTo(ANNOTATION);

    // when
    historyService.clearAnnotationForOperationLogById(userOperationLogEntry.getOperationId());

    userOperationLogEntry = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE)
        .singleResult();

    // then
    assertThat(userOperationLogEntry.getAnnotation()).isNull();
  }

  @Test
  void shouldClearAnnotationForAllEntries() {
    // given
    Task task = createTask();

    updateMultiplePropertiesOfTask(task);

    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_UPDATE)
        .list();

    // assume
    assertThat(userOperationLogEntries).hasSize(2);

    String operationId = userOperationLogEntries.get(0)
        .getOperationId();

    historyService.setAnnotationForOperationLogById(operationId, ANNOTATION);

    userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_UPDATE)
        .list();

    assertThat(userOperationLogEntries.get(0).getAnnotation()).isEqualTo(ANNOTATION);
    assertThat(userOperationLogEntries.get(1).getAnnotation()).isEqualTo(ANNOTATION);

    // when
    historyService.clearAnnotationForOperationLogById(operationId);

    userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_UPDATE)
        .list();

    // then
    assertThat(userOperationLogEntries.get(0).getAnnotation()).isNull();
    assertThat(userOperationLogEntries.get(1).getAnnotation()).isNull();
  }

  @Test
  void shouldWriteOperationLogOnClearAnnotation() {
    // given
    createTask();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .singleResult();

    String operationId = userOperationLogEntry.getOperationId();

    // assume
    assertThat(userOperationLogEntry).isNotNull();

    historyService.setAnnotationForOperationLogById(userOperationLogEntry.getOperationId(), ANNOTATION);

    userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .singleResult();

    assertThat(userOperationLogEntry.getAnnotation()).isEqualTo(ANNOTATION);

    // when
    historyService.clearAnnotationForOperationLogById(userOperationLogEntry.getOperationId());

    userOperationLogEntry = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CLEAR_ANNOTATION)
        .singleResult();

    // then
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.OPERATION_LOG);
    assertThat(userOperationLogEntry.getOperationType())
        .isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CLEAR_ANNOTATION);
    assertThat(userOperationLogEntry.getProperty()).isEqualTo(OPERATION_ID);
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo(operationId);
    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);
  }

  @Test
  void shouldWriteOperationLogOnSetAnnotation() {
    // given
    createTask();

    UserOperationLogEntry userOperationLogEntry = historyService
        .createUserOperationLogQuery()
        .singleResult();

    // assume
    assertThat(userOperationLogEntry).isNotNull();

    // when
    historyService.setAnnotationForOperationLogById(userOperationLogEntry.getOperationId(), OPERATION_ID);

    String operationId = userOperationLogEntry.getOperationId();

    userOperationLogEntry = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.OPERATION_LOG)
        .singleResult();

    // then
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.OPERATION_LOG);
    assertThat(userOperationLogEntry.getOperationType())
        .isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_ANNOTATION);
    assertThat(userOperationLogEntry.getProperty()).isEqualTo(OPERATION_ID);
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo(operationId);
    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);
  }

  @Test
  void shouldThrowExceptionWhenOperationIdNull() {
    // given

    // when/then
    assertThatThrownBy(() -> historyService.setAnnotationForOperationLogById(null, ANNOTATION))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("operation id is null");
  }

  @Test
  void shouldThrowExceptionWhenOperationNull() {
    // given

    // when/then
    assertThatThrownBy(() -> historyService.setAnnotationForOperationLogById("anOperationId", ANNOTATION))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("operation is null");

  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected void updateMultiplePropertiesOfTask(Task task) {
    task.setDueDate(new Date());
    task.setName(TASK_NAME);

    taskService.saveTask(task);
  }

  protected Task createTask() {
    Task task = taskService.newTask(TASK_ID);
    taskService.saveTask(task);
    return task;
  }

}
