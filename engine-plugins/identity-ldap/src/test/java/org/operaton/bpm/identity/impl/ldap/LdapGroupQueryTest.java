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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.GROUP;
import static org.operaton.bpm.identity.ldap.util.LdapTestUtilities.checkPagingResults;
import static org.operaton.bpm.identity.ldap.util.LdapTestUtilities.testGroupPaging;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.ldap.util.LdapTestExtension;


class LdapGroupQueryTest {

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
  void testCountGroups() {
    // given

    // when
    GroupQuery groupQuery = identityService.createGroupQuery();

    // then
    assertThat(groupQuery.listPage(0, Integer.MAX_VALUE)).hasSize(6);
    assertThat(groupQuery.count()).isEqualTo(6);
  }

  @Test
  void testQueryNoFilter() {
    // given

    // when
    List<Group> groupList = identityService.createGroupQuery().list();

    // then
    assertThat(groupList).hasSize(6);
  }

  @Test
  void testFilterByGroupId() {
    // given

    // when
    Group group = identityService.createGroupQuery().groupId("management").singleResult();

    // then
    assertThat(group).isNotNull();
    // validate result
    assertThat(group.getId()).isEqualTo("management");
    assertThat(group.getName()).isEqualTo("management");
  }

  @Test
  void testFilterByNonexistingGroupId() {
    // given

    // when
    Group group = identityService.createGroupQuery().groupId("whatever").singleResult();

    // then
    assertThat(group).isNull();
  }

  @Test
  void testFilterByGroupIdIn() {
    // given

    // when
    List<Group> groups = identityService.createGroupQuery().groupIdIn("external", "management").list();

    // then
    assertThat(groups).hasSize(2);
    assertThat(groups).extracting("id").containsOnly("external", "management");
  }

  @Test
  void testFilterByGroupName() {
    // given

    // when
    Group group = identityService.createGroupQuery().groupName("management").singleResult();

    // then
    assertThat(group).isNotNull();
    // validate result
    assertThat(group.getId()).isEqualTo("management");
    assertThat(group.getName()).isEqualTo("management");
  }

  @Test
  void testFilterByNonexistingGroupName() {
    // given

    // when
    Group group = identityService.createGroupQuery().groupName("whatever").singleResult();

    // then
    assertThat(group).isNull();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
          "Trailing Wildcard, manage*, management",
          "Leading Wildcard, *agement, management",
          "Leading & Trailing Wildcard, *ageme*, management",
          "Middle Wildcard, man*nt, management",
          "Wildcard converted from DB Wildcard, manage%, management",
  })
  void testFilterByGroupNameLike(String description, String groupNameLike, String expectedGroupName) {
      // given

      // when
      Group group = identityService.createGroupQuery().groupNameLike(groupNameLike).singleResult();

      // then
      assertThat(group).isNotNull();
      assertThat(group.getId()).isEqualTo(expectedGroupName);
      assertThat(group.getName()).isEqualTo(expectedGroupName);
  }

  @Test
  void testFilterByNonexistingGroupNameLike() {
    // given

    // when
    Group group = identityService.createGroupQuery().groupNameLike("what*").singleResult();

    // then
    assertThat(group).isNull();
  }

  @Test
  void testFilterByGroupMember() {
    // given

    // when
    List<Group> list = identityService.createGroupQuery().groupMember("sam").list();

    // then
    assertThat(list).hasSize(3);
    assertThat(list).extracting("name").containsOnly("development", "management", "all");
  }

  @Test
  void testFilterByNonexistingGroupMember() {
    // given

    // when
    List<Group> list = identityService.createGroupQuery().groupMember("non-existing").list();

    // then
    assertThat(list).isEmpty();
  }

  @Test
  void testFilterByGroupMemberSpecialCharacter() {
    // given

    // when
    List<Group> list = identityService.createGroupQuery().groupMember("uncledeadly(IT)").list();

    // then
    assertThat(list).hasSize(2);
    assertThat(list).extracting("name").containsOnly("sales", "all");
  }

  @Test
  void testFilterByGroupMemberPosix() {
    // given
    // by default the configuration does not use posix groups
    LdapConfiguration ldapConfiguration = new LdapConfiguration();
    ldapConfiguration.setGroupMemberAttribute("memberUid");
    ldapConfiguration.setGroupSearchFilter("(someFilter)");

    LdapIdentityProviderSession session = new LdapIdentityProviderSession(ldapConfiguration) {
      // mock getDnForUser
      @Override
      protected String getDnForUser(String userId) {
        return userId+ ", fullDn";
      }
    };

    // when I query for groups by group member
    LdapGroupQuery query = new LdapGroupQuery();
    query.groupMember("jonny");

    // then the full DN is requested. This is the default behavior.
    String filter = session.getGroupSearchFilter(query);
    assertThat(filter).isEqualTo("(&(someFilter)(memberUid=jonny, fullDn))");

    // If I turn on posix groups
    ldapConfiguration.setUsePosixGroups(true);

    //  then the filter string does not contain the full DN for the
    // user but the simple (unqualified) userId as provided in the query
    filter = session.getGroupSearchFilter(query);
    assertThat(filter).isEqualTo("(&(someFilter)(memberUid=jonny))");
  }


  @Test
  void testPagination() {
    testGroupPaging(identityService);
  }

  @Test
  void testPaginationWithAuthenticatedUser() {
    createGrantAuthorization(GROUP, "management", "oscar", READ);
    createGrantAuthorization(GROUP, "consulting", "oscar", READ);
    createGrantAuthorization(GROUP, "external", "oscar", READ);

    try {
      processEngineConfiguration.setAuthorizationEnabled(true);

      identityService.setAuthenticatedUserId("oscar");

      Set<String> groupNames = new HashSet<>();
      List<Group> groups = identityService.createGroupQuery().listPage(0, 2);
      assertThat(groups).hasSize(2);
      checkPagingResults(groupNames, groups.get(0).getId(), groups.get(1).getId());

      groups = identityService.createGroupQuery().listPage(2, 2);
      assertThat(groups).hasSize(1);
      assertThat(groupNames).doesNotContain(groups.get(0).getId());
      groupNames.add(groups.get(0).getId());

      groups = identityService.createGroupQuery().listPage(4, 2);
      assertThat(groups).isEmpty();

      identityService.setAuthenticatedUserId("kermit");

      groups = identityService.createGroupQuery().listPage(0, 2);
      assertThat(groups).isEmpty();

    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();

      for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
        authorizationService.deleteAuthorization(authorization.getId());
      }

    }
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
