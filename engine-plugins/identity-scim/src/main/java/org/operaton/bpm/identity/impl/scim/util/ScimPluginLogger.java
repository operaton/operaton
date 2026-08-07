/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.identity.impl.scim.util;

import org.operaton.commons.logging.BaseLogger;

/**
 * Logger for SCIM Identity Provider Plugin.
 */
public class ScimPluginLogger extends BaseLogger {

  public static final String PROJECT_CODE = "SCIM";

  public static final ScimPluginLogger INSTANCE = BaseLogger.createLogger(
      ScimPluginLogger.class, PROJECT_CODE, "org.operaton.bpm.identity.impl.scim", "00");

  public void pluginActivated(String pluginClassName, String engineName) {
    logInfo("001", "PLUGIN {} activated on process engine {}", pluginClassName, engineName);
  }

  public void acceptingUntrustedCertificates() {
    logWarn("002", "Enabling accept of untrusted certificates. Use at own risk.");
  }

  public void httpClientException(String operation, Exception e) {
    logError("003", "HTTP client exception during {}: {}", operation, e.getMessage(), e);
  }

  public void invalidScimEntityReturned(String entityType, String resourceId) {
    logError("004", "SCIM query returned a {} with id null. Resource ID: {}. This entity will be ignored.", entityType, resourceId);
  }

  public void queryResult(String result) {
    logDebug("005", result);
  }

  public void oauth2TokenRefresh(boolean verbose) {
    if (verbose) {
      logInfo("006", "Refreshing OAuth2 access token");
    } else {
      logDebug("006", "Refreshing OAuth2 access token");
    }
  }

  public void scimFilterQuery(String filter) {
    logDebug("007", "SCIM filter query: {}", filter);
  }

  public void scimRequestError(int statusCode, String responseBody) {
    logError("008", "SCIM request failed with status code {}: {}", statusCode, responseBody);
  }

  public void authenticationFailure(String message) {
    logError("009", "SCIM authentication failure: {}", message);
  }

  public void httpClientRequest(boolean verbose, String method, String url, String body, boolean hideBody) {
    body = (body == null) ? "empty" : (hideBody == true) ? "***" : body;
    if (verbose) {
      logInfo("010", ">>>>>>> ScimClient {}: {} => body: {}", method, url, (body != null ? body : "empty"));
    } else {
      logDebug("010", ">>>>>>> ScimClient {}: {} => body: {}", method, url, (body != null ? body : "empty"));
    }
  }

  public void httpClientResponse(boolean verbose, String method, int code) {
    if (verbose) {
      logInfo("011", "<<<<<<< ScimClient {}: status code {}", method, code);
    } else {
      logDebug("011", "<<<<<<< ScimClient {}: status code {}", method, code);
    }
  }

  public void userAuthentication(boolean verbose, String userName) {
    if (verbose) {
      logInfo("012", "SCIM authenticate user {}", userName);
    } else {
      logDebug("012", "SCIM authenticate user {}", userName);
    }
  }

  public void userAuthenticationRequest(boolean verbose, String protocol, String url, String userName) {
    if (verbose) {
      logInfo("013", ">>>>>>> ScimClient {}: {} => user: {}", protocol, url, userName);
    } else {
      logDebug("013", ">>>>>>> ScimClient {}: {} => user: {}", protocol, url, userName);
    }
  }

  public void userAuthenticationResponse(boolean verbose, String protocol, String userName, int code) {
    if (verbose) {
      logInfo("014", "<<<<<<< ScimClient {}: user {} status code {}", protocol, userName, code);
    } else {
      logDebug("014", "<<<<<<< ScimClient {}: user {} status code {}", protocol, userName, code);
    }
  }


  public void userCacheUnavailable() {
    logWarn("015", "SCIM can't cache authenticated user");
  }
}
