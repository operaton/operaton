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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.Execution;

import static org.operaton.bpm.model.bpmn.Bpmn.createExecutableProcess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makes sure that a command which has loaded an execution, but not yet its execution tree,
 * fails with an {@link OptimisticLockingException} if the process instance is deleted
 * concurrently in the meantime.
 *
 * <p>
 * This happens for example when a timer job inside an embedded subprocess is executed
 * while the process instance is deleted: the job's execution is loaded, the deletion is
 * committed, and leaving the timer event loads the execution tree, which is empty by then.
 * </p>
 */
class CompetingProcessInstanceDeletionTest extends ConcurrencyTestCase {

  @Test
  void shouldThrowOptimisticLockingExceptionWhenProcessInstanceIsDeletedBeforeLoadingExecutionTree() {
    // given a process instance waiting at a timer inside an embedded subprocess
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

    String processInstanceId = runtimeService.startProcessInstanceByKey("timerInSubProcess").getId();
    Execution timerExecution = runtimeService.createExecutionQuery().activityId("timer").singleResult();
    assertThat(timerExecution.getId()).isNotEqualTo(processInstanceId);

    // when a command has loaded the timer's execution but not yet the execution tree
    ThreadControl loadingThread = executeControllableCommand(new LoadExecutionTreeCmd(timerExecution.getId()));
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

  static class LoadExecutionTreeCmd extends ControllableCommand<Void> {

    protected String executionId;

    public LoadExecutionTreeCmd(String executionId) {
      this.executionId = executionId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(executionId);

      monitor.sync();

      // loads the execution tree, as PvmExecutionImpl#getDelayedEvents does when leaving the timer event
      execution.getProcessInstance();

      return null;
    }
  }

}
