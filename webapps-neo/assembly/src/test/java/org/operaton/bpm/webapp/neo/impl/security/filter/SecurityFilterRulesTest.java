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
package org.operaton.bpm.webapp.neo.impl.security.filter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.webapp.neo.impl.security.auth.Authentications;
import org.operaton.bpm.webapp.neo.impl.security.auth.UserAuthentication;
import org.operaton.bpm.webapp.neo.impl.security.filter.util.FilterRules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Asserts what webapps-neo's real {@code securityFilterRules.json} lets through.
 *
 * <p>The evaluation is fail-open by design: a path that appears in no {@code deniedPaths}
 * entry is granted anonymously. That makes the deny list, not the allow list, the thing that
 * actually protects a namespace — which is why {@code /api/admin/.*} being absent from it left
 * every present and future admin resource unauthenticated. These tests pin both namespaces.</p>
 */
class SecurityFilterRulesTest {

  public static final String FILTER_RULES_FILE = "src/main/webapp/WEB-INF/securityFilterRules.json";

  protected static final String EMPTY_PATH = "";
  protected static final String CUSTOM_APP_PATH = "/my-custom/application/path";

  protected List<SecurityFilterRule> filterRules;
  protected String applicationPath;

  public static Collection<String> data() {
    return List.of(EMPTY_PATH, CUSTOM_APP_PATH);
  }

  void init(String applicationPath) {
    this.applicationPath = applicationPath;
    try (InputStream is = new FileInputStream(FILTER_RULES_FILE)) {
      filterRules = FilterRules.load(is, applicationPath);
    } catch (IOException e) {
      fail("Could not load security filter rules from " + FILTER_RULES_FILE, e);
    }
  }

  @AfterEach
  void clearAuthentication() {
    Authentications.clearCurrent();
  }

  // --- engine namespace -------------------------------------------------------------------

  @MethodSource("data")
  @ParameterizedTest
  void shouldGrantTheEngineListAnonymously(String applicationPath) {
    init(applicationPath);
    assertThat(isAuthorized("GET", applicationPath + "/api/engine/engine/")).isTrue();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldDenyTheEngineApiWithoutAuthentication(String applicationPath) {
    init(applicationPath);
    assertThat(isAuthorized("GET", applicationPath + "/api/engine/engine/default/user")).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldGrantTheEngineApiToAnAuthenticatedUser(String applicationPath) {
    init(applicationPath);
    authenticatedFor("default");
    assertThat(isAuthorized("GET", applicationPath + "/api/engine/engine/default/user")).isTrue();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldDenyTheEngineApiToAUserAuthenticatedForAnotherEngine(String applicationPath) {
    init(applicationPath);
    authenticatedFor("another-engine");
    assertThat(isAuthorized("GET", applicationPath + "/api/engine/engine/default/user")).isFalse();
  }

  // --- admin namespace --------------------------------------------------------------------

  @MethodSource("data")
  @ParameterizedTest
  void shouldGrantTheAuthenticationEndpointsAnonymously(String applicationPath) {
    init(applicationPath);
    // nobody can be signed in yet when these are called
    assertThat(isAuthorized("GET", applicationPath + "/api/admin/auth/user/default")).isTrue();
    assertThat(isAuthorized("POST", applicationPath + "/api/admin/auth/user/default/login/neo")).isTrue();
    assertThat(isAuthorized("POST", applicationPath + "/api/admin/auth/user/default/logout")).isTrue();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldGrantTheSetupEndpointsAnonymously(String applicationPath) {
    init(applicationPath);
    // the resource itself refuses once an administrator exists
    assertThat(isAuthorized("GET", applicationPath + "/api/admin/setup/default")).isTrue();
    assertThat(isAuthorized("POST", applicationPath + "/api/admin/setup/default/user/create")).isTrue();
  }

  /**
   * The regression guard for the deny rule. Without {@code /api/admin/.*} in
   * {@code deniedPaths} this returns true, and any resource added to the admin application
   * later is reachable unauthenticated with no code change and no failing test.
   */
  @MethodSource("data")
  @ParameterizedTest
  void shouldDenyAnyOtherAdminPathWithoutAuthentication(String applicationPath) {
    init(applicationPath);
    assertThat(isAuthorized("GET", applicationPath + "/api/admin/anything-else")).isFalse();
    assertThat(isAuthorized("POST", applicationPath + "/api/admin/setup/default/other")).isFalse();
    assertThat(isAuthorized("GET", applicationPath + "/api/admin/auth/user/default/other")).isFalse();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldGrantOtherAdminAuthPathsToAnAuthenticatedUser(String applicationPath) {
    init(applicationPath);
    authenticatedFor("default");
    assertThat(isAuthorized("GET", applicationPath + "/api/admin/auth/user/default/other")).isTrue();
  }

  // --- helpers ----------------------------------------------------------------------------

  protected boolean isAuthorized(String method, String uri) {
    return FilterRules.authorize(method, uri, filterRules).isGranted();
  }

  protected void authenticatedFor(String engineName) {
    Authentications authentications = new Authentications();
    authentications.addOrReplace(new UserAuthentication("user", engineName));
    Authentications.setCurrent(authentications);
  }
}
