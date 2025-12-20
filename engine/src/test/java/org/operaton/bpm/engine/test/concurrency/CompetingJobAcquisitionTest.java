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
package org.operaton.bpm.engine.test.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * This test covers the behavior of two competing JobAcquisition threads.
 *
 * <p>
 * In the test:
 * 1. The first JobAcquisition thread is started.
 * 1.1. The first JobAcquisition thread attempts to acquire the job, and blocks.
 * 2. The second JobAcquisition thread is started.
 * 2.1. The second JobAcquisition thread attempts to acquire the job, and blocks.
 * 3. The first JobAcquisition thread unblocks, and successfully locks the acquired job in the DB.
 * 5. The second JobAcquisition thread unblocks, attempts to lock the acquired job in the DBm and receives an OptimisticLockingException.
 * 5.1. The OptimisticLockingListener on the second JobAcquisition thread handles
 *      the OptimisticLockingException by excluding the failed jobs.
 * 6. The second JobAcquisition thread acquires no jobs, but finishes without failing.
 * </p>
 *
 * @author Tom Baeyens
 */
class CompetingJobAcquisitionTest {

  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);


  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;

  protected static ControllableThread activeThread;
  protected String databaseType;


  @BeforeEach
  void initializeServices() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    databaseType = processEngineConfiguration.getDatabaseType();
    runtimeService = engineRule.getRuntimeService();
  }

  @Deployment
  @Test
  void testCompetingJobAcquisitions() {
    runtimeService.startProcessInstanceByKey("CompetingJobAcquisitionProcess");

    LOG.debug("test thread starts thread one");
    JobAcquisitionThread threadOne = new JobAcquisitionThread();
    threadOne.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread continues to start thread two");
    JobAcquisitionThread threadTwo = new JobAcquisitionThread();
    threadTwo.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();
    // the job was acquired
    assertThat(threadOne.jobs.size()).isEqualTo(1);

    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();

    // the acquisition did NOT fail
    assertThat(threadTwo.exception).isNull();
    // but the job was not acquired
    assertThat(threadTwo.jobs.size()).isZero();
  }

  public class JobAcquisitionThread extends ControllableThread {
    OptimisticLockingException exception;
    AcquiredJobs jobs;

    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    @Override
    public void run() {
      try {
        JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
        jobs = (AcquiredJobs) processEngineConfiguration
          .getCommandExecutorTxRequired()
          .execute(new ControlledCommand(activeThread, new AcquireJobsCmd(jobExecutor)));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug("{} ends", getName());
    }
  }

}
