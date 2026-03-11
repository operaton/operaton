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
package org.operaton.bpm.engine.rest.sub.history.impl;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.history.HistoricProcessInstanceResource;

public class HistoricProcessInstanceResourceImpl implements HistoricProcessInstanceResource {

  private final ProcessEngine engine;
  private final String processInstanceId;

  public HistoricProcessInstanceResourceImpl(ProcessEngine engine, String processInstanceId) {
    this.engine = engine;
    this.processInstanceId = processInstanceId;
  }

  @Override
  public HistoricProcessInstanceDto getHistoricProcessInstance() {
    HistoryService historyService = engine.getHistoryService();
    HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    if (instance == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Historic process instance with id %s does not exist".formatted(processInstanceId));
    }

    return HistoricProcessInstanceDto.fromHistoricProcessInstance(instance);
  }

  @Override
  public void deleteHistoricProcessInstance(Boolean failIfNotExists) {
    HistoryService historyService = engine.getHistoryService();
    try {
      if(failIfNotExists == null || failIfNotExists) {
        historyService.deleteHistoricProcessInstance(processInstanceId);
      }else {
        historyService.deleteHistoricProcessInstanceIfExists(processInstanceId);
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e, "Historic process instance with id %s does not exist".formatted(processInstanceId));
    }
  }

}
