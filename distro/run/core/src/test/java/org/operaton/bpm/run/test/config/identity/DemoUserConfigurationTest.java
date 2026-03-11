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
package org.operaton.bpm.run.test.config.identity;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin;
import org.operaton.bpm.run.OperatonApp;
import org.operaton.bpm.run.property.OperatonBpmRunLdapProperties;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {OperatonApp.class})
@ActiveProfiles(profiles = {"test-auth-disabled", "test-demo-user"})
class DemoUserConfigurationTest {

  @Autowired
  ProcessEngine engine;
  IdentityService identityService;

  @Autowired(required = false)
  OperatonBpmRunLdapProperties props;

  @Autowired(required = false)
  LdapIdentityProviderPlugin ldapPlugin;

  @BeforeEach
  void init() {
    identityService = engine.getIdentityService();
  }

  @Test
  void shouldFindDemoUser() {
    // given
    UserQuery userQuery = identityService.createUserQuery();

    // when
    long userCount = userQuery.count();
    List<User> userList = userQuery.list();

    // then
    assertThat(userCount).isOne();
    String userId = userList.get(0).getId();
    assertThat(userId).isEqualTo("demo");
    assertThat(identityService.checkPassword(userId, "demo")).isTrue();
  }

  @Test
  void shouldNotEnableLdap() {
    assertThat(props).isNull();
    assertThat(ldapPlugin).isNull();
  }
}
