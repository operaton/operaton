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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.util.ResourceTypeUtil;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Daniel Meyer
 *
 */
public class AuthorizationEntity implements Authorization, DbEntity, HasDbRevision, HasDbReferences, Serializable {

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;
  @Serial private static final long serialVersionUID = 1L;

  protected String id;
  protected int revision;

  protected int authorizationType;
  protected int permissions;
  protected String userId;
  protected String groupId;
  protected Integer resourceType;
  protected String resourceId;
  protected Date removalTime;
  protected String rootProcessInstanceId;

  private Set<Permission> cachedPermissions = new HashSet<>();

  public AuthorizationEntity() {
  }

  public AuthorizationEntity(int type) {
    this.authorizationType = type;

    if(authorizationType == AUTH_TYPE_GLOBAL) {
      this.userId = ANY;
    }

    resetPermissions();
  }

  protected void resetPermissions() {
    cachedPermissions = new HashSet<>();

    if(authorizationType == AUTH_TYPE_GLOBAL) {
      this.permissions = Permissions.NONE.getValue();

    } else if(authorizationType == AUTH_TYPE_GRANT) {
      this.permissions = Permissions.NONE.getValue();

    } else if(authorizationType == AUTH_TYPE_REVOKE) {
      this.permissions = Permissions.ALL.getValue();

    } else {
      throw LOG.engineAuthorizationTypeException(authorizationType, AUTH_TYPE_GLOBAL, AUTH_TYPE_GRANT, AUTH_TYPE_REVOKE);
    }
  }

  // grant / revoke methods ////////////////////////////

  @Override
  public void addPermission(Permission p) {
    cachedPermissions.add(p);
    permissions |= p.getValue();
  }

  @Override
  public void removePermission(Permission p) {
    cachedPermissions.add(p);
    permissions &= ~p.getValue();
  }

  @Override
  public boolean isPermissionGranted(Permission p) {
    if(AUTH_TYPE_REVOKE == authorizationType) {
      throw LOG.permissionStateException("isPermissionGranted", "REVOKE");
    }

    ensureNotNull("Authorization 'resourceType' cannot be null", "authorization.getResource()", resourceType);

    if (!ResourceTypeUtil.resourceIsContainedInArray(resourceType, p.getTypes())) {
      return false;
    }
    return (permissions & p.getValue()) == p.getValue();
  }

  @Override
  public boolean isPermissionRevoked(Permission p) {
    if(AUTH_TYPE_GRANT == authorizationType) {
      throw LOG.permissionStateException("isPermissionRevoked", "GRANT");
    }

    ensureNotNull("Authorization 'resourceType' cannot be null", "authorization.getResource()", resourceType);

    if (!ResourceTypeUtil.resourceIsContainedInArray(resourceType, p.getTypes())) {
      return false;
    }
    return (permissions & p.getValue()) != p.getValue();
  }

  @Override
  public boolean isEveryPermissionGranted() {
    if(AUTH_TYPE_REVOKE == authorizationType) {
      throw LOG.permissionStateException("isEveryPermissionGranted", "REVOKE");
    }
    return permissions == Permissions.ALL.getValue();
  }

  @Override
  public boolean isEveryPermissionRevoked() {
    if (authorizationType == AUTH_TYPE_GRANT) {
      throw LOG.permissionStateException("isEveryPermissionRevoked", "GRANT");
    }
    return permissions == 0;
  }

  @Override
  public Permission[] getPermissions(Permission[] permissions) {
    List<Permission> result = new ArrayList<>();
    for (Permission permission : permissions) {
      boolean granted = (AUTH_TYPE_GLOBAL == authorizationType || AUTH_TYPE_GRANT == authorizationType) && isPermissionGranted(permission);
      boolean revoked = AUTH_TYPE_REVOKE == authorizationType && isPermissionRevoked(permission);
      if (granted || revoked) {
        result.add(permission);
      }
    }
    return result.toArray(new Permission[ result.size() ]);
  }

  @Override
  public void setPermissions(Permission[] permissions) {
    resetPermissions();
    for (Permission permission : permissions) {
      if(AUTH_TYPE_REVOKE == authorizationType) {
        removePermission(permission);

      } else {
        addPermission(permission);

      }
    }
  }

  // getters setters ///////////////////////////////

  @Override
  public int getAuthorizationType() {
    return authorizationType;
  }

  public void setAuthorizationType(int authorizationType) {
    this.authorizationType = authorizationType;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public void setGroupId(String groupId) {
    if(groupId != null && authorizationType == AUTH_TYPE_GLOBAL) {
      throw LOG.notUsableGroupIdForGlobalAuthorizationException();
    }
    this.groupId = groupId;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public void setUserId(String userId) {
    if(userId != null && authorizationType == AUTH_TYPE_GLOBAL && !ANY.equals(userId)) {
      throw LOG.illegalValueForUserIdException(userId, ANY);
    }
    this.userId = userId;
  }

  @Override
  public int getResourceType() {
    return resourceType;
  }

  @Override
  public void setResourceType(int type) {
    this.resourceType = type;
  }

  public Integer getResource() {
    return resourceType;
  }

  @Override
  public void setResource(Resource resource) {
    this.resourceType = resource.resourceType();
  }

  @Override
  public String getResourceId() {
    return resourceId;
  }

  @Override
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  public void setPermissions(int permissions) {
    this.permissions = permissions;
  }

  public int getPermissions() {
    return permissions;
  }

  public Set<Permission> getCachedPermissions() {
    return cachedPermissions;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  @Override
  public Object getPersistentState() {

    HashMap<String, Object> state = new HashMap<>();
    state.put("userId", userId);
    state.put("groupId", groupId);
    state.put("resourceType", resourceType);
    state.put("resourceId", resourceId);
    state.put("permissions", permissions);
    state.put("removalTime", removalTime);
    state.put("rootProcessInstanceId", rootProcessInstanceId);

    return state;
  }

  @Override
  public Date getRemovalTime() {
    return removalTime;
  }

  public void setRemovalTime(Date removalTime) {
    this.removalTime = removalTime;
  }

  @Override
  public String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  public void setRootProcessInstanceId(String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=" + id
           + ", revision=" + revision
           + ", authorizationType=" + authorizationType
           + ", permissions=" + permissions
           + ", userId=" + userId
           + ", groupId=" + groupId
           + ", resourceType=" + resourceType
           + ", resourceId=" + resourceId
           + "]";
  }
}
