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
package org.operaton.bpm.engine.test.api.mgmt.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.jobexecutor.CallerRunsRejectedJobsHandler;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.concurrency.ConcurrencyTestHelper.ThreadControl;
import org.operaton.bpm.engine.test.jobexecutor.ControllableJobExecutor;
import org.operaton.bpm.engine.variable.Variables;


/**
 * @author Thorben Lindhauer
 *
 */
class JobExecutorMetricsTest extends AbstractMetricsTest {

  protected JobExecutor defaultJobExecutor;
  protected ProcessEngine processEngine;

  @BeforeEach
  void saveJobExecutor() {
    processEngine = engineRule.getProcessEngine();
    defaultJobExecutor = processEngineConfiguration.getJobExecutor();

    // Deletes all Metrics
    processEngine.getManagementService().deleteMetrics(null);
  }

  @AfterEach
  void restoreJobExecutor() {
    processEngineConfiguration.setJobExecutor(defaultJobExecutor);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/metrics/asyncServiceTaskProcess.bpmn20.xml")
  @Test
  void testJobAcquisitionMetricReporting() {

    // given
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("asyncServiceTaskProcess");
    }

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000);
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // then
    long acquisitionAttempts = managementService.createMetricsQuery().name(Metrics.JOB_ACQUISITION_ATTEMPT).sum();
    assertThat(acquisitionAttempts).isPositive();

    long acquiredJobs = managementService.createMetricsQuery()
        .name(Metrics.JOB_ACQUIRED_SUCCESS).sum();
    assertThat(acquiredJobs).isEqualTo(3);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/metrics/asyncServiceTaskProcess.bpmn20.xml")
  @Test
  void testCompetingJobAcquisitionMetricReporting() {
    // given
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("asyncServiceTaskProcess");
    }

    // replace job executor
    ControllableJobExecutor jobExecutor1 = new ControllableJobExecutor((ProcessEngineImpl) processEngine);
    processEngineConfiguration.setJobExecutor(jobExecutor1);
    ControllableJobExecutor jobExecutor2 = new ControllableJobExecutor((ProcessEngineImpl) processEngine);

    ThreadControl jobAcquisitionThread1 = jobExecutor1.getAcquisitionThreadControl();
    ThreadControl jobAcquisitionThread2 = jobExecutor2.getAcquisitionThreadControl();

    // when both executors are waiting to finish acquisition
    jobExecutor1.start();
    jobAcquisitionThread1.waitForSync(); // wait before starting acquisition
    jobAcquisitionThread1.makeContinueAndWaitForSync(); // wait before finishing acquisition

    jobExecutor2.start();
    jobAcquisitionThread2.waitForSync(); // wait before starting acquisition
    jobAcquisitionThread2.makeContinueAndWaitForSync(); // wait before finishing acquisition

    // thread 1 is able to acquire all jobs
    jobAcquisitionThread1.makeContinueAndWaitForSync();
    // thread 2 cannot acquire any jobs since they have been locked (and executed) by thread1 meanwhile
    jobAcquisitionThread2.makeContinueAndWaitForSync();

    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // then
    long acquisitionAttempts = managementService.createMetricsQuery().name(Metrics.JOB_ACQUISITION_ATTEMPT).sum();
    // each job executor twice (since the controllable thread always waits when already acquiring jobs)
    assertThat(acquisitionAttempts).isEqualTo(2 + 2);

    long acquiredJobs = managementService.createMetricsQuery()
        .name(Metrics.JOB_ACQUIRED_SUCCESS).sum();
    assertThat(acquiredJobs).isEqualTo(3);

    long acquiredJobsFailure = managementService.createMetricsQuery()
        .name(Metrics.JOB_ACQUIRED_FAILURE).sum();

    assertThat(acquiredJobsFailure).isEqualTo(3);

    // cleanup
    jobExecutor1.shutdown();
    jobExecutor2.shutdown();

    processEngineConfiguration.getDbMetricsReporter().reportNow();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/metrics/asyncServiceTaskProcess.bpmn20.xml")
  @Test
  void testJobExecutionMetricReporting() {
    // given
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("asyncServiceTaskProcess");
    }
    for (int i = 0; i < 2; i++) {
      runtimeService.startProcessInstanceByKey("asyncServiceTaskProcess",
          Variables.createVariables().putValue("fail", true));
    }

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000);

    // then
    long jobsSuccessful = managementService.createMetricsQuery().name(Metrics.JOB_SUCCESSFUL).sum();
    assertThat(jobsSuccessful).isEqualTo(3);

    long jobsFailed = managementService.createMetricsQuery().name(Metrics.JOB_FAILED).sum();
    // 2 jobs * 3 tries
    assertThat(jobsFailed).isEqualTo(6);

    long jobCandidatesForAcquisition = managementService.createMetricsQuery()
        .name(Metrics.JOB_ACQUIRED_SUCCESS).sum();
    assertThat(jobCandidatesForAcquisition).isEqualTo(3 + 6);
  }

  @Deployment
  @Test
  void testJobExecutionMetricExclusiveFollowUp() {
    // given
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("exclusiveServiceTasksProcess");
    }

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000);

    // then
    long jobsSuccessful = managementService.createMetricsQuery().name(Metrics.JOB_SUCCESSFUL).sum();
    long jobsFailed = managementService.createMetricsQuery().name(Metrics.JOB_FAILED).sum();
    long jobCandidatesForAcquisition = managementService.createMetricsQuery()
      .name(Metrics.JOB_ACQUIRED_SUCCESS).sum();
    long exclusiveFollowupJobs = managementService.createMetricsQuery()
      .name(Metrics.JOB_LOCKED_EXCLUSIVE).sum();

    assertThat(jobsSuccessful).isEqualTo(6);
    assertThat(jobsFailed).isZero();
    // the respective follow-up jobs are exclusive and have been executed right away without
    // acquisition
    assertThat(jobCandidatesForAcquisition).isEqualTo(3);
    assertThat(exclusiveFollowupJobs).isEqualTo(3);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/metrics/asyncServiceTaskProcess.bpmn20.xml")
  @Test
  void testJobRejectedExecutionMetricReporting() {
    // replace job executor with one that rejects all jobs
    RejectingJobExecutor rejectingExecutor = new RejectingJobExecutor();
    processEngineConfiguration.setJobExecutor(rejectingExecutor);
    rejectingExecutor.registerProcessEngine((ProcessEngineImpl) processEngine);

    // given three jobs
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("asyncServiceTaskProcess");
    }

    // when executing the jobs
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then all of them were rejected by the job executor which is reflected by the metric
    long numRejectedJobs = managementService.createMetricsQuery().name(Metrics.JOB_EXECUTION_REJECTED).sum();

    assertThat(numRejectedJobs).isEqualTo(3);
  }

  public static class RejectingJobExecutor extends DefaultJobExecutor {

    public RejectingJobExecutor() {
      BlockingQueue<Runnable> threadPoolQueue = new ArrayBlockingQueue<>(queueSize);
      threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS, threadPoolQueue) {

        @Override
        public void execute(Runnable command) {
          throw new RejectedExecutionException();
        }
      };
      threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

      rejectedJobsHandler = new CallerRunsRejectedJobsHandler();
    }
  }

}
