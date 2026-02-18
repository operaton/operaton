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

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.impl.BootstrapEngineCommand;
import org.operaton.bpm.engine.impl.cfg.TransactionState;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


class ConcurrentHistoryCleanupUpdateOfFailingJobTest extends ConcurrencyTestHelper {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .configurator(configuration -> configuration.setHistoryCleanupBatchWindowStartTime("00:00"))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected ManagementService managementService;
  protected int retries = 5;

  @BeforeEach
  void initializeProcessEngine() {
    processEngineConfiguration =engineRule.getProcessEngineConfiguration();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
  }

  @AfterEach
  void tearDown() {
    testRule.deleteHistoryCleanupJobs();
  }

  @Test
  void testFailedHistoryCleanupJobUpdate() throws Exception {
    // given configured History cleanup

    String cleanUpJobId;
    if (historyService.findHistoryCleanupJobs().isEmpty()) {
      cleanUpJobId = historyService.cleanUpHistoryAsync(true).getId();
    } else {
      cleanUpJobId = historyService.findHistoryCleanupJobs().get(0).getId();
    }

    processEngineConfiguration.getCommandExecutorTxRequired().<Void>execute(c -> {
      // add failure to the history cleanup job
      JobEntity cleanupJob = c.getJobManager().findJobById(cleanUpJobId);
      cleanupJob.setExceptionStacktrace("foo");

      return null;
    });

    ThreadControl threadOne = executeControllableCommand(new JobUpdateCmd(cleanUpJobId));

    ThreadControl threadTwo = executeControllableCommand(new ControllableBootstrap());
    threadTwo.reportInterrupts();
    threadOne.waitForSync();
    threadTwo.waitForSync();

    threadOne.makeContinue();
    threadOne.waitForSync();

    threadTwo.makeContinue();

    // wait a bit until t2 is blocked during the flush
    await().atMost(3, TimeUnit.SECONDS)
           .until(() -> threadTwo.syncAvailable || threadTwo.getException() != null);

    threadOne.waitUntilDone(); // let t1 commit, unblocking t2

    threadTwo.waitUntilDone(true); // continue with t2, expected to roll back

    // then
    assertThat(threadTwo.getException()).isNull();

    Job cleanupJob = historyService.findHistoryCleanupJobs().get(0);
    assertThat(cleanupJob.getRetries()).isEqualTo(retries);

    String stacktrace = managementService.getJobExceptionStacktrace(cleanupJob.getId());
    assertThat(stacktrace).isEqualTo("foo");
  }

  public class ControllableBootstrap extends ControllableCommand<Void> {

    @Override
    public Void execute(CommandContext commandContext) {

      monitor.sync();
      new BootstrapEngineCommand().execute(commandContext);
      monitor.sync();
      return null;
    }

  }

  public class JobUpdateCmd extends ControllableCommand<Void> {

    private String jobId;

    public JobUpdateCmd(String jobId) {
      this.jobId = jobId;
    }

    @Override
    public Void execute(CommandContext commandContext) {

      commandContext.getTransactionContext().addTransactionListener(TransactionState.COMMITTING, cc -> monitor.sync());
      monitor.sync();
      JobEntity job = commandContext.getJobManager().findJobById(jobId);
      job.setRetries(retries); // for reproducing the problem, it is important that
                               // this tx does not delete the exception stack trace
      return null;
    }

  }
}
