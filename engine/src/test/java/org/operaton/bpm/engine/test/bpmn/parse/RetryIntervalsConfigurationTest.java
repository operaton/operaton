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
package org.operaton.bpm.engine.test.bpmn.parse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.AbstractAsyncOperationsTest;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.assertj.core.api.Assertions.assertThat;

class RetryIntervalsConfigurationTest extends AbstractAsyncOperationsTest {

  private static final DateTimeFormatter SIMPLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final String PROCESS_ID = "process";
  private static final String FAILING_CLASS = "this.class.does.not.Exist";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(configuration -> {
        configuration.setFailedJobRetryTimeCycle("PT5M,PT20M, PT3M");
        configuration.setEnableExceptionsAfterUnhandledBpmnError(true);
      }).build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  private static Date parseDate(String dateString) {
    LocalDateTime parsedDateTime = LocalDateTime.parse(dateString, SIMPLE_DATE_FORMATTER);
    return Date.from(parsedDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  @BeforeEach
  void setUp() {
    initDefaults(engineRule);
    engineRule.getProcessEngineConfiguration().setFailedJobRetryTimeCycle("PT5M,PT20M, PT3M");
  }

  @Test
  void testRetryGlobalConfiguration() throws Exception {
    // given global retry conf. ("PT5M,PT20M, PT3M")
    BpmnModelInstance bpmnModelInstance = prepareProcessFailingServiceTask();
    testRule.deploy(bpmnModelInstance);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    int jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(3);
    currentTime = DateUtils.addMinutes(currentTime, 5);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(2);
    currentTime = DateUtils.addMinutes(currentTime, 20);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(1);
    currentTime = DateUtils.addMinutes(currentTime, 3);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isZero();
  }

  @Test
  void testRetryGlobalConfigurationWithExecutionListener() throws Exception {
    // given
    engineRule.getProcessEngineConfiguration().setFailedJobRetryTimeCycle("PT5M");

    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess(PROCESS_ID)
    .startEvent()
    .serviceTask()
      .operatonClass(FAILING_CLASS)
      .operatonAsyncBefore()
      .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
    .endEvent()
    .done();
    testRule.deploy(bpmnModelInstance);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    int jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(1);
    currentTime = DateUtils.addMinutes(currentTime, 5);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isZero();
  }

  @Test
  void testRetryMixConfiguration() throws Exception {
    // given
    BpmnModelInstance bpmnModelInstance = prepareProcessFailingServiceTaskWithRetryCycle("R3/PT1M");

    testRule.deploy(bpmnModelInstance);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);
    assertThat(pi).isNotNull();

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    int jobRetries;

    for (int i = 0; i < 3; i++) {
      jobRetries = executeJob(processInstanceId);
      assertThat(jobRetries).isEqualTo(2 - i);
      currentTime = DateUtils.addMinutes(currentTime, 1);
      assertDueDateTime(currentTime);
      ClockUtil.setCurrentTime(currentTime);
    }
  }

  @Test
  void testRetryIntervals() throws Exception {
    // given
    BpmnModelInstance bpmnModelInstance = prepareProcessFailingServiceTaskWithRetryCycle("PT3M, PT10M,PT8M");
    testRule.deploy(bpmnModelInstance);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);
    assertThat(pi).isNotNull();

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    int jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(3);
    currentTime = DateUtils.addMinutes(currentTime, 3);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(2);
    currentTime = DateUtils.addMinutes(currentTime, 10);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(1);
    currentTime = DateUtils.addMinutes(currentTime, 8);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isZero();
  }

