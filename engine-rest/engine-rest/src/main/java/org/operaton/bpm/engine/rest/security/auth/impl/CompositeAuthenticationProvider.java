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
 * Tries JWT bearer authentication first and falls back to HTTP Basic.
 */
public class CompositeAuthenticationProvider implements AuthenticationProvider {

  protected final AuthenticationProvider primaryProvider;
  protected final AuthenticationProvider fallbackProvider;

  public CompositeAuthenticationProvider() {
    this(new JwtTokenAuthenticationProvider(), new HttpBasicAuthenticationProvider());
  }

  public CompositeAuthenticationProvider(AuthenticationProvider primaryProvider, AuthenticationProvider fallbackProvider) {
    this.primaryProvider = primaryProvider;
    this.fallbackProvider = fallbackProvider;
  }

  @Override
  public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
    AuthenticationResult result = primaryProvider.extractAuthenticatedUser(request, engine);
    if (result.isAuthenticated()) {
      return result;
    }
    return fallbackProvider.extractAuthenticatedUser(request, engine);
  }

  @Override
  public void augmentResponseByAuthenticationChallenge(HttpServletResponse response, ProcessEngine engine) {
    primaryProvider.augmentResponseByAuthenticationChallenge(response, engine);
    fallbackProvider.augmentResponseByAuthenticationChallenge(response, engine);
  }

}
