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
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricDecisionInstancesBuilder;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricDecisionInstanceQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.JsonPathUtil;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_DECISION_DEFINITION_ID;
import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_DECISION_INSTANCE_ID;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class HistoricDecisionInstanceRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_DECISION_INSTANCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/decision-instance";
  protected static final String HISTORIC_SINGLE_DECISION_INSTANCE_URL = HISTORIC_DECISION_INSTANCE_URL + "/{id}";
  protected static final String HISTORIC_DECISION_INSTANCES_DELETE_ASYNC_URL = HISTORIC_DECISION_INSTANCE_URL + "/delete";
  protected static final String SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL = HISTORIC_DECISION_INSTANCE_URL + "/set-removal-time";

  protected HistoryService historyServiceMock;
  protected HistoricDecisionInstance historicInstanceMock;
  protected HistoricDecisionInstanceQuery historicQueryMock;

  @BeforeEach
  void setUpRuntimeData() {
    historyServiceMock = mock(HistoryService.class);

    // runtime service
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);

    historicInstanceMock = MockProvider.createMockHistoricDecisionInstance();
    historicQueryMock = mock(HistoricDecisionInstanceQuery.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.createHistoricDecisionInstanceQuery()).thenReturn(historicQueryMock);
    when(historicQueryMock.decisionInstanceId(anyString())).thenReturn(historicQueryMock);
    when((Object) historicQueryMock.singleResult()).thenReturn(historicInstanceMock);
  }

  @Test
  void testGetSingleHistoricDecisionInstance() {
    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_DECISION_INSTANCE_URL);

    InOrder inOrder = inOrder(historicQueryMock);
    inOrder.verify(historicQueryMock).decisionInstanceId(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    inOrder.verify(historicQueryMock).singleResult();

    String content = response.asString();

    String returnedHistoricDecisionInstanceId = from(content).getString("id");
    String returnedDecisionDefinitionId = from(content).getString("decisionDefinitionId");
    String returnedDecisionDefinitionKey = from(content).getString("decisionDefinitionKey");
    String returnedDecisionDefinitionName = from(content).getString("decisionDefinitionName");
    String returnedEvaluationTime = from(content).getString("evaluationTime");
    String returnedProcessDefinitionId = from(content).getString("processDefinitionId");
    String returnedProcessDefinitionKey = from(content).getString("processDefinitionKey");
    String returnedProcessInstanceId = from(content).getString("processInstanceId");
    String returnedCaseDefinitionId = from(content).getString("caseDefinitionId");
    String returnedCaseDefinitionKey = from(content).getString("caseDefinitionKey");
    String returnedCaseInstanceId = from(content).getString("caseInstanceId");
    String returnedActivityId = from(content).getString("activityId");
    String returnedActivityInstanceId = from(content).getString("activityInstanceId");
    String returnedUserId = from(content).getString("userId");
    List<Map<String, Object>> returnedInputs = from(content).getList("inputs");
    List<Map<String, Object>> returnedOutputs = from(content).getList("outputs");
    Double returnedCollectResultValue = from(content).getDouble("collectResultValue");
    String returnedTenantId = from(content).getString("tenantId");
    String returnedRootDecisionInstanceId = from(content).getString("rootDecisionInstanceId");
    String returnedDecisionRequirementsDefinitionId = from(content).getString("decisionRequirementsDefinitionId");
    String returnedDecisionRequirementsDefinitionKey = from(content).getString("decisionRequirementsDefinitionKey");

    assertThat(returnedHistoricDecisionInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    assertThat(returnedDecisionDefinitionId).isEqualTo(EXAMPLE_DECISION_DEFINITION_ID);
    assertThat(returnedDecisionDefinitionKey).isEqualTo(MockProvider.EXAMPLE_DECISION_DEFINITION_KEY);
    assertThat(returnedDecisionDefinitionName).isEqualTo(MockProvider.EXAMPLE_DECISION_DEFINITION_NAME);
    assertThat(returnedEvaluationTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_EVALUATION_TIME);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedCaseDefinitionId).isEqualTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    assertThat(returnedCaseDefinitionKey).isEqualTo(MockProvider.EXAMPLE_CASE_DEFINITION_KEY);
    assertThat(returnedCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ACTIVITY_ID);
    assertThat(returnedActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ACTIVITY_INSTANCE_ID);
    assertThat(returnedUserId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_USER_ID);
    assertThat(returnedInputs).isNull();
    assertThat(returnedOutputs).isNull();
    assertThat(returnedCollectResultValue).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_COLLECT_RESULT_VALUE);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedRootDecisionInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    assertThat(returnedDecisionRequirementsDefinitionId).isEqualTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);
    assertThat(returnedDecisionRequirementsDefinitionKey).isEqualTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY);
  }

  @Test
  void testGetSingleHistoricDecisionInstanceWithInputs() {
    historicInstanceMock = MockProvider.createMockHistoricDecisionInstanceWithInputs();
    when(historicQueryMock.singleResult()).thenReturn(historicInstanceMock);

    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
        .queryParam("includeInputs", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_DECISION_INSTANCE_URL);

    InOrder inOrder = inOrder(historicQueryMock);
    inOrder.verify(historicQueryMock).decisionInstanceId(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    inOrder.verify(historicQueryMock).includeInputs();
    inOrder.verify(historicQueryMock, never()).includeOutputs();
    inOrder.verify(historicQueryMock).singleResult();

    String content = response.asString();

    List<Map<String, Object>> returnedInputs = from(content).getList("inputs");
    List<Map<String, Object>> returnedOutputs = from(content).getList("outputs");
    assertThat(returnedInputs)
            .isNotNull()
            .hasSize(3);
    assertThat(returnedOutputs).isNull();
  }

  @Test
  void testGetSingleHistoricDecisionInstanceWithOutputs() {
    historicInstanceMock = MockProvider.createMockHistoricDecisionInstanceWithOutputs();
    when(historicQueryMock.singleResult()).thenReturn(historicInstanceMock);

    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
        .queryParam("includeOutputs", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_DECISION_INSTANCE_URL);

    InOrder inOrder = inOrder(historicQueryMock);
    inOrder.verify(historicQueryMock).decisionInstanceId(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    inOrder.verify(historicQueryMock, never()).includeInputs();
    inOrder.verify(historicQueryMock).includeOutputs();
    inOrder.verify(historicQueryMock).singleResult();

    String content = response.asString();

    List<Map<String, Object>> returnedInputs = from(content).getList("inputs");
    List<Map<String, Object>> returnedOutputs = from(content).getList("outputs");
    assertThat(returnedInputs).isNull();
    assertThat(returnedOutputs)
            .isNotNull()
            .hasSize(3);
  }

  @Test
  void testGetSingleHistoricDecisionInstanceWithInputsAndOutputs() {
    historicInstanceMock = MockProvider.createMockHistoricDecisionInstanceWithInputsAndOutputs();
    when(historicQueryMock.singleResult()).thenReturn(historicInstanceMock);

    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
        .queryParam("includeInputs", true)
        .queryParam("includeOutputs", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_DECISION_INSTANCE_URL);

    InOrder inOrder = inOrder(historicQueryMock);
    inOrder.verify(historicQueryMock).decisionInstanceId(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    inOrder.verify(historicQueryMock).includeInputs();
    inOrder.verify(historicQueryMock).includeOutputs();
    inOrder.verify(historicQueryMock).singleResult();

    String content = response.asString();

    List<Map<String, Object>> returnedInputs = from(content).getList("inputs");
    List<Map<String, Object>> returnedOutputs = from(content).getList("outputs");
    assertThat(returnedInputs)
            .isNotNull()
            .hasSize(3);
    assertThat(returnedOutputs)
            .isNotNull()
            .hasSize(3);
  }

  @Test
  void testGetSingleHistoricDecisionInstanceWithDisabledBinaryFetching() {
    historicInstanceMock = MockProvider.createMockHistoricDecisionInstanceWithInputsAndOutputs();
    when(historicQueryMock.singleResult()).thenReturn(historicInstanceMock);

    given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
        .queryParam("disableBinaryFetching", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_DECISION_INSTANCE_URL);

    InOrder inOrder = inOrder(historicQueryMock);
    inOrder.verify(historicQueryMock).decisionInstanceId(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    inOrder.verify(historicQueryMock).disableBinaryFetching();
    inOrder.verify(historicQueryMock).singleResult();
  }

  @Test
  void testGetSingleHistoricDecisionInstanceWithDisabledCustomObjectDeserialization() {
    historicInstanceMock = MockProvider.createMockHistoricDecisionInstanceWithInputsAndOutputs();
    when(historicQueryMock.singleResult()).thenReturn(historicInstanceMock);

    given()
        .pathParam("id", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
        .queryParam("disableCustomObjectDeserialization", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_SINGLE_DECISION_INSTANCE_URL);

    InOrder inOrder = inOrder(historicQueryMock);
    inOrder.verify(historicQueryMock).decisionInstanceId(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    inOrder.verify(historicQueryMock).disableCustomObjectDeserialization();
    inOrder.verify(historicQueryMock).singleResult();
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
      .body("message", equalTo("Historic decision instance with id '" + MockProvider.NON_EXISTING_ID + "' does not exist"))
    .when()
      .get(HISTORIC_SINGLE_DECISION_INSTANCE_URL);
  }

  @Test
  void testDeleteAsync() {
    List<String> ids = List.of(EXAMPLE_DECISION_INSTANCE_ID);

    Batch batchEntity = MockProvider.createMockBatch();

    when(historyServiceMock.deleteHistoricDecisionInstancesAsync(any(), any(), any())).thenReturn(batchEntity);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("historicDecisionInstanceIds", ids);
    messageBodyJson.put("deleteReason", "a-delete-reason");

    Response response = given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(HISTORIC_DECISION_INSTANCES_DELETE_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(historyServiceMock, times(1)).deleteHistoricDecisionInstancesAsync(ids, (HistoricDecisionInstanceQuery) null, "a-delete-reason");
  }

  @Test
  void testDeleteAsyncWithQuery() {
    Batch batchEntity = MockProvider.createMockBatch();

    when(historyServiceMock.deleteHistoricDecisionInstancesAsync(any(), any(), any())).thenReturn(batchEntity);

    Map<String, Object> messageBodyJson = new HashMap<>();
    HistoricDecisionInstanceQueryDto query = new HistoricDecisionInstanceQueryDto();
    query.setDecisionDefinitionKey("decision");
    messageBodyJson.put("historicDecisionInstanceQuery", query);
    messageBodyJson.put("deleteReason", "a-delete-reason");

    Response response = given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(HISTORIC_DECISION_INSTANCES_DELETE_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(historyServiceMock, times(1)).deleteHistoricDecisionInstancesAsync(eq((List<String>) null), any(HistoricDecisionInstanceQuery.class), eq("a-delete-reason"));
  }

  @Test
  void testDeleteAsyncWithIdsAndQuery() {
    Batch batchEntity = MockProvider.createMockBatch();

    when(historyServiceMock.deleteHistoricDecisionInstancesAsync(
        anyList(),
        any(HistoricDecisionInstanceQuery.class),
        anyString()
    )).thenReturn(batchEntity);

    Map<String, Object> messageBodyJson = new HashMap<>();
    HistoricDecisionInstanceQueryDto query = new HistoricDecisionInstanceQueryDto();
    query.setDecisionDefinitionKey("decision");
    messageBodyJson.put("historicDecisionInstanceQuery", query);

    List<String> ids = List.of(EXAMPLE_DECISION_INSTANCE_ID);
    messageBodyJson.put("historicDecisionInstanceIds", ids);
    messageBodyJson.put("deleteReason", "a-delete-reason");

    Response response = given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(HISTORIC_DECISION_INSTANCES_DELETE_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(historyServiceMock, times(1)).deleteHistoricDecisionInstancesAsync(eq(ids), any(HistoricDecisionInstanceQuery.class), eq("a-delete-reason"));
  }

  @Test
  void testDeleteAsyncWithBadRequestQuery() {
    doThrow(new BadUserRequestException("process instance ids are empty"))
        .when(historyServiceMock).deleteHistoricDecisionInstancesAsync(eq((List<String>) null), eq((HistoricDecisionInstanceQuery) null), any());

    given()
        .contentType(ContentType.JSON).body(EMPTY_JSON_OBJECT)
        .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when().post(HISTORIC_DECISION_INSTANCES_DELETE_ASYNC_URL);
  }

  @Test
  void shouldSetRemovalTime_ByIds() {
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricDecisionInstances()).thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicDecisionInstanceIds", Collections.singletonList(EXAMPLE_DECISION_INSTANCE_ID));
    payload.put("calculatedRemovalTime", true);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricDecisionInstances();

    verify(builder).calculatedRemovalTime();
    verify(builder).byIds(EXAMPLE_DECISION_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_ByQuery() {
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricDecisionInstances()).thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("calculatedRemovalTime", true);
    payload.put("historicDecisionInstanceQuery", Collections.singletonMap("decisionInstanceId", EXAMPLE_DECISION_INSTANCE_ID));

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricDecisionInstances();

    verify(historicQueryMock).decisionInstanceId(EXAMPLE_DECISION_INSTANCE_ID);

    verify(builder).calculatedRemovalTime();
    verify(builder).byIds((String[]) null);
    verify(builder).byQuery(historicQueryMock);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_Absolute() {
    Date removalTime = new Date();

    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricDecisionInstances()).thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicDecisionInstanceIds", Collections.singletonList(EXAMPLE_DECISION_INSTANCE_ID));
    payload.put("absoluteRemovalTime", removalTime);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricDecisionInstances();

    verify(builder).absoluteRemovalTime(removalTime);
    verify(builder).byIds(EXAMPLE_DECISION_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldNotSetRemovalTime_Absolute() {
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricDecisionInstances()).thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicDecisionInstanceIds", Collections.singletonList(EXAMPLE_DECISION_INSTANCE_ID));
    payload.put("absoluteRemovalTime", null);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL);

    SetRemovalTimeToHistoricDecisionInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricDecisionInstances();

    verify(builder).byIds(EXAMPLE_DECISION_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldClearRemovalTime() {
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricDecisionInstances())
      .thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicDecisionInstanceIds", Collections.singletonList(EXAMPLE_DECISION_INSTANCE_ID));
    payload.put("clearedRemovalTime", true);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricDecisionInstances();

    verify(builder).clearedRemovalTime();
    verify(builder).byIds(EXAMPLE_DECISION_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_Response() {
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricDecisionInstances()).thenReturn(builderMock);

    Batch batchEntity = MockProvider.createMockBatch();
    when(builderMock.executeAsync()).thenReturn(batchEntity);

    Response response = given()
      .contentType(ContentType.JSON)
      .body(Collections.emptyMap())
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL);

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldSetRemovalTime_ThrowBadUserException() {
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricDecisionInstances()).thenReturn(builderMock);

    doThrow(BadUserRequestException.class).when(builderMock).executeAsync();

    given()
      .contentType(ContentType.JSON)
      .body(Collections.emptyMap())
    .then()
      .expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_DECISION_INSTANCES_ASYNC_URL);
  }

  protected void verifyBatchJson(String batchJson) {
    BatchDto batch = JsonPathUtil.from(batchJson).getObject("", BatchDto.class);
    assertThat(batch).as("The returned batch should not be null.").isNotNull();
    assertThat(batch.getId()).isEqualTo(MockProvider.EXAMPLE_BATCH_ID);
    assertThat(batch.getType()).isEqualTo(MockProvider.EXAMPLE_BATCH_TYPE);
    assertThat(batch.getTotalJobs()).isEqualTo(MockProvider.EXAMPLE_BATCH_TOTAL_JOBS);
    assertThat(batch.getBatchJobsPerSeed()).isEqualTo(MockProvider.EXAMPLE_BATCH_JOBS_PER_SEED);
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(MockProvider.EXAMPLE_INVOCATIONS_PER_BATCH_JOB);
    assertThat(batch.getSeedJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_SEED_JOB_DEFINITION_ID);
    assertThat(batch.getMonitorJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_MONITOR_JOB_DEFINITION_ID);
    assertThat(batch.getBatchJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_BATCH_JOB_DEFINITION_ID);
    assertThat(batch.getTenantId()).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
  }

}
