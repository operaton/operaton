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
package org.operaton.bpm.engine.test.standalone.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricTaskInstanceQueryOrTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  HistoryService historyService;
  RuntimeService runtimeService;
  TaskService taskService;
  CaseService caseService;
  RepositoryService repositoryService;
  FilterService filterService;
  HistoricTaskInstanceQuery historicTaskInstanceQueryStartWithOr;

  @BeforeEach
  void init() {
    historicTaskInstanceQueryStartWithOr = historyService.createHistoricTaskInstanceQuery().or();
  }

  @AfterEach
  void tearDown() {
    for (org.operaton.bpm.engine.repository.Deployment deployment:
      repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

    for (HistoricTaskInstance task: historyService.createHistoricTaskInstanceQuery().list()) {
      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  void shouldThrowExceptionByMissingStartOr() {
    // given
    HistoricTaskInstanceQuery query = historicTaskInstanceQueryStartWithOr
      .endOr();

    // when/then
    assertThatThrownBy(query::endOr)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set endOr() before or()");
  }

  @Test
  void shouldThrowExceptionByNesting() {
    // when/then
    assertThatThrownBy(() ->historicTaskInstanceQueryStartWithOr.or())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set or() within 'or' query");
  }

  @Test
  void shouldThrowExceptionByWithCandidateGroupsApplied() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.withCandidateGroups())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set withCandidateGroups() within 'or' query");
  }

  @Test
  void shouldThrowExceptionByWithoutCandidateGroupsApplied() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.withoutCandidateGroups())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set withoutCandidateGroups() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTenantId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTenantId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTenantId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByHistoricActivityInstanceId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByHistoricActivityInstanceId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByHistoricActivityInstanceId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByProcessDefinitionId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByProcessDefinitionId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessDefinitionId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByProcessInstanceId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByProcessInstanceId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessInstanceId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByExecutionId() {
    // when/then
    assertThatThrownBy(() ->historicTaskInstanceQueryStartWithOr.orderByExecutionId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByExecutionId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByHistoricTaskInstanceDuration() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByHistoricTaskInstanceDuration())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByHistoricTaskInstanceDuration() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByHistoricTaskInstanceEndTime() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByHistoricTaskInstanceEndTime())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByHistoricTaskInstanceEndTime() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByHistoricActivityInstanceStartTime() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByHistoricTaskInstanceEndTime())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByHistoricTaskInstanceEndTime() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskName() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskName())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskName() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskDescription() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskDescription())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskDescription() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskAssignee() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskAssignee())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskAssignee() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskOwner() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskOwner())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskOwner() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskDueDate() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskDueDate())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskDueDate() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskFollowUpDate() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskFollowUpDate())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskFollowUpDate() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByDeleteReason() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByDeleteReason())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByDeleteReason() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByTaskDefinitionKey() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskDefinitionKey())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskDefinitionKey() within 'or' query");

  }

  @Test
  void shouldThrowExceptionOnOrderByTaskPriority() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByTaskPriority())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTaskPriority() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByCaseDefinitionId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByCaseDefinitionId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByCaseDefinitionId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByCaseInstanceId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByCaseInstanceId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByCaseInstanceId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByCaseExecutionId() {
    // when/then
    assertThatThrownBy(() -> historicTaskInstanceQueryStartWithOr.orderByCaseExecutionId())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByCaseExecutionId() within 'or' query");
  }

  @Test
  void shouldReturnHistoricTasksWithEmptyOrQuery() {
    // given
    taskService.saveTask(taskService.newTask());
    taskService.saveTask(taskService.newTask());

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void shouldReturnHistoricTasksWithTaskNameOrTaskDescription() {
    // given
    Task task1 = taskService.newTask();
    task1.setName("aTaskName");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setDescription("aTaskDescription");
    taskService.saveTask(task2);

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .taskName("aTaskName")
        .taskDescription("aTaskDescription")
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void shouldReturnHistoricTasksWithMultipleOrCriteria() {
    // given
    Task task1 = taskService.newTask();
    task1.setName("aTaskName");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setDescription("aTaskDescription");
    taskService.saveTask(task2);

    Task task3 = taskService.newTask();
    taskService.saveTask(task3);

    Task task4 = taskService.newTask();
    task4.setPriority(5);
    taskService.saveTask(task4);

    Task task5 = taskService.newTask();
    task5.setOwner("aTaskOwner");
    taskService.saveTask(task5);

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .taskName("aTaskName")
        .taskDescription("aTaskDescription")
        .taskId(task3.getId())
        .taskPriority(5)
        .taskOwner("aTaskOwner")
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(5);
  }

  @Test
  void shouldReturnHistoricTasksFilteredByMultipleOrAndCriteria() {
    // given
    Task task1 = taskService.newTask();
    task1.setPriority(4);
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setName("aTaskName");
    task2.setOwner("aTaskOwner");
    task2.setAssignee("aTaskAssignee");
    task2.setPriority(4);
    taskService.saveTask(task2);

    Task task3 = taskService.newTask();
    task3.setName("aTaskName");
    task3.setOwner("aTaskOwner");
    task3.setAssignee("aTaskAssignee");
    task3.setPriority(4);
    task3.setDescription("aTaskDescription");
    taskService.saveTask(task3);

    Task task4 = taskService.newTask();
    task4.setOwner("aTaskOwner");
    task4.setAssignee("aTaskAssignee");
    task4.setPriority(4);
    task4.setDescription("aTaskDescription");
    taskService.saveTask(task4);

    Task task5 = taskService.newTask();
    task5.setDescription("aTaskDescription");
    task5.setOwner("aTaskOwner");
    taskService.saveTask(task5);

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .taskName("aTaskName")
        .taskDescription("aTaskDescription")
        .taskId(task3.getId())
      .endOr()
      .taskOwner("aTaskOwner")
      .taskPriority(4)
      .taskAssignee("aTaskAssignee")
      .list();

    // then
    assertThat(tasks).hasSize(3);
  }

  @Test
  void shouldReturnHistoricTasksFilteredByMultipleOrQueries() {
    // given
    Task task1 = taskService.newTask();
    task1.setName("aTaskName");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setName("aTaskName");
    task2.setDescription("aTaskDescription");
    taskService.saveTask(task2);

    Task task3 = taskService.newTask();
    task3.setName("aTaskName");
    task3.setDescription("aTaskDescription");
    task3.setOwner("aTaskOwner");
    taskService.saveTask(task3);

    Task task4 = taskService.newTask();
    task4.setName("aTaskName");
    task4.setDescription("aTaskDescription");
    task4.setOwner("aTaskOwner");
    task4.setAssignee("aTaskAssignee");
    taskService.saveTask(task4);

    Task task5 = taskService.newTask();
    task5.setName("aTaskName");
    task5.setDescription("aTaskDescription");
    task5.setOwner("aTaskOwner");
    task5.setAssignee("aTaskAssignee");
    task5.setPriority(4);
    taskService.saveTask(task5);

    Task task6 = taskService.newTask();
    task6.setName("aTaskName");
    task6.setDescription("aTaskDescription");
    task6.setOwner("aTaskOwner");
    task6.setAssignee("aTaskAssignee");
    task6.setPriority(4);
    taskService.saveTask(task6);

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .taskName("aTaskName")
        .taskDescription("aTaskDescription")
      .endOr()
      .or()
        .taskName("aTaskName")
        .taskDescription("aTaskDescription")
        .taskAssignee("aTaskAssignee")
      .endOr()
      .or()
        .taskName("aTaskName")
        .taskDescription("aTaskDescription")
        .taskOwner("aTaskOwner")
        .taskAssignee("aTaskAssignee")
      .endOr()
      .or()
        .taskAssignee("aTaskAssignee")
        .taskPriority(4)
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(3);
  }

  @Test
  void shouldReturnHistoricTasksWhereSameCriterionWasAppliedThreeTimesInOneQuery() {
    // given
    Task task1 = taskService.newTask();
    task1.setName("task1");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setName("task2");
    taskService.saveTask(task2);

    Task task3 = taskService.newTask();
    task3.setName("task3");
    taskService.saveTask(task3);

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .taskName("task1")
        .taskName("task2")
        .taskName("task3")
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(1);
  }

  @Test
  void shouldReturnHistoricTasksWithActivityInstanceIdInOrTaskId() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("aProcessDefinition")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    repositoryService
      .createDeployment()
      .addModelInstance("foo.bpmn", aProcessDefinition)
      .deploy();

    ProcessInstance processInstance1 = runtimeService
      .startProcessInstanceByKey("aProcessDefinition");

    String activityInstanceId = runtimeService.getActivityInstance(processInstance1.getId())
      .getChildActivityInstances()[0].getId();

    Task task2 = taskService.newTask();
    taskService.saveTask(task2);

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .activityInstanceIdIn(activityInstanceId)
        .taskId(task2.getId())
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void shouldTestDueDateCombinations() throws ParseException {
    HashMap<String, Date> dates = createFollowUpAndDueDateTasks();
    taskService.saveTask(taskService.newTask());

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueDate(dates.get("date"))
        .taskDueBefore(dates.get("oneHourAgo"))
        .endOr()
        .count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueDate(dates.get("date"))
        .taskDueBefore(dates.get("oneHourAgo"))
        .withoutTaskDueDate()
        .endOr()
        .count()).isEqualTo(3);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueDate(dates.get("date"))
        .taskDueAfter(dates.get("oneHourLater"))
        .endOr()
        .count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueDate(dates.get("date"))
        .taskDueAfter(dates.get("oneHourLater"))
        .withoutTaskDueDate()
        .endOr()
        .count()).isEqualTo(3);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueBefore(dates.get("oneHourAgo"))
        .taskDueAfter(dates.get("oneHourLater"))
        .endOr()
        .count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueBefore(dates.get("oneHourAgo"))
        .taskDueAfter(dates.get("oneHourLater"))
        .withoutTaskDueDate()
        .endOr()
        .count()).isEqualTo(3);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueBefore(dates.get("oneHourLater"))
        .taskDueAfter(dates.get("oneHourAgo"))
        .endOr()
        .count()).isEqualTo(3);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueBefore(dates.get("oneHourLater"))
        .taskDueAfter(dates.get("oneHourAgo"))
        .withoutTaskDueDate()
        .endOr()
        .count()).isEqualTo(4);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueDate(dates.get("date"))
        .taskDueBefore(dates.get("oneHourAgo"))
        .taskDueAfter(dates.get("oneHourLater"))
        .endOr()
        .count()).isEqualTo(3);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskDueDate(dates.get("date"))
        .taskDueBefore(dates.get("oneHourAgo"))
        .taskDueAfter(dates.get("oneHourLater"))
        .withoutTaskDueDate()
        .endOr()
        .count()).isEqualTo(4);
  }

  @Test
  void shouldTestFollowUpDateCombinations() throws ParseException {
    HashMap<String, Date> dates = createFollowUpAndDueDateTasks();

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskFollowUpDate(dates.get("date"))
        .taskFollowUpBefore(dates.get("oneHourAgo"))
        .endOr()
        .count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskFollowUpDate(dates.get("date"))
        .taskFollowUpAfter(dates.get("oneHourLater"))
        .endOr()
        .count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskFollowUpBefore(dates.get("oneHourAgo"))
        .taskFollowUpAfter(dates.get("oneHourLater"))
        .endOr()
        .count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskFollowUpBefore(dates.get("oneHourLater"))
        .taskFollowUpAfter(dates.get("oneHourAgo"))
        .endOr()
        .count()).isEqualTo(3);

    assertThat(historyService.createHistoricTaskInstanceQuery()
        .or()
        .taskFollowUpDate(dates.get("date"))
        .taskFollowUpBefore(dates.get("oneHourAgo"))
        .taskFollowUpAfter(dates.get("oneHourLater"))
        .endOr()
        .count()).isEqualTo(3);

    // followUp before or null
    taskService.saveTask(taskService.newTask());

    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(4);
  }

  @Test
  void shouldQueryStartedBeforeOrAfter() {
    // given
    Date dateOne = new Date(1363607000000L);
    ClockUtil.setCurrentTime(dateOne);

    Task taskOne = taskService.newTask();
    taskService.saveTask(taskOne);

    Date dateTwo = new Date(dateOne.getTime() + 7000000);
    ClockUtil.setCurrentTime(dateTwo);

    Task taskTwo = taskService.newTask();
    taskService.saveTask(taskTwo);

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
        .or()
          .startedBefore(new Date(dateOne.getTime() + 1000))
          .startedAfter(new Date(dateTwo.getTime() - 1000))
        .endOr()
        .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldQueryStandaloneOrEmbeddedTaskByProcessDefinitionKey() {
    // given
    Task taskOne = taskService.newTask();
    taskService.saveTask(taskOne);

    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
        .or()
          .taskId(taskOne.getId())
          .processDefinitionKey("oneTaskProcess")
        .endOr()
        .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldQueryStandaloneOrEmbeddedTaskByProcessInstanceId() {
    // given
    Task taskOne = taskService.newTask();
    taskService.saveTask(taskOne);

    runtimeService.startProcessInstanceByKey("oneTaskProcess", "aBusinessKey");

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
        .or()
          .taskId(taskOne.getId())
          .processInstanceBusinessKey("aBusinessKey")
        .endOr()
        .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  void shouldQueryStandaloneOrEmbeddedTaskByCaseDefinitionId() {
    // given
    Task taskOne = taskService.newTask();
    taskService.saveTask(taskOne);

    caseService.createCaseInstanceByKey("oneTaskCase");

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
        .or()
          .taskId(taskOne.getId())
          .caseDefinitionKey("oneTaskCase")
        .endOr()
        .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void shouldQueryFinishedBeforeOrAfter() {
    // given
    Date dateOne = new Date(1363607000000L);
    ClockUtil.setCurrentTime(dateOne);

    Task taskOne = taskService.newTask();
    taskService.saveTask(taskOne);
    taskService.complete(taskOne.getId());

    Date dateTwo = new Date(dateOne.getTime() + 7000000);
    ClockUtil.setCurrentTime(dateTwo);

    Task taskTwo = taskService.newTask();
    taskService.saveTask(taskTwo);
    taskService.complete(taskTwo.getId());

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
        .or()
          .finishedBefore(new Date(dateOne.getTime() + 1000))
          .finishedAfter(new Date(dateTwo.getTime() - 1000))
        .endOr()
        .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldReturnHistoricTasksWithHadCandidateUserOrHadCandidateGroup() {
    // given
    Task task1 = taskService.newTask();
    taskService.saveTask(task1);
    taskService.addCandidateUser(task1.getId(), "USER_TEST");

    Task task2 = taskService.newTask();
    taskService.saveTask(task2);
    taskService.addCandidateGroup(task2.getId(), "GROUP_TEST");

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .taskHadCandidateUser("USER_TEST")
        .taskHadCandidateGroup("GROUP_TEST")
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldReturnHistoricTasksWithCandidateCandidateUserInvolvedOrCandidateGroupInvolved() {
    // given
    Task task1 = taskService.newTask();
    taskService.saveTask(task1);
    taskService.addCandidateUser(task1.getId(), "USER_TEST");

    Task task2 = taskService.newTask();
    taskService.saveTask(task2);
    taskService.addCandidateGroup(task2.getId(), "GROUP_TEST");

    // when
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
      .or()
        .taskInvolvedUser("USER_TEST")
        .taskInvolvedGroup("GROUP_TEST")
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  public HashMap<String, Date> createFollowUpAndDueDateTasks() throws ParseException {
    final Date date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("27/07/2017 01:12:13"),
      oneHourAgo = new Date(date.getTime() - 60 * 60 * 1000),
      oneHourLater = new Date(date.getTime() + 60 * 60 * 1000);

    Task taskDueBefore = taskService.newTask();
    taskDueBefore.setFollowUpDate(new Date(oneHourAgo.getTime() - 1000));
    taskDueBefore.setDueDate(new Date(oneHourAgo.getTime() - 1000));
    taskService.saveTask(taskDueBefore);

    Task taskDueDate = taskService.newTask();
    taskDueDate.setFollowUpDate(date);
    taskDueDate.setDueDate(date);
    taskService.saveTask(taskDueDate);

    Task taskDueAfter = taskService.newTask();
    taskDueAfter.setFollowUpDate(new Date(oneHourLater.getTime() + 1000));
    taskDueAfter.setDueDate(new Date(oneHourLater.getTime() + 1000));
    taskService.saveTask(taskDueAfter);

    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(3);

    return new HashMap<>() {{
      put("date", date);
      put("oneHourAgo", oneHourAgo);
      put("oneHourLater", oneHourLater);
    }};
  }
}
