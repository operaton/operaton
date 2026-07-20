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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.property.NeoWebappProperty;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.operaton.bpm.spring.boot.starter.webapp.neo.filter.LazyDelegateFilter.InitHook;
import org.operaton.bpm.spring.boot.starter.webapp.neo.filter.LazyInitRegistration;
import org.operaton.bpm.spring.boot.starter.webapp.neo.filter.ResourceLoaderDependingFilter;

/**
 * Auto configuration for the new web apps (webapps-neo). The SPA is served as a
 * static resource bundle, by default from the application root ({@code /}). It is
 * gated independently from the legacy webapps via {@code operaton.bpm.webapp.neo.enabled}
 * so both can run side by side.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = WebappProperty.PREFIX, name = "neo.enabled", havingValue = "true")
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
  OperatonBpmWebappNeoInitializer operatonBpmWebappNeoInitializer() {
    return new OperatonBpmWebappNeoInitializer(properties);
  }

  @Bean(name = "neoResourceLoaderDependingInitHook")
  InitHook<ResourceLoaderDependingFilter> neoResourceLoaderDependingInitHook() {
    return filter -> {
      filter.setResourceLoader(resourceLoader);
      filter.setWebappProperty(properties.getWebapp());
    };
  }

  @Bean
  LazyInitRegistration neoLazyInitRegistration() {
    return new LazyInitRegistration();
  }

  @Bean
  FaviconResourceResolver neoFaviconResourceResolver() {
    return new FaviconResourceResolver();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    NeoWebappProperty neo = properties.getWebapp().getNeo();
    String base = neo.getApplicationPath();
    String classpath = "classpath:" + neo.getWebjarClasspath() + "/";

    // Serve the SPA bundle (index.html, assets, favicon, ...) and fall back to
    // index.html for client-side routes (deep links). More specific servlet
    // mappings (e.g. /engine-rest/*, the legacy /operaton/* webapp) take
    // precedence over this catch-all, so they are not shadowed.
    registry.addResourceHandler(base + "/**")
        .addResourceLocations(classpath)
        .resourceChain(true)
        .addResolver(new SpaResourceResolver());
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    NeoWebappProperty neo = properties.getWebapp().getNeo();
    String base = neo.getApplicationPath();

    // Serve the SPA shell at the application path root
    registry.addViewController(base + "/").setViewName("forward:" + base + "/index.html");

    // When served from a sub-path, optionally redirect the server root to it.
    // When served from the root itself, index.html is already returned for "/".
    if (!base.isEmpty() && neo.isIndexRedirectEnabled()) {
      registry.addRedirectViewController("/", base + "/");
    }
  }

}
