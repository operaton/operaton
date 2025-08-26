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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.operaton.bpm.engine.rest.dto.runtime.CaseExecutionTriggerDto;
import org.operaton.bpm.engine.rest.dto.runtime.CaseInstanceDto;
import org.operaton.bpm.engine.rest.sub.VariableResource;

/**
 *
 * @author Roman Smirnov
 *
 */
public interface CaseInstanceResource {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  CaseInstanceDto getCaseInstance();

  @POST
  @Path("/complete")
  @Consumes(MediaType.APPLICATION_JSON)
  void complete(CaseExecutionTriggerDto triggerDto);

  @POST
  @Path("/close")
  @Consumes(MediaType.APPLICATION_JSON)
  void close(CaseExecutionTriggerDto triggerDto);

  @POST
  @Path("/terminate")
  @Consumes(MediaType.APPLICATION_JSON)
  void terminate(CaseExecutionTriggerDto triggerDto);

  @Path("/variables")
  VariableResource getVariablesResource();

}
