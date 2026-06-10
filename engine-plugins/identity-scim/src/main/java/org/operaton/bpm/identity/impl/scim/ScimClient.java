/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.operaton.bpm.engine.impl.identity.IdentityProviderException;
import org.operaton.bpm.identity.impl.scim.ScimClient.HttpMethod;
import org.operaton.bpm.identity.impl.scim.util.ScimPluginLogger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for SCIM API operations.
 */
public class ScimClient {

  protected final ScimConfiguration configuration;
  protected final ObjectMapper objectMapper;
  protected CloseableHttpClient httpClient;
  protected ScimOAuth2TokenStore oauth2TokenStore;
  protected enum HttpMethod {GET, POST, PUT, PATCH, DEL};
  protected ScimSimpleCache<JsonNode> responseCache;

  public ScimClient(ScimConfiguration configuration) {
    this(configuration, null, null);
  }

  public ScimClient(ScimConfiguration configuration, ScimSimpleCache<JsonNode> responseCache) {
    this(configuration, responseCache, null);
  }

  public ScimClient(ScimConfiguration configuration, ScimSimpleCache<JsonNode> responseCache, ScimOAuth2TokenStore oauth2TokenStore) {
    this.configuration = configuration;
    this.objectMapper = new ObjectMapper();
    this.responseCache = responseCache;
    this.oauth2TokenStore = oauth2TokenStore;
    checkConfiguration();
    initializeHttpClient();
  }

  protected void checkConfiguration() {
    if (configuration == null) {
      throw new IdentityProviderException("Failed to check SCIM configuration: configuration is empty.");
    }   
    if (configuration.getServerUrl() == null || configuration.getServerUrl().isEmpty()) {
      throw new IdentityProviderException("Failed to check SCIM configuration: serverUrl is not set.");
    }
  }

  @SuppressWarnings("deprecation")
  protected void initializeHttpClient() {
    try {
      PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();
      if (configuration.isAcceptUntrustedCertificates()) {
        try {
            SSLContext sslContext = createTrustAllSSLContext();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext, NoopHostnameVerifier.INSTANCE);
            connectionManagerBuilder.setSSLSocketFactory(sslSocketFactory);
        }
        catch (KeyManagementException | NoSuchAlgorithmException e) {
          throw new Exception("Failed to initialize trust-all ssl context", e);
        }
      }
      
      ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(configuration.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .setSocketTimeout(configuration.getSocketTimeout(), TimeUnit.MILLISECONDS)
        .build();
      
      PoolingHttpClientConnectionManager connectionManager = connectionManagerBuilder.build();
      connectionManager.setDefaultConnectionConfig(connectionConfig);
      connectionManager.setMaxTotal(configuration.getMaxConnections());
      connectionManager.setDefaultMaxPerRoute(configuration.getMaxConnections());
      httpClient = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .build();
    } catch (Exception e) {
      throw new IdentityProviderException("Failed to initialize SCIM HTTP client", e);
    }
  }

