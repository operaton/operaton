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
package org.operaton.bpm.engine.rest.sub.identity.impl;

import java.net.URI;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.rest.UserRestService;
import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserProfileDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.identity.UserResource;

import static org.operaton.bpm.engine.authorization.Permissions.DELETE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.USER;

/**
 * @author Daniel Meyer
 *
 */
public class UserResourceImpl extends AbstractIdentityResource implements UserResource {

  protected String rootResourcePath;

  public UserResourceImpl(String processEngineName, String userId, String rootResourcePath, ObjectMapper objectMapper) {
    super(processEngineName, USER, userId, objectMapper);
    this.rootResourcePath = rootResourcePath;
  }

  @Override
  public UserProfileDto getUserProfile(UriInfo context) {

    User dbUser = findUserObject();
    if(dbUser == null) {
      throw new UserNotFoundException(resourceId);
    }

    return UserProfileDto.fromUser(dbUser);
  }

  @Override
  public ResourceOptionsDto availableOperations(UriInfo context) {
    ResourceOptionsDto dto = new ResourceOptionsDto();

    // add links if operations are authorized
    UriBuilder baseUriBuilder = context.getBaseUriBuilder()
        .path(rootResourcePath)
        .path(UserRestService.PATH)
        .path(resourceId);
    URI baseUri = baseUriBuilder.build();
    URI profileUri = baseUriBuilder.path("/profile").build();

    dto.addReflexiveLink(profileUri, HttpMethod.GET, "self");

    if(!identityService.isReadOnly() && isAuthorized(DELETE)) {
      dto.addReflexiveLink(baseUri, HttpMethod.DELETE, "delete");
    }
    if(!identityService.isReadOnly() && isAuthorized(UPDATE)) {
      dto.addReflexiveLink(profileUri, HttpMethod.PUT, "update");
    }

    return dto;
  }

  @Override
  public void deleteUser() {
    ensureNotReadOnly();
    identityService.deleteUser(resourceId);
  }

  @Override
  public void unlockUser() {
    ensureNotReadOnly();
    identityService.unlockUser(resourceId);
  }

  @Override
  public void updateCredentials(UserCredentialsDto account) {
    ensureNotReadOnly();

    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    if((currentAuthentication != null && currentAuthentication.getUserId() != null)
            && !identityService.checkPassword(currentAuthentication.getUserId(), account.getAuthenticatedUserPassword())) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "The given authenticated user password is not valid.");
    }

    User dbUser = findUserObject();
    if(dbUser == null) {
      throw new UserNotFoundException(resourceId);
    }

    dbUser.setPassword(account.getPassword());

    identityService.saveUser(dbUser);
  }

  @Override
  public void updateProfile(UserProfileDto profile) {
    ensureNotReadOnly();

    User dbUser = findUserObject();
    if(dbUser == null) {
      throw new UserNotFoundException(resourceId);
    }

    profile.update(dbUser);

    identityService.saveUser(dbUser);
  }

  protected User findUserObject() {
    try {
      return identityService.createUserQuery()
          .userId(resourceId)
          .singleResult();
    } catch(ProcessEngineException e) {
      throw new InvalidRequestException(Status.INTERNAL_SERVER_ERROR, "Exception while performing user query: "+e.getMessage());
    }
  }

  @SuppressWarnings("java:S110")
  private static class UserNotFoundException extends InvalidRequestException {
    UserNotFoundException(String resourceId) {
      super(Status.NOT_FOUND, "User with id %s does not exist".formatted(resourceId));
    }
  }

}
