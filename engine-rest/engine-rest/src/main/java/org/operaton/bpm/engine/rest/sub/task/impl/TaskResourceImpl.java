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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Variant;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.form.FormData;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidationException;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.dto.task.*;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.hal.task.HalTask;
import org.operaton.bpm.engine.rest.sub.VariableResource;
import org.operaton.bpm.engine.rest.sub.task.TaskAttachmentResource;
import org.operaton.bpm.engine.rest.sub.task.TaskCommentResource;
import org.operaton.bpm.engine.rest.sub.task.TaskResource;
import org.operaton.bpm.engine.rest.util.ApplicationContextPathUtil;
import org.operaton.bpm.engine.rest.util.ContentTypeUtil;
import org.operaton.bpm.engine.rest.util.EncodingUtil;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.variable.VariableMap;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

public class TaskResourceImpl implements TaskResource {

  private static final List<Variant> VARIANTS = Variant.mediaTypes(MediaType.APPLICATION_JSON_TYPE, Hal.APPLICATION_HAL_JSON_TYPE).add().build();

  protected ProcessEngine engine;
  protected String taskId;
  protected String rootResourcePath;
  protected ObjectMapper objectMapper;
  protected boolean withCommentAttachmentInfo;
  protected boolean withTaskVariablesInReturn;
  protected boolean withTaskLocalVariablesInReturn;

  public TaskResourceImpl(ProcessEngine engine,
                          String taskId,
                          String rootResourcePath,
                          ObjectMapper objectMapper,
                          boolean withCommentAttachmentInfo,
                          boolean withTaskVariablesInReturn,
                          boolean withTaskLocalVariablesInReturn) {
    this.engine = engine;
    this.taskId = taskId;
    this.rootResourcePath = rootResourcePath;
    this.objectMapper = objectMapper;
    this.withCommentAttachmentInfo = withCommentAttachmentInfo;
    this.withTaskVariablesInReturn = withTaskVariablesInReturn;
    this.withTaskLocalVariablesInReturn = withTaskLocalVariablesInReturn;
  }

  @Override
  public void claim(UserIdDto dto) {
    TaskService taskService = engine.getTaskService();

    taskService.claim(taskId, dto.getUserId());
  }

  @Override
  public void unclaim() {
    engine.getTaskService().setAssignee(taskId, null);
  }

