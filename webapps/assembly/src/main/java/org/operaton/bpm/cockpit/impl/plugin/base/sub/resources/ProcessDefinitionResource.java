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
package org.operaton.bpm.cockpit.impl.plugin.base.sub.resources;

import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessDefinitionQueryDto;
import org.operaton.bpm.cockpit.plugin.resource.AbstractPluginResource;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;

import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

public class ProcessDefinitionResource extends AbstractPluginResource {

  protected String id;

  public ProcessDefinitionResource(String engineName, String id) {
    super(engineName);
    this.id = id;
  }

  @GET
  @Path("/called-process-definitions")
  @Produces(MediaType.APPLICATION_JSON)
  public List<ProcessDefinitionDto> getCalledProcessDefinitions(@Context UriInfo uriInfo) {
    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto(uriInfo.getQueryParameters());
    return queryCalledProcessDefinitions(queryParameter);
  }

  @POST
  @Path("/called-process-definitions")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<ProcessDefinitionDto> queryCalledProcessDefinitions(ProcessDefinitionQueryDto queryParameter) {
    return getCommandExecutor().executeCommand(new QueryCalledProcessDefinitionsCmd(queryParameter));
  }

  protected void configureExecutionQuery(ProcessDefinitionQueryDto query) {
    configureAuthorizationCheck(query);
    configureTenantCheck(query);
    addPermissionCheck(query, PROCESS_INSTANCE, "EXEC2.PROC_INST_ID_", READ);
    addPermissionCheck(query, PROCESS_DEFINITION, "PROCDEF.KEY_", READ_INSTANCE);
  }

  protected class QueryCalledProcessDefinitionsCmd implements Command<List<ProcessDefinitionDto>> {

    protected ProcessDefinitionQueryDto queryParameter;

    public QueryCalledProcessDefinitionsCmd(ProcessDefinitionQueryDto queryParameter) {
      this.queryParameter = queryParameter;
    }

    @Override
    public List<ProcessDefinitionDto> execute(CommandContext commandContext) {
      queryParameter.setParentProcessDefinitionId(id);
      injectEngineConfig(queryParameter);
      configureExecutionQuery(queryParameter);
      queryParameter.disableMaxResultsLimit();
      return getQueryService().executeQuery("selectCalledProcessDefinitions", queryParameter);
    }

    private void injectEngineConfig(ProcessDefinitionQueryDto parameter) {

      ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl) getProcessEngine()).getProcessEngineConfiguration();
      if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_NONE)) {
        parameter.setHistoryEnabled(false);
      }

      parameter.initQueryVariableValues(processEngineConfiguration.getVariableSerializers(), processEngineConfiguration.getDatabaseType());
    }
  }
}
