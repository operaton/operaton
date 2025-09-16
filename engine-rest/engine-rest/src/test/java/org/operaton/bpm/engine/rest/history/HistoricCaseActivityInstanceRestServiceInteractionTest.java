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
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstanceQuery;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HistoricCaseActivityInstanceRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_CASE_ACTIVITY_INSTANCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/case-activity-instance";
  protected static final String HISTORIC_SINGLE_CASE_ACTIVITY_INSTANCE_URL = HISTORIC_CASE_ACTIVITY_INSTANCE_URL + "/{id}";

  protected HistoryService historyServiceMock;
  protected HistoricCaseActivityInstance historicInstanceMock;
  protected HistoricCaseActivityInstanceQuery historicQueryMock;

  @BeforeEach
  void setUpRuntimeData() {
    historyServiceMock = mock(HistoryService.class);

    // runtime service
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);

    historicInstanceMock = MockProvider.createMockHistoricCaseActivityInstance();
    historicQueryMock = mock(HistoricCaseActivityInstanceQuery.class);

    when(historyServiceMock.createHistoricCaseActivityInstanceQuery()).thenReturn(historicQueryMock);
    when(historicQueryMock.caseActivityInstanceId(anyString())).thenReturn(historicQueryMock);
    when(historicQueryMock.singleResult()).thenReturn(historicInstanceMock);
  }

  @Test
  void testGetSingleHistoricCaseInstance() {
    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_ID)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_CASE_ACTIVITY_INSTANCE_URL);

    String content = response.asString();

    String returnedCaseActivityInstanceId = from(content).getString("id");
    String returnedParentCaseActivityInstanceId = from(content).getString("parentCaseActivityInstanceId");
    String returnedCaseActivityId = from(content).getString("caseActivityId");
    String returnedCaseActivityName = from(content).getString("caseActivityName");
    String returnedCaseActivityType = from(content).getString("caseActivityType");
    String returnedCaseDefinitionId = from(content).getString("caseDefinitionId");
    String returnedCaseInstanceId = from(content).getString("caseInstanceId");
    String returnedCaseExecutionId = from(content).getString("caseExecutionId");
    String returnedTaskId = from(content).getString("taskId");
    String returnedCalledProcessInstanceId = from(content).getString("calledProcessInstanceId");
    String returnedCalledCaseInstanceId = from(content).getString("calledCaseInstanceId");
    String returnedCreateTime = from(content).getString("createTime");
    String returnedEndTime = from(content).getString("endTime");
    String returnedTenantId = from(content).getString("tenantId");
    long returnedDurationInMillis = from(content).getLong("durationInMillis");
    boolean required = from(content).getBoolean("required");
    boolean available = from(content).getBoolean("available");
    boolean enabled = from(content).getBoolean("enabled");
    boolean disabled = from(content).getBoolean("disabled");
    boolean active = from(content).getBoolean("active");
    boolean completed = from(content).getBoolean("completed");
    boolean terminated = from(content).getBoolean("terminated");

    assertThat(returnedCaseActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_ID);
    assertThat(returnedParentCaseActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_PARENT_CASE_ACTIVITY_INSTANCE_ID);
    assertThat(returnedCaseActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_ID);
    assertThat(returnedCaseActivityName).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_NAME);
    assertThat(returnedCaseActivityType).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_TYPE);
    assertThat(returnedCaseDefinitionId).isEqualTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    assertThat(returnedCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID);
    assertThat(returnedCaseExecutionId).isEqualTo(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    assertThat(returnedTaskId).isEqualTo(MockProvider.EXAMPLE_TASK_ID);
    assertThat(returnedCalledProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_CALLED_PROCESS_INSTANCE_ID);
    assertThat(returnedCalledCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_CALLED_CASE_INSTANCE_ID);
    assertThat(returnedCreateTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_CREATE_TIME);
    assertThat(returnedEndTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_END_TIME);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedDurationInMillis).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_DURATION);
    assertThat(required).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_IS_REQUIRED);
    assertThat(available).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_IS_AVAILABLE);
    assertThat(enabled).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_IS_ENABLED);
    assertThat(disabled).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_IS_DISABLED);
    assertThat(active).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_IS_ACTIVE);
    assertThat(completed).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_IS_COMPLETED);
    assertThat(terminated).isEqualTo(MockProvider.EXAMPLE_HISTORIC_CASE_ACTIVITY_INSTANCE_IS_TERMINATED);
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
      .body("message", equalTo("Historic case activity instance with id '" + MockProvider.NON_EXISTING_ID + "' does not exist"))
    .when()
      .get(HISTORIC_SINGLE_CASE_ACTIVITY_INSTANCE_URL);
  }

}
