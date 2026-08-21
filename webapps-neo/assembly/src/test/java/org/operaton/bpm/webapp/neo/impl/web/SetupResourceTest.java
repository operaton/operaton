/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.webapp.neo.impl.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserDto;
import org.operaton.bpm.engine.rest.dto.identity.UserProfileDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * The first-run setup resource, ported into webapps-neo so a deployment with no administrator
 * can bootstrap one. The endpoints answer without authentication by necessity, so the guard
 * that closes them once an admin exists is the security-relevant part and is covered here.
 */
class SetupResourceTest {

  protected static final String ENGINE_NAME = "webapps-test-engine";

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder()
      .configurationResource("operaton-test-engine.cfg.xml").build();

  ProcessEngineConfiguration processEngineConfiguration;
  IdentityService identityService;
  AuthorizationService authorizationService;

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);

    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  void shouldReportSetupAvailableWhileNoAdministratorExists() throws Exception {
    // given an empty identity store

    // when
    var availability = new SetupResource().isSetupAvailable(ENGINE_NAME);

    // then
    assertThat(availability.setupAvailable()).isTrue();
  }

  @Test
  void shouldCreateTheInitialAdministrator() throws Exception {
    // given
    SetupResource resource = new SetupResource();

    // when
    resource.createInitialUser(ENGINE_NAME, userDto("kermit", "kermitspassword"));

    // then
    assertThat(identityService.createUserQuery().userId("kermit").count()).isEqualTo(1);
    assertThat(identityService.createUserQuery().memberOfGroup(Groups.OPERATON_ADMIN).count()).isEqualTo(1);
    assertThat(authorizationService.createAuthorizationQuery()
        .groupIdIn(Groups.OPERATON_ADMIN)
        .resourceType(Resources.APPLICATION)
        .resourceId(Authorization.ANY)
        .count()).isEqualTo(1);
  }

  @Test
  void shouldReportSetupUnavailableOnceAnAdministratorExists() throws Exception {
    // given
    SetupResource resource = new SetupResource();
    resource.createInitialUser(ENGINE_NAME, userDto("kermit", "kermitspassword"));

    // when
    var availability = resource.isSetupAvailable(ENGINE_NAME);

    // then
    assertThat(availability.setupAvailable()).isFalse();
  }

  /**
   * The endpoint is anonymous, so this is what stops anyone from minting themselves an
   * administrator on an already-configured deployment.
   */
  @Test
  void shouldRejectASecondInitialUser() throws Exception {
    // given
    SetupResource resource = new SetupResource();
    resource.createInitialUser(ENGINE_NAME, userDto("kermit", "kermitspassword"));

    // when
    InvalidRequestException exception = catchThrowableOfType(InvalidRequestException.class,
        () -> resource.createInitialUser(ENGINE_NAME, userDto("gonzo", "gonzospassword")));

    // then
    assertThat(exception).isNotNull();
    assertThat(exception.getStatus().getStatusCode()).isEqualTo(403);
    assertThat(identityService.createUserQuery().userId("gonzo").count()).isZero();
  }

  protected UserDto userDto(String id, String password) {
    UserProfileDto profile = new UserProfileDto();
    profile.setId(id);
    profile.setFirstName("First");
    profile.setLastName("Last");
    profile.setEmail(id + "@localhost");

    UserCredentialsDto credentials = new UserCredentialsDto();
    credentials.setPassword(password);

    UserDto user = new UserDto();
    user.setProfile(profile);
    user.setCredentials(credentials);
    return user;
  }
}
