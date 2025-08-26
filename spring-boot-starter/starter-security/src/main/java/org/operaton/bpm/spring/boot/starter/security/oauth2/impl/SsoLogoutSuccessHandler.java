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
package org.operaton.bpm.spring.boot.starter.security.oauth2.impl;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import org.operaton.bpm.spring.boot.starter.security.oauth2.OAuth2Properties;

/**
 * {@link OidcClientInitiatedLogoutSuccessHandler} with logging.
 */
public class SsoLogoutSuccessHandler extends OidcClientInitiatedLogoutSuccessHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SsoLogoutSuccessHandler.class);

  public SsoLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository,
                                 OAuth2Properties oAuth2Properties) {
    super(clientRegistrationRepository);
    this.setPostLogoutRedirectUri(oAuth2Properties.getSsoLogout().getPostLogoutRedirectUri());
  }

  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    LOGGER.debug("Initiating SSO logout for '{}'", authentication.getName());
    super.onLogoutSuccess(request, response, authentication);
  }
}
