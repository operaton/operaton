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
package org.operaton.bpm.webapp.impl.security.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.webapp.impl.security.auth.Authentications;
import org.operaton.bpm.webapp.impl.security.filter.util.FilterRules;
import org.operaton.bpm.webapp.impl.util.ServletContextUtil;


/**
 * <p>Simple filter implementation which delegates to a list of {@link SecurityFilterRule FilterRules},
 * evaluating their {@link SecurityFilterRule#setAuthorized(org.operaton.bpm.webapp.impl.security.filter.AppRequest)} condition
 * for the given request.</p>
 *
 * <p>This filter must be configured using a init-param in the web.xml file. The parameter must be named
 * "configFile" and point to the configuration file located in the servlet context.</p>
 *
 * @author Daniel Meyer
 * @author nico.rehwaldt
 */
public class SecurityFilter implements Filter {

  protected List<SecurityFilterRule> filterRules = new ArrayList<>();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    doFilterSecure((HttpServletRequest) request, (HttpServletResponse) response, chain);
  }

  public void doFilterSecure(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

    String requestUri = getRequestUri(request);

    Authorization authorization = authorize(request.getMethod(), requestUri, filterRules);

    // attach authorization headers
    // to response
    authorization.attachHeaders(response);

    if (authorization.isGranted()) {

      // if request is authorized
      chain.doFilter(request, response);
    } else
    if (authorization.isAuthenticated()) {
      String application = authorization.getApplication();

      if (application != null) {
        sendForbiddenApplicationAccess(application, response);
      } else {
        sendForbidden(response);
      }
    } else {
      sendUnauthorized(response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    ServletContext servletContext = filterConfig.getServletContext();

    String applicationPath = ServletContextUtil.getAppPath(servletContext);
    loadFilterRules(filterConfig, applicationPath);
  }

  /**
   * Iterate over a number of filter rules and match them against
   * the specified request.
   *
   * @param request
   * @param filterRules
   *
   * @return the joined {@link AuthorizationStatus} for this request matched against all filter rules
   */
  public static Authorization authorize(String requestMethod, String requestUri, List<SecurityFilterRule> filterRules) {
    return FilterRules.authorize(requestMethod, requestUri, filterRules);
  }

  protected void loadFilterRules(FilterConfig filterConfig,
                                 String applicationPath) throws ServletException {
    String configFileName = filterConfig.getInitParameter("configFile");
    InputStream configFileResource = filterConfig.getServletContext().getResourceAsStream(configFileName);
    if (configFileResource == null) {
      throw new ServletException("Could not read security filter config file '"+configFileName+"': no such resource in servlet context.");
    } else {
      try {
        filterRules = FilterRules.load(configFileResource, applicationPath);
      } catch (Exception e) {
        throw new RuntimeException("Exception while parsing '%s'".formatted(configFileName), e);
      } finally {
        IoUtil.closeSilently(configFileResource);
      }
    }
  }

  protected void sendForbidden(HttpServletResponse response) throws IOException {
    response.sendError(403);
  }

  protected void sendUnauthorized(HttpServletResponse response) throws IOException {
    response.sendError(401);
  }

  protected void sendForbiddenApplicationAccess(String application, HttpServletResponse response) throws IOException {
    response.sendError(403, "No access rights for " + application);
  }

  protected boolean isAuthenticated() {
    return Authentications.getCurrent() != null;
  }

  protected String getRequestUri(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    return request.getRequestURI().substring(contextPath.length());
  }
}