  @Override
  public Response complete(CompleteTaskDto dto) {
    TaskService taskService = engine.getTaskService();

    try {
      VariableMap variables = VariableValueDto.toMap(dto.getVariables(), engine, objectMapper);
      if (dto.isWithVariablesInReturn()) {
        VariableMap taskVariables = taskService.completeWithVariablesInReturn(taskId, variables, false);

        Map<String, VariableValueDto> body = VariableValueDto.fromMap(taskVariables, true);

        return Response
            .ok(body)
            .type(MediaType.APPLICATION_JSON)
            .build();
      } else {
        taskService.complete(taskId, variables);
        return Response.noContent().build();
      }

    } catch (RestException e) {
      String errorMessage = "Cannot complete task %s: %s".formatted(taskId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    } catch (AuthorizationException e) {
      throw e;

    } catch (FormFieldValidationException e) {
      String errorMessage = "Cannot complete task %s: %s".formatted(taskId, e.getMessage());
      throw new RestException(Status.BAD_REQUEST, e, errorMessage);

    } catch (ProcessEngineException e) {
      String errorMessage = "Cannot complete task %s: %s".formatted(taskId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }
  }

  @Override
  public Response submit(CompleteTaskDto dto) {
    FormService formService = engine.getFormService();

    try {
      VariableMap variables = VariableValueDto.toMap(dto.getVariables(), engine, objectMapper);
      if (dto.isWithVariablesInReturn()) {
        VariableMap taskVariables = formService.submitTaskFormWithVariablesInReturn(taskId, variables, false);

        Map<String, VariableValueDto> body = VariableValueDto.fromMap(taskVariables, true);
        return Response
            .ok(body)
            .type(MediaType.APPLICATION_JSON)
            .build();
      } else {
        formService.submitTaskForm(taskId, variables);
        return Response.noContent().build();
      }

    } catch (RestException e) {
      String errorMessage = "Cannot submit task form %s: %s".formatted(taskId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    } catch (AuthorizationException e) {
      throw e;

    } catch (FormFieldValidationException e) {
      String errorMessage = "Cannot submit task form %s: %s".formatted(taskId, e.getMessage());
      throw new RestException(Status.BAD_REQUEST, e, errorMessage);

    } catch (ProcessEngineException e) {
      String errorMessage = "Cannot submit task form %s: %s".formatted(taskId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }

  }

  @Override
  public void delegate(UserIdDto delegatedUser) {
    engine.getTaskService().delegateTask(taskId, delegatedUser.getUserId());
  }

  @Override
  public Object getTask(Request request) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(variant.getMediaType())) {
        return getJsonTask();
      }
      else if (Hal.APPLICATION_HAL_JSON_TYPE.equals(variant.getMediaType())) {
        return getHalTask();
      }
    }
    throw new InvalidRequestException(Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  public TaskDto getJsonTask() {
    Task task = getTaskById(taskId, withCommentAttachmentInfo);
    if (task == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "No matching task with id " + taskId);
    }
    if ((withTaskVariablesInReturn || withTaskLocalVariablesInReturn) && withCommentAttachmentInfo) {
      Map<String, VariableValueDto> taskVariables = getTaskVariables(withTaskVariablesInReturn);
      return TaskWithAttachmentAndCommentDto.fromEntity(task, taskVariables);
    }
    if (withCommentAttachmentInfo) {
      return TaskWithAttachmentAndCommentDto.fromEntity(task, null);
    }
    if (withTaskVariablesInReturn || withTaskLocalVariablesInReturn) {
      Map<String, VariableValueDto> taskVariables = getTaskVariables(withTaskVariablesInReturn);
      return TaskWithVariablesDto.fromEntity(task, taskVariables);
    } else {
      return TaskDto.fromEntity(task);
    }
  }

  public HalTask getHalTask() {
    Task task = getTaskById(taskId, withCommentAttachmentInfo);
    if (task == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "No matching task with id " + taskId);
    }

    return HalTask.generate(task, engine);
  }

  @Override
  public FormDto getForm() {
    FormService formService = engine.getFormService();
    Task task = getTaskById(taskId, withCommentAttachmentInfo);
    FormData formData;
    try {
      formData = formService.getTaskFormData(taskId);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new RestException(Status.BAD_REQUEST, e, "Cannot get form for task " + taskId);
    }

    FormDto dto = FormDto.fromFormData(formData);
    if ((dto.getKey() == null || dto.getKey().isEmpty())
      && formData != null
      && formData.getFormFields() != null
      && !formData.getFormFields().isEmpty()) {
      dto.setKey("embedded:engine://engine/:engine/task/%s/rendered-form".formatted(taskId));
    }

    // to get the application context path it is necessary to
    // execute it without authentication (tries to fetch the
    // process definition), because:
    // - user 'demo' has READ permission on a specific task resource
    // - user 'demo' does not have a READ permission on the corresponding
    //   process definition
    // -> running the following lines with authorization would lead
    // to an AuthorizationException because the user 'demo' does not
    // have READ permission on the corresponding process definition
    runWithoutAuthorization(() -> {
      String processDefinitionId = task.getProcessDefinitionId();
      String caseDefinitionId = task.getCaseDefinitionId();
      if (processDefinitionId != null) {
        dto.setContextPath(ApplicationContextPathUtil.getApplicationPathByProcessDefinitionId(engine, processDefinitionId));

      } else if (caseDefinitionId != null) {
        dto.setContextPath(ApplicationContextPathUtil.getApplicationPathByCaseDefinitionId(engine, caseDefinitionId));
      }
      return null;
    });

    return dto;
  }

  @Override
  public Response getRenderedForm() {
    FormService formService = engine.getFormService();

    Object renderedTaskForm = formService.getRenderedTaskForm(taskId);
    if(renderedTaskForm != null) {
      String content = renderedTaskForm.toString();
      InputStream stream = new ByteArrayInputStream(content.getBytes(EncodingUtil.DEFAULT_ENCODING));
      return Response
          .ok(stream)
          .type(MediaType.APPLICATION_XHTML_XML)
          .build();
    }

    throw new InvalidRequestException(Status.NOT_FOUND, "No matching rendered form for task with the id %s found.".formatted(taskId));
  }

  @Override
  public void resolve(CompleteTaskDto dto) {
    TaskService taskService = engine.getTaskService();

    try {
      VariableMap variables = VariableValueDto.toMap(dto.getVariables(), engine, objectMapper);
      taskService.resolveTask(taskId, variables);

    } catch (RestException e) {
      String errorMessage = "Cannot resolve task %s: %s".formatted(taskId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    }

  }

  @Override
  public void setAssignee(UserIdDto dto) {
    TaskService taskService = engine.getTaskService();
    taskService.setAssignee(taskId, dto.getUserId());
  }

  @Override
  public List<IdentityLinkDto> getIdentityLinks(String type) {
    TaskService taskService = engine.getTaskService();
    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);

    List<IdentityLinkDto> result = new ArrayList<>();
    for (IdentityLink link : identityLinks) {
      if (type == null || type.equals(link.getType())) {
        result.add(IdentityLinkDto.fromIdentityLink(link));
      }
    }

    return result;
  }

  @Override
  public void addIdentityLink(IdentityLinkDto identityLink) {
    TaskService taskService = engine.getTaskService();

    identityLink.validate();

    if (identityLink.getUserId() != null) {
      taskService.addUserIdentityLink(taskId, identityLink.getUserId(), identityLink.getType());
    } else if (identityLink.getGroupId() != null) {
      taskService.addGroupIdentityLink(taskId, identityLink.getGroupId(), identityLink.getType());
    }

  }

  @Override
  public void deleteIdentityLink(IdentityLinkDto identityLink) {
    TaskService taskService = engine.getTaskService();

    identityLink.validate();

    if (identityLink.getUserId() != null) {
      taskService.deleteUserIdentityLink(taskId, identityLink.getUserId(), identityLink.getType());
    } else if (identityLink.getGroupId() != null) {
      taskService.deleteGroupIdentityLink(taskId, identityLink.getGroupId(), identityLink.getType());
    }

  }

  @Override
  public TaskCommentResource getTaskCommentResource() {
    return new TaskCommentResourceImpl(engine, taskId, rootResourcePath);
  }

  @Override
  public TaskAttachmentResource getAttachmentResource() {
    return new TaskAttachmentResourceImpl(engine, taskId, rootResourcePath);
  }

  @Override
  public VariableResource getLocalVariables() {
    return new LocalTaskVariablesResource(engine, taskId, objectMapper);
  }

  @Override
  public VariableResource getVariables() {
    return new TaskVariablesResource(engine, taskId, objectMapper);
  }

  @Override
  public Map<String, VariableValueDto> getFormVariables(String variableNames, boolean deserializeValues) {

    final FormService formService = engine.getFormService();
    List<String> formVariables = null;

    if(variableNames != null) {
      StringListConverter stringListConverter = new StringListConverter();
      formVariables = stringListConverter.convertQueryParameterToType(variableNames);
    }

    VariableMap startFormVariables = formService.getTaskFormVariables(taskId, formVariables, deserializeValues);

    return VariableValueDto.fromMap(startFormVariables);
  }

  @Override
  public void updateTask(TaskDto taskDto) {
    TaskService taskService = engine.getTaskService();

    Task task = getTaskById(taskId, withCommentAttachmentInfo);

    if (task == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "No matching task with id " + taskId);
    }

    taskDto.updateTask(task);
    taskService.saveTask(task);
  }

