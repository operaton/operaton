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
package org.operaton.bpm.engine.test.jobexecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.concurrency.ConcurrencyTestHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.fail;

/**
 * Base class for tests that verify the SKIP LOCKED job acquisition against real,
 * uncommitted database row locks. Unlike the tests that simulate a lock by writing
 * committed LOCK_OWNER_/LOCK_EXP_TIME_ values (which only exercise the WHERE clause),
 * these tests hold actual row locks from a second transaction, so they prove or
 * disprove the vendor-specific SKIP LOCKED behavior itself.
 *
 * <p>The assertions state the desired contract for every database. A failure on a
 * specific vendor is a finding about that vendor's SQL variant, not a broken test.
 */
public abstract class AbstractJobExecutorSkipLockedRowLockTest extends ConcurrencyTestHelper {

  protected static final long BLOCK_DETECTION_TIMEOUT_MS = 30_000;

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();

  protected ManagementService managementService;
  protected RuntimeService runtimeService;

  protected ExecutorService boundedExecutor;
  protected final List<Connection> rawConnections = new ArrayList<>();

  private boolean skipLockedBefore;

  @BeforeEach
  void setUpSkipLockedEngine() {
    processEngineConfiguration = rule.getProcessEngineConfiguration();
    managementService = rule.getManagementService();
    runtimeService = rule.getRuntimeService();

    skipLockedBefore = processEngineConfiguration.isJobExecutorAcquireWithSkipLocked();
    processEngineConfiguration.setJobExecutorAcquireWithSkipLocked(true);

    boundedExecutor = Executors.newCachedThreadPool();
  }

  @AfterEach
  void tearDownSkipLockedEngine() {
    // release all external row locks before the concurrency helper joins its threads,
    // otherwise a thread blocked on a lock would never terminate
    for (Connection connection : rawConnections) {
      try {
        connection.rollback();
        connection.close();
      } catch (SQLException e) {
        // connection may already be broken; the container teardown will reclaim it
      }
    }
    rawConnections.clear();

    boundedExecutor.shutdownNow();

    processEngineConfiguration.setJobExecutorAcquireWithSkipLocked(skipLockedBefore);
  }

  protected JobExecutor getJobExecutor() {
    return processEngineConfiguration.getJobExecutor();
  }

  protected String getDatabaseType() {
    return processEngineConfiguration.getDatabaseType();
  }

  protected void startProcessInstances(String key, int count) {
    for (int i = 0; i < count; i++) {
      runtimeService.startProcessInstanceByKey(key);
    }
  }

  protected List<String> findAllJobIds() {
    return managementService.createJobQuery().list().stream().map(Job::getId).toList();
  }

  /**
   * Opens a connection outside of the process engine with autocommit disabled.
   * It is rolled back and closed automatically after the test. The isolation level
   * is pinned to READ COMMITTED because that is what every real competing engine
   * node runs with (enforced at engine bootstrap) — vendor defaults like MariaDB's
   * REPEATABLE READ would add gap/next-key locks no real peer would hold.
   */
  protected Connection openExternalConnection() throws SQLException {
    Connection connection = processEngineConfiguration.getDataSource().getConnection();
    connection.setAutoCommit(false);
    connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    rawConnections.add(connection);
    return connection;
  }

