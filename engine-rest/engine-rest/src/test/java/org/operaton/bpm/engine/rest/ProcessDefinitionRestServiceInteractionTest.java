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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.operaton.bpm.ProcessApplicationService;
import org.operaton.bpm.application.ProcessApplicationInfo;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidationException;
import org.operaton.bpm.engine.impl.repository.CalledProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.repository.CalledProcessDefinition;
import org.operaton.bpm.engine.repository.DeleteProcessDefinitionsBuilder;
import org.operaton.bpm.engine.repository.DeleteProcessDefinitionsSelectBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.rest.dto.HistoryTimeToLiveDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.EqualsMap;
import org.operaton.bpm.engine.rest.helper.EqualsVariableMap;
import org.operaton.bpm.engine.rest.helper.ErrorMessageHelper;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.VariableTypeHelper;
import org.operaton.bpm.engine.rest.helper.variable.EqualsObjectValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsPrimitiveValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsUntypedValue;
import org.operaton.bpm.engine.rest.sub.repository.impl.ProcessDefinitionResourceImpl;
import org.operaton.bpm.engine.rest.util.EncodingUtil;
import org.operaton.bpm.engine.rest.util.ModificationInstructionBuilder;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.operaton.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.VariableMapImpl;
import org.operaton.bpm.engine.variable.type.ValueType;

