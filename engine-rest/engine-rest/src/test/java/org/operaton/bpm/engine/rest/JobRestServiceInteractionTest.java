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

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.management.SetJobRetriesBuilder;
import org.operaton.bpm.engine.management.SetJobRetriesByJobsAsyncBuilder;
import org.operaton.bpm.engine.management.UpdateJobSuspensionStateSelectBuilder;
import org.operaton.bpm.engine.management.UpdateJobSuspensionStateTenantBuilder;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobSuspensionStateDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.MockJobBuilder;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.JsonPathUtil;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JobRestServiceInteractionTest extends AbstractRestServiceTest {

  private static final String RETRIES = "retries";
  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String JOB_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/job";
  protected static final String SINGLE_JOB_RESOURCE_URL = JOB_RESOURCE_URL + "/{id}";
  protected static final String JOB_RESOURCE_SET_RETRIES_URL = SINGLE_JOB_RESOURCE_URL + "/retries";
  protected static final String JOBS_SET_RETRIES_URL = JOB_RESOURCE_URL + "/retries";
  protected static final String JOB_RESOURCE_SET_PRIORITY_URL = SINGLE_JOB_RESOURCE_URL + "/priority";
  protected static final String JOB_RESOURCE_EXECUTE_JOB_URL = SINGLE_JOB_RESOURCE_URL + "/execute";
  protected static final String JOB_RESOURCE_GET_STACKTRACE_URL = SINGLE_JOB_RESOURCE_URL + "/stacktrace";
  protected static final String JOB_RESOURCE_SET_DUEDATE_URL = SINGLE_JOB_RESOURCE_URL + "/duedate";
  protected static final String JOB_RESOURCE_RECALC_DUEDATE_URL = JOB_RESOURCE_SET_DUEDATE_URL + "/recalculate";
  protected static final String SINGLE_JOB_SUSPENDED_URL = SINGLE_JOB_RESOURCE_URL + "/suspended";
  protected static final String JOB_SUSPENDED_URL = JOB_RESOURCE_URL + "/suspended";

  private ProcessEngine namedProcessEngine;
  private ManagementService mockManagementService;

  private UpdateJobSuspensionStateTenantBuilder mockSuspensionStateBuilder;
  private UpdateJobSuspensionStateSelectBuilder mockSuspensionStateSelectBuilder;

  private JobQuery mockQuery;
  private SetJobRetriesByJobsAsyncBuilder mockSetJobRetriesByJobsAsyncBuilder;
  private SetJobRetriesBuilder mockSetJobRetriesBuilder;

  @BeforeEach
  void setUpRuntimeData() {

    mockQuery = mock(JobQuery.class);
    Job mockedJob = new MockJobBuilder()
      .id(MockProvider.EXAMPLE_JOB_ID)
      .processInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .executionId(MockProvider.EXAMPLE_EXECUTION_ID)
      .retries(MockProvider.EXAMPLE_JOB_RETRIES)
      .exceptionMessage(MockProvider.EXAMPLE_JOB_NO_EXCEPTION_MESSAGE)
      .failedActivityId(MockProvider.EXAMPLE_JOB_FAILED_ACTIVITY_ID)
      .dueDate(new Date())
      .priority(MockProvider.EXAMPLE_JOB_PRIORITY)
      .jobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .tenantId(MockProvider.EXAMPLE_TENANT_ID)
      .createTime(DateTimeUtil.parseDate(MockProvider.EXAMPLE_JOB_CREATE_TIME))
      .batchId(MockProvider.EXAMPLE_BATCH_ID)
      .build();

    when(mockQuery.singleResult()).thenReturn(mockedJob);
    when(mockQuery.jobId(MockProvider.EXAMPLE_JOB_ID)).thenReturn(mockQuery);

    mockManagementService = mock(ManagementService.class);
    when(mockManagementService.createJobQuery()).thenReturn(mockQuery);

    mockSetJobRetriesByJobsAsyncBuilder = MockProvider.createMockSetJobRetriesByJobsAsyncBuilder(mockManagementService);

    mockSetJobRetriesBuilder = MockProvider.createMockSetJobRetriesBuilder(mockManagementService);

    mockSuspensionStateSelectBuilder = mock(UpdateJobSuspensionStateSelectBuilder.class);
    when(mockManagementService.updateJobSuspensionState()).thenReturn(mockSuspensionStateSelectBuilder);

    mockSuspensionStateBuilder = mock(UpdateJobSuspensionStateTenantBuilder.class);
    when(mockSuspensionStateSelectBuilder.byJobId(anyString())).thenReturn(mockSuspensionStateBuilder);
    when(mockSuspensionStateSelectBuilder.byJobDefinitionId(anyString())).thenReturn(mockSuspensionStateBuilder);
    when(mockSuspensionStateSelectBuilder.byProcessInstanceId(anyString())).thenReturn(mockSuspensionStateBuilder);
    when(mockSuspensionStateSelectBuilder.byProcessDefinitionId(anyString())).thenReturn(mockSuspensionStateBuilder);
    when(mockSuspensionStateSelectBuilder.byProcessDefinitionKey(anyString())).thenReturn(mockSuspensionStateBuilder);

    namedProcessEngine = getProcessEngine(MockProvider.EXAMPLE_PROCESS_ENGINE_NAME);
    when(namedProcessEngine.getManagementService()).thenReturn(mockManagementService);
  }

  @Test
  void testSetRetries() {
    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_RESOURCE_SET_RETRIES_URL);

    verify(mockManagementService).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder).jobId(MockProvider.EXAMPLE_JOB_ID);
    verify(mockSetJobRetriesBuilder).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetRetriesWithDueDate() {
    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);
    Date newDueDate = new Date(1675752840000L);
    retriesVariableJson.put("dueDate", newDueDate);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson).then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_RESOURCE_SET_RETRIES_URL);

    verify(mockManagementService).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder).jobId(MockProvider.EXAMPLE_JOB_ID);
    verify(mockSetJobRetriesBuilder).dueDate(newDueDate);
    verify(mockSetJobRetriesBuilder).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetRetriesWithNullDueDate() {
    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);
    retriesVariableJson.put("dueDate", null);

    given()
    .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .contentType(ContentType.JSON)
    .body(retriesVariableJson).then().expect()
    .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
    .put(JOB_RESOURCE_SET_RETRIES_URL);

    verify(mockManagementService).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder).jobId(MockProvider.EXAMPLE_JOB_ID);
    verify(mockSetJobRetriesBuilder).dueDate(null);
    verify(mockSetJobRetriesBuilder).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetRetriesNonExistentJob() {
    String expectedMessage = "No job found with id '" + MockProvider.NON_EXISTING_JOB_ID + "'.";

    doThrow(new ProcessEngineException(expectedMessage)).when(mockSetJobRetriesBuilder).execute();

    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson)
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_RESOURCE_SET_RETRIES_URL);

    verify(mockManagementService).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder).jobId(MockProvider.NON_EXISTING_JOB_ID);
    verify(mockSetJobRetriesBuilder).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetRetriesNegativeRetries() {

    String expectedMessage = "The number of job retries must be a non-negative Integer, but '" + MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES
        + "' has been provided.";

    doThrow(new ProcessEngineException(expectedMessage)).when(mockManagementService).setJobRetries(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);

    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson)
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_RESOURCE_SET_RETRIES_URL);

    verify(mockManagementService).setJobRetries(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetRetriesThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(mockManagementService).setJobRetries(anyInt());

    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON).body(retriesVariableJson)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .put(JOB_RESOURCE_SET_RETRIES_URL);

    verify(mockManagementService).setJobRetries(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSimpleJobGet() {
    given().pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("id", equalTo(MockProvider.EXAMPLE_JOB_ID))
      .body("processInstanceId", equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .body("executionId", equalTo(MockProvider.EXAMPLE_EXECUTION_ID))
      .body("exceptionMessage", equalTo(MockProvider.EXAMPLE_JOB_NO_EXCEPTION_MESSAGE))
      .body("failedActivityId", equalTo(MockProvider.EXAMPLE_JOB_FAILED_ACTIVITY_ID))
      .body("priority", equalTo(MockProvider.EXAMPLE_JOB_PRIORITY))
      .body("jobDefinitionId", equalTo(MockProvider.EXAMPLE_JOB_DEFINITION_ID))
      .body("tenantId", equalTo(MockProvider.EXAMPLE_TENANT_ID))
      .body("createTime", equalTo(MockProvider.EXAMPLE_JOB_CREATE_TIME))
      .body("batchId", equalTo(MockProvider.EXAMPLE_BATCH_ID))
    .when()
      .get(SINGLE_JOB_RESOURCE_URL);

    InOrder inOrder = inOrder(mockQuery);
    inOrder.verify(mockQuery).jobId(MockProvider.EXAMPLE_JOB_ID);
    inOrder.verify(mockQuery).singleResult();
  }

  @Test
  void testJobGetIdDoesntExist() {
    JobQuery invalidQueryNonExistingJob;
    invalidQueryNonExistingJob = mock(JobQuery.class);
    when(mockManagementService.createJobQuery().jobId(MockProvider.NON_EXISTING_JOB_ID)).thenReturn(invalidQueryNonExistingJob);
    when(invalidQueryNonExistingJob.singleResult()).thenReturn(null);

    String jobId = MockProvider.NON_EXISTING_JOB_ID;

    given()
      .pathParam("id", jobId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Job with id " + jobId + " does not exist"))
    .when()
      .get(SINGLE_JOB_RESOURCE_URL);
  }

  @Test
  void testExecuteJob() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(JOB_RESOURCE_EXECUTE_JOB_URL);

    verify(mockManagementService).executeJob(MockProvider.EXAMPLE_JOB_ID);
  }

  @Test
  void testExecuteJobIdDoesntExist() {
    String jobId = MockProvider.NON_EXISTING_JOB_ID;

    String expectedMessage = "No job found with id '" + jobId + "'";

    doThrow(new ProcessEngineException(expectedMessage)).when(mockManagementService).executeJob(MockProvider.NON_EXISTING_JOB_ID);

    given().pathParam("id", jobId)
    .then().expect().statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
    .body("type", equalTo(InvalidRequestException.class.getSimpleName())).body("message", equalTo(expectedMessage))
    .when().post(JOB_RESOURCE_EXECUTE_JOB_URL);
  }

  @Test
  void testExecuteJobRuntimeException() {
    String jobId = MockProvider.EXAMPLE_JOB_ID;

    doThrow(new RuntimeException("Runtime exception")).when(mockManagementService).executeJob(jobId);

    given().pathParam("id", jobId)
    .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
    .body("type", equalTo(RestException.class.getSimpleName())).body("message", equalTo("Runtime exception"))
    .when().post(JOB_RESOURCE_EXECUTE_JOB_URL);
  }

  @Test
  void testExecuteJobThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(mockManagementService).executeJob(anyString());

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .post(JOB_RESOURCE_EXECUTE_JOB_URL);
  }

  @Test
  void testGetStacktrace() {
    String stacktrace = "aStacktrace";
    when(mockManagementService.getJobExceptionStacktrace(MockProvider.EXAMPLE_JOB_ID)).thenReturn(stacktrace);

    Response response = given().pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect().statusCode(Status.OK.getStatusCode()).contentType(ContentType.TEXT)
    .when().get(JOB_RESOURCE_GET_STACKTRACE_URL);

    String content = response.asString();
    assertThat(content).isEqualTo(stacktrace);
  }

  @Test
  void testGetStacktraceJobNotFound() {
    String exceptionMessage = "job not found";
    doThrow(new ProcessEngineException(exceptionMessage)).when(mockManagementService).getJobExceptionStacktrace(MockProvider.EXAMPLE_JOB_ID);

    given().pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect().statusCode(Status.NOT_FOUND.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo(exceptionMessage))
    .when().get(JOB_RESOURCE_GET_STACKTRACE_URL);
  }

  @Test
  void testGetStacktraceJobThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(mockManagementService).getJobExceptionStacktrace(MockProvider.EXAMPLE_JOB_ID);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .get(JOB_RESOURCE_GET_STACKTRACE_URL);
  }

  @Test
  void testSetJobDuedate() {
    Date newDuedate = MockProvider.createMockDuedate();
    Map<String, Object> duedateVariableJson = new HashMap<>();
    duedateVariableJson.put("duedate", newDuedate);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(duedateVariableJson)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_RESOURCE_SET_DUEDATE_URL);

    verify(mockManagementService).setJobDuedate(MockProvider.EXAMPLE_JOB_ID, newDuedate, false);
  }

  @Test
  void testSetJobDuedateNull() {
    Map<String, Object> duedateVariableJson = new HashMap<>();
    duedateVariableJson.put("duedate", null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(duedateVariableJson)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_RESOURCE_SET_DUEDATE_URL);

    verify(mockManagementService).setJobDuedate(MockProvider.EXAMPLE_JOB_ID, null, false);
  }

  @Test
  void testSetJobDuedateCascade() {
    Date newDuedate = MockProvider.createMockDuedate();
    Map<String, Object> duedateVariableJson = new HashMap<>();
    duedateVariableJson.put("duedate", newDuedate);
    duedateVariableJson.put("cascade", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(duedateVariableJson)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_RESOURCE_SET_DUEDATE_URL);

    verify(mockManagementService).setJobDuedate(MockProvider.EXAMPLE_JOB_ID, newDuedate, true);
  }

  @Test
  void testSetJobDuedateNullCascade() {
    Map<String, Object> duedateVariableJson = new HashMap<>();
    duedateVariableJson.put("duedate", null);
    duedateVariableJson.put("cascade", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(duedateVariableJson)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_RESOURCE_SET_DUEDATE_URL);

    verify(mockManagementService).setJobDuedate(MockProvider.EXAMPLE_JOB_ID, null, true);
  }

  @Test
  void testSetJobDuedateNonExistentJob() {
    Date newDuedate = MockProvider.createMockDuedate();
    String expectedMessage = "No job found with id '" + MockProvider.NON_EXISTING_JOB_ID + "'.";

    doThrow(new ProcessEngineException(expectedMessage)).when(mockManagementService).setJobDuedate(MockProvider.NON_EXISTING_JOB_ID,
        newDuedate, false);

    Map<String, Object> duedateVariableJson = new HashMap<>();
    duedateVariableJson.put("duedate", newDuedate);

    given().pathParam("id", MockProvider.NON_EXISTING_JOB_ID).contentType(ContentType.JSON)
    .body(duedateVariableJson).then().expect()
    .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
    .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
    .body("message", equalTo(expectedMessage))
    .when().put(JOB_RESOURCE_SET_DUEDATE_URL);

    verify(mockManagementService).setJobDuedate(MockProvider.NON_EXISTING_JOB_ID, newDuedate, false);
  }

  @Test
  void testSetJobDuedateThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(mockManagementService).setJobDuedate(anyString(), any(Date.class), anyBoolean());

    Date newDuedate = MockProvider.createMockDuedate();
    Map<String, Object> duedateVariableJson = new HashMap<>();
    duedateVariableJson.put("duedate", newDuedate);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(duedateVariableJson)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .put(JOB_RESOURCE_SET_DUEDATE_URL);
  }

  @Test
  void testActivateJob() {
    JobSuspensionStateDto dto = new JobSuspensionStateDto();
    dto.setSuspended(false);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobId(MockProvider.EXAMPLE_JOB_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateThrowsProcessEngineException() {
    JobSuspensionStateDto dto = new JobSuspensionStateDto();
    dto.setSuspended(false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateThrowsAuthorizationException() {
    JobSuspensionStateDto dto = new JobSuspensionStateDto();
    dto.setSuspended(false);

    String expectedMessage = "expectedMessage";

    doThrow(new AuthorizationException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJob() {
    JobSuspensionStateDto dto = new JobSuspensionStateDto();
    dto.setSuspended(true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobId(MockProvider.EXAMPLE_JOB_ID);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendedThrowsProcessEngineException() {
    JobSuspensionStateDto dto = new JobSuspensionStateDto();
    dto.setSuspended(true);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendWithMultipleByParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("jobDefinitionId", MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "Only one of jobId, jobDefinitionId, processInstanceId, processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(SINGLE_JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendThrowsAuthorizationException() {
    JobSuspensionStateDto dto = new JobSuspensionStateDto();
    dto.setSuspended(true);

    String expectedMessage = "expectedMessage";

    doThrow(new AuthorizationException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByProcessDefinitionKey() {
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
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobByProcessDefinitionKeyWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByProcessDefinitionKeyThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByProcessDefinitionKey() {
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
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobByProcessDefinitionKeyWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByProcessDefinitionKeyThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByProcessDefinitionKeyAndTenantId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("processDefinitionTenantId", MockProvider.EXAMPLE_TENANT_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionTenantId(MockProvider.EXAMPLE_TENANT_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobByProcessDefinitionKeyWithoutTenantId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("processDefinitionWithoutTenantId", true);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionWithoutTenantId();
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testSuspendJobByProcessDefinitionKeyAndTenantId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("processDefinitionTenantId", MockProvider.EXAMPLE_TENANT_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionTenantId(MockProvider.EXAMPLE_TENANT_ID);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobByProcessDefinitionKeyWithoutTenantId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("processDefinitionWithoutTenantId", true);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionWithoutTenantId();
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testActivateJobByProcessDefinitionId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobByProcessDefinitionIdWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByProcessDefinitionIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByProcessDefinitionId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobByProcessDefinitionIdWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByProcessDefinitionIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByProcessInstanceId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobByProcessInstanceIdWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByProcessInstanceIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByProcessInstanceId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobByProcessInstanceIdWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByProcessInstanceIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByJobDefinitionId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("jobDefinitionId", MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobByJobDefinitionIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("jobDefinitionId", MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByJobDefinitionId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("jobDefinitionId", MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobByJobDefinitionIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("jobDefinitionId", MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new AuthorizationException(expectedException))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedException))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testActivateJobByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("jobId", MockProvider.EXAMPLE_JOB_ID);

    String message = "Either jobDefinitionId, processInstanceId, processDefinitionId or processDefinitionKey can be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("jobId", MockProvider.EXAMPLE_JOB_ID);

    String message = "Either jobDefinitionId, processInstanceId, processDefinitionId or processDefinitionKey can be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobByNothing() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);

    String message = "Either jobId, jobDefinitionId, processInstanceId, processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_SUSPENDED_URL);
  }

  @Test
  void testSetJobPriority() {
    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_PRIORITY);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when().put(JOB_RESOURCE_SET_PRIORITY_URL);

    verify(mockManagementService).setJobPriority(MockProvider.EXAMPLE_JOB_ID, MockProvider.EXAMPLE_JOB_PRIORITY);
  }

  @Test
  void testSetJobPriorityToExtremeValue() {
    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", Long.MAX_VALUE);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when().put(JOB_RESOURCE_SET_PRIORITY_URL);

    verify(mockManagementService).setJobPriority(MockProvider.EXAMPLE_JOB_ID, Long.MAX_VALUE);
  }

  @Test
  void testSetJobPriorityNonExistentJob() {
    String expectedMessage = "No job found with id '" + MockProvider.NON_EXISTING_JOB_ID + "'.";

    doThrow(new NotFoundException(expectedMessage))
      .when(mockManagementService).setJobPriority(MockProvider.NON_EXISTING_JOB_ID, MockProvider.EXAMPLE_JOB_PRIORITY);

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_PRIORITY);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo(expectedMessage))
    .when().put(JOB_RESOURCE_SET_PRIORITY_URL);

    verify(mockManagementService).setJobPriority(MockProvider.NON_EXISTING_JOB_ID, MockProvider.EXAMPLE_JOB_PRIORITY);
  }

  @Test
  void testSetJobPriorityFailure() {
    String expectedMessage = "No job found with id '" + MockProvider.EXAMPLE_JOB_ID + "'.";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockManagementService).setJobPriority(MockProvider.EXAMPLE_JOB_ID, MockProvider.EXAMPLE_JOB_PRIORITY);

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_PRIORITY);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", equalTo(expectedMessage))
    .when().put(JOB_RESOURCE_SET_PRIORITY_URL);

    verify(mockManagementService).setJobPriority(MockProvider.EXAMPLE_JOB_ID, MockProvider.EXAMPLE_JOB_PRIORITY);
  }

  @Test
  void testSetNullJobPriorityFailure() {
    String expectedMessage = "Priority for job '" +  MockProvider.EXAMPLE_JOB_ID + "' cannot be null.";

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", equalTo(expectedMessage))
    .when().put(JOB_RESOURCE_SET_PRIORITY_URL);

    verifyNoMoreInteractions(mockManagementService);
  }

  @Test
  void testSetJobPriorityThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message))
      .when(mockManagementService).setJobPriority(any(), anyLong());

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_PRIORITY);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", equalTo(message))
    .when()
      .put(JOB_RESOURCE_SET_PRIORITY_URL);
  }

  @Test
  void deleteJob() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_JOB_RESOURCE_URL);

    verify(mockManagementService).deleteJob(MockProvider.EXAMPLE_JOB_ID);
    verifyNoMoreInteractions(mockManagementService);
  }

  @Test
  void deleteNotExistingJob() {
    String jobId = MockProvider.NON_EXISTING_JOB_ID;

    String expectedMessage = "No job found with id '" + jobId + "'.";

    doThrow(new NullValueException(expectedMessage))
      .when(mockManagementService).deleteJob(jobId);

    given()
      .pathParam("id", jobId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo(expectedMessage))
    .when()
      .delete(SINGLE_JOB_RESOURCE_URL);

    verify(mockManagementService).deleteJob(jobId);
    verifyNoMoreInteractions(mockManagementService);
  }

  @Test
  void deleteLockedJob() {
    String jobId = MockProvider.EXAMPLE_JOB_ID;

    String expectedMessage = "Cannot delete job when the job is being executed. Try again later.";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockManagementService).deleteJob(jobId);

    given()
      .pathParam("id", jobId)
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", equalTo(RestException.class.getSimpleName()))
      .body("message", equalTo(expectedMessage))
    .when()
      .delete(SINGLE_JOB_RESOURCE_URL);

    verify(mockManagementService).deleteJob(jobId);
    verifyNoMoreInteractions(mockManagementService);
  }

  @Test
  void deleteJobThrowAuthorizationException() {
    String jobId = MockProvider.EXAMPLE_JOB_ID;

    String expectedMessage = "Missing permissions";

    doThrow(new AuthorizationException(expectedMessage))
      .when(mockManagementService).deleteJob(jobId);

    given()
      .pathParam("id", jobId)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", is(AuthorizationException.class.getSimpleName()))
      .body("message", is(expectedMessage))
    .when()
      .delete(SINGLE_JOB_RESOURCE_URL);

    verify(mockManagementService).deleteJob(jobId);
    verifyNoMoreInteractions(mockManagementService);
  }

  @Test
  void testSetRetriesByJobsAsync() {
    List<String> ids = List.of(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("jobIds", ids);
    messageBodyJson.put(RETRIES, 5);

    Response response =
        given()
          .contentType(ContentType.JSON)
          .body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(JOBS_SET_RETRIES_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByJobsAsync(5);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobIds(ids);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobQuery(null);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByJobsAsyncBuilder);
  }

  @Test
  void testSetRetriesAsyncWithDueDate() {
    List<String> ids = List.of(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("jobIds", ids);
    Date newDueDate = new Date(1675752840000L);
    messageBodyJson.put("dueDate", newDueDate);
    messageBodyJson.put(RETRIES, 5);

    Response response =
        given()
          .contentType(ContentType.JSON)
          .body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(JOBS_SET_RETRIES_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByJobsAsync(5);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobIds(ids);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobQuery(null);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).dueDate(newDueDate);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByJobsAsyncBuilder);
  }

  @Test
  void testSetRetriesAsyncWithNullDueDate() {
    List<String> ids = List.of(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("jobIds", ids);
    messageBodyJson.put("dueDate", null);
    messageBodyJson.put(RETRIES, 5);

    Response response =
        given()
          .contentType(ContentType.JSON)
          .body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(JOBS_SET_RETRIES_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByJobsAsync(5);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobIds(ids);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobQuery(null);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).dueDate(null);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByJobsAsyncBuilder);
  }

  @Test
  void testSetRetriesAsyncWithQuery() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, 5);
    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    messageBodyJson.put("jobQuery", query);

    Response response =
        given()
          .contentType(ContentType.JSON)
          .body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(JOBS_SET_RETRIES_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByJobsAsync(5);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobIds(null);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobQuery(any(JobQuery.class));
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByJobsAsyncBuilder);
  }


  @Test
  void testSetRetriesAsyncWithCreateTimesQuery() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, 5);
    Map<String, Object> condition = new HashMap<>();
    condition.put("operator", "lt");
    condition.put("value", "2022-12-15T10:45:00.000+0100");
    Map<String, Object> jobQueryDto = new HashMap<>();
    jobQueryDto.put("createTimes", List.of(condition));
    messageBodyJson.put("jobQuery", jobQueryDto);

    Response response = given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(JOBS_SET_RETRIES_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByJobsAsync(5);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobIds(null);
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobQuery(any(JobQuery.class));
    verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByJobsAsyncBuilder);
  }


  @Test
  void testSetRetriesAsyncWithDueDatesQuery() {
      Map<String, Object> messageBodyJson = new HashMap<>();
      messageBodyJson.put(RETRIES, 5);
      Map<String, Object> condition = new HashMap<>();
      condition.put("operator", "lt");
      condition.put("value", "2022-12-15T10:45:00.000+0100");
      Map<String, Object> jobQueryDto = new HashMap<>();
      jobQueryDto.put("dueDates", List.of(condition));
      messageBodyJson.put("jobQuery", jobQueryDto);

      Response response = given()
          .contentType(ContentType.JSON).body(messageBodyJson)
          .then().expect()
          .statusCode(Status.OK.getStatusCode())
          .when().post(JOBS_SET_RETRIES_URL);

      verifyBatchJson(response.asString());

      verify(mockManagementService, times(1)).setJobRetriesByJobsAsync(5);
      verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobIds(null);
      verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).jobQuery(any(JobQuery.class));
      verify(mockSetJobRetriesByJobsAsyncBuilder, times(1)).executeAsync();
      verifyNoMoreInteractions(mockSetJobRetriesByJobsAsyncBuilder);
  }


  @Test
  void testSetRetriesWithBadRequestQuery() {
    doThrow(new BadUserRequestException("job ids are empty"))
        .when(mockSetJobRetriesByJobsAsyncBuilder).jobQuery((JobQuery) null);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, 5);

    given()
      .contentType(ContentType.JSON).body(messageBodyJson)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(JOBS_SET_RETRIES_URL);
  }

  @Test
  void testSetRetriesWithoutBody() {
    given()
        .contentType(ContentType.JSON)
        .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when().post(JOBS_SET_RETRIES_URL);

    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetRetriesWithNegativeRetries() {
    doThrow(new BadUserRequestException("retries are negative"))
        .when(mockManagementService).setJobRetriesByJobsAsync(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);
    JobQueryDto query = new JobQueryDto();
    messageBodyJson.put("jobQuery", query);

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
      .when()
        .post(JOBS_SET_RETRIES_URL);
  }

  @Test
  void testSetRetriesWithoutRetries() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("jobIds", null);

    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(JOBS_SET_RETRIES_URL);

    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testRecalculateDuedateWithoutDateBase() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when().post(JOB_RESOURCE_RECALC_DUEDATE_URL);

    verify(mockManagementService).recalculateJobDuedate(MockProvider.EXAMPLE_JOB_ID, true);
  }

  @Test
  void testRecalculateDuedateCreationDateBased() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .queryParam("creationDateBased", true)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when().post(JOB_RESOURCE_RECALC_DUEDATE_URL);

    verify(mockManagementService).recalculateJobDuedate(MockProvider.EXAMPLE_JOB_ID, true);
  }

  @Test
  void testRecalculateDuedateCurrentDateBased() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_ID)
      .queryParam("creationDateBased", false)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when().post(JOB_RESOURCE_RECALC_DUEDATE_URL);

    verify(mockManagementService).recalculateJobDuedate(MockProvider.EXAMPLE_JOB_ID, false);
  }

  @Test
  void testRecalculateDuedateWithUnknownJobId() {
    String jobId = MockProvider.NON_EXISTING_JOB_ID;

    String expectedMessage = "No job found with id '" + jobId + "'.";

    doThrow(new NotFoundException(expectedMessage))
      .when(mockManagementService).recalculateJobDuedate(jobId, true);

    given()
      .pathParam("id", jobId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
      .body("type", is(InvalidRequestException.class.getSimpleName()))
      .body("message", is(expectedMessage))
    .when().post(JOB_RESOURCE_RECALC_DUEDATE_URL);

    verify(mockManagementService).recalculateJobDuedate(jobId, true);
    verifyNoMoreInteractions(mockManagementService);
  }

  @Test
  void testRecalculateDuedateUnauthorized() {
    String jobId = MockProvider.EXAMPLE_JOB_ID;

    String expectedMessage = "Missing permissions";

    doThrow(new AuthorizationException(expectedMessage))
      .when(mockManagementService).recalculateJobDuedate(jobId, true);

    given()
      .pathParam("id", jobId)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", is(AuthorizationException.class.getSimpleName()))
      .body("message", is(expectedMessage))
    .when().post(JOB_RESOURCE_RECALC_DUEDATE_URL);

    verify(mockManagementService).recalculateJobDuedate(jobId, true);
    verifyNoMoreInteractions(mockManagementService);
  }

  protected void verifyBatchJson(String batchJson) {
    BatchDto batch = JsonPathUtil.from(batchJson).getObject("", BatchDto.class);
    assertThat(batch).as("The returned batch should not be null.").isNotNull();
    assertThat(batch.getId()).isEqualTo(MockProvider.EXAMPLE_BATCH_ID);
    assertThat(batch.getType()).isEqualTo(MockProvider.EXAMPLE_BATCH_TYPE);
    assertThat(batch.getTotalJobs()).isEqualTo(MockProvider.EXAMPLE_BATCH_TOTAL_JOBS);
    assertThat(batch.getBatchJobsPerSeed()).isEqualTo(MockProvider.EXAMPLE_BATCH_JOBS_PER_SEED);
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(MockProvider.EXAMPLE_INVOCATIONS_PER_BATCH_JOB);
    assertThat(batch.getSeedJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_SEED_JOB_DEFINITION_ID);
    assertThat(batch.getMonitorJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_MONITOR_JOB_DEFINITION_ID);
    assertThat(batch.getBatchJobDefinitionId()).isEqualTo(MockProvider.EXAMPLE_BATCH_JOB_DEFINITION_ID);
    assertThat(batch.getTenantId()).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
  }

}
