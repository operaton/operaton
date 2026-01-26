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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ClockTestUtil;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobExpectingException;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joram Barrez
 */
class BoundaryTimerNonInterruptingEventTest {

  protected static final String TIMER_NON_INTERRUPTING_EVENT = "org/operaton/bpm/engine/test/bpmn/event/timer/BoundaryTimerNonInterruptingEventTest.shouldReevaluateTimerCycleWhenDue.bpmn20.xml";

  protected static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1L);
  protected static final long TWO_HOURS = TimeUnit.HOURS.toMillis(2L);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  ManagementService managementService;
  TaskService taskService;
  boolean reevaluateTimeCycleWhenDue;

  @BeforeEach
  void setUp() {
    reevaluateTimeCycleWhenDue = processEngineConfiguration.isReevaluateTimeCycleWhenDue();
  }

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
    processEngineConfiguration.getBeans().remove("myCycleTimerBean");
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(reevaluateTimeCycleWhenDue);
  }

  @Deployment
  @Test
  void testMultipleTimersOnUserTask() {
    // Set the clock fixed
    Date startTime = new Date();

    // After process start, there should be 3 timers created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingTimersOnUserTask");
    Task task1 = taskService.createTaskQuery().singleResult();
    assertThat(task1.getName()).isEqualTo("First Task");

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(2);

    // After setting the clock to time '1 hour and 5 seconds', the first timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
    testHelper.waitForJobExecutorToProcessAllJobs(5000L);

    // we still have one timer more to fire
    assertThat(jobQuery.count()).isOne();

    // and we are still in the first state, but in the second state as well!
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2L);
    List<Task> taskList = taskService.createTaskQuery().orderByTaskName().desc().list();
    assertThat(taskList.get(0).getName()).isEqualTo("First Task");
    assertThat(taskList.get(1).getName()).isEqualTo("Escalation Task 1");

    // complete the task and end the forked execution
    taskService.complete(taskList.get(1).getId());

    // but we still have the original executions
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("First Task");

    // After setting the clock to time '2 hour and 5 seconds', the second timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((2 * 60 * 60 * 1000) + 5000)));
    testHelper.waitForJobExecutorToProcessAllJobs(5000L);

    // no more timers to fire
    assertThat(jobQuery.count()).isZero();

    // and we are still in the first state, but in the next escalation state as well
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2L);
    taskList = taskService.createTaskQuery().orderByTaskName().desc().list();
    assertThat(taskList.get(0).getName()).isEqualTo("First Task");
    assertThat(taskList.get(1).getName()).isEqualTo("Escalation Task 2");

    // This time we end the main task
    taskService.complete(taskList.get(0).getId());

    // but we still have the escalation task
    assertThat(taskService.createTaskQuery().count()).isOne();
    Task escalationTask = taskService.createTaskQuery().singleResult();
    assertThat(escalationTask.getName()).isEqualTo("Escalation Task 2");

    taskService.complete(escalationTask.getId());

    // now we are really done :-)
    testHelper.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTimerOnMiUserTask() {

    // After process start, there should be 1 timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingTimersOnUserTask");
    List<Task> taskList = taskService.createTaskQuery().list();
    assertThat(taskList).hasSize(5);
    for (Task task : taskList) {
      assertThat(task.getName()).isEqualTo("First Task");
    }

    Job job = managementService.createJobQuery()
        .processInstanceId(pi.getId())
        .singleResult();
    assertThat(job).isNotNull();

    // execute the timer
    managementService.executeJob(job.getId());

    // now there are 6 tasks
    taskList = taskService.createTaskQuery()
        .orderByTaskName()
        .asc()
        .list();
    assertThat(taskList).hasSize(6);

    // first task is the escalation task
    Task escalationTask = taskList.remove(0);
    assertThat(escalationTask.getName()).isEqualTo("Escalation Task 1");
    // complete it
    taskService.complete(escalationTask.getId());

    // now complete the remaining tasks
    for (Task task : taskList) {
      taskService.complete(task.getId());
    }

    // process instance is ended
    testHelper.assertProcessEnded(pi.getId());

  }

  @Deployment
  @Test
  void testJoin() {
    // After process start, there should be 3 timers created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testJoin");
    Task task1 = taskService.createTaskQuery().singleResult();
    assertThat(task1.getName()).isEqualTo("Main Task");

    Job job = managementService.createJobQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    // we now have both tasks
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2L);

    // end the first
    taskService.complete(task1.getId());

    // we now have one task left
    assertThat(taskService.createTaskQuery().count()).isOne();
    Task task2 = taskService.createTaskQuery().singleResult();
    assertThat(task2.getName()).isEqualTo("Escalation Task");

    // complete the task, the parallel gateway should fire
    taskService.complete(task2.getId());

    // and the process has ended
    testHelper.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTimerOnConcurrentMiTasks() {

    // After process start, there should be 1 timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("timerOnConcurrentMiTasks");
    List<Task> taskList = taskService.createTaskQuery()
        .orderByTaskName()
        .desc()
        .list();
    assertThat(taskList).hasSize(6);
    Task secondTask = taskList.remove(0);
    assertThat(secondTask.getName()).isEqualTo("Second Task");
    for (Task task : taskList) {
      assertThat(task.getName()).isEqualTo("First Task");
    }

    Job job = managementService.createJobQuery()
        .processInstanceId(pi.getId())
        .singleResult();
    assertThat(job).isNotNull();

    // execute the timer
    managementService.executeJob(job.getId());

    // now there are 7 tasks
    taskList = taskService.createTaskQuery()
        .orderByTaskName()
        .asc()
        .list();
    assertThat(taskList).hasSize(7);

    // first task is the escalation task
    Task escalationTask = taskList.remove(0);
    assertThat(escalationTask.getName()).isEqualTo("Escalation Task 1");
    // complete it
    taskService.complete(escalationTask.getId());

    // now complete the remaining tasks
    for (Task task : taskList) {
      taskService.complete(task.getId());
    }

    // process instance is ended
    testHelper.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTimerOnConcurrentTasks() {
    String procId = runtimeService.startProcessInstanceByKey("nonInterruptingOnConcurrentTasks").getId();
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

    // Complete task that was reached by non interrupting timer
    Task task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // Complete other tasks
    for (Task t : taskService.createTaskQuery().list()) {
      taskService.complete(t.getId());
    }
    testHelper.assertProcessEnded(procId);
  }

  // Difference with previous test: now the join will be reached first
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/timer/BoundaryTimerNonInterruptingEventTest.testTimerOnConcurrentTasks.bpmn20.xml"})
  @Test
  void testTimerOnConcurrentTasks2() {
    String procId = runtimeService.startProcessInstanceByKey("nonInterruptingOnConcurrentTasks").getId();
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

    // Complete 2 tasks that will trigger the join
    Task task = taskService.createTaskQuery().taskDefinitionKey("firstTask").singleResult();
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().taskDefinitionKey("secondTask").singleResult();
    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();

    // Finally, complete the task that was created due to the timer
    task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
    taskService.complete(task.getId());

    testHelper.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testTimerWithCycle() {
    runtimeService.startProcessInstanceByKey("nonInterruptingCycle").getId();
    TaskQuery tq = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask");
    assertThat(tq.count()).isZero();
    moveByHours(1);
    assertThat(tq.count()).isOne();
    moveByHours(1);
    assertThat(tq.count()).isEqualTo(2);

    Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());

    moveByHours(1);
    assertThat(tq.count()).isEqualTo(2);
  }

  /*
   * see http://jira.codehaus.org/browse/ACT-1173
   */
  @Deployment
  @Test
  void testTimerOnEmbeddedSubprocess() {
    String id = runtimeService.startProcessInstanceByKey("nonInterruptingTimerOnEmbeddedSubprocess").getId();

    TaskQuery tq = taskService.createTaskQuery().taskAssignee("kermit");

    assertThat(tq.count()).isOne();

    // Simulate timer
    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());

    tq = taskService.createTaskQuery().taskAssignee("kermit");

    assertThat(tq.count()).isEqualTo(2);

    List<Task> tasks = tq.list();

    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    testHelper.assertProcessEnded(id);
  }

  @Deployment
  /*
   * see http://jira.codehaus.org/browse/ACT-1106
   */
  @Test
  void testReceiveTaskWithBoundaryTimer(){
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("timeCycle", "R/PT1H");
    Date startTime = ClockUtil.getCurrentTime();

    // After process start, there should be a timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingCycle",variables);

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);

    // The Execution Query should work normally and find executions in state "task"
    List<Execution> executions = runtimeService.createExecutionQuery()
                                               .activityId("task")
                                               .list();
    assertThat(executions).hasSize(1);
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(executions.get(0).getId());
    assertThat(activeActivityIds).hasSize(1);
    assertThat(activeActivityIds.get(0)).isEqualTo("task");

    runtimeService.signal(executions.get(0).getId());

    // After setting the clock to time '1 hour and 5 seconds', the second timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));

    JobExecutorWaitUtils.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, 5000L);
    assertThat(jobQuery.count()).isZero();

    // which means the process has ended
    testHelper.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTimerOnConcurrentSubprocess() {
    String procId = runtimeService.startProcessInstanceByKey("testTimerOnConcurrentSubprocess").getId();
    assertThat(taskService.createTaskQuery().count()).isEqualTo(4);

    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(5);

    // Complete 4 tasks that will trigger the join
    Task task = taskService.createTaskQuery().taskDefinitionKey("sub1task1").singleResult();
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().taskDefinitionKey("sub1task2").singleResult();
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().taskDefinitionKey("sub2task1").singleResult();
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().taskDefinitionKey("sub2task2").singleResult();
    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();

    // Finally, complete the task that was created due to the timer
    task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
    taskService.complete(task.getId());

    testHelper.assertProcessEnded(procId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/timer/BoundaryTimerNonInterruptingEventTest.testTimerOnConcurrentSubprocess.bpmn20.xml")
  @Test
  void testTimerOnConcurrentSubprocess2() {
    String procId = runtimeService.startProcessInstanceByKey("testTimerOnConcurrentSubprocess").getId();
    assertThat(taskService.createTaskQuery().count()).isEqualTo(4);

    Job timer = managementService.createJobQuery().singleResult();
    managementService.executeJob(timer.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(5);

    Task task = taskService.createTaskQuery().taskDefinitionKey("sub1task1").singleResult();
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().taskDefinitionKey("sub1task2").singleResult();
    taskService.complete(task.getId());

    // complete the task that was created due to the timer
    task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("sub2task1").singleResult();
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().taskDefinitionKey("sub2task2").singleResult();
    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().count()).isZero();

    testHelper.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testMultipleOutgoingSequenceFlows() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingTimer");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(3);

    List<Task> tasks = taskQuery.list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testHelper.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testMultipleOutgoingSequenceFlowsOnSubprocess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingTimer");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    Task task = taskService.createTaskQuery().taskDefinitionKey("innerTask1").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("innerTask2").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask1").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask2").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    testHelper.assertProcessEnded(pi.getId());

    // Case 2: fire outer tasks first

    pi = runtimeService.startProcessInstanceByKey("nonInterruptingTimer");

    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask1").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask2").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("innerTask1").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskDefinitionKey("innerTask2").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    testHelper.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testMultipleOutgoingSequenceFlowsOnSubprocessMi() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingTimer");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(10);

    List<Task> tasks = taskQuery.list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testHelper.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/timer/BoundaryTimerNonInterruptingEventTest.testTimerWithCycle.bpmn20.xml"})
  @Test
  void testTimeCycle() {
    // given
    runtimeService.startProcessInstanceByKey("nonInterruptingCycle");

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isOne();
    String jobId = jobQuery.singleResult().getId();

    // when
    managementService.executeJob(jobId);

    // then
    assertThat(jobQuery.count()).isOne();

    String anotherJobId = jobQuery.singleResult().getId();
    assertThat(anotherJobId).isNotEqualTo(jobId);
  }

  @Deployment
  @Test
  void testFailingTimeCycle() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    JobQuery failedJobQuery = managementService.createJobQuery();
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.count()).isOne();

    String jobId = jobQuery.singleResult().getId();
    failedJobQuery.jobId(jobId);

    // when (1)
    executeJobExpectingException(managementService, jobId);

    // then (1)
    Job failedJob = failedJobQuery.singleResult();
    assertThat(failedJob.getRetries()).isEqualTo(2);

    // a new timer job has been created
    assertThat(jobQuery.count()).isEqualTo(2);

    assertThat(managementService.createJobQuery().withException().count()).isOne();
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isEqualTo(2);

    // when (2)
    executeJobIgnoringException(managementService, jobId);

    // then (2)
    failedJob = failedJobQuery.singleResult();
    assertThat(failedJob.getRetries()).isEqualTo(1);

    // there are still two jobs
    assertThat(jobQuery.count()).isEqualTo(2);

    assertThat(managementService.createJobQuery().withException().count()).isOne();
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isEqualTo(2);
  }

  @Deployment
  @Test
  void testUpdateTimerRepeat() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Calendar currentTime = Calendar.getInstance();
    ClockUtil.setCurrentTime(currentTime.getTime());

    // GIVEN
    // Start process instance with a non-interrupting boundary timer event
    // on a user task
    runtimeService.startProcessInstanceByKey("timerRepeat");

    // there should be a single user task for the process instance
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("User Waiting");

    // there should be a single timer job (R5/PT1H)
    TimerEntity timerJob = (TimerEntity) managementService.createJobQuery().singleResult();
    assertThat(timerJob).isNotNull();
    assertThat(timerJob.getRepeat()).isEqualTo("R5/" + sdf.format(ClockUtil.getCurrentTime()) + "/PT1H");

    // WHEN
    // we update the repeat property of the timer job
    processEngineConfiguration.getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {

      TimerEntity timerEntity = (TimerEntity) commandContext.getProcessEngineConfiguration()
        .getManagementService()
        .createJobQuery()
        .singleResult();

      // update repeat property
      timerEntity.setRepeat("R3/PT3H");

      return null;
    });

    // THEN
    // the timer job should be updated
    TimerEntity updatedTimerJob = (TimerEntity) managementService.createJobQuery().singleResult();
    assertThat(updatedTimerJob.getRepeat()).isEqualTo("R3/PT3H");

    currentTime.add(Calendar.HOUR, 1);
    ClockUtil.setCurrentTime(currentTime.getTime());
    managementService.executeJob(timerJob.getId());

    // and when the timer executes, there should be 2 user tasks waiting
    tasks = taskService.createTaskQuery().orderByTaskCreateTime().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("User Waiting");
    assertThat(tasks.get(1).getName()).isEqualTo("Timer Fired");

    // finally, the second timer job should have a DueDate in 3 hours instead of 1 hour
    // and its repeat property should be the one we updated
    TimerEntity secondTimerJob = (TimerEntity) managementService.createJobQuery().singleResult();
    currentTime.add(Calendar.HOUR, 3);
    assertThat(secondTimerJob.getRepeat()).isEqualTo("R3/PT3H");
    assertThat(sdf.format(secondTimerJob.getDuedate())).isEqualTo(sdf.format(currentTime.getTime()));
  }

  @Test
  void shouldExecuteTimerJobOnOrAfterDueDate() {
    // given
    Date currentTime = ClockTestUtil.setClockToDateWithoutMilliseconds();
    Date timerDueDate = Date.from(currentTime.toInstant().plusMillis(3000L));

    BpmnModelInstance instance = Bpmn.createExecutableProcess("timerProcess")
                                     .startEvent()
                                       .operatonAsyncBefore()
                                     .userTask("user-task-with-timer")
                                       .boundaryEvent("non-interruption-timer")
                                         .cancelActivity(false)
                                         .timerWithDuration("R/PT3S")
                                       .endEvent()
                                       .moveToActivity("user-task-with-timer")
                                     .endEvent()
                                     .done();
    testHelper.deploy(instance);
    runtimeService.startProcessInstanceByKey("timerProcess");

    // when
    testHelper.waitForJobExecutorToProcessAllJobs(6000L);

    // then
    Job timerJob = managementService.createJobQuery()
                                    .timers()
                                    .activityId("non-interruption-timer")
                                    .singleResult();
    Task userTask = taskService.createTaskQuery().singleResult();

    // assert that the timer job is not acquirable
    assertThat(userTask).isNotNull();
    assertThat(timerJob).isNotNull();
    assertThat(timerJob.getDuedate()).isEqualTo(timerDueDate);
  }

  @Test
  @Timeout(value = 10000L, unit = TimeUnit.MILLISECONDS)
  void shouldExecuteTimeoutListenerJobOnOrAfterDueDate() {
    // given
    Date currentTime = ClockTestUtil.setClockToDateWithoutMilliseconds();
    Date timerDueDate = Date.from(currentTime.toInstant().plusMillis(3000L));

    BpmnModelInstance instance = Bpmn.createExecutableProcess("timeoutProcess")
                                     .startEvent()
                                       .operatonAsyncBefore()
                                     .userTask("user-task-with-timer")
                                       .operatonTaskListenerExpressionTimeoutWithCycle(
                                           TaskListener.EVENTNAME_TIMEOUT,
                                           "${true}",
                                           "R/PT3S")
                                     .endEvent()
                                     .done();
    testHelper.deploy(instance);
    runtimeService.startProcessInstanceByKey("timeoutProcess");

    // when
    testHelper.waitForJobExecutorToProcessAllJobs(6000L);

    // then
    Job timerJob = managementService.createJobQuery()
                                    .timers()
                                    .singleResult();
    Task userTask = taskService.createTaskQuery().singleResult();

    // assert that the timer job is not acquirable
    assertThat(userTask).isNotNull();
    assertThat(timerJob).isNotNull();
    assertThat(timerJob.getDuedate()).isEqualTo(timerDueDate);
  }

  @Test
  @Deployment(resources = {TIMER_NON_INTERRUPTING_EVENT})
  void shouldReevaluateLongerTimerCycleWhenDue() {
    // given
    ClockUtil.setCurrentTime(new Date(1457326800000L));

    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R2/PT1H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    runtimeService.startProcessInstanceByKey("nonInterruptingCycle").getId();

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isOne();
    moveByHours(1); // execute first job
    assertThat(jobQuery.count()).isOne();

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/PT2H");
    moveByHours(1); // execute second job

    // then a job is due in 2 hours
    assertThat(jobQuery.count()).isOne();
    assertThat(jobQuery.singleResult().getDuedate())
      .isCloseTo(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS), 5000);

    moveByHours(2); // execute first job of the new cycle

    // then a job is due in 2 hours
    assertThat(jobQuery.singleResult().getDuedate())
      .isCloseTo(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS), 5000);
    assertThat(jobQuery.count()).isOne();

    moveByHours(2);  // execute second job of the new cycle => no more jobs
    assertThat(jobQuery.count()).isZero();
  }

  @Test
  @Deployment(resources = {TIMER_NON_INTERRUPTING_EVENT})
  void shouldReevaluateShorterTimerCycleWhenDue() {
    // given
    ClockUtil.setCurrentTime(new Date(1457326800000L));

    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R3/PT2H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    runtimeService.startProcessInstanceByKey("nonInterruptingCycle").getId();

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isOne();
    moveByHours(2); // execute first job
    assertThat(jobQuery.count()).isOne();

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/PT1H");
    moveByHours(2); // execute second job

    // then one more job is left due in 1 hours
    assertThat(jobQuery.count()).isOne();
    assertThat(jobQuery.singleResult().getDuedate())
      .isCloseTo(new Date(ClockUtil.getCurrentTime().getTime() + ONE_HOUR), 5000);

    moveByHours(1); // execute first job of the new cycle

     // then a job is due in 1 hours
    assertThat(jobQuery.singleResult().getDuedate())
      .isCloseTo(new Date(ClockUtil.getCurrentTime().getTime() + ONE_HOUR), 5000);
    assertThat(jobQuery.count()).isOne();

    moveByHours(1); // execute second job of the new cycle => no more jobs
    assertThat(jobQuery.count()).isZero();
  }

  @Test
  @Deployment(resources = {TIMER_NON_INTERRUPTING_EVENT})
  void shouldNotReevaluateTimerCycleIfCycleDoesNotChange() {
    // given
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R2/PT1H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    runtimeService.startProcessInstanceByKey("nonInterruptingCycle").getId();

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isOne();
    moveByHours(1); // execute first job
    assertThat(jobQuery.count()).isOne();

    // when job is due
    moveByHours(1); // execute second job

    // then no more jobs are left (two jobs has been executed already)
    assertThat(managementService.createJobQuery().singleResult()).isNull();
  }

  @Test
  @Deployment(resources = {TIMER_NON_INTERRUPTING_EVENT})
  void shouldNotReevaluateTimerCycleWhenNewCycleIsIncorrect() {
    // given
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R2/PT1H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    runtimeService.startProcessInstanceByKey("nonInterruptingCycle").getId();

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isOne();
    moveByHours(1); // execute first job
    assertThat(jobQuery.count()).isOne();

    // when job is due
    myCycleTimerBean.setCycle("R2\\PT2H"); // set incorrect cycle
    moveByHours(1); // execute second job

    // then no more jobs are left (two jobs has been executed already)
    assertThat(managementService.createJobQuery().singleResult()).isNull();
  }


  protected void moveByHours(int hours) {
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + (TimeUnit.HOURS.toMillis(hours) + 5000)));
    testHelper.executeAvailableJobs(false);
  }
}
