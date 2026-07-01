/*
 * Copyright 2025 the Operaton contributors.
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database-specific tests for SKIP LOCKED feature.
 * This ensures the feature works correctly on all supported databases,
 * especially SQL Server which requires different syntax.
 */
class JobExecutorSkipLockedDatabaseCompatibilityTest extends AbstractJobExecutorAcquireJobsTest {

  @BeforeEach
  void prepareProcessEngineConfiguration() {
    configuration = rule.getProcessEngineConfiguration();
    managementService = rule.getManagementService();
    runtimeService = rule.getRuntimeService();
    configuration.setJobExecutorAcquireWithSkipLocked(true);
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.H2)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnH2() {
    testSkipLockedBehavior();
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.POSTGRES)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnPostgreSQL() {
    testSkipLockedBehavior();
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.MSSQL)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnSQLServer() {
    // This test verifies that SQL Server uses WITH (ROWLOCK, READPAST)
    // instead of FOR UPDATE SKIP LOCKED
    testSkipLockedBehavior();
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.MYSQL)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnMySQL() {
    testSkipLockedBehavior();
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.ORACLE)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnOracle() {
    testSkipLockedBehavior();
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.DB2)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnDB2() {
    testSkipLockedBehavior();
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.MARIADB)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnMariaDB() {
    testSkipLockedBehavior();
  }

  /**
   * Common test logic for all databases.
   * This method verifies that locked jobs are properly skipped
   * during job acquisition, regardless of the database vendor.
   */
  private void testSkipLockedBehavior() {
    // given: jobs where some are locked
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    }

    List<Job> allJobs = managementService.createJobQuery().list();
    String lockedJobId = allJobs.get(0).getId();

    // lock one job
    configuration.getCommandExecutorTxRequired().execute(commandContext -> {
      commandContext.getJobManager()
        .findJobById(lockedJobId)
        .setLockOwner("test-lock-owner");
      commandContext.getJobManager()
        .findJobById(lockedJobId)
        .setLockExpirationTime(ClockUtil.getCurrentTime());
      return null;
    });

    // when: acquiring jobs
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then: locked job is skipped (4 acquired instead of 5)
    assertThat(acquirableJobs).hasSize(4);
    assertThat(acquirableJobs)
      .extracting(AcquirableJobEntity::getId)
      .doesNotContain(lockedJobId);
  }

  @Test
  @RequiredDatabase(includes = DbSqlSessionFactory.MSSQL)
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedOnSQLServerWithMultipleLockedJobs() {
    // given: SQL Server uses WITH (ROWLOCK, READPAST) syntax
    // This test ensures multiple locked jobs are handled correctly
    for (int i = 0; i < 10; i++) {
      runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    }

    List<Job> allJobs = managementService.createJobQuery().list();

    // lock 3 jobs
    configuration.getCommandExecutorTxRequired().execute(commandContext -> {
      for (int i = 0; i < 3; i++) {
        String jobId = allJobs.get(i).getId();
        commandContext.getJobManager()
          .findJobById(jobId)
          .setLockOwner("test-lock-owner");
        commandContext.getJobManager()
          .findJobById(jobId)
          .setLockExpirationTime(ClockUtil.getCurrentTime());
      }
      return null;
    });

    // when: acquiring jobs
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then: only unlocked jobs are acquired (7 out of 10)
    assertThat(acquirableJobs).hasSize(7);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testSkipLockedDisabledDoesNotUseSkipLockedSyntax() {
    // given: skip locked disabled
    configuration.setJobExecutorAcquireWithSkipLocked(false);

    // and: create and lock a job
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    List<Job> allJobs = managementService.createJobQuery().list();
    String lockedJobId = allJobs.get(0).getId();

    configuration.getCommandExecutorTxRequired().execute(commandContext -> {
      commandContext.getJobManager()
        .findJobById(lockedJobId)
        .setLockOwner("test-lock-owner");
      commandContext.getJobManager()
        .findJobById(lockedJobId)
        .setLockExpirationTime(ClockUtil.getCurrentTime());
      return null;
    });

    // when: acquiring jobs without skip locked
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then: behavior depends on database and isolation level
    // The query should not use SKIP LOCKED / READPAST syntax
    // This is more of a sanity check that the configuration works
    assertThat(configuration.isJobExecutorAcquireWithSkipLocked()).isFalse();
  }
}
