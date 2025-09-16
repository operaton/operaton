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
package org.operaton.bpm.identity.ldap.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Contains some test utilities to test the Ldap plugin.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public final class LdapTestUtilities {

  public static void checkPagingResults(Set<String> results, String result1, String result2) {
    assertNotSame(result1, result2);
    assertFalse(results.contains(result1));
    results.add(result1);
    assertFalse(results.contains(result2));
    results.add(result2);
  }

  public static void testGroupPaging(IdentityService identityService) {
    Set<String> groupNames = new HashSet<>();
    List<Group> groups = identityService.createGroupQuery().listPage(0, 2);
    assertThat(groups.size()).isEqualTo(2);
    checkPagingResults(groupNames, groups.get(0).getId(), groups.get(1).getId());

    groups = identityService.createGroupQuery().listPage(2, 2);
    assertThat(groups.size()).isEqualTo(2);
    checkPagingResults(groupNames, groups.get(0).getId(), groups.get(1).getId());

    groups = identityService.createGroupQuery().listPage(4, 2);
    assertThat(groups.size()).isEqualTo(2);
    assertFalse(groupNames.contains(groups.get(0).getId()));
    groupNames.add(groups.get(0).getId());

    groups = identityService.createGroupQuery().listPage(6, 2);
    assertThat(groups.size()).isEqualTo(0);
  }

  public static void testUserPaging(IdentityService identityService, LdapTestContext ldapTestContext) {
    Set<String> userNames = new HashSet<>();
    List<User> users = identityService.createUserQuery().listPage(0, 2);
    assertThat(users.size()).isEqualTo(2);
    checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

    users = identityService.createUserQuery().listPage(2, 2);
    assertThat(users.size()).isEqualTo(2);
    checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

    users = identityService.createUserQuery().listPage(4, 2);
    assertThat(users.size()).isEqualTo(2);
    checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

    users = identityService.createUserQuery().listPage(6, 2);
    assertThat(users.size()).isEqualTo(2);
    checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

    // over the page.
    users = identityService.createUserQuery().listPage(ldapTestContext.numberOfGeneratedUsers() + 1, 2);
    assertThat(users.size()).isEqualTo(0);
  }

  public static void testUserPagingWithMemberOfGroup(IdentityService identityService) {
    Set<String> userNames = new HashSet<>();
    List<User> users = identityService.createUserQuery().memberOfGroup("all").listPage(0, 2);
    assertThat(users.size()).isEqualTo(2);
    checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

    users = identityService.createUserQuery().memberOfGroup("all").listPage(2, 2);
    assertThat(users.size()).isEqualTo(2);
    checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

    users = identityService.createUserQuery().memberOfGroup("all").listPage(4, 2);
    assertThat(users.size()).isEqualTo(2);
    checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

    users = identityService.createUserQuery().memberOfGroup("all").listPage(11, 2);
    assertThat(users.size()).isEqualTo(1);
    assertFalse(userNames.contains(users.get(0).getId()));

    users = identityService.createUserQuery().memberOfGroup("all").listPage(12, 2);
    assertThat(users.size()).isEqualTo(0);
  }

  private LdapTestUtilities() {
  }
}