  protected SSLContext createTrustAllSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {
          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) {
          }
        }
    };

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    return sslContext;
  }

  /**
   * Search for users using SCIM filter.
   */
  public JsonNode searchUsers(String filter, int startIndex, int count, String sorting) {
    StringBuilder url = new StringBuilder(configuration.getServerUrl());
    url.append(configuration.getUsersEndpoint());
    
    boolean hasParams = false;
    if (filter != null && !filter.isEmpty()) {
      url.append("?filter=").append(encodeUrlParameter(filter));
      hasParams = true;
    }
    
    url.append(hasParams ? "&" : "?").append("startIndex=").append(startIndex);
    url.append("&count=").append(count);
    
    // sorting, contains sortby and orderby
    if (sorting != null) {
      url.append("&").append(sorting);
    }

    ScimPluginLogger.INSTANCE.scimFilterQuery(filter);
    return executeGet(url.toString());
  }

  /**
   * Search for groups using SCIM filter.
   */
  public JsonNode searchGroups(String filter, int startIndex, int count, String sorting) {
    StringBuilder url = new StringBuilder(configuration.getServerUrl());
    url.append(configuration.getGroupsEndpoint());
    
    boolean hasParams = false;
    if (filter != null && !filter.isEmpty()) {
      url.append("?filter=").append(encodeUrlParameter(filter));
      hasParams = true;
    }
    
    url.append(hasParams ? "&" : "?").append("startIndex=").append(startIndex);
    url.append("&count=").append(count);
    
   // sorting, contains sortby and orderby
    if (sorting != null) {
      url.append("&").append(sorting);
    }

    ScimPluginLogger.INSTANCE.scimFilterQuery(filter);
    return executeGet(url.toString());
  }

  /**
   * Get a specific user by scim ID.
   */
  public JsonNode getUserByScimId(String scimId) {
    String url = configuration.getServerUrl() + configuration.getUsersEndpoint() + "/" + encodeUrlParameter(scimId);
    return executeGet(url);
  }
  
  /**
   * Patch a specific user by scim ID.
   */
  public JsonNode patchUserByScimId(String scimId, JsonNode patchBody) {
    String url = configuration.getServerUrl() + configuration.getUsersEndpoint() + "/" + encodeUrlParameter(scimId);
    return executePatch(url, patchBody);
  }
   
  /**
   * Delete a specific user by scim ID.
   */
  public JsonNode deleteUserByScimId(String scimId) {
    String url = configuration.getServerUrl() + configuration.getUsersEndpoint() + "/" + encodeUrlParameter(scimId);
    return executeDel(url);
  }

  /**
   * Get a specific group by scim ID.
   */
  public JsonNode getGroupByScimId(String scimId) {
    String url = configuration.getServerUrl() + configuration.getGroupsEndpoint() + "/" + encodeUrlParameter(scimId);
    return executeGet(url);
  }
  
  /**
   * Patch a specific group by scim ID.
   */
  public JsonNode patchGroupByScimId(String scimId, JsonNode patchBody) {
    String url = configuration.getServerUrl() + configuration.getGroupsEndpoint() + "/" + encodeUrlParameter(scimId);
    return executePatch(url, patchBody);
  }
  
  /**
   * Delete a specific group by scim ID.
   */
  public JsonNode deleteGroupByScimId(String scimId) {
    String url = configuration.getServerUrl() + configuration.getGroupsEndpoint() + "/" + encodeUrlParameter(scimId);
    return executeDel(url);
  }

  protected JsonNode executeGet(String url) {
    if (responseCache != null) {
      JsonNode cached = responseCache.get(url);
      if (cached != null) {
        return cached;
      }
    }
    JsonNode result = executeHttpRequest(HttpMethod.GET, url, null, false);
    if (responseCache != null && result != null) {
      responseCache.put(url, result);
    }
    return result;
  }
  
  protected JsonNode executePost(String url, JsonNode postBody) {
    JsonNode result = executeHttpRequest(HttpMethod.POST, url, postBody, false);
    if (responseCache != null) {
      responseCache.invalidateAll();
    }
    return result;
  }
  
  protected JsonNode executeDel(String url) {
    JsonNode result = executeHttpRequest(HttpMethod.DEL, url, null, false);
    if (responseCache != null) {
      responseCache.invalidateAll();
    }
    return result;
  }
  
  protected JsonNode executePatch(String url, JsonNode patchBody) {
    JsonNode result = executeHttpRequest(HttpMethod.PATCH, url, patchBody, false);
    if (responseCache != null) {
      responseCache.invalidateAll();
    }
    return result;
  }
 
  @SuppressWarnings("deprecation")
  protected JsonNode executeHttpRequest(HttpMethod method, String url, JsonNode body, boolean isRetry) {
    HttpUriRequestBase request = 
        method == HttpMethod.GET ? new HttpGet(url) :
        method == HttpMethod.POST ? new HttpPost(url) : 
        method == HttpMethod.PUT ? new HttpPut(url) :
        method == HttpMethod.PATCH ? new HttpPatch(url) :
        method == HttpMethod.DEL ? new HttpDelete(url) : null;
       
    addAuthenticationHeader(request);
    addCustomHeaders(request);

    if (body != null) {
      request.setEntity(new StringEntity(
          body.toString(),
          ContentType.create("application/scim+json", StandardCharsets.UTF_8)
      ));
    }
    
    boolean verbose = configuration.isVerbose();
    boolean hideBody = (method == HttpMethod.POST && url.endsWith(".search"));
    ScimPluginLogger.INSTANCE.httpClientRequest(verbose, method.toString(), url, body != null ? body.toString() : "", hideBody);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      int statusCode = response.getCode();
      String responseBody = response.getEntity() != null ? 
          EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "{}";

      ScimPluginLogger.INSTANCE.httpClientResponse(verbose, method.toString(), statusCode);

      if (statusCode == 200 || statusCode == 201 || statusCode == 204) {
        return objectMapper.readTree(responseBody);
      } else if (statusCode == 401 && isOAuth2Authentication() && !isRetry) {
        // Try to refresh OAuth2 token and retry once
        refreshOAuth2Token();
        return executeHttpRequest(method, url, body, true);
      } else {
        ScimPluginLogger.INSTANCE.scimRequestError(statusCode, responseBody);
        throw new IdentityProviderException("SCIM request failed with status: " + statusCode);
      }
    } catch (IOException | ParseException e) {
      ScimPluginLogger.INSTANCE.httpClientException(method.toString() + " " + url, e);
      throw new IdentityProviderException("SCIM HTTP request failed", e);
    }
  }

  protected void addAuthenticationHeader(HttpUriRequestBase request) {
    String authType = configuration.getAuthenticationType().toLowerCase();
    
    switch (authType) {
      case "bearer":
        if (configuration.getBearerToken() != null) {
          request.setHeader("Authorization", "Bearer " + configuration.getBearerToken());
        }
        break;
      case "basic":
        if (configuration.getUsername() != null && configuration.getPassword() != null) {
          String auth = configuration.getUsername() + ":" + configuration.getPassword();
          String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
          request.setHeader("Authorization", "Basic " + encodedAuth);
        }
        break;
      case "oauth2":
        ensureOAuth2Token();
        if (oauth2TokenStore != null && oauth2TokenStore.getToken() != null) {
          request.setHeader("Authorization", "Bearer " + oauth2TokenStore.getToken());
        }
        break;
    }
  }

  protected void addCustomHeaders(HttpUriRequestBase request) {
    request.setHeader("Accept", "application/scim+json");
    request.setHeader("Content-Type", "application/scim+json");
    
    for (Map.Entry<String, String> header : configuration.getCustomHeaders().entrySet()) {
      request.setHeader(header.getKey(), header.getValue());
    }
  }

  protected boolean isOAuth2Authentication() {
    return "oauth2".equalsIgnoreCase(configuration.getAuthenticationType());
  }

  protected void ensureOAuth2Token() {
    if (oauth2TokenStore == null || !oauth2TokenStore.isTokenValid()) {
      refreshOAuth2Token();
    }
  }

  @SuppressWarnings("deprecation")
  protected void refreshOAuth2Token() {
    if (oauth2TokenStore == null) {
      throw new IdentityProviderException("OAuth2 token store not initialized");
    }
    if (configuration.getOauth2TokenUrl() == null) {
      throw new IdentityProviderException("OAuth2 token URL not configured");
    }

    boolean verbose = configuration.isVerbose();
    ScimPluginLogger.INSTANCE.oauth2TokenRefresh(verbose);

    // Actually not a refresh process, but a request for a new access token (server-to-server auth)
    HttpPost request = new HttpPost(configuration.getOauth2TokenUrl());
    request.setHeader("Content-Type", "application/x-www-form-urlencoded");

    StringBuilder body = new StringBuilder();
    body.append("grant_type=client_credentials");
    body.append("&client_id=").append(encodeUrlParameter(configuration.getOauth2ClientId()));
    body.append("&client_secret=").append(encodeUrlParameter(configuration.getOauth2ClientSecret()));
    if (configuration.getOauth2Scope() != null) {
      body.append("&scope=").append(encodeUrlParameter(configuration.getOauth2Scope()));
    }

    request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      int statusCode = response.getCode();
      String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

      if (statusCode == 200) {
        JsonNode tokenResponse = objectMapper.readTree(responseBody);
        String accessToken = tokenResponse.get("access_token").asText();
        int expiresIn = tokenResponse.has("expires_in") ? tokenResponse.get("expires_in").asInt() : 3600;
        expiresIn = expiresIn > 120 ? expiresIn - 60 : expiresIn;  // Refresh 1 minute early if there is enough time
        long expiryTime = System.currentTimeMillis() + expiresIn * 1000L; 
        oauth2TokenStore.setToken(accessToken);
        oauth2TokenStore.setExpiryTime(expiryTime);
      } else {
        ScimPluginLogger.INSTANCE.authenticationFailure("OAuth2 token request failed with status: " + statusCode + ", response: " + responseBody);
        throw new IdentityProviderException("OAuth2 token request failed");
      }
    } catch (IOException | ParseException e) {
      ScimPluginLogger.INSTANCE.httpClientException("OAuth2 token refresh", e);
      throw new IdentityProviderException("OAuth2 token refresh failed", e);
    }
  }
  
  @SuppressWarnings("deprecation")
  protected boolean checkUserPasswordWithOidc(String userName, String password) {
    String url = configuration.getUserAuthenticationUrl();
    if (url == null || url.isEmpty()) {
      throw new IdentityProviderException("User authentication URL not configured");
    }

    boolean verbose = configuration.isVerbose();
    ScimPluginLogger.INSTANCE.userAuthenticationRequest(verbose, "OIDC", url, userName);

    // Actually not a refresh process, but a request for a new access token (server-to-server auth)
    HttpPost request = new HttpPost(url);
    request.setHeader("Content-Type", "application/x-www-form-urlencoded");

    StringBuilder body = new StringBuilder();
    body.append("grant_type=password");
    body.append("&client_id=").append(encodeUrlParameter(configuration.getOauth2ClientId()));
    body.append("&client_secret=").append(encodeUrlParameter(configuration.getOauth2ClientSecret()));
    body.append("&username=").append(userName);
    body.append("&password=").append(encodeUrlParameter(password));

    request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      int statusCode = response.getCode();
      String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      ScimPluginLogger.INSTANCE.userAuthenticationResponse(verbose, "OIDC", userName, statusCode);

      if (statusCode == 200) {
        return true;
      } else {
        ScimPluginLogger.INSTANCE.authenticationFailure("User authentication failed with status: " + statusCode + ", response: " + responseBody );
        return false;
      }
    } catch (IOException | ParseException e) {
      ScimPluginLogger.INSTANCE.httpClientException("User authentication with OIDC failed", e);
      throw new IdentityProviderException("User authentication failed", e);
    }
  }

  protected boolean checkUserWithSearchFilter(String userId, String filter) {
    String url = configuration.getServerUrl() + configuration.getUsersEndpoint() + "/.search";
 
    boolean verbose = configuration.isVerbose();
    ScimPluginLogger.INSTANCE.userAuthenticationRequest(verbose, "SCIM", url, userId);

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode schemas = mapper.createArrayNode().add("urn:ietf:params:scim:api:messages:2.0:SearchRequest");
    root.set("schemas", schemas);
    root.put("filter", filter);

    JsonNode response = executeHttpRequest(HttpMethod.POST, url, root, false);
    boolean result = (response != null && response.has("Resources") && response.get("Resources").size() == 1);
    ScimPluginLogger.INSTANCE.userAuthenticationResponse(verbose, "SCIM", userId, result ? 200 : 401);
    
    return result;
  }
  
  protected String encodeUrlParameter(String param) {
    if (param == null) {
      return "";
    }
    return URLEncoder.encode(param, StandardCharsets.UTF_8);
  }

  public void close() {
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (IOException e) {
        ScimPluginLogger.INSTANCE.httpClientException("close", e);
      }
    }
  }
}
