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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.rest.TaskRestService;
import org.operaton.bpm.engine.rest.dto.task.AttachmentDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.mapper.MultipartFormData;
import org.operaton.bpm.engine.rest.mapper.MultipartFormData.FormPart;
import org.operaton.bpm.engine.rest.sub.task.TaskAttachmentResource;
import org.operaton.bpm.engine.task.Attachment;

public class TaskAttachmentResourceImpl implements TaskAttachmentResource {

  private final ProcessEngine engine;
  private final String taskId;
  private final String rootResourcePath;

  public TaskAttachmentResourceImpl(ProcessEngine engine, String taskId, String rootResourcePath) {
    this.engine = engine;
    this.taskId = taskId;
    this.rootResourcePath = rootResourcePath;
  }

  @Override
  public List<AttachmentDto> getAttachments() {
    if (!isHistoryEnabled()) {
      return Collections.emptyList();
    }

    ensureTaskExists(Status.NOT_FOUND);

    List<Attachment> taskAttachments = engine.getTaskService().getTaskAttachments(taskId);

    List<AttachmentDto> attachments = new ArrayList<>();
    for (Attachment attachment : taskAttachments) {
      attachments.add(AttachmentDto.fromAttachment(attachment));
    }

    return attachments;
  }

  @Override
  public AttachmentDto getAttachment(String attachmentId) {
    ensureHistoryEnabled(Status.NOT_FOUND);

    Attachment attachment = engine.getTaskService().getTaskAttachment(taskId, attachmentId);

    if (attachment == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Task attachment with id %s does not exist for task id '%s'.".formatted(attachmentId, taskId));
    }

    return AttachmentDto.fromAttachment(attachment);
  }

  @Override
  public InputStream getAttachmentData(String attachmentId) {
    ensureHistoryEnabled(Status.NOT_FOUND);

    InputStream attachmentData = engine.getTaskService().getTaskAttachmentContent(taskId, attachmentId);

    if (attachmentData != null) {
      return attachmentData;
    }
    else {
      throw new InvalidRequestException(Status.NOT_FOUND, "Attachment content for attachment with id '%s' does not exist for task id '%s'.".formatted(attachmentId, taskId));
    }
  }

  @Override
  public void deleteAttachment(String attachmentId) {
    ensureHistoryEnabled(Status.FORBIDDEN);

    try {
      engine.getTaskService().deleteTaskAttachment(taskId, attachmentId);
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Deletion is not possible. No attachment exists for task id '%s' and attachment id '%s'.".formatted(taskId, attachmentId));
    }
  }

  @Override
  public AttachmentDto addAttachment(UriInfo uriInfo, MultipartFormData payload) {
    ensureHistoryEnabled(Status.FORBIDDEN);
    ensureTaskExists(Status.BAD_REQUEST);

    FormPart attachmentNamePart = payload.getNamedPart("attachment-name");
    FormPart attachmentTypePart = payload.getNamedPart("attachment-type");
    FormPart attachmentDescriptionPart = payload.getNamedPart("attachment-description");
    FormPart contentPart = payload.getNamedPart("content");
    FormPart urlPart = payload.getNamedPart("url");

    if (urlPart == null && contentPart == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "No content or url to remote content exists to create the task attachment.");
    }

    String attachmentName = null;
    String attachmentDescription = null;
    String attachmentType = null;
    if (attachmentNamePart != null) {
      attachmentName = attachmentNamePart.getTextContent();
    }
    if (attachmentDescriptionPart != null) {
      attachmentDescription = attachmentDescriptionPart.getTextContent();
    }
    if (attachmentTypePart != null) {
      attachmentType = attachmentTypePart.getTextContent();
    }

    Attachment attachment = null;
    try {
      if (contentPart != null) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contentPart.getBinaryContent());
        attachment = engine.getTaskService().createAttachment(attachmentType, taskId, null, attachmentName, attachmentDescription, byteArrayInputStream);
      } else if (urlPart != null) {
        attachment = engine.getTaskService().createAttachment(attachmentType, taskId, null, attachmentName, attachmentDescription, urlPart.getTextContent());
      }
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Task id is null");
    }

    URI uri = uriInfo.getBaseUriBuilder()
        .path(rootResourcePath)
        .path(TaskRestService.PATH)
        .path(taskId + "/attachment/" + attachment.getId())
        .build();

    AttachmentDto attachmentDto = AttachmentDto.fromAttachment(attachment);

    // GET /
    attachmentDto.addReflexiveLink(uri, HttpMethod.GET, "self");

    return attachmentDto;
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
