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
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.operaton.bpm.webapp.impl.IllegalWebAppConfigurationException;
import org.operaton.bpm.webapp.impl.security.SecurityActions;
import org.operaton.bpm.webapp.impl.security.SecurityActions.SecurityAction;
import org.operaton.bpm.webapp.impl.util.ServletContextUtil;
import org.operaton.bpm.webapp.impl.util.ServletFilterUtil;

/**
 * <p>Servlet {@link Filter} implementation responsible for populating the
 * {@link Authentications#getCurrent()} thread-local (ie. binding the current
 * set of authentications to the current thread so that it may easily be obtained
 * by application parts not having access to the current session.</p>
 *
 * @author Daniel Meyer
 * @author nico.rehwaldt
 */
public class AuthenticationFilter implements Filter {

  public static final String AUTH_CACHE_TTL_INIT_PARAM_NAME = "cacheTimeToLive";

  protected Long cacheTimeToLive;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    String authCacheTTLAsString = filterConfig.getInitParameter(AUTH_CACHE_TTL_INIT_PARAM_NAME);
    if (!ServletFilterUtil.isEmpty(authCacheTTLAsString)) {
      cacheTimeToLive = Long.parseLong(authCacheTTLAsString.trim());

      if (cacheTimeToLive < 0) {
        throw new IllegalWebAppConfigurationException("'%s' cannot be negative.".formatted(AUTH_CACHE_TTL_INIT_PARAM_NAME));
      }
    }
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
    throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;

    HttpSession session = req.getSession(true);

    // get authentication from session
    Authentications authentications = AuthenticationUtil.getAuthsFromSession(session);

    if (cacheTimeToLive != null) {
      if (cacheTimeToLive > 0) {
        ServletContext servletContext = request.getServletContext();
        ServletContextUtil.setCacheTTLForLogin(cacheTimeToLive, servletContext);
      }
      AuthenticationUtil.updateCache(authentications, session, cacheTimeToLive);
    }

    Authentications.setCurrent(authentications);

    try {

      SecurityActions.runWithAuthentications((SecurityAction<Void>) () -> {
        chain.doFilter(request, response);
        return null;
      }, authentications);
    } finally {
      Authentications.clearCurrent();
      AuthenticationUtil.updateSession(req.getSession(false), authentications);
    }

  }

  public Long getCacheTimeToLive() {
    return cacheTimeToLive;
  }

}
