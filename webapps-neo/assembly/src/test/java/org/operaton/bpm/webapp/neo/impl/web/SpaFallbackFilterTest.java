/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.webapp.neo.impl.web;

import java.net.URI;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaFallbackFilterTest {

  @Mock
  FilterConfig filterConfig;

  @Mock
  ServletContext servletContext;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  FilterChain chain;

  @Mock
  RequestDispatcher dispatcher;

  final SpaFallbackFilter filter = new SpaFallbackFilter();

  @BeforeEach
  void setUp() {
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    filter.init(filterConfig);
    lenient().when(request.getContextPath()).thenReturn("");
    lenient().when(request.getMethod()).thenReturn("GET");
    lenient().when(request.getRequestDispatcher("/index.html")).thenReturn(dispatcher);
  }

  @Test
  void shouldForwardClientRouteToIndex() throws Exception {
    when(request.getRequestURI()).thenReturn("/processes/invoice");

    filter.doFilter(request, response, chain);

    verify(dispatcher).forward(request, response);
    verify(chain, never()).doFilter(request, response);
  }

  @Test
  void shouldForwardClientRouteUnderContextPath() throws Exception {
    when(request.getContextPath()).thenReturn("/neo");
    when(request.getRequestURI()).thenReturn("/neo/tasks");

    filter.doFilter(request, response, chain);

    verify(servletContext).getResource("/tasks");
    verify(dispatcher).forward(request, response);
  }

  @Test
  void shouldServeExistingResource() throws Exception {
    when(request.getRequestURI()).thenReturn("/plugins");
    when(servletContext.getResource("/plugins")).thenReturn(URI.create("file:/plugins").toURL());

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "DELETE"})
  void shouldNotForwardWriteRequests(String method) throws Exception {
    when(request.getMethod()).thenReturn(method);
    when(request.getRequestURI()).thenReturn("/tasks");

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/", "/api", "/api/", "/api/admin/auth/user/default", "/assets/missing.js",
      "/config.json"})
  void shouldNotTreatAsClientRoute(String path) {
    assertThat(filter.isClientRoute(path)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/tasks", "/processes/invoice/", "/apis", "/admin/users"})
  void shouldTreatAsClientRoute(String path) {
    assertThat(filter.isClientRoute(path)).isTrue();
  }
}
