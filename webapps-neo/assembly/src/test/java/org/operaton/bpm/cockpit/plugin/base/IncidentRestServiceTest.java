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
package org.operaton.bpm.cockpit.plugin.base;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.IncidentDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.IncidentQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.IncidentRestService;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author roman.smirnov
 */
class IncidentRestServiceTest extends AbstractCockpitPluginTest {

  private IncidentRestService resource;

  @BeforeEach
  void setUp() {
    resource = new IncidentRestService(processEngine.getName());
  }

  @AfterEach
  void clearAuthentication() {
    identityService.clearAuthentication();
  }

  @AfterEach
  void resetQueryMaxResultsLimit() {
    processEngineConfiguration.setQueryMaxResultsLimit(Integer.MAX_VALUE);
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn"
  })
  void queryByProcessInstanceId() {
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("FailingProcess");
    runtimeService.startProcessInstanceByKey("FailingProcess");

    executeAvailableJobs();

    String incidentId = runtimeService.createIncidentQuery().processInstanceId(processInstance1.getId()).singleResult().getId();

    runtimeService.setAnnotationForIncidentById(incidentId, "an Annotation");

    String[] processInstanceIds= {processInstance1.getId()};

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setProcessInstanceIdIn(processInstanceIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    IncidentDto incident = result.get(0);

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentMessage()).isEqualTo("I am failing!");
    assertThat(incident.getIncidentTimestamp()).isNotNull();
    assertThat(incident.getActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getFailedActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getProcessInstanceId()).isEqualTo(processInstance1.getId());
    assertThat(incident.getProcessDefinitionId()).isEqualTo(processInstance1.getProcessDefinitionId());
    assertThat(incident.getExecutionId()).isEqualTo(processInstance1.getId());
    assertThat(incident.getConfiguration()).isNotNull();
    assertThat(incident.getAnnotation()).isEqualTo("an Annotation");
    assertThat(incident.getCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getCauseIncidentProcessInstanceId()).isEqualTo(processInstance1.getId());
    assertThat(incident.getCauseIncidentProcessDefinitionId()).isEqualTo(processInstance1.getProcessDefinitionId());
    assertThat(incident.getCauseIncidentActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getCauseIncidentFailedActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getRootCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getRootCauseIncidentProcessInstanceId()).isEqualTo(processInstance1.getId());
    assertThat(incident.getRootCauseIncidentProcessDefinitionId()).isEqualTo(processInstance1.getProcessDefinitionId());
    assertThat(incident.getRootCauseIncidentActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getRootCauseIncidentFailedActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getRootCauseIncidentConfiguration()).isNotNull();
    assertThat(incident.getRootCauseIncidentMessage()).isEqualTo("I am failing!");
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn"
  })
  void queryByProcessInstanceIds() {
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("FailingProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("FailingProcess");

    executeAvailableJobs();

    String[] processInstanceIds= {processInstance1.getId(), processInstance2.getId()};

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setProcessInstanceIdIn(processInstanceIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/process-with-two-parallel-failing-services.bpmn"
  })
  void queryByActivityId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processWithTwoParallelFailingServices");

    executeAvailableJobs();

    String[] activityIds= {"theServiceTask1"};

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setActivityIdIn(activityIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    IncidentDto incident = result.get(0);

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentMessage()).isEqualTo("I am failing!");
    assertThat(incident.getIncidentTimestamp()).isNotNull();
    assertThat(incident.getActivityId()).isEqualTo("theServiceTask1");
    assertThat(incident.getFailedActivityId()).isEqualTo("theServiceTask1");
    assertThat(incident.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(incident.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(incident.getExecutionId()).isNotNull();
    assertThat(incident.getConfiguration()).isNotNull();
    assertThat(incident.getCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getCauseIncidentProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(incident.getCauseIncidentProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(incident.getCauseIncidentActivityId()).isEqualTo("theServiceTask1");
    assertThat(incident.getCauseIncidentFailedActivityId()).isEqualTo("theServiceTask1");
    assertThat(incident.getRootCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getRootCauseIncidentProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(incident.getRootCauseIncidentProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(incident.getRootCauseIncidentActivityId()).isEqualTo("theServiceTask1");
    assertThat(incident.getRootCauseIncidentFailedActivityId()).isEqualTo("theServiceTask1");
    assertThat(incident.getRootCauseIncidentConfiguration()).isNotNull();
    assertThat(incident.getRootCauseIncidentMessage()).isEqualTo("I am failing!");
  }

  @Test
  @Deployment(resources = {
    "processes/process-with-two-parallel-failing-services.bpmn"
  })
  void queryByActivityIds() {
    runtimeService.startProcessInstanceByKey("processWithTwoParallelFailingServices");

    executeAvailableJobs();

    String[] activityIds= {"theServiceTask1", "theServiceTask2"};

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setActivityIdIn(activityIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn"
  })
  void queryByProcessInstanceIdAndActivityId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("FailingProcess");

    executeAvailableJobs();

    String[] processInstanceIds= {processInstance.getId()};
    String[] activityIds= {"ServiceTask_1"};

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setProcessInstanceIdIn(processInstanceIds);
    queryParameter.setActivityIdIn(activityIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn",
    "processes/process-with-two-parallel-failing-services.bpmn"
  })
  void queryByProcessInstanceIdAndActivityIdShouldReturnEmptyList() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("FailingProcess");
    runtimeService.startProcessInstanceByKey("processWithTwoParallelFailingServices");

    executeAvailableJobs();

    String[] processInstanceIds= {processInstance.getId()};
    String[] activityIds= {"theServiceTask1"}; // is an activity id in "processWithTwoParallelFailingServices"

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setProcessInstanceIdIn(processInstanceIds);
    queryParameter.setActivityIdIn(activityIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);

    assertThat(result).isEmpty();
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn",
    "processes/call-activity.bpmn",
    "processes/nested-call-activity.bpmn"
  })
  void queryWithNestedIncidents() {
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("NestedCallActivity");

    executeAvailableJobs();

    ProcessInstance processInstance2 = runtimeService.createProcessInstanceQuery().processDefinitionKey("CallActivity").singleResult();
    ProcessInstance processInstance3 = runtimeService.createProcessInstanceQuery().processDefinitionKey("FailingProcess").singleResult();

    String[] processInstanceIds= {processInstance1.getId()};

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setProcessInstanceIdIn(processInstanceIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    IncidentDto incident = result.get(0);

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentMessage()).isNull();
    assertThat(incident.getIncidentTimestamp()).isNotNull();
    assertThat(incident.getActivityId()).isEqualTo("CallActivity_1");
    assertThat(incident.getFailedActivityId()).isEqualTo("CallActivity_1");
    assertThat(incident.getProcessInstanceId()).isEqualTo(processInstance1.getId());
    assertThat(incident.getProcessDefinitionId()).isEqualTo(processInstance1.getProcessDefinitionId());
    assertThat(incident.getExecutionId()).isNotNull();
    assertThat(incident.getConfiguration()).isNull();

    assertThat(incident.getCauseIncidentId()).isNotEqualTo(incident.getId());
    assertThat(incident.getCauseIncidentProcessInstanceId()).isEqualTo(processInstance2.getId());
    assertThat(incident.getCauseIncidentProcessDefinitionId()).isEqualTo(processInstance2.getProcessDefinitionId());
    assertThat(incident.getCauseIncidentActivityId()).isEqualTo("CallActivity_1");
    assertThat(incident.getCauseIncidentFailedActivityId()).isEqualTo("CallActivity_1");

    assertThat(incident.getRootCauseIncidentId()).isNotEqualTo(incident.getId());
    assertThat(incident.getRootCauseIncidentProcessInstanceId()).isEqualTo(processInstance3.getId());
    assertThat(incident.getRootCauseIncidentProcessDefinitionId()).isEqualTo(processInstance3.getProcessDefinitionId());
    assertThat(incident.getRootCauseIncidentActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getRootCauseIncidentFailedActivityId()).isEqualTo("ServiceTask_1");
    assertThat(incident.getRootCauseIncidentConfiguration()).isNotNull();
    assertThat(incident.getRootCauseIncidentMessage()).isEqualTo("I am failing!");
  }

  @Test
  @Deployment(resources = {
    "processes/process-with-two-parallel-failing-services.bpmn"
  })
  void queryPaginiation() {
    runtimeService.startProcessInstanceByKey("processWithTwoParallelFailingServices");

    executeAvailableJobs();

    IncidentQueryDto queryParameter = new IncidentQueryDto();

    List<IncidentDto> result = resource.queryIncidents(queryParameter, 0, 2);
    assertThat(result)
      .isNotEmpty()
      .hasSize(2);

    result = resource.queryIncidents(queryParameter, 2, 1);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    result = resource.queryIncidents(queryParameter, 4, null);
    assertThat(result)
      .isNotEmpty().hasSize(6);

    result = resource.queryIncidents(queryParameter, null, 4);
    assertThat(result)
      .isNotEmpty()
      .hasSize(4);
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn",
    "processes/call-activity.bpmn",
    "processes/nested-call-activity.bpmn"
  })
  void queryByProcessDefinitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("NestedCallActivity");

    executeAvailableJobs();

    String[] processDefinitionIds = { processInstance.getProcessDefinitionId() };

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setProcessDefinitionIdIn(processDefinitionIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    IncidentDto incident = result.get(0);

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentMessage()).isNull();
    assertThat(incident.getIncidentTimestamp()).isNotNull();
    assertThat(incident.getActivityId()).isEqualTo("CallActivity_1");
    assertThat(incident.getFailedActivityId()).isEqualTo("CallActivity_1");
    assertThat(incident.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(incident.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(incident.getExecutionId()).isNotNull();
    assertThat(incident.getConfiguration()).isNull();
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn",
    "processes/call-activity.bpmn",
    "processes/nested-call-activity.bpmn"
  })
  void queryByProcessDefinitionIds() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("NestedCallActivity");

    executeAvailableJobs();

    String processDefinition2 = repositoryService.createProcessDefinitionQuery().processDefinitionKey("CallActivity").singleResult().getId();

    String[] processDefinitionIds = { processInstance.getProcessDefinitionId(), processDefinition2 };

    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setProcessDefinitionIdIn(processDefinitionIds);

    List<IncidentDto> result = resource.queryIncidents(queryParameter, null, null);
    assertThat(result)
      .isNotEmpty()
      .hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn",
    "processes/call-activity.bpmn",
    "processes/nested-call-activity.bpmn"
  })
  void querySorting() {
    runtimeService.startProcessInstanceByKey("NestedCallActivity");

    executeAvailableJobs();

    // asc
    verifySorting("incidentMessage", "asc", 3);
    verifySorting("incidentTimestamp", "asc", 3);
    verifySorting("incidentType", "asc", 3);
    verifySorting("activityId", "asc", 3);
    verifySorting("causeIncidentProcessInstanceId", "asc", 3);
    verifySorting("rootCauseIncidentProcessInstanceId", "asc", 3);

    // desc
    verifySorting("incidentMessage", "desc", 3);
    verifySorting("incidentTimestamp", "desc", 3);
    verifySorting("incidentType", "desc", 3);
    verifySorting("activityId", "desc", 3);
    verifySorting("causeIncidentProcessInstanceId", "desc", 3);
    verifySorting("rootCauseIncidentProcessInstanceId", "desc", 3);
  }

  @Test
  @Deployment(resources = "processes/simple-user-task-process.bpmn")
  void querySortingByIncidentMessage()
  {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("simpleUserTaskProcess");
    Incident incident1 = runtimeService.createIncident("foo", instance.getId(), null, "message1");
    Incident incident2 = runtimeService.createIncident("foo", instance.getId(), null, "message3");
    Incident incident3 = runtimeService.createIncident("foo", instance.getId(), null, "message2");

    // when
    List<IncidentDto> ascending = queryIncidents("incidentMessage", "asc");
    List<IncidentDto> descending = queryIncidents("incidentMessage", "desc");

    // then
    assertThat(ascending).extracting("id").containsExactly(incident1.getId(), incident3.getId(), incident2.getId());
    assertThat(descending).extracting("id").containsExactly(incident2.getId(), incident3.getId(), incident1.getId());
  }

  @Test
  void shouldReturnPaginatedResult() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);

    identityService.setAuthenticatedUserId("foo");

    assertDoesNotThrow(() -> {
      // when
      resource.queryIncidents(new IncidentQueryDto(), 0, 10);
      // then: no exception expected
    }, "No exception expected");
  }

  @Test
  void shouldReturnUnboundedResult_NotAuthenticated() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);

    assertDoesNotThrow(() -> {
      // when
      resource.queryIncidents(new IncidentQueryDto(), null, null);
      // then: no exception expected
    }, "No exception expected");
  }

  @Test
  void shouldReturnUnboundedResult_NoLimitConfigured() {
    // given
    identityService.setAuthenticatedUserId("foo");
    var incidentQueryDto = new IncidentQueryDto();

    // when + then
    assertDoesNotThrow(() -> resource.queryIncidents(incidentQueryDto, null, null), "No exception expected");
  }

  @Test
  void shouldThrowExceptionWhenMaxResultsLimitExceeded() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);
    var incidentQueryDto = new IncidentQueryDto();

    identityService.setAuthenticatedUserId("foo");

    // when + then
    assertThatThrownBy(() -> resource.queryIncidents(incidentQueryDto, 0, 11))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("Max results limit of 10 exceeded!");
  }

  @Test
  void shouldThrowExceptionWhenQueryUnbounded() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);
    var incidentQueryDto = new IncidentQueryDto();

    identityService.setAuthenticatedUserId("foo");

    // when + then
    assertThatThrownBy(() -> resource.queryIncidents(incidentQueryDto, null, null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("An unbound number of results is forbidden!");
  }

  protected List<IncidentDto> queryIncidents(String sorting, String order)
  {
    IncidentQueryDto queryParameter = new IncidentQueryDto();
    queryParameter.setSortBy(sorting);
    queryParameter.setSortOrder(order);

    return resource.queryIncidents(queryParameter, null, null);
  }

  protected void verifySorting(String sortBy, String sortOrder, int expectedResult) {
    List<IncidentDto> result = queryIncidents(sortBy, sortOrder);
    assertThat(result)
            .isNotEmpty()
            .hasSize(expectedResult);
  }

}
