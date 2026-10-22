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
package org.operaton.bpm.webapp.neo.impl.web;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.operaton.bpm.engine.impl.util.StringUtil.hasText;

/**
 * Hides the webapp unless explicitly enabled.
 */
public class NeoEnabledFilter implements Filter {

  public static final String ENABLED_PROPERTY = "operaton.bpm.webapp.neo.enabled";
  public static final String ENABLED_ENV = "OPERATON_BPM_WEBAPP_NEO_ENABLED";
  public static final String DISABLED_REDIRECT_PARAM = "disabledRedirect";

  protected boolean enabled;
  protected String disabledRedirect;

  @Override
  public void init(FilterConfig filterConfig) {
    enabled = isEnabled();
    disabledRedirect = trim(filterConfig.getInitParameter(DISABLED_REDIRECT_PARAM));
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (enabled) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

    if (hasText(disabledRedirect) && (path.isEmpty() || "/".equals(path))) {
      httpResponse.sendRedirect(disabledRedirect);
    } else {
      httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  protected boolean isEnabled() {
    String value = trim(getSystemProperty(ENABLED_PROPERTY));
    if (!hasText(value)) {
      value = trim(getEnv(ENABLED_ENV));
    }
    return Boolean.parseBoolean(value);
  }

  protected static String trim(String value) {
    return value == null ? null : value.trim();
  }

  protected String getSystemProperty(String name) {
    return System.getProperty(name);
  }

  protected String getEnv(String name) {
    return System.getenv(name);
  }
}
