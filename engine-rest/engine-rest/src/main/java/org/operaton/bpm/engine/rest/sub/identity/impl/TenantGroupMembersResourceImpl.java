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
package org.operaton.bpm.engine.rest.sub.identity.impl;

import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE;

import java.net.URI;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.rest.TenantRestService;
import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.sub.identity.TenantGroupMembersResource;
import org.operaton.bpm.engine.rest.util.PathUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TenantGroupMembersResourceImpl extends AbstractIdentityResource implements TenantGroupMembersResource {

  public TenantGroupMembersResourceImpl(String processEngineName, String resourceId, String rootResourcePath, ObjectMapper objectMapper) {
    super(processEngineName, Resources.TENANT_MEMBERSHIP, resourceId, objectMapper);
    this.relativeRootResourcePath = rootResourcePath;
  }

  @Override
  public void createMembership(String groupId) {
    ensureNotReadOnly();

    groupId = PathUtil.decodePathParam(groupId);
    identityService.createTenantGroupMembership(resourceId, groupId);
  }

  @Override
  public void deleteMembership(String groupId) {
    ensureNotReadOnly();

    groupId = PathUtil.decodePathParam(groupId);
    identityService.deleteTenantGroupMembership(resourceId, groupId);
  }

  @Override
  public ResourceOptionsDto availableOperations(UriInfo context) {
    ResourceOptionsDto dto = new ResourceOptionsDto();

    URI uri = context.getBaseUriBuilder()
        .path(relativeRootResourcePath)
        .path(TenantRestService.PATH)
        .path(resourceId)
        .path(TenantGroupMembersResource.PATH)
        .build();

    if (!identityService.isReadOnly() && isAuthorized(DELETE)) {
      dto.addReflexiveLink(uri, HttpMethod.DELETE, "delete");
    }
    if (!identityService.isReadOnly() && isAuthorized(CREATE)) {
      dto.addReflexiveLink(uri, HttpMethod.PUT, "create");
    }

    return dto;
  }

}
