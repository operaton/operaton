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

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import org.operaton.bpm.spring.boot.starter.property.NeoWebappProperty;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The web apps default to a username and password form and pick up single
 * sign-on by themselves once an identity provider is configured, so that
 * integrating one needs no second switch and no frontend rebuild.
 */
class NeoClientConfigResolverTest {

  protected static final String REGISTRATION =
      "spring.security.oauth2.client.registration.operaton.client-id";

  protected OperatonBpmProperties properties = new OperatonBpmProperties();
  protected MockEnvironment environment = new MockEnvironment();

  protected NeoWebappProperty neo() {
    return properties.getWebapp().getNeo();
  }

  protected NeoClientConfig resolve() {
    return new NeoClientConfigResolver(properties, environment).resolve("", null);
  }

  @Test
  void shouldDefaultToBasicAuthentication() {
    assertThat(resolve().authMode()).isEqualTo("basic");
    assertThat(resolve().oauth()).isNull();
  }

  @Test
  void shouldSwitchToOAuth2WhenAClientIsRegistered() {
    environment.setProperty(REGISTRATION, "operaton");

    NeoClientConfig config = resolve();

    assertThat(config.authMode()).isEqualTo("oauth2");
    assertThat(config.oauth().flow()).isEqualTo("session");
    assertThat(config.oauth().login()).isEqualTo("/oauth2/authorization/operaton");
    assertThat(config.oauth().logout()).isEqualTo("/logout");
  }

  @Test
  void shouldHonourAnExplicitBasicOverride() {
    environment.setProperty(REGISTRATION, "operaton");
    neo().setAuthMode(NeoWebappProperty.AUTH_MODE_BASIC);

    assertThat(resolve().authMode()).isEqualTo("basic");
    assertThat(resolve().oauth()).isNull();
  }

  @Test
  void shouldHonourAnExplicitOAuth2Override() {
    neo().setAuthMode(NeoWebappProperty.AUTH_MODE_OAUTH2);

    NeoClientConfig config = resolve();

    assertThat(config.authMode()).isEqualTo("oauth2");
    // no registration to name, so defer the choice to Spring Security's login page
    assertThat(config.oauth().login()).isEqualTo("/login");
  }

  @Test
  void shouldKeepLoginTargetsUnderTheContextPath() {
    environment.setProperty(REGISTRATION, "operaton");

    NeoClientConfig config =
        new NeoClientConfigResolver(properties, environment).resolve("/engine", null);

    assertThat(config.oauth().login()).isEqualTo("/engine/oauth2/authorization/operaton");
    assertThat(config.oauth().logout()).isEqualTo("/engine/logout");
  }

  @Test
  void shouldReportTheAuthenticatedUser() {
    Principal principal = () -> "demo";

    NeoClientConfig config =
        new NeoClientConfigResolver(properties, environment).resolve("", principal);

    assertThat(config.user().id()).isEqualTo("demo");
  }

  @Test
  void shouldReportNoUserWithoutASession() {
    assertThat(resolve().user()).isNull();
  }

  @Test
  void shouldOmitAnUnsetPluginsUrl() {
    assertThat(resolve().pluginsUrl()).isNull();

    neo().setPluginsUrl("/custom/plugins.json");
    assertThat(resolve().pluginsUrl()).isEqualTo("/custom/plugins.json");
  }

  @Test
  void shouldRejectAnUnknownAuthMode() {
    assertThatThrownBy(() -> neo().setAuthMode("kerberos"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("auto");
  }
}
