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
package org.operaton.bpm.engine.test.api.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Sebastian Menski
 */
@ExtendWith(ProcessEngineExtension.class)
class FilterAuthorizationsTest {

  protected User testUser;

  protected Authorization createAuthorization;
  protected Authorization updateAuthorization;
  protected Authorization readAuthorization;
  protected Authorization deleteAuthorization;

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;
  protected FilterService filterService;
  protected AuthorizationService authorizationService;
  protected TaskService taskService;

  @BeforeEach
  void setUp() {
    testUser = createTestUser("test");

    createAuthorization = createAuthorization(Permissions.CREATE, Authorization.ANY);
    updateAuthorization = createAuthorization(Permissions.UPDATE, null);
    readAuthorization = createAuthorization(Permissions.READ, null);
    deleteAuthorization = createAuthorization(Permissions.DELETE, null);

    processEngineConfiguration.setAuthorizationEnabled(true);
    identityService.setAuthenticatedUserId(testUser.getId());
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    for (Filter filter : filterService.createFilterQuery().list()) {
      filterService.deleteFilter(filter.getId());
    }
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  void testCreateFilterNotPermitted() {
    assertThatThrownBy(() -> filterService.newTaskFilter())
      .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testCreateFilterPermitted() {
    grantCreateFilter();
    Filter filter = filterService.newTaskFilter();
    assertThat(filter).isNotNull();
  }

  @Test
  void testSaveFilterNotPermitted() {
    Filter filter = new FilterEntity(EntityTypes.TASK);
    assertThatThrownBy(() -> filterService.saveFilter(filter))
      .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testSaveFilterPermitted() {
    Filter filter = new FilterEntity(EntityTypes.TASK)
      .setName("testFilter");

    grantCreateFilter();

    filterService.saveFilter(filter);

    assertThat(filter.getId()).isNotNull();
  }

  @Test
  void testUpdateFilterNotPermitted() {
    Filter filter = createTestFilter();

    filter.setName("anotherName");

    assertThatThrownBy(() -> filterService.saveFilter(filter))
      .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testUpdateFilterPermitted() {
    Filter filter = createTestFilter();

    filter.setName("anotherName");

    grantUpdateFilter(filter.getId());

    filter = filterService.saveFilter(filter);
    assertThat(filter.getName()).isEqualTo("anotherName");
  }

  @Test
  void testDeleteFilterNotPermitted() {
    String filterId = createTestFilter().getId();

    assertThatThrownBy(() -> filterService.deleteFilter(filterId))
      .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testDeleteFilterPermitted() {
    Filter filter = createTestFilter();

    grantDeleteFilter(filter.getId());

    filterService.deleteFilter(filter.getId());

    long count = filterService.createFilterQuery().count();
    assertThat(count).isZero();
  }

  @Test
  void testReadFilterNotPermitted() {
    Filter filter = createTestFilter();

    long count = filterService.createFilterQuery().count();
    assertThat(count).isZero();

    Filter returnedFilter = filterService.createFilterQuery().filterId(filter.getId()).singleResult();
    assertThat(returnedFilter).isNull();

    String filterId = filter.getId();
    assertThatThrownBy(() -> filterService.getFilter(filterId))
      .isInstanceOf(AuthorizationException.class);

    String filterId1 = filter.getId();
    assertThatThrownBy(() -> filterService.singleResult(filterId1))
      .isInstanceOf(AuthorizationException.class);

    String filterId2 = filter.getId();
    assertThatThrownBy(() -> filterService.list(filterId2))
      .isInstanceOf(AuthorizationException.class);

    String filterId3 = filter.getId();
    assertThatThrownBy(() -> filterService.listPage(filterId3, 1, 2))
      .isInstanceOf(AuthorizationException.class);

    String filterId4 = filter.getId();
    assertThatThrownBy(() -> filterService.count(filterId4))
      .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testReadFilterPermitted() {
    Filter filter = createTestFilter();

    grantReadFilter(filter.getId());

    long count = filterService.createFilterQuery().count();
    assertThat(count).isEqualTo(1);

    Filter returnedFilter = filterService.createFilterQuery().filterId(filter.getId()).singleResult();
    assertThat(returnedFilter).isNotNull();

    returnedFilter = filterService.getFilter(filter.getId());
    assertThat(returnedFilter).isNotNull();

    // create test Task
    Task task = taskService.newTask("test");
    taskService.saveTask(task);

    Task result = filterService.singleResult(filter.getId());
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(task.getId());

    List<Task> resultList = filterService.list(filter.getId());
    assertThat(resultList)
            .isNotNull()
            .hasSize(1);
    assertThat(resultList.get(0).getId()).isEqualTo(task.getId());

    resultList = filterService.listPage(filter.getId(), 0, 2);
    assertThat(resultList)
            .isNotNull()
            .hasSize(1);
    assertThat(resultList.get(0).getId()).isEqualTo(task.getId());

    count = filterService.count(filter.getId());
    assertThat(count).isEqualTo(1);

    // remove Task
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  void shouldNotFindFilterWithRevokedReadPermissionOnAnyFilter() {
    Filter filter = createTestFilter();

    grantReadFilter(filter.getId());
    revokeReadFilter(filter.getId());

    long count = filterService.createFilterQuery().count();
    assertThat(count).isZero();

    Filter returnedFilter = filterService.createFilterQuery().filterId(filter.getId()).singleResult();
    assertThat(returnedFilter).isNull();

    String filterId1 = filter.getId();
    assertThatThrownBy(() -> filterService.getFilter(filterId1))
      .isInstanceOf(AuthorizationException.class);

    String filterId2 = filter.getId();
    assertThatThrownBy(() -> filterService.singleResult(filterId2))
      .isInstanceOf(AuthorizationException.class);

    String filterId3 = filter.getId();
    assertThatThrownBy(() -> filterService.list(filterId3))
      .isInstanceOf(AuthorizationException.class);

    String filterId4 = filter.getId();
    assertThatThrownBy(() -> filterService.listPage(filterId4, 1, 2))
      .isInstanceOf(AuthorizationException.class);

    String filterId5 = filter.getId();
    assertThatThrownBy(() -> filterService.count(filterId5))
      .isInstanceOf(AuthorizationException.class);
  }

  @Test
  void testReadFilterPermittedWithMultiple() {
    Filter filter = createTestFilter();

    grantReadFilter(filter.getId());
    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.addPermission(Permissions.READ);
    authorization.setUserId(Authorization.ANY);
    authorization.setResource(Resources.FILTER);
    authorization.setResourceId(Authorization.ANY);
    authorizationService.saveAuthorization(authorization);

    long count = filterService.createFilterQuery().count();
    assertThat(count).isEqualTo(1);

    Filter returnedFilter = filterService.createFilterQuery().filterId(filter.getId()).singleResult();
    assertThat(returnedFilter).isNotNull();

    returnedFilter = filterService.getFilter(filter.getId());
    assertThat(returnedFilter).isNotNull();

    authorizationService.deleteAuthorization(authorization.getId());
  }

  @Test
  void testDefaultFilterAuthorization() {
    // create two other users beside testUser
    User ownerUser = createTestUser("ownerUser");
    User anotherUser = createTestUser("anotherUser");

    // grant testUser create permission
    grantCreateFilter();

    // create a new filter with ownerUser as owner
    Filter filter = filterService.newTaskFilter("testFilter");
    filter.setOwner(ownerUser.getId());
    filterService.saveFilter(filter);

    assertFilterPermission(Permissions.CREATE, testUser, null, true);
    assertFilterPermission(Permissions.CREATE, ownerUser, null, false);
    assertFilterPermission(Permissions.CREATE, anotherUser, null, false);

    assertFilterPermission(Permissions.UPDATE, testUser, filter.getId(), false);
    assertFilterPermission(Permissions.UPDATE, ownerUser, filter.getId(), true);
    assertFilterPermission(Permissions.UPDATE, anotherUser, filter.getId(), false);

    assertFilterPermission(Permissions.READ, testUser, filter.getId(), false);
    assertFilterPermission(Permissions.READ, ownerUser, filter.getId(), true);
    assertFilterPermission(Permissions.READ, anotherUser, filter.getId(), false);

    assertFilterPermission(Permissions.DELETE, testUser, filter.getId(), false);
    assertFilterPermission(Permissions.DELETE, ownerUser, filter.getId(), true);
    assertFilterPermission(Permissions.DELETE, anotherUser, filter.getId(), false);
  }

  @Test
  void testCreateFilterGenericOwnerId() {
    grantCreateFilter();

    Filter filter = filterService.newTaskFilter("someName");
    filter.setOwner("*");

    assertThatThrownBy(() -> filterService.saveFilter(filter))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create default authorization for filter owner *: id cannot be *. * is a reserved identifier.");
  }

  @Disabled("CAM-4889")
  @Test
  void testUpdateFilterGenericOwnerId() {
    grantCreateFilter();

    Filter filter = filterService.newTaskFilter("someName");
    filterService.saveFilter(filter);

    grantUpdateFilter(filter.getId());
    filter.setOwner("*");

    assertThatThrownBy(() -> filterService.saveFilter(filter))
      .withFailMessage("it should not be possible to save a filter with the generic owner id")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining("foo");
  }

  protected User createTestUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);

    // give user all permission to manipulate authorisations
    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setUserId(user.getId());
    authorization.setResource(Resources.AUTHORIZATION);
    authorization.setResourceId(Authorization.ANY);
    authorization.addPermission(Permissions.ALL);
    authorizationService.saveAuthorization(authorization);

    // give user all permission to manipulate users
    authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setUserId(user.getId());
    authorization.setResource(Resources.USER);
    authorization.setResourceId(Authorization.ANY);
    authorization.addPermission(Permissions.ALL);
    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setUserId(user.getId());
    authorization.setResource(Resources.TASK);
    authorization.setResourceId(Authorization.ANY);
    authorization.addPermission(Permissions.ALL);
    authorizationService.saveAuthorization(authorization);

    return user;
  }

  protected Filter createTestFilter() {
    grantCreateFilter();
    Filter filter = filterService.newTaskFilter("testFilter");
    return filterService.saveFilter(filter);
  }

  protected Authorization createAuthorization(Permission permission, String resourceId) {
    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setUserId(testUser.getId());
    authorization.setResource(Resources.FILTER);
    authorization.addPermission(permission);
    if (resourceId != null) {
      authorization.setResourceId(resourceId);
    }
    return authorization;
  }

  protected void grantCreateFilter() {
    grantFilterPermission(createAuthorization, null);
    assertFilterPermission(Permissions.CREATE, testUser, null, true);
  }

  protected void grantUpdateFilter(String filterId) {
    grantFilterPermission(updateAuthorization, filterId);
    assertFilterPermission(Permissions.UPDATE, testUser, filterId, true);
  }

  protected void grantReadFilter(String filterId) {
    grantFilterPermission(readAuthorization, filterId);
    assertFilterPermission(Permissions.READ, testUser, filterId, true);
  }

  protected void revokeReadFilter(String filterId) {
    revokeFilterPermission(readAuthorization, filterId);
    assertFilterPermission(Permissions.READ, testUser, filterId, false);
  }

  protected void grantDeleteFilter(String filterId) {
    grantFilterPermission(deleteAuthorization, filterId);
    assertFilterPermission(Permissions.DELETE, testUser, filterId, true);
  }

  protected void grantFilterPermission(Authorization authorization, String filterId) {
    if (filterId != null) {
      authorization.setResourceId(filterId);
    }
    authorizationService.saveAuthorization(authorization);
  }

  protected void revokeFilterPermission(Authorization authorization, String filterId) {
    if (filterId != null) {
      authorization.setResourceId(filterId);
    }
    ((AuthorizationEntity) authorization).setAuthorizationType(Authorization.AUTH_TYPE_REVOKE);
    authorizationService.saveAuthorization(authorization);
  }

  protected void assertFilterPermission(Permission permission, User user, String filterId, boolean expected) {
    boolean result;
    if (filterId != null) {
      result = authorizationService.isUserAuthorized(user.getId(), null, permission, Resources.FILTER, filterId);
    }
    else {
      result = authorizationService.isUserAuthorized(user.getId(), null, permission, Resources.FILTER);
    }
    assertThat(result).isEqualTo(expected);
  }

}
