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
package org.operaton.bpm.webapp.neo.impl.security.filter.csrf;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import org.operaton.bpm.webapp.neo.impl.security.filter.CsrfPreventionFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same-origin verification against a configured {@code targetOrigin}.
 *
 * <p>A browser sends the literal string {@code null} as the Origin header for
 * privacy-sensitive contexts (sandboxed iframes, redirects across origins). Parsing that
 * as a URL threw, and because the exception escaped {@code doFilter} the caller saw a 500
 * rather than the 403 the filter intends. These tests pin the rejection down to a status
 * code so the filter cannot regress into leaking a stack trace.</p>
 */
class CsrfPreventionFilterOriginTest {

  private static final String TARGET_ORIGIN = "http://localhost:8080";

  private Filter csrfPreventionFilter;

  @BeforeEach
  void setup() throws ServletException {
    MockFilterConfig config = new MockFilterConfig();
    config.addInitParameter("targetOrigin", TARGET_ORIGIN);
    csrfPreventionFilter = new CsrfPreventionFilter();
    csrfPreventionFilter.init(config);
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "not a uri", "http://host:port", "://missing-scheme"})
  void shouldRejectUnparseableOriginWithForbidden(String origin) throws Exception {
    MockHttpServletResponse response = performModifyingRequest(origin);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(response.getErrorMessage()).contains("CSRFPreventionFilter");
  }

  @Test
  void shouldRejectOriginFromAnotherHostWithForbidden() throws Exception {
    MockHttpServletResponse response = performModifyingRequest("http://evil.example:8080");

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  void shouldRejectMissingOriginAndRefererWithForbidden() throws Exception {
    MockHttpServletResponse response = performModifyingRequest(null);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
  }

  private MockHttpServletResponse performModifyingRequest(String origin) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setSession(new MockHttpSession());
    request.setContextPath("/operaton");
    request.setRequestURI("/operaton/api/engine/engine/default/task/task-id/submit-form");
    if (origin != null) {
      request.addHeader("Origin", origin);
    }

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = new MockFilterChain();
    csrfPreventionFilter.doFilter(request, response, filterChain);
    return response;
  }
}
