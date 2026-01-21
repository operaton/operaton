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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.JsonPathUtil;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HistoricProcessInstanceRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String DELETE_REASON = "deleteReason";
  protected static final String TEST_DELETE_REASON = "test";
  protected static final String FAIL_IF_NOT_EXISTS = "failIfNotExists";
  protected static final String HISTORIC_PROCESS_INSTANCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/process-instance";
  protected static final String HISTORIC_SINGLE_PROCESS_INSTANCE_URL = HISTORIC_PROCESS_INSTANCE_URL + "/{id}";
  protected static final String DELETE_HISTORIC_PROCESS_INSTANCES_ASYNC_URL = HISTORIC_PROCESS_INSTANCE_URL + "/delete";
  protected static final String SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL = HISTORIC_PROCESS_INSTANCE_URL + "/set-removal-time";
  protected static final String HISTORIC_SINGLE_PROCESS_INSTANCE_VARIABLES_URL = HISTORIC_PROCESS_INSTANCE_URL + "/{id}/variable-instances";

  private HistoryService historyServiceMock;

  @BeforeEach
  void setUpRuntimeData() {
    historyServiceMock = mock(HistoryService.class);

    // runtime service
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);
  }

  @Test
  void testGetSingleInstance() {
    HistoricProcessInstance mockInstance = MockProvider.createMockHistoricProcessInstance();
    HistoricProcessInstanceQuery sampleInstanceQuery = mock(HistoricProcessInstanceQuery.class);

    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.processInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.singleResult()).thenReturn(mockInstance);

    Response response = given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_SINGLE_PROCESS_INSTANCE_URL);

    String content = response.asString();

    String returnedProcessInstanceId = from(content).getString("id");
    String returnedProcessInstanceBusinessKey = from(content).getString("businessKey");
    String returnedProcessDefinitionId = from(content).getString("processDefinitionId");
    String returnedProcessDefinitionKey = from(content).getString("processDefinitionKey");
    String returnedStartTime = from(content).getString("startTime");
    String returnedEndTime = from(content).getString("endTime");
    long returnedDurationInMillis = from(content).getLong("durationInMillis");
    String returnedStartUserId = from(content).getString("startUserId");
    String returnedStartActivityId = from(content).getString("startActivityId");
    String returnedDeleteReason = from(content).getString(DELETE_REASON);
    String returnedSuperProcessInstanceId = from(content).getString("superProcessInstanceId");
    String returnedSuperCaseInstanceId = from(content).getString("superCaseInstanceId");
    String returnedCaseInstanceId = from(content).getString("caseInstanceId");
    String returnedTenantId = from(content).getString("tenantId");
    String returnedState = from(content).getString("state");
    String restartedProcessInstanceId = from(content).getString("restartedProcessInstanceId");

    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedProcessInstanceBusinessKey).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    assertThat(returnedStartTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_START_TIME);
    assertThat(returnedEndTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_END_TIME);
    assertThat(returnedDurationInMillis).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_DURATION_MILLIS);
    assertThat(returnedStartUserId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_START_USER_ID);
    assertThat(returnedStartActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_START_ACTIVITY_ID);
    assertThat(returnedDeleteReason).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_DELETE_REASON);
    assertThat(returnedSuperProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_SUPER_PROCESS_INSTANCE_ID);
    assertThat(returnedSuperCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_SUPER_CASE_INSTANCE_ID);
    assertThat(returnedCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_CASE_INSTANCE_ID);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedState).isEqualTo(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_STATE);
    assertThat(restartedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

  }

  @Test
  void testGetNonExistingProcessInstance() {
    HistoricProcessInstanceQuery sampleInstanceQuery = mock(HistoricProcessInstanceQuery.class);

    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.processInstanceId(anyString())).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.singleResult()).thenReturn(null);

    given().pathParam("id", "aNonExistingInstanceId")
        .then().expect().statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Historic process instance with id aNonExistingInstanceId does not exist"))
        .when().get(HISTORIC_SINGLE_PROCESS_INSTANCE_URL);
  }

  @Test
  void testDeleteProcessInstance() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
        .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
        .when().delete(HISTORIC_SINGLE_PROCESS_INSTANCE_URL);

    verify(historyServiceMock).deleteHistoricProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
  }

  @Test
  void testDeleteNonExistingProcessInstance() {
    doThrow(new ProcessEngineException("expected exception")).when(historyServiceMock).deleteHistoricProcessInstance(anyString());

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
        .then().expect().statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Historic process instance with id " + MockProvider.EXAMPLE_PROCESS_INSTANCE_ID + " does not exist"))
        .when().delete(HISTORIC_SINGLE_PROCESS_INSTANCE_URL);
  }

  @Test
  void testDeleteNonExistingProcessInstanceIfExists() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParam("failIfNotExists", false)
    .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
    .when().delete(HISTORIC_SINGLE_PROCESS_INSTANCE_URL);

    verify(historyServiceMock).deleteHistoricProcessInstanceIfExists(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
  }

  @Test
  void testDeleteProcessInstanceThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(historyServiceMock).deleteHistoricProcessInstance(anyString());

    given()
        .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
        .then().expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(AuthorizationException.class.getSimpleName()))
        .body("message", equalTo(message))
        .when()
        .delete(HISTORIC_SINGLE_PROCESS_INSTANCE_URL);
  }

  @Test
  void testDeleteAsync() {
    List<String> ids = List.of(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    Batch batchEntity = MockProvider.createMockBatch();
    when(historyServiceMock.deleteHistoricProcessInstancesAsync(anyList(), any(), anyString())).thenReturn(batchEntity);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("historicProcessInstanceIds", ids);
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);

    Response response = given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(historyServiceMock, times(1)).deleteHistoricProcessInstancesAsync(
        ids, (HistoricProcessInstanceQuery) null, TEST_DELETE_REASON);
  }

  @Test
  void testDeleteAsyncWithQuery() {
    Batch batchEntity = MockProvider.createMockBatch();
    when(historyServiceMock.deleteHistoricProcessInstancesAsync(any(), any(), any())
    ).thenReturn(batchEntity);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);
    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    messageBodyJson.put("historicProcessInstanceQuery", query);

    Response response = given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(historyServiceMock, times(1)).deleteHistoricProcessInstancesAsync(
        isNull(), isNull(), eq(TEST_DELETE_REASON));
  }


  @Test
  void testDeleteAsyncWithBadRequestQuery() {
    doThrow(new BadUserRequestException("process instance ids are empty"))
        .when(historyServiceMock).deleteHistoricProcessInstancesAsync(eq((List<String>) null), eq((HistoricProcessInstanceQuery) null), anyString());

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when().post(DELETE_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);
  }

  @Test
  void testDeleteAllVariablesByProcessInstanceId() {
    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(HISTORIC_SINGLE_PROCESS_INSTANCE_VARIABLES_URL);

    verify(historyServiceMock).deleteHistoricVariableInstancesByProcessInstanceId(EXAMPLE_PROCESS_INSTANCE_ID);
  }

  @Test
  void testDeleteAllVariablesForNonExistingProcessInstance() {
    doThrow(new NotFoundException("No historic process instance found with id: 'NON_EXISTING_ID'"))
    .when(historyServiceMock).deleteHistoricVariableInstancesByProcessInstanceId("NON_EXISTING_ID");

    given()
      .pathParam("id", "NON_EXISTING_ID")
    .expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .body(containsString("No historic process instance found with id: 'NON_EXISTING_ID'"))
    .when()
      .delete(HISTORIC_SINGLE_PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void shouldSetRemovalTime_ByIds() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricProcessInstances()).thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicProcessInstanceIds", Collections.singletonList(EXAMPLE_PROCESS_INSTANCE_ID));
    payload.put("calculatedRemovalTime", true);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricProcessInstances();

    verify(builder).calculatedRemovalTime();
    verify(builder).byIds(EXAMPLE_PROCESS_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_ByQuery() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricProcessInstances()).thenReturn(builderMock);

    HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQueryImpl.class, RETURNS_DEEP_STUBS);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(query);

    Map<String, Object> payload = new HashMap<>();
    payload.put("calculatedRemovalTime", true);
    payload.put("historicProcessInstanceQuery", Collections.singletonMap("processDefinitionId", EXAMPLE_PROCESS_DEFINITION_ID));

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricProcessInstances();

    verify(query).processDefinitionId(EXAMPLE_PROCESS_DEFINITION_ID);

    verify(builder).calculatedRemovalTime();
    verify(builder).byIds((String[]) null);
    verify(builder).byQuery(query);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_Absolute() {
    Date removalTime = new Date();

    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricProcessInstances()).thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicProcessInstanceIds", Collections.singletonList(EXAMPLE_PROCESS_INSTANCE_ID));
    payload.put("absoluteRemovalTime", removalTime);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricProcessInstances();

    verify(builder).absoluteRemovalTime(removalTime);
    verify(builder).byIds(EXAMPLE_PROCESS_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_AbsoluteNoTime() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricProcessInstances()).thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicProcessInstanceIds", Collections.singletonList(EXAMPLE_PROCESS_INSTANCE_ID));
    payload.put("absoluteRemovalTime", null);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    SetRemovalTimeToHistoricProcessInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricProcessInstances();

    verify(builder).byIds(EXAMPLE_PROCESS_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_ClearTime() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricProcessInstances())
      .thenReturn(builderMock);

    Map<String, Object> payload = new HashMap<>();
    payload.put("historicProcessInstanceIds", Collections.singletonList(EXAMPLE_PROCESS_INSTANCE_ID));
    payload.put("clearedRemovalTime", true);

    given()
      .contentType(ContentType.JSON)
      .body(payload)
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder =
      historyServiceMock.setRemovalTimeToHistoricProcessInstances();

    verify(builder).clearedRemovalTime();
    verify(builder).byIds(EXAMPLE_PROCESS_INSTANCE_ID);
    verify(builder).byQuery(null);
    verify(builder).executeAsync();
    verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_Response() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricProcessInstances()).thenReturn(builderMock);

    Batch batchEntity = MockProvider.createMockBatch();
    when(builderMock.executeAsync()).thenReturn(batchEntity);

    Response response = given()
      .contentType(ContentType.JSON)
      .body(Collections.emptyMap())
    .then()
      .expect().statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldSetRemovalTime_FailBadUserRequest() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
      mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

    when(historyServiceMock.setRemovalTimeToHistoricProcessInstances()).thenReturn(builderMock);

    doThrow(BadUserRequestException.class).when(builderMock).executeAsync();

    given()
      .contentType(ContentType.JSON)
      .body(Collections.emptyMap())
    .then()
      .expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);
  }

  @Test
  void shouldSetRemovalTime_InChunks() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
        mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

      when(historyServiceMock.setRemovalTimeToHistoricProcessInstances())
        .thenReturn(builderMock);

      Map<String, Object> payload = new HashMap<>();
      payload.put("historicProcessInstanceIds", Collections.singletonList(EXAMPLE_PROCESS_INSTANCE_ID));
      payload.put("clearedRemovalTime", true);
      payload.put("updateInChunks", true);

      given()
        .contentType(ContentType.JSON)
        .body(payload)
      .then()
        .expect().statusCode(Status.OK.getStatusCode())
      .when()
        .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

      SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder =
        historyServiceMock.setRemovalTimeToHistoricProcessInstances();

      verify(builder).clearedRemovalTime();
      verify(builder).byIds(EXAMPLE_PROCESS_INSTANCE_ID);
      verify(builder).byQuery(null);
      verify(builder).updateInChunks();
      verify(builder).executeAsync();
      verifyNoMoreInteractions(builder);
  }

  @Test
  void shouldSetRemovalTime_ChunkSize() {
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builderMock =
        mock(SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder.class, RETURNS_DEEP_STUBS);

      when(historyServiceMock.setRemovalTimeToHistoricProcessInstances())
        .thenReturn(builderMock);

      Map<String, Object> payload = new HashMap<>();
      payload.put("historicProcessInstanceIds", Collections.singletonList(EXAMPLE_PROCESS_INSTANCE_ID));
      payload.put("clearedRemovalTime", true);
      payload.put("updateChunkSize", 20);

      given()
        .contentType(ContentType.JSON)
        .body(payload)
      .then()
        .expect().statusCode(Status.OK.getStatusCode())
      .when()
        .post(SET_REMOVAL_TIME_HISTORIC_PROCESS_INSTANCES_ASYNC_URL);

      SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder =
        historyServiceMock.setRemovalTimeToHistoricProcessInstances();

      verify(builder).clearedRemovalTime();
      verify(builder).byIds(EXAMPLE_PROCESS_INSTANCE_ID);
      verify(builder).byQuery(null);
      verify(builder).chunkSize(20);
      verify(builder).executeAsync();
      verifyNoMoreInteractions(builder);
  }

  @Test
  void testOrQuery() {
    // given
    HistoricProcessInstanceQueryImpl mockedQuery = mock(HistoricProcessInstanceQueryImpl.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedQuery);

    String payload = """
    {
      "orQueries": [
        {
          "processDefinitionKey": "aKey",
          "processInstanceBusinessKey": "aBusinessKey",
          "completed": true,
          "active": true
        }
      ]
    }
    """;

    // when
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .header(ACCEPT_JSON_HEADER)
      .body(payload)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_PROCESS_INSTANCE_URL);

    ArgumentCaptor<HistoricProcessInstanceQueryImpl> argument =
        ArgumentCaptor.forClass(HistoricProcessInstanceQueryImpl.class);

    verify(mockedQuery).addOrQuery(argument.capture());

    // then
    assertThat(argument.getValue().getProcessDefinitionKey()).isEqualTo("aKey");
    assertThat(argument.getValue().getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(argument.getValue().getState()).isEqualTo(new HashSet<>(List.of("COMPLETED", "ACTIVE")));
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
