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
package org.operaton.bpm.spring.boot.starter.webapp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppendTrailingSlashFilterTest {

  private final AppendTrailingSlashFilter filter = new AppendTrailingSlashFilter();

  @Test
  void shouldRedirectLocalPathWithTrailingSlash() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/operaton/app");

    filter.doFilter(request, response, chain);

    verify(response).sendRedirect("/operaton/app/");
    verify(chain, never()).doFilter(request, response);
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://example.org/app", "//example.org/app", "app" })
  void shouldContinueFilterChainForUnsafeRedirectTargets(String requestURI) throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn(requestURI);

    filter.doFilter(request, response, chain);

    verify(response, never()).sendRedirect(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void shouldContinueFilterChainForMissingRequestUri() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(response, never()).sendRedirect(any());
    verify(chain).doFilter(request, response);
  }

}
