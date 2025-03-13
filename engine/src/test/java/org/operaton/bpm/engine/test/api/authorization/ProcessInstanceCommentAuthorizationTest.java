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
package org.operaton.bpm.engine.test.api.authorization;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessInstanceCommentAuthorizationTest extends AuthorizationTest {
  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceTaskCommentWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createTask(TASK_ID);

    String createdCommentId = createComment(TASK_ID, processInstanceId, "aComment").getId();

    // when
    assertThatThrownBy(() -> taskService.deleteProcessInstanceComment(processInstanceId, createdCommentId))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'UPDATE'");

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceTaskComment() {
    // given
    createTask(TASK_ID);
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    Comment createdComment = taskService.createComment(TASK_ID, processInstanceId, "aComment");
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    taskService.deleteProcessInstanceComment(processInstanceId, createdComment.getId());

    // then
    List<Comment> deletedCommentLst = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(deletedCommentLst).as("The comments list should be empty").isEqualTo(Collections.emptyList());

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceTaskCommentsWithoutAuthorization() {
    // given
    createTask(TASK_ID);
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createComment(TASK_ID, processInstanceId, "aComment");

    try {
      // when
      taskService.deleteProcessInstanceComments(processInstanceId);
      fail("Exception expected: It should not be possible to delete a comment.");
    } catch (AuthorizationException e) {
      // then
      testRule.assertTextPresent("The user with id 'test' does not have one of the following permissions: 'UPDATE'",
          e.getMessage());
    }

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceTaskComments() {
    // given
    createTask(TASK_ID);
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    taskService.createComment(TASK_ID, processInstanceId, "aCommentOne");
    taskService.createComment(TASK_ID, processInstanceId, "aCommentTwo");

    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    taskService.deleteProcessInstanceComments(processInstanceId);

    // then
    List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(comments).as("The comments list should be empty").isEqualTo(Collections.emptyList());

    // triggers a db clean up
    deleteTask(TASK_ID, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testUpdateProcessInstanceTaskCommentWithoutAuthorization() {
    // given
    createTask(TASK_ID);
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    String createdCommentId = createComment(TASK_ID, processInstanceId, "originalComment").getId();

    // when
    assertThatThrownBy(() -> taskService.updateProcessInstanceComment(processInstanceId, createdCommentId, "updateMessage"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'UPDATE'");

    deleteTask(TASK_ID, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testUpdateProcessInstanceTaskComment() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();

    String commentMessage = "OriginalCommentMessage";
    String updatedMessage = "UpdatedCommentMessage";
    Comment comment = taskService.createComment(taskId, processInstanceId, commentMessage);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    taskService.updateProcessInstanceComment(processInstanceId, comment.getId(), updatedMessage);

    // then
    List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(comments).as("The comments list should not be empty").isNotEmpty();
    assertThat(comments.get(0).getFullMessage()).isEqualTo(updatedMessage);

    // triggers a db clean up
    deleteTask(taskId, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceCommentWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    String createdCommentId = createComment(null, processInstanceId, "aComment").getId();

    // when
    assertThatThrownBy(() -> taskService.deleteProcessInstanceComment(processInstanceId, createdCommentId))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'UPDATE'");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceComment() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    Comment createdComment = taskService.createComment(null, processInstanceId, "aComment");
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    taskService.deleteProcessInstanceComment(processInstanceId, createdComment.getId());

    // then
    List<Comment> deletedCommentLst = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(deletedCommentLst).as("The comments list should be empty").isEqualTo(Collections.emptyList());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceCommentsWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createComment(null, processInstanceId, "aComment");

    try {
      // when
      taskService.deleteProcessInstanceComments(processInstanceId);
      fail("Exception expected: It should not be possible to delete a comment.");
    } catch (AuthorizationException e) {
      // then
      testRule.assertTextPresent("The user with id 'test' does not have one of the following permissions: 'UPDATE'",
          e.getMessage());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstanceComments() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    taskService.createComment(null, processInstanceId, "aCommentOne");
    taskService.createComment(null, processInstanceId, "aCommentTwo");

    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    taskService.deleteProcessInstanceComments(processInstanceId);

    // then
    List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(comments).as("The comments list should be empty").isEqualTo(Collections.emptyList());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testUpdateProcessInstanceCommentWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    String createdCommentId = createComment(null, processInstanceId, "originalComment").getId();

    // when
    assertThatThrownBy(() -> taskService.updateProcessInstanceComment(processInstanceId, createdCommentId, "updateMessage"))
      // then
      .withFailMessage("Exception expected: It should not be possible to delete a task.")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("The user with id 'test' does not have one of the following permissions: 'UPDATE'");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testUpdateProcessInstanceComment() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();

    String commentMessage = "OriginalCommentMessage";
    String updatedMessage = "UpdatedCommentMessage";
    Comment comment = taskService.createComment(null, processInstanceId, commentMessage);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    taskService.updateProcessInstanceComment(processInstanceId, comment.getId(), updatedMessage);

    // then
    List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(comments).as("The comments list should not be empty").isNotEmpty();
    assertThat(comments.get(0).getFullMessage()).isEqualTo(updatedMessage);
  }

}
