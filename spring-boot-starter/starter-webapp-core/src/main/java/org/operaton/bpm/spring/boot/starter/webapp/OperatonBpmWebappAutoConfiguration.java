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
package org.operaton.bpm.spring.boot.starter.webapp;

import jakarta.servlet.ServletContext;
import org.operaton.bpm.spring.boot.starter.webapp.filter.SessionCookiePathFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.operaton.bpm.spring.boot.starter.webapp.filter.LazyDelegateFilter.InitHook;
import org.operaton.bpm.spring.boot.starter.webapp.filter.LazyInitRegistration;
import org.operaton.bpm.spring.boot.starter.webapp.filter.ResourceLoaderDependingFilter;

@AutoConfiguration
@ConditionalOnProperty(prefix = WebappProperty.PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnBean(OperatonBpmProperties.class)
@ConditionalOnWebApplication
@AutoConfigureAfter(OperatonBpmAutoConfiguration.class)
public class OperatonBpmWebappAutoConfiguration implements WebMvcConfigurer {

  private final ResourceLoader resourceLoader;

  private final OperatonBpmProperties properties;

  public OperatonBpmWebappAutoConfiguration(ResourceLoader resourceLoader, OperatonBpmProperties properties) {
    this.resourceLoader = resourceLoader;
    this.properties = properties;
  }


  @Bean
  OperatonBpmWebappInitializer operatonBpmWebappInitializer() {
    return new OperatonBpmWebappInitializer(properties);
  }

  @Bean(name = "resourceLoaderDependingInitHook")
  InitHook<ResourceLoaderDependingFilter> resourceLoaderDependingInitHook() {
    return filter -> {
      filter.setResourceLoader(resourceLoader);
      filter.setWebappProperty(properties.getWebapp());
    };
  }

  @Bean
  LazyInitRegistration lazyInitRegistration() {
    return new LazyInitRegistration();
  }

  @Bean
  FaviconResourceResolver faviconResourceResolver() {
    return new FaviconResourceResolver();
  }

  @Bean
  @ConditionalOnProperty(prefix = WebappProperty.PREFIX, name = "session-cookie-path-enforcement", havingValue = "true")
  public FilterRegistrationBean<SessionCookiePathFilter> sessionCookiePathFilter(
          @Value("${server.servlet.session.cookie.name:JSESSIONID}") String sessionCookieName, ServletContext servletContext) {
    String contextPath = servletContext.getContextPath();
    if (contextPath == null || contextPath.equals("/")) {
      contextPath = "";
    }

    String applicationPath = properties.getWebapp().getApplicationPath();
    if (applicationPath == null) {
      applicationPath = "";
    }
    
    String rawCookiePath = contextPath + applicationPath;
    
    if (rawCookiePath.matches(".*[\\s;].*")) {
      throw new IllegalArgumentException("Security violation: Configured cookie path contains illegal characters (whitespace or semicolon). Path: " + rawCookiePath);
    }

    String cookiePath = rawCookiePath.replaceAll("/+", "/");
    
    if (cookiePath.length() > 1 && cookiePath.endsWith("/")) {
      cookiePath = cookiePath.substring(0, cookiePath.length() - 1);
    }
    
    if (cookiePath.isEmpty()) {
      cookiePath = "/";
    } else if (!cookiePath.startsWith("/")) {
      cookiePath = "/" + cookiePath;
    }
    
    FilterRegistrationBean<SessionCookiePathFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new SessionCookiePathFilter());
    registrationBean.setName("Operaton Session Cookie Path Filter");
    
    String urlPattern = applicationPath.isEmpty() ? "/*" : applicationPath + "/*";
    registrationBean.addUrlPatterns(urlPattern.replaceAll("/+", "/"));
    registrationBean.addInitParameter(SessionCookiePathFilter.PARAM_COOKIE_PATH, cookiePath);
    registrationBean.addInitParameter(SessionCookiePathFilter.PARAM_SESSION_COOKIE_NAME, sessionCookieName);
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registrationBean;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    final String classpath = "classpath:" + properties.getWebapp().getWebjarClasspath();
    WebappProperty webapp = properties.getWebapp();
    String applicationPath = webapp.getApplicationPath();

    registry.addResourceHandler(applicationPath + "/lib/**")
        .addResourceLocations(classpath + "/lib/");
    registry.addResourceHandler(applicationPath + "/api/**")
        .addResourceLocations("classpath:/api/");
    registry.addResourceHandler(applicationPath + "/app/**")
        .addResourceLocations(classpath + "/app/");
    registry.addResourceHandler(applicationPath + "/assets/**")
        .addResourceLocations(classpath + "/assets/");
     registry.addResourceHandler(applicationPath + "/favicon.ico")
         .addResourceLocations(classpath + "/") // add slash to get rid of the WARN log
         .resourceChain(true)
         .addResolver(faviconResourceResolver());
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    WebappProperty webapp = properties.getWebapp();
    if (webapp.isIndexRedirectEnabled()) {
      String applicationPath = webapp.getApplicationPath();
      registry.addRedirectViewController("/", applicationPath + "/app/");
    }
  }
}
