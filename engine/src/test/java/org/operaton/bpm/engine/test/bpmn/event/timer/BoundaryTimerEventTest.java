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
package org.operaton.bpm.engine.test.bpmn.event.timer;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joram Barrez
 */
@SuppressWarnings({"java:S4144", "java:S5976"})
class BoundaryTimerEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  ManagementService managementService;
  TaskService taskService;

  /*
   * Test for when multiple boundary timer events are defined on the same user
   * task
   *
   * Configuration: - timer 1 -> 2 hours -> secondTask - timer 2 -> 1 hour ->
   * thirdTask - timer 3 -> 3 hours -> fourthTask
   *
   * See process image next to the process xml resource
   */
  @Deployment
  @Test
  void testMultipleTimersOnUserTask() {

    // Set the clock fixed
    Date startTime = new Date();

    // After process start, there should be 3 timers created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("multipleTimersOnUserTask");
    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(3);

    // After setting the clock to time '1 hour and 5 seconds', the second timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);
    assertThat(jobQuery.count()).isZero();

    // which means that the third task is reached
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Third Task");
  }

  @Deployment
  @Test
  void testTimerOnNestingOfSubprocesses() {

    runtimeService.startProcessInstanceByKey("timerOnNestedSubprocesses");
    List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("Inner subprocess task 1");
    assertThat(tasks.get(1).getName()).isEqualTo("Inner subprocess task 2");

    Job timer = managementService.createJobQuery().timers().singleResult();
    managementService.executeJob(timer.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task outside subprocess");
  }

  @Deployment
  @Test
  void testExpressionOnTimer(){
    // Set the clock fixed
    Date startTime = new Date();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("duration", "PT1H");

    // After process start, there should be a timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnTimer", variables);

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);

    // After setting the clock to time '1 hour and 5 seconds', the second timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);
    assertThat(jobQuery.count()).isZero();

    // which means the process has ended
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testRecalculateUnchangedExpressionOnTimerCurrentDateBased(){
    // Set the clock fixed
    Date startTime = new Date();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("duedate", "PT1H");

    // After process start, there should be a timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnTimer", variables);

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);
    Date oldDate = job.getDuedate();

    // After recalculation of the timer, the job's duedate should be changed
    Date currentTime = new Date(startTime.getTime() + TimeUnit.MINUTES.toMillis(5));
    ClockUtil.setCurrentTime(currentTime);
    managementService.recalculateJobDuedate(job.getId(), false);
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(job.getId());
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDate);
    assertThat(oldDate.before(jobUpdated.getDuedate())).isTrue();
    Date expectedDate = LocalDateTime.fromDateFields(currentTime).plusHours(1).toDate();
    assertThat(jobUpdated.getDuedate()).isCloseTo(expectedDate, 1000L);

    // After setting the clock to time '1 hour and 6 min', the second timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + TimeUnit.HOURS.toMillis(1L) + TimeUnit.MINUTES.toMillis(6L)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);
    assertThat(jobQuery.count()).isZero();

    // which means the process has ended
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/timer/BoundaryTimerEventTest.testRecalculateUnchangedExpressionOnTimerCurrentDateBased.bpmn20.xml")
  @Test
  void testRecalculateUnchangedExpressionOnTimerCreationDateBased(){
    // Set the clock fixed
    Date startTime = new Date();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("duedate", "PT1H");

    // After process start, there should be a timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnTimer", variables);

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);

    // After recalculation of the timer, the job's duedate should be based on the creation date
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + TimeUnit.SECONDS.toMillis(5)));
    managementService.recalculateJobDuedate(job.getId(), true);
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(job.getId());
    Date expectedDate = LocalDateTime.fromDateFields(jobUpdated.getCreateTime()).plusHours(1).toDate();
    assertThat(jobUpdated.getDuedate()).isEqualTo(expectedDate);

    // After setting the clock to time '1 hour and 15 seconds', the second timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + TimeUnit.HOURS.toMillis(1L) + TimeUnit.SECONDS.toMillis(15L)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);
    assertThat(jobQuery.count()).isZero();

    // which means the process has ended
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/timer/BoundaryTimerEventTest.testRecalculateUnchangedExpressionOnTimerCurrentDateBased.bpmn20.xml")
  @Test
  void testRecalculateChangedExpressionOnTimerCurrentDateBased(){
    // Set the clock fixed
    Date startTime = new Date();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("duedate", "PT1H");

    // After process start, there should be a timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnTimer", variables);

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);
    Date oldDate = job.getDuedate();
    ClockUtil.offset(2000L);

    // After recalculation of the timer, the job's duedate should be changed
    managementService.recalculateJobDuedate(job.getId(), false);
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(job.getId());
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDate);
    assertThat(oldDate.before(jobUpdated.getDuedate())).isTrue();

    // After setting the clock to time '16 minutes', the timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + TimeUnit.HOURS.toMillis(2L)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);
    assertThat(jobQuery.count()).isZero();

    // which means the process has ended
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/timer/BoundaryTimerEventTest.testRecalculateUnchangedExpressionOnTimerCurrentDateBased.bpmn20.xml")
  @Test
  void testRecalculateChangedExpressionOnTimerCreationDateBased(){
    // Set the clock fixed
    Date startTime = new Date();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("duedate", "PT1H");

    // After process start, there should be a timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnTimer", variables);

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);
    Date oldDate = job.getDuedate();

    // After recalculation of the timer, the job's duedate should be the same
    runtimeService.setVariable(pi.getId(), "duedate", "PT15M");
    managementService.recalculateJobDuedate(job.getId(), true);
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(job.getId());
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDate);
    assertThat(jobUpdated.getDuedate()).isEqualTo(LocalDateTime.fromDateFields(jobUpdated.getCreateTime()).plusMinutes(15).toDate());

    // After setting the clock to time '16 minutes', the timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + TimeUnit.MINUTES.toMillis(16L)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);
    assertThat(jobQuery.count()).isZero();

    // which means the process has ended
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTimerInSingleTransactionProcess() {
    // make sure that if a PI completes in single transaction, JobEntities associated with the execution are deleted.
    // broken before 5.10, see ACT-1133
    runtimeService.startProcessInstanceByKey("timerOnSubprocesses");
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testRepeatingTimerWithCancelActivity() {
    runtimeService.startProcessInstanceByKey("repeatingTimerAndCallActivity");
    assertThat(managementService.createJobQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().count()).isOne();

    // Firing job should cancel the user task, destroy the scope,
    // re-enter the task and recreate the task. A new timer should also be created.
    // This didn't happen before 5.11 (new jobs kept being created). See ACT-1427
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());
    assertThat(managementService.createJobQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().count()).isOne();
  }

  @Deployment
  @Test
  void testMultipleOutgoingSequenceFlows() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("interruptingTimer");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    List<Task> tasks = taskQuery.list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testMultipleOutgoingSequenceFlowsOnSubprocess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("interruptingTimer");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    List<Task> tasks = taskQuery.list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testMultipleOutgoingSequenceFlowsOnSubprocessMi() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("interruptingTimer");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    List<Task> tasks = taskQuery.list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testInterruptingTimerDuration() {

    // Start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("escalationExample");

    // There should be one task, with a timer : first line support
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("First line support");

    // Manually execute the job
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    // The timer has fired, and the second task (secondlinesupport) now exists
    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Handle escalated issue");
  }

}
