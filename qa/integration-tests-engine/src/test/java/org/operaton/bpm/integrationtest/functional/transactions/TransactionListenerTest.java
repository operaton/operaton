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
package org.operaton.bpm.integrationtest.functional.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.impl.cfg.TransactionListener;
import org.operaton.bpm.engine.impl.cfg.TransactionState;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;


/**
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TransactionListenerTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment();
  }

  @Test
  void testSynchronizationOnRollback() {

    final TestTransactionListener rolledBackListener = new TestTransactionListener();
    final TestTransactionListener committedListener = new TestTransactionListener();
    assertThat(rolledBackListener.isInvoked()).isFalse();
    assertThat(committedListener.isInvoked()).isFalse();

    try {

      processEngineConfiguration.getCommandExecutorTxRequired().execute(new Command<Void>() {

        @Override
        public Void execute(CommandContext commandContext) {
          commandContext.getTransactionContext().addTransactionListener(TransactionState.ROLLED_BACK, rolledBackListener);
          commandContext.getTransactionContext().addTransactionListener(TransactionState.COMMITTED, committedListener);

          throw new RuntimeException("Booum! Rollback!");
        }

      });

    }catch(Exception e) {
      assertThat(e.getMessage()).contains("Rollback!");
    }

    assertThat(rolledBackListener.isInvoked()).isTrue();
    assertThat(committedListener.isInvoked()).isFalse();

  }

  @Test
  void testSynchronizationOnCommitted() {

    final TestTransactionListener rolledBackListener = new TestTransactionListener();
    final TestTransactionListener committedListener = new TestTransactionListener();

    assertThat(rolledBackListener.isInvoked()).isFalse();
    assertThat(committedListener.isInvoked()).isFalse();

    try {

      processEngineConfiguration.getCommandExecutorTxRequired().execute(new Command<Void>() {

        @Override
        public Void execute(CommandContext commandContext) {
          commandContext.getTransactionContext().addTransactionListener(TransactionState.ROLLED_BACK, rolledBackListener);
          commandContext.getTransactionContext().addTransactionListener(TransactionState.COMMITTED, committedListener);
          return null;
        }

      });

    }catch(Exception e) {
      assertThat(e.getMessage()).contains("Rollback!");
    }

    assertThat(rolledBackListener.isInvoked()).isFalse();
    assertThat(committedListener.isInvoked()).isTrue();

  }

  protected static class TestTransactionListener implements TransactionListener {

    protected volatile boolean invoked = false;

    @Override
    public void execute(CommandContext commandContext) {
      invoked = true;
    }

    public boolean isInvoked() {
      return invoked;
    }

  }

}
