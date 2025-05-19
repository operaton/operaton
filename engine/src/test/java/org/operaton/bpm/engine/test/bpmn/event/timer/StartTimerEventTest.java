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
package org.operaton.bpm.engine.test.bpmn.event.timer;

import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.operaton.bpm.engine.impl.cmd.DeleteJobsCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.mock.Mocks;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertNotSame;

/**
 * @author Joram Barrez
 */
public class StartTimerEventTest extends PluggableProcessEngineTest {

  protected static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1L);
  protected static final long TWO_HOURS = TimeUnit.HOURS.toMillis(2L);
  private static final Date START_DATE = new GregorianCalendar(2023, Calendar.AUGUST, 18, 8, 0, 0).getTime();

  protected boolean reevaluateTimeCycleWhenDue;

  @Before
  public void setUp() {
    reevaluateTimeCycleWhenDue = processEngineConfiguration.isReevaluateTimeCycleWhenDue();
  }

  @After
  public void tearDown() {
    processEngineConfiguration.getBeans().remove("myCycleTimerBean");
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(reevaluateTimeCycleWhenDue);
  }

  @Deployment
  @Test
  public void testDurationStartTimerEvent() {
    // Set the clock fixed
    Date startTime = new Date();

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    // After setting the clock to time '50minutes and 5 seconds', the second
    // timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((50 * 60 * 1000) + 5000)));

    executeAllJobs();

    executeAllJobs();

    List<ProcessInstance> pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").list();
    assertThat(pi).hasSize(1);
    assertThat(pi.get(0).getProcessDefinitionKey()).isEqualTo("startTimerEventExample");
    assertThat(jobQuery.count()).isZero();

  }

  @Deployment
  @Test
  public void testFixedDateStartTimerEvent() throws Exception {

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    ClockUtil.setCurrentTime(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("15/11/2036 11:12:30"));
    executeAllJobs();

    List<ProcessInstance> pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").list();
    assertThat(pi).hasSize(1);

    assertThat(jobQuery.count()).isZero();

  }

  // FIXME: This test likes to run in an endless loop when invoking the
  // waitForJobExecutorOnCondition method
  @Deployment
  @Ignore
  @Test
  public void testCycleDateStartTimerEvent() {
    ClockUtil.setCurrentTime(new Date());

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    final ProcessInstanceQuery piq = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample");

    assertThat(piq.count()).isZero();

    moveByMinutes(5);
    executeAllJobs();
    assertThat(piq.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(1);

    moveByMinutes(5);
    executeAllJobs();
    assertThat(piq.count()).isEqualTo(1);

    assertThat(jobQuery.count()).isEqualTo(1);
    // have to manually delete pending timer
//    cleanDB();

  }

  private void moveByMinutes(int minutes) {
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + ((minutes * 60 * 1000) + 5000)));
  }

  @Deployment
  @Test
  public void testCycleWithLimitStartTimerEvent() {
    ClockUtil.setCurrentTime(new Date());

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    // ensure that the deployment Id is set on the new job
    Job job = jobQuery.singleResult();
    assertThat(job.getDeploymentId()).isNotNull();

    final ProcessInstanceQuery piq = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExampleCycle");

    assertThat(piq.count()).isZero();

    moveByMinutes(5);
    executeAllJobs();
    assertThat(piq.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(1);

    // ensure that the deployment Id is set on the new job
    job = jobQuery.singleResult();
    assertThat(job.getDeploymentId()).isNotNull();

    moveByMinutes(5);
    executeAllJobs();
    assertThat(piq.count()).isEqualTo(2);
    assertThat(jobQuery.count()).isZero();

  }

  @Deployment
  @Test
  public void testPriorityInTimerCycleEvent() {
    ClockUtil.setCurrentTime(new Date());

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    // ensure that the deployment Id is set on the new job
    Job job = jobQuery.singleResult();
    assertThat(job.getDeploymentId()).isNotNull();
    assertThat(job.getPriority()).isEqualTo(9999);

    final ProcessInstanceQuery piq = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("startTimerEventExampleCycle");

    assertThat(piq.count()).isZero();

    moveByMinutes(5);
    executeAllJobs();
    assertThat(piq.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(1);

    // ensure that the deployment Id is set on the new job
    job = jobQuery.singleResult();
    assertThat(job.getDeploymentId()).isNotNull();

    // second job should have the same priority
    assertThat(job.getPriority()).isEqualTo(9999);
  }

  @Deployment
  @Test
  public void testExpressionStartTimerEvent() throws Exception {
    // ACT-1415: fixed start-date is an expression
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    ClockUtil.setCurrentTime(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("15/11/2036 11:12:30"));
    executeAllJobs();

    List<ProcessInstance> pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").list();
    assertThat(pi).hasSize(1);

    assertThat(jobQuery.count()).isZero();
  }

  @Deployment
  @Test
  public void testRecalculateExpressionStartTimerEvent() {
    // given
    JobQuery jobQuery = managementService.createJobQuery();
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample");
    assertThat(jobQuery.count()).isEqualTo(1);
    assertThat(processInstanceQuery.count()).isZero();

    Job job = jobQuery.singleResult();
    Date oldDate = job.getDuedate();

    // when
    moveByMinutes(2);
    Date currentTime = ClockUtil.getCurrentTime();
    managementService.recalculateJobDuedate(job.getId(), false);

    // then
    assertThat(jobQuery.count()).isEqualTo(1);
    assertThat(processInstanceQuery.count()).isZero();

    Date newDate = jobQuery.singleResult().getDuedate();
    assertThat(newDate).isNotEqualTo(oldDate);
    assertThat(oldDate.before(newDate)).isTrue();
    Date expectedDate = LocalDateTime.fromDateFields(currentTime).plusHours(2).toDate();
    assertThat(newDate).isCloseTo(expectedDate, 1000l);

    // move the clock forward 2 hours and 2 min
    moveByMinutes(122);
    executeAllJobs();

    List<ProcessInstance> pi = processInstanceQuery.list();
    assertThat(pi).hasSize(1);

    assertThat(jobQuery.count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/timer/StartTimerEventTest.testRecalculateExpressionStartTimerEvent.bpmn20.xml")
  @Test
  public void testRecalculateUnchangedExpressionStartTimerEventCreationDateBased() {
    // given
    JobQuery jobQuery = managementService.createJobQuery();
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample");
    assertThat(jobQuery.count()).isEqualTo(1);
    assertThat(processInstanceQuery.count()).isZero();

    // when
    moveByMinutes(1);
    managementService.recalculateJobDuedate(jobQuery.singleResult().getId(), true);

    // then due date should be based on the creation time
    assertThat(jobQuery.count()).isEqualTo(1);
    assertThat(processInstanceQuery.count()).isZero();

    Job jobUpdated = jobQuery.singleResult();
    Date expectedDate = LocalDateTime.fromDateFields(jobUpdated.getCreateTime()).plusHours(2).toDate();
    assertThat(jobUpdated.getDuedate()).isEqualTo(expectedDate);

    // move the clock forward 2 hours and 1 minute
    moveByMinutes(121);
    executeAllJobs();

    List<ProcessInstance> pi = processInstanceQuery.list();
    assertThat(pi).hasSize(1);

    assertThat(jobQuery.count()).isZero();
  }

  @Deployment
  @Test
  public void testVersionUpgradeShouldCancelJobs() {
    ClockUtil.setCurrentTime(new Date());

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    // we deploy new process version, with some small change
    InputStream in = getClass().getResourceAsStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml");
    String process = new String(IoUtil.readInputStream(in, "")).replace("beforeChange", "changed");
    IoUtil.closeSilently(in);
    in = new ByteArrayInputStream(process.getBytes());
    String id = repositoryService.createDeployment().addInputStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml", in).deploy().getId();
    IoUtil.closeSilently(in);

    assertThat(jobQuery.count()).isEqualTo(1);

    moveByMinutes(5);
    executeAllJobs();
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").singleResult();
    String pi = processInstance.getProcessInstanceId();
    assertThat(runtimeService.getActiveActivityIds(pi).get(0)).isEqualTo("changed");

    assertThat(jobQuery.count()).isEqualTo(1);

//    cleanDB();
    repositoryService.deleteDeployment(id, true);
  }

  @Deployment
  @Test
  public void testTimerShouldNotBeRecreatedOnDeploymentCacheReboot() {

    // Just to be sure, I added this test. Sounds like something that could
    // easily happen
    // when the order of deploy/parsing is altered.

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    // Reset deployment cache
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();

    // Start one instance of the process definition, this will trigger a cache
    // reload
    runtimeService.startProcessInstanceByKey("startTimer");

    // No new jobs should have been created
    assertThat(jobQuery.count()).isEqualTo(1);
  }

  // Test for ACT-1533
  @Test
  public void testTimerShouldNotBeRemovedWhenUndeployingOldVersion() {
    // Deploy test process
    InputStream in = getClass().getResourceAsStream("StartTimerEventTest.testTimerShouldNotBeRemovedWhenUndeployingOldVersion.bpmn20.xml");
    String process = new String(IoUtil.readInputStream(in, ""));
    IoUtil.closeSilently(in);

    in = new ByteArrayInputStream(process.getBytes());
    String firstDeploymentId = repositoryService.createDeployment().addInputStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml", in)
        .deploy().getId();
    IoUtil.closeSilently(in);

    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    // we deploy new process version, with some small change
    String processChanged = process.replace("beforeChange", "changed");
    in = new ByteArrayInputStream(processChanged.getBytes());
    String secondDeploymentId = repositoryService.createDeployment().addInputStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml", in)
        .deploy().getId();
    IoUtil.closeSilently(in);
    assertThat(jobQuery.count()).isEqualTo(1);

    // Remove the first deployment
    repositoryService.deleteDeployment(firstDeploymentId, true);

    // The removal of an old version should not affect timer deletion
    // ACT-1533: this was a bug, and the timer was deleted!
    assertThat(jobQuery.count()).isEqualTo(1);

    // Cleanup
    cleanDB();
    repositoryService.deleteDeployment(secondDeploymentId, true);
  }

  @Deployment
  @Test
  public void testStartTimerEventInEventSubProcess() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startTimerEventInEventSubProcess");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);
    // execute existing timer job
    managementService.executeJob(managementService.createJobQuery().list().get(0).getId());
    assertThat(jobQuery.count()).isZero();

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isZero();

    // check if execution doesn't exist because timer start event is
    // interrupting
    assertThat(executionQuery.count()).isZero();

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isZero();
  }

  @Test
  @Deployment
  public void shouldEvaluateExpressionStartTimerEventInEventSubprocess() {
    // given
    ProcessInstantiationBuilder builder = runtimeService.createProcessInstanceByKey("shouldEvaluateExpressionStartTimerEventInEventSubprocess")
        .setVariable("duration", "PT5M");

    // when
    ProcessInstance processInstance = builder.startBeforeActivity("processUserTask").execute();

    // then
    ProcessInstance startedProcessInstance = runtimeService.createProcessInstanceQuery().singleResult();
    // make sure process instance was started
    assertThat(processInstance.getId()).isEqualTo(startedProcessInstance.getId());

  }

  @Deployment
  @Test
  public void testNonInterruptingStartTimerEventInEventSubProcess() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingStartTimerEventInEventSubProcess");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);
    // execute existing job timer
    managementService.executeJob(managementService.createJobQuery().list().get(0).getId());
    assertThat(jobQuery.count()).isZero();

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task still exists because timer start event is non
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if execution still exists because timer start event is non
    // interrupting
    assertThat(executionQuery.count()).isEqualTo(1);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);
  }

  @Deployment
  @Test
  public void testStartTimerEventSubProcessInSubProcess() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startTimerEventSubProcessInSubProcess");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(2);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);
    // execute existing timer job
    managementService.executeJob(managementService.createJobQuery().list().get(0).getId());
    assertThat(jobQuery.count()).isZero();

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isZero();

    // check if execution doesn't exist because timer start event is
    // interrupting
    assertThat(executionQuery.count()).isZero();

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isZero();

  }

  @Deployment
  @Test
  public void testNonInterruptingStartTimerEventSubProcessInSubProcess() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingStartTimerEventSubProcessInSubProcess");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(2);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);
    // execute existing timer job
    managementService.executeJob(jobQuery.list().get(0).getId());
    assertThat(jobQuery.count()).isZero();

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task still exists because timer start event is non
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if execution still exists because timer start event is non
    // interrupting
    assertThat(executionQuery.count()).isEqualTo(2);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  @Deployment
  @Test
  public void testStartTimerEventWithTwoEventSubProcesses() {
    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startTimerEventWithTwoEventSubProcesses");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(2);
    // get all timer jobs ordered by dueDate
    List<Job> orderedJobList = jobQuery.orderByJobDuedate().asc().list();
    // execute first timer job
    managementService.executeJob(orderedJobList.get(0).getId());
    assertThat(jobQuery.count()).isZero();

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isZero();

    // check if execution doesn't exist because timer start event is
    // interrupting
    assertThat(executionQuery.count()).isZero();

    // check if process instance doesn't exist because timer start event is
    // interrupting
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isZero();

  }

  @Deployment
  @Test
  public void testNonInterruptingStartTimerEventWithTwoEventSubProcesses() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingStartTimerEventWithTwoEventSubProcesses");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(2);
    // get all timer jobs ordered by dueDate
    List<Job> orderedJobList = jobQuery.orderByJobDuedate().asc().list();
    // execute first timer job
    managementService.executeJob(orderedJobList.get(0).getId());
    assertThat(jobQuery.count()).isEqualTo(1);

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    DummyServiceTask.wasExecuted = false;

    // check if user task still exists because timer start event is non
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if execution still exists because timer start event is non
    // interrupting
    assertThat(executionQuery.count()).isEqualTo(1);

    // execute second timer job
    managementService.executeJob(orderedJobList.get(1).getId());
    assertThat(jobQuery.count()).isZero();

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task still exists because timer start event is non
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if execution still exists because timer event is non interrupting
    assertThat(executionQuery.count()).isEqualTo(1);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  @Deployment
  @Test
  public void testStartTimerEventSubProcessWithUserTask() {
    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startTimerEventSubProcessWithUserTask");

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(2);
    // get all timer jobs ordered by dueDate
    List<Job> orderedJobList = jobQuery.orderByJobDuedate().asc().list();
    // execute first timer job
    managementService.executeJob(orderedJobList.get(0).getId());
    assertThat(jobQuery.count()).isZero();

    // check if user task of event subprocess named "subProcess" exists
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(taskQuery.list().get(0).getTaskDefinitionKey()).isEqualTo("subprocessUserTask");

    // check if process instance exists because subprocess named "subProcess" is
    // already running
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/simpleProcessWithCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/timer/StartTimerEventTest.testStartTimerEventWithTwoEventSubProcesses.bpmn20.xml" })
  @Test
  public void testStartTimerEventSubProcessCalledFromCallActivity() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("calledProcess", "startTimerEventWithTwoEventSubProcesses");
    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleCallActivityProcess", variables);

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(2);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(2);
    // get all timer jobs ordered by dueDate
    List<Job> orderedJobList = jobQuery.orderByJobDuedate().asc().list();
    // execute first timer job
    managementService.executeJob(orderedJobList.get(0).getId());
    assertThat(jobQuery.count()).isZero();

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isZero();

    // check if execution doesn't exist because timer start event is
    // interrupting
    assertThat(executionQuery.count()).isZero();

    // check if process instance doesn't exist because timer start event is
    // interrupting
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isZero();

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/simpleProcessWithCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/timer/StartTimerEventTest.testNonInterruptingStartTimerEventWithTwoEventSubProcesses.bpmn20.xml" })
  @Test
  public void testNonInterruptingStartTimerEventSubProcessesCalledFromCallActivity() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingStartTimerEventWithTwoEventSubProcesses");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(2);
    // get all timer jobs ordered by dueDate
    List<Job> orderedJobList = jobQuery.orderByJobDuedate().asc().list();
    // execute first timer job
    managementService.executeJob(orderedJobList.get(0).getId());
    assertThat(jobQuery.count()).isEqualTo(1);

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    DummyServiceTask.wasExecuted = false;

    // check if user task still exists because timer start event is non
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if execution still exists because timer start event is non
    // interrupting
    assertThat(executionQuery.count()).isEqualTo(1);

    // execute second timer job
    managementService.executeJob(orderedJobList.get(1).getId());
    assertThat(jobQuery.count()).isZero();

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task still exists because timer start event is non
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if execution still exists because timer event is non interrupting
    assertThat(executionQuery.count()).isEqualTo(1);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  @Deployment
  @Test
  public void testStartTimerEventSubProcessInMultiInstanceSubProcess() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startTimerEventSubProcessInMultiInstanceSubProcess");

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);
    String jobIdFirstLoop = jobQuery.list().get(0).getId();
    // execute timer job
    managementService.executeJob(jobIdFirstLoop);

    assertThat(DummyServiceTask.wasExecuted).isTrue();
    DummyServiceTask.wasExecuted = false;

    // execute multiInstance loop number 2
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(1);
    String jobIdSecondLoop = jobQuery.list().get(0).getId();
    assertNotSame(jobIdFirstLoop, jobIdSecondLoop);
    // execute timer job
    managementService.executeJob(jobIdSecondLoop);

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // multiInstance loop finished
    assertThat(jobQuery.count()).isZero();

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isZero();

    // check if process instance doesn't exist because timer start event is
    // interrupting
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  public void testNonInterruptingStartTimerEventInMultiInstanceEventSubProcess() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingStartTimerEventInMultiInstanceEventSubProcess");

    // execute multiInstance loop number 1

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);
    String jobIdFirstLoop = jobQuery.list().get(0).getId();
    // execute timer job
    managementService.executeJob(jobIdFirstLoop);

    assertThat(DummyServiceTask.wasExecuted).isTrue();
    DummyServiceTask.wasExecuted = false;

    assertThat(taskQuery.count()).isEqualTo(1);
    // complete existing task to start new execution for multi instance loop
    // number 2
    taskService.complete(taskQuery.list().get(0).getId());

    // execute multiInstance loop number 2
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(1);
    String jobIdSecondLoop = jobQuery.list().get(0).getId();
    assertNotSame(jobIdFirstLoop, jobIdSecondLoop);
    // execute timer job
    managementService.executeJob(jobIdSecondLoop);

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // multiInstance loop finished
    assertThat(jobQuery.count()).isZero();

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if process instance doesn't exist because timer start event is
    // interrupting
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  @Deployment
  @Test
  public void testStartTimerEventSubProcessInParallelMultiInstanceSubProcess() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startTimerEventSubProcessInParallelMultiInstanceSubProcess");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(6);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(2);
    // execute timer job
    for (Job job : jobQuery.list()) {
      managementService.executeJob(job.getId());

      assertThat(DummyServiceTask.wasExecuted).isTrue();
      DummyServiceTask.wasExecuted = false;
    }

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isZero();

    // check if execution doesn't exist because timer start event is
    // interrupting
    assertThat(executionQuery.count()).isZero();

    // check if process instance doesn't exist because timer start event is
    // interrupting
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isZero();

  }

  @Deployment
  @Test
  public void testNonInterruptingStartTimerEventSubProcessWithParallelMultiInstance() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingParallelMultiInstance");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(6);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(2);
    // execute all timer jobs
    for (Job job : jobQuery.list()) {
      managementService.executeJob(job.getId());

      assertThat(DummyServiceTask.wasExecuted).isTrue();
      DummyServiceTask.wasExecuted = false;
    }

    assertThat(jobQuery.count()).isZero();

    // check if user task doesn't exist because timer start event is
    // interrupting
    assertThat(taskQuery.count()).isEqualTo(2);

    // check if execution doesn't exist because timer start event is
    // interrupting
    assertThat(executionQuery.count()).isEqualTo(6);

    // check if process instance doesn't exist because timer start event is
    // interrupting
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  /**
   * test scenario: - start process instance with multiInstance sequential -
   * execute interrupting timer job of event subprocess - execute non
   * interrupting timer boundary event of subprocess
   */
  @Deployment
  @Test
  public void testStartTimerEventSubProcessInMultiInstanceSubProcessWithNonInterruptingBoundaryTimerEvent() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    // 1 start timer job and 1 boundary timer job
    assertThat(jobQuery.count()).isEqualTo(2);
    // execute interrupting start timer event subprocess job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(1).getId());

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // after first interrupting start timer event sub process execution
    // multiInstance loop number 2
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(2);

    // execute non interrupting boundary timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(0).getId());

    // after non interrupting boundary timer job execution
    assertThat(jobQuery.count()).isEqualTo(1);
    assertThat(taskQuery.count()).isEqualTo(1);
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  /**
   * test scenario: - start process instance with multiInstance sequential -
   * execute interrupting timer job of event subprocess - execute interrupting
   * timer boundary event of subprocess
   */
  @Deployment
  @Test
  public void testStartTimerEventSubProcessInMultiInstanceSubProcessWithInterruptingBoundaryTimerEvent() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // execute multiInstance loop number 1
    // check if execution exists

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    // 1 start timer job and 1 boundary timer job
    assertThat(jobQuery.count()).isEqualTo(2);
    // execute interrupting start timer event subprocess job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(1).getId());

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // after first interrupting start timer event sub process execution
    // multiInstance loop number 2
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(2);

    // execute interrupting boundary timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(0).getId());

    // after interrupting boundary timer job execution
    assertThat(jobQuery.count()).isZero();
    assertThat(taskQuery.count()).isZero();

    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  public void testNonInterruptingStartTimerEventSubProcessInMultiInstanceSubProcessWithInterruptingBoundaryTimerEvent() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // execute multiInstance loop number 1
    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(3);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(1);

    JobQuery jobQuery = managementService.createJobQuery();
    // 1 start timer job and 1 boundary timer job
    assertThat(jobQuery.count()).isEqualTo(2);
    // execute non interrupting start timer event subprocess job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(1).getId());

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // complete user task to finish execution of first multiInstance loop
    assertThat(taskQuery.count()).isEqualTo(1);
    taskService.complete(taskQuery.list().get(0).getId());

    // after first non interrupting start timer event sub process execution
    // multiInstance loop number 2
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(2);

    // execute interrupting boundary timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(0).getId());

    // after interrupting boundary timer job execution
    assertThat(jobQuery.count()).isZero();
    assertThat(taskQuery.count()).isZero();
    assertThat(executionQuery.count()).isZero();
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isZero();

  }

  /**
   * test scenario: - start process instance with multiInstance parallel -
   * execute interrupting timer job of event subprocess - execute non
   * interrupting timer boundary event of subprocess
   */
  @Deployment
  @Test
  public void testStartTimerEventSubProcessInParallelMultiInstanceSubProcessWithNonInterruptingBoundaryTimerEvent() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // execute multiInstance loop number 1
    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(6);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(3);

    // execute interrupting timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(1).getId());

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // after interrupting timer job execution
    assertThat(jobQuery.count()).isEqualTo(2);
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(executionQuery.count()).isEqualTo(5);

    // execute non interrupting boundary timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(0).getId());

    // after non interrupting boundary timer job execution
    assertThat(jobQuery.count()).isEqualTo(1);
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(executionQuery.count()).isEqualTo(5);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId());
    assertThat(processInstanceQuery.count()).isEqualTo(1);

  }

  /**
   * test scenario: - start process instance with multiInstance parallel -
   * execute interrupting timer job of event subprocess - execute interrupting
   * timer boundary event of subprocess
   */
  @Deployment
  @Test
  public void testStartTimerEventSubProcessInParallelMultiInstanceSubProcessWithInterruptingBoundaryTimerEvent() {
    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // execute multiInstance loop number 1
    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(6);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(3);

    // execute interrupting timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(1).getId());

    // after interrupting timer job execution
    assertThat(jobQuery.count()).isEqualTo(2);
    assertThat(taskQuery.count()).isEqualTo(1);
    assertThat(executionQuery.count()).isEqualTo(5);

    // execute interrupting boundary timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(0).getId());

    // after interrupting boundary timer job execution
    assertThat(jobQuery.count()).isZero();
    assertThat(taskQuery.count()).isZero();
    assertThat(executionQuery.count()).isZero();

    testRule.assertProcessEnded(processInstance.getId());

  }

  /**
   * test scenario: - start process instance with multiInstance parallel -
   * execute non interrupting timer job of event subprocess - execute
   * interrupting timer boundary event of subprocess
   */
  @Deployment
  @Test
  public void testNonInterruptingStartTimerEventSubProcessInParallelMiSubProcessWithInterruptingBoundaryTimerEvent() {
    DummyServiceTask.wasExecuted = false;

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // execute multiInstance loop number 1
    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(6);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(2);

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(3);

    // execute non interrupting timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(1).getId());

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // after non interrupting timer job execution
    assertThat(jobQuery.count()).isEqualTo(2);
    assertThat(taskQuery.count()).isEqualTo(2);
    assertThat(executionQuery.count()).isEqualTo(6);

    // execute interrupting boundary timer job
    managementService.executeJob(jobQuery.orderByJobDuedate().asc().list().get(0).getId());

    // after interrupting boundary timer job execution
    assertThat(jobQuery.count()).isZero();
    assertThat(taskQuery.count()).isZero();
    assertThat(executionQuery.count()).isZero();

    testRule.assertProcessEnded(processInstance.getId());

    // start process instance again and
    // test if boundary events deleted after all tasks are completed
    processInstance = runtimeService.startProcessInstanceByKey("process");
    jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(3);

    assertThat(taskQuery.count()).isEqualTo(2);
    // complete all existing tasks
    for (Task task : taskQuery.list()) {
      taskService.complete(task.getId());
    }

    assertThat(jobQuery.count()).isZero();
    assertThat(taskQuery.count()).isZero();
    assertThat(executionQuery.count()).isZero();

    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  public void testTimeCycle() {
    // given
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    String jobId = jobQuery.singleResult().getId();

    // when
    managementService.executeJob(jobId);

    // then
    assertThat(jobQuery.count()).isEqualTo(1);

    String anotherJobId = jobQuery.singleResult().getId();
    assertThat(anotherJobId).isNotEqualTo(jobId);
  }

  @Test
  public void testRecalculateTimeCycleExpressionCurrentDateBased() {
    // given
    Mocks.register("cycle", "R/PT15M");

    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");

    BpmnModelInstance modelInstance = processBuilder
      .startEvent().timerWithCycle("${cycle}")
        .userTask("aTaskName")
      .endEvent()
      .done();

    testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("process.bpmn", modelInstance));

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    Job job = jobQuery.singleResult();
    String jobId = job.getId();
    Date oldDuedate = job.getDuedate();

    // when
    moveByMinutes(1);
    managementService.recalculateJobDuedate(jobId, false);

    // then
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(jobId);
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDuedate);
    assertThat(oldDuedate.before(jobUpdated.getDuedate())).isTrue();

    // when
    Mocks.register("cycle", "R/PT10M");
    managementService.recalculateJobDuedate(jobId, false);

    // then
    jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(jobId);
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDuedate);
    assertThat(oldDuedate.after(jobUpdated.getDuedate())).isTrue();

    Mocks.reset();
  }

  @Test
  public void testRecalculateTimeCycleExpressionCreationDateBased() {
    // given
    Mocks.register("cycle", "R/PT15M");

    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");

    BpmnModelInstance modelInstance = processBuilder
      .startEvent().timerWithCycle("${cycle}")
        .userTask("aTaskName")
      .endEvent()
      .done();

    testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("process.bpmn", modelInstance));

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    Job job = jobQuery.singleResult();
    String jobId = job.getId();
    Date oldDuedate = job.getDuedate();

    // when
    moveByMinutes(1);
    managementService.recalculateJobDuedate(jobId, true);

    // then
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(jobId);
    Date expectedDate = LocalDateTime.fromDateFields(jobUpdated.getCreateTime()).plusMinutes(15).toDate();
    assertThat(jobUpdated.getDuedate()).isEqualTo(expectedDate);

    // when
    Mocks.register("cycle", "R/PT10M");
    managementService.recalculateJobDuedate(jobId, true);

    // then
    jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(jobId);
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDuedate);
    assertThat(oldDuedate.after(jobUpdated.getDuedate())).isTrue();
    expectedDate = LocalDateTime.fromDateFields(jobUpdated.getCreateTime()).plusMinutes(10).toDate();
    assertThat(jobUpdated.getDuedate()).isEqualTo(expectedDate);

    Mocks.reset();
  }

  @Deployment
  @Test
  public void testFailingTimeCycle() {
    // given
    JobQuery query = managementService.createJobQuery();
    JobQuery failedJobQuery = managementService.createJobQuery();

    // a job to start a process instance
    assertThat(query.count()).isEqualTo(1);

    String jobId = query.singleResult().getId();
    failedJobQuery.jobId(jobId);

    moveByMinutes(5);

    // when (1)
    try {
      managementService.executeJob(jobId);
    } catch (Exception e) {
      // expected
    }

    // then (1)
    Job failedJob = failedJobQuery.singleResult();
    assertThat(failedJob.getRetries()).isEqualTo(2);

    // a new timer job has been created
    assertThat(query.count()).isEqualTo(2);

    assertThat(managementService.createJobQuery().withException().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isEqualTo(2);

    // when (2)
    try {
      managementService.executeJob(jobId);
    } catch (Exception e) {
      // expected
    }

    // then (2)
    failedJob = failedJobQuery.singleResult();
    assertThat(failedJob.getRetries()).isEqualTo(1);

    // there are still two jobs
    assertThat(query.count()).isEqualTo(2);

    assertThat(managementService.createJobQuery().withException().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isEqualTo(2);
  }

  @Deployment
  @Test
  public void testNonInterruptingTimeCycleInEventSubProcess() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    String jobId = jobQuery.singleResult().getId();

    // when
    managementService.executeJob(jobId);

    // then
    assertThat(jobQuery.count()).isEqualTo(1);

    String anotherJobId = jobQuery.singleResult().getId();
    assertThat(anotherJobId).isNotEqualTo(jobId);
  }

  @Test
  public void testInterruptingWithDurationExpression() {
    // given
    Mocks.register("duration", "PT60S");

    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");

    BpmnModelInstance modelInstance = processBuilder
      .startEvent().timerWithDuration("${duration}")
        .userTask("aTaskName")
      .endEvent()
      .done();

    testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("process.bpmn", modelInstance));

    // when
    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.executeJob(jobId);

    // then
    assertThat(taskService.createTaskQuery().taskName("aTaskName").list()).hasSize(1);

    // cleanup
    Mocks.reset();
  }

  @Test
  public void testInterruptingWithDurationExpressionInEventSubprocess() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");

    BpmnModelInstance modelInstance = processBuilder
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    processBuilder.eventSubProcess()
      .startEvent().timerWithDuration("${duration}")
        .userTask("taskInSubprocess")
      .endEvent();

    testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("process.bpmn", modelInstance));

    // when
    runtimeService.startProcessInstanceByKey("process",
      Variables.createVariables()
        .putValue("duration", "PT60S"));

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.executeJob(jobId);

    // then
    assertThat(taskService.createTaskQuery().taskName("taskInSubprocess").list()).hasSize(1);
  }

  @Test
  public void testNonInterruptingWithDurationExpressionInEventSubprocess() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");

    BpmnModelInstance modelInstance = processBuilder
      .startEvent()
        .userTask()
      .endEvent().done();

    processBuilder.eventSubProcess()
      .startEvent().interrupting(false).timerWithDuration("${duration}")
        .userTask("taskInSubprocess")
      .endEvent();

    testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("process.bpmn", modelInstance));

    // when
    runtimeService.startProcessInstanceByKey("process",
      Variables.createVariables()
        .putValue("duration", "PT60S"));

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.executeJob(jobId);

    // then
    assertThat(taskService.createTaskQuery().taskName("taskInSubprocess").list()).hasSize(1);
  }

  @Test
  public void testRecalculateNonInterruptingWithUnchangedDurationExpressionInEventSubprocessCurrentDateBased() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");

    BpmnModelInstance modelInstance = processBuilder
      .startEvent()
        .userTask()
      .endEvent().done();

    processBuilder.eventSubProcess()
      .startEvent().interrupting(false).timerWithDuration("${duration}")
        .userTask("taskInSubprocess")
      .endEvent();

    testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("process.bpmn", modelInstance));

    runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("duration", "PT70S"));

    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    String jobId = job.getId();
    Date oldDueDate = job.getDuedate();

    // when
    moveByMinutes(2);
    Date currentTime = ClockUtil.getCurrentTime();
    managementService.recalculateJobDuedate(jobId, false);

    // then
    assertThat(jobQuery.count()).isEqualTo(1L);
    Date newDuedate = jobQuery.singleResult().getDuedate();
    assertThat(newDuedate).isNotEqualTo(oldDueDate);
    assertThat(oldDueDate.before(newDuedate)).isTrue();
    Date expectedDate = LocalDateTime.fromDateFields(currentTime).plusSeconds(70).toDate();
    assertThat(newDuedate).isCloseTo(expectedDate, 1000l);

    managementService.executeJob(jobId);
    assertThat(taskService.createTaskQuery().taskName("taskInSubprocess").list()).hasSize(1);
  }

  @Test
  public void testRecalculateNonInterruptingWithChangedDurationExpressionInEventSubprocessCreationDateBased() {
    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");

    BpmnModelInstance modelInstance = processBuilder
      .startEvent()
        .userTask()
      .endEvent().done();

    processBuilder.eventSubProcess()
      .startEvent().interrupting(false).timerWithDuration("${duration}")
        .userTask("taskInSubprocess")
      .endEvent();

    testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("process.bpmn", modelInstance));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("duration", "PT60S"));

    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    String jobId = job.getId();
    Date oldDueDate = job.getDuedate();

    // when
    runtimeService.setVariable(pi.getId(), "duration", "PT2M");
    managementService.recalculateJobDuedate(jobId, true);

    // then
    assertThat(jobQuery.count()).isEqualTo(1L);
    Date newDuedate = jobQuery.singleResult().getDuedate();
    Date expectedDate = LocalDateTime.fromDateFields(jobQuery.singleResult().getCreateTime()).plusMinutes(2).toDate();
    assertThat(oldDueDate.before(newDuedate)).isTrue();
    assertThat(newDuedate).isEqualTo(expectedDate);

    managementService.executeJob(jobId);
    assertThat(taskService.createTaskQuery().taskName("taskInSubprocess").list()).hasSize(1);
  }

  @Deployment
  @Test
  public void testNonInterruptingFailingTimeCycleInEventSubProcess() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    JobQuery failedJobQuery = managementService.createJobQuery();
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.count()).isEqualTo(1);
    String jobId = jobQuery.singleResult().getId();

    failedJobQuery.jobId(jobId);

    // when (1)
    try {
      managementService.executeJob(jobId);
      fail("");
    } catch (Exception e) {
      // expected
    }

    // then (1)
    Job failedJob = failedJobQuery.singleResult();
    assertThat(failedJob.getRetries()).isEqualTo(2);

    // a new timer job has been created
    assertThat(jobQuery.count()).isEqualTo(2);

    assertThat(managementService.createJobQuery().withException().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isEqualTo(2);

    // when (2)
    try {
      managementService.executeJob(jobId);
    } catch (Exception e) {
      // expected
    }

    // then (2)
    failedJob = failedJobQuery.singleResult();
    assertThat(failedJob.getRetries()).isEqualTo(1);

    // there are still two jobs
    assertThat(jobQuery.count()).isEqualTo(2);

    assertThat(managementService.createJobQuery().withException().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isEqualTo(2);
  }

  @Test
  public void shouldReevaluateTimerCycleWhenDue() {
    // given
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R2/PT1H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();

    moveByHours(1); // execute first job

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/PT2H");
    moveByHours(1); // execute second job

    // then one more job is left due in 2 hours
    Date duedate = managementService.createJobQuery().singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));
  }

  @Test
  public void shouldNotReevaluateTimerCycle() {
    // given
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R2/PT1H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();

    // when the timer is executed twice
    moveByHours(1); // execute first job
    moveByHours(1); // execute second job

    // then no more jobs left
    assertThat(managementService.createJobQuery().singleResult()).isNull();
  }

  @Test
  public void shouldNotReevaluateTimerCycleWhenFeatureDisabled() {
    // given
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R2/PT1H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(false);

    createAndDeployProcessWithStartTimer();

    TaskQuery taskQuery = taskService.createTaskQuery().taskDefinitionKey("aTaskName");
    assertThat(taskQuery.count()).isZero();
    moveByHours(1); // execute first job
    assertThat(taskQuery.count()).isEqualTo(1);

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/PT2H");
    moveByHours(1); // execute second job

    // then no more job left
    assertThat(managementService.createJobQuery().singleResult()).isNull();
    assertThat(taskQuery.count()).isEqualTo(2);
  }

  @Test
  public void shouldReevaluateCronTimerCycleWhenDue() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("0 0 * ? * * *"); // every hour
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();

    moveByHours(1); // execute first job

    // when bean changed and job is due
    myCycleTimerBean.setCycle("0 0 0/2 ? * * *"); // at 0 minutes past the hour, every 2 hours
    moveByHours(1); // execute second job

    // then one more job is left due in 2 hours
    Date duedate = managementService.createJobQuery().singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));
  }

  @Test
  public void shouldReevaluateRepeatingToCronTimerCycle() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R2/PT1H");
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();
    moveByHours(1); // execute first job

    // when bean changed and job is due
    myCycleTimerBean.setCycle("0 0 0/2 ? * * *"); // at 0 minutes past the hour, every 2 hours
    moveByHours(1); // execute second job

    // then one more job is left due in 2 hours
    Date duedate = managementService.createJobQuery().singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));
  }

  @Test
  public void shouldReevaluateCronToRepeatingTimerCycle() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("0 0 * ? * * *"); // every hour
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();
    moveByHours(1); // execute first job

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/PT2H");
    moveByHours(1); // execute second job
    JobQuery jobQuery = managementService.createJobQuery();

    // then one more job is left due in 2 hours
    Date duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));

    moveByHours(2); // execute first job from new cycle

    // one more job is left due in 2 hours
    duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));

    moveByHours(2); // execute second job from new cycle

    // then no more jobs left
    assertThat(jobQuery.singleResult())
        .isNull();
  }

  @Test
  public void shouldReevaluateCronToRepeatingTimerCycleWithDate() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("0 0 * ? * * *"); // every hour
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();
    moveByHours(1); // execute first job, "2023/8/18 9:00:00"

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/2023-08-18T14:00/PT30M");
    moveByHours(1); // execute second job, "2023/8/18 10:00:00"
    JobQuery jobQuery = managementService.createJobQuery();

    // then one more job is left due in 4 hours
    Date duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TimeUnit.HOURS.toMillis(4L)));

    moveByHours(4); // execute first job from new cycle, "2023/8/18 14:00:00"

    // one more job is left due in 30 minutes
    duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TimeUnit.MINUTES.toMillis(30L)));

    moveByHours(1); // execute second job from new cycle

    // then no more jobs left
    assertThat(jobQuery.singleResult())
        .isNull();
  }

  @Test
  public void shouldReevaluateRepeatingTimerCycleWithDate() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R3/2023-08-18T8:00/PT1H"); // every hour
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();
    moveByHours(1); // execute first job, "2023/8/18 9:00:00"

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/PT2H");
    moveByHours(1); // execute second job, "2023/8/18 10:00:00"
    JobQuery jobQuery = managementService.createJobQuery();

    // then one more job is left due in 2 hours
    Date duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));

    moveByHours(2); // execute first job from new cycle, "2023/8/18 12:00:00"

    // one more job is left due in 2 hours
    duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
        .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));

    moveByHours(2); // execute second job from new cycle

    // then no more jobs left
    assertThat(jobQuery.singleResult())
        .isNull();
  }

  @Test
  public void shouldReevaluateRepeatingTimerCycleToTimerCycleWithDate() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    MyCycleTimerBean myCycleTimerBean = new MyCycleTimerBean("R3/PT1H"); // every hour
    processEngineConfiguration.getBeans().put("myCycleTimerBean", myCycleTimerBean);
    processEngineConfiguration.setReevaluateTimeCycleWhenDue(true);

    createAndDeployProcessWithStartTimer();
    moveByHours(1); // execute first job, "2023/8/18 9:00:00"

    // when bean changed and job is due
    myCycleTimerBean.setCycle("R2/2023-08-18T10:00/PT2H");
    moveByHours(1); // execute second job, "2023/8/18 10:00:00"
    JobQuery jobQuery = managementService.createJobQuery();

    // then one more job is left due in 2 hours
    Date duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
    .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));

    moveByHours(2); // execute first job from new cycle, "2023/8/18 12:00:00"

    // one more job is left due in 2 hours
    duedate = jobQuery.singleResult().getDuedate();
    assertThat(duedate)
    .isEqualToIgnoringMinutes(new Date(ClockUtil.getCurrentTime().getTime() + TWO_HOURS));

    moveByHours(2); // execute second job from new cycle

    // then no more jobs left
    assertThat(jobQuery.singleResult())
    .isNull();
  }


  // util methods ////////////////////////////////////////

  /**
   * executes all jobs in this threads until they are either done or retries are
   * exhausted.
   */
  protected void executeAllJobs() {
    String nextJobId = getNextExecutableJobId();

    while (nextJobId != null) {
      try {
        managementService.executeJob(nextJobId);
      } catch (Throwable t) { /* ignore */
      }
      nextJobId = getNextExecutableJobId();
    }

  }

  protected void moveByHours(int hours) {
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + (TimeUnit.HOURS.toMillis(hours) + 5000)));
    testRule.executeAvailableJobs(false);
  }

  protected String getNextExecutableJobId() {
    List<Job> jobs = managementService.createJobQuery().executable().listPage(0, 1);
    if (jobs.size() == 1) {
      return jobs.get(0).getId();
    } else {
      return null;
    }
  }

  private void cleanDB() {
    String jobId = managementService.createJobQuery().singleResult().getId();
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new DeleteJobsCmd(jobId, true));
  }

  protected void createAndDeployProcessWithStartTimer() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .timerWithCycle("#{myCycleTimerBean.getCycle()}")
        .userTask("aTaskName")
        .endEvent()
        .done());
  }

}
