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
package org.operaton.bpm.engine.test.api.mgmt.metrics;

import java.time.Instant;
import java.util.Date;
import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMetricsTest {

  protected static final String PROCESS_KEY = "process";
  protected static final BpmnModelInstance USER_TASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_KEY)
      .operatonHistoryTimeToLive(180)
      .startEvent()
      .userTask("task").operatonAssignee("kermit")
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(config -> config.setTaskMetricsEnabled(true))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected TaskService taskService;

  @AfterEach
  void cleanUp() {
    managementService.deleteTaskMetrics(null);
    testRule.deleteAllStandaloneTasks();
  }

  @Test
  void shouldDeleteTaskMetrics() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // assume
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
    // when
    managementService.deleteTaskMetrics(null);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isZero();
  }

  @Test
  void shouldDeleteTaskMetricsWithTimestamp() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // assume
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
    // when
    managementService.deleteTaskMetrics(getOneMinuteFromNow());
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isZero();
  }

  @Test
  void shouldNotDeleteTaskMetricsWithTimestampBefore() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // assume
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
    // when
    managementService.deleteTaskMetrics(getOneMinuteAgo());
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldGetUniqueTaskWorkerCountForSameAssigneeOnDifferentTasksAsOne() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // when a second instance is started
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldGetUniqueTaskWorkerCountWithStartDateInclusive() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    ClockUtil.setCurrentTime(new Date(4000L));
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // when
    ClockUtil.setCurrentTime(new Date(5000L));
    taskService.setAssignee(taskService.createTaskQuery().singleResult().getId(), "gonzo");
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(new Date(5000L), null)).isOne();
    assertThat(managementService.getUniqueTaskWorkerCount(new Date(4000L), null)).isEqualTo(2L);
  }

  @Test
  void shouldGetUniqueTaskWorkerCountWithEndDateExclusive() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    ClockUtil.setCurrentTime(new Date(4000L));
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // when
    ClockUtil.setCurrentTime(new Date(5000L));
    taskService.setAssignee(taskService.createTaskQuery().singleResult().getId(), "gonzo");
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, new Date(5000L))).isOne();
    assertThat(managementService.getUniqueTaskWorkerCount(null, new Date(6000L))).isEqualTo(2L);
  }

  @Test
  void shouldGetUniqueTaskWorkerCountWithoutStartDate() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    // when
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, getOneMinuteFromNow())).isOne();
  }

  @Test
  void shouldGetUniqueTaskWorkerCountWithoutEndDate() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    // when
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(getOneMinuteAgo(), null)).isOne();
  }

  // test without start and end date => shouldCreateTaskMetricWithAssignmentByOperatonAssigneeExtension

  @Test
  void shouldCreateTaskMetricWithAssignmentByTaskListener() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("task").operatonTaskListenerClass("create", AssignmentTaskListener.class)
        .endEvent()
        .done());
    // when
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldCreateTaskMetricWithAssignmentByOperatonAssigneeExtension() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    // when
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  @Deployment
  void shouldCreateTaskMetricWithAssignmentByHumanPerformer() {
    // given a model with human performer
    // when
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldCreateTaskMetricsWithMultipleAssignments() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("task")
          .operatonAssignee("kermit")
          .operatonTaskListenerClass("create", AssignmentTaskListener.class)
        .endEvent()
        .done());
    // when
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isEqualTo(2L);
  }

  @Test
  void shouldCreateTaskMetricOnDelegation() {
    // given
    testRule.deploy(USER_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // when
    taskService.delegateTask(taskId, "gonzo");
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isEqualTo(2L);
  }

  @Test
  void shouldCreateTaskMetricOnClaim() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("task")
        .endEvent()
        .done());
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // when
    taskService.claim(taskId, "gonzo");
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldCreateTaskMetricsWithinErrorMarginWithHigherLoadOfAssignments() {
    // given
    long taskWorkers = 3500L;
    long lowerErrorBoundary = Math.floorDiv((int)(taskWorkers * 90), 100);// 10% off is acceptable
    testRule.deploy(Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("task")
        .endEvent()
        .done());
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // when
    LongStream.range(0L, taskWorkers).forEach(i -> taskService.setAssignee(taskId, "kermit" + i));
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null))
      .isGreaterThan(lowerErrorBoundary)
      .isLessThanOrEqualTo(taskWorkers);
  }

  // Standalone Tasks

  @Test
  void shouldNotCreateTaskMetricForTransientStandaloneTask() {
    // given
    Task newTask = taskService.newTask();
    // when
    newTask.setAssignee("kermit");
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isZero();
  }

  @Test
  void shouldCreateTaskMetricWhenInsertingStandaloneTask() {
    // given
    Task newTask = taskService.newTask();
    newTask.setAssignee("kermit");
    // when
    taskService.saveTask(newTask);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldCreateTaskMetricWhenUpdatingStandaloneTask() {
    // given
    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);
    // when
    newTask.setAssignee("kermit");
    taskService.saveTask(newTask);
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldCreateTaskMetricWhenUpdatingStandaloneTaskInCommandContext() {
    // given
    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);
    // when
    processEngineConfiguration.getCommandExecutorTxRequired().execute(c -> {
      TaskEntity task = c.getTaskManager().findTaskById(newTask.getId());
      task.setAssignee("kermit");
      return null;
    });
    // then
    assertThat(taskService.createTaskQuery().taskId(newTask.getId()).singleResult().getAssignee()).isEqualTo("kermit");
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  @Test
  void shouldCreateTaskMetricForAssignmentOnStandaloneTask() {
    // given
    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);
    // when
    taskService.setAssignee(newTask.getId(), "kermit");
    // then
    assertThat(managementService.getUniqueTaskWorkerCount(null, null)).isOne();
  }

  public static class AssignmentTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
      delegateTask.setAssignee("gonzo");
    }
  }

  protected Date getOneMinuteFromNow() {
    return Date.from(Instant.now().plusSeconds(60));
  }

  protected Date getOneMinuteAgo() {
    return Date.from(Instant.now().minusSeconds(60));
  }
}
