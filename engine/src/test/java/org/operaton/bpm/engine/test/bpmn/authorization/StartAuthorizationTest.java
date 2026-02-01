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
package org.operaton.bpm.engine.test.bpmn.authorization;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


/**
 * @author Saeid Mirzaei
 * @author Tijs Rademakers
 */
class StartAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  IdentityService identityService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;

  User userInGroup1;
  User userInGroup2;
  User userInGroup3;

  Group group1;
  Group group2;
  Group group3;

  protected void setUpUsersAndGroups() {

    identityService = processEngine.getIdentityService();

    identityService.saveUser(identityService.newUser("user1"));
    identityService.saveUser(identityService.newUser("user2"));
    identityService.saveUser(identityService.newUser("user3"));

    // create users
    userInGroup1 = identityService.newUser("userInGroup1");
    identityService.saveUser(userInGroup1);

    userInGroup2 = identityService.newUser("userInGroup2");
    identityService.saveUser(userInGroup2);

    userInGroup3 = identityService.newUser("userInGroup3");
    identityService.saveUser(userInGroup3);

    // create groups
    group1 = identityService.newGroup("group1");
    identityService.saveGroup(group1);

    group2 = identityService.newGroup("group2");
    identityService.saveGroup(group2);

    group3 = identityService.newGroup("group3");
    identityService.saveGroup(group3);

    // relate users to groups
    identityService.createMembership(userInGroup1.getId(), group1.getId());
    identityService.createMembership(userInGroup2.getId(), group2.getId());
    identityService.createMembership(userInGroup3.getId(), group3.getId());
  }

  protected void tearDownUsersAndGroups() {
    identityService.deleteMembership(userInGroup1.getId(), group1.getId());
    identityService.deleteMembership(userInGroup2.getId(), group2.getId());
    identityService.deleteMembership(userInGroup3.getId(), group3.getId());

    identityService.deleteGroup(group1.getId());
    identityService.deleteGroup(group2.getId());
    identityService.deleteGroup(group3.getId());

    identityService.deleteUser(userInGroup1.getId());
    identityService.deleteUser(userInGroup2.getId());
    identityService.deleteUser(userInGroup3.getId());

    identityService.deleteUser("user1");
    identityService.deleteUser("user2");
    identityService.deleteUser("user3");
  }

  @Deployment
  @Test
  void testIdentityLinks() {

    setUpUsersAndGroups();

    try {
      ProcessDefinition latestProcessDef = repositoryService
          .createProcessDefinitionQuery().processDefinitionKey("process1")
          .singleResult();
      assertThat(latestProcessDef).isNotNull();
      List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).isEmpty();

      latestProcessDef = repositoryService
          .createProcessDefinitionQuery().processDefinitionKey("process2")
          .singleResult();
      assertThat(latestProcessDef).isNotNull();
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).hasSize(2);
      assertThat(containsUserOrGroup("user1", null, links)).isTrue();
      assertThat(containsUserOrGroup("user2", null, links)).isTrue();

      latestProcessDef = repositoryService
          .createProcessDefinitionQuery().processDefinitionKey("process3")
          .singleResult();
      assertThat(latestProcessDef).isNotNull();
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).hasSize(1);
      assertThat(links.get(0).getUserId()).isEqualTo("user1");

      latestProcessDef = repositoryService
          .createProcessDefinitionQuery().processDefinitionKey("process4")
          .singleResult();
      assertThat(latestProcessDef).isNotNull();
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).hasSize(4);
      assertThat(containsUserOrGroup("userInGroup2", null, links)).isTrue();
      assertThat(containsUserOrGroup(null, "group1", links)).isTrue();
      assertThat(containsUserOrGroup(null, "group2", links)).isTrue();
      assertThat(containsUserOrGroup(null, "group3", links)).isTrue();

    } finally {
      tearDownUsersAndGroups();
    }
  }

  @Deployment
  @Test
  void testAddAndRemoveIdentityLinks() {

    setUpUsersAndGroups();

    try {
      ProcessDefinition latestProcessDef = repositoryService
          .createProcessDefinitionQuery().processDefinitionKey("potentialStarterNoDefinition")
          .singleResult();
      assertThat(latestProcessDef).isNotNull();
      List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).isEmpty();

      repositoryService.addCandidateStarterGroup(latestProcessDef.getId(), "group1");
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).hasSize(1);
      assertThat(links.get(0).getGroupId()).isEqualTo("group1");

      repositoryService.addCandidateStarterUser(latestProcessDef.getId(), "user1");
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).hasSize(2);
      assertThat(containsUserOrGroup(null, "group1", links)).isTrue();
      assertThat(containsUserOrGroup("user1", null, links)).isTrue();

      repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), "nonexisting");
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).hasSize(2);

      repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), "group1");
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).hasSize(1);
      assertThat(links.get(0).getUserId()).isEqualTo("user1");

      repositoryService.deleteCandidateStarterUser(latestProcessDef.getId(), "user1");
      links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
      assertThat(links).isEmpty();

    } finally {
      tearDownUsersAndGroups();
    }
  }

  private boolean containsUserOrGroup(String userId, String groupId, List<IdentityLink> links) {
    return links.stream().anyMatch(identityLink ->
      Objects.equals(userId, identityLink.getUserId()) ||
      Objects.equals(groupId, identityLink.getGroupId())
    );
  }

  @Deployment
  @Test
  void testPotentialStarter() {
    // first check an unauthorized user. An exception is expected

    setUpUsersAndGroups();

    try {

	    // Authentication should not be done. So an unidentified user should also be able to start the process
	    identityService.setAuthenticatedUserId("unauthorizedUser");
	    assertThatCode(() -> runtimeService.startProcessInstanceByKey("potentialStarter"))
        .doesNotThrowAnyException();

	    // check with an authorized user obviously it should be no problem starting the process
	    identityService.setAuthenticatedUserId("user1");
	    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("potentialStarter");
	    testRule.assertProcessEnded(processInstance.getId());
      assertThat(processInstance.isEnded()).isTrue();
    } finally {

      tearDownUsersAndGroups();
    }
  }

  /*
   * if there is no security definition, then user authorization check is not
   * done. This ensures backward compatibility
   */
  @Deployment
  @Test
  void testPotentialStarterNoDefinition() {
    identityService = processEngine.getIdentityService();

    identityService.setAuthenticatedUserId("someOneFromMars");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("potentialStarterNoDefinition");
    assertThat(processInstance.getId()).isNotNull();
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(processInstance.isEnded()).isTrue();
  }

  // this test checks the list without user constraint
  @Deployment
  @Test
  void testProcessDefinitionList() {

    setUpUsersAndGroups();
    try {

      // Process 1 has no potential starters
      ProcessDefinition latestProcessDef = repositoryService
              .createProcessDefinitionQuery().processDefinitionKey("process1")
              .singleResult();
      List<User> authorizedUsers = identityService.createUserQuery().potentialStarter(latestProcessDef.getId()).list();
      assertThat(authorizedUsers).isEmpty();

      // user1 and user2 are potential Startes of Process2
      latestProcessDef = repositoryService
              .createProcessDefinitionQuery().processDefinitionKey("process2")
              .singleResult();
      authorizedUsers =  identityService.createUserQuery().potentialStarter(latestProcessDef.getId()).orderByUserId().asc().list();
      assertThat(authorizedUsers).hasSize(2);
      assertThat(authorizedUsers.get(0).getId()).isEqualTo("user1");
      assertThat(authorizedUsers.get(1).getId()).isEqualTo("user2");

      // Process 2 has no potential starter groups
      latestProcessDef = repositoryService
              .createProcessDefinitionQuery().processDefinitionKey("process2")
              .singleResult();
      List<Group> authorizedGroups = identityService.createGroupQuery().potentialStarter(latestProcessDef.getId()).list();
      assertThat(authorizedGroups).isEmpty();

      // Process 3 has 3 groups as authorized starter groups
      latestProcessDef = repositoryService
              .createProcessDefinitionQuery().processDefinitionKey("process4")
              .singleResult();
      authorizedGroups = identityService.createGroupQuery().potentialStarter(latestProcessDef.getId()).orderByGroupId().asc().list();
      assertThat(authorizedGroups).hasSize(3);
      assertThat(authorizedGroups.get(0).getId()).isEqualTo("group1");
      assertThat(authorizedGroups.get(1).getId()).isEqualTo("group2");
      assertThat(authorizedGroups.get(2).getId()).isEqualTo("group3");

      // do not mention user, all processes should be selected
      List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
          .orderByProcessDefinitionName().asc().list();

      assertThat(processDefinitions).hasSize(4);

      assertThat(processDefinitions.get(0).getKey()).isEqualTo("process1");
      assertThat(processDefinitions.get(1).getKey()).isEqualTo("process2");
      assertThat(processDefinitions.get(2).getKey()).isEqualTo("process3");
      assertThat(processDefinitions.get(3).getKey()).isEqualTo("process4");

      // check user1, process3 has "user1" as only authorized starter, and
      // process2 has two authorized starters, of which one is "user1"
      processDefinitions = repositoryService.createProcessDefinitionQuery()
          .orderByProcessDefinitionName().asc().startableByUser("user1").list();

      assertThat(processDefinitions).hasSize(2);
      assertThat(processDefinitions.get(0).getKey()).isEqualTo("process2");
      assertThat(processDefinitions.get(1).getKey()).isEqualTo("process3");


      // "user2" can only start process2
      processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("user2").list();

      assertThat(processDefinitions).hasSize(1);
      assertThat(processDefinitions.get(0).getKey()).isEqualTo("process2");

      // no process could be started with "user4"
      processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("user4").list();
      assertThat(processDefinitions).isEmpty();

      // "userInGroup3" is in "group3" and can start only process4 via group authorization
      processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("userInGroup3").list();
      assertThat(processDefinitions).hasSize(1);
      assertThat(processDefinitions.get(0).getKey()).isEqualTo("process4");

      // "userInGroup2" can start process4, via both user and group authorizations
      // but we have to be sure that process4 appears only once
      processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("userInGroup2").list();
      assertThat(processDefinitions).hasSize(1);
      assertThat(processDefinitions.get(0).getKey()).isEqualTo("process4");

    } finally {
      tearDownUsersAndGroups();
    }
  }

}
