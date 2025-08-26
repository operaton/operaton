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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.rest.dto.metrics.MetricsIntervalResultDto;
import org.operaton.bpm.engine.rest.sub.metrics.MetricsResource;

/**
 * @author Daniel Meyer
 *
 */
@Produces(MediaType.APPLICATION_JSON)
public interface MetricsRestService {

  String PATH = "/metrics";

  @Path("/{name}")
  MetricsResource getMetrics(@PathParam("name") String name);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<MetricsIntervalResultDto> interval(@Context UriInfo uriInfo);

  @DELETE
  @Path("/task-worker")
  Response deleteTaskMetrics(@QueryParam("date") String dateString);
}
