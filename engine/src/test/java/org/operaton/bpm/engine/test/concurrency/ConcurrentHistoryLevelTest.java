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

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.awaitility.core.ConditionTimeoutException;

import org.operaton.bpm.engine.impl.HistoryLevelSetupCommand;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.test.util.DatabaseHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.awaitility.Awaitility.await;

/**
 * <p>Tests cluster scenario with two nodes trying to write the history level property in parallel.</p>
 *
 * <p><b>Note:</b> the test is not executed on H2 because it doesn't support the
 * exclusive lock on table.</p>
 *
 */
class ConcurrentHistoryLevelTest extends ConcurrencyTestCase {

  @BeforeEach
  void setUp() {
    TestHelper.deleteHistoryLevel(processEngineConfiguration);
  }

  @Test
  @RequiredDatabase(excludes = {DbSqlSessionFactory.H2, DbSqlSessionFactory.MARIADB})
  void test() throws Exception {
    Integer transactionIsolationLevel = DatabaseHelper.getTransactionIsolationLevel(processEngineConfiguration);
    assumeThat(transactionIsolationLevel != null && !transactionIsolationLevel.equals(Connection.TRANSACTION_READ_COMMITTED));
    ThreadControl thread1 = executeControllableCommand(new ControllableUpdateHistoryLevelCommand());
    thread1.reportInterrupts();
    thread1.waitForSync();

    ThreadControl thread2 = executeControllableCommand(new ControllableUpdateHistoryLevelCommand());
    thread2.reportInterrupts();
    thread2.waitForSync();

    thread1.makeContinue();
    thread1.waitForSync();

    thread2.makeContinue();

    await().atMost(2, TimeUnit.SECONDS)
           .until(() -> thread2.syncAvailable || thread2.getException() != null);

    thread1.waitUntilDone();

    thread2.waitForSync();
    thread2.waitUntilDone();

    assertThat(thread1.getException()).isNull();
    Throwable thread2Exception = thread2.getException();
    assertThat(thread2Exception).isNull();
    HistoryLevel historyLevel = processEngineConfiguration.getHistoryLevel();
    assertThat(historyLevel.getName()).isEqualTo("full");
  }

  protected static class ControllableUpdateHistoryLevelCommand extends ControllableCommand<Void> {

    @Override
    public Void execute(CommandContext commandContext) {

      monitor.sync(); // thread will block here until makeContinue() is called from main thread

      new HistoryLevelSetupCommand().execute(commandContext);

      monitor.sync(); // thread will block here until waitUntilDone() is called form main thread

      return null;
    }

  }
}
