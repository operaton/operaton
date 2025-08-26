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
import java.util.List;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.rest.GroupRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.dto.identity.GroupDto;
import org.operaton.bpm.engine.rest.dto.identity.GroupQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.identity.GroupResource;
import org.operaton.bpm.engine.rest.sub.identity.impl.GroupResourceImpl;
import org.operaton.bpm.engine.rest.util.PathUtil;
import org.operaton.bpm.engine.rest.util.QueryUtil;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Resources.GROUP;

/**
 * @author Daniel Meyer
 *
 */
public class GroupRestServiceImpl extends AbstractAuthorizedRestResource implements GroupRestService {

  public GroupRestServiceImpl(String engineName, final ObjectMapper objectMapper) {
    super(engineName, GROUP, ANY, objectMapper);
  }

  @Override
  public GroupResource getGroup(String id) {
    id = PathUtil.decodePathParam(id);
    return new GroupResourceImpl(getProcessEngine().getName(), id, relativeRootResourcePath, getObjectMapper());
  }

  @Override
  public List<GroupDto> queryGroups(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    GroupQueryDto queryDto = new GroupQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryGroups(queryDto, firstResult, maxResults);
  }

  @Override
  public List<GroupDto> queryGroups(GroupQueryDto queryDto, Integer firstResult, Integer maxResults) {

    queryDto.setObjectMapper(getObjectMapper());
    GroupQuery query = queryDto.toQuery(getProcessEngine());

    List<Group> resultList = QueryUtil.list(query, firstResult, maxResults);

    return GroupDto.fromGroupList(resultList);
  }

  @Override
  public CountResultDto getGroupCount(UriInfo uriInfo) {
    GroupQueryDto queryDto = new GroupQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryGroupCount(queryDto);
  }

  @Override
  public CountResultDto queryGroupCount(GroupQueryDto queryDto) {
    GroupQuery query = queryDto.toQuery(getProcessEngine());
    long count = query.count();
    return new CountResultDto(count);
  }

  @Override
  public void createGroup(GroupDto groupDto) {
    final IdentityService identityService = getIdentityService();

    if(identityService.isReadOnly()) {
      throw new InvalidRequestException(Status.FORBIDDEN, "Identity service implementation is read-only.");
    }

    Group newGroup = identityService.newGroup(groupDto.getId());
    groupDto.update(newGroup);
    identityService.saveGroup(newGroup);

  }

  @Override
  public ResourceOptionsDto availableOperations(UriInfo context) {

    final IdentityService identityService = getIdentityService();

    UriBuilder baseUriBuilder = context.getBaseUriBuilder()
        .path(relativeRootResourcePath)
        .path(GroupRestService.PATH);

    ResourceOptionsDto resourceOptionsDto = new ResourceOptionsDto();

    // GET /
    URI baseUri = baseUriBuilder.build();
    resourceOptionsDto.addReflexiveLink(baseUri, HttpMethod.GET, "list");

    // GET /count
    URI countUri = baseUriBuilder.clone().path("/count").build();
    resourceOptionsDto.addReflexiveLink(countUri, HttpMethod.GET, "count");

    // POST /create
    if(!identityService.isReadOnly() && isAuthorized(CREATE)) {
      URI createUri = baseUriBuilder.clone().path("/create").build();
      resourceOptionsDto.addReflexiveLink(createUri, HttpMethod.POST, "create");
    }

    return resourceOptionsDto;
  }

  // utility methods //////////////////////////////////////

  protected IdentityService getIdentityService() {
    return getProcessEngine().getIdentityService();
  }

}
