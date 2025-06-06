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
package org.operaton.bpm.engine.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerRule;
import org.operaton.bpm.engine.telemetry.ApplicationServer;
import org.operaton.bpm.engine.telemetry.Database;
import org.operaton.bpm.engine.telemetry.Internals;
import org.operaton.bpm.engine.telemetry.Jdk;
import org.operaton.bpm.engine.telemetry.Product;
import org.operaton.bpm.engine.telemetry.TelemetryData;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import io.restassured.http.ContentType;

public class TelemetryRestServiceTest extends AbstractRestServiceTest {

  @ClassRule
  public static TestContainerRule rule = new TestContainerRule();

  protected static final String TELEMETRY_URL = TEST_RESOURCE_ROOT_PATH +  TelemetryRestService.PATH;
  protected static final String TELEMETRY_CONFIG_URL = TELEMETRY_URL + "/configuration";
  protected static final String TELEMETRY_DATA_URL = TELEMETRY_URL + "/data";

  protected ManagementService managementServiceMock;


  @Before
  public void setupMocks() {
    managementServiceMock = mock(ManagementService.class);
    when(processEngine.getManagementService()).thenReturn(managementServiceMock);
  }

  @Test
  public void shouldDisableTelemetry() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("enableTelemetry", false);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
    .post(TELEMETRY_CONFIG_URL);

    verify(managementServiceMock).toggleTelemetry(false);
  }

  @Test
  public void shouldFetchDisabledTelemetryConfiguration() {
    when(managementServiceMock.isTelemetryEnabled()).thenReturn(false);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("enableTelemetry", equalTo(false))
    .when()
      .get(TELEMETRY_CONFIG_URL);

    verify(managementServiceMock).isTelemetryEnabled();
  }

  @Test
  public void shouldGetTelemetryData() {
    when(managementServiceMock.getTelemetryData()).thenReturn(MockProvider.EXAMPLE_TELEMETRY_DATA);

    given()
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("installation", equalTo(MockProvider.EXAMPLE_TELEMETRY_INSTALLATION_ID))
        .body("product.name", equalTo(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_NAME))
        .body("product.version", equalTo(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_VERSION))
        .body("product.edition", equalTo(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_EDITION))
        .body("product.internals.database.vendor", equalTo(MockProvider.EXAMPLE_TELEMETRY_DB_VENDOR))
        .body("product.internals.database.version", equalTo(MockProvider.EXAMPLE_TELEMETRY_DB_VERSION))
        .body("product.internals.commands.FetchExternalTasksCmd.count", equalTo(100))
        .body("product.internals.commands.StartProcessInstanceCmd.count", equalTo(40))
        .body("product.internals.metrics.process-instances.count", equalTo(936))
        .body("product.internals.metrics.flow-node-instances.count", equalTo(6125))
        .body("product.internals.metrics.executed-decision-elements.count", equalTo(732))
        .body("product.internals.metrics.decision-instances.count", equalTo(140))
        .body("product.internals.webapps[0]", equalTo("cockpit"))
        .body("product.internals.jdk.vendor", equalTo(MockProvider.EXAMPLE_TELEMETRY_JDK_VENDOR))
        .body("product.internals.jdk.version", equalTo(MockProvider.EXAMPLE_TELEMETRY_JDK_VERSION))
        .body("product.internals.application-server.vendor", equalTo(MockProvider.EXAMPLE_TELEMETRY_APP_SERVER_VENDOR))
        .body("product.internals.application-server.version", equalTo(MockProvider.EXAMPLE_TELEMETRY_APP_SERVER_VERSION))
        .body("product.internals.operaton-integration[0]", equalTo("spring-boot"))
        .body("product.internals.data-collection-start-date", equalTo(MockProvider.EXAMPLE_TELEMETRY_DATA_COLLECTION_START_DATE))
    .when()
      .get(TELEMETRY_DATA_URL);

    verify(managementServiceMock).getTelemetryData();
  }

  @Test
  public void shouldGetMinimalTelemetryData() {

    TelemetryData telemetryData = mock(TelemetryData.class);
    when(telemetryData.getInstallation()).thenReturn(MockProvider.EXAMPLE_TELEMETRY_INSTALLATION_ID);

    Product product = mock(Product.class);
    when(product.getName()).thenReturn(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_NAME);
    when(product.getVersion()).thenReturn(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_VERSION);
    when(product.getEdition()).thenReturn(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_EDITION);
    when(telemetryData.getProduct()).thenReturn(product);

    Internals internals = mock(Internals.class);
    when(product.getInternals()).thenReturn(internals);

    when(internals.getApplicationServer()).thenReturn(mock(ApplicationServer.class));
    when(internals.getOperatonIntegration()).thenReturn(new HashSet<>());
    when(internals.getCommands()).thenReturn(new HashMap<>());
    when(internals.getDatabase()).thenReturn(mock(Database.class));
    when(internals.getJdk()).thenReturn(mock(Jdk.class));
    when(internals.getMetrics()).thenReturn(new HashMap<>());
    when(internals.getWebapps()).thenReturn(new HashSet<>());

    when(managementServiceMock.getTelemetryData()).thenReturn(telemetryData);

    given()
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("installation", equalTo(MockProvider.EXAMPLE_TELEMETRY_INSTALLATION_ID))
        .body("product.name", equalTo(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_NAME))
        .body("product.version", equalTo(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_VERSION))
        .body("product.edition", equalTo(MockProvider.EXAMPLE_TELEMETRY_PRODUCT_EDITION))
    .when()
      .get(TELEMETRY_DATA_URL);

    verify(managementServiceMock).getTelemetryData();
  }

}
