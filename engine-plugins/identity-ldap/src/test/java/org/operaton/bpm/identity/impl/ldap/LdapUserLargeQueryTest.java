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

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.ldap.util.LdapTestExtension;

class LdapUserLargeQueryTest {

  @RegisterExtension
  @Order(1)
  static LdapTestExtension ldapExtension = new LdapTestExtension()
          .withAdditionalNumberOfGroups(5)
          .withAdditionalNumberOfRoles(5)
          .withAdditionalNumberOfUsers(80); // Attention, stay under 80, there is a limitation in the query on 100

  @RegisterExtension
  @Order(2)
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .configurationResource("operaton.ldap.pages.cfg.xml") // pageSize = 3 in this configuration
    .configurator(ldapExtension::injectLdapUrlIntoProcessEngineConfiguration)
    .closeEngineAfterAllTests()
    .build();

  IdentityService identityService;

  @Test
  void testAllUsersQuery() {
    List<User> listUsers = identityService.createUserQuery().list();

    // In this group, we expect more than a page size
    assertThat(listUsers).hasSize(ldapExtension.getLdapTestContext().numberOfGeneratedUsers());
  }

  @Test
  void testPagesAllUsersQuery() {
    List<User> listUsers = identityService.createUserQuery().list();

    assertThat(listUsers).hasSize(ldapExtension.getLdapTestContext().numberOfGeneratedUsers());

    // ask 3 pages
    for (int firstResult = 0; firstResult < 10; firstResult += 4) {
      List<User> listPages = identityService.createUserQuery().listPage(firstResult, 5);
      for (int i = 0; i < listPages.size(); i++) {
        assertThat(listPages.get(i).getId()).isEqualTo(listUsers.get(firstResult + i).getId());
        assertThat(listPages.get(i).getLastName()).isEqualTo(listUsers.get(firstResult + i).getLastName());
      }

    }
  }

  @Test
  void testQueryPaging() {
    UserQuery query = identityService.createUserQuery();

    assertThat(query.listPage(0, Integer.MAX_VALUE)).hasSize(92);

    // Verifying the un-paged results
    assertThat(query.count()).isEqualTo(92);
    assertThat(query.list()).hasSize(92);

    // Verifying paged results
    assertThat(query.listPage(0, 2)).hasSize(2);
    assertThat(query.listPage(2, 2)).hasSize(2);
    assertThat(query.listPage(4, 3)).hasSize(3);
    assertThat(query.listPage(91, 3)).hasSize(1);
    assertThat(query.listPage(91, 1)).hasSize(1);

    // Verifying odd usages
    assertThat(query.listPage(-1, -1)).isEmpty();
    assertThat(query.listPage(92, 2)).isEmpty(); // 92 is the last index with a result
    assertThat(query.listPage(0, 93)).hasSize(92); // there are only 92 groups
  }

}
