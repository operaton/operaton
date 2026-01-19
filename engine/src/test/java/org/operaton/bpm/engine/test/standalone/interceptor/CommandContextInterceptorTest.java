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
package org.operaton.bpm.engine.test.standalone.interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Tom Baeyens
 */
class CommandContextInterceptorTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  HistoryService historyService;

  @Test
  void testCommandContextGetCurrentAfterException() {
    var commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    assertThatThrownBy(() -> commandExecutor.execute(commandContext -> {
      throw new IllegalStateException("here i come!");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(Context.getCommandContext()).isNull();
  }

  @Test
  void testCommandContextNestedFailingCommands() {
    // given
    final ExceptionThrowingCmd innerCommand1 = new ExceptionThrowingCmd(new IdentifiableRuntimeException(1));
    final ExceptionThrowingCmd innerCommand2 = new ExceptionThrowingCmd(new IdentifiableRuntimeException(2));
    var commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();

    // when/then
    assertThatThrownBy(() -> commandExecutor.execute(commandContext -> {
      commandExecutor.execute(innerCommand1);
      commandExecutor.execute(innerCommand2);

      return null;
    }))
      .isInstanceOf(IdentifiableRuntimeException.class)
      .extracting(e -> ((IdentifiableRuntimeException) e).id)
      .isEqualTo(1);

    assertThat(innerCommand1.executed).isTrue();
    assertThat(innerCommand2.executed).isFalse();
  }

  @Test
  void testCommandContextNestedTryCatch() {
    final ExceptionThrowingCmd innerCommand = new ExceptionThrowingCmd(new IdentifiableRuntimeException(1));

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      CommandExecutor commandExecutor = Context.getProcessEngineConfiguration().getCommandExecutorTxRequired();

      try {
        commandExecutor.execute(innerCommand);
        fail("exception expected to pop up during execution of inner command");
      } catch (IdentifiableRuntimeException e) {
        // happy path
        assertThat(Context.getCommandInvocationContext().getThrowable()).as("the exception should not have been propagated to this command's context").isNull();
      }

      return null;
    });
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void testCommandContextNestedFailingCommandsNotExceptions() {
    // given
    final BpmnModelInstance modelInstance =
      Bpmn.createExecutableProcess("processThrowingThrowable")
        .startEvent()
          .serviceTask()
          .operatonClass(ThrowErrorJavaDelegate.class)
        .endEvent().done();

   testRule.deploy(modelInstance);

    var commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();

    // when
    assertThatThrownBy(() -> commandExecutor.execute(commandContext -> {
      runtimeService.startProcessInstanceByKey("processThrowingThrowable");
      return null;
    }))
      .isInstanceOf(StackOverflowError.class);

    // then
    assertThat(ThrowErrorJavaDelegate.executed).isTrue();

    // Check data base consistency
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
  }

  protected class ExceptionThrowingCmd implements Command<Void> {

    protected boolean executed;

    protected RuntimeException exceptionToThrow;

    public ExceptionThrowingCmd(RuntimeException e) {
      executed = false;
      exceptionToThrow = e;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      executed = true;
      throw exceptionToThrow;
    }

  }

  protected class IdentifiableRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    protected int id;
    public IdentifiableRuntimeException(int id) {
      this.id = id;
    }
  }

}
