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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
    assertThat(resultType).isEqualTo(MessageCorrelationResultType.Execution.name());
    //execution should be filled and process instance should be null
    assertThat(from(content).<String>get("[" + idx + "].execution.id")).isEqualTo(MockProvider.EXAMPLE_EXECUTION_ID);
    assertThat(from(content).<String>get("[" + idx + "].execution.processInstanceId")).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
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
    assertThat(results).hasSize(2);
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
    assertThat(results).hasSize(2);
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
    assertThat(results).hasSize(4);
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

  @ParameterizedTest
  @MethodSource("postMessage_shouldReturnBadRequest_givenUnparseableParameterOfSupportedType_args")
  void postMessage_shouldReturnBadRequest_givenUnparseableParameterOfSupportedType(String parameterName, Class<?> conversionType) {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = conversionType.getSimpleName();

    Map<String, Object> variableJson = VariablesBuilder.create()
            .variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put(parameterName, variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
            .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
            .body("message", equalTo("Cannot deliver message: "
                    + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, conversionType)))
            .when().post(MESSAGE_URL);
  }

  static Stream<Arguments> postMessage_shouldReturnBadRequest_givenUnparseableParameterOfSupportedType_args() {
    return Stream.of(
            arguments("correlationKeys",                  Integer.class),
            arguments("localCorrelationKeys",             Integer.class),
            arguments("processVariables",                 Integer.class),
            arguments("processVariablesLocal",            Integer.class),
            arguments("processVariablesToTriggeredScope", Integer.class),
            arguments("correlationKeys",                  Short.class),
            arguments("localCorrelationKeys",             Short.class),
            arguments("processVariables",                 Short.class),
            arguments("processVariablesLocal",            Short.class),
            arguments("processVariablesToTriggeredScope", Short.class),
            arguments("correlationKeys",                  Long.class),
            arguments("localCorrelationKeys",             Long.class),
            arguments("processVariables",                 Long.class),
            arguments("processVariablesLocal",            Long.class),
            arguments("processVariablesToTriggeredScope", Long.class),
            arguments("correlationKeys",                  Double.class),
            arguments("localCorrelationKeys",             Double.class),
            arguments("processVariables",                 Double.class),
            arguments("processVariablesLocal",            Double.class),
            arguments("processVariablesToTriggeredScope", Double.class),
            arguments("correlationKeys",                  Date.class),
            arguments("localCorrelationKeys",             Date.class),
            arguments("processVariables",                 Date.class),
            arguments("processVariablesLocal",            Date.class),
            arguments("processVariablesToTriggeredScope", Date.class)
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
          "correlationKeys",
          "localCorrelationKeys",
          "processVariables",
          "processVariablesLocal",
          "processVariablesToTriggeredScope"
  })
  void postMessage_shouldReturnBadRequest_givenUnsupportedType(String parameterName) {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create()
            .variable(variableKey, variableValue, variableType).getVariables();

    String messageName = "aMessageName";

    Map<String, Object> messageParameters = new HashMap<>();
    messageParameters.put("messageName", messageName);
    messageParameters.put(parameterName, variableJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(messageParameters)
            .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
            .body("message", equalTo("Cannot deliver message: Unsupported value type 'X'"))
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
  void testCorrelateAllThrowsAuthorizationException() {
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
    assertThat(results).hasSize(1);
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
      assertThat(from(content).getMap(variablePath + ".valueInfo")).containsEntry("serializationDataFormat", MockProvider.FORMAT_APPLICATION_JSON);
      assertThat(from(content).<String>get(variablePath + ".value")).isEqualTo(MockProvider.EXAMPLE_VARIABLE_INSTANCE_SERIALIZED_VALUE);
      assertThat(from(content).<String>get(variablePath + ".type")).isEqualTo(VariableTypeHelper.toExpectedValueTypeName(ValueType.OBJECT));
    }

    assertThat(from(content).getMap("[" + idx + "].variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".valueInfo")).containsEntry("objectTypeName", ArrayList.class.getName());
    assertThat(from(content).getMap("[" + idx + "].variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".valueInfo")).containsEntry("objectTypeName", Object.class.getName());
  }

}
