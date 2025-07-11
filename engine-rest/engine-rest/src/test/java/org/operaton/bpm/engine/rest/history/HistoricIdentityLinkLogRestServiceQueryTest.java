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

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLogQuery;
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

import static io.restassured.RestAssured.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
public class HistoricIdentityLinkLogRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORY_IDENTITY_LINK_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/history/identity-link-log";
  protected static final String HISTORY_IDENTITY_LINK_COUNT_QUERY_URL = HISTORY_IDENTITY_LINK_QUERY_URL + "/count";

  private HistoricIdentityLinkLogQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockHistoricIdentityLinkQuery(MockProvider.createMockHistoricIdentityLinks());
  }

  private HistoricIdentityLinkLogQuery setUpMockHistoricIdentityLinkQuery(List<HistoricIdentityLinkLog> mockedHistoricIdentityLinks) {

    HistoricIdentityLinkLogQuery mockedHistoricIdentityLinkQuery = mock(HistoricIdentityLinkLogQuery.class);
    when(mockedHistoricIdentityLinkQuery.list()).thenReturn(mockedHistoricIdentityLinks);
    when(mockedHistoricIdentityLinkQuery.count()).thenReturn((long) mockedHistoricIdentityLinks.size());

    when(processEngine.getHistoryService().createHistoricIdentityLinkLogQuery()).thenReturn(mockedHistoricIdentityLinkQuery);

    return mockedHistoricIdentityLinkQuery;
  }

  @Test
  void testEmptyQuery() {
    String queryKey = "";
    given().queryParam("userId", queryKey).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("processInstanceId", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  @Test
  void testQueryWithoutTenantIdQueryParameter() {
    // given
    mockedQuery = setUpMockHistoricIdentityLinkQuery(Collections.singletonList(MockProvider.createMockHistoricIdentityLink(null)));

    // when
    Response response = given()
        .queryParam("withoutTenantId", true)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(HISTORY_IDENTITY_LINK_QUERY_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId).isNull();
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given().queryParam("sortBy", sortBy).queryParam("sortOrder", sortOrder).then().expect().statusCode(expectedStatus.getStatusCode()).when()
        .get(HISTORY_IDENTITY_LINK_QUERY_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given().queryParam("sortOrder", "asc").then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required")).when()
        .get(HISTORY_IDENTITY_LINK_QUERY_URL);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);

    // assignerId
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("assignerId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByAssignerId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("assignerId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByAssignerId();
    inOrder.verify(mockedQuery).desc();

    // userId
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("userId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByUserId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("userId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByUserId();
    inOrder.verify(mockedQuery).desc();

    // groupId
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("groupId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByGroupId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("groupId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByGroupId();
    inOrder.verify(mockedQuery).desc();

    // taskId
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("taskId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTaskId();
    inOrder.verify(mockedQuery).desc();

    // operationType
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("operationType", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByOperationType();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("operationType", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByOperationType();
    inOrder.verify(mockedQuery).desc();

    // processDefinitionId
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).desc();

    // processDefinitionKey
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionKey", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("processDefinitionKey", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockedQuery).desc();

    // type
    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("type", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByType();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("type", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByType();
    inOrder.verify(mockedQuery).desc();

    // tenantId
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
  void testSimpleHistoricIdentityLinkQuery() {
    Response response = given().then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> identityLinks = from(content).getList("");
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(identityLinks.get(0)).as("The returned incident should not be null.").isNotNull();

    String returnedAssignerId = from(content).getString("[0].assignerId");
    String returnedTenantId = from(content).getString("[0].tenantId");
    String returnedUserId = from(content).getString("[0].userId");
    String returnedGroupId = from(content).getString("[0].groupId");
    String returnedTaskId = from(content).getString("[0].taskId");
    String returnedType = from(content).getString("[0].type");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String returnedOperationType = from(content).getString("[0].operationType");
    Date loggedDate = DateTimeUtil.parseDate(from(content).getString("[0].time"));
    Date returnedRemovalTime = DateTimeUtil.parseDate(from(content).getString("[0].removalTime"));
    String returnedRootProcessInstanceId = from(content).getString("[0].rootProcessInstanceId");

    Assertions.assertEquals(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TIME), loggedDate);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_ASSIGNER_ID, returnedAssignerId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_USER_ID, returnedUserId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_GROUP_ID, returnedGroupId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TASK_ID, returnedTaskId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_PROC_DEFINITION_ID, returnedProcessDefinitionId);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_PROC_DEFINITION_KEY, returnedProcessDefinitionKey);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TYPE, returnedType);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_OPERATION_TYPE, returnedOperationType);
    Assertions.assertEquals(MockProvider.EXAMPLE_TENANT_ID, returnedTenantId);
    Assertions.assertEquals(DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_REMOVAL_TIME), returnedRemovalTime);
    Assertions.assertEquals(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_ROOT_PROC_INST_ID, returnedRootProcessInstanceId);
  }

  @Test
  void testQueryByTenantIds() {
    mockedQuery = setUpMockHistoricIdentityLinkQuery(Arrays.asList(
        MockProvider.createMockHistoricIdentityLink(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockHistoricIdentityLink(MockProvider.ANOTHER_EXAMPLE_TENANT_ID)));

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORY_IDENTITY_LINK_QUERY_URL);

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
  void testSuccessfulPagination() {
    int firstResult = 0;
    int maxResults = 10;

    given().queryParam("firstResult", firstResult).queryParam("maxResults", maxResults).then().expect().statusCode(Status.OK.getStatusCode()).when()
        .get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).listPage(firstResult, maxResults);
  }

  @Test
  void testMissingFirstResultParameter() {
    int maxResults = 10;

    given().queryParam("maxResults", maxResults).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).listPage(0, maxResults);
  }

  @Test
  void testMissingMaxResultsParameter() {
    int firstResult = 10;

    given().queryParam("firstResult", firstResult).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect().statusCode(Status.OK.getStatusCode()).body("count", equalTo(1)).when().get(HISTORY_IDENTITY_LINK_COUNT_QUERY_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testQueryByAssignerId() {
    String assignerId = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_ASSIGNER_ID;

    given().queryParam("assignerId", assignerId).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).assignerId(assignerId);
  }

  @Test
  void testQueryByUserId() {
    String userId = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_USER_ID;

    given().queryParam("userId", userId).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).userId(userId);
  }

  @Test
  void testQueryByGroupId() {
    String groupId = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_GROUP_ID;

    given().queryParam("groupId", groupId).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).groupId(groupId);
  }

  @Test
  void testQueryByTaskId() {
    String taskId = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TASK_ID;

    given().queryParam("taskId", taskId).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).taskId(taskId);
  }

  @Test
  void testQueryByProcessDefinitionId() {
    String processDefinitionId = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_PROC_DEFINITION_ID;

    given().queryParam("processDefinitionId", processDefinitionId).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).processDefinitionId(processDefinitionId);
  }

  @Test
  void testQueryByProcessDefinitionKey() {
    String processDefinitionKey = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_PROC_DEFINITION_KEY;

    given().queryParam("processDefinitionKey", processDefinitionKey).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).processDefinitionKey(processDefinitionKey);
  }

  @Test
  void testQueryByType() {
    String type = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TYPE;

    given().queryParam("type", type).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).type(type);
  }

  @Test
  void testQueryByOperationType() {
    String operationType = MockProvider.EXAMPLE_HIST_IDENTITY_LINK_OPERATION_TYPE;

    given().queryParam("operationType", operationType).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).operationType(operationType);
  }

  @Test
  void testQueryByDateBefore() {
    Date dateBefore = DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TIME);
    given().queryParam("dateBefore", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TIME).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).dateBefore(dateBefore);
  }

  @Test
  void testQueryByDateAfter() {
    Date dateAfter = DateTimeUtil.parseDate(MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TIME);

    given().queryParam("dateAfter", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TIME).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).dateAfter(dateAfter);
  }

  @Test
  void testQueryByTenantId() {
    String tenantId = MockProvider.EXAMPLE_TENANT_ID;

    given().queryParam("tenantIdIn", tenantId).then().expect().statusCode(Status.OK.getStatusCode()).when().get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verify(mockedQuery).tenantIdIn(tenantId);
  }

  @Test
  void testAdditionalParameters() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    given()
      .queryParams(stringQueryParameters)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORY_IDENTITY_LINK_QUERY_URL);

    verifyStringParameterQueryInvocations();
  }
  protected Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("assignerId", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_ASSIGNER_ID);
    parameters.put("dateBefore", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_DATE_BEFORE);
    parameters.put("dateAfter", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_DATE_AFTER);
    parameters.put("userId", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_USER_ID);
    parameters.put("groupId", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_GROUP_ID);
    parameters.put("taskId", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TASK_ID);
    parameters.put("processDefinitionId", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_PROC_DEFINITION_ID);
    parameters.put("processDefinitionKey", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_PROC_DEFINITION_KEY);
    parameters.put("operationType", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_OPERATION_TYPE);
    parameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID);
    parameters.put("type", MockProvider.EXAMPLE_HIST_IDENTITY_LINK_TYPE);
    return parameters;
  }
  protected void verifyStringParameterQueryInvocations() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();
    verify(mockedQuery).assignerId(stringQueryParameters.get("assignerId"));
    verify(mockedQuery).userId(stringQueryParameters.get("userId"));
    verify(mockedQuery).groupId(stringQueryParameters.get("groupId"));
    verify(mockedQuery).taskId(stringQueryParameters.get("taskId"));
    verify(mockedQuery).dateBefore(DateTimeUtil.parseDate(stringQueryParameters.get("dateBefore")));
    verify(mockedQuery).dateAfter(DateTimeUtil.parseDate(stringQueryParameters.get("dateAfter")));
    verify(mockedQuery).type(stringQueryParameters.get("type"));
    verify(mockedQuery).operationType(stringQueryParameters.get("operationType"));
    verify(mockedQuery).processDefinitionId(stringQueryParameters.get("processDefinitionId"));
    verify(mockedQuery).processDefinitionKey(stringQueryParameters.get("processDefinitionKey"));
    verify(mockedQuery).tenantIdIn(stringQueryParameters.get("tenantIdIn"));
    verify(mockedQuery).list();
  }
}
