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
package org.operaton.bpm.engine.rest.sub.task.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.rest.TaskRestService;
import org.operaton.bpm.engine.rest.dto.task.CommentDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.task.TaskCommentResource;
import org.operaton.bpm.engine.task.Comment;

public class TaskCommentResourceImpl implements TaskCommentResource {

  private final ProcessEngine engine;
  private final String taskId;
  private final String rootResourcePath;

  public TaskCommentResourceImpl(ProcessEngine engine, String taskId, String rootResourcePath) {
    this.engine = engine;
    this.taskId = taskId;
    this.rootResourcePath = rootResourcePath;
  }

  @Override
  public List<CommentDto> getComments() {
    if (!isHistoryEnabled()) {
      return Collections.emptyList();
    }

    ensureTaskExists(Status.NOT_FOUND);

    List<Comment> taskComments = engine.getTaskService().getTaskComments(taskId);

    List<CommentDto> comments = new ArrayList<>();
    for (Comment comment : taskComments) {
      comments.add(CommentDto.fromComment(comment));
    }

    return comments;
  }

  @Override
  public CommentDto getComment(String commentId) {
    ensureHistoryEnabled(Status.NOT_FOUND);

    Comment comment = engine.getTaskService().getTaskComment(taskId, commentId);
    if (comment == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Task comment with id %s does not exist for task id '%s'.".formatted(commentId, taskId));
    }

    return CommentDto.fromComment(comment);
  }

  @Override
  public void deleteComment(String commentId) {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureTaskExists(Status.NOT_FOUND);

    TaskService taskService = engine.getTaskService();
    try {
      taskService.deleteTaskComment(taskId, commentId);
    } catch (NullValueException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public void updateComment(CommentDto comment) {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureTaskExists(Status.NOT_FOUND);

    try {
      engine.getTaskService().updateTaskComment(taskId, comment.getId(), comment.getMessage());
    } catch (NullValueException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public void deleteComments() {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureTaskExists(Status.NOT_FOUND);
    TaskService taskService = engine.getTaskService();

    try {
      taskService.deleteTaskComments(taskId);
    } catch (NullValueException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

    @Override
    public CommentDto createComment(UriInfo uriInfo, CommentDto commentDto) {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureTaskExists(Status.BAD_REQUEST);

    Comment comment;

    String processInstanceId = commentDto.getProcessInstanceId();
    try {
      comment = engine.getTaskService().createComment(taskId, processInstanceId, commentDto.getMessage());
    }
    catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Not enough parameters submitted");
    }

    URI uri = uriInfo.getBaseUriBuilder()
      .path(rootResourcePath)
      .path(TaskRestService.PATH)
      .path(taskId + "/comment/" + comment.getId())
      .build();

    CommentDto resultDto = CommentDto.fromComment(comment);

    // GET /
    resultDto.addReflexiveLink(uri, HttpMethod.GET, "self");

    return resultDto;
  }

  private boolean isHistoryEnabled() {
    IdentityService identityService = engine.getIdentityService();
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    try {
      identityService.clearAuthentication();
      int historyLevel = engine.getManagementService().getHistoryLevel();
      return historyLevel > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE;
    } finally {
      identityService.setAuthentication(currentAuthentication);
    }
  }

  private void ensureHistoryEnabled(Status status) {
    if (!isHistoryEnabled()) {
      throw new InvalidRequestException(status, "History is not enabled");
    }
  }

  private void ensureTaskExists(Status status) {
    HistoricTaskInstance historicTaskInstance = engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
    if (historicTaskInstance == null) {
      throw new InvalidRequestException(status, "No task found for task id %s".formatted(taskId));
    }
  }

}
