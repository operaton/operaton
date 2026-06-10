/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0; you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.client.interceptor.auth;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.client.exception.ExternalTaskClientException;
import org.operaton.bpm.client.interceptor.ClientRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2ClientCredentialsProviderTest {

  private HttpServer tokenServer;
  private final AtomicInteger requestCount = new AtomicInteger();
  private final AtomicReference<String> requestBody = new AtomicReference<>();

  @AfterEach
  void tearDown() {
    if (tokenServer != null) {
      tokenServer.stop(0);
    }
  }

  @Test
  void shouldFetchClientCredentialsTokenAndReuseCachedToken() throws Exception {
    String tokenUri = startTokenServer(200, "{\"access_token\":\"access-token\",\"expires_in\":3600}");
    OAuth2ClientCredentialsProvider provider = OAuth2ClientCredentialsProvider.builder()
        .tokenUri(tokenUri)
        .clientId("external-task-client")
        .clientSecret("secret")
        .scope("engine-rest/.default")
        .audience("engine-rest")
        .resource("api://engine-rest")
        .additionalParameter("tenant", "demo")
        .build();

    RecordingClientRequestContext firstRequest = new RecordingClientRequestContext();
    RecordingClientRequestContext secondRequest = new RecordingClientRequestContext();
    provider.intercept(firstRequest);
    provider.intercept(secondRequest);

    assertThat(firstRequest.headers).containsEntry("Authorization", "Bearer access-token");
    assertThat(secondRequest.headers).containsEntry("Authorization", "Bearer access-token");
    assertThat(requestCount).hasValue(1);
    assertThat(parseForm(requestBody.get()))
        .containsEntry("grant_type", "client_credentials")
        .containsEntry("client_id", "external-task-client")
        .containsEntry("client_secret", "secret")
        .containsEntry("scope", "engine-rest/.default")
        .containsEntry("audience", "engine-rest")
        .containsEntry("resource", "api://engine-rest")
        .containsEntry("tenant", "demo");
  }

  @Test
  void shouldSendClientAssertionWhenNoClientSecretIsConfigured() throws Exception {
    String tokenUri = startTokenServer(200, "{\"access_token\":\"assertion-token\",\"expires_in\":3600}");
    OAuth2ClientCredentialsProvider provider = OAuth2ClientCredentialsProvider.builder()
        .tokenUri(tokenUri)
        .clientId("external-task-client")
        .clientAssertionProvider(() -> "signed-client-assertion")
        .build();

    provider.intercept(new RecordingClientRequestContext());

    assertThat(parseForm(requestBody.get()))
        .containsEntry("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
        .containsEntry("client_assertion", "signed-client-assertion");
  }

  @Test
  void shouldRejectInvalidOAuth2Configuration() {
    assertThatThrownBy(() -> OAuth2ClientCredentialsProvider.builder()
        .clientId("client")
        .clientSecret("secret")
        .build())
        .isInstanceOf(ExternalTaskClientException.class)
        .hasMessageContaining("OAuth2 token URI");

    assertThatThrownBy(() -> OAuth2ClientCredentialsProvider.builder()
        .tokenUri("http://localhost/token")
        .clientSecret("secret")
        .build())
        .isInstanceOf(ExternalTaskClientException.class)
        .hasMessageContaining("OAuth2 client ID");

    assertThatThrownBy(() -> OAuth2ClientCredentialsProvider.builder()
        .tokenUri("http://localhost/token")
        .clientId("client")
        .build())
        .isInstanceOf(ExternalTaskClientException.class)
        .hasMessageContaining("Either clientSecret or clientAssertionProvider");
  }

  @Test
  void shouldWrapTokenEndpointErrors() throws Exception {
    String tokenUri = startTokenServer(401, "{\"error\":\"invalid_client\",\"error_description\":\"bad credentials\"}");
    OAuth2ClientCredentialsProvider provider = OAuth2ClientCredentialsProvider.builder()
        .tokenUri(tokenUri)
        .clientId("external-task-client")
        .clientSecret("secret")
        .build();

    assertThatThrownBy(() -> provider.intercept(new RecordingClientRequestContext()))
        .isInstanceOf(ExternalTaskClientException.class)
        .hasMessageContaining("Failed to acquire OAuth2 access token")
        .hasRootCauseMessage("Token endpoint returned HTTP 401: invalid_client - bad credentials");
  }

  private String startTokenServer(int statusCode, String responseBody) throws IOException {
    tokenServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    tokenServer.createContext("/token", exchange -> {
      requestCount.incrementAndGet();
      requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(statusCode, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    tokenServer.start();
    return "http://127.0.0.1:" + tokenServer.getAddress().getPort() + "/token";
  }

  private static Map<String, String> parseForm(String body) {
    Map<String, String> params = new HashMap<>();
    for (String pair : body.split("&")) {
      String[] keyValue = pair.split("=", 2);
      params.put(
          URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8),
          URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
    }
    return params;
  }

  private static class RecordingClientRequestContext implements ClientRequestContext {

    private final Map<String, String> headers = new HashMap<>();

    @Override
    public void addHeader(String name, String value) {
      headers.put(name, value);
    }
  }
}
