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
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Joram Barrez
 */
public class GroupQueryTest extends PluggableProcessEngineTest {

  @Before
  public void setUp() {


    createGroup("muppets", "Muppet show characters_", "user");
    createGroup("frogs", "Famous frogs", "user");
    createGroup("mammals", "Famous mammals from eighties", "user");
    createGroup("admin", "Administrators", "security");

    identityService.saveUser(identityService.newUser("kermit"));
    identityService.saveUser(identityService.newUser("fozzie"));
    identityService.saveUser(identityService.newUser("mispiggy"));

    identityService.saveTenant(identityService.newTenant("tenant"));

    identityService.createMembership("kermit", "muppets");
    identityService.createMembership("fozzie", "muppets");
    identityService.createMembership("mispiggy", "muppets");

    identityService.createMembership("kermit", "frogs");

    identityService.createMembership("fozzie", "mammals");
    identityService.createMembership("mispiggy", "mammals");

    identityService.createMembership("kermit", "admin");

    identityService.createTenantGroupMembership("tenant", "frogs");

  }

  private Group createGroup(String id, String name, String type) {
    Group group = identityService.newGroup(id);
    group.setName(name);
    group.setType(type);
    identityService.saveGroup(group);
    return group;
  }

  @After
  public void tearDown() {
    identityService.deleteUser("kermit");
    identityService.deleteUser("fozzie");
    identityService.deleteUser("mispiggy");

    identityService.deleteGroup("muppets");
    identityService.deleteGroup("mammals");
    identityService.deleteGroup("frogs");
    identityService.deleteGroup("admin");

    identityService.deleteTenant("tenant");


  }

