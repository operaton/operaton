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

import jakarta.servlet.ServletException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.webapp.impl.util.ServletContextUtil;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

import java.io.IOException;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.webapp.impl.security.filter.util.CookieConstants.SET_COOKIE_HEADER_NAME;
import static org.operaton.bpm.webapp.impl.security.filter.util.CsrfConstants.CSRF_PATH_FIELD_NAME;

public class CsrfPreventionFilterAppPathTest extends CsrfPreventionFilterTest {

  protected static final String MY_APP_PATH = "/my/application/path";

  protected MockServletContext mockServletContext;

  public static Collection<Object[]> getRequestUrls() {
    return CsrfPreventionFilterTest.getRequestUrls();
  }

    @BeforeEach
  @Override
  public void setup() throws Exception {
    mockServletContext = new MockServletContext();
    ServletContextUtil.setAppPath(MY_APP_PATH, mockServletContext);
    super.setup();
  }

  @MethodSource("getRequestUrls")
  @ParameterizedTest
  void shouldCheckNonModifyingRequestTokenGenerationWithRootContextPathAndEmptyAppPath()
    throws IOException, ServletException {
    // given
    ServletContextUtil.setAppPath("", mockServletContext);

    MockHttpSession session = new MockHttpSession();
    MockHttpServletRequest nonModifyingRequest = new MockHttpServletRequest(mockServletContext);
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

    assertThat(cookieToken).isNotNull().isNotEmpty();
    assertThat(headerToken).isNotNull().isNotEmpty();

    String regex = CSRF_COOKIE_NAME + "=[A-Z0-9]{32}" + CSRF_PATH_FIELD_NAME + "/;SameSite=Lax";
    assertThat(cookieToken)
      .matches(regex.replace(";", ";\\s*"))
      .contains(headerToken);
  }

  @Override
  protected String getCookiePath(String contextPath) {
    return super.getCookiePath(contextPath + MY_APP_PATH);
  }

  @Override
  protected MockHttpServletRequest getMockedRequest() {
    return new MockHttpServletRequest(mockServletContext);
  }

}
