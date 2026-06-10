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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.operaton.bpm.client.exception.ExternalTaskClientException;

/**
 * Supplies Azure Workload Identity federated tokens as OAuth2 client assertions.
 */
public class AzureWorkloadIdentityAssertionProvider implements ClientAssertionProvider {

  protected static final String ENV_FEDERATED_TOKEN_FILE = "AZURE_FEDERATED_TOKEN_FILE";

  @Override
  public String getAssertion() {
    String tokenFilePath = System.getenv(ENV_FEDERATED_TOKEN_FILE);
    if (tokenFilePath == null || tokenFilePath.isBlank()) {
      throw new ExternalTaskClientException(
          "Environment variable '" + ENV_FEDERATED_TOKEN_FILE + "' is not set. "
              + "Ensure the Azure Workload Identity webhook has been applied to this workload.");
    }

    try {
      return Files.readString(Path.of(tokenFilePath), StandardCharsets.UTF_8).strip();
    } catch (IOException e) {
      throw new ExternalTaskClientException(
          "Failed to read Azure federated identity token from file '" + tokenFilePath + "'", e);
    }
  }

}
