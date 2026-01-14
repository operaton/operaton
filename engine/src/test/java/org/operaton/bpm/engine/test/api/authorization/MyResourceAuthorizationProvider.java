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
package org.operaton.bpm.engine.test.api.authorization;

import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.auth.ResourceAuthorizationProvider;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;

/**
 * @author Roman Smirnov
 *
 */
public class MyResourceAuthorizationProvider implements ResourceAuthorizationProvider {
  private static final AuthorizationEntity[] NO_AUTHORIZATIONS = new AuthorizationEntity[0];

  // assignee
  public static String oldAssignee;
  public static String newAssignee;

  // owner
  public static String oldOwner;
  public static String newOwner;

  // add user identity link
  public static String addUserIdentityLinkType;
  public static String addUserIdentityLinkUser;

  // delete user identity link
  public static String deleteUserIdentityLinkType;
  public static String deleteUserIdentityLinkUser;

  // add group identity link
  public static String addGroupIdentityLinkType;
  public static String addGroupIdentityLinkGroup;

  // delete group identity link
  public static String deleteGroupIdentityLinkType;
  public static String deleteGroupIdentityLinkGroup;

  @Override
  public AuthorizationEntity[] newUser(User user) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newGroup(Group group) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newTenant(Tenant tenant) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] groupMembershipCreated(String groupId, String userId) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] tenantMembershipCreated(Tenant tenant, User user) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] tenantMembershipCreated(Tenant tenant, Group group) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newFilter(Filter filter) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newDeployment(Deployment deployment) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newProcessDefinition(ProcessDefinition processDefinition) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newProcessInstance(ProcessInstance processInstance) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newTask(Task task) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newTaskAssignee(Task task, String oldAssignee, String newAssignee) {
    MyResourceAuthorizationProvider.oldAssignee = oldAssignee;
    MyResourceAuthorizationProvider.newAssignee = newAssignee;
    return null;
  }

  @Override
  public AuthorizationEntity[] newTaskOwner(Task task, String oldOwner, String newOwner) {
    MyResourceAuthorizationProvider.oldOwner = oldOwner;
    MyResourceAuthorizationProvider.newOwner = newOwner;
    return null;
  }

  @Override
  public AuthorizationEntity[] newTaskUserIdentityLink(Task task, String userId, String type) {
    addUserIdentityLinkType = type;
    addUserIdentityLinkUser = userId;
    return null;
  }

  @Override
  public AuthorizationEntity[] newTaskGroupIdentityLink(Task task, String groupId, String type) {
    addGroupIdentityLinkType = type;
    addGroupIdentityLinkGroup = groupId;
    return null;
  }

  @Override
  public AuthorizationEntity[] deleteTaskUserIdentityLink(Task task, String userId, String type) {
    deleteUserIdentityLinkType = type;
    deleteUserIdentityLinkUser = userId;
    return null;
  }

  @Override
  public AuthorizationEntity[] deleteTaskGroupIdentityLink(Task task, String groupId, String type) {
    deleteGroupIdentityLinkType = type;
    deleteGroupIdentityLinkGroup = groupId;
    return null;
  }

  public static void clearProperties() {
    oldAssignee = null;
    newAssignee = null;
    oldOwner = null;
    newOwner = null;
    addUserIdentityLinkType = null;
    addUserIdentityLinkUser = null;
    addGroupIdentityLinkType = null;
    addGroupIdentityLinkGroup = null;
    deleteUserIdentityLinkType = null;
    deleteUserIdentityLinkUser = null;
    deleteGroupIdentityLinkType = null;
    deleteGroupIdentityLinkGroup = null;
  }

  @Override
  public AuthorizationEntity[] newDecisionDefinition(DecisionDefinition decisionDefinition) {
    return NO_AUTHORIZATIONS;
  }

  @Override
  public AuthorizationEntity[] newDecisionRequirementsDefinition(DecisionRequirementsDefinition decisionRequirementsDefinition) {
    return NO_AUTHORIZATIONS;
  }

}
