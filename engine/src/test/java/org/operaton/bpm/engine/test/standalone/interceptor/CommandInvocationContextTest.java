/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.standalone.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

public class CommandInvocationContextTest extends PluggableProcessEngineTest {

  /**
   * Test that the command invocation context always holds the correct command;
   * in outer commands as well as nested commands.
   */
  @Test
  public void testGetCurrentCommand() {
    Command<?> outerCommand = new SelfAssertingCommand(new SelfAssertingCommand(null));

    processEngineConfiguration.getCommandExecutorTxRequired().execute(outerCommand);
  }

  protected class SelfAssertingCommand implements Command<Void> {

    protected Command<Void> innerCommand;

    public SelfAssertingCommand(Command<Void> innerCommand) {
      this.innerCommand = innerCommand;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      assertThat(Context.getCommandInvocationContext().getCommand()).isEqualTo(this);

      if (innerCommand != null) {
        CommandExecutor commandExecutor = Context.getProcessEngineConfiguration().getCommandExecutorTxRequired();
        commandExecutor.execute(innerCommand);

        // should still be correct after command invocation
        assertThat(Context.getCommandInvocationContext().getCommand()).isEqualTo(this);
      }

      return null;
    }

  }

}
