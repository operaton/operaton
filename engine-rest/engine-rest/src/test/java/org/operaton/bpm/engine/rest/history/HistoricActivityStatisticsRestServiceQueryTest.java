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
package org.operaton.bpm.engine.rest.history;

import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.engine.history.HistoricActivityStatistics;
import org.operaton.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.operaton.bpm.engine.impl.HistoricActivityStatisticsQueryImpl;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static org.operaton.bpm.engine.rest.util.DateTimeUtils.DATE_FORMAT_WITH_TIMEZONE;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * @author Roman Smirnov
 *
 */
public class HistoricActivityStatisticsRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORY_URL = TEST_RESOURCE_ROOT_PATH + "/history";
  protected static final String HISTORIC_ACTIVITY_STATISTICS_URL = HISTORY_URL + "/process-definition/{id}/statistics";

  private HistoricActivityStatisticsQuery historicActivityStatisticsQuery;

  @BeforeEach
  void setUpRuntimeData() {
    setupHistoricActivityStatisticsMock();
  }

  private void setupHistoricActivityStatisticsMock() {
    List<HistoricActivityStatistics> mocks = MockProvider.createMockHistoricActivityStatistics();

    historicActivityStatisticsQuery = mock(HistoricActivityStatisticsQueryImpl.class);
    when(processEngine.getHistoryService().createHistoricActivityStatisticsQuery(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(historicActivityStatisticsQuery);
    when(historicActivityStatisticsQuery.unlimitedList()).thenReturn(mocks);
  }

  @Test
  void testHistoricActivityStatisticsRetrieval() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("$.size()", is(2))
      .body("id", hasItems(MockProvider.EXAMPLE_ACTIVITY_ID, MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID))
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);
  }

  @Test
  void testAdditionalCanceledOption() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("canceled", "true")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).includeCanceled();
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testAdditionalFinishedOption() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("finished", "true")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).includeFinished();
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testAdditionalCompleteScopeOption() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    . queryParam("completeScope", "true")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).includeCompleteScope();
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testAdditionalStartedAfterOption() {
    final Date testDate = new Date(0);
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("startedAfter", DATE_FORMAT_WITH_TIMEZONE.format(testDate))
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).startedAfter(testDate);
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testAdditionalStartedBeforeOption() {
    final Date testDate = new Date(0);
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("startedBefore", DATE_FORMAT_WITH_TIMEZONE.format(testDate))
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).startedBefore(testDate);
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testAdditionalFinishedAfterOption() {
    final Date testDate = new Date(0);
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("finishedAfter", DATE_FORMAT_WITH_TIMEZONE.format(testDate))
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).finishedAfter(testDate);
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testAdditionalFinishedBeforeOption() {
    final Date testDate = new Date(0);
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("finishedBefore", DATE_FORMAT_WITH_TIMEZONE.format(testDate))
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).finishedBefore(testDate);
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testAdditionalCompleteScopeAndCanceledOption() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("completeScope", "true")
      .queryParam("canceled", "true")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    verify(historicActivityStatisticsQuery).includeCompleteScope();
    verify(historicActivityStatisticsQuery).includeCanceled();
    verify(historicActivityStatisticsQuery).unlimitedList();
    verifyNoMoreInteractions(historicActivityStatisticsQuery);
  }

  @Test
  void testAdditionalCompleteScopeAndFinishedOption() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("completeScope", "true")
      .queryParam("finished", "true")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    verify(historicActivityStatisticsQuery).includeCompleteScope();
    verify(historicActivityStatisticsQuery).includeFinished();
    verify(historicActivityStatisticsQuery).unlimitedList();
    verifyNoMoreInteractions(historicActivityStatisticsQuery);
  }

  @Test
  void testAdditionalCanceledAndFinishedOption() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("canceled", "true")
      .queryParam("finished", "true")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    verify(historicActivityStatisticsQuery).includeCanceled();
    verify(historicActivityStatisticsQuery).includeFinished();
    verify(historicActivityStatisticsQuery).unlimitedList();
    verifyNoMoreInteractions(historicActivityStatisticsQuery);
  }

  @Test
  void testAdditionalCompleteScopeAndFinishedAndCanceledOption() {
    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("completeScope", "true")
      .queryParam("finished", "true")
      .queryParam("canceled", "true")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    verify(historicActivityStatisticsQuery).includeCompleteScope();
    verify(historicActivityStatisticsQuery).includeFinished();
    verify(historicActivityStatisticsQuery).includeCanceled();
    verify(historicActivityStatisticsQuery).unlimitedList();
    verifyNoMoreInteractions(historicActivityStatisticsQuery);
  }

  @Test
  void testAdditionalCompleteScopeAndFinishedAndCanceledOptionFalse() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("completeScope", "false")
      .queryParam("finished", "false")
      .queryParam("canceled", "false")
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    verify(historicActivityStatisticsQuery).unlimitedList();
    verifyNoMoreInteractions(historicActivityStatisticsQuery);
  }


  @Test
  void testProcessInstanceIdInFilter() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("processInstanceIdIn", "foo,bar")
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).processInstanceIdIn(new String[] {"foo", "bar"});
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testIncidentsFilter() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("incidents", "true")
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).includeIncidents();
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testSimpleTaskQuery() {
    Response response = given()
          .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
         .then().expect()
           .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);

    String content = response.asString();
    List<Map<String, Object>> result = from(content).getList("");
    assertThat(result.size()).isEqualTo(2);

    assertThat(result.get(0)).isNotNull();
    assertThat(result.get(1)).isNotNull();

    String id = from(content).getString("[0].id");
    long instances = from(content).getLong("[0].instances");
    long canceled = from(content).getLong("[0].canceled");
    long finished = from(content).getLong("[0].finished");
    long completeScope = from(content).getLong("[0].completeScope");
    long openIncidents = from(content).getLong("[0].openIncidents");
    long resolvedIncidents = from(content).getLong("[0].resolvedIncidents");
    long deletedIncidents = from(content).getLong("[0].deletedIncidents");

    assertThat(id).isEqualTo(MockProvider.EXAMPLE_ACTIVITY_ID);
    assertThat(instances).isEqualTo(MockProvider.EXAMPLE_INSTANCES_LONG);
    assertThat(canceled).isEqualTo(MockProvider.EXAMPLE_CANCELED_LONG);
    assertThat(finished).isEqualTo(MockProvider.EXAMPLE_FINISHED_LONG);
    assertThat(completeScope).isEqualTo(MockProvider.EXAMPLE_COMPLETE_SCOPE_LONG);
    assertThat(openIncidents).isEqualTo(MockProvider.EXAMPLE_OPEN_INCIDENTS_LONG);
    assertThat(resolvedIncidents).isEqualTo(MockProvider.EXAMPLE_RESOLVED_INCIDENTS_LONG);
    assertThat(deletedIncidents).isEqualTo(MockProvider.EXAMPLE_DELETED_INCIDENTS_LONG);

    id = from(content).getString("[1].id");
    instances = from(content).getLong("[1].instances");
    canceled = from(content).getLong("[1].canceled");
    finished = from(content).getLong("[1].finished");
    completeScope = from(content).getLong("[1].completeScope");
    openIncidents = from(content).getLong("[1].openIncidents");
    resolvedIncidents = from(content).getLong("[1].resolvedIncidents");
    deletedIncidents = from(content).getLong("[1].deletedIncidents");

    assertThat(id).isEqualTo(MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID);
    assertThat(instances).isEqualTo(MockProvider.ANOTHER_EXAMPLE_INSTANCES_LONG);
    assertThat(canceled).isEqualTo(MockProvider.ANOTHER_EXAMPLE_CANCELED_LONG);
    assertThat(finished).isEqualTo(MockProvider.ANOTHER_EXAMPLE_FINISHED_LONG);
    assertThat(completeScope).isEqualTo(MockProvider.ANOTHER_EXAMPLE_COMPLETE_SCOPE_LONG);
    assertThat(openIncidents).isEqualTo(MockProvider.ANOTHER_EXAMPLE_OPEN_INCIDENTS_LONG);
    assertThat(resolvedIncidents).isEqualTo(MockProvider.ANOTHER_EXAMPLE_RESOLVED_INCIDENTS_LONG);
    assertThat(deletedIncidents).isEqualTo(MockProvider.ANOTHER_EXAMPLE_DELETED_INCIDENTS_LONG);

  }

  @Test
  void testSortByParameterOnly() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("sortBy", "activityId")
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("sortOrder", "asc")
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);
  }

  @Test
  void testInvalidSortOrder() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("sortOrder", "invalid")
      .queryParam("sortBy", "activityId")
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Cannot set query parameter 'sortOrder' to value 'invalid'"))
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);
  }

  @Test
  void testInvalidSortByParameterOnly() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("sortOrder", "asc")
      .queryParam("sortBy", "invalid")
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Cannot set query parameter 'sortBy' to value 'invalid'"))
      .when().get(HISTORIC_ACTIVITY_STATISTICS_URL);
  }

  @Test
  void testValidSortingParameters() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("sortOrder", "asc")
      .queryParam("sortBy", "activityId")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_ACTIVITY_STATISTICS_URL);

    InOrder inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).orderByActivityId();
    inOrder.verify(historicActivityStatisticsQuery).asc();
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("sortOrder", "desc")
      .queryParam("sortBy", "activityId")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_ACTIVITY_STATISTICS_URL);

    inOrder = Mockito.inOrder(historicActivityStatisticsQuery);
    inOrder.verify(historicActivityStatisticsQuery).orderByActivityId();
    inOrder.verify(historicActivityStatisticsQuery).desc();
    inOrder.verify(historicActivityStatisticsQuery).unlimitedList();
    inOrder.verifyNoMoreInteractions();
  }

}
