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
package org.operaton.bpm;

import java.util.List;
import java.util.logging.Logger;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriverService;

import org.operaton.bpm.util.TestUtil;

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

  protected Client client;
  protected String httpPort;

  protected String csrfToken;
  protected String sessionId;

  // current target under test
  protected WebTarget target;
  // current response under test
  protected Response response;

  @BeforeEach
  public void before() throws Exception {
    testProperties = new TestProperties(48080);
    testUtil = new TestUtil(testProperties);
  }

  @AfterEach
  public void destroyClient() {
    client.close();
    if (response != null) {
      response.close();
    }
  }

  public void createClient(String ctxPath) throws Exception {
    testProperties = new TestProperties();

    appBasePath = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application " + appBasePath);

    var clientConfig = new ClientConfig();
    clientConfig.register(JacksonJaxbJsonProvider.class);  // Register Jackson for POJO mapping

    client = ClientBuilder.newClient(clientConfig);
  }

  protected void getTokens() {
    // First request, first set of cookies
    target = client.target(appBasePath + "/tasklist"); // replace TASKLIST_PATH
    Response clientResponse = target.request().get();
    List<Object> cookieValues = getCookieHeaders(clientResponse);
    clientResponse.close();

    String startCsrfCookie = getCookie(cookieValues, XSRF_TOKEN_IDENTIFIER);
    String startSessionCookie = getCookie(cookieValues, JSESSIONID_IDENTIFIER);

    // Login with user, update session cookie
    target = client.target(appBasePath + "api/admin/auth/user/default/login/cockpit");
    clientResponse = target
            .request()
            .header(COOKIE_HEADER, createCookieHeader(startCsrfCookie, startSessionCookie))
            .header(X_XSRF_TOKEN_HEADER, startCsrfCookie)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity("username=demo&password=demo", MediaType.APPLICATION_FORM_URLENCODED));

    cookieValues = clientResponse.getHeaders().get("Set-Cookie");
    clientResponse.close();

    sessionId = getCookie(cookieValues, JSESSIONID_IDENTIFIER);

    // Update CSRF cookie
    clientResponse = client.target(appBasePath + "api/engine/engine")
            .request()
            .header(COOKIE_HEADER, createCookieHeader(startCsrfCookie, sessionId))
            .header(X_XSRF_TOKEN_HEADER, startCsrfCookie)
            .get();

    cookieValues = getCookieHeaders(clientResponse);
    clientResponse.close();

    csrfToken = getCookie(cookieValues, XSRF_TOKEN_IDENTIFIER);
  }

  protected List<Object> getCookieHeaders(Response response) {
    return response.getHeaders().get("Set-Cookie");
  }

  protected String getCookie(List<Object> cookieValues, String cookieName) {
    String cookieValue = getCookieValue(cookieValues, cookieName);
    int valueEnd = cookieValue.contains(";") ? cookieValue.indexOf(';') : cookieValue.length() - 1;
    return cookieValue.substring(cookieName.length(), valueEnd);
  }

  protected String createCookieHeader() {
    return createCookieHeader(csrfToken, sessionId);
  }

  protected String createCookieHeader(String csrf, String session) {
    return XSRF_TOKEN_IDENTIFIER + csrf + "; " + JSESSIONID_IDENTIFIER + session;
  }

  protected String getXsrfTokenHeader(Response response) {
    return response.getHeaders().getFirst(X_XSRF_TOKEN_HEADER).toString();
  }

  protected String getXsrfCookieValue(Response response) {
    return getCookieValue(response, XSRF_TOKEN_IDENTIFIER);
  }

  protected String getCookieValue(Response response, String cookieName) {
    return getCookieValue(getCookieHeaders(response), cookieName);
  }

  protected String getCookieValue(List<Object> cookies, String cookieName) {
    for (Object cookie : cookies) {
      if (cookie.toString().startsWith(cookieName)) {
        return cookie.toString();
      }
    }

    return "";
  }

  protected void preventRaceConditions() throws InterruptedException {
    // just wait some seconds before starting because of Wildfly / Cargo race conditions
    Thread.sleep(5 * 1000L);
  }

  protected String getWebappCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.webapp", null);
  }

  protected String getRestCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.rest", null);
  }
}
