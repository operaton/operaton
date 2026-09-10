/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.identity.IdentityOperationResult;
import org.operaton.bpm.engine.impl.identity.IdentityProviderException;
import org.operaton.bpm.engine.impl.identity.WritableIdentityProvider;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.operaton.bpm.engine.impl.context.Context.getCommandContext;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * SCIM Identity Provider Session implementing ReadOnlyIdentityProvider.
 */
public class ScimIdentityProviderWritable extends ScimIdentityProviderReadOnly implements WritableIdentityProvider { 

  public ScimIdentityProviderWritable(ScimConfiguration scimConfiguration) {
    super(scimConfiguration);
  }

  public ScimIdentityProviderWritable(ScimConfiguration scimConfiguration, ScimSimpleCache<JsonNode> responseCache, 
      ScimSimpleCache<String> userCache) {
    super(scimConfiguration, responseCache, userCache);
  }

  public ScimIdentityProviderWritable(ScimConfiguration scimConfiguration, ScimSimpleCache<JsonNode> responseCache, 
      ScimSimpleCache<String> userCache, ScimOAuth2TokenStore oauth2TokenStore) {
    super(scimConfiguration, responseCache, userCache, oauth2TokenStore);
  }

  // Session Lifecycle

  @Override
  public void flush() {
    super.flush();
  }

  @Override
  public void close() {
    super.close();
  }

  // Users
  
  @Override
  public User createNewUser(String userId) {
    return new ScimUserEntity(userId);
  }
 
  @Override
  public IdentityOperationResult saveUser(User user) {
    ScimUserEntity scimUserNew = (ScimUserEntity) user;
    String operation = IdentityOperationResult.OPERATION_NONE;
    if(scimUserNew.getScimId() == null || scimUserNew.getScimId().isEmpty()) {
      operation = IdentityOperationResult.OPERATION_CREATE;
      checkAuthorization(Permissions.CREATE, Resources.USER, null);
      String postUrl = scimConfiguration.getServerUrl() + scimConfiguration.getUsersEndpoint();
      JsonNode postBody = transformUser(scimUserNew);
      scimClient.executePost(postUrl, postBody);
      //createDefaultAuthorizations(user);
    } else {
      ScimUserEntity scimUserOld = (ScimUserEntity)findUserById(scimUserNew.getId());
      if (scimUserOld != null) {
        // use op_update for ID update and op_none for other attribs update to prevent not needed auth update
        if (!scimUserNew.getId().equals(scimUserOld.getId())) {
          operation = IdentityOperationResult.OPERATION_UPDATE;
        }
        checkAuthorization(Permissions.UPDATE, Resources.USER, user.getId());
        JsonNode patchBody = createUserPatch(scimUserOld, scimUserNew);     
        scimClient.patchUserByScimId(scimUserNew.getScimId(), patchBody);
      }
    }

    return new IdentityOperationResult(user, operation);
  }

