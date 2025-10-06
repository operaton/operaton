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
package org.operaton.bpm.engine.rest.sub.repository;

import java.util.List;
import java.util.Map;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.rest.dto.HistoryTimeToLiveDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.dmn.EvaluateDecisionDto;
import org.operaton.bpm.engine.rest.dto.repository.DecisionDefinitionDiagramDto;
import org.operaton.bpm.engine.rest.dto.repository.DecisionDefinitionDto;

public interface DecisionDefinitionResource {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  DecisionDefinitionDto getDecisionDefinition();

  @GET
  @Path("/xml")
  @Produces(MediaType.APPLICATION_JSON)
  DecisionDefinitionDiagramDto getDecisionDefinitionDmnXml();

  @GET
  @Path("/diagram")
  Response getDecisionDefinitionDiagram();

  @POST
  @Path("/evaluate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  List<Map<String, VariableValueDto>> evaluateDecision(@Context UriInfo context, EvaluateDecisionDto parameters);

  @PUT
  @Path("/history-time-to-live")
  @Consumes(MediaType.APPLICATION_JSON)
  void updateHistoryTimeToLive(HistoryTimeToLiveDto historyTimeToLiveDto);

}
