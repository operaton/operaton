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
package org.operaton.bpm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.USER;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineTestExtension.class)
class AdminUsersTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;
  protected ManagementService managementService;

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    cleanupAfterTest();
  }

  protected void cleanupAfterTest() {
    processEngineConfiguration.getAdminUsers().clear();
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  void testWithoutAdminUser() {
    processEngineConfiguration.setAuthorizationEnabled(false);

    identityService.setAuthentication("adminUser", null, null);
    Authorization userAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    userAuth.setUserId("adminUser");
    userAuth.setResource(USER);
    userAuth.setResourceId(ANY);
    userAuth.addPermission(READ);
    authorizationService.saveAuthorization(userAuth);
    processEngineConfiguration.setAuthorizationEnabled(true);


    // when/then
    assertThatThrownBy(() -> managementService.getProperties())
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("ENGINE-03110 Required admin authenticated group or user or any of the following permissions:");

  }

  @Test
  void testWithAdminUser() {
    processEngineConfiguration.getAdminUsers().add("adminUser");

    processEngineConfiguration.setAuthorizationEnabled(false);

    identityService.setAuthentication("adminUser", null, null);
    Authorization userAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    userAuth.setUserId("adminUser");
    userAuth.setResource(USER);
    userAuth.setResourceId(ANY);
    userAuth.addPermission(READ);
    authorizationService.saveAuthorization(userAuth);
    processEngineConfiguration.setAuthorizationEnabled(true);

    // when/then no exception
    assertThat(managementService.getProperties()).isNotNull();
  }
}
