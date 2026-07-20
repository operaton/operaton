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
package org.operaton.bpm.spring.boot.starter.webapp.apppath;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import org.operaton.bpm.spring.boot.starter.webapp.WebappTestApp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When webapps-neo is served from a sub-path, the bare base path is canonicalized
 * to the trailing-slash variant so the SPA shell resolves. Both variants must end
 * up serving the application (HTTP 200) after following the redirect.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(
  classes = {WebappTestApp.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
  "operaton.bpm.webapp.neo.enabled=true",
  "operaton.bpm.webapp.neo.application-path=" + RequestTrailingSlashIT.MY_APP_PATH
})
class RequestTrailingSlashIT {

  protected static final String MY_APP_PATH = "/app-neo";

  final TestRestTemplate client = new TestRestTemplate();

  @LocalServerPort
  public int port;

  @Test
  void shouldServeAppPathWithAndWithoutTrailingSlash() {
    // when calling the SPA base path with and without a trailing slash
    String url = "http://localhost:" + port + MY_APP_PATH;
    ResponseEntity<String> withoutSlash = client.getForEntity(url, String.class);
    ResponseEntity<String> withSlash = client.getForEntity(url + "/", String.class);

    // then both resolve to the SPA shell (the redirect is followed for the bare path)
    assertThat(withoutSlash.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(withSlash.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

}
