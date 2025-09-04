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
package org.operaton.bpm.run.qa.webapps;

import java.util.Arrays;
import java.util.Collection;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.run.qa.util.SpringBootManagedContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE:
 * copied from
 * <a href="https://github.com/operaton/operaton/blob/main/qa/integration-tests-webapps/integration-tests/src/main/java/org/operaton/bpm/PluginsRootResourceIT.java">platform</a>
 * then added <code>@BeforeParam</code> and <code>@AfterParam</code> methods for container setup
 * and changed  <code>appBasePath</code> to <code>APP_BASE_PATH</code>, might be removed with
 * <a href="https://jira.camunda.com/browse/CAM-11379">CAM-11379</a>
 */
class PluginsRootResourceIT extends AbstractWebIT {
  String assetName;
  boolean assetAllowed;

  @BeforeEach
  void createClient() throws Exception {
    createClient(getWebappCtxPath());
  }

  private SpringBootManagedContainer container;

  void startContainer() {
    container = new SpringBootManagedContainer("--webapps");
    try {
      container.start();
    } catch (Exception e) {
      throw new RuntimeException("Cannot start managed Spring Boot application!", e);
    }
  }

  @AfterEach
  void stopContainer() {
    try {
      if (container != null) {
        container.stop();
      }
    } catch (Exception e) {
      throw new RuntimeException("Cannot stop managed Spring Boot application!", e);
    } finally {
      container = null;
    }
  }

  static Collection<Object[]> getAssets() {
    return Arrays.asList(new Object[][]{
        {"app/plugin.js", true},
        {"app/plugin.css", true},
        {"app/asset.js", false},
        {"../..", false},
        {"../../annotations-api.jar", false},
    });
  }

  @MethodSource("getAssets")
  @ParameterizedTest(name = "Test instance: {index}. Asset: {0}, Allowed: {1}")
  void shouldGetAssetIfAllowed(String assetName, boolean assetAllowed) {
    startContainer();
    initPluginsRootResourceIT(assetName, assetAllowed);
    // when
    Response response = getAsset("api/admin/plugin/adminPlugins/static/" + assetName);

    // then
    assertResponse(assetName, response);

    // cleanup
    response.close();
  }

  protected Response getAsset(String path) {
    // Ensure the client is properly initialized
    JerseyClient client = (JerseyClient) JerseyClientBuilder.newClient();

    // Build the target URI using the base path and path parameter
    String fullPath = appBasePath + path;

    // Perform the GET request to fetch the asset
    return client.target(fullPath)  // Use target() to define the endpoint
            .request()  // Prepare to send the request
            .get();  // Execute the GET request
  }

  protected void assertResponse(String asset, Response response) {
    if (assetAllowed) {
      assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    } else {
      assertThat(response.getStatus()).isEqualTo(Status.FORBIDDEN.getStatusCode());
      assertThat(response.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
      String responseEntity = response.readEntity(String.class);
      assertThat(responseEntity)
              .contains("\"type\":\"RestException\"")
              .contains("\"message\":\"Not allowed to load the following file '" + asset + "'.\"");
    }
  }

  void initPluginsRootResourceIT(String assetName, boolean assetAllowed) {
    this.assetName = assetName;
    this.assetAllowed = assetAllowed;
  }

}
