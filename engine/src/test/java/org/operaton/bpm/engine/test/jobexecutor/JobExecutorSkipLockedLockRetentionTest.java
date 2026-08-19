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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves or disproves, per database: the SKIP LOCKED acquisition select actually
 * retains row locks on the acquired jobs until its transaction commits. If it does
 * not (e.g. if the locking clause is accepted syntactically but locks are released
 * at fetch time), competing acquirers have nothing to skip and the feature silently
 * degrades to plain optimistic locking.
 */
class JobExecutorSkipLockedLockRetentionTest extends AbstractJobExecutorSkipLockedRowLockTest {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/nonExclusiveAsyncProcess.bpmn20.xml")
  void shouldHoldRowLocksOnAcquiredJobsUntilCommit() throws Exception {
    // given: an acquisition transaction that is paused after its SKIP LOCKED select
    startProcessInstances("nonExclusiveAsyncProcess", 3);

    ControllableAcquisitionCommand acquisition = new ControllableAcquisitionCommand(getJobExecutor(), 3);
    ThreadControl acquisitionThread = executeControllableCommand(acquisition);
    acquisitionThread.reportInterrupts();
    acquisitionThread.waitForSync(BLOCK_DETECTION_TIMEOUT_MS);
    assertThat(acquisition.selectionCompleted)
      .as("[%s] test setup: the paused acquisition never finished its select", getDatabaseType())
      .isTrue();
    String acquiredJobId = jobIdsOf(acquisition.acquiredJobs).get(0);

    // when: an external transaction updates one of the acquired rows
    Future<Integer> update = boundedExecutor.submit(() -> {
      try (Connection connection = processEngineConfiguration.getDataSource().getConnection()) {
        connection.setAutoCommit(true);
        try (PreparedStatement statement = connection.prepareStatement(
            "update ACT_RU_JOB set REV_ = REV_ + 1 where ID_ = ?")) {
          statement.setString(1, acquiredJobId);
          return statement.executeUpdate();
        }
      }
    });

    // then: the update blocks while the acquisition transaction is open...
    assertThatThrownBy(() -> update.get(2, TimeUnit.SECONDS))
      .as("[%s] the acquisition select did NOT retain a row lock on the acquired job — "
          + "competing acquirers have nothing to skip on this database", getDatabaseType())
      .isInstanceOf(TimeoutException.class);

    // ...and completes once the acquisition commits
    acquisitionThread.waitUntilDone();
    assertThat(update.get(BLOCK_DETECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isEqualTo(1);
  }

}
