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
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import kong.unirest.HttpResponse;
import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jakarta.ws.rs.core.HttpHeaders.SET_COOKIE;

/**
 * @author Daniel Meyer
 * @author Roman Smirnov
 */
public abstract class AbstractWebIntegrationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebIntegrationTest.class);

  protected static final String TASKLIST_PATH = "app/tasklist/default/";

  protected static final String COOKIE_HEADER = "Cookie";
  protected static final String X_XSRF_TOKEN_HEADER = "X-XSRF-TOKEN";

  protected static final String JSESSIONID_IDENTIFIER = "JSESSIONID=";
  protected static final String XSRF_TOKEN_IDENTIFIER = "XSRF-TOKEN=";

  protected String appBasePath;
  protected String appUrl;
  protected TestProperties testProperties;

  protected static ChromeDriverService service;

  protected String csrfToken;
  protected String sessionId;

  @BeforeAll
  public static void setUpClass() {
    Unirest.config().reset().enableCookieManagement(false).setObjectMapper(new ObjectMapper() {
      final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

      @Override
      public String writeValue(Object value) {
        try {
          return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public <T> T readValue(String value, Class<T> valueType) {
        try {
          return mapper.readValue(value, valueType);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @BeforeEach
  public void before() {
    testProperties = new TestProperties(48080);
  }

  public void createClient(String ctxPath) {
    testProperties = new TestProperties();

    appBasePath = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application {}", appBasePath);
  }

  protected void getTokens() {
    // First request, first set of cookies
    HttpResponse<String> response = Unirest.get(appBasePath + TASKLIST_PATH).asString();
    List<String> cookieValues = response.getHeaders().get(SET_COOKIE);

    String startCsrfCookie = getCookie(cookieValues, XSRF_TOKEN_IDENTIFIER);
    String startSessionCookie = getCookie(cookieValues, JSESSIONID_IDENTIFIER);

    // login with user, update session cookie
    response = Unirest.post(appBasePath + "api/admin/auth/user/default/login/cockpit")
            .body("username=demo&password=demo")
            .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
            .header(COOKIE_HEADER, createCookieHeader(startCsrfCookie, startSessionCookie))
            .header(X_XSRF_TOKEN_HEADER, startCsrfCookie)
            .header("Accept", MediaType.APPLICATION_JSON)
            .asString();
    cookieValues = response.getHeaders().get(SET_COOKIE);

    sessionId = getCookie(cookieValues, JSESSIONID_IDENTIFIER);

    // update CSRF cookie
    response = Unirest.get(appBasePath + "api/engine/engine")
            .header(COOKIE_HEADER, createCookieHeader(startCsrfCookie, sessionId))
            .header(X_XSRF_TOKEN_HEADER, startCsrfCookie)
            .asString();

    cookieValues = response.getHeaders().get(SET_COOKIE);

    csrfToken = getCookie(cookieValues, XSRF_TOKEN_IDENTIFIER);
  }

  protected List<String> getCookieHeaders(HttpResponse<?> response) {
    return response.getHeaders().get(SET_COOKIE);
  }

  protected String getCookie(List<String> cookieValues, String cookieName) {
    String cookieValue = getCookieValue(cookieValues, cookieName);
    if (cookieValue == null || cookieValue.isEmpty() || cookieValue.length() <= cookieName.length()) {
      return "";
    }
    int valueEnd = cookieValue.contains(";") ? cookieValue.indexOf(';') : cookieValue.length();
    return cookieValue.substring(cookieName.length(), valueEnd);
  }

  protected String createCookieHeader() {
    return createCookieHeader(csrfToken, sessionId);
  }

  protected String createCookieHeader(String csrf, String session) {
    return XSRF_TOKEN_IDENTIFIER + csrf + "; " + JSESSIONID_IDENTIFIER + session;
  }

  protected String getXsrfTokenHeader(HttpResponse<?> response) {
    return response.getHeaders().getFirst(X_XSRF_TOKEN_HEADER);
  }

  protected String getXsrfCookieValue(HttpResponse<?> response) {
    return getCookieValue(response, XSRF_TOKEN_IDENTIFIER);
  }

  protected String getCookieValue(HttpResponse<?> response, String cookieName) {
    return getCookieValue(getCookieHeaders(response), cookieName);
  }

  protected String getCookieValue(List<String> cookieValues, String cookieName) {
    if (cookieValues != null) {
      for (String cookieValue : cookieValues) {
        if (cookieValue != null && cookieValue.contains(cookieName)) {
          return cookieValue;
        }
      }
    }

    return "";
  }

  // Helper methods for common test operations
  protected String getWebappCtxPath() {
    return testProperties.getWebappCtxPath();
  }

  protected String getRestCtxPath() {
    return testProperties.getRestCtxPath();
  }

  protected void preventRaceConditions() {
    try {
      Thread.sleep(500); // Simple delay to prevent race conditions
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
