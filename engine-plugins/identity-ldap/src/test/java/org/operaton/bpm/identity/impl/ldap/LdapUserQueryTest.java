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
package org.operaton.bpm.identity.impl.ldap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.ldap.util.LdapTestExtension;

import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.USER;
import static org.operaton.bpm.identity.ldap.util.LdapTestUtilities.checkPagingResults;
import static org.operaton.bpm.identity.ldap.util.LdapTestUtilities.testUserPaging;
import static org.operaton.bpm.identity.ldap.util.LdapTestUtilities.testUserPagingWithMemberOfGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LdapUserQueryTest {

  @RegisterExtension
  @Order(1)
  static LdapTestExtension ldapExtension = new LdapTestExtension();

  @RegisterExtension
  @Order(2)
  static ProcessEngineExtension engineRule = ProcessEngineExtension
          .builder()
          .configurator(ldapExtension::injectLdapUrlIntoProcessEngineConfiguration)
          .closeEngineAfterAllTests()
          .build();

  ProcessEngineConfiguration processEngineConfiguration;
  IdentityService identityService;
  AuthorizationService authorizationService;

  @Test
  void testCountUsers() {
    // given

    // when
    UserQuery userQuery = identityService.createUserQuery();

    // then
    assertThat(userQuery.listPage(0, Integer.MAX_VALUE)).hasSize(12);
    assertThat(userQuery.count()).isEqualTo(ldapExtension.getLdapTestContext().numberOfGeneratedUsers());
  }

  @Test
  void testQueryNoFilter() {
    // given

    // when
    List<User> result = identityService.createUserQuery().list();

    // then
    assertThat(result).hasSize(ldapExtension.getLdapTestContext().numberOfGeneratedUsers());
  }

  @Test
  void testFilterByUserId() {
    // given

    // when
    User user = identityService.createUserQuery().userId("oscar").singleResult();

    // then
    assertThat(user).isNotNull();

    // validate user
    assertThat(user.getId()).isEqualTo("oscar");
    assertThat(user.getFirstName()).isEqualTo("Oscar");
    assertThat(user.getLastName()).isEqualTo("The Crouch");
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void testFilterByNonexistentUserId() {
    // given

    // when
    User user = identityService.createUserQuery().userId("non-existing").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void testFilterByUserIdIn() {
    // given

    // when
    List<User> users = identityService.createUserQuery().userIdIn("oscar", "monster").list();

    // then
    assertThat(users).hasSize(2);
    assertThat(users).extracting("id").containsOnly("oscar", "monster");
  }

  @Test
  void testFilterByNonExistingUserIdIn() {
    // given

    // when
    List<User> users = identityService.createUserQuery().userIdIn("oscar", "monster", "sam", "non-existing").list();

    // then
    assertThat(users)
      .isNotNull()
      .hasSize(3)
      .extracting("id").containsOnly("oscar", "monster", "sam");
  }

  @Test
  void testFilterByUserIdWithCapitalization() {
    try {
      // given
      processEngineConfiguration.setAuthorizationEnabled(true);
      identityService.setAuthenticatedUserId("Oscar");

      // when
      User user = identityService.createUserQuery().userId("Oscar").singleResult();

      // then
      assertThat(user).isNotNull();

      // validate user
      assertThat(user.getId()).isEqualTo("oscar");
      assertThat(user.getFirstName()).isEqualTo("Oscar");
      assertThat(user.getLastName()).isEqualTo("The Crouch");
      assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();
    }
  }

  @Test
  void testFilterByFirstname() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstName("Oscar").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void testFilterByNonexistingFirstname() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstName("non-existing").singleResult();

    // then
    assertThat(user).isNull();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
          "Trailing Wildcard, Osc*, Oscar",
          "Leading Wildcard, *car, Oscar",
          "Leading & Trailing Wildcard, *sca*, Oscar",
          "Middle Wildcard, O*ar, Oscar",
          "Wildcard converted from DB Wildcard, Osc%, Oscar",
  })
  void testFilterByFirstnameLike(String testName, String nameSearchText, String expectedNameFound) {
    // when
    User user = identityService.createUserQuery().userFirstNameLike(nameSearchText).singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo(expectedNameFound);
  }

  @Test
  void testFilterByNonexistingFirstnameLike() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstNameLike("non-exist*").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void testFilterByLastname() {
    // given

    // when
    User user = identityService.createUserQuery().userLastName("The Crouch").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void testFilterByNonexistingLastname() {
    // given

    // when
    User user = identityService.createUserQuery().userLastName("non-existing").singleResult();

    // then
    assertThat(user).isNull();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
          "Trailing Wildcard, The Cro*, The Crouch",
          "Leading Wildcard, * Crouch, The Crouch",
          "Leading & Trailing Wildcard, * Cro*, The Crouch",
          "Middle Wildcard, The *uch, The Crouch",
          "Wildcard converted from DB Wildcard, The Cro%, The Crouch",
  })
  void testFilterByLastnameLike(String testName, String nameSearchText, String expectedNameFound) {
    // when
    User user = identityService.createUserQuery().userLastNameLike(nameSearchText).singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo(expectedNameFound);
  }

  @Test
  void testFilterByNonexistingLastnameLike() {
    // given

    // when
    User user = identityService.createUserQuery().userLastNameLike("non-exist*").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void testFilterByEmail() {
    // given

    // when
    User user = identityService.createUserQuery().userEmail("oscar@operaton.org").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void testFilterByNonexistingEmail() {
    // given

    // when
      User user = identityService.createUserQuery().userEmail("non-exist").singleResult();

    // then
    assertThat(user).isNull();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
          "Trailing Wildcard, oscar@*, oscar@operaton.org",
          "Leading Wildcard, *car@operaton.org, oscar@operaton.org",
          "Leading & Trailing Wildcard, *car@*, oscar@operaton.org",
          "Middle Wildcard, oscar@*.org, oscar@operaton.org",
          "Wildcard converted from DB Wildcard, oscar@%, oscar@operaton.org",
  })
  void testFilterByEmailLike(String testName, String nameSearchText, String expectedNameFound) {
    // when
    User user = identityService.createUserQuery().userEmailLike(nameSearchText).singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo(expectedNameFound);
  }

  @Test
  void testFilterByNonexistingEmailLike() {
    // given

    // when
    User user = identityService.createUserQuery().userEmailLike("non-exist*").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void testFilterByGroupId() {
    // given

    // when
    List<User> result = identityService.createUserQuery().memberOfGroup("development").list();

    // then
    assertThat(result).hasSize(3);
    assertThat(result).extracting("id").containsOnly("kermit", "sam", "oscar");
  }

  @Test
  void testFilterByGroupIdAndFirstname() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userFirstName("Oscar")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void testFilterByGroupIdAndId() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userId("oscar")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("oscar");
  }

  @Test
  void testFilterByGroupIdAndLastname() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userLastName("The Crouch")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void testFilterByGroupIdAndEmail() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userEmail("oscar@operaton.org")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void testFilterByGroupIdAndEmailLike() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userEmailLike("*@operaton.org")
            .list();

    // then
    assertThat(result).hasSize(3);
    assertThat(result).extracting("email").containsOnly("kermit@operaton.org", "sam@operaton.org", "oscar@operaton.org");
  }

  @Test
  void testFilterByGroupIdAndIdForDnUsingCn() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("external")
            .userId("fozzie")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("fozzie");
  }

  @Test
  void testAuthenticatedUserSeesThemselves() {
    try {
      // given
      processEngineConfiguration.setAuthorizationEnabled(true);
      identityService.setAuthenticatedUserId("oscar");

      // when
      User user = identityService.createUserQuery().singleResult();

      // then
      assertThat(user).isNotNull();
      assertThat(user.getId()).isEqualTo("oscar");
    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();
    }
  }

  @Test
  void testNonexistingAuthenticatedUserDoesNotSeeThemselve() {
    try {
      // given
      processEngineConfiguration.setAuthorizationEnabled(true);
      identityService.setAuthenticatedUserId("non-existing");

      // when
      User user = identityService.createUserQuery().singleResult();

      // then
      assertThat(user).isNull();
    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();
    }
  }

  @Test
  void testPagination() {
    testUserPaging(identityService, ldapExtension.getLdapTestContext());
  }

  @Test
  void testPaginationWithMemberOfGroup() {
    testUserPagingWithMemberOfGroup(identityService);
  }

  @Test
  void testPaginationWithAuthenticatedUser() {
    createGrantAuthorization(USER, "kermit", "oscar", READ);
    createGrantAuthorization(USER, "sam", "oscar", READ);
    createGrantAuthorization(USER, "monster", "oscar", READ);
    createGrantAuthorization(USER, "bobo", "oscar", READ);

    try {
      processEngineConfiguration.setAuthorizationEnabled(true);

      identityService.setAuthenticatedUserId("oscar");

      Set<String> userNames = new HashSet<>();
      List<User> users = identityService.createUserQuery().listPage(0, 2);
      assertThat(users).hasSize(2);
      checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

      users = identityService.createUserQuery().listPage(2, 2);
      assertThat(users).hasSize(2);
      checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

      users = identityService.createUserQuery().listPage(4, 2);
      assertThat(users).hasSize(1);
      assertThat(userNames).doesNotContain(users.get(0).getId());
      userNames.add(users.get(0).getId());

      identityService.setAuthenticatedUserId("kermit");

      users = identityService.createUserQuery().listPage(0, 2);
      assertThat(users).hasSize(1);

      assertThat(users.get(0).getId()).isEqualTo("kermit");

      users = identityService.createUserQuery().listPage(2, 2);
      assertThat(users).isEmpty();

    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();

      for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
        authorizationService.deleteAuthorization(authorization.getId());
      }

    }
  }

  @Test
  void testNativeQueryFail() {
    assertThatThrownBy(() -> identityService.createNativeUserQuery())
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Native user queries are not supported for LDAP");
    System.out.println("FUZ");
  }

  protected void createGrantAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
    Authorization authorization = createAuthorization(AUTH_TYPE_GRANT, resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }
    authorizationService.saveAuthorization(authorization);
  }

  protected Authorization createAuthorization(int type, Resource resource, String resourceId) {
    Authorization authorization = authorizationService.createNewAuthorization(type);

    authorization.setResource(resource);
    if (resourceId != null) {
      authorization.setResourceId(resourceId);
    }

    return authorization;
  }

}
