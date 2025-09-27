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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.dto.identity.GroupDto;
import org.operaton.bpm.engine.rest.dto.identity.GroupQueryDto;
import org.operaton.bpm.engine.rest.sub.identity.GroupResource;

/**
 * @author Daniel Meyer
 *
 */
@Produces(MediaType.APPLICATION_JSON)
public interface GroupRestService {

  String PATH = "/group";

  @Path("/{id}")
  GroupResource getGroup(@PathParam("id") String id);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<GroupDto> queryGroups(@Context UriInfo uriInfo,
      @QueryParam("firstResult") Integer firstResult, @QueryParam("maxResults") Integer maxResults);

  @GET
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto getGroupCount(@Context UriInfo uriInfo);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  List<GroupDto> queryGroups(GroupQueryDto query,
      @QueryParam("firstResult") Integer firstResult, @QueryParam("maxResults") Integer maxResults);

  @POST
  @Path("/count")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  CountResultDto queryGroupCount(GroupQueryDto query);

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  void createGroup(GroupDto groupDto);

  @OPTIONS
  @Produces(MediaType.APPLICATION_JSON)
  ResourceOptionsDto availableOperations(@Context UriInfo context);
}
