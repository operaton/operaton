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

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.persistence.entity.CommentEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Event;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.history.useroperationlog.AbstractUserOperationLogTest;

import static org.operaton.bpm.engine.task.Event.ACTION_ADD_ATTACHMENT;
import static org.operaton.bpm.engine.task.Event.ACTION_ADD_GROUP_LINK;
import static org.operaton.bpm.engine.task.Event.ACTION_ADD_USER_LINK;
import static org.operaton.bpm.engine.task.Event.ACTION_DELETE_ATTACHMENT;
import static org.operaton.bpm.engine.task.Event.ACTION_DELETE_GROUP_LINK;
import static org.operaton.bpm.engine.task.Event.ACTION_DELETE_USER_LINK;
import static org.operaton.bpm.engine.task.IdentityLinkType.CANDIDATE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 */
@SuppressWarnings("removal")
class TaskEventsTest extends AbstractUserOperationLogTest {

  static final String JONNY = "jonny";
  static final String ACCOUNTING = "accounting";
  static final String IMAGE_PNG = "application/png";
  static final String IMAGE_NAME = "my-image.png";
  static final String IMAGE_DESC = "a super duper image";
  static final String IMAGE_URL = "file://some/location/my-image.png";

  private Task task;

  @BeforeEach
  void setUp() {
    task = taskService.newTask();
    taskService.saveTask(task);
  }

  @AfterEach
  void tearDown() {
    // delete task
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  void testAddUserLinkEvents() {

    // initially there are no task events
    assertThat(taskService.getTaskEvents(task.getId())).isEmpty();

    taskService.addCandidateUser(task.getId(), JONNY);

    // now there is a task event created
    List<Event> events = taskService.getTaskEvents(task.getId());
    assertThat(events).hasSize(1);
    Event event = events.get(0);
    assertThat(event.getMessageParts().get(0)).isEqualTo(JONNY);
    assertThat(event.getMessageParts().get(1)).isEqualTo(CANDIDATE);
    assertThat(event.getTaskId()).isEqualTo(task.getId());
    assertThat(event.getAction()).isEqualTo(ACTION_ADD_USER_LINK);
    assertThat(event.getMessage()).isEqualTo(JONNY + CommentEntity.MESSAGE_PARTS_MARKER + CANDIDATE);
    assertThat(event.getProcessInstanceId()).isNull();
    assertThat(event.getTime().getTime()).isLessThanOrEqualTo(ClockUtil.getCurrentTime().getTime());

    assertNoCommentsForTask();
  }

  @Test
  void testDeleteUserLinkEvents() {

    // initially there are no task events
    assertThat(taskService.getTaskEvents(task.getId())).isEmpty();

    taskService.addCandidateUser(task.getId(), JONNY);

    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 5000));

    taskService.deleteCandidateUser(task.getId(), JONNY);

    // now there is a task event created
    List<Event> events = taskService.getTaskEvents(task.getId());
    assertThat(events).hasSize(2);
    Event event = events.get(0);
    assertThat(event.getMessageParts().get(0)).isEqualTo(JONNY);
    assertThat(event.getMessageParts().get(1)).isEqualTo(CANDIDATE);
    assertThat(event.getTaskId()).isEqualTo(task.getId());
    assertThat(event.getAction()).isEqualTo(ACTION_DELETE_USER_LINK);
    assertThat(event.getMessage()).isEqualTo(JONNY + CommentEntity.MESSAGE_PARTS_MARKER + CANDIDATE);
    assertThat(event.getProcessInstanceId()).isNull();
    assertThat(event.getTime().getTime()).isLessThanOrEqualTo(ClockUtil.getCurrentTime().getTime());

