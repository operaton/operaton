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
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.CalledProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.IncidentStatisticsDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.CalledProcessInstanceQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.base.sub.resources.ProcessInstanceResource;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author roman.smirnov
 */
class ProcessInstanceResourceTest extends AbstractCockpitPluginTest {
  private ProcessInstanceResource resource;

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
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void getCalledProcessInstancesByParentProcessInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess");

    resource = new ProcessInstanceResource(getProcessEngine().getName(), processInstance.getId());

    executeAvailableJobs();

    ProcessDefinition userTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessDefinition anotherUserTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("anotherUserTaskProcess")
        .singleResult();

    CalledProcessInstanceQueryDto queryParameter = new CalledProcessInstanceQueryDto();

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result)
      .isNotEmpty()
      .hasSize(2);

    ProcessDefinition compareWith = null;
    for (ProcessInstanceDto instance : result) {
      CalledProcessInstanceDto dto = (CalledProcessInstanceDto) instance;
      if (dto.getProcessDefinitionId().equals(userTaskProcess.getId())) {
        compareWith = userTaskProcess;
        assertThat(dto.getCallActivityId()).isEqualTo("firstCallActivity");
      } else if (dto.getProcessDefinitionId().equals(anotherUserTaskProcess.getId())) {
        compareWith = anotherUserTaskProcess;
        assertThat(dto.getCallActivityId()).isEqualTo("secondCallActivity");
      } else {
        fail("Unexpected called process instance: " + dto.getId());
      }

      assertThat(dto.getCallActivityInstanceId()).isNotNull();

      assertThat(dto.getProcessDefinitionId()).isEqualTo(compareWith.getId());
      assertThat(dto.getProcessDefinitionName()).isEqualTo(compareWith.getName());
      assertThat(dto.getProcessDefinitionKey()).isEqualTo(compareWith.getKey());
    }
  }

  @Test
  @Deployment(resources = {
      "processes/two-parallel-call-activities-calling-different-process.bpmn",
      "processes/user-task-process.bpmn",
      "processes/another-user-task-process.bpmn"
  })
  void testGetCalledProcessInstancesByParentProcessInstanceIdContainsBusinessKey() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess", "myBusinessKey");

    resource = new ProcessInstanceResource(getProcessEngine().getName(), processInstance.getId());
    executeAvailableJobs();

    CalledProcessInstanceQueryDto queryParameter = new CalledProcessInstanceQueryDto();

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getBusinessKey()).isEqualTo("firstCall:myBusinessKey");
    assertThat(result.get(1).getBusinessKey()).isEqualTo("secondCall:myBusinessKey");
  }


  @Test
  @Deployment(resources = {
    "processes/two-parallel-call-activities-calling-different-process.bpmn",
    "processes/user-task-process.bpmn",
    "processes/another-user-task-process.bpmn"
  })
  void getCalledProcessInstancesByParentProcessInstanceIdAndActivityInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess");

    resource = new ProcessInstanceResource(getProcessEngine().getName(), processInstance.getId());

    ActivityInstance processInstanceActivityInstance = runtimeService.getActivityInstance(processInstance.getId());

    String firstActivityInstanceId = null;
    String secondActivityInstanceId = null;

    for (ActivityInstance child : processInstanceActivityInstance.getChildActivityInstances()) {
      if ("firstCallActivity".equals(child.getActivityId())) {
        firstActivityInstanceId = child.getId();
      } else if ("secondCallActivity".equals(child.getActivityId())) {
        secondActivityInstanceId = child.getId();
      } else {
        fail("Unexpected activity instance with activity id: %s and instance id: %s".formatted(child.getActivityId(), child.getId()));
      }
    }

    executeAvailableJobs();

    CalledProcessInstanceQueryDto queryParameter1 = new CalledProcessInstanceQueryDto();

    String[] activityInstanceIds1 = {firstActivityInstanceId};
    queryParameter1.setActivityInstanceIdIn(activityInstanceIds1);

    List<CalledProcessInstanceDto> result1 = resource.queryCalledProcessInstances(queryParameter1);
    assertThat(result1)
      .isNotEmpty()
      .hasSize(1);

    CalledProcessInstanceQueryDto queryParameter2 = new CalledProcessInstanceQueryDto();
    String[] activityInstanceIds2 = {secondActivityInstanceId};
    queryParameter2.setActivityInstanceIdIn(activityInstanceIds2);

    List<CalledProcessInstanceDto> result2 = resource.queryCalledProcessInstances(queryParameter2);
    assertThat(result2)
      .isNotEmpty()
      .hasSize(1);

    CalledProcessInstanceQueryDto queryParameter3 = new CalledProcessInstanceQueryDto();
    String[] activityInstanceIds3 = {firstActivityInstanceId, secondActivityInstanceId};
    queryParameter3.setActivityInstanceIdIn(activityInstanceIds3);

    List<CalledProcessInstanceDto> result3 = resource.queryCalledProcessInstances(queryParameter3);
    assertThat(result3)
      .isNotEmpty()
      .hasSize(2);
  }

  @Test
  @Deployment(resources = {
    "processes/call-activity.bpmn",
    "processes/failing-process.bpmn"
  })
  void calledProcessIntancesIncidents() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("CallActivity");

    resource = new ProcessInstanceResource(getProcessEngine().getName(), processInstance.getId());

    runtimeService.getActivityInstance(processInstance.getId());

    String processDefinition1 = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("CallActivity")
      .singleResult()
      .getId();

    String processDefinition2 = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("FailingProcess")
      .singleResult()
      .getId();

    executeAvailableJobs();

    CalledProcessInstanceQueryDto queryParameter1 = new CalledProcessInstanceQueryDto();
    queryParameter1.setProcessDefinitionId(processDefinition1);

    List<CalledProcessInstanceDto> callActivityInstances = resource.queryCalledProcessInstances(queryParameter1);
    assertThat(callActivityInstances)
      .isNotEmpty()
      .hasSize(1);

    List<IncidentStatisticsDto> incidents1 = callActivityInstances.get(0).getIncidents();
    assertThat(incidents1)
      .isNotEmpty()
      .hasSize(1);

    assertThat(incidents1.get(0).getIncidentCount()).isEqualTo(1);
    assertThat(incidents1.get(0).getIncidentType()).isEqualTo("failedJob");

    CalledProcessInstanceQueryDto queryParameter2 = new CalledProcessInstanceQueryDto();
    queryParameter2.setProcessDefinitionId(processDefinition2);

    List<CalledProcessInstanceDto> failingProcessInstance = resource.queryCalledProcessInstances(queryParameter2);
    assertThat(failingProcessInstance)
      .isNotEmpty()
      .hasSize(1);

    List<IncidentStatisticsDto> incidents2 = failingProcessInstance.get(0).getIncidents();
    assertThat(incidents2)
      .isNotEmpty()
      .hasSize(1);

    assertThat(incidents2.get(0).getIncidentCount()).isEqualTo(1);
    assertThat(incidents2.get(0).getIncidentType()).isEqualTo("failedJob");
  }

  @Test
  void shouldNotThrowExceptionWhenQueryUnbounded() {
    // given
    resource = new ProcessInstanceResource(getProcessEngine().getName(), "anId");

    processEngineConfiguration.setQueryMaxResultsLimit(10);

    identityService.setAuthenticatedUserId("foo");

    var calledProcessInstanceQueryDto = new CalledProcessInstanceQueryDto();

    // when + then
    assertThatCode(() -> resource.queryCalledProcessInstances(calledProcessInstanceQueryDto))
      .doesNotThrowAnyException();
  }

}
