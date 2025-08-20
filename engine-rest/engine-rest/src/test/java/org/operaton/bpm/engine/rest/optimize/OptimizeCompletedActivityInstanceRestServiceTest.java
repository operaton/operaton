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

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.helper.MockProvider;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.rest.util.DateTimeUtils.DATE_FORMAT_WITH_TIMEZONE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class OptimizeCompletedActivityInstanceRestServiceTest extends AbstractRestServiceTest {

  public static final String OPTIMIZE_COMPLETED_ACTIVITY_INSTANCE_PATH =
    TEST_RESOURCE_ROOT_PATH + "/optimize/activity-instance/completed";

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
      .get(OPTIMIZE_COMPLETED_ACTIVITY_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricActivityInstances(null, null, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testFinishedAfterQueryParameter() {
    Date now = new Date();
    given()
      .queryParam("finishedAfter", DATE_FORMAT_WITH_TIMEZONE.format(now))
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_ACTIVITY_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricActivityInstances(now, null, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testFinishedAtQueryParameter() {
    Date now = new Date();
    given()
      .queryParam("finishedAt", DATE_FORMAT_WITH_TIMEZONE.format(now))
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_ACTIVITY_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricActivityInstances(null, now, Integer.MAX_VALUE);
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
      .get(OPTIMIZE_COMPLETED_ACTIVITY_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricActivityInstances(null, null, 10);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testQueryParameterCombination() {
    Date now = new Date();
    given()
      .queryParam("finishedAfter", DATE_FORMAT_WITH_TIMEZONE.format(now))
      .queryParam("finishedAt", DATE_FORMAT_WITH_TIMEZONE.format(now))
      .queryParam("maxResults", 10)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_ACTIVITY_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricActivityInstances(now, now, 10);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  void testPresenceOfSequenceCounterProperty() {
    final HistoricActivityInstanceEntity mock = mock(HistoricActivityInstanceEntity.class);
    when(mock.getSequenceCounter()).thenReturn(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_SEQUENCE_COUNTER);
    when(mockedOptimizeService.getCompletedHistoricActivityInstances(null, null, Integer.MAX_VALUE))
      .thenReturn(Collections.singletonList(mock));

    final Response response = given()
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(MediaType.APPLICATION_JSON)
      .when()
        .get(OPTIMIZE_COMPLETED_ACTIVITY_INSTANCE_PATH);

    String content = response.asString();
    long sequenceCounter = from(content).getLong("[0].sequenceCounter");

    assertThat(sequenceCounter).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_SEQUENCE_COUNTER);
  }

}
