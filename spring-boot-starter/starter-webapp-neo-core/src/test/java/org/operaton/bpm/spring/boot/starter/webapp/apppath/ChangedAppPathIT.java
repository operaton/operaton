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
package org.operaton.bpm.spring.boot.starter.webapp.apppath;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import org.operaton.bpm.spring.boot.starter.webapp.WebappTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static org.operaton.bpm.webapp.impl.security.filter.headersec.provider.impl.ContentSecurityPolicyProvider.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that webapps-neo can be served from a configurable sub-path: the
 * plugin API namespace, the webapp filter chain and the root redirect all move
 * with the configured {@code operaton.bpm.webapp.neo.application-path}.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(
  classes = {WebappTestApp.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
  "operaton.bpm.webapp.neo.enabled=true",
  "operaton.bpm.webapp.neo.application-path=" + ChangedAppPathIT.MY_APP_PATH
})
class ChangedAppPathIT {

  protected static final String MY_APP_PATH = "/my/application/path";

  @RegisterExtension
  HttpClientExtension httpClientExtension = new HttpClientExtension();

  @LocalServerPort
  public int port;

  @BeforeEach
  void assignPort() {
    httpClientExtension.setPort(port);
  }

  @Autowired
  protected TestRestTemplate restClient;

  @Test
  void shouldCheckPresenceOfCsrfPreventionFilter() {
    // when
    httpClientExtension.performRequest("http://localhost:" + port + MY_APP_PATH +
        "/api/engine/engine/");

    // then
    String xsrfCookieValue = httpClientExtension.getXsrfCookie();
    String xsrfTokenHeader = httpClientExtension.getXsrfTokenHeader();

    assertThat(xsrfCookieValue).matches("XSRF-TOKEN=[A-Z0-9]{32};" +
        "Path=" + MY_APP_PATH + ";SameSite=Lax");
    assertThat(xsrfTokenHeader).matches("[A-Z0-9]{32}");

    assertThat(xsrfCookieValue).contains(xsrfTokenHeader);
  }

  @Test
  void shouldRedirectRootToAppPath() {
    // when
    httpClientExtension.performRequest("http://localhost:" + port + "/");

    // then
    assertThat(httpClientExtension.getHeader("Location")).isEqualTo("http://localhost:" + port +
        MY_APP_PATH + "/");
  }

  @Test
  void shouldCheckPresenceOfHeaderSecurityFilter() {
    // when
    ResponseEntity<String> response = restClient.getForEntity(MY_APP_PATH +
        "/api/engine/engine/", String.class);

    // then
    List<String> contentSecurityPolicyHeaders = response.getHeaders()
        .get(HEADER_NAME);

    String expectedHeaderPattern = HEADER_DEFAULT_VALUE.replace(HEADER_NONCE_PLACEHOLDER, "'nonce-([-_a-zA-Z\\d]*)'");
    assertThat(contentSecurityPolicyHeaders).anyMatch(val -> val.matches(expectedHeaderPattern));
  }

  @Test
  void shouldCheckPresenceOfRestApi() {
    // when
    ResponseEntity<String> response = restClient.getForEntity(MY_APP_PATH +
        "/api/engine/engine/", String.class);

    // then
    assertThat(response.getBody()).isEqualTo("[{\"name\":\"default\"}]");
  }

  @Test
  void shouldCheckPresenceOfSecurityFilter() {
    // when
    ResponseEntity<String> response = restClient.getForEntity(MY_APP_PATH +
        "/api/engine/engine/default/group/count", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
