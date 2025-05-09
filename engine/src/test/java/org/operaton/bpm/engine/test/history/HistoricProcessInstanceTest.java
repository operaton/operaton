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

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CallActivityModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CompensationModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.util.ChangeVariablesDelegate;
import org.operaton.bpm.engine.test.jobexecutor.FailingDelegate;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicProcessInstanceByProcessDefinitionId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicProcessInstanceByProcessDefinitionKey;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicProcessInstanceByProcessDefinitionName;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicProcessInstanceByProcessDefinitionVersion;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicProcessInstanceByProcessInstanceId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.*;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricProcessInstanceTest {

  public static final BpmnModelInstance FORK_JOIN_SUB_PROCESS_MODEL = ProcessModels.newModel()
    .startEvent()
    .subProcess("subProcess")
    .embeddedSubProcess()
      .startEvent()
      .parallelGateway("fork")
        .userTask("userTask1")
        .name("completeMe")
      .parallelGateway("join")
      .endEvent()
      .moveToNode("fork")
        .userTask("userTask2")
      .connectTo("join")
    .subProcessDone()
    .endEvent()
    .done();

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain chain = RuleChain.outerRule(engineRule).around(testHelper);

  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;
  protected TaskService taskService;
  protected CaseService caseService;

  @Before
  public void initServices() {
    repositoryService = engineRule.getRepositoryService();
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
    taskService = engineRule.getTaskService();
    caseService = engineRule.getCaseService();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testHistoricDataCreatedForProcessExecution() {

    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, 2010);
    calendar.set(Calendar.MONTH, 8);
    calendar.set(Calendar.DAY_OF_MONTH, 30);
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date noon = calendar.getTime();

    ClockUtil.setCurrentTime(noon);
    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "myBusinessKey");

    assertThat(historyService.createHistoricProcessInstanceQuery().unfinished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isZero();
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
    assertThat(historicProcessInstance.getBusinessKey()).isEqualTo(processInstance.getBusinessKey());
    assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(historicProcessInstance.getStartTime()).isEqualTo(noon);
    assertThat(historicProcessInstance.getEndTime()).isNull();
    assertThat(historicProcessInstance.getDurationInMillis()).isNull();
    assertThat(historicProcessInstance.getCaseInstanceId()).isNull();

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();

    assertThat(tasks).hasSize(1);

    // in this test scenario we assume that 25 seconds after the process start, the
    // user completes the task (yes! he must be almost as fast as me)
    Date twentyFiveSecsAfterNoon = new Date(noon.getTime() + 25*1000);
    ClockUtil.setCurrentTime(twentyFiveSecsAfterNoon);
    taskService.complete(tasks.get(0).getId());

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
    assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(historicProcessInstance.getStartTime()).isEqualTo(noon);
    assertThat(historicProcessInstance.getEndTime()).isEqualTo(twentyFiveSecsAfterNoon);
    assertThat(historicProcessInstance.getDurationInMillis()).isEqualTo(Long.valueOf(25 * 1000));
    assertThat(((HistoricProcessInstanceEventEntity) historicProcessInstance).getDurationRaw()).isGreaterThanOrEqualTo(25000);
    assertThat(historicProcessInstance.getCaseInstanceId()).isNull();

    assertThat(historyService.createHistoricProcessInstanceQuery().unfinished().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1);

    runtimeService.startProcessInstanceByKey("oneTaskProcess", "myBusinessKey");
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().unfinished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().unfinished().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testLongRunningHistoricDataCreatedForProcessExecution() {
    final long ONE_YEAR = 1000 * 60 * 60 * 24 * 365;

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    Date now = cal.getTime();
    ClockUtil.setCurrentTime(now);

    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "myBusinessKey");

    assertThat(historyService.createHistoricProcessInstanceQuery().unfinished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isZero();
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(historicProcessInstance.getStartTime()).isEqualTo(now);

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);

    // in this test scenario we assume that one year after the process start, the
    // user completes the task (incredible speedy!)
    cal.add(Calendar.YEAR, 1);
    Date oneYearLater = cal.getTime();
    ClockUtil.setCurrentTime(oneYearLater);

    taskService.complete(tasks.get(0).getId());

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(historicProcessInstance.getStartTime()).isEqualTo(now);
    assertThat(historicProcessInstance.getEndTime()).isEqualTo(oneYearLater);
    assertThat(historicProcessInstance.getDurationInMillis()).isGreaterThanOrEqualTo(ONE_YEAR);
    assertThat(((HistoricProcessInstanceEventEntity) historicProcessInstance).getDurationRaw()).isGreaterThanOrEqualTo(ONE_YEAR);

    assertThat(historyService.createHistoricProcessInstanceQuery().unfinished().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testDeleteProcessInstanceHistoryCreated() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertThat(processInstance).isNotNull();

    // delete process instance should not delete the history
    runtimeService.deleteProcessInstance(processInstance.getId(), "cancel");
    HistoricProcessInstance historicProcessInstance =
      historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(historicProcessInstance.getEndTime()).isNotNull();
  }

  @Test
  public void testDeleteProcessInstanceWithoutSubprocessInstances() {
    // given a process instance with subprocesses
    BpmnModelInstance calling =
        Bpmn.createExecutableProcess("calling")
          .startEvent()
          .callActivity()
            .calledElement("called")
          .endEvent("endA")
          .done();

    BpmnModelInstance called = Bpmn.createExecutableProcess("called")
        .startEvent()
        .userTask("Task1")
        .endEvent()
        .done();

    deployment(calling, called);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("calling");

    // when the process instance is deleted and we do skip sub processes
    String id = instance.getId();
    runtimeService.deleteProcessInstance(id, "test_purposes", false, true, false, true);

    // then
    List<HistoricProcessInstance> historicSubprocessList = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("called").list();
    for (HistoricProcessInstance historicProcessInstance : historicSubprocessList) {
      assertThat(historicProcessInstance.getSuperProcessInstanceId()).isNull();
    }
  }

  @Test
  public void testDeleteProcessInstanceWithSubprocessInstances() {
    // given a process instance with subprocesses
    BpmnModelInstance calling =
        Bpmn.createExecutableProcess("calling")
          .startEvent()
          .callActivity()
            .calledElement("called")
          .endEvent("endA")
          .done();

    BpmnModelInstance called = Bpmn.createExecutableProcess("called")
        .startEvent()
        .userTask("Task1")
        .endEvent()
        .done();

    deployment(calling, called);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("calling");

    // when the process instance is deleted and we do not skip sub processes
    String id = instance.getId();
    runtimeService.deleteProcessInstance(id, "test_purposes", false, true, false, false);

    // then
    List<HistoricProcessInstance> historicSubprocessList = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("called").list();
    for (HistoricProcessInstance historicProcessInstance : historicSubprocessList) {
      assertThat(historicProcessInstance.getSuperProcessInstanceId()).isNotNull();
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @SuppressWarnings("deprecation")
  public void testHistoricProcessInstanceStartDate() {
    ClockUtil.setCurrentTime(new Date());
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Date date = ClockUtil.getCurrentTime();

    assertThat(historyService.createHistoricProcessInstanceQuery().startDateOn(date).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().startDateBy(date).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().startDateBy(DateUtils.addDays(date, -1)).count()).isEqualTo(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().startDateBy(DateUtils.addDays(date, 1)).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().startDateOn(DateUtils.addDays(date, -1)).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().startDateOn(DateUtils.addDays(date, 1)).count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @SuppressWarnings("deprecation")
  public void testHistoricProcessInstanceFinishDateUnfinished() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Date date = new Date();

    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateOn(date).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateBy(date).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateBy(DateUtils.addDays(date, 1)).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateBy(DateUtils.addDays(date, -1)).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateOn(DateUtils.addDays(date, -1)).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateOn(DateUtils.addDays(date, 1)).count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @SuppressWarnings("deprecation")
  public void testHistoricProcessInstanceFinishDateFinished() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Date date = new Date();

    runtimeService.deleteProcessInstance(pi.getId(), "cancel");

    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateOn(date).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateBy(date).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateBy(DateUtils.addDays(date, 1)).count()).isEqualTo(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateBy(DateUtils.addDays(date, -1)).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateOn(DateUtils.addDays(date, -1)).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishDateOn(DateUtils.addDays(date, 1)).count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void testHistoricProcessInstanceDelete() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    runtimeService.deleteProcessInstance(pi.getId(), "cancel");

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicProcessInstance.getDeleteReason()).isNotNull();
    assertThat(historicProcessInstance.getDeleteReason()).isEqualTo("cancel");

    assertThat(historicProcessInstance.getEndTime()).isNotNull();
  }

  /** See: <a href="https://app.camunda.com/jira/browse/CAM-1324">CAM-1324</a> */
  @Test
  @Deployment
  public void testHistoricProcessInstanceDeleteAsync() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failing");

    runtimeService.deleteProcessInstance(pi.getId(), "cancel");

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicProcessInstance.getDeleteReason()).isNotNull();
    assertThat(historicProcessInstance.getDeleteReason()).isEqualTo("cancel");

    assertThat(historicProcessInstance.getEndTime()).isNotNull();
  }

  @Test
  @Deployment
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcessInstanceQueryWithIncidents() {
    // start instance with incidents
    runtimeService.startProcessInstanceByKey("Process_1");
    testHelper.executeAvailableJobs();

    // start instance without incidents
    runtimeService.startProcessInstanceByKey("Process_1");

    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(2);
    assertThat(historyService.createHistoricProcessInstanceQuery().list()).hasSize(2);

    assertThat(historyService.createHistoricProcessInstanceQuery().withIncidents().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().withIncidents().list()).hasSize(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessageLike("Unknown property used%\\_Tr%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessageLike("Unknown property used%\\_Tr%").list()).hasSize(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessageLike("Unknown message%").count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessageLike("Unknown message%").list()).isEmpty();

    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessage("Unknown property used in expression: ${incidentTrigger1}. Cause: Cannot resolve identifier 'incidentTrigger1'").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessage("Unknown property used in expression: ${incidentTrigger1}. Cause: Cannot resolve identifier 'incidentTrigger1'").list()).hasSize(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessage("Unknown property used in expression: ${incident_Trigger2}. Cause: Cannot resolve identifier 'incident_Trigger2'").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessage("Unknown property used in expression: ${incident_Trigger2}. Cause: Cannot resolve identifier 'incident_Trigger2'").list()).hasSize(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessage("Unknown message").count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessage("Unknown message").list()).isEmpty();

    assertThat(historyService.createHistoricProcessInstanceQuery().incidentType("failedJob").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentType("failedJob").list()).hasSize(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().withRootIncidents().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().withRootIncidents().list()).hasSize(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldDeleteIncidentAfterJobWasSuccessfully.bpmn"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcessInstanceQueryIncidentStatusOpen() {
    //given a processes instance, which will fail
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    runtimeService.startProcessInstanceByKey("failingProcessWithUserTask", parameters);

    //when jobs are executed till retry count is zero
    testHelper.executeAvailableJobs();

    //then query for historic process instance with open incidents will return one
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentStatus("open").count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldDeleteIncidentAfterJobWasSuccessfully.bpmn"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcessInstanceQueryIncidentStatusResolved() {
    //given a incident processes instance
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("failingProcessWithUserTask", parameters);
    testHelper.executeAvailableJobs();

    //when `fail` variable is set to true and job retry count is set to one and executed again
    runtimeService.setVariable(pi1.getId(), "fail", false);
    Job jobToResolve = managementService.createJobQuery().processInstanceId(pi1.getId()).singleResult();
    managementService.setJobRetries(jobToResolve.getId(), 1);
    testHelper.executeAvailableJobs();

    //then query for historic process instance with resolved incidents will return one
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentStatus("resolved").count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldDeleteIncidentAfterJobWasSuccessfully.bpmn"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcessInstanceQueryIncidentStatusOpenWithTwoProcesses() {
    //given two processes, which will fail, are started
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("failingProcessWithUserTask", parameters);
    runtimeService.startProcessInstanceByKey("failingProcessWithUserTask", parameters);
    testHelper.executeAvailableJobs();
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentStatus("open").count()).isEqualTo(2);

    //when 'fail' variable is set to false, job retry count is set to one
    //and available jobs are executed
    runtimeService.setVariable(pi1.getId(), "fail", false);
    Job jobToResolve = managementService.createJobQuery().processInstanceId(pi1.getId()).singleResult();
    managementService.setJobRetries(jobToResolve.getId(), 1);
    testHelper.executeAvailableJobs();

    //then query with open and with resolved incidents returns one
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentStatus("open").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentStatus("resolved").count()).isEqualTo(1);
  }

  @Test
  public void testHistoricProcessInstanceQueryWithIncidentMessageNull() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.incidentMessage(null);
      fail("incidentMessage with null value is not allowed");
    } catch( NullValueException nex ) {
      assertThat(nex.getMessage()).isEqualTo("incidentMessage is null");
    }
  }

  @Test
  public void testHistoricProcessInstanceQueryWithIncidentMessageLikeNull() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.incidentMessageLike(null);
      fail("incidentMessageLike with null value is not allowed");
    } catch( NullValueException nex ) {
      assertThat(nex.getMessage()).isEqualTo("incidentMessageLike is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneAsyncTaskProcess.bpmn20.xml"})
  public void testHistoricProcessInstanceQuery() {
    Calendar startTime = Calendar.getInstance();

    ClockUtil.setCurrentTime(startTime.getTime());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey_123");
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    // Start/end dates
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedBefore(hourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedBefore(hourFromNow.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedAfter(hourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().startedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().startedBefore(hourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().startedAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().startedAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().startedAfter(hourFromNow.getTime()).startedBefore(hourAgo.getTime()).count()).isZero();

    // General fields
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey("businessKey_123").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKeyLike("business%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKeyLike("%sinessKey\\_123").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKeyLike("%siness%").count()).isEqualTo(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionName("The One Task_Process").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionNameLike("The One Task%").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionNameLike("%One Task\\_Process").count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionNameLike("%One Task%").count()).isEqualTo(1);

    List<String> exludeIds = new ArrayList<>();
    exludeIds.add("unexistingProcessDefinition");

    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKeyNotIn(exludeIds).count()).isEqualTo(1);

    exludeIds.add("oneTaskProcess");
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTaskProcess").processDefinitionKeyNotIn(exludeIds).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKeyNotIn(exludeIds).count()).isZero();
    var emptyProcessDefinitionKeys = List.of("");
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    try {
      // oracle handles empty string like null which seems to lead to undefined behavior of the LIKE comparison
      historicProcessInstanceQuery.processDefinitionKeyNotIn(emptyProcessDefinitionKeys);
      fail("Exception expected");
    }
    catch (NotValidException e) {
      assertThat(e.getMessage()).isEqualTo("processDefinitionKeys contains empty string");
    }

    // After finishing process
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
    assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedBefore(hourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedAfter(hourFromNow.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().finishedAfter(hourFromNow.getTime()).finishedBefore(hourAgo.getTime()).count()).isZero();

    // No incidents should are created
    assertThat(historyService.createHistoricProcessInstanceQuery().withIncidents().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().incidentMessageLike("Unknown property used%").count()).isZero();
    assertThat(historyService
        .createHistoricProcessInstanceQuery()
        .incidentMessage("Unknown property used in expression: #{failing}. Cause: Cannot resolve identifier 'failing'")
        .count()).isZero();

    // execute activities
    assertThat(historyService.createHistoricProcessInstanceQuery().executedActivityAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().executedActivityBefore(hourAgo.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().executedActivityBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().executedActivityAfter(hourFromNow.getTime()).count()).isZero();

    // execute jobs
    if (engineRule.getProcessEngineConfiguration().getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_FULL)) {
      assertThat(historyService.createHistoricProcessInstanceQuery().executedJobAfter(hourAgo.getTime()).count()).isEqualTo(1);
      assertThat(historyService.createHistoricProcessInstanceQuery().executedActivityBefore(hourAgo.getTime()).count()).isZero();
      assertThat(historyService.createHistoricProcessInstanceQuery().executedActivityBefore(hourFromNow.getTime()).count()).isEqualTo(1);
      assertThat(historyService.createHistoricProcessInstanceQuery().executedActivityAfter(hourFromNow.getTime()).count()).isZero();
    }
  }

  @Test
  public void testHistoricProcessInstanceSorting() {

    deployment("org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml");
    deployment("org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testSorting.bpmn20.xml");

    //deploy second version of the same process definition
    deployment("org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml");

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").list();
    for (ProcessDefinition processDefinition: processDefinitions) {
      runtimeService.startProcessInstanceById(processDefinition.getId());
    }
    runtimeService.startProcessInstanceByKey("process");

    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().asc().list();
    assertThat(processInstances).hasSize(3);
    verifySorting(processInstances, historicProcessInstanceByProcessInstanceId());

    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().asc().list()).hasSize(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().asc().list()).hasSize(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().asc().list()).hasSize(3);

    processInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().asc().list();
    assertThat(processInstances).hasSize(3);
    verifySorting(processInstances, historicProcessInstanceByProcessDefinitionId());

    processInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionKey().asc().list();
    assertThat(processInstances).hasSize(3);
    verifySorting(processInstances, historicProcessInstanceByProcessDefinitionKey());

    processInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionName().asc().list();
    assertThat(processInstances).hasSize(3);
    verifySorting(processInstances, historicProcessInstanceByProcessDefinitionName());

    processInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionVersion().asc().list();
    assertThat(processInstances).hasSize(3);
    verifySorting(processInstances, historicProcessInstanceByProcessDefinitionVersion());

    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().asc().list()).hasSize(3);

    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().desc().list()).hasSize(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc().list()).hasSize(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().list()).hasSize(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().desc().list()).hasSize(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().desc().list()).hasSize(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().desc().list()).hasSize(3);

    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().asc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().asc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().asc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().asc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().asc().count()).isEqualTo(3);

    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().desc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().desc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().desc().count()).isEqualTo(3);
    assertThat(historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().desc().count()).isEqualTo(3);

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testHistoricProcessInstanceSubProcess() {
    ProcessInstance superPi = runtimeService.startProcessInstanceByKey("subProcessQueryTest");
    ProcessInstance subPi = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superPi.getProcessInstanceId()).singleResult();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().subProcessInstanceId(subPi.getProcessInstanceId()).singleResult();
    assertThat(historicProcessInstance).isNotNull();
    assertThat(superPi.getId()).isEqualTo(historicProcessInstance.getId());
  }

  @Test
  public void testInvalidSorting() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.asc();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");
    }

    try {
      historicProcessInstanceQuery.desc();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");
    }

    var historicProcessInstanceQuery1 = historicProcessInstanceQuery.orderByProcessInstanceId();
    try {
      historicProcessInstanceQuery1.list();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid query: call asc() or desc() after using orderByXX(): direction is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  // ACT-1098
  public void testDeleteReason() {
    if(!ProcessEngineConfiguration.HISTORY_NONE.equals(engineRule.getProcessEngineConfiguration().getHistory())) {
      final String deleteReason = "some delete reason";
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");
      runtimeService.deleteProcessInstance(pi.getId(), deleteReason);
      HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(pi.getId()).singleResult();
      assertThat(hpi.getDeleteReason()).isEqualTo(deleteReason);
    }
  }

  @Test
  @Deployment
  public void testLongProcessDefinitionKey() {
    // must be equals to attribute id of element process in process model
    final String PROCESS_DEFINITION_KEY = "myrealrealrealrealrealrealrealrealrealrealreallongprocessdefinitionkeyawesome";

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    // get HPI by process instance id
    HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(hpi).isNotNull();
    testHelper.assertProcessEnded(hpi.getId());

    // get HPI by process definition key
    HistoricProcessInstance hpi2 = historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertThat(hpi2).isNotNull();
    testHelper.assertProcessEnded(hpi2.getId());

    // check we got the same HPIs
    assertThat(hpi2.getId()).isEqualTo(hpi.getId());

  }

  @Test
  @Deployment(resources =
    {
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testQueryByCaseInstanceId.cmmn",
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testQueryByCaseInstanceId.bpmn20.xml"
      })
  public void testQueryByCaseInstanceId() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    query.caseInstanceId(caseInstanceId);

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);

    HistoricProcessInstance historicProcessInstance = query.singleResult();
    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getEndTime()).isNull();

    assertThat(historicProcessInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // complete existing user task -> completes the process instance
    String taskId = taskService
        .createTaskQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult()
        .getId();
    taskService.complete(taskId);

    // the completed historic process instance is still associated with the
    // case instance id
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);

    historicProcessInstance = query.singleResult();
    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getEndTime()).isNotNull();

    assertThat(historicProcessInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

  }

  @Test
  @Deployment(resources =
    {
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testQueryByCaseInstanceId.cmmn",
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testQueryByCaseInstanceIdHierarchy-super.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testQueryByCaseInstanceIdHierarchy-sub.bpmn20.xml"
      })
  public void testQueryByCaseInstanceIdHierarchy() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    query.caseInstanceId(caseInstanceId);

    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);

    for (HistoricProcessInstance hpi : query.list()) {
      assertThat(hpi.getCaseInstanceId()).isEqualTo(caseInstanceId);
    }

    // complete existing user task -> completes the process instance(s)
    String taskId = taskService
        .createTaskQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult()
        .getId();
    taskService.complete(taskId);

    // the completed historic process instance is still associated with the
    // case instance id
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);

    for (HistoricProcessInstance hpi : query.list()) {
      assertThat(hpi.getCaseInstanceId()).isEqualTo(caseInstanceId);
    }

  }

  @Test
  public void testQueryByInvalidCaseInstanceId() {
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    query.caseInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  @Deployment(resources =
    {
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testBusinessKey.cmmn",
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testBusinessKey.bpmn20.xml"
      })
  public void testBusinessKey() {
    // given
    String businessKey = "aBusinessKey";

    caseService
      .withCaseDefinitionByKey("case")
      .businessKey(businessKey)
      .create()
      .getId();

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    query.processInstanceBusinessKey(businessKey);

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);

    HistoricProcessInstance historicProcessInstance = query.singleResult();
    assertThat(historicProcessInstance).isNotNull();

    assertThat(historicProcessInstance.getBusinessKey()).isEqualTo(businessKey);

  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testStartActivityId-super.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testStartActivityId-sub.bpmn20.xml"
  })
  public void testStartActivityId() {
    // given

    // when
    runtimeService.startProcessInstanceByKey("super");

    // then
    HistoricProcessInstance hpi = historyService
        .createHistoricProcessInstanceQuery()
        .processDefinitionKey("sub")
        .singleResult();

    assertThat(hpi.getStartActivityId()).isEqualTo("theSubStart");

  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testStartActivityId-super.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testAsyncStartActivityId-sub.bpmn20.xml"
  })
  public void testAsyncStartActivityId() {
    // given
    runtimeService.startProcessInstanceByKey("super");

    // when
    testHelper.executeAvailableJobs();

    // then
    HistoricProcessInstance hpi = historyService
        .createHistoricProcessInstanceQuery()
        .processDefinitionKey("sub")
        .singleResult();

    assertThat(hpi.getStartActivityId()).isEqualTo("theSubStart");

  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testStartByKeyWithCaseInstanceId() {
    String caseInstanceId = "aCaseInstanceId";

    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", null, caseInstanceId).getId();

    HistoricProcessInstance firstInstance = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    assertThat(firstInstance).isNotNull();

    assertThat(firstInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // the second possibility to start a process instance /////////////////////////////////////////////

    processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", null, caseInstanceId, null).getId();

    HistoricProcessInstance secondInstance = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    assertThat(secondInstance).isNotNull();

    assertThat(secondInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testStartByIdWithCaseInstanceId() {
    String processDefinitionId = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("oneTaskProcess")
        .singleResult()
        .getId();

    String caseInstanceId = "aCaseInstanceId";
    String processInstanceId = runtimeService.startProcessInstanceById(processDefinitionId, null, caseInstanceId).getId();

    HistoricProcessInstance firstInstance = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    assertThat(firstInstance).isNotNull();

    assertThat(firstInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // the second possibility to start a process instance /////////////////////////////////////////////

    processInstanceId = runtimeService.startProcessInstanceById(processDefinitionId, null, caseInstanceId, null).getId();

    HistoricProcessInstance secondInstance = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    assertThat(secondInstance).isNotNull();

    assertThat(secondInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

  }

  @Test
  @Deployment
  @SuppressWarnings("deprecation")
  public void testEndTimeAndEndActivity() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    String taskId = taskService
        .createTaskQuery()
        .taskDefinitionKey("userTask2")
        .singleResult()
        .getId();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when (1)
    taskService.complete(taskId);

    // then (1)
    HistoricProcessInstance historicProcessInstance = query.singleResult();

    assertThat(historicProcessInstance.getEndActivityId()).isNull();
    assertThat(historicProcessInstance.getEndTime()).isNull();

    // when (2)
    runtimeService.deleteProcessInstance(processInstanceId, null);

    // then (2)
    historicProcessInstance = query.singleResult();

    assertThat(historicProcessInstance.getEndActivityId()).isNull();
    assertThat(historicProcessInstance.getEndTime()).isNotNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"
  })
  public void testQueryBySuperCaseInstanceId() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneProcessTaskCase").getId();

    HistoricProcessInstanceQuery query = historyService
        .createHistoricProcessInstanceQuery()
        .superCaseInstanceId(superCaseInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);

    HistoricProcessInstance subProcessInstance = query.singleResult();
    assertThat(subProcessInstance).isNotNull();
    assertThat(subProcessInstance.getSuperCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subProcessInstance.getSuperProcessInstanceId()).isNull();
  }

  @Test
  public void testQueryByInvalidSuperCaseInstanceId() {
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    query.superCaseInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithCaseCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn" })
  public void testQueryBySubCaseInstanceId() {
    String superProcessInstanceId = runtimeService.startProcessInstanceByKey("subProcessQueryTest").getId();

    String subCaseInstanceId = caseService
        .createCaseInstanceQuery()
        .superProcessInstanceId(superProcessInstanceId)
        .singleResult()
        .getId();

    HistoricProcessInstanceQuery query = historyService
        .createHistoricProcessInstanceQuery()
        .subCaseInstanceId(subCaseInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);

    HistoricProcessInstance superProcessInstance = query.singleResult();
    assertThat(superProcessInstance).isNotNull();
    assertThat(superProcessInstance.getId()).isEqualTo(superProcessInstanceId);
    assertThat(superProcessInstance.getSuperCaseInstanceId()).isNull();
    assertThat(superProcessInstance.getSuperProcessInstanceId()).isNull();
  }

  @Test
  public void testQueryByInvalidSubCaseInstanceId() {
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    query.subCaseInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"
  })
  public void testSuperCaseInstanceIdProperty() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneProcessTaskCase").getId();

    caseService
        .createCaseExecutionQuery()
        .activityId("PI_ProcessTask_1")
        .singleResult()
        .getId();

    HistoricProcessInstance instance = historyService
        .createHistoricProcessInstanceQuery()
        .singleResult();

    assertThat(instance).isNotNull();
    assertThat(instance.getSuperCaseInstanceId()).isEqualTo(superCaseInstanceId);

    String taskId = taskService
        .createTaskQuery()
        .singleResult()
        .getId();
    taskService.complete(taskId);

    instance = historyService
        .createHistoricProcessInstanceQuery()
        .singleResult();

    assertThat(instance).isNotNull();
    assertThat(instance.getSuperCaseInstanceId()).isEqualTo(superCaseInstanceId);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testProcessDefinitionKeyProperty() {
    // given
    String key = "oneTaskProcess";
    String processInstanceId = runtimeService.startProcessInstanceByKey(key).getId();

    // when
    HistoricProcessInstance instance = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    // then
    assertThat(instance.getProcessDefinitionKey()).isNotNull();
    assertThat(instance.getProcessDefinitionKey()).isEqualTo(key);
  }

  @Test
  @Deployment
  public void testProcessInstanceShouldBeActive() {
    // given

    // when
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // then
    HistoricProcessInstance historicProcessInstance = historyService
      .createHistoricProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    assertThat(historicProcessInstance.getEndTime()).isNull();
    assertThat(historicProcessInstance.getDurationInMillis()).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testRetrieveProcessDefinitionName() {

    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    // then
    assertThat(historicProcessInstance.getProcessDefinitionName()).isEqualTo("The One Task Process");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testRetrieveProcessDefinitionVersion() {

    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    // then
    assertThat(historicProcessInstance.getProcessDefinitionVersion().intValue()).isEqualTo(1);
  }

  @Test
  public void testHistoricProcInstExecutedActivityInInterval() {
    // given proc instance with wait state
    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc")
                                  .startEvent()
                                    .userTask()
                                  .endEvent()
                                  .done();
    deployment(model);

    Calendar hourFromNow = (Calendar) now.clone();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    runtimeService.startProcessInstanceByKey("proc");

    //when query historic process instance which has executed an activity after the start time
    // and before a hour after start time
    HistoricProcessInstance historicProcessInstance =
      historyService.createHistoricProcessInstanceQuery()
        .executedActivityAfter(now.getTime())
        .executedActivityBefore(hourFromNow.getTime())
        .singleResult();


    //then query returns result
    assertThat(historicProcessInstance).isNotNull();


    // when proc inst is not in interval
    Calendar sixHoursFromNow = (Calendar) now.clone();
    sixHoursFromNow.add(Calendar.HOUR_OF_DAY, 6);


    historicProcessInstance =
      historyService.createHistoricProcessInstanceQuery()
        .executedActivityAfter(hourFromNow.getTime())
        .executedActivityBefore(sixHoursFromNow.getTime())
        .singleResult();

    //then query should return NO result
    assertThat(historicProcessInstance).isNull();
  }

  @Test
  public void testHistoricProcInstExecutedActivityAfter() {
    // given
    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar hourFromNow = (Calendar) now.clone();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    runtimeService.startProcessInstanceByKey("proc");

    //when query historic process instance which has executed an activity after the start time
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().executedActivityAfter(now.getTime()).singleResult();

    //then query returns result
    assertThat(historicProcessInstance).isNotNull();

    //when query historic proc inst with execute activity after a hour of the starting time
    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().executedActivityAfter(hourFromNow.getTime()).singleResult();

    //then query returns no result
    assertThat(historicProcessInstance).isNull();
  }

  @Test
  public void testHistoricProcInstExecutedActivityBefore() {
    // given
    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar hourBeforeNow = (Calendar) now.clone();
    hourBeforeNow.add(Calendar.HOUR, -1);

    runtimeService.startProcessInstanceByKey("proc");

    //when query historic process instance which has executed an activity before the start time
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().executedActivityBefore(now.getTime()).singleResult();

    //then query returns result, since the query is less-then-equal
    assertThat(historicProcessInstance).isNotNull();

    //when query historic proc inst which executes an activity an hour before the starting time
    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().executedActivityBefore(hourBeforeNow.getTime()).singleResult();

    //then query returns no result
    assertThat(historicProcessInstance).isNull();
  }

  @Test
  public void testHistoricProcInstExecutedActivityWithTwoProcInsts() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar now = Calendar.getInstance();
    Calendar hourBeforeNow = (Calendar) now.clone();
    hourBeforeNow.add(Calendar.HOUR, -1);

    ClockUtil.setCurrentTime(hourBeforeNow.getTime());
    runtimeService.startProcessInstanceByKey("proc");

    ClockUtil.setCurrentTime(now.getTime());
    runtimeService.startProcessInstanceByKey("proc");

    //when query execute activity between now and an hour ago
    List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery()
                                                       .executedActivityAfter(hourBeforeNow.getTime())
                                                       .executedActivityBefore(now.getTime()).list();

    //then two historic process instance have to be returned
    assertThat(list).hasSize(2);

    //when query execute activity after an half hour before now
    Calendar halfHour = (Calendar) now.clone();
    halfHour.add(Calendar.MINUTE, -30);
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .executedActivityAfter(halfHour.getTime()).singleResult();

    //then only the latest historic process instance is returned
    assertThat(historicProcessInstance).isNotNull();
  }


  @Test
  public void testHistoricProcInstExecutedActivityWithEmptyInterval() {
    // given
    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar hourBeforeNow = (Calendar) now.clone();
    hourBeforeNow.add(Calendar.HOUR, -1);

    runtimeService.startProcessInstanceByKey("proc");

    //when query historic proc inst which executes an activity an hour before and after the starting time
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .executedActivityBefore(hourBeforeNow.getTime())
      .executedActivityAfter(hourBeforeNow.getTime()).singleResult();

    //then query returns no result
    assertThat(historicProcessInstance).isNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcInstExecutedJobAfter() {
    // given
    BpmnModelInstance asyncModel = Bpmn.createExecutableProcess("async").startEvent().operatonAsyncBefore().endEvent().done();
    deployment(asyncModel);
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    Calendar hourFromNow = (Calendar) now.clone();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    runtimeService.startProcessInstanceByKey("async");
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());
    runtimeService.startProcessInstanceByKey("proc");

    //when query historic process instance which has executed an job after the start time
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .executedJobAfter(now.getTime()).singleResult();

    //then query returns only a single process instance
    assertThat(historicProcessInstance).isNotNull();

    //when query historic proc inst with execute job after a hour of the starting time
    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().executedJobAfter(hourFromNow.getTime()).singleResult();

    //then query returns no result
    assertThat(historicProcessInstance).isNull();
  }


  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcInstExecutedJobBefore() {
    // given
    BpmnModelInstance asyncModel = Bpmn.createExecutableProcess("async").startEvent().operatonAsyncBefore().endEvent().done();
    deployment(asyncModel);
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    Calendar hourBeforeNow = (Calendar) now.clone();
    hourBeforeNow.add(Calendar.HOUR_OF_DAY, -1);

    runtimeService.startProcessInstanceByKey("async");
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());
    runtimeService.startProcessInstanceByKey("proc");

    //when query historic process instance which has executed an job before the start time
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .executedJobBefore(now.getTime()).singleResult();

    //then query returns only a single process instance since before is less-then-equal
    assertThat(historicProcessInstance).isNotNull();

    //when query historic proc inst with executed job before an hour of the starting time
    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().executedJobBefore(hourBeforeNow.getTime()).singleResult();

    //then query returns no result
    assertThat(historicProcessInstance).isNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcInstExecutedJobWithTwoProcInsts() {
    // given
    BpmnModelInstance asyncModel = Bpmn.createExecutableProcess("async").startEvent().operatonAsyncBefore().endEvent().done();
    deployment(asyncModel);

    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    Calendar hourBeforeNow = (Calendar) now.clone();
    hourBeforeNow.add(Calendar.HOUR_OF_DAY, -1);

    ClockUtil.setCurrentTime(hourBeforeNow.getTime());
    runtimeService.startProcessInstanceByKey("async");
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    ClockUtil.setCurrentTime(now.getTime());
    runtimeService.startProcessInstanceByKey("async");
    runtimeService.startProcessInstanceByKey("proc");

    //when query executed job between now and an hour ago
    List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery()
      .executedJobAfter(hourBeforeNow.getTime())
      .executedJobBefore(now.getTime()).list();

    //then the two async historic process instance have to be returned
    assertThat(list).hasSize(2);

    //when query execute activity after an half hour before now
    Calendar halfHour = (Calendar) now.clone();
    halfHour.add(Calendar.MINUTE, -30);
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .executedJobAfter(halfHour.getTime()).singleResult();

    //then only the latest async historic process instance is returned
    assertThat(historicProcessInstance).isNotNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcInstExecutedJobWithEmptyInterval() {
    // given
    BpmnModelInstance asyncModel = Bpmn.createExecutableProcess("async").startEvent().operatonAsyncBefore().endEvent().done();
    deployment(asyncModel);
    BpmnModelInstance model = Bpmn.createExecutableProcess("proc").startEvent().endEvent().done();
    deployment(model);

    Calendar now = Calendar.getInstance();
    ClockUtil.setCurrentTime(now.getTime());
    Calendar hourBeforeNow = (Calendar) now.clone();
    hourBeforeNow.add(Calendar.HOUR_OF_DAY, -1);

    runtimeService.startProcessInstanceByKey("async");
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());
    runtimeService.startProcessInstanceByKey("proc");

    //when query historic proc inst with executed job before and after an hour before the starting time
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .executedJobBefore(hourBeforeNow.getTime())
      .executedJobAfter(hourBeforeNow.getTime()).singleResult();

    //then query returns no result
    assertThat(historicProcessInstance).isNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcInstQueryWithExecutedActivityIds() {
    // given
    deployment(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    Task task = taskService.createTaskQuery().active().singleResult();
    taskService.complete(task.getId());

    // assume
    HistoricActivityInstance historicActivityInstance = historyService
        .createHistoricActivityInstanceQuery()
        .processInstanceId(processInstance.getId())
        .activityId("userTask1")
        .singleResult();
    assertThat(historicActivityInstance).isNotNull();

    // when
    List<HistoricProcessInstance> result = historyService
        .createHistoricProcessInstanceQuery()
        .executedActivityIdIn(historicActivityInstance.getActivityId())
        .list();

    // then
    assertThat(result)
            .isNotNull()
            .hasSize(1);
    assertThat(processInstance.getId()).isEqualTo(result.get(0).getId());
  }

  @Test
  public void testHistoricProcInstQueryWithExecutedActivityIdsNull() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.executedActivityIdIn((String[]) null);
      fail("exception expected");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("activity ids is null");
    }
  }

  @Test
  public void testHistoricProcInstQueryWithExecutedActivityIdsContainNull() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.executedActivityIdIn(null, "1");
      fail("exception expected");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("activity ids contains null");
    }
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testHistoricProcInstQueryWithActiveActivityIds() {
    // given
    deployment(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    // assume
    HistoricActivityInstance historicActivityInstance = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("userTask1")
        .singleResult();
    assertThat(historicActivityInstance).isNotNull();

    // when
    List<HistoricProcessInstance> result = historyService
        .createHistoricProcessInstanceQuery()
        .activeActivityIdIn(historicActivityInstance.getActivityId())
        .list();

    // then
    assertThat(result)
            .isNotNull()
            .hasSize(1);
    assertThat(processInstance.getId()).isEqualTo(result.get(0).getId());
  }

  @Test
  public void shouldFailWhenQueryByNullActivityId() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.activityIdIn((String) null);
      fail("exception expected");
    }
    catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("activity ids contains null value");
    }
  }

  @Test
  public void shouldFailWhenQueryByNullActivityIds() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.activityIdIn((String[]) null);
      fail("exception expected");
    }
    catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("activity ids is null");
    }
  }

  @Test
  public void shouldReturnEmptyWhenQueryByUnknownActivityId() {
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
        .activityIdIn("unknown");

    assertThat(query.list()).isEmpty();
  }

  @Test
  public void shouldQueryByLeafActivityId() {
    // given
    ProcessDefinition oneTaskDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition gatewaySubProcessDefinition = testHelper.deployAndGetDefinition(FORK_JOIN_SUB_PROCESS_MODEL);

    // when
    String oneTaskPiOne = runtimeService.startProcessInstanceById(oneTaskDefinition.getId()).getId();
    String oneTaskPiTwo = runtimeService.startProcessInstanceById(oneTaskDefinition.getId()).getId();
    String gatewaySubProcessPiOne = runtimeService.startProcessInstanceById(gatewaySubProcessDefinition.getId()).getId();
    String gatewaySubProcessPiTwo = runtimeService.startProcessInstanceById(gatewaySubProcessDefinition.getId()).getId();

    Task task = engineRule.getTaskService().createTaskQuery()
        .processInstanceId(gatewaySubProcessPiTwo)
        .taskName("completeMe")
        .singleResult();
    engineRule.getTaskService().complete(task.getId());

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().activityIdIn("userTask");
    assertThat(query.list()).extracting("id").containsExactlyInAnyOrder(oneTaskPiOne, oneTaskPiTwo);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("userTask1", "userTask2");
    assertThat(query.list()).extracting("id")
        .containsExactlyInAnyOrder(gatewaySubProcessPiOne, gatewaySubProcessPiTwo);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("userTask", "userTask1");
    assertThat(query.list()).extracting("id")
        .containsExactlyInAnyOrder(oneTaskPiOne, oneTaskPiTwo, gatewaySubProcessPiOne);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("userTask", "userTask1", "userTask2");
    assertThat(query.list()).extracting("id")
        .containsExactlyInAnyOrder(oneTaskPiOne, oneTaskPiTwo, gatewaySubProcessPiOne, gatewaySubProcessPiTwo);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("join");
    assertThat(query.list()).extracting("id").containsExactlyInAnyOrder(gatewaySubProcessPiTwo);
  }

  @Test
  public void shouldReturnEmptyWhenQueryByNonLeafActivityId() {
    // given
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(FORK_JOIN_SUB_PROCESS_MODEL);

    // when
    runtimeService.startProcessInstanceById(processDefinition.getId());

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
        .activityIdIn("subProcess", "fork");
    assertThat(query.list()).isEmpty();
  }

  @Test
  public void shouldQueryByAsyncBeforeActivityId() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(ProcessModels.newModel()
      .startEvent("start").operatonAsyncBefore()
      .subProcess("subProcess").operatonAsyncBefore()
      .embeddedSubProcess()
        .startEvent()
        .serviceTask("task").operatonAsyncBefore().operatonExpression("${true}")
        .endEvent()
      .subProcessDone()
      .endEvent("end").operatonAsyncBefore()
      .done()
    );

    // when
    String instanceBeforeStart = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    String instanceBeforeSubProcess = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    executeJobForProcessInstance(instanceBeforeSubProcess);
    String instanceBeforeTask = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    executeJobForProcessInstance(instanceBeforeTask);
    executeJobForProcessInstance(instanceBeforeTask);
    String instanceBeforeEnd = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    executeJobForProcessInstance(instanceBeforeEnd);
    executeJobForProcessInstance(instanceBeforeEnd);
    executeJobForProcessInstance(instanceBeforeEnd);

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().activityIdIn("start");
    assertThat(query.singleResult().getId()).isEqualTo(instanceBeforeStart);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess");
    assertThat(query.singleResult().getId()).isEqualTo(instanceBeforeSubProcess);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("task");
    assertThat(query.singleResult().getId()).isEqualTo(instanceBeforeTask);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("end");
    assertThat(query.singleResult().getId()).isEqualTo(instanceBeforeEnd);
  }

  @Test
  public void shouldQueryByAsyncAfterActivityId() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(ProcessModels.newModel()
      .startEvent("start").operatonAsyncAfter()
      .subProcess("subProcess").operatonAsyncAfter()
      .embeddedSubProcess()
        .startEvent()
        .serviceTask("task").operatonAsyncAfter().operatonExpression("${true}")
        .endEvent()
      .subProcessDone()
      .endEvent("end").operatonAsyncAfter()
      .done()
    );

    // when
    String instanceAfterStart = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    String instanceAfterTask = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    executeJobForProcessInstance(instanceAfterTask);
    String instanceAfterSubProcess = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    executeJobForProcessInstance(instanceAfterSubProcess);
    executeJobForProcessInstance(instanceAfterSubProcess);
    String instanceAfterEnd = runtimeService.startProcessInstanceById(testProcess.getId()).getId();
    executeJobForProcessInstance(instanceAfterEnd);
    executeJobForProcessInstance(instanceAfterEnd);
    executeJobForProcessInstance(instanceAfterEnd);

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().activityIdIn("start");
    assertThat(query.singleResult().getId()).isEqualTo(instanceAfterStart);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("task");
    assertThat(query.singleResult().getId()).isEqualTo(instanceAfterTask);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess");
    assertThat(query.singleResult().getId()).isEqualTo(instanceAfterSubProcess);

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("end");
    assertThat(query.singleResult().getId()).isEqualTo(instanceAfterEnd);
  }

  @Test
  public void shouldReturnEmptyWhenQueryByActivityIdBeforeCompensation() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    // when
    runtimeService.startProcessInstanceById(testProcess.getId());
    testHelper.completeTask("userTask1");

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess");
    assertThat(query.list()).isEmpty();
  }

  @Test
  public void shouldQueryByActivityIdDuringCompensation() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(testProcess.getId());
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");

    // then
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess");
    assertThat(query.singleResult().getId()).isEqualTo(processInstance.getId());

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("compensationEvent");
    assertThat(query.singleResult().getId()).isEqualTo(processInstance.getId());

    query = historyService.createHistoricProcessInstanceQuery().activityIdIn("compensationHandler");
    assertThat(query.singleResult().getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void shouldQueryWithActivityIdsWithOneId() {
    // given
    String userTask1 = "userTask1";
    deployment(ProcessModels.TWO_TASKS_PROCESS);
    runtimeService.startProcessInstanceByKey("Process");
    runtimeService.startProcessInstanceByKey("Process");

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    taskService.complete(tasks.get(0).getId());

    // when
    List<HistoricProcessInstance> result = historyService
        .createHistoricProcessInstanceQuery()
        .activityIdIn(userTask1)
        .list();

    // then
    assertThat(result).extracting("id")
        .containsExactlyInAnyOrder(tasks.get(1).getProcessInstanceId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void shouldQueryWithActivityIdsWithMultipleIds() {
    // given
    String userTask1 = "userTask1";
    String userTask2 = "userTask2";
    deployment(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    taskService.complete(tasks.get(0).getId());

    // when
    List<HistoricProcessInstance> result = historyService
        .createHistoricProcessInstanceQuery()
        .activityIdIn(userTask1, userTask2)
        .list();

    // then
    assertThat(result).extracting("id")
        .containsExactlyInAnyOrder(processInstance.getId(), processInstance2.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void shouldQueryWithActivityIdsWhenDeletedCompetedInstancesExist() {
    // given
    String userTask1 = "userTask1";
    String userTask2 = "userTask2";
    deployment(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("Process");

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    taskService.complete(tasks.get(0).getId());

    ProcessInstance completedProcessInstance = runtimeService.startProcessInstanceByKey("Process");
    Task task = taskService.createTaskQuery().processInstanceId(completedProcessInstance.getId()).singleResult();
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().processInstanceId(completedProcessInstance.getId()).singleResult();
    taskService.complete(task.getId());


    ProcessInstance deletedProcessInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.deleteProcessInstance(deletedProcessInstance.getId(), "Testing");

    // when
    List<HistoricProcessInstance> result = historyService
        .createHistoricProcessInstanceQuery()
        .activityIdIn(userTask1, userTask2)
        .list();

    // then
    assertThat(result).extracting("id")
        .containsExactlyInAnyOrder(processInstance.getId(), processInstance2.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testHistoricProcessInstanceQueryActivityIdInWithIncident.bpmn"})
  public void shouldQueryQueryWithActivityIdsWithFailingActivity() {
    // given
    String piOne = runtimeService.startProcessInstanceByKey("failingProcess").getId();
    testHelper.executeAvailableJobs();

    String piTwo = runtimeService.startProcessInstanceByKey("failingProcess").getId();

    // assume
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(2);
    assertThat(historyService.createHistoricProcessInstanceQuery().list()).hasSize(2);

    assertThat(historyService.createHistoricProcessInstanceQuery().withIncidents().count()).isEqualTo(1);
    assertThat(historyService.createHistoricProcessInstanceQuery().withIncidents().list()).hasSize(1);

    // when
    List<HistoricProcessInstance> result = historyService
        .createHistoricProcessInstanceQuery()
        .activityIdIn("serviceTask")
        .list();

    // then
    assertThat(result).extracting("id").containsExactlyInAnyOrder(piOne, piTwo);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  public void shouldNotRetrieveInstanceWhenQueryByActivityIdInWithFailingSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testHelper.executeAvailableJobs();

    // when
    HistoricProcessInstance historicPI =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess").singleResult();

    // then
    assertThat(historicPI).isNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  public void shouldQueryByActivityIdInWithFailingSubServiceTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testHelper.executeAvailableJobs();

    // when
    HistoricProcessInstance historicPI =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("serviceTask").singleResult();

    // then
    assertThat(historicPI).isNotNull();
    assertThat(historicPI.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml"})
  public void shouldNotReturnInstanceWhenQueryByActivityIdInWithSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
    // when
    HistoricProcessInstance historicPI =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess").singleResult();

    // then
    assertThat(historicPI).isNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml"})
  public void shouldQueryByActivityIdInWithActivityIdOfSubServiceTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
    // when
    HistoricProcessInstance historicPI =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("innerTask").singleResult();

    // then
    assertThat(historicPI).isNotNull();
    assertThat(historicPI.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.failingSubprocessWithAsyncBeforeTask.bpmn20.xml"})
  public void shouldQueryByActivityIdInWithMultipleScopeAndIncident() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("failingSubProcess");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.complete(task.getId());

    testHelper.executeAvailableJobs();

    // when
    List<HistoricProcessInstance> queryByInnerServiceActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("innerServiceTask").list();
    List<HistoricProcessInstance>  queryBySubProcessActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess").list();
    List<HistoricProcessInstance>  queryByOuterProcessActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("outerTask").list();
    List<HistoricProcessInstance> queryByOuterAndInnedActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("innerServiceTask", "outerTask").list();

    // then
    assertThat(queryByInnerServiceActivityId).extracting("id")
          .containsExactlyInAnyOrder(processInstance.getId());
    assertThat(queryBySubProcessActivityId).isEmpty();
    assertThat(queryByOuterProcessActivityId).extracting("id")
          .containsExactlyInAnyOrder(processInstance2.getId());
    assertThat(queryByOuterAndInnedActivityId).extracting("id")
          .containsExactlyInAnyOrder(processInstance.getId(), processInstance2.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocessWithAsyncBeforeTask.bpmn20.xml"})
  public void shouldQueryByActivityIdInWithMultipleScope() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("failingSubProcess");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.complete(task.getId());

    // when
    List<HistoricProcessInstance> queryByInnerServiceActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("innerTask").list();
    List<HistoricProcessInstance>  queryBySubProcessActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("subProcess").list();
    List<HistoricProcessInstance>  queryByOuterProcessActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("outerTask").list();
    List<HistoricProcessInstance> queryByOuterAndInnedActivityId =
        historyService.createHistoricProcessInstanceQuery().activityIdIn("innerTask", "outerTask").list();

    // then
    assertThat(queryByInnerServiceActivityId).extracting("id")
          .containsExactlyInAnyOrder(processInstance.getId());
    assertThat(queryBySubProcessActivityId).isEmpty();
    assertThat(queryByOuterProcessActivityId).extracting("id")
          .containsExactlyInAnyOrder(processInstance2.getId());
    assertThat(queryByOuterAndInnedActivityId).extracting("id")
          .containsExactlyInAnyOrder(processInstance.getId(), processInstance2.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void shouldQueryByActivityIdWhereIncidentOccurred() {
    // given
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .serviceTask("theTask")
        .operatonAsyncBefore()
        .operatonClass(ChangeVariablesDelegate.class)
      .serviceTask("theTask2").operatonClass(ChangeVariablesDelegate.class)
      .serviceTask("theTask3").operatonClass(FailingDelegate.class)
      .endEvent()
      .done());

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", true));
    JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();

    // when: incident is raised
    for(int i = 0; i<3; i++) {
      var jobId = job.getId();
      try {
        managementService.executeJob(jobId);
        fail("Exception expected");
      } catch (Exception e) {
        // exception expected
      }
    }

    // then
    HistoricProcessInstance theTask = historyService.createHistoricProcessInstanceQuery()
        .activityIdIn("theTask")
        .singleResult();
    assertThat(theTask).isNotNull();
    HistoricProcessInstance theTask3 = historyService.createHistoricProcessInstanceQuery()
        .activityIdIn("theTask3")
        .singleResult();
    assertThat(theTask3).isNull();
  }

  @Test
  public void testHistoricProcInstQueryWithActiveActivityIdsNull() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.activeActivityIdIn((String[]) null);
      fail("exception expected");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("activity ids is null");
    }
  }

  @Test
  public void testHistoricProcInstQueryWithActiveActivityIdsContainNull() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.activeActivityIdIn(null, "1");
      fail("exception expected");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("activity ids contains null");
    }
  }

  @Test
  public void testQueryByActiveActivityIdInAndProcessDefinitionKey() {
    // given
    deployment(ProcessModels.ONE_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    // when
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .processDefinitionKey("Process")
      .activeActivityIdIn("userTask")
      .singleResult();

    // then
    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  public void testQueryByExecutedActivityIdInAndProcessDefinitionKey() {
    // given
    deployment(ProcessModels.ONE_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .processDefinitionKey("Process")
      .executedActivityIdIn("userTask")
      .singleResult();

    // then
    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testQueryWithRootIncidents() {
    // given
    deployment("org/operaton/bpm/engine/test/history/HistoricProcessInstanceTest.testQueryWithRootIncidents.bpmn20.xml");
    deployment(CallActivityModels.oneBpmnCallActivityProcess("Process_1"));

    runtimeService.startProcessInstanceByKey("Process");
    ProcessInstance calledProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("Process_1").singleResult();
    testHelper.executeAvailableJobs();

    // when
    List<HistoricProcessInstance> historicProcInstances = historyService.createHistoricProcessInstanceQuery().withRootIncidents().list();

    // then
    assertThat(calledProcessInstance).isNotNull();
    assertThat(historicProcInstances).hasSize(1);
    assertThat(historicProcInstances.get(0).getId()).isEqualTo(calledProcessInstance.getId());
  }

  @Test
  public void testQueryWithProcessDefinitionKeyIn() {
    // given
    deployment(ProcessModels.ONE_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY);
    runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY);
    runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY);

    deployment(modify(ProcessModels.TWO_TASKS_PROCESS).changeElementId(ProcessModels.PROCESS_KEY, "ONE_TASKS_PROCESS"));
    runtimeService.startProcessInstanceByKey("ONE_TASKS_PROCESS");
    runtimeService.startProcessInstanceByKey("ONE_TASKS_PROCESS");

    deployment(modify(ProcessModels.TWO_TASKS_PROCESS).changeElementId(ProcessModels.PROCESS_KEY, "TWO_TASKS_PROCESS"));
    runtimeService.startProcessInstanceByKey("TWO_TASKS_PROCESS");
    runtimeService.startProcessInstanceByKey("TWO_TASKS_PROCESS");
    runtimeService.startProcessInstanceByKey("TWO_TASKS_PROCESS");
    runtimeService.startProcessInstanceByKey("TWO_TASKS_PROCESS");

    // assume
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(9L);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
      .processDefinitionKeyIn("ONE_TASKS_PROCESS", "TWO_TASKS_PROCESS");

    // then
    assertThat(query.count()).isEqualTo(6L);
    assertThat(query.list()).hasSize(6);
  }

  @Test
  public void testQueryByNonExistingProcessDefinitionKeyIn() {
    // given
    deployment(ProcessModels.ONE_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
      .processDefinitionKeyIn("not-existing-key");

    // then
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  public void testQueryByOneInvalidProcessDefinitionKeyIn() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      // when
      historicProcessInstanceQuery.processDefinitionKeyIn((String) null);
      fail("Exception expected");
    } catch(ProcessEngineException expected) {
      // then Exception is expected
    }
  }

  @Test
  public void testQueryByMultipleInvalidProcessDefinitionKeyIn() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      // when
      historicProcessInstanceQuery.processDefinitionKeyIn(ProcessModels.PROCESS_KEY, null);
      fail("Exception expected");
    } catch(ProcessEngineException expected) {
      // then Exception is expected
    }
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  public void shouldQueryProcessInstancesWithIncidentIdIn() {
    // GIVEN
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("failingProcess");
    runtimeService.startProcessInstanceByKey("failingProcess");
    List<String> queriedProcessInstances = Arrays.asList(processInstance.getId(), processInstance2.getId());

    testHelper.executeAvailableJobs();
    Incident incident = runtimeService.createIncidentQuery().processInstanceId(queriedProcessInstances.get(0)).singleResult();
    Incident incident2 = runtimeService.createIncidentQuery().processInstanceId(queriedProcessInstances.get(1)).singleResult();

    // WHEN
    List<HistoricProcessInstance> processInstanceList =
        historyService.createHistoricProcessInstanceQuery().incidentIdIn(incident.getId(), incident2.getId()).list();

    // THEN
    assertThat(processInstanceList).hasSize(2);
    assertThat(queriedProcessInstances)
        .containsExactlyInAnyOrderElementsOf(
            processInstanceList.stream()
                .map(HistoricProcessInstance::getId)
                .toList());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingProcessAfterUserTaskCreateOneIncident.bpmn20.xml"})
  public void shouldOnlyQueryProcessInstancesWithIncidentIdIn() {
    // GIVEN
    ProcessInstance processWithIncident1 = runtimeService.startProcessInstanceByKey("failingProcess");
    ProcessInstance processWithIncident2 = runtimeService.startProcessInstanceByKey("failingProcess");

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    ProcessInstance processWithoutIncident = runtimeService.startProcessInstanceByKey("failingProcess");

    List<String> queriedProcessInstances = Arrays.asList(processWithIncident1.getId(), processWithIncident2.getId());

    testHelper.executeAvailableJobs();
    Incident incident = runtimeService.createIncidentQuery().processInstanceId(queriedProcessInstances.get(0)).singleResult();
    Incident incident2 = runtimeService.createIncidentQuery().processInstanceId(queriedProcessInstances.get(1)).singleResult();

    // WHEN
    List<HistoricProcessInstance> processInstanceList =
        historyService.createHistoricProcessInstanceQuery().incidentIdIn(incident.getId(), incident2.getId()).list();

    // THEN
    assertThat(processInstanceList).hasSize(2);
    assertThat(queriedProcessInstances)
        .containsExactlyInAnyOrderElementsOf(
            processInstanceList.stream()
                .map(HistoricProcessInstance::getId)
                .toList());
  }

  @Test
  public void shouldFailWhenQueryWithNullIncidentIdIn() {
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    try {
      historicProcessInstanceQuery.incidentIdIn(null);
      fail("incidentMessage with null value is not allowed");
    } catch( NullValueException nex ) {
      assertThat(nex.getMessage()).isEqualTo("incidentIds is null");
    }
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  public void shouldQueryByIncidentIdInSubProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    // when
    HistoricProcessInstance historicPI =
        historyService.createHistoricProcessInstanceQuery().incidentIdIn(incident.getId()).singleResult();

    // then
    assertThat(historicPI).isNotNull();
    assertThat(historicPI.getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneAsyncTaskProcess.bpmn20.xml"})
  public void testShouldStoreHistoricProcessInstanceVariableOnAsyncBefore() {
    // given definition with asyncBefore startEvent

    // when trigger process instance with variables
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.createVariables().putValue("foo", "bar"));

    // then
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().variableName("foo").singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneAsyncTaskProcess.bpmn20.xml"})
  public void testShouldStoreInitialHistoricProcessInstanceVariableOnAsyncBefore() {
    // given definition with asyncBefore startEvent

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.createVariables().putValue("foo", "bar"));

    runtimeService.setVariable(processInstance.getId(), "goo", "car");

    // when
    executeJob(managementService.createJobQuery().singleResult());

    // then
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().variableName("foo").singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getValue()).isEqualTo("bar");
    historicVariable = historyService.createHistoricVariableInstanceQuery().variableName("goo").singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getValue()).isEqualTo("car");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneAsyncTaskProcess.bpmn20.xml"})
  public void testShouldSetVariableBeforeAsyncBefore() {
    // given definition with asyncBefore startEvent

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    runtimeService.setVariable(processInstance.getId(), "goo", "car");

    // when
    executeJob(managementService.createJobQuery().singleResult());

    // then
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().variableName("goo").singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getValue()).isEqualTo("car");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_1() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo"))
        .getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo"))
        .getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .variableValueEquals("bar", "foo")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_2() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo"))
        .getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo"))
        .getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .variableValueEquals("foo", "bar")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_3() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo"))
        .getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo"))
        .getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_4() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar"))
        .getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("bar", "foo"))
        .getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_5() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar"))
        .getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("bar", "foo"))
        .getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .variableValueEquals("foo", "bar")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_6() {
    // GIVEN
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.putValue("foo", "bar"));
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.putValue("bar", "foo"));

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .variableValueEquals("bar", "foo")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_7() {
    // GIVEN
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.putValue("foo", "foo"));
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.putValue("bar", "foo"));

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .variableValueEquals("foo", "foo")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_8() {
    // GIVEN
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.putValue("foo", "foo"));
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.putValue("foo", "bar"));

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals("foo", "bar")
        .variableValueEquals("foo", "foo")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_9() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess", "a-business-key").getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess", "another-business-key").getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKeyIn("a-business-key", "another-business-key")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_10() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess", "a-business-key").getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKeyIn("a-business-key", "another-business-key")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_11() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess", "a-business-key").getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess", "a-business-key").getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKeyIn("a-business-key")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_12() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess", "a-business-key").getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess", "a-business-key").getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKeyIn("a-business-key", "another-business-key")
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

    @Test
    @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
    public void shouldExcludeByProcessInstanceIdNotIn() {
        // GIVEN
        String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();
        String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();

        // WHEN
        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery().list();
        List<HistoricProcessInstance> excludedFirst = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIdNotIn(processInstanceIdOne).list();
        List<HistoricProcessInstance> excludedAll = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIdNotIn(processInstanceIdOne, processInstanceIdTwo).list();

        // THEN
        assertThat(processInstances)
                .extracting("processInstanceId")
                .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
        assertThat(excludedFirst)
                .extracting("processInstanceId")
                .containsExactly(processInstanceIdTwo);
        assertThat(excludedAll)
                .extracting("processInstanceId")
                .isEmpty();
    }

    @Test
    @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testWithNonExistentProcessInstanceIdNotIn() {
        // GIVEN
        String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();
        String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();

        String nonExistentProcessInstanceId = "ThisIsAFake";

        // WHEN
        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery().list();
        List<HistoricProcessInstance> excludedNonExistent = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIdNotIn(nonExistentProcessInstanceId).list();

        // THEN
        assertThat(processInstances)
                .extracting("processInstanceId")
                .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
        assertThat(excludedNonExistent)
                .extracting("processInstanceId")
                .containsExactly(processInstanceIdOne, processInstanceIdTwo);
    }

    @Test
    @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testQueryByOneInvalidProcessInstanceIdNotIn() {
        try {
            // when
            historyService.createHistoricProcessInstanceQuery()
                    .processInstanceIdNotIn((String) null);
            fail();
        } catch(ProcessEngineException expected) {
            // then Exception is expected
        }
    }

    @Test
    @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testExcludingProcessInstanceAndProcessInstanceIdNotIn() {
        // GIVEN
        String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();
        runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();

        // WHEN
        long count = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceIdOne)
                .processInstanceIdNotIn(processInstanceIdOne).count();

        // THEN making a query that has contradicting conditions should succeed
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testExcludingProcessInstanceIdsAndProcessInstanceIdNotIn() {
        // GIVEN
        String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();
        String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();
        runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();

        // WHEN
        long count = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(new HashSet<>(Arrays.asList(processInstanceIdOne, processInstanceIdTwo)))
                .processInstanceIdNotIn(processInstanceIdOne, processInstanceIdTwo).count();

        // THEN making a query that has contradicting conditions should succeed
        assertThat(count).isEqualTo(0L);
    }


    protected void deployment(String... resources) {
    testHelper.deploy(resources);
  }

  protected void deployment(BpmnModelInstance... modelInstances) {
    testHelper.deploy(modelInstances);
  }

  protected void executeJob(Job job) {
    while (job != null && job.getRetries() > 0) {
      try {
        managementService.executeJob(job.getId());
      }
      catch (Exception e) {
        // ignore
      }

      job = managementService.createJobQuery().jobId(job.getId()).singleResult();
    }
  }

  protected void executeJobForProcessInstance(String processInstanceId) {
    Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
    managementService.executeJob(job.getId());
  }

}
