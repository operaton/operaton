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
package org.operaton.bpm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.UserEntity;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;


/**
 * @author Joram Barrez
 */
@ExtendWith(ProcessEngineExtension.class)
class UserQueryTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;
  protected ManagementService managementService;

  @BeforeEach
  void setUp() {


    createUser("kermit", "Kermit_", "The_frog", "kermit_@muppetshow.com");
    createUser("fozzie", "Fozzie", "Bear", "fozzie@muppetshow.com");
    createUser("gonzo", "Gonzo", "The great", "gonzo@muppetshow.com");

    identityService.saveGroup(identityService.newGroup("muppets"));
    identityService.saveGroup(identityService.newGroup("frogs"));

    identityService.saveTenant(identityService.newTenant("tenant"));

    identityService.createMembership("kermit", "muppets");
    identityService.createMembership("kermit", "frogs");
    identityService.createMembership("fozzie", "muppets");
    identityService.createMembership("gonzo", "muppets");

    identityService.createTenantUserMembership("tenant", "kermit");
  }

  private User createUser(String id, String firstName, String lastName, String email) {
    User user = identityService.newUser(id);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(email);
    identityService.saveUser(user);
    return user;
  }

  @AfterEach
  void tearDown() {
    identityService.deleteUser("kermit");
    identityService.deleteUser("fozzie");
    identityService.deleteUser("gonzo");

    identityService.deleteGroup("muppets");
    identityService.deleteGroup("frogs");

    identityService.deleteTenant("tenant");


  }

  @Test
  void testQueryByNoCriteria() {
    UserQuery query = identityService.createUserQuery();
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryById() {
    UserQuery query = identityService.createUserQuery().userId("kermit");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidId() {
    UserQuery query = identityService.createUserQuery().userId("invalid");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery();

    try {
      userQuery.userId(null);
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByFirstName() {
    UserQuery query = identityService.createUserQuery().userFirstName("Gonzo");
    verifyQueryResults(query, 1);

    User result = query.singleResult();
    assertThat(result.getId()).isEqualTo("gonzo");
  }

  @Test
  void testQueryByInvalidFirstName() {
    UserQuery query = identityService.createUserQuery().userFirstName("invalid");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery().userFirstName(null);

    try {
      userQuery.singleResult();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByFirstNameLike() {
    UserQuery query = identityService.createUserQuery().userFirstNameLike("%o%");
    verifyQueryResults(query, 2);

    query = identityService.createUserQuery().userFirstNameLike("Ker%");
    verifyQueryResults(query, 1);

    identityService.createUserQuery().userFirstNameLike("%mit\\_");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidFirstNameLike() {
    UserQuery query = identityService.createUserQuery().userFirstNameLike("%mispiggy%");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery();

    try {
      userQuery.userFirstNameLike(null);
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByLastName() {
    UserQuery query = identityService.createUserQuery().userLastName("Bear");
    verifyQueryResults(query, 1);

    User result = query.singleResult();
    assertThat(result.getId()).isEqualTo("fozzie");
  }

  @Test
  void testQueryByInvalidLastName() {
    UserQuery query = identityService.createUserQuery().userLastName("invalid");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery().userLastName(null);

    try {
      userQuery.singleResult();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByLastNameLike() {
    UserQuery query = identityService.createUserQuery().userLastNameLike("%\\_frog%");
    verifyQueryResults(query, 1);

    query = identityService.createUserQuery().userLastNameLike("%ea%");
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByInvalidLastNameLike() {
    UserQuery query = identityService.createUserQuery().userLastNameLike("%invalid%");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery();

    try {
      userQuery.userLastNameLike(null);
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByEmail() {
    UserQuery query = identityService.createUserQuery().userEmail("kermit_@muppetshow.com");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidEmail() {
    UserQuery query = identityService.createUserQuery().userEmail("invalid");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery().userEmail(null);

    try {
      userQuery.singleResult();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByEmailLike() {
    UserQuery query = identityService.createUserQuery().userEmailLike("%muppetshow.com");
    verifyQueryResults(query, 3);

    query = identityService.createUserQuery().userEmailLike("%kermit\\_%");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidEmailLike() {
    UserQuery query = identityService.createUserQuery().userEmailLike("%invalid%");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery();

    try {
      userQuery.userEmailLike(null);
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQuerySorting() {
    // asc
    assertThat(identityService.createUserQuery().orderByUserId().asc().count()).isEqualTo(3);
    assertThat(identityService.createUserQuery().orderByUserEmail().asc().count()).isEqualTo(3);
    assertThat(identityService.createUserQuery().orderByUserFirstName().asc().count()).isEqualTo(3);
    assertThat(identityService.createUserQuery().orderByUserLastName().asc().count()).isEqualTo(3);

    // desc
    assertThat(identityService.createUserQuery().orderByUserId().desc().count()).isEqualTo(3);
    assertThat(identityService.createUserQuery().orderByUserEmail().desc().count()).isEqualTo(3);
    assertThat(identityService.createUserQuery().orderByUserFirstName().desc().count()).isEqualTo(3);
    assertThat(identityService.createUserQuery().orderByUserLastName().desc().count()).isEqualTo(3);

    // Combined with criteria
    UserQuery query = identityService.createUserQuery().userLastNameLike("%ea%").orderByUserFirstName().asc();
    List<User> users = query.list();
    assertThat(users).hasSize(2);
    assertThat(users.get(0).getFirstName()).isEqualTo("Fozzie");
    assertThat(users.get(1).getFirstName()).isEqualTo("Gonzo");
  }

  @Test
  void testQueryInvalidSortingUsage() {
    var userQuery1 = identityService.createUserQuery().orderByUserId();
    try {
      userQuery1.list();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }

    var userQuery2 = identityService.createUserQuery().orderByUserId().orderByUserEmail();
    try {
      userQuery2.list();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByMemberOfGroup() {
    UserQuery query = identityService.createUserQuery().memberOfGroup("muppets");
    verifyQueryResults(query, 3);

    query = identityService.createUserQuery().memberOfGroup("frogs");
    verifyQueryResults(query, 1);

    User result = query.singleResult();
    assertThat(result.getId()).isEqualTo("kermit");
  }

  @Test
  void testQueryByInvalidMemberOfGroup() {
    UserQuery query = identityService.createUserQuery().memberOfGroup("invalid");
    verifyQueryResults(query, 0);
    var userQuery = identityService.createUserQuery();

    try {
      userQuery.memberOfGroup(null);
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByMemberOfTenant() {
    UserQuery query = identityService.createUserQuery().memberOfTenant("nonExisting");
    verifyQueryResults(query, 0);

    query = identityService.createUserQuery().memberOfTenant("tenant");
    verifyQueryResults(query, 1);

    User result = query.singleResult();
    assertThat(result.getId()).isEqualTo("kermit");
  }

  @Test
  void testQueryByIdIn() {

    // empty list
    assertThat(identityService.createUserQuery().userIdIn("a", "b").list()).isEmpty();


    // collect all ids
    List<User> list = identityService.createUserQuery().list();
    String[] ids = new String[list.size()];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = list.get(i).getId();
    }

    List<User> idInList = identityService.createUserQuery().userIdIn(ids).list();
    for (User user : idInList) {
      boolean found = false;
      for (User otherUser : list) {
        if(otherUser.getId().equals(user.getId())) {
          found = true; break;
        }
      }
      if(!found) {
        fail("Expected to find user " + user);
      }
    }
  }

  @Test
  void testNativeQuery() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    // just test that the query will be constructed and executed, details are tested in the TaskQueryTest
    assertThat(managementService.getTableName(UserEntity.class)).isEqualTo(tablePrefix + "ACT_ID_USER");

    long userCount = identityService.createUserQuery().count();

    assertThat(identityService.createNativeUserQuery().sql("SELECT * FROM " + managementService.getTableName(UserEntity.class)).list()).hasSize(Long.valueOf(userCount).intValue());
    assertThat(identityService.createNativeUserQuery().sql("SELECT count(*) FROM " + managementService.getTableName(UserEntity.class)).count()).isEqualTo(userCount);
  }

  @Test
  void testNativeQueryOrLike() {
    String searchPattern = "%frog";

    String fromWhereClauses = String.format("FROM %s WHERE FIRST_ LIKE #{searchPattern} OR LAST_ LIKE #{searchPattern} OR EMAIL_ LIKE #{searchPattern}",
        managementService.getTableName(UserEntity.class));

    assertThat(identityService.createNativeUserQuery().sql("SELECT * " + fromWhereClauses).parameter("searchPattern", searchPattern).list()).hasSize(1);
    assertThat(identityService.createNativeUserQuery().sql("SELECT count(*) " + fromWhereClauses).parameter("searchPattern", searchPattern).count()).isEqualTo(1);
  }

  @Test
  void testNativeQueryPaging() {
    assertThat(identityService.createNativeUserQuery().sql("SELECT * FROM " + managementService.getTableName(UserEntity.class)).listPage(1, 2)).hasSize(2);
    assertThat(identityService.createNativeUserQuery().sql("SELECT * FROM " + managementService.getTableName(UserEntity.class)).listPage(2, 1)).hasSize(1);
  }

}
