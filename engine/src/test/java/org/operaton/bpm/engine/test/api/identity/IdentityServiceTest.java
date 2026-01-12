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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Picture;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.identity.Account;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.impl.persistence.entity.UserEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.WatchLogger;

import static org.operaton.bpm.engine.test.util.ProcessEngineUtils.newRandomProcessEngineName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Frederik Heremans
 */
class IdentityServiceTest {

  private static final String INVALID_ID_MESSAGE = "%s has an invalid id: '%s' is not a valid resource identifier.";

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final String IDENTITY_LOGGER = "org.operaton.bpm.engine.identity";

  private static final String PROCESS_ENGINE_NAME = newRandomProcessEngineName();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension();

  protected IdentityService identityService;
  protected ProcessEngine processEngine;

  private static Date parseDate(String dateString) {
    LocalDateTime parsedDateTime = LocalDateTime.parse(dateString, dateFormatter);
    return Date.from(parsedDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  @AfterEach
  void cleanUp() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    ClockUtil.setCurrentTime(new Date());

    if (processEngine != null) {

      for (User user : processEngine.getIdentityService().createUserQuery().list()) {
        processEngine.getIdentityService().deleteUser(user.getId());
      }
      for (Group group : processEngine.getIdentityService().createGroupQuery().list()) {
        processEngine.getIdentityService().deleteGroup(group.getId());
      }
      for (Tenant tenant : processEngine.getIdentityService().createTenantQuery().list()) {
        processEngine.getIdentityService().deleteTenant(tenant.getId());
      }
      for (Authorization authorization : processEngine.getAuthorizationService().createAuthorizationQuery().list()) {
        processEngine.getAuthorizationService().deleteAuthorization(authorization.getId());
      }
      if (processEngine != ProcessEngines.getDefaultProcessEngine()) {
        processEngine.close();
      }
    }
  }

  @Test
  void testIsReadOnly() {
    assertThat(identityService.isReadOnly()).isFalse();
  }

  @Test
  void testUserInfo() {
    User user = identityService.newUser("testuser");
    identityService.saveUser(user);

    identityService.setUserInfo("testuser", "myinfo", "myvalue");
    assertThat(identityService.getUserInfo("testuser", "myinfo")).isEqualTo("myvalue");

    identityService.setUserInfo("testuser", "myinfo", "myvalue2");
    assertThat(identityService.getUserInfo("testuser", "myinfo")).isEqualTo("myvalue2");

    identityService.deleteUserInfo("testuser", "myinfo");
    assertThat(identityService.getUserInfo("testuser", "myinfo")).isNull();

    identityService.deleteUser(user.getId());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testUserAccount() {
    User user = identityService.newUser("testuser");
    identityService.saveUser(user);

    identityService.setUserAccount("testuser", "123", "google", "mygoogleusername", "mygooglepwd", null);
    Account googleAccount = identityService.getUserAccount("testuser", "123", "google");
    assertThat(googleAccount.getName()).isEqualTo("google");
    assertThat(googleAccount.getUsername()).isEqualTo("mygoogleusername");
    assertThat(googleAccount.getPassword()).isEqualTo("mygooglepwd");

    identityService.setUserAccount("testuser", "123", "google", "mygoogleusername2", "mygooglepwd2", null);
    googleAccount = identityService.getUserAccount("testuser", "123", "google");
    assertThat(googleAccount.getName()).isEqualTo("google");
    assertThat(googleAccount.getUsername()).isEqualTo("mygoogleusername2");
    assertThat(googleAccount.getPassword()).isEqualTo("mygooglepwd2");

    identityService.setUserAccount("testuser", "123", "alfresco", "myalfrescousername", "myalfrescopwd", null);
    identityService.setUserInfo("testuser", "myinfo", "myvalue");
    identityService.setUserInfo("testuser", "myinfo2", "myvalue2");

    List<String> expectedUserAccountNames = new ArrayList<>();
    expectedUserAccountNames.add("google");
    expectedUserAccountNames.add("alfresco");
    List<String> userAccountNames = identityService.getUserAccountNames("testuser");
    assertListElementsMatch(expectedUserAccountNames, userAccountNames);

    identityService.deleteUserAccount("testuser", "google");

    expectedUserAccountNames.remove("google");

    userAccountNames = identityService.getUserAccountNames("testuser");
    assertListElementsMatch(expectedUserAccountNames, userAccountNames);

    identityService.deleteUser(user.getId());
  }

  private void assertListElementsMatch(List<String> list1, List<String> list2) {
    if (list1 != null) {
      assertThat(list2)
              .isNotNull()
              .hasSize(list1.size());
      for (String value : list1) {
        assertThat(list2).contains(value);
      }
    } else {
      assertThat(list2).isNull();
    }

  }

  @Test
  @SuppressWarnings("deprecation")
  void testUserAccountDetails() {
    User user = identityService.newUser("testuser");
    identityService.saveUser(user);

    Map<String, String> accountDetails = new HashMap<>();
    accountDetails.put("server", "localhost");
    accountDetails.put("port", "35");
    identityService.setUserAccount("testuser", "123", "google", "mygoogleusername", "mygooglepwd", accountDetails);
    Account googleAccount = identityService.getUserAccount("testuser", "123", "google");
    assertThat(googleAccount.getDetails()).isEqualTo(accountDetails);

    identityService.deleteUser(user.getId());
  }

  @Test
  void testCreateExistingUser() {
    User user = identityService.newUser("testuser");
    identityService.saveUser(user);

    User secondUser = identityService.newUser("testuser");

    try {
      identityService.saveUser(secondUser);
      fail("BadUserRequestException is expected");
    } catch (Exception ex) {
      if (!(ex instanceof BadUserRequestException)) {
        fail("BadUserRequestException is expected, but another exception was received:  " + ex);
      }
      assertThat(ex.getMessage()).isEqualTo("The user already exists");
    }
  }

  @Test
  void testUpdateUser() {
    // First, create a new user
    User user = identityService.newUser("johndoe");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("johndoe@alfresco.com");
    user.setPassword("s3cret");
    identityService.saveUser(user);

    // Fetch and update the user
    user = identityService.createUserQuery().userId("johndoe").singleResult();
    user.setEmail("updated@alfresco.com");
    user.setFirstName("Jane");
    user.setLastName("Donnel");
    identityService.saveUser(user);

    user = identityService.createUserQuery().userId("johndoe").singleResult();
    assertThat(user.getFirstName()).isEqualTo("Jane");
    assertThat(user.getLastName()).isEqualTo("Donnel");
    assertThat(user.getEmail()).isEqualTo("updated@alfresco.com");
    assertThat(identityService.checkPassword("johndoe", "s3cret")).isTrue();

    identityService.deleteUser(user.getId());
  }

  @Test
  void testUserPicture() {
    // First, create a new user
    User user = identityService.newUser("johndoe");
    identityService.saveUser(user);
    String userId = user.getId();

    Picture picture = new Picture("niceface".getBytes(), "image/string");
    identityService.setUserPicture(userId, picture);

    picture = identityService.getUserPicture(userId);

    // Fetch and update the user
    user = identityService.createUserQuery().userId("johndoe").singleResult();
    assertThat(Arrays.equals("niceface".getBytes(), picture.getBytes())).as("byte arrays differ").isTrue();
    assertThat(picture.getMimeType()).isEqualTo("image/string");

    identityService.deleteUserPicture("johndoe");
    // this is ignored
    identityService.deleteUserPicture("someone-else-we-dont-know");

    // picture does not exist
    picture = identityService.getUserPicture("johndoe");
    assertThat(picture).isNull();

    // add new picture
    picture = new Picture("niceface".getBytes(), "image/string");
    identityService.setUserPicture(userId, picture);

    // makes the picture go away
    identityService.deleteUser(user.getId());
  }

  @Test
  void testCreateExistingGroup() {
    Group group = identityService.newGroup("greatGroup");
    identityService.saveGroup(group);

    Group secondGroup = identityService.newGroup("greatGroup");

    try {
      identityService.saveGroup(secondGroup);
      fail("BadUserRequestException is expected");
    } catch (Exception ex) {
      if (!(ex instanceof BadUserRequestException)) {
        fail("BadUserRequestException is expected, but another exception was received:  " + ex);
      }
      assertThat(ex.getMessage()).isEqualTo("The group already exists");
    }
  }

  @Test
  void testUpdateGroup() {
    Group group = identityService.newGroup("sales");
    group.setName("Sales");
    identityService.saveGroup(group);

    group = identityService.createGroupQuery().groupId("sales").singleResult();
    group.setName("Updated");
    identityService.saveGroup(group);

    group = identityService.createGroupQuery().groupId("sales").singleResult();
    assertThat(group.getName()).isEqualTo("Updated");

    identityService.deleteGroup(group.getId());
  }

  @Test
  void findUserByUnexistingId() {
    User user = identityService.createUserQuery().userId("unexistinguser").singleResult();
    assertThat(user).isNull();
  }

  @Test
  void findGroupByUnexistingId() {
    Group group = identityService.createGroupQuery().groupId("unexistinggroup").singleResult();
    assertThat(group).isNull();
  }

  @Test
  void testCreateMembershipUnexistingGroup() {
    User johndoe = identityService.newUser("johndoe");
    identityService.saveUser(johndoe);
    String userId = johndoe.getId();

    // when/then
    assertThatThrownBy(() -> identityService.createMembership(userId, "unexistinggroup"))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("No group found with id 'unexistinggroup'.: group is null");
  }

  @Test
  void testCreateMembershipUnexistingUser() {
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);
    String groupId = sales.getId();

    // when/then
    assertThatThrownBy(() -> identityService.createMembership("unexistinguser", groupId))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("No user found with id 'unexistinguser'.: user is null");
  }

  @Test
  void testCreateMembershipAlreadyExisting() {
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);
    User johndoe = identityService.newUser("johndoe");
    identityService.saveUser(johndoe);
    String userId = johndoe.getId();
    String groupId = sales.getId();

    // Create the membership
    identityService.createMembership(userId, groupId);

    // when/then
    assertThatThrownBy(() -> identityService.createMembership(userId, groupId))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSaveGroupNullArgument() {
    // when/then
    assertThatThrownBy(() -> identityService.saveGroup(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("group is null");
  }

  @Test
  void testSaveUserNullArgument() {
    // when/then
    assertThatThrownBy(() -> identityService.saveUser(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("user is null");
  }

  @Test
  void testFindGroupByIdNullArgument() {
    var groupQuery = identityService.createGroupQuery();
    // when/then
    assertThatThrownBy(() -> groupQuery.groupId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("id is null");
  }

  @Test
  void testCreateMembershipNullUserArgument() {
    // when/then
    assertThatThrownBy(() -> identityService.createMembership(null, "group"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("userId is null");
  }

  @Test
  void testCreateMembershipNullGroupArgument() {
    // when/then
    assertThatThrownBy(() -> identityService.createMembership("userId", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("groupId is null");
  }

  @Test
  void testFindGroupsByUserIdNullArguments() {
    var groupQuery = identityService.createGroupQuery();
    // when/then
    assertThatThrownBy(() -> groupQuery.groupMember(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("userId is null");
  }

  @Test
  void testFindUsersByGroupUnexistingGroup() {
    List<User> users = identityService.createUserQuery().memberOfGroup("unexistinggroup").list();
    assertThat(users).isNotNull().isEmpty();
  }

  @Test
  void testDeleteGroupNullArguments() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteGroup(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("groupId is null");
  }

  @Test
  void testDeleteMembership() {
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);

    User johndoe = identityService.newUser("johndoe");
    identityService.saveUser(johndoe);
    // Add membership
    identityService.createMembership(johndoe.getId(), sales.getId());

    List<Group> groups = identityService.createGroupQuery().groupMember(johndoe.getId()).list();
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).getId()).isEqualTo("sales");

    // Delete the membership and check members of sales group
    identityService.deleteMembership(johndoe.getId(), sales.getId());
    groups = identityService.createGroupQuery().groupMember(johndoe.getId()).list();
    assertThat(groups).isEmpty();

    identityService.deleteGroup("sales");
    identityService.deleteUser("johndoe");
  }

  @Test
  void testDeleteMembershipWhenUserIsNoMember() {
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);

    User johndoe = identityService.newUser("johndoe");
    identityService.saveUser(johndoe);

    // Delete the membership when the user is no member
    assertThatCode(() -> identityService.deleteMembership(johndoe.getId(), sales.getId()))
      .doesNotThrowAnyException();

    identityService.deleteGroup("sales");
    identityService.deleteUser("johndoe");
  }

  @Test
  void testDeleteMembershipUnexistingGroup() {
    User johndoe = identityService.newUser("johndoe");
    identityService.saveUser(johndoe);
    // No exception should be thrown when group doesn't exist
    assertThatCode(() -> identityService.deleteMembership(johndoe.getId(), "unexistinggroup"))
      .doesNotThrowAnyException();
    identityService.deleteUser(johndoe.getId());
  }

  @Test
  void testDeleteMembershipUnexistingUser() {
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);
    // No exception should be thrown when user doesn't exist
    assertThatCode(() -> identityService.deleteMembership("unexistinguser", sales.getId()))
      .doesNotThrowAnyException();
    identityService.deleteGroup(sales.getId());
  }

  @Test
  void testDeleteMemberschipNullUserArgument() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteMembership(null, "group"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("userId is null");
  }

  @Test
  void testDeleteMemberschipNullGroupArgument() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteMembership("user", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("groupId is null");
  }

  @Test
  void testDeleteUserNullArguments() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteUser(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("userId is null");
  }

  @Test
  void testDeleteUserUnexistingUserId() {
    // No exception should be thrown. Deleting an unexisting user should
    // be ignored silently
    assertThatCode(() -> identityService.deleteUser("unexistinguser"))
      .doesNotThrowAnyException();
  }

  @Test
  void testCheckPassword() {

    // store user with password
    User user = identityService.newUser("secureUser");
    user.setPassword("s3cret");
    identityService.saveUser(user);

    assertThat(identityService.checkPassword(user.getId(), "s3cret")).isTrue();
    assertThat(identityService.checkPassword(user.getId(), "wrong")).isFalse();

    identityService.deleteUser(user.getId());

  }

  @Test
  void testUpdatePassword() {

    // store user with password
    User user = identityService.newUser("secureUser");
    user.setPassword("s3cret");
    identityService.saveUser(user);

    assertThat(identityService.checkPassword(user.getId(), "s3cret")).isTrue();

    user.setPassword("new-password");
    identityService.saveUser(user);

    assertThat(identityService.checkPassword(user.getId(), "new-password")).isTrue();

    identityService.deleteUser(user.getId());

  }

  @Test
  void testCheckPasswordNullSafe() {
    assertThat(identityService.checkPassword("userId", null)).isFalse();
    assertThat(identityService.checkPassword(null, "passwd")).isFalse();
    assertThat(identityService.checkPassword(null, null)).isFalse();
  }

  @Test
  void testUserOptimisticLockingException() {
    User user = identityService.newUser("kermit");
    identityService.saveUser(user);

    User user1 = identityService.createUserQuery().singleResult();
    User user2 = identityService.createUserQuery().singleResult();

    user1.setFirstName("name one");
    identityService.saveUser(user1);

    user2.setFirstName("name two");

    // when/then
    assertThatThrownBy(() -> identityService.saveUser(user2))
      .isInstanceOf(OptimisticLockingException.class);
  }

  @Test
  void testGroupOptimisticLockingException() {
    Group group = identityService.newGroup("group");
    identityService.saveGroup(group);

    Group group1 = identityService.createGroupQuery().singleResult();
    Group group2 = identityService.createGroupQuery().singleResult();

    group1.setName("name one");
    identityService.saveGroup(group1);

    group2.setName("name two");

    // when/then
    assertThatThrownBy(() -> identityService.saveGroup(group2))
      .isInstanceOf(OptimisticLockingException.class);
  }

  @Test
  void testSaveUserWithGenericResourceId() {
    processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/identity/generic.resource.id.whitelist.operaton.cfg.xml")
      .setProcessEngineName(PROCESS_ENGINE_NAME)
      .buildProcessEngine();

    User user = identityService.newUser("*");
    IdentityService identityService1 = processEngine.getIdentityService();

    // when/then
    assertThatThrownBy(() -> identityService1.saveUser(user))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("has an invalid id: id cannot be *. * is a reserved identifier.");
  }

  @Test
  void testSaveGroupWithGenericResourceId() {
    processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/identity/generic.resource.id.whitelist.operaton.cfg.xml")
      .setProcessEngineName(PROCESS_ENGINE_NAME)
      .buildProcessEngine();

    Group group = identityService.newGroup("*");
    var identityService1 = processEngine.getIdentityService();

    // when/then
    assertThatThrownBy(() -> identityService1.saveGroup(group))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("has an invalid id: id cannot be *. * is a reserved identifier.");
  }

  @Test
  void testSetAuthenticatedIdToGenericId() {

    // when/then
    assertThatThrownBy(() -> identityService.setAuthenticatedUserId("*"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid user id provided: id cannot be *. * is a reserved identifier.");
  }

  @Test
  void testSetAuthenticationUserIdToGenericId() {
    List<String> tenentIds = List.of("*");
    // when/then
    assertThatThrownBy(() -> identityService.setAuthentication("aUserId", tenentIds))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("invalid group id provided: id cannot be *. * is a reserved identifier.");
  }

  @Test
  void testSetAuthenticatedTenantIdToGenericId() {
    List<String> tenantIds = List.of("*");
    // when/then
    assertThatThrownBy(() -> identityService.setAuthentication(null, null, tenantIds))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("invalid tenant id provided: id cannot be *. * is a reserved identifier.");
  }

  @Test
  void testSetAuthenticatedUserId() {
    identityService.setAuthenticatedUserId("john");

    Authentication currentAuthentication = identityService.getCurrentAuthentication();

    assertThat(currentAuthentication).isNotNull();
    assertThat(currentAuthentication.getUserId()).isEqualTo("john");
    assertThat(currentAuthentication.getGroupIds()).isNull();
    assertThat(currentAuthentication.getTenantIds()).isNull();
  }

  @Test
  void testSetAuthenticatedUserAndGroups() {
    List<String> groups = Arrays.asList("sales", "development");

    identityService.setAuthentication("john", groups);

    Authentication currentAuthentication = identityService.getCurrentAuthentication();

    assertThat(currentAuthentication).isNotNull();
    assertThat(currentAuthentication.getUserId()).isEqualTo("john");
    assertThat(currentAuthentication.getGroupIds()).isEqualTo(groups);
    assertThat(currentAuthentication.getTenantIds()).isNull();
  }

  @Test
  void testSetAuthenticatedUserGroupsAndTenants() {
    List<String> groups = Arrays.asList("sales", "development");
    List<String> tenants = Arrays.asList("tenant1", "tenant2");

    identityService.setAuthentication("john", groups, tenants);

    Authentication currentAuthentication = identityService.getCurrentAuthentication();

    assertThat(currentAuthentication).isNotNull();
    assertThat(currentAuthentication.getUserId()).isEqualTo("john");
    assertThat(currentAuthentication.getGroupIds()).isEqualTo(groups);
    assertThat(currentAuthentication.getTenantIds()).isEqualTo(tenants);
  }

  @Test
  void testAuthentication() {
    User user = identityService.newUser("johndoe");
    user.setPassword("xxx");
    identityService.saveUser(user);

    assertThat(identityService.checkPassword("johndoe", "xxx")).isTrue();
    assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();

    identityService.deleteUser("johndoe");
  }

  @Test
  @WatchLogger(loggerNames = {IDENTITY_LOGGER}, level = "INFO")
  void testUnsuccessfulAttemptsResultInBlockedUser() throws Exception {
    // given
    User user = identityService.newUser("johndoe");
    user.setPassword("xxx");
    identityService.saveUser(user);

    Date now = parseDate("2000-01-24T13:00:00");
    ClockUtil.setCurrentTime(now);

    // when
    for (int i = 0; i < 11; i++) {
      assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();
      now = DateUtils.addMinutes(now, 1);
      ClockUtil.setCurrentTime(now);
    }

    // then
    assertThat(loggingRule.getFilteredLog(IDENTITY_LOGGER, "The user with id 'johndoe' is permanently locked.")).hasSize(1);
  }

  @Test
  void testSuccessfulLoginAfterFailureAndDelay() {
    User user = identityService.newUser("johndoe");
    user.setPassword("xxx");
    identityService.saveUser(user);

    Date now = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(now);
    assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();
    ClockUtil.setCurrentTime(DateUtils.addSeconds(now, 30));
    assertThat(identityService.checkPassword("johndoe", "xxx")).isTrue();

    identityService.deleteUser("johndoe");
  }

  @Test
  @WatchLogger(loggerNames = {IDENTITY_LOGGER}, level = "INFO")
  void testSuccessfulLoginAfterFailureWithoutDelay() {
    // given
    User user = identityService.newUser("johndoe");
    user.setPassword("xxx");
    identityService.saveUser(user);

    Date now = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(now);
    assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();
    assertThat(identityService.checkPassword("johndoe", "xxx")).isFalse();

    // assume
    assertThat(loggingRule.getFilteredLog(IDENTITY_LOGGER, "The user with id 'johndoe' is locked.")).hasSize(1);

    // when
    ClockUtil.setCurrentTime(DateUtils.addSeconds(now, 30));
    boolean checkPassword = identityService.checkPassword("johndoe", "xxx");

    // then
    assertThat(checkPassword).isTrue();
  }

  @Test
  @WatchLogger(loggerNames = {IDENTITY_LOGGER}, level = "INFO")
  void testUnsuccessfulLoginAfterFailureWithoutDelay() {
    // given
    User user = identityService.newUser("johndoe");
    user.setPassword("xxx");
    identityService.saveUser(user);

    Date now = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(now);
    assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();

    ClockUtil.setCurrentTime(DateUtils.addSeconds(now, 1));
    Date expectedLockExpitation = DateUtils.addSeconds(now, 3);

    // when try again before exprTime
    assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();

    // then
    assertThat(loggingRule.getFilteredLog(IDENTITY_LOGGER, "The lock will expire at " + expectedLockExpitation)).hasSize(1);
  }

  @Test
  void testFindGroupsByUserAndType() {
    Group sales = identityService.newGroup("sales");
    sales.setType("hierarchy");
    identityService.saveGroup(sales);

    Group development = identityService.newGroup("development");
    development.setType("hierarchy");
    identityService.saveGroup(development);

    Group admin = identityService.newGroup("admin");
    admin.setType("security-role");
    identityService.saveGroup(admin);

    Group user = identityService.newGroup("user");
    user.setType("security-role");
    identityService.saveGroup(user);

    User johndoe = identityService.newUser("johndoe");
    identityService.saveUser(johndoe);

    User joesmoe = identityService.newUser("joesmoe");
    identityService.saveUser(joesmoe);

    User jackblack = identityService.newUser("jackblack");
    identityService.saveUser(jackblack);

    identityService.createMembership("johndoe", "sales");
    identityService.createMembership("johndoe", "user");
    identityService.createMembership("johndoe", "admin");

    identityService.createMembership("joesmoe", "user");

    List<Group> groups = identityService.createGroupQuery().groupMember("johndoe").groupType("security-role").list();
    Set<String> groupIds = getGroupIds(groups);
    Set<String> expectedGroupIds = new HashSet<>();
    expectedGroupIds.add("user");
    expectedGroupIds.add("admin");
    assertThat(groupIds).isEqualTo(expectedGroupIds);

    groups = identityService.createGroupQuery().groupMember("joesmoe").groupType("security-role").list();
    groupIds = getGroupIds(groups);
    expectedGroupIds = new HashSet<>();
    expectedGroupIds.add("user");
    assertThat(groupIds).isEqualTo(expectedGroupIds);

    groups = identityService.createGroupQuery().groupMember("jackblack").groupType("security-role").list();
    assertThat(groups).isEmpty();

    identityService.deleteGroup("sales");
    identityService.deleteGroup("development");
    identityService.deleteGroup("admin");
    identityService.deleteGroup("user");
    identityService.deleteUser("johndoe");
    identityService.deleteUser("joesmoe");
    identityService.deleteUser("jackblack");
  }

  @Test
  void testUser() {
    User user = identityService.newUser("johndoe");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("johndoe@alfresco.com");
    identityService.saveUser(user);

    user = identityService.createUserQuery().userId("johndoe").singleResult();
    assertThat(user.getId()).isEqualTo("johndoe");
    assertThat(user.getFirstName()).isEqualTo("John");
    assertThat(user.getLastName()).isEqualTo("Doe");
    assertThat(user.getEmail()).isEqualTo("johndoe@alfresco.com");

    identityService.deleteUser("johndoe");
  }

  @Test
  void testGroup() {
    Group group = identityService.newGroup("sales");
    group.setName("Sales division");
    identityService.saveGroup(group);

    group = identityService.createGroupQuery().groupId("sales").singleResult();
    assertThat(group.getId()).isEqualTo("sales");
    assertThat(group.getName()).isEqualTo("Sales division");

    identityService.deleteGroup("sales");
  }

  @Test
  void testMembership() {
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);

    Group development = identityService.newGroup("development");
    identityService.saveGroup(development);

    User johndoe = identityService.newUser("johndoe");
    identityService.saveUser(johndoe);

    User joesmoe = identityService.newUser("joesmoe");
    identityService.saveUser(joesmoe);

    User jackblack = identityService.newUser("jackblack");
    identityService.saveUser(jackblack);

    identityService.createMembership("johndoe", "sales");
    identityService.createMembership("joesmoe", "sales");

    identityService.createMembership("joesmoe", "development");
    identityService.createMembership("jackblack", "development");

    List<Group> groups = identityService.createGroupQuery().groupMember("johndoe").list();
    assertThat(getGroupIds(groups)).containsExactly("sales");

    groups = identityService.createGroupQuery().groupMember("joesmoe").list();
    assertThat(getGroupIds(groups)).containsExactlyInAnyOrder("sales", "development");

    groups = identityService.createGroupQuery().groupMember("jackblack").list();
    assertThat(getGroupIds(groups)).containsExactly("development");

    List<User> users = identityService.createUserQuery().memberOfGroup("sales").list();
    assertThat(getUserIds(users)).containsExactlyInAnyOrder("johndoe", "joesmoe");

    users = identityService.createUserQuery().memberOfGroup("development").list();
    assertThat(getUserIds(users)).containsExactly("joesmoe", "jackblack");

    identityService.deleteGroup("sales");
    identityService.deleteGroup("development");

    identityService.deleteUser("jackblack");
    identityService.deleteUser("joesmoe");
    identityService.deleteUser("johndoe");
  }

  @Test
  void testInvalidUserId() {
    String invalidId = "john doe";
    User user = identityService.newUser(invalidId);

    try {
      identityService.saveUser(user);
      fail("Invalid user id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("User", invalidId));
    }
  }

  @Test
  void testInvalidUserIdOnSave() {
    String invalidId = "john doe";
    User updatedUser = identityService.newUser("john");
    updatedUser.setId(invalidId);
    try {
      identityService.saveUser(updatedUser);

      fail("Invalid user id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("User", invalidId));
    }
  }

  @Test
  void testInvalidGroupId() {
    String invalidId = "john's group";
    Group group = identityService.newGroup(invalidId);
    try {
      identityService.saveGroup(group);
      fail("Invalid group id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("Group", invalidId));
    }
  }

  @Test
  void testInvalidGroupIdOnSave() {
    String invalidId = "john's group";
    Group updatedGroup = identityService.newGroup("group");
    updatedGroup.setId(invalidId);
    try {
      identityService.saveGroup(updatedGroup);

      fail("Invalid group id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("Group", invalidId));
    }
  }

  @Test
  void testOperatonAdminId() {
    String operatonAdminID = "operaton-admin";
    try {
      identityService.newUser(operatonAdminID);
      identityService.newGroup(operatonAdminID);
      identityService.newTenant(operatonAdminID);
    } catch (ProcessEngineException ex) {
      fail(operatonAdminID + " should be a valid id.");
    }
  }

  @Test
  void testCustomResourceWhitelist() {
    processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/identity/custom.whitelist.operaton.cfg.xml")
      .setProcessEngineName(PROCESS_ENGINE_NAME)
      .buildProcessEngine();

    IdentityService processEngineIdentityService = processEngine.getIdentityService();

    String invalidUserId = "johnDoe";
    String invalidGroupId = "johnsGroup";
    String invalidTenantId = "johnsTenant";

    User user = processEngineIdentityService.newUser(invalidUserId);

    try {
      processEngineIdentityService.saveUser(user);
      fail("Invalid user id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("User", invalidUserId));
    }

    Group johnsGroup = processEngineIdentityService.newGroup("johnsGroup");

    try {
      processEngineIdentityService.saveGroup(johnsGroup);
      fail("Invalid group id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("Group", invalidGroupId));
    }

    Tenant tenant = processEngineIdentityService.newTenant(invalidTenantId);
    try {
      processEngineIdentityService.saveTenant(tenant);
      fail("Invalid tenant id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("Tenant", invalidTenantId));
    }
  }

  @Test
  void testSeparateResourceWhitelistPatterns() {
    processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/identity/custom.resource.whitelist.operaton.cfg.xml")
      .setProcessEngineName(PROCESS_ENGINE_NAME)
      .buildProcessEngine();

    IdentityService processEngineIdentityService = processEngine.getIdentityService();

    String invalidUserId = "12345";
    String invalidGroupId = "johnsGroup";
    String invalidTenantId = "!@##$%";

    User user = processEngineIdentityService.newUser(invalidUserId);

    // pattern: [a-zA-Z]+
    try {
      processEngineIdentityService.saveUser(user);
      fail("Invalid user id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("User", invalidUserId));
    }

    Group group = processEngineIdentityService.newGroup(invalidGroupId);

    // pattern: \d+
    try {
      processEngineIdentityService.saveGroup(group);
      fail("Invalid group id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("Group", invalidGroupId));
    }

    Tenant tenant = processEngineIdentityService.newTenant(invalidTenantId);
    // new general pattern (used for tenant whitelisting): [a-zA-Z0-9]+
    try {
      processEngineIdentityService.saveTenant(tenant);
      fail("Invalid tenant id exception expected!");
    } catch (ProcessEngineException ex) {
      assertThat(ex.getMessage()).isEqualTo(INVALID_ID_MESSAGE.formatted("Tenant", invalidTenantId));
    }
  }

  @Test
  void shouldCreateUserWithEmptyUserId() {
    User user = identityService.newUser("");
    assertThat(user).isNotNull();
  }

  @Test
  void shouldNotIncludePlaintextPasswordInUserToString() {
    // given
    User user = identityService.newUser("id");

    String password = "this is a password";
    user.setPassword(password);

    // when
    String toString = user.toString();

    // then
    assertThat(toString).doesNotContain(password);
  }

  @Test
  void shouldNotIncludeHashedPasswordAndSaltInUserToString() {
    // given
    User user = identityService.newUser("id");

    String password = "this is a password";
    user.setPassword(password);

    identityService.saveUser(user);

    UserEntity userEntity = (UserEntity) user;
    String salt = userEntity.getSalt();
    String hashedPassword = userEntity.getPassword();

    // when
    String toString = user.toString();

    // then
    assertThat(toString)
      .doesNotContain(salt)
      .doesNotContain(hashedPassword);

  }

  protected Set<String> getGroupIds(List<Group> groups) {
    Set<String> groupIds = new HashSet<>();
    for (Group group : groups) {
      groupIds.add(group.getId());
    }
    return groupIds;
  }

  protected Set<String> getUserIds(List<User> users) {
    Set<String> userIds = new HashSet<>();
    for (User user : users) {
      userIds.add(user.getId());
    }
    return userIds;
  }

}
