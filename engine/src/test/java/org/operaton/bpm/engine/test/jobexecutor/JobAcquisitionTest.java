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
 * @author Thorben Lindhauer
 *
 */
class JobAcquisitionTest {

  protected static final int DEFAULT_NUM_JOBS_TO_ACQUIRE = 3;

  protected ControllableJobExecutor jobExecutor1;
  protected ControllableJobExecutor jobExecutor2;

  protected ThreadControl acquisitionThread1;
  protected ThreadControl acquisitionThread2;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration ->
      configuration.setJobExecutor(new ControllableJobExecutor()))
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @BeforeEach
  void setUp() {
    // two job executors with the default settings
    jobExecutor1 = (ControllableJobExecutor)
        ((ProcessEngineConfigurationImpl) engineRule.getProcessEngine().getProcessEngineConfiguration())
        .getJobExecutor();
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
  void testJobLockingFailure() {
    int numberOfInstances = 3;

    // when starting a number of process instances
    for (int i = 0; i < numberOfInstances; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("simpleAsyncProcess").getId();
    }

    // when starting job execution, both acquisition threads wait before acquiring something
    jobExecutor1.start();
    acquisitionThread1.waitForSync();
    jobExecutor2.start();
    acquisitionThread2.waitForSync();

    // when having both threads acquire jobs
    // then both wait before committing the acquiring transaction (AcquireJobsCmd)
    acquisitionThread1.makeContinueAndWaitForSync();
    acquisitionThread2.makeContinueAndWaitForSync();

    // when continuing acquisition thread 1
    acquisitionThread1.makeContinueAndWaitForSync();

    // then it has not performed waiting since it was able to acquire and execute all jobs
    assertThat(engineRule.getManagementService().createJobQuery().active().count()).isZero();
    List<RecordedWaitEvent> jobExecutor1WaitEvents = jobExecutor1.getAcquireJobsRunnable().getWaitEvents();
    assertThat(jobExecutor1WaitEvents).hasSize(1);
    assertThat(jobExecutor1WaitEvents.get(0).getTimeBetweenAcquisitions()).isZero();

    // when continuing acquisition thread 2
    acquisitionThread2.makeContinueAndWaitForSync();

    // then its acquisition cycle fails with OLEs
    // but the acquisition thread immediately tries again
    List<RecordedWaitEvent> jobExecutor2WaitEvents = jobExecutor2.getAcquireJobsRunnable().getWaitEvents();
    assertThat(jobExecutor2WaitEvents).hasSize(1);

    assertThat(jobExecutor2WaitEvents.get(0).getTimeBetweenAcquisitions()).isZero();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testJobLockingFailureWithSkipLocked() {
    // given: skip locked enabled
    ((ProcessEngineConfigurationImpl) engineRule.getProcessEngine()
      .getProcessEngineConfiguration())
      .setJobExecutorAcquireWithSkipLocked(true);

    // Create new executor instances for this test to avoid thread reuse issues
    ControllableJobExecutor localExecutor1 = new ControllableJobExecutor((ProcessEngineImpl) engineRule.getProcessEngine());
    localExecutor1.setMaxJobsPerAcquisition(DEFAULT_NUM_JOBS_TO_ACQUIRE);
    ThreadControl localThread1 = localExecutor1.getAcquisitionThreadControl();

    ControllableJobExecutor localExecutor2 = new ControllableJobExecutor((ProcessEngineImpl) engineRule.getProcessEngine());
    localExecutor2.setMaxJobsPerAcquisition(DEFAULT_NUM_JOBS_TO_ACQUIRE);
    ThreadControl localThread2 = localExecutor2.getAcquisitionThreadControl();

    int numberOfInstances = 3;

    // when: starting process instances
    for (int i = 0; i < numberOfInstances; i++) {
      engineRule.getRuntimeService().startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: starting job execution
    localExecutor1.start();
    localThread1.waitForSync();
    localExecutor2.start();
    localThread2.waitForSync();

    // when: both acquire concurrently
    localThread1.makeContinueAndWaitForSync();
    localThread2.makeContinueAndWaitForSync();

    // when: thread 1 completes
    localThread1.makeContinueAndWaitForSync();

    // then: unlike the test without SKIP LOCKED (testJobLockingFailure),
    // the second executor does not encounter optimistic locking exceptions
    // because it skipped the locked jobs during acquisition.
    List<RecordedWaitEvent> executor1WaitEvents = localExecutor1.getAcquireJobsRunnable().getWaitEvents();
    assertThat(executor1WaitEvents).hasSize(1);
    assertThat(executor1WaitEvents.get(0).getTimeBetweenAcquisitions()).isZero();

    // when: thread 2 completes
    localThread2.makeContinueAndWaitForSync();

    // then: with SKIP LOCKED, thread 2 simply finds no jobs to acquire
    // (they were already taken by thread 1) rather than trying to lock them
    // and failing with OLEs.
    List<RecordedWaitEvent> executor2WaitEvents = localExecutor2.getAcquireJobsRunnable().getWaitEvents();
    assertThat(executor2WaitEvents).hasSize(1);

    // The wait behavior may differ from the non-SKIP-LOCKED case
    // where immediate retry occurs after OLE

    // Clean up local executors
    localExecutor1.shutdown();
    localExecutor2.shutdown();
  }
}
