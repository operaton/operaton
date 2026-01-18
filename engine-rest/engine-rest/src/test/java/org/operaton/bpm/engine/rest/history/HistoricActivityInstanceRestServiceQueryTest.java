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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.OrderingBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

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

public class HistoricActivityInstanceRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/activity-instance";

  protected static final String HISTORIC_ACTIVITY_INSTANCE_COUNT_RESOURCE_URL = HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL + "/count";

  protected HistoricActivityInstanceQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockHistoricActivityInstanceQuery(MockProvider.createMockHistoricActivityInstances());
  }

  private HistoricActivityInstanceQuery setUpMockHistoricActivityInstanceQuery(List<HistoricActivityInstance> mockedHistoricActivityInstances) {
    HistoricActivityInstanceQuery mockedhistoricActivityInstanceQuery = mock(HistoricActivityInstanceQuery.class);
    when(mockedhistoricActivityInstanceQuery.list()).thenReturn(mockedHistoricActivityInstances);
    when(mockedhistoricActivityInstanceQuery.count()).thenReturn((long) mockedHistoricActivityInstances.size());

    when(processEngine.getHistoryService().createHistoricActivityInstanceQuery()).thenReturn(mockedhistoricActivityInstanceQuery);

    return mockedhistoricActivityInstanceQuery;
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
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testNoParametersQueryAsPost() {
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("instanceId", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder)
    .then()
      .expect()
        .statusCode(expectedStatus.getStatusCode())
      .when()
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortByParameterOnly() {
    given()
      .queryParam("sortBy", "instanceId")
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when()
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);
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
      .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityInstanceId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityInstanceId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("instanceId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("instanceId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessInstanceId();
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
    executeAndVerifySorting("activityName", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityName();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityName", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityName();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityType", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityType();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityType", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityType();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("startTime", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceStartTime();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("startTime", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceStartTime();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("endTime", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceEndTime();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("endTime", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceEndTime();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("duration", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceDuration();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("duration", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceDuration();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("definitionId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("definitionId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("occurrence", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderPartiallyByOccurrence();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("occurrence", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderPartiallyByOccurrence();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).asc();
  }

  @Test
  void testSecondarySortingAsPost() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    Map<String, Object> json = new HashMap<>();
    json.put("sorting", OrderingBuilder.create()
      .orderBy("definitionId").desc()
      .orderBy("instanceId").asc()
      .getJson());
    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).desc();
    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).asc();
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
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

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
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

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
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(HISTORIC_ACTIVITY_INSTANCE_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testQueryCountForPost() {
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .body("count", equalTo(1))
      .when()
        .post(HISTORIC_ACTIVITY_INSTANCE_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testSimpleHistoricActivityQuery() {
    String processInstanceId = MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    Response response = given()
        .queryParam("processInstanceId", processInstanceId)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).processInstanceId(processInstanceId);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).as("There should be one activity instance returned.").hasSize(1);
    assertThat(instances.get(0)).as("The returned activity instance should not be null.").isNotNull();

    String returnedId = from(content).getString("[0].id");
    String returnedParentActivityInstanceId = from(content).getString("[0].parentActivityInstanceId");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedActivityName = from(content).getString("[0].activityName");
    String returnedActivityType = from(content).getString("[0].activityType");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedExecutionId = from(content).getString("[0].executionId");
    String returnedTaskId = from(content).getString("[0].taskId");
    String returnedCalledProcessInstanceId = from(content).getString("[0].calledProcessInstanceId");
    String returnedCalledCaseInstanceId = from(content).getString("[0].calledCaseInstanceId");
    String returnedAssignee = from(content).getString("[0].assignee");
    Date returnedStartTime = DateTimeUtil.parseDate(from(content).getString("[0].startTime"));
    Date returnedEndTime = DateTimeUtil.parseDate(from(content).getString("[0].endTime"));
    long returnedDurationInMillis = from(content).getLong("[0].durationInMillis");
    boolean canceled = from(content).getBoolean("[0].canceled");
    boolean completeScope = from(content).getBoolean("[0].completeScope");
    String returnedTenantId = from(content).getString("[0].tenantId");

    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_ID);
    assertThat(returnedParentActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_PARENT_ACTIVITY_INSTANCE_ID);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_ACTIVITY_ID);
    assertThat(returnedActivityName).isEqualTo(MockProvider.EXAMPLE_ACTIVITY_NAME);
    assertThat(returnedActivityType).isEqualTo(MockProvider.EXAMPLE_ACTIVITY_TYPE);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_EXECUTION_ID);
    assertThat(returnedTaskId).isEqualTo(MockProvider.EXAMPLE_TASK_ID);
    assertThat(returnedCalledProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_CALLED_PROCESS_INSTANCE_ID);
    assertThat(returnedCalledCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_CALLED_CASE_INSTANCE_ID);
    assertThat(returnedAssignee).isEqualTo(MockProvider.EXAMPLE_TASK_ASSIGNEE_NAME);
    assertThat(returnedStartTime).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_START_TIME));
    assertThat(returnedEndTime).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_END_TIME));
    assertThat(returnedDurationInMillis).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_DURATION);
    assertThat(canceled).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_IS_CANCELED);
    assertThat(completeScope).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_IS_COMPLETE_SCOPE);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
  }

  @Test
  void testAdditionalParameters() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    given()
      .queryParams(stringQueryParameters)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyStringParameterQueryInvocations();
  }

  @Test
  void testAdditionalParametersAsPost() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(stringQueryParameters)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyStringParameterQueryInvocations();
  }

  @Test
  void testBooleanParameters() {
    Map<String, Boolean> params = getCompleteBooleanQueryParameters();

    given()
      .queryParams(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyBooleanParameterQueryInvocations();
  }

  @Test
  void testBooleanParametersAsPost() {
    Map<String, Boolean> params = getCompleteBooleanQueryParameters();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyBooleanParameterQueryInvocations();
  }

  private Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("activityInstanceId", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_ID);
    parameters.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    parameters.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    parameters.put("executionId", MockProvider.EXAMPLE_EXECUTION_ID);
    parameters.put("activityId", MockProvider.EXAMPLE_ACTIVITY_ID);
    parameters.put("activityName", MockProvider.EXAMPLE_ACTIVITY_NAME);
    parameters.put("activityNameLike", MockProvider.EXAMPLE_ACTIVITY_NAME);
    parameters.put("activityType", MockProvider.EXAMPLE_ACTIVITY_TYPE);
    parameters.put("taskAssignee", MockProvider.EXAMPLE_TASK_ASSIGNEE_NAME);

    return parameters;
  }

  private Map<String, Boolean> getCompleteBooleanQueryParameters() {
    Map<String, Boolean> parameters = new HashMap<>();

    parameters.put("canceled", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_IS_CANCELED);
    parameters.put("completeScope", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_IS_COMPLETE_SCOPE);

    return parameters;
  }

  private void verifyStringParameterQueryInvocations() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    verify(mockedQuery).activityInstanceId(stringQueryParameters.get("activityInstanceId"));
    verify(mockedQuery).processInstanceId(stringQueryParameters.get("processInstanceId"));
    verify(mockedQuery).processDefinitionId(stringQueryParameters.get("processDefinitionId"));
    verify(mockedQuery).executionId(stringQueryParameters.get("executionId"));
    verify(mockedQuery).activityId(stringQueryParameters.get("activityId"));
    verify(mockedQuery).activityName(stringQueryParameters.get("activityName"));
    verify(mockedQuery).activityNameLike(stringQueryParameters.get("activityNameLike"));
    verify(mockedQuery).activityType(stringQueryParameters.get("activityType"));
    verify(mockedQuery).taskAssignee(stringQueryParameters.get("taskAssignee"));

    verify(mockedQuery).list();
  }

  private void verifyBooleanParameterQueryInvocations() {
    Map<String, Boolean> booleanParams = getCompleteBooleanQueryParameters();
    Boolean canceled = booleanParams.get("canceled");
    Boolean completeScope = booleanParams.get("completeScope");

    if (canceled != null && canceled) {
      verify(mockedQuery).canceled();
    }
    if (completeScope != null && completeScope) {
      verify(mockedQuery).completeScope();
    }

    verify(mockedQuery).list();
  }

  @Test
  void testFinishedHistoricActivityQuery() {
    Response response = given()
        .queryParam("finished", true)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).finished();
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances.get(0)).as("The returned activity instance should not be null.").isNotNull();

    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedActivityEndTime = from(content).getString("[0].endTime");

    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedActivityEndTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_END_TIME);
  }

  @Test
  void testFinishedHistoricActivityQueryAsPost() {
    Map<String, Boolean> body = new HashMap<>();
    body.put("finished", true);

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(body)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).finished();
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).as("There should be one activity instance returned.").hasSize(1);
    assertThat(instances.get(0)).as("The returned activity instance should not be null.").isNotNull();

    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedActivityEndTime = from(content).getString("[0].endTime");

    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedActivityEndTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_END_TIME);
  }

  @Test
  void testUnfinishedHistoricActivityQuery() {
    List<HistoricActivityInstance> mockedHistoricActivityInstances = MockProvider.createMockRunningHistoricActivityInstances();
    HistoricActivityInstanceQuery mockedhistoricActivityInstanceQuery = mock(HistoricActivityInstanceQuery.class);
    when(mockedhistoricActivityInstanceQuery.list()).thenReturn(mockedHistoricActivityInstances);
    when(processEngine.getHistoryService().createHistoricActivityInstanceQuery()).thenReturn(mockedhistoricActivityInstanceQuery);

    Response response = given()
        .queryParam("unfinished", true)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedhistoricActivityInstanceQuery);
    inOrder.verify(mockedhistoricActivityInstanceQuery).unfinished();
    inOrder.verify(mockedhistoricActivityInstanceQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).as("There should be one activity instance returned.").hasSize(1);
    assertThat(instances.get(0)).as("The returned activity instance should not be null.").isNotNull();

    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedActivityEndTime = from(content).getString("[0].endTime");

    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedActivityEndTime).isNull();
  }

  @Test
  void testUnfinishedHistoricActivityQueryAsPost() {
    List<HistoricActivityInstance> mockedHistoricActivityInstances = MockProvider.createMockRunningHistoricActivityInstances();
    HistoricActivityInstanceQuery mockedhistoricActivityInstanceQuery = mock(HistoricActivityInstanceQuery.class);
    when(mockedhistoricActivityInstanceQuery.list()).thenReturn(mockedHistoricActivityInstances);
    when(processEngine.getHistoryService().createHistoricActivityInstanceQuery()).thenReturn(mockedhistoricActivityInstanceQuery);

    Map<String, Boolean> body = new HashMap<>();
    body.put("unfinished", true);

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(body)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedhistoricActivityInstanceQuery);
    inOrder.verify(mockedhistoricActivityInstanceQuery).unfinished();
    inOrder.verify(mockedhistoricActivityInstanceQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).as("There should be one activity instance returned.").hasSize(1);
    assertThat(instances.get(0)).as("The returned activity instance should not be null.").isNotNull();

    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedActivityEndTime = from(content).getString("[0].endTime");

    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedActivityEndTime).isNull();
  }

  @Test
  void testHistoricBeforeAndAfterStartTimeQuery() {
    given()
      .queryParam("startedBefore", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_STARTED_BEFORE)
      .queryParam("startedAfter", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_STARTED_AFTER)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyStartParameterQueryInvocations();
  }

  @Test
  void testHistoricBeforeAndAfterStartTimeQueryAsPost() {
    Map<String, Date> parameters = getCompleteStartDateQueryParameters();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(parameters)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyStartParameterQueryInvocations();
  }

  private Map<String, Date> getCompleteStartDateQueryParameters() {
    Map<String, Date> parameters = new HashMap<>();

    parameters.put("startedAfter", DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_STARTED_AFTER));
    parameters.put("startedBefore", DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_STARTED_BEFORE));

    return parameters;
  }

  private void verifyStartParameterQueryInvocations() {
    Map<String, Date> startDateParameters = getCompleteStartDateQueryParameters();

    verify(mockedQuery).startedBefore(startDateParameters.get("startedBefore"));
    verify(mockedQuery).startedAfter(startDateParameters.get("startedAfter"));

    verify(mockedQuery).list();
  }

  @Test
  void testHistoricAfterAndBeforeFinishTimeQuery() {
    given()
      .queryParam("finishedAfter", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_FINISHED_AFTER)
      .queryParam("finishedBefore", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_FINISHED_BEFORE)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyFinishedParameterQueryInvocations();
  }

  @Test
  void testHistoricAfterAndBeforeFinishTimeQueryAsPost() {
    Map<String, Date> parameters = getCompleteFinishedDateQueryParameters();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(parameters)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    verifyFinishedParameterQueryInvocations();
  }

  private Map<String, Date> getCompleteFinishedDateQueryParameters() {
    Map<String, Date> parameters = new HashMap<>();

    parameters.put("finishedAfter", DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_FINISHED_AFTER));
    parameters.put("finishedBefore", DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_FINISHED_BEFORE));

    return parameters;
  }

  private void verifyFinishedParameterQueryInvocations() {
    Map<String, Date> finishedDateParameters = getCompleteFinishedDateQueryParameters();

    verify(mockedQuery).finishedAfter(finishedDateParameters.get("finishedAfter"));
    verify(mockedQuery).finishedBefore(finishedDateParameters.get("finishedBefore"));

    verify(mockedQuery).list();
  }

  @Test
  void testTenantIdListParameter() {
    mockedQuery = setUpMockHistoricActivityInstanceQuery(createMockHistoricActivityInstancesTwoTenants());

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

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
  void testTenantIdListPostParameter() {
    mockedQuery = setUpMockHistoricActivityInstanceQuery(createMockHistoricActivityInstancesTwoTenants());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

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
  void testQueryFilterWithoutTenantIdParameter() {
    // given
    HistoricActivityInstance activityInstance = MockProvider.createMockHistoricActivityInstance(null);
    mockedQuery = setUpMockHistoricActivityInstanceQuery(Collections.singletonList(activityInstance));

    // when
    Response response = given()
        .queryParam("withoutTenantId", true)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

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
  void testQueryFilterWithoutTenantIdPostParameter() {
    // given
    HistoricActivityInstance activityInstance = MockProvider.createMockHistoricActivityInstance(null);
    mockedQuery = setUpMockHistoricActivityInstanceQuery(Collections.singletonList(activityInstance));
    Map<String, Object> queryParameters = Collections.singletonMap("withoutTenantId", (Object) true);

    // when
    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(HISTORIC_ACTIVITY_INSTANCE_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId).isNull();
  }

  private List<HistoricActivityInstance> createMockHistoricActivityInstancesTwoTenants() {
    return List.of(
        MockProvider.createMockHistoricActivityInstance(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockHistoricActivityInstance(MockProvider.ANOTHER_EXAMPLE_TENANT_ID));
  }
}
