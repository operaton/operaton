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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.HistoryServiceImpl;
import org.operaton.bpm.engine.impl.ManagementServiceImpl;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.management.SetJobRetriesByProcessAsyncBuilder;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceSuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.runtime.batch.CorrelationMessageAsyncDto;
import org.operaton.bpm.engine.rest.dto.runtime.batch.DeleteProcessInstancesDto;
import org.operaton.bpm.engine.rest.dto.runtime.batch.SetVariablesAsyncDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.EqualsList;
import org.operaton.bpm.engine.rest.helper.EqualsMap;
import org.operaton.bpm.engine.rest.helper.ErrorMessageHelper;
import org.operaton.bpm.engine.rest.helper.ExampleVariableObject;
import org.operaton.bpm.engine.rest.helper.MockObjectValue;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.helper.VariableTypeHelper;
import org.operaton.bpm.engine.rest.helper.variable.EqualsNullValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsObjectValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsPrimitiveValue;
import org.operaton.bpm.engine.rest.helper.variable.EqualsUntypedValue;
import org.operaton.bpm.engine.rest.util.JsonPathUtil;
import org.operaton.bpm.engine.rest.util.ModificationInstructionBuilder;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.MessageCorrelationAsyncBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceModificationInstantiationBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.UpdateProcessInstanceSuspensionStateSelectBuilder;
import org.operaton.bpm.engine.runtime.UpdateProcessInstanceSuspensionStateTenantBuilder;
import org.operaton.bpm.engine.runtime.UpdateProcessInstancesSuspensionStateBuilder;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.SerializableValueType;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.LongValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE;
import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_PROCESS_INSTANCE_COMMENT_ID;
import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_TASK_ID;
import static org.operaton.bpm.engine.rest.helper.MockProvider.NON_EXISTING_ID;
import static org.operaton.bpm.engine.rest.helper.MockProvider.createMockBatch;
import static org.operaton.bpm.engine.rest.helper.MockProvider.createMockHistoricProcessInstance;
import static org.operaton.bpm.engine.rest.util.DateTimeUtils.DATE_FORMAT_WITH_TIMEZONE;
import static org.operaton.bpm.engine.rest.util.DateTimeUtils.withTimezone;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class ProcessInstanceRestServiceInteractionTest extends AbstractRestServiceTest {

  protected static final String TEST_DELETE_REASON = "test";
  protected static final String RETRIES = "retries";
  protected static final String FAIL_IF_NOT_EXISTS = "failIfNotExists";
  protected static final String DELETE_REASON = "deleteReason";
  protected static final String SKIP_IO_MAPPINGS = "skipIoMappings";

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String PROCESS_INSTANCE_URL = TEST_RESOURCE_ROOT_PATH + "/process-instance";
  protected static final String SINGLE_PROCESS_INSTANCE_URL = PROCESS_INSTANCE_URL + "/{id}";
  protected static final String PROCESS_INSTANCE_VARIABLES_URL = SINGLE_PROCESS_INSTANCE_URL + "/variables";
  protected static final String PROCESS_INSTANCE_COMMENTS_URL = SINGLE_PROCESS_INSTANCE_URL + "/comment";
  protected static final String SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL =
      PROCESS_INSTANCE_COMMENTS_URL + "/{commentId}";
  protected static final String DELETE_PROCESS_INSTANCES_ASYNC_URL = PROCESS_INSTANCE_URL + "/delete";
  protected static final String DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL = PROCESS_INSTANCE_URL + "/delete-historic-query-based";
  protected static final String SET_JOB_RETRIES_ASYNC_URL = PROCESS_INSTANCE_URL + "/job-retries";
  protected static final String SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL = PROCESS_INSTANCE_URL + "/job-retries-historic-query-based";
  protected static final String SINGLE_PROCESS_INSTANCE_VARIABLE_URL = PROCESS_INSTANCE_VARIABLES_URL + "/{varId}";
  protected static final String SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL = SINGLE_PROCESS_INSTANCE_VARIABLE_URL + "/data";
  protected static final String PROCESS_INSTANCE_ACTIVIY_INSTANCES_URL = SINGLE_PROCESS_INSTANCE_URL + "/activity-instances";
  protected static final String EXAMPLE_PROCESS_INSTANCE_ID_WITH_NULL_VALUE_AS_VARIABLE = "aProcessInstanceWithNullValueAsVariable";
  protected static final String SINGLE_PROCESS_INSTANCE_SUSPENDED_URL = SINGLE_PROCESS_INSTANCE_URL + "/suspended";
  protected static final String PROCESS_INSTANCE_SUSPENDED_URL = PROCESS_INSTANCE_URL + "/suspended";
  protected static final String PROCESS_INSTANCE_SUSPENDED_ASYNC_URL = PROCESS_INSTANCE_URL + "/suspended-async";
  protected static final String PROCESS_INSTANCE_MODIFICATION_URL = SINGLE_PROCESS_INSTANCE_URL + "/modification";
  protected static final String PROCESS_INSTANCE_MODIFICATION_ASYNC_URL = SINGLE_PROCESS_INSTANCE_URL + "/modification-async";
  protected static final String PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL = PROCESS_INSTANCE_URL + "/variables-async";
  protected static final String PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL = PROCESS_INSTANCE_URL + "/message-async";

  protected static final Answer<Object> RETURNS_SELF = invocation -> {
    Object mock = invocation.getMock();
    if(invocation.getMethod().getReturnType().isInstance(mock)){
        return mock;
    }
    return RETURNS_DEFAULTS.answer(invocation);
  };

  protected static final VariableMap EXAMPLE_OBJECT_VARIABLES = Variables.createVariables();
  static {
    ExampleVariableObject variableValue = new ExampleVariableObject();
    variableValue.setProperty1("aPropertyValue");
    variableValue.setProperty2(true);

    EXAMPLE_OBJECT_VARIABLES.putValueTyped(EXAMPLE_VARIABLE_KEY,
        MockObjectValue
          .fromObjectValue(Variables.objectValue(variableValue).serializationDataFormat("application/json").create())
          .objectTypeName(ExampleVariableObject.class.getName()));
  }

  private List<Comment> mockTaskComments;
  private HistoricProcessInstanceQuery historicProcessInstanceQueryMock;

  private RuntimeServiceImpl runtimeServiceMock;
  private ManagementServiceImpl mockManagementService;
  private HistoryServiceImpl historyServiceMock;
  private TaskService taskServiceMock;

  private UpdateProcessInstanceSuspensionStateTenantBuilder mockUpdateSuspensionStateBuilder;
  private UpdateProcessInstanceSuspensionStateSelectBuilder mockUpdateSuspensionStateSelectBuilder;
  private UpdateProcessInstancesSuspensionStateBuilder mockUpdateProcessInstancesSuspensionStateBuilder;

  private SetJobRetriesByProcessAsyncBuilder mockSetJobRetriesByProcessAsyncBuilder;

  @BeforeEach
  void setUpRuntimeData() {
    runtimeServiceMock = mock(RuntimeServiceImpl.class);
    mockManagementService = mock(ManagementServiceImpl.class);
    historyServiceMock = mock(HistoryServiceImpl.class);

    // variables
    when(runtimeServiceMock.getVariablesTyped(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, true)).thenReturn(EXAMPLE_VARIABLES);
    when(runtimeServiceMock.getVariablesTyped(MockProvider.ANOTHER_EXAMPLE_PROCESS_INSTANCE_ID, true)).thenReturn(EXAMPLE_OBJECT_VARIABLES);
    when(runtimeServiceMock.getVariablesTyped(EXAMPLE_PROCESS_INSTANCE_ID_WITH_NULL_VALUE_AS_VARIABLE, true)).thenReturn(EXAMPLE_VARIABLES_WITH_NULL_VALUE);

    // activity instances
    when(runtimeServiceMock.getActivityInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)).thenReturn(EXAMPLE_ACTIVITY_INSTANCE);

    mockUpdateSuspensionStateSelectBuilder = mock(UpdateProcessInstanceSuspensionStateSelectBuilder.class);
    when(runtimeServiceMock.updateProcessInstanceSuspensionState()).thenReturn(mockUpdateSuspensionStateSelectBuilder);

    mockUpdateSuspensionStateBuilder = mock(UpdateProcessInstanceSuspensionStateTenantBuilder.class);
    when(mockUpdateSuspensionStateSelectBuilder.byProcessInstanceId(any())).thenReturn(mockUpdateSuspensionStateBuilder);
    when(mockUpdateSuspensionStateSelectBuilder.byProcessDefinitionId(any())).thenReturn(mockUpdateSuspensionStateBuilder);
    when(mockUpdateSuspensionStateSelectBuilder.byProcessDefinitionKey(any())).thenReturn(mockUpdateSuspensionStateBuilder);

    mockUpdateProcessInstancesSuspensionStateBuilder = mock(UpdateProcessInstancesSuspensionStateBuilder.class);
    when(mockUpdateSuspensionStateSelectBuilder.byProcessInstanceIds(Mockito.<List<String>>any())).thenReturn(mockUpdateProcessInstancesSuspensionStateBuilder);
    when(mockUpdateSuspensionStateSelectBuilder.byProcessInstanceQuery(any())).thenReturn(mockUpdateProcessInstancesSuspensionStateBuilder);
    when(mockUpdateSuspensionStateSelectBuilder.byHistoricProcessInstanceQuery(any())).thenReturn(mockUpdateProcessInstancesSuspensionStateBuilder);

    // tasks and task service
    taskServiceMock = mock(TaskService.class);
    when(processEngine.getTaskService()).thenReturn(taskServiceMock);

    mockTaskComments = MockProvider.createMockTaskComments();
    when(taskServiceMock.getProcessInstanceComments(EXAMPLE_PROCESS_INSTANCE_ID)).thenReturn(mockTaskComments);

    historicProcessInstanceQueryMock = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQueryMock);
    when(historicProcessInstanceQueryMock.processInstanceId(EXAMPLE_PROCESS_INSTANCE_ID)).thenReturn(historicProcessInstanceQueryMock);
    HistoricProcessInstance historicProcessInstanceMock = createMockHistoricProcessInstance();
    when(historicProcessInstanceQueryMock.singleResult()).thenReturn(historicProcessInstanceMock);

    // runtime service
    when(processEngine.getRuntimeService()).thenReturn(runtimeServiceMock);
    when(processEngine.getManagementService()).thenReturn(mockManagementService);
    when(processEngine.getHistoryService()).thenReturn(historyServiceMock);

    // jobs
    mockSetJobRetriesByProcessAsyncBuilder = MockProvider.createMockSetJobRetriesByProcessAsyncBuilder(mockManagementService);
  }

  public void mockHistoryFull() {
    when(mockManagementService.getHistoryLevel()).thenReturn(ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL);
  }

  public void mockHistoryDisabled() {
    when(mockManagementService.getHistoryLevel()).thenReturn(ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE);
  }

  @Test
  void testGetActivityInstanceTree() {
    Response response = given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
        .then().expect().statusCode(Status.OK.getStatusCode())
        .body("id", Matchers.equalTo(EXAMPLE_ACTIVITY_INSTANCE_ID))
        .body("parentActivityInstanceId", Matchers.equalTo(EXAMPLE_PARENT_ACTIVITY_INSTANCE_ID))
        .body("activityId", Matchers.equalTo(EXAMPLE_ACTIVITY_ID))
        .body("processInstanceId", Matchers.equalTo(EXAMPLE_PROCESS_INSTANCE_ID))
        .body("processDefinitionId", Matchers.equalTo(EXAMPLE_PROCESS_DEFINITION_ID))
        .body("executionIds", Matchers.not(Matchers.empty()))
        .body("executionIds[0]", Matchers.equalTo(EXAMPLE_EXECUTION_ID))
        .body("activityName", Matchers.equalTo(EXAMPLE_ACTIVITY_NAME))
        // "name" is deprecated and there for legacy reasons
        .body("name", Matchers.equalTo(EXAMPLE_ACTIVITY_NAME))
        .body("incidentIds", Matchers.empty())
        .body("incidents", Matchers.not(Matchers.empty()))
        .body("incidents[0].id", Matchers.equalTo("anIncidentId"))
        .body("incidents[0].activityId", Matchers.equalTo("anActivityId"))
        .body("childActivityInstances", Matchers.not(Matchers.empty()))
        .body("childActivityInstances[0].id", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_INSTANCE_ID))
        .body("childActivityInstances[0].parentActivityInstanceId", Matchers.equalTo(CHILD_EXAMPLE_PARENT_ACTIVITY_INSTANCE_ID))
        .body("childActivityInstances[0].activityId", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_ID))
        .body("childActivityInstances[0].activityType", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_TYPE))
        .body("childActivityInstances[0].activityName", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_NAME))
        .body("childActivityInstances[0].name", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_NAME))
        .body("childActivityInstances[0].processInstanceId", Matchers.equalTo(CHILD_EXAMPLE_PROCESS_INSTANCE_ID))
        .body("childActivityInstances[0].processDefinitionId", Matchers.equalTo(CHILD_EXAMPLE_PROCESS_DEFINITION_ID))
        .body("childActivityInstances[0].executionIds", Matchers.not(Matchers.empty()))
        .body("childActivityInstances[0].childActivityInstances", Matchers.empty())
        .body("childActivityInstances[0].childTransitionInstances", Matchers.empty())
        .body("childActivityInstances[0].incidentIds", Matchers.not(Matchers.empty()))
        .body("childActivityInstances[0].incidentIds[0]", Matchers.equalTo(EXAMPLE_INCIDENT_ID))
        .body("childActivityInstances[0].incidents[0].id", Matchers.equalTo("anIncidentId"))
        .body("childActivityInstances[0].incidents[0].activityId", Matchers.equalTo("anActivityId"))
        .body("childTransitionInstances", Matchers.not(Matchers.empty()))
        .body("childTransitionInstances[0].id", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_INSTANCE_ID))
        .body("childTransitionInstances[0].parentActivityInstanceId", Matchers.equalTo(CHILD_EXAMPLE_PARENT_ACTIVITY_INSTANCE_ID))
        .body("childTransitionInstances[0].activityId", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_ID))
        .body("childTransitionInstances[0].activityName", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_NAME))
        .body("childTransitionInstances[0].activityType", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_TYPE))
        .body("childTransitionInstances[0].targetActivityId", Matchers.equalTo(CHILD_EXAMPLE_ACTIVITY_ID))
        .body("childTransitionInstances[0].processInstanceId", Matchers.equalTo(CHILD_EXAMPLE_PROCESS_INSTANCE_ID))
        .body("childTransitionInstances[0].processDefinitionId", Matchers.equalTo(CHILD_EXAMPLE_PROCESS_DEFINITION_ID))
        .body("childTransitionInstances[0].executionId", Matchers.equalTo(EXAMPLE_EXECUTION_ID))
        .body("childTransitionInstances[0].incidentIds", Matchers.not(Matchers.empty()))
        .body("childTransitionInstances[0].incidentIds[0]", Matchers.equalTo(EXAMPLE_ANOTHER_INCIDENT_ID))
        .body("childTransitionInstances[0].incidents[0].id", Matchers.equalTo("anIncidentId"))
        .body("childTransitionInstances[0].incidents[0].activityId", Matchers.equalTo("anActivityId"))
        .when().get(PROCESS_INSTANCE_ACTIVIY_INSTANCES_URL);

    assertThat(response.jsonPath().getMap("")).as("Should return right number of properties").hasSize(13);
  }

  @Test
  void testGetActivityInstanceTreeForNonExistingProcessInstance() {
    when(runtimeServiceMock.getActivityInstance(anyString())).thenReturn(null);

    given().pathParam("id", "aNonExistingProcessInstanceId")
      .then().expect().statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Process instance with id aNonExistingProcessInstanceId does not exist"))
      .when().get(PROCESS_INSTANCE_ACTIVIY_INSTANCES_URL);
  }

  @Test
  void testGetActivityInstanceTreeWithInternalError() {
    when(runtimeServiceMock.getActivityInstance(anyString())).thenThrow(new ProcessEngineException("expected exception"));

    given().pathParam("id", "aNonExistingProcessInstanceId")
      .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("expected exception"))
      .when().get(PROCESS_INSTANCE_ACTIVIY_INSTANCES_URL);
  }

  @Test
  void testGetActivityInstanceTreeThrowsAuthorizationException() {
    String message = "expected exception";
    when(runtimeServiceMock.getActivityInstance(anyString())).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .get(PROCESS_INSTANCE_ACTIVIY_INSTANCES_URL);
  }

  @Test
  void testGetVariables() {
    Response response = given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body(EXAMPLE_VARIABLE_KEY, Matchers.notNullValue())
      .body(EXAMPLE_VARIABLE_KEY + ".value", Matchers.equalTo(EXAMPLE_VARIABLE_VALUE.getValue()))
      .body(EXAMPLE_VARIABLE_KEY + ".type", Matchers.equalTo(String.class.getSimpleName()))
      .when().get(PROCESS_INSTANCE_VARIABLES_URL);

    assertThat(response.jsonPath().getMap("")).as("Should return exactly one variable").hasSize(1);
  }

  @Test
  void testDeleteAsync() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    when(runtimeServiceMock.deleteProcessInstancesAsync(any(), any(), any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(new BatchEntity());

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceIds", ids);
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_URL);

    verify(runtimeServiceMock, times(1)).deleteProcessInstancesAsync(ids, null, null, TEST_DELETE_REASON, false, false, false);
  }

  @Test
  void testDeleteAsyncWithSkipIoMappingsTrue() {
    var ids = List.of(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    when(runtimeServiceMock.deleteProcessInstancesAsync(any(), any(), any(), anyString(), anyBoolean(), anyBoolean(), eq(true))).thenReturn(new BatchEntity());

    var messageBodyJson = Map.of(
        "processInstanceIds", ids,
        DELETE_REASON, TEST_DELETE_REASON,
        SKIP_IO_MAPPINGS, true
    );

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_URL);

    verify(runtimeServiceMock, times(1)).deleteProcessInstancesAsync(ids, null, null, TEST_DELETE_REASON, false, false, true);
  }

  @Test
  void testDeleteAsyncWithSkipIoMappingsFalse() {
    var ids = List.of(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    when(runtimeServiceMock.deleteProcessInstancesAsync(any(), any(), any(), anyString(), anyBoolean(), anyBoolean(), eq(false))).thenReturn(new BatchEntity());

    var messageBodyJson = Map.of(
        "processInstanceIds", ids,
        DELETE_REASON, TEST_DELETE_REASON,
        SKIP_IO_MAPPINGS, false
    );

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_URL);

    verify(runtimeServiceMock, times(1)).deleteProcessInstancesAsync(ids, null, null, TEST_DELETE_REASON, false, false, false);
  }

  @Test
  void testDeleteAsyncWithQuery() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    messageBodyJson.put("processInstanceQuery", query);

    when(runtimeServiceMock.deleteProcessInstancesAsync(any(), any(), any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(new BatchEntity());

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_URL);

    verify(runtimeServiceMock, times(1)).deleteProcessInstancesAsync(
        any(),
        any(),
        any(),
        eq(TEST_DELETE_REASON),
        eq(false),
        eq(false),
        eq(false)
    );
  }

  @Test
  void testDeleteAsyncWithBadRequestQuery() {
    doThrow(new BadUserRequestException("process instance ids are empty"))
      .when(runtimeServiceMock).deleteProcessInstancesAsync(eq(null), eq(null), any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean());

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_URL);
  }

  @Test
  void testDeleteAsyncWithSkipCustomListeners() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(any(), any(), any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(new BatchEntity());

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);
    messageBodyJson.put("processInstanceIds", Arrays.asList("processInstanceId1", "processInstanceId2"));
    messageBodyJson.put("skipCustomListeners", true);

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_URL);

    verify(runtimeServiceMock).deleteProcessInstancesAsync(
        anyList(),
        any(),
        any(),
        eq(TEST_DELETE_REASON),
        eq(true),
        eq(false),
        eq(false)
    );
  }

  @Test
  void testDeleteAsyncWithSkipSubprocesses() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(any(), any(), any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(new BatchEntity());

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(DELETE_REASON, TEST_DELETE_REASON);
    messageBodyJson.put("processInstanceIds", Arrays.asList("processInstanceId1", "processInstanceId2"));
    messageBodyJson.put("skipSubprocesses", true);

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_URL);

    verify(runtimeServiceMock).deleteProcessInstancesAsync(
        anyList(),
        any(),
        any(),
        eq(TEST_DELETE_REASON),
        eq(false),
        eq(true),
        eq(false)
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithQuery() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
      any(),
      eq((ProcessInstanceQuery)null),
      any(),
      any(),
      anyBoolean(),
      anyBoolean(),
      anyBoolean()
    ))
    .thenReturn(new BatchEntity());

    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery = mock(HistoricProcessInstanceQueryImpl.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedHistoricProcessInstanceQuery);

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setHistoricProcessInstanceQuery(new HistoricProcessInstanceQueryDto());

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock,
      times(1)).deleteProcessInstancesAsync(
        null,
        null,
        mockedHistoricProcessInstanceQuery,
        null,
        false,
        false,
        false
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithProcessInstanceIds() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
      any(),
      eq((ProcessInstanceQuery)null),
      eq((HistoricProcessInstanceQuery)null),
      any(),
      anyBoolean(),
      anyBoolean(),
      anyBoolean()
    ))
    .thenReturn(new BatchEntity());

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setProcessInstanceIds(Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock,
      times(1)).deleteProcessInstancesAsync(
      Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID),
      null,
      null,
      null,
      false,
      false,
      false
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithQueryAndProcessInstanceIds() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
      any(),
      eq((ProcessInstanceQuery)null),
      any(),
      any(),
      anyBoolean(),
      anyBoolean(),
      anyBoolean()
    ))
    .thenReturn(new BatchEntity());

    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery = mock(HistoricProcessInstanceQueryImpl.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedHistoricProcessInstanceQuery);

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setHistoricProcessInstanceQuery(new HistoricProcessInstanceQueryDto());
    body.setProcessInstanceIds(Collections.singletonList(MockProvider.ANOTHER_EXAMPLE_PROCESS_INSTANCE_ID));

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock,
      times(1)).deleteProcessInstancesAsync(
      Collections.singletonList(MockProvider.ANOTHER_EXAMPLE_PROCESS_INSTANCE_ID),
      null,
      mockedHistoricProcessInstanceQuery,
      null,
      false,
      false,
      false
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithoutQueryAndWithoutProcessInstanceIds() {
    doThrow(new BadUserRequestException("processInstanceIds is empty"))
      .when(runtimeServiceMock).deleteProcessInstancesAsync(
        eq((List<String>)null),
        eq((ProcessInstanceQuery)null),
        eq((HistoricProcessInstanceQuery)null),
        any(),
        anyBoolean(),
        anyBoolean(),
        anyBoolean());

    given()
      .contentType(ContentType.JSON).body(new DeleteProcessInstancesDto())
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock,
      times(1)).deleteProcessInstancesAsync(
        null,
        null,
        null,
        null,
        false,
        false,
        false
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithDeleteReason() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
      any(),
      any(),
      any(),
      any(),
      anyBoolean(),
      anyBoolean(),
      anyBoolean()
    ))
    .thenReturn(new BatchEntity());

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setDeleteReason(MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_DELETE_REASON);

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock,
      times(1)).deleteProcessInstancesAsync(
        null,
        null,
        null,
        MockProvider.EXAMPLE_HISTORIC_PROCESS_INSTANCE_DELETE_REASON,
        false,
        false,
        false
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithSkipCustomListenerTrue() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
      eq((List<String>)null),
      eq((ProcessInstanceQuery)null),
      any(),
      any(),
      anyBoolean(),
      anyBoolean(),
      anyBoolean()
    ))
    .thenReturn(new BatchEntity());

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setSkipCustomListeners(true);

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock,
      times(1)).deleteProcessInstancesAsync(
      null,
      null,
      null,
      null,
      true,
      false,
      false
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithSkipSubprocesses() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
      eq((List<String>)null),
      eq((ProcessInstanceQuery)null),
      eq((HistoricProcessInstanceQuery)null),
      any(),
      anyBoolean(),
      anyBoolean(),
      anyBoolean()
    ))
    .thenReturn(new BatchEntity());

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setSkipSubprocesses(true);

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock,
      times(1)).deleteProcessInstancesAsync(
      null,
      null,
      null,
      null,
      false,
      true,
      false
    );
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithSkipIoMappingsTrue() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
        eq(null),
        eq(null),
        eq(null),
        any(),
        anyBoolean(),
        anyBoolean(),
        eq(true)
    )).thenReturn(new BatchEntity());

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setSkipIoMappings(true);

    given()
        .contentType(ContentType.JSON).body(body)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock, times(1)).deleteProcessInstancesAsync(null, null, null, null, false, false, true);
  }

  @Test
  void testDeleteAsyncHistoricQueryBasedWithSkipIoMappingsFalse() {
    when(runtimeServiceMock.deleteProcessInstancesAsync(
        eq(null),
        eq(null),
        eq(null),
        any(),
        anyBoolean(),
        anyBoolean(),
        eq(false)
    ))
        .thenReturn(new BatchEntity());

    DeleteProcessInstancesDto body = new DeleteProcessInstancesDto();
    body.setSkipIoMappings(false);

    given()
        .contentType(ContentType.JSON).body(body)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(DELETE_PROCESS_INSTANCES_ASYNC_HIST_QUERY_URL);

    verify(runtimeServiceMock, times(1)).deleteProcessInstancesAsync(null, null, null, null, false, false, false);
  }

  @Test
  void testGetVariablesWithNullValue() {
    Response response = given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID_WITH_NULL_VALUE_AS_VARIABLE)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body(EXAMPLE_ANOTHER_VARIABLE_KEY, Matchers.notNullValue())
      .body(EXAMPLE_ANOTHER_VARIABLE_KEY + ".value", Matchers.nullValue())
      .body(EXAMPLE_ANOTHER_VARIABLE_KEY + ".type", Matchers.equalTo("Null"))
      .when().get(PROCESS_INSTANCE_VARIABLES_URL);

    assertThat(response.jsonPath().getMap("")).as("Should return exactly one variable").hasSize(1);
  }

  @Test
  void testGetProcessInstanceComments() {
    mockHistoryFull();

    Response response = given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .header("accept", MediaType.APPLICATION_JSON)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
      .body("$.size()", equalTo(1))
    .when()
      .get(PROCESS_INSTANCE_COMMENTS_URL);

    verifyTaskComments(mockTaskComments, response);
    verify(taskServiceMock).getProcessInstanceComments(EXAMPLE_PROCESS_INSTANCE_ID);
  }

  @Test
  void testGetProcessInstanceCommentsWithHistoryDisabled() {
    mockHistoryDisabled();

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .header("accept", MediaType.APPLICATION_JSON)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
      .body("$.size()", equalTo(0))
    .when()
      .get(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testDeleteInstanceCommentThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(taskServiceMock)
        .deleteProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID, EXAMPLE_PROCESS_INSTANCE_COMMENT_ID);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .pathParam("commentId", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .when()
        .delete(SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL);
  }

  @Test
  void testDeleteInstanceComment() {
    mockHistoryFull();

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .pathParam("commentId", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
        .delete(SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL);

    verify(taskServiceMock).deleteProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID, EXAMPLE_PROCESS_INSTANCE_COMMENT_ID);
  }

  @Test
  void testDeleteInstanceCommentWithHistoryDisabled() {
    mockHistoryDisabled();

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .pathParam("commentId", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body(containsString("History is not enabled"))
        .when()
        .delete(SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL);
  }

  @Test
  void testDeleteInstanceCommentForNonExistingCommentId() {
    mockHistoryFull();
    doThrow(new NullValueException()).when(taskServiceMock)
        .deleteProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID, NON_EXISTING_ID);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .pathParam("commentId", NON_EXISTING_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .contentType(ContentType.JSON)
        .when()
        .delete(SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL);
  }

  @Test
  void testDeleteInstanceCommentForNonExistingCommentIdWithHistoryDisabled() {
    mockHistoryDisabled();

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .pathParam("commentId", NON_EXISTING_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body(containsString("History is not enabled"))
        .when()
        .delete(SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL);
  }

  @Test
  void testDeleteInstanceCommentForNonExistingProcessInstance() {
    mockHistoryFull();
    historicProcessInstanceQueryMock = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQueryMock);
    when(historicProcessInstanceQueryMock.processInstanceId(NON_EXISTING_ID)).thenReturn(
        historicProcessInstanceQueryMock);
    when(historicProcessInstanceQueryMock.singleResult()).thenReturn(null);

    given().pathParam("id", NON_EXISTING_ID)
        .pathParam("commentId", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .body(containsString("No process instance found for id " + NON_EXISTING_ID))
        .when()
        .delete(SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL);
  }

  @Test
  void testDeleteInstanceCommentForNonExistingWithHistoryDisabled() {
    mockHistoryDisabled();

    given().pathParam("id", NON_EXISTING_ID)
        .pathParam("commentId", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body(containsString("History is not enabled"))
        .when()
        .delete(SINGLE_PROCESS_INSTANCE_SINGLE_COMMENT_URL);
  }

  @Test
  void testDeleteProcessInstanceCommentsThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(taskServiceMock)
        .deleteProcessInstanceComments(MockProvider.EXAMPLE_TASK_ID);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .when()
        .delete(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testDeleteProcessInstanceComments() {
    mockHistoryFull();
    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
        .delete(PROCESS_INSTANCE_COMMENTS_URL);

    verify(taskServiceMock).deleteProcessInstanceComments(EXAMPLE_PROCESS_INSTANCE_ID);
  }

  @Test
  void testDeleteProcessInstanceCommentsWithHistoryDisabled() {
    mockHistoryDisabled();

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body(containsString("History is not enabled"))
        .when()
        .delete(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testDeleteProcessInstanceCommentsForNonExisting() {
    mockHistoryFull();
    historicProcessInstanceQueryMock = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQueryMock);
    when(historicProcessInstanceQueryMock.processInstanceId(NON_EXISTING_ID)).thenReturn(
        historicProcessInstanceQueryMock);
    when(historicProcessInstanceQueryMock.singleResult()).thenReturn(null);

    given().pathParam("id", NON_EXISTING_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .body(containsString("No process instance found for id " + NON_EXISTING_ID))
        .when()
        .delete(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testDeleteProcessInstanceCommentsForNonExistingWithHistoryDisabled() {
    mockHistoryDisabled();

    given().pathParam("id", NON_EXISTING_ID)
        .header("accept", MediaType.APPLICATION_JSON)
        .then()
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body(containsString("History is not enabled"))
        .when()
        .delete(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testUpdateProcessInstanceCommentCommentIdNull() {
    mockHistoryFull();

    String message = "expected exception";
    doThrow(new NullValueException(message)).when(taskServiceMock)
        .updateProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID, null, EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);

    Map<String, Object> json = new HashMap<>();

    json.put("id", null);
    json.put("message", EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .body(json)
        .contentType(ContentType.JSON)
        .header("accept", MediaType.APPLICATION_JSON)
        .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when()
        .put(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testUpdateProcessInstanceCommentMessageIsNull() {
    mockHistoryFull();

    String message = "expected exception";
    doThrow(new NullValueException(message)).when(taskServiceMock)
        .updateProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID, EXAMPLE_PROCESS_INSTANCE_COMMENT_ID, null);

    Map<String, Object> json = new HashMap<>();

    json.put("id", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID);
    json.put("message", null);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .body(json)
        .contentType(ContentType.JSON)
        .header("accept", MediaType.APPLICATION_JSON)
        .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when()
        .put(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testUpdateProcessInstanceComment() {
    mockHistoryFull();
    Map<String, Object> json = new HashMap<>();

    json.put("id", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID);
    json.put("message", EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .body(json)
        .contentType(ContentType.JSON)
        .header("accept", MediaType.APPLICATION_JSON)
        .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
        .put(PROCESS_INSTANCE_COMMENTS_URL);

    verify(taskServiceMock).updateProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID,
        EXAMPLE_PROCESS_INSTANCE_COMMENT_ID, EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);
  }

  @Test
  void testUpdateProcessInstanceCommentExtraProperties() {
    mockHistoryFull();

    Map<String, Object> json = new HashMap<>();
    //Only id and message are used
    json.put("id", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID);
    json.put("userId", "anyUserId");
    json.put("time", withTimezone("2014-01-01T00:00:00"));
    json.put("message", EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);
    json.put("removalTime", withTimezone("2014-05-01T00:00:00"));
    json.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .body(json)
        .contentType(ContentType.JSON)
        .header("accept", MediaType.APPLICATION_JSON)
        .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
        .put(PROCESS_INSTANCE_COMMENTS_URL);

    verify(taskServiceMock).updateProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID,
        EXAMPLE_PROCESS_INSTANCE_COMMENT_ID, EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);
  }

  @Test
  void testUpdateProcessInstanceCommentProcessInstanceIdNotFound() {
    mockHistoryFull();
    historicProcessInstanceQueryMock = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQueryMock);
    when(historicProcessInstanceQueryMock.processInstanceId(NON_EXISTING_ID)).thenReturn(
        historicProcessInstanceQueryMock);
    when(historicProcessInstanceQueryMock.singleResult()).thenReturn(null);

    Map<String, Object> json = new HashMap<>();

    json.put("id", EXAMPLE_PROCESS_INSTANCE_ID);
    json.put("message", EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);

    given().pathParam("id", NON_EXISTING_ID)
        .body(json)
        .contentType(ContentType.JSON)
        .header("accept", MediaType.APPLICATION_JSON)
        .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .contentType(ContentType.JSON)
        .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
        .body(containsString("No process instance found for id " + NON_EXISTING_ID))
        .when()
        .put(PROCESS_INSTANCE_COMMENTS_URL);
  }

  @Test
  void testUpdateProcessInstanceCommentThrowsAuthorizationException() {
    mockHistoryFull();
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(taskServiceMock)
        .updateProcessInstanceComment(EXAMPLE_PROCESS_INSTANCE_ID, EXAMPLE_PROCESS_INSTANCE_COMMENT_ID,
            EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);

    Map<String, Object> json = new HashMap<>();
    json.put("id", EXAMPLE_PROCESS_INSTANCE_COMMENT_ID);
    json.put("message", EXAMPLE_PROCESS_INSTANCE_COMMENT_FULL_MESSAGE);

    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .body(json)
        .contentType(ContentType.JSON)
        .header("accept", MediaType.APPLICATION_JSON)
        .expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .contentType(ContentType.JSON)
        .when()
        .put(PROCESS_INSTANCE_COMMENTS_URL);

  }

  @Test
  void testGetFileVariable() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    String mimeType = "text/plain";
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(mimeType).create();

    when(runtimeServiceMock.getVariableTyped(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, variableKey, true))
    .thenReturn(variableValue);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON.toString())
    .and()
      .body("valueInfo.mimeType", Matchers.equalTo(mimeType))
      .body("valueInfo.filename", Matchers.equalTo(filename))
      .body("value", Matchers.nullValue())
    .when().get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testGetNullFileVariable() {
    String variableKey = "aVariableKey";
    String filename = "test.txt";
    String mimeType = "text/plain";
    FileValue variableValue = Variables.fileValue(filename).mimeType(mimeType).create();

    when(runtimeServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean()))
      .thenReturn(variableValue);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.TEXT.toString())
    .and()
      .body(Matchers.is(Matchers.equalTo("")))
    .when().get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);
  }

  @Test
  void testGetFileVariableDownloadWithType() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(ContentType.TEXT.toString()).create();

    when(runtimeServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

  given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
    .pathParam("varId", variableKey)
  .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.TEXT.toString())
    .and()
      .body(Matchers.is(Matchers.equalTo(new String(byteContent))))
    .when().get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);
  }

  @Test
  void testGetFileVariableDownloadWithTypeAndEncoding() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    String encoding = UTF_8.name();
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).mimeType(ContentType.TEXT.toString()).encoding(encoding).create();

    when(runtimeServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

    Response response = given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body(Matchers.is(Matchers.equalTo(new String(byteContent))))
    .when().get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    String contentType = response.contentType().replaceAll(" ", "");
    assertThat(contentType).isEqualTo(ContentType.TEXT + ";charset=" + encoding);
  }

  @Test
  void testGetFileVariableDownloadWithoutType() {
    String variableKey = "aVariableKey";
    final byte[] byteContent = "some bytes".getBytes();
    String filename = "test.txt";
    FileValue variableValue = Variables.fileValue(filename).file(byteContent).create();

    when(runtimeServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

   given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
    .pathParam("varId", variableKey)
  .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .and()
      .body(Matchers.is(Matchers.equalTo(new String(byteContent))))
      .header("Content-Disposition", Matchers.containsString(filename))
    .when().get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);
  }

  @Test
  void testCannotDownloadVariableOtherThanFile() {
    String variableKey = "aVariableKey";
    LongValue variableValue = Variables.longValue(123L);

    when(runtimeServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean()))
    .thenReturn(variableValue);

   given()
    .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
    .pathParam("varId", variableKey)
  .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .contentType(MediaType.APPLICATION_JSON)
    .and()
    .when().get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);
  }

  @Test
  void testJavaObjectVariableSerialization() {
    Response response = given().pathParam("id", MockProvider.ANOTHER_EXAMPLE_PROCESS_INSTANCE_ID)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body(EXAMPLE_VARIABLE_KEY, Matchers.notNullValue())
      .body(EXAMPLE_VARIABLE_KEY + ".value.property1", Matchers.equalTo("aPropertyValue"))
      .body(EXAMPLE_VARIABLE_KEY + ".value.property2", Matchers.equalTo(true))
      .body(EXAMPLE_VARIABLE_KEY + ".type", Matchers.equalTo(VariableTypeHelper.toExpectedValueTypeName(ValueType.OBJECT)))
      .body(EXAMPLE_VARIABLE_KEY + ".valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, Matchers.equalTo(ExampleVariableObject.class.getName()))
      .body(EXAMPLE_VARIABLE_KEY + ".valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, Matchers.equalTo("application/json"))
      .when().get(PROCESS_INSTANCE_VARIABLES_URL);

    assertThat(response.jsonPath().getMap("")).as("Should return exactly one variable").hasSize(1);
  }

  @Test
  void testGetObjectVariables() {
    // given
    String variableKey = "aVariableId";

    List<String> payload = Arrays.asList("a", "b");
    ObjectValue variableValue =
        MockObjectValue
            .fromObjectValue(Variables
                .objectValue(payload)
                .serializationDataFormat("application/json")
                .create())
            .objectTypeName(ArrayList.class.getName())
            .serializedValue("a serialized value"); // this should differ from the serialized json

    when(runtimeServiceMock.getVariablesTyped(eq(EXAMPLE_PROCESS_INSTANCE_ID), anyBoolean()))
      .thenReturn(Variables.createVariables().putValueTyped(variableKey, variableValue));

    // when
    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body(variableKey + ".value", Matchers.equalTo(payload))
      .body(variableKey + ".type", Matchers.equalTo("Object"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, Matchers.equalTo("application/json"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, Matchers.equalTo(ArrayList.class.getName()))
      .when().get(PROCESS_INSTANCE_VARIABLES_URL);

    // then
    verify(runtimeServiceMock).getVariablesTyped(EXAMPLE_PROCESS_INSTANCE_ID, true);
  }

  @Test
  void testGetObjectVariablesSerialized() {
    // given
    String variableKey = "aVariableId";

    ObjectValue variableValue =
        Variables
          .serializedObjectValue("a serialized value")
          .serializationDataFormat("application/json")
          .objectTypeName(ArrayList.class.getName())
          .create();

    when(runtimeServiceMock.getVariablesTyped(eq(EXAMPLE_PROCESS_INSTANCE_ID), anyBoolean()))
      .thenReturn(Variables.createVariables().putValueTyped(variableKey, variableValue));

    // when
    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .queryParam("deserializeValues", false)
    .then().expect().statusCode(Status.OK.getStatusCode())
      .body(variableKey + ".value", Matchers.equalTo("a serialized value"))
      .body(variableKey + ".type", Matchers.equalTo("Object"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, Matchers.equalTo("application/json"))
      .body(variableKey + ".valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, Matchers.equalTo(ArrayList.class.getName()))
      .when().get(PROCESS_INSTANCE_VARIABLES_URL);

    // then
    verify(runtimeServiceMock).getVariablesTyped(EXAMPLE_PROCESS_INSTANCE_ID, false);
  }

  @Test
  void testGetVariablesForNonExistingProcessInstance() {
    when(runtimeServiceMock.getVariablesTyped(anyString(), anyBoolean())).thenThrow(new ProcessEngineException("expected exception"));

    given().pathParam("id", "aNonExistingProcessInstanceId")
      .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(ProcessEngineException.class.getSimpleName()))
      .body("message", Matchers.equalTo("expected exception"))
      .when().get(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testGetVariablesThrowsAuthorizationException() {
    String message = "expected exception";
    when(runtimeServiceMock.getVariablesTyped(anyString(), anyBoolean())).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .get(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testGetSingleInstance() {
    ProcessInstance mockInstance = MockProvider.createMockInstance();
    ProcessInstanceQuery sampleInstanceQuery = mock(ProcessInstanceQuery.class);
    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.processInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.singleResult()).thenReturn(mockInstance);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("id", Matchers.equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID))
      .body("ended", Matchers.equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_ENDED))
      .body("definitionId", Matchers.equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID))
      .body("definitionKey", Matchers.equalTo(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY))
      .body("businessKey", Matchers.equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_BUSINESS_KEY))
      .body("suspended", Matchers.equalTo(MockProvider.EXAMPLE_PROCESS_INSTANCE_IS_SUSPENDED))
      .body("tenantId", Matchers.equalTo(MockProvider.EXAMPLE_TENANT_ID))
      .when().get(SINGLE_PROCESS_INSTANCE_URL);
  }

  @Test
  void testGetNonExistingProcessInstance() {
    ProcessInstanceQuery sampleInstanceQuery = mock(ProcessInstanceQuery.class);
    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.processInstanceId(anyString())).thenReturn(sampleInstanceQuery);
    when(sampleInstanceQuery.singleResult()).thenReturn(null);

    given().pathParam("id", "aNonExistingInstanceId")
      .then().expect().statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Process instance with id aNonExistingInstanceId does not exist"))
      .when().get(SINGLE_PROCESS_INSTANCE_URL);
  }

  @Test
  void testDeleteProcessInstance() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, false, true, false, false);
  }

  @Test
  void testDeleteNonExistingProcessInstance() {
    doThrow(new NotFoundException()).when(runtimeServiceMock).deleteProcessInstance(any(), any(), anyBoolean(), anyBoolean(), anyBoolean() ,anyBoolean());

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .then().expect().statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .when().delete(SINGLE_PROCESS_INSTANCE_URL);
  }

  @Test
  void testDeleteNonExistingProcessInstanceIfExists() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParam("failIfNotExists", false)
    .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
    .when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstanceIfExists(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, false, true, false, false);
  }

  @Test
  void testDeleteProcessInstanceThrowsAuthorizationException() {
    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(runtimeServiceMock).deleteProcessInstance(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .delete(SINGLE_PROCESS_INSTANCE_URL);
  }

  @Test
  void testDeleteProcessInstanceSkipCustomListeners() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParams("skipCustomListeners", true).then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode()).when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, true, true, false, false);
  }

  @Test
  void testDeleteProcessInstanceWithCustomListeners() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParams("skipCustomListeners", false).then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode()).when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, false, true, false, false);
  }

  @Test
  void testDeleteProcessInstanceSkipIoMappings() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParams("skipIoMappings", true).then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode()).when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, false, true, true, false);
  }

  @Test
  void testDeleteProcessInstanceWithoutSkipingIoMappings() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParams("skipIoMappings", false).then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode()).when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, false, true, false, false);
  }

  @Test
  void testDeleteProcessInstanceSkipSubprocesses() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParams("skipSubprocesses", true).then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode()).when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, false, true, false, true);
  }

  @Test
  void testDeleteProcessInstanceWithoutSkipSubprocesses() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).queryParams("skipSubprocesses", false).then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode()).when().delete(SINGLE_PROCESS_INSTANCE_URL);

    verify(runtimeServiceMock).deleteProcessInstance(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, null, false, true, false, false);
  }

  @Test
  void testVariableModification() {
    String variableKey = "aKey";
    int variableValue = 123;

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue).getVariables();
    messageBodyJson.put("modifications", modifications);

    List<String> deletions = new ArrayList<>();
    deletions.add("deleteKey");
    messageBodyJson.put("deletions", deletions);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);

    Map<String, Object> expectedModifications = new HashMap<>();
    expectedModifications.put(variableKey, variableValue);
    verify(runtimeServiceMock).updateVariables(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), argThat(new EqualsMap(expectedModifications)),
        argThat(new EqualsList(deletions)));
  }

  @Test
  void testVariableModificationWithUnparseableInteger() {
    String variableKey = "aKey";
    String variableValue = "1abc";
    String variableType = "Integer";

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();
    messageBodyJson.put("modifications", modifications);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot modify variables for process instance: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Integer.class)))
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testVariableModificationWithUnparseableShort() {
    String variableKey = "aKey";
    String variableValue = "1abc";
    String variableType = "Short";

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();
    messageBodyJson.put("modifications", modifications);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot modify variables for process instance: "
      + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Short.class)))
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testVariableModificationWithUnparseableLong() {
    String variableKey = "aKey";
    String variableValue = "1abc";
    String variableType = "Long";

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();
    messageBodyJson.put("modifications", modifications);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot modify variables for process instance: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Long.class)))
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testVariableModificationWithUnparseableDouble() {
    String variableKey = "aKey";
    String variableValue = "1abc";
    String variableType = "Double";

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();
    messageBodyJson.put("modifications", modifications);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot modify variables for process instance: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Double.class)))
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testVariableModificationWithUnparseableDate() {
    String variableKey = "aKey";
    String variableValue = "1abc";
    String variableType = "Date";

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();
    messageBodyJson.put("modifications", modifications);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot modify variables for process instance: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, variableType, Date.class)))
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testVariableModificationWithNotSupportedType() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String variableType = "X";

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue, variableType).getVariables();
    messageBodyJson.put("modifications", modifications);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot modify variables for process instance: Unsupported value type 'X'"))
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testVariableModificationForNonExistingProcessInstance() {
    doThrow(new ProcessEngineException("expected exception")).when(runtimeServiceMock).updateVariables(any(), any(), any());

    String variableKey = "aKey";
    int variableValue = 123;

    Map<String, Object> messageBodyJson = new HashMap<>();

    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue).getVariables();

    messageBodyJson.put("modifications", modifications);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(messageBodyJson)
      .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(RestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot modify variables for process instance " + MockProvider.EXAMPLE_PROCESS_INSTANCE_ID + ": expected exception"))
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testEmptyVariableModification() {
    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).contentType(ContentType.JSON).body(EMPTY_JSON_OBJECT)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testVariableModificationThrowsAuthorizationException() {
    String variableKey = "aKey";
    int variableValue = 123;
    Map<String, Object> messageBodyJson = new HashMap<>();
    Map<String, Object> modifications = VariablesBuilder.create().variable(variableKey, variableValue).getVariables();
    messageBodyJson.put("modifications", modifications);

    String message = "excpected exception";
    doThrow(new AuthorizationException(message)).when(runtimeServiceMock).updateVariables(any(), any(), any());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", Matchers.is(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.is(message))
    .when()
      .post(PROCESS_INSTANCE_VARIABLES_URL);
  }

  @Test
  void testGetSingleVariable() {
    String variableKey = "aVariableKey";
    int variableValue = 123;

    when(runtimeServiceMock.getVariableTyped(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, variableKey, true))
      .thenReturn(Variables.integerValue(variableValue));

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("value", Matchers.is(123))
      .body("type", Matchers.is("Integer"))
      .when().get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testNonExistingVariable() {
    String variableKey = "aVariableKey";

    when(runtimeServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean())).thenReturn(null);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .then().expect().statusCode(Status.NOT_FOUND.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.is("process instance variable with name " + variableKey + " does not exist"))
      .when().get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testGetSingleVariableThrowsAuthorizationException() {
    String variableKey = "aVariableKey";

    String message = "excpected exception";
    when(runtimeServiceMock.getVariableTyped(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean())).thenThrow(new AuthorizationException(message));

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", Matchers.is(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.is(message))
    .when()
      .get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testGetSingleLocalVariableData() {

    when(runtimeServiceMock.getVariableTyped(anyString(), eq(EXAMPLE_BYTES_VARIABLE_KEY), eq(false))).thenReturn(EXAMPLE_VARIABLE_VALUE_BYTES);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", EXAMPLE_BYTES_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .when()
      .get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock).getVariableTyped(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, EXAMPLE_BYTES_VARIABLE_KEY, false);
  }

  @Test
  void testGetSingleLocalVariableDataNonExisting() {

    when(runtimeServiceMock.getVariableTyped(anyString(), eq("nonExisting"), eq(false))).thenReturn(null);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", "nonExisting")
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
        .body("message", Matchers.is("process instance variable with name " + "nonExisting" + " does not exist"))
    .when()
      .get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock).getVariableTyped(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, "nonExisting", false);
  }

  @Test
  void testGetSingleLocalVariabledataNotBinary() {

    when(runtimeServiceMock.getVariableTyped(anyString(), eq(EXAMPLE_VARIABLE_KEY), eq(false))).thenReturn(EXAMPLE_VARIABLE_VALUE);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", EXAMPLE_VARIABLE_KEY)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .get(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock).getVariableTyped(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, EXAMPLE_VARIABLE_KEY, false);
  }

  @Test
  void testGetSingleLocalObjectVariable() {
    // given
    String variableKey = "aVariableId";

    List<String> payload = Arrays.asList("a", "b");
    ObjectValue variableValue =
        MockObjectValue
            .fromObjectValue(Variables
                .objectValue(payload)
                .serializationDataFormat("application/json")
                .create())
            .objectTypeName(ArrayList.class.getName())
            .serializedValue("a serialized value"); // this should differ from the serialized json

    when(runtimeServiceMock.getVariableTyped(eq(EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean())).thenReturn(variableValue);

    // when
    given().pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("value", Matchers.equalTo(payload))
      .body("type", Matchers.equalTo("Object"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, Matchers.equalTo("application/json"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, Matchers.equalTo(ArrayList.class.getName()))
      .when().get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    // then
    verify(runtimeServiceMock).getVariableTyped(EXAMPLE_PROCESS_INSTANCE_ID, variableKey, true);
  }

  @Test
  void testGetSingleLocalObjectVariableSerialized() {
    // given
    String variableKey = "aVariableId";

    ObjectValue variableValue =
        Variables
          .serializedObjectValue("a serialized value")
          .serializationDataFormat("application/json")
          .objectTypeName(ArrayList.class.getName())
          .create();

    when(runtimeServiceMock.getVariableTyped(eq(EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), anyBoolean())).thenReturn(variableValue);

    // when
    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
      .queryParam("deserializeValue", false)
    .then().expect().statusCode(Status.OK.getStatusCode())
      .body("value", Matchers.equalTo("a serialized value"))
      .body("type", Matchers.equalTo("Object"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_SERIALIZATION_DATA_FORMAT, Matchers.equalTo("application/json"))
      .body("valueInfo." + SerializableValueType.VALUE_INFO_OBJECT_TYPE_NAME, Matchers.equalTo(ArrayList.class.getName()))
      .when().get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    // then
    verify(runtimeServiceMock).getVariableTyped(EXAMPLE_PROCESS_INSTANCE_ID, variableKey, false);
  }

  @Test
  void testGetVariableForNonExistingInstance() {
    String variableKey = "aVariableKey";

    when(runtimeServiceMock.getVariableTyped(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, variableKey, true))
      .thenThrow(new ProcessEngineException("expected exception"));

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", Matchers.is(RestException.class.getSimpleName()))
      .body("message", Matchers.is("Cannot get process instance variable " + variableKey + ": expected exception"))
      .when().get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "aVariableValue";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsUntypedValue.matcher().value(variableValue)));
  }

  @Test
  void testPutSingleVariableWithTypeString() {
    String variableKey = "aVariableKey";
    String variableValue = "aVariableValue";
    String type = "String";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.stringValue(variableValue)));
  }

  @Test
  void testPutSingleVariableWithTypeInteger() {
    String variableKey = "aVariableKey";
    Integer variableValue = 123;
    String type = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.integerValue(variableValue)));
  }

  @Test
  void testPutSingleVariableWithUnparseableInteger() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Integer";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot put process instance variable aVariableKey: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Integer.class)))
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithTypeShort() {
    String variableKey = "aVariableKey";
    Short variableValue = 123;
    String type = "Short";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.shortValue(variableValue)));
  }

  @Test
  void testPutSingleVariableWithUnparseableShort() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Short";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot put process instance variable aVariableKey: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Short.class)))
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithTypeLong() {
    String variableKey = "aVariableKey";
    Long variableValue = 123L;
    String type = "Long";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.longValue(variableValue)));
  }

  @Test
  void testPutSingleVariableWithUnparseableLong() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Long";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot put process instance variable aVariableKey: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Long.class)))
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithTypeDouble() {
    String variableKey = "aVariableKey";
    Double variableValue = 123.456;
    String type = "Double";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.doubleValue(variableValue)));
  }

  @Test
  void testPutSingleVariableWithUnparseableDouble() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Double";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot put process instance variable aVariableKey: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Double.class)))
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithTypeBoolean() {
    String variableKey = "aVariableKey";
    Boolean variableValue = true;
    String type = "Boolean";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.booleanValue(variableValue)));
  }

  @Test
  void testPutSingleVariableWithTypeDate() throws Exception {
    Date now = new Date();

    String variableKey = "aVariableKey";
    String variableValue = DATE_FORMAT_WITH_TIMEZONE.format(now);
    String type = "Date";

    Date expectedValue = DATE_FORMAT_WITH_TIMEZONE.parse(variableValue);

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.dateValue(expectedValue)));
  }

  @Test
  void testPutSingleVariableWithUnparseableDate() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "Date";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot put process instance variable aVariableKey: "
          + ErrorMessageHelper.getExpectedFailingConversionMessage(variableValue, type, Date.class)))
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableWithNotSupportedType() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "X";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot put process instance variable aVariableKey: Unsupported value type 'X'"))
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableThrowsAuthorizationException() {
    String variableKey = "aVariableKey";
    String variableValue = "1abc";
    String type = "String";
    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue, type);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(runtimeServiceMock).setVariable(anyString(), anyString(), any());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(variableJson)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleBinaryVariable() {
    byte[] bytes = "someContent".getBytes();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
  }

  @Test
  void testPutSingleBinaryVariableWithValueType() {
    byte[] bytes = "someContent".getBytes();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
      .multiPart("valueType", "Bytes", "text/plain")
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
  }

  @Test
  void testPutSingleBinaryVariableWithNoValue() {
    byte[] bytes = new byte[0];

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .multiPart("data", null, bytes)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsPrimitiveValue.bytesValue(bytes)));
  }

  @Test
  void testPutSingleBinaryVariableThrowsAuthorizationException() {
    byte[] bytes = "someContent".getBytes();
    String variableKey = "aVariableKey";

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(runtimeServiceMock).setVariable(anyString(), anyString(), any());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
      .multiPart("data", "unspecified", bytes)
    .expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);
  }

  @Test
  void testPutSingleSerializableVariable() throws Exception {

    ArrayList<String> serializable = new ArrayList<>();
    serializable.add("foo");

    ObjectMapper mapper = new ObjectMapper();
    String jsonBytes = mapper.writeValueAsString(serializable);
    String typeName = TypeFactory.defaultInstance().constructType(serializable.getClass()).toCanonical();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .multiPart("data", jsonBytes, MediaType.APPLICATION_JSON)
      .multiPart("type", typeName, MediaType.TEXT_PLAIN)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsObjectValue.objectValueMatcher().isDeserialized().value(serializable)));
  }

  @Test
  void testPutSingleSerializableVariableUnsupportedMediaType() throws Exception {

    ArrayList<String> serializable = new ArrayList<>();
    serializable.add("foo");

    ObjectMapper mapper = new ObjectMapper();
    String jsonBytes = mapper.writeValueAsString(serializable);
    String typeName = TypeFactory.defaultInstance().constructType(serializable.getClass()).toCanonical();

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .multiPart("data", jsonBytes, "unsupported")
      .multiPart("type", typeName, MediaType.TEXT_PLAIN)
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body(Matchers.containsString("Unrecognized content type for serialized java type: unsupported"))
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    verify(runtimeServiceMock, never()).setVariable(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, variableKey,
        serializable);
  }

  @Test
  void testPutSingleVariableFromSerialized() {
    String serializedValue = "{\"prop\" : \"value\"}";
    Map<String, Object> requestJson = VariablesBuilder
        .getObjectValueMap(serializedValue, ValueType.OBJECT.getName(), "aDataFormat", "aRootType");

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(requestJson)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(
        eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsObjectValue.objectValueMatcher()
          .serializedValue(serializedValue)
          .serializationFormat("aDataFormat")
          .objectTypeName("aRootType")));
  }

  @Test
  void testPutSingleVariableFromInvalidSerialized() {
    String serializedValue = "{\"prop\" : \"value\"}";

    Map<String, Object> requestJson = VariablesBuilder
        .getObjectValueMap(serializedValue, "aNonExistingType", null, null);

    String variableKey = "aVariableKey";

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON)
      .body(requestJson)
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.equalTo("Cannot put process instance variable aVariableKey: Unsupported value type 'aNonExistingType'"))
    .when()
      .put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPutSingleVariableFromSerializedWithNoValue() {
    String variableKey = "aVariableKey";
    Map<String, Object> requestJson = VariablesBuilder
        .getObjectValueMap(null, ValueType.OBJECT.getName(), null, null);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(requestJson)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(
        eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsObjectValue.objectValueMatcher()));
  }

  @Test
  void testPutSingleVariableWithNoValue() {
    String variableKey = "aVariableKey";

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(EMPTY_JSON_OBJECT)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey),
        argThat(EqualsNullValue.matcher()));
  }

  @Test
  void testPutVariableForNonExistingInstance() {
    String variableKey = "aVariableKey";
    String variableValue = "aVariableValue";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue);

    doThrow(new ProcessEngineException("expected exception"))
      .when(runtimeServiceMock)
      .setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), any());

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
      .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", Matchers.is(RestException.class.getSimpleName()))
      .body("message", Matchers.is("Cannot put process instance variable " + variableKey + ": expected exception"))
      .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void shouldReturnErrorOnSettingVariable() {
    String variableKey = "aVariableKey";
    String variableValue = "aVariableValue";

    Map<String, Object> variableJson = VariablesBuilder.getVariableValueMap(variableValue);

    doThrow(new ProcessEngineException("foo", 123))
        .when(runtimeServiceMock)
        .setVariable(eq(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID), eq(variableKey), any());

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .contentType(ContentType.JSON).body(variableJson)
    .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", Matchers.is(RestException.class.getSimpleName()))
      .body("message", Matchers.is("Cannot put process instance variable aVariableKey: foo"))
      .body("code", Matchers.is(123))
    .when().put(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testPostSingleFileVariableWithEncodingAndMimeType() {

    byte[] value = "some text".getBytes();
    String variableKey = "aVariableKey";
    String encoding = UTF_8.name();
    String filename = "test.txt";
    String mimetype = MediaType.TEXT_PLAIN;

    given()
      .pathParam("id", EXAMPLE_TASK_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, value, mimetype + "; encoding="+encoding)
      .multiPart("valueType", "File", "text/plain")
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    ArgumentCaptor<FileValue> captor = ArgumentCaptor.forClass(FileValue.class);
    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_TASK_ID), eq(variableKey),
        captor.capture());
    FileValue captured = captor.getValue();
    assertThat(captured.getEncoding()).isEqualTo(encoding);
    assertThat(captured.getFilename()).isEqualTo(filename);
    assertThat(captured.getMimeType()).isEqualTo(mimetype);
    assertThat(IoUtil.readInputStream(captured.getValue(), null)).isEqualTo(value);
  }

  @Test
  void testPostSingleFileVariableWithMimeType() {

    byte[] value = "some text".getBytes();
    String variableKey = "aVariableKey";
    String filename = "test.txt";
    String mimetype = MediaType.TEXT_PLAIN;

    given()
      .pathParam("id", EXAMPLE_TASK_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, value, mimetype)
      .multiPart("valueType", "File", "text/plain")
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    ArgumentCaptor<FileValue> captor = ArgumentCaptor.forClass(FileValue.class);
    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_TASK_ID), eq(variableKey),
        captor.capture());
    FileValue captured = captor.getValue();
    assertThat(captured.getEncoding()).isNull();
    assertThat(captured.getFilename()).isEqualTo(filename);
    assertThat(captured.getMimeType()).isEqualTo(mimetype);
    assertThat(IoUtil.readInputStream(captured.getValue(), null)).isEqualTo(value);
  }

  @Test
  void testPostSingleFileVariableWithEncoding() {

    byte[] value = "some text".getBytes();
    String variableKey = "aVariableKey";
    String encoding = UTF_8.name();
    String filename = "test.txt";

    given()
      .pathParam("id", EXAMPLE_TASK_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, value, "encoding="+encoding)
      .multiPart("valueType", "File", "text/plain")
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      //when the user passes an encoding, he has to provide the type, too
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);
  }

  @Test
  void testPostSingleFileVariableOnlyFilename() throws Exception {

    String variableKey = "aVariableKey";
    String filename = "test.txt";

    given()
      .pathParam("id", EXAMPLE_TASK_ID).pathParam("varId", variableKey)
      .multiPart("data", filename, new byte[0])
      .multiPart("valueType", "File", "text/plain")
      .header("accept", MediaType.APPLICATION_JSON)
    .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .post(SINGLE_PROCESS_INSTANCE_BINARY_VARIABLE_URL);

    ArgumentCaptor<FileValue> captor = ArgumentCaptor.forClass(FileValue.class);
    verify(runtimeServiceMock).setVariable(eq(MockProvider.EXAMPLE_TASK_ID), eq(variableKey),
        captor.capture());
    FileValue captured = captor.getValue();
    assertThat(captured.getEncoding()).isNull();
    assertThat(captured.getFilename()).isEqualTo(filename);
    assertThat(captured.getMimeType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    assertThat(captured.getValue().available()).isZero();
  }

  @Test
  void testDeleteSingleVariable() {
    String variableKey = "aVariableKey";

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().delete(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);

    verify(runtimeServiceMock).removeVariable(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, variableKey);
  }

  @Test
  void testDeleteVariableForNonExistingInstance() {
    String variableKey = "aVariableKey";

    doThrow(new ProcessEngineException("expected exception"))
      .when(runtimeServiceMock).removeVariable(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, variableKey);

    given().pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID).pathParam("varId", variableKey)
      .then().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", Matchers.is(RestException.class.getSimpleName()))
      .body("message", Matchers.is("Cannot delete process instance variable " + variableKey + ": expected exception"))
      .when().delete(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testDeleteVariableThrowsAuthorizationException() {
    String variableKey = "aVariableKey";

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(runtimeServiceMock).removeVariable(anyString(), anyString());

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .pathParam("varId", variableKey)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.is(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.is(message))
    .when()
      .delete(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

  @Test
  void testActivateInstance() {
    ProcessInstanceSuspensionStateDto dto = new ProcessInstanceSuspensionStateDto();
    dto.setSuspended(false);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    verify(mockUpdateSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateThrowsProcessEngineException() {
    ProcessInstanceSuspensionStateDto dto = new ProcessInstanceSuspensionStateDto();
    dto.setSuspended(false);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockUpdateSuspensionStateBuilder)
      .activate();

    given()
      .pathParam("id", MockProvider.EXAMPLE_NON_EXISTENT_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", Matchers.is(ProcessEngineException.class.getSimpleName()))
        .body("message", Matchers.is(expectedMessage))
      .when()
        .put(SINGLE_PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testActivateThrowsAuthorizationException() {
    ProcessInstanceSuspensionStateDto dto = new ProcessInstanceSuspensionStateDto();
    dto.setSuspended(false);

    String message = "expectedMessage";

    doThrow(new AuthorizationException(message))
      .when(mockUpdateSuspensionStateBuilder)
      .activate();

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
    .put(SINGLE_PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendInstance() {
    ProcessInstanceSuspensionStateDto dto = new ProcessInstanceSuspensionStateDto();
    dto.setSuspended(true);

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(SINGLE_PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceId(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    verify(mockUpdateSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendThrowsProcessEngineException() {
    ProcessInstanceSuspensionStateDto dto = new ProcessInstanceSuspensionStateDto();
    dto.setSuspended(true);

    String expectedMessage = "expectedMessage";

    doThrow(new ProcessEngineException(expectedMessage))
      .when(mockUpdateSuspensionStateBuilder)
      .suspend();

    given()
      .pathParam("id", MockProvider.EXAMPLE_NON_EXISTENT_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", Matchers.is(ProcessEngineException.class.getSimpleName()))
        .body("message", Matchers.is(expectedMessage))
      .when()
        .put(SINGLE_PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendThrowsAuthorizationException() {
    ProcessInstanceSuspensionStateDto dto = new ProcessInstanceSuspensionStateDto();
    dto.setSuspended(true);

    String message = "expectedMessage";

    doThrow(new AuthorizationException(message))
      .when(mockUpdateSuspensionStateBuilder)
      .suspend();

    given()
      .pathParam("id", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(dto)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
    .put(SINGLE_PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionKey() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockUpdateSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionKeyWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockUpdateSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", Matchers.is(ProcessEngineException.class.getSimpleName()))
        .body("message", Matchers.is(expectedException))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionKeyThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "expectedMessage";

    doThrow(new AuthorizationException(message))
      .when(mockUpdateSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
    .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionKey() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockUpdateSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionKeyWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockUpdateSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", Matchers.is(ProcessEngineException.class.getSimpleName()))
        .body("message", Matchers.is(expectedException))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionKeyThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "expectedMessage";

    doThrow(new AuthorizationException(message))
      .when(mockUpdateSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
    .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionKeyAndTenantId() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockUpdateSuspensionStateBuilder).processDefinitionTenantId(MockProvider.EXAMPLE_TENANT_ID);
    verify(mockUpdateSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionKeyWithoutTenantId() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockUpdateSuspensionStateBuilder).processDefinitionWithoutTenantId();
    verify(mockUpdateSuspensionStateBuilder).activate();
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionKeyAndTenantId() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockUpdateSuspensionStateBuilder).processDefinitionTenantId(MockProvider.EXAMPLE_TENANT_ID);
    verify(mockUpdateSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionKeyWithoutTenantId() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionKey(MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);
    verify(mockUpdateSuspensionStateBuilder).processDefinitionWithoutTenantId();
    verify(mockUpdateSuspensionStateBuilder).suspend();
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionId() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockUpdateSuspensionStateBuilder).activate();
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionIdWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockUpdateSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", Matchers.is(ProcessEngineException.class.getSimpleName()))
        .body("message", Matchers.is(expectedException))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessInstanceByProcessDefinitionIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String message = "expectedMessage";

    doThrow(new AuthorizationException(message))
      .when(mockUpdateSuspensionStateBuilder)
      .activate();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionId() {
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
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessDefinitionId(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    verify(mockUpdateSuspensionStateBuilder).suspend();
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionIdWithException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String expectedException = "expectedException";
    doThrow(new ProcessEngineException(expectedException))
      .when(mockUpdateSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .body("type", Matchers.is(ProcessEngineException.class.getSimpleName()))
        .body("message", Matchers.is(expectedException))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessInstanceByProcessDefinitionIdThrowsAuthorizationException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);

    String message = "expectedMessage";

    doThrow(new AuthorizationException(message))
      .when(mockUpdateSuspensionStateBuilder)
      .suspend();

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testActivateProcessInstanceByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", false);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    String message = "Either processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
        .body("message", Matchers.is(message))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessInstanceByIdShouldThrowException() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processInstanceId", MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    String message = "Either processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
        .body("message", Matchers.is(message))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendWithMultipleByParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);
    params.put("processDefinitionId", MockProvider.EXAMPLE_PROCESS_DEFINITION_ID);
    params.put("processDefinitionKey", MockProvider.EXAMPLE_PROCESS_DEFINITION_KEY);

    String message = "Only one of processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
        .body("message", Matchers.is(message))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }

  @Test
  void testSuspendProcessInstanceByNothing() {
    Map<String, Object> params = new HashMap<>();
    params.put("suspended", true);

    String message = "Either processDefinitionId or processDefinitionKey should be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(params)
    .then()
      .expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
        .body("message", Matchers.is(message))
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);
  }


  @Test
  void testSuspendInstances() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceIds", ids);
    messageBodyJson.put("suspended", true);
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceIds(ids);
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).suspend();
  }


  @Test
  void testActivateInstances() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceIds", ids);
    messageBodyJson.put("suspended", false);

    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceIds(ids);
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).activate();
  }

  @Test
  void testSuspendInstancesMultipleGroupOperations() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceIds", ids);
    messageBodyJson.put("processInstanceQuery", query);
    messageBodyJson.put("suspended", true);


    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
      .then()
      .expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
      .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceIds(ids);
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).byProcessInstanceQuery(query.toQuery(processEngine));
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).suspend();
  }


  @Test
  void testSuspendProcessInstanceQuery() {
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceQuery", query);
    messageBodyJson.put("suspended", true);
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceQuery(query.toQuery(processEngine));
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).suspend();
  }


  @Test
  void testActivateProcessInstanceQuery() {
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceQuery", query);
    messageBodyJson.put("suspended", false);

    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceQuery(query.toQuery(processEngine));
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).activate();
  }


  @Test
  void testSuspendHistoricProcessInstanceQuery() {
    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("historicProcessInstanceQuery", query);
    messageBodyJson.put("suspended", true);
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byHistoricProcessInstanceQuery(any());
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).suspend();
  }


  @Test
  void testActivateHistoricProcessInstanceQuery() {
    HistoricProcessInstanceDto query = new HistoricProcessInstanceDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("historicProcessInstanceQuery", query);
    messageBodyJson.put("suspended", false);

    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .put(PROCESS_INSTANCE_SUSPENDED_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byHistoricProcessInstanceQuery(any());
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).activate();
  }

  @Test
  void testSuspendAsyncWithProcessInstances() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    messageBodyJson.put("processInstanceIds", ids);
    messageBodyJson.put("suspended", true);

    when(mockUpdateProcessInstancesSuspensionStateBuilder.suspendAsync()).thenReturn(new BatchEntity());
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_SUSPENDED_ASYNC_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceIds(ids);
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).suspendAsync();
  }

  @Test
  void testActivateAsyncWithProcessInstances() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    messageBodyJson.put("processInstanceIds", ids);
    messageBodyJson.put("suspended", false);

    when(mockUpdateProcessInstancesSuspensionStateBuilder.activateAsync()).thenReturn(new BatchEntity());
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_SUSPENDED_ASYNC_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceIds(ids);
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).activateAsync();
  }

  @Test
  void testSuspendAsyncWithProcessInstanceQuery() {
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceQuery", query);
    messageBodyJson.put("suspended", true);


    when(mockUpdateProcessInstancesSuspensionStateBuilder.suspendAsync()).thenReturn(new BatchEntity());
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
      .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(PROCESS_INSTANCE_SUSPENDED_ASYNC_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceQuery(query.toQuery(processEngine));
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).suspendAsync();
  }

  @Test
  void testActivateAsyncWithProcessInstanceQuery() {
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceQuery", query);
    messageBodyJson.put("suspended", false);

    when(mockUpdateProcessInstancesSuspensionStateBuilder.activateAsync()).thenReturn(new BatchEntity());
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
      .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(PROCESS_INSTANCE_SUSPENDED_ASYNC_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceQuery(query.toQuery(processEngine));
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).activateAsync();
  }

  @Test
  void testSuspendAsyncWithMultipleGroupOperations() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstanceIds", ids);
    messageBodyJson.put("processInstanceQuery", query);
    messageBodyJson.put("suspended", true);

    when(mockUpdateProcessInstancesSuspensionStateBuilder.suspendAsync()).thenReturn(new BatchEntity());
    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
      .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .when()
      .post(PROCESS_INSTANCE_SUSPENDED_ASYNC_URL);

    verify(mockUpdateSuspensionStateSelectBuilder).byProcessInstanceIds(ids);
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).byProcessInstanceQuery(query.toQuery(processEngine));
    verify(mockUpdateProcessInstancesSuspensionStateBuilder).suspendAsync();
  }


  @Test
  void testSuspendAsyncWithNothing() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("suspended", true);

    String message = "Either processInstanceIds, processInstanceQuery or historicProcessInstanceQuery should be set to update the suspension state.";

    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
      .then()
      .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.is(message))
      .when()
      .post(PROCESS_INSTANCE_SUSPENDED_ASYNC_URL);

  }

  @Test
  void testProcessInstanceModification() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);

    Map<String, Object> json = new HashMap<>();
    json.put("skipCustomListeners", true);
    json.put("skipIoMappings", true);

    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.cancellation().activityInstanceId("activityInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.cancellation().transitionInstanceId("transitionInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore()
        .activityId("activityId").ancestorActivityInstanceId("ancestorActivityInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter()
        .activityId("activityId").ancestorActivityInstanceId("ancestorActivityInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());
    instructions.add(ModificationInstructionBuilder.startTransition()
        .transitionId("transitionId").ancestorActivityInstanceId("ancestorActivityInstanceId").getJson());

    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_MODIFICATION_URL);

    verify(runtimeServiceMock).createProcessInstanceModification(EXAMPLE_PROCESS_INSTANCE_ID);

    InOrder inOrder = inOrder(mockModificationBuilder);
    inOrder.verify(mockModificationBuilder).cancelAllForActivity("activityId");
    inOrder.verify(mockModificationBuilder).cancelActivityInstance("activityInstanceId");
    inOrder.verify(mockModificationBuilder).cancelTransitionInstance("transitionInstanceId");
    inOrder.verify(mockModificationBuilder).startBeforeActivity("activityId");
    inOrder.verify(mockModificationBuilder).startBeforeActivity("activityId", "ancestorActivityInstanceId");
    inOrder.verify(mockModificationBuilder).startAfterActivity("activityId");
    inOrder.verify(mockModificationBuilder).startAfterActivity("activityId", "ancestorActivityInstanceId");
    inOrder.verify(mockModificationBuilder).startTransition("transitionId");
    inOrder.verify(mockModificationBuilder).startTransition("transitionId", "ancestorActivityInstanceId");

    inOrder.verify(mockModificationBuilder).execute(true, true);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testProcessInstanceModificationWithVariables() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", "value", "String", false)
              .variable("varLocal", "valueLocal", "String", true)
              .getVariables())
          .getJson());
    instructions.add(
        ModificationInstructionBuilder.startAfter()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", 52, "Integer", false)
              .variable("varLocal", 74, "Integer", true)
              .getVariables())
          .getJson());

    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_MODIFICATION_URL);

    verify(runtimeServiceMock).createProcessInstanceModification(EXAMPLE_PROCESS_INSTANCE_ID);

    InOrder inOrder = inOrder(mockModificationBuilder);
    inOrder.verify(mockModificationBuilder).startBeforeActivity("activityId");

    verify(mockModificationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.stringValue("valueLocal")));
    verify(mockModificationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.stringValue("value")));

    inOrder.verify(mockModificationBuilder).startAfterActivity("activityId");

    verify(mockModificationBuilder).setVariable(eq("var"), argThat(EqualsPrimitiveValue.integerValue(52)));
    verify(mockModificationBuilder).setVariableLocal(eq("varLocal"), argThat(EqualsPrimitiveValue.integerValue(74)));

    inOrder.verify(mockModificationBuilder).execute(false, false);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testInvalidModification() {
    Map<String, Object> json = new HashMap<>();

    // start before: missing activity id
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startBefore().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("'activityId' must be set"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_URL);

    // start after: missing ancestor activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("'activityId' must be set"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_URL);

    // start transition: missing ancestor activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startTransition().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("'transitionId' must be set"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_URL);

    // cancel: missing activity id and activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.cancellation().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("For instruction type 'cancel': exactly one, "
          + "'activityId', 'activityInstanceId', or 'transitionInstanceId', is required"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_URL);

    // cancel: both, activity id and activity instance id, set
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("anActivityId")
        .activityInstanceId("anActivityInstanceId").getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("For instruction type 'cancel': exactly one, "
          + "'activityId', 'activityInstanceId', or 'transitionInstanceId', is required"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_URL);

  }

  @Test
  void testModifyProcessInstanceThrowsAuthorizationException() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(mockModificationBuilder).execute(anyBoolean(), anyBoolean());

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", "value", "String", false)
              .variable("varLocal", "valueLocal", "String", true)
              .getVariables())
          .getJson());
    instructions.add(
        ModificationInstructionBuilder.startAfter()
          .activityId("activityId")
          .variables(VariablesBuilder.create()
              .variable("var", 52, "Integer", false)
              .variable("varLocal", 74, "Integer", true)
              .getVariables())
          .getJson());

    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_URL);
  }

  @Test
  void testSyncProcessInstanceModificationWithAnnotation() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .getJson());
    json.put("instructions", instructions);
    json.put("annotation", "anAnnotation");

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_MODIFICATION_URL);

    verify(runtimeServiceMock).createProcessInstanceModification(EXAMPLE_PROCESS_INSTANCE_ID);

    InOrder inOrder = inOrder(mockModificationBuilder);
    inOrder.verify(mockModificationBuilder).startBeforeActivity("activityId");
    inOrder.verify(mockModificationBuilder).setAnnotation("anAnnotation");


    inOrder.verify(mockModificationBuilder).execute(false, false);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testSetRetriesByProcessAsync() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstances", ids);
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);

    Response response = given()
        .contentType(ContentType.JSON)
        .body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(SET_JOB_RETRIES_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceQuery(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncWithDueDate() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstances", ids);
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    Date newDueDate = new Date(1675752840000L);
    messageBodyJson.put("dueDate", newDueDate);

    Response response =
        given()
          .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(SET_JOB_RETRIES_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceQuery(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).dueDate(newDueDate);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncWithNullDueDate() {
    List<String> ids = Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstances", ids);
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    messageBodyJson.put("dueDate", null);

    Response response =
        given()
          .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(SET_JOB_RETRIES_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceQuery(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).dueDate(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncWithQueryAndDueDate() {
    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(new ProcessInstanceQueryImpl());
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    messageBodyJson.put("processInstanceQuery", query);
    Date newDueDate = new Date(1675752840000L);
    messageBodyJson.put("dueDate", newDueDate);

    Response response = given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .when().post(SET_JOB_RETRIES_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceQuery(any(ProcessInstanceQuery.class));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).dueDate(newDueDate);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncWithQuery() {
    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(new ProcessInstanceQueryImpl());
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    ProcessInstanceQueryDto query = new ProcessInstanceQueryDto();
    messageBodyJson.put("processInstanceQuery", query);

    Response response =
        given()
          .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(SET_JOB_RETRIES_ASYNC_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceQuery(any(ProcessInstanceQuery.class));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessWithBadRequestQuery() {
    doThrow(new BadUserRequestException("job ids are empty"))
        .when(mockSetJobRetriesByProcessAsyncBuilder).executeAsync();

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);

    given()
        .contentType(ContentType.JSON).body(messageBodyJson)
        .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when().post(SET_JOB_RETRIES_ASYNC_URL);

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceQuery(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessWithoutRetries() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put("processInstances", null);

    given()
        .contentType(ContentType.JSON)
        .body(messageBodyJson)
        .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .when().post(SET_JOB_RETRIES_ASYNC_URL);
  }

  @Test
  void testSetRetriesByProcessWithNegativeRetries() {
    doThrow(new BadUserRequestException("retries are negative"))
        .when(mockManagementService).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);

    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);
    HistoricProcessInstanceQueryDto query = new HistoricProcessInstanceQueryDto();
    messageBodyJson.put("processInstanceQuery", query);

    given()
      .contentType(ContentType.JSON)
      .body(messageBodyJson)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .post(SET_JOB_RETRIES_ASYNC_URL);

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithQuery() {
    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedHistoricProcessInstanceQuery);
    List<HistoricProcessInstance> historicProcessInstances = MockProvider.createMockRunningHistoricProcessInstances();
    when(mockedHistoricProcessInstanceQuery.list()).thenReturn(historicProcessInstances);

    Map<String, Object> body = new HashMap<>();
    body.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    body.put("historicProcessInstanceQuery", new HistoricProcessInstanceQueryDto());

    Response response = given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).historicProcessInstanceQuery(any());
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithQueryAndDueDate() {
    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedHistoricProcessInstanceQuery);
    List<HistoricProcessInstance> historicProcessInstances = MockProvider.createMockRunningHistoricProcessInstances();
    when(mockedHistoricProcessInstanceQuery.list()).thenReturn(historicProcessInstances);

    Map<String, Object> body = new HashMap<>();
    body.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    body.put("historicProcessInstanceQueryDto", new HistoricProcessInstanceQueryDto());
    Date newDueDate = new Date(1675752840000L);
    body.put("dueDate", newDueDate);

    Response response =
        given()
          .contentType(ContentType.JSON).body(body)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).historicProcessInstanceQuery(any());
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).dueDate(newDueDate);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithQueryAndNullDueDate() {
    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedHistoricProcessInstanceQuery);
    List<HistoricProcessInstance> historicProcessInstances = MockProvider.createMockRunningHistoricProcessInstances();
    when(mockedHistoricProcessInstanceQuery.list()).thenReturn(historicProcessInstances);

    Map<String, Object> body = new HashMap<>();
    body.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    body.put("historicProcessInstanceQuery", new HistoricProcessInstanceQueryDto());
    body.put("dueDate",null);

    Response response =
        given()
          .contentType(ContentType.JSON).body(body)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verifyBatchJson(response.asString());

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).historicProcessInstanceQuery(any());
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).dueDate(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithProcessInstanceIds() {
    Map<String, Object> messageBodyJson = new HashMap<>();
    messageBodyJson.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    messageBodyJson.put("processInstances", Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));

    given()
      .contentType(ContentType.JSON).body(messageBodyJson)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).historicProcessInstanceQuery(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithProcessInstanceIdsAndDueDate() {
    Map<String, Object> body = new HashMap<>();
    body.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    body.put("processInstances", Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));
    Date newDueDate = new Date(1675752840000L);
    body.put("dueDate", newDueDate);

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(Arrays.asList(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).historicProcessInstanceQuery(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).dueDate(newDueDate);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithQueryAndProcessInstanceIds() {
    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedHistoricProcessInstanceQuery);
    List<HistoricProcessInstance> historicProcessInstances = MockProvider.createMockRunningHistoricProcessInstances();
    when(mockedHistoricProcessInstanceQuery.list()).thenReturn(historicProcessInstances);

    Map<String, Object> body = new HashMap<>();
    body.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);
    body.put("processInstances", Arrays.asList(MockProvider.ANOTHER_EXAMPLE_PROCESS_INSTANCE_ID));
    body.put("historicProcessInstanceQuery", new HistoricProcessInstanceQueryDto());

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when().post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(Arrays.asList(MockProvider.ANOTHER_EXAMPLE_PROCESS_INSTANCE_ID));
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).historicProcessInstanceQuery(any());
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithBadRequestQuery() {
    doThrow(new BadUserRequestException("jobIds is empty"))
      .when(mockSetJobRetriesByProcessAsyncBuilder).executeAsync();

    Map<String, Object> body = new HashMap<>();
    body.put(RETRIES, MockProvider.EXAMPLE_JOB_RETRIES);

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when().post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_JOB_RETRIES);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).processInstanceIds(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).historicProcessInstanceQuery(null);
    verify(mockSetJobRetriesByProcessAsyncBuilder, times(1)).executeAsync();
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testSetRetriesByProcessAsyncHistoricQueryBasedWithNegativeRetries() {
    doThrow(new BadUserRequestException("retries are negative"))
      .when(mockManagementService).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);

    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery = mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(mockedHistoricProcessInstanceQuery);
    List<HistoricProcessInstance> historicProcessInstances = MockProvider.createMockRunningHistoricProcessInstances();
    when(mockedHistoricProcessInstanceQuery.list()).thenReturn(historicProcessInstances);

    Map<String, Object> body = new HashMap<>();
    body.put(RETRIES, MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);
    body.put("historicProcessInstanceQuery", new HistoricProcessInstanceQueryDto());

    given()
      .contentType(ContentType.JSON).body(body)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when().post(SET_JOB_RETRIES_ASYNC_HIST_QUERY_URL);

    verify(mockManagementService, times(1)).setJobRetriesByProcessAsync(MockProvider.EXAMPLE_NEGATIVE_JOB_RETRIES);
    verifyNoMoreInteractions(mockSetJobRetriesByProcessAsyncBuilder);
  }

  @Test
  void testProcessInstanceModificationAsync() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);
    Batch batchMock = createMockBatch();
    when(mockModificationBuilder.executeAsync(anyBoolean(), anyBoolean())).thenReturn(batchMock);

    Map<String, Object> json = new HashMap<>();
    json.put("skipCustomListeners", true);
    json.put("skipIoMappings", true);

    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.cancellation().activityInstanceId("activityInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.cancellation().transitionInstanceId("transitionInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startBefore()
        .activityId("activityId").ancestorActivityInstanceId("ancestorActivityInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter().activityId("activityId").getJson());
    instructions.add(ModificationInstructionBuilder.startAfter()
        .activityId("activityId").ancestorActivityInstanceId("ancestorActivityInstanceId").getJson());
    instructions.add(ModificationInstructionBuilder.startTransition().transitionId("transitionId").getJson());
    instructions.add(ModificationInstructionBuilder.startTransition()
        .transitionId("transitionId").ancestorActivityInstanceId("ancestorActivityInstanceId").getJson());

    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

    verify(runtimeServiceMock).createProcessInstanceModification(EXAMPLE_PROCESS_INSTANCE_ID);

    InOrder inOrder = inOrder(mockModificationBuilder);
    inOrder.verify(mockModificationBuilder).cancelAllForActivity("activityId");
    inOrder.verify(mockModificationBuilder).cancelActivityInstance("activityInstanceId");
    inOrder.verify(mockModificationBuilder).cancelTransitionInstance("transitionInstanceId");
    inOrder.verify(mockModificationBuilder).startBeforeActivity("activityId");
    inOrder.verify(mockModificationBuilder).startBeforeActivity("activityId", "ancestorActivityInstanceId");
    inOrder.verify(mockModificationBuilder).startAfterActivity("activityId");
    inOrder.verify(mockModificationBuilder).startAfterActivity("activityId", "ancestorActivityInstanceId");
    inOrder.verify(mockModificationBuilder).startTransition("transitionId");
    inOrder.verify(mockModificationBuilder).startTransition("transitionId", "ancestorActivityInstanceId");

    inOrder.verify(mockModificationBuilder).executeAsync(true, true);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testInvalidModificationAsync() {
    Map<String, Object> json = new HashMap<>();

    // start before: missing activity id
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startBefore().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("'activityId' must be set"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

    // start after: missing ancestor activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startAfter().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("'activityId' must be set"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

    // start transition: missing ancestor activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.startTransition().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("'transitionId' must be set"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

    // cancel: missing activity id and activity instance id
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.cancellation().getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("For instruction type 'cancel': exactly one, "
          + "'activityId', 'activityInstanceId', or 'transitionInstanceId', is required"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

    // cancel: both, activity id and activity instance id, set
    instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("anActivityId")
        .activityInstanceId("anActivityInstanceId").getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", Matchers.is(InvalidRequestException.class.getSimpleName()))
      .body("message", Matchers.containsString("For instruction type 'cancel': exactly one, "
          + "'activityId', 'activityInstanceId', or 'transitionInstanceId', is required"))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

  }

  @Test
  void testModifyProcessInstanceAsyncThrowsAuthorizationException() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);

    String message = "expected exception";
    doThrow(new AuthorizationException(message)).when(mockModificationBuilder).executeAsync(anyBoolean(), anyBoolean());

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();

    instructions.add(
        ModificationInstructionBuilder.startBefore()
          .activityId("activityId")
          .getJson());
    instructions.add(
        ModificationInstructionBuilder.startAfter()
          .activityId("activityId")
          .getJson());

    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .contentType(ContentType.JSON)
      .body("type", Matchers.equalTo(AuthorizationException.class.getSimpleName()))
      .body("message", Matchers.equalTo(message))
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);
  }


  @Test
  void testAsyncProcessInstanceModificationWithAnnotation() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);
    Batch batchMock = createMockBatch();
    when(mockModificationBuilder.executeAsync(anyBoolean(), anyBoolean())).thenReturn(batchMock);

    Map<String, Object> json = new HashMap<>();

    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());

    json.put("instructions", instructions);
    json.put("annotation", "anAnnotation");

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

    verify(runtimeServiceMock).createProcessInstanceModification(EXAMPLE_PROCESS_INSTANCE_ID);

    InOrder inOrder = inOrder(mockModificationBuilder);
    inOrder.verify(mockModificationBuilder).cancelAllForActivity("activityId");
    inOrder.verify(mockModificationBuilder).setAnnotation("anAnnotation");

    inOrder.verify(mockModificationBuilder).executeAsync(false, false);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testSyncProcessInstanceModificationCancellationSource() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);

    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
      .when()
        .post(PROCESS_INSTANCE_MODIFICATION_URL);

    verify(mockModificationBuilder).cancellationSourceExternal(true);
  }

  @Test
  void testAsyncProcessInstanceModificationCancellationSource() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder = setUpMockModificationBuilder();
    when(runtimeServiceMock.createProcessInstanceModification(anyString())).thenReturn(mockModificationBuilder);
    Batch batchMock = createMockBatch();
    when(mockModificationBuilder.executeAsync(anyBoolean(), anyBoolean())).thenReturn(batchMock);

    Map<String, Object> json = new HashMap<>();
    List<Map<String, Object>> instructions = new ArrayList<>();
    instructions.add(ModificationInstructionBuilder.cancellation().activityId("activityId").getJson());
    json.put("instructions", instructions);

    given()
      .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
      .contentType(ContentType.JSON)
      .body(json)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
    .when()
      .post(PROCESS_INSTANCE_MODIFICATION_ASYNC_URL);

    verify(mockModificationBuilder).cancellationSourceExternal(true);
  }

  @Test
  void shouldSetVariablesAsync() {
    // given
    Batch batchMock = createMockBatch();
    when(runtimeServiceMock.setVariablesAsync(any(), any(), any(), any()))
        .thenReturn(batchMock);

    SetVariablesAsyncDto body = new SetVariablesAsyncDto();

    VariableValueDto variableValueDto = new VariableValueDto();
    variableValueDto.setValue("bar");

    body.setVariables(Collections.singletonMap("foo", variableValueDto));

    // when
    Response response = given()
          .contentType(ContentType.JSON)
          .body(body)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);

    // then
    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    verify(runtimeServiceMock).setVariablesAsync(
        eq(null),
        eq(null),
        eq(null),
        captor.capture()
    );

    assertThat(captor.getValue()).containsEntry("foo", "bar");

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldThrowExceptionWhenSetVariablesAsync_UnsupportedType() {
    // given
    Batch batchMock = createMockBatch();
    when(runtimeServiceMock.setVariablesAsync(any(), any(), any(), any()))
        .thenReturn(batchMock);

    SetVariablesAsyncDto body = new SetVariablesAsyncDto();

    VariableValueDto variableValueDto = new VariableValueDto();
    variableValueDto.setValue("bar");
    variableValueDto.setType("unknown");

    body.setVariables(Collections.singletonMap("foo", variableValueDto));

    // when + then
    given()
          .contentType(ContentType.JSON)
          .body(body)
        .then().expect()
          .statusCode(Status.BAD_REQUEST.getStatusCode())
          .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
          .body("message", equalTo("Cannot set variables: Unsupported value type 'unknown'"))
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);
  }

  /**
   * Thrown when java serialization format is prohibited and java serialized variable is set
   * or null value is given.
   */
  @Test
  void shouldTransformProcessEngineExceptionToInvalidRequestException() {
    // given
    doThrow(new ProcessEngineException("message"))
        .when(runtimeServiceMock).setVariablesAsync(any(), any(), any(), any());

    // when + then
    given()
          .contentType(ContentType.JSON)
          .body("{}")
        .then().expect()
          .statusCode(Status.BAD_REQUEST.getStatusCode())
          .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
          .body("message", equalTo("message"))
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);
  }

  @Test
  void shouldThrowExceptionWhenSetVariablesAsync_AuthorizationException() {
    // given
    doThrow(new AuthorizationException("message"))
        .when(runtimeServiceMock).setVariablesAsync(any(), any(), any(), any());

    // when + then
    given()
          .contentType(ContentType.JSON)
          .body("{}")
        .then().expect()
          .statusCode(Status.FORBIDDEN.getStatusCode())
          .body(Matchers.containsString("{\"type\":\"AuthorizationException\""))
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);
  }

  @Test
  void shouldThrowExceptionWhenSetVariablesAsync_NullValueException() {
    // given
    doThrow(new NullValueException("message"))
        .when(runtimeServiceMock).setVariablesAsync(any(), any(), any(), any());

    // when + then
    given()
          .contentType(ContentType.JSON)
          .body("{}")
        .then().expect()
          .statusCode(Status.BAD_REQUEST.getStatusCode())
          .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
          .body("message", equalTo("message"))
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);
  }

  @Test
  void shouldThrowExceptionWhenSetVariablesAsync_BadUserRequestException() {
    // given
    doThrow(new BadUserRequestException("message"))
        .when(runtimeServiceMock).setVariablesAsync(any(), any(), any(), any());

    // when + then
    given()
          .contentType(ContentType.JSON)
          .body("{}")
        .then().expect()
          .statusCode(Status.BAD_REQUEST.getStatusCode())
          .body("type", equalTo(BadUserRequestException.class.getSimpleName()))
          .body("message", equalTo("message"))
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);
  }

  @Test
  void shouldSetVariablesAsync_RuntimeQuery() {
    // given
    Batch batchMock = createMockBatch();
    when(runtimeServiceMock.setVariablesAsync(any(), any(), any(), any()))
        .thenReturn(batchMock);

    ProcessInstanceQuery mockedProcessInstanceQuery = mock(ProcessInstanceQuery.class);
    when(runtimeServiceMock.createProcessInstanceQuery())
        .thenReturn(mockedProcessInstanceQuery);

    SetVariablesAsyncDto body = new SetVariablesAsyncDto();

    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setProcessDefinitionId("foo");
    body.setProcessInstanceQuery(processInstanceQueryDto);

    // when
    Response response = given()
          .contentType(ContentType.JSON)
          .body(body)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);

    // then

    verify(mockedProcessInstanceQuery)
        .processDefinitionId("foo");

    verify(runtimeServiceMock).setVariablesAsync(
        null,
        mockedProcessInstanceQuery,
        null,
      emptyMap());

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldSetVariablesAsync_HistoryQuery() {
    // given
    Batch batchMock = createMockBatch();
    when(runtimeServiceMock.setVariablesAsync(any(), any(), any(), any()))
        .thenReturn(batchMock);

    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery =
        mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery())
        .thenReturn(mockedHistoricProcessInstanceQuery);

    SetVariablesAsyncDto body = new SetVariablesAsyncDto();

    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto =
        new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessDefinitionId("foo");
    body.setHistoricProcessInstanceQuery(historicProcessInstanceQueryDto);

    // when
    Response response = given()
          .contentType(ContentType.JSON)
          .body(body)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);

    // then
    verify(mockedHistoricProcessInstanceQuery)
        .processDefinitionId("foo");

    verify(runtimeServiceMock).setVariablesAsync(
        null,
        null,
        mockedHistoricProcessInstanceQuery,
      emptyMap());

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldSetVariablesAsync_ByIds() {
    // given
    Batch batchMock = createMockBatch();
    when(runtimeServiceMock.setVariablesAsync(any(), any(), any(), any()))
        .thenReturn(batchMock);

    SetVariablesAsyncDto body = new SetVariablesAsyncDto();

    List<String> processInstanceIds = Arrays.asList("foo", "bar");
    body.setProcessInstanceIds(processInstanceIds);

    // when
    Response response = given()
          .contentType(ContentType.JSON)
          .body(body)
        .then().expect()
          .statusCode(Status.OK.getStatusCode())
        .when()
          .post(PROCESS_INSTANCE_SET_VARIABLES_ASYNC_URL);

    // then
    verify(runtimeServiceMock).setVariablesAsync(
        processInstanceIds,
        null,
        null,
        emptyMap());

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldCorrelateMessageAsync() {
    // given
    Batch batchMock = createMockBatch();
    MessageCorrelationAsyncBuilder builderMock = mock(MessageCorrelationAsyncBuilder.class, RETURNS_SELF);
    when(builderMock.correlateAllAsync()).thenReturn(batchMock);
    when(runtimeServiceMock.createMessageCorrelationAsync(any())).thenReturn(builderMock);

    CorrelationMessageAsyncDto body = new CorrelationMessageAsyncDto();

    // when
    Response response = given()
      .contentType(ContentType.JSON)
      .body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);

    // then
    verify(runtimeServiceMock).createMessageCorrelationAsync(null);
    verify(builderMock).correlateAllAsync();

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldNotTransformProcessEngineExceptionToInvalidRequestExceptionWhenCorrelateMessageAsync() {
    // given
    doThrow(new ProcessEngineException("message")).when(runtimeServiceMock).createMessageCorrelationAsync(any());

    // when + then
    given()
      .contentType(ContentType.JSON)
      .body("{}")
    .then().expect()
      .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
      .body("type", equalTo(ProcessEngineException.class.getSimpleName()))
      .body("message", equalTo("message"))
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);
  }

  @Test
  void shouldThrowExceptionWhenCorrelateMessageAsync_AuthorizationException() {
    // given
    doThrow(new AuthorizationException("message")).when(runtimeServiceMock).createMessageCorrelationAsync(any());

    // when + then
    given()
      .contentType(ContentType.JSON)
      .body("{}")
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
      .body(Matchers.containsString("{\"type\":\"AuthorizationException\""))
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);
  }

  @Test
  void shouldThrowExceptionWhenCorrelateMessageAsync_NullValueException() {
    // given
    doThrow(new NullValueException("message")).when(runtimeServiceMock).createMessageCorrelationAsync(any());

    // when + then
    given()
      .contentType(ContentType.JSON)
      .body("{}")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("message"))
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);
  }

  @Test
  void shouldThrowExceptionWhenCorrelateMessageAsync_BadUserRequestException() {
    // given
    doThrow(new BadUserRequestException("message")).when(runtimeServiceMock).createMessageCorrelationAsync(any());

    // when + then
    given()
      .contentType(ContentType.JSON)
      .body("{}")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
      .body("type", equalTo(BadUserRequestException.class.getSimpleName()))
      .body("message", equalTo("message"))
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);
  }

  @Test
  void shouldCorrelateMessageAsync_RuntimeQuery() {
    // given
    Batch batchMock = createMockBatch();
    MessageCorrelationAsyncBuilder builderMock = mock(MessageCorrelationAsyncBuilder.class, RETURNS_SELF);
    when(builderMock.correlateAllAsync()).thenReturn(batchMock);
    when(runtimeServiceMock.createMessageCorrelationAsync(any())).thenReturn(builderMock);

    ProcessInstanceQuery mockedProcessInstanceQuery = mock(ProcessInstanceQuery.class);
    when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(mockedProcessInstanceQuery);

    CorrelationMessageAsyncDto body = new CorrelationMessageAsyncDto();

    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setProcessDefinitionId("foo");
    body.setProcessInstanceQuery(processInstanceQueryDto);

    // when
    Response response = given()
      .contentType(ContentType.JSON)
      .body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);

    // then

    verify(mockedProcessInstanceQuery)
        .processDefinitionId("foo");

    verify(runtimeServiceMock).createMessageCorrelationAsync(null);
    verify(builderMock).processInstanceQuery(mockedProcessInstanceQuery);
    verify(builderMock).correlateAllAsync();

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldCorrelateMessageAsync_HistoryQuery() {
    // given
    Batch batchMock = createMockBatch();
    MessageCorrelationAsyncBuilder builderMock = mock(MessageCorrelationAsyncBuilder.class, RETURNS_SELF);
    when(builderMock.correlateAllAsync()).thenReturn(batchMock);
    when(runtimeServiceMock.createMessageCorrelationAsync(any())).thenReturn(builderMock);

    HistoricProcessInstanceQuery mockedHistoricProcessInstanceQuery =
        mock(HistoricProcessInstanceQuery.class);
    when(historyServiceMock.createHistoricProcessInstanceQuery())
        .thenReturn(mockedHistoricProcessInstanceQuery);

    CorrelationMessageAsyncDto body = new CorrelationMessageAsyncDto();

    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto =
        new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessDefinitionId("foo");
    body.setHistoricProcessInstanceQuery(historicProcessInstanceQueryDto);

    // when
    Response response = given()
      .contentType(ContentType.JSON)
      .body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);

    // then
    verify(mockedHistoricProcessInstanceQuery)
        .processDefinitionId("foo");

    verify(runtimeServiceMock).createMessageCorrelationAsync(null);
    verify(builderMock).historicProcessInstanceQuery(mockedHistoricProcessInstanceQuery);
    verify(builderMock).correlateAllAsync();

    verifyBatchJson(response.asString());
  }

  @Test
  void shouldCorrelateMessageAsync_ByIds() {
    // given
    Batch batchMock = createMockBatch();
    MessageCorrelationAsyncBuilder builderMock = mock(MessageCorrelationAsyncBuilder.class, RETURNS_SELF);
    when(builderMock.correlateAllAsync()).thenReturn(batchMock);
    when(runtimeServiceMock.createMessageCorrelationAsync(any())).thenReturn(builderMock);

    CorrelationMessageAsyncDto body = new CorrelationMessageAsyncDto();

    List<String> processInstanceIds = Arrays.asList("foo", "bar");
    body.setProcessInstanceIds(processInstanceIds);

    // when
    Response response = given()
      .contentType(ContentType.JSON)
      .body(body)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .post(PROCESS_INSTANCE_CORRELATE_MESSAGE_ASYNC_URL);

    // then
    verify(runtimeServiceMock).createMessageCorrelationAsync(null);
    verify(builderMock).processInstanceIds(processInstanceIds);
    verify(builderMock).correlateAllAsync();

    verifyBatchJson(response.asString());
  }

  @SuppressWarnings("unchecked")
  protected ProcessInstanceModificationInstantiationBuilder setUpMockModificationBuilder() {
    ProcessInstanceModificationInstantiationBuilder mockModificationBuilder =
        mock(ProcessInstanceModificationInstantiationBuilder.class);

    when(mockModificationBuilder.cancelActivityInstance(anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.cancelAllForActivity(anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.cancellationSourceExternal(anyBoolean())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.startAfterActivity(anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.startAfterActivity(anyString(), anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.startBeforeActivity(anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.startBeforeActivity(anyString(), anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.startTransition(anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.startTransition(anyString(), anyString())).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.setVariables(any(Map.class))).thenReturn(mockModificationBuilder);
    when(mockModificationBuilder.setVariablesLocal(any(Map.class))).thenReturn(mockModificationBuilder);

    return mockModificationBuilder;

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

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void verifyTaskComments(List<Comment> mockTaskComments, Response response) {
    List list = response.as(List.class);
    assertThat(list).hasSize(1);

    LinkedHashMap<String, String> resourceHashMap = (LinkedHashMap<String, String>) list.get(0);

    String returnedId = resourceHashMap.get("id");
    String returnedUserId = resourceHashMap.get("userId");
    String returnedTaskId = resourceHashMap.get("taskId");
    Date returnedTime = DateTimeUtil.parseDate(resourceHashMap.get("time"));
    String returnedFullMessage = resourceHashMap.get("message");

    Comment mockComment = mockTaskComments.get(0);

    assertThat(returnedId).isEqualTo(mockComment.getId());
    assertThat(returnedTaskId).isEqualTo(mockComment.getTaskId());
    assertThat(returnedUserId).isEqualTo(mockComment.getUserId());
    assertThat(returnedTime).isEqualTo(mockComment.getTime());
    assertThat(returnedFullMessage).isEqualTo(mockComment.getFullMessage());
  }
}