import static org.operaton.bpm.engine.rest.helper.MockProvider.createMockSerializedVariables;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class ProcessDefinitionRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String PROCESS_DEFINITION_URL = TEST_RESOURCE_ROOT_PATH + "/process-definition";
  protected static final String SINGLE_PROCESS_DEFINITION_URL = PROCESS_DEFINITION_URL + "/{id}";
  protected static final String SINGLE_PROCESS_DEFINITION_BY_KEY_URL = PROCESS_DEFINITION_URL + "/key/{key}";
  protected static final String SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_URL = PROCESS_DEFINITION_URL + "/key/{key}/tenant-id/{tenant-id}";

  protected static final String START_PROCESS_INSTANCE_URL = SINGLE_PROCESS_DEFINITION_URL + "/start";
  protected static final String START_PROCESS_INSTANCE_BY_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/start";
  protected static final String START_PROCESS_INSTANCE_BY_KEY_AND_TENANT_ID_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_URL + "/start";

  protected static final String XML_DEFINITION_URL = SINGLE_PROCESS_DEFINITION_URL + "/xml";
  protected static final String XML_DEFINITION_BY_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/xml";
  protected static final String DIAGRAM_DEFINITION_URL = SINGLE_PROCESS_DEFINITION_URL + "/diagram";
  protected static final String DIAGRAM_DEFINITION_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/diagram";

  protected static final String START_FORM_URL = SINGLE_PROCESS_DEFINITION_URL + "/startForm";
  protected static final String START_FORM_BY_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/startForm";
  protected static final String DEPLOYED_START_FORM_URL = SINGLE_PROCESS_DEFINITION_URL + "/deployed-start-form";
  protected static final String DEPLOYED_START_FORM_BY_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/deployed-start-form";
  protected static final String RENDERED_FORM_URL = SINGLE_PROCESS_DEFINITION_URL + "/rendered-form";
  protected static final String RENDERED_FORM_BY_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/rendered-form";
  protected static final String SUBMIT_FORM_URL = SINGLE_PROCESS_DEFINITION_URL + "/submit-form";
  protected static final String SUBMIT_FORM_BY_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/submit-form";
  protected static final String START_FORM_VARIABLES_URL = SINGLE_PROCESS_DEFINITION_URL + "/form-variables";
  protected static final String START_FORM_VARIABLES_BY_KEY_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/form-variables";

  protected static final String SINGLE_PROCESS_DEFINITION_SUSPENDED_URL = SINGLE_PROCESS_DEFINITION_URL + "/suspended";
  protected static final String SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/suspended";
  protected static final String SINGLE_PROCESS_DEFINITION_HISTORY_TIMETOLIVE_URL = SINGLE_PROCESS_DEFINITION_URL + "/history-time-to-live";
  protected static final String PROCESS_DEFINITION_SUSPENDED_URL = PROCESS_DEFINITION_URL + "/suspended";
  protected static final String SINGLE_PROCESS_DEFINITION_BY_KEY_DELETE_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_URL + "/delete";
  protected static final String SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_DELETE_URL = SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_URL + "/delete";

  protected static final String PROCESS_DEFINITION_CALL_ACTIVITY_MAPPINGS = SINGLE_PROCESS_DEFINITION_URL + "/static-called-process-definitions";

  private RuntimeService runtimeServiceMock;
  private RepositoryService repositoryServiceMock;
  private FormService formServiceMock;
  private ProcessDefinitionQuery processDefinitionQueryMock;
  private ProcessInstantiationBuilder mockInstantiationBuilder;

  @BeforeEach
  void setUpRuntimeData() {
    ProcessDefinition mockDefinition = MockProvider.createMockDefinition();
    setUpRuntimeDataForDefinition(mockDefinition);

    var managementServiceMock = mock(ManagementService.class);
    when(processEngine.getManagementService()).thenReturn(managementServiceMock);
    when(managementServiceMock.getProcessApplicationForDeployment(MockProvider.EXAMPLE_DEPLOYMENT_ID)).thenReturn(MockProvider.EXAMPLE_PROCESS_APPLICATION_NAME);

    // replace the runtime container delegate & process application service with a mock

    ProcessApplicationService processApplicationService = mock(ProcessApplicationService.class);
    ProcessApplicationInfo appMock = MockProvider.createMockProcessApplicationInfo();
    when(processApplicationService.getProcessApplicationInfo(MockProvider.EXAMPLE_PROCESS_APPLICATION_NAME)).thenReturn(appMock);

    RuntimeContainerDelegate delegate = mock(RuntimeContainerDelegate.class);
    when(delegate.getProcessApplicationService()).thenReturn(processApplicationService);
    RuntimeContainerDelegate.INSTANCE.set(delegate);
  }

  private void setUpRuntimeDataForDefinition(ProcessDefinition mockDefinition) {
    var mockInstance = MockProvider.createMockInstanceWithVariables();

    // we replace this mock with every test in order to have a clean one (in terms of invocations) for verification
    runtimeServiceMock = mock(RuntimeService.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeServiceMock);
    when(runtimeServiceMock.startProcessInstanceById(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), Mockito.<Map<String, Object>>any())).thenReturn(mockInstance);
    when(runtimeServiceMock.startProcessInstanceById(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), any(), any(), any())).thenReturn(mockInstance);


    mockInstantiationBuilder = setUpMockInstantiationBuilder();
    when(mockInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean())).thenReturn(mockInstance);
    when(runtimeServiceMock.createProcessInstanceById(any())).thenReturn(mockInstantiationBuilder);


    repositoryServiceMock = mock(RepositoryService.class);
    when(processEngine.getRepositoryService()).thenReturn(repositoryServiceMock);
    when(repositoryServiceMock.getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(mockDefinition);
    when(repositoryServiceMock.getProcessModel(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(createMockProcessDefinitionBpmn20Xml());

    DeleteProcessDefinitionsSelectBuilder deleteProcessDefinitionsSelectBuilder = mock(DeleteProcessDefinitionsSelectBuilder.class, RETURNS_DEEP_STUBS);
    when(repositoryServiceMock.deleteProcessDefinitions()).thenReturn(deleteProcessDefinitionsSelectBuilder);

    setUpMockDefinitionQuery(mockDefinition);

    StartFormData formDataMock = MockProvider.createMockStartFormData(mockDefinition);
    formServiceMock = mock(FormService.class);
    when(processEngine.getFormService()).thenReturn(formServiceMock);
    when(formServiceMock.getStartFormData(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(formDataMock);
    when(formServiceMock.getStartFormKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(MockProvider.EXAMPLE_FORM_KEY);
    when(formServiceMock.submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), any())).thenReturn(mockInstance);
    when(formServiceMock.submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), any(), any())).thenReturn(mockInstance);

    VariableMap startFormVariablesMock = MockProvider.createMockFormVariables();
    when(formServiceMock.getStartFormVariables(eq(EXAMPLE_PROCESS_DEFINITION_ID), any(), anyBoolean())).thenReturn(startFormVariablesMock);

  }

  private InputStream createMockProcessDefinitionBpmn20Xml() {
    // do not close the input stream, will be done in implementation
    InputStream bpmn20XmlIn = ReflectUtil.getResourceAsStream("processes/fox-invoice_en_long_id.bpmn");
    assertThat(bpmn20XmlIn).isNotNull();
    return bpmn20XmlIn;
  }

  private void setUpMockDefinitionQuery(ProcessDefinition mockDefinition) {
    processDefinitionQueryMock = mock(ProcessDefinitionQuery.class);
    when(processDefinitionQueryMock.processDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.processDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.tenantIdIn(anyString())).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.withoutTenantId()).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.latestVersion()).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.singleResult()).thenReturn(mockDefinition);
    when(processDefinitionQueryMock.count()).thenReturn(1L);
    when(processDefinitionQueryMock.list()).thenReturn(Collections.singletonList(mockDefinition));
    when(repositoryServiceMock.createProcessDefinitionQuery()).thenReturn(processDefinitionQueryMock);
  }

  @Test
  void testInstanceResourceLinkResult() {
    String fullInstanceUrl = "http://localhost:" + PORT + TEST_RESOURCE_ROOT_PATH + "/process-instance/" + MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("links[0].href", equalTo(fullInstanceUrl))
      .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testInstanceResourceLinkWithEnginePrefix() {
    String startInstanceOnExplicitEngineUrl = TEST_RESOURCE_ROOT_PATH + "/engine/default/process-definition/{id}/start";

    String fullInstanceUrl = "http://localhost:" + PORT + TEST_RESOURCE_ROOT_PATH + "/engine/default/process-instance/" + MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("links[0].href", equalTo(fullInstanceUrl))
      .when().post(startInstanceOnExplicitEngineUrl);
  }

  @Test
  void testProcessDefinitionBpmn20XmlRetrieval() {
    // Rest-assured has problems with extracting json with escaped quotation marks, i.e. the xml content in our case
    Response response = given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
//      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
//      .body("bpmn20Xml", startsWith("<?xml"))
    .when().get(XML_DEFINITION_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains("<?xml")
      .contains(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testProcessDiagramRetrieval() throws Exception {
    // setup additional mock behavior
    File file = getFile("/processes/todo-process.png");
    when(repositoryServiceMock.getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenReturn(new FileInputStream(file));

    // call method
    byte[] actual = given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType("image/png")
          .header("Content-Disposition", "attachment; " +
                  "filename=\"" + MockProvider.EXAMPLE_PROCESS_DEFINITION_DIAGRAM_RESOURCE_NAME + "\"; " +
                  "filename*=UTF-8''" + MockProvider.EXAMPLE_PROCESS_DEFINITION_DIAGRAM_RESOURCE_NAME)
        .when().get(DIAGRAM_DEFINITION_URL).getBody().asByteArray();

    // verify service interaction
    verify(repositoryServiceMock).getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(repositoryServiceMock).getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    // compare input stream with response body bytes
    byte[] expected = IoUtil.readInputStream(new FileInputStream(file), "process diagram");
    assertThat(actual).containsExactly(expected);
  }

  @Test
  void testProcessDiagramNullFilename() throws Exception {
    // setup additional mock behavior
    File file = getFile("/processes/todo-process.png");
    when(repositoryServiceMock.getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID).getDiagramResourceName())
      .thenReturn(null);
    when(repositoryServiceMock.getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .thenReturn(new FileInputStream(file));

    // call method
    byte[] actual = given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType("application/octet-stream")
      .header("Content-Disposition", "attachment; " +
              "filename=\"" + null + "\"; " +
              "filename*=UTF-8''" + null)
      .when().get(DIAGRAM_DEFINITION_URL).getBody().asByteArray();

    // verify service interaction
    verify(repositoryServiceMock).getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    // compare input stream with response body bytes
    byte[] expected = IoUtil.readInputStream(new FileInputStream(file), "process diagram");
    assertThat(actual).containsExactly(expected);
  }

  @Test
  void testProcessDiagramNotExist() {
    // setup additional mock behavior
    when(repositoryServiceMock.getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(null);

    // call method
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
        .expect().statusCode(Status.NO_CONTENT.getStatusCode())
        .when().get(DIAGRAM_DEFINITION_URL);

    // verify service interaction
    verify(repositoryServiceMock).getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(repositoryServiceMock).getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testProcessDiagramMediaType() {
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.png")).isEqualTo("image/png");
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.PNG")).isEqualTo("image/png");
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.svg")).isEqualTo("image/svg+xml");
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.jpeg")).isEqualTo("image/jpeg");
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.jpg")).isEqualTo("image/jpeg");
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.gif")).isEqualTo("image/gif");
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.bmp")).isEqualTo("image/bmp");
    assertThat(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("process.UNKNOWN")).isEqualTo("application/octet-stream");
  }

  @Test
  void testGetProcessDiagramGetDefinitionThrowsAuthorizationException() {
    String message = "expected exception";
    when(repositoryServiceMock.getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(DIAGRAM_DEFINITION_URL);
  }

  @Test
  void testGetProcessDiagramThrowsAuthorizationException() {
    String message = "expected exception";
    when(repositoryServiceMock.getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(DIAGRAM_DEFINITION_URL);
  }

  @Test
  void testGetProcessDiagramGetDefinitionThrowsAuthorizationException_ByKey() {
    String message = "expected exception";
    when(repositoryServiceMock.getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(DIAGRAM_DEFINITION_KEY_URL);
  }

  @Test
  void testGetProcessDiagramThrowsAuthorizationException_ByKey() {
    String message = "expected exception";
    when(repositoryServiceMock.getProcessDiagram(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(DIAGRAM_DEFINITION_KEY_URL);
  }

  @Test
  void testGetStartFormData() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("key", equalTo(MockProvider.EXAMPLE_FORM_KEY))
    .when().get(START_FORM_URL);
  }

  @Test
  void testGetStartForm_shouldReturnKeyContainingTaskId() {
    ProcessDefinition mockDefinition = MockProvider.createMockDefinition();
    StartFormData mockStartFormData = MockProvider.createMockStartFormDataUsingFormFieldsWithoutFormKey(mockDefinition);
    when(formServiceMock.getStartFormData(mockDefinition.getId())).thenReturn(mockStartFormData);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("key", equalTo("embedded:engine://engine/:engine/process-definition/" + mockDefinition.getId() + "/rendered-form"))
      .body("contextPath", equalTo(MockProvider.EXAMPLE_PROCESS_APPLICATION_CONTEXT_PATH))
      .when().get(START_FORM_URL);
  }

  @Test
  void testGetStartForm_shouldReturnOperatonFormRef() {
    StartFormData mockStartFormData = MockProvider.createMockStartFormDataUsingFormRef();
    when(formServiceMock.getStartFormData(anyString())).thenReturn(mockStartFormData);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("key", nullValue())
      .body("operatonFormRef.key", equalTo(MockProvider.EXAMPLE_FORM_KEY))
      .body("operatonFormRef.binding", equalTo(MockProvider.EXAMPLE_FORM_REF_BINDING))
      .body("operatonFormRef.version", equalTo(MockProvider.EXAMPLE_FORM_REF_VERSION))
    .when().get(START_FORM_URL);
  }

  @Test
  void testGetStartForm_StartFormDataEqualsNull() {
    ProcessDefinition mockDefinition = MockProvider.createMockDefinition();
    when(formServiceMock.getStartFormData(mockDefinition.getId())).thenReturn(null);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("contextPath", equalTo(MockProvider.EXAMPLE_PROCESS_APPLICATION_CONTEXT_PATH))
      .when().get(START_FORM_URL);
  }

  @Test
  void testGetStartFormThrowsAuthorizationException() {
    String message = "expected exception";
    when(formServiceMock.getStartFormData(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(START_FORM_URL);
  }

  @Test
  void testGetRenderedStartForm() {
    String expectedResult = "<formField>anyContent</formField>";

    when(formServiceMock.getRenderedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(expectedResult);

    Response response = given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(XHTML_XML_CONTENT_TYPE)
      .when()
        .get(RENDERED_FORM_URL);

    String responseContent = response.asString();
    assertThat(responseContent).isEqualTo(expectedResult);
  }

  @Test
  void testGetRenderedStartFormForDifferentPlatformEncoding() {
    String expectedResult = "<formField>unicode symbol: \u2200</formField>";
    when(formServiceMock.getRenderedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(expectedResult);

    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
        .then()
          .expect()
            .statusCode(Status.OK.getStatusCode())
            .contentType(XHTML_XML_CONTENT_TYPE)
        .when()
          .get(RENDERED_FORM_URL);

    String responseContent = new String(response.asByteArray(), EncodingUtil.DEFAULT_ENCODING);
    assertThat(responseContent).isEqualTo(expectedResult);
  }

  @Test
  void testGetRenderedStartFormReturnsNotFound() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .then()
        .expect()
          .statusCode(Status.NOT_FOUND.getStatusCode())
          .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
          .body("message", equalTo("No matching rendered start form for process definition with the id " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + " found."))
      .when()
        .get(RENDERED_FORM_URL);
  }

  @Test
  void testGetRenderedStartFormThrowsAuthorizationException() {
    String message = "expected exception";
    when(formServiceMock.getRenderedStartForm(anyString())).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(RENDERED_FORM_URL);
  }

  @Test
  void testSubmitStartForm() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
      .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
      .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
    .when().post(SUBMIT_FORM_URL);

    verify(formServiceMock).submitStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, null);
  }

  @Test
  void testSubmitStartFormWithParameters() {
    Map<String, Object> variables = VariablesBuilder.create()
        .variable("aVariable", "aStringValue")
        .variable("anotherVariable", 42)
        .variable("aThirdValue", Boolean.TRUE).getVariables();

    Map<String, Object> json = new HashMap<>();
    json.put("variables", variables);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aVariable", "aStringValue");
    expectedVariables.put("anotherVariable", 42);
    expectedVariables.put("aThirdValue", Boolean.TRUE);

    verify(formServiceMock).submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), argThat(new EqualsMap(expectedVariables)));
  }

  @Test
  void testSubmitStartFormWithSerializedVariableValue() {

    String jsonValue = "{}";

    Map<String, Object> variables = VariablesBuilder.create()
        .variable("aVariable", "aStringValue")
        .variable("aSerializedVariable", ValueType.OBJECT.getName(), jsonValue, "aFormat", "aRootType")
        .getVariables();

    Map<String, Object> json = new HashMap<>();
    json.put("variables", variables);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_URL);

    verify(formServiceMock).submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID),
        argThat(
            new EqualsVariableMap()
              .matcher("aVariable", EqualsUntypedValue.matcher().value("aStringValue"))
              .matcher("aSerializedVariable", EqualsObjectValue
                                                .objectValueMatcher()
                                                .serializedValue(jsonValue)
                                                .serializationFormat("aFormat")
                                                .objectTypeName("aRootType"))));
  }

  @Test
  void testSubmitStartFormWithBase64EncodedBytes() {

    Map<String, Object> variables = VariablesBuilder.create()
        .variable("aVariable", Base64.getEncoder().encodeToString("someBytes".getBytes()), ValueType.BYTES.getName())
        .getVariables();

    Map<String, Object> json = new HashMap<>();
    json.put("variables", variables);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_URL);

    verify(formServiceMock).submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID),
        argThat(
            new EqualsVariableMap()
              .matcher("aVariable", EqualsPrimitiveValue.bytesValue("someBytes".getBytes()))));
  }

  @Test
  void testSubmitStartFormWithBusinessKey() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_URL);

    verify(formServiceMock).submitStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, "myBusinessKey", null);
  }

  @Test
  void testSubmitStartFormWithBusinessKeyAndParameters() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    Map<String, Object> variables = VariablesBuilder.create()
        .variable("aVariable", "aStringValue")
        .variable("anotherVariable", 42)
        .variable("aThirdValue", Boolean.TRUE).getVariables();

    json.put("variables", variables);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aVariable", "aStringValue");
    expectedVariables.put("anotherVariable", 42);
    expectedVariables.put("aThirdValue", Boolean.TRUE);

    verify(formServiceMock).submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), eq("myBusinessKey"), argThat(new EqualsMap(expectedVariables)));
  }

  @Test
  void testSubmitStartFormWithUnparseableIntegerVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(SUBMIT_FORM_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableShortVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
    .when().post(SUBMIT_FORM_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableLongVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
    .when().post(SUBMIT_FORM_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableDoubleVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
    .when().post(SUBMIT_FORM_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableDateVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(SUBMIT_FORM_URL);
  }

  @Test
  void testSubmitStartFormWithNotSupportedVariableType() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: Unsupported value type 'X'"))
    .when().post(SUBMIT_FORM_URL);
  }

  @Test
  void testUnsuccessfulSubmitStartForm() {
    doThrow(new ProcessEngineException("expected exception")).when(formServiceMock).submitStartForm(any(String.class), Mockito.any());

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", equalTo("Cannot instantiate process definition " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + ": expected exception"))
      .when().post(SUBMIT_FORM_URL);
  }

  @Test
  void testSubmitFormByIdThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(formServiceMock).submitStartForm(any(String.class), Mockito.any());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(SUBMIT_FORM_URL);
  }

  @Test
  void testSubmitFormByIdThrowsFormFieldValidationException() {
    String message = "expected exception";
    doThrow(new FormFieldValidationException("form-exception", message)).when(formServiceMock).submitStartForm(any(String.class), Mockito.any());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", equalTo("Cannot instantiate process definition " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + ": " + message))
    .when()
      .post(SUBMIT_FORM_URL);
  }

  @Test
  void testGetStartFormVariables() {

    given().pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .then().expect()
        .statusCode(Status.OK.getStatusCode()).contentType(ContentType.JSON)
        .body(MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME+".value", equalTo(MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getValue()))
        .body(MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME+".type",
            equalTo(VariableTypeHelper.toExpectedValueTypeName(MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getType())))
      .when().get(START_FORM_VARIABLES_URL)
      .body();

    verify(formServiceMock, times(1)).getStartFormVariables(EXAMPLE_PROCESS_DEFINITION_ID, null, true);
  }

  @Test
  void testGetStartFormVariablesVarNames() {

    given()
      .pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("variableNames", "a,b,c")
    .then().expect()
      .statusCode(Status.OK.getStatusCode()).contentType(ContentType.JSON)
    .when().get(START_FORM_VARIABLES_URL);

    verify(formServiceMock, times(1)).getStartFormVariables(EXAMPLE_PROCESS_DEFINITION_ID, Arrays.asList("a", "b", "c"), true);
  }

  @Test
  void testGetStartFormVariablesAndDoNotDeserializeVariables() {

    given()
      .pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("deserializeValues", false)
     .then()
       .expect()
        .statusCode(Status.OK.getStatusCode()).contentType(ContentType.JSON)
        .body(MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME+".value", equalTo(MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getValue()))
        .body(MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME+".type",
            equalTo(VariableTypeHelper.toExpectedValueTypeName(MockProvider.EXAMPLE_PRIMITIVE_VARIABLE_VALUE.getType())))
      .when().get(START_FORM_VARIABLES_URL)
      .body();

    verify(formServiceMock, times(1)).getStartFormVariables(EXAMPLE_PROCESS_DEFINITION_ID, null, false);
  }

  @Test
  void testGetStartFormVariablesVarNamesAndDoNotDeserializeVariables() {

    given()
      .pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("deserializeValues", false)
      .queryParam("variableNames", "a,b,c")
    .then().expect()
      .statusCode(Status.OK.getStatusCode()).contentType(ContentType.JSON)
    .when().get(START_FORM_VARIABLES_URL);

    verify(formServiceMock, times(1)).getStartFormVariables(EXAMPLE_PROCESS_DEFINITION_ID, Arrays.asList("a", "b", "c"), false);
  }

  @Test
  void testGetStartFormVariablesThrowsAuthorizationException() {
    String message = "expected exception";
    when(formServiceMock.getStartFormVariables(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, null, true)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(START_FORM_VARIABLES_URL);
  }

  @Test
  void testSimpleProcessInstantiation() {
   given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
            .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
            .then().expect()
            .statusCode(Status.OK.getStatusCode())
            .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
            .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testSimpleProcessInstantiationWithVariables() {
    //mock process instance
    ProcessInstanceWithVariables mockProcessInstance = MockProvider.createMockInstanceWithVariables();
    ProcessInstantiationBuilder mockProcessInstantiationBuilder = setUpMockInstantiationBuilder();
    when(mockProcessInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean())).thenReturn(mockProcessInstance);
    when(runtimeServiceMock.createProcessInstanceById(anyString())).thenReturn(mockProcessInstantiationBuilder);

    //given request with parameter withVariables to get variables in return
    Map<String, Object> json = new HashMap<>();
    json.put("withVariablesInReturn", true);

    //when request then return process instance with serialized variables
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
            .contentType(POST_JSON_CONTENT_TYPE).body(json)
            .then().expect()
            .statusCode(Status.OK.getStatusCode())
            .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
            //serialized variable
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".value",
                    equalTo(MockProvider.EXAMPLE_VARIABLE_INSTANCE_SERIALIZED_VALUE))
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".type",
                    equalTo("Object"))
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".valueInfo.objectTypeName",
                    equalTo(ArrayList.class.getName()))
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".valueInfo.serializationDataFormat",
                    equalTo(MockProvider.FORMAT_APPLICATION_JSON))
            //deserialized variable should also be returned as serialized variable
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".value",
                    equalTo(MockProvider.EXAMPLE_VARIABLE_INSTANCE_SERIALIZED_VALUE))
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".type",
                    equalTo("Object"))
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".valueInfo.objectTypeName",
                    equalTo(Object.class.getName()))
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".valueInfo.serializationDataFormat",
                    equalTo(MockProvider.FORMAT_APPLICATION_JSON))
            .when().post(START_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockProcessInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());

  }

  @Test
  void testProcessInstantiationWithParameters() {
    Map<String, Object> parameters = VariablesBuilder.create()
        .variable("aBoolean", Boolean.TRUE)
        .variable("aString", "aStringVariableValue")
        .variable("anInteger", 42).getVariables();

    Map<String, Object> json = new HashMap<>();
    json.put("variables", parameters);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    Map<String, Object> expectedParameters = new HashMap<>();
    expectedParameters.put("aBoolean", Boolean.TRUE);
    expectedParameters.put("aString", "aStringVariableValue");
    expectedParameters.put("anInteger", 42);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).setVariables(argThat(new EqualsMap(expectedParameters)));
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationWithBusinessKey() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).businessKey("myBusinessKey");
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationWithBusinessKeyAndParameters() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    Map<String, Object> parameters = VariablesBuilder.create()
        .variable("aBoolean", Boolean.TRUE)
        .variable("aString", "aStringVariableValue")
        .variable("anInteger", 42).getVariables();

    json.put("variables", parameters);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    Map<String, Object> expectedParameters = new HashMap<>();
    expectedParameters.put("aBoolean", Boolean.TRUE);
    expectedParameters.put("aString", "aStringVariableValue");
    expectedParameters.put("anInteger", 42);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).businessKey("myBusinessKey");
    verify(mockInstantiationBuilder).setVariables(argThat(new EqualsMap(expectedParameters)));
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationWithTransientVariables() {
    Map<String, Object> json = new HashMap<>();

    json.put("variables", VariablesBuilder.create().variableTransient("foo", "bar", "string").getVariables());

    final VariableMap varMap = new VariableMapImpl();

    when(mockInstantiationBuilder.setVariables(anyMap())).thenAnswer((Answer<ProcessInstantiationBuilder>) invocation -> {
      varMap.putAll((VariableMap) invocation.getArguments()[0]);
      return mockInstantiationBuilder;
    });

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    VariableMap expectedVariables = Variables.createVariables().putValueTyped("foo", Variables.stringValue("bar", true));
    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).setVariables(expectedVariables);
    assertThat(varMap.getValueTyped("foo").isTransient()).isEqualTo(expectedVariables.getValueTyped("foo").isTransient());
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationAtActivitiesById() {

    Map<String, Object> json = new HashMap<>();
    json.put("variables", VariablesBuilder.create()
        .variable("processVariable", "aString", "String").getVariables());
    json.put("businessKey", "aBusinessKey");
    json.put("caseInstanceId", "aCaseInstanceId");

    List<Map<String, Object>> startInstructions = new ArrayList<>();

    startInstructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", "value", "String", false)
              .variable("varLocal", "valueLocal", "String", true)
              .getVariables())
          .getJson());
    startInstructions.add(
        ModificationInstructionBuilder.startAfter()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", 52, "Integer", false)
              .variable("varLocal", 74, "Integer", true)
              .getVariables())
          .getJson());
    startInstructions.add(
        ModificationInstructionBuilder.startTransition()
          .transitionId("transitionId")
          .variables(VariablesBuilder.create()
              .variable("var", 53, "Integer", false)
              .variable("varLocal", 75, "Integer", true)
              .getVariables())
          .getJson());


    json.put("startInstructions", startInstructions);

    given().pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).createProcessInstanceById(EXAMPLE_PROCESS_DEFINITION_ID);

    InOrder inOrder = inOrder(mockInstantiationBuilder);

    inOrder.verify(mockInstantiationBuilder).businessKey("aBusinessKey");
    inOrder.verify(mockInstantiationBuilder).caseInstanceId("aCaseInstanceId");
    inOrder.verify(mockInstantiationBuilder).setVariables(argThat(EqualsVariableMap.matches()
        .matcher("processVariable", EqualsPrimitiveValue.stringValue("aString"))));

    inOrder.verify(mockInstantiationBuilder).startBeforeActivity("activityId");

    verify(mockInstantiationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.stringValue("valueLocal")));
    verify(mockInstantiationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.stringValue("value")));

    inOrder.verify(mockInstantiationBuilder).startAfterActivity("activityId");

    verify(mockInstantiationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.integerValue(52)));
    verify(mockInstantiationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.integerValue(74)));

    inOrder.verify(mockInstantiationBuilder).startTransition("transitionId");

    verify(mockInstantiationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.integerValue(53)));
    verify(mockInstantiationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.integerValue(75)));

    inOrder.verify(mockInstantiationBuilder).executeWithVariablesInReturn(false, false);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testProcessInstantiationAtActivitiesByIdWithVariablesInReturn() {
    //set up variables and parameters
    Map<String, Object> json = new HashMap<>();
    json.put("variables", VariablesBuilder.create()
        .variable("processVariable", "aString", "String").getVariables());
    json.put("businessKey", "aBusinessKey");
    json.put("caseInstanceId", "aCaseInstanceId");

    VariableMap variables = createMockSerializedVariables()
            .putValueTyped("processVariable", Variables.stringValue("aString"))
            .putValueTyped("var", Variables.stringValue("value"))
            .putValueTyped("varLocal", Variables.stringValue("valueLocal"));

    //mock process instance and instantiation builder
    ProcessInstanceWithVariables mockProcessInstance = MockProvider.createMockInstanceWithVariables();
    when(mockProcessInstance.getVariables()).thenReturn(variables);

    ProcessInstantiationBuilder mockProcessInstantiationBuilder = setUpMockInstantiationBuilder();
    when(mockProcessInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean())).thenReturn(mockProcessInstance);
    when(runtimeServiceMock.createProcessInstanceById(anyString())).thenReturn(mockProcessInstantiationBuilder);

    //create instructions
    List<Map<String, Object>> startInstructions = new ArrayList<>();

    startInstructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", "value", "String", false)
              .variable("varLocal", "valueLocal", "String", true)
              .getVariables())
          .getJson());

    json.put("startInstructions", startInstructions);
    json.put("withVariablesInReturn", true);

    //request which response should contain serialized variables of process instance
    given().pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
            .contentType(POST_JSON_CONTENT_TYPE).body(json)
            .then().expect()
            .statusCode(Status.OK.getStatusCode())
            //serialized variable
            .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".value",
                    equalTo(MockProvider.EXAMPLE_VARIABLE_INSTANCE_SERIALIZED_VALUE))
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".type",
                    equalTo("Object"))
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".valueInfo.objectTypeName",
                    equalTo(ArrayList.class.getName()))
            .body("variables." + MockProvider.EXAMPLE_VARIABLE_INSTANCE_NAME + ".valueInfo.serializationDataFormat",
                    equalTo(MockProvider.FORMAT_APPLICATION_JSON))
            //deserialized variable should also be returned as serialized variable
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".value",
                    equalTo(MockProvider.EXAMPLE_VARIABLE_INSTANCE_SERIALIZED_VALUE))
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".type",
                    equalTo("Object"))
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".valueInfo.objectTypeName",
                    equalTo(Object.class.getName()))
            .body("variables." + MockProvider.EXAMPLE_DESERIALIZED_VARIABLE_INSTANCE_NAME + ".valueInfo.serializationDataFormat",
                    equalTo(MockProvider.FORMAT_APPLICATION_JSON))
            .body("variables.processVariable.type", equalTo("String"))
            .body("variables.processVariable.value", equalTo("aString"))
            .body("variables.var.type", equalTo("String"))
            .body("variables.var.value", equalTo("value"))
            .body("variables.varLocal.type", equalTo("String"))
            .body("variables.varLocal.value", equalTo("valueLocal"))
            .when().post(START_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).createProcessInstanceById(EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testProcessInstantiationAtActivitiesByKey() {
    ProcessInstantiationBuilder mockProcessInstantiationBuilder = setUpMockInstantiationBuilder();
    when(runtimeServiceMock.createProcessInstanceById(anyString())).thenReturn(mockProcessInstantiationBuilder);

    Map<String, Object> json = new HashMap<>();
    json.put("variables", VariablesBuilder.create()
        .variable("processVariable", "aString", "String").getVariables());
    json.put("businessKey", "aBusinessKey");
    json.put("caseInstanceId", "aCaseInstanceId");

    List<Map<String, Object>> startInstructions = new ArrayList<>();

    startInstructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", "value", "String", false)
              .variable("varLocal", "valueLocal", "String", true)
              .getVariables())
          .getJson());
    startInstructions.add(
        ModificationInstructionBuilder.startAfter()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", 52, "Integer", false)
              .variable("varLocal", 74, "Integer", true)
              .getVariables())
          .getJson());
    startInstructions.add(
        ModificationInstructionBuilder.startTransition()
          .transitionId("transitionId")
          .variables(VariablesBuilder.create()
              .variable("var", 53, "Integer", false)
              .variable("varLocal", 75, "Integer", true)
              .getVariables())
          .getJson());


    json.put("startInstructions", startInstructions);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    InOrder inOrder = inOrder(mockProcessInstantiationBuilder);

    inOrder.verify(mockProcessInstantiationBuilder).businessKey("aBusinessKey");
    inOrder.verify(mockProcessInstantiationBuilder).caseInstanceId("aCaseInstanceId");
    inOrder.verify(mockProcessInstantiationBuilder).setVariables(argThat(EqualsVariableMap.matches()
        .matcher("processVariable", EqualsPrimitiveValue.stringValue("aString"))));

    inOrder.verify(mockProcessInstantiationBuilder).startBeforeActivity("activityId");

    verify(mockProcessInstantiationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.stringValue("valueLocal")));
    verify(mockProcessInstantiationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.stringValue("value")));

    inOrder.verify(mockProcessInstantiationBuilder).startAfterActivity("activityId");

    verify(mockProcessInstantiationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.integerValue(52)));
    verify(mockProcessInstantiationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.integerValue(74)));

    inOrder.verify(mockProcessInstantiationBuilder).startTransition("transitionId");

    verify(mockProcessInstantiationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.integerValue(53)));
    verify(mockProcessInstantiationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.integerValue(75)));

    inOrder.verify(mockProcessInstantiationBuilder).executeWithVariablesInReturn(false, false);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testProcessInstantiationAtActivitiesSkipIoMappingsAndListeners() {
    ProcessInstantiationBuilder mockProcessInstantiationBuilder = setUpMockInstantiationBuilder();
    when(runtimeServiceMock.createProcessInstanceById(anyString())).thenReturn(mockProcessInstantiationBuilder);

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> startInstructions = new ArrayList<>();

    startInstructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .getJson());

    json.put("startInstructions", startInstructions);
    json.put("skipIoMappings", true);
    json.put("skipCustomListeners", true);

    given().pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).createProcessInstanceById(EXAMPLE_PROCESS_DEFINITION_ID);

    InOrder inOrder = inOrder(mockProcessInstantiationBuilder);

    inOrder.verify(mockProcessInstantiationBuilder).startBeforeActivity("activityId");
    inOrder.verify(mockProcessInstantiationBuilder).executeWithVariablesInReturn(true, true);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testInvalidInstantiationAtActivities() {
    ProcessInstantiationBuilder mockProcessInstantiationBuilder = setUpMockInstantiationBuilder();
    when(runtimeServiceMock.createProcessInstanceById(anyString())).thenReturn(mockProcessInstantiationBuilder);

    Map<String, Object> json = new HashMap<>();

    // start before: missing activity id
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startBefore().getJson());
    json.put("startInstructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", is(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("'activityId' must be set"))
    .when()
      .post(START_PROCESS_INSTANCE_URL);

    // start after: missing ancestor activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().getJson());
    json.put("startInstructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", is(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("'activityId' must be set"))
    .when()
      .post(START_PROCESS_INSTANCE_URL);

    // start transition: missing ancestor activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startTransition().getJson());
    json.put("startInstructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", is(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("'transitionId' must be set"))
    .when()
      .post(START_PROCESS_INSTANCE_URL);
  }

  protected ProcessInstantiationBuilder setUpMockInstantiationBuilder() {
    ProcessInstanceWithVariables resultInstanceWithVariables = MockProvider.createMockInstanceWithVariables();
    ProcessInstantiationBuilder processInstantiationBuilder = mock(ProcessInstantiationBuilder.class);

    when(processInstantiationBuilder.startAfterActivity(any())).thenReturn(processInstantiationBuilder);
    when(processInstantiationBuilder.startBeforeActivity(any())).thenReturn(processInstantiationBuilder);
    when(processInstantiationBuilder.startTransition(any())).thenReturn(processInstantiationBuilder);
    when(processInstantiationBuilder.setVariables(any())).thenReturn(processInstantiationBuilder);
    when(processInstantiationBuilder.setVariablesLocal(any())).thenReturn(processInstantiationBuilder);
    when(processInstantiationBuilder.businessKey(any())).thenReturn(processInstantiationBuilder);
    when(processInstantiationBuilder.caseInstanceId(any())).thenReturn(processInstantiationBuilder);
    when(processInstantiationBuilder.execute(anyBoolean(), anyBoolean())).thenReturn(resultInstanceWithVariables);
    when(processInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean())).thenReturn(resultInstanceWithVariables);

    return processInstantiationBuilder;
  }

  /**
   * {@link RuntimeService#startProcessInstanceById(String, Map)} throws an {@link ProcessEngineException}, if a definition with the given id does not exist.
   */
  @Test
  void testUnsuccessfulInstantiation() {
    when(mockInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean()))
      .thenThrow(new ProcessEngineException("expected exception"));

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", containsString("Cannot instantiate process definition"))
      .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testStartProcessInstanceByIdThrowsAuthorizationException() {
    String message = "expected exception";
    when(mockInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean()))
      .thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testDefinitionRetrieval() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .body("key", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY))
      .body("category", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_CATEGORY))
      .body("name", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_NAME))
      .body("description", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DESCRIPTION))
      .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
      .body("version", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_VERSION))
      .body("resource", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_RESOURCE_NAME))
      .body("diagram", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DIAGRAM_RESOURCE_NAME))
      .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_IS_SUSPENDED))
      .body("tenantId", nullValue())
    .when().get(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testNonExistingProcessDefinitionRetrieval() {
    String nonExistingId = "aNonExistingDefinitionId";
    when(repositoryServiceMock.getProcessDefinition(nonExistingId)).thenThrow(new ProcessEngineException("no matching definition"));

    given().pathParam("id", "aNonExistingDefinitionId")
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("No matching definition with id " + nonExistingId))
    .when().get(SINGLE_PROCESS_DEFINITION_URL);
  }

  @Test
  void testNonExistingProcessDefinitionBpmn20XmlRetrieval() {
    String nonExistingId = "aNonExistingDefinitionId";
    when(repositoryServiceMock.getProcessModel(nonExistingId)).thenThrow(new NotFoundException("no matching process definition found."));

    given().pathParam("id", nonExistingId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("No matching definition with id " + nonExistingId))
    .when().get(XML_DEFINITION_URL);
  }

  @Test
  void testGetProcessDefinitionBpmn20XmlThrowsProcessEngineException() {
    String processDefinitionId = "someId";
    when(repositoryServiceMock.getProcessModel(processDefinitionId)).thenThrow(new ProcessEngineException("generic message"));

    given().pathParam("id", processDefinitionId)
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
      .body("message", equalTo("generic message"))
    .when().get(XML_DEFINITION_URL);
  }

  @Test
  void testGetProcessDefinitionBpmn20XmlThrowsAuthorizationException() {
    String message = "expected exception";
    when(repositoryServiceMock.getProcessModel(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(XML_DEFINITION_URL);
  }

  @Test
  void testDeleteDeployment() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, false, false);
  }


  @Test
  void testDeleteDeploymentCascade() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("cascade", true)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, false, false);
  }

  @Test
  void testDeleteDeploymentCascadeNonsense() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("cascade", "bla")
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, false, false);
  }

  @Test
  void testDeleteDeploymentCascadeFalse() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("cascade", false)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, false, false);
  }

  @Test
  void testDeleteDeploymentSkipCustomListeners() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("skipCustomListeners", true)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, true, false);
  }

  @Test
  void testDeleteDeploymentSkipCustomListenersNonsense() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("skipCustomListeners", "bla")
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, false, false);
  }

  @Test
  void testDeleteDeploymentSkipCustomListenersFalse() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("skipCustomListeners", false)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, false, false);
  }

  @Test
  void testDeleteDeploymentSkipCustomListenersAndCascade() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("cascade", true)
      .queryParam("skipCustomListeners", true)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, true, false);
  }

  @Test
  void testDeleteNonExistingDeployment() {

    doThrow(new NotFoundException("No process definition found with id 'NON_EXISTING_ID'"))
            .when(repositoryServiceMock)
            .deleteProcessDefinition("NON_EXISTING_ID", false, false, false);

    given()
      .pathParam("id", "NON_EXISTING_ID")
    .expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .body(containsString("No process definition found with id 'NON_EXISTING_ID'"))
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);
  }

  @Test
  void testDeleteDeploymentThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, false, false);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", is(AuthorizationException.class.getSimpleName()))
      .body("message", is(message))
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);
  }

  @Test
  void testDeleteDefinitionSkipIoMappings() {

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .queryParam("skipIoMappings", true)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_URL);

    verify(repositoryServiceMock).deleteProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, false, true);
  }

  @Test
  void testDeleteDefinitionsByKey() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeyCascade() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .queryParam("cascade", true)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .cascade();

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeySkipCustomListeners() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .queryParam("skipCustomListeners", true)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .skipCustomListeners();

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeySkipIoMappings() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .queryParam("skipIoMappings", true)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .skipIoMappings();

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeySkipCustomListenersAndCascade() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .queryParam("cascade", true)
      .queryParam("skipCustomListeners", true)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .skipCustomListeners()
      .cascade();

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeyNotExistingKey() {
    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey("NOT_EXISTING_KEY");

    doThrow(new NotFoundException("No process definition found with key 'NOT_EXISTING_KEY'")).when(builder).delete();

    given()
      .pathParam("key", "NOT_EXISTING_KEY")
    .expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .body(containsString("No process definition found with key 'NOT_EXISTING_KEY'"))
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_DELETE_URL);
  }

  @Test
  void testDeleteDefinitionsByKeyWithTenantId() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .withTenantId(MockProvider.EXAMPLE_TENANT_ID);

    verify(builder).delete();
  }


  @Test
  void testDeleteDefinitionsByKeyCascadeWithTenantId() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
      .queryParam("cascade", true)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .withTenantId(MockProvider.EXAMPLE_TENANT_ID)
      .cascade();

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeySkipCustomListenersWithTenantId() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
      .queryParam("skipCustomListeners", true)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .withTenantId(MockProvider.EXAMPLE_TENANT_ID)
      .skipCustomListeners();

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeySkipCustomListenersAndCascadeWithTenantId() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .queryParam("skipCustomListeners", true)
      .queryParam("cascade", true)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_DELETE_URL);

    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .withTenantId(MockProvider.EXAMPLE_TENANT_ID)
      .skipCustomListeners()
      .cascade();

    verify(builder).delete();
  }

  @Test
  void testDeleteDefinitionsByKeyNoPermissions() {
    DeleteProcessDefinitionsBuilder builder = repositoryServiceMock.deleteProcessDefinitions()
      .byKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .withTenantId(MockProvider.EXAMPLE_TENANT_ID);

    doThrow(new AuthorizationException("No permission to delete process definitions")).when(builder).delete();

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body(containsString("No permission to delete process definitions"))
    .when()
      .delete(SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_DELETE_URL);
  }

  @Test
  void testGetStartFormDataForNonExistingProcessDefinition() {
    when(formServiceMock.getStartFormData(anyString())).thenThrow(new ProcessEngineException("expected exception"));

    given().pathParam("id", "aNonExistingProcessDefinitionId")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot get start form data for process definition"))
    .when().get(START_FORM_URL);
  }

  @Test
  void testUnparseableIntegerVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testUnparseableShortVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
    .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testUnparseableLongVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
    .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testUnparseableDoubleVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
    .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testUnparseableDateVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testNotSupportedTypeVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: Unsupported value type 'X'"))
    .when().post(START_PROCESS_INSTANCE_URL);
  }

  @Test
  void testActivateProcessDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);
  }

  @Test
  void testDelayedActivateProcessDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, executionDate);
  }

  @Test
  void testActivateProcessDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, null);
  }

  @Test
  void testDelayedActivateProcessDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", true);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, executionDate);
  }

  @Test
  void testActivateThrowsProcessEngineException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(repositoryServiceMock)
      .activateProcessDefinitionById(eq(MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID), eq(false), isNull());

    given()
      .pathParam("id", MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateNonParseableDateFormat() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);
    params.put("executionDate", "a");

    String expectedMessage = "Invalid format: \"a\"";
    String exceptionMessage = "The suspension state of Process Definition with id " + MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID + " could not be updated due to: " + expectedMessage;

    given()
      .pathParam("id", MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(exceptionMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessDefinitionThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);
  }

  @Test
  void testDelayedSuspendProcessDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, executionDate);
  }

  @Test
  void testSuspendProcessDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, null);
  }

  @Test
  void testDelayedSuspendProcessDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", true);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, executionDate);
  }

  @Test
  void testSuspendThrowsProcessEngineException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(repositoryServiceMock)
      .suspendProcessDefinitionById(eq(MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID), eq(false), isNull());

    given()
      .pathParam("id", MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendNonParseableDateFormat() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);
    params.put("executionDate", "a");

    String expectedMessage = "Invalid format: \"a\"";
    String exceptionMessage = "The suspension state of Process Definition with id " + MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID + " could not be updated due to: " + expectedMessage;

    given()
      .pathParam("id", MockProvider.NON_EXISTING_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(exceptionMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendWithMultipleByParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "Only one of processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessDefinitionByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, null);
  }

  @Test
  void testActivateProcessDefinitionByKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, true, null);
  }

  @Test
  void testDelayedActivateProcessDefinitionByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, executionDate);
  }

  @Test
  void testDelayedActivateProcessDefinitionByKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, true, executionDate);
  }

  @Test
  void testActivateProcessDefinitionByKeyWithUnparseableDate() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", "a");

    String message = "Could not update the suspension state of Process Definitions due to: Invalid format: \"a\"";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessDefinitionByKeyWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(repositoryServiceMock)
      .activateProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, null);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessDefinitionByKeyThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(repositoryServiceMock).activateProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, null);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, null);
  }

  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, true, null);
  }

  @Test
  void testDelayedSuspendProcessDefinitionByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, executionDate);
  }

  @Test
  void testDelayedSuspendProcessDefinitionByKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, true, executionDate);
  }

  @Test
  void testSuspendProcessDefinitionByKeyWithUnparseableDate() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", "a");

    String message = "Could not update the suspension state of Process Definitions due to: Invalid format: \"a\"";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionByKeyWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(repositoryServiceMock)
      .suspendProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, null);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionByKeyThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(repositoryServiceMock).suspendProcessDefinitionByKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY, false, null);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessDefinitionByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String message = "Only processDefinitionKey can be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false", "null"})
  void putProcessDefinitionSuspended_shouldReturnBadRequest_givenInvalidRequest(String suspendedValue) {
    Map<String, Object> params = new HashMap<>();
    // invalid because 'processDefinitionId' and 'processDefinitionKey' are both missing
    // only 'suspended' is provided, or not provided at all
    if (!"null".equals(suspendedValue)) {
      params.put("suspended", Boolean.parseBoolean(suspendedValue));
    }

    String message = "Either processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
            .contentType(ContentType.JSON)
            .body(params)
            .then()
            .expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body("type", is(InvalidRequestException.class.getSimpleName()))
            .body("message", is(message))
            .when()
            .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String message = "Only processDefinitionKey can be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(PROCESS_DEFINITION_SUSPENDED_URL);
  }

  /**
   *
   ********************************* test cases for operations of the latest process definition ********************************
   * get the latest process definition by key
   *
   */

  @Test
  void testInstanceResourceLinkResult_ByKey() {
    String fullInstanceUrl = "http://localhost:" + PORT + TEST_RESOURCE_ROOT_PATH + "/process-instance/" + MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("links[0].href", equalTo(fullInstanceUrl))
      .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testInstanceResourceLinkWithEnginePrefix_ByKey() {
    String startInstanceOnExplicitEngineUrl = TEST_RESOURCE_ROOT_PATH + "/engine/default/process-definition/key/{key}/start";

    String fullInstanceUrl = "http://localhost:" + PORT + TEST_RESOURCE_ROOT_PATH + "/engine/default/process-instance/" + MockProvider.EXAMPLE_PROCESS_INSTANCE_ID;

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("links[0].href", equalTo(fullInstanceUrl))
      .when().post(startInstanceOnExplicitEngineUrl);
  }

  @Test
  void testProcessDefinitionBpmn20XmlRetrieval_ByKey() {
    // Rest-assured has problems with extracting json with escaped quotation marks, i.e. the xml content in our case
    Response response = given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
//      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
//      .body("bpmn20Xml", startsWith("<?xml"))
    .when().get(XML_DEFINITION_BY_KEY_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains("<?xml")
      .contains(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testGetProcessDefinitionBpmn20XmlThrowsAuthorizationException_ByKey() {
    String message = "expected exception";
    when(repositoryServiceMock.getProcessModel(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(XML_DEFINITION_BY_KEY_URL);
  }

  @Test
  void testGetStartFormData_ByKey() {
    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("key", equalTo(MockProvider.EXAMPLE_FORM_KEY))
    .when().get(START_FORM_BY_KEY_URL);
  }

  @Test
  void testGetStartFormThrowsAuthorizationException_ByKey() {
    String message = "expected exception";
    when(formServiceMock.getStartFormData(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(START_FORM_BY_KEY_URL);
  }

  @Test
  void testGetStartForm_shouldReturnKeyContainingTaskId_ByKey() {
    ProcessDefinition mockDefinition = MockProvider.createMockDefinition();
    StartFormData mockStartFormData = MockProvider.createMockStartFormDataUsingFormFieldsWithoutFormKey(mockDefinition);
    when(formServiceMock.getStartFormData(mockDefinition.getId())).thenReturn(mockStartFormData);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("key", equalTo("embedded:engine://engine/:engine/process-definition/" + mockDefinition.getId() + "/rendered-form"))
      .body("contextPath", equalTo(MockProvider.EXAMPLE_PROCESS_APPLICATION_CONTEXT_PATH))
      .when().get(START_FORM_BY_KEY_URL);
  }

  @Test
  void testGetRenderedStartForm_ByKey() {
    String expectedResult = "<formField>anyContent</formField>";

    when(formServiceMock.getRenderedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(expectedResult);

    Response response = given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(XHTML_XML_CONTENT_TYPE)
      .when()
        .get(RENDERED_FORM_BY_KEY_URL);

    String responseContent = response.asString();
    assertThat(responseContent).isEqualTo(expectedResult);
  }

  @Test
  void testGetRenderedStartFormReturnsNotFound_ByKey() {
    when(formServiceMock.getRenderedStartForm(anyString(), anyString())).thenReturn(null);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .then()
        .expect()
          .statusCode(Status.NOT_FOUND.getStatusCode())
          .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
          .body("message", equalTo("No matching rendered start form for process definition with the id " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + " found."))
      .when()
        .get(RENDERED_FORM_BY_KEY_URL);
  }

  @Test
  void testGetRenderedStartFormThrowsAuthorizationException_ByKey() {
    String message = "expected exception";
    when(formServiceMock.getRenderedStartForm(anyString())).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(RENDERED_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitStartForm_ByKey() {
    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
      .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
      .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
    .when().post(SUBMIT_FORM_BY_KEY_URL);

    verify(formServiceMock).submitStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, null);
  }

  @Test
  void testSubmitStartFormWithParameters_ByKey() {
    Map<String, Object> variables = VariablesBuilder.create()
        .variable("aVariable", "aStringValue")
        .variable("anotherVariable", 42)
        .variable("aThirdValue", Boolean.TRUE).getVariables();

    Map<String, Object> json = new HashMap<>();
    json.put("variables", variables);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_BY_KEY_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aVariable", "aStringValue");
    expectedVariables.put("anotherVariable", 42);
    expectedVariables.put("aThirdValue", Boolean.TRUE);

    verify(formServiceMock).submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), argThat(new EqualsMap(expectedVariables)));
  }

  @Test
  void testSubmitStartFormWithBusinessKey_ByKey() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_BY_KEY_URL);

    verify(formServiceMock).submitStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, "myBusinessKey", null);
  }

  @Test
  void testSubmitStartFormWithBusinessKeyAndParameters_ByKey() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    Map<String, Object> variables = VariablesBuilder.create()
        .variable("aVariable", "aStringValue")
        .variable("anotherVariable", 42)
        .variable("aThirdValue", Boolean.TRUE).getVariables();

    json.put("variables", variables);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
        .body("definitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("businessKey", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
        .body("ended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
        .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .when().post(SUBMIT_FORM_BY_KEY_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aVariable", "aStringValue");
    expectedVariables.put("anotherVariable", 42);
    expectedVariables.put("aThirdValue", Boolean.TRUE);

    verify(formServiceMock).submitStartForm(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), eq("myBusinessKey"), argThat(new EqualsMap(expectedVariables)));
  }

  @Test
  void testSubmitStartFormWithUnparseableIntegerVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableShortVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
    .when().post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableLongVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
    .when().post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableDoubleVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
    .when().post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitStartFormWithUnparseableDateVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitStartFormWithNotSupportedVariableType_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: Unsupported value type 'X'"))
    .when().post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testUnsuccessfulSubmitStartForm_ByKey() {
    doThrow(new ProcessEngineException("expected exception")).when(formServiceMock).submitStartForm(any(String.class), Mockito.any());

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", equalTo("Cannot instantiate process definition " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + ": expected exception"))
      .when().post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitFormByKeyThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(formServiceMock).submitStartForm(any(String.class), Mockito.any());

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSubmitFormByKeyThrowsFormFieldValidationException() {
    String message = "expected exception";
    doThrow(new FormFieldValidationException("form-exception", message)).when(formServiceMock).submitStartForm(any(String.class), Mockito.any());

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", equalTo("Cannot instantiate process definition " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + ": " + message))
    .when()
      .post(SUBMIT_FORM_BY_KEY_URL);
  }

  @Test
  void testSimpleProcessInstantiation_ByKey() {
    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testProcessInstantiationWithParameters_ByKey() {
    Map<String, Object> parameters = VariablesBuilder.create()
        .variable("aBoolean", Boolean.TRUE)
        .variable("aString", "aStringVariableValue")
        .variable("anInteger", 42).getVariables();

    Map<String, Object> json = new HashMap<>();
    json.put("variables", parameters);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);

    Map<String, Object> expectedParameters = new HashMap<>();
    expectedParameters.put("aBoolean", Boolean.TRUE);
    expectedParameters.put("aString", "aStringVariableValue");
    expectedParameters.put("anInteger", 42);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).setVariables(argThat(new EqualsMap(expectedParameters)));
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationWithBusinessKey_ByKey() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).businessKey("myBusinessKey");
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationWithBusinessKeyAndParameters_ByKey() {
    Map<String, Object> json = new HashMap<>();
    json.put("businessKey", "myBusinessKey");

    Map<String, Object> parameters = VariablesBuilder.create()
        .variable("aBoolean", Boolean.TRUE)
        .variable("aString", "aStringVariableValue")
        .variable("anInteger", 42).getVariables();

    json.put("variables", parameters);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);

    Map<String, Object> expectedParameters = new HashMap<>();
    expectedParameters.put("aBoolean", Boolean.TRUE);
    expectedParameters.put("aString", "aStringVariableValue");
    expectedParameters.put("anInteger", 42);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).businessKey("myBusinessKey");
    verify(mockInstantiationBuilder).setVariables(argThat(new EqualsMap(expectedParameters)));
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  /**
   * {@link RuntimeService#startProcessInstanceById(String, Map)} throws an {@link ProcessEngineException}, if a definition with the given id does not exist.
   */
  @Test
  void testUnsuccessfulInstantiation_ByKey() {
    when(mockInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean()))
      .thenThrow(new ProcessEngineException("expected exception"));

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", containsString("Cannot instantiate process definition"))
      .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testStartProcessInstanceByKeyThrowsAuthorizationException() {
    String message = "expected exception";
    when(mockInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean()))
      .thenThrow(new AuthorizationException(message));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testDefinitionRetrieval_ByKey() {
    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .body("key", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY))
      .body("category", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_CATEGORY))
      .body("name", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_NAME))
      .body("description", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DESCRIPTION))
      .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
      .body("version", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_VERSION))
      .body("resource", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_RESOURCE_NAME))
      .body("diagram", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DIAGRAM_RESOURCE_NAME))
      .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_IS_SUSPENDED))
      .body("tenantId", nullValue())
    .when().get(SINGLE_PROCESS_DEFINITION_BY_KEY_URL);

    verify(processDefinitionQueryMock).withoutTenantId();
    verify(repositoryServiceMock).getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testNonExistingProcessDefinitionRetrieval_ByKey() {
    String nonExistingKey = "aNonExistingDefinitionKey";

    when(repositoryServiceMock.createProcessDefinitionQuery().processDefinitionKey(nonExistingKey)).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.latestVersion()).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.singleResult()).thenReturn(null);
    when(processDefinitionQueryMock.list()).thenReturn(Collections.emptyList());
    when(processDefinitionQueryMock.count()).thenReturn(0L);

    given().pathParam("key", nonExistingKey)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", is(RestException.class.getSimpleName()))
      .body("message", containsString("No matching process definition with key: " + nonExistingKey + " and no tenant-id"))
    .when().get(SINGLE_PROCESS_DEFINITION_BY_KEY_URL);
  }

  @Test
  void testDefinitionRetrieval_ByKeyAndTenantId() {
    ProcessDefinition mockDefinition = MockProvider.mockDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build();
    setUpRuntimeDataForDefinition(mockDefinition);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .body("key", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY))
      .body("category", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_CATEGORY))
      .body("name", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_NAME))
      .body("description", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DESCRIPTION))
      .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
      .body("version", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_VERSION))
      .body("resource", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_RESOURCE_NAME))
      .body("diagram", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DIAGRAM_RESOURCE_NAME))
      .body("suspended", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_IS_SUSPENDED))
      .body("tenantId", equalTo(MockProvider.EXAMPLE_TENANT_ID))
    .when().get(SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_URL);

    verify(processDefinitionQueryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
    verify(repositoryServiceMock).getProcessDefinition(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testNonExistingProcessDefinitionRetrieval_ByKeyAndTenantId() {
    String nonExistingKey = "aNonExistingDefinitionKey";
    String nonExistingTenantId = "aNonExistingTenantId";

    when(repositoryServiceMock.createProcessDefinitionQuery().processDefinitionKey(nonExistingKey)).thenReturn(processDefinitionQueryMock);
    when(processDefinitionQueryMock.singleResult()).thenReturn(null);

    given()
      .pathParam("key", nonExistingKey)
      .pathParam("tenant-id", nonExistingTenantId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", is(RestException.class.getSimpleName()))
      .body("message", containsString("No matching process definition with key: " + nonExistingKey + " and tenant-id: " + nonExistingTenantId))
    .when().get(SINGLE_PROCESS_DEFINITION_BY_KEY_AND_TENANT_ID_URL);
  }

  @Test
  void testSimpleProcessInstantiation_ByKeyAndTenantId() {
    ProcessDefinition mockDefinition = MockProvider.mockDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build();
    setUpRuntimeDataForDefinition(mockDefinition);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
    .when().post(START_PROCESS_INSTANCE_BY_KEY_AND_TENANT_ID_URL);

    verify(processDefinitionQueryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
  }

  @Test
  void testUnparseableIntegerVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
    .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testUnparseableShortVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
    .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testUnparseableLongVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
    .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testUnparseableDoubleVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
    .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testUnparseableDateVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: "
        + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
    .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testNotSupportedTypeVariable_ByKey() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> variableJson = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();

    Map<String, Object> variables = new HashMap<>();
    variables.put("variables", variableJson);

    given().pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .contentType(POST_JSON_CONTENT_TYPE).body(variables)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot instantiate process definition aProcDefId: Unsupported value type 'X'"))
    .when().post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }


  @Test
  void testUpdateHistoryTimeToLive() {
    given()
        .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto(5))
        .contentType(ContentType.JSON)
        .then().expect()
          .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
          .put(SINGLE_PROCESS_DEFINITION_HISTORY_TIMETOLIVE_URL);

    verify(repositoryServiceMock).updateProcessDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, 5);
  }

  @Test
  void testUpdateHistoryTimeToLiveNullValue() {
    given()
        .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto())
        .contentType(ContentType.JSON)
        .then().expect()
          .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
          .put(SINGLE_PROCESS_DEFINITION_HISTORY_TIMETOLIVE_URL);

    verify(repositoryServiceMock).updateProcessDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, null);
  }

  @Test
  void testUpdateHistoryTimeToLiveNegativeValue() {
    String expectedMessage = "expectedMessage";

    doThrow(new BadUserRequestException(expectedMessage))
        .when(repositoryServiceMock)
        .updateProcessDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, -1);

    given()
        .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto(-1))
        .contentType(ContentType.JSON)
        .then().expect()
          .statusCode(Status.BAD_REQUEST.getStatusCode())
          .body("type", is(BadUserRequestException.class.getSimpleName()))
          .body("message", containsString(expectedMessage))
        .when()
          .put(SINGLE_PROCESS_DEFINITION_HISTORY_TIMETOLIVE_URL);

    verify(repositoryServiceMock).updateProcessDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, -1);
  }

  @Test
  void testUpdateHistoryTimeToLiveAuthorizationException() {
    String expectedMessage = "expectedMessage";

    doThrow(new AuthorizationException(expectedMessage))
        .when(repositoryServiceMock)
        .updateProcessDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, 5);

    given()
        .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto(5))
        .contentType(ContentType.JSON)
        .then().expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", containsString(expectedMessage))
        .when()
        .put(SINGLE_PROCESS_DEFINITION_HISTORY_TIMETOLIVE_URL);

    verify(repositoryServiceMock).updateProcessDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, 5);
  }

  @Test
  void testActivateProcessDefinitionExcludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);
  }

  @Test
  void testDelayedActivateProcessDefinitionExcludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, executionDate);
  }

  @Test
  void testActivateProcessDefinitionIncludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", true);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, null);
  }

  @Test
  void testDelayedActivateProcessDefinitionIncludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", true);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, executionDate);
  }

  @Test
  void testActivateThrowsProcessEngineException_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(repositoryServiceMock)
      .activateProcessDefinitionById(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), eq(false), isNull());

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", containsString(expectedMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);
  }

  @Test
  void testActivateNonParseableDateFormat_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeProcessInstances", false);
    params.put("executionDate", "a");

    String expectedMessage = "Invalid format: \"a\"";
    String exceptionMessage = "The suspension state of Process Definition with id " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + " could not be updated due to: " + expectedMessage;

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(exceptionMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessDefinitionThrowsAuthorizationException_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(repositoryServiceMock).activateProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionExcludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);
  }

  @Test
  void testDelayedSuspendProcessDefinitionExcludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, executionDate);
  }

  @Test
  void testSuspendProcessDefinitionIncludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", true);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, null);
  }


  @Test
  void testDelayedSuspendProcessDefinitionIncludingInstances_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", true);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);

    verify(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, true, executionDate);
  }

  @Test
  void testSuspendThrowsProcessEngineException_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(repositoryServiceMock)
      .suspendProcessDefinitionById(eq(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID), eq(false), isNull());

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", containsString(expectedMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);
  }

  @Test
  void testSuspendNonParseableDateFormat_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeProcessInstances", false);
    params.put("executionDate", "a");

    String expectedMessage = "Invalid format: \"a\"";
    String exceptionMessage = "The suspension state of Process Definition with id " + MockProvider.EXAMPLE_PROCESS_DEFINITION_ID + " could not be updated due to: " + expectedMessage;

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(exceptionMessage))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessDefinitionThrowsAuthorizationException_ByKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(repositoryServiceMock).suspendProcessDefinitionById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, false, null);

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(SINGLE_PROCESS_DEFINITION_BY_KEY_SUSPENDED_URL);
  }

  @Test
  void testProcessInstantiationWithCaseInstanceId() {
    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceId", "myCaseInstanceId");

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).caseInstanceId("myCaseInstanceId");
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationWithCaseInstanceIdAndBusinessKey() {
    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceId", "myCaseInstanceId");
    json.put("businessKey", "myBusinessKey");

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).businessKey("myBusinessKey");
    verify(mockInstantiationBuilder).caseInstanceId("myCaseInstanceId");
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testProcessInstantiationWithCaseInstanceIdAndBusinessKeyAndParameters() {
    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceId", "myCaseInstanceId");
    json.put("businessKey", "myBusinessKey");

    Map<String, Object> parameters = VariablesBuilder.create()
        .variable("aBoolean", Boolean.TRUE)
        .variable("aString", "aStringVariableValue")
        .variable("anInteger", 42).getVariables();

    json.put("variables", parameters);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .when().post(START_PROCESS_INSTANCE_URL);

    Map<String, Object> expectedParameters = new HashMap<>();
    expectedParameters.put("aBoolean", Boolean.TRUE);
    expectedParameters.put("aString", "aStringVariableValue");
    expectedParameters.put("anInteger", 42);

    verify(runtimeServiceMock).createProcessInstanceById(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockInstantiationBuilder).businessKey("myBusinessKey");
    verify(mockInstantiationBuilder).caseInstanceId("myCaseInstanceId");
    verify(mockInstantiationBuilder).setVariables(argThat(new EqualsMap(expectedParameters)));
    verify(mockInstantiationBuilder).executeWithVariablesInReturn(anyBoolean(), anyBoolean());
  }

  @Test
  void testGetStartFormVariablesThrowsAuthorizationException_ByKey() {
    String message = "expected exception";
    when(formServiceMock.getStartFormVariables(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, null, true)).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(START_FORM_VARIABLES_BY_KEY_URL);
  }

  @Test
  void testGetDeployedStartForm_ByKey() {
    InputStream deployedStartFormMock = new ByteArrayInputStream("Test".getBytes());
    when(formServiceMock.getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenReturn(deployedStartFormMock);

    given()
    .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
    .then().expect()
    .statusCode(Status.OK.getStatusCode())
    .body(equalTo("Test"))
    .when()
    .get(DEPLOYED_START_FORM_BY_KEY_URL);

    verify(formServiceMock).getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testGetDeployedStartForm() {
    InputStream deployedStartFormMock = new ByteArrayInputStream("Test".getBytes());
    when(formServiceMock.getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenReturn(deployedStartFormMock);

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
    .statusCode(Status.OK.getStatusCode())
    .body(equalTo("Test"))
    .contentType(MediaType.APPLICATION_XHTML_XML)
    .when()
    .get(DEPLOYED_START_FORM_URL);

    verify(formServiceMock).getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testGetDeployedStartFormJson() {
    InputStream deployedStartFormMock = new ByteArrayInputStream("Test".getBytes());
    when(formServiceMock.getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenReturn(deployedStartFormMock);
    when(formServiceMock.getStartFormKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenReturn("test.form");

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
    .statusCode(Status.OK.getStatusCode())
    .body(equalTo("Test"))
    .contentType(MediaType.APPLICATION_JSON)
    .when()
    .get(DEPLOYED_START_FORM_URL);

    verify(formServiceMock).getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testGetDeployedStartFormWithoutAuthorization() {
    String message = "unauthorized";
    when(formServiceMock.getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenThrow(new AuthorizationException(message));

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
    .statusCode(Status.FORBIDDEN.getStatusCode())
    .body("message", equalTo(message))
    .when()
    .get(DEPLOYED_START_FORM_URL);
  }

  @Test
  void testGetDeployedStartFormWithWrongFormKeyFormat() {
    String message = "wrong key format";
    when(formServiceMock.getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenThrow(new BadUserRequestException(message));

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
    .statusCode(Status.BAD_REQUEST.getStatusCode())
    .body("message", equalTo(message))
    .when()
    .get(DEPLOYED_START_FORM_URL);
  }

  @Test
  void testGetDeployedStartFormWithUnexistingForm() {
    String message = "not found";
    when(formServiceMock.getDeployedStartForm(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .thenThrow(new NotFoundException(message));

    given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
    .statusCode(Status.NOT_FOUND.getStatusCode())
    .body("message", equalTo(message))
    .when()
    .get(DEPLOYED_START_FORM_URL);
  }

  @Test
  void testGetStaticCalledProcessDefinitions() {
    CalledProcessDefinition mock = mock(CalledProcessDefinitionImpl.class);
    when(mock.getCalledFromActivityIds()).thenReturn(Arrays.asList("anActivity", "anotherActivity"));
    when(mock.getId()).thenReturn("aKey:1:123");
    when(mock.getCallingProcessDefinitionId()).thenReturn("aCallingId");
    when(mock.getName()).thenReturn("a Name");
    when(mock.getKey()).thenReturn("aKey");
    when(mock.getVersion()).thenReturn(1);
    List<CalledProcessDefinition> result = Collections.singletonList(mock);
    when(repositoryServiceMock.getStaticCalledProcessDefinitions(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .thenReturn(result);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode()).contentType(ContentType.JSON)
      .body("[0].callingProcessDefinitionId", equalTo(mock.getCallingProcessDefinitionId()))
      .body("[0].id", equalTo(mock.getId()))
      .body("[0].key", equalTo(mock.getKey()))
      .body("[0].name", equalTo(mock.getName()))
      .body("[0].version", equalTo(mock.getVersion()))
      .body("[0].calledFromActivityIds[0]", equalTo("anActivity"))
      .body("[0].calledFromActivityIds[1]", equalTo("anotherActivity"))
    .when()
      .get(PROCESS_DEFINITION_CALL_ACTIVITY_MAPPINGS);
  }

  @Test
  void testGetStaticCalledProcessDefinitionNonExistingProcess() {

    when(repositoryServiceMock.getStaticCalledProcessDefinitions("NonExistingId")).thenThrow(
      new NotFoundException());

    given()
      .pathParam("id", "NonExistingId")
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
    .when()
      .get(PROCESS_DEFINITION_CALL_ACTIVITY_MAPPINGS);
  }

  @Test
  void shouldReturnErrorCodeWhenStartingProcessInstance() {
    when(mockInstantiationBuilder.executeWithVariablesInReturn(anyBoolean(), anyBoolean()))
      .thenThrow(new ProcessEngineException("foo", 123));

    given()
      .pathParam("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", equalTo("Cannot instantiate process definition aProcDefId: foo"))
      .body("code", equalTo(123))
    .when()
      .post(START_PROCESS_INSTANCE_BY_KEY_URL);
  }

  @Test
  void shouldReturnErrorCodeWhenSubmittingForm() {
    doThrow(new ProcessEngineException("foo", 123))
        .when(formServiceMock).submitStartForm(any(String.class), Mockito.any());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", equalTo("Cannot instantiate process definition aProcDefId: foo"))
      .body("code", equalTo(123))
    .when()
      .post(SUBMIT_FORM_URL);
  }

}
