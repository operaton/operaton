/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.scim.util.ScimTestEnvironment;
import org.operaton.bpm.identity.scim.util.ScimTestEnvironmentExtension;

/**
 * Tests for SCIM group queries.
 */
class ScimGroupQueryTest {

  @RegisterExtension
  @Order(1)
  static ScimTestEnvironmentExtension scimExtension = new ScimTestEnvironmentExtension();

  @RegisterExtension
  @Order(2)
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .configurationResource("operaton.cfg.xml")
      .configurator(scimExtension::injectScimUrlWithExternalGroupIds)
      .closeEngineAfterAllTests()
      .build();

  IdentityService identityService;
  ScimTestEnvironment scimTestEnvironment;

  @BeforeEach
  void setup() {
    scimTestEnvironment = scimExtension.getScimTestEnvironment();
  }

  @Test
  public void testCountGroups() {
    // when
    GroupQuery groupQuery = identityService.createGroupQuery();

    // then
    long count = groupQuery.count();
    assertThat(count).isEqualTo(scimTestEnvironment.getTotalNumberOfGroupsCreated());
  }

  @Test
  public void testQueryNoFilter() {
    // when
    List<Group> result = identityService.createGroupQuery().list();

    // then
    assertThat(result).hasSize(scimTestEnvironment.getTotalNumberOfGroupsCreated());
  }

  @Test
  public void testFilterByGroupId() {
    // when
    Group group = identityService.createGroupQuery().groupId("group-development").singleResult();

    // then
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo("group-development");
    assertThat(group.getName()).isEqualTo("development");
  }

  @Test
  public void testFilterByNonexistentGroupId() {
    // when
    Group group = identityService.createGroupQuery().groupId("non-existing").singleResult();

    // then
    assertThat(group).isNull();
  }

  @Test
  public void testFilterByGroupName() {
    // when
    Group group = identityService.createGroupQuery().groupName("management").singleResult();

    // then
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo("group-management");
    assertThat(group.getName()).isEqualTo("management");
  }

  @Test
  public void testFilterGroupsByMemberId() {
    // when
    List<Group> groups = identityService.createGroupQuery().groupMember("oscar").list();

    // then
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).getId()).isEqualTo("group-development");
  }

  @Test
  public void testFilterGroupsByMemberIdMultipleGroups() {
    // when - daniel is member of both groups
    List<Group> groups = identityService.createGroupQuery().groupMember("daniel").list();

    // then
    assertThat(groups).hasSize(2);
    assertThat(groups).extracting("id").containsOnly("group-development", "group-management");
  }

  @Test
  public void testFindGroupById() {
    // when
    Group group = identityService.createGroupQuery().groupId("group-development").singleResult();

    // then
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo("group-development");
    assertThat(group.getName()).isEqualTo("development");
  }

  @Test
  public void testFindGroupByName() {
    // when
    Group group = identityService.createGroupQuery().groupName("development").singleResult();

    // then
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo("group-development");
    assertThat(group.getName()).isEqualTo("development");
  }
}
