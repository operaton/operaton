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
package org.operaton.bpm.engine.rest.sub.authorization.impl;

import java.net.URI;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.rest.AuthorizationRestService;
import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.dto.authorization.AuthorizationDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.impl.AbstractAuthorizedRestResource;
import org.operaton.bpm.engine.rest.sub.authorization.AuthorizationResource;

import static org.operaton.bpm.engine.authorization.Permissions.DELETE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.AUTHORIZATION;

/**
 *
 * @author Daniel Meyer
 *
 */
public class AuthorizationResourceImpl extends AbstractAuthorizedRestResource implements AuthorizationResource {

  protected final AuthorizationService authorizationService;

  public AuthorizationResourceImpl(String processEngineName, String resourceId, String relativeRootResourcePath, ObjectMapper objectMapper) {
    super(processEngineName, AUTHORIZATION, resourceId, objectMapper);
    setRelativeRootResourceUri(relativeRootResourcePath);
    authorizationService = getProcessEngine().getAuthorizationService();
  }

  @Override
  public AuthorizationDto getAuthorization(UriInfo context) {

    Authorization dbAuthorization = getDbAuthorization();

    return AuthorizationDto.fromAuthorization(dbAuthorization, getProcessEngine().getProcessEngineConfiguration());

  }

  @Override
  public void deleteAuthorization() {
    Authorization dbAuthorization = getDbAuthorization();
    authorizationService.deleteAuthorization(dbAuthorization.getId());
  }

  @Override
  public void updateAuthorization(AuthorizationDto dto) {
    // get db auth
    Authorization dbAuthorization = getDbAuthorization();
    // copy values from dto
    AuthorizationDto.update(dto, dbAuthorization, getProcessEngine().getProcessEngineConfiguration());
    // save
    authorizationService.saveAuthorization(dbAuthorization);
  }

  @Override
  public ResourceOptionsDto availableOperations(UriInfo context) {

    ResourceOptionsDto dto = new ResourceOptionsDto();

    URI uri = context.getBaseUriBuilder()
        .path(relativeRootResourcePath)
        .path(AuthorizationRestService.PATH)
        .path(resourceId)
        .build();

    dto.addReflexiveLink(uri, HttpMethod.GET, "self");

    if (isAuthorized(DELETE)) {
      dto.addReflexiveLink(uri, HttpMethod.DELETE, "delete");
    }
    if (isAuthorized(UPDATE)) {
      dto.addReflexiveLink(uri, HttpMethod.PUT, "update");
    }

    return dto;
  }

  // utils //////////////////////////////////////////////////

  protected Authorization getDbAuthorization() {
    Authorization dbAuthorization = authorizationService.createAuthorizationQuery()
      .authorizationId(resourceId)
      .singleResult();

    if (dbAuthorization == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Authorization with id %s does not exist.".formatted(resourceId));

    } else {
      return dbAuthorization;

    }
  }

}
