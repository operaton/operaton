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
package org.operaton.bpm.engine.test.jobexecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandContextFactory;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.concurrency.ConcurrencyTestHelper.ThreadControl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for handling of new jobs created while a job is executed
 *
 * @author Thorben Lindhauer
 */
class JobExecutorFollowUpTest {

  protected static final BpmnModelInstance TWO_TASKS_PROCESS = Bpmn.createExecutableProcess("process")
    .startEvent()
    .serviceTask("serviceTask1")
      .operatonAsyncBefore()
      .operatonClass(SyncDelegate.class.getName())
    .serviceTask("serviceTask2")
      .operatonAsyncBefore()
      .operatonClass(SyncDelegate.class.getName())
    .endEvent()
    .done();

  protected static final BpmnModelInstance TWO_TASKS_DIFFERENT_PRIORITIES_PROCESS = Bpmn.createExecutableProcess("prioritizedTasksProcess")
      .startEvent()
      .serviceTask("prio20serviceTask")
        .operatonAsyncBefore()
        .operatonClass(SyncDelegate.class.getName())
        .operatonJobPriority("20")
      .serviceTask("prio10serviceTask")
        .operatonAsyncBefore()
        .operatonClass(SyncDelegate.class.getName())
        .operatonJobPriority("10")
      .endEvent()
      .done();

  protected static final BpmnModelInstance CALL_ACTIVITY_PROCESS = Bpmn.createExecutableProcess("callActivityProcess")
      .startEvent()
      .callActivity("callActivity")
        .operatonAsyncBefore()
        .calledElement("oneTaskProcess")
      .endEvent()
      .done();

  protected static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess("oneTaskProcess")
      .startEvent()
      .userTask("serviceTask")
        .operatonAsyncBefore()
      .endEvent()
      .done();

  protected static boolean skipFlushControl = true;
  
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .randomEngineName()
      .closeEngineAfterEachTest()
      .configurator(cfg -> {
        cfg.setJobExecutor(buildControllableJobExecutor());
        cfg.setCommandContextFactory(new CommandContextFactory() {
          @Override
          public CommandContext createCommandContext() {
            return new ControllableCommandContext(cfg, executionThread, skipFlushControl);
          }
        });
      })
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected static ControllableJobExecutor buildControllableJobExecutor() {
    ControllableJobExecutor jobExecutor = new ControllableJobExecutor();
    jobExecutor.setMaxJobsPerAcquisition(2);
    jobExecutor.proceedAndWaitOnShutdown(false);
    return jobExecutor;
  }

  protected ControllableJobExecutor jobExecutor;
  protected ThreadControl acquisitionThread;
  protected static ThreadControl executionThread;

  protected ProcessEngineConfigurationImpl configuration;
  protected long defaultJobExecutorPriorityRangeMin;
  protected long defaultJobExecutorPriorityRangeMax;

  @BeforeEach
  void setUp() {
    jobExecutor = (ControllableJobExecutor)
        ((ProcessEngineConfigurationImpl) engineRule.getProcessEngine().getProcessEngineConfiguration()).getJobExecutor();
    jobExecutor.setMaxJobsPerAcquisition(2);
    acquisitionThread = jobExecutor.getAcquisitionThreadControl();
    executionThread = jobExecutor.getExecutionThreadControl();

    configuration = engineRule.getProcessEngineConfiguration();
    defaultJobExecutorPriorityRangeMin = configuration.getJobExecutorPriorityRangeMin();
    defaultJobExecutorPriorityRangeMax = configuration.getJobExecutorPriorityRangeMax();
  }

  @AfterEach
  void tearDown() {
    jobExecutor.shutdown();

    configuration.setJobExecutorPriorityRangeMin(defaultJobExecutorPriorityRangeMin);
    configuration.setJobExecutorPriorityRangeMax(defaultJobExecutorPriorityRangeMax);
  }

