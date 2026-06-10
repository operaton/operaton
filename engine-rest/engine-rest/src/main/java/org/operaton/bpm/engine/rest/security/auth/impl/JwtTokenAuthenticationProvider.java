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

import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.operaton.bpm.engine.AuthenticationException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;
import org.operaton.bpm.engine.rest.security.auth.impl.jwt.Configuration;
import org.operaton.bpm.engine.rest.security.auth.impl.jwt.JwtUser;

public class JwtTokenAuthenticationProvider implements AuthenticationProvider {

  public static final String BEARER_PREFIX = "Bearer ";

  protected final String jwtSecret;
  protected final ObjectMapper objectMapper;

  public JwtTokenAuthenticationProvider() {
    this(Configuration.getInstance().getSecret());
  }

  JwtTokenAuthenticationProvider(String jwtSecret) {
    this.jwtSecret = jwtSecret;
    this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      return AuthenticationResult.unsuccessful();
    }
    if (jwtSecret == null || jwtSecret.isBlank()) {
      return AuthenticationResult.unsuccessful();
    }

    try {
      JwtUser user = parse(authHeader, jwtSecret);
      if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
        return AuthenticationResult.unsuccessful();
      }
      return AuthenticationResult.successful(user.getUserID());
    } catch (AuthenticationException e) {
      return AuthenticationResult.unsuccessful();
    }
  }

  @Override
  public void augmentResponseByAuthenticationChallenge(HttpServletResponse response, ProcessEngine engine) {
    // no additional challenge; composite mode delegates HTTP Basic challenge to the fallback provider
  }

  JwtUser parse(String tokenHeader, String jwtTokenSecret) {
    try {
      String token = tokenHeader.substring(BEARER_PREFIX.length());
      SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtTokenSecret));
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      Object userClaim = claims.get("user");
      if (!(userClaim instanceof String userJson)) {
        throw new AuthenticationException(tokenHeader, "JWT does not contain a serialized user claim");
      }
      return deserialize(userJson);
    } catch (IllegalArgumentException | JwtException e) {
      throw new AuthenticationException(tokenHeader, e.getMessage());
    }
  }

  protected JwtUser deserialize(String json) {
    try {
      return objectMapper.readValue(json, JwtUser.class);
    } catch (IllegalArgumentException | IOException e) {
      throw new AuthenticationException(json, e.getMessage());
    }
  }

}
