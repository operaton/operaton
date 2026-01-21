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

import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;

import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_JOB_DEFINITION_ID;
import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_TENANT_ID_LIST;
import static org.operaton.bpm.engine.rest.helper.MockProvider.NON_EXISTING_JOB_DEFINITION_ID;
import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 *
 * @author Roman Smirnov
 *
 */
public class IncidentRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String INCIDENT_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/incident";
  protected static final String INCIDENT_COUNT_QUERY_URL = INCIDENT_QUERY_URL + "/count";

  private IncidentQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    List<Incident> incidents = MockProvider.createMockIncidents();

    mockedQuery = setupMockIncidentQuery(incidents);
  }

  private IncidentQuery setupMockIncidentQuery(List<Incident> incidents) {
    IncidentQuery sampleQuery = mock(IncidentQuery.class);

    when(sampleQuery.list()).thenReturn(incidents);
    when(sampleQuery.count()).thenReturn((long) incidents.size());

    when(processEngine.getRuntimeService().createIncidentQuery()).thenReturn(sampleQuery);

    return sampleQuery;
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
        .get(INCIDENT_QUERY_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(INCIDENT_QUERY_URL);

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
        .get(INCIDENT_QUERY_URL);
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
      .get(INCIDENT_QUERY_URL);
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
    inOrder.verify(mockedQuery).orderByIncidentMessage();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentMessage", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentMessage();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentTimestamp", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentTimestamp();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("incidentTimestamp", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentTimestamp();
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
    inOrder.verify(mockedQuery).orderByIncidentId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByIncidentId();
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
        .get(INCIDENT_QUERY_URL);

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
        .get(INCIDENT_QUERY_URL);

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
        .get(INCIDENT_QUERY_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(INCIDENT_COUNT_QUERY_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testSimpleIncidentQuery() {
    Response response = given()
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(INCIDENT_QUERY_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> incidents = from(content).getList("");
    assertThat(incidents).as("There should be one incident returned.").hasSize(1);
    assertThat(incidents.get(0)).as("The returned incident should not be null.").isNotNull();

    String returnedId = from(content).getString("[0].id");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedExecutionId = from(content).getString("[0].executionId");
    Date returnedIncidentTimestamp = DateTimeUtil.parseDate(from(content).getString("[0].incidentTimestamp"));
    String returnedIncidentType = from(content).getString("[0].incidentType");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedFailedActivityId = from(content).getString("[0].failedActivityId");
    String returnedCauseIncidentId = from(content).getString("[0].causeIncidentId");
    String returnedRootCauseIncidentId = from(content).getString("[0].rootCauseIncidentId");
    String returnedConfiguration = from(content).getString("[0].configuration");
    String returnedIncidentMessage = from(content).getString("[0].incidentMessage");
    String returnedTenantId = from(content).getString("[0].tenantId");
    String returnedJobDefinitionId = from(content).getString("[0].jobDefinitionId");
    String returnedAnnotation = from(content).getString("[0].annotation");

    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_PROC_INST_ID);
    assertThat(returnedIncidentTimestamp).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_INCIDENT_TIMESTAMP));
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_EXECUTION_ID);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_PROC_DEF_ID);
    assertThat(returnedIncidentType).isEqualTo(MockProvider.EXAMPLE_INCIDENT_TYPE);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_ACTIVITY_ID);
    assertThat(returnedFailedActivityId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_FAILED_ACTIVITY_ID);
    assertThat(returnedCauseIncidentId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_CAUSE_INCIDENT_ID);
    assertThat(returnedRootCauseIncidentId).isEqualTo(MockProvider.EXAMPLE_INCIDENT_ROOT_CAUSE_INCIDENT_ID);
    assertThat(returnedConfiguration).isEqualTo(MockProvider.EXAMPLE_INCIDENT_CONFIGURATION);
    assertThat(returnedIncidentMessage).isEqualTo(MockProvider.EXAMPLE_INCIDENT_MESSAGE);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedJobDefinitionId).isEqualTo(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    assertThat(returnedAnnotation).isEqualTo(MockProvider.EXAMPLE_USER_OPERATION_ANNOTATION);
  }

  @Test
  void testQueryByIncidentId() {
    String incidentId = MockProvider.EXAMPLE_INCIDENT_ID;

    given()
      .queryParam("incidentId", incidentId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentId(incidentId);
  }

  @Test
  void testQueryByIncidentType() {
    String incidentType = MockProvider.EXAMPLE_INCIDENT_TYPE;

    given()
      .queryParam("incidentType", incidentType)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentType(incidentType);
  }

  @Test
  void testQueryByIncidentMessage() {
    String incidentMessage = MockProvider.EXAMPLE_INCIDENT_MESSAGE;

    given()
      .queryParam("incidentMessage", incidentMessage)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentMessage(incidentMessage);
  }

  @Test
  void testQueryByIncidentMessageLike() {
    String incidentMessage = MockProvider.EXAMPLE_INCIDENT_MESSAGE;

    given()
            .queryParam("incidentMessageLike", incidentMessage)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentMessageLike(incidentMessage);
  }

  @Test
  void testQueryByIncidentTimestampBeforeAndAfter() {
    given()
            .queryParam("incidentTimestampBefore", MockProvider.EXAMPLE_INCIDENT_TIMESTAMP_BEFORE)
            .queryParam("incidentTimestampAfter", MockProvider.EXAMPLE_INCIDENT_TIMESTAMP_AFTER)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).incidentTimestampBefore(DateTimeUtil.parseDate(MockProvider.EXAMPLE_INCIDENT_TIMESTAMP_BEFORE));
    verify(mockedQuery).incidentTimestampAfter(DateTimeUtil.parseDate(MockProvider.EXAMPLE_INCIDENT_TIMESTAMP_AFTER));
    verify(mockedQuery).list();
  }

  @Test
  void testQueryByProcessDefinitionId() {
    String processDefinitionId = MockProvider.EXAMPLE_INCIDENT_PROC_DEF_ID;

    given()
      .queryParam("processDefinitionId", processDefinitionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).processDefinitionId(processDefinitionId);
  }

  @Test
  void testQueryByProcessDefinitionKey() {
    String key1 = "foo";
    String key2 = "bar";

    given()
      .queryParam("processDefinitionKeyIn", key1 + "," + key2)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    InOrder inOrder = inOrder(mockedQuery);

    inOrder.verify(mockedQuery).processDefinitionKeyIn("foo", "bar");
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testQueryByProcessInstanceId() {
    String processInstanceId = MockProvider.EXAMPLE_INCIDENT_PROC_INST_ID;

    given()
      .queryParam("processInstanceId", processInstanceId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).processInstanceId(processInstanceId);
  }

  @Test
  void testQueryByExecutionId() {
    String executionId = MockProvider.EXAMPLE_INCIDENT_EXECUTION_ID;

    given()
      .queryParam("executionId", executionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).executionId(executionId);
  }

  @Test
  void testQueryByActivityId() {
    String activityId = MockProvider.EXAMPLE_INCIDENT_ACTIVITY_ID;

    given()
    .queryParam("activityId", activityId)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).activityId(activityId);
  }

  @Test
  void testQueryByFailedActivityId() {
    String activityId = MockProvider.EXAMPLE_INCIDENT_FAILED_ACTIVITY_ID;

    given()
      .queryParam("failedActivityId", activityId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).failedActivityId(activityId);
  }

  @Test
  void testQueryByCauseIncidentId() {
    String causeIncidentId = MockProvider.EXAMPLE_INCIDENT_CAUSE_INCIDENT_ID;

    given()
      .queryParam("causeIncidentId", causeIncidentId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).causeIncidentId(causeIncidentId);
  }

  @Test
  void testQueryByRootCauseIncidentId() {
    String rootCauseIncidentId = MockProvider.EXAMPLE_INCIDENT_ROOT_CAUSE_INCIDENT_ID;

    given()
      .queryParam("rootCauseIncidentId", rootCauseIncidentId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).rootCauseIncidentId(rootCauseIncidentId);
  }

  @Test
  void testQueryByConfiguration() {
    String configuration = MockProvider.EXAMPLE_INCIDENT_CONFIGURATION;

    given()
      .queryParam("configuration", configuration)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(INCIDENT_QUERY_URL);

    verify(mockedQuery).configuration(configuration);
  }

  @Test
  void testQueryByTenantIds() {
    mockedQuery = setupMockIncidentQuery(List.of(
        MockProvider.createMockIncident(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockIncident(MockProvider.ANOTHER_EXAMPLE_TENANT_ID)));

    Response response = given()
      .queryParam("tenantIdIn", EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(INCIDENT_QUERY_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testQueryByJobDefinitionIds() {
    String jobDefinitionIds = EXAMPLE_JOB_DEFINITION_ID + "," + NON_EXISTING_JOB_DEFINITION_ID;

    given()
        .queryParam("jobDefinitionIdIn", jobDefinitionIds)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(INCIDENT_QUERY_URL);

    verify(mockedQuery).jobDefinitionIdIn(EXAMPLE_JOB_DEFINITION_ID, NON_EXISTING_JOB_DEFINITION_ID);
    verify(mockedQuery).list();
  }

}
