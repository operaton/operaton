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
package org.operaton.bpm.webapp.impl.security.auth;

import java.util.Date;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.webapp.impl.util.ServletContextUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * @author Thorben Lindhauer
 *
 */
class UserAuthenticationResourceTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().configurationResource("operaton-test-engine.cfg.xml").build();

  ProcessEngineConfiguration processEngineConfiguration;
  IdentityService identityService;
  AuthorizationService authorizationService;

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
    processEngineConfiguration.setAuthorizationEnabled(false);

    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }

    clearAuthentication();
  }

  @Test
  void authorizationCheckGranted() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setResource(Resources.APPLICATION);
    authorization.setResourceId("tasklist");
    authorization.setPermissions(new Permissions[] {Permissions.ACCESS});
    authorization.setUserId(jonny.getId());
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    setAuthentication("jonny", "webapps-test-engine");

    // when
    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();
    Response response = authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
  }

  @Test
  void sessionRevalidationOnAuthorization() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setResource(Resources.APPLICATION);
    authorization.setResourceId("tasklist");
    authorization.setPermissions(new Permissions[] {Permissions.ACCESS});
    authorization.setUserId(jonny.getId());
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    setAuthentication("jonny", "webapps-test-engine");

    // when
    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();
    String oldSessionId = authResource.request.getSession().getId();

    // first login session
    Response response = authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");
    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    String newSessionId = authResource.request.getSession().getId();

    authResource.doLogout("webapps-test-engine");

    // second login session
    response = authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");
    String newestSessionId = authResource.request.getSession().getId();

    // then
    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    assertThat(newSessionId).isNotEqualTo(oldSessionId);
    assertThat(newestSessionId).isNotEqualTo(newSessionId);
  }

  @Test
  void authorizationCheckNotGranted() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    processEngineConfiguration.setAuthorizationEnabled(true);
    setAuthentication("jonny", "webapps-test-engine");

    // when
    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();
    Response response = authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    assertThat(response.getStatus()).isEqualTo(Status.FORBIDDEN.getStatusCode());
  }

  @Test
  void authorizationCheckDeactivated() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    processEngineConfiguration.setAuthorizationEnabled(false);
    setAuthentication("jonny", "webapps-test-engine");

    // when
    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();
    Response response = authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
  }

  @Test
  void shouldSetAuthCacheValidationTime() {
    // given
    ClockUtil.setCurrentTime(ClockUtil.getCurrentTime());
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    MockHttpServletRequest request = new MockHttpServletRequest();
    ServletContextUtil.setCacheTTLForLogin(1000 * 60 * 5, request.getServletContext());

    // when
    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = request;
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    UserAuthentication userAuthentication = AuthenticationUtil.getAuthsFromSession(request.getSession())
      .getAuthentications()
      .get(0);
    assertThat(userAuthentication.getCacheValidationTime())
      .isEqualTo(new Date(ClockUtil.getCurrentTime().getTime() + 1000 * 60 * 5));
  }

  @Test
  void shouldReturnUnauthorizedOnNullAuthentication() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);
    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    try (MockedStatic<AuthenticationUtil> authenticationUtilMock = mockStatic(AuthenticationUtil.class)) {
      authenticationUtilMock.when(() -> AuthenticationUtil.createAuthentication("webapps-test-engine", "jonny")).thenReturn(null);

      // when
      Response response = authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

      // then
      assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }
  }

  protected void setAuthentication(String user, String engineName) {
    Authentications authentications = new Authentications();
    authentications.addOrReplace(new UserAuthentication(user, engineName));
    Authentications.setCurrent(authentications);
  }

  protected void clearAuthentication() {
    Authentications.clearCurrent();
  }


}
