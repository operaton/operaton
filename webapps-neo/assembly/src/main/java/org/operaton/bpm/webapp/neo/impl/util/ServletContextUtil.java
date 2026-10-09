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
package org.operaton.bpm.webapp.neo.impl.util;

import java.util.Date;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.webapp.neo.impl.security.auth.AuthenticationFilter;
import org.operaton.bpm.webapp.neo.impl.security.auth.UserAuthenticationResource;

/**
 * With Operaton.13 we introduced the application path prefix /operaton to Spring Boot.
 * The application path is set in Spring Boot's servlet context and is consumed by filters and
 * servlets of the Operaton Webapp. This util class holds the methods to get and set the
 * application path.
 */
public final class ServletContextUtil {

  /**
   * Deliberately distinct from the legacy webapp's attribute name. Both webapps can be
   * deployed into the same servlet context, and a shared key would mean whichever
   * initializer ran last decided the application path for both — which previously left the
   * neo SPA emitting its XSRF cookie under the legacy webapp's path.
   */
  protected static final String APP_PATH_ATTR_NAME =
    "org.operaton.bpm.spring.boot.starter.webapp.neo.applicationPath";

  protected static final String SUCCESSFUL_ET_ATTR_NAME =
    "org.operaton.bpm.webapp.neo.telemetry.data.stored";

  protected static final String AUTH_CACHE_TTL_ATTR_NAME =
    "org.operaton.bpm.webapp.neo.auth.cache.ttl";

  private ServletContextUtil() {
  }

  /**
   * Consumed by Operaton CE & EE Webapp:
   * Retrieves the application path from Spring Boot's servlet context.
   *
   * @param servletContext that holds the application path
   * @return a non-empty <code>String</code> containing the application path or an empty
   * <code>String</code> when no application path was set.
   */
  public static String getAppPath(ServletContext servletContext) {
    String applicationPath = (String) servletContext.getAttribute(APP_PATH_ATTR_NAME);

    if (applicationPath == null) {
      return "";

    } else {
      return applicationPath;

    }
  }

  /**
   * Sets an application path into Spring Boot's servlet context.
   *
   * @param applicationPath to be set into Spring Boot's servlet context
   * @param servletContext of Spring Boot the application path should be set into
   */
  public static void setAppPath(String applicationPath, ServletContext servletContext) {
    servletContext.setAttribute(APP_PATH_ATTR_NAME, applicationPath);
  }

  /**
   * @return whether the web application has already successfully been sent to
   *         the engine as telemetry info or not.
   */
  public static boolean isTelemetryDataSentAlready(String webappName, String engineName, ServletContext servletContext) {
    return servletContext.getAttribute(buildTelemetrySentAttribute(webappName, engineName)) != null;
  }

  /**
   * Marks the web application as successfully sent to the engine as telemetry
   * info
   */
  public static void setTelemetryDataSent(String webappName, String engineName, ServletContext servletContext) {
    servletContext.setAttribute(buildTelemetrySentAttribute(webappName, engineName), true);
  }

  protected static String buildTelemetrySentAttribute(String webappName, String engineName) {
    return SUCCESSFUL_ET_ATTR_NAME + "." + webappName + "." + engineName;
  }

  /**
   * Sets {@param cacheTimeToLive} in the {@link AuthenticationFilter} to be used on initial login authentication.
   * See {@link AuthenticationFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
   */
  public static void setCacheTTLForLogin(long cacheTimeToLive, ServletContext servletContext) {
    servletContext.setAttribute(AUTH_CACHE_TTL_ATTR_NAME, cacheTimeToLive);
  }

  /**
   * Returns {@code authCacheValidationTime} from servlet context to be used on initial login authentication.
   * See {@link UserAuthenticationResource#doLogin(String, String, String, String)}
   */
  public static Date getAuthCacheValidationTime(ServletContext servletContext) {
    Long cacheTimeToLive = (Long) servletContext.getAttribute(AUTH_CACHE_TTL_ATTR_NAME);

    if (cacheTimeToLive != null) {
      return new Date(ClockUtil.getCurrentTime().getTime() + cacheTimeToLive);

    } else {
      return null;

    }
  }
}