  @Test
  public void testQueryById() {
    GroupQuery query = identityService.createGroupQuery().groupId("muppets");
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByInvalidId() {
    GroupQuery query = identityService.createGroupQuery().groupId("invalid");
    verifyQueryResults(query, 0);
    var groupQuery = identityService.createGroupQuery();

    try {
      groupQuery.groupId(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided id is null");
    }
  }

  @Test
  public void testQueryByIdIn() {
    // empty list
    assertThat(identityService.createGroupQuery().groupIdIn("a", "b").list()).isEmpty();

    // collect all ids
    List<Group> list = identityService.createGroupQuery().list();
    String[] ids = new String[list.size()];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = list.get(i).getId();
    }

    List<Group> idInList = identityService.createGroupQuery().groupIdIn(ids).list();
    assertThat(idInList).hasSize(list.size());
    for (Group group : idInList) {
      boolean found = false;
      for (Group otherGroup : list) {
        if(otherGroup.getId().equals(group.getId())) {
          found = true; break;
        }
      }
      if(!found) {
        fail("Expected to find group " + group);
      }
    }
  }

  @Test
  public void testQueryByName() {
    GroupQuery query = identityService.createGroupQuery().groupName("Muppet show characters_");
    verifyQueryResults(query, 1);

    query = identityService.createGroupQuery().groupName("Famous frogs");
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByInvalidName() {
    GroupQuery query = identityService.createGroupQuery().groupName("invalid");
    verifyQueryResults(query, 0);
    var groupQuery = identityService.createGroupQuery();

    try {
      groupQuery.groupName(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided name is null");
    }
  }

  @Test
  public void testQueryByNameLike() {
    GroupQuery query = identityService.createGroupQuery().groupNameLike("%Famous%");
    verifyQueryResults(query, 2);

    query = identityService.createGroupQuery().groupNameLike("Famous%");
    verifyQueryResults(query, 2);

    query = identityService.createGroupQuery().groupNameLike("%show%");
    verifyQueryResults(query, 1);

    query = identityService.createGroupQuery().groupNameLike("%ters\\_");
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByInvalidNameLike() {
    GroupQuery query = identityService.createGroupQuery().groupNameLike("%invalid%");
    verifyQueryResults(query, 0);
    var groupQuery = identityService.createGroupQuery();

    try {
      groupQuery.groupNameLike(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided nameLike is null");
    }
  }

  @Test
  public void testQueryByType() {
    GroupQuery query = identityService.createGroupQuery().groupType("user");
    verifyQueryResults(query, 3);

    query = identityService.createGroupQuery().groupType("admin");
    verifyQueryResults(query, 0);
  }

  @Test
  public void testQueryByInvalidType() {
    GroupQuery query = identityService.createGroupQuery().groupType("invalid");
    verifyQueryResults(query, 0);
    var groupQuery = identityService.createGroupQuery();

    try {
      groupQuery.groupType(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided type is null");
    }
  }

  @Test
  public void testQueryByMember() {
    GroupQuery query = identityService.createGroupQuery().groupMember("fozzie");
    verifyQueryResults(query, 2);

    query = identityService.createGroupQuery().groupMember("kermit");
    verifyQueryResults(query, 3);

    query = query.orderByGroupId().asc();
    List<Group> groups = query.list();
    assertThat(groups).hasSize(3);
    assertThat(groups.get(0).getId()).isEqualTo("admin");
    assertThat(groups.get(1).getId()).isEqualTo("frogs");
    assertThat(groups.get(2).getId()).isEqualTo("muppets");

    query = query.groupType("user");
    groups = query.list();
    assertThat(groups).hasSize(2);
    assertThat(groups.get(0).getId()).isEqualTo("frogs");
    assertThat(groups.get(1).getId()).isEqualTo("muppets");
  }

  @Test
  public void testQueryByInvalidMember() {
    GroupQuery query = identityService.createGroupQuery().groupMember("invalid");
    verifyQueryResults(query, 0);
    var groupQuery = identityService.createGroupQuery();

    try {
      groupQuery.groupMember(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided userId is null");
    }
  }

  @Test
  public void testQueryByMemberOfTenant() {
    GroupQuery query = identityService.createGroupQuery().memberOfTenant("nonExisting");
    verifyQueryResults(query, 0);

    query = identityService.createGroupQuery().memberOfTenant("tenant");
    verifyQueryResults(query, 1);

    Group group = query.singleResult();
    assertThat(group.getId()).isEqualTo("frogs");
  }

  @Test
  public void testQuerySorting() {
    // asc
    assertThat(identityService.createGroupQuery().orderByGroupId().asc().count()).isEqualTo(4);
    assertThat(identityService.createGroupQuery().orderByGroupName().asc().count()).isEqualTo(4);
    assertThat(identityService.createGroupQuery().orderByGroupType().asc().count()).isEqualTo(4);

    // desc
    assertThat(identityService.createGroupQuery().orderByGroupId().desc().count()).isEqualTo(4);
    assertThat(identityService.createGroupQuery().orderByGroupName().desc().count()).isEqualTo(4);
    assertThat(identityService.createGroupQuery().orderByGroupType().desc().count()).isEqualTo(4);

    // Multiple sortings
    GroupQuery query = identityService.createGroupQuery().orderByGroupType().asc().orderByGroupName().desc();
    List<Group> groups = query.list();
    assertThat(query.count()).isEqualTo(4);

    assertThat(groups.get(0).getType()).isEqualTo("security");
    assertThat(groups.get(1).getType()).isEqualTo("user");
    assertThat(groups.get(2).getType()).isEqualTo("user");
    assertThat(groups.get(3).getType()).isEqualTo("user");

    assertThat(groups.get(0).getId()).isEqualTo("admin");
    assertThat(groups.get(1).getId()).isEqualTo("muppets");
    assertThat(groups.get(2).getId()).isEqualTo("mammals");
    assertThat(groups.get(3).getId()).isEqualTo("frogs");
  }

  @Test
  public void testQueryInvalidSortingUsage() {
    var groupQuery = identityService.createGroupQuery().orderByGroupId().orderByGroupName();
    try {
      groupQuery.list();
      fail("");
    } catch (ProcessEngineException e) {}

    try {
      groupQuery.list();
      fail("");
    } catch (ProcessEngineException e) {}
  }

  private void verifyQueryResults(GroupQuery query, int countExpected) {
    assertThat(query.list()).hasSize(countExpected);
    assertThat(query.count()).isEqualTo(countExpected);

    if (countExpected == 1) {
      assertThat(query.singleResult()).isNotNull();
    } else if (countExpected > 1){
      verifySingleResultFails(query);
    } else if (countExpected == 0) {
      assertThat(query.singleResult()).isNull();
    }
  }

  private void verifySingleResultFails(GroupQuery query) {
    try {
      query.singleResult();
      fail("");
    } catch (ProcessEngineException e) {}
  }

}
