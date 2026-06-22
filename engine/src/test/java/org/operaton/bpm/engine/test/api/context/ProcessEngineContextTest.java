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
package org.operaton.bpm.engine.test.api.context;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.context.ProcessEngineContext;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProcessEngineContextTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected static final String SIMPLE_PROCESS_KEY = "simple_process";
  protected static final BpmnModelInstance SIMPLE_PROCESS = Bpmn.createExecutableProcess(SIMPLE_PROCESS_KEY)
      .operatonHistoryTimeToLive(180)
      .startEvent()
      .userTask("simpleUserTask")
      .endEvent()
      .done();

  @BeforeEach
  void setUp() {
    testHelper.deploy(SIMPLE_PROCESS);
    engineRule.getRuntimeService().startProcessInstanceByKey(SIMPLE_PROCESS_KEY);
  }

  @Test
  void shouldUseFreshCacheOnNewCommandContext() {
    /* The test ensures that when `ProcessEngineContext#requiresNew()` is called,
    a fresh cache is used for the new command, and no cached data of the outer command
    cache is available (i.e. only persisted data is available).
     */

    // given
    engineRule.getProcessEngineConfiguration()
      .getCommandExecutorTxRequired()
      .execute(
        // a Command that executes an engine API call
          new OuterCommand<>(
              // when
              // a nested Command is executed with an explicitly declared, new CommandContext
              new NestedCommand<>() {
                  @Override
                  public Void call() {
                      // then
                      // the nested CommandContext should be new
                      assertThat(getOuterCommandContext()).isNotEqualTo(commandContext);
              /*
               the queried Process Instance object in the nested command
               should be different from the queried PI object of the
               outer command (new CommandContext and fresh DB cache)
               */
                      assertThat(getNestedPiObject()).isNotEqualTo(getOuterCommand().getOuterPiObject());

                      return null;
                  }
              }, true));
  }

  @Test
  void shouldUseSameCommandContextOnTxRequired() throws Exception {
    /* The test ensures that, even if the `requiresNew()` method is called on a command,
    inner commands still reuse the current CommandContext (unless otherwise specified)
     */

    // given
    // a new CommandContext is explicitly declared for an engine API call
    ProcessEngineContext.withNewProcessEngineContext(() -> engineRule.getProcessEngineConfiguration()
        .getCommandExecutorTxRequired()
        .execute(
            // a Command is executed with the new Context
            new OuterCommand<>(
                // and a nested Command reuses that context
                new NestedCommand<>() {
                    @Override
                    public Void call() {
                        // then
                        // the outer CommandContext should be reused
                        assertThat(getOuterCommandContext()).isEqualTo(getCommandContext());
                    /*
                     the queried Process Instance object in the nested command
                     should be the same from the queried PI object of the outer
                     command (shared CommandContext and DB cache)
                     */
                        assertThat(getNestedPiObject()).isEqualTo(getOuterCommand().getOuterPiObject());

                        return null;
                    }
                }, false
            )
        ));
  }

  protected class OuterCommand<T> implements Command<T> {

    protected CommandContext commandContext;
    protected NestedCommand<T> nestedCommand;
    protected Boolean requiresNew;
    protected ProcessInstance outerPiObject;

    public OuterCommand(NestedCommand<T> nestedCommand, Boolean requiresNew) {
      this.nestedCommand = nestedCommand;
      this.nestedCommand.setOuterCommand(this);
      this.requiresNew = requiresNew;
    }

    @Override
    public T execute(final CommandContext commandContext) {
      // make the outer CommandContext available
      this.commandContext = commandContext;
      this.outerPiObject = engineRule.getRuntimeService()
          .createProcessInstanceQuery()
          .processDefinitionKey(SIMPLE_PROCESS_KEY)
          .singleResult();

      if (Boolean.TRUE.equals(requiresNew)) {
        assertThatCode(() -> ProcessEngineContext.withNewProcessEngineContext(this::executeNestedCommand))
          .doesNotThrowAnyException();
      } else {
        return executeNestedCommand();
      }

      return null;
    }

    protected T executeNestedCommand() {
      return commandContext.getProcessEngineConfiguration()
        .getCommandExecutorTxRequired()
        .execute(nestedCommand);
    }

    public ProcessInstance getOuterPiObject() {
      return outerPiObject;
    }

    public CommandContext getCommandContext() {
      return commandContext;
    }
  }

  protected abstract class NestedCommand<T> implements Command<T>, Callable<T>{

    CommandContext commandContext;
    OuterCommand outerCommand;
    protected ProcessInstance nestedPiObject;

    @Override
    public abstract T call();

    @Override
    public T execute(CommandContext commandContext) {
      // make the nested CommandContext available
      this.commandContext = commandContext;

      this.nestedPiObject = engineRule.getRuntimeService()
        .createProcessInstanceQuery()
        .processDefinitionKey(SIMPLE_PROCESS_KEY)
        .singleResult();

      // when/then
      assertThatCode(this::call)
        .doesNotThrowAnyException();

      return null;
    }

    public OuterCommand getOuterCommand() {
      return outerCommand;
    }

    public void setOuterCommand(OuterCommand outerCommand) {
      this.outerCommand = outerCommand;
    }

    public CommandContext getOuterCommandContext() {
      return getOuterCommand().getCommandContext();
    }

    public CommandContext getCommandContext() {
      return commandContext;
    }

    public ProcessInstance getNestedPiObject() {
      return nestedPiObject;
    }
  }
}
