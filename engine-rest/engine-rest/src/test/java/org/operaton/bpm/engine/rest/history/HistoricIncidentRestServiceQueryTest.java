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

import static io.restassured.RestAssured.expect;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_JOB_DEFINITION_ID;
import static org.operaton.bpm.engine.rest.helper.MockProvider.NON_EXISTING_JOB_DEFINITION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 *
 * @author Roman Smirnov
 *
 */
public class HistoricIncidentRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORY_INCIDENT_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/history/incident";
  protected static final String HISTORY_INCIDENT_COUNT_QUERY_URL = HISTORY_INCIDENT_QUERY_URL + "/count";

  private HistoricIncidentQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockHistoricIncidentQuery(MockProvider.createMockHistoricIncidents());
  }

  private HistoricIncidentQuery setUpMockHistoricIncidentQuery(List<HistoricIncident> mockedHistoricIncidents) {
    HistoricIncidentQuery mockedHistoricIncidentQuery = mock(HistoricIncidentQuery.class);

    when(mockedHistoricIncidentQuery.list()).thenReturn(mockedHistoricIncidents);
    when(mockedHistoricIncidentQuery.count()).thenReturn((long) mockedHistoricIncidents.size());

    when(processEngine.getHistoryService().createHistoricIncidentQuery()).thenReturn(mockedHistoricIncidentQuery);

    return mockedHistoricIncidentQuery;
  }

  @Test
  void testEmptyQuery() {
    String queryKey = "";
    given()
      .queryParam("processInstanceId", queryKey)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORY_INCIDENT_QUERY_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }


  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("processInstanceId", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder)
    .then()
      .expect()
        .statusCode(expectedStatus.getStatusCode())
      .when()
        .get(HISTORY_INCIDENT_QUERY_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given()
    .queryParam("sortOrder", "asc")
  .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(HISTORY_INCIDENT_QUERY_URL);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentMessage", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentMessage", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("createTime", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByCreateTime();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("createTime", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByCreateTime();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("endTime", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByCreateTime();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("endTime", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByEndTime();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentType", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentType();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentType", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentType();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("executionId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByExecutionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("executionId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByExecutionId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processInstanceId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processInstanceId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("causeIncidentId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByCauseIncidentId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("causeIncidentId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByCauseIncidentId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("rootCauseIncidentId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByRootCauseIncidentId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("rootCauseIncidentId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByRootCauseIncidentId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("configuration", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByConfiguration();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("configuration", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByConfiguration();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentState", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentState();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentState", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentState();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionKey", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionKey", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockedQuery).desc();
  }

  @Test
  void testSuccessfulPagination() {
    int firstResult = 0;
    int maxResults = 10;

    given()
      .queryParam("firstResult", firstResult)
      .queryParam("maxResults", maxResults)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).listPage(firstResult, maxResults);
  }

  @Test
  void testMissingFirstResultParameter() {
    int maxResults = 10;

    given()
      .queryParam("maxResults", maxResults)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).listPage(0, maxResults);
  }

  @Test
  void testMissingMaxResultsParameter() {
    int firstResult = 10;

    given()
      .queryParam("firstResult", firstResult)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(HISTORY_INCIDENT_COUNT_QUERY_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testSimpleHistoricTaskInstanceQuery() {
    Response response = given()
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORY_INCIDENT_QUERY_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> incidents = from(content).getList("");
    Assertions.assertEquals(1, incidents.size(), "There should be one incident returned.");
    assertThat(incidents.get(0)).as("The returned incident should not be null.").isNotNull();

    String returnedId = from(content).getString("[0].id");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedExecutionId = from(content).getString("[0].executionId");
    Date returnedCreateTime = DateTimeUtil.parseDate(from(content).getString("[0].createTime"));
    Date returnedEndTime = DateTimeUtil.parseDate(from(content).getString("[0].endTime"));
    String returnedIncidentType = from(content).getString("[0].incidentType");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedFailedActivityId = from(content).getString("[0].failedActivityId");
    String returnedCauseIncidentId = from(content).getString("[0].causeIncidentId");
    String returnedRootCauseIncidentId = from(content).getString("[0].rootCauseIncidentId");
    String returnedConfiguration = from(content).getString("[0].configuration");
    String returnedIncidentMessage = from(content).getString("[0].incidentMessage");
    Boolean returnedIncidentOpen = from(content).getBoolean("[0].open");
    Boolean returnedIncidentDeleted = from(content).getBoolean("[0].deleted");
    Boolean returnedIncidentResolved = from(content).getBoolean("[0].resolved");
    String returnedTenantId = from(content).getString("[0].tenantId");
    String returnedJobDefinitionId = from(content).getString("[0].jobDefinitionId");
    Date returnedRemovalTime = DateTimeUtil.parseDate(from(content).getString("[0].removalTime"));
    String returnedRootProcessInstanceId = from(content).getString("[0].rootProcessInstanceId");
    String returnedAnnotation = from(content).getString("[0].annotation");

    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_ID, returnedId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_PROC_INST_ID, returnedProcessInstanceId);
    Assertions.assertEquals(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_INCIDENT_CREATE_TIME), returnedCreateTime);
    Assertions.assertEquals(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_INCIDENT_END_TIME), returnedEndTime);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_EXECUTION_ID, returnedExecutionId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_PROC_DEF_ID, returnedProcessDefinitionId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_PROC_DEF_KEY, returnedProcessDefinitionKey);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_TYPE, returnedIncidentType);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_ACTIVITY_ID, returnedActivityId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_FAILED_ACTIVITY_ID, returnedFailedActivityId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_CAUSE_INCIDENT_ID, returnedCauseIncidentId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_ROOT_CAUSE_INCIDENT_ID, returnedRootCauseIncidentId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_CONFIGURATION, returnedConfiguration);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_MESSAGE, returnedIncidentMessage);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_STATE_OPEN, returnedIncidentOpen);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_STATE_DELETED, returnedIncidentDeleted);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_STATE_RESOLVED, returnedIncidentResolved);
    Assertions.assertEquals(MockProvider.EXAMPLE_TENANT_ID, returnedTenantId);
    Assertions.assertEquals(EXAMPLE_JOB_DEFINITION_ID, returnedJobDefinitionId);
    Assertions.assertEquals(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_INCIDENT_REMOVAL_TIME), returnedRemovalTime);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_INCIDENT_ROOT_PROC_INST_ID, returnedRootProcessInstanceId);
    Assertions.assertEquals(MockProvider.EXAMPLE_USER_OPERATION_ANNOTATION, returnedAnnotation);

  }

  @Test
  void testQueryByIncidentId() {
    String incidentId = MockProvider.EXAMPLE_HIST_INCIDENT_ID;

    given()
      .queryParam("incidentId", incidentId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentId(incidentId);
  }

  @Test
  void testQueryByIncidentType() {
    String incidentType = MockProvider.EXAMPLE_HIST_INCIDENT_TYPE;

    given()
      .queryParam("incidentType", incidentType)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentType(incidentType);
  }

  @Test
  void testQueryByIncidentMessage() {
    String incidentMessage = MockProvider.EXAMPLE_HIST_INCIDENT_MESSAGE;

    given()
      .queryParam("incidentMessage", incidentMessage)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentMessage(incidentMessage);
  }

  @Test
  void testQueryByIncidentMessageLike() {
    String incidentMessage = MockProvider.EXAMPLE_HIST_INCIDENT_MESSAGE;

    given()
            .queryParam("incidentMessageLike", incidentMessage)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentMessageLike(incidentMessage);
  }

  @Test
  void testQueryByProcessDefinitionId() {
    String processDefinitionId = MockProvider.EXAMPLE_HIST_INCIDENT_PROC_DEF_ID;

    given()
      .queryParam("processDefinitionId", processDefinitionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).processDefinitionId(processDefinitionId);
  }

  @Test
  void testQueryByProcessDefinitionKey() {
    String processDefinitionKey = MockProvider.EXAMPLE_HIST_INCIDENT_PROC_DEF_KEY;

    given()
            .queryParam("processDefinitionKey", processDefinitionKey)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).processDefinitionKey(processDefinitionKey);
  }

  @Test
  void testQueryByProcessDefinitionKeyIn() {
    String key1 = "foo";
    String key2 = "bar";

    given()
      .queryParam("processDefinitionKeyIn", key1 + "," + key2)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    InOrder inOrder = inOrder(mockedQuery);

    inOrder.verify(mockedQuery).processDefinitionKeyIn("foo", "bar");
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testQueryByProcessInstanceId() {
    String processInstanceId = MockProvider.EXAMPLE_HIST_INCIDENT_PROC_INST_ID;

    given()
      .queryParam("processInstanceId", processInstanceId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).processInstanceId(processInstanceId);
  }

  @Test
  void testQueryByExecutionId() {
    String executionId = MockProvider.EXAMPLE_HIST_INCIDENT_EXECUTION_ID;

    given()
      .queryParam("executionId", executionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).executionId(executionId);
  }

  @Test
  void testQueryByActivityId() {
    String activityId = MockProvider.EXAMPLE_HIST_INCIDENT_ACTIVITY_ID;

    given()
    .queryParam("activityId", activityId)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).activityId(activityId);
  }

  @Test
  void testQueryByCreateTimeBeforeAndAfter() {
    given()
            .queryParam("createTimeBefore", MockProvider.EXAMPLE_HIST_INCIDENT_CREATE_TIME_BEFORE)
            .queryParam("createTimeAfter", MockProvider.EXAMPLE_HIST_INCIDENT_CREATE_TIME_AFTER)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).createTimeAfter(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_INCIDENT_CREATE_TIME_AFTER));
    verify(mockedQuery).createTimeBefore(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_INCIDENT_CREATE_TIME_BEFORE));
    verify(mockedQuery).list();
  }

  @Test
  void testQueryByEndTimeBeforeAndAfter() {
    given()
            .queryParam("endTimeBefore", MockProvider.EXAMPLE_HIST_INCIDENT_END_TIME_BEFORE)
            .queryParam("endTimeAfter", MockProvider.EXAMPLE_HIST_INCIDENT_END_TIME_AFTER)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).endTimeAfter(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_INCIDENT_END_TIME_AFTER));
    verify(mockedQuery).endTimeBefore(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_INCIDENT_END_TIME_BEFORE));
    verify(mockedQuery).list();
  }

  @Test
  void testQueryByFailedActivityId() {
    String activityId = MockProvider.EXAMPLE_HIST_INCIDENT_FAILED_ACTIVITY_ID;

    given()
      .queryParam("failedActivityId", activityId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).failedActivityId(activityId);
  }

  @Test
  void testQueryByCauseIncidentId() {
    String causeIncidentId = MockProvider.EXAMPLE_HIST_INCIDENT_CAUSE_INCIDENT_ID;

    given()
      .queryParam("causeIncidentId", causeIncidentId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).causeIncidentId(causeIncidentId);
  }

  @Test
  void testQueryByRootCauseIncidentId() {
    String rootCauseIncidentId = MockProvider.EXAMPLE_HIST_INCIDENT_ROOT_CAUSE_INCIDENT_ID;

    given()
      .queryParam("rootCauseIncidentId", rootCauseIncidentId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).rootCauseIncidentId(rootCauseIncidentId);
  }

  @Test
  void testQueryByConfiguration() {
    String configuration = MockProvider.EXAMPLE_HIST_INCIDENT_CONFIGURATION;

    given()
    .queryParam("configuration", configuration)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).configuration(configuration);
  }

  @Test
  void testQueryByHistoryConfiguration() {
    String historyConfiguration = MockProvider.EXAMPLE_HIST_INCIDENT_HISTORY_CONFIGURATION;

    given()
      .queryParam("historyConfiguration", historyConfiguration)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).historyConfiguration(historyConfiguration);
  }

  @Test
  void testQueryByOpen() {
    given()
      .queryParam("open", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).open();
  }

  @Test
  void testQueryByResolved() {
    given()
      .queryParam("resolved", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).resolved();
  }

  @Test
  void testQueryByDeleted() {
    given()
      .queryParam("deleted", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).deleted();
  }

  @Test
  void testQueryByTenantIds() {
    mockedQuery = setUpMockHistoricIncidentQuery(Arrays.asList(
        MockProvider.createMockHistoricIncident(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockHistoricIncident(MockProvider.ANOTHER_EXAMPLE_TENANT_ID)));

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> incidents = from(content).getList("");
    assertThat(incidents).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testQueryWithoutTenantIdQueryParameter() {
    // given
    mockedQuery = setUpMockHistoricIncidentQuery(Collections.singletonList(MockProvider.createMockHistoricIncident(null)));

    // when
    Response response = given()
        .queryParam("withoutTenantId", true)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(HISTORY_INCIDENT_QUERY_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId).isNull();
  }

  @Test
  void testQueryByJobDefinitionIds() {
    String jobDefinitionIds = EXAMPLE_JOB_DEFINITION_ID + "," + NON_EXISTING_JOB_DEFINITION_ID;

    given()
        .queryParam("jobDefinitionIdIn", jobDefinitionIds)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORY_INCIDENT_QUERY_URL);

    verify(mockedQuery).jobDefinitionIdIn(EXAMPLE_JOB_DEFINITION_ID, NON_EXISTING_JOB_DEFINITION_ID);
    verify(mockedQuery).list();
  }

}