  @Override
  public void deleteTask(String id) {
    TaskService taskService = engine.getTaskService();

    try {
      taskService.deleteTask(id);
    }
    catch (NotValidException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Could not delete task: " + e.getMessage());
    }
  }

  @Override
  public Response getDeployedForm() {
    try {
      InputStream deployedTaskForm = engine.getFormService().getDeployedTaskForm(taskId);
      return Response.ok(deployedTaskForm, getTaskFormMediaType(taskId)).build();
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e.getMessage());
    } catch (NullValueException | BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    } catch (AuthorizationException e) {
      throw new InvalidRequestException(Status.FORBIDDEN, e.getMessage());
    }
  }

  @Override
  public void handleBpmnError(TaskBpmnErrorDto dto) {
    TaskService taskService = engine.getTaskService();

    try {
      taskService.handleBpmnError(taskId, dto.getErrorCode(), dto.getErrorMessage(),
          VariableValueDto.toMap(dto.getVariables(), engine, objectMapper));
    } catch (NotFoundException e) {
      throw new RestException(Status.NOT_FOUND, e, e.getMessage());
    } catch (BadUserRequestException e) {
      throw new RestException(Status.BAD_REQUEST, e, e.getMessage());
    }
  }

  @Override
  public void handleEscalation(TaskEscalationDto dto) {
    TaskService taskService = engine.getTaskService();

    try {
      taskService.handleEscalation(taskId, dto.getEscalationCode(), VariableValueDto.toMap(dto.getVariables(), engine, objectMapper));
    } catch (NotFoundException e) {
      throw new RestException(Status.NOT_FOUND, e, e.getMessage());
    } catch (BadUserRequestException e) {
      throw new RestException(Status.BAD_REQUEST, e, e.getMessage());
    }
  }

  protected Task getTaskById(String id, boolean withCommentAttachmentInfo) {
    if (withCommentAttachmentInfo) {
      return engine.getTaskService().createTaskQuery().taskId(id).withCommentAttachmentInfo().initializeFormKeys().singleResult();
    }
    else{
      return engine.getTaskService().createTaskQuery().taskId(id).initializeFormKeys().singleResult();
    }
  }

  protected String getTaskFormMediaType(String taskId) {
    Task task = engine.getTaskService().createTaskQuery().initializeFormKeys().taskId(taskId).singleResult();
    ensureNotNull("No task found for taskId '%s'".formatted(taskId), "task", task);
    String formKey = task.getFormKey();
    if(formKey != null) {
      return ContentTypeUtil.getFormContentType(formKey);
    } else if(task.getOperatonFormRef() != null) {
      return ContentTypeUtil.getFormContentType(task.getOperatonFormRef());
    }
    return MediaType.APPLICATION_XHTML_XML;
  }

  protected <V extends Object> V runWithoutAuthorization(Supplier<V> action) {
    IdentityService identityService = engine.getIdentityService();
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    try {
      identityService.clearAuthentication();
      return action.get();
    } finally {
      identityService.setAuthentication(currentAuthentication);
    }
  }

  private Map<String, VariableValueDto> getTaskVariables(boolean withTaskVariablesInReturn) {
    VariableResource variableResource;
    if (withTaskVariablesInReturn) {
      variableResource = this.getVariables();
    } else {
      variableResource = this.getLocalVariables();
    }
    return variableResource.getVariables(true);
  }

}
