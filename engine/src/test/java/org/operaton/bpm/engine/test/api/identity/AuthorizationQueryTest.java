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
import static org.junit.Assert.fail;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Daniel Meyer
 *
 */
public class AuthorizationQueryTest extends PluggableProcessEngineTest {

  @Before
  public void setUp() {


    Resource resource1 = TestResource.RESOURCE1;
    Resource resource2 = TestResource.RESOURCE2;

    createAuthorization("user1", null, resource1, "resource1-1", TestPermissions.ACCESS);
    createAuthorization("user1", null, resource2, "resource2-1", TestPermissions.DELETE);
    createAuthorization("user2", null, resource1, "resource1-2");
    createAuthorization("user3", null, resource2, "resource2-1", TestPermissions.READ, TestPermissions.UPDATE);

    createAuthorization(null, "group1", resource1, "resource1-1");
    createAuthorization(null, "group1", resource1, "resource1-2", TestPermissions.UPDATE);
    createAuthorization(null, "group2", resource2, "resource2-2", TestPermissions.READ, TestPermissions.UPDATE);
    createAuthorization(null, "group3", resource2, "resource2-3", TestPermissions.DELETE);

  }
  @After
  public void tearDown() {
    List<Authorization> list = authorizationService.createAuthorizationQuery().list();
    for (Authorization authorization : list) {
      authorizationService.deleteAuthorization(authorization.getId());
    }

  }

  protected void createAuthorization(String userId, String groupId, Resource resourceType, String resourceId, Permission... permissions) {

    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setUserId(userId);
    authorization.setGroupId(groupId);
    authorization.setResource(resourceType);
    authorization.setResourceId(resourceId);

    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }

    authorizationService.saveAuthorization(authorization);
  }

  @Test
  public void testValidQueryCounts() {

    Resource resource1 = TestResource.RESOURCE1;
    Resource resource2 = TestResource.RESOURCE2;
    Resource nonExisting = new NonExistingResource("non-existing", 102);

    // query by user id
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1").count()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user2").count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user3").count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1", "user2").count()).isEqualTo(3);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("non-existing").count()).isEqualTo(0);

    // query by group id
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group1").count()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group2").count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group3").count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group1", "group2").count()).isEqualTo(3);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("non-existing").count()).isEqualTo(0);

    // query by resource type
    assertThat(authorizationService.createAuthorizationQuery().resourceType(resource1).count()).isEqualTo(4);
    assertThat(authorizationService.createAuthorizationQuery().resourceType(nonExisting).count()).isEqualTo(0);
    assertThat(authorizationService.createAuthorizationQuery().resourceType(resource1.resourceType()).count()).isEqualTo(4);
    assertThat(authorizationService.createAuthorizationQuery().resourceType(nonExisting.resourceType()).count()).isEqualTo(0);

    // query by resource id
    assertThat(authorizationService.createAuthorizationQuery().resourceId("resource1-2").count()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().resourceId("non-existing").count()).isEqualTo(0);

    // query by permission
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.ACCESS).count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.DELETE).count()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.READ).count()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.UPDATE).count()).isEqualTo(3);
    // multiple permissions at the same time
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.READ).hasPermission(TestPermissions.UPDATE).count()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.UPDATE).hasPermission(TestPermissions.READ).count()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.READ).hasPermission(TestPermissions.ACCESS).count()).isEqualTo(0);

    // user id & resource type
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1").resourceType(resource1).count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1").resourceType(nonExisting).count()).isEqualTo(0);

    // group id & resource type
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group2").resourceType(resource2).count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group1").resourceType(nonExisting).count()).isEqualTo(0);
  }

  @Test
  public void testValidQueryLists() {

    Resource resource1 = TestResource.RESOURCE1;
    Resource resource2 = TestResource.RESOURCE2;
    Resource nonExisting = new NonExistingResource("non-existing", 102);

    // query by user id
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1").list().size()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user2").list().size()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user3").list().size()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1", "user2").list().size()).isEqualTo(3);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("non-existing").list().size()).isEqualTo(0);

    // query by group id
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group1").list().size()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group2").list().size()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group3").list().size()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group1", "group2").list().size()).isEqualTo(3);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("non-existing").list().size()).isEqualTo(0);

    // query by resource type
    assertThat(authorizationService.createAuthorizationQuery().resourceType(resource1).list().size()).isEqualTo(4);
    assertThat(authorizationService.createAuthorizationQuery().resourceType(nonExisting).list().size()).isEqualTo(0);

    // query by resource id
    assertThat(authorizationService.createAuthorizationQuery().resourceId("resource1-2").list().size()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().resourceId("non-existing").list().size()).isEqualTo(0);

    // query by permission
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.ACCESS).list().size()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.DELETE).list().size()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.READ).list().size()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.UPDATE).list().size()).isEqualTo(3);
    // multiple permissions at the same time
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.READ).hasPermission(TestPermissions.UPDATE).list().size()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.UPDATE).hasPermission(TestPermissions.READ).list().size()).isEqualTo(2);
    assertThat(authorizationService.createAuthorizationQuery().hasPermission(TestPermissions.READ).hasPermission(TestPermissions.ACCESS).list().size()).isEqualTo(0);

    // user id & resource type
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1").resourceType(resource1).list().size()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().userIdIn("user1").resourceType(nonExisting).list().size()).isEqualTo(0);

    // group id & resource type
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group2").resourceType(resource2).list().size()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery().groupIdIn("group1").resourceType(nonExisting).list().size()).isEqualTo(0);
  }

  @Test
  public void testOrderByQueries() {

    Resource resource1 = TestResource.RESOURCE1;
    Resource resource2 = TestResource.RESOURCE2;

    List<Authorization> list = authorizationService.createAuthorizationQuery().orderByResourceType().asc().list();
    assertThat(list.get(0).getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(list.get(1).getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(list.get(2).getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(list.get(3).getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(list.get(4).getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(list.get(5).getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(list.get(6).getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(list.get(7).getResourceType()).isEqualTo(resource2.resourceType());

    list = authorizationService.createAuthorizationQuery().orderByResourceType().desc().list();
    assertThat(list.get(0).getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(list.get(1).getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(list.get(2).getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(list.get(3).getResourceType()).isEqualTo(resource2.resourceType());
    assertThat(list.get(4).getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(list.get(5).getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(list.get(6).getResourceType()).isEqualTo(resource1.resourceType());
    assertThat(list.get(7).getResourceType()).isEqualTo(resource1.resourceType());

    list = authorizationService.createAuthorizationQuery().orderByResourceId().asc().list();
    assertThat(list.get(0).getResourceId()).isEqualTo("resource1-1");
    assertThat(list.get(1).getResourceId()).isEqualTo("resource1-1");
    assertThat(list.get(2).getResourceId()).isEqualTo("resource1-2");
    assertThat(list.get(3).getResourceId()).isEqualTo("resource1-2");
    assertThat(list.get(4).getResourceId()).isEqualTo("resource2-1");
    assertThat(list.get(5).getResourceId()).isEqualTo("resource2-1");
    assertThat(list.get(6).getResourceId()).isEqualTo("resource2-2");
    assertThat(list.get(7).getResourceId()).isEqualTo("resource2-3");

    list = authorizationService.createAuthorizationQuery().orderByResourceId().desc().list();
    assertThat(list.get(0).getResourceId()).isEqualTo("resource2-3");
    assertThat(list.get(1).getResourceId()).isEqualTo("resource2-2");
    assertThat(list.get(2).getResourceId()).isEqualTo("resource2-1");
    assertThat(list.get(3).getResourceId()).isEqualTo("resource2-1");
    assertThat(list.get(4).getResourceId()).isEqualTo("resource1-2");
    assertThat(list.get(5).getResourceId()).isEqualTo("resource1-2");
    assertThat(list.get(6).getResourceId()).isEqualTo("resource1-1");
    assertThat(list.get(7).getResourceId()).isEqualTo("resource1-1");

  }

  @Test
  public void testInvalidOrderByQueries() {
    var authorizationQuery = authorizationService.createAuthorizationQuery().orderByResourceType().orderByResourceId();
    try {
      authorizationQuery.list();
      fail("Exception expected");
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Invalid query: call asc() or desc() after using orderByXX()", e.getMessage());
    }

    try {
      authorizationQuery.list();
      fail("Exception expected");
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Invalid query: call asc() or desc() after using orderByXX()", e.getMessage());
    }

    try {
      authorizationQuery.list();
      fail("Exception expected");
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Invalid query: call asc() or desc() after using orderByXX()", e.getMessage());
    }

    try {
      authorizationQuery.list();
      fail("Exception expected");
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Invalid query: call asc() or desc() after using orderByXX()", e.getMessage());
    }
  }

  @Test
  public void testInvalidQueries() {

    // cannot query for user id and group id at the same time

    try {
      authorizationService.createAuthorizationQuery().groupIdIn("a").userIdIn("b").count();
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot query for user and group authorizations at the same time.", e.getMessage());
    }

    try {
      authorizationService.createAuthorizationQuery().userIdIn("b").groupIdIn("a").count();
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot query for user and group authorizations at the same time.", e.getMessage());
    }

  }

  class NonExistingResource implements Resource {

    protected int id;
    protected String name;

    public NonExistingResource(String name, int id) {
      this.name = name;
      this.id = id;
    }

    @Override
    public String resourceName() {
      return name;
    }

    @Override
    public int resourceType() {
      return id;
    }

  }

}
