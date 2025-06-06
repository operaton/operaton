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
package org.operaton.bpm.engine.rest.history;

import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricTaskInstanceDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricTaskInstanceQueryDto;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author Roman Smirnov
 *
 */
@Path(HistoricVariableInstanceRestService.PATH)
@Produces(MediaType.APPLICATION_JSON)
public interface HistoricTaskInstanceRestService {

  String PATH = "/task";

  /**
   * Exposes the {@link HistoricTaskInstanceQuery} interface as a REST
   * service.
   *
   * @param query
   * @param firstResult
   * @param maxResults
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<HistoricTaskInstanceDto> getHistoricTaskInstances(@Context UriInfo uriInfo, @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults);

  /**
   * @param query
   * @param firstResult
   * @param maxResults
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  List<HistoricTaskInstanceDto> queryHistoricTaskInstances(HistoricTaskInstanceQueryDto queryDto, @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getHistoricTaskInstancesCount(@Context UriInfo uriInfo);

  @POST
  @Path("/count")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto queryHistoricTaskInstancesCount(HistoricTaskInstanceQueryDto queryDto);

  /**
   * Provides a report sub module
   */
  @GET
  @Path("report")
  @Produces(MediaType.APPLICATION_JSON)
  Response getHistoricTaskInstanceReport(@Context UriInfo uriInfo);
}
