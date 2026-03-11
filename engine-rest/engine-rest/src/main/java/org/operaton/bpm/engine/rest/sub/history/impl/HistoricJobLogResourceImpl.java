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
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.rest.dto.history.HistoricJobLogDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.history.HistoricJobLogResource;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricJobLogResourceImpl implements HistoricJobLogResource {

  protected String id;
  protected ProcessEngine engine;

  public HistoricJobLogResourceImpl(String id, ProcessEngine engine) {
    this.id = id;
    this.engine = engine;
  }

  @Override
  public HistoricJobLogDto getHistoricJobLog() {
    HistoryService historyService = engine.getHistoryService();
    HistoricJobLog historicJobLog = historyService
        .createHistoricJobLogQuery()
        .logId(id)
        .singleResult();

    if (historicJobLog == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Historic job log with id %s does not exist".formatted(id));
    }

    return HistoricJobLogDto.fromHistoricJobLog(historicJobLog);
  }

  @Override
  public String getStacktrace() {
    try {
      HistoryService historyService = engine.getHistoryService();
      return historyService.getHistoricJobLogExceptionStacktrace(id);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e.getMessage());
    }
  }

}
