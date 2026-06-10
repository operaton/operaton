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

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;

/**
 * Trusts a user id supplied by an upstream component in a custom HTTP header.
 */
public class PseudoAuthenticationProvider implements AuthenticationProvider {

  public static final String USER_ID_HEADER = "Context-User-ID";

  @Override
  public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
    String userIdHeader = request.getHeader(USER_ID_HEADER);
    if (userIdHeader == null || userIdHeader.isBlank()) {
      return AuthenticationResult.unsuccessful();
    }
    return AuthenticationResult.successful(userIdHeader);
  }

  @Override
  public void augmentResponseByAuthenticationChallenge(HttpServletResponse response, ProcessEngine engine) {
    // no challenge for trusted-header authentication
  }

}
