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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

class DefaultPermissionForTenantMemberTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";
  protected static final String USER_ID = "user";
  protected static final String GROUP_ID = "group";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected AuthorizationService authorizationService;
  protected IdentityService identityService;


  @BeforeEach
  void init() {
    identityService = engineRule.getIdentityService();
    authorizationService = engineRule.getAuthorizationService();

    createTenant(TENANT_ONE);

    User user = identityService.newUser(USER_ID);
    identityService.saveUser(user);

    Group group = identityService.newGroup(GROUP_ID);
    identityService.saveGroup(group);

    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);
  }

  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();

    identityService.deleteUser(USER_ID);
    identityService.deleteGroup(GROUP_ID);
    identityService.deleteTenant(TENANT_ONE);
    identityService.deleteTenant(TENANT_TWO);

    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(false);
  }

  @Test
  void testCreateTenantUserMembership() {

    identityService.createTenantUserMembership(TENANT_ONE, USER_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .userIdIn(USER_ID)
        .resourceType(Resources.TENANT)
        .resourceId(TENANT_ONE)
        .hasPermission(Permissions.READ).count()).isEqualTo(1);

    identityService.setAuthenticatedUserId(USER_ID);

    assertThat(identityService.createTenantQuery()
        .singleResult()
        .getId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testCreateAndDeleteTenantUserMembership() {

    identityService.createTenantUserMembership(TENANT_ONE, USER_ID);

    identityService.deleteTenantUserMembership(TENANT_ONE, USER_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .userIdIn(USER_ID)
        .resourceType(Resources.TENANT)
        .hasPermission(Permissions.READ).count()).isZero();

    identityService.setAuthenticatedUserId(USER_ID);

    assertThat(identityService.createTenantQuery()
        .count()).isZero();
  }

  @Test
  void testCreateAndDeleteTenantUserMembershipForMultipleTenants() {

    createTenant(TENANT_TWO);

    identityService.createTenantUserMembership(TENANT_ONE, USER_ID);
    identityService.createTenantUserMembership(TENANT_TWO, USER_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .userIdIn(USER_ID)
        .resourceType(Resources.TENANT)
        .hasPermission(Permissions.READ).count()).isEqualTo(2);

    identityService.deleteTenantUserMembership(TENANT_ONE, USER_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .userIdIn(USER_ID)
        .resourceType(Resources.TENANT)
        .hasPermission(Permissions.READ).count()).isEqualTo(1);
  }

  @Test
  void testCreateTenantGroupMembership() {

    identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .groupIdIn(GROUP_ID)
        .resourceType(Resources.TENANT)
        .resourceId(TENANT_ONE)
        .hasPermission(Permissions.READ).count()).isEqualTo(1);

    identityService.setAuthentication(USER_ID, Collections.singletonList(GROUP_ID));

    assertThat(identityService.createTenantQuery()
        .singleResult()
        .getId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testCreateAndDeleteTenantGroupMembership() {

    identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ID);

    identityService.deleteTenantGroupMembership(TENANT_ONE, GROUP_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .groupIdIn(GROUP_ID)
        .resourceType(Resources.TENANT)
        .hasPermission(Permissions.READ).count()).isZero();

    identityService.setAuthentication(USER_ID, Collections.singletonList(GROUP_ID));

    assertThat(identityService.createTenantQuery()
        .count()).isZero();
  }

  @Test
  void testCreateAndDeleteTenantGroupMembershipForMultipleTenants() {

    createTenant(TENANT_TWO);

    identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ID);
    identityService.createTenantGroupMembership(TENANT_TWO, GROUP_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .groupIdIn(GROUP_ID)
        .resourceType(Resources.TENANT)
        .hasPermission(Permissions.READ).count()).isEqualTo(2);

    identityService.deleteTenantGroupMembership(TENANT_ONE, GROUP_ID);

    assertThat(authorizationService.createAuthorizationQuery()
        .groupIdIn(GROUP_ID)
        .resourceType(Resources.TENANT)
        .hasPermission(Permissions.READ).count()).isEqualTo(1);
  }

  protected Tenant createTenant(String tenantId) {
    Tenant newTenant = identityService.newTenant(tenantId);
    identityService.saveTenant(newTenant);
    return newTenant;
  }
}
