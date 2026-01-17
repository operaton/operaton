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
package org.operaton.bpm.engine.test.api.encoding;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
@ExtendWith(ProcessEngineExtension.class)
public class ProcessEngineCharacterEncodingTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected TaskService taskService;
  protected Charset defaultCharset;
  protected List<Task> tasks = new ArrayList<>();

  @Parameter(0)
  public Charset charset;

  @Parameters
  public static Collection<Object> scenarios() {
    return List.of(
      StandardCharsets.UTF_8,
      StandardCharsets.UTF_16
    );
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setDefaultCharset(defaultCharset);
    for (Task task : tasks) {
      taskService.deleteTask(task.getId(), true);
    }
  }

  @BeforeEach
  void setUp() {
    defaultCharset = processEngineConfiguration.getDefaultCharset();
    processEngineConfiguration.setDefaultCharset(charset);
  }

  protected Task newTask() {
    Task task = taskService.newTask();
    tasks.add(task);
    taskService.saveTask(task);
    return task;
  }

  protected Task newTaskWithComment(String message) {
    Task task = newTask();
    taskService.createComment(task.getId(), null, message);
    return task;
  }

  protected Comment createNewComment(String taskId, String message) {
    return taskService.createComment(taskId, null, message);
  }

  @TestTemplate
  void shouldPreserveArabicTaskCommentMessageWithCharset() {
    // given
    String message = "این نمونه است";
    Task task = newTaskWithComment(message);

    // when
    List<Comment> taskComments = taskService.getTaskComments(task.getId());

    // then
    assertThat(taskComments).hasSize(1);
    assertThat(taskComments.get(0).getFullMessage()).isEqualTo(message);
  }

  @TestTemplate
  void shouldPreserveLatinTaskCommentMessageWithCharset() {
    // given
    String message = "This is an example";
    Task task = newTaskWithComment(message);

    // when
    List<Comment> taskComments = taskService.getTaskComments(task.getId());

    // then
    assertThat(taskComments).hasSize(1);
    assertThat(taskComments.get(0).getFullMessage()).isEqualTo(message);
  }

  @TestTemplate
  void shouldPreserveArabicTaskUpdateCommentMessageWithCharset() {
    // given
    String taskId = newTask().getId();
    Comment comment = createNewComment(taskId, "OriginalMessage");

    // when
    String updatedMessage = "این نمونه است";
    taskService.updateTaskComment(taskId, comment.getId(), updatedMessage);

    Comment updatedComment = taskService.getTaskComment(taskId, comment.getId());

    // then
    assertThat(updatedComment).isNotNull();
    assertThat(updatedComment.getFullMessage()).isEqualTo(updatedMessage);
  }

  @TestTemplate
  void shouldPreserveLatinTaskUpdateCommentMessageWithCharset() {
    // given
    String taskId = newTask().getId();
    Comment comment = createNewComment(taskId, "OriginalMessage");

    // when
    String updatedMessage = "This is an example";
    taskService.updateTaskComment(taskId, comment.getId(), updatedMessage);

    Comment updatedComment = taskService.getTaskComment(taskId, comment.getId());

    // then
    assertThat(updatedComment).isNotNull();
    assertThat(updatedComment.getFullMessage()).isEqualTo(updatedMessage);
  }
}
