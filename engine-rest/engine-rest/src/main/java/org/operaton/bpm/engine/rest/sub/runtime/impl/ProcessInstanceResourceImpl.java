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

import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.rest.dto.SuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.runtime.ActivityInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.modification.ProcessInstanceModificationDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.VariableResource;
import org.operaton.bpm.engine.rest.sub.runtime.ProcessInstanceCommentResource;
import org.operaton.bpm.engine.rest.sub.runtime.ProcessInstanceResource;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceModificationBuilder;

public class ProcessInstanceResourceImpl implements ProcessInstanceResource {

  protected ProcessEngine engine;
  protected String processInstanceId;
  protected ObjectMapper objectMapper;

  public ProcessInstanceResourceImpl(ProcessEngine engine, String processInstanceId, ObjectMapper objectMapper) {
    this.engine = engine;
    this.processInstanceId = processInstanceId;
    this.objectMapper = objectMapper;
  }

  @Override
  public ProcessInstanceDto getProcessInstance() {
    RuntimeService runtimeService = engine.getRuntimeService();
    ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    if (instance == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Process instance with id %s does not exist".formatted(processInstanceId));
    }

    return ProcessInstanceDto.fromProcessInstance(instance);
  }

  @Override
  public void deleteProcessInstance(boolean skipCustomListeners, boolean skipIoMappings, boolean skipSubprocesses, boolean failIfNotExists) {
    RuntimeService runtimeService = engine.getRuntimeService();
    try {
      if (failIfNotExists) {
        runtimeService.deleteProcessInstance(processInstanceId, null, skipCustomListeners, true, skipIoMappings, skipSubprocesses);
      } else {
        runtimeService.deleteProcessInstanceIfExists(processInstanceId, null, skipCustomListeners, true, skipIoMappings, skipSubprocesses);
      }
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e, e.getMessage());
    }

  }

  @Override
  public VariableResource getVariablesResource() {
    return new ExecutionVariablesResource(engine, processInstanceId, true, objectMapper);
  }

  @Override
  public ActivityInstanceDto getActivityInstanceTree() {
    RuntimeService runtimeService = engine.getRuntimeService();

    ActivityInstance activityInstance = null;

    try {
      activityInstance = runtimeService.getActivityInstance(processInstanceId);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.INTERNAL_SERVER_ERROR, e, e.getMessage());
    }

    if (activityInstance == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Process instance with id %s does not exist".formatted(processInstanceId));
    }

    return ActivityInstanceDto.fromActivityInstance(activityInstance);
  }

  @Override
  public void updateSuspensionState(SuspensionStateDto dto) {
    dto.updateSuspensionState(engine, processInstanceId);
  }

  @Override
  public void modifyProcessInstance(ProcessInstanceModificationDto dto) {
    if (dto.getInstructions() != null && !dto.getInstructions().isEmpty()) {
      ProcessInstanceModificationBuilder modificationBuilder =
          engine.getRuntimeService().createProcessInstanceModification(processInstanceId);

      dto.applyTo(modificationBuilder, engine, objectMapper);

      if (dto.getAnnotation() != null) {
        modificationBuilder.setAnnotation(dto.getAnnotation());
      }

      modificationBuilder.cancellationSourceExternal(true);

      modificationBuilder.execute(dto.isSkipCustomListeners(), dto.isSkipIoMappings());
    }
  }

  @Override
  public BatchDto modifyProcessInstanceAsync(ProcessInstanceModificationDto dto) {
    Batch batch = null;
    if (dto.getInstructions() != null && !dto.getInstructions().isEmpty()) {
      ProcessInstanceModificationBuilder modificationBuilder =
          engine.getRuntimeService().createProcessInstanceModification(processInstanceId);

      dto.applyTo(modificationBuilder, engine, objectMapper);

      if (dto.getAnnotation() != null) {
        modificationBuilder.setAnnotation(dto.getAnnotation());
      }

      modificationBuilder.cancellationSourceExternal(true);

      try {
        batch = modificationBuilder.executeAsync(dto.isSkipCustomListeners(), dto.isSkipIoMappings());
      } catch (BadUserRequestException e) {
        throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
      }
      return BatchDto.fromBatch(batch);
    }

    throw new InvalidRequestException(Status.BAD_REQUEST, "The provided instuctions are invalid.");
  }

  @Override
  public ProcessInstanceCommentResource getProcessInstanceCommentResource() {
    return new ProcessInstanceCommentResourceImpl(engine, processInstanceId);
  }

}
