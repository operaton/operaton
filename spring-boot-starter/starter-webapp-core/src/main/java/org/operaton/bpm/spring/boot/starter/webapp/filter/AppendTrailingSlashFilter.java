/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter to ensure request paths always have a trailing slash. Transparent trailing slash matching was deprecated
 * in Spring Framework 6 and removed in Spring Framework 7. This filter keeps the legacy webapp entry points working for
 * the registered request patterns by redirecting them to their canonical trailing-slash URLs.
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/filters.html">Spring Framework URL Handler Filter</a>
 */
public class AppendTrailingSlashFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String requestURI = ((HttpServletRequest) request).getRequestURI();
    if (isLocalPath(requestURI)) {
      ((HttpServletResponse) response).sendRedirect(requestURI + "/");
    } else {
      chain.doFilter(request, response);
    }
  }

  protected boolean isLocalPath(String requestURI) {
    return requestURI != null
        && requestURI.startsWith("/")
        && !requestURI.startsWith("//")
        && !requestURI.contains("://");
  }

}
