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
package org.operaton.bpm.spring.boot.starter.property;

import java.util.List;

import static org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties.joinOn;

/**
 * Configuration for the new web apps (webapps-neo), a single-page application
 * served as an embedded Spring application.
 *
 * <p>Bound under {@code operaton.bpm.webapp.neo.*}. It is disabled by default at
 * the starter level so existing embedded-starter users are not affected; the
 * Operaton Run distribution enables it explicitly.</p>
 */
public class NeoWebappProperty {

  public static final String DEFAULT_APP_PATH = "";

  /** Resolve the authentication mode from the application configuration. */
  public static final String AUTH_MODE_AUTO = "auto";
  /** Username and password against the REST API. */
  public static final String AUTH_MODE_BASIC = "basic";
  /** Server-side session established by the Spring Security OAuth2 login flow. */
  public static final String AUTH_MODE_OAUTH2 = "oauth2";

  private static final List<String> AUTH_MODES = List.of(AUTH_MODE_AUTO, AUTH_MODE_BASIC, AUTH_MODE_OAUTH2);

  /**
   * Enables the embedded webapps-neo auto configuration. Disabled by default;
   * the Operaton Run distribution turns it on.
   */
  protected boolean enabled = false;

  /**
   * Context path the SPA is served from. Defaults to the root path ("").
   */
  protected String applicationPath = DEFAULT_APP_PATH;

  /**
   * Classpath location the webapps-neo assets are unpacked to. Kept separate
   * from the legacy webapp classpath so both can coexist.
   */
  protected String webjarClasspath = "/META-INF/resources/webjars/operaton-neo";

  protected String securityConfigFile = "/securityFilterRules.json";

  protected boolean indexRedirectEnabled = true;

  /**
   * Authentication mode the SPA is told to use, one of {@code auto}, {@code basic}
   * or {@code oauth2}. With {@code auto} (the default) the mode is derived from the
   * application configuration: {@code oauth2} when Spring Security OAuth2 client
   * registrations are present, {@code basic} otherwise. Set it explicitly to
   * override that detection.
   */
  protected String authMode = AUTH_MODE_AUTO;

  /**
   * URL of the runtime plugin manifest handed to the SPA. Empty means the SPA
   * falls back to {@code /plugins/plugins.json} on its own origin.
   */
  protected String pluginsUrl = "";

  /**
   * Hides the pre-release warning banner in the SPA.
   */
  protected boolean hideReleaseWarning = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getApplicationPath() {
    return applicationPath;
  }

  public void setApplicationPath(String applicationPath) {
    this.applicationPath = sanitizeApplicationPath(applicationPath);
  }

  protected String sanitizeApplicationPath(String applicationPath) {
    if (applicationPath == null || applicationPath.isEmpty()) {
      return "";
    }

    if (!applicationPath.startsWith("/")) {
      applicationPath = "/" + applicationPath;
    }

    if (applicationPath.endsWith("/")) {
      applicationPath = applicationPath.substring(0, applicationPath.length() - 1);
    }

    return applicationPath;
  }

  public String getWebjarClasspath() {
    return webjarClasspath;
  }

  public void setWebjarClasspath(String webjarClasspath) {
    this.webjarClasspath = webjarClasspath;
  }

  public String getSecurityConfigFile() {
    return securityConfigFile;
  }

  public void setSecurityConfigFile(String securityConfigFile) {
    this.securityConfigFile = securityConfigFile;
  }

  public boolean isIndexRedirectEnabled() {
    return indexRedirectEnabled;
  }

  public void setIndexRedirectEnabled(boolean indexRedirectEnabled) {
    this.indexRedirectEnabled = indexRedirectEnabled;
  }

  public String getAuthMode() {
    return authMode;
  }

  public void setAuthMode(String authMode) {
    if (authMode != null && !AUTH_MODES.contains(authMode)) {
      throw new IllegalArgumentException(
        "Please provide a valid authentication mode. The available ones are: " + AUTH_MODES);
    }
    this.authMode = authMode == null ? AUTH_MODE_AUTO : authMode;
  }

  public String getPluginsUrl() {
    return pluginsUrl;
  }

  public void setPluginsUrl(String pluginsUrl) {
    this.pluginsUrl = pluginsUrl == null ? "" : pluginsUrl;
  }

  public boolean isHideReleaseWarning() {
    return hideReleaseWarning;
  }

  public void setHideReleaseWarning(boolean hideReleaseWarning) {
    this.hideReleaseWarning = hideReleaseWarning;
  }

  @Override
  public String toString() {
    return joinOn(this.getClass())
      .add("enabled=" + enabled)
      .add("applicationPath='" + applicationPath + '\'')
      .add("webjarClasspath='" + webjarClasspath + '\'')
      .add("securityConfigFile='" + securityConfigFile + '\'')
      .add("indexRedirectEnabled=" + indexRedirectEnabled)
      .add("authMode='" + authMode + '\'')
      .add("pluginsUrl='" + pluginsUrl + '\'')
      .add("hideReleaseWarning=" + hideReleaseWarning)
      .toString();
  }
}
