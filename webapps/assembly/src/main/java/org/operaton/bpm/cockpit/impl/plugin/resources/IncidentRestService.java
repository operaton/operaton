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
package org.operaton.bpm.cockpit.impl.plugin.resources;

import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.IncidentDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.IncidentQueryDto;
import org.operaton.bpm.cockpit.plugin.resource.AbstractPluginResource;
import org.operaton.bpm.engine.rest.dto.CountResultDto;

/**
 * @author roman.smirnov
 */
@Produces(MediaType.APPLICATION_JSON)
public class IncidentRestService extends AbstractPluginResource {

  public static final String PATH = "/incident";

  public IncidentRestService(String engineName) {
    super(engineName);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<IncidentDto> getIncidents(@Context UriInfo uriInfo,
      @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults) {
    IncidentQueryDto queryParameter = new IncidentQueryDto(uriInfo.getQueryParameters());
    return queryIncidents(queryParameter, firstResult, maxResults);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<IncidentDto> queryIncidents(IncidentQueryDto queryParameter,
      @QueryParam("firstResult") Integer firstResult,
      @QueryParam("maxResults") Integer maxResults) {

    paginateQueryParameters(queryParameter, firstResult, maxResults);
    configureExecutionQuery(queryParameter);
    return getQueryService().executeQuery("selectIncidentWithCauseAndRootCauseIncidents", queryParameter);
  }

  private void paginateQueryParameters(IncidentQueryDto queryParameter, Integer firstResult, Integer maxResults) {
    if (firstResult == null) {
      firstResult = 0;
    }
    if (maxResults == null) {
      maxResults = Integer.MAX_VALUE;
    }
    queryParameter.setFirstResult(firstResult);
    queryParameter.setMaxResults(maxResults);
  }

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  public CountResultDto getIncidentsCount(@Context UriInfo uriInfo) {
    IncidentQueryDto queryParameter = new IncidentQueryDto(uriInfo.getQueryParameters());
    return queryIncidentsCount(queryParameter);
  }

  @POST
  @Path("/count")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CountResultDto queryIncidentsCount(IncidentQueryDto queryParameter) {
    CountResultDto result = new CountResultDto();
    configureExecutionQuery(queryParameter);
    long count = getQueryService().executeQueryRowCount("selectIncidentWithCauseAndRootCauseIncidentsCount", queryParameter);
    result.setCount(count);

    return result;
  }

  protected void configureExecutionQuery(IncidentQueryDto query) {
    configureAuthorizationCheck(query);
    configureTenantCheck(query);
    addPermissionCheck(query, PROCESS_INSTANCE, "RES.PROC_INST_ID_", READ);
    addPermissionCheck(query, PROCESS_DEFINITION, "PROCDEF.KEY_", READ_INSTANCE);
  }

}
