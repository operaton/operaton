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

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import org.operaton.bpm.client.exception.ExternalTaskClientException;
import org.operaton.bpm.client.impl.ExternalTaskClientLogger;
import org.operaton.bpm.client.interceptor.ClientRequestContext;
import org.operaton.bpm.client.interceptor.ClientRequestInterceptor;

import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;

/**
 * OAuth2 client credentials request interceptor for the external task client.
 */
public class OAuth2ClientCredentialsProvider implements ClientRequestInterceptor, Closeable {

  protected static final ExternalTaskClientLogger LOG = ExternalTaskClientLogger.CLIENT_LOGGER;

  protected static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
  protected static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

  protected final String tokenUri;
  protected final String clientId;
  protected final String clientSecret;
  protected final ClientAssertionProvider clientAssertionProvider;
  protected final String scope;
  protected final String audience;
  protected final String resource;
  protected final Map<String, String> additionalParameters;
  protected final Duration expiryBuffer;

  protected final CloseableHttpClient httpClient;
  protected final ObjectMapper objectMapper;

  protected volatile String cachedToken;
  protected volatile Instant tokenExpiry = Instant.EPOCH;

  protected OAuth2ClientCredentialsProvider(Builder builder) {
    this.tokenUri = builder.tokenUri;
    this.clientId = builder.clientId;
    this.clientSecret = builder.clientSecret;
    this.clientAssertionProvider = builder.clientAssertionProvider;
    this.scope = builder.scope;
    this.audience = builder.audience;
    this.resource = builder.resource;
    this.additionalParameters = builder.additionalParameters.isEmpty()
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(new LinkedHashMap<>(builder.additionalParameters));
    this.expiryBuffer = builder.expiryBuffer;
    this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClients.createDefault();
    this.objectMapper = builder.objectMapper != null ? builder.objectMapper : new ObjectMapper();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void intercept(ClientRequestContext requestContext) {
    requestContext.addHeader(AUTHORIZATION, "Bearer " + getValidToken());
  }

  @Override
  public void close() throws IOException {
    httpClient.close();
  }

  protected String getValidToken() {
    if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minus(expiryBuffer))) {
      return cachedToken;
    }
    synchronized (this) {
      if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minus(expiryBuffer))) {
        return cachedToken;
      }
      fetchAndCacheToken();
      return cachedToken;
    }
  }

  protected void fetchAndCacheToken() {
    List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS));
    params.add(new BasicNameValuePair("client_id", clientId));

    if (clientSecret != null && !clientSecret.isBlank()) {
      params.add(new BasicNameValuePair("client_secret", clientSecret));
    } else {
      params.add(new BasicNameValuePair("client_assertion_type", ASSERTION_TYPE));
      params.add(new BasicNameValuePair("client_assertion", clientAssertionProvider.getAssertion()));
    }

    if (scope != null && !scope.isBlank()) {
      params.add(new BasicNameValuePair("scope", scope));
    }
    if (audience != null && !audience.isBlank()) {
      params.add(new BasicNameValuePair("audience", audience));
    }
    if (resource != null && !resource.isBlank()) {
      params.add(new BasicNameValuePair("resource", resource));
    }
    additionalParameters.forEach((key, value) -> {
      if (key != null && !key.isBlank() && value != null) {
        params.add(new BasicNameValuePair(key, value));
      }
    });

    HttpPost request = new HttpPost(tokenUri);
    request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

    try {
      String body = httpClient.execute(request, response -> {
        int statusCode = response.getCode();
        String responseBody = response.getEntity() != null
            ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
            : "";

        if (statusCode != 200) {
          JsonNode errorJson = parseJsonSilently(responseBody);
          String error = errorJson != null ? errorJson.path("error").asText("unknown_error") : "unknown_error";
          String description = errorJson != null ? errorJson.path("error_description").asText("") : "";
          throw new ExternalTaskClientException(
              "Token endpoint returned HTTP " + statusCode + ": " + error
                  + (description.isBlank() ? "" : " - " + description));
        }
        return responseBody;
      });

      JsonNode json = objectMapper.readTree(body);
      String accessToken = json.path("access_token").asText();
      if (accessToken == null || accessToken.isBlank()) {
        throw new ExternalTaskClientException(
            "Token endpoint response did not contain a valid access_token");
      }

      cachedToken = accessToken;
      tokenExpiry = Instant.now().plusSeconds(json.path("expires_in").asLong(3600L));
    } catch (ExternalTaskClientException | IOException e) {
      throw LOG.oauth2TokenAcquisitionFailedException(e);
    }
  }

  private JsonNode parseJsonSilently(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (JsonProcessingException | RuntimeException ignored) {
      return null;
    }
  }

  public static class Builder {

    private String tokenUri;
    private String clientId;
    private String clientSecret;
    private ClientAssertionProvider clientAssertionProvider;
    private String scope;
    private String audience;
    private String resource;
    private Map<String, String> additionalParameters = new LinkedHashMap<>();
    private Duration expiryBuffer = Duration.ofSeconds(30);
    private CloseableHttpClient httpClient;
    private ObjectMapper objectMapper;

    public Builder tokenUri(String tokenUri) {
      this.tokenUri = tokenUri;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder clientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    public Builder clientAssertionProvider(ClientAssertionProvider clientAssertionProvider) {
      this.clientAssertionProvider = clientAssertionProvider;
      return this;
    }

    public Builder scope(String scope) {
      this.scope = scope;
      return this;
    }

    public Builder audience(String audience) {
      this.audience = audience;
      return this;
    }

    public Builder resource(String resource) {
      this.resource = resource;
      return this;
    }

    public Builder additionalParameter(String key, String value) {
      this.additionalParameters.put(key, value);
      return this;
    }

    public Builder additionalParameters(Map<String, String> additionalParameters) {
      this.additionalParameters = additionalParameters != null
          ? new LinkedHashMap<>(additionalParameters)
          : new LinkedHashMap<>();
      return this;
    }

    public Builder expiryBuffer(Duration expiryBuffer) {
      this.expiryBuffer = expiryBuffer;
      return this;
    }

    public Builder httpClient(CloseableHttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    public Builder objectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    public OAuth2ClientCredentialsProvider build() {
      if (tokenUri == null || tokenUri.isBlank()) {
        throw LOG.oauth2TokenUriNullException();
      }
      if (clientId == null || clientId.isBlank()) {
        throw LOG.oauth2ClientIdNullException();
      }
      if ((clientSecret == null || clientSecret.isBlank()) && clientAssertionProvider == null) {
        throw new ExternalTaskClientException(
            "Either clientSecret or clientAssertionProvider must be configured on OAuth2ClientCredentialsProvider");
      }
      if (clientSecret != null && clientAssertionProvider != null) {
        throw new ExternalTaskClientException(
            "Only one of clientSecret or clientAssertionProvider may be set on OAuth2ClientCredentialsProvider, not both");
      }
      if (expiryBuffer != null && expiryBuffer.isNegative()) {
        throw new ExternalTaskClientException(
            "expiryBuffer must not be negative on OAuth2ClientCredentialsProvider");
      }
      if (expiryBuffer == null) {
        expiryBuffer = Duration.ZERO;
      }
      return new OAuth2ClientCredentialsProvider(this);
    }
  }

}
