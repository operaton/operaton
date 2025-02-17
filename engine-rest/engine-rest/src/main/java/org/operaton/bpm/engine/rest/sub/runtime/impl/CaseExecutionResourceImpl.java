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
package org.operaton.bpm.engine.rest.sub.runtime.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.rest.dto.runtime.CaseExecutionDto;
import org.operaton.bpm.engine.rest.dto.runtime.CaseExecutionTriggerDto;
import org.operaton.bpm.engine.rest.dto.runtime.TriggerVariableValueDto;
import org.operaton.bpm.engine.rest.dto.runtime.VariableNameDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.sub.VariableResource;
import org.operaton.bpm.engine.rest.sub.runtime.CaseExecutionResource;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.variable.value.TypedValue;

import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Roman Smirnov
 *
 */
public class CaseExecutionResourceImpl implements CaseExecutionResource {

  protected ProcessEngine engine;
  protected String caseExecutionId;
  protected ObjectMapper objectMapper;

  public CaseExecutionResourceImpl(ProcessEngine engine, String caseExecutionId, ObjectMapper objectMapper) {
    this.engine = engine;
    this.caseExecutionId = caseExecutionId;
    this.objectMapper = objectMapper;
  }

  @Override
  public CaseExecutionDto getCaseExecution() {
    CaseService caseService = engine.getCaseService();

    CaseExecution execution = caseService
        .createCaseExecutionQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    if (execution == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Case execution with id " + caseExecutionId + " does not exist.");
    }

    return CaseExecutionDto.fromCaseExecution(execution);
  }

  @Override
  public void manualStart(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, "start manually");

      commandBuilder.manualStart();

    } catch (NotFoundException e) {
      throw createInvalidRequestException("manualStart", Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException("manualStart", Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException("manualStart", Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException("manualStart", Status.INTERNAL_SERVER_ERROR, e);

    }

  }

  @Override
  public void disable(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, "disable");

      commandBuilder.disable();

    } catch (NotFoundException e) {
      throw createInvalidRequestException("disable", Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException("disable", Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException("disable", Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException("disable", Status.INTERNAL_SERVER_ERROR, e);

    }

  }

  @Override
  public void reenable(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, "reenable");

      commandBuilder.reenable();

    } catch (NotFoundException e) {
      throw createInvalidRequestException("reenable", Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException("reenable", Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException("reenable", Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException("reenable", Status.INTERNAL_SERVER_ERROR, e);

    }
  }

  @Override
  public void complete(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, "complete");

      commandBuilder.complete();

    } catch (NotFoundException e) {
      throw createInvalidRequestException("complete", Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException("complete", Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException("complete", Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException("complete", Status.INTERNAL_SERVER_ERROR, e);

    }
  }

  @Override
  public void terminate(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, "terminate");

      commandBuilder.terminate();

    } catch (NotFoundException e) {
      throw createInvalidRequestException("terminate", Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException("terminate", Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException("terminate", Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException("terminate", Status.INTERNAL_SERVER_ERROR, e);

    }
  }

  protected InvalidRequestException createInvalidRequestException(String transition, Status status, ProcessEngineException cause) {
    String errorMessage = String.format("Cannot %s case execution %s: %s", transition, caseExecutionId, cause.getMessage());
    return new InvalidRequestException(status, cause, errorMessage);
  }

  protected RestException createRestException(String transition, Status status, ProcessEngineException cause) {
    String errorMessage = String.format("Cannot %s case execution %s: %s", transition, caseExecutionId, cause.getMessage());
    return new RestException(status, cause, errorMessage);
  }

  protected void initializeCommand(CaseExecutionCommandBuilder commandBuilder, CaseExecutionTriggerDto triggerDto, String transition) {
    Map<String, TriggerVariableValueDto> variables = triggerDto.getVariables();
    if (variables != null && !variables.isEmpty()) {
      initializeCommandWithVariables(commandBuilder, variables, transition);
    }

    List<VariableNameDto> deletions = triggerDto.getDeletions();
    if (deletions != null && !deletions.isEmpty()) {
      initializeCommandWithDeletions(commandBuilder, deletions, transition);
    }
  }

  protected void initializeCommandWithVariables(CaseExecutionCommandBuilder commandBuilder, Map<String, TriggerVariableValueDto> variables, String transition) {
    for(var vars : variables.entrySet()) {
      String variableName = vars.getKey();
      try {
        TriggerVariableValueDto variableValue = vars.getValue();
        TypedValue typedValue = variableValue.toTypedValue(engine, objectMapper);

        if (variableValue.isLocal()) {
          commandBuilder.setVariableLocal(variableName, typedValue);

        } else {
          commandBuilder.setVariable(variableName, typedValue);
        }

      } catch (RestException e) {
        String errorMessage = String.format("Cannot %s case execution %s due to invalid variable %s: %s", transition, caseExecutionId, variableName, e.getMessage());
        throw new RestException(e.getStatus(), e, errorMessage);

      }
    }
  }

  protected void initializeCommandWithDeletions(CaseExecutionCommandBuilder commandBuilder, List<VariableNameDto> deletions, String transition) {
    for (VariableNameDto variableName : deletions) {
      if (variableName.isLocal()) {
        commandBuilder.removeVariableLocal(variableName.getName());
      } else {
        commandBuilder.removeVariable(variableName.getName());
      }
    }
  }

  @Override
  public VariableResource getVariablesLocal() {
    return new LocalCaseExecutionVariablesResource(engine, caseExecutionId, objectMapper);
  }

  @Override
  public VariableResource getVariables() {
    return new CaseExecutionVariablesResource(engine, caseExecutionId, objectMapper);
  }

}
