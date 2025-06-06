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
package org.operaton.bpm.engine.rest.sub.identity;

import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserProfileDto;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author Daniel Meyer
 *
 */
public interface UserResource {

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  void deleteUser();

  @POST
  @Path("/unlock")
  void unlockUser();

  // profile ///////////////////

  @GET
  @Path("/profile")
  @Produces(MediaType.APPLICATION_JSON)
  UserProfileDto getUserProfile(@Context UriInfo context);

  @PUT
  @Path("/profile")
  @Consumes(MediaType.APPLICATION_JSON)
  void updateProfile(UserProfileDto profile);

  // credentials //////////////

  @PUT
  @Path("/credentials")
  @Consumes(MediaType.APPLICATION_JSON)
  void updateCredentials(UserCredentialsDto account);

  @OPTIONS
  @Produces(MediaType.APPLICATION_JSON)
  ResourceOptionsDto availableOperations(@Context UriInfo context);

}
