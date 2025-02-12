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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.event.HistoricActivityInstanceEventEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Marcel Wieczorek
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricActivityInstanceTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testHistoricActivityInstanceNoop() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("noop").singleResult();

    assertThat(historicActivityInstance.getActivityId()).isEqualTo("noop");
    assertThat(historicActivityInstance.getActivityType()).isEqualTo("serviceTask");
    assertNotNull(historicActivityInstance.getProcessDefinitionId());
    assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicActivityInstance.getExecutionId()).isEqualTo(processInstance.getId());
    assertNotNull(historicActivityInstance.getStartTime());
    assertNotNull(historicActivityInstance.getEndTime());
    assertTrue(historicActivityInstance.getDurationInMillis() >= 0);
  }

  @Deployment
  @Test
  public void testHistoricActivityInstanceReceive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveProcess");

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult();

    assertThat(historicActivityInstance.getActivityId()).isEqualTo("receive");
    assertThat(historicActivityInstance.getActivityType()).isEqualTo("receiveTask");
    assertNull(historicActivityInstance.getEndTime());
    assertNull(historicActivityInstance.getDurationInMillis());
    assertNotNull(historicActivityInstance.getProcessDefinitionId());
    assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicActivityInstance.getExecutionId()).isEqualTo(processInstance.getId());
    assertNotNull(historicActivityInstance.getStartTime());

    // move clock by 1 second
    Date now = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(new Date(now.getTime() + 1000));

    runtimeService.signal(processInstance.getId());

    historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult();

    assertThat(historicActivityInstance.getActivityId()).isEqualTo("receive");
    assertThat(historicActivityInstance.getActivityType()).isEqualTo("receiveTask");
    assertNotNull(historicActivityInstance.getEndTime());
    assertNotNull(historicActivityInstance.getProcessDefinitionId());
    assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicActivityInstance.getExecutionId()).isEqualTo(processInstance.getId());
    assertNotNull(historicActivityInstance.getStartTime());
    assertTrue(historicActivityInstance.getDurationInMillis() >= 1000);
    assertTrue(((HistoricActivityInstanceEventEntity)historicActivityInstance).getDurationRaw() >= 1000);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testHistoricActivityInstanceReceive.bpmn20.xml" })
  @Test
  public void testLongRunningHistoricActivityInstanceReceive() {
    final long ONE_YEAR = 1000 * 60 * 60 * 24 * 365;

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    ClockUtil.setCurrentTime(cal.getTime());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveProcess");

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult();

    assertThat(historicActivityInstance.getActivityId()).isEqualTo("receive");
    assertThat(historicActivityInstance.getActivityType()).isEqualTo("receiveTask");
    assertNull(historicActivityInstance.getEndTime());
    assertNull(historicActivityInstance.getDurationInMillis());
    assertNotNull(historicActivityInstance.getProcessDefinitionId());
    assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicActivityInstance.getExecutionId()).isEqualTo(processInstance.getId());
    assertNotNull(historicActivityInstance.getStartTime());

    // move clock by 1 year
    cal.add(Calendar.YEAR, 1);
    ClockUtil.setCurrentTime(cal.getTime());

    runtimeService.signal(processInstance.getId());

    historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult();

    assertThat(historicActivityInstance.getActivityId()).isEqualTo("receive");
    assertThat(historicActivityInstance.getActivityType()).isEqualTo("receiveTask");
    assertNotNull(historicActivityInstance.getEndTime());
    assertNotNull(historicActivityInstance.getProcessDefinitionId());
    assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicActivityInstance.getExecutionId()).isEqualTo(processInstance.getId());
    assertNotNull(historicActivityInstance.getStartTime());
    assertTrue(historicActivityInstance.getDurationInMillis() >= ONE_YEAR);
    assertTrue(((HistoricActivityInstanceEventEntity)historicActivityInstance).getDurationRaw() >= ONE_YEAR);
  }

  @Deployment
  @Test
  public void testHistoricActivityInstanceQuery() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");

    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("nonExistingActivityId").list()).hasSize(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("noop").list()).hasSize(1);

    assertThat(historyService.createHistoricActivityInstanceQuery().activityType("nonExistingActivityType").list()).hasSize(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityType("serviceTask").list()).hasSize(1);

    assertThat(historyService.createHistoricActivityInstanceQuery().activityName("nonExistingActivityName").list()).hasSize(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityName("No operation").list()).hasSize(1);

    assertThat(historyService.createHistoricActivityInstanceQuery().activityNameLike("operation").list()).hasSize(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityNameLike("%operation").list()).hasSize(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityNameLike("%oper%").list()).hasSize(1);

    assertThat(historyService.createHistoricActivityInstanceQuery().taskAssignee("nonExistingAssignee").list()).hasSize(0);

    assertThat(historyService.createHistoricActivityInstanceQuery().executionId("nonExistingExecutionId").list()).hasSize(0);

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      assertThat(historyService.createHistoricActivityInstanceQuery().executionId(processInstance.getId()).list()).hasSize(3);
    } else {
      assertThat(historyService.createHistoricActivityInstanceQuery().executionId(processInstance.getId()).list()).hasSize(0);
    }

    assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId("nonExistingProcessInstanceId").list()).hasSize(0);

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(3);
    } else {
      assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
    }

    assertThat(historyService.createHistoricActivityInstanceQuery().processDefinitionId("nonExistingProcessDefinitionId").list()).hasSize(0);

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      assertThat(historyService.createHistoricActivityInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list()).hasSize(3);
    } else {
      assertThat(historyService.createHistoricActivityInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list()).hasSize(0);
    }

    assertThat(historyService.createHistoricActivityInstanceQuery().unfinished().list()).hasSize(0);

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      assertThat(historyService.createHistoricActivityInstanceQuery().finished().list()).hasSize(3);
    } else {
      assertThat(historyService.createHistoricActivityInstanceQuery().finished().list()).hasSize(0);
    }

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().list().get(0);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).list()).hasSize(1);
    }
  }

  @Deployment
  @Test
  public void testHistoricActivityInstanceForEventsQuery() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("eventProcess");
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    runtimeService.signalEventReceived("signal");
    testRule.assertProcessEnded(pi.getId());

    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("noop").list()).hasSize(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("userTask").list()).hasSize(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("intermediate-event").list()).hasSize(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("start").list()).hasSize(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("end").list()).hasSize(1);

    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("boundaryEvent").list()).hasSize(1);

    HistoricActivityInstance intermediateEvent = historyService.createHistoricActivityInstanceQuery().activityId("intermediate-event").singleResult();
    assertNotNull(intermediateEvent.getStartTime());
    assertNotNull(intermediateEvent.getEndTime());

    HistoricActivityInstance startEvent = historyService.createHistoricActivityInstanceQuery().activityId("start").singleResult();
    assertNotNull(startEvent.getStartTime());
    assertNotNull(startEvent.getEndTime());

    HistoricActivityInstance endEvent = historyService.createHistoricActivityInstanceQuery().activityId("end").singleResult();
    assertNotNull(endEvent.getStartTime());
    assertNotNull(endEvent.getEndTime());
  }

  @Deployment
  @Test
  public void testHistoricActivityInstanceProperties() {
    // Start process instance
    runtimeService.startProcessInstanceByKey("taskAssigneeProcess");

    // Get task list
    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(historicActivityInstance.getTaskId()).isEqualTo(task.getId());
    assertThat(historicActivityInstance.getAssignee()).isEqualTo("kermit");

    // change assignee of the task
    taskService.setAssignee(task.getId(), "gonzo");
    task = taskService.createTaskQuery().singleResult();

    historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
    assertThat(historicActivityInstance.getAssignee()).isEqualTo("gonzo");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/history/calledProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
  @Test
  public void testHistoricActivityInstanceCalledProcessId() {
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("callSubProcess").singleResult();

    HistoricProcessInstance oldInstance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("calledProcess").singleResult();

    assertThat(historicActivityInstance.getCalledProcessInstanceId()).isEqualTo(oldInstance.getId());
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/history/calledProcessWaiting.bpmn20.xml",
  "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
  @Test
  public void testHistoricActivityInstanceCalledProcessIdWithWaitState() {
    // given
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    ProcessInstance calledInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("calledProcess").singleResult();
    HistoricActivityInstanceQuery activityQuery = historyService.createHistoricActivityInstanceQuery().activityId("callSubProcess");

    // assume
    assertThat(activityQuery.singleResult().getCalledProcessInstanceId()).isEqualTo(calledInstance.getId());

    // when
    taskService.complete(taskService.createTaskQuery().processInstanceId(calledInstance.getId()).singleResult().getId());

    // then
    assertThat(activityQuery.singleResult().getCalledProcessInstanceId()).isEqualTo(calledInstance.getId());
  }

  @Deployment
  @Test
  public void testSorting() {
    runtimeService.startProcessInstanceByKey("process");

    int expectedActivityInstances = -1;
    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      expectedActivityInstances = 2;
    } else {
      expectedActivityInstances = 0;
    }

    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().asc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().asc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().asc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().asc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().asc().list()).hasSize(expectedActivityInstances);

    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().desc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().desc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().desc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().desc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().desc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().desc().list()).hasSize(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().desc().list()).hasSize(expectedActivityInstances);

    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().asc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().asc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().asc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().asc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(expectedActivityInstances);

    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().desc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().desc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().desc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().desc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().desc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().desc().count()).isEqualTo(expectedActivityInstances);
    assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().desc().count()).isEqualTo(expectedActivityInstances);
  }

  @Test
  public void testInvalidSorting() {
    var historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration();
    try {
      historicActivityInstanceQuery.list();
      fail();
    } catch (ProcessEngineException e) {

    }

    try {
      historicActivityInstanceQuery.list();
      fail();
    } catch (ProcessEngineException e) {

    }

    try {
      historicActivityInstanceQuery.list();
      fail();
    } catch (ProcessEngineException e) {

    }
  }
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testHistoricActivityInstanceQueryStartFinishAfterBefore() {
    Calendar startTime = Calendar.getInstance();

    ClockUtil.setCurrentTime(startTime.getTime());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey123");

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    // Start/end dates
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedBefore(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedAfter(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedAfter(hourFromNow.getTime()).startedBefore(hourAgo.getTime()).count()).isEqualTo(0);

    // After finishing process
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourAgo.getTime()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourAgo.getTime()).finishedAfter(hourFromNow.getTime()).count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testHistoricActivityInstanceQueryByCompleteScope() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    List<Task> tasks = taskService.createTaskQuery().list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery().completeScope();

    assertThat(query.count()).isEqualTo(3);

    List<HistoricActivityInstance> instances = query.list();

    for (HistoricActivityInstance instance : instances) {
      if (!instance.getActivityId().equals("innerEnd") && !instance.getActivityId().equals("end1") && !instance.getActivityId().equals("end2")) {
        fail("Unexpected instance with activity id " + instance.getActivityId() + " found.");
      }
    }

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testHistoricActivityInstanceQueryByCompleteScope.bpmn")
  @Test
  public void testHistoricActivityInstanceQueryByCanceled() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery().canceled();

    assertThat(query.count()).isEqualTo(3);

    List<HistoricActivityInstance> instances = query.list();

    for (HistoricActivityInstance instance : instances) {
      if (!instance.getActivityId().equals("subprocess") && !instance.getActivityId().equals("userTask1") && !instance.getActivityId().equals("userTask2")) {
        fail("Unexpected instance with activity id " + instance.getActivityId() + " found.");
      }
    }

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testHistoricActivityInstanceQueryByCompleteScopeAndCanceled() {
    var historicActivityInstanceQuery = historyService
          .createHistoricActivityInstanceQuery()
          .completeScope();
    try {
      historicActivityInstanceQuery.canceled();
      fail("It should not be possible to query by completeScope and canceled.");
    } catch (ProcessEngineException e) {
      // exception expected
    }
  }

  /**
   * https://app.camunda.com/jira/browse/CAM-1537
   */
  @Deployment
  @Test
  public void testHistoricActivityInstanceGatewayEndTimes() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("gatewayEndTimes");

    TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
    List<Task> tasks = query.list();
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // process instance should have finished
    assertNotNull(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult().getEndTime());
    // gateways should have end timestamps
    assertNotNull(historyService.createHistoricActivityInstanceQuery().activityId("Gateway_0").singleResult().getEndTime());

    // there exists two historic activity instances for "Gateway_1" (parallel join)
    HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("Gateway_1");

    assertThat(historicActivityInstanceQuery.count()).isEqualTo(2);
    // they should have an end timestamp
    assertNotNull(historicActivityInstanceQuery.list().get(0).getEndTime());
    assertNotNull(historicActivityInstanceQuery.list().get(1).getEndTime());
  }

  @Deployment
  @Test
  public void testHistoricActivityInstanceTimerEvent() {
    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    Job timer = jobQuery.singleResult();
    managementService.executeJob(timer.getId());

    TaskQuery taskQuery = taskService.createTaskQuery();
    Task task = taskQuery.singleResult();

    assertThat(task.getName()).isEqualTo("afterTimer");

    HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("gw1");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(1);
    assertNotNull(historicActivityInstanceQuery.singleResult().getEndTime());

    historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("timerEvent");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(1);
    assertNotNull(historicActivityInstanceQuery.singleResult().getEndTime());
    assertThat(historicActivityInstanceQuery.singleResult().getActivityType()).isEqualTo("intermediateTimer");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testHistoricActivityInstanceTimerEvent.bpmn20.xml"})
  @Test
  public void testHistoricActivityInstanceMessageEvent() {
    runtimeService.startProcessInstanceByKey("catchSignal");

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    EventSubscriptionQuery eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery();
    assertThat(eventSubscriptionQuery.count()).isEqualTo(1);

    runtimeService.correlateMessage("newInvoice");

    TaskQuery taskQuery = taskService.createTaskQuery();
    Task task = taskQuery.singleResult();

    assertThat(task.getName()).isEqualTo("afterMessage");

    HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("gw1");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(1);
    assertNotNull(historicActivityInstanceQuery.singleResult().getEndTime());

    historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("messageEvent");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(1);
    assertNotNull(historicActivityInstanceQuery.singleResult().getEndTime());
    assertThat(historicActivityInstanceQuery.singleResult().getActivityType()).isEqualTo("intermediateMessageCatch");
  }

  @Deployment
  @Test
  public void testUserTaskStillRunning() {
    runtimeService.startProcessInstanceByKey("nonInterruptingEvent");

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    managementService.executeJob(jobQuery.singleResult().getId());

    HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("userTask");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(1);
    assertNull(historicActivityInstanceQuery.singleResult().getEndTime());

    historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("end1");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(0);

    historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("timer");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(1);
    assertNotNull(historicActivityInstanceQuery.singleResult().getEndTime());

    historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery().activityId("end2");
    assertThat(historicActivityInstanceQuery.count()).isEqualTo(1);
    assertNotNull(historicActivityInstanceQuery.singleResult().getEndTime());
  }

  @Deployment
  @Test
  public void testInterruptingBoundaryMessageEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newMessage").singleResult();

    runtimeService.messageEventReceived("newMessage", execution.getId());

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("message");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("boundaryMessage");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testNonInterruptingBoundaryMessageEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newMessage").singleResult();

    runtimeService.messageEventReceived("newMessage", execution.getId());

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("message");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("boundaryMessage");

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testInterruptingBoundarySignalEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Execution execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("newSignal").singleResult();

    runtimeService.signalEventReceived("newSignal", execution.getId());

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("signal");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("boundarySignal");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testNonInterruptingBoundarySignalEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Execution execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("newSignal").singleResult();

    runtimeService.signalEventReceived("newSignal", execution.getId());

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("signal");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("boundarySignal");

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testInterruptingBoundaryTimerEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);

    managementService.executeJob(job.getId());

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("timer");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("boundaryTimer");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testNonInterruptingBoundaryTimerEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);

    managementService.executeJob(job.getId());

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("timer");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("boundaryTimer");

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testBoundaryErrorEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("error");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("boundaryError");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testBoundaryCancelEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("catchCancel");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());
    assertThat(query.singleResult().getActivityType()).isEqualTo("cancelBoundaryCatch");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testBoundaryCompensateEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    // the compensation boundary event should not appear in history!
    query.activityId("compensate");
    assertThat(query.count()).isEqualTo(0);

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testBoundaryCompensateEvent.bpmn20.xml")
  @Test
  public void testCompensationServiceTaskHasEndTime() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("compensationServiceTask");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testBoundaryCancelEvent.bpmn20.xml")
  @Test
  public void testTransaction() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("transaction");
    assertThat(query.count()).isEqualTo(1);
    assertNotNull(query.singleResult().getEndTime());

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testScopeActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    query.activityId("userTask");
    assertThat(query.count()).isEqualTo(1);

    HistoricActivityInstance historicActivityInstance = query.singleResult();

    assertThat(historicActivityInstance.getParentActivityInstanceId()).isEqualTo(pi.getId());

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testMultiInstanceScopeActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

    HistoricActivityInstance miBodyInstance = query.activityId("userTask#multiInstanceBody").singleResult();

    query.activityId("userTask");
    assertThat(query.count()).isEqualTo(5);


    List<HistoricActivityInstance> result = query.list();

    for (HistoricActivityInstance instance : result) {
      assertThat(instance.getParentActivityInstanceId()).isEqualTo(miBodyInstance.getId());
    }

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testMultiInstanceReceiveActivity() {
    runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();
    HistoricActivityInstance miBodyInstance = query.activityId("receiveTask#multiInstanceBody").singleResult();

    query.activityId("receiveTask");
    assertThat(query.count()).isEqualTo(5);

    List<HistoricActivityInstance> result = query.list();

    for (HistoricActivityInstance instance : result) {
      assertThat(instance.getParentActivityInstanceId()).isEqualTo(miBodyInstance.getId());
    }

  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testEvents.bpmn")
  @Test
  public void testIntermediateCatchEventTypes() {
    HistoricActivityInstanceQuery query = startEventTestProcess("");

    query.activityId("intermediateSignalCatchEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("intermediateSignalCatch");

    query.activityId("intermediateMessageCatchEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("intermediateMessageCatch");

    query.activityId("intermediateTimerCatchEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("intermediateTimer");
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testEvents.bpmn")
  @Test
  public void testIntermediateThrowEventTypes() {
    HistoricActivityInstanceQuery query = startEventTestProcess("");

    query.activityId("intermediateSignalThrowEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("intermediateSignalThrow");

    query.activityId("intermediateMessageThrowEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("intermediateMessageThrowEvent");

    query.activityId("intermediateNoneThrowEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("intermediateNoneThrowEvent");

    query.activityId("intermediateCompensationThrowEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("intermediateCompensationThrowEvent");
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testEvents.bpmn")
  @Test
  public void testStartEventTypes() {
    HistoricActivityInstanceQuery query = startEventTestProcess("");

    query.activityId("timerStartEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("startTimerEvent");

    query.activityId("noneStartEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("startEvent");

    query = startEventTestProcess("CAM-2365");
    query.activityId("messageStartEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("messageStartEvent");
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testEvents.bpmn")
  @Test
  public void testEndEventTypes() {
    HistoricActivityInstanceQuery query = startEventTestProcess("");

    query.activityId("cancellationEndEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("cancelEndEvent");

    query.activityId("messageEndEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("messageEndEvent");

    query.activityId("errorEndEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("errorEndEvent");

    query.activityId("signalEndEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("signalEndEvent");

    query.activityId("terminationEndEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("terminateEndEvent");

    query.activityId("noneEndEvent");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getActivityType()).isEqualTo("noneEndEvent");
  }

  private HistoricActivityInstanceQuery startEventTestProcess(String message) {
    if(message.equals("")) {
      runtimeService.startProcessInstanceByKey("testEvents");
    } else {
      runtimeService.startProcessInstanceByMessage("CAM-2365");
    }

    return historyService.createHistoricActivityInstanceQuery();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.startEventTypesForEventSubprocess.bpmn20.xml")
  @Test
  public void testMessageEventSubprocess() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("shouldThrowError", false);
    runtimeService.startProcessInstanceByKey("process", vars);

    runtimeService.correlateMessage("newMessage");

    HistoricActivityInstance historicActivity = historyService.createHistoricActivityInstanceQuery()
        .activityId("messageStartEvent").singleResult();

    assertThat(historicActivity.getActivityType()).isEqualTo("messageStartEvent");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.startEventTypesForEventSubprocess.bpmn20.xml")
  @Test
  public void testSignalEventSubprocess() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("shouldThrowError", false);
    runtimeService.startProcessInstanceByKey("process", vars);

    runtimeService.signalEventReceived("newSignal");

    HistoricActivityInstance historicActivity = historyService.createHistoricActivityInstanceQuery()
        .activityId("signalStartEvent").singleResult();

    assertThat(historicActivity.getActivityType()).isEqualTo("signalStartEvent");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.startEventTypesForEventSubprocess.bpmn20.xml")
  @Test
  public void testTimerEventSubprocess() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("shouldThrowError", false);
    runtimeService.startProcessInstanceByKey("process", vars);

    Job timerJob = managementService.createJobQuery().singleResult();
    managementService.executeJob(timerJob.getId());

    HistoricActivityInstance historicActivity = historyService.createHistoricActivityInstanceQuery()
        .activityId("timerStartEvent").singleResult();

    assertThat(historicActivity.getActivityType()).isEqualTo("startTimerEvent");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.startEventTypesForEventSubprocess.bpmn20.xml")
  @Test
  public void testErrorEventSubprocess() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("shouldThrowError", true);
    runtimeService.startProcessInstanceByKey("process", vars);

    HistoricActivityInstance historicActivity = historyService.createHistoricActivityInstanceQuery()
        .activityId("errorStartEvent").singleResult();

    assertThat(historicActivity.getActivityType()).isEqualTo("errorStartEvent");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testCaseCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  public void testCaseCallActivity() {
    runtimeService.startProcessInstanceByKey("process");

    String subCaseInstanceId = caseService
        .createCaseInstanceQuery()
        .singleResult()
        .getId();


    HistoricActivityInstance historicCallActivity = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("callActivity")
        .singleResult();

    assertThat(historicCallActivity.getCalledCaseInstanceId()).isEqualTo(subCaseInstanceId);
    assertNull(historicCallActivity.getEndTime());

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService.completeCaseExecution(humanTaskId);

    historicCallActivity = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("callActivity")
        .singleResult();

    assertThat(historicCallActivity.getCalledCaseInstanceId()).isEqualTo(subCaseInstanceId);
    assertNotNull(historicCallActivity.getEndTime());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testProcessDefinitionKeyProperty() {
    // given
    String key = "oneTaskProcess";
    String processInstanceId = runtimeService.startProcessInstanceByKey(key).getId();

    // when
    HistoricActivityInstance activityInstance = historyService
      .createHistoricActivityInstanceQuery()
      .processInstanceId(processInstanceId)
      .activityId("theTask")
      .singleResult();

    // then
    assertNotNull(activityInstance.getProcessDefinitionKey());
    assertThat(activityInstance.getProcessDefinitionKey()).isEqualTo(key);

  }

  @Deployment
  @Test
  public void testEndParallelJoin() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<HistoricActivityInstance> activityInstance = historyService
      .createHistoricActivityInstanceQuery()
      .processInstanceId(pi.getId())
      .activityId("parallelJoinEnd")
      .list();

    assertThat(activityInstance).hasSize(2);
    assertThat(pi.isEnded()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricActivityInstanceTest.testHistoricActivityInstanceProperties.bpmn20.xml"})
  @Test
  public void testAssigneeSavedWhenTaskSaved() {
    // given
    HistoricActivityInstanceQuery query = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("theTask");

    runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
    HistoricActivityInstance historicActivityInstance = query.singleResult();

    Task task = taskService.createTaskQuery().singleResult();

    // assume
    assertThat(historicActivityInstance.getAssignee()).isEqualTo("kermit");

    // when
    task.setAssignee("gonzo");
    taskService.saveTask(task);

    // then
    historicActivityInstance = query.singleResult();
    assertThat(historicActivityInstance.getAssignee()).isEqualTo("gonzo");
  }

}
