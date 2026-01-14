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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.ProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Daniel Meyer
 *
 */
class AuthorizationServiceTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected AuthorizationService authorizationService;
  protected IdentityService identityService;

  protected String testUserId = "test";
  protected String testGroupId = "accounting";

  @AfterEach
  void tearDown() {
    cleanupAfterTest();

  }

  @Test
  void testGlobalAuthorizationType() {
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
  void testGrantAuthorizationType() {
    Authorization grantAuthorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    // I can set userId = null
    grantAuthorization.setUserId(null);
    assertThat(grantAuthorization.getUserId()).isNull();
    // I can set userId = ANY
    grantAuthorization.setUserId(ANY);
    assertThat(grantAuthorization.getUserId()).isEqualTo(ANY);
    // I can set anything else:
    grantAuthorization.setUserId("something");
    assertThat(grantAuthorization.getUserId()).isEqualTo("something");
    // I can set groupId = null
    grantAuthorization.setGroupId(null);
    assertThat(grantAuthorization.getGroupId()).isNull();
    // I can set anything else:
    grantAuthorization.setGroupId("something");
    assertThat(grantAuthorization.getGroupId()).isEqualTo("something");
  }

  @Test
  void testRevokeAuthorizationType() {
    Authorization revokeAuthorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    // I can set userId = null
    revokeAuthorization.setUserId(null);
    assertThat(revokeAuthorization.getUserId()).isNull();
    // I can set userId = ANY
    revokeAuthorization.setUserId(ANY);
    assertThat(revokeAuthorization.getUserId()).isEqualTo(ANY);
    // I can set anything else:
    revokeAuthorization.setUserId("something");
    assertThat(revokeAuthorization.getUserId()).isEqualTo("something");
    // I can set groupId = null
    revokeAuthorization.setGroupId(null);
    assertThat(revokeAuthorization.getGroupId()).isNull();
    // I can set anything else:
    revokeAuthorization.setGroupId("something");
    assertThat(revokeAuthorization.getGroupId()).isEqualTo("something");
  }

  @Test
  void testDeleteNonExistingAuthorization() {

    try {
      authorizationService.deleteAuthorization("nonExisting");
      fail("");
    } catch (Exception e) {
      testRule.assertTextPresent("Authorization for Id 'nonExisting' does not exist: authorization is null", e.getMessage());
    }

  }

  @Test
  void testCreateAuthorizationWithUserId() {

    Resource resource1 = TestResource.RESOURCE1;

    // initially, no authorization exists:
    assertThat(authorizationService.createAuthorizationQuery().count()).isZero();

    // simple create / delete with userId
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("aUserId");
    authorization.setResource(resource1);

    // save the authorization
    authorizationService.saveAuthorization(authorization);
    // authorization exists
    assertThat(authorizationService.createAuthorizationQuery().count()).isOne();
    // delete the authorization
    authorizationService.deleteAuthorization(authorization.getId());
    // it's gone
    assertThat(authorizationService.createAuthorizationQuery().count()).isZero();

  }

  @Test
  void testCreateAuthorizationWithGroupId() {

    Resource resource1 = TestResource.RESOURCE1;

    // initially, no authorization exists:
    assertThat(authorizationService.createAuthorizationQuery().count()).isZero();

    // simple create / delete with userId
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setGroupId("aGroupId");
    authorization.setResource(resource1);

    // save the authorization
    authorizationService.saveAuthorization(authorization);
    // authorization exists
    assertThat(authorizationService.createAuthorizationQuery().count()).isOne();
    // delete the authorization
    authorizationService.deleteAuthorization(authorization.getId());
    // it's gone
    assertThat(authorizationService.createAuthorizationQuery().count()).isZero();

  }

  @Test
  void testInvalidCreateAuthorization() {

    Resource resource1 = TestResource.RESOURCE1;

    // case 1: no user id & no group id ////////////

    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setResource(resource1);

    try {
      authorizationService.saveAuthorization(authorization);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Authorization must either have a 'userId' or a 'groupId'.");
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
      assertThat(e.getMessage()).contains("Authorization 'resourceType' cannot be null.");
    }

    // case 4: no permissions /////////////////

    authorization = authorizationService.createNewAuthorization(AUTH_TYPE_REVOKE);
    authorization.setUserId("someId");

    try {
      authorizationService.saveAuthorization(authorization);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Authorization 'resourceType' cannot be null.");
    }
  }

  @Test
  void testUniqueUserConstraints() {

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
    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorization2)).isInstanceOf(ProcessEngineException.class);

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

    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorization4)).isInstanceOf(Exception.class);
  }

  @Test
  void testUniqueGroupConstraints() {

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
    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorization2)).isInstanceOf(Exception.class);

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

    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorization4)).isInstanceOf(Exception.class);

  }

  @Test
  void testGlobalUniqueConstraints() {

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
    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorization2)).isInstanceOf(Exception.class);
  }

  @Test
  void testUpdateNewAuthorization() {

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
    assertThat(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS)).isTrue();

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
    assertThat(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS)).isTrue();
    assertThat(savedAuthorization.isPermissionGranted(TestPermissions.DELETE)).isTrue();

  }

  @Test
  void testUpdatePersistentAuthorization() {

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
    assertThat(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS)).isTrue();

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
    assertThat(savedAuthorization.isPermissionGranted(TestPermissions.ACCESS)).isTrue();
    assertThat(savedAuthorization.isPermissionGranted(TestPermissions.DELETE)).isTrue();

  }

  @Test
  void testPermissions() {

    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setResource(Resources.USER);

    assertThat(authorization.getPermissions(Permissions.values())).hasSize(1);

    assertThat(authorization.isPermissionGranted(CREATE)).isFalse();
    assertThat(authorization.isPermissionGranted(DELETE)).isFalse();
    assertThat(authorization.isPermissionGranted(READ)).isFalse();
    assertThat(authorization.isPermissionGranted(UPDATE)).isFalse();

    authorization.addPermission(CREATE);
    assertThat(authorization.isPermissionGranted(CREATE)).isTrue();
    assertThat(authorization.isPermissionGranted(DELETE)).isFalse();
    assertThat(authorization.isPermissionGranted(READ)).isFalse();
    assertThat(authorization.isPermissionGranted(UPDATE)).isFalse();

    authorization.addPermission(DELETE);
    assertThat(authorization.isPermissionGranted(CREATE)).isTrue();
    assertThat(authorization.isPermissionGranted(DELETE)).isTrue();
    assertThat(authorization.isPermissionGranted(READ)).isFalse();
    assertThat(authorization.isPermissionGranted(UPDATE)).isFalse();

    authorization.addPermission(READ);
    assertThat(authorization.isPermissionGranted(CREATE)).isTrue();
    assertThat(authorization.isPermissionGranted(DELETE)).isTrue();
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(UPDATE)).isFalse();

    authorization.addPermission(UPDATE);
    assertThat(authorization.isPermissionGranted(CREATE)).isTrue();
    assertThat(authorization.isPermissionGranted(DELETE)).isTrue();
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(UPDATE)).isTrue();

    authorization.removePermission(CREATE);
    assertThat(authorization.isPermissionGranted(CREATE)).isFalse();
    assertThat(authorization.isPermissionGranted(DELETE)).isTrue();
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(UPDATE)).isTrue();

    authorization.removePermission(DELETE);
    assertThat(authorization.isPermissionGranted(CREATE)).isFalse();
    assertThat(authorization.isPermissionGranted(DELETE)).isFalse();
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(UPDATE)).isTrue();

    authorization.removePermission(READ);
    assertThat(authorization.isPermissionGranted(CREATE)).isFalse();
    assertThat(authorization.isPermissionGranted(DELETE)).isFalse();
    assertThat(authorization.isPermissionGranted(READ)).isFalse();
    assertThat(authorization.isPermissionGranted(UPDATE)).isTrue();

    authorization.removePermission(UPDATE);
    assertThat(authorization.isPermissionGranted(CREATE)).isFalse();
    assertThat(authorization.isPermissionGranted(DELETE)).isFalse();
    assertThat(authorization.isPermissionGranted(READ)).isFalse();
    assertThat(authorization.isPermissionGranted(UPDATE)).isFalse();

  }

  @Test
  void testGrantAuthPermissions() {

    AuthorizationEntity authorization = new AuthorizationEntity(AUTH_TYPE_GRANT);
    authorization.setResource(Resources.DEPLOYMENT);

    assertThat(authorization.isPermissionGranted(ALL)).isFalse();
    assertThat(authorization.isPermissionGranted(NONE)).isTrue();
    List<Permission> perms = List.of(authorization.getPermissions(Permissions.values()));
    assertThat(perms)
            .contains(NONE)
            .hasSize(1);

    authorization.addPermission(READ);
    perms = List.of(authorization.getPermissions(Permissions.values()));
    assertThat(perms)
            .contains(NONE)
            .contains(READ)
            .hasSize(2);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(NONE)).isTrue(); // (none is always granted => you are always authorized to do nothing)

    try {
      authorization.isPermissionRevoked(READ);
      fail("Exception expected");
    } catch (IllegalStateException e) {
      testRule.assertTextPresent("ENGINE-03026 Method 'isPermissionRevoked' cannot be used for authorization with type 'GRANT'.", e.getMessage());
    }

  }

  @Test
  void testGlobalAuthPermissions() {

    AuthorizationEntity authorization = new AuthorizationEntity(AUTH_TYPE_GLOBAL);
    authorization.setResource(Resources.DEPLOYMENT);

    assertThat(authorization.isPermissionGranted(ALL)).isFalse();
    assertThat(authorization.isPermissionGranted(NONE)).isTrue();
    List<Permission> perms = List.of(authorization.getPermissions(Permissions.values()));
    assertThat(perms)
            .contains(NONE)
            .hasSize(1);

    authorization.addPermission(READ);
    perms = List.of(authorization.getPermissions(Permissions.values()));
    assertThat(perms)
            .contains(NONE)
            .contains(READ)
            .hasSize(2);
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(NONE)).isTrue(); // (none is always granted => you are always authorized to do nothing)

    assertThat(authorization.isPermissionRevoked(READ)).isFalse();
  }

  @Test
  void testRevokeAuthPermissions() {

    AuthorizationEntity authorization = new AuthorizationEntity(AUTH_TYPE_REVOKE);
    authorization.setResource(Resources.DEPLOYMENT);

    assertThat(authorization.isPermissionRevoked(ALL)).isFalse();
    List<Permission> perms = List.of(authorization.getPermissions(Permissions.values()));
    assertThat(perms).isEmpty();

    authorization.removePermission(READ);
    perms = List.of(authorization.getPermissions(Permissions.values()));
    assertThat(perms)
            .contains(READ)
            .contains(ALL)
            .hasSize(2);

    try {
      authorization.isPermissionGranted(READ);
      fail("Exception expected");
    } catch (IllegalStateException e) {
      testRule.assertTextPresent("ENGINE-03026 Method 'isPermissionGranted' cannot be used for authorization with type 'REVOKE'.", e.getMessage());
    }

  }

  @Test
  void testGlobalGrantAuthorizationCheck() {
    Resource resource1 = TestResource.RESOURCE1;

    // create global authorization which grants all permissions to all users (on resource1):
    Authorization globalAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    globalAuth.setResource(resource1);
    globalAuth.setResourceId(ANY);
    globalAuth.addPermission(TestPermissions.ALL);
    authorizationService.saveAuthorization(globalAuth);

    List<String> jonnysGroups = List.of("sales", "marketing");
    List<String> someOneElsesGroups = List.of("marketing");

    // this authorizes any user to do anything in this resource:
    processEngineConfiguration.setAuthorizationEnabled(true);
    assertThat(authorizationService.isUserAuthorized("jonny", null, TestPermissions.ALL, resource1)).isTrue();
    assertThat(authorizationService.isUserAuthorized("jonny", jonnysGroups, TestPermissions.ALL, resource1)).isTrue();
    assertThat(authorizationService.isUserAuthorized("someone", null, TestPermissions.ACCESS, resource1)).isTrue();
    assertThat(authorizationService.isUserAuthorized("someone", someOneElsesGroups, TestPermissions.ACCESS, resource1)).isTrue();
    assertThat(authorizationService.isUserAuthorized("someone else", null, TestPermissions.DELETE, resource1)).isTrue();
    assertThat(authorizationService.isUserAuthorized("jonny", null, TestPermissions.ALL, resource1, "someId")).isTrue();
    assertThat(authorizationService.isUserAuthorized("jonny", jonnysGroups, TestPermissions.ALL, resource1, "someId")).isTrue();
    assertThat(authorizationService.isUserAuthorized("someone", null, TestPermissions.ACCESS, resource1, "someId")).isTrue();
    assertThat(authorizationService.isUserAuthorized("someone else", null, TestPermissions.DELETE, resource1, "someOtherId")).isTrue();
    processEngineConfiguration.setAuthorizationEnabled(true);
  }

  @Test
  void testDisabledAuthorizationCheck() {
    // given
    Resource resource1 = TestResource.RESOURCE1;

    // when
    boolean isAuthorized = authorizationService.isUserAuthorized("jonny", null, UPDATE, resource1);

    // then
    assertThat(isAuthorized).isTrue();
  }

  @Test
  void testConcurrentIsUserAuthorized() throws Exception {
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
  void testReportResourceAuthorization() {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId(testUserId);
    authorization.addPermission(ALL);
    authorization.setResource(REPORT);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, List.of(testGroupId), ALL, REPORT)).isTrue();
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  void testReportResourcePermissions() {
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
    assertThat(authorizationService.isUserAuthorized(testUserId, null, CREATE, REPORT)).isTrue();
    assertThat(authorizationService.isUserAuthorized(testUserId, null, READ, REPORT)).isTrue();
    assertThat(authorizationService.isUserAuthorized(testUserId, null, UPDATE, REPORT)).isTrue();
    assertThat(authorizationService.isUserAuthorized(testUserId, null, DELETE, REPORT)).isTrue();
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  void testDashboardResourceAuthorization() {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId(testUserId);
    authorization.addPermission(ALL);
    authorization.setResource(DASHBOARD);
    authorization.setResourceId(ANY);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    assertThat(authorizationService.isUserAuthorized(testUserId, List.of(testGroupId), ALL, DASHBOARD)).isTrue();
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  void testDashboardResourcePermission() {
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
    assertThat(authorizationService.isUserAuthorized(testUserId, null, CREATE, DASHBOARD)).isTrue();
    assertThat(authorizationService.isUserAuthorized(testUserId, null, READ, DASHBOARD)).isTrue();
    assertThat(authorizationService.isUserAuthorized(testUserId, null, UPDATE, DASHBOARD)).isTrue();
    assertThat(authorizationService.isUserAuthorized(testUserId, null, DELETE, DASHBOARD)).isTrue();
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  @Test
  void testIsPermissionGrantedAccess() {
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
    assertThat(authorizationResult.isPermissionGranted(Permissions.ACCESS)).isTrue();
    assertThat(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES)).isFalse();
    assertThat(authorizationResult.isPermissionGranted(ProcessInstancePermissions.RETRY_JOB)).isFalse();
    assertThat(authorizationResult.isPermissionGranted(ProcessDefinitionPermissions.RETRY_JOB)).isFalse();
  }

  @Test
  void testIsPermissionGrantedRetryJob() {
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
    assertThat(authorizationResult.isPermissionGranted(ProcessInstancePermissions.RETRY_JOB)).isTrue();
    assertThat(authorizationResult.isPermissionGranted(Permissions.ACCESS)).isFalse();
    assertThat(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES)).isFalse();
    assertThat(authorizationResult.isPermissionGranted(ProcessDefinitionPermissions.RETRY_JOB)).isFalse();
  }

  @Test
  void testIsPermissionGrantedBatchResource() {
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
    assertThat(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES)).isTrue();
    assertThat(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES)).isTrue();
    assertThat(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES)).isTrue();
    assertThat(authorizationResult.isPermissionGranted(BatchPermissions.CREATE_BATCH_MODIFY_PROCESS_INSTANCES)).isFalse();
    assertThat(authorizationResult.isPermissionGranted(Permissions.ACCESS)).isFalse();
    assertThat(authorizationResult.isPermissionGranted(Permissions.CREATE)).isFalse();
  }

  @Test
  void testIsPermissionRevokedAccess() {
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
    assertThat(authorizationResult.isPermissionRevoked(Permissions.ACCESS)).isTrue();
    assertThat(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES)).isFalse();
    assertThat(authorizationResult.isPermissionRevoked(ProcessInstancePermissions.RETRY_JOB)).isFalse();
    assertThat(authorizationResult.isPermissionRevoked(ProcessDefinitionPermissions.RETRY_JOB)).isFalse();
  }

  @Test
  void testIsPermissionRevokedRetryJob() {
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
    assertThat(authorizationResult.isPermissionRevoked(ProcessInstancePermissions.RETRY_JOB)).isTrue();
    assertThat(authorizationResult.isPermissionRevoked(Permissions.ACCESS)).isFalse();
    assertThat(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES)).isFalse();
    assertThat(authorizationResult.isPermissionRevoked(ProcessDefinitionPermissions.RETRY_JOB)).isFalse();
  }

  @Test
  void testIsPermissionRevokedBatchResource() {
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
    assertThat(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES)).isTrue();
    assertThat(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES)).isTrue();
    assertThat(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES)).isTrue();
    assertThat(authorizationResult.isPermissionRevoked(BatchPermissions.CREATE_BATCH_MODIFY_PROCESS_INSTANCES)).isFalse();
    assertThat(authorizationResult.isPermissionRevoked(Permissions.ACCESS)).isFalse();
    assertThat(authorizationResult.isPermissionRevoked(Permissions.CREATE)).isFalse();
  }

  @Test
  void shouldFailSaveAuthorizationWithIncompatibleResourceAndPermission() {
    // given
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setUserId("testUser");
    authorization.addPermission(TestPermissions.RANDOM);
    authorization.setResource(Resources.TASK);
    authorization.setResourceId(ANY);

    // when attempt to save, expect BadUserRequest
    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorization))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("ENGINE-03087 The resource type with id:'%s' is not valid for '%s' permission.".formatted(Resources.TASK.resourceType(), TestPermissions.RANDOM.getName()));
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
