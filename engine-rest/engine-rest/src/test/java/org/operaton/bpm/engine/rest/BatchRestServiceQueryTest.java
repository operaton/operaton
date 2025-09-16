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

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchQuery;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static org.operaton.bpm.engine.rest.util.JsonPathUtil.from;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BatchRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String BATCH_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/batch";
  protected static final String BATCH_QUERY_COUNT_URL = BATCH_RESOURCE_URL + "/count";

  protected BatchQuery queryMock;

  @BeforeEach
  void setUpBatchQueryMock() {
    List<Batch> mockedBatches = MockProvider.createMockBatches();
    queryMock = mock(BatchQuery.class);

    when(queryMock.list()).thenReturn(mockedBatches);
    when(queryMock.count()).thenReturn((long) mockedBatches.size());

    when(processEngine.getManagementService().createBatchQuery()).thenReturn(queryMock);
  }

  @Test
  void testNoParametersQuery() {
    Response response = given()
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(BATCH_RESOURCE_URL);

    verify(queryMock).list();
    verifyNoMoreInteractions(queryMock);

    verifyBatchListJson(response.asString());
  }

  @Test
  void testUnknownQueryParameter() {
    Response response = given()
      .queryParam("unknown", "unknown")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(BATCH_RESOURCE_URL);

    verify(queryMock, never()).batchId(anyString());
    verify(queryMock).list();

    verifyBatchListJson(response.asString());
  }

  @Test
  void testSortByParameterOnly() {
    given()
      .queryParam("sortBy", "batchId")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type",
        equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message",
        equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(BATCH_RESOURCE_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given()
      .queryParam("sortOrder", "asc")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type",
        equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message",
        equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
    .when()
      .get(BATCH_RESOURCE_URL);
  }

  @Test
  void testBatchQueryByBatchId() {
    Response response = given()
      .queryParam("batchId", MockProvider.EXAMPLE_BATCH_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(BATCH_RESOURCE_URL);

    InOrder inOrder = inOrder(queryMock);
    inOrder.verify(queryMock).batchId(MockProvider.EXAMPLE_BATCH_ID);
    inOrder.verify(queryMock).list();
    inOrder.verifyNoMoreInteractions();

    verifyBatchListJson(response.asString());
  }

  @Test
  void testQueryActiveBatches() {
    Response response = given()
      .queryParam("suspended", false)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(BATCH_RESOURCE_URL);

    InOrder inOrder = inOrder(queryMock);
    inOrder.verify(queryMock).active();
    inOrder.verify(queryMock).list();
    inOrder.verifyNoMoreInteractions();

    verifyBatchListJson(response.asString());
  }

  @Test
  void testFullBatchQuery() {
    Response response = given()
        .queryParams(getCompleteQueryParameters())
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(BATCH_RESOURCE_URL);

    verifyQueryParameterInvocations();
    verify(queryMock).list();
    verifyNoMoreInteractions(queryMock);

    verifyBatchListJson(response.asString());
  }

  @Test
  void testQueryCount() {
    given()
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(BATCH_QUERY_COUNT_URL);

    verify(queryMock).count();
    verifyNoMoreInteractions(queryMock);
  }

  @Test
  void testFullQueryCount() {
    given()
      .params(getCompleteQueryParameters())
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
    .when()
      .get(BATCH_QUERY_COUNT_URL);

    verifyQueryParameterInvocations();
    verify(queryMock).count();
    verifyNoMoreInteractions(queryMock);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(queryMock);
    executeAndVerifySorting("batchId", "desc", Status.OK);
    inOrder.verify(queryMock).orderById();
    inOrder.verify(queryMock).desc();

    inOrder = Mockito.inOrder(queryMock);
    executeAndVerifySorting("batchId", "asc", Status.OK);
    inOrder.verify(queryMock).orderById();
    inOrder.verify(queryMock).asc();

    inOrder = Mockito.inOrder(queryMock);
    executeAndVerifySorting("tenantId", "asc", Status.OK);
    inOrder.verify(queryMock).orderByTenantId();
    inOrder.verify(queryMock).asc();

    inOrder = Mockito.inOrder(queryMock);
    executeAndVerifySorting("tenantId", "desc", Status.OK);
    inOrder.verify(queryMock).orderByTenantId();
    inOrder.verify(queryMock).desc();
  }

  private void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder)
    .then().expect()
      .statusCode(expectedStatus.getStatusCode())
    .when()
      .get(BATCH_RESOURCE_URL);
  }

  protected Map<String, Object> getCompleteQueryParameters() {
    Map<String, Object> parameters = new HashMap<>();

    parameters.put("batchId", MockProvider.EXAMPLE_BATCH_ID);
    parameters.put("type", MockProvider.EXAMPLE_BATCH_TYPE);
    parameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID + "," + MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    parameters.put("withoutTenantId", true);
    parameters.put("suspended", true);

    return parameters;
  }

  protected void verifyQueryParameterInvocations() {
    verify(queryMock).batchId(MockProvider.EXAMPLE_BATCH_ID);
    verify(queryMock).type(MockProvider.EXAMPLE_BATCH_TYPE);
    verify(queryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(queryMock).withoutTenantId();
    verify(queryMock).suspended();
  }

  protected void verifyBatchListJson(String batchListJson) {
    List<Object> batches = from(batchListJson).get();
    assertThat(batches).as("There should be one batch returned.").hasSize(1);

    BatchDto batch = from(batchListJson).getObject("[0]", BatchDto.class);
    String returnedStartTime = from(batchListJson).getString("[0].startTime");

    assertThat(batch).as("The returned batch should not be null.").isNotNull();
    assertThat(batch.getId()).isEqualTo(MockProvider.EXAMPLE_BATCH_ID);
    assertThat(batch.getType()).isEqualTo(MockProvider.EXAMPLE_BATCH_TYPE);
    assertThat(batch.getTotalJobs()).isEqualTo(MockProvider.EXAMPLE_BATCH_TOTAL_JOBS);
    assertThat(batch.getJobsCreated()).isEqualTo(MockProvider.EXAMPLE_BATCH_JOBS_CREATED);
    assertThat(batch.getBatchJobsPerSeed()).isEqualTo(MockProvider.EXAMPLE_BATCH_JOBS_PER_SEED);
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(MockProvider.EXAMPLE_INVOCATIONS_PER_BATCH_JOB);
    assertThat(batch.getSeedJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_SEED_JOB_DEFINITION_ID);
    assertThat(batch.getMonitorJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_MONITOR_JOB_DEFINITION_ID);
    assertThat(batch.getBatchJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_BATCH_JOB_DEFINITION_ID);
    assertThat(batch.getTenantId()).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(batch.getCreateUserId()).isEqualTo(MockProvider.EXAMPLE_USER_ID);
    assertThat(returnedStartTime).isEqualTo(MockProvider.EXAMPLE_HISTORIC_BATCH_START_TIME);
    assertThat(batch.isSuspended()).isTrue();
  }

}
