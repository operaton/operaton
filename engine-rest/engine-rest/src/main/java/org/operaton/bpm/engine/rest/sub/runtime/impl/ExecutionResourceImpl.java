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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.rest.dto.CreateIncidentDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.runtime.AdHocActivitiesTriggerDto;
import org.operaton.bpm.engine.rest.dto.runtime.AdHocActivityDto;
import org.operaton.bpm.engine.rest.dto.runtime.AdHocActivityTriggerInstructionDto;
import org.operaton.bpm.engine.rest.dto.runtime.AdHocSubProcessCompletionDto;
import org.operaton.bpm.engine.rest.dto.runtime.ExecutionDto;
import org.operaton.bpm.engine.rest.dto.runtime.ExecutionTriggerDto;
import org.operaton.bpm.engine.rest.dto.runtime.IncidentDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.sub.VariableResource;
import org.operaton.bpm.engine.rest.sub.runtime.EventSubscriptionResource;
import org.operaton.bpm.engine.rest.sub.runtime.ExecutionResource;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.variable.VariableMap;

public class ExecutionResourceImpl implements ExecutionResource {

  protected ProcessEngine engine;
  protected String executionId;
  protected ObjectMapper objectMapper;

  public ExecutionResourceImpl(ProcessEngine engine, String executionId, ObjectMapper objectMapper) {
    this.engine = engine;
    this.executionId = executionId;
    this.objectMapper = objectMapper;
  }

  @Override
  public ExecutionDto getExecution() {
    RuntimeService runtimeService = engine.getRuntimeService();
    Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();

    if (execution == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Execution with id %s does not exist".formatted(executionId));
    }

    return ExecutionDto.fromExecution(execution);
  }

  @Override
  public void signalExecution(ExecutionTriggerDto triggerDto) {
    RuntimeService runtimeService = engine.getRuntimeService();
    try {
      VariableMap variables = VariableValueDto.toMap(triggerDto.getVariables(), engine, objectMapper);
      runtimeService.signal(executionId, variables);

    } catch (RestException e) {
      String errorMessage = "Cannot signal execution %s: %s".formatted(executionId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    } catch (AuthorizationException e) {
      throw e;

    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, "Cannot signal execution %s: %s".formatted(executionId, e.getMessage()));

    }
  }

  @Override
  public List<AdHocActivityDto> getStartableAdHocActivities() {
    RuntimeService runtimeService = engine.getRuntimeService();
    try {
      List<AdHocActivity> activities = runtimeService.getStartableAdHocActivities(executionId);
      return AdHocActivityDto.fromAdHocActivities(activities);

    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e,
          "Cannot get startable ad-hoc activities for execution " + executionId + ": " + e.getMessage());

    } catch (AuthorizationException e) {
      throw e;

    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e,
          "Cannot get startable ad-hoc activities for execution " + executionId + ": " + e.getMessage());

    }
  }

  @Override
  public void triggerAdHocActivities(AdHocActivitiesTriggerDto triggerDto) {
    RuntimeService runtimeService = engine.getRuntimeService();
    try {
      Collection<String> activityIds = new ArrayList<>();
      Map<String, Map<String, Object>> activityVariables = new LinkedHashMap<>();

      if (triggerDto != null && triggerDto.getActivities() != null) {
        for (AdHocActivityTriggerInstructionDto instruction : triggerDto.getActivities()) {
          if (instruction == null) {
            continue;
          }

          String activityId = instruction.getActivityId();
          activityIds.add(activityId);

          Map<String, VariableValueDto> variables = instruction.getVariables();
          Map<String, Object> convertedVariables = VariableValueDto.toMap(variables, engine, objectMapper);
          activityVariables.put(activityId, convertedVariables);
        }
      }

      runtimeService.triggerAdHocActivities(executionId, activityIds, activityVariables);

    } catch (RestException e) {
      String errorMessage = String.format("Cannot trigger ad-hoc activities for execution %s: %s", executionId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e,
          "Cannot trigger ad-hoc activities for execution " + executionId + ": " + e.getMessage());

    } catch (AuthorizationException e) {
      throw e;

    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e,
          "Cannot trigger ad-hoc activities for execution " + executionId + ": " + e.getMessage());

    }
  }

  @Override
  public void completeAdHocSubProcess(AdHocSubProcessCompletionDto completionDto) {
    RuntimeService runtimeService = engine.getRuntimeService();
    try {
      Map<String, VariableValueDto> variables = completionDto != null ? completionDto.getVariables() : null;
      Map<String, Object> convertedVariables = variables != null ? VariableValueDto.toMap(variables, engine, objectMapper) : null;
      runtimeService.completeAdHocSubProcess(executionId, convertedVariables);

    } catch (RestException e) {
      String errorMessage = String.format("Cannot complete ad-hoc subprocess for execution %s: %s", executionId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e,
          "Cannot complete ad-hoc subprocess for execution " + executionId + ": " + e.getMessage());

    } catch (AuthorizationException e) {
      throw e;

    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e,
          "Cannot complete ad-hoc subprocess for execution " + executionId + ": " + e.getMessage());

    }
  }

  @Override
  public VariableResource getLocalVariables() {
    return new LocalExecutionVariablesResource(engine, executionId, objectMapper);
  }

  @Override
  public EventSubscriptionResource getMessageEventSubscription(String messageName) {
    return new MessageEventSubscriptionResource(engine, executionId, messageName, objectMapper);
  }

  @Override
  public IncidentDto createIncident(CreateIncidentDto createIncidentDto) {
    Incident newIncident = null;

    try {
      newIncident = engine.getRuntimeService()
          .createIncident(createIncidentDto.getIncidentType(), executionId, createIncidentDto.getConfiguration(), createIncidentDto.getMessage());
    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
    return IncidentDto.fromIncident(newIncident);
  }
}
