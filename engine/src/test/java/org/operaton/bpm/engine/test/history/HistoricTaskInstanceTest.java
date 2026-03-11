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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.*;


/**
 * @author Tom Baeyens
 * @author Frederik Heremans
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
@ExtendWith(ProcessEngineExtension.class)
class HistoricTaskInstanceTest {

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  RepositoryService repositoryService;
  CaseService caseService;

  @Deployment
  @Test
  void testHistoricTaskInstance() throws Exception {
    String processInstanceId = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest").getId();

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    // Set priority to non-default value
    Task runtimeTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
    runtimeTask.setPriority(1234);

    // Set due-date
    Date dueDate = sdf.parse("01/02/2003 04:05:06");
    runtimeTask.setDueDate(dueDate);
    taskService.saveTask(runtimeTask);

    String taskId = runtimeTask.getId();
    String taskDefinitionKey = runtimeTask.getTaskDefinitionKey();

    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(historicTaskInstance.getId()).isEqualTo(taskId);
    assertThat(historicTaskInstance.getPriority()).isEqualTo(1234);
    assertThat(historicTaskInstance.getName()).isEqualTo("Clean up");
    assertThat(historicTaskInstance.getDescription()).isEqualTo("Schedule an engineering meeting for next week with the new hire.");
    assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
    assertThat(historicTaskInstance.getAssignee()).isEqualTo("kermit");
    assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo(taskDefinitionKey);
    assertThat(historicTaskInstance.getEndTime()).isNull();
    assertThat(historicTaskInstance.getDurationInMillis()).isNull();

    assertThat(historicTaskInstance.getCaseDefinitionId()).isNull();
    assertThat(historicTaskInstance.getCaseInstanceId()).isNull();
    assertThat(historicTaskInstance.getCaseExecutionId()).isNull();
    assertThat(historicTaskInstance.getTaskState()).isEqualTo("Updated");

    // the activity instance id is set
    assertThat(historicTaskInstance.getActivityInstanceId()).isEqualTo(((TaskEntity) runtimeTask).getExecution().getActivityInstanceId());

    runtimeService.setVariable(processInstanceId, "deadline", "yesterday");

    // move clock by 1 second
    Date now = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(new Date(now.getTime() + 1000));

    taskService.complete(taskId);

    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isOne();

    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(historicTaskInstance.getId()).isEqualTo(taskId);
    assertThat(historicTaskInstance.getPriority()).isEqualTo(1234);
    assertThat(historicTaskInstance.getName()).isEqualTo("Clean up");
    assertThat(historicTaskInstance.getDescription()).isEqualTo("Schedule an engineering meeting for next week with the new hire.");
    assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
    assertThat(historicTaskInstance.getAssignee()).isEqualTo("kermit");
    assertThat(historicTaskInstance.getDeleteReason()).isEqualTo(TaskEntity.DELETE_REASON_COMPLETED);
    assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo(taskDefinitionKey);
    assertThat(historicTaskInstance.getEndTime()).isNotNull();
    assertThat(historicTaskInstance.getDurationInMillis()).isNotNull();
    assertThat(historicTaskInstance.getDurationInMillis()).isGreaterThanOrEqualTo(1000);
    assertThat(((HistoricTaskInstanceEntity) historicTaskInstance).getDurationRaw()).isGreaterThanOrEqualTo(1000);
    assertThat(historicTaskInstance.getTaskState()).isEqualTo("Completed");


    assertThat(historicTaskInstance.getCaseDefinitionId()).isNull();
    assertThat(historicTaskInstance.getCaseInstanceId()).isNull();
    assertThat(historicTaskInstance.getCaseExecutionId()).isNull();

    historyService.deleteHistoricTaskInstance(taskId);

    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isZero();
  }

  @Test
  void testDeleteHistoricTaskInstance() {
    // deleting unexisting historic task instance should be silently ignored
    assertThatCode(() -> historyService.deleteHistoricTaskInstance("unexistingId")).doesNotThrowAnyException();
  }

  @Deployment
  @Test
  void testHistoricTaskInstanceQuery() throws Exception {
    // First instance is finished
    ProcessInstance finishedInstance = runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");

    // Set priority to non-default value
    Task task = taskService.createTaskQuery().processInstanceId(finishedInstance.getId()).singleResult();
    task.setPriority(1234);
    Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 04:05:06");
    task.setDueDate(dueDate);

    taskService.saveTask(task);

    // Complete the task
    String taskId = task.getId();
    taskService.complete(taskId);

    // Task id
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId("unexistingtaskid").count()).isZero();

    // Name
    assertThat(historyService.createHistoricTaskInstanceQuery().taskName("Clean_up").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskName("unexistingname").count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("Clean\\_u%").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%lean\\_up").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%lean\\_u%").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%unexistingname%").count()).isZero();


    // Description
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescription("Historic task_description").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescription("unexistingdescription").count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%task\\_description").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("Historic task%").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%task%").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%unexistingdescripton%").count()).isZero();

    // Execution id
    assertThat(historyService.createHistoricTaskInstanceQuery().executionId(finishedInstance.getId()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().executionId("unexistingexecution").count()).isZero();

    // Process instance id
    assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(finishedInstance.getId()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId("unexistingid").count()).isZero();

    // Process definition id
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionId(finishedInstance.getProcessDefinitionId()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionId("unexistingdefinitionid").count()).isZero();

    // Process definition name
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionName("Historic task query test process").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionName("unexistingdefinitionname").count()).isZero();

    // Process definition key
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKey("HistoricTaskQueryTest").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKey("unexistingdefinitionkey").count()).isZero();


    // Assignee
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssignee("ker_mit").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssignee("johndoe").count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%er\\_mit").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("ker\\_mi%").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%er\\_mi%").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%johndoe%").count()).isZero();

    // Delete reason
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDeleteReason(TaskEntity.DELETE_REASON_COMPLETED).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDeleteReason("deleted").count()).isZero();

    // Task definition ID
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("task").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("unexistingkey").count()).isZero();

    // Task priority
    assertThat(historyService.createHistoricTaskInstanceQuery().taskPriority(1234).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskPriority(5678).count()).isZero();


    // Due date
    Calendar anHourAgo = Calendar.getInstance();
    anHourAgo.setTime(dueDate);
    anHourAgo.add(Calendar.HOUR, -1);

    Calendar anHourLater = Calendar.getInstance();
    anHourLater.setTime(dueDate);
    anHourLater.add(Calendar.HOUR, 1);

    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(anHourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(anHourLater.getTime()).count()).isZero();

    // Due date before
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourLater.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourAgo.getTime()).count()).isZero();

    // Due date after
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueAfter(anHourAgo.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueAfter(anHourLater.getTime()).count()).isZero();

    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueBefore(anHourLater.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueBefore(anHourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueAfter(anHourAgo.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueAfter(anHourLater.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourAgo.getTime()).taskDueAfter(anHourLater.getTime()).count()).isZero();

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    // Start/end dates
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedBefore(hourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedBefore(hourFromNow.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedAfter(hourAgo.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().startedBefore(hourFromNow.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().startedBefore(hourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().startedAfter(hourAgo.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().startedAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().startedAfter(hourFromNow.getTime()).startedBefore(hourAgo.getTime()).count()).isZero();

    // Finished and Unfinished - Add anther other instance that has a running task (unfinished)
    runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");

    assertThat(historyService.createHistoricTaskInstanceQuery().finished().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().unfinished().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().unfinished().finished().count()).isZero();
  }

  @Deployment
  @Test
  void testHistoricTaskInstanceQueryByProcessVariableValue() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel >= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      Map<String, Object> variables = new HashMap<>();
      variables.put("hallo", "steffen");

      String processInstanceId = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest", variables).getId();

      Task runtimeTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
      String taskId = runtimeTask.getId();

      HistoricTaskInstance historicTaskInstance = historyService
          .createHistoricTaskInstanceQuery()
          .processVariableValueEquals("hallo", "steffen")
          .singleResult();

      assertThat(historicTaskInstance).isNotNull();
      assertThat(historicTaskInstance.getId()).isEqualTo(taskId);

      taskService.complete(taskId);
      assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).count()).isOne();

      historyService.deleteHistoricTaskInstance(taskId);
      assertThat(historyService.createHistoricTaskInstanceQuery().count()).isZero();
    }
  }

  @Test
  void testHistoricTaskInstanceAssignment() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // task exists & has no assignee:
    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getAssignee()).isNull();
    assertThat(hti.getTaskState()).isEqualTo("Created");

    // assign task to jonny:
    taskService.setAssignee(task.getId(), "jonny");

    // should be reflected in history
    hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getAssignee()).isEqualTo("jonny");
    assertThat(hti.getOwner()).isNull();
    assertThat(hti.getTaskState()).isEqualTo("Updated");

    taskService.deleteTask(task.getId());
    HistoricTaskInstance htiDeleted = historyService
            .createHistoricTaskInstanceQuery()
            .taskId(task.getId())
            .singleResult();
    assertThat(htiDeleted.getTaskState()).isEqualTo("Deleted");

    historyService.deleteHistoricTaskInstance(hti.getId());
  }

  @Deployment
  @Test
  void testHistoricTaskInstanceAssignmentListener() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("assignee", "jonny");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    HistoricActivityInstance hai = historyService.createHistoricActivityInstanceQuery().activityId("task").singleResult();
    assertThat(hai.getAssignee()).isEqualTo("jonny");

    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getAssignee()).isEqualTo("jonny");
    assertThat(hti.getOwner()).isNull();

  }

  @Test
  void testHistoricTaskInstanceOwner() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // task exists & has no owner:
    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getOwner()).isNull();

    // set owner to jonny:
    taskService.setOwner(task.getId(), "jonny");

    // should be reflected in history
    hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getOwner()).isEqualTo("jonny");

    taskService.deleteTask(task.getId());
    historyService.deleteHistoricTaskInstance(hti.getId());
  }

  @Test
  void testHistoricTaskInstancePriority() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // task exists & has normal priority:
    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getPriority()).isEqualTo(Task.PRIORITY_NORMAL);

    // set priority to maximum value:
    taskService.setPriority(task.getId(), Task.PRIORITY_MAXIMUM);

    // should be reflected in history
    hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getPriority()).isEqualTo(Task.PRIORITY_MAXIMUM);

    taskService.deleteTask(task.getId());
    historyService.deleteHistoricTaskInstance(hti.getId());
  }

  @Deployment
  @Test
  void testHistoricTaskInstanceQueryProcessFinished() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoTaskHistoricTaskQueryTest");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Running task on running process should be available
    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isZero();

    // Finished and running task on running process should be available
    taskService.complete(task.getId());
    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isEqualTo(2);
    assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isZero();

    // 2 finished tasks are found for finished process after completing last task of process
    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.complete(task.getId());
    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().processFinished().count()).isZero();
  }

  @Deployment
  @Test
  void testHistoricTaskInstanceQuerySorting() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");

    String taskId = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult().getId();
    taskService.complete(taskId);

    assertThat(historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskName().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskId().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDueDate().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskFollowUpDate().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseDefinitionId().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseInstanceId().asc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseExecutionId().asc().count()).isOne();

    assertThat(historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByExecutionId().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceStartTime().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskName().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskId().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDueDate().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskFollowUpDate().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseDefinitionId().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseInstanceId().desc().count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseExecutionId().desc().count()).isOne();
  }

  @Test
  void testInvalidSorting() {
    // given
    HistoricTaskInstanceQuery historicTaskInstanceQuery1 = historyService.createHistoricTaskInstanceQuery();

    // when/then
    assertThatThrownBy(historicTaskInstanceQuery1::asc)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");

    assertThatThrownBy(historicTaskInstanceQuery1::desc)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");

    var historicTaskInstanceQuery2 = historicTaskInstanceQuery1.orderByProcessInstanceId();
    assertThatThrownBy(historicTaskInstanceQuery2::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Invalid query: call asc() or desc() after using orderByXX(): direction is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  void testHistoricTaskInstanceQueryByFollowUpDate() throws Exception {
    Calendar otherDate = Calendar.getInstance();

    runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");

    // do not find any task instances with follow up date
    assertThat(taskService.createTaskQuery().followUpDate(otherDate.getTime()).count()).isZero();

    Task task = taskService.createTaskQuery().singleResult();

    // set follow-up date on task
    Date followUpDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
    task.setFollowUpDate(followUpDate);
    taskService.saveTask(task);

    // test that follow-up date was written to historic database
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult().getFollowUpDate()).isEqualTo(followUpDate);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpDate(followUpDate).count()).isOne();

    otherDate.setTime(followUpDate);

    otherDate.add(Calendar.YEAR, 1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpDate(otherDate.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpBefore(otherDate.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpAfter(otherDate.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpAfter(otherDate.getTime()).taskFollowUpDate(followUpDate).count()).isZero();

    otherDate.add(Calendar.YEAR, -2);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpAfter(otherDate.getTime()).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpBefore(otherDate.getTime()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult().getFollowUpDate()).isEqualTo(followUpDate);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpBefore(otherDate.getTime()).taskFollowUpDate(followUpDate).count()).isZero();

    taskService.complete(task.getId());

    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult().getFollowUpDate()).isEqualTo(followUpDate);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpDate(followUpDate).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  void testHistoricTaskInstanceQueryByActivityInstanceId() {
    runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");

    String activityInstanceId = historyService.createHistoricActivityInstanceQuery()
        .activityId("task")
        .singleResult()
        .getId();

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
        .activityInstanceIdIn(activityInstanceId);

    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  void testHistoricTaskInstanceQueryByActivityInstanceIds() {
    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");
    ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");

    String activityInstanceId1 = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(pi1.getId())
        .activityId("task")
        .singleResult()
        .getId();

    String activityInstanceId2 = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(pi2.getId())
        .activityId("task")
        .singleResult()
        .getId();

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
        .activityInstanceIdIn(activityInstanceId1, activityInstanceId2);

    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  void testHistoricTaskInstanceQueryByInvalidActivityInstanceId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.activityInstanceIdIn("invalid");
    assertThat(query.count()).isZero();
    String[] values = { "a", null, "b" };

    assertThatThrownBy(() -> query.activityInstanceIdIn(null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query.activityInstanceIdIn((String) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query.activityInstanceIdIn(values)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseDefinitionId() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // then
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionId(caseDefinitionId);

    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();

    HistoricTaskInstance task = query.singleResult();
    assertThat(task).isNotNull();

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  void testQueryByInvalidCaseDefinitionId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

    query.caseDefinitionId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseDefinitionKey() {
    // given
    String key = "oneTaskCase";

    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(key)
        .singleResult()
        .getId();

    String caseInstanceId = caseService
        .withCaseDefinitionByKey(key)
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // then
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionKey(key);

    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();

    HistoricTaskInstance task = query.singleResult();
    assertThat(task).isNotNull();

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  void testQueryByInvalidCaseDefinitionKey() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionKey("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

    query.caseDefinitionKey(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseDefinitionName() {
    // given
    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .singleResult();

    String caseDefinitionName = caseDefinition.getName();
    String caseDefinitionId = caseDefinition.getId();

    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // then
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionName(caseDefinitionName);

    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();

    HistoricTaskInstance task = query.singleResult();
    assertThat(task).isNotNull();

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  void testQueryByInvalidCaseDefinitionName() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionName("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

    query.caseDefinitionName(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseInstanceId() {
    // given
    String key = "oneTaskCase";

    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(key)
        .singleResult()
        .getId();

    String caseInstanceId = caseService
        .withCaseDefinitionByKey(key)
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // then
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseInstanceId(caseInstanceId);

    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();

    HistoricTaskInstance task = query.singleResult();
    assertThat(task).isNotNull();

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }


  @Deployment(resources =
      {
          "org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testQueryByCaseInstanceIdHierarchy.cmmn",
          "org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testQueryByCaseInstanceIdHierarchy.bpmn20.xml"
      })
  @Test
  void testQueryByCaseInstanceIdHierarchy() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    caseService
        .createCaseExecutionQuery()
        .activityId("PI_ProcessTask_1")
        .singleResult()
        .getId();

    // then
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseInstanceId(caseInstanceId);

    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);

    for (HistoricTaskInstance task : query.list()) {
      assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);

      assertThat(task.getCaseDefinitionId()).isNull();
      assertThat(task.getCaseExecutionId()).isNull();

      taskService.complete(task.getId());
    }

    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);

    for (HistoricTaskInstance task : query.list()) {
      assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);

      assertThat(task.getCaseDefinitionId()).isNull();
      assertThat(task.getCaseExecutionId()).isNull();
    }

  }

  @Test
  void testQueryByInvalidCaseInstanceId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseExecutionId() {
    // given
    String key = "oneTaskCase";

    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(key)
        .singleResult()
        .getId();

    String caseInstanceId = caseService
        .withCaseDefinitionByKey(key)
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // then
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseExecutionId(humanTaskId);

    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();

    HistoricTaskInstance task = query.singleResult();
    assertThat(task).isNotNull();

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  void testQueryByInvalidCaseExecutionId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseExecutionId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

    query.caseExecutionId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();

  }

  @Test
  void testHistoricTaskInstanceCaseInstanceId() {
    Task task = taskService.newTask();
    task.setCaseInstanceId("aCaseInstanceId");
    taskService.saveTask(task);

    HistoricTaskInstance hti = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(task.getId())
        .singleResult();

    assertThat(hti.getCaseInstanceId()).isEqualTo("aCaseInstanceId");

    task.setCaseInstanceId("anotherCaseInstanceId");
    taskService.saveTask(task);

    hti = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(task.getId())
        .singleResult();

    assertThat(hti.getCaseInstanceId()).isEqualTo("anotherCaseInstanceId");

    // Finally, delete task
    taskService.deleteTask(task.getId(), true);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessDefinitionKeyProperty() {
    // given
    String key = "oneTaskProcess";
    String processInstanceId = runtimeService.startProcessInstanceByKey(key).getId();

    // when
    HistoricTaskInstance task = historyService
        .createHistoricTaskInstanceQuery()
        .processInstanceId(processInstanceId)
        .taskDefinitionKey("theTask")
        .singleResult();

    // then
    assertThat(task.getProcessDefinitionKey()).isNotNull();
    assertThat(task.getProcessDefinitionKey()).isEqualTo(key);

    assertThat(task.getCaseDefinitionKey()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  void testCaseDefinitionKeyProperty() {
    // given
    String key = "oneTaskCase";
    String caseInstanceId = caseService.createCaseInstanceByKey(key).getId();

    // when
    HistoricTaskInstance task = historyService
        .createHistoricTaskInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .taskDefinitionKey("PI_HumanTask_1")
        .singleResult();

    // then
    assertThat(task.getCaseDefinitionKey()).isNotNull();
    assertThat(task.getCaseDefinitionKey()).isEqualTo(key);

    assertThat(task.getProcessDefinitionKey()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testQueryByTaskDefinitionKey() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    HistoricTaskInstanceQuery query1 = historyService
        .createHistoricTaskInstanceQuery()
        .taskDefinitionKey("theTask");

    HistoricTaskInstanceQuery query2 = historyService
        .createHistoricTaskInstanceQuery()
        .taskDefinitionKeyIn("theTask");

    // then
    assertThat(query1.count()).isOne();
    assertThat(query2.count()).isOne();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testQueryByTaskDefinitionKeys() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    caseService.createCaseInstanceByKey("oneTaskCase");

    // when
    HistoricTaskInstanceQuery query = historyService
        .createHistoricTaskInstanceQuery()
        .taskDefinitionKeyIn("theTask", "PI_HumanTask_1");

    // then
    assertThat(query.count()).isEqualTo(2);
  }

  @Test
  void testQueryByInvalidTaskDefinitionKeys() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.taskDefinitionKeyIn("invalid");
    assertThat(query.count()).isZero();
    String[] values = { "a", null, "b" };

    assertThatThrownBy(() -> query.taskDefinitionKeyIn(null)).isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> query.taskDefinitionKeyIn((String) null)).isInstanceOf(NotValidException.class);

    assertThatThrownBy(() -> query.taskDefinitionKeyIn(values)).isInstanceOf(NotValidException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessInstanceBusinessKey() {
    // given
    ProcessInstance piBusinessKey1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.processInstanceBusinessKey(piBusinessKey1.getBusinessKey()).count()).isOne();
    assertThat(query.processInstanceBusinessKey("unexistingBusinessKey").count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessInstanceBusinessKeyIn() {
    // given
    String businessKey1 = "BUSINESS-KEY-1";
    String businessKey2 = "BUSINESS-KEY-2";
    String businessKey3 = "BUSINESS-KEY-3";
    String unexistingBusinessKey = "unexistingBusinessKey";

    runtimeService.startProcessInstanceByKey("oneTaskProcess", businessKey1);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", businessKey2);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", businessKey3);

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.processInstanceBusinessKeyIn(businessKey1, businessKey2, businessKey3).list()).hasSize(3);
    assertThat(query.processInstanceBusinessKeyIn(businessKey2, unexistingBusinessKey).count()).isOne();
  }

  @Test
  void testQueryByInvalidProcessInstanceBusinessKeyIn() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.processInstanceBusinessKeyIn("invalid");
    assertThat(query.count()).isZero();
    String[] values = { "a", null, "b" };

    assertThatThrownBy(() -> query.processInstanceBusinessKeyIn(null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query.processInstanceBusinessKeyIn((String) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query.processInstanceBusinessKeyIn(values)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessInstanceBusinessKeyLike() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.processInstanceBusinessKeyLike("BUSINESS-KEY-1").list()).hasSize(1);
    assertThat(query.processInstanceBusinessKeyLike("BUSINESS-KEY%").count()).isOne();
    assertThat(query.processInstanceBusinessKeyLike("%KEY-1").count()).isOne();
    assertThat(query.processInstanceBusinessKeyLike("%KEY%").count()).isOne();
    assertThat(query.processInstanceBusinessKeyLike("BUZINESS-KEY%").count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessInstanceBusinessKeyAndArray() {
    // given
    String businessKey1 = "BUSINESS-KEY-1";
    String businessKey2 = "BUSINESS-KEY-2";
    String businessKey3 = "BUSINESS-KEY-3";
    String unexistingBusinessKey = "unexistingBusinessKey";

    runtimeService.startProcessInstanceByKey("oneTaskProcess", businessKey1);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", businessKey2);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", businessKey3);

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.processInstanceBusinessKeyIn(businessKey1, businessKey2).processInstanceBusinessKey(unexistingBusinessKey).count()).isZero();
    assertThat(query.processInstanceBusinessKeyIn(businessKey2, businessKey3).processInstanceBusinessKey(businessKey2).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/history/multiprocess/rootProcess.bpmn",
      "org/operaton/bpm/engine/test/api/history/multiprocess/secondLevelProcess.bpmn",
      "org/operaton/bpm/engine/test/api/history/multiprocess/thirdLevelProcess.bpmn"})
  @Test
  void testQueryByRootProcessInstanceId() {
    // given
    String rootProcessId = runtimeService.startProcessInstanceByKey("root-process").getId();
    runtimeService.startProcessInstanceByKey("process-3");
    while (historyService.createHistoricProcessInstanceQuery().unfinished().count() > 0) {
      taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));
    }
    List<HistoricTaskInstance> allTaskInstances = historyService.createHistoricTaskInstanceQuery().list();

    // when
    HistoricTaskInstanceQuery historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery().rootProcessInstanceId(rootProcessId);

    // then
    assertThat(allTaskInstances).hasSize(8);
    assertThat(allTaskInstances.stream().filter(task -> task.getRootProcessInstanceId().equals(rootProcessId)).count()).isEqualTo(6);
    assertThat(historicTaskInstanceQuery.count()).isEqualTo(6);
    assertThat(historicTaskInstanceQuery.list()).hasSize(6);
  }
}
