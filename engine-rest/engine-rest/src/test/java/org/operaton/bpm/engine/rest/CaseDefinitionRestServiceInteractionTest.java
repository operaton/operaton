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


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.CaseDefinitionQuery;
import org.operaton.bpm.engine.rest.dto.HistoryTimeToLiveDto;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.EqualsVariableMap;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.variable.EqualsPrimitiveValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsUntypedValue;
import org.operaton.bpm.engine.rest.sub.repository.impl.ProcessDefinitionResourceImpl;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.CaseInstanceBuilder;
import org.operaton.bpm.engine.variable.type.ValueType;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * @author Roman Smirnov
 *
 */
public class CaseDefinitionRestServiceInteractionTest extends AbstractRestServiceTest {

  protected static final String CASE_DEFINITION_URL = TEST_RESOURCE_ROOT_PATH + "/case-definition";
  protected static final String SINGLE_CASE_DEFINITION_URL = CASE_DEFINITION_URL + "/{id}";
  protected static final String SINGLE_CASE_DEFINITION_BY_KEY_URL = CASE_DEFINITION_URL + "/key/{key}";
  protected static final String SINGLE_CASE_DEFINITION_BY_KEY_AND_TENANT_ID_URL = CASE_DEFINITION_URL + "/key/{key}/tenant-id/{tenant-id}";

  protected static final String XML_DEFINITION_URL = SINGLE_CASE_DEFINITION_URL + "/xml";
  protected static final String XML_DEFINITION_BY_KEY_URL = SINGLE_CASE_DEFINITION_BY_KEY_URL + "/xml";

  protected static final String CREATE_INSTANCE_URL = SINGLE_CASE_DEFINITION_URL + "/create";
  protected static final String CREATE_INSTANCE_BY_KEY_URL = SINGLE_CASE_DEFINITION_BY_KEY_URL + "/create";
  protected static final String CREATE_INSTANCE_BY_KEY_AND_TENANT_ID_URL = SINGLE_CASE_DEFINITION_BY_KEY_AND_TENANT_ID_URL + "/create";

  protected static final String DIAGRAM_DEFINITION_URL = SINGLE_CASE_DEFINITION_URL + "/diagram";

  protected static final String UPDATE_HISTORY_TIME_TO_LIVE_URL = SINGLE_CASE_DEFINITION_URL + "/history-time-to-live";

  @RegisterExtension
  public static final TestContainerExtension rule = new TestContainerExtension();

  private RepositoryService repositoryServiceMock;
  private CaseService caseServiceMock;
  private CaseDefinitionQuery caseDefinitionQueryMock;
  private CaseInstanceBuilder caseInstanceBuilder;

