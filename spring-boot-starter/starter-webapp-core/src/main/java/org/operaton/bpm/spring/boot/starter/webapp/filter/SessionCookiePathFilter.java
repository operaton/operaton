/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.spring.boot.starter.webapp.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet {@link Filter} that enforces a specific {@code Path} attribute on the session cookie
 * set by the servlet container.
 *
 * <p>When enabled via the {@code operaton.bpm.webapp.session-cookie-path-enforcement=true}
 * property, this filter intercepts outgoing {@code Set-Cookie} headers for the configured
 * session cookie and rewrites their {@code Path} attribute to the value derived from the
 * application's context path and application path. This prevents session cookie conflicts
 * when Operaton runs alongside other applications (e.g. a Keycloak adapter) within a single
 * Spring Boot application.</p>
 *
 * <p>The filter is registered by
 * {@link org.operaton.bpm.spring.boot.starter.webapp.OperatonBpmWebappAutoConfiguration}
 * and should not be instantiated manually.</p>
 *
 * <p>Supported init parameters:</p>
 * <ul>
 *   <li>{@value #PARAM_SESSION_COOKIE_NAME} – name of the session cookie to rewrite
 *       (defaults to {@value #DEFAULT_SESSION_COOKIE_NAME})</li>
 *   <li>{@value #PARAM_COOKIE_PATH} – {@code Path} value to enforce
 *       (defaults to {@value #DEFAULT_COOKIE_PATH})</li>
 * </ul>
 */
public class SessionCookiePathFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(SessionCookiePathFilter.class);

  /** Init-parameter name for the session cookie name. */
  public static final String PARAM_SESSION_COOKIE_NAME = "sessionCookieName";

  /** Init-parameter name for the cookie path to enforce. */
  public static final String PARAM_COOKIE_PATH = "cookiePath";

  /** Default session cookie name used when the init parameter is absent. */
  public static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";

  /** Default cookie path used when the init parameter is absent. */
  public static final String DEFAULT_COOKIE_PATH = "/operaton";

  private String sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;
  private String cookiePath = DEFAULT_COOKIE_PATH;

  @Override
  public void init(FilterConfig filterConfig) {
    String paramName = filterConfig.getInitParameter(PARAM_SESSION_COOKIE_NAME);
    if (paramName != null && !paramName.isBlank()) {
      sessionCookieName = paramName;
    }

    String paramPath = filterConfig.getInitParameter(PARAM_COOKIE_PATH);
    if (paramPath != null && !paramPath.isBlank()) {
      cookiePath = normalizeCookiePath(paramPath);
    }

    log.debug("SessionCookiePathFilter initialized: sessionCookieName='{}', cookiePath='{}'",
        sessionCookieName, cookiePath);
  }

  /**
   * Validates and normalizes a cookie path value.
   *
   * <p>The method rejects paths containing whitespace or semicolons (which would allow
   * header injection), collapses duplicate slashes, strips a trailing slash (unless the
   * path is {@code "/"}), and ensures the path starts with {@code "/"}.</p>
   *
   * @param path the raw path to normalize
   * @return the normalized, validated path
   * @throws IllegalArgumentException if {@code path} contains whitespace or a semicolon
   */
  public static String normalizeCookiePath(String path) {
    if (path.matches(".*[\\s;].*")) {
      throw new IllegalArgumentException(
          "Security violation: Configured cookie path contains illegal characters (whitespace or semicolon). Path: "
              + path);
    }

    String normalized = path.replaceAll("/+", "/");
    if (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (normalized.isEmpty()) {
      normalized = "/";
    } else if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }
    return normalized;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (response instanceof HttpServletResponse httpResponse) {
      chain.doFilter(request, new SessionCookiePathResponseWrapper(httpResponse));
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
    // nothing to clean up
  }

  /**
   * Response wrapper that intercepts cookies added via {@link HttpServletResponse#addCookie(Cookie)}
   * and enforces the configured {@code Path} on the session cookie.
   */
  class SessionCookiePathResponseWrapper extends HttpServletResponseWrapper {

    SessionCookiePathResponseWrapper(HttpServletResponse response) {
      super(response);
    }

    @Override
    public void addCookie(Cookie cookie) {
      if (sessionCookieName.equals(cookie.getName())) {
        cookie.setPath(cookiePath);
        log.trace("Enforcing Path='{}' on session cookie '{}'", cookiePath, cookie.getName());
      }
      super.addCookie(cookie);
    }
  }
}
