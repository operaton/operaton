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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.OrderingBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static org.operaton.bpm.engine.rest.util.DateTimeUtils.withTimezone;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Thorben Lindhauer
 *
 */
public class ExternalTaskRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String EXTERNAL_TASK_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/external-task";
  protected static final String EXTERNAL_TASK_COUNT_QUERY_URL = EXTERNAL_TASK_QUERY_URL + "/count";
  public static final long EXTERNAL_TASK_LOW_BOUND_PRIORITY = 3L;
  public static final long EXTERNAL_TASK_HIGH_BOUND_PRIORITY = 4L;

  protected ExternalTaskQuery mockQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockQuery = setUpMockExternalTaskQuery(MockProvider.createMockExternalTasks());
  }

  private ExternalTaskQuery setUpMockExternalTaskQuery(List<ExternalTask> mockedTasks) {
    ExternalTaskQuery sampleTaskQuery = mock(ExternalTaskQuery.class);
    when(sampleTaskQuery.list()).thenReturn(mockedTasks);
    when(sampleTaskQuery.count()).thenReturn((long) mockedTasks.size());

    when(processEngine.getExternalTaskService().createExternalTaskQuery()).thenReturn(sampleTaskQuery);

    return sampleTaskQuery;
  }

  @Test
  void testInvalidDateParameter() {
    given().queryParams("lockExpirationBefore", "anInvalidDate")
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Cannot set query parameter 'lockExpirationBefore' to value 'anInvalidDate': "
          + "Cannot convert value \"anInvalidDate\" to java type java.util.Date"))
      .when().get(EXTERNAL_TASK_QUERY_URL);
  }

  @Test
  void testSortByParameterOnly() {
    given().queryParam("sortBy", "processInstanceId")
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(EXTERNAL_TASK_QUERY_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given().queryParam("sortOrder", "asc")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(EXTERNAL_TASK_QUERY_URL);
  }

  @Test
  void testSimpleTaskQuery() {
    Response response = given()
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(EXTERNAL_TASK_QUERY_URL);

    Mockito.verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    Assertions.assertEquals(1, instances.size(), "There should be one external task returned.");
    assertThat(instances.get(0)).as("The returned external task should not be null.").isNotNull();

    String activityId = from(content).getString("[0].activityId");
    String activityInstanceId = from(content).getString("[0].activityInstanceId");
    String errorMessage = from(content).getString("[0].errorMessage");
    String executionId = from(content).getString("[0].executionId");
    String id = from(content).getString("[0].id");
    String lockExpirationTime = from(content).getString("[0].lockExpirationTime");
    String createTime = from(content).getString("[0].createTime");
    String processDefinitionId = from(content).getString("[0].processDefinitionId");
    String processDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String processDefinitionVersionTag = from(content).getString("[0].processDefinitionVersionTag");
    String processInstanceId = from(content).getString("[0].processInstanceId");
    Integer retries = from(content).getInt("[0].retries");
    Boolean suspended = from(content).getBoolean("[0].suspended");
    String topicName = from(content).getString("[0].topicName");
    String workerId = from(content).getString("[0].workerId");
    String tenantId = from(content).getString("[0].tenantId");
    long priority = from(content).getLong("[0].priority");
    String businessKey = from(content).getString("[0].businessKey");

    Assertions.assertEquals(MockProvider.EXAMPLE_ACTIVITY_ID, activityId);
    Assertions.assertEquals(MockProvider.EXAMPLE_ACTIVITY_INSTANCE_ID, activityInstanceId);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_ERROR_MESSAGE, errorMessage);
    Assertions.assertEquals(MockProvider.EXAMPLE_EXECUTION_ID, executionId);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_ID, id);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_LOCK_EXPIRATION_TIME, lockExpirationTime);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_CREATE_TIME, createTime);
    Assertions.assertEquals(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, processDefinitionId);
    Assertions.assertEquals(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, processDefinitionKey);
    Assertions.assertEquals(MockProvider.EXAMPLE_PROCESS_DEFINITION_VERSION_TAG, processDefinitionVersionTag);
    Assertions.assertEquals(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, processInstanceId);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_RETRIES, retries);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_SUSPENDED, suspended);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_TOPIC_NAME, topicName);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_WORKER_ID, workerId);
    Assertions.assertEquals(MockProvider.EXAMPLE_TENANT_ID, tenantId);
    Assertions.assertEquals(MockProvider.EXTERNAL_TASK_PRIORITY, priority);
    Assertions.assertEquals(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY, businessKey);
  }

  @Test
  void testCompleteGETQuery() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("externalTaskId", "someExternalTaskId");
    parameters.put("activityId", "someActivityId");
    parameters.put("lockExpirationBefore", withTimezone("2013-01-23T14:42:42"));
    parameters.put("lockExpirationAfter", withTimezone("2013-01-23T15:52:52"));
    parameters.put("topicName", "someTopic");
    parameters.put("locked", "true");
    parameters.put("notLocked", "true");
    parameters.put("executionId", "someExecutionId");
    parameters.put("processInstanceId", "someProcessInstanceId");
    parameters.put("processInstanceIdIn", "aProcessInstanceId,anotherProcessInstanceId");
    parameters.put("processDefinitionId", "someProcessDefinitionId");
    parameters.put("processDefinitionVersionTag", "someProcessDefinitionVersionTag");
    parameters.put("active", "true");
    parameters.put("suspended", "true");
    parameters.put("withRetriesLeft", "true");
    parameters.put("noRetriesLeft", "true");
    parameters.put("workerId", "someWorkerId");
    parameters.put("priorityHigherThanOrEquals", "3");
    parameters.put("priorityLowerThanOrEquals", "4");

    given()
      .queryParams(parameters)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().get(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).externalTaskId("someExternalTaskId");
    verify(mockQuery).activityId("someActivityId");
    verify(mockQuery).lockExpirationBefore(DateTimeUtil.parseDate(withTimezone("2013-01-23T14:42:42")));
    verify(mockQuery).lockExpirationAfter(DateTimeUtil.parseDate(withTimezone("2013-01-23T15:52:52")));
    verify(mockQuery).topicName("someTopic");
    verify(mockQuery).locked();
    verify(mockQuery).notLocked();
    verify(mockQuery).executionId("someExecutionId");
    verify(mockQuery).processInstanceId("someProcessInstanceId");
    verify(mockQuery).processInstanceIdIn("aProcessInstanceId", "anotherProcessInstanceId");
    verify(mockQuery).processDefinitionId("someProcessDefinitionId");
    verify(mockQuery).processDefinitionId("someProcessDefinitionId");
    verify(mockQuery).active();
    verify(mockQuery).suspended();
    verify(mockQuery).withRetriesLeft();
    verify(mockQuery).noRetriesLeft();
    verify(mockQuery).workerId("someWorkerId");
    verify(mockQuery).priorityHigherThanOrEquals(3);
    verify(mockQuery).priorityLowerThanOrEquals(4);
  }

  @Test
  void testCompletePOSTQuery() {
    Map<String, Object> parameters = new HashMap<>();

    parameters.put("externalTaskId", "someExternalTaskId");
    parameters.put("activityId", "someActivityId");
    parameters.put("lockExpirationBefore", withTimezone("2013-01-23T14:42:42"));
    parameters.put("lockExpirationAfter", withTimezone("2013-01-23T15:52:52"));
    parameters.put("topicName", "someTopic");
    parameters.put("locked", "true");
    parameters.put("notLocked", "true");
    parameters.put("executionId", "someExecutionId");
    parameters.put("processInstanceId", "someProcessInstanceId");
    parameters.put("processInstanceIdIn", Arrays.asList("aProcessInstanceId", "anotherProcessInstanceId"));
    parameters.put("processDefinitionId", "someProcessDefinitionId");
    parameters.put("active", "true");
    parameters.put("suspended", "true");
    parameters.put("withRetriesLeft", "true");
    parameters.put("noRetriesLeft", "true");
    parameters.put("workerId", "someWorkerId");
    parameters.put("priorityHigherThanOrEquals", "3");
    parameters.put("priorityLowerThanOrEquals", "4");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(parameters)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().post(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).externalTaskId("someExternalTaskId");
    verify(mockQuery).activityId("someActivityId");
    verify(mockQuery).lockExpirationBefore(DateTimeUtil.parseDate(withTimezone("2013-01-23T14:42:42")));
    verify(mockQuery).lockExpirationAfter(DateTimeUtil.parseDate(withTimezone("2013-01-23T15:52:52")));
    verify(mockQuery).topicName("someTopic");
    verify(mockQuery).locked();
    verify(mockQuery).notLocked();
    verify(mockQuery).executionId("someExecutionId");
    verify(mockQuery).processInstanceId("someProcessInstanceId");
    verify(mockQuery).processInstanceIdIn("aProcessInstanceId", "anotherProcessInstanceId");
    verify(mockQuery).processDefinitionId("someProcessDefinitionId");
    verify(mockQuery).active();
    verify(mockQuery).suspended();
    verify(mockQuery).withRetriesLeft();
    verify(mockQuery).noRetriesLeft();
    verify(mockQuery).workerId("someWorkerId");
    verify(mockQuery).priorityHigherThanOrEquals(3);
    verify(mockQuery).priorityLowerThanOrEquals(4);
  }

  @Test
  void testSortingParameters() {
    // desc
    InOrder inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("id", "desc", Status.OK);
    inOrder.verify(mockQuery).orderById();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("lockExpirationTime", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByLockExpirationTime();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("processInstanceId", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessInstanceId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("processDefinitionId", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessDefinitionId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("processDefinitionKey", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTenantId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("taskPriority", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByPriority();
    inOrder.verify(mockQuery).desc();
    // asc
    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("id", "asc", Status.OK);
    inOrder.verify(mockQuery).orderById();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("lockExpirationTime", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByLockExpirationTime();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("processInstanceId", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessInstanceId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("processDefinitionId", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessDefinitionId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("processDefinitionKey", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTenantId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyGETSorting("taskPriority", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByPriority();
    inOrder.verify(mockQuery).asc();
  }

  protected void executeAndVerifyGETSorting(String sortBy, String sortOrder, Status expectedStatus) {
    given().queryParam("sortBy", sortBy).queryParam("sortOrder", sortOrder)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(expectedStatus.getStatusCode())
      .when().get(EXTERNAL_TASK_QUERY_URL);
  }

  @Test
  void testPOSTQuerySorting() {
    InOrder inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifyPOSTSorting(
      OrderingBuilder.create()
        .orderBy("processDefinitionKey").desc()
        .orderBy("lockExpirationTime").asc()
        .getJson(),
      Status.OK);

    inOrder.verify(mockQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockQuery).desc();
    inOrder.verify(mockQuery).orderByLockExpirationTime();
    inOrder.verify(mockQuery).asc();
  }

  protected void executeAndVerifyPOSTSorting(List<Map<String, Object>> sortingJson, Status expectedStatus) {
    Map<String, Object> json = new HashMap<>();
    json.put("sorting", sortingJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(expectedStatus.getStatusCode())
      .when().post(EXTERNAL_TASK_QUERY_URL);
  }

  @Test
  void testPaginationGET() {
    int firstResult = 0;
    int maxResults = 10;
    given().queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).listPage(firstResult, maxResults);
  }

  @Test
  void testPaginationPOST() {
    int firstResult = 0;
    int maxResults = 10;
    given()
      .queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).listPage(firstResult, maxResults);
  }

  @Test
  void testGETQueryCount() {
    given()
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .body("count", equalTo(1))
    .when()
      .get(EXTERNAL_TASK_COUNT_QUERY_URL);

    verify(mockQuery).count();
  }

  @Test
  void testPOSTQueryCount() {
    given().contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect().statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
      .when().post(EXTERNAL_TASK_COUNT_QUERY_URL);

    verify(mockQuery).count();
  }

  @Test
  void testQueryByTenantIdListGet() {
    mockQuery = setUpMockExternalTaskQuery(createMockExternalTasksTwoTenants());

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testQueryByTenantIdListPost() {
    mockQuery = setUpMockExternalTaskQuery(createMockExternalTasksTwoTenants());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  private List<ExternalTask> createMockExternalTasksTwoTenants() {
    return Arrays.asList(
        MockProvider.mockExternalTask().buildExternalTask(),
        MockProvider.mockExternalTask().tenantId(MockProvider.ANOTHER_EXAMPLE_TENANT_ID).buildExternalTask());
  }

  @Test
  void testQueryByActivityIdListGet() {
    mockQuery = setUpMockExternalTaskQuery(createMockExternalTasksTwoActivityIds());

    Response response = given()
        .queryParam("activityIdIn", MockProvider.EXAMPLE_ACTIVITY_ID_LIST)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).activityIdIn(MockProvider.EXAMPLE_ACTIVITY_ID, MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedActivityId1 = from(content).getString("[0].activityId");
    String returnedActivityId2 = from(content).getString("[1].activityId");

    assertThat(returnedActivityId1).isEqualTo(MockProvider.EXAMPLE_ACTIVITY_ID);
    assertThat(returnedActivityId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID);
  }

  @Test
  void testQueryByActivityIdListPost() {
    mockQuery = setUpMockExternalTaskQuery(createMockExternalTasksTwoActivityIds());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("activityIdIn", MockProvider.EXAMPLE_ACTIVITY_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).activityIdIn(MockProvider.EXAMPLE_ACTIVITY_ID, MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedActivityId1 = from(content).getString("[0].activityId");
    String returnedActivityId2 = from(content).getString("[1].activityId");

    assertThat(returnedActivityId1).isEqualTo(MockProvider.EXAMPLE_ACTIVITY_ID);
    assertThat(returnedActivityId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID);
  }

  private List<ExternalTask> createMockExternalTasksTwoActivityIds() {
    return Arrays.asList(
        MockProvider.mockExternalTask().buildExternalTask(),
        MockProvider.mockExternalTask().activityId(MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID).buildExternalTask());
  }

  @Test
  void testQueryByPriorityListGet() {
    mockQuery = setUpMockExternalTaskQuery(createMockedExternalTasksWithPriorities());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("priorityHigherThanOrEquals", "3");
    queryParameters.put("priorityLowerThanOrEquals", "4");

    Response response = given()
        .queryParams(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).priorityHigherThanOrEquals(EXTERNAL_TASK_LOW_BOUND_PRIORITY);
    verify(mockQuery).priorityLowerThanOrEquals(EXTERNAL_TASK_HIGH_BOUND_PRIORITY);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    long prio1 = from(content).getLong("[0].priority");
    long prio2 = from(content).getLong("[1].priority");

    assertThat(prio1).isEqualTo(EXTERNAL_TASK_LOW_BOUND_PRIORITY);
    assertThat(prio2).isEqualTo(EXTERNAL_TASK_HIGH_BOUND_PRIORITY);
  }

  @Test
  void testQueryByPriorityListPost() {
    mockQuery = setUpMockExternalTaskQuery(createMockedExternalTasksWithPriorities());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("priorityHigherThanOrEquals", "3");
    queryParameters.put("priorityLowerThanOrEquals", "4");

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXTERNAL_TASK_QUERY_URL);

    verify(mockQuery).priorityHigherThanOrEquals(EXTERNAL_TASK_LOW_BOUND_PRIORITY);
    verify(mockQuery).priorityLowerThanOrEquals(EXTERNAL_TASK_HIGH_BOUND_PRIORITY);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    long prio1 = from(content).getLong("[0].priority");
    long prio2 = from(content).getLong("[1].priority");

    assertThat(prio1).isEqualTo(EXTERNAL_TASK_LOW_BOUND_PRIORITY);
    assertThat(prio2).isEqualTo(EXTERNAL_TASK_HIGH_BOUND_PRIORITY);
  }

  private List<ExternalTask> createMockedExternalTasksWithPriorities() {
    return Arrays.asList(
        MockProvider.mockExternalTask().priority(EXTERNAL_TASK_LOW_BOUND_PRIORITY).buildExternalTask(),
        MockProvider.mockExternalTask().priority(EXTERNAL_TASK_HIGH_BOUND_PRIORITY).buildExternalTask());
  }

  @Test
  void testQueryByExternalTaskIdListGet() {
    mockQuery = setUpMockExternalTaskQuery(createMockExternalTasksTwoIds());

    Response response = given()
        .queryParam("externalTaskIdIn", MockProvider.EXTERNAL_TASK_ID_LIST)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(EXTERNAL_TASK_QUERY_URL);

    Set<String> expectedIds = new HashSet<>();
    Collections.addAll(expectedIds, MockProvider.EXTERNAL_TASK_ID, MockProvider.EXTERNAL_TASK_ANOTHER_ID);
    verify(mockQuery).externalTaskIdIn(expectedIds);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedId1 = from(content).getString("[0].id");
    String returnedId2 = from(content).getString("[1].id");

    assertThat(returnedId1).isEqualTo(MockProvider.EXTERNAL_TASK_ID);
    assertThat(returnedId2).isEqualTo(MockProvider.EXTERNAL_TASK_ANOTHER_ID);
  }

  @Test
  void testQueryByExternalTaskIdListPost() {
    mockQuery = setUpMockExternalTaskQuery(createMockExternalTasksTwoIds());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("externalTaskIdIn", MockProvider.EXTERNAL_TASK_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(EXTERNAL_TASK_QUERY_URL);

    Set<String> expectedIds = new HashSet<>();
    Collections.addAll(expectedIds, MockProvider.EXTERNAL_TASK_ID, MockProvider.EXTERNAL_TASK_ANOTHER_ID);
    verify(mockQuery).externalTaskIdIn(expectedIds);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedId1 = from(content).getString("[0].id");
    String returnedId2 = from(content).getString("[1].id");

    assertThat(returnedId1).isEqualTo(MockProvider.EXTERNAL_TASK_ID);
    assertThat(returnedId2).isEqualTo(MockProvider.EXTERNAL_TASK_ANOTHER_ID);
  }

  private List<ExternalTask> createMockExternalTasksTwoIds() {
    return Arrays.asList(
        MockProvider.mockExternalTask().buildExternalTask(),
        MockProvider.mockExternalTask().id(MockProvider.EXTERNAL_TASK_ANOTHER_ID).buildExternalTask());
  }
}
