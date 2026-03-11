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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.rest.dto.runtime.VariableNameDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.ErrorMessageHelper;
import org.operaton.bpm.engine.rest.helper.MockObjectValue;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.VariableTypeHelper;
import org.operaton.bpm.engine.rest.helper.variable.EqualsNullValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsObjectValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsPrimitiveValue;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.SerializableValueType;
import org.operaton.bpm.engine.variable.value.BooleanValue;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.operaton.bpm.engine.rest.util.DateTimeUtils.DATE_FORMAT_WITH_TIMEZONE;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
*
* @author Roman Smirnov
*
*/
public class CaseExecutionRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String CASE_EXECUTION_URL = TEST_RESOURCE_ROOT_PATH + "/case-execution";
  protected static final String SINGLE_CASE_EXECUTION_URL = CASE_EXECUTION_URL + "/{id}";

  protected static final String CASE_EXECUTION_MANUAL_START_URL = SINGLE_CASE_EXECUTION_URL + "/manual-start";
  protected static final String CASE_EXECUTION_REENABLE_URL = SINGLE_CASE_EXECUTION_URL + "/reenable";
  protected static final String CASE_EXECUTION_DISABLE_URL = SINGLE_CASE_EXECUTION_URL + "/disable";
  protected static final String CASE_EXECUTION_COMPLETE_URL = SINGLE_CASE_EXECUTION_URL + "/complete";
  protected static final String CASE_EXECUTION_TERMINATE_URL = SINGLE_CASE_EXECUTION_URL + "/terminate";

  protected static final String CASE_EXECUTION_LOCAL_VARIABLES_URL = SINGLE_CASE_EXECUTION_URL + "/localVariables";
  protected static final String CASE_EXECUTION_VARIABLES_URL = SINGLE_CASE_EXECUTION_URL + "/variables";
  protected static final String SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL = CASE_EXECUTION_LOCAL_VARIABLES_URL + "/{varId}";
  protected static final String SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL = SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL + "/data";
  protected static final String SINGLE_CASE_EXECUTION_VARIABLE_URL = CASE_EXECUTION_VARIABLES_URL + "/{varId}";
  protected static final String SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL = SINGLE_CASE_EXECUTION_VARIABLE_URL + "/data";

  private CaseService caseServiceMock;
  private CaseExecutionQuery caseExecutionQueryMock;
  private CaseExecutionCommandBuilder caseExecutionCommandBuilderMock;

  @BeforeEach
  void setUpRuntime() {
    CaseExecution mockCaseExecution = MockProvider.createMockCaseExecution();

    caseServiceMock = mock(CaseService.class);

    when(processEngine.getCaseService()).thenReturn(caseServiceMock);

    caseExecutionQueryMock = mock(CaseExecutionQuery.class);

    when(caseServiceMock.createCaseExecutionQuery()).thenReturn(caseExecutionQueryMock);
    when(caseExecutionQueryMock.caseExecutionId(MockProvider.EXAMPLE_CASE_EXECUTION_ID)).thenReturn(caseExecutionQueryMock);
    when(caseExecutionQueryMock.singleResult()).thenReturn(mockCaseExecution);

    when(caseServiceMock.getVariableTyped(anyString(), anyString(), eq(true))).thenReturn(EXAMPLE_VARIABLE_VALUE);
    when(caseServiceMock.getVariablesTyped(anyString(), eq(true))).thenReturn(EXAMPLE_VARIABLES);

    when(caseServiceMock.getVariableLocalTyped(anyString(), eq(EXAMPLE_VARIABLE_KEY), anyBoolean())).thenReturn(EXAMPLE_VARIABLE_VALUE);
    when(caseServiceMock.getVariableLocalTyped(anyString(), eq(EXAMPLE_BYTES_VARIABLE_KEY), eq(false))).thenReturn(EXAMPLE_VARIABLE_VALUE_BYTES);
    when(caseServiceMock.getVariablesLocalTyped(anyString(), eq(true))).thenReturn(EXAMPLE_VARIABLES);

    when(caseServiceMock.getVariablesTyped(anyString(), Mockito.any(), eq(true))).thenReturn(EXAMPLE_VARIABLES);
    when(caseServiceMock.getVariablesLocalTyped(anyString(), Mockito.any(), eq(true))).thenReturn(EXAMPLE_VARIABLES);

    caseExecutionCommandBuilderMock = mock(CaseExecutionCommandBuilder.class);

    when(caseServiceMock.withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID)).thenReturn(caseExecutionCommandBuilderMock);

    when(caseExecutionCommandBuilderMock.setVariable(anyString(), any())).thenReturn(caseExecutionCommandBuilderMock);
    when(caseExecutionCommandBuilderMock.setVariableLocal(anyString(), any())).thenReturn(caseExecutionCommandBuilderMock);
    when(caseExecutionCommandBuilderMock.setVariables(Mockito.any())).thenReturn(caseExecutionCommandBuilderMock);
    when(caseExecutionCommandBuilderMock.setVariablesLocal(Mockito.any())).thenReturn(caseExecutionCommandBuilderMock);

    when(caseExecutionCommandBuilderMock.removeVariable(anyString())).thenReturn(caseExecutionCommandBuilderMock);
    when(caseExecutionCommandBuilderMock.removeVariableLocal(anyString())).thenReturn(caseExecutionCommandBuilderMock);
    when(caseExecutionCommandBuilderMock.removeVariables(Mockito.any())).thenReturn(caseExecutionCommandBuilderMock);
    when(caseExecutionCommandBuilderMock.removeVariablesLocal(Mockito.any())).thenReturn(caseExecutionCommandBuilderMock);

  }

  @Test
  void testCaseExecutionRetrieval() {
    Map<String, String> params = new HashMap<>();
    params.put("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_ID))
        .body("caseInstanceId", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_CASE_INSTANCE_ID))
        .body("parentId", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_PARENT_ID))
        .body("caseDefinitionId", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_CASE_DEFINITION_ID))
        .body("activityId", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_ACTIVITY_ID))
        .body("activityName", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_ACTIVITY_NAME))
        .body("activityType", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_ACTIVITY_TYPE))
        .body("activityDescription", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_ACTIVITY_DESCRIPTION))
        .body("tenantId", equalTo(MockProvider.EXAMPLE_TENANT_ID))
        .body("required", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_IS_REQUIRED))
        .body("active", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_IS_ACTIVE))
        .body("enabled", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_IS_ENABLED))
        .body("disabled", equalTo(MockProvider.EXAMPLE_CASE_EXECUTION_IS_DISABLED))
    .when()
      .get(SINGLE_CASE_EXECUTION_URL);

    verify(caseServiceMock).createCaseExecutionQuery();
    verify(caseExecutionQueryMock).caseExecutionId(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionQueryMock).singleResult();
  }

  @Test
  void testManualStart() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testUnsuccessfulManualStart() {
    doThrow(new NotValidException("expected exception")).when(caseExecutionCommandBuilderMock).manualStart();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Cannot manualStart case execution " + MockProvider.EXAMPLE_CASE_EXECUTION_ID + ": expected exception"))
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithSetVariable() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithSetVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer", true)
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithSetVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey, true);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithRemoveVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithSetVariableAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithSetVariableAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithSetVariableLocalAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testManualStartWithSetVariableLocalAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_MANUAL_START_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).manualStart();
  }

  @Test
  void testDisable() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testUnsuccessfulDisable() {
    doThrow(new NotValidException("expected exception")).when(caseExecutionCommandBuilderMock).disable();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Cannot disable case execution " + MockProvider.EXAMPLE_CASE_EXECUTION_ID + ": expected exception"))
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithSetVariable() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithSetVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer", true)
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithSetVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey, true);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithRemoveVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithSetVariableAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithSetVariableAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithSetVariableLocalAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testDisableWithSetVariableLocalAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_DISABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).disable();
  }

  @Test
  void testReenable() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testUnsuccessfulReenable() {
    doThrow(new NotValidException("expected exception")).when(caseExecutionCommandBuilderMock).reenable();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Cannot reenable case execution " + MockProvider.EXAMPLE_CASE_EXECUTION_ID + ": expected exception"))
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithSetVariable() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithSetVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer", true)
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithSetVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey, true);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithRemoveVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithSetVariableAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithSetVariableAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithSetVariableLocalAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testReenableWithSetVariableLocalAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_REENABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).reenable();
  }

  @Test
  void testGetLocalVariables() {
    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .body(EXAMPLE_VARIABLE_KEY, notNullValue())
          .body(EXAMPLE_VARIABLE_KEY + ".value", equalTo(EXAMPLE_VARIABLE_VALUE.getValue()))
          .body(EXAMPLE_VARIABLE_KEY + ".type", equalTo("String"))
      .when()
        .get(CASE_EXECUTION_LOCAL_VARIABLES_URL);

    assertThat(response.jsonPath().getMap("")).as("Should return exactly one variable").hasSize(1);

    verify(caseServiceMock).getVariablesLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, true);
  }

  @Test
  void testGetVariables() {
    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .body(EXAMPLE_VARIABLE_KEY, notNullValue())
          .body(EXAMPLE_VARIABLE_KEY + ".value", equalTo(EXAMPLE_VARIABLE_VALUE.getValue()))
          .body(EXAMPLE_VARIABLE_KEY + ".type",
              equalTo(VariableTypeHelper.toExpectedValueTypeName(EXAMPLE_VARIABLE_VALUE.getType())))
      .when()
        .get(CASE_EXECUTION_VARIABLES_URL);

    assertThat(response.jsonPath().getMap("")).as("Should return exactly one variable").hasSize(1);

    verify(caseServiceMock).getVariablesTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, true);
  }

  @Test
  void testGetLocalObjectVariables() {
    // given
    String variableKey = "aVariableId";

    List<String> payload = List.of("a", "b");
    ObjectValue variableValue =
        MockObjectValue
            .fromObjectValue(Variables
                .objectValue(payload)
                .serializationDataFormat("application/json")
                .create())
            .objectTypeName(ArrayList.class.getName())
            .serializedValue("a serialized value"); // this should differ from the serialized json

    when(caseServiceMock.getVariablesLocalTyped(eq(MockProvider.EXAMPLE_CASE_EXECUTION_ID), anyBoolean()))
      .thenReturn(Variables.createVariables().putValueTyped(variableKey, variableValue));

    // when
    given().pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body(variableKey + ".value", equalTo(payload))
      .body(variableKey + ".type", equalTo("Object"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, equalTo("application/json"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, equalTo(ArrayList.class.getName()))
      .when().get(CASE_EXECUTION_LOCAL_VARIABLES_URL);

    // then
    verify(caseServiceMock).getVariablesLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, true);
  }

  @Test
  void testGetLocalObjectVariablesSerialized() {
    // given
    String variableKey = "aVariableId";

    ObjectValue variableValue =
        Variables
          .serializedObjectValue("a serialized value")
          .serializationDataFormat("application/json")
          .objectTypeName(ArrayList.class.getName())
          .create();

    when(caseServiceMock.getVariablesLocalTyped(eq(MockProvider.EXAMPLE_CASE_EXECUTION_ID), anyBoolean()))
      .thenReturn(Variables.createVariables().putValueTyped(variableKey, variableValue));

    // when
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .queryParam("deserializeValues", false)
    .then().expect().statusCode(Status.OK.getStatusCode())
      .body(variableKey + ".value", equalTo("a serialized value"))
      .body(variableKey + ".type", equalTo("Object"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, equalTo("application/json"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, equalTo(ArrayList.class.getName()))
      .when().get(CASE_EXECUTION_LOCAL_VARIABLES_URL);

    // then
    verify(caseServiceMock).getVariablesLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, false);
  }

  @Test
  void testGetLocalVariablesForNonExistingExecution() {
    when(caseServiceMock.getVariablesLocalTyped(anyString(), anyBoolean())).thenThrow(new ProcessEngineException("expected exception"));

    given()
      .pathParam("id", "aNonExistingExecutionId")
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
        .body("message", equalTo("expected exception"))
      .when()
        .get(CASE_EXECUTION_LOCAL_VARIABLES_URL);

    verify(caseServiceMock).getVariablesLocalTyped("aNonExistingExecutionId", true);
  }

  @Test
  void testGetVariablesForNonExistingExecution() {
    when(caseServiceMock.getVariablesTyped(anyString(), anyBoolean())).thenThrow(new ProcessEngineException("expected exception"));

    given()
      .pathParam("id", "aNonExistingExecutionId")
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
        .body("message", equalTo("expected exception"))
      .when()
        .get(CASE_EXECUTION_VARIABLES_URL);

    verify(caseServiceMock).getVariablesTyped("aNonExistingExecutionId", true);
  }

  @Test
  void testGetFileVariable() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    String mimeType = "text/plain";
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(mimeType).create();

    when(caseServiceMock.getVariableTyped(MockProvider.EXAMPLE_CASE_INSTANCE_ID, variableKey, true))
    .thenReturn(variableValue);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON.toString())
    .and()
      .body("valueInfo.mimeType", equalTo(mimeType))
      .body("valueInfo.filename", equalTo(filename))
      .body("value", nullValue())
    .when().get(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testGetNullFileVariable() {
    String variableKey = "aVariableKey";
    String filename = "test.txt";
    String mimeType = "text/plain";
    FileValue variableValue = Variables.fileValue(filename).mimeType(mimeType).create();

    when(caseServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_CASE_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.TEXT.toString())
    .and()
      .body(is(equalTo("")))
    .when().get(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);
  }

  @Test
  void testGetFileVariableDownloadWithType() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(ContentType.TEXT.toString()).create();

    when(caseServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_CASE_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.TEXT.toString())
    .and()
      .body(is(equalTo(new String(byteContent))))
    .when().get(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);
  }

  @Test
  void testGetFileVariableDownloadWithTypeAndEncoding() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    String encoding = UTF_8.name();
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(ContentType.TEXT.toString()).encoding(encoding).create();

    when(caseServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_CASE_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

    Response response = given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body(is(equalTo(new String(byteContent))))
    .when().get(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);

    String contentType = response.contentType().replaceAll(" ", "");
    assertThat(contentType).isEqualTo(ContentType.TEXT + ";charset=" + encoding);
  }

  @Test
  void testGetFileVariableDownloadWithoutType() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).create();

    when(caseServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_CASE_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .and()
      .body(is(equalTo(new String(byteContent))))
      .header("Content-Disposition", containsString(filename))
    .when().get(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);
  }

  @Test
  void testCannotDownloadVariableOtherThanFile() {
    String variableKey = "aVariableKey";
    BooleanValue variableValue = Variables.booleanValue(true);

    when(caseServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_CASE_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(MediaType.APPLICATION_JSON)
    .when().get(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);
  }

  @Test
  void testLocalVariableModification() {
    Map<String, Object> messageBodyJson = new HashMap<>();

    String variableKey = "aKey";
    int variableValue = 123;

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue).getVariables();
    messageBodyJson.put("modifications", modifications);

    List<String> deletions = new ArrayList<>();
    deletions.add("deleteKey");
    messageBodyJson.put("deletions", deletions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_LOCAL_VARIABLES_URL);

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put(variableKey, variableValue);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariablesLocal(deletions);
    verify(caseExecutionCommandBuilderMock).setVariablesLocal(expectedMap);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testVariableModification() {
    Map<String, Object> messageBodyJson = new HashMap<>();

    String variableKey = "aKey";
    int variableValue = 123;

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue).getVariables();
    messageBodyJson.put("modifications", modifications);

    List<String> deletions = new ArrayList<>();
    deletions.add("deleteKey");
    messageBodyJson.put("deletions", deletions);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_VARIABLES_URL);

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put(variableKey, variableValue);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariables(deletions);
    verify(caseExecutionCommandBuilderMock).setVariables(expectedMap);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testLocalVariableModificationForNonExistingExecution() {
    when(caseServiceMock.withCaseExecution("aNonExistingExecutionId")).thenReturn(caseExecutionCommandBuilderMock);

    doThrow(new ProcessEngineException("expected exception"))
      .when(caseExecutionCommandBuilderMock)
      .execute();

    Map<String, Object> messageBodyJson = new HashMap<>();

    String variableKey = "aKey";
    int variableValue = 123;
    Map<String, Object> modifications = VariablesBuilder
        .create()
        .variable(variableKey, variableValue)
        .getVariables();

    messageBodyJson.put("modifications", modifications);

    given()
      .pathParam("id", "aNonExistingExecutionId")
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", equalTo("Cannot modify variables for case execution " + "aNonExistingExecutionId" + ": expected exception"))
    .when()
      .post(CASE_EXECUTION_LOCAL_VARIABLES_URL);

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put(variableKey, variableValue);

    verify(caseServiceMock).withCaseExecution("aNonExistingExecutionId");
    verify(caseExecutionCommandBuilderMock).removeVariablesLocal(null);
    verify(caseExecutionCommandBuilderMock).setVariablesLocal(expectedMap);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testVariableModificationForNonExistingExecution() {
    when(caseServiceMock.withCaseExecution("aNonExistingExecutionId")).thenReturn(caseExecutionCommandBuilderMock);

    doThrow(new ProcessEngineException("expected exception"))
      .when(caseExecutionCommandBuilderMock)
      .execute();

    Map<String, Object> messageBodyJson = new HashMap<>();

    String variableKey = "aKey";
    int variableValue = 123;
    Map<String, Object> modifications = VariablesBuilder
        .create()
        .variable(variableKey, variableValue)
        .getVariables();

    messageBodyJson.put("modifications", modifications);

    given()
      .pathParam("id", "aNonExistingExecutionId")
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", equalTo("Cannot modify variables for case execution " + "aNonExistingExecutionId" + ": expected exception"))
    .when()
      .post(CASE_EXECUTION_VARIABLES_URL);

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put(variableKey, variableValue);

    verify(caseServiceMock).withCaseExecution("aNonExistingExecutionId");
    verify(caseExecutionCommandBuilderMock).removeVariables(null);
    verify(caseExecutionCommandBuilderMock).setVariables(expectedMap);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testEmptyLocalVariableModification() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_LOCAL_VARIABLES_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariablesLocal(null);
    verify(caseExecutionCommandBuilderMock).setVariablesLocal(emptyMap());
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testEmptyVariableModification() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_VARIABLES_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariables(null);
    verify(caseExecutionCommandBuilderMock).setVariables(emptyMap());
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testGetSingleLocalVariable() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("value", is(EXAMPLE_VARIABLE_VALUE.getValue()))
        .body("type", is(VariableTypeHelper.toExpectedValueTypeName(EXAMPLE_VARIABLE_VALUE.getType())))
    .when()
      .get(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, EXAMPLE_VARIABLE_KEY, true);
  }

  @Test
  void testGetSingleLocalVariableData() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_BYTES_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .when()
      .get(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock).getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, EXAMPLE_BYTES_VARIABLE_KEY, false);
  }

  @Test
  void testGetSingleLocalVariableDataNonExisting() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", "nonExisting")
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is("case execution variable with name " + "nonExisting" + " does not exist"))
    .when()
      .get(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock).getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, "nonExisting", false);
  }

  @Test
  void testGetSingleLocalVariabledataNotBinary() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .get(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock).getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, EXAMPLE_VARIABLE_KEY, false);
  }

  @Test
  void testGetSingleVariable() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("value", is(EXAMPLE_VARIABLE_VALUE.getValue()))
        .body("type", is(VariableTypeHelper.toExpectedValueTypeName(EXAMPLE_VARIABLE_VALUE.getType())))
    .when()
      .get(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).getVariableTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, EXAMPLE_VARIABLE_KEY, true);
  }

  @Test
  void testGetSingleLocalObjectVariable() {
    // given
    String variableKey = "aVariableId";

    List<String> payload = List.of("a", "b");
    ObjectValue variableValue =
        MockObjectValue
            .fromObjectValue(Variables
                .objectValue(payload)
                .serializationDataFormat("application/json")
                .create())
            .objectTypeName(ArrayList.class.getName())
            .serializedValue("a serialized value"); // this should differ from the serialized json

    when(caseServiceMock.getVariableLocalTyped(eq(MockProvider.EXAMPLE_CASE_EXECUTION_ID), eq(variableKey), anyBoolean())).thenReturn(variableValue);

    // when
    given().pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("value", equalTo(payload))
      .body("type", equalTo("Object"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, equalTo("application/json"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, equalTo(ArrayList.class.getName()))
      .when().get(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    // then
    verify(caseServiceMock).getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, variableKey, true);
  }

  @Test
  void testGetSingleLocalObjectVariableSerialized() {
    // given
    String variableKey = "aVariableId";

    ObjectValue variableValue =
        Variables
          .serializedObjectValue("a serialized value")
          .serializationDataFormat("application/json")
          .objectTypeName(ArrayList.class.getName())
          .create();

    when(caseServiceMock.getVariableLocalTyped(eq(MockProvider.EXAMPLE_CASE_EXECUTION_ID), eq(variableKey), anyBoolean())).thenReturn(variableValue);

    // when
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .queryParam("deserializeValue", false)
    .then().expect().statusCode(Status.OK.getStatusCode())
      .body("value", equalTo("a serialized value"))
      .body("type", equalTo("Object"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, equalTo("application/json"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, equalTo(ArrayList.class.getName()))
      .when().get(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    // then
    verify(caseServiceMock).getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, variableKey, false);
  }

  @Test
  void testNonExistingLocalVariable() {
    when(caseServiceMock.getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, EXAMPLE_VARIABLE_KEY, true))
      .thenReturn(null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
      .body("type", is(InvalidRequestException.class.getSimpleName()))
      .body("message", is("case execution variable with name " + EXAMPLE_VARIABLE_KEY + " does not exist"))
    .when()
      .get(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testNonExistingVariable() {
    when(caseServiceMock.getVariableTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, EXAMPLE_VARIABLE_KEY, true))
      .thenReturn(null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
      .body("type", is(InvalidRequestException.class.getSimpleName()))
      .body("message", is("case execution variable with name " + EXAMPLE_VARIABLE_KEY + " does not exist"))
    .when()
      .get(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testGetLocalVariableForNonExistingExecution() {
    when(caseServiceMock.getVariableLocalTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID,
        EXAMPLE_VARIABLE_KEY, true))
      .thenThrow(new ProcessEngineException("expected exception"));

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", is("Cannot get case execution variable " + EXAMPLE_VARIABLE_KEY + ": expected exception"))
    .when()
      .get(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testGetVariableForNonExistingExecution() {
    when(caseServiceMock.getVariableTyped(MockProvider.EXAMPLE_CASE_EXECUTION_ID, EXAMPLE_VARIABLE_KEY, true))
      .thenThrow(new ProcessEngineException("expected exception"));

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", is("Cannot get case execution variable " + EXAMPLE_VARIABLE_KEY + ": expected exception"))
    .when()
      .get(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testPutSingleLocalVariable() {
    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(EXAMPLE_VARIABLE_VALUE.getValue(),
        EXAMPLE_VARIABLE_VALUE.getType().getName());

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(EXAMPLE_VARIABLE_KEY, EXAMPLE_VARIABLE_VALUE);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariable() {
    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(EXAMPLE_VARIABLE_VALUE.getValue(),
        EXAMPLE_VARIABLE_VALUE.getType().getName());

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(EXAMPLE_VARIABLE_KEY, EXAMPLE_VARIABLE_VALUE);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalVariableWithTypeInteger() {
    String variableKey = "aVariableKey";
    Integer variableValue = 123;
    String type = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.integerValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithTypeInteger() {
    String variableKey = "aVariableKey";
    Integer variableValue = 123;
    String type = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.integerValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalVariableWithUnparseableInteger() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Integer.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithUnparseableInteger() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Integer.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testPutSingleLocalVariableWithTypeShort() {
    String variableKey = "aVariableKey";
    Short variableValue = 123;
    String type = "Short";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.shortValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithTypeShort() {
    String variableKey = "aVariableKey";
    Short variableValue = 123;
    String type = "Short";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.shortValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalVariableWithUnparseableShort() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Short";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Short.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithUnparseableShort() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Short";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON).
      body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Short.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testPutSingleLocalVariableWithTypeLong() {
    String variableKey = "aVariableKey";
    Long variableValue = 123L;
    String type = "Long";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.longValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithTypeLong() {
    String variableKey = "aVariableKey";
    Long variableValue = 123L;
    String type = "Long";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.longValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithUnparseableLong() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Long";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Long.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testPutSingleLocalVariableWithTypeDouble() {
    String variableKey = "aVariableKey";
    Double variableValue = 123.456;
    String type = "Double";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.doubleValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithTypeDouble() {
    String variableKey = "aVariableKey";
    Double variableValue = 123.456;
    String type = "Double";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.doubleValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalVariableWithUnparseableDouble() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Double";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Double.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithUnparseableDouble() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Double";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Double.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testPutSingleLocalVariableWithTypeBoolean() {
    String variableKey = "aVariableKey";
    Boolean variableValue = true;
    String type = "Boolean";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.booleanValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithTypeBoolean() {
    String variableKey = "aVariableKey";
    Boolean variableValue = true;
    String type = "Boolean";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.booleanValue(variableValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalVariableWithTypeDate() throws Exception {
    Date now = new Date();

    String variableKey = "aVariableKey";
    String variableValue = DATE_FORMAT_WITH_TIMEZONE.format(now);
    String type = "Date";

    Date expectedValue = DATE_FORMAT_WITH_TIMEZONE.parse(variableValue);

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.dateValue(expectedValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithTypeDate() throws Exception {
    Date now = new Date();

    String variableKey = "aVariableKey";
    String variableValue = DATE_FORMAT_WITH_TIMEZONE.format(now);
    String type = "Date";

    Date expectedValue = DATE_FORMAT_WITH_TIMEZONE.parse(variableValue);

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.dateValue(expectedValue)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalVariableWithUnparseableDate() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Date";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Date.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithUnparseableDate() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Date";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: "
            + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Date.class)))
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testPutSingleLocalVariableWithNotSupportedType() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "X";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: Unsupported value type 'X'"))
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithNotSupportedType() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "X";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot put case execution variable aVariableKey: Unsupported value type 'X'"))
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testPutSingleLocalBinaryVariable() {
    byte[] bytes = "someContent".getBytes();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleBinaryVariable() {
    byte[] bytes = "someContent".getBytes();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", null, bytes, MediaType.APPLICATION_OCTET_STREAM)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalBinaryVariableWithValueType() {
    byte[] bytes = "someContent".getBytes();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
      .multiPart("valueType", "Bytes", "text/plain")
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleBinaryVariableWithValueType() {
    byte[] bytes = "someContent".getBytes();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
      .multiPart("valueType", "Bytes", "text/plain")
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalBinaryVariableWithNoValue() {
    byte[] bytes = new byte[0];

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleBinaryVariableWithNoValue() {
    byte[] bytes = new byte[0];

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalSerializableVariableFromJson() throws Exception {

    ArrayList<String> serializable = new ArrayList<>();
    serializable.add("foo");

    ObjectMapper mapper = new ObjectMapper();
    String jsonBytes = mapper.writeValueAsString(serializable);
    String typeName = TypeFactory.defaultInstance().constructType(serializable.getClass()).toCanonical();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", jsonBytes, MediaType.APPLICATION_JSON)
      .multiPart("type", typeName, MediaType.TEXT_PLAIN)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsObjectValue.objectValueMatcher().isDeserialized().value(serializable)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleSerializableVariableFormJson() throws Exception {

    ArrayList<String> serializable = new ArrayList<>();
    serializable.add("foo");

    ObjectMapper mapper = new ObjectMapper();
    String jsonBytes = mapper.writeValueAsString(serializable);
    String typeName = TypeFactory.defaultInstance().constructType(serializable.getClass()).toCanonical();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", jsonBytes, MediaType.APPLICATION_JSON)
      .multiPart("type", typeName, MediaType.TEXT_PLAIN)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsObjectValue.objectValueMatcher().isDeserialized().value(serializable)));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleLocalSerializableVariableUnsupportedMediaType() throws Exception {

    ArrayList<String> serializable = new ArrayList<>();
    serializable.add("foo");

    ObjectMapper mapper = new ObjectMapper();
    String jsonBytes = mapper.writeValueAsString(serializable);
    String typeName = TypeFactory.defaultInstance().constructType(serializable.getClass()).toCanonical();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", jsonBytes, "unsupported")
      .multiPart("type", typeName, MediaType.TEXT_PLAIN)
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body(containsString("Unrecognized content type for serialized java type: unsupported"))
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    verify(caseServiceMock, never()).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
  }

  @Test
  void testPutSingleSerializableVariableUnsupportedMediaType() throws Exception {

    ArrayList<String> serializable = new ArrayList<>();
    serializable.add("foo");

    ObjectMapper mapper = new ObjectMapper();
    String jsonBytes = mapper.writeValueAsString(serializable);
    String typeName = TypeFactory.defaultInstance().constructType(serializable.getClass()).toCanonical();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", jsonBytes, "unsupported")
      .multiPart("type", typeName, MediaType.TEXT_PLAIN)
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body(containsString("Unrecognized content type for serialized java type: unsupported"))
    .when()
      .post(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);

    verify(caseServiceMock, never()).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
  }

  @Test
  void testPutSingleLocalVariableWithNoValue() {
    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        argThat(EqualsNullValue.matcher()));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutSingleVariableWithNoValue() {
    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        argThat(EqualsNullValue.matcher()));
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testPutLocalVariableForNonExistingExecution() {
    String variableKey = "aVariableKey";
    String variableValue = "aVariableValue";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue);

    doThrow(new ProcessEngineException("expected exception")).when(caseExecutionCommandBuilderMock).execute();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", is("Cannot put case execution variable " + variableKey + ": expected exception"))
    .when()
      .put(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testPostSingleLocalFileVariableWithEncodingAndMimeType() {

    byte[] value = "some text".getBytes();
    String variableKey = "aVariableKey";
    String encoding = UTF_8.name();
    String filename = "test.txt";
    String mimetype = MediaType.TEXT_PLAIN;

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", filename, value, mimetype + "; encoding="+encoding)
      .multiPart("valueType", "File", "text/plain")
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    ArgumentCaptor<FileValue> captor = ArgumentCaptor.forClass(FileValue.class);
    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        captor.capture());
    FileValue captured = captor.getValue();
    assertThat(captured.getEncoding()).isEqualTo(encoding);
    assertThat(captured.getFilename()).isEqualTo(filename);
    assertThat(captured.getMimeType()).isEqualTo(mimetype);
    assertThat(IoUtil.readInputStream(captured.getValue(), null)).isEqualTo(value);
  }

  @Test
  void testPostSingleLocalFileVariableWithMimeType() {

    byte[] value = "some text".getBytes();
    String variableKey = "aVariableKey";
    String filename = "test.txt";
    String mimetype = MediaType.TEXT_PLAIN;

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, value, mimetype)
      .multiPart("valueType", "File", "text/plain")
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    ArgumentCaptor<FileValue> captor = ArgumentCaptor.forClass(FileValue.class);
    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        captor.capture());
    FileValue captured = captor.getValue();
    assertThat(captured.getEncoding()).isNull();
    assertThat(captured.getFilename()).isEqualTo(filename);
    assertThat(captured.getMimeType()).isEqualTo(mimetype);
    assertThat(IoUtil.readInputStream(captured.getValue(), null)).isEqualTo(value);
  }

  @Test
  void testPostSingleLocalFileVariableWithEncoding() {

    byte[] value = "some text".getBytes();
    String variableKey = "aVariableKey";
    String encoding = UTF_8.name();
    String filename = "test.txt";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, value, "encoding="+encoding)
      .multiPart("valueType", "File", "text/plain")
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    //when the user passes an encoding, he has to provide the type, too
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);
  }

  @Test
  void testPostSingleLocalFileVariableOnlyFilename() throws Exception {

    String variableKey = "aVariableKey";
    String filename = "test.txt";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, new byte[0])
      .multiPart("valueType", "File", "text/plain")
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_LOCAL_BINARY_VARIABLE_URL);

    ArgumentCaptor<FileValue> captor = ArgumentCaptor.forClass(FileValue.class);
    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(variableKey),
        captor.capture());
    FileValue captured = captor.getValue();
    assertThat(captured.getEncoding()).isNull();
    assertThat(captured.getFilename()).isEqualTo(filename);
    assertThat(captured.getMimeType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    assertThat(captured.getValue().available()).isZero();
  }

  @Test
  void testPostSingleFileVariable() {

    byte[] value = "some text".getBytes();
    String variableKey = "aVariableKey";
    String encoding = UTF_8.name();
    String filename = "test.txt";
    String mimetype = MediaType.TEXT_PLAIN;

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, value, mimetype + "; encoding="+encoding)
      .header("accept", MediaType.APPLICATION_JSON)
      .multiPart("valueType", "File", "text/plain")
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_CASE_EXECUTION_BINARY_VARIABLE_URL);

    ArgumentCaptor<FileValue> captor = ArgumentCaptor.forClass(FileValue.class);
    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(variableKey),
        captor.capture());
    FileValue captured = captor.getValue();
    assertThat(captured.getEncoding()).isEqualTo(encoding);
    assertThat(captured.getFilename()).isEqualTo(filename);
    assertThat(captured.getMimeType()).isEqualTo(mimetype);
    assertThat(IoUtil.readInputStream(captured.getValue(), null)).isEqualTo(value);
  }

  @Test
  void testPutVariableForNonExistingExecution() {
    String variableKey = "aVariableKey";
    String variableValue = "aVariableValue";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue);

    doThrow(new ProcessEngineException("expected exception")).when(caseExecutionCommandBuilderMock).execute();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", is("Cannot put case execution variable " + variableKey + ": expected exception"))
    .when()
      .put(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testDeleteSingleLocalVariable() {
    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(variableKey);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testDeleteSingleVariable() {
    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_CASE_EXECUTION_VARIABLE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(variableKey);
    verify(caseExecutionCommandBuilderMock).execute();
  }

  @Test
  void testDeleteLocalVariableForNonExistingExecution() {
    String variableKey = "aVariableKey";

    doThrow(new ProcessEngineException("expected exception"))
      .when(caseExecutionCommandBuilderMock).execute();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", is("Cannot delete case execution variable " + variableKey + ": expected exception"))
    .when()
      .delete(SINGLE_CASE_EXECUTION_LOCAL_VARIABLE_URL);
  }

  @Test
  void testDeleteVariableForNonExistingExecution() {
    String variableKey = "aVariableKey";

    doThrow(new ProcessEngineException("expected exception"))
      .when(caseExecutionCommandBuilderMock).execute();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .pathParam("varId", variableKey)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", is("Cannot delete case execution variable " + variableKey + ": expected exception"))
    .when()
      .delete(SINGLE_CASE_EXECUTION_VARIABLE_URL);
  }

  @Test
  void testComplete() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testUnsuccessfulComplete() {
    doThrow(new NotValidException("expected exception")).when(caseExecutionCommandBuilderMock).complete();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Cannot complete case execution " + MockProvider.EXAMPLE_CASE_EXECUTION_ID + ": expected exception"))
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithSetVariable() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithSetVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer", true)
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithSetVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey, true);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithRemoveVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithSetVariableAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithSetVariableAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithSetVariableLocalAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).complete();
  }

  @Test
  void testCompleteWithSetVariableLocalAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_COMPLETE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).complete();
  }

  // /////////////////////////////////////////////////////////////
  @Test
  void testTerminate() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testUnsuccessfulTerminate() {
    doThrow(new NotValidException("expected exception")).when(caseExecutionCommandBuilderMock).terminate();

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Cannot terminate case execution " + MockProvider.EXAMPLE_CASE_EXECUTION_ID + ": expected exception"))
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithSetVariable() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithSetVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer", true)
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithSetVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    int aVariableValue = 123;

    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variablesJson = new HashMap<>();

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(aVariableKey, aVariableValue, "Integer")
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    variablesJson.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(aVariableKey),
        argThat(EqualsPrimitiveValue.integerValue(aVariableValue)));
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey, true);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithRemoveVariableAndVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);
    VariableNameDto secondVariableName = new VariableNameDto(anotherVariableKey);
    variableNames.add(secondVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).removeVariable(anotherVariableKey);
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithSetVariableAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithSetVariableAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String")
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariable(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithSetVariableLocalAndRemoveVariable() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariable(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).terminate();
  }

  @Test
  void testTerminateWithSetVariableLocalAndRemoveVariableLocal() {
    String aVariableKey = "aKey";
    String anotherVariableKey = "anotherKey";
    String anotherVariableValue = "abc";

    Map<String, Object> variables = VariablesBuilder
        .create()
          .variable(anotherVariableKey, anotherVariableValue, "String", true)
          .getVariables();

    List<VariableNameDto> variableNames = new ArrayList<>();

    VariableNameDto firstVariableName = new VariableNameDto(aVariableKey, true);
    variableNames.add(firstVariableName);

    Map<String, Object> variablesJson = new HashMap<>();

    variablesJson.put("variables", variables);
    variablesJson.put("deletions", variableNames);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_EXECUTION_ID)
      .contentType(ContentType.JSON)
      .body(variablesJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(CASE_EXECUTION_TERMINATE_URL);

    verify(caseServiceMock).withCaseExecution(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    verify(caseExecutionCommandBuilderMock).removeVariableLocal(aVariableKey);
    verify(caseExecutionCommandBuilderMock).setVariableLocal(eq(anotherVariableKey),
        argThat(EqualsPrimitiveValue.stringValue(anotherVariableValue)));
    verify(caseExecutionCommandBuilderMock).terminate();
  }
}
