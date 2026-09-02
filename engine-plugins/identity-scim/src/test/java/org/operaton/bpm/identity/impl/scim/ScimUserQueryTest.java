/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.scim.util.ScimTestEnvironment;
import org.operaton.bpm.identity.scim.util.ScimTestEnvironmentExtension;

/**
 * Tests for SCIM user queries.
 */
class ScimUserQueryTest {

  @RegisterExtension
  @Order(1)
  static ScimTestEnvironmentExtension scimExtension = new ScimTestEnvironmentExtension();

  @RegisterExtension
  @Order(2)
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .configurationResource("operaton.cfg.xml")
      .configurator(scimExtension::injectScimUrlIntoProcessEngineConfiguration)
      .closeEngineAfterAllTests()
      .build();

  IdentityService identityService;
  ScimTestEnvironment scimTestEnvironment;

  @BeforeEach
  void setup() {
    scimTestEnvironment = scimExtension.getScimTestEnvironment();
  }

  @Test
  public void testCountUsers() {
    // when
    UserQuery userQuery = identityService.createUserQuery();

    // then
    long count = userQuery.count();
    assertThat(count).isEqualTo(scimTestEnvironment.getTotalNumberOfUsersCreated());
  }

  @Test
  public void testQueryNoFilter() {
    // when
    List<User> result = identityService.createUserQuery().list();

    // then
    assertThat(result).hasSize(scimTestEnvironment.getTotalNumberOfUsersCreated());
  }

  @Test
  public void testFilterByUserId() {
    // when
    User user = identityService.createUserQuery().userId("oscar").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo("oscar");
    assertThat(user.getFirstName()).isEqualTo("Oscar");
    assertThat(user.getLastName()).isEqualTo("The Crouch");
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  public void testFilterByNonexistentUserId() {
    // when
    User user = identityService.createUserQuery().userId("non-existing").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  public void testFilterByUserIdIn() {
    // when
    List<User> users = identityService.createUserQuery().userIdIn("oscar", "monster").list();

    // then
    assertThat(users).hasSize(2);
    assertThat(users).extracting("id").containsOnly("oscar", "monster");
  }

  @Test
  public void testFilterByUserFirstName() {
    // when
    List<User> users = identityService.createUserQuery().userFirstName("Oscar").list();

    // then
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getId()).isEqualTo("oscar");
    assertThat(users.get(0).getFirstName()).isEqualTo("Oscar");
  }

  @Test
  public void testFilterByUserLastName() {
    // when
    List<User> users = identityService.createUserQuery().userLastName("Monster").list();

    // then
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getId()).isEqualTo("monster");
    assertThat(users.get(0).getLastName()).isEqualTo("Monster");
  }

  @Test
  public void testFilterByUserEmail() {
    // when
    List<User> users = identityService.createUserQuery().userEmail("oscar@operaton.org").list();

    // then
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getId()).isEqualTo("oscar");
    assertThat(users.get(0).getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  public void testPagination() {
    // when - get first 2 users
    List<User> firstPage = identityService.createUserQuery()
        .listPage(0, 2);

    // then
    assertThat(firstPage).hasSize(2);

    // when - get next page
    List<User> secondPage = identityService.createUserQuery()
        .listPage(2, 2);

    // then
    assertThat(secondPage).hasSize(1);
  }

  @Test
  public void testFindUserById() {
    // when
    User user = identityService.createUserQuery().userId("oscar").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo("oscar");
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }
}
