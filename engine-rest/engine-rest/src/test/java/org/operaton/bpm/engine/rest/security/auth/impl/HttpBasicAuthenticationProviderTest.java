/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.security.auth.impl;

import java.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;

import static org.operaton.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider.BASIC_AUTH_HEADER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

class HttpBasicAuthenticationProviderTest {

  private static final String USER_ID = "user";

  private static final String PASSWORD = "password";

  private static final String CREDENTIALS =
      Base64.getEncoder().encodeToString("%s:%s".formatted(USER_ID, PASSWORD).getBytes());

  private static final String AUTHORIZATION_HEADER = BASIC_AUTH_HEADER_PREFIX + CREDENTIALS;

  private HttpBasicAuthenticationProvider provider;

  private HttpServletRequest request;

  private ProcessEngine engine;

  private IdentityService identityService;

  @BeforeEach
  void setUp() {
    provider = new HttpBasicAuthenticationProvider();
    request = Mockito.mock(HttpServletRequest.class);
    engine = Mockito.mock(ProcessEngine.class);
    identityService = Mockito.mock(IdentityService.class);
    Mockito.when(engine.getIdentityService()).thenReturn(identityService);
  }

  @Test
  void testExtractAuthenticatedUserNoAuthHeader() {
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertThat(result.isAuthenticated()).isFalse();
    assertThat(result.getAuthenticatedUser()).isNull();
  }

  @Test
  void testExtractAuthenticatedUserHeaderNotBasic() {
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("NOTBASIC user:password");
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertThat(result.isAuthenticated()).isFalse();
    assertThat(result.getAuthenticatedUser()).isNull();
  }

  @Test
  void testExtractAuthenticatedUserNoCredentials() {
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_AUTH_HEADER_PREFIX);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertThat(result.isAuthenticated()).isFalse();
    assertThat(result.getAuthenticatedUser()).isNull();
  }

  @Test
  void testExtractAuthenticatedUserValidCredentials() {
    Mockito.when(identityService.checkPassword(USER_ID, PASSWORD)).thenReturn(true);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AUTHORIZATION_HEADER);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getAuthenticatedUser()).isEqualTo(USER_ID);
  }

  @Test
  void testExtractAuthenticatedUserInvalidCredentials() {
    Mockito.when(identityService.checkPassword(USER_ID, PASSWORD)).thenReturn(false);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AUTHORIZATION_HEADER);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertThat(result.isAuthenticated()).isFalse();
    assertThat(result.getAuthenticatedUser()).isEqualTo(USER_ID);
  }

  @Test
  void testExtractAuthenticatedUserInvalidBase64() {
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_AUTH_HEADER_PREFIX + "!!!invalidbase64!!!");
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertThat(result.isAuthenticated()).isFalse();
    assertThat(result.getAuthenticatedUser()).isNull();
  }

  @Test
  void testAugmentResponseByAuthenticationChallengeSetsHeader() {
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    Mockito.when(engine.getName()).thenReturn("testEngine");
    provider.augmentResponseByAuthenticationChallenge(response, engine);
    Mockito.verify(response).setHeader(HttpHeaders.WWW_AUTHENTICATE,
        BASIC_AUTH_HEADER_PREFIX + "realm=\"testEngine\"");
  }
}
