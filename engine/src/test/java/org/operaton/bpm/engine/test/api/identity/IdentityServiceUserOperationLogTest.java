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

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Tobias Metzke
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineTestExtension.class)
class IdentityServiceUserOperationLogTest {

  protected static final String TEST_USER_ID = "newTestUser";
  protected static final String TEST_GROUP_ID = "newTestGroup";
  protected static final String TEST_TENANT_ID = "newTestTenant";

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected HistoryService historyService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected UserOperationLogQuery query;

  @BeforeEach
  void setUp() {
    query = historyService.createUserOperationLogQuery();
  }

  @AfterEach
  void cleanUp() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Tenant tenant : identityService.createTenantQuery().list()) {
      identityService.deleteTenant(tenant.getId());
    }
    ClockUtil.reset();
  }

  @Test
  void shouldLogUserCreation() {
    // given
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_CREATE, EntityTypes.USER, null, TEST_USER_ID);
  }

  @Test
  void shouldNotLogUserCreationFailure() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");
    User newUser = identityService.newUser(TEST_USER_ID);

    // when/then
    assertThatThrownBy(() -> identityService.saveUser(newUser))
      .isInstanceOf(ProcessEngineException.class);

    // then
    assertThat(query.count()).isZero();

    identityService.clearAuthentication();

  }

  @Test
  void shouldLogUserUpdate() {
    // given
    User newUser = identityService.newUser(TEST_USER_ID);
    identityService.saveUser(newUser);
    assertThat(query.count()).isZero();

    // when
    newUser.setEmail("test@mail.com");
    identityService.setAuthenticatedUserId("userId");
    identityService.saveUser(newUser);
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_UPDATE, EntityTypes.USER, null, TEST_USER_ID);
  }

  @Test
  void shouldLogUserDeletion() {
    // given
    User newUser = identityService.newUser(TEST_USER_ID);
    identityService.saveUser(newUser);
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.deleteUser(newUser.getId());
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_DELETE, EntityTypes.USER, null, TEST_USER_ID);
  }

  @Test
  void shouldNotLogUserDeletionOnNonExisting() {
    // given
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.deleteUser(TEST_USER_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogUserUnlock() {
    // given
    User newUser = identityService.newUser(TEST_USER_ID);
    newUser.setPassword("right");
    identityService.saveUser(newUser);
    identityService.checkPassword(TEST_USER_ID, "wrong!");
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.unlockUser(TEST_USER_ID);
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_UNLOCK, EntityTypes.USER, null, TEST_USER_ID);
  }

  @Test
  void shouldNotLogUserUnlockOnNonExistingUser() {
    // given
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.unlockUser(TEST_USER_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldNotLogUserUnlockOnNonExistingLock() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.unlockUser(TEST_USER_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogGroupCreation() {
    // given
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_CREATE, EntityTypes.GROUP, null, TEST_GROUP_ID);
  }

  @Test
  void shouldNotLogGroupCreationFailure() {
    // given
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");
    Group newGroup = identityService.newGroup(TEST_GROUP_ID);

    // when/then
    assertThatThrownBy(() -> identityService.saveGroup(newGroup))
      .isInstanceOf(ProcessEngineException.class);

    // and
    assertThat(query.count()).isZero();

    identityService.clearAuthentication();
  }

  @Test
  void shouldLogGroupUpdate() {
    // given
    Group newGroup = identityService.newGroup(TEST_GROUP_ID);
    identityService.saveGroup(newGroup);
    assertThat(query.count()).isZero();

    // when
    newGroup.setName("testName");
    identityService.setAuthenticatedUserId("userId");
    identityService.saveGroup(newGroup);
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_UPDATE, EntityTypes.GROUP, null, TEST_GROUP_ID);
  }

  @Test
  void shouldLogGroupDeletion() {
    // given
    Group newGroup = identityService.newGroup(TEST_GROUP_ID);
    identityService.saveGroup(newGroup);
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.deleteGroup(newGroup.getId());
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_DELETE, EntityTypes.GROUP, null, TEST_GROUP_ID);
  }

  @Test
  void shouldNotLogGroupDeletionOnNonExisting() {
    // given
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.deleteGroup(TEST_GROUP_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogTenantCreation() {
    // given
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_CREATE, EntityTypes.TENANT, null, TEST_TENANT_ID);
  }

  @Test
  void shouldNotLogTenantCreationFailure() {
    // given
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");
    Tenant newTenant = identityService.newTenant(TEST_TENANT_ID);

    // when/then
    assertThatThrownBy(() -> identityService.saveTenant(newTenant))
      .isInstanceOf(ProcessEngineException.class);

    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogTenantUpdate() {
    // given
    Tenant newTenant = identityService.newTenant(TEST_TENANT_ID);
    identityService.saveTenant(newTenant);
    assertThat(query.count()).isZero();

    // when
    newTenant.setName("testName");
    identityService.setAuthenticatedUserId("userId");
    identityService.saveTenant(newTenant);
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_UPDATE, EntityTypes.TENANT, null, TEST_TENANT_ID);
  }

  @Test
  void shouldLogTenantDeletion() {
    // given
    Tenant newTenant = identityService.newTenant(TEST_TENANT_ID);
    identityService.saveTenant(newTenant);
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.deleteTenant(newTenant.getId());
    identityService.clearAuthentication();

    // then
    assertLog(UserOperationLogEntry.OPERATION_TYPE_DELETE, EntityTypes.TENANT, null, TEST_TENANT_ID);
  }

  @Test
  void shouldNotLogTenantDeletionOnNonExisting() {
    // given
    assertThat(query.count()).isZero();

    // when
    identityService.setAuthenticatedUserId("userId");
    identityService.deleteTenant(TEST_TENANT_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogGroupMembershipCreation() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.createMembership(TEST_USER_ID, TEST_GROUP_ID);
    identityService.clearAuthentication();

    // then
    assertLogs(UserOperationLogEntry.OPERATION_TYPE_CREATE, EntityTypes.GROUP_MEMBERSHIP,
        Triple.of("userId", null, TEST_USER_ID),
        Triple.of("groupId", null, TEST_GROUP_ID));
  }

  @Test
  void shouldNotLogGroupMembershipCreationFailure() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    identityService.createMembership(TEST_USER_ID, TEST_GROUP_ID);
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when/then
    assertThatThrownBy(() -> identityService.createMembership(TEST_USER_ID, TEST_GROUP_ID))
      .isInstanceOf(ProcessEngineException.class);

    identityService.clearAuthentication();

    // and
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogGroupMembershipDeletion() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    identityService.createMembership(TEST_USER_ID, TEST_GROUP_ID);
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.deleteMembership(TEST_USER_ID, TEST_GROUP_ID);
    identityService.clearAuthentication();

    // then
    assertLogs(UserOperationLogEntry.OPERATION_TYPE_DELETE, EntityTypes.GROUP_MEMBERSHIP,
        Triple.of("userId", null, TEST_USER_ID),
        Triple.of("groupId", null, TEST_GROUP_ID));
  }

  @Test
  void shouldNotLogGroupMembershipDeletionOnNonExisting() {
    // given
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.deleteMembership(TEST_USER_ID, TEST_GROUP_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogTenantUserMembershipCreation() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.createTenantUserMembership(TEST_TENANT_ID, TEST_USER_ID);
    identityService.clearAuthentication();

    // then
    assertLogs(UserOperationLogEntry.OPERATION_TYPE_CREATE, EntityTypes.TENANT_MEMBERSHIP,
        Triple.of("userId", null, TEST_USER_ID),
        Triple.of("tenantId", null, TEST_TENANT_ID));
  }

  @Test
  void shouldNotLogTenantUserMembershipCreationFailure() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    identityService.createTenantUserMembership(TEST_TENANT_ID, TEST_USER_ID);
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when/then
    assertThatThrownBy(() -> identityService.createTenantUserMembership(TEST_TENANT_ID, TEST_USER_ID))
      .isInstanceOf(ProcessEngineException.class);

    identityService.clearAuthentication();

    // and
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogTenantUserMembershipDeletion() {
    // given
    identityService.saveUser(identityService.newUser(TEST_USER_ID));
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    identityService.createTenantUserMembership(TEST_TENANT_ID, TEST_USER_ID);
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.deleteTenantUserMembership(TEST_TENANT_ID, TEST_USER_ID);
    identityService.clearAuthentication();

    // then
    assertLogs(UserOperationLogEntry.OPERATION_TYPE_DELETE, EntityTypes.TENANT_MEMBERSHIP,
        Triple.of("userId", null, TEST_USER_ID),
        Triple.of("tenantId", null, TEST_TENANT_ID));
  }

  @Test
  void shouldNotLogTenantUserMembershipDeletionOnNonExisting() {
    // given
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.deleteTenantUserMembership(TEST_TENANT_ID, TEST_USER_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogTenantGroupMembershipCreation() {
    // given
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.createTenantGroupMembership(TEST_TENANT_ID, TEST_GROUP_ID);
    identityService.clearAuthentication();

    // then
    assertLogs(UserOperationLogEntry.OPERATION_TYPE_CREATE, EntityTypes.TENANT_MEMBERSHIP,
        Triple.of("groupId", null, TEST_GROUP_ID),
        Triple.of("tenantId", null, TEST_TENANT_ID));
  }

  @Test
  void shouldNotLogTenantGroupMembershipCreationFailure() {
    // given
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    identityService.createTenantGroupMembership(TEST_TENANT_ID, TEST_GROUP_ID);
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when/then
    assertThatThrownBy(() -> identityService.createTenantGroupMembership(TEST_TENANT_ID, TEST_GROUP_ID))
      .isInstanceOf(ProcessEngineException.class);

    identityService.clearAuthentication();

    // and
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldLogTenantGroupMembershipDeletion() {
    // given
    identityService.saveGroup(identityService.newGroup(TEST_GROUP_ID));
    identityService.saveTenant(identityService.newTenant(TEST_TENANT_ID));
    identityService.createTenantGroupMembership(TEST_TENANT_ID, TEST_GROUP_ID);
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.deleteTenantGroupMembership(TEST_TENANT_ID, TEST_GROUP_ID);
    identityService.clearAuthentication();

    // then
    assertLogs(UserOperationLogEntry.OPERATION_TYPE_DELETE, EntityTypes.TENANT_MEMBERSHIP,
        Triple.of("groupId", null, TEST_GROUP_ID),
        Triple.of("tenantId", null, TEST_TENANT_ID));
  }

  @Test
  void shouldNotLogTenantGroupMembershipDeletionOnNonExisting() {
    // given
    assertThat(query.count()).isZero();
    identityService.setAuthenticatedUserId("userId");

    // when
    identityService.deleteTenantGroupMembership(TEST_TENANT_ID, TEST_GROUP_ID);
    identityService.clearAuthentication();

    // then
    assertThat(query.count()).isZero();
  }

  protected void assertLog(String operation, String entity, String orgValue, String newValue) {
    assertThat(query.count()).isEqualTo(1);
    UserOperationLogEntry entry = query.singleResult();
    assertThat(entry.getOperationType()).isEqualTo(operation);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
    assertThat(entry.getEntityType()).isEqualTo(entity);
    assertThat(entry.getOrgValue()).isEqualTo(orgValue);
    assertThat(entry.getNewValue()).isEqualTo(newValue);
  }

  @SafeVarargs
  protected final void assertLogs(String operation, String entity, Triple<String, String, String>... values) {
    assertThat(query.count()).isEqualTo(values.length);
    for (Triple<String, String, String> valueTriple : values) {
      UserOperationLogEntry entry = query.property(valueTriple.getLeft()).singleResult();
      assertThat(entry.getOperationType()).isEqualTo(operation);
      assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
      assertThat(entry.getEntityType()).isEqualTo(entity);
      assertThat(entry.getOrgValue()).isEqualTo(valueTriple.getMiddle());
      assertThat(entry.getNewValue()).isEqualTo(valueTriple.getRight());
    }
  }
}
