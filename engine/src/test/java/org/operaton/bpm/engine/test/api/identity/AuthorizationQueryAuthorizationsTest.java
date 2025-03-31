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
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_REVOKE;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.ProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

@ExtendWith(ProcessEngineExtension.class)
public class AuthorizationQueryAuthorizationsTest {

  protected AuthorizationService authorizationService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @AfterEach
  public void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    cleanupAfterTest();
  }

  @Test
  public void testQuerySingleCorrectPermission() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setResource(Resources.PROCESS_DEFINITION);
    authorization.addPermission(Permissions.READ);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // assume
    Authorization authResult = authorizationService.createAuthorizationQuery().userIdIn("userId").resourceType(Resources.PROCESS_DEFINITION).singleResult();
    assertThat(authResult).isNotNull();

    // then
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(Permissions.READ).count()).isEqualTo(1);
  }

  @Test
  public void testQuerySingleIncorrectPermission() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setResource(Resources.BATCH);
    authorization.addPermission(BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // assume
    Authorization authResult = authorizationService.createAuthorizationQuery().userIdIn("userId").resourceType(Resources.BATCH).singleResult();
    assertThat(authResult).isNotNull();

    // then
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(Permissions.CREATE_INSTANCE).count()).isZero();
  }

  @Test
  public void testQueryPermissionsWithWrongResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setResource(Resources.APPLICATION);
    authorization.addPermission(Permissions.ACCESS);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // assume
    Authorization authResult = authorizationService.createAuthorizationQuery().userIdIn("userId").resourceType(Resources.APPLICATION).singleResult();
    assertThat(authResult).isNotNull();

    // when
    Authorization accessResult = authorizationService.createAuthorizationQuery()
        .hasPermission(Permissions.ACCESS)
        .singleResult();
    List<Authorization> retryJobPDResult = authorizationService.createAuthorizationQuery()
        .hasPermission(ProcessDefinitionPermissions.RETRY_JOB)
        .list();
    List<Authorization> retryJobPIResult = authorizationService.createAuthorizationQuery()
        .hasPermission(ProcessInstancePermissions.RETRY_JOB)
        .list();

    // then
    assertThat(accessResult).isNotNull();
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(Permissions.ACCESS).count()).isEqualTo(1);
    assertThat(retryJobPDResult).isEmpty();
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(ProcessDefinitionPermissions.RETRY_JOB).count()).isZero();
    assertThat(retryJobPIResult).isEmpty();
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(ProcessInstancePermissions.RETRY_JOB).count()).isZero();
  }

  @Test
  public void testQueryPermissionWithMixedResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setResource(Resources.APPLICATION);
    authorization.addPermission(Permissions.ACCESS);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // assume
    Authorization authResult = authorizationService.createAuthorizationQuery().userIdIn("userId").resourceType(Resources.APPLICATION).singleResult();
    assertThat(authResult).isNotNull();

    // then
    assertThat(authorizationService.createAuthorizationQuery()
        .resourceType(Resources.BATCH)
        .hasPermission(Permissions.ACCESS)
        .count()).isZero();
  }

  @Test
  public void testQueryPermissionsWithMixedResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setResource(Resources.PROCESS_DEFINITION);
    authorization.addPermission(Permissions.READ);
    authorization.addPermission(ProcessDefinitionPermissions.RETRY_JOB);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // assume
    Authorization authResult = authorizationService.createAuthorizationQuery().userIdIn("userId").resourceType(Resources.PROCESS_DEFINITION).singleResult();
    assertThat(authResult).isNotNull();
    assertThat(authorizationService.createAuthorizationQuery()
        .resourceType(Resources.PROCESS_DEFINITION)
        .hasPermission(ProcessDefinitionPermissions.READ)
        .hasPermission(ProcessDefinitionPermissions.RETRY_JOB)
        .count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery()
        .resourceType(Resources.PROCESS_DEFINITION)
        .hasPermission(ProcessDefinitionPermissions.READ)
        .count()).isEqualTo(1);

    // then
    assertThat(authorizationService.createAuthorizationQuery()
        .resourceType(Resources.PROCESS_DEFINITION)
        .hasPermission(Permissions.READ)
        .hasPermission(Permissions.ACCESS)
        .count()).isZero();
  }

  @Test
  public void testQueryCorrectAndIncorrectPermission() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setResource(Resources.PROCESS_DEFINITION);
    authorization.addPermission(Permissions.READ);
    authorization.addPermission(ProcessDefinitionPermissions.RETRY_JOB);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // assume
    Authorization authResult = authorizationService.createAuthorizationQuery().userIdIn("userId").resourceType(Resources.PROCESS_DEFINITION).singleResult();
    assertThat(authResult).isNotNull();

    // then
    assertThat(authorizationService.createAuthorizationQuery()
        .hasPermission(Permissions.READ)
        .hasPermission(Permissions.ACCESS)
        .count()).isZero();
  }

  @Test
  public void shouldNotFindAllAuthorizationsWithRevokedReadPermissionOnOneAuthorization() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setResource(Resources.PROCESS_DEFINITION);
    authorization.addPermission(Permissions.READ);
    authorization.addPermission(ProcessDefinitionPermissions.RETRY_JOB);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("*");
    authorization.setResource(Resources.AUTHORIZATION);
    authorization.addPermission(Permissions.READ);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    authorization.setUserId("userId");
    authorization.setResource(Resources.AUTHORIZATION);
    authorization.addPermission(Permissions.READ);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    processEngineConfiguration.getIdentityService().setAuthenticatedUserId("userId");

    AuthorizationQuery authQuery = authorizationService.createAuthorizationQuery();

    // when
    long authorizationsCount = authQuery.count();
    List<Authorization> authorizations = authQuery.list();

    // then
    assertThat(authorizationsCount).isZero();
    assertThat(authorizations).isEmpty();
  }

  protected void cleanupAfterTest() {
    processEngineConfiguration.getIdentityService().clearAuthentication();
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }
}
