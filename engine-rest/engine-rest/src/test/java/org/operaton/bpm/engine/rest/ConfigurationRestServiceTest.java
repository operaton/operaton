/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.rest.impl.ConfigurationRestService;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.when;

public class ConfigurationRestServiceTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String CONFIGURATION_URL = TEST_RESOURCE_ROOT_PATH + ConfigurationRestService.PATH;
  protected static final String NAMED_ENGINE_CONFIGURATION_URL = TEST_RESOURCE_ROOT_PATH + "/engine/{name}" + ConfigurationRestService.PATH;

  private ProcessEngineConfiguration processEngineConfigurationMock;

  @BeforeEach
  void setUpMocks() {
    processEngineConfigurationMock = processEngine.getProcessEngineConfiguration();

    when(processEngineConfigurationMock.getProcessEngineName()).thenReturn("default");
    when(processEngineConfigurationMock.getHistory()).thenReturn("full");
    when(processEngineConfigurationMock.isAuthorizationEnabled()).thenReturn(true);
    when(processEngineConfigurationMock.isEnablePasswordPolicy()).thenReturn(false);
  }

  @Test
  void testGetConfiguration() {
    given()
      .header(ACCEPT_JSON_HEADER)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("engineName", equalTo("default"))
      .body("historyLevel", equalTo("full"))
      .body("authorizationEnabled", equalTo(true))
      .body("enablePasswordPolicy", equalTo(false))
    .when().get(CONFIGURATION_URL);
  }

  @Test
  void testGetConfigurationWithNamedEngine() {
    given()
      .header(ACCEPT_JSON_HEADER)
      .pathParam("name", "default")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("engineName", equalTo("default"))
      .body("historyLevel", equalTo("full"))
      .body("authorizationEnabled", equalTo(true))
      .body("enablePasswordPolicy", equalTo(false))
    .when().get(NAMED_ENGINE_CONFIGURATION_URL);
  }

  @Test
  void testGetConfigurationDifferentValues() {
    when(processEngineConfigurationMock.getProcessEngineName()).thenReturn("custom");
    when(processEngineConfigurationMock.getHistory()).thenReturn("none");
    when(processEngineConfigurationMock.isAuthorizationEnabled()).thenReturn(false);
    when(processEngineConfigurationMock.isEnablePasswordPolicy()).thenReturn(true);

    given()
      .header(ACCEPT_JSON_HEADER)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("engineName", equalTo("custom"))
      .body("historyLevel", equalTo("none"))
      .body("authorizationEnabled", equalTo(false))
      .body("enablePasswordPolicy", equalTo(true))
    .when().get(CONFIGURATION_URL);
  }

}
