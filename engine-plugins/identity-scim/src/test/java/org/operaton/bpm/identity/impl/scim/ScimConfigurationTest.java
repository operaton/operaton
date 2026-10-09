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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for SCIM Configuration.
 */
public class ScimConfigurationTest {

  @Test
  public void testDefaultConfiguration() {
    ScimConfiguration config = new ScimConfiguration();

    assertThat(config.getScimVersion()).isEqualTo("2.0");
    assertThat(config.getAuthenticationType()).isEqualTo("bearer");
    assertThat(config.getUsersEndpoint()).isEqualTo("/Users");
    assertThat(config.getGroupsEndpoint()).isEqualTo("/Groups");
    assertThat(config.getUserScimIdAttribute()).isEqualTo("id");
    assertThat(config.getUserIdAttribute()).isEqualTo("userName");
    assertThat(config.getUserFirstnameAttribute()).isEqualTo("name.givenName");
    assertThat(config.getUserLastnameAttribute()).isEqualTo("name.familyName");
    assertThat(config.getPageSize()).isEqualTo(100);
    assertThat(config.isAuthorizationCheckEnabled()).isTrue();
  }

  @Test
  public void testSetServerUrl() {
    ScimConfiguration config = new ScimConfiguration();
    config.setServerUrl("https://scim.example.com");

    assertThat(config.getServerUrl()).isEqualTo("https://scim.example.com");
  }

  @Test
  public void testSetBearerToken() {
    ScimConfiguration config = new ScimConfiguration();
    config.setBearerToken("test-token-123");

    assertThat(config.getBearerToken()).isEqualTo("test-token-123");
  }

  @Test
  public void testSetBasicAuth() {
    ScimConfiguration config = new ScimConfiguration();
    config.setAuthenticationType("basic");
    config.setUsername("admin");
    config.setPassword("password");

    assertThat(config.getAuthenticationType()).isEqualTo("basic");
    assertThat(config.getUsername()).isEqualTo("admin");
    assertThat(config.getPassword()).isEqualTo("password");
  }

  @Test
  public void testSetOAuth2Config() {
    ScimConfiguration config = new ScimConfiguration();
    config.setAuthenticationType("oauth2");
    config.setOauth2TokenUrl("https://auth.example.com/token");
    config.setOauth2ClientId("client-id");
    config.setOauth2ClientSecret("client-secret");
    config.setOauth2Scope("scim.read");

    assertThat(config.getAuthenticationType()).isEqualTo("oauth2");
    assertThat(config.getOauth2TokenUrl()).isEqualTo("https://auth.example.com/token");
    assertThat(config.getOauth2ClientId()).isEqualTo("client-id");
    assertThat(config.getOauth2ClientSecret()).isEqualTo("client-secret");
    assertThat(config.getOauth2Scope()).isEqualTo("scim.read");
  }

  @Test
  public void testSetUserAttributeMapping() {
    ScimConfiguration config = new ScimConfiguration();
    config.setUserIdAttribute("id");
    config.setUserFirstnameAttribute("name.givenName");
    config.setUserLastnameAttribute("name.familyName");

    assertThat(config.getUserIdAttribute()).isEqualTo("id");
    assertThat(config.getUserFirstnameAttribute()).isEqualTo("name.givenName");
    assertThat(config.getUserLastnameAttribute()).isEqualTo("name.familyName");
  }

  @Test
  public void testSetConnectionSettings() {
    ScimConfiguration config = new ScimConfiguration();
    config.setConnectionTimeout(60000);
    config.setSocketTimeout(60000);
    config.setMaxConnections(200);

    assertThat(config.getConnectionTimeout()).isEqualTo(60000);
    assertThat(config.getSocketTimeout()).isEqualTo(60000);
    assertThat(config.getMaxConnections()).isEqualTo(200);
  }

  @Test
  public void testSetPagination() {
    ScimConfiguration config = new ScimConfiguration();
    config.setPageSize(50);

    assertThat(config.getPageSize()).isEqualTo(50);
  }

  @Test
  public void testDisableAuthorizationCheck() {
    ScimConfiguration config = new ScimConfiguration();
    config.setAuthorizationCheckEnabled(false);

    assertThat(config.isAuthorizationCheckEnabled()).isFalse();
  }

  @Test
  public void testAcceptUntrustedCertificates() {
    ScimConfiguration config = new ScimConfiguration();
    config.setAcceptUntrustedCertificates(true);

    assertThat(config.isAcceptUntrustedCertificates()).isTrue();
  }

  @Test
  public void testDefaultCacheConfiguration() {
    ScimConfiguration config = new ScimConfiguration();

    assertThat(config.isCacheEnabled()).isFalse();
    assertThat(config.getMaxCacheSize()).isEqualTo(250);
    assertThat(config.getCacheExpirationTimeoutMin()).isEqualTo(5);
  }

  @Test
  public void testSetCacheConfiguration() {
    ScimConfiguration config = new ScimConfiguration();
    config.setCacheEnabled(true);
    config.setMaxCacheSize(200);
    config.setCacheExpirationTimeoutMin(10);

    assertThat(config.isCacheEnabled()).isTrue();
    assertThat(config.getMaxCacheSize()).isEqualTo(200);
    assertThat(config.getCacheExpirationTimeoutMin()).isEqualTo(10);
  }
}
