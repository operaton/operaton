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

import java.util.HashMap;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.impl.SignalEventReceivedBuilderImpl;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.SignalEventReceivedBuilder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Tassilo Weidner
 */
public class SignalRestServiceTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String SIGNAL_URL = TEST_RESOURCE_ROOT_PATH +  SignalRestService.PATH;

  private RuntimeService runtimeServiceMock;
  private SignalEventReceivedBuilder signalBuilderMock;

  @BeforeEach
  void setupMocks() {
    runtimeServiceMock = mock(RuntimeService.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeServiceMock);

    signalBuilderMock = mock(SignalEventReceivedBuilderImpl.class);
    when(runtimeServiceMock.createSignalEvent(anyString())).thenReturn(signalBuilderMock);
    when(signalBuilderMock.setVariables(Mockito.any())).thenReturn(signalBuilderMock);
    when(signalBuilderMock.executionId(anyString())).thenReturn(signalBuilderMock);
    when(signalBuilderMock.tenantId(anyString())).thenReturn(signalBuilderMock);
    when(signalBuilderMock.withoutTenantId()).thenReturn(signalBuilderMock);
  }

  @Test
  void shouldBroadcast() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent(requestBody.get("name"));
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldBroadcastWithVariables() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("variables",
      VariablesBuilder.create()
      .variable("total", 420)
      .variable("invoiceId", "ABC123")
      .getVariables());

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("total", 420);
    expectedVariables.put("invoiceId", "ABC123");
    verify(signalBuilderMock).setVariables(expectedVariables);
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldBroadcastWithTenant() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("tenantId", "aTenantId");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    verify(signalBuilderMock).tenantId((String) requestBody.get("tenantId"));
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldBroadcastWithVariablesAndTenant() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("variables",
      VariablesBuilder.create()
      .variable("total", 420)
      .variable("invoiceId", "ABC123")
      .getVariables());
    requestBody.put("tenantId", "aTenantId");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("total", 420);
    expectedVariables.put("invoiceId", "ABC123");
    verify(signalBuilderMock).setVariables(expectedVariables);
    verify(signalBuilderMock).tenantId((String) requestBody.get("tenantId"));
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldBroadcastWithoutTenant() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("withoutTenantId", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    verify(signalBuilderMock).withoutTenantId();
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldBroadcastWithoutTenantAndWithVariables() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("variables",
      VariablesBuilder.create()
      .variable("total", 420)
      .variable("invoiceId", "ABC123")
      .getVariables());
    requestBody.put("withoutTenantId", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("total", 420);
    expectedVariables.put("invoiceId", "ABC123");
    verify(signalBuilderMock).setVariables(expectedVariables);
    verify(signalBuilderMock).withoutTenantId();
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldDeliverToSingleExecution() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("executionId", "anExecutionId");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent(requestBody.get("name"));
    verify(signalBuilderMock).executionId(requestBody.get("executionId"));
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldDeliverToSingleExecutionWithVariables() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("executionId", "anExecutionId");
    requestBody.put("variables",
      VariablesBuilder.create()
      .variable("total", 420)
      .variable("invoiceId", "ABC123")
      .getVariables());

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("total", 420);
    expectedVariables.put("invoiceId", "ABC123");
    verify(signalBuilderMock).setVariables(expectedVariables);
    verify(signalBuilderMock).executionId((String) requestBody.get("executionId"));
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldDeliverToSingleExecutionWithTenant() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("tenantId", "aTenantId");
    requestBody.put("executionId", "anExecutionId");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    verify(signalBuilderMock).tenantId((String) requestBody.get("tenantId"));
    verify(signalBuilderMock).executionId((String) requestBody.get("executionId"));
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldDeliverToSingleExecutionWithVariablesAndTenant() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("executionId", "anExecutionId");
    requestBody.put("variables",
      VariablesBuilder.create()
      .variable("total", 420)
      .variable("invoiceId", "ABC123")
      .getVariables());
    requestBody.put("tenantId", "aTenantId");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    verify(signalBuilderMock).executionId((String) requestBody.get("executionId"));
    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("total", 420);
    expectedVariables.put("invoiceId", "ABC123");
    verify(signalBuilderMock).setVariables(expectedVariables);
    verify(signalBuilderMock).tenantId((String) requestBody.get("tenantId"));
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldDeliverToSingleExecutionWithoutTenant() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("executionId", "anExecutionId");
    requestBody.put("withoutTenantId", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    verify(signalBuilderMock).executionId((String) requestBody.get("executionId"));
    verify(signalBuilderMock).withoutTenantId();
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldDeliverToSingleExecutionWithoutTenantAndWithVariables() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("executionId", "anExecutionId");
    requestBody.put("withoutTenantId", true);
    requestBody.put("variables",
      VariablesBuilder.create()
      .variable("total", 420)
      .variable("invoiceId", "ABC123")
      .getVariables());

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SIGNAL_URL);

    verify(runtimeServiceMock).createSignalEvent((String) requestBody.get("name"));
    verify(signalBuilderMock).executionId((String) requestBody.get("executionId"));
    verify(signalBuilderMock).withoutTenantId();
    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("total", 420);
    expectedVariables.put("invoiceId", "ABC123");
    verify(signalBuilderMock).setVariables(expectedVariables);
    verify(signalBuilderMock).send();
    verifyNoMoreInteractions(signalBuilderMock);
  }

  @Test
  void shouldThrowExceptionByMissingName() {
    Map<String, Object> requestBody = new HashMap<>();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("No signal name given"))
    .when()
      .post(SIGNAL_URL);
  }

  @Test
  void shouldThrowBadUserRequestException() {
    String message = "expected exception";
    doThrow(new BadUserRequestException(message)).when(signalBuilderMock).send();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(BadUserRequestException.class.getSimpleName()))
        .body("message", equalTo(message))
    .when()
      .post(SIGNAL_URL);
  }

  @Test
  void shouldThrowAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(signalBuilderMock).send();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", equalTo(AuthorizationException.class.getSimpleName()))
        .body("message", equalTo(message))
    .when()
      .post(SIGNAL_URL);
  }

  @Test
  void shouldThrowProcessEngineException() {
    String message = "expected exception";
    doThrow(new ProcessEngineException(message)).when(signalBuilderMock).send();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
        .body("message", equalTo(message))
    .when()
      .post(SIGNAL_URL);
  }

  @Test
  void shouldReturnInternalServerErrorResponseForNotFoundException() {
    String message = "expected exception";
    doThrow(new NotFoundException(message)).when(signalBuilderMock).send();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");
    requestBody.put("executionId", "foo");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", equalTo(message))
    .when()
      .post(SIGNAL_URL);
  }

  @Test
  void shouldReturnInternalServerErrorResponseJsonWithTypeAndMessage() {
    String message = "expected exception";
    doThrow(new IllegalArgumentException(message)).when(signalBuilderMock).send();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", equalTo(IllegalArgumentException.class.getSimpleName()))
        .body("message", equalTo(message))
    .when()
      .post(SIGNAL_URL);
  }

  @Test
  void shouldReturnError() {
    doThrow(new ProcessEngineException("foo", 123))
        .when(signalBuilderMock).send();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "aSignalName");

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(requestBody)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
        .body("message", equalTo("foo"))
        .body("code", equalTo(123))
    .when()
      .post(SIGNAL_URL);
  }

}
