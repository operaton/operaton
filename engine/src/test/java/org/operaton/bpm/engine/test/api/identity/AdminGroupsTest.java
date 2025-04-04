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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.USER;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineTestExtension.class)
class AdminGroupsTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    cleanupAfterTest();
  }

  protected void cleanupAfterTest() {
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  void testWithoutAdminGroup() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    identityService.newUser("jonny1");

    // no admin group
    identityService.setAuthentication("nonAdmin", null, null);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // when/then
    assertThatThrownBy(() -> identityService.unlockUser("jonny1"))
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("Required admin authenticated group or user.");
  }

  @Test
  void testWithAdminGroup() {
    processEngineConfiguration.getAdminGroups().add("adminGroup");

    processEngineConfiguration.setAuthorizationEnabled(false);

    identityService.setAuthentication("admin", Collections.singletonList("adminGroup"), null);
    Authorization userAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    userAuth.setUserId("admin");
    userAuth.setResource(USER);
    userAuth.setResourceId(ANY);
    userAuth.addPermission(READ);
    authorizationService.saveAuthorization(userAuth);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // when
    assertAll(() -> identityService.unlockUser("jonny1"));
    // then no exception
  }
}
