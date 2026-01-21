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

import java.util.Base64;
import java.util.Collections;
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

import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.history.HistoricDecisionInputInstanceDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricDecisionOutputInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.variable.value.BytesValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.StringValue;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HistoricDecisionInstanceRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_DECISION_INSTANCE_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/decision-instance";
  protected static final String HISTORIC_DECISION_INSTANCE_COUNT_RESOURCE_URL = HISTORIC_DECISION_INSTANCE_RESOURCE_URL + "/count";

  protected HistoricDecisionInstanceQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setUpMockHistoricDecisionInstanceQuery(MockProvider.createMockHistoricDecisionInstances());
  }

  protected HistoricDecisionInstanceQuery setUpMockHistoricDecisionInstanceQuery(List<HistoricDecisionInstance> mockedHistoricDecisionInstances) {
    HistoricDecisionInstanceQuery mockedHistoricDecisionInstanceQuery = mock(HistoricDecisionInstanceQuery.class);
    when(mockedHistoricDecisionInstanceQuery.list()).thenReturn(mockedHistoricDecisionInstances);
    when(mockedHistoricDecisionInstanceQuery.count()).thenReturn((long) mockedHistoricDecisionInstances.size());

    when(processEngine.getHistoryService().createHistoricDecisionInstanceQuery()).thenReturn(mockedHistoricDecisionInstanceQuery);

    return mockedHistoricDecisionInstanceQuery;
  }

  @Test
  void testEmptyQuery() {
    String queryKey = "";
    given()
      .queryParam("caseDefinitionKey", queryKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testNoParametersQuery() {
    expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).list();
    verifyNoMoreInteractions(mockedQuery);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("definitionId", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  @Test
  void testSortByParameterOnly() {
    given()
      .queryParam("sortBy", "evaluationTime")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given()
      .queryParam("sortOrder", "asc")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("evaluationTime", "asc", Status.OK);
    inOrder.verify(mockedQuery).orderByEvaluationTime();
    inOrder.verify(mockedQuery).asc();

    inOrder = Mockito.inOrder(mockedQuery);
    executeAndVerifySorting("evaluationTime", "desc", Status.OK);
    inOrder.verify(mockedQuery).orderByEvaluationTime();
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
  void testSuccessfulPagination() {
    int firstResult = 0;
    int maxResults = 10;

    given()
      .queryParam("firstResult", firstResult)
      .queryParam("maxResults", maxResults)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, maxResults);
  }

  @Test
  void testMissingFirstResultParameter() {
    int maxResults = 10;

    given()
      .queryParam("maxResults", maxResults)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(0, maxResults);
  }

  @Test
  void testMissingMaxResultsParameter() {
    int firstResult = 10;

    given()
      .queryParam("firstResult", firstResult)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).listPage(firstResult, Integer.MAX_VALUE);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(HISTORIC_DECISION_INSTANCE_COUNT_RESOURCE_URL);

    verify(mockedQuery).count();
  }

  @Test
  void testSimpleHistoricDecisionInstanceQuery() {
    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    Response response = given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0)).isNotNull();

    String returnedHistoricDecisionInstanceId = from(content).getString("[0].id");
    String returnedDecisionDefinitionId = from(content).getString("[0].decisionDefinitionId");
    String returnedDecisionDefinitionKey = from(content).getString("[0].decisionDefinitionKey");
    String returnedDecisionDefinitionName = from(content).getString("[0].decisionDefinitionName");
    String returnedEvaluationTime = from(content).getString("[0].evaluationTime");
    String returnedRemovalTime = from(content).getString("[0].removalTime");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessDefinitionKey = from(content).getString("[0].processDefinitionKey");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedCaseDefinitionId = from(content).getString("[0].caseDefinitionId");
    String returnedCaseDefinitionKey = from(content).getString("[0].caseDefinitionKey");
    String returnedCaseInstanceId = from(content).getString("[0].caseInstanceId");
    String returnedActivityId = from(content).getString("[0].activityId");
    String returnedActivityInstanceId = from(content).getString("[0].activityInstanceId");
    List<HistoricDecisionInputInstanceDto> returnedInputs = from(content).getList("[0].inputs");
    List<HistoricDecisionOutputInstanceDto> returnedOutputs = from(content).getList("[0].outputs");
    Double returnedCollectResultValue = from(content).getDouble("[0].collectResultValue");
    String returnedTenantId = from(content).getString("[0].tenantId");
    String returnedRootDecisionInstanceId = from(content).getString("[0].rootDecisionInstanceId");
    String returnedRootProcessInstanceId = from(content).getString("[0].rootProcessInstanceId");
    String returnedDecisionRequirementsDefinitionId = from(content).getString("[0].decisionRequirementsDefinitionId");
    String returnedDecisionRequirementsDefinitionKey = from(content).getString("[0].decisionRequirementsDefinitionKey");

    assertThat(returnedHistoricDecisionInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    assertThat(returnedDecisionDefinitionId).isEqualTo(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
    assertThat(returnedDecisionDefinitionKey).isEqualTo(MockProvider.EXAMPLE_DECISION_DEFINITION_KEY);
    assertThat(returnedDecisionDefinitionName).isEqualTo(MockProvider.EXAMPLE_DECISION_DEFINITION_NAME);
    assertThat(returnedEvaluationTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_EVALUATION_TIME);
    assertThat(returnedRemovalTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_REMOVAL_TIME);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedProcessDefinitionKey).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedCaseDefinitionId).isEqualTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    assertThat(returnedCaseDefinitionKey).isEqualTo(MockProvider.EXAMPLE_CASE_DEFINITION_KEY);
    assertThat(returnedCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID);
    assertThat(returnedActivityId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ACTIVITY_ID);
    assertThat(returnedActivityInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ACTIVITY_INSTANCE_ID);
    assertThat(returnedInputs).isNull();
    assertThat(returnedOutputs).isNull();
    assertThat(returnedCollectResultValue).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_COLLECT_RESULT_VALUE);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedRootDecisionInstanceId).isEqualTo(MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    assertThat(returnedRootProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_ROOT_HISTORIC_PROCESS_INSTANCE_ID);
    assertThat(returnedDecisionRequirementsDefinitionId).isEqualTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);
    assertThat(returnedDecisionRequirementsDefinitionKey).isEqualTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY);
  }

  @Test
  void testAdditionalParameters() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    given()
      .queryParams(stringQueryParameters)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    verifyStringParameterQueryInvocations();
  }

  @Test
  void testIncludeInputs() {
    mockedQuery = setUpMockHistoricDecisionInstanceQuery(Collections.singletonList(MockProvider.createMockHistoricDecisionInstanceWithInputs()));

    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    Response response = given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
        .queryParam("includeInputs", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery).includeInputs();
    inOrder.verify(mockedQuery, never()).includeOutputs();
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0)).isNotNull();

    List<Map<String, Object>> returnedInputs = from(content).getList("[0].inputs");
    List<Map<String, Object>> returnedOutputs = from(content).getList("[0].outputs");

    assertThat(returnedInputs).isNotNull();
    assertThat(returnedOutputs).isNull();

    verifyHistoricDecisionInputInstances(returnedInputs);
  }

  @Test
  void testIncludeOutputs() {
    mockedQuery = setUpMockHistoricDecisionInstanceQuery(Collections.singletonList(MockProvider.createMockHistoricDecisionInstanceWithOutputs()));

    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    Response response = given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
        .queryParam("includeOutputs", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery, never()).includeInputs();
    inOrder.verify(mockedQuery).includeOutputs();
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0)).isNotNull();

    List<Map<String, Object>> returnedInputs = from(content).getList("[0].inputs");
    List<Map<String, Object>> returnedOutputs = from(content).getList("[0].outputs");

    assertThat(returnedInputs).isNull();
    assertThat(returnedOutputs).isNotNull();

    verifyHistoricDecisionOutputInstances(returnedOutputs);
  }

  @Test
  void testIncludeInputsAndOutputs() {
    mockedQuery = setUpMockHistoricDecisionInstanceQuery(Collections.singletonList(MockProvider.createMockHistoricDecisionInstanceWithInputsAndOutputs()));

    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    Response response = given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
        .queryParam("includeInputs", true)
        .queryParam("includeOutputs", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery).includeInputs();
    inOrder.verify(mockedQuery).includeOutputs();
    inOrder.verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0)).isNotNull();

    List<Map<String, Object>> returnedInputs = from(content).getList("[0].inputs");
    List<Map<String, Object>> returnedOutputs = from(content).getList("[0].outputs");

    assertThat(returnedInputs).isNotNull();
    assertThat(returnedOutputs).isNotNull();

    verifyHistoricDecisionInputInstances(returnedInputs);
    verifyHistoricDecisionOutputInstances(returnedOutputs);
  }

  @Test
  void testDefaultBinaryFetching() {
    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery, never()).disableBinaryFetching();
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testDisableBinaryFetching() {
    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
        .queryParam("disableBinaryFetching", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery).disableBinaryFetching();
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testDefaultCustomObjectDeserialization() {
    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery, never()).disableCustomObjectDeserialization();
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testDisableCustomObjectDeserialization() {
    String decisionDefinitionId = MockProvider.EXAMPLE_DECISION_DEFINITION_ID;

    given()
        .queryParam("decisionDefinitionId", decisionDefinitionId)
        .queryParam("disableCustomObjectDeserialization", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    InOrder inOrder = inOrder(mockedQuery);
    inOrder.verify(mockedQuery).decisionDefinitionId(decisionDefinitionId);
    inOrder.verify(mockedQuery).disableCustomObjectDeserialization();
    inOrder.verify(mockedQuery).list();
  }

  @Test
  void testRootDecisionInstancesOnly() {

    given()
        .queryParam("rootDecisionInstancesOnly", true)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).rootDecisionInstancesOnly();
    verify(mockedQuery).list();
  }

  @Test
  void testTenantIdListParameter() {
    mockedQuery = setUpMockHistoricDecisionInstanceQuery(createMockHistoricDecisionInstancesTwoTenants());

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    verify(mockedQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> historicDecisionInstances = from(content).getList("");
    assertThat(historicDecisionInstances).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testWithoutTenantIdQueryParameter() {
    // given
    mockedQuery = setUpMockHistoricDecisionInstanceQuery(Collections.singletonList(MockProvider.createMockHistoricDecisionInstanceBase(null)));

    // when
    Response response = given()
          .queryParam("withoutTenantId", true)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);

    // then
    verify(mockedQuery).withoutTenantId();
    verify(mockedQuery).list();

    String content = response.asString();
    List<Map<String, Object>> definitions = from(content).getList("");
    assertThat(definitions).hasSize(1);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    assertThat(returnedTenantId1).isNull();
  }

  private List<HistoricDecisionInstance> createMockHistoricDecisionInstancesTwoTenants() {
    return List.of(
        MockProvider.createMockHistoricDecisionInstanceBase(MockProvider.EXAMPLE_TENANT_ID),
        MockProvider.createMockHistoricDecisionInstanceBase(MockProvider.ANOTHER_EXAMPLE_TENANT_ID));
  }

  protected Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("decisionInstanceId", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    parameters.put("decisionInstanceIdIn", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID_IN);
    parameters.put("decisionDefinitionId", MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
    parameters.put("decisionDefinitionIdIn", MockProvider.EXAMPLE_DECISION_DEFINITION_ID_IN);
    parameters.put("decisionDefinitionKey", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY);
    parameters.put("decisionDefinitionKeyIn", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY_IN);
    parameters.put("decisionDefinitionName", MockProvider.EXAMPLE_DECISION_DEFINITION_NAME);
    parameters.put("decisionDefinitionNameLike", MockProvider.EXAMPLE_DECISION_DEFINITION_NAME_LIKE);
    parameters.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    parameters.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    parameters.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    parameters.put("caseDefinitionId", MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    parameters.put("caseDefinitionKey", MockProvider.EXAMPLE_CASE_DEFINITION_KEY);
    parameters.put("caseInstanceId", MockProvider.EXAMPLE_CASE_INSTANCE_ID);
    parameters.put("activityIdIn", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ACTIVITY_ID_IN);
    parameters.put("activityInstanceIdIn", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ACTIVITY_INSTANCE_ID_IN);
    parameters.put("evaluatedBefore", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_EVALUATED_BEFORE);
    parameters.put("evaluatedAfter", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_EVALUATED_AFTER);
    parameters.put("userId", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_USER_ID);
    parameters.put("rootDecisionInstanceId", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID);
    parameters.put("decisionRequirementsDefinitionId", MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID);
    parameters.put("decisionRequirementsDefinitionKey", MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY);

    return parameters;
  }

  protected void verifyStringParameterQueryInvocations() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();
    StringArrayConverter stringArrayConverter = new StringArrayConverter();

    verify(mockedQuery).decisionInstanceId(stringQueryParameters.get("decisionInstanceId"));
    verify(mockedQuery).decisionInstanceIdIn(stringArrayConverter.convertQueryParameterToType(stringQueryParameters.get("decisionInstanceIdIn")));
    verify(mockedQuery).decisionDefinitionId(stringQueryParameters.get("decisionDefinitionId"));
    verify(mockedQuery).decisionDefinitionIdIn(stringArrayConverter.convertQueryParameterToType(stringQueryParameters.get("decisionDefinitionIdIn")));
    verify(mockedQuery).decisionDefinitionKey(stringQueryParameters.get("decisionDefinitionKey"));
    verify(mockedQuery).decisionDefinitionKeyIn(stringArrayConverter.convertQueryParameterToType(stringQueryParameters.get("decisionDefinitionKeyIn")));
    verify(mockedQuery).decisionDefinitionName(stringQueryParameters.get("decisionDefinitionName"));
    verify(mockedQuery).decisionDefinitionNameLike(stringQueryParameters.get("decisionDefinitionNameLike"));
    verify(mockedQuery).processDefinitionId(stringQueryParameters.get("processDefinitionId"));
    verify(mockedQuery).processDefinitionKey(stringQueryParameters.get("processDefinitionKey"));
    verify(mockedQuery).processInstanceId(stringQueryParameters.get("processInstanceId"));
    verify(mockedQuery).caseDefinitionId(stringQueryParameters.get("caseDefinitionId"));
    verify(mockedQuery).caseDefinitionKey(stringQueryParameters.get("caseDefinitionKey"));
    verify(mockedQuery).caseInstanceId(stringQueryParameters.get("caseInstanceId"));
    verify(mockedQuery).activityIdIn(stringArrayConverter.convertQueryParameterToType(stringQueryParameters.get("activityIdIn")));
    verify(mockedQuery).activityInstanceIdIn(stringArrayConverter.convertQueryParameterToType(stringQueryParameters.get("activityInstanceIdIn")));
    verify(mockedQuery).evaluatedBefore(DateTimeUtil.parseDate(stringQueryParameters.get("evaluatedBefore")));
    verify(mockedQuery).evaluatedAfter(DateTimeUtil.parseDate(stringQueryParameters.get("evaluatedAfter")));
    verify(mockedQuery).userId(stringQueryParameters.get("userId"));
    verify(mockedQuery).rootDecisionInstanceId(stringQueryParameters.get("rootDecisionInstanceId"));
    verify(mockedQuery).decisionRequirementsDefinitionId(stringQueryParameters.get("decisionRequirementsDefinitionId"));
    verify(mockedQuery).decisionRequirementsDefinitionKey(stringQueryParameters.get("decisionRequirementsDefinitionKey"));

    verify(mockedQuery).list();
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
        .queryParam("sortBy", sortBy)
        .queryParam("sortOrder", sortOrder)
      .then().expect()
        .statusCode(expectedStatus.getStatusCode())
      .when()
        .get(HISTORIC_DECISION_INSTANCE_RESOURCE_URL);
  }

  protected void verifyHistoricDecisionInputInstances(List<Map<String, Object>> returnedInputs) {
    assertThat(returnedInputs).hasSize(3);

    // verify common properties
    for (Map<String, Object> returnedInput : returnedInputs) {
      assertThat(returnedInput)
              .containsEntry("id", MockProvider.EXAMPLE_HISTORIC_DECISION_INPUT_INSTANCE_ID)
              .containsEntry("decisionInstanceId", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
              .containsEntry("clauseId", MockProvider.EXAMPLE_HISTORIC_DECISION_INPUT_INSTANCE_CLAUSE_ID)
              .containsEntry("clauseName", MockProvider.EXAMPLE_HISTORIC_DECISION_INPUT_INSTANCE_CLAUSE_NAME)
              .containsEntry("errorMessage", null)
              .containsEntry("createTime", MockProvider.EXAMPLE_HISTORIC_DECISION_INPUT_INSTANCE_CREATE_TIME)
              .containsEntry("removalTime", MockProvider.EXAMPLE_HISTORIC_DECISION_INPUT_INSTANCE_REMOVAL_TIME)
              .containsEntry("rootProcessInstanceId", MockProvider.EXAMPLE_HISTORIC_DECISION_INPUT_ROOT_PROCESS_INSTANCE_ID);
    }

    verifyStringValue(returnedInputs.get(0));
    verifyByteArrayValue(returnedInputs.get(1));
    verifySerializedValue(returnedInputs.get(2));

  }

  protected void verifyHistoricDecisionOutputInstances(List<Map<String, Object>> returnedOutputs) {
    assertThat(returnedOutputs).hasSize(3);

    // verify common properties
    for (Map<String, Object> returnedOutput : returnedOutputs) {
      assertThat(returnedOutput)
              .containsEntry("id", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_ID)
              .containsEntry("decisionInstanceId", MockProvider.EXAMPLE_HISTORIC_DECISION_INSTANCE_ID)
              .containsEntry("clauseId", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_CLAUSE_ID)
              .containsEntry("clauseName", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_CLAUSE_NAME)
              .containsEntry("ruleId", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_RULE_ID)
              .containsEntry("ruleOrder", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_RULE_ORDER)
              .containsEntry("variableName", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_VARIABLE_NAME)
              .containsEntry("errorMessage", null)
              .containsEntry("createTime", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_CREATE_TIME)
              .containsEntry("removalTime", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_INSTANCE_REMOVAL_TIME)
              .containsEntry("rootProcessInstanceId", MockProvider.EXAMPLE_HISTORIC_DECISION_OUTPUT_ROOT_PROCESS_INSTANCE_ID);
    }

    verifyStringValue(returnedOutputs.get(0));
    verifyByteArrayValue(returnedOutputs.get(1));
    verifySerializedValue(returnedOutputs.get(2));
  }

  protected void verifyStringValue(Map<String, Object> stringValue) {
    StringValue exampleValue = MockProvider.EXAMPLE_HISTORIC_DECISION_STRING_VALUE;
    assertThat(stringValue)
            .containsEntry("type", VariableValueDto.toRestApiTypeName(exampleValue.getType().getName()))
            .containsEntry("value", exampleValue.getValue())
            .containsEntry("valueInfo", Collections.emptyMap());
  }

  protected void verifyByteArrayValue(Map<String, Object> byteArrayValue) {
    BytesValue exampleValue = MockProvider.EXAMPLE_HISTORIC_DECISION_BYTE_ARRAY_VALUE;
    assertThat(byteArrayValue).containsEntry("type", VariableValueDto.toRestApiTypeName(exampleValue.getType().getName()));
    String byteString = Base64.getEncoder().encodeToString(exampleValue.getValue()).trim();
    assertThat(byteArrayValue)
            .containsEntry("value", byteString)
            .containsEntry("valueInfo", Collections.emptyMap());
  }

  @SuppressWarnings("unchecked")
  protected void verifySerializedValue(Map<String, Object> serializedValue) {
    ObjectValue exampleValue = MockProvider.EXAMPLE_HISTORIC_DECISION_SERIALIZED_VALUE;
    assertThat(serializedValue)
            .containsEntry("type", VariableValueDto.toRestApiTypeName(exampleValue.getType().getName()))
            .containsEntry("value", exampleValue.getValue());
    Map<String, String> valueInfo = (Map<String, String>) serializedValue.get("valueInfo");
    assertThat(valueInfo)
            .containsEntry("serializationDataFormat", exampleValue.getSerializationDataFormat())
            .containsEntry("objectTypeName", exampleValue.getObjectTypeName());

  }

}