    assertNoCommentsForTask();
  }

  @Test
  void testAddGroupLinkEvents() {

    // initially there are no task events
    assertThat(taskService.getTaskEvents(task.getId())).isEmpty();

    taskService.addCandidateGroup(task.getId(), ACCOUNTING);

    // now there is a task event created
    List<Event> events = taskService.getTaskEvents(task.getId());
    assertThat(events).hasSize(1);
    Event event = events.get(0);
    assertThat(event.getMessageParts().get(0)).isEqualTo(ACCOUNTING);
    assertThat(event.getMessageParts().get(1)).isEqualTo(CANDIDATE);
    assertThat(event.getTaskId()).isEqualTo(task.getId());
    assertThat(event.getAction()).isEqualTo(ACTION_ADD_GROUP_LINK);
    assertThat(event.getMessage()).isEqualTo(ACCOUNTING + CommentEntity.MESSAGE_PARTS_MARKER + CANDIDATE);
    assertThat(event.getProcessInstanceId()).isNull();
    assertThat(event.getTime().getTime()).isLessThanOrEqualTo(ClockUtil.getCurrentTime().getTime());

    assertNoCommentsForTask();
  }

  @Test
  void testDeleteGroupLinkEvents() {

    // initially there are no task events
    assertThat(taskService.getTaskEvents(task.getId())).isEmpty();

    taskService.addCandidateGroup(task.getId(), ACCOUNTING);

    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 5000));

    taskService.deleteCandidateGroup(task.getId(), ACCOUNTING);

    // now there is a task event created
    List<Event> events = taskService.getTaskEvents(task.getId());
    assertThat(events).hasSize(2);
    Event event = events.get(0);
    assertThat(event.getMessageParts().get(0)).isEqualTo(ACCOUNTING);
    assertThat(event.getMessageParts().get(1)).isEqualTo(CANDIDATE);
    assertThat(event.getTaskId()).isEqualTo(task.getId());
    assertThat(event.getAction()).isEqualTo(ACTION_DELETE_GROUP_LINK);
    assertThat(event.getMessage()).isEqualTo(ACCOUNTING + CommentEntity.MESSAGE_PARTS_MARKER + CANDIDATE);
    assertThat(event.getProcessInstanceId()).isNull();
    assertThat(event.getTime().getTime()).isLessThanOrEqualTo(ClockUtil.getCurrentTime().getTime());

    assertNoCommentsForTask();
  }

  @Test
  void testAddAttachmentEvents() {
    // initially there are no task events
    assertThat(taskService.getTaskEvents(task.getId())).isEmpty();

    identityService.setAuthenticatedUserId(JONNY);
    taskService.createAttachment(IMAGE_PNG, task.getId(), null, IMAGE_NAME, IMAGE_DESC, IMAGE_URL);

    // now there is a task event created
    List<Event> events = taskService.getTaskEvents(task.getId());
    assertThat(events).hasSize(1);
    Event event = events.get(0);
    assertThat(event.getMessageParts()).hasSize(1);
    assertThat(event.getMessageParts().get(0)).isEqualTo(IMAGE_NAME);
    assertThat(event.getTaskId()).isEqualTo(task.getId());
    assertThat(event.getAction()).isEqualTo(ACTION_ADD_ATTACHMENT);
    assertThat(event.getMessage()).isEqualTo(IMAGE_NAME);
    assertThat(event.getProcessInstanceId()).isNull();
    assertThat(event.getTime().getTime()).isLessThanOrEqualTo(ClockUtil.getCurrentTime().getTime());

    assertNoCommentsForTask();
  }

  @Test
  void testDeleteAttachmentEvents() {
    // initially there are no task events
    assertThat(taskService.getTaskEvents(task.getId())).isEmpty();

    identityService.setAuthenticatedUserId(JONNY);
    Attachment attachment = taskService.createAttachment(IMAGE_PNG, task.getId(), null, IMAGE_NAME, IMAGE_DESC, IMAGE_URL);

    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 5000));

    taskService.deleteAttachment(attachment.getId());

    // now there is a task event created
    List<Event> events = taskService.getTaskEvents(task.getId());
    assertThat(events).hasSize(2);
    Event event = events.get(0);
    assertThat(event.getMessageParts()).hasSize(1);
    assertThat(event.getMessageParts().get(0)).isEqualTo(IMAGE_NAME);
    assertThat(event.getTaskId()).isEqualTo(task.getId());
    assertThat(event.getAction()).isEqualTo(ACTION_DELETE_ATTACHMENT);
    assertThat(event.getMessage()).isEqualTo(IMAGE_NAME);
    assertThat(event.getProcessInstanceId()).isNull();
    assertThat(event.getTime().getTime()).isLessThanOrEqualTo(ClockUtil.getCurrentTime().getTime());

    assertNoCommentsForTask();
  }


  private void assertNoCommentsForTask() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      assertThat(commandContext.getCommentManager().findCommentsByTaskId(task.getId())).isEmpty();
      return null;
    });
  }

}
