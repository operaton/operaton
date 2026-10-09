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
package org.operaton.bpm.spring.boot.starter.webapp.filter.headersec.it;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static org.operaton.bpm.webapp.neo.impl.security.filter.headersec.provider.impl.ContentSecurityPolicyProvider.HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A request the security filter turns away must still carry the security headers, and the
 * session cookie minted along the way must still carry its SameSite attribute.
 *
 * <p>Both used to be missing: the header and session-cookie filters were registered after the
 * rejecting filters, so a 401 left the chain before either could touch the response. That is
 * the case with the least trustworthy caller, which is exactly where the headers matter.</p>
 */
@SpringBootTest(classes = {FilterTestApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class RejectedRequestHeaderSecurityIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension = new HttpClientExtension();

  @LocalServerPort
  public int port;

  @BeforeEach
  void assignPort() {
    httpClientExtension.setPort(port);
  }

  @Test
  void shouldSendSecurityHeadersWithARejectedRequest() throws Exception {
    // when asking for an engine resource with no session
    HttpURLConnection connection = httpClientExtension.performRequest(
        "http://localhost:" + port + "/app-neo/api/engine/engine/default/user");

    // then it is refused
    assertThat(connection.getResponseCode()).isEqualTo(401);

    // and the refusal is still decorated
    assertThat(httpClientExtension.getHeader(HEADER_NAME))
        .as("Content-Security-Policy on a 401").isNotNull();
    assertThat(httpClientExtension.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(httpClientExtension.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
  }

  @Test
  void shouldApplySameSiteToASessionCookieMintedOnARejectedRequest() {
    // when
    httpClientExtension.performRequest(
        "http://localhost:" + port + "/app-neo/api/engine/engine/default/user");

    // then
    String sessionCookie = httpClientExtension.getCookie("JSESSIONID");
    assertThat(sessionCookie).as("a session cookie was set").isNotEmpty();
    assertThat(sessionCookie)
        .as("SameSite on the session cookie of a rejected request")
        .contains("SameSite=Lax");
  }
}
