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
package org.operaton.bpm.engine.rest.security.auth.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompositeAuthenticationProviderTest {

  private AuthenticationProvider primaryProvider;
  private AuthenticationProvider fallbackProvider;
  private CompositeAuthenticationProvider compositeProvider;
  private HttpServletRequest request;
  private ProcessEngine engine;

  @BeforeEach
  void setUp() {
    primaryProvider = mock(AuthenticationProvider.class);
    fallbackProvider = mock(AuthenticationProvider.class);
    compositeProvider = new CompositeAuthenticationProvider(primaryProvider, fallbackProvider);
    request = mock(HttpServletRequest.class);
    engine = mock(ProcessEngine.class);
  }

  @Test
  void shouldUsePrimaryProviderWhenItAuthenticates() {
    when(primaryProvider.extractAuthenticatedUser(request, engine))
        .thenReturn(AuthenticationResult.successful("jwt-user"));

    AuthenticationResult result = compositeProvider.extractAuthenticatedUser(request, engine);

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getAuthenticatedUser()).isEqualTo("jwt-user");
    verify(fallbackProvider, never()).extractAuthenticatedUser(request, engine);
  }

  @Test
  void shouldUseFallbackProviderWhenPrimaryProviderFails() {
    when(primaryProvider.extractAuthenticatedUser(request, engine))
        .thenReturn(AuthenticationResult.unsuccessful());
    when(fallbackProvider.extractAuthenticatedUser(request, engine))
        .thenReturn(AuthenticationResult.successful("basic-user"));

    AuthenticationResult result = compositeProvider.extractAuthenticatedUser(request, engine);

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getAuthenticatedUser()).isEqualTo("basic-user");
    verify(fallbackProvider).extractAuthenticatedUser(request, engine);
  }

  @Test
  void shouldReturnUnsuccessfulWhenBothProvidersFail() {
    when(primaryProvider.extractAuthenticatedUser(request, engine))
        .thenReturn(AuthenticationResult.unsuccessful());
    when(fallbackProvider.extractAuthenticatedUser(request, engine))
        .thenReturn(AuthenticationResult.unsuccessful());

    AuthenticationResult result = compositeProvider.extractAuthenticatedUser(request, engine);

    assertThat(result.isAuthenticated()).isFalse();
  }

  @Test
  void shouldDelegateAuthenticationChallengeToBothProviders() {
    HttpServletResponse response = mock(HttpServletResponse.class);

    compositeProvider.augmentResponseByAuthenticationChallenge(response, engine);

    verify(primaryProvider).augmentResponseByAuthenticationChallenge(response, engine);
    verify(fallbackProvider).augmentResponseByAuthenticationChallenge(response, engine);
  }

}
