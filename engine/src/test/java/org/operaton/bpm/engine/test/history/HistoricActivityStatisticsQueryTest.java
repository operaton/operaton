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
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricActivityStatistics;
import org.operaton.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 *
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricActivityStatisticsQueryTest extends PluggableProcessEngineTest {

  private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testNoRunningProcessInstances() {
    String processDefinitionId = getProcessDefinitionId();

    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(0);
    assertThat(statistics).hasSize(0);
  }

  @Deployment
  @Test
  public void testSingleTask() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(1);
    assertThat(statistics).hasSize(1);

    HistoricActivityStatistics statistic = statistics.get(0);

    assertThat(statistic.getId()).isEqualTo("task");
    assertThat(statistic.getInstances()).isEqualTo(5);

    completeProcessInstances();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testFinishedProcessInstances() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    completeProcessInstances();

    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(0);
    assertThat(statistics).hasSize(0);
  }

  @Deployment
  @Test
  public void testMultipleRunningTasks() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .orderByActivityId()
        .asc();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(4);
    assertThat(statistics).hasSize(4);

    // innerTask
    HistoricActivityStatistics innerTask = statistics.get(0);

    assertThat(innerTask.getId()).isEqualTo("innerTask");
    assertThat(innerTask.getInstances()).isEqualTo(25);

    // subprocess
    HistoricActivityStatistics subProcess = statistics.get(1);

    assertThat(subProcess.getId()).isEqualTo("subprocess");
    assertThat(subProcess.getInstances()).isEqualTo(25);

    // subprocess multi instance body
    HistoricActivityStatistics subProcessMiBody = statistics.get(2);

    assertThat(subProcessMiBody.getId()).isEqualTo("subprocess#multiInstanceBody");
    assertThat(subProcessMiBody.getInstances()).isEqualTo(5);

    // task
    HistoricActivityStatistics task = statistics.get(3);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(5);

    completeProcessInstances();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testWithCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.calledProcess.bpmn20.xml" })
  @Test
  public void testMultipleProcessDefinitions() {
    String processId = getProcessDefinitionId();
    String calledProcessId = getProcessDefinitionIdByKey("calledProcess");

    startProcesses(5);

    startProcessesByKey(10, "calledProcess");

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processId)
        .orderByActivityId()
        .asc();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(1);
    assertThat(statistics).hasSize(1);

    // callActivity
    HistoricActivityStatistics calledActivity = statistics.get(0);

    assertThat(calledActivity.getId()).isEqualTo("callActivity");
    assertThat(calledActivity.getInstances()).isEqualTo(5);

    query = historyService
        .createHistoricActivityStatisticsQuery(calledProcessId)
        .orderByActivityId()
        .asc();

    statistics = query.list();

    assertThat(query.count()).isEqualTo(2);
    assertThat(statistics).hasSize(2);

    // task1
    HistoricActivityStatistics task1 = statistics.get(0);

    assertThat(task1.getId()).isEqualTo("task1");
    assertThat(task1.getInstances()).isEqualTo(15);

    // task2
    HistoricActivityStatistics task2 = statistics.get(1);

    assertThat(task2.getId()).isEqualTo("task2");
    assertThat(task2.getInstances()).isEqualTo(15);

    completeProcessInstances();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByFinished() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(2);
    assertThat(statistics).hasSize(2);

    // start
    HistoricActivityStatistics start = statistics.get(0);

    assertThat(start.getId()).isEqualTo("start");
    assertThat(start.getInstances()).isEqualTo(0);
    assertThat(start.getFinished()).isEqualTo(5);

    // task
    HistoricActivityStatistics task = statistics.get(1);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(5);
    assertThat(task.getFinished()).isEqualTo(0);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByFinishedAfterFinishingSomeInstances() {
    String processDefinitionId = getProcessDefinitionId();

    // start five instances
    startProcesses(5);

    // complete two task, so that two process instances are finished
    List<Task> tasks = taskService.createTaskQuery().list();
    for (int i = 0; i < 2; i++) {
      taskService.complete(tasks.get(i).getId());
    }

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .orderByActivityId()
        .asc();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(3);
    assertThat(statistics).hasSize(3);

    // end
    HistoricActivityStatistics end = statistics.get(0);

    assertThat(end.getId()).isEqualTo("end");
    assertThat(end.getInstances()).isEqualTo(0);
    assertThat(end.getFinished()).isEqualTo(2);

    // start
    HistoricActivityStatistics start = statistics.get(1);

    assertThat(start.getId()).isEqualTo("start");
    assertThat(start.getInstances()).isEqualTo(0);
    assertThat(start.getFinished()).isEqualTo(5);

    // task
    HistoricActivityStatistics task = statistics.get(2);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(3);
    assertThat(task.getFinished()).isEqualTo(2);

    completeProcessInstances();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testMultipleRunningTasks.bpmn20.xml")
  @Test
  public void testQueryByFinishedMultipleRunningTasks() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("innerTask").list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .orderByActivityId()
        .asc();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(9);
    assertThat(statistics).hasSize(9);

    // end1
    HistoricActivityStatistics end1 = statistics.get(0);

    assertThat(end1.getId()).isEqualTo("end1");
    assertThat(end1.getInstances()).isEqualTo(0);
    assertThat(end1.getFinished()).isEqualTo(5);

    // gtw
    HistoricActivityStatistics gtw = statistics.get(1);

    assertThat(gtw.getId()).isEqualTo("gtw");
    assertThat(gtw.getInstances()).isEqualTo(0);
    assertThat(gtw.getFinished()).isEqualTo(5);

    // innerEnd
    HistoricActivityStatistics innerEnd = statistics.get(2);

    assertThat(innerEnd.getId()).isEqualTo("innerEnd");
    assertThat(innerEnd.getInstances()).isEqualTo(0);
    assertThat(innerEnd.getFinished()).isEqualTo(25);

    // innerStart
    HistoricActivityStatistics innerStart = statistics.get(3);

    assertThat(innerStart.getId()).isEqualTo("innerStart");
    assertThat(innerStart.getInstances()).isEqualTo(0);
    assertThat(innerStart.getFinished()).isEqualTo(25);

    // innerTask
    HistoricActivityStatistics innerTask = statistics.get(4);

    assertThat(innerTask.getId()).isEqualTo("innerTask");
    assertThat(innerTask.getInstances()).isEqualTo(0);
    assertThat(innerTask.getFinished()).isEqualTo(25);

    // innerStart
    HistoricActivityStatistics start = statistics.get(5);

    assertThat(start.getId()).isEqualTo("start");
    assertThat(start.getInstances()).isEqualTo(0);
    assertThat(start.getFinished()).isEqualTo(5);

    // subprocess
    HistoricActivityStatistics subProcess = statistics.get(6);

    assertThat(subProcess.getId()).isEqualTo("subprocess");
    assertThat(subProcess.getInstances()).isEqualTo(0);
    assertThat(subProcess.getFinished()).isEqualTo(25);

    // subprocess - multi-instance body
    HistoricActivityStatistics subProcessMiBody = statistics.get(7);

    assertThat(subProcessMiBody.getId()).isEqualTo("subprocess#multiInstanceBody");
    assertThat(subProcessMiBody.getInstances()).isEqualTo(0);
    assertThat(subProcessMiBody.getFinished()).isEqualTo(5);

    // task
    HistoricActivityStatistics task = statistics.get(8);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(5);
    assertThat(task.getFinished()).isEqualTo(0);

    completeProcessInstances();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByCompleteScope() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    completeProcessInstances();

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCompleteScope();
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(1);
    assertThat(statistics).hasSize(1);

    // end
    HistoricActivityStatistics end = statistics.get(0);

    assertThat(end.getId()).isEqualTo("end");
    assertThat(end.getInstances()).isEqualTo(0);
    assertThat(end.getCompleteScope()).isEqualTo(5);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByCompleteScopeAfterFinishingSomeInstances() {
    String processDefinitionId = getProcessDefinitionId();

    // start five instances
    startProcesses(5);

    // complete two task, so that two process instances are finished
    List<Task> tasks = taskService.createTaskQuery().list();
    for (int i = 0; i < 2; i++) {
      taskService.complete(tasks.get(i).getId());
    }

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCompleteScope()
        .orderByActivityId()
        .asc();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(2);
    assertThat(statistics).hasSize(2);

    // end
    HistoricActivityStatistics end = statistics.get(0);

    assertThat(end.getId()).isEqualTo("end");
    assertThat(end.getInstances()).isEqualTo(0);
    assertThat(end.getCompleteScope()).isEqualTo(2);

    // task
    HistoricActivityStatistics task = statistics.get(1);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(3);
    assertThat(task.getCompleteScope()).isEqualTo(0);

    completeProcessInstances();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testMultipleRunningTasks.bpmn20.xml")
  @Test
  public void testQueryByCompleteScopeMultipleRunningTasks() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("innerTask").list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCompleteScope()
        .orderByActivityId()
        .asc();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(4);
    assertThat(statistics).hasSize(4);

    // end1
    HistoricActivityStatistics end1 = statistics.get(0);

    assertThat(end1.getId()).isEqualTo("end1");
    assertThat(end1.getInstances()).isEqualTo(0);
    assertThat(end1.getCompleteScope()).isEqualTo(5);

    // innerEnd
    HistoricActivityStatistics innerEnd = statistics.get(1);

    assertThat(innerEnd.getId()).isEqualTo("innerEnd");
    assertThat(innerEnd.getInstances()).isEqualTo(0);
    assertThat(innerEnd.getCompleteScope()).isEqualTo(25);

    // subprocess (completes the multi-instances body scope, see BPMN spec)
    HistoricActivityStatistics subprocess = statistics.get(2);

    assertThat(subprocess.getId()).isEqualTo("subprocess");
    assertThat(subprocess.getInstances()).isEqualTo(0);
    assertThat(subprocess.getCompleteScope()).isEqualTo(25);

    // task
    HistoricActivityStatistics task = statistics.get(3);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(5);
    assertThat(task.getCompleteScope()).isEqualTo(0);

    completeProcessInstances();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByCanceled() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    cancelProcessInstances();

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCanceled();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(1);
    assertThat(statistics).hasSize(1);

    // task
    HistoricActivityStatistics task = statistics.get(0);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(0);
    assertThat(task.getCanceled()).isEqualTo(5);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByCanceledAfterCancelingSomeInstances() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(3);

    // cancel running process instances
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    for (ProcessInstance processInstance : processInstances) {
      runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    startProcesses(2);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCanceled();

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(1);
    assertThat(statistics).hasSize(1);

    // task
    HistoricActivityStatistics task = statistics.get(0);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(2);
    assertThat(task.getCanceled()).isEqualTo(3);

    completeProcessInstances();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByCanceledAndFinished() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(2);

    // cancel running process instances
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    for (ProcessInstance processInstance : processInstances) {
      runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    startProcesses(2);

    // complete running tasks
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    startProcesses(2);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCanceled()
        .includeFinished()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(3);
    assertThat(statistics).hasSize(3);

    // end
    HistoricActivityStatistics end = statistics.get(0);

    assertThat(end.getId()).isEqualTo("end");
    assertThat(end.getInstances()).isEqualTo(0);
    assertThat(end.getCanceled()).isEqualTo(0);
    assertThat(end.getFinished()).isEqualTo(2);

    // start
    HistoricActivityStatistics start = statistics.get(1);

    assertThat(start.getId()).isEqualTo("start");
    assertThat(start.getInstances()).isEqualTo(0);
    assertThat(start.getCanceled()).isEqualTo(0);
    assertThat(start.getFinished()).isEqualTo(6);

    // task
    HistoricActivityStatistics task = statistics.get(2);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(2);
    assertThat(task.getCanceled()).isEqualTo(2);
    assertThat(task.getFinished()).isEqualTo(4);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByCanceledAndFinishedByPeriods() throws ParseException {
    try {

      //start two process instances
      ClockUtil.setCurrentTime(sdf.parse("15.01.2016 12:00:00"));
      startProcesses(2);

      // cancel running process instances
      ClockUtil.setCurrentTime(sdf.parse("15.02.2016 12:00:00"));
      List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
      for (ProcessInstance processInstance : processInstances) {
        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
      }

      //start two process instances
      ClockUtil.setCurrentTime(sdf.parse("01.02.2016 12:00:00"));
      startProcesses(2);

      // complete running tasks
      ClockUtil.setCurrentTime(sdf.parse("25.02.2016 12:00:00"));
      List<Task> tasks = taskService.createTaskQuery().list();
      for (Task task : tasks) {
        taskService.complete(task.getId());
      }

      //starte two more process instances
      ClockUtil.setCurrentTime(sdf.parse("15.03.2016 12:00:00"));
      startProcesses(2);

      //NOW
      ClockUtil.setCurrentTime(sdf.parse("25.03.2016 12:00:00"));

      String processDefinitionId = getProcessDefinitionId();
      //check January by started dates
      HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId).includeCanceled().includeFinished()
        .startedAfter(sdf.parse("01.01.2016 00:00:00")).startedBefore(sdf.parse("31.01.2016 23:59:59")).orderByActivityId().asc();
      List<HistoricActivityStatistics> statistics = query.list();

      assertThat(query.count()).isEqualTo(2);
      assertThat(statistics).hasSize(2);

      // start
      assertActivityStatistics(statistics.get(0), "start", 0, 0, 2);

      // task
      assertActivityStatistics(statistics.get(1), "task", 0, 2, 2);

      //check January by finished dates
      query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId).includeCanceled().includeFinished()
        .finishedAfter(sdf.parse("01.01.2016 00:00:00")).finishedBefore(sdf.parse("31.01.2016 23:59:59")).orderByActivityId().asc();
      statistics = query.list();

      assertThat(query.count()).isEqualTo(0);
      assertThat(statistics).hasSize(0);

      //check February by started dates
      query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId).includeCanceled().includeFinished()
        .startedAfter(sdf.parse("01.02.2016 00:00:00")).startedBefore(sdf.parse("28.02.2016 23:59:59")).orderByActivityId().asc();
      statistics = query.list();

      assertThat(query.count()).isEqualTo(3);
      assertThat(statistics).hasSize(3);

      // end
      assertActivityStatistics(statistics.get(0), "end", 0, 0, 2);

      // start
      assertActivityStatistics(statistics.get(1), "start", 0, 0, 2);

      // task
      assertActivityStatistics(statistics.get(2), "task", 0, 0, 2);

      //check February by finished dates
      query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId).includeCanceled().includeFinished()
        .finishedAfter(sdf.parse("01.02.2016 00:00:00")).finishedBefore(sdf.parse("28.02.2016 23:59:59")).orderByActivityId().asc();
      statistics = query.list();

      assertThat(query.count()).isEqualTo(3);
      assertThat(statistics).hasSize(3);

      // end
      assertActivityStatistics(statistics.get(0), "end", 0, 0, 2);

      // start
      assertActivityStatistics(statistics.get(1), "start", 0, 0, 4);

      // task
      assertActivityStatistics(statistics.get(2), "task", 0, 2, 4);

      //check March by started dates
      query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId).includeCanceled().includeFinished()
        .startedAfter(sdf.parse("01.03.2016 00:00:00")).orderByActivityId().asc();
      statistics = query.list();

      assertThat(query.count()).isEqualTo(2);
      assertThat(statistics).hasSize(2);

      // start
      assertActivityStatistics(statistics.get(0), "start", 0, 0, 2);

      // task
      assertActivityStatistics(statistics.get(1), "task", 2, 0, 0);

      //check March by finished dates
      query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId).includeCanceled().includeFinished()
        .finishedAfter(sdf.parse("01.03.2016 00:00:00")).orderByActivityId().asc();
      statistics = query.list();

      assertThat(query.count()).isEqualTo(0);
      assertThat(statistics).hasSize(0);

      //check whole period by started date
      query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId).includeCanceled().includeFinished()
        .startedAfter(sdf.parse("01.01.2016 00:00:00")).orderByActivityId().asc();
      statistics = query.list();

      assertThat(query.count()).isEqualTo(3);
      assertThat(statistics).hasSize(3);

      // end
      assertActivityStatistics(statistics.get(0), "end", 0, 0, 2);

      // start
      assertActivityStatistics(statistics.get(1), "start", 0, 0, 6);

      // task
      assertActivityStatistics(statistics.get(2), "task", 2, 2, 4);

    } finally {
      ClockUtil.reset();
    }

  }

  protected void assertActivityStatistics(HistoricActivityStatistics activity, String activityName, long instances, long canceled, long finished) {
    assertThat(activity.getId()).isEqualTo(activityName);
    assertThat(activity.getInstances()).isEqualTo(instances);
    assertThat(activity.getCanceled()).isEqualTo(canceled);
    assertThat(activity.getFinished()).isEqualTo(finished);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByCanceledAndCompleteScope() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(2);

    // cancel running process instances
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    for (ProcessInstance processInstance : processInstances) {
      runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    startProcesses(2);

    // complete running tasks
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    startProcesses(2);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCanceled()
        .includeCompleteScope()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(2);
    assertThat(statistics).hasSize(2);

    // end
    HistoricActivityStatistics end = statistics.get(0);

    assertThat(end.getId()).isEqualTo("end");
    assertThat(end.getInstances()).isEqualTo(0);
    assertThat(end.getCanceled()).isEqualTo(0);
    assertThat(end.getCompleteScope()).isEqualTo(2);

    // task
    HistoricActivityStatistics task = statistics.get(1);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(2);
    assertThat(task.getCanceled()).isEqualTo(2);
    assertThat(task.getCompleteScope()).isEqualTo(0);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByFinishedAndCompleteScope() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(2);

    // cancel running process instances
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    for (ProcessInstance processInstance : processInstances) {
      runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    startProcesses(2);

    // complete running tasks
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    startProcesses(2);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCompleteScope()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(3);
    assertThat(statistics).hasSize(3);

    // end
    HistoricActivityStatistics end = statistics.get(0);

    assertThat(end.getId()).isEqualTo("end");
    assertThat(end.getInstances()).isEqualTo(0);
    assertThat(end.getFinished()).isEqualTo(2);
    assertThat(end.getCompleteScope()).isEqualTo(2);

    // start
    HistoricActivityStatistics start = statistics.get(1);

    assertThat(start.getId()).isEqualTo("start");
    assertThat(start.getInstances()).isEqualTo(0);
    assertThat(start.getFinished()).isEqualTo(6);
    assertThat(start.getCompleteScope()).isEqualTo(0);

    // task
    HistoricActivityStatistics task = statistics.get(2);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(2);
    assertThat(task.getFinished()).isEqualTo(4);
    assertThat(task.getCompleteScope()).isEqualTo(0);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByFinishedAndCompleteScopeAndCanceled() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(2);

    // cancel running process instances
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    for (ProcessInstance processInstance : processInstances) {
      runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    startProcesses(2);

    // complete running tasks
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    startProcesses(2);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCompleteScope()
        .includeCanceled()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(3);
    assertThat(statistics).hasSize(3);

    // end
    HistoricActivityStatistics end = statistics.get(0);

    assertThat(end.getId()).isEqualTo("end");
    assertThat(end.getInstances()).isEqualTo(0);
    assertThat(end.getCanceled()).isEqualTo(0);
    assertThat(end.getFinished()).isEqualTo(2);
    assertThat(end.getCompleteScope()).isEqualTo(2);

    // start
    HistoricActivityStatistics start = statistics.get(1);

    assertThat(start.getId()).isEqualTo("start");
    assertThat(start.getInstances()).isEqualTo(0);
    assertThat(start.getCanceled()).isEqualTo(0);
    assertThat(start.getFinished()).isEqualTo(6);
    assertThat(start.getCompleteScope()).isEqualTo(0);

    // task
    HistoricActivityStatistics task = statistics.get(2);

    assertThat(task.getId()).isEqualTo("task");
    assertThat(task.getInstances()).isEqualTo(2);
    assertThat(task.getCanceled()).isEqualTo(2);
    assertThat(task.getFinished()).isEqualTo(4);
    assertThat(task.getCompleteScope()).isEqualTo(0);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryByProcessInstanceIds() {
    // given
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(3);
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String cancelledInstance = processInstances.get(0).getId();
    String completedInstance = processInstances.get(1).getId();

    runtimeService.deleteProcessInstance(cancelledInstance, "test");
    Task task = taskService.createTaskQuery().processInstanceId(completedInstance).singleResult();
    taskService.complete(task.getId());

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .processInstanceIdIn(cancelledInstance, completedInstance) // excluding the third running instance
        .includeFinished()
        .includeCompleteScope()
        .includeCanceled()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    // end
    HistoricActivityStatistics endStats = statistics.get(0);

    assertThat(endStats.getId()).isEqualTo("end");
    assertThat(endStats.getInstances()).isEqualTo(0);
    assertThat(endStats.getCanceled()).isEqualTo(0);
    assertThat(endStats.getFinished()).isEqualTo(1);
    assertThat(endStats.getCompleteScope()).isEqualTo(1);

    // start
    HistoricActivityStatistics startStats = statistics.get(1);

    assertThat(startStats.getId()).isEqualTo("start");
    assertThat(startStats.getInstances()).isEqualTo(0);
    assertThat(startStats.getCanceled()).isEqualTo(0);
    assertThat(startStats.getFinished()).isEqualTo(2);
    assertThat(startStats.getCompleteScope()).isEqualTo(0);

    // task
    HistoricActivityStatistics taskStats = statistics.get(2);

    assertThat(taskStats.getId()).isEqualTo("task");
    assertThat(taskStats.getInstances()).isEqualTo(0);
    assertThat(taskStats.getCanceled()).isEqualTo(1);
    assertThat(taskStats.getFinished()).isEqualTo(2);
    assertThat(taskStats.getCompleteScope()).isEqualTo(0);
  }

  @Test
  public void testCheckProcessInstanceIdsForNull() {
    // given
    HistoricActivityStatisticsQuery query = historyService
    .createHistoricActivityStatisticsQuery("foo");

    // when 1
    try {
      query.processInstanceIdIn((String[]) null);
      fail("exception expected");
    } catch (NullValueException e) {
      // then 1
      testRule.assertTextPresent("processInstanceIds is null", e.getMessage());
    }

    // when 2
    try {
      query.processInstanceIdIn((String) null);
      fail("exception expected");
    } catch (NullValueException e) {
      // then 2
      testRule.assertTextPresent("processInstanceIds contains null value", e.getMessage());
    }
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testSorting() {
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(5);

    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId);

    assertThat(query.orderByActivityId().asc().list()).hasSize(1);
    assertThat(query.orderByActivityId().desc().list()).hasSize(1);

    assertThat(query.orderByActivityId().asc().count()).isEqualTo(1);
    assertThat(query.orderByActivityId().desc().count()).isEqualTo(1);
  }

  @Deployment(resources= {"org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testAnotherSingleTask.bpmn20.xml"})
  @Test
  public void testDifferentProcessesWithSameActivityId() {
    String processDefinitionId = getProcessDefinitionId();
    String anotherProcessDefinitionId = getProcessDefinitionIdByKey("anotherProcess");

    startProcesses(5);

    startProcessesByKey(10, "anotherProcess");

    // first processDefinition
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId);

    List<HistoricActivityStatistics> statistics = query.list();

    assertThat(query.count()).isEqualTo(1);
    assertThat(statistics).hasSize(1);

    HistoricActivityStatistics task = statistics.get(0);
    assertThat(task.getInstances()).isEqualTo(5);

    // second processDefinition
    query = historyService
        .createHistoricActivityStatisticsQuery(anotherProcessDefinitionId);

    statistics = query.list();

    assertThat(query.count()).isEqualTo(1);
    assertThat(statistics).hasSize(1);

    task = statistics.get(0);
    assertThat(task.getInstances()).isEqualTo(10);

  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryIncludeIncidents() {
    // given
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(4);
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String processInstanceWithResolvedIncidents = processInstances.get(1).getId();
    String processInstanceWithDeletedIncident = processInstances.get(2).getId();
    String processInstanceWithOpenIncident = processInstances.get(3).getId();

    Incident resolvedIncident = createIncident(processInstanceWithResolvedIncidents);
    runtimeService.resolveIncident(resolvedIncident.getId());
    resolvedIncident = createIncident(processInstanceWithResolvedIncidents);
    runtimeService.resolveIncident(resolvedIncident.getId());

    createIncident(processInstanceWithDeletedIncident);
    runtimeService.deleteProcessInstance(processInstances.get(2).getId(), "test");

    createIncident(processInstanceWithOpenIncident);

    // when
    final HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCanceled()
        .includeIncidents()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    assertThat(statistics).hasSize(2);

    // start
    assertActivityStatistics(statistics.get(0), "start", 0, 0, 4, 0, 0, 0);

    // task
    assertActivityStatistics(statistics.get(1), "task", 3, 1, 1, 1, 2, 1);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryIncludeIncidentsDeletedOnlyAndProcessInstanceIds() {
    // given
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(3);
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String processInstanceWithResolvedIncident = processInstances.get(1).getId();
    String processInstanceWithDeletedIncident = processInstances.get(2).getId();

    runtimeService.resolveIncident(createIncident(processInstanceWithResolvedIncident).getId());

    createIncident(processInstanceWithDeletedIncident);
    createIncident(processInstanceWithDeletedIncident);
    createIncident(processInstanceWithDeletedIncident);
    runtimeService.deleteProcessInstance(processInstanceWithDeletedIncident, "test");

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .processInstanceIdIn(processInstanceWithDeletedIncident)
        .includeFinished()
        .includeCompleteScope()
        .includeCanceled()
        .includeIncidents()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    assertThat(statistics).hasSize(2);

    // start
    assertActivityStatistics(statistics.get(0), "start", 0, 0, 1, 0, 0, 0);

    // task
    assertActivityStatistics(statistics.get(1), "task", 0, 1, 1, 0, 0, 3);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryIncludeIncidentsWhenNoIncidents() {
    // given
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(2);
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();

    runtimeService.deleteProcessInstance(processInstances.get(0).getId(), null);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCompleteScope()
        .includeCanceled()
        .includeIncidents()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    assertThat(statistics).hasSize(2);

    // start
    assertActivityStatistics(statistics.get(0), "start", 0, 0, 2, 0, 0, 0);

    // task
    assertActivityStatistics(statistics.get(1), "task", 1, 1, 1, 0, 0, 0);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testMultipleRunningTasks.bpmn20.xml")
  @Test
  public void testQueryIncludeIncidentsMultipleRunningTasksDeletedOnly() {
    // given
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(2);
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String cancelledInstanceWithIncident = processInstances.get(0).getId();

     List<Execution> executions = runtimeService.createExecutionQuery()
        .processInstanceId(cancelledInstanceWithIncident)
        .activityId("innerTask").active().list();
    runtimeService.createIncident("foo1", executions.get(0).getId(), ((ExecutionEntity) executions.get(0)).getActivityId(), "bar1");
    runtimeService.createIncident("foo2", executions.get(1).getId(), ((ExecutionEntity) executions.get(1)).getActivityId(), "bar2");
    runtimeService.deleteProcessInstance(cancelledInstanceWithIncident, "test");

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCompleteScope()
        .includeCanceled()
        .includeIncidents()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    assertThat(statistics).hasSize(7);

    assertActivityStatistics(statistics.get(0), "gtw", 0, 0, 2, 0, 0, 0);
    assertActivityStatistics(statistics.get(1), "innerStart", 0, 0, 10, 0, 0, 0);
    assertActivityStatistics(statistics.get(2), "innerTask", 5, 5, 5, 0, 0, 2);
    assertActivityStatistics(statistics.get(3), "start", 0, 0, 2, 0, 0, 0);
    assertActivityStatistics(statistics.get(4), "subprocess", 5, 5, 5, 0, 0, 0);
    assertActivityStatistics(statistics.get(5), "subprocess#multiInstanceBody", 1, 1, 1, 0, 0, 0);
    assertActivityStatistics(statistics.get(6), "task", 1, 1, 1, 0, 0, 0);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testMultipleRunningTasks.bpmn20.xml")
  @Test
  public void testQueryIncludeIncidentsMultipleRunningTasksOpenOnly() {
    // given
    String processDefinitionId = getProcessDefinitionId();

    startProcesses(2);
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String cancelledInstanceWithIncident = processInstances.get(0).getId();

     List<Execution> executions = runtimeService.createExecutionQuery()
        .processInstanceId(cancelledInstanceWithIncident)
        .activityId("innerTask").active().list();
    runtimeService.createIncident("foo1", executions.get(0).getId(), ((ExecutionEntity) executions.get(0)).getActivityId(), "bar1");
    runtimeService.createIncident("foo1", executions.get(0).getId(), ((ExecutionEntity) executions.get(0)).getActivityId(), "bar1");
    runtimeService.createIncident("foo2", executions.get(1).getId(), ((ExecutionEntity) executions.get(1)).getActivityId(), "bar2");

    executions = runtimeService.createExecutionQuery()
        .processInstanceId(cancelledInstanceWithIncident)
        .activityId("task").active().list();
    runtimeService.createIncident("foo", executions.get(0).getId(), ((ExecutionEntity) executions.get(0)).getActivityId(), "bar");

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    assertThat(statistics).hasSize(4);

    assertActivityStatistics(statistics.get(0), "innerTask", 10, 0, 0, 3, 0, 0);
    assertActivityStatistics(statistics.get(1), "subprocess", 10, 0, 0, 0, 0, 0);
    assertActivityStatistics(statistics.get(2), "subprocess#multiInstanceBody", 2, 0, 0, 0, 0, 0);
    assertActivityStatistics(statistics.get(3), "task", 2, 0, 0, 1, 0, 0);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryCancelledIncludeIncidentsDeletedOnly() throws ParseException {
    try {
      // given
      String processDefinitionId = getProcessDefinitionId();

      // start two instances with one incident and cancel them
      ClockUtil.setCurrentTime(sdf.parse("5.10.2019 12:00:00"));
      startProcessesByKey(2, "process");
      List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
      String firstInstance = processInstances.get(0).getId();
      createIncident(firstInstance);
      cancelProcessInstances();

      // start another two instances with two incidents and cancel them
      ClockUtil.setCurrentTime(sdf.parse("20.10.2019 12:00:00"));
      startProcessesByKey(2, "process");
      processInstances = runtimeService.createProcessInstanceQuery().list();
      String thirdInstance = processInstances.get(0).getId();
      String fourthInstance = processInstances.get(1).getId();
      createIncident(thirdInstance);
      createIncident(fourthInstance);
      cancelProcessInstances();

      // when
      final HistoricActivityStatisticsQuery query = historyService
          .createHistoricActivityStatisticsQuery(processDefinitionId)
          .startedAfter(sdf.parse("01.10.2019 12:00:00"))
          .startedBefore(sdf.parse("10.10.2019 12:00:00"))
          .includeFinished()
          .includeCompleteScope()
          .includeCanceled()
          .includeIncidents()
          .orderByActivityId()
          .asc();
      List<HistoricActivityStatistics> statistics = query.list();

      // then results only from the first two instances
      assertThat(statistics).hasSize(2);
      assertActivityStatistics(statistics.get(0), "start", 0, 0, 2, 0, 0, 0);
      assertActivityStatistics(statistics.get(1), "task", 0, 2, 2, 0, 0, 1);
    } finally {
      ClockUtil.reset();
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/history/HistoricActivityStatisticsQueryTest.testSingleTask.bpmn20.xml")
  @Test
  public void testQueryCompletedIncludeIncidentsDeletedOnly() throws ParseException {
    try {
      // given
      String processDefinitionId = getProcessDefinitionId();

      // start two instances with one incident and complete them
      ClockUtil.setCurrentTime(sdf.parse("5.10.2019 12:00:00"));
      startProcessesByKey(2, "process");
      List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
      String firstInstance = processInstances.get(0).getId();
      createIncident(firstInstance);
      completeProcessInstances();

      // start another two instances with two incidents and complete them
      ClockUtil.setCurrentTime(sdf.parse("20.10.2019 12:00:00"));
      startProcessesByKey(2, "process");
      processInstances = runtimeService.createProcessInstanceQuery().list();
      String thirdInstance = processInstances.get(0).getId();
      createIncident(thirdInstance);
      completeProcessInstances();

      // when
      final HistoricActivityStatisticsQuery query = historyService
          .createHistoricActivityStatisticsQuery(processDefinitionId)
          .finishedAfter(sdf.parse("10.10.2019 12:00:00"))
          .finishedBefore(sdf.parse("30.10.2019 12:00:00"))
          .includeFinished()
          .includeCompleteScope()
          .includeCanceled()
          .includeIncidents()
          .orderByActivityId()
          .asc();
      List<HistoricActivityStatistics> statistics = query.list();

      // then results only from the second two instances
      assertThat(statistics).hasSize(3);
      assertActivityStatistics(statistics.get(0), "end", 0, 0, 2, 0, 0, 0);
      assertActivityStatistics(statistics.get(1), "start", 0, 0, 2, 0, 0, 0);
      assertActivityStatistics(statistics.get(2), "task", 0, 0, 2, 0, 0, 1);
    } finally {
      ClockUtil.reset();
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml")
  @Test
  public void testQueryIncludeIncidentsWhenNoHistoricActivityInstanceDeletedOnly() {
    // given
    startProcessesByKey(3, "failingProcess");

    List<Job> list = managementService.createJobQuery().list();
    for (Job job : list) {
      managementService.setJobRetries(job.getId(), 0);
    }

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String cancelledInstance1 = processInstances.get(1).getId();
    String cancelledInstance2 = processInstances.get(2).getId();
    String processDefinitionId = processInstances.get(0).getProcessDefinitionId();

    runtimeService.deleteProcessInstances(java.util.Arrays.asList(cancelledInstance1, cancelledInstance2), "test", false, false);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeIncidents()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    assertThat(statistics).hasSize(2);

    assertActivityStatistics(statistics.get(0), "serviceTask", 0, 0, 0, 1, 0, 2);
    assertActivityStatistics(statistics.get(1), "start", 0, 0, 3, 0, 0, 0);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources="org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml")
  @Test
  public void testQueryIncludeIncidentsWhenNoHistoricActivityInstanceWithoutFilters() {
    // given
    startProcessesByKey(3, "failingProcess");

    List<Job> list = managementService.createJobQuery().list();
    for (Job job : list) {
      managementService.setJobRetries(job.getId(), 0);
    }

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    String cancelledInstance1 = processInstances.get(1).getId();
    String cancelledInstance2 = processInstances.get(2).getId();
    String processDefinitionId = processInstances.get(0).getProcessDefinitionId();

    runtimeService.deleteProcessInstances(java.util.Arrays.asList(cancelledInstance1, cancelledInstance2), "test", false, false);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .orderByActivityId()
        .asc();
    List<HistoricActivityStatistics> statistics = query.list();

    // then
    assertThat(statistics).hasSize(1);

    assertActivityStatistics(statistics.get(0), "serviceTask", 0, 0, 0, 1, 0, 2);
  }

  protected void completeProcessInstances() {
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
  }

  protected void cancelProcessInstances() {
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
    for (ProcessInstance pi : processInstances) {
      runtimeService.deleteProcessInstance(pi.getId(), "test");
    }
  }

  protected void startProcesses(int numberOfInstances) {
    startProcessesByKey(numberOfInstances, "process");
  }

  protected void startProcessesByKey(int numberOfInstances, String key) {
    for (int i = 0; i < numberOfInstances; i++) {
      runtimeService.startProcessInstanceByKey(key);
    }
  }

  protected String getProcessDefinitionIdByKey(String key) {
    return repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).singleResult().getId();
  }

  protected String getProcessDefinitionId() {
    return getProcessDefinitionIdByKey("process");
  }

  protected Incident createIncident(String instanceId) {
    ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(instanceId).active().singleResult();
    return runtimeService.createIncident("foo", execution.getId(), execution.getActivityId(), "exec" + execution.getId());
  }

  protected void assertActivityStatistics(HistoricActivityStatistics activity, String activityName, int instances, int canceled, int finished, int openIncidents, int resolvedIncidents, int deletedIncidents) {
    assertActivityStatistics(activity, activityName, instances, canceled, finished);
    assertThat(activity.getOpenIncidents()).isEqualTo(openIncidents);
    assertThat(activity.getResolvedIncidents()).isEqualTo(resolvedIncidents);
    assertThat(activity.getDeletedIncidents()).isEqualTo(deletedIncidents);
  }

}
