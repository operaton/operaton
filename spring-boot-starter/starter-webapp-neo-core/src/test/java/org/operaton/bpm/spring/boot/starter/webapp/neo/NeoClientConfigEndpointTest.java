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
package org.operaton.bpm.spring.boot.starter.webapp.neo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The SPA fetches this document before it can authenticate, so it has to be
 * reachable anonymously and must never be cached.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(classes = {FilterTestApp.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "operaton.bpm.webapp.neo.enabled=true",
    "operaton.bpm.webapp.neo.application-path=/app-neo"})
@DirtiesContext
class NeoClientConfigEndpointTest {

  @Autowired
  protected TestRestTemplate restClient;

  @Test
  void shouldServeConfigAnonymously() {
    ResponseEntity<String> response = restClient.getForEntity("/app-neo/config.json", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"authMode\"");
  }

  @Test
  void shouldDefaultToBasicAuthentication() {
    // no OAuth2 client registrations are configured, so the web apps ask for
    // a username and password rather than offering an SSO button
    ResponseEntity<String> response = restClient.getForEntity("/app-neo/config.json", String.class);

    assertThat(response.getBody()).contains("\"authMode\":\"basic\"");
    assertThat(response.getBody()).doesNotContain("\"oauth\"");
  }

  @Test
  void shouldReportNoUserWithoutASession() {
    ResponseEntity<String> response = restClient.getForEntity("/app-neo/config.json", String.class);

    assertThat(response.getBody()).doesNotContain("\"user\"");
  }

  @Test
  void shouldOfferTheApplicationsOwnOriginAsBackend() {
    ResponseEntity<String> response = restClient.getForEntity("/app-neo/config.json", String.class);

    assertThat(response.getBody()).contains("\"backends\":[{\"name\":\"Operaton\",\"url\":\"\"}]");
  }

  @Test
  void shouldNotBeCached() {
    // it reports whether the caller currently has a session
    ResponseEntity<String> response = restClient.getForEntity("/app-neo/config.json", String.class);

    assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("no-store");
  }
}
