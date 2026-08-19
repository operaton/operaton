/*
 * Copyright 2026 the Operaton contributors.
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

import java.net.HttpURLConnection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import org.operaton.bpm.spring.boot.starter.webapp.WebappTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The {@code /api} namespace is served by webapps-neo's own JAX-RS applications.
 *
 * <p>It used to be served by nothing: the initializer registered the filter chain
 * over {@code /api/*} but no servlet behind it, so every request there fell through
 * to the SPA's index.html catch-all. The login and logout endpoints were therefore
 * unreachable and the SPA authenticated against the standalone {@code /engine-rest}
 * deployment instead, which is why it had to keep credentials to re-send.</p>
 *
 * <p>The catch-all answers every path with 200 and index.html, so any assertion that
 * an {@code /api} path returns real data, or refuses the caller, distinguishes the two.
 * Content type alone does not: the security filter rejects unauthenticated {@code /api}
 * requests with JSON before a servlet would ever be consulted.</p>
 */
@SpringBootTest(
    classes = {WebappTestApp.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class NeoApiNamespaceIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension = new HttpClientExtension();

  @LocalServerPort
  public int port;

  private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

  private String baseUrl;

  @BeforeEach
  void assignPort() {
    httpClientExtension.setPort(port);
    baseUrl = "http://localhost:" + port + "/app-neo";
  }

  @Test
  void shouldServeTheEngineApi() throws Exception {
    // given the engine application mounted at {base}/api/engine/*, and a path
    // securityFilterRules.json allows anonymously

    // when
    HttpURLConnection connection =
        httpClientExtension.performRequest(baseUrl + "/api/engine/engine/");

    // then the engine resource answered, rather than the SPA's index.html catch-all.
    // Which engines come back depends on what the surrounding context registered, so
    // assert on the shape: a JSON array is the engine list resource replying.
    assertThat(connection.getResponseCode()).isEqualTo(200);
    assertThat(httpClientExtension.getHeader("Content-Type")).contains("application/json");
    assertThat(httpClientExtension.getContent()).startsWith("[");
  }

  @Test
  void shouldRejectLoginWithoutACsrfToken() {
    // given no prior GET, so the caller holds no XSRF token

    // when
    Throwable thrown = catchThrowable(() -> httpClientExtension.performPostRequest(
        baseUrl + "/api/admin/auth/user/default/login/neo",
        Map.of("Content-Type", FORM_CONTENT_TYPE),
        "username=nobody&password=wrong"));

    // then the CSRF filter turns it away before the resource sees it. This is exactly
    // why the SPA's logout never worked: it POSTs without the header and discards the
    // result, so the user appeared logged out while the server session lived on.
    assertThat(thrown).hasMessageContaining("403");
  }

  @Test
  void shouldServeTheAuthenticationEndpointWithACsrfToken() {
    // given a token obtained the way a browser would, from a prior GET
    httpClientExtension.performRequest(baseUrl + "/api/engine/engine/");
    String token = httpClientExtension.getXsrfTokenHeader();
    String sessionCookie = httpClientExtension.getCookieValue("JSESSIONID");
    assertThat(token).as("XSRF token from the first request").isNotEmpty();

    // when logging in with credentials no engine will accept
    Throwable thrown = catchThrowable(() -> httpClientExtension.performPostRequest(
        baseUrl + "/api/admin/auth/user/default/login/neo",
        Map.of("Content-Type", FORM_CONTENT_TYPE,
               "X-XSRF-TOKEN", token,
               "Cookie", sessionCookie + "; XSRF-TOKEN=" + token),
        "username=nobody&password=wrong"));

    // then the resource itself answered and refused. The exact code depends on whether
    // the surrounding context registered the engine (401 for bad credentials, 400 for
    // an unknown engine) — what matters is that something rejected it, because before
    // the servlet existed this path fell through to the SPA shell and returned 200
    // with index.html, which throws nothing at all.
    assertThat(thrown)
        .as("the auth resource should answer, not the index.html catch-all")
        .hasMessageMatching("(?s).*response code: 4\\d\\d.*");
  }

  @Test
  void shouldStillServeTheSpaShellOutsideTheApiNamespace() {
    // given a client-side route

    // when
    httpClientExtension.performRequest(baseUrl + "/processes");

    // then the catch-all is intact for everything that is not /api
    assertThat(httpClientExtension.getHeader("Content-Type")).contains("text/html");
  }

}
