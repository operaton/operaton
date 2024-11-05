/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.identity.impl.ldap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.ldap.util.LdapTestEnvironmentExtension;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Represents a test case where the sortControlSupport property is enabled.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ExtendWith(LdapTestEnvironmentExtension.class)
public class LdapEnableSortControlSupportTest {

  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().configurationResource("operaton.ldap.enable.sort.control.support.cfg.xml").build();

  IdentityService identityService;

  /**
   * FirstName
   */
  @Test
  void orderByUserFirstNameAsc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserFirstName().asc().list();
    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getFirstName))
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getId()).isEqualTo(manualOrderedUsers.get(i).getId());
    }
  }

  @Test
  void orderByUserFirstNameDesc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserFirstName().desc().list();

    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getFirstName).reversed())
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getId()).isEqualTo(manualOrderedUsers.get(i).getId());
    }
  }

  /**
   * LastName
   */
  @Test
  void orderByUserLastNameAsc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserLastName().asc().list();

    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getLastName))
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getLastName()).isEqualTo(manualOrderedUsers.get(i).getLastName());
    }
  }

  @Test
  void orderByUserLastNameDesc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserLastName().desc().list();

    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getLastName).reversed())
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getLastName()).isEqualTo(manualOrderedUsers.get(i).getLastName());
    }
  }

  /**
   * EMAIL
   */
  @Test
  void orderByUserEmailAsc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserEmail().asc().list();
    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getEmail))
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getId()).isEqualTo(manualOrderedUsers.get(i).getId());
    }
  }

  @Test
  void orderByUserEmailDesc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserEmail().desc().list();

    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getEmail).reversed())
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getId()).isEqualTo(manualOrderedUsers.get(i).getId());
    }
  }

  /**
   * ID
   */
  @Test
  void orderByUserIdAsc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserId().asc().list();
    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getId))
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getId()).isEqualTo(manualOrderedUsers.get(i).getId());
    }
  }

  @Test
  void orderByUserIdDesc() {
    List<User> orderedUsers = identityService.createUserQuery().orderByUserId().desc().list();

    List<User> manualOrderedUsers = identityService.createUserQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(User::getId).reversed())
        .toList();

    assertThat(orderedUsers).hasSize(manualOrderedUsers.size());

    for (int i = 0; i < orderedUsers.size(); i++) {
      assertThat(orderedUsers.get(i).getId()).isEqualTo(manualOrderedUsers.get(i).getId());
    }
  }

  /**
   * Group ID Ordering
   */
  @Test
  void orderByGroupIdAsc() {
    List<Group> orderedGroup = identityService.createGroupQuery().orderByGroupId().asc().list();
    List<Group> manualOrderedGroups = identityService.createGroupQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(Group::getId))
        .toList();

    assertThat(orderedGroup).hasSize(manualOrderedGroups.size());

    for (int i = 0; i < orderedGroup.size(); i++) {
      assertThat(orderedGroup.get(i).getId()).isEqualTo(manualOrderedGroups.get(i).getId());
    }
  }

  @Test
  void orderByGroupIdDesc() {
    List<Group> orderedGroup = identityService.createGroupQuery().orderByGroupId().desc().list();
    List<Group> manualOrderedGroups = identityService.createGroupQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(Group::getId).reversed())
        .toList();

    assertThat(orderedGroup).hasSize(manualOrderedGroups.size());

    for (int i = 0; i < orderedGroup.size(); i++) {
      assertThat(orderedGroup.get(i).getId()).isEqualTo(manualOrderedGroups.get(i).getId());
    }
  }

  /**
   * Group Name Ordering
   */
  @Test
  void orderByGroupNameAsc() {
    List<Group> orderedGroup = identityService.createGroupQuery().orderByGroupName().asc().list();
    List<Group> manualOrderedGroups = identityService.createGroupQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(Group::getName))
        .toList();

    assertThat(orderedGroup).hasSize(manualOrderedGroups.size());

    for (int i = 0; i < orderedGroup.size(); i++) {
      assertThat(orderedGroup.get(i).getId()).isEqualTo(manualOrderedGroups.get(i).getId());
    }
  }

  @Test
  void orderByGroupNameDesc() {
    List<Group> orderedGroup = identityService.createGroupQuery().orderByGroupName().desc().list();
    List<Group> manualOrderedGroups = identityService.createGroupQuery()
        .list()
        .stream()
        .sorted(Comparator.comparing(Group::getName).reversed())
        .toList();

    assertThat(orderedGroup).hasSize(manualOrderedGroups.size());

    for (int i = 0; i < orderedGroup.size(); i++) {
      assertThat(orderedGroup.get(i).getId()).isEqualTo(manualOrderedGroups.get(i).getId());
    }
  }

}
