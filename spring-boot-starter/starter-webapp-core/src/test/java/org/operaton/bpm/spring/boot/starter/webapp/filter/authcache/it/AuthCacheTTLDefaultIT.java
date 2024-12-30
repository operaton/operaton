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
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientRule;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.junit.After;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { FilterTestApp.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
  "operaton.bpm.admin-user.id=demo",
  "operaton.bpm.admin-user.password=demo"
})
@DirtiesContext
public class AuthCacheTTLDefaultIT {

  @Rule
  public HttpClientRule httpClientRule = new HttpClientRule();

  @LocalServerPort
  public int port;

  @Autowired
  protected IdentityService identityService;

  @After
  public void reset() {
    ClockUtil.reset();
  }

  @Test
  public void shouldRemoveCache() {
    // given
    httpClientRule.performRequest("http://localhost:" + port + "/operaton/app/welcome/default");

    Map<String, String> headers = new HashMap<>();
    headers.put("X-XSRF-TOKEN", httpClientRule.getHeaderXsrfToken());
    headers.put("Cookie", httpClientRule.getSessionCookieValue());
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept", "application/json");
    httpClientRule.performPostRequest("http://localhost:" + port +
        "/operaton/api/admin/auth/user/default/login/welcome", headers, "username=demo&password=demo");

    headers = new HashMap<>();
    headers.put("Cookie", httpClientRule.getSessionCookieValue());
    headers.put("Accept", "application/json");
    doGetRequestToProfileEndpoint(headers);

    assertThat(httpClientRule.getHeader("X-Authorized-Apps"))
        .isEqualTo("admin,tasklist,welcome,cockpit");

    identityService.deleteUser("demo");
    doGetRequestToProfileEndpoint(headers);

    // assume
    assertThat(httpClientRule.getErrorResponseContent())
        .contains("User with id demo does not exist");

    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 1000 * 60 * 5));

    // when
    doGetRequestToProfileEndpoint(headers);

    // then
    assertThat(httpClientRule.getErrorResponseContent())
        .contains("\"status\":401,\"error\":\"Unauthorized\"");
  }

  protected void doGetRequestToProfileEndpoint(Map<String, String> headers) {
    String baseUrl = "http://localhost:" + port;
    String profileEndpointPath = "/operaton/api/engine/engine/default/user/demo/profile";
    httpClientRule.performRequest( baseUrl + profileEndpointPath, headers);
  }

}
