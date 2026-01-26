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
package org.operaton.bpm.engine.test.concurrency.partitioning;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Tassilo Weidner
 */

class SkipHistoryOptimisticLockingExceptionsDisabledTest extends AbstractPartitioningTest {

  static final String VARIABLE_NAME = "aVariableName";
  static final String VARIABLE_VALUE = "aVariableValue";
  static final String ANOTHER_VARIABLE_VALUE = "anotherVariableValue";

  @Test
  void testHistoryOptimisticLockingExceptionsNotSkipped() {
    // given
    processEngine.getProcessEngineConfiguration().setSkipHistoryOptimisticLockingExceptions(false);

    String processInstanceId = deployAndStartProcess(PROCESS_WITH_USERTASK,
      Variables.createVariables().putValue(VARIABLE_NAME, VARIABLE_VALUE)).getId();

    ThreadControl asyncThread = executeControllableCommand(new AsyncThread(processInstanceId));

    asyncThread.reportInterrupts();

    asyncThread.waitForSync();

    commandExecutor.execute((Command<Void>) commandContext -> {
      HistoricVariableInstanceEntity historicVariableInstanceEntity =
        (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery().singleResult();

      commandContext.getDbEntityManager().delete(historicVariableInstanceEntity);

      return null;
    });

    // assume
    assertThat(historyService.createHistoricVariableInstanceQuery().singleResult()).isNull();

    asyncThread.waitUntilDone();

    // then
    assertThat(asyncThread.getException())
      .hasMessageContaining("Entity was updated by another transaction concurrently");
  }

  public class AsyncThread extends ControllableCommand<Void> {

    String processInstanceId;

    AsyncThread(String processInstanceId) {
      this.processInstanceId = processInstanceId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      historyService.createHistoricVariableInstanceQuery()
        .singleResult()
        .getId(); // cache

      monitor.sync();

        commandContext.getProcessEngineConfiguration()
          .getRuntimeService()
          .setVariable(processInstanceId, VARIABLE_NAME, ANOTHER_VARIABLE_VALUE);

      return null;
    }

  }

}
