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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GLOBAL;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_REVOKE;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.AUTHORIZATION;
import static org.operaton.bpm.engine.authorization.Resources.USER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AdministratorUtil} backs the first-run setup guard. Each rule of its
 * administrator definition is covered here, including the deliberate exceptions that keep the
 * setup page reachable after the last administrator was removed.
 */
class AdministratorUtilTest {

  protected static final String EXTRA_ADMIN_GROUP = "ops";

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder()
      .configurationResource("operaton-test-engine.cfg.xml").build();

  ProcessEngine processEngine;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  IdentityService identityService;
  AuthorizationService authorizationService;

  @BeforeEach
  void setUp() {
    processEngine = processEngineExtension.getProcessEngine();
    processEngineConfiguration.setAuthorizationEnabled(true);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    identityService.clearAuthentication();
    processEngineConfiguration.getAdminUsers().clear();
    processEngineConfiguration.getAdminGroups().remove(EXTRA_ADMIN_GROUP);

    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  void shouldFindNoAdministratorInEmptyStore() {
    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
    assertThat(AdministratorUtil.findAdministrator(processEngine)).isNull();
  }

  @Test
  void shouldFindNoAdministratorWithoutUsersWhenAuthorizationIsDisabled() {
    processEngineConfiguration.setAuthorizationEnabled(false);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldTreatAnyUserAsAdministratorWhenAuthorizationIsDisabled() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    createUser("kermit");

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains("authorization is disabled");
  }

  @Test
  void shouldFindMemberOfDefaultAdministratorGroup() {
    createUser("kermit");
    createGroup(Groups.OPERATON_ADMIN);
    identityService.createMembership("kermit", Groups.OPERATON_ADMIN);

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains(Groups.OPERATON_ADMIN);
  }

  @Test
  void shouldIgnoreConfiguredAdministratorGroupWithoutMembers() {
    processEngineConfiguration.getAdminGroups().add(EXTRA_ADMIN_GROUP);
    createGroup(EXTRA_ADMIN_GROUP);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldFindMemberOfConfiguredAdministratorGroup() {
    processEngineConfiguration.getAdminGroups().add(EXTRA_ADMIN_GROUP);
    createGroup(EXTRA_ADMIN_GROUP);
    createUser("kermit");
    identityService.createMembership("kermit", EXTRA_ADMIN_GROUP);

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains(EXTRA_ADMIN_GROUP);
  }

  @Test
  void shouldIgnoreConfiguredAdministratorUserThatDoesNotExist() {
    processEngineConfiguration.getAdminUsers().add("admin");
    createUser("kermit");

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldFindConfiguredAdministratorUser() {
    processEngineConfiguration.getAdminUsers().add("admin");
    createUser("admin");

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains("configured administrator user");
  }

  @Test
  void shouldFindUserHoldingAllAuthorizationPermissions() {
    createUser("kermit");
    createGrant(AUTH_TYPE_GRANT, "kermit", null, AUTHORIZATION, ALL);

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains("user 'kermit'");
  }

  @Test
  void shouldFindGrantForUserThatIsNotStoredLocally() {
    createGrant(AUTH_TYPE_GRANT, "idp-admin", null, AUTHORIZATION, ALL);

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains("user 'idp-admin'");
  }

  @Test
  void shouldIgnoreGrantForLocalGroupWithoutMembers() {
    createGroup(Groups.OPERATON_ADMIN);
    createGrant(AUTH_TYPE_GRANT, null, Groups.OPERATON_ADMIN, AUTHORIZATION, ALL);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldFindGrantForLocalGroupWithMembers() {
    createGroup(Groups.OPERATON_ADMIN);
    createGrant(AUTH_TYPE_GRANT, null, Groups.OPERATON_ADMIN, AUTHORIZATION, ALL);
    createUser("kermit");
    identityService.createMembership("kermit", Groups.OPERATON_ADMIN);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isTrue();
  }

  @Test
  void shouldFindGrantForGroupThatIsNotStoredLocally() {
    createGrant(AUTH_TYPE_GRANT, null, "idp-admins", AUTHORIZATION, ALL);

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains("group 'idp-admins'");
  }

  @Test
  void shouldIgnoreGlobalGrantWithoutUsers() {
    createGrant(AUTH_TYPE_GLOBAL, ANY, null, AUTHORIZATION, ALL);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldFindGlobalGrantWhenAUserExists() {
    createGrant(AUTH_TYPE_GLOBAL, ANY, null, AUTHORIZATION, ALL);
    createUser("kermit");

    assertThat(AdministratorUtil.findAdministrator(processEngine)).contains("all users");
  }

  @Test
  void shouldIgnoreRevokedAuthorizationPermissions() {
    createUser("kermit");
    createGrant(AUTH_TYPE_REVOKE, "kermit", null, AUTHORIZATION, ALL);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldIgnorePartialAuthorizationPermissions() {
    createUser("kermit");
    createGrant(AUTH_TYPE_GRANT, "kermit", null, AUTHORIZATION, READ);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldIgnoreAllPermissionsOnOtherResources() {
    createUser("kermit");
    createGrant(AUTH_TYPE_GRANT, "kermit", null, USER, ALL);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isFalse();
  }

  @Test
  void shouldIgnoreAndPreserveCallerAuthentication() {
    createUser("kermit");
    createGrant(AUTH_TYPE_GRANT, "kermit", null, AUTHORIZATION, ALL);
    identityService.setAuthentication("nobody", null);

    assertThat(AdministratorUtil.administratorExists(processEngine)).isTrue();
    assertThat(identityService.getCurrentAuthentication().getUserId()).isEqualTo("nobody");
  }

  protected void createUser(String userId) {
    User user = identityService.newUser(userId);
    user.setPassword(userId);
    identityService.saveUser(user);
  }

  protected void createGroup(String groupId) {
    Group group = identityService.newGroup(groupId);
    group.setName(groupId);
    identityService.saveGroup(group);
  }

  protected void createGrant(int type, String userId, String groupId, Resource resource, Permission permission) {
    Authorization authorization = authorizationService.createNewAuthorization(type);
    if (userId != null) {
      authorization.setUserId(userId);
    }
    if (groupId != null) {
      authorization.setGroupId(groupId);
    }
    authorization.setResource(resource);
    authorization.setResourceId(ANY);
    authorization.addPermission(permission);
    authorizationService.saveAuthorization(authorization);
  }

}