  /**
   * Takes uncommitted update locks on the given job rows, simulating a competing
   * node whose acquisition transaction is in flight. Uses the locking-read idiom
   * of the respective database, one point lookup per row — an IN-list may be
   * executed as a scan on small tables, which would lock rows beyond the given ids.
   */
  protected void lockJobRowsExternally(Connection connection, List<String> jobIds) throws SQLException {
    String sql = switch (getDatabaseType()) {
      case DbSqlSessionFactory.MSSQL ->
        "select ID_ from ACT_RU_JOB with (updlock, rowlock) where ID_ = ?";
      case DbSqlSessionFactory.DB2 ->
        "select ID_ from ACT_RU_JOB where ID_ = ? for read only with rs use and keep update locks";
      default ->
        "select ID_ from ACT_RU_JOB where ID_ = ? for update";
    };
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (String jobId : jobIds) {
        statement.setString(1, jobId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            fail("test setup: could not lock job row %s".formatted(jobId));
          }
        }
      }
    }
    if (!DbSqlSessionFactory.DB2.equals(getDatabaseType())) {
      // DB2 has no probe syntax independent of the code under test — see selectWithSkipLockedReader
      List<String> visible = selectWithSkipLockedReader(jobIds);
      if (!visible.isEmpty()) {
        fail(("[%s] test setup: a skip-locked reader on a third connection still sees rows %s "
            + "although they were just locked externally — the external lock does not take effect "
            + "on this database, so the acquisition assertions would be meaningless").formatted(getDatabaseType(), visible));
      }
    }
  }

  /**
   * Setup self-check counterpart: rows NOT locked externally must remain visible
   * to a skip-locked reader. Guards against the external locking read having
   * locked more rows than intended (e.g. via a scan plan).
   */
  protected void assertRowsFreeForSkipLockedReader(List<String> jobIds) throws SQLException {
    if (DbSqlSessionFactory.DB2.equals(getDatabaseType())) {
      return;
    }
    List<String> visible = selectWithSkipLockedReader(jobIds);
    if (visible.size() != jobIds.size()) {
      List<String> invisible = new ArrayList<>(jobIds);
      invisible.removeAll(visible);
      fail(("[%s] test setup: rows %s appear locked to a skip-locked reader although they were "
          + "never locked externally — the external locking read locked more rows than intended").formatted(getDatabaseType(), invisible));
    }
  }

  /**
   * Reads the given rows with a skip-locked read from a fresh connection and
   * returns the ids that were visible (i.e. not locked by anyone). The probing
   * transaction is rolled back immediately so it leaves no locks behind.
   * DB2 has no probe syntax independent of the code under test; callers skip it.
   */
  private List<String> selectWithSkipLockedReader(List<String> jobIds) throws SQLException {
    if (DbSqlSessionFactory.DB2.equals(getDatabaseType()) || jobIds.isEmpty()) {
      return jobIds;
    }
    String sql = DbSqlSessionFactory.MSSQL.equals(getDatabaseType())
      ? "select ID_ from ACT_RU_JOB with (updlock, rowlock, readpast) where ID_ = ?"
      : "select ID_ from ACT_RU_JOB where ID_ = ? for update skip locked";
    Connection probe = openExternalConnection();
    try (PreparedStatement statement = probe.prepareStatement(sql)) {
      List<String> visibleRows = new ArrayList<>();
      for (String jobId : jobIds) {
        statement.setString(1, jobId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            visibleRows.add(resultSet.getString(1));
          }
        }
      }
      return visibleRows;
    } finally {
      probe.rollback();
    }
  }

  /**
   * Runs a full job acquisition on a separate thread and fails the test with
   * {@code blockedMessage} if it does not return within the timeout — i.e. if the
   * acquisition blocked on foreign locks instead of skipping them.
   */
  protected AcquiredJobs acquireJobsBounded(int maxJobs, String blockedMessage) {
    Future<AcquiredJobs> acquisition = boundedExecutor.submit(() ->
        processEngineConfiguration.getCommandExecutorTxRequired()
            .execute(new AcquireJobsCmd(getJobExecutor(), maxJobs)));
    try {
      return acquisition.get(BLOCK_DETECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      acquisition.cancel(true);
      return fail("[%s] %s".formatted(getDatabaseType(), blockedMessage));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return fail("unexpected interruption while acquiring jobs");
    } catch (java.util.concurrent.ExecutionException e) {
      return fail("job acquisition failed on " + getDatabaseType(), e.getCause());
    }
  }

  protected static List<String> jobIdsOf(AcquiredJobs acquiredJobs) {
    return acquiredJobs.getJobIdBatches().stream().flatMap(List::stream).toList();
  }

  /**
   * Acquisition command that pauses after the SKIP LOCKED select has run but before
   * the transaction is flushed and committed — i.e. exactly while the acquisition
   * transaction holds its FOR UPDATE row locks.
   */
  protected static class ControllableAcquisitionCommand extends ControllableCommand<Void> {

    protected final AcquireJobsCmd delegate;
    protected volatile AcquiredJobs acquiredJobs;
    protected volatile boolean selectionCompleted;

    public ControllableAcquisitionCommand(JobExecutor jobExecutor, int maxJobs) {
      this.delegate = new AcquireJobsCmd(jobExecutor, maxJobs);
    }

    @Override
    public Void execute(CommandContext commandContext) {
      acquiredJobs = delegate.execute(commandContext);
      selectionCompleted = true;
      monitor.sync();
      return null;
    }
  }

}
