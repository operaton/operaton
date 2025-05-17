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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.*;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.FailingDelegate;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricIncidentQueryTest {

  public static final String PROCESS_DEFINITION_KEY = "oneFailingServiceTaskProcess";
  public static final BpmnModelInstance FAILING_SERVICE_TASK_MODEL  = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent("start")
    .serviceTask("task")
      .operatonAsyncBefore()
      .operatonClass(FailingDelegate.class.getName())
    .endEvent("end")
    .done();

  ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain chain = RuleChain.outerRule(engineRule).around(testHelper);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testHistoricIncidentQueryCreateEndAfterBefore() {

    // given
    // 1 failed instance of "oneFailingServiceTaskProcess"
    startProcessInstance(PROCESS_DEFINITION_KEY);
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    // one incident of each of the following processes
    testHelper.deploy(Bpmn.createExecutableProcess("proc1").startEvent().userTask().endEvent().done());
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("proc1");
    Incident incident2 = runtimeService.createIncident("foo", instance2.getId(), "a");
    // resolve incident2
    runtimeService.resolveIncident(incident2.getId());

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    // createTime
    assertThat(query.createTimeBefore(hourAgo.getTime()).count()).isZero();
    assertThat(query.createTimeBefore(hourFromNow.getTime()).count()).isEqualTo(2);
    assertThat(query.createTimeAfter(hourAgo.getTime()).count()).isEqualTo(2);
    assertThat(query.createTimeAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(query.createTimeBefore(hourFromNow.getTime()).createTimeAfter(hourAgo.getTime()).count()).isEqualTo(2);

    //endTime
    assertThat(query.endTimeBefore(hourAgo.getTime()).count()).isZero();
    assertThat(query.endTimeBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(query.endTimeAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(query.endTimeAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(query.endTimeBefore(hourFromNow.getTime()).endTimeAfter(hourAgo.getTime()).count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByIncidentId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String incidentId = historyService.createHistoricIncidentQuery().singleResult().getId();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .incidentId(incidentId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByInvalidIncidentId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.incidentId("invalid").list()).isEmpty();
    assertThat(query.incidentId("invalid").count()).isZero();

    try {
      query.incidentId(null);
      fail("It was possible to set a null value as incidentId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByIncidentType() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .incidentType(Incident.FAILED_JOB_HANDLER_TYPE);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidIncidentType() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.incidentType("invalid").list()).isEmpty();
    assertThat(query.incidentType("invalid").count()).isZero();

    try {
      query.incidentType(null);
      fail("It was possible to set a null value as incidentType.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByIncidentMessage() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .incidentMessage("exception0");

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidIncidentMessage() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.incidentMessage("invalid").list()).isEmpty();
    assertThat(query.incidentMessage("invalid").count()).isZero();

    try {
      query.incidentMessage(null);
      fail("It was possible to set a null value as incidentMessage.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByIncidentMessageLike() {

    startProcessInstance(PROCESS_DEFINITION_KEY);
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.incidentMessageLike("exception").list()).isEmpty();
    assertThat(query.incidentMessageLike("exception%").list()).hasSize(1);
    assertThat(query.incidentMessageLike("%xception%").list()).hasSize(1);
  }


  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByProcessDefinitionId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    ProcessInstance pi = runtimeService.createProcessInstanceQuery().singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .processDefinitionId(pi.getProcessDefinitionId());

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidProcessDefinitionId() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.processDefinitionId("invalid").list()).isEmpty();
    assertThat(query.processDefinitionId("invalid").count()).isZero();

    try {
      query.processDefinitionId(null);
      fail("It was possible to set a null value as processDefinitionId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByProcessDefinitionKey() {
    // given
    // 1 failed instance of "oneFailingServiceTaskProcess"
    startProcessInstance(PROCESS_DEFINITION_KEY);

    // one incident of each of the following processes
    testHelper.deploy(Bpmn.createExecutableProcess("proc1").startEvent().userTask().endEvent().done());
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("proc1");
    Incident incident2 = runtimeService.createIncident("foo", instance2.getId(), "a");

    testHelper.deploy(Bpmn.createExecutableProcess("proc2").startEvent().userTask().endEvent().done());
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("proc2");
    Incident incident3 = runtimeService.createIncident("foo", instance3.getId(), "b");

    // when
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();
    List<HistoricIncident> incidents = query.processDefinitionKeyIn("proc1", "proc2")
        .orderByConfiguration()
        .asc()
        .list();

    // then
    assertThat(incidents).hasSize(2);
    assertThat(incidents.get(0).getId()).isEqualTo(incident2.getId());
    assertThat(incidents.get(1).getId()).isEqualTo(incident3.getId());

    assertThat(query.processDefinitionKey("proc").list()).isEmpty();
    assertThat(query.processDefinitionKey("proc1").list()).hasSize(1);
    assertThat(query.processDefinitionKey("proc2").list()).hasSize(1);
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
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByProcessInstanceId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    ProcessInstance pi = runtimeService.createProcessInstanceQuery().singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .processInstanceId(pi.getId());

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidProcessInstanceId() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.processInstanceId("invalid").list()).isEmpty();
    assertThat(query.processInstanceId("invalid").count()).isZero();

    try {
      query.processInstanceId(null);
      fail("It was possible to set a null value as processInstanceId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByExecutionId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    ProcessInstance pi = runtimeService.createProcessInstanceQuery().singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .executionId(pi.getId());

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidExecutionId() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.executionId("invalid").list()).isEmpty();
    assertThat(query.executionId("invalid").count()).isZero();

    try {
      query.executionId(null);
      fail("It was possible to set a null value as executionId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByActivityId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .activityId("theServiceTask");

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByInvalidActivityId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.activityId("invalid").list()).isEmpty();
    assertThat(query.activityId("invalid").count()).isZero();

    try {
      query.activityId(null);
      fail("It was possible to set a null value as activityId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByFailedActivityId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .failedActivityId("theServiceTask");

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByFailedInvalidActivityId() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.failedActivityId("invalid").list()).isEmpty();
    assertThat(query.failedActivityId("invalid").count()).isZero();

    try {
      query.failedActivityId(null);
      fail("It was possible to set a null value as failedActivityId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricIncidentQueryTest.testQueryByCauseIncidentId.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByCauseIncidentId() {
    startProcessInstance("process");

    String processInstanceId = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .singleResult()
        .getId();

    Incident incident = runtimeService.createIncidentQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .causeIncidentId(incident.getId());

    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);
  }

  @Test
  public void testQueryByInvalidCauseIncidentId() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.causeIncidentId("invalid").list()).isEmpty();
    assertThat(query.causeIncidentId("invalid").count()).isZero();

    try {
      query.causeIncidentId(null);
      fail("It was possible to set a null value as causeIncidentId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricIncidentQueryTest.testQueryByCauseIncidentId.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByRootCauseIncidentId() {
    startProcessInstance("process");

    String processInstanceId = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .singleResult()
        .getId();

    Incident incident = runtimeService.createIncidentQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .rootCauseIncidentId(incident.getId());

    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);
  }

  @Test
  public void testQueryByInvalidRootCauseIncidentId() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.rootCauseIncidentId("invalid").list()).isEmpty();
    assertThat(query.rootCauseIncidentId("invalid").count()).isZero();

    try {
      query.rootCauseIncidentId(null);
      fail("It was possible to set a null value as rootCauseIncidentId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByConfiguration() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String configuration = managementService.createJobQuery().singleResult().getId();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .configuration(configuration);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidConfigurationId() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.configuration("invalid").list()).isEmpty();
    assertThat(query.configuration("invalid").count()).isZero();

    try {
      query.configuration(null);
      fail("It was possible to set a null value as configuration.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByHistoryConfiguration() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    // get the latest historic job log id
    Integer latestJobLogId = historyService.createHistoricJobLogQuery().list()
        .stream()
        .map(hjl -> Integer.parseInt(hjl.getId()))
        .max(Integer::compare)
        .get();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .historyConfiguration(latestJobLogId.toString());

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByNotExistingHistoryConfiguration() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .historyConfiguration("-1");

    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
  }

  @Test
  public void testQueryByInvalidHistoryConfigurationId() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.historyConfiguration("invalid").list()).isEmpty();
    assertThat(query.historyConfiguration("invalid").count()).isZero();

    try {
      query.historyConfiguration(null);
      fail("It was possible to set a null value as history configuration.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByOpen() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .open();

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidOpen() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();
    var historicIncidentQuery = query.open();

    try {
      historicIncidentQuery.open();
      fail("It was possible to set a the open flag twice.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByResolved() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 1);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .resolved();

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidResolved() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();
    var historicIncidentQuery = query.resolved();

    try {
      historicIncidentQuery.resolved();
      fail("It was possible to set a the resolved flag twice.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryByDeleted() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();
    runtimeService.deleteProcessInstance(processInstanceId, null);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
        .deleted();

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidDeleted() {
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();
    var historicIncidentQuery = query.deleted();

    try {
      historicIncidentQuery.deleted();
      fail("It was possible to set a the deleted flag twice.");
    } catch (ProcessEngineException e) { }
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

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId1, jobDefinitionId2);

    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);

    query = historyService.createHistoricIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId1);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);

    query = historyService.createHistoricIncidentQuery()
      .jobDefinitionIdIn(jobDefinitionId2);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByUnknownJobDefinitionId() {
    String processDefinitionId = testHelper.deployAndGetDefinition(FAILING_SERVICE_TASK_MODEL).getId();

    runtimeService.startProcessInstanceById(processDefinitionId);
    testHelper.executeAvailableJobs();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery()
      .jobDefinitionIdIn("unknown");

    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
  }

  @Test
  public void testQueryByNullJobDefinitionId() {
    var historicIncidentQuery = historyService.createHistoricIncidentQuery();
    try {
      historicIncidentQuery.jobDefinitionIdIn((String) null);
      fail("Should fail");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("jobDefinitionIds contains null value");
    }
  }

  @Test
  public void testQueryByNullJobDefinitionIds() {
    var historicIncidentQuery = historyService.createHistoricIncidentQuery();
    try {
      historicIncidentQuery.jobDefinitionIdIn((String[]) null);
      fail("Should fail");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("jobDefinitionIds is null");
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQueryPaging() {
    startProcessInstances(PROCESS_DEFINITION_KEY, 4);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    assertThat(query.listPage(0, 4)).hasSize(4);
    assertThat(query.listPage(2, 1)).hasSize(1);
    assertThat(query.listPage(1, 2)).hasSize(2);
    assertThat(query.listPage(1, 4)).hasSize(3);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQuerySorting() {
    startProcessInstances(PROCESS_DEFINITION_KEY, 4);

    assertThat(historyService.createHistoricIncidentQuery().orderByIncidentId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByCreateTime().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByEndTime().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByIncidentType().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByExecutionId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByActivityId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByProcessInstanceId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByProcessDefinitionKey().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByProcessDefinitionId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByCauseIncidentId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByRootCauseIncidentId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByConfiguration().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByIncidentState().asc().list()).hasSize(4);

    assertThat(historyService.createHistoricIncidentQuery().orderByIncidentId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByCreateTime().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByEndTime().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByIncidentType().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByExecutionId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByActivityId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByProcessDefinitionKey().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByProcessInstanceId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByProcessDefinitionId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByCauseIncidentId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByRootCauseIncidentId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByConfiguration().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIncidentQuery().orderByIncidentState().desc().list()).hasSize(4);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  public void testQuerySortingByIncidentMessage()
  {
    // given
    startProcessInstances(PROCESS_DEFINITION_KEY, 4);

    // when
    List<HistoricIncident> ascending = historyService.createHistoricIncidentQuery().orderByIncidentMessage().asc().list();
    List<HistoricIncident> descending = historyService.createHistoricIncidentQuery().orderByIncidentMessage().desc().list();

    // then
    assertThat(ascending).extracting("incidentMessage")
      .containsExactly("exception0", "exception1", "exception2", "exception3");
    assertThat(descending).extracting("incidentMessage")
      .containsExactly("exception3", "exception2", "exception1", "exception0");
  }

  protected void startProcessInstance(String key) {
    startProcessInstances(key, 1);
  }

  protected void startProcessInstances(String key, int numberOfInstances) {

    for (int i = 0; i < numberOfInstances; i++) {
      Map<String, Object> variables = Collections.singletonMap("message", "exception" + i);

      runtimeService.startProcessInstanceByKey(key, i + "", variables);
    }

    testHelper.executeAvailableJobs();
  }
}
