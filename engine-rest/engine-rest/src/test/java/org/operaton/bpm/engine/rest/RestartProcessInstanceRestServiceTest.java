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

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.ModificationInstructionBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.RestartProcessInstanceBuilder;

import static org.operaton.bpm.engine.rest.helper.MockProvider.createMockBatch;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

public class RestartProcessInstanceRestServiceTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String PROCESS_DEFINITION_URL = TEST_RESOURCE_ROOT_PATH + "/process-definition";
  protected static final String SINGLE_PROCESS_DEFINITION_URL = PROCESS_DEFINITION_URL + "/{id}";
  protected static final String RESTART_PROCESS_INSTANCE_URL = SINGLE_PROCESS_DEFINITION_URL + "/restart";
  protected static final String RESTART_PROCESS_INSTANCE_ASYNC_URL = SINGLE_PROCESS_DEFINITION_URL + "/restart-async";

  RuntimeService runtimeServiceMock;
  HistoryService historyServiceMock;
  RestartProcessInstanceBuilder builderMock;

  @BeforeEach
  void setUpRuntimeData() {
    runtimeServiceMock = mock(RuntimeService.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeServiceMock);

    historyServiceMock = mock(HistoryService.class);
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);

    builderMock = mock(RestartProcessInstanceBuilder.class);
    when(builderMock.startAfterActivity(anyString())).thenReturn(builderMock);
    when(builderMock.startBeforeActivity(anyString())).thenReturn(builderMock);
    when(builderMock.startTransition(anyString())).thenReturn(builderMock);
    when(builderMock.processInstanceIds(anyList())).thenReturn(builderMock);
    when(builderMock.historicProcessInstanceQuery(any(HistoricProcessInstanceQuery.class))).thenReturn(builderMock);
    when(builderMock.skipCustomListeners()).thenReturn(builderMock);
    when(builderMock.skipIoMappings()).thenReturn(builderMock);
    when(builderMock.initialSetOfVariables()).thenReturn(builderMock);
    when(builderMock.withoutBusinessKey()).thenReturn(builderMock);

    Batch batchMock = createMockBatch();
    when(builderMock.executeAsync()).thenReturn(batchMock);

    when(runtimeServiceMock.restartProcessInstances(anyString())).thenReturn(builderMock);
  }

  @Test
  void testRestartProcessInstanceSync() {

    HashMap<String, Object> json = new HashMap<>();
    ArrayList<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("processInstanceIds", List.of("processInstanceId1", "processInstanceId2"));

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(ContentType.JSON)
    .body(json)
    .then().expect()
    .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
    .post(RESTART_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).restartProcessInstances(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(builderMock).startAfterActivity("activityId");
    verify(builderMock).processInstanceIds(List.of("processInstanceId1", "processInstanceId2"));
    verify(builderMock).execute();
  }

  @Test
  void testRestartProcessInstanceAsync() {
    HashMap<String, Object> json = new HashMap<>();
    ArrayList<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("processInstanceIds", List.of("processInstanceId1", "processInstanceId2"));

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(ContentType.JSON)
    .body(json)
    .then().expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);

    verify(runtimeServiceMock).restartProcessInstances(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(builderMock).startAfterActivity("activityId");
    verify(builderMock).processInstanceIds(List.of("processInstanceId1", "processInstanceId2"));
    verify(builderMock).executeAsync();
  }

  @Test
  void testRestartProcessInstanceWithNullProcessInstanceIdsSync() {
    doThrow(new BadUserRequestException("processInstanceIds is null")).when(builderMock).execute();

    HashMap<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(ContentType.JSON)
    .body(json)
    .then().expect()
    .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
    .post(RESTART_PROCESS_INSTANCE_URL);
  }

  @Test
  void testRestartProcessInstanceWithNullProcessInstanceIdsAsync() {
    doThrow(new BadUserRequestException("processInstanceIds is null")).when(builderMock).executeAsync();

    HashMap<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(ContentType.JSON)
    .body(json)
    .then().expect()
    .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
    .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);
  }

  @Test
  void testRestartProcessInstanceWithHistoricProcessInstanceQuerySync() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    HashMap<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);

    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    query.setProcessInstanceBusinessKey("businessKey");

    json.put("historicProcessInstanceQuery", query);

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(ContentType.JSON)
    .body(json)
    .then().expect()
    .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
    .post(RESTART_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).restartProcessInstances(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).historicProcessInstanceQuery(query.toQuery(processEngine));
    verify(builderMock).execute();
  }

  @Test
  void testRestartProcessInstanceWithHistoricProcessInstanceQueryAsync() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    HashMap<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);

    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    query.setProcessInstanceBusinessKey("businessKey");

    json.put("historicProcessInstanceQuery", query);

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(ContentType.JSON)
    .body(json)
    .then().expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);

    verify(runtimeServiceMock).restartProcessInstances(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).historicProcessInstanceQuery(query.toQuery(processEngine));
    verify(builderMock).executeAsync();
  }

  @Test
  void testRestartProcessInstanceWithNullInstructionsSync() {
    doThrow(new BadUserRequestException("instructions is null")).when(builderMock).execute();

    HashMap<String, Object> json = new HashMap<>();
    json.put("processInstanceIds", "processInstanceId");

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(RESTART_PROCESS_INSTANCE_URL);
  }

  @Test
  void testRestartProcessInstanceWithNullInstructionsAsync() {
    doThrow(new BadUserRequestException("instructions is null")).when(builderMock).executeAsync();

    HashMap<String, Object> json = new HashMap<>();
    json.put("processInstanceIds", "processInstanceId");

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);
  }

  @Test
  void testRestartProcessInstanceWithInvalidModificationInstructionForStartAfterSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startAfter().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startAfterActivity': 'activityId' must be set"))
    .when()
      .post(RESTART_PROCESS_INSTANCE_URL);
  }

  @Test
  void testRestartProcessInstanceWithInvalidModificationInstructionForStartAfterAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startAfter().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startAfterActivity': 'activityId' must be set"))
    .when()
      .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);
  }

  @Test
  void testRestartProcessInstanceWithInvalidModificationInstructionForStartBeforeSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startBefore().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startBeforeActivity': 'activityId' must be set"))
    .when()
      .post(RESTART_PROCESS_INSTANCE_URL);
  }

  @Test
  void testRestartProcessInstanceWithInvalidModificationInstructionForStartBeforeAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startBefore().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startBeforeActivity': 'activityId' must be set"))
    .when()
      .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);
  }

  @Test
  void testRestartProcessInstanceWithInvalidModificationInstructionForStartTransitionSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startTransition().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startTransition': 'transitionId' must be set"))
    .when()
      .post(RESTART_PROCESS_INSTANCE_URL);
  }

  @Test
  void testRestartProcessInstanceWithInvalidModificationInstructionForStartTransitionAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startTransition().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startTransition': 'transitionId' must be set"))
    .when()
      .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);
  }

  @Test
  void testRestartProcessInstanceWithInitialVariablesAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("processInstance1", "processInstance2"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("initialVariables", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
     .when()
       .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);

    verify(builderMock).processInstanceIds(List.of("processInstance1", "processInstance2"));
    verify(builderMock).initialSetOfVariables();
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).executeAsync();
  }

  @Test
  void testRestartProcessInstanceWithInitialVariablesSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("processInstance1", "processInstance2"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("initialVariables", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
     .when()
       .post(RESTART_PROCESS_INSTANCE_URL);

    verify(builderMock).processInstanceIds(List.of("processInstance1", "processInstance2"));
    verify(builderMock).initialSetOfVariables();
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).execute();
  }

  @Test
  void testRestartProcessInstanceWithSkipCustomListenersAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("processInstance1", "processInstance2"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("skipCustomListeners", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
     .when()
       .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);

    verify(builderMock).processInstanceIds(List.of("processInstance1", "processInstance2"));
    verify(builderMock).skipCustomListeners();
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).executeAsync();
  }

  @Test
  void testRestartProcessInstanceWithSkipCustomListenersSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("processInstance1", "processInstance2"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("skipCustomListeners", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
     .when()
       .post(RESTART_PROCESS_INSTANCE_URL);

    verify(builderMock).processInstanceIds(List.of("processInstance1", "processInstance2"));
    verify(builderMock).skipCustomListeners();
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).execute();
  }

  @Test
  void testRestartProcessInstanceWithSkipIoMappingsAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("processInstance1", "processInstance2"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("skipIoMappings", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
     .when()
       .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);

    verify(builderMock).processInstanceIds(List.of("processInstance1", "processInstance2"));
    verify(builderMock).skipIoMappings();
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).executeAsync();
  }

  @Test
  void testRestartProcessInstanceWithSkipIoMappingsSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("processInstance1", "processInstance2"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("skipIoMappings", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
     .when()
       .post(RESTART_PROCESS_INSTANCE_URL);

    verify(builderMock).processInstanceIds(List.of("processInstance1", "processInstance2"));
    verify(builderMock).skipIoMappings();
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).execute();
  }

  @Test
  void testRestartProcessInstanceWithoutBusinessKey() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("processInstanceIds", List.of("processInstance1", "processInstance2"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("withoutBusinessKey", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
     .when()
       .post(RESTART_PROCESS_INSTANCE_URL);

    verify(builderMock).processInstanceIds(List.of("processInstance1", "processInstance2"));
    verify(builderMock).withoutBusinessKey();
    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).execute();
  }

  @Test
  void testRestartProcessInstanceWithoutProcessInstanceIdsSync() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    query.setFinished(true);
    json.put("historicProcessInstanceQuery", query);
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
     .when()
       .post(RESTART_PROCESS_INSTANCE_URL);

    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).historicProcessInstanceQuery(query.toQuery(processEngine));
    verify(builderMock).execute();
    verifyNoMoreInteractions(builderMock);
  }

  @Test
  void testRestartProcessInstanceWithoutProcessInstanceIdsAsync() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    query.setFinished(true);
    json.put("historicProcessInstanceQuery", query);
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
     .when()
       .post(RESTART_PROCESS_INSTANCE_ASYNC_URL);

    verify(builderMock).startBeforeActivity("activityId");
    verify(builderMock).historicProcessInstanceQuery(query.toQuery(processEngine));
    verify(builderMock).executeAsync();
    verifyNoMoreInteractions(builderMock);
  }
}
