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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Proves or disproves, per database: while one acquisition transaction holds its
 * SKIP LOCKED row locks, a competing acquisition still gets a full, disjoint page.
 *
 * <p>This is the decisive test for two suspected vendor pathologies:
 * <ul>
 *   <li>over-locking — the locking clause locks more rows than the page
 *       (e.g. a plan that locks all matching rows before the row-number cutoff),
 *       so the competitor finds everything locked and comes back empty;</li>
 *   <li>under-filling — the page budget is consumed by locked rows
 *       (e.g. a row-limit applied before the lock attempt), so the competitor
 *       returns fewer jobs than are freely available.</li>
 * </ul>
 * Both manifest here as the competitor acquiring fewer than the expected jobs.
 */
class JobExecutorSkipLockedContentionTest extends AbstractJobExecutorSkipLockedRowLockTest {

  protected static final int PAGE_SIZE = 5;

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/nonExclusiveAsyncProcess.bpmn20.xml")
  void shouldAcquireFullPageWhileCompetitorHoldsRowLocks() {
    // given: 20 acquirable non-exclusive jobs and a competitor paused mid-acquisition,
    // holding row locks on its page
    startProcessInstances("nonExclusiveAsyncProcess", 4 * PAGE_SIZE);

    ControllableAcquisitionCommand competitor = new ControllableAcquisitionCommand(getJobExecutor(), PAGE_SIZE);
    ThreadControl competitorThread = executeControllableCommand(competitor);
    competitorThread.reportInterrupts();
    competitorThread.waitForSync(BLOCK_DETECTION_TIMEOUT_MS);
    assertThat(competitor.selectionCompleted)
      .as("[%s] test setup: the competitor acquisition never finished its select", getDatabaseType())
      .isTrue();
    List<String> competitorJobIds = jobIdsOf(competitor.acquiredJobs);
    assertThat(competitorJobIds).hasSize(PAGE_SIZE);

    // when
    AcquiredJobs acquiredJobs = acquireJobsBounded(PAGE_SIZE,
        "acquisition BLOCKED although a full page of unlocked jobs was available");

    // then: a full page, disjoint from the competitor's page
    List<String> acquiredJobIds = jobIdsOf(acquiredJobs);
    assertThat(acquiredJobIds)
      .as("[%s] a competitor holding %s row locks must not prevent acquiring a full page "
          + "from the remaining jobs (fewer = over-locking or under-filling)", getDatabaseType(), PAGE_SIZE)
      .hasSize(PAGE_SIZE)
      .doesNotContainAnyElementsOf(competitorJobIds);

    // and: the competitor commits its page untouched
    competitorThread.waitUntilDone();
    assertThat(jobIdsOf(competitor.acquiredJobs)).hasSize(PAGE_SIZE);
    assertThat(competitor.acquiredJobs.getNumberOfJobsFailedToLock()).isZero();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/nonExclusiveAsyncProcess.bpmn20.xml")
  void shouldAcquireDisjointSetsUnderConcurrentAcquisition() {
    // given: 20 acquirable non-exclusive jobs
    startProcessInstances("nonExclusiveAsyncProcess", 4 * PAGE_SIZE);

    // when: two acquisitions overlap, both holding their row locks at the same time
    ControllableAcquisitionCommand first = new ControllableAcquisitionCommand(getJobExecutor(), PAGE_SIZE);
    ThreadControl firstThread = executeControllableCommand(first);
    firstThread.reportInterrupts();
    firstThread.waitForSync(BLOCK_DETECTION_TIMEOUT_MS);

    ControllableAcquisitionCommand second = new ControllableAcquisitionCommand(getJobExecutor(), PAGE_SIZE);
    ThreadControl secondThread = executeControllableCommand(second);
    secondThread.reportInterrupts();
    secondThread.waitForSync(BLOCK_DETECTION_TIMEOUT_MS);

    if (!second.selectionCompleted) {
      // unblock the DB before failing, otherwise teardown would hang on the stuck thread
      firstThread.waitUntilDone();
      secondThread.waitForSync(BLOCK_DETECTION_TIMEOUT_MS);
      secondThread.waitUntilDone();
      fail("[%s] the second acquisition BLOCKED on the first one's row locks instead of skipping them"
          .formatted(getDatabaseType()));
    }

    firstThread.waitUntilDone();
    secondThread.waitUntilDone();

    // then: full disjoint pages, no optimistic locking casualties
    List<String> firstJobIds = jobIdsOf(first.acquiredJobs);
    List<String> secondJobIds = jobIdsOf(second.acquiredJobs);

    assertThat(firstJobIds).hasSize(PAGE_SIZE);
    assertThat(secondJobIds)
      .as("[%s] concurrent acquirers must claim full disjoint pages", getDatabaseType())
      .hasSize(PAGE_SIZE)
      .doesNotContainAnyElementsOf(firstJobIds);

    Set<String> distinct = new HashSet<>(firstJobIds);
    distinct.addAll(secondJobIds);
    assertThat(distinct).hasSize(2 * PAGE_SIZE);

    assertThat(first.acquiredJobs.getNumberOfJobsFailedToLock()).isZero();
    assertThat(second.acquiredJobs.getNumberOfJobsFailedToLock())
      .as("[%s] SKIP LOCKED acquirers must not collide optimistically on non-exclusive jobs", getDatabaseType())
      .isZero();
  }

}
