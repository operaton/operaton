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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAuthenticationResourceLoggingTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().configurationResource("operaton-test-engine.cfg.xml").build();
  @RegisterExtension
  public ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension().watch("org.operaton.bpm.webapp")
      .level(Level.INFO);

  ProcessEngine processEngine;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  IdentityService identityService;
  AuthorizationService authorizationService;

  protected boolean authorizationEnabledInitialValue;
  protected boolean webappsAuthenticationLoggingEnabledInitialValue;

  @BeforeEach
  void setUp() {
    authorizationEnabledInitialValue = processEngineConfiguration.isAuthorizationEnabled();
    webappsAuthenticationLoggingEnabledInitialValue = processEngineConfiguration.isWebappsAuthenticationLoggingEnabled();
  }

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
    processEngineConfiguration.setAuthorizationEnabled(authorizationEnabledInitialValue);
    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(webappsAuthenticationLoggingEnabledInitialValue);

    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }

    Authentications.clearCurrent();
  }

  @Test
  void shouldProduceLogStatementOnValidLogin() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(true);

    // when
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).hasSize(1);
    assertThat(filteredLog.get(0).getFormattedMessage()).contains("Successful login for user jonny");
  }

  @Test
  void shouldNotProduceLogStatementOnValidLoginWhenDisabled() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(false);

    // when
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).isEmpty();
  }

  @Test
  void shouldProduceLogStatementOnInvalidLogin() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(true);

    // when
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "NOT_jonnyspassword");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).hasSize(1);
    assertThat(filteredLog.get(0).getFormattedMessage()).contains("Failed login attempt for user jonny. Reason: bad credentials");
  }

  @Test
  void shouldNotProduceLogStatementOnInvalidLoginWhenDisabled() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(false);

    // when
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "NOT_jonnyspassword");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).isEmpty();
  }

  @Test
  void shouldProduceLogStatementOnLogout() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);
    setAuthentication("jonny", "webapps-test-engine");

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(true);

    // when
    authResource.doLogout("webapps-test-engine");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).hasSize(1);
    assertThat(filteredLog.get(0).getFormattedMessage()).contains("Successful logout for user jonny");
  }

  @Test
  void shouldNotProduceLogStatementOnLogoutWhenDisabled() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);
    setAuthentication("jonny", "webapps-test-engine");

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(false);

    // when
    authResource.doLogout("webapps-test-engine");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).isEmpty();
  }

  @Test
  void shouldNotProduceLogStatementOnLogoutWhenNoAuthentication() {
    // given
    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(true);

    // when
    authResource.doLogout("webapps-test-engine");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).isEmpty();
  }

  @Test
  void shouldProduceLogStatementOnLoginWhenAuthorized() {
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
    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(true);

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    // when
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).hasSize(1);
    assertThat(filteredLog.get(0).getFormattedMessage()).contains("Successful login for user jonny");
  }

  @Test
  void shouldProduceLogStatementOnLoginWhenNotAuthorized() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    processEngineConfiguration.setAuthorizationEnabled(true);
    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(true);

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    // when
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).hasSize(1);
    assertThat(filteredLog.get(0).getFormattedMessage()).contains("Failed login attempt for user jonny. Reason: not authorized");
  }

  @Test
  void shouldNotProduceLogStatementOnLoginWhenNotAuthorizedAndWebappsLoggingDisabled() {
    // given
    User jonny = identityService.newUser("jonny");
    jonny.setPassword("jonnyspassword");
    identityService.saveUser(jonny);

    processEngineConfiguration.setAuthorizationEnabled(true);
    processEngineConfiguration.setWebappsAuthenticationLoggingEnabled(false);

    UserAuthenticationResource authResource = new UserAuthenticationResource();
    authResource.request = new MockHttpServletRequest();

    // when
    authResource.doLogin("webapps-test-engine", "tasklist", "jonny", "jonnyspassword");

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("jonny");
    assertThat(filteredLog).isEmpty();
  }

  protected void setAuthentication(String user, String engineName) {
    Authentications authentications = new Authentications();
    authentications.addOrReplace(new UserAuthentication(user, engineName));
    Authentications.setCurrent(authentications);
  }

}