  @Override
  public IdentityOperationResult deleteUser(String userId) {
    checkAuthorization(Permissions.DELETE, Resources.USER, userId);
    ScimUserEntity scimUser = (ScimUserEntity)findUserById(userId);
    if(scimUser != null) {
      // remove authorizations for the group from the database
      getCommandContext().getAuthorizationManager().deleteAuthorizationsByResourceId(Resources.USER, userId);
      // remove the user itself
      scimClient.deleteUserByScimId(scimUser.getScimId());
      return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_DELETE);
    }
    return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  @Override
  public IdentityOperationResult unlockUser(String userId) {
    // Unlocking users is not supported via SCIM; treat as a no-op.
    return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  // Groups
  
  @Override
  public Group createNewGroup(String groupId) {
    return new ScimGroupEntity(groupId);
  }

  @Override
  public IdentityOperationResult saveGroup(Group group) {
    ScimGroupEntity scimGroupNew =  (ScimGroupEntity) group;
    String operation = IdentityOperationResult.OPERATION_NONE;
    if (scimGroupNew.getScimId() == null || scimGroupNew.getScimId().isEmpty()) {
      operation = IdentityOperationResult.OPERATION_CREATE;
      checkAuthorization(Permissions.CREATE, Resources.GROUP, null);
      String postUrl = scimConfiguration.getServerUrl() + scimConfiguration.getGroupsEndpoint();
      JsonNode postBody = transformGroup(scimGroupNew);
      scimClient.executePost(postUrl, postBody);
      // createDefaultAuthorizations(group);
    } else {
      ScimGroupEntity scimGroupOld = (ScimGroupEntity)findGroupById(scimGroupNew.getId());
      if (scimGroupOld != null) {
        // use op_update for ID update and op_none for other attribs update to prevent not needed auth update
        if (!scimGroupNew.getId().equals(scimGroupOld.getId())) {
          operation = IdentityOperationResult.OPERATION_UPDATE;
        }
        checkAuthorization(Permissions.UPDATE, Resources.GROUP, group.getId());
        JsonNode patchBody = createGroupPatch(scimGroupOld, scimGroupNew);
        scimClient.patchGroupByScimId(scimGroupNew.getScimId(), patchBody);
      }
    }

    return new IdentityOperationResult(group, operation);
  }

  @Override
  public IdentityOperationResult deleteGroup(String groupId) {
    checkAuthorization(Permissions.DELETE, Resources.GROUP, groupId);
    ScimGroupEntity scimGroup =  (ScimGroupEntity) findGroupById(groupId);
    if(scimGroup != null) {
      // remove authorizations for the group from the database
      getCommandContext().getAuthorizationManager().deleteAuthorizationsByResourceId(Resources.GROUP, groupId);
      // remove the group itself from the scim servcice
      scimClient.deleteGroupByScimId(scimGroup.getScimId());
      return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_DELETE);
    }
    
    return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  // Tenants: not used by SCIM identity provider
  
  @Override
  public Tenant createNewTenant(String tenantId) {
    throw new IdentityProviderException("This operation is not supported for SCIM identity provider.");
  }

  @Override
  public IdentityOperationResult saveTenant(Tenant tenant) {
    throw new IdentityProviderException("This operation is not supported for SCIM identity provider.");
  }

  @Override
  public IdentityOperationResult deleteTenant(String tenantId) {
    throw new IdentityProviderException("This operation is not supported for SCIM identity provider.");
  }

  // Memberships
  
  @Override
  public IdentityOperationResult createMembership(String userId, String groupId) {
    checkAuthorization(Permissions.CREATE, Resources.GROUP_MEMBERSHIP, groupId);
    ScimUserEntity user = (ScimUserEntity)findUserById(userId);
    ensureNotNull("No user found with id '" + userId + "'.", "user", user);
    ScimGroupEntity group = (ScimGroupEntity)findGroupById(groupId);
    ensureNotNull("No group found with id '" + groupId + "'.", "group", group);
    
    if (!isGroupMember(group, user)) {
      JsonNode patchBody = createMembershipPatch(user.getScimId(), "add", "User");
      scimClient.patchGroupByScimId(group.getScimId(), patchBody);
      //createDefaultMembershipAuthorizations(userId, groupId);
      return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_CREATE);
    }
    return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  @Override
  public IdentityOperationResult deleteMembership(String userId, String groupId) {
    checkAuthorization(Permissions.DELETE, Resources.GROUP_MEMBERSHIP, groupId);
    ScimUserEntity user = (ScimUserEntity)findUserById(userId);
    ensureNotNull("No user found with id '" + userId + "'.", "user", user);
    ScimGroupEntity group = (ScimGroupEntity)findGroupById(groupId);
    ensureNotNull("No group found with id '" + groupId + "'.", "group", group);
    
    if (isGroupMember(group, user)) {
      JsonNode patchBody = createMembershipPatch(user.getScimId(), "remove", null);
      scimClient.patchGroupByScimId(group.getScimId(), patchBody);
      return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_DELETE);
    }
    return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  public void deleteMembershipsByUserId(String userId) {
    ScimGroupQuery groupQuery = (ScimGroupQuery) createGroupQuery();
    groupQuery.groupMember(userId);
    List<Group> userGroups = findGroupByQueryCriteria(groupQuery);
    for (Group group : userGroups) {
      deleteMembership(userId, group.getId());
    }
  }

  @Override
  public IdentityOperationResult createTenantUserMembership(String tenantId, String userId) {
    throw new IdentityProviderException("This operation is not supported for SCIM identity provider.");
    // return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  @Override
  public IdentityOperationResult createTenantGroupMembership(String tenantId, String groupId) {
    throw new IdentityProviderException("This operation is not supported for SCIM identity provider.");
    //  return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  @Override
  public IdentityOperationResult deleteTenantUserMembership(String tenantId, String userId) {
    // throw new IdentityProviderException("This operation is not supported for SCIM identity provider.");
    return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }

  @Override
  public IdentityOperationResult deleteTenantGroupMembership(String tenantId, String groupId) {
    // throw new IdentityProviderException("This operation is not supported for SCIM identity provider.");
    return new IdentityOperationResult(null, IdentityOperationResult.OPERATION_NONE);
  }
  
  protected JsonNode createUserPatch(ScimUserEntity before, ScimUserEntity after) {
    // transform both to the Json and prepare simplest patch
    return createDiffNode(transformUser(before), transformUser(after));
  }

  protected JsonNode createGroupPatch(ScimGroupEntity before, ScimGroupEntity after) {
    // transform both to the Json and prepare simplest patch
    return createDiffNode(transformGroup(before), transformGroup(after));
  }
  
  // patch for a member: operation is add/remove/update, membership type is User/Group 
  protected JsonNode createMembershipPatch(String memberScimId, String operation, String memberType) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode schemas = mapper.createArrayNode().add("urn:ietf:params:scim:api:messages:2.0:PatchOp");
    ArrayNode operations = mapper.createArrayNode();
 
    root.set("schemas", schemas);
    root.set("Operations", operations);

    ObjectNode entry = operations.addObject();
    entry.put("op", operation);
    entry.put("path", "members");

    ArrayNode members = mapper.createArrayNode();
    ObjectNode membership = members.addObject();
    membership.put("value", memberScimId);
    if (!operation.equalsIgnoreCase("remove") && memberType != null && !memberType.isEmpty()) {
      membership.put("type", memberType);
    }
    entry.set("value", members);

    return root;
  }
  
  protected JsonNode createDiffNode(JsonNode first, JsonNode second) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode schemas = mapper.createArrayNode().add("urn:ietf:params:scim:api:messages:2.0:PatchOp");
    ArrayNode operations = mapper.createArrayNode();
    
    root.set("schemas", schemas);
    root.set("Operations", operations);
    
    // simplest for now: compare textual representation of the top-level values
    HashSet<String> topLevelFields = new HashSet<>();
    Iterator<Map.Entry<String, JsonNode>> itOld = first.fields();
    while (itOld.hasNext()) {
      topLevelFields.add(itOld.next().getKey());
    }

    Iterator<Map.Entry<String, JsonNode>> itNew = second.fields();
    while (itNew.hasNext()) {
      topLevelFields.add(itNew.next().getKey());
    }
     
    for (String field : topLevelFields) {
      JsonNode oldVal = first.get(field);
      JsonNode newVal = second.get(field);

      if (oldVal == null) {
        ObjectNode entry = operations.addObject();
        entry.put("op", "add");
          entry.put("path", field);
          entry.set("value", newVal);
      } else if (newVal == null) {
        ObjectNode entry = operations.addObject();
        entry.put("op", "remove");
          entry.put("path", field);
      } else if (!oldVal.toString().equals(newVal.toString())) {
        ObjectNode entry = operations.addObject();
        entry.put("op", "replace");
          entry.put("path", field);
          entry.set("value", newVal);
      }
    }
    
    return root;
  }
  
  protected void checkAuthorization(Permission permission, Resource resource, String resourceId) {
  if (scimConfiguration.isAuthorizationCheckEnabled()) {
         getCommandContext().getAuthorizationManager().checkAuthorization(permission, resource, resourceId);
    }
  }
}
