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

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.authorization.*;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GLOBAL;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_REVOKE;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE;
import static org.operaton.bpm.engine.authorization.Permissions.NONE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.DASHBOARD;
import static org.operaton.bpm.engine.authorization.Resources.REPORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Daniel Meyer
 *
 */
public class AuthorizationServiceTest extends PluggableProcessEngineTest {

  protected String testUserId = "test";
  protected String testGroupId = "accounting";

  @After
  public void tearDown() {
    cleanupAfterTest();

  }

  @Test
  public void testGlobalAuthorizationType() {
    Authorization globalAuthorization = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    // I can set userId = null
    globalAuthorization.setUserId(null);
    // I can set userId = ANY
    globalAuthorization.setUserId(ANY);

    try {
      // I cannot set anything else:
      globalAuthorization.setUserId("something");
      fail("exception expected");

    } catch (Exception e) {
      testRule.assertTextPresent("ENGINE-03028 Illegal value 'something' for userId for GLOBAL authorization. Must be '*'", e.getMessage());

    }

    // I can set groupId = null
    globalAuthorization.setGroupId(null);

    try {
      // I cannot set anything else:
      globalAuthorization.setGroupId("something");
      fail("exception expected");

    } catch (Exception e) {
      testRule.assertTextPresent("ENGINE-03027 Cannot use 'groupId' for GLOBAL authorization", e.getMessage());
    }
  }

  @Test
  public void testGrantAuthorizationType() {
    Authorization grantAuthorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    // I can set userId = null
    grantAuthorization.setUserId(null);
    assertNull(grantAuthorization.getUserId());
    // I can set userId = ANY
    grantAuthorization.setUserId(ANY);
    assertThat(grantAuthorization.getUserId()).isEqualTo(ANY);
    // I can set anything else:
    grantAuthorization.setUserId("something");
    assertThat(grantAuthorization.getUserId()).isEqualTo("something");
    // I can set groupId = null
    grantAuthorization.setGroupId(null);
    assertNull(grantAuthorization.getGroupId());
    // I can set anything else:
    grantAuthorization.setGroupId("something");
    assertThat(grantAuthorization.getGroupId()).isEqualTo("something");
  }

  @Test
  public void testRevokeAuthorizationType() {
    Authorization revokeAuthorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    // I can set userId = null
    revokeAuthorization.setUserId(null);
    assertNull(revokeAuthorization.getUserId());
    // I can set userId = ANY
    revokeAuthorization.setUserId(ANY);
    assertThat(revokeAuthorization.getUserId()).isEqualTo(ANY);
    // I can set anything else:
    revokeAuthorization.setUserId("something");
    assertThat(revokeAuthorization.getUserId()).isEqualTo("something");
    // I can set groupId = null
    revokeAuthorization.setGroupId(null);
    assertNull(revokeAuthorization.getGroupId());
    // I can set anything else:
    revokeAuthorization.setGroupId("something");
    assertThat(revokeAuthorization.getGroupId()).isEqualTo("something");
  }

  @Test
  public void testDeleteNonExistingAuthorization() {

    try {
      authorizationService.deleteAuthorization("nonExisiting");
      fail();
    } catch (Exception e) {
      testRule.assertTextPresent("Authorization for Id 'nonExisiting' does not exist: authorization is null", e.getMessage());
    }

  }

  @Test
  public void testCreateAuthorizationWithUserId() {

    Resource resource1 = TestResource.RESOURCE1;

    // initially, no authorization exists:
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(0);

    // simple create / delete with userId
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("aUserId");
    authorization.setResource(resource1);

    // save the authorization
    authorizationService.saveAuthorization(authorization);
    // authorization exists
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(1);
    // delete the authorization
    authorizationService.deleteAuthorization(authorization.getId());
    // it's gone
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(0);

  }

  @Test
  public void testCreateAuthorizationWithGroupId() {

    Resource resource1 = TestResource.RESOURCE1;

    // initially, no authorization exists:
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(0);

    // simple create / delete with userId
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setGroupId("aGroupId");
    authorization.setResource(resource1);

    // save the authorization
    authorizationService.saveAuthorization(authorization);
    // authorization exists
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(1);
    // delete the authorization
    authorizationService.deleteAuthorization(authorization.getId());
    // it's gone
    assertThat(authorizationService.createAuthorizationQuery().count()).isEqualTo(0);

  }

