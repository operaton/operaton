/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.webapp.filter.authcache.it;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { FilterTestApp.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
  "operaton.bpm.admin-user.id=demo",
  "operaton.bpm.admin-user.password=demo",
  "operaton.bpm.webapp.auth.cache.time-to-live=0"
})
@DirtiesContext
class AuthCacheTTLValidateAllRequestsIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension = new HttpClientExtension();

  @LocalServerPort
  public int port;

  @Autowired
  protected IdentityService identityService;

  @Test
  void shouldRemoveCache() {
    // given
    httpClientExtension.performRequest("http://localhost:" + port + "/operaton/app/welcome/default");

    Map<String, String> headers = new HashMap<>();
    headers.put("X-XSRF-TOKEN", httpClientExtension.getHeaderXsrfToken());
    headers.put("Cookie", httpClientExtension.getSessionCookieValue());
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept", "application/json");
    httpClientExtension.performPostRequest("http://localhost:" + port +
        "/operaton/api/admin/auth/user/default/login/welcome", headers, "username=demo&password=demo");

    headers = new HashMap<>();
    headers.put("Cookie", httpClientExtension.getSessionCookieValue());
    headers.put("Accept", "application/json");
    doGetRequestToProfileEndpoint(headers);

    // assume
    assertThat(httpClientExtension.getHeader("X-Authorized-Apps"))
        .isEqualTo("admin,tasklist,welcome,cockpit");

    identityService.deleteUser("demo");

    // when
    doGetRequestToProfileEndpoint(headers);

    // then
    assertThat(httpClientExtension.getErrorResponseContent())
        .contains("\"status\":401,\"error\":\"Unauthorized\"");
  }

  protected void doGetRequestToProfileEndpoint(Map<String, String> headers) {
    String baseUrl = "http://localhost:" + port;
    String profileEndpointPath = "/operaton/api/engine/engine/default/user/demo/profile";
    httpClientExtension.performRequest( baseUrl + profileEndpointPath, headers);
  }

}
