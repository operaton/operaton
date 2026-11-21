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
package org.operaton.bpm.admin.impl.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserDto;
import org.operaton.bpm.engine.rest.dto.identity.UserProfileDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * The first-run setup resource answers without authentication by necessity, so the guard that
 * closes it once an administrator exists is the security-relevant part and is covered here,
 * including administrators that exist without a membership in {@code operaton-admin}.
 */
class SetupResourceTest {

  protected static final String ENGINE_NAME = "webapps-test-engine";
  protected static final String EXTRA_ADMIN_GROUP = "ops";

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder()
      .configurationResource("operaton-test-engine.cfg.xml").build();

  ProcessEngineConfigurationImpl processEngineConfiguration;
  IdentityService identityService;
  AuthorizationService authorizationService;

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
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
  void shouldCreateTheInitialAdministrator() throws Exception {
    // given an empty identity store

    // when
    new SetupResource().createInitialUser(ENGINE_NAME, userDto("kermit", "kermitspassword"));

    // then
    assertThat(identityService.createUserQuery().userId("kermit").count()).isOne();
    assertThat(identityService.createUserQuery().memberOfGroup(Groups.OPERATON_ADMIN).count()).isOne();
    assertThat(authorizationService.createAuthorizationQuery()
      .groupIdIn(Groups.OPERATON_ADMIN)
      .resourceType(Resources.AUTHORIZATION)
      .resourceId(Authorization.ANY)
      .count()).isOne();
  }

  @Test
  void shouldRejectASecondInitialUser() throws Exception {
    // given
    new SetupResource().createInitialUser(ENGINE_NAME, userDto("kermit", "kermitspassword"));

    // then
    assertSetupClosed();
  }

  @Test
  void shouldRejectInitialUserWhenConfiguredAdministratorUserExists() throws Exception {
    // given
    processEngineConfiguration.setAuthorizationEnabled(true);
    processEngineConfiguration.getAdminUsers().add("admin");
    createUser("admin");

    // then
    assertSetupClosed();
  }

  @Test
  void shouldRejectInitialUserWhenMemberOfConfiguredAdministratorGroupExists() throws Exception {
    // given
    processEngineConfiguration.setAuthorizationEnabled(true);
    processEngineConfiguration.getAdminGroups().add(EXTRA_ADMIN_GROUP);
    Group group = identityService.newGroup(EXTRA_ADMIN_GROUP);
    identityService.saveGroup(group);
    createUser("kermit");
    identityService.createMembership("kermit", EXTRA_ADMIN_GROUP);

    // then
    assertSetupClosed();
  }

  @Test
  void shouldRejectInitialUserWhenUserHoldsAllAuthorizationPermissions() throws Exception {
    // given
    processEngineConfiguration.setAuthorizationEnabled(true);
    createUser("kermit");
    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setUserId("kermit");
    authorization.setResource(Resources.AUTHORIZATION);
    authorization.setResourceId(Authorization.ANY);
    authorization.addPermission(Permissions.ALL);
    authorizationService.saveAuthorization(authorization);

    // then
    assertSetupClosed();
  }

  @Test
  void shouldRejectInitialUserWhenExternalGroupHoldsAllAuthorizationPermissions() throws Exception {
    // given a group managed by an identity provider: authorizations exist, no local group or members
    processEngineConfiguration.setAuthorizationEnabled(true);
    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setGroupId("idp-admins");
    authorization.setResource(Resources.AUTHORIZATION);
    authorization.setResourceId(Authorization.ANY);
    authorization.addPermission(Permissions.ALL);
    authorizationService.saveAuthorization(authorization);

    // then
    assertSetupClosed();
  }

  @Test
  void shouldRejectInitialUserWhenAnyUserExistsWithoutAuthorization() throws Exception {
    // given authorization disabled, every user is unrestricted
    processEngineConfiguration.setAuthorizationEnabled(false);
    createUser("kermit");

    // then
    assertSetupClosed();
  }

  protected void assertSetupClosed() throws Exception {
    InvalidRequestException exception = catchThrowableOfType(InvalidRequestException.class,
        () -> new SetupResource().createInitialUser(ENGINE_NAME, userDto("gonzo", "gonzospassword")));

    assertThat(exception).isNotNull();
    assertThat(exception.getStatus().getStatusCode()).isEqualTo(403);
    assertThat(identityService.createUserQuery().userId("gonzo").count()).isZero();
  }

  protected void createUser(String id) {
    User user = identityService.newUser(id);
    user.setPassword(id + "spassword");
    identityService.saveUser(user);
  }

  protected UserDto userDto(String id, String password) {
    UserProfileDto profile = new UserProfileDto();
    profile.setId(id);
    profile.setFirstName("First");
    profile.setLastName("Last");
    profile.setEmail(id + "@localhost");

    UserCredentialsDto credentials = new UserCredentialsDto();
    credentials.setPassword(password);

    UserDto user = new UserDto();
    user.setProfile(profile);
    user.setCredentials(credentials);
    return user;
  }

}
