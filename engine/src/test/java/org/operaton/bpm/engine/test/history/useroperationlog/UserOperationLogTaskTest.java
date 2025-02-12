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
package org.operaton.bpm.engine.test.history.useroperationlog;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.*;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.ASSIGNEE;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.DELEGATION;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.DELETE;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.OWNER;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.PRIORITY;

import java.util.Date;
import java.util.HashMap;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Danny Gräf
 */
public class UserOperationLogTaskTest extends AbstractUserOperationLogTest {

  protected ProcessDefinition processDefinition;
  protected ProcessInstance process;
  protected Task task;

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testCreateAndCompleteTask() {
    startTestProcess();

    // expect: one entry for process instance creation,
    //         no entry for the task creation by process engine
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isEqualTo(1);

    completeTestProcess();

    // expect: one entry for the task completion
    query = queryOperationDetails(OPERATION_TYPE_COMPLETE);
    assertThat(query.count()).isEqualTo(1);
    UserOperationLogEntry complete = query.singleResult();
    assertThat(complete.getProperty()).isEqualTo(DELETE);
    assertTrue(Boolean.parseBoolean(complete.getNewValue()));
    assertThat(complete.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testAssignTask() {
    startTestProcess();

    // then: assign the task
    taskService.setAssignee(task.getId(), "icke");

    // expect: one entry for the task assignment
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_ASSIGN);
    assertThat(query.count()).isEqualTo(1);

    // assert: details
    UserOperationLogEntry assign = query.singleResult();
    assertThat(assign.getProperty()).isEqualTo(ASSIGNEE);
    assertThat(assign.getNewValue()).isEqualTo("icke");
    assertThat(assign.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    completeTestProcess();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testChangeTaskOwner() {
    startTestProcess();

    // then: change the task owner
    taskService.setOwner(task.getId(), "icke");

    // expect: one entry for the owner change
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_SET_OWNER);
    assertThat(query.count()).isEqualTo(1);

    // assert: details
    UserOperationLogEntry change = query.singleResult();
    assertThat(change.getProperty()).isEqualTo(OWNER);
    assertThat(change.getNewValue()).isEqualTo("icke");
    assertThat(change.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    completeTestProcess();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testSetPriority() {
    startTestProcess();

    // then: set the priority of the task to 10
    taskService.setPriority(task.getId(), 10);

    // expect: one entry for the priority update
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_SET_PRIORITY);
    assertThat(query.count()).isEqualTo(1);

    // assert: correct priority set
    UserOperationLogEntry userOperationLogEntry = query.singleResult();
    assertThat(userOperationLogEntry.getProperty()).isEqualTo(PRIORITY);
    // note: 50 is the default task priority
    assertThat(Integer.parseInt(userOperationLogEntry.getOrgValue())).isEqualTo(50);
    assertThat(Integer.parseInt(userOperationLogEntry.getNewValue())).isEqualTo(10);
    // assert: correct category set
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    // move clock by 5 minutes
    Date date = DateTimeUtil.now().plusMinutes(5).toDate();
    ClockUtil.setCurrentTime(date);

    // then: set priority again
    taskService.setPriority(task.getId(), 75);

    // expect: one entry for the priority update
    query = queryOperationDetails(OPERATION_TYPE_SET_PRIORITY);
    assertThat(query.count()).isEqualTo(2);

    // assert: correct priority set
    userOperationLogEntry = query.orderByTimestamp().asc().list().get(1);
    assertThat(userOperationLogEntry.getProperty()).isEqualTo(PRIORITY);
    assertThat(Integer.parseInt(userOperationLogEntry.getOrgValue())).isEqualTo(10);
    assertThat(Integer.parseInt(userOperationLogEntry.getNewValue())).isEqualTo(75);
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testSetName() {
    // given
    startTestProcess();

    // when
    taskService.setName(task.getId(), "the-name");

    // then
    UserOperationLogQuery query = queryOperationDetails("SetName");

    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry result = query.singleResult();

    assertThat(result.getOperationType()).isEqualTo("SetName");
    assertThat(result.getTaskId()).isEqualTo(task.getId());
    assertThat(result.getProperty()).isEqualTo("name");
    assertThat(result.getNewValue()).isEqualTo("the-name");
    assertThat(result.getEntityType()).isEqualTo("Task");
    assertThat(result.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testSetDescription() {
    // given
    startTestProcess();

    // when
    taskService.setDescription(task.getId(), "the-description");

    // then
    UserOperationLogQuery query = queryOperationDetails("SetDescription");

    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry result = query.singleResult();

    assertThat(result.getOperationType()).isEqualTo("SetDescription");
    assertThat(result.getTaskId()).isEqualTo(task.getId());
    assertThat(result.getProperty()).isEqualTo("description");
    assertThat(result.getNewValue()).isEqualTo("the-description");
    assertThat(result.getEntityType()).isEqualTo("Task");
    assertThat(result.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testSetDueDate() {
    // given
    startTestProcess();

    Date newValue = DateTime.now().toDate();

    // when
    taskService.setDueDate(task.getId(), newValue);

    // then
    UserOperationLogQuery query = queryOperationDetails("SetDueDate");

    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry result = query.singleResult();

    assertThat(result.getOperationType()).isEqualTo("SetDueDate");
    assertThat(result.getTaskId()).isEqualTo(task.getId());
    assertThat(result.getProperty()).isEqualTo("dueDate");
    assertThat(result.getNewValue()).isEqualTo(String.valueOf(newValue.getTime()));
    assertThat(result.getEntityType()).isEqualTo("Task");
    assertThat(result.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void setFollowUpDate() {
    // given
    startTestProcess();

    Date newValue = DateTime.now().toDate();
    // when
    taskService.setFollowUpDate(task.getId(), newValue);

    // then
    UserOperationLogQuery query = queryOperationDetails("SetFollowUpDate");

    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry result = query.singleResult();

    assertThat(result.getOperationType()).isEqualTo("SetFollowUpDate");
    assertThat(result.getTaskId()).isEqualTo(task.getId());
    assertThat(result.getProperty()).isEqualTo("followUpDate");
    assertThat(result.getNewValue()).isEqualTo(String.valueOf(newValue.getTime()));
    assertThat(result.getEntityType()).isEqualTo("Task");
    assertThat(result.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testClaimTask() {
    startTestProcess();

    // then: claim a new the task
    taskService.claim(task.getId(), "icke");

    // expect: one entry for the claim
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_CLAIM);
    assertThat(query.count()).isEqualTo(1);

    // assert: details
    UserOperationLogEntry claim = query.singleResult();
    assertThat(claim.getProperty()).isEqualTo(ASSIGNEE);
    assertThat(claim.getNewValue()).isEqualTo("icke");
    assertThat(claim.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    completeTestProcess();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testDelegateTask() {
    startTestProcess();

    // then: delegate the assigned task
    taskService.claim(task.getId(), "icke");
    taskService.delegateTask(task.getId(), "er");

    // expect: three entries for the delegation
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_DELEGATE);
    assertThat(query.count()).isEqualTo(3);

    // assert: details
    assertThat(queryOperationDetails(OPERATION_TYPE_DELEGATE, OWNER).singleResult().getNewValue()).isEqualTo("icke");
    assertThat(queryOperationDetails(OPERATION_TYPE_DELEGATE, ASSIGNEE).singleResult().getNewValue()).isEqualTo("er");
    assertThat(queryOperationDetails(OPERATION_TYPE_DELEGATE, DELEGATION).singleResult().getNewValue()).isEqualTo(DelegationState.PENDING.toString());
    assertThat(queryOperationDetails(OPERATION_TYPE_DELEGATE, DELEGATION).singleResult().getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    completeTestProcess();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testResolveTask() {
    startTestProcess();

    // then: resolve the task
    taskService.resolveTask(task.getId());

    // expect: one entry for the resolving
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_RESOLVE);
    assertThat(query.count()).isEqualTo(1);

    // assert: details
    UserOperationLogEntry log = query.singleResult();
    assertThat(log.getNewValue()).isEqualTo(DelegationState.RESOLVED.toString());
    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    completeTestProcess();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testSubmitTaskForm_Complete() {
    startTestProcess();

    formService.submitTaskForm(task.getId(), new HashMap<>());

    // expect: one entry for the completion
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_COMPLETE);
    assertThat(query.count()).isEqualTo(1);

    // assert: delete
    UserOperationLogEntry log = query.property("delete").singleResult();
    assertFalse(Boolean.parseBoolean(log.getOrgValue()));
    assertTrue(Boolean.parseBoolean(log.getNewValue()));
    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    testRule.assertProcessEnded(process.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testSubmitTaskForm_Resolve() {
    startTestProcess();

    taskService.delegateTask(task.getId(), "demo");

    formService.submitTaskForm(task.getId(), new HashMap<>());

    // expect: two entries for the resolving (delegation and assignee changed)
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_RESOLVE);
    assertThat(query.count()).isEqualTo(2);

    // assert: delegation
    UserOperationLogEntry log = query.property("delegation").singleResult();
    assertThat(log.getOrgValue()).isEqualTo(DelegationState.PENDING.toString());
    assertThat(log.getNewValue()).isEqualTo(DelegationState.RESOLVED.toString());
    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    // assert: assignee
    log = query.property("assignee").singleResult();
    assertThat(log.getOrgValue()).isEqualTo("demo");
    assertThat(log.getNewValue()).isNull();
    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    completeTestProcess();
  }

  @Test
  public void testDeleteTask() {
    // given
    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    // when
    taskService.deleteTask(newTask.getId());

    // then
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_DELETE);
    assertThat(query.count()).isEqualTo(1);

    // assert: details
    UserOperationLogEntry log = query.singleResult();
    assertThat(log.getProperty()).isEqualTo("delete");
    assertThat(log.getOrgValue()).isEqualTo("false");
    assertThat(log.getNewValue()).isEqualTo("true");
    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    historyService.deleteHistoricTaskInstance(newTask.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testCompleteTask() {
    // given
    startTestProcess();

    // when
    taskService.complete(task.getId());

    // then
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_COMPLETE);
    assertThat(query.count()).isEqualTo(1);

    // assert: details
    UserOperationLogEntry log = query.singleResult();
    assertThat(log.getProperty()).isEqualTo("delete");
    assertThat(log.getOrgValue()).isEqualTo("false");
    assertThat(log.getNewValue()).isEqualTo("true");
    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCompleteCaseExecution() {
    // given
    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .singleResult();

    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinition.getId())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_COMPLETE);

    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry entry = query.singleResult();
    assertNotNull(entry);

    assertThat(entry.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
    assertThat(entry.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(entry.getCaseExecutionId()).isEqualTo(humanTaskId);
    assertThat(entry.getDeploymentId()).isEqualTo(caseDefinition.getDeploymentId());

    assertFalse(Boolean.parseBoolean(entry.getOrgValue()));
    assertTrue(Boolean.parseBoolean(entry.getNewValue()));
    assertThat(entry.getProperty()).isEqualTo(DELETE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testKeepOpLogEntriesOnUndeployment() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    startTestProcess();
    // an op log entry directly related to the process instance is created
    taskService.resolveTask(task.getId());

    // and an op log entry with indirect reference to the process instance is created
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinition.getId());

    // when
    // the deployment is deleted with cascade
    repositoryService.deleteDeployment(deploymentId, true);

    // then
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE).count()).isEqualTo(1);
    assertThat(query.operationType(UserOperationLogEntry.OPERATION_TYPE_SUSPEND).count()).isEqualTo(1);
    assertThat(query.operationType(UserOperationLogEntry.OPERATION_TYPE_RESOLVE).count()).isEqualTo(1);
    assertThat(query.operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE).count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteOpLogEntry() {
    // given
    startTestProcess();

    // an op log instance is created
    taskService.resolveTask(task.getId());
    UserOperationLogEntry opLogEntry = historyService
            .createUserOperationLogQuery()
            .entityType(EntityTypes.TASK)
            .singleResult();

    // when the op log instance is deleted
    historyService.deleteUserOperationLogEntry(opLogEntry.getId());

    // then it should be removed from the database
    assertThat(historyService
        .createUserOperationLogQuery()
        .entityType(EntityTypes.TASK)
        .count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteOpLogEntryWithNullArgument() {
    // given
    startTestProcess();

    // an op log instance is created
    taskService.resolveTask(task.getId());

    // when null is used as deletion parameter
    try {
      historyService.deleteUserOperationLogEntry(null);
      fail("exeception expected");
    } catch (NotValidException e) {
      // then there should be an exception that signals an illegal input
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteOpLogNonExstingEntry() {
    // given
    startTestProcess();

    // an op log instance is created
    taskService.resolveTask(task.getId());
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(2);

    // when a non-existing id is used
    historyService.deleteUserOperationLogEntry("a non existing id");

    // then no op log entry should have been deleted (process instance creation+ resolve task)
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(2);
  }

  @Deployment
  @Test
  public void testOnlyTaskCompletionIsLogged() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    // then
    assertTrue((Boolean) runtimeService.getVariable(processInstanceId, "taskListenerCalled"));
    assertTrue((Boolean) runtimeService.getVariable(processInstanceId, "serviceTaskCalled"));

    // Filter only task entities, as the process start is also recorded
    UserOperationLogQuery query = historyService
            .createUserOperationLogQuery()
            .entityType(EntityTypes.TASK);

    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry log = query.singleResult();
    assertThat(log.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(log.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(log.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(log.getTaskId()).isEqualTo(taskId);
    assertThat(log.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_COMPLETE);
    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  protected void startTestProcess() {
    processDefinition = repositoryService
        .createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();

    process = runtimeService.startProcessInstanceById(processDefinition.getId());
    task = taskService.createTaskQuery().singleResult();
  }

  protected UserOperationLogQuery queryOperationDetails(String type) {
    return historyService.createUserOperationLogQuery().operationType(type);
  }

  protected UserOperationLogQuery queryOperationDetails(String type, String property) {
    return historyService.createUserOperationLogQuery().operationType(type).property(property);
  }

  protected void completeTestProcess() {
    taskService.complete(task.getId());
    testRule.assertProcessEnded(process.getId());
  }

}
