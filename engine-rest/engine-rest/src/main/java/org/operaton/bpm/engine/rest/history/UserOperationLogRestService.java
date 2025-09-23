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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.rest.dto.AnnotationDto;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.UserOperationLogEntryDto;

/**
 * Exposes the {@link org.operaton.bpm.engine.history.UserOperationLogQuery} as REST service.
 *
 * @author Danny Gräf
 */
@Path(UserOperationLogRestService.PATH)
public interface UserOperationLogRestService {

  String PATH = "/user-operation";

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto queryUserOperationCount(@Context UriInfo uriInfo);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<UserOperationLogEntryDto> queryUserOperationEntries(@Context UriInfo uriInfo,
                                                           @QueryParam("firstResult") Integer firstResult,
                                                           @QueryParam("maxResults") Integer maxResults);

  @PUT
  @Path("/{operationId}/set-annotation")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  Response setAnnotation(@PathParam("operationId") String operationId, AnnotationDto annotationDto);

  @PUT
  @Path("/{operationId}/clear-annotation")
  @Produces(MediaType.APPLICATION_JSON)
  Response clearAnnotation(@PathParam("operationId") String operationId);

}
