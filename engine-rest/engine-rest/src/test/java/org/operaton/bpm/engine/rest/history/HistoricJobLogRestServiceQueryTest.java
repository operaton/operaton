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

import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
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

/**
 * @author Roman Smirnov
 *
 */
public class HistoricJobLogRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_JOB_LOG_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/job-log";
  protected static final String HISTORIC_JOB_LOG_COUNT_RESOURCE_URL = HISTORIC_JOB_LOG_RESOURCE_URL + "/count";

  protected static final long JOB_LOG_QUERY_MAX_PRIORITY = Long.MAX_VALUE;
  protected static final long JOB_LOG_QUERY_MIN_PRIORITY = Long.MIN_VALUE;

  protected HistoricJobLogQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockHistoricJobLogQuery(MockProvider.createMockHistoricJobLogs());
  }

  protected HistoricJobLogQuery setUpMockHistoricJobLogQuery(List<HistoricJobLog> mockedHistoricJogLogs) {
    HistoricJobLogQuery mockedhistoricJobLogQuery = mock(HistoricJobLogQuery.class);
    when(mockedhistoricJobLogQuery.list()).thenReturn(mockedHistoricJogLogs);
    when(mockedhistoricJobLogQuery.count()).thenReturn((long) mockedHistoricJogLogs.size());

    when(processEngine.getHistoryService().createHistoricJobLogQuery()).thenReturn(mockedhistoricJobLogQuery);

    return mockedhistoricJobLogQuery;
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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

    verify(mockedQuery).list();
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_JOB_LOG_RESOURCE_URL);

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
        .post(HISTORIC_JOB_LOG_RESOURCE_URL);

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
      .get(HISTORIC_JOB_LOG_RESOURCE_URL);
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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);
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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);
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
    executeAndVerifySorting("jobId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobDefinitionId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobDefinitionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobDefinitionId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobDefinitionId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobDueDate", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobDueDate();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobDueDate", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobDueDate();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobRetries", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobRetries();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobRetries", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobRetries();
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
    executeAndVerifySorting("deploymentId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByDeploymentId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("deploymentId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByDeploymentId();
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
    executeAndVerifySorting("jobPriority", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobPriority();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("jobPriority", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByJobPriority();
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
    executeAndVerifySorting("hostname", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHostname();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("hostname", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHostname();
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
      .when().post(HISTORIC_JOB_LOG_RESOURCE_URL);

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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(HISTORIC_JOB_LOG_COUNT_RESOURCE_URL);

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
        .post(HISTORIC_JOB_LOG_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testSimpleHistoricJobLogQuery() {
    String processInstanceId = MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    Response response = given()
        .queryParam("processInstanceId", processInstanceId)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_JOB_LOG_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).processInstanceId(processInstanceId);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> logs = from(content).getList("");
    assertThat(logs.get(0)).as("The returned historic job log should not be null.").isNotNull();

    verifyHistoricJobLogEntries(content);
  }

  @Test
  void testSimpleHistoricJobLogQueryAsPost() {
    String processInstanceId = MockProvider.EXAMPLE_HISTORIC_JOB_LOG_PROC_INST_ID;

    Map<String, Object> json = new HashMap<>();
    json.put("processInstanceId", processInstanceId);

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(json)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(HISTORIC_JOB_LOG_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).processInstanceId(processInstanceId);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> logs = from(content).getList("");
    assertThat(logs).as("There should be one historic job log returned.").hasSize(1);
    assertThat(logs.get(0)).as("The returned historic job log should not be null.").isNotNull();

    verifyHistoricJobLogEntries(content);
  }

  protected void verifyHistoricJobLogEntries(String content) {
    String returnedId = from(content).getString("[0].id");
    String returnedTimestamp = from(content).getString("[0].timestamp");
    String returnedRemovalTime = from(content).getString("[0].removalTime");
    String returnedJobId = from(content).getString("[0].jobId");
    String returnedJobDueDate = from(content).getString("[0].jobDueDate");
    int returnedJobRetries = from(content).getInt("[0].jobRetries");
    long returnedJobPriority = from(content).getLong("[0].jobPriority");
    String returnedJobExceptionMessage = from(content).getString("[0].jobExceptionMessage");
    String returnedJobDefinitionId = from(content).getString("[0].jobDefinitionId");
    String returnedJobDefinitionType = from(content).getString("[0].jobDefinitionType");
    String returnedJobDefinitionConfiguration = from(content).getString("[0].jobDefinitionConfiguration");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedFailedActivityId = from(content).getString("[0].failedActivityId");
    String returnedExecutionId = from(content).getString("[0].executionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String returnedDeploymentId = from(content).getString("[0].deploymentId");
    String returnedRootProcessInstanceId = from(content).getString("[0].rootProcessInstanceId");
    String returnedHostname = from(content).getString("[0].hostname");
    boolean returnedCreationLog = from(content).getBoolean("[0].creationLog");
    boolean returnedFailureLog = from(content).getBoolean("[0].failureLog");
    boolean returnedSuccessLog = from(content).getBoolean("[0].successLog");
    boolean returnedDeletionLog = from(content).getBoolean("[0].deletionLog");

    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_ID);
    assertThat(returnedTimestamp).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_TIMESTAMP);
    assertThat(returnedRemovalTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_REMOVAL_TIME);
    assertThat(returnedJobId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_ID);
    assertThat(returnedJobDueDate).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_DUE_DATE);
    assertThat(returnedJobRetries).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_RETRIES);
    assertThat(returnedJobPriority).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_PRIORITY);
    assertThat(returnedJobExceptionMessage).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_EXCEPTION_MSG);
    assertThat(returnedJobDefinitionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_DEF_ID);
    assertThat(returnedJobDefinitionType).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_DEF_TYPE);
    assertThat(returnedJobDefinitionConfiguration).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_DEF_CONFIG);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_ACTIVITY_ID);
    assertThat(returnedFailedActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_FAILED_ACTIVITY_ID);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_EXECUTION_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_PROC_INST_ID);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_PROC_DEF_ID);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_PROC_DEF_KEY);
    assertThat(returnedDeploymentId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_DEPLOYMENT_ID);
    assertThat(returnedRootProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_ROOT_PROC_INST_ID);
    assertThat(returnedHostname).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_HOSTNAME);
    assertThat(returnedCreationLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_CREATION_LOG);
    assertThat(returnedFailureLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_FAILURE_LOG);
    assertThat(returnedSuccessLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_SUCCESS_LOG);
    assertThat(returnedDeletionLog).isEqualTo(MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_DELETION_LOG);
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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

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
        .post(HISTORIC_JOB_LOG_RESOURCE_URL);

    verifyStringParameterQueryInvocations();
  }

  protected Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("logId", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_ID);
    parameters.put("jobId", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_ID);
    parameters.put("jobExceptionMessage", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_EXCEPTION_MSG);
    parameters.put("jobDefinitionId", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_DEF_ID);
    parameters.put("jobDefinitionType", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_DEF_CONFIG);
    parameters.put("jobDefinitionConfiguration", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_JOB_DEF_CONFIG);
    parameters.put("processInstanceId", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_PROC_INST_ID);
    parameters.put("processDefinitionId", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_PROC_DEF_ID);
    parameters.put("processDefinitionKey", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_PROC_DEF_KEY);
    parameters.put("deploymentId", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_DEPLOYMENT_ID);
    parameters.put("hostname", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_HOSTNAME);

    return parameters;
  }

  protected void verifyStringParameterQueryInvocations() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    verify(mockedQuery).logId(stringQueryParameters.get("logId"));
    verify(mockedQuery).jobId(stringQueryParameters.get("jobId"));
    verify(mockedQuery).jobExceptionMessage(stringQueryParameters.get("jobExceptionMessage"));
    verify(mockedQuery).jobDefinitionId(stringQueryParameters.get("jobDefinitionId"));
    verify(mockedQuery).jobDefinitionType(stringQueryParameters.get("jobDefinitionType"));
    verify(mockedQuery).jobDefinitionConfiguration(stringQueryParameters.get("jobDefinitionConfiguration"));
    verify(mockedQuery).processInstanceId(stringQueryParameters.get("processInstanceId"));
    verify(mockedQuery).processDefinitionId(stringQueryParameters.get("processDefinitionId"));
    verify(mockedQuery).processDefinitionKey(stringQueryParameters.get("processDefinitionKey"));
    verify(mockedQuery).deploymentId(stringQueryParameters.get("deploymentId"));
    verify(mockedQuery).hostname(stringQueryParameters.get("hostname"));

    verify(mockedQuery).list();
  }

  @Test
  void testListParameters() {
    String anActId = "anActId";
    String anotherActId = "anotherActId";

    String anExecutionId = "anExecutionId";
    String anotherExecutionId = "anotherExecutionId";

    given()
      .queryParam("activityIdIn", anActId + "," + anotherActId)
      .queryParam("executionIdIn", anExecutionId + "," + anotherExecutionId)
      .queryParam("failedActivityIdIn", anActId + "," + anotherActId)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

    verify(mockedQuery).activityIdIn(anActId, anotherActId);
    verify(mockedQuery).executionIdIn(anExecutionId, anotherExecutionId);
    verify(mockedQuery).failedActivityIdIn(anActId, anotherActId);
    verify(mockedQuery).list();
  }

  @Test
  void testListParametersAsPost() {
    String anActId = "anActId";
    String anotherActId = "anotherActId";

    String anExecutionId = "anExecutionId";
    String anotherExecutionId = "anotherExecutionId";

    Map<String, List<String>> json = new HashMap<>();
    json.put("activityIdIn", List.of(anActId, anotherActId));
    json.put("executionIdIn", List.of(anExecutionId, anotherExecutionId));
    json.put("failedActivityIdIn", List.of(anActId, anotherActId));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_JOB_LOG_RESOURCE_URL);

    verify(mockedQuery).activityIdIn(anActId, anotherActId);
    verify(mockedQuery).executionIdIn(anExecutionId, anotherExecutionId);
    verify(mockedQuery).failedActivityIdIn(anActId, anotherActId);
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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

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
        .post(HISTORIC_JOB_LOG_RESOURCE_URL);

    verifyBooleanParameterQueryInvocations();

  }

  protected Map<String, Boolean> getCompleteBooleanQueryParameters() {
    Map<String, Boolean> parameters = new HashMap<>();

    parameters.put("creationLog", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_CREATION_LOG);
    parameters.put("failureLog", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_FAILURE_LOG);
    parameters.put("successLog", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_SUCCESS_LOG);
    parameters.put("deletionLog", MockProvider.EXAMPLE_HISTORIC_JOB_LOG_IS_DELETION_LOG);

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
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

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
        .post(HISTORIC_JOB_LOG_RESOURCE_URL);

    verifyIntegerParameterQueryInvocations();

  }

  protected Map<String, Object> getCompleteIntegerQueryParameters() {
    Map<String, Object> parameters = new HashMap<>();

    parameters.put("jobPriorityLowerThanOrEquals", JOB_LOG_QUERY_MAX_PRIORITY);
    parameters.put("jobPriorityHigherThanOrEquals", JOB_LOG_QUERY_MIN_PRIORITY);

    return parameters;
  }

  protected void verifyIntegerParameterQueryInvocations() {
    verify(mockedQuery).jobPriorityLowerThanOrEquals(JOB_LOG_QUERY_MAX_PRIORITY);
    verify(mockedQuery).jobPriorityHigherThanOrEquals(JOB_LOG_QUERY_MIN_PRIORITY);
    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testTenantIdListParameter() {
    mockedQuery = setUpMockHistoricJobLogQuery(createMockHistoricJobLogsTwoTenants());

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_JOB_LOG_RESOURCE_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> jobLogs = from(content).getList("");
    assertThat(jobLogs).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testTenantIdListPostParameter() {
    mockedQuery = setUpMockHistoricJobLogQuery(createMockHistoricJobLogsTwoTenants());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_JOB_LOG_RESOURCE_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> jobLogs = from(content).getList("");
    assertThat(jobLogs).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testQueryWithoutTenantIdQueryParameter() {
    // given
    HistoricJobLog jobLog = MockProvider.createMockHistoricJobLog(null);
    mockedQuery = setUpMockHistoricJobLogQuery(Collections.singletonList(jobLog));

    // when
    Response response = given()
        .queryParam("withoutTenantId", true)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(HISTORIC_JOB_LOG_RESOURCE_URL);

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
    HistoricJobLog jobLog = MockProvider.createMockHistoricJobLog(null);
    mockedQuery = setUpMockHistoricJobLogQuery(Collections.singletonList(jobLog));
    Map<String, Object> queryParameters = Collections.singletonMap("withoutTenantId", (Object) true);

    // when
    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(HISTORIC_JOB_LOG_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId).isNull();
  }

  private List<HistoricJobLog> createMockHistoricJobLogsTwoTenants() {
    return List.of(
        MockProvider.createMockHistoricJobLog(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockHistoricJobLog(MockProvider.ANOTHER_EXAMPLE_TENANT_ID));
  }
}
