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
package org.operaton.bpm.spring.boot.starter.webapp.filter.redirect;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {FilterTestApp.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "operaton.bpm.webapp.neo.enabled=true",
    "operaton.bpm.webapp.neo.application-path=/app-neo",
    "operaton.bpm.webapp.neo.index-redirect-enabled=false",
    "operaton.bpm.admin-user.id=admin"})
@DirtiesContext
class ResourceLoadingProcessEnginesAppPathOperatonTest {

  @RegisterExtension
  HttpClientExtension rule = new HttpClientExtension().followRedirects(true);

  @LocalServerPort
  public int port;

  @Test
  void shouldNotRedirectRootWhenIndexRedirectDisabled() {
    // when
    // the SPA is served from /app-neo and the root index redirect is disabled
    HttpURLConnection con = rule.performRequest("http://localhost:" + port + "/");

    // then
    // "/" is not redirected to the SPA sub-path
    assertThat(con.getURL()).hasToString("http://localhost:" + port + "/");
  }
}
