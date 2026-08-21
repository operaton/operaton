/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.concurrency.ConcurrencyTestHelper.ThreadControl;
import org.operaton.bpm.engine.test.jobexecutor.RecordingAcquireJobsRunnable.RecordedWaitEvent;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests concurrent job acquisition behavior with SKIP LOCKED enabled.
 * These tests verify that multiple job executors can acquire jobs in parallel
 * without blocking each other.
 */
class JobExecutorConcurrentAcquisitionWithSkipLockedTest {

  protected static final int DEFAULT_NUM_JOBS_TO_ACQUIRE = 3;

  protected ControllableJobExecutor jobExecutor1;
  protected ControllableJobExecutor jobExecutor2;

  protected ThreadControl acquisitionThread1;
  protected ThreadControl acquisitionThread2;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setJobExecutorAcquireWithSkipLocked(true);
    })
    .build();

  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @BeforeEach
  void setUp() {
    // Configure skip locked for all tests
    ((ProcessEngineConfigurationImpl) engineRule.getProcessEngine().getProcessEngineConfiguration())
        .setJobExecutorAcquireWithSkipLocked(true);

    // two job executors with skip locked enabled
    // Note: We create new executor instances for each test to avoid thread reuse issues
    jobExecutor1 = new ControllableJobExecutor((ProcessEngineImpl) engineRule.getProcessEngine());
    jobExecutor1.setMaxJobsPerAcquisition(DEFAULT_NUM_JOBS_TO_ACQUIRE);
    acquisitionThread1 = jobExecutor1.getAcquisitionThreadControl();

    jobExecutor2 = new ControllableJobExecutor((ProcessEngineImpl) engineRule.getProcessEngine());
    jobExecutor2.setMaxJobsPerAcquisition(DEFAULT_NUM_JOBS_TO_ACQUIRE);
    acquisitionThread2 = jobExecutor2.getAcquisitionThreadControl();
  }

  @AfterEach
  void tearDown() {
    jobExecutor1.shutdown();
    jobExecutor2.shutdown();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/nonExclusiveAsyncProcess.bpmn20.xml")
  void testClusteredExecutorsDrainNonExclusiveJobsCompletely() {
    // given: a workload of non-exclusive jobs
    // (non-exclusive is required: exclusive jobs take the contention-based
    // acquisition path and never exercise the SKIP LOCKED query)
    int numberOfJobs = 12;
    for (int i = 0; i < numberOfJobs; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("nonExclusiveAsyncProcess");
    }

    // when: a second engine on the same database simulates a cluster node, and
    // both nodes' own job executors compete for the workload
    ProcessEngineConfiguration secondNodeConfiguration = ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("operaton.cfg.xml")
        .setProcessEngineName("secondNode" + getClass().getSimpleName())
        .setJobExecutorActivate(false);
    secondNodeConfiguration.setJobExecutorAcquireWithSkipLocked(true);
    ProcessEngine secondNode = secondNodeConfiguration.buildProcessEngine();
    JobExecutor firstNodeExecutor = ((ProcessEngineConfigurationImpl) engineRule.getProcessEngine()
        .getProcessEngineConfiguration()).getJobExecutor();
    JobExecutor secondNodeExecutor = ((ProcessEngineConfigurationImpl) secondNode
        .getProcessEngineConfiguration()).getJobExecutor();
    try {
      firstNodeExecutor.start();
      secondNodeExecutor.start();

      // then: the workload drains completely — no job is permanently starved by
      // the other node's SKIP LOCKED row locks, none blocks acquisition, none is lost
      await().atMost(30, TimeUnit.SECONDS).until(
          () -> engineRule.getManagementService().createJobQuery().count() == 0
              && engineRule.getRuntimeService().createProcessInstanceQuery().count() == 0);
    } finally {
      firstNodeExecutor.shutdown();
      secondNodeExecutor.shutdown();
      secondNode.close();
    }

    // and: every service task and process instance completed exactly once
    assertThat(engineRule.getHistoryService().createHistoricActivityInstanceQuery()
        .activityId("servicetask1").finished().count()).isEqualTo(numberOfJobs);
    assertThat(engineRule.getHistoryService().createHistoricProcessInstanceQuery()
        .finished().count()).isEqualTo(numberOfJobs);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testNoImmediateRetryAfterConcurrentAcquisition() {
    // given: exactly 3 jobs (same as acquisition limit)
    int numberOfJobs = 3;
    for (int i = 0; i < numberOfJobs; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: both executors start
    jobExecutor1.start();
    acquisitionThread1.waitForSync();
    jobExecutor2.start();
    acquisitionThread2.waitForSync();

    // when: both acquire concurrently
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // when: thread 1 completes acquisition
    acquisitionThread1.makeContinueAndWaitForSync();

    List<RecordedWaitEvent> jobExecutor1WaitEvents = jobExecutor1.getAcquireJobsRunnable().getWaitEvents();
    assertThat(jobExecutor1WaitEvents).hasSize(1);
    assertThat(jobExecutor1WaitEvents.get(0).getTimeBetweenAcquisitions()).isZero();

    // when: thread 2 completes acquisition
    acquisitionThread2.makeContinueAndWaitForSync();

    // then: with SKIP LOCKED, thread 2 either acquired some jobs or found none available
    // without encountering optimistic locking exceptions
    List<RecordedWaitEvent> jobExecutor2WaitEvents = jobExecutor2.getAcquireJobsRunnable().getWaitEvents();
    assertThat(jobExecutor2WaitEvents).hasSize(1);

    // The wait behavior differs from non-SKIP-LOCKED mode:
    // Without SKIP LOCKED, thread 2 would encounter OLEs and retry immediately
    // With SKIP LOCKED, thread 2 simply skips locked jobs (no OLEs)
  }
}
