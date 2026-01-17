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
package org.operaton.bpm.webapp.impl.security.filter.csrf;
import java.util.List;
import java.io.IOException;
import java.util.Collection;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import org.operaton.bpm.webapp.impl.security.filter.CsrfPreventionFilter;

import static org.operaton.bpm.webapp.impl.security.filter.util.CookieConstants.SET_COOKIE_HEADER_NAME;
import static org.operaton.bpm.webapp.impl.security.filter.util.CsrfConstants.CSRF_PATH_FIELD_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nikola Koevski
 */
public class CsrfPreventionFilterTest {

  protected static final String SERVICE_PATH = "/operaton";
  protected static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
  protected static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
  protected static final String CSRF_HEADER_REQUIRED = "Required";

  protected Filter csrfPreventionFilter;

  protected String nonModifyingRequestUrl;
  protected String modifyingRequestUrl;

  // flags a modifying request (POST/PUT/DELETE) as a non-modifying one
  protected boolean isModifyingFetchRequest;

  public static Collection<Object[]> getRequestUrls() {
    return List.of(new Object[][]{
      {"/app/cockpit/default/", "/api/admin/auth/user/default/login/cockpit", true},
      {"/app/cockpit/engine1/", "/api/admin/auth/user/engine1/login/cockpit", true},

      {"/app/cockpit/default/", "/api/engine/engine/default/history/task/count", false},
      {"/app/cockpit/engine1/", "/api/engine/engine/engine1/history/task/count", false},

      {"/app/tasklist/default/", "/api/admin/auth/user/default/login/tasklist", true},
      {"/app/tasklist/engine1/", "/api/admin/auth/user/engine1/login/tasklist", true},

      {"/app/tasklist/default/", "api/engine/engine/default/task/task-id/submit-form", false},
      {"/app/tasklist/engine2/", "api/engine/engine/engine2/task/task-id/submit-form", false},

      {"/app/admin/default/", "/api/admin/auth/user/default/login/admin", true},
      {"/app/admin/engine1/", "/api/admin/auth/user/engine1/login/admin", true},

      {"/app/admin/default/", "api/admin/setup/default/user/create", false},
      {"/app/admin/engine3/", "api/admin/setup/engine3/user/create", false},

      {"/app/welcome/default/", "/api/admin/auth/user/default/login/welcome", true},
      {"/app/welcome/engine1/", "/api/admin/auth/user/engine1/login/welcome", true}
    });
  }

  public void initCsrfPreventionFilterTest(String nonModifyingRequestUrl, String modifyingRequestUrl, boolean isModifyingFetchRequest) {
    this.nonModifyingRequestUrl = nonModifyingRequestUrl;
    this.modifyingRequestUrl = modifyingRequestUrl;
    this.isModifyingFetchRequest = isModifyingFetchRequest;
  }

  @BeforeEach
  void setup() throws Exception {
    setupFilter();
  }

  protected void setupFilter() throws ServletException {
    MockFilterConfig config = new MockFilterConfig();
    csrfPreventionFilter = new CsrfPreventionFilter();
    csrfPreventionFilter.init(config);
  }

  protected void applyFilter(MockHttpServletRequest request, MockHttpServletResponse response) throws IOException, ServletException {
    FilterChain filterChain = new MockFilterChain();
    csrfPreventionFilter.doFilter(request, response, filterChain);
  }

  @MethodSource("getRequestUrls")
  @ParameterizedTest
  void nonModifyingRequestTokenGeneration(String nonModifyingRequestUrl, String modifyingRequestUrl, boolean isModifyingFetchRequest) throws Exception {
    initCsrfPreventionFilterTest(nonModifyingRequestUrl, modifyingRequestUrl, isModifyingFetchRequest);
    MockHttpServletResponse response = performNonModifyingRequest(nonModifyingRequestUrl, new MockHttpSession());

    String cookieToken = response.getHeader(SET_COOKIE_HEADER_NAME);
    String headerToken = response.getHeader(CSRF_HEADER_NAME);

    assertThat(cookieToken).isNotNull();
    assertThat(headerToken).isNotNull();

    String regex = CSRF_COOKIE_NAME + "=[A-Z0-9]{32}" + CSRF_PATH_FIELD_NAME + getCookiePath(SERVICE_PATH) + ";SameSite=Lax";
    assertThat(cookieToken).matches(regex.replace(";", ";\\s*"));

    assertThat(headerToken).as("No HTTP Header Token!").isNotEmpty();
    assertThat(cookieToken).contains(headerToken);
  }

  @MethodSource("getRequestUrls")
  @ParameterizedTest
  void nonModifyingRequestTokenGenerationWithRootContextPath(String nonModifyingRequestUrl, String modifyingRequestUrl, boolean isModifyingFetchRequest) throws Exception {
    initCsrfPreventionFilterTest(nonModifyingRequestUrl, modifyingRequestUrl, isModifyingFetchRequest);
    // given
    MockHttpSession session = new MockHttpSession();
    MockHttpServletRequest nonModifyingRequest = getMockedRequest();
    nonModifyingRequest.setMethod("GET");
    nonModifyingRequest.setSession(session);

    // set root context path in request
    nonModifyingRequest.setRequestURI("/"  + nonModifyingRequestUrl);
    nonModifyingRequest.setContextPath("");

    // when
    MockHttpServletResponse response = new MockHttpServletResponse();
    applyFilter(nonModifyingRequest, response);

    // then
    String cookieToken = response.getHeader(SET_COOKIE_HEADER_NAME);
    String headerToken = response.getHeader(CSRF_HEADER_NAME);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    assertThat(cookieToken).isNotNull();
    assertThat(headerToken).isNotNull();

    String regex = CSRF_COOKIE_NAME + "=[A-Z0-9]{32}" + CSRF_PATH_FIELD_NAME + getCookiePath("") + ";SameSite=Lax";
    assertThat(cookieToken).matches(regex.replace(";", ";\\s*"));

    assertThat(headerToken).as("No HTTP Header Token!").isNotEmpty();
    assertThat(cookieToken).contains(headerToken);
  }

