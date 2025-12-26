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
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.rest.dto.history.HistoricDecisionInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.history.HistoricDecisionInstanceResource;

public class HistoricDecisionInstanceResourceImpl implements HistoricDecisionInstanceResource {

  private final ProcessEngine engine;
  private final String decisionInstanceId;

  public HistoricDecisionInstanceResourceImpl(ProcessEngine engine, String decisionInstanceId) {
    this.engine = engine;
    this.decisionInstanceId = decisionInstanceId;
  }

  @Override
  public HistoricDecisionInstanceDto getHistoricDecisionInstance(Boolean includeInputs, Boolean includeOutputs, Boolean disableBinaryFetching, Boolean disableCustomObjectDeserialization) {
    HistoryService historyService = engine.getHistoryService();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionInstanceId(decisionInstanceId);
    if (includeInputs != null && includeInputs) {
      query.includeInputs();
    }
    if (includeOutputs != null && includeOutputs) {
      query.includeOutputs();
    }
    if (disableBinaryFetching != null && disableBinaryFetching) {
      query.disableBinaryFetching();
    }
    if (disableCustomObjectDeserialization != null && disableCustomObjectDeserialization) {
      query.disableCustomObjectDeserialization();
    }

    HistoricDecisionInstance instance = query.singleResult();

    if (instance == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Historic decision instance with id '%s' does not exist".formatted(decisionInstanceId));
    }

    return HistoricDecisionInstanceDto.fromHistoricDecisionInstance(instance);
  }
}
