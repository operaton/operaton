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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.rest.SignalRestService;
import org.operaton.bpm.engine.rest.dto.SignalDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.runtime.SignalEventReceivedBuilder;

import jakarta.ws.rs.core.Response.Status;
import java.util.Map;

/**
 * @author Tassilo Weidner
 */
public class SignalRestServiceImpl extends AbstractRestProcessEngineAware implements SignalRestService {

  public SignalRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public void throwSignal(SignalDto dto) {
    String name = dto.getName();
    if (name == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "No signal name given");
    }

    SignalEventReceivedBuilder signalEvent = createSignalEventReceivedBuilder(dto);
    try {
      signalEvent.send();
    } catch (NotFoundException e) {
      // keeping compatibility with older versions where ProcessEngineException (=> 500) was
      // thrown; NotFoundException translates to 400 by default
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, e.getMessage());
    }
  }

  protected SignalEventReceivedBuilder createSignalEventReceivedBuilder(SignalDto dto) {
    RuntimeService runtimeService = getProcessEngine().getRuntimeService();
    String name = dto.getName();
    SignalEventReceivedBuilder signalEvent = runtimeService.createSignalEvent(name);

    String executionId = dto.getExecutionId();
    if (executionId != null) {
      signalEvent.executionId(executionId);
    }

    Map<String, VariableValueDto> variablesDto = dto.getVariables();
    if (variablesDto != null) {
      Map<String, Object> variables = VariableValueDto.toMap(variablesDto, getProcessEngine(), objectMapper);
      signalEvent.setVariables(variables);
    }

    String tenantId = dto.getTenantId();
    if (tenantId != null) {
      signalEvent.tenantId(tenantId);
    }

    boolean isWithoutTenantId = dto.isWithoutTenantId();
    if (isWithoutTenantId) {
      signalEvent.withoutTenantId();
    }

    return signalEvent;
  }
}
