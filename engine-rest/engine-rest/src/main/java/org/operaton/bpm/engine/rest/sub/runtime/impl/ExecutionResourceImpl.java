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
import org.operaton.bpm.engine.rest.dto.CreateIncidentDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.runtime.ExecutionDto;
import org.operaton.bpm.engine.rest.dto.runtime.ExecutionTriggerDto;
import org.operaton.bpm.engine.rest.dto.runtime.IncidentDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.sub.VariableResource;
import org.operaton.bpm.engine.rest.sub.runtime.EventSubscriptionResource;
import org.operaton.bpm.engine.rest.sub.runtime.ExecutionResource;
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
