/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.cmd;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.AttachmentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.AttachmentManager;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.CommentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.CommentManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricTaskInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TaskArtifactReadAuthorizationTest {

  private static final String TASK_ID = "task";
  private static final String ATTACHMENT_ID = "attachment";
  private static final String COMMENT_ID = "comment";

  private final CommandContext context = mock(CommandContext.class);
  private final TaskManager tasks = mock(TaskManager.class);
  private final AttachmentManager attachments = mock(AttachmentManager.class);
  private final CommentManager comments = mock(CommentManager.class);
  private final HistoricTaskInstanceManager history = mock(HistoricTaskInstanceManager.class);
  private final DbEntityManager entities = mock(DbEntityManager.class);
  private final CommandChecker checker = mock(CommandChecker.class);
  private final CommandChecker tenantChecker = mock(CommandChecker.class);

  @BeforeEach
  void configureContext() {
    when(context.getTaskManager()).thenReturn(tasks);
    when(context.getAttachmentManager()).thenReturn(attachments);
    when(context.getCommentManager()).thenReturn(comments);
    when(context.getHistoricTaskInstanceManager()).thenReturn(history);
    when(context.getDbEntityManager()).thenReturn(entities);
    ProcessEngineConfigurationImpl configuration = mock(ProcessEngineConfigurationImpl.class);
    when(context.getProcessEngineConfiguration()).thenReturn(configuration);
    when(configuration.getCommandCheckers()).thenReturn(List.of(checker, tenantChecker));
  }

  @Test
  void readsAllowedTaskArtifacts() {
    TaskEntity task = mock(TaskEntity.class);
    when(tasks.findTaskById(TASK_ID)).thenReturn(task);

    assertReadsTaskArtifacts();

    verify(checker, times(3)).checkReadTask(task);
    verify(tenantChecker, times(3)).checkReadTask(task);
    verifyNoInteractions(history);
  }

  @Test
  void stopsWhenConfiguredCheckerDeniesTaskRead() {
    TaskEntity task = mock(TaskEntity.class);
    when(tasks.findTaskById(TASK_ID)).thenReturn(task);
    doThrow(new AuthorizationException("Task is not visible")).when(tenantChecker).checkReadTask(task);

    for (Command<?> read : reads()) {
      assertThatThrownBy(() -> read.execute(context)).isInstanceOf(AuthorizationException.class);
    }

    verify(checker, times(3)).checkReadTask(task);
    verifyNoInteractions(attachments, comments, entities, history);
  }

  @Test
  void readsAllowedHistoricTaskArtifacts() {
    when(history.findHistoricTaskInstanceCountByQueryCriteria(argThat(this::isExactTaskQuery))).thenReturn(1L);

    assertReadsTaskArtifacts();

    verify(history, times(3)).findHistoricTaskInstanceCountByQueryCriteria(argThat(this::isExactTaskQuery));
    verifyNoInteractions(checker, tenantChecker);
  }

  @Test
  void returnsNullWhenHistoricTaskIsAbsentOrNotVisible() {
    for (Command<?> read : reads()) {
      assertThat(read.execute(context)).isNull();
    }

    verify(history, times(3)).findHistoricTaskInstanceCountByQueryCriteria(argThat(this::isExactTaskQuery));
    verifyNoInteractions(attachments, comments, entities, checker, tenantChecker);
  }

  @Test
  void returnsNullForMissingArtifactsOnVisibleTask() {
    TaskEntity task = mock(TaskEntity.class);
    when(tasks.findTaskById(TASK_ID)).thenReturn(task);

    for (Command<?> read : reads()) {
      assertThat(read.execute(context)).isNull();
    }

    verifyNoInteractions(entities);
  }

  private boolean isExactTaskQuery(HistoricTaskInstanceQueryImpl query) {
    return TASK_ID.equals(query.getTaskId());
  }

  private void assertReadsTaskArtifacts() {
    AttachmentEntity attachment = mock(AttachmentEntity.class);
    CommentEntity comment = mock(CommentEntity.class);
    ByteArrayEntity payload = mock(ByteArrayEntity.class);
    byte[] content = {1, 2, 3};
    when(attachment.getContentId()).thenReturn("content");
    when(payload.getBytes()).thenReturn(content);
    when(entities.selectById(ByteArrayEntity.class, "content")).thenReturn(payload);
    when(attachments.findAttachmentByTaskIdAndAttachmentId(TASK_ID, ATTACHMENT_ID)).thenReturn(attachment);
    when(comments.findCommentByTaskIdAndCommentId(TASK_ID, COMMENT_ID)).thenReturn(comment);

    assertThat(new GetTaskAttachmentCmd(TASK_ID, ATTACHMENT_ID).execute(context)).isSameAs(attachment);
    assertThat(new GetTaskAttachmentContentCmd(TASK_ID, ATTACHMENT_ID).execute(context)).hasBinaryContent(content);
    assertThat(new GetTaskCommentCmd(TASK_ID, COMMENT_ID).execute(context)).isSameAs(comment);
  }

  private List<Command<?>> reads() {
    return List.of(new GetTaskAttachmentCmd(TASK_ID, ATTACHMENT_ID),
        new GetTaskAttachmentContentCmd(TASK_ID, ATTACHMENT_ID),
        new GetTaskCommentCmd(TASK_ID, COMMENT_ID));
  }
}
