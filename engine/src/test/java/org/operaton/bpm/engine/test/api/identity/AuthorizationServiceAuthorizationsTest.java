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

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GLOBAL;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_REVOKE;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.AUTHORIZATION;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestUtil.assertExceptionInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.MissingAuthorization;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.ProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Test;

/**
 * <p>Ensures authorizations are properly
 * enforced by the {@link AuthorizationService}</p>
 *
 * @author Daniel Meyer
 *
 */
public class AuthorizationServiceAuthorizationsTest extends PluggableProcessEngineTest {

  private static final String JONNY_2 = "jonny2";

  @After
  public void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    cleanupAfterTest();

  }

  @Test
  public void testCreateAuthorization() {

    // add base permission which allows nobody to create authorizations
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(AUTHORIZATION);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL); // add all then remove 'create'
    basePerms.removePermission(CREATE);
    authorizationService.saveAuthorization(basePerms);

    // now enable authorizations:
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(JONNY_2);

    try {
      // we cannot create another authorization
      authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
      fail("exception expected");

    } catch (AuthorizationException e) {
      assertThat(e.getMissingAuthorizations().size()).isEqualTo(1);
      MissingAuthorization info = e.getMissingAuthorizations().get(0);
      assertThat(e.getUserId()).isEqualTo(JONNY_2);
      assertExceptionInfo(CREATE.getName(), AUTHORIZATION.resourceName(), null, info);
    }

    // circumvent auth check to get new transient object
    Authorization authorization = new AuthorizationEntity(AUTH_TYPE_REVOKE);
    authorization.setUserId("someUserId");
    authorization.setResource(Resources.APPLICATION);

    try {
      authorizationService.saveAuthorization(authorization);
      fail("exception expected");

    } catch (AuthorizationException e) {
      assertThat(e.getMissingAuthorizations().size()).isEqualTo(1);
      MissingAuthorization info = e.getMissingAuthorizations().get(0);
      assertThat(e.getUserId()).isEqualTo(JONNY_2);
      assertExceptionInfo(CREATE.getName(), AUTHORIZATION.resourceName(), null, info);
    }
  }

  @Test
  public void testDeleteAuthorization() {

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(AUTHORIZATION);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(DELETE); // revoke delete
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(JONNY_2);
    var basePermsId = basePerms.getId();

    try {
      // try to delete authorization
      authorizationService.deleteAuthorization(basePermsId);
      fail("exception expected");

    } catch (AuthorizationException e) {
      assertThat(e.getMissingAuthorizations().size()).isEqualTo(1);
      MissingAuthorization info = e.getMissingAuthorizations().get(0);
      assertThat(e.getUserId()).isEqualTo(JONNY_2);
      assertExceptionInfo(DELETE.getName(), AUTHORIZATION.resourceName(), basePerms.getId(), info);
    }
  }

  @Test
  public void testUserUpdateAuthorizations() {

    // create global auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(AUTHORIZATION);
    basePerms.setResourceId(ANY);
    basePerms.addPermission(ALL);
    basePerms.removePermission(UPDATE); // revoke update
    authorizationService.saveAuthorization(basePerms);

    // turn on authorization
    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(JONNY_2);

    // fetch authhorization
    basePerms = authorizationService.createAuthorizationQuery().singleResult();
    // make some change to the perms
    basePerms.addPermission(ALL);

    try {
      authorizationService.saveAuthorization(basePerms);
      fail("exception expected");

    } catch (AuthorizationException e) {
      assertThat(e.getMissingAuthorizations().size()).isEqualTo(1);
      MissingAuthorization info = e.getMissingAuthorizations().get(0);
      assertThat(e.getUserId()).isEqualTo(JONNY_2);
      assertExceptionInfo(UPDATE.getName(), AUTHORIZATION.resourceName(), basePerms.getId(), info);
    }

    // but we can create a new auth
    Authorization newAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    newAuth.setUserId("jonny2");
    newAuth.setResource(AUTHORIZATION);
    newAuth.setResourceId(ANY);
    newAuth.addPermission(ALL);
    authorizationService.saveAuthorization(newAuth);

  }

  @Test
  public void testAuthorizationQueryAuthorizations() {

    // we are jonny2
    String authUserId = "jonny2";
    identityService.setAuthenticatedUserId(authUserId);

    // create new auth wich revokes read access on auth
    Authorization basePerms = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    basePerms.setResource(AUTHORIZATION);
    basePerms.setResourceId(ANY);
    authorizationService.saveAuthorization(basePerms);

    // I can see it
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(1);

    // now enable checks
    processEngineConfiguration.setAuthorizationEnabled(true);

    // I can't see it
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(0);

  }

  @Test
  public void testSaveAuthorizationAddPermissionWithInvalidResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.addPermission(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES);
    authorization.setResource(Resources.APPLICATION);
    authorization.setResourceId(ANY);

    processEngineConfiguration.setAuthorizationEnabled(true);

    try {
      // when
      authorizationService.saveAuthorization(authorization);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertTrue(e.getMessage().contains("The resource type with id:'0' is not valid for 'CREATE_BATCH_MIGRATE_PROCESS_INSTANCES' permission."));
    }

    // given
    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.addPermission(Permissions.ACCESS);
    authorization.setResource(Resources.BATCH);

    try {
      // when
      authorizationService.saveAuthorization(authorization);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertTrue(e.getMessage().contains("The resource type with id:'13' is not valid for 'ACCESS' permission."));
    }
  }

  @Test
  public void testSaveAuthorizationMultipleResourcesIncludingInvalidResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.addPermission(Permissions.READ_HISTORY);
    authorization.addPermission(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES);
    authorization.setResource(Resources.PROCESS_DEFINITION);

    processEngineConfiguration.setAuthorizationEnabled(true);

    try {
      // when
      authorizationService.saveAuthorization(authorization);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertTrue(e.getMessage().contains("The resource type with id:'6' is not valid for 'CREATE_BATCH_MIGRATE_PROCESS_INSTANCES' permission."));
    }
  }

  @Test
  public void testSaveAuthorizationRemovePermissionWithInvalidResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    authorization.setUserId("userId");
    authorization.removePermission(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES);
    authorization.setResource(Resources.PROCESS_DEFINITION);
    authorization.setResourceId(ANY);

    processEngineConfiguration.setAuthorizationEnabled(true);

    try {
      // when
      authorizationService.saveAuthorization(authorization);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertTrue(e.getMessage().contains("The resource type with id:'6' is not valid for 'CREATE_BATCH_MIGRATE_PROCESS_INSTANCES' permission."));
    }

    // given
    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    authorization.setUserId("userId");
    authorization.addPermission(Permissions.ACCESS);
    authorization.setResource(Resources.PROCESS_DEFINITION);

    try {
      // when
      authorizationService.saveAuthorization(authorization);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertTrue(e.getMessage().contains("The resource type with id:'6' is not valid for 'ACCESS' permission."));
    }
  }

  @Test
  public void testSaveAuthorizationSetPermissionsWithInvalidResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setPermissions(new BatchPermissions[] { BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES });
    authorization.setResource(Resources.PROCESS_INSTANCE);
    authorization.setResourceId(ANY);

    processEngineConfiguration.setAuthorizationEnabled(true);

    try {
      // when
      authorizationService.saveAuthorization(authorization);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertTrue(e.getMessage().contains("The resource type with id:'8' is not valid for 'CREATE_BATCH_MIGRATE_PROCESS_INSTANCES' permission."));
    }

    // given
    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.setPermissions(new Permissions[] { Permissions.CREATE, Permissions.ACCESS });
    authorization.setResource(Resources.PROCESS_INSTANCE);

    try {
      // when
      authorizationService.saveAuthorization(authorization);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertTrue(e.getMessage().contains("The resource type with id:'8' is not valid for 'ACCESS' permission."));
    }
  }

  @Test
  public void testSaveAuthorizationSetPermissionsWithValidResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("userId");
    authorization.addPermission(Permissions.ACCESS);
    // 'ACCESS' is not allowed for Batches
    // however, it will be reset by next line, so saveAuthorization will be successful
    authorization.setPermissions(
        new BatchPermissions[] { BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES, BatchPermissions.CREATE_BATCH_DELETE_DECISION_INSTANCES });
    authorization.setResource(Resources.BATCH);
    authorization.setResourceId(ANY);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // when
    authorizationService.saveAuthorization(authorization);

    // then
    Authorization authorizationResult = authorizationService.createAuthorizationQuery().resourceType(Resources.BATCH).singleResult();
    assertNotNull(authorizationResult);
    assertTrue(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES));
    assertTrue(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_DELETE_DECISION_INSTANCES));
  }

  @Test
  public void testIsUserAuthorizedWithInvalidResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.addPermission(Permissions.ACCESS);
    authorization.setResource(Resources.APPLICATION);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null, Permissions.ACCESS, Resources.APPLICATION)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(userId, null, BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES, Resources.BATCH)).isEqualTo(false);
    assertThat(authorizationService.isUserAuthorized(userId, null, ProcessDefinitionPermissions.RETRY_JOB, Resources.PROCESS_DEFINITION)).isEqualTo(false);
    assertThat(authorizationService.isUserAuthorized(userId, null, ProcessInstancePermissions.RETRY_JOB, Resources.PROCESS_INSTANCE)).isEqualTo(false);
    try {
      authorizationService.isUserAuthorized(userId, null, BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES, Resources.APPLICATION);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertTrue(e.getMessage().contains("The resource type 'Application' is not valid"));
      assertTrue(e.getMessage().contains(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES.getName()));
    }
    try {
      authorizationService.isUserAuthorized(userId, null, ProcessDefinitionPermissions.RETRY_JOB, Resources.APPLICATION);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertTrue(e.getMessage().contains("The resource type 'Application' is not valid"));
      assertTrue(e.getMessage().contains(ProcessDefinitionPermissions.RETRY_JOB.getName()));
    }
    try {
      authorizationService.isUserAuthorized(userId, null, ProcessInstancePermissions.RETRY_JOB, Resources.APPLICATION);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertTrue(e.getMessage().contains("The resource type 'Application' is not valid"));
      assertTrue(e.getMessage().contains(ProcessInstancePermissions.RETRY_JOB.getName()));
    }

  }

  @Test
  public void testIsUserAuthorizedWithInvalidResourceMultiplePermissions() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.addPermission(ProcessInstancePermissions.READ);
    authorization.addPermission(ProcessInstancePermissions.RETRY_JOB);
    authorization.setResource(Resources.PROCESS_INSTANCE);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null, Permissions.READ, Resources.PROCESS_INSTANCE)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(userId, null, ProcessInstancePermissions.RETRY_JOB, Resources.PROCESS_INSTANCE)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(userId, null, BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES, Resources.BATCH)).isEqualTo(false);
    assertThat(authorizationService.isUserAuthorized(userId, null, ProcessDefinitionPermissions.RETRY_JOB, Resources.PROCESS_DEFINITION)).isEqualTo(false);
    assertThat(authorizationService.isUserAuthorized(userId, null, Permissions.ACCESS, Resources.APPLICATION)).isEqualTo(false);
    try {
      authorizationService.isUserAuthorized(userId, null, ProcessDefinitionPermissions.RETRY_JOB, Resources.PROCESS_INSTANCE);
      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertTrue(e.getMessage().contains("The resource type 'ProcessInstance' is not valid"));
      assertTrue(e.getMessage().contains(ProcessDefinitionPermissions.RETRY_JOB.getName()));
    }
  }

  @Test
  public void testIsUserAuthorizedWithValidResourceImpl() {
    // given
    ResourceImpl resource = new ResourceImpl("application", 0);
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.addPermission(Permissions.ACCESS);
    authorization.setResource(Resources.APPLICATION);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);

    // then
    assertThat(authorizationService.isUserAuthorized(userId, null, Permissions.ACCESS, resource)).isEqualTo(true);
  }

  protected void cleanupAfterTest() {
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  class ResourceImpl implements Resource {

    String resourceName;
    int resourceType;

    public ResourceImpl(String resourceName, int resourceType) {
      this.resourceName = resourceName;
      this.resourceType = resourceType;
    }

    @Override
    public String resourceName() {
      return resourceName;
    }

    @Override
    public int resourceType() {
      return resourceType;
    }

  }

}
