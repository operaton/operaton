/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.webapp.filter.session.it.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {FilterTestApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "operaton.bpm.webapp.session-cookie-path-enforcement=true",
        "server.servlet.session.cookie.name=CUSTOM_OPERATON_ID",
        "server.servlet.session.cookie.secure=true"
})
@DirtiesContext
class SessionCookiePathEnforcementCustomIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension = new HttpClientExtension();

  @LocalServerPort
  public int port;

  @BeforeEach
  void assignPort() {
    httpClientExtension.setPort(port);
  }

  @Test
  void shouldApplyAttributesToCustomCookieAlias() {
    httpClientExtension.performRequest("http://localhost:" + port + "/operaton/app/tasklist/default");

    assertThat(httpClientExtension.getCookie("JSESSIONID"))
        .as("JSESSIONID should not exist when operaton.bpm.session.cookie.name is set alias wiring may be broken")
        .isEmpty();

    String rawSetCookie = httpClientExtension.getCookie("CUSTOM_OPERATON_ID");

    assertThat(rawSetCookie)
        .as("Expected CUSTOM_OPERATON_ID cookie to be present with correct attributes")
        .isNotNull()
        .contains("Path=/operaton")
        .contains("SameSite=Lax")
        .contains("Secure");
  }
}