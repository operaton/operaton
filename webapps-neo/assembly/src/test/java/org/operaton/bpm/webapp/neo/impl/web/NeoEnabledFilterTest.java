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

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeoEnabledFilterTest {

  @Mock
  FilterConfig filterConfig;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  FilterChain chain;

  final Map<String, String> properties = new HashMap<>();
  final Map<String, String> env = new HashMap<>();

  final NeoEnabledFilter filter = new NeoEnabledFilter() {
    @Override
    protected String getSystemProperty(String name) {
      return properties.get(name);
    }

    @Override
    protected String getEnv(String name) {
      return env.get(name);
    }
  };

  @BeforeEach
  void setUp() {
    lenient().when(filterConfig.getInitParameter(NeoEnabledFilter.DISABLED_REDIRECT_PARAM)).thenReturn("/operaton/");
    lenient().when(request.getContextPath()).thenReturn("");
  }

  @Test
  void shouldBeDisabledByDefault() throws Exception {
    filter.init(filterConfig);
    when(request.getRequestURI()).thenReturn("/tasks");

    filter.doFilter(request, response, chain);

    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    verifyNoInteractions(chain);
  }

  @Test
  void shouldRedirectRootWhenDisabled() throws Exception {
    filter.init(filterConfig);
    when(request.getRequestURI()).thenReturn("/");

    filter.doFilter(request, response, chain);

    verify(response).sendRedirect("/operaton/");
    verifyNoInteractions(chain);
  }

  @Test
  void shouldRedirectRootUnderContextPath() throws Exception {
    filter.init(filterConfig);
    when(request.getContextPath()).thenReturn("/neo");
    when(request.getRequestURI()).thenReturn("/neo");

    filter.doFilter(request, response, chain);

    verify(response).sendRedirect("/operaton/");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldReturnNotFoundWithoutRedirect(String redirect) throws Exception {
    when(filterConfig.getInitParameter(NeoEnabledFilter.DISABLED_REDIRECT_PARAM)).thenReturn(redirect);
    filter.init(filterConfig);
    when(request.getRequestURI()).thenReturn("/");

    filter.doFilter(request, response, chain);

    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  void shouldPassThroughWhenPropertyEnabled() throws Exception {
    properties.put(NeoEnabledFilter.ENABLED_PROPERTY, "true");
    filter.init(filterConfig);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyNoInteractions(response);
  }

  @Test
  void shouldPassThroughWhenEnvEnabled() throws Exception {
    env.put(NeoEnabledFilter.ENABLED_ENV, " TRUE ");
    filter.init(filterConfig);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void shouldPreferPropertyOverEnv() {
    properties.put(NeoEnabledFilter.ENABLED_PROPERTY, "false");
    env.put(NeoEnabledFilter.ENABLED_ENV, "true");

    assertThat(filter.isEnabled()).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldFallBackToEnvForBlankProperty(String value) {
    properties.put(NeoEnabledFilter.ENABLED_PROPERTY, value);
    env.put(NeoEnabledFilter.ENABLED_ENV, "true");

    assertThat(filter.isEnabled()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"yes", "1", "on"})
  void shouldTreatOtherValuesAsDisabled(String value) {
    properties.put(NeoEnabledFilter.ENABLED_PROPERTY, value);

    assertThat(filter.isEnabled()).isFalse();
  }
}
