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
package org.operaton.bpm.webapp.impl.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.operaton.commons.utils.IoUtil;

/**
 * A {@link Filter} implementation that can be used to realize basic templating.
 *
 * @author nico.rehwaldt
 */
public abstract class AbstractTemplateFilter implements Filter {

  private FilterConfig filterConfig;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
  }

  @Override
  public void destroy() {
    filterConfig = null;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    applyFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
  }

  /**
   * Apply the filter to the given request/response.
   *
   * <p>
   * This method must be provided by subclasses to perform actual work.
   * </p>
   *
   * @param request
   * @param response
   * @param chain
   *
   * @throws IOException
   * @throws ServletException
   */
  protected abstract void applyFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;

  /**
   * Returns true if the given web resource exists.
   *
   * @param name
   * @return
   */
  protected boolean hasWebResource(String name) {
    try {
      URL resource = filterConfig.getServletContext().getResource(name);
      return resource != null;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  /**
   * Returns the string contents of a web resource with the given name.
   *
   * <p>
   * The resource must be static and text based.
   * </p>
   *
   * @param name the name of the resource
   *
   * @return the resource contents
   *
   * @throws IOException when the resource was not found or could not be read
   */
  protected String getWebResourceContents(String name) throws IOException {
    try (var is = filterConfig.getServletContext().getResourceAsStream(name)) {
      if (is == null) {
        throw new IOException("Resource not found: " + name);
      }
      return IoUtil.inputStreamAsString(is);
    }
  }
}
