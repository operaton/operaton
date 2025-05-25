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

import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobSuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.runtime.SetJobRetriesDto;
import org.operaton.bpm.engine.rest.sub.runtime.JobResource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
public interface JobRestService {

	String PATH = "/job";

  @Path("/{id}")
  JobResource getJob(@PathParam("id") String jobId);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<JobDto> getJobs(@Context UriInfo uriInfo,
      @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults);


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  List<JobDto> queryJobs(JobQueryDto queryDto,
      @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getJobsCount(@Context UriInfo uriInfo);

  @POST
  @Path("/count")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto queryJobsCount(JobQueryDto queryDto);


  @POST
  @Path("/retries")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto setRetries (SetJobRetriesDto setJobRetriesDto);

  @PUT
  @Path("/suspended")
  @Consumes(MediaType.APPLICATION_JSON)
  void updateSuspensionState(JobSuspensionStateDto dto);
}
