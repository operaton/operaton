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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.IncidentStatisticsDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessInstanceQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.EQUALS_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.GREATER_THAN_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.GREATER_THAN_OR_EQUALS_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.LESS_THAN_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.LESS_THAN_OR_EQUALS_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.LIKE_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.NOT_EQUALS_OPERATOR_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author roman.smirnov
 * @author nico.rehwaldt
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class ProcessInstanceRestServiceTest extends AbstractCockpitPluginTest {
  private ProcessInstanceRestService resource;

  @BeforeEach
  void setUp() {
    resource = new ProcessInstanceRestService(processEngine.getName());
  }

  @AfterEach
  void clearAuthentication() {
    identityService.clearAuthentication();
  }

  @AfterEach
  void resetQueryMaxResultsLimit() {
    processEngineConfiguration.setQueryMaxResultsLimit(Integer.MAX_VALUE);
  }

  private void startProcessInstances(String processDefinitionKey, int numOfInstances) {
    for (int i = 0; i < numOfInstances; i++) {
      ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 1000));
      runtimeService.startProcessInstanceByKey(processDefinitionKey, "businessKey_" + i);
    }

    executeAvailableJobs();
  }

  private void startProcessInstancesDelayed(String processDefinitionKey, int numOfInstances) {
    for (int i = 0; i < numOfInstances; i++) {
      ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 3000));
      runtimeService.startProcessInstanceByKey(processDefinitionKey, "businessKey_" + i);
    }

    executeAvailableJobs();
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void query() {
    startProcessInstances("userTaskProcess", 3);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(3);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryCount() {
    startProcessInstances("userTaskProcess", 3);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(3);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryOrderByStartTime() {
    startProcessInstancesDelayed("userTaskProcess", 3);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);
    queryParameter.setSortBy("startTime");

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(3);

    for (int i=1; i < result.size(); i++) {
      Date previousStartTime = result.get(i - 1).getStartTime();
      Date startTime = result.get(i).getStartTime();
      assertThat(startTime.after(previousStartTime)).isTrue();
    }
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryOrderByStartTimeAsc() {
    startProcessInstancesDelayed("userTaskProcess", 3);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);
    queryParameter.setSortBy("startTime");
    queryParameter.setSortOrder("asc");

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(3);

    for (int i=1; i < result.size(); i++) {
      Date previousStartTime = result.get(i - 1).getStartTime();
      Date startTime = result.get(i).getStartTime();
      assertThat(startTime.after(previousStartTime)).isTrue();
    }
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryOrderByStartTimeDesc() {
    startProcessInstancesDelayed("userTaskProcess", 3);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);
    queryParameter.setSortBy("startTime");
    queryParameter.setSortOrder("desc");

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(3);

    for (int i=1; i < result.size(); i++) {
      Date previousStartTime = result.get(i - 1).getStartTime();
      Date startTime = result.get(i).getStartTime();
      assertThat(startTime.before(previousStartTime)).isTrue();
    }
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryPagination() {
    startProcessInstances("userTaskProcess", 5);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, 0, 3);
    assertThat(result).isNotEmpty().hasSize(3);

    result = resource.queryProcessInstances(queryParameter, 2, 3);
    assertThat(result).isNotEmpty().hasSize(3);

    result = resource.queryProcessInstances(queryParameter, 3, 1);
    assertThat(result).isNotEmpty().hasSize(1);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryPaginationWithOrderByStartTimeDesc() {
    startProcessInstancesDelayed("userTaskProcess", 8);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);
    queryParameter.setSortBy("startTime");
    queryParameter.setSortOrder("desc");

    List<ProcessInstanceDto> allResults = new ArrayList<>();

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, 0, 3);
    assertThat(result).isNotEmpty().hasSize(3);
    allResults.addAll(result);

    result = resource.queryProcessInstances(queryParameter, 3, 3);
    assertThat(result).isNotEmpty().hasSize(3);
    allResults.addAll(result);

    result = resource.queryProcessInstances(queryParameter, 6, 3);
    assertThat(result).isNotEmpty().hasSize(2);
    allResults.addAll(result);

    for (int i=1; i < allResults.size(); i++) {
      Date previousStartTime = allResults.get(i - 1).getStartTime();
      Date startTime = allResults.get(i).getStartTime();
      assertThat(startTime.before(previousStartTime)).isTrue();
    }

  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryPaginationWithOrderByStartTimeAsc() {
    startProcessInstancesDelayed("userTaskProcess", 8);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);
    queryParameter.setSortBy("startTime");
    queryParameter.setSortOrder("asc");

    List<ProcessInstanceDto> allResults = new ArrayList<>();

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, 0, 3);
    assertThat(result).isNotEmpty().hasSize(3);
    allResults.addAll(result);

    result = resource.queryProcessInstances(queryParameter, 3, 3);
    assertThat(result).isNotEmpty().hasSize(3);
    allResults.addAll(result);

    result = resource.queryProcessInstances(queryParameter, 6, 3);
    assertThat(result).isNotEmpty().hasSize(2);
    allResults.addAll(result);

    for (int i=1; i < allResults.size(); i++) {
      Date previousStartTime = allResults.get(i - 1).getStartTime();
      Date startTime = allResults.get(i).getStartTime();
      assertThat(startTime.after(previousStartTime)).isTrue();
    }

  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryWithoutAnyIncident() {
    startProcessInstances("userTaskProcess", 1);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);

    assertThat(result).isNotEmpty().hasSize(1);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();

    assertThat(incidents).isEmpty();
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn"
  })
  void queryWithContainingIncidents() {
    startProcessInstances("FailingProcess", 1);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);

    assertThat(result).isNotEmpty().hasSize(1);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();

    assertThat(incidents).isNotEmpty().hasSize(1);

    IncidentStatisticsDto incident = incidents.get(0);

    assertThat(incident.getIncidentType()).isEqualTo("failedJob");
    assertThat(incident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {
    "processes/process-with-two-parallel-failing-services.bpmn"
  })
  void queryWithMoreThanOneIncident() {
    startProcessInstances("processWithTwoParallelFailingServices", 1);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDefinitionId);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);

    assertThat(result).isNotEmpty().hasSize(1);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();

    assertThat(incidents).isNotEmpty().hasSize(3);

    for (IncidentStatisticsDto incident : incidents) {
      String incidentType = incident.getIncidentType();
      assertThat(incidentType).isNotNull();

      if ("failedJob".equals(incidentType)) {
        assertThat(incident.getIncidentCount()).isEqualTo(2);
      } else if ("anIncident".equals(incidentType)) {
        assertThat(incident.getIncidentCount()).isEqualTo(3);
      } else if ("anotherIncident".equals(incidentType)) {
        assertThat(incident.getIncidentCount()).isEqualTo(5);
      } else {
        fail(incidentType + " not expected.");
      }

    }
  }

  @Test
  @Deployment(resources = {
    "processes/variables-process.bpmn"
  })
  void queryWithBooleanVariable() {
    // given
    startProcessInstances("variableProcess", 2);

    // when
    VariableQueryParameterDto variable = createVariableParameter("varboolean", EQUALS_OPERATOR_NAME, false);

    ProcessInstanceQueryDto parameter = new ProcessInstanceQueryDto();
    parameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> results = resource.queryProcessInstances(parameter, 0, Integer.MAX_VALUE);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @Deployment(resources = {
    "processes/variables-process.bpmn"
  })
  void queryWithStringVariable() {
    // given
    startProcessInstances("variableProcess", 2);

    // when
    VariableQueryParameterDto variable = createVariableParameter("varstring", LIKE_OPERATOR_NAME, "B%");

    ProcessInstanceQueryDto parameter = new ProcessInstanceQueryDto();
    parameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> results = resource.queryProcessInstances(parameter, 0, Integer.MAX_VALUE);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @Deployment(resources = {
    "processes/variables-process.bpmn"
  })
  void queryWithFloatVariable() {
    // given
    startProcessInstances("variableProcess", 2);

    // when
    VariableQueryParameterDto variable = createVariableParameter("varfloat", EQUALS_OPERATOR_NAME, 0.0);

    ProcessInstanceQueryDto parameter = new ProcessInstanceQueryDto();
    parameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> results = resource.queryProcessInstances(parameter, 0, Integer.MAX_VALUE);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @Deployment(resources = {
    "processes/variables-process.bpmn"
  })
  void queryWithIntegerVariable() {
    // given
    startProcessInstances("variableProcess", 2);

    // when
    VariableQueryParameterDto variable = createVariableParameter("varinteger", NOT_EQUALS_OPERATOR_NAME, 12);

    ProcessInstanceQueryDto parameter = new ProcessInstanceQueryDto();
    parameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(parameter, 0, Integer.MAX_VALUE);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @Deployment(resources = {
    "processes/variables-process.bpmn"
  })
  @RequiredDatabase(excludes = {DbSqlSessionFactory.MYSQL, DbSqlSessionFactory.MARIADB})
  void queryWithComplexVariableFilter() {
    // given
    startProcessInstances("variableProcess", 2);

    // when
    ProcessInstanceQueryDto parameter = new ProcessInstanceQueryDto();

    parameter.setVariables(List.of(
        createVariableParameter("varinteger", VariableQueryParameterDto.GREATER_THAN_OPERATOR_NAME, 11),
        createVariableParameter("varinteger", VariableQueryParameterDto.LESS_THAN_OR_EQUALS_OPERATOR_NAME, 12),
        createVariableParameter("varinteger", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, 12),
        createVariableParameter("varboolean", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, true),
        createVariableParameter("varboolean", VariableQueryParameterDto.NOT_EQUALS_OPERATOR_NAME, false),
        createVariableParameter("varstring", VariableQueryParameterDto.LIKE_OPERATOR_NAME, "F%"),
        createVariableParameter("varstring", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, "FOO"),
        createVariableParameter("varstring", VariableQueryParameterDto.NOT_EQUALS_OPERATOR_NAME, "BAR"),
        createVariableParameter("varstring2", VariableQueryParameterDto.LIKE_OPERATOR_NAME, "F\\_%"),
        createVariableParameter("varfloat", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, 12.12),
        createVariableParameter("varfloat", VariableQueryParameterDto.NOT_EQUALS_OPERATOR_NAME, 13.0),
        createVariableParameter("varfloat", VariableQueryParameterDto.LESS_THAN_OR_EQUALS_OPERATOR_NAME, 12.13)));

    List<ProcessInstanceDto> booleanProcessInstances = resource.queryProcessInstances(parameter, 0, Integer.MAX_VALUE);
    assertThat(booleanProcessInstances).hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/call-activity.bpmn",
    "processes/nested-call-activity.bpmn",
    "processes/failing-process.bpmn"
  })
  void nestedIncidents() {
    startProcessInstances("NestedCallActivity", 1);

    String nestedCallActivityId = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("NestedCallActivity")
        .singleResult()
        .getId();

    String callActivityId = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("CallActivity")
        .singleResult()
        .getId();

    String failingProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("FailingProcess")
        .singleResult()
        .getId();

    ProcessInstanceQueryDto queryParameter1 = new ProcessInstanceQueryDto();
    queryParameter1.setProcessDefinitionId(nestedCallActivityId);

    List<ProcessInstanceDto> nestedCallActivityInstances = resource.queryProcessInstances(queryParameter1, null, null);
    assertThat(nestedCallActivityInstances).isNotEmpty().hasSize(1);

    List<IncidentStatisticsDto> nestedCallActivityIncidents = nestedCallActivityInstances.get(0).getIncidents();
    assertThat(nestedCallActivityIncidents).isNotEmpty().hasSize(1);

    IncidentStatisticsDto nestedCallActivityIncident = nestedCallActivityIncidents.get(0);
    assertThat(nestedCallActivityIncident.getIncidentType()).isEqualTo("failedJob");
    assertThat(nestedCallActivityIncident.getIncidentCount()).isEqualTo(1);

    ProcessInstanceQueryDto queryParameter2 = new ProcessInstanceQueryDto();
    queryParameter2.setProcessDefinitionId(callActivityId);

    List<ProcessInstanceDto> callActivityInstances = resource.queryProcessInstances(queryParameter2, null, null);
    assertThat(callActivityInstances).isNotEmpty().hasSize(1);

    List<IncidentStatisticsDto> callActivityIncidents = callActivityInstances.get(0).getIncidents();
    assertThat(callActivityIncidents).isNotEmpty().hasSize(1);

    IncidentStatisticsDto callActivityIncident = callActivityIncidents.get(0);
    assertThat(callActivityIncident.getIncidentType()).isEqualTo("failedJob");
    assertThat(callActivityIncident.getIncidentCount()).isEqualTo(1);

    ProcessInstanceQueryDto queryParameter3 = new ProcessInstanceQueryDto();
    queryParameter3.setProcessDefinitionId(failingProcess);

    List<ProcessInstanceDto> failingProcessInstances = resource.queryProcessInstances(queryParameter3, null, null);
    assertThat(failingProcessInstances).isNotEmpty().hasSize(1);

    List<IncidentStatisticsDto> failingProcessIncidents = failingProcessInstances.get(0).getIncidents();
    assertThat(failingProcessIncidents).isNotEmpty().hasSize(1);

    IncidentStatisticsDto failingProcessIncident = failingProcessIncidents.get(0);
    assertThat(failingProcessIncident.getIncidentType()).isEqualTo("failedJob");
    assertThat(failingProcessIncident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryByBusinessKey() {
    startProcessInstances("userTaskProcess", 3);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setBusinessKey("businessKey_2");

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryByBusinessKeyCount() {
    startProcessInstances("userTaskProcess", 3);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setBusinessKey("businessKey_2");

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isOne();
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn",
    "processes/failing-process.bpmn"
  })
  void queryByBusinessKeyWithMoreThanOneProcess() {
    startProcessInstances("userTaskProcess", 3);
    startProcessInstances("FailingProcess", 3);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setBusinessKey("businessKey_2");

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn",
    "processes/failing-process.bpmn"
  })
  void queryByBusinessKeyWithMoreThanOneProcessCount() {
    startProcessInstances("userTaskProcess", 3);
    startProcessInstances("FailingProcess", 3);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setBusinessKey("businessKey_2");

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryByBusinessKeyAndProcessDefinition() {
    startProcessInstances("userTaskProcess", 3);

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery().singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setBusinessKey("businessKey_2");
    queryParameter.setProcessDefinitionId(userTaskProcess.getId());

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryByBusinessKeyAndProcessDefinitionCount() {
    startProcessInstances("userTaskProcess", 3);

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery().singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setBusinessKey("businessKey_2");
    queryParameter.setProcessDefinitionId(userTaskProcess.getId());

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isOne();
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByActivityId() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    String[] activityIds = {"firstCallActivity"};
    queryParameter.setActivityIdIn(activityIds);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByActivityIdCount() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    String[] activityIds = {"firstCallActivity"};
    queryParameter.setActivityIdIn(activityIds);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByActivityIds() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    String[] activityIds = {"firstCallActivity", "secondCallActivity"};
    queryParameter.setActivityIdIn(activityIds);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByActivityIdsCount() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    String[] activityIds = {"firstCallActivity", "secondCallActivity"};
    queryParameter.setActivityIdIn(activityIds);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByActivityIdAndProcessDefinitionId() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition processDef = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDef.getId());
    String[] activityIds = {"firstCallActivity"};
    queryParameter.setActivityIdIn(activityIds);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByActivityIdAndProcessDefinitionIdCount() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition processDef = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setProcessDefinitionId(processDef.getId());
    String[] activityIds = {"firstCallActivity"};
    queryParameter.setActivityIdIn(activityIds);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByParentProcessDefinitionId() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition twoCallActivitiesProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setParentProcessDefinitionId(twoCallActivitiesProcess.getId());

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(4);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByParentProcessDefinitionIdCount() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition twoCallActivitiesProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setParentProcessDefinitionId(twoCallActivitiesProcess.getId());

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(4);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByParentProcessDefinitionIdAndProcessDefinitionId() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition twoCallActivitiesProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessDefinition userTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setParentProcessDefinitionId(twoCallActivitiesProcess.getId());

    queryParameter.setProcessDefinitionId(userTaskProcess.getId());

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByParentProcessDefinitionIdAndProcessDefinitionIdCount() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition twoCallActivitiesProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessDefinition userTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setParentProcessDefinitionId(twoCallActivitiesProcess.getId());
    queryParameter.setProcessDefinitionId(userTaskProcess.getId());

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByParentProcessDefinitionIdAndProcessDefinitionIdAndActivityId() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition twoCallActivitiesProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessDefinition userTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setParentProcessDefinitionId(twoCallActivitiesProcess.getId());
    queryParameter.setProcessDefinitionId(userTaskProcess.getId());
    String[] activityIds = {"theUserTask"};
    queryParameter.setActivityIdIn(activityIds);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void queryByParentProcessDefinitionIdAndProcessDefinitionIdAndActivityIdCount() {
    startProcessInstances("TwoParallelCallActivitiesCallingDifferentProcess", 2);

    ProcessDefinition twoCallActivitiesProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    ProcessDefinition userTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setParentProcessDefinitionId(twoCallActivitiesProcess.getId());
    queryParameter.setProcessDefinitionId(userTaskProcess.getId());
    String[] activityIds = {"theUserTask"};
    queryParameter.setActivityIdIn(activityIds);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithIntegerVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithIntegerVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithIntegerVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithIntegerVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithIntegerVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithIntegerVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithLongVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithLongVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithLongVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithLongVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithLongVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithLongVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (long) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithShortVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithShortVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithShortVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithShortVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithShortVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithShortVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (short) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithDoubleVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5.0);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithDoubleVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithDoubleVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithDoubleVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithDoubleVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5.1);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterShortVariableWithDoubleVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (short) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 5.1);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithIntegerVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithIntegerVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithIntegerVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithIntegerVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithIntegerVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithIntegerVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithLongVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithLongVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithLongVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithLongVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithLongVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithLongVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (long) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithShortVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithShortVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithShortVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithShortVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithShortVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithShortVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (short) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithDoubleVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5.0);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithDoubleVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithDoubleVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithDoubleVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithDoubleVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5.1);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterIntegerVariableWithDoubleVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 5.1);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithIntegerVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithIntegerVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithIntegerVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithIntegerVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithIntegerVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithIntegerVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithLongVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithLongVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithLongVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithLongVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithLongVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithLongVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (long) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithShortVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithShortVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithShortVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithShortVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithShortVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithShortVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (short) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithDoubleVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5.0);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithDoubleVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithDoubleVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithDoubleVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithDoubleVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5.1);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Disabled("FIXME")
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterLongVariableWithDoubleVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 5.1);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithIntegerVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithIntegerVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithIntegerVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithIntegerVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithIntegerVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithIntegerVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithLongVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithLongVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithLongVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithLongVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (long) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithLongVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (long) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithLongVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (long) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithShortVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithShortVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithShortVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", (long) 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithShortVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, (short) 4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithShortVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, (short) 5);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithShortVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, (short) 6);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithDoubleVariableEq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.0);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", EQUALS_OPERATOR_NAME, 5.0);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithDoubleVariableNeq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", NOT_EQUALS_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithDoubleVariableGteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OR_EQUALS_OPERATOR_NAME, 5.3);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithDoubleVariableGt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", GREATER_THAN_OPERATOR_NAME, 4.9);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithDoubleVariableLteq() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.1);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OR_EQUALS_OPERATOR_NAME, 5.1);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryAfterDoubleVariableWithDoubleVariableLt() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 5.3);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess", vars);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    VariableQueryParameterDto variable = createVariableParameter("var", LESS_THAN_OPERATOR_NAME, 5.4);
    queryParameter.setVariables(List.of(variable));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(1);

    ProcessInstanceDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryByStartedAfter() {
    String date = "2014-01-01T13:13:00";
    Date currentDate = DateTimeUtil.parseDateTime(date).toDate();

    ClockUtil.setCurrentTime(currentDate);

    startProcessInstances("userTaskProcess", 5);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setStartedAfter(currentDate);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(5);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryByStartedBefore() {
    String date = "2014-01-01T13:13:00";
    Date currentDate = DateTimeUtil.parseDateTime(date).toDate();

    ClockUtil.setCurrentTime(currentDate);

    startProcessInstances("userTaskProcess", 5);

    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setStartedBefore(hourFromNow.getTime());

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(5);
  }

  @Test
  @Deployment(resources = {
    "processes/user-task-process.bpmn"
  })
  void queryByStartedBetween() {
    String date = "2014-01-01T13:13:00";
    Date currentDate = DateTimeUtil.parseDateTime(date).toDate();

    ClockUtil.setCurrentTime(currentDate);

    startProcessInstances("userTaskProcess", 5);

    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setStartedAfter(currentDate);
    queryParameter.setStartedBefore(hourFromNow.getTime());

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty().hasSize(5);
  }

  @Test
  void shouldReturnPaginatedResult() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);

    identityService.setAuthenticatedUserId("foo");

    //when + then
    assertThatCode(() -> resource.queryProcessInstances(new ProcessInstanceQueryDto(), 0, 10))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldReturnUnboundedResult_NotAuthenticated() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);

    //when + then
    assertThatCode(() -> resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldReturnUnboundedResult_NoLimitConfigured() {
    // given
    identityService.setAuthenticatedUserId("foo");

    //when + then
    assertThatCode(() -> resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldThrowExceptionWhenMaxResultsLimitExceeded() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);

    identityService.setAuthenticatedUserId("foo");
    var processInstanceQueryDto = new ProcessInstanceQueryDto();

    // when
    assertThatThrownBy(() -> resource.queryProcessInstances(processInstanceQueryDto, 0, 11))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("Max results limit of 10 exceeded!");
  }

  @Test
  void shouldThrowExceptionWhenQueryUnbounded() {
    // given
    processEngineConfiguration.setQueryMaxResultsLimit(10);

    identityService.setAuthenticatedUserId("foo");

    var processInstanceQueryDto = new ProcessInstanceQueryDto();

    // when
    assertThatThrownBy(() -> resource.queryProcessInstances(processInstanceQueryDto, null, null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("An unbound number of results is forbidden!");
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn",
    "processes/user-task-process.bpmn"
  })
  void shouldFilterWithIncident() {
    startProcessInstances("FailingProcess", 1);
    startProcessInstances("userTaskProcess", 1);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setWithIncident(true);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);

    assertThat(result).hasSize(1);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();

    assertThat(incidents).isNotEmpty().hasSize(1);

    IncidentStatisticsDto incident = incidents.get(0);

    assertThat(incident.getIncidentType()).isEqualTo("failedJob");
    assertThat(incident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {
    "processes/failing-process.bpmn",
    "processes/user-task-process.bpmn"
  })
  void shouldFilterWithIncidentOnCount() {
    startProcessInstances("FailingProcess", 1);
    startProcessInstances("userTaskProcess", 1);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setWithIncident(true);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);

    assertThat(result.getCount()).isOne();
  }

  private VariableQueryParameterDto createVariableParameter(String name, String operator, Object value) {
    VariableQueryParameterDto variable = new VariableQueryParameterDto();
    variable.setName(name);
    variable.setOperator(operator);
    variable.setValue(value);

    return variable;
  }

}
