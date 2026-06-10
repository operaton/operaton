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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;
import org.operaton.bpm.engine.rest.security.auth.impl.jwt.JwtUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenAuthenticationProviderTest {

  private static final String USER_ID = "demo";
  private static final String JWT_SECRET = Base64.getEncoder()
      .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

  @Test
  void shouldAuthenticateBearerJwt() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(JwtTokenAuthenticationProvider.BEARER_PREFIX + createToken(USER_ID));
    JwtTokenAuthenticationProvider provider = new JwtTokenAuthenticationProvider(JWT_SECRET);

    AuthenticationResult result = provider.extractAuthenticatedUser(request, null);

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getAuthenticatedUser()).isEqualTo(USER_ID);
  }

  @Test
  void shouldRejectMissingBearerToken() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    JwtTokenAuthenticationProvider provider = new JwtTokenAuthenticationProvider(JWT_SECRET);

    AuthenticationResult result = provider.extractAuthenticatedUser(request, null);

    assertThat(result.isAuthenticated()).isFalse();
  }

  @Test
  void shouldRejectJwtWhenSecretIsMissing() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(JwtTokenAuthenticationProvider.BEARER_PREFIX + createToken(USER_ID));
    JwtTokenAuthenticationProvider provider = new JwtTokenAuthenticationProvider(null);

    AuthenticationResult result = provider.extractAuthenticatedUser(request, null);

    assertThat(result.isAuthenticated()).isFalse();
  }

  @Test
  void shouldRejectInvalidJwt() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(JwtTokenAuthenticationProvider.BEARER_PREFIX + "invalid");
    JwtTokenAuthenticationProvider provider = new JwtTokenAuthenticationProvider(JWT_SECRET);

    AuthenticationResult result = provider.extractAuthenticatedUser(request, null);

    assertThat(result.isAuthenticated()).isFalse();
  }

  private static String createToken(String userId) throws Exception {
    JwtUser user = new JwtUser(userId);
    SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(JWT_SECRET));
    return Jwts.builder()
        .subject(userId)
        .expiration(new Date(System.currentTimeMillis() + 60000L))
        .issuedAt(new Date())
        .claim("user", new ObjectMapper().writeValueAsString(user))
        .signWith(key)
        .compact();
  }

}
