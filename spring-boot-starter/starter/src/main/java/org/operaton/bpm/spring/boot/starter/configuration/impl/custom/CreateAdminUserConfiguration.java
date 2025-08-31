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
package org.operaton.bpm.spring.boot.starter.configuration.impl.custom;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.spring.boot.starter.configuration.impl.AbstractOperatonConfiguration;
import org.operaton.bpm.spring.boot.starter.property.AdminUserProperty;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Groups.OPERATON_ADMIN;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;

import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.springframework.beans.BeanUtils;

import static java.util.Objects.requireNonNull;

public class CreateAdminUserConfiguration extends AbstractOperatonConfiguration {

  private User adminUser;

  public CreateAdminUserConfiguration(OperatonBpmProperties operatonBpmProperties) {
    super(operatonBpmProperties);
  }

  @PostConstruct
  void init() {
    adminUser = Optional.ofNullable(operatonBpmProperties.getAdminUser())
      .map(AdminUserProperty::init)
      .orElseThrow(fail("adminUser not configured!"));
  }

  @Override
  public void postProcessEngineBuild(final ProcessEngine processEngine) {
    requireNonNull(adminUser, "adminUser not configured!");

    final IdentityService identityService = processEngine.getIdentityService();
    final AuthorizationService authorizationService = processEngine.getAuthorizationService();

    if (userAlreadyExists(identityService, adminUser)) {
      return;
    }

    createUser(identityService, adminUser);

    // create group
    if (identityService.createGroupQuery().groupId(OPERATON_ADMIN).count() == 0) {
      Group operatonAdminGroup = identityService.newGroup(OPERATON_ADMIN);
      operatonAdminGroup.setName("operaton BPM Administrators");
      operatonAdminGroup.setType(Groups.GROUP_TYPE_SYSTEM);
      identityService.saveGroup(operatonAdminGroup);
    }

    // create ADMIN authorizations on all built-in resources
    for (Resource resource : Resources.values()) {
      if (authorizationService.createAuthorizationQuery().groupIdIn(OPERATON_ADMIN).resourceType(resource).resourceId(ANY).count() == 0) {
        AuthorizationEntity userAdminAuth = new AuthorizationEntity(AUTH_TYPE_GRANT);
        userAdminAuth.setGroupId(OPERATON_ADMIN);
        userAdminAuth.setResource(resource);
        userAdminAuth.setResourceId(ANY);
        userAdminAuth.addPermission(ALL);
        authorizationService.saveAuthorization(userAdminAuth);
      }
    }

    identityService.createMembership(adminUser.getId(), OPERATON_ADMIN);
    LOG.creatingInitialAdminUser(adminUser);
  }

  static boolean userAlreadyExists(IdentityService identityService, User adminUser) {
    final User existingUser = identityService.createUserQuery()
      .userId(adminUser.getId())
      .singleResult();
    if (existingUser != null) {
      LOG.skipAdminUserCreation(existingUser);
      return true;
    }
    return false;
  }

  static User createUser(final IdentityService identityService, final User adminUser) {
    User newUser = identityService.newUser(adminUser.getId());
    BeanUtils.copyProperties(adminUser, newUser);
    identityService.saveUser(newUser);
    return newUser;
  }

}
