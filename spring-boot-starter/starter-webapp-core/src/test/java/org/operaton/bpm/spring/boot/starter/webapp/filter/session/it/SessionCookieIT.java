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
package org.operaton.bpm.spring.boot.starter.webapp.filter.session.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {FilterTestApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.web.error.include-message=always"})
@DirtiesContext
class SessionCookieIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension = new HttpClientExtension();

  @LocalServerPort
  public int port;

  @BeforeEach
  void assignPort() {
    httpClientExtension.setPort(port);
  }

  @Test
  void shouldSetCookieWebapp() {
    httpClientExtension.performRequest("http://localhost:" + port + "/operaton/app/tasklist/default");

    String sessionCookieValue = httpClientExtension.getSessionCookie();

    assertThat(sessionCookieValue).matches(httpClientExtension.getSessionCookieRegex("Lax"));
  }

  @Test
  void shouldSetCookieWebappRest() {
    httpClientExtension.performRequest("http://localhost:" + port + "/operaton/api/engine/engine/");

    String sessionCookieValue = httpClientExtension.getSessionCookie();

    assertThat(sessionCookieValue).matches(httpClientExtension.getSessionCookieRegex("Lax"));
  }

}
