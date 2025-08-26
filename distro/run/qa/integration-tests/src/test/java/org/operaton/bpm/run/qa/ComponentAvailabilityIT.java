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
package org.operaton.bpm.run.qa;

import java.util.Arrays;
import java.util.Collection;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.run.qa.util.SpringBootManagedContainer;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * Test cases for ensuring connectivity to REST API based on startup parameters
 */
class ComponentAvailabilityIT {
  public String[] commands;
  public boolean restAvailable;
  public boolean webappsAvailable;
  public boolean exampleAvailable;

  public static Collection<Object[]> commands() {
    return Arrays.asList(new Object[][] {
        { new String[0], true, true, true },
        { new String[]{"--rest"}, true, false, false },
        { new String[]{"--rest", "--webapps"}, true, true, false },
        { new String[]{"--rest", "--example"}, true, false, true },
        { new String[]{"--webapps"}, false, true, false },
        { new String[]{"--rest", "--webapps"}, true, true, false },
        { new String[]{"--rest", "--webapps", "--example"}, true, true, true },
        { new String[]{"--rest", "--webapps", "--example", "--oauth2"}, true, true, true }
    });
  }

  private SpringBootManagedContainer container;

  void startContainer(String[] commands) {
    container = new SpringBootManagedContainer(commands);
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

  @ParameterizedTest(name = "Test instance: {index}. Rest: {1}, Webapps: {2}, Example: {3}")
  @MethodSource("commands")
  void shouldFindEngineViaRestApiRequest(String[] commands, boolean restAvailable, boolean webappsAvailable, boolean exampleAvailable) {
    startContainer(commands);
    initComponentAvailabilityIT(commands, restAvailable, webappsAvailable, exampleAvailable);
    Response response = when().get(container.getBaseUrl() + "/engine-rest/engine");
    if (restAvailable) {
      response.then()
          .body("size()", is(1))
          .body("name[0]", is("default"));
    } else {
      response.then()
          .statusCode(404);
    }
  }

  @ParameterizedTest(name = "Test instance: {index}. Rest: {1}, Webapps: {2}, Example: {3}")
  @MethodSource("commands")
  void shouldFindWelcomeApp(String[] commands, boolean restAvailable, boolean webappsAvailable, boolean exampleAvailable) {
    startContainer(commands);
    initComponentAvailabilityIT(commands, restAvailable, webappsAvailable, exampleAvailable);
    Response response = when().get(container.getBaseUrl() + "/operaton/app/welcome/default");
    if (webappsAvailable) {
      response.then()
          .statusCode(200)
          .body("html.head.title", equalTo("Operaton Welcome"));
    } else {
      response.then()
          .statusCode(404);
    }
  }

  @ParameterizedTest(name = "Test instance: {index}. Rest: {1}, Webapps: {2}, Example: {3}")
  @MethodSource("commands")
  void shouldFindExample(String[] commands, boolean restAvailable, boolean webappsAvailable, boolean exampleAvailable) {
    startContainer(commands);
    initComponentAvailabilityIT(commands, restAvailable, webappsAvailable, exampleAvailable);
    Response response = when().get(container.getBaseUrl() + "/engine-rest/process-definition");
    if (exampleAvailable && restAvailable) {
      response.then()
          .body("size()", is(3))
          .body("key[0]", is("ReviewInvoice"));
    } else if (restAvailable) {
      response.then()
          .body("size()", is(0));
    } else {
      response.then()
          .statusCode(404);
    }
  }

  public void initComponentAvailabilityIT(String[] commands, boolean restAvailable, boolean webappsAvailable, boolean exampleAvailable) {
    this.commands = commands;
    this.restAvailable = restAvailable;
    this.webappsAvailable = webappsAvailable;
    this.exampleAvailable = exampleAvailable;
  }
}
