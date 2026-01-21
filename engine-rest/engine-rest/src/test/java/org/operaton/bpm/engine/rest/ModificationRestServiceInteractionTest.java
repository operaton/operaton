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
import org.mockito.Mockito;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.util.ModificationInstructionBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.ModificationBuilder;

import static org.operaton.bpm.engine.rest.helper.MockProvider.createMockBatch;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

public class ModificationRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String PROCESS_INSTANCE_URL = TEST_RESOURCE_ROOT_PATH + "/modification";
  protected static final String EXECUTE_MODIFICATION_SYNC_URL = PROCESS_INSTANCE_URL + "/execute";
  protected static final String EXECUTE_MODIFICATION_ASYNC_URL = PROCESS_INSTANCE_URL + "/executeAsync";

  protected RuntimeService runtimeServiceMock;
  protected HistoryService historyServiceMock;
  protected ModificationBuilder modificationBuilderMock;

  @BeforeEach
  void setUpRuntimeData() {
    runtimeServiceMock = mock(RuntimeService.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeServiceMock);

    historyServiceMock = mock(HistoryService.class);
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);

    modificationBuilderMock = mock(ModificationBuilder.class);
    when(modificationBuilderMock.cancelAllForActivity(any())).thenReturn(modificationBuilderMock);
    when(modificationBuilderMock.startAfterActivity(any())).thenReturn(modificationBuilderMock);
    when(modificationBuilderMock.startBeforeActivity(any())).thenReturn(modificationBuilderMock);
    when(modificationBuilderMock.startTransition(any())).thenReturn(modificationBuilderMock);
    when(modificationBuilderMock.processInstanceIds(Mockito.<List<String>>any())).thenReturn(modificationBuilderMock);

    Batch batchMock = createMockBatch();
    when(modificationBuilderMock.executeAsync()).thenReturn(batchMock);

    when(runtimeServiceMock.createModification(any())).thenReturn(modificationBuilderMock);
  }

  @Test
  void executeModificationSync() {
    Map<String, Object> json = new HashMap<>();
    json.put("skipCustomListeners", true);
    json.put("skipIoMappings", true);
    json.put("processDefinitionId", "processDefinitionId");
    json.put("processInstanceIds", List.of("100", "20"));
    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());

    json.put("instructions", instructions);

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);

    verify(runtimeServiceMock).createModification("processDefinitionId");
    verify(modificationBuilderMock).processInstanceIds(List.of("100", "20"));
    verify(modificationBuilderMock).cancelAllForActivity("activityId");
    verify(modificationBuilderMock).startBeforeActivity("activityId");
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).startTransition("transitionId");
    verify(modificationBuilderMock).skipCustomListeners();
    verify(modificationBuilderMock).skipIoMappings();
    verify(modificationBuilderMock).execute();
  }

  @Test
  void executeModificationWithNullProcessDefinitionIdAsync() {
    doThrow(new BadUserRequestException("processDefinitionId must be set"))
    .when(modificationBuilderMock).executeAsync();

    Map<String, Object> json = new HashMap<>();
    json.put("skipCustomListeners", true);
    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("100", "20"));
    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());

    json.put("instructions", instructions);

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(runtimeServiceMock).createModification(null);
    verify(modificationBuilderMock).processInstanceIds(List.of("100", "20"));
    verify(modificationBuilderMock).cancelAllForActivity("activityId");
    verify(modificationBuilderMock).startBeforeActivity("activityId");
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).startTransition("transitionId");
    verify(modificationBuilderMock).skipCustomListeners();
    verify(modificationBuilderMock).skipIoMappings();
    verify(modificationBuilderMock).executeAsync();
  }

  @Test
  void executeModificationWithNullProcessDefinitionIdSync() {
    doThrow(new BadUserRequestException("processDefinitionId must be set"))
    .when(modificationBuilderMock).execute();

    Map<String, Object> json = new HashMap<>();
    json.put("skipCustomListeners", true);
    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("100", "20"));
    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());

    json.put("instructions", instructions);

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);

    verify(runtimeServiceMock).createModification(null);
    verify(modificationBuilderMock).processInstanceIds(List.of("100", "20"));
    verify(modificationBuilderMock).cancelAllForActivity("activityId");
    verify(modificationBuilderMock).startBeforeActivity("activityId");
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).startTransition("transitionId");
    verify(modificationBuilderMock).skipCustomListeners();
    verify(modificationBuilderMock).skipIoMappings();
    verify(modificationBuilderMock).execute();
  }

  @Test
  void executeModificationWithNullProcessInstanceIdsSync() {
    Map<String, Object> json = new HashMap<>();
    String message = "Process instance ids is null";
    doThrow(new BadUserRequestException(message))
    .when(modificationBuilderMock).execute();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId(EXAMPLE_ACTIVITY_ID).getJson());
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());
    json.put("processDefinitionId", "processDefinitionId");
    json.put("instructions", instructions);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("message", is(message))
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);
  }

  @Test
  void executeModificationAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    json.put("processDefinitionId", "processDefinitionId");
    json.put("instructions", instructions);
    json.put("processInstanceIds", List.of("100", "20"));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(runtimeServiceMock).createModification("processDefinitionId");
    verify(modificationBuilderMock).processInstanceIds(List.of("100", "20"));
    verify(modificationBuilderMock).cancelAllForActivity("activityId");
    verify(modificationBuilderMock).startBeforeActivity("activityId");
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).startTransition("transitionId");
    verify(modificationBuilderMock).executeAsync();
  }

  @Test
  void executeModificationWithNullProcessInstanceIdsAsync() {
    Map<String, Object> json = new HashMap<>();

    String message = "Process instance ids is null";
    doThrow(new BadUserRequestException(message))
    .when(modificationBuilderMock).executeAsync();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId(EXAMPLE_ACTIVITY_ID).getJson());
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());

    json.put("instructions", instructions);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("message", is(message))
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeModificationWithValidProcessInstanceQuerySync() {

    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(new ProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    json.put("processDefinitionId", "processDefinitionId");

    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setBusinessKey("foo");

    json.put("processInstanceQuery", processInstanceQueryDto);
    json.put("instructions", instructions);

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);

    verify(runtimeServiceMock, times(1)).createProcessInstanceQuery();
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).processInstanceQuery(processInstanceQueryDto.toQuery(processEngine));
    verify(modificationBuilderMock).execute();
  }

  @Test
  void executeModificationWithValidHistoricProcessInstanceQuerySync() {

    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    json.put("processDefinitionId", "processDefinitionId");

    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessInstanceBusinessKey("foo");

    json.put("historicProcessInstanceQuery", historicProcessInstanceQueryDto);

    json.put("instructions", instructions);

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .then()
        .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
        .post(EXECUTE_MODIFICATION_SYNC_URL);

    verify(historyServiceMock, times(1)).createHistoricProcessInstanceQuery();
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).historicProcessInstanceQuery(historicProcessInstanceQueryDto.toQuery(processEngine));
    verify(modificationBuilderMock).execute();
  }


  @Test
  void executeModificationWithValidProcessInstanceQueryAsync() {

    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(new ProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());

    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setBusinessKey("foo");

    json.put("processInstanceQuery", processInstanceQueryDto);
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(runtimeServiceMock, times(1)).createProcessInstanceQuery();
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).processInstanceQuery(processInstanceQueryDto.toQuery(processEngine));
    verify(modificationBuilderMock).executeAsync();
  }

  @Test
  void executeModificationWithValidHistoricProcessInstanceQueryAsync() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());

    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessInstanceBusinessKey("foo");

    json.put("historicProcessInstanceQuery", historicProcessInstanceQueryDto);
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(historyServiceMock, times(1)).createHistoricProcessInstanceQuery();
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).historicProcessInstanceQuery(historicProcessInstanceQueryDto.toQuery(processEngine));
    verify(modificationBuilderMock).executeAsync();
  }

  @Test
  void executeModificationWithBothProcessInstanceQueries() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(new ProcessInstanceQueryImpl());

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());

    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessInstanceBusinessKey("foo");

    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setBusinessKey("foo");

    json.put("processInstanceQuery", processInstanceQueryDto);
    json.put("historicProcessInstanceQuery", historicProcessInstanceQueryDto);
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(historyServiceMock, times(1)).createHistoricProcessInstanceQuery();
    verify(modificationBuilderMock).startAfterActivity("activityId");
    verify(modificationBuilderMock).historicProcessInstanceQuery(historicProcessInstanceQueryDto.toQuery(processEngine));
    verify(runtimeServiceMock, times(1)).createProcessInstanceQuery();
    verify(modificationBuilderMock).processInstanceQuery(processInstanceQueryDto.toQuery(processEngine));
    verify(modificationBuilderMock).executeAsync();
  }

  @Test
  void executeModificationWithInvalidProcessInstanceQuerySync() {

    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(new ProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();

    String message = "Process instance ids is null";
    doThrow(new BadUserRequestException(message)).when(modificationBuilderMock).execute();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("acivityId").getJson());

    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setBusinessKey("foo");
    json.put("processInstanceQuery", processInstanceQueryDto);
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);

  }

  @Test
  void executeModificationWithInvalidHistoricProcessInstanceQuerySync() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());

    Map<String, Object> json = new HashMap<>();

    String message = "Process instance ids is null";
    doThrow(new BadUserRequestException(message)).when(modificationBuilderMock).execute();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("acivityId").getJson());

    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessInstanceBusinessKey("foo");

    json.put("historicProcessInstanceQuery", historicProcessInstanceQueryDto);
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .then()
        .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when()
        .post(EXECUTE_MODIFICATION_SYNC_URL);
  }

  @Test
  void executeModificationWithInvalidProcessInstanceQueryAsync() {

    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(new ProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("acivityId").getJson());

    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setBusinessKey("foo");
    json.put("processInstanceQuery", processInstanceQueryDto);
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeModificationWithInvalidHistoricProcessInstanceQueryAsync() {
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(new HistoricProcessInstanceQueryImpl());
    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("acivityId").getJson());

    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessInstanceBusinessKey("foo");

    json.put("historicProcessInstanceQuery", historicProcessInstanceQueryDto);
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeModificationWithNullInstructionsSync() {
    doThrow(new BadUserRequestException("Instructions must be set")).when(modificationBuilderMock).execute();

    Map<String, Object> json = new HashMap<>();
    json.put("processInstanceIds", List.of("200", "11"));
    json.put("skipIoMappings", true);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Instructions must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);
  }

  @Test
  void executeModificationWithNullInstructionsAsync() {
    doThrow(new BadUserRequestException("Instructions must be set")).when(modificationBuilderMock).executeAsync();
    Map<String, Object> json = new HashMap<>();
    json.put("processInstanceIds", List.of("200", "11"));
    json.put("skipIoMappings", true);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Instructions must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeModificationThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(modificationBuilderMock).executeAsync();

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());

    json.put("instructions", instructions);
    json.put("processInstanceIds", List.of("200", "323"));
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeInvalidModificationForStartAfterSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startAfter().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startAfterActivity': 'activityId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);
  }

  @Test
  void executeInvalidModificationForStartAfterAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startAfter().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startAfterActivity': 'activityId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeInvalidModificationForStartBeforeSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startBefore().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startBeforeActivity': 'activityId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);
  }

  @Test
  void executeInvalidModificationForStartBeforeAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startBefore().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startBeforeActivity': 'activityId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeInvalidModificationForStartTransitionSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startTransition().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startTransition': 'transitionId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);
  }

  @Test
  void executeInvalidModificationForStartTransitionAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startTransition().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'startTransition': 'transitionId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeInvalidModificationForCancelAllSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.cancellation().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'cancel': 'activityId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);
  }

  @Test
  void executeInvalidModificationForCancelAllAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.cancellation().getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("For instruction type 'cancel': 'activityId' must be set"))
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);
  }

  @Test
  void executeCancellationWithActiveFlagSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").cancelCurrentActiveActivityInstances(true).getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);

    verify(modificationBuilderMock).cancelAllForActivity("activityId", true);
    verify(modificationBuilderMock).execute();
  }

  @Test
  void executeCancellationWithActiveFlagAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").cancelCurrentActiveActivityInstances(true).getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(modificationBuilderMock).cancelAllForActivity("activityId", true);
    verify(modificationBuilderMock).executeAsync();
  }

  @Test
  void executeCancellationWithoutActiveFlagSync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").cancelCurrentActiveActivityInstances(false).getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);

    verify(modificationBuilderMock).cancelAllForActivity("activityId");
    verify(modificationBuilderMock).execute();
  }

  @Test
  void executeCancellationWithoutActiveFlagAsync() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").cancelCurrentActiveActivityInstances(false).getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(modificationBuilderMock).cancelAllForActivity("activityId");
    verify(modificationBuilderMock).executeAsync();
  }

  @Test
  void executeSyncModificationWithAnnotation() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipIoMappings", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").cancelCurrentActiveActivityInstances(false).getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");
    json.put("annotation", "anAnnotation");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_SYNC_URL);

    verify(modificationBuilderMock).skipIoMappings();
    verify(modificationBuilderMock).cancelAllForActivity("activityId");
    verify(modificationBuilderMock).setAnnotation("anAnnotation");
    verify(modificationBuilderMock).execute();
  }

  @Test
  void executeAsyncModificationWithAnnotation() {
    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();

    json.put("skipCustomListeners", true);
    json.put("processInstanceIds", List.of("200", "100"));
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    json.put("instructions", instructions);
    json.put("processDefinitionId", "processDefinitionId");
    json.put("annotation", "anAnnotation");

    given()
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(EXECUTE_MODIFICATION_ASYNC_URL);

    verify(modificationBuilderMock).skipCustomListeners();
    verify(modificationBuilderMock).startBeforeActivity("activityId");
    verify(modificationBuilderMock).setAnnotation("anAnnotation");
    verify(modificationBuilderMock).executeAsync();
  }

}
