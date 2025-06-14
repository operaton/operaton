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
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.externaltask.ExternalTaskDto;
import org.operaton.bpm.engine.rest.dto.externaltask.ExternalTaskQueryDto;
import org.operaton.bpm.engine.rest.dto.externaltask.FetchExternalTasksExtendedDto;
import org.operaton.bpm.engine.rest.dto.externaltask.SetRetriesForExternalTasksDto;
import org.operaton.bpm.engine.rest.sub.externaltask.ExternalTaskResource;

/**
 * @author Thorben Lindhauer
 *
 */
public interface ExternalTaskRestService {

  String PATH = "/external-task";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<ExternalTaskDto> getExternalTasks(@Context UriInfo uriInfo,
      @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  List<ExternalTaskDto> queryExternalTasks(ExternalTaskQueryDto query,
      @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getExternalTasksCount(@Context UriInfo uriInfo);

  @POST
  @Path("/count")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto queryExternalTasksCount(ExternalTaskQueryDto query);

  @POST
  @Path("/fetchAndLock")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  void fetchAndLock(FetchExternalTasksExtendedDto dto, @Suspended final AsyncResponse asyncResponse);

  @Path("/{id}")
  ExternalTaskResource getExternalTask(@PathParam("id") String externalTaskId);

  @PUT
  @Path("/retries")
  @Consumes(MediaType.APPLICATION_JSON)
  void setRetries(SetRetriesForExternalTasksDto retriesDto);

  @POST
  @Path("/retries-async")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto setRetriesAsync(SetRetriesForExternalTasksDto retriesDto);

  @GET
  @Path("/topic-names")
  @Produces(MediaType.APPLICATION_JSON)
  List<String> getTopicNames(@QueryParam("withLockedTasks") boolean withLockedTasks,
      @QueryParam("withUnlockedTasks") boolean withUnlockedTasks,
      @QueryParam("withRetriesLeft") boolean withRetriesLeft);

}