  @Test
  void testSingleRetryInterval() throws Exception {
    // given
    BpmnModelInstance bpmnModelInstance = prepareProcessFailingServiceTaskWithRetryCycle("PT8M ");
    testRule.deploy(bpmnModelInstance);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);
    assertThat(pi).isNotNull();

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    int jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(1);
    currentTime = DateUtils.addMinutes(currentTime, 8);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isZero();
  }

  @Test
  void testRetryWithVarList() {
    // given
    BpmnModelInstance bpmnModelInstance = prepareProcessFailingServiceTaskWithRetryCycle("${var}");
    testRule.deploy(bpmnModelInstance);

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("var", "PT1M,PT2M,PT3M,PT4M,PT5M,PT6M,PT7M,PT8M"));

    Job job = managementService.createJobQuery().singleResult();

    // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(8);
  }

  @Test
  void testIntervalsAfterUpdateRetries() throws Exception {
    // given
    BpmnModelInstance bpmnModelInstance = prepareProcessFailingServiceTaskWithRetryCycle("PT3M, PT10M,PT8M");
    testRule.deploy(bpmnModelInstance);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);
    assertThat(pi).isNotNull();

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    int jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(3);
    currentTime = DateUtils.addMinutes(currentTime, 3);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
    managementService.setJobRetries(List.of(job.getId()), 5);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(4);
    currentTime = DateUtils.addMinutes(currentTime, 3);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(3);
    currentTime = DateUtils.addMinutes(currentTime, 3);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(2);
    currentTime = DateUtils.addMinutes(currentTime, 10);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(1);
    currentTime = DateUtils.addMinutes(currentTime, 8);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isZero();
  }

  @Test
  void testMixConfigurationWithinOneProcess() throws Exception {
    // given
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess(PROCESS_ID)
        .startEvent()
        .serviceTask("Task1")
          .operatonClass(ServiceTaskDelegate.class.getName())
          .operatonAsyncBefore()
        .serviceTask("Task2")
          .operatonClass(FAILING_CLASS)
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("PT3M, PT10M,PT8M")
        .endEvent()
        .done();
    testRule.deploy(bpmnModelInstance);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);
    assertThat(pi).isNotNull();

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    // try to execute the first service task without success
    int jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(3);
    currentTime = DateUtils.addMinutes(currentTime, 5);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    ServiceTaskDelegate.firstAttempt = false;

    // finish the first service task
    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(4);

    // try to execute the second service task without success
    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(3);
    currentTime = DateUtils.addMinutes(currentTime, 3);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

  }

  @Test
  void testlocalConfigurationWithNestedChangingExpression() throws Exception {
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask()
          .operatonClass("foo")
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle("${var}")
        .endEvent()
        .done();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date startDate = simpleDateFormat.parse("2017-01-01T09:55:00");
    ClockUtil.setCurrentTime(startDate);

    testRule.deploy(bpmnModelInstance);

    VariableMap params = Variables.createVariables();
    params.putValue("var", "${nestedVar1},PT15M,${nestedVar3}");
    params.putValue("nestedVar", "PT13M");
    params.putValue("nestedVar1", "PT5M");
    params.putValue("nestedVar3", "PT25M");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", params);

    ClockUtil.setCurrentTime(parseDate("2017-01-01T09:55:00"));

    assertThat(pi).isNotNull();

    Date currentTime = parseDate("2017-01-01T10:00:00");
    ClockUtil.setCurrentTime(currentTime);

    String processInstanceId = pi.getProcessInstanceId();

    int jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(3);
    currentTime = DateUtils.addMinutes(currentTime, 5);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(2);
    currentTime = DateUtils.addMinutes(currentTime, 15);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    runtimeService.setVariable(pi.getProcessInstanceId(), "var", "${nestedVar}");

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isEqualTo(1);
    currentTime = DateUtils.addMinutes(currentTime, 13);
    assertDueDateTime(currentTime);
    ClockUtil.setCurrentTime(currentTime);

    jobRetries = executeJob(processInstanceId);
    assertThat(jobRetries).isZero();
  }

  private int executeJob(String processInstanceId) {
    Job job = fetchJob(processInstanceId);

    executeJobIgnoringException(managementService, job.getId());

    job = fetchJob(processInstanceId);

    return job.getRetries();
  }

  private void assertDueDateTime(Date expectedDate) {
    Date dueDateTime = managementService.createJobQuery().singleResult().getDuedate();
    assertThat(dueDateTime).isEqualTo(expectedDate);
  }

  private Job fetchJob(String processInstanceId) {
    return managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
  }

  private BpmnModelInstance prepareProcessFailingServiceTask() {
    return Bpmn.createExecutableProcess(PROCESS_ID)
        .startEvent()
        .serviceTask()
          .operatonClass(FAILING_CLASS)
          .operatonAsyncBefore()
        .endEvent()
        .done();
  }

  private BpmnModelInstance prepareProcessFailingServiceTaskWithRetryCycle(String retryTimeCycle) {
    return Bpmn.createExecutableProcess(PROCESS_ID)
        .startEvent()
        .serviceTask()
          .operatonClass(FAILING_CLASS)
          .operatonAsyncBefore()
          .operatonFailedJobRetryTimeCycle(retryTimeCycle)
        .endEvent()
        .done();
  }

}
