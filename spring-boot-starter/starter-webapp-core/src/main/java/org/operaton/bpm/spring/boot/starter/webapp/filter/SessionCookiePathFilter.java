/*
 * Copyright 2026 the Operaton contributors.
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
 * <li>{@value #PARAM_SESSION_COOKIE_NAME} – name of the session cookie to rewrite
 * (defaults to {@value #DEFAULT_SESSION_COOKIE_NAME})</li>
 * <li>{@value #PARAM_COOKIE_PATH} – {@code Path} value to enforce
 * (defaults to {@value #DEFAULT_COOKIE_PATH})</li>
 * </ul>
 *
 * @since 2.1
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

  /** The resolved session cookie name to intercept and rewrite. */
  private String sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;

  /** The resolved and normalized cookie path to enforce. */
  private String cookiePath = DEFAULT_COOKIE_PATH;

  /**
   * Initializes the filter by reading the configuration parameters.
   * <p>If the {@value #PARAM_SESSION_COOKIE_NAME} or {@value #PARAM_COOKIE_PATH}
   * init parameters are provided, they will override the default values. The parsed
   * cookie path is also validated and normalized.</p>
   *
   * @param filterConfig the filter configuration object provided by the servlet container
   */
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

  /**
   * Intercepts the request and wraps the {@link HttpServletResponse} to rewrite
   * the session cookie path before passing it down the filter chain.
   *
   * @param request  the {@code ServletRequest} object contains the client's request
   * @param response the {@code ServletResponse} object contains the filter's response
   * @param chain    the {@code FilterChain} for invoking the next filter or the resource
   * @throws IOException      if an I/O related error has occurred during the processing
   * @throws ServletException if an exception occurs that interferes with the filter's normal operation
   */
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
   * or headers added via {@link HttpServletResponse#addHeader(String, String)} and
   * {@link HttpServletResponse#setHeader(String, String)}, enforcing the configured {@code Path}
   * on the target session cookie.
   */
  class SessionCookiePathResponseWrapper extends HttpServletResponseWrapper {

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response the {@link HttpServletResponse} to be wrapped
     */
    SessionCookiePathResponseWrapper(HttpServletResponse response) {
      super(response);
    }

    /**
     * Adds the specified cookie to the response.
     * If the cookie matches the configured session cookie name, its path is overwritten.
     *
     * @param cookie the {@link Cookie} to return to the client
     */
    @Override
    public void addCookie(Cookie cookie) {
      if (sessionCookieName.equals(cookie.getName())) {
        cookie.setPath(cookiePath);
        log.trace("Enforcing Path='{}' on session cookie '{}'", cookiePath, cookie.getName());
      }
      super.addCookie(cookie);
    }

    /**
     * Adds a response header with the given name and value.
     * If the header is {@code Set-Cookie} and targets the session cookie, the path is rewritten.
     *
     * @param name  the name of the header
     * @param value the additional header value
     */
    @Override
    public void addHeader(String name, String value) {
      super.addHeader(name, rewriteSetCookieHeader(name, value));
    }

    /**
     * Sets a response header with the given name and value.
     * If the header is {@code Set-Cookie} and targets the session cookie, the path is rewritten.
     *
     * @param name  the name of the header
     * @param value the header value
     */
    @Override
    public void setHeader(String name, String value) {
      super.setHeader(name, rewriteSetCookieHeader(name, value));
    }

    /**
     * Evaluates a header name and value to determine if it is a {@code Set-Cookie} directive
     * for the targeted session cookie. If it matches, any existing {@code Path} attribute
     * is removed and replaced with the enforced {@code cookiePath}.
     *
     * @param name  the header name being intercepted
     * @param value the raw header value being intercepted
     * @return the rewritten header value if it targets the session cookie, otherwise the original value
     */
    private String rewriteSetCookieHeader(String name, String value) {
      if (!"Set-Cookie".equalsIgnoreCase(name)) {
        return value;
      }
      if (value == null || !value.startsWith(sessionCookieName + "=")) {
        return value;
      }

      log.trace("Rewriting Set-Cookie header for session cookie '{}'", sessionCookieName);

      // Remove existing Path attribute and append the new enforced Path
      String rewritten = value.replaceAll("(?i);\\s*Path=[^;]*", "");
      rewritten = rewritten + "; Path=" + cookiePath;

      log.trace("Rewrote Path to '{}' for session cookie '{}'", cookiePath, sessionCookieName);
      return rewritten;
    }
  }
}