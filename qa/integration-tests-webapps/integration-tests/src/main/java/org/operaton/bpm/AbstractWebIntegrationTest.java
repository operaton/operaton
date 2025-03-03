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
package org.operaton.bpm;

import org.operaton.bpm.util.TestUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.chrome.ChromeDriverService;

/**
 *
 * @author Daniel Meyer
 * @author Roman Smirnov
 *
 */
public abstract class AbstractWebIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(AbstractWebIntegrationTest.class.getName());
  
  protected static final String TASKLIST_PATH = "app/tasklist/default/";
  
  protected static final String COOKIE_HEADER = "Cookie";
  protected static final String X_XSRF_TOKEN_HEADER = "X-XSRF-TOKEN";

  protected static final String JSESSIONID_IDENTIFIER = "JSESSIONID=";
  protected static final String XSRF_TOKEN_IDENTIFIER = "XSRF-TOKEN=";
  
  protected static final String HOST_NAME = "localhost";

  protected String appBasePath;
  protected String appUrl;
  protected TestUtil testUtil;
  protected TestProperties testProperties;

  protected static ChromeDriverService service;

  protected HttpClient client;
  protected String httpPort;
  
  protected String csrfToken;
  protected String sessionId;

  @Before
  public void before() throws Exception {
    testProperties = new TestProperties(48080);
    testUtil = new TestUtil(testProperties);
  }

  public void createClient(String ctxPath) throws Exception {
    testProperties = new TestProperties();

    appBasePath = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application " + appBasePath);

    client = HttpClient.newBuilder().build();
  }

  protected void getTokens() throws IOException, InterruptedException {
    // First request to retrieve initial cookies
    HttpRequest initialRequest = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + TASKLIST_PATH))
      .GET()
      .build();
    HttpResponse<String> initialResponse = client.send(initialRequest, HttpResponse.BodyHandlers.ofString());
    List<String> cookieValues = getCookieHeaders(initialResponse);

    String startCsrfCookie = getCookie(cookieValues, XSRF_TOKEN_IDENTIFIER);
    String startSessionCookie = getCookie(cookieValues, JSESSIONID_IDENTIFIER);

    // Login request
    HttpRequest loginRequest = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + "api/admin/auth/user/default/login/cockpit"))
      .header(COOKIE_HEADER, createCookieHeader(startCsrfCookie, startSessionCookie))
      .header(X_XSRF_TOKEN_HEADER, startCsrfCookie)
      .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
      .POST(HttpRequest.BodyPublishers.ofString("username=demo&password=demo"))
      .build();
    HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
    cookieValues = getCookieHeaders(loginResponse);

    sessionId = getCookie(cookieValues, JSESSIONID_IDENTIFIER);

    // Update CSRF cookie
    HttpRequest csrfRequest = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + "api/engine/engine"))
      .header(COOKIE_HEADER, createCookieHeader(startCsrfCookie, sessionId))
      .header(X_XSRF_TOKEN_HEADER, startCsrfCookie)
      .GET()
      .build();
    HttpResponse<String> csrfResponse = client.send(csrfRequest, HttpResponse.BodyHandlers.ofString());
    cookieValues = getCookieHeaders(csrfResponse);

    csrfToken = getCookie(cookieValues, XSRF_TOKEN_IDENTIFIER);
  }

  protected List<String> getCookieHeaders(HttpResponse<String> response) {
    return response.headers().allValues("Set-Cookie");
  }

  protected String getCookie(List<String> cookieValues, String cookieName) {
    return cookieValues.stream()
      .filter(cookie -> cookie.startsWith(cookieName))
      .findFirst()
      .map(cookie -> cookie.substring(cookieName.length()))
      .orElse("");
  }

  protected String getXsrfTokenHeader(HttpResponse<?> response) {
    return response.headers().firstValue(X_XSRF_TOKEN_HEADER).orElseThrow();
  }

  protected String getXsrfCookieValue(HttpResponse<?> response) {
    return response.headers().firstValue(XSRF_TOKEN_IDENTIFIER).orElseThrow();
  }


  protected String createCookieHeader() {
    return createCookieHeader(csrfToken, sessionId);
  }

  protected String createCookieHeader(String csrf, String session) {
    return XSRF_TOKEN_IDENTIFIER + csrf + "; " + JSESSIONID_IDENTIFIER + session;
  }

  protected void preventRaceConditions() throws InterruptedException {
    // Just wait some seconds before starting because of Wildfly / Cargo race conditions
    Thread.sleep(5 * 1000L);
  }

  protected String getWebappCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.webapp", "operaton/");
  }

  protected String getRestCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.rest", "engine-rest/");
  }

  protected boolean isCookieHeaderValuePresent(String expectedHeaderValue, HttpResponse<?> response) {
    var headers = response.headers();

    List<String> values = headers.allValues("Set-Cookie");
    for (String value : values) {
      if (value.startsWith("JSESSIONID=")) {
        return value.contains(expectedHeaderValue);
      }
    }

    return false;
  }

  protected HttpResponse<String> getAsset(String path) {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + path))
      .GET()
      .build();
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }


  protected HttpResponse<String> getTasklistResponse() {
    return getAsset(TASKLIST_PATH);
  }
}
