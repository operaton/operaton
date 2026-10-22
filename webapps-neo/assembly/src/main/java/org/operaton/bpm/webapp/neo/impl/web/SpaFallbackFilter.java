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
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves the SPA shell for deep links.
 */
public class SpaFallbackFilter implements Filter {

  protected static final String INDEX_HTML = "/index.html";

  /** Request prefixes that never get the SPA shell. */
  protected static final List<String> RESERVED_PREFIXES = List.of("/api/");

  protected ServletContext servletContext;

  @Override
  public void init(FilterConfig filterConfig) {
    servletContext = filterConfig.getServletContext();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

    if (isReadRequest(httpRequest) && isClientRoute(path) && servletContext.getResource(path) == null) {
      request.getRequestDispatcher(INDEX_HTML).forward(request, response);
    } else {
      chain.doFilter(request, response);
    }
  }

  protected boolean isReadRequest(HttpServletRequest request) {
    return "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
  }

  protected boolean isClientRoute(String path) {
    // the welcome file serves the root
    if (path.isEmpty() || "/".equals(path)) {
      return false;
    }
    for (String prefix : RESERVED_PREFIXES) {
      if (path.startsWith(prefix) || path.equals(prefix.substring(0, prefix.length() - 1))) {
        return false;
      }
    }
    String lastSegment = path.substring(path.lastIndexOf('/') + 1);
    return !lastSegment.contains(".");
  }
}
