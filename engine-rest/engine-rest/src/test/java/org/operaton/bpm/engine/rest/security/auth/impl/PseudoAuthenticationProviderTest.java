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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PseudoAuthenticationProviderTest {

  private final PseudoAuthenticationProvider provider = new PseudoAuthenticationProvider();

  @Test
  void shouldAuthenticateUserFromContextHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(PseudoAuthenticationProvider.USER_ID_HEADER)).thenReturn("trusted-user");

    AuthenticationResult result = provider.extractAuthenticatedUser(request, null);

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getAuthenticatedUser()).isEqualTo("trusted-user");
  }

  @Test
  void shouldRejectMissingContextHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);

    AuthenticationResult result = provider.extractAuthenticatedUser(request, null);

    assertThat(result.isAuthenticated()).isFalse();
  }

  @Test
  void shouldRejectBlankContextHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(PseudoAuthenticationProvider.USER_ID_HEADER)).thenReturn(" ");

    AuthenticationResult result = provider.extractAuthenticatedUser(request, null);

    assertThat(result.isAuthenticated()).isFalse();
  }

}
