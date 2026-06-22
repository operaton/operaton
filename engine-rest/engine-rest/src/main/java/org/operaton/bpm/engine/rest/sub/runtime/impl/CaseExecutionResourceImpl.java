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

import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

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

/**
 *
 * @author Roman Smirnov
 *
 */
public class CaseExecutionResourceImpl implements CaseExecutionResource {

  private static final String TRANSITION_MANUAL_START = "manualStart";
  private static final String TRANSITION_REENABLE = "reenable";
  private static final String TRANSITION_COMPLETE = "complete";
  private static final String TRANSITION_TERMINATE = "terminate";
  private static final String TRANSITION_DISABLE = "disable";
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
      throw new InvalidRequestException(Status.NOT_FOUND, "Case execution with id %s does not exist.".formatted(caseExecutionId));
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
      throw createInvalidRequestException(TRANSITION_MANUAL_START, Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException(TRANSITION_MANUAL_START, Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException(TRANSITION_MANUAL_START, Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException(TRANSITION_MANUAL_START, Status.INTERNAL_SERVER_ERROR, e);

    }

  }

  @Override
  public void disable(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, TRANSITION_DISABLE);

      commandBuilder.disable();

    } catch (NotFoundException e) {
      throw createInvalidRequestException(TRANSITION_DISABLE, Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException(TRANSITION_DISABLE, Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException(TRANSITION_DISABLE, Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException(TRANSITION_DISABLE, Status.INTERNAL_SERVER_ERROR, e);

    }

  }

  @Override
  public void reenable(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, TRANSITION_REENABLE);

      commandBuilder.reenable();

    } catch (NotFoundException e) {
      throw createInvalidRequestException(TRANSITION_REENABLE, Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException(TRANSITION_REENABLE, Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException(TRANSITION_REENABLE, Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException(TRANSITION_REENABLE, Status.INTERNAL_SERVER_ERROR, e);

    }
  }

  @Override
  public void complete(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, TRANSITION_COMPLETE);

      commandBuilder.complete();

    } catch (NotFoundException e) {
      throw createInvalidRequestException(TRANSITION_COMPLETE, Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException(TRANSITION_COMPLETE, Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException(TRANSITION_COMPLETE, Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException(TRANSITION_COMPLETE, Status.INTERNAL_SERVER_ERROR, e);

    }
  }

  @Override
  public void terminate(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

      initializeCommand(commandBuilder, triggerDto, TRANSITION_TERMINATE);

      commandBuilder.terminate();

    } catch (NotFoundException e) {
      throw createInvalidRequestException(TRANSITION_TERMINATE, Status.NOT_FOUND, e);

    } catch (NotValidException e) {
      throw createInvalidRequestException(TRANSITION_TERMINATE, Status.BAD_REQUEST, e);

    } catch (NotAllowedException e) {
      throw createInvalidRequestException(TRANSITION_TERMINATE, Status.FORBIDDEN, e);

    } catch (ProcessEngineException e) {
      throw createRestException(TRANSITION_TERMINATE, Status.INTERNAL_SERVER_ERROR, e);

    }
  }

  protected InvalidRequestException createInvalidRequestException(String transition, Status status, ProcessEngineException cause) {
    String errorMessage = "Cannot %s case execution %s: %s".formatted(transition, caseExecutionId, cause.getMessage());
    return new InvalidRequestException(status, cause, errorMessage);
  }

  protected RestException createRestException(String transition, Status status, ProcessEngineException cause) {
    String errorMessage = "Cannot %s case execution %s: %s".formatted(transition, caseExecutionId, cause.getMessage());
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
        String errorMessage = "Cannot %s case execution %s due to invalid variable %s: %s".formatted(transition, caseExecutionId, variableName, e.getMessage());
        throw new RestException(e.getStatus(), e, errorMessage);

      }
    }
  }

  @SuppressWarnings("unused")
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
