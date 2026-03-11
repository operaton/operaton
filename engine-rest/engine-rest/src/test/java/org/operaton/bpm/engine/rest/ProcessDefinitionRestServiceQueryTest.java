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
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockDefinitionBuilder;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static org.operaton.bpm.engine.rest.util.DateTimeUtils.withTimezone;
import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProcessDefinitionRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String PROCESS_DEFINITION_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/process-definition";
  protected static final String PROCESS_DEFINITION_COUNT_QUERY_URL = PROCESS_DEFINITION_QUERY_URL + "/count";
  private ProcessDefinitionQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockDefinitionQuery(MockProvider.createMockDefinitions());
  }

  private ProcessDefinitionQuery setUpMockDefinitionQuery(List<ProcessDefinition> mockedDefinitions) {
    ProcessDefinitionQuery sampleDefinitionsQuery = mock(ProcessDefinitionQuery.class);
    when(sampleDefinitionsQuery.list()).thenReturn(mockedDefinitions);
    when(sampleDefinitionsQuery.count()).thenReturn((long) mockedDefinitions.size());
    when(processEngine.getRepositoryService().createProcessDefinitionQuery()).thenReturn(sampleDefinitionsQuery);
    return sampleDefinitionsQuery;
  }

  @Test
  void testEmptyQuery() {
    String queryKey = "";
    given().queryParam("keyLike", queryKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(PROCESS_DEFINITION_QUERY_URL);
  }

  @Test
  void testInvalidNumericParameter() {
    String anInvalidIntegerQueryParam = "aString";
    given().queryParam("version", anInvalidIntegerQueryParam)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Cannot set query parameter 'version' to value 'aString': "
        + "Cannot convert value aString to java type java.lang.Integer"))
      .when().get(PROCESS_DEFINITION_QUERY_URL);
  }

  @Test
  void testInvalidBooleanParameter() {
    String anInvalidBooleanQueryParam = "neitherTrueNorFalse";
    given().queryParam("active", anInvalidBooleanQueryParam)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Cannot set query parameter 'active' to value 'neitherTrueNorFalse': "
          + "Cannot convert value neitherTrueNorFalse to java type java.lang.Boolean"))
      .when().get(PROCESS_DEFINITION_QUERY_URL);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifyFailingSorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST,
        InvalidRequestException.class.getSimpleName(), "Cannot set query parameter 'sortBy' to value 'anInvalidSortByOption'");
    executeAndVerifyFailingSorting("category", "anInvalidSortOrderOption", Status.BAD_REQUEST,
      InvalidRequestException.class.getSimpleName(), "Cannot set query parameter 'sortOrder' to value 'anInvalidSortOrderOption'");
  }

  protected void executeAndVerifySuccessfulSorting(String sortBy, String sortOrder, Status expectedStatus) {
    given().queryParam("sortBy", sortBy).queryParam("sortOrder", sortOrder)
      .then().expect().statusCode(expectedStatus.getStatusCode())
      .when().get(PROCESS_DEFINITION_QUERY_URL);
  }

  protected void executeAndVerifyFailingSorting(String sortBy, String sortOrder, Status expectedStatus, String expectedErrorType, String expectedErrorMessage) {
    given().queryParam("sortBy", sortBy).queryParam("sortOrder", sortOrder)
      .then().expect().statusCode(expectedStatus.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(expectedErrorType))
      .body("message", equalTo(expectedErrorMessage))
      .when().get(PROCESS_DEFINITION_QUERY_URL);
  }

  @Test
  void testSortByParameterOnly() {
    given().queryParam("sortBy", "category")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(PROCESS_DEFINITION_QUERY_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given().queryParam("sortOrder", "asc")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(PROCESS_DEFINITION_QUERY_URL);
  }

  @Test
  void testProcessDefinitionRetrieval() {
    InOrder inOrder = inOrder(mockedQuery);

    String queryKey = "Key";
    Response response = given().queryParam("keyLike", queryKey)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(PROCESS_DEFINITION_QUERY_URL);

    // assert query invocation
    inOrder.verify(mockedQuery).processDefinitionKeyLike(queryKey);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).as("There should be one process definition returned.").hasSize(1);
    assertThat(definitions.get(0)).as("There should be one process definition returned").isNotNull();

    String returnedDefinitionKey = from(content).getString("[0].key");
    String returnedDefinitionId = from(content).getString("[0].id");
    String returnedCategory = from(content).getString("[0].category");
    String returnedDefinitionName = from(content).getString("[0].name");
    String returnedDescription = from(content).getString("[0].description");
    int returnedVersion = from(content).getInt("[0].version");
    String returnedResourceName = from(content).getString("[0].resource");
    String returnedDeploymentId  = from(content).getString("[0].deploymentId");
    String returnedDiagramResourceName = from(content).getString("[0].diagram");
    Boolean returnedIsSuspended = from(content).getBoolean("[0].suspended");
    Boolean returnedIsStartedInTasklist = from(content).getBoolean("[0].startableInTasklist");

    assertThat(returnedDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedDefinitionKey).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    assertThat(returnedCategory).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_CATEGORY);
    assertThat(returnedDefinitionName).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_NAME);
    assertThat(returnedDescription).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DESCRIPTION);
    assertThat(returnedVersion).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_VERSION);
    assertThat(returnedResourceName).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_RESOURCE_NAME);
    assertThat(returnedDeploymentId).isEqualTo(MockProvider.EXAMPLE_DEPLOYMENT_ID);
    assertThat(returnedDiagramResourceName).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_DIAGRAM_RESOURCE_NAME);
    assertThat(returnedIsSuspended).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_IS_SUSPENDED);
    assertThat(returnedIsStartedInTasklist).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_IS_STARTABLE);
  }

  @Test
  void testProcessDefinitionRetrievalByList() {
    mockedQuery = setUpMockDefinitionQuery(MockProvider.createMockTwoDefinitions());

    Response response = given()
      .queryParam("processDefinitionIdIn", MockProvider.EXAMPLE_PROCESS_DEFINTION_ID_LIST)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(PROCESS_DEFINITION_QUERY_URL);

    // assert query invocation
    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).processDefinitionIdIn(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_ID);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(2);

    String returnedDefinitionId1 = from(content).getString("[0].id");
    String returnedDefinitionId2 = from(content).getString("[1].id");

    assertThat(returnedDefinitionId1).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedDefinitionId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testProcessDefinitionRetrievalByEmptyList() {
    mockedQuery = setUpMockDefinitionQuery(MockProvider.createMockTwoDefinitions());

    Response response = given()
      .queryParam("processDefinitionIdIn", "")
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(PROCESS_DEFINITION_QUERY_URL);

    // assert query invocation
    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery, never()).processDefinitionIdIn(Mockito.any());
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(2);

    String returnedDefinitionId1 = from(content).getString("[0].id");
    String returnedDefinitionId2 = from(content).getString("[1].id");

    assertThat(returnedDefinitionId1).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedDefinitionId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_ID);
  }

  @Test
  void testIncompleteProcessDefinition() {
    setUpMockDefinitionQuery(createIncompleteMockDefinitions());
    Response response = expect().statusCode(Status.OK.getStatusCode())
        .when().get(PROCESS_DEFINITION_QUERY_URL);

    String content = response.asString();
    String returnedResourceName = from(content).getString("[0].resource");
    assertThat(returnedResourceName).as("Should be null, as it is also null in the original process definition on the server.").isNull();
  }

  private List<ProcessDefinition> createIncompleteMockDefinitions() {
    List<ProcessDefinition> mocks = new ArrayList<>();

    MockDefinitionBuilder builder = new MockDefinitionBuilder();
    ProcessDefinition mockDefinition =
        builder.id(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
          .category(MockProvider.EXAMPLE_PROCESS_DEFINITION_CATEGORY)
          .name(MockProvider.EXAMPLE_PROCESS_DEFINITION_NAME)
          .key(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
          .suspended(MockProvider.EXAMPLE_PROCESS_DEFINITION_IS_SUSPENDED)
          .version(MockProvider.EXAMPLE_PROCESS_DEFINITION_VERSION).build();

    mocks.add(mockDefinition);
    return mocks;
  }

  @Test
  void testNoParametersQuery() {
    expect().statusCode(Status.OK.getStatusCode()).when().get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testAdditionalParameters() {
    Map<String, String> queryParameters = getCompleteQueryParameters();

    given().queryParams(queryParameters)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().get(PROCESS_DEFINITION_QUERY_URL);

    // assert query invocation
    verify(mockedQuery).processDefinitionId(queryParameters.get("processDefinitionId"));
    verify(mockedQuery).processDefinitionCategory(queryParameters.get("category"));
    verify(mockedQuery).processDefinitionCategoryLike(queryParameters.get("categoryLike"));
    verify(mockedQuery).processDefinitionName(queryParameters.get("name"));
    verify(mockedQuery).processDefinitionNameLike(queryParameters.get("nameLike"));
    verify(mockedQuery).deploymentId(queryParameters.get("deploymentId"));
    verify(mockedQuery).processDefinitionKey(queryParameters.get("key"));
    verify(mockedQuery).processDefinitionKeyLike(queryParameters.get("keyLike"));
    verify(mockedQuery).processDefinitionVersion(Integer.parseInt(queryParameters.get("version")));
    verify(mockedQuery).latestVersion();
    verify(mockedQuery).processDefinitionResourceName(queryParameters.get("resourceName"));
    verify(mockedQuery).processDefinitionResourceNameLike(queryParameters.get("resourceNameLike"));
    verify(mockedQuery).startableByUser(queryParameters.get("startableBy"));
    verify(mockedQuery).active();
    verify(mockedQuery).suspended();
    verify(mockedQuery).incidentId(queryParameters.get("incidentId"));
    verify(mockedQuery).incidentType(queryParameters.get("incidentType"));
    verify(mockedQuery).incidentMessage(queryParameters.get("incidentMessage"));
    verify(mockedQuery).incidentMessageLike(queryParameters.get("incidentMessageLike"));
    verify(mockedQuery).versionTag(queryParameters.get("versionTag"));
    verify(mockedQuery).versionTagLike(queryParameters.get("versionTagLike"));
    verify(mockedQuery).startableInTasklist();
    verify(mockedQuery).list();
  }

  private Map<String, String> getCompleteQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("processDefinitionId", "anId");
    parameters.put("category", "cat");
    parameters.put("categoryLike", "catlike");
    parameters.put("name", "name");
    parameters.put("nameLike", "namelike");
    parameters.put("deploymentId", "depId");
    parameters.put("key", "key");
    parameters.put("keyLike", "keylike");
    parameters.put("version", "0");
    parameters.put("latestVersion", "true");
    parameters.put("resourceName", "res");
    parameters.put("resourceNameLike", "resLike");
    parameters.put("startableBy", "kermit");
    parameters.put("suspended", "true");
    parameters.put("active", "true");
    parameters.put("incidentId", "incId");
    parameters.put("incidentType", "incType");
    parameters.put("incidentMessage", "incMessage");
    parameters.put("incidentMessageLike", "incMessageLike");
    parameters.put("versionTag", "semVer");
    parameters.put("versionTagLike", "semVerLike");
    parameters.put("startableInTasklist", "true");

    return parameters;
  }

  @Test
  void testProcessDefinitionTenantIdList() {
    List<ProcessDefinition> processDefinitions = List.of(
        MockProvider.mockDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build(),
        MockProvider.mockDefinition().id(MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_ID).tenantId(MockProvider.ANOTHER_EXAMPLE_TENANT_ID).build());
    mockedQuery = setUpMockDefinitionQuery(processDefinitions);

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testProcessDefinitionKeysList() {
    List<ProcessDefinition> processDefinitions = List.of(
            MockProvider.mockDefinition().key(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY).build(),
            MockProvider.mockDefinition().key(MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_KEY).build());
    mockedQuery = setUpMockDefinitionQuery(processDefinitions);

    Response response = given()
            .queryParam("keysIn", MockProvider.EXAMPLE_KEY_LIST)
            .then().expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).processDefinitionKeyIn(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY,
            MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(2);

    String returnedKey1 = from(content).getString("[0].key");
    String returnedKey2 = from(content).getString("[1].key");

    assertThat(returnedKey1).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    assertThat(returnedKey2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_KEY);
  }

  @Test
  void testProcessDefinitionWithoutTenantId() {
    Response response = given()
      .queryParam("withoutTenantId", true)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId1).isNull();
  }

  @Test
  void testProcessDefinitionTenantIdIncludeDefinitionsWithoutTenantid() {
    List<ProcessDefinition> processDefinitions = List.of(
        MockProvider.mockDefinition().tenantId(null).build(),
        MockProvider.mockDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build());
    mockedQuery = setUpMockDefinitionQuery(processDefinitions);

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID)
      .queryParam("includeProcessDefinitionsWithoutTenantId", true)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
    verify(mockedQuery).includeProcessDefinitionsWithoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isNull();
    assertThat(returnedTenantId2).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
  }

  @Test
  void testProcessDefinitionVersionTag() {
    List<ProcessDefinition> processDefinitions = List.of(
      MockProvider.mockDefinition().versionTag(MockProvider.EXAMPLE_VERSION_TAG).build(),
      MockProvider.mockDefinition().id(MockProvider.ANOTHER_EXAMPLE_PROCESS_DEFINITION_ID).versionTag(MockProvider.ANOTHER_EXAMPLE_VERSION_TAG).build());
    mockedQuery = setUpMockDefinitionQuery(processDefinitions);

    given()
      .queryParam("versionTag", MockProvider.EXAMPLE_VERSION_TAG)
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).versionTag(MockProvider.EXAMPLE_VERSION_TAG);
    verify(mockedQuery).list();
  }

  @Test
  void testProcessDefinitionWithoutVersionTag() {
    given()
      .queryParam("withoutVersionTag", true)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).withoutVersionTag();
    verify(mockedQuery).list();
  }

  @Test
  void testNotStartableInTasklist() {
    List<ProcessDefinition> processDefinitions = List.of(
      MockProvider.mockDefinition().isStartableInTasklist(false).build());
    mockedQuery = setUpMockDefinitionQuery(processDefinitions);

    given()
      .queryParam("notStartableInTasklist", true)
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).notStartableInTasklist();
    verify(mockedQuery).list();
  }

  @Test
  void testStartableInTasklistPermissionCheck() {
    List<ProcessDefinition> processDefinitions = List.of(
      MockProvider.mockDefinition().isStartableInTasklist(false).build());
    mockedQuery = setUpMockDefinitionQuery(processDefinitions);

    given()
      .queryParam("startablePermissionCheck", true)
      .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).startablePermissionCheck();
    verify(mockedQuery).list();
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("category", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionCategory();
    inOrder.verify(mockedQuery).asc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("key", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionKey();
    inOrder.verify(mockedQuery).desc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("id", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionId();
    inOrder.verify(mockedQuery).asc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("version", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionVersion();
    inOrder.verify(mockedQuery).desc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("name", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByProcessDefinitionName();
    inOrder.verify(mockedQuery).asc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("deploymentId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByDeploymentId();
    inOrder.verify(mockedQuery).desc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("deployTime", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByDeploymentTime();
    inOrder.verify(mockedQuery).desc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).asc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByTenantId();
    inOrder.verify(mockedQuery).desc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("versionTag", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByVersionTag();
    inOrder.verify(mockedQuery).asc();

    inOrder = inOrder(mockedQuery);
    executeAndVerifySuccessfulSorting("versionTag", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByVersionTag();
    inOrder.verify(mockedQuery).asc();
  }

  @Test
  void testSuccessfulPagination() {
    int firstResult = 0;
    int maxResults = 10;
    given().queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).listPage(firstResult, maxResults);
  }

  /**
   * If parameter "firstResult" is missing, we expect 0 as default.
   */
  @Test
  void testMissingFirstResultParameter() {
    int maxResults = 10;
    given().queryParam("maxResults", maxResults)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).listPage(0, maxResults);
  }

  /**
   * If parameter "maxResults" is missing, we expect Integer.MAX_VALUE as default.
   */
  @Test
  void testMissingMaxResultsParameter() {
    int firstResult = 10;
    given().queryParam("firstResult", firstResult)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect().statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
      .when().get(PROCESS_DEFINITION_COUNT_QUERY_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testQueryByDeployTimeAfter() {
    String deployTime = withTimezone("2020-03-27T01:23:45");
    Date date = DateTimeUtil.parseDate(deployTime);

    given().queryParam("deployedAfter", deployTime)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).deployedAfter(date);
    verify(mockedQuery).list();
  }

  @Test
  void testQueryByDeployTimeAt() {
    String deployTime = withTimezone("2020-03-27T05:43:21");
    Date date = DateTimeUtil.parseDate(deployTime);

    given().queryParam("deployedAt", deployTime)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(PROCESS_DEFINITION_QUERY_URL);

    verify(mockedQuery).deployedAt(date);
    verify(mockedQuery).list();
  }
}
