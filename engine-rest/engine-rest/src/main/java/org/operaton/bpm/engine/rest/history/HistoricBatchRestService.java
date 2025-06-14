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

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.history.batch.CleanableHistoricBatchReportResultDto;
import org.operaton.bpm.engine.rest.dto.history.batch.HistoricBatchDto;
import org.operaton.bpm.engine.rest.dto.history.batch.removaltime.SetRemovalTimeToHistoricBatchesDto;
import org.operaton.bpm.engine.rest.sub.history.HistoricBatchResource;

@Path(HistoricBatchRestService.PATH)
public interface HistoricBatchRestService {

  String PATH = "/batch";

  @Path("/{id}")
  HistoricBatchResource getHistoricBatch(@PathParam("id") String batchId);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<HistoricBatchDto> getHistoricBatches(@Context UriInfo uriInfo,
                                            @QueryParam("firstResult") Integer firstResult,
                                            @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getHistoricBatchesCount(@Context UriInfo uriInfo);

  @GET
  @Path("/cleanable-batch-report")
  @Produces(MediaType.APPLICATION_JSON)
  List<CleanableHistoricBatchReportResultDto> getCleanableHistoricBatchesReport(@Context UriInfo uriInfo,
                                                 @QueryParam("firstResult") Integer firstResult,
                                                 @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/cleanable-batch-report/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getCleanableHistoricBatchesReportCount(@Context UriInfo uriInfo);

  @POST
  @Path("/set-removal-time")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto setRemovalTimeAsync(SetRemovalTimeToHistoricBatchesDto dto);

}
