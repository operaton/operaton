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
package org.operaton.bpm.engine.rest;

import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceSuspensionStateAsyncDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceSuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.runtime.SetJobRetriesByProcessDto;
import org.operaton.bpm.engine.rest.dto.runtime.batch.CorrelationMessageAsyncDto;
import org.operaton.bpm.engine.rest.dto.runtime.batch.DeleteProcessInstancesDto;
import org.operaton.bpm.engine.rest.dto.runtime.batch.SetVariablesAsyncDto;
import org.operaton.bpm.engine.rest.sub.runtime.ProcessInstanceResource;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;

@Produces(MediaType.APPLICATION_JSON)
public interface ProcessInstanceRestService {

  String PATH = "/process-instance";

  @Path("/{id}")
  ProcessInstanceResource getProcessInstance(@PathParam("id") String processInstanceId);

  /**
   * Exposes the {@link ProcessInstanceQuery} interface as a REST service.
   *
   * @param uriInfo
   * @param firstResult
   * @param maxResults
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<ProcessInstanceDto> getProcessInstances(@Context UriInfo uriInfo,
                                               @QueryParam("firstResult") Integer firstResult,
                                               @QueryParam("maxResults") Integer maxResults);

  /**
   * Expects the same parameters as
   * {@link ProcessInstanceRestService#getProcessInstances(UriInfo, Integer, Integer)} (as a JSON message body)
   * and allows for any number of variable checks.
   *
   * @param query
   * @param firstResult
   * @param maxResults
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  List<ProcessInstanceDto> queryProcessInstances(ProcessInstanceQueryDto query,
                                                 @QueryParam("firstResult") Integer firstResult,
                                                 @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getProcessInstancesCount(@Context UriInfo uriInfo);

  @POST
  @Path("/count")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto queryProcessInstancesCount(ProcessInstanceQueryDto query);

  @PUT
  @Path("/suspended")
  @Consumes(MediaType.APPLICATION_JSON)
  void updateSuspensionState(ProcessInstanceSuspensionStateDto dto);

  @POST
  @Path("/suspended-async")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto updateSuspensionStateAsync(ProcessInstanceSuspensionStateAsyncDto dto);

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto deleteAsync(DeleteProcessInstancesDto dto);

  @POST
  @Path("/delete-historic-query-based")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto deleteAsyncHistoricQueryBased(DeleteProcessInstancesDto dto);

  @POST
  @Path("/job-retries")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto setRetriesByProcess(SetJobRetriesByProcessDto setJobRetriesDto);

  @POST
  @Path("/job-retries-historic-query-based")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto setRetriesByProcessHistoricQueryBased(SetJobRetriesByProcessDto setJobRetriesDto);

  @POST
  @Path("/variables-async")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto setVariablesAsync(SetVariablesAsyncDto setVariablesAsyncDto);

  @POST
  @Path("/message-async")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto correlateMessageAsync(CorrelationMessageAsyncDto correlationMessageAsyncDto);

}
