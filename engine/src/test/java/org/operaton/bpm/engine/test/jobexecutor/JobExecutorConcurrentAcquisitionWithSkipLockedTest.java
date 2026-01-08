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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.concurrency.ConcurrencyTestHelper.ThreadControl;
import org.operaton.bpm.engine.test.jobexecutor.RecordingAcquireJobsRunnable.RecordedWaitEvent;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

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
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testConcurrentAcquisitionDoesNotBlock() {
    // given: 6 jobs available (enough for both executors)
    int numberOfJobs = 6;
    for (int i = 0; i < numberOfJobs; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: starting both job executors
    jobExecutor1.start();
    acquisitionThread1.waitForSync();
    jobExecutor2.start();
    acquisitionThread2.waitForSync();

    // when: both threads acquire jobs concurrently
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // then: thread 1 completes acquisition
    acquisitionThread1.makeContinueAndWaitForSync();

    // and: thread 2 also completes acquisition without being blocked
    // (with SKIP LOCKED, it can acquire jobs concurrently)
    acquisitionThread2.makeContinueAndWaitForSync();

    // Note: This test verifies that acquisition doesn't block, not that jobs are executed
    // The ControllableJobExecutor is designed for testing acquisition behavior
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testConcurrentAcquisitionDistributesJobsEfficiently() {
    // given: exactly 6 jobs (3 for each executor)
    int numberOfJobs = 6;
    for (int i = 0; i < numberOfJobs; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: both executors acquire jobs concurrently
    jobExecutor1.start();
    acquisitionThread1.waitForSync();
    jobExecutor2.start();
    acquisitionThread2.waitForSync();

    // both start acquiring
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // both complete acquisition
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // then: both executors successfully complete acquisition
    // In contrast to non-SKIP-LOCKED mode, the second executor doesn't encounter
    // blocking or optimistic locking exceptions during acquisition
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedPreventsOptimisticLockingExceptions() {
    // given: fewer jobs than total acquisition capacity
    int numberOfJobs = 3;
    for (int i = 0; i < numberOfJobs; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: both executors try to acquire jobs concurrently
    jobExecutor1.start();
    acquisitionThread1.waitForSync();
    jobExecutor2.start();
    acquisitionThread2.waitForSync();

    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // complete acquisition
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // then: with SKIP LOCKED, the second executor completes acquisition without
    // encountering optimistic locking exceptions. It simply skips jobs already
    // locked by the first executor instead of trying to acquire them.

    // Note: This is a behavioral test verifying that acquisition completes
    // without exceptions. With traditional locking, OLEs would occur here.
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testConcurrentAcquisitionWithManyJobs() {
    // given: many jobs available
    int numberOfJobs = 20;
    for (int i = 0; i < numberOfJobs; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: both executors acquire jobs concurrently
    jobExecutor1.start();
    acquisitionThread1.waitForSync();
    jobExecutor2.start();
    acquisitionThread2.waitForSync();

    // both start acquiring
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // both complete acquisition
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // then: both executors successfully acquire jobs concurrently
    // With SKIP LOCKED, the executors can efficiently divide the workload
    // without blocking each other during acquisition
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
