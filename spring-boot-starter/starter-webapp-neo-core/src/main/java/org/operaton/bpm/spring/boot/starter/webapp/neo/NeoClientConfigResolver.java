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
package org.operaton.bpm.spring.boot.starter.webapp.neo;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import org.operaton.bpm.spring.boot.starter.property.NeoWebappProperty;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

/**
 * Resolves the {@link NeoClientConfig} served to the SPA.
 *
 * <p>The authentication mode defaults to {@link NeoWebappProperty#AUTH_MODE_AUTO},
 * which means: use OAuth2 when the application has Spring Security OAuth2 client
 * registrations configured, and basic authentication otherwise. So a user who
 * integrates an identity provider gets SSO without touching a second switch, and
 * everyone else gets a username and password form.</p>
 *
 * <p>Client registrations are read straight from the {@link Environment} rather
 * than from a {@code ClientRegistrationRepository} bean, so that this module needs
 * no dependency on Spring Security — it is optional on the classpath. This mirrors
 * {@code ClientsNotConfiguredCondition} in the security starter.</p>
 *
 * @since 2.2.0
 */
public class NeoClientConfigResolver {

  protected static final String CLIENT_REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

  protected static final String OAUTH2_LOGIN_PATH = "/oauth2/authorization/";
  protected static final String OAUTH2_LOGOUT_PATH = "/logout";

  /** The SPA talks to the application it is served from. */
  protected static final String EMBEDDED_BACKEND_NAME = "Operaton";
  protected static final String EMBEDDED_BACKEND_URL = "";

  protected final OperatonBpmProperties properties;
  protected final Environment environment;

  public NeoClientConfigResolver(OperatonBpmProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
  }

  /**
   * @param contextPath the servlet context path, so the login and logout targets
   *                    stay correct when the application is not deployed at the root
   * @param principal   the authenticated principal, or {@code null} when there is no session
   */
  public NeoClientConfig resolve(String contextPath, Principal principal) {
    NeoWebappProperty neo = properties.getWebapp().getNeo();
    String authMode = resolveAuthMode(neo);

    return new NeoClientConfig(
      List.of(new NeoClientConfig.Backend(EMBEDDED_BACKEND_NAME, EMBEDDED_BACKEND_URL)),
      authMode,
      NeoWebappProperty.AUTH_MODE_OAUTH2.equals(authMode) ? resolveOAuth(contextPath) : null,
      neo.getPluginsUrl().isEmpty() ? null : neo.getPluginsUrl(),
      neo.isHideReleaseWarning(),
      resolveUser(principal));
  }

  protected String resolveAuthMode(NeoWebappProperty neo) {
    String configured = neo.getAuthMode();
    if (!NeoWebappProperty.AUTH_MODE_AUTO.equals(configured)) {
      return configured;
    }
    return firstClientRegistrationId().isPresent()
      ? NeoWebappProperty.AUTH_MODE_OAUTH2
      : NeoWebappProperty.AUTH_MODE_BASIC;
  }

  protected NeoClientConfig.OAuth resolveOAuth(String contextPath) {
    String prefix = contextPath == null ? "" : contextPath;
    // Without a registration the SPA cannot build the initiation URL itself, so
    // fall back to the Spring Security generated login page, which lists them.
    String login = firstClientRegistrationId()
      .map(id -> prefix + OAUTH2_LOGIN_PATH + id)
      .orElse(prefix + "/login");
    return new NeoClientConfig.OAuth("session", login, prefix + OAUTH2_LOGOUT_PATH);
  }

  protected NeoClientConfig.User resolveUser(Principal principal) {
    return principal == null ? null : new NeoClientConfig.User(principal.getName());
  }

  /**
   * The id of the first configured OAuth2 client registration, empty when none are
   * configured. With several registrations the SPA sends the user to the first one;
   * pick explicitly by setting {@code operaton.bpm.webapp.neo.auth-mode} and letting
   * Spring Security's login page do the choosing.
   */
  protected Optional<String> firstClientRegistrationId() {
    Map<?, ?> registrations = Binder.get(environment)
      .bind(CLIENT_REGISTRATION_PREFIX, Map.class)
      .orElseGet(Map::of);

    return registrations.keySet().stream()
      .findFirst()
      .map(String::valueOf);
  }
}
