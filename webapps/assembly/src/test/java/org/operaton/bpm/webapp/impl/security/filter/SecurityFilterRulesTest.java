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
package org.operaton.bpm.webapp.impl.security.filter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import org.operaton.bpm.cockpit.Cockpit;
import org.operaton.bpm.cockpit.impl.DefaultCockpitRuntimeDelegate;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.webapp.impl.security.auth.Authentication;
import org.operaton.bpm.webapp.impl.security.auth.Authentications;
import org.operaton.bpm.webapp.impl.security.auth.UserAuthentication;
import org.operaton.bpm.webapp.impl.security.filter.util.FilterRules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author nico.rehwaldt
 */
class SecurityFilterRulesTest {

  public static final String FILTER_RULES_FILE = "src/main/webapp/WEB-INF/securityFilterRules.json";

  protected static final String EMPTY_PATH = "";
  protected static final String CUSTOM_APP_PATH = "/my-custom/application/path";

  List<SecurityFilterRule> filterRules;

  public static final Authentication LOGGED_IN_USER = new Authentication("user", "default");

  protected String applicationPath;

  public static Collection<String> data() {
    return List.of(EMPTY_PATH, CUSTOM_APP_PATH);
  }

  public void initSecurityFilterRulesTest(String applicationPath) {
    this.applicationPath = applicationPath;
    try {
      try (InputStream is = new FileInputStream(FILTER_RULES_FILE)) {
        filterRules = FilterRules.load(is, applicationPath);
      }
    } catch (IOException e) {
      fail("Could not load security filter rules from " + FILTER_RULES_FILE, e);
    }

  }

  @BeforeEach
  void createEngine()
  {
    final ProcessEngine engine = Mockito.mock(ProcessEngine.class);

    Cockpit.setCockpitRuntimeDelegate(new DefaultCockpitRuntimeDelegate() {

      @Override
      public ProcessEngine getProcessEngine(String processEngineName) {
        if ("default".equals(processEngineName)) {
          return engine;
        }
        else {
          return null;
        }
      }
    });
  }

