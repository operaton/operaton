/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.webapp.impl.util;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.Authentication;

/**
 * Decides whether a process engine already has an administrator, so that the web application's
 * first-run setup, which creates the initial administrator without authentication, can be closed.
 *
 * <p>{@link #administratorExists(ProcessEngine)} answers the question, and
 * {@link #findAdministrator(ProcessEngine)} additionally names the administrator that was found,
 * for logging. Both apply every definition of an administrator the engine itself uses; see
 * {@link #findAdministrator(ProcessEngine)} for the rules.</p>
 *
 * @since 2.2
 */
public final @NullMarked class AdministratorUtil {

  private AdministratorUtil() {
  }

  /**
   * @param processEngine the engine to inspect
   * @return {@code true} if an administrator exists for the given engine by any of the rules of
   *         {@link #findAdministrator(ProcessEngine)}
   */
  public static boolean administratorExists(ProcessEngine processEngine) {
    return findAdministrator(processEngine) != null;
  }

  /**
   * Finds an administrator of the given engine. An administrator exists if any of the following
   * holds, evaluated in this order:
   * <ol>
   *   <li>Authorization is disabled and at least one user exists. Every user is unrestricted then,
   *       so an anonymous user-creation endpoint must not stay open.</li>
   *   <li>A user is a member of one of the configured administrator groups
   *       ({@link ProcessEngineConfigurationImpl#getAdminGroups()}, always including
   *       {@code operaton-admin}).</li>
   *   <li>One of the configured administrator users
   *       ({@link ProcessEngineConfigurationImpl#getAdminUsers()}) exists. A configured user that
   *       has not been created yet does not count, so that operators are not locked out of the
   *       setup page on a fresh installation.</li>
   *   <li>A {@code GRANT} or {@code GLOBAL} authorization with {@link Permissions#ALL} on all
   *       {@link Resources#AUTHORIZATION} resources exists, because its holder can grant any other
   *       permission to anyone. Every administrator bootstrap path writes such a row. A grant for a
   *       user always counts, as does a grant for a group that is not stored locally (groups managed
   *       by an external identity provider). A grant for a locally stored group that has no members
   *       does not count, so that removing the last administrator from the group reopens the setup
   *       page as before. A global grant counts as soon as any user exists.</li>
   * </ol>
   *
   * <p>Two consequences of the last rule are accepted on purpose. Deleting a user or a group does
   * not delete the grants it holds, so a stale grant for a deleted user or group keeps counting as
   * an administrator and keeps the setup page closed until the grant is removed; failing closed is
   * preferred over reopening an unauthenticated endpoint. And only {@link Permissions#ALL} on all
   * authorizations counts, although a holder of a partial permission such as {@code CREATE} could
   * grant themselves the rest; such partial grants are not written by any bootstrap path.</p>
   *
   * <p>The queries run with the current authentication cleared and restore it afterwards, because
   * identity and authorization queries are themselves subject to authorization checks.</p>
   *
   * @param processEngine the engine to inspect
   * @return a human-readable description of the first administrator found, suitable for logging,
   *         or {@code null} if no administrator exists
   */
  public static @Nullable String findAdministrator(ProcessEngine processEngine) {
    IdentityService identityService = processEngine.getIdentityService();
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    try {
      identityService.clearAuthentication();
      return findAdministrator((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration(),
          identityService, processEngine.getAuthorizationService());
    } finally {
      identityService.setAuthentication(currentAuthentication);
    }
  }

  static @Nullable String findAdministrator(ProcessEngineConfigurationImpl configuration,
      IdentityService identityService, AuthorizationService authorizationService) {

    if (!configuration.isAuthorizationEnabled()) {
      if (identityService.createUserQuery().count() > 0) {
        return "authorization is disabled and a user exists";
      }
      return null;
    }

    List<String> adminGroups = configuration.getAdminGroups();
    if (adminGroups != null) {
      for (String adminGroup : adminGroups) {
        if (identityService.createUserQuery().memberOfGroup(adminGroup).count() > 0) {
          return "a member of the administrator group '" + adminGroup + "' exists";
        }
      }
    }

    List<String> adminUsers = configuration.getAdminUsers();
    if (adminUsers != null && !adminUsers.isEmpty()
        && identityService.createUserQuery().userIdIn(adminUsers.toArray(String[]::new)).count() > 0) {
      return "a configured administrator user exists";
    }

    return authorizationService.createAuthorizationQuery()
        .resourceType(Resources.AUTHORIZATION)
        .resourceId(Authorization.ANY)
        .hasPermission(Permissions.ALL)
        .list()
        .stream()
        .map(grant -> describeAdministratorGrant(grant, identityService))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static @Nullable String describeAdministratorGrant(Authorization grant, IdentityService identityService) {
    if (grant.getAuthorizationType() == Authorization.AUTH_TYPE_REVOKE) {
      return null;
    }

    String groupId = grant.getGroupId();
    if (groupId != null) {
      // a group that is not stored locally is either managed by an identity provider or was deleted
      // while its grant stayed behind; both count, see the Javadoc of findAdministrator
      boolean storedLocally = identityService.createGroupQuery().groupId(groupId).count() > 0;
      boolean hasMembers = identityService.createUserQuery().memberOfGroup(groupId).count() > 0;
      if (storedLocally && !hasMembers) {
        return null;
      }
      return "the group '" + groupId + "' holds all permissions on all authorizations";
    }

    String userId = grant.getUserId();
    if (Authorization.ANY.equals(userId)) {
      if (identityService.createUserQuery().count() > 0) {
        return "all users hold all permissions on all authorizations";
      }
      return null;
    }
    if (userId != null) {
      return "the user '" + userId + "' holds all permissions on all authorizations";
    }

    return null;
  }

}
