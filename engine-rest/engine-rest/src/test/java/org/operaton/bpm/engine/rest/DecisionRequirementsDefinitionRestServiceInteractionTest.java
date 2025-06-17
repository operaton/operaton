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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinitionQuery;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

public class DecisionRequirementsDefinitionRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String DECISION_REQUIREMENTS_DEFINITION_URL = TEST_RESOURCE_ROOT_PATH + "/decision-requirements-definition";
  protected static final String SINGLE_DECISION_REQUIREMENTS_DEFINITION_ID_URL = DECISION_REQUIREMENTS_DEFINITION_URL + "/{id}";
  protected static final String SINGLE_DECISION_REQUIREMENTS_DEFINITION_KEY_URL = DECISION_REQUIREMENTS_DEFINITION_URL + "/key/{key}";
  protected static final String SINGLE_DECISION_REQUIREMENTS_DEFINITION_KEY_AND_TENANT_ID_URL = DECISION_REQUIREMENTS_DEFINITION_URL + "/key/{key}/tenant-id/{tenant-id}";

  protected static final String XML_DEFINITION_URL = SINGLE_DECISION_REQUIREMENTS_DEFINITION_ID_URL + "/xml";

  protected static final String DIAGRAM_DEFINITION_URL = SINGLE_DECISION_REQUIREMENTS_DEFINITION_ID_URL + "/diagram";

  protected RepositoryService repositoryServiceMock;
  protected DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionQueryMock;
  protected DecisionService decisionServiceMock;

  @BeforeEach
  void setUpRuntime() throws FileNotFoundException, URISyntaxException {
    DecisionRequirementsDefinition mockDecisionRequirementsDefinition = MockProvider.createMockDecisionRequirementsDefinition();

    setUpRuntimeData(mockDecisionRequirementsDefinition);
    decisionServiceMock = mock(DecisionService.class);
    when(processEngine.getDecisionService()).thenReturn(decisionServiceMock);
  }

  @Test
  void decisionRequirementsDefinitionRetrievalById() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_RESOURCE_NAME))
        .body("tenantId", equalTo(null))
    .when()
      .get(SINGLE_DECISION_REQUIREMENTS_DEFINITION_ID_URL);

    verify(repositoryServiceMock).getDecisionRequirementsDefinition(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);
  }

  @Test
  void nonExistingDecisionRequirementsDefinitionRetrieval() {
    String nonExistingId = "aNonExistingDefinitionId";

    when(repositoryServiceMock.getDecisionRequirementsDefinition(nonExistingId)).thenThrow(new ProcessEngineException("No matching decision requirements definition"));

    given()
      .pathParam("id", "aNonExistingDefinitionId")
    .then()
      .expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", is(RestException.class.getSimpleName()))
      .body("message", containsString("No matching decision requirements definition"))
    .when().get(SINGLE_DECISION_REQUIREMENTS_DEFINITION_ID_URL);
  }

  @Test
  void decisionRequirementsDefinitionRetrievalByKey() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_RESOURCE_NAME))
        .body("tenantId", equalTo(null))
    .when()
      .get(SINGLE_DECISION_REQUIREMENTS_DEFINITION_KEY_URL);

    verify(repositoryServiceMock).getDecisionRequirementsDefinition(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);
  }

  @Test
  void decisionRequirementsDefinitionRetrievalByNonExistingKey() {

    String nonExistingKey = "aNonExistingRequirementsDefinitionKey";

    when(repositoryServiceMock.createDecisionRequirementsDefinitionQuery()
      .decisionRequirementsDefinitionKey(nonExistingKey))
      .thenReturn(decisionRequirementsDefinitionQueryMock);

    when(decisionRequirementsDefinitionQueryMock.singleResult()).thenReturn(null);

    given()
      .pathParam("key", nonExistingKey)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", containsString("No matching decision requirements definition with key: " + nonExistingKey))
    .when()
      .get(SINGLE_DECISION_REQUIREMENTS_DEFINITION_KEY_URL);

  }

  @Test
  void decisionRequirementsDefinitionRetrievalByKeyAndTenantId() throws FileNotFoundException, URISyntaxException {
    DecisionRequirementsDefinition mockDefinition = MockProvider.mockDecisionRequirementsDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build();
    setUpRuntimeData(mockDefinition);

    given()
      .pathParam("key", MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_RESOURCE_NAME))
        .body("tenantId", equalTo(MockProvider.EXAMPLE_TENANT_ID))
    .when()
      .get(SINGLE_DECISION_REQUIREMENTS_DEFINITION_KEY_AND_TENANT_ID_URL);

    verify(decisionRequirementsDefinitionQueryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
    verify(repositoryServiceMock).getDecisionRequirementsDefinition(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);
  }

  @Test
  void nonExistingDecisionRequirementsDefinitionRetrievalByKeyAndTenantId() {
    String nonExistingKey = "aNonExistingDecisionDefinitionRequirementsDefinitionKey";
    String nonExistingTenantId = "aNonExistingTenantId";

    when(repositoryServiceMock.createDecisionRequirementsDefinitionQuery()
      .decisionRequirementsDefinitionKey(nonExistingKey))
      .thenReturn(decisionRequirementsDefinitionQueryMock);
    when(decisionRequirementsDefinitionQueryMock.singleResult()).thenReturn(null);

    given()
      .pathParam("key", nonExistingKey)
      .pathParam("tenant-id", nonExistingTenantId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", is(RestException.class.getSimpleName()))
      .body("message", containsString("No matching decision requirements definition with key: " + nonExistingKey + " and tenant-id: " + nonExistingTenantId))
    .when().get(SINGLE_DECISION_REQUIREMENTS_DEFINITION_KEY_AND_TENANT_ID_URL);
  }

  // dmn xml retrieval
  @Test
  void decisionRequirementsDefinitionDmnXmlRetrieval() {
    Response response = given()
      .pathParam("id", MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID)
      .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(XML_DEFINITION_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID)
      .contains("<?xml");
  }

  // DRD retrieval
  @Test
  void decisionRequirementsDiagramRetrieval() throws FileNotFoundException, URISyntaxException {
    byte[] actual = given().pathParam("id", MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID)
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType("image/png")
        .header("Content-Disposition", "attachment; " +
                "filename=\"" + MockProvider.EXAMPLE_DECISION_DEFINITION_DIAGRAM_RESOURCE_NAME + "\"; " +
                "filename*=UTF-8''" + MockProvider.EXAMPLE_DECISION_DEFINITION_DIAGRAM_RESOURCE_NAME
        )
      .when().get(DIAGRAM_DEFINITION_URL).getBody().asByteArray();

    verify(repositoryServiceMock).getDecisionRequirementsDefinition(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);
    verify(repositoryServiceMock).getDecisionRequirementsDiagram(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);

    byte[] expected = IoUtil.readInputStream(new FileInputStream(getFile()), "decision requirements diagram");
    assertThat(actual).containsExactly(expected);
  }

  protected void setUpRuntimeData(DecisionRequirementsDefinition mockDecisionRequirementsDefinition) throws FileNotFoundException, URISyntaxException {
    repositoryServiceMock = mock(RepositoryService.class);

    when(processEngine.getRepositoryService()).thenReturn(repositoryServiceMock);
    when(repositoryServiceMock.getDecisionRequirementsDefinition(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID)).thenReturn(mockDecisionRequirementsDefinition);
    when(repositoryServiceMock.getDecisionRequirementsModel(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID)).thenReturn(createMockDecisionRequirementsDefinitionDmnXml());
    when(repositoryServiceMock.getDecisionRequirementsDiagram(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID)).thenReturn(createMockDecisionRequirementsDiagram());

    decisionRequirementsDefinitionQueryMock = mock(DecisionRequirementsDefinitionQuery.class);
    when(decisionRequirementsDefinitionQueryMock.decisionRequirementsDefinitionKey(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY)).thenReturn(decisionRequirementsDefinitionQueryMock);
    when(decisionRequirementsDefinitionQueryMock.tenantIdIn(anyString())).thenReturn(decisionRequirementsDefinitionQueryMock);
    when(decisionRequirementsDefinitionQueryMock.withoutTenantId()).thenReturn(decisionRequirementsDefinitionQueryMock);
    when(decisionRequirementsDefinitionQueryMock.latestVersion()).thenReturn(decisionRequirementsDefinitionQueryMock);
    when(decisionRequirementsDefinitionQueryMock.singleResult()).thenReturn(mockDecisionRequirementsDefinition);
    when(decisionRequirementsDefinitionQueryMock.list()).thenReturn(Collections.singletonList(mockDecisionRequirementsDefinition));
    when(repositoryServiceMock.createDecisionRequirementsDefinitionQuery()).thenReturn(decisionRequirementsDefinitionQueryMock);
  }

  protected InputStream createMockDecisionRequirementsDefinitionDmnXml() {
    // do not close the input stream, will be done in implementation
    InputStream dmnXmlInputStream = ReflectUtil.getResourceAsStream("decisions/decision-requirements-model.dmn");
    assertThat(dmnXmlInputStream).isNotNull();
    return dmnXmlInputStream;
  }

  protected FileInputStream createMockDecisionRequirementsDiagram() throws URISyntaxException, FileNotFoundException {
    File file = getFile();
    return new FileInputStream(file);
  }

  protected File getFile() throws URISyntaxException {
    return getFile("/decisions/decision-requirements-diagram.png");
  }
}
