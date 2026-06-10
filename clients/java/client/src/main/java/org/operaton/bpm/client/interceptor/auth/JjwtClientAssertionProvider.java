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

import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import org.operaton.bpm.client.exception.ExternalTaskClientException;

/**
 * Creates signed RFC 7523 client assertion JWTs with JJWT.
 */
public class JjwtClientAssertionProvider implements ClientAssertionProvider {

  protected static final long ASSERTION_LIFETIME_MS = 5 * 60 * 1000L;

  protected final String clientId;
  protected final String audience;
  protected final PrivateKey privateKey;

  public JjwtClientAssertionProvider(String clientId, String audience, PrivateKey privateKey) {
    this.clientId = clientId;
    this.audience = audience;
    this.privateKey = privateKey;
  }

  @Override
  public String getAssertion() {
    Date now = new Date();
    Date exp = new Date(now.getTime() + ASSERTION_LIFETIME_MS);

    try {
      return Jwts.builder()
          .issuer(clientId)
          .subject(clientId)
          .audience().add(audience).and()
          .id(UUID.randomUUID().toString())
          .issuedAt(now)
          .expiration(exp)
          .signWith(privateKey)
          .compact();
    } catch (JwtException e) {
      throw new ExternalTaskClientException("Failed to sign client assertion JWT", e);
    }
  }

}
