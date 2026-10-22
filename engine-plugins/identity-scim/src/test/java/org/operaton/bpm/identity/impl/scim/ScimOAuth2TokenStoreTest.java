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
 * Test for ScimOAuth2TokenStore.
 */
public class ScimOAuth2TokenStoreTest {

  @Test
  public void testInitialState() {
    ScimOAuth2TokenStore store = new ScimOAuth2TokenStore();
    assertThat(store.getToken()).isNull();
    assertThat(store.getExpiryTime()).isEqualTo(0);
    assertThat(store.isTokenValid()).isFalse();
  }

  @Test
  public void testValidToken() {
    ScimOAuth2TokenStore store = new ScimOAuth2TokenStore();
    store.setToken("test-token");
    store.setExpiryTime(System.currentTimeMillis() + 60000); // expires in 1 minute

    assertThat(store.getToken()).isEqualTo("test-token");
    assertThat(store.isTokenValid()).isTrue();
  }

  @Test
  public void testExpiredToken() {
    ScimOAuth2TokenStore store = new ScimOAuth2TokenStore();
    store.setToken("test-token");
    store.setExpiryTime(System.currentTimeMillis() - 1000); // expired 1 second ago

    assertThat(store.getToken()).isEqualTo("test-token");
    assertThat(store.isTokenValid()).isFalse();
  }

  @Test
  public void testTokenSharedAcrossReferences() {
    ScimOAuth2TokenStore store = new ScimOAuth2TokenStore();
    store.setToken("shared-token");
    store.setExpiryTime(System.currentTimeMillis() + 60000);

    // Simulate two ScimClient instances sharing the same store
    ScimOAuth2TokenStore ref1 = store;
    ScimOAuth2TokenStore ref2 = store;

    assertThat(ref1.getToken()).isEqualTo("shared-token");
    assertThat(ref2.getToken()).isEqualTo("shared-token");

    // Update token through one reference
    ref1.setToken("updated-token");
    assertThat(ref2.getToken()).isEqualTo("updated-token");
  }

  @Test
  public void testFactorySharesTokenStore() {
    ScimConfiguration config = new ScimConfiguration();
    config.setServerUrl("https://scim.example.com");
    config.setAuthenticationType("oauth2");

    ScimIdentityProviderFactory factory = new ScimIdentityProviderFactory();
    factory.setScimConfiguration(config);

    // The factory should create a single shared token store for oauth2
    ScimOAuth2TokenStore store1 = factory.getOAuth2TokenStore();
    ScimOAuth2TokenStore store2 = factory.getOAuth2TokenStore();
    assertThat(store1).isNotNull();
    assertThat(store1).isSameAs(store2);
  }

  @Test
  public void testFactoryNoTokenStoreForBearer() {
    ScimConfiguration config = new ScimConfiguration();
    config.setServerUrl("https://scim.example.com");
    config.setAuthenticationType("bearer");

    ScimIdentityProviderFactory factory = new ScimIdentityProviderFactory();
    factory.setScimConfiguration(config);

    assertThat(factory.getOAuth2TokenStore()).isNull();
  }
}
