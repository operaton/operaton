/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
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
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;


/**
 * @author Tom Baeyens
 * @author Frederik Heremans
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricTaskInstanceTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testHistoricTaskInstance() throws Exception {
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
    assertNull(historicTaskInstance.getEndTime());
    assertNull(historicTaskInstance.getDurationInMillis());

    assertNull(historicTaskInstance.getCaseDefinitionId());
    assertNull(historicTaskInstance.getCaseInstanceId());
    assertNull(historicTaskInstance.getCaseExecutionId());
    assertThat(historicTaskInstance.getTaskState()).isEqualTo("Updated");

    // the activity instance id is set
    assertThat(historicTaskInstance.getActivityInstanceId()).isEqualTo(((TaskEntity) runtimeTask).getExecution().getActivityInstanceId());

    runtimeService.setVariable(processInstanceId, "deadline", "yesterday");

    // move clock by 1 second
    Date now = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(new Date(now.getTime() + 1000));

    taskService.complete(taskId);

    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);

    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(historicTaskInstance.getId()).isEqualTo(taskId);
    assertThat(historicTaskInstance.getPriority()).isEqualTo(1234);
    assertThat(historicTaskInstance.getName()).isEqualTo("Clean up");
    assertThat(historicTaskInstance.getDescription()).isEqualTo("Schedule an engineering meeting for next week with the new hire.");
    assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
    assertThat(historicTaskInstance.getAssignee()).isEqualTo("kermit");
    assertThat(historicTaskInstance.getDeleteReason()).isEqualTo(TaskEntity.DELETE_REASON_COMPLETED);
    assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo(taskDefinitionKey);
    assertNotNull(historicTaskInstance.getEndTime());
    assertNotNull(historicTaskInstance.getDurationInMillis());
    assertTrue(historicTaskInstance.getDurationInMillis() >= 1000);
    assertTrue(((HistoricTaskInstanceEntity)historicTaskInstance).getDurationRaw() >= 1000);
    assertThat(historicTaskInstance.getTaskState()).isEqualTo("Completed");


    assertNull(historicTaskInstance.getCaseDefinitionId());
    assertNull(historicTaskInstance.getCaseInstanceId());
    assertNull(historicTaskInstance.getCaseExecutionId());

    historyService.deleteHistoricTaskInstance(taskId);

    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);
  }

  @Test
  public void testDeleteHistoricTaskInstance() {
    // deleting unexisting historic task instance should be silently ignored
    assertThatCode(() -> historyService.deleteHistoricTaskInstance("unexistingId")).doesNotThrowAnyException();
  }

  @Deployment
  @Test
  public void testHistoricTaskInstanceQuery() throws Exception {
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
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId("unexistingtaskid").count()).isEqualTo(0);

    // Name
    assertThat(historyService.createHistoricTaskInstanceQuery().taskName("Clean_up").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskName("unexistingname").count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("Clean\\_u%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%lean\\_up").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%lean\\_u%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%unexistingname%").count()).isEqualTo(0);


    // Description
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescription("Historic task_description").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescription("unexistingdescription").count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%task\\_description").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("Historic task%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%task%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%unexistingdescripton%").count()).isEqualTo(0);

    // Execution id
    assertThat(historyService.createHistoricTaskInstanceQuery().executionId(finishedInstance.getId()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().executionId("unexistingexecution").count()).isEqualTo(0);

    // Process instance id
    assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(finishedInstance.getId()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId("unexistingid").count()).isEqualTo(0);

    // Process definition id
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionId(finishedInstance.getProcessDefinitionId()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionId("unexistingdefinitionid").count()).isEqualTo(0);

    // Process definition name
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionName("Historic task query test process").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionName("unexistingdefinitionname").count()).isEqualTo(0);

    // Process definition key
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKey("HistoricTaskQueryTest").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKey("unexistingdefinitionkey").count()).isEqualTo(0);


    // Assignee
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssignee("ker_mit").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssignee("johndoe").count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%er\\_mit").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("ker\\_mi%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%er\\_mi%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%johndoe%").count()).isEqualTo(0);

    // Delete reason
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDeleteReason(TaskEntity.DELETE_REASON_COMPLETED).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDeleteReason("deleted").count()).isEqualTo(0);

    // Task definition ID
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("task").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("unexistingkey").count()).isEqualTo(0);

    // Task priority
    assertThat(historyService.createHistoricTaskInstanceQuery().taskPriority(1234).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskPriority(5678).count()).isEqualTo(0);


    // Due date
    Calendar anHourAgo = Calendar.getInstance();
    anHourAgo.setTime(dueDate);
    anHourAgo.add(Calendar.HOUR, -1);

    Calendar anHourLater = Calendar.getInstance();
    anHourLater.setTime(dueDate);
    anHourLater.add(Calendar.HOUR, 1);

    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(anHourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(anHourLater.getTime()).count()).isEqualTo(0);

    // Due date before
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourLater.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourAgo.getTime()).count()).isEqualTo(0);

    // Due date after
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueAfter(anHourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueAfter(anHourLater.getTime()).count()).isEqualTo(0);

    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueBefore(anHourLater.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueBefore(anHourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueAfter(anHourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).taskDueAfter(anHourLater.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourAgo.getTime()).taskDueAfter(anHourLater.getTime()).count()).isEqualTo(0);

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    // Start/end dates
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedBefore(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().finishedAfter(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().startedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().startedBefore(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().startedAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().startedAfter(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().startedAfter(hourFromNow.getTime()).startedBefore(hourAgo.getTime()).count()).isEqualTo(0);

    // Finished and Unfinished - Add anther other instance that has a running task (unfinished)
    runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");

    assertThat(historyService.createHistoricTaskInstanceQuery().finished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().unfinished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().unfinished().finished().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testHistoricTaskInstanceQueryByProcessVariableValue() {
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

      assertNotNull(historicTaskInstance);
      assertThat(historicTaskInstance.getId()).isEqualTo(taskId);

      taskService.complete(taskId);
      assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).count()).isEqualTo(1);

      historyService.deleteHistoricTaskInstance(taskId);
      assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);
    }
  }

  @Test
  public void testHistoricTaskInstanceAssignment() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // task exists & has no assignee:
    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(hti.getAssignee());
    assertThat(hti.getTaskState()).isEqualTo("Created");

    // assign task to jonny:
    taskService.setAssignee(task.getId(), "jonny");

    // should be reflected in history
    hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getAssignee()).isEqualTo("jonny");
    assertNull(hti.getOwner());
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
  public void testHistoricTaskInstanceAssignmentListener() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("assignee", "jonny");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    HistoricActivityInstance hai = historyService.createHistoricActivityInstanceQuery().activityId("task").singleResult();
    assertThat(hai.getAssignee()).isEqualTo("jonny");

    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getAssignee()).isEqualTo("jonny");
    assertNull(hti.getOwner());

  }

  @Test
  public void testHistoricTaskInstanceOwner() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // task exists & has no owner:
    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(hti.getOwner());

    // set owner to jonny:
    taskService.setOwner(task.getId(), "jonny");

    // should be reflected in history
    hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(hti.getOwner()).isEqualTo("jonny");

    taskService.deleteTask(task.getId());
    historyService.deleteHistoricTaskInstance(hti.getId());
  }

  @Test
  public void testHistoricTaskInstancePriority() {
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
  public void testHistoricTaskInstanceQueryProcessFinished() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoTaskHistoricTaskQueryTest");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Running task on running process should be available
    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isEqualTo(0);

    // Finished and running task on running process should be available
    taskService.complete(task.getId());
    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isEqualTo(2);
    assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isEqualTo(0);

    // 2 finished tasks are found for finished process after completing last task of process
    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.complete(task.getId());
    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isEqualTo(2);

    assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().processFinished().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testHistoricTaskInstanceQuerySorting() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");

    String taskId = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult().getId();
    taskService.complete(taskId);

    assertThat(historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskName().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskId().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDueDate().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskFollowUpDate().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseDefinitionId().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseInstanceId().asc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseExecutionId().asc().count()).isEqualTo(1);

    assertThat(historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByExecutionId().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceStartTime().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskName().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskId().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDueDate().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskFollowUpDate().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseDefinitionId().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseInstanceId().desc().count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().orderByCaseExecutionId().desc().count()).isEqualTo(1);
  }

  @Test
  public void testInvalidSorting() {
    HistoricTaskInstanceQuery historicTaskInstanceQuery1 = historyService.createHistoricTaskInstanceQuery();
    try {
      historicTaskInstanceQuery1.asc();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");
    }

    try {
      historicTaskInstanceQuery1.desc();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");
    }

    var historicTaskInstanceQuery2 = historicTaskInstanceQuery1.orderByProcessInstanceId();
    try {
      historicTaskInstanceQuery2.list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid query: call asc() or desc() after using orderByXX(): direction is null");
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  public void testHistoricTaskInstanceQueryByFollowUpDate() throws Exception {
    Calendar otherDate = Calendar.getInstance();

    runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");

    // do not find any task instances with follow up date
    assertThat(taskService.createTaskQuery().followUpDate(otherDate.getTime()).count()).isEqualTo(0);

    Task task = taskService.createTaskQuery().singleResult();

    // set follow-up date on task
    Date followUpDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
    task.setFollowUpDate(followUpDate);
    taskService.saveTask(task);

    // test that follow-up date was written to historic database
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult().getFollowUpDate()).isEqualTo(followUpDate);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpDate(followUpDate).count()).isEqualTo(1);

    otherDate.setTime(followUpDate);

    otherDate.add(Calendar.YEAR, 1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpDate(otherDate.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpBefore(otherDate.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpAfter(otherDate.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpAfter(otherDate.getTime()).taskFollowUpDate(followUpDate).count()).isEqualTo(0);

    otherDate.add(Calendar.YEAR, -2);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpAfter(otherDate.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpBefore(otherDate.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult().getFollowUpDate()).isEqualTo(followUpDate);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpBefore(otherDate.getTime()).taskFollowUpDate(followUpDate).count()).isEqualTo(0);

    taskService.complete(task.getId());

    assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult().getFollowUpDate()).isEqualTo(followUpDate);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskFollowUpDate(followUpDate).count()).isEqualTo(1);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  public void testHistoricTaskInstanceQueryByActivityInstanceId() {
    runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");

    String activityInstanceId = historyService.createHistoricActivityInstanceQuery()
        .activityId("task")
        .singleResult()
        .getId();

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
        .activityInstanceIdIn(activityInstanceId);

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list().size()).isEqualTo(1);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  public void testHistoricTaskInstanceQueryByActivityInstanceIds() {
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
    assertThat(query.list().size()).isEqualTo(2);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testHistoricTaskInstance.bpmn20.xml"})
  @Test
  public void testHistoricTaskInstanceQueryByInvalidActivityInstanceId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.activityInstanceIdIn("invalid");
    assertThat(query.count()).isEqualTo(0);
    String[] values = { "a", null, "b" };

    try {
      query.activityInstanceIdIn(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      query.activityInstanceIdIn((String)null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      query.activityInstanceIdIn(values);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testQueryByCaseDefinitionId() {
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

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list().size()).isEqualTo(1);
    assertNotNull(query.singleResult());

    HistoricTaskInstance task = query.singleResult();
    assertNotNull(task);

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  public void testQueryByInvalidCaseDefinitionId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionId("invalid");

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

    query.caseDefinitionId(null);

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testQueryByCaseDefinitionKey() {
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

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list().size()).isEqualTo(1);
    assertNotNull(query.singleResult());

    HistoricTaskInstance task = query.singleResult();
    assertNotNull(task);

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  public void testQueryByInvalidCaseDefinitionKey() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionKey("invalid");

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

    query.caseDefinitionKey(null);

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testQueryByCaseDefinitionName() {
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

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list().size()).isEqualTo(1);
    assertNotNull(query.singleResult());

    HistoricTaskInstance task = query.singleResult();
    assertNotNull(task);

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  public void testQueryByInvalidCaseDefinitionName() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseDefinitionName("invalid");

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

    query.caseDefinitionName(null);

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testQueryByCaseInstanceId() {
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

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list().size()).isEqualTo(1);
    assertNotNull(query.singleResult());

    HistoricTaskInstance task = query.singleResult();
    assertNotNull(task);

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }



  @Deployment(resources=
    {
      "org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testQueryByCaseInstanceIdHierarchy.cmmn",
      "org/operaton/bpm/engine/test/history/HistoricTaskInstanceTest.testQueryByCaseInstanceIdHierarchy.bpmn20.xml"
    })
  @Test
  public void testQueryByCaseInstanceIdHierarchy() {
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
    assertThat(query.list().size()).isEqualTo(2);

    for (HistoricTaskInstance task : query.list()) {
      assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);

      assertNull(task.getCaseDefinitionId());
      assertNull(task.getCaseExecutionId());

      taskService.complete(task.getId());
    }

    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list().size()).isEqualTo(3);

    for (HistoricTaskInstance task : query.list()) {
      assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);

      assertNull(task.getCaseDefinitionId());
      assertNull(task.getCaseExecutionId());
    }

  }

  @Test
  public void testQueryByInvalidCaseInstanceId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseInstanceId("invalid");

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

    query.caseInstanceId(null);

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testQueryByCaseExecutionId() {
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

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list().size()).isEqualTo(1);
    assertNotNull(query.singleResult());

    HistoricTaskInstance task = query.singleResult();
    assertNotNull(task);

    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  public void testQueryByInvalidCaseExecutionId() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.caseExecutionId("invalid");

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

    query.caseExecutionId(null);

    assertThat(query.count()).isEqualTo(0);
    assertThat(query.list().size()).isEqualTo(0);
    assertNull(query.singleResult());

  }

  @Test
  public void testHistoricTaskInstanceCaseInstanceId() {
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
  public void testProcessDefinitionKeyProperty() {
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
    assertNotNull(task.getProcessDefinitionKey());
    assertThat(task.getProcessDefinitionKey()).isEqualTo(key);

    assertNull(task.getCaseDefinitionKey());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  public void testCaseDefinitionKeyProperty() {
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
    assertNotNull(task.getCaseDefinitionKey());
    assertThat(task.getCaseDefinitionKey()).isEqualTo(key);

    assertNull(task.getProcessDefinitionKey());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  public void testQueryByTaskDefinitionKey() {
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
    assertThat(query1.count()).isEqualTo(1);
    assertThat(query2.count()).isEqualTo(1);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  public void testQueryByTaskDefinitionKeys() {
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
  public void testQueryByInvalidTaskDefinitionKeys() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.taskDefinitionKeyIn("invalid");
    assertThat(query.count()).isEqualTo(0);
    String[] values = { "a", null, "b" };

    try {
      query.taskDefinitionKeyIn(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (NotValidException e) {
      // expected
    }

    try {
      query.taskDefinitionKeyIn((String)null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (NotValidException e) {
      // expected
    }

    try {
      query.taskDefinitionKeyIn(values);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (NotValidException e) {
      // expected
    }

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testQueryByProcessInstanceBusinessKey() {
    // given
    ProcessInstance piBusinessKey1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.processInstanceBusinessKey(piBusinessKey1.getBusinessKey()).count()).isEqualTo(1);
    assertThat(query.processInstanceBusinessKey("unexistingBusinessKey").count()).isEqualTo(0);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testQueryByProcessInstanceBusinessKeyIn() {
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
    assertThat(query.processInstanceBusinessKeyIn(businessKey1, businessKey2, businessKey3).list().size()).isEqualTo(3);
    assertThat(query.processInstanceBusinessKeyIn(businessKey2, unexistingBusinessKey).count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidProcessInstanceBusinessKeyIn() {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    query.processInstanceBusinessKeyIn("invalid");
    assertThat(query.count()).isEqualTo(0);
    String[] values = { "a", null, "b" };

    try {
      query.processInstanceBusinessKeyIn(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      query.processInstanceBusinessKeyIn((String)null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      query.processInstanceBusinessKeyIn(values);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testQueryByProcessInstanceBusinessKeyLike() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

    // then
    assertThat(query.processInstanceBusinessKeyLike("BUSINESS-KEY-1").list().size()).isEqualTo(1);
    assertThat(query.processInstanceBusinessKeyLike("BUSINESS-KEY%").count()).isEqualTo(1);
    assertThat(query.processInstanceBusinessKeyLike("%KEY-1").count()).isEqualTo(1);
    assertThat(query.processInstanceBusinessKeyLike("%KEY%").count()).isEqualTo(1);
    assertThat(query.processInstanceBusinessKeyLike("BUZINESS-KEY%").count()).isEqualTo(0);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testQueryByProcessInstanceBusinessKeyAndArray() {
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
    assertThat(query.processInstanceBusinessKeyIn(businessKey1, businessKey2).processInstanceBusinessKey(unexistingBusinessKey).count()).isEqualTo(0);
    assertThat(query.processInstanceBusinessKeyIn(businessKey2, businessKey3).processInstanceBusinessKey(businessKey2).count()).isEqualTo(1);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/history/multiprocess/rootProcess.bpmn",
      "org/operaton/bpm/engine/test/api/history/multiprocess/secondLevelProcess.bpmn",
      "org/operaton/bpm/engine/test/api/history/multiprocess/thirdLevelProcess.bpmn" })
  @Test
  public void testQueryByRootProcessInstanceId() {
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
    assertThat(allTaskInstances.size()).isEqualTo(8);
    assertThat(allTaskInstances.stream().filter(task -> task.getRootProcessInstanceId().equals(rootProcessId)).count()).isEqualTo(6);
    assertThat(historicTaskInstanceQuery.count()).isEqualTo(6);
    assertThat(historicTaskInstanceQuery.list().size()).isEqualTo(6);
  }
}