  @AfterEach
  void after() {
    Authentications.setCurrent(null);
    Cockpit.setCockpitRuntimeDelegate(null);
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldHaveRulesLoaded(String applicationPath) {
    initSecurityFilterRulesTest(applicationPath);
    assertThat(filterRules).hasSize(1);
  }


  @MethodSource("data")
  @ParameterizedTest
  void shouldPassPasswordPolicy(String applicationPath) {
    initSecurityFilterRulesTest(applicationPath);
    assertThat(isAuthorized("GET",
      applicationPath + "/api/engine/engine/default/identity/password-policy")).isTrue();
    assertThat(isAuthorized("POST",
      applicationPath + "/api/engine/engine/default/identity/password-policy")).isTrue();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassStaticCockpitPluginResources_GET(String applicationPath) {
    initSecurityFilterRulesTest(applicationPath);
    assertThat(isAuthorized("GET",
      applicationPath + "/api/cockpit/plugin/some-plugin/static/foo.html")).isTrue();
    assertThat(isAuthorized("GET",
      applicationPath + "/api/cockpit/plugin/bar/static/foo.html")).isTrue();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldRejectEngineApi_GET(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForEngine("otherEngine", () -> {
      Authorization authorization =
        getAuthorization("POST", applicationPath + "/api/engine/engine/default/bar");

      assertThat(authorization.isGranted()).isFalse();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldGrantEngineApi_GET(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForEngine("default", () -> {
      Authorization authorization =
        getAuthorization("POST", applicationPath + "/api/engine/engine/default/bar");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isTrue();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldRejectCockpitPluginApi_GET(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForEngine("otherEngine", () -> {
      Authorization authorization = getAuthorization("POST",
        applicationPath + "/api/cockpit/plugin/" +
          "reporting-process-count/default/process-instance-count");

      assertThat(authorization.isGranted()).isFalse();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassCockpitPluginApi_GET_LOGGED_IN(String applicationPath) {
    initSecurityFilterRulesTest(applicationPath);
    authenticatedForEngine("default", () -> {
      Authorization authorization =
        getAuthorization("POST",
          applicationPath + "/api/cockpit/plugin/" +
            "reporting-process-count/default/process-instance-count");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isTrue();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassCockpit_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET", applicationPath + "/app/cockpit/non-existing-engine/foo");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassCockpit_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
  authenticatedForApp("default", "cockpit", () -> {
      Authorization authorization =
        getAuthorization("GET", applicationPath + "/app/cockpit/default/");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isTrue();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassCockpitNonExistingEngine_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForApp("default", "cockpit", () -> {
      Authorization authorization =
        getAuthorization("GET", applicationPath + "/app/cockpit/non-existing-engine/");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }


  @MethodSource("data")
  @ParameterizedTest
  void shouldRejectTasklistApi_GET(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);

    authenticatedForEngine("otherEngine", () -> {
      Authorization authorization =
        getAuthorization("POST",
          applicationPath + "/api/tasklist/plugin/example-plugin/default/example-resource");

      assertThat(authorization.isGranted()).isFalse();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassTasklistApi_GET_LOGGED_IN(String applicationPath) {
    initSecurityFilterRulesTest(applicationPath);
    authenticatedForEngine("default", () -> {
      Authorization authorization =
        getAuthorization("POST",
          applicationPath + "/api/tasklist/plugin/example-plugin/default/example-resource");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isTrue();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldRejectTasklistApi_GET_LOGGED_OUT(String applicationPath) {
    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("POST",
        applicationPath + "/api/tasklist/plugin/example-plugin/default/example-resource");

    assertThat(authorization.isGranted()).isFalse();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassTasklistPluginResource_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForEngine("default", () -> {
      Authorization authorization =
        getAuthorization("GET",
          applicationPath + "/api/tasklist/plugin/example-plugin/static/example-resource");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassTasklistPluginResource_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET",
        applicationPath + "/api/tasklist/plugin/example-plugin/static/example-resource");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();
  }


  @MethodSource("data")
  @ParameterizedTest
  void shouldPassTasklist_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET", applicationPath + "/app/tasklist/non-existing-engine");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassTasklist_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForApp("default", "tasklist", () -> {
      Authorization authorization =
        getAuthorization("GET", applicationPath + "/app/tasklist/default/");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isTrue();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldRejectAdminApi_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET", applicationPath + "/api/admin/auth/user/some-engine/");

    assertThat(authorization.isGranted()).isFalse();
    assertThat(authorization.isAuthenticated()).isFalse();

    authorization =
      getAuthorization("GET", applicationPath + "/api/admin/setup/some-engine/");

    assertThat(authorization.isGranted()).isFalse();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdminApi_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForApp("default", "admin", () -> {
      Authorization authorization =
        getAuthorization("GET", applicationPath + "/api/admin/foo/");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdminApi_AnonymousEndpoints_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET", applicationPath + "/api/admin/auth/user/bar");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();

    authorization =
      getAuthorization("POST", applicationPath + "/api/admin/auth/user/bar/logout");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();

    authorization =
      getAuthorization("POST", applicationPath + "/api/admin/auth/user/bar/login/some-app");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();

    authorization =
      getAuthorization("POST", applicationPath + "/api/admin/setup/some-engine/user/create");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();
  }


  @MethodSource("data")
  @ParameterizedTest
  void shouldRejectAdminApiPlugin_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET",
        applicationPath + "/api/admin/plugin/adminPlugins/some-engine/endpoint");

    assertThat(authorization.isGranted()).isFalse();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdminApiPlugin_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForApp("default", "admin", () -> {
      Authorization authorization =
        getAuthorization("GET",
          applicationPath + "/api/admin/plugin/adminPlugins/some-engine");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdmin_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET", applicationPath + "/app/admin/default");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdmin_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    authenticatedForApp("default", "admin", () -> {
      Authorization authorization =
        getAuthorization("GET", applicationPath + "/app/admin/default/");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isTrue();
    });
  }


  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdminResources_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET", applicationPath + "/app/admin/scripts");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdminResources_GET_LOGGED_IN(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
  authenticatedForApp("default", "admin", () -> {
      Authorization authorization =
        getAuthorization("GET", applicationPath + "/app/admin/scripts");

      assertThat(authorization.isGranted()).isTrue();
      assertThat(authorization.isAuthenticated()).isFalse();
    });
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldPassAdminLicenseCheck_GET_LOGGED_OUT(String applicationPath) {

    initSecurityFilterRulesTest(applicationPath);
    Authorization authorization =
      getAuthorization("GET", applicationPath + "/api/admin/plugin/license/default/check-key");

    assertThat(authorization.isGranted()).isTrue();
    assertThat(authorization.isAuthenticated()).isFalse();
  }

  protected Authorization getAuthorization(String method, String uri) {
    return FilterRules.authorize(method, uri, filterRules);
  }

  protected boolean isAuthorized(String method, String uri) {
    return getAuthorization(method, uri).isGranted();
  }

  private void authenticatedForEngine(String engineName, Runnable codeBlock) {
    UserAuthentication engineAuth = new UserAuthentication(LOGGED_IN_USER.getIdentityId(), engineName);

    Authentications authentications = new Authentications();
    authentications.addOrReplace(engineAuth);

    Authentications.setCurrent(authentications);

    try {
      codeBlock.run();
    } finally {
      Authentications.clearCurrent();
    }
  }

  private void authenticatedForApp(String engineName, String appName, Runnable codeBlock) {
    HashSet<String> authorizedApps = new HashSet<>(List.of(appName));

    UserAuthentication engineAuth = new UserAuthentication(LOGGED_IN_USER.getIdentityId(), engineName);
    engineAuth.setGroupIds(Collections. emptyList());
    engineAuth.setTenantIds(Collections. emptyList());
    engineAuth.setAuthorizedApps(authorizedApps);

    Authentications authentications = new Authentications();
    authentications.addOrReplace(engineAuth);

    Authentications.setCurrent(authentications);

    try {
      codeBlock.run();
    } finally {
      Authentications.clearCurrent();
    }
  }
}
