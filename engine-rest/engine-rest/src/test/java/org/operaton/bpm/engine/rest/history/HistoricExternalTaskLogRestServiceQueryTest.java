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

import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricExternalTaskLogQuery;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.OrderingBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HistoricExternalTaskLogRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/external-task-log";
  protected static final String HISTORIC_EXTERNAL_TASK_LOG_COUNT_RESOURCE_URL = HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL + "/count";

  protected static final long EXTERNAL_TASK_LOG_QUERY_MAX_PRIORITY = Long.MAX_VALUE;
  protected static final long EXTERNAL_TASK_LOG_QUERY_MIN_PRIORITY = Long.MIN_VALUE;

  protected HistoricExternalTaskLogQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockHistoricExternalTaskLogQuery(MockProvider.createMockHistoricExternalTaskLogs());
  }

  protected HistoricExternalTaskLogQuery setUpMockHistoricExternalTaskLogQuery(List<HistoricExternalTaskLog> mockedHistoricExternalTaskLogs) {
    HistoricExternalTaskLogQuery mockedHistoricExternalTaskLogQuery = mock(HistoricExternalTaskLogQuery.class);
    when(mockedHistoricExternalTaskLogQuery.list()).thenReturn(mockedHistoricExternalTaskLogs);
    when(mockedHistoricExternalTaskLogQuery.count()).thenReturn((long) mockedHistoricExternalTaskLogs.size());

    when(processEngine.getHistoryService().createHistoricExternalTaskLogQuery()).thenReturn(mockedHistoricExternalTaskLogQuery);

    return mockedHistoricExternalTaskLogQuery;
  }

  @Test
  void testEmptyQuery() {
    String queryKey = "";
    given()
      .queryParam("processDefinitionKey", queryKey)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).list();
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testNoParametersQueryAsPost() {
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("definitionId", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder)
    .then()
      .expect()
        .statusCode(expectedStatus.getStatusCode())
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);
  }

  @Test
  void testSortByParameterOnly() {
    given()
      .queryParam("sortBy", "processDefinitionId")
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);
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
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("timestamp", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTimestamp();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("timestamp", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTimestamp();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("externalTaskId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByExternalTaskId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("externalTaskId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByExternalTaskId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("topicName", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTopicName();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("topicName", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTopicName();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("workerId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByWorkerId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("workerId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByWorkerId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("retries", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByRetries();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("retries", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByRetries();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("priority", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByPriority();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("priority", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByPriority();
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
    executeAndVerifySorting("activityInstanceId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityInstanceId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByActivityInstanceId();
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
    executeAndVerifySorting("processDefinitionKey", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionKey", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).desc();
  }

  @Test
  void testSecondarySortingAsPost() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    Map<String, Object> json = new HashMap<>();
    json.put("sorting", OrderingBuilder.create()
      .orderBy("processInstanceId").desc()
      .orderBy("timestamp").asc()
      .getJson());
    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).desc();
    inOrder.verify(mockedQuery).orderByTimestamp();
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
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

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
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

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
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_COUNT_RESOURCE_URL);

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
      .post(HISTORIC_EXTERNAL_TASK_LOG_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testSimpleHistoricExternalTaskLogQuery() {
    String processInstanceId = MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    Response response = given()
      .queryParam("processInstanceId", processInstanceId)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).processInstanceId(processInstanceId);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> logs = from(content).getList("");
    assertThat(logs).as("There should be one historic externalTask log returned.").hasSize(1);
    assertNotNull("The returned historic externalTask log should not be null.", logs.get(0));

    String returnedId = from(content).getString("[0].id");
    String returnedTimestamp = from(content).getString("[0].timestamp");
    String returnedExternalTaskId = from(content).getString("[0].externalTaskId");
    String returnedExternalTaskTopicName = from(content).getString("[0].topicName");
    String returnedExternalTaskWorkerId = from(content).getString("[0].workerId");
    int returnedRetries = from(content).getInt("[0].retries");
    long returnedPriority = from(content).getLong("[0].priority");
    String returnedErrorMessage = from(content).getString("[0].errorMessage");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedActivityInstanceId = from(content).getString("[0].activityInstanceId");
    String returnedExecutionId = from(content).getString("[0].executionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    boolean returnedCreationLog = from(content).getBoolean("[0].creationLog");
    boolean returnedFailureLog = from(content).getBoolean("[0].failureLog");
    boolean returnedSuccessLog = from(content).getBoolean("[0].successLog");
    boolean returnedDeletionLog = from(content).getBoolean("[0].deletionLog");

    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ID);
    assertThat(returnedTimestamp).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_TIMESTAMP);
    assertThat(returnedExternalTaskId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_EXTERNAL_TASK_ID);
    assertThat(returnedExternalTaskTopicName).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_TOPIC_NAME);
    assertThat(returnedExternalTaskWorkerId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_WORKER_ID);
    assertThat(returnedRetries).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_RETRIES);
    assertThat(returnedPriority).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PRIORITY);
    assertThat(returnedErrorMessage).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ERROR_MSG);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ACTIVITY_ID);
    assertThat(returnedActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ACTIVITY_INSTANCE_ID);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_EXECUTION_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_INST_ID);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_DEF_ID);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_DEF_KEY);
    assertThat(returnedCreationLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_CREATION_LOG);
    assertThat(returnedFailureLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_FAILURE_LOG);
    assertThat(returnedSuccessLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_SUCCESS_LOG);
    assertThat(returnedDeletionLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_DELETION_LOG);
  }

  @Test
  void testSimpleHistoricExternalTaskLogQueryAsPost() {
    String processInstanceId = MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_INST_ID;

    Map<String, Object> json = new HashMap<>();
    json.put("processInstanceId", processInstanceId);

    Response response =
      given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(json)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).processInstanceId(processInstanceId);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> logs = from(content).getList("");
    assertThat(logs).as("There should be one historic externalTask log returned.").hasSize(1);
    assertNotNull("The returned historic externalTask log should not be null.", logs.get(0));

    String returnedId = from(content).getString("[0].id");
    String returnedTimestamp = from(content).getString("[0].timestamp");
    String returnedRemovalTime = from(content).getString("[0].removalTime");
    String returnedExternalTaskId = from(content).getString("[0].externalTaskId");
    String returnedExternalTaskTopicName = from(content).getString("[0].topicName");
    String returnedExternalTaskWorkerId = from(content).getString("[0].workerId");
    int returnedRetries = from(content).getInt("[0].retries");
    long returnedPriority = from(content).getLong("[0].priority");
    String returnedErrorMessage = from(content).getString("[0].errorMessage");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedActivityInstanceId = from(content).getString("[0].activityInstanceId");
    String returnedExecutionId = from(content).getString("[0].executionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String returnedRootProcessInstanceId = from(content).getString("[0].rootProcessInstanceId");
    boolean returnedCreationLog = from(content).getBoolean("[0].creationLog");
    boolean returnedFailureLog = from(content).getBoolean("[0].failureLog");
    boolean returnedSuccessLog = from(content).getBoolean("[0].successLog");
    boolean returnedDeletionLog = from(content).getBoolean("[0].deletionLog");

    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ID);
    assertThat(returnedTimestamp).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_TIMESTAMP);
    assertThat(returnedRemovalTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_REMOVAL_TIME);
    assertThat(returnedExternalTaskId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_EXTERNAL_TASK_ID);
    assertThat(returnedExternalTaskTopicName).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_TOPIC_NAME);
    assertThat(returnedExternalTaskWorkerId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_WORKER_ID);
    assertThat(returnedRetries).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_RETRIES);
    assertThat(returnedPriority).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PRIORITY);
    assertThat(returnedErrorMessage).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ERROR_MSG);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ACTIVITY_ID);
    assertThat(returnedActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ACTIVITY_INSTANCE_ID);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_EXECUTION_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_INST_ID);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_DEF_ID);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_DEF_KEY);
    assertThat(returnedRootProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ROOT_PROC_INST_ID);
    assertThat(returnedCreationLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_CREATION_LOG);
    assertThat(returnedFailureLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_FAILURE_LOG);
    assertThat(returnedSuccessLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_SUCCESS_LOG);
    assertThat(returnedDeletionLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_DELETION_LOG);
  }

  @Test
  void testStringParameters() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    given()
      .queryParams(stringQueryParameters)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verifyStringParameterQueryInvocations();
  }

  @Test
  void testStringParametersAsPost() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(stringQueryParameters)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verifyStringParameterQueryInvocations();
  }

  protected Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("logId", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ID);
    parameters.put("externalTaskId", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_EXTERNAL_TASK_ID);
    parameters.put("topicName", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_TOPIC_NAME);
    parameters.put("workerId", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_WORKER_ID);
    parameters.put("errorMessage", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_ERROR_MSG);
    parameters.put("processInstanceId", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_INST_ID);
    parameters.put("processDefinitionId", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_DEF_ID);
    parameters.put("processDefinitionKey", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_PROC_DEF_KEY);

    return parameters;
  }

  protected void verifyStringParameterQueryInvocations() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    verify(mockedQuery).logId(stringQueryParameters.get("logId"));
    verify(mockedQuery).externalTaskId(stringQueryParameters.get("externalTaskId"));
    verify(mockedQuery).topicName(stringQueryParameters.get("topicName"));
    verify(mockedQuery).workerId(stringQueryParameters.get("workerId"));
    verify(mockedQuery).errorMessage(stringQueryParameters.get("errorMessage"));
    verify(mockedQuery).processInstanceId(stringQueryParameters.get("processInstanceId"));
    verify(mockedQuery).processDefinitionId(stringQueryParameters.get("processDefinitionId"));
    verify(mockedQuery).processDefinitionKey(stringQueryParameters.get("processDefinitionKey"));

    verify(mockedQuery).list();
  }

  @Test
  void testListParameters() {
    String anActId = "anActId";
    String anotherActId = "anotherActId";

    String anActInstId = "anActInstId";
    String anotherActInstId = "anotherActInstId";

    String anExecutionId = "anExecutionId";
    String anotherExecutionId = "anotherExecutionId";

    given()
      .queryParam("activityIdIn", anActId + "," + anotherActId)
      .queryParam("activityInstanceIdIn", anActInstId + "," + anotherActInstId)
      .queryParam("executionIdIn", anExecutionId + "," + anotherExecutionId)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).activityIdIn(anActId, anotherActId);
    verify(mockedQuery).activityInstanceIdIn(anActInstId, anotherActInstId);
    verify(mockedQuery).executionIdIn(anExecutionId, anotherExecutionId);
    verify(mockedQuery).list();
  }

  @Test
  void testListParametersAsPost() {
    String anActId = "anActId";
    String anotherActId = "anotherActId";

    String anActInstId = "anActInstId";
    String anotherActInstId = "anotherActInstId";

    String anExecutionId = "anExecutionId";
    String anotherExecutionId = "anotherExecutionId";

    Map<String, List<String>> json = new HashMap<>();
    json.put("activityIdIn", List.of(anActId, anotherActId));
    json.put("activityInstanceIdIn", List.of(anActInstId, anotherActInstId));
    json.put("executionIdIn", List.of(anExecutionId, anotherExecutionId));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).activityIdIn(anActId, anotherActId);
    verify(mockedQuery).activityInstanceIdIn(anActInstId, anotherActInstId);
    verify(mockedQuery).executionIdIn(anExecutionId, anotherExecutionId);
    verify(mockedQuery).list();
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
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

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
      .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verifyBooleanParameterQueryInvocations();

  }

  protected Map<String, Boolean> getCompleteBooleanQueryParameters() {
    Map<String, Boolean> parameters = new HashMap<>();

    parameters.put("creationLog", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_CREATION_LOG);
    parameters.put("failureLog", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_FAILURE_LOG);
    parameters.put("successLog", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_SUCCESS_LOG);
    parameters.put("deletionLog", MockProvider.EXAMPLE_HISTORIC_EXTERNAL_TASK_LOG_IS_DELETION_LOG);

    return parameters;
  }

  protected void verifyBooleanParameterQueryInvocations() {
    verify(mockedQuery).creationLog();
    verify(mockedQuery).failureLog();
    verify(mockedQuery).successLog();
    verify(mockedQuery).deletionLog();
    verify(mockedQuery).list();
  }

  @Test
  void testIntegerParameters() {
    Map<String, Object> params = getCompleteIntegerQueryParameters();

    given()
      .queryParams(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verifyIntegerParameterQueryInvocations();
  }

  @Test
  void testIntegerParametersAsPost() {
    Map<String, Object> params = getCompleteIntegerQueryParameters();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verifyIntegerParameterQueryInvocations();

  }

  protected Map<String, Object> getCompleteIntegerQueryParameters() {
    Map<String, Object> parameters = new HashMap<>();

    parameters.put("priorityLowerThanOrEquals", EXTERNAL_TASK_LOG_QUERY_MAX_PRIORITY);
    parameters.put("priorityHigherThanOrEquals", EXTERNAL_TASK_LOG_QUERY_MIN_PRIORITY);

    return parameters;
  }

  protected void verifyIntegerParameterQueryInvocations() {
    verify(mockedQuery).priorityLowerThanOrEquals(EXTERNAL_TASK_LOG_QUERY_MAX_PRIORITY);
    verify(mockedQuery).priorityHigherThanOrEquals(EXTERNAL_TASK_LOG_QUERY_MIN_PRIORITY);
    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testTenantIdListParameter() {
    mockedQuery = setUpMockHistoricExternalTaskLogQuery(createMockHistoricExternalTaskLogsTwoTenants());

    Response response =
      given()
        .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> externalTaskLogs = from(content).getList("");
    assertThat(externalTaskLogs).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testTenantIdListPostParameter() {
    mockedQuery = setUpMockHistoricExternalTaskLogQuery(createMockHistoricExternalTaskLogsTwoTenants());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST.split(","));

    Response response =
      given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> externalTaskLogs = from(content).getList("");
    assertThat(externalTaskLogs).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testQueryWithoutTenantIdQueryParameter() {
    // given
    mockedQuery = setUpMockHistoricExternalTaskLogQuery(Collections.singletonList(MockProvider.createMockHistoricExternalTaskLog(null)));

    // when
    Response response = given()
        .queryParam("withoutTenantId", true)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

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
  void testQueryWithoutTenantIdPostParameter() {
    // given
    mockedQuery = setUpMockHistoricExternalTaskLogQuery(Collections.singletonList(MockProvider.createMockHistoricExternalTaskLog(null)));
    Map<String, Object> queryParameters = Collections.singletonMap("withoutTenantId", (Object) true);

    // when
    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(HISTORIC_EXTERNAL_TASK_LOG_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId).isNull();
  }

  private List<HistoricExternalTaskLog> createMockHistoricExternalTaskLogsTwoTenants() {
    return List.of(
      MockProvider.createMockHistoricExternalTaskLog(MockProvider.EXAMPLE_TENANT_ID),
      MockProvider.createMockHistoricExternalTaskLog(MockProvider.ANOTHER_EXAMPLE_TENANT_ID));
  }

}
