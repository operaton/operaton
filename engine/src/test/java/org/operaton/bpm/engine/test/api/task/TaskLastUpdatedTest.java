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
package org.operaton.bpm.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

@Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
@ExtendWith(ProcessEngineExtension.class)
class TaskLastUpdatedTest {

  TaskService taskService;
  RuntimeService runtimeService;
  FormService formService;

  @AfterEach
  void tearDown() {
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      // standalone tasks (deployed process are cleaned up by the engine rule)
      if(task.getProcessDefinitionId() == null) {
        taskService.deleteTask(task.getId(), true);
      }
    }
  }

  // make sure that time passes between two fast operations
  public Date getAfterCurrentTime() {
    return new Date(ClockUtil.getCurrentTime().getTime() + 1000L);
  }

  //make sure that time passes between two fast operations
  public Date getBeforeCurrentTime() {
    return new Date(ClockUtil.getCurrentTime().getTime() - 1000L);
  }

  @Test
  void shouldNotSetLastUpdatedWithoutTaskUpdate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    // no update to task, lastUpdated = null

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult.getLastUpdated()).isNull();
  }

  @Test
  @RequiredDatabase(excludes = {DbSqlSessionFactory.MYSQL})
  void shouldSetLastUpdatedToExactlyNow() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();
    // fix time to one ms after
    Date now = new Date(beforeUpdate.getTime() + 1000L);
    ClockUtil.setCurrentTime(now);

    // when
    taskService.setAssignee(task.getId(), "myself");

    // then
    assertThat(task.getLastUpdated()).isNull();
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskUpdatedAfter(beforeUpdate).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isEqualTo(now);
  }

  @Test
  @RequiredDatabase(includes = {DbSqlSessionFactory.MYSQL})
  void shouldSetLastUpdatedToExactlyNowIgnoringMillis() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();
    // fix time to one ms after
    Date now = new Date(beforeUpdate.getTime() + 1000L);
    ClockUtil.setCurrentTime(now);

    // when
    taskService.setAssignee(task.getId(), "myself");

    // then
    assertThat(task.getLastUpdated()).isNull();
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskUpdatedAfter(beforeUpdate).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isCloseTo(now, 1000);
  }

  @Test
  void shouldSetLastUpdatedOnDescriptionChange() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    task.setDescription("updated");
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.saveTask(task);

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnAssigneeChange() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    task.setAssignee("myself");
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.saveTask(task);

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnVariableChange() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.setVariableLocal(task.getId(), "myVariable", "variableValue");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnCreateAttachment() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.createAttachment(null, task.getId(), processInstance.getId(), "myAttachment", "attachmentDescription", "http://operaton.com");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnChangeAttachment() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Attachment attachment = taskService.createAttachment(null, task.getId(), processInstance.getId(), "myAttachment", "attachmentDescription", "http://operaton.com");
    attachment.setDescription("updatedDescription");
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.saveAttachment(attachment);

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnDeleteAttachment() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Attachment attachment = taskService.createAttachment(null, task.getId(), processInstance.getId(), "myAttachment", "attachmentDescription", "http://operaton.com");
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.deleteAttachment(attachment.getId());

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnDeleteTaskAttachment() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Attachment attachment = taskService.createAttachment(null, task.getId(), processInstance.getId(), "myAttachment", "attachmentDescription", "http://operaton.com");
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.deleteTaskAttachment(task.getId(), attachment.getId());

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnComment() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.createComment(task.getId(), processInstance.getId(), "my comment");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnClaimTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.claim(task.getId(), "myself");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnEveryPropertyChange() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // when
    task.setAssignee("myself");
    taskService.saveTask(task);

    Date expectedBeforeSecondUpdate = getBeforeCurrentTime();

    task.setName("My Task");
    taskService.saveTask(task);

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(expectedBeforeSecondUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnDelegate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // when
    taskService.claim(task.getId(), "myself");
    Date beforeDelegate = getBeforeCurrentTime();
    taskService.delegateTask(task.getId(), "someone");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeDelegate);
  }

  @Test
  void shouldSetLastUpdatedOnResolve() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.resolveTask(task.getId());

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnAddIdentityLink() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.addCandidateUser(task.getId(), "myself");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnDeleteIdentityLink() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.addCandidateUser(task.getId(), "myself");
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.deleteUserIdentityLink(task.getId(), "myself", IdentityLinkType.CANDIDATE);

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnPriorityChange() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.setPriority(task.getId(), 1);

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedOperatonFormSingleTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  void shouldSetLastUpdatedOnSubmitTaskForm() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("FormsProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    // delegate is necessary so that submitting the form does not complete the task
    taskService.delegateTask(task.getId(), "myself");
    Date beforeSubmit = getBeforeCurrentTime();

    // when
    formService.submitTaskForm(task.getId(), null);

    // then
    Task taskResult = taskService.createTaskQuery().singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeSubmit);
  }

  @Test
  void shouldNotSaveTaskConcurrentlyUpdatedByDependentEntity() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.createComment(task.getId(), null, "");

    // when/then
    assertThatThrownBy(() -> taskService.saveTask(task))
      .isInstanceOf(OptimisticLockingException.class);
  }

  @Test
  void shouldSetLastUpdatedOnTaskDeleteComment() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Comment comment = taskService.createComment(task.getId(), processInstance.getId(), "message");

    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.deleteTaskComment(task.getId(), comment.getId());

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnTaskDeleteComments() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.createComment(task.getId(), processInstance.getId(), "message");
    taskService.createComment(task.getId(), null, "message2");

    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.deleteTaskComments(task.getId());

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnTaskCommentUpdate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Comment comment = taskService.createComment(task.getId(), null, "aMessage");

    Date beforeUpdate = getBeforeCurrentTime();

    // when
    taskService.updateTaskComment(task.getId(), comment.getId(), "updatedMessage");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeUpdate);
  }

  @Test
  void shouldSetLastUpdatedOnProcessInstanceDeleteComment() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Comment comment = taskService.createComment(null, processInstance.getId(), "message");

    // when
    taskService.deleteProcessInstanceComment(processInstance.getId(), comment.getId());

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isNull();
  }

  @Test
  void shouldSetLastUpdatedOnProcessInstanceDeleteComments() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    taskService.createComment(null, processInstance.getId(), "message");
    taskService.createComment(null, processInstance.getId(), "message2");

    // when
    taskService.deleteProcessInstanceComments(processInstance.getId());

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isNull();
  }

  @Test
  void shouldSetLastUpdatedOnProcessInstanceCommentUpdate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Comment comment = taskService.createComment(null, processInstance.getId(), "aMessage");

    // when
    taskService.updateProcessInstanceComment(processInstance.getId(), comment.getId(), "updatedMessage");

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isNull();
  }

}
