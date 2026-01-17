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
import java.util.List;

import java.util.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
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

public class HistoricTaskInstanceRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_TASK_INSTANCE_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/task";
  protected static final String HISTORIC_TASK_INSTANCE_COUNT_RESOURCE_URL = HISTORIC_TASK_INSTANCE_RESOURCE_URL + "/count";

  protected HistoricTaskInstanceQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockHistoricTaskInstanceQuery(MockProvider.createMockHistoricTaskInstances());
  }

  private HistoricTaskInstanceQuery setUpMockHistoricTaskInstanceQuery(List<HistoricTaskInstance> mockedHistoricTaskInstances) {
    mockedQuery = mock(HistoricTaskInstanceQuery.class);

    when(mockedQuery.list()).thenReturn(mockedHistoricTaskInstances);
    when(mockedQuery.count()).thenReturn((long) mockedHistoricTaskInstances.size());

    when(processEngine.getHistoryService().createHistoricTaskInstanceQuery()).thenReturn(mockedQuery);

    return mockedQuery;
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
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
      .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);
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
      .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityInstanceId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("activityInstanceId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricActivityInstanceId();
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
    executeAndVerifySorting("processInstanceId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processInstanceId", "desc", Status.OK);
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
    executeAndVerifySorting("duration", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricTaskInstanceDuration();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("duration", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricTaskInstanceDuration();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("endTime", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricTaskInstanceEndTime();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("endTime", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByHistoricTaskInstanceEndTime();
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
    executeAndVerifySorting("taskName", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskName();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskName", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskName();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskDescription", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskDescription();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskDescription", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskDescription();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("assignee", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskAssignee();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("assignee", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskAssignee();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("owner", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskOwner();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("owner", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskOwner();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("dueDate", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskDueDate();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("dueDate", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskDueDate();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("followUpDate", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskFollowUpDate();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("followUpDate", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskFollowUpDate();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("deleteReason", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByDeleteReason();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("deleteReason", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByDeleteReason();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskDefinitionKey", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskDefinitionKey();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskDefinitionKey", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskDefinitionKey();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("priority", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskPriority();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("priority", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskPriority();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("caseDefinitionId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByCaseDefinitionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("caseDefinitionId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByCaseDefinitionId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("caseInstanceId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByCaseInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("caseInstanceId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByCaseInstanceId();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("caseExecutionId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByCaseExecutionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("caseExecutionId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByCaseExecutionId();
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
      .orderBy("owner").desc()
      .orderBy("priority").asc()
      .getJson());
    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    inOrder.verify(mockedQuery).orderByTaskOwner();
    inOrder.verify(mockedQuery).desc();
    inOrder.verify(mockedQuery).orderByTaskPriority();
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
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(HISTORIC_TASK_INSTANCE_COUNT_RESOURCE_URL);

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
        .post(HISTORIC_TASK_INSTANCE_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testSimpleHistoricTaskInstanceQuery() {
    Response response = given()
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).as("There should be one historic task instance returned.").hasSize(1);
    assertThat(instances.get(0)).as("The returned historic task instance should not be null.").isNotNull();

    verifyHistoricTaskInstanceEntries(content);
  }

  @Test
  void testSimpleHistoricTaskInstanceQueryAsPost() {
    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(EMPTY_JSON_OBJECT)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).as("There should be one historic task instance returned.").hasSize(1);
    assertThat(instances.get(0)).as("The returned historic task instance should not be null.").isNotNull();

    verifyHistoricTaskInstanceEntries(content);
  }

  @Test
  void testQueryByTaskId() {
    String taskId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ID;

    given()
      .queryParam("taskId", taskId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskId(taskId);
  }

  @Test
  void testQueryByTaskIdAsPost() {
    String taskId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("taskId", taskId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskId(taskId);
  }

  @Test
  void testQueryByProcessInstanceId() {
    String processInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_INST_ID;

    given()
      .queryParam("processInstanceId", processInstanceId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceId(processInstanceId);
  }

  @Test
  void testQueryByProcessInstanceIdAsPost() {
    String processInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_INST_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("processInstanceId", processInstanceId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceId(processInstanceId);
  }

  @Test
  void testQueryByRootProcessInstanceId() {
    String rootProcessInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ROOT_PROC_INST_ID;

    given().queryParam("rootProcessInstanceId", rootProcessInstanceId)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).rootProcessInstanceId(rootProcessInstanceId);
  }

  @Test
  void testQueryByRootProcessInstanceIdAsPost() {
    String rootProcessInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ROOT_PROC_INST_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("rootProcessInstanceId", rootProcessInstanceId);

    given().contentType(POST_JSON_CONTENT_TYPE)
        .body(params)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).rootProcessInstanceId(rootProcessInstanceId);
  }

  @Test
  void testQueryByProcessInstanceBusinessKey() {
    String processInstanceBusinessKey = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_INST_BUSINESS_KEY;

    given()
      .queryParam("processInstanceBusinessKey", processInstanceBusinessKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceBusinessKey(processInstanceBusinessKey);
  }

  @Test
  void testQueryByProcessInstanceBusinessKeyAsPost() {
    String processInstanceBusinessKey = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_INST_BUSINESS_KEY;

    Map<String, Object> params = new HashMap<>();
    params.put("processInstanceBusinessKey", processInstanceBusinessKey);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceBusinessKey(processInstanceBusinessKey);
  }

  @Test
  void testQueryByProcessInstanceBusinessKeyLike() {
    String processInstanceBusinessKeyLike = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_INST_BUSINESS_KEY;

    given()
      .queryParam("processInstanceBusinessKeyLike", processInstanceBusinessKeyLike)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
  }

  @Test
  void testQueryByProcessInstanceBusinessKeyLikeAsPost() {
    String processInstanceBusinessKeyLike = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_INST_BUSINESS_KEY;

    Map<String, Object> params = new HashMap<>();
    params.put("processInstanceBusinessKeyLike", processInstanceBusinessKeyLike);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
  }

  @Test
  void testQueryByProcessInstanceBusinessKeyIn() {
    given()
        .queryParam("processInstanceBusinessKeyIn", String.join(",", "aBusinessKey", "anotherBusinessKey"))
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceBusinessKeyIn("aBusinessKey", "anotherBusinessKey");
  }

  @Test
  void testQueryByProcessInstanceBusinessKeyInAsPost() {
    String businessKey1 = "aBusinessKey";
    String businessKey2 = "anotherBusinessKey";
    List<String> processInstanceBusinessKeyIn = new ArrayList<>();
    processInstanceBusinessKeyIn.add(businessKey1);
    processInstanceBusinessKeyIn.add(businessKey2);

    Map<String, Object> params = new HashMap<>();
    params.put("processInstanceBusinessKeyIn", processInstanceBusinessKeyIn);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceBusinessKeyIn(businessKey1, businessKey2);
  }

  @Test
  void testQueryByExecutionId() {
    String executionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_EXEC_ID;

    given()
      .queryParam("executionId", executionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).executionId(executionId);
  }

  @Test
  void testQueryByExecutionIdAsPost() {
    String executionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_EXEC_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("executionId", executionId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).executionId(executionId);
  }

  @Test
  void testQueryByActivityInstanceId() {
    String activityInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ACT_INST_ID;

    given()
      .queryParam("activityInstanceIdIn", activityInstanceId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).activityInstanceIdIn(activityInstanceId);
  }

  @Test
  void testQueryByActivityInstanceIdAsPost() {
    String activityInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ACT_INST_ID;

    List<String> activityInstanceIds = new ArrayList<>();
    activityInstanceIds.add(activityInstanceId);

    Map<String, Object> params = new HashMap<>();
    params.put("activityInstanceIdIn", activityInstanceIds);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).activityInstanceIdIn(activityInstanceId);
  }

  @Test
  void testQueryByActivityInstanceIds() {
    String activityInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ACT_INST_ID;
    String anotherActivityInstanceId = "anotherActivityInstanceId";

    given()
      .queryParam("activityInstanceIdIn", activityInstanceId + "," + anotherActivityInstanceId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).activityInstanceIdIn(activityInstanceId, anotherActivityInstanceId);
  }

  @Test
  void testQueryByActivityInstanceIdsAsPost() {
    String activityInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ACT_INST_ID;
    String anotherActivityInstanceId = "anotherActivityInstanceId";

    List<String> activityInstanceIds = new ArrayList<>();
    activityInstanceIds.add(activityInstanceId);
    activityInstanceIds.add(anotherActivityInstanceId);

    Map<String, Object> params = new HashMap<>();
    params.put("activityInstanceIdIn", activityInstanceIds);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).activityInstanceIdIn(activityInstanceId, anotherActivityInstanceId);
  }

  @Test
  void testQueryByProcessDefinitionId() {
    String processDefinitionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_DEF_ID;

    given()
      .queryParam("processDefinitionId", processDefinitionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionId(processDefinitionId);
  }

  @Test
  void testQueryByProcessDefinitionIdAsPost() {
    String processDefinitionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_DEF_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("processDefinitionId", processDefinitionId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionId(processDefinitionId);
  }

  @Test
  void testQueryByProcessDefinitionKey() {
    String processDefinitionKey = "aProcDefKey";

    given()
      .queryParam("processDefinitionKey", processDefinitionKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionKey(processDefinitionKey);
  }

  @Test
  void testQueryByProcessDefinitionKeyAsPost() {
    String processDefinitionKey = "aProcDefKey";

    Map<String, Object> params = new HashMap<>();
    params.put("processDefinitionKey", processDefinitionKey);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionKey(processDefinitionKey);
  }

  @Test
  void testQueryByProcessDefinitionName() {
    String processDefinitionName = "aProcDefName";

    given()
      .queryParam("processDefinitionName", processDefinitionName)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionName(processDefinitionName);
  }

  @Test
  void testQueryByProcessDefinitionNameAsPost() {
    String processDefinitionName = "aProcDefName";

    Map<String, Object> params = new HashMap<>();
    params.put("processDefinitionName", processDefinitionName);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionName(processDefinitionName);
  }

  @Test
  void testQueryByTaskName() {
    String taskName = "aTaskName";

    given()
      .queryParam("taskName", taskName)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskName(taskName);
  }

  @Test
  void testQueryByTaskNameAsPost() {
    String taskName = "aTaskName";

    Map<String, Object> params = new HashMap<>();
    params.put("taskName", taskName);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskName(taskName);
  }

  @Test
  void testQueryByTaskNameLike() {
    String taskNameLike = "aTaskNameLike";

    given()
      .queryParam("taskNameLike", taskNameLike)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskNameLike(taskNameLike);
  }

  @Test
  void testQueryByTaskNameLikeAsPost() {
    String taskNameLike = "aTaskNameLike";

    Map<String, Object> params = new HashMap<>();
    params.put("taskNameLike", taskNameLike);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskNameLike(taskNameLike);
  }

  @Test
  void testQueryByTaskDescription() {
    String taskDescription = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DESCRIPTION;

    given()
      .queryParam("taskDescription", taskDescription)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDescription(taskDescription);
  }

  @Test
  void testQueryByTaskDescriptionAsPost() {
    String taskDescription = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DESCRIPTION;

    Map<String, Object> params = new HashMap<>();
    params.put("taskDescription", taskDescription);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDescription(taskDescription);
  }

  @Test
  void testQueryByTaskDescriptionLike() {
    String taskDescriptionLike = "aTaskDescriptionLike";

    given()
      .queryParam("taskDescriptionLike", taskDescriptionLike)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDescriptionLike(taskDescriptionLike);
  }

  @Test
  void testQueryByTaskDescriptionLikeAsPost() {
    String taskDescriptionLike = "aTaskDescriptionLike";

    Map<String, Object> params = new HashMap<>();
    params.put("taskDescriptionLike", taskDescriptionLike);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDescriptionLike(taskDescriptionLike);
  }

  @Test
  void testQueryByTaskDefinitionKey() {
    String taskDefinitionKey = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DEF_KEY;

    given()
      .queryParam("taskDefinitionKey", taskDefinitionKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDefinitionKey(taskDefinitionKey);
  }

  @Test
  void testQueryByTaskDefinitionKeyAsPost() {
    String taskDefinitionKey = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DEF_KEY;

    Map<String, Object> params = new HashMap<>();
    params.put("taskDefinitionKey", taskDefinitionKey);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDefinitionKey(taskDefinitionKey);
  }

  @Test
  void testQueryByTaskDeleteReason() {
    String taskDeleteReason = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DELETE_REASON;

    given()
      .queryParam("taskDeleteReason", taskDeleteReason)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDeleteReason(taskDeleteReason);
  }

  @Test
  void testQueryByTaskDeleteReasonAsPost() {
    String taskDeleteReason = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DELETE_REASON;

    Map<String, Object> params = new HashMap<>();
    params.put("taskDeleteReason", taskDeleteReason);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDeleteReason(taskDeleteReason);
  }

  @Test
  void testQueryByTaskDeleteReasonLike() {
    String taskDeleteReasonLike = "aTaskDeleteReasonLike";

    given()
      .queryParam("taskDeleteReasonLike", taskDeleteReasonLike)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDeleteReasonLike(taskDeleteReasonLike);
  }

  @Test
  void testQueryByTaskDeleteReasonLikeAsPost() {
    String taskDeleteReasonLike = "aTaskDeleteReasonLike";

    Map<String, Object> params = new HashMap<>();
    params.put("taskDeleteReasonLike", taskDeleteReasonLike);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDeleteReasonLike(taskDeleteReasonLike);
  }

  @Test
  void testQueryByTaskAssignee() {
    String taskAssignee = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ASSIGNEE;

    given()
      .queryParam("taskAssignee", taskAssignee)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskAssignee(taskAssignee);
  }

  @Test
  void testQueryByTaskAssigneeAsPost() {
    String taskAssignee = MockProvider.EXAMPLE_HISTORIC_TASK_INST_ASSIGNEE;

    Map<String, Object> params = new HashMap<>();
    params.put("taskAssignee", taskAssignee);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskAssignee(taskAssignee);
  }

  @Test
  void testQueryByTaskAssigneeLike() {
    String taskAssigneeLike = "aTaskAssigneeLike";

    given()
      .queryParam("taskAssigneeLike", taskAssigneeLike)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskAssigneeLike(taskAssigneeLike);
  }

  @Test
  void testQueryByTaskAssigneeLikeAsPost() {
    String taskAssigneeLike = "aTaskAssigneeLike";

    Map<String, Object> params = new HashMap<>();
    params.put("taskAssigneeLike", taskAssigneeLike);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskAssigneeLike(taskAssigneeLike);
  }

  @Test
  void testQueryByTaskOwner() {
    String taskOwner = MockProvider.EXAMPLE_HISTORIC_TASK_INST_OWNER;

    given()
      .queryParam("taskOwner", taskOwner)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskOwner(taskOwner);
  }

  @Test
  void testQueryByTaskOwnerAsPost() {
    String taskOwner = MockProvider.EXAMPLE_HISTORIC_TASK_INST_OWNER;

    Map<String, Object> params = new HashMap<>();
    params.put("taskOwner", taskOwner);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskOwner(taskOwner);
  }

  @Test
  void testQueryByTaskOwnerLike() {
    String taskOwnerLike = "aTaskOwnerLike";

    given()
      .queryParam("taskOwnerLike", taskOwnerLike)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskOwnerLike(taskOwnerLike);
  }

  @Test
  void testQueryByTaskOwnerLikeAsPost() {
    String taskOwnerLike = "aTaskOwnerLike";

    Map<String, Object> params = new HashMap<>();
    params.put("taskOwnerLike", taskOwnerLike);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskOwnerLike(taskOwnerLike);
  }

  @Test
  void testQueryByTaskPriority() {
    int taskPriority = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PRIORITY;

    given()
      .queryParam("taskPriority", taskPriority)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskPriority(taskPriority);
  }

  @Test
  void testQueryByTaskPriorityAsPost() {
    int taskPriority = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PRIORITY;

    Map<String, Object> params = new HashMap<>();
    params.put("taskPriority", taskPriority);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskPriority(taskPriority);
  }

  @Test
  void testQueryByAssigned() {
    given()
      .queryParam("assigned", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskAssigned();
  }

  @Test
  void testQueryByAssignedAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("assigned", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskAssigned();
  }

  @Test
  void testQueryByWithCandidateGroups() {
    given()
      .queryParam("withCandidateGroups", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).withCandidateGroups();
  }

  @Test
  void testQueryByWithCandidateGroupsAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("withCandidateGroups", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).withCandidateGroups();
  }

  @Test
  void testQueryByWithoutCandidateGroups() {
    given()
      .queryParam("withoutCandidateGroups", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).withoutCandidateGroups();
  }

  @Test
  void testQueryByWithoutCandidateGroupsAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("withoutCandidateGroups", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).withoutCandidateGroups();
  }

  @Test
  void testQueryByUnassigned() {
    given()
      .queryParam("unassigned", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskUnassigned();
  }

  @Test
  void testQueryByUnassignedAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("unassigned", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskUnassigned();
  }

  @Test
  void testQueryByFinished() {
    given()
      .queryParam("finished", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).finished();
  }

  @Test
  void testQueryByFinishedAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("finished", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).finished();
  }

  @Test
  void testQueryByUnfinished() {
    given()
      .queryParam("unfinished", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).unfinished();
  }

  @Test
  void testQueryByUnfinishedAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("unfinished", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).unfinished();
  }

  @Test
  void testQueryByProcessFinished() {
    given()
      .queryParam("processFinished", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processFinished();
  }

  @Test
  void testQueryByProcessFinishedAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("processFinished", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processFinished();
  }

  @Test
  void testQueryByProcessUnfinished() {
    given()
      .queryParam("processUnfinished", true)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processUnfinished();
  }

  @Test
  void testQueryByProcessUnfinishedAsPost() {
    Map<String, Object> params = new HashMap<>();
    params.put("processUnfinished", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processUnfinished();
  }

  @Test
  void testQueryByTaskParentTaskId() {
    String taskParentTaskId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PARENT_TASK_ID;

    given()
      .queryParam("taskParentTaskId", taskParentTaskId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskParentTaskId(taskParentTaskId);
  }

  @Test
  void testQueryByTaskParentTaskIdAsPost() {
    String taskParentTaskId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_PARENT_TASK_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("taskParentTaskId", taskParentTaskId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskParentTaskId(taskParentTaskId);
  }

  @Test
  void testQueryByTaskDueDate() {
    String due = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DUE_DATE;

    given()
      .queryParam("taskDueDate", due)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDueDate(DateTimeUtil.parseDate(due));
  }

  @Test
  void testQueryByTaskDueDateAsPost() {
    String due = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DUE_DATE;

    Map<String, Object> params = new HashMap<>();
    params.put("taskDueDate", DateTimeUtil.parseDate(due));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDueDate(DateTimeUtil.parseDate(due));
  }

  @Test
  void testQueryByTaskDueDateBefore() {
    String due = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DUE_DATE;

    given()
      .queryParam("taskDueDateBefore", due)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDueBefore(DateTimeUtil.parseDate(due));
  }

  @Test
  void testQueryByTaskDueDateBeforeAsPost() {
    String due = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DUE_DATE;

    Map<String, Object> params = new HashMap<>();
    params.put("taskDueDateBefore", DateTimeUtil.parseDate(due));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDueBefore(DateTimeUtil.parseDate(due));
  }

  @Test
  void testQueryByTaskDueDateAfter() {
    String due = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DUE_DATE;

    given()
      .queryParam("taskDueDateAfter", due)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDueAfter(DateTimeUtil.parseDate(due));
  }

  @Test
  void testQueryByTaskDueDateAfterAsPost() {
    String due = MockProvider.EXAMPLE_HISTORIC_TASK_INST_DUE_DATE;

    Map<String, Object> params = new HashMap<>();
    params.put("taskDueDateAfter", DateTimeUtil.parseDate(due));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDueAfter(DateTimeUtil.parseDate(due));
  }

  @Test
  void testQueryWithoutTaskDueDateQueryParameter() {
    // given
    mockedQuery = setUpMockHistoricTaskInstanceQuery(Collections.singletonList(
        MockProvider.createMockHistoricTaskInstance(MockProvider.EXAMPLE_TENANT_ID, null)));

    // when
    Response response = given()
          .queryParam("withoutTaskDueDate", true)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTaskDueDate();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTaskDueDate = from(content).getString("[0].due");
    assertThat(returnedTaskDueDate).isNull();
  }

  @Test
  void testQueryWithoutTaskDueDatePostParameter() {
    // given
    mockedQuery = setUpMockHistoricTaskInstanceQuery(Collections.singletonList(
        MockProvider.createMockHistoricTaskInstance(MockProvider.EXAMPLE_TENANT_ID, null)));
    Map<String, Object> queryParameters = Collections.singletonMap("withoutTaskDueDate", (Object) true);

    // when
    Response response = given()
          .contentType(POST_JSON_CONTENT_TYPE)
          .body(queryParameters)
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTaskDueDate();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTaskDueDate = from(content).getString("[0].due");
    assertThat(returnedTaskDueDate).isNull();
  }

  @Test
  void testQueryByTaskFollowUpDate() {
    String followUp = MockProvider.EXAMPLE_HISTORIC_TASK_INST_FOLLOW_UP_DATE;

    given()
    .queryParam("taskFollowUpDate", followUp)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskFollowUpDate(DateTimeUtil.parseDate(followUp));
  }

  @Test
  void testQueryByTaskFollowUpDateAsPost() {
    String followUp = MockProvider.EXAMPLE_HISTORIC_TASK_INST_FOLLOW_UP_DATE;

    Map<String, Object> params = new HashMap<>();
    params.put("taskFollowUpDate", DateTimeUtil.parseDate(followUp));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskFollowUpDate(DateTimeUtil.parseDate(followUp));
  }

  @Test
  void testQueryByTaskFollowUpDateBefore() {
    String followUp = MockProvider.EXAMPLE_HISTORIC_TASK_INST_FOLLOW_UP_DATE;

    given()
      .queryParam("taskFollowUpDateBefore", followUp)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskFollowUpBefore(DateTimeUtil.parseDate(followUp));
  }

  @Test
  void testQueryByTaskFollowUpDateBeforeAsPost() {
    String followUp = MockProvider.EXAMPLE_HISTORIC_TASK_INST_FOLLOW_UP_DATE;

    Map<String, Object> params = new HashMap<>();
    params.put("taskFollowUpDateBefore", DateTimeUtil.parseDate(followUp));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskFollowUpBefore(DateTimeUtil.parseDate(followUp));
  }

  @Test
  void testQueryByTaskFollowUpDateAfter() {
    String followUp = MockProvider.EXAMPLE_HISTORIC_TASK_INST_FOLLOW_UP_DATE;

    given()
      .queryParam("taskFollowUpDateAfter", followUp)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskFollowUpAfter(DateTimeUtil.parseDate(followUp));
  }

  @Test
  void testQueryByTaskFollowUpDateAfterAsPost() {
    String followUp = MockProvider.EXAMPLE_HISTORIC_TASK_INST_FOLLOW_UP_DATE;

    Map<String, Object> params = new HashMap<>();
    params.put("taskFollowUpDateAfter", DateTimeUtil.parseDate(followUp));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskFollowUpAfter(DateTimeUtil.parseDate(followUp));
  }

  @Test
  void testQueryByStartedBefore() {
      String startedBefore = MockProvider.EXAMPLE_HISTORIC_TASK_INST_START_TIME;

      given()
        .queryParam("startedBefore", startedBefore)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).startedBefore(DateTimeUtil.parseDate(startedBefore));
    }

  @Test
  void testQueryByStartedBeforeWithInvalidParameter() {
    String startedBefore = "\"pizza\"";

    given()
      .queryParam("startedBefore", startedBefore)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON).body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body(
        "message",
        containsString("Cannot convert value " + startedBefore))
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

  }

  @Test
  void testQueryByStartedBeforeAsPost() {
      String startedBefore = MockProvider.EXAMPLE_HISTORIC_TASK_INST_START_TIME;

      Map<String, Object> params = new HashMap<>();
      params.put("startedBefore", DateTimeUtil.parseDate(startedBefore));

      given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(params)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).startedBefore(DateTimeUtil.parseDate(startedBefore));
    }


  @Test
  void testQueryByStartedAfter() {
      String startedAfter = MockProvider.EXAMPLE_HISTORIC_TASK_INST_START_TIME;

      given()
        .queryParam("startedAfter", startedAfter)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).startedAfter(DateTimeUtil.parseDate(startedAfter));
    }

  @Test
  void testQueryByStartedAfterAsPost() {
      String startedAfter = MockProvider.EXAMPLE_HISTORIC_TASK_INST_START_TIME;

      Map<String, Object> params = new HashMap<>();
      params.put("startedAfter", DateTimeUtil.parseDate(startedAfter));

      given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(params)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).startedAfter(DateTimeUtil.parseDate(startedAfter));
    }


  @Test
  void testQueryByFinishedBefore() {
      String finishedBefore = MockProvider.EXAMPLE_HISTORIC_TASK_INST_END_TIME;

      given()
        .queryParam("finishedBefore", finishedBefore)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).finishedBefore(DateTimeUtil.parseDate(finishedBefore));
    }

  @Test
  void testQueryByFinishedBeforeAsPost() {
      String finishedBefore = MockProvider.EXAMPLE_HISTORIC_TASK_INST_END_TIME;

      Map<String, Object> params = new HashMap<>();
      params.put("finishedBefore", DateTimeUtil.parseDate(finishedBefore));

      given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(params)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).finishedBefore(DateTimeUtil.parseDate(finishedBefore));
    }


  @Test
  void testQueryByFinishedAfter() {
      String finishedAfter = MockProvider.EXAMPLE_HISTORIC_TASK_INST_END_TIME;

      given()
        .queryParam("finishedAfter", finishedAfter)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).finishedAfter(DateTimeUtil.parseDate(finishedAfter));
    }

  @Test
  void testQueryByFinishedAfterAsPost() {
      String finishedAfter = MockProvider.EXAMPLE_HISTORIC_TASK_INST_END_TIME;

      Map<String, Object> params = new HashMap<>();
      params.put("finishedAfter", DateTimeUtil.parseDate(finishedAfter));

      given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(params)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).finishedAfter(DateTimeUtil.parseDate(finishedAfter));
    }

  @Test
  void testQueryByTaskVariable() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_eq_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("taskVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskVariableValueEquals(variableName, variableValue);
  }

  @Test
  void testQueryByTaskVariableValueIgnoreCase() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_eq_" + variableValue;

    String queryValue = variableParameter;

    given()
    .queryParam("taskVariables", queryValue)
    .queryParam("variableValuesIgnoreCase", true)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).matchVariableValuesIgnoreCase();
    verify(mockedQuery).taskVariableValueEquals(variableName, variableValue);
  }

  @Test
  void testQueryByTaskVariableNameIgnoreCase() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_eq_" + variableValue;

    String queryValue = variableParameter;

    given()
    .queryParam("taskVariables", queryValue)
    .queryParam("variableNamesIgnoreCase", true)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).matchVariableNamesIgnoreCase();
    verify(mockedQuery).taskVariableValueEquals(variableName, variableValue);
  }

  @Test
  void testQueryByTaskVariableNameValueIgnoreCase() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_eq_" + variableValue;

    String queryValue = variableParameter;

    given()
    .queryParam("taskVariables", queryValue)
    .queryParam("variableNamesIgnoreCase", true)
    .queryParam("variableValuesIgnoreCase", true)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).matchVariableNamesIgnoreCase();
    verify(mockedQuery).matchVariableValuesIgnoreCase();
    verify(mockedQuery).taskVariableValueEquals(variableName, variableValue);
  }

  @Test
  void testQueryByTaskVariableAsPost() {
    String variableName = "varName";
    String variableValue = "varValue";

    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", variableName);
    variableJson.put("operator", "eq");
    variableJson.put("value", variableValue);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> params = new HashMap<>();
    params.put("taskVariables", variables);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskVariableValueEquals(variableName, variableValue);
  }

  @Test
  void testQueryByInvalidTaskVariable() {
    // invalid comparator
    String invalidComparator = "anInvalidComparator";
    String variableName = "varName";
    String variableValue = "varValue";
    String queryValue = variableName + "_" + invalidComparator + "_" + variableValue;

    given()
      .queryParam("taskVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Invalid variable comparator specified: " + invalidComparator))
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    // invalid format
    queryValue = "invalidFormattedVariableQuery";

    given()
      .queryParam("taskVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("variable query parameter has to have format KEY_OPERATOR_VALUE"))
      .when()
      .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testQueryByInvalidTaskVariableAsPost() {
    // invalid comparator
    String invalidComparator = "anInvalidComparator";
    String variableName = "varName";
    String variableValue = "varValue";

    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", variableName);
    variableJson.put("operator", invalidComparator);
    variableJson.put("value", variableValue);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> params = new HashMap<>();
    params.put("taskVariables", variables);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Invalid variable comparator specified: " + invalidComparator))
      .when()
        .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testQueryByProcessVariableEquals() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_eq_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueEquals(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableGreaterThan() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_gt_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueGreaterThan(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableGreaterThanEquals() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_gteq_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueGreaterThanOrEquals(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableLessThan() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_lt_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueLessThan(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableLessThanEquals() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_lteq_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueLessThanOrEquals(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableLike() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_like_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueLike(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableNotLike() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_notLike_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueNotLike(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableNotEquals() {
    String variableName = "varName";
    String variableValue = "varValue";
    String variableParameter = variableName + "_neq_" + variableValue;

    String queryValue = variableParameter;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueNotEquals(variableName, variableValue);
  }

  @Test
  void testQueryByProcessVariableAsPost() {
    String variableName = "varName";
    String variableValue = "varValue";

    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", variableName);
    variableJson.put("operator", "eq");
    variableJson.put("value", variableValue);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> params = new HashMap<>();
    params.put("processVariables", variables);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processVariableValueEquals(variableName, variableValue);
  }

  @Test
  void testQueryByInvalidProcessVariable() {
    // invalid comparator
    String invalidComparator = "anInvalidComparator";
    String variableName = "varName";
    String variableValue = "varValue";
    String queryValue = variableName + "_" + invalidComparator + "_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Invalid process variable comparator specified: " + invalidComparator))
      .when()
        .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    // invalid format
    queryValue = "invalidFormattedVariableQuery";

    given()
      .queryParam("processVariables", queryValue)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("variable query parameter has to have format KEY_OPERATOR_VALUE"))
      .when()
      .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testQueryByInvalidProcessVariableAsPost() {
    // invalid comparator
    String invalidComparator = "anInvalidComparator";
    String variableName = "varName";
    String variableValue = "varValue";

    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", variableName);
    variableJson.put("operator", invalidComparator);
    variableJson.put("value", variableValue);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> params = new HashMap<>();
    params.put("processVariables", variables);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Invalid process variable comparator specified: " + invalidComparator))
      .when()
        .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testQueryByCaseDefinitionId() {
    String caseDefinitionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_DEF_ID;

    given()
      .queryParam("caseDefinitionId", caseDefinitionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseDefinitionId(caseDefinitionId);
  }

  @Test
  void testQueryByCaseDefinitionIdAsPost() {
    String caseDefinitionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_DEF_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("caseDefinitionId", caseDefinitionId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseDefinitionId(caseDefinitionId);
  }

  @Test
  void testQueryByCaseDefinitionKey() {
    String caseDefinitionKey = "aCaseDefKey";

    given()
      .queryParam("caseDefinitionKey", caseDefinitionKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseDefinitionKey(caseDefinitionKey);
  }

  @Test
  void testQueryByCaseDefinitionKeyAsPost() {
    String caseDefinitionKey = "aCaseDefKey";

    Map<String, Object> params = new HashMap<>();
    params.put("caseDefinitionKey", caseDefinitionKey);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseDefinitionKey(caseDefinitionKey);
  }

  @Test
  void testQueryByCaseDefinitionName() {
    String caseDefinitionName = "aCaseDefName";

    given()
      .queryParam("caseDefinitionName", caseDefinitionName)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseDefinitionName(caseDefinitionName);
  }

  @Test
  void testQueryByCaseDefinitionNameAsPost() {
    String caseDefinitionName = "aCaseDefName";

    Map<String, Object> params = new HashMap<>();
    params.put("caseDefinitionName", caseDefinitionName);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseDefinitionName(caseDefinitionName);
  }

  @Test
  void testQueryByCaseInstanceId() {
    String caseInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_INST_ID;

    given()
      .queryParam("caseInstanceId", caseInstanceId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseInstanceId(caseInstanceId);
  }

  @Test
  void testQueryByCaseInstanceIdAsPost() {
    String caseInstanceId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_INST_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("caseInstanceId", caseInstanceId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseInstanceId(caseInstanceId);
  }

  @Test
  void testQueryByCaseExecutionId() {
    String caseExecutionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_EXEC_ID;

    given()
      .queryParam("caseExecutionId", caseExecutionId)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseExecutionId(caseExecutionId);
  }

  @Test
  void testQueryByCaseExecutionIdAsPost() {
    String caseExecutionId = MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_EXEC_ID;

    Map<String, Object> params = new HashMap<>();
    params.put("caseExecutionId", caseExecutionId);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseExecutionId(caseExecutionId);
  }

  @Test
  void testTenantIdListParameter() {
    mockedQuery = setUpMockHistoricTaskInstanceQuery(createMockHistoricTaskInstancesTwoTenants());

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
    mockedQuery = setUpMockHistoricTaskInstanceQuery(createMockHistoricTaskInstancesTwoTenants());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
  void testQueryWithoutTenantIdQueryParameter() {
    // given
    mockedQuery = setUpMockHistoricTaskInstanceQuery(Collections.singletonList(MockProvider.createMockHistoricTaskInstance(null)));

    // when
    Response response = given()
          .queryParam("withoutTenantId", true)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
    mockedQuery = setUpMockHistoricTaskInstanceQuery(Collections.singletonList(MockProvider.createMockHistoricTaskInstance(null)));
    Map<String, Object> queryParameters = Collections.singletonMap("withoutTenantId", (Object) true);

    // when
    Response response = given()
          .contentType(POST_JSON_CONTENT_TYPE)
          .body(queryParameters)
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

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
  void testQueryTaskInvolvedUser() {
    String taskInvolvedUser = MockProvider.EXAMPLE_HISTORIC_TASK_INST_TASK_INVOLVED_USER;
    given()
      .queryParam("taskInvolvedUser", taskInvolvedUser)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskInvolvedUser(taskInvolvedUser);
  }

  @Test
  void testQueryTaskInvolvedGroup() {
    String taskInvolvedGroup = MockProvider.EXAMPLE_HISTORIC_TASK_INST_TASK_INVOLVED_GROUP;
    given()
      .queryParam("taskInvolvedGroup", taskInvolvedGroup)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskInvolvedGroup(taskInvolvedGroup);
  }

  @Test
  void testQueryTaskHadCandidateUser() {
    String taskHadCandidateUser = MockProvider.EXAMPLE_HISTORIC_TASK_INST_TASK_HAD_CANDIDATE_USER;
    given()
      .queryParam("taskHadCandidateUser", taskHadCandidateUser)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskHadCandidateUser(taskHadCandidateUser);
  }

  @Test
  void testQueryTaskHadCandidateGroup() {
    String taskHadCandidateGroup = MockProvider.EXAMPLE_HISTORIC_TASK_INST_TASK_HAD_CANDIDATE_GROUP;
    given()
      .queryParam("taskHadCandidateGroup", taskHadCandidateGroup)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskHadCandidateGroup(taskHadCandidateGroup);
  }

  @Test
  void testQueryByTaskDefinitionKeyIn() {

    String taskDefinitionKey1 = "aTaskDefinitionKey";
    String taskDefinitionKey2 = "anotherTaskDefinitionKey";

    given()
      .queryParam("taskDefinitionKeyIn", taskDefinitionKey1 + "," + taskDefinitionKey2)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDefinitionKeyIn(taskDefinitionKey1, taskDefinitionKey2);
    verify(mockedQuery).list();
  }

  @Test
  void testQueryByTaskDefinitionKeyInAsPost() {

    String taskDefinitionKey1 = "aTaskDefinitionKey";
    String taskDefinitionKey2 = "anotherTaskDefinitionKey";

    List<String> taskDefinitionKeys = new ArrayList<>();
    taskDefinitionKeys.add(taskDefinitionKey1);
    taskDefinitionKeys.add(taskDefinitionKey2);

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("taskDefinitionKeyIn", taskDefinitionKeys);

    given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).taskDefinitionKeyIn(taskDefinitionKey1, taskDefinitionKey2);
    verify(mockedQuery).list();
  }

  @Test
  void testOrQuery() {
    // given
    HistoricTaskInstanceQueryImpl historicTaskInstanceQuery = mock(HistoricTaskInstanceQueryImpl.class);
    when(processEngine.getHistoryService().createHistoricTaskInstanceQuery()).thenReturn(historicTaskInstanceQuery);

    String payload = "{ \"orQueries\": [{" +
        "\"processDefinitionKey\": \"aKey\", " +
        "\"processInstanceBusinessKey\": \"aBusinessKey\"}] }";

    // when
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .header(ACCEPT_JSON_HEADER)
      .body(payload)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_TASK_INSTANCE_RESOURCE_URL);

    ArgumentCaptor<HistoricTaskInstanceQueryImpl> argument =
        ArgumentCaptor.forClass(HistoricTaskInstanceQueryImpl.class);

    verify(historicTaskInstanceQuery).addOrQuery(argument.capture());

    // then
    assertThat(argument.getValue().getProcessDefinitionKey()).isEqualTo("aKey");
    assertThat(argument.getValue().getProcessInstanceBusinessKey()).isEqualTo("aBusinessKey");
  }

  private List<HistoricTaskInstance> createMockHistoricTaskInstancesTwoTenants() {
    return List.of(
        MockProvider.createMockHistoricTaskInstance(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockHistoricTaskInstance(MockProvider.ANOTHER_EXAMPLE_TENANT_ID));
  }

  protected void verifyHistoricTaskInstanceEntries(String content) {
    String returnedId = from(content).getString("[0].id");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedExecutionId = from(content).getString("[0].executionId");
    String returnedActivityInstanceId = from(content).getString("[0].activityInstanceId");
    String returnedName = from(content).getString("[0].name");
    String returnedDescription = from(content).getString("[0].description");
    String returnedDeleteReason = from(content).getString("[0].deleteReason");
    String returnedOwner = from(content).getString("[0].owner");
    String returnedAssignee = from(content).getString("[0].assignee");
    Date returnedStartTime = DateTimeUtil.parseDate(from(content).getString("[0].startTime"));
    Date returnedEndTime = DateTimeUtil.parseDate(from(content).getString("[0].endTime"));
    Long returnedDurationInMillis = from(content).getLong("[0].duration");
    String returnedTaskDefinitionKey = from(content).getString("[0].taskDefinitionKey");
    int returnedPriority = from(content).getInt("[0].priority");
    String returnedParentTaskId = from(content).getString("[0].parentTaskId");
    Date returnedDue = DateTimeUtil.parseDate(from(content).getString("[0].due"));
    Date returnedFollow = DateTimeUtil.parseDate(from(content).getString("[0].followUp"));
    String returnedCaseDefinitionKey = from(content).getString("[0].caseDefinitionKey");
    String returnedCaseDefinitionId = from(content).getString("[0].caseDefinitionId");
    String returnedCaseInstanceId = from(content).getString("[0].caseInstanceId");
    String returnedCaseExecutionId = from(content).getString("[0].caseExecutionId");
    String returnedTenantId = from(content).getString("[0].tenantId");
    String returnedTaskState = from(content).getString("[0].taskState");
    Date returnedRemovalTime = DateTimeUtil.parseDate(from(content).getString("[0].removalTime"));
    String returnedRootProcessInstanceId = from(content).getString("[0].rootProcessInstanceId");

    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_INST_ID);
    assertThat(returnedActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_ACT_INST_ID);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_EXEC_ID);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_DEF_ID);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_PROC_DEF_KEY);
    assertThat(returnedName).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_NAME);
    assertThat(returnedDescription).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_DESCRIPTION);
    assertThat(returnedDeleteReason).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_DELETE_REASON);
    assertThat(returnedOwner).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_OWNER);
    assertThat(returnedAssignee).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_ASSIGNEE);
    assertThat(returnedStartTime).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_TASK_INST_START_TIME));
    assertThat(returnedEndTime).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_TASK_INST_END_TIME));
    assertThat(returnedDurationInMillis).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_DURATION);
    assertThat(returnedTaskDefinitionKey).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_DEF_KEY);
    assertThat(returnedPriority).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_PRIORITY);
    assertThat(returnedDue).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_TASK_INST_DUE_DATE));
    assertThat(returnedFollow).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_TASK_INST_FOLLOW_UP_DATE));
    assertThat(returnedParentTaskId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_PARENT_TASK_ID);
    assertThat(returnedCaseDefinitionKey).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_DEF_KEY);
    assertThat(returnedCaseDefinitionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_DEF_ID);
    assertThat(returnedCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_INST_ID);
    assertThat(returnedCaseExecutionId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_CASE_EXEC_ID);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedRemovalTime).isEqualTo(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HISTORIC_TASK_INST_REMOVAL_TIME));
    assertThat(returnedRootProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_INST_ROOT_PROC_INST_ID);
    assertThat(returnedTaskState).isEqualTo(MockProvider.EXAMPLE_HISTORIC_TASK_STATE);
  }

}
