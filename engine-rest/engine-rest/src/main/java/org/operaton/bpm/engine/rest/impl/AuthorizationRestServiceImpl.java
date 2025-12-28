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
package org.operaton.bpm.engine.rest.impl;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.rest.AuthorizationRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.dto.authorization.AuthorizationCheckResultDto;
import org.operaton.bpm.engine.rest.dto.authorization.AuthorizationCreateDto;
import org.operaton.bpm.engine.rest.dto.authorization.AuthorizationDto;
import org.operaton.bpm.engine.rest.dto.authorization.AuthorizationQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.authorization.AuthorizationResource;
import org.operaton.bpm.engine.rest.sub.authorization.impl.AuthorizationResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;
import org.operaton.bpm.engine.rest.util.ResourceUtil;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Resources.AUTHORIZATION;

/**
 * @author Daniel Meyer
 *
 */
public class AuthorizationRestServiceImpl extends AbstractAuthorizedRestResource implements AuthorizationRestService {

  public AuthorizationRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName,AUTHORIZATION, ANY, objectMapper);
  }

  @Override
  public AuthorizationCheckResultDto isUserAuthorized(String permissionName, String resourceName, Integer resourceType, String resourceId, String userId) {

    // validate request:
    if(permissionName == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Query parameter 'permissionName' cannot be null");

    } else if(resourceName == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Query parameter 'resourceName' cannot be null");

    } else if(resourceType == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Query parameter 'resourceType' cannot be null");

    }

    final Authentication currentAuthentication = getProcessEngine().getIdentityService().getCurrentAuthentication();
    if(currentAuthentication == null) {
      throw new InvalidRequestException(Status.UNAUTHORIZED, "You must be authenticated in order to use this resource.");
    }

    final AuthorizationService authorizationService = getProcessEngine().getAuthorizationService();

    ResourceUtil resource = new ResourceUtil(resourceName, resourceType);
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
    Permission permission = processEngineConfiguration.getPermissionProvider().getPermissionForName(permissionName, resourceType);
    String currentUserId = currentAuthentication.getUserId();

    boolean isUserAuthorized = false;

    String userIdToCheck;
    List<String> groupIdsToCheck;

    if(userId != null && !userId.equals(currentUserId)) {
      boolean isCurrentUserAuthorized = authorizationService.isUserAuthorized(currentUserId, currentAuthentication.getGroupIds(), Permissions.READ, Resources.AUTHORIZATION);
      if (!isCurrentUserAuthorized) {
        throw new InvalidRequestException(Status.FORBIDDEN, "You must have READ permission for Authorization resource.");
      }
      userIdToCheck = userId;
      groupIdsToCheck = getUserGroups(userId);
    } else {
      // userId == null || userId.equals(currentUserId)
      userIdToCheck = currentUserId;
      groupIdsToCheck = currentAuthentication.getGroupIds();
    }

    if(resourceId == null || Authorization.ANY.equals(resourceId)) {
      isUserAuthorized = authorizationService.isUserAuthorized(userIdToCheck, groupIdsToCheck, permission, resource);
    } else {
      isUserAuthorized = authorizationService.isUserAuthorized(userIdToCheck, groupIdsToCheck, permission, resource, resourceId);
    }

    return new AuthorizationCheckResultDto(isUserAuthorized, permissionName, resource, resourceId);
  }

  @Override
  public AuthorizationResource getAuthorization(String id) {
    return new AuthorizationResourceImpl(getProcessEngine().getName(), id, relativeRootResourcePath, getObjectMapper());
  }

  @Override
  public List<AuthorizationDto> queryAuthorizations(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    AuthorizationQueryDto queryDto = new AuthorizationQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryAuthorizations(queryDto, firstResult, maxResults);
  }

  @Override
  public ResourceOptionsDto availableOperations(UriInfo context) {

    UriBuilder baseUriBuilder = context.getBaseUriBuilder()
        .path(relativeRootResourcePath)
        .path(AuthorizationRestService.PATH);

    ResourceOptionsDto resourceOptionsDto = new ResourceOptionsDto();

    // GET /
    URI baseUri = baseUriBuilder.build();
    resourceOptionsDto.addReflexiveLink(baseUri, HttpMethod.GET, "list");

    // GET /count
    URI countUri = baseUriBuilder.clone().path("/count").build();
    resourceOptionsDto.addReflexiveLink(countUri, HttpMethod.GET, "count");

    // POST /create
    if(isAuthorized(CREATE)) {
      URI createUri = baseUriBuilder.clone().path("/create").build();
      resourceOptionsDto.addReflexiveLink(createUri, HttpMethod.POST, "create");
    }

    return resourceOptionsDto;
  }

  public List<AuthorizationDto> queryAuthorizations(AuthorizationQueryDto queryDto, Integer firstResult, Integer maxResults) {

    queryDto.setObjectMapper(getObjectMapper());
    AuthorizationQuery query = queryDto.toQuery(getProcessEngine());

    List<Authorization> resultList = QueryUtil.list(query, firstResult, maxResults);

    return AuthorizationDto.fromAuthorizationList(resultList, getProcessEngine().getProcessEngineConfiguration());
  }

  @Override
  public CountResultDto getAuthorizationCount(UriInfo uriInfo) {
    AuthorizationQueryDto queryDto = new AuthorizationQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return getAuthorizationCount(queryDto);
  }

  protected CountResultDto getAuthorizationCount(AuthorizationQueryDto queryDto) {
    AuthorizationQuery query = queryDto.toQuery(getProcessEngine());
    long count = query.count();
    return new CountResultDto(count);
  }

  @Override
  public AuthorizationDto createAuthorization(UriInfo context, AuthorizationCreateDto dto) {
    final AuthorizationService authorizationService = getProcessEngine().getAuthorizationService();

    Authorization newAuthorization = authorizationService.createNewAuthorization(dto.getType());
    AuthorizationCreateDto.update(dto, newAuthorization, getProcessEngine().getProcessEngineConfiguration());

    newAuthorization = authorizationService.saveAuthorization(newAuthorization);

    return getAuthorization(newAuthorization.getId()).getAuthorization(context);
  }

  // utility methods //////////////////////////////////////

  protected IdentityService getIdentityService() {
    return getProcessEngine().getIdentityService();
  }

  protected List<String> getUserGroups(String userId) {
    List<Group> userGroups = getIdentityService().createGroupQuery()
        .groupMember(userId)
        .unlimitedList();

    List<String> groupIds = new ArrayList<>();
    for (Group group : userGroups) {
      groupIds.add(group.getId());
    }

    return groupIds;
  }

}
