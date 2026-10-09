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

import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import org.operaton.bpm.engine.rest.filter.CacheControlFilter;
import org.operaton.bpm.engine.rest.filter.EmptyBodyFilter;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.operaton.bpm.spring.boot.starter.webapp.neo.filter.AppendTrailingSlashFilter;
import org.operaton.bpm.spring.boot.starter.webapp.neo.filter.LazySecurityFilter;
import org.operaton.bpm.webapp.neo.impl.engine.EngineRestApplication;
import org.operaton.bpm.webapp.neo.impl.security.auth.AuthenticationFilter;
import org.operaton.bpm.webapp.neo.impl.security.filter.CsrfPreventionFilter;
import org.operaton.bpm.webapp.neo.impl.security.filter.SessionCookieFilter;
import org.operaton.bpm.webapp.neo.impl.security.filter.headersec.HttpHeaderSecurityFilter;
import org.operaton.bpm.webapp.neo.impl.security.filter.util.HttpSessionMutexListener;
import org.operaton.bpm.webapp.neo.impl.util.ServletContextUtil;
import org.operaton.bpm.webapp.neo.impl.web.AdminApplication;

import static java.util.Collections.singletonMap;
import static org.glassfish.jersey.servlet.ServletProperties.JAXRS_APPLICATION_CLASS;

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

    // Neo's ServletContextUtil keeps its own attribute name, so this no longer races with
    // OperatonBpmWebappInitializer: each webapp records its own application path and reads
    // back only its own. Previously both shared one key and the legacy value ("/operaton")
    // always won, which made CsrfPreventionFilter scope the neo XSRF cookie to a path the
    // SPA never requests.
    ServletContextUtil.setAppPath(basePath, servletContext);

    // The webapp filter chain guards the API namespace (/api/*) and the SPA app
    // paths. When served from a sub-path we can safely map the app wildcard
    // (basePath + "/*"). At the root the request-rejecting filters must NOT use "/*":
    // that would wrap the whole server, including /engine-rest/* and the legacy
    // /operaton webapp. The header filter is the exception and is mapped separately
    // below, because it only decorates responses.
    boolean servedAtRoot = basePath.isEmpty();
    String[] webappPaths = servedAtRoot
        ? new String[] { apiWildcardPath }
        : new String[] { apiWildcardPath, basePath + "/*" };

    Map<String, String> headerSecurityProperties = webapp
      .getHeaderSecurity()
      .getInitParams();

    // The header filter deliberately gets a wider mapping than the rest of the chain.
    // Served at the root, webappPaths covers only /api/*, which would leave the SPA
    // shell — the HTML at "/" and at every client-side route — without CSP,
    // X-Frame-Options, X-Content-Type-Options or HSTS. Those headers matter most on
    // exactly the document responses the resource handlers serve. Unlike the auth,
    // CSRF and session filters, this one only sets response headers, so widening it to
    // "/*" is safe: it cannot reject a request or touch a session, and the headers it
    // adds to /engine-rest responses are inert there.
    String[] headerSecurityPaths = servedAtRoot ? new String[] { "/*" } : webappPaths;

    // Registration order is chain order, and the two response-shaping filters come first
    // on purpose. The auth, security and CSRF filters below all reject requests outright:
    // if they ran first, a 401 or 403 would leave the chain before the header filter
    // could add CSP/nosniff/HSTS, and before SessionCookieFilter — which wraps the
    // response — could stamp SameSite/Secure onto the JSESSIONID that AuthenticationFilter
    // mints via getSession(true). Both filters only decorate, so nothing downstream
    // depends on them having run last.
    registerFilter("Neo HttpHeaderSecurity", HttpHeaderSecurityFilter.class,
        headerSecurityProperties,
        headerSecurityPaths);
    registerFilter("Neo SessionCookieFilter", SessionCookieFilter.class,
        webapp.getSessionCookie().getInitParams(),
        webappPaths);

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

    registerFilter("Neo EmptyBodyFilter", EmptyBodyFilter.class,
        webappPaths);

    registerFilter("Neo CacheControlFilter", CacheControlFilter.class,
        apiWildcardPath, basePath + "/assets/*");

    // The filters above guard /api/*; these servlets are what actually answers there.
    // Without them the namespace fell through to the SPA's index.html catch-all, which
    // is why the login and logout endpoints could not be reached and the SPA had to
    // authenticate against the standalone /engine-rest deployment instead.
    registerServlet("Neo Admin Api", AdminApplication.class, basePath + "/api/admin/*");
    registerServlet("Neo Engine Api", EngineRestApplication.class, basePath + "/api/engine/*");
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

  private ServletRegistration registerServlet(final String servletName, final Class<?> applicationClass, final String... urlPatterns) {
    ServletRegistration servletRegistration = servletContext.getServletRegistration(servletName);

    if (servletRegistration == null) {
      servletRegistration = servletContext.addServlet(servletName, ServletContainer.class);
      servletRegistration.addMapping(urlPatterns);
      servletRegistration.setInitParameters(singletonMap(JAXRS_APPLICATION_CLASS, applicationClass.getName()));

      log.debug("Servlet {} for URL {} registered.", servletName, urlPatterns);
    }

    return servletRegistration;
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
