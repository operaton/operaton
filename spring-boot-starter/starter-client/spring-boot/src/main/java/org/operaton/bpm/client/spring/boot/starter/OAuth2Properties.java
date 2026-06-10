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
package org.operaton.bpm.client.spring.boot.starter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * OAuth2 client credentials flow settings under {@code operaton.bpm.client.oauth2}.
 */
public class OAuth2Properties {

  private String tokenUri;
  private String clientId;
  private String clientSecret;
  private String scope;
  private String audience;
  private String resource;
  private Map<String, String> additionalParameters = new LinkedHashMap<>();
  private long expiryBufferSeconds = 30L;

  @NestedConfigurationProperty
  private AssertionProperties assertion;

  public String getTokenUri() {
    return tokenUri;
  }

  public void setTokenUri(String tokenUri) {
    this.tokenUri = tokenUri;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getAudience() {
    return audience;
  }

  public void setAudience(String audience) {
    this.audience = audience;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public Map<String, String> getAdditionalParameters() {
    return additionalParameters;
  }

  public void setAdditionalParameters(Map<String, String> additionalParameters) {
    this.additionalParameters = additionalParameters != null ? additionalParameters : new LinkedHashMap<>();
  }

  public long getExpiryBufferSeconds() {
    return expiryBufferSeconds;
  }

  public void setExpiryBufferSeconds(long expiryBufferSeconds) {
    this.expiryBufferSeconds = expiryBufferSeconds;
  }

  public AssertionProperties getAssertion() {
    return assertion;
  }

  public void setAssertion(AssertionProperties assertion) {
    this.assertion = assertion;
  }

  public static class AssertionProperties {

    private AssertionType type;
    private String keyLocation;

    public AssertionType getType() {
      return type;
    }

    public void setType(AssertionType type) {
      this.type = type;
    }

    public String getKeyLocation() {
      return keyLocation;
    }

    public void setKeyLocation(String keyLocation) {
      this.keyLocation = keyLocation;
    }

    public enum AssertionType {
      JJWT,
      AZURE_WORKLOAD_IDENTITY
    }
  }

}
