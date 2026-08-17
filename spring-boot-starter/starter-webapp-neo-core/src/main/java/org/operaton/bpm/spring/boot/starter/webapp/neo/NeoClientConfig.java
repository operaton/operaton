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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration handed to the webapps-neo single-page application at boot.
 *
 * <p>The SPA used to have these values compiled into its bundle by Vite, which
 * meant the distribution could only be reconfigured by rebuilding the frontend.
 * It now fetches this document from {@code {applicationPath}/config.json}, so the
 * same bundle serves every configuration.</p>
 *
 * @since 2.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NeoClientConfig(
    List<Backend> backends,
    String authMode,
    OAuth oauth,
    String pluginsUrl,
    boolean hideReleaseWarning,
    User user) {

  /**
   * A REST API the SPA can talk to. When embedded, the url is empty, meaning the
   * SPA's own origin.
   */
  public record Backend(String name, String url) {
  }

  /**
   * Where the SPA sends the browser to start and end an OAuth2 session.
   *
   * @param flow  {@code session} for the server-side Spring Security login flow.
   *              The SPA also supports {@code pkce}, but that is only ever
   *              configured by a statically served config document, because it
   *              talks to the identity provider directly instead of to us.
   * @param login top-level navigation target that starts the login flow
   * @param logout top-level navigation target that ends the session
   */
  public record OAuth(String flow, String login, String logout) {
  }

  /**
   * The already-authenticated user, absent when there is no session. Lets the SPA
   * skip the login screen on reload without a failed request first.
   */
  public record User(String id) {
  }
}
