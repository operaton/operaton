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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.awaitility.core.ConditionTimeoutException;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.impl.BootstrapEngineCommand;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandInvocationContext;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_END_TIME_BASED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * <p>Tests a concurrent attempt of a bootstrapping Process Engine to reconfigure
 * the HistoryCleanupJob while the JobExecutor tries to execute it.</p>
 *
 * The steps are the following:
 *
 * <p>
 *  1. The (History Cleanup) JobExecution thread is started, and stopped before the job is executed.
 *  2. The Process Engine Bootstrap thread is started, and stopped before the HistoryCleanupJob is reconfigured.
 *  3. The JobExecution thread executes the HistoryCleanupJob and stops before flushing.
 *  4. The Process Engine Bootstrap thread reconfigures the HistoryCleanupJob and stops before flushing.
 *  5. The JobExecution thread flushes the update to the HistoryCleanupJob.
 *  6. The Process Engine Bootstrap thread attempts to flush the reconfigured HistoryCleanupJob.
 *  6.1 An OptimisticLockingException is thrown due to the concurrent JobExecution
 *      thread update to the HistoryCleanupJob.
 *  6.2 The OptimisticLockingListener registered with
 *      the <code>BootstrapEngineCommand#createHistoryCleanupJob()</code> suppresses the exception.
 *  6.3 In case the OptimisticLockingListener didn't handle the OLE,
 *      it's still caught and logged in <code>ProcessEngineImpl#executeSchemaOperations()</code>
 *  7. The Process Engine Bootstrap thread successfully builds and registers the new Process Engine.
 * </p>
 *
 *
 * @author Nikola Koevski
 */
class ConcurrentProcessEngineJobExecutorHistoryCleanupJobTest extends ConcurrencyTestCase {

  private static final String PROCESS_ENGINE_NAME = "historyCleanupJobEngine";

  @BeforeEach
  void setUp() {

    // Ensure that current time is outside batch window
    Calendar timeOfDay = Calendar.getInstance();
    timeOfDay.set(Calendar.HOUR_OF_DAY, 17);
    ClockUtil.setCurrentTime(timeOfDay.getTime());

    processEngineConfiguration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_END_TIME_BASED);
  }

  protected void closeDownProcessEngine() {
    final ProcessEngine otherProcessEngine = ProcessEngines.getProcessEngine(PROCESS_ENGINE_NAME);
    if (otherProcessEngine != null) {

      ((ProcessEngineConfigurationImpl)otherProcessEngine.getProcessEngineConfiguration())
          .getCommandExecutorTxRequired()
          .execute((Command<Void>) commandContext -> {

            List<Job> jobs = otherProcessEngine.getManagementService().createJobQuery().list();
            if (!jobs.isEmpty()) {
              assertThat(jobs).hasSize(1);
              String jobId = jobs.get(0).getId();
              commandContext.getJobManager().deleteJob((JobEntity) jobs.get(0));
              commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobId);
            }

            return null;
      });

      otherProcessEngine.close();
      ProcessEngines.unregister(otherProcessEngine);
    }
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {

      List<Job> jobs = processEngine.getManagementService().createJobQuery().list();
      if (!jobs.isEmpty()) {
        assertThat(jobs).hasSize(1);
        String jobId = jobs.get(0).getId();
        commandContext.getJobManager().deleteJob((JobEntity) jobs.get(0));
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobId);
      }

      return null;
    });
    ClockUtil.setCurrentTime(new Date());
    closeDownProcessEngine();
  }

  @Test
  void testConcurrentHistoryCleanupJobReconfigurationExecution() throws Exception {

    processEngine.getHistoryService().cleanUpHistoryAsync(true);

    ThreadControl thread1 = executeControllableCommand(new ControllableJobExecutionCommand());
    thread1.reportInterrupts();
    thread1.waitForSync();

    ControllableProcessEngineBootstrapCommand bootstrapCommand = new ControllableProcessEngineBootstrapCommand();
    ThreadControl thread2 = executeControllableCommand(bootstrapCommand);
    thread2.reportInterrupts();
    thread2.waitForSync();

    thread1.makeContinue();
    thread1.waitForSync();

    thread2.makeContinue();

    await().atMost(2, TimeUnit.SECONDS)
           .until(() -> thread2.syncAvailable || thread2.getException() != null);

    thread1.waitUntilDone();

    thread2.waitForSync();
    thread2.waitUntilDone(true);

    assertThat(thread1.getException()).isNull();

    assertThat(thread2.getException()).isNull();
    assertThat(bootstrapCommand.getContextSpy().getThrowable()).isNull();
    assertThat(ProcessEngines.getProcessEngines().get(PROCESS_ENGINE_NAME)).isNotNull();
  }

  protected static class ControllableProcessEngineBootstrapCommand extends ControllableCommand<Void> {

    protected ControllableBootstrapEngineCommand bootstrapCommand;

    @Override
    public Void execute(CommandContext commandContext) {

      bootstrapCommand = new ControllableBootstrapEngineCommand(this.monitor);

      ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/concurrency/historycleanup.operaton.cfg.xml");


      processEngineConfiguration.setProcessEngineBootstrapCommand(bootstrapCommand);

      processEngineConfiguration.setProcessEngineName(PROCESS_ENGINE_NAME);
      processEngineConfiguration.buildProcessEngine();

      return null;
    }

    public CommandInvocationContext getContextSpy() {
      return bootstrapCommand.getSpy();
    }
  }

  protected static class ControllableJobExecutionCommand extends ControllableCommand<Void> {

    @Override
    public Void execute(CommandContext commandContext) {

      monitor.sync();

      List<Job> historyCleanupJobs = commandContext.getProcessEngineConfiguration().getHistoryService().findHistoryCleanupJobs();

      for (Job job : historyCleanupJobs) {
        commandContext.getProcessEngineConfiguration().getManagementService().executeJob(job.getId());
      }

      monitor.sync();

      return null;
    }
  }

  protected static class ControllableBootstrapEngineCommand extends BootstrapEngineCommand implements Command<Void> {

    protected final ThreadControl monitor;
    protected CommandInvocationContext spy;

    public ControllableBootstrapEngineCommand(ThreadControl threadControl) {
      this.monitor = threadControl;
    }

    @Override
    protected void createHistoryCleanupJob(CommandContext commandContext) {

      monitor.sync();

      super.createHistoryCleanupJob(commandContext);
      spy = Context.getCommandInvocationContext();

      monitor.sync();
    }

    public CommandInvocationContext getSpy() {
      return spy;
    }
  }
}
