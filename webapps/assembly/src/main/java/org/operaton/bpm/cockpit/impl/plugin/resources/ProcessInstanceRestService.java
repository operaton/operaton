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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessInstanceQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.base.sub.resources.ProcessInstanceResource;
import org.operaton.bpm.cockpit.plugin.resource.AbstractPluginResource;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.rest.dto.CountResultDto;

public class ProcessInstanceRestService extends AbstractPluginResource {

  public static final String PATH = "/process-instance";
  protected ObjectMapper objectMapper;

  public ProcessInstanceRestService(String engineName) {
    super(engineName);
  }

  @Path("/{id}")
  public ProcessInstanceResource getProcessInstance(@PathParam("id") String id) {
    return new ProcessInstanceResource(getProcessEngine().getName(), id);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ProcessInstanceDto> getProcessInstances(@Context UriInfo uriInfo,
      @QueryParam("firstResult") Integer firstResult, @QueryParam("maxResults") Integer maxResults) {
    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto(uriInfo.getQueryParameters());
    return queryProcessInstances(queryParameter, firstResult, maxResults);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<ProcessInstanceDto> queryProcessInstances(final ProcessInstanceQueryDto queryParameter,
      @QueryParam("firstResult") Integer firstResult, @QueryParam("maxResults") Integer maxResults) {

    return getCommandExecutor().executeCommand(new QueryProcessInstancesCmd(queryParameter, firstResult, maxResults));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/count")
  public CountResultDto getProcessInstancesCount(@Context UriInfo uriInfo) {
    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto(uriInfo.getQueryParameters());
    return queryProcessInstancesCount(queryParameter);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/count")
  public CountResultDto queryProcessInstancesCount(ProcessInstanceQueryDto queryParameter) {

    return getCommandExecutor().executeCommand(new QueryProcessInstancesCountCmd(queryParameter));
  }

  private void paginate(ProcessInstanceQueryDto queryParameter, Integer firstResult, Integer maxResults) {
    if (firstResult == null) {
      firstResult = 0;
    }
    if (maxResults == null) {
      maxResults = Integer.MAX_VALUE;
    }
    queryParameter.setFirstResult(firstResult);
    queryParameter.setMaxResults(maxResults);
  }

  private void injectEngineConfig(ProcessInstanceQueryDto parameter) {

    ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl) getProcessEngine()).getProcessEngineConfiguration();
    if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_NONE)) {
      parameter.setHistoryEnabled(false);
    }

    parameter.initQueryVariableValues(processEngineConfiguration.getVariableSerializers(), processEngineConfiguration.getDatabaseType());
  }

  protected void configureExecutionQuery(ProcessInstanceQueryDto query) {
    configureAuthorizationCheck(query);
    configureTenantCheck(query);
    addPermissionCheck(query, PROCESS_INSTANCE, "RES.PROC_INST_ID_", READ);
    addPermissionCheck(query, PROCESS_DEFINITION, "P.KEY_", READ_INSTANCE);
  }

  protected void injectObjectMapper(ProcessInstanceQueryDto queryParameter) {
    queryParameter.setObjectMapper(objectMapper);
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /*
    The Command interface should always be implemented as a regular,
    or inner class so that invoked commands are correctly counted with Telemetry.
   */
  protected class QueryProcessInstancesCmd implements Command<List<ProcessInstanceDto>> {

    protected ProcessInstanceQueryDto queryParameter;
    protected Integer firstResult;
    protected Integer maxResults;

    public QueryProcessInstancesCmd(ProcessInstanceQueryDto queryParameter, Integer firstResult, Integer maxResults) {
      this.queryParameter = queryParameter;
      this.firstResult = firstResult;
      this.maxResults = maxResults;
    }

    @Override
    public List<ProcessInstanceDto> execute(CommandContext commandContext) {
      injectObjectMapper(queryParameter);
      injectEngineConfig(queryParameter);
      paginate(queryParameter, firstResult, maxResults);
      configureExecutionQuery(queryParameter);
      return getQueryService().executeQuery("selectRunningProcessInstancesIncludingIncidents", queryParameter);
    }
  }

  protected class QueryProcessInstancesCountCmd implements Command<CountResultDto> {

    protected ProcessInstanceQueryDto queryParameter;

    public QueryProcessInstancesCountCmd(ProcessInstanceQueryDto queryParameter) {
      this.queryParameter = queryParameter;
    }

    @Override
    public CountResultDto execute(CommandContext commandContext) {
      injectEngineConfig(queryParameter);
      configureExecutionQuery(queryParameter);
      long result = getQueryService().executeQueryRowCount("selectRunningProcessInstancesCount", queryParameter);
      return new CountResultDto(result);
    }
  }

}
