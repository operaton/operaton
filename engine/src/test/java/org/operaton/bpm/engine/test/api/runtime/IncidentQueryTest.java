/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author roman.smirnov
 */
public class IncidentQueryTest {

  public static String PROCESS_DEFINITION_KEY = "oneFailingServiceTaskProcess";
  public static BpmnModelInstance FAILING_SERVICE_TASK_MODEL  = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent("start")
    .serviceTask("task")
      .operatonAsyncBefore()
      .operatonClass(FailingDelegate.class.getName())
    .endEvent("end")
    .done();

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain chain = RuleChain.outerRule(engineRule).around(testHelper);

  private List<String> processInstanceIds;

  protected RuntimeService runtimeService;
  protected ManagementService managementService;

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
  }

  /**
   * Setup starts 4 process instances of oneFailingServiceTaskProcess.
   */
  @Before
  public void startProcessInstances() {
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
  public void testIncidentQueryIncidentTimestampAfterBefore() {

    IncidentQuery query = runtimeService.createIncidentQuery();

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    assertThat(query.incidentTimestampBefore(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(query.incidentTimestampBefore(hourFromNow.getTime()).count()).isEqualTo(4);
    assertThat(query.incidentTimestampAfter(hourAgo.getTime()).count()).isEqualTo(4);
    assertThat(query.incidentTimestampAfter(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(query.incidentTimestampBefore(hourFromNow.getTime())
        .incidentTimestampAfter(hourAgo.getTime()).count()).isEqualTo(4);
  }

  @Test
  public void testQuery() {
    IncidentQuery query = runtimeService.createIncidentQuery();
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(4);
  }

  @Test
  public void testQueryByIncidentType() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentType(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(4);
  }

  @Test
  public void testQueryByInvalidIncidentType() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentType("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByIncidentMessage() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentMessage("exception0");
    assertThat(query.count()).isEqualTo(1);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidIncidentMessage() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentMessage("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByIncidentMessageLike() {
    IncidentQuery query = runtimeService.createIncidentQuery();
    assertThat(query.incidentMessageLike("exception").list().size()).isEqualTo(0);
    assertThat(query.incidentMessageLike("exception%").list().size()).isEqualTo(4);
    assertThat(query.incidentMessageLike("%xception1").list().size()).isEqualTo(1);
  }

  @Test
  public void testQueryByProcessDefinitionId() {
    String processDefinitionId = engineRule.getRepositoryService().createProcessDefinitionQuery().singleResult().getId();

    IncidentQuery query = runtimeService.createIncidentQuery().processDefinitionId(processDefinitionId);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(4);
  }

  @Test
  public void testQueryByInvalidProcessDefinitionId() {
    IncidentQuery query = runtimeService.createIncidentQuery().processDefinitionId("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByProcessDefinitionKeys() {
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
  public void testQueryByInvalidProcessDefinitionKeys() {
    // given
    IncidentQuery incidentQuery = runtimeService.createIncidentQuery();

    // when/then
    assertThatThrownBy(() -> incidentQuery.processDefinitionKeyIn((String[]) null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  public void testQueryByOneInvalidProcessDefinitionKey() {
    // given
    IncidentQuery incidentQuery = runtimeService.createIncidentQuery();

    // when/then
    assertThatThrownBy(() -> incidentQuery.processDefinitionKeyIn((String) null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  public void testQueryByProcessInstanceId() {
    IncidentQuery query = runtimeService.createIncidentQuery().processInstanceId(processInstanceIds.get(0));

    assertThat(query.count()).isEqualTo(1);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(1);

    Incident incident = query.singleResult();
    assertNotNull(incident);
  }

  @Test
  public void testQueryByInvalidProcessInstanceId() {
    IncidentQuery query = runtimeService.createIncidentQuery().processInstanceId("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByIncidentId() {
    Incident incident= runtimeService.createIncidentQuery().processInstanceId(processInstanceIds.get(0)).singleResult();
    assertNotNull(incident);

    IncidentQuery query = runtimeService.createIncidentQuery().incidentId(incident.getId());

    assertThat(query.count()).isEqualTo(1);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidIncidentId() {
    IncidentQuery query = runtimeService.createIncidentQuery().incidentId("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByExecutionId() {
    Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceIds.get(0)).singleResult();
    assertNotNull(execution);

    IncidentQuery query = runtimeService.createIncidentQuery().executionId(execution.getId());

    assertThat(query.count()).isEqualTo(1);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidExecutionId() {
    IncidentQuery query = runtimeService.createIncidentQuery().executionId("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().activityId("task");
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(4);
  }

  @Test
  public void testQueryByInvalidActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().activityId("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByFailedActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().failedActivityId("task");
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(4);
  }

  @Test
  public void testQueryByInvalidFailedActivityId() {
    IncidentQuery query = runtimeService.createIncidentQuery().failedActivityId("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByConfiguration() {
    String jobId = managementService.createJobQuery().processInstanceId(processInstanceIds.get(0)).singleResult().getId();

    IncidentQuery query = runtimeService.createIncidentQuery().configuration(jobId);
    assertThat(query.count()).isEqualTo(1);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidConfiguration() {
    IncidentQuery query = runtimeService.createIncidentQuery().configuration("invalid");

    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());

    Incident incident = query.singleResult();
    assertNull(incident);
  }

  @Test
  public void testQueryByCauseIncidentIdEqualsNull() {
    IncidentQuery query = runtimeService.createIncidentQuery().causeIncidentId(null);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(4);
  }

  @Test
  public void testQueryByInvalidCauseIncidentId() {
    IncidentQuery query = runtimeService.createIncidentQuery().causeIncidentId("invalid");
    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(0);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/IncidentQueryTest.testQueryByCauseIncidentId.bpmn"})
  public void testQueryByCauseIncidentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callFailingProcess");

    testHelper.executeAvailableJobs();

    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
    assertNotNull(subProcessInstance);

    Incident causeIncident = runtimeService.createIncidentQuery().processInstanceId(subProcessInstance.getId()).singleResult();
    assertNotNull(causeIncident);

    IncidentQuery query = runtimeService.createIncidentQuery().causeIncidentId(causeIncident.getId());
    assertThat(query.count()).isEqualTo(2);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(2);
  }

  @Test
  public void testQueryByRootCauseIncidentIdEqualsNull() {
    IncidentQuery query = runtimeService.createIncidentQuery().rootCauseIncidentId(null);
    assertThat(query.count()).isEqualTo(4);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(4);
  }

  @Test
  public void testQueryByRootInvalidCauseIncidentId() {
    IncidentQuery query = runtimeService.createIncidentQuery().rootCauseIncidentId("invalid");
    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertTrue(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(0);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/IncidentQueryTest.testQueryByRootCauseIncidentId.bpmn",
      "org/operaton/bpm/engine/test/api/runtime/IncidentQueryTest.testQueryByCauseIncidentId.bpmn"})
  public void testQueryByRootCauseIncidentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callFailingCallActivity");

    testHelper.executeAvailableJobs();

    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
    assertNotNull(subProcessInstance);

    ProcessInstance failingSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(subProcessInstance.getId()).singleResult();
    assertNotNull(subProcessInstance);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(failingSubProcessInstance.getId()).singleResult();
    assertNotNull(incident);

    IncidentQuery query = runtimeService.createIncidentQuery().rootCauseIncidentId(incident.getId());
    assertThat(query.count()).isEqualTo(3);

    List<Incident> incidents = query.list();
    assertFalse(incidents.isEmpty());
    assertThat(incidents.size()).isEqualTo(3);

    try {
      query.singleResult();
      fail();
    } catch (ProcessEngineException e) {
      // Exception is expected
    }

  }

  @Test
  public void testQueryByJobDefinitionId() {
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

    assertThat(query.list().size()).isEqualTo(2);
    assertThat(query.count()).isEqualTo(2);

    query = runtimeService.createIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId1);

    assertThat(query.list().size()).isEqualTo(1);
    assertThat(query.count()).isEqualTo(1);

    query = runtimeService.createIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId2);

    assertThat(query.list().size()).isEqualTo(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByUnknownJobDefinitionId() {
    IncidentQuery query = runtimeService.createIncidentQuery().jobDefinitionIdIn("unknown");
    assertThat(query.count()).isEqualTo(0);

    List<Incident> incidents = query.list();
    assertThat(incidents.size()).isEqualTo(0);
  }

  @Test
  public void testQueryByNullJobDefinitionId() {
    var incidentQuery = runtimeService.createIncidentQuery();
    try {
      incidentQuery.jobDefinitionIdIn((String) null);
      fail("Should fail");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("jobDefinitionIds contains null value");
    }
  }

  @Test
  public void testQueryByNullJobDefinitionIds() {
    var incidentQuery = runtimeService.createIncidentQuery();
    try {
      incidentQuery.jobDefinitionIdIn((String[]) null);
      fail("Should fail");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("jobDefinitionIds is null");
    }
  }

  @Test
  public void testQueryPaging() {
    assertThat(runtimeService.createIncidentQuery().listPage(0, 4).size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().listPage(2, 1).size()).isEqualTo(1);
    assertThat(runtimeService.createIncidentQuery().listPage(1, 2).size()).isEqualTo(2);
    assertThat(runtimeService.createIncidentQuery().listPage(1, 4).size()).isEqualTo(3);
  }

  @Test
  public void testQuerySorting() {
    assertThat(runtimeService.createIncidentQuery().orderByIncidentId().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentTimestamp().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentType().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByExecutionId().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByActivityId().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessInstanceId().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessDefinitionId().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByCauseIncidentId().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByRootCauseIncidentId().asc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByConfiguration().asc().list().size()).isEqualTo(4);

    assertThat(runtimeService.createIncidentQuery().orderByIncidentId().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentTimestamp().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByIncidentType().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByExecutionId().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByActivityId().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessInstanceId().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByProcessDefinitionId().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByCauseIncidentId().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByRootCauseIncidentId().desc().list().size()).isEqualTo(4);
    assertThat(runtimeService.createIncidentQuery().orderByConfiguration().desc().list().size()).isEqualTo(4);

  }

  @Test
  public void testQuerySortingByIncidentMessage()
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