  @Test
  public void testInvalidCreateAuthorization() {

    Resource resource1 = TestResource.RESOURCE1;

    // case 1: no user id & no group id ////////////

    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setResource(resource1);

    try {
      authorizationService.saveAuthorization(authorization);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      assertTrue(e.getMessage().contains("Authorization must either have a 'userId' or a 'groupId'."));
    }

    // case 2: both user id & group id ////////////

    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setGroupId("someId");
    authorization.setUserId("someOtherId");
    authorization.setResource(resource1);

    try {
      authorizationService.saveAuthorization(authorization);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Authorization must either have a 'userId' or a 'groupId'.", e.getMessage());
    }

    // case 3: no resourceType ////////////

    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("someId");

    try {
      authorizationService.saveAuthorization(authorization);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      assertTrue(e.getMessage().contains("Authorization 'resourceType' cannot be null."));
    }

    // case 4: no permissions /////////////////

    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    authorization.setUserId("someId");

    try {
      authorizationService.saveAuthorization(authorization);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      assertTrue(e.getMessage().contains("Authorization 'resourceType' cannot be null."));
    }
  }

  @Test
  public void testUniqueUserConstraints() {

    Resource resource1 = TestResource.RESOURCE1;

    Authorization authorization1 = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    Authorization authorization2 = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);

    authorization1.setResource(resource1);
    authorization1.setResourceId("someId");
    authorization1.setUserId("someUser");

    authorization2.setResource(resource1);
    authorization2.setResourceId("someId");
    authorization2.setUserId("someUser");

    // the first one can be saved
    authorizationService.saveAuthorization(authorization1);

    // the second one cannot
    try {
      authorizationService.saveAuthorization(authorization2);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      //expected
    }

    // but I can add a AUTH_TYPE_REVOKE auth

