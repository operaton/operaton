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
package org.operaton.bpm.engine.rest.sub.runtime;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.operaton.bpm.engine.rest.dto.SuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.runtime.ActivityInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.modification.ProcessInstanceModificationDto;
import org.operaton.bpm.engine.rest.sub.VariableResource;

public interface ProcessInstanceResource {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  ProcessInstanceDto getProcessInstance();

  @DELETE
  void deleteProcessInstance(@QueryParam("skipCustomListeners") @DefaultValue("false") boolean skipCustomListeners,
      @QueryParam("skipIoMappings") @DefaultValue("false") boolean skipIoMappings,
      @QueryParam("skipSubprocesses") @DefaultValue("false") boolean skipSubprocesses,
      @QueryParam("failIfNotExists") @DefaultValue("true") boolean failIfNotExists);

  @Path("/variables")
  VariableResource getVariablesResource();

  @GET
  @Path("/activity-instances")
  @Produces(MediaType.APPLICATION_JSON)
  ActivityInstanceDto getActivityInstanceTree();

  @PUT
  @Path("/suspended")
  @Consumes(MediaType.APPLICATION_JSON)
  void updateSuspensionState(SuspensionStateDto dto);

  @POST
  @Path("/modification")
  @Consumes(MediaType.APPLICATION_JSON)
  void modifyProcessInstance(ProcessInstanceModificationDto dto);

  @POST
  @Path("/modification-async")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto modifyProcessInstanceAsync(ProcessInstanceModificationDto dto);

  @Path("/comment")
  ProcessInstanceCommentResource getProcessInstanceCommentResource();
}
