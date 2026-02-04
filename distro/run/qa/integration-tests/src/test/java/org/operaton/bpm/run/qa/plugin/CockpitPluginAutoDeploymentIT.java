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
package org.operaton.bpm.run.qa.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.run.qa.util.SpringBootManagedContainer;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CockpitPluginAutoDeploymentIT {

  static final String EXAMPLE_PLUGIN_HOME = "example.plugin.home";
  static final String PLUGIN_ENDPOINT = "/operaton/api/cockpit/plugin/test-cockpit-plugin/test-string";

  static SpringBootManagedContainer container;
  static String baseDirectory = SpringBootManagedContainer.getRunHome();

  protected List<String> deployedPlugins = new ArrayList<>();

  @AfterEach
  void teardown() {
    stopApp();
    undeployPlugins();
  }

  void stopApp() {
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

  void runStartScript() {
    container = new SpringBootManagedContainer();
    container.replaceConfigurationYml(SpringBootManagedContainer.APPLICATION_YML_PATH,
        SpringBootManagedContainer.class.getClassLoader().getResourceAsStream("base-test-application.yml"));
    try {
      container.start();
    } catch (Exception e) {
      throw new RuntimeException("Cannot start managed Spring Boot application!", e);
    }
  }

  @Test
  void shouldAutoDeployCockpitPlugin() throws Exception {
    // given
    deployPlugin("operaton-bpm-run-example-plugin.jar");
    runStartScript();

    // when
    Response response = when().get(container.getBaseUrl() + PLUGIN_ENDPOINT);

    // then
    String responseBody = response.then()
      .statusCode(Status.OK.getStatusCode())
      .extract().body().asString();

    assertThat(responseBody).isEqualTo("test string");
  }

  void deployPlugin(String jarName) throws IOException {
    Path runUserlibDir = Path.of(baseDirectory, SpringBootManagedContainer.USERLIB_PATH);
    String pluginHome = System.getProperty(EXAMPLE_PLUGIN_HOME);

    if (pluginHome == null || pluginHome.isEmpty()) {
      throw new RuntimeException("System property %s not set. This property must point to the root directory of the plugin to deploy.".formatted(EXAMPLE_PLUGIN_HOME));
    }

    Path pluginPath = Path.of(pluginHome, jarName).toAbsolutePath();
    Path copy = Files.copy(pluginPath, runUserlibDir.resolve(pluginPath.getFileName()));

    deployedPlugins.add(copy.toString());
  }

  void undeployPlugins() {
    for (String pluginPath : deployedPlugins) {
      assertThatCode(() -> Files.delete(Path.of(pluginPath)))
        .as("unable to undeploy plugin " + pluginPath)
        .doesNotThrowAnyException();
    }
  }
}
