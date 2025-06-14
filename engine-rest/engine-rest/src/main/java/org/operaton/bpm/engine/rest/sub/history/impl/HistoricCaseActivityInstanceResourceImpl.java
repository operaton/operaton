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

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.rest.dto.history.HistoricCaseActivityInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.history.HistoricCaseActivityInstanceResource;

import jakarta.ws.rs.core.Response.Status;

public class HistoricCaseActivityInstanceResourceImpl implements HistoricCaseActivityInstanceResource {

  private final ProcessEngine engine;
  private final String caseActivityInstanceId;

  public HistoricCaseActivityInstanceResourceImpl(ProcessEngine engine, String caseActivityInstanceId) {
    this.engine = engine;
    this.caseActivityInstanceId = caseActivityInstanceId;
  }

  @Override
  public HistoricCaseActivityInstanceDto getHistoricCaseActivityInstance() {
    HistoryService historyService = engine.getHistoryService();
    HistoricCaseActivityInstance instance = historyService.createHistoricCaseActivityInstanceQuery()
      .caseActivityInstanceId(caseActivityInstanceId).singleResult();

    if (instance == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Historic case activity instance with id '" + caseActivityInstanceId + "' does not exist");
    }

    return HistoricCaseActivityInstanceDto.fromHistoricCaseActivityInstance(instance);
  }

}