  @Test
  void shouldExecuteExclusiveFollowUpJobInSameProcessInstance() {
    testHelper.deploy(TWO_TASKS_PROCESS);

    // given
    // a process instance with a single job
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    jobExecutor.start();

    // and first job acquisition that acquires the job
    acquisitionThread.waitForSync();
    acquisitionThread.makeContinueAndWaitForSync();
    // and first job execution
    acquisitionThread.makeContinue();

    // waiting inside delegate
    executionThread.waitForSync();

    // completing delegate
    executionThread.makeContinueAndWaitForSync();

    // then
    // the follow-up job should be executed right away
    // i.e., there is a transition instance for the second service task
    ActivityInstance activityInstance = engineRule.getRuntimeService().getActivityInstance(processInstance.getId());
    assertThat(activityInstance.getTransitionInstances("serviceTask2")).hasSize(1);

    // and the corresponding job is locked
    JobEntity followUpJob = (JobEntity) engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(followUpJob).isNotNull();
    assertThat(followUpJob.getLockOwner()).isNotNull();
    assertThat(followUpJob.getLockExpirationTime()).isNotNull();

    // and the job can be completed successfully such that the process instance ends
    executionThread.makeContinue();
    acquisitionThread.waitForSync();

    // and the process instance has finished
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void shouldExecuteExclusiveFollowUpJobInDifferentProcessInstance() {
    testHelper.deploy(CALL_ACTIVITY_PROCESS, ONE_TASK_PROCESS);

    // given
    // a process instance with a single job
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("callActivityProcess");

    jobExecutor.start();

    // and first job acquisition that acquires the job
    acquisitionThread.waitForSync();
    acquisitionThread.makeContinueAndWaitForSync();
    // and job is executed
    acquisitionThread.makeContinueAndWaitForSync();

    // then
    // the called instance has been created
    ProcessInstance calledInstance = engineRule.getRuntimeService()
      .createProcessInstanceQuery()
      .superProcessInstanceId(processInstance.getId())
      .singleResult();
    assertThat(calledInstance).isNotNull();

    // and there is a transition instance for the service task
    ActivityInstance activityInstance = engineRule.getRuntimeService().getActivityInstance(calledInstance.getId());
    assertThat(activityInstance.getTransitionInstances("serviceTask")).isNotEmpty().hasSize(1);

    // but the corresponding job is not locked
    JobEntity followUpJob = (JobEntity) engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(followUpJob).isNotNull();
    assertThat(followUpJob.getLockOwner()).isNull();
    assertThat(followUpJob.getLockExpirationTime()).isNull();
  }

  @Test
  void shouldNotExecuteExclusiveFollowUpJobWithOutOfRangePriority() {
    // given
    // first job priority = 20, second job priority = 10
    testHelper.deploy(TWO_TASKS_DIFFERENT_PRIORITIES_PROCESS);

    // allow job executor to execute only the first job
    configuration.setJobExecutorPriorityRangeMin(15L);

    // when
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("prioritizedTasksProcess");
    jobExecutor.start();
    acquireAndCompleteJob();

    // then
    ActivityInstance activityInstance = engineRule.getRuntimeService().getActivityInstance(processInstance.getId());
    assertThat(activityInstance.getTransitionInstances("prio10serviceTask")).hasSize(1);
    Execution execution = engineRule.getRuntimeService().createExecutionQuery().activityId("prio10serviceTask").singleResult();

    JobEntity followUpJob = (JobEntity) engineRule.getManagementService().createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    // the job corresponds to the second service task
    assertThat(followUpJob.getExecutionId()).isEqualTo(execution.getId());
    // the job is not locked
    assertThat(followUpJob).isNotNull();
    assertThat(followUpJob.getLockOwner()).isNull();
    assertThat(followUpJob.getLockExpirationTime()).isNull();

    // simulate another job executor with different priority range
    configuration.setJobExecutorPriorityRangeMin(5L);
    configuration.setJobExecutorPriorityRangeMax(15L);

    // complete job with modified priority range
    acquireAndCompleteJob();

    acquisitionThread.waitForSync();

    // and the process instance has finished
    testHelper.assertProcessEnded(processInstance.getId());
  }

  private void acquireAndCompleteJob() {
    // job acquisition acquires the job
    acquisitionThread.waitForSync();
    acquisitionThread.makeContinueAndWaitForSync();

    // first job execution
    acquisitionThread.makeContinue();

    // waiting inside delegate
    executionThread.waitForSync();

    // completing delegate
    // Enable a sync point for the current CommandContext so we can sync after the
    // CommandContext closed and transactions are flushed. This is to ensure the
    // flush is completed before making further assertions and not run into race conditions.
    skipFlushControl = false;
    executionThread.makeContinueAndWaitForSync();
    skipFlushControl = true;

    executionThread.makeContinue();
  }

  public static class SyncDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      executionThread.sync();
    }
  }
}