  @MethodSource("getRequestUrls")
  @ParameterizedTest
  void consecutiveNonModifyingRequestTokens(String nonModifyingRequestUrl, String modifyingRequestUrl, boolean isModifyingFetchRequest) throws Exception {
    initCsrfPreventionFilterTest(nonModifyingRequestUrl, modifyingRequestUrl, isModifyingFetchRequest);
    MockHttpSession session = new MockHttpSession();

    // first non-modifying request
    MockHttpServletResponse firstResponse = performNonModifyingRequest(nonModifyingRequestUrl, session);
    // second non-modifying request
    MockHttpServletResponse secondResponse = performNonModifyingRequest(nonModifyingRequestUrl, session);

    String headerToken1 = firstResponse.getHeader(CSRF_HEADER_NAME);
    String headerToken2 = secondResponse.getHeader(CSRF_HEADER_NAME);

    assertThat(headerToken1).isNotNull();
    assertThat(headerToken2).isNull();
  }

  @MethodSource("getRequestUrls")
  @ParameterizedTest
  void modifyingRequestTokenValidation(String nonModifyingRequestUrl, String modifyingRequestUrl, boolean isModifyingFetchRequest) throws Exception {
    initCsrfPreventionFilterTest(nonModifyingRequestUrl, modifyingRequestUrl, isModifyingFetchRequest);
    MockHttpSession session = new MockHttpSession();

    // first a non-modifying request to obtain a token
    MockHttpServletResponse nonModifyingResponse = performNonModifyingRequest(nonModifyingRequestUrl, session);

    if (!isModifyingFetchRequest) {
      String token = nonModifyingResponse.getHeader(CSRF_HEADER_NAME);
      HttpServletResponse modifyingResponse = performModifyingRequest(token, session);
      assertThat(modifyingResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }
  }

  @MethodSource("getRequestUrls")
  @ParameterizedTest
  void modifyingRequestInvalidToken(String nonModifyingRequestUrl, String modifyingRequestUrl, boolean isModifyingFetchRequest) throws Exception {
    initCsrfPreventionFilterTest(nonModifyingRequestUrl, modifyingRequestUrl, isModifyingFetchRequest);
    MockHttpSession session = new MockHttpSession();
    performNonModifyingRequest(nonModifyingRequestUrl, session);

    if (!isModifyingFetchRequest) {
      // invalid header token
      MockHttpServletResponse response = performModifyingRequest("invalid header token", session);
      assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
      assertThat(response.getErrorMessage()).isEqualTo("CSRFPreventionFilter: Invalid HTTP Header Token.");

      // no token in header
      MockHttpServletResponse response2 = new MockHttpServletResponse();
      MockHttpServletRequest modifyingRequest = getMockedRequest();
      modifyingRequest.setMethod("POST");
      modifyingRequest.setSession(session);
      modifyingRequest.setRequestURI(SERVICE_PATH  + modifyingRequestUrl);
      modifyingRequest.setContextPath(SERVICE_PATH);

      applyFilter(modifyingRequest, response2);
      assertThat(response2.getHeader(CSRF_HEADER_NAME)).isEqualTo(CSRF_HEADER_REQUIRED);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
      assertThat(response2.getErrorMessage()).isEqualTo("CSRFPreventionFilter: Token provided via HTTP Header is absent/empty.");
      assertThat(session.getId()).isNotEqualTo(modifyingRequest.getSession().getId());
    }
  }

  protected MockHttpServletResponse performNonModifyingRequest(String requestUrl, MockHttpSession session) throws IOException, ServletException {
    MockHttpServletResponse response = new MockHttpServletResponse();

    MockHttpServletRequest nonModifyingRequest = getMockedRequest();
    nonModifyingRequest.setMethod("GET");
    nonModifyingRequest.setSession(session);
    nonModifyingRequest.setRequestURI(SERVICE_PATH  + requestUrl);
    nonModifyingRequest.setContextPath(SERVICE_PATH);

    applyFilter(nonModifyingRequest, response);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    return response;
  }

  protected MockHttpServletResponse performModifyingRequest(String token, MockHttpSession session) throws IOException, ServletException {
    MockHttpServletResponse response = new MockHttpServletResponse();

    MockHttpServletRequest modifyingRequest = getMockedRequest();

    modifyingRequest.setMethod("POST");
    modifyingRequest.setSession(session);
    modifyingRequest.setRequestURI(SERVICE_PATH  + modifyingRequestUrl);
    modifyingRequest.setContextPath(SERVICE_PATH);

    modifyingRequest.addHeader(CSRF_HEADER_NAME, token);
    Cookie[] cookies = {new Cookie(CSRF_COOKIE_NAME, token)};
    modifyingRequest.setCookies(cookies);

    applyFilter(modifyingRequest, response);

    return response;
  }

  protected MockHttpServletRequest getMockedRequest() {
    return new MockHttpServletRequest();
  }

  protected String getCookiePath(String contextPath) {
    if (contextPath.isEmpty()) {
      return "/";

    } else {
      return contextPath;

    }
  }

}
