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
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the exclusive-job guarantees survive the SKIP LOCKED split:
 * exclusive jobs are still acquired via the contention-based path, so sibling
 * exclusive jobs of one process instance are never won by two acquirers, and
 * the split's exclusive-first page fill behaves as decided (and documented).
 */
class JobExecutorSkipLockedExclusiveSerializationTest extends AbstractJobExecutorSkipLockedRowLockTest {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/exclusiveParallelAsyncProcess.bpmn20.xml")
  void shouldNotAcquireSiblingExclusiveJobsTwice() {
    // given: one process instance with two sibling exclusive jobs
    startProcessInstances("exclusiveParallelAsyncProcess", 1);
    List<String> allJobIds = findAllJobIds();
    assertThat(allJobIds).hasSize(2);

    // when: two acquisitions overlap; both may select the exclusive siblings,
    // because exclusive serialization is contention-based, not lock-based
    ControllableAcquisitionCommand first = new ControllableAcquisitionCommand(getJobExecutor(), 10);
    ThreadControl firstThread = executeControllableCommand(first);
    firstThread.reportInterrupts();
    firstThread.waitForSync(BLOCK_DETECTION_TIMEOUT_MS);

    ControllableAcquisitionCommand second = new ControllableAcquisitionCommand(getJobExecutor(), 10);
    ThreadControl secondThread = executeControllableCommand(second);
    secondThread.reportInterrupts();
    secondThread.waitForSync(BLOCK_DETECTION_TIMEOUT_MS);

    firstThread.waitUntilDone();
    secondThread.waitUntilDone();

    // then: every job was won by exactly one acquirer
    List<String> firstJobIds = jobIdsOf(first.acquiredJobs);
    List<String> secondJobIds = jobIdsOf(second.acquiredJobs);

    Set<String> wonByBoth = new HashSet<>(firstJobIds);
    wonByBoth.retainAll(secondJobIds);
    assertThat(wonByBoth)
      .as("[%s] sibling exclusive jobs must never be won by two acquirers", getDatabaseType())
      .isEmpty();
    assertThat(firstJobIds.size() + secondJobIds.size())
      .as("[%s] both exclusive jobs must be acquired exactly once overall", getDatabaseType())
      .isEqualTo(2);

    // and: the loser lost through optimistic locking, which is the intended mechanism
    int failedToLock = first.acquiredJobs.getNumberOfJobsFailedToLock()
        + second.acquiredJobs.getNumberOfJobsFailedToLock();
    assertThat(failedToLock).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/jobexecutor/nonExclusiveAsyncProcess.bpmn20.xml"
  })
  void shouldFillPageWithExclusiveJobsFirst() {
    // documents the DECIDED behavior of the split: exclusive jobs fill the page
    // before any non-exclusive job is considered (accepted fairness bias)
    startProcessInstances("simpleAsyncProcess", 3);
    startProcessInstances("nonExclusiveAsyncProcess", 3);

    List<String> exclusiveJobIds = managementService.createJobQuery()
        .processDefinitionKey("simpleAsyncProcess")
        .list().stream().map(Job::getId).toList();

    AcquiredJobs acquiredJobs = acquireJobsBounded(3,
        "acquisition blocked unexpectedly without any competing locks");

    assertThat(jobIdsOf(acquiredJobs))
      .as("[%s] the split fills the page with exclusive jobs first — if this fails, "
          + "the exclusive-first decision no longer holds and the docs must change", getDatabaseType())
      .containsExactlyInAnyOrderElementsOf(exclusiveJobIds);
  }

}
