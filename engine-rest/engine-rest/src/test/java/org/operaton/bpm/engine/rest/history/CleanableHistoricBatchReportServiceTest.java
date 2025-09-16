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

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReport;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CleanableHistoricBatchReportServiceTest extends AbstractRestServiceTest {

  private static final String EXAMPLE_TYPE = "batchId1";
  private static final int EXAMPLE_TTL = 5;
  private static final long EXAMPLE_FINISHED_COUNT = 10L;
  private static final long EXAMPLE_CLEANABLE_COUNT = 5L;

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORY_URL = TEST_RESOURCE_ROOT_PATH + "/history/batch";
  protected static final String HISTORIC_REPORT_URL = HISTORY_URL + "/cleanable-batch-report";
  protected static final String HISTORIC_REPORT_COUNT_URL = HISTORIC_REPORT_URL + "/count";

  private CleanableHistoricBatchReport historicBatchReport;

  @BeforeEach
  void setUpRuntimeData() {
    setupHistoryReportMock();
  }

  private void setupHistoryReportMock() {
    CleanableHistoricBatchReport report = mock(CleanableHistoricBatchReport.class);

    CleanableHistoricBatchReportResult reportResult = mock(CleanableHistoricBatchReportResult.class);

    when(reportResult.getBatchType()).thenReturn(EXAMPLE_TYPE);
    when(reportResult.getHistoryTimeToLive()).thenReturn(EXAMPLE_TTL);
    when(reportResult.getFinishedBatchesCount()).thenReturn(EXAMPLE_FINISHED_COUNT);
    when(reportResult.getCleanableBatchesCount()).thenReturn(EXAMPLE_CLEANABLE_COUNT);

    CleanableHistoricBatchReportResult anotherReportResult = mock(CleanableHistoricBatchReportResult.class);

    when(anotherReportResult.getBatchType()).thenReturn("batchId2");
    when(anotherReportResult.getHistoryTimeToLive()).thenReturn(null);
    when(anotherReportResult.getFinishedBatchesCount()).thenReturn(13L);
    when(anotherReportResult.getCleanableBatchesCount()).thenReturn(0L);

    List<CleanableHistoricBatchReportResult> mocks = new ArrayList<>();
    mocks.add(reportResult);
    mocks.add(anotherReportResult);

    when(report.list()).thenReturn(mocks);
    when(report.count()).thenReturn((long) mocks.size());

    historicBatchReport = report;
    when(processEngine.getHistoryService().createCleanableHistoricBatchReport()).thenReturn(historicBatchReport);
  }

  @Test
  void testGetReport() {
    given()
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
    .when().get(HISTORIC_REPORT_URL);

    InOrder inOrder = Mockito.inOrder(historicBatchReport);
    inOrder.verify(historicBatchReport).list();
  }

  @Test
  void testReportRetrieval() {
    Response response = given()
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
    .when().get(HISTORIC_REPORT_URL);

    // assert query invocation
    InOrder inOrder = Mockito.inOrder(historicBatchReport);
    inOrder.verify(historicBatchReport).list();

    String content = response.asString();
    List<Map<String, Object>> reportResults = from(content).getList("");
    assertEquals(2, reportResults.size(), "There should be two report results returned.");
    assertThat(reportResults.get(0)).isNotNull();

    String returnedBatchType = from(content).getString("[0].batchType");
    int returnedTTL = from(content).getInt("[0].historyTimeToLive");
    long returnedFinishedCount= from(content).getLong("[0].finishedBatchesCount");
    long returnedCleanableCount = from(content).getLong("[0].cleanableBatchesCount");

    assertEquals(EXAMPLE_TYPE, returnedBatchType);
    assertEquals(EXAMPLE_TTL, returnedTTL);
    assertEquals(EXAMPLE_FINISHED_COUNT, returnedFinishedCount);
    assertEquals(EXAMPLE_CLEANABLE_COUNT, returnedCleanableCount);
  }

  @Test
  void testMissingAuthorization() {
    String message = "not authorized";
    when(historicBatchReport.list()).thenThrow(new AuthorizationException(message));

    given()
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when().get(HISTORIC_REPORT_URL);
  }

  @Test
  void testQueryCount() {
    expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(2))
    .when()
      .get(HISTORIC_REPORT_COUNT_URL);

    verify(historicBatchReport).count();
  }

  @Test
  void testOrderByFinishedBatchOperationAsc() {
    given()
      .queryParam("sortBy", "finished")
      .queryParam("sortOrder", "asc")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_REPORT_URL);

    verify(historicBatchReport).orderByFinishedBatchOperation();
    verify(historicBatchReport).asc();
  }

  @Test
  void testOrderByFinishedBatchOperationDesc() {
    given()
      .queryParam("sortBy", "finished")
      .queryParam("sortOrder", "desc")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(HISTORIC_REPORT_URL);

    verify(historicBatchReport).orderByFinishedBatchOperation();
    verify(historicBatchReport).desc();
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
      .get(HISTORIC_REPORT_URL);
  }

  @Test
  void testInvalidSortingOptions() {
    executeAndVerifySorting("anInvalidSortByOption", "asc", Status.BAD_REQUEST);
    executeAndVerifySorting("finished", "anInvalidSortOrderOption", Status.BAD_REQUEST);
  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given()
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder)
    .then()
      .expect()
        .statusCode(expectedStatus.getStatusCode())
      .when()
        .get(HISTORIC_REPORT_URL);
  }
}
