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
package org.operaton.bpm.engine.rest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.MismatchingMessageCorrelationException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.rest.MessageRestService;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.message.CorrelationMessageDto;
import org.operaton.bpm.engine.rest.dto.message.MessageCorrelationResultDto;
import org.operaton.bpm.engine.rest.dto.message.MessageCorrelationResultWithVariableDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.runtime.MessageCorrelationBuilder;
import org.operaton.bpm.engine.runtime.MessageCorrelationResult;
import org.operaton.bpm.engine.runtime.MessageCorrelationResultWithVariables;

public class MessageRestServiceImpl extends AbstractRestProcessEngineAware implements MessageRestService {

  public MessageRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public Response deliverMessage(CorrelationMessageDto messageDto) {
    if (messageDto.getMessageName() == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "No message name supplied");
    }
    if (messageDto.getTenantId() != null && messageDto.isWithoutTenantId()) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Parameter 'tenantId' cannot be used together with parameter 'withoutTenantId'.");
    }
    boolean variablesInResultEnabled = messageDto.isVariablesInResultEnabled();
    if (!messageDto.isResultEnabled() && variablesInResultEnabled) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Parameter 'variablesInResultEnabled' cannot be used without 'resultEnabled' set to true.");
    }

    List<MessageCorrelationResultDto> resultDtos = new ArrayList<>();
    try {
      MessageCorrelationBuilder correlation = createMessageCorrelationBuilder(messageDto);
        if (!variablesInResultEnabled) {
          resultDtos.addAll(correlate(messageDto, correlation));
        } else {
          resultDtos.addAll(correlateWithVariablesEnabled(messageDto, correlation));
        }
    } catch (RestException e) {
      String errorMessage = "Cannot deliver message: %s".formatted(e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    } catch (MismatchingMessageCorrelationException e) {
      throw new RestException(Status.BAD_REQUEST, e);
    }
    return createResponse(resultDtos, messageDto);
  }

  protected List<MessageCorrelationResultDto> correlate(CorrelationMessageDto messageDto, MessageCorrelationBuilder correlation) {
    List<MessageCorrelationResultDto> resultDtos = new ArrayList<>();
    if (!messageDto.isAll()) {
      MessageCorrelationResult result = correlation.correlateWithResult();
      resultDtos.add(MessageCorrelationResultDto.fromMessageCorrelationResult(result));
    } else {
      List<MessageCorrelationResult> results = correlation.correlateAllWithResult();
      for (MessageCorrelationResult result : results) {
        resultDtos.add(MessageCorrelationResultDto.fromMessageCorrelationResult(result));
      }
    }
    return resultDtos;
  }

  protected List<MessageCorrelationResultWithVariableDto> correlateWithVariablesEnabled(CorrelationMessageDto messageDto, MessageCorrelationBuilder correlation) {
    List<MessageCorrelationResultWithVariableDto> resultDtos = new ArrayList<>();
    if (!messageDto.isAll()) {
      MessageCorrelationResultWithVariables result = correlation.correlateWithResultAndVariables(false);
      resultDtos.add(MessageCorrelationResultWithVariableDto.fromMessageCorrelationResultWithVariables(result));
    } else {
      List<MessageCorrelationResultWithVariables> results = correlation.correlateAllWithResultAndVariables(false);
      for (MessageCorrelationResultWithVariables result : results) {
        resultDtos.add(MessageCorrelationResultWithVariableDto.fromMessageCorrelationResultWithVariables(result));
      }
    }
    return resultDtos;
  }


  protected Response createResponse(List<MessageCorrelationResultDto> resultDtos, CorrelationMessageDto messageDto) {
    Response.ResponseBuilder response = Response.noContent();
    if (messageDto.isResultEnabled()) {
      response = Response.ok(resultDtos, MediaType.APPLICATION_JSON);
    }
    return response.build();
  }

  protected MessageCorrelationBuilder createMessageCorrelationBuilder(CorrelationMessageDto messageDto) {
    ProcessEngine processEngine = getProcessEngine();
    RuntimeService runtimeService = processEngine.getRuntimeService();

    ObjectMapper objectMapper = getObjectMapper();
    Map<String, Object> correlationKeys = VariableValueDto.toMap(messageDto.getCorrelationKeys(), processEngine, objectMapper);
    Map<String, Object> localCorrelationKeys = VariableValueDto.toMap(messageDto.getLocalCorrelationKeys(), processEngine, objectMapper);
    Map<String, Object> processVariables = VariableValueDto.toMap(messageDto.getProcessVariables(), processEngine, objectMapper);
    Map<String, Object> processVariablesLocal = VariableValueDto.toMap(messageDto.getProcessVariablesLocal(), processEngine, objectMapper);
    Map<String, Object> processVariablesToTriggeredScope = VariableValueDto.toMap(messageDto.getProcessVariablesToTriggeredScope(), processEngine, objectMapper);

    MessageCorrelationBuilder builder = runtimeService
        .createMessageCorrelation(messageDto.getMessageName());

    if (!processVariables.isEmpty()) {
      builder.setVariables(processVariables);
    }
    if (!processVariablesLocal.isEmpty()) {
      builder.setVariablesLocal(processVariablesLocal);
    }
    if (!processVariablesToTriggeredScope.isEmpty()) {
      builder.setVariablesToTriggeredScope(processVariablesToTriggeredScope);
    }
    if (messageDto.getBusinessKey() != null) {
      builder.processInstanceBusinessKey(messageDto.getBusinessKey());
    }

    correlationKeys.entrySet().stream().forEach(entry -> builder.processInstanceVariableEquals(entry.getKey(), entry.getValue()));

    localCorrelationKeys.entrySet().stream().forEach(entry -> builder.localVariableEquals(entry.getKey(), entry.getValue()));

    if (messageDto.getTenantId() != null) {
      builder.tenantId(messageDto.getTenantId());

    } else if (messageDto.isWithoutTenantId()) {
      builder.withoutTenantId();
    }

    String processInstanceId = messageDto.getProcessInstanceId();
    if (processInstanceId != null) {
      builder.processInstanceId(processInstanceId);
    }

    return builder;
  }

}
