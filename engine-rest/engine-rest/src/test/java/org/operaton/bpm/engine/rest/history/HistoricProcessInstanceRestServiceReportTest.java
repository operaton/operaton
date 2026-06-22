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

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.DurationReportResult;
import org.operaton.bpm.engine.history.HistoricProcessInstanceReport;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.dto.converter.ReportResultToCsvConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static org.operaton.bpm.engine.query.PeriodUnit.MONTH;
import static org.operaton.bpm.engine.query.PeriodUnit.QUARTER;
import static org.operaton.bpm.engine.rest.helper.MockProvider.*;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricProcessInstanceRestServiceReportTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORIC_PROCESS_INSTANCE_REPORT_URL = TEST_RESOURCE_ROOT_PATH + "/history/process-instance/report";

  protected HistoricProcessInstanceReport mockedReportQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedReportQuery = setUpMockHistoricProcessInstanceReportQuery();
  }

  private HistoricProcessInstanceReport setUpMockHistoricProcessInstanceReportQuery() {
    HistoricProcessInstanceReport historicProcessInstanceReport = mock(HistoricProcessInstanceReport.class);

    when(historicProcessInstanceReport.processDefinitionIdIn(anyString())).thenReturn(historicProcessInstanceReport);
    when(historicProcessInstanceReport.processDefinitionKeyIn(anyString())).thenReturn(historicProcessInstanceReport);
    when(historicProcessInstanceReport.startedAfter(any(Date.class))).thenReturn(historicProcessInstanceReport);
    when(historicProcessInstanceReport.startedBefore(any(Date.class))).thenReturn(historicProcessInstanceReport);

    List<DurationReportResult> durationReportByMonth = createMockHistoricProcessInstanceDurationReportByMonth();
    when(historicProcessInstanceReport.duration(MONTH)).thenReturn(durationReportByMonth);

    List<DurationReportResult> durationReportByQuarter = createMockHistoricProcessInstanceDurationReportByQuarter();
    when(historicProcessInstanceReport.duration(QUARTER)).thenReturn(durationReportByQuarter);

    when(historicProcessInstanceReport.duration(null)).thenThrow(new NotValidException("periodUnit is null"));

    when(processEngine.getHistoryService().createHistoricProcessInstanceReport()).thenReturn(historicProcessInstanceReport);

    return historicProcessInstanceReport;
  }

  @Test
  void testEmptyReportByMonth() {
    given()
      .queryParam("reportType", "duration")
      .queryParam("periodUnit", "month")
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verify(mockedReportQuery).duration(MONTH);
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  void testEmptyReportByQuarter() {
    given()
      .queryParam("reportType", "duration")
      .queryParam("periodUnit", "quarter")
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verify(mockedReportQuery).duration(QUARTER);
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  void testInvalidReportType() {
    given()
    .queryParam("reportType", "abc")
  .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot set query parameter 'reportType' to value 'abc'"))
    .when()
      .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testInvalidPeriodUnit() {
    given()
    .queryParam("periodUnit", "abc")
  .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot set query parameter 'periodUnit' to value 'abc'"))
    .when()
      .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testMissingReportType() {
    given()
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Unknown report type null"))
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testMissingPeriodUnit() {
    given()
      .queryParam("reportType", "duration")
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("periodUnit is null"))
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testMissingAuthorization() {
    String message = "not authorized";
    when(mockedReportQuery.duration(MONTH)).thenThrow(new AuthorizationException(message));

    given()
      .queryParam("reportType", "duration")
      .queryParam("periodUnit", "month")
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(AuthorizationException.class.getSimpleName()))
        .body("message", equalTo(message))
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testDurationReportByMonth() {
    Response response = given()
        .queryParam("periodUnit", "month")
        .queryParam("reportType", "duration")
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(ContentType.JSON)
        .when()
          .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    String content = response.asString();
    List<Map<String, Object>> reports = from(content).getList("");
    assertThat(reports.get(0)).as("The returned report should not be null.").isNotNull();

    long returnedAvg = from(content).getLong("[0].average");
    long returnedMax = from(content).getLong("[0].maximum");
    long returnedMin = from(content).getLong("[0].minimum");
    int returnedPeriod = from(content).getInt("[0].period");
    String returnedPeriodUnit = from(content).getString("[0].periodUnit");

    assertThat(returnedAvg).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_AVG);
    assertThat(returnedMax).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MAX);
    assertThat(returnedMin).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MIN);
    assertThat(returnedPeriod).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_PERIOD);
    assertThat(returnedPeriodUnit).isEqualTo(MONTH.toString());
  }

  @Test
  void testDurationReportByQuarter() {
    Response response = given()
        .queryParam("periodUnit", "quarter")
        .queryParam("reportType", "duration")
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(ContentType.JSON)
        .when()
          .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    String content = response.asString();
    List<Map<String, Object>> reports = from(content).getList("");
    assertThat(reports).as("There should be one report returned.").hasSize(1);
    assertThat(reports.get(0)).as("The returned report should not be null.").isNotNull();

    long returnedAvg = from(content).getLong("[0].average");
    long returnedMax = from(content).getLong("[0].maximum");
    long returnedMin = from(content).getLong("[0].minimum");
    int returnedPeriod = from(content).getInt("[0].period");
    String returnedPeriodUnit = from(content).getString("[0].periodUnit");

    assertThat(returnedAvg).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_AVG);
    assertThat(returnedMax).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MAX);
    assertThat(returnedMin).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MIN);
    assertThat(returnedPeriod).isEqualTo(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_PERIOD);
    assertThat(returnedPeriodUnit).isEqualTo(QUARTER.toString());
  }

  @Test
  void testListParameters() {
    String aProcDefId = "anProcDefId";
    String anotherProcDefId = "anotherProcDefId";

    String aProcDefKey = "anProcDefKey";
    String anotherProcDefKey = "anotherProcDefKey";

    given()
      .queryParam("periodUnit", "month")
      .queryParam("reportType", "duration")
      .queryParam("processDefinitionIdIn", aProcDefId + "," + anotherProcDefId)
      .queryParam("processDefinitionKeyIn", aProcDefKey + "," + anotherProcDefKey)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verify(mockedReportQuery).processDefinitionIdIn(aProcDefId, anotherProcDefId);
    verify(mockedReportQuery).processDefinitionKeyIn(aProcDefKey, anotherProcDefKey);
    verify(mockedReportQuery).duration(MONTH);
  }

  @Test
  void testHistoricBeforeAndAfterStartTimeQuery() {
    given()
      .queryParam("periodUnit", "month")
      .queryParam("reportType", "duration")
      .queryParam("startedBefore", EXAMPLE_HISTORIC_PROCESS_INSTANCE_STARTED_BEFORE)
      .queryParam("startedAfter", EXAMPLE_HISTORIC_PROCESS_INSTANCE_STARTED_AFTER)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
    .when()
      .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verifyStringStartParameterQueryInvocations();
  }

  @Test
  void testEmptyCsvReportByMonth() {
    given()
      .queryParam("reportType", "duration")
      .queryParam("periodUnit", "month")
      .accept("text/csv")
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType("text/csv")
        .header("Content-Disposition", "attachment; " +
                "filename=\"process-instance-report.csv\"; " +
                "filename*=UTF-8''process-instance-report.csv")
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verify(mockedReportQuery).duration(MONTH);
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  void testEmptyCsvReportByQuarter() {
    given()
      .queryParam("reportType", "duration")
      .queryParam("periodUnit", "quarter")
      .accept("text/csv")
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType("text/csv")
        .header("Content-Disposition", "attachment; " +
                "filename=\"process-instance-report.csv\"; " +
                "filename*=UTF-8''process-instance-report.csv")
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verify(mockedReportQuery).duration(QUARTER);
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  void testCsvInvalidReportType() {
    given()
    .queryParam("reportType", "abc")
  .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot set query parameter 'reportType' to value 'abc'"))
    .when()
      .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testCsvInvalidPeriodUnit() {
    given()
    .queryParam("periodUnit", "abc")
  .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", containsString("Cannot set query parameter 'periodUnit' to value 'abc'"))
    .when()
      .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testCsvMissingReportType() {
    given()
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("Unknown report type null"))
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testCsvMissingPeriodUnit() {
    given()
      .queryParam("reportType", "duration")
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString("periodUnit is null"))
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testCsvMissingAuthorization() {
    String message = "not authorized";
    when(mockedReportQuery.duration(MONTH)).thenThrow(new AuthorizationException(message));

    given()
      .queryParam("reportType", "duration")
      .queryParam("periodUnit", "month")
      .accept("text/csv")
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(AuthorizationException.class.getSimpleName()))
        .body("message", equalTo(message))
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);
  }

  @Test
  void testCsvDurationReportByMonth() {
    Response response = given()
        .queryParam("reportType", "duration")
        .queryParam("periodUnit", "month")
        .accept("text/csv")
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType("text/csv")
            .header("Content-Disposition", "attachment; " +
                    "filename=\"process-instance-report.csv\"; " +
                    "filename*=UTF-8''process-instance-report.csv")
        .when().get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    String responseContent = response.asString();
    assertThat(responseContent).contains(ReportResultToCsvConverter.DURATION_HEADER)
      .contains(MONTH.toString())
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_AVG))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MIN))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MAX));
  }

  @Test
  void testCsvDurationReportByQuarter() {
    Response response = given()
        .queryParam("reportType", "duration")
        .queryParam("periodUnit", "quarter")
        .accept("text/csv")
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType("text/csv")
            .header("Content-Disposition", "attachment; " +
                    "filename=\"process-instance-report.csv\"; " +
                    "filename*=UTF-8''process-instance-report.csv")
      .when().get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains(ReportResultToCsvConverter.DURATION_HEADER)
      .contains(QUARTER.toString())
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_AVG))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MIN))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MAX));
  }

  @Test
  void testApplicationCsvDurationReportByMonth() {
    Response response = given()
        .queryParam("reportType", "duration")
        .queryParam("periodUnit", "month")
        .accept("application/csv")
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType("application/csv")
            .header("Content-Disposition", "attachment; " +
                    "filename=\"process-instance-report.csv\"; " +
                    "filename*=UTF-8''process-instance-report.csv")
        .when().get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains(ReportResultToCsvConverter.DURATION_HEADER)
      .contains(MONTH.toString())
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_AVG))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MIN))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MAX));
  }

  @Test
  void testApplicationCsvDurationReportByQuarter() {
    Response response = given()
        .queryParam("reportType", "duration")
        .queryParam("periodUnit", "quarter")
        .accept("application/csv")
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType("application/csv")
            .header("Content-Disposition", "attachment; " +
                    "filename=\"process-instance-report.csv\"; " +
                    "filename*=UTF-8''process-instance-report.csv")
      .when().get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains(ReportResultToCsvConverter.DURATION_HEADER)
      .contains(QUARTER.toString())
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_AVG))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MIN))
      .contains(String.valueOf(EXAMPLE_HISTORIC_PROC_INST_DURATION_REPORT_MAX));
  }

  @Test
  void testCsvListParameters() {
    String aProcDefId = "anProcDefId";
    String anotherProcDefId = "anotherProcDefId";

    String aProcDefKey = "anProcDefKey";
    String anotherProcDefKey = "anotherProcDefKey";

    given()
      .queryParam("periodUnit", "month")
      .queryParam("reportType", "duration")
      .queryParam("processDefinitionIdIn", aProcDefId + "," + anotherProcDefId)
      .queryParam("processDefinitionKeyIn", aProcDefKey + "," + anotherProcDefKey)
      .accept("text/csv")
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType("text/csv")
            .header("Content-Disposition", "attachment; " +
                    "filename=\"process-instance-report.csv\"; " +
                    "filename*=UTF-8''process-instance-report.csv")
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verify(mockedReportQuery).processDefinitionIdIn(aProcDefId, anotherProcDefId);
    verify(mockedReportQuery).processDefinitionKeyIn(aProcDefKey, anotherProcDefKey);
    verify(mockedReportQuery).duration(MONTH);
  }

  @Test
  void testCsvHistoricBeforeAndAfterStartTimeQuery() {
    given()
      .queryParam("periodUnit", "month")
      .queryParam("reportType", "duration")
      .queryParam("startedBefore", EXAMPLE_HISTORIC_PROCESS_INSTANCE_STARTED_BEFORE)
      .queryParam("startedAfter", EXAMPLE_HISTORIC_PROCESS_INSTANCE_STARTED_AFTER)
      .accept("text/csv")
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType("text/csv")
            .header("Content-Disposition", "attachment; " +
                    "filename=\"process-instance-report.csv\"; " +
                    "filename*=UTF-8''process-instance-report.csv")
      .when()
        .get(HISTORIC_PROCESS_INSTANCE_REPORT_URL);

    verifyStringStartParameterQueryInvocations();
  }

  private Map<String, String> getCompleteStartDateAsStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("startedAfter", EXAMPLE_HISTORIC_PROCESS_INSTANCE_STARTED_AFTER);
    parameters.put("startedBefore", EXAMPLE_HISTORIC_PROCESS_INSTANCE_STARTED_BEFORE);

    return parameters;
  }

  private void verifyStringStartParameterQueryInvocations() {
    Map<String, String> startDateParameters = getCompleteStartDateAsStringQueryParameters();

    verify(mockedReportQuery).startedBefore(DateTimeUtil.parseDate(startDateParameters.get("startedBefore")));
    verify(mockedReportQuery).startedAfter(DateTimeUtil.parseDate(startDateParameters.get("startedAfter")));
  }

}
