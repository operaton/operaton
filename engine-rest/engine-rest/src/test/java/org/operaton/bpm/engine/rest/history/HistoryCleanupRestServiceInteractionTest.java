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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.BatchWindowManager;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.DefaultBatchWindowManager;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupHelper;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;
import org.operaton.bpm.engine.rest.util.DateTimeUtils;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.Job;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HistoryCleanupRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String HISTORY_CLEANUP_URL = TEST_RESOURCE_ROOT_PATH + "/history/cleanup";
  protected static final String FIND_HISTORY_CLEANUP_JOB_URL = HISTORY_CLEANUP_URL + "/job";
  protected static final String FIND_HISTORY_CLEANUP_JOBS_URL = HISTORY_CLEANUP_URL + "/jobs";
  protected static final String CONFIGURATION_URL = HISTORY_CLEANUP_URL + "/configuration";

  private HistoryService historyServiceMock;

  @BeforeEach
  void setUpRuntimeData() {
    historyServiceMock = mock(HistoryService.class);
    Job mockJob = MockProvider.createMockJob();
    List<Job> mockJobs = MockProvider.createMockJobs();
    when(historyServiceMock.cleanUpHistoryAsync(anyBoolean()))
        .thenReturn(mockJob);
    when(historyServiceMock.findHistoryCleanupJobs())
    .thenReturn(mockJobs);

    // runtime service
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);
  }

  @Test
  void testFindHistoryCleanupJob() {
    given().contentType(ContentType.JSON)
        .then()
        .expect().statusCode(Status.OK.getStatusCode())
        .when().get(FIND_HISTORY_CLEANUP_JOB_URL);

   verify(historyServiceMock).findHistoryCleanupJobs();
  }

  @Test
  void testFindNoHistoryCleanupJob() {
    when(historyServiceMock.findHistoryCleanupJobs())
        .thenReturn(emptyList());

    given().contentType(ContentType.JSON)
        .then()
        .expect().statusCode(Status.NOT_FOUND.getStatusCode())
        .when().get(FIND_HISTORY_CLEANUP_JOB_URL);

   verify(historyServiceMock).findHistoryCleanupJobs();
  }

  @Test
  void testFindHistoryCleanupJobs() {
    given().contentType(ContentType.JSON)
        .then()
        .expect().statusCode(Status.OK.getStatusCode())
        .when().get(FIND_HISTORY_CLEANUP_JOBS_URL);

   verify(historyServiceMock).findHistoryCleanupJobs();
  }

  @Test
  void testFindNoHistoryCleanupJobs() {
    when(historyServiceMock.findHistoryCleanupJobs())
        .thenReturn(null);

    given().contentType(ContentType.JSON)
        .then()
        .expect().statusCode(Status.NOT_FOUND.getStatusCode())
        .when().get(FIND_HISTORY_CLEANUP_JOBS_URL);

   verify(historyServiceMock).findHistoryCleanupJobs();
  }

  @Test
  void testHistoryCleanupImmediatelyDueDefault() {
    given().contentType(ContentType.JSON)
        .then()
        .expect().statusCode(Status.OK.getStatusCode())
        .when().post(HISTORY_CLEANUP_URL);

    verify(historyServiceMock).cleanUpHistoryAsync(true);
  }

  @Test
  void testHistoryCleanupImmediatelyDue() {
    given().contentType(ContentType.JSON)
        .queryParam("immediatelyDue", true)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().post(HISTORY_CLEANUP_URL);

    verify(historyServiceMock).cleanUpHistoryAsync(true);
  }

  @Test
  void testHistoryCleanup() {
    given().contentType(ContentType.JSON).queryParam("immediatelyDue", false)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().post(HISTORY_CLEANUP_URL);

    verify(historyServiceMock).cleanUpHistoryAsync(false);
  }

  @Test
  void testHistoryConfigurationOutsideBatchWindow() throws Exception {
    ProcessEngineConfigurationImpl processEngineConfigurationImplMock = mock(ProcessEngineConfigurationImpl.class);
    Date startDate = HistoryCleanupHelper.parseTimeConfiguration("23:59");
    Date endDate = HistoryCleanupHelper.parseTimeConfiguration("00:00");
    when(processEngine.getProcessEngineConfiguration()).thenReturn(processEngineConfigurationImplMock);
    when(processEngineConfigurationImplMock.getHistoryCleanupBatchWindowStartTime()).thenReturn("23:59");
    when(processEngineConfigurationImplMock.getHistoryCleanupBatchWindowEndTime()).thenReturn("00:00");
    when(processEngineConfigurationImplMock.getBatchWindowManager()).thenReturn(new DefaultBatchWindowManager());
    when(processEngineConfigurationImplMock.isHistoryCleanupEnabled()).thenReturn(true);

    SimpleDateFormat sdf = new SimpleDateFormat(JacksonConfigurator.getDateFormatString());
    Date now = sdf.parse("2017-09-01T22:00:00.000+0000");

    ClockUtil.setCurrentTime(now);
    Calendar today = Calendar.getInstance();
    today.setTime(now);
    Calendar tomorrow = Calendar.getInstance();
    tomorrow.setTime(DateTimeUtils.addDays(now, 1));

    Date dateToday = DateTimeUtils.updateTime(today.getTime(), startDate);
    Date dateTomorrow = DateTimeUtils.updateTime(tomorrow.getTime(), endDate);

    given()
      .contentType(ContentType.JSON)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("batchWindowStartTime", containsString(sdf.format(dateToday)))
      .body("batchWindowEndTime", containsString(sdf.format(dateTomorrow)))
      .body("enabled", equalTo(true))
    .when()
      .get(CONFIGURATION_URL);

  }

  @Test
  void testHistoryConfigurationWithinBatchWindow() throws Exception {
    ProcessEngineConfigurationImpl processEngineConfigurationImplMock = mock(ProcessEngineConfigurationImpl.class);
    Date startDate = HistoryCleanupHelper.parseTimeConfiguration("22:00+0200");
    Date endDate = HistoryCleanupHelper.parseTimeConfiguration("23:00+0200");
    when(processEngine.getProcessEngineConfiguration()).thenReturn(processEngineConfigurationImplMock);
    when(processEngineConfigurationImplMock.getHistoryCleanupBatchWindowStartTime()).thenReturn("22:00+0200");
    when(processEngineConfigurationImplMock.getHistoryCleanupBatchWindowEndTime()).thenReturn("23:00+0200");
    when(processEngineConfigurationImplMock.getBatchWindowManager()).thenReturn(new DefaultBatchWindowManager());
    when(processEngineConfigurationImplMock.isHistoryCleanupEnabled()).thenReturn(true);

    SimpleDateFormat sdf = new SimpleDateFormat(JacksonConfigurator.getDateFormatString());
    Date now = sdf.parse("2017-09-01T22:00:00.000+0200");
    ClockUtil.setCurrentTime(now);

    Calendar today = Calendar.getInstance();
    today.setTime(now);

    Date dateToday = DateTimeUtils.updateTime(today.getTime(), startDate);
    Date dateTomorrow = DateTimeUtils.updateTime(today.getTime(), endDate);

    given()
      .contentType(ContentType.JSON)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("batchWindowStartTime", containsString(sdf.format(dateToday)))
      .body("batchWindowEndTime", containsString(sdf.format(dateTomorrow)))
      .body("enabled", equalTo(true))
    .when()
      .get(CONFIGURATION_URL);

  }

  @Test
  void testHistoryConfigurationWhenBatchNotDefined() {
    ProcessEngineConfigurationImpl processEngineConfigurationImplMock = mock(ProcessEngineConfigurationImpl.class);
    when(processEngine.getProcessEngineConfiguration()).thenReturn(processEngineConfigurationImplMock);
    when(processEngineConfigurationImplMock.getHistoryCleanupBatchWindowStartTime()).thenReturn(null);
    when(processEngineConfigurationImplMock.getHistoryCleanupBatchWindowEndTime()).thenReturn(null);
    when(processEngineConfigurationImplMock.isHistoryCleanupEnabled()).thenReturn(true);
    when(processEngineConfigurationImplMock.getBatchWindowManager()).thenReturn(new DefaultBatchWindowManager());

    given()
      .contentType(ContentType.JSON)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("batchWindowStartTime", equalTo(null))
      .body("batchWindowEndTime", equalTo(null))
      .body("enabled", equalTo(true))
    .when()
      .get(CONFIGURATION_URL);

  }

  @Test
  void shouldReturnEnabledFalse() {
    ProcessEngineConfigurationImpl engineConfigMock = mock(ProcessEngineConfigurationImpl.class);
    when(processEngine.getProcessEngineConfiguration()).thenReturn(engineConfigMock);

    BatchWindowManager batchWindowManager = mock(BatchWindowManager.class);
    when(engineConfigMock.getBatchWindowManager()).thenReturn(batchWindowManager);
    when(engineConfigMock.isHistoryCleanupEnabled()).thenReturn(false);

    given()
      .contentType(ContentType.JSON)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("batchWindowStartTime", equalTo(null))
      .body("batchWindowEndTime", equalTo(null))
      .body("enabled", equalTo(false))
    .when()
      .get(CONFIGURATION_URL);
  }

}
