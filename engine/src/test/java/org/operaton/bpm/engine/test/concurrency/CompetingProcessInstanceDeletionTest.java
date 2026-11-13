/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.engine.test.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.impl.cmd.ExecuteJobsCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.ExecuteJobHelper;
import org.operaton.bpm.engine.impl.jobexecutor.JobFailureCollector;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;

import static org.operaton.bpm.model.bpmn.Bpmn.createExecutableProcess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makes sure that a command which has loaded an execution, but not yet its process instance,
 * fails with an {@link OptimisticLockingException} if the process instance is deleted
 * concurrently in the meantime, regardless of whether the execution tree is prefetched.
 *
 * <p>
 * This happens for example when a timer job inside an embedded subprocess is executed
 * while the process instance is deleted: the job's execution is loaded, the deletion is
 * committed, and leaving the timer event loads the process instance, which is gone by then.
 * </p>
 */
class CompetingProcessInstanceDeletionTest extends ConcurrencyTestCase {

  protected boolean executionTreePrefetchEnabled;

  @BeforeEach
  void deployTimerInSubProcess() {
    executionTreePrefetchEnabled = processEngineConfiguration.isExecutionTreePrefetchEnabled();

    testRule.deploy(createExecutableProcess("timerInSubProcess")
        .startEvent()
        .subProcess()
          .embeddedSubProcess()
            .startEvent()
            .intermediateCatchEvent("timer").timerWithDuration("PT1H")
            .endEvent()
          .subProcessDone()
        .endEvent()
        .done());
  }

  @AfterEach
  void resetExecutionTreePrefetch() {
    processEngineConfiguration.setExecutionTreePrefetchEnabled(executionTreePrefetchEnabled);
  }

  @ParameterizedTest(name = "executionTreePrefetchEnabled={0}")
  @ValueSource(booleans = {true, false})
  void shouldThrowOptimisticLockingExceptionWhenProcessInstanceIsDeletedBeforeLoadingIt(boolean prefetchEnabled) {
    // given a process instance waiting at a timer inside an embedded subprocess
    processEngineConfiguration.setExecutionTreePrefetchEnabled(prefetchEnabled);
    String processInstanceId = runtimeService.startProcessInstanceByKey("timerInSubProcess").getId();
    Execution timerExecution = runtimeService.createExecutionQuery().activityId("timer").singleResult();
    assertThat(timerExecution.getId()).isNotEqualTo(processInstanceId);

    // when a command has loaded the timer's execution but not yet the process instance
    ThreadControl loadingThread = executeControllableCommand(new LoadProcessInstanceCmd(timerExecution.getId()));
    loadingThread.reportInterrupts();
    loadingThread.waitForSync();

    // and the process instance is deleted by another transaction in the meantime
    runtimeService.deleteProcessInstance(processInstanceId, "deleted concurrently");

    loadingThread.waitUntilDone();

    // then the command fails with an OptimisticLockingException
    assertThat(loadingThread.getException())
        .isInstanceOf(OptimisticLockingException.class)
        .hasMessageContaining(processInstanceId);
  }

  @ParameterizedTest(name = "executionTreePrefetchEnabled={0}")
  @ValueSource(booleans = {true, false})
  void shouldNotCreateIncidentWhenProcessInstanceIsDeletedDuringJobExecution(boolean prefetchEnabled) {
    // given a process instance waiting at a timer inside an embedded subprocess
    processEngineConfiguration.setExecutionTreePrefetchEnabled(prefetchEnabled);
    String processInstanceId = runtimeService.startProcessInstanceByKey("timerInSubProcess").getId();
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();

    // when the timer job has loaded its execution but not yet the process instance
    JobFailureCollector jobFailureCollector = new JobFailureCollector(timerJob.getId());
    ThreadControl jobThread = executeControllableJob(
        new ExecuteJobAfterLoadingExecutionCmd(timerJob.getId(), jobFailureCollector), jobFailureCollector);
    jobThread.reportInterrupts();
    jobThread.waitForSync();

    // and the process instance is deleted by another transaction in the meantime
    runtimeService.deleteProcessInstance(processInstanceId, "deleted concurrently");

    jobThread.waitUntilDone();

    // then the job fails with an OptimisticLockingException
    assertThat(jobThread.getException()).isInstanceOf(OptimisticLockingException.class);
    assertThat(jobFailureCollector.getFailure()).isInstanceOf(OptimisticLockingException.class);

    // and the failed job handling skips the deleted job instead of creating an incident
    assertThat(managementService.createJobQuery().jobId(timerJob.getId()).count()).isZero();
    assertThat(runtimeService.createIncidentQuery().count()).isZero();
    assertThat(historyService.createHistoricIncidentQuery().count()).isZero();
  }

  /**
   * Executes the command like the job executor does, including the handling of job failures.
   */
  protected ThreadControl executeControllableJob(ControllableCommand<Void> command,
      JobFailureCollector jobFailureCollector) {
    final Thread controlThread = Thread.currentThread();

    Thread thread = new Thread(() -> {
      try {
        ExecuteJobHelper.executeJob(processEngineConfiguration.getCommandExecutorTxRequired(), jobFailureCollector,
            command);
      } catch (RuntimeException e) {
        command.monitor.setException(e);
        controlThread.interrupt();
        throw e;
      }
    });

    controllableCommands.add(command);
    command.monitor.executingThread = thread;

    thread.start();

    return command.monitor;
  }

  static class LoadProcessInstanceCmd extends ControllableCommand<Void> {

    protected String executionId;

    public LoadProcessInstanceCmd(String executionId) {
      this.executionId = executionId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(executionId);

      monitor.sync();

      // loads the process instance, as PvmExecutionImpl#getDelayedEvents does when leaving the timer event
      execution.getProcessInstance();

      return null;
    }
  }

  static class ExecuteJobAfterLoadingExecutionCmd extends ControllableCommand<Void> {

    protected String jobId;
    protected JobFailureCollector jobFailureCollector;

    public ExecuteJobAfterLoadingExecutionCmd(String jobId, JobFailureCollector jobFailureCollector) {
      this.jobId = jobId;
      this.jobFailureCollector = jobFailureCollector;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      JobEntity job = commandContext.getJobManager().findJobById(jobId);
      job.getExecution();

      monitor.sync();

      // job and execution are taken from the entity cache, the process instance is loaded when leaving the timer
      return new ExecuteJobsCmd(jobId, jobFailureCollector).execute(commandContext);
    }
  }

}
