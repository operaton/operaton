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
package org.operaton.bpm.webapp.impl.security.auth;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;
import org.operaton.bpm.webapp.impl.util.ProcessEngineUtil;
import org.operaton.bpm.webapp.impl.util.ServletContextUtil;

import static org.operaton.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM;

public class ContainerBasedAuthenticationFilter implements Filter {

  public static final Pattern APP_PATTERN = Pattern.compile("/app/(cockpit|admin|tasklist|welcome)/([^/]+)/");
  public static final Pattern API_ENGINE_PATTERN = Pattern.compile("/api/engine/engine/([^/]+)/.*");
  public static final Pattern API_STATIC_PLUGIN_PATTERN = Pattern.compile("/api/(cockpit|admin|tasklist|welcome)/plugin/[^/]+/static/.*");
  public static final Pattern API_PLUGIN_PATTERN = Pattern.compile("/api/(cockpit|admin|tasklist|welcome)/plugin/[^/]+/([^/]+)/.*");

  protected AuthenticationProvider authenticationProvider;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

    String authenticationProviderClassName = filterConfig.getInitParameter(AUTHENTICATION_PROVIDER_PARAM);

    if (authenticationProviderClassName == null) {
      throw new ServletException("Cannot instantiate authentication filter: no authentication provider set. init-param " + AUTHENTICATION_PROVIDER_PARAM + " missing");
    }

    try {
      Class<?> authenticationProviderClass = Class.forName(authenticationProviderClassName);
      authenticationProvider = (AuthenticationProvider) authenticationProviderClass.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      throw new ServletException("Cannot instantiate authentication filter: authentication provider not found", e);
    } catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) {
      throw new ServletException("Cannot instantiate authentication filter: cannot instantiate authentication provider", e);
    } catch (IllegalAccessException e) {
      throw new ServletException("Cannot instantiate authentication filter: constructor not accessible", e);
    } catch (ClassCastException e) {
      throw new ServletException("Cannot instantiate authentication filter: authentication provider does not implement interface " +
          AuthenticationProvider.class.getName(), e);
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest req = (HttpServletRequest) request;
    final HttpServletResponse resp = (HttpServletResponse) response;

    String engineName = extractEngineName(req);

    if (engineName == null) {
      chain.doFilter(request, response);
      return;
    }

    ProcessEngine engine = getAddressedEngine(engineName);

    if (engine == null) {
      resp.sendError(404, "Process engine " + engineName + " not available");
      return;
    }

    AuthenticationResult authenticationResult = authenticationProvider.extractAuthenticatedUser(req, engine);
    if (authenticationResult.isAuthenticated()) {
      Authentications authentications = AuthenticationUtil.getAuthsFromSession(req.getSession());
      String authenticatedUser = authenticationResult.getAuthenticatedUser();

      if (!existisAuthentication(authentications, engineName, authenticatedUser)) {
        List<String> groups = authenticationResult.getGroups();
        List<String> tenants = authenticationResult.getTenants();

        UserAuthentication authentication = createAuthentication(engine, authenticatedUser, groups, tenants);
        authentications.addOrReplace(authentication);
      }

      chain.doFilter(request, response);
    }
    else {
      resp.setStatus(Status.UNAUTHORIZED.getStatusCode());
      authenticationProvider.augmentResponseByAuthenticationChallenge(resp, engine);
    }

  }

  protected String getRequestUri(HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    String contextPath = request.getContextPath();

    int contextPathLength = contextPath.length();
    if (contextPathLength > 0) {
      requestURI = requestURI.substring(contextPathLength);
    }

    ServletContext servletContext = request.getServletContext();
    String applicationPath = ServletContextUtil.getAppPath(servletContext);
    int applicationPathLength = applicationPath.length();

    if (applicationPathLength > 0) {
      requestURI = requestURI.substring(applicationPathLength);
    }

    return requestURI;
  }

  protected String extractEngineName(HttpServletRequest request) {
    String requestUri = getRequestUri(request);
    String requestMethod = request.getMethod();

    Matcher appMatcher = APP_PATTERN.matcher(requestUri);
    if (appMatcher.matches()) {
      return appMatcher.group(2);
    }

    Matcher apiEngineMatcher = API_ENGINE_PATTERN.matcher(requestUri);
    if (apiEngineMatcher.matches()) {
      return apiEngineMatcher.group(1);
    }

    Matcher apiStaticPluginPattern = API_STATIC_PLUGIN_PATTERN.matcher(requestUri);
    if ("GET".equals(requestMethod) && apiStaticPluginPattern.matches()) {
      return null;
    }

    Matcher apiPluginPattern = API_PLUGIN_PATTERN.matcher(requestUri);
    if (apiPluginPattern.matches()) {
      return apiPluginPattern.group(2);
    }

    return null;
  }

  protected ProcessEngine getAddressedEngine(String engineName) {
    return ProcessEngineUtil.lookupProcessEngine(engineName);
  }

  protected boolean existisAuthentication(Authentications authentications, String engineName, String username) {
    // For each process engine, there can be at most one authentication active in a given session.
    Authentication authentication = authentications.getAuthenticationForProcessEngine(engineName);
    return authentication != null && isAuthenticated(authentication, engineName, username);
  }

  protected boolean isAuthenticated(Authentication authentication, String engineName, String username) {
    String processEngineName = authentication.getProcessEngineName();
    String identityId = authentication.getIdentityId();
    return processEngineName.equals(engineName) && identityId.equals(username);
  }

  protected UserAuthentication createAuthentication(ProcessEngine processEngine, String username, List<String> groups, List<String> tenants) {
    return AuthenticationUtil.createAuthentication(processEngine, username, groups, tenants);
  }

}
