/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.StatisticsResultDto;
import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionDto;
import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionSuspensionStateDto;
import org.operaton.bpm.engine.rest.sub.repository.ProcessDefinitionResource;

@Produces(MediaType.APPLICATION_JSON)
public interface ProcessDefinitionRestService {

  String APPLICATION_BPMN20_XML = "application/bpmn20+xml";
  MediaType APPLICATION_BPMN20_XML_TYPE =
      new MediaType("application", "bpmn20+xml");

  String PATH = "/process-definition";

  @Path("/{id}")
  ProcessDefinitionResource getProcessDefinitionById(@PathParam("id") String processDefinitionId);

  @Path("/key/{key}")
  ProcessDefinitionResource getProcessDefinitionByKey(@PathParam("key") String processDefinitionKey);

  @Path("/key/{key}/tenant-id/{tenantId}")
  ProcessDefinitionResource getProcessDefinitionByKeyAndTenantId(@PathParam("key") String processDefinitionKey,
                                                                 @PathParam("tenantId") String tenantId);

  /**
   * Exposes the {@link ProcessDefinitionQuery} interface as a REST service.
   *
   * @param uriInfo
   * @param firstResult
   * @param maxResults
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<ProcessDefinitionDto> getProcessDefinitions(@Context UriInfo uriInfo,
                                                   @QueryParam("firstResult") Integer firstResult,
                                                   @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getProcessDefinitionsCount(@Context UriInfo uriInfo);

  @GET
  @Path("/statistics")
  @Produces(MediaType.APPLICATION_JSON)
  List<StatisticsResultDto> getStatistics(@QueryParam("failedJobs") Boolean includeFailedJobs,
                                          @QueryParam("rootIncidents") Boolean includeRootIncidents,
                                          @QueryParam("incidents") Boolean includeIncidents,
                                          @QueryParam("incidentsForType") String includeIncidentsForType);

  @PUT
  @Path("/suspended")
  @Consumes(MediaType.APPLICATION_JSON)
  void updateSuspensionState(ProcessDefinitionSuspensionStateDto dto);

  @DELETE
  @Path("/key/{key}/delete")
  void deleteProcessDefinitionsByKey(@PathParam("key") String processDefinitionKey,
                                     @QueryParam("cascade") boolean cascade,
                                     @QueryParam("skipCustomListeners") boolean skipCustomListeners,
                                     @QueryParam("skipIoMappings") boolean skipIoMappings);

  @DELETE
  @Path("/key/{key}/tenant-id/{tenantId}/delete")
  void deleteProcessDefinitionsByKeyAndTenantId(@PathParam("key") String processDefinitionKey,
                                                @QueryParam("cascade") boolean cascade,
                                                @QueryParam("skipCustomListeners") boolean skipCustomListeners,
                                                @QueryParam("skipIoMappings") boolean skipIoMappings,
                                                @PathParam("tenantId") String tenantId);
}
