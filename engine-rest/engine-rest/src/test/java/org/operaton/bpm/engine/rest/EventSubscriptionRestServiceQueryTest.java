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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EventSubscriptionRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String EVENT_SUBSCRIPTION_URL = TEST_RESOURCE_ROOT_PATH + "/event-subscription";
  protected static final String EVENT_SUBSCRIPTION_COUNT_QUERY_URL = EVENT_SUBSCRIPTION_URL + "/count";

  private EventSubscriptionQuery mockedEventSubscriptionQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedEventSubscriptionQuery = setUpMockEventSubscriptionQuery(createMockEventSubscriptionList());
  }

  private EventSubscriptionQuery setUpMockEventSubscriptionQuery(List<EventSubscription> mockedInstances) {
    EventSubscriptionQuery sampleEventSubscriptionsQuery = mock(EventSubscriptionQuery.class);
    when(sampleEventSubscriptionsQuery.list()).thenReturn(mockedInstances);
    when(sampleEventSubscriptionsQuery.count()).thenReturn((long) mockedInstances.size());
    when(processEngine.getRuntimeService().createEventSubscriptionQuery()).thenReturn(sampleEventSubscriptionsQuery);
    return sampleEventSubscriptionsQuery;
  }

  private List<EventSubscription> createMockEventSubscriptionList() {
    List<EventSubscription> mocks = new ArrayList<>();

    mocks.add(MockProvider.createMockEventSubscription());
    return mocks;
  }

  @Test
  void testEmptyQuery() {
    given()
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);
  }

  @Test
  void testEventSubscriptionRetrieval() {
    Response response =
        given()
          .then().expect()
            .statusCode(Status.OK.getStatusCode())
          .when().get(EVENT_SUBSCRIPTION_URL);

    // assert query invocation
    InOrder inOrder = Mockito.inOrder(mockedEventSubscriptionQuery);
    inOrder.verify(mockedEventSubscriptionQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).as("There should be one event subscription returned.").hasSize(1);
    assertThat(instances.get(0)).as("There should be one event subscription returned").isNotNull();

    String returnedEventSubscriptionId = from(content).getString("[0].id");
    String returnedEventType = from(content).getString("[0].eventType");
    String returnedEventName = from(content).getString("[0].eventName");
    String returnedExecutionId = from(content).getString("[0].executionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedCreatedDate = from(content).getString("[0].createdDate");
    String returnedTenantId = from(content).getString("[0].tenantId");

    assertThat(returnedEventSubscriptionId).isEqualTo(MockProvider.EXAMPLE_EVENT_SUBSCRIPTION_ID);
    assertThat(returnedEventType).isEqualTo(MockProvider.EXAMPLE_EVENT_SUBSCRIPTION_TYPE);
    assertThat(returnedEventName).isEqualTo(MockProvider.EXAMPLE_EVENT_SUBSCRIPTION_NAME);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_EXECUTION_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_ACTIVITY_ID);
    assertThat(returnedCreatedDate).isEqualTo(MockProvider.EXAMPLE_EVENT_SUBSCRIPTION_CREATION_DATE);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("definitionId", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  @Test
  void testSortByParameterOnly() {
    given()
      .queryParam("sortBy", "created")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given()
      .queryParam("sortOrder", "asc")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect().statusCode(Status.OK.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);

    verify(mockedEventSubscriptionQuery).list();
    verifyNoMoreInteractions(mockedEventSubscriptionQuery);
  }

  @Test
  void testQueryParameters() {
    Map<String, String> queryParameters = getCompleteQueryParameters();

    given()
      .queryParams(queryParameters)
    .expect().statusCode(Status.OK.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);

    verify(mockedEventSubscriptionQuery).eventSubscriptionId(queryParameters.get("eventSubscriptionId"));
    verify(mockedEventSubscriptionQuery).eventType(queryParameters.get("eventType"));
    verify(mockedEventSubscriptionQuery).eventName(queryParameters.get("eventName"));
    verify(mockedEventSubscriptionQuery).executionId(queryParameters.get("executionId"));
    verify(mockedEventSubscriptionQuery).processInstanceId(queryParameters.get("processInstanceId"));
    verify(mockedEventSubscriptionQuery).activityId(queryParameters.get("activityId"));
    verify(mockedEventSubscriptionQuery).list();
  }

  @Test
  void testTenantIdListParameter() {
    mockedEventSubscriptionQuery = setUpMockEventSubscriptionQuery(createMockEventSubscriptionTwoTenants());

    Response response =
        given()
          .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when().get(EVENT_SUBSCRIPTION_URL);

    verify(mockedEventSubscriptionQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedEventSubscriptionQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }


  @Test
  void testWithoutTenantIdParameter() {
    mockedEventSubscriptionQuery = setUpMockEventSubscriptionQuery(List.of(MockProvider.createMockEventSubscription(null)));

    Response response =
        given()
          .queryParam("withoutTenantId", true)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when().get(EVENT_SUBSCRIPTION_URL);

    verify(mockedEventSubscriptionQuery).withoutTenantId();
    verify(mockedEventSubscriptionQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId1).isNull();
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedEventSubscriptionQuery);
    executeAndVerifySorting("created", "asc", Status.OK);
    inOrder.verify(mockedEventSubscriptionQuery).orderByCreated();
    inOrder.verify(mockedEventSubscriptionQuery).asc();

    inOrder = Mockito.inOrder(mockedEventSubscriptionQuery);
    executeAndVerifySorting("created", "desc", Status.OK);
    inOrder.verify(mockedEventSubscriptionQuery).orderByCreated();
    inOrder.verify(mockedEventSubscriptionQuery).desc();

    inOrder = Mockito.inOrder(mockedEventSubscriptionQuery);
    executeAndVerifySorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockedEventSubscriptionQuery).orderByTenantId();
    inOrder.verify(mockedEventSubscriptionQuery).asc();

    inOrder = Mockito.inOrder(mockedEventSubscriptionQuery);
    executeAndVerifySorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockedEventSubscriptionQuery).orderByTenantId();
    inOrder.verify(mockedEventSubscriptionQuery).desc();

  }

  @Test
  void testSuccessfulPagination() {

    int firstResult = 0;
    int maxResults = 10;
    given()
      .queryParam("firstResult", firstResult)
      .queryParam("maxResults", maxResults)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);

    verify(mockedEventSubscriptionQuery).listPage(firstResult, maxResults);
  }

  /**
   * If parameter "firstResult" is missing, we expect 0 as default.
   */
  @Test
  void testMissingFirstResultParameter() {
    int maxResults = 10;
    given()
      .queryParam("maxResults", maxResults)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);

    verify(mockedEventSubscriptionQuery).listPage(0, maxResults);
  }

  /**
   * If parameter "maxResults" is missing, we expect Integer.MAX_VALUE as
   * default.
   */
  @Test
  void testMissingMaxResultsParameter() {
    int firstResult = 10;
    given()
      .queryParam("firstResult", firstResult)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);

    verify(mockedEventSubscriptionQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode()).body("count", equalTo(1))
    .when().get(EVENT_SUBSCRIPTION_COUNT_QUERY_URL);

    verify(mockedEventSubscriptionQuery).count();
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder)
    .then().expect()
      .statusCode(expectedStatus.getStatusCode())
    .when().get(EVENT_SUBSCRIPTION_URL);
  }

  private Map<String, String> getCompleteQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("eventSubscriptionId", "anEventSubscriptionId");
    parameters.put("eventType", "aEventType");
    parameters.put("eventName", "aEventName");
    parameters.put("executionId", "aExecutionId");
    parameters.put("processInstanceId", "aProcessInstanceId");
    parameters.put("activityId", "aActivityId");

    return parameters;
  }

  private List<EventSubscription> createMockEventSubscriptionTwoTenants() {
    return List.of(
        MockProvider.createMockEventSubscription(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockEventSubscription(MockProvider.ANOTHER_EXAMPLE_TENANT_ID)
      );
  }
}
