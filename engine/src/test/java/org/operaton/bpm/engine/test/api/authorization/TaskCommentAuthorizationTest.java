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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Permissions.TASK_WORK;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_TASK;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.TASK;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

class TaskCommentAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";

  @Test
  void testDeleteTaskCommentWithoutAuthorization() {
    // given
    createTask(TASK_ID);
    Comment createdComment = createComment(TASK_ID, null, "aComment");
    var createdCommentId = createdComment.getId();

    // when + then
    assertThatThrownBy(() -> taskService.deleteTaskComment(TASK_ID, createdCommentId),
            "It should not be possible to delete a comment.")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(TASK_WORK.getName())
        .hasMessageContaining(TASK_ID)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(TASK.resourceName());

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Test
  void testDeleteTaskComment() {
    // given
    createTask(TASK_ID);
    Comment createdComment = taskService.createComment(TASK_ID, null, "aComment");
    createGrantAuthorization(TASK, TASK_ID, userId, UPDATE);

    // when
    taskService.deleteTaskComment(TASK_ID, createdComment.getId());

    // then
    Comment shouldBeDeleletedComment = taskService.getTaskComment(TASK_ID, createdComment.getId());
    assertThat(shouldBeDeleletedComment).isNull();

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Test
  void testDeleteTaskCommentsWithoutAuthorization() {
    // given
    createTask(TASK_ID);
    createComment(TASK_ID, null, "aComment");

    // when + then
    assertThatThrownBy(() -> taskService.deleteTaskComments(TASK_ID),
            "It should not be possible to delete a comment.")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(TASK_WORK.getName())
        .hasMessageContaining(TASK_ID)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(TASK.resourceName());

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Test
  void testDeleteTaskComments() {
    // given
    createTask(TASK_ID);
    taskService.createComment(TASK_ID, null, "aCommentOne");
    taskService.createComment(TASK_ID, null, "aCommentTwo");

    createGrantAuthorization(TASK, TASK_ID, userId, UPDATE);

    // when
    taskService.deleteTaskComments(TASK_ID);

    // then
    List<Comment> comments = taskService.getTaskComments(TASK_ID);
    assertThat(comments).as("The comments list should be empty").isEqualTo(Collections.emptyList());

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Test
  void testUpdateTaskCommentWithoutAuthorization() {
    // given
    createTask(TASK_ID);
    Comment createdComment = createComment(TASK_ID, null, "originalComment");
    var createdCommentId = createdComment.getId();

    // when + then
    assertThatThrownBy(() -> taskService.updateTaskComment(TASK_ID, createdCommentId, "updateMessage"),
            "It should not be possible to delete a comment.")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(TASK_WORK.getName())
        .hasMessageContaining(TASK_ID)
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(TASK.resourceName());

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Test
  void testUpdateTaskComment() {
    // given
    createTask(TASK_ID);
    String commentMessage = "OriginalCommentMessage";
    String updatedMessage = "UpdatedCommentMessage";
    Comment comment = taskService.createComment(TASK_ID, null, commentMessage);
    createGrantAuthorization(TASK, TASK_ID, userId, UPDATE);

    // when
    taskService.updateTaskComment(TASK_ID, comment.getId(), updatedMessage);

    // then
    List<Comment> comments = taskService.getTaskComments(TASK_ID);
    assertThat(comments).as("The comments list should not be empty").isNotEmpty();
    assertThat(comments.get(0).getFullMessage()).isEqualTo(updatedMessage);

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testDeleteProcessTaskCommentWithoutAuthorization() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(PROCESS_KEY);
    var taskId = selectSingleTask().getId();
    var createdCommentId = createComment(taskId, processInstance.getId(), "aComment").getId();

    // when + then
    assertThatThrownBy(() -> taskService.deleteTaskComment(taskId, createdCommentId),
            "It should not be possible to delete a comment.")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(TASK_WORK.getName())
        .hasMessageContaining(UPDATE_TASK.getName())
        .hasMessageContaining(TASK.resourceName());
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testDeleteProcessTaskComment() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");
    Comment createdComment = taskService.createComment(task.getId(), processInstance.getId(), "aComment");
    createGrantAuthorization(TASK, task.getId(), userId, UPDATE);

    // when
    taskService.deleteTaskComment(task.getId(), createdComment.getId());

    // then
    Comment shouldBeDeleletedComment = taskService.getTaskComment(task.getId(), createdComment.getId());
    assertThat(shouldBeDeleletedComment).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testDeleteProcessTaskCommentsWithoutAuthorization() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    var taskId = task.getId();
    createComment(taskId, processInstance.getId(), "aComment");

    // when + then
    assertThatThrownBy(() -> taskService.deleteTaskComments(taskId),
            "It should not be possible to delete a comment.")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(TASK_WORK.getName())
        .hasMessageContaining(UPDATE_TASK.getName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName())
        .hasMessageContaining(processInstance.getProcessDefinitionKey());
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testDeleteProcessTaskComments() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    taskService.createComment(task.getId(), processInstance.getId(), "aCommentOne");
    taskService.createComment(task.getId(), processInstance.getId(), "aCommentTwo");

    createGrantAuthorization(TASK, task.getId(), userId, UPDATE);

    // when
    taskService.deleteTaskComments(task.getId());

    // then
    List<Comment> comments = taskService.getTaskComments(task.getId());
    assertThat(comments).as("The comments list should be empty").isEqualTo(Collections.emptyList());
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testUpdateProcessTaskCommentWithoutAuthorization() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(PROCESS_KEY);
    var taskId = selectSingleTask().getId();
    var createdCommentId = createComment(taskId, processInstance.getId(), "originalComment").getId();

    // when + then
    assertThatThrownBy(() -> taskService.updateTaskComment(taskId, createdCommentId, "updateMessage"),
            "It should not be possible to delete a comment.")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(TASK_WORK.getName())
        .hasMessageContaining(UPDATE_TASK.getName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(TASK.resourceName())
        .hasMessageContaining(taskId)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testUpdateProcessTaskComment() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(PROCESS_KEY);
    Task task = selectSingleTask();
    task.setAssignee("demo");

    String commentMessage = "OriginalCommentMessage";
    String updatedMessage = "UpdatedCommentMessage";
    Comment comment = taskService.createComment(task.getId(), processInstance.getId(), commentMessage);
    createGrantAuthorization(TASK, task.getId(), userId, UPDATE);

    // when
    taskService.updateTaskComment(task.getId(), comment.getId(), updatedMessage);

    // then
    List<Comment> comments = taskService.getTaskComments(task.getId());
    assertThat(comments).as("The comments list should not be empty").isNotEmpty();
    assertThat(comments.get(0).getFullMessage()).isEqualTo(updatedMessage);
  }

}
