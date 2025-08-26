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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.operaton.bpm.webapp.impl.engine.ProcessEnginesFilter;

public class ResourceLoadingProcessEnginesFilter extends ProcessEnginesFilter implements ResourceLoaderDependingFilter {

  protected static final String DEFAULT_REDIRECT_APP = "tasklist";

  protected ResourceLoader resourceLoader;
  protected WebappProperty webappProperty;

  @Override
  protected void applyFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
    String contextPath = request.getContextPath();
    String requestUri = request.getRequestURI().substring(contextPath.length());
    String applicationPath = webappProperty.getApplicationPath();

    requestUri = trimChar(requestUri, '/');
    String appPath = trimChar(applicationPath, '/');
    if (requestUri.equals(appPath) && (!requestUri.isEmpty() || webappProperty.isIndexRedirectEnabled())) {
      // only redirect from index ("/") if index redirect is enabled
      response.sendRedirect("%s%s/app/%s/".formatted(contextPath, applicationPath, DEFAULT_REDIRECT_APP));
      return;
    }

    super.applyFilter(request, response, chain);
  }

  @Override
  protected String getWebResourceContents(String name) throws IOException {
    Resource resource = resourceLoader.getResource("classpath:"+webappProperty.getWebjarClasspath() + name);
    try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
        StringWriter writer = new StringWriter();
        String line;

        while ((line = reader.readLine()) != null) {
          writer.write(line);
          writer.append("\n");
        }

        return writer.toString();
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

  /**
   * @param input - String to trim
   * @param charachter - Char to trim
   * @return the trimmed String
   */
  protected String trimChar(String input, char charachter) {
    input = StringUtils.trimLeadingCharacter(input, charachter);
    input = StringUtils.trimTrailingCharacter(input, charachter);

    return input;
  }
}
