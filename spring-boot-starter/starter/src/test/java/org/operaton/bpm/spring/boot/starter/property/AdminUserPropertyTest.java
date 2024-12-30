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
package org.operaton.bpm.spring.boot.starter.property;

import org.operaton.bpm.engine.identity.User;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
class AdminUserPropertyTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  void fillMissingFields_fail_no_id() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("missing field: operaton.bpm.admin-user.id");

    adminUser(null, null, null, null, null).init();
  }

  @Test
  void fillMissingFields_null() {
    User adminUser = adminUser("admin", "foo", null, null, null).init();

    assertThat(adminUser.getId()).isEqualTo("admin");
    assertThat(adminUser.getPassword()).isEqualTo("foo");
    assertThat(adminUser.getFirstName()).isEqualTo("Admin");
    assertThat(adminUser.getLastName()).isEqualTo("Admin");
    assertThat(adminUser.getEmail()).isEqualTo("admin@localhost");
  }

  @Test
  void fillMissingFields_blank() {
    User adminUser = adminUser("admin", "foo", "", "", "").init();

    assertThat(adminUser.getId()).isEqualTo("admin");
    assertThat(adminUser.getPassword()).isEqualTo("foo");
    assertThat(adminUser.getFirstName()).isEqualTo("Admin");
    assertThat(adminUser.getLastName()).isEqualTo("Admin");
    assertThat(adminUser.getEmail()).isEqualTo("admin@localhost");
  }

  @Test
  void paswordDefaultsToUserId() {
    User adminUser = adminUser("admin", null, "", "", "").init();

    assertThat(adminUser.getId()).isEqualTo("admin");
    assertThat(adminUser.getPassword()).isEqualTo("admin");
  }

  private AdminUserProperty adminUser(String id, String password, String first, String last, String mail) {
    final AdminUserProperty adminUser = new AdminUserProperty();
    adminUser.setId(id);
    adminUser.setEmail(mail);
    adminUser.setFirstName(first);
    adminUser.setLastName(last);
    adminUser.setPassword(password);

    return adminUser;
  }

}
