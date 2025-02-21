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
package org.operaton.bpm.engine.rest.sub;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.operaton.bpm.engine.rest.dto.PatchVariablesDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.mapper.MultipartFormData;

public interface VariableResource {

  public static final String DESERIALIZE_VALUE_QUERY_PARAM = "deserializeValue";
  public static final String DESERIALIZE_VALUES_QUERY_PARAM = DESERIALIZE_VALUE_QUERY_PARAM + "s";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Map<String, VariableValueDto> getVariables(
      @QueryParam(DESERIALIZE_VALUES_QUERY_PARAM) @DefaultValue("true") boolean deserializeValues);

  @GET
  @Path("/{varId}")
  @Produces(MediaType.APPLICATION_JSON)
  VariableValueDto getVariable(
      @PathParam("varId") String variableName,
      @QueryParam(DESERIALIZE_VALUE_QUERY_PARAM) @DefaultValue("true") boolean deserializeValue);

  @GET
  @Path("/{varId}/data")
  public Response getVariableBinary(@PathParam("varId") String variableName);

  @PUT
  @Path("/{varId}")
  @Consumes(MediaType.APPLICATION_JSON)
  void putVariable(@PathParam("varId") String variableName, VariableValueDto variable);

  @POST // using POST since PUT is not as widely supported for file uploads
  @Path("/{varId}/data")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  void setBinaryVariable(@PathParam("varId") String variableName, MultipartFormData multipartFormData);

  @DELETE
  @Path("/{varId}")
  void deleteVariable(@PathParam("varId") String variableName);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  void modifyVariables(PatchVariablesDto patch);
}
