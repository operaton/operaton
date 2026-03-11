/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.impl.AuthLogger;
import org.operaton.bpm.engine.rest.impl.RestLogger;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;

/**
 * Implementation of the {@link AuthenticationProvider} interface that performs
 * HTTP Basic Authentication against the identity service of a process engine.
 *
 * <p>
 * This class extracts credentials from the "Authorization" HTTP header, decodes
 * them, and validates them using the process engine's identity service.
 * </p>
 *
 * <p>
 * If authentication fails, an appropriate challenge response is added to the
 * HTTP response.
 * </p>
 *
 * @author Thorben Lindhauer
 */
public class HttpBasicAuthenticationProvider implements AuthenticationProvider {

  // Prefix for the HTTP Basic Authentication header
  protected static final String BASIC_AUTH_HEADER_PREFIX = "Basic ";

  private static final AuthLogger LOG = RestLogger.AUTH_LOGGER;

  /**
   * Extracts and authenticates the user from the HTTP request using Basic Authentication.
   *
   * @param request the HTTP request containing the "Authorization" header
   * @param engine the process engine used for authentication
   * @return an {@link AuthenticationResult} indicating success or failure
   */
  @Override
  public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request,
      ProcessEngine engine) {
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    // Check if the Authorization header is present and starts with the Basic prefix
    if (authorizationHeader != null && authorizationHeader.startsWith(BASIC_AUTH_HEADER_PREFIX)) {
      // Extract and decode the Base64-encoded credentials
      String encodedCredentials = authorizationHeader.substring(BASIC_AUTH_HEADER_PREFIX.length());
      String decodedCredentials;
      try {
        decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials));
      } catch (IllegalArgumentException e) {
        // IllegalArgumentException is thrown if the Base64 decoding fails.
        // Maintains compatibility with previous behavior where an invalid
        // Base64 string would lead to an unsuccessful authentication.
        LOG.warnInvalidAuthHeader(e);
        return AuthenticationResult.unsuccessful();
      }
      // Find the first colon to separate username and password
      int firstColonIndex = decodedCredentials.indexOf(":");

      if (firstColonIndex == -1) {
        return AuthenticationResult.unsuccessful();
      } else {
        String userName = decodedCredentials.substring(0, firstColonIndex);
        String password = decodedCredentials.substring(firstColonIndex + 1);

        // Authenticate the user using the process engine's identity service
        if (isAuthenticated(engine, userName, password)) {
          return AuthenticationResult.successful(userName);
        } else {
          return AuthenticationResult.unsuccessful(userName);
        }
      }
    } else {
      return AuthenticationResult.unsuccessful();
    }
  }

  /**
   * Validates the provided username and password against the process engine's identity service.
   *
   * @param engine the process engine used for authentication
   * @param userName the username to authenticate
   * @param password the password to authenticate
   * @return true if the credentials are valid, false otherwise
   */
  protected boolean isAuthenticated(ProcessEngine engine, String userName, String password) {
    return engine.getIdentityService().checkPassword(userName, password);
  }

  /**
   * Adds an HTTP Basic Authentication challenge to the response.
   *
   * @param response the HTTP response to augment
   * @param engine the process engine providing the authentication realm
   */
  @Override
  public void augmentResponseByAuthenticationChallenge(
      HttpServletResponse response, ProcessEngine engine) {
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "%srealm=\"%s\"".formatted(BASIC_AUTH_HEADER_PREFIX, engine.getName()));
  }
}
