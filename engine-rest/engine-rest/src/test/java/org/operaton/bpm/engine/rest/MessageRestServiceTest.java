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

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.MismatchingMessageCorrelationException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.EqualsMap;
import org.operaton.bpm.engine.rest.helper.ErrorMessageHelper;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.VariableTypeHelper;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.MessageCorrelationBuilder;
import org.operaton.bpm.engine.runtime.MessageCorrelationResult;
import org.operaton.bpm.engine.runtime.MessageCorrelationResultType;
import org.operaton.bpm.engine.runtime.MessageCorrelationResultWithVariables;
import org.operaton.bpm.engine.variable.type.ValueType;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.ws.rs.core.Response.Status;

public class MessageRestServiceTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String MESSAGE_URL = TEST_RESOURCE_ROOT_PATH +  MessageRestService.PATH;

  private RuntimeService runtimeServiceMock;
  private MessageCorrelationBuilder messageCorrelationBuilderMock;
  private MessageCorrelationResult executionResult;
  private MessageCorrelationResult procInstanceResult;
  private List<MessageCorrelationResult> executionResultList;
  private List<MessageCorrelationResult> procInstanceResultList;
  private List<MessageCorrelationResult> mixedResultList;

  private MessageCorrelationResultWithVariables executionResultWithVariables;
  private List<MessageCorrelationResultWithVariables> execResultWithVariablesList;

  @BeforeEach
  void setupMocks() {
    runtimeServiceMock = mock(RuntimeService.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeServiceMock);

    messageCorrelationBuilderMock = mock(MessageCorrelationBuilder.class);

    when(runtimeServiceMock.createMessageCorrelation(anyString())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.processInstanceId(anyString())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.processInstanceBusinessKey(anyString())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.processInstanceVariableEquals(anyString(), any())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.setVariables(Mockito.any())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.setVariable(anyString(), any())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.setVariablesLocal(Mockito.any())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.setVariableLocal(anyString(), any())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.setVariablesToTriggeredScope(Mockito.any())).thenReturn(messageCorrelationBuilderMock);
    when(messageCorrelationBuilderMock.setVariableToTriggeredScope(anyString(), any())).thenReturn(messageCorrelationBuilderMock);

    executionResult = MockProvider.createMessageCorrelationResult(MessageCorrelationResultType.Execution);
    procInstanceResult = MockProvider.createMessageCorrelationResult(MessageCorrelationResultType.ProcessDefinition);
    executionResultList = MockProvider.createMessageCorrelationResultList(MessageCorrelationResultType.Execution);
    procInstanceResultList = MockProvider.createMessageCorrelationResultList(MessageCorrelationResultType.ProcessDefinition);
    mixedResultList = new ArrayList<>(executionResultList);
    mixedResultList.addAll(procInstanceResultList);

    executionResultWithVariables = MockProvider.createMessageCorrelationResultWithVariables(MessageCorrelationResultType.Execution);
    execResultWithVariablesList = new ArrayList<>();
    execResultWithVariablesList.add(executionResultWithVariables);

  }

  @Test
  void testFullMessageCorrelation() {
    String messageName = "aMessageName";
    String businessKey = "aBusinessKey";
    Map<String, Object> variables = VariablesBuilder.create().variable("aKey", "aValue").getVariables();
    Map<String, Object> variablesLocal = VariablesBuilder.create().variable("aKeyLocal", "aValueLocal").getVariables();
    Map<String, Object> variablesToTriggeredScope = VariablesBuilder.create().variable("aKeyToTriggeredScope", "aValueToTriggeredScope").getVariables();

    Map<String, Object> correlationKeys = VariablesBuilder.create()
        .variable("aKey", "aValue")
        .variable("anotherKey", 1)
        .variable("aThirdKey", true).getVariables();

    Map<String, Object> localCorrelationKeys = VariablesBuilder.create()
        .variable("aLocalKey", "aValue")
        .variable("anotherLocalKey", 1)
        .variable("aThirdLocalKey", false).getVariables();

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", correlationKeys);
    messageParameters.put("localCorrelationKeys", localCorrelationKeys);
    messageParameters.put("processVariables", variables);
    messageParameters.put("processVariablesLocal", variablesLocal);
    messageParameters.put("processVariablesToTriggeredScope", variablesToTriggeredScope);
    messageParameters.put("businessKey", businessKey);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(MESSAGE_URL);

    Map<String, Object> expectedCorrelationKeys = new HashMap<>();
    expectedCorrelationKeys.put("aKey", "aValue");
    expectedCorrelationKeys.put("anotherKey", 1);
    expectedCorrelationKeys.put("aThirdKey", true);

    Map<String, Object> expectedLocalCorrelationKeys = new HashMap<>();
    expectedLocalCorrelationKeys.put("aLocalKey", "aValue");
    expectedLocalCorrelationKeys.put("anotherLocalKey", 1);
    expectedLocalCorrelationKeys.put("aThirdLocalKey", false);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aKey", "aValue");
    Map<String, Object> expectedVariablesLocal = new HashMap<>();
    expectedVariablesLocal.put("aKeyLocal", "aValueLocal");
    Map<String, Object> expectedVariablesToTriggeredScope = new HashMap<>();
    expectedVariablesToTriggeredScope.put("aKeyToTriggeredScope", "aValueToTriggeredScope");

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).processInstanceBusinessKey(businessKey);
    verify(messageCorrelationBuilderMock).setVariables(argThat(new EqualsMap(expectedVariables)));
    verify(messageCorrelationBuilderMock).setVariablesLocal(argThat(new EqualsMap(expectedVariablesLocal)));
    verify(messageCorrelationBuilderMock).setVariablesToTriggeredScope(argThat(new EqualsMap(expectedVariablesToTriggeredScope)));

    for (Entry<String, Object> expectedKey : expectedCorrelationKeys.entrySet()) {
      String name = expectedKey.getKey();
      Object value = expectedKey.getValue();
      verify(messageCorrelationBuilderMock).processInstanceVariableEquals(name, value);
    }

    for (Entry<String, Object> expectedLocalKey : expectedLocalCorrelationKeys.entrySet()) {
      String name = expectedLocalKey.getKey();
      Object value = expectedLocalKey.getValue();
      verify(messageCorrelationBuilderMock).localVariableEquals(name, value);
    }

    verify(messageCorrelationBuilderMock).correlateWithResult();
  }

  @Test
  void testFullMessageCorrelationWithExecutionResult() {
    //given
    when(messageCorrelationBuilderMock.correlateWithResult()).thenReturn(executionResult);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("resultEnabled", true);

    //when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.OK.getStatusCode())
    .when().post(MESSAGE_URL);

    //then
    assertThat(response).isNotNull();
    String content = response.asString();
    assertThat(!content.isEmpty()).isTrue();
    checkExecutionResult(content, 0);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateWithResult();
  }

  protected void checkExecutionResult(String content, int idx) {
    //resultType should be execution
    String resultType = from(content).get("[" + idx + "].resultType").toString();
    assertEquals(MessageCorrelationResultType.Execution.name(), resultType);
    //execution should be filled and process instance should be null
    assertEquals(MockProvider.EXAMPLE_EXECUTION_ID, from(content).get("[" + idx + "].execution.id"));
    assertEquals(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, from(content).get("[" + idx + "].execution.processInstanceId"));
    assertThat(from(content).<String>get("[" + idx + "].processInstance")).isNull();
  }

  @Test
  void testFullMessageCorrelationWithProcessDefinitionResult() {
    //given
    when(messageCorrelationBuilderMock.correlateWithResult()).thenReturn(procInstanceResult);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("resultEnabled", true);

    //when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.OK.getStatusCode())
    .when().post(MESSAGE_URL);

    //then
    assertThat(response).isNotNull();
    String content = response.asString();
    assertThat(!content.isEmpty()).isTrue();
    checkProcessInstanceResult(content, 0);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateWithResult();
  }

  protected void checkProcessInstanceResult(String content, int idx) {
    //resultType should be set to process definition
    String resultType = from(content).get("[" + idx + "].resultType");
    assertThat(resultType).isEqualTo(MessageCorrelationResultType.ProcessDefinition.name());

    //process instance should be filled and execution should be null
    assertThat(from(content).<String>get("[" + idx + "].processInstance.id")).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(from(content).<String>get("[" + idx + "].processInstance.definitionId")).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(from(content).<String>get("[" + idx + "].processInstance.definitionKey")).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    assertThat(from(content).<String>get("[" + idx + "].execution")).isNull();
  }

  @Test
  void testFullMessageCorrelationAll() {
    String messageName = "aMessageName";
    String businessKey = "aBusinessKey";
    Map<String, Object> variables = VariablesBuilder.create().variable("aKey", "aValue").getVariables();
    Map<String, Object> variablesLocal = VariablesBuilder.create().variable("aKeyLocal", "aValueLocal").getVariables();
    Map<String, Object> variablesToTriggeredScope = VariablesBuilder.create().variable("aKeyToTriggeredScope", "aValueToTriggeredScope").getVariables();

    Map<String, Object> correlationKeys = VariablesBuilder.create()
        .variable("aKey", "aValue")
        .variable("anotherKey", 1)
        .variable("aThirdKey", true).getVariables();

    Map<String, Object> localCorrelationKeys = VariablesBuilder.create()
        .variable("aLocalKey", "aValue")
        .variable("anotherLocalKey", 1)
        .variable("aThirdLocalKey", false).getVariables();

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", correlationKeys);
    messageParameters.put("localCorrelationKeys", localCorrelationKeys);
    messageParameters.put("processVariables", variables);
    messageParameters.put("processVariablesLocal", variablesLocal);
    messageParameters.put("processVariablesToTriggeredScope", variablesToTriggeredScope);
    messageParameters.put("businessKey", businessKey);
    messageParameters.put("all", true);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(MESSAGE_URL);

    Map<String, Object> expectedCorrelationKeys = new HashMap<>();
    expectedCorrelationKeys.put("aKey", "aValue");
    expectedCorrelationKeys.put("anotherKey", 1);
    expectedCorrelationKeys.put("aThirdKey", true);

    Map<String, Object> expectedLocalCorrelationKeys = new HashMap<>();
    expectedLocalCorrelationKeys.put("aLocalKey", "aValue");
    expectedLocalCorrelationKeys.put("anotherLocalKey", 1);
    expectedLocalCorrelationKeys.put("aThirdLocalKey", false);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aKey", "aValue");
    Map<String, Object> expectedVariablesLocal = new HashMap<>();
    expectedVariablesLocal.put("aKeyLocal", "aValueLocal");
    Map<String, Object> expectedVariablesToTriggeredScope = new HashMap<>();
    expectedVariablesToTriggeredScope.put("aKeyToTriggeredScope", "aValueToTriggeredScope");

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).processInstanceBusinessKey(businessKey);
    verify(messageCorrelationBuilderMock).setVariables(argThat(new EqualsMap(expectedVariables)));
    verify(messageCorrelationBuilderMock).setVariablesLocal(argThat(new EqualsMap(expectedVariablesLocal)));
    verify(messageCorrelationBuilderMock).setVariablesToTriggeredScope(argThat(new EqualsMap(expectedVariablesToTriggeredScope)));

    for (Entry<String, Object> expectedKey : expectedCorrelationKeys.entrySet()) {
      String name = expectedKey.getKey();
      Object value = expectedKey.getValue();
      verify(messageCorrelationBuilderMock).processInstanceVariableEquals(name, value);
    }

    for (Entry<String, Object> expectedLocalKey : expectedLocalCorrelationKeys.entrySet()) {
      String name = expectedLocalKey.getKey();
      Object value = expectedLocalKey.getValue();
      verify(messageCorrelationBuilderMock).localVariableEquals(name, value);
    }

    verify(messageCorrelationBuilderMock).correlateAllWithResult();
  }

  @Test
  void testFullMessageCorrelationAllWithExecutionResult() {
    //given
    when(messageCorrelationBuilderMock.correlateAllWithResult()).thenReturn(executionResultList);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("all", true);
    messageParameters.put("resultEnabled", true);

    //when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.OK.getStatusCode())
    .when().post(MESSAGE_URL);

    //then
    assertThat(response).isNotNull();
    String content = response.asString();
    assertThat(!content.isEmpty()).isTrue();

    List<HashMap> results = from(content).getList("");
    assertEquals(2, results.size());
    for (int i = 0; i < 2; i++) {
      checkExecutionResult(content, i);
    }

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateAllWithResult();
  }

  @Test
  void testFullMessageCorrelationAllWithProcessInstanceResult() {
    //given
    when(messageCorrelationBuilderMock.correlateAllWithResult()).thenReturn(procInstanceResultList);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("all", true);
    messageParameters.put("resultEnabled", true);

    //when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.OK.getStatusCode())
    .when().post(MESSAGE_URL);

   //then
   assertThat(response).isNotNull();
    String content = response.asString();
   assertThat(!content.isEmpty()).isTrue();

    List<HashMap> results = from(content).getList("");
    assertEquals(2, results.size());
    for (int i = 0; i < 2; i++) {
      checkProcessInstanceResult(content, i);
    }

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateAllWithResult();
  }

  @Test
  void testFullMessageCorrelationAllWithMixedResult() {
    //given
    when(messageCorrelationBuilderMock.correlateAllWithResult()).thenReturn(mixedResultList);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("all", true);
    messageParameters.put("resultEnabled", true);

    //when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.OK.getStatusCode())
    .when().post(MESSAGE_URL);

    //then
    assertThat(response).isNotNull();
    String content = response.asString();
    assertThat(!content.isEmpty()).isTrue();

    List<HashMap> results = from(content).getList("");
    assertEquals(4, results.size());
    for (int i = 0; i < 2; i++) {
      String resultType = from(content).get("[" + i + "].resultType");
      assertThat(resultType).isNotNull();
      if (resultType.equals(MessageCorrelationResultType.Execution.name())) {
        checkExecutionResult(content, i);
      } else {
        checkProcessInstanceResult(content, i);
      }
    }

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateAllWithResult();
  }


  @Test
  void testFullMessageCorrelationAllWithNoResult() {
    //given
    when(messageCorrelationBuilderMock.correlateAllWithResult()).thenReturn(mixedResultList);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("all", true);

    //when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .statusCode(Status.NO_CONTENT.getStatusCode())
    .when().post(MESSAGE_URL);

    //then
    assertThat(response).isNotNull();
    String content = response.asString();
    assertThat(content).isEmpty();

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateAllWithResult();
  }

  @Test
  void testMessageNameOnlyCorrelation() {
    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(MESSAGE_URL);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateWithResult();
    verifyNoMoreInteractions(messageCorrelationBuilderMock);
  }

  @Test
  void testMessageNameAndBusinessKeyCorrelation() {
    String messageName = "aMessageName";
    String businessKey = "aBusinessKey";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("businessKey", businessKey);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(MESSAGE_URL);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).processInstanceBusinessKey(businessKey);
    verify(messageCorrelationBuilderMock).correlateWithResult();
    verifyNoMoreInteractions(messageCorrelationBuilderMock);

  }

  @Test
  void testMessageNameAndBusinessKeyCorrelationAll() {
    String messageName = "aMessageName";
    String businessKey = "aBusinessKey";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("businessKey", businessKey);
    messageParameters.put("all", true);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(MESSAGE_URL);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).processInstanceBusinessKey(businessKey);
    verify(messageCorrelationBuilderMock).correlateAllWithResult();
    verifyNoMoreInteractions(messageCorrelationBuilderMock);

  }

  @Test
  void testMismatchingCorrelation() {
    String messageName = "aMessage";

    doThrow(new MismatchingMessageCorrelationException(messageName, "Expected exception: cannot correlate"))
      .when(messageCorrelationBuilderMock).correlateWithResult();

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", containsString("Expected exception: cannot correlate"))
      .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingInstantiation() {
    String messageName = "aMessage";

    // thrown, if instantiation of the process or signalling the instance fails
    doThrow(new ProcessEngineException("Expected exception"))
      .when(messageCorrelationBuilderMock).correlateWithResult();

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
      .body("message", equalTo("Expected exception"))
      .when().post(MESSAGE_URL);
  }

  @Test
  void testNoMessageNameCorrelation() {
    given().contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("No message name supplied"))
      .when().post(MESSAGE_URL);
  }

  @Test
  void testMessageCorrelationWithTenantId() {
    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("tenantId", MockProvider.EXAMPLE_TENANT_ID);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(MESSAGE_URL);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).tenantId(MockProvider.EXAMPLE_TENANT_ID);
    verify(messageCorrelationBuilderMock).correlateWithResult();
    verifyNoMoreInteractions(messageCorrelationBuilderMock);
  }

  @Test
  void testMessageCorrelationWithoutTenantId() {
    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("withoutTenantId", true);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(MESSAGE_URL);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).withoutTenantId();
    verify(messageCorrelationBuilderMock).correlateWithResult();
    verifyNoMoreInteractions(messageCorrelationBuilderMock);
  }

  @Test
  void testFailingInvalidTenantParameters() {
    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("tenantId", MockProvider.EXAMPLE_TENANT_ID);
    messageParameters.put("withoutTenantId", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(messageParameters)
    .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Parameter 'tenantId' cannot be used together with parameter 'withoutTenantId'."))
    .when()
      .post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableIntegerInCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableIntegerInLocalCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("localCorrelationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
        .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot deliver message: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
        .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableShortInCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableShortInLocalCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("localCorrelationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
        .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot deliver message: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
        .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableLongInCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableLongInLocalCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("localCorrelationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
        .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot deliver message: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
        .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDoubleInCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDoubleInLocalCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("localCorrelationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
        .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot deliver message: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
        .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDateInCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDateInLocalCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("localCorrelationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
        .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot deliver message: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
        .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToNotSupportedTypeInCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("correlationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: Unsupported value type 'X'"))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToNotSupportedTypeInLocalCorrelationKeys() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("localCorrelationKeys", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
        .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot deliver message: Unsupported value type 'X'"))
        .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableIntegerInProcessVariables() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariables", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableShortInProcessVariables() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariables", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableLongInProcessVariables() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariables", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDoubleInProcessVariables() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariables", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDateInProcessVariables() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariables", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToNotSupportedTypeInProcessVariables() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariables", variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: Unsupported value type 'X'"))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableIntegerInProcessVariablesLocal() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableLocalJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariablesLocal", variableLocalJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToNotSupportedTypeInProcessVariablesLocal() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableLocalJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariablesLocal", variableLocalJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: Unsupported value type 'X'"))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDateInProcessVariablesLocal() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableLocalJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariablesLocal", variableLocalJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableIntegerInProcessVariablesToTriggeredScope() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableToTriggeredScopeJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariablesToTriggeredScope", variableToTriggeredScopeJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(MESSAGE_URL);
  }


  @Test
  void testFailingDueToNotSupportedTypeInProcessVariablesToTriggeredScope() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableToTriggeredScopeJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariablesToTriggeredScope", variableToTriggeredScopeJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: Unsupported value type 'X'"))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testFailingDueToUnparseableDateInProcessVariablesToTriggeredScope() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableToTriggeredScopeJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processVariablesToTriggeredScope", variableToTriggeredScopeJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
    .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo("Cannot deliver message: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(MESSAGE_URL);
  }

  @Test
  void testCorrelateThrowsAuthorizationException() {
    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(messageCorrelationBuilderMock).correlateWithResult();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(messageParameters)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(MESSAGE_URL);
  }

  @Test
  void testcorrelateAllThrowsAuthorizationException() {
    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("all", true);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(messageCorrelationBuilderMock).correlateAllWithResult();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(messageParameters)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(MESSAGE_URL);
  }

  @Test
  void testMessageCorrelationWithProcessInstanceId() {
    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
       .body(messageParameters)
    .then()
      .expect().statusCode(Status.NO_CONTENT.getStatusCode())
    .when().post(MESSAGE_URL);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).processInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    verify(messageCorrelationBuilderMock).correlateWithResult();
  }

  @Test
  void testMessageCorrelationWithoutBusinessKey() {
    when(messageCorrelationBuilderMock.processInstanceBusinessKey(null))
      .thenThrow(new NullValueException());

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
       .body(messageParameters)
    .then()
      .expect().statusCode(Status.NO_CONTENT.getStatusCode())
    .when().post(MESSAGE_URL);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);

    verify(messageCorrelationBuilderMock, Mockito.never()).processInstanceBusinessKey(anyString());
    verify(messageCorrelationBuilderMock).correlateWithResult();
    verifyNoMoreInteractions(messageCorrelationBuilderMock);
  }

  @Test
  void testCorrelationWithVariablesInResult() {
    // given
    when(messageCorrelationBuilderMock.correlateWithResultAndVariables(false)).thenReturn(executionResultWithVariables);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("resultEnabled", true);
    messageParameters.put("variablesInResultEnabled", true);

    // when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.OK.getStatusCode())
    .when().post(MESSAGE_URL);

    // then
    assertThat(response).isNotNull();
    String content = response.asString();
    assertThat(!content.isEmpty()).isTrue();
    checkVariablesInResult(content, 0);
    checkExecutionResult(content, 0);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateWithResultAndVariables(false);
  }

  @Test
  void testCorrelationAllWithVariablesInResult() {
    // given
    when(messageCorrelationBuilderMock.correlateAllWithResultAndVariables(false)).thenReturn(execResultWithVariablesList);

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("all", true);
    messageParameters.put("resultEnabled", true);
    messageParameters.put("variablesInResultEnabled", true);

    // when
    Response response = given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.OK.getStatusCode())
    .when().post(MESSAGE_URL);

    // then
    assertThat(response).isNotNull();
    String content = response.asString();
    assertThat(!content.isEmpty()).isTrue();

    List<HashMap<Object, Object>> results = from(content).getList("");
    assertEquals(1, results.size());
    checkVariablesInResult(content, 0);

    verify(runtimeServiceMock).createMessageCorrelation(messageName);
    verify(messageCorrelationBuilderMock).correlateAllWithResultAndVariables(false);
  }

  @Test
  void testFailingCorrelationWithVariablesInResultDueToDisabledResult() {
    // given
    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put("resultEnabled", false);
    messageParameters.put("variablesInResultEnabled", true);

    // when/
    given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.BAD_REQUEST.getStatusCode())
           .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
           .body("message", equalTo("Parameter 'variablesInResultEnabled' cannot be used without 'resultEnabled' set to true."))
    .when().post(MESSAGE_URL);
  }

  @Test
  void shouldReturnErrorOnMessageCorrelation() {
    // given
    doThrow(new ProcessEngineException("foo", 123))
        .when(messageCorrelationBuilderMock).correlateWithResult();

    String messageName = "aMessageName";
    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);

    // when/
    given().contentType(POST_JSON_CONTENT_TYPE)
           .body(messageParameters)
    .then().expect()
           .contentType(ContentType.JSON)
           .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
           .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
           .body("message", equalTo("foo"))
           .body("code", equalTo(123))
    .when().post(MESSAGE_URL);
  }

  protected void checkVariablesInResult(String content, int idx) {
    List<String> variableNames = java.util.Arrays.asList(MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME, MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME);

    for (String variableName : variableNames) {
      String variablePath = "[" + idx + "].variables." + variableName;
      assertEquals(MockProvider.FORMAT_APPLICATION_JSON, from(content).getMap(variablePath + ".valueInfo").get("serializationDataFormat"));
      assertEquals(MockProvider.EXAMPLE_VARIABLE_INSTANCE_SERIALIZED_VALUE, from(content).get(variablePath + ".value"));
      assertEquals(VariableTypeHelper.toExpectedValueTypeName(ValueType.OBJECT), from(content).get(variablePath + ".type"));
    }

    assertEquals(ArrayList.class.getName(),
        from(content).getMap("[" + idx + "].variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".valueInfo").get("objectTypeName"));
    assertEquals(Object.class.getName(),
        from(content).getMap("[" + idx + "].variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".valueInfo").get("objectTypeName"));
  }

}
