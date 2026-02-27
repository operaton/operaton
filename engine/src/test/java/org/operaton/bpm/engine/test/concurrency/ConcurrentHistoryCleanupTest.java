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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.cmd.HistoryCleanupCmd;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.util.DatabaseHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assume.assumeTrue;

/**
 * <p>Tests the call to history cleanup simultaneously.</p>
 *
 * <p><b>Note:</b> the test is not executed on H2 because it doesn't support the
 * exclusive lock on table.</p>
 *
 * @author Svetlana Dorokhova
 */
class ConcurrentHistoryCleanupTest extends ConcurrencyTestCase {

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {

      List<Job> jobs = processEngine.getManagementService().createJobQuery().list();
      if (!jobs.isEmpty()) {
        String jobId = jobs.get(0).getId();
        commandContext.getJobManager().deleteJob((JobEntity) jobs.get(0));
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobId);
      }

      return null;
    });

  }

  @Test
  @RequiredDatabase(excludes = {DbSqlSessionFactory.MARIADB, DbSqlSessionFactory.H2})
  void testRunTwoHistoryCleanups() {
    final Integer transactionIsolationLevel = DatabaseHelper.getTransactionIsolationLevel(processEngineConfiguration);
    assumeTrue(transactionIsolationLevel != null && !transactionIsolationLevel.equals(Connection.TRANSACTION_READ_COMMITTED));

    ThreadControl thread1 = executeControllableCommand(new ControllableHistoryCleanupCommand());
    thread1.waitForSync();

    ThreadControl thread2 = executeControllableCommand(new ControllableHistoryCleanupCommand());
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

    //only one history cleanup job exists -> no exception
    List<Job> historyCleanupJobs = processEngine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);

    assertThat(thread1.getException()).isNull();
    assertThat(thread2.getException()).isNull();

  }

  protected static class ControllableHistoryCleanupCommand extends ControllableCommand<Void> {

    @Override
    public Void execute(CommandContext commandContext) {
      monitor.sync();  // thread will block here until makeContinue() is called from main thread

      new HistoryCleanupCmd(true).execute(commandContext);

      monitor.sync();  // thread will block here until waitUntilDone() is called form main thread

      return null;
    }

  }

}
