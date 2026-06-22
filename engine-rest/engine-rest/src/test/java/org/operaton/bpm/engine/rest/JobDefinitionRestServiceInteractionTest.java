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

import java.util.Collections;
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
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.management.SetJobRetriesBuilder;
import org.operaton.bpm.engine.management.UpdateJobDefinitionSuspensionStateSelectBuilder;
import org.operaton.bpm.engine.management.UpdateJobDefinitionSuspensionStateTenantBuilder;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.MockJobDefinitionBuilder;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JobDefinitionRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String JOB_DEFINITION_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/job-definition";
  protected static final String SINGLE_JOB_DEFINITION_RESOURCE_URL = JOB_DEFINITION_RESOURCE_URL + "/{id}";
  protected static final String SINGLE_JOB_DEFINITION_SUSPENDED_URL = SINGLE_JOB_DEFINITION_RESOURCE_URL + "/suspended";
  protected static final String JOB_DEFINITION_SUSPENDED_URL = JOB_DEFINITION_RESOURCE_URL + "/suspended";
  protected static final String JOB_DEFINITION_RETRIES_URL = SINGLE_JOB_DEFINITION_RESOURCE_URL + "/retries";
  protected static final String JOB_DEFINITION_PRIORITY_URL = SINGLE_JOB_DEFINITION_RESOURCE_URL + "/jobPriority";

  private ProcessEngine namedProcessEngine;
  private ManagementService mockManagementService;

  private UpdateJobDefinitionSuspensionStateTenantBuilder mockSuspensionStateBuilder;
  private UpdateJobDefinitionSuspensionStateSelectBuilder mockSuspensionStateSelectBuilder;

  private JobDefinitionQuery mockQuery;

  private SetJobRetriesBuilder mockSetJobRetriesBuilder;

  @BeforeEach
  void setUpRuntimeData() {
    mockManagementService = mock(ManagementService.class);

    namedProcessEngine = getProcessEngine(MockProvider.EXAMPLE_PROCESS_ENGINE_NAME);
    when(namedProcessEngine.getManagementService()).thenReturn(mockManagementService);

    List<JobDefinition> mockJobDefinitions = Collections.singletonList(MockProvider.createMockJobDefinition());
    mockQuery = setUpMockJobDefinitionQuery(mockJobDefinitions);

    mockSuspensionStateSelectBuilder = mock(UpdateJobDefinitionSuspensionStateSelectBuilder.class);
    when(mockManagementService.updateJobDefinitionSuspensionState()).thenReturn(mockSuspensionStateSelectBuilder);

    mockSuspensionStateBuilder = mock(UpdateJobDefinitionSuspensionStateTenantBuilder.class);
    when(mockSuspensionStateSelectBuilder.byJobDefinitionId(anyString())).thenReturn(mockSuspensionStateBuilder);
    when(mockSuspensionStateSelectBuilder.byProcessDefinitionId(anyString())).thenReturn(mockSuspensionStateBuilder);
    when(mockSuspensionStateSelectBuilder.byProcessDefinitionKey(anyString())).thenReturn(mockSuspensionStateBuilder);

    mockSetJobRetriesBuilder = mock(SetJobRetriesBuilder.class);
    when(mockManagementService.setJobRetries(anyInt())).thenReturn(mockSetJobRetriesBuilder);
    when(mockSetJobRetriesBuilder.jobId(any())).thenReturn(mockSetJobRetriesBuilder);
    when(mockSetJobRetriesBuilder.jobIds(any())).thenReturn(mockSetJobRetriesBuilder);
    when(mockSetJobRetriesBuilder.jobDefinitionId(any())).thenReturn(mockSetJobRetriesBuilder);
    when(mockSetJobRetriesBuilder.dueDate(any())).thenReturn(mockSetJobRetriesBuilder);
  }

  private JobDefinitionQuery setUpMockJobDefinitionQuery(List<JobDefinition> mockedJobDefinitions) {
    JobDefinitionQuery sampleJobDefinitionQuery = mock(JobDefinitionQuery.class);

    when(sampleJobDefinitionQuery.list()).thenReturn(mockedJobDefinitions);
    when(sampleJobDefinitionQuery.count()).thenReturn((long) mockedJobDefinitions.size());
    if(mockedJobDefinitions.size() == 1) {
      when(sampleJobDefinitionQuery.singleResult()).thenReturn(mockedJobDefinitions.get(0));
    }

    when(sampleJobDefinitionQuery.jobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID)).thenReturn(sampleJobDefinitionQuery);

    when(processEngine.getManagementService().createJobDefinitionQuery()).thenReturn(sampleJobDefinitionQuery);
    when(mockManagementService.createJobDefinitionQuery()).thenReturn(sampleJobDefinitionQuery);

    return sampleJobDefinitionQuery;
  }

  @Test
  void testSimpleJobDefinitionGet() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_JOB_DEFINITION_ID))
        .body("processDefinitionId", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
        .body("processDefinitionKey", equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY))
        .body("jobType", equalTo(MockProvider.EXAMPLE_JOB_TYPE))
        .body("jobConfiguration", equalTo(MockProvider.EXAMPLE_JOB_CONFIG))
        .body("activityId", equalTo(MockProvider.EXAMPLE_ACTIVITY_ID))
        .body("suspended", equalTo(MockProvider.EXAMPLE_JOB_DEFINITION_IS_SUSPENDED))
        .body("overridingJobPriority", equalTo(MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY))
        .body("tenantId", equalTo(null))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_JOB_DEFINITION_DEPLOYMENT_ID))
    .when()
      .get(SINGLE_JOB_DEFINITION_RESOURCE_URL);

    InOrder inOrder = inOrder(mockQuery);
    inOrder.verify(mockQuery).jobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    inOrder.verify(mockQuery).singleResult();
  }

  @Test
  void testJobDefinitionGetNullJobPriority() {
    // given
    JobDefinition mockJobDefinition = new MockJobDefinitionBuilder()
      .id(MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .jobPriority(null)
      .build();

    when(mockQuery.singleResult()).thenReturn(mockJobDefinition);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_JOB_DEFINITION_ID))
        .body("jobPriority", nullValue())
    .when()
      .get(SINGLE_JOB_DEFINITION_RESOURCE_URL);
  }

  @Test
  void testJobDefinitionGetIdDoesntExist() {
    JobDefinitionQuery invalidQueryNonExistingJobDefinition;
    invalidQueryNonExistingJobDefinition = mock(JobDefinitionQuery.class);

    when(mockManagementService.createJobDefinitionQuery()
        .jobDefinitionId(MockProvider.NON_EXISTING_JOB_DEFINITION_ID))
        .thenReturn(invalidQueryNonExistingJobDefinition);

    when(invalidQueryNonExistingJobDefinition.singleResult()).thenReturn(null);

    String jobDefinitionId = MockProvider.NON_EXISTING_JOB_DEFINITION_ID;

    given()
      .pathParam("id", jobDefinitionId)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Job Definition with id " + jobDefinitionId + " does not exist"))
    .when()
      .get(SINGLE_JOB_DEFINITION_RESOURCE_URL);
  }

  @Test
  void testActivateJobDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", false);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testDelayedActivateJobDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", false);
    params.put("executionDate", MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(false);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testDelayedActivateJobDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", true);
    params.put("executionDate", MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateThrowProcessEngineException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateNonParseableDateFormat() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", false);
    params.put("executionDate", "a");

    String expectedMessage = "Invalid format: \"a\"";
    String exceptionMessage = "The suspension state of Job Definition with id %s could not be updated due to: %s".formatted(MockProvider.NON_EXISTING_JOB_DEFINITION_ID, expectedMessage);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(exceptionMessage))
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", false);

    String expectedMessage = "expectedMessage";
    doThrow(new AuthorizationException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .activate();

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", false);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).includeJobs(false);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testDelayedSuspendJobDefinitionExcludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", false);
    params.put("executionDate", MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(false);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testDelayedSuspendJobDefinitionIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", true);
    params.put("executionDate", MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_JOB_DEFINITION_DELAYED_EXECUTION);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byJobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendThrowsProcessEngineException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", is(ProcessEngineException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendNonParseableDateFormat() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", false);
    params.put("executionDate", "a");

    String expectedMessage = "Invalid format: \"a\"";
    String exceptionMessage = "The suspension state of Job Definition with id %s could not be updated due to: %s".formatted(MockProvider.NON_EXISTING_JOB_DEFINITION_ID, expectedMessage);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(exceptionMessage))
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendWithMultipleByParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "Only one of jobDefinitionId, processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", false);

    String expectedMessage = "expectedMessage";
    doThrow(new AuthorizationException(expectedMessage))
      .when(mockSuspensionStateBuilder)
      .suspend();

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", is(expectedMessage))
      .when()
        .put(SINGLE_JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionKey() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testDelayedActivateJobDefinitionByProcessDefinitionKey() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testDelayedActivateJobDefinitionByProcessDefinitionKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", true);
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionKeyWithUnparseableDate() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", "a");

    String message = "Could not update the suspension state of Job Definitions due to: Invalid format: \"a\"";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionKeyWithException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionKeyWithAuthorizationException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionKeyAndTenantId() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionTenantId(MockProvider.EXAMPLE_TENANT_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionKeyWithoutTenantId() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionWithoutTenantId();
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionKey() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testDelayedSuspendJobDefinitionByProcessDefinitionKey() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testDelayedSuspendJobDefinitionByProcessDefinitionKeyIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", true);
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionKeyWithUnparseableDate() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    params.put("executionDate", "a");

    String message = "Could not update the suspension state of Job Definitions due to: Invalid format: \"a\"";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionKeyWithException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionKeyWithAuthorizationException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionKeyAndTenantId() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionTenantId(MockProvider.EXAMPLE_TENANT_ID);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionKeyWithoutTenantId() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockSuspensionStateBuilder).processDefinitionWithoutTenantId();
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionId() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionIdIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testDelayedActivateJobDefinitionByProcessDefinitionId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testDelayedActivateJobDefinitionByProcessDefinitionIdIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("includeJobs", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionIdWithUnparseableDate() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("executionDate", "a");

    String message = "Could not update the suspension state of Job Definitions due to: Invalid format: \"a\"";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionIdWithException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByProcessDefinitionIdWithAuthorizationException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionId() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionIdIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testDelayedSuspendJobDefinitionByProcessDefinitionId() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testDelayedSuspendJobDefinitionByProcessDefinitionIdIncludingInstances() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("includeJobs", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("executionDate", MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    Date executionDate = DateTimeUtil.parseDate(MockProvider.EXAMPLE_PROCESS_DEFINITION_DELAYED_EXECUTION);

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);

    verify(mockSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockSuspensionStateBuilder).executionDate(executionDate);
    verify(mockSuspensionStateBuilder).includeJobs(true);
    verify(mockSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionIdWithUnparseableDate() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("executionDate", "a");

    String message = "Could not update the suspension state of Job Definitions due to: Invalid format: \"a\"";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionIdWithException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByProcessDefinitionIdWithAuthorizationException() {
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
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testActivateJobDefinitionByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("jobDefinitionId", MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    String message = "Either processDefinitionId or processDefinitionKey can be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("jobDefinitionId", MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    String message = "Either processDefinitionId or processDefinitionKey can be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSuspendJobDefinitionByNothing() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);

    String message = "Either jobDefinitionId, processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", is(message))
      .when()
        .put(JOB_DEFINITION_SUSPENDED_URL);
  }

  @Test
  void testSetJobRetries() {
    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_DEFINITION_RETRIES_URL);

    verify(mockManagementService, times(1)).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder, times(1)).jobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSetJobRetriesBuilder, times(1)).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetJobRetriesWithDueDate() {
    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);
    Date newDueDate = new Date(1675752840000L);
    retriesVariableJson.put("dueDate", newDueDate);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_DEFINITION_RETRIES_URL);

    verify(mockManagementService, times(1)).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder, times(1)).jobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSetJobRetriesBuilder, times(1)).dueDate(newDueDate);
    verify(mockSetJobRetriesBuilder, times(1)).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetJobRetriesWithNullDueDate() {
    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);
    retriesVariableJson.put("dueDate", null);

    given()
    .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
    .contentType(ContentType.JSON)
    .body(retriesVariableJson)
    .then()
    .expect()
    .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
    .put(JOB_DEFINITION_RETRIES_URL);

    verify(mockManagementService, times(1)).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder, times(1)).jobDefinitionId(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
    verify(mockSetJobRetriesBuilder, times(1)).dueDate(null);
    verify(mockSetJobRetriesBuilder, times(1)).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetJobRetriesExceptionExpected() {
    doThrow(new ProcessEngineException("job definition not found"))
      .when(mockSetJobRetriesBuilder).execute();

    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("job definition not found"))
    .when()
      .put(JOB_DEFINITION_RETRIES_URL);

    verify(mockManagementService, times(1)).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder, times(1)).jobDefinitionId(MockProvider.NON_EXISTING_JOB_DEFINITION_ID);
    verify(mockSetJobRetriesBuilder, times(1)).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetJobRetriesAuthorizationException() {
    String expectedMessage = "expected exception message";
    doThrow(new AuthorizationException(expectedMessage)).when(mockSetJobRetriesBuilder).execute();

    Map<String, Object> retriesVariableJson = new HashMap<>();
    retriesVariableJson.put("retries", MockProvider.EXAMPLE_JOB_RETRIES);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(retriesVariableJson)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", equalTo(AuthorizationException.class.getSimpleName()))
        .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_DEFINITION_RETRIES_URL);

    verify(mockManagementService, times(1)).setJobRetries(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesBuilder, times(1)).jobDefinitionId(MockProvider.NON_EXISTING_JOB_DEFINITION_ID);
    verify(mockSetJobRetriesBuilder, times(1)).execute();
    verifyNoMoreInteractions(mockSetJobRetriesBuilder);
  }

  @Test
  void testSetJobPriority() {
    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);

    verify(mockManagementService).setOverridingJobPriorityForJobDefinition(MockProvider.EXAMPLE_JOB_DEFINITION_ID,
        MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY, false);
  }

  @Test
  void testSetJobPriorityToExtremeValue() {
    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", Long.MAX_VALUE);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);

    verify(mockManagementService).setOverridingJobPriorityForJobDefinition(MockProvider.EXAMPLE_JOB_DEFINITION_ID,
        Long.MAX_VALUE, false);
  }

  @Test
  void testSetJobPriorityIncludeExistingJobs() {
    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY);
    priorityJson.put("includeJobs", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);

    verify(mockManagementService).setOverridingJobPriorityForJobDefinition(MockProvider.EXAMPLE_JOB_DEFINITION_ID,
        MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY, true);
  }

  @Test
  void testSetJobPriorityExceptionExpected() {
    String expectedMessage = "expected exception message";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockManagementService)
        .setOverridingJobPriorityForJobDefinition(eq(MockProvider.EXAMPLE_JOB_DEFINITION_ID),
            eq(MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY), anyBoolean());

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);

    verify(mockManagementService).setOverridingJobPriorityForJobDefinition(MockProvider.EXAMPLE_JOB_DEFINITION_ID,
        MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY, false);
  }

  @Test
  void testSetNonExistingJobDefinitionPriority() {
    String expectedMessage = "expected exception message";

    doThrow(new NotFoundException(expectedMessage))
      .when(mockManagementService)
        .setOverridingJobPriorityForJobDefinition(eq(MockProvider.NON_EXISTING_JOB_DEFINITION_ID),
            eq(MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY), anyBoolean());

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);
  }

  @Test
  void testSetJobPriorityAuthorizationException() {
    String expectedMessage = "expected exception message";
    doThrow(new AuthorizationException(expectedMessage))
      .when(mockManagementService)
        .setOverridingJobPriorityForJobDefinition(eq(MockProvider.EXAMPLE_JOB_DEFINITION_ID),
            eq(MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY), anyBoolean());

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", MockProvider.EXAMPLE_JOB_DEFINITION_PRIORITY);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", equalTo(AuthorizationException.class.getSimpleName()))
        .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);
  }

  @Test
  void testResetJobPriority() {
    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);

    verify(mockManagementService).clearOverridingJobPriorityForJobDefinition(MockProvider.EXAMPLE_JOB_DEFINITION_ID);
  }

  @Test
  void testResetJobPriorityIncludeJobsNotAllowed() {
    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", null);
    priorityJson.put("includeJobs", true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo("Cannot reset priority for job definition " + MockProvider.EXAMPLE_JOB_DEFINITION_ID
            + " with includeJobs=true"))
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);
  }

  @Test
  void testResetJobPriorityExceptionExpected() {
    String expectedMessage = "expected exception message";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockManagementService).clearOverridingJobPriorityForJobDefinition(MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", equalTo(RestException.class.getSimpleName()))
        .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);
  }

  @Test
  void testResetNonExistingJobDefinitionPriority() {
    String expectedMessage = "expected exception message";

    doThrow(new NotFoundException(expectedMessage))
      .when(mockManagementService).clearOverridingJobPriorityForJobDefinition(MockProvider.NON_EXISTING_JOB_DEFINITION_ID);

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", null);

    given()
      .pathParam("id", MockProvider.NON_EXISTING_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);
  }

  @Test
  void testResetJobPriorityAuthorizationException() {
    String expectedMessage = "expected exception message";
    doThrow(new AuthorizationException(expectedMessage))
    .when(mockManagementService).clearOverridingJobPriorityForJobDefinition(MockProvider.EXAMPLE_JOB_DEFINITION_ID);

    Map<String, Object> priorityJson = new HashMap<>();
    priorityJson.put("priority", null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_JOB_DEFINITION_ID)
      .contentType(ContentType.JSON)
      .body(priorityJson)
    .then()
      .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", equalTo(AuthorizationException.class.getSimpleName()))
        .body("message", equalTo(expectedMessage))
    .when()
      .put(JOB_DEFINITION_PRIORITY_URL);
  }

  @Test
  void testTenantIdListParameter() {
    mockQuery = setUpMockJobDefinitionQuery(createMockJobDefinitionsTwoTenants());

    Response response = given()
      .queryParam("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(JOB_DEFINITION_RESOURCE_URL);

    verify(mockQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> jobDefinitions = from(content).getList("");
    assertThat(jobDefinitions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  @Test
  void testTenantIdListPostParameter() {
    mockQuery = setUpMockJobDefinitionQuery(createMockJobDefinitionsTwoTenants());

    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("tenantIdIn", MockProvider.EXAMPLE_TENANT_ID_LIST.split(","));

    Response response = given()
        .contentType(POST_JSON_CONTENT_TYPE)
        .body(queryParameters)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(JOB_DEFINITION_RESOURCE_URL);

    verify(mockQuery).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
    verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> jobDefinitions = from(content).getList("");
    assertThat(jobDefinitions).hasSize(2);

    String returnedTenantId1 = from(content).getString("[0].tenantId");
    String returnedTenantId2 = from(content).getString("[1].tenantId");

    assertThat(returnedTenantId1).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);
    assertThat(returnedTenantId2).isEqualTo(MockProvider.ANOTHER_EXAMPLE_TENANT_ID);
  }

  private List<JobDefinition> createMockJobDefinitionsTwoTenants() {
    return List.of(
        MockProvider.mockJobDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build(),
        MockProvider.mockJobDefinition().tenantId(MockProvider.ANOTHER_EXAMPLE_TENANT_ID).build());
  }

}