  @BeforeEach
  void setUpRuntime() {
    CaseDefinition mockCaseDefinition = MockProvider.createMockCaseDefinition();

    setUpRuntimeData(mockCaseDefinition);

    caseServiceMock = mock(CaseService.class);

    when(processEngine.getCaseService()).thenReturn(caseServiceMock);

    caseInstanceBuilder = mock(CaseInstanceBuilder.class);
    CaseInstance mockCaseInstance = MockProvider.createMockCaseInstance();

    when(caseServiceMock.withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID)).thenReturn(caseInstanceBuilder);
    when(caseInstanceBuilder.businessKey(any())).thenReturn(caseInstanceBuilder);
    when(caseInstanceBuilder.setVariables(any())).thenReturn(caseInstanceBuilder);
    when(caseInstanceBuilder.create()).thenReturn(mockCaseInstance);
  }

  private void setUpRuntimeData(CaseDefinition mockCaseDefinition) {
    repositoryServiceMock = mock(RepositoryService.class);

    when(processEngine.getRepositoryService()).thenReturn(repositoryServiceMock);
    when(repositoryServiceMock.getCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID)).thenReturn(mockCaseDefinition);
    when(repositoryServiceMock.getCaseModel(MockProvider.EXAMPLE_CASE_DEFINITION_ID)).thenReturn(createMockCaseDefinitionCmmnXml());

    caseDefinitionQueryMock = mock(CaseDefinitionQuery.class);
    when(caseDefinitionQueryMock.caseDefinitionKey(MockProvider.EXAMPLE_CASE_DEFINITION_KEY)).thenReturn(caseDefinitionQueryMock);
    when(caseDefinitionQueryMock.tenantIdIn(any())).thenReturn(caseDefinitionQueryMock);
    when(caseDefinitionQueryMock.withoutTenantId()).thenReturn(caseDefinitionQueryMock);
    when(caseDefinitionQueryMock.latestVersion()).thenReturn(caseDefinitionQueryMock);
    when(caseDefinitionQueryMock.singleResult()).thenReturn(mockCaseDefinition);
    when(caseDefinitionQueryMock.list()).thenReturn(Collections.singletonList(mockCaseDefinition));
    when(repositoryServiceMock.createCaseDefinitionQuery()).thenReturn(caseDefinitionQueryMock);
  }

  private InputStream createMockCaseDefinitionCmmnXml() {
    // do not close the input stream, will be done in implementation
    InputStream cmmnXmlInputStream = ReflectUtil.getResourceAsStream("cases/case-model.cmmn");
    assertThat(cmmnXmlInputStream).isNotNull();
    return cmmnXmlInputStream;
  }

  @Test
  void testCaseDefinitionCmmnXmlRetrieval() {
    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(XML_DEFINITION_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains("<?xml")
      .contains(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
  }

  @Test
  void testDefinitionRetrieval() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_RESOURCE_NAME))
        .body("tenantId", equalTo(null))
    .when()
      .get(SINGLE_CASE_DEFINITION_URL);

    verify(repositoryServiceMock).getCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
  }

  @Test
  void testCaseDefinitionCmmnXmlRetrieval_ByKey() {
    Response response = given()
        .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(XML_DEFINITION_BY_KEY_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains(MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .contains("<?xml");
  }

  @Test
  void testDefinitionRetrieval_ByKey() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_RESOURCE_NAME))
        .body("tenantId", equalTo(null))
    .when()
      .get(SINGLE_CASE_DEFINITION_BY_KEY_URL);

    verify(caseDefinitionQueryMock).withoutTenantId();
    verify(repositoryServiceMock).getCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
  }

  @Test
  void testNonExistingCaseDefinitionRetrieval_ByKey() {
    String nonExistingKey = "aNonExistingDefinitionKey";

    when(repositoryServiceMock.createCaseDefinitionQuery().caseDefinitionKey(nonExistingKey)).thenReturn(caseDefinitionQueryMock);
    when(caseDefinitionQueryMock.latestVersion()).thenReturn(caseDefinitionQueryMock);
    when(caseDefinitionQueryMock.singleResult()).thenReturn(null);
    when(caseDefinitionQueryMock.list()).thenReturn(Collections.emptyList());

    given()
      .pathParam("key", nonExistingKey)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", containsString("No matching case definition with key: " + nonExistingKey))
    .when()
      .get(SINGLE_CASE_DEFINITION_BY_KEY_URL);
  }

  @Test
  void testDefinitionRetrieval_ByKeyAndTenantId() {
    CaseDefinition mockDefinition = MockProvider.mockCaseDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build();
    setUpRuntimeData(mockDefinition);

    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_CASE_DEFINITION_RESOURCE_NAME))
        .body("tenantId", equalTo(MockProvider.EXAMPLE_TENANT_ID))
    .when()
      .get(SINGLE_CASE_DEFINITION_BY_KEY_AND_TENANT_ID_URL);

    verify(caseDefinitionQueryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
    verify(repositoryServiceMock).getCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
  }

  @Test
  void testNonExistingCaseDefinitionRetrieval_ByKeyAndTenantId() {
    String nonExistingKey = "aNonExistingDefinitionKey";
    String nonExistingTenantId = "aNonExistingTenantId";

    when(repositoryServiceMock.createCaseDefinitionQuery().caseDefinitionKey(nonExistingKey)).thenReturn(caseDefinitionQueryMock);
    when(caseDefinitionQueryMock.singleResult()).thenReturn(null);

    given()
      .pathParam("key", nonExistingKey)
      .pathParam("tenant-id", nonExistingTenantId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", is(RestException.class.getSimpleName()))
      .body("message", containsString("No matching case definition with key: %s and tenant-id: %s".formatted(nonExistingKey, nonExistingTenantId)))
    .when().get(SINGLE_CASE_DEFINITION_BY_KEY_AND_TENANT_ID_URL);
  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionKeyAndTenantId() {
    CaseDefinition mockDefinition = MockProvider.mockCaseDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build();
    setUpRuntimeData(mockDefinition);

    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_BY_KEY_AND_TENANT_ID_URL);

    verify(caseDefinitionQueryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).create();
  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionId() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_URL);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey(null);
    verify(caseInstanceBuilder).setVariables(emptyMap());
    verify(caseInstanceBuilder).create();

  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionKey() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_BY_KEY_URL);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey(null);
    verify(caseInstanceBuilder).setVariables(emptyMap());
    verify(caseInstanceBuilder).create();
  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionIdWithBusinessKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("businessKey", MockProvider.EXAMPLE_CASE_INSTANCE_BUSINESS_KEY);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_URL);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey(MockProvider.EXAMPLE_CASE_INSTANCE_BUSINESS_KEY);
    verify(caseInstanceBuilder).setVariables(emptyMap());
    verify(caseInstanceBuilder).create();

  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionKeyWithBusinessKey() {
    Map<String, Object> params = new HashMap<>();
    params.put("businessKey", MockProvider.EXAMPLE_CASE_INSTANCE_BUSINESS_KEY);

    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_BY_KEY_URL);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey(MockProvider.EXAMPLE_CASE_INSTANCE_BUSINESS_KEY);
    verify(caseInstanceBuilder).setVariables(emptyMap());
    verify(caseInstanceBuilder).create();
  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionIdWithVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", VariablesBuilder.getVariableValueMap("abc", ValueType.STRING.getName()));
    variables.put("anotherVariableName", VariablesBuilder.getVariableValueMap(900, ValueType.INTEGER.getName()));

    Map<String, Object> params = new HashMap<>();
    params.put("variables", variables);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_URL);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey(null);
    verify(caseInstanceBuilder).setVariables(argThat(EqualsVariableMap.matches()
        .matcher("aVariableName", EqualsPrimitiveValue.stringValue("abc"))
        .matcher("anotherVariableName", EqualsPrimitiveValue.integerValue(900))));
    verify(caseInstanceBuilder).create();

  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionKeyWithVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", VariablesBuilder.getVariableValueMap("abc", null));
    variables.put("anotherVariableName", VariablesBuilder.getVariableValueMap(900, null));

    Map<String, Object> params = new HashMap<>();
    params.put("variables", variables);

    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_BY_KEY_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aVariableName", "abc");
    expectedVariables.put("anotherVariableName", 999);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey(null);
    verify(caseInstanceBuilder).setVariables(argThat(EqualsVariableMap.matches()
        .matcher("aVariableName", EqualsUntypedValue.matcher().value("abc"))
        .matcher("anotherVariableName", EqualsUntypedValue.matcher().value(900))));
    verify(caseInstanceBuilder).create();
  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionIdWithBusinessKeyAndVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", VariablesBuilder.getVariableValueMap("abc", null));
    variables.put("anotherVariableName", VariablesBuilder.getVariableValueMap(900, null));

    Map<String, Object> params = new HashMap<>();
    params.put("variables", variables);
    params.put("businessKey", "aBusinessKey");

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aVariableName", "abc");
    expectedVariables.put("anotherVariableName", 999);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey("aBusinessKey");
    verify(caseInstanceBuilder).setVariables(argThat(EqualsVariableMap.matches()
        .matcher("aVariableName", EqualsUntypedValue.matcher().value("abc"))
        .matcher("anotherVariableName", EqualsUntypedValue.matcher().value(900))));
    verify(caseInstanceBuilder).create();

  }

  @Test
  void testCreateCaseInstanceByCaseDefinitionKeyWithBusinessKeyAndVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", VariablesBuilder.getVariableValueMap("abc", null));
    variables.put("anotherVariableName", VariablesBuilder.getVariableValueMap(900, null));

    Map<String, Object> params = new HashMap<>();
    params.put("variables", variables);
    params.put("businessKey", "aBusinessKey");

    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID))
    .when()
      .post(CREATE_INSTANCE_BY_KEY_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("aVariableName", "abc");
    expectedVariables.put("anotherVariableName", 999);

    verify(caseServiceMock).withCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(caseInstanceBuilder).businessKey("aBusinessKey");
    verify(caseInstanceBuilder).setVariables(argThat(EqualsVariableMap.matches()
        .matcher("aVariableName", EqualsUntypedValue.matcher().value("abc"))
        .matcher("anotherVariableName", EqualsUntypedValue.matcher().value(900))));
    verify(caseInstanceBuilder).create();
  }

  @Test
  void testCreateCaseInstanceByInvalidCaseDefinitionId() {
    when(caseInstanceBuilder.create())
      .thenThrow(new ProcessEngineException("expected exception"));

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", containsString("Cannot instantiate case definition aCaseDefinitionId: expected exception"))
    .when()
      .post(CREATE_INSTANCE_URL);
  }

  @Test
  void testCreateCaseInstanceByInvalidCaseDefinitionKey() {
    when(caseInstanceBuilder.create())
      .thenThrow(new ProcessEngineException("expected exception"));

    given()
      .pathParam("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(EMPTY_JSON_OBJECT)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", containsString("Cannot instantiate case definition aCaseDefinitionId: expected exception"))
    .when()
      .post(CREATE_INSTANCE_BY_KEY_URL);
  }

  @Test
  void testCaseDiagramRetrieval() throws Exception {
    // setup additional mock behavior
    File file = getFile("/processes/todo-process.png");
    when(repositoryServiceMock.getCaseDiagram(MockProvider.EXAMPLE_CASE_DEFINITION_ID))
        .thenReturn(new FileInputStream(file));

    // call method
    byte[] actual = given().pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType("image/png")
          .header("Content-Disposition", "attachment; " +
                  "filename=\"" + MockProvider.EXAMPLE_CASE_DEFINITION_DIAGRAM_RESOURCE_NAME + "\"; " +
                  "filename*=UTF-8''" + MockProvider.EXAMPLE_CASE_DEFINITION_DIAGRAM_RESOURCE_NAME)
        .when().get(DIAGRAM_DEFINITION_URL).getBody().asByteArray();

    // verify service interaction
    verify(repositoryServiceMock).getCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(repositoryServiceMock).getCaseDiagram(MockProvider.EXAMPLE_CASE_DEFINITION_ID);

    // compare input stream with response body bytes
    byte[] expected = IoUtil.readInputStream(new FileInputStream(file), "case diagram");
    assertThat(actual).containsExactly(expected);
  }

  @Test
  void testCaseDiagramNullFilename() throws Exception {
    // setup additional mock behavior
    File file = getFile("/processes/todo-process.png");
    when(repositoryServiceMock.getCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID).getDiagramResourceName())
      .thenReturn(null);
    when(repositoryServiceMock.getCaseDiagram(MockProvider.EXAMPLE_CASE_DEFINITION_ID))
        .thenReturn(new FileInputStream(file));

    // call method
    byte[] actual = given().pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType("application/octet-stream")
      .header("Content-Disposition", "attachment; " +
              "filename=\"" + null + "\"; "+
              "filename*=UTF-8''" + null)
      .when().get(DIAGRAM_DEFINITION_URL).getBody().asByteArray();

    // verify service interaction
    verify(repositoryServiceMock).getCaseDiagram(MockProvider.EXAMPLE_CASE_DEFINITION_ID);

    // compare input stream with response body bytes
    byte[] expected = IoUtil.readInputStream(new FileInputStream(file), "case diagram");
    assertThat(actual).containsExactly(expected);
  }

  @Test
  void testCaseDiagramNotExist() {
    // setup additional mock behavior
    when(repositoryServiceMock.getCaseDiagram(MockProvider.EXAMPLE_CASE_DEFINITION_ID)).thenReturn(null);

    // call method
    given().pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
        .expect().statusCode(Status.NO_CONTENT.getStatusCode())
        .when().get(DIAGRAM_DEFINITION_URL);

    // verify service interaction
    verify(repositoryServiceMock).getCaseDefinition(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    verify(repositoryServiceMock).getCaseDiagram(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
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
  void testUpdateHistoryTimeToLive() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .body(new HistoryTimeToLiveDto(5))
      .contentType(ContentType.JSON)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateCaseDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_CASE_DEFINITION_ID, 5);
  }

  @Test
  void testUpdateHistoryTimeToLiveNullValue() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .body(new HistoryTimeToLiveDto())
      .contentType(ContentType.JSON)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateCaseDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_CASE_DEFINITION_ID, null);
  }

  @Test
  void testUpdateHistoryTimeToLiveNegativeValue() {
    String expectedMessage = "expectedMessage";

    doThrow(new BadUserRequestException(expectedMessage))
        .when(repositoryServiceMock)
        .updateCaseDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_CASE_DEFINITION_ID, -1);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .body(new HistoryTimeToLiveDto(-1))
      .contentType(ContentType.JSON)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(BadUserRequestException.class.getSimpleName()))
        .body("message", containsString(expectedMessage))
    .when()
      .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateCaseDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_CASE_DEFINITION_ID, -1);
  }

  @Test
  void testUpdateHistoryTimeToLiveAuthorizationException() {
    String expectedMessage = "expectedMessage";

    doThrow(new AuthorizationException(expectedMessage))
        .when(repositoryServiceMock)
        .updateCaseDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_CASE_DEFINITION_ID, 5);

    given()
      .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
      .body(new HistoryTimeToLiveDto(5))
      .contentType(ContentType.JSON)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", containsString(expectedMessage))
    .when()
      .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateCaseDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_CASE_DEFINITION_ID, 5);
  }
}
