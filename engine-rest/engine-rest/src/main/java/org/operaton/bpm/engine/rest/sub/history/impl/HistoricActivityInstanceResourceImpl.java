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

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.rest.dto.history.HistoricActivityInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.history.HistoricActivityInstanceResource;

public class HistoricActivityInstanceResourceImpl implements HistoricActivityInstanceResource {

  private final ProcessEngine engine;
  private final String activityInstanceId;

  public HistoricActivityInstanceResourceImpl(ProcessEngine engine, String activityInstanceId) {
    this.engine = engine;
    this.activityInstanceId = activityInstanceId;
  }

  @Override
  public HistoricActivityInstanceDto getHistoricActivityInstance() {
    HistoryService historyService = engine.getHistoryService();
    HistoricActivityInstance instance = historyService.createHistoricActivityInstanceQuery()
      .activityInstanceId(activityInstanceId).singleResult();

    if (instance == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Historic activity instance with id '%s' does not exist".formatted(activityInstanceId));
    }

    final HistoricActivityInstanceDto historicActivityInstanceDto = new HistoricActivityInstanceDto();
    HistoricActivityInstanceDto.fromHistoricActivityInstance(historicActivityInstanceDto, instance);
    return historicActivityInstanceDto;
  }

}