    Authorization authorization3 = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);

    authorization3.setResource(resource1);
    authorization3.setResourceId("someId");
    authorization3.setUserId("someUser");

    authorizationService.saveAuthorization(authorization3);

    // but not a second

    Authorization authorization4 = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);

    authorization4.setResource(resource1);
    authorization4.setResourceId("someId");
    authorization4.setUserId("someUser");

    try {
      authorizationService.saveAuthorization(authorization4);
      fail("exception expected");
    } catch(Exception e) {
      //expected
    }
  }

  @Test
  public void testUniqueGroupConstraints() {

    Resource resource1 = TestResource.RESOURCE1;

    Authorization authorization1 = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    Authorization authorization2 = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);

    authorization1.setResource(resource1);
    authorization1.setResourceId("someId");
    authorization1.setGroupId("someGroup");

    authorization2.setResource(resource1);
    authorization2.setResourceId("someId");
    authorization2.setGroupId("someGroup");

    // the first one can be saved
    authorizationService.saveAuthorization(authorization1);

    // the second one cannot
    try {
      authorizationService.saveAuthorization(authorization2);
      fail("exception expected");
    } catch(Exception e) {
      //expected
    }

    // but I can add a AUTH_TYPE_REVOKE auth

    Authorization authorization3 = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);

    authorization3.setResource(resource1);
    authorization3.setResourceId("someId");
    authorization3.setGroupId("someGroup");

    authorizationService.saveAuthorization(authorization3);

    // but not a second

    Authorization authorization4 = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);

    authorization4.setResource(resource1);
    authorization4.setResourceId("someId");
    authorization4.setGroupId("someGroup");

    try {
      authorizationService.saveAuthorization(authorization4);
      fail("exception expected");
    } catch(Exception e) {
      //expected
    }

  }

  @Test
  public void testGlobalUniqueConstraints() {

    Resource resource1 = TestResource.RESOURCE1;

    Authorization authorization1 = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    Authorization authorization2 = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);

    authorization1.setResource(resource1);
    authorization1.setResourceId("someId");

    authorization2.setResource(resource1);
    authorization2.setResourceId("someId");

    // the first one can be saved
    authorizationService.saveAuthorization(authorization1);

    // the second one cannot
    try {
      authorizationService.saveAuthorization(authorization2);
      fail("exception expected");
    } catch(Exception e) {
      //expected
    }
  }

  @Test
  public void testUpdateNewAuthorization() {

    Resource resource1 = TestResource.RESOURCE1;
    Resource resource2 = TestResource.RESOURCE2;

    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("aUserId");
    authorization.setResource(resource1);
    authorization.setResourceId("aResourceId");
    authorization.addPermission(TestPermissions.ACCESS);

    // save the authorization
    authorizationService.saveAuthorization(authorization);

    // validate authorization
    Authorization savedAuthorization = authorizationService.createAuthorizationQuery().singleResult();
    assertThat(savedAuthorization.getUserId()).isEqualTo("aUserId");
    assertThat(savedAuthorization.getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(savedAuthorization.getResourceId()).isEqualTo("aResourceId");
    assertTrue(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS));

    // update authorization
    authorization.setUserId("anotherUserId");
    authorization.setResource(resource2);
    authorization.setResourceId("anotherResourceId");
    authorization.addPermission(TestPermissions.DELETE);
    authorizationService.saveAuthorization(authorization);

    // validate authorization updated
    savedAuthorization = authorizationService.createAuthorizationQuery().singleResult();
    assertThat(savedAuthorization.getUserId()).isEqualTo("anotherUserId");
    assertThat(savedAuthorization.getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(savedAuthorization.getResourceId()).isEqualTo("anotherResourceId");
    assertTrue(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS));
    assertTrue(savedAuthorization.isPermissionGranted(TestPermissions.DELETE));

  }

  @Test
  public void testUpdatePersistentAuthorization() {

    Resource resource1 = TestResource.RESOURCE1;
    Resource resource2 = TestResource.RESOURCE2;
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("aUserId");
    authorization.setResource(resource1);
    authorization.setResourceId("aResourceId");
    authorization.addPermission(TestPermissions.ACCESS);

    // save the authorization
    authorizationService.saveAuthorization(authorization);

    // validate authorization
    Authorization savedAuthorization = authorizationService.createAuthorizationQuery().singleResult();
    assertThat(savedAuthorization.getUserId()).isEqualTo("aUserId");
    assertThat(savedAuthorization.getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(savedAuthorization.getResourceId()).isEqualTo("aResourceId");
    assertTrue(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS));

    // update authorization
    savedAuthorization.setUserId("anotherUserId");
    savedAuthorization.setResource(resource2);
    savedAuthorization.setResourceId("anotherResourceId");
    savedAuthorization.addPermission(TestPermissions.DELETE);
    authorizationService.saveAuthorization(savedAuthorization);

    // validate authorization updated
    savedAuthorization = authorizationService.createAuthorizationQuery().singleResult();
    assertThat(savedAuthorization.getUserId()).isEqualTo("anotherUserId");
    assertThat(savedAuthorization.getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(savedAuthorization.getResourceId()).isEqualTo("anotherResourceId");
    assertTrue(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS));
    assertTrue(savedAuthorization.isPermissionGranted(TestPermissions.DELETE));

  }

  @Test
  public void testPermissions() {

    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setResource(Resources.USER);

    assertThat(authorization.getPermissions(Permissions.values()).length).isEqualTo(1);

    assertFalse(authorization.isPermissionGranted(CREATE));
    assertFalse(authorization.isPermissionGranted(DELETE));
    assertFalse(authorization.isPermissionGranted(READ));
    assertFalse(authorization.isPermissionGranted(UPDATE));

    authorization.addPermission(CREATE);
    assertTrue(authorization.isPermissionGranted(CREATE));
    assertFalse(authorization.isPermissionGranted(DELETE));
    assertFalse(authorization.isPermissionGranted(READ));
    assertFalse(authorization.isPermissionGranted(UPDATE));

    authorization.addPermission(DELETE);
    assertTrue(authorization.isPermissionGranted(CREATE));
    assertTrue(authorization.isPermissionGranted(DELETE));
    assertFalse(authorization.isPermissionGranted(READ));
    assertFalse(authorization.isPermissionGranted(UPDATE));

    authorization.addPermission(READ);
    assertTrue(authorization.isPermissionGranted(CREATE));
    assertTrue(authorization.isPermissionGranted(DELETE));
    assertTrue(authorization.isPermissionGranted(READ));
    assertFalse(authorization.isPermissionGranted(UPDATE));

    authorization.addPermission(UPDATE);
    assertTrue(authorization.isPermissionGranted(CREATE));
    assertTrue(authorization.isPermissionGranted(DELETE));
    assertTrue(authorization.isPermissionGranted(READ));
    assertTrue(authorization.isPermissionGranted(UPDATE));

    authorization.removePermission(CREATE);
    assertFalse(authorization.isPermissionGranted(CREATE));
    assertTrue(authorization.isPermissionGranted(DELETE));
    assertTrue(authorization.isPermissionGranted(READ));
    assertTrue(authorization.isPermissionGranted(UPDATE));

    authorization.removePermission(DELETE);
    assertFalse(authorization.isPermissionGranted(CREATE));
    assertFalse(authorization.isPermissionGranted(DELETE));
    assertTrue(authorization.isPermissionGranted(READ));
    assertTrue(authorization.isPermissionGranted(UPDATE));

    authorization.removePermission(READ);
    assertFalse(authorization.isPermissionGranted(CREATE));
    assertFalse(authorization.isPermissionGranted(DELETE));
    assertFalse(authorization.isPermissionGranted(READ));
    assertTrue(authorization.isPermissionGranted(UPDATE));

    authorization.removePermission(UPDATE);
    assertFalse(authorization.isPermissionGranted(CREATE));
    assertFalse(authorization.isPermissionGranted(DELETE));
    assertFalse(authorization.isPermissionGranted(READ));
    assertFalse(authorization.isPermissionGranted(UPDATE));

  }

  @Test
  public void testGrantAuthPermissions() {

    AuthorizationEntity authorization = new AuthorizationEntity(AUTH_TYPE_GRANT);
    authorization.setResource(Resources.DEPLOYMENT);

    assertFalse(authorization.isPermissionGranted(ALL));
    assertTrue(authorization.isPermissionGranted(NONE));
    List<Permission> perms = Arrays.asList(authorization.getPermissions(Permissions.values()));
    assertTrue(perms.contains(NONE));
    assertThat(perms.size()).isEqualTo(1);

    authorization.addPermission(READ);
    perms = Arrays.asList(authorization.getPermissions(Permissions.values()));
    assertTrue(perms.contains(NONE));
    assertTrue(perms.contains(READ));
    assertThat(perms.size()).isEqualTo(2);
    assertTrue(authorization.isPermissionGranted(READ));
    assertTrue(authorization.isPermissionGranted(NONE)); // (none is always granted => you are always authorized to do nothing)

    try {
      authorization.isPermissionRevoked(READ);
      fail("Exception expected");
    } catch (IllegalStateException e) {
      testRule.assertTextPresent("ENGINE-03026 Method 'isPermissionRevoked' cannot be used for authorization with type 'GRANT'.", e.getMessage());
    }

  }

  @Test
  public void testGlobalAuthPermissions() {

    AuthorizationEntity authorization = new AuthorizationEntity(AUTH_TYPE_GRANT);
    authorization.setResource(Resources.DEPLOYMENT);

    assertFalse(authorization.isPermissionGranted(ALL));
    assertTrue(authorization.isPermissionGranted(NONE));
    List<Permission> perms = Arrays.asList(authorization.getPermissions(Permissions.values()));
    assertTrue(perms.contains(NONE));
    assertThat(perms.size()).isEqualTo(1);

    authorization.addPermission(READ);
    perms = Arrays.asList(authorization.getPermissions(Permissions.values()));
    assertTrue(perms.contains(NONE));
    assertTrue(perms.contains(READ));
    assertThat(perms.size()).isEqualTo(2);
    assertTrue(authorization.isPermissionGranted(READ));
    assertTrue(authorization.isPermissionGranted(NONE)); // (none is always granted => you are always authorized to do nothing)

    try {
      authorization.isPermissionRevoked(READ);
      fail("Exception expected");
    } catch (IllegalStateException e) {
      testRule.assertTextPresent("ENGINE-03026 Method 'isPermissionRevoked' cannot be used for authorization with type 'GRANT'.", e.getMessage());
    }

  }

  @Test
  public void testRevokeAuthPermissions() {

    AuthorizationEntity authorization = new AuthorizationEntity(AUTH_TYPE_REVOKE);
    authorization.setResource(Resources.DEPLOYMENT);

    assertFalse(authorization.isPermissionRevoked(ALL));
    List<Permission> perms = Arrays.asList(authorization.getPermissions(Permissions.values()));
    assertThat(perms.size()).isEqualTo(0);

    authorization.removePermission(READ);
    perms = Arrays.asList(authorization.getPermissions(Permissions.values()));
    assertTrue(perms.contains(READ));
    assertTrue(perms.contains(ALL));
    assertThat(perms.size()).isEqualTo(2);

    try {
      authorization.isPermissionGranted(READ);
      fail("Exception expected");
    } catch (IllegalStateException e) {
      testRule.assertTextPresent("ENGINE-03026 Method 'isPermissionGranted' cannot be used for authorization with type 'REVOKE'.", e.getMessage());
    }

  }

  @Test
  public void testGlobalGrantAuthorizationCheck() {
    Resource resource1 = TestResource.RESOURCE1;

    // create global authorization which grants all permissions to all users (on resource1):
    Authorization globalAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    globalAuth.setResource(resource1);
    globalAuth.setResourceId(ANY);
    globalAuth.addPermission(TestPermissions.ALL);
    authorizationService.saveAuthorization(globalAuth);

    List<String> jonnysGroups = Arrays.asList(new String[]{"sales", "marketing"});
    List<String> someOneElsesGroups = Arrays.asList(new String[]{"marketing"});

    // this authorizes any user to do anything in this resource:
    processEngineConfiguration.setAuthorizationEnabled(true);
    assertTrue(authorizationService.isUserAuthorized("jonny", null, TestPermissions.ALL, resource1));
    assertTrue(authorizationService.isUserAuthorized("jonny", jonnysGroups, TestPermissions.ALL, resource1));
    assertTrue(authorizationService.isUserAuthorized("someone", null, TestPermissions.ACCESS, resource1));
    assertTrue(authorizationService.isUserAuthorized("someone", someOneElsesGroups, TestPermissions.ACCESS, resource1));
    assertTrue(authorizationService.isUserAuthorized("someone else", null, TestPermissions.DELETE, resource1));
    assertTrue(authorizationService.isUserAuthorized("jonny", null, TestPermissions.ALL, resource1, "someId"));
    assertTrue(authorizationService.isUserAuthorized("jonny", jonnysGroups, TestPermissions.ALL, resource1, "someId"));
    assertTrue(authorizationService.isUserAuthorized("someone", null, TestPermissions.ACCESS, resource1, "someId"));
    assertTrue(authorizationService.isUserAuthorized("someone else", null, TestPermissions.DELETE, resource1, "someOtherId"));
    processEngineConfiguration.setAuthorizationEnabled(true);
  }

  @Test
  public void testDisabledAuthorizationCheck() {
    // given
    Resource resource1 = TestResource.RESOURCE1;

    // when
    boolean isAuthorized = authorizationService.isUserAuthorized("jonny", null, UPDATE, resource1);

    // then
    assertTrue(isAuthorized);
  }

  @Test
  public void testConcurrentIsUserAuthorized() throws Exception {
    int threadCount = 2;
    int invocationCount = 500;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    try {
      ArrayList<Callable<Exception>> callables = new ArrayList<>();

      for (int i = 0; i < invocationCount; i++) {
        callables.add(() -> {
          try {
            authorizationService.isUserAuthorized("jonny", null, UPDATE, TestResource.RESOURCE1, ANY);
          }
          catch (Exception e) {
            return e;
          }
          return null;
        });
      }

      List<Future<Exception>> futures = executorService.invokeAll(callables);

      for (Future<Exception> future : futures) {
        Exception exception = future.get();
        if (exception != null) {
          fail("No exception expected: " + exception.getMessage());
        }
      }

    }
    finally {
      // reset original logging level
      executorService.shutdownNow();
      executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

  }

  @Test
  public void testReportResourceAuthorization() {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId(testUserId);
    authorization.addPermission(ALL);
    authorization.setResource(REPORT);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, Arrays.asList(testGroupId), ALL, REPORT)).isEqualTo(true);
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  public void testReportResourcePermissions() {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId(testUserId);
    authorization.addPermission(CREATE);
    authorization.addPermission(READ);
    authorization.addPermission(UPDATE);
    authorization.addPermission(DELETE);
    authorization.setResource(REPORT);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, CREATE, REPORT)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, READ, REPORT)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, UPDATE, REPORT)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, DELETE, REPORT)).isEqualTo(true);
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  public void testDashboardResourceAuthorization() {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId(testUserId);
    authorization.addPermission(ALL);
    authorization.setResource(DASHBOARD);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, Arrays.asList(testGroupId), ALL, DASHBOARD)).isEqualTo(true);
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  public void testDashboardResourcePermission() {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId(testUserId);
    authorization.addPermission(CREATE);
    authorization.addPermission(READ);
    authorization.addPermission(UPDATE);
    authorization.addPermission(DELETE);
    authorization.setResource(DASHBOARD);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, CREATE, DASHBOARD)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, READ, DASHBOARD)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, UPDATE, DASHBOARD)).isEqualTo(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, null, DELETE, DASHBOARD)).isEqualTo(true);
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  public void testIsPermissionGrantedAccess() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.addPermission(Permissions.ACCESS);
    authorization.setResource(Resources.APPLICATION);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    // then
    Authorization authorizationResult = authorizationService.createAuthorizationQuery().userIdIn(userId).singleResult();
    assertTrue(authorizationResult.isPermissionGranted(Permissions.ACCESS));
    assertFalse(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionGranted(ProcessInstancePermissions.RETRY_JOB));
    assertFalse(authorizationResult.isPermissionGranted(ProcessDefinitionPermissions.RETRY_JOB));
  }

  @Test
  public void testIsPermissionGrantedRetryJob() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.addPermission(ProcessInstancePermissions.RETRY_JOB);
    authorization.setResource(Resources.PROCESS_INSTANCE);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    // then
    Authorization authorizationResult = authorizationService.createAuthorizationQuery().userIdIn(userId).singleResult();
    assertTrue(authorizationResult.isPermissionGranted(ProcessInstancePermissions.RETRY_JOB));
    assertFalse(authorizationResult.isPermissionGranted(Permissions.ACCESS));
    assertFalse(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionGranted(ProcessDefinitionPermissions.RETRY_JOB));
  }

  @Test
  public void testIsPermissionGrantedBatchResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.addPermission(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES);
    authorization.addPermission(BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES);
    authorization.addPermission(BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES);
    authorization.setResource(Resources.BATCH);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    // then
    Authorization authorizationResult = authorizationService.createAuthorizationQuery().userIdIn(userId).singleResult();
    assertTrue(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES));
    assertTrue(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES));
    assertTrue(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MODIFY_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionGranted(Permissions.ACCESS));
    assertFalse(authorizationResult.isPermissionGranted(Permissions.CREATE));
  }

  @Test
  public void testIsPermissionRevokedAccess() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.removePermission(Permissions.ACCESS);
    authorization.setResource(Resources.APPLICATION);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    // then
    Authorization authorizationResult = authorizationService.createAuthorizationQuery().userIdIn(userId).singleResult();
    assertTrue(authorizationResult.isPermissionRevoked(Permissions.ACCESS));
    assertFalse(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionRevoked(ProcessInstancePermissions.RETRY_JOB));
    assertFalse(authorizationResult.isPermissionRevoked(ProcessDefinitionPermissions.RETRY_JOB));
  }

  @Test
  public void testIsPermissionRevokedRetryJob() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.removePermission(ProcessInstancePermissions.RETRY_JOB);
    authorization.setResource(Resources.PROCESS_INSTANCE);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    // then
    Authorization authorizationResult = authorizationService.createAuthorizationQuery().userIdIn(userId).singleResult();
    assertTrue(authorizationResult.isPermissionRevoked(ProcessInstancePermissions.RETRY_JOB));
    assertFalse(authorizationResult.isPermissionRevoked(Permissions.ACCESS));
    assertFalse(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionRevoked(ProcessDefinitionPermissions.RETRY_JOB));
  }

  @Test
  public void testIsPermissionRevokedBatchResource() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    String userId = "userId";
    authorization.setUserId(userId);
    authorization.removePermission(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES);
    authorization.removePermission(BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES);
    authorization.removePermission(BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES);
    authorization.setResource(Resources.BATCH);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    // then
    Authorization authorizationResult = authorizationService.createAuthorizationQuery().userIdIn(userId).singleResult();
    assertTrue(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES));
    assertTrue(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES));
    assertTrue(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MODIFY_PROCESS_INSTANCES));
    assertFalse(authorizationResult.isPermissionRevoked(Permissions.ACCESS));
    assertFalse(authorizationResult.isPermissionRevoked(Permissions.CREATE));
  }

  @Test
  public void shouldFailSaveAuthorizationWithIncompatibleResourceAndPermission() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("testUser");
    authorization.addPermission(TestPermissions.RANDOM);
    authorization.setResource(Resources.TASK);
    authorization.setResourceId(ANY);

    // when attempt to save, expect BadUserRequest
    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorization))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("ENGINE-03087 The resource type with id:'" + Resources.TASK.resourceType() + "' is not valid for '" + TestPermissions.RANDOM.getName() + "' permission." );
  }

  protected void cleanupAfterTest() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }


}
