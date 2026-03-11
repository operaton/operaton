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
package org.operaton.bpm.engine.rest.sub.runtime.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.rest.dto.task.CommentDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.runtime.ProcessInstanceCommentResource;
import org.operaton.bpm.engine.task.Comment;

public class ProcessInstanceCommentResourceImpl implements ProcessInstanceCommentResource {

  private final ProcessEngine engine;
  private final String processInstanceId;

  public ProcessInstanceCommentResourceImpl(ProcessEngine engine, String processInstanceId) {
    this.engine = engine;
    this.processInstanceId = processInstanceId;
  }

  @Override
  public List<CommentDto> getComments() {
    if (!isHistoryEnabled()) {
      return Collections.emptyList();
    }

    ensureProcessInstanceExists(Status.NOT_FOUND);

    List<Comment> processInstanceComments = engine.getTaskService().getProcessInstanceComments(processInstanceId);

    List<CommentDto> comments = new ArrayList<>();
    for (Comment comment : processInstanceComments) {
      comments.add(CommentDto.fromComment(comment));
    }

    return comments;
  }

  /**
   * Deletes a comment by a given commentId
   */
  @Override
  public void deleteComment(String commentId) {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureProcessInstanceExists(Status.NOT_FOUND);

    TaskService taskService = engine.getTaskService();
    try {
      taskService.deleteProcessInstanceComment(processInstanceId, commentId);
    } catch (NullValueException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  /**
   * Updates message for a given processInstanceId and commentId
   */
  @Override
  public void updateComment(CommentDto comment) {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureProcessInstanceExists(Status.NOT_FOUND);

    TaskService taskService = engine.getTaskService();
    try {
      taskService.updateProcessInstanceComment(processInstanceId, comment.getId(), comment.getMessage());
    } catch (NullValueException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  /**
   * Deletes all comments by a given processInstanceId
   */
  @Override
  public void deleteComments() {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureProcessInstanceExists(Status.NOT_FOUND);
    TaskService taskService = engine.getTaskService();

    try {
      taskService.deleteProcessInstanceComments(processInstanceId);
    } catch (NullValueException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  private void ensureHistoryEnabled(Status status) {
    if (!isHistoryEnabled()) {
      throw new InvalidRequestException(status, "History is not enabled");
    }
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

  private void ensureProcessInstanceExists(Status status) {
    HistoricProcessInstance historicProcessInstance = engine.getHistoryService().createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId).singleResult();
    if (historicProcessInstance == null) {
      throw new InvalidRequestException(status, "No process instance found for id %s".formatted(processInstanceId));
    }
  }

}
