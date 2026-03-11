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
package org.operaton.bpm.spring.boot.starter.webapp.filter.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpClientExtension implements AfterEachCallback {
  private static final Logger LOG = LoggerFactory.getLogger(HttpClientExtension.class);
  public static final String PORT_PLACEHOLDER_WEBAPP_URL = "{PORT}";
  public static final String WEBAPP_URL = "http://localhost:" + PORT_PLACEHOLDER_WEBAPP_URL +
      "/operaton/app/tasklist/default";

  protected Integer port;
  protected HttpURLConnection connection;
  protected boolean followRedirects;

  public HttpClientExtension() {
  }

  public HttpClientExtension(int port) {
    this.port = port;
  }

  @Override
  public void afterEach(ExtensionContext context) {
    port = null;
    connection = null;
  }
  public HttpURLConnection performRequest() {
    return performRequest(WEBAPP_URL.replace(PORT_PLACEHOLDER_WEBAPP_URL, String.valueOf(port)), null, null, null);
  }

  public HttpURLConnection performRequest(String url) {
    return performRequest(url, null, null, null);
  }

  public HttpURLConnection performRequest(String url, Map<String, String> headers) {
    return performRequest(url, null, headers, null);
  }

  public HttpURLConnection performRequest(String url, String headerName, String headerValue) {
    return performRequest(url, null, Collections.singletonMap(headerName, headerValue), null);
  }

  public HttpURLConnection performPostRequest(String url, String headerName, String headerValue) {
    return performPostRequest(url, headerName, headerValue, null);
  }

  public HttpURLConnection performPostRequest(String url, String headerName, String headerValue, String payload) {
    return performRequest(url, "POST", Collections.singletonMap(headerName, headerValue), payload);
  }

  public HttpURLConnection performPostRequest(String url, Map<String, String> headers, String payload) {
    return performRequest(url, "POST", headers, payload);
  }

  public HttpURLConnection performRequest(String url, String method, Map<String, String> headers, String payload) {
    try {
      connection =
        (HttpURLConnection) new URL(url)
          .openConnection();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    connection.setInstanceFollowRedirects(followRedirects);

    if ("POST".equals(method)) {
      try {
        connection.setRequestMethod("POST");
      } catch (ProtocolException e) {
        throw new RuntimeException(e);
      }
    }

    if (headers != null) {
      headers.forEach((name, value) -> connection.setRequestProperty(name, value));
    }

    if (payload != null) {
      connection.setDoOutput(true);
      connection.setDoInput(true);
      try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
        wr.write(payload.getBytes( StandardCharsets.UTF_8 ));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    try {
      connection.connect();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (payload != null) {
      try {
        connection.getContent();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if(followRedirects) {
      // trigger resolving the redirects
      connection.getHeaderField("Location");
    }
    return connection;
  }

  public List<String> getCookieHeaders() {
    return getHeaders("Set-Cookie");
  }

  public String getHeaderXsrfToken() {
    return connection.getHeaderField("X-XSRF-TOKEN");
  }

  public String getXsrfTokenHeader() {
    return getHeaderXsrfToken();
  }

  public String getCookieValue(String cookieName) {
    return getCookie(cookieName).split(";")[0];
  }

  public String getSessionCookieValue() {
    return getCookieValue("JSESSIONID");
  }

  public String getCookie(String cookieName) {
    List<String> cookies = getCookieHeaders();

    for (String cookie : cookies) {
      if (cookie.startsWith(cookieName + "=")) {
        return cookie;
      }
    }

    return "";
  }

  public String getXsrfCookie() {
    return getCookie("XSRF-TOKEN");
  }

  public String getSessionCookie() {
    return getCookie("JSESSIONID");
  }

  public String getContent() {
    try {
      return StreamUtils.copyToString(connection.getInputStream(), UTF_8);
    } catch (IOException e) {
      LOG.warn("Error reading content: {}: {}", e.getClass(), e.getMessage());
      return null;
    }
  }

  public String getErrorResponseContent() {
    // ensure input stream consumed like before
    getContent();

    InputStream errorStream = connection.getErrorStream();
    if (errorStream == null) {
      return null;
    }

    try (InputStream is = errorStream) {
      return StreamUtils.copyToString(is, UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<String> getHeaders(String name) {
    Map<String, List<String>> headerFields = connection.getHeaderFields();
    return headerFields.get(name);
  }

  public String getHeader(String name) {
    return getHeaders(name) != null ? getHeaders(name).get(0) : null;
  }

  public boolean headerExists(String name) {
    return getHeaders(name) != null;
  }

  public String getSessionCookieRegex(String sameSite) {
    return getSessionCookieRegex(null, null, sameSite, false);
  }

  public String getSessionCookieRegex(String cookieName, String sameSite) {
    return getSessionCookieRegex(null, cookieName, sameSite, false);
  }

  public String getSessionCookieRegex(String sameSite, boolean secure) {
    return getSessionCookieRegex(null, null, sameSite, secure);
  }

  public String getSessionCookieRegex(String path, String cookieNameInput, String sameSite, boolean secure) {
    String cookieName = StringUtils.isBlank(cookieNameInput) ? "JSESSIONID" : cookieNameInput;
    StringBuilder regex = new StringBuilder(cookieName + "=.*;\\W*Path=/");
    if (path != null) {
      regex.append(path);
    }
    regex.append(";\\W*HttpOnly");
    if (sameSite != null) {
      regex.append(";\\W*SameSite=").append(sameSite);
    }
    if (secure) {
      regex.append(";\\W*Secure");
    }
    return regex.toString();
  }

  public HttpClientExtension followRedirects(boolean followRedirects) {
    this.followRedirects = followRedirects;
    return this;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
