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
import java.io.InputStream;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.operaton.bpm.webapp.impl.security.filter.SecurityFilter;
import org.operaton.bpm.webapp.impl.security.filter.util.FilterRules;

@SuppressWarnings("unused")
class ResourceLoadingSecurityFilter extends SecurityFilter implements ResourceLoaderDependingFilter {

  private ResourceLoader resourceLoader;

  private WebappProperty webappProperty;
  @Override
  @SuppressWarnings("java:S112")
  protected void loadFilterRules(FilterConfig filterConfig, String applicationPath) throws ServletException {
    String configFileName = filterConfig.getInitParameter("configFile");
    Resource resource = resourceLoader.getResource("classpath:" +webappProperty.getWebjarClasspath() + configFileName);
    InputStream configFileResource;
    try {
      configFileResource = resource.getInputStream();
    } catch (IOException e1) {
      throw new ServletException("Could not read security filter config file '%s': no such resource in servlet context.".formatted(configFileName));
    }
    try {
      filterRules = FilterRules.load(configFileResource, applicationPath);
    } catch (Exception e) {
      throw new RuntimeException("Exception while parsing '%s'".formatted(configFileName), e);
    } finally {
      IoUtil.closeSilently(configFileResource);
    }
  }

  /**
   * @return the resourceLoader
   */
  public ResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  /**
   * @param resourceLoader
   *          the resourceLoader to set
   */
  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * @return the webappProperty
   */
    public WebappProperty getWebappProperty() {
        return webappProperty;
    }

  /**
   * @param webappProperty
     *          webappProperty to set
   */
  @Override
  public void setWebappProperty(WebappProperty webappProperty) {
        this.webappProperty = webappProperty;
    }

}
