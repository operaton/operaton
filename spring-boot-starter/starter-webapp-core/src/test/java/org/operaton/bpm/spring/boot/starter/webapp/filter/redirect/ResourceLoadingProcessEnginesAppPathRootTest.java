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
package org.operaton.bpm.spring.boot.starter.webapp.filter.redirect;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.StreamUtils;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {FilterTestApp.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "operaton.bpm.webapp.application-path=/",
    "operaton.bpm.webapp.index-redirect-enabled=false"})
@DirtiesContext
class ResourceLoadingProcessEnginesAppPathRootTest {

  @RegisterExtension
  HttpClientExtension rule = new HttpClientExtension().followRedirects(true);

  @LocalServerPort
  public int port;

  @Test
  void shouldRedirectToStaticContent() throws Exception {
    // given
    // send GET request to /
    HttpURLConnection con = rule.performRequest("http://localhost:" + port + "/");

    // when
    // get content returned by the request
    String body = StreamUtils.copyToString(con.getInputStream(), UTF_8);

    // then
    assertThat(con.getResponseCode()).isEqualTo(200);
    // since index-redirect-enabled=false, Operaton should not redirect to Tasklist
    assertThat(body).doesNotContain("Tasklist")
        .doesNotContain("Operaton")
        // the static index.html from /src/test/resources/static was served instead
        // this is the default Spring Boot behavior that we document for this case
        .contains("Hello World!");
  }

}
