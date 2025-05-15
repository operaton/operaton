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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.authorization.OptimizePermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

class OptimizeAuthorizationTest {

  protected static final String USER_ID = "user";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  AuthorizationService authorizationService;

  @BeforeEach
  void setUp() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    authorizationService = engineRule.getAuthorizationService();
  }

  @Test
  void testOptimizePermissionExists() {
    // given
    authRule.createGrantAuthorization(Resources.OPTIMIZE, ANY, USER_ID, OptimizePermissions.ALL);

    // when
    authRule.enableAuthorization(USER_ID);

    // then
    assertThat(authorizationService.isUserAuthorized(USER_ID, null, OptimizePermissions.EDIT, Resources.OPTIMIZE)).isTrue();
    assertThat(authorizationService.isUserAuthorized(USER_ID, null, OptimizePermissions.SHARE, Resources.OPTIMIZE)).isTrue();
  }

  @AfterEach
  void tearDown() {
    authRule.disableAuthorization();
    authRule.deleteUsersAndGroups();
  }
}
