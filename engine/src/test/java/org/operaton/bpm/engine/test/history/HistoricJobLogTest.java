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
package org.operaton.bpm.engine.test.history;

import java.math.BigInteger;
import java.util.Date;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventSubprocessJobHandler;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.ExceptionUtil;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ResourceTypes;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.FailingDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ClockTestUtil;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobExpectingException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricJobLogTest {

  protected static final String CUSTOM_HOSTNAME = "TEST_HOST";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  HistoryService historyService;

  boolean defaultEnsureJobDueDateSet;
  String defaultHostname;

  @BeforeEach
  void init() {
    defaultEnsureJobDueDateSet = processEngineConfiguration.isEnsureJobDueDateNotNull();
    defaultHostname = processEngineConfiguration.getHostname();
    processEngineConfiguration.setHostname(CUSTOM_HOSTNAME);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
    processEngineConfiguration.setHostname(defaultHostname);
    ClockUtil.reset();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testCreateHistoricJobLogProperties() {
    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService
        .createJobQuery()
        .singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .creationLog()
        .singleResult();
    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getTimestamp()).isNotNull();

    assertThat(historicJob.getJobExceptionMessage()).isNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_BEFORE);
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
    assertThat(historicJob.getJobRetries()).isEqualTo(job.getRetries());
    assertThat(historicJob.getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(historicJob.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(historicJob.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(historicJob.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(historicJob.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(historicJob.getJobPriority()).isEqualTo(job.getPriority());
    assertThat(historicJob.getHostname()).containsIgnoringCase(CUSTOM_HOSTNAME);

    assertThat(historicJob.isCreationLog()).isTrue();
    assertThat(historicJob.isFailureLog()).isFalse();
    assertThat(historicJob.isSuccessLog()).isFalse();
    assertThat(historicJob.isDeletionLog()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testFailedHistoricJobLogProperties() {
    runtimeService.startProcessInstanceByKey("process");

    JobEntity job = (JobEntity) managementService
        .createJobQuery()
        .singleResult();
    var jobId = job.getId();

    executeJobExpectingException(managementService, jobId);

    job = (JobEntity) managementService.createJobQuery().jobId(job.getId()).singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .failureLog()
        .singleResult();
    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getTimestamp()).isNotNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_BEFORE);
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
    assertThat(historicJob.getJobRetries()).isEqualTo(3);
    assertThat(historicJob.getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(historicJob.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(historicJob.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(historicJob.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(historicJob.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(historicJob.getJobExceptionMessage()).isEqualTo(FailingDelegate.EXCEPTION_MESSAGE);
    assertThat(historicJob.getJobPriority()).isEqualTo(job.getPriority());
    assertThat(historicJob.getHostname()).containsIgnoringCase(CUSTOM_HOSTNAME);
    assertThat(historicJob.getFailedActivityId()).isNotNull().isEqualTo(job.getFailedActivityId());

    assertThat(historicJob.isCreationLog()).isFalse();
    assertThat(historicJob.isFailureLog()).isTrue();
    assertThat(historicJob.isSuccessLog()).isFalse();
    assertThat(historicJob.isDeletionLog()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testSuccessfulHistoricJobLogProperties() {
    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", false));

    Job job = managementService
        .createJobQuery()
        .singleResult();

    managementService.executeJob(job.getId());

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .successLog()
        .singleResult();
    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getTimestamp()).isNotNull();

    assertThat(historicJob.getJobExceptionMessage()).isNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_BEFORE);
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
    assertThat(historicJob.getJobRetries()).isEqualTo(job.getRetries());
    assertThat(historicJob.getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(historicJob.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(historicJob.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(historicJob.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(historicJob.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(historicJob.getJobPriority()).isEqualTo(job.getPriority());
    assertThat(historicJob.getHostname()).containsIgnoringCase(CUSTOM_HOSTNAME);

    assertThat(historicJob.isCreationLog()).isFalse();
    assertThat(historicJob.isFailureLog()).isFalse();
    assertThat(historicJob.isSuccessLog()).isTrue();
    assertThat(historicJob.isDeletionLog()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testDeletedHistoricJobLogProperties() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    Job job = managementService
        .createJobQuery()
        .singleResult();

    runtimeService.deleteProcessInstance(processInstanceId, null);

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .deletionLog()
        .singleResult();
    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getTimestamp()).isNotNull();

    assertThat(historicJob.getJobExceptionMessage()).isNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_BEFORE);
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
    assertThat(historicJob.getJobRetries()).isEqualTo(job.getRetries());
    assertThat(historicJob.getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(historicJob.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(historicJob.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(historicJob.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(historicJob.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(historicJob.getJobPriority()).isEqualTo(job.getPriority());
    assertThat(historicJob.getHostname()).containsIgnoringCase(CUSTOM_HOSTNAME);

    assertThat(historicJob.isCreationLog()).isFalse();
    assertThat(historicJob.isFailureLog()).isFalse();
    assertThat(historicJob.isSuccessLog()).isFalse();
    assertThat(historicJob.isDeletionLog()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testAsyncBeforeJobHandlerType() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(false);

    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService
        .createJobQuery()
        .singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(job.getId())
        .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobDueDate()).isNull();

    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_BEFORE);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testAsyncBeforeJobHandlerTypeDueDateSet() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(true);
    Date testDate = ClockTestUtil.setClockToDateWithoutMilliseconds();

    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService
      .createJobQuery()
      .singleResult();

    HistoricJobLog historicJob = historyService
      .createHistoricJobLogQuery()
      .jobId(job.getId())
      .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobDueDate()).isEqualTo(testDate);

    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_BEFORE);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testAsyncAfterJobHandlerType() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(false);

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", false));

    Job job = managementService
        .createJobQuery()
        .singleResult();

    managementService.executeJob(job.getId());

    Job anotherJob = managementService
        .createJobQuery()
        .singleResult();

    assertThat(job.getId()).isNotEqualTo(anotherJob.getId());

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(anotherJob.getId())
        .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobDueDate()).isNull();

    assertThat(historicJob.getJobDefinitionId()).isEqualTo(anotherJob.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_AFTER);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testAsyncAfterJobHandlerTypeDueDateSet() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(true);
    Date testDate = ClockTestUtil.setClockToDateWithoutMilliseconds();

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", false));

    Job job = managementService
      .createJobQuery()
      .singleResult();

    managementService.executeJob(job.getId());

    Job anotherJob = managementService
      .createJobQuery()
      .singleResult();

    assertThat(job.getId()).isNotEqualTo(anotherJob.getId());

    HistoricJobLog historicJob = historyService
      .createHistoricJobLogQuery()
      .jobId(anotherJob.getId())
      .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobDueDate()).isEqualTo(testDate);

    assertThat(historicJob.getJobDefinitionId()).isEqualTo(anotherJob.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_AFTER);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuationWithLongId.bpmn20.xml"})
  @Test
  void testSuccessfulHistoricJobLogEntryStoredForLongActivityId() {
    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", false));

    Job job = managementService
        .createJobQuery()
        .singleResult();

    managementService.executeJob(job.getId());

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .successLog()
        .singleResult();
    assertThat(historicJob).isNotNull();
    assertThat(historicJob.getActivityId())
        .isEqualToIgnoringCase("serviceTaskIdIsReallyLongAndItShouldBeMoreThan64CharsSoItWill" +
        "BlowAnyActivityIdColumnWhereSizeIs64OrLessSoWeAlignItTo255LikeEverywhereElse");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testStartTimerEvent.bpmn20.xml"})
  @Test
  void testStartTimerEventJobHandlerType() {
    Job job = managementService
        .createJobQuery()
        .singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(job.getId())
        .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("theStart");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(TimerStartEventJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo("CYCLE: 0 0/5 * * * ?");
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testStartTimerEventInsideEventSubProcess.bpmn20.xml"})
  @Test
  void testStartTimerEventInsideEventSubProcessJobHandlerType() {
    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService
        .createJobQuery()
        .singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(job.getId())
        .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("subprocessStartEvent");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(TimerStartEventSubprocessJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo("DURATION: PT1M");
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testIntermediateTimerEvent.bpmn20.xml"})
  @Test
  void testIntermediateTimerEventJobHandlerType() {
    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService
        .createJobQuery()
        .singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(job.getId())
        .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("timer");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(TimerCatchIntermediateEventJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo("DURATION: PT1M");
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testBoundaryTimerEvent.bpmn20.xml"})
  @Test
  void testBoundaryTimerEventJobHandlerType() {
    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService
        .createJobQuery()
        .singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(job.getId())
        .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("timer");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(TimerExecuteNestedActivityJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isEqualTo("DURATION: PT5M");
    assertThat(historicJob.getJobDueDate()).isEqualTo(job.getDuedate());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testCatchingSignalEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testThrowingSignalEventAsync.bpmn20.xml"
  })
  @Test
  void testCatchingSignalEventJobHandlerType() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(false);

    runtimeService.startProcessInstanceByKey("catchSignal");
    runtimeService.startProcessInstanceByKey("throwSignal");

    Job job = managementService
        .createJobQuery()
        .singleResult();

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(job.getId())
        .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobDueDate()).isNull();

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("signalEvent");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(ProcessEventJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isNull();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testCatchingSignalEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testThrowingSignalEventAsync.bpmn20.xml"
  })
  @Test
  void testCatchingSignalEventJobHandlerTypeDueDateSet() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(true);
    Date testDate = ClockTestUtil.setClockToDateWithoutMilliseconds();

    runtimeService.startProcessInstanceByKey("catchSignal");
    runtimeService.startProcessInstanceByKey("throwSignal");

    Job job = managementService
      .createJobQuery()
      .singleResult();

    HistoricJobLog historicJob = historyService
      .createHistoricJobLogQuery()
      .jobId(job.getId())
      .singleResult();

    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getJobDueDate()).isEqualTo(testDate);

    assertThat(historicJob.getJobId()).isEqualTo(job.getId());
    assertThat(historicJob.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(historicJob.getActivityId()).isEqualTo("signalEvent");
    assertThat(historicJob.getJobDefinitionType()).isEqualTo(ProcessEventJobHandler.TYPE);
    assertThat(historicJob.getJobDefinitionConfiguration()).isNull();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testCatchingSignalEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testThrowingSignalEventAsync.bpmn20.xml"
  })
  @Test
  void testCatchingSignalEventActivityId() {
    // given + when (1)
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchSignal").getId();
    runtimeService.startProcessInstanceByKey("throwSignal");

    String jobId = managementService.createJobQuery().singleResult().getId();

    // then (1)

    HistoricJobLog historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(jobId)
        .creationLog()
        .singleResult();
    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getActivityId()).isEqualTo("signalEvent");

    // when (2)
    executeJobExpectingException(managementService, jobId);

    // then (2)
    historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(jobId)
        .failureLog()
        .singleResult();
    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getActivityId()).isEqualTo("signalEvent");

    // when (3)
    runtimeService.setVariable(processInstanceId, "fail", false);
    managementService.executeJob(jobId);

    // then (3)

    historicJob = historyService
        .createHistoricJobLogQuery()
        .jobId(jobId)
        .successLog()
        .singleResult();
    assertThat(historicJob).isNotNull();

    assertThat(historicJob.getActivityId()).isEqualTo("signalEvent");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testFailedJobEvents() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);
    HistoricJobLogQuery createdQuery = historyService.createHistoricJobLogQuery().jobId(jobId).creationLog();
    HistoricJobLogQuery failedQuery = historyService.createHistoricJobLogQuery().jobId(jobId).failureLog().orderByJobRetries().desc();

    // there exists one historic job log entry
    assertThat(query.count()).isOne();
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isZero();

    // when (1)
    executeJobExpectingException(managementService, jobId);

    // then (1)
    assertThat(query.count()).isEqualTo(2);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isOne();

    HistoricJobLog createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog failedJobLogEntry = failedQuery.singleResult();
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    // when (2)
    executeJobExpectingException(managementService, jobId);

    // then (2)
    assertThat(query.count()).isEqualTo(3);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isEqualTo(2);

    createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(0);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(1);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(2);

    // when (3)
    executeJobExpectingException(managementService, jobId);

    // then (3)
    assertThat(query.count()).isEqualTo(4);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isEqualTo(3);

    createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(0);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(1);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(2);

    failedJobLogEntry = failedQuery.list().get(2);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(1);

    // when (4)
    executeJobExpectingException(managementService, jobId);

    // then (4)
    assertThat(query.count()).isEqualTo(5);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isEqualTo(4);

    createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(0);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(1);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(2);

    failedJobLogEntry = failedQuery.list().get(2);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(1);

    failedJobLogEntry = failedQuery.list().get(3);
    assertThat(failedJobLogEntry.getJobRetries()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testFailedJobEventsExecutedByJobExecutor() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);
    HistoricJobLogQuery createdQuery = historyService.createHistoricJobLogQuery().jobId(jobId).creationLog();
    HistoricJobLogQuery failedQuery = historyService.createHistoricJobLogQuery().jobId(jobId).failureLog().orderByJobRetries().desc();

    // there exists one historic job log entry
    assertThat(query.count()).isOne();
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isZero();

    // when (1)
    testRule.executeAvailableJobs();

    // then (1)
    assertThat(query.count()).isEqualTo(4);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isEqualTo(3);

    HistoricJobLog createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog failedJobLogEntry = failedQuery.list().get(0);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(1);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(2);

    failedJobLogEntry = failedQuery.list().get(2);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(1);

    // when (2)
    executeJobExpectingException(managementService, jobId);

    // then (2)
    assertThat(query.count()).isEqualTo(5);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isEqualTo(4);

    createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(0);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(1);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(2);

    failedJobLogEntry = failedQuery.list().get(2);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(1);

    failedJobLogEntry = failedQuery.list().get(3);
    assertThat(failedJobLogEntry.getJobRetries()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testSuccessfulJobEvent() {
    // given
    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", false));

    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);
    HistoricJobLogQuery createdQuery = historyService.createHistoricJobLogQuery().jobId(jobId).creationLog();
    HistoricJobLogQuery succeededQuery = historyService.createHistoricJobLogQuery().jobId(jobId).successLog();

    // there exists one historic job log entry
    assertThat(query.count()).isOne();
    assertThat(createdQuery.count()).isOne();
    assertThat(succeededQuery.count()).isZero();

    // when
    managementService.executeJob(jobId);

    // then
    assertThat(query.count()).isEqualTo(2);
    assertThat(createdQuery.count()).isOne();
    assertThat(succeededQuery.count()).isOne();

    HistoricJobLog createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog succeededJobLogEntry = succeededQuery.singleResult();
    assertThat(succeededJobLogEntry.getJobRetries()).isEqualTo(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testSuccessfulJobEventExecutedByJobExecutor() {
    // given
    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", false));

    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);
    HistoricJobLogQuery createdQuery = historyService.createHistoricJobLogQuery().jobId(jobId).creationLog();
    HistoricJobLogQuery succeededQuery = historyService.createHistoricJobLogQuery().jobId(jobId).successLog();

    // there exists one historic job log entry
    assertThat(query.count()).isOne();
    assertThat(createdQuery.count()).isOne();
    assertThat(succeededQuery.count()).isZero();

    // when
    testRule.executeAvailableJobs();

    // then
    assertThat(query.count()).isEqualTo(2);
    assertThat(createdQuery.count()).isOne();
    assertThat(succeededQuery.count()).isOne();

    HistoricJobLog createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog succeededJobLogEntry = succeededQuery.singleResult();
    assertThat(succeededJobLogEntry.getJobRetries()).isEqualTo(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testSuccessfulAndFailedJobEvents() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);
    HistoricJobLogQuery createdQuery = historyService.createHistoricJobLogQuery().jobId(jobId).creationLog();
    HistoricJobLogQuery failedQuery = historyService.createHistoricJobLogQuery().jobId(jobId).failureLog().orderByJobRetries().desc();
    HistoricJobLogQuery succeededQuery = historyService.createHistoricJobLogQuery().jobId(jobId).successLog();

    // there exists one historic job log entry
    assertThat(query.count()).isOne();
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isZero();
    assertThat(succeededQuery.count()).isZero();

    // when (1)
    executeJobExpectingException(managementService, jobId);

    // then (1)
    assertThat(query.count()).isEqualTo(2);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isOne();
    assertThat(succeededQuery.count()).isZero();

    HistoricJobLog createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog failedJobLogEntry = failedQuery.singleResult();
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    // when (2)
    executeJobExpectingException(managementService, jobId);

    // then (2)
    assertThat(query.count()).isEqualTo(3);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isEqualTo(2);
    assertThat(succeededQuery.count()).isZero();

    createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(0);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(1);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(2);

    // when (3)
    runtimeService.setVariable(processInstanceId, "fail", false);
    managementService.executeJob(jobId);

    // then (3)
    assertThat(query.count()).isEqualTo(4);
    assertThat(createdQuery.count()).isOne();
    assertThat(failedQuery.count()).isEqualTo(2);
    assertThat(succeededQuery.count()).isOne();

    createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(0);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(3);

    failedJobLogEntry = failedQuery.list().get(1);
    assertThat(failedJobLogEntry.getJobRetries()).isEqualTo(2);

    HistoricJobLog succeededJobLogEntry = succeededQuery.singleResult();
    assertThat(succeededJobLogEntry.getJobRetries()).isEqualTo(1);
  }

  @Deployment
  @Test
  void testTerminateEndEvent() {
    // given
    runtimeService.startProcessInstanceByKey("process").getId();

    String serviceTask1JobId = managementService.createJobQuery().activityId("serviceTask1").singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();
    assertThat(query.count()).isEqualTo(2);

    // serviceTask1
    HistoricJobLogQuery serviceTask1Query = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId);
    HistoricJobLogQuery serviceTask1CreatedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId).creationLog();
    HistoricJobLogQuery serviceTask1DeletedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId).deletionLog();
    HistoricJobLogQuery serviceTask1SuccessfulQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId).successLog();

    assertThat(serviceTask1Query.count()).isOne();
    assertThat(serviceTask1CreatedQuery.count()).isOne();
    assertThat(serviceTask1DeletedQuery.count()).isZero();
    assertThat(serviceTask1SuccessfulQuery.count()).isZero();

    // serviceTask2
    String serviceTask2JobId = managementService.createJobQuery().activityId("serviceTask2").singleResult().getId();

    HistoricJobLogQuery serviceTask2Query = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId);
    HistoricJobLogQuery serviceTask2CreatedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId).creationLog();
    HistoricJobLogQuery serviceTask2DeletedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId).deletionLog();
    HistoricJobLogQuery serviceTask2SuccessfulQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId).successLog();

    assertThat(serviceTask2Query.count()).isOne();
    assertThat(serviceTask2CreatedQuery.count()).isOne();
    assertThat(serviceTask2DeletedQuery.count()).isZero();
    assertThat(serviceTask2SuccessfulQuery.count()).isZero();

    // when
    managementService.executeJob(serviceTask1JobId);

    // then
    assertThat(query.count()).isEqualTo(4);

    // serviceTas1
    assertThat(serviceTask1Query.count()).isEqualTo(2);
    assertThat(serviceTask1CreatedQuery.count()).isOne();
    assertThat(serviceTask1DeletedQuery.count()).isZero();
    assertThat(serviceTask1SuccessfulQuery.count()).isOne();

    HistoricJobLog serviceTask1CreatedJobLogEntry = serviceTask1CreatedQuery.singleResult();
    assertThat(serviceTask1CreatedJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog serviceTask1SuccessfulJobLogEntry = serviceTask1SuccessfulQuery.singleResult();
    assertThat(serviceTask1SuccessfulJobLogEntry.getJobRetries()).isEqualTo(3);

    // serviceTask2
    assertThat(serviceTask2Query.count()).isEqualTo(2);
    assertThat(serviceTask2CreatedQuery.count()).isOne();
    assertThat(serviceTask2DeletedQuery.count()).isOne();
    assertThat(serviceTask2SuccessfulQuery.count()).isZero();

    HistoricJobLog serviceTask2CreatedJobLogEntry = serviceTask2CreatedQuery.singleResult();
    assertThat(serviceTask2CreatedJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog serviceTask2DeletedJobLogEntry = serviceTask2DeletedQuery.singleResult();
    assertThat(serviceTask2DeletedJobLogEntry.getJobRetries()).isEqualTo(3);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testSuperProcessWithCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricJobLogTest.testSubProcessWithErrorEndEvent.bpmn20.xml"
  })
  @Test
  void testErrorEndEventInterruptingCallActivity() {
    // given
    runtimeService.startProcessInstanceByKey("process").getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();
    assertThat(query.count()).isEqualTo(2);

    // serviceTask1
    String serviceTask1JobId = managementService.createJobQuery().activityId("serviceTask1").singleResult().getId();

    HistoricJobLogQuery serviceTask1Query = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId);
    HistoricJobLogQuery serviceTask1CreatedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId).creationLog();
    HistoricJobLogQuery serviceTask1DeletedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId).deletionLog();
    HistoricJobLogQuery serviceTask1SuccessfulQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask1JobId).successLog();

    assertThat(serviceTask1Query.count()).isOne();
    assertThat(serviceTask1CreatedQuery.count()).isOne();
    assertThat(serviceTask1DeletedQuery.count()).isZero();
    assertThat(serviceTask1SuccessfulQuery.count()).isZero();

    // serviceTask2
    String serviceTask2JobId = managementService.createJobQuery().activityId("serviceTask2").singleResult().getId();

    HistoricJobLogQuery serviceTask2Query = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId);
    HistoricJobLogQuery serviceTask2CreatedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId).creationLog();
    HistoricJobLogQuery serviceTask2DeletedQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId).deletionLog();
    HistoricJobLogQuery serviceTask2SuccessfulQuery = historyService.createHistoricJobLogQuery().jobId(serviceTask2JobId).successLog();

    assertThat(serviceTask2Query.count()).isOne();
    assertThat(serviceTask2CreatedQuery.count()).isOne();
    assertThat(serviceTask2DeletedQuery.count()).isZero();
    assertThat(serviceTask2SuccessfulQuery.count()).isZero();

    // when
    managementService.executeJob(serviceTask1JobId);

    // then
    assertThat(query.count()).isEqualTo(4);

    // serviceTask1
    assertThat(serviceTask1Query.count()).isEqualTo(2);
    assertThat(serviceTask1CreatedQuery.count()).isOne();
    assertThat(serviceTask1DeletedQuery.count()).isZero();
    assertThat(serviceTask1SuccessfulQuery.count()).isOne();

    HistoricJobLog serviceTask1CreatedJobLogEntry = serviceTask1CreatedQuery.singleResult();
    assertThat(serviceTask1CreatedJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog serviceTask1SuccessfulJobLogEntry = serviceTask1SuccessfulQuery.singleResult();
    assertThat(serviceTask1SuccessfulJobLogEntry.getJobRetries()).isEqualTo(3);

    // serviceTask2
    assertThat(serviceTask2Query.count()).isEqualTo(2);
    assertThat(serviceTask2CreatedQuery.count()).isOne();
    assertThat(serviceTask2DeletedQuery.count()).isOne();
    assertThat(serviceTask2SuccessfulQuery.count()).isZero();

    HistoricJobLog serviceTask2CreatedJobLogEntry = serviceTask2CreatedQuery.singleResult();
    assertThat(serviceTask2CreatedJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog serviceTask2DeletedJobLogEntry = serviceTask2DeletedQuery.singleResult();
    assertThat(serviceTask2DeletedJobLogEntry.getJobRetries()).isEqualTo(3);

    // there should be one task after the boundary event
    assertThat(taskService.createTaskQuery().count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testDeletedJob() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);
    HistoricJobLogQuery createdQuery = historyService.createHistoricJobLogQuery().jobId(jobId).creationLog();
    HistoricJobLogQuery deletedQuery = historyService.createHistoricJobLogQuery().jobId(jobId).deletionLog();

    // there exists one historic job log entry
    assertThat(query.count()).isOne();
    assertThat(createdQuery.count()).isOne();
    assertThat(deletedQuery.count()).isZero();

    // when
    managementService.deleteJob(jobId);

    // then
    assertThat(query.count()).isEqualTo(2);
    assertThat(createdQuery.count()).isOne();
    assertThat(deletedQuery.count()).isOne();

    HistoricJobLog createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog deletedJobLogEntry = deletedQuery.singleResult();
    assertThat(deletedJobLogEntry.getJobRetries()).isEqualTo(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testDeletedProcessInstance() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);
    HistoricJobLogQuery createdQuery = historyService.createHistoricJobLogQuery().jobId(jobId).creationLog();
    HistoricJobLogQuery deletedQuery = historyService.createHistoricJobLogQuery().jobId(jobId).deletionLog();

    // there exists one historic job log entry
    assertThat(query.count()).isOne();
    assertThat(createdQuery.count()).isOne();
    assertThat(deletedQuery.count()).isZero();

    // when
    runtimeService.deleteProcessInstance(processInstanceId, null);

    // then
    assertThat(query.count()).isEqualTo(2);
    assertThat(createdQuery.count()).isOne();
    assertThat(deletedQuery.count()).isOne();

    HistoricJobLog createdJobLogEntry = createdQuery.singleResult();
    assertThat(createdJobLogEntry.getJobRetries()).isEqualTo(3);

    HistoricJobLog deletedJobLogEntry = deletedQuery.singleResult();
    assertThat(deletedJobLogEntry.getJobRetries()).isEqualTo(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testExceptionStacktrace() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    executeJobExpectingException(managementService, jobId);

    // then
    String failedHistoricJobLogId = historyService
        .createHistoricJobLogQuery()
        .failureLog()
        .singleResult()
        .getId();

    String stacktrace = historyService.getHistoricJobLogExceptionStacktrace(failedHistoricJobLogId);
    assertThat(stacktrace)
      .isNotNull()
      .containsIgnoringCase(FailingDelegate.EXCEPTION_MESSAGE);
  }

  @Test
  void shouldGetJobExceptionStacktraceUnexistingJobId() {
    // given
    String unexistingJobId = "unexistingjob";

    // when/then
    assertThatThrownBy(() -> historyService.getHistoricJobLogExceptionStacktrace(unexistingJobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No historic job log found with id unexistingjob");
  }

  @Test
  void shouldGetJobExceptionStacktraceNullJobId() {
    // when/then
    assertThatThrownBy(() -> historyService.getHistoricJobLogExceptionStacktrace(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historicJobLogId is null");
  }

  @Deployment
  @Test
  void testDifferentExceptions() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when (1)
    executeJobExpectingException(managementService, jobId);

    // then (1)
    HistoricJobLog serviceTask1FailedHistoricJobLog = historyService
        .createHistoricJobLogQuery()
        .failureLog()
        .singleResult();

    String serviceTask1FailedHistoricJobLogId = serviceTask1FailedHistoricJobLog.getId();

    assertThat(serviceTask1FailedHistoricJobLog.getJobExceptionMessage()).isEqualTo(FirstFailingDelegate.FIRST_EXCEPTION_MESSAGE);

    String serviceTask1Stacktrace = historyService.getHistoricJobLogExceptionStacktrace(serviceTask1FailedHistoricJobLogId);
    assertThat(serviceTask1Stacktrace)
      .isNotNull()
      .containsIgnoringCase(FirstFailingDelegate.FIRST_EXCEPTION_MESSAGE)
      .containsIgnoringCase(FirstFailingDelegate.class.getName());

    // when (2)
    runtimeService.setVariable(processInstanceId, "firstFail", false);
    executeJobExpectingException(managementService, jobId);

    // then (2)
    HistoricJobLog serviceTask2FailedHistoricJobLog = historyService
        .createHistoricJobLogQuery()
        .failureLog()
        .orderByJobRetries()
        .desc()
        .list()
        .get(1);

    String serviceTask2FailedHistoricJobLogId = serviceTask2FailedHistoricJobLog.getId();

    assertThat(serviceTask2FailedHistoricJobLog.getJobExceptionMessage()).isEqualTo(SecondFailingDelegate.SECOND_EXCEPTION_MESSAGE);

    String serviceTask2Stacktrace = historyService.getHistoricJobLogExceptionStacktrace(serviceTask2FailedHistoricJobLogId);
    assertThat(serviceTask2Stacktrace)
      .isNotNull()
      .containsIgnoringCase(SecondFailingDelegate.SECOND_EXCEPTION_MESSAGE)
      .containsIgnoringCase(SecondFailingDelegate.class.getName());

    assertThat(serviceTask1Stacktrace).isNotEqualTo(serviceTask2Stacktrace);
  }

  @Deployment
  @Test
  void testThrowExceptionWithoutMessage() {
    // given
    runtimeService.startProcessInstanceByKey("process").getId();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    executeJobExpectingException(managementService, jobId);

    // then
    HistoricJobLog failedHistoricJobLog = historyService
        .createHistoricJobLogQuery()
        .failureLog()
        .singleResult();

    String failedHistoricJobLogId = failedHistoricJobLog.getId();

    assertThat(failedHistoricJobLog.getJobExceptionMessage()).isNull();

    String stacktrace = historyService.getHistoricJobLogExceptionStacktrace(failedHistoricJobLogId);
    assertThat(stacktrace)
      .isNotNull()
      .containsIgnoringCase(ThrowExceptionWithoutMessageDelegate.class.getName());
  }

  @Deployment
  @Test
  void testThrowExceptionMessageTruncation() {
    // given
    // a random string of size 10000 using characters [0-1]
    String exceptionMessage = new BigInteger(10000, new Random()).toString(2);
    ThrowExceptionWithOverlongMessageDelegate delegate =
        new ThrowExceptionWithOverlongMessageDelegate(exceptionMessage);

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("delegate", delegate));
    Job job = managementService.createJobQuery().singleResult();
    var jobId = job.getId();

    // when
    assertThatCode(() -> managementService.executeJob(jobId)).isInstanceOf(RuntimeException.class);

    // then
    HistoricJobLog failedHistoricJobLog = historyService
        .createHistoricJobLogQuery()
        .failureLog()
        .singleResult();

    assertThat(failedHistoricJobLog).isNotNull();
    assertThat(failedHistoricJobLog.getJobExceptionMessage())
        .isEqualToIgnoringCase(exceptionMessage.substring(0, StringUtil.DB_MAX_STRING_LENGTH));
  }

  @Test
  void testAsyncAfterJobDefinitionAfterEngineRestart() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .manualTask()
      .operatonAsyncBefore()
      .operatonAsyncAfter()
      .endEvent()
      .done();

    testRule.deploy(modelInstance);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    JobDefinition asyncBeforeJobDef = managementService.createJobDefinitionQuery()
        .jobConfiguration("async-before").singleResult();
    JobDefinition asyncAfterJobDef = managementService.createJobDefinitionQuery()
        .jobConfiguration("async-after").singleResult();

    // clearing the deployment cache as if the engine had restarted
    DeploymentCache deploymentCache = processEngineConfiguration.getDeploymentCache();
    deploymentCache.removeProcessDefinition(processInstance.getProcessDefinitionId());

    // when
    Job asyncBeforeJob = managementService.createJobQuery().singleResult();
    managementService.executeJob(asyncBeforeJob.getId());

    Job asyncAfterJob = managementService.createJobQuery().singleResult();
    managementService.executeJob(asyncAfterJob.getId());

    // then
    assertThat(asyncBeforeJob.getJobDefinitionId()).isEqualTo(asyncBeforeJobDef.getId());
    assertThat(asyncAfterJob.getJobDefinitionId()).isEqualTo(asyncAfterJobDef.getId());

    HistoricJobLog asyncBeforeLog = historyService.createHistoricJobLogQuery()
        .creationLog().jobId(asyncBeforeJob.getId()).singleResult();
    assertThat(asyncBeforeLog.getJobDefinitionId()).isEqualTo(asyncBeforeJobDef.getId());

    HistoricJobLog asyncAfterLog = historyService.createHistoricJobLogQuery()
        .creationLog().jobId(asyncAfterJob.getId()).singleResult();
    assertThat(asyncAfterLog.getJobDefinitionId()).isEqualTo(asyncAfterJobDef.getId());
  }

  @Test
  void testDeleteByteArray() {
    final String processDefinitionId = "myProcessDefition";

    processEngineConfiguration.getCommandExecutorTxRequiresNew().execute((Command<Void>) commandContext -> {

      for (int i = 0; i < 1234; i++) {
        HistoricJobLogEventEntity log = new HistoricJobLogEventEntity();
        log.setJobId(String.valueOf(i));
        log.setTimestamp(new Date());
        log.setJobDefinitionType(MessageEntity.TYPE);
        log.setProcessDefinitionId(processDefinitionId);


        byte[] aByteValue = StringUtil.toByteArray("abc");
        ByteArrayEntity byteArray = ExceptionUtil.createJobExceptionByteArray(aByteValue, ResourceTypes.HISTORY);
        log.setExceptionByteArrayId(byteArray.getId());

        commandContext
          .getHistoricJobLogManager()
          .insert(log);
      }

      return null;
    });

    assertThat(historyService.createHistoricJobLogQuery().count()).isEqualTo(1234);

    processEngineConfiguration.getCommandExecutorTxRequiresNew().execute((Command<Void>) commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByProcessDefinitionId(processDefinitionId);
      return null;
    });

    assertThat(historyService.createHistoricJobLogQuery().count()).isZero();
  }

}
