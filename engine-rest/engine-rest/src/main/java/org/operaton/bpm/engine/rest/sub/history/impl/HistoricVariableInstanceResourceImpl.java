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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.rest.dto.history.HistoricVariableInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.AbstractResourceProvider;
import org.operaton.bpm.engine.rest.sub.history.HistoricVariableInstanceResource;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 * @author Ronny Bräunlich
 *
 */
public class HistoricVariableInstanceResourceImpl extends
    AbstractResourceProvider<HistoricVariableInstanceQuery, HistoricVariableInstance, HistoricVariableInstanceDto> implements HistoricVariableInstanceResource {

  public HistoricVariableInstanceResourceImpl(String variableId, ProcessEngine engine) {
    super(variableId, engine);
  }

  protected HistoricVariableInstanceQuery baseQuery() {
    return getEngine().getHistoryService().createHistoricVariableInstanceQuery().variableId(getId());
  }

  @Override
  protected Query<HistoricVariableInstanceQuery, HistoricVariableInstance> baseQueryForBinaryVariable() {
    return baseQuery().disableCustomObjectDeserialization();
  }

  @Override
  protected Query<HistoricVariableInstanceQuery, HistoricVariableInstance> baseQueryForVariable(boolean deserializeObjectValue) {
    HistoricVariableInstanceQuery query = baseQuery().disableBinaryFetching();

    if (!deserializeObjectValue) {
      query.disableCustomObjectDeserialization();
    }
    return query;
  }

  @Override
  protected TypedValue transformQueryResultIntoTypedValue(HistoricVariableInstance queryResult) {
    return queryResult.getTypedValue();
  }

  @Override
  protected HistoricVariableInstanceDto transformToDto(HistoricVariableInstance queryResult) {
    return HistoricVariableInstanceDto.fromHistoricVariableInstance(queryResult);
  }

  @Override
  protected String getResourceNameForErrorMessage() {
    return "Historic variable instance";
  }

  @Override
  public Response deleteVariableInstance() {
    try {
      getEngine().getHistoryService().deleteHistoricVariableInstance(id);
    } catch (NotFoundException nfe) { // rewrite status code from bad request (400) to not found (404)
      throw new InvalidRequestException(Status.NOT_FOUND, nfe, nfe.getMessage());
    }
    // return no content (204) since resource is deleted
    return Response.noContent().build();
  }

}
