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

import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HistoricActivityInstanceRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_ACTIVITY_INSTANCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/activity-instance";
  protected static final String HISTORIC_SINGLE_ACTIVITY_INSTANCE_URL = HISTORIC_ACTIVITY_INSTANCE_URL + "/{id}";

  protected HistoryService historyServiceMock;
  protected HistoricActivityInstance historicInstanceMock;
  protected HistoricActivityInstanceQuery historicQueryMock;

  @BeforeEach
  void setUpRuntimeData() {
    historyServiceMock = mock(HistoryService.class);

    // runtime service
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);

    historicInstanceMock = MockProvider.createMockHistoricActivityInstance();
    historicQueryMock = mock(HistoricActivityInstanceQuery.class);

    when(historyServiceMock.createHistoricActivityInstanceQuery()).thenReturn(historicQueryMock);
    when(historicQueryMock.activityInstanceId(anyString())).thenReturn(historicQueryMock);
    when(historicQueryMock.singleResult()).thenReturn(historicInstanceMock);
  }

  @Test
  void testGetSingleHistoricActivityInstance() {
    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_ID)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_ACTIVITY_INSTANCE_URL);

    String content = response.asString();

    String returnedId = from(content).getString("id");
    String returnedParentActivityInstanceId = from(content).getString("parentActivityInstanceId");
    String returnedActivityId = from(content).getString("activityId");
    String returnedActivityName = from(content).getString("activityName");
    String returnedActivityType = from(content).getString("activityType");
    String returnedProcessDefinitionKey = from(content).getString("processDefinitionKey");
    String returnedProcessDefinitionId = from(content).getString("processDefinitionId");
    String returnedProcessInstanceId = from(content).getString("processInstanceId");
    String returnedExecutionId = from(content).getString("executionId");
    String returnedTaskId = from(content).getString("taskId");
    String returnedCalledProcessInstanceId = from(content).getString("calledProcessInstanceId");
    String returnedCalledCaseInstanceId = from(content).getString("calledCaseInstanceId");
    String returnedAssignee = from(content).getString("assignee");
    String returnedStartTime = from(content).getString("startTime");
    String returnedEndTime = from(content).getString("endTime");
    long returnedDurationInMillis = from(content).getLong("durationInMillis");
    boolean canceled = from(content).getBoolean("canceled");
    boolean completeScope = from(content).getBoolean("completeScope");
    String returnedTenantId = from(content).getString("tenantId");
    String returnedRemovalTime = from(content).getString("removalTime");
    String returnedRootProcessInstanceId= from(content).getString("rootProcessInstanceId");

    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_ID, returnedId);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_PARENT_ACTIVITY_INSTANCE_ID, returnedParentActivityInstanceId);
    assertEquals(MockProvider.EXAMPLE_ACTIVITY_ID, returnedActivityId);
    assertEquals(MockProvider.EXAMPLE_ACTIVITY_NAME, returnedActivityName);
    assertEquals(MockProvider.EXAMPLE_ACTIVITY_TYPE, returnedActivityType);
    assertEquals(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, returnedProcessDefinitionKey);
    assertEquals(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, returnedProcessDefinitionId);
    assertEquals(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, returnedProcessInstanceId);
    assertEquals(MockProvider.EXAMPLE_EXECUTION_ID, returnedExecutionId);
    assertEquals(MockProvider.EXAMPLE_TASK_ID, returnedTaskId);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_CALLED_PROCESS_INSTANCE_ID, returnedCalledProcessInstanceId);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_CALLED_CASE_INSTANCE_ID, returnedCalledCaseInstanceId);
    assertEquals(MockProvider.EXAMPLE_TASK_ASSIGNEE_NAME, returnedAssignee);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_START_TIME, returnedStartTime);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_END_TIME, returnedEndTime);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_DURATION, returnedDurationInMillis);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_IS_CANCELED, canceled);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_IS_COMPLETE_SCOPE, completeScope);
    assertEquals(MockProvider.EXAMPLE_TENANT_ID, returnedTenantId);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_INSTANCE_START_TIME, returnedRemovalTime);
    assertEquals(MockProvider.EXAMPLE_HISTORIC_ACTIVITY_ROOT_PROCESS_INSTANCE_ID, returnedRootProcessInstanceId);
  }

  @Test
  void testGetNonExistingHistoricCaseInstance() {
    when(historicQueryMock.singleResult()).thenReturn(null);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_ID)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Historic activity instance with id '" + MockProvider.NON_EXISTING_ID + "' does not exist"))
    .when()
      .get(HISTORIC_SINGLE_ACTIVITY_INSTANCE_URL);
  }

}
