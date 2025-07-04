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

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.helper.MockHistoricVariableInstanceBuilder;
import org.operaton.bpm.engine.rest.helper.MockObjectValue;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.VariableTypeHelper;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;

/**
 * @author Daniel Meyer
 *
 */
public class HistoricVariableInstanceRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/variable-instance";
  protected static final String VARIABLE_INSTANCE_URL = HISTORIC_VARIABLE_INSTANCE_RESOURCE_URL + "/{id}";
  protected static final String VARIABLE_INSTANCE_BINARY_DATA_URL = VARIABLE_INSTANCE_URL + "/data";

  protected HistoryService historyServiceMock;

  protected HistoricVariableInstanceQuery variableInstanceQueryMock;

  @BeforeEach
  void setupTestData() {
    historyServiceMock = mock(HistoryService.class);
    variableInstanceQueryMock = mock(HistoricVariableInstanceQuery.class);

    // mock engine service.
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);
    when(historyServiceMock.createHistoricVariableInstanceQuery()).thenReturn(variableInstanceQueryMock);
  }

  @Test
  void testGetSingleVariableInstance() {
    MockHistoricVariableInstanceBuilder builder = MockProvider.mockHistoricVariableInstance();

    HistoricVariableInstance variableInstanceMock = builder.build();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableBinaryFetching()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    given().pathParam("id", builder.getId())
    .then().expect().statusCode(Status.OK.getStatusCode())
    .and()
      .body("id", equalTo(builder.getId()))
      .body("name", equalTo(builder.getName()))
      .body("type", equalTo(VariableTypeHelper.toExpectedValueTypeName(builder.getTypedValue().getType())))
      .body("value", equalTo(builder.getValue()))
      .body("processDefinitionKey", equalTo(builder.getProcessDefinitionKey()))
      .body("processDefinitionId", equalTo(builder.getProcessDefinitionId()))
      .body("processInstanceId", equalTo(builder.getProcessInstanceId()))
      .body("executionId", equalTo(builder.getExecutionId()))
      .body("errorMessage", equalTo(builder.getErrorMessage()))
      .body("activityInstanceId", equalTo(builder.getActivityInstanceId()))
      .body("caseDefinitionKey", equalTo(builder.getCaseDefinitionKey()))
      .body("caseDefinitionId", equalTo(builder.getCaseDefinitionId()))
      .body("caseInstanceId", equalTo(builder.getCaseInstanceId()))
      .body("caseExecutionId", equalTo(builder.getCaseExecutionId()))
      .body("taskId", equalTo(builder.getTaskId()))
      .body("tenantId", equalTo(builder.getTenantId()))
      .body("createTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_CREATE_TIME))
      .body("removalTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_REMOVAL_TIME))
      .body("rootProcessInstanceId", equalTo(builder.getRootProcessInstanceId()))
    .when().get(VARIABLE_INSTANCE_URL);

    verify(variableInstanceQueryMock, times(1)).disableBinaryFetching();

  }

  @Test
  void testGetSingleVariableInstanceDeserialized() {
    ObjectValue serializedValue = MockObjectValue.fromObjectValue(
        Variables.objectValue("a value").serializationDataFormat("aDataFormat").create())
        .objectTypeName("aTypeName");

    MockHistoricVariableInstanceBuilder builder = MockProvider.mockHistoricVariableInstance().typedValue(serializedValue);
    HistoricVariableInstance variableInstanceMock = builder.build();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableBinaryFetching()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    given()
      .pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .and()
      .body("id", equalTo(builder.getId()))
      .body("name", equalTo(builder.getName()))
      .body("type", equalTo(VariableTypeHelper.toExpectedValueTypeName(builder.getTypedValue().getType())))
      .body("value", equalTo("a value"))
      .body("valueInfo.serializationDataFormat", equalTo("aDataFormat"))
      .body("valueInfo.objectTypeName", equalTo("aTypeName"))
      .body("processDefinitionKey", equalTo(builder.getProcessDefinitionKey()))
      .body("processDefinitionId", equalTo(builder.getProcessDefinitionId()))
      .body("processInstanceId", equalTo(builder.getProcessInstanceId()))
      .body("executionId", equalTo(builder.getExecutionId()))
      .body("errorMessage", equalTo(builder.getErrorMessage()))
      .body("activityInstanceId", equalTo(builder.getActivityInstanceId()))
      .body("caseDefinitionKey", equalTo(builder.getCaseDefinitionKey()))
      .body("caseDefinitionId", equalTo(builder.getCaseDefinitionId()))
      .body("caseInstanceId", equalTo(builder.getCaseInstanceId()))
      .body("caseExecutionId", equalTo(builder.getCaseExecutionId()))
      .body("taskId", equalTo(builder.getTaskId()))
      .body("tenantId", equalTo(builder.getTenantId()))
      .body("createTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_CREATE_TIME))
      .body("removalTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_REMOVAL_TIME))
      .body("rootProcessInstanceId", equalTo(builder.getRootProcessInstanceId()))
    .when().get(VARIABLE_INSTANCE_URL);

    verify(variableInstanceQueryMock, times(1)).disableBinaryFetching();
    verify(variableInstanceQueryMock, never()).disableCustomObjectDeserialization();
  }

  @Test
  void testGetSingleVariableInstanceSerialized() {
    ObjectValue serializedValue = Variables.serializedObjectValue("a serialized value")
        .serializationDataFormat("aDataFormat").objectTypeName("aTypeName").create();

    MockHistoricVariableInstanceBuilder builder = MockProvider.mockHistoricVariableInstance().typedValue(serializedValue);
    HistoricVariableInstance variableInstanceMock = builder.build();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableBinaryFetching()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    given()
      .pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
      .queryParam("deserializeValue", false)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .and()
      .body("id", equalTo(builder.getId()))
      .body("name", equalTo(builder.getName()))
      .body("type", equalTo(VariableTypeHelper.toExpectedValueTypeName(builder.getTypedValue().getType())))
      .body("value", equalTo("a serialized value"))
      .body("valueInfo.serializationDataFormat", equalTo("aDataFormat"))
      .body("valueInfo.objectTypeName", equalTo("aTypeName"))
      .body("processInstanceId", equalTo(builder.getProcessInstanceId()))
      .body("processDefinitionKey", equalTo(builder.getProcessDefinitionKey()))
      .body("processDefinitionId", equalTo(builder.getProcessDefinitionId()))
      .body("executionId", equalTo(builder.getExecutionId()))
      .body("errorMessage", equalTo(builder.getErrorMessage()))
      .body("activityInstanceId", equalTo(builder.getActivityInstanceId()))
      .body("caseDefinitionKey", equalTo(builder.getCaseDefinitionKey()))
      .body("caseDefinitionId", equalTo(builder.getCaseDefinitionId()))
      .body("caseInstanceId", equalTo(builder.getCaseInstanceId()))
      .body("caseExecutionId", equalTo(builder.getCaseExecutionId()))
      .body("taskId", equalTo(builder.getTaskId()))
      .body("tenantId", equalTo(builder.getTenantId()))
      .body("createTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_CREATE_TIME))
      .body("removalTime", equalTo(MockProvider.EXAMPLE_HISTORIC_VARIABLE_INSTANCE_REMOVAL_TIME))
      .body("rootProcessInstanceId", equalTo(builder.getRootProcessInstanceId()))
    .when().get(VARIABLE_INSTANCE_URL);

    verify(variableInstanceQueryMock, times(1)).disableBinaryFetching();
    verify(variableInstanceQueryMock, times(1)).disableCustomObjectDeserialization();
  }

  @Test
  void testGetSingleVariableInstanceForBinaryVariable() {
    MockHistoricVariableInstanceBuilder builder = MockProvider.mockHistoricVariableInstance();

    HistoricVariableInstance variableInstanceMock = builder
      .typedValue(Variables.byteArrayValue(null))
      .build();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableBinaryFetching()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    given().pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .and()
      .body("type", equalTo(VariableTypeHelper.toExpectedValueTypeName(ValueType.BYTES)))
      .body("value", nullValue())
    .when().get(VARIABLE_INSTANCE_URL);

    verify(variableInstanceQueryMock, times(1)).disableBinaryFetching();

  }

  @Test
  void testGetNonExistingVariableInstance() {

    String nonExistingId = "nonExistingId";

    when(variableInstanceQueryMock.variableId(nonExistingId)).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableBinaryFetching()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(null);

    given().pathParam("id", nonExistingId)
    .then().expect().statusCode(Status.NOT_FOUND.getStatusCode())
    .body(containsString("Historic variable instance with Id 'nonExistingId' does not exist."))
    .when().get(VARIABLE_INSTANCE_URL);

    verify(variableInstanceQueryMock, times(1)).disableBinaryFetching();

  }

  @Test
  void testBinaryDataForBinaryVariable() {
    final byte[] byteContent = "some bytes".getBytes();
    HistoricVariableInstance variableInstanceMock = MockProvider.mockHistoricVariableInstance()
        .typedValue(Variables.byteArrayValue(byteContent))
        .build();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    Response response = given().pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.BINARY.toString())
    .when().get(VARIABLE_INSTANCE_BINARY_DATA_URL);

    byte[] responseBytes = response.getBody().asByteArray();
    Assertions.assertEquals(new String(byteContent), new String(responseBytes));
    verify(variableInstanceQueryMock, never()).disableBinaryFetching();

  }

  @Test
  void testGetBinaryDataForFileVariable() {
    String filename = "test.txt";
    byte[] byteContent = "test".getBytes();
    String encoding = UTF_8.name();
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(ContentType.TEXT.toString()).encoding(encoding).create();
    HistoricVariableInstance variableInstanceMock = MockProvider.mockHistoricVariableInstance().typedValue(variableValue).build();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    Response response = given().pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .and()
      .body(is(equalTo(new String(byteContent))))
      .header("Content-Disposition", "attachment; " +
              "filename=\"" + filename + "\"; " +
              "filename*=UTF-8''" + filename)
    .when().get(VARIABLE_INSTANCE_BINARY_DATA_URL);
    //due to some problems with wildfly we gotta check this separately
    String contentType = response.getContentType();
    assertThat(contentType).isEqualTo(ContentType.TEXT.toString() + "; charset=UTF-8");

    verify(variableInstanceQueryMock, never()).disableBinaryFetching();
  }

  @Test
  void testBinaryDataForNonBinaryVariable() {
    HistoricVariableInstance variableInstanceMock = MockProvider.createMockHistoricVariableInstance();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    given().pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body(containsString("Value of variable with id "+variableInstanceMock.getId()+" is not a binary value"))
    .when().get(VARIABLE_INSTANCE_BINARY_DATA_URL);

    verify(variableInstanceQueryMock, never()).disableBinaryFetching();

  }

  @Test
  void testGetBinaryDataForNonExistingVariableInstance() {

    String nonExistingId = "nonExistingId";

    when(variableInstanceQueryMock.variableId(nonExistingId)).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(null);

    given().pathParam("id", nonExistingId)
    .then().expect().statusCode(Status.NOT_FOUND.getStatusCode())
    .body(containsString("Historic variable instance with Id 'nonExistingId' does not exist."))
    .when().get(VARIABLE_INSTANCE_BINARY_DATA_URL);

    verify(variableInstanceQueryMock, never()).disableBinaryFetching();

  }

  @Test
  void testGetBinaryDataForNullFileVariable() {
    String filename = "test.txt";
    byte[] byteContent = null;
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(ContentType.TEXT.toString()).create();

    HistoricVariableInstance variableInstanceMock = MockProvider.mockHistoricVariableInstance()
        .typedValue(variableValue)
        .build();

    when(variableInstanceQueryMock.variableId(variableInstanceMock.getId())).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.disableCustomObjectDeserialization()).thenReturn(variableInstanceQueryMock);
    when(variableInstanceQueryMock.singleResult()).thenReturn(variableInstanceMock);

    given().pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .and().contentType(ContentType.TEXT)
    .and().body(is(equalTo(new String())))
    .when().get(VARIABLE_INSTANCE_BINARY_DATA_URL);
  }

  @Test
  void testDeleteSingleVariableInstanceById() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(VARIABLE_INSTANCE_URL);

    verify(historyServiceMock).deleteHistoricVariableInstance(MockProvider.EXAMPLE_VARIABLE_INSTANCE_ID);
  }

  @Test
  void testDeleteNonExistingVariableInstanceById() {
    doThrow(new NotFoundException("No historic variable instance found with id: 'NON_EXISTING_ID'"))
    .when(historyServiceMock).deleteHistoricVariableInstance("NON_EXISTING_ID");

    given()
      .pathParam("id", "NON_EXISTING_ID")
    .expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .body(containsString("No historic variable instance found with id: 'NON_EXISTING_ID'"))
    .when()
      .delete(VARIABLE_INSTANCE_URL);
  }
}
