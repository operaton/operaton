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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.TenantQuery;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.operaton.bpm.engine.test.util.ProcessEngineUtils.newRandomProcessEngineName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(ProcessEngineExtension.class)
class IdentityServiceTenantTest {

  protected static final String USER_ONE = "user1";
  protected static final String USER_TWO = "user2";

  protected static final String GROUP_ONE = "group1";
  protected static final String GROUP_TWO = "group2";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  private static final String INVALID_ID_MESSAGE = "%s has an invalid id: '%s' is not a valid resource identifier.";
  private static final String PROCESS_ENGINE_NAME = newRandomProcessEngineName();

  protected IdentityService identityService;
  protected ProcessEngine processEngine;

  @AfterEach
  void cleanUp() {
    identityService.deleteTenant(TENANT_ONE);
    identityService.deleteTenant(TENANT_TWO);

    identityService.deleteGroup(GROUP_ONE);
    identityService.deleteGroup(GROUP_TWO);

    identityService.deleteUser(USER_ONE);
    identityService.deleteUser(USER_TWO);

    if (processEngine != null && ProcessEngines.getDefaultProcessEngine() != processEngine) {
      for (Tenant deleteTenant : processEngine.getIdentityService().createTenantQuery().list()) {
        processEngine.getIdentityService().deleteTenant(deleteTenant.getId());
      }
      for (Authorization authorization : processEngine.getAuthorizationService().createAuthorizationQuery().list()) {
        processEngine.getAuthorizationService().deleteAuthorization(authorization.getId());
      }

      processEngine.close();
      ProcessEngines.unregister(processEngine);
      processEngine = null;
    }
  }

  @Test
  void createTenant() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    tenant.setName("Tenant");
    identityService.saveTenant(tenant);

