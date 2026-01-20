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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockHistoricVariableInstanceBuilder;
import org.operaton.bpm.engine.rest.helper.MockObjectValue;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.VariableTypeHelper;
import org.operaton.bpm.engine.rest.util.OrderingBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.SerializableValueType;
import org.operaton.bpm.engine.variable.type.ValueType;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HistoricVariableInstanceRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/variable-instance";

  protected static final String HISTORIC_VARIABLE_INSTANCE_COUNT_RESOURCE_URL = HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL + "/count";

  protected HistoricVariableInstanceQuery mockedQuery;
  protected HistoricVariableInstance mockInstance;
  protected MockHistoricVariableInstanceBuilder mockInstanceBuilder;

  @BeforeEach
  void setUpRuntimeData() {
    mockInstanceBuilder = MockProvider.mockHistoricVariableInstance();
    mockInstance = mockInstanceBuilder.build();

    List<HistoricVariableInstance> mocks = new ArrayList<>();
    mocks.add(mockInstance);

    mockedQuery = setUpMockHistoricVariableInstanceQuery(mocks);
  }

  private HistoricVariableInstanceQuery setUpMockHistoricVariableInstanceQuery(List<HistoricVariableInstance> mockedHistoricVariableInstances) {

    HistoricVariableInstanceQuery mockedHistoricVariableInstanceQuery = mock(HistoricVariableInstanceQuery.class);
    when(mockedHistoricVariableInstanceQuery.list()).thenReturn(mockedHistoricVariableInstances);
    when(mockedHistoricVariableInstanceQuery.count()).thenReturn((long) mockedHistoricVariableInstances.size());

    when(processEngine.getHistoryService().createHistoricVariableInstanceQuery()).thenReturn(mockedHistoricVariableInstanceQuery);

    return mockedHistoricVariableInstanceQuery;
  }

  @Test
  void testEmptyQuery() {
    String queryKey = "";
    given()
      .queryParam("processInstanceId", queryKey)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).list();
    verify(mockedQuery).disableBinaryFetching();
    verify(mockedQuery, never()).disableCustomObjectDeserialization();

    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testNoParametersQueryDisableObjectDeserialization() {
    given()
      .queryParam("deserializeValues", false)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).list();
    verify(mockedQuery).disableBinaryFetching();
    verify(mockedQuery).disableCustomObjectDeserialization();

    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testNoParametersQueryAsPost() {
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).list();
    verify(mockedQuery).disableBinaryFetching();
    verify(mockedQuery, never()).disableCustomObjectDeserialization();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testNoParametersQueryAsPostDisableObjectDeserialization() {
    given()
      .queryParam("deserializeValues", false)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).list();
    verify(mockedQuery).disableBinaryFetching();
    verify(mockedQuery).disableCustomObjectDeserialization();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("instanceId", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
      .queryParam("sortBy", sortBy).queryParam("sortOrder", sortOrder)
    .then()
      .expect().statusCode(expectedStatus.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortByParameterOnly() {
    given()
      .queryParam("sortBy", "instanceId")
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given()
      .queryParam("sortOrder", "asc")
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);

    executeAndVerifySorting("instanceId", "asc", Status.OK);

    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);

    executeAndVerifySorting("variableName", "desc", Status.OK);

    inOrder.verify(mockedQuery).orderByVariableName();
    inOrder.verify(mockedQuery).desc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).desc();
  }

  @Test
  void testSecondarySortingAsPost() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    Map<String, Object> json = new HashMap<>();
    json.put("sorting", OrderingBuilder.create()
      .orderBy("instanceId").desc()
      .orderBy("variableName").asc()
      .getJson());
    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    inOrder.verify(mockedQuery).orderByProcessInstanceId();
    inOrder.verify(mockedQuery).desc();
    inOrder.verify(mockedQuery).orderByVariableName();
    inOrder.verify(mockedQuery).asc();
  }

  @Test
  void testSuccessfulPagination() {

    int firstResult = 0;
    int maxResults = 10;
    given()
      .queryParam("firstResult", firstResult)
      .queryParam("maxResults", maxResults)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, maxResults);
  }

  @Test
  void testMissingFirstResultParameter() {
    int maxResults = 10;
    given()
      .queryParam("maxResults", maxResults)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(0, maxResults);
  }

  @Test
  void testMissingMaxResultsParameter() {
    int firstResult = 10;
    given()
      .queryParam("firstResult", firstResult)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testQueryCountForPost() {
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .expect()
      .body("count", equalTo(1))
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testVariableNameLikeQuery() {
    String variableNameLike = "aVariableNameLike";

    given()
      .queryParam("variableNameLike", variableNameLike)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).variableNameLike(variableNameLike);
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testVariableNameLikeQueryIgnoreCase() {
    String variableNameLike = "aVariableNameLike";

    given()
    .queryParam("variableNameLike", variableNameLike)
    .queryParam("variableNamesIgnoreCase", true)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).variableNameLike(variableNameLike);
    verify(mockedQuery).matchVariableNamesIgnoreCase();
    verify(mockedQuery).list();
  }

  @Test
  void testHistoricVariableQueryByVariableTypeIn() {
    String aVariableType = "string";
    String anotherVariableType = "integer";

    given()
      .queryParam("variableTypeIn", aVariableType + "," + anotherVariableType)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).variableTypeIn(aVariableType, anotherVariableType);
  }

  @Test
  void testHistoricVariableQueryByVariableTypeInAsPost() {
    String aVariableType = "string";
    String anotherVariableType = "integer";

    List<String> variableTypeIn= new ArrayList<>();
    variableTypeIn.add(aVariableType);
    variableTypeIn.add(anotherVariableType);

    Map<String, Object> json = new HashMap<>();
    json.put("variableTypeIn", variableTypeIn);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).variableTypeIn(aVariableType, anotherVariableType);
  }

  @Test
  void testSimpleHistoricVariableQuery() {
    String processInstanceId = MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    given()
        .queryParam("processInstanceId", processInstanceId)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .and()
          .body("size()", is(1))
          .body("[0].id", equalTo(mockInstanceBuilder.getId()))
          .body("[0].name", equalTo(mockInstanceBuilder.getName()))
          .body("[0].type", equalTo(VariableTypeHelper.toExpectedValueTypeName(mockInstanceBuilder.getTypedValue().getType())))
          .body("[0].value", equalTo(mockInstanceBuilder.getValue()))
          .body("[0].processDefinitionKey", equalTo(mockInstanceBuilder.getProcessDefinitionKey()))
          .body("[0].processDefinitionId", equalTo(mockInstanceBuilder.getProcessDefinitionId()))
          .body("[0].processInstanceId", equalTo(mockInstanceBuilder.getProcessInstanceId()))
          .body("[0].executionId", equalTo(mockInstanceBuilder.getExecutionId()))
          .body("[0].errorMessage", equalTo(mockInstanceBuilder.getErrorMessage()))
          .body("[0].activityInstanceId", equalTo(mockInstanceBuilder.getActivityInstanceId()))
          .body("[0].caseDefinitionKey", equalTo(mockInstanceBuilder.getCaseDefinitionKey()))
          .body("[0].caseDefinitionId", equalTo(mockInstanceBuilder.getCaseDefinitionId()))
          .body("[0].caseInstanceId", equalTo(mockInstanceBuilder.getCaseInstanceId()))
          .body("[0].caseExecutionId", equalTo(mockInstanceBuilder.getCaseExecutionId()))
          .body("[0].taskId", equalTo(mockInstanceBuilder.getTaskId()))
          .body("[0].tenantId", equalTo(mockInstanceBuilder.getTenantId()))
          .body("[0].createTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_CREATE_TIME))
          .body("[0].removalTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_REMOVAL_TIME))
          .body("[0].rootProcessInstanceId", equalTo(mockInstanceBuilder.getRootProcessInstanceId()))
      .when()
        .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).processInstanceId(processInstanceId);
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testSerializableVariableInstanceRetrieval() {
    MockHistoricVariableInstanceBuilder builder = MockProvider.mockHistoricVariableInstance()
        .typedValue(MockObjectValue.fromObjectValue(Variables
            .objectValue("a serialized value")
            .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
            .create())
            .objectTypeName(String.class.getName()));

    List<HistoricVariableInstance> mockInstances = new ArrayList<>();
    mockInstances.add(builder.build());

    mockedQuery = setUpMockHistoricVariableInstanceQuery(mockInstances);

    given()
        .then().expect().statusCode(Status.OK.getStatusCode())
        .and()
          .body("[0].type", equalTo(VariableTypeHelper.toExpectedValueTypeName(ValueType.OBJECT)))
          .body("[0].value", equalTo("a serialized value"))
          .body("[0].valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, equalTo(String.class.getName()))
          .body("[0].valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, equalTo(Variables.SerializationDataFormats.JAVA.getName()))
        .when().get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    // should not resolve custom objects but existing API requires it
//  verify(mockedQuery).disableCustomObjectDeserialization();
    verify(mockedQuery, never()).disableCustomObjectDeserialization();
  }

  @Test
  void testSpinVariableInstanceRetrieval() {
    MockHistoricVariableInstanceBuilder builder = MockProvider.mockHistoricVariableInstance()
        .typedValue(Variables
            .serializedObjectValue("aSpinSerializedValue")
            .serializationDataFormat("aDataFormat")
            .objectTypeName("aRootType")
            .create());

    List<HistoricVariableInstance> mockInstances = new ArrayList<>();
    mockInstances.add(builder.build());

    mockedQuery = setUpMockHistoricVariableInstanceQuery(mockInstances);

    given()
        .then().expect().statusCode(Status.OK.getStatusCode())
        .and()
          .body("size()", is(1))
          .body("[0].type", equalTo(VariableTypeHelper.toExpectedValueTypeName(ValueType.OBJECT)))
          .body("[0].value", equalTo("aSpinSerializedValue"))
          .body("[0].valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME,
              equalTo("aRootType"))
          .body("[0].valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT,
              equalTo("aDataFormat"))
        .when().get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testAdditionalParametersExcludingVariables() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    given()
      .queryParams(stringQueryParameters)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verifyStringParameterQueryInvocations();
    verify(mockedQuery).list();
  }

  private Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("processInstanceId", MockProvider.EXAMPLE_VARIABLE_INSTANCE_PROC_INST_ID);
    parameters.put("variableName", MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME);
    parameters.put("variableValue", MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getValue());
    return parameters;
  }

  private void verifyStringParameterQueryInvocations() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    verify(mockedQuery).processInstanceId(stringQueryParameters.get("processInstanceId"));
    verify(mockedQuery).variableName(stringQueryParameters.get("variableName"));
    verify(mockedQuery).variableValueEquals(stringQueryParameters.get("variableName"), stringQueryParameters.get("variableValue"));
  }

  @Test
  void testVariableNameAndValueQuery() {
    String variableName = MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME;
    String variableValue = MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getValue();

    given()
        .queryParam("variableName", variableName)
        .queryParam("variableValue", variableValue)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
        .and()
          .body("size()", is(1))
          .body("[0].name", equalTo(MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME))
          .body("[0].value", equalTo(MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getValue()))
        .when()
          .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).variableValueEquals(variableName, variableValue);
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testVariableNameAndValueIgnoreCaseQuery() {
    String variableName = MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME;
    String variableValue = MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getValue();

    given()
    .queryParam("variableName", variableName)
    .queryParam("variableNamesIgnoreCase", true)
    .queryParam("variableValue", variableValue)
    .queryParam("variableValuesIgnoreCase", true)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .and()
    .body("size()", is(1))
    .body("[0].name", equalTo(MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME))
    .body("[0].value", equalTo(MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getValue()))
    .when()
    .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).variableValueEquals(variableName, variableValue);
    verify(mockedQuery).matchVariableNamesIgnoreCase();
    verify(mockedQuery).matchVariableValuesIgnoreCase();
    verify(mockedQuery).list();
  }

  @Test
  void testVariableValueQuery_BadRequest() {
    given()
      .queryParam("variableValue", MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Only a single variable value parameter specified: variable name and value are required to be able to query after a specific variable value."))
      .when()
        .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testHistoricVariableQueryByExecutionIdsAndTaskIds() {
      String anExecutionId = "anExecutionId";
      String anotherExecutionId = "anotherExecutionId";

      String aTaskId = "aTaskId";
      String anotherTaskId = "anotherTaskId";

      given()
        .queryParam("executionIdIn", anExecutionId + "," + anotherExecutionId)
        .queryParam("taskIdIn", aTaskId + "," + anotherTaskId)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).executionIdIn(anExecutionId, anotherExecutionId);
      verify(mockedQuery).taskIdIn(aTaskId, anotherTaskId);
  }

  @Test
  void testHistoricVariableQueryByExecutionIdsAndTaskIdsAsPost() {
    String anExecutionId = "anExecutionId";
    String anotherExecutionId = "anotherExecutionId";

    List<String> executionIdIn= new ArrayList<>();
    executionIdIn.add(anExecutionId);
    executionIdIn.add(anotherExecutionId);

    String aTaskId = "aTaskId";
    String anotherTaskId = "anotherTaskId";

    List<String> taskIdIn= new ArrayList<>();
    taskIdIn.add(aTaskId);
    taskIdIn.add(anotherTaskId);

    Map<String, Object> json = new HashMap<>();
    json.put("executionIdIn", executionIdIn);
    json.put("taskIdIn", taskIdIn);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).executionIdIn(anExecutionId, anotherExecutionId);
    verify(mockedQuery).taskIdIn(aTaskId, anotherTaskId);
  }

  @Test
  void testHistoricVariableQueryByProcessInstanceIdIn() {
    String aProcessInstanceId = "aProcessInstanceId";
    String anotherProcessInstanceId = "anotherProcessInstanceId";

    given()
            .queryParam("processInstanceIdIn", aProcessInstanceId + "," + anotherProcessInstanceId)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceIdIn(aProcessInstanceId, anotherProcessInstanceId);
  }

  @Test
  void testHistoricVariableQueryByProcessInstanceIdInAsPOST() {
    String aProcessInstanceId = "aProcessInstanceId";
    String anotherProcessInstanceId = "anotherProcessInstanceId";

    List<String> processInstanceIdIn= new ArrayList<>();
    processInstanceIdIn.add(aProcessInstanceId);
    processInstanceIdIn.add(anotherProcessInstanceId);
    processInstanceIdIn.add(null);

    Map<String, Object> json = new HashMap<>();
    json.put("processInstanceIdIn", processInstanceIdIn);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processInstanceIdIn(aProcessInstanceId, anotherProcessInstanceId,null);
  }

  @Test
  void testHistoricVariableQueryByActivityInstanceIds() {
      String anActivityInstanceId = "anActivityInstanceId";
      String anotherActivityInstanceId = "anotherActivityInstanceId";

      given()
        .queryParam("activityInstanceIdIn", anActivityInstanceId + "," + anotherActivityInstanceId)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).activityInstanceIdIn(anActivityInstanceId, anotherActivityInstanceId);
  }

  @Test
  void testHistoricVariableQueryByActivityInstanceIdsAsPost() {
    String anActivityInstanceId = "anActivityInstanceId";
    String anotherActivityInstanceId = "anotherActivityInstanceId";

    List<String> activityInstanceIdIn= new ArrayList<>();
    activityInstanceIdIn.add(anActivityInstanceId);
    activityInstanceIdIn.add(anotherActivityInstanceId);

    Map<String, Object> json = new HashMap<>();
    json.put("activityInstanceIdIn", activityInstanceIdIn);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).activityInstanceIdIn(anActivityInstanceId, anotherActivityInstanceId);
  }

  @Test
  void testHistoricVariableQueryByCaseInstanceId() {

    given()
      .queryParam("caseInstanceId", MockProvider.EXAMPLE_CASE_INSTANCE_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseInstanceId(MockProvider.EXAMPLE_CASE_INSTANCE_ID);
  }

  @Test
  void testHistoricVariableQueryByCaseInstanceIdAsPost() {
    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceId", MockProvider.EXAMPLE_CASE_INSTANCE_ID);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseInstanceId(MockProvider.EXAMPLE_CASE_INSTANCE_ID);
  }

  @Test
  void testHistoricVariableQueryByCaseExecutionIds() {

    String caseExecutionIds = MockProvider.EXAMPLE_CASE_EXECUTION_ID + "," + MockProvider.ANOTHER_EXAMPLE_CASE_EXECUTION_ID;

    given()
      .queryParam("caseExecutionIdIn", caseExecutionIds)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseExecutionIdIn(MockProvider.EXAMPLE_CASE_EXECUTION_ID, MockProvider.ANOTHER_EXAMPLE_CASE_EXECUTION_ID);
  }

  @Test
  void testHistoricVariableQueryByCaseExecutionIdsAsPost() {
    Map<String, Object> json = new HashMap<>();
    json.put("caseExecutionIdIn", List.of(MockProvider.EXAMPLE_CASE_EXECUTION_ID, MockProvider.ANOTHER_EXAMPLE_CASE_EXECUTION_ID));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseExecutionIdIn(MockProvider.EXAMPLE_CASE_EXECUTION_ID, MockProvider.ANOTHER_EXAMPLE_CASE_EXECUTION_ID);
  }

  @Test
  void testTenantIdListParameter() {
    mockedQuery = setUpMockHistoricVariableInstanceQuery(createMockHistoricVariableInstancesTwoTenants());

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testTenantIdListPostParameter() {
    mockedQuery = setUpMockHistoricVariableInstanceQuery(createMockHistoricVariableInstancesTwoTenants());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> executions = from(content).getList("");
    assertThat(executions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testQueryFilterWithoutTenantIdParameter() {
    // given
    HistoricVariableInstance historicVariableInstance = MockProvider
        .mockHistoricVariableInstance(null)
        .build();
    mockedQuery = setUpMockHistoricVariableInstanceQuery(Collections.singletonList(historicVariableInstance));

    // when
    Response response = given()
        .queryParam("withoutTenantId", true)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId).isNull();
  }

  @Test
  void testQueryFilterWithoutTenantIdPostParameter() {
    // given
    HistoricVariableInstance historicVariableInstance = MockProvider
        .mockHistoricVariableInstance(null)
        .build();
    mockedQuery = setUpMockHistoricVariableInstanceQuery(Collections.singletonList(historicVariableInstance));
    Map<String, Object> queryParameters = Collections.singletonMap("withoutTenantId", (Object) true);

    // when
    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId).isNull();
  }

  @Test
  void testHistoricVariableQueryByCaseActivityIds() {

    String caseExecutionIds = MockProvider.EXAMPLE_CASE_ACTIVITY_ID + "," + MockProvider.ANOTHER_EXAMPLE_CASE_ACTIVITY_ID;

    given()
      .queryParam("caseActivityIdIn", caseExecutionIds)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseActivityIdIn(MockProvider.EXAMPLE_CASE_ACTIVITY_ID, MockProvider.ANOTHER_EXAMPLE_CASE_ACTIVITY_ID);
  }

  @Test
  void testHistoricVariableQueryByCaseActivityIdsAsPost() {
    Map<String, Object> json = new HashMap<>();
    json.put("caseActivityIdIn", List.of(MockProvider.EXAMPLE_CASE_ACTIVITY_ID, MockProvider.ANOTHER_EXAMPLE_CASE_ACTIVITY_ID));

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).caseActivityIdIn(MockProvider.EXAMPLE_CASE_ACTIVITY_ID, MockProvider.ANOTHER_EXAMPLE_CASE_ACTIVITY_ID);
  }

  @Test
  void testIncludeDeletedVariables() {
    when(mockedQuery.includeDeleted()).thenReturn(mockedQuery);

    given()
      .queryParam("includeDeleted", true)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).includeDeleted();
  }

  @Test
  void testHistoricVariableQueryByProcessDefinitionIdAsPost() {
    when(mockedQuery.processDefinitionId(anyString())).thenReturn(mockedQuery);
    Map<String, Object> json = new HashMap<>();
    json.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testHistoricVariableQueryByProcessDefinitionId() {
    when(mockedQuery.processDefinitionId(anyString())).thenReturn(mockedQuery);

    given()
      .queryParam("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testHistoricVariableQueryByProcessDefinitionKeyAsPost() {
    when(mockedQuery.processDefinitionKey(anyString())).thenReturn(mockedQuery);
    Map<String, Object> json = new HashMap<>();
    json.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
  }

  @Test
  void testHistoricVariableQueryByProcessDefinitionKey() {
    when(mockedQuery.processDefinitionKey(anyString())).thenReturn(mockedQuery);

    given()
      .queryParam("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).processDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
  }

  private List<HistoricVariableInstance> createMockHistoricVariableInstancesTwoTenants() {
    return List.of(
        MockProvider.mockHistoricVariableInstance(MockProvider.EXAMPLE_TENANT_ID).build(),
        MockProvider.mockHistoricVariableInstance(MockProvider.ANOTHER_EXAMPLE_TENANT_ID).build());
  }

  @Test
  void testHistoricVariableQueryByVariableNameAndValueIgnoreCaseAsPost() {
    when(mockedQuery.processDefinitionKey(anyString())).thenReturn(mockedQuery);
    Map<String, Object> json = new HashMap<>();
    json.put("variableName", "aVariableName");
    json.put("variableValue", "aVariableValue");
    json.put("variableNamesIgnoreCase", true);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).matchVariableNamesIgnoreCase();
    verify(mockedQuery).matchVariableValuesIgnoreCase();
    verify(mockedQuery).variableName("aVariableName");
    verify(mockedQuery).variableValueEquals("aVariableName", "aVariableValue");
  }

  @Test
  void testHistoricVariableQueryByVariableNameLikeIgnoreCaseAsPost() {
    when(mockedQuery.processDefinitionKey(anyString())).thenReturn(mockedQuery);
    Map<String, Object> json = new HashMap<>();
    json.put("variableNameLike", "aVariableName");
    json.put("variableNamesIgnoreCase", true);

    given()
    .contentType(POST_JSON_CONTENT_TYPE)
    .body(json)
    .then().expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).matchVariableNamesIgnoreCase();
    verify(mockedQuery).variableNameLike("aVariableName");
  }

  @Test
  void shouldQueryByVariableNameInAsGet() {
      String aVariableName = "aVariableName";
      String anotherVariableName = "anotherVariableName";

      given()
        .queryParam("variableNameIn", aVariableName + "," + anotherVariableName)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

      verify(mockedQuery).variableNameIn(aVariableName, anotherVariableName);
  }

  @Test
  void shouldQueryByVariableNameInAsPost() {
    String aVariableName = "aVariableName";
    String anotherVariableName = "anotherVariableName";

    List<String> variableNameIn = new ArrayList<>();
    variableNameIn.add(aVariableName);
    variableNameIn.add(anotherVariableName);

    Map<String, Object> json = new HashMap<>();
    json.put("variableNameIn", variableNameIn);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).variableNameIn(aVariableName, anotherVariableName);
  }

}
