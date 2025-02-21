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
import org.operaton.bpm.engine.rest.dto.runtime.CaseExecutionTriggerDto;
import org.operaton.bpm.engine.rest.dto.runtime.CaseInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.TriggerVariableValueDto;
import org.operaton.bpm.engine.rest.dto.runtime.VariableNameDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.sub.VariableResource;
import org.operaton.bpm.engine.rest.sub.runtime.CaseInstanceResource;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseInstance;

import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Roman Smirnov
 *
 */
public class CaseInstanceResourceImpl implements CaseInstanceResource {

  protected ProcessEngine engine;
  protected String caseInstanceId;
  protected ObjectMapper objectMapper;

  public CaseInstanceResourceImpl(ProcessEngine engine, String caseInstanceId, ObjectMapper objectMapper) {
    this.engine = engine;
    this.caseInstanceId = caseInstanceId;
    this.objectMapper = objectMapper;
  }

  @Override
  public CaseInstanceDto getCaseInstance() {
    CaseService caseService = engine.getCaseService();

    CaseInstance instance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();

    if (instance == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Case instance with id " + caseInstanceId + " does not exist.");
    }

    return CaseInstanceDto.fromCaseInstance(instance);
  }

  @Override
  public void complete(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

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
  public void close(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

      initializeCommand(commandBuilder, triggerDto, "close");

      commandBuilder.close();
    } catch (NotFoundException e) {
      throw createInvalidRequestException("close", Status.NOT_FOUND, e);
    } catch (NotValidException e) {
      throw createInvalidRequestException("close", Status.BAD_REQUEST, e);
    } catch (NotAllowedException e) {
      throw createInvalidRequestException("close", Status.FORBIDDEN, e);
    } catch (ProcessEngineException e) {
      throw createRestException("close", Status.INTERNAL_SERVER_ERROR, e);
    }
  }

  @Override
  public void terminate(CaseExecutionTriggerDto triggerDto) {
    try {
      CaseService caseService = engine.getCaseService();
      CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

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
    String errorMessage = String.format("Cannot %s case instance %s: %s", transition, caseInstanceId, cause.getMessage());
    return new InvalidRequestException(status, cause, errorMessage);
  }

  protected RestException createRestException(String transition, Status status, ProcessEngineException cause) {
    String errorMessage = String.format("Cannot %s case instance %s: %s", transition, caseInstanceId, cause.getMessage());
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

        if (variableValue.isLocal()) {
          commandBuilder.setVariableLocal(variableName, variableValue.toTypedValue(engine, objectMapper));
        } else {
          commandBuilder.setVariable(variableName, variableValue.toTypedValue(engine, objectMapper));
        }
      } catch (RestException e) {
        String errorMessage = String.format("Cannot %s case instance %s due to invalid variable %s: %s", transition, caseInstanceId, variableName, e.getMessage());
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
  public VariableResource getVariablesResource() {
    return new CaseExecutionVariablesResource(engine, caseInstanceId, objectMapper);
  }

}
