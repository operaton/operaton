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
package org.operaton.bpm.spring.boot.starter.webapp.neo;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import jakarta.servlet.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import org.operaton.bpm.engine.rest.filter.CacheControlFilter;
import org.operaton.bpm.engine.rest.filter.EmptyBodyFilter;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.operaton.bpm.spring.boot.starter.webapp.neo.filter.AppendTrailingSlashFilter;
import org.operaton.bpm.spring.boot.starter.webapp.neo.filter.LazySecurityFilter;
import org.operaton.bpm.webapp.impl.security.auth.AuthenticationFilter;
import org.operaton.bpm.webapp.impl.security.filter.CsrfPreventionFilter;
import org.operaton.bpm.webapp.impl.security.filter.SessionCookieFilter;
import org.operaton.bpm.webapp.impl.security.filter.headersec.HttpHeaderSecurityFilter;
import org.operaton.bpm.webapp.impl.security.filter.util.HttpSessionMutexListener;
import org.operaton.bpm.webapp.impl.util.ServletContextUtil;

import static java.util.Collections.singletonMap;

public class OperatonBpmWebappNeoInitializer implements ServletContextInitializer {

  private static final Logger log = LoggerFactory.getLogger(OperatonBpmWebappNeoInitializer.class);

  private static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST);

  private ServletContext servletContext;

  private final OperatonBpmProperties properties;

  OperatonBpmWebappNeoInitializer(OperatonBpmProperties properties) {
    this.properties = properties;
  }

  @Override
  public void onStartup(ServletContext servletContext) {
    this.servletContext = servletContext;

    servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));

    servletContext.addListener(new HttpSessionMutexListener());

    WebappProperty webapp = properties.getWebapp();

    // base path the SPA is served from; empty string means the application root
    String basePath = webapp.getNeo().getApplicationPath();
    String apiWildcardPath = basePath + "/api/*";

    ServletContextUtil.setAppPath(basePath, servletContext);

    // The webapp filter chain guards the API namespace (/api/*) and the SPA app
    // paths. When served from a sub-path we can safely map the app wildcard
    // (basePath + "/*"). At the root we must NOT use "/*": it would wrap the
    // whole server, including /engine-rest/* and the legacy /operaton webapp, so
    // only the API namespace is guarded there (the SPA shell is static and is
    // served via Spring MVC resource handlers).
    boolean servedAtRoot = basePath.isEmpty();
    String[] webappPaths = servedAtRoot
        ? new String[] { apiWildcardPath }
        : new String[] { apiWildcardPath, basePath + "/*" };

    if (!servedAtRoot) {
      // ensures a trailing slash is added when the SPA is served from a sub-path
      registerFilter("Neo AppendTrailingSlashFilter", AppendTrailingSlashFilter.class, basePath);
    }
    registerFilter("Neo Authentication Filter", AuthenticationFilter.class,
        Collections.singletonMap("cacheTimeToLive", getAuthCacheTTL(webapp)),
        webappPaths);
    registerFilter("Neo Security Filter", LazySecurityFilter.class,
        singletonMap("configFile", webapp.getNeo().getSecurityConfigFile()),
        webappPaths);
    registerFilter("Neo CsrfPreventionFilter", CsrfPreventionFilter.class,
        webapp.getCsrf().getInitParams(),
        webappPaths);
    registerFilter("Neo SessionCookieFilter", SessionCookieFilter.class,
        webapp.getSessionCookie().getInitParams(),
        webappPaths);

    Map<String, String> headerSecurityProperties = webapp
      .getHeaderSecurity()
      .getInitParams();

    registerFilter("Neo HttpHeaderSecurity", HttpHeaderSecurityFilter.class,
        headerSecurityProperties,
        webappPaths);

    registerFilter("Neo EmptyBodyFilter", EmptyBodyFilter.class,
        webappPaths);

    registerFilter("Neo CacheControlFilter", CacheControlFilter.class,
        apiWildcardPath, basePath + "/assets/*");
  }

  protected String getAuthCacheTTL(WebappProperty webapp) {
    long authCacheTTL = webapp.getAuth().getCache().getTimeToLive();
    boolean authCacheTTLEnabled = webapp.getAuth().getCache().isTtlEnabled();
    if (authCacheTTLEnabled) {
      return Long.toString(authCacheTTL);
    } else {
      return "";
    }
  }

  private FilterRegistration registerFilter(final String filterName, final Class<? extends Filter> filterClass, final String... urlPatterns) {
    return registerFilter(filterName, filterClass, null, urlPatterns);
  }

  private FilterRegistration registerFilter(final String filterName, final Class<? extends Filter> filterClass, final Map<String, String> initParameters,
                                            final String... urlPatterns) {
    FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);

    if (filterRegistration == null) {
      filterRegistration = servletContext.addFilter(filterName, filterClass);
      filterRegistration.addMappingForUrlPatterns(DISPATCHER_TYPES, true, urlPatterns);

      if (initParameters != null) {
        filterRegistration.setInitParameters(initParameters);
      }

      log.debug("Filter {} for URL {} registered.", filterName, urlPatterns);
    }

    return filterRegistration;
  }
}
