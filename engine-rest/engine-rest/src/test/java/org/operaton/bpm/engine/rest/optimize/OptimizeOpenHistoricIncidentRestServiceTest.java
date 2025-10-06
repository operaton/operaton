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
package org.operaton.bpm.engine.rest.optimize;

import java.util.Collections;
import java.util.Date;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static org.operaton.bpm.engine.rest.util.DateTimeUtils.DATE_FORMAT_WITH_TIMEZONE;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(TestContainerExtension.class)
class OptimizeOpenHistoricIncidentRestServiceTest extends AbstractRestServiceTest {

  private static final String OPTIMIZE_OPEN_HISTORIC_INCIDENT_PATH =
    TEST_RESOURCE_ROOT_PATH + "/optimize/incident/open";

  protected OptimizeService mockedOptimizeService;
  protected ProcessEngine namedProcessEngine;

  @BeforeEach
  void setUpRuntimeData() {
    mockedOptimizeService = mock(OptimizeService.class);
    ProcessEngineConfigurationImpl mockedConfig = mock(ProcessEngineConfigurationImpl.class);

    namedProcessEngine = getProcessEngine(MockProvider.EXAMPLE_PROCESS_ENGINE_NAME);
    when(namedProcessEngine.getProcessEngineConfiguration()).thenReturn(mockedConfig);
    when(mockedConfig.getOptimizeService()).thenReturn(mockedOptimizeService);
  }

  @Test
  void testNoQueryParameters() {
    given()
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_OPEN_HISTORIC_INCIDENT_PATH);

    verify(mockedOptimizeService).getOpenHistoricIncidents(null, null, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testCreatedAfterQueryParameter() {
    Date now = new Date();
    given()
      .queryParam("createdAfter", DATE_FORMAT_WITH_TIMEZONE.format(now))
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_OPEN_HISTORIC_INCIDENT_PATH);

    verify(mockedOptimizeService).getOpenHistoricIncidents(now, null, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testCreatedAtQueryParameter() {
    Date now = new Date();
    given()
      .queryParam("createdAt", DATE_FORMAT_WITH_TIMEZONE.format(now))
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_OPEN_HISTORIC_INCIDENT_PATH);

    verify(mockedOptimizeService).getOpenHistoricIncidents(null, now, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testMaxResultsQueryParameter() {
    given()
      .queryParam("maxResults", 10)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_OPEN_HISTORIC_INCIDENT_PATH);

    verify(mockedOptimizeService).getOpenHistoricIncidents(null, null, 10);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testQueryParameterCombination() {
    Date now = new Date();
    given()
      .queryParam("createdAfter", DATE_FORMAT_WITH_TIMEZONE.format(now))
      .queryParam("createdAt", DATE_FORMAT_WITH_TIMEZONE.format(now))
      .queryParam("maxResults", 10)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_OPEN_HISTORIC_INCIDENT_PATH);

    verify(mockedOptimizeService).getOpenHistoricIncidents(now, now, 10);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testPresenceOfProcessInstanceIdProperty() {
    final HistoricIncidentEntity mock = mock(HistoricIncidentEntity.class);
    when(mock.getProcessInstanceId()).thenReturn(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    when(mockedOptimizeService.getOpenHistoricIncidents(null, null, Integer.MAX_VALUE))
      .thenReturn(Collections.singletonList(mock));

    final Response response = given()
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(MediaType.APPLICATION_JSON)
      .when()
        .get(OPTIMIZE_OPEN_HISTORIC_INCIDENT_PATH);

    String content = response.asString();
    String processInstanceId = from(content).getString("[0].processInstanceId");

    assertThat(processInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
  }

}
