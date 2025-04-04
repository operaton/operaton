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
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.AUTHORIZATION;
import static org.operaton.bpm.engine.authorization.Resources.GROUP;
import static org.operaton.bpm.engine.authorization.Resources.USER;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultAuthorizationProvider;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * <p>Test authorizations provided by {@link DefaultAuthorizationProvider}</p>
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class DefaultAuthorizationProviderTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;

  @BeforeEach
  void setUp() {
    // we are jonny
    identityService.setAuthenticatedUserId("jonny");
    // make sure we can do stuff:
    Authorization jonnyIsGod = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    jonnyIsGod.setUserId("jonny");
    jonnyIsGod.setResource(USER);
    jonnyIsGod.setResourceId(ANY);
    jonnyIsGod.addPermission(ALL);
    authorizationService.saveAuthorization(jonnyIsGod);

    jonnyIsGod = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    jonnyIsGod.setUserId("jonny");
    jonnyIsGod.setResource(GROUP);
    jonnyIsGod.setResourceId(ANY);
    jonnyIsGod.addPermission(ALL);
    authorizationService.saveAuthorization(jonnyIsGod);

    jonnyIsGod = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    jonnyIsGod.setUserId("jonny");
    jonnyIsGod.setResource(AUTHORIZATION);
    jonnyIsGod.setResourceId(ANY);
    jonnyIsGod.addPermission(ALL);
    authorizationService.saveAuthorization(jonnyIsGod);

    // enable authorizations
    processEngineConfiguration.setAuthorizationEnabled(true);

  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    List<Authorization> jonnysAuths = authorizationService.createAuthorizationQuery().userIdIn("jonny").list();
    for (Authorization authorization : jonnysAuths) {
      authorizationService.deleteAuthorization(authorization.getId());
    }

  }

  @Test
  void testCreateUser() {
    // initially there are no authorizations for jonny2:
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("jonny2").count()).isZero();

    // create new user
    identityService.saveUser(identityService.newUser("jonny2"));

    // now there is an authorization for jonny2 which grants him ALL permissions on himself
    Authorization authorization = authorizationService.createAuthorizationQuery().userIdIn("jonny2").singleResult();
    assertThat(authorization).isNotNull();
    assertThat(authorization.getAuthorizationType()).isEqualTo(AUTH_TYPE_GRANT);
    assertThat(authorization.getResourceType()).isEqualTo(USER.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo("jonny2");
    assertThat(authorization.isPermissionGranted(ALL)).isTrue();

    // delete the user
    identityService.deleteUser("jonny2");

    // the authorization is deleted as well:
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("jonny2").count()).isZero();
  }

  @Test
  void testCreateGroup() {
    // initially there are no authorizations for group "sales":
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("sales").count()).isZero();

    // create new group
    identityService.saveGroup(identityService.newGroup("sales"));

    // now there is an authorization for sales which grants all members READ permissions
    Authorization authorization = authorizationService.createAuthorizationQuery().groupIdIn("sales").singleResult();
    assertThat(authorization).isNotNull();
    assertThat(authorization.getAuthorizationType()).isEqualTo(AUTH_TYPE_GRANT);
    assertThat(authorization.getResourceType()).isEqualTo(GROUP.resourceType());
    assertThat(authorization.getResourceId()).isEqualTo("sales");
    assertThat(authorization.isPermissionGranted(READ)).isTrue();

    // delete the group
    identityService.deleteGroup("sales");

    // the authorization is deleted as well:
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("sales").count()).isZero();
  }

}
