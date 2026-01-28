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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author roman.smirnov
 */
public class IncidentQueryTest {

  public static final String PROCESS_DEFINITION_KEY = "oneFailingServiceTaskProcess";
  public static final BpmnModelInstance FAILING_SERVICE_TASK_MODEL  = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent("start")
    .serviceTask("task")
      .operatonAsyncBefore()
      .operatonClass(FailingDelegate.class.getName())
    .endEvent("end")
    .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  private List<String> processInstanceIds;

  RuntimeService runtimeService;
  ManagementService managementService;

  /**
   * Setup starts 4 process instances of oneFailingServiceTaskProcess.
   */
  @BeforeEach
  void startProcessInstances() {
    testHelper.deploy(FAILING_SERVICE_TASK_MODEL);

    processInstanceIds = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      Map<String, Object> variables = Collections.singletonMap("message", "exception" + i);

      processInstanceIds.add(engineRule.getRuntimeService()
        .startProcessInstanceByKey(PROCESS_DEFINITION_KEY, i + "", variables).getId()
      );
    }

    testHelper.executeAvailableJobs();
  }

  @Test
  void testIncidentQueryIncidentTimestampAfterBefore() {

    IncidentQuery query = runtimeService.createIncidentQuery();

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    assertThat(query.incidentTimestampBefore(hourAgo.getTime()).count()).isZero();
    assertThat(query.incidentTimestampBefore(hourFromNow.getTime()).count()).isEqualTo(4);
    assertThat(query.incidentTimestampAfter(hourAgo.getTime()).count()).isEqualTo(4);
    assertThat(query.incidentTimestampAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(query.incidentTimestampBefore(hourFromNow.getTime())
        .incidentTimestampAfter(hourAgo.getTime()).count()).isEqualTo(4);
  }

  @Test
  void testQuery() {
    IncidentQuery query = runtimeService.createIncidentQuery();
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(4);
  }

  @Test
  void testQueryByIncidentType() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentType(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(4);
  }

  @Test
  void testQueryByInvalidIncidentType() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentType("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByIncidentMessage() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentMessage("exception0");
    assertThat(query.count()).isOne();

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);
  }

  @Test
  void testQueryByInvalidIncidentMessage() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentMessage("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByIncidentMessageLike() {
    IncidentQuery query = runtimeService.createIncidentQuery();
    assertThat(query.incidentMessageLike("exception").list()).isEmpty();
    assertThat(query.incidentMessageLike("exception%").list()).hasSize(4);
    assertThat(query.incidentMessageLike("%xception1").list()).hasSize(1);
  }

  @Test
  void testQueryByProcessDefinitionId() {
    String processDefinitionId = engineRule.getRepositoryService().createProcessDefinitionQuery().singleResult().getId();

    IncidentQuery query = runtimeService.createIncidentQuery().processDefinitionId(processDefinitionId);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(4);
  }

  @Test
  void testQueryByInvalidProcessDefinitionId() {
    IncidentQuery query = runtimeService.createIncidentQuery().processDefinitionId("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByProcessDefinitionKeys() {
    // given
    // 4 failed instances of "process"

    // one incident of each of the following processes
    testHelper.deploy(Bpmn.createExecutableProcess("proc1").startEvent().userTask().endEvent().done());
    ProcessInstance instance5 = runtimeService.startProcessInstanceByKey("proc1");
    Incident incident5 = runtimeService.createIncident("foo", instance5.getId(), "a");

    testHelper.deploy(Bpmn.createExecutableProcess("proc2").startEvent().userTask().endEvent().done());
    ProcessInstance instance6 = runtimeService.startProcessInstanceByKey("proc2");
    Incident incident6 = runtimeService.createIncident("foo", instance6.getId(), "b");

    // when
    List<Incident> incidents = runtimeService.createIncidentQuery()
        .processDefinitionKeyIn("proc1", "proc2")
        .orderByConfiguration()
        .asc()
        .list();

    // then
    assertThat(incidents).hasSize(2);
    assertThat(incidents.get(0).getId()).isEqualTo(incident5.getId());
    assertThat(incidents.get(1).getId()).isEqualTo(incident6.getId());
  }

  @Test
  void testQueryByInvalidProcessDefinitionKeys() {
    // given
    IncidentQuery incidentQuery = runtimeService.createIncidentQuery();

    // when/then
    assertThatThrownBy(() -> incidentQuery.processDefinitionKeyIn((String[]) null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByOneInvalidProcessDefinitionKey() {
    // given
    IncidentQuery incidentQuery = runtimeService.createIncidentQuery();

    // when/then
    assertThatThrownBy(() -> incidentQuery.processDefinitionKeyIn((String) null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByProcessInstanceId() {
    IncidentQuery query = runtimeService.createIncidentQuery().processInstanceId(processInstanceIds.get(0));

    assertThat(query.count()).isOne();

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);

    Incident incident = query.singleResult();
    assertThat(incident).isNotNull();
  }

  @Test
  void testQueryByInvalidProcessInstanceId() {
    IncidentQuery query = runtimeService.createIncidentQuery().processInstanceId("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByIncidentId() {
    Incident incident= runtimeService.createIncidentQuery().processInstanceId(processInstanceIds.get(0)).singleResult();
    assertThat(incident).isNotNull();

    IncidentQuery query = runtimeService.createIncidentQuery().incidentId(incident.getId());

    assertThat(query.count()).isOne();

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);
  }

  @Test
  void testQueryByInvalidIncidentId() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentId("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByExecutionId() {
    Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceIds.get(0)).singleResult();
    assertThat(execution).isNotNull();

    IncidentQuery query = runtimeService.createIncidentQuery().executionId(execution.getId());

    assertThat(query.count()).isOne();

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);
  }

  @Test
  void testQueryByInvalidExecutionId() {
    IncidentQuery query = runtimeService.createIncidentQuery().executionId("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().activityId("task");
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(4);
  }

  @Test
  void testQueryByInvalidActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().activityId("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByFailedActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().failedActivityId("task");
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(4);
  }

  @Test
  void testQueryByInvalidFailedActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().failedActivityId("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByConfiguration() {
    String jobId = managementService.createJobQuery().processInstanceId(processInstanceIds.get(0)).singleResult().getId();

    IncidentQuery query = runtimeService.createIncidentQuery().configuration(jobId);
    assertThat(query.count()).isOne();

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);
  }

  @Test
  void testQueryByInvalidConfiguration() {
    IncidentQuery query = runtimeService.createIncidentQuery().configuration("invalid");

    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();

    Incident incident = query.singleResult();
    assertThat(incident).isNull();
  }

  @Test
  void testQueryByCauseIncidentIdEqualsNull() {
    IncidentQuery query = runtimeService.createIncidentQuery().causeIncidentId(null);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(4);
  }

  @Test
  void testQueryByInvalidCauseIncidentId() {
    IncidentQuery query = runtimeService.createIncidentQuery().causeIncidentId("invalid");
    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/IncidentQueryTest.testQueryByCauseIncidentId.bpmn"})
  void testQueryByCauseIncidentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callFailingProcess");

    testHelper.executeAvailableJobs();

    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
    assertThat(subProcessInstance).isNotNull();

    Incident causeIncident = runtimeService.createIncidentQuery().processInstanceId(subProcessInstance.getId()).singleResult();
    assertThat(causeIncident).isNotNull();

    IncidentQuery query = runtimeService.createIncidentQuery().causeIncidentId(causeIncident.getId());
    assertThat(query.count()).isEqualTo(2);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(2);
  }

  @Test
  void testQueryByRootCauseIncidentIdEqualsNull() {
    IncidentQuery query = runtimeService.createIncidentQuery().rootCauseIncidentId(null);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(4);
  }

  @Test
  void testQueryByRootInvalidCauseIncidentId() {
    IncidentQuery query = runtimeService.createIncidentQuery().rootCauseIncidentId("invalid");
    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/IncidentQueryTest.testQueryByRootCauseIncidentId.bpmn",
      "org/operaton/bpm/engine/test/api/runtime/IncidentQueryTest.testQueryByCauseIncidentId.bpmn"})
  void testQueryByRootCauseIncidentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callFailingCallActivity");

    testHelper.executeAvailableJobs();

    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
    assertThat(subProcessInstance).isNotNull();

    ProcessInstance failingSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(subProcessInstance.getId()).singleResult();
    assertThat(subProcessInstance).isNotNull();

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(failingSubProcessInstance.getId()).singleResult();
    assertThat(incident).isNotNull();

    IncidentQuery query = runtimeService.createIncidentQuery().rootCauseIncidentId(incident.getId());
    assertThat(query.count()).isEqualTo(3);

    List<Incident> incidents = query.list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(3);

    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

  }

  @Test
  void testQueryByJobDefinitionId() {
    String processDefinitionId1 = testHelper.deployAndGetDefinition(FAILING_SERVICE_TASK_MODEL).getId();
    String processDefinitionId2 = testHelper.deployAndGetDefinition(FAILING_SERVICE_TASK_MODEL).getId();

    runtimeService.startProcessInstanceById(processDefinitionId1);
    runtimeService.startProcessInstanceById(processDefinitionId2);
    testHelper.executeAvailableJobs();

    String jobDefinitionId1 = managementService.createJobQuery()
      .processDefinitionId(processDefinitionId1)
      .singleResult().getJobDefinitionId();
    String jobDefinitionId2 = managementService.createJobQuery()
      .processDefinitionId(processDefinitionId2)
      .singleResult().getJobDefinitionId();

    IncidentQuery query = runtimeService.createIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId1, jobDefinitionId2);

    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);

    query = runtimeService.createIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId1);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();

    query = runtimeService.createIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId2);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByUnknownJobDefinitionId() {
    IncidentQuery query = runtimeService.createIncidentQuery().jobDefinitionIdIn("unknown");
    assertThat(query.count()).isZero();

    List<Incident> incidents = query.list();
    assertThat(incidents).isEmpty();
  }

  @Test
  void testQueryByNullJobDefinitionId() {
    var incidentQuery = runtimeService.createIncidentQuery().jobDefinitionIdIn((String) null);

    assertThatThrownBy(incidentQuery::list)
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("jobDefinitionIds contains null value");
  }

  @Test
  void testQueryByNullJobDefinitionIds() {
    var incidentQuery = runtimeService.createIncidentQuery().jobDefinitionIdIn((String[]) null);

    assertThatThrownBy(incidentQuery::list)
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("jobDefinitionIds is null");
  }

  @Test
  void testQueryPaging() {
    assertThat(runtimeService.createIncidentQuery().listPage(0, 4)).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().listPage(2, 1)).hasSize(1);
    assertThat(runtimeService.createIncidentQuery().listPage(1, 2)).hasSize(2);
    assertThat(runtimeService.createIncidentQuery().listPage(1, 4)).hasSize(3);
  }

  @Test
  void testQuerySorting() {
    assertThat(runtimeService.createIncidentQuery().orderByIncidentId().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentTimestamp().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentType().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByExecutionId().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByActivityId().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessInstanceId().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessDefinitionId().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByCauseIncidentId().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByRootCauseIncidentId().asc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByConfiguration().asc().list()).hasSize(4);

    assertThat(runtimeService.createIncidentQuery().orderByIncidentId().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentTimestamp().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentType().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByExecutionId().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByActivityId().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessInstanceId().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessDefinitionId().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByCauseIncidentId().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByRootCauseIncidentId().desc().list()).hasSize(4);
    assertThat(runtimeService.createIncidentQuery().orderByConfiguration().desc().list()).hasSize(4);

  }

  @Test
  void testQuerySortingByIncidentMessage()
  {
    // given

    // when
    List<Incident> ascending = runtimeService.createIncidentQuery().orderByIncidentMessage().asc().list();
    List<Incident> descending = runtimeService.createIncidentQuery().orderByIncidentMessage().desc().list();

    // then
    assertThat(ascending).extracting("incidentMessage")
      .containsExactly("exception0", "exception1", "exception2", "exception3");
    assertThat(descending).extracting("incidentMessage")
      .containsExactly("exception3", "exception2", "exception1", "exception0");
  }
}
