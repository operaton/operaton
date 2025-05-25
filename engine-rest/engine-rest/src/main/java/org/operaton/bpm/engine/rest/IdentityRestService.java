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
package org.operaton.bpm.engine.rest;

import org.operaton.bpm.engine.rest.dto.identity.BasicUserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.PasswordPolicyRequestDto;
import org.operaton.bpm.engine.rest.dto.task.GroupInfoDto;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
public interface IdentityRestService {

  String PATH = "/identity";

  @GET
  @Path("/groups")
  @Produces(MediaType.APPLICATION_JSON)
  GroupInfoDto getGroupInfo(@QueryParam("userId") String userId);

  @POST
  @Path("/verify")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  AuthenticationResult verifyUser(BasicUserCredentialsDto credentialsDto);

  @GET
  @Path("/password-policy")
  @Produces(MediaType.APPLICATION_JSON)
  Response getPasswordPolicy();

  @POST
  @Path("/password-policy")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  Response checkPassword(PasswordPolicyRequestDto password);
}
