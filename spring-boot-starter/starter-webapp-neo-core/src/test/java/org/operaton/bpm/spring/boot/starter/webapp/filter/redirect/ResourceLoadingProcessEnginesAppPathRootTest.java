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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {FilterTestApp.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "operaton.bpm.webapp.neo.enabled=true",
    "operaton.bpm.webapp.neo.application-path="})
@DirtiesContext
class ResourceLoadingProcessEnginesAppPathRootTest {

  @RegisterExtension
  HttpClientExtension rule = new HttpClientExtension().followRedirects(true);

  @LocalServerPort
  public int port;

  @Test
  void shouldServeSpaIndexAtRoot() throws Exception {
    // given
    // webapps-neo is served from the application root
    // when
    // send GET request to /
    HttpURLConnection con = rule.performRequest("http://localhost:" + port + "/");
    String body = IOUtils.toString(con.getInputStream(), UTF_8);

    // then
    // the SPA shell (index.html) is served at the root, not the static placeholder
    assertThat(con.getResponseCode()).isEqualTo(200);
    assertThat(body).contains("<title>Operaton</title>")
        .doesNotContain("Hello World!");
  }

}
