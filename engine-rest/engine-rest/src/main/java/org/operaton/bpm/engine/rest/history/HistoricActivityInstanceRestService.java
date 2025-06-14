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

import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricActivityInstanceDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricActivityInstanceQueryDto;
import org.operaton.bpm.engine.rest.sub.history.HistoricActivityInstanceResource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

@Path(HistoricActivityInstanceRestService.PATH)
@Produces(MediaType.APPLICATION_JSON)
public interface HistoricActivityInstanceRestService {

  String PATH = "/activity-instance";

  @Path("/{id}")
  HistoricActivityInstanceResource getHistoricCaseInstance(@PathParam("id") String activityInstanceId);

  /**
   * Exposes the {@link HistoricActivityInstanceQuery} interface as a REST
   * service.
   *
   * @param query
   * @param firstResult
   * @param maxResults
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<HistoricActivityInstanceDto> getHistoricActivityInstances(@Context UriInfo uriInfo, @QueryParam("firstResult") Integer firstResult,
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
  List<HistoricActivityInstanceDto> queryHistoricActivityInstances(HistoricActivityInstanceQueryDto query, @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getHistoricActivityInstancesCount(@Context UriInfo uriInfo);

  @POST
  @Path("/count")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto queryHistoricActivityInstancesCount(HistoricActivityInstanceQueryDto query);
}
