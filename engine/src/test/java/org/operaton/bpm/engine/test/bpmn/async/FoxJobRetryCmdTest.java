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
package org.operaton.bpm.engine.test.bpmn.async;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.MessageEventDefinition;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobExpectingException;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class FoxJobRetryCmdTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
          .randomEngineName()
          .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  ManagementService managementService;
  TaskService taskService;

  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedServiceTask.bpmn20.xml"})
  @Test
  void testFailedServiceTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedServiceTask");

    assertJobRetriesForActivity(pi, "failingServiceTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedUserTask.bpmn20.xml"})
  @Test
  void testFailedUserTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedUserTask");

    assertJobRetriesForActivity(pi, "failingUserTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedBusinessRuleTask.bpmn20.xml"})
  @Test
  void testFailedBusinessRuleTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedBusinessRuleTask");

    assertJobRetriesForActivity(pi, "failingBusinessRuleTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedCallActivity.bpmn20.xml"})
  @Test
  void testFailedCallActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedCallActivity");

    assertJobRetriesForActivity(pi, "failingCallActivity");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedScriptTask.bpmn20.xml"})
  @Test
  void testFailedScriptTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedScriptTask");

    assertJobRetriesForActivity(pi, "failingScriptTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedSendTask.bpmn20.xml"})
  @Test
  void testFailedSendTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedSendTask");

    assertJobRetriesForActivity(pi, "failingSendTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedSubProcess.bpmn20.xml"})
  @Test
  void testFailedSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedSubProcess");

    assertJobRetriesForActivity(pi, "failingSubProcess");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedTask.bpmn20.xml"})
  @Test
  void testFailedTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedTask");

    assertJobRetriesForActivity(pi, "failingTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedTransaction.bpmn20.xml"})
  @Test
  void testFailedTransaction() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedTask");

    assertJobRetriesForActivity(pi, "failingTransaction");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedReceiveTask.bpmn20.xml"})
  @Test
  void testFailedReceiveTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedReceiveTask");

    assertJobRetriesForActivity(pi, "failingReceiveTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedBoundaryTimerEvent.bpmn20.xml"})
  @Test
  void testFailedBoundaryTimerEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedBoundaryTimerEvent");

    assertJobRetriesForActivity(pi, "userTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedIntermediateCatchingTimerEvent.bpmn20.xml"})
  @Test
  void testFailedIntermediateCatchingTimerEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedIntermediateCatchingTimerEvent");

    assertJobRetriesForActivity(pi, "failingTimerEvent");
  }

  @Deployment
  @Test
  void testFailingMultiInstanceBody() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failingMultiInstance");

    // multi-instance body of task
    assertJobRetriesForActivity(pi, "task" + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX);
  }

  @Deployment
  @Test
  void testFailingMultiInstanceInnerActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failingMultiInstance");

    // inner activity of multi-instance body
    assertJobRetriesForActivity(pi, "task");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testBrokenFoxJobRetryValue.bpmn20.xml"})
  @Test
  void testBrokenFoxJobRetryValue() {
    Job job = managementService.createJobQuery().list().get(0);
    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isEqualTo(3);

    waitForExecutedJobWithRetriesLeft(0, job.getId());
    job = refreshJob(job.getId());
    assertThat(job.getRetries()).isZero();
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedStartTimerEvent.bpmn20.xml"})
  @Test
  void testFailedTimerStartEvent() {
    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isOne();

    Job job = managementService.createJobQuery().list().get(0);
    assertThat(job).isNotNull();
    String jobId = job.getId();
    assertThat(job.getRetries()).isEqualTo(5);

    waitForExecutedJobWithRetriesLeft(4, jobId);
    stillOneJobWithExceptionAndRetriesLeft(jobId);

    job = refreshJob(jobId);
    assertThat(job).isNotNull();

    assertThat(job.getRetries()).isEqualTo(4);

    waitForExecutedJobWithRetriesLeft(3, jobId);

    job = refreshJob(jobId);
    assertThat(job.getRetries()).isEqualTo(3);
    stillOneJobWithExceptionAndRetriesLeft(jobId);

    waitForExecutedJobWithRetriesLeft(2, jobId);

    job = refreshJob(jobId);
    assertThat(job.getRetries()).isEqualTo(2);
    stillOneJobWithExceptionAndRetriesLeft(jobId);

    waitForExecutedJobWithRetriesLeft(1, jobId);

    job = refreshJob(jobId);
    assertThat(job.getRetries()).isEqualTo(1);
    stillOneJobWithExceptionAndRetriesLeft(jobId);

    waitForExecutedJobWithRetriesLeft(0, jobId);

    job = refreshJob(jobId);
    assertThat(job.getRetries()).isZero();
    assertThat(managementService.createJobQuery().withException().count()).isOne();
    assertThat(managementService.createJobQuery().jobId(jobId).withRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isOne();
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedIntermediateThrowingSignalEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.failingSignalStart.bpmn20.xml" })
  @Test
  @Disabled("Runs into endless loop - investigate why")
  void testFailedIntermediateThrowingSignalEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedIntermediateThrowingSignalEvent");

    assertJobRetriesForActivity(pi, "failingSignalEvent");
  }

  @Deployment
  @Test
  void testRetryOnTimerStartEventInEventSubProcess() {
    runtimeService.startProcessInstanceByKey("process").getId();

    Job job = managementService.createJobQuery().singleResult();

    assertThat(job.getRetries()).isEqualTo(5);
    var jobId = job.getId();

    executeJobExpectingException(managementService, jobId);

    job = managementService.createJobQuery().singleResult();

    assertThat(job.getRetries()).isEqualTo(4);
  }

  @Test
  void testRetryOnServiceTaskLikeMessageThrowEvent() {
    // given
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .intermediateThrowEvent()
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R10/PT5S")
          .messageEventDefinition("messageDefinition")
            .message("message")
          .messageEventDefinitionDone()
        .endEvent()
        .done();

    MessageEventDefinition messageDefinition = bpmnModelInstance.getModelElementById("messageDefinition");
    messageDefinition.setOperatonClass(FailingDelegate.class.getName());

   testRule.deploy(bpmnModelInstance);

    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery().singleResult();

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(9);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedServiceTask.bpmn20.xml" })
  @Test
  @Disabled("job.getLockExpirationTime is null")
  void testFailedRetryWithTimeShift() throws Exception {
    // set date to hour before time shift (2015-10-25T03:00:00 CEST =>
    // 2015-10-25T02:00:00 CET)
    Date tenMinutesBeforeTimeShift = createDateFromLocalString("2015-10-25T02:50:00 CEST");
    Date fiveMinutesBeforeTimeShift = createDateFromLocalString("2015-10-25T02:55:00 CEST");
    Date twoMinutesBeforeTimeShift = createDateFromLocalString("2015-10-25T02:58:00 CEST");
    ClockUtil.setCurrentTime(tenMinutesBeforeTimeShift);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedServiceTask");
    assertThat(pi).isNotNull();

    // a job is acquirable
    List<AcquirableJobEntity> acquirableJobs = findAndLockAcquirableJobs();
    assertThat(acquirableJobs).hasSize(1);

    // execute job
    waitForExecutedJobWithRetriesLeft(4);

    // the job lock time is after the current time but before the time shift
    JobEntity job = (JobEntity) fetchJob(pi.getProcessInstanceId());
    assertThat(tenMinutesBeforeTimeShift.before(job.getLockExpirationTime())).isTrue();
    assertThat(job.getLockExpirationTime()).isEqualTo(fiveMinutesBeforeTimeShift);
    assertThat(twoMinutesBeforeTimeShift.after(job.getLockExpirationTime())).isTrue();

    // the job is not acquirable
    acquirableJobs = findAndLockAcquirableJobs();
    assertThat(acquirableJobs).isEmpty();

    // set clock to two minutes before time shift
    ClockUtil.setCurrentTime(twoMinutesBeforeTimeShift);

    // the job is now acquirable
    acquirableJobs = findAndLockAcquirableJobs();
    assertThat(acquirableJobs).hasSize(1);

    // execute job
    waitForExecutedJobWithRetriesLeft(3);

    // the job lock time is after the current time
    job = (JobEntity) refreshJob(job.getId());
    assertThat(twoMinutesBeforeTimeShift.before(job.getLockExpirationTime())).isTrue();

    // the job is not acquirable
    acquirableJobs = findAndLockAcquirableJobs();
    assertThat(acquirableJobs).as("Job shouldn't be acquirable").isEmpty();

    ClockUtil.reset();
  }

  @Test
  void testFailedJobRetryTimeCycleWithExpression() {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask()
          .operatonClass("foo")
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("${var}")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("var", "R10/PT5M"));

    Job job = managementService.createJobQuery().singleResult();

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(9);
  }

  @Test
  void testFailedJobRetryTimeCycleWithUndefinedVar() {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask()
          .operatonClass("foo")
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("${var}")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery().singleResult();

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(2); // default behaviour
  }

  @Test
  void testFailedJobRetryTimeCycleWithChangingExpression() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask()
          .operatonClass("foo")
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("${var}")
        .endEvent()
        .done();

    Date startDate = simpleDateFormat.parse("2017-01-01T09:55:00");
    ClockUtil.setCurrentTime(startDate);

   testRule.deploy(bpmnModelInstance);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("var", "R10/PT5M"));

    startDate = simpleDateFormat.parse("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    Job job = managementService.createJobQuery().singleResult();

    // when
    executeJobIgnoringException(managementService, job.getId());

    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(9);

    startDate = simpleDateFormat.parse("2017-01-01T10:05:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.setVariable(pi.getProcessInstanceId(), "var", "R10/PT10M");

    executeJobIgnoringException(managementService, job.getId());

    //then
    Date expectedDate = simpleDateFormat.parse("2017-01-01T10:15:00");
    Date duedateTime = managementService.createJobQuery().singleResult().getDuedate();
    assertThat(duedateTime).isEqualTo(expectedDate);
  }

  @Test
  void testRetryOnTimerStartEventWithExpression() {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
          .operatonFailedJobRetryTimeCycle("${var}")
          .timerWithDuration("PT5M")
        .serviceTask()
          .operatonClass("bar")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    Job job = managementService.createJobQuery().singleResult();

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(2); // default behaviour
  }

  @Test
  void testRetryOnAsyncStartEvent() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R5/PT5M")
        .serviceTask()
          .operatonClass("bar")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    Date startDate = simpleDateFormat.parse("2018-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();

    // assume
    assertThat(job.getRetries()).isEqualTo(5);

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

    Date expectedDate = simpleDateFormat.parse("2018-01-01T10:05:00");
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
  }

  @Test
  void testIntermediateCatchEvent() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .intermediateCatchEvent()
          .message("foo")
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R5/PT5M")
          .operatonExecutionListenerClass("start", "foo")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    Date startDate = simpleDateFormat.parse("2018-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();

    // assume
    assertThat(job.getRetries()).isEqualTo(5);

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

    Date expectedDate = simpleDateFormat.parse("2018-01-01T10:05:00");
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
  }

  @Test
  void testEndEvent() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .endEvent()
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R5/PT5M")
          .operatonExecutionListenerClass("start", "foo")
        .done();

   testRule.deploy(bpmnModelInstance);

    Date startDate = simpleDateFormat.parse("2018-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();

    // assume
    assertThat(job.getRetries()).isEqualTo(5);

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

    Date expectedDate = simpleDateFormat.parse("2018-01-01T10:05:00");
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
  }

  @Test
  void testExclusiveGateway() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .exclusiveGateway()
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R5/PT5M")
          .operatonExecutionListenerClass("start", "foo")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    Date startDate = simpleDateFormat.parse("2018-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();

    // assume
    assertThat(job.getRetries()).isEqualTo(5);

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

    Date expectedDate = simpleDateFormat.parse("2018-01-01T10:05:00");
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
  }

  @Test
  void testInclusiveGateway() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .inclusiveGateway()
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R5/PT5M")
          .operatonExecutionListenerClass("start", "foo")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    Date startDate = simpleDateFormat.parse("2018-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();

    // assume
    assertThat(job.getRetries()).isEqualTo(5);

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

    Date expectedDate = simpleDateFormat.parse("2018-01-01T10:05:00");
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
  }

  @Test
  void testEventBasedGateway() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .eventBasedGateway()
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R5/PT5M")
          .operatonExecutionListenerClass("start", "foo")
        .intermediateCatchEvent()
          .condition("${true}")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    Date startDate = simpleDateFormat.parse("2018-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();

    // assume
    assertThat(job.getRetries()).isEqualTo(5);

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

    Date expectedDate = simpleDateFormat.parse("2018-01-01T10:05:00");
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
  }

  @Test
  void testParallelGateway() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .parallelGateway()
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R5/PT5M")
          .operatonExecutionListenerClass("start", "foo")
        .endEvent()
        .done();

   testRule.deploy(bpmnModelInstance);

    Date startDate = simpleDateFormat.parse("2018-01-01T10:00:00");
    ClockUtil.setCurrentTime(startDate);

    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();

    // assume
    assertThat(job.getRetries()).isEqualTo(5);

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

    Date expectedDate = simpleDateFormat.parse("2018-01-01T10:05:00");
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
  }

  @Test
  void testFailingIntermediateBoundaryTimerJobWithCustomRetries() throws Exception {
    try {
      // given
      BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask("wait")
          .boundaryEvent("timer")
          .cancelActivity(false)
          .timerWithCycle("R4/PT1M")
          .operatonFailedJobRetryTimeCycle("R2/PT10M")
        .serviceTask("failing")
          .operatonClass("foo")
        .endEvent()
        .done();

     testRule.deploy(bpmnModelInstance);

      Date startDate = simpleDateFormat.parse("2019-01-01T10:00:00");
      ClockUtil.setCurrentTime(startDate);

      runtimeService.startProcessInstanceByKey("process");

      ClockUtil.setCurrentTime(simpleDateFormat.parse("2019-01-01T10:01:01"));

      // when the first timer is triggered
      Job firstJob = managementService.createJobQuery().singleResult();
      executeJobIgnoringException(managementService, firstJob.getId());

      // then a second job will be created for the second timer
      List<Job> jobs = managementService.createJobQuery().list();
      assertThat(jobs).hasSize(2);
      for (Job job : jobs) {
        if (job.getRetries() == 1) { // the first job already failed once
          Date expectedDate = simpleDateFormat.parse("2019-01-01T10:11:01");
          assertThat(job.getDuedate()).isEqualTo(expectedDate);
          assertThat(((JobEntity) job).getLockExpirationTime()).isNull();
        } else if (job.getRetries() == 2) { // the second job is not triggered yet
          Date expectedDate = simpleDateFormat.parse("2019-01-01T10:02:00");
          assertThat(job.getDuedate()).isEqualTo(expectedDate);
          assertThat(((JobEntity) job).getLockExpirationTime()).isNull();
        } else {
          fail("Unexpected job");
        }
      }
    } finally {
      ClockUtil.reset();
    }
  }

  @Test
  void testExecuteSecondJobWhenJobFailedWithCustomJobRetriesInSameProcess() {
    // given
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
      .startEvent()
      .parallelGateway("gwt")
        .serviceTask("failing")
          .operatonClass("foo")
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("R2/PT5M")
      .moveToNode("gwt")
        .userTask("beforePassing")
        .serviceTask("passing")
          .operatonExpression("${true}")
          .operatonAsyncBefore()
        .userTask("afterPassing")
      .done();

   testRule.deploy(bpmnModelInstance);

    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery().singleResult();
    executeJobIgnoringException(managementService, job.getId());

    Task task = taskService.createTaskQuery().taskDefinitionKey("beforePassing").singleResult();
    taskService.complete(task.getId());

    // when one failed job and one passing are present
    // only the passing should be executed
    testRule.waitForJobExecutorToProcessAllJobs(5000);

    // then the passing service task has been executed
    task = taskService.createTaskQuery().taskDefinitionKey("afterPassing").singleResult();
    assertThat(task).isNotNull();
    // and the failing job still have one retry left
    Job failedJob = managementService.createJobQuery().singleResult();
    assertThat(failedJob.getRetries()).isEqualTo(1);
    assertThat(((JobEntity) failedJob).getLockExpirationTime()).isNull();
  }

  protected void assertJobRetriesForActivity(ProcessInstance pi, String activityId) {
    assertThat(pi).isNotNull();

    waitForExecutedJobWithRetriesLeft(4);
    stillOneJobWithExceptionAndRetriesLeft();

    Job job = fetchJob(pi.getProcessInstanceId());
    assertThat(job).isNotNull();
    assertThat(job.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());

    assertThat(job.getRetries()).isEqualTo(4);

    ExecutionEntity execution = fetchExecutionEntity(pi.getProcessInstanceId(), activityId);
    assertThat(execution).isNotNull();

    waitForExecutedJobWithRetriesLeft(3);

    job = refreshJob(job.getId());
    assertThat(job.getRetries()).isEqualTo(3);
    stillOneJobWithExceptionAndRetriesLeft();

    execution = refreshExecutionEntity(execution.getId());
    assertThat(execution.getActivityId()).isEqualTo(activityId);

    waitForExecutedJobWithRetriesLeft(2);

    job = refreshJob(job.getId());
    assertThat(job.getRetries()).isEqualTo(2);
    stillOneJobWithExceptionAndRetriesLeft();

    execution = refreshExecutionEntity(execution.getId());
    assertThat(execution.getActivityId()).isEqualTo(activityId);

    waitForExecutedJobWithRetriesLeft(1);

    job = refreshJob(job.getId());
    assertThat(job.getRetries()).isEqualTo(1);
    stillOneJobWithExceptionAndRetriesLeft();

    execution = refreshExecutionEntity(execution.getId());
    assertThat(execution.getActivityId()).isEqualTo(activityId);

    waitForExecutedJobWithRetriesLeft(0);

    job = refreshJob(job.getId());
    assertThat(job.getRetries()).isZero();
    assertThat(managementService.createJobQuery().withException().count()).isOne();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isZero();
    assertThat(managementService.createJobQuery().noRetriesLeft().count()).isOne();

    execution = refreshExecutionEntity(execution.getId());
    assertThat(execution.getActivityId()).isEqualTo(activityId);
  }

  protected void waitForExecutedJobWithRetriesLeft(int retriesLeft, String jobId) {
    JobQuery jobQuery = managementService.createJobQuery();

    if (jobId != null) {
      jobQuery.jobId(jobId);
    }

    Job job = jobQuery.singleResult();

    executeJobIgnoringException(managementService, job.getId());

    // update job
    job = jobQuery.singleResult();

    if (job.getRetries() != retriesLeft) {
      waitForExecutedJobWithRetriesLeft(retriesLeft, jobId);
    }
  }

  protected void waitForExecutedJobWithRetriesLeft(final int retriesLeft) {
    waitForExecutedJobWithRetriesLeft(retriesLeft, null);
  }

  protected ExecutionEntity refreshExecutionEntity(String executionId) {
    return (ExecutionEntity) runtimeService.createExecutionQuery().executionId(executionId).singleResult();
  }

  protected ExecutionEntity fetchExecutionEntity(String processInstanceId, String activityId) {
    return (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(processInstanceId).activityId(activityId).singleResult();
  }

  protected Job refreshJob(String jobId) {
    return managementService.createJobQuery().jobId(jobId).singleResult();
  }

  protected Job fetchJob(String processInstanceId) {
    return managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
  }

  protected void stillOneJobWithExceptionAndRetriesLeft(String jobId) {
    assertThat(managementService.createJobQuery().jobId(jobId).withException().count()).isOne();
    assertThat(managementService.createJobQuery().jobId(jobId).withRetriesLeft().count()).isOne();
  }

  protected void stillOneJobWithExceptionAndRetriesLeft() {
    assertThat(managementService.createJobQuery().withException().count()).isOne();
    assertThat(managementService.createJobQuery().withRetriesLeft().count()).isOne();
  }

  protected Date createDateFromLocalString(String dateString) throws ParseException {
    // Format: 2015-10-25T02:50:00 CEST
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z", Locale.US);
    return dateFormat.parse(dateString);
  }

  protected List<AcquirableJobEntity> findAndLockAcquirableJobs() {
    return processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      List<AcquirableJobEntity> jobs = commandContext.getJobManager().findNextJobsToExecute(new Page(0, 100));
      for (AcquirableJobEntity job : jobs) {
        job.setLockOwner("test");
      }
      return jobs;
    });
  }

}