    tenant = identityService.createTenantQuery().singleResult();
    assertThat(tenant).isNotNull();
    assertThat(tenant.getId()).isEqualTo(TENANT_ONE);
    assertThat(tenant.getName()).isEqualTo("Tenant");
  }

  @Test
  void createExistingTenant() {
    // given
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    tenant.setName("Tenant");
    identityService.saveTenant(tenant);

    Tenant secondTenant = identityService.newTenant(TENANT_ONE);
    secondTenant.setName("Tenant");

    // when/then
    assertThatThrownBy(() -> identityService.saveTenant(secondTenant))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("The tenant already exists");
  }

  @Test
  void updateTenant() {
    // create
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    tenant.setName("Tenant");
    identityService.saveTenant(tenant);

    // update
    tenant = identityService.createTenantQuery().singleResult();
    assertThat(tenant).isNotNull();

    tenant.setName("newName");
    identityService.saveTenant(tenant);

    tenant = identityService.createTenantQuery().singleResult();
    assertThat(tenant.getName()).isEqualTo("newName");
  }

  @Test
  void testInvalidTenantId() {
    // given
    String invalidId = "john's tenant";
    Tenant tenant = identityService.newTenant(invalidId);

    // when/then
    assertThatThrownBy(() -> identityService.saveTenant(tenant))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage(INVALID_ID_MESSAGE.formatted("Tenant", invalidId));
  }

  @Test
  void testInvalidTenantIdOnUpdate() {
    // given
    String invalidId = "john's tenant";
    Tenant updatedTenant = identityService.newTenant("john");
    updatedTenant.setId(invalidId);

    // when/then
    assertThatThrownBy(() -> identityService.saveTenant(updatedTenant))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage(INVALID_ID_MESSAGE.formatted("Tenant", invalidId));
  }

  @Test
  void testCustomCreateTenantWhitelistPattern() {
    // given
    processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/identity/generic.resource.id.whitelist.operaton.cfg.xml")
      .setProcessEngineName(PROCESS_ENGINE_NAME)
      .buildProcessEngine();
    processEngine.getProcessEngineConfiguration().setTenantResourceWhitelistPattern("[a-zA-Z]+");

    String invalidId = "john's tenant";
    Tenant tenant = processEngine.getIdentityService().newTenant(invalidId);

    // when/then
    assertThatThrownBy(() -> identityService.saveTenant(tenant))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage(INVALID_ID_MESSAGE.formatted("Tenant", invalidId));
  }

  @Test
  void testCustomTenantWhitelistPattern() {
    // given
    processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/identity/generic.resource.id.whitelist.operaton.cfg.xml")
      .setProcessEngineName(PROCESS_ENGINE_NAME)
      .buildProcessEngine();
    processEngine.getProcessEngineConfiguration().setTenantResourceWhitelistPattern("[a-zA-Z]+");

    String validId = "johnsTenant";
    String invalidId = "john!@#$%";
    Tenant tenant = processEngine.getIdentityService().newTenant(validId);
    tenant.setId(invalidId);

    // when/then
    assertThatThrownBy(() -> identityService.saveTenant(tenant))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage(INVALID_ID_MESSAGE.formatted("Tenant", invalidId));
  }

  @Test
  void deleteTenant() {
    // create
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    TenantQuery query = identityService.createTenantQuery();
    assertThat(query.count()).isOne();

    identityService.deleteTenant("nonExisting");
    assertThat(query.count()).isOne();

    identityService.deleteTenant(TENANT_ONE);
    assertThat(query.count()).isZero();
  }

  @Test
  void updateTenantOptimisticLockingException() {
    // create
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    Tenant tenant1 = identityService.createTenantQuery().singleResult();
    Tenant tenant2 = identityService.createTenantQuery().singleResult();

    // update
    tenant1.setName("name");
    identityService.saveTenant(tenant1);

    // fail to update old revision
    tenant2.setName("other name");

    assertThatThrownBy(() -> identityService.saveTenant(tenant2))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void createTenantWithGenericResourceId() {
    processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/identity/generic.resource.id.whitelist.operaton.cfg.xml")
      .setProcessEngineName(PROCESS_ENGINE_NAME)
      .buildProcessEngine();

    Tenant tenant = processEngine.getIdentityService().newTenant("*");

    assertThatThrownBy(() -> identityService.saveTenant(tenant))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Tenant has an invalid id: '*' is not a valid resource identifier.");
  }

  @Test
  void createTenantMembershipUnexistingTenant() {
    User user = identityService.newUser(USER_ONE);
    identityService.saveUser(user);
    String userId = user.getId();

    assertThatThrownBy(() -> identityService.createTenantUserMembership("nonExisting", userId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No tenant found with id 'nonExisting'.");
  }

  @Test
  void createTenantMembershipUnexistingUser() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);
    String tenantId = tenant.getId();

    assertThatThrownBy(() -> identityService.createTenantUserMembership(tenantId, "nonExisting"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No user found with id 'nonExisting'.");
  }

  @Test
  void createTenantMembershipUnexistingGroup() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);
    String tenantId = tenant.getId();

    assertThatThrownBy(() -> identityService.createTenantGroupMembership(tenantId, "nonExisting"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No group found with id 'nonExisting'.");

  }

  @Test
  void createTenantUserMembershipAlreadyExisting() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    User user = identityService.newUser(USER_ONE);
    identityService.saveUser(user);

    identityService.createTenantUserMembership(TENANT_ONE, USER_ONE);

    assertThatThrownBy(() -> identityService.createTenantUserMembership(TENANT_ONE, USER_ONE))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void createTenantGroupMembershipAlreadyExisting() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    Group group = identityService.newGroup(GROUP_ONE);
    identityService.saveGroup(group);

    identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ONE);

    assertThatThrownBy(() -> identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ONE))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void deleteTenantUserMembership() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    User user = identityService.newUser(USER_ONE);
    identityService.saveUser(user);

    identityService.createTenantUserMembership(TENANT_ONE, USER_ONE);

    TenantQuery query = identityService.createTenantQuery().userMember(USER_ONE);
    assertThat(query.count()).isOne();

    identityService.deleteTenantUserMembership("nonExisting", USER_ONE);
    assertThat(query.count()).isOne();

    identityService.deleteTenantUserMembership(TENANT_ONE, "nonExisting");
    assertThat(query.count()).isOne();

    identityService.deleteTenantUserMembership(TENANT_ONE, USER_ONE);
    assertThat(query.count()).isZero();
  }

  @Test
  void deleteTenantGroupMembership() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    Group group = identityService.newGroup(GROUP_ONE);
    identityService.saveGroup(group);

    identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ONE);

    TenantQuery query = identityService.createTenantQuery().groupMember(GROUP_ONE);
    assertThat(query.count()).isOne();

    identityService.deleteTenantGroupMembership("nonExisting", GROUP_ONE);
    assertThat(query.count()).isOne();

    identityService.deleteTenantGroupMembership(TENANT_ONE, "nonExisting");
    assertThat(query.count()).isOne();

    identityService.deleteTenantGroupMembership(TENANT_ONE, GROUP_ONE);
    assertThat(query.count()).isZero();
  }

  @Test
  void deleteTenantMembershipsWileDeleteUser() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    User user = identityService.newUser(USER_ONE);
    identityService.saveUser(user);

    identityService.createTenantUserMembership(TENANT_ONE, USER_ONE);

    TenantQuery query = identityService.createTenantQuery().userMember(USER_ONE);
    assertThat(query.count()).isOne();

    identityService.deleteUser(USER_ONE);
    assertThat(query.count()).isZero();
  }

  @Test
  void deleteTenantMembershipsWhileDeleteGroup() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    Group group = identityService.newGroup(GROUP_ONE);
    identityService.saveGroup(group);

    identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ONE);

    TenantQuery query = identityService.createTenantQuery().groupMember(GROUP_ONE);
    assertThat(query.count()).isOne();

    identityService.deleteGroup(GROUP_ONE);
    assertThat(query.count()).isZero();
  }

  @Test
  void deleteTenantMembershipsOfTenant() {
    Tenant tenant = identityService.newTenant(TENANT_ONE);
    identityService.saveTenant(tenant);

    User user = identityService.newUser(USER_ONE);
    identityService.saveUser(user);

    Group group = identityService.newGroup(GROUP_ONE);
    identityService.saveGroup(group);

    identityService.createTenantUserMembership(TENANT_ONE, USER_ONE);
    identityService.createTenantGroupMembership(TENANT_ONE, GROUP_ONE);

    UserQuery userQuery = identityService.createUserQuery().memberOfTenant(TENANT_ONE);
    GroupQuery groupQuery = identityService.createGroupQuery().memberOfTenant(TENANT_ONE);
    assertThat(userQuery.count()).isOne();
    assertThat(groupQuery.count()).isOne();

    identityService.deleteTenant(TENANT_ONE);
    assertThat(userQuery.count()).isZero();
    assertThat(groupQuery.count()).isZero();
  }

}
