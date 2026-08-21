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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves or disproves, per database: an acquisition with
 * {@code jobExecutorAcquireWithSkipLocked} enabled skips job rows that are row-locked
 * by another uncommitted transaction, without blocking on them.
 */
class JobExecutorSkipLockedForeignLockTest extends AbstractJobExecutorSkipLockedRowLockTest {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/nonExclusiveAsyncProcess.bpmn20.xml")
  void shouldSkipRowsLockedByForeignTransaction() throws Exception {
    // given: 5 acquirable non-exclusive jobs, 2 of them row-locked by an uncommitted competitor
    startProcessInstances("nonExclusiveAsyncProcess", 5);
    List<String> allJobIds = findAllJobIds();
    List<String> lockedJobIds = allJobIds.subList(0, 2);
    List<String> freeJobIds = new ArrayList<>(allJobIds.subList(2, allJobIds.size()));

    Connection competitor = openExternalConnection();
    lockJobRowsExternally(competitor, lockedJobIds);
    assertRowsFreeForSkipLockedReader(freeJobIds);

    // when
    AcquiredJobs acquiredJobs = acquireJobsBounded(10,
        "acquisition BLOCKED on foreign row locks instead of skipping them — the SKIP LOCKED variant is ineffective on this database");

    // then: exactly the unlocked jobs are acquired
    assertThat(jobIdsOf(acquiredJobs))
      .as("[%s] acquisition must skip foreign-locked rows and acquire all remaining jobs", getDatabaseType())
      .containsExactlyInAnyOrderElementsOf(freeJobIds);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/nonExclusiveAsyncProcess.bpmn20.xml")
  void shouldReturnEmptyAndNotBlockWhenAllRowsLocked() throws Exception {
    // given: every acquirable job is row-locked by an uncommitted competitor
    startProcessInstances("nonExclusiveAsyncProcess", 5);
    List<String> allJobIds = findAllJobIds();

    Connection competitor = openExternalConnection();
    lockJobRowsExternally(competitor, allJobIds);

    // when
    AcquiredJobs acquiredJobs = acquireJobsBounded(10,
        "acquisition BLOCKED although every candidate row was locked — it must return empty instead");

    // then
    assertThat(jobIdsOf(acquiredJobs))
      .as("[%s] acquisition must come back empty-handed, not blocked, when all rows are locked", getDatabaseType())
      .isEmpty();
  }

}
