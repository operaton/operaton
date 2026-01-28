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
package org.operaton.bpm.engine.test.api.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.MissingAuthorization;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.TenantQuery;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TenantEntity;
import org.operaton.bpm.engine.impl.persistence.entity.UserEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GLOBAL;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_REVOKE;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.GROUP;
import static org.operaton.bpm.engine.authorization.Resources.GROUP_MEMBERSHIP;
import static org.operaton.bpm.engine.authorization.Resources.TENANT;
import static org.operaton.bpm.engine.authorization.Resources.TENANT_MEMBERSHIP;
import static org.operaton.bpm.engine.authorization.Resources.USER;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestUtil.assertExceptionInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class IdentityServiceAuthorizationsTest {

  private static final String USER_ID = "jonny2";

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected AuthorizationService authorizationService;
  protected IdentityService identityService;

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    cleanupAfterTest();

  }

  @Test
  void shouldCreateTransientUserWithoutPermission() {
    // given nobody has CREATE permission on USER resource
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(USER);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // when/then
    assertThatCode(() -> identityService.newUser("jonny1"))
      .doesNotThrowAnyException();
  }

  @Test
  void testUserInsertionAuthorizations() {

    // add base permission which allows nobody to create users:
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(USER);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // given
    User newUser = identityService.newUser("jonny1");

    // when/then
    assertThatThrownBy(() -> identityService.saveUser(newUser))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException authEx = (AuthorizationException) e;
        assertThat(authEx.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = authEx.getMissingAuthorizations().get(0);
        assertThat(authEx.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(CREATE.getName(), USER.resourceName(), null, info);
      });
  }

  @Test
  void testUserDeleteAuthorizations() {

    // crate user while still in god-mode:
    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(USER);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(DELETE); // revoke delete
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // when/then
    assertThatThrownBy(() -> identityService.deleteUser("jonny1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException authEx = (AuthorizationException) e;
        assertThat(authEx.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = authEx.getMissingAuthorizations().get(0);
        assertThat(authEx.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(DELETE.getName(), USER.resourceName(), "jonny1", info);
      });
  }

  @Test
  void testTenantAuthorizationAfterDeleteUser() {
    // given jonny2 who is allowed to do user operations
    User jonny = identityService.newUser(USER_ID);
    identityService.saveUser(jonny);

    grantPermissions();

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // create user
    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);
    String jonny1Id = jonny1.getId();

    // and tenant
    String tenant1 = "tenant1";
    Tenant tenant = identityService.newTenant(tenant1);
    identityService.saveTenant(tenant);
    identityService.createTenantUserMembership(tenant1, jonny1Id);

    // assume
    TenantQuery query = identityService.createTenantQuery().userMember(jonny1Id);
    assertThat(query.count()).isOne();

    // when
    identityService.deleteUser(jonny1Id);

    // turn off authorization
    processEngineConfiguration.setAuthorizationEnabled(false);

    // then
    assertThat(query.count()).isZero();
    assertThat(authorizationService.createAuthorizationQuery().resourceType(TENANT).userIdIn(jonny1Id).count()).isZero();
  }

  @Test
  void testUserUpdateAuthorizations() {

    // insert user while still in god-mode:
    identityService.saveUser(identityService.newUser("jonny1"));

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(USER);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(UPDATE); // revoke update
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // fetch user:
    User jonny1 = identityService.createUserQuery().singleResult();
    jonny1.setFirstName("Jonny");

    assertThatThrownBy(() -> identityService.saveUser(jonny1))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(UPDATE.getName(), USER.resourceName(), "jonny1", info);
      });

    // but I can create a new user:
    identityService.saveUser(identityService.newUser("jonny3"));

  }

  @Test
  void testUserUnlock() {

    // crate user while still in god-mode:
    String userId = "jonny";
    User jonny = identityService.newUser(userId);
    jonny.setPassword("xxx");
    identityService.saveUser(jonny);

    lockUser(userId, "invalid pwd");

    // assume
    int maxNumOfAttempts = 10;
    UserEntity lockedUser = (UserEntity) identityService.createUserQuery().userId(jonny.getId()).singleResult();
    assertThat(lockedUser).isNotNull();
    assertThat(lockedUser.getLockExpirationTime()).isNotNull();
    assertThat(lockedUser.getAttempts()).isEqualTo(maxNumOfAttempts);


    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(USER);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    authorizationService.saveAuthorization(basePerms);

    // set auth
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthentication("admin", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    // when
    identityService.unlockUser(lockedUser.getId());

    // then
    lockedUser = (UserEntity) identityService.createUserQuery().userId(jonny.getId()).singleResult();
    assertThat(lockedUser).isNotNull();
    assertThat(lockedUser.getLockExpirationTime()).isNull();
    assertThat(lockedUser.getAttempts()).isZero();
  }

  @Test
  void testUserUnlockWithoutAuthorization() {
    // create user while still in god-mode:
    String userId = "jonny";
    User jonny = identityService.newUser(userId);
    jonny.setPassword("xxx");
    identityService.saveUser(jonny);

    lockUser(userId, "invalid pwd");

    // assume
    int maxNumOfAttempts = 10;
    UserEntity lockedUser = (UserEntity) identityService.createUserQuery().userId(jonny.getId()).singleResult();
    assertThat(lockedUser).isNotNull();
    assertThat(lockedUser.getLockExpirationTime()).isNotNull();
    assertThat(lockedUser.getAttempts()).isEqualTo(maxNumOfAttempts);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthentication("admin", null, null);
    var lockedUserId = lockedUser.getId();

    // when
    assertThatThrownBy(() -> identityService.unlockUser(lockedUserId))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMessage()).contains("ENGINE-03029 Required admin authenticated group or user.");
      });

    // return to god-mode
    processEngineConfiguration.setAuthorizationEnabled(false);

    // then
    int maxNumOfLoginAttempts = 10;
    lockedUser = (UserEntity) identityService.createUserQuery().userId(jonny.getId()).singleResult();
    assertThat(lockedUser).isNotNull();
    assertThat(lockedUser.getLockExpirationTime()).isNotNull();
    assertThat(lockedUser.getAttempts()).isEqualTo(maxNumOfLoginAttempts);
  }

  @Test
  void shouldCreateTransientGroupWithoutPermission() {
    // given nobody has CREATE permission on GROUP resource
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(GROUP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // when
    assertThatCode(() -> identityService.newGroup("group1"))
      .doesNotThrowAnyException();
  }

  @Test
  void testGroupInsertionAuthorizations() {

    // add base permission which allows nobody to create groups:
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(GROUP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    Group group = identityService.newGroup("group1");

    assertThatThrownBy(() -> identityService.saveGroup(group))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(CREATE.getName(), GROUP.resourceName(), null, info);
      });
  }

  @Test
  void testGroupDeleteAuthorizations() {

    // crate group while still in god-mode:
    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(GROUP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(DELETE); // revoke delete
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.deleteGroup("group1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(DELETE.getName(), GROUP.resourceName(), "group1", info);
      });

  }

  @Test
  void testTenantAuthorizationAfterDeleteGroup() {
    // given jonny2 who is allowed to do group operations
    User jonny = identityService.newUser(USER_ID);
    identityService.saveUser(jonny);

    grantPermissions();

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // create group
    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    // and tenant
    String tenant1 = "tenant1";
    Tenant tenant = identityService.newTenant(tenant1);
    identityService.saveTenant(tenant);
    identityService.createTenantGroupMembership(tenant1, "group1");

    // assume
    TenantQuery query = identityService.createTenantQuery().groupMember("group1");
    assertThat(query.count()).isOne();

    // when
    identityService.deleteGroup("group1");

    // turn off authorization
    processEngineConfiguration.setAuthorizationEnabled(false);

    // then
    assertThat(query.count()).isZero();
    assertThat(authorizationService.createAuthorizationQuery().resourceType(TENANT).groupIdIn("group1").count()).isZero();
  }


  @Test
  void testGroupUpdateAuthorizations() {

    // crate group while still in god-mode:
    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(GROUP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(UPDATE); // revoke update
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // fetch user:
    group1 = identityService.createGroupQuery().singleResult();
    group1.setName("Group 1");

    assertThatThrownBy(() -> identityService.saveGroup(group1))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(UPDATE.getName(), GROUP.resourceName(), "group1", info);
      });

    // but I can create a new group:
    Group group2 = identityService.newGroup("group2");
    identityService.saveGroup(group2);

  }

  @Test
  void shouldCreateTransientTenantWithoutPermission() {
    // given nobody has CREATE permission on TENANT resource
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // when
    assertThatCode(() -> identityService.newTenant("tenant"))
      .doesNotThrowAnyException();
  }

  @Test
  void testTenantInsertionAuthorizations() {

    // add base permission which allows nobody to create tenants:
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    Tenant tenant = identityService.newTenant("tenant");

    assertThatThrownBy(() -> identityService.saveTenant(tenant))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(CREATE.getName(), TENANT.resourceName(), null, info);
      });
  }

  @Test
  void testTenantDeleteAuthorizations() {

    // create tenant
    Tenant tenant = new TenantEntity("tenant");
    identityService.saveTenant(tenant);

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(DELETE); // revoke delete
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.deleteTenant("tenant"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(DELETE.getName(), TENANT.resourceName(), "tenant", info);
      });
  }

  @Test
  void testTenantUpdateAuthorizations() {

    // create tenant
    Tenant tenant = new TenantEntity("tenant");
    identityService.saveTenant(tenant);

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(UPDATE); // revoke update
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    // fetch user:
    tenant = identityService.createTenantQuery().singleResult();
    tenant.setName("newName");

    assertThatThrownBy(() -> identityService.saveTenant(tenant))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(UPDATE.getName(), TENANT.resourceName(), "tenant", info);
      });

    // but I can create a new tenant:
    Tenant newTenant = identityService.newTenant("newTenant");
    identityService.saveTenant(newTenant);
  }

  @Test
  void testMembershipCreateAuthorizations() {

    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);

    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    // add base permission which allows nobody to add users to groups
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(GROUP_MEMBERSHIP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'crate'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.createMembership("jonny1", "group1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(CREATE.getName(), GROUP_MEMBERSHIP.resourceName(), "group1", info);
      });
  }

  @Test
  void testMembershipDeleteAuthorizations() {

    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);

    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    // add base permission which allows nobody to add users to groups
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(GROUP_MEMBERSHIP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'delete'
    basePerms.removePermission(DELETE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.deleteMembership("jonny1", "group1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(DELETE.getName(), GROUP_MEMBERSHIP.resourceName(), "group1", info);
      });
  }

  @Test
  void shouldKeepAuthorizationsForAnyUser() {
    // given
    Group myGroup = identityService.newGroup("myGroup");
    identityService.saveGroup(myGroup);

    User myUser = identityService.newUser("myUser");
    identityService.saveUser(myUser);

    identityService.createMembership(myUser.getId(), myGroup.getId());

    createAuthorization(AUTH_TYPE_GLOBAL, GROUP, myGroup.getId(), "*", ALL);
    createAuthorization(AUTH_TYPE_GLOBAL, GROUP_MEMBERSHIP, myGroup.getId(), "*", ALL);
    createAuthorization(AUTH_TYPE_GLOBAL, USER, myUser.getId(), "*", ALL);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(myUser.getId());

    // when
    identityService.deleteMembership(myUser.getId(), myGroup.getId());

    // then
    processEngineConfiguration.setAuthorizationEnabled(false);
    List<Authorization> list = authorizationService.createAuthorizationQuery().list();
    assertThat(list).extracting("resource", "resourceId", "userId", "permissions")
        .containsExactlyInAnyOrder(tuple(GROUP.resourceType(), myGroup.getId(), "*", ALL.getValue()),
            tuple(GROUP_MEMBERSHIP.resourceType(), myGroup.getId(), "*", ALL.getValue()),
            tuple(USER.resourceType(), myUser.getId(), "*", ALL.getValue()));
  }

  @Test
  void shouldRemoveAuthorizationForUserAndKeepAuthorizationsForAnyUser() {
    // given
    Group myGroup = identityService.newGroup("myGroup");
    identityService.saveGroup(myGroup);

    User myUser = identityService.newUser("myUser");
    identityService.saveUser(myUser);

    identityService.createMembership(myUser.getId(), myGroup.getId());

    createAuthorization(AUTH_TYPE_GLOBAL, GROUP, myGroup.getId(), "*", ALL);
    createAuthorization(AUTH_TYPE_GLOBAL, GROUP_MEMBERSHIP, myGroup.getId(), "*", ALL);
    createAuthorization(AUTH_TYPE_GRANT, GROUP_MEMBERSHIP, myGroup.getId(), myUser.getId(), ALL);
    createAuthorization(AUTH_TYPE_GRANT, GROUP_MEMBERSHIP, myGroup.getId(), "foo", ALL);
    createAuthorization(AUTH_TYPE_GLOBAL, USER, myUser.getId(), "*", ALL);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(myUser.getId());

    // when
    identityService.deleteMembership(myUser.getId(), myGroup.getId());

    // then
    processEngineConfiguration.setAuthorizationEnabled(false);
    List<Authorization> list = authorizationService.createAuthorizationQuery().list();
    assertThat(list).extracting("resource", "resourceId", "userId", "permissions")
        .containsExactlyInAnyOrder(tuple(GROUP.resourceType(), myGroup.getId(), "*", ALL.getValue()),
            tuple(GROUP_MEMBERSHIP.resourceType(), myGroup.getId(), "*", ALL.getValue()),
            tuple(GROUP_MEMBERSHIP.resourceType(), myGroup.getId(), "foo", ALL.getValue()),
            tuple(USER.resourceType(), myUser.getId(), "*", ALL.getValue()));
  }

  @Test
  void testTenantUserMembershipCreateAuthorizations() {

    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);

    Tenant tenant1 = identityService.newTenant("tenant1");
    identityService.saveTenant(tenant1);

    // add base permission which allows nobody to create memberships
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT_MEMBERSHIP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.createTenantUserMembership("tenant1", "jonny1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(CREATE.getName(), TENANT_MEMBERSHIP.resourceName(), "tenant1", info);
      });
  }

  @Test
  void testTenantGroupMembershipCreateAuthorizations() {

    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    Tenant tenant1 = identityService.newTenant("tenant1");
    identityService.saveTenant(tenant1);

    // add base permission which allows nobody to create memberships
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT_MEMBERSHIP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.createTenantGroupMembership("tenant1", "group1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(CREATE.getName(), TENANT_MEMBERSHIP.resourceName(), "tenant1", info);
      });
  }

  @Test
  void testTenantUserMembershipDeleteAuthorizations() {

    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);

    Tenant tenant1 = identityService.newTenant("tenant1");
    identityService.saveTenant(tenant1);

    // add base permission which allows nobody to delete memberships
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT_MEMBERSHIP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'delete'
    basePerms.removePermission(DELETE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.deleteTenantUserMembership("tenant1", "jonny1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(DELETE.getName(), TENANT_MEMBERSHIP.resourceName(), "tenant1", info);
      });
  }

  @Test
  void testTenantGroupMembershipDeleteAuthorizations() {

    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    Tenant tenant1 = identityService.newTenant("tenant1");
    identityService.saveTenant(tenant1);

    // add base permission which allows nobody to delete memberships
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT_MEMBERSHIP);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'delete'
    basePerms.removePermission(DELETE);
    authorizationService.saveAuthorization(basePerms);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(USER_ID);

    assertThatThrownBy(() -> identityService.deleteTenantGroupMembership("tenant1", "group1"))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        AuthorizationException ex = (AuthorizationException) e;
        assertThat(ex.getMissingAuthorizations()).hasSize(1);
        MissingAuthorization info = ex.getMissingAuthorizations().get(0);
        assertThat(ex.getUserId()).isEqualTo(USER_ID);
        assertExceptionInfo(DELETE.getName(), TENANT_MEMBERSHIP.resourceName(), "tenant1", info);
      });
  }

  @Test
  void testUserQueryAuthorizations() {

    // we are jonny2
    String authUserId = "jonny2";
    identityService.setAuthenticatedUserId(authUserId);

    // create new user jonny1
    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);

    // set base permission for all users (no-one has any permissions on users)
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(USER);
    basePerms.setResourceId(ANY);
    authorizationService.saveAuthorization(basePerms);

    // now enable checks
    processEngineConfiguration.setAuthorizationEnabled(true);

    // we cannot fetch the user
    assertThat(identityService.createUserQuery().singleResult()).isNull();
    assertThat(identityService.createUserQuery().count()).isZero();

    processEngineConfiguration.setAuthorizationEnabled(false);

    // now we add permission for jonny2 to read the user:
    Authorization ourPerms = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    ourPerms.setUserId(authUserId);
    ourPerms.setResource(USER);
    ourPerms.setResourceId(ANY);
    ourPerms.addPermission(READ);
    authorizationService.saveAuthorization(ourPerms);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // now we can fetch the user
    assertThat(identityService.createUserQuery().singleResult()).isNotNull();
    assertThat(identityService.createUserQuery().count()).isOne();

    // change the base permission:
    processEngineConfiguration.setAuthorizationEnabled(false);
    basePerms = authorizationService.createAuthorizationQuery().resourceType(USER).userIdIn("*").singleResult();
    basePerms.addPermission(READ);
    authorizationService.saveAuthorization(basePerms);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // we can still fetch the user
    assertThat(identityService.createUserQuery().singleResult()).isNotNull();
    assertThat(identityService.createUserQuery().count()).isOne();


    // revoke permission for jonny2:
    processEngineConfiguration.setAuthorizationEnabled(false);
    ourPerms = authorizationService.createAuthorizationQuery().resourceType(USER).userIdIn(authUserId).singleResult();
    ourPerms.removePermission(READ);
    authorizationService.saveAuthorization(ourPerms);

    Authorization revoke = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    revoke.setUserId(authUserId);
    revoke.setResource(USER);
    revoke.setResourceId(ANY);
    revoke.removePermission(READ);
    authorizationService.saveAuthorization(revoke);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now we cannot fetch the user
    assertThat(identityService.createUserQuery().singleResult()).isNull();
    assertThat(identityService.createUserQuery().count()).isZero();


    // delete our perms
    processEngineConfiguration.setAuthorizationEnabled(false);
    authorizationService.deleteAuthorization(ourPerms.getId());
    authorizationService.deleteAuthorization(revoke.getId());
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now the base permission applies and grants us read access
    assertThat(identityService.createUserQuery().singleResult()).isNotNull();
    assertThat(identityService.createUserQuery().count()).isOne();

  }

  @Test
  void testUserQueryAuthorizationsMultipleGroups() {

    // we are jonny2
    String authUserId = "jonny2";
    identityService.setAuthenticatedUserId(authUserId);

    User demo = identityService.newUser("demo");
    identityService.saveUser(demo);

    User mary = identityService.newUser("mary");
    identityService.saveUser(mary);

    User peter = identityService.newUser("peter");
    identityService.saveUser(peter);

    User john = identityService.newUser("john");
    identityService.saveUser(john);

    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);

    Group accounting = identityService.newGroup("accounting");
    identityService.saveGroup(accounting);

    Group management = identityService.newGroup("management");
    identityService.saveGroup(management);

    identityService.createMembership("demo", "sales");
    identityService.createMembership("demo", "accounting");
    identityService.createMembership("demo", "management");

    identityService.createMembership("john", "sales");
    identityService.createMembership("mary", "accounting");
    identityService.createMembership("peter", "management");

    Authorization demoAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    demoAuth.setUserId("demo");
    demoAuth.setResource(USER);
    demoAuth.setResourceId("demo");
    demoAuth.addPermission(ALL);
    authorizationService.saveAuthorization(demoAuth);

    Authorization johnAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    johnAuth.setUserId("john");
    johnAuth.setResource(USER);
    johnAuth.setResourceId("john");
    johnAuth.addPermission(ALL);
    authorizationService.saveAuthorization(johnAuth);

    Authorization maryAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    maryAuth.setUserId("mary");
    maryAuth.setResource(USER);
    maryAuth.setResourceId("mary");
    maryAuth.addPermission(ALL);
    authorizationService.saveAuthorization(maryAuth);

    Authorization peterAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    peterAuth.setUserId("peter");
    peterAuth.setResource(USER);
    peterAuth.setResourceId("peter");
    peterAuth.addPermission(ALL);
    authorizationService.saveAuthorization(peterAuth);

    Authorization accAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    accAuth.setGroupId("accounting");
    accAuth.setResource(GROUP);
    accAuth.setResourceId("accounting");
    accAuth.addPermission(READ);
    authorizationService.saveAuthorization(accAuth);

    Authorization salesAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    salesAuth.setGroupId("sales");
    salesAuth.setResource(GROUP);
    salesAuth.setResourceId("sales");
    salesAuth.addPermission(READ);
    authorizationService.saveAuthorization(salesAuth);

    Authorization manAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    manAuth.setGroupId("management");
    manAuth.setResource(GROUP);
    manAuth.setResourceId("management");
    manAuth.addPermission(READ);
    authorizationService.saveAuthorization(manAuth);

    Authorization salesDemoAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    salesDemoAuth.setGroupId("sales");
    salesDemoAuth.setResource(USER);
    salesDemoAuth.setResourceId("demo");
    salesDemoAuth.addPermission(READ);
    authorizationService.saveAuthorization(salesDemoAuth);

    Authorization salesJohnAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    salesJohnAuth.setGroupId("sales");
    salesJohnAuth.setResource(USER);
    salesJohnAuth.setResourceId("john");
    salesJohnAuth.addPermission(READ);
    authorizationService.saveAuthorization(salesJohnAuth);

    Authorization manDemoAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    manDemoAuth.setGroupId("management");
    manDemoAuth.setResource(USER);
    manDemoAuth.setResourceId("demo");
    manDemoAuth.addPermission(READ);
    authorizationService.saveAuthorization(manDemoAuth);

    Authorization manPeterAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    manPeterAuth.setGroupId("management");
    manPeterAuth.setResource(USER);
    manPeterAuth.setResourceId("peter");
    manPeterAuth.addPermission(READ);
    authorizationService.saveAuthorization(manPeterAuth);

    Authorization accDemoAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    accDemoAuth.setGroupId("accounting");
    accDemoAuth.setResource(USER);
    accDemoAuth.setResourceId("demo");
    accDemoAuth.addPermission(READ);
    authorizationService.saveAuthorization(accDemoAuth);

    Authorization accMaryAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    accMaryAuth.setGroupId("accounting");
    accMaryAuth.setResource(USER);
    accMaryAuth.setResourceId("mary");
    accMaryAuth.addPermission(READ);
    authorizationService.saveAuthorization(accMaryAuth);

    List<String> groups = new ArrayList<>();
    groups.add("management");
    groups.add("accounting");
    groups.add("sales");

    identityService.setAuthentication("demo", groups);

    processEngineConfiguration.setAuthorizationEnabled(true);

    List<User> salesUser = identityService.createUserQuery().memberOfGroup("sales").list();
    assertThat(salesUser).hasSize(2);

    for (User user : salesUser) {
      if (!"demo".equals(user.getId()) && !"john".equals(user.getId())) {
        fail("Unexpected user for group sales: " + user.getId());
      }
    }

    List<User> accountingUser = identityService.createUserQuery().memberOfGroup("accounting").list();
    assertThat(accountingUser).hasSize(2);

    for (User user : accountingUser) {
      if (!"demo".equals(user.getId()) && !"mary".equals(user.getId())) {
        fail("Unexpected user for group accounting: " + user.getId());
      }
    }

    List<User> managementUser = identityService.createUserQuery().memberOfGroup("management").list();
    assertThat(managementUser).hasSize(2);

    for (User user : managementUser) {
      if (!"demo".equals(user.getId()) && !"peter".equals(user.getId())) {
        fail("Unexpected user for group management: " + user.getId());
      }
    }
  }

  @Test
  void testGroupQueryAuthorizations() {

    // we are jonny2
    String authUserId = "jonny2";
    identityService.setAuthenticatedUserId(authUserId);

    // create new user jonny1
    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);
    // create new group
    Group group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    // set base permission for all users (no-one has any permissions on groups)
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(GROUP);
    basePerms.setResourceId(ANY);
    authorizationService.saveAuthorization(basePerms);

    // now enable checks
    processEngineConfiguration.setAuthorizationEnabled(true);

    // we cannot fetch the group
    assertThat(identityService.createGroupQuery().singleResult()).isNull();
    assertThat(identityService.createGroupQuery().count()).isZero();

    // now we add permission for jonny2 to read the group:
    processEngineConfiguration.setAuthorizationEnabled(false);
    Authorization ourPerms = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    ourPerms.setUserId(authUserId);
    ourPerms.setResource(GROUP);
    ourPerms.setResourceId(ANY);
    ourPerms.addPermission(READ);
    authorizationService.saveAuthorization(ourPerms);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now we can fetch the group
    assertThat(identityService.createGroupQuery().singleResult()).isNotNull();
    assertThat(identityService.createGroupQuery().count()).isOne();

    // change the base permission:
    processEngineConfiguration.setAuthorizationEnabled(false);
    basePerms = authorizationService.createAuthorizationQuery().resourceType(GROUP).userIdIn("*").singleResult();
    basePerms.addPermission(READ);
    authorizationService.saveAuthorization(basePerms);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // we can still fetch the group
    assertThat(identityService.createGroupQuery().singleResult()).isNotNull();
    assertThat(identityService.createGroupQuery().count()).isOne();

    // revoke permission for jonny2:
    processEngineConfiguration.setAuthorizationEnabled(false);
    ourPerms = authorizationService.createAuthorizationQuery().resourceType(GROUP).userIdIn(authUserId).singleResult();
    ourPerms.removePermission(READ);
    authorizationService.saveAuthorization(ourPerms);

    Authorization revoke = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    revoke.setUserId(authUserId);
    revoke.setResource(GROUP);
    revoke.setResourceId(ANY);
    revoke.removePermission(READ);
    authorizationService.saveAuthorization(revoke);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now we cannot fetch the group
    assertThat(identityService.createGroupQuery().singleResult()).isNull();
    assertThat(identityService.createGroupQuery().count()).isZero();

    // delete our perms
    processEngineConfiguration.setAuthorizationEnabled(false);
    authorizationService.deleteAuthorization(ourPerms.getId());
    authorizationService.deleteAuthorization(revoke.getId());
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now the base permission applies and grants us read access
    assertThat(identityService.createGroupQuery().singleResult()).isNotNull();
    assertThat(identityService.createGroupQuery().count()).isOne();

  }

  @Test
  void testTenantQueryAuthorizations() {
    // we are jonny2
    String authUserId = "jonny2";
    identityService.setAuthenticatedUserId(authUserId);

    // create new user jonny1
    User jonny1 = identityService.newUser("jonny1");
    identityService.saveUser(jonny1);
    // create new tenant
    Tenant tenant = identityService.newTenant("tenant");
    identityService.saveTenant(tenant);

    // set base permission for all users (no-one has any permissions on tenants)
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(TENANT);
    basePerms.setResourceId(ANY);
    authorizationService.saveAuthorization(basePerms);

    // now enable checks
    processEngineConfiguration.setAuthorizationEnabled(true);

    // we cannot fetch the tenants
    assertThat(identityService.createTenantQuery().count()).isZero();

    // now we add permission for jonny2 to read the tenants:
    processEngineConfiguration.setAuthorizationEnabled(false);
    Authorization ourPerms = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    ourPerms.setUserId(authUserId);
    ourPerms.setResource(TENANT);
    ourPerms.setResourceId(ANY);
    ourPerms.addPermission(READ);
    authorizationService.saveAuthorization(ourPerms);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now we can fetch the tenants
    assertThat(identityService.createTenantQuery().count()).isOne();

    // change the base permission:
    processEngineConfiguration.setAuthorizationEnabled(false);
    basePerms = authorizationService.createAuthorizationQuery().resourceType(TENANT).userIdIn("*").singleResult();
    basePerms.addPermission(READ);
    authorizationService.saveAuthorization(basePerms);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // we can still fetch the tenants
    assertThat(identityService.createTenantQuery().count()).isOne();

    // revoke permission for jonny2:
    processEngineConfiguration.setAuthorizationEnabled(false);
    ourPerms = authorizationService.createAuthorizationQuery().resourceType(TENANT).userIdIn(authUserId).singleResult();
    ourPerms.removePermission(READ);
    authorizationService.saveAuthorization(ourPerms);

    Authorization revoke = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    revoke.setUserId(authUserId);
    revoke.setResource(TENANT);
    revoke.setResourceId(ANY);
    revoke.removePermission(READ);
    authorizationService.saveAuthorization(revoke);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now we cannot fetch the tenants
    assertThat(identityService.createTenantQuery().count()).isZero();

    // delete our permissions
    processEngineConfiguration.setAuthorizationEnabled(false);
    authorizationService.deleteAuthorization(ourPerms.getId());
    authorizationService.deleteAuthorization(revoke.getId());
    processEngineConfiguration.setAuthorizationEnabled(true);

    // now the base permission applies and grants us read access
    assertThat(identityService.createTenantQuery().count()).isOne();
  }

  @Test
  void shouldDeleteTenantUserMembership() {
    // given
    User userOne = identityService.newUser("userOne");
    identityService.saveUser(userOne);

    User userTwo = identityService.newUser("userTwo");
    identityService.saveUser(userTwo);

    Tenant tenantOne = identityService.newTenant("tenantOne");
    identityService.saveTenant(tenantOne);

    processEngineConfiguration.setAuthorizationEnabled(true);

    identityService.createTenantUserMembership("tenantOne", "userOne");
    identityService.createTenantUserMembership("tenantOne", "userTwo");

    // assume
    List<Authorization> authorizations = authorizationService
        .createAuthorizationQuery()
        .list();

    assertThat(authorizations).extracting("resourceId", "userId")
        .containsExactlyInAnyOrder(
            tuple("tenantOne", "userOne"),
            tuple("tenantOne", "userTwo")
        );

    // when
    identityService.deleteTenantUserMembership("tenantOne", "userOne");

    // then
    authorizations = authorizationService
        .createAuthorizationQuery()
        .list();

    assertThat(authorizations).extracting("resourceId", "userId")
        .containsExactly(
            tuple("tenantOne", "userTwo")
        );
  }

  @Test
  void shouldDeleteTenantGroupMembership() {
    // given
    Group groupOne = identityService.newGroup("groupOne");
    identityService.saveGroup(groupOne);

    Group groupTwo = identityService.newGroup("groupTwo");
    identityService.saveGroup(groupTwo);

    Tenant tenantOne = identityService.newTenant("tenantOne");
    identityService.saveTenant(tenantOne);

    processEngineConfiguration.setAuthorizationEnabled(true);

    identityService.createTenantGroupMembership("tenantOne", "groupOne");
    identityService.createTenantGroupMembership("tenantOne", "groupTwo");

    // assume
    List<Authorization> authorizations = authorizationService
        .createAuthorizationQuery()
        .list();

    assertThat(authorizations).extracting("resourceId", "groupId")
        .containsExactlyInAnyOrder(
            tuple("tenantOne", "groupOne"),
            tuple("tenantOne", "groupTwo")
        );

    // when
    identityService.deleteTenantGroupMembership("tenantOne", "groupOne");

    // then
    authorizations = authorizationService
        .createAuthorizationQuery()
        .list();

    assertThat(authorizations).extracting("resourceId", "groupId")
        .containsExactly(
            tuple("tenantOne", "groupTwo")
        );
  }

  protected void lockUser(String userId, String invalidPassword) {
    Date now = ClockUtil.getCurrentTime();
    try {
      for (int i = 0; i <= 11; i++) {
        assertThat(identityService.checkPassword(userId, invalidPassword)).isFalse();
        now = DateUtils.addMinutes(ClockUtil.getCurrentTime(), 1);
        ClockUtil.setCurrentTime(now);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void grantPermissions() {
    AuthorizationEntity userAdminAuth = new AuthorizationEntity(AUTH_TYPE_GLOBAL);
    userAdminAuth.setResource(USER);
    userAdminAuth.setResourceId(ANY);
    userAdminAuth.addPermission(ALL);
    authorizationService.saveAuthorization(userAdminAuth);

    userAdminAuth = new AuthorizationEntity(AUTH_TYPE_GLOBAL);
    userAdminAuth.setResource(GROUP);
    userAdminAuth.setResourceId(ANY);
    userAdminAuth.addPermission(ALL);
    authorizationService.saveAuthorization(userAdminAuth);

    userAdminAuth = new AuthorizationEntity(AUTH_TYPE_GLOBAL);
    userAdminAuth.setResource(TENANT);
    userAdminAuth.setResourceId(ANY);
    userAdminAuth.addPermission(ALL);
    authorizationService.saveAuthorization(userAdminAuth);

    userAdminAuth = new AuthorizationEntity(AUTH_TYPE_GLOBAL);
    userAdminAuth.setResource(TENANT_MEMBERSHIP);
    userAdminAuth.setResourceId(ANY);
    userAdminAuth.addPermission(ALL);
    authorizationService.saveAuthorization(userAdminAuth);
  }

  protected void cleanupAfterTest() {
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Tenant tenant : identityService.createTenantQuery().list()) {
      identityService.deleteTenant(tenant.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  protected void createAuthorization(int authType,
                                     Resources resource,
                                     String resourceId,
                                     String userId,
                                     Permissions permission) {
    Authorization authorization = authorizationService.createNewAuthorization(authType);
    authorization.setResource(resource);
    authorization.setResourceId(resourceId);
    authorization.addPermission(permission);
    authorization.setUserId(userId);
    authorizationService.saveAuthorization(authorization);
  }

}
