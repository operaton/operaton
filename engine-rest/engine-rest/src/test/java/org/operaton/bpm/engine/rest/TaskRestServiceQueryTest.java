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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.operaton.bpm.ProcessApplicationService;
import org.operaton.bpm.application.ProcessApplicationInfo;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.CaseDefinitionQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.rest.dto.task.TaskQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.helper.EqualsList;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.ValueGenerator;
import org.operaton.bpm.engine.rest.helper.variable.EqualsPrimitiveValue;
import org.operaton.bpm.engine.rest.util.OrderingBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.variable.type.ValueType;

import static org.mockito.Mockito.clearInvocations;
import static org.operaton.bpm.engine.rest.util.DateTimeUtils.withTimezone;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class TaskRestServiceQueryTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String TASK_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/task";
  protected static final String TASK_COUNT_QUERY_URL = TASK_QUERY_URL + "/count";

  private static final String SAMPLE_VAR_NAME = "varName";
  private static final String SAMPLE_VAR_VALUE = "varValue";

  private TaskQuery mockQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockQuery = setUpMockTaskQuery(MockProvider.createMockTasks());
  }

  private TaskQuery setUpMockTaskQuery(List<Task> mockedTasks) {
    TaskQuery sampleTaskQuery = mock(TaskQueryImpl.class);
    when(sampleTaskQuery.list()).thenReturn(mockedTasks);
    when(sampleTaskQuery.count()).thenReturn((long) mockedTasks.size());
    when(sampleTaskQuery.taskCandidateGroup(anyString())).thenReturn(sampleTaskQuery);

    when(processEngine.getTaskService().createTaskQuery()).thenReturn(sampleTaskQuery);

    return sampleTaskQuery;
  }

  @Test
  void testEmptyQuery() {
    String queryKey = "";
    given().queryParam("name", queryKey)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);
  }

  @Test
  void testInvalidDateParameter() {
    given().queryParams("due", "anInvalidDate")
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Cannot set query parameter 'due' to value 'anInvalidDate': "
          + "Cannot convert value \"anInvalidDate\" to java type java.util.Date"))
      .when().get(TASK_QUERY_URL);
  }

  @Test
  void testSortByParameterOnly() {
    given().queryParam("sortBy", "dueDate")
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(TASK_QUERY_URL);
  }

  @Test
  void testSortOrderParameterOnly() {
    given().queryParam("sortOrder", "asc")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(TASK_QUERY_URL);
  }

  @Test
  void testSimpleTaskQuery() {
    String queryName = "name";

    Response response = given().queryParam("name", queryName)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    InOrder inOrder = inOrder(mockQuery);
    inOrder.verify(mockQuery).taskName(queryName);
    inOrder.verify(mockQuery).list();

    String content = response.asString();
    List<LinkedHashMap<String, String>> instances = from(content).getList("");
    assertThat(instances).withFailMessage("There should be one task returned.").hasSize(1);
    assertThat(instances.get(0)).withFailMessage("The returned task should not be null.").isNotNull();

    String returnedTaskName = from(content).getString("[0].name");
    String returnedId = from(content).getString("[0].id");
    String returendAssignee = from(content).getString("[0].assignee");
    String returnedCreateTime = from(content).getString("[0].created");
    String returnedLastUpdated = from(content).getString("[0].lastUpdated");
    String returnedDueDate = from(content).getString("[0].due");
    String returnedFollowUpDate = from(content).getString("[0].followUp");
    String returnedDelegationState = from(content).getString("[0].delegationState");
    String returnedDescription = from(content).getString("[0].description");
    String returnedExecutionId = from(content).getString("[0].executionId");
    String returnedOwner = from(content).getString("[0].owner");
    String returnedParentTaskId = from(content).getString("[0].parentTaskId");
    int returnedPriority = from(content).getInt("[0].priority");
    String returnedProcessDefinitionId = from(content).getString("[0].processDefinitionId");
    String returnedProcessInstanceId = from(content).getString("[0].processInstanceId");
    String returnedTaskDefinitionKey = from(content).getString("[0].taskDefinitionKey");
    String returnedCaseDefinitionId = from(content).getString("[0].caseDefinitionId");
    String returnedCaseInstanceId = from(content).getString("[0].caseInstanceId");
    String returnedCaseExecutionId = from(content).getString("[0].caseExecutionId");
    boolean returnedSuspensionState = from(content).getBoolean("[0].suspended");
    String returnedFormKey = from(content).getString("[0].formKey");
    String returnedTenantId = from(content).getString("[0].tenantId");

    assertThat(returnedTaskName).isEqualTo(MockProvider.EXAMPLE_TASK_NAME);
    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_TASK_ID);
    assertThat(returendAssignee).isEqualTo(MockProvider.EXAMPLE_TASK_ASSIGNEE_NAME);
    assertThat(returnedCreateTime).isEqualTo(MockProvider.EXAMPLE_TASK_CREATE_TIME);
    assertThat(returnedLastUpdated).isEqualTo(MockProvider.EXAMPLE_TASK_LAST_UPDATED);
    assertThat(returnedDueDate).isEqualTo(MockProvider.EXAMPLE_TASK_DUE_DATE);
    assertThat(returnedFollowUpDate).isEqualTo(MockProvider.EXAMPLE_FOLLOW_UP_DATE);
    assertThat(returnedDelegationState).isEqualTo(MockProvider.EXAMPLE_TASK_DELEGATION_STATE.name());
    assertThat(returnedDescription).isEqualTo(MockProvider.EXAMPLE_TASK_DESCRIPTION);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_TASK_EXECUTION_ID);
    assertThat(returnedOwner).isEqualTo(MockProvider.EXAMPLE_TASK_OWNER);
    assertThat(returnedParentTaskId).isEqualTo(MockProvider.EXAMPLE_TASK_PARENT_TASK_ID);
    assertThat(returnedPriority).isEqualTo(MockProvider.EXAMPLE_TASK_PRIORITY);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedTaskDefinitionKey).isEqualTo(MockProvider.EXAMPLE_TASK_DEFINITION_KEY);
    assertThat(returnedCaseDefinitionId).isEqualTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    assertThat(returnedCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID);
    assertThat(returnedCaseExecutionId).isEqualTo(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    assertThat(returnedSuspensionState).isEqualTo(MockProvider.EXAMPLE_TASK_SUSPENSION_STATE);
    assertThat(returnedFormKey).isEqualTo(MockProvider.EXAMPLE_FORM_KEY);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);

  }

  @Test
  void testTaskQueryWithAttachmentAndComment() {
    String queryName = "name";

    Response response = given().queryParam("name", queryName)
            .queryParam("withCommentAttachmentInfo","true")
            .header("accept", MediaType.APPLICATION_JSON)
            .then().expect().statusCode(Status.OK.getStatusCode())
            .when().get(TASK_QUERY_URL);

    InOrder inOrder = inOrder(mockQuery);
    inOrder.verify(mockQuery).taskName(queryName);
    inOrder.verify(mockQuery).list();

    String content = response.asString();
    List<LinkedHashMap<String, String>> instances = from(content).getList("");
    assertThat(instances).withFailMessage("There should be one task returned.").hasSize(1);
    assertThat(instances.get(0)).withFailMessage("The returned task should not be null.").isNotNull();

    boolean returnedAttachmentsInfo = from(content).getBoolean("[0].attachment");
    boolean returnedCommentsInfo = from(content).getBoolean("[0].comment");

    assertThat(returnedAttachmentsInfo).isEqualTo(MockProvider.EXAMPLE_TASK_ATTACHMENT_STATE);
    assertThat(returnedCommentsInfo).isEqualTo(MockProvider.EXAMPLE_TASK_COMMENT_STATE);

  }

  @Test
  void testSimpleHalTaskQuery() {
    String queryName = "name";

    // setup user query mock
    List<User> mockUsers = MockProvider.createMockUsers();
    UserQuery sampleUserQuery = mock(UserQuery.class);
    when(sampleUserQuery.listPage(0, 1)).thenReturn(mockUsers);
    when(sampleUserQuery.userIdIn(MockProvider.EXAMPLE_TASK_ASSIGNEE_NAME)).thenReturn(sampleUserQuery);
    when(sampleUserQuery.userIdIn(MockProvider.EXAMPLE_TASK_OWNER)).thenReturn(sampleUserQuery);
    when(sampleUserQuery.count()).thenReturn(1L);
    when(processEngine.getIdentityService().createUserQuery()).thenReturn(sampleUserQuery);

    // setup process definition query mock
    List<ProcessDefinition> mockDefinitions = MockProvider.createMockDefinitions();
    ProcessDefinitionQuery sampleProcessDefinitionQuery = mock(ProcessDefinitionQuery.class);
    when(sampleProcessDefinitionQuery.listPage(0, 1)).thenReturn(mockDefinitions);
    when(sampleProcessDefinitionQuery.processDefinitionIdIn(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)).thenReturn(sampleProcessDefinitionQuery);
    when(sampleProcessDefinitionQuery.count()).thenReturn(1L);
    when(processEngine.getRepositoryService().createProcessDefinitionQuery()).thenReturn(sampleProcessDefinitionQuery);

    // setup case definition query mock
    List<CaseDefinition> mockCaseDefinitions = MockProvider.createMockCaseDefinitions();
    CaseDefinitionQuery sampleCaseDefinitionQuery = mock(CaseDefinitionQuery.class);
    when(sampleCaseDefinitionQuery.listPage(0, 1)).thenReturn(mockCaseDefinitions);
    when(sampleCaseDefinitionQuery.caseDefinitionIdIn(MockProvider.EXAMPLE_CASE_DEFINITION_ID)).thenReturn(sampleCaseDefinitionQuery);
    when(sampleCaseDefinitionQuery.count()).thenReturn(1L);
    when(processEngine.getRepositoryService().createCaseDefinitionQuery()).thenReturn(sampleCaseDefinitionQuery);

    // setup example process application context path
    when(processEngine.getManagementService().getProcessApplicationForDeployment(MockProvider.EXAMPLE_DEPLOYMENT_ID))
      .thenReturn(MockProvider.EXAMPLE_PROCESS_APPLICATION_NAME);

    // replace the runtime container delegate & process application service with a mock
    ProcessApplicationService processApplicationService = mock(ProcessApplicationService.class);
    ProcessApplicationInfo appMock = MockProvider.createMockProcessApplicationInfo();
    when(processApplicationService.getProcessApplicationInfo(MockProvider.EXAMPLE_PROCESS_APPLICATION_NAME)).thenReturn(appMock);

    RuntimeContainerDelegate delegate = mock(RuntimeContainerDelegate.class);
    when(delegate.getProcessApplicationService()).thenReturn(processApplicationService);
    RuntimeContainerDelegate.INSTANCE.set(delegate);

    Response response = given().queryParam("name", queryName)
      .header("accept", Hal.APPLICATION_HAL_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .contentType(Hal.APPLICATION_HAL_JSON)
      .when().get(TASK_QUERY_URL);

    InOrder inOrder = inOrder(mockQuery);
    inOrder.verify(mockQuery).taskName(queryName);
    inOrder.verify(mockQuery).list();

    // validate embedded tasks
    String content = response.asString();
    List<Map<String,Object>> instances = from(content).getList("_embedded.task");
    assertThat(instances).as("There should be one task returned.").hasSize(1);
    assertThat(instances.get(0)).as("The returned task should not be null.").isNotNull();

    Map<String, Object> taskObject = instances.get(0);

    String returnedTaskName = (String) taskObject.get("name");
    String returnedId = (String) taskObject.get("id");
    String returnedAssignee = (String) taskObject.get("assignee");
    String returnedCreateTime = (String) taskObject.get("created");
    String returnedDueDate = (String) taskObject.get("due");
    String returnedFollowUpDate = (String) taskObject.get("followUp");
    String returnedDelegationState = (String) taskObject.get("delegationState");
    String returnedDescription = (String) taskObject.get("description");
    String returnedExecutionId = (String) taskObject.get("executionId");
    String returnedOwner = (String) taskObject.get("owner");
    String returnedParentTaskId = (String) taskObject.get("parentTaskId");
    int returnedPriority = (Integer) taskObject.get("priority");
    String returnedProcessDefinitionId = (String) taskObject.get("processDefinitionId");
    String returnedProcessInstanceId = (String) taskObject.get("processInstanceId");
    String returnedTaskDefinitionKey = (String) taskObject.get("taskDefinitionKey");
    String returnedCaseDefinitionId = (String) taskObject.get("caseDefinitionId");
    String returnedCaseInstanceId = (String) taskObject.get("caseInstanceId");
    String returnedCaseExecutionId = (String) taskObject.get("caseExecutionId");
    boolean returnedSuspensionState = (Boolean) taskObject.get("suspended");
    String returnedFormKey = (String) taskObject.get("formKey");
    String returnedTenantId = (String) taskObject.get("tenantId");

    assertThat(returnedTaskName).isEqualTo(MockProvider.EXAMPLE_TASK_NAME);
    assertThat(returnedId).isEqualTo(MockProvider.EXAMPLE_TASK_ID);
    assertThat(returnedAssignee).isEqualTo(MockProvider.EXAMPLE_TASK_ASSIGNEE_NAME);
    assertThat(returnedCreateTime).isEqualTo(MockProvider.EXAMPLE_TASK_CREATE_TIME);
    assertThat(returnedDueDate).isEqualTo(MockProvider.EXAMPLE_TASK_DUE_DATE);
    assertThat(returnedFollowUpDate).isEqualTo(MockProvider.EXAMPLE_FOLLOW_UP_DATE);
    assertThat(returnedDelegationState).isEqualTo(MockProvider.EXAMPLE_TASK_DELEGATION_STATE.toString());
    assertThat(returnedDescription).isEqualTo(MockProvider.EXAMPLE_TASK_DESCRIPTION);
    assertThat(returnedExecutionId).isEqualTo(MockProvider.EXAMPLE_TASK_EXECUTION_ID);
    assertThat(returnedOwner).isEqualTo(MockProvider.EXAMPLE_TASK_OWNER);
    assertThat(returnedParentTaskId).isEqualTo(MockProvider.EXAMPLE_TASK_PARENT_TASK_ID);
    assertThat(returnedPriority).isEqualTo(MockProvider.EXAMPLE_TASK_PRIORITY);
    assertThat(returnedProcessDefinitionId).isEqualTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    assertThat(returnedProcessInstanceId).isEqualTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    assertThat(returnedTaskDefinitionKey).isEqualTo(MockProvider.EXAMPLE_TASK_DEFINITION_KEY);
    assertThat(returnedCaseDefinitionId).isEqualTo(MockProvider.EXAMPLE_CASE_DEFINITION_ID);
    assertThat(returnedCaseInstanceId).isEqualTo(MockProvider.EXAMPLE_CASE_INSTANCE_ID);
    assertThat(returnedCaseExecutionId).isEqualTo(MockProvider.EXAMPLE_CASE_EXECUTION_ID);
    assertThat(returnedSuspensionState).isEqualTo(MockProvider.EXAMPLE_TASK_SUSPENSION_STATE);
    assertThat(returnedFormKey).isEqualTo(MockProvider.EXAMPLE_FORM_KEY);
    assertThat(returnedTenantId).isEqualTo(MockProvider.EXAMPLE_TENANT_ID);

    // validate the task count
    assertThat(from(content).getLong("count")).isOne();

    // validate links
    Map<String,Object> selfReference = from(content).getMap("_links.self");
    assertThat(selfReference)
            .isNotNull()
            .containsEntry("href", "/task");

    // validate embedded assignees:
    List<Map<String,Object>> embeddedAssignees = from(content).getList("_embedded.assignee");
    assertThat(embeddedAssignees).as("There should be one assignee returned.").hasSize(1);
    Map<String, Object> embeddedAssignee = embeddedAssignees.get(0);
    assertThat(embeddedAssignee)
            .isNotNull()
            .containsEntry("id", MockProvider.EXAMPLE_USER_ID)
            .containsEntry("firstName", MockProvider.EXAMPLE_USER_FIRST_NAME)
            .containsEntry("lastName", MockProvider.EXAMPLE_USER_LAST_NAME)
            .containsEntry("email", MockProvider.EXAMPLE_USER_EMAIL);

    // validate embedded owners:
    List<Map<String,Object>> embeddedOwners = from(content).getList("_embedded.owner");
    assertThat(embeddedOwners).as("There should be one owner returned.").hasSize(1);
    Map<String, Object> embeddedOwner = embeddedOwners.get(0);
    assertThat(embeddedOwner)
            .isNotNull()
            .containsEntry("id", MockProvider.EXAMPLE_USER_ID)
            .containsEntry("firstName", MockProvider.EXAMPLE_USER_FIRST_NAME)
            .containsEntry("lastName", MockProvider.EXAMPLE_USER_LAST_NAME)
            .containsEntry("email", MockProvider.EXAMPLE_USER_EMAIL);

    // validate embedded processDefinitions:
    List<Map<String,Object>> embeddedDefinitions = from(content).getList("_embedded.processDefinition");
    assertThat(embeddedDefinitions).as("There should be one processDefinition returned.").hasSize(1);
    Map<String, Object> embeddedProcessDefinition = embeddedDefinitions.get(0);
    assertThat(embeddedProcessDefinition)
            .isNotNull()
            .containsEntry("id", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID)
            .containsEntry("key", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY)
            .containsEntry("category", MockProvider.EXAMPLE_PROCESS_DEFINITION_CATEGORY)
            .containsEntry("name", MockProvider.EXAMPLE_PROCESS_DEFINITION_NAME)
            .containsEntry("description", MockProvider.EXAMPLE_PROCESS_DEFINITION_DESCRIPTION)
            .containsEntry("version", MockProvider.EXAMPLE_PROCESS_DEFINITION_VERSION)
            .containsEntry("versionTag", MockProvider.EXAMPLE_VERSION_TAG)
            .containsEntry("resource", MockProvider.EXAMPLE_PROCESS_DEFINITION_RESOURCE_NAME)
            .containsEntry("deploymentId", MockProvider.EXAMPLE_DEPLOYMENT_ID)
            .containsEntry("diagram", MockProvider.EXAMPLE_PROCESS_DEFINITION_DIAGRAM_RESOURCE_NAME)
            .containsEntry("suspended", MockProvider.EXAMPLE_PROCESS_DEFINITION_IS_SUSPENDED)
            .containsEntry("contextPath", MockProvider.EXAMPLE_PROCESS_APPLICATION_CONTEXT_PATH);

    // validate embedded caseDefinitions:
    List<Map<String,Object>> embeddedCaseDefinitions = from(content).getList("_embedded.caseDefinition");
    assertThat(embeddedCaseDefinitions).as("There should be one caseDefinition returned.").hasSize(1);
    Map<String, Object> embeddedCaseDefinition = embeddedCaseDefinitions.get(0);
    assertThat(embeddedCaseDefinition)
            .isNotNull()
            .containsEntry("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
            .containsEntry("key", MockProvider.EXAMPLE_CASE_DEFINITION_KEY)
            .containsEntry("category", MockProvider.EXAMPLE_CASE_DEFINITION_CATEGORY)
            .containsEntry("name", MockProvider.EXAMPLE_CASE_DEFINITION_NAME)
            .containsEntry("version", MockProvider.EXAMPLE_CASE_DEFINITION_VERSION)
            .containsEntry("resource", MockProvider.EXAMPLE_CASE_DEFINITION_RESOURCE_NAME)
            .containsEntry("deploymentId", MockProvider.EXAMPLE_DEPLOYMENT_ID)
            .containsEntry("contextPath", MockProvider.EXAMPLE_PROCESS_APPLICATION_CONTEXT_PATH);
  }

  @Test
  void testNoParametersQuery() {
    given()
      .header("accept", MediaType.APPLICATION_JSON)
    .expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery).initializeFormKeys();
    verify(mockQuery).list();
    verifyNoMoreInteractions(mockQuery);
  }

  @Test
  void testAdditionalParametersExcludingVariables() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();
    Map<String, Integer> intQueryParameters = getCompleteIntQueryParameters();
    Map<String, Boolean> booleanQueryParameters = getCompleteBooleanQueryParameters();

    Map<String, String[]> arrayQueryParameters = getCompleteStringArrayQueryParameters();

    given()
      .queryParams(stringQueryParameters)
      .queryParams(intQueryParameters)
      .queryParams(booleanQueryParameters)
      .queryParam("activityInstanceIdIn", String.join(",", arrayQueryParameters.get("activityInstanceIdIn")))
      .queryParam("taskDefinitionKeyIn", String.join(",", arrayQueryParameters.get("taskDefinitionKeyIn")))
      .queryParam("taskDefinitionKeyNotIn", String.join(",", arrayQueryParameters.get("taskDefinitionKeyNotIn")))
      .queryParam("taskIdIn", String.join(",", arrayQueryParameters.get("taskIdIn")))
      .queryParam("processDefinitionKeyIn", String.join(",", arrayQueryParameters.get("processDefinitionKeyIn")))
      .queryParam("processInstanceBusinessKeyIn", String.join(",", arrayQueryParameters.get("processInstanceBusinessKeyIn")))
      .queryParam("tenantIdIn", String.join(",", arrayQueryParameters.get("tenantIdIn")))
      .queryParam("assigneeIn", String.join(",", arrayQueryParameters.get("assigneeIn")))
      .queryParam("assigneeNotIn", String.join(",", arrayQueryParameters.get("assigneeNotIn")))
      .queryParam("processInstanceIdIn", String.join(",", arrayQueryParameters.get("processInstanceIdIn")))
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verifyIntegerParameterQueryInvocations();
    verifyStringParameterQueryInvocations();
    verifyBooleanParameterQueryInvocation();
    verifyStringArrayParametersInvocations();

    verify(mockQuery).list();
  }

  private void verifyIntegerParameterQueryInvocations() {
    Map<String, Integer> intQueryParameters = getCompleteIntQueryParameters();

    verify(mockQuery).taskMaxPriority(intQueryParameters.get("maxPriority"));
    verify(mockQuery).taskMinPriority(intQueryParameters.get("minPriority"));
    verify(mockQuery).taskPriority(intQueryParameters.get("priority"));
  }

  private Map<String, Integer> getCompleteIntQueryParameters() {
    Map<String, Integer> parameters = new HashMap<>();

    parameters.put("maxPriority", 10);
    parameters.put("minPriority", 9);
    parameters.put("priority", 8);

    return parameters;
  }

  private Map<String, String[]> getCompleteStringArrayQueryParameters() {
    Map<String, String[]> parameters = new HashMap<>();

    String[] activityInstanceIds = { "anActivityInstanceId", "anotherActivityInstanceId" };
    String[] taskDefinitionKeys = { "aTaskDefinitionKey", "anotherTaskDefinitionKey" };
    String[] taskDefinitionKeyNotIn = { "anUnwantedTaskDefinitionKey", "anotherUnwantedTaskDefinitionKey" };
    String[] processDefinitionKeys = { "aProcessDefinitionKey", "anotherProcessDefinitionKey" };
    String[] processInstanceBusinessKeys = { "aBusinessKey", "anotherBusinessKey" };
    String[] tenantIds = { MockProvider.EXAMPLE_TENANT_ID, MockProvider.ANOTHER_EXAMPLE_TENANT_ID };
    String[] assigneeIn = { MockProvider.EXAMPLE_USER_ID, "anAssignee" };
    String[] assigneeNotIn = { MockProvider.EXAMPLE_USER_ID, "anAssignee" };
    String[] taskId = { MockProvider.EXAMPLE_USER_ID, "anTaskId" };
    String[] processInstanceIds = { MockProvider.EXAMPLE_PROCESS_INSTANCE_ID , MockProvider.ANOTHER_EXAMPLE_PROCESS_INSTANCE_ID };


    parameters.put("activityInstanceIdIn", activityInstanceIds);
    parameters.put("taskDefinitionKeyIn", taskDefinitionKeys);
    parameters.put("taskDefinitionKeyNotIn", taskDefinitionKeyNotIn);
    parameters.put("taskIdIn", taskId);
    parameters.put("processDefinitionKeyIn", processDefinitionKeys);
    parameters.put("processInstanceBusinessKeyIn", processInstanceBusinessKeys);
    parameters.put("tenantIdIn", tenantIds);
    parameters.put("assigneeIn", assigneeIn);
    parameters.put("assigneeNotIn", assigneeNotIn);
    parameters.put("processInstanceIdIn", processInstanceIds);

    return parameters;
  }

  private Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("processInstanceBusinessKey", "aBusinessKey");
    parameters.put("processInstanceBusinessKeyLike", "aBusinessKeyLike");
    parameters.put("processDefinitionKey", "aProcDefKey");
    parameters.put("processDefinitionId", "aProcDefId");
    parameters.put("executionId", "anExecId");
    parameters.put("processDefinitionName", "aProcDefName");
    parameters.put("processDefinitionNameLike", "aProcDefNameLike");
    parameters.put("processInstanceId", "aProcInstId");
    parameters.put("assignee", "anAssignee");
    parameters.put("assigneeLike", "anAssigneeLike");
    parameters.put("candidateGroup", "aCandidateGroup");
    parameters.put("candidateGroupLike", "aCandidateGroupLike");
    parameters.put("candidateUser", "aCandidate");
    parameters.put("includeAssignedTasks", "false");
    parameters.put("taskId", "aTaskId");
    parameters.put("taskDefinitionKey", "aTaskDefKey");
    parameters.put("taskDefinitionKeyLike", "aTaskDefKeyLike");
    parameters.put("description", "aDesc");
    parameters.put("descriptionLike", "aDescLike");
    parameters.put("involvedUser", "anInvolvedPerson");
    parameters.put("name", "aName");
    parameters.put("nameNotEqual", "aNameNotEqual");
    parameters.put("nameLike", "aNameLike");
    parameters.put("nameNotLike", "aNameNotLike");
    parameters.put("owner", "anOwner");
    parameters.put("caseDefinitionKey", "aCaseDefKey");
    parameters.put("caseDefinitionId", "aCaseDefId");
    parameters.put("caseDefinitionName", "aCaseDefName");
    parameters.put("caseDefinitionNameLike", "aCaseDefNameLike");
    parameters.put("caseInstanceId", "anCaseInstanceId");
    parameters.put("caseInstanceBusinessKey", "aCaseInstanceBusinessKey");
    parameters.put("caseInstanceBusinessKeyLike", "aCaseInstanceBusinessKeyLike");
    parameters.put("caseExecutionId", "aCaseExecutionId");
    parameters.put("parentTaskId", "aParentTaskId");

    return parameters;
  }

  private Map<String, Boolean> getCompleteBooleanQueryParameters() {
    Map<String, Boolean> parameters = new HashMap<>();

    parameters.put("assigned", true);
    parameters.put("unassigned", true);
    parameters.put("active", true);
    parameters.put("suspended", true);
    parameters.put("withoutTenantId", true);
    parameters.put("withCandidateGroups", true);
    parameters.put("withoutCandidateGroups", true);
    parameters.put("withCandidateUsers", true);
    parameters.put("withoutCandidateUsers", true);
    parameters.put("withoutDueDate", true);
    parameters.put("withCommentAttachmentInfo", true);

    return parameters;
  }

  private void verifyStringParameterQueryInvocations() {
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();

    verify(mockQuery).processInstanceBusinessKey(stringQueryParameters.get("processInstanceBusinessKey"));
    verify(mockQuery).processInstanceBusinessKeyLike(stringQueryParameters.get("processInstanceBusinessKeyLike"));
    verify(mockQuery).processDefinitionKey(stringQueryParameters.get("processDefinitionKey"));
    verify(mockQuery).processDefinitionId(stringQueryParameters.get("processDefinitionId"));
    verify(mockQuery).executionId(stringQueryParameters.get("executionId"));
    verify(mockQuery).processDefinitionName(stringQueryParameters.get("processDefinitionName"));
    verify(mockQuery).processDefinitionNameLike(stringQueryParameters.get("processDefinitionNameLike"));
    verify(mockQuery).processInstanceId(stringQueryParameters.get("processInstanceId"));
    verify(mockQuery).taskAssignee(stringQueryParameters.get("assignee"));
    verify(mockQuery).taskAssigneeLike(stringQueryParameters.get("assigneeLike"));
    verify(mockQuery).taskCandidateGroup(stringQueryParameters.get("candidateGroup"));
    verify(mockQuery).taskCandidateGroupLike(stringQueryParameters.get("candidateGroupLike"));
    verify(mockQuery).taskCandidateUser(stringQueryParameters.get("candidateUser"));
    verify(mockQuery).taskDefinitionKey(stringQueryParameters.get("taskDefinitionKey"));
    verify(mockQuery).taskDefinitionKeyLike(stringQueryParameters.get("taskDefinitionKeyLike"));
    verify(mockQuery).taskDescription(stringQueryParameters.get("description"));
    verify(mockQuery).taskDescriptionLike(stringQueryParameters.get("descriptionLike"));
    verify(mockQuery).taskInvolvedUser(stringQueryParameters.get("involvedUser"));
    verify(mockQuery).taskName(stringQueryParameters.get("name"));
    verify(mockQuery).taskNameNotEqual(stringQueryParameters.get("nameNotEqual"));
    verify(mockQuery).taskNameLike(stringQueryParameters.get("nameLike"));
    verify(mockQuery).taskNameNotLike(stringQueryParameters.get("nameNotLike"));
    verify(mockQuery).taskOwner(stringQueryParameters.get("owner"));
    verify(mockQuery).caseDefinitionKey(stringQueryParameters.get("caseDefinitionKey"));
    verify(mockQuery).caseDefinitionId(stringQueryParameters.get("caseDefinitionId"));
    verify(mockQuery).caseDefinitionName(stringQueryParameters.get("caseDefinitionName"));
    verify(mockQuery).caseDefinitionNameLike(stringQueryParameters.get("caseDefinitionNameLike"));
    verify(mockQuery).caseInstanceId(stringQueryParameters.get("caseInstanceId"));
    verify(mockQuery).caseInstanceBusinessKey(stringQueryParameters.get("caseInstanceBusinessKey"));
    verify(mockQuery).caseInstanceBusinessKeyLike(stringQueryParameters.get("caseInstanceBusinessKeyLike"));
    verify(mockQuery).caseExecutionId(stringQueryParameters.get("caseExecutionId"));
    verify(mockQuery).taskParentTaskId(stringQueryParameters.get("parentTaskId"));

  }

  private void verifyStringArrayParametersInvocations() {
    Map<String, String[]> stringArrayParameters = getCompleteStringArrayQueryParameters();

    verify(mockQuery).activityInstanceIdIn(stringArrayParameters.get("activityInstanceIdIn"));
    verify(mockQuery).taskDefinitionKeyIn(stringArrayParameters.get("taskDefinitionKeyIn"));
    verify(mockQuery).taskDefinitionKeyNotIn(stringArrayParameters.get("taskDefinitionKeyNotIn"));
    verify(mockQuery).processDefinitionKeyIn(stringArrayParameters.get("processDefinitionKeyIn"));
    verify(mockQuery).processInstanceBusinessKeyIn(stringArrayParameters.get("processInstanceBusinessKeyIn"));
    verify(mockQuery).tenantIdIn(stringArrayParameters.get("tenantIdIn"));
    verify(mockQuery).taskIdIn(stringArrayParameters.get("taskIdIn"));
    verify(mockQuery).taskAssigneeIn(stringArrayParameters.get("assigneeIn"));
    verify(mockQuery).taskAssigneeNotIn(stringArrayParameters.get("assigneeNotIn"));
    verify(mockQuery).processInstanceIdIn(stringArrayParameters.get("processInstanceIdIn"));
  }

  private void verifyBooleanParameterQueryInvocation() {
    verify(mockQuery).taskUnassigned();
    verify(mockQuery).active();
    verify(mockQuery).suspended();
    verify(mockQuery).withoutTenantId();
    verify(mockQuery).withCandidateGroups();
    verify(mockQuery).withoutCandidateGroups();
    verify(mockQuery).withCandidateUsers();
    verify(mockQuery).withoutCandidateUsers();
    verify(mockQuery).withoutDueDate();
    verify(mockQuery).withCommentAttachmentInfo();
  }

  @Test
  void testDateParameters() {
    Map<String, String> queryParameters = getDateParameters();

    given().queryParams(queryParameters)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verify(mockQuery).dueAfter(any(Date.class));
    verify(mockQuery).dueBefore(any(Date.class));
    verify(mockQuery).dueDate(any(Date.class));
    verify(mockQuery).followUpAfter(any(Date.class));
    verify(mockQuery).followUpBefore(any(Date.class));
    verify(mockQuery).followUpBeforeOrNotExistent(any(Date.class));
    verify(mockQuery).followUpDate(any(Date.class));
    verify(mockQuery).taskCreatedAfter(any(Date.class));
    verify(mockQuery).taskCreatedBefore(any(Date.class));
    verify(mockQuery).taskCreatedOn(any(Date.class));
    verify(mockQuery).taskUpdatedAfter(any(Date.class));
  }

  @Test
  void testDateParametersPost() {
    Map<String, String> json = getDateParameters();

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).dueAfter(any(Date.class));
    verify(mockQuery).dueBefore(any(Date.class));
    verify(mockQuery).dueDate(any(Date.class));
    verify(mockQuery).followUpAfter(any(Date.class));
    verify(mockQuery).followUpBefore(any(Date.class));
    verify(mockQuery).followUpBeforeOrNotExistent(any(Date.class));
    verify(mockQuery).followUpDate(any(Date.class));
    verify(mockQuery).taskCreatedAfter(any(Date.class));
    verify(mockQuery).taskCreatedBefore(any(Date.class));
    verify(mockQuery).taskCreatedOn(any(Date.class));
    verify(mockQuery).taskUpdatedAfter(any(Date.class));
  }

  @Test
  void testDeprecatedDateParameters() {
    Map<String, String> queryParameters = new HashMap<>();
    queryParameters.put("due", withTimezone("2013-01-23T14:42:44"));
    queryParameters.put("created", withTimezone("2013-01-23T14:42:47"));
    queryParameters.put("followUp", withTimezone("2013-01-23T14:42:50"));

    given()
      .queryParams(queryParameters)
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(TASK_QUERY_URL);

    verify(mockQuery).dueDate(any(Date.class));
    verify(mockQuery).taskCreatedOn(any(Date.class));
    verify(mockQuery).followUpDate(any(Date.class));
  }

  private Map<String, String> getDateParameters() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("dueAfter", withTimezone("2013-01-23T14:42:42"));
    parameters.put("dueBefore", withTimezone("2013-01-23T14:42:43"));
    parameters.put("dueDate", withTimezone("2013-01-23T14:42:44"));
    parameters.put("createdAfter", withTimezone("2013-01-23T14:42:45"));
    parameters.put("createdBefore", withTimezone("2013-01-23T14:42:46"));
    parameters.put("createdOn", withTimezone("2013-01-23T14:42:47"));
    parameters.put("followUpAfter", withTimezone("2013-01-23T14:42:48"));
    parameters.put("followUpBefore", withTimezone("2013-01-23T14:42:49"));
    parameters.put("followUpBeforeOrNotExistent", withTimezone("2013-01-23T14:42:49"));
    parameters.put("followUpDate", withTimezone("2013-01-23T14:42:50"));
    parameters.put("updatedAfter", withTimezone("2013-01-23T14:42:50"));
    return parameters;
  }

  @Test
  void testCandidateGroupInList() {
    List<String> candidateGroups = new ArrayList<>();
    candidateGroups.add("boss");
    candidateGroups.add("worker");
    String queryParam = candidateGroups.get(0) + "," + candidateGroups.get(1);

    given().queryParams("candidateGroups", queryParam)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verify(mockQuery).taskCandidateGroupIn(argThat(new EqualsList(candidateGroups)));
  }

  @Test
  void testDelegationState() {
    given().queryParams("delegationState", "PENDING")
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verify(mockQuery).taskDelegationState(DelegationState.PENDING);

    given().queryParams("delegationState", "RESOLVED")
    .header("accept", MediaType.APPLICATION_JSON)
    .expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery).taskDelegationState(DelegationState.RESOLVED);
  }

  @Test
  void testLowerCaseDelegationStateParam() {
    given().queryParams("delegationState", "resolved")
    .header("accept", MediaType.APPLICATION_JSON)
    .expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery).taskDelegationState(DelegationState.RESOLVED);
  }

  @Test
  void testSortingParameters() {
    InOrder inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("dueDate", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByDueDate();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("followUpDate", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByFollowUpDate();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("instanceId", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessInstanceId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("created", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskCreateTime();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("id", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("priority", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskPriority();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("executionId", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByExecutionId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("assignee", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskAssignee();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("description", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskDescription();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("name", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskName();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("nameCaseInsensitive", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskNameCaseInsensitive();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("caseInstanceId", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByCaseInstanceId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("dueDate", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByDueDate();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("followUpDate", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByFollowUpDate();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("instanceId", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByProcessInstanceId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("created", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskCreateTime();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("id", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("priority", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskPriority();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("executionId", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByExecutionId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("assignee", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskAssignee();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("description", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskDescription();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("name", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskName();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("nameCaseInsensitive", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTaskNameCaseInsensitive();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("caseInstanceId", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByCaseInstanceId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("tenantId", "desc", Status.OK);
    inOrder.verify(mockQuery).orderByTenantId();
    inOrder.verify(mockQuery).desc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("tenantId", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByTenantId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySorting("lastUpdated", "asc", Status.OK);
    inOrder.verify(mockQuery).orderByLastUpdated();
    inOrder.verify(mockQuery).asc();

  }

  protected void executeAndVerifySorting(String sortBy, String sortOrder, Status expectedStatus) {
    given().queryParam("sortBy", sortBy).queryParam("sortOrder", sortOrder)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(expectedStatus.getStatusCode())
      .when().get(TASK_QUERY_URL);
  }

  protected void executeAndVerifySortingAsPost(List<Map<String, Object>> sortingJson, Status expectedStatus) {
    Map<String, Object> json = new HashMap<>();
    json.put("sorting", sortingJson);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(expectedStatus.getStatusCode())
      .when().post(TASK_QUERY_URL);
  }

  @Test
  void testSecondarySortingAsPost() {
    InOrder inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySortingAsPost(
      OrderingBuilder.create()
        .orderBy("dueDate").desc()
        .orderBy("caseExecutionId").asc()
        .getJson(),
      Status.OK);

    inOrder.verify(mockQuery).orderByDueDate();
    inOrder.verify(mockQuery).desc();
    inOrder.verify(mockQuery).orderByCaseExecutionId();
    inOrder.verify(mockQuery).asc();

    inOrder = Mockito.inOrder(mockQuery);
    executeAndVerifySortingAsPost(
      OrderingBuilder.create()
        .orderBy("processVariable").desc()
          .parameter("variable", "var")
          .parameter("type", "String")
        .orderBy("executionVariable").asc()
          .parameter("variable", "var2")
          .parameter("type", "Integer")
        .orderBy("taskVariable").desc()
          .parameter("variable", "var3")
          .parameter("type", "Double")
        .orderBy("caseInstanceVariable").asc()
          .parameter("variable", "var4")
          .parameter("type", "Long")
        .orderBy("caseExecutionVariable").desc()
          .parameter("variable", "var5")
          .parameter("type", "Date")
        .getJson(),
      Status.OK);

    inOrder.verify(mockQuery).orderByProcessVariable("var", ValueType.STRING);
    inOrder.verify(mockQuery).desc();
    inOrder.verify(mockQuery).orderByExecutionVariable("var2", ValueType.INTEGER);
    inOrder.verify(mockQuery).asc();
    inOrder.verify(mockQuery).orderByTaskVariable("var3", ValueType.DOUBLE);
    inOrder.verify(mockQuery).desc();
    inOrder.verify(mockQuery).orderByCaseInstanceVariable("var4", ValueType.LONG);
    inOrder.verify(mockQuery).asc();
    inOrder.verify(mockQuery).orderByCaseExecutionVariable("var5", ValueType.DATE);
    inOrder.verify(mockQuery).desc();
  }

  @Test
  void testSuccessfulPagination() {

    int firstResult = 0;
    int maxResults = 10;
    given().queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verify(mockQuery).listPage(firstResult, maxResults);
  }

  @ParameterizedTest
  @MethodSource("variableParameterProvider")
  void testTaskVariableParameters(String operator, boolean variableNamesIgnoreCase, boolean variableValuesIgnoreCase) {
    // clear previous interactions but keep stubbing
    clearInvocations(mockQuery);

    String queryValue = SAMPLE_VAR_NAME + "_" + operator + "_" + SAMPLE_VAR_VALUE;

    var request = given().queryParam("taskVariables", queryValue);
    if (variableValuesIgnoreCase) {
      request = request.queryParam("variableValuesIgnoreCase", true);
    }
    if (variableNamesIgnoreCase) {
      request = request.queryParam("variableNamesIgnoreCase", true);
    }

    request.header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    if (variableValuesIgnoreCase) {
      verify(mockQuery).matchVariableValuesIgnoreCase();
    }
    if (variableNamesIgnoreCase) {
      verify(mockQuery).matchVariableNamesIgnoreCase();
    }

    switch (operator) {
    case "eq":
      verify(mockQuery).taskVariableValueEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE);
      break;
    case "gt":
      verify(mockQuery).taskVariableValueGreaterThan(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE);
      break;
    case "gteq":
      verify(mockQuery).taskVariableValueGreaterThanOrEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE);
      break;
    case "lt":
      verify(mockQuery).taskVariableValueLessThan(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE);
      break;
    case "lteq":
      verify(mockQuery).taskVariableValueLessThanOrEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE);
      break;
    case "like":
      verify(mockQuery).taskVariableValueLike(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE);
      break;
    case "neq":
      verify(mockQuery).taskVariableValueNotEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE);
      break;
    default:
      throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }

  static Stream<Arguments> variableParameterProvider() {
    return Stream.of(
      // equals variations (case-insensitive combos)
      Arguments.of("eq", false, false),
      Arguments.of("eq", false, true),
      Arguments.of("eq", true, false),
      Arguments.of("eq", true, true),
      // numeric / comparative operators
      Arguments.of("gt", false, false),
      Arguments.of("gteq", false, false),
      Arguments.of("lt", false, false),
      Arguments.of("lteq", false, false),
      // like (with and without value-ignore-case)
      Arguments.of("like", false, false),
      Arguments.of("like", false, true),
      // not equals (with and without value-ignore-case)
      Arguments.of("neq", false, false),
      Arguments.of("neq", false, true)
    );
  }
  @Test
  void testTaskVariableValueEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "eq");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("taskVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).taskVariableValueEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testTaskVariableNameEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME.toLowerCase());
    variableJson.put("operator", "eq");
    variableJson.put("value", SAMPLE_VAR_VALUE);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("taskVariables", variables);
    json.put("variableNamesIgnoreCase", true);

    given()
    .contentType(POST_JSON_CONTENT_TYPE)
    .body(json)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).taskVariableValueEquals(SAMPLE_VAR_NAME.toLowerCase(), SAMPLE_VAR_VALUE);
    reset(mockQuery);

    json.put("variableValuesIgnoreCase", true);

    given()
    .contentType(POST_JSON_CONTENT_TYPE)
    .body(json)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).taskVariableValueEquals(SAMPLE_VAR_NAME.toLowerCase(), SAMPLE_VAR_VALUE);
  }

  @Test
  void testTaskVariableValueNotEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "neq");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("taskVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).taskVariableValueNotEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testTaskVariableValueLikeIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "like");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("taskVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).taskVariableValueLike(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testProcessVariableParameters() {
    // equals
    String variableName = "varName";
    String variableValue = "varValue";
    String queryValue = variableName + "_eq_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueEquals(variableName, variableValue);
    reset(mockQuery);

    //equals case-insensitive
    queryValue = variableName + "_eq_" + variableValue;

    given()
    .queryParam("processVariables", queryValue)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueEquals(variableName, variableValue);
    reset(mockQuery);

    given()
    .queryParam("processVariables", queryValue)
    .queryParam("variableNamesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).processVariableValueEquals(variableName, variableValue);
    reset(mockQuery);

    given()
    .queryParam("processVariables", queryValue)
    .queryParam("variableNamesIgnoreCase", true)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueEquals(variableName, variableValue);

    // greater than
    queryValue = variableName + "_gt_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueGreaterThan(variableName, variableValue);

    // greater than equals
    queryValue = variableName + "_gteq_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueGreaterThanOrEquals(variableName, variableValue);

    // lower than
    queryValue = variableName + "_lt_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueLessThan(variableName, variableValue);

    // lower than equals
    queryValue = variableName + "_lteq_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueLessThanOrEquals(variableName, variableValue);

    // like
    queryValue = variableName + "_like_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueLike(variableName, variableValue);
    reset(mockQuery);

    // like case-insensitive
    queryValue = variableName + "_like_" + variableValue;

    given()
    .queryParam("processVariables", queryValue)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueLike(variableName, variableValue);

    // not equals
    queryValue = variableName + "_neq_" + variableValue;

    given()
      .queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueNotEquals(variableName, variableValue);
    reset(mockQuery);

    // not equals case-insensitive
    queryValue = variableName + "_neq_" + variableValue;

    given()
    .queryParam("processVariables", queryValue)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueNotEquals(variableName, variableValue);
  }

  @Test
  void testProcessVariableValueEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "eq");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("processVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testProcessVariableNameEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME.toLowerCase());
    variableJson.put("operator", "eq");
    variableJson.put("value", SAMPLE_VAR_VALUE);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("processVariables", variables);
    json.put("variableNamesIgnoreCase", true);

    given()
    .contentType(POST_JSON_CONTENT_TYPE)
    .body(json)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).processVariableValueEquals(SAMPLE_VAR_NAME.toLowerCase(), SAMPLE_VAR_VALUE);
    reset(mockQuery);

    json.put("variableValuesIgnoreCase", true);
    given()
    .contentType(POST_JSON_CONTENT_TYPE)
    .body(json)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueEquals(SAMPLE_VAR_NAME.toLowerCase(), SAMPLE_VAR_VALUE);

  }

  @Test
  void testProcessVariableValueNotEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "neq");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("processVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueNotEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testProcessVariableValueLikeIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "like");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("processVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueLike(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testProcessVariableValueNotLikeIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "notLike");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("processVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).processVariableValueNotLike(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testCaseVariableParameters() {
    // equals
    String variableName = "varName";
    String variableValue = "varValue";
    String queryValue = variableName + "_eq_" + variableValue;

    given()
      .queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueEquals(variableName, variableValue);
    reset(mockQuery);

    // equals case-insensitive
    queryValue = variableName + "_eq_" + variableValue;

    given()
    .queryParam("caseInstanceVariables", queryValue)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueEquals(variableName, variableValue);
    reset(mockQuery);

    given()
    .queryParam("caseInstanceVariables", queryValue)
    .queryParam("variableNamesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueEquals(variableName, variableValue);
    reset(mockQuery);

    given()
    .queryParam("caseInstanceVariables", queryValue)
    .queryParam("variableNamesIgnoreCase", true)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueEquals(variableName, variableValue);

    // greater than
    queryValue = variableName + "_gt_" + variableValue;

    given()
      .queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueGreaterThan(variableName, variableValue);

    // greater than equals
    queryValue = variableName + "_gteq_" + variableValue;

    given()
      .queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueGreaterThanOrEquals(variableName, variableValue);

    // lower than
    queryValue = variableName + "_lt_" + variableValue;

    given()
      .queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueLessThan(variableName, variableValue);

    // lower than equals
    queryValue = variableName + "_lteq_" + variableValue;

    given()
      .queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueLessThanOrEquals(variableName, variableValue);

    // like
    queryValue = variableName + "_like_" + variableValue;

    given()
      .queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueLike(variableName, variableValue);
    reset(mockQuery);

    // like case-insensitive
    queryValue = variableName + "_like_" + variableValue;

    given()
    .queryParam("caseInstanceVariables", queryValue)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueLike(variableName, variableValue);

    // not equals
    queryValue = variableName + "_neq_" + variableValue;

    given()
      .queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueNotEquals(variableName, variableValue);
    reset(mockQuery);

    // not equals case-insensitive
    queryValue = variableName + "_neq_" + variableValue;

    given()
    .queryParam("caseInstanceVariables", queryValue)
    .queryParam("variableValuesIgnoreCase", true)
    .header("accept", MediaType.APPLICATION_JSON)
    .then()
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .get(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueNotEquals(variableName, variableValue);
  }

  @Test
  void testCaseInstanceVariableValueEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "eq");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testCaseInstanceVariableNameEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME.toLowerCase());
    variableJson.put("operator", "eq");
    variableJson.put("value", SAMPLE_VAR_VALUE);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceVariables", variables);
    json.put("variableNamesIgnoreCase", true);

    given()
    .contentType(POST_JSON_CONTENT_TYPE)
    .body(json)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueEquals(SAMPLE_VAR_NAME.toLowerCase(), SAMPLE_VAR_VALUE);
    reset(mockQuery);

    json.put("variableValuesIgnoreCase", true);
    given()
    .contentType(POST_JSON_CONTENT_TYPE)
    .body(json)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect()
    .statusCode(Status.OK.getStatusCode())
    .when()
    .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableNamesIgnoreCase();
    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueEquals(SAMPLE_VAR_NAME.toLowerCase(), SAMPLE_VAR_VALUE);
  }

  @Test
  void testCaseInstanceVariableValueNotEqualsIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "neq");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueNotEquals(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testCaseInstanceVariableValueLikeIgnoreCaseAsPost() {
    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", SAMPLE_VAR_NAME);
    variableJson.put("operator", "like");
    variableJson.put("value", SAMPLE_VAR_VALUE.toLowerCase());

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceVariables", variables);
    json.put("variableValuesIgnoreCase", true);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).matchVariableValuesIgnoreCase();
    verify(mockQuery).caseInstanceVariableValueLike(SAMPLE_VAR_NAME, SAMPLE_VAR_VALUE.toLowerCase());
  }

  @Test
  void testMultipleVariableParameters() {
    String variableName1 = "varName";
    String variableValue1 = "varValue";
    String variableParameter1 = variableName1 + "_eq_" + variableValue1;

    String variableName2 = "anotherVarName";
    String variableValue2 = "anotherVarValue";
    String variableParameter2 = variableName2 + "_neq_" + variableValue2;

    String queryValue = variableParameter1 + "," + variableParameter2;

    given().queryParam("taskVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verify(mockQuery).taskVariableValueEquals(variableName1, variableValue1);
    verify(mockQuery).taskVariableValueNotEquals(variableName2, variableValue2);
  }

  @Test
  void testMultipleVariableParametersAsPost() {
    String variableName = "varName";
    String variableValue = "varValue";
    String anotherVariableName = "anotherVarName";
    Integer anotherVariableValue = 30;

    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", variableName);
    variableJson.put("operator", "eq");
    variableJson.put("value", variableValue);

    Map<String, Object> anotherVariableJson = new HashMap<>();
    anotherVariableJson.put("name", anotherVariableName);
    anotherVariableJson.put("operator", "neq");
    anotherVariableJson.put("value", anotherVariableValue);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);
    variables.add(anotherVariableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("taskVariables", variables);

    given().contentType(POST_JSON_CONTENT_TYPE).body(json)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().post(TASK_QUERY_URL);

    verify(mockQuery).taskVariableValueEquals(variableName, variableValue);
    verify(mockQuery).taskVariableValueNotEquals(eq(anotherVariableName), argThat(EqualsPrimitiveValue.numberValue(anotherVariableValue)));

  }

  @Test
  void testMultipleProcessVariableParameters() {
    String variableName1 = "varName";
    String variableValue1 = "varValue";
    String variableParameter1 = variableName1 + "_eq_" + variableValue1;

    String variableName2 = "anotherVarName";
    String variableValue2 = "anotherVarValue";
    String variableParameter2 = variableName2 + "_neq_" + variableValue2;

    String queryValue = variableParameter1 + "," + variableParameter2;

    given().queryParam("processVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueEquals(variableName1, variableValue1);
    verify(mockQuery).processVariableValueNotEquals(variableName2, variableValue2);
  }

  @Test
  void testMultipleProcessVariableParametersAsPost() {
    String variableName = "varName";
    String variableValue = "varValue";
    String anotherVariableName = "anotherVarName";
    Integer anotherVariableValue = 30;

    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", variableName);
    variableJson.put("operator", "eq");
    variableJson.put("value", variableValue);

    Map<String, Object> anotherVariableJson = new HashMap<>();
    anotherVariableJson.put("name", anotherVariableName);
    anotherVariableJson.put("operator", "neq");
    anotherVariableJson.put("value", anotherVariableValue);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);
    variables.add(anotherVariableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("processVariables", variables);

    given()
      .header("accept", MediaType.APPLICATION_JSON)
      .contentType(POST_JSON_CONTENT_TYPE)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).processVariableValueEquals(variableName, variableValue);
    verify(mockQuery).processVariableValueNotEquals(eq(anotherVariableName), argThat(EqualsPrimitiveValue.numberValue(anotherVariableValue)));
  }

  @Test
  void testMultipleCaseVariableParameters() {
    String variableName1 = "varName";
    String variableValue1 = "varValue";
    String variableParameter1 = variableName1 + "_eq_" + variableValue1;

    String variableName2 = "anotherVarName";
    String variableValue2 = "anotherVarValue";
    String variableParameter2 = variableName2 + "_neq_" + variableValue2;

    String queryValue = variableParameter1 + "," + variableParameter2;

    given().queryParam("caseInstanceVariables", queryValue)
      .header("accept", MediaType.APPLICATION_JSON)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueEquals(variableName1, variableValue1);
    verify(mockQuery).caseInstanceVariableValueNotEquals(variableName2, variableValue2);
  }

  @Test
  void testMultipleCaseVariableParametersAsPost() {
    String variableName = "varName";
    String variableValue = "varValue";
    String anotherVariableName = "anotherVarName";
    Integer anotherVariableValue = 30;

    Map<String, Object> variableJson = new HashMap<>();
    variableJson.put("name", variableName);
    variableJson.put("operator", "eq");
    variableJson.put("value", variableValue);

    Map<String, Object> anotherVariableJson = new HashMap<>();
    anotherVariableJson.put("name", anotherVariableName);
    anotherVariableJson.put("operator", "neq");
    anotherVariableJson.put("value", anotherVariableValue);

    List<Map<String, Object>> variables = new ArrayList<>();
    variables.add(variableJson);
    variables.add(anotherVariableJson);

    Map<String, Object> json = new HashMap<>();
    json.put("caseInstanceVariables", variables);

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .header("accept", MediaType.APPLICATION_JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .post(TASK_QUERY_URL);

    verify(mockQuery).caseInstanceVariableValueEquals(variableName, variableValue);
    verify(mockQuery).caseInstanceVariableValueNotEquals(eq(anotherVariableName), argThat(EqualsPrimitiveValue.numberValue(anotherVariableValue)));
  }

  @Test
  void testCompletePostParameters() {

    Map<String, Object> queryParameters = new HashMap<>();
    Map<String, String> stringQueryParameters = getCompleteStringQueryParameters();
    Map<String, Integer> intQueryParameters = getCompleteIntQueryParameters();
    Map<String, Boolean> booleanQueryParameters = getCompleteBooleanQueryParameters();
    Map<String, String[]> stringArrayQueryParameters = getCompleteStringArrayQueryParameters();

    queryParameters.putAll(stringQueryParameters);
    queryParameters.putAll(intQueryParameters);
    queryParameters.putAll(booleanQueryParameters);
    queryParameters.putAll(stringArrayQueryParameters);

    List<String> candidateGroups = new ArrayList<>();
    candidateGroups.add("boss");
    candidateGroups.add("worker");

    queryParameters.put("candidateGroups", candidateGroups);

    queryParameters.put("includeAssignedTasks", true);

    given().contentType(POST_JSON_CONTENT_TYPE).body(queryParameters)
      .header("accept", MediaType.APPLICATION_JSON)
      .expect().statusCode(Status.OK.getStatusCode())
      .when().post(TASK_QUERY_URL);

    verifyStringParameterQueryInvocations();
    verifyIntegerParameterQueryInvocations();
    verifyStringArrayParametersInvocations();
    verifyBooleanParameterQueryInvocation();

    verify(mockQuery).includeAssignedTasks();
    verify(mockQuery).taskCandidateGroupIn(argThat(new EqualsList(candidateGroups)));
  }

  @Test
  void testQueryCount() {
    given()
        .header("accept", MediaType.APPLICATION_JSON)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
      .when()
        .get(TASK_COUNT_QUERY_URL);

    verify(mockQuery).count();
  }

  @Test
  void testQueryCountForPost() {
    given().contentType(POST_JSON_CONTENT_TYPE).body(EMPTY_JSON_OBJECT)
    .header("accept", MediaType.APPLICATION_JSON)
    .expect().statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
      .when().post(TASK_COUNT_QUERY_URL);

    verify(mockQuery).count();
  }

  @Test
  void testQueryWithExpressions() {
    String testExpression = "${'test-%s'}";

    ValueGenerator generator = new ValueGenerator(testExpression);

    Map<String, String> params = new HashMap<>();
    params.put("assigneeExpression", generator.getValue("assigneeExpression"));
    params.put("assigneeLikeExpression", generator.getValue("assigneeLikeExpression"));
    params.put("ownerExpression", generator.getValue("ownerExpression"));
    params.put("involvedUserExpression", generator.getValue("involvedUserExpression"));
    params.put("candidateUserExpression", generator.getValue("candidateUserExpression"));
    params.put("candidateGroupExpression", generator.getValue("candidateGroupExpression"));
    params.put("candidateGroupsExpression", generator.getValue("candidateGroupsExpression"));
    params.put("createdBeforeExpression", generator.getValue("createdBeforeExpression"));
    params.put("createdOnExpression", generator.getValue("createdOnExpression"));
    params.put("createdAfterExpression", generator.getValue("createdAfterExpression"));
    params.put("dueBeforeExpression", generator.getValue("dueBeforeExpression"));
    params.put("dueDateExpression", generator.getValue("dueDateExpression"));
    params.put("dueAfterExpression", generator.getValue("dueAfterExpression"));
    params.put("followUpBeforeExpression", generator.getValue("followUpBeforeExpression"));
    params.put("followUpDateExpression", generator.getValue("followUpDateExpression"));
    params.put("followUpAfterExpression", generator.getValue("followUpAfterExpression"));
    params.put("processInstanceBusinessKeyExpression", generator.getValue("processInstanceBusinessKeyExpression"));
    params.put("processInstanceBusinessKeyLikeExpression", generator.getValue("processInstanceBusinessKeyLikeExpression"));
    params.put("lastUpdatedExpression", generator.getValue("lastUpdatedExpressionExpression"));

    // get
    given()
      .header(ACCEPT_JSON_HEADER)
      .queryParams(params)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(TASK_QUERY_URL);

    verifyExpressionMocks(generator);

    // reset mock
    reset(mockQuery);

    // post
    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .header(ACCEPT_JSON_HEADER)
      .body(params)
    .expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(TASK_QUERY_URL);

    verifyExpressionMocks(generator);

  }

  protected void verifyExpressionMocks(ValueGenerator generator) {
    verify(mockQuery).taskAssigneeExpression(generator.getValue("assigneeExpression"));
    verify(mockQuery).taskAssigneeLikeExpression(generator.getValue("assigneeLikeExpression"));
    verify(mockQuery).taskOwnerExpression(generator.getValue("ownerExpression"));
    verify(mockQuery).taskInvolvedUserExpression(generator.getValue("involvedUserExpression"));
    verify(mockQuery).taskCandidateUserExpression(generator.getValue("candidateUserExpression"));
    verify(mockQuery).taskCandidateGroupExpression(generator.getValue("candidateGroupExpression"));
    verify(mockQuery).taskCandidateGroupInExpression(generator.getValue("candidateGroupsExpression"));
    verify(mockQuery).taskCreatedBeforeExpression(generator.getValue("createdBeforeExpression"));
    verify(mockQuery).taskCreatedOnExpression(generator.getValue("createdOnExpression"));
    verify(mockQuery).taskCreatedAfterExpression(generator.getValue("createdAfterExpression"));
    verify(mockQuery).dueBeforeExpression(generator.getValue("dueBeforeExpression"));
    verify(mockQuery).dueDateExpression(generator.getValue("dueDateExpression"));
    verify(mockQuery).dueAfterExpression(generator.getValue("dueAfterExpression"));
    verify(mockQuery).followUpBeforeExpression(generator.getValue("followUpBeforeExpression"));
    verify(mockQuery).followUpDateExpression(generator.getValue("followUpDateExpression"));
    verify(mockQuery).followUpAfterExpression(generator.getValue("followUpAfterExpression"));
    verify(mockQuery).processInstanceBusinessKeyExpression(generator.getValue("processInstanceBusinessKeyExpression"));
    verify(mockQuery).processInstanceBusinessKeyLikeExpression(generator.getValue("processInstanceBusinessKeyLikeExpression"));

  }

  @Test
  void testQueryWithCandidateUsers() {
    given().queryParam("withCandidateUsers", true)
    .accept(MediaType.APPLICATION_JSON)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery).withCandidateUsers();
  }

  @Test
  void testQueryWithoutCandidateUsers() {
    given().queryParam("withoutCandidateUsers", true)
    .accept(MediaType.APPLICATION_JSON)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery).withoutCandidateUsers();
  }

  @Test
  void testNeverQueryWithCandidateUsers() {
    given().queryParam("withCandidateUsers", false)
    .accept(MediaType.APPLICATION_JSON)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery, never()).withCandidateUsers();
  }

  @Test
  void testNeverQueryWithoutCandidateUsers() {
    given().queryParam("withoutCandidateUsers", false)
    .accept(MediaType.APPLICATION_JSON)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery, never()).withoutCandidateUsers();
  }

  @Test
  void testNeverQueryWithCandidateGroups() {
    given().queryParam("withCandidateGroups", false)
    .accept(MediaType.APPLICATION_JSON)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery, never()).withCandidateGroups();
  }

  @Test
  void testNeverQueryWithoutCandidateGroups() {
    given().queryParam("withoutCandidateGroups", false)
    .accept(MediaType.APPLICATION_JSON)
    .then().expect().statusCode(Status.OK.getStatusCode())
    .when().get(TASK_QUERY_URL);

    verify(mockQuery, never()).withoutCandidateGroups();
  }

  @Test
  void testOrQuery() {
    TaskQueryDto queryDto = TaskQueryDto.fromQuery(new TaskQueryImpl()
      .or()
        .taskName(MockProvider.EXAMPLE_TASK_NAME)
        .taskDescription(MockProvider.EXAMPLE_TASK_DESCRIPTION)
      .endOr());

    given()
      .contentType(POST_JSON_CONTENT_TYPE)
      .header(ACCEPT_JSON_HEADER)
      .body(queryDto)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(TASK_QUERY_URL);

    ArgumentCaptor<TaskQueryImpl> argument = ArgumentCaptor.forClass(TaskQueryImpl.class);
    verify((TaskQueryImpl) mockQuery).addOrQuery(argument.capture());
    assertEquals(MockProvider.EXAMPLE_TASK_NAME, argument.getValue().getName());
    assertEquals(MockProvider.EXAMPLE_TASK_DESCRIPTION, argument.getValue().getDescription());
  }

}
